package com.sidesoft.hrm.payroll.event;

import java.util.Date;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import com.sidesoft.hrm.payroll.ssprreadmissions;

public class ValidateReadmission extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ssprreadmissions.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateremission(event);
  }

  public void onUpdate(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateremission(event);
  }

  private void validateremission(EntityNewEvent event) {

    final ssprreadmissions readmission = (ssprreadmissions) event.getTargetInstance();

    Date dtStartingDate = readmission.getStartingDate();
    String strEmployeeId = readmission.getEmployee().getId();

    BusinessPartner partner = OBDal.getInstance().get(BusinessPartner.class, strEmployeeId);

    OBCriteria<ssprreadmissions> ObjReadmissions = OBDal.getInstance()
        .createCriteria(ssprreadmissions.class);
    ObjReadmissions.add(Restrictions.eq(ssprreadmissions.PROPERTY_EMPLOYEE, partner));
    ObjReadmissions.add(Restrictions.le(ssprreadmissions.PROPERTY_STARTINGDATE, dtStartingDate));
    ObjReadmissions.add(Restrictions.isNull(ssprreadmissions.PROPERTY_ENDINGDATE));

    if (ObjReadmissions.count() > 0) {
      throw new OBException("@Sspr_readmission@");
    }

  }

}
