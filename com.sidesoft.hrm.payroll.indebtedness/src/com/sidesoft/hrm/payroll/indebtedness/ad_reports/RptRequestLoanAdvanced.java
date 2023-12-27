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
package com.sidesoft.hrm.payroll.indebtedness.ad_reports;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.System;
import org.openbravo.model.common.enterprise.DocumentTemplate;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;

import com.sidesoft.hrm.payroll.ssprleaveemp;
import com.sidesoft.hrm.payroll.ssprloans;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

@SuppressWarnings("serial")
public class RptRequestLoanAdvanced extends HttpSecureAppServlet {
  private static Logger log4j = Logger.getLogger(RptRequestLoanAdvanced.class);
  private String strADUSerID = "";

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @SuppressWarnings("unchecked")
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars.getSessionValue("DC31E6F121DF4D76B896CCE17FF3E699|SSPR_LOANS_ID");
      strADUSerID = vars.getUser().toString();
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
    ConnectionProvider conn = new DalConnectionProvider(false);

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ssprloans Objloans = OBDal.getInstance().get(ssprloans.class, strDocumentId);
    String DocType_ID = Objloans.getSfprCDoctype().getId();
    DocumentType ObjDocType = OBDal.getInstance().get(DocumentType.class, DocType_ID);

    // Ruta
    OBCriteria<DocumentTemplate> ObjDocumentTem = OBDal.getInstance()
        .createCriteria(DocumentTemplate.class);
    ObjDocumentTem.add(Restrictions.eq(DocumentTemplate.PROPERTY_DOCUMENTTYPE, ObjDocType));

    List<DocumentTemplate> OBtemplatetcriteria = ObjDocumentTem.list();

    if (OBtemplatetcriteria.size() == 0) {

      throw new OBException(Utility.messageBD(conn, "@Template no Found..@", language));

    }
    /*
     * String StrLocation = OBtemplatetcriteria.get(0).getTemplateLocation().toString(); String
     * StrFile = OBtemplatetcriteria.get(0).getReportFilename().toString(); String StrRuta =
     * StrLocation + StrFile;
     */
    // FIN OBTENGO TIPO DE DOCUMENTO Y RUTA DEL REPORTE

    if (log4j.isDebugEnabled())
      log4j.debug("Output: RptRequestLoanAdvanced - pdf");

    // VALIDACION PARA SQL

    String StrRutaReport = OBtemplatetcriteria.get(0).getTemplateLocation();

    String strReportName = StrRutaReport + "/" + OBtemplatetcriteria.get(0).getTemplateFilename();

    String strNombreFichero = OBtemplatetcriteria.get(0).getTemplateFilename();

    final String strAttach = globalParameters.strFTPDirectory + "/284-" + classInfo.id;

    final String strLanguage = vars.getLanguage();

    final String strBaseDesign = getBaseDesignPath(strLanguage);

    strReportName = Replace.replace(Replace.replace(strReportName, "@basedesign@", strBaseDesign),
        "@attach@", strAttach);
    String outputFile = "";

    if (strNombreFichero.equals("RptRequestLoanAdvanced.jrxml")) {

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      String StrBaseWeb = getBaseDesignPath(strLanguage);
      parameters.put("BASE_WEB", StrBaseWeb);
      String StrNameReport = OBtemplatetcriteria.get(0).getReportFilename().replace(" ", "_")
          + ".jrxml";

      parameters.put("AD_USER_ID", strADUSerID);
      parameters.put("sspr_loans_id", strDocumentId);

      outputFile = GetReport(strReportName, StrNameReport, strNombreFichero, parameters);

    }

    if (strNombreFichero.equals("RptRequestAdvance.jrxml")) {
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("sspr_loans_id", strDocumentId);
      String StrBaseWeb = getBaseDesignPath(strLanguage);
      parameters.put("BASE_WEB", StrBaseWeb);
      String StrNameReport = OBtemplatetcriteria.get(0).getReportFilename().replace(" ", "_")
          + ".jrxml";

      outputFile = GetReport(strReportName, StrNameReport, strNombreFichero, parameters);
    }

    File pdfFile = new File(outputFile);
    response.setContentLength((int) pdfFile.length());

    try {
      FileInputStream fileInputStream = null;
      fileInputStream = new FileInputStream(pdfFile);
      OutputStream responseOutputStream = response.getOutputStream();
      int bytes;
      while ((bytes = fileInputStream.read()) != -1) {
        responseOutputStream.write(bytes);
      }
      responseOutputStream.close();
      responseOutputStream.flush();
      printPageClosePopUpAndRefreshParent(response, vars);
    } catch (Exception e) {

    }
  }

  public String GetReport(String strReportName, String StrNameReport, String strNombreFichero,
      HashMap<String, Object> parameters) {
    JasperReport report = null;

    String SrtLinkReport = null;
    SrtLinkReport = strReportName;
    JasperPrint print;
    Connection con = null;
    final String outputFile = globalParameters.strFTPDirectory + "/"
        + StrNameReport.replace(".jrxml", ".pdf");

    try {
      con = getTransactionConnection();

      parameters.put("REPORT_CONNECTION", con);
      if (StrNameReport.equals("RptRequestLoanAdvanced.jrxml")) {
        parameters.put("AD_USER_ID", con);
      }
      report = JasperCompileManager.compileReport(SrtLinkReport);
      print = JasperFillManager.fillReport(report, parameters, con);
      JasperExportManager.exportReportToPdfFile(print, outputFile);
      con.close();

    } catch (Exception e) {
      // log4j.debug("Error: Goods Movement - pdf");
      throw new OBException("Error template: " + e.getMessage().toString());
    }

    return outputFile;
  }

  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}
