package ec.cusoft.facturaec.services;


import java.util.ArrayList;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;

import ec.cusoft.facturaec.Contingency;
import ec.cusoft.facturaec.services.utils.ContingencyStatusEnum;

/**
 * Clase contenedora de los métodos necesarios para el módulo
 * de factura. Contiene los servicios de búsquedas y actualización de
 * certificados de contigencia en la BD.
 * @author Positive Sl.
 *
 */
public class ContigencyService {
	
	/**
	 * Método para buscar certificados, dado el Id que lo identifica en la tabla
	 * EEI_Contingency.
	 * @param ID
	 * @return Contingency
	 */
	static public Contingency findContingencyCertificate(String ID) {
		Contingency cert = null;
		if (ID != null && !ID.equals("")) 
			cert = OBDal.getInstance().get(Contingency.class, ID);
		return cert;
	}
	
	/**
	 * Método para buscar una factura, dado el Id que la identifica en la tabla
	 * C_Invoice.
	 * @param ID
	 * @return Invoice
	 */
	static public Invoice findInvoice(String ID) {
		Invoice inv = null;
		if (ID != null && !ID.equals("")) 
			inv = OBDal.getInstance().get(Invoice.class, ID);
		return inv;
	}
	
	
	/**
	 * Método para buscar contingencias, pasando por parámetro el enumerado que representa
	 * los estados. Ej: ContingencyStatusEnum.LI -> LIBRE, ContingencyStatusEnum.PR -> PROCESANDO, ContingencyStatusEnum.CO -> CONSUMIDO.
	 * @param status: de tipo ContingencyStatusEnum
	 * @return List<Contingency>
	 */
	static public List<Contingency> findContingencyByStatus(ContingencyStatusEnum status) {
		List<Contingency> Contingencys = new ArrayList<Contingency>();
		/*Se invoca un criterio de hibernate para realizar el filtrado*/
		OBCriteria<Contingency> obCriteria = OBDal.getInstance().createCriteria(Contingency.class);
		/*Se realiza una restricción para quedarnos solamente con el estado solicitado*/
		obCriteria.add(Restrictions.eq("status", status.name()));
		Contingencys = obCriteria.list();
		return Contingencys;
	}
	
	/**
	 * Método para buscar alguna certificado para la contingecia disponible.
	 * Si existe alguno se devuelve un objeto  de tipo Contingency, de lo contrario se retorna NULL incando que
	 * no existe ningún certificado libre o disponible para la contigencia.
	 * @return Contingency
	 */
	static public Contingency findAvailableContingency() {
		List<Contingency> Contingencys = new ArrayList<Contingency>();
		/*Se invoca un criterio de hibernate para realizar el filtrado*/
		OBCriteria<Contingency> obCriteria = OBDal.getInstance().createCriteria(Contingency.class);
		/*Se realiza una restricción para quedarnos solamente con las que son libres*/
		obCriteria.add(Restrictions.eq("status", ContingencyStatusEnum.LI.name()));
		Contingencys = obCriteria.list();
		/* Si la lista retornada es diferente de cero, se devuelve el primer elemento, de lo
		 * contrario null.
		 */
		return Contingencys.size() != 0 ? (Contingency)Contingencys.get(0) : null;
	}
	
	/**
	 * Este método permite marcar un certificado de contingencia Libre o Procesando como Consumido, de esta
	 * manera no podrá utilizarce nuevamente. El objeto Contingency lleva la factura (Invoice).
	 * @param Contingency: Objeto de tipo Contingency
	 * @return Boolean
	 */
	static public boolean markContingencyAsConsumed(Contingency Contingency){
		boolean marked = false;
		if(Contingency.getStatus() != ContingencyStatusEnum.CO.name()){
			/*Se marca el certificado de contingencia como consumido*/
			Contingency.setStatus(ContingencyStatusEnum.CO.name());
			/*Se modifica la BD*/
			OBDal.getInstance().save(Contingency);
			OBDal.getInstance().flush();
			marked = true;
		}
		return marked;
	}
	
	/**
	 * Este método permite marcar un certificado de contingencia Libre o Procesando como Consumido, de esta
	 * manera no podrá utilizarce nuevamente.
	 * @param ID: Identificador de la contingencia en la BD.
	 * @param invoiceID: Identificador de la factura en la BD.
	 * @return Boolean
	 */
	static public boolean markContingencyAsConsumed(String ID, String invoiceID){
		boolean marked = false;
		/*Se carga el certificado de contingencia de la BD*/
		Contingency contingency = findContingencyCertificate(ID);
		if(contingency.getStatus() != ContingencyStatusEnum.CO.name()){
			/*Se marca el certificado de contingencia como consumido*/
			contingency.setStatus(ContingencyStatusEnum.CO.name());
			/*Si se pasa alguna factura por parámetro se carga la misma y se le pone a la contingecia*/
			if(invoiceID != null && invoiceID != ""){
				Invoice invoice = findInvoice(invoiceID);
				contingency.setInvoice(invoice);
			}
			
			/*Se modifica la BD*/
			OBDal.getInstance().save(contingency);
			OBDal.getInstance().flush();
			marked = true;
		}
		return marked;
	}
	
	/**
	 * Este método permite marcar un certificado de contingencia Libre como Procesando
	 * @param ID
	 * @param invoiceID
	 * @return Boolean
	 */
	static public boolean markContingencyAsProcessing(String ID, String invoiceID){
		boolean marked = false;
		/*Se carga el certificado de contingencia de la BD*/
		Contingency contingency = findContingencyCertificate(ID);
		if(contingency.getStatus() != ContingencyStatusEnum.CO.name()){
			/*Se marca el certificado de contingencia como consumido*/
			contingency.setStatus(ContingencyStatusEnum.PR.name());
			/*Si se pasa alguna factura por parámetro se carga la misma y se le pone a la contingecia*/
			if(invoiceID != null && invoiceID != ""){
				Invoice invoice = findInvoice(invoiceID);
				contingency.setInvoice(invoice);
			}
			/*Se modifica la BD*/
			OBDal.getInstance().save(contingency);
			OBDal.getInstance().flush();
			marked = true;
		}
		return marked;
	}
	
	/**
	 * Este método permite marcar un certificado de contingencia Libre como Procesando
	 * @param Contingency: Objeto de tipo Contingency
	 * @return Boolean
	 */
	static public boolean markContingencyAsProcessing(Contingency contingency){
		boolean marked = false;
		if(contingency.getStatus() != ContingencyStatusEnum.CO.name()){
			/*Se marca el certificado de contingencia como consumido*/
			contingency.setStatus(ContingencyStatusEnum.PR.name());
			/*Se modifica la BD*/
			OBDal.getInstance().save(contingency);
			OBDal.getInstance().flush();
			marked = true;
		}
		return marked;
	}
	
	/**
	 * Este método permite agregar la factura a la contingencia, modificando solamente el campo
	 * Invoice_Id.
	 * @param ID: Identificador de la contingencia en la BD.
	 * @param invoiceID: Identificador de la factura en la BD.
	 * 
	 */
	static public void setInvoiceInContingency(String ID, String invoiceID){
		/*Se carga el certificado de contingencia de la BD*/
		Contingency contingency = findContingencyCertificate(ID);
		if(invoiceID != null && invoiceID != ""){
			Invoice invoice = findInvoice(invoiceID);
			contingency.setInvoice(invoice);
		}
		
		/*Se modifica la BD*/
		OBDal.getInstance().save(contingency);
		OBDal.getInstance().flush();
		
	}
}
