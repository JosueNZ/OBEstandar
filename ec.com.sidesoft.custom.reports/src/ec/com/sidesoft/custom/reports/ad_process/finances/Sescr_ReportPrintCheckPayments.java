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
package ec.com.sidesoft.custom.reports.ad_process.finances;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
//import org.openbravo.base.model.Table;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;

import com.sidesoft.localization.ecuador.finances.ssfiFin_payment_detail_v;

import ec.com.sidesoft.custom.reports.SescrTemplateReport;

@SuppressWarnings("serial")
public class Sescr_ReportPrintCheckPayments extends HttpSecureAppServlet {
  private static Logger log4j = Logger.getLogger(Sescr_ReportPrintCheckPayments.class);

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @SuppressWarnings("unchecked")
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars
          .getSessionValue("6F8F913FA60F4CBD93DC1D3AA696E76E|Fin_Payment_ID");

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

  @SuppressWarnings("resource")
  private void printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId) throws IOException, ServletException {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    String StrWindowdID = "";
    String StrTableID = "";
    StrWindowdID = "6F8F913FA60F4CBD93DC1D3AA696E76E";
    StrTableID = "D1A97202E832470285C9B1EB026D54E2";
    String StrTransactionID = vars
        .getSessionValue("6F8F913FA60F4CBD93DC1D3AA696E76E|Fin_Payment_ID");

    ssfiFin_payment_detail_v MMovementID = OBDal.getInstance().get(ssfiFin_payment_detail_v.class,
        StrTransactionID);

    FIN_Payment obdalPayment = OBDal.getInstance().get(FIN_Payment.class, StrTransactionID);
    DocumentType doctyperev = OBDal.getInstance().get(DocumentType.class,
        obdalPayment.getDocumentType().getId());

    Window ADWindow = OBDal.getInstance().get(Window.class, StrWindowdID);
    Table ADTable = OBDal.getInstance().get(Table.class, StrTableID);

    OBCriteria<SescrTemplateReport> PrintWithhRev = OBDal.getInstance()
        .createCriteria(SescrTemplateReport.class);
    PrintWithhRev.add(Restrictions.eq(SescrTemplateReport.PROPERTY_WINDOW, ADWindow));
    PrintWithhRev.add(Restrictions.eq(SescrTemplateReport.PROPERTY_TABLE, ADTable));
    PrintWithhRev.add(Restrictions.eq(SescrTemplateReport.PROPERTY_DOCTYPE, doctyperev));

    List<SescrTemplateReport> LstTemplateRev = PrintWithhRev.list();

    OBCriteria<SescrTemplateReport> PrintWithh = OBDal.getInstance()
        .createCriteria(SescrTemplateReport.class);
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_WINDOW, ADWindow));
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_TABLE, ADTable));

    if (LstTemplateRev.size() > 0) {
      DocumentType doctype = OBDal.getInstance().get(DocumentType.class,
          obdalPayment.getDocumentType().getId());
      PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_DOCTYPE, doctype));
    }

    List<SescrTemplateReport> LstTemplate = PrintWithh.list();
    int ICountTemplate;
    ICountTemplate = LstTemplate.size();

    if (ICountTemplate == 0) {

      throw new OBException(Utility.messageBD(conn, "@Template no Found..@", language));

    }
    if (LstTemplate.get(0).getWindow().getId().equals(StrWindowdID)) {

      String StrRutaReport = LstTemplate.get(0).getTemplateDir();

      String strReportName = StrRutaReport + "/" + LstTemplate.get(0).getNameReport();

      final String strAttach = globalParameters.strFTPDirectory + "/284-" + classInfo.id;

      final String strLanguage = vars.getLanguage();

      final String strBaseDesign = getBaseDesignPath(strLanguage);

      strReportName = Replace.replace(Replace.replace(strReportName, "@basedesign@", strBaseDesign),
          "@attach@", strAttach);

      if (log4j.isDebugEnabled())
        log4j.debug("Output: Goods Movement - pdf");

      // VALIDACION PARA SQL

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DOCUMENT_ID", strDocumentId);
      String StrBaseWeb = getBaseDesignPath(strLanguage);
      parameters.put("BASE_WEB", StrBaseWeb);

      renderJR(vars, response, strReportName, "pdf", parameters, null, null);

      String StrNameReport = LstTemplate.get(0).getTitle().replace(" ", "_") + ".jrxml";

      // response.setContentType("application/pdf;");
      // response.setHeader("Content-Disposition",
      // "attachment; filename=" + StrNameReport.replace(".jrxml", ".pdf"));
      /*
       * String outputFile = ""; outputFile = GetReport(strReportName, StrNameReport, parameters);
       * 
       * File pdfFile = new File(outputFile); response.setContentLength((int) pdfFile.length()); try
       * { FileInputStream fileInputStream = null; fileInputStream = new FileInputStream(pdfFile);
       * OutputStream responseOutputStream = response.getOutputStream(); int bytes; while ((bytes =
       * fileInputStream.read()) != -1) { responseOutputStream.write(bytes); }
       * responseOutputStream.close(); responseOutputStream.flush();
       * printPageClosePopUpAndRefreshParent(response, vars); } catch (Exception e) {
       * 
       * }
       */

    }

  }
  /*
   * public String GetReport(String strReportName, String StrNameReport, HashMap<String, Object>
   * parameters) { JasperReport report = null;
   * 
   * String SrtLinkReport = null; SrtLinkReport = strReportName; JasperPrint print; Connection con =
   * null; final String outputFile = globalParameters.strFTPDirectory + "/" +
   * StrNameReport.replace(".jrxml", ".pdf");
   * 
   * try { con = getTransactionConnection(); parameters.put("REPORT_CONNECTION", con); report =
   * JasperCompileManager.compileReport(SrtLinkReport); print = JasperFillManager.fillReport(report,
   * parameters, con); JasperExportManager.exportReportToPdfFile(print, outputFile); con.close();
   * 
   * } catch (Exception e) { // log4j.debug("Error: Goods Movement - pdf"); throw new
   * OBException("Error template: " + e.getMessage().toString()); }
   * 
   * return outputFile; }
   */

  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}
