package ec.com.sidesoft.custom.payments.partialpayment.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.custom.payments.partialpayment.SsppPartialPaymentInfoConcept;

public class Sspp_ValidateFieldLength extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      SsppPartialPaymentInfoConcept.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final SsppPartialPaymentInfoConcept PartialPaymentInfoConcept = (SsppPartialPaymentInfoConcept) event
        .getTargetInstance();
    ReturnQuery(PartialPaymentInfoConcept);

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final SsppPartialPaymentInfoConcept PartialPaymentInfoConcept = (SsppPartialPaymentInfoConcept) event
        .getTargetInstance();
    ReturnQuery(PartialPaymentInfoConcept);

  }

  private void ReturnQuery(SsppPartialPaymentInfoConcept PartialPaymentInfoConcept) {

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    int CountLengthCode = String.valueOf(PartialPaymentInfoConcept.getCode()).length();

    if (CountLengthCode > 6) {

      throw new OBException(Utility.messageBD(conn, "@Sspp_ValidateLengthFieldValue@", language));
    }
  }
}
