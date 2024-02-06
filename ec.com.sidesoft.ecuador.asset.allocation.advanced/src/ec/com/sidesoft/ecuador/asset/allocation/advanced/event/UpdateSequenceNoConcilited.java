package ec.com.sidesoft.ecuador.asset.allocation.advanced.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;

import ec.com.sidesoft.ecuador.asset.allocation.advanced.CsaaaCustodian;

public class UpdateSequenceNoConcilited extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(CsaaaCustodian.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final CsaaaCustodian eventCustodium = (CsaaaCustodian) event.getTargetInstance();

    if (eventCustodium.getDocumentno() != null) {

      String seqnumber = eventCustodium.getDocumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity CustodiumEntity = ModelProvider.getInstance()
            .getEntity(CsaaaCustodian.ENTITY_NAME);
        final Property valueProperty = CustodiumEntity
            .getProperty(CsaaaCustodian.PROPERTY_DOCUMENTNO);

        event.setCurrentState(valueProperty, subseqnumber);

      }
      Sequence sequence = eventCustodium.getDoctype().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());

    }

  }

}
