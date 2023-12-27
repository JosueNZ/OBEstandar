/*
 ************************************************************************************
 * Copyright (C) 2012-2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core;

import org.openbravo.client.kernel.StaticResourceComponent;

/**
 * Provides the components which generates the static resources such as the javascript loaded in the
 * application.
 * 
 * @author mtaal, ral
 */
public class MobileStaticResourceComponent extends StaticResourceComponent {

  @Override
  public String generateResult(String staticResourceFileName) {
    return ("enyo.depends('" + getContextUrl() + StaticResourceComponent.GEN_TARGET_LOCATION + "/"
        + staticResourceFileName + ".js" + "');").replaceAll("//", "/");
  }

  @Override
  public boolean bypassAuthentication() {
    return true;
  }
}