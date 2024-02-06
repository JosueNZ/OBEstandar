/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2001-2010 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package ec.com.sidesoft.localization.importdata.loadvouchers.ad_callouts;

import java.io.StringReader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.service.db.DalConnectionProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvPartner;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvPurchaseIinvoice;
import ec.gob.sri.comprobantes.ws.aut.Autorizacion;

public class GetDataAuthPurchase extends SimpleCallout {

  private static final long serialVersionUID = 1653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String codeAuthorization = info.getStringParameter("inpwithholdingauthorization", null);
    if (codeAuthorization != null && !codeAuthorization.isEmpty()) {
      String strURLAuthorization = null, strNumeroComprobantes = null, strmsg = null;

      OBCriteria<Organization> obcParameters = OBDal.getInstance()
          .createCriteria(Organization.class);
      obcParameters.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
      OrganizationType objOrgType = OBDal.getInstance().get(OrganizationType.class, "1");
      obcParameters.add(Restrictions.eq(Organization.PROPERTY_ORGANIZATIONTYPE, objOrgType));

      if (obcParameters.list().size() == 0) {
        strmsg = "No existe una organización legal contable activa.";
        info.addResult("ERROR", strmsg);
        return;
      }

      Organization objParameter = OBDal.getInstance().get(Organization.class,
          obcParameters.list().get(0).getId());

      if (objParameter.getSsalpEnvironment().equals("1")) {
        strURLAuthorization = objParameter.getSsalpUrlAuthoTest();
      } else {
        strURLAuthorization = objParameter.getSsalpUrlAuthoProd();
      }

      OBCriteria<ImdlvPurchaseIinvoice> docParameters = OBDal.getInstance()
          .createCriteria(ImdlvPurchaseIinvoice.class);
      docParameters.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_TYPETRX, "PL"));

      if (docParameters.list().size() == 0) {
        strmsg = "No existe una configuración para la factura de liquidación.";
        info.addResult("ERROR", strmsg);
        return;
      }
      ImdlvPurchaseIinvoice docDefault = OBDal.getInstance().get(ImdlvPurchaseIinvoice.class,
          docParameters.list().get(0).getId());

      ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante wsAnswer = null;
      try {
        ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesOfflineService service = new ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesOfflineService(
            new URL(strURLAuthorization));
        ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesOffline pt = service
            .getAutorizacionComprobantesOfflinePort();
        wsAnswer = pt.autorizacionComprobante(codeAuthorization);
        strNumeroComprobantes = wsAnswer.getNumeroComprobantes();
      } catch (Exception e) {
        strmsg = "Error al consultar clave de acceso en servicios del SRI. " + e.getMessage();
        info.addResult("ERROR", strmsg);
        return;
      }

      strNumeroComprobantes = (strNumeroComprobantes == null ? "0" : strNumeroComprobantes);

      if (!strNumeroComprobantes.equals("0")) {
        Autorizacion objAutorizaciones = wsAnswer.getAutorizaciones().getAutorizacion().get(0);
        String strStatus = objAutorizaciones.getEstado();
        strStatus = (strStatus == null ? "" : strStatus);

        if (strStatus.equals("AUTORIZADO")) {
          String strTipoDocumento = null, strIdentificacion = null, strFechaEmision = null,
              strEstablecimiento = null, strSecuencial = null, strPuntoEmision = null,
              strNumeroDocumento = null, strPartnerName = null, strnameProve = null,
              strIdentificacionComprador = null, strAddresProv = null,
              strtipoIdentificacionComprador = null;
          BigDecimal base = BigDecimal.ZERO, base12 = BigDecimal.ZERO;
          try {

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document documento = builder
                .parse(new InputSource(new StringReader(objAutorizaciones.getComprobante())));

            NodeList ndInfoTributaria = documento.getElementsByTagName("infoTributaria");
            Element elmInfoTributaria = (Element) ndInfoTributaria.item(0);

            strTipoDocumento = elmInfoTributaria.getElementsByTagName("codDoc").item(0)
                .getFirstChild().getTextContent();
            if (!strTipoDocumento.equals("01")) {
              strmsg = "La clave de acceso consultada no corresponde a una factura de venta.";
              info.addResult("ERROR", strmsg);
              return;
            }

            // IDENTIFICACIÓN
            strIdentificacion = elmInfoTributaria.getElementsByTagName("ruc").item(0)
                .getFirstChild().getTextContent();

            NodeList infoFactura = documento.getElementsByTagName("infoFactura");
            Element elmInfoFactura = (Element) infoFactura.item(0);

            // PARTNER (PROVIDER) DATA
            strnameProve = elmInfoTributaria.getElementsByTagName("razonSocial").item(0)
                .getFirstChild().getTextContent();
            strIdentificacionComprador = elmInfoTributaria.getElementsByTagName("ruc").item(0)
                .getFirstChild().getTextContent();
            strAddresProv = elmInfoTributaria.getElementsByTagName("dirMatriz").item(0)
                .getFirstChild().getTextContent();
            strtipoIdentificacionComprador = "04"; // elmInfoFactura
            // .getElementsByTagName("tipoIdentificacionComprador").item(0).getFirstChild()
            // .getTextContent();

            // FECHA DE EMISIÓN
            strFechaEmision = elmInfoFactura.getElementsByTagName("fechaEmision").item(0)
                .getFirstChild().getTextContent();

            // NÚMERO DE DOCUMENTO

            strEstablecimiento = elmInfoTributaria.getElementsByTagName("estab").item(0)
                .getFirstChild().getTextContent();
            strPuntoEmision = elmInfoTributaria.getElementsByTagName("ptoEmi").item(0)
                .getFirstChild().getTextContent();
            strSecuencial = elmInfoTributaria.getElementsByTagName("secuencial").item(0)
                .getFirstChild().getTextContent();

            strNumeroDocumento = strEstablecimiento + "-" + strPuntoEmision + "-" + strSecuencial;

            NodeList infoDetalle = documento.getElementsByTagName("detalles");
            for (int i = 0; i < infoDetalle.getLength(); i++) {
              Node ndetalles = infoDetalle.item(i);
              if (ndetalles.getNodeType() == Node.ELEMENT_NODE) {
                NodeList infoDetalleline = ndetalles.getChildNodes();
                for (int j = 0; j < infoDetalleline.getLength(); j++) {
                  Node ndetalleline = infoDetalleline.item(j);
                  if (ndetalleline.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) ndetalleline;
                    String rate = eElement.getElementsByTagName("tarifa").item(0).getTextContent();
                    String baseamount = eElement.getElementsByTagName("baseImponible").item(0)
                        .getTextContent();
                    BigDecimal rate_value = new BigDecimal(rate);
                    if (rate_value.compareTo(new BigDecimal("12")) == 0) {
                      base12 = base12.add(new BigDecimal(baseamount));
                    } else if (rate_value.compareTo(BigDecimal.ZERO) == 0) {
                      base = base.add(new BigDecimal(baseamount));
                    }
                  }
                }
              }
            }
          } catch (Exception e) {
            strmsg = "Error al leer archivo XML de número de autorización consultado. "
                + e.getMessage();
            info.addResult("ERROR", strmsg);
            return;
          }

          try {
            OBCriteria<BusinessPartner> obcBusinessPartner = OBDal.getInstance()
                .createCriteria(BusinessPartner.class);
            obcBusinessPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
            obcBusinessPartner
                .add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, strIdentificacionComprador));
            if (obcBusinessPartner.list().size() == 0) {
              // No existe un proveedor
              // CREAR EL PROVEEDOR CON RESPECTO A LA FACTURA
              String strClientID = OBContext.getOBContext().getCurrentClient().getId();
              String strOrgID = OBContext.getOBContext().getCurrentOrganization().getId();
              String strUserID = OBContext.getOBContext().getUser().getId();

              Object[] objMsgLines = InsertPartner(strOrgID, strClientID, strUserID,
                  strIdentificacionComprador, strnameProve, strAddresProv,
                  strtipoIdentificacionComprador);
              strmsg = objMsgLines[1].toString();
              if (strmsg.equals("Error")) {
                info.addResult("ERROR", strmsg);
                return;
              }
              if (strmsg.equals("Sucess")) {
                process_callout(info, strIdentificacionComprador, strEstablecimiento,
                    strPuntoEmision, strSecuencial, strFechaEmision, strNumeroDocumento, base,
                    base12, docDefault);
              }

            } else if (obcBusinessPartner.list().size() > 0 && strFechaEmision != null
                && strNumeroDocumento != null) {

              BusinessPartner objBusinessPartner = obcBusinessPartner.list().get(0);
              info.addResult("inpcBpartnerId", objBusinessPartner.getId());
              SimpleDateFormat objFormato = new SimpleDateFormat("dd/MM/yyyy");
              Date dtFechaEmision = objFormato.parse(strFechaEmision);
              info.addResult("inpdateemission", dtFechaEmision);
              info.addResult("inpstablishment", strEstablecimiento);
              info.addResult("inpshell", strPuntoEmision);
              info.addResult("inpporeference", strSecuencial);
              info.addResult("SUCCESS",
                  "Datos de compra cargados: <b>Tercero:</b> " + objBusinessPartner.getName()
                      + " - <b>Fecha de Emisión:</b> " + strFechaEmision
                      + " - <b>Número de documento:</b> " + strNumeroDocumento);
              if (docDefault.getSswhCodelivelihoodt() != null) {
                info.addResult("inpsswhCodelivelihoodtId",
                    docDefault.getSswhCodelivelihoodt().getId());
              }
              if (docDefault.getSswhLivelihoodt() != null) {
                info.addResult("inpsswhLivelihoodtId", docDefault.getSswhLivelihoodt().getId());
              }
              info.addResult("inpuntaxedBasis", base);
              info.addResult("inptaxbaserefund", base12);

              /*
               * } else if (obcBusinessPartner.list().size() == 0) { strmsg =
               * "No existe un proveedor (<b>registrado/activo</b>) en Openbravo con identificación: <b>"
               * + strIdentificacion + "</b>"; info.addResult("ERROR", strmsg); return;
               */
            } else if (strFechaEmision == null) {
              strmsg = "Fecha de emisión nula.";
              info.addResult("ERROR", strmsg);
              return;
            } else if (strNumeroDocumento == null) {
              strmsg = "Número de documento nulo.";
              info.addResult("ERROR", strmsg);
              return;
            }

          } catch (Exception e) {
            strmsg = "Error al insertar información de factura consultada. " + e.getMessage();
            info.addResult("ERROR", strmsg);
            return;
          }

        } else {
          strmsg = "Número de autorización consultada en estado: <b>" + strStatus + "</b>";
          info.addResult("ERROR", strmsg);
          return;
        }

      } else {
        strmsg = "Número de autorización consultada no encontrada.";
        info.addResult("ERROR", strmsg);
        return;
      }
    }
  }

  public void process_callout(CalloutInfo info, String strIdentificacionComprador,
      String strEstablecimiento, String strPuntoEmision, String strSecuencial,
      String strFechaEmision, String strNumeroDocumento, BigDecimal base, BigDecimal base12,
      ImdlvPurchaseIinvoice docDefault) throws ParseException {
    OBCriteria<BusinessPartner> obcBusinessPartner = OBDal.getInstance()
        .createCriteria(BusinessPartner.class);
    obcBusinessPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
    // obcBusinessPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_VENDOR, true));
    obcBusinessPartner
        .add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, strIdentificacionComprador));

    BusinessPartner objBusinessPartner = obcBusinessPartner.list().get(0);
    info.addResult("inpcBpartnerId", objBusinessPartner.getId());
    SimpleDateFormat objFormato = new SimpleDateFormat("dd/MM/yyyy");
    Date dtFechaEmision = objFormato.parse(strFechaEmision);
    info.addResult("inpdateemission", dtFechaEmision);
    info.addResult("inpstablishment", strEstablecimiento);
    info.addResult("inpshell", strPuntoEmision);
    info.addResult("inpporeference", strSecuencial);
    info.addResult("SUCCESS",
        "Datos de compra cargados: <b>Tercero:</b> " + objBusinessPartner.getName()
            + " - <b>Fecha de Emisión:</b> " + strFechaEmision + " - <b>Número de documento:</b> "
            + strNumeroDocumento);
    if (docDefault.getSswhCodelivelihoodt() != null) {
      info.addResult("inpsswhCodelivelihoodtId", docDefault.getSswhCodelivelihoodt().getId());
    }
    if (docDefault.getSswhLivelihoodt() != null) {
      info.addResult("inpsswhLivelihoodtId", docDefault.getSswhLivelihoodt().getId());
    }
    info.addResult("inpuntaxedBasis", base);
    info.addResult("inptaxbaserefund", base12);

    /*
     * } else if (obcBusinessPartner.list().size() == 0) { strmsg =
     * "No existe un proveedor (<b>registrado/activo</b>) en Openbravo con identificación: <b>" +
     * strIdentificacion + "</b>"; info.addResult("ERROR", strmsg); return;
     */

  }

  public Object[] InsertPartner(String strOrg, String strClientID, String strUserID, String strDni,
      String strName, String StrAddress, String strtipoIdentificacionComprador) {
    String strSql = "";

    String strAddress1 = "";
    String strAddress2 = "";
    Object[] strMsg = new Object[2];

    int countAddress = StrAddress.length();

    if (countAddress > 60) {
      strAddress1 = StrAddress.substring(0, 59);
    } else {
      strAddress1 = StrAddress.substring(0, countAddress);
    }

    // **valores por defecto creacion del tercero
    String strCurrencyID = "USD";
    // String StrDateAcctInvoice =StrDateInvoice;
    String strCurrency = "(select c_currency_id from c_currency where iso_code='" + strCurrencyID
        + "')";
    String strPaymentTermID = "";
    String strPriceListID = "";
    String strGroupPartner = "";

    String strFinPaymentMethodID = "";
    String strPaymentMethodXLS = "";
    String strSswhTaxpayer = "";

    OBCriteria<ImdlvPartner> obcDefaultPartner = OBDal.getInstance()
        .createCriteria(ImdlvPartner.class);
    obcDefaultPartner.add(Restrictions.eq(ImdlvPartner.PROPERTY_ACTIVE, true));

    if (obcDefaultPartner.list().size() > 0) {

      strPaymentTermID = obcDefaultPartner.list().get(0).getPaymentterm().getId();
      strPriceListID = obcDefaultPartner.list().get(0).getPricelist().getId();
      strGroupPartner = obcDefaultPartner.list().get(0).getBpGroup().getId();
      strFinPaymentMethodID = obcDefaultPartner.list().get(0).getFINPaymentmethod().getId();
      strCurrency = obcDefaultPartner.list().get(0).getCurrency().getId();
      strSswhTaxpayer = obcDefaultPartner.list().get(0).getSswhTaxpayer().getId();
    }

    // **

    strSql = strSql + "INSERT INTO c_bpartner("
        + "            c_bpartner_id, ad_client_id, ad_org_id, isactive, created, createdby,"
        + "            updated, updatedby, value, name, name2, description, issummary, "
        + "            c_bp_group_id, isonetime, isprospect, isvendor, iscustomer, isemployee, "
        + "            issalesrep,  ad_language, taxid, istaxexempt, "
        + "            acqusitioncost, potentiallifetimevalue, actuallifetimevalue, "
        + "            so_creditlimit, so_creditused,"
        + "            isdiscountprinted, po_pricelist_id, po_paymentterm_id, "
        + "            documentcopies, c_greeting_id, invoicerule, socreditstatus,  "
        + "            showpriceinorder, invoicegrouping, isworker,"
        + "            last_days, customer_blocking, vendor_blocking, "
        + "            so_payment_blocking, po_payment_blocking, so_invoice_blocking, "
        + "            po_invoice_blocking, so_order_blocking, po_order_blocking, so_goods_blocking, "
        + "            po_goods_blocking, iscashvat, update_currency, EM_Sswh_Taxidtype, EM_SSWH_Taxpayer_ID, PO_Paymentmethod_ID)"
        + "    VALUES (get_uuid(), '" + OBContext.getOBContext().getCurrentClient().getId() + "', '"
        + strOrg + "', 'Y', now(), " + "              '"
        + OBContext.getOBContext().getUser().getId() + "', " + "            now(), '"
        + OBContext.getOBContext().getUser().getId() + "' , " + "            '" + strDni + "','"
        + strName + "' , '" + strName + "', '', 'N', " + " '" + strGroupPartner + "' "
        + "           , 'N', 'N', 'Y', 'Y', 'N'," + "            'N',  'es_ES', '" + strDni
        + "', 'N', " + "            0, 0, 0, " + "            0, 0, "
        + "            'N', /*po_pricelist_id*/ '" + strPriceListID + "', '" + strPaymentTermID
        + "', " + "            null, null, 1, 0,  "
        + "            'Y', /*invoicegrouping*/ '000000000000000', 'N',"
        + "            1000, 'N', 'N'," + "            'N', 'Y', 'Y', "
        + "            'Y', 'Y', 'Y', 'Y', " + "            'N', 'N', 'N',"
        + "            CASE WHEN '" + strtipoIdentificacionComprador + "' = '04' THEN 'R' "
        + "                                      WHEN '" + strtipoIdentificacionComprador
        + "' = '05' THEN 'R'" + "                                      WHEN '"
        + strtipoIdentificacionComprador + "' = '06' THEN 'P'"
        + "                                      WHEN '" + strtipoIdentificacionComprador
        + "' = '07' THEN 'EEI_C' " + "                                      WHEN '"
        + strtipoIdentificacionComprador + "' = '08' THEN 'EEI_E'  "
        + "                                      END , '" + strSswhTaxpayer + "','"
        + strFinPaymentMethodID + "' );";
    System.out.println(strSql);
    strMsg = executeInserts(strSql);

    return strMsg;
  }

  public Object[] executeInserts(String strSQL) {

    Object[] objMsg = new Object[2];
    String returnString = "OK";
    try {
      // Se llama a la función para insertar en las líneas de detalle
      org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
      CallableStatement InsertLineStatement = cp.getConnection()
          .prepareCall("{call IMDLV_INSERTS(?)}"); // ORIGINAL
      InsertLineStatement.setString(1, strSQL);
      InsertLineStatement.execute();
      ResultSet rs = InsertLineStatement.getResultSet();
      while (rs.next()) {
        returnString = (rs.getString(1));
      }
      if (returnString.contains("ERROR")) {
        objMsg[0] = "ERROR";
        objMsg[1] = returnString;

      }
      if (returnString.contains("OK")) {
        objMsg[0] = "OK";
        objMsg[1] = "Sucess";
      }
      cp.getStatement().getConnection().commit();
      cp.getStatement().close();
      InsertLineStatement.close();
      cp.destroy();
      rs.close();
    } catch (Exception e) {
      objMsg[0] = "ERROR";
      objMsg[1] = returnString;
    }
    return objMsg;
  }

}
