/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.List;
import org.openbravo.model.ad.domain.ListTrl;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.utils.CryptoUtility;

/**
 *
 * @author nicholas
 */
public class CaseUpdateSender extends DalBaseProcess{
    
    @Override
    public void doExecute(ProcessBundle bundle) throws Exception {
        
        final OBError msg = new OBError();
        final ProcessLogger logger = bundle.getLogger();
                
        Organization currentOrg = null;
        int result=-1;
        boolean sendingIssues = false;
        
        currentOrg = OBContext.getOBContext().getCurrentOrganization();
        
        final OBCriteria<Opcrmcase> caseRecords = OBDal.getInstance().createCriteria(Opcrmcase.class);
        caseRecords.add(Restrictions.eq(Opcrmcase.PROPERTY_SENDUPDATE, true));
        //caseRecords.add(Restrictions.eq(Opcrmcase.PROPERTY_ORGANIZATION,currentOrg));
        
        for (Opcrmcase c : caseRecords.list()){
            result = sendCase(c);
            
            if(result>0){
                c.setSendupdate(Boolean.FALSE);
            }
            else{
                sendingIssues=true;
            }
            /*
            Se l'invio e' andato a buon fine
                OBDal.getInstance().save(c);
                OBDal.getInstance().flush();
        
            */
        }
        
        if(sendingIssues)
            logger.logln(MailUtils.GetMessage("OPCRM_CASES_SENDING_ISSUES"));
        else
            logger.logln(MailUtils.GetMessage("OPCRM_CASES_SENT_OK"));
            
    }
    
    private int sendCase(Opcrmcase c) {
        File calendarCaseFile = null;
        String contextPath="";
        User destUser;
        String assignedUserId="";
        
        boolean sentToAssignedUser = false;
        if (c.getAssignedTo()!=null)
            assignedUserId = c.getAssignedTo().getId();
        
        final OBCriteria<opcrmCasesAccess> casesAccessRecord = OBDal.getInstance().createCriteria(opcrmCasesAccess.class);
        casesAccessRecord.add(Restrictions.eq(opcrmCasesAccess.PROPERTY_OPCRMCASES, c));
        
        final OBCriteria<EmailServerConfiguration> emailServerConfigurationRecord = OBDal.getInstance().createCriteria(EmailServerConfiguration.class);
        emailServerConfigurationRecord.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_CLIENT, c.getClient()));
        EmailServerConfiguration emailConfig = emailServerConfigurationRecord.list().get(0);
        
        String host = emailConfig.getSmtpServer();
        String user = emailConfig.getSmtpServerAccount() == null ? "" : emailConfig.getSmtpServerAccount();
        String password = emailConfig.getSmtpServerPassword() == null ? "" : emailConfig.getSmtpServerPassword();
        boolean authentication = emailConfig.isSMTPAuthentification() != null ? emailConfig.isSMTPAuthentification() : false;
        
        String senderAddress = ""; 
        
        if(c.getCreatedBy().getEmail()!=null)
            senderAddress = c.getCreatedBy().getEmail(); 
        else
            senderAddress = "noreply@openiacrm.com";
            
        int port = emailConfig.getSmtpPort().intValue();
        
        if(c.getDeadlineDate() != null){
            ICalMaker calendarIcsFileMaker = new ICalMaker();
            calendarIcsFileMaker.setSubject(c.getCaseSubject());
            calendarIcsFileMaker.setStart(c.getDeadlineDate());
            
            contextPath = org.openbravo.dal.core.DalContextListener.getServletContext().getRealPath(c.getId()+"case.ics");
            
            try{
                calendarCaseFile = calendarIcsFileMaker.Create(contextPath);
            }catch(IOException e){
                Logger.getLogger(CaseSender.class.getName()).log(Level.SEVERE, null, e);
                return -1;
            } 
        }
        
        ArrayList <File> attachment = new ArrayList<File>();
        if(calendarCaseFile!=null)
            attachment.add(calendarCaseFile);
    
        //spedisco il caso a tutti gli utenti che hanno visibilita'
        for (opcrmCasesAccess guest : casesAccessRecord.list()) {
            
            if(guest.getUserContact().getId() == assignedUserId)
                sentToAssignedUser = true;
            
            if(guest.getUserContact() != null && guest.getUserContact().getEmail() != null){
                try {
                    org.openbravo.erpCommon.utility.poc.EmailManager.sendEmail(host, authentication, user, CryptoUtility.decrypt(password), null, port,
                        senderAddress, guest.getUserContact().getEmail(), null, null,
                        null, "[Case Update] " + c.getCaseSubject(), getMessage(c), "text/html", (!attachment.isEmpty())?attachment:null, new java.util.Date(), null);
                }catch (Exception e) {
                    Logger.getLogger(CaseSender.class.getName()).log(Level.SEVERE, null, e);
                    return -1;
                }
            
            }
        
        }
        
        //If mail hasn't been sent to the assigned user --> send him the ticket
        if(c.getAssignedTo() !=null && c.getAssignedTo().getEmail() != null && !sentToAssignedUser){
            try {
                    org.openbravo.erpCommon.utility.poc.EmailManager.sendEmail(host, authentication, user, CryptoUtility.decrypt(password), null, port,
                        senderAddress, c.getAssignedTo().getEmail(), null, null,
                        null, "[Case Update] " + c.getCaseSubject(), getMessage(c), "text/html", (!attachment.isEmpty())?attachment:null, new java.util.Date(), null);
                }catch (Exception e) {
                    Logger.getLogger(CaseSender.class.getName()).log(Level.SEVERE, null, e);
                    return -1;
                }
        }
        
        // Attempt to delete file
        boolean success = false;
        if(calendarCaseFile!=null){
            success = calendarCaseFile.delete();
        
            if (!success){
                Logger.getLogger(CaseSender.class.getName()).log(Level.SEVERE, null, "Delete: File "+calendarCaseFile.getName()+" deletion failed");
                return -1;
            }
        }
        return 1;
    }
    
    private String getMessage(Opcrmcase c){
        
        StringBuilder str = new StringBuilder();
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        
        str.append("<div>");
        
        //SUBJECT
        str.append("<b>"+MailUtils.GetField("3B36A3EFC4D94AF2BF9E4442906D333C")+"</b>: ").append(c.getCaseSubject()).append("<br>");
        
        //TYPE
        if(c.getOpcrmWorktype()!=null)
            str.append("<b>"+MailUtils.GetField("A3E6CAB920214B0EA8A6ED9BBE3D8642")+"</b>: ").append(c.getOpcrmWorktype().getCommercialName()).append("<br>");
        
        //ACTIVITY
        if(c.getOpcrmCaseactivity()!=null)
            str.append("<b>"+MailUtils.GetField("18E63DAF90754CFF850C417D378C0756")+"</b>: ").append(c.getOpcrmCaseactivity().getCommercialName()).append("<br>");
        
        //PRIORITY
        if(c.getCasePriority()!=null)
            str.append("<b>"+MailUtils.GetField("0AA7D66896484AB0904E610176EC6660")+"</b>: ").append(getText("opcrmCaseProrityList", c.getCasePriority(), lang)).append("<br>");
        
        //STATUS
        if(c.getCaseStatus()!=null)
            str.append("<b>"+MailUtils.GetField("FC0F21CAEF784EEC8ECD14F8836C73B8")+"</b>: ").append(getText("opcrmCaseStatusList", c.getCaseStatus(), lang)).append("<br>");
        
        //DESCRIPTION
        if(c.getCaseDescription() != null && !c.getCaseDescription().isEmpty())
            str.append("<b>"+MailUtils.GetField("EAD1E12F63B848C1ADF33642625A5BDC")+"</b>: ").append(c.getCaseDescription()).append("<br>");
        
        //RESOLUTION
        if(c.getCaseResolution() != null && !c.getCaseResolution().isEmpty())
            str.append("<b>"+MailUtils.GetField("1CDB88CC85724F8FA636C99D6ACC6DA3")+"</b>: ").append(c.getCaseResolution()).append("<br>");
        
        //BUSINESS PARTNER
        if(c.getBusinessPartner()!=null)
            str.append("<b>"+MailUtils.GetField("1758244022824C839C273631D8134397")+"</b>: ").append(c.getBusinessPartner().getName()).append("<br>");
        
        
        //ASSIGNED TO
        StringBuilder assignedTo=new StringBuilder();
        if(c.getAssignedTo()!=null){
            if(c.getAssignedTo().getName() != null)
                assignedTo.append(c.getAssignedTo().getName());
            
            if(c.getAssignedTo().getLastName() != null)
                assignedTo.append(" ");
                assignedTo.append(c.getAssignedTo().getLastName());
        }
        if(!assignedTo.toString().isEmpty())
            str.append("<b>"+MailUtils.GetField("72A2FF4DC797466994A36EE0EFAAED90")+"</b>: ").append(assignedTo.toString()).append("<br>");
        
        //DEADLINE
        SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        if(c.getDeadlineDate()!=null)
            str.append("<b>"+MailUtils.GetField("6E6EEDCE62DA494386D2078C25A05777")+"</b>: ").append(dt.format(c.getDeadlineDate())).append("<br>");
        
        str.append("</div>");
        
        return str.toString();
    }
    
    private String getText(String refName, String listKey, String lang){
        
        String res = "";
        
        OBContext.setAdminMode(true);
        
        OBCriteria<Reference> refRecord = OBDal.getInstance().createCriteria(Reference.class);
        refRecord.add(Restrictions.eq(Reference.PROPERTY_NAME,refName));
        
        OBCriteria<List> listRecord = OBDal.getInstance().createCriteria(List.class);
        listRecord.add(Restrictions.eq(List.PROPERTY_SEARCHKEY,listKey));
        
        OBCriteria<Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,lang));
        
        OBCriteria<ListTrl> listTrlRecord = OBDal.getInstance().createCriteria(ListTrl.class);
        listTrlRecord.add(Restrictions.eq(ListTrl.PROPERTY_LISTREFERENCE,listRecord.list().get(0)));
        listTrlRecord.add(Restrictions.eq(ListTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        if(!listTrlRecord.list().isEmpty())
            res = listTrlRecord.list().get(0).getName(); 
        else
            res = listRecord.list().get(0).getName();
        
        OBContext.restorePreviousMode();
        
        return res;
    }
    
    
}
