/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.process;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.mobile.core.MobileCoreKernelUtils;
import org.openbravo.service.importprocess.ImportEntryManager;
import org.openbravo.service.importprocess.ImportProcessContextListener;

/**
 * Initializes the import process layer by calling {@link ImportEntryManager#start()}. The
 * stop/shutdown is called from the {@link ImportProcessContextListener} and not from this class.
 * 
 * @author mtaal
 */
public class POSTerminalContextListener implements ServletContextListener {

  private ImportEntryManager importEntryManager;

  public void contextInitialized(ServletContextEvent event) {
    final MobileCoreKernelUtils mobileCoreKernelUtils = new MobileCoreKernelUtils();
    KernelUtils.setInstance(mobileCoreKernelUtils);
    importEntryManager = WeldUtils.getInstanceFromStaticBeanManager(ImportEntryManager.class);
    importEntryManager.start();
  }

  public void contextDestroyed(ServletContextEvent event) {
  }
}
