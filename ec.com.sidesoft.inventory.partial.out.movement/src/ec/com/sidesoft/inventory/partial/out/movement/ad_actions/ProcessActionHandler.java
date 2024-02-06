package ec.com.sidesoft.inventory.partial.out.movement.ad_actions;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;

public class ProcessActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    OBContext.setAdminMode(true);
    JSONObject response = new JSONObject();
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider con = new DalConnectionProvider(false);

    try {
      final JSONObject jsonData = new JSONObject(data);
      final JSONArray rows = jsonData.getJSONArray("rows");

      for (int i = 0; i < rows.length(); i++) {
        String result = process(rows.getString(i));
        if (!result.equals("OK")) {
          throw new OBException(result);
        }
      }

      OBDal.getInstance().flush();
      response.put("status", "OK");
    } catch (Exception e) {
      System.out.println("ProcessActionHandler: "
          + Utility.messageBD(con, e.getMessage().replace("@", ""), language));
      try {
        response.put("status", "ERROR");
        response.put("message", Utility.messageBD(con, e.getMessage().replace("@", ""), language));

      } catch (Exception e2) {
      }
    }
    OBContext.setAdminMode(false);
    return response;
  }

  private String process(String recordId) {
    String result = null;
    ConnectionProvider conn = new DalConnectionProvider(false);
    try {
      String sql = "SELECT ssipotm_process(?) AS result";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(sql);
      st.setString(1, recordId);
      ResultSet resultSet = st.executeQuery();

      while (resultSet.next()) {
        result = resultSet.getString("result");
      }
    } catch (Exception e) {
      throw new OBException("process: " + e.getMessage());
    }
    return result;
  }
}
