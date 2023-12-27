package com.sidesoft.hrm.payroll.early.payment.ad_process;

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

public class ArchivePaymentFortnightTXT extends DalBaseProcess {

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
      final String strTypeIncome = (String) bundle.getParams().get("typeincome");

      // Get the Payroll Ticket data
      // Obtener los datos de la Boleta de Nomina
      ArchivePaymentFortnightTXTData data[] = ArchivePaymentFortnightTXTData.select(conn,
          strDocumentno);
      if (data != null && data.length > 0) {
        bundle.setResult(message);

        // Prepar browser to receive file
        // Preparar el navegador para recibir el archivo
        response.setCharacterEncoding("Cp1252");
        response.setContentType("application/txt");
        response
            .setHeader("Content-Disposition", "attachment; filename=PayFortnightProdubanco.txt");
        // Build txt file
        // Consrtuir el archivo txt
        PrintWriter out = response.getWriter();
        try {
          // out.println((Utility.fileToString(file.getAbsolutePath())));

          // final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy h:mm:ss");
          // final Date date = new Date();
          Integer secuencial = 1;
          String strNumeroCuenta = null;
          for (ArchivePaymentFortnightTXTData archPayFortnightxtData : data) {
            out.write(archPayFortnightxtData.codigoorientacion);
            out.write("\t");
            out.write(archPayFortnightxtData.cuentaempresa);
            out.write("\t");
            out.write(archPayFortnightxtData.secuencialpago);
            out.write("\t");
            out.write(archPayFortnightxtData.comprobantepago);
            out.write("\t");
            out.write(archPayFortnightxtData.contrapartida);
            out.write("\t");
            out.write(archPayFortnightxtData.moneda);
            out.write("\t");

            // Para dar formato los números decimales
            DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
            simbolos.setDecimalSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#########0.00", simbolos);
            BigDecimal bgdImpFormat = BigDecimal.ZERO;
            bgdImpFormat = new BigDecimal(String.valueOf(archPayFortnightxtData.valor));

            String StrImportNew = formatter.format(bgdImpFormat).toString() == null ? "0.00"
                : formatter.format(bgdImpFormat);
            out.write(String.format("%013d",
                Integer.parseInt(StrImportNew.replaceAll("\\.", "").replaceAll(",", ""))));

            out.write("\t");
            out.write(archPayFortnightxtData.formapago);
            out.write("\t");
            out.write(archPayFortnightxtData.codigoinstfin);
            out.write("\t");
            out.write(archPayFortnightxtData.tipocuenta);
            out.write("\t");
            if (archPayFortnightxtData.codigoinstfin.toString().equals("BP")) {
              strNumeroCuenta = String.format("%011d",
                  Integer.parseInt(archPayFortnightxtData.numerocuenta));
            } else {
              strNumeroCuenta = archPayFortnightxtData.numerocuenta;
            }
            out.write(strNumeroCuenta);
            out.write("\t");
            out.write(archPayFortnightxtData.tipoidbeneficiario);
            out.write("\t");
            out.write(archPayFortnightxtData.idbeneficiario);
            out.write("\t");
            out.write(archPayFortnightxtData.nombrebenef);
            out.write("\t");
            out.write(archPayFortnightxtData.direccionbenef);
            out.write("\t");
            out.write(archPayFortnightxtData.ciudadbenef);
            out.write("\t");
            out.write(archPayFortnightxtData.telefonobenef);
            out.write("\t");
            out.write(archPayFortnightxtData.localidadpago);
            out.write("\t");
            out.write(archPayFortnightxtData.referencia);
            out.write("\t");
            out.write(archPayFortnightxtData.refadicional);
            out.write("\t");
            out.println();
          }
          // Send file to browser
          // Enviar el archivo al navegador
          out.close();

          // TODO: Restore previous headers
          // if (!headers.isEmpty()) {
          // response.setHeader(headers.get(0).nextElement().toString(), headers.get(0)
          // .nextElement().toString());
          // }
          // for (int i = 1; i < headers.size(); i++) {
          // response.addHeader(headers.get(i), headers.get(i)
          // .nextElement().toString());
          // }
          // response.setCharacterEncoding(oldCharacterEncoding);
          // response.setContentType(oldContentType);

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
