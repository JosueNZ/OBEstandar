package ec.com.sidesoft.localization.importdata.loadvouchers.bussinessevent;

import java.util.List;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.utility.Sequence;

import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvVoucherData;



public class UpdateSequenceImportDataEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ImdlvVoucherData.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final ImdlvVoucherData imporData = (ImdlvVoucherData) event.getTargetInstance();


    if (imporData.getDocumentno() != null) {

      String seqnumber = imporData.getDocumentno();

      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        final Entity settlementEntity = ModelProvider.getInstance().getEntity(
        		ImdlvVoucherData.ENTITY_NAME);
        final Property valueProperty = settlementEntity
            .getProperty(ImdlvVoucherData.PROPERTY_DOCUMENTNO);

        event.setCurrentState(valueProperty, subseqnumber);

      }

      Sequence sequence = imporData.getDoctype().getDocumentSequence();
      sequence.setNextAssignedNumber(sequence.getNextAssignedNumber() + sequence.getIncrementBy());
    }

  }
  /*public void onDelete(@Observes EntityDeleteEvent event) {
	    final CsspidPurchaseeImportData headerImporData  = (CsspidPurchaseeImportData) event.getTargetInstance();
	    
	    String strID = headerImporData.getId().toString();
		CsspidPurchaseeImportData imporData = OBDal.getInstance().get(CsspidPurchaseeImportData.class, strID );

	    OBCriteria<CsspidPurchaseLineTemp> obcLinesTmp = OBDal.getInstance().createCriteria(
				CsspidPurchaseLineTemp.class);
		obcLinesTmp.add(Restrictions.eq(CsspidPurchaseLineTemp.PROPERTY_CSSPIDPURCHASEIMPDATA,
				imporData));
		if(obcLinesTmp.list().size()>0) {
			List<CsspidPurchaseLineTemp> listLinestmp= obcLinesTmp.list();
			for (CsspidPurchaseLineTemp colLinestmp: listLinestmp) {
				CsspidPurchaseLineTemp removeLines = colLinestmp;
				
				OBDal.getInstance().remove(removeLines);
			}
		}

	  }*/

}
