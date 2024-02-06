/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.mobile.core.MobileServerDefinition;
import org.openbravo.mobile.core.authenticate.MobileAuthenticationKeyUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * This class can be started as a background process. It will check the server status and will send
 * the status of the current server to the other servers.
 * 
 * @author mdejuana
 */
public class ServerStateBackground extends DalBaseProcess {
  private static final String TRANSITION_TO_ONLINE_LOG = "Transition to online";
  private static final String TRANSITION_TO_OFFLINE_LOG = "Transition to offline";
  private static final Logger log4j = Logger.getLogger(ServerStateBackground.class);
  private static final int SERVERS_PER_THREAD = 60;
  private static final int WAIT_TIME = 1000 * 60 * 10;

  private Set<PingServerChecker> pingServerCheckers;

  public ServerStateBackground() {
    super();
  }

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    ProcessLogger logger = bundle.getLogger();
    OBError result = new OBError();
    try {
      OBContext.setAdminMode(false);

      MobileServerDefinition thisServerDef = MobileServerController.getInstance()
          .getThisServerDefinition();
      if (thisServerDef == null) {
        // no server key defined probably, don't execute this process then
        log4j
            .debug("No server key defined or no Mobile Server Definitions with the defined server key. "
                + "The ServerStateBackground process can only run if the server key is defined in Openbravo.properties");
        return;
      }

      log4j.debug("Starting ServerStateBackground process");

      boolean shouldGoOffline = false;
      if (MobileServerController.getInstance().isThisACentralServer()) {
        pingStoreServersAndUpdateTheirState(bundle.getContext(), thisServerDef, result);
      } else {
        shouldGoOffline = isCentralServerOffline(thisServerDef);
      }
      // if any of the offline checks fail, the server should go offline
      shouldGoOffline |= checkOfflineHandlers(thisServerDef);

      // transition depending on the number of online/offline servers
      // read the server def again as the checks above can have taken some time
      final MobileServerState currentState = MobileServerController.getInstance()
          .getMobileServerState(thisServerDef);
      String logAction = "";
      if (shouldGoOffline) {
        logAction = goOfflineIfPossible(currentState);
      } else {
        logAction = goOnlineIfPossible(currentState, thisServerDef);
      }
      result.setType("Success");
      result.setTitle(OBMessageUtils.messageBD("Success"));
      result.setMessage(result.getMessage() + ". Action: " + logAction);
      logger.logln(result.getMessage());
      bundle.setResult(result);

      // clean up any open connections
      OBDal.getInstance().commitAndClose();
    } catch (Throwable t) {
      result = OBMessageUtils.translateError(bundle.getConnection(), bundle.getContext().toVars(),
          OBContext.getOBContext().getLanguage().getLanguage(), t.getMessage());
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));
      log4j.error(result.getMessage(), t);
      logger.logln(result.getMessage());
      bundle.setResult(result);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String goOnlineIfPossible(MobileServerState state, MobileServerDefinition thisServerDef) {
    String logAction = null;
    switch (state) {
    case TRANSITION_TO_OFFLINE:
      // state is transitioning offline, but not really doing that, repair it
      if (!MobileServerController.getInstance().isTransitioningToOffline()) {
        logAction = TRANSITION_TO_ONLINE_LOG;
        MobileServerController.getInstance().transitionToOnline();
      } else {
        // do nothing in this case, just wait until the transition to offline is done
        // will automatically retry in due course
        logAction = "Already transitioning to offline, wait for next cycle for transition to online";
      }
      break;
    case ONLINE:
      // already online do nothing
      logAction = "Already online";
      // reset the log
      thisServerDef.setOfflineLog(null);
      break;
    case TRANSITION_TO_ONLINE:
      // if transitioning to online then check if the transition process is running
      // if not then start it, if yes, then we are fine, no need to do anything
      if (MobileServerController.getInstance().isTransitioningToOnline()) {
        logAction = "Already transitioning to online";
        // just let it go
      } else {
        logAction = TRANSITION_TO_ONLINE_LOG;
        MobileServerController.getInstance().transitionToOnline();
      }
      break;
    case OFFLINE:
      logAction = TRANSITION_TO_ONLINE_LOG;
      log4j.debug(logAction);
      MobileServerController.getInstance().transitionToOnline();
      break;
    }
    log4j.debug(logAction);
    return logAction;
  }

  private String goOfflineIfPossible(MobileServerState state) {
    // offline servers, let's go offline
    String logAction = null;
    switch (state) {
    case TRANSITION_TO_OFFLINE:
      logAction = "Already transitioning to offline";
      MobileServerController.getInstance().checkStartServerStateLogic();
      break;
    case ONLINE:
      logAction = TRANSITION_TO_OFFLINE_LOG;
      MobileServerController.getInstance().transitionToOffline();
      break;
    case TRANSITION_TO_ONLINE:
      // if transitioning to online then check if the transition process is running
      // if not then go offline, if yes, then we need to wait and will try again
      if (!MobileServerController.getInstance().isTransitioningToOnline()) {
        logAction = TRANSITION_TO_OFFLINE_LOG;
        MobileServerController.getInstance().transitionToOffline();
      } else {
        // do nothing in this case, just wait until the transition to offline is done
        // will automatically retry in due course
        logAction = "Already transitioning to online, wait for next cycle for transition to offline";
      }
      break;
    case OFFLINE:
      // already offline do nothing
      logAction = "Already offline";
      break;
    }
    log4j.info(logAction);
    return logAction;
  }

  public static boolean isCentralServerOffline(MobileServerDefinition thisServerDef) {
    Date lastPingFromCentral = getLastPingFromCentral();
    Date lastPingThreshold = getLastPingThreshold();
    log4j.debug("Checking if the central server is online");
    MobileServerDefinition centralServer = MobileServerController.getInstance().getCentralServer();
    boolean lastPingFromCentralIsExpired = lastPingFromCentral.before(lastPingThreshold);
    boolean shouldGoOffline = lastPingFromCentralIsExpired
        || MobileServerState.OFFLINE.getValue().equals(centralServer.getStatus());
    if (lastPingFromCentralIsExpired) {
      log4j.info("Central server seems to be offline, no ping has been received in a long time");
      log4j.debug("Last ping from central: " + lastPingFromCentral
          + ". If the last ping was done before " + lastPingThreshold
          + " then central server is offline");
      updateServerStatus(centralServer, MobileServerState.OFFLINE.getValue());
      thisServerDef.setOfflineLog("Last ping " + lastPingFromCentral + " before threshold "
          + lastPingThreshold + "");
    } else {
      log4j
          .debug("Central server seems to be online. The last ping from the central server was received on "
              + lastPingFromCentral);
    }
    return shouldGoOffline;
  }

  static Date getLastPingThreshold() {
    int pingPeriodicitySeconds = 300;
    try {
      pingPeriodicitySeconds = Integer.parseInt(Preferences.getPreferenceValue(
          "OBMOBC_Ping_Periodicity", true, null, null, null, null, (String) null));
    } catch (PropertyException e) {
      // the default value of 5 minutes (300 seconds) will be used
    }
    int nPingsLostToGoOffline = 3;
    return new Date(new Date().getTime() - nPingsLostToGoOffline * pingPeriodicitySeconds * 1000);
  }

  static Date getLastPingFromCentral() {
    MobileServerDefinition centralServer = MobileServerController.getInstance().getCentralServer();
    Date lastPing = centralServer.getLastPing();
    if (lastPing == null) {
      lastPing = new Date();
      centralServer.setLastPing(lastPing);
    }
    return lastPing;
  }

  private void pingStoreServersAndUpdateTheirState(ProcessContext processContext,
      MobileServerDefinition thisServerDef, OBError result) throws Exception {
    final List<MobileServerDefinition> servers = getServersToSendPing(thisServerDef);
    final int numberOfServers = servers.size();

    log4j.debug("Found " + servers.size() + " servers ");

    final List<PingRunnable> pingRunnables = new ArrayList<>();
    PingRunnable currentPingRunnable = new PingRunnable();
    currentPingRunnable.setProcessContext(processContext);
    currentPingRunnable.setThisServerDef(thisServerDef);
    currentPingRunnable.setObContext(OBContext.getOBContext());
    pingRunnables.add(currentPingRunnable);

    for (MobileServerDefinition srv : servers) {
      log4j.debug("Server " + srv + " status " + srv.getStatus());
      if (pingShouldBeDoneToServer(srv)) {
        if (currentPingRunnable.getNumberOfAssignedServers() == SERVERS_PER_THREAD) {
          currentPingRunnable = new PingRunnable();
          currentPingRunnable.setProcessContext(processContext);
          currentPingRunnable.setThisServerDef(thisServerDef);
          currentPingRunnable.setObContext(OBContext.getOBContext());
          pingRunnables.add(currentPingRunnable);
        }
        currentPingRunnable.addServer(srv.getId());
      } else {
        // not pingable so assume offline
        updateServerStatus(srv, MobileServerState.OFFLINE.getValue());
      }
    }

    final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat(this.getClass().getSimpleName()).setDaemon(true).build();
    final ExecutorService executorService = Executors.newFixedThreadPool(pingRunnables.size(),
        threadFactory);
    try {
      for (PingRunnable pingRunnable : pingRunnables) {
        executorService.submit(pingRunnable);
      }
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(WAIT_TIME, TimeUnit.MILLISECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt(); // Here!
        executorService.shutdownNow();
        throw new RuntimeException(e);
      }
    } finally {
      executorService.shutdownNow();
    }

    int cntOfflineServers = 0;
    for (PingRunnable pingRunnable : pingRunnables) {
      pingRunnable.setStop(true);
      cntOfflineServers += pingRunnable.getCntOfflineServers();
    }

    result.setMessage("Server count: " + numberOfServers + " - offline server count "
        + cntOfflineServers);
  }

  private static void updateServerStatus(MobileServerDefinition srv, String newStatus) {
    srv.setStatus(newStatus);
    OBDal.getInstance().save(srv);
    OBDal.getInstance().flush();
  }

  public static boolean checkOfflineHandlers(MobileServerDefinition thisServerDef) {
    boolean shouldGoOffline = false;
    final StringBuilder offlineServersLog = new StringBuilder();
    log4j.debug("Checking offline handlers");
    for (CheckServerOfflineHandler handler : WeldUtils
        .getInstances(CheckServerOfflineHandler.class)) {
      log4j.debug("Checking handler " + handler.getClass().getSimpleName());
      if (handler.isServerOffline()) {
        offlineServersLog.append(" " + handler.getClass().getSimpleName());
        shouldGoOffline = true;
      }
      if (handler.isServerOffline()) {
        log4j.info("Handler " + handler.getClass().getSimpleName() + " is offline");
      }
    }
    if (shouldGoOffline) {
      thisServerDef.setOfflineLog("Offline servers/checks: " + offlineServersLog.toString());
    }
    return shouldGoOffline;
  }

  private boolean pingShouldBeDoneToServer(MobileServerDefinition server) {

    for (PingServerChecker pingServerChecker : getPingServerCheckers()) {
      if (!pingServerChecker.pingShouldBeDone(server)) {
        return false;
      }
    }
    return true;
  }

  private Set<PingServerChecker> getPingServerCheckers() {
    if (pingServerCheckers == null) {
      pingServerCheckers = new HashSet<>();
      // PingServerChecker must be injected manually, because this class is a DalBaseProcess, that
      // are not instantiated with Weld
      BeanManager bm = WeldUtils.getStaticInstanceBeanManager();
      final Set<Bean<?>> beans = bm.getBeans(PingServerChecker.class);
      for (Bean<?> bean : beans) {
        PingServerChecker pingServerChecker = (PingServerChecker) bm.getReference(bean,
            PingServerChecker.class, bm.createCreationalContext(bean));
        pingServerCheckers.add(pingServerChecker);
      }
    }
    return pingServerCheckers;
  }

  private List<MobileServerDefinition> getServersToSendPing(MobileServerDefinition thisServerDef) {
    if (!MobileServerController.getInstance().isThisACentralServer()) {
      throw new OBException("This method should only be called in the central server");
    }
    OBQuery<MobileServerDefinition> serversQry = OBDal.getInstance().createQuery(
        MobileServerDefinition.class,
        "client.id=:clientId and " + MobileServerDefinition.PROPERTY_MOBILESERVERKEY + "!='"
            + thisServerDef.getMobileServerKey() + "' and "
            + MobileServerDefinition.PROPERTY_SERVERTYPE + "='" + MobileServerUtils.STORE_SERVER
            + "'" + " order by " + MobileServerDefinition.PROPERTY_PRIORITY);
    serversQry.setFilterOnReadableClients(false);
    serversQry.setFilterOnReadableOrganization(false);
    serversQry.setNamedParameter("clientId", MobileServerController.getInstance()
        .getThisServerClientId());
    return serversQry.list();
  }

  private class PingRunnable implements Runnable {
    private static final String SERVER_STATUS_PROPERTY_NAME = "serverStatus";
    private List<String> serverIds = new ArrayList<>();
    private ProcessContext processContext = null;
    private OBContext obContext = null;

    public void setObContext(OBContext obContext) {
      this.obContext = obContext;
    }

    private MobileServerDefinition thisServerDef;
    private int cntOfflineServers = 0;
    private boolean stop = false;

    public void setStop(boolean stop) {
      this.stop = stop;
    }

    public int getCntOfflineServers() {
      return cntOfflineServers;
    }

    public void setProcessContext(ProcessContext processContext) {
      this.processContext = processContext;
    }

    public void setThisServerDef(MobileServerDefinition thisServerDef) {
      this.thisServerDef = thisServerDef;
    }

    public void addServer(String serverId) {
      serverIds.add(serverId);
    }

    public int getNumberOfAssignedServers() {
      return serverIds.size();
    }

    @Override
    public void run() {
      try {
        OBContext.setOBContext(obContext);
        OBContext.setAdminMode();
        final JSONObject parameters = new JSONObject();
        parameters.put("organization", processContext.getOrganization());
        parameters.put("client", processContext.getClient());
        parameters.put("mobileServerKey", thisServerDef.getMobileServerKey());
        parameters.put(SERVER_STATUS_PROPERTY_NAME, thisServerDef.getStatus());

        final String queryParams = MobileServerUtils.getAuthenticationQueryParams(
            processContext.getClient(), processContext.getOrganization(), processContext.getRole(),
            processContext.getUser())
            + "&"
            + MobileAuthenticationKeyUtils.USE_USER_CACHE_PARAMETER
            + "=true";

        OBDal.getInstance().commitAndClose();

        for (String serverId : serverIds) {
          if (stop) {
            return;
          }
          pingServer(serverId, parameters, queryParams);
        }
        OBDal.getInstance().commitAndClose();
      } catch (JSONException e) {
        throw new OBException(e);
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    private void pingServer(String serverId, final JSONObject parameters, final String queryParams) {
      MobileServerDefinition srv = OBDal.getInstance().get(MobileServerDefinition.class, serverId);
      String newStatus = srv.getStatus();
      JSONObject resp = null;
      try {
        resp = new JSONObject(MobileServerRequestExecutor.getInstance().request(srv,
            MobileServerUtils.OBWSPATH + MobileServerStatusInformation.class.getName(),
            queryParams, "POST", parameters)).getJSONObject("response");
        newStatus = getStatusFromResponse(resp);
      } catch (Exception e) {
        log4j.debug(e.getMessage(), e);
        newStatus = MobileServerState.OFFLINE.getValue();
      }
      if (MobileServerState.OFFLINE.getValue().equals(newStatus)) {
        cntOfflineServers++;
      }
      if (!newStatus.equals(srv.getStatus())) {
        try {
          updateServerStatus(srv, newStatus);
          OBDal.getInstance().commitAndClose();
        } catch (Throwable logIt) {
          log4j.error("Error when pinging server " + srv.getMobileServerKey(), logIt);
          OBDal.getInstance().rollbackAndClose();
        }
      }
    }

    private String getStatusFromResponse(JSONObject response) throws JSONException {
      String newStatus = null;
      if (response != null) {
        if (response.has(SERVER_STATUS_PROPERTY_NAME)) {
          log4j.debug("Response has server status " + response.get(SERVER_STATUS_PROPERTY_NAME));
          newStatus = response.getString(SERVER_STATUS_PROPERTY_NAME);
        } else {
          newStatus = MobileServerState.ONLINE.getValue();
        }
      } else {
        log4j.debug("Resp == null --> offline");
        newStatus = MobileServerState.OFFLINE.getValue();
      }
      return newStatus;
    }
  }
}
