/*
 ************************************************************************************
 * Copyright (C) 2013-2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.login;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.process.ProcessHQLQuery;

public class ContextInformation extends ProcessHQLQuery {

  @Override
  protected boolean isAdminMode() {
    return true;
  }

  @Override
  protected Map<String, Object> getParameterValues(JSONObject jsonsent) throws JSONException {
    try {
      OBContext.setAdminMode(true);

      Map<String, Object> paramValues = new HashMap<String, Object>();

      paramValues.put("orgId", OBContext.getOBContext().getCurrentOrganization().getId());
      paramValues.put("clientId", OBContext.getOBContext().getCurrentClient().getId());

      return paramValues;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    List<String> hqls = new ArrayList<String>();
    String hql1 = "select u.id as userId, r.id as roleId, org.id as orgId, cli.id as clientId "
        + "from ADUser u, ADRole r, Organization org, ADClient cli "
        + "where u.id = $userId and u.$readableSimpleCriteria and u.$activeCriteria and r.id = $roleId and r.$readableSimpleCriteria and r.$activeCriteria and org.id = :orgId "
        + " and cli.id =  :clientId ";
    hqls.add(hql1);
    return hqls;
  }

  @Override
  protected boolean bypassSecurity() {
    return true;
  }

}
