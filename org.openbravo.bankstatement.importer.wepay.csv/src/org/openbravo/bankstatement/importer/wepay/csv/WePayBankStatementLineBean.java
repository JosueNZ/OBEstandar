/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */

package org.openbravo.bankstatement.importer.wepay.csv;

import java.math.BigDecimal;

import org.openbravo.bankstatement.importer.generic.csv.implementation.bean.GenericBankStatementLineBean;
import org.openbravo.bankstatement.importer.generic.csv.util.Utility;

public class WePayBankStatementLineBean extends GenericBankStatementLineBean {
  public static final String PROPERTY_AMOUNTSTRING = "amountString";
  public static final String PROPERTY_EMAIL = "email";
  public static final String PROPERTY_MEMO = "memo";
  public static final String PROPERTY_STATUS = "status";

  private BigDecimal amount;
  private String amountString;

  private String email;
  private String memo;
  private String status;

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getAmountString() {
    return amountString;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getMemo() {
    return memo;
  }

  public void setMemo(String memo) {
    this.memo = memo;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setAmountString(String amountString) {
    this.amountString = amountString;
    setAmount(Utility.stringToBigDecimal(amountString, getDecimalSeparator()));

    if (getAmount().compareTo(BigDecimal.ZERO) >= 0) {
      // Payment
      setCramount(getAmount());
      setDramount(BigDecimal.ZERO);
    } else {
      // Withdrawal
      setDramount(getAmount());
      setCramount(BigDecimal.ZERO);
    }
  }
}
