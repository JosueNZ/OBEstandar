/*
 ************************************************************************************
 * Copyright (C) 2012-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */

package org.openbravo.bankstatement.importer.generic.csv.bean.mappingstrategy;

import org.openbravo.bankstatement.importer.generic.csv.GenericCSVImporter;
import org.openbravo.base.exception.OBException;

import au.com.bytecode.opencsv.bean.HeaderColumnNameTranslateMappingStrategy;

public class GenericHeaderColumnNameTranslateMappingStrategy<GenericCsvImporterBean> extends
    HeaderColumnNameTranslateMappingStrategy<GenericCsvImporterBean> {

  private String dateFormat;
  private String decimalSeparator;
  private String fieldDelimiter = GenericCSVImporter.DEFAULT_FIELDDELIMITER;

  public GenericHeaderColumnNameTranslateMappingStrategy(final String dateFormat,
      final String decimalSeparator) {
    this.dateFormat = dateFormat;
    this.decimalSeparator = decimalSeparator;
  }

  public GenericHeaderColumnNameTranslateMappingStrategy(final String dateFormat,
      final String decimalSeparator, final String fieldDelimiter) {
    this.dateFormat = dateFormat;
    this.decimalSeparator = decimalSeparator;
    this.fieldDelimiter = fieldDelimiter;
  }

  public GenericCsvImporterBean createBean() throws InstantiationException, IllegalAccessException {
    try {
      GenericCsvImporterBean bean = type.getDeclaredConstructor().newInstance();
      Class.forName("org.openbravo.bankstatement.importer.generic.csv.bean.GenericCsvImporterBean")
          .getMethod("setDateFormat", String.class).invoke(bean, dateFormat);
      Class.forName("org.openbravo.bankstatement.importer.generic.csv.bean.GenericCsvImporterBean")
          .getMethod("setDecimalSeparator", String.class).invoke(bean, decimalSeparator);
      Class.forName("org.openbravo.bankstatement.importer.generic.csv.bean.GenericCsvImporterBean")
          .getMethod("setFieldDelimiter", String.class).invoke(bean, fieldDelimiter);
      // bean.getClass().getSuperclass().getMethod("setDateFormat", String.class)
      // .invoke(bean, dateFormat);
      // bean.getClass().getSuperclass().getMethod("setDecimalSeparator", String.class)
      // .invoke(bean, decimalSeparator);
      return bean;
    } catch (Exception e) {
      throw new OBException("Exception trying to set configuration parameters to bean", e);
    }
  }
}
