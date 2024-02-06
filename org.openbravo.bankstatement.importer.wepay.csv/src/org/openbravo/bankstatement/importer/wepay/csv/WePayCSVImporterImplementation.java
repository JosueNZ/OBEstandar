/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */
package org.openbravo.bankstatement.importer.wepay.csv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.bankstatement.importer.generic.csv.GenericCSVImporter;
import org.openbravo.bankstatement.importer.generic.csv.util.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;

public class WePayCSVImporterImplementation extends GenericCSVImporter<WePayBankStatementLineBean> {

  protected static final String DESCRIPTION_SEPARATOR = "\n";
  protected static final String STATUS_COMPLETE = "Complete";
  protected static final String STATUS_CANCELLED = "Cancelled";

  public Long lineNo = 10l;

  @Override
  public List<FIN_BankStatementLine> generateFIN_BankStatementLine() {
    List<FIN_BankStatementLine> bankStatementLines = new ArrayList<FIN_BankStatementLine>();
    for (final WePayBankStatementLineBean gbslb : getBankStatementLines()) {
      // TODO Manage statuses. Currently we don't import cancelled lines
      if (!STATUS_CANCELLED.equals(gbslb.getStatus())) {
        lineNo = lineNo + 10l;
        gbslb.setDescription(generateDescription(gbslb));
        gbslb.setReferenceNo(lineNo.toString());
        bankStatementLines.add(Utility.createFIN_BankStatementLine(gbslb, getTargetBankStatement(),
            lineNo));
      }
    }

    return bankStatementLines;
  }

  @Override
  public Map<String, String> generateColumnMapping() {
    Map<String, String> map = new HashMap<String, String>();
    map.put("Date", WePayBankStatementLineBean.PROPERTY_TRANSACTIONDATESTRING);
    map.put("From/To", WePayBankStatementLineBean.PROPERTY_BPARTNERNAME);
    map.put("Email", WePayBankStatementLineBean.PROPERTY_EMAIL);
    map.put("Status", WePayBankStatementLineBean.PROPERTY_STATUS);
    map.put("Amount", WePayBankStatementLineBean.PROPERTY_AMOUNTSTRING);
    map.put("Purpose", WePayBankStatementLineBean.PROPERTY_DESCRIPTION);
    map.put("Memo", WePayBankStatementLineBean.PROPERTY_MEMO);
    return map;
  }

  @Override
  public Class<WePayBankStatementLineBean> configureBean() {
    return WePayBankStatementLineBean.class;
  }

  protected String generateDescription(WePayBankStatementLineBean gbslb) {
    final StringBuffer sb = new StringBuffer();
    sb.append(gbslb.getDescription());
    if (!StringUtils.isBlank(gbslb.getEmail())) {
      sb.append(DESCRIPTION_SEPARATOR);
      sb.append(gbslb.getEmail());
    }
    if (!StringUtils.isBlank(gbslb.getMemo())) {
      sb.append(DESCRIPTION_SEPARATOR);
      sb.append(gbslb.getMemo());
    }

    return sb.substring(0, sb.length() > 2000 ? 2000 : sb.length());
  }

}
