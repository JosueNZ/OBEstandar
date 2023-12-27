package ec.cusoft.facturaec.ad_process.PrintElectronicDocuments;

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
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;

@SuppressWarnings("serial")
public class ReportPrintElectronicWithholdings extends HttpSecureAppServlet {
	private static Logger log4j = Logger.getLogger(AccountingFact.class);

	public void init(ServletConfig config) {
		super.init(config);
		boolHist = false;
	}

	@SuppressWarnings("unchecked")
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		VariablesSecureApp vars = new VariablesSecureApp(request);

		if (vars.commandIn("DEFAULT")) {
			String strDocumentId = vars.getSessionValue("183|C_INVOICE_ID");

			// normalize the string of ids to a comma separated list
			strDocumentId = strDocumentId.replaceAll("\\(|\\)|'", "");
			if (strDocumentId.length() == 0)
				throw new ServletException(Utility.messageBD(this,
						"NoDocument", vars.getLanguage()));

			if (log4j.isDebugEnabled())
				log4j.debug("strDocumentId: " + strDocumentId);
			printPagePDF(response, vars, strDocumentId);
		} else {
			pageError(response);
		}
	}

	private void printPagePDF(HttpServletResponse response,
			VariablesSecureApp vars, String strDocumentId) throws IOException,
			ServletException {

		if (log4j.isDebugEnabled())
			log4j.debug("Output: RptC_Contract - pdf");

		ReportPrintElectronicInvoiceData[] data = ReportPrintElectronicInvoiceData
				.select(this, strDocumentId);
		if (log4j.isDebugEnabled())
			log4j.debug("data: " + (data == null ? "null" : "not null"));
		if (data == null || data.length == 0)
			data = ReportPrintElectronicInvoiceData.set();

		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("DOCUMENT_ID", strDocumentId);

		String strReportName = "@basedesign@/ec/cusoft/facturaec/ad_process/PrintElectronicDocuments/RptSswh_Withholding_Electronic.jrxml";
		renderJR(vars, response, strReportName, "pdf", parameters, data, null);
	}

	public String getServletInfo() {
		return "Servlet that processes the print action";
	} // End of getServletInfo() method
}