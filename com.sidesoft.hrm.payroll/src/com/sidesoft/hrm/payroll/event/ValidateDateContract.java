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
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.hrm.payroll.Contract;

public class ValidateDateContract extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Contract.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Contract contract = (Contract) event.getTargetInstance();

    ReturnQuery(contract);

  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws ServletException {
    if (!isValidEvent(event)) {
      return;
    }

    final Contract contract = (Contract) event.getTargetInstance();

    ReturnQuery(contract);
  }

  private void ReturnQuery(Contract Strserach) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    Date V_StartDate = Strserach.getStartdate();
    Date V_EndDate = Strserach.getEnddate();

    int val = V_EndDate.compareTo(V_StartDate);
    if (val == -1) {
      throw new OBException(Utility.messageBD(conn, "@20504@", language));
    }

    String v_c_bpartner_id = Strserach.getId().toString();

    BusinessPartner StrBusinessPartner = Strserach.getBusinessPartner();

    OBCriteria<Contract> UserCriteria = OBDal.getInstance().createCriteria(Contract.class);
    UserCriteria.add(Restrictions.or(
        Restrictions.and(Restrictions.lt(Contract.PROPERTY_STARTDATE, V_StartDate),
            Restrictions.ge(Contract.PROPERTY_ENDDATE, V_StartDate)),
        Restrictions.and(Restrictions.lt(Contract.PROPERTY_STARTDATE, V_EndDate),
            Restrictions.ge(Contract.PROPERTY_ENDDATE, V_EndDate))));
    UserCriteria.add(Restrictions.ne(Contract.PROPERTY_ID, v_c_bpartner_id));
    UserCriteria.add(Restrictions.eq(Contract.PROPERTY_BUSINESSPARTNER, StrBusinessPartner));

    List<Contract> ContractCriteriaList = UserCriteria.list();

    int NumUser = ContractCriteriaList.size();

    if (NumUser > 0) {
      throw new OBException(Utility.messageBD(conn, "@20504@", language));
    }
  }
}
