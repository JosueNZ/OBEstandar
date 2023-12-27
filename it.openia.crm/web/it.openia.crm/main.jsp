<%@ page import="it.openia.crm.*" %>
<%@page import="java.lang.String"%>
<%@page import="java.util.ArrayList"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<!-- Copyright (C) 2008-2013 Openia S.r.l.
   - This Source Code Form is subject to the terms of the Mozilla Public
   - License, v. 2.0. If a copy of the MPL was not distributed with this
   - file, You can obtain one at http://mozilla.org/MPL/2.0/. -->
<html>
    <head>
        <title>Openia CRM - Openbravo ERP module</title>
        <link rel='stylesheet' type='text/css' href='../web/it.openia.crm/css/fullcalendar.css'>
        <link rel='stylesheet' type='text/css' href='../web/it.openia.crm/css/jquery.qtip.min.css'>
        <link rel="stylesheet" type='text/css' href='http://code.jquery.com/ui/1.10.0/themes/base/jquery-ui.css' />
        <link rel="stylesheet" type='text/css' href='../web/it.openia.crm/css/bootstrap-datetimepicker.min.css' /> 
	<link rel="stylesheet" type='text/css' href="../web/it.openia.crm/css/bootstrap-combined.min.css">
		
	<style type="text/css">
            .ui-widget input, .ui-widget select, .ui-widget textarea, .ui-widget button { font-family: Verdana,Arial,sans-serif/*{ffDefault}*/; font-size:small;}   
            .ui-dialog .ui-dialog-titlebar { padding: .1em 1em; }
            .ui-widget-header { background: rgb(234, 245, 214);  border:0px;}
            .ui-dialog .ui-dialog-title {font-family: 'lucida sans',sans-serif; font-size: 16px; font-weight: 700; }
            
            a:hover, a:active {outline: 0px;color: #005580; text-decoration: none;}
           
            /* Opportunity Event Color */
            .Opportunity,
            .Opportunity div,
            .Opportunity span {
                background-color: green; /* background color */
                border-color: green;     /* border color */
                color: white;           /* text color */
            }
            
            /*Closed Event color*/
            .Closed,
            .Closed div,
            .Closed span {
                background-color: grey; /* background color */
                border-color: grey;     /* border color */
                color: white;           /* text color */
            }
            
        </style>
        
        <script type='text/javascript' src='../web/it.openia.crm/js/jquery-1.8.1.min.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/js/jquery-ui-1.8.23.custom.min.js'></script>
        <script type='text/javascript' src="../web/it.openia.crm/js/jquery-1.8.3.js"></script>
        
        <script type="text/javascript"
     			src="http://netdna.bootstrapcdn.com/twitter-bootstrap/2.2.2/js/bootstrap.min.js">
    	</script>
        
        <script type='text/javascript' src="../web/it.openia.crm/js/jquery-ui.js"></script>
        <script type='text/javascript' src="../web/it.openia.crm/js/fullcalendar_<%=GetEvents.GetLocalization()%>.min.js"></script>
        <script type='text/javascript' src='../web/it.openia.crm/js/jquery.qtip.min.js'></script>
        
        <script type='text/javascript' src='../web/it.openia.crm/js/bootstrap-datetimepicker.min.js'></script>
		
        <script type="text/javascript" src="../web/it.openia.crm/js/bootstrap-datetimepicker.en-US.js"></script>
	<script type="text/javascript" src="../web/it.openia.crm/js/bootstrap-datetimepicker.it-IT.js"></script>	
	<script type="text/javascript" src="../web/it.openia.crm/js/bootstrap-datetimepicker.es-ES.js"></script>
        
        <script type='text/javascript'>
            
            function resizeCalendarDiv(){
                $("#calendar").css("height", document.body.parentNode.clientHeight);
            }
            
            $(document).ready(function() {
                $('#activitydialog').hide();
                $('#ConfirmationDiv').hide();
                $('#RescheduleDiv').hide();
                $('#leadStatusRow').hide();
                var eventsFromActivities;
                var activityTypes;
                var leadList;
                var statuses;
                var view; 
                var insertDate;
                var evento;
                var event_qtip_id='';
                var allevents=false;
                
                //shows the activity status dropdownlist when an activity type is chosen on the 'activitydialog' popup
                var setActivityStatuses = function(strActId){
                    
                    $('#evtDoneAndNextBtn').show();
                    
                    $('select[name="activity_status"]').empty();
        			
                    $('select[name="activity_status"]').append($('<option/>').attr("value", 0).text(''));
                    var count=1;
                    var arr= activityTypes[strActId-1].activityStatusList;
                    $.each(activityTypes[strActId-1].activityStatusList, function(i, option) {
                            if(option.defaultStatus)
                                $('select[name="activity_status"]').append($('<option/>').attr("value",count).attr("selected",true).text(option.statusTranslation));
                	    else 	
                                $('select[name="activity_status"]').append($('<option/>').attr("value",count).text(option.statusTranslation));    
                                count++;
                    });
                    
                    $('#ActivityStatus').show();
 
        	};
                
                //shows the activity status dropdownlist for the 'next activity' editing window when an activity is chosen on the 'activitydialog' popup
        	var setActivityStatusesNext = function(strActId){
                    
                    $('select[name="activity_status_next"]').empty();
        			
                    $('select[name="activity_status_next"]').append($('<option/>').attr("value", 0).text(''));
                    var count=1;
                    var arr= activityTypes[strActId-1].activityStatusList;
                    $.each(activityTypes[strActId-1].activityStatusList, function(i, option) {
                            if(option.defaultStatus)
                                $('select[name="activity_status_next"]').append($('<option/>').attr("value",count).attr("selected",true).text(option.statusTranslation));
                            else
                                $('select[name="activity_status_next"]').append($('<option/>').attr("value",count).text(option.statusTranslation));
                        
                        count++;
                    });
                    
                    $('#ActivityStatusNext').show();
 
        	};
        	
                //when loading an event on the activitydialog popup, this method sets the value of the activity status type on the dropdownlist
                var setValueOnStatusId = function(strActId,statusId){
                    var statusArray = activityTypes[strActId-1].activityStatusList;
                    for (index in statusArray){
                        if(statusArray[index].statusKey == statusId){
                        	var i = parseInt(index)+1;
                            $('select[name="activity_status"]').val(i);
                            return;
                         }
                    }
                }
                
                //when saving the event that has been edited on the 'activitydialog' popup this method gets the activity status type Id
                var getActualActivityStatus = function(strActId){
                    var statusIndex = $('select[name="activity_status"]').attr('value');
                    if(statusIndex!=0){
                        var a = activityTypes[strActId-1];
                        var b = a.activityStatusList[statusIndex-1].statusKey;
                        return activityTypes[strActId-1].activityStatusList[statusIndex-1].statusKey;
                    }
                    else "";
                }
                
                //same as getActualActivityStatus, gets called when saving the 'next activity' 
                var getActualActivityStatusNext = function(strActId){
                    var statusIndex = $('select[name="activity_status_next"]').attr('value');
                    if(statusIndex!=0){
                        var a = activityTypes[strActId-1];
                        var b = a.activityStatusList[statusIndex-1].statusKey;
                        return activityTypes[strActId-1].activityStatusList[statusIndex-1].statusKey;
                    }
                    else "";
                }
                
                //on each activity type dropdownlist selection, this method sets the related activity status dropdownlist,
                //hides the status dropdownlist if the selected activity type is null
        	$('select[name="tipo_attivita"]').change(function() {
  			var selection = $('select[name="tipo_attivita"]').attr('value');
        		
        		//next activity gets same type by default
        		$('select[name="tipo_attivita_next"]').val(selection);
        		
        		if ( selection == 0){
        			$('#ActivityStatus').hide();
        			$('select[name="activity_status"]').empty();
        			$('#evtDoneAndNextBtn').hide();
        			
        			//next activity gets previous same type by default
        			$('#ActivityStatusNext').hide();
        			$('select[name="activity_status_next"]').empty();
        		}
        		else{
                            setActivityStatuses(selection);
        					setActivityStatusesNext(selection);
        		}
		});
           
           //same as above but for the 'next activity' window
           $('select[name="tipo_attivita_next"]').change(function() {
  			var selection = $('select[name="tipo_attivita_next"]').attr('value');
        			
        		if ( selection == 0){
        			$('#ActivityStatusNext').hide();
        			$('select[name="activity_status_next"]').empty();
        		}
        		else
                            setActivityStatusesNext(selection);
        		
		});	
        	
        	//ajax call that retrieves the events and initializes the fullcalendar object
                $.ajax({
                    url: "../it.openia.crm.GetEvents"
                }).done(function(data) { 
                    eventsFromActivities=data.events;
                    activityTypes = data.actTypes;
                    statuses = data.leadStatuses;
                    leadList = data.leadsList;
                    
                    $('select[name="tipo_attivita"]').append($('<option/>').attr("value", 0).text(''));
                
                    $('select[name="tipo_attivita_next"]').append($('<option/>').attr("value", 0).text(''));
                    
                    $('select[name="lead_status"]').append($('<option/>').attr("value", 0).text(''));
                    
                    $('select[name="lead_dropdownlist"]').append($('<option/>').attr("value", 0).text(''));
                    
                    $.each(activityTypes, function(i, option) {
                        $('select[name="tipo_attivita"]').append($('<option/>').attr("value", option.id+1).text(option.name));
                    });
                    
                    $.each(activityTypes, function(i, option) {
                        $('select[name="tipo_attivita_next"]').append($('<option/>').attr("value", option.id+1).text(option.name));
                    });
                    
                    $.each(statuses, function(i,option){
                        $('select[name="lead_status"]').append($('<option/>').attr("value", option.id+1).text(option.name));
                        
                    });
        	    
                    $.each(leadList, function(i,option){
                        if(option.commercialname!=undefined && option.commercialname.length>0)
                            $('select[name="lead_dropdownlist"]').append($('<option/>').attr("value", option.id).text(option.firstname+" "+option.lastname+" - "+option.commercialname));
                        else
                            $('select[name="lead_dropdownlist"]').append($('<option/>').attr("value", option.id).text(option.firstname+" "+option.lastname));
                    });
                    
                    $('#calendar').fullCalendar({
                        height: 500,
                        firstDay: 1,
                        header: {
                            left: 'prevYear,prev,next,nextYear today',
                            center: 'title',
                            right: 'month,agendaWeek,agendaDay'
                        },
                        editable: true,
                        events:eventsFromActivities,
                        timeFormat: 'H:mm' ,
                        axisFormat: 'H:mm' ,
                        
                        eventRender: function(event, element, calEvent) {
                            element.find(".fc-event-time").before($("<span class=\"fc-event-icons\"></span>")
                            .html(event.imgText));
                            
                            $('.userPopUpInfo').smallipop();
                        },
                        
                        eventDrop:function(event,dayDelta,minuteDelta,allDay,revertFunc) {
                                            
                            $.ajax({
                                url: "../it.openia.crm.ModifyEvents",
                                data: {
                                    id: event.id,
                                    dayDt: dayDelta,
                                    minuteDt: minuteDelta,
                                    daily: allDay.toString()
                                }
                                                     
                            });
                        },
                        eventResize: function(event,dayDelta,minuteDelta,revertFunc) {
                                           
                            $.ajax({
                                url: "../it.openia.crm.ModifyEvents",
                                data: {
                                    id: event.id,
                                    dayDt: dayDelta,
                                    minuteDt: minuteDelta
                                }
                                                     
                            });  
                        },
                         
                        eventClick: function(event) {
                            var d_start_title = $.fullCalendar.formatDate( event.start, "dd/MM/yyyy");
                            var d_start = $.fullCalendar.formatDate( event.start, "dd/MM/yyyy HH:mm");
                            var d_end = $.fullCalendar.formatDate( event.end, "dd/MM/yyyy HH:mm");
                            
                            $('#actOBLink').show();
                            $('#actOBLink').attr('href',"../?tabId=046D51DD5D4840C58C36AA09C776F0E1&recordId="+event.id);
                            
                            $('#ActivityStatusNext').hide();
                            
                            if(event.actType>0){
                            	setActivityStatuses(event.actType);
                                setValueOnStatusId(event.actType,event.actTypeStatusId);
                                
                                setActivityStatusesNext(event.actType);
                            }
                            else
                            	$('#ActivityStatus').hide();
                            
                            $('#partnerDiv').empty();
                            if(event.bpName.length != 0 | event.leadName.length != 0){
                                if(event.bpName.length != 0)
                                	$('#partnerDiv').append(event.bpName+"<br/>");
                                if(event.leadName.length != 0)	
                                	$('#partnerDiv').append("<div style='font-size:13px;'>"+event.leadName+"<br/><br/><\/div>");
                                
                                $('#leadStatusRow').show();
                                $('select[name="lead_status"]').val(event.statName);
                            }
                            else 
                                $('#leadStatusRow').hide();
                            
                            $('#partnerDivNextEvt').empty();
                            if(event.bpName.length != 0 | event.leadName.length != 0){
                                if(event.bpName.length != 0)
                                	$('#partnerDivNextEvt').append(event.bpName+"<br/>");
                                if(event.leadName.length != 0)	
                                	$('#partnerDivNextEvt').append("<div style='font-size:13px;'>"+event.leadName+"<br/><br/><\/div>");
                            }
                            
                            $('#activitydialog').dialog({ title: d_start_title, minWidth : (screen.width)/3 , minHeight: 100});
                            $('#activitydialog').attr('evt_id',event.id);
                                
                            $('input[name="info_attivita"]').val(event.subject);
                            $('input[name="info_next_attivita"]').val(event.subject),
                            $('textarea[name="desc_attivita"]').val(event.description);
                            $('input[name="start_attivita"]').val(d_start);
                            $('input[name="end_attivita"]').val(d_end);
                            $('select[name="tipo_attivita"]').val(event.actType);
                            $('select[name="tipo_attivita_next"]').val(event.actType);
                            
                            $('select[name="lead_dropdownlist"]').val('');
                    		$('#LeadSelect').hide();
                    
                            $('#evtDeleteBtn').show();
                            $('#evtRescheduleBtn').show();
                            $('#activitydialog').show();
                            $('#evtDoneAndNextBtn').show();
                            insertDate = d_start;
                            
                            var pickerStart = $('#datetimepicker1').data('datetimepicker');
                            pickerStart.setLocalDate(new Date(event.start.getFullYear(), event.start.getMonth(), event.start.getDate(), event.start.getHours(), event.start.getMinutes(), 0, 0));
                            var pickerEnd = $('#datetimepicker2').data('datetimepicker');
                            pickerEnd.setLocalDate(new Date(event.end.getFullYear(), event.end.getMonth(), event.end.getDate(), event.end.getHours(), event.end.getMinutes(), 0, 0));
                        },
                        
                        dayClick: function(date, allDay, jsEvent, view) {
                            $('#partnerDiv').empty();
                            $('#leadStatusRow').hide();
                            $('#ActivityStatus').hide();
                            $('#actOBLink').hide();
                            
                            if(! $('#activitydialog').is(':visible'))
                            {
                            	$('select[name="lead_dropdownlist"]').val('');
                    			$('#LeadSelect').show();
                    
                            	var d = date.getDate()+"/"+(date.getMonth()+1).toString()+"/"+date.getFullYear();
                                $('#activitydialog').dialog({ title: d, minWidth : (screen.width)/3 , minHeight: 100});
                                var date_default =$.fullCalendar.formatDate(date, "dd/MM/yyyy HH:mm");
                                $('input[name="start_attivita"]').val(date_default);
                                $('#evtDeleteBtn').hide();
                                $('#evtRescheduleBtn').hide();
                                $('#evtDoneAndNextBtn').hide();
                                $('#activitydialog').show();
                                insertDate = d;
                                
                                var pickerStart = $('#datetimepicker1').data('datetimepicker');
				pickerStart.setLocalDate(new Date(date.getFullYear(), date.getMonth(), date.getDate(), 9, 0, 0, 0));
                            	var pickerEnd = $('#datetimepicker2').data('datetimepicker');
                            	pickerEnd.setLocalDate(new Date(date.getFullYear(), date.getMonth(), date.getDate(), 10, 0, 0, 0));
                            }
                            else{
                                $('#activitydialog').dialog( 'close' );
                            }
                            
                        }                                            
                    });
                    
                    var refresh_button = '&nbsp;&nbsp<span class="fc-button fc-state-default fc-corner-left fc-corner-right fc-button-refresh" title="see open activities"><span class="fc-button-inner"><span class="fc-button-content"><img src="../web/it.openia.crm/images/arrow_rotate_anticlockwise.png" style="position:relative;top:4px"/><\/span><span class="fc-button-effect"><span/><\/span><\/span><\/span>';
                    var getAllEvents_button = '&nbsp;&nbsp<span class="fc-button fc-state-default fc-corner-left fc-corner-right fc-button-getAll" title="include closed events"><span class="fc-button-inner"><span class="fc-button-content"><img src="../web/it.openia.crm/images/funnel-icon.png" style="position:relative;top:4px"/><\/span><span class="fc-button-effect"><span/><\/span><\/span><\/span>';
                    var menu_button = '&nbsp;&nbsp;<span class="fc-button fc-state-default fc-corner-left fc-corner-right fc-button-menu"><span class="fc-button-inner"><span class="fc-button-content">Menu<\/span><span class="fc-button-effect"><span/><\/span><\/span><\/span>';
                    if(data.showMenu)
                        $('.fc-header-left').append(refresh_button).append(getAllEvents_button).append(menu_button);
                    else $('.fc-header-left').append(refresh_button).append(getAllEvents_button);    
                });  
        		
        	var refreshCalendar = function() {
                    view = $('#calendar').fullCalendar('getView');
                    
                    if(allevents==false)
                        //prev month
                        $.ajax({
                            url: "../it.openia.crm.GetEvents",
                            data: {
                                from: view.start, 
                                to: view.end,
                                all:"N"
                            } 
                        }).done(function(data) { 
                            eventsFromActivities=data.events;
                
                            $('#calendar').fullCalendar('removeEvents');
                            $('#calendar').fullCalendar('addEventSource',eventsFromActivities);    
                            $(".fc-button-refresh img").attr("src",'../web/it.openia.crm/images/arrow_rotate_anticlockwise.png');
                        });
                    
                    else
                    //prev month
                    $.ajax({
                        url: "../it.openia.crm.GetEvents",
                        data: {
                            from: view.start, 
                            to: view.end,
                            all:"Y"
                        } 
                    }).done(function(data) { 
                        eventsFromActivities=data.events;
                        
                        $('#calendar').fullCalendar('removeEvents');
                        $('#calendar').fullCalendar('addEventSource',eventsFromActivities);    
                    	$('.fc-button-getAll span').removeClass("fc-state-down");
                    });
                    
                };
                
                var refreshCalendarWithClosedEvents = function() {
                    view = $('#calendar').fullCalendar('getView');
                    //prev month
                    $.ajax({
                        url: "../it.openia.crm.GetEvents",
                        data: {
                            from: view.start, 
                            to: view.end,
                            all:"Y"
                        } 
                    }).done(function(data) { 
                        eventsFromActivities=data.events;
                
                        $('#calendar').fullCalendar('removeEvents');
                        $('#calendar').fullCalendar('addEventSource',eventsFromActivities);    
                    	$('.fc-button-getAll span').removeClass("fc-state-down");
                    });
                };
                
                //http://arshaw.com/fullcalendar/docs/views/View_Object/
                $('.fc-button-prev span, .fc-button-next span, .fc-button-prevYear span, .fc-button-nextYear span, .fc-button-today span, .fc-button-month span, .fc-button-agendaWeek span, fc-button-agendaDay span').live('click',function(){
                    refreshCalendar();
                });
        
                $('.fc-button-refresh span').live('click',function(){
                    $(".fc-button-refresh img").attr("src",'../web/it.openia.crm/images/arrow_rotate_clockwise.png');
                    allevents=false;
                    refreshCalendar();
                });
                
                $('.fc-button-getAll span').live('click',function(){
                    $('.fc-button-getAll span').addClass("fc-state-down");
                    allevents=true;
                    refreshCalendar();
                });
                
                $('.fc-button-menu span').live('click',function(){
                    window.location.href="../it.openia.crm/";
                });
       
                $('#eventDiv').keypress(function(e) {
                    var match = /^desc_attivita$/;
				
                    if(e.which == 13 & !match.test($("*:focus").attr('name'))) {
                        $.ajax({
                            url: "../it.openia.crm.InsertEvent",
                            data: {
                                from: $('input[name="start_attivita"]').val(), 
                                info: $('input[name="info_attivita"]').val(),
                                descr: $('textarea[name="desc_attivita"]').val(),
                                enddate: $('input[name="end_attivita"]').val(),
                                id: $('#activitydialog').attr('evt_id'),
                                idtype:$('select[name="tipo_attivita"]').attr('value'),
                                plannext:'N',
                                statName:$('select[name="lead_status"]').attr('value'),
                                actStatus: getActualActivityStatus($('select[name="tipo_attivita"]').attr('value')),
                                leadId:$('select[name="lead_dropdownlist"]').attr('value')
                            } 
                        }).done(function(data) { 
                            if(data.match(/OK/)){
                                $('#activitydialog').dialog( 'close' );
                                refreshCalendar();
                            }
                            else
                                alert(data);
                        });
                    }        
                });
            
                $('#activitydialog').on( "dialogclose", function( event, ui ) {
            
                    $('#activitydialog').attr('evt_id','');
                    $('input[name="info_attivita"]').val('');
                    $('textarea[name="desc_attivita"]').val('');
                    $('input[name="start_attivita"]').val('');
                    $('input[name="end_attivita"]').val('');
                    $('select[name="tipo_attivita"]').val('');
                    $('select[name="lead_status"]').val('');
                    $('select[name="activity_status"]').val('');
                    $('#actOBLink').attr('href','');
                    
                    $('select[name="lead_dropdownlist"]').val('');
                    $('#LeadSelect').hide();
                    
                    $('#partnerDiv').empty();
                    $('#leadStatusRow').hide();
                    $('#ActivityStatus').hide();
                    
                    $('input[name="info_next_attivita"]').val('');
                    $('textarea[name="desc_next_attivita"]').val('');
                    $('input[name="next_activity"]').val('');
                    $('input[name="next_activity_end"]').val('');
                    $('select[name="tipo_attivita_next"]').val('');
                    $('#partnerDivNextEvt').empty();
            		
                    $('#ActivityStatusNext').hide();
            		
                    $('#eventDiv').show();
                    $('#ConfirmationDiv').hide();
                    $('#RescheduleDiv').hide();
                } );    
        
                $('#evtSaveBtn').live('click',function(){
                    $.ajax({
                        url: "../it.openia.crm.InsertEvent",
                        data: {
                            from: $('input[name="start_attivita"]').val(), 
                            info: $('input[name="info_attivita"]').val(),
                            descr: $('textarea[name="desc_attivita"]').val(),
                            enddate: $('input[name="end_attivita"]').val(),
                            id: $('#activitydialog').attr('evt_id'),
                            idtype:$('select[name="tipo_attivita"]').attr('value'),
                            plannext:'N',
                            statName:$('select[name="lead_status"]').attr('value'),
                            actStatus: getActualActivityStatus($('select[name="tipo_attivita"]').attr('value')),
                            leadId:$('select[name="lead_dropdownlist"]').attr('value')
                        } 
                    }).done(function(data) { 
                        if(data.match(/OK/)){
                            $('#activitydialog').dialog( 'close' );
                            refreshCalendar();
                        }
                        else
                          alert(data);  
                    });
                });
        	
        
                $('#evtDeleteBtn').live('click',function(){
                    $('#eventDiv').hide();
                    $('#ConfirmationDiv').show();
                }); 
 				
 				$('#evtRescheduleBtn').live('click',function(){
 					$('#eventDiv').hide();
                                        $('#actOBLink').hide();
 					$('#RescheduleDiv').show();
 				});
                
                $('#evtDoneAndNextBtn').live('click',function(){
                    
                    $.ajax({
                        url: "../it.openia.crm.InsertEvent",
                        data: {
                            from: $('input[name="start_attivita"]').val(), 
                            info: $('input[name="info_attivita"]').val(),
                            descr: $('textarea[name="desc_attivita"]').val(),
                            enddate: $('input[name="end_attivita"]').val(),
                            id: $('#activitydialog').attr('evt_id'),
                            idtype:$('select[name="tipo_attivita"]').attr('value'),
                            plannext:'N',
                            statName:$('select[name="lead_status"]').attr('value'),
                            actStatus: getActualActivityStatus($('select[name="tipo_attivita"]').attr('value')),
                            eventDone:'Y'
                        } 
                    }).done(function(data) { 
                        
                        if(data.match(/OK/)){
                            refreshCalendar();
                            $('#eventDiv').hide();
                            $('#actOBLink').hide();
                            $('#RescheduleDiv').show();
                        }
                        else
                          alert(data);  
                    });
                    
                 });                 

 				
                $('#backToEvt').live('click',function(){
                    $('#eventDiv').show();
                    $('#ConfirmationDiv').hide();
                }); 
	   			
	   			$('#backToEvtFromAct').live('click',function(){
	   				$('#eventDiv').show();
                                        $('#actOBLink').show();
	   				$('#RescheduleDiv').hide();
	   			});
	   
                $('#confirmDeleteEvt').live('click',function(){
           
                    $.ajax({
                        url: "../it.openia.crm.DeleteEvent",
                        data: {
                            id: $('#activitydialog').attr('evt_id')
                        } 
                    }).done(function(data) { 
                        $('#activitydialog').dialog( 'close' );
                        refreshCalendar();
                    });
       		
                });
	   			
              $('#confirmNextActEvt').live('click',function(){
	   				
	   				$.ajax({
                        url: "../it.openia.crm.InsertEvent",
                        data: {
                            from: $('input[name="next_activity"]').val(), 
                            info: $('input[name="info_next_attivita"]').val(),
                            descr: $('textarea[name="desc_next_attivita"]').val(),
                            enddate: $('input[name="next_activity_end"]').val(),
                            id: $('#activitydialog').attr('evt_id'),
                            idtype:$('select[name="tipo_attivita_next"]').attr('value'),
                            plannext:'Y',
                            statName:$('select[name="lead_status"]').attr('value'),
                            actStatus: getActualActivityStatusNext($('select[name="tipo_attivita_next"]').attr('value'))
                        } 
                    }).done(function(data) { 
                        $('#activitydialog').dialog( 'close' );
                        refreshCalendar();
                    });
	   			});	
	   
            });

        </script>
    </head>
    <body onload="resizeCalendarDiv()" onresize="resizeCalendarDiv()">
        <div id="activitydialog" evt_id="" style="background: rgb(234, 245, 214);font-size: 16px;">

            <div id="eventDiv">
                <center>
                    
                    <div id="partnerDiv"></div>
                    <table>
                        <tr>

                            <td><span><%=GetEvents.GetField("C0983A77EEF145FD98D032500C43C6A8")%>*:</span></td>
                            <td><span><input type="text" name="info_attivita" placeholder="nome" autofocus></span>
                                <a style="padding-bottom: 5px;padding-left: 5px;" href="" id="actOBLink" target="_blank">
                                    <img src="../web/it.openia.crm/images/link_go.png" alt="open activity window in Openbravo" title="open activity window in Openbravo"/></a>
                            </td>

                        </tr>
                        <tr>    
                            <td><span><%=GetEvents.GetField("B7086C8DB5044690B058D61BFEF62E5A")%>*:</span></td>  <td><span>
                            							  	<div class="well">
  																<div id="datetimepicker1" class="input-append date">			
                            										<input name="start_attivita" placeholder="dd/mm/yyyy HH:mm" data-format="dd/MM/yyyy hh:mm" type="text"></input>
                            										<span class="add-on">
      																<i data-time-icon="icon-time" data-date-icon="icon-calendar">
      																</i></span>
      													  		</div>
      													  	</div>
      													  	<script type="text/javascript">
  																$(function() {
    																$('#datetimepicker1').datetimepicker({
      																	language: '<%=GetEvents.GetLocalization()%>'
    																});
  																});
															</script>
															</span>
															</td>
                        </tr>    
                        <tr>
                            <td><span><%=GetEvents.GetField("0498AD39A62342518CAA1A35FF659322")%>:</span></td>  <td><span>
                            								<div class="well">
  																<div id="datetimepicker2" class="input-append date">			
                            										<input name="end_attivita" placeholder="dd/mm/yyyy HH:mm" data-format="dd/MM/yyyy hh:mm" type="text"></input>
                            										<span class="add-on">
      																<i data-time-icon="icon-time" data-date-icon="icon-calendar">
      																</i></span>
      													  		</div>
      													  	</div>
      													  	<script type="text/javascript">
  																$(function() {
    																$('#datetimepicker2').datetimepicker({
      																	language: '<%=GetEvents.GetLocalization()%>'
    																});
  																});
															</script>
                            							</span></td>
                        </tr>
                        <tr>                
                            <td><span><%=GetEvents.GetField("E57398E301A44BB3B7CC5457C37F6145")%>:</span></td>  
                            <!-- <td><span><input type="text" name="desc_attivita"></span></td> -->
                            <td><span><center><textarea style="height:40px;width:233px;resize: none;overflow-y:auto;" name="desc_attivita"></textarea></center></span></td>

                        </tr>
                        <tr id="LeadSelect">
                            <td><span>Lead:</span></td>  <td><span>
                                    <select name="lead_dropdownlist" style="width: 247px;">
				    </select>
                                </span></td>
                        </tr>
                        <tr>
                            <td><span><%=GetEvents.GetField("EE834F13B27F4B0D8262C3DBAEDCD9A8")%>:</span></td>  <td><span>
                                    <select name="tipo_attivita" style="width: 163px;">
									</select>
                                </span></td>
                        </tr>
                        <tr id="ActivityStatus">
                            <td><span><%=GetEvents.GetField("97833219C3884345A87819FE15C5AAEE")%>:</span></td>  <td><span>
                                    <select name="activity_status" style="width: 163px;">
                                    </select>
                                </span></td>
                        </tr> 
                         <tr id="leadStatusRow">
                            <td><span><%=GetEvents.GetField("C1C8461D35D141DD8D45F8FF1A606840")%>:</span></td>  <td><span>
                                    <select name="lead_status" style="width: 163px;">
                                    </select>
                                </span></td>
                        </tr>
                    	
                    </table>

                    <br/>
                    <button id="evtSaveBtn" title="save activity" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img src="../web/it.openia.crm/images/disk.png"/>
                    </button>
                    &nbsp;&nbsp;
                    <button id="evtDeleteBtn" title="delete activity" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img src="../web/it.openia.crm/images/delete.png"/>
                    </button>
                    &nbsp;&nbsp;
                    <button id="evtRescheduleBtn" title="next activity" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img src="../web/it.openia.crm/images/calendar_edit.png"/>
                    </button>
                    &nbsp;&nbsp;
                    <button id="evtDoneAndNextBtn" title="Set activity as Done and book next activity" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img src="../web/it.openia.crm/images/date_go.png"/>
                    </button>
                </center>    
            </div>

            <div id="ConfirmationDiv">
                <center>
                    Delete Activity?
                    &nbsp;<button id="confirmDeleteEvt" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:46px;">Yes</button>
                    &nbsp;&nbsp;&nbsp;<button id="backToEvt" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:46px;">No</button>	
                </center>
            </div>
			
			<div id="RescheduleDiv">
                <center>
                    <div id="partnerDivNextEvt"></div>
                    <table>
                    	
                    	<tr>

                            <td><span><%=GetEvents.GetField("C0983A77EEF145FD98D032500C43C6A8")%>*:</span></td>  <td><span><input type="text" name="info_next_attivita" placeholder="nome"></span></td>

                        </tr>
                        <tr>

                            <td><span><%=GetEvents.GetField("B7086C8DB5044690B058D61BFEF62E5A")%>*:</span></td>  <td><span>
                            								<div class="well">
  																<div id="datetimepicker3" class="input-append date">			
                            										<input name="next_activity" placeholder="dd/mm/yyyy HH:mm" data-format="dd/MM/yyyy hh:mm" type="text"></input>
                            										<span class="add-on">
      																<i data-time-icon="icon-time" data-date-icon="icon-calendar">
      																</i></span>
      													  		</div>
      													  	</div>
      													  	<script type="text/javascript">
  																$(function() {
    																$('#datetimepicker3').datetimepicker({
      																	language: '<%=GetEvents.GetLocalization()%>'
    																});
  																});
															</script>
                            							</span></td>

                        </tr>
                        
                        <tr>
                        
                        	<td><span><%=GetEvents.GetField("0498AD39A62342518CAA1A35FF659322")%>:</span></td>  <td><span>
                        									<div id="datetimepicker4" class="input-append date">			
                            										<input name="next_activity_end" placeholder="dd/mm/yyyy HH:mm" data-format="dd/MM/yyyy hh:mm" type="text"></input>
                            										<span class="add-on">
      																<i data-time-icon="icon-time" data-date-icon="icon-calendar">
      																</i></span>
      													  		</div>
      													  	</div>
      													  	<script type="text/javascript">
  																$(function() {
    																$('#datetimepicker4').datetimepicker({
      																	language: '<%=GetEvents.GetLocalization()%>'
    																});
  																});
															</script>
                        							      </span></td>
                        
                        </tr>
                        <tr>

                            <td><span><%=GetEvents.GetField("E57398E301A44BB3B7CC5457C37F6145")%>:</span></td>  
                            <td><span><center><textarea style="height:40px;width:233px;resize: none;overflow-y:auto;" name="desc_next_attivita"></textarea></center></span></td>

                        </tr>
                    	 
                        <tr>
                            <td><span><%=GetEvents.GetField("EE834F13B27F4B0D8262C3DBAEDCD9A8")%>:</span></td>  <td><span>
                                    <select name="tipo_attivita_next" style="width: 163px;">

                                    </select>
                                </span></td>
                        </tr>
                        
                        <tr id="ActivityStatusNext">
                            <td><span><%=GetEvents.GetField("97833219C3884345A87819FE15C5AAEE")%>:</span></td>  <td><span>
                                    <select name="activity_status_next" style="width: 163px;">
                                    </select>
                                </span></td>
                        </tr>
                        
                    </table>
                    
                    </br>
                    &nbsp;<button id="confirmNextActEvt" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:46px;">Ok</button>
                    &nbsp;&nbsp;&nbsp;<button id="backToEvtFromAct" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:60px;">Cancel</button>	
                </center>
            </div>
			
        </div>


        <hr size="1px">
        <div id='calendar' style='font-size: 13px; overflow-y: auto'></div>
    </body>
</html>