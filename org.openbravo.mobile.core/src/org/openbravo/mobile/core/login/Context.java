/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.login;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.process.JSONRowConverter;
import org.openbravo.mobile.core.process.WebServiceAbstractServlet;
import org.openbravo.mobile.core.servercontroller.MobileServerUtils;
import org.openbravo.mobile.core.utils.OBMOBCUtils;
import org.openbravo.service.json.DataResolvingMode;
import org.openbravo.service.json.JsonUtils;

public class Context extends WebServiceAbstractServlet {

  private static final Logger log = Logger.getLogger(Context.class);

  private static final long serialVersionUID = 1L;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {

    // if not a valid origin still allow temporary cors header to respond
    // with an error
    if (MobileServerUtils.isInvalidOrigin(this, request, response)) {

      MobileServerUtils.setTemporaryCORSHeaders(request, response);

      // write an error json back
      response.setContentType("application/json;charset=UTF-8");
      response.setHeader("Content-Type", "application/json;charset=UTF-8");
      final Writer w = response.getWriter();
      JSONObject result = new JSONObject();
      try {
        result.put("exception", "invalidRequestOrigin");
      } catch (JSONException e) {
        throw new OBException(e);
      }
      w.write(result.toString());
      w.close();
      return;
    }

    OBContext.setAdminMode(false);
    try {
      JSONObject result = new JSONObject();
      if (OBMOBCUtils.isAuthenticated(this, request, response)) {

        result.put("data", getContextInfo());

      } else {
        result.put("exception", "No context loaded");
      }
      writeResult(response, result.toString());
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      writeResult(response, JsonUtils.convertExceptionToJson(e));
    } finally {
      OBContext.restorePreviousMode();
      OBContext.setOBContext((OBContext) null);
    }
  }

  public static JSONArray getContextInfo() throws JSONException {

    final JSONRowConverter converter = new JSONRowConverter(DataResolvingMode.FULL);
    JSONArray data = new JSONArray();

    String hqlUser = "select u as user, r as role, org as organization, cli as client "
        + "from ADUser u left outer join u.businessPartner bp, ADRole r, Organization org, ADClient cli "
        + "where u.id = :userId " + " and u.active = true " + " and r.id = :roleId "
        + " and org.id = :orgId " + " and cli.id = :clientId ";

    Query qryUser = OBDal.getInstance().getSession().createQuery(hqlUser);
    qryUser.setParameter("userId", OBContext.getOBContext().getUser().getId());
    qryUser.setParameter("roleId", OBContext.getOBContext().getRole().getId());
    qryUser.setParameter("orgId", OBContext.getOBContext().getCurrentOrganization().getId());
    qryUser.setParameter("clientId", OBContext.getOBContext().getCurrentClient().getId());

    for (Object qryUserObject : qryUser.list()) {
      final Object[] qryUserObjectItem = (Object[]) qryUserObject;

      JSONObject item = new JSONObject();
      item.put("user", converter.convert(qryUserObjectItem[0]));
      item.put("role", converter.convert(qryUserObjectItem[1]));
      item.put("organization", converter.convert(qryUserObjectItem[2]));
      item.put("client", converter.convert(qryUserObjectItem[3]));
      data.put(item);
    }
    return data;

  }

  // @Override
  // protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
  // return Arrays
  // .asList(new String[] {
  // "select u as user, img.bindaryData as img, r as role, org as organization, cli as client "
  // + "from ADUser u left outer join u.image img, ADRole r, Organization org, ADClient cli "
  // +
  // "where u.id = $userId and u.$readableSimpleCriteria and u.$activeCriteria and r.id = $roleId
  // and r.$readableSimpleCriteria and r.$activeCriteria and org.id ='"
  // + OBContext.getOBContext().getCurrentOrganization().getId()
  // + "' and cli.id = '"
  // + OBContext.getOBContext().getCurrentClient().getId() + "'" });
  // }
}
