/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.process;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryManager.ImportEntryProcessorSelector;
import org.openbravo.service.importprocess.ImportEntryPostProcessor;
import org.openbravo.service.importprocess.ImportEntryProcessor.ImportEntryProcessRunnable;
import org.openbravo.service.json.JsonConstants;

/**
 * Mobile specific import entry processing thread.
 * 
 * @author mtaal
 * 
 */
public abstract class MobileImportEntryProcessorRunnable extends ImportEntryProcessRunnable {

  @Inject
  @Any
  private Instance<ImportEntryPostProcessor> importEntryPostProcessors;

  protected void processEntry(ImportEntry importEntry) throws Exception {
    OBContext.setAdminMode(false);
    String importEntryId = null;
    String json = null;
    try {
      importEntryId = importEntry.getId();
      json = importEntry.getJsonInfo();
    } finally {
      OBContext.restorePreviousMode();
    }
    final JSONProcessSimple jsonProcess = WeldUtils
        .getInstanceFromStaticBeanManager(getJSONProcessorClass());
    jsonProcess.setImportEntryId(importEntryId);
    JSONObject result = jsonProcess.exec(new JSONObject(json));
    if (SessionHandler.isSessionHandlerPresent()) {
      OBDal.getInstance().commitAndClose();
    }
    if (result == null
        || result.get(JsonConstants.RESPONSE_STATUS)
            .equals(JsonConstants.RPCREQUEST_STATUS_SUCCESS)) {
      // Execute post process hooks.
      for (ImportEntryPostProcessor importEntryPostProcessor : importEntryPostProcessors
          .select(new ImportEntryProcessorSelector(importEntry.getTypeofdata()))) {
        importEntryPostProcessor.afterProcessing(importEntry);
      }
    }
  }

  /**
   * This method is in effect deprecated, you should override the {#getJSONProcessorClass()} method.
   */
  protected Class<? extends DataSynchronizationProcess> getDataSynchronizationClass() {
    throw new OBException("This method or the getJSONProcessorClass method should be overridden");
  }

  /**
   * Override this method, for backward compatibility this method calls the
   * {@link #getDataSynchronizationClass()} method.
   */
  protected Class<? extends JSONProcessSimple> getJSONProcessorClass() {
    return getDataSynchronizationClass();
  }
}
