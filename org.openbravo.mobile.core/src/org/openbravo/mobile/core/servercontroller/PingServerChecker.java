/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import org.openbravo.mobile.core.MobileServerDefinition;

/**
 * Allows to define whether if the given mobile server should be pinged to determine its status. If
 * the pingShouldBeDone returns false, then the ping will not be done, and the given mobile server
 * will be considered offline
 */
public interface PingServerChecker {

  boolean pingShouldBeDone(MobileServerDefinition mobileServer);

}
