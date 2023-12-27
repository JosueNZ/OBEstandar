/*
 * ************************************************************************ The
 * contents of this file are subject to the Openbravo Public License Version 1.1
 * (the "License"), being the Mozilla Public License Version 1.1 with a
 * permitted attribution clause; you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.openbravo.com/legal/license.html Software distributed under the
 * License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing rights and limitations under the License. The Original Code is
 * Openbravo ERP. The Initial Developer of the Original Code is Openbravo SLU All
 * portions are Copyright (C) 2001-2010 Openbravo SLU All Rights Reserved.
 * Contributor(s): ______________________________________.
 * ***********************************************************************
 */
package ec.com.sidesoft.localization.inventory.PrintInventory;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.Utility;

/*import org.openbravo.model.financialmgmt.accounting.AccountingFact;
 ----pack org.openbravo.model.financialmgmt.accounting -- valor campo Java Package *
 ----tabla                                            .AccountingFact;--valor campo Java Class Name*/

@SuppressWarnings("serial")
public class PrintPhysicalInventory extends HttpSecureAppServlet {
  private static Logger log4j = Logger.getLogger(PrintPhysicalInventory.class);

  private String strADUSerID = "";

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @SuppressWarnings("unchecked")
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars.getSessionValue("168|M_INVENTORY_ID");// ID
                                                                        // WINDOW-CAMPO
                                                                        // REFERENCIA EN
                                                                        // LA VENTANA
      strADUSerID = vars.getUser().toString();
      // normalize the string of ids to a comma separated list
      strDocumentId = strDocumentId.replaceAll("\\(|\\)|'", "");
      if (strDocumentId.length() == 0)
        throw new ServletException(Utility.messageBD(this, "NoDocument", vars.getLanguage()));

      if (log4j.isDebugEnabled())
        log4j.debug("strDocumentId: " + strDocumentId);
      printPagePDF(response, vars, strDocumentId);
    } else {
      pageError(response);
    }
  }

  private void printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId) throws IOException, ServletException {

    if (log4j.isDebugEnabled())
      log4j.debug("Output: Medical_History - pdf");

    PrintPhysicalInventoryData[] data = PrintPhysicalInventoryData.select(this, strDocumentId);// instancio
                                                                                               // un
    // objeto de la clase
    // data
    log4j.debug("data: " + (data == null ? "null" : "not null"));
    if (data == null || data.length == 0)
      data = PrintPhysicalInventoryData.set();
    data = null;
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("DOCUMENT_ID", strDocumentId);// nombre del parametro igual al del reporte
    parameters.put("AD_USER_ID", strADUSerID);

    String strReportName = "@basedesign@/ec/com/sidesoft/localization/inventory/PrintInventory/PrintPhysicalInventory.jrxml";

    renderJR(vars, response, strReportName, "xls", parameters, data, null);

  }

  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}
