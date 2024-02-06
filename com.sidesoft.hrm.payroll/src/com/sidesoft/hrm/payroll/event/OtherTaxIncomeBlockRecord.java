package com.sidesoft.hrm.payroll.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.hrm.payroll.Sspr_OtherTaxIncome;
import com.sidesoft.hrm.payroll.Sspr_OtherTaxIncomeLine;

public class OtherTaxIncomeBlockRecord extends EntityPersistenceEventObserver {
    private static Entity[] entities = {
            ModelProvider.getInstance().getEntity(Sspr_OtherTaxIncome.ENTITY_NAME),
            ModelProvider.getInstance().getEntity(Sspr_OtherTaxIncomeLine.ENTITY_NAME) };

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    @Override
    protected Entity[] getObservedEntities() {
        return entities;
    }

    public void onSave(@Observes EntityNewEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
    }

    public void onUpdate(@Observes EntityUpdateEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
    }

    public void onDelete(@Observes EntityDeleteEvent event) {
        if (!isValidEvent(event)) {
            return;
        }

        try {
            switch (event.getTargetInstance().getEntityName()) {
                case Sspr_OtherTaxIncome.ENTITY_NAME:
                    Sspr_OtherTaxIncome otherTaxIncome = (Sspr_OtherTaxIncome) event.getTargetInstance();
                    if (otherTaxIncome.isProcessed()) {
                        throw new OBException("@DocumentProcessed@");
                    }
                    break;
                case Sspr_OtherTaxIncomeLine.ENTITY_NAME:
                    Sspr_OtherTaxIncomeLine otherTaxIncomeLine = (Sspr_OtherTaxIncomeLine) event.getTargetInstance();
                    if (otherTaxIncomeLine.getSsprOtherTaxIncome().isProcessed()) {
                        throw new OBException("@DocumentProcessed@");
                    }
                    break;
            }
        } catch (Exception e) {
            String message = Utility.messageBD(conn, e.getMessage(), language);
            try {
                conn.destroy();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            throw new OBException(message);

        }
    }

}
