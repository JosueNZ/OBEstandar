/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.listener;

import java.sql.Timestamp;
import java.util.Date;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.hibernate.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.mobile.core.servercontroller.MobileServerController;
import org.openbravo.mobile.core.servercontroller.MobileServerUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context listener in charge of scheduling one instance of the ServerStateBackground, using the
 * OBMOBC_Ping_Periodicity preference to determine how often it should be invoked. All other process
 * requests that reference the ServerStateBackground process are unscheduled
 */
public class StatusBackgroundProcessScheduler implements ServletContextListener {
  private static final Logger log = LoggerFactory.getLogger(StatusBackgroundProcessScheduler.class);
  private static final String SERVER_STATE_BACKGROUND_PROCESS_ID = "A8D3265435F94CCFB3AF14437E5FAD1F";
  private static final String PROCESS_REQUEST_ID = "9067AECD3B044552BDFFA0CF9056B2C9";
  private static final String SECONDS = "1";

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try {
      OBContext.setAdminMode(false);

      if (!MobileServerUtils.isMultiServerEnabled()) {
        return;
      }

      String serverClientId = null;
      try {
        // if the mobile.server.key is not defined it will return null
        // if it is defined but with a wrong value it will throw an exception, catch it and stop
        // tomcat
        serverClientId = MobileServerController.getInstance().getThisServerClientId();
      } catch (Exception e) {
        log.error(
            "Error while trying to fetch the definition of the current server " + e.getMessage(), e);
        // print to more output streams, to just give the most output as it is quite aggressive to
        // do system.exit
        e.printStackTrace(System.err);
        e.printStackTrace(System.out);
        System.exit(-1);

      }
      if (serverClientId == null) {
        // no need to do anything if the current server is not a central or a store server
        return;
      }
      unscheduleOtherStatusBackgroundProcesses();
      scheduleStatusBackgroundProcess(OBDal.getInstance().get(Client.class, serverClientId));
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
  }

  private void unscheduleOtherStatusBackgroundProcesses() {
    StringBuilder hqlBuilder = new StringBuilder();
    hqlBuilder.append(" UPDATE ProcessRequest ");
    hqlBuilder.append(" SET status = :status ");
    hqlBuilder.append(" WHERE process.id = :processId ");
    hqlBuilder.append(" AND id <> :processRequestId ");
    final Query query = OBDal.getInstance().getSession().createQuery(hqlBuilder.toString());
    query.setString("status", Process.UNSCHEDULED);
    query.setString("processId", SERVER_STATE_BACKGROUND_PROCESS_ID);
    query.setString("processRequestId", PROCESS_REQUEST_ID);
    query.executeUpdate();
  }

  private void scheduleStatusBackgroundProcess(Client client) {
    ProcessRequest processRequest = OBDal.getInstance().get(ProcessRequest.class,
        PROCESS_REQUEST_ID);
    // if the process request does not exist create it, if it does just updated it
    if (processRequest == null) {
      processRequest = OBProvider.getInstance().get(ProcessRequest.class);
      processRequest.setNewOBObject(true);
      processRequest.setId(PROCESS_REQUEST_ID);
    }
    configureProcessRequest(processRequest, client);
    OBDal.getInstance().save(processRequest);
  }

  private void configureProcessRequest(ProcessRequest processRequest, Client client) {
    processRequest.setProcess(OBDal.getInstance().getProxy(org.openbravo.model.ad.ui.Process.class,
        SERVER_STATE_BACKGROUND_PROCESS_ID));
    processRequest.setTiming("S");
    processRequest.setClient(client);
    processRequest.setStatus(Process.SCHEDULED);
    Date now = new Date();
    processRequest.setStartDate(now);
    processRequest.setStartTime(new Timestamp(now.getTime()));
    processRequest.setFrequency(SECONDS);
    processRequest.setIntervalInSeconds(new Long(getPingInterval()));
    processRequest.setOpenbravoContext(getProcessContext(client));
  }

  private String getProcessContext(Client client) {
    VariablesSecureApp vars = new VariablesSecureApp("0", client.getId(), "0", "0");
    ProcessContext context = new ProcessContext(vars);
    return context.toString();
  }

  private int getPingInterval() {
    int pingPeriodicitySeconds = 300;
    try {
      pingPeriodicitySeconds = Integer.parseInt(Preferences.getPreferenceValue(
          "OBMOBC_Ping_Periodicity", true, "0", "0", "100", null, (String) null));
    } catch (PropertyException e) {
      // the default value of 5 minutes (300 seconds) will be used
    }
    return pingPeriodicitySeconds;
  }

  @Override
  public void contextDestroyed(ServletContextEvent arg0) {
  }

}
