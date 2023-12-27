package ec.com.sidesoft.asset.transfer.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;

import ec.com.sidesoft.asset.transfer.SsatrAssetTransfer;

public class UpdateSequenceTransfer extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(SsatrAssetTransfer.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final SsatrAssetTransfer transfer = (SsatrAssetTransfer) event.getTargetInstance();

    if (transfer.getDocumentno() != null) {

      String seqnumber = transfer.getDocumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity transferEntity = ModelProvider.getInstance()
            .getEntity(SsatrAssetTransfer.ENTITY_NAME);
        final Property valueProperty = transferEntity
            .getProperty(SsatrAssetTransfer.PROPERTY_DOCUMENTNO);

        event.setCurrentState(valueProperty, subseqnumber);

      }

      Sequence sequence = transfer.getDoctype().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
    }

  }

}
