package ec.cusoft.facturaec.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerContext;

import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;
import ec.cusoft.facturaec.filewriter.PurchaseSettlementGenerationEcuador;

public class GeneratePurchaseSettlement extends DalBaseProcess {
	OBError message;
	static Logger log4j = Logger.getLogger(GenerateFE.class);
	public static String dateTimeFormat;
	public static String sqlDateTimeFormat;
	private SchedulerContext ctx;
	public TaxRate taxRate;
	public String strNewInvoiceID;
	public String strAttachment;
	public String strFTP;
	public Connection connectionDB = null;
	public String strSearchInvoice;
	public ConfigParameters cf;
	public String strFinancialAccountID = null;
	public String strDocumentnoPaymentIn;
	public static final String TRXTYPE_BPDeposit = "BPD";
	public static final String TRXTYPE_BPWithdrawal = "BPW";
	public static final String TRXTYPE_BankFee = "BF";
	public String strFinPaymentScheduleDetailID = "";

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		String strSessionUserId = OBContext.getOBContext().getUser().getId();
		ConnectionProvider conn = new DalConnectionProvider(false);
		try {
			message = new OBError();

			String strInvoiceID = (String) bundle.getParams().get(
					"C_Invoice_ID");
			Invoice invoice = OBDal.getInstance().get(Invoice.class,
					strInvoiceID);

			Boolean boolButtonStatus = invoice.isEeiTemporalsend();
			String strStatus = invoice.getEeiStatus2();

			// ******SE COMPRUEBA SI EL BOTÓN FUE PRESIONADO
			if ((!boolButtonStatus && strStatus == null)
					|| (!boolButtonStatus && (!strStatus.equals("GE")
							&& !strStatus.equals("RE")
							&& !strStatus.equals("AU") && !strStatus
								.equals("PP")))) {
				processRequest(strInvoiceID, null, null, null, strSessionUserId);
			} else {

				log4j.debug("BOTÓN YA PRESIONADO O TRANSACCIÓN EN ESTADO: "
						+ (strStatus == null ? "" : strStatus));

			}

		} catch (Exception e) {
			message.setTitle(Utility.messageBD(conn, "Error", language));
			message.setType("Error");
			message.setMessage(e.getMessage());
		} finally {
			bundle.setResult(message);
		}
		// Y process, N unprocess Status
	}

	public void processRequest(String strInvoiceID, String strURLWSOffline,
			String strCorreoPorDefecto, String strGenerarClaveAcceso,
			String strSessionUserId) {
		message = new OBError();
		ConnectionProvider conn = new DalConnectionProvider(false);
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		Invoice invoice1 = OBDal.getInstance().get(Invoice.class, strInvoiceID);
		Invoice invoice = OBDal.getInstance().get(Invoice.class, strInvoiceID);
		String strResult = null, strTipoResult = "E", strDocType = "", strStatus = null, strStatusSettlement = null;
		boolean boolIsEDoc = false, boolYaEnviado = false;

		try {

			OBContext.setAdminMode(true);
			// INICIO ACTUALIZA BOTÓN PRESIONADO AL PRESIONAR POR PRIMERA VEZ
			invoice1.setEeiTemporalsend(true);
			OBDal.getInstance().save(invoice1);
			OBDal.getInstance().flush();

			// FIN ACTUALIZA BOTÓN PRESIONADO

			log4j.debug("---------------------------------------------------------------------------------------");
			log4j.debug("PROCESANDO DOCUMENTO ELECTRÓNICO "
					+ invoice.getDocumentNo() + " (ID:" + strInvoiceID + ")");

			ConnectionProvider con = new DalConnectionProvider(false);

			// *************OBTENER DATOS DEL DOCUMENTO ELECTRÓNICO
			try {

				boolIsEDoc = invoice.getDocumentType().isEeiIsEdoc();
				strDocType = invoice.getDocumentType().getEeiEdocType();
				strStatus = (invoice.getEeiStatus2() == null ? "ND" : invoice
						.getEeiStatus2());

				log4j.debug("Datos de documentos electrónicos obtenidos");
			} catch (Exception e) {
				strResult = ("Error al obtener datos para generación del documento electrónico (EeiEdocType - EeiIsEdoc).  "
						+ e + "/n");
				strTipoResult = "E";
				throw new OBException(strResult);
			}
			// ------------INICIO GENERACION XML
			if (boolIsEDoc) {

				if (!strStatus.equals("AU") && !strStatus.equals("RE")
						&& !strStatus.equals("GE") && !strStatus.equals("PP")) {

					String strXML = "";

					boolean boolKeyAccessGenerate = false;

					if (strDocType.equals("03")) {// LIQUIDACIÓN DE COMPRA
						if (strGenerarClaveAcceso == null) {
							boolKeyAccessGenerate = (ClientSOAP.SelectParams(3)
									.equals("Y") ? true : false);
						} else {
							boolKeyAccessGenerate = (strGenerarClaveAcceso
									.equals("Y") ? true : false);
						}

						if (boolKeyAccessGenerate) {
							boolean boolKeyGenerate = invoice.getDocumentType()
									.isEeiKeyAccessGenerated();
							if ((invoice.getEeiCodigo2() != null
									&& !boolKeyGenerate && (strStatus == null
									|| strStatus.equals("")
									|| strStatus.equals("NR")
									|| strStatus.equals("NA") || strStatus
										.equals("ND")))
									|| (invoice.getEeiCodigo2() != null
											&& boolKeyGenerate && (strStatus
											.equals("NR") || strStatus
											.equals("NA")))
									|| (invoice.getEeiCodigo2() == null || invoice
											.getEeiCodigo2().equals(""))) {
								try {
									GenerateFE
											.setKeyAccess(invoice, strDocType);
								} catch (Exception e) {
									strResult = ("Error al generar clave de acceso:  " + e
											.getMessage());
									strTipoResult = "E";
									throw new OBException(strResult);
								}
							}
						}

						try {

							PurchaseSettlementGenerationEcuador purchaseSettlement = new PurchaseSettlementGenerationEcuador();
							strXML = purchaseSettlement.generateFile(invoice,
									null, OBContext.getOBContext()
											.getLanguage().getLanguage());

							log4j.debug("Termina clase de generación de xml.");
						} catch (OBException e) {
							strResult = ("Error al generar documento Liquidación de compras:  "
									+ e + "\n" + strXML);
							strTipoResult = "E";
							throw new OBException(strResult);
						}

					} else {
						strResult = "Error: "
								+ " No es posible generar Documento Electrónico.\n"
								+ "Transacción no configurada como Liquidación de compra.";
						strTipoResult = "E";
						throw new OBException(strResult);
					}

					log4j.debug(strXML);

					// ---------------FIN GENERACIÓN XML

					// --------------INICIO ENVÍO A WS OFFLINE

					if (strXML.contains("<?xml version")) {
						if (strDocType.equals("03")) {
							ClientSOAP clientWS = new ClientSOAP();
							String strWSResult = null;

							try {
								strWSResult = clientWS.sendInvoice(strXML,
										invoice.getId().toString(), invoice,
										null, null, null, strURLWSOffline,
										strCorreoPorDefecto,0);

							} catch (Exception e) {
								strResult = "Error al enviar a WebService."
										+ (strWSResult == null ? ""
												: strWSResult) + " " + e;
								strTipoResult = "E";
								throw new OBException(strResult);
							}
							if (strWSResult != null) {

								log4j.debug("Resultado consulta WS:  "
										+ strWSResult);

								String strWSResultado[] = strWSResult
										.split(";");
								String strWSEstado = strWSResultado[0];
								String strClaveAcceso = null, strUrlXML = null, strUrlRide = null;

								// SI LA RESPUESTA ES ÉXITO
								if (strWSEstado.trim().toUpperCase()
										.equals("OK")) {

									try {
										String strResultado[] = strWSResult
												.split(";");
										strClaveAcceso = strResultado[1];
										strUrlXML = strResultado[4];
										strUrlRide = strResultado[5];

										log4j.debug(strUrlXML);
										log4j.debug(strUrlRide);

										try {

											UpdateInvoice(conn, strInvoiceID,
													strClaveAcceso, strUrlXML,
													strUrlRide,
													boolKeyAccessGenerate);

										} catch (Exception ex) {

											log4j.debug(ex.getMessage());

											throw new OBException(
													"Error al actualizar el estado y campos de Factura Electrónica. "
															+ ex.getMessage());
										}

										strResult = "GENERADO";
										strTipoResult = "S";
									} catch (Exception e) {
										strResult = "Error al actualizar datos de confirmación(OB). "
												+ e;
										strTipoResult = "E";
										throw new OBException(
												"Error al actualizar datos de confirmación(OB)."
														+ e);

									}
									// SI LA RESPUESTA ES ERROR
								} else {
									strResult = "Documento Electrónico No Generado. "
											+ strWSResult;
									strTipoResult = "E";
									throw new OBException(strResult);
								}
							} else {
								strResult = "Error al conectar con Web Service.";
								strTipoResult = "E";
								throw new OBException(strResult);
							}
						} else {
							strResult = "Error: "
									+ " No es posible generar Documento Electrónico.\n"
									+ "Transacción no configurada como Liquidación de compra.";
							strTipoResult = "E";
							throw new OBException(strResult);
						}

					} else {
						strResult = strXML;
						strTipoResult = "E";
						throw new OBException(strXML);
					}
					// ---------------FIN ENVÍO WEB SERVICE OFFLINE

				} else {
					String strYa = null;
					if (strStatus.equals("AU")) {
						strYa = "ya autorizado por el SRI";
					} else if (strStatus.equals("RE")) {
						strYa = "ya recibido y a espera de autorización.";
					} else if (strStatus.equals("PP")) {
						strYa = "en procesamiento en Servicio de Rentas Internas.";
					} else {
						strYa = "ya generado y a espera de recepción.";
					}
					strResult = "Documento electrónico " + strYa;
					strTipoResult = "E";
					boolYaEnviado = true;
					throw new OBException(strResult);
				}

			} else {
				strResult = "No es posible generar Documento Electrónico"
						+ "La parametrización del tipo de documento no es válida.";
				strTipoResult = "E";
				throw new OBException(strResult);
			}

		} catch (Exception e) {
			strResult = "ERROR. " + e;
			strTipoResult = "E";
			e.printStackTrace();

		} finally {

			try {
				if (strTipoResult.equals("S")) {
					// SI SE GENERÓ EXISTOSAMENTE
					message.setTitle(Utility.messageBD(conn, "Success",
							language));
					message.setType("Success");
					message.setMessage(Utility.messageBD(conn,
							"Transacción generada exitosamente.", language));

				} else {
					// SI EXISTIÓ UN ERROR
					try {
						if (!boolYaEnviado) {
							invoice.setEeiStatus2("NG");
							OBDal.getInstance().save(invoice);
							OBDal.getInstance().flush();
						}
					} catch (Exception ex) {
						log4j.debug("No se pudo cambiar a estado 'No Generado'. "
								+ ex.getMessage());
					}

					message.setTitle(Utility.messageBD(conn, "Error", language));
					message.setType("Error");
					message.setMessage(strResult);
				}

				log4j.debug("RESULTADO: " + strResult);
				log4j.debug("---------------------------------------------------------------------------------------");

				// INSERTAR LOG

				GenerateFE.InsertLogs(conn, invoice, strResult, strTipoResult,
						strSessionUserId, strDocType);

				// ACTUALIZAR ESTADO BOTÓN NO PRESIONADO
				invoice.setEeiTemporalsend(false);
				OBDal.getInstance().save(invoice);
				OBDal.getInstance().flush();
				// CIERRA CONEXION A BD
				try {
					conn.destroy();
				} catch (Exception e) {

				}
			} catch (Exception e) {

				log4j.debug("No se pudo insertar el log. " + e);

				// ACTUALIZAR ESTADO BOTÓN NO PRESIONADO
				invoice.setEeiTemporalsend(false);
				OBDal.getInstance().save(invoice);
				OBDal.getInstance().flush();
				OBContext.restorePreviousMode();
				OBDal.getInstance().commitAndClose();
				// CIERRA CONEXION A BD
				try {
					conn.destroy();
				} catch (Exception ex) {
				}
				throw new OBException("No se pudo insertar el log. " + e);
			}

			OBContext.restorePreviousMode();
			OBDal.getInstance().commitAndClose();
		}

	}

	public static void UpdateInvoice(ConnectionProvider connectionProvider,
			String strInvID, String strClaveAcceso, String strURLXml,
			String strURLRide, boolean boolKeyAccessGenerate) {
		String strSql = null;

		if (!boolKeyAccessGenerate) {
			strSql = "update c_invoice set em_eei_codigo_2=?, em_eei_urlxml_2=?, em_eei_urlride_2=?, em_eei_status_2='GE' where c_invoice_id =?";
		} else {
			strSql = "update c_invoice set em_eei_urlxml_2=?, em_eei_urlride_2=?, em_eei_status_2='GE' where c_invoice_id =?";
		}

		int updateCount = 0;
		PreparedStatement st = null;

		try {
			st = connectionProvider.getPreparedStatement(strSql);
			if (!boolKeyAccessGenerate) {
				st.setString(1, strClaveAcceso);
				st.setString(2, strURLXml);
				st.setString(3, strURLRide);
				st.setString(4, strInvID);
			} else {
				st.setString(1, strURLXml);
				st.setString(2, strURLRide);
				st.setString(3, strInvID);
			}

			updateCount = st.executeUpdate();
			if (updateCount > 0) {

				log4j.debug("Estado de transacción actualizado.");

			} else {

			}
			st.close();
		} catch (SQLException e) {
			log4j.debug(e.getMessage());
		} catch (Exception ex) {
			log4j.debug(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {
				log4j.debug(ignore.getMessage());
				ignore.printStackTrace();
			}
		}
	}

	public static String truncate(String value, int length) {

		if (value == null || value.equals("")) {
			return null;
		} else {
			if (value.length() > length) {
				return value.substring(0, length);
			} else {
				return value;
			}
		}
	}

}
