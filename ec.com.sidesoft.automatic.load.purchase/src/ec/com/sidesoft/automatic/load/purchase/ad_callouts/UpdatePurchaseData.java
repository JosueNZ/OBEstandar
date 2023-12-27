package ec.com.sidesoft.automatic.load.purchase.ad_callouts;

import java.io.StringReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ec.gob.sri.comprobantes.ws.aut.Autorizacion;

public class UpdatePurchaseData extends SimpleCallout {

	@Override
	protected void execute(CalloutInfo info) throws ServletException {

		try {
			String strAuthorizationNum = info.getStringParameter(
					"inpemSswhNroauthorization", null);
			if (strAuthorizationNum.length() == 49) {

				String strURLAuthorization = null, strNumeroComprobantes = null;

				OBCriteria<Organization> obcParameters = OBDal.getInstance()
						.createCriteria(Organization.class);
				obcParameters.add(Restrictions.eq(Organization.PROPERTY_ACTIVE,
						true));
				OrganizationType objOrgType = OBDal.getInstance().get(
						OrganizationType.class, "1");
				obcParameters.add(Restrictions.eq(
						Organization.PROPERTY_ORGANIZATIONTYPE, objOrgType));

				if (obcParameters.list().size() == 0) {
					throw new OBException(
							"No existe una organización legal contable activa.");
				}
				Organization objParameter = null;
				objParameter = OBDal.getInstance().get(Organization.class,
						obcParameters.list().get(0).getId());

				if (objParameter.getSsalpEnvironment().equals("1")) {
					strURLAuthorization = objParameter.getSsalpUrlAuthoTest();
				} else {
					strURLAuthorization = objParameter.getSsalpUrlAuthoProd();
				}

				ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante wsAnswer = null;
				try {
					ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesOfflineService service = new ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesOfflineService(
							new URL(strURLAuthorization));
					ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesOffline pt = service
							.getAutorizacionComprobantesOfflinePort();
					wsAnswer = pt.autorizacionComprobante(strAuthorizationNum);
					strNumeroComprobantes = wsAnswer.getNumeroComprobantes();

				} catch (Exception e) {

					throw new OBException(
							"Error al consultar clave de acceso en servicios del SRI. "
									+ e.getMessage());
				}

				strNumeroComprobantes = (strNumeroComprobantes == null ? "0"
						: strNumeroComprobantes);

				if (!strNumeroComprobantes.equals("0")) {
					Autorizacion objAutorizaciones = wsAnswer
							.getAutorizaciones().getAutorizacion().get(0);
					String strStatus = objAutorizaciones.getEstado();
					strStatus = (strStatus == null ? "" : strStatus);

					if (strStatus.equals("AUTORIZADO")) {
						String strTipoDocumento = null, strIdentificacion = null, strFechaEmision = null, strEstablecimiento = null, strSecuencial = null, strPuntoEmision = null, strNumeroDocumento = null;
						try {

							DocumentBuilderFactory factory = DocumentBuilderFactory
									.newInstance();
							DocumentBuilder builder = factory
									.newDocumentBuilder();
							Document documento = builder.parse(new InputSource(
									new StringReader(objAutorizaciones
											.getComprobante())));

							NodeList ndInfoTributaria = documento
									.getElementsByTagName("infoTributaria");
							Element elmInfoTributaria = (Element) ndInfoTributaria
									.item(0);

							strTipoDocumento = elmInfoTributaria
									.getElementsByTagName("codDoc").item(0)
									.getFirstChild().getTextContent();
							if (!strTipoDocumento.equals("01")) {
								throw new OBException(
										"La clave de acceso consultada no corresponde a una factura de venta.");
							}

							// IDENTIFICACIÓN
							strIdentificacion = elmInfoTributaria
									.getElementsByTagName(
											"ruc").item(0)
									.getFirstChild().getTextContent();

							NodeList infoFactura = documento
									.getElementsByTagName("infoFactura");
							Element elmInfoFactura = (Element) infoFactura
									.item(0);

							// FECHA DE EMISIÓN
							strFechaEmision = elmInfoFactura
									.getElementsByTagName("fechaEmision")
									.item(0).getFirstChild().getTextContent();

							// NÚMERO DE DOCUMENTO

							strEstablecimiento = elmInfoTributaria
									.getElementsByTagName("estab").item(0)
									.getFirstChild().getTextContent();
							strPuntoEmision = elmInfoTributaria
									.getElementsByTagName("ptoEmi").item(0)
									.getFirstChild().getTextContent();
							strSecuencial = elmInfoTributaria
									.getElementsByTagName("secuencial").item(0)
									.getFirstChild().getTextContent();

							strNumeroDocumento = strEstablecimiento + "-"
									+ strPuntoEmision + "-" + strSecuencial;

						} catch (Exception e) {
							throw new OBException(
									"Error al leer archivo XML de número de autorización consultado. "
											+ e.getMessage());
						}

						try {
							OBCriteria<BusinessPartner> obcBusinessPartner = OBDal
									.getInstance().createCriteria(
											BusinessPartner.class);
							obcBusinessPartner.add(Restrictions.eq(
									BusinessPartner.PROPERTY_ACTIVE, true));
							obcBusinessPartner.add(Restrictions.eq(
									BusinessPartner.PROPERTY_VENDOR, true));
							obcBusinessPartner.add(Restrictions.eq(
									BusinessPartner.PROPERTY_TAXID,
									strIdentificacion));

							if (obcBusinessPartner.list().size() > 0
									&& strFechaEmision != null
									&& strNumeroDocumento != null) {
								
								BusinessPartner objBusinessPartner = obcBusinessPartner
										.list().get(0);
								info.addResult("inpcBpartnerId",
										objBusinessPartner.getId());

								SimpleDateFormat objFormato = new SimpleDateFormat(
										"dd/MM/yyyy");
								Date dtFechaEmision = objFormato
										.parse(strFechaEmision);
								info.addResult("inpdateinvoiced",
										dtFechaEmision);
								info.addResult("inpdateacct", dtFechaEmision);
								info.addResult("inpemSswhExpirationdate",
										dtFechaEmision);

								info.addResult("inpporeference",
										strNumeroDocumento);
								info.addResult("inpemSswhIseinvoice", true);
								info.addResult("SUCCESS",
										"Datos de compra cargados: - Tercero: "+objBusinessPartner.getName()+" - Fecha de Emisión: "+strFechaEmision+" - Número de documento: "+strNumeroDocumento);

							} else if (obcBusinessPartner.list().size() == 0) {
								throw new OBException(
										"No existe un proveedor activo registrado en Openbravo con identificación: "
												+ strIdentificacion);
							} else if (strFechaEmision == null) {
								throw new OBException("Fecha de emisión nula.");
							} else if (strNumeroDocumento == null) {
								throw new OBException(
										"Número de documento nulo.");
							}

						} catch (Exception e) {
							throw new OBException(
									"Error al insertar información de factura consultada. "
											+ e.getMessage());
						}

					} else {
						throw new OBException(
								"Número de autorización consultada en estado: "
										+ strStatus);
					}

				} else {
					throw new OBException(
							"Número de autorización consultada no encontrada.");
				}
			}
		} catch (Exception e) {
			info.addResult("ERROR",
					(e.getMessage() == null ? "" : e.getMessage()));
		}
	}
}
