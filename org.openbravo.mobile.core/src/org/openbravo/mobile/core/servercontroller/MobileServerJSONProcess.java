/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.mobile.core.process.JSONProcessSimple;

/**
 * The base class for implementing services which are to be executed on this server and should
 * behave differently depending on the state of the server.
 * 
 * @author mtaal
 */
public abstract class MobileServerJSONProcess extends JSONProcessSimple {

  @Override
  public JSONObject exec(JSONObject json) {
    try {
      JSONObject result = new JSONObject();

      if (multiServerNotConfigured()) {
        // in single server installations the server cannot be offline
        return executeWhenOnline(json);
      }

      // if the request has server status info then make use of it to update the
      // server status
      MobileServerUtils.updateServerStatusFromJSON(json);

      switch (MobileServerController.getInstance().getThisMobileServerState()) {
      case ONLINE:
        result = executeWhenOnline(json);
        break;
      case OFFLINE:
        result = executeWhenOffline(json);
        break;
      case TRANSITION_TO_OFFLINE:
        if (MobileServerUtils.isMultiServerEnabled()) {
          result = executeWhenTransitioningtoOffline(json);
          result.put("serverStatusSignal", MobileServerController.getInstance()
              .getThisMobileServerState().getValue());
        } else {
          result = executeWhenOffline(json);
        }
        break;
      case TRANSITION_TO_ONLINE:
        if (MobileServerUtils.isMultiServerEnabled()) {
          result = executeWhenTransitioningtoOnline(json);
          result.put("serverStatusSignal", MobileServerController.getInstance()
              .getThisMobileServerState().getValue());
        } else {
          result = executeWhenOnline(json);
        }
        break;
      }
      return result;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  private boolean multiServerNotConfigured() {
    return !MobileServerUtils.isMultiServerEnabled()
        || MobileServerController.getInstance().getThisServerDefinitionId() == null;
  }

  /**
   * This method will be used when the server is in {@link MobileServerState#ONLINE} state.
   * 
   * @param json
   *          json send in from the client to process
   * @return result
   */
  protected abstract JSONObject executeWhenOnline(JSONObject json);

  /**
   * This method will be used when the server is in {@link MobileServerState#OFFLINE} state.
   * 
   * @param json
   *          json send in from the client to process
   * @return result
   */
  protected JSONObject executeWhenOffline(JSONObject json) throws JSONException, ServletException {
    return executeWhenOnline(json);
  }

  /**
   * This method will be used when the server is in {@link MobileServerState#TRANSITION_TO_OFFLINE}
   * state.
   * 
   * @param json
   *          json send in from the client to process
   * @return result
   */
  protected JSONObject executeWhenTransitioningtoOffline(JSONObject json) {
    return new JSONObject();
  }

  /**
   * This method will be used when the server is in {@link MobileServerState#TRANSITION_TO_ONLINE}
   * state.
   * 
   * @param json
   *          json send in from the client to process
   * @return result
   */
  protected JSONObject executeWhenTransitioningtoOnline(JSONObject json) {
    return new JSONObject();
  }

}
