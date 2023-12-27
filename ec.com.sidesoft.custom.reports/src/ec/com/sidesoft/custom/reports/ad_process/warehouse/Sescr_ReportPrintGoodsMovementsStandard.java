package ec.com.sidesoft.custom.reports.ad_process.warehouse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentTemplate;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

public class Sescr_ReportPrintGoodsMovementsStandard extends HttpSecureAppServlet {
  private static Logger log4j = Logger.getLogger(Sescr_ReportPrintGoodsMovements.class);

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @SuppressWarnings("unchecked")
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars.getSessionValue("170|M_MOVEMENT_ID");

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
    String StrTransactionID = vars.getSessionValue("170|M_MOVEMENT_ID");

    InternalMovement MMovementID = OBDal.getInstance().get(InternalMovement.class,
        StrTransactionID);
    DocumentType doctype = OBDal.getInstance().get(DocumentType.class,
        MMovementID.getSsrsCDoctype().getId());

    /***************************************************
     * Config By Type Document
     *****************************************************/
    OBContext.setAdminMode(true);
    OBCriteria<DocumentTemplate> obc = OBDal.getInstance().createCriteria(DocumentTemplate.class);
    obc.add(Restrictions.eq(DocumentTemplate.PROPERTY_DOCUMENTTYPE, doctype));
    obc.addOrderBy(DocumentTemplate.PROPERTY_CREATIONDATE, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setMaxResults(1);
    OBContext.setAdminMode(false);
    DocumentTemplate docTemplate = (DocumentTemplate) obc.uniqueResult();
    if (docTemplate == null) {
      throw new OBException(Utility.messageBD(conn, "@Template not config..@", language));
    }

    String StrRutaReport = docTemplate.getTemplateLocation().toString();// LstTemplate.get(0).getTemplateDir();
    String strReportName = StrRutaReport + "/" + docTemplate.getReportFilename().toString(); // LstTemplate.get(0).getNameReport();
    final String strAttach = globalParameters.strFTPDirectory + "/284-" + classInfo.id;
    final String strLanguage = vars.getLanguage();
    final String strBaseDesign = getBaseDesignPath(strLanguage);

    strReportName = Replace.replace(Replace.replace(strReportName, "@basedesign@", strBaseDesign),
        "@attach@", strAttach);

    // Exist File
    File tempFile = new File(strReportName);
    boolean exists = tempFile.exists();
    if (!exists) {
      throw new OBException(Utility.messageBD(conn, "@Template no Found..@", language));
    }

    if (log4j.isDebugEnabled())
      log4j.debug("Output: Goods Movement - pdf");

    // VALIDACION PARA SQL
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("DOCUMENT_ID", strDocumentId);
    String StrBaseWeb = getBaseDesignPath(strLanguage);
    parameters.put("BASE_WEB", StrBaseWeb);
    String StrNameReport = docTemplate.getName().replace(" ", "_") + ".jrxml";

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
