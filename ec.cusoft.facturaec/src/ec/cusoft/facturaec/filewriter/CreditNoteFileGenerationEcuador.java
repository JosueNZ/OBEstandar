/************************************************************************************ 
 * Copyright (C) 2012 Cheli Pineda Ferrer 
 * Licensed under the General Public License 3.0 
 * You may obtain a copy of the License at http://www.gnu.org/licenses/gpl.html
 ************************************************************************************/

package ec.cusoft.facturaec.filewriter;

import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

import org.apache.log4j.Logger;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.access.InvoiceLineTax;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.geography.CountryTrl;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ec.cusoft.facturaec.EEIParamFacturae;
import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;
import ec.cusoft.facturaec.generador.ECWSClient;
import ec.cusoft.facturaec.templates.OBEInvoice_I;
import ec.cusoft.facturaec.templates.OBWSEInvoice_I;

//import ec.cusoft.facturaec.EEIParamFacturae;

public class CreditNoteFileGenerationEcuador extends AbstractFileGeneration implements
    OBEInvoice_I, OBWSEInvoice_I {

  static Logger log4j = Logger.getLogger(CreditNoteFileGenerationEcuador.class);

  public String getFTPFolderName() {
    return "Nota_Credito";
  }

  public String generateFile(Invoice invoice, String rootDirectory, String lang) throws Exception {
    String strDocType = invoice.getDocumentType().getEeiEdocType().toString().replaceAll("\\s", "");
    boolean boolIsEDoc = invoice.getDocumentType().isEeiIsEdoc();

    if (!boolIsEDoc) {
      throw new OBException(
          "No es posible generar Documento Electrónico,la parametrización del tipo de documento no es válida.");
    }
    if (!strDocType.trim().equals("04")) {
      throw new OBException("Tipo de documento electrónico no configurado como Nota de Crédito.");
    }

    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

    OBCriteria<EEIParamFacturae> objParams = OBDal.getInstance()
        .createCriteria(EEIParamFacturae.class);
    objParams.add(Restrictions.eq(EEIParamFacturae.PROPERTY_ACTIVE, true));

    String strXmlVersion = objParams.list().get(0).getXMLVersion();

    if (!strXmlVersion.equals("1.0.0") && !strXmlVersion.equals("1.1.0")) {
      throw new OBException(
          "Versión de XML inválida. Por favor configure la versión (1.0.0 - 1.1.0)");
    }

    if (invoice.getEeiRefInv() == null)
      throw new OBException(
          "El documento seleccionado no tiene asignada una factura de referencia.");
    else if (!invoice.getEeiRefInv().isSalesTransaction())
      throw new OBException("La factura de referencia no puede ser de Compras.");

    // root elements
    Document doc = docBuilder.newDocument();
    doc.setXmlStandalone(true);
    Element rootElement = doc.createElement("notaCredito");
    rootElement.setAttribute("id", "comprobante");
    rootElement.setAttribute("version", strXmlVersion);
    doc.appendChild(rootElement);

    /**
     * Nodo Info Tributaria
     **********************************************************************************************************************/

    Element infoTributaria = doc.createElement("infoTributaria");
    rootElement.appendChild(infoTributaria);

    Element tipoEmision = doc.createElement("tipoEmision");
    tipoEmision.appendChild(doc.createTextNode("1")); // 1 normal - 2
    // indisponibilidad
    // del
    // sistema
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

    // TIPO DOCUMENTO
    String strCodDoc = invoice.getDocumentType().getEeiEdocType();
    Element codDoc = doc.createElement("codDoc");
    codDoc.appendChild(doc.createTextNode(strCodDoc));
    infoTributaria.appendChild(codDoc);

    // NÚMERO DE DOCUMENTO
    if (invoice.getDocumentNo().length() < 8) {
      throw new OBException("Formato de número de documento inválido. (Prefijo 000-000-).");
    }
    String strSubDocumentNo = invoice.getDocumentNo().substring(8);

    while (strSubDocumentNo.length() < 9) {
      strSubDocumentNo = "0" + strSubDocumentNo;
    }

    log4j.debug("parte 2    " + strSubDocumentNo);

    String strSubDocumentNo1 = invoice.getDocumentNo().substring(0, 8);

    log4j.debug("parte 1   " + strSubDocumentNo1);
    String documentnoX = strSubDocumentNo1 + strSubDocumentNo;
    String[] documentno = null;

    log4j.debug(documentnoX);

    if (documentnoX.matches("^\\d{3}-\\d{3}-\\d{9}$")) {
      // documentno = invoice.getDocumentNo().split("-");
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
    
    // DIRECCIÓN
    String headquartersCountry = null;
    try {
      OBContext.setAdminMode(true);
      for (CountryTrl countryTrl : invoice.getOrganization().getOrganizationInformationList()
          .get(0).getLocationAddress().getCountry().getCountryTrlList()) {
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
      headquartersCountry = invoice.getOrganization().getOrganizationInformationList().get(0)
          .getLocationAddress().getCountry().getName();
    }

    String headQuartersaddress = (invoice.getOrganization().getOrganizationInformationList().get(0)
        .getLocationAddress().getAddressLine1() == null ? " " : invoice.getOrganization()
        .getOrganizationInformationList().get(0).getLocationAddress().getAddressLine1())
        + "--"
        + (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getAddressLine2() == null ? " " : invoice.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getAddressLine2())
        + "--"
        + (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getPostalCode() == null ? " " : invoice.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getPostalCode())
        + "--"
        + (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getCityName() == null ? " " : invoice.getOrganization()
            .getOrganizationInformationList().get(0).getLocationAddress().getCityName())
        + "--"
        + (invoice.getOrganization().getOrganizationInformationList().get(0).getLocationAddress()
            .getRegion() == null ? " " : invoice.getOrganization().getOrganizationInformationList()
            .get(0).getLocationAddress().getRegion().getName()) + "--" + headquartersCountry;

    /*
     * Element dirMatriz = doc.createElement("dirMatriz");
     * dirMatriz.appendChild(doc.createTextNode(headQuartersaddress));
     * infoTributaria.appendChild(dirMatriz);
     */

    /**
     * fin Nodo Info Tributaria
     *******************************************************************************************************/

    /**
     * Nodo Info Nota Crédito
     *******************************************************************************************************/

    Element infoNotaCredito = doc.createElement("infoNotaCredito");
    rootElement.appendChild(infoNotaCredito);

    // fecha de emisión
    Element fechaEmision = doc.createElement("fechaEmision");
    SimpleDateFormat ecFormat = new SimpleDateFormat("dd/MM/yyyy");

    if (invoice.getInvoiceDate() == null)
      throw new OBException("El documento debe tener fecha de facturación.");

    String strFechaEmision = ecFormat.format(invoice.getInvoiceDate());
    fechaEmision.appendChild(doc.createTextNode(strFechaEmision));
    infoNotaCredito.appendChild(fechaEmision);

    // dirección establecimiento
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
        + (invoice.getPartnerAddress().getLocationAddress().getPostalCode() == null ? " " : invoice
            .getPartnerAddress().getLocationAddress().getPostalCode())
        + "--"
        + (invoice.getPartnerAddress().getLocationAddress().getCityName() == null ? " " : invoice
            .getPartnerAddress().getLocationAddress().getCityName())
        + "--"
        + (invoice.getPartnerAddress().getLocationAddress().getRegion() == null ? " " : invoice
            .getPartnerAddress().getLocationAddress().getRegion().getName()) + "--" + country;

    if (headQuartersaddress == null) {
      throw new OBException("Dirección de Establecimiento es nula.");
    } else {
      headQuartersaddress = headQuartersaddress.replaceAll("[\n]", " ");
    }
    Element dirEstablecimiento = doc.createElement("dirEstablecimiento");
    dirEstablecimiento.appendChild(doc.createTextNode(truncate(headQuartersaddress, 300)));
    infoNotaCredito.appendChild(dirEstablecimiento);

    // Tipo de identificación del comprador
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

    Element tipoIdentificacionComprador = doc.createElement("tipoIdentificacionComprador");
    tipoIdentificacionComprador.appendChild(doc.createTextNode(idType));
    infoNotaCredito.appendChild(tipoIdentificacionComprador); 
    
    // Razon social del comprador

    String strDescription = invoice.getBusinessPartner().getDescription() == null ? "ND"
        : invoice.getBusinessPartner().getDescription();
    String strName2 = strExtract && !strDescription.equals("ND") ? strDescription
        : invoice.getBusinessPartner().getName();
    if (strName2 == null || strName2.trim().equals("")) {
      throw new OBException("El comprador no tiene nombre fiscal.");
    }
    strName2 = truncate(strName2, 300);
    Element razonSocialComprador = doc.createElement("razonSocialComprador");
    razonSocialComprador.appendChild(doc.createTextNode(strName2));
    infoNotaCredito.appendChild(razonSocialComprador);

    // identificación del comprador
    String strTaxid = invoice.getBusinessPartner().getTaxID();
    if (strTaxid == null || strTaxid.trim().equals("")) {
      throw new OBException("El comprador no tiene RUC.");
    }

    Element identificacionComprador = doc.createElement("identificacionComprador");
    identificacionComprador.appendChild(doc.createTextNode(strTaxid));
    infoNotaCredito.appendChild(identificacionComprador);

    // Código Docuemento Modificado
    String strCodDocModificado = "01";
    Element codDocModificado = doc.createElement("codDocModificado");
    codDocModificado.appendChild(doc.createTextNode(strCodDocModificado));
    infoNotaCredito.appendChild(codDocModificado);

    // NÚMERO DE DOCUMENTO MODIFICADO
    if (invoice.getEeiRefInv().getDocumentNo().length() < 8) {
      throw new OBException(
          "Formato de número de documento de comprobante modificado inválido. (Prefijo 000-000-).");
    }

    String strNumDocModificadoSecuencial = invoice.getEeiRefInv().getDocumentNo().substring(8);

    while (strNumDocModificadoSecuencial.length() < 9) { // COMPLETAR CON CEROS LA SECUENCIA
      strNumDocModificadoSecuencial = "0" + strNumDocModificadoSecuencial;
    }

    String strNumDocModificado1 = truncate(invoice.getEeiRefInv().getDocumentNo(), 8); // TOMAR
                                                                                       // SECUENCIA
                                                                                       // 000-000-
    String strNumDocModificado = strNumDocModificado1 + strNumDocModificadoSecuencial; // CONCATENAR
                                                                                       // CON
                                                                                       // SECUENCIA
                                                                                       // 000000000

    if (!strNumDocModificado.matches("^\\d{3}-\\d{3}-\\d{9}$")) {
      throw new OBException(
          "El formato del número de documento de la Factura referenciada es incorrecto. El formato corecto es: 000-000-000000000.");
    }

    Element numDocModificado = doc.createElement("numDocModificado");
    numDocModificado.appendChild(doc.createTextNode(strNumDocModificado));
    infoNotaCredito.appendChild(numDocModificado);

    // fecha de emisión Documento sustento
    Element fechaEmisionDocSustento = doc.createElement("fechaEmisionDocSustento");
    SimpleDateFormat ecFormatDocSustento = new SimpleDateFormat("dd/MM/yyyy");
    String strFechaEmisionsustento = invoice.getEeiRefInv().getInvoiceDate() != null ? ecFormatDocSustento
        .format(invoice.getEeiRefInv().getInvoiceDate()) : "";
    fechaEmisionDocSustento.appendChild(doc.createTextNode(strFechaEmisionsustento));
    infoNotaCredito.appendChild(fechaEmisionDocSustento);

    // formateador
    DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
    simbolos.setDecimalSeparator('.');
    DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);

    // Total sin Impuestos
    double TotalSinImpuestos = Math.abs(invoice.getSummedLineAmount().doubleValue());

    Element totalSinImpuestos = doc.createElement("totalSinImpuestos");
    totalSinImpuestos.appendChild(doc.createTextNode(formateador.format(TotalSinImpuestos)
        .toString()));
    infoNotaCredito.appendChild(totalSinImpuestos);

    // Valor Modificación
    // String strValorModificacion =
    // invoice.getEeiRefInv().getTotalPaid().toString();
    double strValorModificacion = Math.abs(invoice.getGrandTotalAmount().doubleValue());
    Element valorModificacion = doc.createElement("valorModificacion");
    valorModificacion.appendChild(doc.createTextNode(formateador.format(strValorModificacion)
        .toString()));
    infoNotaCredito.appendChild(valorModificacion);

    // moneda
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
    infoNotaCredito.appendChild(moneda);

    // total con impuestos
    List<InvoiceTax> taxes = invoice.getInvoiceTaxList();

    if (taxes.size() > 0) {
      Element totalConImpuestos = doc.createElement("totalConImpuestos");
      infoNotaCredito.appendChild(totalConImpuestos);

      // para control de lineas
      int lineNO = 0;

      // generar cada uno de los impuestos
      for (InvoiceTax tax : taxes) {
        lineNO += 10;// incrementar linea

        // chequear que no hayan impuestos 0
        double taxAmount = Math.abs(tax.getTaxAmount().doubleValue());

        if (taxAmount >= 0) {
          Element totalConImpuesto = doc.createElement("totalImpuesto");
          totalConImpuestos.appendChild(totalConImpuesto);

          String taxName = tax.getTax().getEeiSriTaxcatCode();

          if (taxName == null || taxName.equals("")) {
            String msg = "En la Tasa de Impuesto de la línea de impuestos número " + lineNO
                + " no está configurado el campo \"Identificador SRI de Impuestos\".";

            throw new OBException(msg);
          }

          // codigo
          Element codigo = doc.createElement("codigo");

          if (tax.getTax().isTaxdeductable() && !tax.getTax().getRate().toString().equals("0"))// (tax.getTax().getName().toString().equals("IVA
            // 12%
            // -
            // 01-01-2011"))
            codigo.appendChild(doc.createTextNode("2"));
          else if (tax.getTax().isTaxdeductable() && tax.getTax().getRate().toString().equals("0"))// (tax.getTax().getName().toString().equals("IVA
            // 0%
            // -
            // 01-01-2011"))
            codigo.appendChild(doc.createTextNode("2"));
          else
            codigo.appendChild(doc.createTextNode("3"));

          totalConImpuesto.appendChild(codigo);

          // codigo porcentaje
          double porciento = tax.getTax().getRate().doubleValue();

          if (porciento < 0)
            porciento = porciento * (-1);

          Element codigoPorcentaje = doc.createElement("codigoPorcentaje");

          if (tax.getTax().isTaxdeductable() && !tax.getTax().getRate().toString().equals("0"))// (tax.getTax().getName().toString().equals("IVA
            // 12%
            // -
            // 01-01-2011"))
            codigoPorcentaje.appendChild(doc.createTextNode(tax.getTax().getEeiSriTaxcatCode()));
          else if (tax.getTax().isTaxdeductable() && tax.getTax().getRate().toString().equals("0"))// (tax.getTax().getName().toString().equals("IVA
            // 0%
            // -
            // 01-01-2011"))
            codigoPorcentaje.appendChild(doc.createTextNode("0"));
          else
            codigoPorcentaje.appendChild(doc.createTextNode("6"));

          totalConImpuesto.appendChild(codigoPorcentaje);

          // base imponible
          double taxableAmount = Math.abs(tax.getTaxableAmount().doubleValue());
          Element baseImponible = doc.createElement("baseImponible");
          baseImponible.appendChild(doc
              .createTextNode(formateador.format(taxableAmount).toString()));
          totalConImpuesto.appendChild(baseImponible);

          // valor
          double amount = Math.abs(tax.getTaxAmount().doubleValue());
          Element valor = doc.createElement("valor");
          valor.appendChild(doc.createTextNode(formateador.format(amount).toString()));
          totalConImpuesto.appendChild(valor);
        }

      }

    }
    // motivo
    // String strMotivo = invoice.getEeiRefInv().getDescription();
    // log4j.debug("Motivo1:" + invoice.getDescription());
    String strMotivo = truncate(invoice.getDescription(), 300);// .replaceAll("[\n]",
    // "
    // "), 300);

    log4j.debug("Motivo2:" + strMotivo);
    if (strMotivo == null || strMotivo.equals("")) {
      throw new OBException(
          "La Nota de Crédito debe tener una descripción válida. (Motivo de Devolución)");
    } else {
      strMotivo = strMotivo.replaceAll("[\n]", " ");
    }
    Element motivo = doc.createElement("motivo");
    motivo.appendChild(doc.createTextNode(strMotivo));
    infoNotaCredito.appendChild(motivo);

    /**
     * Fin Nodo Info Nota Crédito
     *******************************************************************************************************/

    /**
     * Nodo Detalles
     *******************************************************************************************************/
    // impuestos
    Element detalles = doc.createElement("detalles");
    rootElement.appendChild(detalles);

    // chequear si lineas negativas
    // COMPROBACIÓN DE LINEAS NEGATIVAS ELIMINADA -TICKET 2310(OP)
    // List<InvoiceLine> negativesLines =
    // hasInvoiceLinesWithNegativeAmounts(
    // invoice.getInvoiceLineList(), new BigDecimal(TotalSinImpuestos));

    OBCriteria<InvoiceLine> objInvoiceLine = OBDal.getInstance().createCriteria(InvoiceLine.class);
    objInvoiceLine.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE, invoice));
    objInvoiceLine.addOrder(Order.desc(InvoiceLine.PROPERTY_LINENO));

    List<InvoiceLine> details = objInvoiceLine.list();
    // List<InvoiceLine> details = invoice.getInvoiceLineList();

    // if (negativesLines != null)
    // details = negativesLines;

    // ORDENAR POR NÚMERO DE LÍNEA
    Collections.sort(details, new Comparator<InvoiceLine>() {
      @Override
      public int compare(InvoiceLine o1, InvoiceLine o2) {
        // TODO Auto-generated method stub
        return o1.getLineNo().compareTo(o2.getLineNo());
      }
    });

    DecimalFormat formateador6 = new DecimalFormat("#########0.000000", simbolos);

    for (InvoiceLine invoiceLine : details) {
      Element detalle = doc.createElement("detalle");
      detalles.appendChild(detalle);

      // TICKET 2963 A.M. 12/06/2018
      OBCriteria<EEIParamFacturae> ObjEeiParam = OBDal.getInstance().createCriteria(
          EEIParamFacturae.class);
      ObjEeiParam.add(Restrictions.eq(EEIParamFacturae.PROPERTY_ACTIVE, true));

      EEIParamFacturae ObjParams = null;
      ObjParams = OBDal.getInstance()
          .get(EEIParamFacturae.class, ObjEeiParam.list().get(0).getId());

      // codigo Interno
      if (ObjParams.isShowprincipalcode()) {
        String strCodigoInterno = null;
        strCodigoInterno = truncate(invoiceLine.getProduct().getSearchKey(), 25);
        if (strCodigoInterno == null || strCodigoInterno.trim().equals("")) {
          strCodigoInterno = "-";
        }
        /*
         * if (strCodigoInterno == null || strCodigoInterno.trim().equals("")) { throw new
         * OBException("El producto (" + invoiceLine.getProduct().getName() +
         * ") no tiene código princial (value)."); }
         */
        log4j.debug("Código Interno:" + strCodigoInterno);
        Element codigoInterno = doc.createElement("codigoInterno");
        codigoInterno.appendChild(doc.createTextNode(strCodigoInterno));
        detalle.appendChild(codigoInterno);
      }
      // codigo Adicional

      if (ObjParams.isShowauxiliarycode()) {

        String strCodigoAdicional = null;
        strCodigoAdicional = truncate(invoiceLine.getProduct().getEeiAlternativeidentifier(), 25);

        if (strCodigoAdicional == null || strCodigoAdicional.trim().equals("")) {
          strCodigoAdicional = truncate(invoiceLine.getProduct().getSearchKey(), 25);
        }
        if (strCodigoAdicional == null || strCodigoAdicional.trim().equals("")) {
          strCodigoAdicional = "-";
        }
        log4j.debug("Código Adicional: " + strCodigoAdicional);

        Element codigoAdicional = doc.createElement("codigoAdicional");
        codigoAdicional.appendChild(doc.createTextNode(strCodigoAdicional));
        detalle.appendChild(codigoAdicional);
      }
      // FIN TICKET 2963

      // descripcion
      String strDescripcion = "";
      
      strDescripcion = invoiceLine.getDescription() == null ? (invoiceLine.getProduct().getName() == null ? "" : invoiceLine.getProduct().getName() ) 
          :invoiceLine.getDescription();
        /*
      if (ObjParams.getProductName().equals("N")) {
    	  strDescripcion = invoiceLine.getProduct().getName();
      }else if (ObjParams.getProductName().equals("D")){
    	  strDescripcion = invoiceLine.getProduct().getDescription();
      }else if (ObjParams.getProductName().equals("ND")){
    	  strDescripcion = invoiceLine.getProduct().getName()+
    			  (invoiceLine.getProduct().getDescription()==null?"":" - "+ invoiceLine.getProduct().getDescription());
      }
      */
      
      strDescription = truncate(strDescripcion, 300);
      if (strDescription == null || strDescription.trim().equals("")) {
        throw new OBException(
            "El producto (" + invoiceLine.getProduct().getName() + ") no tiene descripción o nombre.");
      }
      strDescripcion = strDescripcion.replaceAll("[\n]", " ");
      Element descripcion = doc.createElement("descripcion");
      descripcion.appendChild(doc.createTextNode(strDescripcion));
      detalle.appendChild(descripcion);

      // cantidad
      double Cantidad = Math.abs(invoiceLine.getInvoicedQuantity().doubleValue());
      double dbPrecioUnitario = Math.abs(invoiceLine.getUnitPrice().doubleValue());
      String strCantidad = "", strPrecioUnitario = "";
      if (strXmlVersion.equals("1.1.0")) {
        strCantidad = formateador6.format(Cantidad);
        strPrecioUnitario = formateador6.format(dbPrecioUnitario);
      } else {
        strCantidad = formateador.format(Cantidad);
        strPrecioUnitario = formateador.format(dbPrecioUnitario);
      }
      Element cantidad = doc.createElement("cantidad");
      cantidad.appendChild(doc.createTextNode(strCantidad));
      detalle.appendChild(cantidad);

      DecimalFormat formatDecimal = new DecimalFormat("#########0.00", simbolos);

      // precio unitario

      Element precioUnitario = doc.createElement("precioUnitario");
      precioUnitario.appendChild(doc.createTextNode(strPrecioUnitario));
      detalle.appendChild(precioUnitario);

      // descuento
      if (invoiceLine.getPriceAdjustment() != null) {
        String strDescuento = formateador.format(
            Math.abs(invoiceLine.getPriceAdjustment().getDiscount().doubleValue())).toString();

        Element descuento = doc.createElement("descuento");
        descuento.appendChild(doc.createTextNode(strDescuento));
        detalle.appendChild(descuento);
      }

      double dbsPrecioTotalSinImpuesto = Math.abs(invoiceLine.getLineNetAmount().doubleValue());
      Element precioTotalSinImpuesto = doc.createElement("precioTotalSinImpuesto");
      precioTotalSinImpuesto.appendChild(doc.createTextNode(formatDecimal.format(
          dbsPrecioTotalSinImpuesto).toString()));
      detalle.appendChild(precioTotalSinImpuesto);

      if (invoiceLine.getDescription() != null && !invoiceLine.getDescription().trim().equals("")) {
        // detalles->detalle->detallesAdicionales
        Element detallesAdicionales;
        Element detAdicional;
        detallesAdicionales = doc.createElement("detallesAdicionales");
        detalle.appendChild(detallesAdicionales);

        detAdicional = doc.createElement("detAdicional");

        detAdicional.setAttribute("nombre", "detadicional");

        String strAttribute = truncate(invoiceLine.getDescription(), 300);
        if (strAttribute == null || strAttribute.trim().equals("")) {
          throw new OBException("El producto (" + invoiceLine.getProduct().getName()
              + ") no tiene un detalle adicional (Descripción en línea).");
        }
        strAttribute = strAttribute.replaceAll("[\n]", " ");
        detAdicional.setAttribute("valor", strAttribute);
        detallesAdicionales.appendChild(detAdicional);
      }

      // impuestos
      Element invImpuestos = doc.createElement("impuestos");
      detalle.appendChild(invImpuestos);

      List<InvoiceLineTax> taxesLines = invoiceLine.getInvoiceLineTaxList();
      for (InvoiceLineTax invoiceLineTax : taxesLines) {

        Element invImpuesto = doc.createElement("impuesto");
        invImpuestos.appendChild(invImpuesto);

        // codigo
        Element codigoT = doc.createElement("codigo");

        if (invoiceLineTax.getTax().isTaxdeductable()
            && !invoiceLineTax.getTax().getRate().toString().equals("0"))// (invoiceLineTax.getTax().getName().toString().equals("IVA
          // 12% - 01-01-2011"))
          codigoT.appendChild(doc.createTextNode("2"));
        else if (invoiceLineTax.getTax().isTaxdeductable()
            && invoiceLineTax.getTax().getRate().toString().equals("0"))// (invoiceLineTax.getTax().getName().toString().equals("IVA
          // 0% - 01-01-2011"))
          codigoT.appendChild(doc.createTextNode("2"));
        else
          codigoT.appendChild(doc.createTextNode("3"));

        invImpuesto.appendChild(codigoT);

        // codigo porcentaje
        /*
         * double porciento = invoiceLineTax.getTax().getRate().doubleValue();
         * 
         * if(porciento < 0) porciento = porciento * (-1);
         */

        Element codigoPorcentajeT = doc.createElement("codigoPorcentaje");

        if (invoiceLineTax.getTax().isTaxdeductable()
            && !invoiceLineTax.getTax().getRate().toString().equals("0"))// (invoiceLineTax.getTax().getName().toString().equals("IVA
          // 12% - 01-01-2011"))
          codigoPorcentajeT.appendChild(doc.createTextNode(invoiceLineTax.getTax()
              .getEeiSriTaxcatCode())); // validar
        else if (invoiceLineTax.getTax().isTaxdeductable()
            && invoiceLineTax.getTax().getRate().toString().equals("0"))// (invoiceLineTax.getTax().getName().toString().equals("IVA
          // 0% - 01-01-2011"))
          codigoPorcentajeT.appendChild(doc.createTextNode("0")); // validar
        else
          codigoPorcentajeT.appendChild(doc.createTextNode("6")); // validar

        invImpuesto.appendChild(codigoPorcentajeT);

        // tarifa
        String strTarifa = invoiceLineTax.getTax().getRate().toString();
        Element tarifa = doc.createElement("tarifa");
        tarifa.appendChild(doc.createTextNode(strTarifa));
        invImpuesto.appendChild(tarifa);

        // base imponible
        double taxableAmount = Math.abs(invoiceLineTax.getTaxableAmount().doubleValue());
        Element baseImponibleT = doc.createElement("baseImponible");
        baseImponibleT
            .appendChild(doc.createTextNode(formateador.format(taxableAmount).toString()));
        invImpuesto.appendChild(baseImponibleT);

        // valor
        double amount = Math.abs(invoiceLineTax.getTaxAmount().doubleValue());
        Element valorT = doc.createElement("valor");
        valorT.appendChild(doc.createTextNode(formateador.format(amount).toString()));
        invImpuesto.appendChild(valorT);
      }
    }
    /**
     * Fin Nodo Detalles
     *******************************************************************************************************/

    /**
     * Nodo Info Adicional
     *******************************************************************************************************/

    // INFOADICIONAL

    String dataInvoice[] = new String[6];
    dataInvoice = ClientSOAP.getDataInv(invoice.getId(), invoice, null);    
    
    OBCriteria<EEIParamFacturae> ObjEeiParam = OBDal.getInstance()
            .createCriteria(EEIParamFacturae.class);
        ObjEeiParam.add(Restrictions.eq(EEIParamFacturae.PROPERTY_ACTIVE, true));
    EEIParamFacturae ObjParams = null;
    ObjParams = OBDal.getInstance().get(EEIParamFacturae.class, ObjEeiParam.list().get(0).getId());
    
    if ((((invoice.getDescription() != null && !invoice.getDescription().trim().equals("")) || (invoice
        .getEeiDescription() != null && !invoice.getEeiDescription().trim().equals(""))) && !invoice
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
          StrUnionCadenaSinSaltos=";";
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

      if (((invoice.getDescription() != null && !invoice.getDescription().trim().equals("")) || (invoice
          .getEeiDescription() != null && !invoice.getEeiDescription().trim().equals("")))
          && !invoice.getDocumentType().getEeiDescriptionfields().equals("NO")) {

        if (invoice.getDocumentType().getEeiDescriptionfields().equals("DEDA")) {

          StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos+(invoice.getDescription() == null ? "" : invoice
              .getDescription())
              + ";"
              + (invoice.getEeiDescription() == null ? "" : invoice.getEeiDescription());

        } else if (invoice.getDocumentType().getEeiDescriptionfields().equals("DE")) {

          StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos+(invoice.getDescription() == null ? "" : invoice
              .getDescription());

        } else if (invoice.getDocumentType().getEeiDescriptionfields().equals("DA")) {

          StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos+(invoice.getEeiDescription() == null ? "" : invoice
              .getEeiDescription());

        }

        StrUnionCadenaSinSaltos = String.valueOf(StrUnionCadenaSinSaltos).replaceAll("[\n]", ";");

        StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos.replaceAll(";;;", ";");
        StrUnionCadenaSinSaltos = StrUnionCadenaSinSaltos.replaceAll(";;", ";");

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
    /**
     * Fin Info Adicional
     *******************************************************************************************************/

    DOMSource domSource = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.transform(domSource, result);

    return writer.toString();
  }

  public String generateFile(Set<Invoice> invoices, String rootDirectory, String lang)
      throws Exception {
    return null;// new File(rootDirectory, "ecuador.xml");
  }

  public boolean validateFile(File file) throws Exception {
    URL schemaFile = new URL("http://cheli.aradaen.com/factura.xsd");
    Source xmlFile = new StreamSource(file);
    // SchemaFactory schemaFactory =
    // SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
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

  @Override
  public String sendFile(ConnectionProvider con, File file, Invoice invoice, String strLanguage)
      throws Exception {

    ECWSClient client = new ECWSClient();
    String res = null; // client.send(con, file, invoice, strLanguage);

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

    return code;
  }
}
