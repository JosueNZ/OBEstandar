/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import org.openbravo.model.ad.access.User;

/**
 *
 * @author diurno
 */
public class Contact {

    String id;
    String l;
    String d;
    String rev;
    String fileAsStr;
    //Attributes
    String lastName;
    String firstname;
    String email;
    String mobilePhone;
    String workPhone;
    String notes;
    //Openbravo
    User adUserOwner;

    public String getD() {
        return d;
    }

    public void setD(String d) {
        this.d = d;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFileAsStr() {
        return fileAsStr;
    }

    public void setFileAsStr(String fileAsStr) {
        this.fileAsStr = fileAsStr;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getL() {
        return l;
    }

    public void setL(String l) {
        this.l = l;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRev() {
        return rev;
    }

    public void setRev(String rev) {
        this.rev = rev;
    }

    public String getWorkPhone() {
        return workPhone;
    }

    public void setWorkPhone(String workPhone) {
        this.workPhone = workPhone;
    }

    public User getAdUserOwner() {
        return adUserOwner;
    }

    public void setAdUserOwner(User adUserOwner) {
        this.adUserOwner = adUserOwner;
    }

}
