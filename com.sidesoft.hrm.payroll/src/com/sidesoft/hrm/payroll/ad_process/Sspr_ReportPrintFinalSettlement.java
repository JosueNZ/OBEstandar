package com.sidesoft.hrm.payroll.ad_process;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

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
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;

import com.sidesoft.hrm.payroll.sspr_settlement;

import ec.com.sidesoft.custom.reports.SescrTemplateReport;

@SuppressWarnings("serial")
public class Sspr_ReportPrintFinalSettlement extends HttpSecureAppServlet {
  private static Logger log4j = Logger.getLogger(Sspr_ReportPrintFinalSettlement.class);

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @SuppressWarnings("unchecked")
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars
          .getSessionValue("3F0BFA1E7F2643CD84E05BA6BDA8220D|SSPR_SETTLEMENT_ID");

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
    StrWindowdID = "3F0BFA1E7F2643CD84E05BA6BDA8220D";
    StrTableID = "DC962E28F8E2426DB8C0AD4BF8744B8D";
    String StrTransactionID = vars
        .getSessionValue("3F0BFA1E7F2643CD84E05BA6BDA8220D|SSPR_SETTLEMENT_ID");
        log4j.debug("Output: Goods Movement - pdf");

    sspr_settlement SsprSettlement = OBDal.getInstance().get(sspr_settlement.class,
        StrTransactionID);
    Organization ADOrg = OBDal.getInstance().get(Organization.class,
        SsprSettlement.getOrganization().getId().toString());

    Window ADWindow = OBDal.getInstance().get(Window.class, StrWindowdID);
    Table ADTable = OBDal.getInstance().get(Table.class, StrTableID);

    OBCriteria<SescrTemplateReport> PrintWithh = OBDal.getInstance().createCriteria(
        SescrTemplateReport.class);
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_WINDOW, ADWindow));
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_TABLE, ADTable));
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_ORGANIZATION, ADOrg));

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

      strReportName = Replace.replace(
          Replace.replace(strReportName, "@basedesign@", strBaseDesign), "@attach@", strAttach);

      if (log4j.isDebugEnabled())
        log4j.debug("Output: Goods Movement - pdf");

      // VALIDACION PARA SQL

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DOCUMENT_ID", strDocumentId);
      String StrBaseWeb = getBaseDesignPath(strLanguage);
      parameters.put("BASE_WEB", StrBaseWeb);
      String StrNameReport = LstTemplate.get(0).getTitle().replace(" ", "_") + ".jrxml";

      // response.setContentType("application/pdf;");
      // response.setHeader("Content-Disposition",
      // "attachment; filename=" + StrNameReport.replace(".jrxml", ".pdf"));

      String outputFile = "";
      outputFile = GetReport(strReportName, StrNameReport, parameters);

      File pdfFile = new File(outputFile);
      response.setContentLength((int) pdfFile.length());
      try {
        FileInputStream fileInputStream = null;
        fileInputStream = new FileInputStream(pdfFile);
        OutputStream responseOutputStream = response.getOutputStream();
        int bytes;
        while ((bytes = fileInputStream.read()) != -1) {
          responseOutputStream.write(bytes);
        }
        responseOutputStream.close();
        responseOutputStream.flush();
        printPageClosePopUpAndRefreshParent(response, vars);
      } catch (Exception e) {

      }

    }

  }

  public String GetReport(String strReportName, String StrNameReport,
      HashMap<String, Object> parameters) {
    JasperReport report = null;

    String SrtLinkReport = null;
    SrtLinkReport = strReportName;
    JasperPrint print;
    Connection con = null;
    final String outputFile = globalParameters.strFTPDirectory + "/"
        + StrNameReport.replace(".jrxml", ".pdf");

    try {
      con = getTransactionConnection();
      parameters.put("REPORT_CONNECTION", con);
      report = JasperCompileManager.compileReport(SrtLinkReport);
      print = JasperFillManager.fillReport(report, parameters, con);
      JasperExportManager.exportReportToPdfFile(print, outputFile);
      con.close();

    } catch (Exception e) {
      // log4j.debug("Error: Goods Movement - pdf");
      throw new OBException("Error template: " + e.getMessage().toString());
    }

    return outputFile;
  }

  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}
