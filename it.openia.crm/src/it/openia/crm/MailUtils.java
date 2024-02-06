/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.ad.ui.Message;
import org.openbravo.model.ad.ui.MessageTrl;

/**
 *
 * @author diurno
 */
public class MailUtils {

    public static final int INVITATION = 1;
    public static final int REMINDER = 2;

    public static String getMessage(Opcrmactivity activity, int type) {

        StringBuilder res = new StringBuilder();

        String typeStr = null;
        if (type == INVITATION) {
            typeStr = "INVITATION";
        } else if (type == REMINDER) {
            typeStr = "REMINDER";
        }

        DateTime when = new DateTime(activity.getActivityStartdate());
        String whenStr = when.getDayOfMonth() 
                + "/" + when.getMonthOfYear() 
                + "/" + when.getYear() 
                + " - " 
                + when.getHourOfDay() 
                + ":" + when.getMinuteOfHour();

        String description = activity.getDescription();
        if (description == null) {
            description = "";
        }

        res.append("<div>");
        res.append("<b>").append(typeStr).append("</b>").append("<br><hr>");
        res.append("<b>"+MailUtils.GetField("C0983A77EEF145FD98D032500C43C6A8")+"</b>: ").append(activity.getActivitySubject()).append("<br>");
        res.append("<b>"+MailUtils.GetField("B7086C8DB5044690B058D61BFEF62E5A")+"</b>: ").append(whenStr).append("<br>");
        res.append("<b>"+MailUtils.GetField("E57398E301A44BB3B7CC5457C37F6145")+"</b>: ").append(description).append("<br>");
        res.append("</div>");

        return res.toString();
    }
    
    public static String GetField(String id){
        
        OBCriteria<Field> fieldList;
        OBCriteria<Language> langList;
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        String res="";
        
        langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        
        OBContext.setAdminMode(true);
        
        fieldList = OBDal.getInstance().createCriteria(Field.class);
        fieldList.add(Restrictions.eq(Field.PROPERTY_ID, id));
        
        if (!lang.matches("en_US")) {
            OBCriteria<FieldTrl> trlList;
            trlList = OBDal.getInstance().createCriteria(FieldTrl.class);
            trlList.add(Restrictions.eq(FieldTrl.PROPERTY_FIELD, fieldList.list().get(0)));
            trlList.add(Restrictions.eq(FieldTrl.PROPERTY_LANGUAGE, langList.list().get(0)));
            
            if (!trlList.list().isEmpty())
                res = trlList.list().get(0).getName();
            else
                res = fieldList.list().get(0).getName();
        }
        else 
            res = fieldList.list().get(0).getName();
        
        OBContext.restorePreviousMode();
        
        
        return res;
    }
    
    public static String GetMessage(String key){
        OBCriteria<Message> messageList;
        OBCriteria<Language> langList;
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        String res="";
        
        langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        
        OBContext.setAdminMode(true);
        
        messageList = OBDal.getInstance().createCriteria(Message.class);
        messageList.add(Restrictions.eq(Message.PROPERTY_SEARCHKEY, key));
        
        if (!lang.matches("en_US")) {
            OBCriteria<MessageTrl> trlList;
            trlList = OBDal.getInstance().createCriteria(MessageTrl.class);
            trlList.add(Restrictions.eq(MessageTrl.PROPERTY_MESSAGE, messageList.list().get(0)));
            trlList.add(Restrictions.eq(MessageTrl.PROPERTY_LANGUAGE, langList.list().get(0)));
            
            if (!trlList.list().isEmpty())
                res = trlList.list().get(0).getMessageText();
            else
                res = messageList.list().get(0).getMessageText();
        }
        else 
            res = messageList.list().get(0).getMessageText();
        
        OBContext.restorePreviousMode();
        
        
        return res;

    }
}