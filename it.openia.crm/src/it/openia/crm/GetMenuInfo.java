/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package it.openia.crm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.ad.ui.WindowTrl;
import org.quartz.JobExecutionException;

/**
 *
 * @author nicholas
 */
public class GetMenuInfo extends HttpBaseServlet{
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        
        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }
        
        response.setContentType("application/json; charset=utf-8");
        
        boolean logoutButton=false;
        VariablesSecureApp vars = new VariablesSecureApp(request);
        String strUserId = vars.getUser();

        if (strUserId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getUser().getId();
            logoutButton=true;
        }
        
        String lang = vars.getLanguage();
        if (lang.isEmpty()) {
            lang = OBContext.getOBContext().getLanguage().getLanguage();
        }
        
        PrintWriter out = response.getWriter();
        
        try {
            out.println(GetMenuInfoJSon(lang,logoutButton));
        } catch (Exception ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }
    
    public String GetMenuInfoJSon(String lang, boolean logoutBtn) throws JobExecutionException {
        String res = "";

        ArrayList lookuptypes;

        try {
            lookuptypes = GetMenuInfoArray(lang);
        } catch (Exception e) {
            throw new JobExecutionException(e.getMessage(), e);
        }

        res = ToJson(lookuptypes, logoutBtn);

        return res;
    }
    
    public ArrayList<LookupType> GetMenuInfoArray(String lang) throws JobExecutionException {


        ArrayList<LookupType> typeList = new ArrayList<LookupType>();

        //hardcoded window ids
        String activityWindowId = "62BBB1F6FB0A4BE18266EAFBD33226C0";
        String opportunityWindowId = "EE70F389626F4BD9BF96DA5267338B87";
        String bPartnerWindowId = "123";
        String leadsWindowId = "69469E04533A46EDA7D923964AE18BCA";
        ArrayList<String> windowIds = new ArrayList<String>();

        windowIds.add(activityWindowId);
        windowIds.add(opportunityWindowId);
        windowIds.add(bPartnerWindowId);
        windowIds.add(leadsWindowId);
        
        OBCriteria<Window> windowList;
        OBCriteria<Language> langList;
        OBContext.setAdminMode(true);

        langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        int enumcount = 4; //getting the last 3 enums of the set

        if (!lang.contains("en_")) {

            OBCriteria<WindowTrl> trlList;

            for (String wId : windowIds) {

                windowList = OBDal.getInstance().createCriteria(Window.class);
                windowList.add(Restrictions.eq(Window.PROPERTY_ID, wId));

                trlList = OBDal.getInstance().createCriteria(WindowTrl.class);
                trlList.add(Restrictions.eq(WindowTrl.PROPERTY_WINDOW, windowList.list().get(0)));
                trlList.add(Restrictions.eq(WindowTrl.PROPERTY_LANGUAGE, langList.list().get(0)));

                if (!trlList.list().isEmpty()) {
                    typeList.add(new LookupType(trlList.list().get(0).getName(), LookupEnum.values()[enumcount]));
                } else {
                    typeList.add(new LookupType(windowList.list().get(0).getName(), LookupEnum.values()[enumcount]));
                }

                enumcount++;
            }

        } else {
            enumcount = 0;
            for (String wId : windowIds) {

                windowList = OBDal.getInstance().createCriteria(Window.class);
                windowList.add(Restrictions.eq(Window.PROPERTY_ID, wId));

                typeList.add(new LookupType(windowList.list().get(0).getName(), LookupEnum.values()[enumcount+4]));
                enumcount++;
            }

        }

        OBContext.restorePreviousMode();

        return typeList;
    }
    
    
    public String ToJson(ArrayList<LookupType> lookupTypes, boolean lgout) {

        LookupTypeList typeList = new LookupTypeList();
        typeList.setLookupTypeList(lookupTypes);
        typeList.setLogout(lgout);

        return JSONSerializer.getJSON(typeList);
    }
    
}
