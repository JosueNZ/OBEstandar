package ec.com.sidesoft.actuaria.special.customization.ad_process;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.actuaria.special.customization.Scactu_Config;
import ec.com.sidesoft.actuaria.special.customization.Scactu_Log;
import ec.com.sidesoft.actuaria.special.customization.utils.Scactu_Helper;

public class Scactu_IntizaSync extends DalBaseProcess implements KillableProcess {
	private final Logger logger = Logger.getLogger(Scactu_IntizaSync.class);
	private ProcessLogger processLogger;
	private boolean killProcess = false;

	@Override
	public void doExecute(ProcessBundle bundle) throws Exception {
		ConnectionProvider conn = new DalConnectionProvider(false);
		try {
			processLogger = bundle.getLogger();
			logger.info("Begin Intiza Sync");
			OBContext.setAdminMode(true);

			process(conn);
		} catch (final Exception e) {
			String message = Scactu_Helper.getErrorMessage(logger, e);
			processLogger.logln(message);
		} finally {
			try {
				conn.getConnection().close();
				conn.destroy();
			} catch (Exception e) {
				Scactu_Helper.getErrorMessage(logger, e);
			}
			OBContext.restorePreviousMode();
			logger.info("Finish Intiza Sync");
		}
	}

	@Override
	public void kill(ProcessBundle processBundle) throws Exception {
		OBDal.getInstance().flush();
		this.killProcess = true;
	}

	private void process(ConnectionProvider conn) throws Exception {
		OBCriteria<Scactu_Config> qConfig = OBDal.getInstance().createCriteria(Scactu_Config.class);
		qConfig.add(Restrictions.eq(Scactu_Config.PROPERTY_ACTIVE, true));
		qConfig.addOrder(org.hibernate.criterion.Order.desc(Scactu_Config.PROPERTY_ORGANIZATION));
		qConfig.setMaxResults(1);
		if (qConfig.list().size() == 0) {
			throw new OBException("Configuration not found");
		}
		Scactu_Config config = qConfig.list().get(0);

		try {
			businessPartnerSync(conn, config);
		} catch (Exception e) {
			String message = Scactu_Helper.getErrorMessage(logger, e);
			processLogger.logln(message);
		}

		try {
			saleInvoiceSync(conn, config);
		} catch (Exception e) {
			String message = Scactu_Helper.getErrorMessage(logger, e);
			processLogger.logln(message);
		}

		try {
			paymentSync(conn, config);
		} catch (Exception e) {
			String message = Scactu_Helper.getErrorMessage(logger, e);
			processLogger.logln(message);
		}
	}

	private void businessPartnerSync(ConnectionProvider conn, Scactu_Config config)
			throws Exception {
		String action = "SetClients";

		Date date = new Date();
		ScactuBusinessPartnerData[] lines = ScactuBusinessPartnerData.select(conn);

		SOAPConnectionFactory connectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection connection = connectionFactory.createConnection();
		MessageFactory messageFactory = MessageFactory.newInstance();
		for (ScactuBusinessPartnerData line : lines) {
			if (killProcess) {
				throw new OBException("Process killed");
			}

			String logId = null;
			try {
				Scactu_Log log = OBProvider.getInstance().get(Scactu_Log.class);
				log.setInterface("I");
				log.setEndpoint(action);
				log.setRecordID(line.cBpartnerId);
				log.setType("OUT");
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();
				logId = log.getId();

				SOAPMessage message = messageFactory.createMessage();
				SOAPPart part = message.getSOAPPart();

				// SOAP Envelope
				SOAPEnvelope envelope = part.getEnvelope();
				envelope.addNamespaceDeclaration("xsi",
						"http://www.w3.org/2001/XMLSchema-instance");
				envelope.addNamespaceDeclaration("web", config.getNamespace());

				MimeHeaders headers = message.getMimeHeaders();
				headers.addHeader("Content-Type", "text/xml; charset=utf-8");
				headers.addHeader("SOAPAction", config.getNamespace() + action);

				// SOAP Body
				SOAPBody body = envelope.getBody();
				SOAPElement setClients = body.addChildElement(action, "web");
				setClients.setAttribute("xmlns", config.getNamespace());
				setClients.addChildElement("Uid", "web").addTextNode(config.getIntizaUser());
				setClients.addChildElement("Pwd", "web").addTextNode(config.getIntizaPass());

				SOAPElement clients = setClients.addChildElement("Clients", "web");
				SOAPElement client = clients.addChildElement("Client", "web");
				client.addChildElement("Company_ID", "web").addTextNode(config.getIntizaCompany());
				client.addChildElement("Client_ID", "web").addTextNode(line.taxid);
				client.addChildElement("Name", "web").addTextNode(line.bpName);
				client.addChildElement("Phone", "web").addTextNode(line.phone);
				client.addChildElement("Address", "web").addTextNode(line.address);
				client.addChildElement("Notes", "web").addTextNode("");

				SOAPElement additionals = client.addChildElement("Additionals", "web");
				SOAPElement additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("Grupo");
				additional.addChildElement("Value", "web").addTextNode(line.mainAccount);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("Portafolio cart");
				additional.addChildElement("Value", "web").addTextNode(line.portfolio);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("telcas");
				additional.addChildElement("Value", "web").addTextNode(line.phone2);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("ciucli");
				additional.addChildElement("Value", "web").addTextNode(line.city);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web")
						.addTextNode("IDENTIFICACION FISCAL DEL CLIENTE");
				additional.addChildElement("Value", "web").addTextNode(line.taxid);

				OBCriteria<User> qUser = OBDal.getInstance().createCriteria(User.class);
				qUser.add(Restrictions.eq(User.PROPERTY_ACTIVE, true));
				qUser.add(Restrictions.eq(User.PROPERTY_BUSINESSPARTNER,
						OBDal.getInstance().get(BusinessPartner.class, line.cBpartnerId)));
				qUser.addOrder(Order.desc(User.PROPERTY_SCACTUISCOLLECTION));
				qUser.addOrder(Order.desc(User.PROPERTY_SCACTUISBILLING));
				qUser.addOrder(Order.asc(User.PROPERTY_CREATIONDATE));
				qUser.setMaxResults(4);

				for (int i = 0; i < qUser.list().size(); i++) {
					User contact = qUser.list().get(i);

					String name = contact.getName() == null || contact.getName().isEmpty() ? ""
							: contact.getName();
					if (name.isEmpty()) {
						name = contact.getFirstName() == null || contact.getFirstName().isEmpty()
								? ""
								: contact.getFirstName();
						name += contact.getLastName() == null || contact.getLastName().isEmpty()
								? ""
								: contact.getLastName();
					}

					if (i == 0) {
						client.addChildElement("Contact", "web").addTextNode(name);
						client.addChildElement("Email", "web")
								.addTextNode(contact.getEmail() != null ? contact.getEmail() : "");
					} else {
						additional = additionals.addChildElement("Additional", "web");
						additional.addChildElement("Name", "web")
								.addTextNode("NOMBRE CONTACTO " + (i + 1));
						additional.addChildElement("Value", "web").addTextNode(name);
						additional = additionals.addChildElement("Additional", "web");
						additional.addChildElement("Name", "web").addTextNode("mail" + (i + 1));
						additional.addChildElement("Value", "web")
								.addTextNode(contact.getEmail() != null ? contact.getEmail() : "");
					}

					additional = additionals.addChildElement("Additional", "web");
					additional.addChildElement("Name", "web").addTextNode("Cargo" + (i + 1));
					additional.addChildElement("Value", "web").addTextNode(
							contact.getPosition() != null ? contact.getPosition() : "");

				}

				message.saveChanges();
				log.setJsonRequest(Scactu_Helper.xmlToString(message));
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();

				SOAPMessage response = connection.call(message, config.getIntizaWriteEndpoint());
				JSONObject json = Scactu_Helper.xmlToJSON(response);

				log.setJsonResponse(Scactu_Helper.xmlToString(response));
				log.setReferenceNo(line.taxid);
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();

				if (!json.has(action + "Response")) {
					throw new OBException("ERROR");
				}

				JSONObject result = json.getJSONObject(action + "Response")
						.getJSONObject(action + "Result");
				if (result.getInt("Received") > result.getInt("Processed")) {
					throw new OBException(result.getString("Description"));
				}

				ScactuBusinessPartnerData.update(conn, Scactu_Helper.getTimestampString(date),
						line.cBpartnerId);

				log.setResult("OK");
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();
			} catch (Exception e) {
				conn.getConnection().rollback();
				String message = Scactu_Helper.getErrorMessage(logger, e);
				processLogger.logln(message);
				Scactu_Log log = OBDal.getInstance().get(Scactu_Log.class, logId);
				if (log != null) {
					log.setResult("ERROR");
					log.setError(message);
					OBDal.getInstance().save(log);
					OBDal.getInstance().flush();
					conn.getConnection().commit();
				}
			}
		}
		connection.close();
	}

	private void saleInvoiceSync(ConnectionProvider conn, Scactu_Config config) throws Exception {
		String action = "SetInvoices";

		Date date = new Date();
		ScactuSaleInvoiceData[] lines = ScactuSaleInvoiceData.select(conn);

		SOAPConnectionFactory connectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection connection = connectionFactory.createConnection();
		MessageFactory messageFactory = MessageFactory.newInstance();
		for (ScactuSaleInvoiceData line : lines) {
			if (killProcess) {
				throw new OBException("Process killed");
			}

			if (StringUtils.isNotEmpty(line.intizaLastSync)) {
				action = "EditInvoices";
			}

			String logId = null;
			try {
				Scactu_Log log = OBProvider.getInstance().get(Scactu_Log.class);
				log.setInterface("I");
				log.setEndpoint(action);
				log.setRecordID(line.cInvoiceId);
				log.setType("OUT");
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();
				logId = log.getId();

				SOAPMessage message = messageFactory.createMessage();
				SOAPPart part = message.getSOAPPart();

				// SOAP Envelope
				SOAPEnvelope envelope = part.getEnvelope();
				envelope.addNamespaceDeclaration("xsi",
						"http://www.w3.org/2001/XMLSchema-instance");
				envelope.addNamespaceDeclaration("web", config.getNamespace());

				MimeHeaders headers = message.getMimeHeaders();
				headers.addHeader("Content-Type", "text/xml; charset=utf-8");
				headers.addHeader("SOAPAction", config.getNamespace() + action);

				// SOAP Body
				SOAPBody body = envelope.getBody();
				SOAPElement setClients = body.addChildElement(action, "web");
				setClients.setAttribute("xmlns", config.getNamespace());
				setClients.addChildElement("Uid", "web").addTextNode(config.getIntizaUser());
				setClients.addChildElement("Pwd", "web").addTextNode(config.getIntizaPass());

				SOAPElement invoices = setClients.addChildElement("Invoices", "web");
				SOAPElement invoice = invoices.addChildElement("Invoice", "web");
				invoice.addChildElement("Company_ID", "web").addTextNode(config.getIntizaCompany());
				invoice.addChildElement("Client_ID", "web").addTextNode(line.taxid);
				invoice.addChildElement("Invoice_ID", "web").addTextNode(line.documentno);
				invoice.addChildElement("Currency_ID", "web").addTextNode(line.currency);
				invoice.addChildElement("EmittedDate", "web").addTextNode(line.dateinvoiced);
				invoice.addChildElement("Amount", "web").addTextNode(line.grandtotal);
				invoice.addChildElement("ReferenceNumber", "web").addTextNode(line.documentno);
				invoice.addChildElement("DueDate", "web").addTextNode(line.duedate);
				invoice.addChildElement("Status", "web").addTextNode("");

				SOAPElement additionals = invoice.addChildElement("Additionals", "web");
				SOAPElement additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("PORTAFOLIO");
				additional.addChildElement("Value", "web").addTextNode(line.commercialTeam);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("NUMERO DE PROCESO");
				additional.addChildElement("Value", "web").addTextNode(line.oDocumentno);

				Invoice inv = OBDal.getInstance().get(Invoice.class, line.cInvoiceId);

				OBCriteria<InvoiceLine> qInvoiceLine = OBDal.getInstance()
						.createCriteria(InvoiceLine.class);
				qInvoiceLine.add(Restrictions.eq(InvoiceLine.PROPERTY_ACTIVE, true));
				qInvoiceLine.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE, inv));
				qInvoiceLine.addOrder(Order.asc(InvoiceLine.PROPERTY_LINENO));

				Currency currency = inv.getCurrency();
				int precision = currency.getPricePrecision().intValue();

				String lineInfo = "";
				for (int i = 0; i < qInvoiceLine.list().size(); i++) {
					InvoiceLine invoiceLine = qInvoiceLine.list().get(i);

					Product product = invoiceLine.getProduct();

					OBCriteria<TaxRate> qTax = OBDal.getInstance().createCriteria(TaxRate.class);
					qTax.add(Restrictions.eq(TaxRate.PROPERTY_ACTIVE, true));
					qTax.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY,
							product.getTaxCategory()));
					qTax.add(Restrictions.eq(TaxRate.PROPERTY_DEFAULT, true));
					qTax.setMaxResults(1);

					TaxRate tax = qTax.list().get(0);

					BigDecimal discount = new BigDecimal(0);
					if (invoiceLine.getUnitPrice().compareTo(invoiceLine.getUnitPrice()) != 0) {
						discount = new BigDecimal(100).subtract(invoiceLine.getUnitPrice()
								.divide(invoiceLine.getListPrice()).multiply(new BigDecimal(100)))
								.setScale(precision, RoundingMode.HALF_UP);
					}

					TaxRate taxRate = invoiceLine.getTax();
					BigDecimal rate = taxRate.getRate().divide(new BigDecimal(100))
							.setScale(precision, RoundingMode.HALF_UP);
					BigDecimal IVA = rate.multiply(invoiceLine.getLineNetAmount())
							.setScale(precision, RoundingMode.HALF_UP);

					if (!lineInfo.isEmpty()) {
						lineInfo += " | ";
					}

					lineInfo += "Código: " + product.getSearchKey() + ", Nombre: "
							+ product.getName() + ", Cantidad: " + invoiceLine.getInvoicedQuantity()
							+ ", Precio: " + invoiceLine.getUnitPrice() + ", Descuento: "
							+ (discount != null ? discount : "0.0") + ", Total: "
							+ invoiceLine.getLineNetAmount() + ", IVA: " + IVA;

					if (qInvoiceLine.list().size() == 1) {
						additional = additionals.addChildElement("Additional", "web");
						additional.addChildElement("Name", "web")
								.addTextNode("CODIGO DEL ARTICULO VENDIDO");
						additional.addChildElement("Value", "web")
								.addTextNode(product.getSearchKey());
						additional = additionals.addChildElement("Additional", "web");
						additional.addChildElement("Name", "web")
								.addTextNode("CANTIDAD DE TRABAJADORES");
						additional.addChildElement("Value", "web")
								.addTextNode(invoiceLine.getDescription() != null
										? invoiceLine.getDescription()
										: "");
						additional = additionals.addChildElement("Additional", "web");
						additional.addChildElement("Name", "web").addTextNode("PRECIO UNITARIO");
						additional.addChildElement("Value", "web")
								.addTextNode(invoiceLine.getUnitPrice().toString());
						additional = additionals.addChildElement("Additional", "web");
						additional.addChildElement("Name", "web").addTextNode("DESCUENTO (%)");
						additional.addChildElement("Value", "web").addTextNode(
								(discount != null ? discount : new BigDecimal("0")).toString());
						additional = additionals.addChildElement("Additional", "web");
						additional.addChildElement("Name", "web").addTextNode("PRECIO TOTAL");
						additional.addChildElement("Value", "web")
								.addTextNode(invoiceLine.getLineNetAmount().toString());
						additional = additionals.addChildElement("Additional", "web");
						additional.addChildElement("Name", "web").addTextNode("IVA");
						additional.addChildElement("Value", "web").addTextNode(IVA.toString());
					}
				}
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web")
						.addTextNode("NOMBRE DEL ARTICULO VENDIDO");
				additional.addChildElement("Value", "web").addTextNode(
						lineInfo.length() > 500 ? lineInfo.substring(0, 500) : lineInfo);

				message.saveChanges();
				log.setJsonRequest(Scactu_Helper.xmlToString(message));
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();

				SOAPMessage response = connection.call(message, config.getIntizaWriteEndpoint());
				JSONObject json = Scactu_Helper.xmlToJSON(response);

				log.setJsonResponse(Scactu_Helper.xmlToString(response));
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();

				if (!json.has(action + "Response")) {
					throw new OBException("ERROR");
				}

				JSONObject result = json.getJSONObject(action + "Response")
						.getJSONObject(action + "Result");
				if (result.getInt("Received") > result.getInt("Processed")) {
					throw new OBException(result.getString("Description"));
				}

				ScactuSaleInvoiceData.update(conn, Scactu_Helper.getTimestampString(date),
						line.cInvoiceId);

				log.setResult("OK");
				log.setReferenceNo(line.documentno);
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();
			} catch (Exception e) {
				conn.getConnection().rollback();
				String message = Scactu_Helper.getErrorMessage(logger, e);
				processLogger.logln(message);
				Scactu_Log log = OBDal.getInstance().get(Scactu_Log.class, logId);
				if (log != null) {
					log.setResult("ERROR");
					log.setError(message);
					OBDal.getInstance().save(log);
					OBDal.getInstance().flush();
					conn.getConnection().commit();
				}
			}
		}
		connection.close();
	}

	private void paymentSync(ConnectionProvider conn, Scactu_Config config) throws Exception {
		String action = "SetPayments";

		Date date = new Date();
		ScactuPaymentData[] lines = ScactuPaymentData.select(conn);

		SOAPConnectionFactory connectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection connection = connectionFactory.createConnection();
		MessageFactory messageFactory = MessageFactory.newInstance();
		for (ScactuPaymentData line : lines) {
			if (killProcess) {
				throw new OBException("Process killed");
			}

			String logId = null;
			try {
				Scactu_Log log = OBProvider.getInstance().get(Scactu_Log.class);
				log.setInterface("I");
				log.setEndpoint(action);
				log.setRecordID(line.finPaymentId);
				log.setType("OUT");
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();
				logId = log.getId();

				SOAPMessage message = messageFactory.createMessage();
				SOAPPart part = message.getSOAPPart();

				// SOAP Envelope
				SOAPEnvelope envelope = part.getEnvelope();
				envelope.addNamespaceDeclaration("xsi",
						"http://www.w3.org/2001/XMLSchema-instance");
				envelope.addNamespaceDeclaration("web", config.getNamespace());

				MimeHeaders headers = message.getMimeHeaders();
				headers.addHeader("Content-Type", "text/xml; charset=utf-8");
				headers.addHeader("SOAPAction", config.getNamespace() + action);

				// SOAP Body
				SOAPBody body = envelope.getBody();
				SOAPElement setPayments = body.addChildElement(action, "web");
				setPayments.setAttribute("xmlns", config.getNamespace());
				setPayments.addChildElement("Uid", "web").addTextNode(config.getIntizaUser());
				setPayments.addChildElement("Pwd", "web").addTextNode(config.getIntizaPass());

				SOAPElement payments = setPayments.addChildElement("Payments", "web");
				SOAPElement payment = payments.addChildElement("Payment", "web");
				payment.addChildElement("Company_ID", "web").addTextNode(config.getIntizaCompany());
				payment.addChildElement("Client_ID", "web").addTextNode(line.taxid);
				payment.addChildElement("Invoice_ID", "web").addTextNode(line.iDocumentno);
				payment.addChildElement("Payment_ID", "web").addTextNode(line.pDocumentno);
				payment.addChildElement("Amount", "web").addTextNode(line.amount);
				payment.addChildElement("Date", "web").addTextNode(line.paymentdate);
				payment.addChildElement("Type", "web").addTextNode(line.paymentType);

				SOAPElement additionals = payment.addChildElement("Additionals", "web");
				SOAPElement additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("Asiento Contable");
				additional.addChildElement("Value", "web").addTextNode(line.pDocumentno);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("Concepto de ingreso");
				additional.addChildElement("Value", "web").addTextNode(line.financialAccount);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("Comprobante");
				additional.addChildElement("Value", "web").addTextNode(line.referenceno);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("Dep Cheque");
				additional.addChildElement("Value", "web").addTextNode(line.deposit);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("Nombre del banco");
				additional.addChildElement("Value", "web")
						.addTextNode(line.isCheck.equals("Y") || line.isWithholding.equals("Y")
								? line.bank
								: line.financialAccount);
				additional = additionals.addChildElement("Additional", "web");
				additional.addChildElement("Name", "web").addTextNode("Numero de cheque");
				additional.addChildElement("Value", "web")
						.addTextNode(line.isCheck.equals("Y") ? line.checkno : line.referenceno);

				message.saveChanges();
				log.setJsonRequest(Scactu_Helper.xmlToString(message));
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();

				SOAPMessage response = connection.call(message, config.getIntizaWriteEndpoint());
				JSONObject json = Scactu_Helper.xmlToJSON(response);

				log.setJsonResponse(Scactu_Helper.xmlToString(response));
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();

				if (!json.has(action + "Response")) {
					throw new OBException("ERROR");
				}

				JSONObject result = json.getJSONObject(action + "Response")
						.getJSONObject(action + "Result");
				if (result.getInt("Received") > result.getInt("Processed")) {
					throw new OBException(result.getString("Description"));
				}

				if (line.isWithholding.equals("N")) {
					ScactuPaymentData.update(conn, Scactu_Helper.getTimestampString(date),
							line.finPaymentId);
				} else {
					ScactuPaymentData.updateWithholdingSale(conn,
							Scactu_Helper.getTimestampString(date), line.finPaymentId);
				}

				log.setResult("OK");
				log.setReferenceNo(line.pDocumentno);
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				conn.getConnection().commit();
			} catch (Exception e) {
				conn.getConnection().rollback();
				String message = Scactu_Helper.getErrorMessage(logger, e);
				processLogger.logln(message);
				Scactu_Log log = OBDal.getInstance().get(Scactu_Log.class, logId);
				if (log != null) {
					log.setResult("ERROR");
					log.setError(message);
					OBDal.getInstance().save(log);
					OBDal.getInstance().flush();
					conn.getConnection().commit();
				}
			}
		}
		connection.close();
	}

}
