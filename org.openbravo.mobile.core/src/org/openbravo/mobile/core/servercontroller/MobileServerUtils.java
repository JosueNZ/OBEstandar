/*
 ************************************************************************************
 * Copyright (C) 2015-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.AllowedCrossDomainsHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.mobile.core.MobileServerDefinition;
import org.openbravo.mobile.core.authenticate.MobileAuthenticationKeyUtils;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * @author mtaal
 */
public class MobileServerUtils {

  private static final Logger log = Logger.getLogger(MobileServerUtils.class);

  public static final String MOBILE_SERVER_KEY = "mobile.server.key";
  public static final String MAIN_SERVER = "MAIN";
  public static final String STORE_SERVER = "STORE";
  public static final String OBWSPATH = "/org.openbravo.mobile.core.service.jsonrest/";

  private static Boolean multiServerControllerEnabled = null;

  public static boolean isInvalidOrigin(HttpServlet servlet, HttpServletRequest request,
      HttpServletResponse response) {
    try {
      // only return invalid origin error in case not yet authenticated and in multi-server
      // environment, and when invalid
      // in case already authenticated then the logic of setting the right cors header will
      // take care of preventing illegal requests
      boolean invalidOrigin = !OBMOBCUtils.isAuthenticated(servlet, request, response);
      invalidOrigin &= MobileServerUtils.isMultiServerEnabled();
      invalidOrigin &= MobileServerController.getInstance().isThisACentralServer()
          || MobileServerController.getInstance().isThisAStoreServer();
      invalidOrigin &= AllowedCrossDomainsHandler.getInstance().isCheckedInvalidOrigin(request);
      return invalidOrigin;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Set a temporary CORS header which can be used to return an error to the client in case of invalid cross domain requests.
   */
  public static void setTemporaryCORSHeaders(HttpServletRequest request,
      HttpServletResponse response) {
    // cross domain not allowed but still allow it for a short time so that it can
    // be reported to the user
    // set some headers to let the response take it in any case.
    response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
    response.setHeader("Access-Control-Allow-Credentials", "true");
    response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
    response.setHeader("Access-Control-Allow-Headers",
        "Content-Type, Origin, Accept, X-Requested-With, Access-Control-Allow-Credentials");

    // only valid for 1 second
    response.setHeader("Access-Control-Max-Age", "1");
  }

  /**
   * Return true if the passed server is a main server.
   */
  public static boolean isTriggerStateServer(MobileServerDefinition server) {
    return MAIN_SERVER.equals(server.getServerType());
  }

  /**
   * @deprecated use {{@link #isMultiServerEnabled()} method
   */
  public static boolean isMobileServerControllerEnabled() {
    return isMultiServerEnabled();
  }

  /**
   * Return true if the preference OBMOBC_MultiServerArchitecture is set.
   */
  public static boolean isMultiServerEnabled(Client client, Organization organization) {
    boolean multiServerEnabled = false;
    try {
      OBContext.setAdminMode(false);
      multiServerEnabled = "Y".equals(Preferences.getPreferenceValue(
          "OBMOBC_MultiServerArchitecture", true, client, organization, null, null, null).trim());
      // check if there are any wrongly defined preferences
      final Query qry = OBDal
          .getInstance()
          .getSession()
          .createQuery(
              "from " + Preference.ENTITY_NAME + " where " + Preference.PROPERTY_PROPERTY
                  + "='OBMOBC_MultiServerArchitecture'");
      for (Object o : qry.list()) {
        final Preference p = (Preference) o;
        if (!"0".equals(p.getClient().getId()) || !"0".equals(p.getOrganization().getId())) {
          log.error("The preference "
              + p
              + " has a non-zero client or org, this is wrong, these preferences are ignored, define them using client/org 0.");
        } else if (p.getVisibleAtClient() != null || p.getVisibleAtOrganization() != null
            || p.getVisibleAtRole() != null || p.getUserContact() != null) {
          log.error("The preference "
              + p
              + " has a visibility filter set, this is wrong, this preference is ignored, define it without visibility filters");
        }
      }

    } catch (PropertyException ignore) {
      // ignore on purpose
    } finally {
      OBContext.restorePreviousMode();
    }
    return multiServerEnabled;
  }

  /**
   * Return true if the preference OBMOBC_MultiServerArchitecture is set.
   */
  public static boolean isMultiServerEnabled() {
    if (multiServerControllerEnabled == null) {
      // is sometimes called without a context even, before login, therefore
      // always use admin mode
      OBContext.setAdminMode(false);
      try {
        multiServerControllerEnabled = isMultiServerEnabled(OBContext.getOBContext()
            .getCurrentClient(), OBContext.getOBContext().getCurrentOrganization());
      } finally {
        OBContext.restorePreviousMode();
      }
    }
    return multiServerControllerEnabled;
  }

  public static Integer getIntegerPreference(String preference, Integer defaultVal) {
    try {
      OBContext.setAdminMode(false);
      final String value = Preferences.getPreferenceValue(preference, true, OBContext
          .getOBContext().getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(),
          OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
      return Integer.parseInt(value);
    } catch (Exception e) {
      return defaultVal;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static Boolean getBooleanPreference(String preference, Boolean defaultVal) {
    try {
      OBContext.setAdminMode(false);
      final String value = Preferences.getPreferenceValue(preference, true, OBContext
          .getOBContext().getCurrentClient(), OBContext.getOBContext().getCurrentOrganization(),
          OBContext.getOBContext().getUser(), OBContext.getOBContext().getRole(), null);
      if (value == null || value.trim().length() == 0) {
        return defaultVal;
      }
      if (!"N".equals(value) && !"Y".equals(value)) {
        log.error("The preference " + preference
            + " does not have an expected boolean value Y or N, but " + value
            + " will try our best to parse it anyway to a boolean");
      }
      return "Y".equals(value);
    } catch (Exception e) {
      return defaultVal;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Get the server status from the json and update the corresponding server with it.
   */
  public static void updateServerStatusFromJSON(JSONObject json) {
    try {
      if (json.has("mobileServerKey") && json.has("serverStatus")) {
        MobileServerStatusUpdateHandler.getInstance().setServerStatus(
            (String) json.get("mobileServerKey"), (String) json.get("serverStatus"));
      }
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  public static boolean isOpenbravoServer(MobileServerDefinition serverDef) {
    return (serverDef.getServerType().equals(MAIN_SERVER) || serverDef.getServerType().equals(
        STORE_SERVER));
  }

  public static String getAuthenticationQueryParams() {
    final OBContext obContext = OBContext.getOBContext();
    return getAuthenticationQueryParams(obContext.getCurrentClient().getId(), obContext
        .getCurrentOrganization().getId(), obContext.getRole().getId(), obContext.getUser().getId());
  }

  public static String getAuthenticationQueryParams(String clientId, String orgId, String roleId,
      String userId) {
    try {
      String authenticationClient = URLEncoder.encode(clientId, "UTF-8");
      String authenticationToken = URLEncoder.encode(MobileAuthenticationKeyUtils
          .getEncryptedAuthenticationToken(clientId, orgId, userId, roleId, "-1"), "UTF-8");
      return "authenticationClient=" + authenticationClient + "&authenticationToken="
          + authenticationToken;
    } catch (UnsupportedEncodingException e) {
      throw new OBException(e);
    }
  }
}
