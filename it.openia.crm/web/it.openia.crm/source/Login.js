/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

enyo.kind(
    {name: "OpeniaLogoSmall", style: "background-color: rgb(234, 245, 214);", components: [
            {tag: "img", 
             style: "margin-left: auto; margin-right: 1px; display: block;", 
             attributes: {src : "../web/it.openia.crm/images/Logo_big_openiaCRM.png"}}       
]});

enyo.kind({
    name:"LoginPanel",    
    components: [
        {name: "mainBox", components: [           
            {kind: "onyx.Groupbox", classes: "groupbox", components: [
                {components: [                        
                    {kind: "OpeniaLogoSmall"}
                ]},
                {kind: "onyx.GroupboxHeader", classes: "groupboxHeader", components: [                        
                        {content: "Openia CRM"}                        
                ]},
                {name: "username", components:[
                    {kind: "onyx.Input", classes: "textbox", placeholder: "Username"}        
                ]},
                {name: "password", components:[
                    {kind: "onyx.Input", classes: "textbox", type: "password", placeholder: "Password"}
                ]},
                {name: "submit", style: "background-color: rgb(234, 245, 214);", classes: "btnsubmit", components:[
                    {kind:"onyx.Button", content: "Login", ontap:"loginBtn"}
                ]}
            ]}      
        ]}
    ],
    loginBtn: function() {
        document.body.innerHTML += '<form id="dynForm" action="?" method="post"><input type="hidden" name="l" value="' + $("#login_loginPanel_input").val() + '"><input type="hidden" name="p" value="' + $("#login_loginPanel_input2").val() + '"></form>';
        document.getElementById("dynForm").submit();
    }
});

enyo.kind({
    name: "Login",
    components: [
        {kind: "LoginPanel"}
    ]
});