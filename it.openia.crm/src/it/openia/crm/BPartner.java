/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

/**
 *
 * @author nicholas
 */
public class BPartner {

    private String name;
    private String address;
    private String phone;
    private String id;
    
    public BPartner(String n, String a, String p, String bpId){
        this.name=n;
        this.address=a;
        this.phone=p;
        this.id = bpId;
    }
    
    public String getName(){
        return this.name;
    }
    
    public String getAddress(){
        return this.address;
    }
    
    public String getPhone(){
        return this.phone;
    }
    
    public String getId(){
        return this.id;
    }
    
}
