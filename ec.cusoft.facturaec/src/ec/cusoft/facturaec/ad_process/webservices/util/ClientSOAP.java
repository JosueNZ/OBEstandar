package ec.cusoft.facturaec.ad_process.webservices.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.service.db.DalConnectionProvider;

import ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_PortType;
import ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_ServiceLocator;

public class ClientSOAP {
  private static final Logger log4j = Logger.getLogger(ClientSOAP.class);

  public String sendInvoice(String xml, String idInv, Invoice invoice, ShipmentInOut inout,
      InternalMovement movement, String strIdRemissionGuideTable, String strURLWSOffline2,
      String strCorreoPorDefecto, int intTimeOutPara) {

    String response = null;
    String strURLWSOffline = null;
    if (strURLWSOffline2 == null) {
      try {
        strURLWSOffline = SelectParams(1);

        log4j.debug("URL WS: " + strURLWSOffline);

        if (strURLWSOffline.length() == 0 || strURLWSOffline == null
            || strURLWSOffline.equals("")) {
          throw new OBException(
              "No se encontró parametrización de tipo de procesamiento en lote en documentos electrónicos.");
        }
      } catch (Exception e) {
        throw new OBException(
            "Error al obtener la parametrización de Facturación Electrónica. " + e.getMessage());
      }
    } else {
      strURLWSOffline = strURLWSOffline2;
    }

    try {

      WSRecepcion_ServiceLocator wsLocator = new WSRecepcion_ServiceLocator();
      wsLocator.setUrl(strURLWSOffline);
      WSRecepcion_PortType wsRecepcionPort = wsLocator.getWSRecepcionPort();

      String dataInvoice[] = new String[6];
      if (invoice != null) {
        dataInvoice = getDataInv(idInv, invoice, strCorreoPorDefecto);
      } else {
        dataInvoice = getDataRemissionGuide(idInv, inout, movement, strIdRemissionGuideTable,
            strCorreoPorDefecto);
      }

      String direccion = dataInvoice[0];
      String telefono = dataInvoice[1];
      String email = dataInvoice[2];
      String subTotal0 = dataInvoice[3];
      String subTotal12 = dataInvoice[4];
      String iva12 = dataInvoice[5];
      String subTotalExcento = dataInvoice[6];
      String subTotalNoIVA = dataInvoice[7];

      log4j.debug("XML Generado ---> " + xml);

      response = wsRecepcionPort.generar(xml, subTotal12, subTotal0, subTotalNoIVA, subTotalExcento,
          iva12, idInv, direccion, telefono, email, intTimeOutPara);

    } catch (Exception e) {
      throw new OBException(e.getMessage());
    }

    return response;
  }

  public static String[] getDataInv(String idInv, Invoice invoice, String strCorreoPorDefecto) {
    String dataInvoice[] = new String[8];
    try {

      String subTotal[] = getSubtotales(invoice);
      dataInvoice[0] = getAddressComplete(invoice.getPartnerAddress());
      dataInvoice[1] = (invoice.getPartnerAddress().getPhone() == null ? "-"
          : invoice.getPartnerAddress().getPhone());

      String strCorreo = "nulo";

      if (invoice.isSalesTransaction()) {
        strCorreo = invoice.getEeiEmailTo() == null
            ? StringUtils.isBlank(invoice.getBusinessPartner().getEEIEmail()) ? "nulo"
                : invoice.getBusinessPartner().getEEIEmail().replaceAll(";;;", ";").replaceAll(";;",
                    ";")
            : invoice.getEeiEmailTo().replaceAll(";;;", ";").replaceAll(";;", ";");
      } else {
        strCorreo = (StringUtils.isBlank(invoice.getBusinessPartner().getEEIPOEmail()) ? "nulo"
            : invoice.getBusinessPartner().getEEIPOEmail().replaceAll(";;;", ";").replaceAll(";;",
                ";"));
      }

      // (invoice.getBusinessPartner().getEEIEmail() == null ? "nulo" : invoice
      // .getBusinessPartner().getEEIEmail().replaceAll(";;;", ";").replaceAll(";;", ";"));

      if (strCorreo.equals("nulo")) {
        if (strCorreoPorDefecto == null) {
          dataInvoice[2] = SelectParams(2);
        } else {
          dataInvoice[2] = strCorreoPorDefecto;
        }
      } else {
        dataInvoice[2] = strCorreo;
      }
      dataInvoice[3] = subTotal[0];
      dataInvoice[4] = subTotal[1];
      dataInvoice[5] = subTotal[2];
      dataInvoice[6] = subTotal[3];
      dataInvoice[7] = subTotal[4];

    } catch (Exception e) {
      throw new OBException(
          "Error al obtener parámetros para enviar a WS (dirección,teléfono,correo electrónico o subtotales)"
              + e);
    }

    return dataInvoice;
  }

  public static String[] getDataRemissionGuide(String idInout, ShipmentInOut inout,
      InternalMovement movement, String strIdRemissionGuideTable, String strCorreoPorDefecto) {
    String dataInvoice[] = new String[8];
    try {

      String strCorreo = null;
      if (inout != null) {
        dataInvoice[0] = getAddressComplete(inout.getPartnerAddress());

        dataInvoice[1] = (inout.getPartnerAddress().getPhone() == null ? "-"
            : inout.getPartnerAddress().getPhone());
        strCorreo = (inout.getBusinessPartner().getEEIEmail() == null ? "nulo"
            : inout.getBusinessPartner().getEEIEmail().replaceAll(";;;", ";").replaceAll(";;",
                ";"));
      } else if (movement != null) {
        String[] strPartnerData = new String[3];
        strPartnerData = SelectMovementData(movement.getOrganization().getId().toString());

        strCorreo = (strPartnerData[0] == null ? "nulo"
            : strPartnerData[0].replaceAll(";;;", ";").replaceAll(";;", ";"));

        // DIRECCIÓN
        dataInvoice[0] = getAddressComplete(
            OBDal.getInstance().get(Location.class, strPartnerData[1]));
        // TELÉFONO
        dataInvoice[1] = (strPartnerData[2] == null ? "-" : strPartnerData[2]);
        // CORREO

      } else {
        throw new OBException();
      }

      if (strCorreo.equals("nulo")) {
        if (strCorreoPorDefecto == null) {
          dataInvoice[2] = SelectParams(2);
        } else {
          dataInvoice[2] = strCorreoPorDefecto;
        }
      } else {
        dataInvoice[2] = strCorreo;
      }
      dataInvoice[3] = "0";
      dataInvoice[4] = "0";
      dataInvoice[5] = strIdRemissionGuideTable; // SE ENVÍA ID DE LA
      // TABLA DESDE LA QUE SE
      // GENERA
      // LA GUÍA DE REMISIÓN
      dataInvoice[6] = "0";
      dataInvoice[7] = "0";

    } catch (Exception e) {

      throw new OBException(

          "Error al obtener parámetros para enviar a WS (dirección,teléfono,correo electrónico o subtotales)"
              + e.getMessage());
    }

    return dataInvoice;
  }

  public static String[] getSubtotales(Invoice invOb) {
    String[] subTotales = new String[5];
    ConnectionProvider conn = new DalConnectionProvider(false);
    try {
      String strSql = " SELECT * FROM ssfi_bases_v where c_invoice_id= ?";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      st.setString(1, invOb.getId());
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {

        subTotales[0] = String
            .valueOf(Math.abs(Double.parseDouble(rsConsulta.getString("basecero"))));
        subTotales[1] = String
            .valueOf(Math.abs(Double.parseDouble(rsConsulta.getString("base12"))));
        subTotales[3] = String
            .valueOf(Math.abs(Double.parseDouble(rsConsulta.getString("base_excento"))));
        subTotales[4] = String
            .valueOf(Math.abs(Double.parseDouble(rsConsulta.getString("base_no_iva"))));
      }
      subTotales[2] = getIva(invOb);

      return subTotales;

    } catch (Exception e) {

      throw new OBException("Error al consultar vista de subtotales ssfi_bases_v" + e.getMessage());
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static String getIva(Invoice invOb) {
    double dblIva = 0.00;
    ConnectionProvider conn = new DalConnectionProvider(false);
    try {
      String strSql = "SELECT coalesce(il.taxamt,0) as iva12 FROM  c_invoicetax il "
          + "INNER JOIN c_tax ct on ct.c_tax_id = il.c_tax_id WHERE "
          + "ct.istaxdeductable = 'Y' AND ct.rate <> 0 AND il.c_invoice_id= ? ";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(strSql);
      st.setString(1, invOb.getId());
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        dblIva = rsConsulta.getDouble("iva12");
      }

      return String.valueOf(Math.abs(dblIva));

    } catch (Exception e) {

      throw new OBException("Error al consultar la tabla c_invoicetax. " + e.getMessage());
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static String SelectParams(int intParameter) {
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {
      String strSql = "SELECT url_ws_validacion, default_email, keyaccess_generation, extract_client_description  FROM eei_param_facturae where isactive='Y' and url_ws_validacion is not null";
      PreparedStatement st = null;
      String strParametro = null;
      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      int contador = 0;
      while (rsConsulta.next()) {
        contador = contador + 1;
        if (intParameter == 1) {
          strParametro = rsConsulta.getString("url_ws_validacion");
        } else if (intParameter == 2) {
          strParametro = rsConsulta.getString("default_email");
        } else if (intParameter == 3) {
          strParametro = rsConsulta.getString("keyaccess_generation");
        } else if (intParameter == 4) {
          strParametro = rsConsulta.getString("extract_client_description");
        }

      }
      if (contador == 0) {
        throw new OBException(
            "No se encontró parametrización de url de webservice en documentos electrónicos.");
      } else if (contador > 1) {

        throw new OBException(
            "Existe más de una parametrización activa de documentos electrónicos.");
      }
      return strParametro;
    } catch (Exception e) {

      throw new OBException("Error al consultar la tabla eei_param_facturae (Parámetro WS) " + e);
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static String[] SelectMovementData(String strOrganizationId) {
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {
      String strSql = "SELECT AO.C_BPARTNER_ID AS PARTNERID FROM M_MOVEMENT MM INNER JOIN AD_ORGINFO AO ON MM.AD_ORG_ID = AO.AD_ORG_ID WHERE AO.ISACTIVE='Y' AND MM.AD_ORG_ID = ? ORDER BY AO.CREATED DESC";
      PreparedStatement st = null;
      String strParametro = null;
      st = conn.getPreparedStatement(strSql);
      st.setString(1, strOrganizationId);
      ResultSet rsConsulta = st.executeQuery();
      int contador = 0;
      while (rsConsulta.next()) {
        contador = contador + 1;
        strParametro = rsConsulta.getString("PARTNERID");
        break;
      }
      if (contador == 0) {
        throw new OBException("No se encontró un tercero atado a la organización. ");
      }

      String[] strPartnerData = new String[3];
      strSql = "SELECT CP.EM_EEI_EMAIL AS EMAIL ,CL.C_BPARTNER_LOCATION_ID,CL.PHONE AS TELEFONO FROM C_BPARTNER CP INNER JOIN C_BPARTNER_LOCATION CL ON CP.C_BPARTNER_ID =CL.C_BPARTNER_ID WHERE CP.C_BPARTNER_ID= ?";
      st = conn.getPreparedStatement(strSql);
      st.setString(1, strParametro);
      ResultSet rsConsulta2 = st.executeQuery();
      int contador2 = 0;
      while (rsConsulta2.next()) {
        contador2 = contador2 + 1;
        strPartnerData[0] = rsConsulta2.getString("EMAIL");
        strPartnerData[1] = rsConsulta2.getString("C_BPARTNER_LOCATION_ID");
        strPartnerData[2] = rsConsulta2.getString("TELEFONO");
      }

      return strPartnerData;
    } catch (Exception e) {

      throw new OBException(
          "Error al consultar información del tercero atado a la organización. " + e);
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static String getAddressComplete(Location objBPLocation) {

    try {

      org.openbravo.model.common.geography.Location objLocation = objBPLocation
          .getLocationAddress();
      String strAddressComplete = "";
      strAddressComplete = (objLocation.getAddressLine1() == null ? " "
          : objLocation.getAddressLine1()) + "-"
          + (objLocation.getAddressLine2() == null ? " " : objLocation.getAddressLine2()) + "-"
          + (objLocation.getPostalCode() == null ? " " : objLocation.getPostalCode()) + "-"
          + (objLocation.getCityName() == null ? " " : objLocation.getCityName()) + "-"
          + (objLocation.getRegion() == null ? " " : objLocation.getRegion().getName()) + "-"
          + (objLocation.getCountry() == null ? " " : objLocation.getCountry().getName());

      return strAddressComplete;
    } catch (Exception ex) {

      throw new OBException(
          "Error al consultar información de dirección del tercero (Envío WS). " + ex.getMessage());
    }

  }
}
