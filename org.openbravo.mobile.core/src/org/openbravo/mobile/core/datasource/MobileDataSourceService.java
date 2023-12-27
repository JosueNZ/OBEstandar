/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.datasource;

import java.util.Map;

import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.service.json.JsonConstants;

/**
 * MobileDataSourceService is used by retail online DAL datasources.
 */
public class MobileDataSourceService extends DefaultDataSourceService {

  @Override
  public void checkEditDatasourceAccess(Map<String, String> parameter) {
  }

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
  }

  @Override
  public String fetch(Map<String, String> parameters) {
    updateIdParameterToEntityName(parameters);
    return super.fetch(parameters);
  }

  @Override
  public String add(Map<String, String> parameters, String content) {
    updateIdParameterToEntityName(parameters);
    return super.add(parameters, content);
  }

  @Override
  public String remove(Map<String, String> parameters) {
    updateIdParameterToEntityName(parameters);
    return super.remove(parameters);
  }

  @Override
  public String update(Map<String, String> parameters, String content) {
    updateIdParameterToEntityName(parameters);
    return super.update(parameters, content);

  }

  private void updateIdParameterToEntityName(Map<String, String> parameters) {
    // id parameter is used to select the standard datasource invoked from mobile infrastructure
    parameters.put(JsonConstants.ENTITYNAME, parameters.get(JsonConstants.ID));
    parameters.remove(JsonConstants.ID);
  }
}