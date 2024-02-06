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
public class LookupTypeList implements GridDataObject{
    
    private ArrayList <LookupType> lookupTypes;
    
    boolean logoutButton;
    
    public ArrayList <LookupType> getLookupTypeList(){
        return this.lookupTypes;
    }
    
    public void setLookupTypeList(ArrayList <LookupType> e){
        this.lookupTypes = e;
    }
    
    public boolean getLogout(){
        return this.logoutButton;
    }
    
    public void setLogout(boolean l){
        this.logoutButton = l;
    }
}
