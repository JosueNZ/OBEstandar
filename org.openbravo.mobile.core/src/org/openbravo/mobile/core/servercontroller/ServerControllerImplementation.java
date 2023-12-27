/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import org.openbravo.server.ServerController;

/**
 * Implementation of AbstractServerControllerImplementation that uses the MobileServerController to
 * retrieve information about the current server
 */

public class ServerControllerImplementation extends ServerController {

  private boolean isInitialized = false;
  private boolean isCentralServerCachedValue;

  @Override
  public boolean isThisACentralServer() {
    if (!isInitialized) {
      try {
        isCentralServerCachedValue = MobileServerController.getInstance().isThisACentralServer();
      } catch (Exception e) {
        // it might be that no mobile servers are defined, in that case assume the current server is
        // a central server
        isCentralServerCachedValue = true;
      } finally {
        isInitialized = true;
      }
    }
    return isCentralServerCachedValue;
  }

  @Override
  public boolean isThisAStoreServer() {
    return !isThisACentralServer();
  }

  @Override
  public int getPriority() {
    return 1;
  }
}
