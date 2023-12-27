/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.process;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.MobileCoreKernelUtils;
import org.openbravo.mobile.core.MobileStaticResourceComponent;
import org.openbravo.mobile.core.servercontroller.MobileServerController;
import org.openbravo.mobile.core.servercontroller.MobileServerUtils;

public abstract class JSONProcessSimple extends SecuredJSONProcess {

  @Inject
  private MobileStaticResourceComponent mobileStaticResourceComponent;

  private String importEntryId;

  public String getImportEntryId() {
    return importEntryId;
  }

  public void setImportEntryId(String importEntryId) {
    this.importEntryId = importEntryId;
  }

  public abstract JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException;

  public void exec(Writer w, JSONObject jsonsent) throws IOException, ServletException {

    // don't do any actions when transitioning
    if (MobileServerUtils.isMultiServerEnabled()) {
      // if the request has server status info then make use of it to update the
      // server status
      MobileServerUtils.updateServerStatusFromJSON(jsonsent);

      if (!ignoreTransitioningStatus()
          && MobileServerController.getInstance().serverHasTransitioningStatus()) {
        MobileServerController.getInstance().writeServerStatusJSON(w);
        return;
      }
    }

    try {
      String s = exec(jsonsent).toString();
      if (s.startsWith("{") && s.endsWith("}")) {
        // write only the properties, brackets are written outside.
        JSONObject response = new JSONObject(s);
        JSONObject contextInfo = getContextInformation();
        if (contextInfo != null) {
          response.put("contextInfo", contextInfo);
        }
        if (!(this instanceof DataSynchronizationImportProcess)) {
          OBContext.setAdminMode(false);
          try {
            final Map<String, Object> parameters = MobileCoreKernelUtils.getParameterMap(
                RequestContext.getServletContext(), RequestContext.get().getRequest());
            if (jsonsent.has("appName")) {
              parameters.put("_appName", jsonsent.getString("appName"));
            }
            mobileStaticResourceComponent.setParameters(parameters);

            // prevent session from being created in case of stateless
            final boolean stateless = AuthenticationManager.isStatelessService(this.getClass())
                || (RequestContext.get().getRequest() != null && AuthenticationManager
                    .isStatelessRequest(RequestContext.get().getRequest()));
            if (!stateless && !mobileStaticResourceComponent.isInDevelopment()) {
              if (RequestContext.get().getSession() != null
                  && RequestContext.get().getSessionAttribute("sourceVersion") == null) {
                RequestContext.get().setSessionAttribute("sourceVersion",
                    mobileStaticResourceComponent.getStaticResourceFileName());
              }
              response.put("sourceVersion",
                  RequestContext.get().getSessionAttribute("sourceVersion"));
            }
          } finally {
            OBContext.restorePreviousMode();
          }
        }
        String responseStr = response.toString();
        w.write(responseStr.substring(1, responseStr.length() - 1));
      } else {
        throw new JSONException("Result is not a JSON object.");
      }
    } catch (JSONException e) {
      JSONRowConverter.addJSONExceptionFields(w, e);
    }
  }

  /**
   * Return true if the logic of the subclass should be executed also in case of transitioning
   * status. Default is false, so in case of transitioning status nothing is executed.
   */
  protected boolean ignoreTransitioningStatus() {
    return false;
  }
}
