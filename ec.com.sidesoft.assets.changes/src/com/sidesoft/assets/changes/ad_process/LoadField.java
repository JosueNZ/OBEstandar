package ec.com.sidesoft.assets.changes.ad_process;

import java.util.List;
import java.sql.*;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.model.ad.ui.FieldTrl;

import ec.com.sidesoft.assets.changes.SSACHModifyAsset;
import ec.com.sidesoft.assets.changes.ModifyLine;
import ec.com.sidesoft.assets.changes.AssetsChanges;

public class LoadField extends DalBaseProcess {
  OBError message;

  public ModifyLine newLineModifyLine = null;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    try {
      message = new OBError();
      processRequest(bundle);
    } catch (Exception e) {
      String language = OBContext.getOBContext().getLanguage().getLanguage();
      ConnectionProvider conn = new DalConnectionProvider(false);
      message.setMessage(e.getMessage());
      message.setTitle(Utility.messageBD(conn, "Error", language));
      message.setType("Error");
    } finally {
      bundle.setResult(message);
    }
  }

  private void processRequest(ProcessBundle bundle) throws Exception {

    String modifyAssetId = (String) bundle.getParams().get("Ssach_Modify_Asset_ID");
    SSACHModifyAsset modifyAsset = OBDal.getInstance().get(SSACHModifyAsset.class, modifyAssetId);


    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);

    if(modifyAssetId!= null){
      loadFieldLine(modifyAssetId);
      modifyAsset.setIsstatus("IP");
      message.setMessage(Utility.messageBD(conn, "ssach_load_field", language));
      message.setType("Success");
      // message.setTitle(Utility.messageBD(conn, "ssach_load_field", language));
      message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
    }
    else{
      message.setMessage("No se pudo realizar la carga !!");
      // message.setMessage(Utility.messageBD(conn, "ecspp_line_budgeted", language));
      message.setType("Error");
      message.setTitle(Utility.messageBD(conn, "Error", language));
    }

  }

  // Carga en las lineas los campos y los valores del activo
  private void loadFieldLine (String modifyAssetId) throws Exception {

    OBContext.setAdminMode(true);

    OBCriteria<AssetsChanges> assetsChangesList = OBDal.getInstance()
						.createCriteria(AssetsChanges.class);
    SSACHModifyAsset modifyAsset = OBDal.getInstance().get(SSACHModifyAsset.class, modifyAssetId);
    OBCriteria<FieldTrl> fieldTrlList = OBDal.getInstance()
						.createCriteria(FieldTrl.class);
    String fielName = null;

		for (AssetsChanges assetChange : assetsChangesList.list()) {

      if(assetChange.isActive()==true){      

        for (FieldTrl fieldTrl : fieldTrlList.list()) {

            if(fieldTrl.getField().getId().equals(assetChange.getField().getId())){
              fielName = fieldTrl.getName();
            }
            // else{
            //   fielName =assetChange.getField().getName();
            // }
        }

        newLineModifyLine = OBProvider.getInstance().get(ModifyLine.class);
        newLineModifyLine.setNameField(fielName);
        newLineModifyLine.setActualRecord(assetChange.getColumn().getDBColumnName());
        newLineModifyLine.setSsachModifyAsset(modifyAsset);
        newLineModifyLine.setSsachAssetsChanges(assetChange);
        // newLineModifyLine.setSsachModifyAsset(modifyAsset);
        // newLineModifyLine.setLineNo(getSequenceNumber(order));

        OBDal.getInstance().save(newLineModifyLine);
        OBDal.getInstance().flush();
        
      }
      else{}

    }
  }

}
