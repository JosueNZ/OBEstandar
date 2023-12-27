package com.sidesoft.hrm.payroll.advanced.event;

import java.util.Date;
import java.util.List;

import javax.enterprise.event.Observes;
import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.hrm.payroll.Contract;
import com.sidesoft.hrm.payroll.advanced.SfprEvolutionSalary;

public class ValidateDateEvolutionSalary extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      SfprEvolutionSalary.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final SfprEvolutionSalary evolutionSalary = (SfprEvolutionSalary) event.getTargetInstance();

    ReturnQuery(evolutionSalary);

  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws ServletException {
    if (!isValidEvent(event)) {
      return;
    }

    final SfprEvolutionSalary evolutionSalary = (SfprEvolutionSalary) event.getTargetInstance();

    ReturnQuery(evolutionSalary);
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;

    }
    final SfprEvolutionSalary evolutionSalary = (SfprEvolutionSalary) event.getTargetInstance();
    DeleteEvoultionSalary(evolutionSalary);

  }

  private void DeleteEvoultionSalary(SfprEvolutionSalary Strserach) {

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    if (Strserach.getStatusEs().equals("E")) {
      throw new OBException(Utility.messageBD(conn, "@Sfpr_ErrorExpire@", language));

    }
  }

  private void ReturnQuery(SfprEvolutionSalary Strserach) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    Date V_StartDate = Strserach.getStartingDate();
    Date V_EndDate = Strserach.getEndingDate();

    int val = V_EndDate.compareTo(V_StartDate);
    if (val == -1) {
      throw new OBException(Utility.messageBD(conn, "@Sfpr_ErrorInvalidDates@", language));
    }

    String strcontract = Strserach.getId().toString();

    Contract contract = Strserach.getSsprContract();

    OBCriteria<SfprEvolutionSalary> UserCriteria = OBDal.getInstance().createCriteria(
        SfprEvolutionSalary.class);
    UserCriteria.add(Restrictions.or(Restrictions.and(
        Restrictions.lt(SfprEvolutionSalary.PROPERTY_STARTINGDATE, V_StartDate),
        Restrictions.ge(SfprEvolutionSalary.PROPERTY_ENDINGDATE, V_StartDate)), Restrictions.and(
        Restrictions.lt(SfprEvolutionSalary.PROPERTY_STARTINGDATE, V_EndDate),
        Restrictions.ge(SfprEvolutionSalary.PROPERTY_ENDINGDATE, V_EndDate))));
    UserCriteria.add(Restrictions.ne(SfprEvolutionSalary.PROPERTY_ID, strcontract));
    UserCriteria.add(Restrictions.eq(SfprEvolutionSalary.PROPERTY_SSPRCONTRACT, contract));

    List<SfprEvolutionSalary> ContractCriteriaList = UserCriteria.list();

    int NumUser = ContractCriteriaList.size();

    if (NumUser > 0) {
      // throw new OBException(Utility.messageBD(conn, "@20504@", language));
    }

    OBCriteria<SfprEvolutionSalary> EvoSal = OBDal.getInstance().createCriteria(
        SfprEvolutionSalary.class);
    EvoSal.add(Restrictions.ne(SfprEvolutionSalary.PROPERTY_ID, strcontract));
    EvoSal.add(Restrictions.eq(SfprEvolutionSalary.PROPERTY_SSPRCONTRACT, contract));
    EvoSal.add(Restrictions.eq(SfprEvolutionSalary.PROPERTY_STATUSES, "A"));
    if (EvoSal.count() >= 1) {
      throw new OBException(Utility.messageBD(conn, "@Sfpr_ValidateEvolutionSalaryExpire@",
          language));
    }
  }
}
