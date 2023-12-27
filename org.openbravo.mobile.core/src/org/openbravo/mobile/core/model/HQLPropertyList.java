/*
 ************************************************************************************
 * Copyright (C) 2013-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.transform.AliasToEntityMapResultTransformer;
import org.openbravo.base.exception.OBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience class to store a List of HQLProperties for model extensions
 * 
 * @author alostale
 * 
 */
public class HQLPropertyList {
  private static final Logger log = LoggerFactory.getLogger(HQLPropertyList.class);

  private List<HQLProperty> properties;

  public HQLPropertyList() {
    properties = new ArrayList<HQLProperty>();
  }

  /**
   * @return the properties
   */
  public List<HQLProperty> getProperties() {
    return properties;
  }

  public void addAll(List<HQLProperty> hqlProperties) {
    for (HQLProperty property : hqlProperties) {
      add(property);
    }
  }

  public void add(HQLProperty property) {
    for (int i = 0; i < properties.size(); i++) {
      HQLProperty current = properties.get(i);
      if (current.getJsonName().equals(property.getJsonName())) {
        if (current.getPriority() < property.getPriority()) {
          // replaces current HQLProperty current because new HQLProperty has greater priority
          properties.set(i, property);
        } else if (current.getPriority() == property.getPriority()) {
          throw new OBException(
              String
                  .format(
                      "Property '%s' already exists. Consider setting a priority to the property in order to replace the existing.",
                      property.getJsonName()));
        }
        return;
      }
    }
    properties.add(property);
  }

  /**
   * Returns a String to be used in the HQL's select clause based on all properties defined for this
   * model.
   * 
   */
  public String getHqlSelect() {
    boolean firstProperty = true;
    String hqlSelect = "";
    for (HQLProperty property : properties) {
      if (!firstProperty) {
        hqlSelect += ", \n";
      }
      firstProperty = false;
      hqlSelect += property.hqlProperty + " as " + property.getJsonName();
    }
    return " " + hqlSelect + " ";
  }

  /**
   * Returns a String to be used in the HQL's groupBy clause based on all properties defined for
   * this model.
   * 
   */
  public String getHqlGroupBy() {
    boolean firstProperty = true;
    String hqlGroupBy = "";
    for (HQLProperty property : properties) {
      if (property.includeInGroupBy) {
        if (!firstProperty) {
          hqlGroupBy += ", \n";
        }
        firstProperty = false;
        hqlGroupBy += property.hqlProperty;
      }
    }
    return " " + hqlGroupBy + " ";
  }

  /**
   * Returns a JSONArray of JSONObjects with the result of the query
   */
  public JSONArray getJSONArray(Query query) throws JSONException {
    JSONArray jsonArray = new JSONArray();
    query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> aliasToValueMapList = query.list();
    for (Map<String, Object> map : aliasToValueMapList) {
      JSONObject jsonObject = new JSONObject();
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        // TODO: verify that all the entries are in the properties list
        if (entry.getValue() instanceof Date) {
          DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
          df.setTimeZone(TimeZone.getTimeZone("UTC"));
          String nowAsISO = df.format(entry.getValue());
          jsonObject.put(entry.getKey(), nowAsISO);
        } else {
          jsonObject.put(entry.getKey(), entry.getValue());
        }
      }
      jsonArray.put(jsonObject);
    }
    return jsonArray;
  }

  /**
   * Returns a JSONArray with an element for each of the rows returned in the query. Query should
   * have been generated using the getHqlSelect method. Deprecated: use getJSONArray instead.
   */
  @Deprecated
  public JSONArray getJSONObject(Query query) {
    JSONArray result = new JSONArray();

    // TODO: make this scrollable
    for (Object obj : query.list()) {
      Object[] row = (Object[]) obj;
      result.put(getJSONObjectRow(row));
    }

    return result;
  }

  /**
   * Returns a JSONObject for a single row. Deprecated: use getJSONArray instead.
   */
  @Deprecated
  public JSONObject getJSONObjectRow(Object[] row) {
    JSONObject result = new JSONObject();
    int i = 0;
    for (HQLProperty property : properties) {
      try {
        if (row[i] instanceof Date) {
          TimeZone tz = TimeZone.getTimeZone("UTC");
          DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
          df.setTimeZone(tz);
          String nowAsISO = df.format(row[i]);
          result.put(property.getJsonName(), nowAsISO);
        } else {
          result.put(property.getJsonName(), row[i]);
        }
      } catch (JSONException e) {
        log.error("Error generating JSONObject for property " + property, e);
      }
      i++;
    }
    return result;
  }

  /**
   * Returns a String for a jsonName
   */
  public String getHqlProperty(String jsonName) {
    for (HQLProperty property : properties) {
      if (property.getJsonName().equals(jsonName)) {
        return property.getHqlProperty();
      }
    }
    return null;
  }

  /**
   * Returns the index of a certain property
   */
  public int getHqlPropertyIndex(String jsonName) {
    int counter = 0;
    for (HQLProperty property : properties) {
      if (property.getJsonName().equals(jsonName)) {
        return counter;
      }
      counter += 1;
    }
    return -1;
  }

}
