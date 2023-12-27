package ec.com.sidesoft.saleorder.relations;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InvoiceReferenceActionHandler extends BaseProcessActionHandler {

  private static final Logger log = LoggerFactory.getLogger(InvoiceReferenceActionHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject result = new JSONObject();

    JSONObject request = null;
    try {

      JSONArray actions = new JSONArray();
      JSONObject msgAction = new JSONObject();

      JSONObject msg = new JSONObject();
      OBError message = null;

      request = new JSONObject(content);
      JSONObject params = request.getJSONObject("_params");

      // Retrieve parameters
      String invoiceId = request.getString("C_Invoice_ID");
      JSONObject invoiceLineGrid = params.getJSONObject("invoice_lines");
      JSONArray invoiceLineArray = invoiceLineGrid.getJSONArray("_selection");
      String selectedInvoiceId = params.getString("invoiceReference");

      final Invoice selectedInvoice = OBDal.getInstance().get(Invoice.class, selectedInvoiceId);
      final Invoice currentInvoice = OBDal.getInstance().get(Invoice.class, invoiceId);

      if (currentInvoice.getScnrInvoice() != null
          && !StringUtils.equals(currentInvoice.getScnrInvoice().getId(), selectedInvoiceId)) {
        deleteInvoiceLine(invoiceId);
        currentInvoice.getInvoiceLineList().removeAll(currentInvoice.getInvoiceLineList());
      }

      currentInvoice.setScnrInvoice(selectedInvoice);
      currentInvoice.setSalesOrder(selectedInvoice.getSalesOrder());

      DocumentType documentType = currentInvoice.getTransactionDocument();

      message = createInvoiceLine(invoiceId, invoiceLineArray, selectedInvoiceId,
          documentType.isReturn());

      msg.put("msgType", message.getType().toLowerCase());
      msg.put("msgTitle", message.getTitle());
      msg.put("msgText", message.getMessage());
      msgAction.put("showMsgInView", msg);
      actions.put(msgAction);
      request.put("responseActions", actions);

    } catch (Exception e) {
      try {
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", e.getMessage());
        request.put("message", errorMessage);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }

    }

    return request;
  }

  private OBError createInvoiceLine(final String invoiceId, final JSONArray invoiceLineArray,
      final String selectedInvoiceId, final boolean isReturn) {
    OBError message = new OBError();
    Invoice currentInvoice = OBDal.getInstance().get(Invoice.class, invoiceId);
    try {

      for (int i = 0; i < invoiceLineArray.length(); i++) {
        final String invoiceLineId = invoiceLineArray.getJSONObject(i).getString("id");

        String qtyReturned = (String) invoiceLineArray.getJSONObject(i).get("returned");
        BigDecimal quantityReturned = new BigDecimal(qtyReturned);

        final InvoiceLine invLine = OBDal.getInstance().get(InvoiceLine.class, invoiceLineId);

        OBProvider.getInstance().get(InvoiceLine.class);

        InvoiceLine objNewInvoiceLine = OBProvider.getInstance().get(InvoiceLine.class);
        objNewInvoiceLine.setLineNo(invLine.getLineNo());
        objNewInvoiceLine.setProduct(invLine.getProduct());
        if (isReturn) {
          objNewInvoiceLine.setInvoicedQuantity(new BigDecimal(-1)
              .multiply(invLine.getInvoicedQuantity().subtract(quantityReturned)));
        } else {
          objNewInvoiceLine.setInvoicedQuantity(invLine.getInvoicedQuantity());
        }

        objNewInvoiceLine.setUnitPrice(invLine.getUnitPrice());
        objNewInvoiceLine.setListPrice(invLine.getListPrice());
        objNewInvoiceLine.setUnitPrice(invLine.getUnitPrice());
        objNewInvoiceLine.setLineNetAmount(new BigDecimal(-1).multiply(invLine.getLineNetAmount()));
        objNewInvoiceLine.setPriceLimit(invLine.getPriceLimit());
        objNewInvoiceLine.setUOM(invLine.getUOM());
        objNewInvoiceLine.setTax(invLine.getTax());
        objNewInvoiceLine.setCreationDate(new Date());
        objNewInvoiceLine.setUpdated(new Date());
        objNewInvoiceLine.setCreatedBy(OBContext.getOBContext().getUser());
        objNewInvoiceLine.setUpdatedBy(OBContext.getOBContext().getUser());
        objNewInvoiceLine.setInvoice(currentInvoice);
        objNewInvoiceLine.setSalesOrderLine(invLine.getSalesOrderLine());
        objNewInvoiceLine.setNewOBObject(true);
        objNewInvoiceLine.setSsorelInvlineid(invLine.getId());
        objNewInvoiceLine.setOrganization(currentInvoice.getOrganization());

        OBDal.getInstance().save(objNewInvoiceLine);
        currentInvoice.getInvoiceLineList().add(objNewInvoiceLine);

      }

      OBDal.getInstance().flush();

    } catch (Exception e) {
      throw new OBException(e.getMessage());
    }

    message.setType("Success");
    message.setTitle(OBMessageUtils.messageBD("success"));

    return message;
  }

  private void deleteInvoiceLine(final String invoiceId) {
    final String deleteHql = "delete from InvoiceLine where invoice.id = :invoiceId ";
    final Query deleteQry = OBDal.getInstance().getSession().createQuery(deleteHql.toString());
    deleteQry.setParameter("invoiceId", invoiceId);
    deleteQry.executeUpdate();
    OBDal.getInstance().flush();
  }

}
