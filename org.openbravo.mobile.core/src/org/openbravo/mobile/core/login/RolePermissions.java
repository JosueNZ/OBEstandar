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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.mobile.core.MobileCoreConstants;
import org.openbravo.mobile.core.process.JSONRowConverter;
import org.openbravo.mobile.core.process.SecuredJSONProcess;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.service.json.JsonConstants;

public class RolePermissions extends SecuredJSONProcess {

  private static final Logger log = Logger.getLogger(RolePermissions.class);

  @Override
  protected boolean bypassSecurity() {
    return true;
  }

  @Override
  public void exec(Writer w, JSONObject jsonsent) throws IOException, ServletException {
    OBContext.setAdminMode(true);
    try {
      String moduleId = jsonsent.getJSONObject("parameters").getJSONObject("appModuleId")
          .getString("value");
      buildResponse(w, getPrefList(moduleId));
    } catch (Exception e) {
      log.error("error getting role permissions", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void buildResponse(Writer w, List<String> prefs) throws IOException {

    final int startRow = 0;
    int rows = 0;
    Throwable t = null;

    Map<String, Object> prefValues = new HashMap<String, Object>();
    // default is false
    for (String pref : prefs) {
      prefValues.put(pref, false);
    }
    prefValues.putAll(getPreferenceValues(prefs));

    try {
      w.write("\"data\":[");
      for (String key : prefValues.keySet()) {
        if (rows > 0) {
          w.write(',');
        }
        JSONObject json = new JSONObject();
        json.put("key", key);
        json.put("value", prefValues.get(key));
        w.write(json.toString());
        rows++;
      }
    } catch (JSONException e) {
      t = e;
    } finally {
      w.write("],");
      if (t == null) {
        // Add success fields
        w.write("\"");
        w.write(JsonConstants.RESPONSE_STARTROW);
        w.write("\":");
        w.write(Integer.toString(startRow));
        w.write(",\"");
        w.write(JsonConstants.RESPONSE_ENDROW);
        w.write("\":");
        w.write(Integer.toString(rows > 0 ? rows + startRow - 1 : 0));
        w.write(",\"");
        if (rows == 0) {
          w.write(JsonConstants.RESPONSE_TOTALROWS);
          w.write("\":0,\"");
        }
        w.write(JsonConstants.RESPONSE_STATUS);
        w.write("\":");
        w.write(Integer.toString(JsonConstants.RPCREQUEST_STATUS_SUCCESS));
      } else {
        JSONRowConverter.addJSONExceptionFields(w, t);
      }
    }
  }

  private static Map<String, Object> getPreferenceValues(List<String> preferenceKeys) {
    HashSet<String> prefKeys = new HashSet<String>(preferenceKeys);
    List<Preference> preferences = Preferences.getAllPreferences(OBContext.getOBContext()
        .getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getUser().getId(), OBContext.getOBContext().getRole().getId());
    Map<String, Object> result = new HashMap<String, Object>();
    for (Preference preference : preferences) {
      if (preference.getProperty() == null || !prefKeys.contains(preference.getProperty())) {
        continue;
      }
      Object prefValue = preference.getSearchKey();
      if ("Y".equals(prefValue)) {
        prefValue = Boolean.TRUE;
      } else if ("N".equals(prefValue)) {
        prefValue = Boolean.FALSE;
      }
      result.put(preference.getProperty(), prefValue);
    }
    return result;
  }

  private static List<String> getPrefList(String moduleId) {
    String whereClause = "where reference.id = :refId and module.id in "
        + LabelsComponent.getMobileAppDependantModuleIds(moduleId);
    OBQuery<org.openbravo.model.ad.domain.List> refLists = OBDal.getInstance().createQuery(
        org.openbravo.model.ad.domain.List.class, whereClause);
    refLists.setNamedParameter("refId", MobileCoreConstants.PREFERENCE_LIST_ID);
    List<String> preferenceList = new ArrayList<String>();
    for (org.openbravo.model.ad.domain.List listRef : refLists.list()) {
      preferenceList.add(listRef.getSearchKey());
    }
    return preferenceList;
  }

  public static JSONArray buildPermissionsObject(String moduleId) throws IOException {

    JSONArray permissionsObject = new JSONArray();

    List<String> prefs = getPrefList(moduleId);
    Map<String, Object> prefValues = new HashMap<String, Object>();
    // default is false
    for (String pref : prefs) {
      prefValues.put(pref, false);
    }
    prefValues.putAll(getPreferenceValues(prefs));

    for (String key : prefValues.keySet()) {
      try {
        JSONObject json = new JSONObject();
        json.put("key", key);
        json.put("value", prefValues.get(key));
        permissionsObject.put(json);
      } catch (JSONException e) {
        // won't happen
      }
    }

    return permissionsObject;
  }
}
