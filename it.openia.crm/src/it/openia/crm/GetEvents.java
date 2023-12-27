/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.ListTrl;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.domain.ReferenceTrl;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.ad.ui.Message;
import org.openbravo.model.ad.ui.MessageTrl;
import org.openbravo.model.common.businesspartner.Category;
import org.quartz.JobExecutionException;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.ad.access.User;
import it.openia.crm.*;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.base.exception.OBException;

/**
 *
 * @author nicholas
 */
public class GetEvents extends HttpBaseServlet {
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        
        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }
       
        response.setContentType("application/json; charset=utf-8");
        VariablesSecureApp vars = new VariablesSecureApp(request);
        boolean menuButton = false;
        String allEvents="N";
        String strUserId = vars.getUser();
        if (strUserId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getUser().getId();
            menuButton = true;
        }
        
        String strClientId = vars.getClient();
        if (strClientId == null || "".equals(strClientId)) { // in case of mobile ...
            strClientId = OBContext.getOBContext().getCurrentClient().getId();
            //menuButton = true;
        }
        
        String lang = vars.getLanguage();
        if (lang.isEmpty()) {
            lang = OBContext.getOBContext().getLanguage().getLanguage();
        }
        
        PrintWriter out = response.getWriter();

        if(request.getParameter("all")!=null)
            allEvents = request.getParameter("all");
        
        try {
            out.println(GetEventsJSon(request, strUserId, strClientId, GetTimestamp(request.getParameter("from")), GetTimestamp(request.getParameter("to")), menuButton, lang,allEvents));
        } catch (JobExecutionException ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }
    
    }
    
    public String GetEventsJSon(HttpServletRequest request, String userId, String clientId, Timestamp from, Timestamp to, boolean menuButton, String lang, String all) 
    throws JobExecutionException, ServletException{
        String res="";
        ArrayList events;
        try{
            events = GetEventsArray(request, userId, clientId, from, to, all);
        } catch (Exception e){ throw new JobExecutionException(e.getMessage(), e); }
        
        res = ToJson(userId, events,menuButton, lang);
        
        return res;
    }
    
    
    public ArrayList<Event> GetEventsArray(HttpServletRequest request, String userId, String clientId, Timestamp from, Timestamp to, String all) 
    throws JobExecutionException, ServletException{

        ArrayList <Event> res = new ArrayList();
        HashMap <String,Event> keyEvts = new HashMap <String, Event>();
        int month;
        User activityUser = null;
        
        java.util.Date date= new java.util.Date();
        Calendar c = Calendar.getInstance();  
        c.setTime(date);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR, 0);
        c.set(Calendar.MINUTE, 0);
        
        Calendar c2 = Calendar.getInstance();
        c2.setTime(c.getTime());
        
        c.add(Calendar.DATE,-7); //starts from the week before the start of the month
        Timestamp monthBegin = new Timestamp(c.getTimeInMillis());
        
        c2.add(Calendar.DATE,+38); //gets end of month plus next week
        Timestamp monthEnd = new Timestamp(c2.getTimeInMillis());
        
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));
        
        final OBCriteria<Client> clList = OBDal.getInstance().createCriteria(Client.class);
        clList.add(Restrictions.eq(User.PROPERTY_ID, clientId));
        
        Disjunction orActivityStart; //insieme eventi che hanno startdate tra 'from' e 'to'
        Disjunction orActivityDuration; //insieme di eventi attivi tra 'from' e 'to'
        Disjunction orActivityHideInCalNull;
        Disjunction orActivityHideInCalFalse;
        
        if (!userList.list().isEmpty()) 
            activityUser = userList.list().get(0);
        
        try {
             List<Opcrmactivity> actRecs =null;
             
             // nuova query di ricerca hql in sostituzione a OBCriteria
             if(from !=null && to!=null){
                 Days d = Days.daysBetween(new DateTime(from.getTime()), new DateTime( to.getTime()));
                 int days = d.getDays();
                 
                 if (days>7){
                        Calendar f = Calendar.getInstance();
                        f.setTime(from);
                        Calendar t = Calendar.getInstance();
                        t.setTime(to);
                        
                        f.add(Calendar.DATE,-7); //starts from the week before the start of the month
                        Timestamp fminusweek = new Timestamp(f.getTimeInMillis());
        
                        t.add(Calendar.DATE,+38); //gets end of month plus next week
                        Timestamp tplusweek = new Timestamp(t.getTimeInMillis());
                        
                        actRecs = GetEventsArrayNew(request,userList.list().get(0), clList.list().get(0),fminusweek,tplusweek,all);
                 }
                 else
                    actRecs = GetEventsArrayNew(request, userList.list().get(0), clList.list().get(0),from,to,all);
             }
             else
                 actRecs = GetEventsArrayNew(request,userList.list().get(0), clList.list().get(0),monthBegin,monthEnd,all);
             
             
              // --------- Main User Activities
             
              /*
              final OBCriteria<Opcrmactivity> activityRecords = OBDal.getInstance().createCriteria(Opcrmactivity.class);
              activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ACTIVE, true));
              
              orActivityHideInCalNull = Restrictions.disjunction();
              orActivityHideInCalNull.add(Restrictions.isNull(Opcrmactivity.PROPERTY_HIDEINCALENDAR));
              
              orActivityHideInCalFalse = Restrictions.disjunction();
              orActivityHideInCalFalse.add(Restrictions.eq(Opcrmactivity.PROPERTY_HIDEINCALENDAR, false));
              
              activityRecords.add(Restrictions.or(orActivityHideInCalNull, orActivityHideInCalFalse));
              
              if(from !=null & to !=null){
                  
                orActivityStart = Restrictions.disjunction();  
                orActivityStart.add(Restrictions.gt(Opcrmactivity.PROPERTY_ACTIVITYSTARTDATE, from));
                orActivityStart.add(Restrictions.lt(Opcrmactivity.PROPERTY_ACTIVITYSTARTDATE, to));
                
                orActivityDuration = Restrictions.disjunction();  
                orActivityDuration.add(Restrictions.lt(Opcrmactivity.PROPERTY_ACTIVITYSTARTDATE, from));
                orActivityDuration.add(Restrictions.gt(Opcrmactivity.PROPERTY_ACTIVITYDUEDATE, from));
                
                activityRecords.add(Restrictions.or(orActivityStart, orActivityDuration));
                
              }
              else {
                  
                orActivityStart = Restrictions.disjunction();  
                orActivityStart.add(Restrictions.gt(Opcrmactivity.PROPERTY_ACTIVITYSTARTDATE, monthBegin));
                orActivityStart.add(Restrictions.lt(Opcrmactivity.PROPERTY_ACTIVITYSTARTDATE, monthEnd));
                
                orActivityDuration = Restrictions.disjunction();  
                orActivityDuration.add(Restrictions.lt(Opcrmactivity.PROPERTY_ACTIVITYSTARTDATE, monthBegin));
                orActivityDuration.add(Restrictions.gt(Opcrmactivity.PROPERTY_ACTIVITYDUEDATE, monthBegin));
                
                activityRecords.add(Restrictions.or(orActivityStart, orActivityDuration));
                
              }        
              
              // ---------- Activities Visibility
              ArrayList <String> activitiesID = new ArrayList();
              final OBCriteria<opcrmLeadActivity> leadActivityRecords = OBDal.getInstance().createCriteria(opcrmLeadActivity.class);
              
              final OBCriteria <OpcrmSuperact> supUsers = OBDal.getInstance().createCriteria(OpcrmSuperact.class);
              supUsers.add(Restrictions.eq(OpcrmSuperact.PROPERTY_USERCONTACT,activityUser));
              
              // ---------- If the user isn't a super user filter by regular "assigned to" "CRM Enabled User" criteria
              if(supUsers.list().isEmpty() && !activityUser.getId().matches("100"))
              {
                if (activityUser != null)
                        leadActivityRecords.add(Restrictions.eq(opcrmLeadActivity.PROPERTY_USERCONTACT, activityUser));
              
                if (!leadActivityRecords.list().isEmpty()){
                        for (opcrmLeadActivity g : leadActivityRecords.list())
                            activitiesID.add(g.getOpcrmActivity().getId());
                        } 
                        
                if (activityUser != null & !activitiesID.isEmpty()){
                        Disjunction orActivitiesID = Restrictions.disjunction();
                        orActivitiesID.add(Restrictions.in(Opcrmactivity.PROPERTY_ID, activitiesID));
                        activityRecords.add(Restrictions.or(Restrictions.eq(Opcrmactivity.PROPERTY_ASSIGNEDTO, activityUser),orActivitiesID));
                        }
                else activityRecords.add(Restrictions.eq(Opcrmactivity.PROPERTY_ASSIGNEDTO, activityUser));
              }
              
              if(all.matches("N"))
              {
                  //closed activities have to be older than a week to be cancelled from the calendar view
                  Date now = new Date();
                  Calendar calend = Calendar.getInstance();
                  calend.setTime(now);
                  calend.add(Calendar.DAY_OF_MONTH, -7);
                  
                  OBCriteria<Opcrmstatusfilter> filterList = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
                  filterList.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ISCLOSINGSTATE,true));
                  
                  Disjunction orNotClosedActivity = Restrictions.disjunction();
                  if(filterList.list()!=null && !filterList.list().isEmpty())
                    orNotClosedActivity.add(Restrictions.not(Restrictions.in(Opcrmactivity.PROPERTY_ACTIVITYSTATUS, filterList.list())));
                  
                  Disjunction orWithoutStatus = Restrictions.disjunction();
                  orWithoutStatus.add(Restrictions.isNull(Opcrmactivity.PROPERTY_ACTIVITYSTATUS));
                  
                  Disjunction orClosedInWeek = Restrictions.disjunction();
                  //orClosedInWeek.add(Restrictions.in(Opcrmactivity.PROPERTY_ACTIVITYSTATUS, filterList.list()));
                  orClosedInWeek.add(Restrictions.between(Opcrmactivity.PROPERTY_ACTIVITYDUEDATE,calend.getTime(),new Date()));
                  
                  activityRecords.add(Restrictions.or(Restrictions.or(orWithoutStatus,orNotClosedActivity), orClosedInWeek));
              }
              
              */
              
                Calendar cal;
                cal = Calendar.getInstance(); 
                boolean allday;
                String desc="";
                String url="";
                String newLine = System.getProperty("line.separator");
                String evtType ="";
                String actStatusId="";
                String partnerName=""; 
                String leadName="";
                String evtTitle="";
                String leadCommercialName="";
                String subject="";
                String imgCode="";
                User usr=null;
                
                if(actRecs!=null && !actRecs.isEmpty())    
                    for(Opcrmactivity activity : actRecs){
                            url="";
                            desc="";
                            leadName ="";
                            leadCommercialName="";
                            evtTitle="";
                            subject="";
                            imgCode="";
                            
                            OBCriteria<User> usrList = OBDal.getInstance().createCriteria(User.class);
                            
                            if(activity.getRelatedLead()!=null){
                                usrList.add(Restrictions.eq(User.PROPERTY_ID,activity.getRelatedLead().getId()));
                                usr=usrList.list().get(0);
                            }        
                            else usr=null;        
                            
                            cal.setTime(activity.getActivityStartdate()); 
                            cal.add(Calendar.HOUR_OF_DAY, activity.getDurationHours().intValue()); 
                            cal.add(Calendar.MINUTE, activity.getDurationMinutes().intValue());
                            SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            
                            allday = activity.isAllday();
                            if (activity.getDescription()!=null)desc=activity.getDescription();   
                            if (activity.getActivityUrl() != null) url= activity.getActivityUrl();
                            if (activity.getActivityStatus()!=null && 
                                activity.getActivityStatus().isClosingstate()!=null && 
                                activity.getActivityStatus().isClosingstate()==true)
                                evtType = "Closed";
                            else if (activity.getActivityType()!=null && activity.getActivityType().equalsIgnoreCase("OPPORTUNITY")) evtType = "Opportunity";
                            else evtType ="Activity";
                            
                            if(activity.getBusinessPartner() != null)
                                partnerName = activity.getBusinessPartner().getName();
                            else partnerName = "";
                            
                            if(usr != null){
                                
                                //leadName += "<a href=\"../?tabId=84A996CBB6434F07949CD86F62F05E71&recordId="+activity.getRelatedLead().getId()+"\" target=\"_blank\">";
                                
                                leadName +="<a href=\"#\" onclick=\"openWindowEntry('84A996CBB6434F07949CD86F62F05E71', '"+activity.getRelatedLead().getId()+"');return false;\">";
                                
                                if(usr.getOpcrmCommercialname() != null){
                                    leadCommercialName= usr.getOpcrmCommercialname();
                                    leadName += leadCommercialName+" - ";
                                }
                                if(usr.getFirstName() != null)
                                    leadName += usr.getFirstName();
                                if(usr.getLastName() != null)
                                    leadName +=" "+usr.getLastName();
                                
                                leadName += "</a>";
                                
                                if(usr.getEmail() != null){
                                    leadName += "<br><a href=\"mailto:"+usr.getEmail()+"\">"+usr.getEmail()+"</a> &nbsp;&nbsp;";
                                    
                                    leadName+= "<a href=\"https://mail.google.com/mail/?view=cm&fs=1&to="+usr.getEmail()+"&su="+activity.getActivitySubject()+"&body=BODY&tf=1\" target=\"_new\">"+ 
                                            "<img src=\"https://mail.google.com/favicon.ico\" alt=\"some_text\"/>"+
                                               "</a>";
                                }    
                                if(usr.getPhone() != null)
                                    leadName += "<br><a href=\"tel:"+usr.getPhone()+"\">Tel."+usr.getPhone()+"</a>";
                                if(usr.getOpcrmMobile() != null)
                                    leadName += "<br><a href=\"tel:"+usr.getOpcrmMobile()+"\">Cel."+usr.getOpcrmMobile()+"</a>";
                            
                            }
                            else leadName ="";
                            
                            if(activity.getActivityStatus() != null)
                                actStatusId = activity.getActivityStatus().getId(); 
                            else actStatusId = "";
                            
                            if(!leadCommercialName.isEmpty())
                                evtTitle=activity.getActivitySubject()+"-"+leadCommercialName;
                            else
                                evtTitle=activity.getActivitySubject();
                            
                            if(activity.getAssignedTo()!=null &&
                               activity.getAssignedTo().getOpcrmUsrimage()!=null &&
                               !activity.getAssignedTo().getOpcrmUsrimage().isEmpty()){
                                
                                    imgCode="<a class=\"userPopUpInfo\" href=\"#\" title=\""+activity.getAssignedTo().getName() + "\">"+
                                            "<img src=\"../web/it.openia.crm/images/usericons/"+activity.getAssignedTo().getOpcrmUsrimage()
                                            +".png\" alt=\"user\">"+
                                       "</a>&nbsp;";
                            
                            }
                            
                            subject=activity.getActivitySubject();
                            
                            res.add(new Event(activity.getId(), 
                                              evtTitle,
                                              dt1.format(activity.getActivityStartdate()).replace(" ", "T").concat("Z"),
                                              dt1.format(cal.getTime()).replace(" ", "T").concat("Z"),
                                              allday,
                                              url,
                                              getActivityName(activity.getActivityType()),
                                              desc.replace(newLine, "<br/>"),
                                              evtType,
                                              partnerName,
                                              leadName,
                                              getStatusName(activity.getLeadstatus()),
                                              actStatusId,
                                              subject,
                                              imgCode
                                             ));
                       
                    }
                           
              
        }catch(Exception e) {
            throw new JobExecutionException(e.getMessage(), e);
        }
        
        return res;
    } 
    
    public List <Opcrmactivity> GetEventsArrayNew(HttpServletRequest request, User usr, Client cl, Date from, Date to, String all) throws JobExecutionException, ServletException{
        
        String isFilter = (request.getParameter("isFilter") == null) ? "N" : request.getParameter("isFilter");
        String partnerId = "";
        String activityStatusId = "";
        String activitytypeId = "";
        String relatedleadId = "";
        String assignedToId = "";
        InsertEvent insertEvent = new InsertEvent();


        final OBCriteria <OpcrmSuperact> supUsers = OBDal.getInstance().createCriteria(OpcrmSuperact.class);
        supUsers.add(Restrictions.eq(OpcrmSuperact.PROPERTY_USERCONTACT,usr));

        String hqlQuery="";
        String sqlQuery="";
        
        Date now = new Date();
        Calendar calendWeekAgo = Calendar.getInstance();
        calendWeekAgo.setTime(now);
        calendWeekAgo.add(Calendar.DAY_OF_MONTH, -7);
        
        Calendar calendNextWeek = Calendar.getInstance();
        calendNextWeek.setTime(now);
        calendNextWeek.add(Calendar.DAY_OF_MONTH, 7);

    if (isFilter.equals("N")) {
        
        if(supUsers.list().isEmpty() && !usr.getId().matches("100")){
            
            if(all.equals("Y")){
                
                //Query che estrae attivita' per giorno in relazione allo user corrente comprese le attivita' chiuse
                hqlQuery = " select act from opcrm_activity act " + 
                            " where  (act.hideincalendar is null or  act.hideincalendar = false) and "+
                            " ( act.createdBy = :usrid or" + 
                            " act.assignedTo.id = :usrid or"+
                            " act.id in (select acc.opcrmActivity.id from opcrm_lead_activity acc" + 
                            " where acc.userContact.id = :usrid) )"+ 
                            " and "+
                            "( ( act.activityStartdate >= :start and act.activityStartdate <= :end ) or ( act.activityStartdate < :start and act.activityDueDate >= :start ) )" +
                            " order by act.activityStartdate asc";}
            else {
                //Query che estrae attivita' per giorno in relazione allo user corrente eccetto quelle chiuse ammesso che non siano recenti
                hqlQuery = "select act " + 
                           "from opcrm_activity as act " +
                           "left join act.activityStatus as status " + 
	                       "where  (act.hideincalendar is null or  act.hideincalendar = false) and " + 
		                   "( "+
		                   "( status.isclosingstate = true and ( act.activityDueDate > :lastweek and " + 
                           "act.activityDueDate < :nextweek ) ) or " +
                           " ( act.activityType is null)  or " +
                           " ( act.activityType is not null and (status is null or status.isclosingstate is null or (status.isclosingstate is not null and status.isclosingstate = false))) " +  
                           ") and " +
		                   "( " + 
                           "( act.activityStartdate >= :start and act.activityStartdate <= :end ) or " + 
                           "( act.activityStartdate < :start and act.activityDueDate >= :start ) " + 
                           ") and " +
                           " ( act.createdBy = :usrid or " + 
                           " act.assignedTo.id = :usrid or "+
                           " act.id in (select acc.opcrmActivity.id from opcrm_lead_activity acc " + 
                           " where acc.userContact.id = :usrid) )" +
                           "order by act.activityStartdate asc ";}
        }    
        else{
            if(all.equals("Y")){
                // prende tutte le attivita' del client all'interno di start e end comprese quelle chiuse
                hqlQuery = " select act from opcrm_activity act " + 
                            " where  (act.hideincalendar is null or  act.hideincalendar = false) and "+
                            " ( ( act.activityStartdate >= :start and act.activityStartdate <= :end ) or ( act.activityStartdate < :start and act.activityDueDate >= :start ) ) and" +
                            " (act.client.id = :clientid) " +  
                            " order by act.activityStartdate asc";}
            else {
                // prende tutte le attivita' del client all'interno di start e end con quelle chiuse piu' di recente
                hqlQuery = " select act from opcrm_activity as act " + 
                    "left join act.activityStatus as status " + 
	                "where  (act.hideincalendar is null or  act.hideincalendar = false) and " + 
		            "( "+
		            "( status.isclosingstate = true and ( act.activityDueDate > :lastweek and " + 
                    "act.activityDueDate < :nextweek ) ) or " +
                    " ( act.activityType is null)  or " +
                    " ( act.activityType is not null and (status is null or status.isclosingstate is null or (status.isclosingstate is not null and status.isclosingstate = false))) " +  
                    ") and " +
		            "( " + 
                    "( act.activityStartdate >= :start and act.activityStartdate <= :end ) or " + 
                    "( act.activityStartdate < :start and act.activityDueDate >= :start ) " + 
                    ") and " +
                    " (act.client.id = :clientid) " +   
                    " order by act.activityStartdate asc";}
        }

        
        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        List <Opcrmactivity> actList;
        if(supUsers.list().isEmpty() && !usr.getId().matches("100"))
            query.setString("usrid", usr.getId());
        else
            query.setString("clientid", cl.getId());
        
        
        if(!all.equals("Y")){
            query.setDate("lastweek", calendWeekAgo.getTime());
            query.setDate("nextweek", calendNextWeek.getTime());
        }
                
        
        query.setDate("start", from);
        query.setDate("end",to);
        actList = query.list();
        
        
        return actList;} else { 
        partnerId = ( request.getParameter("partnerId")== null || request.getParameter("partnerId").equals("") ) ? "NULL" : request.getParameter("partnerId");
        activityStatusId = (request.getParameter("activityStatus")== null || request.getParameter("activityStatus").equals("")) ? "NULL" : request.getParameter("activityStatus");
        activitytypeId = (Long.valueOf(request.getParameter("idtype"))== Long.valueOf(0) || request.getParameter("idtype")== null || request.getParameter("idtype").equals("")) ? "NULL" : insertEvent.GetActivitySearchKey(Long.valueOf(request.getParameter("idtype"))-1);
        relatedleadId = (Long.valueOf(request.getParameter("leadId"))== Long.valueOf(0) || request.getParameter("leadId")== null || request.getParameter("leadId").equals("") )? "NULL" : insertEvent.GetStatusSearchKey(Long.valueOf(request.getParameter("leadId"))-1);
        assignedToId = (request.getParameter("assignedtoId")== null || request.getParameter("assignedtoId").equals("")) ? "NULL" : request.getParameter("assignedtoId");

        List <Opcrmactivity> actList=null;
        BusinessPartner partner = null;
        Opcrmstatusfilter statusActivity=null;
        User assignedTo=null;
        final OBCriteria<Opcrmactivity> activities = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        
        if(supUsers.list().isEmpty() && !usr.getId().matches("100")){
        activities.add(Restrictions.eq(Opcrmactivity.PROPERTY_ASSIGNEDTO, usr));
        }else{}
        // Filtro Tercero
        if(!partnerId.equals("NULL")){
        partner = OBDal.getInstance().get(BusinessPartner.class, partnerId);
        activities.add(Restrictions.eq(Opcrmactivity.PROPERTY_BUSINESSPARTNER, partner));}
        // Filtro estado de la actividad
        else if(!activityStatusId.equals("NULL")){
        statusActivity = OBDal.getInstance().get(Opcrmstatusfilter.class, activityStatusId);
        activities.add(Restrictions.eq(Opcrmactivity.PROPERTY_ACTIVITYSTATUS, statusActivity));}        
        // Filtro typo de actividad
        else if(!activitytypeId.equals("NULL")){
        activities.add(Restrictions.eq(Opcrmactivity.PROPERTY_ACTIVITYTYPE, activitytypeId));}        
        // Filtro lead status
        else if(!relatedleadId.equals("NULL")){
        activities.add(Restrictions.eq(Opcrmactivity.PROPERTY_LEADSTATUS, relatedleadId));}
        // Filtro asigano a
        else if(!assignedToId.equals("NULL")){
        assignedTo = OBDal.getInstance().get(User.class, assignedToId);
        activities.add(Restrictions.eq(Opcrmactivity.PROPERTY_ASSIGNEDTO, assignedTo));}
        else{}
        actList = activities.list();
            
        return actList;
        }
    }
    
    public String ToJson(String usrId, ArrayList <Event> events,boolean menuButton, String lang){
        
        EventList evtList = new EventList();
        evtList.setEventList(events);
        evtList.setActivityType(GetReferences(lang));
        evtList.setLeadStatuses(GetStatuses(lang));
        evtList.setShowMenu(menuButton);
        evtList.setPartnerList(GetPartner());
        evtList.setAssignedtoList(GetAssignedto());
        //evtList.setLeadsList(GetLeads(usrId));
        
        return JSONSerializer.getJSON(evtList);
    }
    
    public Timestamp GetTimestamp(String time){
        Timestamp t = null;
        
        if(time!=null){
            SimpleDateFormat dt = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.US);
            time=time.split("GMT")[0];
            try {
                t = new Timestamp( dt.parse(time).getTime() );
                
            } catch (ParseException ex) {
                Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return t;
    }

    public ArrayList<BPartner> GetPartner() {

        ArrayList <BPartner> partnerSelect = new ArrayList<BPartner>(); 

            try {
            ConnectionProvider connection = new DalConnectionProvider(false);
            PreparedStatement statement = null;
                try {
                    String consulta = "SELECT op.c_bpartner_id as id , k.name  as name FROM opcrm_activity op "+
                                      "LEFT JOIN c_bpartner k ON op.c_bpartner_id = k.c_bpartner_id "+
                                      "GROUP BY op.c_bpartner_id,k.name";
                    statement = connection.getPreparedStatement(consulta);
                    ResultSet rs = statement.executeQuery();;
                    while ( rs.next() )
                    {
                    partnerSelect.add(new BPartner(rs.getString("name"), null, null, rs.getString("id")));
                    }
                    rs.close();
                } finally {
                    if (statement != null && !statement.isClosed()) {
                        statement.close();
                    }
                }
            } catch (Exception e) {
                throw new OBException(e);
            }    
        return partnerSelect;
    }

    public ArrayList<UserAssinedTo> GetAssignedto(){

        OBContext.setAdminMode(true);

        ArrayList <UserAssinedTo> assignedtoSelect = new ArrayList<UserAssinedTo>(); 
        
        final OBCriteria<User> assignedtoList = OBDal.getInstance().createCriteria(User.class);
        assignedtoList.add(Restrictions.eq(User.PROPERTY_OPCRMISCRMUSER, true));
               
        for (User assignedto : assignedtoList.list()){
            assignedtoSelect.add(new UserAssinedTo(assignedto.getUsername(), assignedto.getId()));
        }        
        return assignedtoSelect;
    }
    
    public ArrayList <ActivityType> GetReferences(String lang){
        ArrayList <ActivityType> actTypes = new ArrayList<ActivityType>(); 
        ArrayList <ActivityStatus> actStatusList;
        
        int count =0;
        
        OBContext.setAdminMode(true);
        final OBCriteria <Reference> actRef = OBDal.getInstance().createCriteria(Reference.class);
        actRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "opcrmActivtyType"));
        
        final OBCriteria <org.openbravo.model.ad.domain.List> actRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
        actRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,actRef.list().get(0)));
        actRefList.addOrderBy(org.openbravo.model.ad.domain.List.PROPERTY_SEQUENCENUMBER, true);        
        
        for (org.openbravo.model.ad.domain.List l : actRefList.list()){
            
            actStatusList = GetActivityStatuses(l.getSearchKey(), lang);
            
            actTypes.add(count,new ActivityType(l.getSequenceNumber(),l.getName(),actStatusList));
            count++;
        }
        
        OBContext.restorePreviousMode();
        return actTypes;
    }
    
    public ArrayList<ActivityStatus> GetActivityStatuses(String actKey, String lang){
        
        ArrayList <ActivityStatus> actStatusList = new ArrayList<ActivityStatus>();
        
        final OBCriteria <Opcrmstatusfilter> actFilter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
        actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY, actKey));
        
        OBCriteria <OpcrmStatusfilterTrl> actFilterTrl;
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        boolean isDefault=false;        
        if(!lang.matches("en_US")){
            for (Opcrmstatusfilter f : actFilter.list()){
                isDefault=false;
                actFilterTrl = OBDal.getInstance().createCriteria(OpcrmStatusfilterTrl.class);
                actFilterTrl.add(Restrictions.eq(OpcrmStatusfilterTrl.PROPERTY_OPCRMSTATUSFILTER,f));
                actFilterTrl.add(Restrictions.eq(OpcrmStatusfilterTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
                if (f.isDefaultstate() != null)
                        isDefault=f.isDefaultstate();
                
                if(!actFilterTrl.list().isEmpty())
                    actStatusList.add(new ActivityStatus(f.getId(),actFilterTrl.list().get(0).getCommercialName(),isDefault));
                else
                    actStatusList.add(new ActivityStatus(f.getId(),f.getName(),isDefault));
            }
        }
        else{
           for (Opcrmstatusfilter f : actFilter.list()){
               isDefault=false;
               
               if (f.isDefaultstate() != null)
                        isDefault=f.isDefaultstate();
                
               actStatusList.add(new ActivityStatus(f.getId(),f.getName(),isDefault));
           
           } 
        }
            
        
        
        return actStatusList;
    }
    
    public ArrayList <LeadStatus> GetStatuses(String lang){
        ArrayList <LeadStatus> statTypes = new ArrayList<LeadStatus>(); 
        int count =0;
        
        OBContext.setAdminMode(true);
        final OBCriteria <Reference> statRef = OBDal.getInstance().createCriteria(Reference.class);
        statRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "leadStatus"));
        
        OBCriteria <ListTrl> refListTrl;
        
        final OBCriteria <org.openbravo.model.ad.domain.List> statRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
        statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,statRef.list().get(0)));
        statRefList.addOrderBy(org.openbravo.model.ad.domain.List.PROPERTY_SEQUENCENUMBER, true);        
        
        
        for (org.openbravo.model.ad.domain.List l : statRefList.list()){
            if(!lang.matches("en_US"))
            {
                refListTrl = OBDal.getInstance().createCriteria(ListTrl.class);
                refListTrl.add(Restrictions.eq(ListTrl.PROPERTY_LISTREFERENCE, l));
                
                if(!refListTrl.list().isEmpty() && refListTrl.list().get(0) != null)
                    statTypes.add(count,new LeadStatus(l.getSequenceNumber(),refListTrl.list().get(0).getName()));    
                else
                    statTypes.add(count,new LeadStatus(l.getSequenceNumber(),l.getName()));
            }
            else
                statTypes.add(count,new LeadStatus(l.getSequenceNumber(),l.getName()));
            
            count++;
        }
        
        OBContext.restorePreviousMode();
        
        return statTypes;
    }
    
    public long getActivityName(String searchKey){
        long res = 0;
        if(searchKey != null){
            OBContext.setAdminMode(true);
            final OBCriteria <Reference> actRef = OBDal.getInstance().createCriteria(Reference.class);
            actRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "opcrmActivtyType"));

            final OBCriteria <org.openbravo.model.ad.domain.List> actRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
            actRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,actRef.list().get(0)));
            actRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY, searchKey));
            res = actRefList.list().get(0).getSequenceNumber()+1;
            OBContext.restorePreviousMode();
        }
        return res;
    }
    
    public long getStatusName(String searchKey){
        long res = 0;
        
        if(searchKey != null){
            OBContext.setAdminMode(true);
            final OBCriteria <Reference> statRef = OBDal.getInstance().createCriteria(Reference.class);
            statRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "leadStatus"));
            
            final OBCriteria <org.openbravo.model.ad.domain.List> statRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,statRef.list().get(0)));
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY, searchKey));
            res = statRefList.list().get(0).getSequenceNumber()+1;
            
            OBContext.restorePreviousMode();
        }
        
        return res;
    }
    
    public static String GetLocalization(){
        String defLoc= "en_US";
        
        String Loc = OBContext.getOBContext().getLanguage().getLanguage();
        
        if (!Loc.matches("it_IT") && !Loc.matches("es_ES"))
            return defLoc.replace("_", "-");
        else 
            return Loc.replace("_", "-");
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
    
    public ArrayList <Lead> GetLeads(String userId){
        
        ArrayList leads = new ArrayList<Lead>();
        
        String hqlQuery = " select usr from ADUser usr" + 
                          " where usr.opcrmIslead = true and ( usr.oPCRMAssignedTo.id = :usrid or usr.id in (select acc.userContact.id from opcrm_lead_access acc " + 
                          " where acc.cRMUser.id = :usrid) ) order by usr.lastName asc";
        
        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        query.setString("usrid", userId);
        List <User> leadList = query.list();
        
        String id=""; String firstname=""; String lastname=""; String commercialname="";
        
        for(User u : leadList){
            if(u.getFirstName() !=null)
                firstname = u.getFirstName();
            else firstname = "";
            
            if(u.getLastName() !=null)
                lastname = u.getLastName();
            else lastname = "";
            
            if(u.getOpcrmCommercialname() !=null)
                commercialname = u.getOpcrmCommercialname();
            else commercialname = "";
            
            leads.add(new Lead(u.getId(),firstname,lastname,commercialname));
        }
        
        return leads;
    }
    
    public String getServletInfo() {
        return "CRM Module - class GetEvents - Openia S.r.l.";
    }
    
}
