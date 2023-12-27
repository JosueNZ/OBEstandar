/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.MobileServerDefinition;

/**
 * This class implements the thread which checks if the main servers and the external servers are
 * back online and will start the transitioning back to online if so. It is called when transition
 * to offline starts and sets the server to offline after a specific offline wait time. The offline
 * wait time is controlled by a preference.
 * 
 * This thread will check the status of the server until: a) All the required servers (external
 * servers for the central server, and external servers and central server for store servers) are
 * online or b) Enough time has passed to consider that the server status needs to be updated to
 * OFFLINE
 * 
 * @author mdejuana
 */
public class MobileMainServerCheck implements Runnable {

  private static final Logger log = Logger.getLogger(MobileMainServerCheck.class);
  private static boolean threadIsRunning = false;
  private static final int TEN_SECONDS = 10000;
  private static final int ONE_MINUTE = 60000;
  private static final int FIVE_MINUTES = 5 * ONE_MINUTE;
  private static final int TEN_MINUTES = 10 * ONE_MINUTE;
  private static final int THIRTY_MINUTES = 30 * ONE_MINUTE;

  public static boolean isThreadRunning() {
    return threadIsRunning;
  }

  private static void setThreadIsRunning(boolean isRunning) {
    threadIsRunning = isRunning;
  }

  private OBContext obContext;
  private long startTime = System.currentTimeMillis();
  private long offlineWaitTime = -1;
  private long intervalTime = 10000;

  public MobileMainServerCheck(OBContext context) {
    this.obContext = context;
    offlineWaitTime = MobileServerUtils.getIntegerPreference("OBMOBC_TransitionToOfflineWait",
        FIVE_MINUTES);
    if (offlineWaitTime > TEN_MINUTES) {
      log.warn("A very long transition to offline time (" + offlineWaitTime
          + ") has been set, ignoring it, using default of 5 mins");
      offlineWaitTime = FIVE_MINUTES;
    }

    intervalTime = MobileServerUtils.getIntegerPreference("OBMOBC_RequestRouterPingTime",
        ONE_MINUTE);
    if (intervalTime < TEN_SECONDS || intervalTime > THIRTY_MINUTES) {
      log.warn("The specified interval time is outside reasonable limits " + intervalTime
          + " ignoring it and falling back to sensible default (1 minute)");
      // check somewhat arbitrary numbers
      intervalTime = ONE_MINUTE;
    }
  }

  public void run() {
    // already running bail out
    if (isThreadRunning()) {
      log.debug("MobileMainServerCheck thread is already running");
      return;
    }
    setThreadIsRunning(true);
    try {
      log.debug("Started main and external server check");
      OBContext.setOBContext(obContext);
      OBContext.setAdminMode(false);

      // the mobile servers will check of they can go online:
      // - the central server will check the offline handlers
      // - mobile servers will also check the status of the central server
      MobileServerDefinition thisServerDef = MobileServerController.getInstance()
          .getThisServerDefinition();
      // keep transitioning to offline until all offline checks are passed or until
      // the process has been transitioning to offline longer than OBMOBC_TransitionToOfflineWait
      boolean keepTransitioningToOffline = true;
      while (keepTransitioningToOffline) {
        boolean shouldGoOffline = executeOfflineChecks(thisServerDef);

        // save any status updates
        OBDal.getInstance().commitAndClose();

        if (shouldGoOffline) {
          // if we have waited/tried for the offline wait time period then move ourselves
          // to offline
          if (transitionToOfflineIntervalHasPassed(thisServerDef)) {
            goOffline();
            // break out of the loop, there is a separate process request which will check if the
            // server can go back online, so not needed to continue in this loop
            keepTransitioningToOffline = false;
          } else {
            log.debug("Some offline checks have failed. Transition to offline will continue for "
                + ((startTime + offlineWaitTime) - System.currentTimeMillis()) + " milliseconds");
          }
          waitBeforeNextAttempt();
        } else {
          // all main and external servers are back online
          transitionToOnlineIfNeeded(thisServerDef);
          keepTransitioningToOffline = false;
        }
      }
      OBDal.getInstance().commitAndClose();
    } catch (Throwable t) {
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(t);
    } finally {
      OBContext.restorePreviousMode();
      setThreadIsRunning(false);
    }
  }

  private void goOffline() {
    log.info("Setting status to offline");
    MobileServerController.getInstance().setMobileServerState(MobileServerState.OFFLINE);
    OBDal.getInstance().commitAndClose();
  }

  private void transitionToOnlineIfNeeded(MobileServerDefinition thisServerDef) {
    // already online
    if (MobileServerState.ONLINE.getValue().equals(thisServerDef.getStatus())) {
      log.debug("Server is back online already, returning");
    } else {
      log.info("Transitioning to online");
      MobileServerController.getInstance().transitionToOnline();
    }
  }

  private void waitBeforeNextAttempt() {
    try {
      log.info("Waiting " + intervalTime + " milliseconds before rechecking");
      Thread.sleep(intervalTime);
    } catch (InterruptedException e) {
      log.error("MobileMainServerCheck interruption when sleeping", e);
      Thread.currentThread().interrupt(); // Here!
      throw new RuntimeException(e);
    }
  }

  private boolean transitionToOfflineIntervalHasPassed(MobileServerDefinition thisServerDef) {
    return System.currentTimeMillis() > (startTime + offlineWaitTime)
        && !MobileServerState.OFFLINE.getValue().equals(thisServerDef.getStatus());
  }

  private boolean executeOfflineChecks(MobileServerDefinition thisServerDef) {
    boolean thereAreOfflineExternalServers = ServerStateBackground
        .checkOfflineHandlers(thisServerDef);

    boolean shouldGoOffline = thereAreOfflineExternalServers;

    if (MobileServerController.getInstance().isThisAStoreServer()) {
      boolean centralServerIsOffline = ServerStateBackground.isCentralServerOffline(thisServerDef);
      shouldGoOffline |= centralServerIsOffline;
    }
    return shouldGoOffline;
  }
}
