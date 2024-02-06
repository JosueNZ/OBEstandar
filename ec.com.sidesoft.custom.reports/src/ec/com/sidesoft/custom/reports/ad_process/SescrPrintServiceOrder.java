package ec.com.sidesoft.custom.reports.ad_process;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.Utility;

public class SescrPrintServiceOrder extends HttpSecureAppServlet {
  // private static Logger log4j = Logger.getLogger(SproctmPrintTimeEsp.class);

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars
          .getSessionValue("94CD1D04743C404CBA5AF1BCFF0BC353|Atindpo_Postventa_ID"); // ID Ventana -
                                                                                     // ID KEY

      // normalize the string of ids to a comma separated list
      strDocumentId = strDocumentId.replaceAll("\\(|\\)|'", "");
      if (strDocumentId.length() == 0)
        throw new ServletException(Utility.messageBD(this, "NoDocument", vars.getLanguage()));

      printPagePDF(response, vars, strDocumentId);
    } else {
      pageError(response);

    }
  }

  private void printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId) throws IOException, ServletException {
    String strAD_UserID = vars.getUser().toString();
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("DOCUMENT_ID", strDocumentId);
    String strReportName = "@basedesign@/ec/com/sidesoft/custom/reports/ad_reports/CustomOrdenServicio.jrxml";
    renderJR(vars, response, strReportName, "pdf", parameters, null, null);

  }

  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method

}
