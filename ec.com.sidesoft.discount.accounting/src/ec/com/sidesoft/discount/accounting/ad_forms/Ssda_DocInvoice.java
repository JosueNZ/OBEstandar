
package ec.com.sidesoft.discount.accounting.ad_forms;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.Account;
import org.openbravo.erpCommon.ad_forms.AcctSchema;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.ad_forms.DocInvoice;
import org.openbravo.erpCommon.ad_forms.DocInvoiceTemplate;
import org.openbravo.erpCommon.ad_forms.DocLine;
import org.openbravo.erpCommon.ad_forms.DocLine_Invoice;
import org.openbravo.erpCommon.ad_forms.DocTax;
import org.openbravo.erpCommon.ad_forms.Fact;
import org.openbravo.erpCommon.ad_forms.FactLine;
import org.openbravo.erpCommon.ad_forms.ProductInfo;
import org.openbravo.erpCommon.utility.CashVATUtil;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductAccounts;
import org.openbravo.service.db.DalConnectionProvider;

public class Ssda_DocInvoice extends DocInvoiceTemplate {

	private static final long serialVersionUID = 1L;
	static Logger log4jDocInvoice = Logger.getLogger(Ssda_DocInvoice.class);
	String SeqNo = "0";

	private boolean isReversedInvoice(ConnectionProvider conn, String Record_ID) {
		try {
			DocInvoiceData[] revInv = DocInvoiceData.getIsReversedInvoice(conn, Record_ID);
			return (revInv != null && revInv.length != 0);
		} catch (ServletException e) {
			log4jDocInvoice.warn(e);
		}
		return false;
	}

	public Fact createFact(DocInvoice docInvoice, AcctSchema as, ConnectionProvider conn,
			Connection con, VariablesSecureApp vars) throws ServletException {

		// RR2802 Begin
		Invoice invoice = OBDal.getInstance().get(Invoice.class, docInvoice.Record_ID);
		// RR2802 End

		boolean isReversedInvoice = isReversedInvoice(conn, docInvoice.Record_ID);

		log4jDocInvoice.debug("Starting create fact");
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
								docInvoice.getConvertedAmt(
										docInvoice.m_debt_payments[i].getAmount(),
										docInvoice.m_debt_payments[i].getC_Currency_ID_From(),
										docInvoice.C_Currency_ID, docInvoice.DateAcct, "",
										docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
								"", Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					else
						fact.createLine(docInvoice.m_debt_payments[i],
								docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
										docInvoice.m_debt_payments[i].getDpStatus(), conn),
								docInvoice.C_Currency_ID, "",
								docInvoice.getConvertedAmt(
										docInvoice.m_debt_payments[i].getAmount(),
										docInvoice.m_debt_payments[i].getC_Currency_ID_From(),
										docInvoice.C_Currency_ID, docInvoice.DateAcct, "",
										docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
				}
			else
				for (int i = 0; docInvoice.getM_payments() != null
						&& i < docInvoice.getM_payments().length; i++) {
					fact.createLine(docInvoice.getM_payments()[i],
							docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false,
									conn),
							docInvoice.C_Currency_ID, docInvoice.getM_payments()[i].getAmount(), "",
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
					if (docInvoice.getM_payments()[i].getC_Currency_ID_From()
							.equals(as.m_C_Currency_ID)
							&& new BigDecimal(docInvoice.getM_payments()[i].getPrepaidAmount())
									.compareTo(BigDecimal.ZERO) != 0) {
						fact.createLine(docInvoice.getM_payments()[i],
								docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true,
										true, conn),
								docInvoice.C_Currency_ID,
								docInvoice.getM_payments()[i].getPrepaidAmount(), "",
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					} else if (!docInvoice.getM_payments()[i].getC_Currency_ID_From()
							.equals(as.m_C_Currency_ID)) {
						try {
							DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(
									docInvoice.getConnectionProvider(),
									docInvoice.getM_payments()[i].getLine_ID(),
									docInvoice.Record_ID);
							for (int j = 0; j < prepayments.length; j++) {
								BigDecimal prePaymentAmt = docInvoice.convertAmount(
										new BigDecimal(prepayments[j].prepaidamt), true,
										docInvoice.DateAcct, docInvoice.TABLEID_Payment,
										prepayments[j].finPaymentId,
										docInvoice.getM_payments()[i].getC_Currency_ID_From(),
										as.m_C_Currency_ID, docInvoice.getM_payments()[i], as, fact,
										Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), conn);
								fact.createLine(docInvoice.getM_payments()[i],
										docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
												true, true, conn),
										docInvoice.getM_payments()[i].getC_Currency_ID_From(),
										prePaymentAmt.toString(), "", Fact_Acct_Group_ID,
										docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
							}
						} catch (ServletException e) {
							log4jDocInvoice.warn(e);
						}
					}
				}
			if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
					&& (docInvoice.m_debt_payments == null
							|| docInvoice.m_debt_payments.length == 0)) {
				BigDecimal grossamt = new BigDecimal(docInvoice.Amounts[docInvoice.AMTTYPE_Gross]);
				BigDecimal prepayment = new BigDecimal(docInvoice.getPrepaymentamt());
				BigDecimal difference = grossamt.subtract(prepayment);
				if (!docInvoice.getPrepaymentamt().equals("0")) {
					if (grossamt.abs().compareTo(prepayment.abs()) != 0) {
						if (docInvoice.IsReturn.equals("Y") || isReversedInvoice) {
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											true, true, conn),
									docInvoice.C_Currency_ID, "", docInvoice.getPrepaymentamt(),
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											true, false, conn),
									docInvoice.C_Currency_ID, "", difference.toString(),
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						} else {
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											true, true, conn),
									docInvoice.C_Currency_ID, docInvoice.getPrepaymentamt(), "",
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											true, false, conn),
									docInvoice.C_Currency_ID, difference.toString(), "",
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						}
					} else {
						if (docInvoice.IsReturn.equals("Y") || isReversedInvoice) {
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											true, true, conn),
									docInvoice.C_Currency_ID, "", docInvoice.getPrepaymentamt(),
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						} else {
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											true, true, conn),
									docInvoice.C_Currency_ID, docInvoice.getPrepaymentamt(), "",
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						}
					}
				} else {
					fact.createLine(null,
							docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false,
									conn),
							docInvoice.C_Currency_ID, docInvoice.Amounts[docInvoice.AMTTYPE_Gross],
							"", Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
				}

			}
			// Charge CR
			log4jDocInvoice.debug("The first create line");
			fact.createLine(null, docInvoice.getAccount(AcctServer.ACCTTYPE_Charge, as, conn),
					docInvoice.C_Currency_ID, "", docInvoice.getAmount(AcctServer.AMTTYPE_Charge),
					Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
			// TaxDue CR
			log4jDocInvoice.debug("docInvoice.getM_taxes().length: " + docInvoice.getM_taxes());
			BigDecimal grossamt = new BigDecimal(docInvoice.Amounts[docInvoice.AMTTYPE_Gross]);
			BigDecimal prepayment = new BigDecimal(docInvoice.getPrepaymentamt());

			for (int i = 0; docInvoice.getM_taxes() != null
					&& i < docInvoice.getM_taxes().length; i++) {
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
					Currency currency = OBDal.getInstance().get(Currency.class,
							docInvoice.C_Currency_ID);
					precission = currency.getStandardPrecision().intValue();
				} finally {
					OBContext.restorePreviousMode();
				}
				if (docInvoice.IsReversal.equals("Y")) {
					if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
						if ((docInvoice.getM_payments() == null
								|| docInvoice.getM_payments().length == 0)
								&& (docInvoice.m_debt_payments == null
										|| docInvoice.m_debt_payments.length == 0)
								&& (!docInvoice.getPrepaymentamt().equals("0"))) {
							percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
									.divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
							taxToTransAccount = CashVATUtil.calculatePercentageAmount(
									CashVATUtil._100.subtract(percentageFinalAccount),
									taxesAmountTotal, docInvoice.C_Currency_ID);
						} else {
							percentageFinalAccount = CashVATUtil
									.calculatePrepaidPercentageForCashVATTax(
											docInvoice.getM_taxes()[i].m_C_Tax_ID,
											docInvoice.Record_ID);
							taxToTransAccount = CashVATUtil.calculatePercentageAmount(
									CashVATUtil._100.subtract(percentageFinalAccount),
									taxesAmountTotal, docInvoice.C_Currency_ID);
						}
						fact.createLine(docLine,
								docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue_Trans,
										as, conn),
								docInvoice.C_Currency_ID, taxToTransAccount.toString(), "",
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					}
					final BigDecimal taxToFinalAccount = taxesAmountTotal
							.subtract(taxToTransAccount);
					fact.createLine(docLine,
							docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue, as, conn),
							docInvoice.C_Currency_ID, taxToFinalAccount.toString(), "",
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
				} else {
					if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
						if ((docInvoice.getM_payments() == null
								|| docInvoice.getM_payments().length == 0)
								&& (docInvoice.m_debt_payments == null
										|| docInvoice.m_debt_payments.length == 0)
								&& (!docInvoice.getPrepaymentamt().equals("0"))) {
							percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
									.divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
							taxToTransAccount = CashVATUtil.calculatePercentageAmount(
									CashVATUtil._100.subtract(percentageFinalAccount),
									taxesAmountTotal, docInvoice.C_Currency_ID);
						} else {
							percentageFinalAccount = CashVATUtil
									.calculatePrepaidPercentageForCashVATTax(
											docInvoice.getM_taxes()[i].m_C_Tax_ID,
											docInvoice.Record_ID);
							taxToTransAccount = CashVATUtil.calculatePercentageAmount(
									CashVATUtil._100.subtract(percentageFinalAccount),
									taxesAmountTotal, docInvoice.C_Currency_ID);
						}
						fact.createLine(docLine,
								docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue_Trans,
										as, conn),
								docInvoice.C_Currency_ID, "", taxToTransAccount.toString(),
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					}
					final BigDecimal taxToFinalAccount = taxesAmountTotal
							.subtract(taxToTransAccount);
					fact.createLine(docLine,
							docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue, as, conn),
							docInvoice.C_Currency_ID, "", taxToFinalAccount.toString(),
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
				}
			}
			// Revenue CR
			if (docInvoice.p_lines != null && docInvoice.p_lines.length > 0) {
				for (int i = 0; i < docInvoice.p_lines.length; i++) {

					// RR2802 Begin
					String lineId = docInvoice.p_lines[i].m_TrxLine_ID;
					InvoiceLine line = OBDal.getInstance().get(InvoiceLine.class, lineId);
					BigDecimal discount = getDiscount(invoice, line);
					insertDiscount(docInvoice, docInvoice.p_lines[i], Fact_Acct_Group_ID, fact,
							invoice, line, discount);
					// RR2802 End

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
							docInvoice.TABLEID_Invoice, docInvoice.Record_ID,
							docInvoice.C_Currency_ID, as.m_C_Currency_ID);
					if (conversionRateCurrentDoc != null) {
						amountConverted = docInvoice
								.applyRate(new BigDecimal(docInvoice.p_lines[i].getAmount()),
										conversionRateCurrentDoc, true)
								.setScale(2, RoundingMode.HALF_UP).toString();
					} else {
						amountConverted = docInvoice.getConvertedAmt(
								docInvoice.p_lines[i].getAmount(), docInvoice.C_Currency_ID,
								as.m_C_Currency_ID, docInvoice.DateAcct, "",
								docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn);
					}
					if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()) {
						amount = docInvoice.createAccDefRevenueFact(fact,
								(DocLine_Invoice) docInvoice.p_lines[i], account,
								((DocLine_Invoice) docInvoice.p_lines[i])
										.getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
								amountConverted, as.m_C_Currency_ID, conn);
						if (docInvoice.IsReversal.equals("Y")) {
							fact.createLine(docInvoice.p_lines[i], account, as.m_C_Currency_ID,
									amount, "", Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						} else {
							fact.createLine(docInvoice.p_lines[i], account, as.m_C_Currency_ID, "",
									amount, Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						}
					} else {
						amount = new BigDecimal(amount).add(discount).toString();
						if (docInvoice.IsReversal.equals("Y")) {
							fact.createLine(docInvoice.p_lines[i], account,
									docInvoice.C_Currency_ID, amount, "", Fact_Acct_Group_ID,
									docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
						} else {
							fact.createLine(docInvoice.p_lines[i], account,
									docInvoice.C_Currency_ID, "", amount, Fact_Acct_Group_ID,
									docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
						}
					}
					// If revenue has been deferred
					if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()
							&& !amount.equals(amountConverted)) {
						amount = new BigDecimal(amountConverted).subtract(new BigDecimal(amount))
								.toString();
						if (docInvoice.IsReversal.equals("Y")) {
							fact.createLine(docInvoice.p_lines[i],
									((DocLine_Invoice) docInvoice.p_lines[i]).getAccount(
											ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
									as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID,
									docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
						} else {
							fact.createLine(docInvoice.p_lines[i],
									((DocLine_Invoice) docInvoice.p_lines[i]).getAccount(
											ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
									as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID,
									docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
						}
					}
				}
			}

			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), true, conn); // from
																							// Loc
					fLines[i].setLocationFromBPartner(docInvoice.C_BPartner_Location_ID, false,
							conn); // to Loc
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
					// BigDecimal BigDecimal.ZERO = BigDecimal.ZERO;
					fact.createLine(docInvoice.m_debt_payments[i],
							docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true,
									docInvoice.m_debt_payments[i].getDpStatus(), conn),
							docInvoice.C_Currency_ID, "",
							docInvoice.getConvertedAmt(((amount.negate())).toPlainString(),
									docInvoice.m_debt_payments[i].getC_Currency_ID_From(),
									docInvoice.C_Currency_ID, docInvoice.DateAcct, "",
									docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
				}
			else
				for (int i = 0; docInvoice.getM_payments() != null
						&& i < docInvoice.getM_payments().length; i++) {
					BigDecimal amount = new BigDecimal(docInvoice.getM_payments()[i].getAmount());
					BigDecimal prepaidAmount = new BigDecimal(
							docInvoice.getM_payments()[i].getPrepaidAmount());
					fact.createLine(docInvoice.getM_payments()[i],
							docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false,
									conn),
							docInvoice.C_Currency_ID, "", amount.negate().toString(),
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
					// Pre-payment: Probably not needed as at this point we can not generate
					// pre-payments
					// against ARC. getAmount() is negated
					if (docInvoice.getM_payments()[i].getC_Currency_ID_From().equals(
							as.m_C_Currency_ID) && prepaidAmount.compareTo(BigDecimal.ZERO) != 0) {
						fact.createLine(docInvoice.getM_payments()[i],
								docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true,
										true, conn),
								docInvoice.C_Currency_ID, "", prepaidAmount.negate().toString(),
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					} else if (!docInvoice.getM_payments()[i].getC_Currency_ID_From()
							.equals(as.m_C_Currency_ID)) {
						try {
							DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(
									docInvoice.getConnectionProvider(),
									docInvoice.getM_payments()[i].getLine_ID(),
									docInvoice.Record_ID);
							for (int j = 0; j < prepayments.length; j++) {
								BigDecimal prePaymentAmt = docInvoice.convertAmount(
										new BigDecimal(prepayments[j].prepaidamt).negate(), true,
										docInvoice.DateAcct, docInvoice.TABLEID_Payment,
										prepayments[j].finPaymentId,
										docInvoice.getM_payments()[i].getC_Currency_ID_From(),
										as.m_C_Currency_ID, docInvoice.getM_payments()[i], as, fact,
										Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), conn);
								fact.createLine(docInvoice.getM_payments()[i],
										docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
												true, true, conn),
										docInvoice.getM_payments()[i].getC_Currency_ID_From(), "",
										prePaymentAmt.toString(), Fact_Acct_Group_ID,
										docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
							}
						} catch (ServletException e) {
							log4jDocInvoice.warn(e);
						}
					}
				}
			if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
					&& (docInvoice.m_debt_payments == null
							|| docInvoice.m_debt_payments.length == 0)) {
				fact.createLine(null,
						docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, true, false,
								conn),
						docInvoice.C_Currency_ID, "", docInvoice.Amounts[docInvoice.AMTTYPE_Gross],
						Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType,
						conn);
			}
			// Charge DR
			fact.createLine(null, docInvoice.getAccount(AcctServer.ACCTTYPE_Charge, as, conn),
					docInvoice.C_Currency_ID, docInvoice.getAmount(AcctServer.AMTTYPE_Charge), "",
					Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
			// TaxDue DR
			for (int i = 0; docInvoice.getM_taxes() != null
					&& i < docInvoice.getM_taxes().length; i++) {
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
							docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue_Trans, as,
									conn),
							docInvoice.C_Currency_ID, taxToTransAccount.toString(), "",
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
				}
				final BigDecimal taxToFinalAccount = taxesAmountTotal.subtract(taxToTransAccount);
				fact.createLine(docLine,
						docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxDue, as, conn),
						docInvoice.C_Currency_ID, taxToFinalAccount.toString(), "",
						Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType,
						conn);
			}
			// Revenue CR
			for (int i = 0; docInvoice.p_lines != null && i < docInvoice.p_lines.length; i++) {

				// RR2802 Begin (Nota de Credito)
				String lineId = docInvoice.p_lines[i].m_TrxLine_ID;
				InvoiceLine line = OBDal.getInstance().get(InvoiceLine.class, lineId);
				BigDecimal discount = getDiscount(invoice, line);
				insertDiscount(docInvoice, docInvoice.p_lines[i], Fact_Acct_Group_ID, fact, invoice,
						line, discount);
				// RR2802 End

				String amount = docInvoice.p_lines[i].getAmount();
				String amountCoverted = "";
				ConversionRateDoc conversionRateCurrentDoc = docInvoice.getConversionRateDoc(
						docInvoice.TABLEID_Invoice, docInvoice.Record_ID, docInvoice.C_Currency_ID,
						as.m_C_Currency_ID);
				if (conversionRateCurrentDoc != null) {
					amountCoverted = docInvoice
							.applyRate(new BigDecimal(docInvoice.p_lines[i].getAmount()),
									conversionRateCurrentDoc, true)
							.setScale(2, RoundingMode.HALF_UP).toString();
				} else {
					amountCoverted = docInvoice.getConvertedAmt(docInvoice.p_lines[i].getAmount(),
							docInvoice.C_Currency_ID, as.m_C_Currency_ID, docInvoice.DateAcct, "",
							docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn);
				}
				Account account = ((DocLine_Invoice) docInvoice.p_lines[i])
						.getAccount(ProductInfo.ACCTTYPE_P_Revenue, as, conn);
				if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()) {
					amount = docInvoice.createAccDefRevenueFact(fact,
							(DocLine_Invoice) docInvoice.p_lines[i], account,
							((DocLine_Invoice) docInvoice.p_lines[i])
									.getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
							amountCoverted, as.m_C_Currency_ID, conn);
					fact.createLine(docInvoice.p_lines[i], account, as.m_C_Currency_ID, amount, "",
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
				} else {
					fact.createLine(docInvoice.p_lines[i], account, docInvoice.C_Currency_ID,
							new BigDecimal(amount).add(discount).toString(), "", Fact_Acct_Group_ID,
							docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
				}
				// If revenue has been deferred
				if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()
						&& !amount.equals(amountCoverted)) {
					amount = new BigDecimal(amountCoverted).subtract(new BigDecimal(amount))
							.toString();
					fact.createLine(docInvoice.p_lines[i],
							((DocLine_Invoice) docInvoice.p_lines[i])
									.getAccount(ProductInfo.ACCTTYPE_P_DefRevenue, as, conn),
							as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID,
							docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
				}
			}

			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; fLines != null && i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), true, conn); // from
																							// Loc
					fLines[i].setLocationFromBPartner(docInvoice.C_BPartner_Location_ID, false,
							conn); // to Loc
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
								docInvoice.getConvertedAmt(
										docInvoice.m_debt_payments[i].getAmount(),
										docInvoice.m_debt_payments[i].getC_Currency_ID_From(),
										docInvoice.C_Currency_ID, docInvoice.DateAcct, "",
										docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
								"", Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					else
						fact.createLine(docInvoice.m_debt_payments[i],
								docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
										docInvoice.m_debt_payments[i].getDpStatus(), conn),
								docInvoice.C_Currency_ID, "",
								docInvoice.getConvertedAmt(
										docInvoice.m_debt_payments[i].getAmount(),
										docInvoice.m_debt_payments[i].getC_Currency_ID_From(),
										docInvoice.C_Currency_ID, docInvoice.DateAcct, "",
										docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
				}
			else
				for (int i = 0; docInvoice.getM_payments() != null
						&& i < docInvoice.getM_payments().length; i++) {
					fact.createLine(docInvoice.getM_payments()[i],
							docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
									false, conn),
							docInvoice.C_Currency_ID, "", docInvoice.getM_payments()[i].getAmount(),
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
					if (docInvoice.getM_payments()[i].getC_Currency_ID_From()
							.equals(as.m_C_Currency_ID)
							&& new BigDecimal(docInvoice.getM_payments()[i].getPrepaidAmount())
									.compareTo(BigDecimal.ZERO) != 0) {
						fact.createLine(docInvoice.getM_payments()[i],
								docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
										true, conn),
								docInvoice.C_Currency_ID, "",
								docInvoice.getM_payments()[i].getPrepaidAmount(),
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					} else if (!docInvoice.getM_payments()[i].getC_Currency_ID_From()
							.equals(as.m_C_Currency_ID)) {
						try {
							DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(
									docInvoice.getConnectionProvider(),
									docInvoice.getM_payments()[i].getLine_ID(),
									docInvoice.Record_ID);
							for (int j = 0; j < prepayments.length; j++) {
								BigDecimal prePaymentAmt = docInvoice.convertAmount(
										new BigDecimal(prepayments[j].prepaidamt), false,
										docInvoice.DateAcct, docInvoice.TABLEID_Payment,
										prepayments[j].finPaymentId,
										docInvoice.getM_payments()[i].getC_Currency_ID_From(),
										as.m_C_Currency_ID, docInvoice.getM_payments()[i], as, fact,
										Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), conn);
								fact.createLine(docInvoice.getM_payments()[i],
										docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
												false, true, conn),
										docInvoice.getM_payments()[i].getC_Currency_ID_From(), "",
										prePaymentAmt.toString(), Fact_Acct_Group_ID,
										docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
							}
						} catch (ServletException e) {
							log4jDocInvoice.warn(e);
						}
					}
				}
			if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
					&& (docInvoice.m_debt_payments == null
							|| docInvoice.m_debt_payments.length == 0)) {
				BigDecimal grossamt = new BigDecimal(docInvoice.Amounts[docInvoice.AMTTYPE_Gross]);
				BigDecimal prepayment = new BigDecimal(docInvoice.getPrepaymentamt());
				BigDecimal difference = grossamt.subtract(prepayment);
				if (!docInvoice.getPrepaymentamt().equals("0")) {
					if (grossamt.abs().compareTo(prepayment.abs()) != 0) {
						if (docInvoice.IsReturn.equals("Y") || isReversedInvoice) {
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											false, true, conn),
									docInvoice.C_Currency_ID, docInvoice.getPrepaymentamt(), "",
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											false, false, conn),
									docInvoice.C_Currency_ID, difference.toString(), "",
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						} else {
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											false, true, conn),
									docInvoice.C_Currency_ID, "", docInvoice.getPrepaymentamt(),
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											false, false, conn),
									docInvoice.C_Currency_ID, "", difference.toString(),
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						}
					} else {
						if (docInvoice.IsReturn.equals("Y") || isReversedInvoice) {
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											false, true, conn),
									docInvoice.C_Currency_ID, docInvoice.getPrepaymentamt(), "",
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						} else {
							fact.createLine(null,
									docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
											false, true, conn),
									docInvoice.C_Currency_ID, "", docInvoice.getPrepaymentamt(),
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						}
					}
				} else {
					fact.createLine(null,
							docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
									false, conn),
							docInvoice.C_Currency_ID, "",
							docInvoice.Amounts[docInvoice.AMTTYPE_Gross], Fact_Acct_Group_ID,
							docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
				}

			}
			// Charge DR
			fact.createLine(null, docInvoice.getAccount(AcctServer.ACCTTYPE_Charge, as, conn),
					docInvoice.C_Currency_ID, docInvoice.getAmount(AcctServer.AMTTYPE_Charge), "",
					Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
			BigDecimal grossamt = new BigDecimal(docInvoice.Amounts[docInvoice.AMTTYPE_Gross]);
			BigDecimal prepayment = new BigDecimal(docInvoice.getPrepaymentamt());
			// TaxCredit DR
			for (int i = 0; docInvoice.getM_taxes() != null
					&& i < docInvoice.getM_taxes().length; i++) {
				// New docLine created to assign C_Tax_ID value to the entry
				DocLine docLine = new DocLine(docInvoice.DocumentType, docInvoice.Record_ID, "");
				docLine.m_C_Tax_ID = docInvoice.getM_taxes()[i].m_C_Tax_ID;
				OBContext.setAdminMode(true);
				int precission = 0;
				try {
					Currency currency = OBDal.getInstance().get(Currency.class,
							docInvoice.C_Currency_ID);
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
							if ((docInvoice.getM_payments() == null
									|| docInvoice.getM_payments().length == 0)
									&& (docInvoice.m_debt_payments == null
											|| docInvoice.m_debt_payments.length == 0)
									&& (!docInvoice.getPrepaymentamt().equals("0"))) {
								percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
										.divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
								taxToTransAccount = CashVATUtil.calculatePercentageAmount(
										CashVATUtil._100.subtract(percentageFinalAccount),
										taxesAmountTotal, docInvoice.C_Currency_ID);
							} else {
								percentageFinalAccount = CashVATUtil
										.calculatePrepaidPercentageForCashVATTax(
												docInvoice.getM_taxes()[i].m_C_Tax_ID,
												docInvoice.Record_ID);
								taxToTransAccount = CashVATUtil.calculatePercentageAmount(
										CashVATUtil._100.subtract(percentageFinalAccount),
										taxesAmountTotal, docInvoice.C_Currency_ID);
							}
							fact.createLine(docLine,
									docInvoice.getM_taxes()[i]
											.getAccount(DocTax.ACCTTYPE_TaxCredit_Trans, as, conn),
									docInvoice.C_Currency_ID, "", taxToTransAccount.toString(),
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						}
						final BigDecimal taxToFinalAccount = taxesAmountTotal
								.subtract(taxToTransAccount);
						fact.createLine(docLine,
								docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxCredit, as,
										conn),
								docInvoice.C_Currency_ID, "", taxToFinalAccount.toString(),
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					} else {
						if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
							if ((docInvoice.getM_payments() == null
									|| docInvoice.getM_payments().length == 0)
									&& (docInvoice.m_debt_payments == null
											|| docInvoice.m_debt_payments.length == 0)
									&& (!docInvoice.getPrepaymentamt().equals("0"))) {
								percentageFinalAccount = ((prepayment.multiply(new BigDecimal(100)))
										.divide(grossamt.abs(), precission, RoundingMode.HALF_UP));
								taxToTransAccount = CashVATUtil.calculatePercentageAmount(
										CashVATUtil._100.subtract(percentageFinalAccount),
										taxesAmountTotal, docInvoice.C_Currency_ID);
							} else {
								percentageFinalAccount = CashVATUtil
										.calculatePrepaidPercentageForCashVATTax(
												docInvoice.getM_taxes()[i].m_C_Tax_ID,
												docInvoice.Record_ID);
								taxToTransAccount = CashVATUtil.calculatePercentageAmount(
										CashVATUtil._100.subtract(percentageFinalAccount),
										taxesAmountTotal, docInvoice.C_Currency_ID);
							}
							fact.createLine(docLine,
									docInvoice.getM_taxes()[i]
											.getAccount(DocTax.ACCTTYPE_TaxCredit_Trans, as, conn),
									docInvoice.C_Currency_ID, taxToTransAccount.toString(), "",
									Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
									docInvoice.DocumentType, conn);
						}
						final BigDecimal taxToFinalAccount = taxesAmountTotal
								.subtract(taxToTransAccount);
						fact.createLine(docLine,
								docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxCredit, as,
										conn),
								docInvoice.C_Currency_ID, taxToFinalAccount.toString(), "",
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					}

				} else {
					DocLineInvoiceData[] data = null;
					try {
						data = DocLineInvoiceData.selectUndeductable(
								docInvoice.getConnectionProvider(), docInvoice.Record_ID,
								docInvoice.getM_taxes()[i].m_C_Tax_ID);
					} catch (ServletException e) {
						log4jDocInvoice.warn(e);
					}

					BigDecimal cumulativeTaxLineAmount = new BigDecimal(0);
					BigDecimal taxAmount = new BigDecimal(0);
					for (int j = 0; data != null && j < data.length; j++) {
						DocLine docLine1 = new DocLine(docInvoice.DocumentType,
								docInvoice.Record_ID, "");
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
								dataEx = DocInvoiceData.selectGLItemAcctForTaxLine(conn,
										as.getC_AcctSchema_ID(),
										docInvoice.getM_taxes()[i].m_C_Tax_ID,
										docInvoice.Record_ID);
							}
							strtaxAmount = docInvoice.getM_taxes()[i].getAmount();
							taxAmount = new BigDecimal(
									strtaxAmount.equals("") ? "0.00" : strtaxAmount);
							if (j == data.length - 1) {
								data[j].taxamt = taxAmount.subtract(cumulativeTaxLineAmount)
										.toPlainString();
							}
							try {

								if (docInvoice.DocumentType.equals(AcctServer.DOCTYPE_APInvoice)) {
									if (docInvoice.IsReversal.equals("Y")) {
										fact.createLine(docLine1,
												Account.getAccount(conn, dataEx[0].pExpenseAcct),
												docInvoice.C_Currency_ID, "", data[j].taxamt,
												Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
												docInvoice.DocumentType, conn);

									} else {
										fact.createLine(docLine1,
												Account.getAccount(conn, dataEx[0].pExpenseAcct),
												docInvoice.C_Currency_ID, data[j].taxamt, "",
												Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
												docInvoice.DocumentType, conn);
									}
								} else if (docInvoice.DocumentType
										.equals(AcctServer.DOCTYPE_APCredit)) {
									fact.createLine(docLine1,
											Account.getAccount(conn, dataEx[0].pExpenseAcct),
											docInvoice.C_Currency_ID, "", data[j].taxamt,
											Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
											docInvoice.DocumentType, conn);
								}
								cumulativeTaxLineAmount = cumulativeTaxLineAmount
										.add(new BigDecimal(data[j].taxamt));
							} catch (ServletException e) {
								log4jDocInvoice.error(
										"Exception in createLineForTaxUndeductable method: " + e);
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
					amount = docInvoice.createAccDefExpenseFact(fact,
							(DocLine_Invoice) docInvoice.p_lines[i],
							((DocLine_Invoice) docInvoice.p_lines[i])
									.getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
							((DocLine_Invoice) docInvoice.p_lines[i])
									.getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
							amountConverted, as.m_C_Currency_ID, conn);
					if (docInvoice.IsReversal.equals("Y")) {
						fact.createLine(docInvoice.p_lines[i],
								((DocLine_Invoice) docInvoice.p_lines[i])
										.getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
								as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID,
								docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
					} else {
						fact.createLine(docInvoice.p_lines[i],
								((DocLine_Invoice) docInvoice.p_lines[i])
										.getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
								as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID,
								docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
					}
				} else {
					if (docInvoice.IsReversal.equals("Y")) {
						fact.createLine(docInvoice.p_lines[i],
								((DocLine_Invoice) docInvoice.p_lines[i])
										.getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
								docInvoice.C_Currency_ID, "", amount, Fact_Acct_Group_ID,
								docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
					} else {
						fact.createLine(docInvoice.p_lines[i],
								((DocLine_Invoice) docInvoice.p_lines[i])
										.getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn),
								docInvoice.C_Currency_ID, amount, "", Fact_Acct_Group_ID,
								docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
					}
				}
				// If expense has been deferred
				if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()
						&& !amount.equals(amountConverted)) {
					amount = new BigDecimal(amountConverted).subtract(new BigDecimal(amount))
							.toString();
					if (docInvoice.IsReversal.equals("Y")) {
						fact.createLine(docInvoice.p_lines[i],
								((DocLine_Invoice) docInvoice.p_lines[i])
										.getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
								as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID,
								docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
					} else {
						fact.createLine(docInvoice.p_lines[i],
								((DocLine_Invoice) docInvoice.p_lines[i])
										.getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
								as.m_C_Currency_ID, amount, "", Fact_Acct_Group_ID,
								docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
					}
				}
			}
			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; fLines != null && i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromBPartner(docInvoice.C_BPartner_Location_ID, true,
							conn); // from Loc
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), false, conn); // to
																								// Loc
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
					// BigDecimal BigDecimal.ZERO = BigDecimal.ZERO;
					fact.createLine(docInvoice.m_debt_payments[i],
							docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
									docInvoice.m_debt_payments[i].getDpStatus(), conn),
							docInvoice.C_Currency_ID,
							docInvoice.getConvertedAmt(((amount.negate())).toPlainString(),
									docInvoice.m_debt_payments[i].getC_Currency_ID_From(),
									docInvoice.C_Currency_ID, docInvoice.DateAcct, "",
									docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn),
							"", Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
				}
			else
				for (int i = 0; docInvoice.getM_payments() != null
						&& i < docInvoice.getM_payments().length; i++) {
					BigDecimal amount = new BigDecimal(docInvoice.getM_payments()[i].getAmount());
					BigDecimal prepaidAmount = new BigDecimal(
							docInvoice.getM_payments()[i].getPrepaidAmount());
					fact.createLine(docInvoice.getM_payments()[i],
							docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
									false, conn),
							docInvoice.C_Currency_ID, amount.negate().toString(), "",
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
					// Pre-payment: Probably not needed as at this point we can not generate
					// pre-payments
					// against APC. getAmount() is negated
					if (docInvoice.getM_payments()[i].getC_Currency_ID_From().equals(
							as.m_C_Currency_ID) && prepaidAmount.compareTo(BigDecimal.ZERO) != 0) {
						fact.createLine(docInvoice.getM_payments()[i],
								docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false,
										true, conn),
								docInvoice.C_Currency_ID, prepaidAmount.negate().toString(), "",
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					} else if (!docInvoice.getM_payments()[i].getC_Currency_ID_From()
							.equals(as.m_C_Currency_ID)) {
						try {
							DocInvoiceData[] prepayments = DocInvoiceData.selectPrepayments(
									docInvoice.getConnectionProvider(),
									docInvoice.getM_payments()[i].getLine_ID(),
									docInvoice.Record_ID);
							for (int j = 0; j < prepayments.length; j++) {
								BigDecimal prePaymentAmt = docInvoice.convertAmount(
										new BigDecimal(prepayments[j].prepaidamt).negate(), false,
										docInvoice.DateAcct, docInvoice.TABLEID_Payment,
										prepayments[j].finPaymentId,
										docInvoice.getM_payments()[i].getC_Currency_ID_From(),
										as.m_C_Currency_ID, docInvoice.getM_payments()[i], as, fact,
										Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), conn);
								fact.createLine(docInvoice.getM_payments()[i],
										docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as,
												false, true, conn),
										docInvoice.getM_payments()[i].getC_Currency_ID_From(),
										prePaymentAmt.toString(), "", Fact_Acct_Group_ID,
										docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
							}
						} catch (ServletException e) {
							log4jDocInvoice.warn(e);
						}
					}
				}
			if ((docInvoice.getM_payments() == null || docInvoice.getM_payments().length == 0)
					&& (docInvoice.m_debt_payments == null
							|| docInvoice.m_debt_payments.length == 0)) {
				fact.createLine(null,
						docInvoice.getAccountBPartner(docInvoice.C_BPartner_ID, as, false, false,
								conn),
						docInvoice.C_Currency_ID, docInvoice.Amounts[docInvoice.AMTTYPE_Gross], "",
						Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType,
						conn);
			}
			// Charge CR
			fact.createLine(null, docInvoice.getAccount(AcctServer.ACCTTYPE_Charge, as, conn),
					docInvoice.C_Currency_ID, "", docInvoice.getAmount(AcctServer.AMTTYPE_Charge),
					Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
			// TaxCredit CR
			for (int i = 0; docInvoice.getM_taxes() != null
					&& i < docInvoice.getM_taxes().length; i++) {
				// New docLine created to assign C_Tax_ID value to the entry
				DocLine docLine = new DocLine(docInvoice.DocumentType, docInvoice.Record_ID, "");
				docLine.m_C_Tax_ID = docInvoice.getM_taxes()[i].m_C_Tax_ID;
				if (docInvoice.getM_taxes()[i].m_isTaxUndeductable) {
					docInvoice.computeTaxUndeductableLine(conn, as, fact, docLine,
							Fact_Acct_Group_ID, docInvoice.getM_taxes()[i].m_C_Tax_ID,
							docInvoice.getM_taxes()[i].getAmount());

				} else {
					BigDecimal percentageFinalAccount = CashVATUtil._100;
					final BigDecimal taxesAmountTotal = new BigDecimal(
							StringUtils.isBlank(docInvoice.getM_taxes()[i].getAmount()) ? "0"
									: docInvoice.getM_taxes()[i].getAmount());
					BigDecimal taxToTransAccount = BigDecimal.ZERO;
					if (docInvoice.isCashVAT() && docInvoice.getM_taxes()[i].m_isCashVAT) {
						percentageFinalAccount = CashVATUtil
								.calculatePrepaidPercentageForCashVATTax(
										docInvoice.getM_taxes()[i].m_C_Tax_ID,
										docInvoice.Record_ID);
						taxToTransAccount = CashVATUtil.calculatePercentageAmount(
								CashVATUtil._100.subtract(percentageFinalAccount), taxesAmountTotal,
								docInvoice.C_Currency_ID);
						fact.createLine(docLine,
								docInvoice.getM_taxes()[i]
										.getAccount(DocTax.ACCTTYPE_TaxCredit_Trans, as, conn),
								docInvoice.C_Currency_ID, "", taxToTransAccount.toString(),
								Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
								docInvoice.DocumentType, conn);
					}
					final BigDecimal taxToFinalAccount = taxesAmountTotal
							.subtract(taxToTransAccount);
					fact.createLine(docLine,
							docInvoice.getM_taxes()[i].getAccount(DocTax.ACCTTYPE_TaxCredit, as,
									conn),
							docInvoice.C_Currency_ID, "", taxToFinalAccount.toString(),
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
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
					amountConverted = docInvoice
							.applyRate(new BigDecimal(docInvoice.p_lines[i].getAmount()),
									conversionRateCurrentDoc, true)
							.setScale(2, RoundingMode.HALF_UP).toString();
				} else {
					amountConverted = docInvoice.getConvertedAmt(docInvoice.p_lines[i].getAmount(),
							docInvoice.C_Currency_ID, as.m_C_Currency_ID, docInvoice.DateAcct, "",
							docInvoice.AD_Client_ID, docInvoice.AD_Org_ID, conn);
				}
				Account account = ((DocLine_Invoice) docInvoice.p_lines[i])
						.getAccount(ProductInfo.ACCTTYPE_P_Expense, as, conn);
				if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()) {
					amount = docInvoice.createAccDefExpenseFact(fact,
							(DocLine_Invoice) docInvoice.p_lines[i], account,
							((DocLine_Invoice) docInvoice.p_lines[i])
									.getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
							amountConverted, as.m_C_Currency_ID, conn);
					fact.createLine(docInvoice.p_lines[i], account, as.m_C_Currency_ID, "", amount,
							Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
				} else {
					fact.createLine(docInvoice.p_lines[i], account, docInvoice.C_Currency_ID, "",
							amount, Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
							docInvoice.DocumentType, conn);
				}
				// If expense has been deferred
				if (((DocLine_Invoice) docInvoice.p_lines[i]).isDeferred()
						&& !amount.equals(amountConverted)) {
					amount = new BigDecimal(amountConverted).subtract(new BigDecimal(amount))
							.toString();
					fact.createLine(docInvoice.p_lines[i],
							((DocLine_Invoice) docInvoice.p_lines[i])
									.getAccount(ProductInfo.ACCTTYPE_P_DefExpense, as, conn),
							as.m_C_Currency_ID, "", amount, Fact_Acct_Group_ID,
							docInvoice.nextSeqNo(SeqNo), docInvoice.DocumentType, conn);
				}

			}
			// Set Locations
			FactLine[] fLines = fact.getLines();
			for (int i = 0; fLines != null && i < fLines.length; i++) {
				if (fLines[i] != null) {
					fLines[i].setLocationFromBPartner(docInvoice.C_BPartner_Location_ID, true,
							conn); // from Loc
					fLines[i].setLocationFromOrg(fLines[i].getAD_Org_ID(conn), false, conn); // to
																								// Loc
				}
			}
		} else {
			log4jDocInvoice.warn(
					"Doc_Invoice - docInvoice.DocumentType unknown: " + docInvoice.DocumentType);
			fact = null;
		}

		SeqNo = "0";
		return fact;
	}// createFact

	// RR2802 Begin
	private BigDecimal getDiscount(Invoice invoice, String lineId) throws ServletException {
		BigDecimal discount = BigDecimal.ZERO;

		if (!invoice.isSalesTransaction())
			return discount;

		try {
			ConnectionProvider conn = new DalConnectionProvider(false);

			String sql = " SELECT ABS(il.qtyinvoiced * (CASE WHEN COALESCE(pp.pricelist,0) != 0 THEN pp.pricelist ELSE il.priceactual END - il.priceactual)) discount "
					+ " FROM c_invoiceline il "
					+ " JOIN c_invoice i ON i.c_invoice_id = il.c_invoice_id "
					+ " JOIN m_productprice pp ON pp.m_product_id = il.m_product_id "
					+ " JOIN m_pricelist_version plv ON plv.m_pricelist_version_id = pp.m_pricelist_version_id"
					+ " AND plv.m_pricelist_id = i.m_pricelist_id "
					+ " WHERE il.c_invoiceline_id = ? ";

			PreparedStatement st = conn.getPreparedStatement(sql);
			st.setString(1, lineId);
			ResultSet rs = st.executeQuery();

			while (rs.next()) {
				discount = rs.getBigDecimal("discount");
			}

			rs.close();
			st.close();
			conn.destroy();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return discount;
	}

	private BigDecimal getDiscount(Invoice invoice, InvoiceLine line) throws ServletException {
		BigDecimal discount = BigDecimal.ZERO;
		if (!invoice.isSalesTransaction())
			return discount;

		BigDecimal qtyLine = line.getInvoicedQuantity();
		BigDecimal listPrice = (line.getListPrice() != null ? line.getListPrice() : BigDecimal.ZERO)
				.multiply(qtyLine);
		BigDecimal unitPrice = line.getUnitPrice().multiply(qtyLine);

		if (listPrice.compareTo(BigDecimal.ZERO) == 0 || listPrice.compareTo(unitPrice) == 0)
			return discount;

		discount = listPrice.subtract(unitPrice);

		return discount;
	}

	private void insertDiscount(DocInvoice docInvoice, DocLine docInvoiceLine,
			String Fact_Acct_Group_ID, Fact fact, Invoice invoice, InvoiceLine line,
			BigDecimal discount) throws ServletException {
		if (discount.compareTo(BigDecimal.ZERO) == 0)
			return;

		Product product = line.getProduct();
		ProductAccounts productAccounts = product.getProductAccountsList().size() > 0
				? product.getProductAccountsList().get(0)
				: null;
		if (productAccounts == null && productAccounts.getSsdaPDiscountAcct() == null)
			return;

		Account account = Account.getAccount(docInvoice.getConnectionProvider(),
				productAccounts.getSsdaPDiscountAcct().getId());

		DocLine docLine = new DocLine(docInvoice.DocumentType, docInvoice.Record_ID, null);
		docLine.setAmount(discount.toString());
		docLine.m_C_BPartner_ID = docInvoiceLine.m_C_BPartner_ID;
		docLine.m_M_Product_ID = docInvoiceLine.m_M_Product_ID;
		docLine.m_C_Costcenter_ID = docInvoiceLine.m_C_Costcenter_ID;
		docLine.m_User1_ID = docInvoiceLine.m_User1_ID;
		docLine.m_User2_ID = docInvoiceLine.m_User2_ID;

		BigDecimal debitAmt = BigDecimal.ZERO;
		BigDecimal creditAmt = BigDecimal.ZERO;
		if (docInvoice.DocumentType.equals(AcctServer.DOCTYPE_ARInvoice)
				|| docInvoice.DocumentType.equals(AcctServer.DOCTYPE_ARProForma)
				|| docInvoice.DocumentType.equals(AcctServer.DOCTYPE_RMSalesInvoice)) {
			debitAmt = discount;
		} else {
			creditAmt = discount;
		}

		fact.createLine(docLine, account, docInvoice.C_Currency_ID, debitAmt.toString(),
				creditAmt.toString(), Fact_Acct_Group_ID, docInvoice.nextSeqNo(SeqNo),
				docInvoice.DocumentType, docInvoice.getConnectionProvider());
	}

	// RR2802 End
}
