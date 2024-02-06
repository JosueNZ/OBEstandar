package com.sidesoft.hrm.payroll.action_handler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;

import com.sidesoft.hrm.payroll.ssprline_loans;

public class Sspr_AdvancePayment extends BaseProcessActionHandler {

	private static final Logger log = Logger.getLogger(Sspr_AdvancePayment.class);

	@Override
	protected JSONObject doExecute(Map<String, Object> parameters, String content) {
		try {
			JSONObject request = new JSONObject(content);
			JSONObject params = new JSONObject(request.getString("_params"));
			JSONObject window = new JSONObject(params.getString("window"));
			JSONArray selection = new JSONArray(window.getString("_selection"));
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			for (int i = 0; i < selection.length(); i++) {
				JSONObject row = selection.getJSONObject(i);
				ssprline_loans line = OBDal.getInstance().get(ssprline_loans.class, row.getString("id"));
				String strPayDate = row.getString("paydate");
				try {
					line.setPaydate(df.parse(strPayDate));
				} catch (Exception e) {
					log.error("Error Sspr_PreCancellation: " + e.getMessage(), e);
				}
				OBDal.getInstance().save(line);
			}
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();
			return request;
		} catch (Exception e) {
			OBDal.getInstance().rollbackAndClose();
			log.error("Error Sspr_PreCancellation: " + e.getMessage(), e);
		}
		return new JSONObject();
	}
}