/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.listener;

import java.util.Properties;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.MobileServerDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set the current mobile server in the database based in the openbravo.properties configuration
 *
 */

public class SetCurrentMobileServerListener implements ServletContextListener {

  private static final Logger log = LoggerFactory.getLogger(SetCurrentMobileServerListener.class);
  private static final String MOBILE_SERVER_KEY = "mobile.server.key";

  /**
   * Reads the Openbravo.properties file and set the current mobile server in the database.
   * 
   * @param sce
   *          ServletContextEvent
   */

  public void contextInitialized(ServletContextEvent sce) {
    try {
      resetIsCurrentFlag();
      Properties properties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
      String serverKey = properties.getProperty(MOBILE_SERVER_KEY);
      if (serverKey != null) {
        OBContext.setAdminMode(false);
        MobileServerDefinition currentMobileServer = getCurrentServer(serverKey);
        try {
          if (currentMobileServer != null) {
            currentMobileServer.setCurrent(true);
          }
        } finally {
          OBContext.restorePreviousMode();
        }
      }
      OBDal.getInstance().commitAndClose();
    } catch (Throwable t) {
      log.error("Error while set current mobile server listener", t);
      OBDal.getInstance().rollbackAndClose();
    }
  }

  private void resetIsCurrentFlag() {
    final String hql = "update OBMOBC_SERVER_DEFINITION e  set e.iscurrent='N' ";
    final Query query = OBDal.getInstance().getSession().createQuery(hql);
    query.executeUpdate();
  }

  private MobileServerDefinition getCurrentServer(String serverKey) {
    final OBCriteria<MobileServerDefinition> mobileServerQuery = OBDal.getInstance()
        .createCriteria(MobileServerDefinition.class);
    mobileServerQuery.setFilterOnReadableClients(false);
    mobileServerQuery.setFilterOnReadableOrganization(false);
    mobileServerQuery.add(Restrictions.eq(MobileServerDefinition.PROPERTY_MOBILESERVERKEY,
        serverKey));
    MobileServerDefinition currentMobileServer = (MobileServerDefinition) mobileServerQuery
        .uniqueResult();
    return currentMobileServer;
  }

  /**
   * This method is invoked when the server is shot down, it deactivates all sessions in this
   * context.
   */
  @Override
  public void contextDestroyed(ServletContextEvent arg0) {

  }

}
