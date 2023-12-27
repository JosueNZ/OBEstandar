/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.login;

import java.io.IOException;
import java.io.Writer;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.mobile.core.process.WebServiceAbstractServlet;

/**
 * Checks whether the server is available or not.
 * 
 * If it returns error, the client-side will remain offline.
 * 
 * If it doesn't, the client-side will consider it as a positive sign, but will not necessarily
 * become online. This will depend on how the criteria to come back online was configured in the
 * system.
 * 
 * Core provides some basic checks. Custom checks can be added by extending the class
 * ServerAvailabilityCheck
 *
 */
public class CheckServerAvailability extends WebServiceAbstractServlet {

  private static final long serialVersionUID = 1L;

  private static final Logger log = Logger.getLogger(Context.class);

  @Inject
  @Any
  private Instance<ServerAvailabilityCheck> serverAvailabilityChecks;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Content-Type", "application/json;charset=UTF-8");

    final Writer w = response.getWriter();
    JSONObject result = new JSONObject();
    JSONObject responseObj = new JSONObject();
    try {

      JSONObject params = new JSONObject(request.getParameter("params"));
      for (ServerAvailabilityCheck check : serverAvailabilityChecks) {
        check.doCheck(params, responseObj);
      }
      result.put("response", responseObj);
    } catch (Exception e) {
      log.error("Server is still offline: " + e.getMessage());

      try {
        responseObj.put("error", e.getMessage());
        result.put("response", responseObj);
      } catch (JSONException e1) {
        // wont happen
      }
    }
    w.write(result.toString());
    w.close();

    return;
  }

}
