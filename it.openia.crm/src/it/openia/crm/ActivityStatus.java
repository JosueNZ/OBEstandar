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
public class ActivityStatus {
    
    private String statusKey;
    private String statusTranslation;
    private boolean defaultStatus;
    
    public ActivityStatus(String statkey, String stTrsl, boolean defStat){
        
        this.statusKey = statkey;
        this.statusTranslation = stTrsl;
        this.defaultStatus = defStat;
    }
    
    public String GetStatusKey(){
        return this.statusKey;
    }
    
    public String GetStatusTranslation(){
        return this.statusTranslation;
    }
    
    public boolean GetDefaultStatus(){
        return this.defaultStatus;
    }
}
