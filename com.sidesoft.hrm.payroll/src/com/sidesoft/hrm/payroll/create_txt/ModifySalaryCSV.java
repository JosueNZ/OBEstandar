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

public class ModifySalaryCSV extends DalBaseProcess {

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

      final String strCPeriodId = (String) bundle.getParams().get("cPeriodId");

      // Get the Payroll Ticket data
      // Obtener los datos de la Boleta de Nomina
      ModifySalaryData data[] = ModifySalaryData.select(conn, strCPeriodId);
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

        // Prepar browser to receive file
        // Preparar el navegador para recibir el archivo
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=ModifyPayment.csv");
        // Build csv file
        // Consrtuir el archivo csv
        PrintWriter out = response.getWriter();
        try {
          // out.println((Utility.fileToString(file.getAbsolutePath())));

          for (ModifySalaryData modSalaryData : data) {
            out.write(modSalaryData.rucempresa);
            out.write("," + modSalaryData.estcode);
            out.write("," + modSalaryData.anio);
            out.write("," + modSalaryData.mes);
            out.write("," + modSalaryData.msu);
            out.write("," + modSalaryData.taxid);
            out.println("," + modSalaryData.amount);
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
}
