/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.log4j.Logger;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryManager;
import org.openbravo.service.importprocess.ImportEntryManager.ImportEntryQualifier;
import org.openbravo.service.importprocess.ImportEntryProcessor;

/**
 * Is called to process a message received on the server. This class will a
 * {@link MobileServerMessageHandler} for each message.
 */
@ImportEntryQualifier(entity = "OBMOBC_ServerMessage")
@ApplicationScoped
public class MobileServerMessageProcessor extends ImportEntryProcessor {

  protected ImportEntryProcessRunnable createImportEntryProcessRunnable() {
    return WeldUtils.getInstanceFromStaticBeanManager(MobileServerMessageRunnable.class);
  }

  protected boolean canHandleImportEntry(ImportEntry importEntryInformation) {
    return "OBMOBC_ServerMessage".equals(importEntryInformation.getTypeofdata());
  }

  // process all messages in one thread
  protected String getProcessSelectionKey(ImportEntry importEntry) {
    return "1";
  }

  private static class MobileServerMessageRunnable extends ImportEntryProcessRunnable {

    protected void processEntry(ImportEntry importEntry) throws Exception {
      final Logger logger = Logger.getLogger(this.getClass());
      logger.debug("Processing message " + importEntry.getJsonInfo());

      final BeanManager beanManager = WeldUtils.getStaticInstanceBeanManager();
      final Set<Bean<?>> beans = beanManager.getBeans(MobileServerMessageHandler.class);
      final List<MobileServerMessageHandler> handlers = new ArrayList<MobileServerMessageHandler>();
      for (Bean<?> bean : beans) {
        handlers.add((MobileServerMessageHandler) beanManager.getReference(bean,
            MobileServerMessageHandler.class, beanManager.createCreationalContext(bean)));
      }
      Collections.sort(handlers, new Comparator<MobileServerMessageHandler>() {
        @Override
        public int compare(MobileServerMessageHandler o1, MobileServerMessageHandler o2) {
          return o1.getPriority() - o2.getPriority();
        }
      });
      for (MobileServerMessageHandler handler : handlers) {
        logger.debug("Trying " + handler.getClass().getName() + " to process message ");
        if (handler.processMessage(importEntry.getOrganization(), importEntry.getJsonInfo())) {
          try {
            OBContext.setAdminMode(false);
            logger.debug("Message processed");
            ImportEntryManager.getInstance().setImportEntryProcessed(importEntry.getId());
            OBDal.getInstance().commitAndClose();
          } finally {
            OBContext.restorePreviousMode();
          }
          return;
        }
      }
      OBDal.getInstance().commitAndClose();
    }
  }
}
