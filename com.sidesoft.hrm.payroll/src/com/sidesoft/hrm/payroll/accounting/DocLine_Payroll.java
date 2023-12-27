/*
 ******************************************************************************
 * The contents of this file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use this file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SLU
 * Contributions are Copyright (C) 2001-2010 Openbravo S.L.U.
 ******************************************************************************
 */
package com.sidesoft.hrm.payroll.accounting;

import org.apache.log4j.Logger;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.Account;
import org.openbravo.erpCommon.ad_forms.AcctSchema;
import org.openbravo.erpCommon.ad_forms.DocLine;
import org.openbravo.erpCommon.ad_forms.DocLine_Cash;

public class DocLine_Payroll extends DocLine {
  static Logger log4jDocLine_Cash = Logger.getLogger(DocLine_Cash.class);

  public DocLine_Payroll(String DocumentType, String TrxHeader_ID, String TrxLine_ID) {
    super(DocumentType, TrxHeader_ID, TrxLine_ID);
  }

  // References
  public String m_SSPR_Concept_ID = "";
  public String m_SSPR_Category_Acct_ID = "";
  public String m_IsBalance = "N";

  // Amounts
  public String m_Amount = ZERO.toString();

  public ConceptInfo c_conceptInfo = null;

  /**
   * Get Amount
   * 
   * @return Amount
   */
  public String getAmount() {
    return m_Amount;
  }

  /**
   * Set Amount
   * 
   * @param Amount
   *          amount
   */
  public void setAmount(String Amount) {
    if (!Amount.equals(""))
      m_Amount = Amount;
    super.setAmount(Amount);
  } // setAmount

  /**
   * Line Account from Business Concept.
   * 
   * @param AcctType
   *          see BusinessConcept.ACCTTYPE_* (0..3)
   * @param as
   *          Accounting schema
   * @param AcctCategory
   *          Accounting Category
   * @return Requested Business Concept Account
   */
  public Account getAccount(String AcctType, AcctSchema as, String AcctCategory,
      ConnectionProvider conn) {

    // Concept Account
    return c_conceptInfo.getAccount(AcctType, as, m_SSPR_Category_Acct_ID, conn);
  }

  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method
}
