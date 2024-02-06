/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package it.openia.crm.ad_forms;

import it.openia.crm.AuthUtils;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
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
 * @author nicholas
 */
public class CalendarIcsDownload extends HttpBaseServlet {
    
    private static final long serialVersionUID = 1L;
    public static String actId="";
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {

        VariablesSecureApp vars = new VariablesSecureApp(request);
        
        actId = vars.getGlobalVariable("Opcrm_Activity_ID", "", "");
        
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/web/it.openia.crm/downloadics.jsp");
        dispatcher.forward(request, response);
        return;
    }
    
    public static String GetActId(){
        return actId;
    }
    
    public static String GetMessage(String key){
        
        //OPCRM_DOWNLOADING
        
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
