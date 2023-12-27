package ec.com.sidesoft.importdata.payments.ad_events;

import javax.enterprise.event.Observes;

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

import ec.com.sidesoft.importdata.payments.Simppys_PaymentDataUpload;
import ec.com.sidesoft.importdata.payments.Simppys_PaymentDetail;

public class Simppys_BlockRecord extends EntityPersistenceEventObserver {
    private static Entity[] entities = {
            ModelProvider.getInstance().getEntity(Simppys_PaymentDataUpload.ENTITY_NAME),
            ModelProvider.getInstance().getEntity(Simppys_PaymentDetail.ENTITY_NAME) };

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
                case Simppys_PaymentDataUpload.ENTITY_NAME:
                    Simppys_PaymentDataUpload paymentDataUpload = (Simppys_PaymentDataUpload) event.getTargetInstance();
                    if (paymentDataUpload.isProcessed()) {
                        throw new OBException("@DocumentProcessed@");
                    }

                    OBCriteria<Simppys_PaymentDetail> qPDetail = OBDal.getInstance()
                            .createCriteria(Simppys_PaymentDetail.class);
                    qPDetail.add(Restrictions.eq(Simppys_PaymentDetail.PROPERTY_SIMPPYSPAYMENTDATAUPLOAD,
                            paymentDataUpload));
                    qPDetail.add(Restrictions.eq(Simppys_PaymentDetail.PROPERTY_PROCESSED,
                            true));
                    if (qPDetail.list().size() > 0) {
                        throw new OBException("@DocumentProcessed@");
                    }
                    break;
                case Simppys_PaymentDetail.ENTITY_NAME:
                    Simppys_PaymentDetail pDetail = (Simppys_PaymentDetail) event.getTargetInstance();
                    if (pDetail.isProcessed()) {
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
