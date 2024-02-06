package com.sidesoft.ecuador.asset.allocation.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;

import com.sidesoft.ecuador.asset.allocation.SsalAssetReturn;

public class UpdateNumberRequestEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
		  SsalAssetReturn.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final SsalAssetReturn AssetReturnObj = (SsalAssetReturn) event.getTargetInstance();

    if (AssetReturnObj.getDocumentNo() != null) {

      String seqnumber = AssetReturnObj.getDocumentNo();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity loansEntity = ModelProvider.getInstance().getEntity(
        		SsalAssetReturn.ENTITY_NAME);
        final Property valueProperty = loansEntity
            .getProperty(SsalAssetReturn.PROPERTY_DOCUMENTNO);
        
        event.setCurrentState(valueProperty, subseqnumber );

      }

      Sequence sequence = AssetReturnObj.getDocumentType().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
    }

  }

}
