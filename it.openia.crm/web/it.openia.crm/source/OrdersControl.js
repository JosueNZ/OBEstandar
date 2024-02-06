/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

enyo.kind({
    name: "OrdersApp",
    classes: "list-sample-contacts enyo-fit",
    fit: true,
    //controlli
    components:[
            {kind:"Panels", classes:"enyo-fit", arrangerKind:"CollapsingArranger", components: [
                    {kind:"Panels", name:"listPanels", classes:"onyx todos-panels", arrangerKind:"CarouselArranger", components: [
                        
                        // PANEL 1 (LIST OF ORDER HEADERS)
                        {kind:"FittableRows", components: [
                            {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                                {name:"OrdersSet", content:"Order Set", fit:true}
                            ]},
                            {kind:"Scroller", fit:true, classes:"todos-list", components: [
                                
                                {name:"orderSetRepeater" ,kind:"Repeater", onSetupItem: "setOrderSet", count:"", components: [
                                   {kind:"onyx.Item", classes:"todos-list-item", ontap:"InstanceInsert", components: [
                                        {name:"orderSetInstance", content:"nuovo ordine", classes:"row_title"},
                                        {name: "orderInstanceValue", content:"inserisci nuovo", classes: "list-sample-contacts-description"},
                                        {name: "addLinesButton", content:"Add Lines", classes:"list-orders-button", kind: "onyx.Button", ontap: "setAddLinesPanel", 
                                        	published:{
                                                    orderId: ""
						},
                                                setOrderId: function(id){this.orderId=id;},
						getOrderId: function(){return this.orderId;}
					},
                                        {name: "removeOrdersButton", content:"Remove Order", classes:"list-orders-button", kind: "onyx.Button", ontap: "removeOrderPopupStart", popup:"orderRemovePopup", 
                                        	published:{
                                                    orderId: ""
						},
                                                setOrderId: function(id){this.orderId=id;},
						getOrderId: function(){return this.orderId;}
					}
                                         
                                    ]},
                                
                                ]}
                            	   
                            ]},
                            {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", components: [
                                {kind:"onyx.Grabber"},
                                {name:"logoutButton", kind:"onyx.Button", content:"Menu", ontap:"redirectToMenu"},
                                {name:"flushButton",kind:"onyx.Button", content:"Sync Orders", ontap:"flushOrdersInDB", popup:"orderFlushSyncPopup"},
                                {name: "orderRemovePopup", kind: "onyx.Popup", centered: true, floating: true, modal: true, classes:"onyx-sample-popup", style: "padding: 10px;background-color: #64932D;font-size: 21px;", 
                                            components:[
                                                {name:"removeText", content: ""},
                                                {kind:"onyx.Button", content: "Yes", ontap:"removeOrder"},
                                                {tag:"span"},
                                                {kind:"onyx.Button", content: "No", ontap:"hideOrderRemovePopup"}
                                            ]
                                },
                                {name: "orderFlushSyncPopup", kind: "onyx.Popup", centered: true, floating: true, modal: true, content:"", classes:"onyx-sample-popup", style: "padding: 10px;background-color: #64932D;font-size: 21px;"},
                                {fit:true}
                            ]}
                        ]},
                    
                        // PANEL 2 (HEADER FIELDS AND LINE LIST)                  
                        {kind:"FittableRows", components: [
                            {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                                {name:"Ordersmenu", content:"Insert Order", fit:true}
                            ]},
                            {kind:"Scroller", fit:true, classes:"todos-list", components: [
                                
                                {name:"orderMenuRepeater" ,kind:"Repeater", onSetupItem: "setOrderField", count:"", components: [
                                   {kind:"onyx.Item", classes:"todos-list-item", ontap:"fieldDispatch", components: [
                                        {name:"orderField", content:"", classes:"row_title"},
                                        {name: "orderFieldValue", content:"", classes: "list-sample-contacts-description"},
                                    ]} 
                                ]},
                                {name:"orderLineSetRepeater" ,kind:"Repeater", onSetupItem: "setOrderLineSet", count:"", components: [
                                   {kind:"onyx.Item", classes:"todos-list-item", ontap:"OrderLineInstanceInsert", components: [
                                        {name:"orderLineInstance", content:"Insert Line", classes:"row_title"},
                                        {name: "orderLineInstanceValue", content:"New Line", classes: "list-sample-contacts-description"},
                                        {name: "removeLinesButton", content:"Remove Orderline", classes:"list-orders-button", kind: "onyx.Button", ontap: "removeOrderLinePopupStart", popup:"orderLineRemovePopup", 
                                        	published:{
                                                    orderId: ""
						},
                                                setOrderId: function(id){this.orderId=id;},
						getOrderId: function(){return this.orderId;}
					}
                                    ]} 
                                ]}
                            	   
                            ]},
                            {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", components: [
                                {kind:"onyx.Grabber"},
                                {kind:"onyx.Button", content:"Back", ontap:"backToInstances"},
                                //{name:"showButton",kind:"onyx.Button", content:"Show Order", ontap:"showOrderHeader"},
                                {name:"saveButton",kind:"onyx.Button", content:"Save Order", ontap:"saveOrderHeader", popup: "orderPopup"},
                                //{name:"removeButton",kind:"onyx.Button", content:"Remove All (debug button)", ontap:"removeOrderHeader"},
                                {name: "orderPopup", kind: "onyx.Popup", onHide:"setOrderInstancePanel", centered: true, floating: true, modal: true, classes:"onyx-sample-popup", style: "padding: 10px;background-color: #64932D;", content: ""}, 
                                {name: "orderLineRemovePopup", kind: "onyx.Popup", centered: true, floating: true, modal: true, classes:"onyx-sample-popup", style: "padding: 10px;background-color: #64932D;font-size: 21px;", 
                                            components:[
                                                {name:"removeLineText", content: ""},
                                                {kind:"onyx.Button", content: "Yes", ontap:"removeOrderLine"},
                                                {tag:"span"},
                                                {kind:"onyx.Button", content: "No", ontap:"hideOrderLineRemovePopup"}
                                            ]},
                                {fit:true}
                            ]}
                        ]},

                        // PANEL 3 (EDIT HEADER OR LINE FIELDS)
                        {kind:"FittableRows", components: [
                            {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                                {name:"descrHeader", content:"Description", fit:true}
                            ]},
                            {kind:"Scroller",name:"OrderValueScroller", fit:true, classes:"todos-list", components: [
                                 {name:"descrRepeater", kind:"Repeater", ontap:"descrDispatch", onSetupItem: "setDescrItem", count:"", components: [
                                    
                                    {name: "TapItem", kind: "ItemToTap", classes: "todos-list-item list-sample-contacts-item enyo-border-box"}
                                 
                                 ]},
                                 {name:"dateBox", classes: "onyx-toolbar-inline", components: [
									{name:"datePicker", kind:"onyx.DatePicker", minYear:2010, maxYear:2020},
								    {tag: "span"},
								    {kind:"onyx.Button", content: "SAVE", ontap:"descrDispatch"}
									
								]},
                                 {name:"OrderLineFieldRepeater", kind:"Repeater", onSetupItem: "setOrderLineField", count:"", components: [
                                    {kind:"onyx.Item", classes:"todos-list-item", ontap:"OrderLineFieldDispatch", components: [
                                        {name:"orderLineField", content:"testField", classes:"row_title"},
                                        {name: "orderLineFieldValue", content:"testValue", classes: "list-sample-contacts-description"}, 
                                    ]}    
                                ]}                           
                                                            
                            ]},
                            
                            {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                                {kind:"onyx.Button", content:"Back", ontap:"backToMain"},
                                {name:"saveLineButton",kind:"onyx.Button", content:"Save Orderline", ontap:"saveOrderLine", popup: "orderLinePopup"},
                                {name: "orderLinePopup", kind: "onyx.Popup", onHide:"setOrderLineInstancePanel", centered: true, floating: true, modal: true, classes:"onyx-sample-popup", style: "padding: 10px;background-color: #64932D;", content: ""},
                                {fit:true}
                            ]}
                        ]},

                        // PANEL 4 (EDIT LINE FIELDS)
                        {kind:"FittableRows", components: [ 
                            {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                                {name:"lineDescrHeader", content:"Line Description", fit:true}
                            ]},
                            
                            {kind:"Scroller",name:"OrderLineValueScroller", fit:true, classes:"todos-list", components: [
                                {name:"OrderLineValueRepeater", kind:"Repeater", ontap:"lineDispatch", onSetupItem: "setOrderLineValues", count:"", components: [
                                    
                                    {name:"orderLineValue", kind: "OrderLineToTap", classes: "todos-list-item list-sample-contacts-item enyo-border-box"}
                                       
                                ]},
                            
                                {style:"margin-left: 30px;margin-top: 30px;",name:"numberInbutBox",kind: "onyx.InputDecorator", components: [
                                    //quantity of item
                                    {name:"numberInput", kind: "onyx.Input", placeholder: "Enter number", value: "1"},
                                ]},
                            
                                {style:"margin-left: 10px;",name:"numberInputSave",kind:"onyx.Button", content: "SAVE", ontap:"lineDispatch"}
                            ]},
                            
                            
                            {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [                                
                                {kind:"onyx.Button", content:"Back", ontap:"backToProd"},
                                {fit:true}
                            ]}
                        ]}
                    
                    ]},                    
                    {kind: "orderFieldManager", onOrderTypes:"setOrderMenuRepeater"}
            ]}
        
    ],
    
    rendered: function() {
		this.inherited(arguments);
                this.getOrdersInStorage();
                this.fillStorageOrderInstances();
                this.$.orderLineSetRepeater.hide();
	},
    
    backToMain: function() {
    	this.$.listPanels.setIndex(1);
    },
    
    backToInstances: function(){
        if (this.fieldTypes != undefined)
            this.ResetOrderFields();
        
        this.$.listPanels.setIndex(0);
    },
    
    backToProd: function(){
        this.$.numberInput.setValue("");
        this.$.listPanels.setIndex(2);
        return true;
    },
    
    noDrag:function(){
    	return true;
    },
    
    getOrdersInStorage: function(){
        this.ordersInStorage = new Array(); 
        var count = 0;
        
        for (var i=0, l=localStorage.length; i<l; i++){
            var key = localStorage.key(i);
            if(key.indexOf("ORD-") != -1){
                var value = localStorage[key];
                this.ordersInStorage[count] = JSON.parse(value);
                count++;
            }
        }
        
    },

    removeOrdersFromStorage: function(){
        for (var i = 0 , l = localStorage.length; i<l ; i++) {
            var k = localStorage.key(i);
            if(k.indexOf("ORD-") != -1)
                localStorage.removeItem(k);
        }
    },
    
flushOrdersInDB: function(inSender, inEvent){
        var p = this.$[inSender.popup];
        
        // salva ogni ordine in this.ordersInStorage nel db e lo flusha dall'interfaccia
				
        var controlObj = this;
        if(this.ordersInStorage.length>0){
			
			for (i=0; i<this.ordersInStorage.length; i++){
		        var ordersArrayToSave = JSON.stringify(this.ordersInStorage[i]); 
			
			$.ajax({
				type: "POST",
                url: "../it.openia.crm.SaveOrdersInDB",
                error: function(err) {
						alert(err.responseText);
                            if (p) {
                                p.setContent("Unable to connect to server..");
                                p.show();
                                setInterval(function(){p.hide()},800);
                            }
                        },
                data: {
                                jsonOrders:ordersArrayToSave
                    } 
                }).done(function(data) { 
                    if(data.length!=0 && data.indexOf("ERROR") != 0){
                            controlObj.removeOrdersFromStorage();
                            controlObj.getOrdersInStorage();
                            controlObj.$.orderSetRepeater.setCount(controlObj.ordersInStorage.length+1);

                            if (p) {
                                if(parseInt(data)==1)
                                    p.setContent(data+" new order saved in Openbravo!");
                                else
                                    p.setContent(data+" new orders saved in Openbravo!");

                                p.show();
                                setInterval(function(){p.hide()},800);
                            }


                        } else{
                            p.setContent(data);
                            p.show();
                            setInterval(function(){p.hide()},800);
                        }
                });
			
			}			
            
        }
        else
            {
                p.setContent("No orders to save");
                p.show();
                setInterval(function(){p.hide()},800);
            }
        
    },
    
    // - - - - METODI PRIMO PANNELLO ORDINI - - - - - //
    fillStorageOrderInstances: function(){
        this.$.orderSetRepeater.setCount(this.ordersInStorage.length+1);
    },
    
    removeOrderPopupStart: function(inSender, inEvent){
        var p = this.$[inSender.popup];
        var i = inEvent.index;
        
        this.KeyToRemove = this.ordersInStorage[i].OrderIndex;
        
        if (p) {
            this.$.removeText.setContent("Remove Order "+this.KeyToRemove+"?");
            p.show();
        }
        return true;
    },
    hideOrderRemovePopup: function(){
        this.$.orderRemovePopup.hide();
    },
    
    removeOrder: function(inSender, inEvent){
        //...
        var i = inEvent.index;
        
        localStorage.removeItem(this.KeyToRemove);
        
        this.$.orderRemovePopup.hide();
        
        this.getOrdersInStorage();
        this.fillStorageOrderInstances();
    
    },
    
    setOrderSet: function(inSender, inEvent){
        var item = inEvent.item;
    	var i = inEvent.index;
        
        if(i!=this.ordersInStorage.length){
            var field = this.ordersInStorage[i].OrderIndex;
            item.$.orderSetInstance.setContent(field);
            item.$.orderInstanceValue.setContent("");
            item.$.addLinesButton.setOrderId(field);
            item.$.addLinesButton.show();
            item.$.removeOrdersButton.setOrderId(field);
            item.$.removeOrdersButton.show();
        }
        else{
            item.$.orderSetInstance.setContent("New Order");
            item.$.orderInstanceValue.setContent("Insert New");
            item.$.addLinesButton.hide();
            item.$.removeOrdersButton.hide();
        }
    },
    
    InstanceInsert: function(inSender, inEvent){
        var i = inEvent.index;
        if(inEvent.index < this.ordersInStorage.length)
        {
            // Retrieve the object from storage
            var retrievedObject = this.ordersInStorage[i];

            this.OrderInstance = retrievedObject;
            
            this.$.orderFieldManager.getOrderFields();
            
            this.setOrderInterface();
            
            this.$.listPanels.setIndex(1);
        }
        else{
            this.OrderInstance = undefined;
            this.$.orderFieldManager.getOrderFields();
            
            this.setOrderInterface();
            
            this.$.listPanels.setIndex(1);
        }
    },
    
    setAddLinesPanel:function(inSender, inEvent){
        this.setOrderLineInterface();
        var i = inEvent.index;
        
        this.OrderInstance = this.ordersInStorage[i];
        
        this.orderLinesArray = this.OrderInstance.OrderLines;
        
        this.$.orderLineSetRepeater.setCount(this.orderLinesArray.length+1);
        
        this.$.listPanels.setIndex(1);
        return true;
    },
    
    // - - - - - - METODI SECONDO PANNELLO ORDINI - - - - - - - - - - //
    setOrderMenuRepeater:function(inSender, inResults) {
  		this.fieldTypes = inResults;
                
                if (this.OrderInstance == undefined){
                    this.OrderInstance = new SalesOrder("", "", "", "", "", "", "", "", "", "", "",new Array(),"");
                    this.prefillHeader();
                }
                else{
                    //modifica ordine
                    this.fillHeaderByOrder(this.OrderInstance);
                }
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
    },
    
    setOrderField: function(inSender, inEvent) {
    	
    	var item = inEvent.item;
    	var i = inEvent.index;
        var field = this.fieldTypes[i];
        if(this.fieldTypes != undefined)
        	item.$.orderField.setContent(this.fieldTypes[i].fieldName);        
                if (this.fieldTypes[i].fieldValue != undefined)
                    item.$.orderFieldValue.setContent(this.fieldTypes[i].fieldValue);
                
        return true;
    },
    
    fillHeaderByOrder: function(order){
        var bpName = this.GetBPNameById(order.BPartnerId);
        
        this.fieldTypes[0].fieldValue = bpName;
        this.fieldTypes[1].fieldValue = this.GetAddrNameById(order.BPartnerAddressId);
        this.fieldTypes[1].extraId = order.BPartnerId;
        this.fieldTypes[2].fieldValue = this.GetTrxNameById(order.TrxDocId);
        this.fieldTypes[3].fieldValue = order.OrderDate;
        this.fieldTypes[4].fieldValue = order.DeliveryDate;
        this.fieldTypes[5].fieldValue = this.GetPaymentTermNameById(order.PaymentTermId);
        this.fieldTypes[6].fieldValue = this.GetInvoiceTermByKey(order.InvoiceTermKey);
        this.fieldTypes[7].fieldValue = this.GetAddrNameById(order.InvoiceAddressId);
        this.fieldTypes[7].extraId = order.BPartnerId;
        this.fieldTypes[8].fieldValue = this.GetWarehouseNameById(order.WarehouseId);
        this.fieldTypes[9].fieldValue = this.GetPriceListById(order.PriceListId);
        this.fieldTypes[10].fieldValue = this.GetPaymentMethodById(order.PaymentMethodId);
        
    },
    
    ResetOrderFields: function(){
        //valori da resettare
        
        this.fieldTypes[0].fieldValue = "";
        this.fieldTypes[1].fieldValue = "";
        this.fieldTypes[1].extraId = "";
        this.fieldTypes[2].fieldValue = "";
        this.fieldTypes[3].fieldValue = "";
        this.fieldTypes[4].fieldValue = "";
        this.fieldTypes[5].fieldValue = "";
        this.fieldTypes[6].fieldValue = "";
        this.fieldTypes[7].fieldValue = "";
        this.fieldTypes[7].extraId = "";
        this.fieldTypes[8].fieldValue = "";
        this.fieldTypes[9].fieldValue = "";
        this.fieldTypes[10].fieldValue = "";
        
    },
    
    GetBPNameById: function(partnerId){
                
        for (index in myBPS)
        {
            if (myBPS[index].bpId == partnerId)
                return myBPS[index].bpName;
        }
        return "";
    },
    
    GetAddrNameById:function(AddressId){
        for (index in myBPAddresses)
        {
            if (myBPAddresses[index].AddressId == AddressId)
                return myBPAddresses[index].Name;
        }
        return "";
    },
    
    GetTrxNameById: function(TrxId){
        for (index in myTrxDocuments)
        {
            if (myTrxDocuments[index].TrxDocId == TrxId)
                return myTrxDocuments[index].Name;
        }
        return "";
        
    },  
    
    GetPaymentTermNameById: function(PaymTermid){
        for (index in myPaymentTerms)
        {
            if (myPaymentTerms[index].termId == PaymTermid)
                return myPaymentTerms[index].Name;
        }
        return "";
    },
    
    GetInvoiceTermByKey: function(termKey){
        for (index in myInvoiceTerms)
        {
            if (myInvoiceTerms[index].Key == termKey)
                return myInvoiceTerms[index].Name;
        }
        return "";
    },
    
    GetWarehouseNameById: function(WarehouseId){
        for (index in myWarehouses)
        {
            if (myWarehouses[index].warehouseId == WarehouseId)
                return myWarehouses[index].Name;
        }
        return "";
    },
    
    getDefaultWarehouse:function(){
        
        for (index in myWarehouses)
        {
            if (myWarehouses[index].isSessionWareHouse)
                return myWarehouses[index];
        }
        
    },
    
    GetPriceListById: function(PriceListId){
        for (index in myPriceLists){
            
            if(myPriceLists[index].ListId == PriceListId)
                return myPriceLists[index].Name;
        }
        return "";
    },
    
    getDefaultPriceList: function(){
        
        for (index in myPriceLists){
            
            if(myPriceLists[index].IsDefault)
                return myPriceLists[index];
        }
        return null;
        
    },
    
    GetPaymentMethodById: function(payMethId){
        
        for (index in myPaymentMethods){
            
            if(myPaymentMethods[index].PaymentMethodId == payMethId)
                return myPaymentMethods[index].Name;
        }
        return "";
        
    },
    
    prefillHeader: function(){
    	
    	/* TRANSACTIONDOC */
    	if(myTrxDocuments.length != 0){
            this.fieldTypes[2].fieldValue = myTrxDocuments[0].Name;
            this.OrderInstance.TrxDocId = myTrxDocuments[0].TrxDocId;
        }
    	/* ORDERDATE & SCHEDULEDATE*/
    	var defaultdate = new Date(this.$.datePicker.value);
        var formatDefault = defaultdate.getDate() +"/"+(defaultdate.getMonth() + 1)+"/"+defaultdate.getFullYear();
        this.fieldTypes[3].fieldValue = formatDefault;
    	this.fieldTypes[4].fieldValue = formatDefault;
    	this.OrderInstance.OrderDate = formatDefault;
        this.OrderInstance.DeliveryDate = formatDefault;
        
    	/* PAYMENTTERMS */
    	if(myPaymentTerms.length != 0){
            this.fieldTypes[5].fieldValue = myPaymentTerms[0].Name;
            this.OrderInstance.PaymentTermId = myPaymentTerms[0].termId;
        }
        
    	/* INVOICETERMS */
    	if(myInvoiceTerms.length != 0){
            this.fieldTypes[6].fieldValue = myInvoiceTerms[0].Name;
            this.OrderInstance.InvoiceTermKey = myInvoiceTerms[0].Key;    
        }
    	/* WAREHOUSE */
    	if(myWarehouses.length != 0){
            var defWarehouse = this.getDefaultWarehouse();
            
            if(defWarehouse != undefined){
                this.fieldTypes[8].fieldValue = defWarehouse.Name;
                this.OrderInstance.WarehouseId = defWarehouse.warehouseId;
            }
            
            else{
                this.fieldTypes[8].fieldValue = myWarehouses[0].Name;
                this.OrderInstance.WarehouseId = myWarehouses[0].warehouseId;    
            }    
        }
        /* PRICE LIST */
        if(myPriceLists.length != 0){
            var defPList = this.getDefaultPriceList();
            if(defPList != null){
                this.fieldTypes[9].fieldValue = defPList.Name;
                this.OrderInstance.PriceListId = defPList.ListId;
            }
        }
        
    },
    
    fieldDispatch: function(inSender, inEvent) {
        
        this.selectedField = this.fieldTypes[inEvent.index];
        this.selectedFieldType = this.selectedField.fieldEnum;
            	
        this.$.descrHeader.setContent(this.selectedField.fieldName); //Settaggio titolo di testata del secondo pannello
        
        switch(OrderHeaderEnum[this.selectedFieldType])
        {
            case 0 /* BUSINESSPARTNER */:
                this.setOrderRepetaerInterface();
                this.setBusinessPartnersPanel();
                this.$.listPanels.setIndex(2);
                break;
            case 1 /* PARTNERADDRESS */:
                if(this.fieldTypes[1].extraId.length>0){
                    this.setOrderRepetaerInterface(); 
                    this.setBPartnerAddressPanel(this.fieldTypes[1].extraId);
                    this.$.listPanels.setIndex(2);
                }    
                break;
            
            case 2 /* TRANSACTIONDOC */:
                this.setOrderRepetaerInterface(); 
                this.setTrxDocumentPanel();
                this.$.listPanels.setIndex(2);
                break;
            
            case 3 /* ORDERDATE */:
                this.setDateBoxInterface();
                this.$.listPanels.setIndex(2);
                break;
            
            case 4 /* SCHEDULEDELDATE */:
                this.setDateBoxInterface();
                this.$.listPanels.setIndex(2);
                break;
            
            case 5 /* PAYMENTTERMS */:
                this.setOrderRepetaerInterface();
                this.setPaymentTermPanel();
                this.$.listPanels.setIndex(2);
                break;
            
            case 6 /* INVOICETERMS */:
                this.setOrderRepetaerInterface(); 
                this.setInvoiceTermPanel();
                this.$.listPanels.setIndex(2);
                break;
            
            case 7 /* INVOICEADDRESS */:
                if(this.fieldTypes[1].extraId.length>0){
                    this.setOrderRepetaerInterface();
                    this.setBPartnerInvoiceAddressPanel(this.fieldTypes[1].extraId);
                    this.$.listPanels.setIndex(2);
                }    
                break;
            
            case 8 /* WAREHOUSE */:
                this.setOrderRepetaerInterface();
                this.setWarehousePanel();
                this.$.listPanels.setIndex(2);
                break;
                
            case 9 /* PRICE LIST */: 
                this.setOrderRepetaerInterface();
                this.setPriceListPanel();
                this.$.listPanels.setIndex(2);
                break;
                
            case 10 /* PAYMENT METHOD */: 
                this.setOrderRepetaerInterface();
                this.setPaymentMethodPanel();
                this.$.listPanels.setIndex(2);
                break;
         }

        
    },
    
    setDateBoxInterface: function(){
        this.$.dateBox.show();
    
        this.$.descrRepeater.hide();
        this.$.OrderLineFieldRepeater.hide();
    },
    setOrderRepetaerInterface: function(){
        this.$.descrRepeater.show();
        
        this.$.OrderLineFieldRepeater.hide();
        this.$.dateBox.hide();
    },
    setOrderLineInterface: function(){
        this.$.OrderLineFieldRepeater.show();
        this.$.orderLineSetRepeater.show();
        this.$.saveLineButton.show();
        
        this.$.orderMenuRepeater.hide();
        this.$.saveButton.hide();
        this.$.descrRepeater.hide();
        this.$.dateBox.hide();
        
    },
    setOrderInterface: function(){
        this.$.OrderLineFieldRepeater.show();
        this.$.descrRepeater.show();
        this.$.orderMenuRepeater.show();
        this.$.saveButton.show();
        
        this.$.dateBox.hide();
        this.$.orderLineSetRepeater.hide();
        this.$.saveLineButton.hide();
    },
    
    // - - - - - - - METODI SECONDO PANNELLO LINEE ORDINI - - - - 
    removeOrderLinePopupStart: function(inSender, inEvent){
        var p = this.$[inSender.popup];
        var i = inEvent.index;
        
        var orderLineInd = this.orderLinesArray[i].OrderLineIndex;
        
        this.OrderLineToRemove = this.returnOrdeLineArrayIndex(orderLineInd);;
        
        if (p) {
            this.$.removeLineText.setContent("Remove Orderline "+this.orderLinesArray[i].ProductName+"?");
            p.show();            
        }
        return true;
    },
    hideOrderLineRemovePopup: function(){
         this.$.orderLineRemovePopup.hide();
    },
    removeOrderLine: function(){
        this.orderLinesArray.splice(this.OrderLineToRemove,1);
        this.$.orderLineSetRepeater.setCount(this.orderLinesArray.length+1);
        this.$.orderLineRemovePopup.hide();
    },
    
    setOrderLineSet: function(inSender, inEvent){
        
        var item = inEvent.item;
    	var i = inEvent.index;
        if(this.orderLinesArray != undefined && i != this.orderLinesArray.length){
            var field = this.orderLinesArray[i];
            //da settare nome prodotto linea inserito
            item.$.orderLineInstance.setContent(this.orderLinesArray[i].ProductName);
            item.$.orderLineInstanceValue.setContent(this.orderLinesArray[i].Quantity);
            item.$.removeLinesButton.show();
        }
        else{
            item.$.orderLineInstance.setContent("Insert Line");
            item.$.orderLineInstanceValue.setContent("New line");
            item.$.removeLinesButton.hide();
        }         
        
        return true;
        
    },
    
    OrderLineInstanceInsert: function(inSender,inEvent){
        var i = inEvent.index;
        this.orderLineTypes = myOrderLineFields;
        
        if(inEvent.index < this.orderLinesArray.length)
        {
            this.SelectedOrderLine = this.orderLinesArray[i];
            this.FillByOrderLine(this.SelectedOrderLine);
            
        }
        else{
            //nuova linea ordine
            this.SelectedOrderLine = new SalesOrderLine("","","","","");
            this.ResetOrderLine();
            
        }
        //repeater linea Ordine chiamata a set count
        this.$.OrderLineFieldRepeater.setCount(this.orderLineTypes.length);
        this.$.listPanels.setIndex(2);
    },
    
    FillByOrderLine: function(orderline){
        //valori da inizializzare
        this.orderLineTypes[0].fieldId = orderline.ProductId; 
        this.orderLineTypes[0].fieldValue = orderline.ProductName;
        this.orderLineTypes[1].fieldValue = orderline.Quantity;
        this.orderLineTypes[2].fieldValue = orderline.UnitPrice;
        
    },
    
    ResetOrderLine: function(){
        //valori da resettare
        //valori da inizializzare
        this.orderLineTypes[0].fieldId = ""; 
        this.orderLineTypes[0].fieldValue = "";
        this.orderLineTypes[1].fieldValue = "";
        this.orderLineTypes[2].fieldValue = "";
        
    },
    
    
    // - - - - - - - METODI TERZO PANNELLO LINEE ORDINI - - - - -
    
    setOrderLineField: function(inSender,inEvent){
        
        var item = inEvent.item;
    	var i = inEvent.index;
        var field = this.orderLineTypes[i];
        if(this.orderLineTypes != undefined)
        	item.$.orderLineField.setContent(this.orderLineTypes[i].fieldName);        
                if (this.orderLineTypes[i].fieldValue != undefined)
                    item.$.orderLineFieldValue.setContent(this.orderLineTypes[i].fieldValue);
                
        return true;
     },
    
    OrderLineFieldDispatch: function(inSender,inEvent){
        
        this.selectedOrderLineField = this.orderLineTypes[inEvent.index];
        this.selectedOrderLineFieldType = this.selectedOrderLineField.fieldEnum;
        
        switch(OrderLineEnum[this.selectedOrderLineFieldType])
        {
            case 0 /* PRODUCT */:
                this.setProductsPanel();
                this.$.OrderLineValueRepeater.show();
                this.$.numberInbutBox.hide();
                this.$.numberInputSave.hide();
                
                this.$.listPanels.setIndex(3);
                break;
            case 1 /* QUANTITY */:
                
                this.$.OrderLineValueRepeater.hide();
                this.$.numberInbutBox.show();
                this.$.numberInputSave.show();
                
                if(this.orderLineTypes[1].fieldValue.length!=0)
                    this.$.numberInput.setValue(this.orderLineTypes[1].fieldValue);
                else
                    this.$.numberInput.setValue(0);
                
                this.$.listPanels.setIndex(3);
                break;
            case 2 /* UNIT PRICE */:
                this.$.OrderLineValueRepeater.hide();
                this.$.numberInbutBox.show();
                this.$.numberInputSave.show();
                this.$.numberInput.setValue(this.orderLineTypes[2].fieldValue);
                this.$.listPanels.setIndex(3);
                break;
        }
        
        
    },
    
    // - - - - - - METODI QUARTO PANNELLO LINEE ORDINE - - - - - -
    
    // Setting 3rd Panel as the Product Selection Panel
    setProductsPanel: function(){
        this.lineDescrElements = myProducts;
        this.$.OrderLineValueRepeater.setCount(this.lineDescrElements.length);
    },
    
    setOrderLineValues: function(inSender,inEvent){
        var item = inEvent.item;
    	var i = inEvent.index;
        var field = this.lineDescrElements[i];
        if(this.lineDescrElements != undefined)
        	item.$.orderLineValue.setContact(field,OrderLineEnum[this.selectedOrderLineFieldType]);        
    
        return true;
    },
    
    lineDispatch: function(inSender,inEvent){
        
        var selectedElem;
        
        switch(OrderLineEnum[this.selectedOrderLineFieldType])
        {
            case 0 /* PRODUCTS */:
                selectedElem = this.lineDescrElements[inEvent.index];
                this.orderLineTypes[0].fieldValue = selectedElem.ProductName;
                this.SelectedOrderLine.ProductId = selectedElem.ProductId;
                this.SelectedOrderLine.ProductName = selectedElem.ProductName;
                
                //prefill Unit Price with Product List Price, orelse with standard price
                var listPrice = this.getProductPriceByList(selectedElem,this.OrderInstance.PriceListId);
                this.SelectedOrderLine.UnitPrice = listPrice;
                this.orderLineTypes[2].fieldValue = listPrice;
                
                //prefill Quantity field
                this.SelectedOrderLine.Quantity = 1;
                this.orderLineTypes[1].fieldValue = 1;
                
                break;
            case 1 /* QUANTITY */:
                this.orderLineTypes[1].fieldValue = this.$.numberInput.getValue();
                
                this.SelectedOrderLine.Quantity = this.$.numberInput.getValue();
                
                this.$.numberInput.setValue("");
                break;
            case 2 /* UNIT PRICE */:
                this.orderLineTypes[2].fieldValue = this.$.numberInput.getValue();
                
                this.SelectedOrderLine.UnitPrice = this.$.numberInput.getValue();
                
                this.$.numberInput.setValue("");
                break;
        }     
      this.$.OrderLineFieldRepeater.setCount(this.orderLineTypes.length);  
      this.$.listPanels.setIndex(2);
    },
    
    getProductPriceByList: function(prod, selectedPriceListId){
        
        for (index in prod.PListArray){
            
            if(prod.PListArray[index].Id == selectedPriceListId)
                return prod.PListArray[index].ListPrice;
        }
        
        return prod.StandardPrice;
        
    },
    
    // - - - - - - - METODI TERZO PANNELLO ORDINI - - - - - - - -
    
    // Setting 3rd Panel as the Business Partner Selection Panel
    setBusinessPartnersPanel : function() {
  		this.descrElements = myBPS;
		this.$.descrRepeater.setCount(this.descrElements.length);
    },
    
    
    // Setting the 3rd Panel as the Business Partner Address Selection Panel
    setBPartnerAddressPanel: function(partnerId) {
        
        this.descrElements = this.GetAddressesById(partnerId);
        this.$.descrRepeater.setCount(this.descrElements.length);
    },
    
    // Setting the 3rd Panel as the Invoice Address Selection Panel
    setBPartnerInvoiceAddressPanel: function(partnerId) {
        
        this.descrElements = this.GetAddressesById(partnerId);
        this.$.descrRepeater.setCount(this.descrElements.length);
    },
    
    GetAddressesById: function(partnerId){
        
        var Addresses = new Array();
        
        for (addr in myBPAddresses)
        {
            if (myBPAddresses[addr].BPartnerId == partnerId)
                Addresses.push(myBPAddresses[addr]);
        }
        return Addresses;
    },
    
    // Setting the 3rd Panel as the Document Transaction Selection Panel
    
    setTrxDocumentPanel: function(){
      
      this.descrElements = myTrxDocuments;
      this.$.descrRepeater.setCount(this.descrElements.length);
    },
    
    // Setting the 3rd Panel as the Payment Term Selection Panel
    
    setPaymentTermPanel: function(){
        
        this.descrElements = myPaymentTerms;
        this.$.descrRepeater.setCount(this.descrElements.length);
        
    },
    
    // Setting the 3rd Panel as the Invoice Term Selection Panel
    setInvoiceTermPanel: function(){
        
        this.descrElements = myInvoiceTerms;
        this.$.descrRepeater.setCount(this.descrElements.length);
    },
    
    // Setting the 3rd Panel as the Warehouse Selection Panel
    setWarehousePanel: function(){
        this.descrElements = myWarehouses;
        this.$.descrRepeater.setCount(this.descrElements.length);
    },
    
    setPriceListPanel: function(){
        this.descrElements = myPriceLists;
        this.$.descrRepeater.setCount(this.descrElements.length);
    },

    getPriceListNameById: function(pListId){
        
        for (index in myPriceLists)
        {
            if (myPriceLists[index].ListId == pListId)
                return myPriceLists[index].Name;
        }
        return "";
    },
    
    setPaymentMethodPanel: function(){
        
        this.descrElements = myPaymentMethods;
        this.$.descrRepeater.setCount(this.descrElements.length);
        
    },
    
    setDescrItem: function(inSender, inEvent) {
    	
    	var item = inEvent.item;
    	var i = inEvent.index;
        var field = this.descrElements[i];
        if(this.descrElements != undefined)
        	item.$.TapItem.setContact(field,OrderHeaderEnum[this.selectedFieldType]);        
    
        return true;
    },
    
    descrDispatch: function(inSender, inEvent) {
        
        var selectedDescr; 
        
        switch(OrderHeaderEnum[this.selectedFieldType])
        {
            case 0 /* BUSINESSPARTNER */:
                selectedDescr = this.descrElements[inEvent.index];
                this.fieldTypes[0].fieldValue = selectedDescr.bpName;
                
                //setting value of bpartner location in INVOICE and PARTNER address
                this.fieldTypes[1].fieldValue = selectedDescr.bpLocation;
                this.fieldTypes[7].fieldValue = selectedDescr.bpLocation;
                
                //setting id of bpartner location in INVOICE and PARTNER address
                this.fieldTypes[1].extraId = selectedDescr.bpId;
                this.fieldTypes[7].extraId = selectedDescr.bpId;
                
                //setting value of bpartner pricelist
                if(selectedDescr.PriceListId.length != 0)
                {   
                    var name = this.getPriceListNameById(selectedDescr.PriceListId);
                    if(name.length>0){
                        this.fieldTypes[9].fieldValue = name;
                        this.OrderInstance.PriceListId = selectedDescr.PriceListId;
                    }
                }
                
                //setting value of bpartner payment term
                if(selectedDescr.PaymTermsId.length != 0)
                {   
                    name = this.GetPaymentTermNameById(selectedDescr.PaymTermsId);
                    if(name.length>0){
                        this.fieldTypes[5].fieldValue = name;
                        this.OrderInstance.PaymTermsId = selectedDescr.PaymTermsId;
                    }
                }
                
                //setting value of bpartner invoice term
                if(selectedDescr.invoiceTermKey.length != 0)
                {   
                    name = this.GetInvoiceTermByKey(selectedDescr.invoiceTermKey);
                    if(name.length>0){
                        this.fieldTypes[6].fieldValue = name;
                        this.OrderInstance.InvoiceTermKey = selectedDescr.invoiceTermKey;
                    }
                }
                
                //setting value of bpartner payment method
                if(selectedDescr.PaymentMethodId.length != 0)
                {
                    name = this.GetPaymentMethodById(selectedDescr.PaymentMethodId);
                    if(name.length>0){
                        this.fieldTypes[10].fieldValue = name;
                        this.OrderInstance.PaymentMethodId = selectedDescr.PaymentMethodId;
                    }
                    
                }
                
                
                this.OrderInstance.BPartnerId = selectedDescr.bpId;
                this.OrderInstance.BPartnerAddressId = selectedDescr.bpLocationId;
                this.OrderInstance.InvoiceAddressId = selectedDescr.bpLocationId;
                
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            case 1 /* PARTNERADDRESS */:
                selectedDescr = this.descrElements[inEvent.index];
                this.fieldTypes[1].fieldValue = selectedDescr.Name;
                this.OrderInstance.BPartnerAddressId = selectedDescr.AddressId;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            
            case 2 /* TRANSACTIONDOC */:
                selectedDescr = this.descrElements[inEvent.index];
                this.fieldTypes[2].fieldValue = selectedDescr.Name;
                this.OrderInstance.TrxDocId = selectedDescr.TrxDocId;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            
            case 3 /* ORDERDATE */:
                var d1 = new Date(this.$.datePicker.value);
                var format1 = d1.getDate() +"/"+(d1.getMonth() + 1)+"/"+d1.getFullYear();
                this.fieldTypes[3].fieldValue = format1;
                this.OrderInstance.OrderDate = format1;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            
            case 4 /* SCHEDULEDELDATE */:
                var d2 = new Date(this.$.datePicker.value);
                var format2 = d2.getDate() +"/"+(d2.getMonth() + 1)+"/"+d2.getFullYear();
                this.fieldTypes[4].fieldValue = format2;
                this.OrderInstance.DeliveryDate = format2;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            
            case 5 /* PAYMENTTERMS */:
                selectedDescr = this.descrElements[inEvent.index];
                this.fieldTypes[5].fieldValue = selectedDescr.Name;
                this.OrderInstance.PaymentTermId = selectedDescr.termId;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            
            case 6 /* INVOICETERMS */:
                selectedDescr = this.descrElements[inEvent.index];
                this.fieldTypes[6].fieldValue = selectedDescr.Name;
                this.OrderInstance.InvoiceTermKey = selectedDescr.Key ;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            
            case 7 /* INVOICEADDRESS */:
                selectedDescr = this.descrElements[inEvent.index];
                this.fieldTypes[7].fieldValue = selectedDescr.Name;
                this.OrderInstance.InvoiceAddressId = selectedDescr.AddressId;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            
            case 8 /* WAREHOUSE */:
                selectedDescr = this.descrElements[inEvent.index];
                this.fieldTypes[8].fieldValue = selectedDescr.Name;
                this.OrderInstance.WarehouseId = selectedDescr.warehouseId;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            case 9 /* PRICE LIST*/:
                selectedDescr = this.descrElements[inEvent.index];
                this.fieldTypes[9].fieldValue = selectedDescr.Name;
                this.OrderInstance.PriceListId = selectedDescr.ListId;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
            case 10 /* PAYMENT METHOD*/:    
                selectedDescr = this.descrElements[inEvent.index];
                this.fieldTypes[10].fieldValue = selectedDescr.Name;
                this.OrderInstance.PaymentMethodId = selectedDescr.PaymentMethodId;
                this.$.orderMenuRepeater.setCount(this.fieldTypes.length);
                break;
        }
        
        this.$.listPanels.setIndex(1);
    },
    
    //  - - - LOCAL STORAGE MANAGEMENT - - - - //
    
    showOrderHeader: function(){
      if(localStorage.getItem('testObject')!=undefined) 
            alert('there are ' + localStorage.length + ' items in the storage array.\n testobject:'+ localStorage.getItem('testObject'));
      else
            alert('there are ' + localStorage.length + ' items in the storage array');
     
    },
    
    saveOrderHeader: function(inSender){
    	var found = false;
    	var p = this.$[inSender.popup];
    	
    	for(var i=0; i<10 ; i++){
    		if (this.fieldTypes[i].fieldValue == undefined || this.fieldTypes[i].fieldValue.length ==0)
    			{
    				if (p) {
                                                this.Undone=true;
                                                p.setContent("Please fill out each field");
						p.show();
                                                setInterval(function(){p.hide()},800);
                                                
					}
					found=true;
					break;
    			}
    	}
        // Put the object into storage
        if(!found){
                if(this.OrderInstance.OrderIndex == undefined || this.OrderInstance.OrderIndex.length==0){
                    var dateString = new Date().getTime().toString();
                    var newOrderCode = dateString.substring(0,2)+"."+dateString.substring(2,6)+"."+dateString.substring(6,11);
                    
                    var strKeyNuovoOrdine = "ORD-"+newOrderCode;
                    this.OrderInstance.OrderIndex = strKeyNuovoOrdine;
                }
                
                localStorage.setItem(this.OrderInstance.OrderIndex, JSON.stringify(this.OrderInstance));
                this.getOrdersInStorage();
    		if (p) {                        
                                                this.Undone=false;
						p.setContent("Order saved!");
						p.show();
                                                setInterval(function(){p.hide()},800);
                                                
					}
    	}
    },
    
    removeOrderHeader: function(){
        // remove object
        
        //if(this.OrderInstance.OrderIndex != undefined && this.OrderInstance.OrderIndex.length > 0)
          //  localStorage.removeItem(this.OrderInstance.OrderIndex);
        
        localStorage.clear();
    },
    setOrderInstancePanel:function(){
    if(!this.Undone){
            this.$.orderSetRepeater.setCount(this.ordersInStorage.length+1);
            this.ResetOrderFields();
            this.$.listPanels.setIndex(0);
        }
    },
    
    setOrderLineInstancePanel: function(){
    if(!this.Undone){
            this.$.orderLineSetRepeater.setCount(this.orderLinesArray.length+1);
            this.ResetOrderLine();
            this.$.listPanels.setIndex(1);
        }
    },
    
    saveOrderLine:function(inSender){
        var found = false;
    	var p = this.$[inSender.popup];
        
        for(var i=0; i<3 ; i++){
            if (this.orderLineTypes[i].fieldValue == undefined || this.orderLineTypes[i].fieldValue.length ==0)
                {
                    if (p) {
                        this.Undone=true;
                        p.setContent("Please fill out each field");
			p.show();
                        setInterval(function(){p.hide()},800);
                        
                    }
                    found=true;
                    break;
                }
        }
        
        if(!found){
            
            var index = this.returnOrdeLineArrayIndex(this.SelectedOrderLine.OrderLineIndex);
            
            if( index != null)
                this.orderLinesArray[index] = this.SelectedOrderLine;
            else{
                //this.SelectedOrderLine.OrderLineIndex = new Date().getTime(); //FIXME
                this.SelectedOrderLine.NetPrice = 0;  //FIXME
                this.SelectedOrderLine.ListPrice = 0; //FIXME
                this.SelectedOrderLine.Discount = 0; //FIXME
                this.SelectedOrderLine.DiscountAmt = 0; //FIXME
                if (this.orderLinesArray == ""){
                    this.orderLinesArray = new Array();
		}
                this.orderLinesArray[this.orderLinesArray.length] = this.SelectedOrderLine;
            }
            this.OrderInstance.OrderLines = this.orderLinesArray;
            
            localStorage.setItem(this.OrderInstance.OrderIndex, JSON.stringify(this.OrderInstance));
            this.getOrdersInStorage();
            
            if (p) {
                this.Undone=false;
                p.setContent("Orderline saved!");
		p.show();
                setInterval(function(){p.hide()},800);
                
            }
        }
        
    },
    returnOrdeLineArrayIndex: function(extId){
        for (index in this.orderLinesArray)
        {
            if (this.orderLinesArray[index].OrderLineIndex == extId)
                return index;
        }
        return null;
    },

    redirectToMenu:function(){
    	window.location.href="../it.openia.crm/";
    }  
        
});

// Component per la Search.
enyo.kind({
	name: "orderFieldManager",
	kind: "Component",
	events: {
		onOrderTypes:""
		
	},
        getOrderFields: function() {
            
            this.doOrderTypes(HeaderFields);
            
        }
        
});

// Componente che rappresenta l'ordine.
enyo.kind({
    
        name: "ItemToTap",
	
	components: [
		{name: "name", classes: "row_title"},
		{name: "name2", classes: "list-sample-contacts-description"}
	],
        
	setContact: function(inContact, inFieldEnum) {
            
            
		switch(inFieldEnum)
                    {
                        case 0 /* BUSINESSPARTNER */:
                            this.$.name.setContent(inContact.bpName);
                            this.$.name2.setContent(inContact.bpLocation);
                            break;
                        case 1 /* PARTNERADDRESS */:
                            this.$.name.setContent(inContact.Name);
                            break;

                        case 2 /* TRANSACTIONDOC */:
                            this.$.name.setContent(inContact.Name);
                            break;

                        case 3 /* ORDERDATE */:
                            break;

                        case 4 /* SCHEDULEDELDATE */:
                            break;

                        case 5 /* PAYMENTTERMS */:
                            this.$.name.setContent(inContact.Name);
                            break;

                        case 6 /* INVOICETERMS */:
                            this.$.name.setContent(inContact.Name);
                            break;

                        case 7 /* INVOICEADDRESS */:
                            this.$.name.setContent(inContact.Name);
                            break;

                        case 8 /* WAREHOUSE */:
                            this.$.name.setContent(inContact.Name);
                            break;
                        
                        case 9 /* PRICELIST */:
                            this.$.name.setContent(inContact.Name);
                            break
                        
                        case 10/* PAYMENT METHOD */:
                            this.$.name.setContent(inContact.Name);
                            break
                    }    
        
        }
        
});

// Componente che rappresenta l'ordine.
enyo.kind({
    
        name: "OrderLineToTap",
	
	components: [
		{name: "name", classes: "row_title"},
		{name: "name2", classes: "list-sample-contacts-description"}
	],

        setContact: function(inContact, inFieldEnum) {
            
            
		switch(inFieldEnum)
                    {
                        case 0 /* PRODUCT */:
                            this.$.name.setContent(inContact.ProductName);
                            break;
                        case 1 /* QUANTITY */:
                            break;
                        case 2 /* UNIT PRICE */: 
                            break;
                    }
        }
}); 