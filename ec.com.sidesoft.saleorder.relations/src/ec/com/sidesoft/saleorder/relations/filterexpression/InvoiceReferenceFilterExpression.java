package ec.com.sidesoft.saleorder.relations.filterexpression;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.FilterExpression;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;

public class InvoiceReferenceFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    JSONObject request;
    try {
      request = new JSONObject(requestMap.get("context"));
      final Invoice invoice = OBDal.getInstance().get(Invoice.class, request.get("inpcInvoiceId"));
      return invoice.getScnrInvoice() != null ? invoice.getScnrInvoice().getId() : "";
    } catch (Exception e) {
      throw new OBException(e.getMessage());
    }
  }

}
