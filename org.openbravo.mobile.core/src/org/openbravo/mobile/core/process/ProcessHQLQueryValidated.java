/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.process;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;

public abstract class ProcessHQLQueryValidated extends ProcessHQLQuery {

  protected abstract List<String> getQueryValidated(JSONObject jsonsent) throws JSONException;

  protected abstract String getFilterEntity();

  private String getPropertyQuery(String filter) {
    Query propertyQuery = OBDal.getInstance().getSession()
        .createQuery("select property from ADPreference where property like :fullEnding");
    propertyQuery.setParameter("fullEnding", filter);
    propertyQuery.setMaxResults(1);
    return (String) propertyQuery.uniqueResult();
  }

  protected String getFullSortingPreferenceProperty(String filter) {
    String fullEnding = String.format("%%EntitySelector_%s_%s_s", getFilterEntity(), filter);
    return getPropertyQuery(fullEnding);
  }

  protected String getFullFilteringPreferenceProperty(String filter) {
    String fullEnding = String.format("%%EntitySelector_%s_%s_f", getFilterEntity(), filter);
    return getPropertyQuery(fullEnding);
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {
    validateFilters(jsonsent);
    return getQueryValidated(jsonsent);
  }

  protected void validateFilters(JSONObject jsonsent) throws JSONException {
    validateSorting(jsonsent);
    validateFiltering(jsonsent);
    if (sortingErrors.size() > 0 || filterErrors.size() > 0) {
      StringBuffer errorMessage = new StringBuffer();
      if (sortingErrors.size() > 0) {
        errorMessage.append("###");
        errorMessage.append("OBMOBC_SortingNotAllowed");
        errorMessage.append("###");
        errorMessage.append(sortingErrors.get(0));
      }
      if (filterErrors.size() > 0) {
        errorMessage.append("###");
        errorMessage.append("OBMOBC_FilteringNotAllowed");
        for (String error : filterErrors) {
          errorMessage.append("###");
          errorMessage.append(error);
        }
      }
      throw new OBException(errorMessage.toString());
    }
  }

  List<String> sortingErrors = new ArrayList<String>();
  List<String> filterErrors = new ArrayList<String>();

  protected void validateSorting(JSONObject jsonsent) throws JSONException {
    if (jsonsent.has("orderByProperties") && jsonsent.get("orderByProperties") != JSONObject.NULL) {
      JSONArray orderByProperties = jsonsent.getJSONArray("orderByProperties");
      for (int i = 0; i < orderByProperties.length(); i++) {
        JSONObject orderByProperty = orderByProperties.getJSONObject(i);
        if (orderByProperty.has("property")) {
          String column = orderByProperty.getString("property");
          String sortingProperty = getFullSortingPreferenceProperty(column);
          if (sortingProperty != null) {
            validateProperty(column, sortingProperty, sortingErrors);
          }
        }
      }
    }
  }

  protected void validateFiltering(JSONObject jsonsent) throws JSONException {
    if (jsonsent.has("remoteFilters")) {
      JSONArray remoteFilters = jsonsent.getJSONArray("remoteFilters");
      for (int i = 0; i < remoteFilters.length(); i++) {
        JSONObject remoteFilter = remoteFilters.getJSONObject(i);
        JSONArray columns = remoteFilter.getJSONArray("columns");
        String value = remoteFilter.getString("value");
        if (!value.equals("")) {
          for (int j = 0; j < columns.length(); j++) {
            String column = (String) columns.get(j);
            String filteringProperty = getFullFilteringPreferenceProperty(column);
            if (filteringProperty != null) {
              validateProperty(column, filteringProperty, filterErrors);
            }
          }
        }
      }
    }
  }

  private void validateProperty(String column, String property, List<String> listOfErrors) {
    try {
      String filteringPropertyValue = Preferences.getPreferenceValue(property, true, OBContext
          .getOBContext().getCurrentClient().getId(), OBContext.getOBContext()
          .getCurrentOrganization().getId(), OBContext.getOBContext().getUser().getId(), OBContext
          .getOBContext().getRole().getId(), null);
      if (filteringPropertyValue.equals("Y")) {
        listOfErrors.add(column);
      }
    } catch (PropertyException e) {
      throw new OBException(e.getMessage());
    }
  }
}