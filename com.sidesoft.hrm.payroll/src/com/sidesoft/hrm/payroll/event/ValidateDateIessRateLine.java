package com.sidesoft.hrm.payroll.event;

import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.servlet.ServletException;

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

import com.sidesoft.hrm.payroll.sspr_iessrate;
import com.sidesoft.hrm.payroll.sspr_iessrateline;

public class ValidateDateIessRateLine extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      sspr_iessrateline.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final sspr_iessrateline iessrateline = (sspr_iessrateline) event.getTargetInstance();

    ReturnQuery(iessrateline);

  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws ServletException {
    if (!isValidEvent(event)) {
      return;
    }

    final sspr_iessrateline iessrateline = (sspr_iessrateline) event.getTargetInstance();

    ReturnQuery(iessrateline);
  }

  private void ReturnQuery(sspr_iessrateline Strserach) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    Date V_StartDate = Strserach.getValidfrom();
    Date V_EndDate = Strserach.getValidto();

    int val = V_EndDate.compareTo(V_StartDate);
    if (val == -1) {
      throw new OBException(Utility.messageBD(conn, "@20504@", language));
    }

    String striessrate = Strserach.getId().toString();

    sspr_iessrate iessrate = Strserach.getSsprIessrate();

    OBCriteria<sspr_iessrateline> UserCriteria = OBDal.getInstance().createCriteria(
        sspr_iessrateline.class);
    UserCriteria.add(Restrictions.or(Restrictions.and(
        Restrictions.lt(sspr_iessrateline.PROPERTY_VALIDFROM, V_StartDate),
        Restrictions.ge(sspr_iessrateline.PROPERTY_VALIDTO, V_StartDate)), Restrictions.and(
        Restrictions.lt(sspr_iessrateline.PROPERTY_VALIDFROM, V_EndDate),
        Restrictions.ge(sspr_iessrateline.PROPERTY_VALIDTO, V_EndDate))));
    UserCriteria.add(Restrictions.ne(sspr_iessrateline.PROPERTY_ID, striessrate));
    UserCriteria.add(Restrictions.eq(sspr_iessrateline.PROPERTY_SSPRIESSRATE, iessrate));

    List<sspr_iessrateline> ContractCriteriaList = UserCriteria.list();

    int NumUser = ContractCriteriaList.size();

    if (NumUser > 0) {
      throw new OBException(Utility.messageBD(conn, "@20504@", language));
    }
  }
}
