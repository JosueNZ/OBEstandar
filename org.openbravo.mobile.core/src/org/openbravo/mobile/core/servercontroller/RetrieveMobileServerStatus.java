/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.mobile.core.MobileServerDefinition;
import org.openbravo.mobile.core.process.JSONProcessSimple;

/**
 * This class gets a json request for a status of a server. If the status is requested for the
 * server itself then it returns the status from the database.
 * 
 * If the status is requested from another server then return the status from the database if the
 * last update was not too old. If the last status is too old then request the status from the
 * server itself.
 * 
 * If the request fails then it is assumed that the other server is offline.
 * 
 * The class can handle input json like this: { 'serverKey': 'CAEN' }.
 * 
 * it returns the status like this: { 'serverKey': 'CAEN' , 'serverStatus': 'OFF'}
 * 
 * Status can have different values:
 * 
 * - OFF means offline
 * 
 * - ON means online
 * 
 * - TON means transitioning to online
 * 
 * - TOFF means transitioning to offline
 * 
 * @author mtaal
 */

@AuthenticationManager.Stateless
public class RetrieveMobileServerStatus extends JSONProcessSimple {

  public JSONObject exec(JSONObject jsonSent) throws JSONException, ServletException {
    final JSONObject result = new JSONObject();

    int maxAge = MobileServerUtils.getIntegerPreference("OBMOBC_ServerStatusMaxAge", 300000);

    final String serverKey = jsonSent.getString("serverKey");
    result.put("serverKey", serverKey);
    String status = MobileServerState.OFFLINE.getValue();
    String errorMsg = null;
    try {
      OBContext.setAdminMode(false);

      final MobileServerDefinition serverDef = getServerDefinition(serverKey);
      if (serverDef != null) {
        status = serverDef.getStatus();

        // if too old then update the status
        // and only call the server if we are not the requested server
        final boolean thisIsTheRequestedServer = MobileServerController.getInstance()
            .getMobileServerKey().equals(serverKey);
        if (!thisIsTheRequestedServer
            && (System.currentTimeMillis() - serverDef.getUpdated().getTime()) > maxAge) {
          final JSONObject reqResult = MobileServerRequestExecutor.getInstance()
              .executeServerRequest(serverKey, MobileServerUtils.OBWSPATH + getClass().getName(),
                  jsonSent);
          if (reqResult != null && reqResult.has("serverStatus")) {
            status = reqResult.getString("serverStatus");

            // we have a new status update it
            MobileServerStatusUpdateHandler.getInstance().setServerStatus(serverKey, status);
          } else {
            status = MobileServerState.OFFLINE.getValue();
          }
        }
      }
    } catch (Throwable t) {
      status = MobileServerState.OFFLINE.getValue();
      errorMsg = t.getMessage();
    } finally {
      OBContext.restorePreviousMode();
    }
    result.put("serverStatus", status);
    if (errorMsg != null) {
      result.put("error", errorMsg);
    }
    return result;
  }

  private MobileServerDefinition getServerDefinition(String serverKey) {
    final OBQuery<MobileServerDefinition> query = OBDal.getInstance().createQuery(
        MobileServerDefinition.class,
        MobileServerDefinition.PROPERTY_MOBILESERVERKEY + "=:serverKey");
    query.setFilterOnReadableClients(false);
    query.setFilterOnReadableOrganization(false);
    query.setNamedParameter("serverKey", serverKey);
    final MobileServerDefinition server = query.uniqueResult();
    return server;
  }

  // don't send any context information
  protected JSONObject getContextInformation() {
    return null;
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
