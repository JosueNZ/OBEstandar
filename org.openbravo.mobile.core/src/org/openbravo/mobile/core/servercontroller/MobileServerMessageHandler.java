/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import org.openbravo.model.common.enterprise.Organization;

/**
 * Is called to process a message received on the server.
 */
public abstract class MobileServerMessageHandler {

  /**
   * Is called to process the message, if the message is not processed and should/can be processed
   * by someone else then return false. If the message is processed by this handler then return
   * true.
   * 
   * Handlers are called in order of their priority, the default priority is 10.
   */
  public abstract boolean processMessage(Organization org, String json);

  /**
   * Return the priority, the priority determines the order (lowest number first) in which handlers
   * are called to process a message. The default priority is 10.
   */
  public int getPriority() {
    return 10;
  }

}
