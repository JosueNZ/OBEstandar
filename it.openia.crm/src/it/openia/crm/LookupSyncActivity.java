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
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.Reference;

/**
 *
 * @author nicholas
 */
public class LookupSyncActivity extends HttpBaseServlet {
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }
        
        long status=-1; String actId=""; String type=""; String actStatusId="";
        
        if(request.getParameter("leadstatus")!=null)
            status = Long.valueOf(request.getParameter("leadstatus"));
        
        actStatusId = request.getParameter("activityStatusId");
        actId = request.getParameter("activityId");
        type = request.getParameter("updateType");
        
        PrintWriter out = response.getWriter();
        
        try {
            if(type.matches("leadStatus"))
                UpdateLeadStatus(status, actId);
            if(type.matches("activityStatus")) 
                UpdateActivityStatus(actStatusId,actId);
            
            out.println("OK");
        } catch (Exception ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
            out.println("Error, please check logfile");
        }
    }
    
    public void UpdateLeadStatus(long statusCode, String id){
        
        final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ID, id));
        Opcrmactivity activity = activityRecords.list().get(0);
        
        if(statusCode >=0)
            activity.setLeadstatus(GetStatusSearchKey(statusCode));
        else     
            activity.setLeadstatus(null);
        
        OBDal.getInstance().save(activity);
        OBDal.getInstance().flush();
    }
    
    public String GetStatusSearchKey(long number){
        String res="";
        
        OBContext.setAdminMode(true);
        final OBCriteria <Reference> statRef = OBDal.getInstance().createCriteria(Reference.class);
        statRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "leadStatus"));
        
        final OBCriteria <org.openbravo.model.ad.domain.List> statRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
        statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,statRef.list().get(0)));
        statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEQUENCENUMBER, number));
        res = statRefList.list().get(0).getSearchKey();
            
        OBContext.restorePreviousMode();
        
        return res;
    }
    
    public void UpdateActivityStatus(String statId,String actId){
        final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ID, actId));
        Opcrmactivity activity = activityRecords.list().get(0);
        
        if(!statId.isEmpty()){
            final OBCriteria <Opcrmstatusfilter> actFilter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
            actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ID, statId));
            activity.setActivityStatus(actFilter.list().get(0));
        }
        else
            activity.setActivityStatus(null);
        
        OBDal.getInstance().save(activity);
        OBDal.getInstance().flush();
    }
}
