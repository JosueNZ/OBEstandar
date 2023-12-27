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
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

public class AccumulateActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    OBContext.setAdminMode(true);
    JSONObject response = new JSONObject();

    try {
      final JSONObject jsonData = new JSONObject(data);
      final JSONArray rows = jsonData.getJSONArray("rows");

      Organization org = OBContext.getOBContext().getCurrentOrganization();
      User user = OBContext.getOBContext().getUser();

      for (int i = 0; i < rows.length(); i++) {
        String result = accumulate(org.getId(), user.getId(), rows.getString(i));
        if (!result.equals("OK")) {
          throw new OBException(result);
        }
      }

      OBDal.getInstance().flush();
      response.put("status", "OK");
    } catch (Exception e) {
      System.out.println("AccumulateActionHandler: " + e.getMessage());
      try {
        response.put("status", "ERROR");
        response.put("message", e.getMessage());
      } catch (Exception e2) {
      }
    }
    OBContext.setAdminMode(false);
    return response;
  }

  private String accumulate(String orgId, String userId, String recordId) {
    String result = null;
    ConnectionProvider conn = new DalConnectionProvider(false);
    try {
      String sql = "SELECT ssipotm_accumulate(?,?,?) AS result";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(sql);
      st.setString(1, orgId);
      st.setString(2, userId);
      st.setString(3, recordId);
      ResultSet resultSet = st.executeQuery();

      while (resultSet.next()) {
        result = resultSet.getString("result");
      }
    } catch (Exception e) {
      throw new OBException("loadLines: " + e.getMessage());
    }
    return result;
  }
}
