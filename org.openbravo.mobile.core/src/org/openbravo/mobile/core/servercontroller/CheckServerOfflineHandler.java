/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

/**
 * Is a base class which can be extended by others to implement specific code which checks if
 * important external servers are reachable/offline or not. Is called from the store server as well
 * as on the central server.
 * 
 * Note that if true is returned on the isServerOffline call then the calling server will go
 * offline. So if the code is executed on the central server then the central server will go
 * offline.
 * 
 * These handlers are called when a server goes/is offline but also at specific time intervals when
 * the check server state process request is scheduled.
 * 
 * @author mtaal
 */
public abstract class CheckServerOfflineHandler {

  /**
   * Called to check if there is a server(s) offline. If one of the relevant servers is offline then
   * return true, if all are online then return false.
   */
  public abstract boolean isServerOffline();
}
