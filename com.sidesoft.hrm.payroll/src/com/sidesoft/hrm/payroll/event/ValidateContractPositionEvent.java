package com.sidesoft.hrm.payroll.event;

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

import com.sidesoft.hrm.payroll.Contract;
import com.sidesoft.hrm.payroll.ContractPosition;

public class ValidateContractPositionEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ContractPosition.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final ContractPosition contractposition = (ContractPosition) event.getTargetInstance();
    ReturnQuery(contractposition);

  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws ServletException {
    if (!isValidEvent(event)) {
      return;
    }

    final ContractPosition contractposition1 = (ContractPosition) event.getTargetInstance();

    ReturnQueryUpdate(contractposition1);
  }

  private void ReturnQuery(ContractPosition Strserach) {

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    String strcontractid = Strserach.getSsprContract().getId().toString();

    Contract obdalContract = OBDal.getInstance().get(Contract.class, strcontractid);

    OBCriteria<ContractPosition> ConceptsCritria = OBDal.getInstance()
        .createCriteria(ContractPosition.class);
    ConceptsCritria.add(Restrictions.eq(ContractPosition.PROPERTY_SSPRCONTRACT, obdalContract));
    ConceptsCritria.add(Restrictions.eq(ContractPosition.PROPERTY_ACTIVE, true));

    List<ContractPosition> certificateCriteriaList = ConceptsCritria.list();
    int tm = certificateCriteriaList.size();

    if (tm > 0) {
      throw new OBException(Utility.messageBD(conn, "@Sspr_ValidateContracPosition@", language));
    }

  }

  private void ReturnQueryUpdate(ContractPosition Strserach) {

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    String strcontractid = Strserach.getSsprContract().getId().toString();
    Contract obdalContract = OBDal.getInstance().get(Contract.class, strcontractid);

    OBCriteria<ContractPosition> ConceptsCritria = OBDal.getInstance()
        .createCriteria(ContractPosition.class);
    ConceptsCritria.add(Restrictions.eq(ContractPosition.PROPERTY_SSPRCONTRACT, obdalContract));
    ConceptsCritria
        .add(Restrictions.ne(ContractPosition.PROPERTY_ID, Strserach.getId().toString()));
    ConceptsCritria.add(Restrictions.eq(ContractPosition.PROPERTY_ACTIVE, true));

    List<ContractPosition> certificateCriteriaList = ConceptsCritria.list();
    int tm = certificateCriteriaList.size();

    if (tm > 0) {
      throw new OBException(Utility.messageBD(conn, "@Sspr_ValidateContracPosition@", language));
    }
  }

}
