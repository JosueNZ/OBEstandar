package com.sidesoft.hrm.payroll.event;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

import com.sidesoft.hrm.payroll.Contract;

public class ValidateContractLiquidated extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Contract.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Contract ContractEmploye = (Contract) event.getTargetInstance();

    // OBTENGO TODOS LOS CONTRATOS DEL TERCERO
    OBCriteria<Contract> ContractObj = OBDal.getInstance().createCriteria(Contract.class);
    ContractObj.add(Restrictions.eq(Contract.PROPERTY_BUSINESSPARTNER,
        ContractEmploye.getBusinessPartner()));

    // VALIDA SI TIENE ALGUN CONTRATO
    if (ContractObj.count() > 0) {
      ContractObj.addOrderBy(Contract.PROPERTY_STARTDATE, false);

      Boolean StrLiquidation = ContractObj.list().get(0).isStatusliquidation();

      // VALIDA SI EL ULTIMO CONTRATO ESTA LIQUIDADO
      if (StrLiquidation == false) {
        throw new OBException(
            "Error: No se puede Ingresar un Nuevo Contrato - Debe Liquidar el Contrato Anterior");
      }
    }
  }

}
