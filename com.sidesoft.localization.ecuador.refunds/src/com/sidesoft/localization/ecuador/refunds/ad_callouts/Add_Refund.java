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
 * Contributor(s):  ___Santos- 22022012_______.
 ************************************************************************
 */
//package org.openbravo.erpCommon.ad_callouts;
package com.sidesoft.localization.ecuador.refunds.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.xmlEngine.XmlDocument;
//import org.openbravo.erpCommon.businessUtility.BpartnerMiscData;
//importar el xsql - en estecaso desde eldirectorio - pakete

public class Add_Refund extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  // recibe la llamada al servlet
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strChanged = vars.getStringParameter("inpemSsreRefundedId"); // recupera el valor de la
                                                                          // pgina web

      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      String strCodelivelihood = vars.getStringParameter("inpemSswhCodelivelihood");
      String strLivelihood = vars.getStringParameter("inpemSswhLivelihood");

      try {
        printPage(response, vars, strChanged, strCodelivelihood, strLivelihood);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strChanged,
      String strCodelivelihood, String strLivelihood) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    AddRefundData[] data = AddRefundData.select(this, strChanged);
    StringBuffer resultado = new StringBuffer();

    resultado.append("var calloutName=\"Add_Refund\";\n\n");
    resultado.append("var respuesta = new Array(");
    if (data != null && data.length > 0) {
      resultado.append("new Array(\"inpemSswhCodelivelihood\", \"" + data[0].sswhCodelivelihoodtId
          + "\"),\n");
      resultado.append("new Array(\"inpemSswhLivelihood\", \"" + data[0].sswhLivelihoodtId
          + "\")\n");

    }
    resultado.append(");\n");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}