/*
 ************************************************************************************
 * Copyright (C) 2015-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.Check;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.mobile.core.MobileServerDefinition;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.importprocess.ImportEntry;

/**
 * Provides methods to obtain and manage the online status of the current server.
 * 
 * @author mtaal
 */
public class MobileServerController {

  private static final Logger log = Logger.getLogger(MobileServerController.class);

  private String thisServerDefinitionId = null;

  private static MobileServerController instance = new MobileServerController();

  public static MobileServerController getInstance() {
    return instance;
  }

  public static void setInstance(MobileServerController instance) {
    MobileServerController.instance = instance;
  }

  private boolean isTransitioningToOnline = false;
  private final String mobileServerKey;

  private final boolean isMultiServer;
  private boolean cachedDataInitialized = false;
  private boolean isStoreServer = false;
  private boolean isCentralServer = false;
  private String centralServerKey = null;
  private String centralServerId = null;
  private String thisServerClientId = null;

  private long transitionToOnlineWaitTimeStamp = -1;

  public MobileServerController() {
    // get mobile.server.key from the Openbravo.properties
    mobileServerKey = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty(MobileServerUtils.MOBILE_SERVER_KEY);
    isMultiServer = mobileServerKey != null;
  }

  public String getMobileServerKey() {
    return mobileServerKey;
  }

  private void initializeCachedValues() {
    if (!isMultiServer) {
      return;
    }
    if (cachedDataInitialized) {
      return;
    }
    try {
      OBContext.setAdminMode(false);

      final MobileServerDefinition serverDef = getThisServerDefinition();
      if (serverDef == null) {
        log.warn("Server definition not found for serverkey " + mobileServerKey);
        cachedDataInitialized = true;
        return;
      }
      thisServerClientId = serverDef.getClient().getId();
      isCentralServer = MobileServerUtils.MAIN_SERVER.equals(serverDef.getServerType());
      isStoreServer = MobileServerUtils.STORE_SERVER.equals(serverDef.getServerType());
      if (!isCentralServer) {
        final MobileServerDefinition centralServerDef = readCentralServer();
        centralServerKey = centralServerDef.getMobileServerKey();
        centralServerId = centralServerDef.getId();
      } else {
        centralServerKey = mobileServerKey;
        centralServerId = serverDef.getId();
      }
      cachedDataInitialized = true;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public String getCentralServerKey() {
    initializeCachedValues();
    return centralServerKey;
  }

  public String getThisServerClientId() {
    initializeCachedValues();
    return thisServerClientId;
  }

  /**
   * Return the central server, assuming that there is only one active one for the client.
   * 
   * If no central server is defined then also an OBException is thrown.
   */
  public MobileServerDefinition getCentralServer() {

    initializeCachedValues();
    if (centralServerId == null) {
      return null;
    }
    try {
      OBContext.setAdminMode(false);
      return OBDal.getInstance().get(MobileServerDefinition.class, centralServerId);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private MobileServerDefinition readCentralServer() {

    MobileServerDefinition thisServer = getThisServerDefinition();
    if (thisServer == null) {
      return null;
    }

    OBContext.setAdminMode(false);
    try {
      // Get the central server, don't find ourselves to prevent infinite loops
      // note: filters for the client of the store
      OBQuery<MobileServerDefinition> servers = OBDal.getInstance().createQuery(
          MobileServerDefinition.class,
          "client.id=:clientId and " + MobileServerDefinition.PROPERTY_ACTIVE + "=true and "
              + MobileServerDefinition.PROPERTY_MOBILESERVERKEY + "!='" + mobileServerKey
              + "' and " + MobileServerDefinition.PROPERTY_SERVERTYPE + "='"
              + MobileServerUtils.MAIN_SERVER + "' order by "
              + MobileServerDefinition.PROPERTY_PRIORITY);

      // disable the client and organization filters to allow reading this server definition
      // with a client 0 (for instance from an ant task)
      // it is safe to do it because there is a unique constraints on the mobile server key
      servers.setFilterOnReadableClients(false);
      servers.setFilterOnReadableOrganization(false);
      servers.setNamedParameter("clientId", thisServer.getClient().getId());

      MobileServerDefinition result = null;
      for (MobileServerDefinition srv : servers.list()) {
        if (result == null) {
          result = srv;
        } else {
          log.warn("Multiple central servers are reachable " + result + " other " + srv);
        }
      }

      if (result == null) {
        throw new OBException("No central server defined/reachable");
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public boolean isCentralServerOnline() {

    OBContext.setAdminMode(false);
    try {

      initializeCachedValues();

      // read from the db as it can change along the way
      final Query qry = OBDal
          .getInstance()
          .getSession()
          .createQuery(
              "select " + MobileServerDefinition.PROPERTY_STATUS + " from "
                  + MobileServerDefinition.ENTITY_NAME + " where id=:id");
      qry.setParameter("id", centralServerId);
      final String result = (String) qry.uniqueResult();
      return MobileServerState.ONLINE.getValue().equals(result);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Return true if a valid central server exists.
   */
  public boolean isThereACentralServerDefined() {

    if (isThisACentralServer()) {
      throw new OBException("Do not call this method as a central server");
    }
    initializeCachedValues();
    if (thisServerClientId == null) {
      return false;
    }
    try {
      OBContext.setAdminMode(false);

      // Get the central server
      // note: filters for the client of the store
      OBQuery<MobileServerDefinition> servers = OBDal.getInstance().createQuery(
          MobileServerDefinition.class,
          "client.id=:clientId and " + MobileServerDefinition.PROPERTY_ACTIVE + "=true and "
              + MobileServerDefinition.PROPERTY_MOBILESERVERKEY + "!='" + mobileServerKey
              + "' and " + MobileServerDefinition.PROPERTY_SERVERTYPE + "='"
              + MobileServerUtils.MAIN_SERVER + "' order by "
              + MobileServerDefinition.PROPERTY_PRIORITY);

      servers.setNamedParameter("clientId", thisServerClientId);

      MobileServerDefinition result = null;
      for (MobileServerDefinition srv : servers.list()) {
        if (result == null) {
          result = srv;
        } else {
          log.warn("Multiple central servers are reachable " + result + " other " + srv);
        }
      }
      return result != null;
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void checkMultiServer() {
    Check.isTrue(isMultiServer, "There is no mobile server key defined for this server");
  }

  /**
   * Creates the message to be send to another server. Note the caller has to call commit to make
   * sure the message gets send.
   * 
   * Message sending is asynchronous.
   * 
   * @return the created {@link ImportEntry}
   */
  public ImportEntry sendMessageToStore(Organization store, String json) {
    checkMultiServer();

    try {
      OBContext.setAdminMode(false);
      final ImportEntry importEntry = OBProvider.getInstance().get(ImportEntry.class);
      importEntry.setImportStatus("Processed");
      importEntry.setImported(new Date());
      importEntry.setClient(store.getClient());
      importEntry.setRole(OBDal.getInstance().getProxy(Role.class,
          OBContext.getOBContext().getRole().getId()));
      importEntry.setOrganization(store);
      importEntry.setJsonInfo(json);
      importEntry.setTypeofdata("OBMOBC_ServerMessage");
      OBDal.getInstance().save(importEntry);
      return importEntry;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Creates the message to be send to a central server (so no specific organization is used). Note
   * the caller has to call commit to make sure the message gets send.
   * 
   * Message sending is asynchronous.
   * 
   * @return the created {@link ImportEntry} containing the message
   */
  public ImportEntry sendMessageToCentral(String json) {
    checkMultiServer();

    try {
      OBContext.setAdminMode(false);
      final ImportEntry importEntry = OBProvider.getInstance().get(ImportEntry.class);
      // it is processed on this side, when the message is replicated to the store server
      // it is
      importEntry.setOrganization(OBDal.getInstance().getProxy(Organization.class, "0"));
      importEntry.setRole(OBDal.getInstance().getProxy(Role.class,
          OBContext.getOBContext().getRole().getId()));
      importEntry.setImportStatus("Processed");
      importEntry.setImported(new Date());
      importEntry.setJsonInfo(json);
      importEntry.setTypeofdata("OBMOBC_ServerMessage");
      OBDal.getInstance().save(importEntry);
      return importEntry;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * @param mobileServerDefinition
   *          the server for which the state must be returned
   * @return return the enum representing the state of the server
   */
  public MobileServerState getMobileServerState(MobileServerDefinition mobileServerDefinition) {

    try {
      OBContext.setAdminMode(false);
      return MobileServerState.getMobileServerState(mobileServerDefinition.getStatus());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Utility method to write the current server state to json.
   */
  public void writeServerStatusJSON(Writer w) {
    try {
      final MobileServerState state = getMobileServerState(getThisServerDefinition());
      if (state == null) {
        return;
      }
      w.write("\"serverStatusSignal\": \"" + state.getValue() + "\"");
      return;
    } catch (IOException e) {
      throw new OBException(e);
    }
  }

  /**
   * Utility method to write the current server state to json.
   */
  public void setServerStatusJSON(JSONObject json) {
    try {
      final MobileServerState state = getMobileServerState(getThisServerDefinition());
      if (state == null) {
        return;
      }
      json.put("serverStatusSignal", state.getValue());
      return;
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  /**
   * Return true if the current server state is either
   * {@link MobileServerState#TRANSITION_TO_OFFLINE} or
   * {@link MobileServerState#TRANSITION_TO_ONLINE}. Return false otherwise.
   */
  public boolean serverHasTransitioningStatus() {
    if (!isMultiServer) {
      return false;
    }

    final MobileServerState state = getMobileServerState(getThisServerDefinition());
    if (state == null) {
      return false;
    }
    return MobileServerState.TRANSITION_TO_OFFLINE.equals(state)
        || MobileServerState.TRANSITION_TO_ONLINE.equals(state);
  }

  public MobileServerState getThisMobileServerState() {
    return getMobileServerState(getThisServerDefinition());
  }

  /**
   * Sets and save the state but does not do a commit.
   */
  public void setMobileServerState(MobileServerState state) {
    MobileServerDefinition thisServer = getThisServerDefinition();
    thisServer.setStatus(state.getValue());
    OBDal.getInstance().save(thisServer);
  }

  /**
   * Returns the {@link MobileServerDefinition} for this server.
   */
  public MobileServerDefinition getThisServerDefinition() {
    if (!isMultiServer) {
      return null;
    }
    OBContext.setAdminMode(false);
    try {
      return OBDal.getInstance().get(MobileServerDefinition.class, getThisServerDefinitionId());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Return a specific mobile server
   */
  public MobileServerDefinition getMobileServer(String serverKey) {

    if (serverKey.equals(centralServerKey)) {
      return getCentralServer();
    }

    if (thisServerDefinitionId != null && serverKey.equals(mobileServerKey)) {
      return getThisServerDefinition();
    }

    OBContext.setAdminMode(false);
    try {
      OBQuery<MobileServerDefinition> servers = OBDal.getInstance().createQuery(
          MobileServerDefinition.class,
          MobileServerDefinition.PROPERTY_MOBILESERVERKEY + "=:serverKey");

      // disable the client and organization filters to allow reading this server definition
      // with a client 0 (for instance from an ant task)
      // it is safe to do it because there is a unique constraints on the mobile server key
      servers.setFilterOnReadableClients(false);
      servers.setFilterOnReadableOrganization(false);

      servers.setNamedParameter("serverKey", serverKey);

      final MobileServerDefinition result = servers.uniqueResult();

      if (result == null) {
        throw new OBException("Server not defined " + serverKey);
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Reads the id of the {@link MobileServerDefinition} for the current server and returns it. Does
   * not cache the instance.
   */
  public String getThisServerDefinitionId() {

    if (mobileServerKey == null) {
      return null;
    }

    if (thisServerDefinitionId != null) {
      return thisServerDefinitionId;
    }

    try {
      OBContext.setAdminMode(false);
      MobileServerDefinition serverDefinition = getMobileServer(mobileServerKey);
      thisServerDefinitionId = serverDefinition.getId();
      return thisServerDefinitionId;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Trigger the transition to offline code.
   */
  public synchronized void transitionToOffline() {
    checkMultiServer();

    if (getThisMobileServerState().equals(MobileServerState.TRANSITION_TO_OFFLINE)) {
      log.debug("Already transitioning to offline");
      return;
    }
    // meaning if we are transitioning to online then do not transition to offline,
    // transition to online should detect that it can't transition and move back to
    // offline
    if (!getThisMobileServerState().equals(MobileServerState.ONLINE)) {
      log.debug("Current state is " + getThisMobileServerState()
          + " no need to transition to offline");
      return;
    }
    OBContext.setAdminMode(false);
    try {
      setMobileServerState(MobileServerState.TRANSITION_TO_OFFLINE);
      OBDal.getInstance().commitAndClose();
      doTransitionToOffline();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void doTransitionToOffline() {
    checkMultiServer();

    final MobileMainServerCheck checker = new MobileMainServerCheck(OBContext.getOBContext());
    final Thread thrd = new Thread(checker);
    // daemon ensures that the threads does not block exiting the jvm
    thrd.setDaemon(true);
    thrd.setName("MobileMainServerCheckThread");
    thrd.start();
  }

  /**
   * Check the server status and also checks if the corresponding background logic is running to
   * manage the state. If not it starts the logic.
   */
  public void checkStartServerStateLogic() {
    checkMultiServer();

    final MobileServerState serverState = getMobileServerState(getThisServerDefinition());

    if (MobileServerState.TRANSITION_TO_ONLINE.equals(serverState)) {
      // the transition to online is not being executed, restart it
      if (!isTransitioningToOnline) {
        log.debug("Transition to online status but no transitioning to online running, restarting it");
        transitionToOnline();
      }
    } else if (MobileServerState.OFFLINE.equals(serverState)
        || MobileServerState.TRANSITION_TO_OFFLINE.equals(serverState)) {
      if (!MobileMainServerCheck.isThreadRunning()) {
        log.debug("Transition to offline status but no transitioning to offline running, restarting it");
        doTransitionToOffline();
      }
    }
  }

  /**
   * return true if the server is transitioning to offline currently
   */
  public boolean isTransitioningToOffline() {
    return MobileMainServerCheck.isThreadRunning();
  }

  /**
   * return true if the server is transitioning to online currently
   */
  public boolean isTransitioningToOnline() {
    return isTransitioningToOnline;
  }

  /**
   * Force/start the transition to online procedure.
   */

  public synchronized void transitionToOnline() {
    checkMultiServer();
    if (isTransitioningToOnline) {
      // already here go away
      log.debug("Already transitioning to online");
      return;
    }

    // already there
    if (getThisMobileServerState().equals(MobileServerState.ONLINE)) {
      log.debug("Server is already online");
      return;
    }

    // wait for a while with transitioning to online
    if (transitionToOnlineWaitTimeStamp > System.currentTimeMillis()) {
      log.debug("No unforced transition to online is allowed until "
          + transitionToOnlineWaitTimeStamp + " (current timestamp " + System.currentTimeMillis()
          + ")");
      return;
    }

    final int waitTimeSeconds = MobileServerUtils.getIntegerPreference(
        "OBMOBC_FailedTransitionToOnlineWaitTime", 3600);
    transitionToOnlineWaitTimeStamp = waitTimeSeconds * 1000 + System.currentTimeMillis();

    isTransitioningToOnline = true;
    TransitionToOnline toOnline = new TransitionToOnline();
    toOnline.setOBContext(OBContext.getOBContext());
    final Thread thrd = new Thread(toOnline);
    // daemon ensures that the threads does not block exiting the jvm
    thrd.setDaemon(true);
    thrd.setName("TransitionToOnlineThread");
    thrd.start();
  }

  public void forceTransitionToOnline() {
    log.info("Forcing a transition to online attempt");
    resetTransitionToOnlineWaitTimeStamp();
    transitionToOnline();
  }

  // on purpose package private
  void resetTransitionToOnlineWaitTimeStamp() {
    transitionToOnlineWaitTimeStamp = -1;
  }

  public boolean isThisAStoreServer() {
    if (!isMultiServer) {
      return false;
    }
    initializeCachedValues();
    return isStoreServer;
  }

  public boolean isThisACentralServer() {
    if (!isMultiServer) {
      return false;
    }
    initializeCachedValues();
    return isCentralServer;
  }

  private class TransitionToOnline implements Runnable {

    private OBContext obContext;

    @Override
    public void run() {
      OBContext.setAdminMode(false);
      try {
        initializeOBContext();

        // first do a precheck
        log.info("Checking if transition to offline can start");
        if (!canTransitionToOnline()) {
          log.info("Transition to online can not start, so cancelling it");
          return;
        }

        log.info("Starting transition to online");
        setMobileServerState(MobileServerState.TRANSITION_TO_ONLINE);
        OBDal.getInstance().commitAndClose();

        // do the actual transition
        boolean isTransitioned = doTransitionToOnline();

        if (!isTransitioned) {
          // offline log was already set, now set the state
          setMobileServerState(MobileServerState.OFFLINE);
          OBDal.getInstance().commitAndClose();
          return;
        } else {
          log.info("All handlers were ready to go online, going online");
          final MobileServerDefinition thisServer = getThisServerDefinition();
          thisServer.setOfflineLog(null);
          thisServer.setStatus(MobileServerState.ONLINE.getValue());
          OBDal.getInstance().commitAndClose();
          resetTransitionToOnlineWaitTimeStamp();
          return;
        }
      } catch (Throwable t) {
        final OBException e = new OBException(t);
        try {
          OBDal.getInstance().rollbackAndClose();
          // failed back offline
          setMobileServerState(MobileServerState.OFFLINE);
          OBDal.getInstance().commitAndClose();
        } catch (Throwable t2) {
          // throw the original exception
          throw e;
        }
      } finally {
        isTransitioningToOnline = false;
        OBContext.restorePreviousMode();
      }
    }

    private StringBuilder buildOfflineLogFromHandlers(
        List<MobileServerTransitionToOnlineHandler> handlers) {
      StringBuilder offlineLog = new StringBuilder();
      if (!handlers.isEmpty()) {
        offlineLog.append(">Transition to online failed because of handlers:");
        for (MobileServerTransitionToOnlineHandler handler : handlers) {
          final String handlerLog = handler.getOfflineLog();
          if (handlerLog != null) {
            offlineLog.append("\n");
            offlineLog.append(handlerLog);
          }
        }
      }
      return offlineLog;
    }

    private boolean canTransitionToOnline() throws Exception {
      return tryTransitionToOnline(true);
    }

    private boolean doTransitionToOnline() throws Exception {
      return tryTransitionToOnline(false);
    }

    private boolean tryTransitionToOnline(boolean checkOnly) throws Exception {
      log.debug("Transition to online, check mode " + checkOnly);

      final List<MobileServerTransitionToOnlineHandler> handlers = WeldUtils
          .getInstances(MobileServerTransitionToOnlineHandler.class);

      if (checkOnly) {
        log.info("Checking if transition to offline can start");
      } else {
        log.info("Doing transition to online");
      }

      // try the handlers 3 times if checking, if really transition do it 10 times
      for (int i = 0; i < (checkOnly ? 3 : 10); i++) {
        boolean handlersReady = true;
        for (MobileServerTransitionToOnlineHandler handler : new ArrayList<MobileServerTransitionToOnlineHandler>(
            handlers)) {
          if (checkOnly) {
            handlersReady &= handler.canStartTransitionToOnline();
          } else {
            // really processing
            handler.processTransactions();
            handlersReady &= handler.isReadyToGoOnline();
          }
          log.debug("Calling online transition handler: "
              + handler.getClass().getName() + " iteration " + i + "current result : "
              + handlersReady);
        }

        // already build the total log
        final StringBuilder offlineLog = buildOfflineLogFromHandlers(handlers);

        // the handlers are fine, now check if there are servers offline
        if (handlersReady) {
          boolean thereAreOfflineServers = false;
          final StringBuilder offlineServersLog = new StringBuilder();
          for (CheckServerOfflineHandler handler : WeldUtils
              .getInstances(CheckServerOfflineHandler.class)) {
            if (handler.isServerOffline()) {
              offlineServersLog.append("\n" + handler.getClass().getSimpleName());
              thereAreOfflineServers = true;
            }
          }
          if (thereAreOfflineServers) {
            offlineLog.append("\n>Offline servers check:" + offlineServersLog);
          } else {
            // servers fine, can do transition
            return true;
          }
        }

        // not possible to go online, log it and wait short time before retrying
        // not successful, save the log
        final MobileServerDefinition thisServerDef = getThisServerDefinition();
        thisServerDef.setOfflineLog(offlineLog.toString());
        log.warn("Transition try " + i + " failed caused by: " + thisServerDef.getOfflineLog());
        OBDal.getInstance().commitAndClose();
        if (checkOnly) {
          Thread.sleep(500);
        } else {
          Thread.sleep(10000);
        }
      }
      return false;
    }

    public void setOBContext(OBContext obContext) {
      this.obContext = obContext;
    }

    private void initializeOBContext() {
      OBContext.setOBContext(obContext);
      if (isSystemContext(obContext)) {
        // if the provided context is a system context, use a default one based on the client of the
        // current mobile server
        setDefaultNonSystemOBContext();
      }
    }

    private void setDefaultNonSystemOBContext() {
      Client mobileServerClient = OBDal.getInstance().getProxy(Client.class,
          MobileServerController.getInstance().getThisServerClientId());
      Role role = getAnyNonSystemRoleForClient(mobileServerClient);
      OBContext.getOBContext().setCurrentClient(mobileServerClient);
      OBContext.getOBContext().setRole(role);
    }

    private Role getAnyNonSystemRoleForClient(Client mobileServerClient) {
      OBContext.setAdminMode(false);
      OBCriteria<Role> criteria = OBDal.getInstance().createCriteria(Role.class);
      criteria.setFilterOnReadableClients(false);
      criteria.setFilterOnReadableOrganization(false);
      criteria.add(Restrictions.eq(Role.PROPERTY_CLIENT, mobileServerClient));
      String systemLevel = "S";
      criteria.add(Restrictions.ne(Role.PROPERTY_USERLEVEL, systemLevel));
      criteria.setMaxResults(1);
      return (Role) criteria.uniqueResult();
    }

    private boolean isSystemContext(OBContext context) {
      return context.getCurrentClient().getId().equals("0")
          || context.getRole().getId().equals("0");
    }
  }
}
