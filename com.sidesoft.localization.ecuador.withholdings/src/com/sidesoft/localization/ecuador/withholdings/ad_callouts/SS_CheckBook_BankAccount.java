/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2001-2009 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ___Santos- 15112011-1310_______.
 ************************************************************************
 */
//package org.openbravo.erpCommon.ad_callouts;
package com.sidesoft.localization.ecuador.withholdings.ad_callouts;


import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Expression;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
//import org.openbravo.erpCommon.businessUtility.BpartnerMiscData;
//importar el xsql - en estecaso desde eldirectorio - pakete

import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.xmlEngine.XmlDocument;

public class SS_CheckBook_BankAccount extends HttpSecureAppServlet 
{
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }
  
//recibe la llamada al servelet
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,ServletException 
  {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) 
    {
      String strChanged = vars.getStringParameter("inpLastFieldChanged"); //recupera el valor de la pgina web
	  
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      String strBankAccount = vars.getStringParameter("inpcBankaccountId");
	  
	   try {
	        printPage(response, vars, strBankAccount);
	       } 
	   catch (ServletException ex) 
	       {
	        pageErrorCallOut(response);
	       }
    } 
    else
    	  pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strBankAccount) throws IOException, ServletException 
  {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    SSCheckBookBankAccountData[] data = SSCheckBookBankAccountData.select(this, strBankAccount );
    StringBuffer resultado = new StringBuffer();
    
    resultado.append("var calloutName='SS_CheckBook_BankAccount';\n\n");
    resultado.append("var respuesta = new Array(");
    if (data != null && data.length > 0) 
    {
//      resultado.append("new Array(\"inpidentify\", \"" + data[0].emssctidentify + "\")\n");
    	resultado.append("new Array(\"inpgenericaccount\", \"" + data[0].genericaccount + "\")\n");
    }
    resultado.append(");\n");
      //resultado.append("new Array(\"inpcCurrencyId\", \"" + data[0].cCurrencyId + "\")\n");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}