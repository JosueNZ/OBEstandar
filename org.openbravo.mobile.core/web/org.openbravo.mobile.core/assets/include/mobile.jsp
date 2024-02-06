<%-- 
 ************************************************************************************
 * Copyright (C) 2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 
Mobile Web Application Attributes

--%>

<%-- Viewport attributes --%>
<meta name="viewport" content="width=device-width, user-scalable=no, initial-scale=1.0, maximum-scale=1.0">

<%
  boolean isIOS = false;
  String userAgent = request.getHeader("user-agent");
  if (userAgent.matches(".*Macintosh.*") || userAgent.matches(".*iPhone.*") || userAgent.matches(".*iPad.*")) {
    isIOS = true;
  }
%>

<%--  Safari application capable attributes --%>
<%-- https://developer.apple.com/library/safari/documentation/AppleApplications/Reference/SafariHTMLRef/Articles/MetaTags.html --%>
<meta name="apple-mobile-web-app-capable" content="yes" />
<meta name="apple-mobile-web-app-status-bar-style" content="black" />
<meta name="apple-touch-fullscreen" content="yes" />
<link rel="apple-touch-startup-image" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webappstartup.png"/>
<link rel="apple-touch-icon" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp60x60.png"/>
<link rel="apple-touch-icon" sizes="76x76" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp76x76.png"/>
<link rel="apple-touch-icon" sizes="120x120" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp120x120.png"/>
<link rel="apple-touch-icon" sizes="128x128" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp128x128.png"/>
<link rel="apple-touch-icon" sizes="152x152" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp152x152.png"/>
<link rel="apple-touch-icon" sizes="196x196" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp196x196.png"/>

<%-- Chrome application capable attributes --%>
<%-- https://developer.chrome.com/multidevice/android/installtohomescreen --%>
<meta name="mobile-web-app-capable" content="yes">
<link rel="icon" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp60x60.png"/>
<link rel="icon" sizes="76x76" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp76x76.png"/>
<link rel="icon" sizes="120x120" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp120x120.png"/>
<link rel="icon" sizes="128x128" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp128x128.png"/>
<link rel="icon" sizes="152x152" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp152x152.png"/>
<link rel="icon" sizes="196x196" href="../org.openbravo.mobile.core/assets/img/<%=(isIOS ? "ios" : "mobile")%>webapp196x196.png"/>

