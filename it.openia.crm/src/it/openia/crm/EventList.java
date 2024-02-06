/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.util.ArrayList;
/**
 *
 * @author nicholas
 */
public class EventList implements GridDataObject{
    private ArrayList <Event> events;
    private ArrayList <ActivityType> actTypes;
    private ArrayList <LeadStatus> leadStatuses;
    private ArrayList <Lead> leadsList;
    private ArrayList <BPartner> partnerList;
    private ArrayList <UserAssinedTo> assignedtoList;

    boolean showMenu;
    
    public ArrayList <Event> getEventList(){
        return this.events;
    }
    
    public ArrayList <ActivityType> getActivityType(){
        return this.actTypes;
    }
    
    public boolean getShowMenu(){
        return this.showMenu;
    }
    
    public ArrayList <Lead> getLeadsList(){
        return this.leadsList;
    }
    
    public void setEventList(ArrayList <Event> e){
        this.events = e;
    }
    
    public void setActivityType(ArrayList <ActivityType> e){
        this.actTypes = e;
    }
    
    public void setLeadStatuses(ArrayList <LeadStatus> e){
        this.leadStatuses = e;
    }
    
    public void setShowMenu(boolean sm){
        this.showMenu = sm;
    }
    
    public void setLeadsList(ArrayList<Lead> e){
        this.leadsList = e;
    }

    public ArrayList<BPartner> getPartnerList() {
        return this.partnerList;
    }

    public void setPartnerList(ArrayList<BPartner> partnerList) {
        this.partnerList = partnerList;
    }

    public ArrayList<UserAssinedTo> getAssignedtoList() {
        return this.assignedtoList;
    }

    public void setAssignedtoList(ArrayList<UserAssinedTo> assignedtoList) {
        this.assignedtoList = assignedtoList;
    }
    
}
