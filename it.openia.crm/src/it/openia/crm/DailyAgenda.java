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
public class DailyAgenda {
    
    private String date;
    private ArrayList <Detail> activity;

    
    public DailyAgenda(String d, ArrayList <Detail> a){
        this.date=d;
        this.activity=a;
    }
    
    public ArrayList<Detail> getActivity() {
        return activity;
    }

    public void setActivity(ArrayList<Detail> activity) {
        this.activity = activity;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    
    
    
}
