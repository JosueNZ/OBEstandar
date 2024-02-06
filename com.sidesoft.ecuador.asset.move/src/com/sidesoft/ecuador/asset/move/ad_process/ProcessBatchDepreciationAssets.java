package com.sidesoft.ecuador.asset.move.ad_process;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.assetmgmt.AmortizationLine;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

public class ProcessBatchDepreciationAssets extends DalBaseProcess {
  OBError message;
  static Logger log4j = Logger.getLogger(ProcessBatchDepreciationAssets.class);
  public static String dateTimeFormat;
  public static String sqlDateTimeFormat;
  public TaxRate taxRate;
  public String strNewInvoiceID;
  public String strAttachment;
  public String strFTP;
  public Connection connectionDB = null;
  public String strSearchInvoice;
  public ConfigParameters cf;
  public String successMessage = null;
  public String strFinancialAccountID = null;
  public String strDocumentnoPaymentIn;
  public static final String TRXTYPE_BPDeposit = "BPD";
  public static final String TRXTYPE_BPWithdrawal = "BPW";
  public static final String TRXTYPE_BankFee = "BF";
  public String strFinPaymentScheduleDetailID = "";
  ConnectionProvider cnn_insert;
  ConnectionProvider cnn_insert2;

  private String strAssetsError = "Sucessfull";

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
    try {

      ConnectionProvider conn = new DalConnectionProvider(false);
      String language = OBContext.getOBContext().getLanguage().getLanguage();

      OBCriteria<Asset> obcAsset = OBDal.getInstance().createCriteria(Asset.class);
      obcAsset.add(Restrictions.eq(Asset.PROPERTY_ACTIVE, true));
      obcAsset.add(Restrictions.eq(Asset.PROPERTY_DEPRECIATE, true));

      List<Asset> lstAsset = obcAsset.list();
      int count = 0;
      for (Asset collectionAsset : lstAsset) {

        Asset asset = (Asset) OBDal.getInstance().getProxy(Asset.ENTITY_NAME,
            collectionAsset.getId());

        OBCriteria<AmortizationLine> obcAmortizationLine = OBDal.getInstance()
            .createCriteria(AmortizationLine.class);
        obcAmortizationLine.add(Restrictions.eq(AmortizationLine.PROPERTY_ASSET, asset));

        @SuppressWarnings("unused")
        String strMessage = "";

        if (obcAmortizationLine.list().size() == 0) {
          // updateInventory(asset.getId().toString());

          strMessage = updateInventory2(conn, asset.getId().toString());

          if(strMessage.equals("@SEACT_DoctypeNotDefined@")) {
            strAssetsError = Utility.messageBD(conn, strMessage.replaceAll("@", ""), language).toString();
            throw new RuntimeException(strAssetsError);
          } else if (!strMessage.equals("@RowsInserted@")) {
            /*
            strAssetsError = asset.getName().toString() + ": "
                + Utility.messageBD(conn, strMessage.replaceAll("@", ""), language).toString()
                + "\n";
            */
            strAssetsError = Utility.messageBD(conn, strMessage.replaceAll("@", ""), language).toString() + ". Activo " + asset.getSearchKey().toString();
            throw new RuntimeException(strAssetsError);
          }
        } else {
          count++;
        }
      }

      message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
      message.setType("Success");
      //message.setMessage(Utility.messageBD(conn, strAssetsError, language));
      message.setMessage(Utility.messageBD(conn, strAssetsError, language) + ". Planes creados para (" + String.valueOf(count) + ") activos");

    } catch (Exception e) {
      message.setTitle("Error");
      message.setType("Error");
      //message.setMessage(e.getMessage());
      message.setMessage(strAssetsError);

    }
  }

  protected void updateInventory(String StrAssetID) {
    try {
      org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
      CallableStatement cs = cp.getConnection().prepareCall("{call SSAM_ASSET_POST(?)}");

      // ID Asset
      cs.setString(1, StrAssetID);

      cs.execute();
      cs.close();
    } catch (Exception e) {
      throw new OBException(e.getMessage(), e);
    }
  }

  public static String updateInventory2(ConnectionProvider connectionProvider, String StrAssetID) {
    String strSql = "";
    strSql = strSql + "       SELECT ssam_asset_post('" + StrAssetID + "') as name"
        + "       FROM dual";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();

      if (result.next()) {
        strReturn = UtilSql.getValue(result, "name");
      }

      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);

    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return strReturn;
  }

}