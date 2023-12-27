///* Copyright (C) 2008-2013 Openia S.r.l.
//   - This Source Code Form is subject to the terms of the Mozilla Public
//   - License, v. 2.0. If a copy of the MPL was not distributed with this
//   - file, You can obtain one at http://mozilla.org/MPL/2.0/. */

function SalesOrderLine(orderLineIndex, idProd, ProdVal, Qty, unitPrice, netPrice, listPrice, discount, department,retItem, discountAmt){
    
    this.OrderLineIndex = orderLineIndex;
    this.ProductId = idProd;
    this.ProductName = ProdVal;
    this.Quantity = Qty;
    this.UnitPrice = unitPrice;
    this.NetPrice = netPrice;
    this.ListPrice = listPrice;
    this.Discount = discount;
    this.department = department;
    this.ReturnItem = retItem;
    this.DiscountAmt = discountAmt;
}


