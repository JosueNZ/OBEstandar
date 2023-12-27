package ec.com.sidesoft.actuaria.special.customization.webservices;

import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetailV;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;

import ec.com.sidesoft.actuaria.special.customization.Scactu_Config;
import ec.com.sidesoft.actuaria.special.customization.Scactu_ConfigOrder;
import ec.com.sidesoft.actuaria.special.customization.Scactu_Log;
import ec.com.sidesoft.actuaria.special.customization.utils.Scactu_Helper;

public class Scactu_SaleOrder implements WebService {
	private final Logger logger = Logger.getLogger(Scactu_SaleOrder.class);

	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = new JSONObject();
		String logId = null;
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin SaleOrder doPost");

			JSONObject body = Scactu_Helper.readAllIntoJSONObject(request.getInputStream());
			logger.info(body.toString());

			Scactu_Log log = OBProvider.getInstance().get(Scactu_Log.class);
			log.setInterface(Scactu_Helper.getString(body, "interface", false));
			log.setEndpoint("createSaleOrder");
			log.setJsonRequest(body.toString());
			OBDal.getInstance().save(log);
			OBDal.getInstance().flush();
			OBDal.getInstance().getConnection().commit();
			logId = log.getId();

			createSaleOrder(conn, body);
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
			logger.info("Finish SaleOrder doPost");
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
		JSONObject json = new JSONObject();

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin SaleOrder doGet");

			String documentNo = request.getParameter("documentNo");
			if (StringUtils.isEmpty(documentNo)) {
				throw new OBException("documentNo is required");
			}

			JSONObject body = new JSONObject();
			getSaleOrder(documentNo.trim(), body);
			json = Scactu_Helper.getResponse(body);
		} catch (Exception e) {
			String message = Scactu_Helper.getErrorMessage(logger, e);
			logger.error(message);
			json = Scactu_Helper.getErrorResponse(message);
		} finally {
			OBContext.restorePreviousMode();
			logger.info("Finish SaleOrder doGet");
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
		JSONObject json = new JSONObject();
		String logId = null;
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin SaleOrder doPut");

			JSONObject body = Scactu_Helper.readAllIntoJSONObject(request.getInputStream());
			logger.info(body.toString());

			Scactu_Log log = OBProvider.getInstance().get(Scactu_Log.class);
			log.setInterface(Scactu_Helper.getString(body, "interface", false));
			log.setEndpoint("updateSaleOrder");
			log.setJsonRequest(body.toString());
			OBDal.getInstance().save(log);
			OBDal.getInstance().flush();
			OBDal.getInstance().getConnection().commit();
			logId = log.getId();

			updateSaleOrder(conn, body);
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
			logger.info("Finish SaleOrder doPut");
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		Writer w = response.getWriter();
		w.write(json.toString());
		w.close();
	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	public void createSaleOrder(ConnectionProvider conn, JSONObject body) throws Exception {
		String pInterface = Scactu_Helper.getString(body, "interface", true).toUpperCase();
		String interfaceId = Scactu_Helper.getString(body, "interfaceId", true);
		Boolean pInvoice = Scactu_Helper.getBoolean(body, "invoice", true);
		Date dateOrdered = Scactu_Helper.getDate(body, "dateOrdered", true);
		String paymentMethodId = Scactu_Helper.getString(body, "paymentMethodId", true);
		String poReference = Scactu_Helper.getString(body, "poReference", false);
		String taxId = Scactu_Helper.getString(body, "taxId", true);
		String taxIdInvoice = Scactu_Helper.getString(body, "taxIdInvoice", false);
		String email = Scactu_Helper.getString(body, "email", false);

		OBCriteria<Scactu_Config> qConfig = OBDal.getInstance().createCriteria(Scactu_Config.class);
		qConfig.add(Restrictions.eq(Scactu_Config.PROPERTY_ACTIVE, true));
		qConfig.addOrder(org.hibernate.criterion.Order.desc(Scactu_Config.PROPERTY_ORGANIZATION));
		qConfig.setMaxResults(1);
		if (qConfig.list().size() == 0) {
			throw new OBException("Configuration not found");
		}
		Scactu_Config config = qConfig.list().get(0);

		OBCriteria<Order> qInterface = OBDal.getInstance().createCriteria(Order.class);
		qInterface.add(Restrictions.eq(Order.PROPERTY_SCACTUINTERFACEID, interfaceId));
		qInterface.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
		if (qInterface.list().size() > 0) {
			throw new OBException("interfaceId [" + interfaceId + "] is already registered");
		}

		OBCriteria<Scactu_ConfigOrder> qConfigOrder = OBDal.getInstance()
				.createCriteria(Scactu_ConfigOrder.class);
		qConfigOrder.add(Restrictions.eq(Scactu_ConfigOrder.PROPERTY_ACTIVE, true));
		qConfigOrder.add(Restrictions.eq(Scactu_ConfigOrder.PROPERTY_SCACTUCONFIG, config));
		qConfigOrder.add(Restrictions.eq(Scactu_ConfigOrder.PROPERTY_INTERFACE, pInterface));
		qConfigOrder.setMaxResults(1);
		if (qConfigOrder.list().size() == 0) {
			throw new OBException("Configuration not found");
		}
		Scactu_ConfigOrder configOrder = qConfigOrder.list().get(0);

		OBCriteria<FIN_PaymentMethod> qPaymentMethod = OBDal.getInstance()
				.createCriteria(FIN_PaymentMethod.class);
		qPaymentMethod.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_ACTIVE, true));
		qPaymentMethod.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_ID, paymentMethodId));
		qPaymentMethod.setMaxResults(1);
		if (qPaymentMethod.list().size() == 0) {
			throw new OBException("paymentMethodId [" + paymentMethodId + "] not found");
		}
		FIN_PaymentMethod paymentMethod = qPaymentMethod.list().get(0);

		Currency currency = config.getPriceList().getCurrency();

		OBCriteria<BusinessPartner> qBusinessPartner = OBDal.getInstance()
				.createCriteria(BusinessPartner.class);
		qBusinessPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
		qBusinessPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, taxId));
		qBusinessPartner.setMaxResults(1);
		if (qBusinessPartner.list().size() == 0) {
			throw new OBException("taxId [" + taxId + "] not found");
		}
		BusinessPartner businessPartner = qBusinessPartner.list().get(0);

		OBCriteria<Location> qBusinessPartnerLocation = OBDal.getInstance()
				.createCriteria(Location.class);
		qBusinessPartnerLocation
				.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, businessPartner));
		qBusinessPartnerLocation.add(Restrictions.eq(Location.PROPERTY_SCACTUINTERFACE, true));
		qBusinessPartnerLocation.setMaxResults(1);
		if (qBusinessPartnerLocation.list().size() == 0) {
			throw new OBException("address not found");
		}
		Location businessPartnerLocation = qBusinessPartnerLocation.list().get(0);

		BusinessPartner businessPartnerInvoice = null;
		Location businessPartnerLocationInvoice = null;
		if (taxIdInvoice != null && !taxIdInvoice.isEmpty()) {
			OBCriteria<BusinessPartner> qBusinessPartnerInvoice = OBDal.getInstance()
					.createCriteria(BusinessPartner.class);
			qBusinessPartnerInvoice.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
			qBusinessPartnerInvoice
					.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, taxIdInvoice));
			qBusinessPartnerInvoice.setMaxResults(1);
			if (qBusinessPartnerInvoice.list().size() == 0) {
				throw new OBException("taxIdInvoice [" + taxIdInvoice + "] not found");
			}
			businessPartnerInvoice = qBusinessPartnerInvoice.list().get(0);

			OBCriteria<Location> qBusinessPartnerLocationInvoice = OBDal.getInstance()
					.createCriteria(Location.class);
			qBusinessPartnerLocationInvoice.add(
					Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, businessPartnerInvoice));
			qBusinessPartnerLocationInvoice
					.add(Restrictions.eq(Location.PROPERTY_SCACTUINTERFACE, true));
			qBusinessPartnerLocationInvoice.setMaxResults(1);
			if (qBusinessPartnerLocationInvoice.list().size() == 0) {
				throw new OBException("address not found");
			}
			businessPartnerLocationInvoice = qBusinessPartnerLocationInvoice.list().get(0);
		}

		Order order = OBProvider.getInstance().get(Order.class);
		order.setSalesTransaction(true);
		order.setClient(config.getOrganization().getClient());
		order.setOrganization(config.getOrganization());
		order.setOrderDate(dateOrdered);
		order.setAccountingDate(dateOrdered);
		order.setScheduledDeliveryDate(dateOrdered);

		DocumentType docType = null;
		if (pInvoice) {
			order.setDocumentStatus("DR");
			docType = configOrder.getDoctypeInv();
		} else {
			order.setDocumentStatus("IP");
			docType = configOrder.getDocumentType();
		}

		order.setDocumentType(docType);
		order.setTransactionDocument(docType);
		String saleOrderNo = Utility.getDocumentNo(conn.getConnection(), conn,
				RequestContext.get().getVariablesSecureApp(), "", order.getEntityName(),
				docType.getId(), docType.getId(), false, true);
		order.setDocumentNo(saleOrderNo);

		order.setDocumentAction("CO");
		order.setProcessed(false);
		order.setProcessNow(false);
		order.setPaymentMethod(paymentMethod);
		order.setBusinessPartner(businessPartner);
		order.setPartnerAddress(businessPartnerLocation);
		order.setDeliveryLocation(businessPartnerLocation);
		order.setInvoiceAddress(businessPartnerLocation);
		order.setCurrency(currency);
		order.setPaymentTerms(config.getPaymentTerms());
		order.setPriceList(config.getPriceList());
		order.setWarehouse(config.getWarehouse());
		order.setCostcenter(config.getCostCenter());
		order.setStDimension(config.getStDimension());
		order.setNdDimension(config.getNdDimension());
		order.setOrderReference(poReference);
		order.setInvoiceTerms(config.getInvoiceTerms());
		order.setEeiEmailTo(StringUtils.isNotBlank(businessPartner.getEEIEmail())
				? businessPartner.getEEIEmail()
				: email);

		order.setScactuInterface(pInterface);
		order.setScactuInterfaceID(interfaceId);
		order.setScactuBpartnerInvoice(businessPartnerInvoice);
		order.setScactuBplInvoice(businessPartnerLocationInvoice);

		// Logistics Data
		JSONObject logisticsData = body.has("logisticsData") ? body.getJSONObject("logisticsData")
				: null;
		if (logisticsData != null) {
			String title = Scactu_Helper.getString(logisticsData, "title", false);
			String contact = Scactu_Helper.getString(logisticsData, "contact", false);
			String position = Scactu_Helper.getString(logisticsData, "position", false);
			String city = Scactu_Helper.getString(logisticsData, "city", false);
			String address = Scactu_Helper.getString(logisticsData, "address", false);
			String guideNo = Scactu_Helper.getString(logisticsData, "guideNo", false);
			Date actualDelivery = Scactu_Helper.getDate(logisticsData, "actualDelivery", false);
			String receives = Scactu_Helper.getString(logisticsData, "receives", false);
			Date dispatch = Scactu_Helper.getDate(logisticsData, "dispatch", false);

			order.setScactuTitle(title);
			order.setScactuContact(contact);
			order.setScactuPosition(position);
			order.setScactuCity(city);
			order.setScactuAdress(address);
			order.setScactuGuideno(guideNo);
			order.setScactuActualdelivery(actualDelivery);
			order.setScactuReceives(receives);
			order.setScactuOffice(dispatch);
		}

		// CRM Data
		JSONObject crmData = body.has("crmData") ? body.getJSONObject("crmData") : null;
		if (crmData != null) {
			String effectiveSaleCode = Scactu_Helper.getString(crmData, "effectiveSaleCode", false);
			if (effectiveSaleCode != null) {
				checkEffectiveSaleCode(effectiveSaleCode);
			}

			String commercialTeam = Scactu_Helper.getString(crmData, "commercialTeam", false);
			String technical = Scactu_Helper.getString(crmData, "technical", false);
			String orderInstructions = Scactu_Helper.getString(crmData, "orderInstructions", false);
			String orderDetail = Scactu_Helper.getString(crmData, "orderDetail", false);
			String nameRequest = Scactu_Helper.getString(crmData, "nameRequest", false);
			String emailRequest = Scactu_Helper.getString(crmData, "emailRequest", false);
			String states = Scactu_Helper.getString(crmData, "states", false);
			Date estimatedDate = Scactu_Helper.getDate(crmData, "estimatedDate", false);
			Date admissionDate = Scactu_Helper.getDate(crmData, "admissionDate", false);
			Date processEndDate = Scactu_Helper.getDate(crmData, "processEndDate", false);
			String executive = Scactu_Helper.getString(crmData, "executive", false);

			order.setScactuEffectivecode(effectiveSaleCode);
			order.setScactuCommercialTeam(commercialTeam);
			order.setScactuTechnical(technical);
			order.setScactuOrderinstructions(orderInstructions);
			order.setScactuOrderdetail(orderDetail);
			order.setScactuNamerequests(nameRequest);
			order.setScactuEmailrequest(emailRequest);
			order.setScactuState(states);
			order.setScactuEstimateddate(estimatedDate);
			order.setScheduledDeliveryDate(estimatedDate);
			order.setScactuAdmissiondate(admissionDate);
			order.setScactuProcessdate(processEndDate);
			order.setScactuExecutive(executive);
		}

		OBDal.getInstance().save(order);
		OBDal.getInstance().flush();

		body.put("id", order.getId());
		body.put("documentNo", saleOrderNo);

		JSONArray items = body.has("items") ? body.getJSONArray("items") : new JSONArray();
		if (items.length() == 0) {
			throw new OBException("items is required");
		}

		int precision = currency.getPricePrecision().intValue();

		createSaleOrderLines(body, order, items, precision);

		Invoice invoice = null;
		if (pInvoice) {
			final List<Object> parameters = new ArrayList<Object>();
			parameters.add(null);
			parameters.add(order.getId());
			parameters.add("N");
			final String procedureName = "c_order_post1";
			CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);

			OBDal.getInstance().refresh(order);

			List<Invoice> invoices = order.getInvoiceList();
			if (invoices.size() == 0) {
				return;
			}
			invoice = invoices.get(0);

			JSONObject saleInvoice = new JSONObject();
			saleInvoice.put("id", invoice.getId());
			saleInvoice.put("documentNo", invoice.getDocumentNo());
			body.put("saleInvoice", saleInvoice);
		}

		if (invoice != null && body.has("paymentIn")) {
			JSONObject paymentIn = body.getJSONObject("paymentIn");
			paymentIn.put("interface", pInterface);
			paymentIn.put("invoiceNo", invoice.getDocumentNo());
			paymentIn.put("paymentMethodId", paymentMethodId);

			Date paymentDate = Scactu_Helper.getDate(paymentIn, "paymentDate", false);

			if (paymentDate != null) {
				paymentIn.put("paymentDate", Scactu_Helper.getDateString(paymentDate));
			} else {
				paymentIn.put("paymentDate", Scactu_Helper.getDateString(dateOrdered));

			}

			FIN_Payment payment = new Scactu_PaymentIn().createPaymentIn(conn, paymentIn);
			paymentIn.put("id", payment.getId());
			paymentIn.put("documentNo", payment.getDocumentNo());
			body.put("paymentIn", paymentIn);
		}
	}

	private void checkEffectiveSaleCode(String effectiveSaleCode) throws Exception {
		OBCriteria<Order> qOrder = OBDal.getInstance().createCriteria(Order.class);
		qOrder.add(Restrictions.isNotNull(Order.PROPERTY_SCACTUINTERFACE));
		qOrder.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
		qOrder.add(Restrictions.sqlRestriction("UPPER(TRIM(em_scactu_effectivecode)) = ?",
				effectiveSaleCode.toUpperCase(), StringType.INSTANCE));
		if (qOrder.list().size() > 0) {
			throw new OBException("duplicate effectiveSaleCode [" + effectiveSaleCode + "]");
		}
	}

	public void createSaleOrderLines(JSONObject body, Order order, JSONArray items, int precision)
			throws Exception {
		Long lineNo = new Long(10);
		for (int i = 0; i < items.length(); i++) {
			JSONObject item = items.getJSONObject(i);
			String searchKey = Scactu_Helper.getString(item, "searchKey", true);
			BigDecimal listPrice = Scactu_Helper.getBigDecimal(item, "listPrice", true);
			BigDecimal unitPrice = Scactu_Helper.getBigDecimal(item, "unitPrice", true);
			BigDecimal orderedQuantity = Scactu_Helper.getBigDecimal(item, "quantity", true);
			String description = Scactu_Helper.getString(item, "description", false);

			OBCriteria<Product> qProduct = OBDal.getInstance().createCriteria(Product.class);
			qProduct.add(Restrictions.eq(Product.PROPERTY_ACTIVE, true));
			qProduct.add(Restrictions.sqlRestriction("UPPER(TRIM(value)) = ?",
					searchKey.toUpperCase(), StringType.INSTANCE));
			qProduct.setMaxResults(1);
			if (qProduct.list().size() == 0) {
				throw new OBException("searchKey [" + searchKey + "] not found");
			}
			Product product = qProduct.list().get(0);

			OBCriteria<TaxRate> qTax = OBDal.getInstance().createCriteria(TaxRate.class);
			qTax.add(Restrictions.eq(TaxRate.PROPERTY_ACTIVE, true));
			qTax.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, product.getTaxCategory()));
			qTax.add(Restrictions.eq(TaxRate.PROPERTY_DEFAULT, true));
			qTax.setMaxResults(1);
			if (qTax.list().size() == 0) {
				throw new OBException("Tax rate not found");
			}
			TaxRate tax = qTax.list().get(0);

			OrderLine line = OBProvider.getInstance().get(OrderLine.class);
			line.setClient(order.getClient());
			line.setOrganization(order.getOrganization());
			line.setSalesOrder(order);
			line.setLineNo(lineNo);
			line.setOrderDate(order.getOrderDate());
			line.setScheduledDeliveryDate(order.getOrderDate());
			line.setBusinessPartner(order.getBusinessPartner());
			line.setWarehouse(order.getWarehouse());
			line.setCurrency(order.getCurrency());
			line.setProduct(product);
			line.setUOM(product.getUOM());
			line.setTax(tax);
			listPrice = listPrice.setScale(precision, RoundingMode.HALF_UP);
			line.setListPrice(listPrice);
			unitPrice = unitPrice.setScale(precision, RoundingMode.HALF_UP);
			line.setUnitPrice(unitPrice);
			orderedQuantity = orderedQuantity.setScale(precision, RoundingMode.HALF_UP);
			line.setOrderedQuantity(orderedQuantity);
			line.setStandardPrice(unitPrice);
			line.setCostcenter(product.getScactuCostcenter());
			line.setStDimension(product.getScactuUser1());
			line.setNdDimension(product.getScactuUser2());
			line.setDescription(description);

			if (unitPrice.compareTo(listPrice) != 0) {
				Float discount = 100 - ((unitPrice.floatValue() / listPrice.floatValue()) * 100);
				line.setDiscount(
						new BigDecimal(discount).setScale(precision, RoundingMode.HALF_UP));
			}

			BigDecimal subTotal = unitPrice.multiply(orderedQuantity).setScale(precision,
					RoundingMode.HALF_UP);
			BigDecimal total = subTotal
					.multiply(tax.getRate().divide(new BigDecimal(100)).add(new BigDecimal(1)))
					.setScale(precision, RoundingMode.HALF_UP);
			line.setLineNetAmount(total);

			OBDal.getInstance().save(line);
			OBDal.getInstance().flush();

			item.put("id", line.getId());
			items.put(i, item);

			lineNo += 10;
		}
		body.put("items", items);
	}

	public void updateSaleOrder(ConnectionProvider conn, JSONObject body) throws Exception {
		String documentNo = Scactu_Helper.getString(body, "documentNo", true);
		String taxIdInvoice = Scactu_Helper.getString(body, "taxIdInvoice", false);

		OBCriteria<Order> qOrder = OBDal.getInstance().createCriteria(Order.class);
		qOrder.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
		qOrder.add(Restrictions.or(
				Restrictions.sqlRestriction("UPPER(TRIM(documentno)) = ?", documentNo.toUpperCase(),
						StringType.INSTANCE),
				Restrictions.sqlRestriction("UPPER(TRIM(em_scactu_interface_id)) = ?",
						documentNo.toUpperCase(), StringType.INSTANCE)));
		if (qOrder.list().size() == 0) {
			throw new OBException("documentNo [" + documentNo + "] not found");
		}
		Order order = qOrder.list().get(0);
		body.put("id", order.getId());

		if (!(order.getDocumentStatus().equals("DR") || order.getDocumentStatus().equals("IP"))) {
			throw new OBException("The sales order can no longer be modified");
		}

		BusinessPartner businessPartnerInvoice = null;
		Location businessPartnerLocationInvoice = null;
		if (taxIdInvoice != null && !taxIdInvoice.isEmpty()) {
			OBCriteria<BusinessPartner> qBusinessPartnerInvoice = OBDal.getInstance()
					.createCriteria(BusinessPartner.class);
			qBusinessPartnerInvoice.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
			qBusinessPartnerInvoice
					.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, taxIdInvoice));
			qBusinessPartnerInvoice.setMaxResults(1);
			if (qBusinessPartnerInvoice.list().size() == 0) {
				throw new OBException("taxIdInvoice [" + taxIdInvoice + "] not found");
			}
			businessPartnerInvoice = qBusinessPartnerInvoice.list().get(0);

			OBCriteria<Location> qBusinessPartnerLocationInvoice = OBDal.getInstance()
					.createCriteria(Location.class);
			qBusinessPartnerLocationInvoice.add(
					Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, businessPartnerInvoice));
			qBusinessPartnerLocationInvoice
					.add(Restrictions.eq(Location.PROPERTY_SCACTUINTERFACE, true));
			qBusinessPartnerLocationInvoice.setMaxResults(1);
			if (qBusinessPartnerLocationInvoice.list().size() == 0) {
				throw new OBException("address not found");
			}
			businessPartnerLocationInvoice = qBusinessPartnerLocationInvoice.list().get(0);

			order.setScactuBpartnerInvoice(businessPartnerInvoice);
			order.setScactuBplInvoice(businessPartnerLocationInvoice);
		}

		order.setEeiEmailTo(order.getBusinessPartner().getEEIEmail());

		// CRM Data
		JSONObject crmData = body.has("crmData") ? body.getJSONObject("crmData") : null;
		if (crmData != null) {
			String commercialTeam = Scactu_Helper.getString(crmData, "commercialTeam", false);
			String technical = Scactu_Helper.getString(crmData, "technical", false);
			String orderInstructions = Scactu_Helper.getString(crmData, "orderInstructions", false);
			String orderDetail = Scactu_Helper.getString(crmData, "orderDetail", false);
			String nameRequest = Scactu_Helper.getString(crmData, "nameRequest", false);
			String emailRequest = Scactu_Helper.getString(crmData, "emailRequest", false);
			String states = Scactu_Helper.getString(crmData, "states", false);
			Date estimatedDate = Scactu_Helper.getDate(crmData, "estimatedDate", false);
			Date admissionDate = Scactu_Helper.getDate(crmData, "admissionDate", false);
			Date processEndDate = Scactu_Helper.getDate(crmData, "processEndDate", false);
			String executive = Scactu_Helper.getString(crmData, "executive", false);

			order.setScactuCommercialTeam(commercialTeam);
			order.setScactuTechnical(technical);
			order.setScactuOrderinstructions(orderInstructions);
			order.setScactuOrderdetail(orderDetail);
			order.setScactuNamerequests(nameRequest);
			order.setScactuEmailrequest(emailRequest);
			order.setScactuState(states);
			order.setScactuEstimateddate(estimatedDate);
			order.setScheduledDeliveryDate(estimatedDate);
			order.setScactuAdmissiondate(admissionDate);
			order.setScactuProcessdate(processEndDate);
			order.setScactuExecutive(executive);
		}

		OBDal.getInstance().save(order);
		OBDal.getInstance().flush();

		JSONArray items = body.has("items") ? body.getJSONArray("items") : new JSONArray();
		if (items.length() > 0) {
			Currency currency = order.getPriceList().getCurrency();
			int precision = currency.getPricePrecision().intValue();

			order.getOrderLineList().removeAll(order.getOrderLineList());
			OBDal.getInstance().save(order);
			OBDal.getInstance().flush();

			createSaleOrderLines(body, order, items, precision);
		}

	}

	public void getSaleOrder(String documentNo, JSONObject body) throws Exception {
		OBCriteria<Order> qOrder = OBDal.getInstance().createCriteria(Order.class);
		qOrder.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
		qOrder.add(Restrictions.or(
				Restrictions.sqlRestriction("UPPER(TRIM(documentno)) = ?", documentNo.toUpperCase(),
						StringType.INSTANCE),
				Restrictions.sqlRestriction("UPPER(TRIM(em_scactu_interface_id)) = ?",
						documentNo.toUpperCase(), StringType.INSTANCE)));
		qOrder.setMaxResults(1);
		if (qOrder.list().size() == 0) {
			throw new OBException("documentNo [" + documentNo + "] not found");
		}
		Order order = qOrder.list().get(0);

		body.put("id", order.getId());
		body.put("documentNo", order.getDocumentNo());
		body.put("interface", order.getScactuInterface());
		body.put("interfaceId", order.getScactuInterfaceID());
		body.put("paymentMethodId", order.getPaymentMethod().getId());
		body.put("dateOrdered", Scactu_Helper.getDateString(order.getOrderDate()));
		body.put("poReference", order.getOrderReference() != null ? order.getOrderReference() : "");
		body.put("taxId", order.getBusinessPartner().getSearchKey());
		body.put("addressId", order.getPartnerAddress().getId());

		JSONObject logisticsData = new JSONObject();
		logisticsData.put("contact",
				order.getScactuContact() != null ? order.getScactuContact() : "");
		logisticsData.put("position",
				order.getScactuPosition() != null ? order.getScactuPosition() : "");
		logisticsData.put("city", order.getScactuCity() != null ? order.getScactuCity() : "");
		logisticsData.put("address",
				order.getScactuAdress() != null ? order.getScactuAdress() : "");
		logisticsData.put("guideNo",
				order.getScactuGuideno() != null ? order.getScactuGuideno() : "");
		String actualdelivery = Scactu_Helper.getDateString(order.getScactuActualdelivery());
		logisticsData.put("actualDelivery", actualdelivery != null ? actualdelivery : "");
		logisticsData.put("receives",
				order.getScactuReceives() != null ? order.getScactuReceives() : "");
		String dispatch = Scactu_Helper.getDateString(order.getScactuOffice());
		logisticsData.put("dispatch", dispatch != null ? dispatch : "");
		body.put("logisticsData", logisticsData);

		JSONObject crmData = new JSONObject();
		crmData.put("effectiveSaleCode",
				order.getScactuEffectivecode() != null ? order.getScactuEffectivecode() : "");
		crmData.put("commercialTeam",
				order.getScactuCommercialTeam() != null ? order.getScactuCommercialTeam() : "");
		crmData.put("technical",
				order.getScactuTechnical() != null ? order.getScactuTechnical() : "");
		crmData.put("orderInstructions",
				order.getScactuOrderinstructions() != null ? order.getScactuOrderinstructions()
						: "");
		crmData.put("orderDetail",
				order.getScactuOrderdetail() != null ? order.getScactuOrderdetail() : "");
		crmData.put("nameRequest",
				order.getScactuNamerequests() != null ? order.getScactuNamerequests() : "");
		crmData.put("emailRequest",
				order.getScactuEmailrequest() != null ? order.getScactuEmailrequest() : "");
		crmData.put("states", order.getScactuState() != null ? order.getScactuState() : "");
		String estimatedDate = Scactu_Helper.getDateString(order.getScactuEstimateddate());
		crmData.put("estimatedDate", estimatedDate != null ? estimatedDate : "");
		String admissionDate = Scactu_Helper.getDateString(order.getScactuAdmissiondate());
		crmData.put("admissionDate", admissionDate != null ? admissionDate : "");
		String processEndDate = Scactu_Helper.getDateString(order.getScactuProcessdate());
		crmData.put("processEndDate", processEndDate != null ? processEndDate : "");
		crmData.put("executive",
				order.getScactuExecutive() != null ? order.getScactuExecutive() : "");
		body.put("crmData", crmData);

		Currency currency = order.getPriceList().getCurrency();
		int precision = currency.getPricePrecision().intValue();

		JSONArray items = new JSONArray();
		for (OrderLine line : order.getOrderLineList()) {
			JSONObject item = new JSONObject();
			item.put("id", line.getId());
			item.put("searchKey", line.getProduct().getSearchKey());
			item.put("quantity", line.getOrderedQuantity());
			item.put("unitPrice", line.getUnitPrice());
			item.put("listPrice", line.getListPrice());

			TaxRate taxRate = line.getTax();
			BigDecimal rate = new BigDecimal(1).add(taxRate.getRate().divide(new BigDecimal(100))
					.setScale(precision, RoundingMode.HALF_UP));
			item.put("total", line.getLineNetAmount().multiply(rate).setScale(precision,
					RoundingMode.HALF_UP));

			items.put(item);
		}
		body.put("items", items);

		if (order.getInvoiceList() != null && order.getInvoiceList().size() > 0) {
			Invoice invoice = null;
			for (Invoice inv : order.getInvoiceList()) {
				if (inv.getDocumentStatus().equals("CO")) {
					invoice = inv;
				}
			}

			if (invoice != null) {
				JSONObject saleInvoice = new JSONObject();
				saleInvoice.put("id", invoice.getId());
				saleInvoice.put("documentNo", invoice.getDocumentNo());
				body.put("saleInvoice", saleInvoice);

				OBCriteria<FIN_PaymentDetailV> qPayment = OBDal.getInstance()
						.createCriteria(FIN_PaymentDetailV.class);
				qPayment.add(Restrictions.eq(FIN_PaymentDetailV.PROPERTY_INVOICENO,
						invoice.getDocumentNo()));
				qPayment.add(Restrictions.eq(FIN_PaymentDetailV.PROPERTY_ORDERNO,
						order.getDocumentNo()));
				qPayment.setMaxResults(1);
				if (qPayment.list().size() > 0) {
					FIN_PaymentDetailV payment = qPayment.list().get(0);

					JSONObject paymentIn = new JSONObject();
					paymentIn.put("id", payment.getPayment().getId());
					paymentIn.put("interface", order.getScactuInterface());
					paymentIn.put("documentNo", payment.getPaymentno());
					paymentIn.put("paymentDate",
							Scactu_Helper.getDateString(payment.getPaymentDate()));
					paymentIn.put("paymentMethodId", payment.getPaymentMethod().getId());
					paymentIn.put("amount", payment.getPaidAmount());
					paymentIn.put("invoiceNo", payment.getInvoiceno());

					body.put("paymentIn", paymentIn);
				}
			}
		}
	}
}
