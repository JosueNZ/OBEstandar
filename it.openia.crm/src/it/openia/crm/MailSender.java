/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.List;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.utils.CryptoUtility;
import org.quartz.JobExecutionException;

/**
 *
 * @author nicholas
 */
public class MailSender extends DalBaseProcess {

    private ProcessLogger logger;
    private int port;

    @Override
    public void doExecute(ProcessBundle bundle) throws Exception {
        Connection conn = bundle.getConnection().getConnection();
        logger = bundle.getLogger();
        Client activityClient = null;

        java.util.Date date = new java.util.Date();
        Timestamp now = new Timestamp(date.getTime());

        final OBCriteria<Client> clientList = OBDal.getInstance().createCriteria(Client.class);
        clientList.add(Restrictions.eq(Client.PROPERTY_ID, bundle.getContext().getClient()));
        if (!clientList.list().isEmpty()) {
            activityClient = clientList.list().get(0);
        }

        try {
            //Estrazione prossime attivita' in calendario con il reminder diverso da null
            final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
            activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ACTIVE, true));
            activityRecords.add(Restrictions.gt(Opcrmactivity.PROPERTY_ACTIVITYSTARTDATE, now));
            activityRecords.add(Restrictions.isNotNull(Opcrmactivity.PROPERTY_REMINDER));
            if (activityClient != null) {
                activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_CLIENT, activityClient));
            }

            for (Opcrmactivity activity : activityRecords.list()) {
                if (reminderActivate(now, activity.getActivityStartdate(), activity.getReminder())) {
                    sendMail(activity);
                }
            }

        } catch (Exception e) {
            logger.log("Error: " + e.getMessage());
            throw new JobExecutionException(e.getMessage(), e);
        }
    }

    public boolean reminderActivate(Timestamp now, Date activitytime, String reminder) {

        boolean remind = false;

        // calcolo differenza tra l'ora attuale e l'ora dell'attivita'
        long diff = activitytime.getTime() - now.getTime();
        long diffMinutes = diff / (60 * 1000);

        OBContext.setAdminMode(true);

        // estraggo la voce relativa al reminder dell'attivita'
        final OBCriteria<Reference> reference = OBDal.getInstance().createCriteria(Reference.class);
        reference.add(Restrictions.eq(Reference.PROPERTY_NAME, "opcrmReminderList"));

        final OBCriteria<List> referenceList = OBDal.getInstance().createCriteria(List.class);
        referenceList.add(Restrictions.eq(List.PROPERTY_REFERENCE, reference.list().get(0)));
        referenceList.add(Restrictions.eq(List.PROPERTY_SEARCHKEY, reminder));


        // switch case : se i minuti di differenza coincidono con il reminder il metodo restituisce true
        if (!referenceList.list().isEmpty()) {

            switch ((int) diffMinutes) {
                case 1:
                    if (referenceList.list().get(0).getSequenceNumber() == 0) {
                        remind = true;
                    }
                    break;
                case 5:
                    if (referenceList.list().get(0).getSequenceNumber() == 1) {
                        remind = true;
                    }
                    break;
                case 15:
                    if (referenceList.list().get(0).getSequenceNumber() == 2) {
                        remind = true;
                    }
                    break;
                case 30:
                    if (referenceList.list().get(0).getSequenceNumber() == 3) {
                        remind = true;
                    }
                    break;
                case 60:
                    if (referenceList.list().get(0).getSequenceNumber() == 4) {
                        remind = true;
                    }
                    break;

            }
        }
        OBContext.restorePreviousMode();

        return remind;
    }

    public void sendMail(Opcrmactivity activity) throws JobExecutionException {

        boolean found = false;
        String message = MailUtils.getMessage(activity, MailUtils.REMINDER);

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

        //mando il reminder a tutti i guest
        for (opcrmGuest guest : guestList.list()) {
            destUser = guest.getUserContact();
            try {
                
                it.openia.crm.EmailManager.sendEmail(host, true, user, CryptoUtility.decrypt(password), null, port,
                        senderAddress, destUser.getEmail(), null, null,
                        null, "[Activity] " + activity.getActivitySubject(), message, "text/html;", null, "text/calendar;method=\"REQUEST\";charset=\"UTF-8\";",new java.util.Date(), null);

                if (activity.getAssignedTo() != null & destUser == activity.getAssignedTo()) {
                    found = true;
                }
            } catch (Exception e) {
                logger.log("Error: " + e.getMessage());
                throw new JobExecutionException(e.getMessage(), e);
            }
        }

        //se non e' stata ancora mandata allo user al quale l'attivita' e' stata assegnata allora spedisco anche a lui il reminder
        if (activity.getAssignedTo() != null & found == false) {
            try {
                EmailManager.sendEmail(host, true, user, CryptoUtility.decrypt(password), null, port,
                        senderAddress, activity.getAssignedTo().getEmail(), null, null,
                        null, "[Activity] " + activity.getActivitySubject(), message, "text/html", null, new java.util.Date(), null);
            } catch (Exception e) {
                logger.log("Error: " + e.getMessage());
                throw new JobExecutionException(e.getMessage(), e);
            }
        }


    }
    
}
