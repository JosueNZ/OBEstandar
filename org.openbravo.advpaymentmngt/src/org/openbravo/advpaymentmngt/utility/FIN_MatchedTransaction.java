/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2010-2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.utility;

import java.util.List;

import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;

public class FIN_MatchedTransaction {
  private List<FIN_FinaccTransaction> transactions;

  public static final String STRONG = "AU";
  public static final String WEAK = "AP";
  public static final String NOMATCH = "NO";
  public static final String MANUALMATCH = "MA";
  private String level = NOMATCH;

  public FIN_MatchedTransaction(List<FIN_FinaccTransaction> _transactions, String _level) {
    transactions = _transactions;
    level = _level;
  }

  public FIN_FinaccTransaction getTransaction() {
    return transactions.get(0);
  }

  public List<FIN_FinaccTransaction> getTransactions() {
    return transactions;
  }

  public String getMatchLevel() {
    return level;
  }
}