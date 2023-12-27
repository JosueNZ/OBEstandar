package com.sidesoft.localization.ecuador.withholdings.event;

import javax.enterprise.event.Observes;
import java.sql.Timestamp;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.service.db.DalConnectionProvider;

public class InvoiceEventHandle extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    checkReference((Invoice) event.getTargetInstance());
    logger.info("Invoice " + event.getTargetInstance().getId() + " is being created");
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    checkReference((Invoice) event.getTargetInstance());
    logger.info("Invoice " + event.getTargetInstance().getId() + " is being updated");
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Invoice invoice = (Invoice) event.getTargetInstance();
    Invoice InvoiceObj = OBDal.getInstance().get(Invoice.class, invoice.getId().toString());

    OBCriteria<InvoiceLine> obcInvoiceLine = OBDal.getInstance().createCriteria(InvoiceLine.class);
    obcInvoiceLine.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE, InvoiceObj));

    String error_msg = "";
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    if (obcInvoiceLine.count() > 0) {
      error_msg = Utility.messageBD(conn, "Sswh_DocumentInvoiceDeleteFailed", language);
      logger.error(error_msg);
      throw new OBException(error_msg);
    }

  }

  private void checkReference(Invoice invoice) {
    String error_msg = "";
	String entrada = "2018-07-01 00:00:00";

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    OBCriteria<Invoice> obcInvoice = OBDal.getInstance().createCriteria(Invoice.class,"invoice");
    obcInvoice.createAlias("invoice.transactionDocument", "doctype");
    obcInvoice.add(Restrictions.eq(Invoice.PROPERTY_CLIENT, invoice.getClient()));
    obcInvoice.add(Restrictions.eq(Invoice.PROPERTY_ORDERREFERENCE, invoice.getOrderReference()));
    obcInvoice.add(Restrictions.eq(Invoice.PROPERTY_BUSINESSPARTNER, invoice.getBusinessPartner()));
	obcInvoice.add(Restrictions.ge(Invoice.PROPERTY_CREATIONDATE, Timestamp.valueOf(entrada)));

    obcInvoice
        .add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, invoice.isSalesTransaction()));
    obcInvoice.add(Restrictions.ne(Invoice.PROPERTY_ID, invoice.getId()));
    obcInvoice.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION,false ));
    obcInvoice.add(Restrictions.eq("doctype.return",false));
    obcInvoice.add(Restrictions.ne(Invoice.PROPERTY_DOCUMENTSTATUS,"VO"));
    obcInvoice.add(Restrictions.eq("doctype.id", invoice.getTransactionDocument().getId()));
    obcInvoice.add(Restrictions.eq(Invoice.PROPERTY_SSWHLIVELIHOOD,invoice.getSswhLivelihood()));
    
    if (obcInvoice.count() > 0 && !invoice.getDocumentType().isReturn()) {

      error_msg = Utility.messageBD(conn, "SSWH_NoReferenceNo", language);
      logger.error(error_msg);
      throw new OBException(error_msg + " - " +invoice.getOrderReference());
    }
  }
}
