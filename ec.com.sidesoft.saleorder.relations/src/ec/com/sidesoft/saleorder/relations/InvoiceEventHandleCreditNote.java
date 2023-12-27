package ec.com.sidesoft.saleorder.relations;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.common.invoice.Invoice;

public class InvoiceEventHandleCreditNote extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());
  private static String DOCUMENT_CATEGORY_RETURN_MATERIAL = "ARI_RM";

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    Invoice currentInvoice = (Invoice) event.getTargetInstance();

    if (currentInvoice.isSalesTransaction()) {

      final Property invoiceIsReferenceProperty = event.getTargetInstance().getEntity()
          .getProperty(Invoice.PROPERTY_SCNRISREFINV);

      Boolean scnrIsrefInvCurrentState = (Boolean) event
          .getCurrentState(invoiceIsReferenceProperty);

      if (scnrIsrefInvCurrentState == true) {
        final Property invoiceReferenceProperty = event.getTargetInstance().getEntity()
            .getProperty(Invoice.PROPERTY_SCNRINVOICE);
        Invoice invoice = (Invoice) event.getCurrentState(invoiceReferenceProperty);

        if (invoice != null && invoice.getSalesOrder() != null) {
          final Property salesOrderProperty = event.getTargetInstance().getEntity()
              .getProperty(Invoice.PROPERTY_SALESORDER);
          event.setCurrentState(salesOrderProperty, invoice.getSalesOrder());
        }
      }
    }

    logger.info("Invoice " + event.getTargetInstance().getId() + " is being created");
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    Invoice currentInvoice = (Invoice) event.getTargetInstance();

    if (currentInvoice.isSalesTransaction()) {

      final Property invoiceIsReferenceProperty = event.getTargetInstance().getEntity()
          .getProperty(Invoice.PROPERTY_SCNRISREFINV);

      Boolean scnrIsrefInvCurrentState = (Boolean) event
          .getCurrentState(invoiceIsReferenceProperty);

      if (scnrIsrefInvCurrentState == true) {
        final Property invoiceReferenceProperty = event.getTargetInstance().getEntity()
            .getProperty(Invoice.PROPERTY_SCNRINVOICE);
        Invoice invoice = (Invoice) event.getCurrentState(invoiceReferenceProperty);

        if (invoice != null && invoice.getSalesOrder() != null) {
          final Property salesOrderProperty = event.getTargetInstance().getEntity()
              .getProperty(Invoice.PROPERTY_SALESORDER);
          event.setCurrentState(salesOrderProperty, invoice.getSalesOrder());
        }
      }
    }

    logger.info("Invoice " + event.getTargetInstance().getId() + " is being updated");
  }

}
