package com.sidesoft.hrm.payroll.advanced.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;

import com.sidesoft.hrm.payroll.Concept;


public class UpdateConceptLineEvent extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
		  Concept.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

	        final Entity BConceptEntity = ModelProvider.getInstance().getEntity(
	        		Concept.ENTITY_NAME);
	        final Property valueProperty = BConceptEntity
	            .getProperty(Concept.PROPERTY_SSPRCONCEPTFORMULA);
	        
	        event.setCurrentState(valueProperty, null );
	        
	        final Property valueProperty2 = BConceptEntity
		            .getProperty(Concept.PROPERTY_OPERATION);
		        
	        event.setCurrentState(valueProperty2, null );
	        
	        final Property valueProperty3 = BConceptEntity
		            .getProperty(Concept.PROPERTY_AMOUNT);
		        
	        event.setCurrentState(valueProperty3, null );

  }
  public void onUpdate(@Observes EntityUpdateEvent event) {
	    if (!isValidEvent(event)) {
	      return;
	    }

        final Entity BConceptEntity = ModelProvider.getInstance().getEntity(
        		Concept.ENTITY_NAME);
        final Property valueProperty = BConceptEntity
            .getProperty(Concept.PROPERTY_SSPRCONCEPTFORMULA);
        
        event.setCurrentState(valueProperty, null );
        
        final Property valueProperty2 = BConceptEntity
	            .getProperty(Concept.PROPERTY_OPERATION);
	        
        event.setCurrentState(valueProperty2, null );
        
        final Property valueProperty3 = BConceptEntity
	            .getProperty(Concept.PROPERTY_AMOUNT);
	        
        event.setCurrentState(valueProperty3, null );
	  }
  
  public void onDelete(@Observes EntityDeleteEvent event) {
	    if (!isValidEvent(event)) {
	      return;
	    }
	    
	    final Concept BConcept = (Concept) event.getTargetInstance();
   	    
	    String ConceptID = BConcept.getId().toString();

	    Concept ConceptObj = OBDal.getInstance().get(Concept.class, ConceptID);
	    
	    String PreviewValue = ConceptObj.getValue().toString();
	    
	    String PreviewConceptType = ConceptObj.getConcepttype().toString();
	    
	    if (!PreviewConceptType.equals('F')){
	    	
	        String SSql = "concepttype = 'F' AND formula like '%" + PreviewValue + "%'";
		    
	        OBQuery<Concept> ConceptObj2 = OBDal.getInstance().createQuery(Concept.class, SSql);
	    	
	    	if (ConceptObj2.list().size() > 0){
	    		OBError myMessage = new OBError();
	            myMessage.setTitle("");
	            myMessage.setType("Error");
	            myMessage.setMessage(String.format(
	                OBMessageUtils.messageBD("SSPR_ConceptInUse"), ""));
	            System.err.println(myMessage.getMessage().toString());
            
	    	}
    	
	    }
    	
      /*  final Entity BConceptEntity = ModelProvider.getInstance().getEntity(
        		Concept.ENTITY_NAME);
        final Property valueProperty = BConceptEntity
            .getProperty(Concept.PROPERTY_SSPRCONCEPTFORMULA);
        
        event.setCurrentState(valueProperty, null );
        
        final Property valueProperty2 = BConceptEntity
	            .getProperty(Concept.PROPERTY_OPERATION);
	        
        event.setCurrentState(valueProperty2, null );
        
        final Property valueProperty3 = BConceptEntity
	            .getProperty(Concept.PROPERTY_AMOUNT);
	        
        event.setCurrentState(valueProperty3, null );*/	    
	  }  

}

