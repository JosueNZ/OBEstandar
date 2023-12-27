/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import java.io.StringWriter;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.util.Check;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.mobile.core.MobileServerDefinition;
import org.openbravo.mobile.core.process.JSONProcessSimple;
import org.openbravo.mobile.core.process.JSONRowConverter;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryArchive;
import org.openbravo.service.importprocess.ImportEntryArchivePreProcessor;
import org.openbravo.service.importprocess.ImportEntryManager;
import org.openbravo.service.importprocess.ImportEntryPreProcessor;
import org.openbravo.service.importprocess.ImportProcessUtils;
import org.openbravo.service.json.JsonConstants;

/**
 * Handles many cases of WebPOS working together with central and store server. This class can
 * handle situations of a store being offline, central being offline or both being available.
 * 
 * If the json is sent from WebPOS and processed on the store server there are several options which
 * play a role:
 * <ul>
 * <li>_onlyDoCentralFromStore: if this json property has value true then the action is forwarded to
 * the central. If the central server is not available then an exception is returned.</li>
 * <li>_tryCentralFromStore: if this json property has true then first the central server is tried
 * before possibly executing locally. If the central server is not available then the action is
 * executed locally and no exception is thrown. If the central server is available and the action is
 * executed there then depending on the value of the _executeInOneServer property the action is
 * repeated on the store server:
 * <ul>
 * <li>true: the action is only executed on one server. So if the central server already executed
 * the action then it is not repeated on the store server.</li>
 * <li>false: the action is executed on both servers if possible. So if the central server already
 * executed the action then it is repeated on the store server by creating an import entry.</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * If the json is sent from WebPOS and processed on the central server. Happens when WebPOS connects
 * to the central server because the store server is down. Then the logic will process locally in
 * central and create an import entry which is later replicated to the store server.
 * 
 * If the json is sent from the STORE in a webrequest then this is a direct call from the store
 * server, process in central but don't create import entry as the store already knows about the
 * action.
 * 
 * If the json is sent from the STORE to central not-in-a-webrequest then this is an import entry
 * replicated from the store. Set the import entry to processed after processing in central.
 * 
 * If the json is sent from CENTRAL and processed in the store. Then this must be through an import
 * entry replicated from central. Process the json and set the import entry to processed.
 * 
 * The source of the sending of the json is obtained from the json itself.
 * 
 * Note, additional features: if the _executeInOneServer property is passed in the json then an
 * action is executed at most in one server, first central and if central is not available then in
 * the store. No import entries are created to be replicated, the assumption is that the
 * transactional tables are replicated.
 * 
 * This class also provides several protected methods which can be overridden to change the generic
 * behavior in the specific concrete subclass.
 * 
 * For more information see this wiki page:
 * http://wiki.openbravo.com/wiki/Retail:Store_Server#Synchronized_Transactions
 * 
 * @author mtaal
 */
public abstract class MultiServerJSONProcess extends JSONProcessSimple {

  public static final String CALL_CENTRAL_PROP = "_tryCentralFromStore";
  public static final String CALL_ONLY_CENTRAL_PROP = "_onlyDoCentralFromStore";
  public static final String EXECUTE_IN_ONE_SERVER = "_executeInOneServer";
  public static final String RESULT_PROP = "_result";
  public static final String SOURCE_PROP = "_source";
  public static final String SOURCE_WEBPOS = "WEBPOS";
  public static final String SOURCE_STORE = "STORE";
  public static final String SOURCE_CENTRAL = "CENTRAL";

  private static final Logger log = Logger.getLogger(MultiServerJSONProcess.class);

  private Entity entryEntity = ModelProvider.getInstance().getEntity(ImportEntry.ENTITY_NAME);
  private Entity archiveEntity = ModelProvider.getInstance().getEntity(
      ImportEntryArchive.ENTITY_NAME);

  @Inject
  @Any
  private Instance<ImportEntryArchivePreProcessor> archiveEntryPreProcessors;

  @Inject
  private ImportEntryManager importEntryManager;

  @Inject
  @Any
  private Instance<ImportEntryPreProcessor> entryPreProcessors;

  @Inject
  @Any
  private Instance<MultiServerJSONProcessHook> processHooks;

  @Override
  public JSONObject exec(JSONObject jsonSent) {
    // logTime is used to relate log statements in begin and end to eachother
    final long logTime = System.currentTimeMillis();
    Throwable thrownError = null;

    if (log.isDebugEnabled()) {
      log.debug("Pre-Handling of json (" + logTime + ") input " + jsonSent);
    }

    // do a pre-check if the message was already handled somehow
    OBContext.setAdminMode(true);
    try {
      final ImportEntryArchive archiveEntry = getImportEntryArchive(jsonSent);
      if (archiveEntry != null) {
        // entry present, so already being handled, but check different states
        final String importStatus = archiveEntry.getImportStatus();
        if ("Error".equals(importStatus)) {
          throw new OBException(archiveEntry.getErrorinfo());
        } else if ("Processed".equals(importStatus)) {
          // in other cases, just return success
          return createSuccessResponse(jsonSent, new JSONObject());
        } else {
          // still being processed, show message
          final JSONObject result = createSuccessResponse(jsonSent, new JSONObject());
          result.put("showMessage",
              OBMessageUtils.getI18NMessage("OBMOBC_DataIsBeingProcessed", null));
          return result;
        }
      } else if (jsonSent.has("messageId") && getImportEntryId() == null
          && !doesImportEntryExist(jsonSent.getString("messageId"))) {
        // only create an archive entry if there is no import entry
        createArchiveEntry(jsonSent.getString("messageId"), jsonSent);
        OBDal.getInstance().commitAndClose();
      } else if (jsonSent.has("messageId") && getImportEntryId() == null
          && doesImportEntryExist(jsonSent.getString("messageId"))) {
        log.warn("The message was already processed, there is an import entry - archive for it, "
            + "not processing this duplicate message, message content: " + jsonSent);
        return createSuccessResponse(jsonSent, new JSONObject());
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

    // returning must be done using the returnResult at the end of the try-finally
    // don't return in the middle as extra checking is done at the end of the try-finally
    JSONObject returnResult = null;
    boolean errorOccurred = true;
    executeProcessHooks(jsonSent);
    String messageId = null;
    try {
      try {
        // handle different cases
        if (!jsonSent.has(SOURCE_PROP)) {
          jsonSent.put(SOURCE_PROP, SOURCE_WEBPOS);
        }

        // make sure the json has a message id so duplicates can be detected
        if (!jsonSent.has("messageId")) {
          jsonSent.put("messageId", SequenceIdData.getUUID());
        }
        messageId = jsonSent.getString("messageId");

        final String source = jsonSent.getString(SOURCE_PROP);

        if (SOURCE_CENTRAL.equals(source)) {
          // case 1: processing an import entry in the store which was processed before in the
          // central server
          returnResult = executeLocal(jsonSent);

          if (getImportEntryId() == null) {
            // note: normal case is that we are processed from import entry
            // anything else is not expected
            // in future it can be the case that also central sends transactions
            // directly to store using a webservice, at that time this line may go
            throw new OBException("Source central is expected to go through import entry "
                + jsonSent);
          }

        } else if (SOURCE_WEBPOS.equals(source)) {
          if (MobileServerController.getInstance().isThisAStoreServer()) {
            // case 2: if from webpos and this is store server
            returnResult = executeFromWebPOSInStore(jsonSent);
          } else if (MobileServerController.getInstance().isThisACentralServer()) {
            // case 3: if from webpos and this is central server then we are called directly
            // from webpos, happens when store server is down,
            // the import entry should be created to synchronize
            // back to the store server
            returnResult = executeFromWebPOSInCentral(jsonSent);
          } else {
            // case 4 not a multi-server environment, process locally
            returnResult = executeLocal(jsonSent);
          }
        } else if (SOURCE_STORE.equals(source)) {
          // case 5: transaction received from store, we must be central then
          if (MobileServerController.getInstance().isThisACentralServer()) {

            // execute locally
            returnResult = executeLocal(jsonSent);

            if (isErrorJson(returnResult)) {
              // don't do anything, will be handled in finally block
            } else if (RequestContext.get().getRequest() != null) {
              // case 5a: web request send from store, don't create import entry as this
              // is done by store itself, only create archive

              // be defensive
              if (getImportEntryId() != null) {
                throw new OBException("Illegal flow, did not expect import entry to be set "
                    + jsonSent);
              }

            } else {
              // case 5b: import entry received from store server, this flow happens
              // when the store server can not reach central and processes transactions
              // offline/in the store

              // be defensive
              if (getImportEntryId() == null) {
                throw new OBException("Illegal flow, expect import entry to be set " + jsonSent);
              }

              // import entry is set to processed in finally block
            }
          } else {
            // defensive, not expected flow
            throw new OBException("Source of json is STORE but this is "
                + "not a central server unsupported case " + jsonSent);
          }
        } else {
          // defensive coding
          throw new OBException("Source not supported, see json " + jsonSent);
        }

        if (log.isDebugEnabled()) {
          log.debug("Result (" + logTime + "): " + returnResult);
        }

        // last checks, return is done at the end of the method
        Check.isNotNull(returnResult, "Return result may not be null");
        errorOccurred = isErrorJson(returnResult);
      } finally {

        // note this finally block is inside another finally block
        // because the commit here can again throw an exception
        // which should be correctly handled.
        if (!errorOccurred) {
          OBContext.setAdminMode();
          try {
            if (getImportEntryId() != null
                && !importEntryManager.isImportEntryError(getImportEntryId())) {
              importEntryManager.setImportEntryProcessed(getImportEntryId());
            }

            if (getImportEntryId() == null) {
              updateArchiveStatus(messageId, "Processed");
            }

            OBDal.getInstance().commitAndClose();
          } finally {
            OBContext.restorePreviousMode();
          }

          // kick the import entry thread to process right away
          importEntryManager.notifyNewImportEntryCreated();
        }
      }
    } catch (Throwable t) {
      thrownError = t;
      log.error(t.getMessage() + " - " + (jsonSent != null ? jsonSent.toString() : "NULL"), t);

      final StringWriter sw = new StringWriter();
      JSONRowConverter.addJSONExceptionFields(sw, t);
      try {
        return new JSONObject("{" + sw.toString() + "}");
      } catch (JSONException e) {
        throw new OBException(e);
      }
    } finally {
      if (thrownError != null || errorOccurred) {
        try {
          OBDal.getInstance().rollbackAndClose();
        } catch (Throwable ignore) {
          // don't hide the other error
        }

        // set the import entry to error
        if (getImportEntryId() != null) {
          try {

            if (thrownError == null) {
              // error json
              thrownError = new Exception(returnResult != null ? returnResult.toString()
                  : "ERROR No returnResult");
            }

            importEntryManager.setImportEntryErrorIndependent(getImportEntryId(), thrownError);
          } catch (Throwable ignore) {
            // don't hide the other error
          }
        } else {
          if (thrownError == null) {
            thrownError = new Exception(returnResult != null ? returnResult.toString()
                : "ERROR No returnResult");
          }
          setImportEntryArchiveErrorIndependent(messageId, thrownError);
        }
      }
      // is not really needed as this instance should not be re-used, but to be safe
      setImportEntryId(null);
    }

    for (MultiServerJSONProcessHook processor : processHooks) {
      processor.doAction(returnResult);
    }

    // we get here if no exception was thrown in the inner code above
    return returnResult;
  }

  private ImportEntryArchive getImportEntryArchive(JSONObject jsonSent) {
    try {
      if (!jsonSent.has("messageId")) {
        return null;
      }
      return OBDal.getInstance().get(ImportEntryArchive.class, jsonSent.get("messageId"));
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  /**
   * Return the data type string used when creating the import entry
   */
  protected String getImportEntryDataType() {
    throw new OBException(
        "MultiServerJSONProcess doesn't implement the getImportEntryDataType() method by default, subclasses need to implement it if they are designed to use the Import Entry infrastructure.");
  }

  /**
   * This method should do the real action, and return the json response.
   * 
   * Any error situation should be handled by throwing an exception. This exception is handled in
   * this class and returned to the caller of this class.
   */
  protected abstract JSONObject execute(JSONObject json);

  protected JSONObject executeFromWebPOSInCentral(JSONObject jsonSent) throws Exception {
    // case 2: if from webpos and central server then we are called directly
    // from webpos, the processed import entry should be created to synchronize
    // back to the store server
    final JSONObject result = executeLocal(jsonSent);

    if (!isErrorJson(result) && !executeInOneServer(jsonSent)) {
      final JSONObject json = new JSONObject(jsonSent.toString());
      // sent along from the central to store, set the source to CENTRAL
      json.put(SOURCE_PROP, SOURCE_CENTRAL);
      createImportEntry(jsonSent.getString("messageId"), json, result, null);
      // is set to processed to in finally block of main loop
    }

    // return the result to the store server
    return result;
  }

  protected JSONObject executeFromWebPOSInStore(JSONObject jsonSent) throws Exception {
    final String messageId = jsonSent.getString("messageId");

    // if only execute in central then forward to central, note if central is offline
    // then an exception is thrown but no transition is happening
    if (executeOnlyInCentral(jsonSent)) {
      final JSONObject jsonToCentral = new JSONObject(jsonSent.toString());
      // sent along from the store to central, set the source to STORE
      jsonToCentral.put(SOURCE_PROP, SOURCE_STORE);
      // prevent cycling around
      jsonToCentral.remove(CALL_ONLY_CENTRAL_PROP);
      JSONObject centralResult = null;
      try {
        centralResult = MobileServerRequestExecutor.getInstance().executeCentralRequest(
            MobileServerUtils.OBWSPATH + this.getClass().getName(), jsonToCentral);
      } catch (Throwable t) {
        return handleCentralServerRequestException(t);
      }
      afterReturnFromCentral(jsonToCentral, centralResult);
      return centralResult;
    } else
    // If store is online and central is online execute on central and create import entry to
    // process
    // locally. So in this flow the json results in processing in both central and store.
    // Unless the executeInOneServer returns true then it is processed in one server
    // only
    // The store server processing is done in parallel by creating an import entry.
    // In this execution flow transactional tables are not replicated. The import entry is
    // potentially replicated. This all depends on the synchronized table setup.
    if (MobileServerController.getInstance().getThisMobileServerState()
        .equals(MobileServerState.ONLINE)
        && executeFirstInCentral(jsonSent)
        && MobileServerController.getInstance().isCentralServerOnline()) {
      try {
        JSONObject centralResult = null;
        // execute in the central server
        final JSONObject jsonToCentral = new JSONObject(jsonSent.toString());
        try {
          // sent along from the store to central, set the source to STORE
          jsonToCentral.put(SOURCE_PROP, SOURCE_STORE);
          // prevent cycling around
          jsonToCentral.remove(CALL_CENTRAL_PROP);
          centralResult = MobileServerRequestExecutor.getInstance().executeCentralRequest(
              MobileServerUtils.OBWSPATH + this.getClass().getName(), jsonToCentral);
        } catch (Exception e) {
          return handleCentralServerRequestException(e);
        }
        // do after the catch block, so that a failure here does not force the system
        // to go offline
        afterReturnFromCentral(jsonToCentral, centralResult);

        // central server is offline, check if transitioning
        if (centralResult.has("serverStatus")
            && !centralResult.get("serverStatus").equals(MobileServerState.ONLINE.getValue())) {

          // if transitioning then don't do anything local anymore, return.
          final MobileServerDefinition server = MobileServerController.getInstance()
              .getThisServerDefinition();
          if (MobileServerState.TRANSITION_TO_ONLINE.getValue().equals(server.getStatus())
              || MobileServerState.TRANSITION_TO_OFFLINE.getValue().equals(server.getStatus())) {
            final JSONObject result = new JSONObject();
            result.put("serverStatusSignal", server.getStatus());
            return result;
          } else if (MobileServerState.OFFLINE.getValue().equals(server.getStatus())) {
            // went offline along the way, execute local
            final JSONObject result = executeLocal(jsonSent);
            if (isErrorJson(result)) {
              return result;
            }
            if (!executeInOneServer(jsonSent)) {
              createImportEntry(messageId, jsonSent, result, null);
              // processed is set in finally block of main loop
            }

            return result;
          }
        }

        // error in central, return the error and don't create local import entry
        if (isErrorJson(centralResult)) {
          return centralResult;
        }

        // a webrequest, create local copy for local processing
        if (getImportEntryId() == null && !executeInOneServer(jsonSent)) {
          // local copy
          final JSONObject json = new JSONObject(jsonSent.toString());
          // set the source to central has it already was processed there, so
          // is the same as if the central sends it back to store
          // this property is not overwritten in createImportEntry
          json.put(SOURCE_PROP, SOURCE_CENTRAL);

          // create local import entry which will be processed in parallel
          // calls setImportEntryId
          createImportEntry(messageId, json, centralResult, null);
          // this is a special case, the created import entry should not be set to
          // processed. This is accomplished by resetting the global import entry id to
          // null. The createImportEntry will set it, so here we reset it.
          setImportEntryId(null);
        }

        return centralResult;
      } catch (Throwable t) {
        throw new OBException(t);
      }
    } else if (MobileServerController.getInstance().serverHasTransitioningStatus()) {
      final JSONObject result = new JSONObject();
      MobileServerController.getInstance().setServerStatusJSON(result);
      return result;
    } else {
      // store offline, execute local and if needed create processed import entry which will be
      // replicated to the central server
      final JSONObject result = executeLocal(jsonSent);
      if (isErrorJson(result)) {
        return result;
      }
      if (!executeInOneServer(jsonSent)) {
        // import entry will be replicated to central to be executed there
        createImportEntry(messageId, jsonSent, result, null);
        // processed is set in finally block of main loop
      }

      return result;
    }
  }

  private JSONObject handleCentralServerRequestException(Throwable t) throws Exception {
    log.error(t.getMessage(), t);

    OBContext.setAdminMode(false);
    try {
      // set the central server as not being reachable
      final MobileServerDefinition centralServer = MobileServerController.getInstance()
          .getCentralServer();
      centralServer.setStatus(MobileServerState.OFFLINE.getValue());
      OBDal.getInstance().save(centralServer);
    } finally {
      OBContext.restorePreviousMode();
    }

    if (!MobileServerState.TRANSITION_TO_OFFLINE.equals(MobileServerController.getInstance()
        .getThisMobileServerState())) {
      // transition to offline,
      // which causes the next if to be executed
      log.info("Could not communicate with the central server, starting transition to offline");
      MobileServerController.getInstance().transitionToOffline();
    }

    if (MobileServerController.getInstance().serverHasTransitioningStatus()) {
      final JSONObject result = new JSONObject();
      MobileServerController.getInstance().setServerStatusJSON(result);
      return result;
    }

    // return a exception json, the transaction will be committed is fine
    // as the central server will move to offline so database action
    // happened
    final String msg = OBMessageUtils.getI18NMessage("OBMOBC_MsgCentralServerNotAvailable", null);
    return OBMOBCUtils.createSimpleErrorJson(msg);
  }

  /**
   * Method which is called after the logic has called central and received a response from central.
   * 
   * Is therefore only called when the current server is a store server
   * 
   * @param jsonSent
   *          the json which was sent to central
   * @param resultFromCentral
   *          the result received back from central
   */
  protected void afterReturnFromCentral(JSONObject jsonSent, JSONObject resultFromCentral) {
  }

  /**
   * Controls if when the request is being handled in the store it should forward to central and not
   * do any local actions, so not create a local import entry.
   */
  protected boolean executeOnlyInCentral(JSONObject json) throws JSONException {
    return json.has(CALL_ONLY_CENTRAL_PROP) && json.getBoolean(CALL_ONLY_CENTRAL_PROP);
  }

  /**
   * Execute the action in one server at most, assume that any transactions are replicated through
   * other means than import entry.
   */
  protected boolean executeInOneServer(JSONObject json) throws JSONException {
    return json.has(EXECUTE_IN_ONE_SERVER) && json.getBoolean(EXECUTE_IN_ONE_SERVER);
  }

  /**
   * Controls if the store should first call central and then create a local import entry for local
   * transactions.
   */
  protected boolean executeFirstInCentral(JSONObject json) throws JSONException {
    return json.has(CALL_CENTRAL_PROP) && json.getBoolean(CALL_CENTRAL_PROP);
  }

  /**
   * Creates the import entry by combining the result json and the json which was sent in.
   * 
   * Organization in which the import entry will be created can be passed in. This to allow
   * distributing the import entry to other stores.
   * 
   * If null is passed in for the organization then as a default the system will use the current
   * organization of the user.
   * 
   * Override this method and call the super implementation with a different organization if this is
   * functionally relevant.
   */
  protected void createImportEntry(String messageId, JSONObject sentIn, JSONObject processResult,
      Organization organization) throws JSONException {
    // don't create import entry in error
    if (isErrorJson(processResult)) {
      log.error("Error result not creating import entry " + processResult.toString());
      return;
    }
    // the finally block in the main loop uses this to set status processed
    // There is one case where the entry should not be set to processed, that's done by resetting
    // the import entry id to null setImportEntryId(null) see executeFromWebPOSInStore
    setImportEntryId(messageId);

    final JSONObject result = new JSONObject(sentIn.toString());

    // can already be set by caller
    if (!result.has(SOURCE_PROP)) {
      result.put(SOURCE_PROP,
          (MobileServerController.getInstance().isThisACentralServer() ? SOURCE_CENTRAL
              : SOURCE_STORE));
    }
    result.put(RESULT_PROP, processResult);

    OBContext.setAdminMode(true);
    try {
      if (doesImportEntryExist(messageId)) {
        return;
      }

      final ImportEntry importEntry = localCreateImportEntry(messageId, result, organization);

      OBDal.getInstance().save(importEntry);
    } finally {
      OBContext.restorePreviousMode();
    }
    // note: set to processed is done in a finally block later
  }

  private ImportEntry localCreateImportEntry(String messageId, JSONObject json,
      Organization organization) {

    ImportEntry importEntry = OBProvider.getInstance().get(ImportEntry.class);
    importEntry.setId(messageId);
    importEntry.setRole(OBDal.getInstance().getProxy(Role.class,
        OBContext.getOBContext().getRole().getId()));
    importEntry.setNewOBObject(true);
    importEntry.setImportStatus("Initial");
    importEntry.setImported(null);
    importEntry.setTypeofdata(getImportEntryDataType());
    importEntry.setJsonInfo(json.toString());
    if (organization != null) {
      importEntry.setOrganization(organization);
    }

    for (ImportEntryPreProcessor processor : entryPreProcessors) {
      processor.beforeCreate(importEntry);
    }
    return importEntry;
  }

  private boolean doesImportEntryExist(String id) {
    // check if it is not there already or already archived
    {
      final Query qry = SessionHandler.getInstance().getSession()
          .createQuery("select 1 from " + ImportEntry.ENTITY_NAME + " where id=:id");
      qry.setParameter("id", id);
      qry.setMaxResults(1);
      if (qry.list().size() > 0) {
        return true;
      }
    }
    {
      final Query qry = SessionHandler.getInstance().getSession()
          .createQuery("select 1 from " + ImportEntryArchive.ENTITY_NAME + " where id=:id");
      qry.setParameter("id", id);
      qry.setMaxResults(1);
      if (qry.list().size() > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Update status of import entry archive
   */
  protected void updateArchiveStatus(String id, String status) {
    final ImportEntryArchive archiveEntry = OBDal.getInstance().get(ImportEntryArchive.class, id);
    if (archiveEntry != null) {
      archiveEntry.setImportStatus(status);
    }
  }

  private void setImportEntryArchiveErrorIndependent(String id, Throwable t) {
    OBDal.getInstance().rollbackAndClose();
    final OBContext prevOBContext = OBContext.getOBContext();
    OBContext.setOBContext("0", "0", "0", "0");
    try {
      // do not do org/client check as the error can be related to org/client access
      // so prevent this check to be done to even be able to save org/client access
      // exceptions
      OBContext.setAdminMode(false);
      ImportEntryArchive importEntry = OBDal.getInstance().get(ImportEntryArchive.class, id);
      if (importEntry != null) {
        importEntry.setImportStatus("Error");
        importEntry.setErrorinfo(ImportProcessUtils.getErrorMessage(t));
        OBDal.getInstance().save(importEntry);
        OBDal.getInstance().commitAndClose();
      }
    } catch (Throwable throwable) {
      try {
        OBDal.getInstance().rollbackAndClose();
      } catch (Throwable ignored) {
      }
      throw new OBException(throwable);
    } finally {
      OBContext.restorePreviousMode();
      OBContext.setOBContext(prevOBContext);
    }
  }

  /**
   * Create an archive entry to log what has been done.
   */
  protected void createArchiveEntry(String id, JSONObject json) throws JSONException {
    if (doesImportEntryExist(id)) {
      return;
    }

    OBContext.setAdminMode(true);
    try {
      // note import entry is only created to make it easier to create an archive
      // It should only be created and not saved as then it would be replicated
      // to other servers
      final ImportEntry importEntry = localCreateImportEntry(id, json, null);
      // create the archive
      final ImportEntryArchive archiveEntry = OBProvider.getInstance()
          .get(ImportEntryArchive.class);
      // copy properties with the same name
      for (Property sourceProperty : entryEntity.getProperties()) {
        // ignore these ones
        if (sourceProperty.isOneToMany() || !archiveEntity.hasProperty(sourceProperty.getName())) {
          continue;
        }
        Property targetProperty = archiveEntity.getProperty(sourceProperty.getName());
        // should be the same type
        if (targetProperty.getDomainType().getClass() != sourceProperty.getDomainType().getClass()) {
          continue;
        }
        try {
          archiveEntry
              .set(targetProperty.getName(), importEntry.getValue(sourceProperty.getName()));
        } catch (Exception e) {
          throw new OBException(e);
        }
      }

      for (ImportEntryArchivePreProcessor processor : archiveEntryPreProcessors) {
        processor.beforeArchive(importEntry, archiveEntry);
      }

      // as the id is also copied set it explicitly to new
      archiveEntry.setNewOBObject(true);
      OBDal.getInstance().save(archiveEntry);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  protected JSONObject executeLocal(JSONObject jsonSent) throws Exception {

    final JSONObject result = execute(jsonSent);

    // error was returned here
    if (isErrorJson(result)) {
      return result;
    }

    return createSuccessResponse(jsonSent, result);
  }

  private JSONObject createSuccessResponse(JSONObject jsonSent, JSONObject result) {
    try {
      final JSONObject response = new JSONObject();
      // put the message id and other info also in the result
      response.put("messageId", jsonSent.getString("messageId"));
      if (jsonSent.has("posTerminal")) {
        response.put("posTerminal", jsonSent.getString("posTerminal"));
      }
      if (!response.has(JsonConstants.RESPONSE_STATUS)) {
        response.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      }
      response.put(JsonConstants.RESPONSE_DATA, result);
      return response;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  protected boolean isErrorJson(JSONObject checkResult) throws JSONException {
    return checkResult.has(JsonConstants.RESPONSE_STATUS)
        && checkResult.get(JsonConstants.RESPONSE_STATUS).equals(
            JsonConstants.RPCREQUEST_STATUS_FAILURE);
  }

  protected boolean bypassSecurity() {
    return true;
  }

  protected boolean bypassPreferenceCheck() {
    return true;
  }

  public static abstract class MultiServerJSONProcessHook {
    public abstract void doAction(JSONObject json);

    public void preProcess(MultiServerJSONProcess process, JSONObject jsonsent) {
    }
  }

  private void executeProcessHooks(JSONObject jsonsent) {
    for (MultiServerJSONProcessHook hook : WeldUtils.getInstances(MultiServerJSONProcessHook.class)) {
      hook.preProcess(this, jsonsent);
    }
  }

}
