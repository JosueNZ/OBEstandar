package ec.com.sidesoft.localization.inventoryaccounting.acc_template;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.costing.CostingAlgorithm.CostDimension;
import org.openbravo.costing.CostingStatus;
import org.openbravo.costing.CostingUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.Account;
import org.openbravo.erpCommon.ad_forms.AcctSchema;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.ad_forms.DocInOut;
import org.openbravo.erpCommon.ad_forms.DocInOutTemplate;
import org.openbravo.erpCommon.ad_forms.DocInventory;
import org.openbravo.erpCommon.ad_forms.DocInventoryTemplate;
import org.openbravo.erpCommon.ad_forms.DocLine;
import org.openbravo.erpCommon.ad_forms.DocLine_FinPaymentSchedule;
import org.openbravo.erpCommon.ad_forms.Fact;
import org.openbravo.erpCommon.ad_forms.FactLine;
import org.openbravo.erpCommon.ad_forms.ProductInfo;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

import org.openbravo.erpCommon.ad_forms.DocLine_Material;

public class InventoryAccountingTemplate extends DocInventoryTemplate {



	  private static final long serialVersionUID = 1L;
	  private static final String SO_COST_PRODUCT = "4";

	  // Final Settlement
	  public static final String DOCTYPE_Alienate = "ARI";
	  private String SeqNo = "0";
	  protected Logger logger = Logger.getLogger(this.getClass());
	  static Logger log4jDocInventory = Logger.getLogger(InventoryAccountingTemplate.class);
	  public String strMessage = null;
	  public String strOrderID = "";

	  public DocLine[] p_lines = new DocLine[0];

	  public String DocumentType = "";
	  public String IsReturn = "";

	  public String AD_Table_ID = "";

	  public String AD_Client_ID = "";
	  public String AD_Org_ID = "";
	  public String Status = "";
	  public String C_BPartner_ID = "";
	  public String C_BPartner_Location_ID = "";
	  public String M_Product_ID = "";
	  public String AD_OrgTrx_ID = "";
	  public String C_SalesRegion_ID = "";
	  public String C_Project_ID = "";
	  public String C_Campaign_ID = "";
	  public String C_Activity_ID = "";
	  public String C_LocFrom_ID = "";
	  public String C_LocTo_ID = "";
	  public String User1_ID = "";
	  public String User2_ID = "";
	  public String C_Costcenter_ID = "";
	  public String Name = "";
	  public String DocumentNo = "";
	  public String DateAcct = "";
	  public Date dateAcct = null;
	  public String DateDoc = "";
	  public String C_Period_ID = "";
	  public String C_Currency_ID = "";
	  public String C_DocType_ID = "";
	  public String C_Charge_ID = "";
	  public String ChargeAmt = "";
	  public String C_BankAccount_ID = "";
	  public String C_CashBook_ID = "";
	  public String M_Warehouse_ID = "";
	  public String Posted = "";

	  public String Record_ID = "";

	  protected static final String NO_CURRENCY = "-1";

	  public String[] Amounts = new String[4];
	  /** Amount Type - Invoice */
	  public static final int AMTTYPE_Gross = 0;
	  public static final int AMTTYPE_Net = 1;
	  public static final int AMTTYPE_Charge = 2;
	  /** Amount Type - Allocation */
	  public static final int AMTTYPE_Invoice = 0;
	  public static final int AMTTYPE_Allocation = 1;
	  public static final int AMTTYPE_Discount = 2;
	  public static final int AMTTYPE_WriteOff = 3;

	  OBError messageResult = null;
	  /** Document Status */
	  public static final String STATUS_NotPosted = "N";
	  /** Document Status */
	  public static final String STATUS_NotBalanced = "b";
	  /** Document Status */
	  public static final String STATUS_NotConvertible = "c";
	  /** Document Status */
	  public static final String STATUS_PeriodClosed = "p";
	  /** Document Status */
	  public static final String STATUS_InvalidAccount = "i";
	  /** Document Status */
	  public static final String STATUS_PostPrepared = "y";
	  /** Document Status */
	  public static final String STATUS_Posted = "Y";
	  /** Document Status */
	  public static final String STATUS_Error = "E";
	  /** Document Status */
	  public static final String STATUS_InvalidCost = "C";
	  /** Document Status */
	  public static final String STATUS_NotCalculatedCost = "NC";
	  /** Document Status */
	  public static final String STATUS_NoRelatedPO = "NO";
	  /** Document Status */
	  public static final String STATUS_DocumentLocked = "L";
	  /** Document Status */
	  public static final String STATUS_DocumentDisabled = "D";

	  public static final String STATUS_TableDisabled = "T";
	  /** Document Status */
	  public static final String STATUS_BackgroundDisabled = "d";
	  /** Document Status */
	  public static final String STATUS_NoAccountingDate = "AD";

	  public String tableName = "";

	  /** Table IDs for document level conversion rates */
	  public static final String TABLEID_Invoice = "318";
	  public static final String TABLEID_Payment = "D1A97202E832470285C9B1EB026D54E2";
	  public static final String TABLEID_Transaction = "4D8C3B3C31D1410DA046140C9F024D17";
	  public static final String TABLEID_Reconciliation = "B1B7075C46934F0A9FD4C4D0F1457B42";

	  /** Account Type - Invoice */
	  public static final String ACCTTYPE_Charge = "0";
	  public static final String ACCTTYPE_C_Receivable = "1";
	  public static final String ACCTTYPE_V_Liability = "2";
	  public static final String ACCTTYPE_V_Liability_Services = "3";

	  /** Account Type - Payment */
	  public static final String ACCTTYPE_UnallocatedCash = "10";
	  public static final String ACCTTYPE_BankInTransit = "11";
	  public static final String ACCTTYPE_PaymentSelect = "12";
	  public static final String ACCTTYPE_WriteOffDefault = "13";
	  public static final String ACCTTYPE_WriteOffDefault_Revenue = "63";
	  public static final String ACCTTYPE_BankInTransitDefault = "14";
	  public static final String ACCTTYPE_ConvertChargeDefaultAmt = "15";
	  public static final String ACCTTYPE_ConvertGainDefaultAmt = "16";

	  /** Account Type - Cash */
	  public static final String ACCTTYPE_CashAsset = "20";
	  public static final String ACCTTYPE_CashTransfer = "21";
	  public static final String ACCTTYPE_CashExpense = "22";
	  public static final String ACCTTYPE_CashReceipt = "23";
	  public static final String ACCTTYPE_CashDifference = "24";

	  /** Account Type - Allocation */
	  public static final String ACCTTYPE_DiscountExp = "30";
	  public static final String ACCTTYPE_DiscountRev = "31";
	  public static final String ACCTTYPE_WriteOff = "32";
	  public static final String ACCTTYPE_WriteOff_Revenue = "64";

	  /** Account Type - Bank Statement */
	  public static final String ACCTTYPE_BankAsset = "40";
	  public static final String ACCTTYPE_InterestRev = "41";
	  public static final String ACCTTYPE_InterestExp = "42";
	  public static final String ACCTTYPE_ConvertChargeLossAmt = "43";
	  public static final String ACCTTYPE_ConvertChargeGainAmt = "44";

	  /** Inventory Accounts */
	  public static final String ACCTTYPE_InvDifferences = "50";
	  public static final String ACCTTYPE_NotInvoicedReceipts = "51";

	  /** Project Accounts */
	  public static final String ACCTTYPE_ProjectAsset = "61";
	  public static final String ACCTTYPE_ProjectWIP = "62";

	  /** GL Accounts */
	  public static final String ACCTTYPE_PPVOffset = "60";

	  // Reference (to find SalesRegion from BPartner)
	  public String BP_C_SalesRegion_ID = ""; // set in FactLine

	  public int errors = 0;
	  int success = 0;
	  // Distinguish background process
	  boolean isBackground = false;

	  public static Category log4j;

	  public String StrMInoutLine = "";
	  public boolean blnComWokrPr = false;

	  public Fact createFact(DocInventory docInventory, AcctSchema as,
		      ConnectionProvider conn, Connection con, VariablesSecureApp vars) throws ServletException {

		Fact fact = new Fact(docInventory, as, Fact.POST_Actual);


		String strInventoryID = docInventory.Record_ID.toString();
		Record_ID = docInventory.Record_ID.toString();



		InventoryCount inventoryCount = OBDal.getInstance().get(InventoryCount.class, strInventoryID);

		DocumentType = inventoryCount.getSsinDoctype().getDocumentCategory();
		    
		    		  
		String stradOrgID = inventoryCount.getOrganization().getId();
		String stradClientID = vars.getClient();
		String stradUSerID = vars.getUser();
		
		DocInventoryValidOrganizationData data[] = null;
		
		int  intCountInv = 0;
		int  intCountTempInv = 0;
		
		String  docInv1 = "ND";
		String  docInv2 = "ND";
		
		String strAccountID = "ND";
		
		
		try {
			intCountInv = Integer.parseInt((DocInventoryValidOrganizationData.existsParams(conn ,inventoryCount.getSsinDoctype().getId())==null?"0":DocInventoryValidOrganizationData.existsParams(conn , inventoryCount.getSsinDoctype().getId())));
			
		}catch(Exception e) {
			
		}
		
		
		if (intCountInv>0) {
			// Revisa si hay organizaciones padres e hijas
			
			data = DocInventoryValidOrganizationData.selectAccountOrganization(conn, stradOrgID);

			if (data.length>0) {
				
				// Recupera configuración contable de la organizacion padre 1
				docInv2 = DocInventoryValidOrganizationData.selectAccounting(conn, data[0].hijoid, inventoryCount.getSsinDoctype().getId())==null? "ND": DocInventoryValidOrganizationData.selectAccount1(conn, data[0].hijoid, inventoryCount.getSsinDoctype().getId());
				strAccountID = docInv2;
				
				String strInventoryAcctOrg="";
				if (docInv2.equals("ND")) {
					strInventoryAcctOrg = data[0].padreid;
					
					while(docInv2.equals("ND")) {
						
						DocInventoryValidOrganizationData data2[] = null;
						
						data2 = DocInventoryValidOrganizationData.selectAccountOrganization(conn, strInventoryAcctOrg);
						
						if (data2.length>0) {
							String strOrgID= String.valueOf(data2[0].padreid);
							if (strOrgID.equals("0")) {
								docInv2 = DocInventoryValidOrganizationData.selectAccounting(conn, data2[0].hijoid, inventoryCount.getSsinDoctype().getId())==null? "ND": DocInventoryValidOrganizationData.selectAccount1(conn, data2[0].hijoid, inventoryCount.getSsinDoctype().getId());
								strInventoryAcctOrg =  data2[0].hijoid;
							}else {
						
								docInv2 = DocInventoryValidOrganizationData.selectAccounting(conn, data2[0].padreid, inventoryCount.getSsinDoctype().getId())==null? "ND": DocInventoryValidOrganizationData.selectAccount1(conn, data2[0].padreid, inventoryCount.getSsinDoctype().getId());
								strInventoryAcctOrg =  data2[0].padreid;
							}

						}
	
						strAccountID = docInv2;

						if(!docInv2.equals("ND")) {
							strAccountID = docInv2;

						}else {
							intCountTempInv++;
						}
						
						if(intCountTempInv>intCountInv) {
							docInv2="NF";
						}

						

					}
				}
				
				if(docInv2.equals("NF")) {
					docInv2="ND";
				}
				
				if (docInv2.equals("ND")) {
					// Recupera configuración contable de la organizacion padre 2					
					docInv2 = DocInventoryValidOrganizationData.selectAccounting(conn, stradOrgID, inventoryCount.getSsinDoctype().getId())==null? "ND": DocInventoryValidOrganizationData.selectAccount1(conn, stradOrgID, inventoryCount.getSsinDoctype().getId());
					strAccountID = docInv2;
				}
				
				if (docInv2.equals("ND")) {
					
					docInv1 = DocInventoryValidOrganizationData.selectAccount2(conn , "0", inventoryCount.getSsinDoctype().getId())==null?"ND":DocInventoryValidOrganizationData.selectAccount2(conn , "0", inventoryCount.getSsinDoctype().getId());
					
					if (docInv1.equals("ND")) {
						return null;
					}else {
						strAccountID = docInv1;
					}
					
				}
				
			}else {
				
				
			
				if (docInv2.equals("ND")) {
					// Recupera configuración contable de la organizacion padre 2					
					docInv2 = DocInventoryValidOrganizationData.selectAccounting(conn, stradOrgID, inventoryCount.getSsinDoctype().getId())==null? "ND": DocInventoryValidOrganizationData.selectAccount1(conn, stradOrgID, inventoryCount.getSsinDoctype().getId());
					strAccountID = docInv2;
				}
				
				if (docInv2.equals("ND")) {
					
					docInv1 = DocInventoryValidOrganizationData.selectAccount2(conn , "0", inventoryCount.getSsinDoctype().getId())==null?"ND":DocInventoryValidOrganizationData.selectAccount2(conn , "0", inventoryCount.getSsinDoctype().getId());
					
					if (docInv1.equals("ND")) {
						return null;
					}else {
						strAccountID = docInv1;
					}
					
				}
			}
			
			/*
			docInv2 = DocInventoryValidOrganizationData.selectAccount1(conn, stradOrgID, inventoryCount.getSsinDoctype().getId())==null? "ND": DocInventoryValidOrganizationData.selectAccount1(conn, stradOrgID, inventoryCount.getSsinDoctype().getId());
			if (docInv2.equals("ND")) {
				docInv1 = DocInventoryValidOrganizationData.selectAccount2(conn , stradOrgID, inventoryCount.getSsinDoctype().getId())==null?"ND":DocInventoryValidOrganizationData.selectAccount2(conn , stradOrgID, inventoryCount.getSsinDoctype().getId());
				if (docInv1.equals("ND")) {
					
					docInv1 = DocInventoryValidOrganizationData.selectAccount2(conn , "0", inventoryCount.getSsinDoctype().getId())==null?"ND":DocInventoryValidOrganizationData.selectAccount2(conn , "0", inventoryCount.getSsinDoctype().getId());
					
					if (docInv1.equals("ND")) {
						return null;
					}else {
						strAccountID = docInv1;
					}
					
				}else {
					strAccountID = docInv1;
				}
			}else {
				strAccountID = docInv2;
			}*/
			

			
		}
		
		if (intCountInv>0 && !strAccountID.equals("ND"))
		{
	    // create Fact Header

	    String Fact_Acct_Group_ID = SequenceIdData.getUUID();

	    p_lines = docInventory.loadLines(conn);
	    FactLine dr = null;
	    FactLine cr = null;

	    // Sales or Return from Customer
	    if (DocumentType.equals(AcctServer.DOCTYPE_MatInventory)) {
	    	

	        log4jDocInventory.debug("CreateFact - before loop");
	        if (p_lines.length == 0) {
	          setStatus(STATUS_DocumentDisabled);
	        }
	        int countInvLinesWithTrnCostZero = 0;
	        for (int i = 0; i < p_lines.length; i++) {
	          DocLine_Material line = (DocLine_Material) p_lines[i];
	          if (CostingStatus.getInstance().isMigrated() && line.transaction != null
	              && "NC".equals(line.transaction.getCostingStatus())) {
	            setStatus(STATUS_NotCalculatedCost);
	          }

	          if (line.transaction == null
	              //|| (line.transaction.getTransactionCost() != null && line.transaction
	              //    .getTransactionCost().compareTo(ZERO) == 0)
	              ) {
	            countInvLinesWithTrnCostZero++;
	          }
	        }
/*	        if (p_lines.length == countInvLinesWithTrnCostZero) {
	          setStatus(STATUS_DocumentDisabled);
	        }*/
	        for (int i = 0; i < p_lines.length; i++) {
	          DocLine_Material line = (DocLine_Material) p_lines[i];

	          Currency costCurrency = FinancialUtils.getLegalEntityCurrency(OBDal.getInstance().get(
	              Organization.class, line.m_AD_Org_ID));
	          if (!CostingStatus.getInstance().isMigrated()) {
	            costCurrency = OBDal.getInstance().get(Client.class, AD_Client_ID).getCurrency();
	          } else if (line.transaction != null && line.transaction.getCurrency() != null) {
	            costCurrency = line.transaction.getCurrency();
	          }
	          if (CostingStatus.getInstance().isMigrated() && line.transaction != null
	              && !line.transaction.isCostCalculated()) {
	            Map<String, String> parameters = getNotCalculatedCostParameters(line.transaction);
	            setMessageResult(conn, STATUS_NotCalculatedCost, "error", parameters);
	            throw new IllegalStateException();
	          }
	          String costs = "";
	          BigDecimal b_Costs = BigDecimal.ZERO;
	          if (line.transaction != null) {
	            costs = line.getProductCosts(DateAcct, as, conn, con);
	            b_Costs = new BigDecimal(costs);
	          }
	          log4jDocInventory.debug("CreateFact - before DR - Costs: " + costs);
	          Account assetAccount = line.getAccount(ProductInfo.ACCTTYPE_P_Asset, as, conn);
	          if (assetAccount == null) {
	            Product product = OBDal.getInstance().get(Product.class, line.m_M_Product_ID);
	            org.openbravo.model.financialmgmt.accounting.coa.AcctSchema schema = OBDal.getInstance()
	                .get(org.openbravo.model.financialmgmt.accounting.coa.AcctSchema.class,
	                    as.m_C_AcctSchema_ID);
	            log4j.error("No Account Asset for product: " + product.getName()
	                + " in accounting schema: " + schema.getName());
	          }
	          if (b_Costs.compareTo(BigDecimal.ZERO) == 0 && !CostingStatus.getInstance().isMigrated()
	              && DocInOutTemplateData.existsCost(conn, DateAcct, line.m_M_Product_ID).equals("0")) {
	            Map<String, String> parameters = getInvalidCostParameters(
	                OBDal.getInstance().get(Product.class, line.m_M_Product_ID).getIdentifier(), DateAcct);
	            setMessageResult(conn, STATUS_InvalidCost, "error", parameters);
	            throw new IllegalStateException();
	          }
	          // Inventory DR CR
	          dr = fact.createLine(line, assetAccount, costCurrency.getId(), costs, Fact_Acct_Group_ID,
	              nextSeqNo(SeqNo), DocumentType, conn);
	          // may be zero difference - no line created.
	          if (dr == null) {
	            continue;
	          }
	          dr.setM_Locator_ID(line.m_M_Locator_ID);
	          log4jDocInventory.debug("CreateFact - before CR");
	          // InventoryDiff DR CR
	          // or Charge
	          Account invDiff = line.getChargeAccount(as, b_Costs.negate(), conn);
	          log4jDocInventory.debug("CreateFact - after getChargeAccount");
	          if (invDiff == null) {
	              invDiff = getAccount("4", strAccountID,as, conn);
	            }
	          
	          if (invDiff == null) {
	            invDiff = getAccount(AcctServer.ACCTTYPE_InvDifferences, as, conn);
	          }
	          log4jDocInventory.debug("CreateFact - after getAccount - invDiff; " + invDiff);
	          cr = fact.createLine(line, invDiff, costCurrency.getId(), (b_Costs.negate()).toString(),
	              Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
	          if (cr == null) {
	            continue;
	          }
	          cr.setM_Locator_ID(line.m_M_Locator_ID);
	        }
	        log4jDocInventory.debug("CreateFact - after loop");
	        SeqNo = "0";
	    } else {
	      log4jDocInventory.warn("createFact - " + "DocumentType unknown: " + DocumentType);
	      return null;
	    }
	  }else {
	      log4jDocInventory.warn("createFact - " + "DocumentType unknown: " + DocumentType);

		  return null;
          
	  }

	    return fact;
	  }


	  public Account getAccount(String AcctType, String strAccountID, AcctSchema as,
	      ConnectionProvider conn) {
	    if (Integer.parseInt(AcctType) < 1 || Integer.parseInt(AcctType) > 10)
	      return null;

	    //String validCombination_ID = "9322E18C126A4CC285714D21296A5C2B";
	    String validCombination_ID = strAccountID;
	/*
	    if (blnComWokrPr) {
	      validCombination_ID = ShpInoutLine.getProduct().getProductAccountsList().get(0)
	    } else {
	      if (ShpInoutLine.isSstpcIsconsworkp()) {
	        validCombination_ID = ShpInoutLine.getProduct().getProductAccountsList().get(0)
	            .getSstpcCostProcessProd().getId();
	      } else {
	        validCombination_ID = ShpInoutLine.getProduct().getProductAccountsList().get(0)
	            .getProductCOGS().getId();
	      }
	    }*/

	    Account acc = null;
	    try {

	      switch (Integer.parseInt(AcctType)) {
	      case 4:
	        // validCombination_ID = "1C2ED57CB1544FC68A2F871381BC99A3";// data[0].pCogsAcct;
	        if (validCombination_ID == null || validCombination_ID.equals("")) {
	          String language = OBContext.getOBContext().getLanguage().getLanguage();
	          OBError err = new OBError();
	          err.setType("Error");
	          err.setMessage(strMessage);
	          setMessageResult(err);
	        }
	        break;
	      }
	      if (validCombination_ID.equals(""))
	        return null;
	      acc = Account.getAccount(conn, validCombination_ID);
	      // log4jDocAlienate.debug("DocAmortization - getAccount - " + acc.Account_ID);
	    } catch (ServletException e) {
	      // log4jDocAlienate.warn(e);
	    }
	    return acc;
	  }

	  private void setMessageResult(OBError err) {
	    // TODO Auto-generated method stub

	  }

	  public Map<String, String> getNotCalculatedCostParameters(MaterialTransaction trx) {
	    Map<String, String> parameters = new HashMap<String, String>();
	    parameters.put("trx", trx.getIdentifier());
	    parameters.put("product", trx.getProduct().getIdentifier());
	    return parameters;
	  }

	  public void setMessageResult(ConnectionProvider conn, String _strStatus, String strMessageType,
	      Map<String, String> _parameters) {
	    HttpServletRequest request = RequestContext.get().getRequest();
	    VariablesSecureApp vars;

	    if (request != null) {
	      // getting context info from session
	      vars = new VariablesSecureApp(RequestContext.get().getRequest());
	    } else {
	      // there is no session, getting context info from OBContext
	      OBContext ctx = OBContext.getOBContext();
	      vars = new VariablesSecureApp((String) DalUtil.getId(ctx.getUser()),
	          (String) DalUtil.getId(ctx.getCurrentClient()),
	          (String) DalUtil.getId(ctx.getCurrentOrganization()),
	          (String) DalUtil.getId(ctx.getRole()), ctx.getLanguage().getLanguage());
	    }
	    setMessageResult(conn, vars, _strStatus, strMessageType, _parameters);
	  }

	  public void setMessageResult(ConnectionProvider conn, VariablesSecureApp vars, String _strStatus,
	      String strMessageType, Map<String, String> _parameters) {
	    String strStatus = StringUtils.isEmpty(_strStatus) ? getStatus() : _strStatus;
	    setStatus(strStatus);
	    String strTitle = "";
	    Map<String, String> parameters = _parameters != null ? _parameters
	        : new HashMap<String, String>();
	    if (messageResult == null)
	      messageResult = new OBError();
	    if (strMessageType == null || strMessageType.equals(""))
	      messageResult.setType("Error");
	    else
	      messageResult.setType(strMessageType);
	    if (strStatus.equals(STATUS_Error))
	      strTitle = "@ProcessRunError@";
	    else if (strStatus.equals(STATUS_DocumentLocked)) {
	      strTitle = "@OtherPostingProcessActive@";
	      messageResult.setType("Warning");
	    } else if (strStatus.equals(STATUS_NotCalculatedCost)) {
	      if (parameters.isEmpty()) {
	        strTitle = "@NotCalculatedCost@";
	      } else {
	        strTitle = "@NotCalculatedCostWithTransaction@";
	      }
	    } else if (strStatus.equals(STATUS_InvalidCost)) {
	      if (parameters.isEmpty()) {
	        strTitle = "@InvalidCost@";
	      } else {
	        strTitle = "@InvalidCostWhichProduct@";
	        // Account name from messages
	        parameters.put("Account",
	            Utility.parseTranslation(conn, vars, vars.getLanguage(), parameters.get("Account")));
	      }
	    } else if (strStatus.equals(STATUS_NoRelatedPO)) {
	      if (parameters.isEmpty()) {
	        strTitle = "@GoodsReceiptTransactionWithNoPO@";
	      } else {
	        strTitle = "@GoodsReceiptTransactionWithNoPOWichProduct@";
	      }
	    } else if (strStatus.equals(STATUS_DocumentDisabled)) {
	      strTitle = "@DocumentDisabled@";
	      messageResult.setType("Warning");
	    } else if (strStatus.equals(STATUS_BackgroundDisabled)) {
	      strTitle = "@BackgroundDisabled@";
	      messageResult.setType("Warning");
	    } else if (strStatus.equals(STATUS_InvalidAccount)) {
	      if (parameters.isEmpty()) {
	        strTitle = "@InvalidAccount@";
	      } else {
	        strTitle = "@InvalidWhichAccount@";
	        // Transalate account name from messages
	        parameters.put("Account",
	            Utility.parseTranslation(conn, vars, vars.getLanguage(), parameters.get("Account")));
	      }
	    } else if (strStatus.equals(STATUS_PeriodClosed)) {
	      strTitle = "@PeriodNotAvailable@";
	    } else if (strStatus.equals(STATUS_NotConvertible)) {
	      strTitle = "@NotConvertible@";
	    } else if (strStatus.equals(STATUS_NotBalanced)) {
	      strTitle = "@NotBalanced@";
	    } else if (strStatus.equals(STATUS_NotPosted)) {
	      strTitle = "@NotPosted@";
	    } else if (strStatus.equals(STATUS_PostPrepared)) {
	      strTitle = "@PostPrepared@";
	    } else if (strStatus.equals(STATUS_Posted)) {
	      strTitle = "@Posted@";
	    } else if (strStatus.equals(STATUS_TableDisabled)) {
	      strTitle = "@TableDisabled@";
	      parameters.put("Table", tableName);
	      messageResult.setType("Warning");
	    } else if (strStatus.equals(STATUS_NoAccountingDate)) {
	      strTitle = "@NoAccountingDate@";
	    }
	    messageResult.setMessage(Utility.parseTranslation(conn, vars, parameters, vars.getLanguage(),
	        Utility.parseTranslation(conn, vars, vars.getLanguage(), strTitle)));
	    if (strMessage != null) {
	      messageResult.setMessage(Utility.parseTranslation(conn, vars, parameters, vars.getLanguage(),
	          Utility.parseTranslation(conn, vars, vars.getLanguage(), strMessage)));
	    }
	  }

	  public String getStatus() {
	    return Status;
	  }

	  public void setStatus(String strStatus) {
	    Status = strStatus;
	  }

	  public Map<String, String> getInvalidCostParameters(String strProduct, String strDate) {
	    Map<String, String> parameters = new HashMap<String, String>();
	    parameters.put("Product", strProduct);
	    parameters.put("Date", strDate);
	    return parameters;
	  }

	  public String nextSeqNo(String oldSeqNo) {
	    log4jDocInventory.debug("DocInOut - oldSeqNo = " + oldSeqNo);
	    BigDecimal seqNo = new BigDecimal(oldSeqNo);
	    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
	    log4jDocInventory.debug("DocInOut - nextSeqNo = " + SeqNo);
	    return SeqNo;
	  }

	  public static String getConvertedAmt(String Amt, String CurFrom_ID, String CurTo_ID,
	      String ConvDate, String RateType, ConnectionProvider conn) {
	    Category log4j = null;
	    if (log4j.isDebugEnabled())
	      log4j.debug("AcctServer - getConvertedAmount no client nor org");
	    return getConvertedAmt(Amt, CurFrom_ID, CurTo_ID, ConvDate, RateType, "", "", conn);
	  }

	  public static String getConvertedAmt(String Amt, String CurFrom_ID, String CurTo_ID,
	      String ConvDate, String RateType, String client, String org, ConnectionProvider conn) {
	    if (log4j.isDebugEnabled())
	      log4j.debug("AcctServer - getConvertedAmount - starting method - Amt : " + Amt
	          + " - CurFrom_ID : " + CurFrom_ID + " - CurTo_ID : " + CurTo_ID + "- ConvDate: "
	          + ConvDate + " - RateType:" + RateType + " - client:" + client + "- org:" + org);
	    if (Amt.equals(""))
	      throw new IllegalArgumentException(
	          "AcctServer - getConvertedAmt - required parameter missing - Amt");
	    if (CurFrom_ID.equals(CurTo_ID) || Amt.equals("0"))
	      return Amt;
	    AcctServerTemplateData[] data = null;
	    try {
	      if (ConvDate != null && ConvDate.equals(""))
	        ConvDate = DateTimeData.today(conn);
	      // ConvDate IN DATE
	      if (RateType == null || RateType.equals(""))
	        RateType = "S";
	      data = AcctServerTemplateData.currencyConvert(conn, Amt, CurFrom_ID, CurTo_ID, ConvDate,
	          RateType, client, org);
	    } catch (ServletException e) {
	      log4j.warn(e);
	      e.printStackTrace();
	    }
	    if (data == null || data.length == 0) {
	      /*
	       * log4j.error("No conversion ratio"); throw new
	       * ServletException("No conversion ratio defined!");
	       */
	      return "";
	    } else {
	      if (log4j.isDebugEnabled())
	        log4j.debug("getConvertedAmount - converted:" + data[0].converted);
	      return data[0].converted;
	    }
	  } // getConvertedAmt

	  public final Account getAccount(String AcctType, AcctSchema as, ConnectionProvider conn) {
	    BigDecimal AMT = null;
	    AcctServerTemplateData[] data = null;

	    // if (log4j.isDebugEnabled())
	    // log4j.debug("*******************************getAccount 1: AcctType:-->"
	    // + AcctType);
	    try {
	      /** Account Type - Invoice */
	      if (AcctType.equals(ACCTTYPE_Charge)) { // see getChargeAccount in
	        // DocLine
	        // if (log4j.isDebugEnabled())
	        // log4j.debug("AcctServer - *******************amount(AMT);-->"
	        // + getAmount(AMTTYPE_Charge));
	        AMT = new BigDecimal(getAmount(AMTTYPE_Charge));
	        // if (log4j.isDebugEnabled())
	        // log4j.debug("AcctServer - *******************AMT;-->" + AMT);
	        int cmp = AMT.compareTo(BigDecimal.ZERO);
	        // if (log4j.isDebugEnabled())
	        // log4j.debug("AcctServer - ******************* CMP: " + cmp);
	        if (cmp == 0)
	          return null;
	        else if (cmp < 0)
	          data = AcctServerTemplateData.selectExpenseAcct(conn, C_Charge_ID,
	              as.getC_AcctSchema_ID());
	        else
	          data = AcctServerTemplateData.selectRevenueAcct(conn, C_Charge_ID,
	              as.getC_AcctSchema_ID());
	        // if (log4j.isDebugEnabled())
	        // log4j.debug("AcctServer - *******************************getAccount 2");
	      } else if (AcctType.equals(ACCTTYPE_V_Liability)) {
	        data = AcctServerTemplateData.selectLiabilityAcct(conn, C_BPartner_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_V_Liability_Services)) {
	        data = AcctServerTemplateData.selectLiabilityServicesAcct(conn, C_BPartner_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_C_Receivable)) {
	        data = AcctServerTemplateData.selectReceivableAcct(conn, C_BPartner_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_UnallocatedCash)) {
	        /** Account Type - Payment */
	        data = AcctServerTemplateData.selectUnallocatedCashAcct(conn, C_BankAccount_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_BankInTransit)) {
	        data = AcctServerTemplateData.selectInTransitAcct(conn, C_BankAccount_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_BankInTransitDefault)) {
	        data = AcctServerTemplateData.selectInTransitDefaultAcct(conn, as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_ConvertChargeDefaultAmt)) {
	        data = AcctServerTemplateData.selectConvertChargeDefaultAmtAcct(conn,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_ConvertGainDefaultAmt)) {
	        data = AcctServerTemplateData.selectConvertGainDefaultAmtAcct(conn,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_PaymentSelect)) {
	        data = AcctServerTemplateData.selectPaymentSelectAcct(conn, C_BankAccount_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_WriteOffDefault)) {
	        data = AcctServerTemplateData.selectWriteOffDefault(conn, as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_WriteOffDefault_Revenue)) {
	        data = AcctServerTemplateData.selectWriteOffDefaultRevenue(conn, as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_DiscountExp)) {
	        /** Account Type - Allocation */
	        data = AcctServerTemplateData.selectDiscountExpAcct(conn, C_BPartner_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_DiscountRev)) {
	        data = AcctServerTemplateData.selectDiscountRevAcct(conn, C_BPartner_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_WriteOff)) {
	        data = AcctServerTemplateData.selectWriteOffAcct(conn, C_BPartner_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_WriteOff_Revenue)) {
	        data = AcctServerTemplateData.selectWriteOffAcctRevenue(conn, C_BPartner_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_ConvertChargeLossAmt)) {
	        /** Account Type - Bank Statement */
	        data = AcctServerTemplateData.selectConvertChargeLossAmt(conn, C_BankAccount_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_ConvertChargeGainAmt)) {
	        data = AcctServerTemplateData.selectConvertChargeGainAmt(conn, C_BankAccount_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_BankAsset)) {
	        data = AcctServerTemplateData.selectAssetAcct(conn, C_BankAccount_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_InterestRev)) {
	        data = AcctServerTemplateData.selectInterestRevAcct(conn, C_BankAccount_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_InterestExp)) {
	        data = AcctServerTemplateData.selectInterestExpAcct(conn, C_BankAccount_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_CashAsset)) {
	        /** Account Type - Cash */
	        data = AcctServerTemplateData.selectCBAssetAcct(conn, C_CashBook_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_CashTransfer)) {
	        data = AcctServerTemplateData.selectCashTransferAcct(conn, C_CashBook_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_CashExpense)) {
	        data = AcctServerTemplateData.selectCBExpenseAcct(conn, C_CashBook_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_CashReceipt)) {
	        data = AcctServerTemplateData.selectCBReceiptAcct(conn, C_CashBook_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_CashDifference)) {
	        data = AcctServerTemplateData.selectCBDifferencesAcct(conn, C_CashBook_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_InvDifferences)) {
	        /** Inventory Accounts */
	        data = AcctServerTemplateData.selectWDifferencesAcct(conn, M_Warehouse_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_NotInvoicedReceipts)) {
	        if (log4j.isDebugEnabled())
	          log4j.debug("AcctServer - getAccount - ACCTYPE_NotInvoicedReceipts - C_BPartner_ID - "
	              + C_BPartner_ID);
	        data = AcctServerTemplateData.selectNotInvoicedReceiptsAcct(conn, C_BPartner_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_ProjectAsset)) {
	        /** Project Accounts */
	        data = AcctServerTemplateData.selectPJAssetAcct(conn, C_Project_ID,
	            as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_ProjectWIP)) {
	        data = AcctServerTemplateData.selectPJWIPAcct(conn, C_Project_ID, as.getC_AcctSchema_ID());
	      } else if (AcctType.equals(ACCTTYPE_PPVOffset)) {
	        /** GL Accounts */
	        data = AcctServerTemplateData.selectPPVOffsetAcct(conn, as.getC_AcctSchema_ID());
	      } else {
	        log4j.warn("AcctServer - getAccount - Not found AcctType=" + AcctType);
	        return null;
	      }
	      // if (log4j.isDebugEnabled())
	      // log4j.debug("AcctServer - *******************************getAccount 3");
	    } catch (ServletException e) {
	      log4j.warn(e);
	      e.printStackTrace();
	    }
	    // Get Acct
	    String Account_ID = "";
	    if (data != null && data.length != 0) {
	      Account_ID = data[0].accountId;
	    } else
	      return null;
	    // No account
	    if (Account_ID.equals("")) {
	      log4j.warn("AcctServer - getAccount - NO account Type=" + AcctType + ", Record=" + Record_ID);
	      return null;
	    }
	    // if (log4j.isDebugEnabled())
	    // log4j.debug("AcctServer - *******************************getAccount 4");
	    // Return Account
	    Account acct = null;
	    try {
	      acct = Account.getAccount(conn, Account_ID);
	    } catch (ServletException e) {
	      log4j.warn(e);
	      e.printStackTrace();
	    }
	    return acct;
	  } // getAccount

	  public String getAmount(int AmtType) {
	    if (AmtType < 0 || Amounts == null || AmtType >= Amounts.length)
	      return null;
	    return (Amounts[AmtType].equals("")) ? "0" : Amounts[AmtType];
	  }

	  public String getAmount() {
	    return Amounts[0];
	  }

	  private DocLines[] loadLines(ConnectionProvider conn) {
		    ArrayList<Object> list = new ArrayList<Object>();
		    DocInventoryLineTemplateData[] data = null;
		    OBContext.setAdminMode(false);
		    try {
		      data = DocInventoryLineTemplateData.select(conn, Record_ID);
		      for (int i = 0; i < data.length; i++) {
		        String Line_ID = data[i].getField("mInventorylineId");
		        ec.com.sidesoft.localization.inventoryaccounting.acc_template.DocLine_Material docLine = new ec.com.sidesoft.localization.inventoryaccounting.acc_template.DocLine_Material(DocumentType, Record_ID, Line_ID);
		        docLine.loadAttributes(data[i],conn);
		        log4jDocInventory.debug("QtyBook = " + data[i].getField("qtybook") + " - QtyCount = "
		            + data[i].getField("qtycount"));
		        BigDecimal QtyBook = new BigDecimal(data[i].getField("qtybook"));
		        BigDecimal QtyCount = new BigDecimal(data[i].getField("qtycount"));
		        docLine.setQty((QtyCount.subtract(QtyBook)).toString(), conn);
		        docLine.m_M_Locator_ID = data[i].getField("mLocatorId");
		        // Get related M_Transaction_ID
		        InventoryCountLine invLine = OBDal.getInstance().get(InventoryCountLine.class, Line_ID);
		        if (invLine.getMaterialMgmtMaterialTransactionList().size() > 0) {
		          docLine.setTransaction(invLine.getMaterialMgmtMaterialTransactionList().get(0));
		        }
		        DocInventoryTemplateData[] data1 = null;
		        try {
		          data1 = DocInventoryTemplateData.selectWarehouse(conn, docLine.m_M_Locator_ID);
		        } catch (ServletException e) {
		          log4jDocInventory.warn(e);
		        }
		        if (data1 != null && data1.length > 0)
		          this.M_Warehouse_ID = data1[0].mWarehouseId;
		        // Set Charge ID only when Inventory Type = Charge
		        if (!"C".equals(data[i].getField("inventorytype")))
		          docLine.m_C_Charge_ID = "";
		        //
		        list.add(docLine);
		      }
		    } catch (ServletException e) {
		      log4jDocInventory.warn(e);
		    } finally {
		      OBContext.restorePreviousMode();
		    }
		    // Return Array
		    DocLines[] dl = new DocLines[list.size()];
		    list.toArray(dl);
		    return dl;
		  } // loadLines
	  public String getServletInfo() {
		    return "Servlet for the accounting";
		  } // end of getServletInfo() method
}
