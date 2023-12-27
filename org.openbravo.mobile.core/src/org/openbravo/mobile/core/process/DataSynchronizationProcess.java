/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.process;

import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryManager;
import org.openbravo.service.importprocess.ImportProcessUtils;
import org.openbravo.service.json.JsonConstants;

public abstract class DataSynchronizationProcess extends JSONProcessSimple {

  private static final Logger log = Logger.getLogger(DataSynchronizationProcess.class);

  @Inject
  @Any
  private Instance<DataSynchronizationErrorHandler> errorHandlers;

  @Inject
  private ImportEntryManager importEntryManager;

  protected ImportEntryManager getImportEntryManager() {
    return importEntryManager;
  }

  protected void createImportEntry(JSONObject jsonObject) {
    try {
      // the id is a bit hidden...
      // TODO: apparently a json array can be sent
      // the id is derived from the first entry in the array
      // in that case. It is somewhat important that a
      // message received from the backend always has the same id
      // TODO: check if this indeed applies if orders are sent in
      // batches which can change as new orders are being added
      // while a subsequent batch fails. Theoretical case probably
      String id = ImportProcessUtils.getJSONProperty(jsonObject, "messageId");
      if (id == null) {
        id = ImportProcessUtils.getJSONProperty(jsonObject, "id");
      }
      if (id == null) {
        throw new OBException("JSONObject " + jsonObject + " can not be handled to retrieve an id");
      }
      // note original jsonObject is set in the import entry, on purpose
      importEntryManager.createImportEntry(id, getImportQualifier(), jsonObject.toString());
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    return exec(jsonsent, true);
  }

  protected String getImportQualifier() {
    throw new OBException("Method getImportQualifier should be implemented by subclass");
  }

  /**
   * Default implementation of the {@link DataSynchronizationImportProcess} which calls
   * {@link #createImportEntry(JSONObject)} to create the import entry. This method will write a
   * standard success
   * 
   * @param w
   * @param jsonObject
   */
  public void executeCreateImportEntry(Writer w, JSONObject jsonObject) {
    try {
      if (getImportQualifier() == null) {
        return;
      }
      createImportEntry(jsonObject);

      JSONObject jsonResponse = new JSONObject();
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      jsonResponse.put("result", "0");
      JSONObject contextInfo = getContextInformation();
      jsonResponse.put("contextInfo", contextInfo);
      // write only the properties, brackets are written outside.
      final String jsonResponseStr = jsonResponse.toString();
      w.write(jsonResponseStr.substring(1, jsonResponseStr.length() - 1));
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * This method executes the process for all the records in the "data" array, inside the JSON
   * object. If the process fails for a record, it will automatically find an appropriate error
   * handler class, and execute it. A transaction will be used for every record, so every record is
   * considered independent
   * 
   * @param jsonsent
   *          The JSON object which should contain a "data" property, an array which contains a
   *          JSONObject for every record which needs to be saved
   * @param shouldFailWithError
   *          If this is set to true, the process will fail if an error was detected for at least
   *          one error
   * @return returns a JSONObject with the result of the execution
   * @throws JSONException
   * @throws ServletException
   */
  public JSONObject exec(JSONObject jsonsent, boolean shouldFailWithError) throws JSONException,
      ServletException {
    Object jsondata = jsonsent.get("data");

    JSONArray array = null;
    if (jsondata instanceof JSONObject) {
      array = new JSONArray();
      array.put(jsondata);
    } else if (jsondata instanceof String) {
      JSONObject obj = new JSONObject((String) jsondata);
      array = new JSONArray();
      array.put(obj);
    } else if (jsondata instanceof JSONArray) {
      array = (JSONArray) jsondata;
    }

    long t1 = System.currentTimeMillis();
    JSONObject result = this.saveRecord(array, shouldFailWithError);
    log.debug("Final total time: " + (System.currentTimeMillis() - t1));

    return result;
  }

  public JSONObject saveRecord(JSONArray jsonarray, boolean shouldFailWithError)
      throws JSONException {
    boolean error = false;
    boolean obContextChanged = false;
    List<String> errorIds = new ArrayList<String>();
    List<String> errorMsgs = new ArrayList<String>();

    OBContext cnx = OBContext.getOBContext();
    String originalUser = cnx.getUser().getId();
    String originalOrg = cnx.getCurrentOrganization().getId();
    String originalRole = cnx.getRole().getId();

    JSONObject jsonResponse = null;

    OBContext.setAdminMode(false);
    try {
      for (int i = 0; i < jsonarray.length(); i++) {
        String currentUser = cnx.getUser().getId();
        String currentOrg = cnx.getCurrentOrganization().getId();
        String currentRole = cnx.getRole().getId();
        long t1 = System.currentTimeMillis();
        JSONObject jsonRecord = jsonarray.getJSONObject(i);
        String userId = null;
        if (jsonRecord.has("createdBy") && !"null".equals(jsonRecord.getString("createdBy"))) {
          userId = jsonRecord.getString("createdBy");
        } else if (jsonRecord.has("userId") && !"null".equals(jsonRecord.getString("userId"))) {
          userId = jsonRecord.getString("userId");
        } else {
          userId = currentUser;
        }

        String orgId = jsonRecord.has("organization") ? jsonRecord.getString("organization")
            : currentOrg;
        if (!currentUser.equals(userId) || !currentOrg.equals(orgId)) {
          obContextChanged = true;
          OBContext.setOBContext(userId, currentRole, OBContext.getOBContext().getCurrentClient()
              .getId(), orgId);
        }
        JSONObject result = null;
        try {
          result = saveRecord(jsonRecord);

          if (!result.get(JsonConstants.RESPONSE_STATUS).equals(
              JsonConstants.RPCREQUEST_STATUS_SUCCESS)) {
            log.error(this.getClass().getName() + ": There was an error importing record: "
                + jsonRecord.toString());
            error = true;
            errorIds.add(jsonRecord.getString("id"));
            if (result.get(JsonConstants.RESPONSE_ERROR) != null
                && ((JSONObject) result.get(JsonConstants.RESPONSE_ERROR)).getString("message") != null) {
              errorMsgs.add(((JSONObject) result.get(JsonConstants.RESPONSE_ERROR))
                  .getString("message"));
            } else {
              errorMsgs.add("Record " + jsonRecord.getString("id") + " cannot be saved \n");
            }
          }
          if (i % 1 == 0) {
            OBDal.getInstance().flush();
            if (!isRunInSynchronizedMode()) {
              OBDal.getInstance().getConnection(false).commit();
              OBDal.getInstance().getSession().clear();
            }
          }
          log.debug("Total process " + this.getClass().getName() + " time: "
              + (System.currentTimeMillis() - t1));
        } catch (Throwable t) {
          OBDal.getInstance().rollbackAndClose();
          if (TriggerHandler.getInstance().isDisabled()) {
            TriggerHandler.getInstance().enable();
          }
          if (shouldFailWithError) {
            error = true;
          }

          // in synched mode return the error to the caller
          // don't do anything more
          if (isRunInSynchronizedMode()) {
            Throwable localThrowable = getCause(t);
            log.error(localThrowable.getMessage(), localThrowable);
            return OBMOBCUtils.createSimpleErrorJson(localThrowable.getMessage());
          }

          // setImportEntryError is called in handleError
          handleError(t, this.getEntity(), result, jsonRecord);

          try {
            OBDal.getInstance().getConnection().commit();
          } catch (SQLException e1) {
            error = true;
            errorIds.add(jsonRecord.getString("id"));
            errorMsgs.add("Record " + jsonRecord.getString("id") + " cannot be saved \n");

            log.error(
                this.getClass().getName()
                    + ": Critical Error: The process to save the record"
                    + jsonRecord.getString("id")
                    + " has failed and the record cannot be saved as an error. To avoid lose data, please don't remove the browser cache. \n Record: "
                    + jsonRecord.toString() + " \n", e1);
          }
        }
      }

      if (getImportEntryId() != null && !isImportEntryInError()) {

        if (!error) {
          jsonResponse = createSuccessResponse(jsonarray);
          ImportEntry importEntry = OBDal.getInstance().get(ImportEntry.class, getImportEntryId());
          if (importEntry != null) {
            importEntry.setResponseinfo(jsonResponse.toString());
          }
        }

        setImportEntryProcessed();

        try {
          OBDal.getInstance().getConnection().commit();
        } catch (Throwable t) {
          importEntryManager.setImportEntryErrorIndependent(getImportEntryId(), t);
        }
      }

    } finally {
      OBContext.restorePreviousMode();
      if (obContextChanged) {
        OBContext.setOBContext(originalUser, originalRole, OBContext.getOBContext()
            .getCurrentClient().getId(), originalOrg);
      }
      setImportEntryId(null);
    }

    // be sure to always return something
    if (jsonResponse == null) {
      if (!error) {
        jsonResponse = createSuccessResponse(jsonarray);
      } else {
        jsonResponse = createErrorResponse(jsonarray, errorIds, errorMsgs);
      }
    }

    return jsonResponse;
  }

  protected JSONObject createSuccessResponse(JSONArray incomingJson) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
    jsonResponse.put("result", "0");
    return jsonResponse;
  }

  protected JSONObject createErrorResponse(JSONArray incomingJson, List<String> errorIds,
      List<String> errorMsgs) throws JSONException {
    JSONObject jsonResponse = new JSONObject();
    jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
    jsonResponse.put("result", "0");
    JSONObject errors = new JSONObject();
    if (errorIds.size() > 0) {
      jsonResponse.put("errorids", errorIds);
      errors.put("message", "Some records cannot be saved: \n" + errorMsgs.toString());
    } else {
      errors.put("message", "Some records cannot be saved");
    }
    jsonResponse.put("error", errors);
    return jsonResponse;
  }

  private Throwable getCause(Throwable throwable) {
    Throwable localThrowable = DbUtility.getUnderlyingSQLException(throwable);
    final boolean thereWasNoUnderlyingDBException = localThrowable == throwable;
    if (thereWasNoUnderlyingDBException && throwable.getCause() != throwable
        && throwable.getCause() != null) {
      return getCause(throwable.getCause());
    }
    return localThrowable;
  }

  private boolean isImportEntryInError() {
    return importEntryManager.isImportEntryError(getImportEntryId());
  }

  protected void setImportEntryProcessed() {
    if (getImportEntryId() != null) {
      importEntryManager.setImportEntryProcessed(getImportEntryId());
    }
  }

  protected void setImportEntryError(Throwable t) {
    if (getImportEntryId() != null) {
      importEntryManager.setImportEntryError(getImportEntryId(), t);
    }
  }

  private void handleError(Throwable t, Entity entity, JSONObject result, JSONObject jsonRecord) {
    DataSynchronizationErrorHandler errorHandler = getErrorHandler();
    if (errorHandler == null) {
      try {
        log.error(
            this.getClass().getName() + ": Synchronization of record " + jsonRecord.getString("id")
                + " failed (no error handler available)", t);
      } catch (JSONException e1) {
        // won't happen
      }

      if (getImportEntryId() != null) {
        setImportEntryError(t);
      }

    } else {
      additionalProcessForRecordsSavedInErrorsWindow(jsonRecord);
      errorHandler.handleError(t, entity, result, jsonRecord);
      if (getImportEntryId() != null) {
        if (errorHandler.setImportEntryStatusToError()) {
          // if error not handled then set error, if error handled then assume
          // the errorhandler will somehow keep track of errors..
          setImportEntryError(t);
        } else {
          setImportEntryProcessed();
        }
      }
    }
  }

  private DataSynchronizationErrorHandler getErrorHandler() {

    DataSynchronizationErrorHandler errorHandler = null;
    try {
      errorHandler = errorHandlers.select(new ComponentProvider.Selector(getAppName())).get();
    } catch (Exception e) {
      log.debug("Error retrieving error handler for " + getAppName(), e);
      // ignore it
    }
    return errorHandler;
  }

  /**
   * Optionally process for the records that they are saved in Errors While Importing Data window
   */
  protected void additionalProcessForRecordsSavedInErrorsWindow(JSONObject record) {
  }

  public Entity getEntity() {
    if (this.getClass().getAnnotation(DataSynchronization.class) == null) {
      return null;
    }
    return ModelProvider.getInstance().getEntity(
        this.getClass().getAnnotation(DataSynchronization.class).entity());
  }

  public abstract String getAppName();

  public abstract JSONObject saveRecord(JSONObject jsonRecord) throws Exception;

  @javax.inject.Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  public @interface DataSynchronization {
    String entity();
  }

  @SuppressWarnings("all")
  public static class Selector extends AnnotationLiteral<DataSynchronization> implements
      DataSynchronization {
    private static final long serialVersionUID = 1L;

    final String entity;

    public Selector(String entity) {
      this.entity = entity;
    }

    public String entity() {
      return entity;
    }
  }

}
