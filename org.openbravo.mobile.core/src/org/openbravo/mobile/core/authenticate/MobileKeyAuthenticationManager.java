/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.authenticate;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.authentication.basic.DefaultAuthenticationManager;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.LoginUtils;
import org.openbravo.base.secureApp.LoginUtils.RoleDefaults;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.authenticate.MobileAuthenticationKeyUtils.AuthenticationToken;
import org.openbravo.service.web.UserContextCache;

/**
 * An {@link AuthenticationManager} which can work with encrypted keys.
 * 
 * @author mtaal
 */
public class MobileKeyAuthenticationManager extends DefaultAuthenticationManager {

  private static final String DEFAULT_AUTH_CLASS = "org.openbravo.authentication.basic.DefaultAuthenticationManager";

  private static final Logger logger = Logger.getLogger(MobileKeyAuthenticationManager.class);
  private static Boolean isMobileKeyAuthenticationManagerInstalled = null;

  public static boolean isInstalled() {
    if (isMobileKeyAuthenticationManagerInstalled != null) {
      return isMobileKeyAuthenticationManagerInstalled;
    }

    AuthenticationManager authManager;
    String authClass = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("authentication.class", DEFAULT_AUTH_CLASS);
    if (authClass == null || authClass.equals("")) {
      // If not defined, load default
      authClass = "org.openbravo.authentication.basic.DefaultAuthenticationManager";
    }
    try {
      authManager = (AuthenticationManager) OBClassLoader.getInstance().loadClass(authClass)
          .newInstance();
    } catch (Exception e) {
      authManager = new DefaultAuthenticationManager();
    }
    isMobileKeyAuthenticationManagerInstalled = authManager instanceof MobileKeyAuthenticationManager;
    return isMobileKeyAuthenticationManagerInstalled;
  }

  @Override
  protected String doAuthenticate(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException, ServletException, IOException {

    String sUserId = request.getSession(false) == null ? "" : (String) request.getSession()
        .getAttribute("#Authenticated_user");
    if (!StringUtils.isEmpty(sUserId)) {
      return sUserId;
    }

    final String token = request
        .getParameter(MobileAuthenticationKeyUtils.AUTHENTICATION_TOKEN_PARAM);
    final String clientId = request
        .getParameter(MobileAuthenticationKeyUtils.AUTHENTICATION_CLIENT_PARAM);

    if (token == null || clientId == null) {
      if (AuthenticationManager.isStatelessRequest(request)) {
        return super.doWebServiceAuthenticate(request);
      }
      return doDefaultAuthenticate(request, response);
    }

    try {
      return doInternalAuthenticate(request, token, clientId);
    } catch (Throwable t) {
      if (AuthenticationManager.isStatelessRequest(request)) {
        logger.debug("Authentication login failed, stateless so stopping here", t);
        throw new AuthenticationException("Authentication Login Failed", t);
      }
      logger.debug("Authentication login failed, continueing with standard login approach", t);
      return super.doAuthenticate(request, response);
    }
  }

  private String doInternalAuthenticate(HttpServletRequest request, String token, String clientId)
      throws Exception {
    final AuthenticationToken authenticationToken = MobileAuthenticationKeyUtils.decrypt(clientId,
        token);

    // support faster login
    if ("true".equals(request.getParameter(MobileAuthenticationKeyUtils.USE_USER_CACHE_PARAMETER))) {
      OBContext.setOBContext(UserContextCache.getInstance().getCreateOBContext(
          authenticationToken.userId, authenticationToken.roleId, authenticationToken.orgId));
      OBContext.setOBContextInSession(request, OBContext.getOBContext());
    }

    if (AuthenticationManager.isStatelessRequest(request)) {
      return authenticationToken.getUserId();
    }
    // code from DefaultAuthenticationManager
    // Using the Servlet API instead of vars.setSessionValue to avoid breaking code
    // vars.setSessionValue always transform the key to upper-case
    HttpSession session = request.getSession(true);
    session.setAttribute("#Authenticated_user", authenticationToken.getUserId());
    session.setAttribute("#Authenticated_by_Token", "true");

    // set the client/org/role
    final VariablesSecureApp vars = new VariablesSecureApp(request);
    vars.setSessionValue("POSTerminal", authenticationToken.getTerminalId());

    // session id will be set by caller
    vars.setSessionValue("#LogginIn", "N");

    // tell fillsessionarguments to use light login
    vars.setSessionValue("#Light_Login", "Y");

    ConfigParameters globalParameters = ConfigParameters.retrieveFrom(session.getServletContext());

    RoleDefaults defaults = LoginUtils.getLoginDefaults(authenticationToken.getUserId(),
        authenticationToken.getRoleId(), conn);

    String whId = defaults.warehouse;
    try {
      OBContext.setAdminMode();
      if (authenticationToken.getOrgId() != defaults.org) {
        whId = LoginUtils.getDefaultWarehouse(conn, authenticationToken.getClientId(),
            authenticationToken.getOrgId(), authenticationToken.getRoleId());
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    LoginUtils.fillSessionArguments(conn, vars, authenticationToken.getUserId(),
        LoginUtils.getDefaultLanguage(conn, authenticationToken.getUserId()),
        LoginUtils.isDefaultRtl(conn, authenticationToken.getUserId()),
        authenticationToken.getRoleId(), authenticationToken.getClientId(),
        authenticationToken.getOrgId(), whId);
    LoginUtils.readNumberFormat(vars, globalParameters.getFormatPath());
    LoginUtils.saveLoginBD(request, vars, "0", "0");

    return authenticationToken.getUserId();
  }

  @Override
  protected String doWebServiceAuthenticate(HttpServletRequest request) {
    try {
      final String token = request
          .getParameter(MobileAuthenticationKeyUtils.AUTHENTICATION_TOKEN_PARAM);
      final String clientId = request
          .getParameter(MobileAuthenticationKeyUtils.AUTHENTICATION_CLIENT_PARAM);

      if (token == null || clientId == null) {
        return super.doWebServiceAuthenticate(request);
      }

      final String userId = doInternalAuthenticate(request, token, clientId);
      if (userId == null) {
        return super.doWebServiceAuthenticate(request);
      }
      return userId;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }

  /**
   * Calls the super {@link DefaultAuthenticationManager#doAuthenticate}, protected method so it can purposely be overriden by a
   * subclass.
   */
  protected String doDefaultAuthenticate(HttpServletRequest request, HttpServletResponse response)
      throws AuthenticationException, ServletException, IOException {
    return super.doAuthenticate(request, response);
  }
}
