/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

enyo.kind({
    name:"MenuPanel",    
    components: [
        {name: "mainBox", components: [           
            {kind: "onyx.Groupbox", classes: "groupbox", components: [
                {components: [                        
                    {kind: "OpeniaLogoSmall"}
                ]},
                {kind: "onyx.GroupboxHeader", classes: "groupboxHeader", components: [                        
                        {content: "Openia CRM"}                        
                ]},
                {name: "lookup", ontap: "lookup", style: "padding: 10px;", components:[
                    {content: "Fast Lookup"}        
                ]},
                {name: "calendar", ontap: "calendar", style: "padding: 10px;", components:[
                    {content: "Calendar"}
                ]},
                {name: "smartorders", ontap: "smartorders", style: "padding: 10px;", components:[
                    {content: "Smart Orders"}
                ]},
                {name: "submit", style: "background-color: rgb(234, 245, 214);", classes: "logout", components:[
                    {kind:"onyx.Button", content: "Logout", ontap:"logoutBtn"}
                ]}

            ]}      
        ]}
    ],
    lookup: function() {
        document.body.innerHTML += '<form id="dynForm" action="../it.openia.crm.ad_forms/CRMLookup.html" method="post"></form>';
        document.getElementById("dynForm").submit();
    },
    calendar: function() {
        document.body.innerHTML += '<form id="dynForm" action="../it.openia.crm.ad_forms/CRMCalendar.html" method="post"></form>';
        document.getElementById("dynForm").submit();
    },
    smartorders: function() {
        document.body.innerHTML += '<form id="dynForm" action="../it.openia.crm.ad_forms/OrdersInsert.html" method="post"></form>';
        document.getElementById("dynForm").submit();
    },
    logoutBtn: function() {
        document.body.innerHTML += '<form id="dynForm" action="../it.openia.crm/?logout" method="post"></form>';
        document.getElementById("dynForm").submit();
    }
});

enyo.kind({
    name: "MobileMenu",
    components: [
        {kind: "MenuPanel"}    
    ]
});
