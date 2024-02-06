///* Copyright (C) 2008-2013 Openia S.r.l.
//   - This Source Code Form is subject to the terms of the Mozilla Public
//   - License, v. 2.0. If a copy of the MPL was not distributed with this
//   - file, You can obtain one at http://mozilla.org/MPL/2.0/. */

function TransactionDocument(TrxDocId, DocName, keyName, isKey, isReceipt, isCreditNote, isInvoice, isReturnInv){
    
    this.TrxDocId = TrxDocId;
    this.Name = DocName;
    this.KeyName = keyName;
    this.isPosKey = isKey;
    this.isReceipt = isReceipt;
    this.isCreditNote = isCreditNote;
    this.isInvoice = isInvoice;
    this.isReturnInvoice = isReturnInv;
}


