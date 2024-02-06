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
 * The states of a mobile server.
 * 
 * @author mtaal
 */
public enum MobileServerState {
  ONLINE("ON"), TRANSITION_TO_OFFLINE("TOFF"), OFFLINE("OFF"), TRANSITION_TO_ONLINE("TON");

  private String value = null;

  @Override
  public String toString() {
    return String.format("%s (value = %s)", name(), value);
  }

  public String getValue() {
    return this.value;
  }

  public static MobileServerState getMobileServerState(String value) {

    MobileServerState[] enums = MobileServerState.values();
    for (int i = 0; i < enums.length; i++) {
      if (enums[i].getValue().equals(value)) {
        return enums[i];
      }
    }
    return null;
  }

  private MobileServerState(final String value) {
    this.value = value;
  }

}
