/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.access.User;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.utils.CryptoUtility;

public class ZimbraContactSync extends DalBaseProcess {

    private ProcessLogger logger;

    @Override
    public void doExecute(ProcessBundle bundle) throws NoConnectionAvailableException {

        Connection conn = bundle.getConnection().getConnection();
        logger = bundle.getLogger();

        try {

            final OBCriteria<OpcrmConfig> config = OBDal.getInstance().createCriteria(OpcrmConfig.class);
            config.add(Restrictions.eq(OpcrmConfig.PROPERTY_ACTIVE, true));
            config.add(Restrictions.eq(OpcrmConfig.PROPERTY_ZIMBRASYNCADDRESSBOOK, true));

            List<Contact> contactList = new ArrayList<Contact>();

            for (OpcrmConfig r : config.list()) {

                if (r.isZimbraSyncAddressBook() && r.getZimbraAddressBook() != null) {

                    JSONObject contactsObj = APIZimbra.getJson(r.getZimbraHost(), r.getZimbraPort(),
                            r.getZimbraUser(), CryptoUtility.decrypt(r.getZimbraPassword()), r.getZimbraAddressBook(), r.isZimbraUseSsl());

                    JSONArray contacts = contactsObj.getJSONArray("cn");
                    Contact contact = null;

                    for (int i = 0; i < contacts.length(); i++) {

                        contact = new Contact();
                        JSONObject c = contacts.getJSONObject(i);

                        if (c.has("id")) {
                            contact.setId(c.getString("id"));
                        }
                        if (c.has("rev")) {
                            contact.setRev(c.getString("rev"));
                        }
                        
                        contact.setAdUserOwner(r.getCreatedBy());

                        if (c.has("_attrs")) {
                            JSONObject attrs = c.getJSONObject("_attrs");

                            if (attrs.has("email")) {
                                contact.setEmail(attrs.getString("email"));
                            }
                            if (attrs.has("firstName")) {
                                contact.setFirstname(attrs.getString("firstName"));
                            }
                            if (attrs.has("lastName")) {
                                contact.setLastName(attrs.getString("lastName"));
                            }
                            if (attrs.has("mobilePhone")) {
                                contact.setMobilePhone(attrs.getString("mobilePhone"));
                            }
                            if (attrs.has("workPhone")) {
                                contact.setWorkPhone(attrs.getString("workPhone"));
                            }
                            if (attrs.has("notes")) {
                                contact.setNotes(attrs.getString("notes"));
                            }
                        }

                        contactList.add(contact);
                    }
                }
            }

            updateOpenbravoContacts(contactList);

        } catch (Exception e) {
            logger.log("Errore: " + e.getMessage());
        }
    }

    private void updateOpenbravoContacts(List<Contact> contactList) {

        for (Contact c : contactList) {
            final OBCriteria<User> obUsers = OBDal.getInstance().createCriteria(User.class);
            obUsers.add(Restrictions.eq(User.PROPERTY_OPCRMZM, c.getId()));

            if (obUsers.list().isEmpty()) {

                //create user
                final User u = OBProvider.getInstance().get(User.class);

                u.setFirstName(c.getFirstname());
                u.setLastName(c.getLastName());
                u.setName(c.getFirstname() + " " + c.getLastName());
                u.setUsername(c.getAdUserOwner().getId() + " " + c.getId() + " " + c.getFirstname() + " " + c.getLastName());
                u.setEmail(c.getEmail());
                u.setPhone(c.getWorkPhone());
                u.setAlternativePhone(c.getMobilePhone());
                u.setComments(c.getNotes());
                u.setOpcrmZm(c.getId());
                u.setOpcrmZmRev(c.getRev());
                u.setOpcrmIslead(true);
                u.setOpcrmLeadstatus("opcrmNew");
                u.setOPCRMAssignedTo(c.getAdUserOwner());

                OBDal.getInstance().save(u);


            } else {

                for (User u : obUsers.list()) {
                    if (c.getId().equals(u.getOpcrmZm())) { //exists a contact previously imported from zimbra
                        if (!c.getRev().equals(u.getOpcrmZmRev())) { // if has different revision

                            u.setFirstName(c.getFirstname());
                            u.setLastName(c.getLastName());
                            u.setName(c.getFirstname() + " " + c.getLastName());
                            u.setUsername(c.getFirstname() + " " + c.getLastName());
                            u.setEmail(c.getEmail());
                            u.setPhone(c.getWorkPhone());
                            u.setAlternativePhone(c.getMobilePhone());
                            u.setComments(c.getNotes());
                            u.setOpcrmZmRev(c.getRev());

                            OBDal.getInstance().save(u);

                        }
                    }
                }
            }
        }
    }
}