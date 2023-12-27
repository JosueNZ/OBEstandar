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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.mobile.core.model.HQLPropertyList;
import org.openbravo.mobile.core.servercontroller.MobileServerController;
import org.openbravo.mobile.core.servercontroller.MobileServerUtils;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonToDataConverter;

public abstract class ProcessHQLQuery extends SecuredJSONProcess {

  protected abstract List<String> getQuery(JSONObject jsonsent) throws JSONException;

  private final static Logger log = Logger.getLogger(ProcessHQLQuery.class);

  protected boolean isAdminMode() {
    return false;
  }

  protected StrategyQuery getStrategyQuery() {
    return ProcessHQLQuery.StrategyQueryScroll;
  }

  protected Map<String, Object> getParameterValues(JSONObject jsonsent) throws JSONException {
    return null;
  }

  protected List<HQLPropertyList> getHqlProperties(JSONObject jsonsent) {
    return null;
  }

  /**
   * If true, the query will not be executed if significant filters are not provided
   *
   * @return boolean, true if empty search's are not allowed
   */
  protected boolean mustHaveRemoteFilters() {
    return false;
  }

  /**
   * Provides the message which should be used when not enough filters are provided
   *
   * @return string, the message search key
   */
  protected String messageWhenNoFilters() {
    return "OBMOBC_RemoteSearchWithoutFilters";
  }

  @Inject
  @Any
  private Instance<HQLCriteriaProcess> hqlCriterias;

  @Override
  public void exec(Writer w, JSONObject jsonsent) throws IOException, ServletException {

    // don't do any actions when transitioning
    if (MobileServerUtils.isMultiServerEnabled()) {
      // if the request has server status info then make use of it to update the
      // server status
      MobileServerUtils.updateServerStatusFromJSON(jsonsent);

      if (MobileServerController.getInstance().serverHasTransitioningStatus()) {
        MobileServerController.getInstance().writeServerStatusJSON(w);
        return;
      }
    }

    Query rememberQueryForErrorLog = null;
    try {
      boolean streamOpened = false;
      if (isAdminMode()) {
        OBContext.setAdminMode(false);
      }

      Long lastUpdated = null;
      // Get timeout from milliseconds to seconds
      Integer timeout = jsonsent.has("timeout") ? jsonsent.getInt("timeout") / 1000 : null;
      try {
        lastUpdated = (jsonsent.has("lastUpdated") && !jsonsent.isNull("lastUpdated")
            && !jsonsent.get("lastUpdated").equals("null") && !jsonsent.get("lastUpdated").equals(
            "undefined")) ? jsonsent.getLong("lastUpdated") : null;
      } catch (Exception e) {
        lastUpdated = null;
        log.warn("lastUpdated param is not a valid timestamp. Full refresh will be executed this time: "
            + e.getMessage());
      }

      String dateFormat = jsonsent.has("_dateFormat") ? jsonsent.getString("_dateFormat") : null;

      JSONArray remoteFilters = jsonsent.has("remoteFilters") && !jsonsent.isNull("remoteFilters") ? jsonsent
          .getJSONArray("remoteFilters") : null;

      if (jsonsent.has("parameters") && jsonsent.getJSONObject("parameters").has("remoteModel")
          && jsonsent.getJSONObject("parameters").getBoolean("remoteModel")
          && mustHaveRemoteFilters()
          && (remoteFilters == null || !hasRelevantRemoteFilters(remoteFilters))) {
        JSONObject errorMessage = errorMessage(messageWhenNoFilters());
        log.debug("Search without relevant filters rejected for class " + this.getClass().getName());
        w.write(errorMessage.toString().substring(1, errorMessage.toString().length() - 1));
        return;
      }

      String orderByClause = jsonsent.has("orderByClause") && !jsonsent.isNull("orderByClause") ? jsonsent
          .getString("orderByClause") : null;

      JSONArray orderByProperties = jsonsent.has("orderByProperties")
          && !jsonsent.isNull("orderByProperties") ? jsonsent.getJSONArray("orderByProperties")
          : null;

      boolean isMasterdata = jsonsent.has("_isMasterdata") ? jsonsent.getBoolean("_isMasterdata")
          : false;
      int totalRows = 0;
      int queryRows = 0;
      int limit = -1;
      int offset = -1;
      int count = 0;
      if (jsonsent.has("_limit") && !jsonsent.isNull("_limit")) {
        try {
          limit = jsonsent.getInt("_limit");
        } catch (Exception e) {
          limit = -1;
        }
      }
      Boolean limitIsArray = false;
      JSONArray arrLimit = jsonsent.optJSONArray("_limit");
      boolean firstQuery = true;
      Map<String, Object> parameterValues = this.getParameterValues(jsonsent);
      List<String> queries = getQuery(jsonsent);
      List<HQLPropertyList> properties = getHqlProperties(jsonsent);

      for (int i = 0; i < queries.size(); i++) {
        if (limit == 0) {
          break;
        }
        String hqlQuery = queries.get(i);
        HQLPropertyList hqlProperty;
        if (properties != null) {
          hqlProperty = properties.get(i);
        } else {
          hqlProperty = null;
        }
        rememberQueryForErrorLog = null;

        SimpleQueryBuilder querybuilder = new SimpleQueryBuilder(hqlQuery, OBContext.getOBContext()
            .getCurrentClient().getId(), OBContext.getOBContext().getCurrentOrganization().getId(),
            lastUpdated != null ? new Date(lastUpdated) : null, remoteFilters, hqlProperty,
            orderByClause, orderByProperties);

        if (hqlCriterias != null) {
          querybuilder.setHQLCriteria(hqlCriterias);
        }

        final Query query = querybuilder.getDalQuery();
        setParameterValues(query, parameterValues);
        if (timeout != null) {
          query.setTimeout(timeout);
        }
        rememberQueryForErrorLog = query;

        if (arrLimit != null && arrLimit.length() > 0) {
          limitIsArray = true;
          if (i < arrLimit.length()) {
            try {
              limit = arrLimit.getInt(i);
            } catch (Exception e2) {
              limit = -1;
            }
          } else {
            limit = -1;
          }
        }
        if (limit > -1) {
          query.setMaxResults(limit);
        }

        if (jsonsent.has("_offset")) {
          offset = jsonsent.getInt("_offset");
          query.setFirstResult(jsonsent.getInt("_offset"));
        }

        if (jsonsent.has("_count")) {
          count = jsonsent.getInt("_count");
        }

        List<String> queryParams = new ArrayList<String>(Arrays.asList(query.getNamedParameters()));

        if (jsonsent.has("parameters")) {
          JSONObject jsonparams = jsonsent.getJSONObject("parameters");
          Iterator<?> it = jsonparams.keys();
          while (it.hasNext()) {
            String key = (String) it.next();
            if (!queryParams.contains(key)) {
              continue;
            }
            queryParams.remove(key);

            Object value = jsonparams.get(key);
            if (value instanceof JSONObject) {
              JSONObject jsonvalue = (JSONObject) value;
              query.setParameter(
                  key,
                  JsonToDataConverter.convertJsonToPropertyValue(
                      PropertyByType.get(jsonvalue.getString("type")), jsonvalue.get("value")));
            } else {
              query.setParameter(key, JsonToDataConverter.convertJsonToPropertyValue(
                  PropertyByType.infer(value), value));
            }
          }
        }

        // XXX: for standard params (client, org, pos), no need to add as extra
        if (!queryParams.isEmpty()) {
          for (String param : queryParams) {
            if (jsonsent.has(param)) {
              Object value = jsonsent.get(param);
              if (value instanceof JSONObject) {
                JSONObject jsonvalue = (JSONObject) value;
                query.setParameter(
                    param,
                    JsonToDataConverter.convertJsonToPropertyValue(
                        PropertyByType.get(jsonvalue.getString("type")), jsonvalue.get("value")));
              } else {
                query.setParameter(param, JsonToDataConverter.convertJsonToPropertyValue(
                    PropertyByType.infer(value), value));
              }
            }
          }
        }

        if (!streamOpened) {
          JSONRowConverter.startResponse(w);
          streamOpened = true;
        }
        if (dateFormat == null) {
          queryRows = getStrategyQuery().buildResponse(w, query, firstQuery);
        } else {
          queryRows = getStrategyQuery().buildResponse(w, query, firstQuery, dateFormat);
        }
        totalRows += queryRows;

        if (totalRows > 0) {
          firstQuery = false;
        }
        if (isMasterdata) {
          String terminalName = "";
          if (jsonsent != null && jsonsent.has("terminalName")) {
            terminalName = jsonsent.get("terminalName").toString();
          }
          log.debug(terminalName + " - Model " + this.getClass().getName() + " with limit " + limit
              + " and offset " + offset + " has obtained " + totalRows + " records");
        }
        /*
         * Do not recalculate the limit for next queries when: 1.- We send an array of limits where
         * we define the limit for each query. 2.- When the model is a masterdata model. In this
         * case limit is used for pagination.
         * 
         * It must be used just when we do remote queries
         */
        if (limit > -1 && !limitIsArray && !isMasterdata) {
          limit = limit - queryRows;
        }
      }
      if (!streamOpened) {
        JSONRowConverter.startResponse(w);
        streamOpened = true;
        totalRows = 0;
      }
      JSONRowConverter.endResponse(w, totalRows + count);

    } catch (Exception e) {
      log.error(this.getClass().getName() + ": Error when generating query: "
          + rememberQueryForErrorLog + " (" + e.getMessage() + ")", e);
      JSONRowConverter.addJSONExceptionFields(w, e);
    } finally {
      if (isAdminMode()) {
        OBContext.restorePreviousMode();
      }
    }
  }

  public void setParameterValues(Query query, Map<String, Object> parameterValues) {
    Entry<String, Object> value = null;

    try {
      if (parameterValues != null) {
        Iterator<Entry<String, Object>> iter = parameterValues.entrySet().iterator();
        while (iter.hasNext()) {
          value = iter.next();
          String paramName = value.getKey();
          if (!ArrayUtils.contains(query.getNamedParameters(), paramName)) {
            continue;
          }
          if (parameterValues.get(paramName) instanceof BigDecimal) {
            query.setParameter(paramName, parameterValues.get(paramName));
          } else if (parameterValues.get(paramName) instanceof Date) {
            query.setParameter(paramName, parameterValues.get(paramName));
          } else if (parameterValues.get(paramName) instanceof Boolean) {
            query.setParameter(paramName,
                Boolean.parseBoolean(parameterValues.get(paramName).toString()));
          } else if (parameterValues.get(paramName) instanceof JSONArray) {
            JSONArray a = (JSONArray) parameterValues.get(paramName);
            List<String> parameter = new ArrayList<String>();
            for (int j = 0; j < a.length(); j++) {
              parameter.add(a.get(j).toString());
            }
            query.setParameterList(paramName, parameter);
          } else if (parameterValues.get(paramName) instanceof Collection<?>) {
            query.setParameterList(paramName, (Collection<?>) parameterValues.get(paramName));
          } else {
            query.setParameter(paramName, parameterValues.get(paramName).toString());
          }
        }
      }
    } catch (Exception e) {
      throw new OBException("Error when converting value " + value.getValue(), e);
    }
  }

  protected boolean hasRelevantRemoteFilters(JSONArray remoteFilters) {
    try {
      for (int i = 0; i < remoteFilters.length(); i++) {
        JSONObject objFilter = (JSONObject) remoteFilters.get(i);
        // Check filter has a non empty value
        if (!objFilter.getString("value").equals("__all__")
            && !objFilter.getString("value").equals("")) {
          if (!objFilter.getString("operator").equals("startsWith")
              && !objFilter.getString("operator").equals("contains")) {
            return true; // equals, notEquals, greaterThan and lessThan are always relevant
          }
          if (objFilter.getString("value").length() >= 3) {
            return true; // for startsWith and contains, the filter is relevant if length >= 3
          }
        }
      }
      return false; // No filter of the list is relevant
    } catch (JSONException e) {
      throw new OBException("Unable to read remote filters.");
    }

  }

  protected JSONObject errorMessage(String message) throws Exception {
    final JSONObject jsonResponse = new JSONObject();

    jsonResponse.put(JsonConstants.RESPONSE_STATUS, JsonConstants.RPCREQUEST_STATUS_FAILURE);
    jsonResponse.put("result", "-1");
    jsonResponse.put("ignoreForClientLog", true);

    jsonResponse.put("message", message);
    final JSONObject error = new JSONObject();
    error.put("message", message);
    jsonResponse.put("error", error);
    return jsonResponse;
  }

  public interface StrategyQuery {
    public int buildResponse(Writer w, Query query, boolean firstQuery) throws JSONException,
        IOException;

    public int buildResponse(Writer w, Query query, boolean firstQuery, String dateFormat)
        throws JSONException, IOException;
  }

  public final static StrategyQuery StrategyQueryScroll = new StrategyQuery() {
    public int buildResponse(Writer w, Query query, boolean firstQuery, String dateFormat)
        throws JSONException, IOException {
      ScrollableResults listdata = query.scroll(ScrollMode.FORWARD_ONLY);
      String[] aliases = query.getReturnAliases();

      return JSONRowConverter.buildResponse(w, Scroll.create(listdata), aliases, firstQuery,
          dateFormat);
    }

    public int buildResponse(Writer w, Query query, boolean firstQuery) throws JSONException,
        IOException {
      return buildResponse(w, query, firstQuery, null);
    }
  };

  public final static StrategyQuery StrategyQueryList = new StrategyQuery() {
    public int buildResponse(Writer w, Query query, boolean firstQuery, String dateFormat)
        throws JSONException, IOException {
      return buildResponse(w, query, firstQuery);
    }

    public int buildResponse(Writer w, Query query, boolean firstQuery) throws JSONException,
        IOException {
      List<?> listdata = query.list();
      String[] aliases = query.getReturnAliases();
      return JSONRowConverter.buildResponse(w, listdata, aliases, firstQuery);
    }
  };
}
