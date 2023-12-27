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

public class ArchTransferPayrollBankAustro extends DalBaseProcess {

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

      // Recupera los parametros de la sesión
      final String strDocumentno = (String) bundle.getParams().get("documentno");
      final String strBanktransfer = (String) bundle.getParams().get("ssfiBanktransferId");      
		
      // Obtener los datos de la Boleta de Nomina
      ArchTransferPayrollBankAustroData data[] = ArchTransferPayrollBankAustroData.select(conn,
          strDocumentno,strBanktransfer);
      if (data != null && data.length > 0) {
        bundle.setResult(message);

        // Prepar browser to receive file
        // Preparar el navegador para recibir el archivo
        response.setCharacterEncoding("Cp1252");
        response.setContentType("application/txt");
        response.setHeader("Content-Disposition",
            "attachment; filename=TransferenciaNóminaBancodelAustro.txt");
        // Build txt file
        // Consrtuir el archivo txt
        PrintWriter out = response.getWriter();
        try {
          Integer secuencial = 1;
          String strNumeroCuenta = null;
          String entidad = String.format("%-45s",data[0].entidad);
          out.write(entidad);
          out.write("\t");
          String codigo = String.format("%-13s",data[0].codigo);
          out.write(codigo);
          String formapago = String.format("%-13s",data[0].formapago);
          out.write("\t");
          out.write(formapago);
          out.write("\t");
          out.write("\r\n");   // write new line
          for (ArchTransferPayrollBankAustroData archPAPProduBankData : data) {
            String empleado = String.format("%-45s", archPAPProduBankData.empleado);
            out.write(empleado);
            out.write("\t");
            out.write(String.format("%013d",Integer.parseInt(archPAPProduBankData.cedula)));
            out.write("\t");
            // // Para dar formato los números decimales
            DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
            simbolos.setDecimalSeparator('.');
            DecimalFormat formatter = new DecimalFormat("#########0.00", simbolos);
            BigDecimal bgdImpFormat = BigDecimal.ZERO;
            bgdImpFormat = new BigDecimal(String.valueOf(archPAPProduBankData.valor));
            String StrImportNew = formatter.format(bgdImpFormat).toString() == null ? "0.00"
                : formatter.format(bgdImpFormat);
            out.write(String.format("%015d",
                Integer.parseInt(StrImportNew.replaceAll("\\.", "").replaceAll(",", ""))));
            out.write("\t");
            out.write(archPAPProduBankData.cuenta);
            out.write("\t");
            out.write(archPAPProduBankData.tipocuenta);
            out.write("\t");            
            out.write("C");
            out.write("\r\n");
          }
          // Enviar el archivo al navegador
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
