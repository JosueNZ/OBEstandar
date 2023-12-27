/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Language;
import org.quartz.JobExecutionException;

/**
 *
 * @author nicholas
 */
public class CreateActivityGetData extends HttpBaseServlet {
    //This Class is Frozen and under development (along with ActivityFromLead.java and ActFromLead.jsp)
    
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        
        response.setContentType("application/json; charset=utf-8");
        VariablesSecureApp vars = new VariablesSecureApp(request);
        
        String leadId = request.getParameter("leadId");
        
        PrintWriter out = response.getWriter();
        
        String lang = vars.getLanguage();
        if (lang.isEmpty()) {
            lang = OBContext.getOBContext().getLanguage().getLanguage();
        }
        
        out.println(GetInfoJSon(lang,leadId));
        
    }
    
    public String GetInfoJSon(String lang, String leadId){
        //GetLeadInfo(leadId);
        
        return ToJson(lang);
    }
    
    public String ToJson(String lang){
        
        EventList activityInfos = new EventList();
        //evtList.setEventList(events);
        activityInfos.setActivityType(GetReferences(lang));
        //evtList.setLeadStatuses(GetStatuses(lang));
        //evtList.setShowMenu(menuButton);
        //evtList.setLeadsList(GetLeads(usrId));
        
        return JSONSerializer.getJSON(activityInfos);
    }
    
    public String GetLeadInfo(String leadId){
        
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, leadId));
        
        return "";
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
    
}
