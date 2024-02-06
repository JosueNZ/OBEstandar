package ec.com.sidesoft.custom.payments.partialpayment.ad_process;

import java.io.IOException;
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
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;

import ec.com.sidesoft.custom.reports.SescrTemplateReport;

//ec.com.sidesoft.safety.dailymedic.ad_process.Ssohdn_PrintGenericMedic
@SuppressWarnings("serial")
public class Sspp_PrintGenericPaymentsPartial extends HttpSecureAppServlet {

  private String strSessionValue = "";
  private String StrWindowdID = "";
  private String StrTableID = "";

  private static Logger log4j1 = Logger.getLogger(Sspp_PrintGenericPaymentsPartial.class);

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    strSessionValue = "00E058D512D048EF9AD984A924141680|SSPP_PAYMENTS_ID";
    StrWindowdID = "00E058D512D048EF9AD984A924141680";
    StrTableID = "E9FCD022113D4C16AF20280D0F650BE6";

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars.getSessionValue(strSessionValue);

      // normalize the string of id to a comma separated list
      strDocumentId = strDocumentId.replaceAll("\\(|\\)|'", "");
      if (strDocumentId.length() == 0)
        throw new ServletException(Utility.messageBD(this, "NoDocument", vars.getLanguage()));

      if (log4j1.isDebugEnabled())
        log4j1.debug("strDocumentId: " + strDocumentId);
      printPagePDF(response, vars, strDocumentId);
    } else {
      pageError(response);

    }
  }

  private void printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId) throws IOException, ServletException {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    Window ADWindow = OBDal.getInstance().get(Window.class, StrWindowdID);
    Table ADTable = OBDal.getInstance().get(Table.class, StrTableID);

    OBCriteria<SescrTemplateReport> PrintWithh = OBDal.getInstance().createCriteria(
        SescrTemplateReport.class);
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_WINDOW, ADWindow));
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_TABLE, ADTable));
    // PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_ORGANIZATION, ADOrg)); optional

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

      if (log4j1.isDebugEnabled())
        log4j1.debug("Output: Payments Partial - xls");

      // VALIDACION PARA SQL

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DOCUMENT_ID", strDocumentId);
      String StrBaseWeb = getBaseDesignPath(strLanguage);
      parameters.put("BASE_WEB", StrBaseWeb);
      // String StrNameReport = LstTemplate.get(0).getTitle().replace(" ", "_") + ".jrxml";

      renderJR(vars, response, strReportName, "xls", parameters, null, null);

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
