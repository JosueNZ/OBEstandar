package com.sidesoft.localization.ecuador.withholdings.create_xml;

import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

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

public class ArchProviderTransferTXT extends DalBaseProcess {

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

      final String strCDoctypeId = (String) bundle.getParams().get("cDoctypeId");
      final String strSendNo = (String) bundle.getParams().get("sendno");
      final String strDateFrom = (String) bundle.getParams().get("datefrom");
      final String strDateTo = (String) bundle.getParams().get("dateto");
      final String strFINFinnantialAccountId = (String) bundle.getParams().get(
          "finFinancialAccountId");

      // Get the Payroll Ticket data
      // Obtener los datos de la Boleta de Nomina
      ArchProviderTransferData data[] = ArchProviderTransferData.select(conn, strDateFrom,
          strDateTo, strCDoctypeId, strFINFinnantialAccountId);
      if (data != null && data.length > 0) {
        bundle.setResult(message);

        // Prepar browser to receive file
        // Preparar el navegador para recibir el archivo
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/txt");
        response.setHeader("Content-Disposition", "attachment; filename=ProviderTransfer.txt");
        // Build txt file
        // Consrtuir el archivo txt
        PrintWriter out = response.getWriter();
        try {
          // out.println((Utility.fileToString(file.getAbsolutePath())));

          Integer numberEmployee = 0;
          // BANDERA VALIDACION ERROR CUENTA
          Integer flag_error = 0;
          Integer flag_error_tercero = 0;

          for (ArchProviderTransferData archPTransfData : data) {
            if (Double.valueOf(archPTransfData.totalnet) != 0) {
              numberEmployee++;
            }
          }

          final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy h:mm:ss");
          final Date date = new Date();

          out.write(dateFormat.format(date).toString());
          out.write("," + strSendNo);
          out.write("," + numberEmployee.toString());
          out.write("," + data[0].constante1);
          out.write("," + data[0].totalnetemployee);
          out.write("," + data[0].constante2);
          out.write("," + data[0].constante3);
          out.write("," + data[0].constante4);
          out.write("," + removeAcute(data[0].razonsocial));
          out.write("," + removeAcute(data[0].city));
          out.write("," + data[0].period);
          out.println("");
          Integer secuencial = 1;
          for (ArchProviderTransferData archPTransfData : data) {
            if (Double.valueOf(archPTransfData.totalnet) != 0) {

              String StrAccount = "";
              try {
                StrAccount = archPTransfData.accountno;
              } catch (Exception e) {
              }
              String StrFleetcode = "";
              try {
                StrFleetcode = archPTransfData.fleetcode;
              } catch (Exception e) {
              }
              // VALIDA QUE ESTE CONFIGURADA CUENTA PAGOS AUTOMATICOS
              if (StrAccount == null || StrAccount.equals("") || StrAccount.isEmpty()) {
                // ACTIVO BANDERA DE ERROR
                flag_error = 1;
                flag_error_tercero = 1;
              } else {
                flag_error_tercero = 0;
              }

              if (StrFleetcode == null || StrFleetcode.equals("") || StrFleetcode.isEmpty()) {
                // ACTIVO BANDERA DE ERROR
                flag_error = 1;
                flag_error_tercero = 1;
              } else {
                flag_error_tercero = 0;
              }

              // VALIDA BANDERA ERROR PARA ESCRITURA
              if (flag_error == 0) {

                out.write(secuencial.toString());
                out.write("," + archPTransfData.totalnet);
                out.write("," + removeAcute(archPTransfData.fleetcode));
                out.write("," + archPTransfData.bankcode);
                out.write("," + archPTransfData.accountno);
                out.write("," + archPTransfData.bankaccounttype);
                out.write("," + removeAcute(archPTransfData.employee));
                out.write("," + removeAcute(archPTransfData.description));
                out.println("," + archPTransfData.ci);
                secuencial++;
              } else {
                if (flag_error_tercero == 1) {

                  if (StrFleetcode == null || StrFleetcode.equals("") || StrFleetcode.isEmpty()) {
                    out.write(secuencial.toString());
                    out.write("," + archPTransfData.totalnet);
                    out.write("," + "TERCERO NO TIENE CONFIGURADO TIPO DE PAGO");
                    out.write("," + archPTransfData.bankcode);
                    out.write("," + archPTransfData.accountno);
                    out.write("," + archPTransfData.bankaccounttype);
                    out.write("," + removeAcute(archPTransfData.employee));
                    out.write("," + removeAcute(archPTransfData.description));
                    out.println("," + archPTransfData.ci);
                    secuencial++;

                  }

                  if (StrAccount == null || StrAccount.equals("") || StrAccount.isEmpty()) {
                    out.write(secuencial.toString());
                    out.write("," + archPTransfData.totalnet);
                    out.write("," + removeAcute(archPTransfData.fleetcode));
                    out.write(","
                        + "ERROR TERCERO NO TIENE CUENTA CONFIGURADA PARA PAGOS AUTOMATICOS");
                    out.write(","
                        + "ERROR TERCERO NO TIENE CUENTA CONFIGURADA PARA PAGOS AUTOMATICOS");
                    out.write("," + archPTransfData.bankaccounttype);
                    out.write("," + removeAcute(archPTransfData.employee));
                    out.write("," + removeAcute(archPTransfData.description));
                    out.println("," + archPTransfData.ci);
                    secuencial++;
                  }
                }
              }
            }
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
