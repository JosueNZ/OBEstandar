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

import com.sidesoft.localization.ecuador.withholdings.Authorization;

public class AuthorizationEventHandle extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      Authorization.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    checkDateRangeNumberRange((Authorization) event.getTargetInstance());
    logger.info("Authorization " + event.getTargetInstance().getId() + " is being updated");
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    checkDateRangeNumberRange((Authorization) event.getTargetInstance());
    logger.info("Authorization " + event.getTargetInstance().getId() + " is being created");
  }

  private void checkDateRangeNumberRange(Authorization authorization) {
    boolean error = false;
    String error_msg = "";

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    OBCriteria<Authorization> obcAuthorization = OBDal.getInstance().createCriteria(
        Authorization.class);
    obcAuthorization.add(Restrictions.eq(Authorization.PROPERTY_DOCUMENTTYPE,
        authorization.getDocumentType()));
    obcAuthorization.add(Restrictions.eq(Authorization.PROPERTY_ESTABLISHMENT,
        authorization.getEstablishment()));
    obcAuthorization.add(Restrictions.eq(Authorization.PROPERTY_CASHREGISTER,
        authorization.getCashRegister()));
    obcAuthorization.add(Restrictions.ne(Authorization.PROPERTY_ID, authorization.getId()));

    for (Authorization auth : obcAuthorization.list()) {
      if (!(authorization.getNumberTo() < auth.getNumberFrom() || authorization.getNumberFrom() > auth
          .getNumberTo())) {
        error = true;
        error_msg = Utility.messageBD(conn, "SSWH_NumberRangeOverlap", language);
      }

      if (!(authorization.getEndingDate().compareTo(auth.getStartingDate()) < 0 || authorization
          .getStartingDate().compareTo(auth.getEndingDate()) > 0)) {
        if (error) {
          error_msg = error_msg + " - ";
        }

        error = true;
        error_msg = error_msg + Utility.messageBD(conn, "SSWH_DateRangeOverlap", language);
      }

      if (error) {
        logger.error(error_msg);
        throw new OBException(error_msg);
      }
    }
  }
}
