/*
 ************************************************************************************
 * Copyright (C) 2012-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.process;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.model.HQLPropertyList;

/**
 * An HQL Query builder.
 * 
 * @author adrianromero
 */

public class SimpleQueryBuilder {

  private String hql;
  private String client;
  private String org;
  private Date lastUpdated;
  private JSONArray filters;
  private String orderByClause;
  private JSONArray orderByProperties;
  private HQLPropertyList properties;
  private Instance<HQLCriteriaProcess> hqlCriteria;

  private Map<String, Object> paramValues;

  private int parameterCounter = 0;

  public enum Operator {
    equals, //
    notEquals, //
    greaterThan, //
    lessThan, //
    startsWith, //
    contains;
  }

  public SimpleQueryBuilder(String hql, String client, String org, Date lastUpdated) {
    this.hql = hql;
    this.client = client;
    this.org = org;
    this.lastUpdated = lastUpdated;
    // :orgCriteria
    // :clientCriteria
    // :activeCriteria
  }

  public SimpleQueryBuilder(String hql, String client, String org, Date lastUpdated,
      JSONArray filters, HQLPropertyList properties) {
    this.hql = hql;
    this.client = client;
    this.org = org;
    this.lastUpdated = lastUpdated;
    this.filters = filters;
    this.properties = properties;
    // :orgCriteria
    // :clientCriteria
    // :activeCriteria
  }

  public SimpleQueryBuilder(String hql, String client, String org, Date lastUpdated,
      JSONArray filters, HQLPropertyList properties, String orderByClause) {
    this.hql = hql;
    this.client = client;
    this.org = org;
    this.lastUpdated = lastUpdated;
    this.filters = filters;
    this.properties = properties;
    this.orderByClause = orderByClause;
    // :orgCriteria
    // :clientCriteria
    // :activeCriteria
  }

  public SimpleQueryBuilder(String hql, String client, String org, Date lastUpdated,
      JSONArray filters, HQLPropertyList properties, String orderByClause,
      JSONArray orderByProperties) {
    this.hql = hql;
    this.client = client;
    this.org = org;
    this.lastUpdated = lastUpdated;
    this.filters = filters;
    this.properties = properties;
    this.orderByClause = orderByClause;
    this.orderByProperties = orderByProperties;
  }

  private static String getClientFilter(Collection<String> clients) {

    StringBuilder clientfilter = new StringBuilder();

    if (clients.size() == 0) {
      clientfilter.append(" (1=1) ");
    } else {
      clientfilter.append(" ($$$$client.id in (");
      boolean comma = false;
      for (String s : clients) {
        if (comma) {
          clientfilter.append(", ");
        } else {
          comma = true;
        }
        clientfilter.append("'");
        clientfilter.append(s);
        clientfilter.append("'");
      }
      clientfilter.append(")) ");
    }

    return clientfilter.toString();
  }

  private class NaturalOrganizationCriteria extends FiltersPartBuilderOrgCriteria {

    public NaturalOrganizationCriteria(Map<String, Object> paramValues, String client, String org) {
      super(paramValues);
      this.filter = OBContext.getOBContext().getOrganizationStructureProvider(client)
          .getNaturalTree(org);
    }
  }

  private class ChildOrganizationCriteria extends FiltersPartBuilderOrgCriteria {

    public ChildOrganizationCriteria(Map<String, Object> paramValues, String client, String org) {
      super(paramValues);
      this.filter = OBContext.getOBContext().getOrganizationStructureProvider(client)
          .getChildTree(org, true);
    }
  }

  private class ParentOrganizationCriteria extends FiltersPartBuilderOrgCriteria {

    public ParentOrganizationCriteria(Map<String, Object> paramValues, String client, String org) {
      super(paramValues);
      this.filter = OBContext.getOBContext().getOrganizationStructureProvider(client)
          .getParentTree(org, true);
    }
  }

  private class CurrentOrganizationCriteria extends FiltersPartBuilderOrgCriteria {
    public CurrentOrganizationCriteria(Map<String, Object> paramValues) {
      super(paramValues);
      this.filter = OBContext.getOBContext().getCurrentOrganization().getId();
    }
  }

  private static class CurrentClientCriteria implements PartBuilder {
    public String getPart() {
      return "'" + OBContext.getOBContext().getCurrentClient().getId() + "'";
    }
  }

  private class OrganizationCriteria extends FiltersPartBuilderOrgCriteria {
    public OrganizationCriteria(Map<String, Object> paramValues) {
      super(paramValues);
      this.filter = Arrays.asList(OBContext.getOBContext().getReadableOrganizations());
    }
  }

  private static class ClientCriteria implements PartBuilder {
    public String getPart() {
      return getClientFilter(Arrays.asList(OBContext.getOBContext().getReadableClients()));
    }
  }

  private class ReadableCriteria extends FiltersPartBuilder {
    public ReadableCriteria(Map<String, Object> paramValues) {
      super(paramValues);
    }

    private PartBuilder clientCriteria = new ClientCriteria();
    private PartBuilder orgCriteria = new OrganizationCriteria(paramValues1);
    private PartBuilder active = new ActiveCriteria();

    public String getPart() {
      return " (" + this.clientCriteria.getPart() + " and " + this.orgCriteria.getPart() + " and "
          + active.getPart() + ") ";
    }
  }

  private static class ReadableClientCriteria implements PartBuilder {
    private PartBuilder client = new ClientCriteria();
    private PartBuilder active = new ActiveCriteria();

    public String getPart() {
      return " (" + client.getPart() + " and " + active.getPart() + ") ";
    }
  }

  private static class ActiveCriteria implements PartBuilder {
    public String getPart() {
      return " ($$$$active = 'Y') ";
    }
  }

  private class ReadableSimpleCriteria extends FiltersPartBuilder {
    public ReadableSimpleCriteria(Map<String, Object> paramValues) {
      super(paramValues);
    }

    private PartBuilder clientCriteria = new ClientCriteria();
    private FiltersPartBuilderOrgCriteria orgCriteria = new OrganizationCriteria(paramValues1);

    public String getPart() {
      return " (" + clientCriteria.getPart() + " and " + orgCriteria.getPart() + ") ";
    }
  }

  private static class ReadableSimpleClientCriteria implements PartBuilder {
    private PartBuilder client = new ClientCriteria();

    public String getPart() {
      return " (" + client.getPart() + ") ";
    }
  }

  private class RoleId extends FiltersPartBuilderIdCriteria {
    public RoleId(Map<String, Object> paramValues) {
      super(paramValues);
      this.filter = OBContext.getOBContext().getRole().getId();
    }
  }

  private class UserId extends FiltersPartBuilderIdCriteria {
    public UserId(Map<String, Object> paramValues) {
      super(paramValues);
      this.filter = OBContext.getOBContext().getUser().getId();
    }
  }

  private class LanguageId extends FiltersPartBuilderIdCriteria {
    public LanguageId(Map<String, Object> paramValues) {
      super(paramValues);
      this.filter = OBContext.getOBContext().getLanguage().getId();
    }
  }

  private class IncrementalUpdateCriteria extends FiltersPartBuilder {
    Date lastUpdate;
    private PartBuilder active = new ActiveCriteria();

    public IncrementalUpdateCriteria(Map<String, Object> paramValues, Date lastUpdate) {
      super(paramValues);
      this.lastUpdate = lastUpdate;
    }

    public String getPart() {

      String part = "";
      if (lastUpdate != null) {
        String operatorValueName = "valueDate";
        part += " $$$$updated> :" + operatorValueName + parameterCounter + " ";
        fillParamValues(operatorValueName, lastUpdate);
      } else {
        part += active.getPart();
      }
      return part;
    }

  }

  private class FiltersPartBuilderIdCriteria extends FiltersPartBuilder {
    protected Object filter;

    public FiltersPartBuilderIdCriteria(Map<String, Object> paramValues) {
      super(paramValues);
    }

    public String getPart() {
      String operatorValueName = "valueEqId";
      String idCriteria = " :" + operatorValueName + parameterCounter + " ";
      fillParamValues(operatorValueName, this.filter);
      return idCriteria;
    }

  }

  private class FiltersPartBuilderOrgCriteria extends FiltersPartBuilder {
    protected Object filter;

    public FiltersPartBuilderOrgCriteria(Map<String, Object> paramValues) {
      super(paramValues);
    }

    public String getPart() {
      String operatorValueName = "valueEqArray";
      String orgCriteria = "( $$$$organization.id in (:" + operatorValueName + parameterCounter
          + " ))";
      fillParamValues(operatorValueName, this.filter);
      return orgCriteria;
    }
  }

  private abstract class FiltersPartBuilder implements PartBuilder {

    public Map<String, Object> paramValues1;

    public FiltersPartBuilder(Map<String, Object> paramValues) {
      this.paramValues1 = paramValues;
    }

    public void fillParamValues(String operatorName, Object value) {
      if (!operatorName.equals("")) {
        this.paramValues1.put(operatorName + parameterCounter, value);
        parameterCounter++;
      }
    }

    public abstract String getPart();
  }

  private class FiltersCriteria extends FiltersPartBuilder {
    JSONArray filters1;
    private HQLPropertyList properties1;

    public FiltersCriteria(JSONArray filters, HQLPropertyList properties,
        Map<String, Object> paramValues) {
      super(paramValues);
      this.filters1 = filters;
      this.properties1 = properties;
    }

    public String getPart() {
      if (filters1 == null || properties1 == null) {
        return "1=1";
      }
      Object value;
      String operatorValueStart = "";
      String operatorValueName = "";
      String operatorValueFinal = "";
      String operatorValue = "";
      String filterQuery = "";
      boolean addAnd = false;
      for (int i = 0; i < filters1.length(); i++) {
        operatorValueStart = "";
        operatorValueName = "";
        operatorValueFinal = "";
        try {
          JSONObject objFilter = (JSONObject) filters1.get(i);
          JSONArray columns = objFilter.getJSONArray("columns");
          if (objFilter.getString("value").equals("__all__")
              || objFilter.getString("value").equals("")) {
            continue;
          }
          value = objFilter.getString("value");

          if (objFilter.getString("operator").equals(Operator.equals.toString())) {
            if (objFilter.has("isId") && objFilter.getBoolean("isId")) {
              operatorValueStart = "=:";
              operatorValueName = "valueEqId";
            } else if (objFilter.has("boolean") && objFilter.getBoolean("boolean")) {
              operatorValueStart = "=:";
              operatorValueName = "valueEqB";
              value = objFilter.getBoolean("value");
            } else if (objFilter.has("fieldType")
                && objFilter.getString("fieldType").equals("Number")) {
              operatorValueStart = "=:";
              operatorValueName = "valueEqNum";
              value = new BigDecimal(objFilter.getString("value"));
            } else if (objFilter.has("fieldType")
                && objFilter.getString("fieldType").equals("forceString")) {
              operatorValueName = "valueEqFs";
              operatorValueStart = "='" + value + "'";
            } else if (objFilter.get("value") instanceof JSONArray) {
              operatorValueStart = " in (:";
              operatorValueName = "valueEqArray";
              operatorValueFinal = ")";
              value = new JSONArray(objFilter.getString("value"));
            } else {
              operatorValueStart = "=upper(:";
              operatorValueName = "valueEqUp";
              operatorValueFinal = ")";
            }
          } else if (objFilter.getString("operator").equals(Operator.notEquals.toString())) {
            operatorValueStart = "<> :";
            operatorValueName = "valueNEq";
            value = new BigDecimal(objFilter.getString("value"));
          } else if (objFilter.getString("operator").equals(Operator.greaterThan.toString())) {
            operatorValueStart = "> :";
            operatorValueName = "valueGt";
            value = new BigDecimal(objFilter.getString("value"));
          } else if (objFilter.getString("operator").equals(Operator.lessThan.toString())) {
            operatorValueStart = "< :";
            operatorValueName = "valueLt";
            value = new BigDecimal(objFilter.getString("value"));
          } else if (objFilter.getString("operator").equals(Operator.startsWith.toString())) {
            operatorValueStart = " like upper(:";
            operatorValueName = "valueSt";
            operatorValueFinal = ")";
          } else if (objFilter.getString("operator").equals(Operator.contains.toString())) {
            operatorValueStart = " like upper(:";
            operatorValueName = "valueCon";
            operatorValueFinal = ")";
          } else if (objFilter.getString("operator").equals("filter")) {
            continue;
          } else if (objFilter.has("isId") && objFilter.getBoolean("isId")) {
            operatorValueStart = "=:";
            operatorValueName = "valueEqId";
          } else {
            operatorValueStart = "=upper(:";
            operatorValueName = "valueEqUp";
            operatorValueFinal = ")";
          }
          if (addAnd) {
            filterQuery += " AND ";
          }

          if (columns.length() != 0) {
            filterQuery += " (";
            for (int j = 0; j < columns.length(); j++) {
              operatorValue = operatorValueStart + operatorValueName + parameterCounter + " "
                  + operatorValueFinal;
              fillParamValues(operatorValueName, value);
              if ((objFilter.has("isId") && objFilter.getBoolean("isId"))
                  || (objFilter.has("boolean") && objFilter.getBoolean("boolean"))
                  || (objFilter.has("fieldType") && objFilter.getString("fieldType").equals(
                      "Number")) || (objFilter.get("value") instanceof JSONArray)) {
                filterQuery += properties1.getHqlProperty((String) columns.get(j)) + operatorValue;
              } else if (objFilter.has("fieldType")
                  && objFilter.getString("fieldType").equals("forceString")) {
                filterQuery += properties1.getHqlProperty((String) columns.get(j))
                    + operatorValueStart;

              } else {
                filterQuery += "upper(" + properties1.getHqlProperty((String) columns.get(j)) + ")"
                    + operatorValue;
              }
              if (j != columns.length() - 1) {
                filterQuery += " OR ";
              }

            }

            filterQuery += ") ";
          } else {
            operatorValue = operatorValueStart + operatorValueName + parameterCounter + " "
                + operatorValueFinal;
            filterQuery += operatorValue;
            fillParamValues(operatorValueName, value);
          }
          addAnd = true;
        } catch (JSONException ignored) {
        }

      }
      if (filterQuery != "") {
        filterQuery = " (" + filterQuery + ") ";
      } else {
        filterQuery = "1=1";
      }
      return filterQuery;
    }

  }

  private static class OrderByCriteria implements PartBuilder {
    private String orderByClause;
    private JSONArray orderByProperties;
    private HQLPropertyList hqlProperties;

    public OrderByCriteria(String orderByClause, JSONArray orderByProperties,
        HQLPropertyList properties) {
      this.orderByClause = orderByClause;
      this.orderByProperties = orderByProperties;
      this.hqlProperties = properties;
    }

    public String getPart() {
      String orderBy = "ORDER BY ";
      boolean addComma = false;
      if (orderByProperties != null) {

        for (int i = 0; i < orderByProperties.length(); i++) {
          try {
            JSONObject orderByProp = (JSONObject) orderByProperties.get(i);
            if (orderByProp.has("property")) {
              if (addComma) {
                orderBy += ", ";
              }
              String property = orderByProp.getString("property");
              orderBy += hqlProperties.getHqlProperty(property);
              if (orderByProp.has("sorting")) {
                orderBy += " " + orderByProp.getString("sorting") + " ";
              }
              addComma = true;
            }
          } catch (JSONException ignored) {
          }
        }

      } else if (orderByClause != null) {
        orderBy += orderByClause;
      }
      return orderBy;
    }
  }

  private class HQLCriteria extends FiltersPartBuilder {
    JSONArray filters1;
    private Instance<HQLCriteriaProcess> hqlCriteria1;

    public HQLCriteria(JSONArray filters, Instance<HQLCriteriaProcess> hqlCriteria,
        Map<String, Object> paramValues) {
      super(paramValues);
      this.filters1 = filters;
      this.hqlCriteria1 = hqlCriteria;
    }

    public String getPart() {

      if (filters1 == null) {
        return "1=1";
      }

      String filterQuery = "";
      boolean addAnd = false;
      String filter = "";
      for (int i = 0; i < filters1.length(); i++) {
        try {
          JSONObject objFilter = (JSONObject) filters1.get(i);
          if (objFilter.getString("operator").equals("filter")) {
            if (hqlCriteria1 != null) {
              HQLCriteriaProcess criteria = hqlCriteria1.select(
                  new ComponentProvider.Selector(objFilter.getString("value"))).get();
              String hqlFilter = criteria.getHQLFilter(objFilter.has("params") ? objFilter
                  .getString("params") : null);
              String fieldtype = (objFilter.has("fieldType") ? objFilter.getString("fieldType")
                  : null);
              if (addAnd) {
                if (objFilter.has("filter") && filter.equals(objFilter.getString("filter"))) {
                  filterQuery += " " + criteria.getOperatorForMultipleFilters() + " ( "
                      + replaceParams(hqlFilter, objFilter.getString("params"), fieldtype) + " ) ";
                  filterQuery += " ) ";
                } else {
                  filterQuery += " ) AND ( "
                      + replaceParams(hqlFilter, objFilter.getString("params"), fieldtype);
                }
              } else {
                filterQuery += replaceParams(hqlFilter, objFilter.getString("params"), fieldtype);
              }
              addAnd = true;
              if (objFilter.has("filter")) {
                filter = objFilter.getString("filter");
              }
            }
          }
        } catch (JSONException ignored) {
        }
      }
      if (filterQuery != "") {
        filterQuery = " (" + filterQuery + ") ";
      } else {
        filterQuery = "1=1";
      }
      return filterQuery;
    }

    public String replaceParams(String hqlFilter, String params, String fieldtype) {
      JSONArray paramsArray;
      String newHqlFilter = hqlFilter;
      try {
        paramsArray = new JSONArray(params);
        String operatorValueName = null;
        if (paramsArray.length() > 0) {
          for (int i = 0; i < paramsArray.length(); i++) {

            if (paramsArray.get(i) instanceof Number) {
              if (fieldtype != null && fieldtype.equals("BigDecimal")) {
                operatorValueName = "valueEqBD";
              } else if (fieldtype != null && fieldtype.equals("Long")) {
                operatorValueName = "valueEqLong";
              } else {
                operatorValueName = "valueEqNum";
              }
            } else if (paramsArray.get(i) instanceof Boolean) {
              operatorValueName = "valueEqB";
            } else if (paramsArray.get(i) instanceof JSONArray) {
              operatorValueName = "valueEqArray";
            } else {
              operatorValueName = "valueEqSt";
            }
            newHqlFilter = newHqlFilter.replace("'$" + (i + 1) + "'", "$" + (i + 1));
            newHqlFilter = newHqlFilter.replace("$" + (i + 1), ":" + operatorValueName
                + parameterCounter + " ");
            fillParamValues(operatorValueName, paramsArray.get(i));

          }
        }
      } catch (JSONException ignored) {
      }
      return newHqlFilter;
    }
  }

  @Deprecated
  /** This method should not be used anymore, as the internal criterias will generate named parameters which need to be set in the query.
   *  If it's necessary to use it for some reason, then it's mandatory to call the fillQueryWithParameters method after
   *  the query has been constructed
   */
  public String getHQLQuery() {

    String newhql = hql;

    // These two filters: $filtersCriteria and $hqlCriteria must be done at the very beginning
    // because can include other tags like $naturalOrgCriteria, ...

    this.paramValues = new HashMap<String, Object>();
    newhql = replaceAll(newhql, "$filtersCriteria", new FiltersCriteria(filters, properties,
        this.paramValues));
    newhql = replaceAll(newhql, "$orderByCriteria", new OrderByCriteria(orderByClause,
        orderByProperties, properties));
    newhql = replaceAll(newhql, "$hqlCriteria", new HQLCriteria(filters, hqlCriteria,
        this.paramValues));

    newhql = replaceAll(newhql, "$clientCriteria", new ClientCriteria());

    newhql = replaceAll(newhql, "$orgCriteria", new OrganizationCriteria(paramValues));
    newhql = replaceAll(newhql, "$clientId", new CurrentClientCriteria());
    newhql = replaceAll(newhql, "$orgId", new CurrentOrganizationCriteria(paramValues));
    newhql = replaceAll(newhql, "$naturalOrgCriteria", new NaturalOrganizationCriteria(paramValues,
        client, org));
    newhql = replaceAll(newhql, "$parentOrgCriteria", new ParentOrganizationCriteria(paramValues,
        client, org));
    newhql = replaceAll(newhql, "$childOrgCriteria", new ChildOrganizationCriteria(paramValues,
        client, org));
    newhql = replaceAll(newhql, "$activeCriteria", new ActiveCriteria());
    newhql = replaceAll(newhql, "$incrementalUpdateCriteria", new IncrementalUpdateCriteria(
        paramValues, lastUpdated));

    newhql = replaceAll(newhql, "$readableCriteria", new ReadableCriteria(paramValues));
    newhql = replaceAll(newhql, "$readableSimpleCriteria", new ReadableSimpleCriteria(paramValues));
    newhql = replaceAll(newhql, "$readableClientCriteria", new ReadableClientCriteria());
    newhql = replaceAll(newhql, "$readableSimpleClientCriteria", new ReadableSimpleClientCriteria());

    newhql = replaceAll(newhql, "$roleId", new RoleId(paramValues));
    newhql = replaceAll(newhql, "$userId", new UserId(paramValues));
    newhql = replaceAll(newhql, "$languageId", new LanguageId(paramValues));

    return newhql;
  }

  public Query getDalQuery() {
    Session session = OBDal.getInstance().getSession();
    Query query = session.createQuery(getHQLQuery());
    fillQueryWithParameters(query);

    return query;
  }

  private String replaceAll(String s, String search, PartBuilder part) {
    String news = s;
    int i = news.indexOf(search);
    if (i >= 0) {
      String replacement = part.getPart();
      while (i >= 0) {
        int alias = findalias(news, i);
        if (alias >= 0) {
          news = news.substring(0, alias)
              + replacement.replaceAll("\\$\\$\\$\\$", news.substring(alias, i))
              + news.substring(i + search.length());
        } else {
          news = news.substring(0, i) + replacement.replaceAll("\\$\\$\\$\\$", "")
              + news.substring(i + search.length());
        }

        i = news.indexOf(search);
      }
    }
    return news;
  }

  private int findalias(String sentence, int position) {

    int i = position - 1;
    int s = 0;

    while (i > 0) {
      char c = sentence.charAt(i);
      if (s == 0) {
        if (c == '.') {
          s = 1;
        } else {
          return -1;
        }
      } else if (s == 1) {
        if (Character.isLetterOrDigit(c)) {
          s = 2;
        } else {
          return -1;
        }
      } else if (s == 2) {
        if (!Character.isLetterOrDigit(c) && c != '.' && c != '_') {
          if (Character.isWhitespace(c) || c == ')' || c == '(') {
            return i + 1;
          } else {
            return -1;
          }
        }
      }
      i--;
    }
    return -1;
  }

  public void setHQLCriteria(Instance<HQLCriteriaProcess> hqlCriterias) {
    this.hqlCriteria = hqlCriterias;
  }

  @SuppressWarnings("unchecked")
  public void fillQueryWithParameters(Query query) {
    Entry<String, Object> value = null;
    try {
      if (paramValues != null) {
        Iterator<Entry<String, Object>> iter = paramValues.entrySet().iterator();
        while (iter.hasNext()) {
          value = iter.next();
          if (!query.toString().contains((value.getKey() + " "))) {
            continue;
          }
          if (value.getKey().startsWith("valueSt")) {
            query.setParameter(value.getKey(), value.getValue().toString() + "%");
          } else if (value.getKey().startsWith("valueCon")) {
            query.setParameter(value.getKey(), "%" + value.getValue().toString() + "%");
          } else if (value.getKey().startsWith("valueEqFs")) {
            query.setParameter(value.getKey(), "'" + value.getValue().toString() + "'");
          } else if (value.getValue() instanceof BigDecimal) {
            query.setParameter(value.getKey(), value.getValue());
          } else if (value.getKey().startsWith("valueEqLong")) {
            query.setParameter(value.getKey(), new Long(value.getValue().toString()));
          } else if (value.getKey().startsWith("valueEqBD")) {
            query.setParameter(value.getKey(), new BigDecimal(value.getValue().toString()));
          } else if (value.getKey().startsWith("valueEqNum")) {
            query.setParameter(value.getKey(), new BigDecimal(value.getValue().toString()));
          } else if (value.getValue() instanceof Boolean) {
            query.setParameter(value.getKey(), Boolean.parseBoolean(value.getValue().toString()));
          } else if (value.getValue() instanceof JSONArray) {
            JSONArray a = (JSONArray) value.getValue();
            List<String> parameter = new ArrayList<String>();
            for (int i = 0; i < a.length(); i++) {
              parameter.add(a.get(i).toString());
            }
            query.setParameterList(value.getKey(), parameter);
          } else if (value.getValue() instanceof List<?>) {
            List<String> parameter = (List<String>) value.getValue();
            query.setParameterList(value.getKey(), parameter);
          } else if (value.getValue() instanceof HashSet) {
            Set<String> parameter = new HashSet<String>();
            parameter.addAll((HashSet<String>) value.getValue());
            query.setParameterList(value.getKey(), parameter);
          } else if (value.getValue() instanceof Date) {
            query.setParameter(value.getKey(), value.getValue());
          } else {
            query.setParameter(value.getKey(), value.getValue().toString());
          }
        }
      }
    } catch (Exception e) {
      throw new OBException("Error when converting value " + value.getValue(), e);
    }
  }
}
