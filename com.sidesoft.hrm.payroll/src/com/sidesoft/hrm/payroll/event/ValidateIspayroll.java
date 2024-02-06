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
import org.openbravo.model.common.businesspartner.BankAccount;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.db.DalConnectionProvider;


public class ValidateIspayroll extends EntityPersistenceEventObserver {

	  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
	      BankAccount.ENTITY_NAME) };

	  @Override
	  protected Entity[] getObservedEntities() {
	    return entities;
	  }

	  public void onSave(@Observes EntityNewEvent event) {
	    if (!isValidEvent(event)) {
	      return;
	    }
	          
	    		//final Concept BCONCEPT2 = (Concept) event.getTargetInstance();
	                final BankAccount banckaccount = (BankAccount) event.getTargetInstance();
		        ReturnQuery(banckaccount);

	  }
	  public void onUpdate(@Observes EntityUpdateEvent event) throws ServletException{
		    if (!isValidEvent(event)) {
		      return;
		    }
		    
			//final Concept BConcept = (Concept) event.getTargetInstance();
			final BankAccount banckaccount1 = (BankAccount) event.getTargetInstance();
	        
	        ReturnQueryUpdate(banckaccount1);
		  }
	  
	  private void ReturnQuery(BankAccount Strserach) {
		  
		    ConnectionProvider conn = new DalConnectionProvider(false);
		    String language = OBContext.getOBContext().getLanguage().getLanguage();
	        
		    //boolean StrIncomeTax = Strserach.isIncomeTax().booleanValue();
		    boolean Strispayroll = Strserach.isSsprIspayroll().booleanValue();
		    boolean Strisemployee = Strserach.getBusinessPartner().isEmployee().booleanValue();
		    
		    BusinessPartner partner = OBDal.getInstance().get(BusinessPartner.class, Strserach.getBusinessPartner().getId().toString());
 
		        OBCriteria<BankAccount> ConceptsCritria = OBDal.getInstance().createCriteria(BankAccount.class);
		        ConceptsCritria.add(Restrictions.eq(BankAccount.PROPERTY_SSPRISPAYROLL, true));
		        ConceptsCritria.add(Restrictions.eq(BankAccount.PROPERTY_BUSINESSPARTNER, partner));
		        

		        List<BankAccount> certificateCriteriaList = ConceptsCritria.list();
		        int tm = 0;
		        tm = certificateCriteriaList.size();

			  if (tm >= 1  & Strispayroll == true & Strisemployee == true) {
			    	
     				throw new OBException(Utility.messageBD(conn, "@Aqui esta el mesaje de error insert@ " + String.valueOf(tm) ,language ));
			     // throw new OBException(Utility.messageBD(conn, "@sspr_ispayroll_unique@" ));
			//}else{

//				throw new OBException(Utility.messageBD(conn, "@Aqui esta el mesaje de error insert@ " + String.valueOf(tm),language ));
				}
			   
		    
		  }

	private void ReturnQueryUpdate(BankAccount Strserach) {
		  
		    ConnectionProvider conn = new DalConnectionProvider(false);
		    String language = OBContext.getOBContext().getLanguage().getLanguage();
	        
		    //boolean StrIncomeTax = Strserach.isIncomeTax().booleanValue();
		    boolean Strispayroll = Strserach.isSsprIspayroll().booleanValue();
		    boolean Strisemployee = Strserach.getBusinessPartner().isEmployee().booleanValue();
		    
		    BusinessPartner partner = OBDal.getInstance().get(BusinessPartner.class, Strserach.getBusinessPartner().getId().toString());
 
		        OBCriteria<BankAccount> ConceptsCritria = OBDal.getInstance().createCriteria(BankAccount.class);
		        ConceptsCritria.add(Restrictions.eq(BankAccount.PROPERTY_SSPRISPAYROLL, true));
		        ConceptsCritria.add(Restrictions.eq(BankAccount.PROPERTY_BUSINESSPARTNER, partner));
		        

		        List<BankAccount> certificateCriteriaList = ConceptsCritria.list();
		        int tm = 0;
		        tm = certificateCriteriaList.size();

			  if (tm > 1  & Strispayroll == true & Strisemployee == true) {
			    	
     				throw new OBException(Utility.messageBD(conn, "@Aqui esta el mesaje de error update@ " + String.valueOf(tm),language ));
			     // throw new OBException(Utility.messageBD(conn, "@sspr_ispayroll_unique@" ));
				//}else{
				//throw new OBException(Utility.messageBD(conn, "@Aqui esta el mesaje de error update@ " +  String.valueOf(tm),language ));				

				}

			  // }
		    
		  }
	}
