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

public class ArchivePaymentProdubancoBankTXT extends DalBaseProcess {

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
      ArchivePaymentProdubancoBankData data[] = ArchivePaymentProdubancoBankData.select(conn,
          strDocumentno);
      if (data != null && data.length > 0) {
        bundle.setResult(message);

        // Prepar browser to receive file
        // Preparar el navegador para recibir el archivo
        response.setCharacterEncoding("Cp1252");
        response.setContentType("application/txt");
        response.setHeader("Content-Disposition",
            "attachment; filename=PayUtilitiesProdubancoBank.txt");
        // Build txt file
        // Consrtuir el archivo txt
        PrintWriter out = response.getWriter();
        try {
          // out.println((Utility.fileToString(file.getAbsolutePath())));

          // final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy h:mm:ss");
          // final Date date = new Date();
          Integer secuencial = 1;
          String strNumeroCuenta = null;
          for (ArchivePaymentProdubancoBankData archPAPProduBankData : data) {
            out.write(archPAPProduBankData.codigoempresa);
            out.write("\t");
            out.write(archPAPProduBankData.cuentaempresa);
            out.write("\t");
            out.write(archPAPProduBankData.secuencialpago);
            out.write("\t");
            out.write(archPAPProduBankData.comprobantepago);
            out.write("\t");
            out.write(archPAPProduBankData.documentno);
            out.write("\t");
            out.write(archPAPProduBankData.moneda);
            out.write("\t");
            // Para dar formato los números decimales
            DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
            simbolos.setDecimalSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#########0.00", simbolos);
            BigDecimal bgdImpFormat = BigDecimal.ZERO;
            bgdImpFormat = new BigDecimal(String.valueOf(archPAPProduBankData.valor));

            String StrImportNew = formatter.format(bgdImpFormat).toString() == null ? "0.00"
                : formatter.format(bgdImpFormat);
            out.write(String.format("%013d",
                Integer.parseInt(StrImportNew.replaceAll("\\.", "").replaceAll(",", ""))));
            out.write("\t");
            out.write(archPAPProduBankData.formapago);
            out.write("\t");
            out.write(archPAPProduBankData.codigoinstfin);
            out.write("\t");
            out.write(archPAPProduBankData.tipocuenta);
            out.write("\t");
            if (archPAPProduBankData.codigoinstfin.toString().equals("BP")) {
              strNumeroCuenta = String.format("%011d",
                  Integer.parseInt(archPAPProduBankData.numerocuenta));
            } else {
              strNumeroCuenta = archPAPProduBankData.numerocuenta;
            }
            out.write(strNumeroCuenta);
            out.write("\t");
            out.write(archPAPProduBankData.tipoidbeneficiario);
            out.write("\t");
            out.write(archPAPProduBankData.idbeneficiario);
            out.write("\t");
            out.write(archPAPProduBankData.nombrebenef);
            out.write("\t");
            out.write(archPAPProduBankData.direccionbenef);
            out.write("\t");
            out.write(archPAPProduBankData.ciudadbenef);
            out.write("\t");
            out.write(archPAPProduBankData.telefonobenef);
            out.write("\t");
            out.write(archPAPProduBankData.localidadpago);
            out.write("\t");
            out.write(archPAPProduBankData.referencia);
            out.write("\t");
            out.write(archPAPProduBankData.refadicional);
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
