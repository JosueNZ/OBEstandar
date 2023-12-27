<%@ page import="it.openia.crm.*" %>
<%@ page import="it.openia.crm.ad_forms.ActivityFromLead" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<!-- Copyright (C) 2008-2013 Openia S.r.l.
   - This Source Code Form is subject to the terms of the Mozilla Public
   - License, v. 2.0. If a copy of the MPL was not distributed with this
   - file, You can obtain one at http://mozilla.org/MPL/2.0/. -->

<html>
    <!-- jsp is frozen and still under development (along with ActivityFromLead.java and CreateActivityGetData.java) -->
    
    <head> 
        <title>Openia CRM - Activity From Lead</title>
        <script type='text/javascript' src="../web/it.openia.crm/js/jquery-1.8.3.js"></script>
        <link rel="stylesheet" type='text/css' href='http://code.jquery.com/ui/1.10.0/themes/base/jquery-ui.css' />
        <link rel="stylesheet" type='text/css' href='../web/it.openia.crm/css/bootstrap-datetimepicker.min.css' /> 
		<link rel="stylesheet" type='text/css' href="../web/it.openia.crm/css/bootstrap-combined.min.css">
	
		<script type='text/javascript' src='../web/it.openia.crm/js/jquery-1.8.1.min.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/js/jquery-ui-1.8.23.custom.min.js'></script>
        <script type='text/javascript' src="../web/it.openia.crm/js/jquery-1.8.3.js"></script>
        
        <script type="text/javascript"
     			src="http://netdna.bootstrapcdn.com/twitter-bootstrap/2.2.2/js/bootstrap.min.js">
    	</script>
        
        <script type='text/javascript' src="../web/it.openia.crm/js/jquery-ui.js"></script>
        
        <script type='text/javascript' src='../web/it.openia.crm/js/bootstrap-datetimepicker.min.js'></script>
		
        <script type="text/javascript" src="../web/it.openia.crm/js/bootstrap-datetimepicker.en-US.js"></script>
	<script type="text/javascript" src="../web/it.openia.crm/js/bootstrap-datetimepicker.it-IT.js"></script>	
	<script type="text/javascript" src="../web/it.openia.crm/js/bootstrap-datetimepicker.es-ES.js"></script>
	
        <script type='text/javascript'>
        $(document).ready(function() {
            var activityTypes;
            
            $.ajax({
                url: "../it.openia.crm.CreateActivityGetData",
                data: {
                    leadId:'<%=ActivityFromLead.GetLeadId()%>'
                    }
                }).done(function(data) {
                    
                    activityTypes = data.actTypes;
                    
                    
                    //initialize selectable activiy types
                    $('select[name="tipo_attivita"]').append($('<option/>').attr("value", 0).text(''));
                    $.each(activityTypes, function(i, option) {
                        $('select[name="tipo_attivita"]').append($('<option/>').attr("value", option.id+1).text(option.name));
                    });
                    
                    $('#ActivityStatus').hide();
                    
                    var date = new Date();
                    
                    //default time pickers initialization
                    var pickerStart = $('#datetimepicker1').data('datetimepicker');
                    pickerStart.setLocalDate(new Date(date.getFullYear(), date.getMonth(), date.getDate(), 9, 0, 0, 0));
                    var pickerEnd = $('#datetimepicker2').data('datetimepicker');
                    pickerEnd.setLocalDate(new Date(date.getFullYear(), date.getMonth(), date.getDate(), 10, 0, 0, 0));
            });
        
            //on each activity type dropdownlist selection, this method sets the related activity status dropdownlist,
            //hides the status dropdownlist if the selected activity type is null
            $('select[name="tipo_attivita"]').change(function() {
                var selection = $('select[name="tipo_attivita"]').attr('value');
        		
                if ( selection == 0){
                    $('#ActivityStatus').hide();
                    $('select[name="activity_status"]').empty();
        	}
        	else
                    setActivityStatuses(selection);
        		
             });
        
             //shows the activity status dropdownlist when an activity type is chosen on the 'activitydialog' popup
                var setActivityStatuses = function(strActId){
                    
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
    
        });
        
        </script>
                
        </head>

    <body bgcolor="#E6E6FA">
        <div id="eventDiv">
                <center>
                    
                    <div id="partnerDiv"></div>
                    <table>
                        <tr>

                            <td><span><%=GetEvents.GetField("C0983A77EEF145FD98D032500C43C6A8")%>*:</span></td>
                            <td><span><input type="text" name="info_attivita" value="<%=ActivityFromLead.GetName()%>" placeholder="nome" autofocus></span></td>

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
                         
                    </table>

                    <br/>
                    <button id="evtSaveBtn" title="save activity" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img src="../web/it.openia.crm/images/disk.png"/>
                    </button>
                    &nbsp;&nbsp;
                    <button id="evtDeleteBtn" title="delete activity" type="button" style="cursor: hand; cursor: pointer;heigth:33px;width:46px;"><img src="../web/it.openia.crm/images/delete.png"/>
                    </button>
                    
                </center>    
            </div>
        
    </body>

</html>