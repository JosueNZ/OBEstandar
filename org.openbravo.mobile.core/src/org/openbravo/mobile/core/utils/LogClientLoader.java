/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.UtilSql;
import org.openbravo.database.SessionInfo;
import org.openbravo.mobile.core.obmobcLogClient;
import org.openbravo.mobile.core.process.JSONProcessSimple;
import org.openbravo.service.db.QueryTimeOutUtil;
import org.openbravo.service.json.JsonConstants;

public class LogClientLoader extends JSONProcessSimple {
  private static final Logger log = Logger.getLogger(LogClientLoader.class);

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    JSONArray jsonarraylogclient = jsonsent.getJSONArray("logclient");

    long t1 = System.currentTimeMillis();
    JSONObject result = this.saveLogClient(jsonarraylogclient);
    log.debug("Final total time: " + (System.currentTimeMillis() - t1));
    return result;
  }

  private JSONObject saveLogClient(JSONArray jsonarray) throws JSONException {
    boolean error = false;
    OBContext.setAdminMode(true);
    long t1 = System.currentTimeMillis();
    try {
      for (int i = 0; i < jsonarray.length(); i++) {
        JSONObject jsonlogclient = jsonarray.getJSONObject(i);
        error |= saveLogClient(jsonlogclient);
      }
      log.debug("Total log import time: " + (System.currentTimeMillis() - t1));
    } finally {
      OBContext.restorePreviousMode();
    }
    JSONObject jsonResponse = new JSONObject();
    if (!error) {
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      jsonResponse.put("result", "0");
    } else {
      jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
      jsonResponse.put("result", "0");
    }
    return jsonResponse;
  }

  private boolean saveLogClient(JSONObject jsonlogclient) throws JSONException {
    boolean error = false;

    long t0 = System.currentTimeMillis();

    Connection connection = OBDal.getInstance().getConnection();

    final String strClientId = OBContext.getOBContext().getCurrentClient().getId();
    final String strOrgId = OBContext.getOBContext().getCurrentOrganization().getId();

    final String strId = jsonlogclient.getString("id");
    final String jsonMsgString = jsonlogclient.getString("json");
    final JSONObject jsonMsg = new JSONObject(jsonMsgString);

    final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

    final String strCreated = dateFormatter.format(new Date(jsonMsg.getLong("created")));
    final String strCreatedBy;
    if (jsonMsg.has("createdby")) {
      strCreatedBy = jsonMsg.getString("createdby");
    } else {
      strCreatedBy = OBContext.getOBContext().getUser().getId();
    }
    final String strDeviceId = jsonMsg.getString("deviceId");
    final String strMsg = jsonMsg.getString("msg");
    final String strLogLevel = jsonMsg.getString("loglevel");
    final String strCacheSessionId = jsonMsg.getString("cacheSessionId");
    final String isOnline;
    if (jsonMsg.has("isOnline")) {
      isOnline = (jsonMsg.getString("isOnline").equals("true") ? "Y" : "N");
    } else {
      isOnline = "Y"; // default value, since cannot be null
    }

    String strSql = "";
    strSql = strSql
        + "        insert into obmobc_logclient"
        + "          (obmobc_logclient_id, ad_client_id, ad_org_id, isactive,"
        + "           created, createdby," //
        + "           updated, updatedby,"
        + "           deviceid, msg, loglevel, cache_session_id, isonline) values"
        + "           (?, ?, ?, 'Y',"
        + "           to_timestamp(to_char(?), to_char('DD-MM-YYYY HH24:MI:SS')), ?,"
        + "           to_timestamp(to_char(?), to_char('DD-MM-YYYY HH24:MI:SS')), ?,"
        + "           ?, ?, ?, ?, ?)";

    PreparedStatement st = null;

    try {
      st = connection.prepareStatement(strSql);
      QueryTimeOutUtil.getInstance().setQueryTimeOut(st, SessionInfo.getQueryProfile());
      UtilSql.setValue(st, 1, 12, null, strId);
      UtilSql.setValue(st, 2, 12, null, strClientId);
      UtilSql.setValue(st, 3, 12, null, strOrgId);
      UtilSql.setValue(st, 4, 12, null, strCreated);
      UtilSql.setValue(st, 5, 12, null, strCreatedBy);
      UtilSql.setValue(st, 6, 12, null, strCreated);
      UtilSql.setValue(st, 7, 12, null, strCreatedBy);
      UtilSql.setValue(st, 8, 12, null, strDeviceId);
      UtilSql.setValue(st, 9, 12, null, strMsg);
      UtilSql.setValue(st, 10, 12, null, strLogLevel);
      UtilSql.setValue(st, 11, 12, null, strCacheSessionId);
      UtilSql.setValue(st, 12, 12, null, isOnline);

      st.executeUpdate();

      connection.commit();
    } catch (Exception e) {
      try {
        connection.rollback();
      } catch (SQLException e1) {
        log.error("rollback fail", e);
      }
      // Force execute query to db
      OBQuery<obmobcLogClient> records = OBDal.getInstance().createQuery(obmobcLogClient.class,
          "id=:logid");
      records.setNamedParameter("logid", jsonlogclient.getString("id"));
      // Org/Client filter will not be done, to ensure that if a logclient record exists and was
      // created in a different, non-accessible organization, the process doesn't mistakenly
      // conclude that it doesn't exist
      records.setFilterOnActive(false);
      records.setFilterOnReadableClients(false);
      records.setFilterOnReadableOrganization(false);
      if (records.count() == 0) {
        log.error("An unexpected error happened when processing a logClient insert: ", e);
        error = true;
      } else {
        error = false;
        log.debug("Found duplicated log client message with id: " + jsonlogclient.getString("id")
            + ". Ignoring");
      }

    } finally {
      try {
        st.close();
      } catch (Exception e) {
        log.error("Error during release Statement of query: " + strSql, e);
      }
    }

    log.debug("Creation of Log Client records: " + (System.currentTimeMillis() - t0));
    return error;
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

  @Override
  protected boolean bypassSecurity() {
    return true;
  }
}
