/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */

package org.openbravo.test;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import junit.framework.TestCase;

import org.openbravo.bankstatement.importer.generic.csv.GenericCSVImporter;
import org.openbravo.bankstatement.importer.generic.csv.implementation.GenericCSVImporterImplementation;
import org.openbravo.bankstatement.importer.generic.csv.implementation.bean.GenericBankStatementLineBean;

public class GenericCSVImporterTest extends TestCase {

  private static final String DECIMALSEPARATOR_COMMA = ",";
  private static final String DECIMALSEPARATOR_DOT = ".";

  private static final String DATEFORMAT_1 = "dd/MM/yyyy";
  private static final String DATEFORMAT_2 = "MM/dd/yyyy";
  private static final String DATEFORMAT_3 = "yyyyMMdd";

  public void test_Strings() throws UnsupportedEncodingException {
    final String s = "Transaction Date,Reference No.,Business Partner Name,Amount OUT,Amount IN,Description\n"
        + "20/10/2012,000001,Foo Bar,1500,\"150,65\",\n"
        + "01/12/2012,\"25,123\",My Business Partner,2222,\"0,005\",A description for this line";

    GenericCSVImporterImplementation importer = new GenericCSVImporterImplementation();
    importer.setIn(new ByteArrayInputStream(s.getBytes("UTF-8")));
    importer.setDateFormat(DATEFORMAT_1);
    importer.setDecimalSeparator(DECIMALSEPARATOR_COMMA);
    importer.setFieldDelimiter(GenericCSVImporter.DEFAULT_FIELDDELIMITER);
    List<GenericBankStatementLineBean> lines = importer.getBankStatementLines();

    assertTrue(lines.size() == 2);
    assertTrue(lines.get(0).getBpartnername().equals("Foo Bar"));
    assertTrue(lines.get(1).getBpartnername().equals("My Business Partner"));

    assertTrue(lines.get(0).getReferenceNo().equals("000001"));
    assertTrue(lines.get(1).getReferenceNo().equals("25,123"));
  }

  public void test_comma() throws UnsupportedEncodingException, ParseException {
    final String s = "Transaction Date,Reference No.,Business Partner Name,Amount OUT,Amount IN,Description\n"
        + "20/10/2012,000001,Foo Bar,1500,\"150,65\",\n"
        + "01/12/2012,\"25,123\",My Business Partner,2222,\"0,005\",A description for this line";

    GenericCSVImporterImplementation importer = new GenericCSVImporterImplementation();
    importer.setIn(new ByteArrayInputStream(s.getBytes("UTF-8")));
    importer.setDateFormat(DATEFORMAT_1);
    importer.setDecimalSeparator(DECIMALSEPARATOR_COMMA);
    importer.setFieldDelimiter(GenericCSVImporter.DEFAULT_FIELDDELIMITER);
    List<GenericBankStatementLineBean> lines = importer.getBankStatementLines();

    assertTrue(lines.size() == 2);

    assertTrue(lines.get(0).getDramount().compareTo(getBigDecimal("1500")) == 0);
    assertTrue(lines.get(0).getCramount().compareTo(getBigDecimal("150,65")) == 0);

    assertTrue(lines.get(1).getDramount().compareTo(getBigDecimal("2222")) == 0);
    assertTrue(lines.get(1).getCramount().compareTo(getBigDecimal("0,005")) == 0);
  }

  public void test_comma_complex() throws UnsupportedEncodingException, ParseException {
    final String s = "Transaction Date,Reference No.,Business Partner Name,Amount OUT,Amount IN,Description\n"
        + "20/10/2012,000001,Foo Bar,1500,\"999.150,65\",\n"
        + "01/12/2012,\"25,123\",My Business Partner,999.2222,\"0,005\",A description for this line";

    GenericCSVImporterImplementation importer = new GenericCSVImporterImplementation();
    importer.setIn(new ByteArrayInputStream(s.getBytes("UTF-8")));
    importer.setDateFormat(DATEFORMAT_1);
    importer.setDecimalSeparator(DECIMALSEPARATOR_COMMA);
    importer.setFieldDelimiter(GenericCSVImporter.DEFAULT_FIELDDELIMITER);
    List<GenericBankStatementLineBean> lines = importer.getBankStatementLines();

    assertTrue(lines.size() == 2);

    assertTrue(lines.get(0).getDramount().compareTo(getBigDecimal("1500")) == 0);
    assertTrue(lines.get(0).getCramount().compareTo(getBigDecimal("999150,65")) == 0);

    assertTrue(lines.get(1).getDramount().compareTo(getBigDecimal("9992222")) == 0);
    assertTrue(lines.get(1).getCramount().compareTo(getBigDecimal("0,005")) == 0);
  }

  public void test_dot() throws UnsupportedEncodingException, ParseException {
    final String s = "Transaction Date,Reference No.,Business Partner Name,Amount OUT,Amount IN,Description\n"
        + "20/10/2012,000001,Foo Bar,1500,150.65,\n"
        + "01/12/2012,\"25,123\",My Business Partner,2222,0.005,A description for this line";

    GenericCSVImporterImplementation importer = new GenericCSVImporterImplementation();
    importer.setIn(new ByteArrayInputStream(s.getBytes("UTF-8")));
    importer.setDateFormat(DATEFORMAT_1);
    importer.setDecimalSeparator(DECIMALSEPARATOR_DOT);
    importer.setFieldDelimiter(GenericCSVImporter.DEFAULT_FIELDDELIMITER);
    List<GenericBankStatementLineBean> lines = importer.getBankStatementLines();

    assertTrue(lines.size() == 2);

    assertTrue(lines.get(0).getDramount().compareTo(getBigDecimal("1500")) == 0);
    assertTrue(lines.get(0).getCramount().compareTo(getBigDecimal("150,65")) == 0);

    assertTrue(lines.get(1).getDramount().compareTo(getBigDecimal("2222")) == 0);
    assertTrue(lines.get(1).getCramount().compareTo(getBigDecimal("0,005")) == 0);
  }

  public void test_dot_complex() throws UnsupportedEncodingException, ParseException {
    final String s = "Transaction Date,Reference No.,Business Partner Name,Amount OUT,Amount IN,Description\n"
        + "20/10/2012,000001,Foo Bar,1500,\"999,150.65\",\n"
        + "01/12/2012,\"25,123\",My Business Partner,2222,\"9,990.005\",A description for this line";

    GenericCSVImporterImplementation importer = new GenericCSVImporterImplementation();
    importer.setIn(new ByteArrayInputStream(s.getBytes("UTF-8")));
    importer.setDateFormat(DATEFORMAT_1);
    importer.setDecimalSeparator(DECIMALSEPARATOR_DOT);
    importer.setFieldDelimiter(GenericCSVImporter.DEFAULT_FIELDDELIMITER);
    List<GenericBankStatementLineBean> lines = importer.getBankStatementLines();

    assertTrue(lines.size() == 2);

    assertTrue(lines.get(0).getDramount().compareTo(getBigDecimal("1500")) == 0);
    assertTrue(lines.get(0).getCramount().compareTo(getBigDecimal("999150,65")) == 0);

    assertTrue(lines.get(1).getDramount().compareTo(getBigDecimal("2222")) == 0);
    assertTrue(lines.get(1).getCramount().compareTo(getBigDecimal("9990,005")) == 0);
  }

  public void test_dateformat_1() throws UnsupportedEncodingException, ParseException {
    final String s = "Transaction Date,Reference No.,Business Partner Name,Amount OUT,Amount IN,Description\n"
        + "20/10/2012,000001,Foo Bar,1500,\"150,65\",\n"
        + "01/12/2012,\"25,123\",My Business Partner,2222,\"0,005\",A description for this line";

    GenericCSVImporterImplementation importer = new GenericCSVImporterImplementation();
    importer.setIn(new ByteArrayInputStream(s.getBytes("UTF-8")));
    importer.setDateFormat(DATEFORMAT_1);
    importer.setDecimalSeparator(DECIMALSEPARATOR_COMMA);
    importer.setFieldDelimiter(GenericCSVImporter.DEFAULT_FIELDDELIMITER);
    List<GenericBankStatementLineBean> lines = importer.getBankStatementLines();

    assertTrue(lines.size() == 2);
    assertTrue(lines.get(0).getTransactionDate().equals(getDate("20/10/2012")));
    assertTrue(lines.get(1).getTransactionDate().equals(getDate("01/12/2012")));
  }

  public void test_dateformat_2() throws UnsupportedEncodingException, ParseException {
    final String s = "Transaction Date,Reference No.,Business Partner Name,Amount OUT,Amount IN,Description\n"
        + "10/20/2012,000001,Foo Bar,1500,\"150,65\",\n"
        + "12/01/2012,\"25,123\",My Business Partner,2222,\"0,005\",A description for this line";

    GenericCSVImporterImplementation importer = new GenericCSVImporterImplementation();
    importer.setIn(new ByteArrayInputStream(s.getBytes("UTF-8")));
    importer.setDateFormat(DATEFORMAT_2);
    importer.setDecimalSeparator(DECIMALSEPARATOR_COMMA);
    importer.setFieldDelimiter(GenericCSVImporter.DEFAULT_FIELDDELIMITER);
    List<GenericBankStatementLineBean> lines = importer.getBankStatementLines();

    assertTrue(lines.size() == 2);

    assertTrue(lines.get(0).getTransactionDate().equals(getDate("20/10/2012")));
    assertTrue(lines.get(1).getTransactionDate().equals(getDate("01/12/2012")));
  }

  public void test_dateformat_3() throws UnsupportedEncodingException, ParseException {
    final String s = "Transaction Date,Reference No.,Business Partner Name,Amount OUT,Amount IN,Description\n"
        + "20121020,000001,Foo Bar,1500,\"150,65\",\n"
        + "20121201,\"25,123\",My Business Partner,2222,\"0,005\",A description for this line";

    GenericCSVImporterImplementation importer = new GenericCSVImporterImplementation();
    importer.setIn(new ByteArrayInputStream(s.getBytes("UTF-8")));
    importer.setDateFormat(DATEFORMAT_3);
    importer.setDecimalSeparator(DECIMALSEPARATOR_COMMA);
    importer.setFieldDelimiter(GenericCSVImporter.DEFAULT_FIELDDELIMITER);
    List<GenericBankStatementLineBean> lines = importer.getBankStatementLines();

    assertTrue(lines.size() == 2);

    assertTrue(lines.get(0).getTransactionDate().equals(getDate("20/10/2012")));
    assertTrue(lines.get(1).getTransactionDate().equals(getDate("01/12/2012")));
  }

  private BigDecimal getBigDecimal(final String numberString) throws ParseException {
    DecimalFormat dF = (DecimalFormat) DecimalFormat.getInstance(Locale.ITALY);
    dF.setParseBigDecimal(true);
    return (BigDecimal) dF.parse(numberString);
  }

  private Date getDate(final String dateString) throws ParseException {
    return new SimpleDateFormat(DATEFORMAT_1).parse(dateString);
  }
}
