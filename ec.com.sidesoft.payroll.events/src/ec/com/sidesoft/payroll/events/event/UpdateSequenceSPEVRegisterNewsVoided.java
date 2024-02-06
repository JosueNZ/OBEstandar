package ec.com.sidesoft.payroll.events.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.ad.utility.Sequence;

import ec.com.sidesoft.payroll.events.SPEVRegisterNews;

public class UpdateSequenceSPEVRegisterNewsVoided extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(SPEVRegisterNews.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final SPEVRegisterNews SPEVRegisterNews = (SPEVRegisterNews) event.getTargetInstance();

    if (SPEVRegisterNews.getDoumentno() != null) {

      String seqnumber = SPEVRegisterNews.getDoumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity SPEVRegisterNewEntity = ModelProvider.getInstance()
            .getEntity(SPEVRegisterNews.ENTITY_NAME);
        final Property valueProperty = SPEVRegisterNewEntity
            .getProperty(SPEVRegisterNews.PROPERTY_DOUMENTNO);

        event.setCurrentState(valueProperty, subseqnumber);

      }

      Sequence sequence = SPEVRegisterNews.getDoctypetarget().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());

    }

  }

}
