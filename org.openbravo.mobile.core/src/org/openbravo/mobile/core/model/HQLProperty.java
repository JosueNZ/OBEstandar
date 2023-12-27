/*
 ************************************************************************************
 * Copyright (C) 2013-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.model;

import org.openbravo.base.model.domaintype.BasePrimitiveDomainType;

/**
 * Convenience class to store HQLProperties for model extensions
 * 
 * @author alostale, ral
 * 
 */
public class HQLProperty {
  String hqlProperty;
  private String jsonName;
  Boolean includeInGroupBy;
  Class<? extends BasePrimitiveDomainType> basePrimitiveDomainType;
  private final int priority;

  /***
   * generates HQLProperties for model extensions
   * 
   * @param hqlProperty
   *          property in the select clause of the HQL
   * @param jsonName
   *          name of the property in the generated JSONObject
   */
  public HQLProperty(String hqlProperty, String jsonName) {
    this(hqlProperty, jsonName, true, BasePrimitiveDomainType.class, 0);
  }

  /***
   * generates HQLProperties for model extensions
   * 
   * @param hqlProperty
   *          property in the select clause of the HQL
   * @param jsonName
   *          name of the property in the generated JSONObject
   * @param priority
   *          set the priority for HQL properties with the same name
   */
  public HQLProperty(String hqlProperty, String jsonName, int priority) {
    this(hqlProperty, jsonName, true, BasePrimitiveDomainType.class, priority);
  }

  /***
   * generates HQLProperties for model extensions
   * 
   * @param hqlProperty
   *          property in the select clause of the HQL
   * @param jsonName
   *          name of the property in the generated JSONObject
   * @param includeInGroupBy
   *          include this field in the GROUP BY
   */
  public HQLProperty(String hqlProperty, String jsonName, Boolean includeInGroupBy) {
    this(hqlProperty, jsonName, includeInGroupBy, BasePrimitiveDomainType.class, 0);
  }

  /***
   * generates HQLProperties for model extensions
   * 
   * @param hqlProperty
   *          property in the select clause of the HQL
   * @param jsonName
   *          name of the property in the generated JSONObject
   * @param includeInGroupBy
   *          include this field in the GROUP BY
   * @param basePrimitiveDomainTypeClass
   *          specify basePrimitiveDomainTypeClass
   */
  public HQLProperty(String hqlProperty, String jsonName, Boolean includeInGroupBy,
      Class<? extends BasePrimitiveDomainType> basePrimitiveDomainTypeClass) {
    this(hqlProperty, jsonName, includeInGroupBy, basePrimitiveDomainTypeClass, 0);
  }

  /***
   * generates HQLProperties for model extensions
   * 
   * @param hqlProperty
   *          property in the select clause of the HQL
   * @param jsonName
   *          name of the property in the generated JSONObject
   * @param includeInGroupBy
   *          include this field in the GROUP BY
   * @param basePrimitiveDomainTypeClass
   *          specify basePrimitiveDomainTypeClass
   * @param priority
   *          sets the priority for HQL properties with the same name
   */
  public HQLProperty(String hqlProperty, String jsonName, Boolean includeInGroupBy,
      Class<? extends BasePrimitiveDomainType> basePrimitiveDomainTypeClass, int priority) {
    this.hqlProperty = hqlProperty;
    this.jsonName = jsonName;
    this.includeInGroupBy = includeInGroupBy;
    this.basePrimitiveDomainType = basePrimitiveDomainTypeClass;
    this.priority = priority;
  }

  /**
   * @return the JSON Name
   */
  public String getJsonName() {
    return jsonName;
  }

  /**
   * @return the HQL Property
   */
  public String getHqlProperty() {
    return hqlProperty;
  }

  /**
   * @return the Base Primitive Domain Type
   */
  public Class<? extends BasePrimitiveDomainType> getBasePrimitiveDomainType() {
    return basePrimitiveDomainType;
  }

  /**
   * @return the priority
   */
  public int getPriority() {
    return priority;
  }
}
