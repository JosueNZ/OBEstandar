package com.sidesoft.hrm.payroll.tenth.create_txt;

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

public class ArchTenthCtralBankTXT extends DalBaseProcess {

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

      final String strDocumentNo = (String) bundle.getParams().get("documentno");
      final String strSSPRCategoryAcctId = (String) bundle.getParams().get("ssprCategoryAcctId");
      final String strSendNo = (String) bundle.getParams().get("sendno");

      // Get the Payroll Ticket data
      // Obtener los datos de la Boleta de Nomina
      ArchTenthCtralBankData data[] = ArchTenthCtralBankData.select(conn, strDocumentNo,
          strSSPRCategoryAcctId);
      if (data != null && data.length > 0) {
        bundle.setResult(message);

        // TODO: Save actual headers
        // // Get actual headers
        // // Obtener cabeceras actuales
        // Enumeration<String> headersNames = request.getHeaderNames();
        // ArrayList<Enumeration> headers = new ArrayList<Enumeration>();
        // while (headersNames.hasMoreElements()) {
        // String name = headersNames.nextElement();
        // headers.add(request.getHeaders(name));
        // }
        // String oldCharacterEncoding = response.getCharacterEncoding();
        // String oldContentType = response.getContentType();

        // Prepar browser to receive file
        // Preparar el navegador para recibir el archivo
        response.setCharacterEncoding("Cp1252");
        response.setContentType("application/txt");
        response.setHeader("Content-Disposition", "attachment; filename=TenthCtralBank.txt");
        // Build txt file
        // Consrtuir el archivo txt
        PrintWriter out = response.getWriter();
        try {
          // out.println((Utility.fileToString(file.getAbsolutePath())));

          Integer numberEmployee = 0;

          for (ArchTenthCtralBankData archPCBankData : data) {
            if (Double.valueOf(archPCBankData.totalnet) != 0) {
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
          out.println("," + data[0].period);
          Integer secuencial = 1;
          for (ArchTenthCtralBankData archPCBankData : data) {
            if (Double.valueOf(archPCBankData.totalnet) != 0) {
              out.write(secuencial.toString());
              out.write("," + archPCBankData.totalnet);
              out.write("," + archPCBankData.codcategoryacct);
              out.write("," + archPCBankData.bankcode);
              out.write(","
                  + ((archPCBankData.accountno == null || archPCBankData.accountno.equals("") || archPCBankData.accountno
                      .isEmpty()) ? "ERROR TERCERO NO TIENE CUENTA CONFIGURADA PARA PAGOS AUTOMATICOS"
                      : archPCBankData.accountno));
              out.write(","
                  + ((archPCBankData.bankaccounttype == null
                      || archPCBankData.bankaccounttype.equals("") || archPCBankData.bankaccounttype
                        .isEmpty()) ? "ERROR TERCERO NO TIENE CUENTA CONFIGURADA PARA PAGOS AUTOMATICOS"
                      : archPCBankData.bankaccounttype));
              out.write("," + removeAcute(archPCBankData.employee));
              out.write("," + removeAcute(archPCBankData.description));
              out.println("," + archPCBankData.ci);
              secuencial++;
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
