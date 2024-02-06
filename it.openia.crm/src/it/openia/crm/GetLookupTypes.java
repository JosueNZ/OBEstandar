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
public class GetLookupTypes extends HttpBaseServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {

        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }
        
        response.setContentType("application/json; charset=utf-8");
        VariablesSecureApp vars = new VariablesSecureApp(request);

        String lang = vars.getLanguage();
        if (lang.isEmpty()) {
            lang = OBContext.getOBContext().getLanguage().getLanguage();
        }

        PrintWriter out = response.getWriter();

        try {
            out.println(GetLookupTypesJSon(lang));
        } catch (Exception ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }


    }

    public String GetLookupTypesJSon(String lang) throws JobExecutionException {
        String res = "";

        ArrayList lookuptypes;

        try {
            lookuptypes = GetLookupTypesArray(lang);
        } catch (Exception e) {
            throw new JobExecutionException(e.getMessage(), e);
        }

        res = ToJson(lookuptypes);

        return res;
    }

    public ArrayList<LookupType> GetLookupTypesArray(String lang) throws JobExecutionException {


        ArrayList<LookupType> typeList = new ArrayList<LookupType>();

        //hardcoded window ids
        String quotationWindowId = "6CB5B67ED33F47DFA334079D3EA2340E"; 
        String salesOrderWindowId = "143";
        String gShipmentWindowId = "169";
        String salesInvoiceWindowId = "167";
        String activityWindowId = "62BBB1F6FB0A4BE18266EAFBD33226C0";
        String opportunityWindowId = "EE70F389626F4BD9BF96DA5267338B87";
        ArrayList<String> windowIds = new ArrayList<String>();

        windowIds.add(quotationWindowId);
        windowIds.add(salesOrderWindowId);
        windowIds.add(gShipmentWindowId);
        windowIds.add(salesInvoiceWindowId);
        windowIds.add(activityWindowId);
        windowIds.add(opportunityWindowId);


        OBCriteria<Window> windowList;
        OBCriteria<Language> langList;
        OBContext.setAdminMode(true);

        langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        int enumcount = 0;

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

                typeList.add(new LookupType(windowList.list().get(0).getName(), LookupEnum.values()[enumcount]));
                enumcount++;
            }

        }

        OBContext.restorePreviousMode();

        return typeList;
    }

    public String ToJson(ArrayList<LookupType> lookupTypes) {

        LookupTypeList typeList = new LookupTypeList();
        typeList.setLookupTypeList(lookupTypes);

        return JSONSerializer.getJSON(typeList);
    }
}
