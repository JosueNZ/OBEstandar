/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.authenticate;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.process.JSONProcessSimple;
import org.openbravo.service.json.JsonConstants;

public class GetToken extends JSONProcessSimple {

  private static final Logger logger = Logger.getLogger(GetToken.class);

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    final JSONObject result = new JSONObject();
    try {
      OBContext.setAdminMode(false);
      if (RequestContext.get().getSession().getAttribute("#Authenticated_user") == null) {
        throw new AuthenticationException("User not authenticated");
      }

      final JSONObject response = new JSONObject();
      // not authenticated by token, so must be real authentication
      if (RequestContext.get().getSessionAttribute(
          MobileAuthenticationKeyUtils.AUTHENTICATED_BY_TOKEN_SESSION_PARAM) == null) {
        final OBContext obContext = OBContext.getOBContext();
        String token = MobileAuthenticationKeyUtils.getEncryptedAuthenticationToken(obContext
            .getCurrentClient().getId(), obContext.getCurrentOrganization().getId(), obContext
            .getUser().getId(), obContext.getRole().getId(), jsonsent.optString("pos"));
        response.put("success", true);
        response.put(MobileAuthenticationKeyUtils.AUTHENTICATION_TOKEN_PARAM, token);
        response.put(MobileAuthenticationKeyUtils.AUTHENTICATION_CLIENT_PARAM, obContext
            .getCurrentClient().getId());
      }
      // else: if authenticated by token then don't generate a new token, just return
      // empty response

      result.put(JsonConstants.RESPONSE_DATA, response);
      result.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_SUCCESS);
      return result;
    } catch (Exception e) {
      // log, but for the rest just return anything
      logger.error(e.getMessage(), e);
      result.put("success", false);
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
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
