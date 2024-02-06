package ec.com.sidesoft.localization.inventoryaccounting.businessevent;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.localization.inventoryaccounting.SliaInventoryParams;
import ec.com.sidesoft.localization.inventoryaccounting.SliaInventoryParams;

public class Slia_InventoryParamsEvent extends EntityPersistenceEventObserver {

	  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
	      SliaInventoryParams.ENTITY_NAME) };

	  @Override
	  protected Entity[] getObservedEntities() {
	    return entities;
	  }

	  public void onSave(@Observes EntityNewEvent event) {
	    if (!isValidEvent(event)) {
	      return;
	    }

	    final SliaInventoryParams invParams = (SliaInventoryParams) event.getTargetInstance();
	    
	    String strDocBaseType = invParams.getDocbasetype();
	    String strOrgID = invParams.getOrganization().getId();
	    String strInvParamsID= invParams.getId();
	    
	    if (strOrgID.equals("0")) {
	    	 	
	        	OBCriteria<SliaInventoryParams> invParamsLine = OBDal.getInstance().createCriteria(
	        	SliaInventoryParams.class);
	            invParamsLine.add(Restrictions.ne(SliaInventoryParams.PROPERTY_ID, strInvParamsID));
	            invParamsLine.add(Restrictions.eq(SliaInventoryParams.PROPERTY_DOCBASETYPE, strDocBaseType));

	            int countRegInvParmsLine = invParamsLine.count();
	            if (countRegInvParmsLine>0) {
	            	ConnectionProvider conn = new DalConnectionProvider(false);
	            	String language = OBContext.getOBContext().getLanguage().getLanguage();
	            	
	                throw new OBException(Utility.messageBD(conn, "@Slia_ErrorInvParms@", language));
	                

	            }
	           
	    }
	    
	    if (!strOrgID.equals("0")) {
    	 	
    		Organization orgObj = OBDal.getInstance().get(Organization.class, strOrgID);
    	 	
        	OBCriteria<SliaInventoryParams> invParamsLine = OBDal.getInstance().createCriteria(
        	SliaInventoryParams.class);
        	invParamsLine.add(Restrictions.ne(SliaInventoryParams.PROPERTY_ID, strInvParamsID));
            invParamsLine.add(Restrictions.eq(SliaInventoryParams.PROPERTY_DOCBASETYPE, strDocBaseType));
            invParamsLine.add(Restrictions.eq(SliaInventoryParams.PROPERTY_ORGANIZATION, orgObj));

            int countRegInvParmsLine = invParamsLine.count();
            if (countRegInvParmsLine>0) {
            	ConnectionProvider conn = new DalConnectionProvider(false);
            	String language = OBContext.getOBContext().getLanguage().getLanguage();
            	
                throw new OBException(Utility.messageBD(conn, "@Slia_ErrorInvParms@", language));
                

            }
           
    }
	    


	  }
	  public void onUpdate(@Observes EntityUpdateEvent event) {
		    if (!isValidEvent(event)) {
			      return;
			    }

			    final SliaInventoryParams invParams = (SliaInventoryParams) event.getTargetInstance();
			    
			    String strDocBaseType = invParams.getDocbasetype();
			    String strOrgID = invParams.getOrganization().getId();
			    String strInvParamsID= invParams.getId();
			    
			    if (strOrgID.equals("0")) {
			    	 	
			        	OBCriteria<SliaInventoryParams> invParamsLine = OBDal.getInstance().createCriteria(
			        	SliaInventoryParams.class);
			            invParamsLine.add(Restrictions.ne(SliaInventoryParams.PROPERTY_ID, strInvParamsID));
			            invParamsLine.add(Restrictions.eq(SliaInventoryParams.PROPERTY_DOCBASETYPE, strDocBaseType));

			            int countRegInvParmsLine = invParamsLine.count();
			            if (countRegInvParmsLine>0) {
			            	ConnectionProvider conn = new DalConnectionProvider(false);
			            	String language = OBContext.getOBContext().getLanguage().getLanguage();
			            	
			                throw new OBException(Utility.messageBD(conn, "@El tipo de base de documento seleccionado ya se encuentra creado en otra organización@", language));
			                

			            }
			           
			    }
			    
			    if (!strOrgID.equals("0")) {
		    	 	
		    		Organization orgObj = OBDal.getInstance().get(Organization.class, strOrgID);
		    	 	
		        	OBCriteria<SliaInventoryParams> invParamsLine = OBDal.getInstance().createCriteria(
		        	SliaInventoryParams.class);
		        	invParamsLine.add(Restrictions.ne(SliaInventoryParams.PROPERTY_ID, strInvParamsID));
		            invParamsLine.add(Restrictions.eq(SliaInventoryParams.PROPERTY_DOCBASETYPE, strDocBaseType));
		            invParamsLine.add(Restrictions.eq(SliaInventoryParams.PROPERTY_ORGANIZATION, orgObj));

		            int countRegInvParmsLine = invParamsLine.count();
		            if (countRegInvParmsLine>0) {
		            	ConnectionProvider conn = new DalConnectionProvider(false);
		            	String language = OBContext.getOBContext().getLanguage().getLanguage();
		            	
		                throw new OBException(Utility.messageBD(conn, "@El tipo de base de documento seleccionado ya se encuentra creado en otra organización@", language));
		                

		            }
		  }
	  }  

	}
