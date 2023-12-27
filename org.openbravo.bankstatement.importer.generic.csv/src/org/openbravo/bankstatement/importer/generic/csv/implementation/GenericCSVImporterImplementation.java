/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */

package org.openbravo.bankstatement.importer.generic.csv.implementation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openbravo.bankstatement.importer.generic.csv.GenericCSVImporter;
import org.openbravo.bankstatement.importer.generic.csv.implementation.bean.GenericBankStatementLineBean;
import org.openbravo.bankstatement.importer.generic.csv.util.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;

public class GenericCSVImporterImplementation extends
    GenericCSVImporter<GenericBankStatementLineBean> {

  @Override
  public Map<String, String> generateColumnMapping() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("Transaction Date", GenericBankStatementLineBean.PROPERTY_TRANSACTIONDATESTRING);
    map.put("Reference No.", GenericBankStatementLineBean.PROPERTY_REFERENCENO);
    map.put("Business Partner Name", GenericBankStatementLineBean.PROPERTY_BPARTNERNAME);
    map.put("Amount OUT", GenericBankStatementLineBean.PROPERTY_DRAMOUNTSTRING);
    map.put("Amount IN", GenericBankStatementLineBean.PROPERTY_CRAMOUNTSTRING);
    map.put("Description", GenericBankStatementLineBean.PROPERTY_DESCRIPTION);
    return map;
  }

  @Override
  public Class<GenericBankStatementLineBean> configureBean() {
    return GenericBankStatementLineBean.class;
  }

  @Override
  public List<FIN_BankStatementLine> generateFIN_BankStatementLine() {
    List<FIN_BankStatementLine> bankStatementLines = new ArrayList<FIN_BankStatementLine>();
    for (final GenericBankStatementLineBean gbslb : getBankStatementLines()) {
      bankStatementLines.add(Utility.createFIN_BankStatementLine(gbslb, getTargetBankStatement(),
          getLineNo() + 10l));
    }

    return bankStatementLines;
  }

}
