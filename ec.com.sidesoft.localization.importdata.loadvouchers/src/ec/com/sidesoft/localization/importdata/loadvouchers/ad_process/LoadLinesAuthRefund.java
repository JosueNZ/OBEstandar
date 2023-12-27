package ec.com.sidesoft.localization.importdata.loadvouchers.ad_process;

import java.util.List;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.InputStreamReader;

import java.sql.BatchUpdateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.localization.ecuador.refunds.ssrerefundinvoice;

import org.openbravo.base.secureApp.VariablesSecureApp;

import org.openbravo.client.kernel.RequestContext;
import org.openbravo.base.session.OBPropertiesProvider;

import org.apache.log4j.Logger;

public class LoadLinesAuthRefund extends DalBaseProcess {
  OBError message;
  private boolean insert = false;
  private int processed;
  private int notprocessed;
  private int rejected;
  private boolean cancelled;
  protected ConnectionProvider conn;
  protected VariablesSecureApp vars;
  protected String language;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception, IOException {

    VariablesSecureApp vars = bundle.getContext().toVars();
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    this.conn = conn;
    this.vars = vars;
    this.language = language;

    try {
      message = new OBError();
      processRequest(bundle);
    } catch (Exception e) {
      // log4j1.info("exeption" + e);
      message.setMessage(e.getMessage());
      message.setTitle(Utility.messageBD(conn, "Error", language));
      message.setType("Error");
    } finally {
      bundle.setResult(message);
    }
  }

  public void processRequest(ProcessBundle bundle) throws Exception {

    String messageT = "";
    String typeM = "";
    String titleM = "";
    String recordId = (String) bundle.getParams().get("C_Invoice_ID");
    Invoice record = OBDal.getInstance().get(Invoice.class, recordId);

    if (record.getDocumentStatus().equals("DR") && record.getInvoiceLineList().size() == 0
        && record.getSsreRefundinvoiceList().size() > 0) {
      long numLine = 10;
      for (ssrerefundinvoice line : record.getSsreRefundinvoiceList()) {
        if (line.getUntaxedBasis().compareTo(BigDecimal.ZERO) != 0) {
          insertLineSalesInvoice(numLine, line, record, true);
          numLine = numLine + 10;
        }
        if (line.getTaxbaserefund().compareTo(BigDecimal.ZERO) != 0) {
          insertLineSalesInvoice(numLine, line, record, false);
          numLine = numLine + 10;
        }
      }
      message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
      message.setType("Success");
    }
  }

  private void insertLineSalesInvoice(long numLine, ssrerefundinvoice line, Invoice newInvoice,
      boolean base) {

    if (line.getImdlvProduct() != null) {

      // IMPUESTO DE LA LINEA
      OBCriteria<TaxRate> taxRate = OBDal.getInstance().createCriteria(TaxRate.class);
      taxRate.add(
          Restrictions.eq(TaxRate.PROPERTY_RATE, base ? BigDecimal.ZERO : new BigDecimal("12")));
      taxRate.add(Restrictions.eq(TaxRate.PROPERTY_WITHHOLDINGTAX, false));
      // PRECIO
      int stdPrecision = newInvoice.getCurrency().getStandardPrecision().intValue();
      BigDecimal price = base ? line.getUntaxedBasis() : line.getTaxbaserefund();

      // MONTO NETO DE LA LINEA
      BigDecimal lineNetAmount = BigDecimal.ONE.multiply(price).setScale(stdPrecision,
          RoundingMode.HALF_UP);

      InvoiceLine newInvoiceLine = OBProvider.getInstance().get(InvoiceLine.class);
      newInvoiceLine.setOrganization(newInvoice.getOrganization());
      newInvoiceLine.setClient(newInvoice.getClient());
      newInvoiceLine.setInvoice(newInvoice);
      newInvoiceLine.setLineNo(numLine);
      newInvoiceLine.setProduct(line.getImdlvProduct());
      newInvoiceLine.setTax(taxRate.list().get(0));
      newInvoiceLine.setUOM(line.getImdlvProduct().getUOM());
      newInvoiceLine.setLineNetAmount(lineNetAmount);
      newInvoiceLine.setListPrice(price);
      newInvoiceLine.setUnitPrice(price);
      newInvoiceLine.setPriceLimit(price);
      newInvoiceLine.setStandardPrice(price);
      newInvoiceLine.setInvoicedQuantity(BigDecimal.ONE);
      newInvoiceLine.setExcludeforwithholding(true);
      String rucProvider = line.getTaxidruc();
      String noRefundInvoice = line.getStablishment() + "-" + line.getShell() + "-"
          + line.getOrderReference();
      String codeRefund = "0";
      // COD LIQUIDACIONES REEMBOLSO
      if (newInvoice.getSsreRefunded() != null && newInvoice.getSsreRefunded().getCodigo() != null
          && newInvoice.getSsreRefunded().getCodigo().equals("41")
          && newInvoice.getSsreRefunded().getType() != null
          && newInvoice.getSsreRefunded().getType().equals("PR")) {
        codeRefund = newInvoice.getSsreRefunded().getCodigo();
      }
      newInvoiceLine.setDescription(newInvoice.getOrderReference() + "," + rucProvider + ","
          + noRefundInvoice + "," + codeRefund);
      OBDal.getInstance().save(newInvoiceLine);
      OBDal.getInstance().flush();
    } else {
      throw new OBException("No se asigno un producto para para la linea de la factura.");
    }
  }

}
