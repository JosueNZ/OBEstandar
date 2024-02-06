package com.sidesoft.localization.ecuador.withholdings.event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
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
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.db.DalConnectionProvider;

public class OrderEventHandle extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Order.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }
 
  
 
  public void onDelete(@Observes EntityDeleteEvent event) {
		if (!isValidEvent(event)) {
		  return;
		}
		    
		final Order order = (Order) event.getTargetInstance();
		String StrOrder_ID = order.getId().toString();
		Order OrderObj = OBDal.getInstance().get(Order.class,StrOrder_ID );
		    
	    OBCriteria<OrderLine> obcInvoiceLine = OBDal.getInstance().createCriteria(OrderLine.class);
	    obcInvoiceLine.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, OrderObj));
	    
	    String error_msg = "";
	    ConnectionProvider conn = new DalConnectionProvider(false);
	    String language = OBContext.getOBContext().getLanguage().getLanguage();
	    if (obcInvoiceLine.count()>0){
	        error_msg = Utility.messageBD(conn, "Sswh_DocumentOrderDeleteFailed", language);
	        logger.error(error_msg);
	        throw new OBException(error_msg);
	    }
	    	
 }
  
}

