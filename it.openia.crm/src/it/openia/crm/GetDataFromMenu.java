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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.Query;
import org.hibernate.criterion.Disjunction;
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
public class GetDataFromMenu extends HttpBaseServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {


        if (AuthUtils.isLoggeOut(request, response, getServletContext())) {
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
        String leadSearchText = request.getParameter("leadText");

        PrintWriter out = response.getWriter();

        try {
            out.println(Dispatch(strUserId, strClientId, enReq, lang, leadSearchText));
        } catch (Exception ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String Dispatch(String strUserId, String strClientId, LookupEnum lookup, String lang, String leadSearchText) {
        String res;
        ArrayList dispatched = new ArrayList();
        Detail valueNames = new Detail("", "", "", "", -1, "", null, "", "");

        switch (lookup) {

            case ACTIVITY:
                dispatched = GetActivityArray(strUserId);
                valueNames = GetActivityNames(lang);
                break;

            case OPPORTUNITY:
                dispatched = GetOppourtunityArray(strUserId, strClientId);
                valueNames = GetOppourtunityNames(lang);
                break;

            case LEAD:
                dispatched = GetLeadsArray(strUserId, strClientId, lang, leadSearchText);
                valueNames = GetLeadsNames(lang);
                break;
        }

        res = ToJson(dispatched, valueNames, strUserId);

        return res;
    }

    public ArrayList<Detail> GetActivityArray(String userId) {
        ArrayList<Detail> res = new ArrayList<Detail>();

        String fieldSubject = "Activity_Subject";
        String fieldStartDate = "Activity_Startdate";
        String fieldActType = "Activity_Type";

        //all the activities that have been assigned to the CRM user or that the CRM user has been invited to as a guest
        String hqlQuery = " select act from opcrm_activity act "
                + " where  (act.hideincalendar = null or  act.hideincalendar = false) and "
                + " ( act.createdBy = :usrid or"
                + "   act.assignedTo.id = :usrid or"
                + "   act.id in (select acc.opcrmActivity.id from opcrm_lead_activity acc"
                + " where acc.userContact.id = :usrid) )"
                + " order by act.activityStartdate desc";

        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        query.setString("usrid", userId);
        List<Opcrmactivity> actList = query.list();


        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        long leadstatus = -1;
        String activitystatus = "";
        ArrayList<ActivityStatus> actStatuses = null;
        String phonenumber = "";
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        User usr = null;

        for (Opcrmactivity act : actList) {
            phonenumber = "";
            OBCriteria<User> usrList = OBDal.getInstance().createCriteria(User.class);

            if (act.getRelatedLead() != null) {
                usrList.add(Restrictions.eq(User.PROPERTY_ID, act.getRelatedLead().getId()));
                usr = usrList.list().get(0);
            } else {
                usr = null;
            }


            if (act.getLeadstatus() != null) {
                leadstatus = getStatusName(act.getLeadstatus());
            } else {
                leadstatus = -1;
            }

            if (act.getActivityStatus() != null) {
                activitystatus = act.getActivityStatus().getId();
            } else {
                activitystatus = "";
            }

            if (act.getActivityType() != null) {
                actStatuses = GetActivityStatuses(act.getActivityType(), lang);
            }

            if (usr != null) {
                if (usr.getOpcrmMobile() != null) {
                    phonenumber = usr.getOpcrmMobile();
                } else if (usr.getPhone() != null) {
                    phonenumber = usr.getPhone();
                } else if (usr.getAlternativePhone() != null) {
                    phonenumber = usr.getAlternativePhone();
                }
            }

            res.add(new Detail(act.getActivitySubject(), dt1.format(act.getActivityStartdate()), act.getActivityType(), act.getId(), leadstatus, activitystatus, actStatuses, phonenumber, ""));
        }

        return res;
    }

    public Detail GetActivityNames(String lang) {

        String fieldSubject = "Activity_Subject";
        String fieldStartDate = "Activity_Startdate";
        String fieldActType = "Activity_Type";
        String fieldleadStatus = "Leadstatus";
        String actStatus = "Activity_Status";

        return new Detail(getTrlName(fieldSubject, lang), getTrlName(fieldStartDate, lang), getTrlName(fieldActType, lang), getTrlName(fieldleadStatus, lang), getTrlName(actStatus, lang));
    }

    public ArrayList<Detail> GetOppourtunityArray(String userId, String strClientId) {
        ArrayList<Detail> res = new ArrayList<Detail>();

        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));

        User activityUser = userList.list().get(0);

        final OBCriteria<OpcrmSuperopp> supUsers = OBDal.getInstance().createCriteria(OpcrmSuperopp.class);
        supUsers.add(Restrictions.eq(OpcrmSuperopp.PROPERTY_USERCONTACT, activityUser));

        String fieldName = "Opportunity_Name";
        String fieldCloseDate = "Expected_Close_Date";
        String fieldAmount = "Opportunity_Amount";
        String amt = "";
        String hqlQuery = "";

        if (supUsers.list().isEmpty() && !activityUser.getId().matches("100")) //all the opportunities that have been assigned to the CRM user or that the CRM user has been invited to as a guest
        {
            hqlQuery = " select opp from opcrm_opportunities opp"
                    + " where opp.assignedTo.id = :usrid or opp.id in (select acc.opcrmOpportunities.id from opcrm_opp_access"
                    + " acc where acc.userContact.id = :usrid) order by opp.expectedCloseDate desc";
        } else {
            hqlQuery = " select opp from opcrm_opportunities opp"
                    + " where opp.client.id = :clientid "
                    + " order by opp.expectedCloseDate desc";
        }

        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);

        if (supUsers.list().isEmpty() && !activityUser.getId().matches("100")) {
            query.setString("usrid", userId);
        } else {
            query.setString("clientid", strClientId);
        }

        List<Opcrmopportunities> opportunityList = query.list();

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");

        for (Opcrmopportunities opp : opportunityList) {
            if (opp.getOpportunityAmount() != null) {
                amt = opp.getOpportunityAmount().toString();
            } else {
                amt = "";
            }

            res.add(new Detail(opp.getOpportunityName(), dt1.format(opp.getExpectedCloseDate()), amt, opp.getId(), -1, "", null, "", ""));
        }

        return res;
    }

    public Detail GetOppourtunityNames(String lang) {

        String fieldName = "Opportunity_Name";
        String fieldCloseDate = "Expected_Close_Date";
        String fieldAmount = "Opportunity_Amount";

        return new Detail(getTrlName(fieldName, lang), getTrlName(fieldCloseDate, lang), getTrlName(fieldAmount, lang), "", "");
    }

    public String ToJson(ArrayList<Detail> detailList, Detail names, String usrId) {

        String lang = OBContext.getOBContext().getLanguage().getLanguage();

        DetailList detList = new DetailList();
        detList.setDetailList(detailList);
        detList.setValueNames(names);

        detList.setActivityType(GetReferences(lang));
        detList.setLeadStatuses(GetStatuses(lang));
        //detList.setLeadsList(GetLeads(usrId));

        return JSONSerializer.getJSON(detList);
    }

    public String getTrlName(String columnName, String lang) {
        String res = "";
        boolean found = false;

        OBContext.setAdminMode(true);

        final OBCriteria<Column> colList = OBDal.getInstance().createCriteria(Column.class);
        colList.add(Restrictions.eq(Column.PROPERTY_DBCOLUMNNAME, columnName));

        final OBCriteria<Field> fieldList = OBDal.getInstance().createCriteria(Field.class);
        fieldList.add(Restrictions.eq(Field.PROPERTY_COLUMN, colList.list().get(0)));

        final OBCriteria<FieldTrl> fieldTrlList = OBDal.getInstance().createCriteria(FieldTrl.class);
        fieldTrlList.add(Restrictions.eq(FieldTrl.PROPERTY_FIELD, fieldList.list().get(0)));


        for (FieldTrl trl : fieldTrlList.list()) {
            if (trl.getLanguage().getLanguage().equals(lang)) {
                res = trl.getName();
                found = true;
            }
        }

        if (!found) {
            res = fieldList.list().get(0).getName();
        }

        OBContext.restorePreviousMode();

        return res;
    }

    public ArrayList<ActivityType> GetReferences(String lang) {
        ArrayList<ActivityType> actTypes = new ArrayList<ActivityType>();
        ArrayList<ActivityStatus> actStatusList;

        int count = 0;

        OBContext.setAdminMode(true);
        final OBCriteria<Reference> actRef = OBDal.getInstance().createCriteria(Reference.class);
        actRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "opcrmActivtyType"));

        final OBCriteria<org.openbravo.model.ad.domain.List> actRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
        actRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE, actRef.list().get(0)));
        actRefList.addOrderBy(org.openbravo.model.ad.domain.List.PROPERTY_SEQUENCENUMBER, true);

        for (org.openbravo.model.ad.domain.List l : actRefList.list()) {

            actStatusList = GetActivityStatuses(l.getSearchKey(), lang);

            actTypes.add(count, new ActivityType(l.getSequenceNumber(), l.getName(), actStatusList));
            count++;
        }

        OBContext.restorePreviousMode();
        return actTypes;
    }

    @Deprecated
    public ArrayList<Lead> GetLeads(String userId) {

        ArrayList leads = new ArrayList<Lead>();

        String hqlQuery = " select usr from ADUser usr"
                + " where usr.opcrmIslead = true and ( usr.oPCRMAssignedTo.id = :usrid or usr.id in (select acc.userContact.id from opcrm_lead_access acc "
                + " where acc.cRMUser.id = :usrid) ) order by usr.lastName asc";

        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        query.setString("usrid", userId);
        List<User> leadList = query.list();

        String id = "";
        String firstname = "";
        String lastname = "";
        String commercialname = "";

        for (User u : leadList) {
            if (u.getFirstName() != null) {
                firstname = u.getFirstName();
            } else {
                firstname = "";
            }

            if (u.getLastName() != null) {
                lastname = u.getLastName();
            } else {
                lastname = "";
            }

            if (u.getOpcrmCommercialname() != null) {
                commercialname = u.getOpcrmCommercialname();
            } else {
                commercialname = "";
            }

            leads.add(new Lead(u.getId(), firstname, lastname, commercialname));
        }

        return leads;
    }

    public ArrayList<ActivityStatus> GetActivityStatuses(String actKey, String lang) {

        ArrayList<ActivityStatus> actStatusList = new ArrayList<ActivityStatus>();

        final OBCriteria<Opcrmstatusfilter> actFilter = OBDal.getInstance().createCriteria(Opcrmstatusfilter.class);
        actFilter.add(Restrictions.eq(Opcrmstatusfilter.PROPERTY_ACTIVITYKEY, actKey));

        OBCriteria<OpcrmStatusfilterTrl> actFilterTrl;
        OBCriteria<Language> langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        boolean isDefault = false;
        if (!lang.matches("en_US")) {
            for (Opcrmstatusfilter f : actFilter.list()) {
                isDefault = false;
                actFilterTrl = OBDal.getInstance().createCriteria(OpcrmStatusfilterTrl.class);
                actFilterTrl.add(Restrictions.eq(OpcrmStatusfilterTrl.PROPERTY_OPCRMSTATUSFILTER, f));
                actFilterTrl.add(Restrictions.eq(OpcrmStatusfilterTrl.PROPERTY_LANGUAGE, langList.list().get(0)));

                if (f.isDefaultstate() != null) {
                    isDefault = f.isDefaultstate();
                }

                if (!actFilterTrl.list().isEmpty()) {
                    actStatusList.add(new ActivityStatus(f.getId(), actFilterTrl.list().get(0).getCommercialName(), isDefault));
                } else {
                    actStatusList.add(new ActivityStatus(f.getId(), f.getName(), isDefault));
                }
            }
        } else {
            for (Opcrmstatusfilter f : actFilter.list()) {
                if (f.isDefaultstate() != null) {
                    isDefault = f.isDefaultstate();
                }

                actStatusList.add(new ActivityStatus(f.getId(), f.getName(), isDefault));
            }
        }



        return actStatusList;
    }

    public ArrayList<LeadStatus> GetStatuses(String lang) {
        ArrayList<LeadStatus> statTypes = new ArrayList<LeadStatus>();
        int count = 0;

        OBContext.setAdminMode(true);
        final OBCriteria<Reference> statRef = OBDal.getInstance().createCriteria(Reference.class);
        statRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "leadStatus"));

        OBCriteria<ListTrl> refListTrl;

        final OBCriteria<org.openbravo.model.ad.domain.List> statRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
        statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE, statRef.list().get(0)));
        statRefList.addOrderBy(org.openbravo.model.ad.domain.List.PROPERTY_SEQUENCENUMBER, true);


        for (org.openbravo.model.ad.domain.List l : statRefList.list()) {
            if (!lang.matches("en_US")) {
                refListTrl = OBDal.getInstance().createCriteria(ListTrl.class);
                refListTrl.add(Restrictions.eq(ListTrl.PROPERTY_LISTREFERENCE, l));

                if (!refListTrl.list().isEmpty() && refListTrl.list().get(0) != null) {
                    statTypes.add(count, new LeadStatus(l.getSequenceNumber(), refListTrl.list().get(0).getName()));
                } else {
                    statTypes.add(count, new LeadStatus(l.getSequenceNumber(), l.getName()));
                }
            } else {
                statTypes.add(count, new LeadStatus(l.getSequenceNumber(), l.getName()));
            }

            count++;
        }

        OBContext.restorePreviousMode();

        return statTypes;
    }

    public long getStatusName(String searchKey) {
        long res = -1;

        if (searchKey != null) {
            OBContext.setAdminMode(true);
            final OBCriteria<Reference> statRef = OBDal.getInstance().createCriteria(Reference.class);
            statRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "leadStatus"));

            final OBCriteria<org.openbravo.model.ad.domain.List> statRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE, statRef.list().get(0)));
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY, searchKey));

            if (!statRefList.list().isEmpty()) {
                res = statRefList.list().get(0).getSequenceNumber();
            }

            OBContext.restorePreviousMode();
        }

        return res;
    }

    public String getStatusNameString(String searchKey, String lang) {
        String res = "";
        OBCriteria<ListTrl> refListTrl;

        if (searchKey != null) {
            OBContext.setAdminMode(true);
            final OBCriteria<Reference> statRef = OBDal.getInstance().createCriteria(Reference.class);
            statRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "leadStatus"));

            final OBCriteria<org.openbravo.model.ad.domain.List> statRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE, statRef.list().get(0)));
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY, searchKey));

            if (!statRefList.list().isEmpty()) {
                if (!lang.matches("en_US")) {
                    refListTrl = OBDal.getInstance().createCriteria(ListTrl.class);
                    refListTrl.add(Restrictions.eq(ListTrl.PROPERTY_LISTREFERENCE, statRefList.list().get(0)));

                    if (!refListTrl.list().isEmpty() && refListTrl.list().get(0) != null) {
                        res = refListTrl.list().get(0).getName();
                    } else {
                        res = statRefList.list().get(0).getName();
                    }
                } else {
                    res = statRefList.list().get(0).getName();
                }


            }
            OBContext.restorePreviousMode();
        }

        return res;
    }

    private ArrayList GetLeadsArray(String strUserId, String strClientId, String lang, String leadSearchText) {
        //throw new UnsupportedOperationException("Not yet implemented");

        ArrayList<Detail> res = new ArrayList<Detail>();
        String phone = "";
        String status = "";
        String email = "";
        long leadstat;

        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, strUserId));

        ArrayList leads = new ArrayList<Lead>();

        String hqlQuery = "";

        final OBCriteria<OpcrmSuperusers> supUsers = OBDal.getInstance().createCriteria(OpcrmSuperusers.class);
        supUsers.add(Restrictions.eq(OpcrmSuperusers.PROPERTY_USERCONTACT, userList.list().get(0)));

        boolean limit = false;
        int limitTo = 20;

        if (supUsers.list().isEmpty() && !userList.list().get(0).getId().matches("100")) {
            if (leadSearchText != null && !leadSearchText.isEmpty()) {
                hqlQuery = " select usr from ADUser usr"
                        + " where usr.opcrmIslead = true and ( usr.oPCRMAssignedTo.id = :usrid or usr.id in (select acc.userContact.id from opcrm_lead_access acc "
                        + " where acc.cRMUser.id = :usrid) ) and lower(usr.opcrmComputednames) like lower('%" + leadSearchText + "%') order by usr.lastName asc";
            } else {
                limit = true;
                hqlQuery = " select usr from ADUser usr"
                        + " where usr.opcrmIslead = true and ( usr.oPCRMAssignedTo.id = :usrid or usr.id in (select acc.userContact.id from opcrm_lead_access acc "
                        + " where acc.cRMUser.id = :usrid) ) order by usr.lastName asc";
            }
        } else {
            if (leadSearchText != null && !leadSearchText.isEmpty()) {
                hqlQuery = " select usr from ADUser usr"
                        + " where usr.opcrmIslead = true and usr.client.id = :clientid and lower(usr.opcrmComputednames) like lower('%" + leadSearchText + "%') "
                        + " order by usr.lastName asc";
            } else {
                limit = true;
                hqlQuery = " select usr from ADUser usr"
                        + " where usr.opcrmIslead = true and usr.client.id = :clientid "
                        + " order by usr.lastName asc";
            }
        }
        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        if (limit) {
            query.setMaxResults(limitTo);
        }
        if (supUsers.list().isEmpty() && !userList.list().get(0).getId().matches("100")) {
            query.setString("usrid", strUserId);
        } else {
            query.setString("clientid", strClientId);
        }

        List<User> leadList = query.list();

        String id = "";
        String firstname = "";
        String lastname = "";
        String commercialname = "";

        for (User u : leadList) {
            if (u.getOpcrmMobile() != null) {
                phone = u.getOpcrmMobile();
            } else if (u.getPhone() != null) {
                phone = u.getPhone();
            } else {
                phone = "";
            }

            if (u.getEmail() != null) {
                email = u.getEmail();
            } else {
                email = "";
            }

            if (u.getOpcrmLeadstatus() != null) {
                leadstat = getStatusName(u.getOpcrmLeadstatus());
            } else {
                leadstat = -1;
            }

            res.add(new Detail(u.getOpcrmComputednames(), phone, getStatusNameString(u.getOpcrmLeadstatus(), lang), u.getId(), leadstat, email, null, "", ""));
        }


        return res;
    }

    private Detail GetLeadsNames(String lang) {
        //throw new UnsupportedOperationException("Not yet implemented");
        String leadName = "Name";
        String leadPhone = "Phone";
        String leadState = "EM_Opcrm_Leadstatus";

        return new Detail(getTrlName(leadName, lang), getTrlName(leadPhone, lang), getTrlName(leadState, lang), "", "");


    }
}
