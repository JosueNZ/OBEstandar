/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.process;

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.service.web.InvalidRequestException;
import org.openbravo.service.web.WebServiceUtil;

/**
 * A web service which provides Mobile services.
 * 
 * @author adrianromero
 */
public class MobileService extends WebServiceAuthenticatedServlet {

  private static final Logger log = Logger.getLogger(MobileService.class);
  private static final long serialVersionUID = 1L;

  private static String SERVLET_PATH = "org.openbravo.mobile.core.service.jsonrest";

  @Inject
  @Any
  private Instance<MobileServiceRequestAllowedHandler> allowHandlers;

  public void init(ServletConfig config) {
    super.init(config);
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    doGetOrPost(request, response, null);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    doGetOrPost(request, response, getRequestContent(request));
  }

  public void service(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // determine if the
    String[] pathparts = checkSetParameters(request, response);
    if (pathparts == null) {
      super.service(request, response);
      return;
    } else if (pathparts.length == 1 || "hql".equals(pathparts[1])) {
      super.service(request, response);
      return;
    }

    // is a service, determine if all its implementations are stateless
    try {
      final List<? extends JSONProcess> jsonProcesses = new MobileServiceProcessor()
          .getServiceClassInstances(pathparts[1]);
      boolean stateless = true;
      for (JSONProcess jsonProcess : jsonProcesses) {
        stateless &= AuthenticationManager.isStatelessService(jsonProcess.getClass())
            || AuthenticationManager.isStatelessRequest(request);
      }
      if (stateless) {
        request.setAttribute(AuthenticationManager.STATELESS_REQUEST_PARAMETER, "true");
      }
    } catch (Throwable ignore) {
      // ignore, will re-appear in further processing and handled better there
    }
    super.service(request, response);
  }

  private void doGetOrPost(HttpServletRequest request, HttpServletResponse response, String content)
      throws IOException, ServletException {

    for (Iterator<MobileServiceRequestAllowedHandler> procIter = allowHandlers.iterator(); procIter
        .hasNext();) {
      MobileServiceRequestAllowedHandler allowHandler = procIter.next();
      if (!allowHandler.allowRequest(request, response)) {
        return;
      }
    }

    String[] pathparts = checkSetParameters(request, response);
    if (pathparts == null) {
      return;
    }
    try {
      final Object jsonsent = getContentAsJSON((content == null && pathparts.length == 3) ? java.net.URLDecoder
          .decode(pathparts[2], "UTF-8") : content);

      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");

      if (pathparts.length == 1 || "hql".equals(pathparts[1])) {
        final Writer w = response.getWriter();
        execThingArray(w, jsonsent);
        w.close();
      } else {
        // Command it is a class name
        try {
          if (jsonsent instanceof JSONObject) {
            final Writer w = response.getWriter();
            w.write("{\"response\":{");
            new MobileServiceProcessor().execServiceName(w, pathparts[1], (JSONObject) jsonsent);
            w.write("}}");
            w.close();

          } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
                "Content is not a JSON object.")));
          }
        } catch (ClassNotFoundException e) {
          log.error(e.getMessage(), e);
          response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
          writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
              "Command class not found: " + pathparts[1])));
        }
      }
    } catch (Throwable t) {
      log.error(t.getMessage(), t);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      writeResult(response, JsonUtils.convertExceptionToJson(t));
    }
  }

  private void execThingArray(Writer w, Object jsonContent) throws Exception {

    if (jsonContent instanceof JSONArray) {
      JSONArray jsonArray = (JSONArray) jsonContent;
      w.write("{\"response\":{");
      try {
        w.write("\"responses\":[");
        for (int i = 0; i < jsonArray.length(); i++) {
          if (i > 0) {
            w.write(',');
          }
          w.write('{');
          execThing(w, jsonArray.getJSONObject(i));
          w.write('}');
        }
        w.write("]");
      } catch (JSONException e) {
        w.write("],");
        JSONRowConverter.addJSONExceptionFields(w, e);
      }
      w.write("}}");
    } else if (jsonContent instanceof JSONObject) {
      w.write("{\"response\":{");
      execThing(w, (JSONObject) jsonContent);
      w.write("}}");
    } else {
      w.write(JsonUtils.convertExceptionToJson(new JSONException("Expected JSON object or array.")));
    }
  }

  private void execThing(Writer w, JSONObject jsonsent) throws Exception {

    // this is the response object
    // JSONProcess writes the properties of the response object.

    if (jsonsent.has("className")) {
      try {
        new MobileServiceProcessor().execServiceName(w, jsonsent.getString("className"), jsonsent);
      } catch (JSONException e) {
        JSONRowConverter.addJSONExceptionFields(w, e);
      } catch (ClassNotFoundException e) {
        JSONRowConverter.addJSONExceptionFields(w, e);
      }
    } else if (jsonsent.has("process")) { // It is a Process
      execProcedureName(w, jsonsent);
    } else {
      JSONRowConverter.addJSONExceptionFields(w, new JSONException(
          "Expected one of the following properties: \"className\" or \"process\"."));
    }
  }

  /**
   * Executes the Procedure set in the jsonsent JSONObject.
   * 
   * @param w
   *          Writer where the result is stored.
   * @param jsonsent
   *          JSONObject with the process input parameters.
   * @throws IOException
   *           Exception thrown on errors writing the response.
   * @throws ServletException
   *           Exception thrown by the JSONProcess execution.
   */
  private void execProcedureName(Writer w, JSONObject jsonsent) throws IOException,
      ServletException {

    ProcessProcedure proc = WeldUtils.getInstanceFromStaticBeanManager(ProcessProcedure.class);
    proc.secureExec(w, jsonsent);
  }

  private String[] checkSetParameters(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    if (!request.getRequestURI().contains("/" + SERVLET_PATH)) {
      writeResult(response, JsonUtils.convertExceptionToJson(new InvalidRequestException(
          "Invalid url, the path should contain the service name: " + SERVLET_PATH)));
      return null;
    }

    final int nameIndex = request.getRequestURI().indexOf(SERVLET_PATH);
    final String servicePart = request.getRequestURI().substring(nameIndex);
    final String[] pathParts = WebServiceUtil.getInstance().getSegments(servicePart);
    if (pathParts.length == 0 || !pathParts[0].equals(SERVLET_PATH)) {
      writeResult(
          response,
          JsonUtils.convertExceptionToJson(new InvalidRequestException("Invalid url: "
              + request.getRequestURI())));
      return null;
    }

    return pathParts;
  }

  @javax.inject.Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE })
  public @interface MobileServiceQualifier {
    String serviceName();
  }

  @SuppressWarnings("all")
  public static class MobileServiceClassSelector extends AnnotationLiteral<MobileServiceQualifier>
      implements MobileServiceQualifier {
    private static final long serialVersionUID = 1L;

    final String serviceName;

    public MobileServiceClassSelector(String entity) {
      this.serviceName = entity;
    }

    public String serviceName() {
      return serviceName;
    }
  }

  /**
   * Is used to control/check if a request is allowed to be handled and if not generates a response
   * to send back to the client.
   * 
   * @author mtaal
   */
  public static abstract class MobileServiceRequestAllowedHandler {
    /**
     * Return false if the request is not allowed to be processed further. In this case the callee
     * can generate a response in the response object.
     */
    public abstract boolean allowRequest(HttpServletRequest request, HttpServletResponse response);
  }
}
