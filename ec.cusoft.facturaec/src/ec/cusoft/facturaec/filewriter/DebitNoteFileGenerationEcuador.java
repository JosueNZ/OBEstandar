/************************************************************************************ 
 * Copyright (C) 2012 Cheli Pineda Ferrer 
 * Licensed under the General Public License 3.0 
 * You may obtain a copy of the License at http://www.gnu.org/licenses/gpl.html
 ************************************************************************************/

package ec.cusoft.facturaec.filewriter;

import java.io.File;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
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
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ec.cusoft.facturaec.EEIParamFacturae;
import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;
import ec.cusoft.facturaec.generador.ECWSClient;
import ec.cusoft.facturaec.templates.OBEInvoice_I;
import ec.cusoft.facturaec.templates.OBWSEInvoice_I;

public class DebitNoteFileGenerationEcuador extends AbstractFileGeneration implements OBEInvoice_I,
    OBWSEInvoice_I {

  static Logger log4j = Logger.getLogger(DebitNoteFileGenerationEcuador.class);

  public String getFTPFolderName() {
    return "Nota_Debito";
  }

  public String generateFile(Invoice invoice, String rootDirectory, String lang) throws Exception {
    String strDocType = invoice.getDocumentType().getEeiEdocType().toString().replaceAll("\\s", "");
    boolean boolIsEDoc = invoice.getDocumentType().isEeiIsEdoc();

    if (!boolIsEDoc) {
      throw new OBException(
          "No es posible generar Documento Electrónico,la parametrización del tipo de documento no es válida.");
    }
    if (!strDocType.trim().equals("05")) {
      throw new OBException("Tipo de documento electrónico no configurado como Nota de Débito.");
    }
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
    // EEIParamFacturae params = getParamFEC(invoice);

    if (invoice.getEeiRefInv() == null)
      throw new OBException(
          "El documento seleccionado no tiene asignada una factura de referencia.");
    // else if (invoice.getEeiRefInv().isSalesTransaction())
    // throw new
    // OBException("La factura de referencia no puede ser de Ventas.");

    // root elements
    Document doc = docBuilder.newDocument();
    doc.setXmlStandalone(true);
    Element rootElement = doc.createElement("notaDebito");
    rootElement.setAttribute("id", "comprobante");
    rootElement.setAttribute("version", "1.0.0");
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
    
    // DIRECCIÓN MATRIZ
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
     * Nodo Info Nota Débito
     *******************************************************************************************************/

    Element infoNotaDebito = doc.createElement("infoNotaDebito");
    rootElement.appendChild(infoNotaDebito);

    // fecha de emisión
    Element fechaEmision = doc.createElement("fechaEmision");
    SimpleDateFormat ecFormat = new SimpleDateFormat("dd/MM/yyyy");

    if (invoice.getInvoiceDate() == null)
      throw new OBException("El documento debe tener fecha de facturación.");

    String strFechaEmision = ecFormat.format(invoice.getInvoiceDate());
    fechaEmision.appendChild(doc.createTextNode(strFechaEmision));
    infoNotaDebito.appendChild(fechaEmision);

    // dirección establecimiento
    if (headQuartersaddress == null) {
      throw new OBException("Dirección de Establecimiento es nula.");
    } else {
      headQuartersaddress = headQuartersaddress.replaceAll("[\n]", " ");
    }
    Element dirEstablecimiento = doc.createElement("dirEstablecimiento");
    dirEstablecimiento.appendChild(doc.createTextNode(truncate(headQuartersaddress, 300)));
    infoNotaDebito.appendChild(dirEstablecimiento);

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
    tipoIdentificacionComprador.appendChild(doc.createTextNode(idType)); // 04
    // RUC
    // 05 cédula
    // 06 pasaporte
    // 07 consumidor
    infoNotaDebito.appendChild(tipoIdentificacionComprador); // Preguntar si
    // en este
    // caso el
    // comprado
    // es el
    // Bussisnes
    // Partner

    // Razon social del comprador

    String strDescription = invoice.getBusinessPartner().getDescription() == null ? "ND"
        : invoice.getBusinessPartner().getDescription();
    String strName2 = strExtract && !strDescription.equals("ND") ? strDescription
        : invoice.getBusinessPartner().getName();
    if (strName2 == null || strName2.trim().equals("")) {
      throw new OBException("El comprador no tiene nombre fiscal.");
    }
    strName2 = truncate(strName2, 300);
    strName2 = strName2.replaceAll("[\n]", " ");
    Element razonSocialComprador = doc.createElement("razonSocialComprador");
    razonSocialComprador.appendChild(doc.createTextNode(strName2));
    infoNotaDebito.appendChild(razonSocialComprador);

    // identificación del comprador
    String strTaxid = invoice.getBusinessPartner().getTaxID();
    if (strTaxid == null || strTaxid.trim().equals("")) {
      throw new OBException("El comprador no tiene RUC.");
    }

    Element identificacionComprador = doc.createElement("identificacionComprador");
    identificacionComprador.appendChild(doc.createTextNode(strTaxid));
    infoNotaDebito.appendChild(identificacionComprador);

    // Código Documento Modificado
    String strCodDocModificado = invoice.getDocumentType().getDocumentCategory() == "AR factura" ? "01"
        : invoice.getDocumentType().getEeiEdocType();
    Element codDocModificado = doc.createElement("codDocModificado");
    codDocModificado.appendChild(doc.createTextNode("01"));
    infoNotaDebito.appendChild(codDocModificado);

    // Número Docuemento Modificado
    String strNumDocModificado = invoice.getEeiRefInv().getDocumentNo();// .substring(8);

    System.out.println("****************** La orden de referencia de la factura referenciada es : "
        + invoice.getEeiRefInv().getOrderReference() + "**************");

    if (strNumDocModificado == null || strNumDocModificado.equals(""))
      throw new OBException(
          "La Factura referenciada no tiene definido el campo Orden de Referencia.");

    strNumDocModificado = strNumDocModificado.substring(8);

    while (strNumDocModificado.length() < 9) {
      strNumDocModificado = "0" + strNumDocModificado;
    }

    String strNumDocModificado1 = invoice.getDocumentNo().substring(0, 8);

    strNumDocModificado = strNumDocModificado1 + strNumDocModificado;

    if (!strNumDocModificado.matches("^\\d{3}-\\d{3}-\\d{9}$"))
      throw new OBException(
          "El formato del campo Código de Documento Modificado es incorrecto. El formato corecto es: 000-000-000000000.");

    Element numDocModificado = doc.createElement("numDocModificado");
    numDocModificado.appendChild(doc.createTextNode(strNumDocModificado));
    infoNotaDebito.appendChild(numDocModificado);

    System.out.println("****************** El codigo para numDocModificado es: "
        + strNumDocModificado + "**************");

    // fecha de emisión Documento sustento
    Element fechaEmisionDocSustento = doc.createElement("fechaEmisionDocSustento");
    SimpleDateFormat ecFormatDocSustento = new SimpleDateFormat("dd/MM/yyyy");
    String strFechaEmisionsustento = invoice.getEeiRefInv().getInvoiceDate() != null ? ecFormatDocSustento
        .format(invoice.getEeiRefInv().getInvoiceDate()) : "";
    fechaEmisionDocSustento.appendChild(doc.createTextNode(strFechaEmisionsustento));
    infoNotaDebito.appendChild(fechaEmisionDocSustento);

    // formateador
    DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
    simbolos.setDecimalSeparator('.');
    DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);

    // Total sin Impuestos
    double TotalSinImpuestos = invoice.getSummedLineAmount().doubleValue();

    Element totalSinImpuestos = doc.createElement("totalSinImpuestos");
    totalSinImpuestos.appendChild(doc.createTextNode(formateador.format(TotalSinImpuestos)
        .toString()));
    infoNotaDebito.appendChild(totalSinImpuestos);

    Element impuestos = doc.createElement("impuestos");
    infoNotaDebito.appendChild(impuestos);

    for (InvoiceTax impuestoObj : invoice.getInvoiceTaxList()) {
      Element impuesto = doc.createElement("impuesto");
      impuestos.appendChild(impuesto);

      /* CODIGO DEL IMPUESTO */
      Element codigo = doc.createElement("codigo");

      /*
       * if (impuestoObj.getTax().isSswhAtsIva().toString().equals("N") &&
       * impuestoObj.getTax().isSswhAtsSource().toString().equals("N"))
       * codigo.appendChild(doc.createTextNode("2")); else
       * codigo.appendChild(doc.createTextNode("3")); totalImpuesto.appendChild(codigo);
       */

      if (impuestoObj.getTax().isTaxdeductable()
          && !impuestoObj.getTax().getRate().toString().equals("0"))// (impuestoObj.getTax().getName().toString().equals("IVA
        // 12%
        // -
        // 01-01-2011"))
        codigo.appendChild(doc.createTextNode("2"));
      else if (impuestoObj.getTax().isTaxdeductable()
          && impuestoObj.getTax().getRate().toString().equals("0"))// (impuestoObj.getTax().getName().toString().equals("IVA
        // 0%
        // -
        // 01-01-2011"))
        codigo.appendChild(doc.createTextNode("2"));
      else
        codigo.appendChild(doc.createTextNode("3"));

      impuesto.appendChild(codigo);
      /* FIN DE CODIGO DEL IMPUESTO */

      /* CODIGO DEL IMPUESTO RATE */
      Element codigoPorcentaje = doc.createElement("codigoPorcentaje");
      if (impuestoObj.getTax().isTaxdeductable()
          && !impuestoObj.getTax().getRate().toString().equals("0"))// (impuestoObj.getTax().getName().toString().equals("IVA
        // 12%
        // -
        // 01-01-2011"))
        codigoPorcentaje
            .appendChild(doc.createTextNode(impuestoObj.getTax().getEeiSriTaxcatCode()));
      else if (impuestoObj.getTax().isTaxdeductable()
          && impuestoObj.getTax().getRate().toString().equals("0"))// (impuestoObj.getTax().getName().toString().equals("IVA
        // 0%
        // -
        // 01-01-2011"))
        codigoPorcentaje.appendChild(doc.createTextNode("0"));
      else
        codigoPorcentaje.appendChild(doc.createTextNode("6"));
      impuesto.appendChild(codigoPorcentaje);
      /* FIN DE CODIGO DEL IMPUESTO RATE */

      double Tarifa = impuestoObj.getTax().getRate().doubleValue();
      Element tarifa = doc.createElement("tarifa");
      // tarifa.appendChild(doc.createTextNode(impuestoObj.getTax().getRate().toPlainString()));
      // //
      // validar original
      tarifa.appendChild(doc.createTextNode(formateador.format(Tarifa).toString()));
      impuesto.appendChild(tarifa);

      double BaseImp = impuestoObj.getTaxableAmount().doubleValue();
      // System.out.print("valor nativo baseimp con format "+formateador.format(BaseImp));
      Element baseImponible = doc.createElement("baseImponible");
      // baseImponible.appendChild(doc.createTextNode(impuestoObj.getTaxableAmount().toPlainString()));
      // original
      baseImponible.appendChild(doc.createTextNode(formateador.format(BaseImp).toString()));
      impuesto.appendChild(baseImponible);

      double Valor = impuestoObj.getTaxAmount().doubleValue();
      // System.out.print("valor nativo con format "+formateador.format(Valor));
      Element valor = doc.createElement("valor");
      // valor.appendChild(doc.createTextNode(impuestoObj.getTaxAmount().toPlainString()));
      // original
      valor.appendChild(doc.createTextNode(formateador.format(Valor).toString()));
      impuesto.appendChild(valor);
    }

    // valorTotal
    Element valorTotal = doc.createElement("valorTotal");

    double valorTotalAmount = invoice.getGrandTotalAmount().doubleValue();
    valorTotal.appendChild(doc.createTextNode(formateador.format(valorTotalAmount).toString()));

    infoNotaDebito.appendChild(valorTotal);

    // *************************INICIA PAGOS
    /** FORMAS DE PAGOS - CC **/

    int intPaymentScheduleDatail = 0;
    String strFinPaymentScheduleID = "ND";
    for (FIN_PaymentSchedule paymentSchedule2 : invoice.getFINPaymentScheduleList()) {
      for (FIN_PaymentScheduleDetail psd2 : paymentSchedule2
          .getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {

        try {
          strFinPaymentScheduleID = psd2.getPaymentDetails().getId() == null ? "ND" : psd2
              .getPaymentDetails().getId();
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

      for (FIN_PaymentSchedule paymentSchedule : invoice.getFINPaymentScheduleList()) {

        List<FIN_PaymentSchedule> lstPaymentSchedule = invoice.getFINPaymentScheduleList();

        if (lstPaymentSchedule.size() > 0) {

          for (FIN_PaymentScheduleDetail psd : paymentSchedule
              .getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {

            String strCodePaymentMethod = "ND";

            try {

              pago = doc.createElement("pago");

              Element formaPago = doc.createElement("formaPago");

              try {
                strReviewPaymentMethod = psd.getPaymentDetails().getId() == null ? "ND" : psd
                    .getPaymentDetails().getId();
              } catch (Exception e1) {
              }
              if (strReviewPaymentMethod.equals("ND")) {

                strCodePaymentMethod = "";

              } else {

                strPaymentMethod = psd.getPaymentDetails().getFinPayment().getPaymentMethod()
                    .getId() == null ? "ND" : psd.getPaymentDetails().getFinPayment()
                    .getPaymentMethod().getId();

                strCodePaymentMethod = psd.getPaymentDetails().getFinPayment().getPaymentMethod()
                    .getEeiCodeEi() == null ? "ND" : psd.getPaymentDetails().getFinPayment()
                    .getPaymentMethod().getEeiCodeEi();

                formaPago.appendChild(doc.createTextNode(strCodePaymentMethod));
                pago.appendChild(formaPago);

                Element total = doc.createElement("total");
                double dblpagos = psd.getAmount().doubleValue();
                total.appendChild(doc.createTextNode(formateador.format(dblpagos).toString()));
                pago.appendChild(total);

                if (invoice.getPaymentMethod().getId()
                    .equals(psd.getPaymentDetails().getFinPayment().getPaymentMethod().getId())) {

                  Element plazo = doc.createElement("plazo");

                  String strUnitTime = "";
                  if (invoice.getPaymentTerms().getOverduePaymentDaysRule() > 0) {

                    double dblplazo = invoice.getPaymentTerms().getOverduePaymentDaysRule();
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
                }
                pagos.appendChild(pago);

                dbdTotalPaymentOut = dbdTotalPaymentOut + psd.getAmount().doubleValue();

              }
            } catch (Exception e) {
            }
            /*
             * if (strCodePaymentMethod.equals("ND")) { throw new OBException("El método de Pago  "
             * + psd.getPaymentDetails ().getFinPayment().getPaymentMethod().getName() +
             * " no esta configurado para el proceso de Facturación Electrónica" );
             * 
             * }
             */
          }

        }

        bgdGrandTotalPayment = new BigDecimal(dbdTotalPaymentOut);
        bgdGrandTotalPayment = bgdGrandTotalPayment.setScale(2, RoundingMode.HALF_UP);
      }

      BigDecimal bgbGranTotalInvoice = invoice.getGrandTotalAmount().setScale(2,
          RoundingMode.HALF_UP);

      if (!bgbGranTotalInvoice.equals(bgdGrandTotalPayment)) {

        if (!strPaymentMethod.equals(invoice.getPaymentMethod().getId())) {
          pagos = doc.createElement("pagos");

          pago = doc.createElement("pago");

          Element formaPago = doc.createElement("formaPago");
          String strCodePaymentMethod = invoice.getPaymentMethod().getEeiCodeEi() == null ? "ND"
              : invoice.getPaymentMethod().getEeiCodeEi();
          if (strCodePaymentMethod.equals("ND")) {
            throw new OBException("El método de Pago " + invoice.getPaymentMethod().getName()
                + " no esta configurado para el proceso de Facturación Electrónica");

          }
          formaPago.appendChild(doc.createTextNode(strCodePaymentMethod));
          pago.appendChild(formaPago);

          Element total = doc.createElement("total");
          double dblpagos = invoice.getGrandTotalAmount().doubleValue();
          total.appendChild(doc.createTextNode(formateador.format(dblpagos).toString()));
          pago.appendChild(total);

          Element plazo = doc.createElement("plazo");

          String strUnitTime = "";
          if (invoice.getPaymentTerms().getOverduePaymentDaysRule() > 0) {

            double dblplazo = invoice.getPaymentTerms().getOverduePaymentDaysRule();
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

        } else {

          pago2 = (doc.createElement("pago"));

          Element formaPago = doc.createElement("formaPago");

          String strCodePaymentMethod = invoice.getPaymentMethod().getEeiCodeEi() == null ? "ND"
              : invoice.getPaymentMethod().getEeiCodeEi();
          if (strCodePaymentMethod.equals("ND")) {
            throw new OBException("El método de Pago " + invoice.getPaymentMethod().getName()
                + " no esta configurado para el proceso de Facturación Electrónica");

          }
          formaPago.appendChild(doc.createTextNode(strCodePaymentMethod));
          pago2.appendChild(formaPago);

          Element total = doc.createElement("total");
          double dblpagos = invoice.getGrandTotalAmount().doubleValue();
          total.appendChild(doc.createTextNode(formateador.format(dblpagos).toString()));
          pago2.appendChild(total);

          Element plazo = doc.createElement("plazo");

          String strUnitTime = "";

          if (invoice.getPaymentTerms().getOverduePaymentDaysRule() > 0) {

            double dblplazo = invoice.getPaymentTerms().getOverduePaymentDaysRule();
            plazo.appendChild(doc.createTextNode(String.valueOf(dblplazo)));
            pago2.appendChild(plazo);

            Element unidadTiempo = doc.createElement("unidadTiempo");
            strUnitTime = "dias";
            unidadTiempo.appendChild(doc.createTextNode(strUnitTime));
            pago2.appendChild(unidadTiempo);
          } else if (invoice.getPaymentTerms().getOffsetMonthDue() > 0) {

            double dblplazo = invoice.getPaymentTerms().getOffsetMonthDue();
            plazo.appendChild(doc.createTextNode(String.valueOf(dblplazo)));
            pago2.appendChild(plazo);

            Element unidadTiempo = doc.createElement("unidadTiempo");

            strUnitTime = "meses";
            unidadTiempo.appendChild(doc.createTextNode(strUnitTime));
            pago2.appendChild(unidadTiempo);
          }

          pagos2.appendChild(pago2);
        }

      }

      if (!bgbGranTotalInvoice.equals(bgdGrandTotalPayment)) {

        if (!strPaymentMethod.equals(invoice.getPaymentMethod().getId())) {
          infoNotaDebito.appendChild(pagos);
        } else {
          infoNotaDebito.appendChild(pagos2);
        }
      } else if (bgbGranTotalInvoice.equals(bgdGrandTotalPayment)) {
        infoNotaDebito.appendChild(pagos);

      }

    } else {

      Element pagos = doc.createElement("pagos");

      Element pago = doc.createElement("pago");

      Element formaPago = doc.createElement("formaPago");
      String strCodePaymentMethod = invoice.getPaymentMethod().getEeiCodeEi() == null ? "ND"
          : invoice.getPaymentMethod().getEeiCodeEi();
      if (strCodePaymentMethod.equals("ND")) {
        throw new OBException("El método de Pago " + invoice.getPaymentMethod().getName()
            + " no esta configurado para el proceso de Facturación Electrónica");

      }
      formaPago.appendChild(doc.createTextNode(strCodePaymentMethod));
      pago.appendChild(formaPago);

      Element total = doc.createElement("total");
      double dblpagos = invoice.getGrandTotalAmount().doubleValue();
      total.appendChild(doc.createTextNode(formateador.format(dblpagos).toString()));
      pago.appendChild(total);

      Element plazo = doc.createElement("plazo");

      String strUnitTime = "";
      if (invoice.getPaymentTerms().getOverduePaymentDaysRule() > 0) {

        double dblplazo = invoice.getPaymentTerms().getOverduePaymentDaysRule();
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

      infoNotaDebito.appendChild(pagos);

    }

    /** FIN FORMAS DE PAGOS - CC **/

    // *************************TERMINA PAGOS

    /**
     * Fin Nodo Info Nota Débito
     *******************************************************************************************************/

    // motivos
    Element motivos = doc.createElement("motivos");
    rootElement.appendChild(motivos);

    // motivo
    String strMotivo = truncate(invoice.getDescription(), 300);

    if (strMotivo == null || strMotivo.equals("")) {
      throw new OBException(
          "La Nota de Débito debe tener una descripción válida. (Motivo de Devolución)");
    }
    strMotivo = strMotivo.replaceAll("[\n]", " ");
    Element motivo = doc.createElement("motivo");

    Element razon = doc.createElement("razon");
    Element valor = doc.createElement("valor");

    razon.appendChild(doc.createTextNode(strMotivo));
    valor.appendChild(doc.createTextNode(formateador.format(TotalSinImpuestos).toString()));

    motivo.appendChild(razon);
    motivo.appendChild(valor);

    motivos.appendChild(motivo);

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

  @Override
  public String sendFile(ConnectionProvider con, File file, Invoice invoice, String strLanguage)
      throws Exception {

    ECWSClient client = new ECWSClient();
    String res = null; // client.send(con, file, invoice, strLanguage);

    return res;
  }
}
