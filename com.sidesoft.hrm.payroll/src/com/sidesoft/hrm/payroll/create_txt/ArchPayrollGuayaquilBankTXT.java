package com.sidesoft.hrm.payroll.create_txt;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.xmlEngine.XmlEngine;

import com.sidesoft.hrm.payroll.Payroll;
import com.sidesoft.hrm.payroll.ssprpayrollautline;
import com.sidesoft.hrm.payroll.ssprpayrollaut;

public class ArchPayrollGuayaquilBankTXT extends DalBaseProcess {

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
      final String ssfiBanktransferId = (String) bundle.getParams().get("ssfiBanktransferId");

      OBCriteria<Payroll> obc = OBDal.getInstance().createCriteria(Payroll.class);
      obc.add(Restrictions.eq(Payroll.PROPERTY_DOCUMENTNO, strDocumentNo.trim()));
      obc.add(Restrictions.eq(Payroll.PROPERTY_PAYROLL, true));
      obc.setFilterOnReadableOrganization(false);
      obc.setMaxResults(1);
      Payroll attach = (Payroll) obc.uniqueResult();
      if (attach != null) {
        // Get the Payroll Ticket data
        // Obtener los datos de la Boleta de Nomina para el banco de Guayaquil
        ArchPayrollGuayaquilBankData data[] = ArchPayrollGuayaquilBankData.select(conn,
            attach.getId(), ssfiBanktransferId);
        if (data != null && data.length > 0) {
          bundle.setResult(message);
          ssprpayrollautline autLine = attach.getSsprPayrollAutLineList().get(0);
          ssprpayrollaut aut = autLine.getSsprPayrollAut();
          aut.setProcessCounter(aut.getProcessCounter().add(BigDecimal.ONE));

          // TODO: Save actual headers
          final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
          final Date date = new Date();
          // Prepar browser to receive file
          // Preparar el navegador para recibir el archivo
          String number_process = aut.getProcessCounter().toString();
          String name_file = "NCR" + dateFormat.format(date).toString() + "EEV_" + number_process
              + ".txt";
          response.setCharacterEncoding("Cp1252");
          response.setContentType("application/txt");
          response.setHeader("Content-Disposition", "attachment; filename=" + name_file);
          // Build txt file
          // Consrtuir el archivo txt
          PrintWriter out = response.getWriter();
          try {
            for (ArchPayrollGuayaquilBankData payrollData : data) {

              out.write(payrollData.cuenta);
              out.write(String.format("%-20s", ""));
              out.write(payrollData.cliente);
              out.write("\r\n");
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
          message.setTitle(Utility.messageBD(conn, "Error", language));
          message.setType("Error");
          message.setMessage("No se encontró información en la consulta");
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
