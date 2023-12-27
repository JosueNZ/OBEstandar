package com.sidesoft.hrm.payroll.create_txt;

import java.io.PrintWriter;

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

public class ArchivePaymentUtilitiesRuminahuiBankTXT extends DalBaseProcess {

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

      final String strYearID = (String) bundle.getParams().get("cYearId");

      // Get the Payroll Ticket data
      // Obtener los datos de la Boleta de Nomina
      ArchivePaymentUtilitiesRuminahuiBankData data[] = ArchivePaymentUtilitiesRuminahuiBankData
          .select(conn, strYearID);
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
        response.setHeader("Content-Disposition",
            "attachment; filename=PayUtilitiesRuminahuiBank.txt");
        // Build txt file
        // Consrtuir el archivo txt
        PrintWriter out = response.getWriter();
        try {
          // out.println((Utility.fileToString(file.getAbsolutePath())));

          // final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy h:mm:ss");
          // final Date date = new Date();
          Integer secuencial = 1;
          for (ArchivePaymentUtilitiesRuminahuiBankData archPAPRumBankData : data) {
            out.write(archPAPRumBankData.pa);
            out.write("\t");
            out.write(archPAPRumBankData.idcliente);
            out.write("\t");
            out.write(archPAPRumBankData.moneda);
            out.write("\t");
            out.write(archPAPRumBankData.totalutilidades.toString().replaceAll("\\.", "")
                .replaceAll(",", ""));
            out.write("\t");
            out.write(archPAPRumBankData.cta);
            out.write("\t");
            out.write(archPAPRumBankData.bankacctype);
            out.write("\t");
            out.write(archPAPRumBankData.accountno);
            out.write("\t");
            out.write(archPAPRumBankData.referencia);
            out.write("\t");
            out.write(archPAPRumBankData.typeid);
            out.write("\t");
            out.write(archPAPRumBankData.idcliente2);
            out.write("\t");
            out.write(archPAPRumBankData.cliente);
            out.write("\t");
            out.println(archPAPRumBankData.codigobanco);
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
