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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.process;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.ServletException;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.CashVATUtil;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationInformation;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentPropDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentProposal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedInvV;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;
import org.openbravo.scheduling.ProcessBundle;

public class FIN_AddPayment {
  private static AdvPaymentMngtDao dao;

  /**
   * Saves the payment and the payment details based on the given Payment Schedule Details. If no
   * FIN_Payment is given it creates a new one.
   * 
   * If the Payment Scheduled Detail is not completely paid and the difference is not written a new
   * Payment Schedule Detail is created with the difference.
   * 
   * If a Refund Amount is given an extra Payment Detail will be created with it.
   * 
   * @param _payment
   *          FIN_Payment where new payment details will be saved.
   * @param isReceipt
   *          boolean to define if the Payment is a Receipt Payment (true) or a Payable Payment
   *          (false). Used when no FIN_Payment is given.
   * @param docType
   *          DocumentType of the Payment. Used when no FIN_Payment is given.
   * @param strPaymentDocumentNo
   *          String with the Document Number of the new payment. Used when no FIN_Payment is given.
   * @param businessPartner
   *          BusinessPartner of the new Payment. Used when no FIN_Payment is given.
   * @param paymentMethod
   *          FIN_PaymentMethod of the new Payment. Used when no FIN_Payment is given.
   * @param finAccount
   *          FIN_FinancialAccount of the new Payment. Used when no FIN_Payment is given.
   * @param strPaymentAmount
   *          String with the Payment Amount of the new Payment. Used when no FIN_Payment is given.
   * @param paymentDate
   *          Date when the Payment is done. Used when no FIN_Payment is given.
   * @param organization
   *          Organization of the new Payment. Used when no FIN_Payment is given.
   * @param selectedPaymentScheduleDetails
   *          List of FIN_PaymentScheduleDetail to be included in the Payment. If one of the items
   *          is contained in other payment the method will throw an exception. Prevent
   *          invoice/order to be paid several times.
   * @param selectedPaymentScheduleDetailsAmounts
   *          HashMap with the Amount to be paid for each Scheduled Payment Detail.
   * @param isWriteoff
   *          Boolean to write off the difference when the payment amount is lower than the Payment
   *          Scheduled PAyment Detail amount.
   * @param isRefund
   *          Not used.
   * @param paymentCurrency
   *          The currency that the payment is being made in. Will default to financial account
   *          currency if not specified
   * @param finTxnConvertRate
   *          Exchange rate to convert between payment currency and financial account currency for
   *          this payment. Defaults to 1.0 if not supplied
   * @param finTxnAmount
   *          Amount of payment in currency of financial account
   * @param doFlush
   *          Force to flush inside the method after creating the payment
   * @param paymentId
   *          id to set in new entities
   * @return The FIN_Payment OBObject containing all the Payment Details.
   */
  public static FIN_Payment savePayment(FIN_Payment _payment, boolean isReceipt,
      DocumentType docType, String strPaymentDocumentNo, BusinessPartner businessPartner,
      FIN_PaymentMethod paymentMethod, FIN_FinancialAccount finAccount, String strPaymentAmount,
      Date paymentDate, Organization organization, String referenceNo,
      List<FIN_PaymentScheduleDetail> selectedPaymentScheduleDetails,
      HashMap<String, BigDecimal> selectedPaymentScheduleDetailsAmounts, boolean isWriteoff,
      boolean isRefund, Currency paymentCurrency, BigDecimal finTxnConvertRate,
      BigDecimal finTxnAmount, boolean doFlush, String paymentId) {
    dao = new AdvPaymentMngtDao();

    BigDecimal assignedAmount = BigDecimal.ZERO;
    final FIN_Payment payment;
    if (_payment != null) {
      payment = _payment;
    } else {
      payment = dao.getNewPayment(isReceipt, organization, docType, strPaymentDocumentNo,
          businessPartner, paymentMethod, finAccount, strPaymentAmount, paymentDate, referenceNo,
          paymentCurrency, finTxnConvertRate, finTxnAmount, paymentId);
      if (doFlush) {
        try {
          OBDal.getInstance().flush();
        } catch (Exception e) {
          throw new OBException(FIN_Utility.getExceptionMessage(e));
        }
      }
    }

    for (FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
      assignedAmount = assignedAmount.add(paymentDetail.getAmount());
    }
    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done
    OBContext.setAdminMode();
    try {
      for (FIN_PaymentScheduleDetail paymentScheduleDetail : selectedPaymentScheduleDetails) {
        BigDecimal paymentDetailAmount = selectedPaymentScheduleDetailsAmounts
            .get(paymentScheduleDetail.getId());
        // Payment Schedule Detail already linked to a payment detail.
        OBDal.getInstance().refresh(paymentScheduleDetail);

        BigDecimal assignedAmountDiff = updatePaymentDetail(paymentScheduleDetail, payment,
            paymentDetailAmount, isWriteoff, doFlush);
        assignedAmount = assignedAmount.add(assignedAmountDiff);
      }
      // TODO: Review this condition !=0??
      if (assignedAmount.compareTo(payment.getAmount()) == -1) {
        FIN_PaymentScheduleDetail refundScheduleDetail = dao.getNewPaymentScheduleDetail(
            payment.getOrganization(), payment.getAmount().subtract(assignedAmount), paymentId);
        dao.getNewPaymentDetail(payment, refundScheduleDetail,
            payment.getAmount().subtract(assignedAmount), BigDecimal.ZERO, false, null, true,
            paymentId);
      }
    } catch (final Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return payment;
  }

  /**
   * Saves the payment and the payment details based on the given Payment Schedule Details. If no
   * FIN_Payment is given it creates a new one.
   * 
   * If the Payment Scheduled Detail is not completely paid and the difference is not written a new
   * Payment Schedule Detail is created with the difference.
   * 
   * If a Refund Amount is given an extra Payment Detail will be created with it.
   * 
   * @param _payment
   *          FIN_Payment where new payment details will be saved.
   * @param isReceipt
   *          boolean to define if the Payment is a Receipt Payment (true) or a Payable Payment
   *          (false). Used when no FIN_Payment is given.
   * @param docType
   *          DocumentType of the Payment. Used when no FIN_Payment is given.
   * @param strPaymentDocumentNo
   *          String with the Document Number of the new payment. Used when no FIN_Payment is given.
   * @param businessPartner
   *          BusinessPartner of the new Payment. Used when no FIN_Payment is given.
   * @param paymentMethod
   *          FIN_PaymentMethod of the new Payment. Used when no FIN_Payment is given.
   * @param finAccount
   *          FIN_FinancialAccount of the new Payment. Used when no FIN_Payment is given.
   * @param strPaymentAmount
   *          String with the Payment Amount of the new Payment. Used when no FIN_Payment is given.
   * @param paymentDate
   *          Date when the Payment is done. Used when no FIN_Payment is given.
   * @param organization
   *          Organization of the new Payment. Used when no FIN_Payment is given.
   * @param selectedPaymentScheduleDetails
   *          List of FIN_PaymentScheduleDetail to be included in the Payment. If one of the items
   *          is contained in other payment the method will throw an exception. Prevent
   *          invoice/order to be paid several times.
   * @param selectedPaymentScheduleDetailsAmounts
   *          HashMap with the Amount to be paid for each Scheduled Payment Detail.
   * @param isWriteoff
   *          Boolean to write off the difference when the payment amount is lower than the Payment
   *          Scheduled PAyment Detail amount.
   * @param isRefund
   *          Not used.
   * @param paymentCurrency
   *          The currency that the payment is being made in. Will default to financial account
   *          currency if not specified
   * @param finTxnConvertRate
   *          Exchange rate to convert between payment currency and financial account currency for
   *          this payment. Defaults to 1.0 if not supplied
   * @param finTxnAmount
   *          Amount of payment in currency of financial account
   * @return The FIN_Payment OBObject containing all the Payment Details.
   */
  public static FIN_Payment savePayment(FIN_Payment _payment, boolean isReceipt,
      DocumentType docType, String strPaymentDocumentNo, BusinessPartner businessPartner,
      FIN_PaymentMethod paymentMethod, FIN_FinancialAccount finAccount, String strPaymentAmount,
      Date paymentDate, Organization organization, String referenceNo,
      List<FIN_PaymentScheduleDetail> selectedPaymentScheduleDetails,
      HashMap<String, BigDecimal> selectedPaymentScheduleDetailsAmounts, boolean isWriteoff,
      boolean isRefund, Currency paymentCurrency, BigDecimal finTxnConvertRate,
      BigDecimal finTxnAmount) {
    return savePayment(_payment, isReceipt, docType, strPaymentDocumentNo, businessPartner,
        paymentMethod, finAccount, strPaymentAmount, paymentDate, organization, referenceNo,
        selectedPaymentScheduleDetails, selectedPaymentScheduleDetailsAmounts, isWriteoff,
        isRefund, paymentCurrency, finTxnConvertRate, finTxnAmount, true, null);
  }

  /*
   * Temporary method to supply defaults for exchange Rate and converted amount
   */
  public static FIN_Payment savePayment(FIN_Payment _payment, boolean isReceipt,
      DocumentType docType, String strPaymentDocumentNo, BusinessPartner businessPartner,
      FIN_PaymentMethod paymentMethod, FIN_FinancialAccount finAccount, String strPaymentAmount,
      Date paymentDate, Organization organization, String referenceNo,
      List<FIN_PaymentScheduleDetail> selectedPaymentScheduleDetails,
      HashMap<String, BigDecimal> selectedPaymentScheduleDetailsAmounts, boolean isWriteoff,
      boolean isRefund) {
    return savePayment(_payment, isReceipt, docType, strPaymentDocumentNo, businessPartner,
        paymentMethod, finAccount, strPaymentAmount, paymentDate, organization, referenceNo,
        selectedPaymentScheduleDetails, selectedPaymentScheduleDetailsAmounts, isWriteoff,
        isRefund, null, null, null, true, null);
  }

  /**
   * Saves the payment and the payment details based on the given Payment Schedule Details. If no
   * FIN_Payment is given it creates a new one.
   * 
   * If the Payment Scheduled Detail is not completely paid and the difference is not written a new
   * Payment Schedule Detail is created with the difference.
   * 
   * If a Refund Amount is given an extra Payment Detail will be created with it.
   * 
   * @param _payment
   *          FIN_Payment where new payment details will be saved.
   * @param isReceipt
   *          boolean to define if the Payment is a Receipt Payment (true) or a Payable Payment
   *          (false). Used when no FIN_Payment is given.
   * @param docType
   *          DocumentType of the Payment. Used when no FIN_Payment is given.
   * @param strPaymentDocumentNo
   *          String with the Document Number of the new payment. Used when no FIN_Payment is given.
   * @param businessPartner
   *          BusinessPartner of the new Payment. Used when no FIN_Payment is given.
   * @param paymentMethod
   *          FIN_PaymentMethod of the new Payment. Used when no FIN_Payment is given.
   * @param finAccount
   *          FIN_FinancialAccount of the new Payment. Used when no FIN_Payment is given.
   * @param strPaymentAmount
   *          String with the Payment Amount of the new Payment. Used when no FIN_Payment is given.
   * @param paymentDate
   *          Date when the Payment is done. Used when no FIN_Payment is given.
   * @param organization
   *          Organization of the new Payment. Used when no FIN_Payment is given.
   * @param selectedPaymentScheduleDetails
   *          List of FIN_PaymentScheduleDetail to be included in the Payment. If one of the items
   *          is contained in other payment the method will throw an exception. Prevent
   *          invoice/order to be paid several times.
   * @param selectedPaymentScheduleDetailsAmounts
   *          HashMap with the Amount to be paid for each Scheduled Payment Detail.
   * @param isWriteoff
   *          Boolean to write off the difference when the payment amount is lower than the Payment
   *          Scheduled PAyment Detail amount.
   * @param isRefund
   *          Not used.
   * @param doFlush
   *          Force to flush inside the method after creating the payment
   * @return The FIN_Payment OBObject containing all the Payment Details.
   */
  public static FIN_Payment savePayment(FIN_Payment _payment, boolean isReceipt,
      DocumentType docType, String strPaymentDocumentNo, BusinessPartner businessPartner,
      FIN_PaymentMethod paymentMethod, FIN_FinancialAccount finAccount, String strPaymentAmount,
      Date paymentDate, Organization organization, String referenceNo,
      List<FIN_PaymentScheduleDetail> selectedPaymentScheduleDetails,
      HashMap<String, BigDecimal> selectedPaymentScheduleDetailsAmounts, boolean isWriteoff,
      boolean isRefund, boolean doFlush) {
    return savePayment(_payment, isReceipt, docType, strPaymentDocumentNo, businessPartner,
        paymentMethod, finAccount, strPaymentAmount, paymentDate, organization, referenceNo,
        selectedPaymentScheduleDetails, selectedPaymentScheduleDetailsAmounts, isWriteoff,
        isRefund, null, null, null, doFlush, null);
  }

  /**
   * Updates the paymentScheduleDetail with the paymentDetailAmount. If it is not related to the
   * Payment a new Payment Detail is created. If isWriteoff is true and the amount is different to
   * the outstanding amount the difference is written off.
   * 
   * @param paymentScheduleDetail
   *          the Payment Schedule Detail to be assigned to the Payment
   * @param payment
   *          the FIN_Payment that it is being paid
   * @param paymentDetailAmount
   *          the amount of this paymentScheduleDetail that it is being paid
   * @param isWriteoff
   *          flag to write off the difference when there is an outstanding amount remaining to pay
   * @return a BigDecimal with the amount newly assigned to the payment. For example, when the
   *         paymentScheduleDetail is already related to the payment and its amount is not changed
   *         BigDecimal.ZERO is returned.
   * @throws OBException
   *           when the paymentDetailAmount is related to a different payment.
   */
  public static BigDecimal updatePaymentDetail(FIN_PaymentScheduleDetail paymentScheduleDetail,
      FIN_Payment payment, BigDecimal paymentDetailAmount, boolean isWriteoff) throws OBException {
    return updatePaymentDetail(paymentScheduleDetail, payment, paymentDetailAmount, isWriteoff,
        true);
  }

  /**
   * Updates the paymentScheduleDetail with the paymentDetailAmount. If it is not related to the
   * Payment a new Payment Detail is created. If isWriteoff is true and the amount is different to
   * the outstanding amount the difference is written off.
   * 
   * @param paymentScheduleDetail
   *          the Payment Schedule Detail to be assigned to the Payment
   * @param payment
   *          the FIN_Payment that it is being paid
   * @param paymentDetailAmount
   *          the amount of this paymentScheduleDetail that it is being paid
   * @param isWriteoff
   *          flag to write off the difference when there is an outstanding amount remaining to pay
   * @param doFlush
   *          Force to flush inside the method
   * @return a BigDecimal with the amount newly assigned to the payment. For example, when the
   *         paymentScheduleDetail is already related to the payment and its amount is not changed
   *         BigDecimal.ZERO is returned.
   * @throws OBException
   *           when the paymentDetailAmount is related to a different payment.
   */
  public static BigDecimal updatePaymentDetail(FIN_PaymentScheduleDetail paymentScheduleDetail,
      FIN_Payment payment, BigDecimal paymentDetailAmount, boolean isWriteoff, boolean doFlush)
      throws OBException {
    BigDecimal assignedAmount = paymentDetailAmount;
    if (paymentScheduleDetail.getPaymentDetails() != null) {
      if (!paymentScheduleDetail.getPaymentDetails().getFinPayment().getId()
          .equals(payment.getId())) {
        // If payment schedule detail belongs to a different payment
        throw new OBException(String.format(FIN_Utility.messageBD("APRM_PsdInSeveralPayments"),
            paymentScheduleDetail.getIdentifier()));
      }
      // Detail for this payment already exists. Payment being edited
      // If amount has changed payment schedule details needs to be updated. Aggregate amount
      // coming from unpaid schedule detail which remains unpaid
      if (paymentScheduleDetail.getAmount().add(paymentScheduleDetail.getWriteoffAmount())
          .compareTo(paymentDetailAmount) != 0) {
        // update Amounts as they have changed
        assignedAmount = assignedAmount.subtract(paymentScheduleDetail.getPaymentDetails()
            .getAmount());
        // update detail with the new value
        List<FIN_PaymentScheduleDetail> outStandingPSDs = getOutstandingPSDs(paymentScheduleDetail);
        BigDecimal difference = paymentScheduleDetail.getAmount()
            .add(paymentScheduleDetail.getWriteoffAmount()).subtract(paymentDetailAmount);
        // Assume doubtful debt is always positive
        BigDecimal doubtFulDebtAmount = BigDecimal.ZERO;
        if (outStandingPSDs.size() == 0) {
          doubtFulDebtAmount = getDoubtFulDebtAmount(
              paymentScheduleDetail.getAmount().add(paymentScheduleDetail.getWriteoffAmount()),
              paymentDetailAmount, paymentScheduleDetail.getDoubtfulDebtAmount());
          if (!isWriteoff) {
            // No outstanding PSD exists so one needs to be created for the difference
            FIN_PaymentScheduleDetail outstandingPSD = (FIN_PaymentScheduleDetail) DalUtil.copy(
                paymentScheduleDetail, false);
            outstandingPSD.setAmount(difference);
            outstandingPSD.setWriteoffAmount(BigDecimal.ZERO);
            outstandingPSD.setDoubtfulDebtAmount(paymentScheduleDetail.getDoubtfulDebtAmount()
                .subtract(doubtFulDebtAmount));
            outstandingPSD.setPaymentDetails(null);
            paymentScheduleDetail.setAmount(paymentScheduleDetail.getAmount().add(
                paymentScheduleDetail.getWriteoffAmount()));
            paymentScheduleDetail.setWriteoffAmount(BigDecimal.ZERO);
            paymentScheduleDetail.getPaymentDetails().setWriteoffAmount(BigDecimal.ZERO);
            OBDal.getInstance().save(outstandingPSD);
          } else {
            // If it is write Off then incorporate all doubtful debt
            doubtFulDebtAmount = paymentScheduleDetail.getDoubtfulDebtAmount();
            // Set difference as writeoff
            paymentScheduleDetail.setWriteoffAmount(difference);
            paymentScheduleDetail.setDoubtfulDebtAmount(doubtFulDebtAmount);
            OBDal.getInstance().save(paymentScheduleDetail);
            paymentScheduleDetail.getPaymentDetails().setWriteoffAmount(difference);
            OBDal.getInstance().save(paymentScheduleDetail.getPaymentDetails());
          }
        } else {
          FIN_PaymentScheduleDetail outstandingPSD = outStandingPSDs.get(0);
          if (!isWriteoff) {
            if (outstandingPSD.getAmount().add(difference).signum() == 0) {
              // If the outstanding psd amount is zero after adding the difference delete it.
              doubtFulDebtAmount = paymentScheduleDetail.getDoubtfulDebtAmount().add(
                  outstandingPSD.getDoubtfulDebtAmount());
              OBDal.getInstance().remove(outstandingPSD);
            } else {
              // update existing PD with difference
              doubtFulDebtAmount = getDoubtFulDebtAmount(
                  paymentScheduleDetail.getAmount().add(outstandingPSD.getAmount()),
                  paymentDetailAmount,
                  paymentScheduleDetail.getDoubtfulDebtAmount().add(
                      outstandingPSD.getDoubtfulDebtAmount()));
              outstandingPSD.setAmount(outstandingPSD.getAmount().add(difference));
              outstandingPSD.setDoubtfulDebtAmount(outstandingPSD.getDoubtfulDebtAmount().add(
                  paymentScheduleDetail.getDoubtfulDebtAmount().subtract(doubtFulDebtAmount)));
              OBDal.getInstance().save(outstandingPSD);
            }
            paymentScheduleDetail.setWriteoffAmount(BigDecimal.ZERO);
            paymentScheduleDetail.getPaymentDetails().setWriteoffAmount(BigDecimal.ZERO);
          } else {
            paymentScheduleDetail.setWriteoffAmount(difference.add(outstandingPSD.getAmount()));
            doubtFulDebtAmount = outstandingPSD.getDoubtfulDebtAmount().add(
                paymentScheduleDetail.getDoubtfulDebtAmount());
            OBDal.getInstance().save(paymentScheduleDetail);
            paymentScheduleDetail.getPaymentDetails().setWriteoffAmount(
                difference.add(outstandingPSD.getAmount()));
            OBDal.getInstance().save(paymentScheduleDetail.getPaymentDetails());
            OBDal.getInstance().remove(outstandingPSD);
          }
        }
        paymentScheduleDetail.setAmount(paymentDetailAmount);
        paymentScheduleDetail.setDoubtfulDebtAmount(doubtFulDebtAmount);
        OBDal.getInstance().save(paymentScheduleDetail);
        paymentScheduleDetail.getPaymentDetails().setAmount(paymentDetailAmount);
        OBDal.getInstance().save(paymentScheduleDetail.getPaymentDetails());
      } else if (isWriteoff) {
        // The amount of the Payment Detail has not changed but the outstanding amount is written
        // off.
        // If any outstanding psd is found it is deleted and the payment schedule detail is
        // updated.
        List<FIN_PaymentScheduleDetail> outStandingPSDs = getOutstandingPSDs(paymentScheduleDetail);
        BigDecimal writeOffAmt = BigDecimal.ZERO;
        BigDecimal doubtfulAmt = BigDecimal.ZERO;
        for (FIN_PaymentScheduleDetail outstandingPSD : outStandingPSDs) {
          writeOffAmt = writeOffAmt.add(outstandingPSD.getAmount());
          doubtfulAmt = doubtfulAmt.add(outstandingPSD.getDoubtfulDebtAmount());
          OBDal.getInstance().remove(outstandingPSD);
        }
        paymentScheduleDetail.setWriteoffAmount(writeOffAmt);
        paymentScheduleDetail.setDoubtfulDebtAmount(paymentScheduleDetail.getDoubtfulDebtAmount()
            .add(doubtfulAmt));
        OBDal.getInstance().save(paymentScheduleDetail);
        paymentScheduleDetail.getPaymentDetails().setWriteoffAmount(writeOffAmt);
        OBDal.getInstance().save(paymentScheduleDetail.getPaymentDetails());
      }
    } else {
      // If detail to be added is zero amount, skip it
      if (paymentDetailAmount.signum() == 0 && !isWriteoff) {
        return BigDecimal.ZERO;
      }
      dao = new AdvPaymentMngtDao();

      BigDecimal amountDifference = paymentScheduleDetail.getAmount().subtract(paymentDetailAmount);
      // Debt Payment
      BigDecimal doubtfulDebtAmount = getDoubtFulDebtAmount(
          paymentScheduleDetail.getAmount().add(paymentScheduleDetail.getWriteoffAmount()),
          paymentDetailAmount, paymentScheduleDetail.getDoubtfulDebtAmount());
      if (amountDifference.signum() != 0) {
        if (!isWriteoff) {
          dao.duplicateScheduleDetail(paymentScheduleDetail, amountDifference,
              paymentScheduleDetail.getDoubtfulDebtAmount().subtract(doubtfulDebtAmount));
          amountDifference = BigDecimal.ZERO;
        } else {
          doubtfulDebtAmount = paymentScheduleDetail.getDoubtfulDebtAmount();
          paymentScheduleDetail.setWriteoffAmount(amountDifference);
        }
        paymentScheduleDetail.setAmount(paymentDetailAmount);
        paymentScheduleDetail.setDoubtfulDebtAmount(doubtfulDebtAmount);
        OBDal.getInstance().save(paymentScheduleDetail);
      }
      dao.getNewPaymentDetail(payment, paymentScheduleDetail, paymentDetailAmount,
          amountDifference, false, null, doFlush, null);
    }
    return assignedAmount;
  }

  public static FIN_Payment setFinancialTransactionAmountAndRate(VariablesSecureApp vars,
      FIN_Payment payment, BigDecimal _finTxnConvertRate, BigDecimal _finTxnAmount) {
    if (payment == null) {
      return payment;
    }

    BigDecimal paymentAmount = payment.getAmount();
    if (paymentAmount == null) {
      paymentAmount = BigDecimal.ZERO;
    }
    BigDecimal finTxnConvertRate = _finTxnConvertRate;
    if (finTxnConvertRate == null || finTxnConvertRate.compareTo(BigDecimal.ZERO) <= 0) {
      finTxnConvertRate = BigDecimal.ONE;
    }
    BigDecimal finTxnAmount = _finTxnAmount;
    if (finTxnAmount == null || finTxnAmount.compareTo(BigDecimal.ZERO) == 0) {
      finTxnAmount = paymentAmount.multiply(finTxnConvertRate);
    } else if (paymentAmount.compareTo(BigDecimal.ZERO) != 0) {
      // Correct exchange rate for rounding that occurs in UI
      finTxnConvertRate = finTxnAmount.divide(paymentAmount, MathContext.DECIMAL64);
      if (vars != null) {
        DecimalFormat generalQtyRelationFmt = Utility.getFormat(vars, "generalQtyEdition");
        finTxnConvertRate = finTxnConvertRate.setScale(
            generalQtyRelationFmt.getMaximumFractionDigits(), RoundingMode.HALF_UP);
      }
    }

    payment.setFinancialTransactionAmount(finTxnAmount);
    payment.setFinancialTransactionConvertRate(finTxnConvertRate);

    return payment;
  }

  public static FIN_Payment setFinancialTransactionAmountAndRate(FIN_Payment payment,
      BigDecimal finTxnConvertRate, BigDecimal finTxnAmount) {
    return setFinancialTransactionAmountAndRate(null, payment, finTxnConvertRate, finTxnAmount);
  }

  public static FIN_Payment createRefundPayment(ConnectionProvider conProvider,
      VariablesSecureApp vars, FIN_Payment payment, BigDecimal refundAmount) {
    return createRefundPayment(conProvider, vars, payment, refundAmount, null);
  }

  public static FIN_Payment createRefundPayment(ConnectionProvider conProvider,
      VariablesSecureApp vars, FIN_Payment payment, BigDecimal refundAmount,
      BigDecimal conversionRate) {
    dao = new AdvPaymentMngtDao();
    FIN_Payment refundPayment;
    if (payment.getFINPaymentDetailList().isEmpty())
      refundPayment = payment;
    else {
      refundPayment = (FIN_Payment) DalUtil.copy(payment, false);
      String strDescription = Utility.messageBD(conProvider, "APRM_RefundPayment",
          vars.getLanguage());
      strDescription += ": " + payment.getDocumentNo();
      refundPayment.setDescription(strDescription);
      refundPayment.setGeneratedCredit(BigDecimal.ZERO);
      final String strDocumentNo = FIN_Utility.getDocumentNo(payment.getOrganization(), payment
          .getDocumentType().getDocumentCategory(), "DocumentNo_FIN_Payment");
      refundPayment.setDocumentNo(strDocumentNo);
    }
    refundPayment.setProcessed(false);
    refundPayment.setStatus("RPAP");
    OBDal.getInstance().save(refundPayment);
    OBDal.getInstance().flush();
    refundPayment.setAmount(refundAmount);
    refundPayment.setUsedCredit(refundAmount.negate());

    setFinancialTransactionAmountAndRate(refundPayment, conversionRate, null);

    FIN_PaymentScheduleDetail refundScheduleDetail = dao.getNewPaymentScheduleDetail(
        payment.getOrganization(), refundAmount);
    dao.getNewPaymentDetail(refundPayment, refundScheduleDetail, refundAmount, BigDecimal.ZERO,
        true, null);

    return refundPayment;
  }

  /**
   * Adds new Details to the given Payment Proposal based on the List of Payment Schedule Details.
   * 
   * @param paymentProposal
   *          FIN_PaymentProposal where new Details are added.
   * @param paymentAmount
   *          Total amount to be paid.
   * @param selectedPaymentScheduleDetails
   *          List of FIN_PaymentScheduleDetail that needs to be added to the Payment Proposal.
   * @param selectedPaymentScheduleDetailAmounts
   *          HashMap with the Amount to be paid for each Scheduled Payment Detail.
   * @param writeOffAmt
   *          Total amount to be written off.
   */
  public static void savePaymentProposal(FIN_PaymentProposal paymentProposal,
      BigDecimal paymentAmount, List<FIN_PaymentScheduleDetail> selectedPaymentScheduleDetails,
      HashMap<String, BigDecimal> selectedPaymentScheduleDetailAmounts, BigDecimal writeOffAmt) {
    dao = new AdvPaymentMngtDao();
    paymentProposal.setAmount(paymentAmount);
    paymentProposal.setWriteoffAmount((writeOffAmt != null) ? writeOffAmt : BigDecimal.ZERO);
    BigDecimal convertRate = paymentProposal.getFinancialTransactionConvertRate();
    if (BigDecimal.ONE.equals(convertRate)) {
      paymentProposal.setFinancialTransactionAmount(paymentAmount);
    } else {
      Currency finAccountCurrency = paymentProposal.getAccount().getCurrency();
      BigDecimal finAccountTxnAmount = paymentAmount.multiply(convertRate);
      long faPrecision = finAccountCurrency.getStandardPrecision();
      finAccountTxnAmount = finAccountTxnAmount.setScale((int) faPrecision, RoundingMode.HALF_UP);

      paymentProposal.setFinancialTransactionAmount(finAccountTxnAmount);
    }

    for (FIN_PaymentScheduleDetail paymentScheduleDetail : selectedPaymentScheduleDetails) {
      BigDecimal detailWriteOffAmt = null;
      if (writeOffAmt != null)
        detailWriteOffAmt = paymentScheduleDetail.getAmount().subtract(
            selectedPaymentScheduleDetailAmounts.get(paymentScheduleDetail.getId()));

      dao.getNewPaymentProposalDetail(paymentProposal.getOrganization(), paymentProposal,
          paymentScheduleDetail,
          selectedPaymentScheduleDetailAmounts.get(paymentScheduleDetail.getId()),
          detailWriteOffAmt, null);
    }
  }

  /**
   * It adds to the Payment a new Payment Detail with the given GL Item and amount.
   * 
   * @param payment
   *          Payment where the new Payment Detail needs to be added.
   * @param glitemAmount
   *          Amount of the new Payment Detail.
   * @param glitem
   *          GLItem to be set in the new Payment Detail.
   * @param paymentId
   *          id to set in new entities
   */
  public static void saveGLItem(FIN_Payment payment, BigDecimal glitemAmount, GLItem glitem,
      String paymentId) {
    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done
    dao = new AdvPaymentMngtDao();
    OBContext.setAdminMode();
    try {
      FIN_PaymentScheduleDetail psd = dao.getNewPaymentScheduleDetail(payment.getOrganization(),
          glitemAmount, paymentId);
      FIN_PaymentDetail pd = dao.getNewPaymentDetail(payment, psd, glitemAmount, BigDecimal.ZERO,
          false, glitem, true, paymentId);
      pd.setFinPayment(payment);
      OBDal.getInstance().save(pd);
      OBDal.getInstance().save(payment);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void saveGLItem(FIN_Payment payment, BigDecimal glitemAmount, GLItem glitem) {
    saveGLItem(payment, glitemAmount, glitem, null);
  }

  /**
   * It adds to the Payment a new Payment Detail with the given GL Item, amount and accounting
   * dimensions
   * 
   * @param payment
   *          Payment where the new Payment Detail needs to be added.
   * @param glitemAmount
   *          Amount of the new Payment Detail.
   * @param glitem
   *          GLItem to be set in the new Payment Detail.
   * @param businessPartner
   *          accounting dimension
   * @param product
   *          accounting dimension
   * @param project
   *          accounting dimension
   * @param campaign
   *          accounting dimension
   * @param activity
   *          accounting dimension
   * @param salesRegion
   *          accounting dimension
   * @param costCenter
   *          accounting dimension
   * @param user1
   *          accounting dimension
   * @param user2
   *          accounting dimension
   */
  public static void saveGLItem(FIN_Payment payment, BigDecimal glitemAmount, GLItem glitem,
      BusinessPartner businessPartner, Product product, Project project, Campaign campaign,
      ABCActivity activity, SalesRegion salesRegion, Costcenter costCenter, UserDimension1 user1,
      UserDimension2 user2) {
    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done
    dao = new AdvPaymentMngtDao();
    OBContext.setAdminMode();
    try {
      FIN_PaymentScheduleDetail psd = dao.getNewPaymentScheduleDetail(payment.getOrganization(),
          glitemAmount, businessPartner, product, project, campaign, activity, salesRegion,
          costCenter, user1, user2);
      FIN_PaymentDetail pd = dao.getNewPaymentDetail(payment, psd, glitemAmount, BigDecimal.ZERO,
          false, glitem);
      pd.setFinPayment(payment);
      OBDal.getInstance().save(pd);
      OBDal.getInstance().save(payment);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * It adds to the Payment a new Payment Detail with the given GL Item, amount and accounting
   * dimensions
   * 
   * @param payment
   *          Payment where the new Payment Detail needs to be added.
   * @param glitemAmount
   *          Amount of the new Payment Detail.
   * @param glitem
   *          GLItem to be set in the new Payment Detail.
   * @param businessPartner
   *          accounting dimension
   * @param product
   *          accounting dimension
   * @param project
   *          accounting dimension
   * @param campaign
   *          accounting dimension
   * @param activity
   *          accounting dimension
   * @param salesRegion
   *          accounting dimension
   */
  public static void saveGLItem(FIN_Payment payment, BigDecimal glitemAmount, GLItem glitem,
      BusinessPartner businessPartner, Product product, Project project, Campaign campaign,
      ABCActivity activity, SalesRegion salesRegion) {
    saveGLItem(payment, glitemAmount, glitem, businessPartner, product, project, campaign,
        activity, salesRegion, null, null, null);
  }

  /**
   * Removes the Payment Detail from the Payment when the Detail is related to a GLItem
   * 
   * @param payment
   *          FIN_Payment that contains the Payment Detail.
   * @param paymentDetail
   *          FIN_PaymentDetail to be removed.
   */
  public static void removeGLItem(FIN_Payment payment, FIN_PaymentDetail paymentDetail) {
    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done
    dao = new AdvPaymentMngtDao();
    OBContext.setAdminMode();
    try {
      List<FIN_PaymentDetail> pdl = payment.getFINPaymentDetailList();
      if (paymentDetail != null) {
        pdl.remove(paymentDetail);
        OBDal.getInstance().remove(paymentDetail);
      } else {
        List<String> pdlIDs = new ArrayList<String>();
        for (FIN_PaymentDetail deletePaymentDetail : pdl)
          pdlIDs.add(deletePaymentDetail.getId());

        for (String pdlID : pdlIDs) {
          pdl.remove(dao.getObject(FIN_PaymentDetail.class, pdlID));
          OBDal.getInstance().remove(dao.getObject(FIN_PaymentDetail.class, pdlID));
        }
      }
      payment.setFINPaymentDetailList(pdl);
      OBDal.getInstance().save(payment);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * It adds to the scheduledPaymentDetails List the FIN_PaymentScheduleDetails given in the
   * strSelectedPaymentDetailsIds comma separated String of Id's that are not yet included on it.
   * 
   * @param scheduledPaymentDetails
   *          List of FIN_PaymentScheduleDetail.
   * @param strSelectedPaymentDetailsIds
   *          String of comma separated id's that needs to be included in the List if they are not
   *          present.
   * @return returns a List of FIN_PaymentScheduleDetail including all the Payment Schedule Details.
   */
  public static List<FIN_PaymentScheduleDetail> getSelectedPaymentDetails(
      List<FIN_PaymentScheduleDetail> scheduledPaymentDetails, String strSelectedPaymentDetailsIds) {
    final List<FIN_PaymentScheduleDetail> selectedScheduledPaymentDetails;
    if (scheduledPaymentDetails == null)
      selectedScheduledPaymentDetails = new ArrayList<FIN_PaymentScheduleDetail>();
    else
      selectedScheduledPaymentDetails = scheduledPaymentDetails;
    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done
    OBContext.setAdminMode();
    try {
      // selected scheduled payments list
      final List<FIN_PaymentScheduleDetail> tempSelectedScheduledPaymentDetails = FIN_Utility
          .getOBObjectList(FIN_PaymentScheduleDetail.class, strSelectedPaymentDetailsIds);
      for (FIN_PaymentScheduleDetail tempPaymentScheduleDetail : tempSelectedScheduledPaymentDetails) {
        if (!selectedScheduledPaymentDetails.contains(tempPaymentScheduleDetail))
          selectedScheduledPaymentDetails.add(tempPaymentScheduleDetail);

      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return selectedScheduledPaymentDetails;
  }

  /**
   * Creates a HashMap with the FIN_PaymentScheduleDetail id's and the amount gotten from the
   * Session.
   * 
   * The amounts are stored in Session like "inpPaymentAmount"+paymentScheduleDetail.Id
   * 
   * @param vars
   *          VariablseSecureApp with the session data.
   * @param selectedPaymentScheduleDetails
   *          List of FIN_PaymentScheduleDetails that need to be included in the HashMap.
   * @return A HashMap mapping the FIN_PaymentScheduleDetail's Id with the corresponding amount.
   */
  public static HashMap<String, BigDecimal> getSelectedPaymentDetailsAndAmount(
      VariablesSecureApp vars, List<FIN_PaymentScheduleDetail> selectedPaymentScheduleDetails)
      throws ServletException {
    return getSelectedBaseOBObjectAmount(vars, selectedPaymentScheduleDetails, "inpPaymentAmount");
  }

  /**
   * Creates a HashMap with the BaseOBObject id's and the amount gotten from the Session.
   * 
   * The amounts are stored in Session like "htmlElementId"+basobObject.Id
   * 
   * @param vars
   *          VariablseSecureApp with the session data.
   * @param selectedBaseOBObjects
   *          List of bobs that need to be included in the HashMap.
   * @return A HashMap mapping the Id with the corresponding amount.
   */
  public static <T extends BaseOBObject> HashMap<String, BigDecimal> getSelectedBaseOBObjectAmount(
      VariablesSecureApp vars, List<T> selectedBaseOBObjects, String htmlElementId)
      throws ServletException {
    HashMap<String, BigDecimal> selectedBaseOBObjectAmounts = new HashMap<String, BigDecimal>();

    for (final T o : selectedBaseOBObjects) {
      selectedBaseOBObjectAmounts.put((String) o.getId(),
          new BigDecimal(vars.getNumericParameter(htmlElementId + (String) o.getId(), "")));
    }
    return selectedBaseOBObjectAmounts;
  }

  private static List<FIN_PaymentScheduleDetail> getOrderedPaymentScheduleDetails(Set<String> psdSet) {
    OBCriteria<FIN_PaymentScheduleDetail> orderedPSDs = OBDal.getInstance().createCriteria(
        FIN_PaymentScheduleDetail.class);
    orderedPSDs.add(Restrictions.in(FIN_PaymentScheduleDetail.PROPERTY_ID, psdSet));
    orderedPSDs.addOrderBy(FIN_PaymentScheduleDetail.PROPERTY_AMOUNT, true);
    return orderedPSDs.list();
  }

  private static HashMap<String, BigDecimal> calculateAmounts(BigDecimal recordAmount,
      Set<String> psdSet) {
    BigDecimal remainingAmount = recordAmount;
    HashMap<String, BigDecimal> recordsAmounts = new HashMap<String, BigDecimal>();
    // PSD needs to be properly ordered to ensure negative amounts are processed first
    List<FIN_PaymentScheduleDetail> psds = getOrderedPaymentScheduleDetails(psdSet);
    for (FIN_PaymentScheduleDetail paymentScheduleDetail : psds) {
      BigDecimal outstandingAmount = paymentScheduleDetail.getAmount();
      // Manage negative amounts
      if ((remainingAmount.compareTo(BigDecimal.ZERO) > 0 && remainingAmount
          .compareTo(outstandingAmount) >= 0)
          || ((remainingAmount.compareTo(BigDecimal.ZERO) == -1 && outstandingAmount
              .compareTo(BigDecimal.ZERO) == -1) && (remainingAmount.compareTo(outstandingAmount) <= 0))) {
        recordsAmounts.put(paymentScheduleDetail.getId(), outstandingAmount);
        remainingAmount = remainingAmount.subtract(outstandingAmount);
      } else {
        recordsAmounts.put(paymentScheduleDetail.getId(), remainingAmount);
        if (psdSet.size() > 0) {
          remainingAmount = BigDecimal.ZERO;
        }
      }

    }
    return recordsAmounts;
  }

  /**
   * Builds a FieldProvider with a set of Payment Schedule Details based on the
   * selectedScheduledPaymentDetails and filteredScheduledPaymentDetails Lists. When the firstLoad
   * parameter is true the "paymentAmount" field is loaded from the corresponding Payment Proposal
   * Detail if it exists, when false it gets the amount from session.
   * 
   * @param vars
   *          VariablesSecureApp containing the Session data.
   * @param selectedScheduledPaymentDetails
   *          List of FIN_PaymentScheduleDetails that need to be selected by default.
   * @param filteredScheduledPaymentDetails
   *          List of FIN_PaymentScheduleDetails that need to be unselected by default.
   * @param firstLoad
   *          Boolean to set if the PaymentAmount is gotten from the PaymentProposal (true) or from
   *          Session (false)
   * @param paymentProposal
   *          PaymentProposal used to get the amount when firstLoad is true.
   * @return a FieldProvider object with all the given FIN_PaymentScheduleDetails.
   */
  public static FieldProvider[] getShownScheduledPaymentDetails(VariablesSecureApp vars,
      List<FIN_PaymentScheduleDetail> selectedScheduledPaymentDetails,
      List<FIN_PaymentScheduleDetail> filteredScheduledPaymentDetails, boolean firstLoad,
      FIN_PaymentProposal paymentProposal) throws ServletException {
    return getShownScheduledPaymentDetails(vars, selectedScheduledPaymentDetails,
        filteredScheduledPaymentDetails, firstLoad, paymentProposal, null);
  }

  public static FieldProvider[] getShownScheduledPaymentDetails(VariablesSecureApp vars,
      List<FIN_PaymentScheduleDetail> selectedScheduledPaymentDetails,
      List<FIN_PaymentScheduleDetail> filteredScheduledPaymentDetails, boolean firstLoad,
      FIN_PaymentProposal paymentProposal, String strSelectedPaymentDetails)
      throws ServletException {
    return getShownScheduledPaymentDetails(vars, selectedScheduledPaymentDetails,
        filteredScheduledPaymentDetails, firstLoad, paymentProposal, strSelectedPaymentDetails,
        false);
  }

  public static FieldProvider[] getShownScheduledPaymentDetails(VariablesSecureApp vars,
      List<FIN_PaymentScheduleDetail> selectedScheduledPaymentDetails,
      List<FIN_PaymentScheduleDetail> filteredScheduledPaymentDetails, boolean firstLoad,
      FIN_PaymentProposal paymentProposal, String strSelectedPaymentDetails,
      boolean showDoubtfulDebtAmount) throws ServletException {

    String strSelectedRecords = "";
    if (!"".equals(strSelectedPaymentDetails) && strSelectedPaymentDetails != null) {
      strSelectedRecords = strSelectedPaymentDetails;
      strSelectedRecords = strSelectedRecords.replace("(", "");
      strSelectedRecords = strSelectedRecords.replace(")", "");
    }
    dao = new AdvPaymentMngtDao();
    final List<FIN_PaymentScheduleDetail> shownScheduledPaymentDetails = new ArrayList<FIN_PaymentScheduleDetail>();
    shownScheduledPaymentDetails.addAll(selectedScheduledPaymentDetails);
    shownScheduledPaymentDetails.addAll(filteredScheduledPaymentDetails);
    FIN_PaymentScheduleDetail[] FIN_PaymentScheduleDetails = new FIN_PaymentScheduleDetail[0];
    FIN_PaymentScheduleDetails = shownScheduledPaymentDetails.toArray(FIN_PaymentScheduleDetails);
    FieldProvider[] data = FieldProviderFactory.getFieldProviderArray(shownScheduledPaymentDetails);
    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);
    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done
    OBContext.setAdminMode();
    try {
      for (int i = 0; i < data.length; i++) {
        String selectedId = (selectedScheduledPaymentDetails
            .contains(FIN_PaymentScheduleDetails[i])) ? FIN_PaymentScheduleDetails[i].getId() : "";
        // If selectedId belongs to a grouping selection calculate whether it should be selected or
        // not
        if (!"".equals(selectedId) && !"".equals(strSelectedPaymentDetails)
            && strSelectedPaymentDetails != null) {
          StringTokenizer records = new StringTokenizer(strSelectedRecords, "'");
          Set<String> recordSet = new LinkedHashSet<String>();
          while (records.hasMoreTokens()) {
            recordSet.add(records.nextToken());
          }
          if (recordSet.contains(selectedId)) {
            FieldProviderFactory.setField(data[i], "finSelectedPaymentDetailId", selectedId);
          } else {
            String selectedRecord = FIN_PaymentScheduleDetails[i].getId();
            // Find record which contains psdId
            Set<String> psdIdSet = new LinkedHashSet<String>();
            for (String record : recordSet) {
              if (record.contains(selectedId)) {
                selectedRecord = record;
                StringTokenizer st = new StringTokenizer(record, ",");
                while (st.hasMoreTokens()) {
                  psdIdSet.add(st.nextToken());
                }
              }
            }
            String psdAmount = vars.getNumericParameter("inpPaymentAmount" + selectedRecord, "");

            HashMap<String, BigDecimal> idsAmounts = calculateAmounts(new BigDecimal(psdAmount),
                psdIdSet);
            FieldProviderFactory.setField(data[i], "finSelectedPaymentDetailId", selectedId);
            FieldProviderFactory.setField(data[i], "paymentAmount", idsAmounts.get(selectedId)
                .toString());
          }
        }

        FieldProviderFactory
            .setField(data[i], "finSelectedPaymentDetailId", (selectedScheduledPaymentDetails
                .contains(FIN_PaymentScheduleDetails[i])) ? FIN_PaymentScheduleDetails[i].getId()
                : "");
        FieldProviderFactory.setField(data[i], "finScheduledPaymentDetailId",
            FIN_PaymentScheduleDetails[i].getId());
        if (FIN_PaymentScheduleDetails[i].getOrderPaymentSchedule() != null) {
          FieldProviderFactory.setField(data[i], "orderNr", FIN_PaymentScheduleDetails[i]
              .getOrderPaymentSchedule().getOrder().getDocumentNo());
          FieldProviderFactory.setField(data[i], "orderNrTrunc", FIN_PaymentScheduleDetails[i]
              .getOrderPaymentSchedule().getOrder().getDocumentNo());
          FieldProviderFactory.setField(data[i], "orderPaymentScheduleId",
              FIN_PaymentScheduleDetails[i].getOrderPaymentSchedule().getId());
        } else {
          FieldProviderFactory.setField(data[i], "orderNr", "");
          FieldProviderFactory.setField(data[i], "orderNrTrunc", "");
          FieldProviderFactory.setField(data[i], "orderPaymentScheduleId", "");
        }
        if (FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule() != null) {
          FIN_PaymentSchedule psd = FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule();
          OrganizationInformation orgInfo = OBDao.getActiveOBObjectList(psd.getOrganization(),
              Organization.PROPERTY_ORGANIZATIONINFORMATIONLIST) != null ? (OrganizationInformation) OBDao
              .getActiveOBObjectList(psd.getOrganization(),
                  Organization.PROPERTY_ORGANIZATIONINFORMATIONLIST).get(0) : null;
          if (!psd.getInvoice().isSalesTransaction() && orgInfo != null
              && orgInfo.getAPRMPaymentDescription().equals("Supplier Reference")) {
            // When the Organization of the Invoice sets that the Invoice Document No. is the
            // supplier's
            FieldProviderFactory.setField(data[i], "invoiceNr", FIN_PaymentScheduleDetails[i]
                .getInvoicePaymentSchedule().getInvoice().getOrderReference());
            FieldProviderFactory.setField(data[i], "invoiceNrTrunc", FIN_PaymentScheduleDetails[i]
                .getInvoicePaymentSchedule().getInvoice().getOrderReference());
          } else {
            // When the Organization of the Invoice sets that the Invoice Document No. is the
            // default
            // Invoice Number
            FieldProviderFactory.setField(data[i], "invoiceNr", FIN_PaymentScheduleDetails[i]
                .getInvoicePaymentSchedule().getInvoice().getDocumentNo());
            FieldProviderFactory.setField(data[i], "invoiceNrTrunc", FIN_PaymentScheduleDetails[i]
                .getInvoicePaymentSchedule().getInvoice().getDocumentNo());
          }
          FieldProviderFactory.setField(data[i], "invoicePaymentScheduleId",
              FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule().getId());
        } else {
          FieldProviderFactory.setField(data[i], "invoiceNr", "");
          FieldProviderFactory.setField(data[i], "invoiceNrTrunc", "");
          FieldProviderFactory.setField(data[i], "invoicePaymentScheduleId", "");
        }
        if (FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule() != null) {
          FieldProviderFactory.setField(
              data[i],
              "expectedDate",
              dateFormater.format(
                  FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule().getExpectedDate())
                  .toString());
          FieldProviderFactory.setField(
              data[i],
              "dueDate",
              dateFormater.format(
                  FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule().getDueDate())
                  .toString());
          FieldProviderFactory.setField(
              data[i],
              "transactionDate",
              dateFormater.format(
                  FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule().getInvoice()
                      .getInvoiceDate()).toString());
          FieldProviderFactory.setField(data[i], "invoicedAmount", FIN_PaymentScheduleDetails[i]
              .getInvoicePaymentSchedule().getInvoice().getGrandTotalAmount().toString());
          FieldProviderFactory.setField(data[i], "expectedAmount", FIN_PaymentScheduleDetails[i]
              .getInvoicePaymentSchedule().getAmount().toString());

          // Truncate Business Partner
          String businessPartner = FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule()
              .getInvoice().getBusinessPartner().getIdentifier();
          FieldProviderFactory.setField(data[i], "businessPartnerId", FIN_PaymentScheduleDetails[i]
              .getInvoicePaymentSchedule().getInvoice().getBusinessPartner().getId());
          String truncateBusinessPartner = (businessPartner.length() > 18) ? businessPartner
              .substring(0, 15).concat("...").toString() : businessPartner;
          FieldProviderFactory.setField(data[i], "businessPartnerName",
              (businessPartner.length() > 18) ? businessPartner : "");
          FieldProviderFactory.setField(data[i], "businessPartnerNameTrunc",
              truncateBusinessPartner);

          // Truncate Payment Method
          String paymentMethodName = FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule()
              .getFinPaymentmethod().getName();
          String truncatePaymentMethodName = (paymentMethodName.length() > 18) ? paymentMethodName
              .substring(0, 15).concat("...").toString() : paymentMethodName;
          FieldProviderFactory.setField(data[i], "paymentMethodName",
              (paymentMethodName.length() > 18) ? paymentMethodName : "");
          FieldProviderFactory.setField(data[i], "paymentMethodNameTrunc",
              truncatePaymentMethodName);

          if (FIN_PaymentScheduleDetails[i].getInvoicePaymentSchedule().getFINPaymentPriority() != null) {
            FieldProviderFactory.setField(data[i], "gridLineColor", FIN_PaymentScheduleDetails[i]
                .getInvoicePaymentSchedule().getFINPaymentPriority().getColor());
          }
        } else {
          FieldProviderFactory.setField(
              data[i],
              "expectedDate",
              dateFormater.format(
                  FIN_PaymentScheduleDetails[i].getOrderPaymentSchedule().getExpectedDate())
                  .toString());
          FieldProviderFactory.setField(
              data[i],
              "dueDate",
              dateFormater.format(
                  FIN_PaymentScheduleDetails[i].getOrderPaymentSchedule().getDueDate()).toString());
          FieldProviderFactory.setField(
              data[i],
              "transactionDate",
              dateFormater
                  .format(
                      FIN_PaymentScheduleDetails[i].getOrderPaymentSchedule().getOrder()
                          .getOrderDate()).toString());
          FieldProviderFactory.setField(data[i], "invoicedAmount", "");
          FieldProviderFactory.setField(data[i], "expectedAmount", FIN_PaymentScheduleDetails[i]
              .getOrderPaymentSchedule().getAmount().toString());

          // Truncate Business Partner
          String businessPartner = FIN_PaymentScheduleDetails[i].getOrderPaymentSchedule()
              .getOrder().getBusinessPartner().getIdentifier();
          FieldProviderFactory.setField(data[i], "businessPartnerId", FIN_PaymentScheduleDetails[i]
              .getOrderPaymentSchedule().getOrder().getBusinessPartner().getId());
          String truncateBusinessPartner = (businessPartner.length() > 18) ? businessPartner
              .substring(0, 15).concat("...").toString() : businessPartner;
          FieldProviderFactory.setField(data[i], "businessPartnerName",
              (businessPartner.length() > 18) ? businessPartner : "");
          FieldProviderFactory.setField(data[i], "businessPartnerNameTrunc",
              truncateBusinessPartner);

          // Truncate Payment Method
          String paymentMethodName = FIN_PaymentScheduleDetails[i].getOrderPaymentSchedule()
              .getFinPaymentmethod().getName();
          String truncatePaymentMethodName = (paymentMethodName.length() > 18) ? paymentMethodName
              .substring(0, 15).concat("...").toString() : paymentMethodName;
          FieldProviderFactory.setField(data[i], "paymentMethodName",
              (paymentMethodName.length() > 18) ? paymentMethodName : "");
          FieldProviderFactory.setField(data[i], "paymentMethodNameTrunc",
              truncatePaymentMethodName);

          if (FIN_PaymentScheduleDetails[i].getOrderPaymentSchedule().getFINPaymentPriority() != null) {
            FieldProviderFactory.setField(data[i], "gridLineColor", FIN_PaymentScheduleDetails[i]
                .getOrderPaymentSchedule().getFINPaymentPriority().getColor());
          }
        }
        FieldProviderFactory.setField(data[i], "outstandingAmount", FIN_PaymentScheduleDetails[i]
            .getAmount().toString());
        FieldProviderFactory.setField(data[i], "doubtfulDebtAmount", FIN_PaymentScheduleDetails[i]
            .getDoubtfulDebtAmount().toString());
        FieldProviderFactory.setField(data[i], "displayDoubtfulDebt", showDoubtfulDebtAmount ? ""
            : "display: none;");

        String strPaymentAmt = "";
        String strDifference = "";
        if (firstLoad && (selectedScheduledPaymentDetails.contains(FIN_PaymentScheduleDetails[i]))
            && paymentProposal != null) {
          strPaymentAmt = dao.getPaymentProposalDetailAmount(FIN_PaymentScheduleDetails[i],
              paymentProposal);
        } else {
          strPaymentAmt = vars.getNumericParameter("inpPaymentAmount"
              + FIN_PaymentScheduleDetails[i].getId(), "");
        }
        if (!"".equals(strPaymentAmt)) {
          strDifference = FIN_PaymentScheduleDetails[i].getAmount()
              .subtract(new BigDecimal(strPaymentAmt)).toString();
        }
        if (data[i].getField("paymentAmount") == null
            || "".equals(data[i].getField("paymentAmount"))) {
          FieldProviderFactory.setField(data[i], "paymentAmount", strPaymentAmt);
        }
        FieldProviderFactory.setField(data[i], "difference", strDifference);
        FieldProviderFactory.setField(data[i], "rownum", String.valueOf(i));

      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return data;
  }

  /**
   * Returns a List of FIN_PaymentScheduleDetails related to the Proposal Details of the given
   * Payment Proposal.
   * 
   * @param paymentProposal
   */
  public static List<FIN_PaymentScheduleDetail> getSelectedPendingPaymentsFromProposal(
      FIN_PaymentProposal paymentProposal) {
    List<FIN_PaymentScheduleDetail> existingPaymentScheduleDetail = new ArrayList<FIN_PaymentScheduleDetail>();
    for (FIN_PaymentPropDetail proposalDetail : paymentProposal.getFINPaymentPropDetailList())
      existingPaymentScheduleDetail.add(proposalDetail.getFINPaymentScheduledetail());

    return existingPaymentScheduleDetail;
  }

  /**
   * This method groups several payment schedule details by {PaymentDetails, OrderPaymenSchedule,
   * InvoicePaymentSchedule}.
   * 
   * @param psd
   *          Payment Schedule Detail base. The amount will be updated here.
   */
  public static void mergePaymentScheduleDetails(FIN_PaymentScheduleDetail psd) {
    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done

    OBContext.setAdminMode();
    try {
      OBCriteria<FIN_PaymentScheduleDetail> psdFilter = OBDal.getInstance().createCriteria(
          FIN_PaymentScheduleDetail.class);
      psdFilter.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_CLIENT, psd.getClient()));
      psdFilter.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_ORGANIZATION,
          psd.getOrganization()));
      psdFilter.add(Restrictions.isNull(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS));
      if (psd.getOrderPaymentSchedule() == null) {
        psdFilter.add(Restrictions.isNull(FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE));
      } else {
        psdFilter.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE,
            psd.getOrderPaymentSchedule()));
      }
      if (psd.getInvoicePaymentSchedule() == null) {
        psdFilter.add(Restrictions
            .isNull(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE));
      } else {
        psdFilter.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
            psd.getInvoicePaymentSchedule()));
      }

      // Update amount and remove payment schedule detail
      final List<String> removedPDSIds = new ArrayList<String>();
      for (FIN_PaymentScheduleDetail psdToRemove : psdFilter.list()) {
        psd.setAmount(psd.getAmount().add(psdToRemove.getAmount()));
        psd.setDoubtfulDebtAmount(psd.getDoubtfulDebtAmount().add(
            psdToRemove.getDoubtfulDebtAmount()));
        // TODO: Set 0 as default value for writeoffamt column in FIN_Payment_ScheduleDetail table
        BigDecimal sum1 = (psd.getWriteoffAmount() == null) ? BigDecimal.ZERO : psd
            .getWriteoffAmount();
        BigDecimal sum2 = (psdToRemove.getWriteoffAmount() == null) ? BigDecimal.ZERO : psdToRemove
            .getWriteoffAmount();
        psd.setWriteoffAmount(sum1.add(sum2));

        OBDal.getInstance().save(psdToRemove);
        removedPDSIds.add(psdToRemove.getId());
      }

      for (String pdToRm : removedPDSIds) {
        FIN_PaymentScheduleDetail psdToRemove = OBDal.getInstance().get(
            FIN_PaymentScheduleDetail.class, pdToRm);
        if (psdToRemove.getInvoicePaymentSchedule() != null) {
          psdToRemove.getInvoicePaymentSchedule()
              .getFINPaymentScheduleDetailInvoicePaymentScheduleList().remove(psdToRemove);
          OBDal.getInstance().save(psdToRemove.getInvoicePaymentSchedule());
        }
        if (psdToRemove.getOrderPaymentSchedule() != null) {
          psdToRemove.getOrderPaymentSchedule()
              .getFINPaymentScheduleDetailOrderPaymentScheduleList().remove(psdToRemove);
          OBDal.getInstance().save(psdToRemove.getOrderPaymentSchedule());
        }
        OBDal.getInstance().remove(psdToRemove);
      }

      psd.setAmount(psd.getAmount().add(
          (psd.getWriteoffAmount() == null) ? BigDecimal.ZERO : psd.getWriteoffAmount()));
      psd.setWriteoffAmount(BigDecimal.ZERO);
      psd.setPaymentDetails(null);
      OBDal.getInstance().save(psd);
      OBDal.getInstance().flush();
      OBDal.getInstance().getSession().refresh(psd);

    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Update Payment Schedule amounts with the amount of the Payment Schedule Detail or Payment
   * Detail. Useful when paying orders
   * 
   * @param paymentSchedule
   *          Payment Schedule to be updated
   * @param amount
   *          Amount of the Payment Schedule Detail or Payment Detail
   * @param writeOffAmount
   *          Write off amount, null or 0 if not applicable.
   * 
   * @deprecated This method doesn't support Cash VAT flow, so it's better to use
   *             {@link #updatePaymentDetail(FIN_PaymentScheduleDetail, FIN_Payment, BigDecimal, boolean)}
   */
  @Deprecated
  public static void updatePaymentScheduleAmounts(FIN_PaymentSchedule paymentSchedule,
      BigDecimal amount, BigDecimal writeOffAmount) {
    updatePaymentScheduleAmounts(null, paymentSchedule, amount, writeOffAmount);
  }

  /**
   * Update Payment Schedule amounts with the amount of the Payment Schedule Detail or Payment
   * Detail. Useful when paying invoices. It supports Invoices with Cash VAT, creating the records
   * into the Cash VAT management table (InvoiceTaxCashVAT)
   * 
   * @param paymentDetail
   *          payment
   * @param paymentSchedule
   *          Payment Schedule to be updated
   * @param amount
   *          Amount of the Payment Schedule Detail or Payment Detail
   * @param writeOffAmount
   *          Write off amount, null or 0 if not applicable.
   */
  public static void updatePaymentScheduleAmounts(FIN_PaymentDetail paymentDetail,
      FIN_PaymentSchedule paymentSchedule, BigDecimal amount, BigDecimal writeOffAmount) {
    paymentSchedule.setPaidAmount(paymentSchedule.getPaidAmount().add(amount));
    paymentSchedule.setOutstandingAmount(paymentSchedule.getOutstandingAmount().subtract(amount));
    if (writeOffAmount != null && writeOffAmount.compareTo(BigDecimal.ZERO) != 0) {
      paymentSchedule.setPaidAmount(paymentSchedule.getPaidAmount().add(writeOffAmount));
      paymentSchedule.setOutstandingAmount(paymentSchedule.getOutstandingAmount().subtract(
          writeOffAmount));
    }
    OBDal.getInstance().save(paymentSchedule);
    CashVATUtil.createInvoiceTaxCashVAT(paymentDetail, paymentSchedule, amount.add(writeOffAmount));
    if (paymentSchedule.getInvoice() != null) {
      updateInvoicePaymentMonitor(paymentSchedule, amount, writeOffAmount);
    }
  }

  /**
   * Method used to update the payment monitor based on the payment made by the user.
   * 
   * @param invoicePaymentSchedule
   * @param amount
   *          Amount of the transaction.
   * @param writeOffAmount
   *          Amount that has been wrote off.
   */
  private static void updateInvoicePaymentMonitor(FIN_PaymentSchedule invoicePaymentSchedule,
      BigDecimal amount, BigDecimal writeOffAmount) {
    Invoice invoice = invoicePaymentSchedule.getInvoice();
    Date dueDate = invoicePaymentSchedule.getDueDate();
    boolean isDueDateFlag = dueDate.compareTo(new Date()) <= 0;
    invoice.setTotalPaid(invoice.getTotalPaid().add(amount));
    invoice.setLastCalculatedOnDate(new Date());
    invoice.setOutstandingAmount(invoice.getOutstandingAmount().subtract(amount));
    if (isDueDateFlag)
      invoice.setDueAmount(invoice.getDueAmount().subtract(amount));
    if (writeOffAmount != null && writeOffAmount.compareTo(BigDecimal.ZERO) != 0) {
      invoice.setTotalPaid(invoice.getTotalPaid().add(writeOffAmount));
      invoice.setOutstandingAmount(invoice.getOutstandingAmount().subtract(writeOffAmount));
      if (isDueDateFlag)
        invoice.setDueAmount(invoice.getDueAmount().subtract(writeOffAmount));
    }

    if (0 == invoice.getOutstandingAmount().compareTo(BigDecimal.ZERO)) {
      Date finalSettlementDate = getFinalSettlementDate(invoice);
      // If date is null invoice amount = 0 then nothing to set
      if (finalSettlementDate != null) {
        invoice.setFinalSettlementDate(finalSettlementDate);
        invoice.setDaysSalesOutstanding(FIN_Utility.getDaysBetween(invoice.getInvoiceDate(),
            finalSettlementDate));
      }
      invoice.setPaymentComplete(true);
    } else {
      invoice.setPaymentComplete(false);
      invoice.setFinalSettlementDate(null);
    }
    List<FIN_PaymentSchedule> paymentSchedList = invoice.getFINPaymentScheduleList();
    Date firstDueDate = null;
    for (FIN_PaymentSchedule paymentSchedule : paymentSchedList) {
      if (paymentSchedule.getOutstandingAmount().compareTo(BigDecimal.ZERO) > 0
          && (firstDueDate == null || firstDueDate.after(paymentSchedule.getDueDate())))
        firstDueDate = paymentSchedule.getDueDate();
    }

    BigDecimal overdueAmount = calculateOverdueAmount(invoicePaymentSchedule);
    invoice.setPercentageOverdue(overdueAmount.multiply(new BigDecimal("100"))
        .divide(invoice.getGrandTotalAmount(), 2, RoundingMode.HALF_UP).longValue());

    if (firstDueDate != null)
      invoice.setDaysTillDue(FIN_Utility.getDaysToDue(firstDueDate));
    else
      invoice.setDaysTillDue(0L);
    OBDal.getInstance().save(invoice);
  }

  private static BigDecimal calculateOverdueAmount(FIN_PaymentSchedule invoicePaymentSchedule) {
    Invoice invoice = invoicePaymentSchedule.getInvoice();
    BigDecimal overdueOriginal = BigDecimal.ZERO;
    FIN_PaymentScheduleDetail currentPSD = getLastCreatedPaymentScheduleDetail(invoicePaymentSchedule);
    for (FIN_PaymentSchedule paymentSchedule : invoice.getFINPaymentScheduleList()) {
      Date paymentDueDate = paymentSchedule.getDueDate();
      for (FIN_PaymentScheduleDetail psd : paymentSchedule
          .getFINPaymentScheduleDetailInvoicePaymentScheduleList()) {
        if (!psd.isCanceled() && psd.getPaymentDetails() != null
            && (psd.isInvoicePaid() || currentPSD.getId().equals(psd.getId()))) {
          Date paymentDate = psd.getPaymentDetails().getFinPayment().getPaymentDate();
          if (paymentDate.after(paymentDueDate)) {
            overdueOriginal = overdueOriginal.add(psd.getAmount());
          }
        }
      }

    }
    return overdueOriginal;
  }

  private static FIN_PaymentScheduleDetail getLastCreatedPaymentScheduleDetail(
      FIN_PaymentSchedule invoicePaymentSchedule) {
    final OBCriteria<FIN_PaymentScheduleDetail> obc = OBDal.getInstance().createCriteria(
        FIN_PaymentScheduleDetail.class);
    OBContext.setAdminMode();
    try {
      obc.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
          invoicePaymentSchedule));
      obc.addOrderBy(FIN_PaymentScheduleDetail.PROPERTY_CREATIONDATE, false);
      obc.setMaxResults(1);
      return (FIN_PaymentScheduleDetail) obc.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /**
   * Returns the date in which last payment for this invoice took place
   */
  private static Date getFinalSettlementDate(Invoice invoice) {
    final OBCriteria<FIN_PaymentSchedInvV> obc = OBDal.getInstance().createCriteria(
        FIN_PaymentSchedInvV.class);
    OBContext.setAdminMode();
    try {
      obc.add(Restrictions.eq(FIN_PaymentSchedInvV.PROPERTY_INVOICE, invoice));
      obc.setProjection(Projections.max(FIN_PaymentSchedInvV.PROPERTY_LASTPAYMENT));
      return (Date) obc.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns true if a financial account transactions has to be automatically triggered after
   * payment is processed.
   * 
   * @param payment
   * @return Returns true if a financial account transactions has to be automatically triggered
   *         after payment is processed.
   */
  public static Boolean isForcedFinancialAccountTransaction(FIN_Payment payment) {
    OBCriteria<FinAccPaymentMethod> psdFilter = OBDal.getInstance().createCriteria(
        FinAccPaymentMethod.class);
    psdFilter.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, payment.getAccount()));
    psdFilter.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD,
        payment.getPaymentMethod()));
    for (FinAccPaymentMethod paymentMethod : psdFilter.list()) {
      return payment.isReceipt() ? paymentMethod.isAutomaticDeposit() : paymentMethod
          .isAutomaticWithdrawn();
    }
    return false;
  }

  /**
   * Method used to get a list of payments identifiers associated to a payment proposal
   * 
   * @param paymentProposal
   * @return List of payment identifiers
   */
  @SuppressWarnings("unchecked")
  public static List<String> getPaymentFromPaymentProposal(FIN_PaymentProposal paymentProposal) {
    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done
    OBContext.setAdminMode();
    try {
      StringBuilder hql = new StringBuilder();
      final Session session = OBDal.getInstance().getSession();
      hql.append("SELECT distinct(p." + FIN_Payment.PROPERTY_ID + ") ");
      hql.append("FROM " + FIN_PaymentPropDetail.TABLE_NAME + " as ppd ");
      hql.append("inner join ppd." + FIN_PaymentPropDetail.PROPERTY_FINPAYMENTSCHEDULEDETAIL
          + " as psd ");
      hql.append("inner join psd." + FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS + " as pd ");
      hql.append("inner join pd." + FIN_PaymentDetail.PROPERTY_FINPAYMENT + " as p ");
      hql.append("WHERE ppd." + FIN_PaymentPropDetail.PROPERTY_FINPAYMENTPROPOSAL + "."
          + FIN_PaymentProposal.PROPERTY_ID + "= :paymentProposalId");
      final Query obqPay = session.createQuery(hql.toString());
      obqPay.setParameter("paymentProposalId", paymentProposal.getId());

      return obqPay.list();

    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * It calls the PAyment Process for the given payment and action.
   * 
   * @param vars
   *          VariablesSecureApp with the session data.
   * @param conn
   *          ConnectionProvider with the connection being used.
   * @param strAction
   *          String with the action of the process. {P, D, R}
   * @param payment
   *          FIN_Payment that needs to be processed.
   * @return a OBError with the result message of the process.
   * @throws Exception
   */
  public static OBError processPayment(VariablesSecureApp vars, ConnectionProvider conn,
      String strAction, FIN_Payment payment) throws Exception {
    OBError myMessage = processPayment(vars, conn, strAction, payment, null, null);
    return myMessage;
  }

  /**
   * It calls the PAyment Process for the given payment, action and origin.
   * 
   * @param vars
   *          VariablesSecureApp with the session data.
   * @param conn
   *          ConnectionProvider with the connection being used.
   * @param strAction
   *          String with the action of the process. {P, D, R}
   * @param payment
   *          FIN_Payment that needs to be processed.
   * @param comingFrom
   *          Origin where the process is invoked
   * @return a OBError with the result message of the process.
   * @throws Exception
   */
  public static OBError processPayment(VariablesSecureApp vars, ConnectionProvider conn,
      String strAction, FIN_Payment payment, String comingFrom) throws Exception {
    OBError myMessage = processPayment(vars, conn, strAction, payment, comingFrom, null);
    return myMessage;
  }

  /**
   * It calls the PAyment Process for the given payment, action and origin.
   * 
   * @param vars
   *          VariablesSecureApp with the session data.
   * @param conn
   *          ConnectionProvider with the connection being used.
   * @param strAction
   *          String with the action of the process. {P, D, R}
   * @param payment
   *          FIN_Payment that needs to be processed.
   * @param comingFrom
   *          Origin where the process is invoked
   * @param selectedCreditLineIds
   *          Id's of selected lines in Credit to Use grid
   * @return a OBError with the result message of the process.
   * @throws Exception
   */
  public static OBError processPayment(VariablesSecureApp vars, ConnectionProvider conn,
      String strAction, FIN_Payment payment, String comingFrom, String selectedCreditLineIds)
      throws Exception {
    ProcessBundle pb = new ProcessBundle("6255BE488882480599C81284B70CD9B3", vars).init(conn);
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("action", strAction);
    parameters.put("Fin_Payment_ID", payment.getId());
    parameters.put("comingFrom", comingFrom);
    parameters.put("selectedCreditLineIds", selectedCreditLineIds);
    pb.setParams(parameters);
    OBError myMessage = null;
    new FIN_PaymentProcess().execute(pb);
    myMessage = (OBError) pb.getResult();
    return myMessage;
  }

  /**
   * It calls the Payment Process using the given ProcessBundle
   * 
   * @param pb
   *          ProcessBundle already created and initialized. This improves the performance when
   *          calling this method in a loop
   * @param payment
   *          FIN_Payment that needs to be processed.
   * @param comingFrom
   *          Origin where the process is invoked
   * @param selectedCreditLineIds
   *          Id's of selected lines in Credit to Use grid
   * @param doFlush
   *          Force to flush inside the method
   * @return a OBError with the result message of the process.
   * @throws Exception
   */
  public static OBError processPayment(ProcessBundle pb, String strAction, FIN_Payment payment,
      String comingFrom, String selectedCreditLineIds, boolean doFlush) throws Exception {
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("action", strAction);
    parameters.put("Fin_Payment_ID", payment.getId());
    parameters.put("comingFrom", comingFrom);
    parameters.put("selectedCreditLineIds", selectedCreditLineIds);
    parameters.put("doFlush", doFlush);
    pb.setParams(parameters);
    OBError myMessage = null;
    new FIN_PaymentProcess().execute(pb);
    myMessage = (OBError) pb.getResult();
    return myMessage;
  }

  /**
   * It calls the Payment Proposal Process for the given payment proposal and action.
   * 
   * @param vars
   *          VariablesSecureApp with the session data.
   * @param conn
   *          ConnectionProvider with the connection being used.
   * @param strProcessProposalAction
   *          String with the action of the process. {GSP, RE}
   * @param strFinPaymentProposalId
   *          String with FIN_PaymentProposal Id to be processed.
   * @return a OBError with the result message of the process.
   * @throws Exception
   */
  public static OBError processPaymentProposal(VariablesSecureApp vars, ConnectionProvider conn,
      String strProcessProposalAction, String strFinPaymentProposalId) throws Exception {
    ProcessBundle pb = new ProcessBundle("D16966FBF9604A3D91A50DC83C6EA8E3", vars).init(conn);
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("processProposalAction", strProcessProposalAction);
    parameters.put("Fin_Payment_Proposal_ID", strFinPaymentProposalId);
    pb.setParams(parameters);
    OBError myMessage = null;
    new FIN_PaymentProposalProcess().execute(pb);
    myMessage = (OBError) pb.getResult();
    return myMessage;
  }

  /**
   * It calls the Bank Statement Process for the given bank statement and action.
   * 
   * @param vars
   *          VariablesSecureApp with the session data.
   * @param conn
   *          ConnectionProvider with the connection being used.
   * @param strBankStatementAction
   *          String with the action of the process. {P, R}
   * @param strBankStatementId
   *          String with FIN_BankStatement Id to be processed.
   * @return a OBError with the result message of the process.
   * @throws Exception
   */
  public static OBError processBankStatement(VariablesSecureApp vars, ConnectionProvider conn,
      String strBankStatementAction, String strBankStatementId) throws Exception {
    ProcessBundle pb = new ProcessBundle("58A9261BACEF45DDA526F29D8557272D", vars).init(conn);
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("action", strBankStatementAction);
    parameters.put("FIN_Bankstatement_ID", strBankStatementId);
    pb.setParams(parameters);
    OBError myMessage = null;
    new FIN_BankStatementProcess().execute(pb);
    myMessage = (OBError) pb.getResult();
    return myMessage;
  }

  public static List<FIN_PaymentScheduleDetail> getOutstandingPSDs(
      FIN_PaymentScheduleDetail paymentScheduleDetail) {
    OBContext.setAdminMode();
    try {
      OBCriteria<FIN_PaymentScheduleDetail> obc = OBDal.getInstance().createCriteria(
          FIN_PaymentScheduleDetail.class);
      obc.add(Restrictions.isNull(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS));
      if (paymentScheduleDetail.getInvoicePaymentSchedule() != null) {
        obc.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
            paymentScheduleDetail.getInvoicePaymentSchedule()));
      }
      if (paymentScheduleDetail.getOrderPaymentSchedule() != null) {
        obc.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE,
            paymentScheduleDetail.getOrderPaymentSchedule()));
      }
      return obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static List<FIN_PaymentScheduleDetail> getGLItemScheduleDetails(FIN_Payment payment) {
    if (payment.getFINPaymentDetailList().size() > 0) {
      OBContext.setAdminMode();
      try {
        OBCriteria<FIN_PaymentScheduleDetail> selectedGLItems = OBDal.getInstance().createCriteria(
            FIN_PaymentScheduleDetail.class);
        selectedGLItems.createAlias(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS, "pd");
        selectedGLItems.add(Restrictions.in(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS,
            payment.getFINPaymentDetailList()));
        selectedGLItems.add(Restrictions.isNotNull("pd." + FIN_PaymentDetail.PROPERTY_GLITEM));
        return selectedGLItems.list();
      } finally {
        OBContext.restorePreviousMode();
      }
    } else {
      return new ArrayList<FIN_PaymentScheduleDetail>();
    }
  }

  /**
   * Calculates the resultant doubtful debt amount. Used when editing payment schedule detail amount
   * to be collected.
   * 
   * @param scheduleDetailsTotalAmount
   *          Payment Schedule Detail amount.
   * @param paymentAmount
   *          Amount selected to be collected. Always less or equal than scheduleDetailAmount.
   * @param doubtfulDebtTotalAmount
   *          Payment Schedule Detail doubtFulDebt amount.
   * @return resultant doubtful debt amount. Zero if no doubtful debt amount was present.
   */
  private static BigDecimal getDoubtFulDebtAmount(BigDecimal scheduleDetailsTotalAmount,
      BigDecimal paymentAmount, BigDecimal doubtfulDebtTotalAmount) {
    BigDecimal calculatedDoubtFulDebtAmount = BigDecimal.ZERO;
    if (doubtfulDebtTotalAmount.compareTo(BigDecimal.ZERO) == 0) {
      return calculatedDoubtFulDebtAmount;
    }
    calculatedDoubtFulDebtAmount = paymentAmount.subtract(scheduleDetailsTotalAmount
        .subtract(doubtfulDebtTotalAmount));
    // There can not be negative Doubtful Debt Amounts. If it is negative, set it to Zero as the
    // other Payment Schedule Detail will compensate it.
    if (calculatedDoubtFulDebtAmount.signum() > 0) {
      return calculatedDoubtFulDebtAmount;
    }
    return BigDecimal.ZERO;
  }
}
