package com.sidesoft.hrm.payroll.create_txt;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.ConfigParameters;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.xmlEngine.XmlEngine;
//import org.openbravo.base.secureApp;
import org.quartz.SchedulerContext;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;

public class UtilitiesCSV extends DalBaseProcess {
  private ConfigParameters cf;
  private SchedulerContext ctx;
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
      final String strOutputType = (String) bundle.getParams().get("outputtype");
      Year objYear = OBDal.getInstance().get(Year.class, strYearID);
      int intYearNumb = Integer.parseInt(objYear.getFiscalYear());
      UtilitiesCSVData data[] = UtilitiesCSVData.select(conn, strYearID);

      if (strOutputType.equals("XLS")) {
        try {

          if (data != null && data.length > 0) {
            bundle.setResult(message);

                        // WritableWorkbook w = Workbook.createWorkbook(response.getOutputStream());

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        WritableWorkbook w = Workbook.createWorkbook(baos);

                        WritableSheet s = w.createSheet("Utilities", 0);

            s.addCell(new Label(0, 0, "Cedula (Ejm.:0502366503)"));
            s.addCell(new Label(1, 0, "Nombres"));
            s.addCell(new Label(2, 0, "Apellidos"));
            s.addCell(new Label(3, 0, "Genero (Masculino=M ó Femenino=F)"));
            s.addCell(new Label(4, 0, "Ocupacion"));
            s.addCell(new Label(5, 0, "Cargas familiares"));
            s.addCell(new Label(6, 0, "Días laborados (360 días equivalen a un año)"));
            s.addCell(new Label(7, 0,
                "Tipo de Pago Utilidad(Pago Directo=P,Deposito MDT=D,Acreditación en Cuenta=A,Retencion Pago Directo=RP,Retencion Deposito MDT=RD,Retencion Acreditación en Cuenta=RA)"));
            s.addCell(new Label(8, 0,
                "JORNADA PARCIAL PERMANENTE(Ponga una X si el trabajador tiene un JORNADA PARCIAL PERMANENTE)"));
            s.addCell(new Label(9, 0,
                "DETERMINE EN HORAS LA JORNADA PARCIAL PERMANENTE SEMANAL ESTIPULADO EN EL CONTRATO"));
            s.addCell(
                new Label(10, 0, "DISCAPACITADOS(Ponga una X si el trabajador tienediscapacidad)"));
            s.addCell(new Label(11, 0, "RUC DE LA EMPRESA COMPLEMENTARIA O DE UNIFICACION"));
            s.addCell(new Label(12, 0, "DECIMOTERCERO VALOR PROPORCIONAL AL TIEMPO LABORADO "
                + String.valueOf(intYearNumb) + "")); //
            s.addCell(new Label(13, 0, "DECIMOCUARTO VALOR PROPORCIONAL AL TIEMPO LABORADO "
                + String.valueOf(intYearNumb) + "")); //
            s.addCell(new Label(14, 0,
                "PARTICIPACION DE UTILIDADES " + String.valueOf(intYearNumb - 1) + ""));
            s.addCell(new Label(15, 0, "SALARIOS PERCIBIDOS " + String.valueOf(intYearNumb))); //
            s.addCell(new Label(16, 0, "FONDOS DE RESERVA " + String.valueOf(intYearNumb))); //
            s.addCell(new Label(17, 0, "COMISIONES DEL " + String.valueOf(intYearNumb))); //
            s.addCell(new Label(18, 0,
                "BENEFICIOS ADICIONALES EN EFECTIVO " + String.valueOf(intYearNumb) + ""));//
            s.addCell(new Label(19, 0, "Anticipo de Utilidad"));
            s.addCell(new Label(20, 0, "Retencion Judicial"));
            s.addCell(new Label(21, 0, "Impuesto Retencion"));
            s.addCell(new Label(22, 0, "Información MDT(No ingrese datos en esta columna)"));
            s.addCell(new Label(23, 0,
                "Tipo de Pago Salario Digno(Pago Directo=P,Deposito MDT=D,Acreditación en Cuenta=A)"));

            int i = 1;
            for (UtilitiesCSVData utilitiesData : data) {

              s.addCell(new Label(0, i, utilitiesData.cedula));
              s.addCell(new Label(1, i, utilitiesData.nombre));
              s.addCell(new Label(2, i, utilitiesData.apellido));
              s.addCell(new Label(3, i, utilitiesData.genero));
              s.addCell(new Label(4, i, utilitiesData.ocupacion));
              s.addCell(new Label(5, i, utilitiesData.cargaFamiliar));
              s.addCell(new Label(6, i, utilitiesData.diasLaborados));
              s.addCell(new Label(7, i, utilitiesData.tipoPagoUtilidad));
              s.addCell(new Label(8, i, utilitiesData.jornadaParcial)); // Jornada parcial//
                                                                        // permanente
              s.addCell(new Label(9, i, utilitiesData.horasSemanaJparcial)); // horas jornada//
                                                                             // parcial
              s.addCell(new Label(10, i, utilitiesData.discapacitado));
              s.addCell(new Label(11, i, utilitiesData.rucEmpresa));
              s.addCell(new Label(12, i, utilitiesData.decimoTercero));
              s.addCell(new Label(13, i, utilitiesData.decimoCuarto));
              s.addCell(new Label(14, i, utilitiesData.participacionUtil));
              s.addCell(new Label(15, i, utilitiesData.salarioPercibido));
              s.addCell(new Label(16, i, utilitiesData.fondoReserva));
              s.addCell(new Label(17, i, utilitiesData.comision));
              s.addCell(new Label(18, i, "0")); // Beneficios Adicionales
              s.addCell(new Label(19, i, utilitiesData.anticipoUtilidad));
              s.addCell(new Label(20, i, utilitiesData.retencionJudicial));
              s.addCell(new Label(21, i, utilitiesData.impuestoRetencion));
              s.addCell(new Label(22, i, "")); // Información MDT
              s.addCell(new Label(23, i, utilitiesData.tipoPagoSalario));
              i++;
            }

		            w.write();
                w.close();

                byte[] byteArray = baos.toByteArray();

                response.setContentType("application/vnd.ms-excel");
                response.setHeader("Content-Disposition", "attachment; filename=Utilities.xls");
                response.setContentLength(byteArray.length);

                OutputStream os = response.getOutputStream();
                try {
                    os.write(byteArray, 0, byteArray.length);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    os.close();
                }

          }

        } catch (final Exception e) {
          e.printStackTrace(System.err);
          message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
          message.setType("Error");
          message.setMessage(e.getMessage() + e.fillInStackTrace());
        } finally {
          bundle.setResult(message);
        }

      } else {

        // Get the Payroll Ticket data
        // Obtener los datos de la Boleta de Nomina

        if (data != null && data.length > 0) {
          bundle.setResult(message);

          // Prepar browser to receive file
          // Preparar el navegador para recibir el archivo
          response.setCharacterEncoding("Cp1252");
          response.setContentType("application/csv");
          response.setHeader("Content-Disposition", "attachment; filename=UtilitiesCSV.csv");
          // Build txt file
          // Consrtuir el archivo txt
          PrintWriter out = response.getWriter();
          try {
            // out.println((Utility.fileToString(file.getAbsolutePath())));

            // final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy h:mm:ss");
            // final Date date = new Date();

            out.write("Cedula (Ejm.:0502366503);");
            out.write("Nombres;");
            out.write("Apellidos;");
            out.write("Genero (Masculino=M ó Femenino=F);");
            out.write("Ocupacion;");
            out.write("Cargas familiares;");
            out.write("Días laborados (360 días equivalen a un año);");
            out.write(
                "Tipo de Pago Utilidad(Pago Directo=P,Deposito MDT=D,Acreditación en Cuenta=A,Retencion Pago Directo=RP,Retencion Deposito MDT=RD,Retencion Acreditación en Cuenta=RA);");
            out.write(
                "JORNADA PARCIAL PERMANENTE(Ponga una X si el trabajador tiene un JORNADA PARCIAL PERMANENTE);");
            out.write(
                "DETERMINE EN HORAS LA JORNADA PARCIAL PERMANENTE SEMANAL ESTIPULADO EN EL CONTRATO;");
            out.write("DISCAPACITADOS(Ponga una X si el trabajador tienediscapacidad);");
            out.write("RUC DE LA EMPRESA COMPLEMENTARIA O DE UNIFICACION;");
            out.write("DECIMOTERCERO VALOR PROPORCIONAL AL TIEMPO LABORADO "
                + String.valueOf(intYearNumb) + ";"); //
            out.write("DECIMOCUARTO VALOR PROPORCIONAL AL TIEMPO LABORADO "
                + String.valueOf(intYearNumb) + ";"); //
            out.write("PARTICIPACION DE UTILIDADES " + String.valueOf(intYearNumb - 1) + ";");
            out.write("SALARIOS PERCIBIDOS " + String.valueOf(intYearNumb) + ";"); //
            out.write("FONDOS DE RESERVA " + String.valueOf(intYearNumb) + ";"); //
            out.write("COMISIONES DEL " + String.valueOf(intYearNumb) + ";"); //
            out.write("BENEFICIOS ADICIONALES EN EFECTIVO " + String.valueOf(intYearNumb) + ";");//
            out.write("Anticipo de Utilidad;");
            out.write("Retencion Judicial;");
            out.write("Impuesto Retencion;");
            out.write("Información MDT(No ingrese datos en esta columna);");
            out.println(
                "Tipo de Pago Salario Digno(Pago Directo=P,Deposito MDT=D,Acreditación en Cuenta=A)");

            Integer secuencial = 1;
            for (UtilitiesCSVData utilitiesData : data) {

              out.write(utilitiesData.cedula);
              out.write(";");
              out.write(utilitiesData.nombre);
              out.write(";");
              out.write(utilitiesData.apellido);
              out.write(";");
              out.write(utilitiesData.genero);
              out.write(";");
              out.write(utilitiesData.ocupacion);
              out.write(";");
              out.write(utilitiesData.cargaFamiliar);
              out.write(";");
              out.write(utilitiesData.diasLaborados);
              out.write(";");
              out.write(utilitiesData.tipoPagoUtilidad);
              out.write(";");
              out.write(utilitiesData.jornadaParcial); // Jornada parcial permanente
              out.write(";");
              out.write(utilitiesData.horasSemanaJparcial); // horas jornada parcial
              out.write(";");
              out.write(utilitiesData.discapacitado);
              out.write(";");
              out.write(utilitiesData.rucEmpresa);
              out.write(";");
              out.write(utilitiesData.decimoTercero);
              out.write(";");
              out.write(utilitiesData.decimoCuarto);
              out.write(";");
              out.write(utilitiesData.participacionUtil);
              out.write(";");
              out.write(utilitiesData.salarioPercibido);
              out.write(";");
              out.write(utilitiesData.fondoReserva);
              out.write(";");
              out.write(utilitiesData.comision);
              out.write(";");
              out.write("0"); // Beneficios Adicionales
              out.write(";");
              out.write(utilitiesData.anticipoUtilidad);
              out.write(";");
              out.write(utilitiesData.retencionJudicial);
              out.write(";");
              out.write(utilitiesData.impuestoRetencion);
              out.write(";");
              out.write(""); // Información MDT
              out.write(";");
              out.println(utilitiesData.tipoPagoSalario);

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
