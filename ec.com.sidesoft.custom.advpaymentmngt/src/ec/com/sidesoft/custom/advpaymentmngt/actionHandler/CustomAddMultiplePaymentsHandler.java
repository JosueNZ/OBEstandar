/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package ec.com.sidesoft.custom.advpaymentmngt.actionHandler;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;

public class CustomAddMultiplePaymentsHandler extends BaseProcessActionHandler {
  private static final Logger log = Logger.getLogger(CustomAddMultiplePaymentsHandler.class);
  private static final SimpleDateFormat jsDateFormat = JsonUtils.createDateFormat();
  private static final String ACTION_PROCESS_TRANSACTION = "P";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String data) {
    try {
      final JSONObject jsonData = new JSONObject(data);
      final JSONObject jsonparams = jsonData.getJSONObject("_params");

      final JSONArray selectedPayments = jsonparams.getJSONObject("payments")
          .getJSONArray("_selection");
      final Date statementDate = jsDateFormat.parse(jsonparams.getString("statementDate"));
      final Date dateAcct = jsDateFormat.parse(jsonparams.getString("dateAcct"));
      final String strAccountId = jsonData.getString("Fin_Financial_Account_ID");
      final String strDepositNumber = (jsonparams.getString("Deposit") == null
          || jsonparams.getString("Deposit") == "null") ? "ND" : jsonparams.getString("Deposit");

      if (!strDepositNumber.equals("ND")) {
        OBCriteria<FIN_Payment> searchDepositNo = OBDal.getInstance()
            .createCriteria(FIN_Payment.class);
        searchDepositNo.add(Restrictions.eq(FIN_Payment.PROPERTY_ECSCAPDEPOSIT, strDepositNumber));

        if (searchDepositNo.list().size() > 0) {
          return getErrorMessage(OBMessageUtils.messageBD("ECSCAP_ErrorDespositNo"));
        }

      }

      int selectedPaymentsLength = selectedPayments.length();
      if (selectedPaymentsLength == 0) {
        // Validation error: No lines selected
        return getErrorMessage(OBMessageUtils.messageBD("APRM_NO_PAYMENTS_SELECTED"));
      }
      OBCriteria<DocumentType> crtdoctype = OBDal.getInstance().createCriteria(DocumentType.class);
      crtdoctype.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, "FAT"));
      crtdoctype.setMaxResults(1);
      DocumentType dt = null;
      String docno = "";

      if (crtdoctype.count() > 0) {
        dt = (DocumentType) crtdoctype.uniqueResult();
        Sequence seq = dt.getDocumentSequence();
        docno = (seq.getPrefix() != null ? seq.getPrefix() : "")
            + seq.getNextAssignedNumber().toString()
            + (seq.getSuffix() != null ? seq.getSuffix() : "");
        seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
      }

      for (int i = 0; i < selectedPaymentsLength; i++) {
        final JSONObject paymentJS = selectedPayments.getJSONObject(i);
        createAndProcessTransactionFromPayment(paymentJS, statementDate, dateAcct, strAccountId,
            strDepositNumber, dt, docno);
      }

      // Success Message
      return getSuccessMessage(String.format(
          OBMessageUtils.messageBD("APRM_MULTIPLE_TRANSACTIONS_ADDED"), selectedPaymentsLength));

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception creating multiple transactions from payments", e);

      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        return getErrorMessage(message);
      } catch (Exception ignore) {
      }
    }

    return new JSONObject();
  }

  /**
   * Creates a new transaction from the payment and then it processes the transaction
   */
  private void createAndProcessTransactionFromPayment(final JSONObject paymentJS,
      final Date transactionDate, final Date acctDate, String strAccountId, String strDeposit,
      DocumentType doctype, String docno) throws JSONException {

    try {
      OBContext.setAdminMode(true);
      final String paymentId = paymentJS.getString("id");
      log.debug("Creating transaction for FIN_Payment_ID: " + paymentId);
      final FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, paymentId);
      FIN_FinancialAccount account = OBDal.getInstance().get(FIN_FinancialAccount.class,
          strAccountId);

      if (payment != null) {
        if (StringUtils.isEmpty(payment.getReferenceNo()) && !strDeposit.equals("ND")) {
          payment.setReferenceNo(strDeposit);
        }
        final FIN_FinaccTransaction transaction = createFinAccTransaction(payment, strDeposit,
            doctype, docno);
        transaction.setTransactionDate(transactionDate);
        transaction.setDateAcct(acctDate);
        transaction.setAccount(account);
        transaction.setLineNo(TransactionsDao.getTransactionMaxLineNo(account) + 10);
        FIN_TransactionProcess.doTransactionProcess(ACTION_PROCESS_TRANSACTION, transaction);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns a JSONObject with the success message to be printed
   */
  private static JSONObject getSuccessMessage(final String msgText) {
    final JSONObject result = new JSONObject();
    try {
      final JSONArray actions = new JSONArray();
      final JSONObject msgInBPTab = new JSONObject();
      msgInBPTab.put("msgType", "success");
      msgInBPTab.put("msgTitle", OBMessageUtils.messageBD("success"));
      msgInBPTab.put("msgText", msgText);
      final JSONObject msgInBPTabAction = new JSONObject();
      msgInBPTabAction.put("showMsgInProcessView", msgInBPTab);
      actions.put(msgInBPTabAction);
      result.put("responseActions", actions);
    } catch (Exception e) {
      log.error(e);
    }

    return result;
  }

  /**
   * Returns a JSONObject with the error message to be printed and retry execution
   */
  private static JSONObject getErrorMessage(final String msgText) {
    final JSONObject result = new JSONObject();
    try {
      final JSONObject msg = new JSONObject();
      msg.put("severity", "error");
      msg.put("text", msgText);
      result.put("message", msg);
      result.put("retryExecution", true);
    } catch (Exception e) {
      log.error(e);
    }
    return result;
  }

  public static FIN_FinaccTransaction createFinAccTransaction(FIN_Payment payment,
      String strDeposit, DocumentType doctype, String docno) {
    final FIN_FinaccTransaction newTransaction = OBProvider.getInstance()
        .get(FIN_FinaccTransaction.class);
    OBContext.setAdminMode();
    try {
      newTransaction.setId(payment.getId());
      newTransaction.setNewOBObject(true);
      newTransaction.setFinPayment(payment);
      newTransaction.setOrganization(payment.getOrganization());
      newTransaction.setAccount(payment.getAccount());
      newTransaction.setDateAcct(payment.getPaymentDate());
      newTransaction.setTransactionDate(payment.getPaymentDate());
      newTransaction.setActivity(payment.getActivity());
      newTransaction.setProject(payment.getProject());
      newTransaction.setCostCenter(payment.getCostCenter());
      newTransaction.setStDimension(payment.getStDimension());
      newTransaction.setNdDimension(payment.getNdDimension());
      newTransaction.setCurrency(payment.getAccount().getCurrency());
      if (doctype != null) {
        newTransaction.setSfadtDocumentno(docno);
        newTransaction.setSfadtCDoctype(doctype);
        newTransaction.setScactuMultiplePayment(true);
      }

      String desc = "";
      if (payment.getDescription() != null && !payment.getDescription().isEmpty()) {
        desc = payment.getDescription().replace("\n", ". ").substring(0,
            payment.getDescription().length() > 254 ? 254 : payment.getDescription().length());
      }
      newTransaction.setDescription(desc);
      newTransaction.setClient(payment.getClient());
      newTransaction.setLineNo(getTransactionMaxLineNo(payment.getAccount()) + 10);

      BigDecimal depositAmt = FIN_Utility.getDepositAmount(
          payment.getDocumentType().getDocumentCategory().equals("ARR"),
          payment.getFinancialTransactionAmount());
      BigDecimal paymentAmt = FIN_Utility.getPaymentAmount(
          payment.getDocumentType().getDocumentCategory().equals("ARR"),
          payment.getFinancialTransactionAmount());

      newTransaction.setDepositAmount(depositAmt);
      newTransaction.setPaymentAmount(paymentAmt);
      newTransaction.setStatus(
          newTransaction.getDepositAmount().compareTo(newTransaction.getPaymentAmount()) > 0 ? "RPR"
              : "PPM");
      if (!newTransaction.getCurrency().equals(payment.getCurrency())) {
        newTransaction.setForeignCurrency(payment.getCurrency());
        newTransaction.setForeignConversionRate(payment.getFinancialTransactionConvertRate());
        newTransaction.setForeignAmount(payment.getAmount());
      }
      payment.getFINFinaccTransactionList().add(newTransaction);

      if (!strDeposit.equals("ND") && payment.isReceipt()) {

        FIN_Payment depositPayment = payment;
        depositPayment.setEcscapDeposit(strDeposit);
        OBDal.getInstance().save(depositPayment);
      }

      OBDal.getInstance().save(newTransaction);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
    return newTransaction;
  }

  public static Long getTransactionMaxLineNo(FIN_FinancialAccount financialAccount) {
    Query query = OBDal.getInstance().getSession().createQuery(
        "select max(f.lineNo) as maxLineno from FIN_Finacc_Transaction as f where account.id=?");
    query.setString(0, financialAccount.getId());
    for (Object obj : query.list()) {
      if (obj != null) {
        return (Long) obj;
      }
    }
    return 0l;
  }

}
