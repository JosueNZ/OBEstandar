/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 *
 * @author nicholas
 */
public class InsertEvent extends HttpBaseServlet {
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
    
        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }
        
        String from, to, info, descr, id, planNext, activityStatusId, leadId, eventDone; 
        long idActType, idStatCode;
        response.setContentType("text/html; charset=utf-8");
            
        from = request.getParameter("from");
        to = request.getParameter("enddate");
        info = request.getParameter("info");
        descr = request.getParameter("descr");
        id = request.getParameter("id");
        idActType = Long.valueOf(request.getParameter("idtype"));

        //planNext is set to "Y" if i'm planning a next event on the information of its previous one
        planNext = request.getParameter("plannext");
        
        //eventDone is set to "Y" if i'm inserting or modifying an event and i want to set it directly to its closing state
        eventDone = request.getParameter("eventDone");
        
        if(request.getParameter("statName")!=null && !request.getParameter("statName").isEmpty())
            idStatCode = Long.valueOf(request.getParameter("statName"));
        else idStatCode = 0;
        
        activityStatusId = request.getParameter("actStatus");
        leadId = request.getParameter("leadId");
        
        VariablesSecureApp vars = new VariablesSecureApp(request);
        String strUserId = vars.getUser();
        if (strUserId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getUser().getId();
        }
        
        String strClientId = OBContext.getOBContext().getCurrentClient().getId();
        String strOrgId = OBContext.getOBContext().getCurrentOrganization().getId();
        PrintWriter out = response.getWriter();
        
        try{
            //if the activity id is empty then the activity has to be created from scratch
            if(id.isEmpty()){
            			System.out.println("Now the output is redirected!");
                insertEvent(from, to, info, descr, strUserId, strClientId, strOrgId, idActType, activityStatusId, leadId, eventDone);
            }
            else {
                if(planNext.matches("Y"))
                    planNextEvent(from, to, info, descr, id, idActType, strUserId, strClientId, strOrgId, idStatCode, activityStatusId);
                else    
                    modifyEvent(from, to, info, descr, id, idActType, idStatCode, activityStatusId, eventDone);
            }
            
            out.print("OK");
        }catch(Exception e){
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, e);
            out.println(e.toString());
        } 
    }
    
    public String insertEvent (String from, String to, String info, String descr, String userId, String clientId, String orgId, long idActivity, String activityStatusId, String leadId, String eventDone){
        String res="";
        String startHH="";
        String name="";
        Date startDate;
        Date endDate;
        long durationHours=0;
        long durationMinutes=0;
        SimpleDateFormat all =  new SimpleDateFormat("dd/MM/yyyy HH:mm");
        SimpleDateFormat justDate =  new SimpleDateFormat("dd/MM/yyyy");
        //Pattern pattern = Pattern.compile("(([01]?[0-9]|2[0-3]):[0-5][0-9])");
        //Matcher matcher = pattern.matcher(info);
        
        if(!info.isEmpty()){

            name = info;
        }
        else {return null;}
        
        try{
            startDate = all.parse(from); 
        } catch (ParseException e){
            try{
                startDate = justDate.parse(from);
            } catch (ParseException e2){
                Logger.getLogger(InsertEvent.class.getName()).log(Level.SEVERE, null, e2);
                return null;
            }    
        }
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        long hours = cal.get(Calendar.HOUR_OF_DAY);
        long minutes = cal.get(Calendar.MINUTE);
        
        try{
                endDate = all.parse(to);
            } catch (ParseException e2){
                cal.add(Calendar.HOUR_OF_DAY, 1);
                endDate = cal.getTime();
            } 
        
         long durationInMillis = endDate.getTime() - startDate.getTime();
         long resultMinutes =  durationInMillis/60000;  
            
            
         durationHours = resultMinutes / 60;
         durationMinutes = resultMinutes - (durationHours*60);

        final OBCriteria<Client> clientList = OBDal.getInstance().createCriteria(Client.class);
        clientList.add(Restrictions.eq(Client.PROPERTY_ID, clientId));
        
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));
        
        if (userList.list().get(0).isOpcrmIsCrmUser() == null || !userList.list().get(0).isOpcrmIsCrmUser()) return null;
        
        final OBCriteria<Organization> orgList = OBDal.getInstance().createCriteria(Organization.class);
        orgList.add(Restrictions.eq(Organization.PROPERTY_ID, orgId));
        
        Opcrmactivity nuovo = OBProvider.getInstance().get(Opcrmactivity.class);
        nuovo.setActive(true);
        nuovo.setDescription(descr);
        nuovo.setCreationDate(new Date());
        nuovo.setUpdated(new Date());
        nuovo.setUpdatedBy(userList.list().get(0));
        nuovo.setAssignedTo(userList.list().get(0));
        nuovo.setCreatedBy(userList.list().get(0));
        nuovo.setClient(clientList.list().get(0));
        nuovo.setOrganization(orgList.list().get(0));
        nuovo.setActivityStartdate(startDate);
        
        nuovo.setDurationHours(durationHours);
        
        nuovo.setDurationMinutes(durationMinutes);
        
        nuovo.setActivitySubject(name);
        nuovo.setStartMinute(minutes);
        nuovo.setStartHour(hours);
        
        String searchkey="";
        OBCriteria <Opcrmstatusfilter> actFilter;
        if(idActivity-1 >= 0){
            searchkey = GetActivitySearchKey(idActivity-1);
            
            nuovo.setActivityType(searchkey);
            
            //if i want to set the activity to its closing state
            if(eventDone!=null && eventDone.matches("Y")){
                actFilter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
                    actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ISCLOSINGSTATE, true));
                    actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY, searchkey));
                    
                    if(!actFilter.list().isEmpty())
                        nuovo.setActivityStatus(actFilter.list().get(0));
            }
            //else i'll check the activity status id
            else{
                if(activityStatusId != null && !activityStatusId.isEmpty())
                {
                    actFilter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
                    actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ID, activityStatusId));
                    actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY, searchkey));
                    nuovo.setActivityStatus(actFilter.list().get(0));
                }
                else{
                    final OBCriteria <Opcrmstatusfilter> actDefaultStateFilter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
                    actDefaultStateFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ISDEFAULTSTATE, true));
                    actDefaultStateFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY, searchkey));
                
                    if(!actDefaultStateFilter.list().isEmpty())
                        nuovo.setActivityStatus(actDefaultStateFilter.list().get(0));
                }
            
            }
            
        }
        
        if(leadId!=null && !leadId.isEmpty() && !leadId.matches("0")){
            final OBCriteria<User> leadList = OBDal.getInstance().createCriteria(User.class);
            leadList.add(Restrictions.eq(User.PROPERTY_ID, leadId));
            
            //final OBCriteria<opcrmLeadselector> leadList = OBDal.getInstance().createCriteria(opcrmLeadselector.class);
            //leadList.add(Restrictions.eq(User.PROPERTY_ID, leadId));
            
            nuovo.setRelatedLead(leadList.list().get(0));
            //nuovo.setBusinessPartner(leadList.list().get(0));
        }
        
        OBDal.getInstance().save(nuovo);
        OBDal.getInstance().flush();
        res="ok";
        return res;
    }

    public String modifyEvent (String from, String to, String info, String descr, String evtId, long idActivity, long idStatCode, String activityStatusId, String eventDone){
        String res="";
        String startHH="";
        String name="";
        Date startDate;
        Date endDate;
        long durationHours=0;
        long durationMinutes=0;
        SimpleDateFormat all =  new SimpleDateFormat("dd/MM/yyyy HH:mm");
        SimpleDateFormat justDate =  new SimpleDateFormat("dd/MM/yyyy");
        //Pattern pattern = Pattern.compile("(([01]?[0-9]|2[0-3]):[0-5][0-9])");
        //Matcher matcher = pattern.matcher(info);
        
        if(!info.isEmpty()){

            name = info;
        }
        else {return null;}
        
        try{
            startDate = all.parse(from); 
        } catch (ParseException e){
            Logger.getLogger(InsertEvent.class.getName()).log(Level.SEVERE, null, e);
            return null; 
        }
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        long hours = cal.get(Calendar.HOUR_OF_DAY);
        long minutes = cal.get(Calendar.MINUTE);
        
        try{
                endDate = all.parse(to);
            } catch (ParseException e2){
                cal.add(Calendar.HOUR_OF_DAY, 1 );
                endDate = cal.getTime(); 
                 
            } 
        
        
        if (startDate != null & endDate != null){
            long durationInMillis = endDate.getTime() - startDate.getTime();
            long resultMinutes =  durationInMillis/60000;  
            
            
            durationHours = resultMinutes / 60;
            durationMinutes = resultMinutes - (durationHours*60);

        } 
        
        final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ID, evtId));
        Opcrmactivity activity = activityRecords.list().get(0);
        
        if (activity == null) return null;
        
        activity.setActivityStartdate(startDate);
        
        if(durationHours != 0)
            activity.setDurationHours(durationHours);
        
        
       if(durationMinutes != 0)
           activity.setDurationMinutes(durationMinutes);
        
        activity.setActivitySubject(name);
        activity.setStartMinute(minutes);
        activity.setStartHour(hours);
        activity.setDescription(descr);
        String searchkey="";
        OBCriteria <Opcrmstatusfilter> actFilter;
        if(idActivity-1 >= 0){
            searchkey=GetActivitySearchKey(idActivity-1);
            activity.setActivityType(searchkey);
            
            //if i want to set the activity to its closing state
            if(eventDone!=null && eventDone.matches("Y")){
                actFilter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
                    actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ISCLOSINGSTATE, true));
                    actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY, searchkey));
                    
                    if(!actFilter.list().isEmpty())
                        activity.setActivityStatus(actFilter.list().get(0));
            }
            //else i'll check the activity status id
            else{
                if(activityStatusId != null && !activityStatusId.isEmpty())
                {
                    actFilter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
                    actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ID, activityStatusId));
                    actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY, searchkey));
                    activity.setActivityStatus(actFilter.list().get(0));
                }
            }
        }
        else
            activity.setActivityType(null);
        
        
        if(idStatCode-1 >=0)
            activity.setLeadstatus(GetStatusSearchKey(idStatCode-1));
        else     
            activity.setLeadstatus(null);
        
        OBDal.getInstance().save(activity);
        OBDal.getInstance().flush();
        
        res="ok";
        
        return res;
    }
    
    public String planNextEvent(String from, String to, String info, String descr, String evtId, long idActivity, String userId, String clientId, String orgId, long idStatusCode, String activityStatusId){
        String res="";
        String startHH="";
        String name="";
        Date startDate;
        Date endDate;
        long durationHours=0;
        long durationMinutes=0;
        SimpleDateFormat all =  new SimpleDateFormat("dd/MM/yyyy HH:mm");
        SimpleDateFormat justDate =  new SimpleDateFormat("dd/MM/yyyy");
        //Pattern pattern = Pattern.compile("(([01]?[0-9]|2[0-3]):[0-5][0-9])");
        //Matcher matcher = pattern.matcher(info);
        
        if(!info.isEmpty()){

            name = info;
        }
        else {return null;}
        
        try{
            startDate = all.parse(from); 
        } catch (ParseException e){
            Logger.getLogger(InsertEvent.class.getName()).log(Level.SEVERE, null, e);
            return null; 
        }
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(startDate);
        long hours = cal.get(Calendar.HOUR_OF_DAY);
        long minutes = cal.get(Calendar.MINUTE);
        
        try{
                endDate = all.parse(to);
            } catch (ParseException e2){
                cal.add(Calendar.HOUR_OF_DAY, 1 );
                endDate = cal.getTime(); 
                 
            } 
        
        
        if (startDate != null & endDate != null){
            long durationInMillis = endDate.getTime() - startDate.getTime();
            long resultMinutes =  durationInMillis/60000;  
            
            
            durationHours = resultMinutes / 60;
            durationMinutes = resultMinutes - (durationHours*60);

        } 
        
        //gets the previous activity to et the related business partner lead
        final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ID, evtId));
        Opcrmactivity activity = activityRecords.list().get(0);
        
        if (activity == null) return null;
        
        final OBCriteria<Client> clientList = OBDal.getInstance().createCriteria(Client.class);
        clientList.add(Restrictions.eq(Client.PROPERTY_ID, clientId));
        
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));
        
        if (userList.list().get(0).isOpcrmIsCrmUser() == null || !userList.list().get(0).isOpcrmIsCrmUser()) return null;
        
        final OBCriteria<Organization> orgList = OBDal.getInstance().createCriteria(Organization.class);
        orgList.add(Restrictions.eq(Organization.PROPERTY_ID, orgId));
        
        Opcrmactivity nuovo = OBProvider.getInstance().get(Opcrmactivity.class);
        nuovo.setActive(true);
        nuovo.setDescription(descr);
        nuovo.setCreationDate(new Date());
        nuovo.setUpdated(new Date());
        nuovo.setUpdatedBy(userList.list().get(0));
        nuovo.setAssignedTo(userList.list().get(0));
        nuovo.setCreatedBy(userList.list().get(0));
        nuovo.setClient(clientList.list().get(0));
        nuovo.setOrganization(orgList.list().get(0));
        nuovo.setActivityStartdate(startDate);
        
        nuovo.setDurationHours(durationHours);
        
        nuovo.setDurationMinutes(durationMinutes);
        
        nuovo.setActivitySubject(name);
        nuovo.setStartMinute(minutes);
        nuovo.setStartHour(hours);
        
        //sets same partner and lead of previous activity
        if(activity.getBusinessPartner() != null)
            nuovo.setBusinessPartner(activity.getBusinessPartner());
        if(activity.getRelatedLead() != null)
            nuovo.setRelatedLead(activity.getRelatedLead());
        
        if(idActivity-1 >= 0){
            nuovo.setActivityType(GetActivitySearchKey(idActivity-1));
            if(activityStatusId != null && !activityStatusId.isEmpty())
            {
                final OBCriteria <Opcrmstatusfilter> actFilter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
                actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ID, activityStatusId));
                nuovo.setActivityStatus(actFilter.list().get(0));
            }
        }
        if(idStatusCode-1 >=0)
            nuovo.setLeadstatus(GetStatusSearchKey(idStatusCode-1));
        else     
            nuovo.setLeadstatus(null);
        
        OBDal.getInstance().save(nuovo);
        OBDal.getInstance().flush();
        res="ok";
        return res;
    }
    
    
    public String GetActivitySearchKey(long seqnumber){
        String res="";
        
            OBContext.setAdminMode(true);
            final OBCriteria <Reference> actRef = OBDal.getInstance().createCriteria(Reference.class);
            actRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "opcrmActivtyType"));

            final OBCriteria <org.openbravo.model.ad.domain.List> actRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
            actRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,actRef.list().get(0)));
            actRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEQUENCENUMBER, seqnumber));
            res = actRefList.list().get(0).getSearchKey();
            OBContext.restorePreviousMode();
        
        return res;
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
    
    
}
