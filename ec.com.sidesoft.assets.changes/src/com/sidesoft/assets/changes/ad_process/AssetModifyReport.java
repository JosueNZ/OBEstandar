package ec.com.sidesoft.assets.changes.ad_process;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.jasperreports.engine.JasperPrint;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;

public class AssetModifyReport extends HttpSecureAppServlet {

	private static final long serialVersionUID = 1L;

	public void init(ServletConfig config) {
		super.init(config);
		boolHist = false;
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		VariablesSecureApp vars = new VariablesSecureApp(request);

		if (vars.commandIn("DEFAULT")) {
			String documentId = vars.getSessionValue("PrintAssetModify.A_Asset_ID");
			if (documentId.equals(""))
				documentId = vars.getSessionValue("PrintAssetModify.inpssachModifyAssetId");
			String strADUSerID = vars.getUser().toString();
			printPagePartePDF(response, vars, documentId, strADUSerID);
		} else
			pageError(response);
	}

	void printPagePartePDF(HttpServletResponse response,
			VariablesSecureApp vars, String documentId, String strADUSerID) throws IOException,
			ServletException {

		if (log4j.isDebugEnabled())
			log4j.debug("Output: Asset - pdf");
		JasperPrint jasperPrint;

        final String strLanguage = vars.getLanguage();
		final String strBaseDesign = getBaseDesignPath(strLanguage);

		String strReportName = "@basedesign@/com/sidesoft/assets/changes/ad_Reports/Rpt_AssetModify.jrxml";
		response.setHeader("Content-disposition",
				"inline; filename=Asset.pdf");

		HashMap<String, Object> parameters = new HashMap<String, Object>();
		/*String RecordId=(String) bundle.getParams().get("XXX_Import_ID");
		log.info("Record ID "+RecordId);*/
		String sourcepath=      OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("attach.path");
		if (log4j.isDebugEnabled())
			log4j.debug("OB SOURCE PATH "+sourcepath);

      	String StrBaseWeb = getBaseDesignPath(strLanguage);
      	parameters.put("BASE_SUB", StrBaseWeb);
		documentId = documentId.replaceAll("\\(|\\)|'", "");
		parameters.put("DOCUMENT_ID", documentId);
		parameters.put("AD_USER_ID", strADUSerID);
		parameters.put("SOURCE_PATH", sourcepath);

		renderJR(vars, response, strReportName, "pdf", parameters, null, null);
	}

	public String getServletInfo() {
		return "Servlet that presents the RptMInout seeker";
	} // End of getServletInfo() method

}
