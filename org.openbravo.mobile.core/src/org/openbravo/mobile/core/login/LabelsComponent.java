/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.login;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.MobileCoreConstants;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.module.ModuleDependency;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Message;

public class LabelsComponent {
  private static final Logger log = Logger.getLogger(LabelsComponent.class);

  public static JSONObject getLabels(String languageId, String moduleId) {
    try {
      String modules = getMobileAppDependantModuleIds(moduleId);
      JSONObject labels = new JSONObject();
      String hqlLabel = "select message.searchKey, message.messageText "//
          + "from ADMessage message " //
          + "where module.id in " + modules;
      Query qryLabel = OBDal.getInstance().getSession().createQuery(hqlLabel);
      for (Object qryLabelObject : qryLabel.list()) {
        final Object[] qryLabelObjectItem = (Object[]) qryLabelObject;
        labels.put(qryLabelObjectItem[0].toString(), qryLabelObjectItem[1].toString());
      }

      Language currentLanguage = null;
      Language computedLanguage = null;
      if (languageId != null) {
        currentLanguage = OBDal.getInstance().get(Language.class, languageId);
      } else {
        computedLanguage = getComputedLanguageObj();
      }

      String langId = currentLanguage != null ? currentLanguage.getId() : computedLanguage.getId();
      String langSk = currentLanguage != null ? currentLanguage.getLanguage() : computedLanguage
          .getLanguage();
      String hqlTrlLabels = "select trl.message.searchKey, trl.messageText from ADMessageTrl trl where trl.message.module.id in "
          + modules + " and trl.language.id='" + langId + "'";
      Query qryTrlLabels = OBDal.getInstance().getSession().createQuery(hqlTrlLabels);
      for (Object qryTrlObj : qryTrlLabels.list()) {
        final Object[] qryTrlObject = (Object[]) qryTrlObj;
        labels.put(qryTrlObject[0].toString(), qryTrlObject[1].toString());
      }

      // For symbols
      Message message = OBDal.getInstance().get(Message.class, "EFF14FC95BD84896969B5EB6E943BE54");
      labels.put(message.getSearchKey(), message.getMessageText());

      labels.put("languageSk", langSk);
      labels.put("languageId", langId);
      labels.put("userId", OBContext.getOBContext().getUser().getId());
      return labels;
    } catch (Exception e) {
      log.error("There was an exception while generating the Mobile labels", e);
      return new JSONObject();
    }
  }

  public static JSONObject getLists(String languageId, String moduleId) {
    try {
      String modules = getMobileAppDependantModuleIds(moduleId);
      JSONObject lists = new JSONObject();
      String computedLanguage = getComputedLanguage();
      String langId = languageId != null ? languageId : computedLanguage;
      String hqlLists = "select list.reference.id, list.searchKey as id, coalesce("
          + " (select trl.name from list.aDListTrlList trl where trl.language.id = '" + langId
          + "'), list.name) as name "
          + "from ADList list " //
          + "where list.reference.module.id in " + modules + " and list.module.id in " + modules
          + "order by list.reference.id, list.sequenceNumber";
      Query qryLists = OBDal.getInstance().getSession().createQuery(hqlLists);
      JSONArray listValues = null;
      String lastReference = "";
      for (Object qryTrlObj : qryLists.list()) {
        final Object[] qryTrlObject = (Object[]) qryTrlObj;
        if (!lastReference.equals(qryTrlObject[0].toString())) {
          listValues = new JSONArray();
          lists.put(qryTrlObject[0].toString(), listValues);
          lastReference = qryTrlObject[0].toString();
        }
        JSONObject item = new JSONObject();
        item.put("id", qryTrlObject[1].toString());
        item.put("name", qryTrlObject[2].toString());
        listValues.put(item);
      }
      lists.put("languageId", computedLanguage);
      return lists;
    } catch (Exception e) {
      log.error("There was an exception while generating the Mobile reference list", e);
      return new JSONObject();
    }
  }

  public static String getMobileAppDependantModuleIds(String moduleId) {
    StringBuffer ids = new StringBuffer();

    List<Module> dependantModules = new ArrayList<Module>();
    Module theModule = OBDal.getInstance().get(Module.class, moduleId);

    // Add the module itself and mobile core as dependency
    dependantModules.add(theModule);
    dependantModules.add(OBDal.getInstance().get(Module.class, MobileCoreConstants.MODULE_ID));

    OBCriteria<ModuleDependency> allDeps = OBDal.getInstance().createCriteria(
        ModuleDependency.class);
    dependantModules.addAll(getDependantModules(theModule, allDeps.list()));

    int n = 0;
    ids.append("(");
    for (Module mod : dependantModules) {
      if (n > 0) {
        ids.append(",");
      }
      ids.append("'" + mod.getId() + "'");
      n++;
    }
    ids.append(")");
    return ids.toString();
  }

  private static List<Module> getDependantModules(Module module, List<ModuleDependency> allDeps) {
    List<Module> moduleList = new ArrayList<Module>();
    for (ModuleDependency depModule : allDeps) {
      if (depModule.getDependentModule().equals(module)
          && !moduleList.contains(depModule.getModule())) {
        moduleList.add(depModule.getModule());
        moduleList.addAll(getDependantModules(depModule.getModule(), allDeps));
      }
    }
    return moduleList;
  }

  private static String getComputedLanguage() {
    return LabelsComponent.getComputedLanguageObj().getId();
  }

  private static Language getComputedLanguageObj() {
    Language computedLanguage = OBDal.getInstance().get(Language.class, "192");
    if (OBContext.getOBContext().getUser().getId().equals("0")) {
      Client sysClient = OBDal.getInstance().get(Client.class, "0");
      computedLanguage = sysClient.getLanguage();
    } else {
      computedLanguage = OBContext.getOBContext().getLanguage();
    }
    return computedLanguage;
  }
}
