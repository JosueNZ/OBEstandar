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
public class LookupType {
    
    private String name;
    private LookupEnum type;
    
    public LookupType(String n, LookupEnum t){
        this.name = n;
        this.type = t;
    }
    
    public String getName(){
        return this.name;
    }
    
    public LookupEnum getLookupEnum(){
        return this.type;
    }
}
