package ec.com.sidesoft.assets.changes.ad_process;

import java.util.List;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.assets.changes.SSACHModifyAsset;
import ec.com.sidesoft.assets.changes.ModifyLine;
import ec.com.sidesoft.assets.changes.AssetsChanges;

import org.openbravo.model.financialmgmt.assetmgmt.Asset;

public class ProcessAssetChanges extends DalBaseProcess {
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
      boolean status = loadChangesAsset(modifyAssetId);
      if (status) {
      modifyAsset.setIsstatus("SSACH_PR");
      message.setMessage(Utility.messageBD(conn, "ssach_load_field", language));
      message.setType("Success");
      message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
      }
      else {
      message.setMessage(Utility.messageBD(conn, "ssach_no_line", language));
      message.setType("Error");
      message.setTitle(Utility.messageBD(conn, "Error", language));
      }

    }
    else{
      message.setMessage(Utility.messageBD(conn, "ssach_load_field", language));
      message.setType("Success");
      message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
    }

  }

    private boolean loadChangesAsset (String modifyAssetId) throws Exception {

    SSACHModifyAsset modifyAsset = OBDal.getInstance().get(SSACHModifyAsset.class, modifyAssetId);
    OBCriteria<ModifyLine> modifyLineList = OBDal.getInstance()
						.createCriteria(ModifyLine.class);
    modifyLineList.add(Restrictions.eq(ModifyLine.PROPERTY_SSACHMODIFYASSET, modifyAsset));
    modifyLineList.add(Restrictions.eq(ModifyLine.PROPERTY_MODIFY, true));
    Asset asset = OBDal.getInstance().get(Asset.class, modifyAssetId);

    if(modifyLineList.count()>0){
      OBCriteria<ModifyLine> modifyLineListPass = OBDal.getInstance()
						.createCriteria(ModifyLine.class);
      modifyLineListPass.add(Restrictions.eq(ModifyLine.PROPERTY_SSACHMODIFYASSET, modifyAsset));
      for (ModifyLine modifyLine : modifyLineListPass.list()) {
        if(modifyLine.isModify()==true){
        loadValueAsset(modifyLine, modifyAsset);
        }
        else{
          OBDal.getInstance().remove(modifyLine);
          OBDal.getInstance().flush();
        }
          }
        return true;
        }
    else{
        return false;
    }

  }

  // Carga en las lineas los campos y los valores del activo
  public void loadValueAsset (ModifyLine modifyLine, SSACHModifyAsset modifyAsset)throws Exception {
		try {
      ConnectionProvider connection = new DalConnectionProvider(false);
		  PreparedStatement statement = null;
			try {
				String consulta = "UPDATE public.a_asset "+
                          "SET "+modifyLine.getSsachAssetsChanges().getColumn().getDBColumnName().toLowerCase()+
                          "=? WHERE a_asset_id='"+modifyAsset.getAsset().getId()+"'";
				statement = connection.getPreparedStatement(consulta);

        boolean date = validateJavaDate(modifyLine.getActualRecord());
        boolean num = validateJavaNumeric(modifyLine.getActualRecord());
        if(date){

        SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyy-MM-dd");
        sdfrmt.setLenient(false);
        Date javaDate = sdfrmt.parse(modifyLine.getActualRecord());
        Timestamp timestamp = new Timestamp(javaDate.getTime());
        statement.setTimestamp(1, timestamp);
        }
        else if(num){
        Double javaNum = Double.parseDouble(modifyLine.getActualRecord());
				statement.setDouble(1,javaNum);
        }
        else{
				statement.setString(1, modifyLine.getActualRecord());
        }

			  statement.executeUpdate();
			} finally {
				if (statement != null && !statement.isClosed()) {
					statement.close();
				}
			}
		} catch (Exception e) {
			throw new OBException(e);
		}
	}

   public boolean validateJavaDate(String strDate)
   {
      if (strDate.trim().equals(""))
      {return true;}
      else
      {
          SimpleDateFormat sdfrmt = new SimpleDateFormat("yyyy-mm-dd");
          sdfrmt.setLenient(false);
          try
          {
              Date javaDate = sdfrmt.parse(strDate); 
          }
          catch (ParseException e)
          {
              return false;
          }
          return true;
	}
   }
     public boolean validateJavaNumeric(String strNum)
   {
      if (strNum.trim().equals(""))
      {return true;}
      else
      {
        try {
            Double num = Double.parseDouble(strNum);
        } catch (NumberFormatException e) {
            return false;}
        return true;
	    }

  } 
}
