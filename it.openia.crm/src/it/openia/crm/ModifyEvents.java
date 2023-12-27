/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

/**
 *
 * @author nicholas
 */
public class ModifyEvents extends HttpBaseServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
    
        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }
        
        String eventId, dayDelta, minuteDelta, allDay;
        String name, descr, startdate, enddate;
        
        eventId = request.getParameter("id");
        dayDelta = request.getParameter("dayDt");
        minuteDelta = request.getParameter("minuteDt");
        allDay = request.getParameter("daily");
        
        
        name = request.getParameter("name");
        descr = request.getParameter("descr");
        startdate = request.getParameter("startdate");
        enddate = request.getParameter("enddate");
        
        if(!dayDelta.isEmpty())
            FullCalUpdateEvent(eventId,dayDelta,minuteDelta,allDay);
        else
            ToolTipUpdateEvent(eventId, name, descr, startdate, enddate);
        
    }
    
    public void FullCalUpdateEvent(String id, String dayDelta, String minDelta, String allDay){
        
        int dayDiff = Integer.parseInt(dayDelta);
        int minuteDiff = Integer.parseInt(minDelta);
        
        final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ID, id));
        
        Opcrmactivity activity = activityRecords.list().get(0);
        Calendar c = Calendar.getInstance();  
        Timestamp updated;
        
        //event dragging
        if(allDay!=null){
            c.setTime(activity.getActivityStartdate());    
            c.add(Calendar.DAY_OF_MONTH, dayDiff);
            c.add(Calendar.MINUTE, minuteDiff);
            updated = new Timestamp(c.getTimeInMillis());
            activity.setActivityStartdate(updated);
            activity.setAllday(Boolean.parseBoolean(allDay));
            OBDal.getInstance().save(activity);
        }
        //event resize
        else{
            c.setTime(activity.getActivityStartdate());    
            c.add(Calendar.DAY_OF_MONTH, dayDiff);
            c.add(Calendar.MINUTE, minuteDiff);
            updated = new Timestamp(c.getTimeInMillis());
            activity.setActivityStartdate(updated);
            OBDal.getInstance().save(activity);
        }
        
    
    }
    
    public void ToolTipUpdateEvent(String id, String name, String descr, String start, String end){
        final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ID, id));
        
        Opcrmactivity activity = activityRecords.list().get(0);
        
        SimpleDateFormat dateFormat =  new SimpleDateFormat("dd/MM/yyyy");

        Date startDate;
        Date endDate;
        
        if (!name.isEmpty())
            activity.setActivitySubject(name);
        
        if (descr != null)
            activity.setDescription(descr);
        
        if(!start.isEmpty()){
            try{
            
                startDate = dateFormat.parse(start);
                activity.setActivityStartdate(startDate);
            
            }catch(ParseException e1){
                startDate = null;
            }
        }
        
        if(!end.isEmpty()){
            try{
            
                endDate = dateFormat.parse(start);
                activity.setActivityStartdate(endDate);
            
            }catch(ParseException e1){
                endDate = null;
            }
        }
        
        OBDal.getInstance().save(activity);
        
        
    }
    
}
