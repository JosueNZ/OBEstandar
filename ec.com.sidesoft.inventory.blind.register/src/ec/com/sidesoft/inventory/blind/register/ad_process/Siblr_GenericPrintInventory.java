package ec.com.sidesoft.inventory.blind.register.ad_process;

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
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;

import ec.com.sidesoft.custom.reports.SescrTemplateReport;
import ec.com.sidesoft.inventory.blind.register.SiblrPhysicalInventory;

@SuppressWarnings("serial")
public class Siblr_GenericPrintInventory extends HttpSecureAppServlet {

  private String strSessionValue = "";
  private String StrWindowdID = "";
  private String StrTableID = "";
  private String strADUSerID = "";

  private static Logger log4j1 = Logger.getLogger(Siblr_GenericPrintInventory.class);

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    strSessionValue = "2671D49BE8404D209A20D595BDC69679|SIBLR_PHYSICAL_INVENTORY_ID";
    StrWindowdID = "2671D49BE8404D209A20D595BDC69679";
    StrTableID = "AEF7D47B95B04EF39665BA6B5791349F";

    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars.getSessionValue(strSessionValue);

      strADUSerID = vars.getUser().toString();

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

    SiblrPhysicalInventory pi = OBDal.getInstance().get(SiblrPhysicalInventory.class,
        strDocumentId);

    String StrLocatorID = pi.getStorageBin().getId();

    String StrID = pi.getId();
    Boolean printed = pi.isPrint();

    OBCriteria<SescrTemplateReport> PrintWithh = OBDal.getInstance()
        .createCriteria(SescrTemplateReport.class);
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_WINDOW, ADWindow));
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_TABLE, ADTable));
    // PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_ORGANIZATION, ADOrg)); optional

    List<SescrTemplateReport> LstTemplate = PrintWithh.list();
    int ICountTemplate;
    ICountTemplate = LstTemplate.size();

    if (ICountTemplate == 0) {

      throw new OBException(Utility.messageBD(conn, "@Template no Found..@", language));

    }

    /*
     * if (printed.equals(false)) {
     */
    if (LstTemplate.get(0).getWindow().getId().equals(StrWindowdID)) {

      String StrRutaReport = LstTemplate.get(0).getTemplateDir();

      String strReportName = StrRutaReport + "/" + LstTemplate.get(0).getNameReport();

      final String strAttach = globalParameters.strFTPDirectory + "/284-" + classInfo.id;

      final String strLanguage = vars.getLanguage();

      final String strBaseDesign = getBaseDesignPath(strLanguage);

      strReportName = Replace.replace(Replace.replace(strReportName, "@basedesign@", strBaseDesign),
          "@attach@", strAttach);

      if (log4j1.isDebugEnabled())
        log4j1.debug("Output: Payments Partial - pdf");

      // VALIDACION PARA SQL
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      // parameters.put("DOCUMENT_ID", strDocumentId);
      parameters.put("M_LOCATOR_ID", StrLocatorID);
      parameters.put("siblr_physical_inventory", StrID);
      String StrBaseWeb = getBaseDesignPath(strLanguage);
      parameters.put("BASE_WEB", StrBaseWeb);
      parameters.put("AD_USER_ID", strADUSerID);
      // String StrNameReport = LstTemplate.get(0).getTitle().replace(" ", "_") + ".jrxml";

      renderJR(vars, response, strReportName, "pdf", parameters, null, null);
     // pi.setPrint(false);

      /*
       * OBDal.getInstance().save(pi); OBDal.getInstance().flush();
       */
    }

    /*
     * } else { throw new OBException(Utility.messageBD(conn, "El documento ya fue impreso.",
     * language)); }
     */

  }

  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}
