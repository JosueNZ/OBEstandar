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

//package org.openbravo.erpCommon.ad_forms;
package ec.com.sidesoft.localization.ecuador.resupply.ad_forms;

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
//import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

//import org.openbravo.model.procurement.RequisitionLine;
import ec.com.sidesoft.localization.ecuador.resupply.ssrsresupplyline;

public class RequisitionToOrder extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strProductId = vars.getGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID", "");
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "RequisitionToOrder|DateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo", "");
      String strRequesterId = vars.getGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID", "");
      String strVendorId = vars.getGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID", "");
      String strIncludeVendor = vars.getGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor", "Y");
      String strOrgId = vars.getGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID",
          vars.getOrg());
      String strDocumentNoFrom = vars.getGlobalVariable("inpFromDocumentNo",
          "RequisitionToOrder|FromDocumentNo", "");
      String strDocumentNoTo = vars.getGlobalVariable("inpToDocumentNo",
          "RequisitionToOrder|ToDocumentNo", "");
      vars.setSessionValue("RequisitionToOrder|isSOTrx", "N");
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId, strDocumentNoFrom, strDocumentNoTo);
    } else if (vars.commandIn("FIND")) {
      String strProductId = vars.getRequestGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "RequisitionToOrder|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo");
      String strRequesterId = vars.getRequestGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID");
      String strVendorId = vars.getRequestGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID");
      String strIncludeVendor = vars.getRequestGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      String strDocumentNoFrom = vars.getRequestGlobalVariable("inpFromDocumentNo",
          "RequisitionToOrder|FromDocumentNo");
      String strDocumentNoTo = vars.getRequestGlobalVariable("inpToDocumentNo",
          "RequisitionToOrder|ToDocumentNo");
      updateLockedLines(vars, strOrgId);
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId, strDocumentNoFrom, strDocumentNoTo);
    } else if (vars.commandIn("DELETE")) {
      String strProductId = vars.getRequestGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "RequisitionToOrder|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo");
      String strRequesterId = vars.getRequestGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID");
      String strVendorId = vars.getRequestGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID");
      String strIncludeVendor = vars.getRequestGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      String strDocumentNoFrom = vars.getRequestGlobalVariable("inpFromDocumentNo",
          "RequisitionToOrder|FromDocumentNo");
      String strDocumentNoTo = vars.getRequestGlobalVariable("inpToDocumentNo",
          "RequisitionToOrder|ToDocumentNo");
      String strRequisitionLines = vars.getRequiredInStringParameter("inpRequisitionLine",
          IsIDFilter.instance);
      deleteResupplyLine(vars, strRequisitionLines);
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId, strDocumentNoFrom, strDocumentNoTo);
    } else if (vars.commandIn("ADD")) {
      String strProductId = vars.getRequestGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "RequisitionToOrder|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo");
      String strRequesterId = vars.getRequestGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID");
      String strVendorId = vars.getRequestGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID");
      String strIncludeVendor = vars.getRequestGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      String strDocumentNoFrom = vars.getRequestGlobalVariable("inpFromDocumentNo",
          "RequisitionToOrder|FromDocumentNo");
      String strDocumentNoTo = vars.getRequestGlobalVariable("inpToDocumentNo",
          "RequisitionToOrder|ToDocumentNo");
      String strRequisitionLines = vars.getRequiredInStringParameter("inpRequisitionLine",
          IsIDFilter.instance);
      updateLockedLines(vars, strOrgId);
      lockRequisitionLines(vars, strRequisitionLines);
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId, strDocumentNoFrom, strDocumentNoTo);
    } else if (vars.commandIn("REMOVE")) {
      String strProductId = vars.getRequestGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "RequisitionToOrder|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo");
      String strRequesterId = vars.getRequestGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID");
      String strVendorId = vars.getRequestGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID");
      String strIncludeVendor = vars.getRequestGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      String strDocumentNoFrom = vars.getRequestGlobalVariable("inpFromDocumentNo",
          "RequisitionToOrder|FromDocumentNo");
      String strDocumentNoTo = vars.getRequestGlobalVariable("inpToDocumentNo",
          "RequisitionToOrder|ToDocumentNo");
      String strSelectedLines = vars.getRequiredInStringParameter("inpSelectedReq",
          IsIDFilter.instance);
      unlockRequisitionLines(vars, strSelectedLines);
      updateLockedLines(vars, strOrgId);
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId, strDocumentNoFrom, strDocumentNoTo);
    } else if (vars.commandIn("OPEN_CREATE")) {
      String strSelectedLines = vars.getRequiredInStringParameter("inpSelectedReq",
          IsIDFilter.instance);
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      updateLockedLines(vars, strOrgId);
      // checkSelectedRequisitionLines(response, vars, strSelectedLines);
    } else if (vars.commandIn("GENERATE")) {
      String strSelectedLines = vars.getRequiredInStringParameter("inpSelectedReq",
          IsIDFilter.instance);

      // // Start New development
      // final String strTableId = vars.getGlobalVariable("inpTableId", "CreateFrom|tableId");
      // // Close New development

      String strProductId = vars.getRequestGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "RequisitionToOrder|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo");
      String strRequesterId = vars.getRequestGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID");
      String strVendorId = vars.getRequestGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID");
      String strIncludeVendor = vars.getRequestGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor");
      String strDocumentNoFrom = vars.getRequestGlobalVariable("inpFromDocumentNo",
          "RequisitionToOrder|FromDocumentNo");
      String strDocumentNoTo = vars.getRequestGlobalVariable("inpToDocumentNo",
          "RequisitionToOrder|ToDocumentNo");
      String strOrderDate = DateTimeData.today(this);
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      boolean check = checkSelectedRequisitionLines(response, vars, strSelectedLines, strProductId,
          strDateFrom, strDateTo, strRequesterId, strVendorId, strIncludeVendor, strOrgId,
          strDocumentNoFrom, strDocumentNoTo);
      if (check) {
        updateLockedLines(vars, strOrgId);
        OBError myMessage1 = processMovementStorageReception(vars, strSelectedLines, strOrderDate,
            "", "", "", "");
        OBError myMessage = processPurchaseOrder(vars, strSelectedLines, strOrderDate, "", "",
            strOrgId, "");
        vars.setMessage("RequisitionToOrder", myMessage1);
        vars.setMessage("RequisitionToOrder", myMessage);

      }
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId, strDocumentNoFrom, strDocumentNoTo);
    } else
      pageError(response);
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strProductId, String strDateFrom, String strDateTo, String strRequesterId,
      String strVendorId, String strIncludeVendor, String strOrgId, String strDocumentNoFrom,
      String strDocumentNoTo) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;

    String strTreeOrg = ResupplyToMovementData.treeOrg(this, vars.getClient());
    ResupplyToMovementData[] datalines = ResupplyToMovementData.selectLines(this, vars
        .getLanguage(), Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"), Tree
        .getMembers(this, strTreeOrg, strOrgId), strDateFrom, DateTimeData.nDaysAfter(this,
        strDateTo, "1"), strProductId, strRequesterId, (strIncludeVendor.equals("Y") ? strVendorId
        : null), (strIncludeVendor.equals("Y") ? null : strVendorId),
        (strDocumentNoFrom.equals("") ? null : strDocumentNoFrom),
        (strDocumentNoTo.equals("") ? null : strDocumentNoTo));

    ResupplyToMovementData[] dataselected = ResupplyToMovementData.selectSelected(this,
        vars.getLanguage(), vars.getUser(),
        Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"),
        Tree.getMembers(this, strTreeOrg, strOrgId));
    String discard[] = { "" };
    if (dataselected == null || dataselected.length == 0) {
      dataselected = ResupplyToMovementData.set();
      discard[0] = "funcSelectedEvenOddRow";
    }
    xmlDocument = xmlEngine.readXmlTemplate(
        "ec/com/sidesoft/localization/ecuador/resupply/ad_forms/RequisitionToOrder", discard)
        .createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "RequisitionToOrder", false, "", "",
        "", false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "ec/com/sidesoft/localization/ecuador/resupply/ad_forms/RequisitionToOrder");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "RequisitionToOrder.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "RequisitionToOrder.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("RequisitionToOrder");
      vars.removeMessage("RequisitionToOrder");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("paramProductId", strProductId);
    xmlDocument.setParameter("paramProductDescription", strProductId.equals("") ? ""
        : ResupplyToMovementData.mProductDescription(this, strProductId, vars.getLanguage()));
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("paramRequester", strRequesterId);
    xmlDocument.setParameter("paramBPartnerId", strVendorId);
    xmlDocument.setParameter("paramBPartnerDescription", strVendorId.equals("") ? ""
        : ResupplyToMovementData.bPartnerDescription(this, strVendorId, vars.getLanguage()));
    xmlDocument.setParameter("paramShowNullVendor", strIncludeVendor);
    xmlDocument.setParameter("paramAdOrgId", strOrgId);
    xmlDocument.setParameter("documentNoFrom", strDocumentNoFrom);
    xmlDocument.setParameter("documentNoTo", strDocumentNoTo);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_User_ID", "",
          "UsersWithRequisition", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "RequisitionToOrder"), Utility.getContext(this, vars, "#User_Client",
              "RequisitionToOrder"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder",
          strRequesterId);
      xmlDocument.setData("reportRequester_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org Security validation", Utility.getContext(this, vars, "#User_Org",
              "RequisitionToOrder"), Utility.getContext(this, vars, "#User_Client",
              "RequisitionToOrder"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder", strOrgId);
      xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    // Hay que hacer la query del selected.

    xmlDocument.setData("structureSearch", datalines);
    xmlDocument.setData("structureSelected", dataselected);
    out.println(xmlDocument.print());
    out.close();
  }

  private void lockRequisitionLines(VariablesSecureApp vars, String strRequisitionLines)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Locking requisition lines: " + strRequisitionLines);
    ResupplyToMovementData.lock(this, vars.getUser(), strRequisitionLines);
  }

  private void unlockRequisitionLines(VariablesSecureApp vars, String strRequisitionLines)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Unlocking requisition lines: " + strRequisitionLines);
    ResupplyToMovementData.unlock(this, strRequisitionLines);
  }

  private void updateLockedLines(VariablesSecureApp vars, String strOrgId) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Update locked lines");
    String strTreeOrg = ResupplyToMovementData.treeOrg(this, vars.getClient());
    ResupplyToMovementData[] dataselected = ResupplyToMovementData.selectSelected(this,
        vars.getLanguage(), vars.getUser(),
        Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"),
        Tree.getMembers(this, strTreeOrg, strOrgId));
    for (int i = 0; dataselected != null && i < dataselected.length; i++) {
      String strLockQty = vars.getNumericParameter("inpQty" + dataselected[i].ssrsResupplylineId);
      String strLockPrice = vars.getNumericParameter("inpPrice"
          + dataselected[i].ssrsResupplylineId);
      ResupplyToMovementData.updateLock(this, strLockQty, strLockQty, strLockPrice,
          dataselected[i].ssrsResupplylineId);
    }
  }

  private void deleteResupplyLine(VariablesSecureApp vars, String strRequisitionLines)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Delete requisition lines: " + strRequisitionLines);
    ResupplyToMovementData.deleteResupply(this, strRequisitionLines);
  }

  private boolean checkSelectedRequisitionLines(HttpServletResponse response,
      VariablesSecureApp vars, String strSelected, String strProductId, String strDateFrom,
      String strDateTo, String strRequesterId, String strVendorId, String strIncludeVendor,
      String strOrgId, String strDocumentNoFrom, String strDocumentNoTo) throws IOException,
      ServletException {
    // if (log4j.isDebugEnabled())

    boolean check = true;
    if (!strSelected.equals("")) {
      ResupplyToMovementData[] linesvalidation = ResupplyToMovementData.linesToOrderValidate(this,
          strSelected);
      String resupplyvalidation = "";
      resupplyvalidation = linesvalidation[0].documentno;
      String strorgDoctypefrom = linesvalidation[0].orgDoctypefrom;
      String strordDoctypeto = linesvalidation[0].orgDoctypeto;
      String strorgDoctypefromreq = linesvalidation[0].orgDoctypefromreq;
      String strordDoctypetoreq = linesvalidation[0].orgDoctypetoreq;
      String DOCNOSEQUENCE_ID_FROM_SEND = linesvalidation[0].docnosequenceIdFromSend;
      String DOCNOSEQUENCE_ID_TO_SEND = linesvalidation[0].docnosequenceIdToSend;
      String DOCNOSEQUENCE_ID_FROM_RECEPTION = linesvalidation[0].docnosequenceIdFromReception;
      String DOCNOSEQUENCE_ID_TO_RECEPTION = linesvalidation[0].docnosequenceIdToReception;

      for (int v = 0; linesvalidation != null && v < linesvalidation.length; v++) {
        if (!resupplyvalidation.equals(linesvalidation[v].documentno)) {
          OBError myMessage = new OBError();
          myMessage.setTitle("");
          myMessage.setType("Error");
          myMessage.setMessage(String.format(
              OBMessageUtils.messageBD("ssrs_ValidationLineResupply"), resupplyvalidation));
          vars.setMessage("RequisitionToOrder", myMessage);
          check = false;
          break;
        } else {
          check = true;
        }
        String strLockQty = vars.getNumericParameter("inpQty"
            + linesvalidation[v].ssrsResupplylineId);
        int LockQty = Integer.parseInt(strLockQty);
        int StockQty = Integer.parseInt(linesvalidation[v].stockdis);
        if (LockQty > StockQty) {
          OBError myMessage = new OBError();
          myMessage.setTitle("");
          myMessage.setType("Error");
          myMessage.setMessage(String.format(OBMessageUtils.messageBD("ssrs_amountrequest"),
              resupplyvalidation, linesvalidation[v].line));
          vars.setMessage("RequisitionToOrder", myMessage);
          check = false;
          break;
        } else {
          check = true;
        }
        if (!strorgDoctypefrom.equals("") && !strordDoctypeto.equals("")
            && !strorgDoctypefromreq.equals(null) && !strordDoctypetoreq.equals(null)) {
          check = true;
        } else {
          OBError myMessage = new OBError();
          myMessage.setTitle("");
          myMessage.setType("Error");
          myMessage.setMessage(String.format(OBMessageUtils.messageBD("em_ssrs_Doctype_Org")));
          vars.setMessage("RequisitionToOrder", myMessage);
          check = false;
          break;
        }
        if (DOCNOSEQUENCE_ID_FROM_SEND.equals("") || DOCNOSEQUENCE_ID_TO_SEND.equals("")
            || DOCNOSEQUENCE_ID_FROM_RECEPTION.equals("")
            || DOCNOSEQUENCE_ID_TO_RECEPTION.equals("")) {
          OBError myMessage = new OBError();
          myMessage.setTitle("");
          myMessage.setType("Error");
          myMessage.setMessage(String.format(OBMessageUtils.messageBD("em_ssrs_documentnumber")));
          vars.setMessage("RequisitionToOrder", myMessage);
          check = false;
          break;
        } else {
          check = true;
        }
      }
    } else {
      OBError myMessage = new OBError();
      myMessage.setTitle("");
      myMessage.setType("Info");
      myMessage.setMessage(Utility.messageBD(this, "MustSelectLines", vars.getLanguage()));
      vars.setMessage("RequisitionToOrder", myMessage);
    }
    return check;
  }

  private void printPageCreate(HttpServletResponse response, VariablesSecureApp vars,
      String strOrderDate, String strVendorId, String strPriceListId, String strOrgId,
      String strSelected) throws IOException, ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Print Create Movement");
    String strDescription = Utility.messageBD(this, "ResupplyToMovementCreate", vars.getLanguage());
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "ec/com/sidesoft/localization/ecuador/resupply/ad_forms/ResupplyToMovementCreate")
        .createXmlDocument();
    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\r\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("help", Replace.replace(strDescription, "\\n", "\n"));
    xmlDocument.setParameter("paramLoginWarehouseId", vars.getSessionValue("#M_WAREHOUSE_ID"));
    {
      OBError myMessage = vars.getMessage("ResupplyToMovementCreate");
      vars.removeMessage("ResupplyToMovementCreate");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    xmlDocument.setParameter("paramSelected", strSelected);
    xmlDocument.setParameter("paramOrderVendorId", strVendorId);
    xmlDocument.setParameter("paramOrderVendorDescription", strVendorId.equals("") ? ""
        : ResupplyToMovementData.bPartnerDescription(this, strVendorId, vars.getLanguage()));
    xmlDocument.setParameter("orderDate", strOrderDate);
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("paramOrderOrgId", strOrgId);
    xmlDocument.setParameter(
        "arrayWarehouse",
        Utility.arrayDobleEntrada(
            "arrWarehouse",
            ResupplyToMovementData.selectWarehouseDouble(this, vars.getClient(),
                Utility.getContext(this, vars, "#AccessibleOrgTree", "RequisitionToOrder"),
                Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"))));
    xmlDocument.setParameter("paramPriceListId", strPriceListId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org is transactions allowed", Utility.getContext(this, vars, "#User_Org",
              "RequisitionToOrder"), Utility.getContext(this, vars, "#User_Client",
              "RequisitionToOrder"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder", strOrgId);
      xmlDocument.setData("reportOrderOrg_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "M_Pricelist_ID",
          "", "Purchase Pricelist", Utility.getContext(this, vars, "#AccessibleOrgTree",
              "RequisitionToOrder"), Utility.getContext(this, vars, "#User_Client",
              "RequisitionToOrder"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder",
          strPriceListId);
      xmlDocument.setData("reportPriceList_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError processPurchaseOrder(VariablesSecureApp vars, String strSelected,
      String strOrderDate, String strVendor, String strPriceListId, String strOrg,
      String strWarehouse) throws IOException, ServletException {
    StringBuffer textMessage = new StringBuffer();
    Connection conn = null;

    OBError myMessage = null;
    myMessage = new OBError();
    myMessage.setTitle("");
    try {

      String strOrgId = "";
      conn = getTransactionConnection();
      String strCOrderId = SequenceIdData.getUUID();
      ResupplyToMovementData[] Org = ResupplyToMovementData.selectOrg(this, vars.getLanguage(),
          strSelected);
      strOrgId = vars.getOrg(); // Org[0].adOrgId;

      // String docTargetType = ResupplyToMovementData.cDoctypeTarget(conn, this, vars.getClient(),
      // strOrgId);
      String strDocumentNo = "";
      String strDoctypefrom_id = "";
      ResupplyToMovementData[] head = ResupplyToMovementData.linesToOrderHead(this, strSelected);
      for (int j = 0; head != null && j < head.length; j++) {
        try {
          String strType = "Y";
          String strIsResupply = "Y";
          strDoctypefrom_id = ResupplyToMovementData.Documenttransactionsend(conn, this,
              head[j].adOrgIdReq);
          strDocumentNo = Utility.getDocumentNo(this, vars, "", "M_Movement", strDoctypefrom_id,
              strDoctypefrom_id, false, true);
          ResupplyToMovementData.insertCMovement(conn, this, strCOrderId, vars.getClient(),
              head[j].adOrgIdReq, vars.getUser(), strOrderDate, strDocumentNo,
              head[j].ssrsResupplyId, strIsResupply, strType, strDoctypefrom_id);

        } catch (ServletException ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          releaseRollbackConnection(conn);
          return myMessage;
        }

        int line = 0;
        String strCOrderlineID = "";
        BigDecimal qty = new BigDecimal("0");
        BigDecimal qtyOrder = new BigDecimal("0");
        boolean insertLine = false;
        ResupplyToMovementData[] lines = ResupplyToMovementData.linesToOrder(this,
            head[j].adOrgIdGroup, strSelected);
        for (int i = 0; lines != null && i < lines.length; i++) {
          if ("".equals(lines[i].tax)) {
            ssrsresupplyline rl = OBDal.getInstance().get(ssrsresupplyline.class,
                lines[i].ssrsResupplylineId);
            myMessage.setType("Error");
            myMessage.setMessage(String.format(OBMessageUtils.messageBD("NoTaxRequisition"),
                rl.getLineNo(), rl.getSsrsResupply().getDocumentNo()));
            releaseRollbackConnection(conn);
            return myMessage;
          }

          strCOrderlineID = SequenceIdData.getUUID();
          // if (i == lines.length - 1) {
          // insertLine = true;
          // } else if (!lines[i + 1].mProductId.equals(lines[i].mProductId)) {
          // insertLine = true;
          // qtyOrder = qty;
          // qty = new BigDecimal(0);
          // } else {
          // qty = qty.add(new BigDecimal(lines[i].lockqty));
          // }
          lines[i].cOrderlineId = strCOrderlineID;
          // if (insertLine) {
          insertLine = false;
          // line += 10;
          BigDecimal qtyAux = new BigDecimal(lines[i].lockqty);
          qtyOrder = qtyOrder.add(qtyAux);
          if (log4j.isDebugEnabled())
            log4j.debug("Lockqty: " + lines[i].lockqty + " qtyorder: " + qtyOrder.toPlainString()
                + " new BigDecimal: " + (new BigDecimal(lines[i].lockqty)).toString() + " qtyAux: "
                + qtyAux.toString());
          try {
            int qtyreq = Integer.parseInt(lines[i].lockqty);
            int qtyreqvar = qtyreq;
            ResupplyToMovementData[] lote = ResupplyToMovementData.stockStorageDetail(this,
                lines[i].mProductId, lines[i].emSsrsMWarehouseId);
            for (int l = 0; lote != null && l < lote.length; l++) {
              strCOrderlineID = SequenceIdData.getUUID();
              line += 10;

              if (Integer.parseInt(lote[l].stockdis) < qtyreqvar) {
                ResupplyToMovementData.insertCOrderline(conn, this, strCOrderlineID,
                    vars.getClient(), head[j].adOrgIdReq, vars.getUser(), strCOrderId,
                    lote[l].mLocatorIdVal2, lines[i].emSsrsMLocatortrnId, lines[i].mProductId,
                    Integer.toString(line), lote[l].stockdis, lote[l].mAttributesetinstanceId,
                    lines[i].cUomId, lines[i].mProductUomId, lines[i].lockqtyconversion,
                    lines[i].ssrsResupplyId);
                // ResupplyToMovementData.updateStockReserve(this, lote[l].stockact,
                // lote[l].mStorageDetailId);
                qtyreqvar = qtyreqvar - Integer.parseInt(lote[l].stockdis);
              } else {
                ResupplyToMovementData.insertCOrderline(conn, this, strCOrderlineID,
                    vars.getClient(), head[j].adOrgIdReq, vars.getUser(), strCOrderId,
                    lote[l].mLocatorIdVal2, lines[i].emSsrsMLocatortrnId, lines[i].mProductId,
                    Integer.toString(line), Integer.toString(qtyreqvar),
                    lote[l].mAttributesetinstanceId, lines[i].cUomId, lines[i].mProductUomId,
                    lines[i].lockqtyconversion, lines[i].ssrsResupplyId);
                // ResupplyToMovementData.updateStockReserve(this, Integer.toString(qtyreq),
                // lote[l].mStorageDetailId);
                // qtyreqvar = Integer.parseInt(lote[l].stockdis) - qtyreqvar;
                qtyreqvar = Integer.parseInt(lote[l].stockdis) - qtyreqvar;
                break;
              }
              // ResupplyToMovementData.insertCOrderline(conn, this, strCOrderlineID,
              // vars.getClient(),
              // strOrgId, vars.getUser(), strCOrderId, lines[i].mLocatorId,
              // lines[i].emSsrsMLocatortrnId, lines[i].mProductId, Integer.toString(line),
              // lines[i].lockqty, lines[i].mAttributesetinstanceId, lines[i].cUomId);
            }

          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
          strCOrderlineID = SequenceIdData.getUUID();
          // }
        }
        strCOrderId = SequenceIdData.getUUID();

        // unlockRequisitionLines(vars, strSelected);
        for (int i = 0; lines != null && i < lines.length; i++) {
          String strRequisitionOrderId = SequenceIdData.getUUID();
          try {
            ResupplyToMovementData.updateOrderedQty(this, lines[i].lockqty, vars.getUser(),
                lines[i].ssrsResupplylineId);
          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
          if (lines[i].toClose.equals("Y"))
            ResupplyToMovementData.resupplyStatus(conn, this, lines[i].ssrsResupplylineId,
                vars.getUser());
        }
      }
      // OBError myMessageAux = cOrderPost(conn, vars, strCOrderId);
      releaseCommitConnection(conn);
      String strWindowName = WindowTabsData.selectWindowInfo(this, vars.getLanguage(), "181");
      textMessage.append(strWindowName).append(" ").append(strDocumentNo).append(": ");
      // if (myMessageAux.getMessage().equals(""))
      // textMessage.append(Utility.messageBD(this, "Success", vars.getLanguage()));
      // else
      // textMessage.append(myMessageAux.getMessage());
      //
      // myMessage.setType(myMessageAux.getType());
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

  // **************************************
  // New developmen storage reception
  // **************************************

  private OBError processMovementStorageReception(VariablesSecureApp vars, String strSelected,
      String strOrderDate, String strVendor, String strPriceListId, String strOrg,
      String strWarehouse) throws IOException, ServletException {
    StringBuffer textMessage = new StringBuffer();
    Connection conn = null;

    OBError myMessage1 = null;
    myMessage1 = new OBError();
    myMessage1.setTitle("");

    try {
      String strOrgId = "";
      conn = getTransactionConnection();
      String strCOrderId = SequenceIdData.getUUID();
      strOrgId = vars.getOrg();
      // String docTargetType = ResupplyToMovementData.cDoctypeTarget(conn, this, vars.getClient(),
      // strOrgId);
      String strDocumentNo = "";
      String strDoctypeto_id = "";
      ResupplyToMovementData[] head = ResupplyToMovementData.linesToOrderHead(this, strSelected);
      for (int j = 0; head != null && j < head.length; j++) {
        try {
          String strType = "N";
          String strIsResupply = "Y";
          strDoctypeto_id = ResupplyToMovementData.Documenttransactionreception(conn, this,
              head[j].adOrgIdGroup);
          strDocumentNo = Utility.getDocumentNo(this, vars, "", "M_Movement", strDoctypeto_id,
              strDoctypeto_id, false, true);
          ResupplyToMovementData.insertCMovement(conn, this, strCOrderId, vars.getClient(),
              head[j].adOrgIdGroup, vars.getUser(), strOrderDate, strDocumentNo,
              head[j].ssrsResupplyId, strIsResupply, strType, strDoctypeto_id);
        } catch (ServletException ex) {
          myMessage1 = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          releaseRollbackConnection(conn);
          return myMessage1;
        }

        int line = 0;
        String strCOrderlineID = "";
        ResupplyToMovementData[] lines = ResupplyToMovementData.linesToOrder(this,
            head[j].adOrgIdGroup, strSelected);
        for (int i = 0; lines != null && i < lines.length; i++) {
          if ("".equals(lines[i].tax)) {
            ssrsresupplyline rl = OBDal.getInstance().get(ssrsresupplyline.class,
                lines[i].ssrsResupplylineId);
            myMessage1.setType("Error");
            myMessage1.setMessage(String.format(OBMessageUtils.messageBD("NoTaxRequisition"),
                rl.getLineNo(), rl.getSsrsResupply().getDocumentNo()));
            releaseRollbackConnection(conn);
            return myMessage1;
          }

          strCOrderlineID = SequenceIdData.getUUID();
          lines[i].cOrderlineId = strCOrderlineID;
          // line += 10;
          try {
            int qtyreq = Integer.parseInt(lines[i].lockqty);
            int qtyreqvar = qtyreq;
            ResupplyToMovementData[] lote = ResupplyToMovementData.stockStorageDetail(this,
                lines[i].mProductId, lines[i].emSsrsMWarehouseId);
            for (int l = 0; lote != null && l < lote.length; l++) {
              strCOrderlineID = SequenceIdData.getUUID();
              line += 10;

              if (Integer.parseInt(lote[l].stockdis) < qtyreqvar) {
                ResupplyToMovementData.insertCOrderline(conn, this, strCOrderlineID,
                    vars.getClient(), head[j].adOrgIdGroup, vars.getUser(), strCOrderId,
                    lines[i].emSsrsMLocatortrnId, lines[i].emSsrsMLocatorrcpId,
                    lines[i].mProductId, Integer.toString(line), lines[i].cero,
                    lote[l].mAttributesetinstanceId, lines[i].cUomId, lines[i].mProductUomId,
                    lines[i].cer0conversion, lines[i].ssrsResupplyId);
                // ResupplyToMovementData.updateStockReserve(this, lote[l].stockact,
                // lote[l].mStorageDetailId);
                qtyreqvar = qtyreqvar - Integer.parseInt(lote[l].stockdis);
              } else {
                ResupplyToMovementData.insertCOrderline(conn, this, strCOrderlineID,
                    vars.getClient(), head[j].adOrgIdGroup, vars.getUser(), strCOrderId,
                    lines[i].emSsrsMLocatortrnId, lines[i].emSsrsMLocatorrcpId,
                    lines[i].mProductId, Integer.toString(line), lines[i].cero,
                    lote[l].mAttributesetinstanceId, lines[i].cUomId, lines[i].mProductUomId,
                    lines[i].cer0conversion, lines[i].ssrsResupplyId);
                // ResupplyToMovementData.updateStockReserve(this, Integer.toString(qtyreq),
                // lote[l].mStorageDetailId);
                qtyreqvar = Integer.parseInt(lote[l].stockdis) - qtyreqvar;
                break;
              }
            }
            // ResupplyToMovementData.insertCOrderline(conn, this, strCOrderlineID,
            // vars.getClient(),
            // strOrgId, vars.getUser(), strCOrderId, lines[i].emSsrsMLocatortrnId,
            // lines[i].emSsrsMLocatorrcpId, lines[i].mProductId, Integer.toString(line),
            // lines[i].cero, lines[i].mAttributesetinstanceId, lines[i].cUomId);

          } catch (ServletException ex) {
            myMessage1 = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage1;
          }
          strCOrderlineID = SequenceIdData.getUUID();
        }
        strCOrderId = SequenceIdData.getUUID();
      }

      // OBError myMessageAux = cOrderPost(conn, vars, strCOrderId);
      releaseCommitConnection(conn);
      String strWindowName = WindowTabsData.selectWindowInfo(this, vars.getLanguage(), "181");
      textMessage.append(strWindowName).append(" ").append(strDocumentNo).append(": ");
      // if (myMessageAux.getMessage().equals(""))
      // textMessage.append(Utility.messageBD(this, "Success", vars.getLanguage()));
      // else
      // textMessage.append(myMessageAux.getMessage());
      //
      // myMessage1.setType(myMessageAux.getType());
      myMessage1.setMessage(textMessage.toString());
      return myMessage1;
    } catch (Exception e) {
      try {
        if (conn != null)
          releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage1.setType("Error");
      myMessage1.setMessage(Utility.messageBD(this, "ProcessRunError", vars.getLanguage()));
      return myMessage1;
    }
  }

  // ****************************************
  // CLose new development storage reception
  // ****************************************

  private OBError cOrderPost(Connection conn, VariablesSecureApp vars, String strcOrderId)
      throws IOException, ServletException {
    String pinstance = SequenceIdData.getUUID();

    PInstanceProcessData.insertPInstance(conn, this, pinstance, "104", strcOrderId, "N",
        vars.getUser(), vars.getClient(), vars.getOrg());
    ResupplyToMovementData.cOrderPost0(conn, this, pinstance);

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
    return "Servlet RequisitionToOrder.";
  } // end of getServletInfo() method
}
