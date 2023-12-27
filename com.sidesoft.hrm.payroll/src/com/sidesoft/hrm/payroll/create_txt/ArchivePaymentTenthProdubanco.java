package com.sidesoft.hrm.payroll.create_txt;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.xmlEngine.XmlEngine;

public class ArchivePaymentTenthProdubanco extends DalBaseProcess {

  public XmlEngine xmlEngine = null;
  public static String strDireccion;

  @SuppressWarnings({ "deprecation", "null" })
  public void doExecute(ProcessBundle bundle) throws Exception {

    final OBError message = new OBError();

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    // ConnectionProvider conn = new DalConnectionProvider(false);

    ConnectionProvider conn = bundle.getConnection();

    // VariablesSecureApp varsAux = bundle.getContext().toVars();
    HttpServletResponse response = RequestContext.get().getResponse();
    HttpServletRequest request = RequestContext.get().getRequest();

    try {

      // retrieve the parameters from the bundle
      // Recupera los parametros de la sesión

      final String strDocumentno = (String) bundle.getParams().get("documentno");

      // Get the Payroll Ticket data
      // Obtener los datos de la Boleta de Nomina
      ArchivePaymentTenthProdubancoData data[] = ArchivePaymentTenthProdubancoData.select(conn,
          strDocumentno);
      if (data != null && data.length > 0) {
        bundle.setResult(message);

        // Prepar browser to receive file
        // Preparar el navegador para recibir el archivo
        response.setCharacterEncoding("Cp1252");
        response.setContentType("application/txt");
        response.setHeader("Content-Disposition",
            "attachment; filename=ArchivePaymentTenthProdubanco.txt");
        // Build txt file
        // Consrtuir el archivo txt
        PrintWriter out = response.getWriter();
        try {

          Integer secuencial = 1;
          String strNumeroCuenta = null;
          for (ArchivePaymentTenthProdubancoData paytenth : data) {
            out.write(paytenth.pa);
            out.write("\t");
            out.write(paytenth.cuentacompania);
            out.write("\t");
            out.write(Integer.toString(secuencial));
            out.write("\t");
            out.write(paytenth.comprobantepago);
            out.write("\t");
            out.write(paytenth.documentno);
            out.write("\t");
            out.write(paytenth.usd);
            out.write("\t");
            // Para dar formato los números decimales
            DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
            simbolos.setDecimalSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#########0.00", simbolos);
            BigDecimal bgdImpFormat = BigDecimal.ZERO;
            bgdImpFormat = new BigDecimal(String.valueOf(paytenth.valor));

            String StrImportNew = formatter.format(bgdImpFormat).toString() == null ? "0.00"
                : formatter.format(bgdImpFormat);
            out.write(String.format("%013d",
                Integer.parseInt(StrImportNew.replaceAll("\\.", "").replaceAll(",", ""))));
            // out.write(paytenth.valor);
            out.write("\t");
            out.write(paytenth.formapago);
            out.write("\t");
            out.write(paytenth.codigobanco);
            out.write("\t");
            out.write(paytenth.tipocta);
            out.write("\t");
//            if (archPAPProduBankData.codigoinstfin.toString().equals("BP")) {
//              strNumeroCuenta = String.format("%011d",
//                  Integer.parseInt(archPAPProduBankData.numerocuenta));
//            } else {
//              strNumeroCuenta = archPAPProduBankData.numerocuenta;
//            }
            out.write(paytenth.nocuenta);
            out.write("\t");
            out.write(paytenth.tipoidempleado);
            out.write("\t");
            out.write(paytenth.cedula);
            out.write("\t");
            out.write(removeAcute(paytenth.nombreempleado));
            out.write("\t");
            out.write(paytenth.direccion);
            out.write("\t");
            out.write(paytenth.ciudad);
            out.write("\t");
            out.write(paytenth.telefono);
            out.write("\t");
            out.write(paytenth.localidad);
            out.write("\t");
            out.write(paytenth.referencia);
            out.write("\t");
            out.write(paytenth.referenciaadicional);
            out.write("\t");
            secuencial += 1;
            out.println();
          }
          out.close();

        } catch (final Exception e) {
          e.printStackTrace(System.err);
          message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
          message.setType("Error");
          message.setMessage(e.getMessage() + e.fillInStackTrace());
        } finally {
          bundle.setResult(message);
        }
      } else {
        message.setTitle(Utility.messageBD(conn, "Error", language));
        message.setType("Error");
        message.setMessage("No se encontró información en la consulta");
      }

    } finally {
      bundle.setResult(message);
    }
  }

  /**
   * Función que elimina acentos y caracteres especiales de una cadena de texto.
   * 
   * @param input
   * @return cadena de texto limpia de acentos y caracteres especiales.
   */
  public static String removeAcute(String input) {
    // Cadena de caracteres original a sustituir.
    String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
    // Cadena de caracteres ASCII que reemplazarán los originales.
    String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
    String output = input;
    for (int i = 0; i < original.length(); i++) {
      // Reemplazamos los caracteres especiales.
      output = output.replace(original.charAt(i), ascii.charAt(i));
    } // for i
    return output;
  }// removeAcute
}
