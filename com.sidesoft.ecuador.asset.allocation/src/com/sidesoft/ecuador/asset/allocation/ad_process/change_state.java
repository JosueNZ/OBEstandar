package com.sidesoft.ecuador.asset.allocation.ad_process;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.ecuador.asset.allocation.SsalActiveMain;

public class change_state extends DalBaseProcess {
  OBError message;

  protected void doExecute(ProcessBundle bundle) throws Exception {
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {
      message = new OBError();
      processRequest(bundle);
    } catch (Exception e) {
      message.setTitle(Utility.messageBD(conn, "Error", language));
      message.setType("Error");
      message.setMessage(e.getMessage());
    } finally {
      bundle.setResult(message);
    }
    // Y process, N unprocess Status
  }

  private void processRequest(ProcessBundle bundle) {

    String fixed_asset_id = (String) bundle.getParams().get("Ssal_Active_Main_ID");
    /*
     * SsalApplActive fixed_asset = OBDal.getInstance().get(SsalApplActive.class, fixed_asset_id);
     */
    SsalActiveMain fixed_asset = OBDal.getInstance().get(SsalActiveMain.class, fixed_asset_id);

    fixed_asset.setState("2");

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);
    // String successMessage = null;

    message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
    message.setType("Success");
    message.setMessage(Utility.messageBD(conn, "Request Processed Successfully", language));
    OBDal.getInstance().save(fixed_asset);
    OBDal.getInstance().flush();

  }
}