package com.sidesoft.hrm.payroll.event;

import javax.enterprise.event.Observes;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.hrm.payroll.ssprpayrollaut;

public class SsprPayrollAutEventHandler extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ssprpayrollaut.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    final ssprpayrollaut ssprPayrollAut = (ssprpayrollaut) event.getTargetInstance();

    if (getIsProcessed(ssprPayrollAut)) {
      throw new OBException(
          Utility.messageBD(conn, "@SSPR_NotDeleteProcessedPayrollOut@", language));
    }

  }

  private boolean getIsProcessed(ssprpayrollaut ssprPayrollAut) {
    String sql = " select 1 from sspr_payroll_aut pa "
        + "  where pa.sspr_payroll_aut_id = :ssprPayrollAutId and pa.processed = 'Y' ";
    Session session = OBDal.getInstance().getSession();
    SQLQuery qry = session.createSQLQuery(sql);
    qry.setParameter("ssprPayrollAutId", ssprPayrollAut.getId());

    if (qry.list().size() > 0) {
      return true;
    }
    return false;

  }

}
