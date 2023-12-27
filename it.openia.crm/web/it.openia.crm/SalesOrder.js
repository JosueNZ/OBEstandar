///* Copyright (C) 2008-2013 Openia S.r.l.
//   - This Source Code Form is subject to the terms of the Mozilla Public
//   - License, v. 2.0. If a copy of the MPL was not distributed with this
//   - file, You can obtain one at http://mozilla.org/MPL/2.0/. */

function SalesOrder(orderId,orderIndex, idBPartner, idBPartnerAddress, idTrxDoc, OrdDate, DelivDate, idPaymentTerm, keyInvoiceTerm, idInvoiceAddress, idWarehouse, idPriceList, linesArray, idPayMeth, totalPaid, Status){
    this.OrderId = orderId;
    this.OrderIndex = orderIndex;
    this.BPartnerId = idBPartner;
    this.BPartnerAddressId = idBPartnerAddress;
    this.TrxDocId = idTrxDoc;
    this.OrderDate = OrdDate;
    this.DeliveryDate = DelivDate;
    this.PaymentTermId = idPaymentTerm;
    this.InvoiceTermKey = keyInvoiceTerm;
    this.InvoiceAddressId = idInvoiceAddress;
    this.WarehouseId = idWarehouse;
    this.PriceListId = idPriceList;
    this.OrderLines = linesArray;
    this.PaymentMethodId = idPayMeth;
    this.TotalPaid = totalPaid;
    this.Status = Status;
}




