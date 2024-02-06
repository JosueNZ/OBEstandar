package com.sidesoft.hrm.payroll.advanced.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;
import com.sidesoft.hrm.payroll.ssprloans;

public class UpdateSequenceLoansEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ssprloans.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final ssprloans loans = (ssprloans) event.getTargetInstance();

    if (loans.getSfprDocumentno() != null) {

      String seqnumber = loans.getSfprDocumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity loansEntity = ModelProvider.getInstance().getEntity(
            ssprloans.ENTITY_NAME);
        final Property valueProperty = loansEntity
            .getProperty(ssprloans.PROPERTY_SFPRDOCUMENTNO);
        
        event.setCurrentState(valueProperty, subseqnumber );

      }

      Sequence sequence = loans.getSfprCDoctype().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
    }

  }

}
