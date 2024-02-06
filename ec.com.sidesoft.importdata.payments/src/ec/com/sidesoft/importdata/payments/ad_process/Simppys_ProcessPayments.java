package ec.com.sidesoft.importdata.payments.ad_process;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.importdata.payments.Simppys_PaymentDataUpload;
import ec.com.sidesoft.importdata.payments.Simppys_PaymentDetail;
import ec.com.sidesoft.importdata.payments.utils.Simppys_Helper;

public class Simppys_ProcessPayments extends DalBaseProcess {
    private final Logger logger = Logger.getLogger(Simppys_ProcessPayments.class);

    @Override
    public void doExecute(ProcessBundle bundle) throws Exception {
        OBError msg = new OBError();
        ConnectionProvider conn = new DalConnectionProvider(false);

        try {
            OBContext.setAdminMode(true);
            logger.info("Begin ProcessPayments");

            String id = (String) bundle.getParams().get("Simppys_PaymentDataUpload_ID");
            Simppys_PaymentDataUpload paymentDataUpload = OBDal.getInstance().get(Simppys_PaymentDataUpload.class, id);

            process(conn, paymentDataUpload);

            OBDal.getInstance().commitAndClose();

            msg.setType("Success");
            msg.setTitle(OBMessageUtils.messageBD("Success"));
        } catch (final Exception e) {
            OBDal.getInstance().rollbackAndClose();
            String message = Simppys_Helper.getErrorMessage(logger, e);
            logger.error(message);

            msg.setTitle(OBMessageUtils.messageBD("Error"));
            msg.setType("Error");
            msg.setMessage(message);
        } finally {
            bundle.setResult(msg);
            OBContext.restorePreviousMode();
            logger.info("Finish ProcessPayments");
        }
    }

    private void process(ConnectionProvider conn, Simppys_PaymentDataUpload paymentDataUpload) throws Exception {
        OBCriteria<Simppys_PaymentDetail> qPDetail = OBDal.getInstance().createCriteria(Simppys_PaymentDetail.class);
        qPDetail.add(Restrictions.eq(Simppys_PaymentDetail.PROPERTY_ACTIVE, true));
        qPDetail.add(Restrictions.eq(Simppys_PaymentDetail.PROPERTY_SIMPPYSPAYMENTDATAUPLOAD, paymentDataUpload));
        qPDetail.add(Restrictions.eq(Simppys_PaymentDetail.PROPERTY_PROCESSED, false));

        for (Simppys_PaymentDetail pDetail : qPDetail.list()) {
            try {
                if (pDetail.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new OBException(
                            "Valor del cobro/pago (" + pDetail.getAmount() + ") debe ser mayor a 0.0");
                }

                AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
                List<FIN_PaymentScheduleDetail> scheduleDetails = dao
                        .getInvoicePendingScheduledPaymentDetails(pDetail.getInvoice());

                HashMap<String, BigDecimal> paidAmount = new HashMap<String, BigDecimal>();

                BigDecimal amount = new BigDecimal(pDetail.getAmount().toString());
                BigDecimal pending = pDetail.getInvoice().getOutstandingAmount();
                String pDetailDueDate = Simppys_Helper.getDateString(pDetail.getDueDate());
                if (StringUtils.isEmpty(pDetailDueDate)) {
                    for (int i = 0; i < scheduleDetails.size(); i++) {
                        FIN_PaymentScheduleDetail sDetail = scheduleDetails.get(i);
                        if (amount.compareTo(sDetail.getAmount()) <= 0) {
                            paidAmount.put(sDetail.getId(), amount);
                            amount = BigDecimal.ZERO;
                        } else {
                            paidAmount.put(sDetail.getId(), sDetail.getAmount());
                            amount = amount.subtract(sDetail.getAmount());
                        }

                        if (amount.compareTo(BigDecimal.ZERO) == 0) {
                            i = scheduleDetails.size();
                        }
                    }

                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        throw new OBException(
                                "Valor del cobro/pago (" + pDetail.getAmount()
                                        + ") excede el total pendiente de la factura (" + pending + ")");
                    }
                } else {
                    boolean found = false;
                    for (int i = 0; i < scheduleDetails.size(); i++) {
                        FIN_PaymentScheduleDetail sDetail = scheduleDetails.get(i);
                        FIN_PaymentSchedule pSchedule = sDetail.getInvoicePaymentSchedule();
                        String pScheduleDueDate = Simppys_Helper.getDateString(pSchedule.getDueDate());
                        if (pDetailDueDate.equals(pScheduleDueDate)) {
                            if (amount.compareTo(sDetail.getAmount()) > 0) {
                                throw new OBException(
                                        "Valor del cobro/pago (" + amount + ") excede el total pendiente de la cuota "
                                                + pDetailDueDate + " (" + sDetail.getAmount() + ")");
                            }
                            paidAmount.put(sDetail.getId(), amount);
                            found = true;
                            i = scheduleDetails.size();
                        }
                    }

                    if (!found) {
                        throw new OBException(
                                "Cuota [" + pDetailDueDate + "] no encontrada");
                    }

                }

                boolean isWriteOff = false;

                VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

                String documentNo = pDetail.getDocumentNo();
                if (StringUtils.isEmpty(documentNo)) {
                    documentNo = Utility.getDocumentNo(conn.getConnection(), conn, vars,
                            "", FIN_Payment.ENTITY_NAME, pDetail.getPaymentDocType().getId(),
                            pDetail.getPaymentDocType().getId(), false, true);
                }

                FIN_Payment payment = FIN_AddPayment.savePayment(null, pDetail.getInvoice().isSalesTransaction(),
                        pDetail.getPaymentDocType(), documentNo, pDetail.getBusinessPartner(),
                        pDetail.getPaymentMethod(),
                        pDetail.getFinancialAccount(), pDetail.getAmount().toString(), pDetail.getPaymentDate(),
                        pDetail.getOrganization(),
                        null, scheduleDetails, paidAmount, isWriteOff, false);

                payment.setReferenceNo(pDetail.getReferenceNo());
                payment.setCurrency(pDetail.getCurrency());
                payment.setDescription(pDetail.getDescription());
                payment.setSswhBpBankaccount(pDetail.getPartnerBankAccount());
                payment.setEcscapBanktransfer(pDetail.getSsfiBanktransfer());
                payment.setEcscapNoCheck(pDetail.getCheckno());
                payment.setEcscapDeposit(pDetail.getDepositno());
                payment.setSspacCBpartner(pDetail.getSeller());
                payment.setCostCenter(pDetail.getCostCenter());
                payment.setStDimension(pDetail.getStDimension());
                payment.setNdDimension(pDetail.getNdDimension());

                OBDal.getInstance().save(payment);
                OBDal.getInstance().flush();

                // RPR - Cobrado
                // RDNC - Cobro depositado
                // PPM - Pagado

                String strDocAction = "P";
                OBError message = null;
                if (pDetail.isOnlyPayment()) {
                    message = FIN_AddPayment.processPayment(vars, conn, strDocAction, payment, "TRANSACTION");
                } else {
                    message = FIN_AddPayment.processPayment(vars, conn, strDocAction, payment);
                }

                if ("Error".equals(message.getType())) {
                    throw new OBException(message.getMessage());
                }

                pDetail.setDocumentNo(documentNo);
                pDetail.setResult("OK");
                pDetail.setError(null);
                pDetail.setProcessed(true);

                OBDal.getInstance().save(pDetail);
                OBDal.getInstance().flush();
                conn.getConnection().commit();
            } catch (Exception e) {
                conn.getConnection().rollback();

                String message = Simppys_Helper.getErrorMessage(logger, e);
                logger.error(message);

                pDetail.setResult("ERROR");
                pDetail.setError(message);

                OBDal.getInstance().save(pDetail);
                OBDal.getInstance().flush();
                conn.getConnection().commit();
            }
        }

        qPDetail = OBDal.getInstance().createCriteria(Simppys_PaymentDetail.class);
        qPDetail.add(Restrictions.eq(Simppys_PaymentDetail.PROPERTY_ACTIVE, true));
        qPDetail.add(Restrictions.eq(Simppys_PaymentDetail.PROPERTY_SIMPPYSPAYMENTDATAUPLOAD, paymentDataUpload));
        qPDetail.add(Restrictions.eq(Simppys_PaymentDetail.PROPERTY_PROCESSED, false));
        if (qPDetail.list().size() == 0) {
            paymentDataUpload.setProcessed(true);
            paymentDataUpload.setProcessDate(new Date());

            OBDal.getInstance().save(paymentDataUpload);
            OBDal.getInstance().flush();
        }
    }

}
