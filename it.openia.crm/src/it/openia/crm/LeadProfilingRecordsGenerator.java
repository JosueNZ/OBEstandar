/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm;

import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.List;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.common.businesspartner.Category;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

/**
 *
 * @author nicholas
 */
public class LeadProfilingRecordsGenerator extends DalBaseProcess{
    
    @Override
    public void doExecute(ProcessBundle bundle) throws Exception {
        
        final OBError msg = new OBError();
        
        final OBCriteria<Organization> orgRecord = OBDal.getInstance().createCriteria(Organization.class);
        orgRecord.add(Restrictions.eq(Organization.PROPERTY_ID,bundle.getContext().getOrganization()));
        
        final OBCriteria<Client> clientRecord = OBDal.getInstance().createCriteria(Client.class);
        clientRecord.add(Restrictions.eq(Client.PROPERTY_ID,bundle.getContext().getClient()));
        
        final OBCriteria<User> userRecord = OBDal.getInstance().createCriteria(User.class);
        userRecord.add(Restrictions.eq(Client.PROPERTY_ID,bundle.getContext().getUser()));
        
        try{
            StagesGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            RejectionFactorGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            JobPositionGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            ProfileGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            EmployeesGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            IndustryGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            LeadSourcesGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            LookingForGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            TimingGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            FuncOfInterestGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            TitleGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            ImplementationStrategyGen(clientRecord.list().get(0),orgRecord.list().get(0),userRecord.list().get(0));
        
            AnnualRevenueGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            ActivityStatusesGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
            
            CaseTypeActivityGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));

            WorkTypeGen(clientRecord.list().get(0),orgRecord.list().get(0), userRecord.list().get(0));
        
            //DA VEDERE
            ConfigTableGen(clientRecord.list().get(0), orgRecord.list().get(0), userRecord.list().get(0));
            
        }catch(Exception e){
            
            Logger.getLogger(LeadProfilingRecordsGenerator.class.getName()).log(Level.SEVERE, null, e);
            
            msg.setType("Error");
            msg.setTitle("Something went wrong while creating profiling records, please see log file for info");
            msg.setMessage("WARNING");

            bundle.setResult(msg);
            return;
        }
        
        msg.setType("Success");
        msg.setTitle("Lead Profiling info has been set");
        msg.setMessage("OK");

        bundle.setResult(msg);
    }
    
    // - - - - opcrm_stat_stage  - - - -
    
    public void StagesGen(Client c, Organization org, User u){
        ArrayList <String> StagesArray = new ArrayList<String>();
        StagesArray.add("Awareness");StagesArray.add("Evaluation");StagesArray.add("Ready To Buy");
        
        ArrayList <String> StagesArrayTrlIta = new ArrayList<String>();
        StagesArrayTrlIta.add("Interessato");StagesArrayTrlIta.add("In valutazione");StagesArrayTrlIta.add("Pronto a comprare");
        
        OBCriteria <OpcrmStatStage> stage;  
        OpcrmStatStage stagenew;  
        int count=0;
        
        for (String s : StagesArray){
            stage = OBDal.getInstance().createCriteria(OpcrmStatStage.class);
            stage.add(Restrictions.eq(OpcrmStatStage.PROPERTY_CLIENT,c));
            stage.add(Restrictions.eq(OpcrmStatStage.PROPERTY_COMMERCIALNAME,s));
            
            if(stage.list().isEmpty()){
                stagenew = OBProvider.getInstance().get(OpcrmStatStage.class);
                stagenew.setActive(true);
        
                stagenew.setCreationDate(new Date());
                stagenew.setUpdated(new Date());
                stagenew.setUpdatedBy(u);
                stagenew.setCreatedBy(u);
                stagenew.setClient(c);
                stagenew.setOrganization(org);
                
                stagenew.setCommercialName(s);
                
                OBDal.getInstance().save(stagenew);
                OBDal.getInstance().flush();
                
                setStageTrlIta(stagenew, StagesArrayTrlIta.get(count));
            }
            
            count++;
        }
        
        setStageOrdering(c);
    }
    
    public void setStageTrlIta(OpcrmStatStage st, String trl){
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmStatStageTrl> translationList = OBDal.getInstance().createCriteria(OpcrmStatStageTrl.class);
        translationList.add(Restrictions.eq(OpcrmStatStageTrl.PROPERTY_OPCRMSTATSTAGE,st));
        translationList.add(Restrictions.eq(OpcrmStatStageTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
    }
    
    public void setStageOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmStatStage> stagesList = OBDal.getInstance().createCriteria(OpcrmStatStage.class);
        stagesList.add(Restrictions.eq(OpcrmStatStage.PROPERTY_CLIENT,c));
        stagesList.addOrderBy(OpcrmStatStage.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmStatStage s : stagesList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    
    // - - - - opcrm_reject_fact  - - - -
    
    public void RejectionFactorGen(Client c, Organization org, User u){
        ArrayList <String> RejectionArray = new ArrayList<String>();
        RejectionArray.add("Not possible to contact");RejectionArray.add("Lost interest/disappeared");RejectionArray.add("No fit or no need");
        RejectionArray.add("Missing or insufficient budget");RejectionArray.add("Missing decision maker");RejectionArray.add("Missing project or decision timeline");
        RejectionArray.add("Does not fit our customer target");
        
        ArrayList <String> RejectionArrayTrlIta = new ArrayList<String>();
        RejectionArrayTrlIta.add("Impossibile da contattare");RejectionArrayTrlIta.add("Perso interesse/scomparto");RejectionArrayTrlIta.add("Non serve");
        RejectionArrayTrlIta.add("Budget di spesa insufficiente");RejectionArrayTrlIta.add("Manca chi decide");RejectionArrayTrlIta.add("Manca il progetto e data di decisione");
        RejectionArrayTrlIta.add("Non rietra nel nostro target di clienti");
        
        OBCriteria <OpcrmRejectFact> rejectionFactor;  
        OpcrmRejectFact factorNew;  
        int count=0;
        
        for (String s : RejectionArray){
            
            rejectionFactor = OBDal.getInstance().createCriteria(OpcrmRejectFact.class);
            rejectionFactor.add(Restrictions.eq(OpcrmRejectFact.PROPERTY_CLIENT,c));
            rejectionFactor.add(Restrictions.eq(OpcrmRejectFact.PROPERTY_COMMERCIALNAME,s));
            
            if(rejectionFactor.list().isEmpty()){
                
                factorNew = OBProvider.getInstance().get(OpcrmRejectFact.class);
                factorNew.setActive(true);
        
                factorNew.setCreationDate(new Date());
                factorNew.setUpdated(new Date());
                factorNew.setUpdatedBy(u);
                factorNew.setCreatedBy(u);
                factorNew.setClient(c);
                factorNew.setOrganization(org);
                
                factorNew.setCommercialName(s);
                
                OBDal.getInstance().save(factorNew);
                OBDal.getInstance().flush();
                
                setRejectionTrlIta(factorNew, RejectionArrayTrlIta.get(count));
            }
            
        
            count++;
        }
        
        setRejectionOrdering(c);
    }
    
    public void setRejectionTrlIta(OpcrmRejectFact st, String trl){
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmRejectFactTrl> translationList = OBDal.getInstance().createCriteria(OpcrmRejectFactTrl.class);
        translationList.add(Restrictions.eq(OpcrmRejectFactTrl.PROPERTY_OPCRMREJECTFACT,st));
        translationList.add(Restrictions.eq(OpcrmRejectFactTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
    }
    
    public void setRejectionOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmRejectFact> rejectFactList = OBDal.getInstance().createCriteria(OpcrmRejectFact.class);
        rejectFactList.add(Restrictions.eq(OpcrmRejectFact.PROPERTY_CLIENT,c));
        rejectFactList.addOrderBy(OpcrmRejectFact.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmRejectFact s : rejectFactList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    // - - - - opcrm_lead_pos - - - -
    
    public void JobPositionGen(Client c, Organization org, User u){
        //OpcrmLeadPos
        ArrayList <String> JobPositionArray = new ArrayList<String>();
        JobPositionArray.add("CXO");JobPositionArray.add("Director");JobPositionArray.add("IT");
        JobPositionArray.add("Consultant");JobPositionArray.add("Student");JobPositionArray.add("Other");
           
        ArrayList <String> JobPositionArrayTrlIta = new ArrayList<String>();
        JobPositionArrayTrlIta.add("Capo esecutivo");JobPositionArrayTrlIta.add("Direttore");JobPositionArrayTrlIta.add("IT");
        JobPositionArrayTrlIta.add("Consulente");JobPositionArrayTrlIta.add("Studente");JobPositionArrayTrlIta.add("Altro");
        
        OBCriteria <OpcrmLeadPos> position;  
        OpcrmLeadPos positionNew;  
        int count=0;
        
        for (String s : JobPositionArray){
            
            position = OBDal.getInstance().createCriteria(OpcrmLeadPos.class);
            position.add(Restrictions.eq(OpcrmLeadPos.PROPERTY_CLIENT,c));
            position.add(Restrictions.eq(OpcrmLeadPos.PROPERTY_COMMERCIALNAME,s));
            
            if(position.list().isEmpty()){
                positionNew = OBProvider.getInstance().get(OpcrmLeadPos.class);
                positionNew.setActive(true);
        
                positionNew.setCreationDate(new Date());
                positionNew.setUpdated(new Date());
                positionNew.setUpdatedBy(u);
                positionNew.setCreatedBy(u);
                positionNew.setClient(c);
                positionNew.setOrganization(org);
                
                positionNew.setCommercialName(s);
                
                OBDal.getInstance().save(positionNew);
                OBDal.getInstance().flush();
                
                setPositionTrlIta(positionNew, JobPositionArrayTrlIta.get(count));
                
            }
        
            count++;
        }
        setJobPositionOrdering(c);
    }
    
    public void setPositionTrlIta(OpcrmLeadPos st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmLeadPosTrl> translationList = OBDal.getInstance().createCriteria(OpcrmLeadPosTrl.class);
        translationList.add(Restrictions.eq(OpcrmLeadPosTrl.PROPERTY_OPCRMLEADPOS,st));
        translationList.add(Restrictions.eq(OpcrmLeadPosTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setJobPositionOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmLeadPos> jobPosList = OBDal.getInstance().createCriteria(OpcrmLeadPos.class);
        jobPosList.add(Restrictions.eq(OpcrmLeadPos.PROPERTY_CLIENT,c));
        jobPosList.addOrderBy(OpcrmLeadPos.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmLeadPos s : jobPosList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // - - - - opcrm_profile - - - - 
    
    public void ProfileGen(Client c, Organization org, User u){
        //OpcrmProfile
        ArrayList <String> ProfileArray = new ArrayList<String>();
        ProfileArray.add("- None -");ProfileArray.add("Prospective Basic Customer");ProfileArray.add("Prospective Elite Customer");
        ProfileArray.add("Basic Customer");ProfileArray.add("Elite Customer");ProfileArray.add("Former Customer");
        ProfileArray.add("Prospective Affiliate Partner");ProfileArray.add("Prospective Elite Partner");ProfileArray.add("Affiliate Partner");
        ProfileArray.add("Elite Partner");ProfileArray.add("Former Partner");
        
        ArrayList <String> ProfileArrayTrlIta = new ArrayList<String>();
        ProfileArrayTrlIta.add("- Nessuno -");ProfileArrayTrlIta.add("Potenziale Cliente Basic");ProfileArrayTrlIta.add("Potenziale Cliente Elite");
        ProfileArrayTrlIta.add("Cliente Basic");ProfileArrayTrlIta.add("Cliente Elite");ProfileArrayTrlIta.add("Ex Cliente");
        ProfileArrayTrlIta.add("Potenziale Partner Affifliato");ProfileArrayTrlIta.add("Potenziale Partner Elite");ProfileArrayTrlIta.add("Partner Affiliato");
        ProfileArrayTrlIta.add("Partner Elite");ProfileArrayTrlIta.add("Ex Partner");
        
        OBCriteria <OpcrmProfile> profile;  
        OpcrmProfile profileNew;  
        int count=0;
        
        for (String s : ProfileArray){
            profile = OBDal.getInstance().createCriteria(OpcrmProfile.class);
            profile.add(Restrictions.eq(OpcrmProfile.PROPERTY_CLIENT,c));
            profile.add(Restrictions.eq(OpcrmProfile.PROPERTY_COMMERCIALNAME,s));
            
            if(profile.list().isEmpty()){
                
                profileNew = OBProvider.getInstance().get(OpcrmProfile.class);
                profileNew.setActive(true);
        
                profileNew.setCreationDate(new Date());
                profileNew.setUpdated(new Date());
                profileNew.setUpdatedBy(u);
                profileNew.setCreatedBy(u);
                profileNew.setClient(c);
                profileNew.setOrganization(org);
                
                profileNew.setCommercialName(s);
                
                OBDal.getInstance().save(profileNew);
                OBDal.getInstance().flush();
                
                setProfileTrlIta(profileNew, ProfileArrayTrlIta.get(count));
                
            }
            
            
            count++;
        }
        setProfileOrdering(c);
    }
    
    public void setProfileTrlIta(OpcrmProfile st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmProfileTrl> translationList = OBDal.getInstance().createCriteria(OpcrmProfileTrl.class);
        translationList.add(Restrictions.eq(OpcrmProfileTrl.PROPERTY_OPCRMPROFILE,st));
        translationList.add(Restrictions.eq(OpcrmProfileTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setProfileOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmProfile> profileList = OBDal.getInstance().createCriteria(OpcrmProfile.class);
        profileList.add(Restrictions.eq(OpcrmProfile.PROPERTY_CLIENT,c));
        profileList.addOrderBy(OpcrmProfile.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmProfile s : profileList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // - - - - opcrm_employ_num - - - -
    
    public void EmployeesGen(Client c, Organization org, User u){
        //OpcrmEmployNum
        ArrayList <String> EmployeesArray = new ArrayList<String>();
        EmployeesArray.add("1");EmployeesArray.add("2-9");EmployeesArray.add("10-49");EmployeesArray.add("50-99");EmployeesArray.add("100-249");
        EmployeesArray.add("250-499");EmployeesArray.add("500-1000");EmployeesArray.add(">1000");
        
        OBCriteria <OpcrmEmployNum> employees;  
        OpcrmEmployNum employeesNew;  
        int count=0;
        
        for (String s : EmployeesArray){
            
            employees = OBDal.getInstance().createCriteria(OpcrmEmployNum.class);
            employees.add(Restrictions.eq(OpcrmProfile.PROPERTY_CLIENT,c));
            employees.add(Restrictions.eq(OpcrmProfile.PROPERTY_COMMERCIALNAME,s));
            
            if(employees.list().isEmpty()){
                
                employeesNew = OBProvider.getInstance().get(OpcrmEmployNum.class);
                employeesNew.setActive(true);
        
                employeesNew.setCreationDate(new Date());
                employeesNew.setUpdated(new Date());
                employeesNew.setUpdatedBy(u);
                employeesNew.setCreatedBy(u);
                employeesNew.setClient(c);
                employeesNew.setOrganization(org);
                
                employeesNew.setCommercialName(s);
                
                OBDal.getInstance().save(employeesNew);
                OBDal.getInstance().flush();
                
            }
            
            count++;
        }
        
        setEmpolyeeOrdering(c);
    }
    
    public void setEmpolyeeOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmEmployNum> employeeList = OBDal.getInstance().createCriteria(OpcrmEmployNum.class);
        employeeList.add(Restrictions.eq(OpcrmEmployNum.PROPERTY_CLIENT,c));
        employeeList.addOrderBy(OpcrmEmployNum.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmEmployNum s : employeeList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // opcrm_industry
    public void IndustryGen(Client c, Organization org, User u){
        //OpcrmIndustry
        ArrayList <String> IndustryArray = new ArrayList<String>();
        IndustryArray.add("- None -");IndustryArray.add("Services");IndustryArray.add("Aerospace and Defense");IndustryArray.add("Agriculture");
        IndustryArray.add("Automotive");IndustryArray.add("Chemical");IndustryArray.add("Communications & High-Tech");IndustryArray.add("Construction");
        IndustryArray.add("Consumer Goods and Services");IndustryArray.add("Coworking Spaces");IndustryArray.add("Education & Research");
        IndustryArray.add("Financial Services & Insurance");IndustryArray.add("Food & Beverage");IndustryArray.add("Foundation / NGO / Third Sector");
        IndustryArray.add("Freight & Logistics");IndustryArray.add("Healthcare & Life Sciences");IndustryArray.add("Hospitality");
        IndustryArray.add("Insustrial Supplies & Accessories");IndustryArray.add("Media & Entertainment");IndustryArray.add("Other");
        IndustryArray.add("Professional Services");IndustryArray.add("Public sector (Government)");IndustryArray.add("Retail");IndustryArray.add("Utilities");
        
        ArrayList <String> IndustryArrayTrlIta = new ArrayList<String>();
        IndustryArrayTrlIta.add("- Nessuno -");IndustryArrayTrlIta.add("Servizi");IndustryArrayTrlIta.add("Aerospazio e Difesa");
        IndustryArrayTrlIta.add("Agricoltura");IndustryArrayTrlIta.add("Automotive");IndustryArrayTrlIta.add("Chimico");
        IndustryArrayTrlIta.add("Comunicazioni & High-Tech");IndustryArrayTrlIta.add("Costruzioni");IndustryArrayTrlIta.add("Beni e servizi di consumo");
        IndustryArrayTrlIta.add("Spazi di coworking");IndustryArrayTrlIta.add("Educazione e ricerca");IndustryArrayTrlIta.add("Servizi finanziari e assicurativi");
        IndustryArrayTrlIta.add("Prodotti alimentari e bevande");IndustryArrayTrlIta.add("Fondazioni / onlus / terzo settore");IndustryArrayTrlIta.add("Merci e logistica");
        IndustryArrayTrlIta.add("Sanità e scienze della vita");IndustryArrayTrlIta.add("Ospedali e case di cura");IndustryArrayTrlIta.add("Forniture industriali e accessori");
        IndustryArrayTrlIta.add("Media & Entertainment");IndustryArrayTrlIta.add("Altro");IndustryArrayTrlIta.add("Servizi professionali");
        IndustryArrayTrlIta.add("Pubblico settore");IndustryArrayTrlIta.add("Vendita al dettaglio");IndustryArrayTrlIta.add("Utilities");
        
        
        OBCriteria <OpcrmIndustry> industry;  
        OpcrmIndustry industryNew;  
        int count=0;
        
        for (String s : IndustryArray){
            industry = OBDal.getInstance().createCriteria(OpcrmIndustry.class);
            industry.add(Restrictions.eq(OpcrmIndustry.PROPERTY_CLIENT,c));
            industry.add(Restrictions.eq(OpcrmIndustry.PROPERTY_COMMERCIALNAME,s));
            
            if(industry.list().isEmpty()){
                
                industryNew = OBProvider.getInstance().get(OpcrmIndustry.class);
                industryNew.setActive(true);
        
                industryNew.setCreationDate(new Date());
                industryNew.setUpdated(new Date());
                industryNew.setUpdatedBy(u);
                industryNew.setCreatedBy(u);
                industryNew.setClient(c);
                industryNew.setOrganization(org);
                
                industryNew.setCommercialName(s);
                
                OBDal.getInstance().save(industryNew);
                OBDal.getInstance().flush();
                
                setIndustryTrlIta(industryNew, IndustryArrayTrlIta.get(count));
            }
            
            count++;
        }
        
        setIndustryOrdering(c);
    }
    
    public void setIndustryTrlIta(OpcrmIndustry st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmIndustryTrl> translationList = OBDal.getInstance().createCriteria(OpcrmIndustryTrl.class);
        translationList.add(Restrictions.eq(OpcrmIndustryTrl.PROPERTY_OPCRMINDUSTRY,st));
        translationList.add(Restrictions.eq(OpcrmIndustryTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setIndustryOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmIndustry> industryList = OBDal.getInstance().createCriteria(OpcrmIndustry.class);
        industryList.add(Restrictions.eq(OpcrmIndustry.PROPERTY_CLIENT,c));
        industryList.addOrderBy(OpcrmIndustry.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmIndustry s : industryList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // - - - - opcrm_lead_source - - - -
    public void LeadSourcesGen(Client c, Organization org, User u){
        //OpcrmLeadSource
        ArrayList <String> SourceArray = new ArrayList<String>();
        SourceArray.add("- None -");SourceArray.add("Direct Contact");SourceArray.add("Event");SourceArray.add("Forum/Blog");
        SourceArray.add("Media");SourceArray.add("Referral");SourceArray.add("Search Engine");SourceArray.add("Openia CRM");
        SourceArray.add("Other");
        
        ArrayList <String> SourceArrayTrlIta = new ArrayList<String>();
        SourceArrayTrlIta.add("- Nessuno -");SourceArrayTrlIta.add("Contatto diretto");SourceArrayTrlIta.add("Evento");
        SourceArrayTrlIta.add("Forum/Blog");SourceArrayTrlIta.add("Media");SourceArrayTrlIta.add("Referenza");
        SourceArrayTrlIta.add("Search Engine");SourceArrayTrlIta.add("Openia CRM");SourceArrayTrlIta.add("Altro");
        
        OBCriteria <OpcrmLeadSource> source;  
        OpcrmLeadSource sourceNew;  
        int count=0;
        
        for (String s : SourceArray){
            source = OBDal.getInstance().createCriteria(OpcrmLeadSource.class);
            source.add(Restrictions.eq(OpcrmIndustry.PROPERTY_CLIENT,c));
            source.add(Restrictions.eq(OpcrmIndustry.PROPERTY_COMMERCIALNAME,s));
            
            if(source.list().isEmpty()){
                
                sourceNew = OBProvider.getInstance().get(OpcrmLeadSource.class);
                sourceNew.setActive(true);
        
                sourceNew.setCreationDate(new Date());
                sourceNew.setUpdated(new Date());
                sourceNew.setUpdatedBy(u);
                sourceNew.setCreatedBy(u);
                sourceNew.setClient(c);
                sourceNew.setOrganization(org);
                
                sourceNew.setCommercialName(s);
                
                OBDal.getInstance().save(sourceNew);
                OBDal.getInstance().flush();
                
                setSourceTrlIta(sourceNew, SourceArrayTrlIta.get(count));
            }
            
            count++;
        
        }
        setSourceOrdering(c);    
    }
    
    public void setSourceTrlIta(OpcrmLeadSource st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmLeadSourceTrl> translationList = OBDal.getInstance().createCriteria(OpcrmLeadSourceTrl.class);
        translationList.add(Restrictions.eq(OpcrmLeadSourceTrl.PROPERTY_OPCRMLEADSOURCE,st));
        translationList.add(Restrictions.eq(OpcrmLeadSourceTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setSourceOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmLeadSource> sourceList = OBDal.getInstance().createCriteria(OpcrmLeadSource.class);
        sourceList.add(Restrictions.eq(OpcrmLeadSource.PROPERTY_CLIENT,c));
        sourceList.addOrderBy(OpcrmLeadSource.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmLeadSource s : sourceList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // - - - -  opcrm_revenue - - - - 
    public void AnnualRevenueGen(Client c, Organization org, User u){
        //OpcrmRevenue
        ArrayList <String> RevenueArray = new ArrayList<String>();
        RevenueArray.add("- None -");RevenueArray.add("< 1 mn");RevenueArray.add("1 – 5 mn");
        RevenueArray.add("5 – 10 mn");RevenueArray.add("10 – 25 mn");RevenueArray.add("25 – 50 mn");RevenueArray.add("> 50 mn");
        
        OBCriteria <OpcrmRevenue> revenue;  
        OpcrmRevenue revenueNew;  
        int count=0;
        
        for (String s : RevenueArray){
            revenue = OBDal.getInstance().createCriteria(OpcrmRevenue.class);
            revenue.add(Restrictions.eq(OpcrmIndustry.PROPERTY_CLIENT,c));
            revenue.add(Restrictions.eq(OpcrmIndustry.PROPERTY_COMMERCIALNAME,s));
            
            if(revenue.list().isEmpty()){
                
                revenueNew = OBProvider.getInstance().get(OpcrmRevenue.class);
                revenueNew.setActive(true);
        
                revenueNew.setCreationDate(new Date());
                revenueNew.setUpdated(new Date());
                revenueNew.setUpdatedBy(u);
                revenueNew.setCreatedBy(u);
                revenueNew.setClient(c);
                revenueNew.setOrganization(org);
                
                revenueNew.setCommercialName(s);
                
                OBDal.getInstance().save(revenueNew);
                OBDal.getInstance().flush();
                
            }    
            
            count++;
        }
        
        setRevenueOrdering(c);
    }
    
    public void setRevenueOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmRevenue> revenueList = OBDal.getInstance().createCriteria(OpcrmRevenue.class);
        revenueList.add(Restrictions.eq(OpcrmRevenue.PROPERTY_CLIENT,c));
        revenueList.addOrderBy(OpcrmRevenue.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmRevenue s : revenueList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // - - - - opcrm_interests - - - -
    public void LookingForGen(Client c, Organization org, User u){
        //OpcrmInterests
        ArrayList <String> InterestsArray = new ArrayList<String>();
        InterestsArray.add("- None -");InterestsArray.add("Becoming a Partner");InterestsArray.add("Becoming a Customer");
        InterestsArray.add("Learn about our company");InterestsArray.add("Other");
        
        ArrayList <String> InterestsArrayTrlIta = new ArrayList<String>();
        InterestsArrayTrlIta.add("- Nessuno -");InterestsArrayTrlIta.add("Diventare un Partner");InterestsArrayTrlIta.add("Diventare un Cliente");
        InterestsArrayTrlIta.add("Avere informazioni sull'Azienda");InterestsArrayTrlIta.add("Altro");
        
        
        OBCriteria <OpcrmInterests> interest;  
        OpcrmInterests interestNew;  
        int count=0;
        
        for (String s : InterestsArray){
            
            interest = OBDal.getInstance().createCriteria(OpcrmInterests.class);
            interest.add(Restrictions.eq(OpcrmIndustry.PROPERTY_CLIENT,c));
            interest.add(Restrictions.eq(OpcrmIndustry.PROPERTY_COMMERCIALNAME,s));
            
            if(interest.list().isEmpty()){
                
                interestNew = OBProvider.getInstance().get(OpcrmInterests.class);
                interestNew.setActive(true);
        
                interestNew.setCreationDate(new Date());
                interestNew.setUpdated(new Date());
                interestNew.setUpdatedBy(u);
                interestNew.setCreatedBy(u);
                interestNew.setClient(c);
                interestNew.setOrganization(org);
                
                interestNew.setCommercialName(s);
                
                OBDal.getInstance().save(interestNew);
                OBDal.getInstance().flush();
                
                setInterestTrlIta(interestNew, InterestsArrayTrlIta.get(count));
            }    
            
            count++;
        }
        setInterestOrdering(c);
    }
    
    public void setInterestTrlIta(OpcrmInterests st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmInterestsTrl> translationList = OBDal.getInstance().createCriteria(OpcrmInterestsTrl.class);
        translationList.add(Restrictions.eq(OpcrmInterestsTrl.PROPERTY_OPCRMINTERESTS,st));
        translationList.add(Restrictions.eq(OpcrmInterestsTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setInterestOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmInterests> interestList = OBDal.getInstance().createCriteria(OpcrmInterests.class);
        interestList.add(Restrictions.eq(OpcrmInterests.PROPERTY_CLIENT,c));
        interestList.addOrderBy(OpcrmInterests.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmInterests s : interestList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // - - - - opcrm_evaluation - - - -
    public void TimingGen(Client c, Organization org, User u){
        //OpcrmEvaluation
        ArrayList <String> EvaluationArray = new ArrayList<String>();
        EvaluationArray.add("- None -");EvaluationArray.add("Immediately");EvaluationArray.add("Undecided");
        EvaluationArray.add("Within 3 months");EvaluationArray.add("Within 6 months");
        EvaluationArray.add("Within 12 months");EvaluationArray.add("Not withing the Next Year");
        
        ArrayList <String> EvaluationArrayTrlIta = new ArrayList<String>();
        EvaluationArrayTrlIta.add("- Nessuno -");EvaluationArrayTrlIta.add("Adesso");EvaluationArrayTrlIta.add("Indeciso");
        EvaluationArrayTrlIta.add("Entro 3 mesi");EvaluationArrayTrlIta.add("Entro 6 mesi");
        EvaluationArrayTrlIta.add("Entro 12 mesi");EvaluationArrayTrlIta.add("L'anno seguente");
        
        OBCriteria <OpcrmEvaluation> evaluation;  
        OpcrmEvaluation evaluationNew;  
        int count=0;
        
        for (String s : EvaluationArray){
            
            evaluation = OBDal.getInstance().createCriteria(OpcrmEvaluation.class);
            evaluation.add(Restrictions.eq(OpcrmEvaluation.PROPERTY_CLIENT,c));
            evaluation.add(Restrictions.eq(OpcrmEvaluation.PROPERTY_COMMERCIALNAME,s));
            
            if(evaluation.list().isEmpty()){
                
                evaluationNew = OBProvider.getInstance().get(OpcrmEvaluation.class);
                evaluationNew.setActive(true);
        
                evaluationNew.setCreationDate(new Date());
                evaluationNew.setUpdated(new Date());
                evaluationNew.setUpdatedBy(u);
                evaluationNew.setCreatedBy(u);
                evaluationNew.setClient(c);
                evaluationNew.setOrganization(org);
                
                evaluationNew.setCommercialName(s);
                
                OBDal.getInstance().save(evaluationNew);
                OBDal.getInstance().flush();
                
                setEvaluationTrlIta(evaluationNew, EvaluationArrayTrlIta.get(count));
            }
            
            
            count++;
        }
        setEvaluationOrdering(c);
    }
    
    public void setEvaluationTrlIta(OpcrmEvaluation st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <opcrmEvaluationTrl> translationList = OBDal.getInstance().createCriteria(opcrmEvaluationTrl.class);
        translationList.add(Restrictions.eq(opcrmEvaluationTrl.PROPERTY_OPCRMEVALUATION,st));
        translationList.add(Restrictions.eq(opcrmEvaluationTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setEvaluationOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmEvaluation> evaluationList = OBDal.getInstance().createCriteria(OpcrmEvaluation.class);
        evaluationList.add(Restrictions.eq(OpcrmEvaluation.PROPERTY_CLIENT,c));
        evaluationList.addOrderBy(OpcrmEvaluation.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmEvaluation s : evaluationList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // - - - - opcrm_funcinterest  - - - -
    public void FuncOfInterestGen(Client c, Organization org, User u){
        //OpcrmFuncInterest
        ArrayList <String> FuncIntersArray = new ArrayList<String>();
        FuncIntersArray.add("Functionality of Interest 1");FuncIntersArray.add("Functionality of Interest 2");
        FuncIntersArray.add("Functionality of Interest 3");
        
        ArrayList <String> FuncIntersArrayTrlIta = new ArrayList<String>();
        FuncIntersArrayTrlIta.add("Funzionalità d'interesse 1");FuncIntersArrayTrlIta.add("Funzionalità d'interesse 2");
        FuncIntersArrayTrlIta.add("Funzionalità d'interesse 3");
        
        OBCriteria <OpcrmFuncInterest> funcint;  
        OpcrmFuncInterest functintNew;  
        int count=0;
        
        for (String s : FuncIntersArray){
            funcint = OBDal.getInstance().createCriteria(OpcrmFuncInterest.class);
            funcint.add(Restrictions.eq(OpcrmFuncInterest.PROPERTY_CLIENT,c));
            funcint.add(Restrictions.eq(OpcrmFuncInterest.PROPERTY_COMMERCIALNAME,s));
            
            if(funcint.list().isEmpty()){
                
                functintNew = OBProvider.getInstance().get(OpcrmFuncInterest.class);
                functintNew.setActive(true);
        
                functintNew.setCreationDate(new Date());
                functintNew.setUpdated(new Date());
                functintNew.setUpdatedBy(u);
                functintNew.setCreatedBy(u);
                functintNew.setClient(c);
                functintNew.setOrganization(org);
                
                functintNew.setCommercialName(s);
                
                OBDal.getInstance().save(functintNew);
                OBDal.getInstance().flush();
                
                setFuncIntTrlIta(functintNew, FuncIntersArrayTrlIta.get(count));
            }
            
            
            count++;
        }
        setFuncIntOrdering(c);
    }
    
    public void setFuncIntTrlIta(OpcrmFuncInterest st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmFuncinterestTrl> translationList = OBDal.getInstance().createCriteria(OpcrmFuncinterestTrl.class);
        translationList.add(Restrictions.eq(OpcrmFuncinterestTrl.PROPERTY_FUNCTIONALITYOFINTEREST,st));
        translationList.add(Restrictions.eq(OpcrmFuncinterestTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setFuncIntOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmFuncInterest> funcInterestList = OBDal.getInstance().createCriteria(OpcrmFuncInterest.class);
        funcInterestList.add(Restrictions.eq(OpcrmFuncInterest.PROPERTY_CLIENT,c));
        funcInterestList.addOrderBy(OpcrmFuncInterest.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmFuncInterest s : funcInterestList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // - - - - opcrm_title - - - -
    public void TitleGen(Client c, Organization org, User u){
        //OpcrmTitle
        ArrayList <String> TitleArray = new ArrayList<String>();
        TitleArray.add("Mr.");TitleArray.add("Ms.");
        TitleArray.add("Dr.");TitleArray.add("Eng.");TitleArray.add("Archt.");
        
        ArrayList <String> TitleArrayTrlIta = new ArrayList<String>();
        TitleArrayTrlIta.add("Sig.");TitleArrayTrlIta.add("Sig.ra");
        TitleArrayTrlIta.add("Dott.");TitleArrayTrlIta.add("Ing.");TitleArrayTrlIta.add("Arch.");
        
        OBCriteria <OpcrmTitle> title;  
        OpcrmTitle titleNew;  
        int count=0;
        
        for (String s : TitleArray){
            title = OBDal.getInstance().createCriteria(OpcrmTitle.class);
            title.add(Restrictions.eq(OpcrmTitle.PROPERTY_CLIENT,c));
            title.add(Restrictions.eq(OpcrmTitle.PROPERTY_COMMERCIALNAME,s));
            
            if(title.list().isEmpty()){
                
                titleNew = OBProvider.getInstance().get(OpcrmTitle.class);
                titleNew.setActive(true);
        
                titleNew.setCreationDate(new Date());
                titleNew.setUpdated(new Date());
                titleNew.setUpdatedBy(u);
                titleNew.setCreatedBy(u);
                titleNew.setClient(c);
                titleNew.setOrganization(org);
                
                titleNew.setCommercialName(s);
                
                OBDal.getInstance().save(titleNew);
                OBDal.getInstance().flush();
                
                setTitleTrlIta(titleNew, TitleArrayTrlIta.get(count));
            }
            
            count++;
        }
        setTitleOrdering(c);
    }
    
    public void setTitleTrlIta(OpcrmTitle st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmTitleTrl> translationList = OBDal.getInstance().createCriteria(OpcrmTitleTrl.class);
        translationList.add(Restrictions.eq(OpcrmTitleTrl.PROPERTY_OPCRMTITLE,st));
        translationList.add(Restrictions.eq(OpcrmTitleTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setTitleOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmTitle> titlesList = OBDal.getInstance().createCriteria(OpcrmTitle.class);
        titlesList.add(Restrictions.eq(OpcrmTitle.PROPERTY_CLIENT,c));
        titlesList.addOrderBy(OpcrmTitle.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmTitle s : titlesList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    // - - - -  opcrm_strategy - - - - 
    public void ImplementationStrategyGen(Client c, Organization org, User u){
        //OpcrmStrategy
        ArrayList <String> StrategyArray = new ArrayList<String>();
        StrategyArray.add("- None -");StrategyArray.add("Internally");
        StrategyArray.add("With external support");StrategyArray.add("Undecided");
        
        ArrayList <String> StrategyArrayTrlIta = new ArrayList<String>();
        StrategyArrayTrlIta.add("- Nessuna -");StrategyArrayTrlIta.add("Interna");
        StrategyArrayTrlIta.add("Con supporto esterno");StrategyArrayTrlIta.add("Indeciso");
        
        OBCriteria <OpcrmStrategy> strategy;  
        OpcrmStrategy strategyNew;  
        int count=0;
        
        for (String s : StrategyArray){
            strategy = OBDal.getInstance().createCriteria(OpcrmStrategy.class);
            strategy.add(Restrictions.eq(OpcrmStrategy.PROPERTY_CLIENT,c));
            strategy.add(Restrictions.eq(OpcrmStrategy.PROPERTY_COMMERCIALNAME,s));
            
            if(strategy.list().isEmpty()){
                
                strategyNew = OBProvider.getInstance().get(OpcrmStrategy.class);
                strategyNew.setActive(true);
        
                strategyNew.setCreationDate(new Date());
                strategyNew.setUpdated(new Date());
                strategyNew.setUpdatedBy(u);
                strategyNew.setCreatedBy(u);
                strategyNew.setClient(c);
                strategyNew.setOrganization(org);
                
                strategyNew.setCommercialName(s);
                
                OBDal.getInstance().save(strategyNew);
                OBDal.getInstance().flush();
                
                setStrategyTrlIta(strategyNew, StrategyArrayTrlIta.get(count));
            }
            
            count++;
        }
        setStrategyOrdering(c);
    }
    
    public void setStrategyTrlIta(OpcrmStrategy st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmStrategyTrl> translationList = OBDal.getInstance().createCriteria(OpcrmStrategyTrl.class);
        translationList.add(Restrictions.eq(OpcrmStrategyTrl.PROPERTY_OPCRMSTRATEGY,st));
        translationList.add(Restrictions.eq(OpcrmStrategyTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setStrategyOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmStrategy> strategyList = OBDal.getInstance().createCriteria(OpcrmStrategy.class);
        strategyList.add(Restrictions.eq(OpcrmStrategy.PROPERTY_CLIENT,c));
        strategyList.addOrderBy(OpcrmStrategy.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmStrategy s : strategyList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    }
    
    //Case Type Activity Gen
    // - - - -  opcrm_caseactivity - - - - 
    public void CaseTypeActivityGen(Client c, Organization org, User u){
        
        //OpcrmCaseactivity
        ArrayList <String> CaseActArray = new ArrayList<String>();
        CaseActArray.add("Case Activity 1");CaseActArray.add("Case Activity 2");
        CaseActArray.add("Case Activity 3");
        
        ArrayList <String> CaseActArrayTrlIta = new ArrayList<String>();
        CaseActArrayTrlIta.add("Attivita' 1");CaseActArrayTrlIta.add("Attivita' 2");
        CaseActArrayTrlIta.add("Attivita' 3");
        
        OBCriteria <OpcrmCaseactivity> activity;  
        OpcrmCaseactivity ActivityNew;  
        int count=0;
        
        for (String s : CaseActArray){
            activity = OBDal.getInstance().createCriteria(OpcrmCaseactivity.class);
            activity.add(Restrictions.eq(OpcrmCaseactivity.PROPERTY_CLIENT,c));
            activity.add(Restrictions.eq(OpcrmCaseactivity.PROPERTY_COMMERCIALNAME,s));
            
            if(activity.list().isEmpty()){
                
                ActivityNew = OBProvider.getInstance().get(OpcrmCaseactivity.class);
                ActivityNew.setActive(true);
        
                ActivityNew.setCreationDate(new Date());
                ActivityNew.setUpdated(new Date());
                ActivityNew.setUpdatedBy(u);
                ActivityNew.setCreatedBy(u);
                ActivityNew.setClient(c);
                ActivityNew.setOrganization(org);
                
                ActivityNew.setCommercialName(s);
                
                OBDal.getInstance().save(ActivityNew);
                OBDal.getInstance().flush();
                
                setCaseActTrlIta(ActivityNew, CaseActArrayTrlIta.get(count));
            }
            count++;
        }
        
        setCaseActOrdering(c);
    }
    
    public void setCaseActTrlIta(OpcrmCaseactivity cact, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmCaseactivityTrl> translationList = OBDal.getInstance().createCriteria(OpcrmCaseactivityTrl.class);
        translationList.add(Restrictions.eq(OpcrmCaseactivityTrl.PROPERTY_OPCRMCASEACTIVITY,cact));
        translationList.add(Restrictions.eq(OpcrmCaseactivityTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public void setCaseActOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmCaseactivity> activityList = OBDal.getInstance().createCriteria(OpcrmCaseactivity.class);
        activityList.add(Restrictions.eq(OpcrmCaseactivity.PROPERTY_CLIENT,c));
        activityList.addOrderBy(OpcrmCaseactivity.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmCaseactivity s : activityList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
    
    }
    
    //OpcrmWorktypeGen
    //- - - - opcrm_worktype  - - - -
    public void WorkTypeGen(Client c, Organization org, User u){
        //OpcrmWorktype
        ArrayList <String> WorkTypeArray = new ArrayList<String>();
        WorkTypeArray.add("Case Type 1");WorkTypeArray.add("Case Type 2");
        WorkTypeArray.add("Case Type 3");
        
        ArrayList <String> WorkTypeTrlIta = new ArrayList<String>();
        WorkTypeTrlIta.add("Tipo 1");WorkTypeTrlIta.add("Tipo 2");
        WorkTypeTrlIta.add("Tipo 3");
        
        OBCriteria <OpcrmWorktype> worktype;  
        OpcrmWorktype WorkTypeNew;  
        int count=0;
        
        for (String s : WorkTypeArray){
            worktype = OBDal.getInstance().createCriteria(OpcrmWorktype.class);
            worktype.add(Restrictions.eq(OpcrmWorktype.PROPERTY_CLIENT,c));
            worktype.add(Restrictions.eq(OpcrmWorktype.PROPERTY_COMMERCIALNAME,s));
            
            if(worktype.list().isEmpty()){
                
                WorkTypeNew = OBProvider.getInstance().get(OpcrmWorktype.class);
                WorkTypeNew.setActive(true);
        
                WorkTypeNew.setCreationDate(new Date());
                WorkTypeNew.setUpdated(new Date());
                WorkTypeNew.setUpdatedBy(u);
                WorkTypeNew.setCreatedBy(u);
                WorkTypeNew.setClient(c);
                WorkTypeNew.setOrganization(org);
                
                WorkTypeNew.setCommercialName(s);
                
                OBDal.getInstance().save(WorkTypeNew);
                OBDal.getInstance().flush();
                
                setWorkTypeTrlIta(WorkTypeNew, WorkTypeTrlIta.get(count));
            }
            count++;
        }
        setWorkTypeOrdering(c);
    }
    
    public void setWorkTypeTrlIta(OpcrmWorktype wt, String trl){
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmWorktypeTrl> translationList = OBDal.getInstance().createCriteria(OpcrmWorktypeTrl.class);
        translationList.add(Restrictions.eq(OpcrmWorktypeTrl.PROPERTY_OPCRMWORKTYPE,wt));
        translationList.add(Restrictions.eq(OpcrmWorktypeTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
    }
    
    public void setWorkTypeOrdering(Client c){
        int count=0;
        OBCriteria <OpcrmWorktype> worktypeList = OBDal.getInstance().createCriteria(OpcrmWorktype.class);
        worktypeList.add(Restrictions.eq(OpcrmWorktype.PROPERTY_CLIENT,c));
        worktypeList.addOrderBy(OpcrmWorktype.PROPERTY_CREATIONDATE, true);
        
        for (OpcrmWorktype s : worktypeList.list()){
            s.setOrdering(new Long(count*10));
            OBDal.getInstance().save(s);
            count++;
        }
        
        OBDal.getInstance().flush();
        
    }
    
    //ACTIVITY STATUSES : opcrm_statusfilter
    public void ActivityStatusesGen(Client c, Organization org, User u){
        //ACTIVITY TYPES BY SEQUENCE NUMBER: 0 - CALL, 1 - MEETING, 2 - TASK, 3 - OPPORTUNITY, 4 - EMAIL
        
        CallStatusGen(c, org, u);
        
        MeetingStatusGen( c, org, u);
        
        TaskStatusGen( c, org, u);
        
        OpportunityStatusGen( c, org, u);
        
        EmailStatusGen(c, org, u);
        
    }
    
    // CALL ACTIVITY
    public void CallStatusGen(Client c, Organization org, User u){
        ArrayList <String> CallArray = new ArrayList<String>();
        CallArray.add("Planned");CallArray.add("Held");
        CallArray.add("Not held");
        
        ArrayList <String> CallArrayTrlIta = new ArrayList<String>();
        CallArrayTrlIta.add("Fissata");CallArrayTrlIta.add("Tenuta");
        CallArrayTrlIta.add("Non tenuta");
        
        OBCriteria <Opcrmstatusfilter> filter;  
        Opcrmstatusfilter filterNew;  
        int count=0;
        
        for (String s : CallArray){
            filter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_CLIENT,c));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_NAME,s));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY,"CALL"));
            
            if(filter.list().isEmpty()){
                
                filterNew = OBProvider.getInstance().get(Opcrmstatusfilter.class);
                filterNew.setActive(true);
        
                filterNew.setCreationDate(new Date());
                filterNew.setUpdated(new Date());
                filterNew.setUpdatedBy(u);
                filterNew.setCreatedBy(u);
                filterNew.setClient(c);
                filterNew.setOrganization(org);
                
                filterNew.setActivityKey("CALL");
                filterNew.setName(s); 
                
                if(count==0)
                    filterNew.setDefaultstate(Boolean.TRUE);
                if(count==1)
                    filterNew.setClosingstate(Boolean.TRUE);
                    
                OBDal.getInstance().save(filterNew);
                OBDal.getInstance().flush();
                
                setActivityTrlIta(filterNew, CallArrayTrlIta.get(count));
                
            }
            
            count++;
        }
        
    }
    
    //MEETING ACTIVITY
    public void MeetingStatusGen(Client c, Organization org, User u){
        ArrayList <String> MeetingArray = new ArrayList<String>();
        MeetingArray.add("Planned");MeetingArray.add("Held");
        MeetingArray.add("Not held");
        
        ArrayList <String> MeetingArrayTrlIta = new ArrayList<String>();
        MeetingArrayTrlIta.add("Fissata");MeetingArrayTrlIta.add("Tenuta");
        MeetingArrayTrlIta.add("Non tenuta");
        
        OBCriteria <Opcrmstatusfilter> filter;  
        Opcrmstatusfilter filterNew;  
        int count=0;
        
        for (String s : MeetingArray){
            filter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_CLIENT,c));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_NAME,s));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY,"MEETING"));
            
            if(filter.list().isEmpty()){
                
                filterNew = OBProvider.getInstance().get(Opcrmstatusfilter.class);
                filterNew.setActive(true);
        
                filterNew.setCreationDate(new Date());
                filterNew.setUpdated(new Date());
                filterNew.setUpdatedBy(u);
                filterNew.setCreatedBy(u);
                filterNew.setClient(c);
                filterNew.setOrganization(org);
                
                filterNew.setActivityKey("MEETING");
                filterNew.setName(s); 
                
                if(count==0)
                    filterNew.setDefaultstate(Boolean.TRUE);
                if(count==1)
                    filterNew.setClosingstate(Boolean.TRUE);
                    
                OBDal.getInstance().save(filterNew);
                OBDal.getInstance().flush();
                
                setActivityTrlIta(filterNew, MeetingArrayTrlIta.get(count));
                
            }
            
            count++;
        }
        
        
    }
    
    //TASK ACTIVITY
    public void TaskStatusGen(Client c, Organization org, User u){
        ArrayList <String> TaskArray = new ArrayList<String>();
        TaskArray.add("In progress");TaskArray.add("Pending input");
        TaskArray.add("Completed");TaskArray.add("Deferred");TaskArray.add("Not started");
        
        ArrayList <String> TaskArrayTrlIta = new ArrayList<String>();
        TaskArrayTrlIta.add("In lavorazione");TaskArrayTrlIta.add("Input in sospeso");
        TaskArrayTrlIta.add("Completato");TaskArrayTrlIta.add("Rinviato");TaskArrayTrlIta.add("Non iniziato");
        
        OBCriteria <Opcrmstatusfilter> filter;  
        Opcrmstatusfilter filterNew;  
        int count=0;
        
        for (String s : TaskArray){
            filter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_CLIENT,c));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_NAME,s));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY,"TASK"));
            
            if(filter.list().isEmpty()){
                
                filterNew = OBProvider.getInstance().get(Opcrmstatusfilter.class);
                filterNew.setActive(true);
        
                filterNew.setCreationDate(new Date());
                filterNew.setUpdated(new Date());
                filterNew.setUpdatedBy(u);
                filterNew.setCreatedBy(u);
                filterNew.setClient(c);
                filterNew.setOrganization(org);
                
                filterNew.setActivityKey("TASK");
                filterNew.setName(s); 
                
                if(count==0)
                    filterNew.setDefaultstate(Boolean.TRUE);
                if(count==2)
                    filterNew.setClosingstate(Boolean.TRUE);
                    
                OBDal.getInstance().save(filterNew);
                OBDal.getInstance().flush();
                
                setActivityTrlIta(filterNew, TaskArrayTrlIta.get(count));
                
            }
            
            count++;
        }
        
        
    }
    
    //OPPORTUNITY ACTIVITY
    public void OpportunityStatusGen(Client c, Organization org, User u){
        ArrayList <String> OpportunityArray = new ArrayList<String>();
        OpportunityArray.add("In progress");OpportunityArray.add("Completed");
        
        
        ArrayList <String> OpportunityArrayTrlIta = new ArrayList<String>();
        OpportunityArrayTrlIta.add("In lavorazione");OpportunityArrayTrlIta.add("Conclusa");
        
        
        OBCriteria <Opcrmstatusfilter> filter;  
        Opcrmstatusfilter filterNew;  
        int count=0;
        
        for (String s : OpportunityArray){
            filter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_CLIENT,c));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_NAME,s));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY,"OPPORTUNITY"));
            
            if(filter.list().isEmpty()){
                
                filterNew = OBProvider.getInstance().get(Opcrmstatusfilter.class);
                filterNew.setActive(true);
        
                filterNew.setCreationDate(new Date());
                filterNew.setUpdated(new Date());
                filterNew.setUpdatedBy(u);
                filterNew.setCreatedBy(u);
                filterNew.setClient(c);
                filterNew.setOrganization(org);
                
                filterNew.setActivityKey("OPPORTUNITY");
                filterNew.setName(s); 
                
                if(count==0)
                    filterNew.setDefaultstate(Boolean.TRUE);
                if(count==1)
                    filterNew.setClosingstate(Boolean.TRUE);
                    
                OBDal.getInstance().save(filterNew);
                OBDal.getInstance().flush();
                
                setActivityTrlIta(filterNew, OpportunityArrayTrlIta.get(count));
                
            }
            
            count++;
        }
        
        
    }
    
    //EMAIL ACTIVITY
    public void EmailStatusGen(Client c, Organization org, User u){
        ArrayList <String> OpportunityArray = new ArrayList<String>();
        OpportunityArray.add("In progress");OpportunityArray.add("Sent");
        
        
        ArrayList <String> OpportunityArrayTrlIta = new ArrayList<String>();
        OpportunityArrayTrlIta.add("In lavorazione");OpportunityArrayTrlIta.add("Spedita");
        
        
        OBCriteria <Opcrmstatusfilter> filter;  
        Opcrmstatusfilter filterNew;  
        int count=0;
        
        for (String s : OpportunityArray){
            filter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_CLIENT,c));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_NAME,s));
            filter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY,"EMAIL"));
            
            if(filter.list().isEmpty()){
                
                filterNew = OBProvider.getInstance().get(Opcrmstatusfilter.class);
                filterNew.setActive(true);
        
                filterNew.setCreationDate(new Date());
                filterNew.setUpdated(new Date());
                filterNew.setUpdatedBy(u);
                filterNew.setCreatedBy(u);
                filterNew.setClient(c);
                filterNew.setOrganization(org);
                
                filterNew.setActivityKey("EMAIL");
                filterNew.setName(s); 
                
                if(count==0)
                    filterNew.setDefaultstate(Boolean.TRUE);
                if(count==1)
                    filterNew.setClosingstate(Boolean.TRUE);
                    
                OBDal.getInstance().save(filterNew);
                OBDal.getInstance().flush();
                
                setActivityTrlIta(filterNew, OpportunityArrayTrlIta.get(count));
                
            }
            
            count++;
        }
        
        
    }
    
    public void setActivityTrlIta(Opcrmstatusfilter st, String trl){
        
        OBCriteria <Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE,"it_IT"));
        
        OBCriteria <OpcrmStatusfilterTrl> translationList = OBDal.getInstance().createCriteria(OpcrmStatusfilterTrl.class);
        translationList.add(Restrictions.eq(OpcrmStatusfilterTrl.PROPERTY_OPCRMSTATUSFILTER,st));
        translationList.add(Restrictions.eq(OpcrmStatusfilterTrl.PROPERTY_LANGUAGE,langList.list().get(0)));
        
        translationList.list().get(0).setCommercialName(trl);

        OBDal.getInstance().save(translationList.list().get(0));
        
    }
    
    public Category BPCategoryGen(Client c, Organization org, User u){
        OBCriteria <Category> catList = OBDal.getInstance().createCriteria(Category.class);
        Category bpCat = null;
        
        catList.add(Restrictions.eq(Category.PROPERTY_CLIENT,c));
        catList.add(Restrictions.eq(Category.PROPERTY_SEARCHKEY,"CRM Lead"));
        
        if(catList.list().isEmpty())
        {
            bpCat = OBProvider.getInstance().get(Category.class);
            bpCat.setActive(true);
            bpCat.setCreationDate(new Date());
            bpCat.setUpdated(new Date());
            bpCat.setUpdatedBy(u);
            bpCat.setCreatedBy(u);
            bpCat.setClient(c);
            bpCat.setOrganization(org);
        
            bpCat.setSearchKey("CRM Lead");
            bpCat.setName("CRM Lead");
            bpCat.setDescription("CRM Lead");
            
            OBDal.getInstance().save(bpCat);
            OBDal.getInstance().flush();
        }
        else
            bpCat = catList.list().get(0);
        
        return bpCat;
        
    }
    
    //OpcrmConfig
    //Crea un record di configurazione per ogni utente del client dal quale viene lanciata la procedura
    public void ConfigTableGen(Client c, Organization org, User u){
        OBCriteria <OpcrmConfig> configList;
        OpcrmConfig config;
        Category cat;
        
        OBCriteria <User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_CLIENT, c));
        userList.add(Restrictions.eq(User.PROPERTY_OPCRMISLEAD, false));
        userList.add(Restrictions.eq(User.PROPERTY_OPCRMISCRMUSER, true));
        
        for(User usr : userList.list()){
            configList = OBDal.getInstance().createCriteria(OpcrmConfig.class);
            //configList.add(Restrictions.eq(OpcrmConfig.PROPERTY_ACTIVE,true));
            configList.add(Restrictions.eq(OpcrmConfig.PROPERTY_CLIENT,c));
            configList.add(Restrictions.eq(OpcrmConfig.PROPERTY_USERCONTACT,usr));
            
            cat = BPCategoryGen(c, org, u);
        
            if(configList.list().isEmpty())
            {
                config = OBProvider.getInstance().get(OpcrmConfig.class);
                config.setActive(true);
                config.setCreationDate(new Date());
                config.setUpdated(new Date());
                config.setUpdatedBy(u);
                config.setCreatedBy(u);
                config.setClient(c);
                config.setOrganization(org);
        
                config.setBusinessPartnerCategory(cat);
                config.setUserContact(usr);
            
                OBDal.getInstance().save(config);
                OBDal.getInstance().flush();
            }
        }
    }
    
}
