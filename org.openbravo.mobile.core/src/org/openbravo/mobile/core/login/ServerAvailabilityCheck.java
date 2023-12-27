/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.login;

import org.codehaus.jettison.json.JSONObject;

/**
 * Used to implement a check to decide if server is available or not.
 * 
 * Should throw an exception in case a problem was detected in the server
 * 
 * @author openbravo
 *
 */
public abstract class ServerAvailabilityCheck {

  public abstract void doCheck(JSONObject params, JSONObject responseObj);

}
