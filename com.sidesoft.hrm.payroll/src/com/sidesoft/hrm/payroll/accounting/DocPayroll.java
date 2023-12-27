package com.sidesoft.hrm.payroll.accounting;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.Account;
import org.openbravo.erpCommon.ad_forms.AcctSchema;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.ad_forms.DocLine;
import org.openbravo.erpCommon.ad_forms.Fact;
import org.openbravo.erpCommon.ad_forms.FactLine;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import com.sidesoft.hrm.payroll.SsprGeneralParametersPayroll;

public class DocPayroll extends AcctServer {
  private static final long serialVersionUID = 1L;
  private String SeqNo = "0";
  protected Logger logger = Logger.getLogger(this.getClass());
  private DocPayrollData[] headerData = null;
  String strMessage = null;

  static Logger log4jDocPayroll = Logger.getLogger(DocPayroll.class);

  /** Budget Certificate */
  public static final String DOCTYPE_Payroll = "SFB_BC";

  /**
   * Constructor
   * 
   * @param AD_Client_ID
   *          AD_Client_ID
   */

  public DocPayroll() {

  }

  public DocPayroll(String AD_Client_ID, String AD_Org_ID, ConnectionProvider connectionProvider) {
    super(AD_Client_ID, AD_Org_ID, connectionProvider);
  }

  public String nextSeqNo(String oldSeqNo) {
    log4jDocPayroll.debug("DocPayroll - oldSeqNo = " + oldSeqNo);
    BigDecimal seqNo = new BigDecimal(oldSeqNo);
    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
    log4jDocPayroll.debug("DocPayroll - nextSeqNo = " + SeqNo);
    return SeqNo;
  }

  public void loadObjectFieldProvider(ConnectionProvider conn, String stradClientId, String Id)
      throws ServletException {
    setObjectFieldProvider(DocPayrollData.selectRecord(conn, stradClientId, Id));
  }

  /**
   * Load Specific Document Details
   * 
   * @return true if loadDocumentType was set
   */
  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
    DocumentType = DOCTYPE_Payroll;
    log4jDocPayroll.debug("data.length = " + data.length + " - DocumentType = " + DocumentType);

    // Amounts
    Amounts[AcctServer.AMTTYPE_Gross] = data[0].getField("Value");
    if (Amounts[AcctServer.AMTTYPE_Gross] == null)
      Amounts[AcctServer.AMTTYPE_Gross] = ZERO.toString();

    loadDocumentType(); // lines require doc type

    // Contained Objects
    p_lines = loadLines(conn);
    log4jDocPayroll.debug("Lines=" + p_lines.length);
    return true;
  } // loadDocumentDetails

  /**
   * Load Budget Certificate Line
   * 
   * @return DocLine Array
   */
  private DocLine[] loadLines(ConnectionProvider conn) {
    ArrayList<Object> list = new ArrayList<Object>();
    DocLinePayrollData[] data = null;
    try {
      String businessPartner = null;
      Double amount = 0.0;
      Integer line = 10;
      PayrollCategoryAcctPayrollData[] prCatAcct = null;
      headerData = DocPayrollData.selectRecord(connectionProvider, AD_Client_ID, Record_ID);
      if (headerData.length == 0) {
        String language = OBContext.getOBContext().getLanguage().getLanguage();
        strMessage = Utility.messageBD(conn, "NoRecordsFound", language);
        OBError err = new OBError();
        err.setType("Error");
        err.setMessage(strMessage);
        setMessageResult(err);
      }
      data = DocLinePayrollData.select(connectionProvider, Record_ID);

      // Data Payroll of the Employee by Cost Center
      DocLinePayrollCostCenterData[] dataCostCenter = null;

      // Setting Parameters Payroll

      OBCriteria<SsprGeneralParametersPayroll> GenealPayrollParamCriteria = OBDal.getInstance()
          .createCriteria(SsprGeneralParametersPayroll.class);
      GenealPayrollParamCriteria
          .add(Restrictions.eq(SsprGeneralParametersPayroll.PROPERTY_ACTIVE, true));

      boolean postedByCostCenter = false;

      if (GenealPayrollParamCriteria.count() > 0) {
        List<SsprGeneralParametersPayroll> lstGenPayParm = GenealPayrollParamCriteria.list();

        postedByCostCenter = lstGenPayParm.get(0).isProcessingByCostcenter();
      }

      for (int i = 0; data != null && i < data.length; i++) {
        String t_Line_ID = data[i].ssprPayrollTicketConceptId;
        if (businessPartner != null && !data[i].cBpartnerId.equals(businessPartner)) {
          prCatAcct = PayrollCategoryAcctPayrollData.select(connectionProvider, businessPartner);
          if (prCatAcct.length > 0 && !prCatAcct[0].balanceacctId.equals("")) {
            DocLine_Payroll docLine = new DocLine_Payroll(DocumentType, Record_ID, null);
            Account balanceAccount = new Account(connectionProvider, prCatAcct[0].balanceacctId);
            docLine.loadAttributes(headerData[0], this);
            docLine.setAccount(balanceAccount);
            docLine.m_IsBalance = "Y";
            docLine.m_Line = line.toString();
            line += 10;
            docLine.setAmount(amount.toString());
            list.add(docLine);
          } else {
            String language = OBContext.getOBContext().getLanguage().getLanguage();
            strMessage = Utility.messageBD(conn, "20270", language);
            OBError err = new OBError();
            err.setType("Error");
            err.setMessage(strMessage);
            setMessageResult(err);
          }
        }
        businessPartner = data[i].cBpartnerId;

        dataCostCenter = DocLinePayrollCostCenterData.selected(conn, Record_ID, businessPartner,
            data[i].ssprConceptId);

        if (dataCostCenter.length > 0 && postedByCostCenter) {

          int intCursor = i;

          for (int j = 0; j < dataCostCenter.length; j++) {
            DocLine_Payroll docLine = new DocLine_Payroll(DocumentType, Record_ID, t_Line_ID);

            docLine.loadAttributes(data[intCursor], this);
            docLine.m_SSPR_Concept_ID = data[intCursor].ssprConceptId;
            docLine.m_SSPR_Category_Acct_ID = dataCostCenter[j].catgAcct;
            docLine.c_conceptInfo = new ConceptInfo(docLine.m_SSPR_Concept_ID, conn);
            docLine.m_C_Costcenter_ID = dataCostCenter[j].cCostcenterId;
            docLine.m_Line = line.toString();
            line += 10;
            docLine.setAmount(dataCostCenter[j].amount);
            list.add(docLine);
          }

        } else {
          DocLine_Payroll docLine = new DocLine_Payroll(DocumentType, Record_ID, t_Line_ID);

          docLine.loadAttributes(data[i], this);
          docLine.m_SSPR_Concept_ID = data[i].ssprConceptId;
          docLine.m_SSPR_Category_Acct_ID = data[i].ssprCategoryAcctId;
          docLine.c_conceptInfo = new ConceptInfo(docLine.m_SSPR_Concept_ID, conn);

          String strCostCenterID = "ERROR";
          try {
            strCostCenterID = (data[i].cCostcenterId.equals(null)
                || data[i].cCostcenterId.equals("")) ? "ERROR" : data[i].cCostcenterId;
          } catch (Exception e10) {

          }
          if (strCostCenterID.equals("ERROR")) {
            BusinessPartner obdalPartner = OBDal.getInstance().get(BusinessPartner.class,
                businessPartner);

            String language = OBContext.getOBContext().getLanguage().getLanguage();
            strMessage = Utility.messageBD(conn, "El Empleado " + obdalPartner.getName()
                + "  no tiene configurado el Centro de Costo", language);
            OBError err = new OBError();
            err.setType("Error");
            err.setMessage(strMessage);
            setMessageResult(err);
          }
          docLine.m_C_Costcenter_ID = data[i].cCostcenterId;
          docLine.m_Line = line.toString();
          line += 10;
          docLine.setAmount(data[i].amount);
          list.add(docLine);
        }

      }
    } catch (ServletException e) {
      log4jDocPayroll.warn(e);
    }
    // Return Array
    DocLine[] dl = new DocLine[list.size()];
    list.toArray(dl);
    return dl;
  } // loadLines

  public Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
      VariablesSecureApp vars) throws ServletException {

    log4jDocPayroll.debug("Starting create fact");

    // create Fact Header
    Fact fact = new Fact(this, as, Fact.POST_Actual);

    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    Double amount = 0.0;

    ConceptInfoData[] data = null;
    // Lines
    for (int i = 0; p_lines != null && i < p_lines.length; i++) {
      DocLine_Payroll line = (DocLine_Payroll) p_lines[i];

      try {
        if (!line.m_IsBalance.equals("Y")) {
          if (Double.parseDouble(line.getAmount()) != 0.0) {
            data = ConceptInfoData.selectConceptAcctDetails(conn, line.m_SSPR_Concept_ID,
                as.getC_AcctSchema_ID(), line.m_SSPR_Category_Acct_ID);
            if (data.length > 0) {
              if (data[0].isaccountpayroll.equals("Y")) {
                Account account = ((DocLine_Payroll) p_lines[i]).getAccount(
                    ConceptInfo.ACCTTYPE_C_Debit, as, line.m_SSPR_Category_Acct_ID, conn);

                fact.createLine(line, account, C_Currency_ID, line.getAmount(), "",
                    Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);

                account = ((DocLine_Payroll) p_lines[i]).getAccount(ConceptInfo.ACCTTYPE_C_Credit,
                    as, line.m_SSPR_Category_Acct_ID, conn);

                if (account != null) {
                  fact.createLine(line, account, C_Currency_ID, "", line.getAmount(),
                      Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);
                }
              } else {
                if (data[0].isliability.equals("Y")) {
                  Account account = ((DocLine_Payroll) p_lines[i]).getAccount(
                      ConceptInfo.ACCTTYPE_C_Debit, as, line.m_SSPR_Category_Acct_ID, conn);

                  fact.createLine(line, account, C_Currency_ID, "", line.getAmount(),
                      Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);

                  amount -= Double.parseDouble(line.getAmount());
                } else {
                  Account account = ((DocLine_Payroll) p_lines[i]).getAccount(
                      ConceptInfo.ACCTTYPE_C_Debit, as, line.m_SSPR_Category_Acct_ID, conn);

                  fact.createLine(line, account, C_Currency_ID, line.getAmount(), "",
                      Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);

                  amount += Double.parseDouble(line.getAmount());
                }
              }
            } else {
              String language = OBContext.getOBContext().getLanguage().getLanguage();
              strMessage = Utility.messageBD(conn, "SSPR_NoConceptAcct", language);
              OBError err = new OBError();
              err.setType("Error");
              err.setMessage(strMessage);
              setMessageResult(err);
            }
          }
        } else {
          Account account = line.getAccount();
          line.setAmount(amount.toString());
          fact.createLine(line, account, C_Currency_ID, "", line.getAmount(), Fact_Acct_Group_ID,
              nextSeqNo(SeqNo), DocumentType, conn);
          amount = 0.0;
        }
      } catch (ServletException e) {
        log4jDocPayroll.warn(e);
      }
    }

    FactLine closing = fact.balanceSource(conn);
    closing.setAccount(as, ConceptInfo.getAccountDefault(ConceptInfo.ACCTTYPE_C_Closing, as, conn));

    SeqNo = "0";
    return fact;

  }// createFact

  /**
   * Get Source Currency Balance - subtracts line amounts from total - no rounding
   * 
   * @return positive amount, if total invoice is bigger than lines
   */
  public BigDecimal getBalance() {
    return ZERO; // Lines are balanced
  } // getBalance

  /**
   * Get Document Confirmation
   * 
   * not used
   */
  public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
    return true;
  }

  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method
}