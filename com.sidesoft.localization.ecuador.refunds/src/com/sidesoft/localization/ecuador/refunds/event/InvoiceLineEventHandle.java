package com.sidesoft.localization.ecuador.refunds.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.InvoiceLine;

public class InvoiceLineEventHandle extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      InvoiceLine.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    InvoiceLine ref = ((InvoiceLine) event.getTargetInstance()).getSsreRefundedinvlineRef();
    if (ref != null) {
      ref.setSsreIsrefunded(false);
      OBDal.getInstance().save(ref);
      OBDal.getInstance().flush();
    }
    logger.info("Invoice line" + event.getTargetInstance().getId() + " is being deleted");
  }
}
