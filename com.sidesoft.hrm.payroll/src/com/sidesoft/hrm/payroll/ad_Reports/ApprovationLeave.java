/*
 * ************************************************************************ The
 * contents of this file are subject to the Openbravo Public License Version 1.1
 * (the "License"), being the Mozilla Public License Version 1.1 with a
 * permitted attribution clause; you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.openbravo.com/legal/license.html Software distributed under the
 * License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing rights and limitations under the License. The Original Code is
 * Openbravo ERP. The Initial Developer of the Original Code is Openbravo SLU All
 * portions are Copyright (C) 2001-2010 Openbravo SLU All Rights Reserved.
 * Contributor(s): ______________________________________.
 * ***********************************************************************
 */
package com.sidesoft.hrm.payroll.ad_Reports;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentTemplate;
import org.openbravo.model.common.enterprise.DocumentType;


import com.sidesoft.hrm.payroll.ssprleaveemp;

@SuppressWarnings("serial")
public class ApprovationLeave extends HttpSecureAppServlet {
  private static Logger log4j = Logger.getLogger(ApprovationLeave.class);

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @SuppressWarnings("unchecked")
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars
          .getSessionValue("1143BE455E91420CB1DFE4AA45EC8566|SSPR_LEAVE_EMP_ID");

      // normalize the string of ids to a comma separated list
      strDocumentId = strDocumentId.replaceAll("\\(|\\)|'", "");
      if (strDocumentId.length() == 0)
        throw new ServletException(Utility.messageBD(this, "NoDocument", vars.getLanguage()));

      if (log4j.isDebugEnabled())
        log4j.debug("strDocumentId: " + strDocumentId);
      printPagePDF(response, vars, strDocumentId);
    } else {
      pageError(response);
    }
  }

  private void printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId) throws IOException, ServletException {

    // OBTENGO TIPO DE DOCUMENTO Y RUTA DEL REPORTE

    // REF. DOCTYPE
    ssprleaveemp ObjLeaveEmp = OBDal.getInstance().get(ssprleaveemp.class, strDocumentId);

    String DocType_ID = ObjLeaveEmp.getDocumentType().getId();
    DocumentType ObjDocType = OBDal.getInstance().get(DocumentType.class, DocType_ID);

    // OBCriteria<DocumentType> ObjDocumentType = OBDal.getInstance().createCriteria(
    // DocumentType.class);
    // ObjDocumentType.add(Restrictions.eq(DocumentType.PROPERTY_ID, ObjDocType));

    // Ruta
    OBCriteria<DocumentTemplate> ObjDocumentTem = OBDal.getInstance().createCriteria(
        DocumentTemplate.class);
    ObjDocumentTem.add(Restrictions.eq(DocumentTemplate.PROPERTY_DOCUMENTTYPE, ObjDocType));

    List<DocumentTemplate> OBtemplatetcriteria = ObjDocumentTem.list();
    String StrLocation = OBtemplatetcriteria.get(0).getTemplateLocation().toString();
    String StrFile = OBtemplatetcriteria.get(0).getReportFilename().toString();
    String StrRuta = StrLocation + StrFile;

    /*
     * String StrLocation = ObjDocumentTem.list().get(0).getTemplateLocation().toString(); String
     * StrFile = ObjDocumentTem.list().get(0).getReportFilename().toString(); String StrRuta =
     * StrLocation + StrFile;
     * 
     * DocumentTemplate ObjDocumentT = OBDal.getInstance().get(DocumentTemplate.class,
     * ObjDocumentTem.list().get(0).getId());
     * 
     * String StrLocation = ObjDocumentT.getTemplateLocation(); String StrFile =
     * ObjDocumentT.getReportFilename(); String StrRuta = StrLocation + StrFile;
     */
    // FIN OBTENGO TIPO DE DOCUMENTO Y RUTA DEL REPORTE

    if (log4j.isDebugEnabled())
      log4j.debug("Output: Rpt_RequestLeave - pdf");

    // VALIDACION PARA SQL

    if (StrFile.equals("RequestLeaveVacation.jrxml")) {
      String strUser = OBContext.getOBContext().getUser().getId();

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("AD_USER_ID", strUser);
      parameters.put("DOCUMENT_ID", strDocumentId);
      String strReportName = StrRuta;
      renderJR(vars, response, strReportName, "pdf", parameters, null, null);

    }

    if (StrFile.equals("RequestLeaveDomesticCalamity.jrxml")) {
      RptRequestLeave3Data[] data = RptRequestLeave3Data.select3(this, strDocumentId);
      if (log4j.isDebugEnabled())
        log4j.debug("data: " + (data == null ? "null" : "not null"));
      if (data == null || data.length == 0)
        data = RptRequestLeave3Data.set();

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DOCUMENT_ID", strDocumentId);
      String strReportName = StrRuta;
      renderJR(vars, response, strReportName, "pdf", parameters, data, null);
    }

    if (StrFile.equals("RequestLeaveOccasional.jrxml")) {
      RptRequestLeave4Data[] data = RptRequestLeave4Data.select4(this, strDocumentId);
      if (log4j.isDebugEnabled())
        log4j.debug("data: " + (data == null ? "null" : "not null"));
      if (data == null || data.length == 0)
        data = RptRequestLeave4Data.set();

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DOCUMENT_ID", strDocumentId);
      String strReportName = StrRuta;
      renderJR(vars, response, strReportName, "pdf", parameters, data, null);
    }

    if (StrFile.equals("RequestLeaveCommission.jrxml")) {
      RptRequestLeave5Data[] data = RptRequestLeave5Data.select5(this, strDocumentId);
      if (log4j.isDebugEnabled())
        log4j.debug("data: " + (data == null ? "null" : "not null"));
      if (data == null || data.length == 0)
        data = RptRequestLeave5Data.set();

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DOCUMENT_ID", strDocumentId);
      String strReportName = StrRuta;
      renderJR(vars, response, strReportName, "pdf", parameters, data, null);
    }

    if (StrFile.equals("RequestLeaveVacationCustom.jrxml")) {
      RptRequestLeaveVacCustomData[] data = RptRequestLeaveVacCustomData.selectv(this, strDocumentId);
      if (log4j.isDebugEnabled())
        log4j.debug("data: " + (data == null ? "null" : "not null"));
      if (data == null || data.length == 0)
        data = RptRequestLeaveVacCustomData.set();

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DOCUMENT_ID", strDocumentId);
      String strReportName = StrRuta;
      renderJR(vars, response, strReportName, "pdf", parameters, data, null);

    }


  }

  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}
