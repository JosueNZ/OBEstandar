/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import java.util.HashMap;
import java.util.Map;

import org.jfree.util.Log;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.MobileServerDefinition;

/**
 * This is a separate class which updates the server status in a separate thread to prevent locking
 * issues of doing it in the main request thread. The reason that locking can occur is that the
 * status is updated at every request received on a server. So the MobileServerDefinition table
 * maybe accessed/updated continuously. Therefore a separate class which does this in a separate
 * thread.
 * 
 * @author mtaal
 */
public class MobileServerStatusUpdateHandler {

  private static MobileServerStatusUpdateHandler instance = new MobileServerStatusUpdateHandler();

  public static MobileServerStatusUpdateHandler getInstance() {
    return instance;
  }

  public static void setInstance(MobileServerStatusUpdateHandler instance) {
    MobileServerStatusUpdateHandler.instance = instance;
  }

  private boolean threadIsRunning = false;

  private Map<String, String> statuses = new HashMap<String, String>();

  public synchronized void setServerStatus(String serverKey, String serverStatus) {
    statuses.put(serverKey, serverStatus);
    if (!threadIsRunning) {
      startThread();
    }
  }

  private synchronized void startThread() {
    final Map<String, String> tmpStatuses = statuses;
    statuses = new HashMap<String, String>();
    threadIsRunning = true;
    final StatusUpdateRunnable runnable = new StatusUpdateRunnable(tmpStatuses, this);
    final Thread thrd = new Thread(runnable);
    // daemon ensures that the threads does not block exiting the jvm
    thrd.setDaemon(true);
    thrd.setName("StatusUpdateThread");
    thrd.start();
  }

  private synchronized void threadFinished() {
    threadIsRunning = false;
    if (statuses.size() > 0) {
      startThread();
    }
  }

  private static class StatusUpdateRunnable implements Runnable {

    private Map<String, String> handleStatuses;
    private MobileServerStatusUpdateHandler handler;

    StatusUpdateRunnable(Map<String, String> handleStatuses, MobileServerStatusUpdateHandler handler) {
      this.handleStatuses = handleStatuses;
      this.handler = handler;
    }

    public void run() {
      try {
        OBContext.setAdminMode(false);
        for (String key : handleStatuses.keySet()) {
          try {
            final String status = handleStatuses.get(key);
            final MobileServerDefinition server = MobileServerController.getInstance()
                .getMobileServer(key);
            if (server != null && !status.equals(server.getStatus())) {
              server.setStatus(status);
              OBDal.getInstance().save(server);
              OBDal.getInstance().flush();
            }
          } catch (Exception logIt) {
            // only log it, maybe other statuses can be updated
            Log.error(logIt.getMessage(), logIt);
          }
        }
        OBDal.getInstance().commitAndClose();
      } finally {
        handler.threadFinished();
        OBContext.restorePreviousMode();
      }
    }
  }
}
