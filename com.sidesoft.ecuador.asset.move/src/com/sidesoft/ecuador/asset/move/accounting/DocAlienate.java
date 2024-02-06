package com.sidesoft.ecuador.asset.move.accounting;

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
import org.openbravo.erpCommon.ad_forms.DocLine;
import org.openbravo.erpCommon.ad_forms.Fact;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;


/**
 * MODULARIZACION CONTABILIDAD ENAJENACIÓN DE ACTIVOS
 * CREADO POR: ING DIEGO ARMANDO GUALLASAMIN COLUMBA
 * FECHA DE CREACION: 8 DE MARZO DEL 2016
 */

public class DocAlienate extends AcctServer {
	private static final long serialVersionUID = 1L;
	private String SeqNo = "0";
	protected Logger logger = Logger.getLogger(this.getClass());
	static Logger log4jDocAlienate= Logger.getLogger(DocAlienate.class);
	public String strMessage = null;
	
	private static final String ASS_DEPRECIATION_ACCT = "1";
	private static final String ASS_ACCUMDEPRECIATION_ACCT = "2";
	private static final String ASS_SALES_ACCT = "3";
	private static final String ASS_HISTORICCOST_ACCT = "4" ;
	private static final String ASS_RESULTALIENATE_ACCT = "5";
	private static final String GRP_DEPRECIATION_ACCT = "6";
	private static final String GRP_ASACCUMDEPRECIATION_ACCT = "7";
	private static final String GRP_ASSALES_ACCT = "8";
	private static final String GRP_ASHISTORICCOST_ACCT = "9";
	private static final String GRP_ASRESULTALIENATE_ACCT = "10";

	//Final Settlement
	  public static final String DOCTYPE_Alienate = "SSAM_ALT";
	  
	  public DocAlienate() {
	  }
	 
	  public DocAlienate(String AD_Client_ID, String AD_Org_ID, ConnectionProvider connectionProvider) {
		    super(AD_Client_ID, AD_Org_ID, connectionProvider);
	  }
	 
	  public String nextSeqNo(String oldSeqNo) {
			    log4jDocAlienate.debug("DocSettlement - oldSeqNo = " + oldSeqNo);
			    BigDecimal seqNo = new BigDecimal(oldSeqNo);
			    SeqNo = (seqNo.add(new BigDecimal("10"))).toString();
			    log4jDocAlienate.debug("DocSettlement - nextSeqNo = " + SeqNo);
			    return SeqNo;
	  }
	 
	  public void loadObjectFieldProvider(ConnectionProvider conn, String stradClientId, String Id)
			    throws ServletException {
			    setObjectFieldProvider(DocAlienateData.selectRecord(conn, stradClientId, Id));
	  }

	  @Override
	  public boolean loadDocumentDetails(FieldProvider[] data, ConnectionProvider conn) {
	    DocumentType = DOCTYPE_Alienate; 
	    log4jDocAlienate.debug("data.length = " + data.length + " - DocumentType = " + DocumentType);

	    // Amounts
	    Amounts[AcctServer.AMTTYPE_Gross] = data[0].getField("Value");
	    if (Amounts[AcctServer.AMTTYPE_Gross] == null)
	      Amounts[AcctServer.AMTTYPE_Gross] = ZERO.toString();

	    loadDocumentType(); // lines require doc type

	    //Contained Objects
	    p_lines = loadLines(conn);
	    log4jDocAlienate.debug("Lines=" + p_lines.length);
	    return true;
	  } // loadDocumentDetails
	
	  private DocLine[] loadLines(ConnectionProvider conn) {
		    ArrayList<Object> list = new ArrayList<Object>();
		    DocLineAlienateData[] data = null;	
		    
		    try {
		      data = DocLineAlienateData.select(conn, Record_ID);
		    } catch (ServletException e) {
		    	log4jDocAlienate.warn(e);
		    }
		  
		    for (int i = 0; data != null && i < data.length; i++) {
		      String Line_ID = data[i].ssamAlienatelineId;
		      DocLine_Alienate docLine = new DocLine_Alienate(DocumentType, Record_ID, Line_ID);
		      docLine.loadAttributes(data[i], this);
		      docLine.m_a_asset_id = data[i].aAssetId;
		      docLine.m_a_asset_group_id = data[i].aAssetGroupId;
		      docLine.m_typereason = data[i].typereason;
		      docLine.m_assetvalueamt = data[i].assetvalueamt;
		      docLine.m_amortizationvalue = data[i].amortizationvalue;
		      docLine.m_netvalue = data[i].netvalue;
		      list.add(docLine);
		    }

		    // Return Array
		    DocLine[] dl = new DocLine[list.size()];
		    list.toArray(dl);
		    return dl;
	  } // loadLines
	 
	  public Fact createFact(AcctSchema as, ConnectionProvider conn, Connection con,
		      VariablesSecureApp vars) throws ServletException {
		  log4jDocAlienate.debug("createFact - Inicio");
	
		    // create Fact Header
		    Fact fact = null;
		    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
		    log4jDocAlienate.debug("createFact - object created");
		    log4jDocAlienate.debug("createFact - p_lines.length - " + p_lines.length);
		    // Lines
		    fact = new Fact(this, as, Fact.POST_Actual);
		    for (int i = 0; p_lines != null && i < p_lines.length; i++) {
		    	DocLine_Alienate line = (DocLine_Alienate) p_lines[i];
		    	
		    	if(line.m_typereason.equals("S")) {
		    		log4jDocAlienate.debug("createFact - sales asset");
		    		   fact.createLine(line, getAccount(ASS_ACCUMDEPRECIATION_ACCT, line.m_a_asset_id, as, conn),
		    			          line.m_C_Currency_ID, line.m_amortizationvalue, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
		    			          DocumentType, conn);
		    		   fact.createLine(line, getAccount(ASS_SALES_ACCT, line.m_a_asset_id, as, conn),
		    			          line.m_C_Currency_ID, line.m_netvalue, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
		    			          DocumentType, conn);
		    		   fact.createLine(line, getAccount(ASS_HISTORICCOST_ACCT, line.m_a_asset_id, as, conn),
		    			          line.m_C_Currency_ID, "", line.m_assetvalueamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
		    			          DocumentType, conn);
		    		   DocAlienateAcctData.updateStatusAsset(conn, "W",line.m_a_asset_id);

		    	}
		    	if(line.m_typereason.equals("L")){
		    		log4jDocAlienate.debug("createFact - low asset");
		    		fact.createLine(line, getAccount(ASS_RESULTALIENATE_ACCT, line.m_a_asset_id, as, conn),
	    			          line.m_C_Currency_ID, line.m_netvalue, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
	    			          DocumentType, conn);
		    		fact.createLine(line, getAccount(ASS_ACCUMDEPRECIATION_ACCT, line.m_a_asset_id, as, conn),
	    			          line.m_C_Currency_ID, line.m_amortizationvalue, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
	    			          DocumentType, conn);
		    		fact.createLine(line, getAccount(ASS_HISTORICCOST_ACCT, line.m_a_asset_id, as, conn),
	    			          line.m_C_Currency_ID, "", line.m_assetvalueamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
	    			          DocumentType, conn);
		    		   DocAlienateAcctData.updateStatusAsset(conn, line.m_typereason,line.m_a_asset_id);

		    	}
		    	if(line.m_typereason.equals("D")){
		    		log4jDocAlienate.debug("createFact - Delivery other entity/Institution asset");
		    		fact.createLine(line, getAccount(ASS_RESULTALIENATE_ACCT, line.m_a_asset_id, as, conn),
	    			          line.m_C_Currency_ID, line.m_netvalue, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
	    			          DocumentType, conn);
		    		fact.createLine(line, getAccount(ASS_ACCUMDEPRECIATION_ACCT, line.m_a_asset_id, as, conn),
	    			          line.m_C_Currency_ID, line.m_amortizationvalue, "", Fact_Acct_Group_ID, nextSeqNo(SeqNo),
	    			          DocumentType, conn);
		    		fact.createLine(line, getAccount(ASS_HISTORICCOST_ACCT, line.m_a_asset_id, as, conn),
	    			          line.m_C_Currency_ID, "", line.m_assetvalueamt, Fact_Acct_Group_ID, nextSeqNo(SeqNo),
	    			          DocumentType, conn);
		    		   DocAlienateAcctData.updateStatusAsset(conn, line.m_typereason,line.m_a_asset_id);

		    	}
		
		   
		    }
		    SeqNo = "0";
		    return fact;
		  } // createFact

	  public Account getAccount(String AcctType, String A_Asset_ID, AcctSchema as,
		      ConnectionProvider conn) {
		    if (Integer.parseInt(AcctType) < 1 || Integer.parseInt(AcctType) > 10)
		      return null;
		 
		    DocAlienateAcctData[] data = null;
		    Account acc = null;
		    try {
		      data = DocAlienateAcctData.selectAcct(conn, A_Asset_ID, as.getC_AcctSchema_ID());
		      if (data == null || data.length == 0)
		        return null;
		     
		      String validCombination_ID = "";
		      switch (Integer.parseInt(AcctType)) {
		      case 1:
		    	  	validCombination_ID = data[0].assDepreciationAcct;
		    	  		if(validCombination_ID == null || validCombination_ID.equals("")){
			    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
		                    strMessage = Utility.messageBD(conn, "Asset: Amortization Account is null " + data[0].nameassets, language);
		                    OBError err = new OBError();
		                    err.setType("Error");
		                    err.setMessage(strMessage);
		                    setMessageResult(err);
			    	  	}
		        break;
		      case 2:
		    	  	validCombination_ID = data[0].assAccumdepreciationAcct;
		    	  	if(validCombination_ID == null || validCombination_ID.equals("")){
		    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
	                    strMessage = Utility.messageBD(conn, "Asset: Amortization Account Accum is null " + data[0].nameassets, language);
	                    OBError err = new OBError();
	                    err.setType("Error");
	                    err.setMessage(strMessage);
	                    setMessageResult(err);
		    	  	}
		        break;
		      case 3:
			        validCombination_ID = data[0].assSalesAcct;
			        if(validCombination_ID == null || validCombination_ID.equals("")){
		    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
	                    strMessage = Utility.messageBD(conn, "Asset: Sales Account is null " + data[0].nameassets, language);
	                    OBError err = new OBError();
	                    err.setType("Error");
	                    err.setMessage(strMessage);
	                    setMessageResult(err);
		    	  	}
			        break;
		      case 4:
			        validCombination_ID = data[0].assHistoriccostAcct;
			        if(validCombination_ID == null || validCombination_ID.equals("")){
		    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
	                    strMessage = Utility.messageBD(conn, "Asset: Historic Account is null " + data[0].nameassets, language);
	                    OBError err = new OBError();
	                    err.setType("Error");
	                    err.setMessage(strMessage);
	                    setMessageResult(err);
		    	  	}
			        break;
		      case 5:
			        validCombination_ID = data[0].assResultalienateAcct;
			        if(validCombination_ID == null || validCombination_ID.equals("")){
		    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
	                    strMessage = Utility.messageBD(conn, "Asset: Result Alienate Account is null " + data[0].nameassets, language);
	                    OBError err = new OBError();
	                    err.setType("Error");
	                    err.setMessage(strMessage);
	                    setMessageResult(err);
		    	  	}
			        break;
		      case 6:
			        validCombination_ID = data[0].grpDepreciationAcct;
			        if(validCombination_ID == null || validCombination_ID.equals("")){
		    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
	                    strMessage = Utility.messageBD(conn, "Asset Group: Amortization Account is null " + data[0].nameassets, language);
	                    OBError err = new OBError();
	                    err.setType("Error");
	                    err.setMessage(strMessage);
	                    setMessageResult(err);
		    	  	}
			        break;
		      case 7:
			        validCombination_ID = data[0].grpAccumdepreciationAcct;
			        if(validCombination_ID == null || validCombination_ID.equals("")){
		    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
	                    strMessage = Utility.messageBD(conn, "Asset Group: Amortization Account Accum is null " + data[0].nameassets, language);
	                    OBError err = new OBError();
	                    err.setType("Error");
	                    err.setMessage(strMessage);
	                    setMessageResult(err);
			        }
			        break;
		      case 8:
			        validCombination_ID = data[0].grpSalesAcct;
			        if(validCombination_ID == null || validCombination_ID.equals("")){
		    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
	                    strMessage = Utility.messageBD(conn, "Asset Group: Sales Account is null " + data[0].nameassets, language);
	                    OBError err = new OBError();
	                    err.setType("Error");
	                    err.setMessage(strMessage);
	                    setMessageResult(err);
		    	  	}
			        break;
		      case 9:
			        validCombination_ID = data[0].grpHistoriccostAcct;
			        if(validCombination_ID == null || validCombination_ID.equals("")){
		    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
	                    strMessage = Utility.messageBD(conn, "Asset Group: Historic Account is null " + data[0].nameassets, language);
	                    OBError err = new OBError();
	                    err.setType("Error");
	                    err.setMessage(strMessage);
	                    setMessageResult(err);
		    	  	}
			        break;
		      case 10:
			        validCombination_ID = data[0].grpResultalienateAcct;
			        if(validCombination_ID == null || validCombination_ID.equals("")){
		    	  		String language = OBContext.getOBContext().getLanguage().getLanguage();
	                    strMessage = Utility.messageBD(conn, "Asset Group: Result Alienate Account is null " + data[0].nameassets, language);
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
		      log4jDocAlienate.debug("DocAmortization - getAccount - " + acc.Account_ID);
		    } catch (ServletException e) {
		    	log4jDocAlienate.warn(e);
		    }
		    return acc;
	} // getAccount

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
