package ec.com.sidesoft.localization.inventory.events;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;

public class UpdateSequenceInventoryEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      InventoryCount.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final InventoryCount headerInventory = (InventoryCount) event.getTargetInstance();
    Sequence sequence = headerInventory.getSsinDoctype().getDocumentSequence();

    if (sequence == null) {
      throw new OBException(String.format(OBMessageUtils.messageBD("DocumentTypeSequenceNotFound")));
    }

    if (headerInventory.getSsinDocumentno() != null) {

      String seqnumber = headerInventory.getSsinDocumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity loansEntity = ModelProvider.getInstance().getEntity(
        		InventoryCount.ENTITY_NAME);
        final Property valueProperty = loansEntity
            .getProperty(InventoryCount.PROPERTY_SSINDOCUMENTNO);
        
        event.setCurrentState(valueProperty, subseqnumber );

      }
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
    }

  }

}
