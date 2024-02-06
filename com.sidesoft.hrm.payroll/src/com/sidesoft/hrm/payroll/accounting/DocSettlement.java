package com.sidesoft.hrm.payroll.accounting;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.Account;
import org.openbravo.erpCommon.ad_forms.AcctSchema;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import com.sidesoft.hrm.payroll.accounting.ConceptInfo;
import com.sidesoft.hrm.payroll.accounting.ConceptInfoData;
import com.sidesoft.hrm.payroll.accounting.ConceptInfoSettlementData;
import org.openbravo.erpCommon.ad_forms.DocLine;
import com.sidesoft.hrm.payroll.accounting.DocLineSettlementData;
import com.sidesoft.hrm.payroll.accounting.DocLine_Settlement;
import org.openbravo.erpCommon.ad_forms.Fact;
import com.sidesoft.hrm.payroll.accounting.PayrollCategoryAcctData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;

/**
 * MODULARIZACION CONTABILIDAD LIQUIDACION DE EMPLEADOS
 * CREADO POR: ING DIEGO ARMANDO GUALLASAMIN COLUMBA
 * FECHA DE CREACION: 7 DE MARZO DEL 2016
 */

public class DocSettlement extends AcctServer {
	private static final long serialVersionUID = 1L;
	private String SeqNo = "0";
	protected Logger logger = Logger.getLogger(this.getClass());
	static Logger log4jDocSettlement = Logger.getLogger(DocSettlement.class);
	public String strMessage = null;

	//Final Settlement
	  public static final String DOCTYPE_FinalSettlement = "SSPR_FST";
	  
	  public DocSettlement() {
	  }
	 
	  public DocSettlement(String AD_Client_ID, String AD_Org_ID, ConnectionProvider connectionProvider) {
		    super(AD_Client_ID, AD_Org_ID, connectionProvider);
	  }
	 
	  public String nextSeqNo(String oldSeqNo) {
			    log4jDocSettlement.debug("DocSettlement - oldSeqNo = " + oldSeqNo);
			    BigDecimal seqNo = new BigDecimal(oldSeqNo);
			    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
			    log4jDocSettlement.debug("DocSettlement - nextSeqNo = " + SeqNo);
			    return SeqNo;
	  }
	 
	  public void loadObjectFieldProvider(ConnectionProvider conn, String stradClientId, String Id)
			    throws ServletException {
			    setObjectFieldProvider(DocSettlementData.selectRecord(conn, stradClientId, Id));
	  }

	  @Override
	  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
	    DocumentType = DOCTYPE_FinalSettlement; 
	    log4jDocSettlement.debug("data.length = " + data.length + " - DocumentType = " + DocumentType);

	    // Amounts
	    Amounts[AcctServer.AMTTYPE_Gross] = data[0].getField("Value");
	    if (Amounts[AcctServer.AMTTYPE_Gross] == null)
	      Amounts[AcctServer.AMTTYPE_Gross] = ZERO.toString();

	    loadDocumentType(); // lines require doc type

	    //Contained Objects
	    p_lines = loadLines(conn);
	    log4jDocSettlement.debug("Lines=" + p_lines.length);
	    return true;
	  } // loadDocumentDetails
	
	 private DocLine[] loadLines(ConnectionProvider conn) {
		    ArrayList<Object> list = new ArrayList<Object>();
		    DocLineSettlementData[] data = null;
		    Integer line = 10;

		    try {
		      data = DocLineSettlementData.selectline(connectionProvider, Record_ID, Record_ID);
		      for (int i = 0; data != null && i < data.length; i++) {
		    	  String t_Line_ID = data[i].ssprSettlementlineId;
		        DocLine_Settlement docLine = new DocLine_Settlement(DocumentType, Record_ID,
		            t_Line_ID);
		        docLine.loadAttributes(data[i], this);
		        docLine.m_SSPR_Concept_ID = data[i].ssprConceptId;
		        docLine.m_SSPR_Category_Acct_ID = data[i].ssprCategoryAcctId;
		        docLine.c_conceptInfo = new ConceptInfo(docLine.m_SSPR_Concept_ID, conn);
		        docLine.setAmount(data[i].totalnet);
		        docLine.m_IsBalance = "Y";
		        docLine.m_IsComplete = data[i].iscomplete;
		        docLine.m_NameConcept = data[i].nameconcept;
		        list.add(docLine);
		        line += 10;
		      }
		    } catch (ServletException e) {
		    	log4jDocSettlement.warn(e);
		    }
		    // Return Array
		    DocLine[] dl = new DocLine[list.size()];
		    list.toArray(dl);
		    return dl;
	 } // loadLines
	 
	 @SuppressWarnings("unused")
	 public Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
	       VariablesSecureApp vars) throws ServletException {

	     log4jDocSettlement.debug("Starting create fact");

	     // create Fact Header
	     Fact fact = new Fact(this, as, Fact.POST_Actual);

	     String Fact_Acct_Group_ID = SequenceIdData.getUUID();
	     Double amount = 0.0;

	     ConceptInfoData[] data = null;
	     ConceptInfoSettlementData[] datatypeconcept = null;
	     
	     // Lines
	     for (int i = 0; p_lines != null && i < p_lines.length; i++) {
	       DocLine_Settlement line = (DocLine_Settlement) p_lines[i];

	       try {
	         if (line.m_IsComplete.equals("N")) {
	         	  
	             data = ConceptInfoData.selectConceptAcctDetails(conn, line.m_SSPR_Concept_ID,
	                 as.getC_AcctSchema_ID(), line.m_SSPR_Category_Acct_ID);
	             
	             if (data.length > 0) {
	             	
	             	datatypeconcept = ConceptInfoSettlementData.selectConceptType(conn, line.m_SSPR_Concept_ID);
	             	
	             	if(datatypeconcept[0].conceptsubtype.equals("In")){
	             			Account account = ((DocLine_Settlement) p_lines[i]).getAccount(
	                             ConceptInfo.ACCTTYPE_C_Credit, as, line.m_SSPR_Category_Acct_ID, conn);

	             			if(account != null){
	             				 fact.createLine(line, account, C_Currency_ID, line.getAmount(), "",
	                                      Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);

	                                  amount += Double.parseDouble(line.getAmount());
	             			} else{
	             				String language = OBContext.getOBContext().getLanguage().getLanguage();
	                             strMessage = Utility.messageBD(conn, "No found account for the concept " + line.m_NameConcept, language);
	                             OBError err = new OBError();
	                             err.setType("Error");
	                             err.setMessage(strMessage);
	                             setMessageResult(err);
	             			}
	                        
	             	} else {
	             		
	             		Account account = ((DocLine_Settlement) p_lines[i]).getAccount(
	                             ConceptInfo.ACCTTYPE_C_Credit, as, line.m_SSPR_Category_Acct_ID, conn);

	             		if(account != null){
	             			
	             			fact.createLine(line, account, C_Currency_ID,"", line.getAmount(),
	                                 Fact_Acct_Group_ID, nextSeqNo(SeqNo), DocumentType, conn);

	                             amount -= Double.parseDouble(line.getAmount());
	             		} else {
	             			 String language = OBContext.getOBContext().getLanguage().getLanguage();
	                          strMessage = Utility.messageBD(conn, "No found account for the concept " + line.m_NameConcept, language);
	                          OBError err = new OBError();
	                          err.setType("Error");
	                          err.setMessage(strMessage);
	                          setMessageResult(err);
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
	           
	         } else {
	             PayrollCategoryAcctData[] prCatAcct = null;
	             prCatAcct = PayrollCategoryAcctData.select(connectionProvider, line.m_C_BPartner_ID);
	             Account account = new Account(connectionProvider, prCatAcct[0].balanceacctId);
	             
	             if(account != null){
	             	  line.setAmount(amount.toString());
	                   fact.createLine(line, account , C_Currency_ID, "", line.getAmount(), Fact_Acct_Group_ID,
	                       nextSeqNo(SeqNo), DocumentType, conn);
	                   amount = 0.0;
	             } else {
	             	              	
	                 	String language = OBContext.getOBContext().getLanguage().getLanguage();
	                     strMessage = Utility.messageBD(conn, "noaccountcategory", language);
	                     OBError err = new OBError();
	                     err.setType("Error");
	                     err.setMessage(strMessage);
	                     setMessageResult(err);
	             }          
	         }
	       } catch (ServletException e) {
	         log4jDocSettlement.warn(e);
	       }
	     }	

	     //FactLine closing = fact.balanceSource(conn);
	     //closing.setAccount(as, ConceptInfo.getAccountDefault(ConceptInfo.ACCTTYPE_C_Closing, as, conn));

	     SeqNo = "0";
	     return fact;

	}// createFact

	@Override
	public BigDecimal getBalance() {
	    return ZERO; // Lines are balanced
	}
	
	public boolean getDocumentConfirmation(ConnectionProvider conn, String strRecordId) {
	    return true;
	}
	
	public String getServletInfo() {
	    return "Servlet for the accounting";
	} // end of getServletInfo() method

}
