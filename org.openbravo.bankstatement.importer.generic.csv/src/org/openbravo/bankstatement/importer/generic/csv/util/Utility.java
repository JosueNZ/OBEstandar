/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */

package org.openbravo.bankstatement.importer.generic.csv.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.bankstatement.importer.generic.csv.GenericCSVImporter;
import org.openbravo.bankstatement.importer.generic.csv.bean.GenericCsvImporterBean;
import org.openbravo.bankstatement.importer.generic.csv.bean.mappingstrategy.GenericHeaderColumnNameTranslateMappingStrategy;
import org.openbravo.bankstatement.importer.generic.csv.implementation.bean.GenericBankStatementLineBean;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.CsvToBean;

public class Utility {
  private static final Locale DECIMALSEPARATOR_COMMA = Locale.ITALY;
  private static final Locale DECIMALSEPARATOR_DOT = Locale.US;

  public static BigDecimal stringToBigDecimal(final String numberString,
      final String decimalSeparator) {
    Locale locale;
    if (",".equals(decimalSeparator)) {
      locale = DECIMALSEPARATOR_COMMA;
    } else if (".".equals(decimalSeparator)) {
      locale = DECIMALSEPARATOR_DOT;
    } else {
      locale = Locale.getDefault();
    }

    DecimalFormat dF = (DecimalFormat) DecimalFormat.getInstance(locale);
    dF.setParseBigDecimal(true);
    try {
      return (BigDecimal) dF.parse(numberString);
    } catch (ParseException e) {
      throw new OBException("Impossible to parse number: " + numberString, e);
    }
  }

  public static Date stringToDate(final String dateString, final String dateFormat) {
    try {
      return new SimpleDateFormat(dateFormat).parse(dateString);
    } catch (ParseException e) {
      throw new OBException("Impossible to parse string: " + dateString, e);
    }
  }

  public static <T extends GenericCsvImporterBean> List<T> parseCSV(Class<T> type,
      Map<String, String> columnMapping, InputStream in, String dateFormat, String decimalSeparator) {
    return parseCSV(type, columnMapping, in, dateFormat, decimalSeparator,
        GenericCSVImporter.DEFAULT_FIELDDELIMITER);
  }

  public static <T extends GenericCsvImporterBean> List<T> parseCSV(Class<T> type,
      Map<String, String> columnMapping, InputStream in, String dateFormat,
      String decimalSeparator, String fieldDelimiter) {
    return parseCSV(type, columnMapping, new BufferedReader(new InputStreamReader(in)), dateFormat,
        decimalSeparator, fieldDelimiter);
  }

  public static <T extends GenericCsvImporterBean> List<T> parseCSV(Class<T> type,
      Map<String, String> columnMapping, Reader reader, String dateFormat, String decimalSeparator) {
    return parseCSV(type, columnMapping, reader, dateFormat, decimalSeparator,
        GenericCSVImporter.DEFAULT_FIELDDELIMITER);
  }

  public static <T extends GenericCsvImporterBean> List<T> parseCSV(Class<T> type,
      Map<String, String> columnMapping, Reader reader, String dateFormat, String decimalSeparator,
      String fieldDelimiter) {
    GenericHeaderColumnNameTranslateMappingStrategy<T> strategy = new GenericHeaderColumnNameTranslateMappingStrategy<T>(
        dateFormat, decimalSeparator, fieldDelimiter);
    strategy.setType(type);
    strategy.setColumnMapping(columnMapping);
    CsvToBean<T> csv = new CsvToBean<T>();
    CSVReader csvReader = new CSVReader(reader, fieldDelimiter.charAt(0), '\"', '\\', 0, false,
        true);
    return csv.parse(strategy, csvReader);
  }

  public static FIN_BankStatementLine createFIN_BankStatementLine(
      GenericBankStatementLineBean bean, FIN_BankStatement bankstatement, Long lineNo) {
    final FIN_BankStatementLine newBankStatementLine = OBProvider.getInstance().get(
        FIN_BankStatementLine.class);
    newBankStatementLine.setBankStatement(bankstatement);
    newBankStatementLine.setClient(bankstatement.getClient());
    newBankStatementLine.setOrganization(bankstatement.getOrganization());
    newBankStatementLine.setDramount(bean.getDramount());
    newBankStatementLine.setCramount(bean.getCramount());
    newBankStatementLine.setLineNo(lineNo);
    if (StringUtils.isNotBlank(bean.getBpartnername())) {
      newBankStatementLine.setBpartnername(bean.getBpartnername().substring(0,
          bean.getBpartnername().length() > 60 ? 60 : bean.getBpartnername().length()));
    }
    if (StringUtils.isNotBlank(bean.getDescription())) {
      newBankStatementLine.setDescription(bean.getDescription().substring(0,
          bean.getDescription().length() > 2000 ? 2000 : bean.getDescription().length()));
    }
    newBankStatementLine.setTransactionDate(bean.getTransactionDate());
    if (StringUtils.isNotBlank(bean.getReferenceNo())) {
      newBankStatementLine.setReferenceNo(bean.getReferenceNo().substring(0,
          bean.getReferenceNo().length() > 30 ? 30 : bean.getReferenceNo().length()));
    } else {
      newBankStatementLine.setReferenceNo("**");
    }
    return newBankStatementLine;
  }
}
