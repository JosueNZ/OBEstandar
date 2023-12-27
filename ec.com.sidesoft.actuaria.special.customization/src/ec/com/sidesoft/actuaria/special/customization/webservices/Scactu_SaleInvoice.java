package ec.com.sidesoft.actuaria.special.customization.webservices;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.web.WebService;

import ec.com.sidesoft.actuaria.special.customization.utils.Scactu_Helper;

public class Scactu_SaleInvoice implements WebService {
	private final Logger logger = Logger.getLogger(Scactu_SaleInvoice.class);

	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	@Override
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = new JSONObject();

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin SaleInvoice doGet");

			String documentNo = request.getParameter("documentNo");
			if (StringUtils.isEmpty(documentNo)) {
				throw new OBException("documentNo is required");
			}

			JSONObject body = new JSONObject();
			getSaleInvoice(documentNo.trim(), body);
			json = Scactu_Helper.getResponse(body);
		} catch (Exception e) {
			String message = Scactu_Helper.getErrorMessage(logger, e);
			logger.error(message);
			json = Scactu_Helper.getErrorResponse(message);
		} finally {
			OBContext.restorePreviousMode();
			logger.info("Finish SaleInvoice doGet");
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		Writer w = response.getWriter();
		w.write(json.toString());
		w.close();
	}

	@Override
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	public void getSaleInvoice(String documentNo, JSONObject body) throws Exception {
		OBCriteria<Invoice> qInvoice = OBDal.getInstance().createCriteria(Invoice.class);
		qInvoice.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, true));
		qInvoice.add(Restrictions.sqlRestriction("UPPER(TRIM(documentno)) = ?",
				documentNo.toUpperCase(), StringType.INSTANCE));
		qInvoice.setMaxResults(1);
		if (qInvoice.list().size() == 0) {
			throw new OBException("documentNo [" + documentNo + "] not found");
		}
		Invoice invoice = qInvoice.list().get(0);

		body.put("id", invoice.getId());
		body.put("documentNo", invoice.getDocumentNo());
		body.put("paymentMethod", invoice.getPaymentMethod().getName());
		body.put("paymentMethodId", invoice.getPaymentMethod().getId());
		body.put("invoiceDate", Scactu_Helper.getDateString(invoice.getInvoiceDate()));
		body.put("poReference",
				invoice.getOrderReference() != null ? invoice.getOrderReference() : "");

		Order order = invoice.getSalesOrder();
		body.put("orderdocumentNo", order != null ? order.getDocumentNo() : "");

		BusinessPartner bPartner = invoice.getBusinessPartner();
		body.put("taxId", bPartner.getSearchKey());
		body.put("fiscalName", bPartner.getName());
		body.put("commercialName", bPartner.getName2());

		body.put("total", invoice.getGrandTotalAmount());
		body.put("paid", invoice.getTotalPaid());
		body.put("pending", invoice.getOutstandingAmount());
	}
}
