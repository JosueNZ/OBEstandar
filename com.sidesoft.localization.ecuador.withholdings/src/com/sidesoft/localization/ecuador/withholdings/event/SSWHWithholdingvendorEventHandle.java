package com.sidesoft.localization.ecuador.withholdings.event;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.localization.ecuador.withholdings.SSWHWithholdingvendor;

public class SSWHWithholdingvendorEventHandle extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      SSWHWithholdingvendor.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    checkDateRangeNumberRange((SSWHWithholdingvendor) event.getTargetInstance());
    logger.info("SSWHWithholdingvendor " + event.getTargetInstance().getId() + " is being updated");
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    checkDateRangeNumberRange((SSWHWithholdingvendor) event.getTargetInstance());
    logger.info("SSWHWithholdingvendor " + event.getTargetInstance().getId() + " is being created");
  }

  private void checkDateRangeNumberRange(SSWHWithholdingvendor Withholdingvendor) {
    boolean error = false;
    String error_msg = "";

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    OBCriteria<SSWHWithholdingvendor> obcWithholdingvendor = OBDal.getInstance().createCriteria(
        SSWHWithholdingvendor.class);
    obcWithholdingvendor.add(Restrictions.eq(SSWHWithholdingvendor.PROPERTY_BUSINESSPARTNER,
        Withholdingvendor.getBusinessPartner()));
    obcWithholdingvendor.add(Restrictions.eq(SSWHWithholdingvendor.PROPERTY_STABLISHMENT,
        Withholdingvendor.getStablishment()));
    obcWithholdingvendor.add(Restrictions.eq(SSWHWithholdingvendor.PROPERTY_SHELL,
        Withholdingvendor.getShell()));
    obcWithholdingvendor.add(Restrictions.ne(SSWHWithholdingvendor.PROPERTY_ID,
        Withholdingvendor.getId()));

    for (SSWHWithholdingvendor whvendor : obcWithholdingvendor.list()) {
      if (!(Withholdingvendor.getNumberTo() < whvendor.getNumberFrom() || Withholdingvendor
          .getNumberFrom() > whvendor.getNumberTo())) {
        error = true;
        error_msg = Utility.messageBD(conn, "SSWH_NumberRangeOverlap", language);
      }

      /*if (!(Withholdingvendor.getEndingDate().compareTo(whvendor.getStartingDate()) < 0 || Withholdingvendor
          .getStartingDate().compareTo(whvendor.getEndingDate()) > 0)) {
        if (error) {
          error_msg = error_msg + " - ";
        }

        error = true;
        error_msg = error_msg + Utility.messageBD(conn, "SSWH_DateRangeOverlap", language);
      }*/

      if (error) {
        logger.error(error_msg);
        throw new OBException(error_msg);
      }
    }
  }
}
