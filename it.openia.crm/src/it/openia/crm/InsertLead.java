/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.io.IOException;
import java.io.PrintWriter;
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
public class InsertLead extends HttpBaseServlet {
    
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        
        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }
        
        String firstname, lastname, commercialname, phone, email, leadid;
        long leadstatus=-1;
        response.setContentType("text/html; charset=utf-8");
            
        firstname = request.getParameter("firstname");
        lastname = request.getParameter("lastname");
        commercialname = request.getParameter("commercialname");
        phone = request.getParameter("phone");
        email = request.getParameter("email");
        
        leadid = request.getParameter("leadId");
        if (request.getParameter("leadstatus")!=null)
            leadstatus = Long.parseLong(request.getParameter("leadstatus"));
        
        VariablesSecureApp vars = new VariablesSecureApp(request);
        String strUserId = vars.getUser();
        if (strUserId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getUser().getId();
        }
        
        String strClientId = OBContext.getOBContext().getCurrentClient().getId();
        String strOrgId = OBContext.getOBContext().getCurrentOrganization().getId();
        PrintWriter out = response.getWriter();
        
        try{
            if(leadid==null)
                InsertNewLead(firstname,lastname,commercialname,phone, email, strUserId, strClientId, strOrgId);
            else
                ModifyLead(leadid, leadstatus);
            
            out.print("OK");
        }catch(Exception e){
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, e);
            out.println(e.toString());
        }

    }
    
    private String InsertNewLead(String firstname, String lastname, String commercialname, String phone, String email, String userId, String clientId, String orgId) {
        String res="";
        
        final OBCriteria<Client> clientList = OBDal.getInstance().createCriteria(Client.class);
        clientList.add(Restrictions.eq(Client.PROPERTY_ID, clientId));
        
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));
        
        if (userList.list().get(0).isOpcrmIsCrmUser() == null || !userList.list().get(0).isOpcrmIsCrmUser()) return null;
        
        final OBCriteria<Organization> orgList = OBDal.getInstance().createCriteria(Organization.class);
        orgList.add(Restrictions.eq(Organization.PROPERTY_ID, orgId));
        
        User nuovo = OBProvider.getInstance().get(User.class);
        nuovo.setActive(true);
        
        nuovo.setCreationDate(new Date());
        nuovo.setUpdated(new Date());
        nuovo.setUpdatedBy(userList.list().get(0));
        nuovo.setOPCRMAssignedTo(userList.list().get(0));
        nuovo.setCreatedBy(userList.list().get(0));
        nuovo.setClient(clientList.list().get(0));
        nuovo.setOrganization(orgList.list().get(0));
        
        nuovo.setFirstName(firstname);
        nuovo.setLastName(lastname);
        nuovo.setName(firstname+" "+lastname);
        nuovo.setOpcrmCommercialname(commercialname);
        nuovo.setPhone(phone);
        nuovo.setEmail(email);
        
        nuovo.setOpcrmIslead(Boolean.TRUE);
        
        OBDal.getInstance().save(nuovo);
        OBDal.getInstance().flush();
        
        res="ok";
        return res;
    }

    private void ModifyLead(String leadid, long leadstatus) {
        //throw new UnsupportedOperationException("Not yet implemented");
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(Organization.PROPERTY_ID, leadid));
        
        User u = userList.list().get(0);
        
        String status = getStatusKey(leadstatus);
        
        if(!status.isEmpty())
            u.setOpcrmLeadstatus(status);
        
        OBDal.getInstance().save(u);
        OBDal.getInstance().flush();
        
    }
    
    public String getStatusKey(long statusNum){
        String res="";
        
        if(statusNum != -1){
            OBContext.setAdminMode(true);
            final OBCriteria <Reference> statRef = OBDal.getInstance().createCriteria(Reference.class);
            statRef.add(Restrictions.eq(Reference.PROPERTY_NAME, "leadStatus"));
            
            final OBCriteria <org.openbravo.model.ad.domain.List> statRefList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE,statRef.list().get(0)));
            statRefList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEQUENCENUMBER, statusNum));
            res = statRefList.list().get(0).getSearchKey(); 
            
            OBContext.restorePreviousMode();
        }
        
        return res;
    }
    
}
