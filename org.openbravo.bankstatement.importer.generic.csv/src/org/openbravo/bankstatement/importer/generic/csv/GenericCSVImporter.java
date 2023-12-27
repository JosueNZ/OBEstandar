/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */

package org.openbravo.bankstatement.importer.generic.csv;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openbravo.advpaymentmngt.utility.FIN_BankStatementImport;
import org.openbravo.bankstatement.importer.generic.csv.bean.GenericCsvImporterBean;
import org.openbravo.bankstatement.importer.generic.csv.util.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;

public abstract class GenericCSVImporter<T extends GenericCsvImporterBean> extends
    FIN_BankStatementImport {

  static final String DEFAULT_DATEFORMAT = "dd/MM/yyyy";
  static final String DEFAULT_DECIMALSEPARATOR = ",";
  public static final String DEFAULT_FIELDDELIMITER = ",";

  private GenericCSVImporterDao dao = new GenericCSVImporterDao();
  private Map<String, String> columnMapping = new HashMap<String, String>();
  private Long lineNo = 0l;
  private Class<T> bean;
  private InputStream in;
  private FIN_BankStatement targetBankStatement;
  private String dateFormat;
  private String decimalSeparator;
  private String fieldDelimiter;

  public GenericCSVImporter() {
    setColumnMapping(generateColumnMapping());
    setBean(configureBean());
  }

  public List<FIN_BankStatementLine> loadFile(InputStream inFile, FIN_BankStatement bankStatement) {
    /* Assign configuration parameters */
    this.in = inFile;
    this.targetBankStatement = bankStatement;
    final Map<String, String> configParams = dao.getConfigParameters(this.getClass().getName(),
        bankStatement.getOrganization());
    this.dateFormat = configParams.get("dateFormat");
    this.decimalSeparator = configParams.get("decimalSeparator");
    this.fieldDelimiter = configParams.get("fieldDelimiter");
    return generateFIN_BankStatementLine();
  }

  public List<T> getBankStatementLines() {
    return Utility.parseCSV(getBean(), getColumnMapping(), getIn(), getDateFormat(),
        getDecimalSeparator(), getFieldDelimiter());
  }

  protected InputStream getIn() {
    return in;
  }

  protected FIN_BankStatement getTargetBankStatement() {
    return targetBankStatement;
  }

  protected String getDateFormat() {
    return dateFormat;
  }

  protected String getDecimalSeparator() {
    return decimalSeparator;
  }

  protected String getFieldDelimiter() {
    return fieldDelimiter;
  }

  protected Map<String, String> getColumnMapping() {
    return columnMapping;
  }

  private void setColumnMapping(Map<String, String> columnMapping) {
    getColumnMapping().putAll(columnMapping);
  }

  protected Long getLineNo() {
    return lineNo;
  }

  protected Class<T> getBean() {
    return bean;
  }

  public void setIn(InputStream in) {
    this.in = in;
  }

  private void setBean(Class<T> bean) {
    this.bean = bean;
  }

  public void setDateFormat(String dateFormat) {
    this.dateFormat = dateFormat;
  }

  public void setDecimalSeparator(String decimalSeparator) {
    this.decimalSeparator = decimalSeparator;
  }

  public void setFieldDelimiter(String fieldDelimiter) {
    this.fieldDelimiter = fieldDelimiter;
  }

  /**
   * This method must contain the logic for generating the List of FIN_BankStatementLine that will
   * be automatically saved into the ERP
   * 
   * @return List<FIN_BankStatementLine> to be saved into the ERP
   */
  public abstract List<FIN_BankStatementLine> generateFIN_BankStatementLine();

  /**
   * Allows to configure the mapping between the CSV columns in the header and the Java Bean used to
   * import the data
   * 
   * @return Map<String, String> where the keys are the CSV columns and the values are the names of
   *         the String java bean attributes in charge of storing this data
   */
  public abstract Map<String, String> generateColumnMapping();

  /**
   * Sets the Java Bean class in charge of storing the data gotten from the CSV file
   * 
   * @return
   */
  public abstract Class<T> configureBean();

}
