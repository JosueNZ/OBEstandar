<%@ page import="it.openia.crm.*" %>
<%@ page import="it.openia.crm.ad_forms.CalendarIcsDownload" %>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<!-- Copyright (C) 2008-2013 Openia S.r.l.
   - This Source Code Form is subject to the terms of the Mozilla Public
   - License, v. 2.0. If a copy of the MPL was not distributed with this
   - file, You can obtain one at http://mozilla.org/MPL/2.0/. -->

<html>
    <head> 
        <title>Openia CRM - Openbravo ICS Download</title>
        <script type='text/javascript' src="../web/it.openia.crm/js/jquery-1.8.3.js"></script>
    </head>

    <body bgcolor="#E6E6FA">
        <div id="downloadMessageDiv">
            
            <%=CalendarIcsDownload.GetMessage("OPCRM_DOWNLOADING")%>
            
            <!-- <img src="../web/it.openia.crm/images/OpeniaCRM.png"> -->
            
            <script type='text/javascript'>
                window.location="../it.openia.crm.CalendarIcsFileGenerator?activityId="+"<%=CalendarIcsDownload.GetActId()%>";
            </script>
        </div>
        
    </body>

</html>