/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */

package org.openbravo.bankstatement.importer.generic.csv.bean;

/**
 * Abstract class that every bean created for storing the Bank Statement CSV file data must extend.
 * It defines the date format and decimal separator configured into the system for the client
 * 
 * @author openbravo
 * 
 */
public abstract class GenericCsvImporterBean {
  private String dateFormat;
  private String decimalSeparator;
  private String fieldDelimiter;

  public final String getDecimalSeparator() {
    return decimalSeparator;
  }

  public final void setDecimalSeparator(String decimalSeparator) {
    this.decimalSeparator = decimalSeparator;
  }

  public final String getDateFormat() {
    return dateFormat;
  }

  public final void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public String getFieldDelimiter() {
    return fieldDelimiter;
  }

  public void setFieldDelimiter(String fieldDelimiter) {
    this.fieldDelimiter = fieldDelimiter;
  }

}
