package ec.com.sidesoft.actuaria.special.customization.webservices;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;

import ec.com.sidesoft.actuaria.special.customization.Scactu_Config;
import ec.com.sidesoft.actuaria.special.customization.Scactu_ConfigPayment;
import ec.com.sidesoft.actuaria.special.customization.Scactu_ConfigPaymentMethod;
import ec.com.sidesoft.actuaria.special.customization.Scactu_Log;
import ec.com.sidesoft.actuaria.special.customization.utils.Scactu_Helper;

public class Scactu_PaymentIn implements WebService {
	private final Logger logger = Logger.getLogger(Scactu_PaymentIn.class);

	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = new JSONObject();
		String logId = null;
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin PaymentIn doPost");

			JSONObject body = Scactu_Helper.readAllIntoJSONObject(request.getInputStream());
			logger.info(body.toString());

			Scactu_Log log = OBProvider.getInstance().get(Scactu_Log.class);
			log.setInterface(Scactu_Helper.getString(body, "interface", false));
			log.setEndpoint("createPaymentIn");
			log.setJsonRequest(body.toString());
			OBDal.getInstance().save(log);
			OBDal.getInstance().flush();
			OBDal.getInstance().getConnection().commit();
			logId = log.getId();

			createPaymentIn(conn, body);
			json = Scactu_Helper.getResponse(body);

			log.setJsonResponse(json.toString());
			log.setRecordID(body.getString("id"));
			log.setReferenceNo(body.getString("documentNo"));
			log.setResult("OK");
			OBDal.getInstance().save(log);
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();
		} catch (Exception e) {
			OBDal.getInstance().rollbackAndClose();
			String message = Scactu_Helper.getErrorMessage(logger, e);
			logger.error(message);
			Scactu_Log log = OBDal.getInstance().get(Scactu_Log.class, logId);
			if (log != null) {
				log.setResult("ERROR");
				log.setError(message);
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				OBDal.getInstance().getConnection().commit();
			}
			json = Scactu_Helper.getErrorResponse(message);
		} finally {
			try {
				conn.getConnection().close();
				conn.destroy();
			} catch (Exception e) {
				Scactu_Helper.getErrorMessage(logger, e);
			}
			OBContext.restorePreviousMode();
			logger.info("Finish PaymentIn doPost");
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		Writer w = response.getWriter();
		w.write(json.toString());
		w.close();
	}

	@Override
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	@Override
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	public FIN_Payment createPaymentIn(ConnectionProvider conn, JSONObject body) throws Exception {
		String pInterface = Scactu_Helper.getString(body, "interface", true).toUpperCase();
		String invoiceNo = Scactu_Helper.getString(body, "invoiceNo", true);
		String paymentMethodId = Scactu_Helper.getString(body, "paymentMethodId", false);
		Date paymentDate = Scactu_Helper.getDate(body, "paymentDate", true);
		BigDecimal amount = Scactu_Helper.getBigDecimal(body, "amount", true);
		String bankCode = Scactu_Helper.getString(body, "bankCode", true);
		String referenceNo = Scactu_Helper.getString(body, "referenceNo", false);

		OBCriteria<Scactu_Config> qConfig = OBDal.getInstance().createCriteria(Scactu_Config.class);
		qConfig.add(Restrictions.eq(Scactu_Config.PROPERTY_ACTIVE, true));
		qConfig.addOrder(org.hibernate.criterion.Order.desc(Scactu_Config.PROPERTY_ORGANIZATION));
		qConfig.setMaxResults(1);
		if (qConfig.list().size() == 0) {
			throw new OBException("Configuration not found");
		}
		Scactu_Config config = qConfig.list().get(0);

		OBCriteria<Scactu_ConfigPayment> qConfigPayment = OBDal.getInstance()
				.createCriteria(Scactu_ConfigPayment.class);
		qConfigPayment.add(Restrictions.eq(Scactu_ConfigPayment.PROPERTY_ACTIVE, true));
		qConfigPayment.add(Restrictions.eq(Scactu_ConfigPayment.PROPERTY_SCACTUCONFIG, config));
		qConfigPayment.add(Restrictions.eq(Scactu_ConfigPayment.PROPERTY_INTERFACE, pInterface));
		qConfigPayment.setMaxResults(1);
		if (qConfigPayment.list().size() == 0) {
			throw new OBException("Payment configuration not found");
		}
		Scactu_ConfigPayment configPayment = qConfigPayment.list().get(0);

		OBCriteria<Invoice> qInvoice = OBDal.getInstance().createCriteria(Invoice.class);
		qInvoice.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, true));
		qInvoice.add(Restrictions.sqlRestriction("TRIM(documentno) = ?", invoiceNo,
				StringType.INSTANCE));
		qInvoice.setMaxResults(1);
		if (qInvoice.list().size() == 0) {
			throw new OBException("invoiceNo [" + invoiceNo + "] not found");
		}
		Invoice invoice = qInvoice.list().get(0);

		Currency currency = invoice.getCurrency();
		int precision = currency.getPricePrecision().intValue();

		FIN_PaymentMethod paymentMethod = invoice.getPaymentMethod();
		if (paymentMethodId != null) {
			OBCriteria<FIN_PaymentMethod> qPaymentMethod = OBDal.getInstance()
					.createCriteria(FIN_PaymentMethod.class);
			qPaymentMethod.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_ACTIVE, true));
			qPaymentMethod.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_ID, paymentMethodId));
			qPaymentMethod.setMaxResults(1);
			if (qPaymentMethod.list().size() == 0) {
				throw new OBException("paymentMethodId [" + paymentMethodId + "] not found");
			}
			paymentMethod = qPaymentMethod.list().get(0);
		}

		OBCriteria<Scactu_ConfigPaymentMethod> qConfigPaymentMethod = OBDal.getInstance()
				.createCriteria(Scactu_ConfigPaymentMethod.class);
		qConfigPaymentMethod.add(Restrictions.eq(Scactu_ConfigPaymentMethod.PROPERTY_ACTIVE, true));
		qConfigPaymentMethod
				.add(Restrictions.eq(Scactu_ConfigPaymentMethod.PROPERTY_SCACTUCONFIG, config));
		qConfigPaymentMethod
				.add(Restrictions.eq(Scactu_ConfigPaymentMethod.PROPERTY_INTERFACE, pInterface));
		qConfigPaymentMethod.add(
				Restrictions.eq(Scactu_ConfigPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
		qConfigPaymentMethod.setMaxResults(1);
		if (qConfigPaymentMethod.list().size() == 0) {
			throw new OBException("Payment method configuration not found");
		}
		Scactu_ConfigPaymentMethod configPaymentMethod = qConfigPaymentMethod.list().get(0);

		OBCriteria<FIN_FinancialAccount> financialAccountCrt = OBDal.getInstance()
				.createCriteria(FIN_FinancialAccount.class);
		financialAccountCrt.add(Restrictions.eq(FIN_FinancialAccount.PROPERTY_SCACTUBANKCODE,
				StringUtils.substring(bankCode, 0, 60)));
		financialAccountCrt.setMaxResults(1);
		if (financialAccountCrt.count() == 0) {
			throw new OBException("Financial Account not found");
		}
		FIN_FinancialAccount finAccount = (FIN_FinancialAccount) financialAccountCrt.uniqueResult();

		amount = amount.setScale(precision, RoundingMode.HALF_UP);
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new OBException(
					"Amount of the payment (" + amount + ") must be greater than 0.0");
		}
		AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
		List<FIN_PaymentScheduleDetail> scheduleDetails = dao
				.getInvoicePendingScheduledPaymentDetails(invoice);
		if (scheduleDetails.size() == 0) {
			throw new OBException(
					"The invoice [" + invoiceNo + "] does not have an outstanding amount");
		}

		List<FIN_PaymentScheduleDetail> details = new ArrayList<FIN_PaymentScheduleDetail>();
		HashMap<String, BigDecimal> paidAmounts = new HashMap<String, BigDecimal>();
		BigDecimal amountPay = new BigDecimal(amount.toString());
		for (int j = 0; j < scheduleDetails.size(); j++) {
			FIN_PaymentScheduleDetail sDetail = scheduleDetails.get(j);
			if (amountPay.compareTo(sDetail.getAmount()) <= 0) {
				paidAmounts.put(sDetail.getId(), amountPay);
				amountPay = BigDecimal.ZERO;
			} else {
				paidAmounts.put(sDetail.getId(), sDetail.getAmount());
				amountPay = amountPay.subtract(sDetail.getAmount());
			}

			if (amountPay.compareTo(BigDecimal.ZERO) == 0) {
				j = scheduleDetails.size();
			}
			details.add(sDetail);
		}

		if (amountPay.compareTo(BigDecimal.ZERO) > 0) {
			throw new OBException("Amount of the payment (" + amount
					+ ") exceeds the total pending of the sales invoice ("
					+ invoice.getOutstandingAmount() + ")");
		}

		boolean isWriteOff = false;

		VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

		String paymentInNo = Utility.getDocumentNo(conn.getConnection(), conn, vars, "",
				invoice.getEntityName(), configPayment.getDocumentType().getId(),
				configPayment.getDocumentType().getId(), false, true);

		FIN_Payment payment = FIN_AddPayment.savePayment(null, invoice.isSalesTransaction(),
				configPayment.getDocumentType(), paymentInNo, invoice.getBusinessPartner(),
				paymentMethod, finAccount, amount.toString(), paymentDate,
				invoice.getOrganization(), null, details, paidAmounts, isWriteOff, false);

		if (StringUtils.isNotBlank(referenceNo)) {
			payment.setReferenceNo(StringUtils.substring(referenceNo, 0, 40));
		}

		payment.setNdDimension(configPayment.getNdDimension());

		OBDal.getInstance().save(payment);
		OBDal.getInstance().flush();

		String strDocAction = "P";
		OBError message = null;
		if (configPaymentMethod.isProcessPayment()) {
			message = FIN_AddPayment.processPayment(vars, conn, strDocAction, payment);
		} else {
			message = FIN_AddPayment.processPayment(vars, conn, strDocAction, payment,
					"TRANSACTION");
		}

		if ("Error".equals(message.getType())) {
			throw new OBException(message.getMessage());
		}

		body.put("id", payment.getId());
		body.put("documentNo", payment.getDocumentNo());

		return payment;
	}
}
