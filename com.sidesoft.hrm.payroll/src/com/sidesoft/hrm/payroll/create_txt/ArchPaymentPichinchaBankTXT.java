package com.sidesoft.hrm.payroll.create_txt;

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

public class ArchPaymentPichinchaBankTXT extends DalBaseProcess {

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
      final String documentno = (String) bundle.getParams().get("documentno");
      final String strBankId = (String) bundle.getParams().get("ssfiBanktransferId");
      final String strCategoryId = (String) bundle.getParams().get("ssprCategoryAcctId");
      final String strCostCenter = (String) bundle.getParams().get("cCostcenterId");

      // Get the Payroll Ticket data
      // Obtener los datos de la Boleta de Nomina
      ArchPaymentPichinchaBankTXTData data[] = ArchPaymentPichinchaBankTXTData.select(conn,
    		  documentno, strBankId, strCategoryId, strCostCenter);
      
      if (data != null && data.length > 0) {
        bundle.setResult(message);

        // Prepar browser to receive file
        // Preparar el navegador para recibir el archivo
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/txt");
        response.setHeader("Content-Disposition", "attachment; filename=PichinchaTransfer.txt");
        // Build txt file
        // Consrtuir el archivo txt
        PrintWriter out = response.getWriter();
        try {
          // out.println((Utility.fileToString(file.getAbsolutePath())));

          Integer numberEmployee = 0;
          // BANDERA VALIDACION ERROR CUENTA
          Integer flag_error = 0;
          Integer flag_error_tercero = 0;
          //Integer Sec_contrapartida = 1;

          for (ArchPaymentPichinchaBankTXTData archPTransfData : data) {
            if (Double.valueOf(archPTransfData.valor) != 0) {

              /*
               * String StrAccount = ""; try { StrAccount = archPTransfData.accountno; } catch
               * (Exception e) { }
               */
              // VALIDA BANDERA ERROR PARA ESCRITURA
              String str = (archPTransfData.valor).replace(".", "");
              String strZeros = String.format("%13s", str).replace(' ', '0');              

              out.write(archPTransfData.pa);
              out.write("\t");
              out.write(archPTransfData.contrapartida);
              out.write("\t");
              out.write(archPTransfData.moneda);
              out.write("\t");
              out.write(strZeros);
              out.write("\t");
              out.write(archPTransfData.formacobro);
              out.write("\t");
              out.write(archPTransfData.tipocuenta);
              out.write("\t");
              out.write(removeAcute(archPTransfData.cuenta));
              out.write("\t");
              out.write(removeAcute(archPTransfData.referencia));
              out.write("\t");
              out.write(archPTransfData.tipoidcliente);
              out.write("\t");
              out.write(archPTransfData.numidcliente);
              out.write("\t");
              out.write(removeAcute(archPTransfData.tercero));
              if(strBankId == null || strBankId.isEmpty()) {
            	  out.write("\t");
            	  out.write(archPTransfData.codigo);
            	  out.write("\t");
            	  out.write(archPTransfData.transfer);            	  
              }
              //out.write("\t");
              //out.write(archPTransfData.code);
              out.println();

              //Sec_contrapartida = Sec_contrapartida + 1;

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
            "No existen empleados configurados para la generacion del reporte. Revise la configuracion e intente denuevo.");
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
    }// for i
    return output;
  }// removeAcute
}
