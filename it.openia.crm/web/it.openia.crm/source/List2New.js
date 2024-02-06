/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

//PATCH PER MALFUNZINOAMENTO DI REPEATER ANNIDATI 
// ( trovata sul commento di "theryanjduffy" in http://forums.enyojs.com/discussion/1423/enyo-repeater-inside-repeater 
//  copiata e incollata dal jsfiddle http://jsfiddle.net/7sB6K/2/ )

enyo.kind({
	name: "enyo.OwnerProxy",
	tag: null,
	decorateEvent: function(inEventName, inEvent, inSender) {
        //if (inEvent) { // this is the broken line
        if (inEvent && !("index" in inEvent)) {
			inEvent.index = this.index;
		}
		this.inherited(arguments);
	},
	delegateEvent: function(inDelegate, inName, inEventName, inEvent, inSender) {
		if (inDelegate == this) {
			inDelegate = this.owner.owner;
		}
		return this.inherited(arguments, [inDelegate, inName, inEventName, inEvent, inSender]);
	}
});


enyo.kind({
    name: "App",
    classes: "list-sample-contacts enyo-fit",
    fit: true,
    handlers:{
        onRefreshActs:"refreshActivities"
    },
    components:[
        {kind:"Panels", classes:"enyo-fit", arrangerKind:"CollapsingArranger", components: [
            {kind:"Panels", name:"listPanels", classes:"onyx todos-panels", arrangerKind:"CarouselArranger", components: [
            
                //Menu
                {kind:"FittableRows", components: [
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {name:"menuHeader", content:"Menu", fit:true}
                    ]},
                    {kind:"Scroller", ondragstart:"noDrag", fit:true, classes:"todos-list", components: [
                        {name:"menuRepeater", kind:"Repeater", onSetupItem: "setMenuRep", count:"", components: [
                            {kind:"onyx.Item", classes:"todos-list-item", ontap:"selectMenuItem", components: [
                                {name:"menuType",content:"", classes:"todo-name"}
                            ]} 
                        ]}
                    ]},
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", attributes:{"data-role":"none"},components: [
                        {kind:"onyx.Grabber"},
                        {name:"logoutButton",kind:"onyx.Button", content:"Menu", ontap:"redirectToMenu", attributes:{"data-role":"none"}},
                        {fit:true}
                    ]}
                ]},
                
                //Opportunity and Activities
                {kind:"FittableRows", components: [
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {name:"descrHeader", content:"Description"},
                        {name:"newActivityButton",kind:"onyx.Button", content:"New", ontap:"newActPopupCall", attributes:{"data-role":"none"}},
                        {name:"nextSevenDays",kind:"onyx.Button", content:"+7", ontap:"GetNextSevenDays", attributes:{"data-role":"none"}},
                        {name:"prevSevenDays",kind:"onyx.Button", content:"-7", ontap:"GetPrevSevenDays", attributes:{"data-role":"none"}}
                    ]},
                    {kind:"Scroller", ondragstart:"noDrag", fit:true, classes:"todos-list", components: [
                        
                        {name:"descrRepeater", kind:"Repeater", onSetupItem: "setDescr", count:"0", components: [
                                 {name: "typeDescr", kind: "detailItem", classes: "todos-list-item list-sample-contacts-item enyo-border-box"},
                        ]},
                    	
                    	{name:"descrDailyRepeater", kind:"Repeater", onSetupItem: "setDayDescr", count:"0", components: [
                            
                            {name: "dayHeader", style:"height:40px;height: 40px;background-color: gainsboro;",
                                    classes: "todos-list-item list-sample-contacts-item enyo-border-box"}, 
                        
                            {name:"descrActivityRepeater", kind:"Repeater", onSetupItem: "setActivityDescr", count:"0", components: [
                                 
                                {name: "actDescr", kind: "activityItem", ontap:"changeActView", style:"height:111px;",
                                    classes: "todos-list-item list-sample-contacts-item enyo-border-box"},
                        
                            ]},
                        ]},
                    
                        {name:"SpinnerPanelOne", style:"background:white; border-radius:5px; padding:15px; ", components: [
                            {kind: "onyx.Spinner", style:"background: url(../web/it.openia.crm/images/spinner-light.gif) no-repeat 0 0;", classes: "onyx-light"}
			]}   
                        
                    
                    ]},
                    
                    //popup per nuova attivita'
                    {name: "newActivityPopup", kind: "onyx.Popup", style:"font-size: 20px;padding: 12px;background: rgba(76, 76, 76, 0.82)",autoDismiss: false ,centered: true, floating: true, modal: true, 
                    	components:[
                        	{tag:"center",style:"height: 100%;width: 100%;", components:[
                            	{kind:"FittableRows", components: [
                                	//oggetto attivita'
                                	{kind:"FittableColumns", components: [
                                    		//{content:"oggetto : ",style:"width: 40%;"},
                                            {name:"subjectInput", style:"height: 25px;font-size: 13px;", kind: "onyx.Input", fit:true}
                                        ]},
                                        {tag:"div", style:"height:10px;"},
                                        //data
                                        {kind:"FittableColumns", components: [
                                        	{kind: "onyx.PickerDecorator", components: [
												{style:"padding: 6px 18px;width: 65px;font-size: 13px;"},
												{name: "monthPicker", kind: "onyx.Picker"}
											]},
											{kind: "onyx.PickerDecorator",components: [
												{style:"padding: 6px 18px;width: 65px;font-size: 13px;"},
												{name: "dayPicker", kind: "onyx.Picker"}
											]},
											{kind: "onyx.PickerDecorator", components: [
												{name:"yearPickerButton", style:"padding: 6px 18px;width: 72px;font-size: 13px;"},
												{name: "yearPicker", kind: "onyx.FlyweightPicker", count: 200, onSetupItem: "setupYear", components: [
													{name: "year"}
												]}
											]}
                                        ]},
                                        {tag:"div", style:"height:10px;"},
                                        //ora
                                        {kind:"FittableColumns", components: [
                                        	{kind: "onyx.PickerDecorator", components: [
												{style:"padding: 6px 18px;width: 65px;font-size: 13px;"},
												{name: "hourPicker", kind: "onyx.Picker"}
											]},
											{kind: "onyx.PickerDecorator",components: [
												{style:"padding: 6px 18px;width: 65px;font-size: 13px;"},
												{name: "minutePicker", kind: "onyx.Picker"}
											]},
											//durata
											{tag:"div", style:"width:10%",components:[
                                        		{kind: "enyo.Image", style:"width:100%;margin-left: 15%;",src:'../web/it.openia.crm/images/time.png'},
                                            ]},
                                            {tag:"div", style:"width:5px;"},
                                            {kind: "onyx.PickerDecorator",components: [
												{style:"padding: 6px 18px;width: 45px;font-size: 13px;"},
												{name: "durationPicker", kind: "onyx.Picker"}
											]},
                                        ]},
                                        {tag:"div", style:"height:10px;"},
                                        //tipo
                                        {kind:"FittableRows", components: [
                                        	{name:"actTypeText",content:"tipo : ",style:""},
                                        	{tag:"div", style:"height:10px;"},
                                            {kind: "onyx.PickerDecorator",components: [
												{style:"padding: 6px 18px;width: 121px;font-size: 13px;"},
												{name: "actTypePicker", kind: "onyx.Picker"}
											]},
                                        ]},
                                        {tag:"div", style:"height:10px;"},
                                        //lead
                                        {kind:"FittableColumns", components: [
                                        	{content:"lead",style:"width: 40%;"},
                                            
                                            /* MOD LEAD
                                            {kind: "onyx.PickerDecorator",components: [
                                                {style:"padding: 6px 18px;height: 26px;width: 121px;font-size: 13px;"},
												{name: "actLeadPicker", kind: "onyx.Picker"}
                                            ]},
                                            */
                                            
                                            {tag:"input", kind: "onyx.Input",name:"actLeadInput", style:"height: 25px;font-size: 13px;", 
                                             attributes: {label:"", value: "", idlead:""}, fit:true,
                                             rendered:function(inSender,inEvent){
                                             	var obj=this;

                                             	$("#app_actLeadInput").autocomplete({
        										source: "../it.openia.crm.GetLeadsForCalendar",                    
            										minLength: 2,
            										delay: 500,
            										change: function( event, ui ) {
            										if (ui != undefined && ui.item != undefined){
                   										obj.attributes.idlead=ui.item.id;
                                                        }		
                   									}
        										});
        										
        									    $('.ui-menu-item').live('tap', 
        									    function(event) { 
        									    	$('.ui-menu-item').trigger('click');
        									    	event.stopPropagation();
        									    });
        									  	
        									  }
                                              
                                             }
										]},
                                        {tag:"div", style:"height:10px;"}
                                    ]},
                                {kind:"FittableColumns", components: [
                        			{kind:"onyx.Button", content:"Save", ontap:"saveNewActivity"},
                        			{tag:"div", style:"width: 4%;"},
                        			{kind:"onyx.Button", content:"Cancel", ontap:"CancelActivity"},	 
                      			]}    
                        	]},
                        //{name: "activitySyncPopup", kind: "onyx.Popup", onHide:"", centered: true, floating: true, modal: true, content: "Save"}, 
                      	{name: "messageActivityPopup", kind: "onyx.Popup", style:"font-size: 20px;padding: 12px;background: orange",centered: true, floating: true, modal: true}
                      ]},
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {kind:"onyx.Button", content:"Back", ontap:"backToMain", attributes:{"data-role":"none"}},
                        {fit:true}
                    ]}
                ]},
                
                //Business Partners
                {kind:"FittableRows", components: [
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {kind: "onyx.InputDecorator", components: [
							{name:"searchInput", kind: "onyx.Input",style: "width:50px;background-color: transparent;border: transparent;", placeholder: "Search", onchange:"inputChanged"},
							{kind: "Image", src: "http://nightly.enyojs.com/latest/sampler/assets/search-input-search.png"}
						]},
						
						{name:"pickerDec",kind: "onyx.PickerDecorator",onSelect: "itemSelected", components: [
							{kind: "onyx.PickerButton", content: "Partner Category", style: "width: 170px; padding: 0px 0px;", attributes:{"data-role":"none"}},
								{kind: "onyx.Picker", components: [
								    {content:"Cust. & Supp.",value: "1"},
									{content: "Customers",value: "2"},
									{content: "Suppliers",value: "3"}
								]}
						]}
                    ]},
                    {kind:"Scroller", ondragstart:"noDrag", fit:true, classes:"todos-list", components: [
                        {name:"partnerRepeater" ,kind:"Repeater", onSetupItem: "setItem",count:"0", ontap:"selectList", classes:"list-sample-contacts-list", components: [
                            {name: "contactData", kind: "contactItem", classes: "todos-list-item list-sample-contacts-item enyo-border-box"}
                            
                        ]}
                        
                    ]},
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", components: [
                        {kind:"onyx.Button", content:"Back", ontap:"backToMain", attributes:{"data-role":"none"}}
                    ]}
                ]},
                {kind:"FittableRows", components: [
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {name:"partnerHeader", content:"Dettagli Partner", fit:true}
                    ]},
                    {kind:"Scroller", ondragstart:"noDrag", fit:true, classes:"todos-list", components: [
                        {name:"typesRepeater" ,kind:"Repeater", onSetupItem: "setReportTypes", count:"", components: [
                            {kind:"onyx.Item", classes:"todos-list-item", ontap:"selectDetail", components: [
                                {name:"partnerReportType" , content:"", classes:"todo-name"}
                            ]}
                        ]},
                            
                        {name:"SpinnerPanelThree", style:"background:white; border-radius:5px; padding:15px; ", components: [
                            {kind: "onyx.Spinner", style:"background: url(../web/it.openia.crm/images/spinner-light.gif) no-repeat 0 0;", classes: "onyx-light"}
                        ]}
                    ]},
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {kind:"onyx.Button", content:"Back", ontap:"backToLists"},
                        {fit:true}
                    ]}
                ]},
                {kind:"FittableRows", components: [
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {name:"detailHeader", content:"Dettagli", fit:true}
                    ]},
                    {kind:"Scroller", ondragstart:"noDrag", fit:true, classes:"todos-list", components: [
                        {name:"detailRepeater", kind:"Repeater", onSetupItem: "setDetails", ontap:"viewFocus", count:"0", components: [
                                 {name: "typeDetail", kind: "detailItem", classes: "todos-list-item list-sample-contacts-item enyo-border-box"}
                        ]},
                            
                        {name:"SpinnerPanelFour", style:"background:white; border-radius:5px; padding:15px; ", components: [
                            {kind: "onyx.Spinner", style:"background: url(../web/it.openia.crm/images/spinner-light.gif) no-repeat 0 0;", classes: "onyx-light"}
                        ]}
                    ]},
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {kind:"onyx.Button", content:"Back", ontap:"backToElementList"},
                        {fit:true}
                    ]}
                ]},
                {kind:"FittableRows", components: [
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {name:"focusHeader", content:"Focus", fit:true}
                    ]},
                    {kind:"Scroller", ondragstart:"noDrag", fit:true, classes:"todos-list", components: [
                        {name:"focusRepeater", kind:"Repeater", onSetupItem: "setFocus", count:"0", components: [
                       			{name: "focusDetail", kind: "focusItem", classes: "todos-list-item list-sample-contacts-item enyo-border-box"}
                                   
                        ]},
                            
                        {name:"SpinnerPanelFive", style:"background:white; border-radius:5px; padding:15px; ", components: [
                            {kind: "onyx.Spinner", style:"background: url(../web/it.openia.crm/images/spinner-light.gif) no-repeat 0 0;", classes: "onyx-light"}
                        ]}
                    ]},
                    {kind:"onyx.Toolbar", ondragstart:"noDrag", style: "background-color: #64932D;", layoutKind:"FittableColumnsLayout", components: [
                        {kind:"onyx.Button", content:"Back", ontap:"backToDetailList"},
                        {fit:true}
                    ]}
                ]}
                
            ]},
            {kind: "contentSearch", onResults: "searchResults", onMenuTypes:"setMenuTypes", onDataDescr:"setDataDescr" ,onLookupTypes:"setLookupTypes", onLookupDetail:"setLookupDetail", onLookupFocus:"setLookupFocus",onLogoutResult:"logoutButtonView" }
        ]}
    ],
    
    setInitialMenu: function (){
    	 this.$.contentSearch.getMenuInfoTypes();
         this.hideSpinners();
    },
    
    noDrag:function(){
    	return true;
    },
    
    hideSpinners: function(){
      this.$.SpinnerPanelOne.hide();
      this.$.SpinnerPanelThree.hide();
      this.$.SpinnerPanelFour.hide();
      this.$.SpinnerPanelFive.hide();  
    },
    
    rendered: function() {
		this.inherited(arguments);
		this.setInitialMenu();
		this.populateList();
		this.setActivityDatePicker();
	},
	
    selectMenuItem: function(inSender, inEvent) {
		
		var selectedItem = this.menuTypes[inEvent.index];
        this.selectedItemType = selectedItem.type;
        if(this.selectedItemType!='ACTIVITY' & this.selectedItemType != 'OPPORTUNITY' & this.selectedItemType != 'LEAD')
        	{
            	this.$.listPanels.setIndex(2);
            }
		else{
			if(this.selectedItemType=='ACTIVITY'){	
                                this.deltaDays=0;
                            
				this.$.contentSearch.getTypeDetails("", this.selectedItemType, this.deltaDays);
				this.$.descrHeader.setContent("Activities");
				this.$.listPanels.setIndex(1);
                                
                                this.$.SpinnerPanelOne.show();
				
                                this.$.descrRepeater.hide();
				this.$.descrDailyRepeater.show();
				this.$.newActivityButton.show();
                                this.$.nextSevenDays.show();
                                this.$.prevSevenDays.show();
                        }
			else if(this.selectedItemType=='OPPORTUNITY'){
				this.$.contentSearch.getTypeDetails("",this.selectedItemType);
				this.$.descrHeader.setContent("Opportunities");
				this.$.listPanels.setIndex(1);
                                
                                this.$.SpinnerPanelOne.show();
                                
				this.$.descrRepeater.show();
				this.$.descrDailyRepeater.hide();
				this.$.newActivityButton.hide();
                                this.$.nextSevenDays.hide();
                                this.$.prevSevenDays.hide();
			}
			else{
				
				this.$.contentSearch.getTypeDetails("",this.selectedItemType);
				this.$.descrHeader.setContent("Leads");
				this.$.listPanels.setIndex(1);
                                
                                this.$.SpinnerPanelOne.show();
                                
				this.$.descrRepeater.show();
				this.$.descrDailyRepeater.hide();
				this.$.newActivityButton.hide();
                                this.$.nextSevenDays.hide();
                                this.$.prevSevenDays.hide();
				
				
			}
		}
	},
	
	newActPopupCall: function(inSender, inEvent){
        this.$.actLeadInput.attributes.idlead="";
    	this.$.actLeadInput.attributes.value="";
    	this.$.actLeadInput.attributes.label="";
    	//this.$.actLeadInput.eventNode.value="";
    	this.$.subjectInput.setValue("");
        this.$.subjectInput.setContent("");
                
        this.$.newActivityPopup.show();		
	},
	
        GetNextSevenDays:function(inSender, inEvent){
            
            this.deltaDays+=7;
            this.$.contentSearch.getTypeDetails("", this.selectedItemType, this.deltaDays);
            
        },
        
        GetPrevSevenDays:function(inSender, inEvent){
            
            this.deltaDays-=7;
            this.$.contentSearch.getTypeDetails("", this.selectedItemType, this.deltaDays);
            
        },
        
        changeActView:function(inSender, inEvent){
            var itm = inSender;
               
            if(itm.getMainInfoView()==true){
                itm.applyStyle("height", "246px");
                itm.setExtraInfo();
                itm.setMainInfoView(false);
            }
            else{
                itm.applyStyle("height", "111px");
                itm.setMainInfo();
                itm.setMainInfoView(true);
            }
			
        	return true;
        },
	saveNewActivity: function(inSender, inEvent){
	
		//salva, chiudi popup, apri popup con un messaggio riguardo l'esito della procedura...
		if(this.$.subjectInput.getValue().length==0){
			this.$.messageActivityPopup.setContent("subject missing!");
			this.$.messageActivityPopup.show();
			var p = this.$.messageActivityPopup;
			setInterval(function(){p.hide()},1500);
		}
		else{
			var start = this.$.dayPicker.selected.content + "/" + (this.$.monthPicker.selected.value + 1) + "/" + this.$.yearPickerButton.getContent() + " " + 
						this.$.hourPicker.selected.content +":"+ this.$.minutePicker.selected.content;
			
			var end = new Date(this.$.yearPickerButton.getContent(), this.$.monthPicker.selected.value, this.$.dayPicker.selected.content, this.$.hourPicker.selected.content, this.$.minutePicker.selected.content, 0, 0);
			
			switch(this.$.durationPicker.selected.value){
				case 0:
					end.setMinutes(end.getMinutes()+15);
					break;
				case 1:
					end.setMinutes(end.getMinutes()+30);
					break;
				case 2:
					end.setHours(end.getHours()+1);
					break;
				case 3:
					end.setHours(end.getHours()+2);
					break;
			}
			
			
			var subject =  this.$.subjectInput.getValue();
			var type = this.$.actTypePicker.selected.value+1;
			var popup = this.$.messageActivityPopup;
			var lead;
			
                        //MOD LEAD
			//if(this.$.actLeadPicker.selected != undefined)
			//	lead = this.$.actLeadPicker.selected.id;
			
			if(this.$.actLeadInput.attributes.idlead.length>0)
			  lead = this.$.actLeadInput.attributes.idlead;
            else 
              lead = "";
			
			var obj=this;
			$.ajax({
                            url: "../it.openia.crm.InsertEvent",
                            data: {
                                from: start, 
                                info: subject,
                                descr: "",
                                enddate: end.getDate()+"/"+(end.getMonth()+1)+"/"+end.getFullYear()+" "+end.getHours()+":"+end.getMinutes(),
                                id: "",
                                idtype:type,
                                plannext:'N',
                                statName:"",
                                actStatus: "",
                                leadId: lead
                            } 
                        }).done(function(data) { 
                            if(data.match(/OK/)){
                                popup.setContent("saved!");
								popup.show();
								setInterval(function(){popup.hide()},1500);
								obj.resetActivityDatePicker();
								obj.refreshActivities();
                        }
                            else
                                alert(data);
                        });
			
                        this.$.newActivityPopup.hide();		
            }
	},
	
	CancelActivity: function(inSender, inEvent){
		this.$.newActivityPopup.hide();		
	},
	
        resetActivityDatePicker: function(inSender, inEvent){
            this.$.subjectInput.setContent("");
            
        },
        
	setActivityDatePicker: function(inSender, inEvent){
		// month
		this.months = ["JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"];
		var duration = ["15m","30m","1h","2h"];
		var d = new Date();
		var i, m, t;
		
		for (i=0; i<this.months.length; i++) {
			m=this.months[i];
			this.$.monthPicker.createComponent({content: m, active: i==d.getMonth() , value:i});
		}
		// day
		for (i=0; i<30; i++) {
			this.$.dayPicker.createComponent({content: i+1, active: i==d.getDate()-1});
		}
		
		// year
		var y = d.getYear();
		this.$.yearPicker.setSelected(y);
		this.$.yearPickerButton.setContent(1900+y);
		
		//hour
		for (i=0; i<24; i++) {
			this.$.hourPicker.createComponent({content: i, active: i==d.getHours()});
		}
		
		//minute
		for (i=0; i<60; i++) {
			this.$.minutePicker.createComponent({content: i, active: i==d.getMinutes()});
		}
		
		//duration
		for (i=0;i<duration.length;i++){
			t=duration[i];
			this.$.durationPicker.createComponent({content:t, active: i==2, value:i});
		}
		
	},
	
	refreshActivities: function(inSender, inEvent){
		this.$.contentSearch.getTypeDetails("",this.selectedItemType,this.deltaDays);		
	},
	
	setPopupActivityTypes: function(actTypesArray){
		var i;
		for (i=0;i<actTypesArray.length;i++){
			this.$.actTypePicker.createComponent({ content:actTypesArray[i].name, active: i==0, value:i });
		}
	},
	
	/*
	setPopupActivityLeads: function(actLeads){
		var i;
		//MOD LEAD
                //this.$.actLeadPicker.createComponent({ content: "", id: "" ,active: false });
		//for (i=0;i<actLeads.length;i++){
		//	this.$.actLeadPicker.createComponent({ content: actLeads[i].firstname +" "+ actLeads[i].lastname, id: actLeads[i].id ,active: false });
		//}
		
	},
	*/
	
	setNewActivityPopupNames: function(inContactNames){
		this.$.actTypeText.setContent(inContactNames.value3);
	},
	
	setupYear: function(inSender, inEvent) {
		this.$.year.setContent(1900+inEvent.index);
	},
	
	selectList: function(inSender, inEvent) {
        var selectedPartner = this.results[inEvent.index];
        this.$.partnerHeader.setContent(selectedPartner.name);
        this.$.contentSearch.getBpReportTypes();
        this.selectedPartnerId = selectedPartner.id;
        
        this.$.SpinnerPanelThree.show();
        this.$.listPanels.setIndex(3);
        
    },
    
    selectDetail: function(inSender, inEvent) {
    	var selectedDetail = this.lkupTypes[inEvent.index];
    	this.$.detailHeader.setContent(this.$.partnerHeader.getContent()+" - "+selectedDetail.name);
        this.$.contentSearch.getTypeDetails(this.selectedPartnerId,selectedDetail.type);
    	this.selectedDetailEnum = selectedDetail.type;
        
        this.$.SpinnerPanelFour.show();
        
        this.$.listPanels.setIndex(4);
    },
    
    viewFocus: function(inSender, inEvent) {
            var selectedFocus = this.details[inEvent.index];
            this.selectedFocusId = selectedFocus.id;
            if(this.selectedDetailEnum!='ACTIVITY' & this.selectedDetailEnum!='OPPORTUNITY')
            {
            	this.$.focusHeader.setContent(this.$.detailHeader.getContent()+" - "+selectedFocus.value1);
            	this.$.contentSearch.getDetailElements(this.selectedFocusId,this.selectedDetailEnum);
        
            	this.$.listPanels.setIndex(5);
                this.$.SpinnerPanelFive.show();
            }
        
    },
    
    backToMain: function() {
    	this.$.listPanels.setIndex(0);
    },
    
    backToLists: function() {
        this.$.listPanels.setIndex(2);
    },
    backToElementList: function() {
        this.$.listPanels.setIndex(3);
    },
    
    backToDetailList:function() {
        this.$.listPanels.setIndex(4);
    },
    
    //evento che viene chiamato ogni volta che viene lanciata la setCount, e inizializza ogni elemento del Repeater
    setItem: function(inSender, inEvent) {
    	
    	var item = inEvent.item;
    	var i = inEvent.index;
    	if(this.results != undefined)
    		item.$.contactData.setContact(this.results[i]);            
    
        return true;
	},
	
	setMenuRep: function(inSender, inEvent) {
    	
    	var item = inEvent.item;
    	var i = inEvent.index;
        if(this.menuTypes != undefined)
        	item.$.menuType.setContent(this.menuTypes[i].name);        
    
        return true;
	},
	
	setDescr: function(inSender, inEvent){
		var item = inEvent.item;
    	var i = inEvent.index;
        if(this.dataDescrTypes != undefined)
        	item.$.typeDescr.setDetail(this.dataDescrTypes[i],this.dataDescrNames);
        
        return true;	
	},
	
    setActivityDescr: function(inSender,inEvent){
	var item = inEvent.item;
    	var i = inEvent.index;
        
        if(this.dataDescrTypes != undefined)
        	item.$.actDescr.setDetail(this.dailyactivities[i],this.dataDescrNames,this.lStatuses);
        
        return true;
	},
    
    setDayDescr: function(inSender,inEvent){
        var item = inEvent.item;
    	var i = inEvent.index;
        
        if(this.dataDescrTypes != undefined){
        	item.$.dayHeader.setContent(this.dataDescrTypes[i].date);
                
            this.dailyactivities=this.dataDescrTypes[i].activity;
                
            item.$.descrActivityRepeater.setCount(this.dailyactivities.length);
        }
        return true;
        
    },
    

    setReportTypes: function(inSender, inEvent) {
    	
    	var item = inEvent.item;
    	var i = inEvent.index;
        if(this.lkupTypes != undefined)
        	item.$.partnerReportType.setContent(this.lkupTypes[i].name);

        return true;
	},
    
    setDetails: function(inSender, inEvent) {
    	
    	var item = inEvent.item;
    	var i = inEvent.index;
        if(this.details != undefined)
        	item.$.typeDetail.setDetail(this.details[i],this.detailnames);
        
        return true;
	},
    
    setFocus: function(inSender, inEvent) {
    	
    	var item = inEvent.item;
    	var i = inEvent.index;
        if(this.focus != undefined)
                item.$.focusDetail.setDetail(this.focus[i],this.focusnames);
	return true;
        },    
    
    inputChanged:function(inSender, inEvent) {
        
        this.searchText = this.$.searchInput.getValue();
        
        if(this.selectedPickerValue==undefined)
        	this.$.contentSearch.search(this.searchText,0);
        else this.$.contentSearch.search(this.searchText,this.selectedPickerValue);
    },
    
    itemSelected:function(inSender, inEvent) {
    	this.$.pickerDec.setContent(inEvent.selected.content);
    	this.selectedPickerValue = inEvent.selected.value;
    	this.$.contentSearch.search(this.searchText,this.selectedPickerValue);
    },
    
    //Evento che viene lanciato dall'oggetto ContentSearch alla ricezione dei dati.
    //Provvede all'aggiornamento degli elementi della vista 
    searchResults:function(inSender, inResults) {
  		this.results = inResults;
		this.$.partnerRepeater.setCount(this.results.length);
    },
    
    setMenuTypes:function(inSender, inResults) {
  		this.menuTypes = inResults;
		this.$.menuRepeater.setCount(this.menuTypes.length);
    },
    
    setLookupTypes:function(inSender, inResults) {
  		this.lkupTypes = inResults;
                
                this.$.SpinnerPanelThree.hide();
		
                this.$.typesRepeater.setCount(this.lkupTypes.length);
    },
    
    setDataDescr: function(inSender,inResults){
		
                //this.dataDescrTypes = inResults.detailList;
		this.dataDescrNames = inResults.valueNames;
                this.lStatuses = inResults.leadStatuses;
                
                this.$.SpinnerPanelOne.hide();
                
		if(this.selectedItemType=='ACTIVITY'){	
                    this.dataDescrTypes = inResults.agendaDays;
                    
                    //this.$.descrActivityRepeater.setCount(this.dataDescrTypes.length);
                    this.$.descrDailyRepeater.setCount(this.dataDescrTypes.length);
                    
                    this.setPopupActivityTypes(inResults.actTypes);
                    //this.setPopupActivityLeads(inResults.leadsList);
                    this.setNewActivityPopupNames(this.dataDescrNames);
                }
                else{
                    this.dataDescrTypes = inResults.detailList;
                    this.$.descrRepeater.setCount(this.dataDescrTypes.length);
                }        
    },
    
    setLookupDetail:function(inSender, inResults1) {
  		this.details = inResults1.detailList;
                this.detailnames = inResults1.valueNames;
                
                this.$.SpinnerPanelFour.hide();
                
		this.$.detailRepeater.setCount(this.details.length);
    },
    
    setLookupFocus: function(inSender, inResults1) {
  		this.focus = inResults1.detailList;
  		this.focusnames = inResults1.valueNames;
                
                this.$.SpinnerPanelFive.hide();
                
		this.$.focusRepeater.setCount(this.focus.length);
    },
    
    populateList:function(inSender, inEvent) {
        
        this.$.contentSearch.search("",0);
        
    },
    logoutButtonView:function(inSender,inResults) {
        if(inResults.logoutButton==false)
            this.$.logoutButton.hide();
        
    },
    redirectToMenu:function(){
    	window.location.href="../it.openia.crm/";
    }  
    
});

// Component per la Search.
enyo.kind({
	name: "contentSearch",
	kind: "Component",
	published: {
		searchText: ""
	},
	events: {
		onResults: "",
		onMenuTypes:"",
		onLookupTypes: "",
		onDataDescr:"",
        onLookupDetail:"",
        onLookupFocus:"",
        onLogoutResult:""
	},
	
	search: function(inSearchText,inPickervalue) {
		// 1 chiamata ajax per prendere i dati
		var partners;
		var parentFunc = this;	
                $.ajax({
                          url: "../it.openia.crm.GetLookupInfo",
                          data: {
                                   text: inSearchText,
                                   value: inPickervalue
                                 }
                                                     
                       }).done(function(data) { 
                                    partners = data.bpartners;
				    parentFunc.doResults(partners);
                                    
                	    });
                                       
		
	},
        
   getBpReportTypes: function(Id) {
		// 1 chiamata ajax per prendere i dati
		var lookupTypes;
		var lkupParentFunc = this;	
                $.ajax({
                          url: "../it.openia.crm.GetLookupTypes"
                       
                       }).done(function(data) { 
                                    lookupTypes = data.lookupTypes;
                                    lkupParentFunc.doLookupTypes(lookupTypes);
                                    
                	    });
                                       
   },
   
   getMenuInfoTypes: function(){
   		var menuTypes;
   		var lkupParentFunc = this;
   		
   				$.ajax({
                          url: "../it.openia.crm.GetMenuInfo"
                       
                       }).done(function(data) { 
                                    menuTypes = data.lookupTypes;
                                    lkupParentFunc.doMenuTypes(menuTypes);
                                    lkupParentFunc.doLogoutResult(data);
                	    });
   			 
   	
   },
	
    getTypeDetails: function( bpId, lookupEnum, deltaDays){
    	var detailList;
        var detailNames;
    	var detParentFunc = this;
    		if(bpId.length!=0){
    			$.ajax({
                          url: "../it.openia.crm.TypeRequestDispatcher",
                       	  data: {
                                bpartner: bpId,
                                lookupType: lookupEnum
                          }
                       }).done(function(data) { 
                                    
                                    detParentFunc.doLookupDetail(data);
                                    
                	    });  
            }    	    
            else{ 
            
                if(lookupEnum.match(/ACTIVITY/))
                    $.ajax({
                          url: "../it.openia.crm.GetWeeklyActivities",
                          data: {
                                selectedType: lookupEnum,
                                delta:deltaDays
                          }
                       }).done(function(data) { 
                                    detParentFunc.doDataDescr(data);
                	    });	    	    
    		else
                    $.ajax({
                          url: "../it.openia.crm.GetDataFromMenu",
                       	  data: {
                                selectedType: lookupEnum
                          }
                       }).done(function(data) { 
                                    detParentFunc.doDataDescr(data);
                	    });
            
            }
    },
    
    getDetailElements: function(recId,lookupEnum){
    	var elemList;
        var elemNames;
    	var elemParentFunc = this;
    		$.ajax({
                          url: "../it.openia.crm.FocusRequestDispatcher",
                       	  data: {
                                recordId: recId,
                                lookupType: lookupEnum
                          }
                       }).done(function(data) { 
                                    
                                    elemParentFunc.doLookupFocus(data);
                	    });
           
            
    }
    
});

// Componente che rappresenta il contatto.
enyo.kind({
    
        name: "contactItem",
	
	components: [
		{name: "name", classes: "row_title"},
		{name: "address", classes: "list-sample-contacts-description"},
		{name:"phone", classes: "list-sample-contacts-description"}
		//,{kind:"onyx.Button", content:"test", ontap:'alertest' }
	],
        
	setContact: function(inContact) {
		this.$.name.setContent(inContact.name);
		this.$.address.setContent(inContact.address);
		this.$.phone.setContent(inContact.phone);
	}
});


// Componente che rappresenta il contatto.
enyo.kind({
    
        name: "detailItem",
	
	components: [
		{name: "name1", classes: "row_title"},
		{name: "name2", classes: "list-sample-contacts-description"},
		{name: "name3", classes: "list-sample-contacts-description"}
	],
        
	setDetail: function(inContact,inContactNames) {
		this.$.name1.setContent(inContact.value1);
		this.$.name2.setContent(inContactNames.value2+" : "+inContact.value2);
        this.$.name3.setContent(inContactNames.value3+" : "+inContact.value3);
		
	}
    
});

// Componente che rappresenta l'attivita'.
enyo.kind({
    
        name: "activityItem",
		published: {
        	mainInfoView: true
        },
	components: [
            {tag:"div", name:"mainInfo", components:[
            	{name: "leadname", style:"font-size: 16px;" ,classes: "list-sample-contacts-description"},
            	{name: "name1", classes: "row_title"},
            	{name: "name2", style:"font-size: 16px;" ,classes: "list-sample-contacts-description"},
            	{name: "name3", classes: "list-sample-contacts-description"},
            	{kind: enyo.Control, tag: "a", name:"name6", style:"text-decoration: none;color: #333333;font-size: 16px;"},
            ]},
            {tag:"div", name:"extraInfo", components:[
            	{tag:"div", style:"height:5px;",allowHtml:true, content:"&nbsp;"},
            	{name: "name5", style:"font-size: 16px;",classes: "list-sample-contacts-description", content:""},
            
            	{kind: "onyx.PickerDecorator", onSelect: "actSelected", name:"actDropDownListActPicker", components:[
                	{style:"width: 165px;font-size: 15px;height: 28px;padding: 0px;"},
                	{kind: "onyx.Picker", style:"width: 165px;font-size:15px;", name:"actStatusDropDownList"}
                    ],
                    tap: function(inSender, inEvent) {
    					return true;
					}
                },
            	
            
            	{tag:"div", style:"height:5px;",allowHtml:true, content:"&nbsp;"},
            	{name: "name4", style:"font-size: 16px;",classes: "list-sample-contacts-description"},
            	
            	{kind: "onyx.PickerDecorator", onSelect: "itemSelected", name:"actDropDownListPicker", components:[
                	{style:"width: 165px;font-size: 15px;height: 28px;padding: 0px;"},
                	{kind: "onyx.Picker", style:"width: 165px;font-size:15px;", name:"actDropDownList"}
                    ],
                    tap: function(inSender, inEvent) {
    					return true;
					}
                },
                {tag:"div", style:"height:10px;",allowHtml:true, content:"&nbsp;"},    
          {kind:"FittableColumns", components: [
            		{kind: "onyx.Button", style:"height: 26px;background-color:white;", ontap:"DownloadIcsFile", components: [
						{kind: "onyx.Icon", src: "../web/it.openia.crm/images/arrow_down.png"}
					]},
					{tag:"div", style:"width:5px;",allowHtml:true, content:"&nbsp;"},    
            		{kind: "onyx.Button", style:"height: 26px;background-color:white;", ontap:"ConfirmDeleteAct", components: [
						{kind: "onyx.Icon", src: "../web/it.openia.crm/images/delete.png"}
					]}
            	]},
          {name: "DeleteActivityPopup",content:"Delete Activity?", kind: "onyx.Popup", style:"font-size: 20px;padding: 12px;background: rgba(76, 76, 76, 0.82)",centered: true, floating: true, modal: true, 
          	components:[
          	{kind:"FittableRows", components: [	
          		{tag:"div", content:"Delete Activity?"},
          		{tag:"div",components:[
          			{kind:"FittableColumns", components: [	
          				{kind: "onyx.Button", style:"height: 26px;background-color:white;", content:"Yes", ontap:"DeleteAct"},
						{tag:"div", style:"width:5px"},
						{kind: "onyx.Button", style:"height: 26px;background-color:white;", content:"No", ontap:"CloseDeleteAct"},	
          			]}
          		]}
          	]}
          	]},
            ]},
          {name:"actId"}
        ],
        
    rendered:function(){
    	this.$.actId.hide();
        this.$.mainInfo.show();
        this.$.extraInfo.hide();
    },
    setDetail: function(inContact,inContactNames, statusesArray) {
		//this.$.name1.setContent(inContactNames.value1+" : "+inContact.value1);
	if(inContact.leadName.length>0){
		this.$.leadname.setContent(inContact.leadName);
		this.$.leadname.show();
	}
	else{
		this.$.leadname.setContent("");
		this.$.leadname.hide();
	}
	
	this.$.name1.setContent(inContact.value1);
	//this.$.name2.setContent(inContactNames.value2+" : "+inContact.value2);
        this.$.name2.setContent(inContact.value2);
        
        this.$.name3.setContent(inContactNames.value3+" : "+inContact.value3);
        this.$.name4.setContent(inContactNames.value4);
        this.setLeadStatuses(statusesArray,inContact.value4num);
        this.$.name5.setContent(inContactNames.value5);
        this.setActivityStatuses(inContact.actStatuses,inContact.value5);
		this.$.actId.setContent(inContact.id);
        if(inContact.phoneNum.length != 0){        
            this.$.name6.setAttribute("href", "tel:"+inContact.phoneNum);        
            this.$.name6.setContent("Tel. "+inContact.phoneNum);
            }
         
        },
    setLeadStatuses: function(statusesArray,index){
            this.$.actDropDownList.createComponent({value:"-1", content:""});
            var list = this.$.actDropDownList;
            for(i in statusesArray){
            	if(i==index)
            		list.createComponent({value:statusesArray[i].id, content:statusesArray[i].name, active:true});
            	else
            		list.createComponent({value:statusesArray[i].id, content:statusesArray[i].name, active:false});
            }
            
            list.render();
            
        },
        
   setActivityStatuses: function(actStatusesArray,id){
       this.$.actStatusDropDownList.createComponent({value:"",content:""});
       var list = this.$.actStatusDropDownList;
       
       for(i in actStatusesArray){
           if(actStatusesArray[i].statusKey==id)
               list.createComponent({value:actStatusesArray[i].statusKey, content:actStatusesArray[i].statusTranslation, active:true});
           else
               list.createComponent({value:actStatusesArray[i].statusKey, content:actStatusesArray[i].statusTranslation, active:false});
       }
       
		list.render();
   },
   
   itemSelected: function(inSender, inEvent){
       var selected = inEvent.selected.value;
       $.ajax({
                url: "../it.openia.crm.LookupSyncActivity",
                data: {
                        leadstatus: selected,
                        activityId: this.$.actId.getContent(),
                        updateType: "leadStatus"
                      }
             }).done(function(data) { 
             });
   	   return true;	
   },
   
   noAction:function(inSender, inEvent){
       return true;			
   },
   
   actSelected: function(inSender, inEvent){
        var selected = inEvent.selected.value;
        
        $.ajax({
                url: "../it.openia.crm.LookupSyncActivity",
                data: {
                        activityStatusId: selected,
                        activityId: this.$.actId.getContent(),
                        updateType: "activityStatus"
                      }
             }).done(function(data) { 
             });
     return true;  
   },
   DownloadIcsFile: function(inSender, inEvent){
   
   		window.location="../it.openia.crm.CalendarIcsFileGenerator?activityId="+this.$.actId.getContent();
   		return true;		
   },
   ConfirmDeleteAct:function(inSender, inEvent){
   		this.$.DeleteActivityPopup.show();
   		return true;
   },
   
   DeleteAct: function(inSender, inEvent){
   		var obj=this;
   		
   		$.ajax({
                url: "../it.openia.crm.DeleteEvent",
                data: {
                        id: this.$.actId.getContent()
				}
             }).done(function(data) { 
             	obj.bubble("onRefreshActs");
             });
        this.$.DeleteActivityPopup.hide();     
        return true;
   },
   
   CloseDeleteAct: function(inSender, inEvent){
   		this.$.DeleteActivityPopup.hide();
   		return true;
   },
   
   setExtraInfo: function(inSender, inEvent){
        this.$.extraInfo.show();
   },
   setMainInfo: function(inSender, inEvent){
   	this.$.extraInfo.hide();	
   }
            
    
});


// Componente che rappresenta il contatto.
enyo.kind({
    
        name: "focusItem",
	
	components: [
		{name: "name1", classes: "row_title"},
		{name: "name2", classes: "list-sample-contacts-description"},
		{name: "name3", classes: "list-sample-contacts-description"}
	],
        
	setDetail: function(inContact,inContactNames) {
		this.$.name1.setContent(inContact.value1);
		this.$.name2.setContent(inContactNames.value2+" : "+inContact.value2);
                this.$.name3.setContent(inContactNames.value3+" : "+inContact.value3);		
	}
   
    
});
