/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.servercontroller.MobileServerController;
import org.openbravo.mobile.core.servercontroller.MobileServerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sets to null the last ping column for the central server row in the OBMOBC_SERVER_DEFINITION
 * table.
 * 
 * This is needed to prevent detecting the central server as offline when it really isn't. For
 * instance, if a store server has been offline for some time then during that time it cannot
 * receive pings from the central server. If the store server starts and the ping is not reset, it
 * might happen that the old ping is checked before the central server has the change to send a new
 * ping, and the store server would consider the central server offline because the last ping was
 * received a long time ago
 */
public class InitializeLastPingFromCentralServer implements ServletContextListener {

  private static final Logger log = LoggerFactory
      .getLogger(InitializeLastPingFromCentralServer.class);

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    try {
      OBContext.setAdminMode(false);
      if (!MobileServerUtils.isMultiServerEnabled()
          || MobileServerController.getInstance().isThisACentralServer()) {
        return;
      }
      resetLastPingFromCentralServer();
    } catch (Exception e) {
      log.error("Error when trying to reset the last ping received from the central server", e);
      OBDal.getInstance().rollbackAndClose();
    } finally {
      OBDal.getInstance().commitAndClose();
      OBContext.restorePreviousMode();
    }
  }

  private void resetLastPingFromCentralServer() {
    StringBuilder hqlBuilder = new StringBuilder();
    hqlBuilder.append(" UPDATE OBMOBC_SERVER_DEFINITION ");
    hqlBuilder.append(" SET last_ping = null ");
    hqlBuilder.append(" WHERE serverType = 'MAIN' ");
    final Query query = OBDal.getInstance().getSession().createQuery(hqlBuilder.toString());
    int nUpdates = query.executeUpdate();
    if (nUpdates == 0) {
      log.info("There is no central server defined in obmobc_server_definition, nothing to update");
    } else {
      log.info("Central server last ping reset properly");
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent arg0) {
    // nothing to clean
  }

}
