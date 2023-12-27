package ec.com.sidesoft.hrm.payroll.payment.rol.ad_process;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
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
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.utils.Replace;

import ec.com.sidesoft.custom.reports.SescrTemplateReport;

public class SSPRPR_PrintReportPaymentRol extends HttpSecureAppServlet {

  private static final long serialVersionUID = 1L;
  private static Logger log4j = Logger.getLogger(SSPRPR_PrintReportPaymentRol.class);
  private ProcessLogger logger;
  public String str_Atachment, str_FTP;
  public Connection connection_DB = null;
  
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public String doPost(HttpServletRequest request, HttpServletResponse response,
      String strAtachments, String strFTP, Connection DBConnection, String formarReport, String customReportName, String cedulaEmpleado, 
      String empleadoID, String documentno, String strADUSerID)
      throws IOException, ServletException {
    
    VariablesSecureApp vars = new VariablesSecureApp(request);
    
    String outputFile = "";    
    str_Atachment = strAtachments;
    str_FTP = strFTP;
    connection_DB = DBConnection;

    outputFile = printPagePDF(response, vars, str_Atachment, str_FTP, formarReport, customReportName, cedulaEmpleado, empleadoID, documentno,strADUSerID);
    
    return outputFile;
  }

  private String printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strAtachment, String str_FtpDirecotry, String formarReport, String customReportName, String cedulaEmpleado, 
      String empleadoID, String documentno,String strADUSerID) throws IOException,
      ServletException {

	ConnectionProvider conn = new DalConnectionProvider(false);
	String language = OBContext.getOBContext().getLanguage().getLanguage();	  
	  
    String outputFile = "";  
    String strReportName = "", StrNameReport="";
    Window ADWindow = OBDal.getInstance().get(Window.class, "FD76646320E84CEB8F243C1A6628020B");
    Table ADTable = OBDal.getInstance().get(Table.class, "BCEEDEC4FE2B4B3FAEA32084BB88AD95");

    OBCriteria<SescrTemplateReport> PrintWithh = OBDal.getInstance()
        .createCriteria(SescrTemplateReport.class);
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_WINDOW, ADWindow));
    PrintWithh.add(Restrictions.eq(SescrTemplateReport.PROPERTY_TABLE, ADTable));
    
    if (PrintWithh.list().size() == 0) {    	
    	strReportName = "@basedesign@/com/sidesoft/hrm/payroll/ad_Reports/Rptm_IndividualPayroll.jrxml";
    	StrNameReport = "Rptm_IndividualPayroll.jrxml";
    }else {
    	strReportName = PrintWithh.list().get(0).getTemplateDir()+PrintWithh.list().get(0).getNameReport();
    	StrNameReport = PrintWithh.list().get(0).getNameReport();	
    }
    
    final String strAttach = strAtachment;
    
    strReportName = Replace.replace(Replace.replace(strReportName, "@basedesign@", strAttach),
        "@attach@", strAttach);
    
    if (log4j.isDebugEnabled())
      log4j.debug("Output: Payment Rol - pdf");

    // PARAMETROS DEL REPORTE
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("c_bpartner_id", empleadoID);
    parameters.put("documentno", documentno);
    parameters.put("AD_User_ID", strADUSerID);
    parameters.put("BASE_DESIGN", strAttach);
    
    
    outputFile = GetReport(strReportName, StrNameReport, parameters, formarReport, customReportName, cedulaEmpleado);
    
    return outputFile;

  }

  public String GetReport(String strReportName, String StrNameReport,
      HashMap<String, Object> parameters, String format, String customReportName, String cedulaEmpleado) {
    
    JasperReport report = null;
    String SrtLinkReport = null;
    SrtLinkReport = strReportName;
    JasperPrint print;
    String customFileName = customReportName;
    
    // NOMBRE CON QUE SE GUARDA EL PDF EN LA CARPETA ATTACHMENTS
    final String outputFile = str_FTP + "/Nomina/Mensual/" + cedulaEmpleado + "/" + customFileName;
    
    String directoryPath = str_FTP + "/Nomina/Mensual/" + cedulaEmpleado +"/";
    File file = new File(directoryPath);

    // SI EL PATH NO ES UN DIRECTORIO ENTONCES LO CREA
    if (!file.isDirectory()) {
      boolean dirCreated = file.mkdirs();
      if(dirCreated) {
        //logger.logln("Directory created sucessful" + directoryPath);
      }        
    }   
    
    try {

      parameters.put("REPORT_CONNECTION", connection_DB);
      report = JasperCompileManager.compileReport(SrtLinkReport);
      print = JasperFillManager.fillReport(report, parameters, connection_DB);
      JasperExportManager.exportReportToPdfFile(print, outputFile);

    } catch (Exception e) {
      throw new OBException("Error template: " + e.getMessage().toString());
    }

    return outputFile;
  }
  
  public String getServletInfo() {
    return "Servlet that processes the print action";
  } // End of getServletInfo() method
}