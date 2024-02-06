/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2008-2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package com.sidesoft.localization.ecuador.refunds.ad_forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.businessUtility.WindowTabsData;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

public class Refund extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strPoReference = vars.getRequestGlobalVariable("inpPoReference", "Refund|PoReference");
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "Refund|DateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "Refund|DateTo", "");
      String strRequesterId = vars.getGlobalVariable("inpRequesterId", "Refund|Requester_ID", "");
      String strCustomerId = vars.getGlobalVariable("inpcBpartnerId", "Refund|C_BPartner_ID", "");
      String strIncludeCustomer = vars.getGlobalVariable("inpShowNullCustomer",
          "Refund|ShowNullCustomer", "Y");
      String strOrgId = vars.getGlobalVariable("inpadOrgId", "Refund|AD_Org_ID", vars.getOrg());
      vars.setSessionValue("Refund|isSOTrx", "N");
      printPageDataSheet(response, vars, strPoReference, strDateFrom, strDateTo, strRequesterId,
          strCustomerId, strIncludeCustomer, strOrgId);
    } else if (vars.commandIn("FIND")) {
      String strPoReference = vars.getRequestGlobalVariable("inpPoReference", "Refund|PoReference");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "Refund|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "Refund|DateTo");
      String strRequesterId = vars
          .getRequestGlobalVariable("inpRequesterId", "Refund|Requester_ID");
      String strCustomerId = vars
          .getRequestGlobalVariable("inpcBpartnerId", "Refund|C_BPartner_ID");
      String strIncludeCustomer = vars.getRequestGlobalVariable("inpShowNullCustomer",
          "Refund|ShowNullCustomer");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "Refund|AD_Org_ID");
      printPageDataSheet(response, vars, strPoReference, strDateFrom, strDateTo, strRequesterId,
          strCustomerId, strIncludeCustomer, strOrgId);
    } else if (vars.commandIn("ADD")) {
      String strPoReference = vars.getRequestGlobalVariable("inpPoReference", "Refund|PoReference");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "Refund|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "Refund|DateTo");
      String strRequesterId = vars
          .getRequestGlobalVariable("inpRequesterId", "Refund|Requester_ID");
      String strCustomerId = vars
          .getRequestGlobalVariable("inpcBpartnerId", "Refund|C_BPartner_ID");
      String strIncludeCustomer = vars.getRequestGlobalVariable("inpShowNullCustomer",
          "Refund|ShowNullCustomer");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "Refund|AD_Org_ID");
      String strRefundLines = vars.getRequiredInStringParameter("inpInvoice", IsIDFilter.instance);
      lockRefundLines(vars, strRefundLines);
      printPageDataSheet(response, vars, strPoReference, strDateFrom, strDateTo, strRequesterId,
          strCustomerId, strIncludeCustomer, strOrgId);
    }

    else if (vars.commandIn("REMOVE")) {
      String strPoReference = vars.getRequestGlobalVariable("inpPoReference", "Refund|PoReference");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "Refund|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "Refund|DateTo");
      String strRequesterId = vars
          .getRequestGlobalVariable("inpRequesterId", "Refund|Requester_ID");
      String strCustomerId = vars
          .getRequestGlobalVariable("inpcBpartnerId", "Refund|C_BPartner_ID");
      String strIncludeCustomer = vars.getRequestGlobalVariable("inpShowNullCustomer",
          "Refund|ShowNullCustomer");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "REFUND|AD_Org_ID");
      String strSelectedLines = vars.getRequiredInStringParameter("inpSelectedReq",
          IsIDFilter.instance);
      unlockRefundLines(vars, strSelectedLines);
      printPageDataSheet(response, vars, strPoReference, strDateFrom, strDateTo, strRequesterId,
          strCustomerId, strIncludeCustomer, strOrgId);
    } else if (vars.commandIn("OPEN_CREATE")) {
      String strSelectedLines = vars.getRequiredInStringParameter("inpSelectedReq",
          IsIDFilter.instance);
      checkSelectedRequisitionLines(response, vars, strSelectedLines);
    } else if (vars.commandIn("GENERATE")) {
      String strSelectedLines = vars.getRequiredGlobalVariable("inpSelected",
          "RefundCreate|SelectedLines");
      String strDocType = vars.getRequiredGlobalVariable("inpDocTypeId", "RefundCreate|DocType");
      String strInvoiceDate = vars.getRequiredGlobalVariable("inpInvoiceDate",
          "RefundCreate|InvoiceDate");
      String strCustomer = vars.getRequiredGlobalVariable("inpInvoiceCustomerId",
          "RefundCreate|InvoiceCustomer");
      String strPriceListId = vars.getRequiredGlobalVariable("inpPriceListId",
          "RefundCreate|PriceListId");
      String strOrg = vars.getRequiredGlobalVariable("inpInvoiceOrg", "RefundCreate|Org");
      OBError myMessage = processSalesInvoice(vars, strSelectedLines, strDocType, strInvoiceDate,
          strCustomer, strPriceListId, strOrg);
      vars.setMessage("RefundCreate", myMessage);
      printPageCreate(response, vars, "", "", "", "", "", "");
    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strPoReference, String strDateFrom, String strDateTo, String strRequesterId,
      String strCustomerId, String strIncludeCustomer, String strOrgId) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;

    String strTreeOrg = RefundData.treeOrg(this, vars.getClient());
    RefundData[] datalines = RefundData.selectLines(this, vars.getLanguage(), Utility.getContext(
        this, vars, "#User_Client", "Refund"), Tree.getMembers(this, strTreeOrg, strOrgId),
        strDateFrom, DateTimeData.nDaysAfter(this, strDateTo, "1"), strRequesterId,
        (strIncludeCustomer.equals("Y") ? strCustomerId : null),
        (strIncludeCustomer.equals("Y") ? null : strCustomerId), (strPoReference.equals("") ? null
            : strPoReference));

    RefundData[] dataselected = RefundData.selectSelected(this, vars.getLanguage(), vars.getUser(),
        Utility.getContext(this, vars, "#User_Client", "Refund"),
        Tree.getMembers(this, strTreeOrg, strOrgId));

    String discard[] = { "" };
    if (dataselected == null || dataselected.length == 0) {
      dataselected = RefundData.set();
      discard[0] = "funcSelectedEvenOddRow";
    }
    xmlDocument = xmlEngine.readXmlTemplate(
        "com/sidesoft/localization/ecuador/refunds/ad_forms/Refund", discard).createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "Refund", false, "", "", "", false,
        "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "com.sidesoft.localization.ecuador.refunds.ad_forms.Refund");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "Refund.html", classInfo.id,
          classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "Refund.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("Refund");
      vars.removeMessage("Refund");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("poReference", strPoReference);
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("paramRequester", strRequesterId);
    xmlDocument.setParameter("paramBPartnerId", strCustomerId);
    xmlDocument.setParameter(
        "paramBPartnerDescription",
        strCustomerId.equals("") ? "" : RefundData.bPartnerDescription(this, strCustomerId,
            vars.getLanguage()));
    xmlDocument.setParameter("paramShowNullCustomer", strIncludeCustomer);
    xmlDocument.setParameter("paramAdOrgId", strOrgId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_User_ID", "",
          "AD_User - Internal", Utility.getContext(this, vars, "#AccessibleOrgTree", "Refund"),
          Utility.getContext(this, vars, "#User_Client", "Refund"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "Refund", strRequesterId);
      xmlDocument.setData("reportRequester_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org Security validation", Utility.getContext(this, vars, "#User_Org", "Refund"),
          Utility.getContext(this, vars, "#User_Client", "Refund"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "Refund", strOrgId);
      xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    // We must execute the selected query.

    xmlDocument.setData("structureSearch", datalines);
    xmlDocument.setData("structureSelected", dataselected);
    out.println(xmlDocument.print());
    out.close();
  }

  private void lockRefundLines(VariablesSecureApp vars, String strRefundLines) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Locking refund lines: " + strRefundLines);
    RefundData.lock(this, vars.getUser(), strRefundLines);
  }

  private void unlockRefundLines(VariablesSecureApp vars, String strRefundLines)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Unlocking refund lines: " + strRefundLines);
    RefundData.unlock(this, strRefundLines);
  }

  private void checkSelectedRequisitionLines(HttpServletResponse response, VariablesSecureApp vars,
      String strSelected) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Check selected refund lines");

    // Check unique partner
    String strCustomerId = "";
    String strInvoiceDate = DateTimeData.today(this);
    String strPriceListId = "";
    String strDocTypeId = "";
    String strOrgId = "";
    String strMessage = "";
    if (!strSelected.equals("")) {
      RefundData[] customer = RefundData.selectCustomer(this, strSelected);
      if (customer != null && customer.length == 1) {
        strCustomerId = customer[0].customerId;
        strMessage = Utility.messageBD(this, "SSRE_AllLinesSameCustomer", vars.getLanguage())
            + ": "
            + RefundData.bPartnerDescription(this, customer[0].customerId, vars.getLanguage());
      } else if (customer != null && customer.length > 1) {
        // Error, the selected lines are of different customers, it is
        // necessary to set one.
        strMessage = Utility.messageBD(this, "SSRE_MoreThanOneCustomer", vars.getLanguage());
      } else {
        // Error, it is necessary to select a customer.
        strMessage = Utility.messageBD(this, "SSRE_AllLinesNullCustomer", vars.getLanguage());
      }
      // Check unique pricelist
      RefundData[] pricelist = RefundData.selectPriceList(this, vars.getLanguage(), strSelected);
      if (pricelist != null && pricelist.length == 1) {
        strPriceListId = pricelist[0].mPricelistId;
        strMessage += "<br>" + Utility.messageBD(this, "AllLinesSamePricelist", vars.getLanguage())
            + ": " + pricelist[0].pricelistid;
      } else if (pricelist != null && pricelist.length > 1) {
        // Error, the selected lines are of different pricelists, it is
        // necessary to set one.
        strMessage += "<br>"
            + Utility.messageBD(this, "SSRE_MoreThanOnePricelist", vars.getLanguage());
      } else {
        // Error, it is necessary to select a pricelist.
        strMessage += "<br>"
            + Utility.messageBD(this, "SSRE_AllLinesNullCustomer", vars.getLanguage());
      }

      // Check unique org
      RefundData[] org = RefundData.selectOrg(this, vars.getLanguage(), strSelected);
      if (org != null && org.length == 1) {
        strOrgId = org[0].adOrgId;
        strMessage += "<br>" + Utility.messageBD(this, "AllLinesSameOrg", vars.getLanguage())
            + ": " + org[0].org;
      } else {
        // Error, the selected lines are of different orgs, it is
        // necessary to set one.
        strMessage += "<br>" + Utility.messageBD(this, "SSRE_MoreThanOneOrg", vars.getLanguage());
      }

      // Get Default Document Type
      strDocTypeId = RefundData.cDoctypeTarget(this, vars.getClient(), vars.getOrg());

      OBError myMessage = new OBError();
      myMessage.setTitle("");
      myMessage.setType("Info");
      myMessage.setMessage(strMessage);
      vars.setMessage("RefundCreate", myMessage);
    } else {
      OBError myMessage = new OBError();
      myMessage.setTitle("");
      myMessage.setType("Info");
      myMessage.setMessage(Utility.messageBD(this, "MustSelectLines", vars.getLanguage()));
      vars.setMessage("RefundCreate", myMessage);
    }

    printPageCreate(response, vars, strInvoiceDate, strCustomerId, strPriceListId, strDocTypeId,
        strOrgId, strSelected);
  }

  private void printPageCreate(HttpServletResponse response, VariablesSecureApp vars,
      String strInvoiceDate, String strCustomerId, String strPriceListId, String strDocTypeId,
      String strOrgId, String strSelected) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Print Create Sales Invoice");
    String strDescription = Utility.messageBD(this, "RefundCreate", vars.getLanguage());
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "com/sidesoft/localization/ecuador/refunds/ad_forms/RefundCreate").createXmlDocument();
    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\r\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("help", Replace.replace(strDescription, "\\n", "\n"));
    {
      OBError myMessage = vars.getMessage("RefundCreate");
      vars.removeMessage("RefundCreate");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    xmlDocument.setParameter("paramSelected", strSelected);
    xmlDocument.setParameter("paramInvoiceCustomerId", strCustomerId);
    xmlDocument.setParameter("paramInvoiceCustomerDescription", strCustomerId.equals("") ? ""
        : RefundData.bPartnerDescription(this, strCustomerId, vars.getLanguage()));
    xmlDocument.setParameter("invoiceDate", strInvoiceDate);
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("paramInvoiceOrgId", strOrgId);
    xmlDocument.setParameter("paramPriceListId", strPriceListId);
    xmlDocument.setParameter("paramDocTypeId", strDocTypeId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org is transactions allowed", Utility.getContext(this, vars, "#User_Org", "Refund"),
          Utility.getContext(this, vars, "#User_Client", "Refund"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "Refund", strOrgId);
      xmlDocument.setData("reportInvoiceOrg_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "M_Pricelist_ID",
          "", "Price List isSOTrx", Utility.getContext(this, vars, "#AccessibleOrgTree", "Refund"),
          Utility.getContext(this, vars, "#User_Client", "Refund"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "Refund", strPriceListId);
      xmlDocument.setData("reportPriceList_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "C_Doctype_ID",
          "", "C_DocType AR/AP Invoices and Credit Memos", Utility.getContext(this, vars,
              "#AccessibleOrgTree", "Refund"), Utility.getContext(this, vars, "#User_Client",
              "Refund"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "Refund", strDocTypeId);
      xmlDocument.setData("reportDocType_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError processSalesInvoice(VariablesSecureApp vars, String strSelected,
      String strDocType, String strInvoiceDate, String strCustomer, String strPriceListId,
      String strOrg) throws IOException, ServletException {
    StringBuffer textMessage = new StringBuffer();
    Connection conn = null;

    OBError myMessage = null;
    myMessage = new OBError();
    myMessage.setTitle("");

    String strPriceListVersionId = RefundData.getPricelistVersion(this, strPriceListId,
        strInvoiceDate);

    RefundData[] data1 = RefundData.selectCustomerData(this, strCustomer);
    if (data1[0].cPaymenttermId == null || data1[0].cPaymenttermId.equals("")) {
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "SSRE_CustomerWithNoPaymentTerm",
          vars.getLanguage()));
      return myMessage;
    }
    if ("".equals(RefundData.cBPartnerLocationId(this, strCustomer))) {
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "NoBPLocation", vars.getLanguage()));
      return myMessage;
    }

    try {
      conn = getTransactionConnection();
      String strCInvoiceId = SequenceIdData.getUUID();
      String strDocumentNo = Utility.getDocumentNo(this, vars, "", "C_Invoice", strDocType,
          strDocType, false, true);
      String cCurrencyId = RefundData.selectCurrency(this, strPriceListId);

      try {
        RefundData.insertCInvoice(conn, this, strCInvoiceId, vars.getClient(), strOrg,
            vars.getUser(), strDocumentNo, "DR", "CO", "0", strDocType, strInvoiceDate,
            strInvoiceDate, strCustomer, RefundData.billto(this, strCustomer), cCurrencyId,
            isAlternativeFinancialFlow() ? "P" : data1[0].paymentrule, data1[0].cPaymenttermId,
            strPriceListId, "", "", "", data1[0].finPaymentmethodId);
      } catch (ServletException ex) {
        myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
        releaseRollbackConnection(conn);
        return myMessage;
      }

      int line = 0;
      String strCInvoicelineID = "";

      RefundData[] lines = RefundData.linesToInvoice(
          this,
          strInvoiceDate,
          strOrg,
          vars.getWarehouse(),
          RefundData.billto(this, strCustomer).equals("") ? RefundData.cBPartnerLocationId(this,
              strCustomer) : RefundData.billto(this, strCustomer), RefundData.cBPartnerLocationId(
              this, strCustomer), strSelected);

      unlockRefundLines(vars, strSelected);
      strCInvoicelineID = SequenceIdData.getUUID();
      for (int i = 0; lines != null && i < lines.length; i++) {
        if ("".equals(lines[i].tax)) {
          Invoice il = OBDal.getInstance().get(Invoice.class, lines[i].cInvoiceId);
          myMessage.setType("Error");
          myMessage.setMessage(String.format(OBMessageUtils.messageBD("NoTaxRequisition"),
              il.getDocumentNo()));
          releaseRollbackConnection(conn);
          return myMessage;
        }

        line += 10;

        if (log4j.isDebugEnabled())
          log4j.debug("qtyInvoice: " + lines[i].qtyinvoiced + " new BigDecimal: "
              + (new BigDecimal(lines[i].qtyinvoiced)).toString());

        try {
          RefundData.insertCInvoiceline(conn, this, strCInvoicelineID, vars.getClient(), strOrg,
              vars.getUser(), strCInvoiceId, Integer.toString(line), strCustomer,
              lines[i].description, lines[i].mProductId, lines[i].mAttributesetinstanceId,
              lines[i].cUomId, lines[i].qtyinvoiced, lines[i].grandtotal, lines[i].grandtotal,
              lines[i].grandtotal, lines[i].grandtotal, lines[i].grandtotal, lines[i].grandtotal,
              lines[i].tax, "", lines[i].grandtotal, lines[i].cInvoiceId);
        } catch (ServletException ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          releaseRollbackConnection(conn);
          return myMessage;
        }
        strCInvoicelineID = SequenceIdData.getUUID();
      }

      for (int i = 0; lines != null && i < lines.length; i++) {
        RefundData.refunded(conn, this, vars.getUser(), lines[i].cInvoiceId);
      }

      OBError myMessageAux = cInvoicePost(conn, vars, strCInvoiceId);
      releaseCommitConnection(conn);
      String strWindowName = WindowTabsData.selectWindowInfo(this, vars.getLanguage(), "167");
      textMessage.append(strWindowName).append(" ").append(strDocumentNo).append(": ");
      if (myMessageAux.getMessage().equals(""))
        textMessage.append(Utility.messageBD(this, "Success", vars.getLanguage()));
      else
        textMessage.append(myMessageAux.getMessage());

      myMessage.setType(myMessageAux.getType());
      myMessage.setMessage(textMessage.toString());
      return myMessage;
    } catch (Exception e) {
      try {
        if (conn != null)
          releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "ProcessRunError", vars.getLanguage()));
      return myMessage;
    }
  }

  private OBError cInvoicePost(Connection conn, VariablesSecureApp vars, String strcInvoiceId)
      throws IOException, ServletException {
    String pinstance = SequenceIdData.getUUID();

    PInstanceProcessData.insertPInstance(conn, this, pinstance, "111", strcInvoiceId, "N",
        vars.getUser(), vars.getClient(), vars.getOrg());
    RefundData.cInvoicePost0(conn, this, pinstance);

    PInstanceProcessData[] pinstanceData = PInstanceProcessData.selectConnection(conn, this,
        pinstance);
    OBError myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
    return myMessage;
  }

  /**
   * Checks if the any module implements and alternative Financial Management preference. It should
   * be the Advanced Payables and Receivables module.
   * 
   * @return true if any module implements and alternative Financial Management preference.
   */
  private boolean isAlternativeFinancialFlow() {
    try {
      try {
        Preferences.getPreferenceValue("FinancialManagement", true, null, null, OBContext
            .getOBContext().getUser(), null, null);
      } catch (PropertyNotFoundException e) {
        return false;
      }
    } catch (PropertyException e) {
      return false;
    }
    return true;
  }

  public String getServletInfo() {
    return "Servlet Refund.";
  } // end of getServletInfo() method

}
