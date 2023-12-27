package ec.com.sidesoft.magicstudio.statusclient.background;


import org.openbravo.scheduling.KillableProcess;
//import necesarios para las clases
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang.text.StrBuilder;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import it.openia.crm.Opcrmopportunities;
import ec.com.sidesoft.magicstudio.statusclient.SmsscStatusClient;

public class StatusClientBackground extends DalBaseProcess implements KillableProcess {
	private static final Logger log4j = Logger.getLogger(StatusClientBackground.class);
	private static ProcessLogger logger;
	 String msgTitle = "";
	 String msgMessage = "";
	 String msgType = "";
	 private boolean killProcess = false;
	 public ConfigParameters cf;
	 
	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		cf = bundle.getConfig(); // Obtener la configuración de la App OB
	    logger = bundle.getLogger();
	    OBError result = new OBError();
	    ConnectionProvider conn = new DalConnectionProvider(false);
	    String language = OBContext.getOBContext().getLanguage().getLanguage();
		try {
			OBContext.setAdminMode(false);
		      result.setType("Error");
		      result.setTitle(OBMessageUtils.messageBD("Error"));
		      String response = updateStatusClient(conn);
		      logger.logln(response);
		}catch(Exception e) {
			  result.setTitle(Utility.messageBD(conn, "Error", language));
		      result.setType("Error");
		      result.setMessage(e.getMessage());
		      log4j.error(result.getMessage(), e);
		      logger.logln(result.getMessage());
		      bundle.setResult(result);
		      return;
			
		}finally {
		      OBContext.restorePreviousMode();
		      conn.destroy();
		}
	}

	
	
	@Override
	public void kill(ProcessBundle processBundle) throws Exception {
		
		this.killProcess = true;
		
	}
	
	private String updateStatusClient(ConnectionProvider conn) throws ServletException, ParseException {
			String result = "OK";
			StatusClientBackgroundData[] contacts = StatusClientBackgroundData.selectContacts(conn);
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
			
			for(StatusClientBackgroundData contact : contacts ) {
				String clientID =  contact.userId;
				String orgId = contact.orgId;
				String adClientId = contact.clientId;
				Long quotedCustomer = Long.parseLong(contact.quoteAmount);
				//Cotizaciones por cada contacto
				StatusClientBackgroundData[] orders = StatusClientBackgroundData.select(conn, clientID);
				//Instacia del statusClient tabla
				StringBuilder whereClause = new StringBuilder();
				whereClause.append("WHERE");
				whereClause.append(" TRIM('" + clientID + "') IN (" + "ad_user_id" + ")" );
				OBQuery<SmsscStatusClient> clients =
						(OBQuery<SmsscStatusClient>)OBDal.getInstance().createQuery((Class)SmsscStatusClient.class, whereClause.toString());
				//Igualamos las variables o los actualizarce desde las cotizaciones
				String lastOrderDate = orders[0].orderDate;
				//Aumentamos el contador de las cotizaciones aprobadaspo empresa
				Long quotesApproved = this.countQuotesApprovedByCompany(orders);
				// Numero de oportnidades creadas por una empresa
				Long timesQuotedToCompany = this.countTimesQuotedToCompany(conn,clientID);
				// Contador de contizaciones no aprobadas por la empresa
				Long quotesNotApproved = this.QuotesNotApprovedToCompany(orders);
				//Validamos si existe un usuario con en la tabla de SmsscStatusClient
				//Para agreagar una nueva columna o actualizarla si existe
				
				if( clients.list() == null || clients.list().size() == 0  ) {
					continue; // si no esta presente en la tabla de status_client no hace nada
				} 
				//actualizamos los datos
				SmsscStatusClient statusClient = clients.list().get(0);
				Date date = dateFormat.parse(lastOrderDate);
				//FECHA ÚLTIMA COTIZACIÓN
				statusClient.setLastQuotDate(date);
				//NÚMERO DE VECES COTIZADAS A LA EMPRESA
				statusClient.setTimesQuotedToCompany(timesQuotedToCompany);
				//NÚMERO DE VECES COTIZADAS AL CLIENTE
				statusClient.setTimesQuotedToCustomer(quotedCustomer);
				//NÚMERO DE COTIZACIONES NO APROBADAS A LA EMPRESA
				statusClient.setQuotesNotApprovedToCompany(quotesNotApproved);
				//NÚMERO DE COTIZACIONES APROBADAS POR LA EMPRESA
				statusClient.setQuotesApprovedByCompany(quotesApproved);
				OBDal.getInstance().save((Object)statusClient);
                OBDal.getInstance().flush();
                
			}
			
			return result;
	}
	
	private Long countQuotesApprovedByCompany(StatusClientBackgroundData[] orders) {
		Integer count = 0;
		for(StatusClientBackgroundData order : orders) {
			if(order.alertStatus.contains("AP")) {
				count += 1;
			}
		}
		final Long result = count.longValue();
		
		return result;
	}
	
	private Long QuotesNotApprovedToCompany(StatusClientBackgroundData[] orders) {
		Integer count = 0;
		for(StatusClientBackgroundData order : orders) {
			if(order.alertStatus.contains("SMSSC_LO")) { //tienen el prefijo del modulo status client														
				count += 1;								 // por que se aumneto en la lista desde ese modulo
			}
		}
		final Long result = count.longValue();
		
		return result;
	}
	
	private Long countTimesQuotedToCompany(ConnectionProvider conn, String clientId) throws ServletException {
		
		String countOpportunities = StatusClientBackgroundData.countOpportunities(conn, clientId);
		
		return Long.parseLong(countOpportunities);
	}
}
