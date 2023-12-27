package ec.cusoft.facturaec.filewriter;

import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.ConfigParameters;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

import ec.cusoft.facturaec.templates.OBEInvoice_I;

public class FileGeneration {

	public String file;

	public String process(ConnectionProvider con, HttpServletResponse response,
			VariablesSecureApp vars, ConfigParameters globalParameters,
			Invoice i, String strKey, String strTabId, String strPosted) {
		OBEInvoice_I classEinvoice = null;
		String strJavaClass = null;
		try {
			OBContext.setAdminMode(true);
			/*
			 * if (!i.getBusinessPartner().isEEIEeioice()) { return
			 * Utility.messageBD(con, "EEI_BPartner_Not_EInvoice",
			 * vars.getLanguage()); }
			 */
			strJavaClass = i.getBusinessPartner().getEEIFormat().getValue();
			classEinvoice = (OBEInvoice_I) Class.forName(strJavaClass)
					.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			return Utility.messageBD(con, "Error", vars.getLanguage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return Utility.messageBD(con, "Error", vars.getLanguage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return Utility.messageBD(con, "EEI_ClassNotFound",
					vars.getLanguage())
					+ " " + strJavaClass;
		} finally {
			OBContext.restorePreviousMode();
		}
		return writeFile(con, vars, globalParameters, classEinvoice, i);
	}

	public String process(ConnectionProvider con, VariablesSecureApp vars,
			ConfigParameters globalParameters, Invoice i, String genClass) {
		OBEInvoice_I classEinvoice = null;
		String strJavaClass = null;
		try {
			OBContext.setAdminMode(true);

			/*
			 * if (!i.getBusinessPartner().isEEIEeioice()) return
			 * Utility.messageBD(con, "EEI_BPartner_Not_EInvoice",
			 * vars.getLanguage());
			 */

			strJavaClass = genClass;
			classEinvoice = (OBEInvoice_I) Class.forName(strJavaClass)
					.newInstance();

		} catch (InstantiationException e) {
			e.printStackTrace();
			return Utility.messageBD(con, "Error", vars.getLanguage());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return Utility.messageBD(con, "Error", vars.getLanguage());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return Utility.messageBD(con, "EEI_ClassNotFound",
					vars.getLanguage())
					+ " " + strJavaClass;
		} finally {
			OBContext.restorePreviousMode();
		}

		return writeFile2(con, vars, globalParameters, classEinvoice, i);
	}

	private String writeFile(ConnectionProvider con, VariablesSecureApp vars,
			ConfigParameters globalParameters, OBEInvoice_I classEinvoice,
			Invoice invoice) {

		try {
			file = classEinvoice
					.generateFile(invoice, null, vars.getLanguage());
		} catch (Exception e) {
			e.printStackTrace();
			return Utility.messageBD(con, e.getMessage(), vars.getLanguage());
		}

		return "Success";
	}

	private String writeFile2(ConnectionProvider con, VariablesSecureApp vars,
			ConfigParameters globalParameters, OBEInvoice_I classEinvoice,
			Invoice invoice) {

		String strXml = "";
		try {
			file = classEinvoice
					.generateFile(invoice, null, vars.getLanguage());
			strXml = (file);

		} catch (Exception e) {
			e.printStackTrace();
			return Utility.messageBD(con, e.getMessage(), vars.getLanguage());
		}

		return strXml;
	}

	private String writeRemissionGuide(ConnectionProvider con,
			VariablesSecureApp vars, ConfigParameters globalParameters,
			OBEInvoice_I classEinvoice, Invoice invoice) {

		String strXml = "";
		try {
			file = classEinvoice
					.generateFile(invoice, null, vars.getLanguage());
			strXml = (file);

		} catch (Exception e) {
			e.printStackTrace();
			return Utility.messageBD(con, e.getMessage(), vars.getLanguage());
		}

		return strXml;
	}

}