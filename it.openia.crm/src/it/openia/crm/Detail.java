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
public class Detail {
    
    private String value1;
    private String value2;
    private String value3;
    private String id;
    private String value4;
    private String value5;
    private long value4num;
    ArrayList <ActivityStatus> actStatuses;
    String phoneNum;
    String leadName;
    
    public Detail (String v1, String v2, String v3, String i, long v4, String v5, ArrayList <ActivityStatus> acts, String phonenum, String leadname){
        this.value1 = v1;
        this.value2 = v2;
        this.value3 = v3;
        this.id = i;
        this.value4num = v4;
        this.value5 = v5;
        this.actStatuses = acts;
        this.phoneNum = phonenum;
        this.leadName = leadname;
    }
    
    public Detail (String v1, String v2, String v3, String v4, String v5){
        this.value1 = v1;
        this.value2 = v2;
        this.value3 = v3;
        this.value4 = v4;
        this.value5 = v5;
    }
    
    public Detail (String v1, String v2, String v3){
        this.value1 = v1;
        this.value2 = v2;
        this.value3 = v3;
    }
    
    public Detail (String v1, String v2, String v3, String v4){
        this.value1 = v1;
        this.value2 = v2;
        this.value3 = v3;
        this.value4 = v4;
    }
    
    public String GetValue1(){
        return this.value1;
    }
    
    public String GetValue2(){
        return this.value2;
    }
    
    public String GetValue3(){
        return this.value3;
    }
    
    public String GetId(){
        return this.id;
    }
    
    
    
}
