///* Copyright (C) 2008-2013 Openia S.r.l.
//   - This Source Code Form is subject to the terms of the Mozilla Public
//   - License, v. 2.0. If a copy of the MPL was not distributed with this
//   - file, You can obtain one at http://mozilla.org/MPL/2.0/. */

var OrderHeaderEnum = {'BUSINESSPARTNER':0, 'PARTNERADDRESS':1, 'TRANSACTIONDOC':2, 
                                'ORDERDATE':3, 'SCHEDULEDELDATE':4, 'PAYMENTTERMS':5, 'INVOICETERMS':6, 
                                'INVOICEADDRESS':7, 'WAREHOUSE':8, 'PRICELIST':9, 'PAYMENTMETHOD':10 };

function OrderHeaderField(fId, fName, fEnum, fVal, fExtraId){
    
    this.fieldId = fId;
    this.fieldName = fName;
    this.fieldEnum = fEnum;
    this.fieldValue = fVal;
    this.extraId = fExtraId;
}