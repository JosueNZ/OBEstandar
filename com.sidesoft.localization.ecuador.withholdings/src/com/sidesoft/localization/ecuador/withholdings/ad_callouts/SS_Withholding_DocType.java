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
 * All portions are Copyright (C) 2001-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.sidesoft.localization.ecuador.withholdings.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;

public class SS_Withholding_DocType extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strWithholdingDocType = vars.getStringParameter("inpemSswhCDoctypeId");
      String strDateWithhold = vars.getStringParameter("inpemSswhDatewithhold");
      String strTabId = vars.getStringParameter("inpTabId");
      String strCInvoiceId = vars.getStringParameter("inpcInvoiceId");

      try {
        printPage(response, vars, strWithholdingDocType, strDateWithhold, strTabId, strCInvoiceId);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strWithholdingDocType, String strDateWithhold, String strTabId, String strCInvoiceId)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    SSWithholdingDocTypeData[] data = SSWithholdingDocTypeData.select(this, strWithholdingDocType,
        strDateWithhold);

    StringBuffer resultado = new StringBuffer();

    resultado.append("var calloutName='SS_Withholding_DocType';\n\n");
    resultado.append("var respuesta = new Array(");

    if (data == null || data.length == 0) {
      resultado.append("new Array(\"inpemSswhWithholdingref\", \"\"),");
      resultado.append("new Array(\"inpemSswhAuthorization\", \"\")");
    } else {

      // check if doc type target is different, in this case assing new
      // documentno otherwise matain the previous one
      String strWithholdingDoctypetinvoice = SSWithholdingDocTypeData
          .selectWithholdingDoctypeinvoice(this, strCInvoiceId);

      String strDocumentNo = null;
      if (strWithholdingDoctypetinvoice == null || strWithholdingDoctypetinvoice.equals("")
          || !strWithholdingDoctypetinvoice.equals(strWithholdingDocType)) {
        if (data[0].isdocnocontrolled.equals("Y")) {
          strDocumentNo = data[0].currentnext;
        }
        resultado.append("new Array(\"inpemSswhWithholdingref\", \"<" + strDocumentNo + ">\"),");
        resultado.append("new Array(\"inpemSswhAuthorization\", \"" + data[0].authorizationno
            + "\")");
      } else if (strWithholdingDoctypetinvoice != null && !strWithholdingDoctypetinvoice.equals("")
          && strWithholdingDoctypetinvoice.equals(strWithholdingDocType)) {
        resultado
            .append("new Array(\"inpemSswhWithholdingref\", \""
                + SSWithholdingDocTypeData.selectActualinvoicewithholding(this, strCInvoiceId)
                + "\"),");
        resultado.append("new Array(\"inpemSswhAuthorization\", \"" + data[0].authorizationno
            + "\")");
      }
    }
    resultado.append(");");

    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
