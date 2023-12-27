/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

/**
 * Is the base class for all classes implementing transition to online logic. When the system
 * transitions back to online functional code needs to send out transactions do different servers to
 * make sure that they are all processed.
 * 
 * When the processing method returns the caller will call the isReadyToGoOnline method if false is
 * returned then the system will not go online but remain in transition to offline status.
 * 
 * @author mtaal
 */
public abstract class MobileServerTransitionToOnlineHandler {

  /**
   * Is called when the main servers are again reachable and transactions can be processed.
   */
  public abstract void processTransactions();

  /**
   * Should return true if the internal queue of transactions is empty
   */
  public abstract boolean isReadyToGoOnline();

  public String getOfflineLog() {
    return this.getClass().getSimpleName();
  }

  /**
   * Called before the actual transition to online to do checks if the transition to online process can start.
   * 
   * The default implementation calls the {@link #processTransactions()} and the {@link #isReadyToGoOnline()} and returns the
   * result.
   */
  public boolean canStartTransitionToOnline() {
    processTransactions();
    return isReadyToGoOnline();
  }

}
