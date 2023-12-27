package com.sidesoft.localization.ecuador.withholdings.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;

import com.sidesoft.localization.ecuador.withholdings.CsspidSalesTickets;

public class SalesTicketBusinessEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
    CsspidSalesTickets.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final CsspidSalesTickets SalesTickets = (CsspidSalesTickets) event
        .getTargetInstance();

   

    if (SalesTickets.getDocumentno() != null) {

      String seqnumber = SalesTickets.getDocumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity WithholdingEntity = ModelProvider.getInstance().getEntity(
            CsspidSalesTickets.ENTITY_NAME);
        final Property valueProperty = WithholdingEntity
            .getProperty(CsspidSalesTickets.PROPERTY_DOCUMENTNO);

        event.setCurrentState(valueProperty, subseqnumber);

      }
      Sequence sequence = SalesTickets.getDoctype().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());

    }

  }

}
