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
public class ActivityType {
    private long id;
    private String name;
    private ArrayList <ActivityStatus> activityStatusList;
    
    public ActivityType(long i, String n, ArrayList<ActivityStatus> actList){
        this.id = i;
        this.name = n;
        this.activityStatusList = actList;
    }
    
    public long getId(){
        return this.id;
    }
    
    public String getName(){
        return this.name;
    }
    
    public ArrayList <ActivityStatus> GetActivityStatusList(){
        return this.activityStatusList;
    }
    
    
}
