package com.sidesoft.ecuador.asset.allocation.ad_process;

import java.util.Calendar;
import java.util.Date;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.ecuador.asset.allocation.SsalApplActive;
import com.sidesoft.ecuador.asset.allocation.SsalAssetReturn;

public class ReturnAssetsStore extends DalBaseProcess {
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

    String return_assets_id = (String) bundle.getParams().get("Ssal_Asset_Return_ID");
    SsalAssetReturn return_assets = OBDal.getInstance()
        .get(SsalAssetReturn.class, return_assets_id);

    /*String fixed_asset_id = return_assets.getSsalApplActive().getId();
    SsalApplActive fixed_asset = OBDal.getInstance().get(SsalApplActive.class, fixed_asset_id);

    String a_asset_id = fixed_asset.getAsset().getId();
    Asset a_asset = OBDal.getInstance().get(Asset.class, a_asset_id);

    a_asset.setSsalIsavailable(true);

    Calendar calendar = Calendar.getInstance();

    fixed_asset.setDateReturn(calendar.getTime());
    fixed_asset.setMotiveReturn(return_assets.getDescription());
    fixed_asset.setDoctypeIdReturn(return_assets.getDocumentType());
    fixed_asset.setDocumentnoReturn(return_assets.getDocumentNo());
    fixed_asset.setEnddate(return_assets.getEnddate());

    fixed_asset.setReturn(true);

    return_assets.setLoadAssets(true);

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);
    // String successMessage = null;

    message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
    message.setType("Success");
    message.setMessage(Utility.messageBD(conn, "Request Processed Successfully", language));
    OBDal.getInstance().save(a_asset);
    OBDal.getInstance().flush();*/

  }
}
