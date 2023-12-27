<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<!-- Copyright (C) 2008-2013 Openia S.r.l.
   - This Source Code Form is subject to the terms of the Mozilla Public
   - License, v. 2.0. If a copy of the MPL was not distributed with this
   - file, You can obtain one at http://mozilla.org/MPL/2.0/. -->

<html>
    <head> 
        <title>Openia CRM - Openbravo ERP module</title>
        <meta http-equiv="Content-Type" content="text/html; charset=utf8"/>
        <meta name="apple-mobile-web-app-capable" content="yes"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
        <link rel="shortcut icon" href="assets/favicon.ico"/>
        <!--
        <link rel="stylesheet" type='text/css' href='http://code.jquery.com/ui/1.10.0/themes/base/jquery-ui.css' />
        <link rel="stylesheet" type='text/css' href='http://code.jquery.com/mobile/1.3.2/jquery.mobile.structure-1.3.2.css' />
        <link rel="stylesheet" href="http://code.jquery.com/mobile/1.3.2/jquery.mobile-1.3.2.min.css" />
        -->
        
        <link rel="stylesheet" type='text/css' href='http://code.jquery.com/ui/1.10.0/themes/base/jquery-ui.css' />
        
        <script type='text/javascript' src='../web/it.openia.crm/js/jquery-1.8.1.min.js'></script>
        
        <script type='text/javascript' src="../web/it.openia.crm/js/jquery-ui.js"></script>
        <script type='text/javascript' src='../web/it.openia.crm/js/jquery-ui-1.8.23.custom.min.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/js/jquery.mobile-1.3.2.js'></script>
        
		<script src="../web/it.openia.crm/enyo/enyo.js" type="text/javascript"></script>
        
        <script src="../web/it.openia.crm/source/list2package.js" type="text/javascript"></script>
    	
    	<script type="text/javascript">
    		$(document).on("pageshow", "[data-role='page']", function () {
 				$('div.ui-loader').remove();
			});
    	</script>
    	
    </head>

    <div class="enyo-unselectable" data-role="page">
    	<script type="text/javascript">
            new App({fit:true}).renderInto(document.body);
		</script>
    </div>

</html>