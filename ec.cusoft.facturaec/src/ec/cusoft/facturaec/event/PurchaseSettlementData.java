package ec.cusoft.facturaec.event;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.model.common.invoice.Invoice;

public class PurchaseSettlementData extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    try{
    	
    	Invoice objInvoice = (Invoice) event.getTargetInstance();
    	
    	boolean blSalesTransaction = objInvoice.isSalesTransaction();
    	boolean blIsEdoc = objInvoice.getTransactionDocument().isEeiIsEdoc();
    	String stblIsEdocrDoctype = objInvoice.getTransactionDocument().getEeiEdocType();
    	stblIsEdocrDoctype = (stblIsEdocrDoctype==null?"":stblIsEdocrDoctype);
    	
    	if (!blSalesTransaction && blIsEdoc && stblIsEdocrDoctype.equals("03")){

			final Entity objInvoiceUpdate = ModelProvider.getInstance().getEntity(
					Invoice.ENTITY_NAME);
			final Property proOrderReference = objInvoiceUpdate
					.getProperty(Invoice.PROPERTY_ORDERREFERENCE);
			final Property proNoAuthorization = objInvoiceUpdate
					.getProperty(Invoice.PROPERTY_SSWHNROAUTHORIZATION);
			final Property proExpDate = objInvoiceUpdate
					.getProperty(Invoice.PROPERTY_SSWHEXPIRATIONDATE);
			String  strSubDocumentNoTotal= "";
			
			if(objInvoice.getDocumentNo().contains("<") && objInvoice.getDocumentNo().contains(">")){
				
				String strPrefijo = objInvoice.getTransactionDocument().getDocumentSequence().getPrefix();
				String strSufijo = objInvoice.getTransactionDocument().getDocumentSequence().getSuffix();
				String strSecuencia = objInvoice.getDocumentNo().replace("<", "").replace(">", "");
				
				strPrefijo = (strPrefijo==null?"":strPrefijo);
				strSufijo = (strSufijo==null?"":strSufijo);
				strSecuencia = (strSecuencia==null?"":strSecuencia);
				
				if ((strPrefijo).length() < 8) {
				  throw new OBException("Formato de número de documento inválido. (Prefijo 000-000-).");
				}
				strSubDocumentNoTotal = strPrefijo+strSecuencia+strSufijo;
				
			}else{
				
				strSubDocumentNoTotal = objInvoice.getDocumentNo();
				
			}

				String strSubDocumentNo = strSubDocumentNoTotal.substring(8);

				while (strSubDocumentNo.length() < 9) {
				  strSubDocumentNo = "0" + strSubDocumentNo;
				}
				
				String strSubDocumentNo1 = truncate(strSubDocumentNoTotal, 8);
				String documentnoX = strSubDocumentNo1 + strSubDocumentNo;
				
				if (!documentnoX.matches("^\\d{3}-\\d{3}-\\d{9}$")) {
				  throw new OBException("El formato del número de documento es incorrecto (000-000-000000000).");
				}
				
			event.setCurrentState(proOrderReference, documentnoX);
		
            event.setCurrentState(proNoAuthorization, "1111111111");
   		
    		DateFormat objFormat = new SimpleDateFormat("yyyy-MM-dd");
    		Date dtFechaActual = new Date();
    		event.setCurrentState(proExpDate,objFormat.parse(objFormat.format(dtFechaActual)));
    		
    	}
    
    }catch(Exception e){
    	throw new OBException("Error. "+e.getMessage());
    }
  }
  
  public static String truncate(String value, int length) {

    if (value == null || value.equals("")) {
      return null;
    } else {
      if (value.length() > length) {
        return value.substring(0, length);
      } else {
        return value;
      }
    }
  }
}
