/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package it.openia.crm;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.ListTrl;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;

/**
 *
 * @author nicholas
 */
public class GetWeeklyActivities extends HttpBaseServlet{
    
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

        String strUserId = vars.getUser();
        if (strUserId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getUser().getId();
        }
        
        String strClientId = vars.getClient();
        if (strClientId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getCurrentClient().getId();
        }
        
        LookupEnum enReq = LookupEnum.valueOf(request.getParameter("selectedType"));
        int deltaDays = Integer.parseInt(request.getParameter("delta"));

        PrintWriter out = response.getWriter();
        
        try {
            out.println(Dispatch(strUserId, strClientId, enReq, lang, deltaDays));
        } catch (Exception ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String Dispatch(String strUserId, String strClientId, LookupEnum lookup, String lang, int delta) {
        String res;
        
        ArrayList dispatched = new ArrayList();
        Detail valueNames = new Detail("", "", "", "", -1, "", null,"","");
        
        switch (lookup) {
            
            case ACTIVITY:
                dispatched = GetActivityArray(strUserId, strClientId, delta);
                valueNames = GetActivityNames(lang);
                break;
        }
        
        res = ToJson(dispatched, valueNames, strUserId, strClientId);
        
        return res;
    }
    
    public String ToJson(ArrayList<DailyAgenda> agendaList, Detail names, String usrId, String clientId) {
        
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        
        DailyAgendaList detList = new DailyAgendaList();
        
        detList.setAgendaDays(agendaList);
        detList.setValueNames(names);
        
        detList.setActTypes(GetReferences(lang));
        detList.setLeadStatuses(GetStatuses(lang));
        //detList.setLeadsList(GetLeads(usrId, clientId));
        
        return JSONSerializer.getJSON(detList);
        
    }
    
    public ArrayList<DailyAgenda> GetActivityArray(String userId, String clientId,int delta) {
        
        /*
          DayArray[0] : <Date, DetailArrayPerDate> 
          .
          .
          DayArray[6] : <Date, DetailArrayPerDate> 
        */
        
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));
        
        User activityUser = userList.list().get(0);
        
        
        ArrayList<Detail> res = new ArrayList<Detail>();
        
        //Per adesso estrae le attivita' da oggi a distanza una settimana, in seguito sara' parametrizzato
        Date now = new Date();
        Calendar calend = Calendar.getInstance();
        calend.setTime(now);
        
        calend.add(Calendar.DAY_OF_MONTH,delta);
        calend.set(Calendar.HOUR_OF_DAY, 0);
        calend.set(Calendar.MINUTE,0);
        now = calend.getTime();
        
        calend.set(Calendar.HOUR_OF_DAY, 24);
        calend.set(Calendar.MINUTE,59);
        Date dayDelta = calend.getTime();
        
        String fieldSubject = "Activity_Subject";
        String fieldStartDate = "Activity_Startdate";
        String fieldActType = "Activity_Type";
        
        final OBCriteria <OpcrmSuperact> supUsers = OBDal.getInstance().createCriteria(OpcrmSuperact.class);
        supUsers.add(Restrictions.eq(OpcrmSuperact.PROPERTY_USERCONTACT,activityUser));
        
        String hqlQuery="";
        
        if(supUsers.list().isEmpty() && !activityUser.getId().matches("100"))
            //Query che estrae attivita' per giorno
            hqlQuery = " select act from opcrm_activity act " + 
                          " where  (act.hideincalendar = null or  act.hideincalendar = false) and "+
                                " ( act.createdBy = :usrid or" + 
                                  " act.assignedTo.id = :usrid or"+
                                  " act.id in (select acc.opcrmActivity.id from opcrm_lead_activity acc" + 
                                                  " where acc.userContact.id = :usrid) )"+ 
                           " and "+
                                  " ( act.activityStartdate >= :start and act.activityStartdate <= :end )" +
                          " order by act.activityStartdate asc";
        else
            hqlQuery = " select act from opcrm_activity act " + 
                          " where  (act.hideincalendar = null or  act.hideincalendar = false) and "+
                                 " ( act.activityStartdate >= :start and act.activityStartdate <= :end ) and" +
                                 " (act.client.id = :clientid) " +   
                          " order by act.activityStartdate asc";
        
        
        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        List <Opcrmactivity> actList; 
        
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        SimpleDateFormat dtCompare = new SimpleDateFormat("E dd-MM-yyyy");
        //dt1.format(act.getActivityStartdate())
        
        long leadstatus = -1;
        String activitystatus="";
        ArrayList<ActivityStatus> actStatuses=null;
        ArrayList<DailyAgenda> agenda= new ArrayList<DailyAgenda>();
        String phonenumber="";
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        User usr=null;
        
        for( int i = 0 ; i < 7 ; i++ ){
            if(supUsers.list().isEmpty() && !activityUser.getId().matches("100"))
                query.setString("usrid", userId);
            else
                query.setString("clientid", clientId);
            
            query.setDate("start", now);
            query.setDate("end",dayDelta);
            actList = query.list();
            
            
            agenda.add(new DailyAgenda(dtCompare.format(now),GetDailyActivities(actList)));
                
            
            calend.setTime(now);
            calend.add(Calendar.DAY_OF_MONTH, 1);
            now=calend.getTime();
            
            calend.setTime(dayDelta);
            calend.add(Calendar.DAY_OF_MONTH, 1);
            dayDelta=calend.getTime();
        }
        
        return agenda;
        
    }
    
    public ArrayList<Detail> GetDailyActivities(List <Opcrmactivity> actList){
        
        ArrayList<Detail> res = new ArrayList<Detail>();
        
        //SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        SimpleDateFormat dt1 = new SimpleDateFormat("HH:mm");
        
        long leadstatus = -1;
        String activitystatus="";
        ArrayList<ActivityStatus> actStatuses=null;
        String phonenumber="";
        String leadname="";
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        User usr=null;
                
        for (Opcrmactivity act : actList) {
            phonenumber = "";
            OBCriteria<User> usrList = OBDal.getInstance().createCriteria(User.class);
            
            if(act.getRelatedLead() != null){
                usrList.add(Restrictions.eq(User.PROPERTY_ID,act.getRelatedLead().getId()));
                usr=usrList.list().get(0);
                if(!usr.getOpcrmComputednames().isEmpty())
                    leadname=usr.getOpcrmComputednames();
            }        
            else { usr=null; leadname=""; }
                
            
            if(act.getLeadstatus()!=null) leadstatus= getStatusName(act.getLeadstatus());
            else leadstatus=-1;
            
            if(act.getActivityStatus() != null) activitystatus=act.getActivityStatus().getId();
            else activitystatus="";        
            
            if (act.getActivityType() != null)
                actStatuses = GetActivityStatuses(act.getActivityType(), lang);
            
            if(usr != null){
                if(usr.getOpcrmMobile() != null)
                    phonenumber = usr.getOpcrmMobile();
                else if(usr.getPhone() != null)
                    phonenumber = usr.getPhone();
                else if (usr.getAlternativePhone() != null)
                    phonenumber = usr.getAlternativePhone();
            }
            
            res.add(new Detail(act.getActivitySubject(), dt1.format(act.getActivityStartdate()), act.getActivityType(), act.getId(), leadstatus, activitystatus, actStatuses, phonenumber, leadname));
        }

        return res;

    
    }
    
    
    public Detail GetActivityNames(String lang) {
    
        String fieldSubject = "Activity_Subject";
        String fieldStartDate = "Activity_Startdate";
        String fieldActType = "Activity_Type";
        String fieldleadStatus = "Leadstatus";
        String actStatus = "Activity_Status";

        return new Detail(getTrlName(fieldSubject, lang), getTrlName(fieldStartDate, lang), getTrlName(fieldActType, lang), getTrlName(fieldleadStatus,lang),getTrlName(actStatus,lang));
    
    }
    
    public String getTrlName(String columnName, String lang) {
        String res="";
        boolean found=false;
        
        OBContext.setAdminMode(true);
        
        final OBCriteria<Column> colList = OBDal.getInstance().createCriteria(Column.class);
        colList.add(Restrictions.eq(Column.PROPERTY_DBCOLUMNNAME, columnName));
        
        final OBCriteria<Field> fieldList = OBDal.getInstance().createCriteria(Field.class);
        fieldList.add(Restrictions.eq(Field.PROPERTY_COLUMN, colList.list().get(0)));
        
        final OBCriteria<FieldTrl> fieldTrlList = OBDal.getInstance().createCriteria(FieldTrl.class);
        fieldTrlList.add(Restrictions.eq(FieldTrl.PROPERTY_FIELD,fieldList.list().get(0)));
        
        
        for(FieldTrl trl : fieldTrlList.list()){
            if(trl.getLanguage().getLanguage().equals(lang)){
                res = trl.getName();
                found=true;
            }
        }        
                
        if(!found)
            res = fieldList.list().get(0).getName();
        
        OBContext.restorePreviousMode();
        
        return res;
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
                
                if(f.isDefaultstate()!=null)
                    isDefault=f.isDefaultstate();        
                    
                if(!actFilterTrl.list().isEmpty())
                    actStatusList.add(new ActivityStatus(f.getId(),actFilterTrl.list().get(0).getCommercialName(),isDefault));
                else
                    actStatusList.add(new ActivityStatus(f.getId(),f.getName(),isDefault));
            }
        }
        else{
           for (Opcrmstatusfilter f : actFilter.list()){
               if(f.isDefaultstate()!=null)
                    isDefault=f.isDefaultstate();        
                 
               actStatusList.add(new ActivityStatus(f.getId(),f.getName(),isDefault));
           }
        }
            
        
        
        return actStatusList;
    }
    
    
    public long getStatusName(String searchKey){
        long res = -1;
        
        if(searchKey != null){
            OBContext.setAdminMode(true);
            final OBCriteria <Reference> statRef = OBDal.getInstance().createCriteria(Reference.class);
            statRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "leadStatus"));
            
            final OBCriteria <org.openbravo.model.ad.domain.List> statRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,statRef.list().get(0)));
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY, searchKey));
            res = statRefList.list().get(0).getSequenceNumber(); 
            
            OBContext.restorePreviousMode();
        }
        
        return res;
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
    
    //deprecated
    public ArrayList <Lead> GetLeads(String userId, String clientId){
        
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));
        
        ArrayList leads = new ArrayList<Lead>();
        
        String hqlQuery="";
        
        final OBCriteria <OpcrmSuperusers> supUsers = OBDal.getInstance().createCriteria(OpcrmSuperusers.class);
        supUsers.add(Restrictions.eq(OpcrmSuperusers.PROPERTY_USERCONTACT,userList.list().get(0)));
        
        if(supUsers.list().isEmpty() && !userList.list().get(0).getId().matches("100"))
            hqlQuery = " select usr from ADUser usr" + 
                          " where usr.opcrmIslead = true and ( usr.oPCRMAssignedTo.id = :usrid or usr.id in (select acc.userContact.id from opcrm_lead_access acc " + 
                          " where acc.cRMUser.id = :usrid) ) order by usr.lastName asc";
        else
            hqlQuery = " select usr from ADUser usr" + 
                          " where usr.client.id = :clientid " + 
                          " order by usr.lastName asc";
        
        
        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        
        if(supUsers.list().isEmpty() && !userList.list().get(0).getId().matches("100"))
            query.setString("usrid", userId);
        else
            query.setString("clientid", clientId);
        
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
    
}
