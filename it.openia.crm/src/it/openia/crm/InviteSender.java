/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.utils.CryptoUtility;
import org.quartz.JobExecutionException;

/**
 *
 * @author nicholas
 */
public class InviteSender  extends DalBaseProcess{
    
    @Override
    public void doExecute(ProcessBundle bundle) throws Exception {
        
        final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ACTIVE, true));
        activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ID,bundle.getParams().get("Opcrm_Activity_ID")));
        
        
        if(!activityRecords.list().isEmpty())
            try {
                sendMail(activityRecords.list().get(0));
            }catch(JobExecutionException e){
                Logger.getLogger(CalendarIcsFileGenerator.class.getName()).log(Level.SEVERE, null, e);
                return;
            }
        
    }
    
    
    public void sendMail(Opcrmactivity activity) throws JobExecutionException {
        
        String message = MailUtils.getMessage(activity, MailUtils.INVITATION);
        String contextPath="";
        
        final OBCriteria<opcrmGuest> guestList = OBDal.getInstance().createCriteria(opcrmGuest.class);
        guestList.add(Restrictions.eq(opcrmGuest.PROPERTY_OPCRMACTIVITY, activity));
        User destUser;

        final OBCriteria<EmailServerConfiguration> emailServerConfigurationRecord = OBDal.getInstance().createCriteria(EmailServerConfiguration.class);
        emailServerConfigurationRecord.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_CLIENT, activity.getClient()));
        EmailServerConfiguration emailConfig = emailServerConfigurationRecord.list().get(0);

        String host = emailConfig.getSmtpServer();
        String user = emailConfig.getSmtpServerAccount() == null ? "" : emailConfig.getSmtpServerAccount();
        String password = emailConfig.getSmtpServerPassword() == null ? "" : emailConfig.getSmtpServerPassword();
        
        
        String senderAddress = ""; 
        
        if(activity.getCreatedBy().getEmail()!=null)
            senderAddress = activity.getCreatedBy().getEmail(); 
        else
            senderAddress = "noreply@crm.com";
            
        int port = emailConfig.getSmtpPort().intValue();
        
        
        //mando l'invito a tutti i guest
        for (opcrmGuest guest : guestList.list()) {
            
            destUser = guest.getUserContact();
            
            //Create Invitation File
                ICalMaker calendarIcsFileMaker = new ICalMaker();
                calendarIcsFileMaker.setSubject(activity.getActivitySubject());
                calendarIcsFileMaker.setStart(activity.getActivityStartdate());
                calendarIcsFileMaker.setEnd(GetEndDate(activity));
                
                File calendarActivityFile;
                String guestName=""; String guestLastName=""; String guestEmail="";
                String organizerName="";String organizerLastName=""; String organizerEmail="";
                
                if(guest.getUserContact().getEmail()!=null)
                    guestEmail=guest.getUserContact().getEmail();
                else guestEmail =""; //Guest EMAIL must be initialized!
                    
                if(guest.getUserContact().getName()!=null)
                    guestName = guest.getUserContact().getName();
                else guestName="";
                    
                if(guest.getUserContact().getLastName()!=null)
                    guestLastName = guest.getUserContact().getLastName();
                else guestLastName="";
                
                if(activity.getCreatedBy().getEmail() != null)
                    organizerEmail = activity.getCreatedBy().getEmail();
                
                if(activity.getCreatedBy().getName() != null)
                    organizerName = activity.getCreatedBy().getName();
                
                if(activity.getCreatedBy().getLastName() != null)
                    organizerLastName = activity.getCreatedBy().getLastName();
                
                contextPath = org.openbravo.dal.core.DalContextListener.getServletContext().getRealPath(activity.getId()+"invite.ics");
                
                try{
                    calendarActivityFile = calendarIcsFileMaker.CreateGuest(contextPath, guestEmail, guestName+" "+guestLastName, organizerEmail, organizerName+" "+organizerLastName);
                }catch(IOException e){
                    Logger.getLogger(CalendarIcsFileGenerator.class.getName()).log(Level.SEVERE, null, e);
                    return;
                }
                
                ArrayList <File> attachment = new ArrayList<File>();
                attachment.add(calendarActivityFile);
                
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(new FileReader(calendarActivityFile));
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(InviteSender.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                String line = null;
                StringBuilder str = new StringBuilder();
                String ls = System.getProperty("line.separator");
                
                try {
                    
                    while( ( line = reader.readLine() ) != null ) {
                        str.append( line );
                        str.append( ls );
                    }
                
                } catch (IOException ex) {
                    Logger.getLogger(InviteSender.class.getName()).log(Level.SEVERE, null, ex);
                }
            
                try {
                    it.openia.crm.EmailManager.sendEmail(host, true, user, CryptoUtility.decrypt(password), null, port,
                        senderAddress, destUser.getEmail(), null, null,
                        null, "[Activity] " + activity.getActivitySubject(), message, "text/html;", attachment, "text/calendar;method=\"REQUEST\";charset=\"UTF-8\";",new java.util.Date(), null);
                } catch (Exception ex) {
                    Logger.getLogger(InviteSender.class.getName()).log(Level.SEVERE, null, ex);
                }

                boolean success = calendarActivityFile.delete();
                if(!success)
                    Logger.getLogger(InviteSender.class.getName()).log(Level.SEVERE, null, "Delete: File "+calendarActivityFile.getName()+" deletion failed");
        }
        
            
    }
    
    public Date GetEndDate(Opcrmactivity activity){
        Calendar cal = Calendar.getInstance();
        
        cal.setTime(activity.getActivityStartdate()); 
        cal.add(Calendar.HOUR_OF_DAY, activity.getDurationHours().intValue()); 
        cal.add(Calendar.MINUTE, activity.getDurationMinutes().intValue());
        
       return cal.getTime();
    }
    
    
}
