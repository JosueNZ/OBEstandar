/*
 ************************************************************************************
 * Copyright (C) 2013-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.AllowedCrossDomainsHandler;
import org.openbravo.client.application.window.servlet.CalloutHttpServletResponse;
import org.openbravo.mobile.core.authenticate.MobileAuthenticationKeyUtils;
import org.openbravo.mobile.core.process.PropertyByType;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonToDataConverter;

/**
 * @author iperdomo
 * 
 */
public class OBMOBCUtils {

  public static final Logger log = Logger.getLogger(OBMOBCUtils.class);

  public static JSONObject createSimpleErrorJson(String msg) throws JSONException {
    JSONObject json = new JSONObject();
    json.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
    json.put("result", "0");
    json.put("error", msg);
    return json;
  }

  // TODO: add isAuthenticated to generic AuthenticationManager api
  public static boolean isAuthenticated(HttpServlet servlet, HttpServletRequest req,
      HttpServletResponse resp) throws Exception {

    // authentication tokens are there, try to login
    final String token = req.getParameter(MobileAuthenticationKeyUtils.AUTHENTICATION_TOKEN_PARAM);
    final String clientId = req
        .getParameter(MobileAuthenticationKeyUtils.AUTHENTICATION_CLIENT_PARAM);
    if (token != null && clientId != null) {
      CalloutHttpServletResponse fakeResponse = new CalloutHttpServletResponse(resp);
      return AuthenticationManager.getAuthenticationManager(servlet)
          .authenticate(req, fakeResponse) != null;
    }

    if (req.getSession(false) == null) {
      return false;
    }
    if (req.getSession(false).getAttribute("#AD_SESSION_ID") == null) {
      return false;
    }
    if (req.getSession().getAttribute("#Authenticated_user") != null) {
      return true;
    }

    return false;
  }

  /**
   * @deprecated use
   *             {@link AllowedCrossDomainsHandler#setCORSHeaders(HttpServletRequest, HttpServletResponse)}
   */
  public static void setCORSHeaders(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    AllowedCrossDomainsHandler.getInstance().setCORSHeaders(request, response);
  }

  /**
   * This method will return the correct date which should be stored in the database taking into
   * account the client and the server date
   */
  public static Date calculateClientDatetime(String orgClientDate, Long dateOffset) {
    Date serverDate = (Date) JsonToDataConverter.convertJsonToPropertyValue(
        PropertyByType.DATETIME,
        orgClientDate.lastIndexOf(".") != -1 ? orgClientDate.subSequence(0,
            orgClientDate.lastIndexOf(".")) : orgClientDate);
    // date is the date in the server timezone, we need to convert it to the date in the
    // original time zone
    Date dateUTC = convertToUTC(serverDate);
    Date clientDate = new Date();
    clientDate.setTime(dateUTC.getTime() - dateOffset * 60 * 1000);
    return clientDate;
  }

  /**
   * This method will return the correct date in server taking into account the client date
   */
  public static Date calculateServerDatetime(String orgClientDate, Long dateOffset) {
    Date serverDate = (Date) JsonToDataConverter.convertJsonToPropertyValue(
        PropertyByType.DATETIME,
        orgClientDate.lastIndexOf(".") != -1 ? orgClientDate.subSequence(0,
            orgClientDate.lastIndexOf(".")) : orgClientDate);

    return serverDate;
  }

  public static Date calculateServerDate(String orgClientDate, Long dateOffset) {
    Date clientDate = calculateClientDatetime(orgClientDate, dateOffset);
    clientDate = stripTime(clientDate);
    return clientDate;
  }

  public static Date convertToUTC(Date localTime) {
    Calendar now = Calendar.getInstance();
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(localTime);

    int gmtMillisecondOffset = (now.get(Calendar.ZONE_OFFSET) + now.get(Calendar.DST_OFFSET));
    calendar.add(Calendar.MILLISECOND, -gmtMillisecondOffset);

    return calendar.getTime();
  }

  public static Date stripTime(Date dateWithTime) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateWithTime);
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);
    calendar.set(Calendar.SECOND, 0);
    calendar.set(Calendar.MILLISECOND, 0);
    return calendar.getTime();
  }

  /**
   * Returns a new fix UUID using the string parameter. It generates a new UUID using
   * nameUUIDFromBytes function, so every time we call this function with same string will return
   * always the same UUID
   * 
   * @return a new fix UUID
   */
  public static String getUUIDbyString(String txt) {
    try {
      return UUID.nameUUIDFromBytes(txt.getBytes("UTF-8")).toString().replace("-", "")
          .toUpperCase();
    } catch (UnsupportedEncodingException e) {
      throw new OBException(e.getMessage(), e);
    }
  }

  /**
   * Returns TRUE when a certain property of a JSONObject satisfy all of above conditions
   * 
   * 1. The property Exists
   * 
   * 2. The property is not NULL
   * 
   * 3. The property is a String
   * 
   * 4. The property is a non empty string
   * 
   * @return is valid or not
   */
  public static boolean isJsonObjectPropertyStringPresentNotNullAndNotEmptyString(
      JSONObject jsonObject, String propertyToCheck) {
    if (jsonObject.has(propertyToCheck) && !jsonObject.isNull(propertyToCheck)) {
      String readedValue;
      try {
        readedValue = jsonObject.getString(propertyToCheck);
        if (!StringUtils.isEmpty(readedValue)) {
          return true;
        }
      } catch (JSONException e) {
        return false;
      }
    }
    return false;
  }
}
