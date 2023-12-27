package ec.com.sidesoft.hrm.payroll.payment.rol.fortnight.background;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.FormatUtilities;

import ec.com.sidesoft.hrm.payroll.payment.rol.fortnight.ad_process.SSPRPRF_PrintReportPaymentRoFortnight;
import ec.com.sidesoft.hrm.payroll.payment.rol.SSPRPRConfig;

public class SendingPaymentRolesFortnightBackground extends DalBaseProcess implements
KillableProcess {
  
  private static final Logger log4j = Logger.getLogger(SendingPaymentRolesFortnightBackground.class);
  private static ProcessLogger logger;
  private VariablesSecureApp vars;
  String msgTitle = "";
  String msgMessage = "";
  String msgType = ""; // success, warning or error
  public ConfigParameters cf;
  private static String strAttachment;
  private static String strFTP;
  private static Connection connectionDB = null; 
  private String configBodyFortnight = "";
  private String configFooterFortnight = "";
  private String configSubjectFortnight = "";
  private String reportFormat = "";
  private String routeReport = "";
  private boolean killProcess = false;
  private String nombreArchivo = "";
  private String cedulaEmpleado = "";
  private String nombreEmpleado = "";
  private String fechanomina = "";
  private String IDEmpleado = "";
  private String documentno = "";
  private String strADUSerID;
  
  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    cf = bundle.getConfig(); // Obtener la configuración de la App OB
    logger = bundle.getLogger();
    OBError result = new OBError();
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false); 
    try {
      
      OBContext.setAdminMode(false);
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));

      JSONArray fortnightPayrollList = null;
      
      try {
        connectionDB = conn.getTransactionConnection();
      } catch (Exception e) {}

      // *********************************************************************
      // INICIO VERIFICO QUE EXISTA CONFIGURACION ENVIO AUTOMATICO ROLES
      // *********************************************************************
      OBCriteria<SSPRPRConfig> config = OBDal.getInstance().createCriteria(SSPRPRConfig.class);
      config.add(Restrictions.eq(SSPRPRConfig.PROPERTY_ACTIVE, true));
      config.addOrderBy(SSPRPRConfig.PROPERTY_CREATIONDATE, false);
      config.setMaxResults(1);

      if (config.list() != null && config.list().size() > 0) { 

        vars = bundle.getContext().toVars();
        
        strAttachment = cf.getBaseDesignPath() + "/design/";
        strFTP = cf.strFTPDirectory;        
        
        configBodyFortnight = config.list().get(0).getSsprprfBody().trim();
        configFooterFortnight = config.list().get(0).getSsprprfFooter();
        configSubjectFortnight = config.list().get(0).getSsprprfSubject().trim();
        reportFormat = config.list().get(0).getReportformat().trim();
        Boolean isFailedProcess = false;        
        // *********************************************************************        
        // OBTENGO LAS NOMINAS MENSUALES QUE SE TIENEN QUE ENVIAR
        // *********************************************************************        
        fortnightPayrollList = getFortnightPayroll(conn);
        if (fortnightPayrollList != null && fortnightPayrollList.length() != 0) {

          try{

            ArrayList<String> arrayUpdateNomina = new ArrayList<String>();
            int countPayrollFortnigh = 0;
            
            // SI EXISTEN NOMINAS QUINCENALES PARA ENVIAR
            for (int i = 0; i < fortnightPayrollList.length(); i++) {
              
              if (killProcess) {
                throw new OBException("Process killed");
              }

              JSONObject dataFortnightPayroll =  (JSONObject) fortnightPayrollList.get(i);
              
              arrayUpdateNomina.add(dataFortnightPayroll.getString("nominaid"));
              isFailedProcess = generatePDFAndSendMail(dataFortnightPayroll); 
              countPayrollFortnigh = countPayrollFortnigh + 1;   
               
              /*if(isFailedProcess) { 
                 break; 
              } */

            }
            
            // SE HACE EL UPDATE DE LAS NOMINAS ENVIADAS
            if(arrayUpdateNomina.size() > 0 && !isFailedProcess) {

              ArrayList<String> newList = removeDuplicates(arrayUpdateNomina);
              String strNominas = StringUtils.join(newList, "' , '");
              strNominas = "'" + strNominas + "'";
              updateFieldSendMaildPayrollFortnigh(strNominas,conn);
              
              logger.logln("Proceso ejecutado exitosamente. Correos enviados: " + countPayrollFortnigh);              

            }

          } catch (Exception e) { 
            logger.logln("ERROR " + e.getMessage()); 
          }              
          
                   
        } else {
          logger.logln("No existen nominas quincenales para enviar.");
        }
        // *********************************************************************        
        // *********************************************************************         
        
      } else {
        logger.logln("No existe Configuracion para el proceso de Envío de roles de pago.");
      }
      // *********************************************************************
      // FIN VERIFICO QUE EXISTA CONFIGURACION ENVIO AUTOMATICO ROLES
      // *********************************************************************
      
      connectionDB.close();
      OBDal.getInstance().commitAndClose();
      
    } catch (Exception e) {
      result.setTitle(Utility.messageBD(conn, "Error", language));
      result.setType("Error");
      result.setMessage(e.getMessage());
      log4j.error(result.getMessage(), e);
      logger.logln(result.getMessage());
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }  
    
  }
  
  private Boolean generatePDFAndSendMail(JSONObject data) throws JSONException {
    
    cedulaEmpleado = data.getString("empleado");
    IDEmpleado = data.getString("empleadoid");
    nombreEmpleado = data.getString("empleadonombre");
    nombreArchivo = data.getString("nombrearchivo");
    documentno = data.getString("documentno");
    fechanomina = data.getString("fechanomina");
    routeReport = printReport(); 
    
    if(!routeReport.equals("")) {
      
      // Variables de Envio mensaje
      String host = null;
      boolean auth = true;
      String username = null;
      String password = null;
      String connSecurity = null;
      int port = 25;
      String senderAddress = "";
      String contentType = "";
      String recipientTO = "";
      String emailSubject = null, emailBody = null;
      List<File> attachments = new ArrayList<File>();      
      Boolean isFailed = true;

      // CORREO DEL EMPLEADO
      recipientTO = data.getString("correo");      
      
      // RUTA DEL REPORTE GENERADO EN LOS ATTACHMENTS
      File file = new File(routeReport);
      attachments.add(file);
      
      // OBTENER EL MES EN LETRAS DE LA FECHA
      String[] mesAnioFechaNomina = fechanomina.split("/");
      String mesLetrasNomina = getMonthInLetters(mesAnioFechaNomina[0]);
      String formatoFechaNomina = mesLetrasNomina + " " + mesAnioFechaNomina[1];      
      
      // SE GENERA EL DETALLE DEL CORREO    
      emailSubject = configSubjectFortnight;
      emailBody = "<html> \n"
          + "<head> \n"
          + "<meta charset=\"UTF-8\">  \n"
          + "</head> \n"
          + "<body> \n"
          + configBodyFortnight + " " + " ( <strong> " + formatoFechaNomina + " </strong> para <strong> " + nombreEmpleado + " </strong>) </p>"
          + configFooterFortnight
          + "<br><br> \n"
          + "</body> \n"
          + "</html> \n";
      contentType = "text/html; charset=utf-8";      
      
      /* ********************************************************** */
      /* INICIO CONFIGURACION DEL CORREO
      /* ********************************************************** */
      try {
        OBCriteria<EmailServerConfiguration> EmailServerConfiguration2 = OBDal.getInstance()
            .createCriteria(EmailServerConfiguration.class);

        EmailServerConfiguration2
            .add(Restrictions.eq(EmailServerConfiguration.PROPERTY_ACTIVE, true));

        if (EmailServerConfiguration2.list().size() == 0) {
          logger.logln("Error Configuración correo electrónico - No esta configurado el correo electrónico para la entidad. ");
          return isFailed;
        } else {

          List<EmailServerConfiguration> ContractCriteriaList = EmailServerConfiguration2.list();

          for (EmailServerConfiguration mailConfig : ContractCriteriaList) {
            host = mailConfig.getSmtpServer();
            if (!mailConfig.isSMTPAuthentification()) {
              auth = false;
            }

            try {
                username = mailConfig.getSmtpServerAccount();
                }catch(Exception e) {
                	
                }
                
                try {
                password = FormatUtilities.encryptDecrypt(mailConfig.getSmtpServerPassword(), false);
              	}catch(Exception e) {}
            
            connSecurity = mailConfig.getSmtpConnectionSecurity();
            port = mailConfig.getSmtpPort().intValue();
            senderAddress = mailConfig.getSmtpServerSenderAddress();
          }
        }

      } catch (Exception exception) {
        logger.logln("Error Recuperando datos para correo de la entidad "+ exception.getMessage()); 
        return isFailed; 
      } finally {
        OBContext.restorePreviousMode();
      } 
      /* ********************************************************** */
      /* FIN CONFIGURACION DEL CORREO
      /* ********************************************************** */
        
      /* ********************************************************** */
      /* INICIO ENVIO DEL CORREO
      /* ********************************************************** */
      try {
        
            EmailManager.sendEmail(host, auth, username, password, connSecurity, port, senderAddress,
                recipientTO, recipientTO, recipientTO, recipientTO, emailSubject, emailBody, contentType,
                attachments, new Date(), null);
        
      } catch (Exception exception) {
        msgMessage = exception.toString();
        logger.logln("Error " + msgMessage);
        return isFailed;
      } finally {
        // Delete the temporary files generated for the email attachments
        for (File attachment : attachments) {
          if (attachment.exists() && !attachment.isDirectory()) {
            attachment.delete();
          }
        }
      }      
      /* ********************************************************** */
      /* FIN ENVIO DEL CORREO
      /* ********************************************************** */  

    }else { 
      // no se genero el reporte
      logger.logln("No se genero el reporte para adjuntarlo en el correo.");
    }

    return false;
    
  }
  
  private static void updateFieldSendMaildPayrollFortnigh(String arrayIds, ConnectionProvider conn) {
    
    String strSql = "UPDATE spep_advance_payment SET em_spepm_sentmail = 'Y' WHERE spep_advance_payment_id IN("+arrayIds+")";    

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = conn.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();
    } catch (Exception e) {
      logger.logln("Error al acualizar el campo email enviado - Nominas Mensuales - " + e.getMessage());
      throw new OBException("Error al acualizar el campo email enviado - Nominas Mensuales - " + e.getMessage());
    }
  }

  private static JSONArray getFortnightPayroll(ConnectionProvider conn) {
    
    try {
      
      String strSql = null;      
      strSql = "select cbp.value as empleado, \n"
          + " cbp.c_bpartner_id as empleadoid, \n"
          + " cbp.name as empleadonombre,  \n"
          + " trim(cbp.em_sspr_email) as correo, \n"
          + " ap.documentno, \n"
          + " TO_CHAR(cp.startdate, 'MM/YYYY') as fechanomina,  \n"
          + " to_char(cp.startdate,'MMYYYY')|| '_' || cbp.value  || '.pdf' as nombrearchivo, \n"
          + " ap.spep_advance_payment_id as nominaid \n"
          + " from SPEP_Advance_Payment ap \n"
          + " left join SPEP_Advance_PaymentLine apl on apl.SPEP_Advance_Payment_id=ap.SPEP_Advance_Payment_id \n"
          + " left join c_bpartner cbp on apl.c_bpartner_id=cbp.c_bpartner_id \n"
          + " left join c_period cp on ap.c_period_id=cp.c_period_id \n"
          + " where ap.em_spepm_sendmail='Y' \n"
          + " and em_spepm_sentmail='N'  \n"
          + " and cbp.em_sspr_email is not null \n";
      
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      ArrayList<String> strResult = new ArrayList<String>();
      strResult.clear();
      JSONArray array= new JSONArray();
      
      while (rsConsulta.next()) {

        array.put(new JSONObject()
          .put("empleado", rsConsulta.getString("empleado"))
          .put("empleadoid", rsConsulta.getString("empleadoid"))
          .put("empleadonombre", rsConsulta.getString("empleadonombre"))
          .put("correo", rsConsulta.getString("correo"))
          .put("documentno", rsConsulta.getString("documentno"))
          .put("nombrearchivo", rsConsulta.getString("nombrearchivo"))
          .put("nominaid", rsConsulta.getString("nominaid"))
          .put("fechanomina", rsConsulta.getString("fechanomina"))
        );       

      }      
      return array;

    } catch (Exception e) {
      throw new OBException("Error al buscar nominas quincenales - " + e.getMessage());
    }    
    
  }

  public String printReport() {

    final HttpServletRequest request = RequestContext.get().getRequest();
    final HttpServletResponse response = RequestContext.get().getResponse();

    String strReport = "";
    strADUSerID = vars.getUser().toString();

    SSPRPRF_PrintReportPaymentRoFortnight printReport = new SSPRPRF_PrintReportPaymentRoFortnight();
    
    try {
      strReport = printReport.doPost(request, response, strAttachment, strFTP, connectionDB, reportFormat, nombreArchivo, cedulaEmpleado, IDEmpleado, documentno, strADUSerID);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ServletException e) {
      e.printStackTrace();
    }
    
    return strReport;

  }
  
  public static <T> ArrayList<T> removeDuplicates(ArrayList<T> list){ 

      ArrayList<T> newList = new ArrayList<T>(); 
      for (T element : list) { 
          if (!newList.contains(element)) { 
              newList.add(element); 
          } 
      } 

      return newList; 
  }   
  
  private String getMonthInLetters(String numeroMes) {

    String mesLetras = "";
    int num = Integer.parseInt(numeroMes);
    String[] mes = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio", "Agosto", "Setiembre", "Octubre", "Noviembre", "Diciembre"};
    mesLetras = mes[num -1];
    
    return mesLetras;
    
  }  

  @Override
  public void kill(ProcessBundle processBundle) throws Exception {
    OBDal.getInstance().flush();
    this.killProcess = true;
  }

}
