package ec.cusoft.facturaec.ad_offline;

import java.util.List;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Expression;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerContext;

import ec.com.sidesoft.facturaec.test.ProcessFETest;
import ec.cusoft.facturaec.ad_process.utils.ProcessUtils;
import ec.cusoft.facturaec.background.EEIOfflineBatchBackground;
import ec.cusoft.facturaec.filewriter.CreditNoteFileGenerationEcuador;
import ec.cusoft.facturaec.filewriter.DebitNoteFileGenerationEcuador;
import ec.cusoft.facturaec.filewriter.FileGeneration;
import ec.cusoft.facturaec.filewriter.FileGenerationEcuador;
import ec.cusoft.facturaec.filewriter.WithholdingFileGenerationEcuador;

public class fe_generation_offline extends DalBaseProcess {
	// private static final Logger log4j =
	// Logger.getLogger(fe_generation_offline.class);
	// public static final String AD_PROCESS_ID =
	// "499146DD50734090998A092ADD5D5421";
	private static final Logger log4j = Logger
			.getLogger(fe_generation_offline.class);
	private ProcessLogger logger;
	private int maxTransactions = 0;
	private FileGeneration fileGeneration = new FileGeneration();
	String msgTitle = "";
	String msgMessage = "";
	String msgType = ""; // success, warning or error
	public ConfigParameters cf;
	private SchedulerContext ctx;
	OBError message;

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		message = new OBError();

		ConnectionProvider conn = new DalConnectionProvider(false);
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		String strTipoResult = null, strResult = null;
		ProcessFETest docObj = null;
		try {
			OBContext.setAdminMode(true);

			// Revisar los tipos de documentos Configurados
			String strTmp = (String) bundle.getParams().get("Fets_Test_ID");

			docObj = OBDal.getInstance().get(ProcessFETest.class, strTmp);

			String strDocType = (docObj.getDoctype() == null ? "" : docObj
					.getDoctype());

			String strinvoiceID = docObj.getInvoice().getId();

			Invoice invoice = docObj.getInvoice();
			log4j.debug("---------------------------------------------------------------------------------------");
			log4j.debug("PRUEBA DE GENERACIÓN XML DOCUMENTO ELECTRÓNICO  "
					+ invoice.getDocumentNo() + " (ID:" + strinvoiceID + ")");
			boolean isEDoc = invoice.getDocumentType().isEeiIsEdoc();

			String genClass = null;

			if (isEDoc) {
				strTipoResult = "S";
				if (strDocType.equals("01")) {// Factura de Venta
					try {
						log4j.debug("Ejecutando clase de generación de xml.");
						genClass = FileGenerationEcuador.class
								.getCanonicalName();

						strResult = executeClassFE(genClass, invoice);
						log4j.debug("Termina clase de generación de xml.");
					} catch (OBException e) {
						strResult = ("Error al generar documento Factura Electrónica:  "
								+ e + "/n" + strResult);
						strTipoResult = "E";
						throw new OBException(strResult);
					}

				} else if (strDocType.equals("04")) {// Nota de Crédito
					try {

						genClass = CreditNoteFileGenerationEcuador.class
								.getCanonicalName();
						strResult = executeClassFE(genClass, invoice);
					} catch (OBException e) {
						strResult = ("Error al generar documento Nota de Crédito:  "
								+ e + "/n" + strResult);
						strTipoResult = "E";
						throw new OBException(strResult);
					}
				} else if (strDocType.equals("05")) {// Nota de Débito
					try {
						genClass = DebitNoteFileGenerationEcuador.class
								.getCanonicalName();
						strResult = executeClassFE(genClass, invoice);
					} catch (OBException e) {
						strResult = ("Error al generar documento Nota de Débito:  "
								+ e + "/n" + strResult);
						strTipoResult = "E";
						throw new OBException(strResult);
					}
				} else if (strDocType.equals("07")) {// Retención
					try {
						genClass = WithholdingFileGenerationEcuador.class
								.getCanonicalName();
						strResult = executeClassFE(genClass, invoice);
					} catch (OBException e) {

						strResult = "Error al generar documento Retención:  "
								+ e + "/n" + strResult;
						strTipoResult = "E";
						throw new OBException(strResult);
					}
				} else if (!strDocType.equals("01") && !strDocType.equals("04")
						&& !strDocType.equals("05") && !strDocType.equals("07")) {
					strResult = "Error: "
							+ " No es posible generar Documento Electrónico. \n"
							+ "La generación de Documento Electrónico solo es válida para documentos cuyo respectivo Tipo de Documento Electrónico sea Factura de Venta(01),"
							+ " Nota de Crédito(04), Nota de Débito(05) o Retención(07).";
					strTipoResult = "E";
					throw new OBException(strResult);
				}
			} else {
				strResult = "No es posible generar Documento Electrónico. "
						+ "La parametrización del tipo de documento no es válida.";
				strTipoResult = "E";
				throw new OBException(strResult);
			}

		} catch (Exception e) {
			e.printStackTrace();
			strResult = "ERROR. " + e;
			strTipoResult = "E";

		} finally {

			// try {
			if (strResult.contains("<?xml version")
					&& strTipoResult.equals("S")) { // SI SE GENERÓ
													// EXISTOSAMENTE

				message.setTitle(Utility.messageBD(conn, "Success", language));
				message.setType("Success");
				message.setMessage(Utility.messageBD(conn,
						"Documento XML Generado Existosamente.", language));
				bundle.setResult(message);
			}
			/*
			 * else { // SI EXISTIÓ UN ERROR
			 * 
			 * message.setTitle(Utility.messageBD(conn, "Error", language));
			 * message.setType("Error"); message.setMessage(strResult); }
			 */log4j.debug("RESULTADO: " + strResult);
			log4j.debug("---------------------------------------------------------------------------------------");
			/*
			 * } catch (Exception e) {
			 * log4j.debug("Error al mostrar mensaje de respuesta en pantalla. "
			 * + e.getMessage()); } finally { bundle.setResult(message); }
			 */
			docObj.setXml(strResult);
			OBDal.getInstance().save(docObj);
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();
			// CIERRA CONEXION A BD
			try {
				conn.destroy();
			} catch (Exception e) {

			}
			OBContext.restorePreviousMode();
		}
	}

	public String executeClassFE(String strClass, Invoice invoice) {

		String result = null;
		ConnectionProvider con = new DalConnectionProvider();
		try {
			if (!strClass.equals("")) {

				VariablesSecureApp vars = ProcessUtils.getVars();

				try {
					result = fileGeneration.process(con, vars, null, invoice,
							strClass);
				} catch (Exception e1) {
					log4j.debug(e1.getMessage());
				}

				return result;
			}
		} catch (Exception e) {
			result = "Error al ejecutar clase de generación de xml. " + e;
			throw new OBException(
					"Error al ejecutar clase de generación de xml. " + e);
		} finally {
			try {
				con.destroy();
			} catch (Exception e) {

			}
		}

		return result;

	}

	public ConfigParameters getConfigParameters() {
		return (ConfigParameters) ctx.get(ConfigParameters.CONFIG_ATTRIBUTE);
	}

	public static ConfigParameters retrieveFrom(ServletContext context) {
		ConfigParameters params = (ConfigParameters) context
				.getAttribute(ConfigParameters.CONFIG_ATTRIBUTE);
		if (params == null) {
			params = new ConfigParameters(context);
			params.storeIn(context);
		}
		return params;
	}
}