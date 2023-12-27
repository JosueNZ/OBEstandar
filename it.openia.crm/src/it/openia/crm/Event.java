/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.util.Date;

/**
 *
 * @author nicholas
 */
public class Event {
    
    private String id;
    private String title;
    private String start;
    private String end;
    private String description;
    private String url;
    private String className;
    private String bpName;
    private String leadName;
    private String subject;
    private String actTypeStatusId;
    private long actType;
    private long statName;
    private boolean allDay;
    private String imgText;
    
    
    public Event(String id, String t, String s, String e, boolean ad, String u, long act, String d, String cn, 
                    String partnerName, String lName, long stName, String actstatusid, String subject, String img){
        this.id = id;
        this.title = t;
        this.start = s;
        this.end = e;
        this.allDay = ad;
        this.url = u;
        this.actType = act;
        this.description = d;
        this.className = cn;
        this.bpName = partnerName;
        this.leadName = lName;
        this.statName = stName;
        this.actTypeStatusId = actstatusid;
        this.subject = subject;
        this.imgText = img;
    }
    
    public String GetTitle(){
        return this.title;
    }
    
    public String GetStart(){
        return this.start;
    }
    
    public String GetEnd(){
        return this.end;
    }
    
    public String GetId(){
        return this.id;
    }
    
    public boolean GetAllDay(){
        return this.allDay;
    }
    
    public String getUrl(){
        return this.url;
    }
    
    public long getActType(){
        return this.actType;
    }
    
    public long GetStatName(){
        return this.statName;
    }
    
    public String GetDescription(){
        return this.description;
    }
    
    public String GetClassName(){
        return this.className;
    }
    
    public String GetbpName(){
        return this.bpName;
    }
    
    public String GetleadName(){
        return this.leadName;
    }
    
    public String GetActTypeStatusId(){
        return this.actTypeStatusId;
    }
    
    public String GetImgText(){
        return this.imgText;
    }
}
