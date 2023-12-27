package com.sidesoft.localization.ecuador.withholdings.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.localization.ecuador.withholdings.SswhFormulary;
import com.sidesoft.localization.ecuador.withholdings.SswhFormularyLine;

public class Sswh_FormularyEvent extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      SswhFormulary.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    logger.info("Formulary " + event.getTargetInstance().getId() + " is being deleted");

    final SswhFormulary sswhForm = (SswhFormulary) event.getTargetInstance();
    boolean error = false;
    String error_msg = "";
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    OBCriteria<SswhFormularyLine> obcForm = OBDal.getInstance().createCriteria(
        SswhFormularyLine.class);
    obcForm.add(Restrictions.eq(SswhFormularyLine.PROPERTY_SSWHFORMULARY, sswhForm));

    if (obcForm.count() > 0 && sswhForm.getStatus().equals("DR")) {
      error = true;
      error_msg = Utility.messageBD(conn, "Sswh_FormularyHeader", language);

      if (error) {
        logger.error(error_msg);
        throw new OBException(error_msg);
      }
    }
  }

}
