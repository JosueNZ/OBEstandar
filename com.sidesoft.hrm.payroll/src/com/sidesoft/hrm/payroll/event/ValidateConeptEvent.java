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

import com.sidesoft.hrm.payroll.Concept;

public class ValidateConeptEvent extends EntityPersistenceEventObserver {

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

	    		final Concept BCONCEPT2 = (Concept) event.getTargetInstance();
	        
		        ReturnQuery(BCONCEPT2);

	  }
	  public void onUpdate(@Observes EntityUpdateEvent event) throws ServletException{
		    if (!isValidEvent(event)) {
		      return;
		    }
		    
			final Concept BConcept = (Concept) event.getTargetInstance();
	        
	        ReturnQuery(BConcept);
		  }
	  
	  private void ReturnQuery(Concept Strserach) {
		  
		    ConnectionProvider conn = new DalConnectionProvider(false);
		    String language = OBContext.getOBContext().getLanguage().getLanguage();
	        
	        boolean StrIncomeTax = Strserach.isIncomeTax().booleanValue();
 
		        OBCriteria<Concept> ConceptsCritria = OBDal.getInstance().createCriteria(
		        		Concept.class);
		        
		        ConceptsCritria.add(Restrictions.eq(Concept.PROPERTY_ISINCOMETAX, true));

		        List<Concept> certificateCriteriaList = ConceptsCritria.list();
		        
		        int tm = certificateCriteriaList.size();

			    //if (tm >0 & StrIncomeTax==true) {
			    	
			    //  throw new OBException(Utility.messageBD(conn, "ERROR=IMPUESTO A LA RENTA YA CREADO", language));
			      
			    //}
		    
		  }
	}
