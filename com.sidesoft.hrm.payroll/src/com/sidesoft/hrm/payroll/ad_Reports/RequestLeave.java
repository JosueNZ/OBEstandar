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

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.DocumentTemplate;
import org.openbravo.model.common.enterprise.Organization;
import com.sidesoft.hrm.payroll.ssprleaveemp;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;

@SuppressWarnings("serial")
public class RequestLeave extends HttpSecureAppServlet {
  private static Logger log4j = Logger.getLogger(RequestLeave.class);
  private String strADUSerID = "";
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @SuppressWarnings("unchecked")
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDocumentId = vars.getSessionValue("10EEAB9C1A8845FC8929DABBD7428E26|SSPR_LEAVE_EMP_ID");

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

  @SuppressWarnings("resource")
  private void printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strDocumentId) throws IOException, ServletException {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    String StrWindowdID = "";
    String StrTableID = "";
    StrWindowdID = "10EEAB9C1A8845FC8929DABBD7428E26";
    StrTableID = "881B6BC8F33E49168898C1FB4994099F";
    String StrTransactionID = vars.getSessionValue("10EEAB9C1A8845FC8929DABBD7428E26|SSPR_LEAVE_EMP_ID");
    strADUSerID = vars.getUser().toString();
    ssprleaveemp leaveemp = OBDal.getInstance()
        .get(ssprleaveemp.class, StrTransactionID);
    Organization ADOrg = OBDal.getInstance().get(Organization.class,
        leaveemp.getOrganization().getId().toString());

    Window ADWindow = OBDal.getInstance().get(Window.class, StrWindowdID);
    Table ADTable = OBDal.getInstance().get(Table.class, StrTableID);
    DocumentType doctype = OBDal.getInstance().get(DocumentType.class, leaveemp.getDocumentType().getId());

    OBCriteria<DocumentTemplate> PrintWithh = OBDal.getInstance().createCriteria(
        DocumentTemplate.class);
    //PrintWithh.add(Restrictions.eq(DocumentTemplate.PROPERTY_WINDOW, ADWindow));
    //PrintWithh.add(Restrictions.eq(DocumentTemplate.PROPERTY_TABLE, ADTable));
    PrintWithh.add(Restrictions.eq(DocumentTemplate.PROPERTY_DOCUMENTTYPE, doctype));

    
    List<DocumentTemplate> LstTemplate = PrintWithh.list();
    int ICountTemplate;
    ICountTemplate = LstTemplate.size();

    if (ICountTemplate == 0) {

      throw new OBException(Utility.messageBD(conn, "@Template no Found..@", language));

    }
    //if (LstTemplate.get(0).getWindow().getId().equals(StrWindowdID)) {

      String StrRutaReport = LstTemplate.get(0).getTemplateLocation();

      String strReportName = StrRutaReport + "/" + LstTemplate.get(0).getTemplateFilename();
      
      String strNombreFichero = LstTemplate.get(0).getTemplateFilename();

      final String strAttach = globalParameters.strFTPDirectory + "/284-" + classInfo.id;

      final String strLanguage = vars.getLanguage();

      final String strBaseDesign = getBaseDesignPath(strLanguage);

      strReportName = Replace.replace(
          Replace.replace(strReportName, "@basedesign@", strBaseDesign), "@attach@", strAttach);

      if (log4j.isDebugEnabled())
        log4j.debug("Output: Goods Movement - pdf");

      // VALIDACION PARA SQL

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("DOCUMENT_ID", strDocumentId);
      String StrBaseWeb = getBaseDesignPath(strLanguage);
      parameters.put("BASE_WEB", StrBaseWeb);
      String StrNameReport = LstTemplate.get(0).getReportFilename().replace(" ", "_") + ".jrxml";
      parameters.put("AD_USER_ID", strADUSerID);

      if(strNombreFichero.equals("RequestLeaveOccasional.jrxml")){
        parameters.put("AD_USER_ID", strADUSerID);
      }
      
   


      // response.setContentType("application/pdf;");
      // response.setHeader("Content-Disposition",
      // "attachment; filename=" + StrNameReport.replace(".jrxml", ".pdf"));

      String outputFile = "";
      outputFile = GetReport(strReportName, StrNameReport,strNombreFichero, parameters);

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

//    }

  }

  public String GetReport(String strReportName, String StrNameReport,String strNombreFichero,
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
      if(StrNameReport.equals("RequestLeaveOccasional.jrxml")){
      parameters.put("AD_USER_ID", strADUSerID);
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
