///* Copyright (C) 2008-2013 Openia S.r.l.
//   - This Source Code Form is subject to the terms of the Mozilla Public
//   - License, v. 2.0. If a copy of the MPL was not distributed with this
//   - file, You can obtain one at http://mozilla.org/MPL/2.0/. */


var OrderLineEnum = {'PRODUCT':0, 'QUANTITY':1, 'UNITPRICE':2};

function OrderLineField(fId, fName, fEnum, fVal, fExtraId){
    
    this.fieldId = fId;
    this.fieldName = fName;
    this.fieldEnum = fEnum;
    this.fieldValue = fVal;
    this.extraId = fExtraId;
}