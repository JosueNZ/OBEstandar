package ec.cusoft.facturaec.background;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.cusoft.facturaec.EEIInvoiceLog;
import ec.cusoft.facturaec.EEIParamFacturae;
import ec.cusoft.facturaec.ad_process.GenerateFE;
import ec.cusoft.facturaec.ad_process.webservices.WebServiceEstados.WSEstados_PortType;
import ec.cusoft.facturaec.ad_process.webservices.WebServiceEstados.WSEstados_ServiceLocator;

public class EEIStatusValidate extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(EEIOfflineBatchBackground.class);
  private ProcessLogger logger;
  public ConfigParameters cf;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    
    cf = bundle.getConfig();
    logger = bundle.getLogger();
    OBError result = new OBError();
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    String strSessionUserId = OBContext.getOBContext().getUser().getId();
    String strMessage ="";

    try{
    	OBContext.setAdminMode();
    	// LISTAR DOCUMENTOS GENERADOS O RECIBIDOS
        OBCriteria<Invoice> objInvoice = OBDal.getInstance().createCriteria(Invoice.class);
        objInvoice.add(Restrictions.disjunction()
            .add(Restrictions.eq(Invoice.PROPERTY_EEISTATUS, "GE"))
            .add(Restrictions.eq(Invoice.PROPERTY_EEISTATUS, "RE"))
            .add(Restrictions.eq(Invoice.PROPERTY_EEISTATUS2, "GE"))
            .add(Restrictions.eq(Invoice.PROPERTY_EEISTATUS2, "RE"))
            );
 
        if( objInvoice.list().size()!=0) {
	        // OBTENER ORGANIZACIÓN CONTABLE
	        OBCriteria<Organization> objOrganization = OBDal.getInstance().createCriteria(Organization.class,"org");
	        objOrganization.createAlias(Organization.PROPERTY_ORGANIZATIONTYPE, "orgt");
	        objOrganization.add(Restrictions.eq("orgt."+OrganizationType.PROPERTY_LEGALENTITYWITHACCOUNTING, true));
	        // OBTENER PARÁMETRO URL WS
	        OBCriteria<EEIParamFacturae> objParams = OBDal.getInstance().createCriteria(EEIParamFacturae.class);
	        objParams.add(Restrictions.eq(EEIParamFacturae.PROPERTY_ACTIVE, true));
	        WSEstados_ServiceLocator wsLocator = new WSEstados_ServiceLocator();
	        wsLocator.setUrl(objParams.list().get(0).getURLWsValidacion().replace("WSRecepcion","WSEstados"));
	        WSEstados_PortType wsEstadosPort = wsLocator.getWSEstadosPort();        

	        for (Invoice obInvoice : objInvoice.list()) {

	        	String strDType="";
	        	if(!obInvoice.isSalesTransaction() && (obInvoice.getEeiStatus().equals("RE") || obInvoice.getEeiStatus().equals("GE"))) {
	        		strDType = obInvoice.getSswhCDoctype().getEeiEdocType();
	        	}else {
	        		strDType = obInvoice.getDocumentType().getEeiEdocType();
	        	}
	        	
	        	String strResult =wsEstadosPort.getLastStatus(objOrganization.list().get(0).getOrganizationInformationList().get(0).getTaxID()
	        			, obInvoice.getId(), strDType);

	        	if (strResult.contains("OK;;")) {
		        	String[] lstResult = strResult.split(";;");
		        	SimpleDateFormat formatter=new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
                    if (lstResult[1].equals("GE") || lstResult[1].equals("RE") || lstResult[1].equals("NR") 
                        || lstResult[1].equals("AU") || lstResult[1].equals("NA")|| lstResult[1].equals("PP")){	                    
                    	
                    	if (obInvoice.getEeiStatus()!=null && (strDType.equals("01") || strDType.equals("04") || strDType.equals("05") || strDType.equals("07"))) {
	                        if(obInvoice.getEeiStatus().equals("RE") || obInvoice.getEeiStatus().equals("GE")) {
	                                strMessage = strMessage+"\n"+strDType+" - "+(!obInvoice.isSalesTransaction()? obInvoice.getSswhWithholdingref() :obInvoice.getDocumentNo())
	                                        +" - ANTERIOR: "+obInvoice.getEeiStatus()+" - ACTUAL: "+lstResult[1];
	
	                                obInvoice.setEeiStatus(lstResult[1]);
	                                if(lstResult[1].equals("AU")){
	                                    if(!lstResult[5].equals("")){
	                                        obInvoice.setEeiFechaauto(formatter.parse(lstResult[5]));
	                                    }
	                                    obInvoice.setEeiNumauto(lstResult[4]);
	                                    obInvoice.setEeiFechaautotext(lstResult[5]);
	                                }                                
	                                obInvoice.setEeiUrlxml(lstResult[8]);
	                                obInvoice.setEeiUrlride(lstResult[9]);
	                                OBDal.getInstance().save(obInvoice);                            
	                        }
                    	}
                        if (obInvoice.getEeiStatus2()!=null && strDType.equals("03")) {
	                        if(obInvoice.getEeiStatus2().equals("RE") || obInvoice.getEeiStatus2().equals("GE")) {
	                            strMessage = strMessage+"\n"+strDType+" - "+obInvoice.getDocumentNo()
	                                    +" - ANTERIOR: "+obInvoice.getEeiStatus2()+" - ACTUAL: "+lstResult[1];
	
	                            if(lstResult[1].equals("AU")){
	                                obInvoice.setEeiStatus2(lstResult[1]);
	                                obInvoice.setEeiNumauto2(lstResult[4]);
	                                obInvoice.setEeiFechaautotext2(lstResult[5]);
	                            }
	                            obInvoice.setEeiUrlxml2(lstResult[8]);
	                            obInvoice.setEeiUrlride2(lstResult[9]);
	                            OBDal.getInstance().save(obInvoice);
	                        }
                        }
                        String strTypeResult="E";
                        if(lstResult[1].equals("AU") || lstResult[1].equals("RE")){
                            strTypeResult = "S";
                        }
                        
		                GenerateFE.InsertLogs(conn, obInvoice, lstResult[7], strTypeResult,strSessionUserId,lstResult[3].trim());                        

                    }else{
                        strMessage = strMessage+"\n"+obInvoice.getDocumentNo()+" - ESTADO NO VÁLIDO - "+lstResult[1];
                    }
	        	}else{
                    strMessage = strMessage+"\n"+obInvoice.getDocumentNo()+" - ERROR AL BUSCAR - "+strResult;
                }


	        }

        }else {
        	strMessage = "Ningún registro";
        }
        result.setType("Success");
        result.setTitle(Utility.messageBD(conn, "Success", language));
        result.setMessage(strMessage);
        bundle.setResult(result);
        
    } catch (Exception e) {
      result.setTitle(Utility.messageBD(conn, "Error", language));
      result.setType("Error");
      result.setMessage(e.getMessage());

      log4j.error(result.getMessage(), e);
      logger.logln(result.getMessage());
      bundle.setResult(result);
      return;
    } finally {
        logger.log(strMessage);
        OBContext.restorePreviousMode();
        conn.destroy();
    }
  }      

}