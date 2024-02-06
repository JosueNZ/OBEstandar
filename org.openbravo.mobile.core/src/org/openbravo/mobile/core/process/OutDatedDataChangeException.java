/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.process;

import org.openbravo.base.exception.OBException;

/**
 * This exception is used in case of OutDated Data Exception.
 * 
 */
public class OutDatedDataChangeException extends OBException {

  private static final long serialVersionUID = 1L;

  public OutDatedDataChangeException(String message) {
    super(message);
    getLogger().error(message, this);
  }
}
