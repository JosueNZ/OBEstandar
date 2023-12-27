/*
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.AllowedCrossDomainsHandler;
import org.openbravo.client.kernel.BaseComponent;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentGenerator;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.service.web.WebServiceUtil;

/**
 * The mobile core entry point for getting to static resources without needing to login. All
 * components called by this servlet need to let {@link BaseComponent#bypassAuthentication()} return
 * true.
 * 
 * @author mtaal
 */
public class MobileCoreComponentServlet extends HttpServlet {
  private static final String RESPONSE_HEADER_ETAG = "ETag";
  private static final String RESPONSE_HEADER_LASTMODIFIED = "Last-Modified";
  private static final String RESPONSE_HEADER_CACHE_CONTROL = "Cache-Control";
  private static final String RESPONSE_NO_CACHE = "no-cache";
  private static final String RESPONSE_HEADER_CONTENTTYPE = "Content-Type";

  private static final String REQUEST_HEADER_IFMODIFIEDSINCE = "If-Modified-Since";
  private static final String REQUEST_HEADER_IFNONEMATCH = "If-None-Match";

  private static final Logger log = Logger.getLogger(MobileCoreComponentServlet.class);

  private static final long serialVersionUID = 1L;

  private static final String servletPathPart = "org.openbravo.mobile.core";

  private static ServletContext servletContext;

  @Inject
  @Any
  private Instance<ComponentProvider> componentProviders;

  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    servletContext = config.getServletContext();
  }

  @Override
  public void service(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    // always set the cors headers
    AllowedCrossDomainsHandler.getInstance().setCORSHeaders(request, response);

    // don't process any further requests otherwise sessions are created for OPTIONS
    // requests, the cors headers have already been set so can return
    if (request.getMethod().equals("OPTIONS")) {
      return;
    }

    super.service(request, response);
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    if (!request.getRequestURI().contains("/" + servletPathPart)) {
      throw new UnsupportedOperationException("Invalid url " + request.getRequestURI());
    }

    OBContext.setAdminMode(true);
    doOptions(request, response);
    try {
      processComponentRequest(request, response);
    } finally {
      OBContext.restorePreviousMode();
      OBContext.setOBContext((OBContext) null);
    }
  }

  @Override
  public void doOptions(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    // normally this is already handled in the service method above,
    // but to be complete implemented the doOptions as well
    AllowedCrossDomainsHandler.getInstance().setCORSHeaders(request, response);
  }

  // NOTE: this exact same method is present in the KernelServlet, if changed
  // here, please also check the method in the KernelServlet class
  private Component getComponent(HttpServletRequest request, HttpServletResponse response) {

    final int nameIndex = request.getRequestURI().indexOf(servletPathPart);
    final String servicePart = request.getRequestURI().substring(nameIndex);
    final String[] pathParts = WebServiceUtil.getInstance().getSegments(servicePart);
    if (pathParts.length < 2) {
      throw new UnsupportedOperationException("No service name present in url "
          + request.getRequestURI());
    }
    final String componentProviderName = pathParts[1];

    final String componentId;
    if (pathParts.length > 2) {
      componentId = pathParts[2];
    } else {
      componentId = null;
    }

    try {
      final ComponentProvider componentProvider = componentProviders.select(
          new ComponentProvider.Selector(componentProviderName)).get();

      final Map<String, Object> parameters = MobileCoreKernelUtils.getParameterMap(servletContext,
          request);
      final Component component = componentProvider.getComponent(componentId, parameters);

      // we are not logging in allow only selected components if bypassing auth or already
      // authenticated
      if (!((BaseComponent) component).bypassAuthentication()
          && !OBMOBCUtils.isAuthenticated(this, request, response)) {
        throw new UnsupportedOperationException(String.format("Request not supported '%s'",
            request.getRequestURI()));
      }

      return component;
    } catch (UnsatisfiedResolutionException e) {
      throw new UnsupportedOperationException(String.format("Wrong URI request: '%s'",
          request.getRequestURI()), e);
    } catch (Exception e) {
      throw new UnsupportedOperationException(String.format(
          "Error loading bean with this URI request: '%s'", request.getRequestURI()), e);
    }
  }

  // NOTE: this exact same method is present in the KernelServlet, if changed
  // here, please also check the method in the KernelServlet class
  private void processComponentRequest(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    Component component = getComponent(request, response);
    OBContext.setAdminMode(true);
    String eTag;
    try {
      eTag = component.getETag();
    } finally {
      OBContext.restorePreviousMode();
    }
    final String requestETag = request.getHeader(REQUEST_HEADER_IFNONEMATCH);

    if (requestETag != null && eTag.equals(requestETag)) {
      response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
      response.setDateHeader(RESPONSE_HEADER_LASTMODIFIED,
          request.getDateHeader(REQUEST_HEADER_IFMODIFIEDSINCE));
      return;
    }

    try {
      final String result = ComponentGenerator.getInstance().generate(component);

      response.setHeader(RESPONSE_HEADER_ETAG, eTag);
      response.setDateHeader(RESPONSE_HEADER_LASTMODIFIED, component.getLastModified().getTime());
      response.setContentType(component.getContentType());
      response.setHeader(RESPONSE_HEADER_CONTENTTYPE, component.getContentType());
      response.setHeader(RESPONSE_HEADER_CACHE_CONTROL, RESPONSE_NO_CACHE);

      final PrintWriter pw = response.getWriter();
      pw.write(result);
      pw.close();
    } catch (Exception e) {
      log.error("Generating content using component: "
          + (component != null ? component.getClass().getName() : "NULL"), e);
      if (!response.isCommitted()) {
        response.setContentType(KernelConstants.JAVASCRIPT_CONTENTTYPE);
        response.setHeader(RESPONSE_HEADER_CONTENTTYPE, KernelConstants.JAVASCRIPT_CONTENTTYPE);
        response.setHeader(RESPONSE_HEADER_CACHE_CONTROL, RESPONSE_NO_CACHE);
        response.getWriter().write(KernelUtils.getInstance().createErrorJavaScript(e));
      }
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    doGet(request, response);
  }

  @Override
  public void doDelete(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    throw new UnsupportedOperationException("Only GET/POST is supported");
  }

  @Override
  public void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    throw new UnsupportedOperationException("Only GET/POST is supported");
  }
}
