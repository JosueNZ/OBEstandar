package ec.cusoft.facturaec.filewriter;

import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.geography.CountryTrl;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.db.DalConnectionProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.apache.commons.lang.StringUtils;
import ec.cusoft.facturaec.EEIParamFacturae;
import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;
import ec.cusoft.facturaec.generador.ECWSClient;
import ec.cusoft.facturaec.templates.OBEInvoice_I;
import ec.cusoft.facturaec.templates.OBWSEInvoice_I;

public class WithholdingFileGenerationEcuador extends AbstractFileGeneration implements OBEInvoice_I, OBWSEInvoice_I {

	static Logger log4j = Logger.getLogger(WithholdingFileGenerationEcuador.class);

	public String getFTPFolderName() {
		return "Retenciones";
	}

	public String generateFile(Invoice invoice, String rootDirectory, String lang) throws Exception {
		String strDocType = invoice.getSswhCDoctype().getEeiEdocType().toString().replaceAll("\\s", "");
		boolean boolIsEDoc = invoice.getSswhCDoctype().isEeiIsEdoc();

		DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
		simbolos.setDecimalSeparator('.');
		DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);

		if (!boolIsEDoc) {
			throw new OBException(
					"No es posible generar Documento Electrónico,la parametrización del tipo de documento no es válida.");
		}
		if (!strDocType.trim().equals("07")) {
			throw new OBException("Tipo de documento electrónico no configurado como Retención.");
		}
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// ELEMENTOS PRINCIPALES
		Document doc = docBuilder.newDocument();
		doc.setXmlStandalone(true);
		Element rootElement = doc.createElement("comprobanteRetencion");
		rootElement.setAttribute("id", "comprobante");
		rootElement.setAttribute("version", "2.0.0");
		doc.appendChild(rootElement);

		// INFOTRIBUTARIA
		Element infoTributaria = doc.createElement("infoTributaria");
		rootElement.appendChild(infoTributaria);

		Element tipoEmision = doc.createElement("tipoEmision");
		tipoEmision.appendChild(doc.createTextNode("1"));
		infoTributaria.appendChild(tipoEmision);

		// RUC
		String strRuc = invoice.getOrganization().getOrganizationInformationList().get(0).getTaxID();
		if (!strRuc.matches("^\\d{13}$")) {
			throw new OBException("El formato del RUC es incorrecto.");
		}
		Element ruc = doc.createElement("ruc");
		ruc.appendChild(doc.createTextNode(strRuc));
		infoTributaria.appendChild(ruc);

		// CLAVE DE ACCESO
		boolean strKeyAccessGenerate = (ClientSOAP.SelectParams(3).equals("Y") ? true : false);

		// New Name - Description
		boolean strExtract = (ClientSOAP.SelectParams(4).equals("Y") ? true : false);

		if (strKeyAccessGenerate) {
			String strKeyAccess = null;
			strKeyAccess = invoice.getEeiCodigo();
			if (strKeyAccess == null || strKeyAccess.equals("")) {
				throw new OBException("Clave de acceso no encontrada.");
			}
			if (strKeyAccess.length() != 49) {
				throw new OBException("Extensión de clave de acceso no válida (49 dígitos).");
			}
			Element keyAccess = doc.createElement("claveAcceso");
			keyAccess.appendChild(doc.createTextNode(strKeyAccess));
			infoTributaria.appendChild(keyAccess);
		}

		// TIPO DE DOCUMENTO
		if (invoice.getSswhCDoctype() == null) {
			throw new OBException("No se encontró un tipo de documento para la retención.");
		}
		String strCodDoc = invoice.getSswhCDoctype().getEeiEdocType();
		Element codDoc = doc.createElement("codDoc");
		codDoc.appendChild(doc.createTextNode(strCodDoc));
		infoTributaria.appendChild(codDoc);

		// NÚMERO DE DOCUMENTO
		String orderRef = invoice.getSswhWithholdingref();

		if (orderRef == null || orderRef.equals("")) {
			throw new OBException("La factura no tiene asociada el Número de Retención.");
		}

		if (invoice.getSswhWithholdingref().length() < 8) {
			throw new OBException("Formato de número de documento inválido. (Prefijo 000-000-).");
		}
		String strSubDocumentNo = orderRef.substring(8);
		while (strSubDocumentNo.length() < 9) {
			strSubDocumentNo = "0" + strSubDocumentNo;
		}

		log4j.debug("parte 2    " + strSubDocumentNo);

		String strSubDocumentNo1 = invoice.getSswhWithholdingref().substring(0, 8);
		log4j.debug("parte 1   " + strSubDocumentNo1);
		String documentnoX = strSubDocumentNo1 + strSubDocumentNo;
		String[] documentno = null;

		log4j.debug(documentnoX);

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
		OBCriteria<Organization> ObjOrganization = OBDal.getInstance().createCriteria(Organization.class);
		OrganizationType objOrganizationType = OBDal.getInstance().get(OrganizationType.class, "1");
		ObjOrganization.add(Restrictions.eq(Organization.PROPERTY_ORGANIZATIONTYPE, objOrganizationType));

		String strMicroBusiness = null;
		String strWithholdingAgent = null;

		if (ObjOrganization.list().size() > 0) {
			strMicroBusiness = ObjOrganization.list().get(0).getEeiMicroBusiness();
			strWithholdingAgent = ObjOrganization.list().get(0).getEeiWithholdingAgent();
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

		// DIRECCIÓN MATRIZ
		String headquartersCountry = null;
		try {
			OBContext.setAdminMode(true);
			for (CountryTrl countryTrl : invoice.getOrganization().getOrganizationInformationList().get(0)
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
			headquartersCountry = invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
					.getCountry().getName();
		}

		String headQuartersaddress = (invoice.getOrganization().getOrganizationInformationList().get(0)
				.getLocationAddress().getAddressLine1() == null
						? " "
						: invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
								.getAddressLine1())
				+ "--"
				+ (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
						.getAddressLine2() == null
								? " "
								: invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
										.getAddressLine2())
				+ "--"
				+ (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
						.getPostalCode() == null
								? " "
								: invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
										.getPostalCode())
				+ "--"
				+ (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
						.getCityName() == null
								? " "
								: invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
										.getCityName())
				+ "--"
				+ (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
						.getRegion() == null ? " "
								: invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
										.getRegion().getName())
				+ "--" + headquartersCountry;

		// INFO COMPROBANTE RETENCIÓN
		Element infoCompRetencion = doc.createElement("infoCompRetencion");
		rootElement.appendChild(infoCompRetencion);

		log4j.debug("Clave" + infoCompRetencion);

		// FECHA DE EMISIÓN
		Element fechaEmision = doc.createElement("fechaEmision");
		SimpleDateFormat ecFormat = new SimpleDateFormat("dd/MM/yyyy");

		if (invoice.getSswhDatewithhold() == null) {
			throw new OBException("Fecha Retención no seleccionada.");
		}
		fechaEmision.appendChild(doc.createTextNode(ecFormat.format(invoice.getSswhDatewithhold())));
		infoCompRetencion.appendChild(fechaEmision);

		// DIRECCIÓN ESTABLECIMIENTO
		String country = null;
		try {
			OBContext.setAdminMode(true);
			for (CountryTrl countryTrl : invoice.getPartnerAddress().getLocationAddress().getCountry()
					.getCountryTrlList()) {
				if (countryTrl.getLanguage().getLanguage().equals(lang)) {
					country = countryTrl.getName();
				}
			}
		} finally {
			OBContext.restorePreviousMode();
		}

		if (country == null) {
			country = invoice.getPartnerAddress().getLocationAddress().getCountry().getName();
		}

		String address = (invoice.getPartnerAddress().getLocationAddress().getAddressLine1() == null ? " "
				: invoice.getPartnerAddress().getLocationAddress().getAddressLine1())
				+ "--"
				+ (invoice.getPartnerAddress().getLocationAddress().getAddressLine2() == null ? " "
						: invoice.getPartnerAddress().getLocationAddress().getAddressLine2())
				+ "--"
				+ (invoice.getPartnerAddress().getLocationAddress().getPostalCode() == null ? " "
						: invoice.getPartnerAddress().getLocationAddress().getPostalCode())
				+ "--"
				+ (invoice.getPartnerAddress().getLocationAddress().getCityName() == null ? " "
						: invoice.getPartnerAddress().getLocationAddress().getCityName())
				+ "--" + (invoice.getPartnerAddress().getLocationAddress().getRegion() == null ? " "
						: invoice.getPartnerAddress().getLocationAddress().getRegion().getName())
				+ "--" + country;

		if (headQuartersaddress == null) {
			throw new OBException("Dirección de Establecimiento es nula.");
		} else {
			headQuartersaddress = headQuartersaddress.replaceAll("[\n]", " ");
		}
		Element dirEstablecimiento = doc.createElement("dirEstablecimiento");
		dirEstablecimiento.appendChild(doc.createTextNode(truncate(headQuartersaddress, 300)));
		infoCompRetencion.appendChild(dirEstablecimiento);

		String idType = null;

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

		Element tipoIdentificacionSujetoRetenido = doc.createElement("tipoIdentificacionSujetoRetenido");
		tipoIdentificacionSujetoRetenido.appendChild(doc.createTextNode(idType));
		infoCompRetencion.appendChild(tipoIdentificacionSujetoRetenido);

		// TIPO SUJETO RETENIDO
		if (StringUtils.equals(idType, "08")) {

			String strTipoSujetoRetenido = invoice.getBusinessPartner().getSSWHTaxpayer().getSearchKey();
			if (StringUtils.isEmpty(strTipoSujetoRetenido)) {
				throw new OBException("El tipo de contribuyente del tercero no tiene un código relacionado");
			}

			Element tipoSujetoRetenido = doc.createElement("tipoSujetoRetenido");
			tipoSujetoRetenido.appendChild(doc.createTextNode(strTipoSujetoRetenido));
			infoCompRetencion.appendChild(tipoSujetoRetenido);
		}

		// PARTE RELACIONADA
		String strParteRel = invoice.getBusinessPartner().getSSWHTaxpayer().equals("Y") ? "SI" : "NO";
		Element parteRel = doc.createElement("parteRel");
		parteRel.appendChild(doc.createTextNode(strParteRel));
		infoCompRetencion.appendChild(parteRel);

		// RAZÓN SOCIAL
		String strDescription = invoice.getBusinessPartner().getDescription() == null ? "ND"
				: invoice.getBusinessPartner().getDescription();
		String strName2 = strExtract && !strDescription.equals("ND") ? strDescription
				: invoice.getBusinessPartner().getName();
		if (strName2 == null || strName2.trim().equals("")) {
			throw new OBException("El cliente no tiene nombre fiscal.");
		}
		strName2 = truncate(strName2, 300);
		strName2 = strName2.replaceAll("[\n]", " ");
		Element razonSocialComprador = doc.createElement("razonSocialSujetoRetenido");
		razonSocialComprador.appendChild(doc.createTextNode(strName2));
		infoCompRetencion.appendChild(razonSocialComprador);

		log4j.debug("Razon Social" + strName2);

		// IDENTIFICACIÓN
		String strTaxid = invoice.getBusinessPartner().getTaxID();
		if (strTaxid == null || strTaxid.trim().equals("")) {
			throw new OBException("El cliente no tiene RUC.");
		}

		Element identificacionComprador = doc.createElement("identificacionSujetoRetenido");
		identificacionComprador.appendChild(doc.createTextNode(strTaxid));
		infoCompRetencion.appendChild(identificacionComprador);

		// PERÍODO FISCAL
		int year = invoice.getSswhDatewithhold().getYear() + 1900;
		int month = invoice.getSswhDatewithhold().getMonth() + 1;

		String strYear = String.valueOf(year);
		String strMonth = String.valueOf(month);

		if (month < 10)
			strMonth = "0" + strMonth;

		String fiscalPeriod = strMonth + "/" + strYear;

		Element fiscalP = doc.createElement("periodoFiscal");
		fiscalP.appendChild(doc.createTextNode(fiscalPeriod));
		infoCompRetencion.appendChild(fiscalP);

		// -----DOCUMENTOS SUSTENTO
		Element docsSustento = doc.createElement("docsSustento");
		rootElement.appendChild(docsSustento);

		Element docSustento = doc.createElement("docSustento");
		docsSustento.appendChild(docSustento);

		// CÓDIGO SUSTENTO
		if (invoice.getSswhCodelivelihood() == null) {
			throw new OBException("No existe un código de sustento seleccionado.");
		}
		String strCodigoSustento = invoice.getSswhCodelivelihood().getSearchKey();
		Element codSustento = doc.createElement("codSustento");
		codSustento.appendChild(doc.createTextNode(strCodigoSustento));
		docSustento.appendChild(codSustento);

		// CÓDIGO DOCUMENTO SUSTENTO
		if (invoice.getSswhLivelihood() == null) {
			throw new OBException("No existe un tipo de comprobante de sustento seleccionado.");
		}
		String strCodigoDocumentoSustento = invoice.getSswhLivelihood().getSearchKey();
		Element codDocSustento = doc.createElement("codDocSustento");
		codDocSustento.appendChild(doc.createTextNode(strCodigoDocumentoSustento));
		docSustento.appendChild(codDocSustento);

		// NÚMERO DE DOCUMENTO SUSTENTO
		if (invoice.getOrderReference() == null) {
			throw new OBException("No existe número de documento de sustento.");
		}
		Element numDocSustento = doc.createElement("numDocSustento");
		numDocSustento.appendChild(doc.createTextNode(invoice.getOrderReference().replaceAll("-", "")));
		docSustento.appendChild(numDocSustento);

		// FECHA EMISIÓN DOCUMENTO SUSTENTO
		Element fechaEmisionDocSustento = doc.createElement("fechaEmisionDocSustento");
		fechaEmisionDocSustento.appendChild(doc.createTextNode(ecFormat.format(invoice.getInvoiceDate())));
		docSustento.appendChild(fechaEmisionDocSustento);

		// FECHA REGISTRO CONTABLE
		Element fechaRegistroContable = doc.createElement("fechaRegistroContable");
		fechaRegistroContable.appendChild(doc.createTextNode(ecFormat.format(invoice.getAccountingDate())));
		docSustento.appendChild(fechaRegistroContable);

		// NÚMERO DE AUTORIZACIÓN DOCUMENTO RELACIONADO
		if (StringUtils.isEmpty(invoice.getSswhNroauthorization())) {
			throw new OBException("No existe número de autorización relacionado.");
		}
		Element numAutDocSustento = doc.createElement("numAutDocSustento");
		numAutDocSustento.appendChild(doc.createTextNode(invoice.getSswhNroauthorization()));
		docSustento.appendChild(numAutDocSustento);

		// PAGO LOCAL
		if (StringUtils.isEmpty(invoice.getPaymentMethod().getSSWHTypePayment())) {
			throw new OBException("No existe el tipo de pago.");
		}
		Element pagoLocExt = doc.createElement("pagoLocExt");
		pagoLocExt.appendChild(doc.createTextNode(invoice.getPaymentMethod().getSSWHTypePayment()));
		docSustento.appendChild(pagoLocExt);

		if (StringUtils.equals(invoice.getPaymentMethod().getSSWHTypePayment(), "02")
				|| StringUtils.equals(invoice.getBusinessPartner().getSswhTaxidtype(), "P")) {

			// TIPO REGION
			if (invoice.getPartnerAddress().getLocationAddress() == null
					|| invoice.getPartnerAddress().getLocationAddress().getCountry() == null
					|| invoice.getPartnerAddress().getLocationAddress().getCountry().getSswhTaxregime() == null
					|| StringUtils.isEmpty(invoice.getPartnerAddress().getLocationAddress().getCountry()
							.getSswhTaxregime().getSearchKey())) {
				throw new OBException("No existe el tipo de región");
			}
			String strTipoRegion = invoice.getPartnerAddress().getLocationAddress().getCountry().getSswhTaxregime()
					.getSearchKey();
			Element tipoRegi = doc.createElement("tipoRegi");
			tipoRegi.appendChild(doc.createTextNode(strTipoRegion));
			docSustento.appendChild(tipoRegi);

			// PAIS EFECTIVO PAGO
			if (invoice.getPaymentMethod().getSSWHCountrySourceOfPayment() == null
					|| StringUtils.isEmpty(invoice.getPaymentMethod().getSSWHCountrySourceOfPayment().getSearchKey())) {
				throw new OBException("No existe número un país de pago efectivo.");
			}
			String strEfecPago = invoice.getPaymentMethod().getSSWHCountrySourceOfPayment().getSearchKey();
			Element paisEfecPago = doc.createElement("paisEfecPago");
			paisEfecPago.appendChild(doc.createTextNode(strEfecPago));
			docSustento.appendChild(paisEfecPago);

			// APLICA DOBLE TRIBUTACIÓN
			String strDobTrib = invoice.getPaymentMethod().isSSWHApplyDoubleTaxation() ? "SI" : "NO";
			Element aplicConvDobTrib = doc.createElement("aplicConvDobTrib");
			aplicConvDobTrib.appendChild(doc.createTextNode(strDobTrib));
			docSustento.appendChild(aplicConvDobTrib);

			// PAGO EXTERNO SUJETO RETENIDO
			String strPagExt = invoice.getPaymentMethod().isSSWHSubjectToWithholding() ? "SI" : "NO";
			Element pagExtSujRetNorLeg = doc.createElement("pagExtSujRetNorLeg");
			pagExtSujRetNorLeg.appendChild(doc.createTextNode(strPagExt));
			docSustento.appendChild(pagExtSujRetNorLeg);

			// PAGO RÉGIMEN FISCAL
			String strFiscalRegime = invoice.getPaymentMethod().isSswhFiscalregime() ? "SI" : "NO";
			Element pagoRegFis = doc.createElement("pagoRegFis");
			pagoRegFis.appendChild(doc.createTextNode(strFiscalRegime));
			docSustento.appendChild(pagoRegFis);
		}

		if (invoice.getSswhCodelivelihood() != null
				&& StringUtils.equals(invoice.getSswhCodelivelihood().getSearchKey(), "41")) {

			BigDecimal impuestoTotal = BigDecimal.ZERO;

			for (InvoiceTax tax : invoice.getInvoiceTaxList()) {
				if (tax.getTax().isTaxdeductable()) {
					impuestoTotal.add(tax.getTaxAmount());
				}
			}

			// TOTAL COMPROBANTES REEMBOLSO
			Element totalComprobantesReembolso = doc.createElement("totalComprobantesReembolso");
			totalComprobantesReembolso.appendChild(
					doc.createTextNode(formateador.format(invoice.getSummedLineAmount().add(impuestoTotal))));
			docSustento.appendChild(totalComprobantesReembolso);

			// TOTAL BASE IMPONIBLE REEMBOLSO
			Element totalBaseImponibleReembolso = doc.createElement("totalBaseImponibleReembolso");
			totalBaseImponibleReembolso
					.appendChild(doc.createTextNode(formateador.format(invoice.getSummedLineAmount())));
			docSustento.appendChild(totalBaseImponibleReembolso);

			// TOTAL IMPUESTO REEMBOLSO
			Element totalImpuestoReembolso = doc.createElement("totalImpuestoReembolso");
			totalImpuestoReembolso.appendChild(doc.createTextNode(formateador.format(impuestoTotal)));
			docSustento.appendChild(totalImpuestoReembolso);

		}

		// TOTAL SIN IMPUESTOS
		Element totalSinImpuestos = doc.createElement("totalSinImpuestos");
		totalSinImpuestos.appendChild(doc.createTextNode(formateador.format(invoice.getSummedLineAmount())));
		docSustento.appendChild(totalSinImpuestos);

		// IMPORTE TOTAL
		Element importeTotal = doc.createElement("importeTotal");
		importeTotal.appendChild(doc.createTextNode(formateador.format(invoice.getGrandTotalAmount())));
		docSustento.appendChild(importeTotal);

		// IMPUESTOS DOCUMENTO DE SUSTENTO
		Element impuestosDocSustento = doc.createElement("impuestosDocSustento");
		docSustento.appendChild(impuestosDocSustento);

		for (InvoiceTax tax : invoice.getInvoiceTaxList()) {

			if (tax.getTax().isTaxdeductable()) {

				Element impuestoDocSustento = doc.createElement("impuestoDocSustento");
				impuestosDocSustento.appendChild(impuestoDocSustento);

				// CODIGO IMPUESTO
				if (StringUtils.isEmpty(tax.getTax().getEeiSriTaxType())) {
					throw new OBException(
							"No existe un tipo de impuesto SRI seleccionado en el impuesto: " + tax.getTax().getName());
				}
				Element codImpuestoDocSustento = doc.createElement("codImpuestoDocSustento");
				codImpuestoDocSustento.appendChild(doc.createTextNode(tax.getTax().getEeiSriTaxType()));
				impuestoDocSustento.appendChild(codImpuestoDocSustento);

				// CODIGO PORCENTAJE
				if (StringUtils.isEmpty(tax.getTax().getEeiSriTaxcatCode())) {
					throw new OBException("No existe un código de porcentaje SRI seleccionado en el impuesto: "
							+ tax.getTax().getName());
				}
				Element codigoPorcentaje = doc.createElement("codigoPorcentaje");
				codigoPorcentaje.appendChild(doc.createTextNode(tax.getTax().getEeiSriTaxcatCode()));
				impuestoDocSustento.appendChild(codigoPorcentaje);

				Element baseImponible = doc.createElement("baseImponible");
				baseImponible.appendChild(doc.createTextNode(formateador.format(tax.getTaxableAmount())));
				impuestoDocSustento.appendChild(baseImponible);

				Element tarifa = doc.createElement("tarifa");
				tarifa.appendChild(doc.createTextNode(formateador.format(tax.getTax().getRate())));
				impuestoDocSustento.appendChild(tarifa);

				Element valorImpuesto = doc.createElement("valorImpuesto");
				valorImpuesto.appendChild(doc.createTextNode(formateador.format(tax.getTaxAmount())));
				impuestoDocSustento.appendChild(valorImpuesto);

			}

		}

		// RETENCIONES
		Element impuestos = doc.createElement("retenciones");
		docSustento.appendChild(impuestos);

		ConnectionProvider conn = new DalConnectionProvider(false);
		Statement sentencia = null;
		ResultSet resultado;
		sentencia = conn.getStatement();
		String StrQuery = "select * from eei_view_invoice where c_invoice_id ='" + invoice.getId().toString() + "'";
		resultado = sentencia.executeQuery(StrQuery);

		int lineNO = 0;
		int countInvoice = 0;

		log4j.debug("\nNumero de lineas encontradas " + resultado.getRow());

		// GENERACIÓN DE LÍNEAS
		while (resultado.next()) {

			lineNO += 10;

			double taxAmount = resultado.getDouble("taxamt");
			double taxableAmount = resultado.getDouble("taxbaseamt");

			TaxRate objTaxRate = OBDal.getInstance().get(TaxRate.class, resultado.getString("c_tax_id"));

			if (taxAmount <= 0 && !objTaxRate.isTaxdeductable() && taxableAmount > 0) {
				countInvoice++;
				Element impuesto = doc.createElement("retencion");
				impuestos.appendChild(impuesto);

				String taxCategoryStr = resultado.getString("em_eei_sri_tax_type");
				String taxRetentionCodeStr = "00000";

				String taxName = resultado.getString("em_eei_sri_taxcat_code");

				log4j.debug("taxName" + taxName);
				if (taxName == null || taxName.equals("")) {
					String msg = "En la Tasa de Impuesto de la línea de impuestos número " + lineNO
							+ " no está configurado el campo \"Identificador SRI de Impuestos\".";

					throw new OBException(msg);
				}

				if (taxCategoryStr.equals("1")) {
					taxRetentionCodeStr = taxName;
					log4j.debug("taxRetentionCodeStr" + taxRetentionCodeStr);

					if (taxRetentionCodeStr.equals("")) {
						String msg = "Para las facturas con impuesto del tipo Rentención RENTA los únicos conceptos de impuesto permitidos son aquellos cuyos códigos son:"
								+ " 303, 304, 307, 308, 309, 310, 311, 312, 320, 322, 332, 340, 341 , 421 , 500 , 501 , 502 , 503 y 524. Verificar que en la configuración de la Tasa de Impuesto"
								+ " definida para la línea de impuestos número " + lineNO + " el campo "
								+ " \"Nombre\" incluya uno de estos códigos.";

						throw new OBException(msg);
					}
				} else if (taxCategoryStr.equals("2")) {
					taxRetentionCodeStr = taxName;

					if (taxRetentionCodeStr.equals("")) {
						String msg = "Para las facturas con impuesto del tipo Rentención IVA los únicos conceptos de impuesto permitidos son aquellos con códigos: "
								+ "1, 2 y 3. Verificar que en la configuración de la Tasa de Impuesto"
								+ " definida para la línea de impuestos número " + lineNO + " el campo "
								+ " \"Nombre\" incluya uno de estos códigos.";

						throw new OBException(msg);
					}
				} else if (taxCategoryStr.equals("6"))
					taxCategoryStr = "4580";
				else {
					String msg = "La línea No." + " tiene el Tipo de Impuesto " + taxCategoryStr
							+ ". Los valores válidos son RETENCIONES RENTA, RETENCIONES IVA o ISD.";

					throw new OBException(msg);
				}

				Element taxCod = doc.createElement("codigo");
				taxCod.appendChild(doc.createTextNode(taxCategoryStr));
				impuesto.appendChild(taxCod);

				log4j.debug("TaxCod" + taxCategoryStr);

				Element codigoRetencion = doc.createElement("codigoRetencion");
				codigoRetencion.appendChild(doc.createTextNode(taxRetentionCodeStr));
				impuesto.appendChild(codigoRetencion);

				log4j.debug("codigoRetencion" + taxRetentionCodeStr);

				Element baseImponible = doc.createElement("baseImponible");
				baseImponible.appendChild(doc.createTextNode(formateador.format(taxableAmount).toString()));
				impuesto.appendChild(baseImponible);

				log4j.debug("baseImponible" + formateador.format(taxableAmount).toString());
				// PORCENTAJE A RETENER
				double drate = resultado.getDouble("rate");

				if (drate < 0)
					drate = drate * (-1);

				Element porcientoRetener = doc.createElement("porcentajeRetener");
				porcientoRetener.appendChild(doc.createTextNode(String.valueOf(formateador.format(drate))));
				impuesto.appendChild(porcientoRetener);

				log4j.debug("porcentajeRetener" + formateador.format(drate).toString());

				// VALOR RETENIDO
				if (taxAmount < 0)
					taxAmount = taxAmount * (-1);

				Element valorRetenido = doc.createElement("valorRetenido");
				valorRetenido.appendChild(doc.createTextNode(formateador.format(taxAmount).toString()));
				impuesto.appendChild(valorRetenido);

				// log4j.debug("valorRetenido" + formateador.format(taxAmount).toString());

				// NÚMERO DE DOCUMENTO SUSTENTO
				/*
				 * if (invoice.getOrderReference() == null) { throw new
				 * OBException("No existe número de documento de sustento."); } Element
				 * numDocSustento = doc.createElement("numDocSustento");
				 * numDocSustento.appendChild(doc.createTextNode(invoice.getOrderReference().
				 * replaceAll("-", ""))); impuesto.appendChild(numDocSustento);
				 */

				// log4j.debug("numDocSustento" + invoice.getOrderReference().replaceAll("-",
				// ""));

				// FECHA EMISIÓN
				/*
				 * Element fechaEmisionDocSustento =
				 * doc.createElement("fechaEmisionDocSustento");
				 * fechaEmisionDocSustento.appendChild(doc.createTextNode(ecFormat.format(
				 * invoice .getInvoiceDate()))); impuesto.appendChild(fechaEmisionDocSustento);
				 * log4j.debug("fechaEmisionDocSustento" +
				 * ecFormat.format(invoice.getInvoiceDate()));
				 */
			}

		}

		// PAGOS
		Element pagos = doc.createElement("pagos");
		docSustento.appendChild(pagos);

		// PAGO
		Element pago = doc.createElement("pago");
		pagos.appendChild(pago);

		// FORMA PAGO
		if (StringUtils.isEmpty(invoice.getPaymentMethod().getEeiCodeEi())) {
			throw new OBException("Código de método de pago no seleccionado.");
		}
		Element formaPago = doc.createElement("formaPago");
		formaPago.appendChild(doc.createTextNode(invoice.getPaymentMethod().getEeiCodeEi()));
		pago.appendChild(formaPago);

		// TOTAL
		Element total = doc.createElement("total");
		total.appendChild(doc.createTextNode(formateador.format(invoice.getGrandTotalAmount())));
		pago.appendChild(total);

		try {
			conn.destroy();
		} catch (Exception e) {

		}

		if (countInvoice == 0) {
			throw new OBException("No se ha encontrado impuestos válidos para la retención.");
		}

		// INFOADICIONAL

		String dataInvoice[] = new String[6];
		dataInvoice = ClientSOAP.getDataInv(invoice.getId(), invoice, null);

		OBCriteria<EEIParamFacturae> ObjEeiParam = OBDal.getInstance().createCriteria(EEIParamFacturae.class);
		ObjEeiParam.add(Restrictions.eq(EEIParamFacturae.PROPERTY_ACTIVE, true));
		EEIParamFacturae ObjParams = null;
		ObjParams = OBDal.getInstance().get(EEIParamFacturae.class, ObjEeiParam.list().get(0).getId());

		if ((((invoice.getDescription() != null && !invoice.getDescription().trim().equals(""))
				|| (invoice.getEeiDescription() != null && !invoice.getEeiDescription().trim().equals("")))
				&& !invoice.getSswhCDoctype().getEeiDescriptionfields().equals("NO"))
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

			if (ObjParams.getAdittionalInfo() != null && !ObjParams.getAdittionalInfo().trim().equals("")) {
				Element campoAdicional = doc.createElement("campoAdicional");
				campoAdicional.setAttribute("nombre", "Descripción");
				campoAdicional.appendChild(doc.createTextNode(truncate(ObjParams.getAdittionalInfo(), 300)));
				infoAdicional.appendChild(campoAdicional);
				StrUnionCadenaSinSaltos = ";";
			}

			if (invoice.getBusinessPartner().getName2() != null
					|| invoice.getBusinessPartner().getDescription() == null) {

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

			if (((invoice.getDescription() != null && !invoice.getDescription().trim().equals(""))
					|| (invoice.getEeiDescription() != null && !invoice.getEeiDescription().trim().equals("")))
					&& !invoice.getSswhCDoctype().getEeiDescriptionfields().equals("NO")) {

				if (invoice.getSswhCDoctype().getEeiDescriptionfields().equals("DEDA")) {

					StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos
							+ (invoice.getDescription() == null ? "" : invoice.getDescription()) + ";"
							+ (invoice.getEeiDescription() == null ? "" : invoice.getEeiDescription());

				} else if (invoice.getSswhCDoctype().getEeiDescriptionfields().equals("DE")) {

					StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos
							+ (invoice.getDescription() == null ? "" : invoice.getDescription());

				} else if (invoice.getSswhCDoctype().getEeiDescriptionfields().equals("DA")) {

					StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos
							+ (invoice.getEeiDescription() == null ? "" : invoice.getEeiDescription());

				}

				StrUnionCadenaSinSaltos = String.valueOf(StrUnionCadenaSinSaltos).replaceAll("[\n]", ";");

				StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos.replaceAll(";;;", ";");
				StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos.replaceAll(";;", ";");

				if (StrUnionCadenaSinSaltos.equals(";")) {
					StrUnionCadenaSinSaltos = "";
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
					strCadenaConcatenada = strCadenaConcatenada + strCadenaParcial;
					intContador = intContador + 1;
					Element campoAdicional2 = doc.createElement("campoAdicional");
					campoAdicional2.setAttribute("nombre", "Descripción" + intContador);
					campoAdicional2.appendChild(doc.createTextNode(truncate(strCadenaParcial, 300)));
					infoAdicional.appendChild(campoAdicional2);
				}
				if (strCadenaConcatenada.length() < StrUnionCadenaSinSaltos.length()) {
					intContador = intContador + 1;
					strCadenaParcial = StrUnionCadenaSinSaltos.substring(strCadenaConcatenada.length(),
							StrUnionCadenaSinSaltos.length());
					Element campoAdicional2 = doc.createElement("campoAdicional");
					campoAdicional2.setAttribute("nombre", "Descripción" + intContador);
					campoAdicional2.appendChild(doc.createTextNode(truncate(strCadenaParcial, 300)));
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

	public String generateFile(Set<Invoice> invoices, String rootDirectory, String lang) throws Exception {
		return null;// new File(rootDirectory, "ecuador.xml");
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

	public boolean validateFile(File file) throws Exception {
		URL schemaFile = new URL("http://cheli.aradaen.com/factura.xsd");
		Source xmlFile = new StreamSource(file);
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.XMLNS_ATTRIBUTE_NS_URI);
		Schema schema = schemaFactory.newSchema(schemaFile);
		Validator validator = schema.newValidator();
		try {
			validator.validate(xmlFile);
		} catch (SAXException e) {
			return false;
		}
		return true;
	}

	@Override
	public String sendFile(ConnectionProvider con, File file, Invoice invoice, String strLanguage) throws Exception {

		ECWSClient client = new ECWSClient();
		String res = null;
		return res;
	}

	private String getRetentionRentCode(String taxName) {
		String code = "";

		if (taxName.contains("311"))
			code = "311";

		if (taxName.contains("309"))
			code = "309";

		if (taxName.contains("310"))
			code = "310";

		if (taxName.contains("341"))
			code = "341";

		if (taxName.contains("303"))
			code = "303";

		if (taxName.contains("304"))
			code = "304";

		if (taxName.contains("308"))
			code = "308";

		if (taxName.contains("320"))
			code = "320";

		if (taxName.contains("322"))
			code = "322";

		if (taxName.contains("340"))
			code = "340";

		if (taxName.contains("307"))
			code = "307";

		if (taxName.contains("312"))
			code = "312";

		if (taxName.contains("332"))
			code = "332";

		if (taxName.contains("323"))
			code = "323";

		if (taxName.contains("327"))
			code = "327";

		if (taxName.contains("340A"))
			code = "340A";

		if (taxName.contains("340B"))
			code = "340B";

		if (taxName.contains("343"))
			code = "343";

		if (taxName.contains("344"))
			code = "344";

		if (taxName.contains("421"))
			code = "421";

		if (taxName.contains("500"))
			code = "500";

		if (taxName.contains("501"))
			code = "501";

		if (taxName.contains("502"))
			code = "502";

		if (taxName.contains("503"))
			code = "503";

		if (taxName.contains("524"))
			code = "524";

		return code;
	}

	private String getRetentionIVACode(String taxName) {
		String code = "";

		if (taxName.contains("1"))
			code = "1";

		if (taxName.contains("2"))
			code = "2";

		if (taxName.contains("3"))
			code = "3";

		if (taxName.contains("9"))

			code = "9";

		if (taxName.contains("10"))

			code = "10";

		return code;
	}
}
