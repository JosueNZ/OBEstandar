///* Copyright (C) 2008-2013 Openia S.r.l.
//   - This Source Code Form is subject to the terms of the Mozilla Public
//   - License, v. 2.0. If a copy of the MPL was not distributed with this
//   - file, You can obtain one at http://mozilla.org/MPL/2.0/. */

function BusinessPartner(pId, pName, pLocation, pLocId, pListId, pTermId, invTermKey, pMethodId, docId, isDeflt, cardNumber){
    
    this.bpId = pId;
    this.bpName = pName;
    this.bpLocation = pLocation;
    this.bpLocationId = pLocId;
    this.PriceListId = pListId;
    this.PaymTermsId = pTermId;
    this.invoiceTermKey = invTermKey;
    this.PaymentMethodId = pMethodId;
    this.TrxDocId = docId;
    this.isDefault = isDeflt;
    this.CardNumber = cardNumber;
}
