/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
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
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.quartz.JobExecutionException;

/**
 *
 * @author nicholas
 */
public class GetLookupInfo extends HttpBaseServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {

        if (AuthUtils.isLoggeOut(request, response, getServletContext())) {
            return;
        }

        //response.setContentType("application/json; charset=utf-8");
        response.setContentType("application/json; charset=utf-8");

        boolean logoutButton = false;
        VariablesSecureApp vars = new VariablesSecureApp(request);
        String strUserId = vars.getUser();

        if (strUserId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getUser().getId();
            logoutButton = true;
        }

        String partnerText = "";
        if (request.getParameter("text") != null) {
            partnerText = request.getParameter("text");
        }

        int partnerCategory = 0;
        if (request.getParameter("value") != null) {
            partnerCategory = Integer.parseInt(request.getParameter("value"));
        }


        PrintWriter out = response.getWriter();

        try {
            out.println(GetBPartnerJSon(strUserId, partnerText, partnerCategory, logoutButton));
        } catch (Exception ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public String GetBPartnerJSon(String userId, String partnerName, int bpCat, boolean logoutButton) throws JobExecutionException {
        String res = "";

        ArrayList partners;

        try {
            partners = GetPartnersArray(userId, partnerName, bpCat);
        } catch (Exception e) {
            throw new JobExecutionException(e.getMessage(), e);
        }

        res = ToJson(partners, logoutButton);

        return res;

    }

    public ArrayList<BPartner> GetPartnersArray(String userId, String partnerName, int bpCat) throws JobExecutionException {

        ArrayList<BPartner> res = new ArrayList();

        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, userId));

        final OBCriteria<BusinessPartner> bpList = OBDal.getInstance().createCriteria(BusinessPartner.class);
        //bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_CLIENT, userList.list().get(0).getClient()));

        //ricerca nome bp ed eventualmente categoria
        bpList.setMaxResults(20);
        if (!partnerName.isEmpty()) {
            bpList.add(Restrictions.ilike(BusinessPartner.PROPERTY_NAME, "%" + partnerName + "%"));
        }
        if (bpCat == 2) {
            bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_CUSTOMER, true));
        }
        if (bpCat == 3) {
            bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_VENDOR, true));
        }

        bpList.addOrderBy(BusinessPartner.PROPERTY_NAME, true);

        OBCriteria<Location> bpLoc;
        String name, address, phone;
        //StringBuilder address;
        Location loc;

        for (BusinessPartner bp : bpList.list()) {
            address = "";
            phone = "";
            name = "";
            bpLoc = OBDal.getInstance().createCriteria(Location.class);
            bpLoc.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, bp));
            bpLoc.add(Restrictions.eq(Location.PROPERTY_INVOICETOADDRESS, true));

            if (!bpLoc.list().isEmpty()) {
                loc = bpLoc.list().get(0);
                address = ((loc.getLocationAddress().getAddressLine1() != null) ? loc.getLocationAddress().getAddressLine1() + ", " : "").concat((loc.getLocationAddress().getCityName() != null) ? loc.getLocationAddress().getCityName() + ", " : "").concat((loc.getLocationAddress().getCountry() != null) ? loc.getLocationAddress().getCountry().getName() : "");

                //address = loc.getName();


                if (loc.getPhone() != null) {
                    phone = "Tel. " + loc.getPhone();
                }

            }

            res.add(new BPartner(bp.getName(), address, phone, bp.getId()));

        }

        return res;
    }

    public String ToJson(ArrayList<BPartner> bPBs, boolean logout) {

        BPartnerList partnerList = new BPartnerList();
        partnerList.setBPartnerList(bPBs);
        partnerList.setLogout(logout);

        return JSONSerializer.getJSON(partnerList);
    }
}
