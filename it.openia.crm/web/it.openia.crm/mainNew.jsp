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
    <!-- <link rel="stylesheet" type='text/css' href='http://code.jquery.com/ui/1.10.0/themes/base/jquery-ui.css' /> -->
    <link rel="stylesheet" type='text/css' href='../web/it.openia.crm/css/jquery-ui.css' />
    <link rel="stylesheet" type='text/css' href='../web/it.openia.crm/css/bootstrap-datetimepicker.min.css' />
    <link rel="stylesheet" type='text/css' href="../web/it.openia.crm/css/bootstrap-combined.min.css">
    <link rel="stylesheet" type='text/css' href="../web/it.openia.crm/css/jquery.smallipop.css" media="all"
        title="Screen">
    <link rel="stylesheet" type='text/css' href="../web/it.openia.crm/css/animate.min.css" media="all" title="Screen">

    <style type="text/css">
        body {
        margin: 0;
        padding: 0;
        }

        .header {
        text-align: center;
        }

        .ui-widget input,
        .ui-widget select,
        .ui-widget textarea,
        .ui-widget button {
            font-family: Verdana,
            Arial,
            sans-serif
            /*{ffDefault}*/
            ;
            font-size: small;
        }

        .ui-dialog .ui-dialog-titlebar {
            padding: .1em 1em;
        }

        .ui-widget-header {
            background: rgb(234, 245, 214);
            border: 0px;
        }

        .ui-dialog .ui-dialog-title {
            font-family: 'lucida sans', sans-serif;
            font-size: 16px;
            font-weight: 700;
        }

        a:hover,
        a:active {
            outline: 0px;
            color: #005580;
            text-decoration: none;
        }

        /* Opportunity Event Color */
        .Opportunity,
        .Opportunity div,
        .Opportunity span {
            background-color: green;
            /* background color */
            border-color: green;
            /* border color */
            color: white;
            /* text color */
        }

        /*Closed Event color*/
        .Closed,
        .Closed div,
        .Closed span {
            background-color: grey;
            /* background color */
            border-color: grey;
            /* border color */
            color: white;
            /* text color */
        }
        #calendar {
        max-width: 100%;
        margin: 0 auto;
        height: 100vh;
        }
    </style>

    <script type='text/javascript' src='../web/it.openia.crm/js/jquery-1.8.1.min.js'></script>
    <script type='text/javascript' src='../web/it.openia.crm/js/jquery-ui-1.8.23.custom.min.js'></script>
    <script type='text/javascript' src="../web/it.openia.crm/js/jquery-1.8.3.js"></script>

    <script type='text/javascript' src="../web/it.openia.crm/js/modernizr.js"></script>
    <script type='text/javascript' src="../web/it.openia.crm/js/jquery.smallipop.js"></script>

    <script language="JavaScript" src="../web/js/utils.js" type="text/javascript"></script>

    <!--
        <script data-main="../web/it.openia.crm/js/main.js" src="../web/it.openia.crm/js/contrib/require.min.js" type="text/javascript"></script>
        -->

    <!-- <script type="text/javascript" src="http://netdna.bootstrapcdn.com/twitter-bootstrap/2.2.2/js/bootstrap.min.js"> </script>  -->
    <script type="text/javascript" src="../web/it.openia.crm/js/bootstrap.min.js"> </script>

    <script type='text/javascript' src="../web/it.openia.crm/js/jquery-ui.js"></script>


    <script type='text/javascript'
        src="../web/it.openia.crm/js/fullcalendar_<%=GetEvents.GetLocalization()%>.min.js"></script>
    <script type='text/javascript' src='../web/it.openia.crm/js/jquery.qtip.min.js'></script>

    <script type='text/javascript' src='../web/it.openia.crm/js/bootstrap-datetimepicker.min.js'></script>

    <script type="text/javascript" src="../web/it.openia.crm/js/bootstrap-datetimepicker.en-US.js"></script>
    <script type="text/javascript" src="../web/it.openia.crm/js/bootstrap-datetimepicker.it-IT.js"></script>
    <script type="text/javascript" src="../web/it.openia.crm/js/bootstrap-datetimepicker.es-ES.js"></script>

    <script type='text/javascript'>

        function resizeCalendarDiv() {
            $("#calendar").css("height", document.body.parentNode.clientHeight);
        }

        function openWindowEntry(tabId, recordId) {
            var myframe = getFrame('LayoutMDI') || top.opener;
            if (myframe) {
                myframe.OB.Utilities.openDirectTab(tabId, recordId);
            }
        };

        $(document).ready(function () {
            $('#activitydialog').hide();
            $('#ConfirmationDiv').hide();
            $('#RescheduleDiv').hide();
            $('#leadStatusRow').hide();
            $('#partner').hide();
            $('#assignedto').hide();
            var eventsFromActivities;
            var activityTypes;
            var leadList;
            var statuses;
            var view;
            var insertDate;
            var evento;
            var event_qtip_id = '';
            var allevents = false;
            var filterActive = true;

            var activity_types='';
            var lead_status='';
            var activity_status='';
            var partner_id='';
            var assigned_to='';

            //shows the activity status dropdownlist when an activity type is chosen on the 'activitydialog' popup
            var setActivityStatuses = function (strActId) {

                if (filterActive==false) {
                    $('#evtDoneAndNextBtn').show();
                } else {
                    $('#evtDoneAndNextBtn').hide();
                }

                $('select[name="activity_status"]').empty();

                $('select[name="activity_status"]').append($('<option/>').attr("value", 0).text(''));
                var count = 1;
                var arr = activityTypes[strActId - 1].activityStatusList;
                $.each(activityTypes[strActId - 1].activityStatusList, function (i, option) {
                    if (option.defaultStatus)
                        $('select[name="activity_status"]').append($('<option/>').attr("value", count).attr("selected", true).text(option.statusTranslation));
                    else
                        $('select[name="activity_status"]').append($('<option/>').attr("value", count).text(option.statusTranslation));
                    count++;
                });

                $('#ActivityStatus').show();

            };

            //shows the activity status dropdownlist for the 'next activity' editing window when an activity is chosen on the 'activitydialog' popup
            var setActivityStatusesNext = function (strActId) {

                $('select[name="activity_status_next"]').empty();

                $('select[name="activity_status_next"]').append($('<option/>').attr("value", 0).text(''));
                var count = 1;
                var arr = activityTypes[strActId - 1].activityStatusList;
                $.each(activityTypes[strActId - 1].activityStatusList, function (i, option) {
                    if (option.defaultStatus)
                        $('select[name="activity_status_next"]').append($('<option/>').attr("value", count).attr("selected", true).text(option.statusTranslation));
                    else
                        $('select[name="activity_status_next"]').append($('<option/>').attr("value", count).text(option.statusTranslation));

                    count++;
                });
                if (filterActive==false) {
                    $('#ActivityStatusNext').show();
                } else {
                    $('#ActivityStatusNext').hide();
                }
            };

            //when loading an event on the activitydialog popup, this method sets the value of the activity status type on the dropdownlist
            var setValueOnStatusId = function (strActId, statusId) {
                var statusArray = activityTypes[strActId - 1].activityStatusList;
                for (index in statusArray) {
                    if (statusArray[index].statusKey == statusId) {
                        var i = parseInt(index) + 1;
                        $('select[name="activity_status"]').val(i);
                        return;
                    }
                }
            }

            //when saving the event that has been edited on the 'activitydialog' popup this method gets the activity status type Id
            var getActualActivityStatus = function (strActId) {
                var statusIndex = $('select[name="activity_status"]').attr('value');
                if (statusIndex != 0) {
                    var a = activityTypes[strActId - 1];
                    var b = a.activityStatusList[statusIndex - 1].statusKey;
                    return activityTypes[strActId - 1].activityStatusList[statusIndex - 1].statusKey;
                }
                else "";
            }

            //same as getActualActivityStatus, gets called when saving the 'next activity' 
            var getActualActivityStatusNext = function (strActId) {
                var statusIndex = $('select[name="activity_status_next"]').attr('value');
                if (statusIndex != 0) {
                    var a = activityTypes[strActId - 1];
                    var b = a.activityStatusList[statusIndex - 1].statusKey;
                    return activityTypes[strActId - 1].activityStatusList[statusIndex - 1].statusKey;
                }
                else "";
            }

            //on each activity type dropdownlist selection, this method sets the related activity status dropdownlist,
            //hides the status dropdownlist if the selected activity type is null
            $('select[name="tipo_attivita"]').change(function () {
                var selection = $('select[name="tipo_attivita"]').attr('value');

                //next activity gets same type by default
                $('select[name="tipo_attivita_next"]').val(selection);

                if (selection == 0) {
                    $('#ActivityStatus').hide();
                    $('select[name="activity_status"]').empty();
                    $('#evtDoneAndNextBtn').hide();

                    //next activity gets previous same type by default
                    $('#ActivityStatusNext').hide();
                    $('select[name="activity_status_next"]').empty();
                }
                else {
                    setActivityStatuses(selection);
                    setActivityStatusesNext(selection);
                }
            });

            //same as above but for the 'next activity' window
            $('select[name="tipo_attivita_next"]').change(function () {
                var selection = $('select[name="tipo_attivita_next"]').attr('value');

                if (selection == 0) {
                    $('#ActivityStatusNext').hide();
                    $('select[name="activity_status_next"]').empty();
                }
                else
                    setActivityStatusesNext(selection);

            });

            //ajax call that retrieves the events and initializes the fullcalendar object
            $.ajax({
                url: "../it.openia.crm.GetEvents"
            }).done(function (data) {
                eventsFromActivities = data.events;
                activityTypes = data.actTypes;
                statuses = data.leadStatuses;
                leadList = data.leadsList;
                partnerList = data.partnerList;
                assignedtoList = data.assignedtoList;

                $('select[name="tipo_attivita"]').append($('<option/>').attr("value", 0).text(''));

                $('select[name="tipo_attivita_next"]').append($('<option/>').attr("value", 0).text(''));

                $('select[name="lead_status"]').append($('<option/>').attr("value", 0).text(''));

                $('select[name="partner_list"]').append($('<option/>').attr("value", 0).text(''));

                $('select[name="assignedto_list"]').append($('<option/>').attr("value", 0).text(''));

                //$('select[name="lead_dropdownlist"]').append($('<option/>').attr("value", 0).text(''));

                $.each(activityTypes, function (i, option) {
                    $('select[name="tipo_attivita"]').append($('<option/>').attr("value", option.id + 1).text(option.name));
                });

                $.each(activityTypes, function (i, option) {
                    $('select[name="tipo_attivita_next"]').append($('<option/>').attr("value", option.id + 1).text(option.name));
                });

                $.each(statuses, function (i, option) {
                    $('select[name="lead_status"]').append($('<option/>').attr("value", option.id + 1).text(option.name));

                });

                $.each(partnerList, function (i, option) {
                    $('select[name="partner_list"]').append($('<option/>').attr("value", option.id).text(option.name));

                });

                $.each(assignedtoList, function (i, option) {
                    $('select[name="assignedto_list"]').append($('<option/>').attr("value", option.id).text(option.name));

                });
                /*
            var arrOfLeadChoices = [];
            $.each(leadList, function(i,option){
                var obj;
            	
                if(option.commercialname!=undefined && option.commercialname.length>0)
                    obj = {label:option.firstname+" "+option.lastname+" - "+option.commercialname, value:option.firstname+" "+option.lastname, id:option.id};
                else
                    obj = {label:option.firstname+" "+option.lastname, value:option.firstname+" "+option.lastname, id:option.id};
            	
                arrOfLeadChoices.push(obj);
            });
            */
                $('input[name="leads_select"]').autocomplete({
                    source: "../it.openia.crm.GetLeadsForCalendar",
                    minLength: 2,
                    delay: 500,
                    change: function (event, ui) {
                        if (ui.item != null && ui.item != undefined)
                            $('input[name="leads_select"]').attr('idlead', ui.item.id);
                        else
                            $('input[name="leads_select"]').attr('idlead', '');
                    }
                });


                $('#calendar').fullCalendar({
                    height: 450,
                    firstDay: 1,
                    header: {
                        left: 'prev,next today',
                        right: 'month,agendaWeek,agendaDay'
                    },
                    editable: true,
                    events: eventsFromActivities,
                    timeFormat: 'H:mm',
                    axisFormat: 'H:mm',
                    viewDisplay: function(view) {
                        $('#title').text(view.title);                   
                    },
                    eventRender: function (event, element, calEvent) {
                        element.find(".fc-event-time").before($("<span class=\"fc-event-icons\"></span>")
                            .html(event.imgText));

                        $('.userPopUpInfo').smallipop();
                    },

                    eventDrop: function (event, dayDelta, minuteDelta, allDay, revertFunc) {

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
                    eventResize: function (event, dayDelta, minuteDelta, revertFunc) {

                        $.ajax({
                            url: "../it.openia.crm.ModifyEvents",
                            data: {
                                id: event.id,
                                dayDt: dayDelta,
                                minuteDt: minuteDelta
                            }

                        });
                    },

                    eventClick: function (event) {
                        filterActive = false;
                        var d_start_title = $.fullCalendar.formatDate(event.start, "dd/MM/yyyy");
                        var d_start = $.fullCalendar.formatDate(event.start, "dd/MM/yyyy HH:mm");
                        var d_end = $.fullCalendar.formatDate(event.end, "dd/MM/yyyy HH:mm");
                        activity_types='';
                        lead_status='';
                        activity_status='';
                        partner_id='';
                        assigned_to='';

                        $('#subjectActivity').show();
                        $('#startDateActivity').show();
                        $('#endDateActivity').show();
                        $('#TextAreaCalendar').show();
                        $('#evtFilterBtn').hide();
                        $('#evtSaveBtn').show();


                        $('#actOBLink').show();
                        $('#actOBLink').attr('onclick', "openWindowEntry('046D51DD5D4840C58C36AA09C776F0E1', '" + event.id + "');return false;");

                        $('#ActivityStatusNext').hide();
                        $('#partner').hide();
                        $('#assignedto').hide();

                        if (event.actType > 0) {
                            setActivityStatuses(event.actType);
                            setValueOnStatusId(event.actType, event.actTypeStatusId);

                            setActivityStatusesNext(event.actType);
                        }
                        else
                            $('#ActivityStatus').hide();

                        $('#partnerDiv').empty();
                        if (event.bpName.length != 0 | event.leadName.length != 0) {
                            if (event.bpName.length != 0)
                                $('#partnerDiv').append(event.bpName + "<br/>");
                            if (event.leadName.length != 0)
                                $('#partnerDiv').append("<div style='font-size:13px;'>" + event.leadName + "<br/><br/><\/div>");

                            $('#leadStatusRow').show();
                            $('select[name="lead_status"]').val(event.statName);
                        }
                        else
                            $('#leadStatusRow').hide();

                        $('#partnerDivNextEvt').empty();
                        if (event.bpName.length != 0 | event.leadName.length != 0) {
                            if (event.bpName.length != 0)
                                $('#partnerDivNextEvt').append(event.bpName + "<br/>");
                            if (event.leadName.length != 0)
                                $('#partnerDivNextEvt').append("<div style='font-size:13px;'>" + event.leadName + "<br/><br/><\/div>");
                        }

                        $('#activitydialog').dialog({ title: d_start_title, minWidth: (screen.width) / 3, minHeight: 100 });
                        $('#activitydialog').attr('evt_id', event.id);

                        $('input[name="info_attivita"]').val(event.subject);
                        $('input[name="info_next_attivita"]').val(event.subject),
                            $('textarea[name="desc_attivita"]').val(event.description);
                        $('input[name="start_attivita"]').val(d_start);
                        $('input[name="end_attivita"]').val(d_end);
                        $('select[name="tipo_attivita"]').val(event.actType);
                        $('select[name="tipo_attivita_next"]').val(event.actType);

                        $('input[name="leads_select"]').val('');
                        $('input[name="leads_select"]').attr('idlead', '');
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

                    dayClick: function (date, allDay, jsEvent, view) {
                        filterActive = false;
                        activity_types='';
                        lead_status='';
                        activity_status='';
                        partner_id='';
                        assigned_to='';
                        
                        $('#partnerDiv').empty();
                        $('#leadStatusRow').hide();
                        $('#ActivityStatus').hide();
                        $('#actOBLink').hide();
                        $('#partner').hide();
                        $('#evtFilterBtn').hide();
                        $('#assignedto').hide();
                        $('#evtSaveBtn').show();

                        $('#subjectActivity').show();
                        $('#startDateActivity').show();
                        $('#endDateActivity').show();
                        $('#TextAreaCalendar').show();

                        if (!$('#activitydialog').is(':visible')) {
                            $('input[name="leads_select"]').val('');
                            $('#LeadSelect').show();

                            var d = date.getDate() + "/" + (date.getMonth() + 1).toString() + "/" + date.getFullYear();
                            $('#activitydialog').dialog({ title: d, minWidth: (screen.width) / 3, minHeight: 100 });
                            var date_default = $.fullCalendar.formatDate(date, "dd/MM/yyyy HH:mm");
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
                        else {
                            $('#activitydialog').dialog('close');
                        }

                    }
                });

                var refresh_button = '&nbsp;&nbsp<span class="fc-button fc-state-default fc-corner-left fc-corner-right fc-button-refresh" title="ver actividades abiertas"><span class="fc-button-inner"><span class="fc-button-content"><img src="../web/it.openia.crm/images/arrow_rotate_anticlockwise.png" style="position:relative;top:4px"/><\/span><span class="fc-button-effect"><span/><\/span><\/span><\/span>';
                var getAllEvents_button = '&nbsp;&nbsp<span class="fc-button fc-state-default fc-corner-left fc-corner-right fc-button-getAll" title="incluir eventos cerrados"><span class="fc-button-inner"><span class="fc-button-content"><img src="../web/it.openia.crm/images/funnel-icon.png" style="position:relative;top:4px"/><\/span><span class="fc-button-effect"><span/><\/span><\/span><\/span>';
                var getFilterEvents_button = '&nbsp;&nbsp<span class="fc-button fc-state-default fc-corner-left fc-corner-right fc-button-getbyFilter" title="filtrar actividades"><span class="fc-button-inner"><span class="fc-button-content"><img src="../web/it.openia.crm/images/filter.svg" width="16" height="16" style="position:relative;top:4px"/><\/span><span class="fc-button-effect"><span/><\/span><\/span><\/span>';
                var menu_button = '&nbsp;&nbsp;<span class="fc-button fc-state-default fc-corner-left fc-corner-right fc-button-menu"><span class="fc-button-inner"><span class="fc-button-content">Menu<\/span><span class="fc-button-effect"><span/><\/span><\/span><\/span>';
                if (data.showMenu)
                    $('.fc-header-left').append(refresh_button).append(getAllEvents_button).append(getFilterEvents_button).append(menu_button);
                else $('.fc-header-left').append(refresh_button).append(getAllEvents_button).append(getFilterEvents_button);
            });

            var refreshCalendar = function () {
                view = $('#calendar').fullCalendar('getView');

                if (allevents == false)
                    //prev month
                    $.ajax({
                        url: "../it.openia.crm.GetEvents",
                        data: {
                            from: view.start,
                            to: view.end,
                            all: "N",
                            isFilter: "N",
                            // idtype: $('select[name="tipo_attivita"]').attr('value'),
                            // leadId: $('select[name="lead_status"]').attr('value'),
                            // activityStatus: getActualActivityStatus($('select[name="tipo_attivita"]').attr('value')),
                            // partnerId: $('select[name="partner_list"]').attr('value'),
                            // assignedtoId: $('select[name="assignedto_list"]').attr('value')
                        }
                    }).done(function (data) {
                        eventsFromActivities = data.events;

                        $('#calendar').fullCalendar('removeEvents');
                        $('#calendar').fullCalendar('addEventSource', eventsFromActivities);
                        $(".fc-button-refresh img").attr("src", '../web/it.openia.crm/images/arrow_rotate_anticlockwise.png');
                    });

                else
                    //prev month
                    $.ajax({
                        url: "../it.openia.crm.GetEvents",
                        data: {
                            from: view.start,
                            to: view.end,
                            all: "Y",
                            isFilter: "N",
                            // idtype: $('select[name="tipo_attivita"]').attr('value'),
                            // leadId: $('select[name="lead_status"]').attr('value'),
                            // activityStatus: getActualActivityStatus($('select[name="tipo_attivita"]').attr('value')),
                            // partnerId: $('select[name="partner_list"]').attr('value'),
                            // assignedtoId: $('select[name="assignedto_list"]').attr('value')
                        }
                    }).done(function (data) {
                        eventsFromActivities = data.events;

                        $('#calendar').fullCalendar('removeEvents');
                        $('#calendar').fullCalendar('addEventSource', eventsFromActivities);
                        $('.fc-button-getAll span').removeClass("fc-state-down");
                    });

            };

            var refreshCalendarWithClosedEvents = function () {
                view = $('#calendar').fullCalendar('getView');
                //prev month
                $.ajax({
                    url: "../it.openia.crm.GetEvents",
                    data: {
                        from: view.start,
                        to: view.end,
                        all: "Y",
                        isFilter: "N",
                    }
                }).done(function (data) {
                    eventsFromActivities = data.events;

                    $('#calendar').fullCalendar('removeEvents');
                    $('#calendar').fullCalendar('addEventSource', eventsFromActivities);
                    $('.fc-button-getAll span').removeClass("fc-state-down");
                });
            };

            //http://arshaw.com/fullcalendar/docs/views/View_Object/
            $('.fc-button-prev span, .fc-button-next span, .fc-button-prevYear span, .fc-button-nextYear span, .fc-button-today span, .fc-button-month span, .fc-button-agendaWeek span, fc-button-agendaDay span').live('click', function () {
                refreshCalendar();
            });

            $('.fc-button-refresh span').live('click', function () {
                $(".fc-button-refresh img").attr("src", '../web/it.openia.crm/images/arrow_rotate_clockwise.png');
                allevents = false;
                refreshCalendar();
            });

            $('.fc-button-getAll span').live('click', function () {
                $('.fc-button-getAll span').addClass("fc-state-down");
                allevents = true;
                refreshCalendar();
            });

            $('.fc-button-getbyFilter span').live('click', function () {
                filterActive = true;
                $('#activitydialog').dialog({ title: 'Filtro de Actividades', minWidth: (screen.width) / 3, minHeight: 100 });
                $('#leadStatusRow').show();                
                $('#partner').show();
                $('#assignedto').show();
                $('#evtFilterBtn').show();
                $('select[name="tipo_attivita"]').val('');
                $('select[name="lead_status"]').val('');
                $('select[name="activity_status"]').val('');
                $('select[name="partner_list"]').val('');
                $('select[name="assignedto_list"]').val('');
                $('#evtDeleteBtn').hide();
                $('#evtRescheduleBtn').hide();
                $('#evtDoneAndNextBtn').hide();
                $('#evtSaveBtn').hide();
                $('#subjectActivity').hide();
                $('#startDateActivity').hide();
                $('#endDateActivity').hide();
                $('#TextAreaCalendar').hide();
                $('#LeadSelect').hide();
            });

            $('.fc-button-menu span').live('click', function () {
                window.location.href = "../it.openia.crm/";
            });

            $('#eventDiv').keypress(function (e) {
                var match = /^desc_attivita$/;

                if (e.which == 13 & !match.test($("*:focus").attr('name'))) {
                    $.ajax({
                        url: "../it.openia.crm.InsertEvent",
                        data: {
                            from: $('input[name="start_attivita"]').val(),
                            info: $('input[name="info_attivita"]').val(),
                            descr: $('textarea[name="desc_attivita"]').val(),
                            enddate: $('input[name="end_attivita"]').val(),
                            id: $('#activitydialog').attr('evt_id'),
                            idtype: $('select[name="tipo_attivita"]').attr('value'),
                            plannext: 'N',
                            statName: $('select[name="lead_status"]').attr('value'),
                            actStatus: getActualActivityStatus($('select[name="tipo_attivita"]').attr('value')),
                            leadId: $('input[name="leads_select"]').attr('idlead')
                        }
                    }).done(function (data) {
                        if (data.match(/OK/)) {
                            $('#activitydialog').dialog('close');
                            refreshCalendar();
                        }
                        else
                            alert(data);
                    });
                }
            });

            $('#activitydialog').on("dialogclose", function (event, ui) {

                $('#activitydialog').attr('evt_id', '');
                $('input[name="info_attivita"]').val('');
                $('textarea[name="desc_attivita"]').val('');
                $('input[name="start_attivita"]').val('');
                $('input[name="end_attivita"]').val('');
                $('select[name="tipo_attivita"]').val('');
                $('select[name="lead_status"]').val('');
                $('select[name="activity_status"]').val('');
                $('select[name="partner_list"]').val('');
                $('select[name="assignedto_list"]').val('');
                $('#actOBLink').attr('onclick', '');

                $('input[name="leads_select"]').val('');
                $('input[name="leads_select"]').attr('idlead', '');
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
            });

            // Funcion filtrar actividades
            $('#evtFilterBtn').live('click', function () {
                view = $('#calendar').fullCalendar('getView');
                
                partner_id = $( "#partner_list option:selected" ).text() == '' ? '' : $('select[name="partner_list"]').attr('value');
                assigned_to = $( "#assignedto_list option:selected" ).text()== '' ? '' : $('select[name="assignedto_list"]').attr('value');
                activity_types=$('select[name="tipo_attivita"]').attr('value');
                lead_status=$('select[name="lead_status"]').attr('value');
                activity_status=getActualActivityStatus($('select[name="tipo_attivita"]').attr('value'));
                
                $.ajax({
                        url: "../it.openia.crm.GetEvents",
                        data: {
                            from: view.start,
                            to: view.end,
                            all: "N",
                            isFilter: "Y",
                            idtype:activity_types ,
                            leadId: lead_status,
                            activityStatus: activity_status,
                            partnerId:partner_id, 
                            assignedtoId: assigned_to
                        }
                    }).done(function (data) {
                        $('#activitydialog').dialog('close');
                        if (Array.isArray(data.events) && data.events.length) {
                            eventsFromActivities = data.events;
                        $('#calendar').fullCalendar('removeEvents');
                        $('#calendar').fullCalendar('addEventSource', eventsFromActivities);
                        $(".fc-button-refresh img").attr("src", '../web/it.openia.crm/images/arrow_rotate_anticlockwise.png');
                        } else {
                            alert('No se encotraron actividades');
                            refreshCalendar();                     
                        }

                    });
            });

            $('#evtSaveBtn').live('click', function () {
                $.ajax({
                    url: "../it.openia.crm.InsertEvent",
                    data: {
                        from: $('input[name="start_attivita"]').val(),
                        info: $('input[name="info_attivita"]').val(),
                        descr: $('textarea[name="desc_attivita"]').val(),
                        enddate: $('input[name="end_attivita"]').val(),
                        id: $('#activitydialog').attr('evt_id'),
                        idtype: $('select[name="tipo_attivita"]').attr('value'),
                        plannext: 'N',
                        statName: $('select[name="lead_status"]').attr('value'),
                        actStatus: getActualActivityStatus($('select[name="tipo_attivita"]').attr('value')),
                        leadId: $('input[name="leads_select"]').attr('idlead')
                    }
                }).done(function (data) {
                    if (data.match(/OK/)) {
                        $('#activitydialog').dialog('close');
                        refreshCalendar();
                    }
                    else
                        alert(data);
                });
            });


            $('#evtDeleteBtn').live('click', function () {
                $('#eventDiv').hide();
                $('#ConfirmationDiv').show();
            });

            $('#evtRescheduleBtn').live('click', function () {
                $('#eventDiv').hide();
                $('#actOBLink').hide();
                $('#RescheduleDiv').show();
            });

            $('#evtDoneAndNextBtn').live('click', function () {

                $.ajax({
                    url: "../it.openia.crm.InsertEvent",
                    data: {
                        from: $('input[name="start_attivita"]').val(),
                        info: $('input[name="info_attivita"]').val(),
                        descr: $('textarea[name="desc_attivita"]').val(),
                        enddate: $('input[name="end_attivita"]').val(),
                        id: $('#activitydialog').attr('evt_id'),
                        idtype: $('select[name="tipo_attivita"]').attr('value'),
                        plannext: 'N',
                        statName: $('select[name="lead_status"]').attr('value'),
                        actStatus: getActualActivityStatus($('select[name="tipo_attivita"]').attr('value')),
                        eventDone: 'Y'
                    }
                }).done(function (data) {

                    if (data.match(/OK/)) {
                        refreshCalendar();
                        $('#eventDiv').hide();
                        $('#actOBLink').hide();
                        $('#RescheduleDiv').show();
                    }
                    else
                        alert(data);
                });

            });


            $('#backToEvt').live('click', function () {
                $('#eventDiv').show();
                $('#ConfirmationDiv').hide();
            });

            $('#backToEvtFromAct').live('click', function () {
                $('#eventDiv').show();
                $('#actOBLink').show();
                $('#RescheduleDiv').hide();
            });

            $('#confirmDeleteEvt').live('click', function () {

                $.ajax({
                    url: "../it.openia.crm.DeleteEvent",
                    data: {
                        id: $('#activitydialog').attr('evt_id')
                    }
                }).done(function (data) {
                    $('#activitydialog').dialog('close');
                    refreshCalendar();
                });

            });

            $('#confirmNextActEvt').live('click', function () {

                $.ajax({
                    url: "../it.openia.crm.InsertEvent",
                    data: {
                        from: $('input[name="next_activity"]').val(),
                        info: $('input[name="info_next_attivita"]').val(),
                        descr: $('textarea[name="desc_next_attivita"]').val(),
                        enddate: $('input[name="next_activity_end"]').val(),
                        id: $('#activitydialog').attr('evt_id'),
                        idtype: $('select[name="tipo_attivita_next"]').attr('value'),
                        plannext: 'Y',
                        statName: $('select[name="lead_status"]').attr('value'),
                        actStatus: getActualActivityStatusNext($('select[name="tipo_attivita_next"]').attr('value'))
                    }
                }).done(function (data) {
                    $('#activitydialog').dialog('close');
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
                <div id="partnerDiv">
                </div>
                <table>
                    <tr id="subjectActivity">

                        <td><span><%=GetEvents.GetField("C0983A77EEF145FD98D032500C43C6A8")%>*:</span></td>
                        <td><span><input type="text" name="info_attivita" placeholder="nome" autofocus></span>
                            <a style="padding-bottom: 5px;padding-left: 5px;" href="#" onclick="" id="actOBLink"
                                target="_blank">
                                <img src="../web/it.openia.crm/images/link_go.png"
                                    alt="open activity window in Openbravo"
                                    title="open activity window in Openbravo" /></a>
                        </td>

                    </tr>
                    <tr id="startDateActivity">
                        <td><span><%=GetEvents.GetField("B7086C8DB5044690B058D61BFEF62E5A")%>*:</span></td>
                        <td><span>
                                <div class="well">
                                    <div id="datetimepicker1" class="input-append date">
                                        <input name="start_attivita" placeholder="dd/mm/yyyy HH:mm"
                                            data-format="dd/MM/yyyy hh:mm" type="text"></input>
                                        <span class="add-on">
                                            <i data-time-icon="icon-time" data-date-icon="icon-calendar">
                                            </i></span>
                                    </div>
                                </div>
                                <script type="text/javascript">
                                    $(function () {
                                        $('#datetimepicker1').datetimepicker({
                                            language: '<%=GetEvents.GetLocalization()%>'
                                        });
                                    });
                                </script>
                            </span>
                        </td>
                    </tr>
                    <tr id="endDateActivity">
                        <td><span><%=GetEvents.GetField("0498AD39A62342518CAA1A35FF659322")%>:</span></td>
                        <td><span>
                                <div class="well">
                                    <div id="datetimepicker2" class="input-append date">
                                        <input name="end_attivita" placeholder="dd/mm/yyyy HH:mm"
                                            data-format="dd/MM/yyyy hh:mm" type="text"></input>
                                        <span class="add-on">
                                            <i data-time-icon="icon-time" data-date-icon="icon-calendar">
                                            </i></span>
                                    </div>
                                </div>
                                <script type="text/javascript">
                                    $(function () {
                                        $('#datetimepicker2').datetimepicker({
                                            language: '<%=GetEvents.GetLocalization()%>'
                                        });
                                    });
                                </script>
                            </span></td>
                    </tr>
                    <tr id="TextAreaCalendar">
                        <td><span><%=GetEvents.GetField("E57398E301A44BB3B7CC5457C37F6145")%>:</span></td>
                        <!-- <td><span><input type="text" name="desc_attivita"></span></td> -->
                        <td><span>
                                <center><textarea style="height:40px;width:233px;resize: none;overflow-y:auto;"
                                        name="desc_attivita"></textarea></center>
                            </span></td>

                    </tr>
                    <tr id="LeadSelect">
                        <td style="width:253px;height:37px;"><span>Lead:</span></td>
                        <td><span>
                                <!-- <select name="lead_dropdownlist" style="width: 247px;">
                    				     </select>  -->
                                <input name="leads_select" idlead=""
                                    style="width: 247px;border-radius: 5px;border-left-width: 1px;width: 247px;height: 25px;border-top-width: 1px;border: 1px solid #ccc;">
                            </span></td>
                    </tr>
                    <tr>
                        <td><span><%=GetEvents.GetField("EE834F13B27F4B0D8262C3DBAEDCD9A8")%>:</span></td>
                        <td><span>
                                <select id="tipo_attivita" name="tipo_attivita" style="width: 163px;">
                                </select>
                            </span></td>
                    </tr>
                    <tr id="ActivityStatus">
                        <td><span><%=GetEvents.GetField("97833219C3884345A87819FE15C5AAEE")%>:</span></td>
                        <td><span>
                                <select name="activity_status" style="width: 163px;">
                                </select>
                            </span></td>
                    </tr>
                    <tr id="leadStatusRow">
                        <td><span><%=GetEvents.GetField("C1C8461D35D141DD8D45F8FF1A606840")%>:</span></td>
                        <td><span>
                                <select id="lead_status" name="lead_status" style="width: 163px;">
                                </select>
                            </span></td>
                    </tr>
                    <tr id="partner">
                        <td><span><%=GetEvents.GetField("C4F2848101D147968B15A83200468F19")%>:</span></td>
                        <td><span>
                                <select id="partner_list" name="partner_list" style="width: 163px;">
                                </select>
                            </span></td>
                    </tr>
                    <tr id="assignedto">
                        <td><span><%=GetEvents.GetField("867A0409276940C697C8D08E904D1794")%>:</span></td>
                        <td><span>
                                <select id="assignedto_list" name="assignedto_list" style="width: 163px;">
                                </select>
                            </span></td>
                    </tr>

                </table>

                <br />
                <a class="userPopUpInfo" href="#" title="<%=GetEvents.GetMessage("OPCRM_FILTER")%>">
                    <button id="evtFilterBtn" title="<%=GetEvents.GetMessage("OPCRM_FILTER")%>" type="button"
                        style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img
                            src="../web/it.openia.crm/images/funnel-icon.png" />
                    </button>
                </a>
                &nbsp;&nbsp;
                <a class="userPopUpInfo" href="#" title="<%=GetEvents.GetMessage("OPCRM_SAVEACT")%>">
                    <button id="evtSaveBtn" title="<%=GetEvents.GetMessage("OPCRM_SAVEACT")%>" type="button"
                        style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img
                            src="../web/it.openia.crm/images/disk.png" />
                    </button>
                </a>
                &nbsp;&nbsp;
                <a class="userPopUpInfo" href="#" title="<%=GetEvents.GetMessage("OPCRM_DELETEACT")%>">
                    <button id="evtDeleteBtn" title="<%=GetEvents.GetMessage("OPCRM_DELETEACT")%>" type="button"
                        style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img
                            src="../web/it.openia.crm/images/delete.png" />
                    </button>
                </a>
                &nbsp;&nbsp;
                <a class="userPopUpInfo" href="#" title="<%=GetEvents.GetMessage("OPCRM_NEXTACT")%>">
                    <button id="evtRescheduleBtn" title="<%=GetEvents.GetMessage("OPCRM_NEXTACT")%>" type="button"
                        style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img
                            src="../web/it.openia.crm/images/calendar_edit.png" />
                    </button>
                </a>
                &nbsp;&nbsp;
                <a class="userPopUpInfo" href="#" title="<%=GetEvents.GetMessage("OPCRM_SETASDONE")%>">
                    <button id="evtDoneAndNextBtn" title="<%=GetEvents.GetMessage("OPCRM_SETASDONE")%>" type="button"
                        style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img
                            src="../web/it.openia.crm/images/date_go.png" />
                    </button>
                </a>
            </center>
        </div>

        <div id="ConfirmationDiv">
            <center>
                Delete Activity?
                &nbsp;<button id="confirmDeleteEvt" type="button"
                    style="cursor: hand; cursor: pointer;heigth:33px;width:46px;">Yes</button>
                &nbsp;&nbsp;&nbsp;<button id="backToEvt" type="button"
                    style="cursor: hand; cursor: pointer;heigth:33px;width:46px;">No</button>
            </center>
        </div>

        <div id="RescheduleDiv">
            <center>
                <div id="partnerDivNextEvt"></div>
                <table>

                    <tr>

                        <td><span><%=GetEvents.GetField("C0983A77EEF145FD98D032500C43C6A8")%>*:</span></td>
                        <td><span><input type="text" name="info_next_attivita" placeholder="nome"></span></td>

                    </tr>
                    <tr>

                        <td><span><%=GetEvents.GetField("B7086C8DB5044690B058D61BFEF62E5A")%>*:</span></td>
                        <td><span>
                                <div class="well">
                                    <div id="datetimepicker3" class="input-append date">
                                        <input name="next_activity" placeholder="dd/mm/yyyy HH:mm"
                                            data-format="dd/MM/yyyy hh:mm" type="text"></input>
                                        <span class="add-on">
                                            <i data-time-icon="icon-time" data-date-icon="icon-calendar">
                                            </i></span>
                                    </div>
                                </div>
                                <script type="text/javascript">
                                    $(function () {
                                        $('#datetimepicker3').datetimepicker({
                                            language: '<%=GetEvents.GetLocalization()%>'
                                        });
                                    });
                                </script>
                            </span></td>

                    </tr>

                    <tr>

                        <td><span><%=GetEvents.GetField("0498AD39A62342518CAA1A35FF659322")%>:</span></td>
                        <td><span>
                                <div id="datetimepicker4" class="input-append date">
                                    <input name="next_activity_end" placeholder="dd/mm/yyyy HH:mm"
                                        data-format="dd/MM/yyyy hh:mm" type="text"></input>
                                    <span class="add-on">
                                        <i data-time-icon="icon-time" data-date-icon="icon-calendar">
                                        </i></span>
                                </div>
        </div>
        <script type="text/javascript">
            $(function () {
                $('#datetimepicker4').datetimepicker({
                    language: '<%=GetEvents.GetLocalization()%>'
                });
            });
        </script>
        </span></td>

        </tr>
        <tr>

            <td><span><%=GetEvents.GetField("E57398E301A44BB3B7CC5457C37F6145")%>:</span></td>
            <td><span>
                    <center><textarea style="height:40px;width:233px;resize: none;overflow-y:auto;"
                            name="desc_next_attivita"></textarea></center>
                </span></td>

        </tr>

        <tr>
            <td><span><%=GetEvents.GetField("EE834F13B27F4B0D8262C3DBAEDCD9A8")%>:</span></td>
            <td><span>
                    <select name="tipo_attivita_next" style="width: 163px;">

                    </select>
                </span></td>
        </tr>

        <tr id="ActivityStatusNext">
            <td><span><%=GetEvents.GetField("97833219C3884345A87819FE15C5AAEE")%>:</span></td>
            <td><span>
                    <select name="activity_status_next" style="width: 163px;">
                    </select>
                </span></td>
        </tr>

        </table>

        </br>
        &nbsp;<button id="confirmNextActEvt" type="button"
            style="cursor: hand; cursor: pointer;heigth:33px;width:46px;">Ok</button>
        &nbsp;&nbsp;&nbsp;<button id="backToEvtFromAct" type="button"
            style="cursor: hand; cursor: pointer;heigth:33px;width:60px;">Cancel</button>
        </center>
    </div>

    </div>

    <div class="header">
        <h2 id="title"></h2>
    </div>
    <div id='calendar' style='font-size: 13px; overflow-y: auto'></div>
</body>

</html>