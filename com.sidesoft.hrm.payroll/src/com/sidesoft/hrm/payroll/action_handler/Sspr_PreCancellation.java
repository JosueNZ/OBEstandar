package com.sidesoft.hrm.payroll.action_handler;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;

import com.sidesoft.hrm.payroll.ssprline_loans;

public class Sspr_PreCancellation extends BaseProcessActionHandler {

	private static final Logger log = Logger.getLogger(Sspr_PreCancellation.class);

	@Override
	protected JSONObject doExecute(Map<String, Object> parameters, String content) {
		try {
			JSONObject request = new JSONObject(content);
			JSONObject params = new JSONObject(request.getString("_params"));
			JSONObject window = new JSONObject(params.getString("window"));
			JSONArray selection = new JSONArray(window.getString("_selection"));
			for (int i = 0; i < selection.length(); i++) {
				JSONObject row = selection.getJSONObject(i);
				if (row.getBoolean("manualCancellation")) {
					ssprline_loans line = OBDal.getInstance().get(ssprline_loans.class, row.getString("id"));
					line.setManualCancellation(row.getBoolean("manualCancellation"));
					line.setStatus("Cancel");
					OBDal.getInstance().save(line);
				}
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