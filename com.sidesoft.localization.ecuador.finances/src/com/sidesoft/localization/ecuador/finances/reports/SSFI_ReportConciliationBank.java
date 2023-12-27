package com.sidesoft.localization.ecuador.finances.reports;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

public class SSFI_ReportConciliationBank extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strFinReconciliationID = vars.getGlobalVariable("inpfinReconciliationId", "");
      String strFinFinancialAccountId = vars.getGlobalVariable("inpfinFinancialAccountId", "");
      String strDateTo = vars.getGlobalVariable("inpdateto", "");
      printPageDataPDF(request, response, vars, strFinReconciliationID,
          OBDal.getInstance().get(FIN_FinancialAccount.class, strFinFinancialAccountId).getName(),
          strDateTo, OBDal.getInstance().get(FIN_FinancialAccount.class, strFinFinancialAccountId)
              .getGenericAccountNo());
    }
  }

  private void printPageDataPDF(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strFinReconciliationId, String strFinFinancialAccountName,
      String strDateTo, String strFinFinancialGenericAccount) throws IOException, ServletException {
    log4j.debug("Output: Reconciliation PDF report");

    String strMainReportName = "@basedesign@/com/sidesoft/localization/ecuador/finances/reports/SSFI_ReportConciliationBank.jrxml";
    FIN_Reconciliation reconciliation = null;
    OBContext.setAdminMode(true);
    try {
      reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class, strFinReconciliationId);
    } finally {
      OBContext.restorePreviousMode();
    }
    HashMap<String, Object> parameters = new HashMap<String, Object>();

    String strLanguage = vars.getLanguage();

    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);

    // Common
    parameters.put("FINACCOUNT_INFO", strFinFinancialAccountName);
    parameters.put("GENERIC_ACCOUNT", strFinFinancialGenericAccount);
    parameters.put("DATEFORMAT", sdf);
    parameters.put("RECONCILIATION_ID", strFinReconciliationId);
	String StrBaseWeb = getBaseDesignPath(strLanguage);
      parameters.put("BASE_WEB", StrBaseWeb);
    OBContext.setAdminMode(true);
    try {
      //
      parameters.put("DATE", reconciliation.getEndingDate());
      parameters.put("EMISSION_DATE", reconciliation.getCreationDate());
      parameters.put("END_BALANCE", reconciliation.getEndingBalance());
      parameters.put("START_BALANCE", reconciliation.getStartingbalance());
      parameters.put("ORGANIZATIONID", reconciliation.getOrganization().getId());
      parameters.put("ORGANIZATIONNAME", reconciliation.getOrganization().getName());
    } finally {
      OBContext.restorePreviousMode();
    }

    response.setContentType("text/html; charset=UTF-8");
    renderJR(vars, response, strMainReportName, "pdf", parameters, null, null);
  }

}
