package com.sidesoft.localization.ecuador.invoices.action_handler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.tax.TaxRate;

public class Sfpsi_ConsolidateSalesOrder extends BaseProcessActionHandler {

  private static final Logger log = Logger.getLogger(Sfpsi_ConsolidateSalesOrder.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    OBError myMessage = null;
    Connection conn = null;
    try {
      JSONObject request = new JSONObject(content);
      JSONObject params = request.getJSONObject("_params");
      JSONObject window = params.getJSONObject("window");
      JSONArray selection = window.getJSONArray("_selection");

      String invoiceId = request.getString("C_Invoice_ID");
      Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceId);

      Currency currency = invoice.getPriceList().getCurrency();
      int precision = currency.getStandardPrecision().intValue();

      Long lineNo = new Long(10);
      OBCriteria<InvoiceLine> qInvoiceLine = OBDal.getInstance().createCriteria(InvoiceLine.class);
      qInvoiceLine.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE, invoice));
      qInvoiceLine.setProjection(Projections.max(InvoiceLine.PROPERTY_LINENO));
      Long maxLineNo = (Long) qInvoiceLine.uniqueResult();
      if (maxLineNo != null && maxLineNo >= lineNo) {
        lineNo = maxLineNo + 10;
      }
      String orderId = "";
      String orderEmailTo = "";
      boolean writeEmailTo = true;

      for (int i = 0; i < selection.length(); i++) {

        JSONObject row = selection.getJSONObject(i);
        String orderLineId = row.getString("id");
        OrderLine orderLine = OBDal.getInstance().get(OrderLine.class, orderLineId);

        BigDecimal quantity = new BigDecimal(row.getString("quantity"));

        InvoiceLine invoiceLine = OBProvider.getInstance().get(InvoiceLine.class);
        if (orderLine.getSalesOrder().getEeiEmailTo() != null && writeEmailTo == true) {
          orderId = row.getString("salesOrder");
          orderEmailTo = orderLine.getSalesOrder().getEeiEmailTo();
          writeEmailTo = false;
        }

        invoiceLine.setInvoice(invoice);
        invoiceLine.setClient(invoice.getClient());
        invoiceLine.setOrganization(invoice.getOrganization());
        invoiceLine.setLineNo(lineNo);
        invoiceLine.setProduct(orderLine.getProduct());
        invoiceLine.setSprliIdentifier(orderLine.getProduct().getSearchKey());
        invoiceLine.setUOM(orderLine.getUOM());
        invoiceLine.setDescription(orderLine.getDescription());
        invoiceLine.setInvoicedQuantity(quantity);
        invoiceLine.setUnitPrice(orderLine.getUnitPrice());
        invoiceLine.setStandardPrice(orderLine.getStandardPrice());
        invoiceLine.setLineNetAmount(orderLine.getLineNetAmount());
        invoiceLine.setListPrice(orderLine.getListPrice());
        invoiceLine.setTax(orderLine.getTax());
        invoiceLine.setSsbodDiscountRate(orderLine.getSsbodDiscountRate());
        invoiceLine.setSseedDiscount(orderLine.getDiscount());
        invoiceLine.setAttributeSetValue(orderLine.getAttributeSetValue());
        invoiceLine.setSalesOrderLine(orderLine);
        invoiceLine.setCostcenter(orderLine.getCostcenter());
        invoiceLine.setNdDimension(orderLine.getNdDimension());
        invoiceLine.setStDimension(orderLine.getStDimension());

        TaxRate taxRate = orderLine.getTax();
        BigDecimal rate = taxRate.getRate().divide(new BigDecimal(100)).setScale(precision,
            RoundingMode.HALF_UP);
        invoiceLine.setTaxAmount(
            orderLine.getLineNetAmount().multiply(rate).setScale(precision, RoundingMode.HALF_UP));

        OBDal.getInstance().save(invoiceLine);
        OBDal.getInstance().flush();

        lineNo = lineNo + 10;
      }

      if (orderEmailTo.length() > 0) {
        invoice.setEeiEmailTo(orderEmailTo);
      }

      OBDal.getInstance().commitAndClose();

      return request;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Sfpsi_ReconcileSalesOrder: " + e.getMessage());
    }
    return new JSONObject();
  }
}