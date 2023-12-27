package com.sidesoft.localization.ecuador.finances.ad_process;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Calendar;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openbravo.erpCommon.utility.Utility;

import org.apache.commons.fileupload.FileItem;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.service.db.DalConnectionProvider;
import org.apache.log4j.Logger;
import org.openbravo.utils.Replace;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;

import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;

public class ReconciliationDetailReport extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private String strSessionValue = "";
  private static Logger log4j1 = Logger.getLogger(ReconciliationDetailReport.class);

  public void init(ServletConfig config) {
    super.init(config);
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    strSessionValue = "94EAA455D2644E04AB25D93BE5157B6D|FIN_Reconciliation_ID";
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      // Retrieves the file content inside a FileItem class
      String strDocumentId = vars.getSessionValue(strSessionValue);
      String item = vars.getStringParameter("inpBankBalance");

      strDocumentId = strDocumentId.replaceAll("\\(|\\)|'", "");
      item = item.replace(",", "");

      if (strDocumentId.length() == 0)
        throw new ServletException(Utility.messageBD(this, "NoDocument", vars.getLanguage()));

      if (log4j1.isDebugEnabled())
        log4j1.debug("strDocumentId: " + strDocumentId);

      printPagePDF(response, vars, strDocumentId, item);
    } else {
      pageError(response);
    }
  }

  private void printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId, String bankBalance) throws IOException, ServletException {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    String strAD_UserID = vars.getUser().toString();

    FIN_Reconciliation finReconciliation = OBDal.getInstance().get(FIN_Reconciliation.class,
        strDocumentId.trim());

    Calendar cal = Calendar.getInstance();
    cal.setTime(finReconciliation.getTransactionDate());

    // Obtenemos el año de la fecha de la transaccion
    OBCriteria<Year> yearcrt = OBDal.getInstance().createCriteria(Year.class);
    yearcrt
        .add(Restrictions.eq(Year.PROPERTY_FISCALYEAR, Integer.toString(cal.get(Calendar.YEAR))));
    yearcrt.setFilterOnReadableOrganization(false);
    yearcrt.setMaxResults(1);

    Year year = (Year) yearcrt.uniqueResult();

    // Obtenemos el mes de la fecha de la transaccion
    OBCriteria<Period> periodcrt = OBDal.getInstance().createCriteria(Period.class);
    periodcrt.add(Restrictions.eq(Period.PROPERTY_PERIODNO, Long.valueOf(cal.get(Calendar.MONTH)+1)));
    periodcrt.add(Restrictions.eq(Period.PROPERTY_YEAR, year));
    periodcrt.setFilterOnReadableOrganization(false);
    periodcrt.setMaxResults(1);

    Period period = (Period) periodcrt.uniqueResult();

    final String strAttach = globalParameters.strFTPDirectory + "/284-" + classInfo.id;

    final String strLanguage = vars.getLanguage();

    final String strBaseDesign = getBaseDesignPath(strLanguage);

    String strReportName = "@basedesign@/com/sidesoft/localization/ecuador/finances/reports/Rpt_ReconciliationDetaill.jrxml";

    strReportName = Replace.replace(Replace.replace(strReportName, "@basedesign@", strBaseDesign),
        "@attach@", strAttach);
    String sourcepath = getAttachmentsDirectoryPath();

    if (log4j1.isDebugEnabled())
      log4j1.debug("Output: Reconciliation Detaill - pdf");

    // VALIDACION PARA SQL
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("DOCUMENT_ID", strDocumentId);
    String StrBaseWeb = getBaseDesignPath(strLanguage);
    parameters.put("BASE_WEB", StrBaseWeb);
    parameters.put("AD_USER_ID", strAD_UserID);
    parameters.put("SOURCE_PATH", sourcepath);
    parameters.put("C_Year_ID", year.getId());
    parameters.put("C_Period_ID", period.getId());
    parameters.put("Fin_Financial_Account_ID", finReconciliation.getAccount().getId());
    parameters.put("Bank_Balance", (!bankBalance.isEmpty()) ? new BigDecimal(bankBalance)
        : finReconciliation.getEndingBalance());

    String StrNameReport = "Rpt_" + "ConciliacionMensual";
    // renderJR(vars, response, strReportName, "pdf", parameters, null, null);
    renderJR(vars, response, strReportName, StrNameReport, "pdf", parameters, null, null, false);

  }

  private void closePopUp(Writer writer, boolean res) {
    try {
      String p = (res) ? "true" : "false";
      writer.write("<HTML><BODY><SCRIPT type=\"text/javascript\">");
      writer.write("top.OB.SSFI.closePopUp(" + p + ");");
      writer.write("</SCRIPT></BODY></HTML>");
    } catch (IOException ioex) {
    }
  }

  private String getAttachmentsDirectoryPath() {
    return OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("attach.path");
  }

  public String getServletInfo() {
    return "Servlet that creates a file sent by the client in the server";
  }
}