package ec.cusoft.facturaec.ad_process.utils;

import java.util.List;

import org.hibernate.criterion.Expression;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;

public class ProcessUtils {
	//funcion para obtener las variables globales
		static public VariablesSecureApp getVars(){

			String clientID =  OBContext.getOBContext().getCurrentClient().getId();
			String userID = OBContext.getOBContext().getUser().getId();
			String lenguageID = OBContext.getOBContext().getLanguage().getId();
			String orgID = OBContext.getOBContext().getCurrentOrganization().getId();
			String roleID = OBContext.getOBContext().getRole().getId();
			VariablesSecureApp vars = new VariablesSecureApp(userID, clientID,orgID,roleID, lenguageID );

			return vars;
		}


		//funci'on para buscar la factura dado su ID
		static public Invoice getInvoice(String invoiceID){
			Invoice invoice = null;

			OBContext.setAdminMode(true);

			final OBCriteria<Invoice> inv = OBDal.getInstance().createCriteria(Invoice.class);
			inv.add(Expression.eq(Invoice.PROPERTY_ID, invoiceID));
			List<Invoice>   li = inv.list();

			OBContext.restorePreviousMode();

			if (li != null && !li.isEmpty()) {
				invoice = li.get(0);
			}		

			return invoice;
		}
}
