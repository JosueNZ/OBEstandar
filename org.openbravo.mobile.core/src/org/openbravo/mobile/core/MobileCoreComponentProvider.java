/*
 ************************************************************************************
 * Copyright (C) 2012-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.BaseComponentProvider.ComponentResource.ComponentResourceType;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.mobile.core.model.ClientModelComponent;

@ApplicationScoped
@ComponentProvider.Qualifier(MobileCoreConstants.COMPONENT_TYPE)
public class MobileCoreComponentProvider extends BaseComponentProvider {

  static {
    // Set dependency on Mobile Core app
    BaseComponentProvider.setAppDependencies(MobileCoreConstants.RETAIL_CORE,
        Arrays.asList(MobileCoreConstants.APP_IDENTIFIER));
  }

  private List<ComponentResource> globalResources = null;

  @Override
  public Component getComponent(final String componentId, final Map<String, Object> parameters) {
    if (MobileCoreConstants.LIBRARIES_COMPONENT.equals(componentId)) {
      final LibraryResource c = getComponent(LibraryResource.class);
      c.setParameters(parameters);
      c.setId(componentId);
      return c;
    } else if (KernelConstants.RESOURCE_COMPONENT_ID.equals(componentId)) {
      final MobileStaticResourceComponent c = getComponent(MobileStaticResourceComponent.class);
      c.setParameters(parameters);
      c.setId(componentId);
      return c;
    } else if (MobileCoreConstants.CLIENT_MODEL_COMPONENT.equals(componentId)) {
      final ClientModelComponent component = getComponent(ClientModelComponent.class);
      component.setId(componentId);
      component.setParameters(parameters);
      return component;
    }
    // else if ("Proxy".equals(componentId)) {
    // return getProxiedComponent(parameters);
    // }
    throw new IllegalStateException("Component not supported");
  }

  @Override
  public synchronized List<ComponentResource> getGlobalComponentResources() {
    if (globalResources != null) {
      return globalResources;
    }
    globalResources = new ArrayList<ComponentResource>();

    final String prefix = "web/" + MobileCoreConstants.MODULE_JAVAPACKAGE;

    // add the files that have to be loaded
    // how they are sorted, is critical

    final ArrayList<String> jsDependency = new ArrayList<String>();
    jsDependency.add("utils/ob-check"); // must be first because don't want to load a wrong url
    jsDependency.add("utils/ob-debug"); // must be second because the OB namespace is defined in it
    jsDependency.add("utils/ob-versionmanagement");
    jsDependency.add("main");
    jsDependency.add("utils/ob-synchronization");
    jsDependency.add("utils/ob-localStorage");
    jsDependency.add("data/ob-cache");
    jsDependency.add("data/ob-datasource");
    jsDependency.add("data/ob-requestrouter");
    jsDependency.add("utils/ob-utilities");
    jsDependency.add("data/ob-dal");
    jsDependency.add("data/ob-model");
    jsDependency.add("data/ob-windowmodel");
    jsDependency.add("component/ob-dateformat");
    jsDependency.add("component/ob-terminal-component");
    jsDependency.add("model/ob-router");
    jsDependency.add("model/ob-terminal-model");
    jsDependency.add("model/logclient");
    jsDependency.add("model/ob-message");
    jsDependency.add("component/ob-commonbuttons");
    jsDependency.add("component/ob-login");
    jsDependency.add("utils/logClientSyncUtils");
    jsDependency.add("utils/ob-testregistry");
    jsDependency.add("utils/ob-utilitiesui");
    jsDependency.add("utils/ob-arithmetic");
    jsDependency.add("utils/ob-hooks");
    jsDependency.add("component/ob-clock");
    jsDependency.add("component/ob-windowview");
    jsDependency.add("component/ob-layout");
    jsDependency.add("component/ob-scrollabletable");
    jsDependency.add("utils/ob-i18n");
    jsDependency.add("../../org.openbravo.client.application/js/utilities/ob-utilities-number");
    jsDependency.add("../../org.openbravo.client.application/js/utilities/ob-utilities-date");
    jsDependency.add("component/ob-keyboard");
    jsDependency.add("component/ob-context-menu");
    jsDependency.add("component/ob-listcontextmenu");
    jsDependency.add("component/ob-keypadbasic");
    jsDependency.add("component/ob-table");
    jsDependency.add("component/dialog/ob-profile");
    jsDependency.add("component/dialog/ob-modalonline");
    jsDependency.add("component/dialog/ob-logout");
    jsDependency.add("component/dialog/ob-modalmodelstosync");
    jsDependency.add("component/dialog/ob-modalitemstosync");
    jsDependency.add("component/dialog/ob-expirationpassword");
    jsDependency.add("component/dialog/ob-advancedfilterselector");
    jsDependency.add("component/dialog/ob-modaladvancedfilters");
    jsDependency.add("component/ob-menu");
    jsDependency.add("component/dialog/ob-properties");
    jsDependency.add("offline/ob-session");
    jsDependency.add("offline/ob-user");
    jsDependency.add("component/obpos-approval");
    jsDependency.add("component/ob-datepicker");
    jsDependency.add("component/ob-markdown");
    jsDependency.add("component/ob-performancetest");

    final ArrayList<String> jsRetailDependency = new ArrayList<String>();
    jsRetailDependency.add("component/ob-retail-product-browser");
    jsRetailDependency.add("component/ob-retail-filterbuilder");
    jsRetailDependency.add("component/ob-retail-searchproducts");
    jsRetailDependency.add("component/ob-retail-searchproductcharacteristic");

    final ArrayList<String> cssDependency = new ArrayList<String>();
    cssDependency.add("css/ob-login");
    cssDependency.add("css/ob-standard");

    for (final String resource : jsDependency) {
      globalResources.add(createComponentResource(ComponentResourceType.Static, prefix + "/source/"
          + resource + ".js", MobileCoreConstants.APP_IDENTIFIER));
    }

    for (final String resource : jsRetailDependency) {
      globalResources.add(createComponentResource(ComponentResourceType.Static, prefix
          + "/source/retail/" + resource + ".js", MobileCoreConstants.RETAIL_CORE));
    }

    for (final String resource : cssDependency) {
      globalResources.add(createComponentResource(ComponentResourceType.Stylesheet, prefix
          + "/assets/" + resource + ".css", MobileCoreConstants.APP_IDENTIFIER));
    }

    return globalResources;
  }
}
