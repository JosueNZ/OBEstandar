package ec.com.sidesoft.localization.finance.reports;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.client.application.report.BaseReportActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;

public class FinanceReports extends BaseReportActionHandler {

  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonContent = null;
    Organization org = null;
    try {
      OBCriteria<OrganizationType> crtOrgType = OBDal.getInstance()
          .createCriteria(OrganizationType.class);
      crtOrgType.add(Restrictions.eq(OrganizationType.PROPERTY_LEGALENTITY, true));
      crtOrgType.add(Restrictions.eq(OrganizationType.PROPERTY_LEGALENTITYWITHACCOUNTING, true));
      crtOrgType.setMaxResults(1);
      if (crtOrgType.count() > 0) {
        OrganizationType orgType = (OrganizationType) crtOrgType.uniqueResult();
        org = orgType.getOrganizationList().get(0);
      }

      jsonContent = new JSONObject(content);
      JSONObject params = jsonContent.getJSONObject("_params");
      final String bpartner_id = (String) params.get("bpartner_id");
      BusinessPartner businessPartner = OBDal.getInstance().get(BusinessPartner.class, bpartner_id);
      params.put("codigo", businessPartner.getTaxID());
      if (org != null) {
        final String orgNameCurrent = org.getName();
        params.put("org_name", orgNameCurrent);
        final String orgIdCurrent = org.getId();
        params.put("org_id", orgIdCurrent);
      }

      final String userNameCurrent = OBContext.getOBContext().getUser().getName();
      params.put("user", userNameCurrent);
      String currentDate = OBDateUtils.formatDate(new Date(), "dd/MM/yyyy HH:mm:ss");
      params.put("current_date", currentDate);
      BigDecimal initialBalace = getInitialBalance(params);
      params.put("initial_balance", initialBalace);
      jsonContent.put("_params", params);
    } catch (Exception e) {
      e.printStackTrace();
    }

    return super.doExecute(parameters, jsonContent.toString());
  }

  private BigDecimal getInitialBalance(JSONObject params)
      throws ServletException, JSONException, ParseException {
    final String accounts = commaSeparated(params.getJSONArray("accounts"));
    BigDecimal initialBalace = BigDecimal.ZERO;
    InitialBalanceData balanceData[] = null;
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();

    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date dateFrom = outputFormat.parse(params.getString("date_from"));

    balanceData = InitialBalanceData.select(readOnlyCP,
        params.getString("issotrx").equals("false") ? "N" : "Y", params.getString("bpartner_id"),
        OBDateUtils.formatDate(dateFrom, "dd-MM-yyyy"), accounts);

    initialBalace = balanceData.length > 0 && balanceData[0].ibalance != ""
        ? new BigDecimal(balanceData[0].ibalance)
        : initialBalace;

    return initialBalace;
  }

  private String commaSeparated(final JSONArray accountJSONArray) throws JSONException {
    String accountIdscommaSeparated = "";

    for (int i = 0; i < accountJSONArray.length(); i++) {
      if (i > 0) {
        accountIdscommaSeparated += ", ";
      }
      String value = accountJSONArray.getString(i);
      accountIdscommaSeparated += "'" + value + "'";
    }
    return accountIdscommaSeparated;
  }

}
