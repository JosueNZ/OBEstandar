package ec.com.sidesoft.ecuador.asset.allocation.advanced.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;

import ec.com.sidesoft.ecuador.asset.allocation.advanced.CsaaaInventoryTaking;

public class UpdateSequenceInventoryTaking extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(CsaaaInventoryTaking.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final CsaaaInventoryTaking eventInvTaking = (CsaaaInventoryTaking) event.getTargetInstance();

    if (eventInvTaking.getDocumentno() != null) {

      String seqnumber = eventInvTaking.getDocumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity InvTakingEntity = ModelProvider.getInstance()
            .getEntity(CsaaaInventoryTaking.ENTITY_NAME);
        final Property valueProperty = InvTakingEntity
            .getProperty(CsaaaInventoryTaking.PROPERTY_DOCUMENTNO);

        event.setCurrentState(valueProperty, subseqnumber);

      }
      Sequence sequence = eventInvTaking.getDoctype().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());

    }

  }

}
