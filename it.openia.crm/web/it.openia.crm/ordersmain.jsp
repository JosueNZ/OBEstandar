<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<!-- Copyright (C) 2008-2013 Openia S.r.l.
   - This Source Code Form is subject to the terms of the Mozilla Public
   - License, v. 2.0. If a copy of the MPL was not distributed with this
   - file, You can obtain one at http://mozilla.org/MPL/2.0/. -->

<html manifest="../web/it.openia.crm/test.appcache">
    <head> 
        <title>Openia CRM - Openbravo Orders Test</title>
        <script type='text/javascript' src='../web/it.openia.crm/js/jquery-1.8.1.min.js'></script>
        
        <meta name="apple-mobile-web-app-capable" content="yes"/>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no"/>
        
        <!--
        <link rel="shortcut icon" href="assets/favicon.ico"/>
        -->
        
        <script src="../web/it.openia.crm/enyo/enyo.js" type="text/javascript"></script>
        <script src="../web/it.openia.crm/source/list2package.js" type="text/javascript"></script>
        
        
        <script type='text/javascript' src='../web/it.openia.crm/BusinessPartner.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/OrderHeaderField.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/OrderLineField.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/BPartnerAddress.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/TransactionDocument.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/PaymentTerm.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/InvoiceTerm.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/Warehouse.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/Product.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/PriceList.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/ProductPList.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/SalesOrder.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/SalesOrderLine.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/ProductCategory.js'></script>
        <script type='text/javascript' src='../web/it.openia.crm/PaymentMethod.js'></script>
        
        <script type='text/javascript' src='../it.openia.crm/masterdata.js'></script>

        <script type='text/javascript'>
        	
               
            // Check if a new cache is available on page load.
            window.addEventListener('load', function(e) {

                window.applicationCache.addEventListener('updateready', function(e) {
                    if (window.applicationCache.status == window.applicationCache.UPDATEREADY) {
                        // Browser downloaded a new app cache.
                        // Swap it in and reload the page to get the new hotness.
                        window.applicationCache.swapCache();
                            window.location.reload();
                        
                    } 
                    else{
                         // Manifest didn't change. Nothing new to server.
                    }
                }, false);
            
            }, false); 
            
                /*
                            var appCache = window.applicationCache;
                            appCache.update(); 
                            window.applicationCache.swapCache();
                            window.location.reload();
                */            
        </script>
    </head>

    <body>
        <div id="testDiv">
            
         <div class="enyo-unselectable">
            <script type="text/javascript">
                new OrdersApp({fit:true}).renderInto(document.body);
            </script>
        </div>
        
    </body>

</html>