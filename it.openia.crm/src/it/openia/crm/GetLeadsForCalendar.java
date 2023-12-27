/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
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
import org.openbravo.model.ad.system.Client;

/**
 *
 * @author nicholas
 */
public class GetLeadsForCalendar extends HttpBaseServlet {

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        VariablesSecureApp vars = new VariablesSecureApp(request);
        
        User u = OBContext.getOBContext().getUser();
        Client c = OBContext.getOBContext().getCurrentClient();
        String t = request.getParameter("term");
        
        final OBCriteria <OpcrmSuperusers> supUsers = OBDal.getInstance().createCriteria(OpcrmSuperusers.class);
        supUsers.add(Restrictions.eq(OpcrmSuperusers.PROPERTY_USERCONTACT,u));
        
        String hqlQuery = "";
        
        if(supUsers.list().isEmpty() && !u.getId().matches("100"))
            hqlQuery = " select usr from ADUser usr" + 
                          " where usr.opcrmIslead = true and ( usr.oPCRMAssignedTo.id = :usrid or usr.id in (select acc.userContact.id from opcrm_lead_access acc " + 
                          " where acc.cRMUser.id = :usrid) ) and "+
                          " ( lower(usr.opcrmComputednames) like lower('%"+ t +"%'))" +
                          " order by usr.lastName asc";
        else
            hqlQuery = " select usr from ADUser usr" + 
                          " where usr.opcrmIslead = true and usr.client.id = :clientid and " +
                          " ( lower(usr.opcrmComputednames) like lower('%"+ t +"%'))" +
                          " order by usr.lastName asc";
        
        Query query = OBDal.getInstance().getSession().createQuery(hqlQuery);
        
        if(supUsers.list().isEmpty() && !u.getId().matches("100"))
            query.setString("usrid", u.getId());
        else
            query.setString("clientid", c.getId());
        
        List <User> leadList = query.list();
        
        
        StringBuilder jsonResult = new StringBuilder();
        jsonResult.append("[");
        int count = 0;
        boolean found=false;
        
        for (User p : leadList)    
            jsonResult.append("{\"id\":\"" + p.getId() + "\", \"value\":\"" + p.getOpcrmComputednames() + "\", \"label\":\"" + p.getOpcrmComputednames() + "\"}");
         
        
        jsonResult.append("]");

        response.setContentType("application/json; charset=utf-8");
        PrintWriter out = response.getWriter();
        out.println(jsonResult.toString());
    }
    
}
