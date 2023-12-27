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
public class DetailList implements GridDataObject{
    
    private ArrayList <Detail> detailList;
    private Detail valueNames;
    private ArrayList <ActivityType> actTypes;
    private ArrayList <Lead> leadsList;
    private ArrayList <LeadStatus> leadStatuses;
    ArrayList <ActivityStatus> actStatuses;
    
    public ArrayList <Detail> getDetailList(){
        return this.detailList;
    }
    
    public void setDetailList(ArrayList <Detail> e){
        this.detailList = e;
    }
    
    public void setValueNames(Detail vn){
        this.valueNames = vn;
    }
    
    public Detail getValueNames(){
        return this.valueNames;
    }
    
    public ArrayList <ActivityType> getActivityType(){
        return this.actTypes;
    }
    
    public ArrayList <LeadStatus> getLeadStatuses(){
        return this.leadStatuses;
    }
    
    public ArrayList <ActivityStatus> getActivityStatuses(){
        return this.actStatuses;
    }
    
    public ArrayList <Lead> getLeadsList(){
        return this.leadsList;
    }
    
    public void setActivityType(ArrayList <ActivityType> e){
        this.actTypes = e;
    }
    
    public void setLeadStatuses(ArrayList <LeadStatus> e){
        this.leadStatuses = e;
    }
    
    public void setActivityStatuses(ArrayList<ActivityStatus> e){
        this.actStatuses = e;
    }
    
    public void setLeadsList(ArrayList<Lead> e){
        this.leadsList = e;
    }
   
}


