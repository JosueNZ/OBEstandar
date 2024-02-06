package com.sidesoft.localization.ecuador.withholdings.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;

import com.sidesoft.localization.ecuador.withholdings.SswhWithhCardCredit;

public class Sswh_UpdateDocumentWithholdingCardEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      SswhWithhCardCredit.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final SswhWithhCardCredit WithholdingCard = (SswhWithhCardCredit) event.getTargetInstance();

    if (WithholdingCard.getDocumentno() != null) {

      String seqnumber = WithholdingCard.getDocumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity WithholdingEntity = ModelProvider.getInstance().getEntity(
            SswhWithhCardCredit.ENTITY_NAME);
        final Property valueProperty = WithholdingEntity
            .getProperty(SswhWithhCardCredit.PROPERTY_DOCUMENTNO);

        event.setCurrentState(valueProperty, subseqnumber);

      }

      Sequence sequence = WithholdingCard.getDoctype().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());

    }

  }

}
