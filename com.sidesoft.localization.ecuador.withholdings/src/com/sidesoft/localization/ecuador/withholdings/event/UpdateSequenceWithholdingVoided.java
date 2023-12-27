package com.sidesoft.localization.ecuador.withholdings.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;

import com.sidesoft.localization.ecuador.withholdings.SswhWithholdingsVoided;

public class UpdateSequenceWithholdingVoided extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      SswhWithholdingsVoided.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final SswhWithholdingsVoided WithholdingVoided = (SswhWithholdingsVoided) event
        .getTargetInstance();

    /*
     * if (WithholdingVoided.getStablishment() != null) {
     * 
     * ConnectionProvider conn = new DalConnectionProvider(false); String language =
     * OBContext.getOBContext().getLanguage().getLanguage();
     * 
     * String V_WithholdingStablishment = WithholdingVoided.getStablishment(); String
     * V_WithholdingShell = WithholdingVoided.getShell(); String V_WithholdingSeriesFrom =
     * WithholdingVoided.getReferencenoFrom(); String V_WithholdingSeriesTo =
     * WithholdingVoided.getReferencenoTo();
     * 
     * OBCriteria<SswhWithholdingsVoided> OBSswhWithholdingVoided = OBDal.getInstance()
     * .createCriteria(SswhWithholdingsVoided.class); OBSswhWithholdingVoided
     * .add(Restrictions.or(Restrictions.and(Restrictions.lt(
     * SswhWithholdingsVoided.PROPERTY_REFERENCENOFROM, V_WithholdingSeriesFrom),
     * Restrictions.ge(SswhWithholdingsVoided.PROPERTY_REFERENCENOTO, V_WithholdingSeriesFrom)),
     * Restrictions.and(Restrictions.lt(SswhWithholdingsVoided.PROPERTY_REFERENCENOTO,
     * V_WithholdingSeriesTo), Restrictions.ge( SswhWithholdingsVoided.PROPERTY_REFERENCENOTO,
     * V_WithholdingSeriesTo))));
     * 
     * if (OBSswhWithholdingVoided.count() >= 1) { throw new OBException(Utility.messageBD(conn,
     * "@Las serie desde y/o hasta se encuentran registradas@", language)); }
     * 
     * OBCriteria<SswhWithholdingsVoided> OBSswhWithholdingVoided2 = OBDal.getInstance()
     * .createCriteria(SswhWithholdingsVoided.class);
     * OBSswhWithholdingVoided2.add(Restrictions.eq(SswhWithholdingsVoided.PROPERTY_REFERENCENOFROM,
     * V_WithholdingSeriesFrom));
     * 
     * OBSswhWithholdingVoided2.add(Restrictions.eq(SswhWithholdingsVoided.PROPERTY_REFERENCENOTO,
     * V_WithholdingSeriesTo));
     * 
     * OBSswhWithholdingVoided2.add(Restrictions.eq(SswhWithholdingsVoided.PROPERTY_SHELL,
     * V_WithholdingShell));
     * OBSswhWithholdingVoided2.add(Restrictions.eq(SswhWithholdingsVoided.PROPERTY_STABLISHMENT,
     * V_WithholdingStablishment));
     * 
     * if (OBSswhWithholdingVoided.count() >= 1) { throw new OBException( Utility .messageBD( conn,
     * "@No puede repetirse la misma serie desde y hasta con el mismo establecimiento y  punto de emisión@"
     * , language)); }
     * 
     * }
     */

    if (WithholdingVoided.getDocumentNo() != null) {

      String seqnumber = WithholdingVoided.getDocumentNo();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity WithholdingEntity = ModelProvider.getInstance().getEntity(
            SswhWithholdingsVoided.ENTITY_NAME);
        final Property valueProperty = WithholdingEntity
            .getProperty(SswhWithholdingsVoided.PROPERTY_DOCUMENTNO);

        event.setCurrentState(valueProperty, subseqnumber);

      }

    }

  }

}
