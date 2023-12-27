/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import java.io.StringWriter;
import java.io.Writer;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.process.JSONProcessSimple;
import org.openbravo.mobile.core.process.MobileImportEntryProcessorRunnable;
import org.openbravo.mobile.core.process.MobileServiceProcessor;
import org.openbravo.service.importprocess.ImportEntry;
import org.openbravo.service.importprocess.ImportEntryManager.ImportEntryQualifier;
import org.openbravo.service.importprocess.ImportEntryProcessor;
import org.openbravo.service.json.JsonConstants;

/**
 * Handler which receives a json which calls other json processes in order and does one commit at
 * the end. Exceptions are returned directly to the client.
 * 
 * The class has a main flow logic which calls central server or processes locally depending on
 * being online or offline or being store or central server.
 * 
 * For more information see these wiki pages:
 * http://wiki.openbravo.com/wiki/Multi-Server_Process_Calls_Concept
 * http://wiki.openbravo.com/wiki/Retail:Store_Server#Synchronized_Transactions
 * 
 * @author mtaal
 */
public class SynchronizedServerProcessCaller extends MultiServerJSONProcess {

  public static final String SYNCHRONIZED_DATA_TYPE = "OBMOBC_SynchronizedData";

  @Override
  protected String getImportEntryDataType() {
    return SYNCHRONIZED_DATA_TYPE;
  }

  protected JSONObject execute(JSONObject jsonSent) {
    try {
      final MobileServiceProcessor processor = new MobileServiceProcessor();
      processor.setSynchronizedMode(true);

      final Writer w = new StringWriter();
      w.write("{\"result\":[");
      final JSONArray data = jsonSent.getJSONArray("data");
      for (int i = 0; i < data.length(); i++) {
        final JSONObject content = data.getJSONObject(i);
        final String serviceName = content.getString("_serviceName");
        if (i > 0) {
          w.write(",");
        }

        final StringWriter localWriter = new StringWriter();
        localWriter.write("{");
        localWriter.write("\"_serviceName\": \"" + serviceName + "\",");
        processor.execServiceName(localWriter, serviceName, content);

        localWriter.write("}");
        final JSONObject checkResult = new JSONObject(localWriter.toString());
        // if error then return it
        if (isErrorJson(checkResult)) {
          return checkResult;
        }
        w.write(localWriter.toString());
      }
      w.write("]");
      w.write("}");
      w.close();

      final JSONObject result = new JSONObject(w.toString());
      // important to put the same message id in the return!
      // this so that symmetric ds can detect that the message
      // has already been replicated/created
      result.put("messageId", jsonSent.getString("messageId"));
      // note: this is mobile core, posterminal is a strange one
      // to have here, to solve one day
      if (jsonSent.has("posTerminal")) {
        result.put("posTerminal", jsonSent.getString("posTerminal"));
      }
      result.put("source",
          (MobileServerController.getInstance().isThisACentralServer() ? SOURCE_CENTRAL
              : SOURCE_STORE));
      result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);

      result.put("_handler", this.getClass().getSimpleName());

      return result;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  @ImportEntryQualifier(entity = SYNCHRONIZED_DATA_TYPE)
  @ApplicationScoped
  public static class SynchronizedProcessImportEntryProcessor extends ImportEntryProcessor {

    protected ImportEntryProcessRunnable createImportEntryProcessRunnable() {
      return WeldUtils.getInstanceFromStaticBeanManager(SynchronizedProcessRunnable.class);
    }

    protected boolean canHandleImportEntry(ImportEntry importEntryInformation) {
      return SYNCHRONIZED_DATA_TYPE.equals(importEntryInformation.getTypeofdata());
    }

    protected String getProcessSelectionKey(ImportEntry importEntry) {
      return importEntry.getOrganization().getId();
    }
  }

  private static class SynchronizedProcessRunnable extends MobileImportEntryProcessorRunnable {
    protected Class<? extends JSONProcessSimple> getJSONProcessorClass() {
      return SynchronizedServerProcessCaller.class;
    }

    protected void processEntry(ImportEntry importEntry) throws Exception {
      // check that there are no other import entries for the terminal
      // which have not yet been processed

      try {
        OBContext.setAdminMode(false);
        if (thereIsDataInImportQueue(importEntry)) {
          // close and commit
          OBDal.getInstance().commitAndClose();
          return;
        }

      } finally {
        OBContext.restorePreviousMode();
      }
      super.processEntry(importEntry);
    }

    private boolean thereIsDataInImportQueue(ImportEntry importEntry) {
      try {
        OBContext.setAdminMode(false);

        if (0 < countEntries("Error", importEntry)) {
          // if there are related error entries before this one then this is an error
          // throw an exception to move this entry also to error status
          throw new OBException("There are error records before this record " + importEntry
              + ", moving this entry also to error status.");
        }

        return 0 < countEntries("Initial", importEntry);
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    private int countEntries(String importStatus, ImportEntry importEntry) {
      final String whereClause = ImportEntry.PROPERTY_IMPORTSTATUS + "='" + importStatus + "' and "
          + ImportEntry.PROPERTY_TYPEOFDATA + "='OBMOBC_SynchronizedData' and "
          + ImportEntry.PROPERTY_CREATIONDATE + "<:creationDate and "
          + ImportEntry.PROPERTY_ORGANIZATION + "=:org and id!=:id";
      final Query qry = OBDal.getInstance().getSession()
          .createQuery("select count(*) from " + ImportEntry.ENTITY_NAME + " where " + whereClause);
      qry.setParameter("id", importEntry.getId());
      qry.setTimestamp("creationDate", importEntry.getCreationDate());
      qry.setParameter("org", importEntry.getOrganization());

      return ((Number) qry.uniqueResult()).intValue();
    }
  }

}
