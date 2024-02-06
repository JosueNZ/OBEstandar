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
public class UserAssinedTo {

    private String name;
    private String id;
    
    public UserAssinedTo(String n,String bpId){
        this.name=n;
        this.id = bpId;
    }
    
    public String getName(){
        return this.name;
    }
    
    public String getId(){
        return this.id;
    }
    
}
