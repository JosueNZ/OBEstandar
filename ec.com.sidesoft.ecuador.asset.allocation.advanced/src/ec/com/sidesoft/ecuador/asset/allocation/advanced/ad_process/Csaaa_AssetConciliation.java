package ec.com.sidesoft.ecuador.asset.allocation.advanced.ad_process;

import java.sql.Connection;
import java.util.Date;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.model.financialmgmt.assetmgmt.AssetGroup;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerContext;

import com.sidesoft.ecuador.asset.allocation.SsalApplActive;
import com.sidesoft.ecuador.asset.allocation.ssal_unit;
import com.sidesoft.ecuador.asset.allocation.ssalbuilding;
import com.sidesoft.ecuador.asset.allocation.ssaldepartment;

import ec.com.sidesoft.ecuador.asset.allocation.advanced.CsaaaInventoryTaking;
import ec.com.sidesoft.ecuador.asset.allocation.advanced.CsaaaInventoryTakingLine;
import ec.com.sidesoft.ecuador.asset.allocation.advanced.CsaaaLogAsset;

public class Csaaa_AssetConciliation extends DalBaseProcess {
  OBError message;
  static Logger log4j = Logger.getLogger(Csaaa_AssetConciliation.class);
  public static String dateTimeFormat;
  public static String sqlDateTimeFormat;
  private SchedulerContext ctx;
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

  // private AdvPaymentMngtDao dao;

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
      String strInventoryTakingID = (String) bundle.getParams().get("Csaaa_Inventory_Taking_ID");
      ConnectionProvider conn = new DalConnectionProvider(false);
      String language = OBContext.getOBContext().getLanguage().getLanguage();

      // Cabecera de Activos
      CsaaaInventoryTaking obdalInTaking = OBDal.getInstance().get(CsaaaInventoryTaking.class,
          strInventoryTakingID);

      // Asignar los Activos Scaneados

      String strAssetSearch = "";
      strAssetSearch = obdalInTaking.getBarcade() == null ? "" : obdalInTaking.getBarcade();

      strAssetSearch = strAssetSearch.replace("\n", ",");

      String strAssetSearchArray[] = strAssetSearch.split(",");

      // Preprarar Lista de Activos del Custodio
      OBCriteria<CsaaaInventoryTakingLine> csaaaInvTakingLine = OBDal.getInstance()
          .createCriteria(CsaaaInventoryTakingLine.class);
      csaaaInvTakingLine.add(Restrictions.eq(CsaaaInventoryTakingLine.PROPERTY_ACTIVE, true));
      csaaaInvTakingLine.add(
          Restrictions.eq(CsaaaInventoryTakingLine.PROPERTY_CSAAAINVENTORYTAKING, obdalInTaking));

      Object[] strAssetNoConcilited = new Object[1000];

      int AcumAssetNoConcilited = 0;

      boolean flagAsset = false;

      if (strAssetSearchArray.length > 0) {

        if (csaaaInvTakingLine.list().size() > 0) {

          for (int i = 0; i < strAssetSearchArray.length; i++) {

            String strbarcodeAsset1 = "";
            flagAsset = false;

            for (CsaaaInventoryTakingLine collInvTaking : csaaaInvTakingLine.list()) {

              strbarcodeAsset1 = strAssetSearchArray[i].toString();
              String strbarcodeAsset2 = collInvTaking.getBarcode();
              // System.out.println("A: " + strbarcodeAsset1 + " 2: " + strbarcodeAsset2);

              if (strbarcodeAsset1.equals(strbarcodeAsset2)
                  && ((!strbarcodeAsset1.equals("")) || strbarcodeAsset1 != null)
                  && ((!strbarcodeAsset2.equals("")) || strbarcodeAsset2 != null)) {
                CsaaaInventoryTakingLine obdalInTakingLine = OBDal.getInstance()
                    .get(CsaaaInventoryTakingLine.class, collInvTaking.getId());
                obdalInTakingLine.setConciliation(true);
                OBDal.getInstance().save(obdalInTakingLine);

                flagAsset = true;
              }
            }

            if (!flagAsset) {

              strAssetNoConcilited[AcumAssetNoConcilited] = strAssetSearchArray[i].toString();
              AcumAssetNoConcilited++;

            }
          }

          if (AcumAssetNoConcilited > 0) {

            Long lngLine = (long) obdalInTaking.getCsaaaInvtTkgLineList().size();
            for (int i = 0; i < (AcumAssetNoConcilited); i++) {

              lngLine = (lngLine * 10) + 10;

              // Preprarar Lista de Activos del Custodio
              OBCriteria<Asset> obcriteriaActive = OBDal.getInstance().createCriteria(Asset.class);
              obcriteriaActive.add(Restrictions.eq(Asset.PROPERTY_ACTIVE, true));
              obcriteriaActive.add(
                  Restrictions.eq(Asset.PROPERTY_SSALBARCODE, strAssetNoConcilited[i].toString()));

              if (obcriteriaActive.list().size() > 0) {

                Asset obdalAsset = OBDal.getInstance().get(Asset.class,
                    obcriteriaActive.list().get(0).getId());

                BusinessPartner bpCustodian = obdalInTaking.getCustodium();

                // Preprarar Lista de Activos del Custodio
                OBCriteria<SsalApplActive> obcriteriaAppActive = OBDal.getInstance()
                    .createCriteria(SsalApplActive.class);
                obcriteriaAppActive.add(Restrictions.eq(SsalApplActive.PROPERTY_ACTIVE, true));
                obcriteriaAppActive.add(Restrictions.eq(SsalApplActive.PROPERTY_ASSET, obdalAsset));
                obcriteriaAppActive
                    .add(Restrictions.ne(SsalApplActive.PROPERTY_CUSTODIAN, bpCustodian));

                if (obcriteriaAppActive.list().size() > 0) {

                  CsaaaInventoryTakingLine obdalNewInTakingLine = OBProvider.getInstance()
                      .get(CsaaaInventoryTakingLine.class);

                  AssetGroup assetCategory = obdalAsset.getAssetCategory();

                  Costcenter costCenter = obcriteriaAppActive.list().get(0).getCostCenter() != null
                      ? obcriteriaAppActive.list().get(0).getCostCenter()
                      : null;
                  UserDimension1 user1 = obcriteriaAppActive.list().get(0).getStDimension() != null
                      ? obcriteriaAppActive.list().get(0).getStDimension()
                      : null;
                  UserDimension2 user2 = obcriteriaAppActive.list().get(0).getNdDimension() != null
                      ? obcriteriaAppActive.list().get(0).getNdDimension()
                      : null;

                  ssalbuilding ssalBuilding = obcriteriaAppActive.list().get(0).getEdifice() != null
                      ? obcriteriaAppActive.list().get(0).getEdifice()
                      : null;

                  ssaldepartment ssaldepartment = obcriteriaAppActive.list().get(0)
                      .getDepartment() != null ? obcriteriaAppActive.list().get(0).getDepartment()
                          : null;

                  ssal_unit ssalUnit = obcriteriaAppActive.list().get(0).getUnit() != null
                      ? obcriteriaAppActive.list().get(0).getUnit()
                      : null;

                  obdalNewInTakingLine.setAsset(obdalAsset);
                  obdalNewInTakingLine.setBarcode(obdalAsset.getSsalBarCode());
                  obdalNewInTakingLine.setDescription(
                      obdalAsset.getDescription() == null ? null : obdalAsset.getDescription());
                  obdalNewInTakingLine.setAssetCategory(assetCategory);
                  obdalNewInTakingLine.setCostcenter(costCenter);
                  obdalNewInTakingLine.setUser1(user1);
                  obdalNewInTakingLine.setUser2(user2);
                  obdalNewInTakingLine.setEdifice(ssalBuilding);
                  obdalNewInTakingLine.setDepartment(ssaldepartment);
                  obdalNewInTakingLine.setUnit(ssalUnit);
                  obdalNewInTakingLine.setLine(lngLine);
                  obdalNewInTakingLine.setConciliation(true);
                  obdalNewInTakingLine.setCsaaaInventoryTaking(obdalInTaking);
                  OBDal.getInstance().save(obdalNewInTakingLine);

                  // Recuperar Datos - actiguo Custodio
                  SsalApplActive obdalApplActiveAsset = OBDal.getInstance()
                      .get(SsalApplActive.class, obcriteriaAppActive.list().get(0).getId());

                  obdalApplActiveAsset.setCsaaaIsconciliation(true);
                  OBDal.getInstance().save(obdalApplActiveAsset);

                  // OBDal.getInstance().flush();

                  // Nuevo Custodio - Reasignación

                  SsalApplActive obdalApplActive = OBProvider.getInstance()
                      .get(SsalApplActive.class);
                  obdalApplActive.setAsset(obdalAsset);
                  obdalApplActive.setAssetCod(obdalAsset);
                  obdalApplActive.setDescriptionActive(obdalAsset);
                  obdalApplActive.setUPCEAN(obdalAsset);
                  obdalApplActive.setApproved(true);
                  obdalApplActive.setState("3");
                  ;
                  obdalApplActive.setAssetCategory(assetCategory);
                  obdalApplActive
                      .setBusinessPartner((obdalApplActiveAsset.getBusinessPartner() != null
                          ? obdalApplActiveAsset.getBusinessPartner()
                          : null));
                  obdalApplActive.setCustodian(bpCustodian);
                  obdalApplActive.setDocumentType(obdalApplActiveAsset.getDocumentType());
                  obdalApplActive.setDocumentNo(obdalApplActiveAsset.getDocumentNo());
                  obdalApplActive.setCostCenter(costCenter);
                  obdalApplActive.setStDimension(user1);
                  obdalApplActive.setNdDimension(user2);
                  obdalApplActive.setEdifice(ssalBuilding);
                  obdalApplActive.setDepartment(ssaldepartment);
                  obdalApplActive.setUnit(ssalUnit);
                  OBDal.getInstance().save(obdalApplActive);
		  
		  obdalAsset.setSsalCustodio(bpCustodian);
                  OBDal.getInstance().save(obdalAsset);
	
                  // Nuevo Log
                  CsaaaLogAsset obdalLog = OBProvider.getInstance().get(CsaaaLogAsset.class);

                  obdalLog.setCsaaApplActive(obdalApplActive);
                  obdalLog.setDateConciliation(new Date());
                  OBDal.getInstance().save(obdalLog);

                  OBDal.getInstance().flush();

                }

              }
            }
          }
          OBDal.getInstance().flush();

          message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
          message.setType("Success");
          message.setMessage(Utility.messageBD(conn, "Csaaa_AssetsConciliation", language));

        } else {

          throw new OBException(Utility.messageBD(conn, "Csaaa_NoExistsAssetsByProcess", language));
        }
      } else

      {

        throw new OBException(Utility.messageBD(conn, "Csaaa_NoExistsAssetsScan", language));

      }

    } catch (Exception e) {
      System.out.println(e.getMessage());
      message.setTitle("Error");
      message.setType("Error");
      message.setMessage(e.getMessage());

    }
  }
}
