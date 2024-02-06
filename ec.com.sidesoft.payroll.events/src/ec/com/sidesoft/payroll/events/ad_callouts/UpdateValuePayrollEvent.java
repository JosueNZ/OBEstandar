package ec.com.sidesoft.payroll.events.ad_callouts;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.service.db.DalConnectionProvider;

public class UpdateValuePayrollEvent extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strMaintenanceNewsId = info.getStringParameter("inpspevMaintenanceNewsId", null);
    String value = getId(strMaintenanceNewsId);

    if (value != null) {
      BigDecimal rmu = new BigDecimal(value);
      info.addResult("inpvalue", rmu.toString());
    } else {
      info.addResult("inpvalue", 0);
    }
  }

  private static String getId(String spev_maintenance_news_id) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = null;
    try {

      String strSql = "SELECT coalesce(to_number(p.value),0) AS DefaultValue \n"
          + "FROM spev_maintenance_news t  \n"
          + "LEFT JOIN spev_config_news p on t.spev_maintenance_news_id = p.spev_maintenance_news_id \n"
          + "WHERE t.spev_maintenance_news_id = '" + spev_maintenance_news_id + "'"
          + "and t.valid = 'N'";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("DefaultValue");
      }

      return strResult;

    } catch (Exception e) {

      throw new OBException("Error al consultar el valor de la Novedad " + e.getMessage());
    }

  }

}
