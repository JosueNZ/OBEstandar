package com.sidesoft.hrm.payroll.advanced.create_txt;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.xmlEngine.XmlEngine;

public class ArchPayTenthBankPichinchaTXT extends DalBaseProcess {

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
      final String cyearid = (String) bundle.getParams().get("documentno");
      final String ssfibanktransferid = (String) bundle.getParams().get("ssfiBanktransferId");
      final String ssprcategoryacctid = (String) bundle.getParams().get("ssprCategoryAcctId");
      // Get the Payroll Ticket data
      // Obtener los datos de la Boleta de Nomina
      ArchPayTenthBankPichinchaTXTData data[] = ArchPayTenthBankPichinchaTXTData.select(conn,
          cyearid, ssfibanktransferid, ssprcategoryacctid.length() > 0 ? ssprcategoryacctid : "%");
      if (data != null && data.length > 0) {
        bundle.setResult(message);

        // Prepar browser to receive file
        // Preparar el navegador para recibir el archivo
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/txt");
        response.setHeader("Content-Disposition",
            "attachment; filename=TransferenciaDecimosPichincha.txt");
        // Build txt file
        // Consrtuir el archivo txt
        PrintWriter out = response.getWriter();
        try {
          // out.println((Utility.fileToString(file.getAbsolutePath())));

          Integer numberEmployee = 0;
          // BANDERA VALIDACION ERROR CUENTA
          Integer flag_error = 0;
          Integer flag_error_tercero = 0;
          // Integer Sec_contrapartida = 1;

          for (ArchPayTenthBankPichinchaTXTData archUTransfData : data) {
            if (Double.valueOf(archUTransfData.valor) != 0) {

              /*
               * String StrAccount = ""; try { StrAccount = archPTransfData.accountno; } catch
               * (Exception e) { }
               */
              // VALIDA BANDERA ERROR PARA ESCRITURA

              String str = (archUTransfData.valor).replace(".", "");
              String strZeros = String.format("%13s", str).replace(' ', '0');

              out.write(archUTransfData.codorientacion);
              out.write("\t");
              out.write(archUTransfData.contrapartida);
              out.write("\t");
              out.write(archUTransfData.moneda);
              out.write("\t");
              out.write(strZeros);
              out.write("\t");
              out.write(archUTransfData.formapc);
              out.write("\t");
              out.write(archUTransfData.tipocuenta);
              out.write("\t");
              out.write(removeAcute(archUTransfData.numerocuenta));
              out.write("\t");
              out.write(removeAcute(archUTransfData.referencia));
              out.write("\t");
              out.write(archUTransfData.tipoidclie);
              out.write("\t");
              out.write(archUTransfData.numidclie);
              out.write("\t");
              out.write(removeAcute(archUTransfData.nomclie));
              // out.write("\t");
              // out.write(archPTransfData.code);
              out.println();

              /*
               * out.write(archUTransfData.codorientacion); out.write("       " +
               * archUTransfData.contrapartida); out.write("       " + archUTransfData.moneda);
               * out.write("       " + (archUTransfData.valor).replace(".", ""));
               * out.write("       " + archUTransfData.formapc); out.write("       " +
               * archUTransfData.tipocuenta); out.write("       " +
               * removeAcute(archUTransfData.numerocuenta)); out.write("       " +
               * removeAcute(archUTransfData.referencia)); out.write("     " +
               * archUTransfData.tipoidclie); out.write("     " + archUTransfData.numidclie);
               * out.write("     " + removeAcute(archUTransfData.nomclie)); out.write("     " +
               * archUTransfData.codbanco); out.println();
               */
              // Sec_contrapartida = Sec_contrapartida + 1;

            }
          }
          // Send file to browser
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
        throw new OBException(
            "No existen registros configurados para la generacion del reporte. Revise la configuracion e intente denuevo.");
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
