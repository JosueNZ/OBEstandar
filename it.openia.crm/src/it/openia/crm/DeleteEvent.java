/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package it.openia.crm;

import java.io.IOException;
import java.io.PrintWriter;
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

/**
 *
 * @author nicholas
 */
public class DeleteEvent extends HttpBaseServlet {
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
        ServletException {
        
        
        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }
        
        String id="";
        response.setContentType("text/html; charset=utf-8");
        VariablesSecureApp vars = new VariablesSecureApp(request);
        String strUserId = vars.getUser();
        if (strUserId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getUser().getId();
        }
        
        PrintWriter out = response.getWriter();
        
        id = request.getParameter("id");
        
        DeleteActivity(id);
    
    }
    
    public String DeleteActivity(String id){
        String res="";
        final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ID, id));
        Opcrmactivity activity = activityRecords.list().get(0);
        
        if (activity == null) return null;
        try{
        OBDal.getInstance().remove(activity);
        OBDal.getInstance().flush();
        } catch(Exception e){
            Logger.getLogger(DeleteEvent.class.getName()).log(Level.SEVERE, null, e);
        }
        return res;
    }
    
}
