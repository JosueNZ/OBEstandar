package ec.cusoft.facturaec.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Random;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerContext;

import ec.cusoft.facturaec.EEIParamFacturae;
import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;
import ec.cusoft.facturaec.filewriter.RemissionGuideGenerationEcuador;

public class Generate_Remission_Guide extends DalBaseProcess {
	OBError message;
	static Logger log4j = Logger.getLogger(Generate_Remission_Guide.class);
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
	// public String successMessage = null;
	public String strFinancialAccountID = null;
	public String strDocumentnoPaymentIn;
	public static final String TRXTYPE_BPDeposit = "BPD";
	public static final String TRXTYPE_BPWithdrawal = "BPW";
	public static final String TRXTYPE_BankFee = "BF";
	public String strFinPaymentScheduleDetailID = "";

	// private AdvPaymentMngtDao dao;

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		String strSessionUserId = OBContext.getOBContext().getUser().getId();		
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			message = new OBError();
			String strInoutID = (String) bundle.getParams().get("M_InOut_ID");
			String strTableID = "319";// ID DE TABLA M_INOUT

			ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class,
					strInoutID);

			Boolean boolButtonStatus = inout.isEeiTemporalsend();
			String strStatus = inout.getEeiStatus();

			// ******SE COMPRUEBA SI EL BOTÓN FUE PRESIONADO
			if ((!boolButtonStatus && strStatus == null)
					|| (!boolButtonStatus && (!strStatus.equals("GE")
							&& !strStatus.equals("RE")
							&& !strStatus.equals("AU") && !strStatus
								.equals("PP")))) {
				processRequest(strInoutID, strTableID,strSessionUserId);
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
	}

	public void processRequest(String strInoutID, String strTableID,String strSessionUserId) {
		message = new OBError();
		ConnectionProvider conn = new DalConnectionProvider(false);
		String language = OBContext.getOBContext().getLanguage().getLanguage();

		ShipmentInOut inout1 = OBDal.getInstance().get(ShipmentInOut.class,
				strInoutID);
		ShipmentInOut inout = OBDal.getInstance().get(ShipmentInOut.class,
				strInoutID);
		String strResult = null, strTipoResult = "E", strDocType = "", strStatus = null;
		boolean boolIsEDoc = false, boolYaEnviado = false;
		;

		try {

			OBContext.setAdminMode(true);
			// INICIO ACTUALIZA BOTÓN PRESIONADO AL PRESIONAR POR PRIMERA VEZ
			inout1.setEeiTemporalsend(true);
			OBDal.getInstance().save(inout1);
			OBDal.getInstance().flush();

			// FIN ACTUALIZA BOTÓN PRESIONADO

			log4j.debug("---------------------------------------------------------------------------------------");
			log4j.debug("PROCESANDO DOCUMENTO ELECTRÓNICO "
					+ inout.getDocumentNo() + " (ID:" + strInoutID + ")");

			ConnectionProvider con = new DalConnectionProvider(false);

			// *************OBTENER DATOS DEL DOCUMENTO ELECTRÓNICO
			try {
				strDocType = inout.getDocumentType().getEeiEdocType()
						.toString().replaceAll("\\s", "");
				boolIsEDoc = inout.getDocumentType().isEeiIsEdoc();
				strStatus = (inout.getEeiStatus() == null ? "ND" : inout
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
				log4j.debug("Es documento electrónico.");

				if (!strStatus.equals("AU") && !strStatus.equals("RE")
						&& !strStatus.equals("GE") && !strStatus.equals("PP")) {

					boolean boolKeyAccessGenerate = (ClientSOAP.SelectParams(3)
							.equals("Y") ? true : false);

					if (boolKeyAccessGenerate) {
						boolean boolKeyGenerate = inout.getDocumentType()
								.isEeiKeyAccessGenerated();
						if ((inout.getEeiCodigo() != null && !boolKeyGenerate && (strStatus == null
								|| strStatus.equals("")
								|| strStatus.equals("NR")
								|| strStatus.equals("NA") || strStatus
									.equals("ND")))
								|| (inout.getEeiCodigo() != null
										&& boolKeyGenerate && (strStatus
										.equals("NR") || strStatus.equals("NA")))
								|| (inout.getEeiCodigo() == null || inout
										.getEeiCodigo().equals(""))) {
							try {
								setKeyAccess(inout, strDocType);
							} catch (Exception e) {
								strResult = ("Error al generar clave de acceso:  " + e
										.getMessage());
								strTipoResult = "E";
								throw new OBException(strResult);
							}
						}
					}

					String strXML = null;
					if (strDocType.equals("06")) {// Guía de Remisión
						try {
							log4j.debug("Ejecutando clase de generación de xml.");

							RemissionGuideGenerationEcuador remissionguide = new RemissionGuideGenerationEcuador();
							strXML = remissionguide.generateFile(inout,
									language);
							log4j.debug("Termina clase de generación de xml.");
						} catch (OBException e) {
							strResult = ("Error al generar XML Guía de Remisión:  "
									+ e.getMessage() + ". " + strXML);
							strTipoResult = "E";
							throw new OBException(strResult);
						}

					} else {
						strResult = "Error: "
								+ " No es posible generar Documento Electrónico. "
								+ "Tipo de documento no configurado como Guía de Remisión.";
						strTipoResult = "E";
						throw new OBException(strResult);
					}

					log4j.debug(strXML);

					// ---------------FIN GENERACIÓN XML

					// --------------INICIO ENVÍO A WS OFFLINE

					if (strXML.contains("<?xml version")) {
						if (strDocType.equals("06")) {
							ClientSOAP clientWS = new ClientSOAP();

							String strWSResult = null;

							try {
								strWSResult = clientWS.sendInvoice(strXML,
										inout.getId().toString(), null, inout,
										null, strTableID, null, null,0);
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

											UpdateInvoice(conn, strInoutID,
													strClaveAcceso, strUrlXML,
													strUrlRide);

										} catch (Exception ex) {
											strResult = "Error al actualizar el estado y campos de Documento Electrónico. "
													+ ex.getMessage();
											strTipoResult = "E";
											throw new OBException(strResult);
										}

										strResult = "GENERADO";
										strTipoResult = "S";
									} catch (Exception e) {
										strResult = "Error al actualizar datos de documento Generado (OB). "
												+ e.getMessage();
										strTipoResult = "E";
										throw new OBException(strResult);

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
									+ "No es posible generar Documento Electrónico. "
									+ "Tipo de documento no configurado como Guía de Remisión.";
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
						+ "La parametrización del tipo de documento no es válida.";
				strTipoResult = "E";
				throw new OBException(strResult);
			}

		} catch (Exception e) {
			e.printStackTrace();
			strResult = "ERROR. "
					+ (e.getMessage() == null ? "" : e.getMessage());
			strTipoResult = "E";

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
							inout.setEeiStatus("NG");
							OBDal.getInstance().save(inout);
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
				InsertLogs(conn, inout, strResult, strTipoResult, strTableID,strSessionUserId);

				// ACTUALIZAR ESTADO BOTÓN NO PRESIONADO
				inout.setEeiTemporalsend(false);
				OBDal.getInstance().save(inout);
				OBDal.getInstance().flush();
				// CIERRA CONEXION A BD
				try {
					conn.destroy();
				} catch (Exception e) {

				}
			} catch (Exception e) {

				log4j.debug("No se pudo insertar el log. " + e);
				// ACTUALIZAR ESTADO BOTÓN NO PRESIONADO
				inout.setEeiTemporalsend(false);
				OBDal.getInstance().save(inout);
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
			OBDal.getInstance().commitAndClose();
			OBContext.restorePreviousMode();

		}

	}

	public static void UpdateInvoice(ConnectionProvider connectionProvider,
			String strInvID, String strClaveAcceso, String strURLXml,
			String strURLRide) {

		String strSql = null;

		strSql = "update m_inout set em_eei_codigo=?, em_eei_urlxml=?,em_eei_urlride=?, em_eei_status='GE' where m_inout_id =?";

		int updateCount = 0;
		PreparedStatement st = null;

		try {
			st = connectionProvider.getPreparedStatement(strSql);
			st.setString(1, strClaveAcceso);
			st.setString(2, strURLXml);
			st.setString(3, strURLRide);
			st.setString(4, strInvID);

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

	public static void InsertLogs(ConnectionProvider connectionProvider,
			ShipmentInOut inout, String strLog, String strTipo,
			String strTableID,String strUserSessionId) {
		String strSql = null;
		String strInoutId = inout.getId();

		strSql = "INSERT INTO EEI_REMISSIONGUIDELOG(\n"
				+ "            EEI_REMISSIONGUIDELOG_ID, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE, \n"
				+ "            CREATED, CREATEDBY, UPDATED, UPDATEDBY, LINE, AD_TABLE_ID, RECORD_ID, \n"
				+ "            LOGTYPE, DESCRIPTION)\n"
				+ "    VALUES (GET_UUID(),?,?,\n"
				+ "     'Y',NOW(),?,NOW(),?,\n"
				+ "          (SELECT COALESCE(MAX(LINE),0)+10 FROM  EEI_REMISSIONGUIDELOG WHERE AD_TABLE_ID=? AND RECORD_ID =?), ?, ?, ?, ?)";

		int updateCount = 0;
		PreparedStatement st = null;

		try {
			st = connectionProvider.getPreparedStatement(strSql);
			st.setString(1, inout.getClient().getId());
			st.setString(2, inout.getOrganization().getId());
			st.setString(3, strUserSessionId);//CREATEDBY
			st.setString(4, strUserSessionId);//UPDATEDBY

			st.setString(5, strTableID);
			st.setString(6, strInoutId);
			st.setString(7, strTableID);
			st.setString(8, strInoutId);
			st.setString(9, strTipo);
			st.setString(10, strLog);

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

	public static void setKeyAccess(ShipmentInOut inout, String strDoctype) {
		// CLAVE DE ACCESO
		String strClaveAcceso = "";

		// FECHA DE EMISIÓN
		SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy");
		String strFechaEmision = null;

		strFechaEmision = dateFormat.format(inout.getMovementDate());
		strFechaEmision = (strFechaEmision == null ? "" : strFechaEmision);

		// RUC
		String strRuc = inout.getOrganization()
				.getOrganizationInformationList().get(0).getTaxID();
		if (!strRuc.matches("^\\d{13}$")) {
			throw new OBException(
					"El formato del RUC de la organización es incorrecto.");
		}
		strRuc = (strRuc == null ? "" : strRuc);

		// DOCUMENTNO
		String strSubDocumentNo = null;
		String strSubDocumentNo1 = null;

		if (inout.getDocumentNo().length() < 8) {
			throw new OBException(
					"Formato de número de documento inválido. (Prefijo 000-000-).");
		}
		strSubDocumentNo = inout.getDocumentNo().substring(8);
		strSubDocumentNo1 = truncate(inout.getDocumentNo(), 8);

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
		inout.setEeiCodigo(strClaveAcceso);
		OBDal.getInstance().save(inout);
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
