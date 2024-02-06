<%-- 
    Document   : purchase_and_sold
    Created on : 12-lug-2013, 15.33.41
    Author     : diurno
--%>

<%@page contentType="text/html" pageEncoding="UTF-8" import="it.openia.crm.charts.ChartPurchasedAndSoldData"%>
<!DOCTYPE html>
<html>
  <head>
    <script type="text/javascript" src="https://www.google.com/jsapi"></script>
    <script type="text/javascript">
      
      google.load("visualization", "1", {packages:["corechart"]});
      google.setOnLoadCallback(drawChart);
      
      function drawChart() {
        var data = google.visualization.arrayToDataTable([
            // GetField(YEAR_TEXT_ID) , GetField(SALES_TEXT_ID), GetField(PURCHASES_TEXT_ID)
          ['<%=ChartPurchasedAndSoldData.GetField("OPCRM_YEAR")%>', '<%=ChartPurchasedAndSoldData.GetField("OPCRM_SALES")%>', '<%=ChartPurchasedAndSoldData.GetField("OPCRM_PURCHASES")%>'],
          <%
          out.println(ChartPurchasedAndSoldData.getData(4));          
          %>
          ]);

        var options = {
            // GetField(BUSINESS_PERFORMANCE_TEXT_ID)
          title: '<%=ChartPurchasedAndSoldData.GetField("OPCRM_BPERFORMANCE")%>',
            // GetField(YEAR_ID)
          hAxis: {title: '<%=ChartPurchasedAndSoldData.GetField("OPCRM_YEAR")%>',  titleTextStyle: {color: 'green'}}
        };

        var chart = new google.visualization.AreaChart(document.getElementById('chart_div'));
        chart.draw(data, options);
      }
      
    </script>
  </head>
  <body>
    <div id="chart_div" style="width: 475px; height: 260px;"></div>
  </body>

</html>
