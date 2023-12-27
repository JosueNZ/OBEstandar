package ec.com.sidesoft.payroll.events.ad_process;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.hrm.payroll.Concept;

import ec.com.sidesoft.payroll.events.SPEVConfigNews;
import ec.com.sidesoft.payroll.events.SPEVDetailNews;
import ec.com.sidesoft.payroll.events.SPEVRegisterNews;
import ec.com.sidesoft.payroll.events.SPEVRegisterNewsline;
//import ec.com.sidesoft.retail.combo.process.ProcessOrder;

public class ProcessPayrollEvent extends BaseProcessActionHandler {
  private static Logger log = Logger.getLogger(ProcessPayrollEvent.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    try {

      JSONObject contentObject = new JSONObject(content);

      String registerNewsId = contentObject.getString("inpspevRegisterNewsId");

      SPEVRegisterNews registerNews = OBDal.getInstance().get(SPEVRegisterNews.class,
          registerNewsId);

      if (registerNews.getState().equals("PR")) {

        return createResult(MessageType.ERROR,
            OBMessageUtils.getI18NMessage("SPEV_is_processed", null));

      } else {

        Organization ad_org_id = registerNews.getOrganization();
        Client ad_client_id = registerNews.getClient();
        User ad_user_id = registerNews.getCreatedBy();
        Date date = registerNews.getDateRegister();
        String documentNo = registerNews.getDoumentno();
        Concept accountingConcept = null;
        String accountingConceptType = "";
        SPEVConfigNews configurationNewsId = null;

        Map<SPEVDetailNews, BigDecimal> detailInserts = new HashMap<SPEVDetailNews, BigDecimal>();

        List<SPEVRegisterNewsline> lines = registerNews.getSPEVRegisterNewslineList();

        for (SPEVRegisterNewsline line : lines) {

          UUID uuid = UUID.randomUUID();
          String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

          OBCriteria<SPEVConfigNews> config = OBDal.getInstance()
              .createCriteria(SPEVConfigNews.class);
          config.add(Restrictions.eq(SPEVConfigNews.PROPERTY_SPEVMAINTENANCENEWS,
              line.getSpevMaintenanceNews()));
          config.setMaxResults(1);

          if (config.list() != null && config.list().size() > 0) {
            configurationNewsId = OBDal.getInstance().get(SPEVConfigNews.class,
                config.list().get(0).getId());
            accountingConcept = config.list().get(0).getSsprConcept();
            accountingConceptType = config.list().get(0).getSsprConcept().getConceptsubtype();
          }

          // PENDIENTE POR HACER
          Period period = null;
          String cPeriodID = getPeriod(date);
          if (cPeriodID != null) {
            period = OBDal.getInstance().get(Period.class, cPeriodID);
          }

          SPEVDetailNews detail = OBProvider.getInstance().get(SPEVDetailNews.class);
          detail.setNewOBObject(true);
          detail.setId(randomUUIDString);
          detail.setOrganization(ad_org_id);
          detail.setClient(ad_client_id);
          detail.setCreatedBy(ad_user_id);
          detail.setUpdatedBy(ad_user_id);
          detail.setDateDetail(date);
          detail.setDoumentno(documentNo);
          detail.setBpartner(line.getBpartner());
          detail.setSpevConfigNews(configurationNewsId);
          detail.setSpevMaintenanceNews(line.getSpevMaintenanceNews());
          detail.setSsprConcept(accountingConcept);
          detail.setConceptType(accountingConceptType);
          detail.setValue(line.getValue());
          detail.setType("BR");
          detail.setProcess("MN");
          detail.setPeriod(period);
          detail.setCostcenter(line.getBpartner().getSsprCostcenter());
          OBDal.getInstance().save(detail);
          OBDal.getInstance().flush();

          detailInserts.put(detail, line.getValue());
        }

        if (!detailInserts.isEmpty()) {
          SPEVRegisterNews updateRegisterNews = OBDal.getInstance().get(SPEVRegisterNews.class,
              registerNewsId);
          updateRegisterNews.setState("PR");
          updateRegisterNews.setProcessed(true);
          OBDal.getInstance().save(updateRegisterNews);
          OBDal.getInstance().flush();
          return createResult(MessageType.SUCCESS,
              OBMessageUtils.getI18NMessage("SPEV_process_success", null));
        } else {
          return createResult(MessageType.ERROR,
              OBMessageUtils.getI18NMessage("SPEV_process_fail", null));
        }

      }

    } catch (JSONException e) {
      log.error(e.getMessage());
      e.printStackTrace();
      return createResult(MessageType.ERROR, e.getMessage());
    }

  }

  private JSONObject createResult(MessageType messageType, String text) {
    return getResponseBuilder().showMsgInProcessView(messageType, "Message Title", text)
        .refreshGrid().build();
  }

  private static String getPeriod(Date date) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = null;
    String functionName = "spev_penalty";

    try {

      String strSql = "SELECT spev_get_period( '" + date + "', '" + functionName
          + "') as c_period_id FROM DUAL;";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("c_period_id");
      }

      return strResult;

    } catch (Exception e) {
      throw new OBException("Error al consultar canton de la Organizacion. " + e.getMessage());
    }

  }

}
