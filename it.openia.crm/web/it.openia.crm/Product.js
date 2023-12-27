///* Copyright (C) 2008-2013 Openia S.r.l.
//   - This Source Code Form is subject to the terms of the Mozilla Public
//   - License, v. 2.0. If a copy of the MPL was not distributed with this
//   - file, You can obtain one at http://mozilla.org/MPL/2.0/. */

function Product(idProd, ProdName, stdPrice, PricesArray, catId, imgid, UPC, UPC2, UPC3, trate, isFav, department, barCodesArray){
    
    this.ProductId = idProd;
    this.ProductName = ProdName;
    this.StandardPrice = stdPrice;
    this.PListArray = PricesArray;
    this.categoryId = catId;
    this.imageID = imgid;
    this.upcCode = UPC;
    this.upc2Code = UPC2;
    this.upc3Code = UPC3;
    this.TaxRate = trate;
    this.isFavourite = isFav;
    this.department = department;
    this.barcodesArray = barCodesArray;
}