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
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

/**
 *
 * @author nicholas
 */
public class TypeRequestDispatcher extends HttpBaseServlet {

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
        
        String partnerId = request.getParameter("bpartner");
        LookupEnum enReq = LookupEnum.valueOf(request.getParameter("lookupType"));


        PrintWriter out = response.getWriter();

        try {
            out.println(Dispatch(strUserId, partnerId, enReq, lang));
        } catch (Exception ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String Dispatch(String strUserId, String partnerId, LookupEnum lookup, String lang) {
        String res;
        ArrayList dispatched = new ArrayList();
        Detail valueNames = new Detail("", "", "", "");

        switch (lookup) {
            
            case QUOTATION:
                dispatched = GetQuotationArray(partnerId);
                valueNames = GetOrderNames(lang);
                break;

            case ORDER:
                dispatched = GetOrderArray(partnerId);
                valueNames = GetOrderNames(lang);
                break;

            case SHIPMENT:
                dispatched = GetShipmentArray(partnerId);
                valueNames = GetShipmentNames(lang);
                break;

            case INVOICE:
                dispatched = GetInvoiceArray(partnerId);
                valueNames = GetInvoiceNames(lang);
                break;

            case ACTIVITY:
                dispatched = GetActivityArray(partnerId, strUserId);
                valueNames = GetActivityNames(lang);
                break;

            case OPPORTUNITY:
                dispatched = GetOppourtunityArray(partnerId, strUserId);
                valueNames = GetOppourtunityNames(lang);
                break;
        }

        //if(!dispatched.isEmpty())
        res = ToJson(dispatched, valueNames);

        return res;
    }
    
    public ArrayList<Detail> GetQuotationArray(String partnerId) {
        ArrayList<Detail> res = new ArrayList<Detail>();
        
        final OBCriteria<BusinessPartner> bpList = OBDal.getInstance().createCriteria(BusinessPartner.class);
        bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_ID, partnerId));

        final OBCriteria<Order> orderList = OBDal.getInstance().createCriteria(Order.class);
        orderList.add(Restrictions.eq(Order.PROPERTY_BUSINESSPARTNER, bpList.list().get(0)));
        orderList.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
        orderList.add(Restrictions.eq(Order.PROPERTY_DOCUMENTSTATUS, "UE"));
        

        orderList.addOrderBy(Order.PROPERTY_ORDERDATE, false);
        
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");

        for (Order o : orderList.list()) {
            res.add(new Detail(o.getDocumentNo(), dt1.format(o.getOrderDate()), o.getDocumentType().getName(), o.getId()));
        }

        return res;
    }
    

    public ArrayList<Detail> GetOrderArray(String partnerId) {
        ArrayList<Detail> res = new ArrayList<Detail>();

        final OBCriteria<BusinessPartner> bpList = OBDal.getInstance().createCriteria(BusinessPartner.class);
        bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_ID, partnerId));

        Disjunction orCompleted = Restrictions.disjunction();
        orCompleted.add(Restrictions.eq(Order.PROPERTY_DOCUMENTSTATUS, "CO"));
        Disjunction orClosed = Restrictions.disjunction();
        orClosed.add(Restrictions.eq(Order.PROPERTY_DOCUMENTSTATUS, "CL"));

        final OBCriteria<Order> orderList = OBDal.getInstance().createCriteria(Order.class);
        orderList.add(Restrictions.eq(Order.PROPERTY_BUSINESSPARTNER, bpList.list().get(0)));
        orderList.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));

        orderList.add(Restrictions.or(orCompleted, orClosed));

        orderList.addOrderBy(Order.PROPERTY_ORDERDATE, false);

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
                
        for (Order o : orderList.list()) {
            res.add(new Detail(o.getDocumentNo(), dt1.format(o.getOrderDate()), o.getDocumentType().getName(), o.getId()));
        }

        return res;
    }

    public Detail GetOrderNames(String lang) {

        String fieldDocNo = "DocumentNo";
        String fieldDateOrdered = "DateOrdered";
        String fieldTypeDoc = "C_DocType_ID";

        return new Detail(getTrlName(fieldDocNo, lang), getTrlName(fieldDateOrdered, lang), getTrlName(fieldTypeDoc, lang));
    }

    public ArrayList<Detail> GetShipmentArray(String partnerId) {
        ArrayList<Detail> res = new ArrayList<Detail>();

        final OBCriteria<BusinessPartner> bpList = OBDal.getInstance().createCriteria(BusinessPartner.class);
        bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_ID, partnerId));

        Disjunction orCompleted = Restrictions.disjunction();
        orCompleted.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTSTATUS, "CO"));
        Disjunction orClosed = Restrictions.disjunction();
        orClosed.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTSTATUS, "CL"));

        final OBCriteria<ShipmentInOut> shipList = OBDal.getInstance().createCriteria(ShipmentInOut.class);
        shipList.add(Restrictions.eq(ShipmentInOut.PROPERTY_BUSINESSPARTNER, bpList.list().get(0)));
        shipList.add(Restrictions.eq(ShipmentInOut.PROPERTY_SALESTRANSACTION, true));

        shipList.add(Restrictions.or(orCompleted, orClosed));

        shipList.addOrderBy(ShipmentInOut.PROPERTY_MOVEMENTDATE, false);

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
                
        for (ShipmentInOut s : shipList.list()) {
            res.add(new Detail(s.getDocumentNo(), dt1.format(s.getMovementDate()), s.getDocumentType().getName(), s.getId()));
        }


        return res;
    }

    public Detail GetShipmentNames(String lang) {

        String fieldDocNo = "DocumentNo";
        String fieldMovDate = "MovementDate";
        String fieldTypeDoc = "C_DocType_ID";


        return new Detail(getTrlName(fieldDocNo, lang), getTrlName(fieldMovDate, lang), getTrlName(fieldTypeDoc, lang));
    }

    public ArrayList<Detail> GetInvoiceArray(String partnerId) {
        ArrayList<Detail> res = new ArrayList<Detail>();

        String fieldDocNo = "DocumentNo";
        String fieldInvDate = "DateInvoiced";
        String fieldTypeDoc = "C_DocType_ID";


        final OBCriteria<BusinessPartner> bpList = OBDal.getInstance().createCriteria(BusinessPartner.class);
        bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_ID, partnerId));

        Disjunction orCompleted = Restrictions.disjunction();
        orCompleted.add(Restrictions.eq(Invoice.PROPERTY_DOCUMENTSTATUS, "CO"));
        Disjunction orClosed = Restrictions.disjunction();
        orClosed.add(Restrictions.eq(Invoice.PROPERTY_DOCUMENTSTATUS, "CL"));

        final OBCriteria<Invoice> invoiceList = OBDal.getInstance().createCriteria(Invoice.class);
        invoiceList.add(Restrictions.eq(Invoice.PROPERTY_BUSINESSPARTNER, bpList.list().get(0)));
        invoiceList.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, true));

        invoiceList.add(Restrictions.or(orCompleted, orClosed));

        invoiceList.addOrderBy(Invoice.PROPERTY_INVOICEDATE, false);

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
                
        for (Invoice i : invoiceList.list()) {
            res.add(new Detail(i.getDocumentNo(), dt1.format(i.getInvoiceDate()), i.getDocumentType().getName(), i.getId()));
        }

        return res;
    }

    public Detail GetInvoiceNames(String lang) {

        String fieldDocNo = "DocumentNo";
        String fieldInvDate = "DateInvoiced";
        String fieldTypeDoc = "C_DocType_ID";


        return new Detail(getTrlName(fieldDocNo, lang), getTrlName(fieldInvDate, lang), getTrlName(fieldTypeDoc, lang));
    }

    public ArrayList<Detail> GetActivityArray(String partnerId, String userId) {
        ArrayList<Detail> res = new ArrayList<Detail>();

        String fieldSubject = "Activity_Subject";
        String fieldStartDate = "Activity_Startdate";
        String fieldActType = "Activity_Type";
        
        //all the activities that have been assigned to the CRM user or that the CRM user has been invited to as a guest
        String hqlQuery = " select act from opcrm_activity act" + 
                          " where act.businessPartner.id= :bpid and ( act.assignedTo.id = :usrid or act.assignedTo.id in (select acc.opcrmActivity.id from opcrm_lead_activity" + 
                          " acc where acc.userContact.id = :usrid) ) order by act.activityStartdate desc";
        
        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        query.setString("bpid", partnerId);
        query.setString("usrid", userId);
        List <Opcrmactivity> actList = query.list();
        
        /*
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));
        User usr = userList.list().get(0); 
        
        final OBCriteria<BusinessPartner> bpList = OBDal.getInstance().createCriteria(BusinessPartner.class);
        bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_ID, partnerId));

        final OBCriteria<Opcrmactivity> actList = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        actList.add(Restrictions.eq(Opcrmactivity.PROPERTY_BUSINESSPARTNER, bpList.list().get(0)));
        actList.add(Restrictions.eq(Opcrmactivity.PROPERTY_ASSIGNEDTO, usr));
        actList.addOrderBy(Opcrmactivity.PROPERTY_ACTIVITYSTARTDATE, false);
        
        */
        
        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        
        for (Opcrmactivity act : actList) {
            res.add(new Detail(act.getActivitySubject(), dt1.format(act.getActivityStartdate()), act.getActivityType(), act.getId()));
        }

        return res;
    }

    public Detail GetActivityNames(String lang) {

        String fieldSubject = "Activity_Subject";
        String fieldStartDate = "Activity_Startdate";
        String fieldActType = "Activity_Type";

        return new Detail(getTrlName(fieldSubject, lang), getTrlName(fieldStartDate, lang), getTrlName(fieldActType, lang));
    }

    public ArrayList<Detail> GetOppourtunityArray(String partnerId, String userId) {
        ArrayList<Detail> res = new ArrayList<Detail>();

        String fieldName = "Opportunity_Name";
        String fieldCloseDate = "Expected_Close_Date";
        String fieldAmount = "Opportunity_Amount";
        
        //all the opportunities that have been assigned to the CRM user or that the CRM user has been invited to as a guest
        String hqlQuery = " select opp from opcrm_opportunities opp" + 
                          " where opp.businessPartner.id= :bpid and ( opp.assignedTo.id = :usrid or opp.assignedTo.id in (select acc.opcrmOpportunities.id from opcrm_opp_access" + 
                          " acc where acc.userContact.id = :usrid) ) order by opp.expectedCloseDate desc";
        
        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        query.setString("bpid", partnerId);
        query.setString("usrid", userId);
        List <Opcrmopportunities> opportunityList = query.list();
        
        /*
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));
        User usr = userList.list().get(0); 

        final OBCriteria<BusinessPartner> bpList = OBDal.getInstance().createCriteria(BusinessPartner.class);
        bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_ID, partnerId));

        final OBCriteria<Opcrmopportunities> opportunityList = OBDal.getInstance().createCriteria(Opcrmopportunities.class);
        opportunityList.add(Restrictions.eq(Opcrmopportunities.PROPERTY_BUSINESSPARTNER, bpList.list().get(0)));
        opportunityList.add(Restrictions.eq(Opcrmopportunities.PROPERTY_ASSIGNEDTO,bpList.list().get(0)));
                
        opportunityList.addOrderBy(Opcrmopportunities.PROPERTY_EXPECTEDCLOSEDATE, false);
        */
        

        SimpleDateFormat dt1 = new SimpleDateFormat("yyyy-MM-dd");
                
        for (Opcrmopportunities opp : opportunityList) {
            res.add(new Detail(opp.getOpportunityName(), dt1.format(opp.getExpectedCloseDate()), opp.getOpportunityAmount().toString(), opp.getId()));
        }

        return res;
    }

    public Detail GetOppourtunityNames(String lang) {

        String fieldName = "Opportunity_Name";
        String fieldCloseDate = "Expected_Close_Date";
        String fieldAmount = "Opportunity_Amount";

        return new Detail(getTrlName(fieldName, lang), getTrlName(fieldCloseDate, lang), getTrlName(fieldAmount, lang));
    }

    public String ToJson(ArrayList<Detail> detailList, Detail names) {

        DetailList detList = new DetailList();
        detList.setDetailList(detailList);
        detList.setValueNames(names);

        return JSONSerializer.getJSON(detList);
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
}
