/*
 ******************************************************************************
 * The contents of docInvoice file are subject to the   Compiere License  Version 1.1
 * ("License"); You may not use docInvoice file except in compliance with the License
 * You may obtain a copy of the License at http://www.compiere.org/license.html
 * Software distributed under the License is distributed on an  "AS IS"  basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 * The Original Code is                  Compiere  ERP & CRM  Business Solution
 * The Initial Developer of the Original Code is Jorg Janke  and ComPiere, Inc.
 * Portions created by Jorg Janke are Copyright (C) 1999-2001 Jorg Janke, parts
 * created by ComPiere are Copyright (C) ComPiere, Inc.;   All Rights Reserved.
 * Contributor(s): Openbravo SLU
 * Contributions are Copyright (C) 2001-2018 Openbravo S.L.U.
 ******************************************************************************
 */
package ec.com.sidesoft.account.purchase.accounting;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.Account;
import org.openbravo.erpCommon.ad_forms.AcctSchema;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.ad_forms.DocInvoiceTemplate;
import org.openbravo.erpCommon.ad_forms.DocLine;
import org.openbravo.erpCommon.ad_forms.DocLine_FinPaymentSchedule;
import org.openbravo.erpCommon.ad_forms.DocLine_Invoice;
import org.openbravo.erpCommon.ad_forms.DocLine_Payment;
import org.openbravo.erpCommon.ad_forms.DocTax;
import org.openbravo.erpCommon.ad_forms.Fact;
import org.openbravo.erpCommon.ad_forms.FactLine;
import org.openbravo.erpCommon.ad_forms.ProductInfo;
import org.openbravo.erpCommon.utility.AccDefUtility;
import org.openbravo.erpCommon.utility.CashVATUtil;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.invoice.InvoiceTax;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.calendar.Period;

public class DocInvoice extends DocInvoiceTemplate {
  private static final long serialVersionUID = 1L;
  static Logger log4jDocInvoice = Logger.getLogger(DocInvoice.class);

  ConnectionProvider connectionProvider;
  String Record_ID;

  private boolean isReversedInvoice() {
    try {
      DocInvoiceData[] revInv = DocInvoiceData.getIsReversedInvoice(connectionProvider, Record_ID);
      return (revInv != null && revInv.length != 0);
    } catch (ServletException e) {
      log4jDocInvoice.warn(e);
    }
    return false;
  }

  public Fact createFact(org.openbravo.erpCommon.ad_forms.DocInvoice docInvoice, AcctSchema as,
      ConnectionProvider conn, Connection con, VariablesSecureApp vars) throws ServletException {
    log4jDocInvoice.debug("Starting create fact");

    connectionProvider = conn;
    Record_ID = docInvoice.Record_ID;

    // create Fact Header
    Fact fact = new Fact(docInvoice, as, Fact.POST_Actual);
    String Fact_Acct_Group_ID = SequenceIdData.getUUID();
    // Cash based accounting
    if (!as.isAccrual())
      return null;

    /** @todo Assumes TaxIncluded = N */

    // ARI, ARF, ARI_RM
    if (docInvoice.DocumentType.equals(AcctServer.DOCTYPE_ARInvoice)
        || docInvoice.DocumentType.equals(AcctServer.DOCTYPE_ARProForma)
        || docInvoice.DocumentType.equals(AcctServer.DOCTYPE_RMSalesInvoice)) {
      log4jDocInvoice.debug("Point 1");
      // Receivables DR
      if (docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
        for (int i = 0; docInvoice.m_debt_payments != null
            && i < docInvoice.m_debt_payments.length; i++) {
          if (docInvoice.m_debt_payments[i].getIsReceipt().equals("Y"))
            fact.createLine(docInvoice.m_debt_payments[i],
                docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true,
                    docInvoice.m_debt_payments[i].getDpStatus(), conn),
                docInvoice.C_Currency_ID,
                docInvoice.getConvertedAmt(docInvoice.m_debt_payments[i].getAmount(),
                    docInvoice.m_debt_payments[i].getC_Currency_ID_From(), docInvoice.C_Currency_ID,
                    docInvoice.DateAcct, "", docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
                "", Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                docInvoice.DocumentType, conn);
          else
            fact.createLine(docInvoice.m_debt_payments[i],
                docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
                    docInvoice.m_debt_payments[i].getDpStatus(), conn),
                docInvoice.C_Currency_ID, "",
                docInvoice.getConvertedAmt(docInvoice.m_debt_payments[i].getAmount(),
                    docInvoice.m_debt_payments[i].getC_Currency_ID_From(), docInvoice.C_Currency_ID,
                    docInvoice.DateAcct, "", docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
                Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                docInvoice.DocumentType, conn);
        }
      else
        for (int i = 0; docInvoice.getM_payments() != null
            && i < docInvoice.getM_payments().length; i++) {
          fact.createLine(docInvoice.getM_payments()[i],
              docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false, conn),
              docInvoice.C_Currency_ID, docInvoice.getM_payments()[i].getAmount(), "",
              Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
          if (docInvoice.getM_payments()[i].getC_Currency_ID_From().equals(as.m_C_Currency_ID)
              && new BigDecimal(docInvoice.getM_payments()[i].getPrepaidAmount())
                  .compareTo(BigDecimal.ZERO) != 0) {
            fact.createLine(docInvoice.getM_payments()[i],
                docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, true, conn),
                docInvoice.C_Currency_ID, docInvoice.getM_payments()[i].getPrepaidAmount(), "",
                Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                docInvoice.DocumentType, conn);
          } else if (!docInvoice.getM_payments()[i].getC_Currency_ID_From()
              .equals(as.m_C_Currency_ID)) {
            try {
              DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(conn,
                  docInvoice.getM_payments()[i].getLine_ID(), docInvoice.Record_ID);
              for (int j = 0; j < prepayments.length; j++) {
                BigDecimal prePaymentAmt = docInvoice.convertAmount(
                    new BigDecimal(prepayments[j].prepaidamt), true, docInvoice.DateAcct,
                    docInvoice.TABLEID_Payment, prepayments[j].finPaymentId,
                    docInvoice.getM_payments()[i].getC_Currency_ID_From(), as.m_C_Currency_ID,
                    docInvoice.getM_payments()[i], as, fact, Fact_Acct_Group_ID,
                    docInvoice.nextSeqNo(docInvoice.getSeqNo()), conn);
                fact.createLine(docInvoice.getM_payments()[i],
                    docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, true, conn),
                    docInvoice.getM_payments()[i].getC_Currency_ID_From(), prePaymentAmt.toString(),
                    "", Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                    docInvoice.DocumentType, conn);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
          && (docInvoice.m_debt_payments == null || docInvoice.m_debt_payments.length == 0)) {
        BigDecimal grossamt = new BigDecimal(docInvoice.Amounts[docInvoice.AMTTYPE_Gross]);
        BigDecimal prepayment = new BigDecimal(docInvoice.getPrepaymentamt());
        BigDecimal difference = grossamt.subtract(prepayment);
        if (!docInvoice.getPrepaymentamt().equals("0")) {
          if (grossamt.abs().compareTo(prepayment.abs()) != 0) {
            if (docInvoice.IsReturn.equals("Y") || isReversedInvoice()) {
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, true, conn),
                  docInvoice.C_Currency_ID, "", docInvoice.getPrepaymentamt(), Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false, conn),
                  docInvoice.C_Currency_ID, "", difference.toString(), Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            } else {
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, true, conn),
                  docInvoice.C_Currency_ID, docInvoice.getPrepaymentamt(), "", Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false, conn),
                  docInvoice.C_Currency_ID, difference.toString(), "", Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            }
          } else {
            if (docInvoice.IsReturn.equals("Y") || isReversedInvoice()) {
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, true, conn),
                  docInvoice.C_Currency_ID, "", docInvoice.getPrepaymentamt(), Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            } else {
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, true, conn),
                  docInvoice.C_Currency_ID, docInvoice.getPrepaymentamt(), "", Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            }
          }
        } else {
          fact.createLine(null,
              docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false, conn),
              docInvoice.C_Currency_ID, docInvoice.Amounts[docInvoice.AMTTYPE_Gross], "",
              Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
        }

      }
      // Charge CR
      log4jDocInvoice.debug("The first create line");
      fact.createLine(null, docInvoice.getAccount(AcctServer.ACCTTYPE_Charge, as, conn),
          docInvoice.C_Currency_ID, "", docInvoice.getAmount(AcctServer.AMTTYPE_Charge),
          Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType,
          conn);
      // TaxDue CR
      log4jDocInvoice.debug("m_taxes.length: " + docInvoice.getM_taxes());
      BigDecimal grossamt = new BigDecimal(docInvoice.Amounts[docInvoice.AMTTYPE_Gross]);
      BigDecimal prepayment = new BigDecimal(docInvoice.getPrepaymentamt());

      for (int i = 0; docInvoice.getM_taxes() != null && i < docInvoice.getM_taxes().length; i++) {
        // New docLine created to assign C_Tax_ID value to the entry
        DocLine docLine = new DocLine(docInvoice.DocumentType, docInvoice.Record_ID, "");
        docLine.m_C_Tax_ID = docInvoice.getM_taxes()[i].m_C_Tax_ID;

        BigDecimal percentageFinalAccount = CashVATUtil._100;
        final BigDecimal taxesAmountTotal = new BigDecimal(
            StringUtils.isBlank(docInvoice.getM_taxes()[i].m_amount) ? "0"
                : docInvoice.getM_taxes()[i].m_amount);
        BigDecimal taxToTransAccount = BigDecimal.ZERO;
        int precission = 0;
        OBContext.setAdminMode(true);
        try {
          Currency currency = OBDal.getInstance().get(Currency.class, docInvoice.C_Currency_ID);
          precission = currency.getStandardPrecision().intValue();
        } finally {
          OBContext.restorePreviousMode();
        }
        if (docInvoice.IsReversal.equals("Y")) {
          if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
            if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
                && (docInvoice.m_debt_payments == null || docInvoice.m_debt_payments.length == 0)
                && (!docInvoice.getPrepaymentamt().equals("0"))) {
              percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
                  .divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
              taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                  CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                  docInvoice.C_Currency_ID);
            } else {
              percentageFinalAccount = CashVATUtil.calculatePrepaidPercentageForCashVATTax(
                  docInvoice.getM_taxes()[i].m_C_Tax_ID, docInvoice.Record_ID);
              taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                  CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                  docInvoice.C_Currency_ID);
            }
            fact.createLine(docLine,
                docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue_Trans, as, conn),
                docInvoice.C_Currency_ID, taxToTransAccount.toString(), "", Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          }
          final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
          fact.createLine(docLine,
              docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue, as, conn),
              docInvoice.C_Currency_ID, taxToFinalAccount.toString(), "", Fact_Acct_Group_ID,
              docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
        } else {
          if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
            if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
                && (docInvoice.m_debt_payments == null || docInvoice.m_debt_payments.length == 0)
                && (!docInvoice.getPrepaymentamt().equals("0"))) {
              percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
                  .divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
              taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                  CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                  docInvoice.C_Currency_ID);
            } else {
              percentageFinalAccount = CashVATUtil.calculatePrepaidPercentageForCashVATTax(
                  docInvoice.getM_taxes()[i].m_C_Tax_ID, docInvoice.Record_ID);
              taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                  CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                  docInvoice.C_Currency_ID);
            }
            fact.createLine(docLine,
                docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue_Trans, as, conn),
                docInvoice.C_Currency_ID, "", taxToTransAccount.toString(), Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          }
          final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
          fact.createLine(docLine,
              docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue, as, conn),
              docInvoice.C_Currency_ID, "", taxToFinalAccount.toString(), Fact_Acct_Group_ID,
              docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
        }
      }
      // Revenue CR
      if (docInvoice.p_lines != null && docInvoice.p_lines.length > 0) {
        for (int i = 0; i < docInvoice.p_lines.length; i++) {
          Account account = ((DocLine_Invoice) docInvoice.p_lines[i])
              .getAccount(ProductInfo.ACCTTYPE_P_Revenue, as, conn);
          if (docInvoice.DocumentType.equals(AcctServer.DOCTYPE_RMSalesInvoice)) {
            Account accountReturnMaterial = ((DocLine_Invoice) docInvoice.p_lines[i])
                .getAccount(ProductInfo.ACCTTYPE_P_RevenueReturn, as, conn);
            if (accountReturnMaterial != null) {
              account = accountReturnMaterial;
            }
          }
          String amount = docInvoice.p_lines[i].getAmount();
          String amountConverted = "";
          ConversionRateDoc conversionRateCurrentDoc = docInvoice.getConversionRateDoc(
              docInvoice.TABLEID_Invoice, docInvoice.Record_ID, docInvoice.C_Currency_ID,
              as.m_C_Currency_ID);
          if (conversionRateCurrentDoc != null) {
            amountConverted = docInvoice
                .applyRate(new BigDecimal(docInvoice.p_lines[i].getAmount()),
                    conversionRateCurrentDoc, true)
                .setScale(2, RoundingMode.HALF_UP).toString();
          } else {
            amountConverted = docInvoice.getConvertedAmt(docInvoice.p_lines[i].getAmount(),
                docInvoice.C_Currency_ID, as.m_C_Currency_ID, docInvoice.DateAcct, "",
                docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn);
          }
          if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()) {
            amount = docInvoice.createAccDefRevenueFact(fact,
                (DocLine_Invoice) docInvoice.p_lines[i], account,
                ((DocLine_Invoice) docInvoice.p_lines[i])
                    .getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
                amountConverted, as.m_C_Currency_ID, conn);
            if (docInvoice.IsReversal.equals("Y")) {
              fact.createLine(docInvoice.p_lines[i], account, as.m_C_Currency_ID, amount, "",
                  Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                  docInvoice.DocumentType, conn);
            } else {
              fact.createLine(docInvoice.p_lines[i], account, as.m_C_Currency_ID, "", amount,
                  Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                  docInvoice.DocumentType, conn);
            }
          } else {
            if (docInvoice.IsReversal.equals("Y")) {
              fact.createLine(docInvoice.p_lines[i], account, docInvoice.C_Currency_ID, amount, "",
                  Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                  docInvoice.DocumentType, conn);
            } else {
              fact.createLine(docInvoice.p_lines[i], account, docInvoice.C_Currency_ID, "", amount,
                  Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                  docInvoice.DocumentType, conn);
            }
          }
          // If revenue has been deferred
          if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()
              && !amount.equals(amountConverted)) {
            amount = new BigDecimal(amountConverted).subtract(new BigDecimal(amount)).toString();
            if (docInvoice.IsReversal.equals("Y")) {
              fact.createLine(docInvoice.p_lines[i],
                  ((DocLine_Invoice) docInvoice.p_lines[i])
                      .getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
                  as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            } else {
              fact.createLine(docInvoice.p_lines[i],
                  ((DocLine_Invoice) docInvoice.p_lines[i])
                      .getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
                  as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            }
          }
        }
      }
      // Set Locations
      FactLine[] fLines = fact.getLines();
      for (int i = 0; i < fLines.length; i++) {
        if (fLines[i] != null) {
          fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), true, conn); // from Loc
          fLines[i].setLocationFromBPartner(docInvoice.C_BPartner_Location_ID, false, conn); // to
                                                                                             // Loc
        }
      }
    }
    // ARC
    else if (docInvoice.DocumentType.equals(AcctServer.DOCTYPE_ARCredit)) {
      log4jDocInvoice.debug("Point 2");
      // Receivables CR
      if (docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
        for (int i = 0; docInvoice.m_debt_payments != null
            && i < docInvoice.m_debt_payments.length; i++) {
          BigDecimal amount = new BigDecimal(docInvoice.m_debt_payments[i].getAmount());
          // BigDecimal ZERO = BigDecimal.ZERO;
          fact.createLine(
              docInvoice.m_debt_payments[i], docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID,
                  as, true, docInvoice.m_debt_payments[i].getDpStatus(), conn),
              docInvoice.C_Currency_ID, "",
              docInvoice.getConvertedAmt(((amount.negate())).toPlainString(),
                  docInvoice.m_debt_payments[i].getC_Currency_ID_From(), docInvoice.C_Currency_ID,
                  docInvoice.DateAcct, "", docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
              Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
        }
      else
        for (int i = 0; docInvoice.getM_payments() != null
            && i < docInvoice.getM_payments().length; i++) {
          BigDecimal amount = new BigDecimal(docInvoice.getM_payments()[i].getAmount());
          BigDecimal prepaidAmount = new BigDecimal(
              docInvoice.getM_payments()[i].getPrepaidAmount());
          fact.createLine(docInvoice.getM_payments()[i],
              docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false, conn),
              docInvoice.C_Currency_ID, "", amount.negate().toString(), Fact_Acct_Group_ID,
              docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          // Pre-payment: Probably not needed as at docInvoice point we can not generate
          // pre-payments
          // against ARC. Amount is negated
          if (docInvoice.getM_payments()[i].getC_Currency_ID_From().equals(as.m_C_Currency_ID)
              && prepaidAmount.compareTo(BigDecimal.ZERO) != 0) {
            fact.createLine(docInvoice.getM_payments()[i],
                docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, true, conn),
                docInvoice.C_Currency_ID, "", prepaidAmount.negate().toString(), Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          } else if (!docInvoice.getM_payments()[i].getC_Currency_ID_From()
              .equals(as.m_C_Currency_ID)) {
            try {
              DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(conn,
                  docInvoice.getM_payments()[i].getLine_ID(), docInvoice.Record_ID);
              for (int j = 0; j < prepayments.length; j++) {
                BigDecimal prePaymentAmt = docInvoice.convertAmount(
                    new BigDecimal(prepayments[j].prepaidamt).negate(), true, docInvoice.DateAcct,
                    docInvoice.TABLEID_Payment, prepayments[j].finPaymentId,
                    docInvoice.getM_payments()[i].getC_Currency_ID_From(), as.m_C_Currency_ID,
                    docInvoice.getM_payments()[i], as, fact, Fact_Acct_Group_ID,
                    docInvoice.nextSeqNo(docInvoice.getSeqNo()), conn);
                fact.createLine(docInvoice.getM_payments()[i],
                    docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, true, conn),
                    docInvoice.getM_payments()[i].getC_Currency_ID_From(), "",
                    prePaymentAmt.toString(), Fact_Acct_Group_ID,
                    docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
          && (docInvoice.m_debt_payments == null || docInvoice.m_debt_payments.length == 0)) {
        fact.createLine(null,
            docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false, conn),
            docInvoice.C_Currency_ID, "", docInvoice.Amounts[docInvoice.AMTTYPE_Gross],
            Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
            docInvoice.DocumentType, conn);
      }
      // Charge DR
      fact.createLine(null, docInvoice.getAccount(AcctServer.ACCTTYPE_Charge, as, conn),
          docInvoice.C_Currency_ID, docInvoice.getAmount(AcctServer.AMTTYPE_Charge), "",
          Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType,
          conn);
      // TaxDue DR
      for (int i = 0; docInvoice.getM_taxes() != null && i < docInvoice.getM_taxes().length; i++) {
        // New docLine created to assign C_Tax_ID value to the entry
        DocLine docLine = new DocLine(docInvoice.DocumentType, docInvoice.Record_ID, "");
        docLine.m_C_Tax_ID = docInvoice.getM_taxes()[i].m_C_Tax_ID;

        BigDecimal percentageFinalAccount = CashVATUtil._100;
        final BigDecimal taxesAmountTotal = new BigDecimal(
            StringUtils.isBlank(docInvoice.getM_taxes()[i].getAmount()) ? "0"
                : docInvoice.getM_taxes()[i].getAmount());
        BigDecimal taxToTransAccount = BigDecimal.ZERO;
        if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
          percentageFinalAccount = CashVATUtil.calculatePrepaidPercentageForCashVATTax(
              docInvoice.getM_taxes()[i].m_C_Tax_ID, docInvoice.Record_ID);
          taxToTransAccount = CashVATUtil.calculatePercentageAmount(
              CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
              docInvoice.C_Currency_ID);
          fact.createLine(docLine,
              docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue_Trans, as, conn),
              docInvoice.C_Currency_ID, taxToTransAccount.toString(), "", Fact_Acct_Group_ID,
              docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
        }
        final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
        fact.createLine(docLine,
            docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue, as, conn),
            docInvoice.C_Currency_ID, taxToFinalAccount.toString(), "", Fact_Acct_Group_ID,
            docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
      }
      // Revenue CR
      for (int i = 0; docInvoice.p_lines != null && i < docInvoice.p_lines.length; i++) {
        String amount = docInvoice.p_lines[i].getAmount();
        String amountCoverted = "";
        ConversionRateDoc conversionRateCurrentDoc = docInvoice.getConversionRateDoc(
            docInvoice.TABLEID_Invoice, docInvoice.Record_ID, docInvoice.C_Currency_ID,
            as.m_C_Currency_ID);
        if (conversionRateCurrentDoc != null) {
          amountCoverted = docInvoice.applyRate(new BigDecimal(docInvoice.p_lines[i].getAmount()),
              conversionRateCurrentDoc, true).setScale(2, RoundingMode.HALF_UP).toString();
        } else {
          amountCoverted = docInvoice.getConvertedAmt(docInvoice.p_lines[i].getAmount(),
              docInvoice.C_Currency_ID, as.m_C_Currency_ID, docInvoice.DateAcct, "",
              docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn);
        }
        Account account = ((DocLine_Invoice) docInvoice.p_lines[i])
            .getAccount(ProductInfo.ACCTTYPE_P_Revenue, as, conn);
        if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()) {
          amount = docInvoice.createAccDefRevenueFact(fact, (DocLine_Invoice) docInvoice.p_lines[i],
              account,
              ((DocLine_Invoice) docInvoice.p_lines[i])
                  .getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
              amountCoverted, as.m_C_Currency_ID, conn);
          fact.createLine(docInvoice.p_lines[i], account, as.m_C_Currency_ID, amount, "",
              Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
        } else {
          fact.createLine(docInvoice.p_lines[i], account, docInvoice.C_Currency_ID, amount, "",
              Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
        }
        // If revenue has been deferred
        if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()
            && !amount.equals(amountCoverted)) {
          amount = new BigDecimal(amountCoverted).subtract(new BigDecimal(amount)).toString();
          fact.createLine(docInvoice.p_lines[i],
              ((DocLine_Invoice) docInvoice.p_lines[i])
                  .getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
              as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID,
              docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
        }
      }
      // Set Locations
      FactLine[] fLines = fact.getLines();
      for (int i = 0; fLines != null && i < fLines.length; i++) {
        if (fLines[i] != null) {
          fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), true, conn); // from Loc
          fLines[i].setLocationFromBPartner(docInvoice.C_BPartner_Location_ID, false, conn); // to
                                                                                             // Loc
        }
      }
    }
    // API
    else if (docInvoice.DocumentType.equals(AcctServer.DOCTYPE_APInvoice)) {
      log4jDocInvoice.debug("Point 3");
      // Liability CR
      if (docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
        for (int i = 0; docInvoice.m_debt_payments != null
            && i < docInvoice.m_debt_payments.length; i++) {
          if (docInvoice.m_debt_payments[i].getIsReceipt().equals("Y"))
            fact.createLine(docInvoice.m_debt_payments[i],
                docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true,
                    docInvoice.m_debt_payments[i].getDpStatus(), conn),
                docInvoice.C_Currency_ID,
                docInvoice.getConvertedAmt(docInvoice.m_debt_payments[i].getAmount(),
                    docInvoice.m_debt_payments[i].getC_Currency_ID_From(), docInvoice.C_Currency_ID,
                    docInvoice.DateAcct, "", docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
                "", Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                docInvoice.DocumentType, conn);
          else
            fact.createLine(docInvoice.m_debt_payments[i],
                docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
                    docInvoice.m_debt_payments[i].getDpStatus(), conn),
                docInvoice.C_Currency_ID, "",
                docInvoice.getConvertedAmt(docInvoice.m_debt_payments[i].getAmount(),
                    docInvoice.m_debt_payments[i].getC_Currency_ID_From(), docInvoice.C_Currency_ID,
                    docInvoice.DateAcct, "", docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
                Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                docInvoice.DocumentType, conn);
        }
      else
        for (int i = 0; docInvoice.getM_payments() != null
            && i < docInvoice.getM_payments().length; i++) {
          fact.createLine(docInvoice.getM_payments()[i],
              docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, false, conn),
              docInvoice.C_Currency_ID, "", docInvoice.getM_payments()[i].getAmount(),
              Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
          if (docInvoice.getM_payments()[i].getC_Currency_ID_From().equals(as.m_C_Currency_ID)
              && new BigDecimal(docInvoice.getM_payments()[i].getPrepaidAmount())
                  .compareTo(BigDecimal.ZERO) != 0) {
            fact.createLine(docInvoice.getM_payments()[i],
                docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, true, conn),
                docInvoice.C_Currency_ID, "", docInvoice.getM_payments()[i].getPrepaidAmount(),
                Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                docInvoice.DocumentType, conn);
          } else if (!docInvoice.getM_payments()[i].getC_Currency_ID_From()
              .equals(as.m_C_Currency_ID)) {
            try {
              DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(conn,
                  docInvoice.getM_payments()[i].getLine_ID(), docInvoice.Record_ID);
              for (int j = 0; j < prepayments.length; j++) {
                BigDecimal prePaymentAmt = docInvoice.convertAmount(
                    new BigDecimal(prepayments[j].prepaidamt), false, docInvoice.DateAcct,
                    docInvoice.TABLEID_Payment, prepayments[j].finPaymentId,
                    docInvoice.getM_payments()[i].getC_Currency_ID_From(), as.m_C_Currency_ID,
                    docInvoice.getM_payments()[i], as, fact, Fact_Acct_Group_ID,
                    docInvoice.nextSeqNo(docInvoice.getSeqNo()), conn);
                fact.createLine(docInvoice.getM_payments()[i],
                    docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, true, conn),
                    docInvoice.getM_payments()[i].getC_Currency_ID_From(), "",
                    prePaymentAmt.toString(), Fact_Acct_Group_ID,
                    docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
          && (docInvoice.m_debt_payments == null || docInvoice.m_debt_payments.length == 0)) {
        BigDecimal grossamt = new BigDecimal(docInvoice.Amounts[docInvoice.AMTTYPE_Gross]);
        BigDecimal prepayment = new BigDecimal(docInvoice.getPrepaymentamt());
        BigDecimal difference = grossamt.subtract(prepayment);
        if (!docInvoice.getPrepaymentamt().equals("0")) {
          if (grossamt.abs().compareTo(prepayment.abs()) != 0) {
            if (docInvoice.IsReturn.equals("Y") || isReversedInvoice()) {
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, true, conn),
                  docInvoice.C_Currency_ID, docInvoice.getPrepaymentamt(), "", Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, false, conn),
                  docInvoice.C_Currency_ID, difference.toString(), "", Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            } else {
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, true, conn),
                  docInvoice.C_Currency_ID, "", docInvoice.getPrepaymentamt(), Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, false, conn),
                  docInvoice.C_Currency_ID, "", difference.toString(), Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            }
          } else {
            if (docInvoice.IsReturn.equals("Y") || isReversedInvoice()) {
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, true, conn),
                  docInvoice.C_Currency_ID, docInvoice.getPrepaymentamt(), "", Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            } else {
              fact.createLine(null,
                  docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, true, conn),
                  docInvoice.C_Currency_ID, "", docInvoice.getPrepaymentamt(), Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            }
          }
        } else {
          fact.createLine(null,
              docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, false, conn),
              docInvoice.C_Currency_ID, "", docInvoice.Amounts[docInvoice.AMTTYPE_Gross],
              Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
        }

      }
      // Charge DR
      fact.createLine(null, docInvoice.getAccount(AcctServer.ACCTTYPE_Charge, as, conn),
          docInvoice.C_Currency_ID, docInvoice.getAmount(AcctServer.AMTTYPE_Charge), "",
          Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType,
          conn);
      BigDecimal grossamt = new BigDecimal(docInvoice.Amounts[docInvoice.AMTTYPE_Gross]);
      BigDecimal prepayment = new BigDecimal(docInvoice.getPrepaymentamt());
      // TaxCredit DR
      for (int i = 0; docInvoice.getM_taxes() != null && i < docInvoice.getM_taxes().length; i++) {
        // New docLine created to assign C_Tax_ID value to the entry
        DocLine docLine = new DocLine(docInvoice.DocumentType, docInvoice.Record_ID, "");
        docLine.m_C_Tax_ID = docInvoice.getM_taxes()[i].m_C_Tax_ID;
        OBContext.setAdminMode(true);
        int precission = 0;
        try {
          Currency currency = OBDal.getInstance().get(Currency.class, docInvoice.C_Currency_ID);
          precission = currency.getStandardPrecision().intValue();
        } finally {
          OBContext.restorePreviousMode();
        }
        if (!docInvoice.getM_taxes()[i].m_isTaxUndeductable) {
          BigDecimal percentageFinalAccount = CashVATUtil._100;
          final BigDecimal taxesAmountTotal = new BigDecimal(
              StringUtils.isBlank(docInvoice.getM_taxes()[i].getAmount()) ? "0"
                  : docInvoice.getM_taxes()[i].getAmount());
          BigDecimal taxToTransAccount = BigDecimal.ZERO;
          if (docInvoice.IsReversal.equals("Y")) {
            if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
              if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
                  && (docInvoice.m_debt_payments == null || docInvoice.m_debt_payments.length == 0)
                  && (!docInvoice.getPrepaymentamt().equals("0"))) {
                percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
                    .divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
                taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                    CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                    docInvoice.C_Currency_ID);
              } else {
                percentageFinalAccount = CashVATUtil.calculatePrepaidPercentageForCashVATTax(
                    docInvoice.getM_taxes()[i].m_C_Tax_ID, docInvoice.Record_ID);
                taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                    CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                    docInvoice.C_Currency_ID);
              }
              fact.createLine(docLine,
                  docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxCredit_Trans, as, conn),
                  docInvoice.C_Currency_ID, "", taxToTransAccount.toString(), Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            }
            final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
            fact.createLine(docLine,
                docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxCredit, as, conn),
                docInvoice.C_Currency_ID, "", taxToFinalAccount.toString(), Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          } else {
            if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
              if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
                  && (docInvoice.m_debt_payments == null || docInvoice.m_debt_payments.length == 0)
                  && (!docInvoice.getPrepaymentamt().equals("0"))) {
                percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
                    .divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
                taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                    CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                    docInvoice.C_Currency_ID);
              } else {
                percentageFinalAccount = CashVATUtil.calculatePrepaidPercentageForCashVATTax(
                    docInvoice.getM_taxes()[i].m_C_Tax_ID, docInvoice.Record_ID);
                taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                    CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                    docInvoice.C_Currency_ID);
              }
              fact.createLine(docLine,
                  docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxCredit_Trans, as, conn),
                  docInvoice.C_Currency_ID, taxToTransAccount.toString(), "", Fact_Acct_Group_ID,
                  docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
            }
            final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
            fact.createLine(docLine,
                docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxCredit, as, conn),
                docInvoice.C_Currency_ID, taxToFinalAccount.toString(), "", Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          }

        } else {
          DocLineInvoiceData[] data = null;
          try {
            data = DocLineInvoiceData.selectUndeductable(conn, docInvoice.Record_ID,
                docInvoice.getM_taxes()[i].m_C_Tax_ID);
          } catch (ServletException e) {
            log4jDocInvoice.warn(e);
          }

          BigDecimal cumulativeTaxLineAmount = new BigDecimal(0);
          BigDecimal taxAmount = new BigDecimal(0);
          for (int j = 0; data != null && j < data.length; j++) {
            DocLine docLine1 = new DocLine(docInvoice.DocumentType, docInvoice.Record_ID, "");
            docLine1.m_C_Tax_ID = data[j].cTaxId;
            docLine1.m_TrxLine_ID = data[j].cInvoicelineId;
            docLine1.m_C_BPartner_ID = data[j].cBpartnerId;
            docLine1.m_M_Product_ID = data[j].mProductId;
            docLine1.m_C_Costcenter_ID = data[j].cCostcenterId;
            docLine1.m_C_Project_ID = data[j].cProjectId;
            docLine1.m_User1_ID = data[j].user1id;
            docLine1.m_User2_ID = data[j].user2id;
            docLine1.m_C_Activity_ID = data[j].cActivityId;
            docLine1.m_C_Campaign_ID = data[j].cCampaignId;
            docLine1.m_A_Asset_ID = data[j].aAssetId;
            String strtaxAmount = null;

            try {
              DocInvoiceData[] dataEx = DocInvoiceData.selectProductAcct(conn,
                  as.getC_AcctSchema_ID(), docInvoice.getM_taxes()[i].m_C_Tax_ID,
                  docInvoice.Record_ID, docLine1.m_M_Product_ID);
              if (dataEx.length == 0) {
                dataEx = DocInvoiceData.selectGLItemAcctForTaxLine(conn, as.getC_AcctSchema_ID(),
                    docInvoice.getM_taxes()[i].m_C_Tax_ID, docInvoice.Record_ID);
              }
              strtaxAmount = docInvoice.getM_taxes()[i].getAmount();
              taxAmount = new BigDecimal(strtaxAmount.equals("") ? "0.00" : strtaxAmount);
              if (j == data.length - 1) {
                data[j].taxamt = taxAmount.subtract(cumulativeTaxLineAmount).toPlainString();
              }
              try {

                if (docInvoice.DocumentType.equals(AcctServer.DOCTYPE_APInvoice)) {
                  if (docInvoice.IsReversal.equals("Y")) {
                    fact.createLine(docLine1, Account.getAccount(conn, dataEx[0].pExpenseAcct),
                        docInvoice.C_Currency_ID, "", data[j].taxamt, Fact_Acct_Group_ID,
                        docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);

                  } else {
                    fact.createLine(docLine1, Account.getAccount(conn, dataEx[0].pExpenseAcct),
                        docInvoice.C_Currency_ID, data[j].taxamt, "", Fact_Acct_Group_ID,
                        docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
                  }
                } else if (docInvoice.DocumentType.equals(AcctServer.DOCTYPE_APCredit)) {
                  fact.createLine(docLine1, Account.getAccount(conn, dataEx[0].pExpenseAcct),
                      docInvoice.C_Currency_ID, "", data[j].taxamt, Fact_Acct_Group_ID,
                      docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
                }
                cumulativeTaxLineAmount = cumulativeTaxLineAmount
                    .add(new BigDecimal(data[j].taxamt));
              } catch (ServletException e) {
                log4jDocInvoice.error("Exception in createLineForTaxUndeductable method: " + e);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      }
      // Expense DR
      for (int i = 0; docInvoice.p_lines != null && i < docInvoice.p_lines.length; i++) {
        String amount = docInvoice.p_lines[i].getAmount();
        String amountConverted = "";
        ConversionRateDoc conversionRateCurrentDoc = docInvoice.getConversionRateDoc(
            docInvoice.TABLEID_Invoice, docInvoice.Record_ID, docInvoice.C_Currency_ID,
            as.m_C_Currency_ID);
        if (conversionRateCurrentDoc != null) {
          amountConverted = docInvoice.applyRate(new BigDecimal(docInvoice.p_lines[i].getAmount()),
              conversionRateCurrentDoc, true).setScale(2, RoundingMode.HALF_UP).toString();
        } else {
          amountConverted = docInvoice.getConvertedAmt(docInvoice.p_lines[i].getAmount(),
              docInvoice.C_Currency_ID, as.m_C_Currency_ID, docInvoice.DateAcct, "",
              docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn);
        }
        if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()) {
          amount = docInvoice.createAccDefExpenseFact(fact, (DocLine_Invoice) docInvoice.p_lines[i],
              ((DocLine_Invoice) docInvoice.p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense,
                  as, conn),
              ((DocLine_Invoice) docInvoice.p_lines[i])
                  .getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
              amountConverted, as.m_C_Currency_ID, conn);
          if (docInvoice.IsReversal.equals("Y")) {
            fact.createLine(docInvoice.p_lines[i],
                ((DocLine_Invoice) docInvoice.p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense,
                    as, conn),
                as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          } else {
            fact.createLine(docInvoice.p_lines[i],
                ((DocLine_Invoice) docInvoice.p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense,
                    as, conn),
                as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          }
        } else {
          if (docInvoice.IsReversal.equals("Y")) {
            fact.createLine(docInvoice.p_lines[i],
                ((DocLine_Invoice) docInvoice.p_lines[i]).getAccount(ProductInfo.ACCTTYPE_P_Expense,
                    as, conn),
                docInvoice.C_Currency_ID, "", amount, Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          } else {
            /*
             * RARC
             */
            Product product = OBDal.getInstance().get(Product.class,
                docInvoice.p_lines[i].m_M_Product_ID);
            BusinessPartner bPartner = OBDal.getInstance().get(BusinessPartner.class,
                docInvoice.C_BPartner_ID);
            Account account=null;
            if (product.isStocked() && !product.isScrtlaControlAssets()) {
              String accountingCombinationId = bPartner.getBusinessPartnerCategory()
                  .getBusinessPartnerCategoryAccountList().get(0).getNonInvoicedReceipts().getId();
              account = Account.getAccount(conn, accountingCombinationId);
            } else if (product.isStocked() && product.isScrtlaControlAssets())  {
                account = ((DocLine_Invoice) docInvoice.p_lines[i])
                        .getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn);
            } else if (!product.isStocked() && !product.isScrtlaControlAssets())  {
                account = ((DocLine_Invoice) docInvoice.p_lines[i])
                        .getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn);
            }
            
            fact.createLine(docInvoice.p_lines[i], account, docInvoice.C_Currency_ID, amount, "",
                Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                docInvoice.DocumentType, conn);
          }
        }
        // If expense has been deferred
        if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()
            && !amount.equals(amountConverted)) {
          amount = new BigDecimal(amountConverted).subtract(new BigDecimal(amount)).toString();
          if (docInvoice.IsReversal.equals("Y")) {
            fact.createLine(docInvoice.p_lines[i],
                ((DocLine_Invoice) docInvoice.p_lines[i])
                    .getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
                as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          } else {
            fact.createLine(docInvoice.p_lines[i],
                ((DocLine_Invoice) docInvoice.p_lines[i])
                    .getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
                as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          }
        }
      }
      // Set Locations
      FactLine[] fLines = fact.getLines();
      for (int i = 0; fLines != null && i < fLines.length; i++) {
        if (fLines[i] != null) {
          fLines[i].setLocationFromBPartner(docInvoice.C_BPartner_Location_ID, true, conn); // from
                                                                                            // Loc
          fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), false, conn); // to Loc
        }
      }
      docInvoice.updateProductInfo(as.getC_AcctSchema_ID(), conn, con); // only API
    }
    // APC
    else if (docInvoice.DocumentType.equals(AcctServer.DOCTYPE_APCredit)) {
      log4jDocInvoice.debug("Point 4");
      // Liability DR
      if (docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
        for (int i = 0; docInvoice.m_debt_payments != null
            && i < docInvoice.m_debt_payments.length; i++) {
          BigDecimal amount = new BigDecimal(docInvoice.m_debt_payments[i].getAmount());
          // BigDecimal ZERO = BigDecimal.ZERO;
          fact.createLine(
              docInvoice.m_debt_payments[i], docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID,
                  as, false, docInvoice.m_debt_payments[i].getDpStatus(), conn),
              docInvoice.C_Currency_ID,
              docInvoice.getConvertedAmt(((amount.negate())).toPlainString(),
                  docInvoice.m_debt_payments[i].getC_Currency_ID_From(), docInvoice.C_Currency_ID,
                  docInvoice.DateAcct, "", docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
              "", Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
        }
      else
        for (int i = 0; docInvoice.getM_payments() != null
            && i < docInvoice.getM_payments().length; i++) {
          BigDecimal amount = new BigDecimal(docInvoice.getM_payments()[i].getAmount());
          BigDecimal prepaidAmount = new BigDecimal(
              docInvoice.getM_payments()[i].getPrepaidAmount());
          fact.createLine(docInvoice.getM_payments()[i],
              docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, false, conn),
              docInvoice.C_Currency_ID, amount.negate().toString(), "", Fact_Acct_Group_ID,
              docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          // Pre-payment: Probably not needed as at docInvoice point we can not generate
          // pre-payments
          // against APC. Amount is negated
          if (docInvoice.getM_payments()[i].getC_Currency_ID_From().equals(as.m_C_Currency_ID)
              && prepaidAmount.compareTo(BigDecimal.ZERO) != 0) {
            fact.createLine(docInvoice.getM_payments()[i],
                docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, true, conn),
                docInvoice.C_Currency_ID, prepaidAmount.negate().toString(), "", Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          } else if (!docInvoice.getM_payments()[i].getC_Currency_ID_From()
              .equals(as.m_C_Currency_ID)) {
            try {
              DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(conn,
                  docInvoice.getM_payments()[i].getLine_ID(), docInvoice.Record_ID);
              for (int j = 0; j < prepayments.length; j++) {
                BigDecimal prePaymentAmt = docInvoice.convertAmount(
                    new BigDecimal(prepayments[j].prepaidamt).negate(), false, docInvoice.DateAcct,
                    docInvoice.TABLEID_Payment, prepayments[j].finPaymentId,
                    docInvoice.getM_payments()[i].getC_Currency_ID_From(), as.m_C_Currency_ID,
                    docInvoice.getM_payments()[i], as, fact, Fact_Acct_Group_ID,
                    docInvoice.nextSeqNo(docInvoice.getSeqNo()), conn);
                fact.createLine(docInvoice.getM_payments()[i],
                    docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, true, conn),
                    docInvoice.getM_payments()[i].getC_Currency_ID_From(), prePaymentAmt.toString(),
                    "", Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
                    docInvoice.DocumentType, conn);
              }
            } catch (ServletException e) {
              log4jDocInvoice.warn(e);
            }
          }
        }
      if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
          && (docInvoice.m_debt_payments == null || docInvoice.m_debt_payments.length == 0)) {
        fact.createLine(null,
            docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, false, conn),
            docInvoice.C_Currency_ID, docInvoice.Amounts[docInvoice.AMTTYPE_Gross], "",
            Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
            docInvoice.DocumentType, conn);
      }
      // Charge CR
      fact.createLine(null, docInvoice.getAccount(AcctServer.ACCTTYPE_Charge, as, conn),
          docInvoice.C_Currency_ID, "", docInvoice.getAmount(AcctServer.AMTTYPE_Charge),
          Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType,
          conn);
      // TaxCredit CR
      for (int i = 0; docInvoice.getM_taxes() != null && i < docInvoice.getM_taxes().length; i++) {
        // New docLine created to assign C_Tax_ID value to the entry
        DocLine docLine = new DocLine(docInvoice.DocumentType, docInvoice.Record_ID, "");
        docLine.m_C_Tax_ID = docInvoice.getM_taxes()[i].m_C_Tax_ID;
        if (docInvoice.getM_taxes()[i].m_isTaxUndeductable) {
          docInvoice.computeTaxUndeductableLine(conn, as, fact, docLine, Fact_Acct_Group_ID,
              docInvoice.getM_taxes()[i].m_C_Tax_ID, docInvoice.getM_taxes()[i].getAmount());

        } else {
          BigDecimal percentageFinalAccount = CashVATUtil._100;
          final BigDecimal taxesAmountTotal = new BigDecimal(
              StringUtils.isBlank(docInvoice.getM_taxes()[i].getAmount()) ? "0"
                  : docInvoice.getM_taxes()[i].getAmount());
          BigDecimal taxToTransAccount = BigDecimal.ZERO;
          if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
            percentageFinalAccount = CashVATUtil.calculatePrepaidPercentageForCashVATTax(
                docInvoice.getM_taxes()[i].m_C_Tax_ID, docInvoice.Record_ID);
            taxToTransAccount = CashVATUtil.calculatePercentageAmount(
                CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
                docInvoice.C_Currency_ID);
            fact.createLine(docLine,
                docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxCredit_Trans, as, conn),
                docInvoice.C_Currency_ID, "", taxToTransAccount.toString(), Fact_Acct_Group_ID,
                docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
          }
          final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
          fact.createLine(docLine,
              docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxCredit, as, conn),
              docInvoice.C_Currency_ID, "", taxToFinalAccount.toString(), Fact_Acct_Group_ID,
              docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
        }
      }
      // Expense CR
      for (int i = 0; docInvoice.p_lines != null && i < docInvoice.p_lines.length; i++) {
        String amount = docInvoice.p_lines[i].getAmount();
        String amountConverted = "";
        ConversionRateDoc conversionRateCurrentDoc = docInvoice.getConversionRateDoc(
            docInvoice.TABLEID_Invoice, docInvoice.Record_ID, docInvoice.C_Currency_ID,
            as.m_C_Currency_ID);
        if (conversionRateCurrentDoc != null) {
          amountConverted = docInvoice.applyRate(new BigDecimal(docInvoice.p_lines[i].getAmount()),
              conversionRateCurrentDoc, true).setScale(2, RoundingMode.HALF_UP).toString();
        } else {
          amountConverted = docInvoice.getConvertedAmt(docInvoice.p_lines[i].getAmount(),
              docInvoice.C_Currency_ID, as.m_C_Currency_ID, docInvoice.DateAcct, "",
              docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn);
        }
        /*
         * RARC
         */
        Product product = OBDal.getInstance().get(Product.class,
            docInvoice.p_lines[i].m_M_Product_ID);
        BusinessPartner bPartner = OBDal.getInstance().get(BusinessPartner.class,
            docInvoice.C_BPartner_ID);
        Account account=null;
      
        if (product.isStocked() && !product.isScrtlaControlAssets()) {
            String accountingCombinationId = bPartner.getBusinessPartnerCategory()
                .getBusinessPartnerCategoryAccountList().get(0).getNonInvoicedReceipts().getId();
            account = Account.getAccount(conn, accountingCombinationId);
          } else if (product.isStocked() && product.isScrtlaControlAssets())  {
              account = ((DocLine_Invoice) docInvoice.p_lines[i])
                      .getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn);
          } else if (!product.isStocked() && !product.isScrtlaControlAssets())  {
              account = ((DocLine_Invoice) docInvoice.p_lines[i])
                      .getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn);
          }

        if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()) {
          amount = docInvoice.createAccDefExpenseFact(fact, (DocLine_Invoice) docInvoice.p_lines[i],
              account,
              ((DocLine_Invoice) docInvoice.p_lines[i])
                  .getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
              amountConverted, as.m_C_Currency_ID, conn);
          fact.createLine(docInvoice.p_lines[i], account, as.m_C_Currency_ID, "", amount,
              Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
        } else {
          fact.createLine(docInvoice.p_lines[i], account, docInvoice.C_Currency_ID, "", amount,
              Fact_Acct_Group_ID, docInvoice.nextSeqNo(docInvoice.getSeqNo()),
              docInvoice.DocumentType, conn);
        }
        // If expense has been deferred
        if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()
            && !amount.equals(amountConverted)) {
          amount = new BigDecimal(amountConverted).subtract(new BigDecimal(amount)).toString();
          fact.createLine(docInvoice.p_lines[i],
              ((DocLine_Invoice) docInvoice.p_lines[i])
                  .getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
              as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID,
              docInvoice.nextSeqNo(docInvoice.getSeqNo()), docInvoice.DocumentType, conn);
        }

      }
      // Set Locations
      FactLine[] fLines = fact.getLines();
      for (int i = 0; fLines != null && i < fLines.length; i++) {
        if (fLines[i] != null) {
          fLines[i].setLocationFromBPartner(docInvoice.C_BPartner_Location_ID, true, conn); // from
                                                                                            // Loc
          fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), false, conn); // to Loc
        }
      }
    } else {
      log4jDocInvoice
          .warn("Doc_Invoice - docInvoice.DocumentType unknown: " + docInvoice.DocumentType);
      fact = null;
    }
    docInvoice.setSeqNo("0");
    return fact;
  }// createFact

}
