/*
 ************************************************************************************
 * Copyright (C) 2012-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.login;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.mobile.core.MobileDefaults;
import org.openbravo.mobile.core.MobileDefaultsHandler;
import org.openbravo.mobile.core.process.JSONProcessSimple;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.service.json.JsonConstants;

public class ProfileUtils extends JSONProcessSimple {
  @Inject
  private MobileDefaultsHandler defaultsHandler;

  @Override
  protected boolean bypassSecurity() {
    return true;
  }

  @Override
  protected boolean bypassPreferenceCheck() {
    return true;
  }

  @Override
  public JSONObject exec(JSONObject jsonsent) throws JSONException, ServletException {
    OBContext.setAdminMode(true);
    try {
      JSONObject response = new JSONObject();
      String appName = jsonsent.getString("appName");
      MobileDefaults defaults = defaultsHandler.getDefaults(appName);
      response.put("status", 0);
      response.put("data", getProfileData(defaults));

      return response;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private JSONObject getProfileData(MobileDefaults defaults) throws JSONException {
    final JSONObject result = new JSONObject();
    result.put("language", createLanguageFormItemInfo());
    result.put("role", createRoleInfo(defaults));
    return result;
  }

  private JSONObject createRoleInfo(MobileDefaults defaults) throws JSONException {
    final JSONObject formItemInfo = new JSONObject();
    formItemInfo.put("value", OBContext.getOBContext().getRole().getId());
    final List<Role> roles = getRoles(defaults);
    final List<Role> sortedRoles = new ArrayList<Role>(roles);
    DalUtil.sortByIdentifier(sortedRoles);
    final JSONArray valueMap = new JSONArray();
    for (Role role : sortedRoles) {
      final JSONObject valueMapItem = new JSONObject();
      valueMapItem.put(JsonConstants.ID, role.getId());
      valueMapItem.put(JsonConstants.IDENTIFIER, role.getIdentifier() + " - "
          + role.getClient().getIdentifier());
      valueMap.put(valueMapItem);
    }
    formItemInfo.put("valueMap", valueMap);

    // now for each role store the information
    final JSONArray jsonRoles = new JSONArray();

    for (Role role : roles) {
      final JSONObject jsonRole = new JSONObject();
      jsonRole.put("id", role.getId());

      jsonRole.put("client", role.getClient().getIdentifier());

      // now set the organizations
      final List<Organization> orgs = getOrganizations(role.getId());
      final JSONArray orgValueMap = new JSONArray();
      for (Organization org : orgs) {
        final JSONObject orgValueMapItem = new JSONObject();
        orgValueMapItem.put(JsonConstants.ID, org.getId());
        orgValueMapItem.put(JsonConstants.IDENTIFIER, org.getIdentifier());
        orgValueMap.put(orgValueMapItem);
      }
      jsonRole.put("organizationValueMap", orgValueMap);
      jsonRole.put("warehouseOrgMap", getWarehouses(role.getClient().getId(), orgs));
      jsonRoles.put(jsonRole);
    }
    formItemInfo.put("roles", jsonRoles);
    return formItemInfo;
  }

  protected JSONArray getWarehouses(String clientId, List<Organization> orgs) throws JSONException {
    List<JSONObject> orgWarehouseArray = new ArrayList<JSONObject>();
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(clientId);
    for (Organization org : orgs) {
      JSONObject orgWarehouse = new JSONObject();
      orgWarehouse.put("orgId", org.getId());
      StringBuffer hqlQuery = new StringBuffer();
      hqlQuery.append("organization.id in (:orgList) AND ");
      hqlQuery.append("client.id=:clientId AND ");
      hqlQuery.append("organization.active=true ");
      hqlQuery.append("order by name");
      final OBQuery<Warehouse> warehouses = OBDal.getInstance().createQuery(Warehouse.class,
          hqlQuery.toString());
      warehouses.setNamedParameter("orgList", osp.getNaturalTree(org.getId()));
      warehouses.setNamedParameter("clientId", clientId);
      warehouses.setFilterOnReadableClients(false);
      warehouses.setFilterOnReadableOrganization(false);
      orgWarehouse.put("warehouseMap", createValueMapObject(warehouses.list(), null, null));
      orgWarehouseArray.add(orgWarehouse);
    }
    return new JSONArray(orgWarehouseArray);
  }

  protected List<Organization> getOrganizations(String roleId) throws JSONException {
    final OBQuery<RoleOrganization> roleOrgs = OBDal.getInstance().createQuery(
        RoleOrganization.class, "role.id=:roleId and organization.active=true");
    roleOrgs.setFilterOnReadableClients(false);
    roleOrgs.setFilterOnReadableOrganization(false);
    roleOrgs.setNamedParameter("roleId", roleId);
    final List<Organization> orgs = new ArrayList<Organization>();
    for (RoleOrganization roleOrg : roleOrgs.list()) {
      if (!orgs.contains(roleOrg.getOrganization())) {
        orgs.add(roleOrg.getOrganization());
      }
    }
    DalUtil.sortByIdentifier(orgs);
    return orgs;
  }

  private JSONObject createLanguageFormItemInfo() throws JSONException {
    final JSONObject formItemInfo = new JSONObject();
    formItemInfo.put("value", OBContext.getOBContext().getLanguage().getId());

    final OBQuery<Language> languages = OBDal.getInstance().createQuery(
        Language.class,
        "(" + Language.PROPERTY_SYSTEMLANGUAGE + "=true or " + Language.PROPERTY_BASELANGUAGE
            + "=true)");
    languages.setFilterOnReadableClients(false);
    languages.setFilterOnReadableOrganization(false);
    formItemInfo.put("valueMap", createValueMapObject(languages.list(), "SK", "language"));
    return formItemInfo;
  }

  protected JSONArray createValueMapObject(List<? extends BaseOBObject> objects, String extraField,
      String extraFieldProperty) throws JSONException {
    // sort the list by their identifier
    DalUtil.sortByIdentifier(objects);
    final JSONArray jsonArray = new JSONArray();
    for (BaseOBObject bob : objects) {
      final JSONObject jsonArrayItem = new JSONObject();
      jsonArrayItem.put(JsonConstants.ID, (String) bob.getId());
      jsonArrayItem.put(JsonConstants.IDENTIFIER, (String) bob.getIdentifier());
      if (extraField != null) {
        jsonArrayItem.put(extraField, (String) bob.getValue(extraFieldProperty));
      }
      jsonArray.put(jsonArrayItem);
    }
    return jsonArray;
  }

  protected List<Role> getRoles(MobileDefaults defaults) {
    String formId = defaults.getFormId();
    String clientId = OBContext.getOBContext().getCurrentClient().getId();
    String whereClause = "as r where userContact.id=:user and role.active=true ";
    if (formId != null) {
      whereClause += "and exists (select 1 from ADFormAccess a "//
          + " where a.active = true" //
          + " and a.role.id = r.role.id "//
          + " and a.role.client.id = :clientId "//
          + " and a.specialForm.id = :formId) ";
    }
    whereClause += "order by role.name asc";

    final OBQuery<UserRoles> rolesQuery = OBDal.getInstance().createQuery(UserRoles.class,
        whereClause);

    rolesQuery.setFilterOnReadableClients(false);
    rolesQuery.setFilterOnReadableOrganization(false);
    rolesQuery.setNamedParameter("user", OBContext.getOBContext().getUser().getId());

    if (formId != null) {
      rolesQuery.setNamedParameter("formId", formId);
      rolesQuery.setNamedParameter("clientId", clientId);
    }

    final List<Role> result = new ArrayList<Role>();
    for (UserRoles userRole : rolesQuery.list()) {
      if (!result.contains(userRole.getRole())) {
        result.add(userRole.getRole());
      }
    }
    return result;
  }
}
