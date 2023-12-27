/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */

package org.openbravo.bankstatement.importer.generic.csv.implementation.bean;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;

import org.openbravo.bankstatement.importer.generic.csv.bean.GenericCsvImporterBean;
import org.openbravo.bankstatement.importer.generic.csv.util.Utility;

public class GenericBankStatementLineBean extends GenericCsvImporterBean {

  public static final String PROPERTY_BPARTNERNAME = "bpartnername";
  public static final String PROPERTY_REFERENCENO = "referenceNo";
  public static final String PROPERTY_DESCRIPTION = "description";
  public static final String PROPERTY_TRANSACTIONDATESTRING = "transactionDateString";
  public static final String PROPERTY_CRAMOUNTSTRING = "cramountString";
  public static final String PROPERTY_DRAMOUNTSTRING = "dramountString";

  private String bpartnername;

  private BigDecimal cramount;
  private String cramountString;

  private BigDecimal dramount;
  private String dramountString;

  private String referenceNo;
  private String description;
  private String transactionDateString;
  private Date transactionDate;

  public String getBpartnername() {
    return bpartnername;
  }

  public void setBpartnername(String bpartnername) {
    this.bpartnername = bpartnername;
  }

  public BigDecimal getCramount() {
    return cramount;
  }

  public void setCramount(BigDecimal cramount) {
    this.cramount = cramount;
  }

  public BigDecimal getDramount() {
    return dramount;
  }

  public void setDramount(BigDecimal dramount) {
    this.dramount = dramount;
  }

  public String getReferenceNo() {
    return referenceNo;
  }

  public void setReferenceNo(String referenceNo) {
    this.referenceNo = referenceNo;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getTransactionDateString() {
    return transactionDateString;
  }

  public Date getTransactionDate() {
    return transactionDate;
  }

  public void setTransactionDate(Date transactionDate) {
    this.transactionDate = transactionDate;
  }

  public String getCramountString() {
    return cramountString;
  }

  public String getDramountString() {
    return dramountString;
  }

  public void setCramountString(String cramountString) {
    this.cramountString = cramountString;
    setCramount(Utility.stringToBigDecimal(cramountString, getDecimalSeparator()));
  }

  public void setDramountString(String dramountString) {
    this.dramountString = dramountString;
    setDramount(Utility.stringToBigDecimal(dramountString, getDecimalSeparator()));
  }

  public void setTransactionDateString(String transactionDateString) throws ParseException {
    this.transactionDateString = transactionDateString;
    setTransactionDate(Utility.stringToDate(transactionDateString, getDateFormat()));
  }

}
