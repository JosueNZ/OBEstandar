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
 * All portions are Copyright (C) 2008-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package ec.com.sidesoft.localization.ecuador.resupply.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.utils.FormatUtilities;
import org.openbravo.xmlEngine.XmlDocument;

public class SL_ResupplyLine_Conversion extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strChanged = vars.getStringParameter("inpLastFieldChanged");
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      String strUOM = vars.getStringParameter("inpcUomId");
      String strMProductUOMID = vars.getStringParameter("inpmProductUomId");
      String strQuantityOrder = vars.getNumericParameter("inpquantityorder");
      String strTabId = vars.getStringParameter("inpTabId");

      try {
        printPage(response, vars, strUOM, strMProductUOMID, strQuantityOrder, strTabId);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars, String strUOM,
      String strMProductUOMID, String strQuantityOrder, String strTabId) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();
    if (strUOM.startsWith("\""))
      strUOM = strUOM.substring(1, strUOM.length() - 1);
    int stdPrecision = Integer.valueOf(SLResupplyLineConversionData.stdPrecision(this, strUOM))
        .intValue();
    String strInitUOM = SLResupplyLineConversionData.initUOMId(this, strMProductUOMID);
    String strMultiplyRate;
    boolean check = false;

    strMultiplyRate = SLResupplyLineConversionData.multiplyRate(this, strInitUOM, strUOM);
    if (strInitUOM.equals(strUOM))
      strMultiplyRate = "1";
    if (strMultiplyRate.equals(""))
      strMultiplyRate = SLResupplyLineConversionData.divideRate(this, strUOM, strInitUOM);
    if (strMultiplyRate.equals("")) {
      strMultiplyRate = "1";
      if (!strMProductUOMID.equals(""))
        check = true;
    }

    BigDecimal quantityOrder, movementQty, multiplyRate;

    multiplyRate = new BigDecimal(strMultiplyRate);

    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='SL_ResupplyLine_Conversion';\n\n");
    if (strMultiplyRate.equals("0") || strQuantityOrder.equals("")) {
      resultado.append("var respuesta = null");
    } else {
      resultado.append("var respuesta = new Array(");
      quantityOrder = new BigDecimal(strQuantityOrder);
      movementQty = quantityOrder.multiply(multiplyRate);
      if (movementQty.scale() > stdPrecision)
        movementQty = movementQty.setScale(stdPrecision, BigDecimal.ROUND_HALF_UP);
      resultado.append("new Array(\"inpqty\", " + movementQty.toString() + ")");
      if (check) {
        if (!strQuantityOrder.equals(""))
          resultado.append(",");
        resultado.append("new Array('MESSAGE', \""
            + FormatUtilities.replaceJS(Utility.messageBD(this, "NoUOMConversion",
                vars.getLanguage())) + "\")");
      }
      resultado.append(");");
    }
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
