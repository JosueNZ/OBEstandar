package ec.cusoft.facturaec.filewriter;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.access.InvoiceLineTax;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.geography.CountryTrl;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.db.DalConnectionProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sidesoft.localization.ecuador.refunds.ssrerefundinvoice;

import ec.cusoft.facturaec.EEIParamFacturae;
import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;

public class PurchaseSettlementGenerationEcuador {

	static Logger log4j = Logger
			.getLogger(PurchaseSettlementGenerationEcuador.class);

	public String generateFile(Invoice invoice, String rootDirectory,
			String lang) throws Exception {
		String strDocType = invoice.getDocumentType().getEeiEdocType()
				.toString().replaceAll("\\s", "");
		boolean boolIsEDoc = invoice.getDocumentType().isEeiIsEdoc();

		if (!boolIsEDoc) {
			throw new OBException(
					"No es posible generar Documento Electrónico,la parametrización del tipo de documento no es válida.");
		}
		if (!strDocType.trim().equals("03")) {
			throw new OBException(
					"Tipo de documento electrónico no configurado como Liquidación de compra.");
		}
		DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// ELEMENTOS INICIALES
		Document doc = docBuilder.newDocument();
		doc.setXmlStandalone(true);
		Element rootElement = doc.createElement("liquidacionCompra");
		rootElement.setAttribute("id", "comprobante");
		rootElement.setAttribute("version", "1.0.0");
		doc.appendChild(rootElement);

		// INFOTRIBUTARIA
		Element infoTributaria = doc.createElement("infoTributaria");
		rootElement.appendChild(infoTributaria);

		Element tipoEmision = doc.createElement("tipoEmision");
		tipoEmision.appendChild(doc.createTextNode("1"));
		infoTributaria.appendChild(tipoEmision);

		// RUC
		String strRuc = invoice.getOrganization()
				.getOrganizationInformationList().get(0).getTaxID();
		if (!strRuc.matches("^\\d{13}$")) {
			throw new OBException("El formato del RUC es incorrecto.");
		}
		Element ruc = doc.createElement("ruc");
		ruc.appendChild(doc.createTextNode(strRuc));
		infoTributaria.appendChild(ruc);

		// CLAVE DE ACCESO
		boolean strKeyAccessGenerate = (ClientSOAP.SelectParams(3).equals("Y") ? true
				: false);
		    // New Name - Description
		    boolean strExtract = (ClientSOAP.SelectParams(4).equals("Y") ? true : false);

		if (strKeyAccessGenerate) {
			String strKeyAccess = null;
			strKeyAccess = invoice.getEeiCodigo2();
			if (strKeyAccess == null || strKeyAccess.equals("")) {
				throw new OBException("Clave de acceso no encontrada.");
			}
			if (strKeyAccess.length() != 49) {
				throw new OBException(
						"Extensión de clave de acceso no válida (49 dígitos).");
			}
			Element keyAccess = doc.createElement("claveAcceso");
			keyAccess.appendChild(doc.createTextNode(strKeyAccess));
			infoTributaria.appendChild(keyAccess);
		}

		// TIPO DE DOCUMENTO
		if (invoice.getDocumentType() == null) {
			throw new OBException(
					"No se encontró un tipo de documento para la retención.");
		}
		String strCodDoc = invoice.getDocumentType().getEeiEdocType();
		Element codDoc = doc.createElement("codDoc");
		codDoc.appendChild(doc.createTextNode(strCodDoc));
		infoTributaria.appendChild(codDoc);

		// NÚMERO DE DOCUMENTO
		String orderRef = invoice.getDocumentNo();
		if (orderRef == null || orderRef.equals("")) {
			throw new OBException(
					"La factura no tiene asociada el Número de Retención.");
		}
		if (invoice.getDocumentNo().length() < 8) {
			throw new OBException(
					"Formato de número de documento inválido. (Prefijo 000-000-).");
		}
		String strSubDocumentNo = orderRef.substring(8);
		while (strSubDocumentNo.length() < 9) {
			strSubDocumentNo = "0" + strSubDocumentNo;
		}
		String strSubDocumentNo1 = invoice.getDocumentNo().substring(0, 8);
		String documentnoX = strSubDocumentNo1 + strSubDocumentNo;
		String[] documentno = null;
		if (documentnoX.matches("^\\d{3}-\\d{3}-\\d{9}$")) {
			documentno = documentnoX.split("-");
		} else {
			throw new OBException(
					"El formato del número de documento de la Orden de Compra es incorrecto (000-000-000000000).");
		}

		// ESTABLECIMIENTO
		Element estab = doc.createElement("estab");
		estab.appendChild(doc.createTextNode(documentno[0]));
		infoTributaria.appendChild(estab);

		// PUNTO DE EMISIÓN
		Element ptoEmi = doc.createElement("ptoEmi");
		ptoEmi.appendChild(doc.createTextNode(documentno[1]));
		infoTributaria.appendChild(ptoEmi);

		// SECUENCIAL
		Element secuencial = doc.createElement("secuencial");
		secuencial.appendChild(doc.createTextNode(documentno[2]));
		infoTributaria.appendChild(secuencial);
	    
	    // AGENTE DE RETENCIÓN - MICROEMPRESAS
	    OBCriteria<Organization> ObjOrganization = OBDal.getInstance()
	        .createCriteria(Organization.class);
	    OrganizationType objOrganizationType = OBDal.getInstance().get(OrganizationType.class, "1");
	    ObjOrganization
	        .add(Restrictions.eq(Organization.PROPERTY_ORGANIZATIONTYPE, objOrganizationType));

	    String strMicroBusiness = null;
	    String strWithholdingAgent = null;
	    String strRegimeRimpe = null;

	    if (ObjOrganization.list().size() > 0) {
	    	strMicroBusiness = ObjOrganization.list().get(0).getEeiMicroBusiness();
	    	strWithholdingAgent = ObjOrganization.list().get(0).getEeiWithholdingAgent(); 
	    	strRegimeRimpe = ObjOrganization.list().get(0).getEeiRimpe();
	    }

	    if (strMicroBusiness != null) {
	        Element regimenMicroempresas = doc.createElement("regimenMicroempresas");
	        regimenMicroempresas.appendChild(doc.createTextNode(strMicroBusiness));
	        infoTributaria.appendChild(regimenMicroempresas);
	    }
	    
	    if (strWithholdingAgent != null) {
	        Element agenteRetencion = doc.createElement("agenteRetencion");
	        agenteRetencion.appendChild(doc.createTextNode(strWithholdingAgent));
	        infoTributaria.appendChild(agenteRetencion);
	    }
	    
	    if (strRegimeRimpe != null) {
		    Element contribuyenteRimpe = doc.createElement("contribuyenteRimpe");
		    contribuyenteRimpe.appendChild(doc.createTextNode(strRegimeRimpe));
		    infoTributaria.appendChild(contribuyenteRimpe);
	    }
	    
		// DIRECCIÓN MATRIZ
		String headquartersCountry = null;
		try {
			OBContext.setAdminMode(true);
			for (CountryTrl countryTrl : invoice.getOrganization()
					.getOrganizationInformationList().get(0)
					.getLocationAddress().getCountry().getCountryTrlList()) {
				if (countryTrl.getLanguage().getLanguage().equals(lang)) {
					headquartersCountry = countryTrl.getName();
				}
			}
		} catch (NullPointerException e) {
			throw new OBException("La Organización no tiene dirección.");
		} finally {
			OBContext.restorePreviousMode();
		}

		if (headquartersCountry == null) {
			headquartersCountry = invoice.getOrganization()
					.getOrganizationInformationList().get(0)
					.getLocationAddress().getCountry().getName();
		}

		String headQuartersaddress = (invoice.getOrganization()
				.getOrganizationInformationList().get(0).getLocationAddress()
				.getAddressLine1() == null ? " " : invoice.getOrganization()
				.getOrganizationInformationList().get(0).getLocationAddress()
				.getAddressLine1())
				+ "--"
				+ (invoice.getOrganization().getOrganizationInformationList()
						.get(0).getLocationAddress().getAddressLine2() == null ? " "
						: invoice.getOrganization()
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getAddressLine2())
				+ "--"
				+ (invoice.getOrganization().getOrganizationInformationList()
						.get(0).getLocationAddress().getPostalCode() == null ? " "
						: invoice.getOrganization()
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getPostalCode())
				+ "--"
				+ (invoice.getOrganization().getOrganizationInformationList()
						.get(0).getLocationAddress().getCityName() == null ? " "
						: invoice.getOrganization()
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getCityName())
				+ "--"
				+ (invoice.getOrganization().getOrganizationInformationList()
						.get(0).getLocationAddress().getRegion() == null ? " "
						: invoice.getOrganization()
								.getOrganizationInformationList().get(0)
								.getLocationAddress().getRegion().getName())
				+ "--" + headquartersCountry;

		// INFO LIQUIDACIÓN COMPRA
		Element infoLiquidacionCompra = doc
				.createElement("infoLiquidacionCompra");
		rootElement.appendChild(infoLiquidacionCompra);

		// FECHA DE EMISIÓN
		Element fechaEmision = doc.createElement("fechaEmision");
		SimpleDateFormat ecFormat = new SimpleDateFormat("dd/MM/yyyy");

		if (invoice.getInvoiceDate() == null) {
			throw new OBException("Fecha Retención no seleccionada.");
		}
		fechaEmision.appendChild(doc.createTextNode(ecFormat.format(invoice
				.getInvoiceDate())));
		infoLiquidacionCompra.appendChild(fechaEmision);

		// DIRECCIÓN ESTABLECIMIENTO
		String country = null;
		try {
			OBContext.setAdminMode(true);
			for (CountryTrl countryTrl : invoice.getPartnerAddress()
					.getLocationAddress().getCountry().getCountryTrlList()) {
				if (countryTrl.getLanguage().getLanguage().equals(lang)) {
					country = countryTrl.getName();
				}
			}
		} finally {
			OBContext.restorePreviousMode();
		}

		if (country == null) {
			country = invoice.getPartnerAddress().getLocationAddress()
					.getCountry().getName();
		}

		String address = (invoice.getPartnerAddress().getLocationAddress()
				.getAddressLine1() == null ? " " : invoice.getPartnerAddress()
				.getLocationAddress().getAddressLine1())
				+ "--"
				+ (invoice.getPartnerAddress().getLocationAddress()
						.getAddressLine2() == null ? " " : invoice
						.getPartnerAddress().getLocationAddress()
						.getAddressLine2())
				+ "--"
				+ (invoice.getPartnerAddress().getLocationAddress()
						.getPostalCode() == null ? " " : invoice
						.getPartnerAddress().getLocationAddress()
						.getPostalCode())
				+ "--"
				+ (invoice.getPartnerAddress().getLocationAddress()
						.getCityName() == null ? " " : invoice
						.getPartnerAddress().getLocationAddress().getCityName())
				+ "--"
				+ (invoice.getPartnerAddress().getLocationAddress().getRegion() == null ? " "
						: invoice.getPartnerAddress().getLocationAddress()
								.getRegion().getName()) + "--" + country;

		if (headQuartersaddress == null) {
			throw new OBException("Dirección de Establecimiento es nula.");
		} else {
			headQuartersaddress = headQuartersaddress.replaceAll("[\n]", " ");
		}
		Element dirEstablecimiento = doc.createElement("dirEstablecimiento");
		dirEstablecimiento.appendChild(doc.createTextNode(truncate(
				headQuartersaddress, 300)));
		infoLiquidacionCompra.appendChild(dirEstablecimiento);

		String idType=null;

		if (invoice.getBusinessPartner().getSswhTaxidtype().equals("R")) {
			idType = "04";
		} else if (invoice.getBusinessPartner().getSswhTaxidtype().equals("D")) {
			idType = "05";
		} else if (invoice.getBusinessPartner().getSswhTaxidtype().equals("P")) {
			idType = "06";
	    } else if (invoice.getBusinessPartner().getSswhTaxidtype().equals("EEI_C")) {
	    	idType = "07";
	    } else if (invoice.getBusinessPartner().getSswhTaxidtype().equals("EEI_E")) {    	
	    	idType = "08";
	    }
		
		Element tipoIdentificacionProveedor = doc
				.createElement("tipoIdentificacionProveedor");
		tipoIdentificacionProveedor.appendChild(doc.createTextNode(idType));
		infoLiquidacionCompra.appendChild(tipoIdentificacionProveedor);

		// RAZÓN SOCIAL

		    String strDescription2 = invoice.getBusinessPartner().getDescription() == null ? "ND"
			: invoice.getBusinessPartner().getDescription();
		    String strName2 = strExtract && !strDescription2.equals("ND") ? strDescription2
			: invoice.getBusinessPartner().getName();
		    
		if (strName2 == null || strName2.trim().equals("")) {
			throw new OBException("El cliente no tiene nombre fiscal.");
		}
		strName2 = truncate(strName2, 300);
		strName2 = strName2.replaceAll("[\n]", " ");
		Element razonSocialProveedor = doc
				.createElement("razonSocialProveedor");
		razonSocialProveedor.appendChild(doc.createTextNode(strName2));
		infoLiquidacionCompra.appendChild(razonSocialProveedor);

		// IDENTIFICACIÓN
		String strTaxid = invoice.getBusinessPartner().getTaxID();
		if (strTaxid == null || strTaxid.trim().equals("")) {
			throw new OBException("El cliente no tiene RUC.");
		}

		Element identificacionProveedor = doc
				.createElement("identificacionProveedor");
		identificacionProveedor.appendChild(doc.createTextNode(strTaxid));
		infoLiquidacionCompra.appendChild(identificacionProveedor);

		// DIRECCIÓN PROVEEDOR
		String strDireccionTercero = ClientSOAP.getAddressComplete(invoice
				.getPartnerAddress());
		Element direccionProveedor = doc.createElement("direccionProveedor");
		direccionProveedor.appendChild(doc.createTextNode(strDireccionTercero));
		infoLiquidacionCompra.appendChild(direccionProveedor);

		DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
		simbolos.setDecimalSeparator('.');
		DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);

		// TOTAL SIN IMPUESTOS
		double TotalSinImpuestos = invoice.getSummedLineAmount().doubleValue();
		Element totalSinImpuestos = doc.createElement("totalSinImpuestos");
		totalSinImpuestos.appendChild(doc.createTextNode(formateador.format(
				TotalSinImpuestos).toString()));
		infoLiquidacionCompra.appendChild(totalSinImpuestos);

		// TOTAL DESCUENTO
		Element totalDescuento = doc.createElement("totalDescuento");
		totalDescuento.appendChild(doc.createTextNode(formateador.format(0)
				.toString()));
		infoLiquidacionCompra.appendChild(totalDescuento);

		// *****************************INICIO REEMBOLSOS CABECERA

		String strRefundCode = null, strRefundTotal = null, strRefundBaseImponible = null, strRefundTotalImpuestos = null;

		if (invoice.getSswhLivelihood().isRefund()) {
			try {

				strRefundCode = invoice.getSswhLivelihood().getSearchKey();
				strRefundTotal = formateador.format(invoice
						.getGrandTotalAmount().doubleValue());
				strRefundBaseImponible = formateador.format(invoice
						.getSummedLineAmount().doubleValue());
				strRefundTotalImpuestos = formateador.format(invoice
						.getGrandTotalAmount()
						.subtract(invoice.getSummedLineAmount()).doubleValue());

				if (strRefundCode != null && strRefundTotal != null
						&& strRefundBaseImponible != null
						&& strRefundTotalImpuestos != null) {

					Element codDocReembolso = doc
							.createElement("codDocReembolso");
					codDocReembolso.appendChild(doc
							.createTextNode(strRefundCode));
					infoLiquidacionCompra.appendChild(codDocReembolso);

					Element totalComprobantesReembolso = doc
							.createElement("totalComprobantesReembolso");
					totalComprobantesReembolso.appendChild(doc
							.createTextNode(strRefundTotal));
					infoLiquidacionCompra
							.appendChild(totalComprobantesReembolso);

					Element totalBaseImponibleReembolso = doc
							.createElement("totalBaseImponibleReembolso");
					totalBaseImponibleReembolso.appendChild(doc
							.createTextNode(strRefundBaseImponible));
					infoLiquidacionCompra
							.appendChild(totalBaseImponibleReembolso);

					Element totalImpuestoReembolso = doc
							.createElement("totalImpuestoReembolso");
					totalImpuestoReembolso.appendChild(doc
							.createTextNode(strRefundTotalImpuestos));
					infoLiquidacionCompra.appendChild(totalImpuestoReembolso);

				} else {
					String strError = null;
					if (strRefundCode == null
							|| strRefundCode.trim().equals("")) {
						throw new OBException(
								"Identificador del tipo de comprobante en datos de retención de la factura de compra referenciada (reembolso) no configurado.");
					}
					if (strRefundTotal == null
							|| strRefundTotal.trim().equals("")) {
						strError = "Total";
					}
					if (strRefundBaseImponible == null
							|| strRefundBaseImponible.trim().equals("")) {
						strError = "Base Imponible";
					}
					if (strRefundTotalImpuestos == null
							|| strRefundTotalImpuestos.trim().equals("")) {
						strError = "Total Impuestos";
					}
					throw new OBException(
							"Error al obtener valor de "
									+ strError
									+ " de la factura de compra referenciada (reembolso).");
				}

			} catch (Exception e) {

				throw new OBException(
						"Error al obtener información de reembolso (Cabecera). "
								+ e.getMessage());
			}
		}

		// *****************************FIN REEMBOLSOS CABECERA

		// *****************************INICIO TOTAL IMPUESTOS
		Element totalConImpuestos = doc.createElement("totalConImpuestos");
		infoLiquidacionCompra.appendChild(totalConImpuestos);

		for (InvoiceTax impuestoObj : invoice.getInvoiceTaxList()) {

			if (impuestoObj.getTax().isTaxdeductable()) {
				Element totalImpuesto = doc.createElement("totalImpuesto");
				totalConImpuestos.appendChild(totalImpuesto);

				// CÓDIGO DE IMPUESTO
				Element codigo = doc.createElement("codigo");
				codigo.appendChild(doc.createTextNode(impuestoObj.getTax()
						.getEeiSriTaxType()));
				totalImpuesto.appendChild(codigo);

				// CÓDIGO PORCENTAJE
				String strTaxCode = impuestoObj.getTax().getEeiSriTaxcatCode();
				if (strTaxCode == null) {
					throw new OBException(
							"Código de impuesto SRI no configurado. ");
				}
				Element codigoPorcentaje = doc
						.createElement("codigoPorcentaje");
				codigoPorcentaje.appendChild(doc.createTextNode(strTaxCode));
				totalImpuesto.appendChild(codigoPorcentaje);

				// BASE IMPONIBLE
				double BaseImp = impuestoObj.getTaxableAmount().doubleValue();
				Element baseImponible = doc.createElement("baseImponible");
				baseImponible.appendChild(doc.createTextNode(formateador
						.format(BaseImp).toString()));
				totalImpuesto.appendChild(baseImponible);

				// TARIFA
				double Tarifa = impuestoObj.getTax().getRate().doubleValue();
				Element tarifa = doc.createElement("tarifa");
				tarifa.appendChild(doc.createTextNode(formateador
						.format(Tarifa).toString()));
				totalImpuesto.appendChild(tarifa);

				// VALOR
				double Valor = impuestoObj.getTaxAmount().doubleValue();
				Element valor = doc.createElement("valor");
				valor.appendChild(doc.createTextNode(formateador.format(Valor)
						.toString()));
				totalImpuesto.appendChild(valor);

			}
		}

		// *****************************FIN TOTAL IMPUESTOS

		// IMPORTE TOTAL
		double ImporteTotal = invoice.getGrandTotalAmount().doubleValue();
		double dbRetencionFuente = 0, dbRetencionIVA=0;
		
		if (invoice.getSswhTotalwithholdingincome()!=null){
			if(invoice.getSswhTotalwithholdingincome().compareTo(BigDecimal.ZERO)==-1){
				dbRetencionFuente =invoice.getSswhTotalwithholdingincome().doubleValue();
			}
		}
		if (invoice.getSswhTotalwithholdingvat()!=null){
			if(invoice.getSswhTotalwithholdingvat().compareTo(BigDecimal.ZERO)==-1){
				dbRetencionIVA =invoice.getSswhTotalwithholdingvat().doubleValue();
			}
		}		
		Element importeTotal = doc.createElement("importeTotal");
		importeTotal.appendChild(doc.createTextNode(formateador.format(
				ImporteTotal-dbRetencionFuente-dbRetencionIVA).toString()));
		infoLiquidacionCompra.appendChild(importeTotal);

		// MONEDA
		String strMoneda;
		if (invoice.getCurrency().getISOCode().equals("USD")) {
			strMoneda = "DOLAR";
		} else if (invoice.getCurrency().getISOCode().equals("EUR")) {
			strMoneda = "EURO";
		} else {
			strMoneda = "DOLAR";
		}
		Element moneda = doc.createElement("moneda");
		moneda.appendChild(doc.createTextNode(strMoneda));
		infoLiquidacionCompra.appendChild(moneda);

		// *****************************INICIA PAGOS

		int intPaymentScheduleDatail = 0;
		String strFinPaymentScheduleID = "ND";
		for (FIN_PaymentSchedule paymentSchedule2 : invoice
				.getFINPaymentScheduleList()) {
			for (FIN_PaymentScheduleDetail psd2 : paymentSchedule2
					.getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {

				try {
					strFinPaymentScheduleID = psd2.getPaymentDetails().getId() == null ? "ND"
							: psd2.getPaymentDetails().getId();
				} catch (Exception e) {
				}

				if (!strFinPaymentScheduleID.equals("ND")) {
					intPaymentScheduleDatail++;
				}
			}
		}

		if (intPaymentScheduleDatail > 0) {

			Element pagos = doc.createElement("pagos");

			Element pago = null;

			Element pagos2 = doc.createElement("pagos");

			Element pago2 = null;

			double dbdTotalPaymentOut = 0;
			BigDecimal bgdGrandTotalPayment = BigDecimal.ZERO;

			String strPaymentMethod = "ND";

			String strReviewPaymentMethod = "ND";

			for (FIN_PaymentSchedule paymentSchedule : invoice
					.getFINPaymentScheduleList()) {

				List<FIN_PaymentSchedule> lstPaymentSchedule = invoice
						.getFINPaymentScheduleList();

				if (lstPaymentSchedule.size() > 0) {

					for (FIN_PaymentScheduleDetail psd : paymentSchedule
							.getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {

						String strCodePaymentMethod = "ND";

						try {

							pago = doc.createElement("pago");

							Element formaPago = doc.createElement("formaPago");

							try {
								strReviewPaymentMethod = psd
										.getPaymentDetails().getId() == null ? "ND"
										: psd.getPaymentDetails().getId();
							} catch (Exception e1) {
							}
							if (strReviewPaymentMethod.equals("ND")) {

								strCodePaymentMethod = "";

							} else {

								strPaymentMethod = psd.getPaymentDetails()
										.getFinPayment().getPaymentMethod()
										.getId() == null ? "ND" : psd
										.getPaymentDetails().getFinPayment()
										.getPaymentMethod().getId();

								strCodePaymentMethod = psd.getPaymentDetails()
										.getFinPayment().getPaymentMethod()
										.getEeiCodeEi() == null ? "ND" : psd
										.getPaymentDetails().getFinPayment()
										.getPaymentMethod().getEeiCodeEi();

								formaPago.appendChild(doc
										.createTextNode(strCodePaymentMethod));
								pago.appendChild(formaPago);

								Element total = doc.createElement("total");
								double dblpagos = psd.getAmount().doubleValue();
								total.appendChild(doc
										.createTextNode(formateador.format(
												dblpagos).toString()));
								pago.appendChild(total);

								if (invoice
										.getPaymentMethod()
										.getId()
										.equals(psd.getPaymentDetails()
												.getFinPayment()
												.getPaymentMethod().getId())) {

									Element plazo = doc.createElement("plazo");

									String strUnitTime = "";
									if (invoice.getPaymentTerms()
											.getOverduePaymentDaysRule() > 0) {

										double dblplazo = invoice
												.getPaymentTerms()
												.getOverduePaymentDaysRule();
										plazo.appendChild(doc
												.createTextNode(String
														.valueOf(dblplazo)));
										pago.appendChild(plazo);

										Element unidadTiempo = doc
												.createElement("unidadTiempo");
										strUnitTime = "dias";
										unidadTiempo.appendChild(doc
												.createTextNode(strUnitTime));
										pago.appendChild(unidadTiempo);

									} else if (invoice.getPaymentTerms()
											.getOffsetMonthDue() > 0) {

										double dblplazo = invoice
												.getPaymentTerms()
												.getOffsetMonthDue();
										plazo.appendChild(doc
												.createTextNode(String
														.valueOf(dblplazo)));
										pago.appendChild(plazo);

										Element unidadTiempo = doc
												.createElement("unidadTiempo");

										strUnitTime = "meses";
										unidadTiempo.appendChild(doc
												.createTextNode(strUnitTime));
										pago.appendChild(unidadTiempo);
									}
								}
								pagos.appendChild(pago);

								dbdTotalPaymentOut = dbdTotalPaymentOut
										+ psd.getAmount().doubleValue();

							}
						} catch (Exception e) {
						}
					}

				}

				bgdGrandTotalPayment = new BigDecimal(dbdTotalPaymentOut);
				bgdGrandTotalPayment = bgdGrandTotalPayment.setScale(2,
						RoundingMode.HALF_UP);
			}

			BigDecimal bgbGranTotalInvoice = invoice.getGrandTotalAmount()
					.setScale(2, RoundingMode.HALF_UP);

			if (!bgbGranTotalInvoice.equals(bgdGrandTotalPayment)) {

				if (!strPaymentMethod
						.equals(invoice.getPaymentMethod().getId())) {
					pagos = doc.createElement("pagos");

					pago = doc.createElement("pago");

					Element formaPago = doc.createElement("formaPago");
					String strCodePaymentMethod = invoice.getPaymentMethod()
							.getEeiCodeEi() == null ? "ND" : invoice
							.getPaymentMethod().getEeiCodeEi();
					if (strCodePaymentMethod.equals("ND")) {
						throw new OBException(
								"El método de Pago "
										+ invoice.getPaymentMethod().getName()
										+ " no esta configurado para el proceso de Facturación Electrónica");

					}
					formaPago.appendChild(doc
							.createTextNode(strCodePaymentMethod));
					pago.appendChild(formaPago);

					Element total = doc.createElement("total");
					double dblpagos = invoice.getGrandTotalAmount()
							.doubleValue();
					total.appendChild(doc.createTextNode(formateador.format(
							dblpagos).toString()));
					pago.appendChild(total);

					Element plazo = doc.createElement("plazo");

					String strUnitTime = "";
					if (invoice.getPaymentTerms().getOverduePaymentDaysRule() > 0) {

						double dblplazo = invoice.getPaymentTerms()
								.getOverduePaymentDaysRule();
						plazo.appendChild(doc.createTextNode(String
								.valueOf(dblplazo)));
						pago.appendChild(plazo);

						Element unidadTiempo = doc
								.createElement("unidadTiempo");
						strUnitTime = "dias";
						unidadTiempo.appendChild(doc
								.createTextNode(strUnitTime));
						pago.appendChild(unidadTiempo);
					} else if (invoice.getPaymentTerms().getOffsetMonthDue() > 0) {

						double dblplazo = invoice.getPaymentTerms()
								.getOffsetMonthDue();
						plazo.appendChild(doc.createTextNode(String
								.valueOf(dblplazo)));
						pago.appendChild(plazo);

						Element unidadTiempo = doc
								.createElement("unidadTiempo");

						strUnitTime = "meses";
						unidadTiempo.appendChild(doc
								.createTextNode(strUnitTime));
						pago.appendChild(unidadTiempo);
					}

					pagos.appendChild(pago);

				} else {

					pago2 = (doc.createElement("pago"));

					Element formaPago = doc.createElement("formaPago");

					String strCodePaymentMethod = invoice.getPaymentMethod()
							.getEeiCodeEi() == null ? "ND" : invoice
							.getPaymentMethod().getEeiCodeEi();
					if (strCodePaymentMethod.equals("ND")) {
						throw new OBException(
								"El método de Pago "
										+ invoice.getPaymentMethod().getName()
										+ " no esta configurado para el proceso de Facturación Electrónica");

					}
					formaPago.appendChild(doc
							.createTextNode(strCodePaymentMethod));
					pago2.appendChild(formaPago);

					Element total = doc.createElement("total");
					double dblpagos = invoice.getGrandTotalAmount()
							.doubleValue();
					total.appendChild(doc.createTextNode(formateador.format(
							dblpagos).toString()));
					pago2.appendChild(total);

					Element plazo = doc.createElement("plazo");

					String strUnitTime = "";

					if (invoice.getPaymentTerms().getOverduePaymentDaysRule() > 0) {

						double dblplazo = invoice.getPaymentTerms()
								.getOverduePaymentDaysRule();
						plazo.appendChild(doc.createTextNode(String
								.valueOf(dblplazo)));
						pago2.appendChild(plazo);

						Element unidadTiempo = doc
								.createElement("unidadTiempo");
						strUnitTime = "dias";
						unidadTiempo.appendChild(doc
								.createTextNode(strUnitTime));
						pago2.appendChild(unidadTiempo);
					} else if (invoice.getPaymentTerms().getOffsetMonthDue() > 0) {

						double dblplazo = invoice.getPaymentTerms()
								.getOffsetMonthDue();
						plazo.appendChild(doc.createTextNode(String
								.valueOf(dblplazo)));
						pago2.appendChild(plazo);

						Element unidadTiempo = doc
								.createElement("unidadTiempo");

						strUnitTime = "meses";
						unidadTiempo.appendChild(doc
								.createTextNode(strUnitTime));
						pago2.appendChild(unidadTiempo);
					}

					pagos2.appendChild(pago2);
				}

			}

			if (!bgbGranTotalInvoice.equals(bgdGrandTotalPayment)) {

				if (!strPaymentMethod
						.equals(invoice.getPaymentMethod().getId())) {
					infoLiquidacionCompra.appendChild(pagos);
				} else {
					infoLiquidacionCompra.appendChild(pagos2);
				}
			} else if (bgbGranTotalInvoice.equals(bgdGrandTotalPayment)) {
				infoLiquidacionCompra.appendChild(pagos);

			}

		} else {

			Element pagos = doc.createElement("pagos");

			Element pago = doc.createElement("pago");

			Element formaPago = doc.createElement("formaPago");
			String strCodePaymentMethod = invoice.getPaymentMethod()
					.getEeiCodeEi() == null ? "ND" : invoice.getPaymentMethod()
					.getEeiCodeEi();
			if (strCodePaymentMethod.equals("ND")) {
				throw new OBException(
						"El método de Pago "
								+ invoice.getPaymentMethod().getName()
								+ " no esta configurado para el proceso de Facturación Electrónica");

			}
			formaPago.appendChild(doc.createTextNode(strCodePaymentMethod));
			pago.appendChild(formaPago);

			Element total = doc.createElement("total");
			double dblpagos = invoice.getGrandTotalAmount().doubleValue();
			total.appendChild(doc.createTextNode(formateador.format(dblpagos)
					.toString()));
			pago.appendChild(total);

			Element plazo = doc.createElement("plazo");

			String strUnitTime = "";
			if (invoice.getPaymentTerms().getOverduePaymentDaysRule() > 0) {

				double dblplazo = invoice.getPaymentTerms()
						.getOverduePaymentDaysRule();
				plazo.appendChild(doc.createTextNode(String.valueOf(dblplazo)));
				pago.appendChild(plazo);

				Element unidadTiempo = doc.createElement("unidadTiempo");
				strUnitTime = "dias";
				unidadTiempo.appendChild(doc.createTextNode(strUnitTime));
				pago.appendChild(unidadTiempo);
			} else if (invoice.getPaymentTerms().getOffsetMonthDue() > 0) {

				double dblplazo = invoice.getPaymentTerms().getOffsetMonthDue();
				plazo.appendChild(doc.createTextNode(String.valueOf(dblplazo)));
				pago.appendChild(plazo);

				Element unidadTiempo = doc.createElement("unidadTiempo");

				strUnitTime = "meses";
				unidadTiempo.appendChild(doc.createTextNode(strUnitTime));
				pago.appendChild(unidadTiempo);

			}

			pagos.appendChild(pago);

			infoLiquidacionCompra.appendChild(pagos);

		}

		// *************************TERMINA PAGOS

		// *************************INICIA DETALLES
		Element detalles = doc.createElement("detalles");
		rootElement.appendChild(detalles);

		Element detalle;
		Element codigoPrincipal;
		Element codigoAuxiliar;
		Element descripcion;
		Element cantidad;
		Element precioUnitario;
		Element descuento;
		Element precioTotalSinImpuesto;
		Element detallesAdicionales;
		Element detAdicional;
		Element impuestos;
		Element impuesto;
		Element tarifaImpuesto;
		Element valorImpuesto;
		Element baseImponibleImpuesto;

		// CHEQUEAR LÍNEAS NEGATIVAS
		List<InvoiceLine> negativesLines = hasInvoiceLinesWithNegativeAmounts(
				invoice.getInvoiceLineList(), new BigDecimal(TotalSinImpuestos));

		List<InvoiceLine> lines = invoice.getInvoiceLineList();

		if (negativesLines != null)
			lines = negativesLines;

		// ORDENAR POR NÚMERO DE LÍNEA
		Collections.sort(lines, new Comparator<InvoiceLine>() {
			@Override
			public int compare(InvoiceLine o1, InvoiceLine o2) {
				return o1.getLineNo().compareTo(o2.getLineNo());
			}
		});

		OBCriteria<EEIParamFacturae> ObjEeiParam = OBDal.getInstance()
				.createCriteria(EEIParamFacturae.class);
		ObjEeiParam.add(Restrictions.eq(EEIParamFacturae.PROPERTY_ACTIVE,
				true));

		EEIParamFacturae ObjParams = null;
		ObjParams = OBDal.getInstance().get(EEIParamFacturae.class,
				ObjEeiParam.list().get(0).getId());
		
		for (InvoiceLine detalleObj : lines) {
			detalle = doc.createElement("detalle");
			detalles.appendChild(detalle);


			// CÓDIGO PRINCIPAL
			if (ObjParams.isShowprincipalcode()) {
				codigoPrincipal = doc.createElement("codigoPrincipal");
				String strCodigoPrincipal = null;
				strCodigoPrincipal = truncate(detalleObj.getProduct()
						.getSearchKey(), 25);
				if (strCodigoPrincipal == null
						|| strCodigoPrincipal.trim().equals("")) {
					strCodigoPrincipal = "-";
				}

				codigoPrincipal.appendChild(doc
						.createTextNode(strCodigoPrincipal));
				detalle.appendChild(codigoPrincipal);
			}

			// CÓDIGO AUXILIAR
			if (ObjParams.isShowauxiliarycode()) {
				codigoAuxiliar = doc.createElement("codigoAuxiliar");
				String strCodigoAuxiliar = null;
				strCodigoAuxiliar = truncate(detalleObj.getProduct()
						.getEeiAlternativeidentifier(), 25);

				if (strCodigoAuxiliar == null
						|| strCodigoAuxiliar.trim().equals("")) {
					strCodigoAuxiliar = truncate(detalleObj.getProduct()
							.getSearchKey(), 25);
				}
				if (strCodigoAuxiliar == null
						|| strCodigoAuxiliar.trim().equals("")) {
					strCodigoAuxiliar = "-";
				}
				codigoAuxiliar.appendChild(doc
						.createTextNode(strCodigoAuxiliar));
				detalle.appendChild(codigoAuxiliar);
			}

			// DESCRIPCIÓN
		    String strDescription = "";
		    if (ObjParams.getProductName().equals("N")) {
		    	strDescription = detalleObj.getProduct().getName();
		    }else if (ObjParams.getProductName().equals("D")){
		    	strDescription = detalleObj.getProduct().getDescription();
		    }else if (ObjParams.getProductName().equals("ND")){
		    	strDescription = detalleObj.getProduct().getName()+
		    			  (detalleObj.getProduct().getDescription()==null?"":" - "+ detalleObj.getProduct().getDescription());
		    }
		      
		    strDescription = truncate(strDescription, 300);
		    if (strDescription == null || strDescription.trim().equals("")) {
		      throw new OBException(
		          "El producto (" + detalleObj.getProduct().getName() + ") no tiene descripción o nombre.");
		    }
			strDescription = strDescription.replaceAll("[\n]", " ");
			descripcion = doc.createElement("descripcion");
			descripcion.appendChild(doc.createTextNode(strDescription));
			detalle.appendChild(descripcion);

			// CANTIDAD
			double Cantidad = detalleObj.getInvoicedQuantity().doubleValue();
			cantidad = doc.createElement("cantidad");
			cantidad.appendChild(doc.createTextNode(formateador
					.format(Cantidad).toString()));
			detalle.appendChild(cantidad);

			double PrecioUnitario = detalleObj.getUnitPrice().doubleValue();
			double PrecioTarifa = detalleObj.getListPrice().doubleValue();
			double PrecioTotalSinImpuesto = detalleObj.getLineNetAmount()
					.doubleValue();
			double DescuentoLinea = 0;
			if (PrecioTarifa > 0 & PrecioTarifa > PrecioUnitario) {
				PrecioUnitario = PrecioTarifa;
				DescuentoLinea = Double.parseDouble(formateador.format(
						Cantidad * PrecioUnitario).toString())
						- PrecioTotalSinImpuesto;
			}

			precioUnitario = doc.createElement("precioUnitario");
			precioUnitario.appendChild(doc.createTextNode(formateador.format(
					PrecioUnitario).toString()));
			detalle.appendChild(precioUnitario);

			descuento = doc.createElement("descuento");
			descuento.appendChild(doc.createTextNode(formateador.format(
					DescuentoLinea).toString()));
			detalle.appendChild(descuento);

			precioTotalSinImpuesto = doc
					.createElement("precioTotalSinImpuesto");
			precioTotalSinImpuesto.appendChild(doc.createTextNode(formateador
					.format(PrecioTotalSinImpuesto).toString()));
			detalle.appendChild(precioTotalSinImpuesto);

			if (detalleObj.getDescription() != null
					&& !detalleObj.getDescription().trim().equals("")) {
				detallesAdicionales = doc.createElement("detallesAdicionales");
				detalle.appendChild(detallesAdicionales);

				detAdicional = doc.createElement("detAdicional");

				detAdicional.setAttribute("nombre", "detadicional");

				String strAttribute = truncate(detalleObj.getDescription(), 300);
				if (strAttribute == null || strAttribute.trim().equals("")) {
					throw new OBException(
							"El producto ("
									+ detalleObj.getProduct().getName()
									+ ") no tiene un detalle adicional (Descripción en línea).");
				}
				strAttribute = strAttribute.replaceAll("[\n]", " ");
				detAdicional.setAttribute("valor", strAttribute);
				detallesAdicionales.appendChild(detAdicional);
			}

			impuestos = doc.createElement("impuestos");
			detalle.appendChild(impuestos);

			for (InvoiceLineTax detalleImpuestoObj : detalleObj
					.getInvoiceLineTaxList()) {
				impuesto = doc.createElement("impuesto");
				impuestos.appendChild(impuesto);

				// CÓDIGO DE IMPUESTO
				Element codigo = doc.createElement("codigo");
				codigo.appendChild(doc.createTextNode(detalleImpuestoObj
						.getTax().getEeiSriTaxType()));
				impuesto.appendChild(codigo);

				// CÓDIGO PORCENTAJE
				String strTaxCode = detalleImpuestoObj.getTax()
						.getEeiSriTaxcatCode();
				if (strTaxCode == null) {
					throw new OBException(
							"Código de impuesto SRI no configurado. ");
				}
				Element codigoPorcentaje = doc
						.createElement("codigoPorcentaje");
				codigoPorcentaje.appendChild(doc.createTextNode(strTaxCode));
				impuesto.appendChild(codigoPorcentaje);

				// TARIFA
				double TarifaImpuesto = detalleImpuestoObj.getTax().getRate()
						.doubleValue();
				tarifaImpuesto = doc.createElement("tarifa");
				tarifaImpuesto.appendChild(doc.createTextNode(formateador
						.format(TarifaImpuesto).toString()));
				impuesto.appendChild(tarifaImpuesto);

				double BaseImponibleImpuesto = detalleImpuestoObj
						.getTaxableAmount().doubleValue();
				baseImponibleImpuesto = doc.createElement("baseImponible");
				baseImponibleImpuesto.appendChild(doc
						.createTextNode(formateador.format(
								BaseImponibleImpuesto).toString()));
				impuesto.appendChild(baseImponibleImpuesto);

				double ValorImpuesto = detalleImpuestoObj.getTaxAmount()
						.doubleValue();
				valorImpuesto = doc.createElement("valor");
				valorImpuesto.appendChild(doc.createTextNode(formateador
						.format(ValorImpuesto).toString()));
				impuesto.appendChild(valorImpuesto);
			}
		}

		// *****************************FIN DETALLES

		// *****************************INICIO REEMBOLSOS DETALLE

		if (invoice.getSswhLivelihood().isRefund()) {
			try {

				List<ssrerefundinvoice> lstRefundInvoices = invoice
						.getSsreRefundinvoiceList();

				if (lstRefundInvoices.size() == 0) {
					throw new OBException(
							"No existen registros en la solapa de Reembolsos. ");
				}
				
                                Element reembolsos = doc.createElement("reembolsos");
                                rootElement.appendChild(reembolsos);
                                
				for (ssrerefundinvoice objRefundInvoice : lstRefundInvoices) {

					Element reembolsoDetalle = doc
							.createElement("reembolsoDetalle");
					reembolsos.appendChild(reembolsoDetalle);

					// TIPO IDENTIFICACIÓN
					String strRefundTypeTaxID = null;
					if (objRefundInvoice.getTaxidtype().equals("R")) {
						strRefundTypeTaxID = "04";
					} else if (objRefundInvoice.getTaxidtype().equals("D")) {
						strRefundTypeTaxID = "05";
					} else if (objRefundInvoice.getTaxidtype().equals("P")) {
						strRefundTypeTaxID = "06";
				    } else if (objRefundInvoice.getTaxidtype().equals("C")) {
				    	strRefundTypeTaxID = "07";
				    } else if (objRefundInvoice.getTaxidtype().equals("E")) {    	
				    	strRefundTypeTaxID = "08";
				    }
					Element tipoIdentificacionProveedorReembolso = doc
							.createElement("tipoIdentificacionProveedorReembolso");
					tipoIdentificacionProveedorReembolso.appendChild(doc
							.createTextNode(strRefundTypeTaxID));
					reembolsoDetalle
							.appendChild(tipoIdentificacionProveedorReembolso);

					// IDENTIFICACIÓN
					String strRefundTaxid = objRefundInvoice.getTaxidruc();
					if (strRefundTaxid == null
							|| strRefundTaxid.trim().equals("")) {
						throw new OBException(
								"El cliente no tiene identificación (RUC/Cédula - Reembolsos).");
					}

					Element identificacionProveedorReembolso = doc
							.createElement("identificacionProveedorReembolso");
					identificacionProveedorReembolso.appendChild(doc
							.createTextNode(strRefundTaxid));
					reembolsoDetalle
							.appendChild(identificacionProveedorReembolso);

					// CÓDIGO PAÍS
					OBCriteria<EEIParamFacturae> objFEParams = OBDal
							.getInstance().createCriteria(
									EEIParamFacturae.class);
					objFEParams.add(Restrictions.eq(
							EEIParamFacturae.PROPERTY_ACTIVE, true));
					EEIParamFacturae objParam = (EEIParamFacturae) objFEParams
							.uniqueResult();
					if (objParam == null) {
						throw new OBException(
								"No se encontró parametrización de envío de documentos electrónicos.");
					}
					String strCodigoPais = objParam.getCountryCode();
					if (strCodigoPais.length() != 3) {
						throw new OBException(
								"El código de país debe ser númerico de 3 dígitos (Parámetros Facturación Electrónica).");
					}
					Element codPaisPagoProveedorReembolso = doc
							.createElement("codPaisPagoProveedorReembolso");
					codPaisPagoProveedorReembolso.appendChild(doc
							.createTextNode(strCodigoPais));
					reembolsoDetalle.appendChild(codPaisPagoProveedorReembolso);

					// TIPO DE PROVEEDOR REEMBOLSO
					if(objRefundInvoice
							.getBpartner().getSSWHTaxpayer() ==null){
						throw new OBException(
								"Tipo de contribuyente no configurado en tercero (Detalle Reembolso).");					
					}					
					String strTipoProveedorReembolso = objRefundInvoice
							.getBpartner().getSSWHTaxpayer().getSearchKey();

					if (strTipoProveedorReembolso == null
							|| strTipoProveedorReembolso.equals("")) {
						throw new OBException(
								"Identificador de tipo de contribuyente del tercero de la factura de compra referenciada (reembolso) no configurado.");
					}
					Element tipoProveedorReembolso = doc
							.createElement("tipoProveedorReembolso");
					tipoProveedorReembolso.appendChild(doc
							.createTextNode(strTipoProveedorReembolso));
					reembolsoDetalle.appendChild(tipoProveedorReembolso);

					// CÓDIGO DOCUMENTO REEMBOLSO
					String strCodDocReembolso = objRefundInvoice
							.getLivelihood().getSearchKey();
					if (strCodDocReembolso == null
							|| strCodDocReembolso.equals("")) {
						throw new OBException(
								"Identificador del tipo de comprobante en datos de retención de la factura de compra referenciada (reembolso) no configurado.");
					}
					Element codDocReembolso = doc
							.createElement("codDocReembolso");
					codDocReembolso.appendChild(doc
							.createTextNode(strCodDocReembolso));
					reembolsoDetalle.appendChild(codDocReembolso);

					// NÚMERO DE FACTURA DE COMPRA
					String strDocRefund = objRefundInvoice.getOrderReference();

					if (strDocRefund == null || strDocRefund.equals("")) {
						throw new OBException(
								"La factura de compra (reembolsos) no tiene asociada el Número de Retención");
					}

					String strNumEstablecimiento = objRefundInvoice
							.getStablishment();
					String strNumPuntoEmision = objRefundInvoice.getShell();
					String strNumSerie = objRefundInvoice.getOrderReference();
					String strNumDocumento = strNumEstablecimiento + "-"
							+ strNumPuntoEmision + "-" + strNumSerie;

					if (!strNumDocumento.matches("^\\d{3}-\\d{3}-\\d{9}$")) {
						throw new OBException(
								"El formato del número de documento de la factura de compra asociada (reembolso) es incorrecto (000-000-000000000).");
					}

					Element estabDocReembolso = doc
							.createElement("estabDocReembolso");
					estabDocReembolso.appendChild(doc
							.createTextNode(strNumEstablecimiento));
					reembolsoDetalle.appendChild(estabDocReembolso);

					Element ptoEmiDocReembolso = doc
							.createElement("ptoEmiDocReembolso");
					ptoEmiDocReembolso.appendChild(doc
							.createTextNode(strNumPuntoEmision));
					reembolsoDetalle.appendChild(ptoEmiDocReembolso);

					Element secuencialDocReembolso = doc
							.createElement("secuencialDocReembolso");
					secuencialDocReembolso.appendChild(doc
							.createTextNode(strNumSerie));
					reembolsoDetalle.appendChild(secuencialDocReembolso);

					Element fechaEmisionDocReembolso = doc
							.createElement("fechaEmisionDocReembolso");
					fechaEmisionDocReembolso.appendChild(doc
							.createTextNode(ecFormat.format(objRefundInvoice
									.getDateemission())));
					reembolsoDetalle.appendChild(fechaEmisionDocReembolso);

					String strRefAuthNumber = objRefundInvoice
							.getWithholdingAuthorization();
					if (strRefAuthNumber == null || strRefAuthNumber.equals("")) {
						throw new OBException(
								"El número de autorización de la factura de compra referenciada (reembolso) es nulo. ");
					}

					Element numeroautorizacionDocReemb = doc
							.createElement("numeroautorizacionDocReemb");
					numeroautorizacionDocReemb.appendChild(doc
							.createTextNode(strRefAuthNumber));
					reembolsoDetalle.appendChild(numeroautorizacionDocReemb);

					Element detalleImpuestos = doc
							.createElement("detalleImpuestos");
					reembolsoDetalle.appendChild(detalleImpuestos);

					Element detalleImpuesto = doc
							.createElement("detalleImpuesto");
					detalleImpuestos.appendChild(detalleImpuesto);

					// CÓDIGO DE IMPUESTO
					TaxRate objTaxRate = OBDal.getInstance().get(TaxRate.class,
							getTaxIdRefund(invoice.getId()));
					Element codigo = doc.createElement("codigo");
					codigo.appendChild(doc.createTextNode(objTaxRate
							.getEeiSriTaxType()));
					detalleImpuesto.appendChild(codigo);

					// CÓDIGO PORCENTAJE
					String strTaxCode = objTaxRate.getEeiSriTaxcatCode();
					if (strTaxCode == null) {
						throw new OBException(
								"Código de impuesto SRI no configurado (Reembolsos). ");
					}
					Element codigoPorcentaje = doc
							.createElement("codigoPorcentaje");
					codigoPorcentaje
							.appendChild(doc.createTextNode(strTaxCode));
					detalleImpuesto.appendChild(codigoPorcentaje);

					// TARIFA
					double Tarifa = objTaxRate.getRate().doubleValue();
					int intRate = (int) Tarifa;
					Element tarifa = doc.createElement("tarifa");
					tarifa.appendChild(doc.createTextNode(String
							.valueOf(intRate)));
					detalleImpuesto.appendChild(tarifa);

					// BASE IMPONIBLE REEMBOLSO
					double sbBaseImponible = (objRefundInvoice
							.getTaxbaserefund() == null ? BigDecimal.ZERO
							: objRefundInvoice
									.getTaxbaserefund())
							.doubleValue();
					Element baseImponibleReembolso = doc
							.createElement("baseImponibleReembolso");
					baseImponibleReembolso.appendChild(doc
							.createTextNode(formateador.format(sbBaseImponible)
									.toString()));
					detalleImpuesto.appendChild(baseImponibleReembolso);

					// IMPUESTO REEMBOLSO
					double dbimpuestoReembolso = (objRefundInvoice
							.getTaxAmount() == null ? BigDecimal.ZERO
							: objRefundInvoice.getTaxAmount()).doubleValue();
					Element impuestoReembolso = doc
							.createElement("impuestoReembolso");
					impuestoReembolso.appendChild(doc
							.createTextNode(formateador.format(
									dbimpuestoReembolso).toString()));
					detalleImpuesto.appendChild(impuestoReembolso);

					// IMPUESTOS EN CERO

					// BASE NO GRABA IVA
					double bdBaseNoGrabaIva = (objRefundInvoice.getTaxbase() == null ? BigDecimal.ZERO
							: objRefundInvoice.getTaxbase()).doubleValue();
					if (bdBaseNoGrabaIva != 0) {

						detalleImpuesto = doc.createElement("detalleImpuesto");
						detalleImpuestos.appendChild(detalleImpuesto);

						// CÓDIGO DE IMPUESTO
						codigo = doc.createElement("codigo");
						codigo.appendChild(doc.createTextNode("2"));
						detalleImpuesto.appendChild(codigo);

						// CÓDIGO PORCENTAJE
						codigoPorcentaje = doc
								.createElement("codigoPorcentaje");
						codigoPorcentaje.appendChild(doc.createTextNode("6"));
						detalleImpuesto.appendChild(codigoPorcentaje);

						// TARIFA
						tarifa = doc.createElement("tarifa");
						tarifa.appendChild(doc.createTextNode("0"));
						detalleImpuesto.appendChild(tarifa);

						// BASE IMPONIBLE
						baseImponibleReembolso = doc
								.createElement("baseImponibleReembolso");
						baseImponibleReembolso.appendChild(doc
								.createTextNode(formateador.format(
										bdBaseNoGrabaIva).toString()));
						detalleImpuesto.appendChild(baseImponibleReembolso);

						// IMPUESTO REEMBOLSO
						impuestoReembolso = doc
								.createElement("impuestoReembolso");
						impuestoReembolso.appendChild(doc
								.createTextNode(formateador.format(0)
										.toString()));
						detalleImpuesto.appendChild(impuestoReembolso);
					}

					// BASE IVA 0%
					double dbBaseIvaCero = (objRefundInvoice.getUntaxedBasis() == null ? BigDecimal.ZERO
							: objRefundInvoice.getUntaxedBasis()).doubleValue();
					if (dbBaseIvaCero != 0) {

						detalleImpuesto = doc.createElement("detalleImpuesto");
						detalleImpuestos.appendChild(detalleImpuesto);

						// CÓDIGO DE IMPUESTO
						codigo = doc.createElement("codigo");
						codigo.appendChild(doc.createTextNode("2"));
						detalleImpuesto.appendChild(codigo);

						// CÓDIGO PORCENTAJE
						codigoPorcentaje = doc
								.createElement("codigoPorcentaje");
						codigoPorcentaje.appendChild(doc.createTextNode("0"));
						detalleImpuesto.appendChild(codigoPorcentaje);

						// TARIFA
						tarifa = doc.createElement("tarifa");
						tarifa.appendChild(doc.createTextNode("0"));
						detalleImpuesto.appendChild(tarifa);

						// BASE IMPONIBLE
						baseImponibleReembolso = doc
								.createElement("baseImponibleReembolso");
						baseImponibleReembolso.appendChild(doc
								.createTextNode(formateador.format(
										dbBaseIvaCero).toString()));
						detalleImpuesto.appendChild(baseImponibleReembolso);

						// IMPUESTO REEMBOLSO
						impuestoReembolso = doc
								.createElement("impuestoReembolso");
						impuestoReembolso.appendChild(doc
								.createTextNode(formateador.format(0)
										.toString()));
						detalleImpuesto.appendChild(impuestoReembolso);

					}

					// BASE EXCENTA IVA
					double dbBaseExcemto = (objRefundInvoice.getExemptBase() == null ? BigDecimal.ZERO
							: objRefundInvoice.getExemptBase()).doubleValue();
					if (dbBaseExcemto != 0) {

						detalleImpuesto = doc.createElement("detalleImpuesto");
						detalleImpuestos.appendChild(detalleImpuesto);

						// CÓDIGO DE IMPUESTO
						codigo = doc.createElement("codigo");
						codigo.appendChild(doc.createTextNode("2"));
						detalleImpuesto.appendChild(codigo);

						// CÓDIGO PORCENTAJE
						codigoPorcentaje = doc
								.createElement("codigoPorcentaje");
						codigoPorcentaje.appendChild(doc.createTextNode("7"));
						detalleImpuesto.appendChild(codigoPorcentaje);

						// TARIFA
						tarifa = doc.createElement("tarifa");
						tarifa.appendChild(doc.createTextNode("0"));
						detalleImpuesto.appendChild(tarifa);

						// BASE IMPONIBLE
						baseImponibleReembolso = doc
								.createElement("baseImponibleReembolso");
						baseImponibleReembolso.appendChild(doc
								.createTextNode(formateador.format(
										dbBaseIvaCero).toString()));
						detalleImpuesto.appendChild(baseImponibleReembolso);

						// IMPUESTO REEMBOLSO
						impuestoReembolso = doc
								.createElement("impuestoReembolso");
						impuestoReembolso.appendChild(doc
								.createTextNode(formateador.format(0)
										.toString()));
						detalleImpuesto.appendChild(impuestoReembolso);

					}

				}

			} catch (Exception e) {

				throw new OBException(
						"Error al obtener información de reembolso (Detalle). "
								+ e.getMessage());
			}
		}

		// *****************************FIN REEMBOLSOS DETALLE
	    String dataInvoice[] = new String[6];
	    dataInvoice = ClientSOAP.getDataInv(invoice.getId(), invoice, null);		

		// INFOADICIONAL
	    
		if ((((invoice.getDescription() != null && !invoice.getDescription()
				.trim().equals("")) || (invoice.getEeiDescription() != null && !invoice
				.getEeiDescription().trim().equals(""))) && !invoice
				.getDocumentType().getEeiDescriptionfields().equals("NO"))
				|| (invoice.getBusinessPartner().getName2() != null)
		        || (ObjParams.getAdittionalInfo() != null && !ObjParams.getAdittionalInfo().trim().equals(""))
		        || (dataInvoice[0] != null || dataInvoice[1] != null || dataInvoice[2] != null)) {

			Element infoAdicional = doc.createElement("infoAdicional");
			rootElement.appendChild(infoAdicional);
		    String StrUnionCadenaSinSaltos = "";

		      if (dataInvoice[0] != null) {
		          Element campoAdicionaldir = doc.createElement("campoAdicional");
		          campoAdicionaldir.setAttribute("nombre", "Dirección");
		          campoAdicionaldir.appendChild(doc.createTextNode(dataInvoice[0]));
		          infoAdicional.appendChild(campoAdicionaldir);
		      }      
		      if (dataInvoice[1] != null) {
		          Element campoAdicionaltel = doc.createElement("campoAdicional");
		          campoAdicionaltel.setAttribute("nombre", "Teléfono");
		          campoAdicionaltel.appendChild(doc.createTextNode(dataInvoice[1]));
		          infoAdicional.appendChild(campoAdicionaltel);
		      }
		      if (dataInvoice[2] != null) {
		          Element campoAdicionalem = doc.createElement("campoAdicional");
		          campoAdicionalem.setAttribute("nombre", "E-mail");
		          campoAdicionalem.appendChild(doc.createTextNode(dataInvoice[2]));
		          infoAdicional.appendChild(campoAdicionalem);
		      } 		    

		    if(ObjParams.getAdittionalInfo() != null && !ObjParams.getAdittionalInfo().trim().equals("")) {
		        Element campoAdicional = doc.createElement("campoAdicional");
		        campoAdicional.setAttribute("nombre", "Descripción");
		        campoAdicional.appendChild(doc.createTextNode(truncate(ObjParams.getAdittionalInfo(),300)));
		        infoAdicional.appendChild(campoAdicional);
		        StrUnionCadenaSinSaltos = ";"; 		        
		    }			

      if (invoice.getBusinessPartner().getName2() != null
          || invoice.getBusinessPartner().getDescription() != null) {

        String strDescription3 = invoice.getBusinessPartner().getDescription() == null ? "ND"
            : invoice.getBusinessPartner().getDescription();
        String strNam2 = invoice.getBusinessPartner().getName2() == null ? "ND"
            : invoice.getBusinessPartner().getName2();

        String strName3 = strExtract && !strDescription3.equals("ND") ? strDescription3 : strNam2;

        if (!strName3.equals("ND")) {
          Element campoAdicional3 = doc.createElement("campoAdicional");
          campoAdicional3.setAttribute("nombre", "NombreComercialTercero");
          campoAdicional3.appendChild(doc.createTextNode(truncate(strName3, 300)));
          infoAdicional.appendChild(campoAdicional3);
        }
      }

			if (((invoice.getDescription() != null && !invoice.getDescription()
					.trim().equals("")) || (invoice.getEeiDescription() != null && !invoice
					.getEeiDescription().trim().equals("")))
					&& !invoice.getDocumentType().getEeiDescriptionfields()
							.equals("NO")) {

				if (invoice.getDocumentType().getEeiDescriptionfields()
						.equals("DEDA")) {

					StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos+(invoice.getDescription() == null ? ""
							: invoice.getDescription())
							+ ";"
							+ (invoice.getEeiDescription() == null ? ""
									: invoice.getEeiDescription());

				} else if (invoice.getDocumentType().getEeiDescriptionfields()
						.equals("DE")) {

					StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos+(invoice.getDescription() == null ? ""
							: invoice.getDescription());

				} else if (invoice.getDocumentType().getEeiDescriptionfields()
						.equals("DA")) {

					StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos+(invoice.getEeiDescription() == null ? ""
							: invoice.getEeiDescription());

				}

				StrUnionCadenaSinSaltos = String.valueOf(
						StrUnionCadenaSinSaltos).replaceAll("[\n]", ";");

				StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos.replaceAll(
						";;;", ";");
				StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos.replaceAll(
						";;", ";");

		        if(StrUnionCadenaSinSaltos.equals(";")) {
		        	StrUnionCadenaSinSaltos="";
		        }
				
				String strCadenaParcial = "";
				String strCadenaConcatenada = "";
				int intContador = 0;
				int j = 0;
				for (int i = 0; i < StrUnionCadenaSinSaltos.length(); i = i + 300) {
					j = i + 300;
					if (j > StrUnionCadenaSinSaltos.length()) {
						break;
					}
					strCadenaParcial = StrUnionCadenaSinSaltos.substring(i, j);
					strCadenaConcatenada = strCadenaConcatenada
							+ strCadenaParcial;
					intContador = intContador + 1;
					Element campoAdicional2 = doc
							.createElement("campoAdicional");
					campoAdicional2.setAttribute("nombre", "Descripción"
							+ intContador);
					campoAdicional2.appendChild(doc.createTextNode(truncate(
							strCadenaParcial, 300)));
					infoAdicional.appendChild(campoAdicional2);
				}
				if (strCadenaConcatenada.length() < StrUnionCadenaSinSaltos
						.length()) {
					intContador = intContador + 1;
					strCadenaParcial = StrUnionCadenaSinSaltos.substring(
							strCadenaConcatenada.length(),
							StrUnionCadenaSinSaltos.length());
					Element campoAdicional2 = doc
							.createElement("campoAdicional");
					campoAdicional2.setAttribute("nombre", "Descripción"
							+ intContador);
					campoAdicional2.appendChild(doc.createTextNode(truncate(
							strCadenaParcial, 300)));
					infoAdicional.appendChild(campoAdicional2);
				}

			}
		}

		DOMSource domSource = new DOMSource(doc);
		StringWriter writer = new StringWriter();
		StreamResult result = new StreamResult(writer);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.transform(domSource, result);

		return writer.toString();
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

	public static String getTaxIdRefund(String v_invoice_id)
			throws ServletException {

		ConnectionProvider con = new DalConnectionProvider(false);
		String strSql = "";
		strSql = strSql
				+ "   select coalesce( "
				+ " (select e.c_tax_id from c_tax e where  e.rate <> 0  and e.istaxdeductable ='Y' and e.isactive='Y'"
				+ " and validfrom = (select max(validfrom) from c_tax e where e.rate <> 0  and e.isactive='Y' and e.istaxdeductable ='Y' and validfrom <= dateinvoiced"
				+ " )),to_char(0))"
				+ " as tax_res from c_invoice where c_invoice_id = '"
				+ v_invoice_id + "'";

		ResultSet result;
		String strReturn = null;
		PreparedStatement st = null;

		try {
			st = con.getPreparedStatement(strSql);

			result = st.executeQuery();
			if (result.next()) {
				strReturn = UtilSql.getValue(result, "tax_res");
			}
			result.close();
			st.close();
		} catch (SQLException e) {
			throw new ServletException("@CODE="
					+ Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
		} catch (Exception ex) {
			throw new ServletException("@CODE=@" + ex.getMessage());
		} finally {
			try {
				con.releasePreparedStatement(st);
			} catch (Exception ignore) {
				ignore.printStackTrace();
			}
		}
		return (strReturn);
	}

	public List<InvoiceLine> hasInvoiceLinesWithNegativeAmounts(
			List<InvoiceLine> lines, BigDecimal netSubTotal) {
		List<InvoiceLine> line = null;

		boolean hasNegatives = false;

		double count = 0;
		double unitPrice = 0;
		double netPriceWithoutTax = 0;
		double taxAmount = 0;
		double taxableAmount = 0;

		InvoiceLine first = lines.get(0);

		for (InvoiceLine invoiceLine : lines) {
			double tempCount = invoiceLine.getInvoicedQuantity().doubleValue();
			double tempUnitPrice = invoiceLine.getUnitPrice().doubleValue();
			double tempPriceWithoutTax = invoiceLine.getLineNetAmount()
					.doubleValue();

			// CANTIDAD
			if (tempCount < 0) {
				hasNegatives = true;

				tempCount = tempCount * -1;
			}

			count += tempCount;

			// PRECIO UNITARIO
			unitPrice += tempUnitPrice;

			// TOTAL SIN IMPUESTOS
			if (tempPriceWithoutTax < 0) {
				hasNegatives = true;

				tempPriceWithoutTax = tempPriceWithoutTax * -1;
			}

			netPriceWithoutTax += tempPriceWithoutTax;

			// IMPUESTO
			double tempTaxAmount = invoiceLine.getInvoiceLineTaxList().get(0)
					.getTaxAmount().doubleValue();
			double tempTaxableAmount = invoiceLine.getInvoiceLineTaxList()
					.get(0).getTaxableAmount().doubleValue();

			taxAmount += tempTaxAmount;
			taxableAmount += tempTaxableAmount;
		}

		if (hasNegatives) {
			line = new ArrayList<InvoiceLine>();

			InvoiceLine newLine = copyInvoiceLine(first);
			newLine.setUnitPrice(netSubTotal);
			newLine.setLineNetAmount(netSubTotal);
			newLine.getInvoiceLineTaxList().get(0)
					.setTaxAmount(new BigDecimal(taxAmount));
			newLine.getInvoiceLineTaxList().get(0)
					.setTaxableAmount(new BigDecimal(taxableAmount));

			line.add(newLine);
		}

		return line;
	}

	protected InvoiceLine copyInvoiceLine(InvoiceLine source) {
		InvoiceLine target = new InvoiceLine();

		InvoiceLineTax lineTax = new InvoiceLineTax();

		lineTax.setTax(source.getInvoiceLineTaxList().get(0).getTax());

		target.getInvoiceLineTaxList().add(lineTax);

		// valores a copiar
		target.setInvoicedQuantity(new BigDecimal(1));
		target.setProduct(source.getProduct());
		target.setAttributeSetValue(source.getAttributeSetValue());
		target.setTax(source.getTax());

		return target;
	}
}
