package ec.com.sidesoft.ecuador.asset.allocation.advanced.actionhandler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBLedgerUtils;
import org.openbravo.model.financialmgmt.accounting.coa.AcctSchema;

public class BarcodeActionHandler extends BaseActionHandler {

  private static final Logger log = Logger
      .getLogger(BarcodeActionHandler.class);

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject resultMessage = new JSONObject();
    JSONObject errorMessage = new JSONObject();
    try {

      JSONObject jsonContent = new JSONObject(content);
      JSONObject data = new JSONObject(content);
      String orgId = jsonContent.getString("organization");

      String generalLedgerId = OBLedgerUtils.getOrgLedger(orgId);
      AcctSchema generalLedger = OBDal.getInstance().get(AcctSchema.class, generalLedgerId);

      data.put("value", generalLedgerId);
      data.put("identifier", generalLedger.getName());
      resultMessage.put("response", data);

    } catch (JSONException e) {
      log.error("Error creating JSON Object ", e);
      e.printStackTrace();
      try {
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", "Error");
        errorMessage.put("text", e.getMessage());
        resultMessage.put("message", errorMessage);
      } catch (JSONException e1) {
        log.error("Error creating JSON Object ", e);
      }
    }

    return resultMessage;
  }

}
