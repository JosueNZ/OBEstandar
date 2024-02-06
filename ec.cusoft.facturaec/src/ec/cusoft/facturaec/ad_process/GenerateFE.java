package ec.cusoft.facturaec.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Random;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
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

import ec.cusoft.facturaec.EEIParamFacturae;
import ec.cusoft.facturaec.ad_process.utils.ProcessUtils;
import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;
import ec.cusoft.facturaec.filewriter.CreditNoteFileGenerationEcuador;
import ec.cusoft.facturaec.filewriter.DebitNoteFileGenerationEcuador;
import ec.cusoft.facturaec.filewriter.FileGeneration;
import ec.cusoft.facturaec.filewriter.FileGenerationEcuador;
import ec.cusoft.facturaec.filewriter.WithholdingFileGenerationEcuador;

public class GenerateFE extends DalBaseProcess {
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

	private FileGeneration fileGeneration = new FileGeneration();
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
			String strStatus = invoice.getEeiStatus();

			// ******SE COMPRUEBA SI EL BOTÓN FUE PRESIONADO
			if ((!boolButtonStatus && strStatus == null)
					|| (!boolButtonStatus && (!strStatus.equals("GE")
							&& !strStatus.equals("RE")
							&& !strStatus.equals("AU") && !strStatus
								.equals("PP")))) {
				processRequest(strInvoiceID, null, null, null,strSessionUserId,0);
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
			String strCorreoPorDefecto, String strGenerarClaveAcceso,String strSessionUserId,int intTimeOutPara) {
		message = new OBError();
		ConnectionProvider conn = new DalConnectionProvider(false);
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		Invoice invoice1 = OBDal.getInstance().get(Invoice.class, strInvoiceID);
		Invoice invoice = OBDal.getInstance().get(Invoice.class, strInvoiceID);
		String strResult = null, strTipoResult = "E", strDocType = "", strStatus = null, strStatusSettlement = null;
		boolean boolIsEDoc = false, boolIsPurchaseSettlement = false, boolYaEnviado = false;

		try {

			OBContext.setAdminMode(true);
			// INICIO ACTUALIZA BOTÓN PRESIONADO AL PRESIONAR POR PRIMERA VEZ
			invoice1.setEeiTemporalsend(true);
			OBDal.getInstance().save(invoice1);
			// OBDal.getInstance().refresh(invoice);
			OBDal.getInstance().flush();

			// FIN ACTUALIZA BOTÓN PRESIONADO

			log4j.debug("---------------------------------------------------------------------------------------");
			log4j.debug("PROCESANDO DOCUMENTO ELECTRÓNICO "
					+ invoice.getDocumentNo() + " (ID:" + strInvoiceID + ")");

			ConnectionProvider con = new DalConnectionProvider(false);

			// *************OBTENER DATOS DEL DOCUMENTO ELECTRÓNICO
			try {

				if (!invoice.isSalesTransaction()) {

					if (invoice.getSswhCDoctype() == null) {
						throw new OBException(
								"No se encontró un tipo de documento para la retención");
					}
					boolIsEDoc = invoice.getSswhCDoctype().isEeiIsEdoc();
					strDocType = invoice.getSswhCDoctype().getEeiEdocType();
				} else {
					boolIsEDoc = invoice.getDocumentType().isEeiIsEdoc();
					strDocType = invoice.getDocumentType().getEeiEdocType();
				}
				strStatus = (invoice.getEeiStatus() == null ? "ND" : invoice
						.getEeiStatus());

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

					String genClass = "", strXML = "";
					
					if (!strDocType.equals("01") && !strDocType.equals("04")
							&& !strDocType.equals("05")
							&& !strDocType.equals("07")) {
						strResult = "Error: "
								+ " No es posible generar Documento Electrónico.\n"
								+ "La generación de Documento Electrónico solo es válida para documentos cuyo respectivo Tipo de Documento Electrónico sea Factura de Venta,"
								+ " Nota de Crédito, Nota de Débito o Retención.";
						strTipoResult = "E";
						throw new OBException(strResult);
					}
					
					boolean boolKeyAccessGenerate = false;
					if (strGenerarClaveAcceso == null) {
						boolKeyAccessGenerate = (ClientSOAP.SelectParams(3)
								.equals("Y") ? true : false);
					} else {
						boolKeyAccessGenerate = (strGenerarClaveAcceso
								.equals("Y") ? true : false);
					}

					if (boolKeyAccessGenerate) {
						boolean boolKeyGenerate = false;
						if (!invoice.isSalesTransaction()) {
							boolKeyGenerate = invoice.getSswhCDoctype()
									.isEeiKeyAccessGenerated();
						} else {
							boolKeyGenerate = invoice.getDocumentType()
									.isEeiKeyAccessGenerated();
						}

						if ((invoice.getEeiCodigo() != null && !boolKeyGenerate && (strStatus == null
								|| strStatus.equals("")
								|| strStatus.equals("NR")
								|| strStatus.equals("NA") || strStatus
									.equals("ND")))
								|| (invoice.getEeiCodigo() != null
										&& boolKeyGenerate && (strStatus
										.equals("NR") || strStatus.equals("NA")))
								|| (invoice.getEeiCodigo() == null || invoice
										.getEeiCodigo().equals(""))) {
							try {
								setKeyAccess(invoice, strDocType);
							} catch (Exception e) {
								strResult = ("Error al generar clave de acceso:  " + e
										.getMessage());
								strTipoResult = "E";
								throw new OBException(strResult);
							}
						}
					}

					if (strDocType.equals("01")) {// Factura de Venta
						try {
							log4j.debug("Ejecutando clase de generación de xml.");
							genClass = FileGenerationEcuador.class
									.getCanonicalName();

							strXML = executeClassFE(genClass, invoice);
							log4j.debug("Termina clase de generación de xml.");
						} catch (OBException e) {
							strResult = ("Error al generar documento Factura Electrónica:  "
									+ e + "/n" + strXML);
							strTipoResult = "E";
							throw new OBException(strResult);
						}

					} else if (strDocType.equals("04")) {// Nota de Crédito
						try {

							genClass = CreditNoteFileGenerationEcuador.class
									.getCanonicalName();
							strXML = executeClassFE(genClass, invoice);
						} catch (OBException e) {
							strResult = ("Error al generar documento Nota de Crédito:  "
									+ e + "/n" + strXML);
							strTipoResult = "E";
							throw new OBException(strResult);
						}
					} else if (strDocType.equals("05")) {// Nota de Débito
						try {
							genClass = DebitNoteFileGenerationEcuador.class
									.getCanonicalName();
							strXML = executeClassFE(genClass, invoice);
						} catch (OBException e) {
							strResult = ("Error al generar documento Nota de Débito:  "
									+ e + "/n" + strXML);
							strTipoResult = "E";
							throw new OBException(strResult);
						}
					} else if (strDocType.equals("07")) {// Retención
						try {
							genClass = WithholdingFileGenerationEcuador.class
									.getCanonicalName();
							strXML = executeClassFE(genClass, invoice);
						} catch (OBException e) {

							strResult = "Error al generar documento Retención:  "
									+ e + "/n" + strXML;
							strTipoResult = "E";
							throw new OBException(strResult);
						}
					} else if (!strDocType.equals("01")
							&& !strDocType.equals("04")
							&& !strDocType.equals("05")
							&& !strDocType.equals("07")) {
						strResult = "Error: "
								+ " No es posible generar Documento Electrónico.\n"
								+ "La generación de Documento Electrónico solo es válida para documentos cuyo respectivo Tipo de Documento Electrónico sea Factura de Venta,"
								+ " Nota de Crédito, Nota de Débito o Retención.";
						strTipoResult = "E";
						throw new OBException(strResult);
					}

					log4j.debug(strXML);

					// ---------------FIN GENERACIÓN XML

					// --------------INICIO ENVÍO A WS OFFLINE

					if (strXML.contains("<?xml version")) {
						if (strDocType.equals("01") || strDocType.equals("04")
								|| strDocType.equals("05")
								|| strDocType.equals("07")) {
							ClientSOAP clientWS = new ClientSOAP();
							String strWSResult = null;

							try {
								strWSResult = clientWS.sendInvoice(strXML,
										invoice.getId().toString(), invoice,
										null, null, null, strURLWSOffline,
										strCorreoPorDefecto,intTimeOutPara);

								log4j.debug("VALOR RESULTADO: " + strWSResult);

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
									+ "La generación de Documento Electrónico solo es válida para documentos cuyo respectivo Tipo de Documento Electrónico sea Factura de Venta,"
									+ " Nota de Crédito, Nota de Débito o Retención.";
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
				strResult = "No es posible generar Documento Electrónico. "
						+ "Tipo de documento no marcado como Documento Electrónico.";
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
							invoice.setEeiStatus("NG");
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
				// InsertInvoiceLog(invoice, strResult, strTipoResult);

				InsertLogs(conn, invoice, strResult, strTipoResult,strSessionUserId,strDocType);

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

	public static void UpdateInvoice(ConnectionProvider connectionProvider,
			String strInvID, String strClaveAcceso, String strURLXml,
			String strURLRide, boolean boolKeyAccessGenerate) {
		String strSql = null;

		if (!boolKeyAccessGenerate) {
			strSql = "update c_invoice set em_eei_codigo=?, em_eei_urlxml=?,em_eei_urlride=?, em_eei_status='GE' where c_invoice_id =?";
		} else {
			strSql = "update c_invoice set em_eei_urlxml=?, em_eei_urlride=?, em_eei_status='GE' where c_invoice_id =?";
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

			// throw new OBException(e.getMessage());
		} catch (Exception ex) {

			log4j.debug(ex.getMessage());

			// throw new OBException(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {

				log4j.debug(ignore.getMessage());

				ignore.printStackTrace();
				// throw new OBException(ignore.getMessage());

			}
		}
	}

	public static void InsertLogs(ConnectionProvider connectionProvider,
			Invoice invoice, String strLog, String strTipo,String strSessionUserId,String strDocTypeId) {
		String strSql = null;
		String strInvoiceId = invoice.getId();

		strSql = "INSERT INTO EEI_INVOICELOG (C_INVOICE_ID,LINE,DESCRIPTION,ISACTIVE,EEI_INVOICELOG_ID,AD_CLIENT_ID,AD_ORG_ID,CREATED,CREATEDBY,UPDATED,UPDATEDBY,LOGTYPE,EDOC_TYPE)\n"
				+ "VALUES(?,\n"
				+ "(SELECT COALESCE(MAX(LINE),0)+10 FROM  EEI_INVOICELOG WHERE C_INVOICE_ID = ?),\n"
				+ "? ,'Y',GET_UUID(),\n"
				+ "?,\n"
				+ "?,NOW(),\n"
				+ "?,NOW(),\n"
				+ "?,\n"
				+ "?,\n"
				+ "?)";

		int updateCount = 0;
		PreparedStatement st = null;

		try {
			st = connectionProvider.getPreparedStatement(strSql);
			st.setString(1, strInvoiceId);
			st.setString(2, strInvoiceId);
			st.setString(3, strLog);
			st.setString(4, invoice.getClient().getId());
			st.setString(5, invoice.getOrganization().getId());
			st.setString(6, strSessionUserId);
			st.setString(7, strSessionUserId);
			st.setString(8, strTipo);
			st.setString(9, strDocTypeId);

			updateCount = st.executeUpdate();

			if (updateCount > 0) {
				log4j.debug("Log Insertado.");
			} else {
				log4j.debug("Log NO Insertado.");
			}

			st.close();
		} catch (Exception ex) {

			log4j.debug(ex.getMessage());

			// throw new OBException(ex.getMessage());
		} finally {
			try {
				connectionProvider.releasePreparedStatement(st);
			} catch (Exception ignore) {

				log4j.debug(ignore.getMessage());

				ignore.printStackTrace();
				// throw new OBException(ignore.getMessage());

			}
		}
	}

	public static void setKeyAccess(Invoice invoice, String strDoctype) {
		// CLAVE DE ACCESO
		String strClaveAcceso = "";

		// FECHA DE EMISIÓN
		SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
		String strFechaEmision = null;
		if (strDoctype.equals("07")) {
			if (invoice.getSswhDatewithhold() == null) {
				throw new OBException("Fecha Retención no seleccionada.");
			}
			strFechaEmision = dateFormat.format(invoice.getSswhDatewithhold());

		} else {
			strFechaEmision = dateFormat.format(invoice.getInvoiceDate());
		}
		strFechaEmision = (strFechaEmision == null ? "" : strFechaEmision);

		// RUC
		String strRuc = invoice.getOrganization()
				.getOrganizationInformationList().get(0).getTaxID();
		if (!strRuc.matches("^\\d{13}$")) {
			throw new OBException(
					"El formato del RUC de la organización es incorrecto.");
		}
		strRuc = (strRuc == null ? "" : strRuc);

		// DOCUMENTNO
		String strSubDocumentNo = null;
		String strSubDocumentNo1 = null;
		if (strDoctype.equals("07")) {
			String orderRef = invoice.getSswhWithholdingref();

			if (orderRef == null || orderRef.equals(""))
				throw new OBException(
						"La factura no tiene asociada el Número de Retención");
			if (orderRef.length() < 8) {
				throw new OBException(
						"Formato de número de documento inválido.(Prefijo 000-000-) ");
			}

			strSubDocumentNo = orderRef.substring(8);
			strSubDocumentNo1 = truncate(orderRef, 8);

		} else {
			if (invoice.getDocumentNo().length() < 8) {
				throw new OBException(
						"Formato de número de documento inválido. (Prefijo 000-000-).");
			}
			strSubDocumentNo = invoice.getDocumentNo().substring(8);
			strSubDocumentNo1 = truncate(invoice.getDocumentNo(), 8);
		}

		while (strSubDocumentNo.length() < 9) {
			strSubDocumentNo = "0" + strSubDocumentNo;
		}

		String documentnoX = strSubDocumentNo1 + strSubDocumentNo;
		String[] documentno = null;

		if (documentnoX.matches("^\\d{3}-\\d{3}-\\d{9}$")) {
			documentno = documentnoX.split("-");
		} else {
			throw new OBException(
					"El formato del número de documento es incorrecto (000-000-000000000).");
		}
		strSubDocumentNo = (strSubDocumentNo == null ? "" : strSubDocumentNo);

		// AMBIENTE
		OBCriteria<EEIParamFacturae> objFEParams = OBDal.getInstance()
				.createCriteria(EEIParamFacturae.class);
		objFEParams
				.add(Restrictions.eq(EEIParamFacturae.PROPERTY_ACTIVE, true));
		EEIParamFacturae objParam = (EEIParamFacturae) objFEParams
				.uniqueResult();
		if (objParam == null) {
			throw new OBException(
					"No se encontró parametrización de envío de documentos electrónicos.");
		}
		String strAmbiente = objParam.getEnvironment();
		strAmbiente = (strAmbiente == null ? "" : strAmbiente);

		// ALEATORIO
		Random digit = new Random();
		String strRandom = "";
		for (int count = 0; count < 8; count++) {
			strRandom = strRandom + digit.nextInt(10);
		}
		String strTipoEmision = "1";
		strClaveAcceso = strFechaEmision + strDoctype + strRuc + strAmbiente
				+ documentno[0] + documentno[1] + documentno[2] + strRandom
				+ strTipoEmision;
		strClaveAcceso = strClaveAcceso + mod11(strClaveAcceso);

		// ACTUALIZA CLAVE DE ACCESO
		if (strDoctype.equals("03")) {
			invoice.setEeiCodigo2(strClaveAcceso);
		} else {
			invoice.setEeiCodigo(strClaveAcceso);
		}
		OBDal.getInstance().save(invoice);
		OBDal.getInstance().flush();

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

	public static int mod11(String key) {
		int mod11, result, total = 0;

		log4j.debug(key);

		if (!key.matches("^\\d{48}$")) {
			log4j.debug("EL FORMATO DE LA CLAVE DE ACCESO ES INCORRECTO");
		}

		for (int i = key.length() - 1, weight = 2; i >= 0; i--) {
			total = total + (Character.getNumericValue(key.charAt(i)) * weight);

			if (weight == 7) {
				weight = 2;
			} else {
				weight++;
			}
		}

		mod11 = 11 - (total % 11);

		switch (mod11) {
		case 11:
			result = 0;
			break;
		case 10:
			result = 1;
			break;
		default:
			result = mod11;
			break;
		}

		return result;
	}
}
