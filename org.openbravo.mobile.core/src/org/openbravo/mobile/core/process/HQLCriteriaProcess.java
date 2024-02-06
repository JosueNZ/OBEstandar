/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.process;

public abstract class HQLCriteriaProcess {
  public static String OPERATOR_AND = "AND";
  public static String OPERATOR_OR = "OR";

  @Deprecated
  public String getHQLFilter() {
    return null;
  }

  public String getHQLFilter(String params) {
    return getHQLFilter();
  }

  public String getOperatorForMultipleFilters() {
    return OPERATOR_OR;
  }

}
