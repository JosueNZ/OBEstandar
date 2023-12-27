package com.sidesoft.localization.ecuador.withholdings.ad_process;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.localization.ecuador.withholdings.SswhWithholdingsVoided;

public class Sswh_ProcessWithholdingVoided extends DalBaseProcess {
  OBError message;

  @Override
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
    String StrWithholdingID = (String) bundle.getParams().get("Sswh_Withholdings_Voided_ID");
    SswhWithholdingsVoided OBWithholding = OBDal.getInstance().get(SswhWithholdingsVoided.class,
        StrWithholdingID);

    String V_Processed = OBWithholding.get(SswhWithholdingsVoided.PROPERTY_PROCESSED).toString();

    if (V_Processed.equals("N")) {
      OBWithholding.set(SswhWithholdingsVoided.PROPERTY_PROCESSED, "Y");
      OBDal.getInstance().save(OBWithholding);
      OBDal.getInstance().flush();

    } else {
      if (V_Processed.equals("Y")) {

        OBWithholding.set(SswhWithholdingsVoided.PROPERTY_PROCESSED, "N");
        OBDal.getInstance().save(OBWithholding);
        OBDal.getInstance().flush();

      }
    }

  }
}
