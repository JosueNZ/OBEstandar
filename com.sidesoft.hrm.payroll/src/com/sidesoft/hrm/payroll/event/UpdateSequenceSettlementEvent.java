package com.sidesoft.hrm.payroll.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;

import com.sidesoft.hrm.payroll.sspr_settlement;

public class UpdateSequenceSettlementEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      sspr_settlement.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final sspr_settlement settlement = (sspr_settlement) event.getTargetInstance();

    if (settlement.getDocumentnonew() != null) {

      String seqnumber = settlement.getDocumentnonew();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity settlementEntity = ModelProvider.getInstance().getEntity(
            sspr_settlement.ENTITY_NAME);
        final Property valueProperty = settlementEntity
            .getProperty(sspr_settlement.PROPERTY_DOCUMENTNONEW);

        event.setCurrentState(valueProperty, subseqnumber);

      }

      Sequence sequence = settlement.getDoctype().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
    }

  }

}
