/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm.beh;

import it.openia.crm.MailUtils;
import it.openia.crm.Opcrmactivity;
import it.openia.crm.opcrmGuest;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.event.Observes;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.utils.CryptoUtility;

public class InviteGuestToActivity extends EntityPersistenceEventObserver {

    private static Entity[] entities = {ModelProvider.getInstance().getEntity(opcrmGuest.ENTITY_NAME)};

    @Override
    protected Entity[] getObservedEntities() {
        return entities;
    }

    public void onUpdate(@Observes EntityUpdateEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
    }
    
    
    @Deprecated   
    public void onSave(@Observes EntityNewEvent event) {
        /*
        if (!isValidEvent(event)) {
            return;
        }

        String destAddress = null;

        Opcrmactivity activity = null;
        opcrmGuest guest = null;

        for (Object o : event.getCurrentState()) {
            if (o instanceof Opcrmactivity) {
                activity = (Opcrmactivity) o;
            }
            if (o instanceof User) {
                destAddress = ((User) o).getEmail();
            }
        }

        final OBCriteria<EmailServerConfiguration> emailServerConfigurationRecord = OBDal.getInstance().createCriteria(EmailServerConfiguration.class);
        emailServerConfigurationRecord.add(Restrictions.eq(EmailServerConfiguration.PROPERTY_CLIENT, activity.getClient()));
        EmailServerConfiguration emailConfig = emailServerConfigurationRecord.list().get(0);

        String host = emailConfig.getSmtpServer();
        String user = emailConfig.getSmtpServerAccount() == null ? "" : emailConfig.getSmtpServerAccount();
        String password = emailConfig.getSmtpServerPassword() == null ? "" : emailConfig.getSmtpServerPassword();
        String senderAddress = emailConfig.getSmtpServerSenderAddress() == null ? "openiacrm@example.com" : emailConfig.getSmtpServerSenderAddress();
        String message = MailUtils.getMessage(activity, MailUtils.INVITATION);
        int port = emailConfig.getSmtpPort().intValue();

        try {
            EmailManager.sendEmail(host, true, user, CryptoUtility.decrypt(password), null, port,
                    senderAddress, destAddress, null, null,
                    null, "[Activity] " + activity.getActivitySubject(), message, "text/html", null, new java.util.Date(), null);

        } catch (Exception ex) {
            Logger.getLogger(InviteGuestToActivity.class.getName()).log(Level.SEVERE, null, ex);
        }
        */
    }

    public void onDelete(@Observes EntityDeleteEvent event) {
        if (!isValidEvent(event)) {
            return;
        }
    }
    
}