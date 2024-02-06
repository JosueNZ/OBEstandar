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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Set the current mobile server in the database based in the openbravo.properties configuration
 *
 */

public class SetErrorImportEntryArchiveListener implements ServletContextListener {

  private static final Logger log = LoggerFactory
      .getLogger(SetErrorImportEntryArchiveListener.class);

  /**
   * Sets to Error the Import Entries that where being processing when the server went down
   * 
   * @param sce
   *          ServletContextEvent
   */

  public void contextInitialized(ServletContextEvent sce) {
    try {
      OBContext.setAdminMode(false);
      try {
        final String hql = "update C_Import_Entry_Archive cia SET cia.importStatus='Error', cia.errorinfo='Server restarted while Import Entry was being executed' WHERE cia.typeofdata='OBMOBC_SynchronizedData' AND cia.importStatus='Initial'";
        final Query query = OBDal.getInstance().getSession().createQuery(hql);
        query.executeUpdate();
      } finally {
        OBContext.restorePreviousMode();
      }
      OBDal.getInstance().commitAndClose();
    } catch (Throwable t) {
      log.error("Error while setting Import Entry Archive from Inital to Error", t);
      OBDal.getInstance().rollbackAndClose();
    }
  }

  /**
   * This method is invoked when the server is shot down, it deactivates all sessions in this
   * context.
   */
  @Override
  public void contextDestroyed(ServletContextEvent arg0) {

  }

}
