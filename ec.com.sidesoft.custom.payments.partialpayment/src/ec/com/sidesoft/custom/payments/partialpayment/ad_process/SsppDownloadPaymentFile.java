package ec.com.sidesoft.custom.payments.partialpayment.ad_process;

import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.secureApp.ClassInfoData;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.xmlEngine.XmlEngine;

import ec.com.sidesoft.custom.payments.partialpayment.SSPPPAYMENTS;

public class SsppDownloadPaymentFile extends DalBaseProcess {

  public XmlEngine xmlEngine = null;
  public static String strDireccion;
  protected Logger log4j = Logger.getLogger(this.getClass());
  protected ConfigParameters globalParameters;
  protected ClassInfoData classInfo;

  public void doExecute(ProcessBundle bundle) throws Exception {
    // Declara de una bariable tipo error
    final OBError message = new OBError();
    // conneccion
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    ConnectionProvider conn = bundle.getConnection();

    HttpServletResponse response = RequestContext.get().getResponse();
    HttpServletRequest request = RequestContext.get().getRequest();

    try {

      // OutputStream out = null;

      try {

        // Recupera el ID del registro

        final String StrPaymentID = (String) bundle.getParams().get("Sspp_Payments_ID");

        // Declara un objeto de este registro

        SSPPPAYMENTS SsppPayment = OBDal.getInstance().get(SSPPPAYMENTS.class, StrPaymentID);

        if (!SsppPayment.getDocumentType().getId().isEmpty()) {
          bundle.setResult(message);

          java.util.Date fecha_actual = new java.util.Date();
          SimpleDateFormat formato_fecha = new SimpleDateFormat("dd/MM/yyyy");

          String new_fecha = formato_fecha.format(fecha_actual);
          // Prepar browser to receive file
          // Preparar el navegador para recibir el archivo
          response.setCharacterEncoding("Cp1252");
          response.setContentType("application/txt");
          response.setHeader("Content-Disposition",
              "attachment; filename=LoteDePagos-" + new_fecha + ".txt");
          // Build txt file
          // Consrtuir el archivo txt
          PrintWriter out = response.getWriter();
          // /**
          // * response.setContentType("application/vnd.ms-excel");
          // *
          // * response.setHeader("Content-Disposition", "attachment;
          // * filename=TransferenciaLotes.xls");
          // *
          // **/
          // response.setCharacterEncoding("Cp1252");
          // response.setContentType("application/txt");
          // response.setHeader("Content-Disposition",
          // "attachment; filename=PayUtilitiesProdubancoBank.txt");
          //
          // WritableWorkbook w = null;
          // w = Workbook.createWorkbook(response.getOutputStream());
          // WritableSheet s = w.createSheet("DETALLE DE PAGOS", 0);
          //
          // // Header
          // s.addCell(new Label(0, 0, "COD\n ORIENTACION"));
          // s.addCell(new Label(1, 0, "CONTRAPARTIDA"));
          // s.addCell(new Label(2, 0, "MONEDA"));
          // s.addCell(new Label(3, 0, "VALOR"));
          // s.addCell(new Label(4, 0, "FORMA DE COMBRO/PAGO"));
          // s.addCell(new Label(5, 0, "TIPO CUENTA"));
          // s.addCell(new Label(6, 0, "NUM CUENTA"));
          // s.addCell(new Label(7, 0, "REFERENCIA"));
          // s.addCell(new Label(8, 0, "TIPO CLIENTE "));
          // s.addCell(new Label(9, 0, "NUMERO CLIENTE"));
          // s.addCell(new Label(10, 0, "PAGOS"));

          SsppDownloadPaymentFileData data[] = SsppDownloadPaymentFileData.select(conn,
              StrPaymentID);
          if (data != null && data.length > 0) {

            /**
             * OBCriteria<SSPPPAYMENTSLINE> paymentsLine = OBDal.getInstance().createCriteria(
             * SSPPPAYMENTSLINE.class);
             * paymentsLine.add(Restrictions.eq(SSPPPAYMENTSLINE.PROPERTY_SSPPPAYMENTS,
             * SsppPayment)); int countPayments = paymentsLine.count(); if (countPayments > 0) { for
             * (SSPPPAYMENTSLINE collpaymentLine : paymentsLine.list()) {
             * 
             * SSPPPAYMENTSLINE updatePaymentsLine = OBDal.getInstance().get(
             * SSPPPAYMENTSLINE.class, collpaymentLine.getId().toString());
             * 
             * updatePaymentsLine.setPaidOut(true); OBDal.getInstance().save(updatePaymentsLine);
             * OBDal.getInstance().flush(); } }
             **/
            for (SsppDownloadPaymentFileData CollSsppPaymentLine : data) {
              // Detail

              // Ruc, Pasaporte, Tarjeta de identificación
              out.write(CollSsppPaymentLine.codOrientacion);
              out.write("\t");
              out.write(CollSsppPaymentLine.contrapartida);
              out.write("\t");
              out.write(CollSsppPaymentLine.moneda);
              out.write("\t");

              DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
              simbolos.setDecimalSeparator('.');
              DecimalFormat formatter = new DecimalFormat("#########0.00", simbolos);
              BigDecimal bgdImpFormat = BigDecimal.ZERO;
              bgdImpFormat = new BigDecimal(String.valueOf(CollSsppPaymentLine.valor));
              String StrImportNew = formatter.format(bgdImpFormat).toString() == null ? "0.00"
                  : formatter.format(bgdImpFormat);
              out.write(String.format("%013d",
                  Integer.parseInt(StrImportNew.replaceAll("\\.", "").replaceAll(",", ""))));
              out.write("\t");

              out.write(CollSsppPaymentLine.formaPago);
              out.write("\t");

              out.write(CollSsppPaymentLine.tipoCuenta);
              out.write("\t");
              out.write(CollSsppPaymentLine.numCuenta);
              out.write("\t");

              out.write(CollSsppPaymentLine.pago);

              out.write("\t");
              out.write(CollSsppPaymentLine.tipoIdCliente);
              out.write("\t");
              out.write(CollSsppPaymentLine.numIdCliente);
              out.write("\t");
              out.write(CollSsppPaymentLine.nombreCliente);
              out.write("\t");
              out.write(CollSsppPaymentLine.codigoBanco);
              out.write("\t");
              out.println();

            }
            out.close();
          }
        }

      } catch (Exception e) {
        throw new ServletException("Exception in Excel Sample Servlet", e);
      } finally {
        // if (out != null)
        // out.close();
      }
    } finally {
      OBDal.getInstance().commitAndClose();
      bundle.setResult(message);
    }
  }

  protected String formatDate(java.util.Date date) {
    return new SimpleDateFormat((String) OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .get(KernelConstants.DATE_FORMAT_PROPERTY)).format(date);
  }

  public String getServletInfo() {
    return "Servlet PaymentReport.";
  } // end of getServletInfo() method

}
