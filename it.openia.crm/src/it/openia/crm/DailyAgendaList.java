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
public class DailyAgendaList implements GridDataObject{
    private ArrayList <DailyAgenda> agendaDays;
    
    private Detail valueNames;
    private ArrayList <ActivityType> actTypes;
    private ArrayList <Lead> leadsList;
    private ArrayList <LeadStatus> leadStatuses;
    ArrayList <ActivityStatus> actStatuses;

    public ArrayList<ActivityStatus> getActStatuses() {
        return actStatuses;
    }

    public void setActStatuses(ArrayList<ActivityStatus> actStatuses) {
        this.actStatuses = actStatuses;
    }

    public ArrayList<ActivityType> getActTypes() {
        return actTypes;
    }

    public void setActTypes(ArrayList<ActivityType> actTypes) {
        this.actTypes = actTypes;
    }

    public ArrayList<LeadStatus> getLeadStatuses() {
        return leadStatuses;
    }

    public void setLeadStatuses(ArrayList<LeadStatus> leadStatuses) {
        this.leadStatuses = leadStatuses;
    }

    public ArrayList<Lead> getLeadsList() {
        return leadsList;
    }

    public void setLeadsList(ArrayList<Lead> leadsList) {
        this.leadsList = leadsList;
    }

    public Detail getValueNames() {
        return valueNames;
    }

    public void setValueNames(Detail valueNames) {
        this.valueNames = valueNames;
    }
    
    public ArrayList<DailyAgenda> getAgendaDays() {
        return agendaDays;
    }

    public void setAgendaDays(ArrayList<DailyAgenda> agendaDays) {
        this.agendaDays = agendaDays;
    }
    
}
