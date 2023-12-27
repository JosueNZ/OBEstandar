package ec.com.sidesoft.accounting.process;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.common.actionhandler.ResetAccountingHandler;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResetAccountingDocumentTypeHandler extends ResetAccountingHandler {

  private static final Logger log = LoggerFactory
      .getLogger(ResetAccountingDocumentTypeHandler.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject result = null;
    try {
      result = new JSONObject();
      JSONObject request = new JSONObject(content);
      JSONObject params = request.getJSONObject("_params");

      String adClientId = params.getString("AD_Client_ID");
      String adOrgId = params.getString("AD_Org_ID");
      String documenType = params.getString("C_DocType_ID");
      String deletePosting = params.getString("DeletePosting");
      String datefrom = StringUtils.equals(params.getString("datefrom"), "null") ? "" : OBDateUtils
          .formatDate(JsonUtils.createDateFormat().parse(params.getString("datefrom")));
      String dateto = StringUtils.equals(params.getString("dateto"), "null") ? "" : OBDateUtils
          .formatDate(JsonUtils.createDateFormat().parse(params.getString("dateto")));
      List<String> tableIdsList = new ArrayList<String>();
      tableIdsList.add(params.getString("AD_Table_ID"));

      if (documenType != "null")
        ResetAccountingUtil.setDocumenTypeId(documenType);
      else
        ResetAccountingUtil.setDocumenTypeId(null);

      HashMap<String, Integer> results = new HashMap<String, Integer>();
      if (StringUtils.equals(deletePosting, "true")) {
        results = ResetAccountingUtil.delete(adClientId, adOrgId, tableIdsList, datefrom, dateto);
      } else {
        results = ResetAccountingUtil.restore(adClientId, adOrgId, tableIdsList, datefrom, dateto);
      }

      int counter = results.get("updated");
      int counterDeleted = results.get("deleted");
      JSONObject successMessage = new JSONObject();
      successMessage.put("severity", "success");
      StringBuilder message = new StringBuilder();
      message.append(OBMessageUtils.parseTranslation("@UnpostedDocuments@ = " + counter
          + ", @DeletedEntries@ = " + counterDeleted));
      successMessage.put("text", message);
      result.put("message", successMessage);

    } catch (Exception e) {
      log.error("Error in Reset Accounting Action Handler", e);
      try {
        result = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        result.put("message", errorMessage);
      } catch (Exception e2) {
        log.error(e.getMessage(), e2);
      }
    }
    return result;
  }

}
