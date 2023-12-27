	package com.sidesoft.hrm.payroll.ad_callouts;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.xmlEngine.XmlDocument;

public class Add_Concept extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
      ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    if (vars.commandIn("DEFAULT")) {
      String strChanged = vars.getStringParameter("inpLastFieldChanged");
      if (log4j.isDebugEnabled())
        log4j.debug("CHANGED: " + strChanged);
      String strConceptFormulaId = vars.getStringParameter("inpssprConceptFormulaId");
      String strFormula = vars.getStringParameter("inpformula");

      try {
        printPage(response, vars, strChanged, strConceptFormulaId, strFormula);
      } catch (ServletException ex) {
        pageErrorCallOut(response);
      }
    } else
      pageError(response);
  }

  void printPage(HttpServletResponse response, VariablesSecureApp vars, String strChanged,
      String strConceptFormulaId, String strFormula) throws IOException,
      ServletException {
    if (log4j.isDebugEnabled())
      log4j.debug("Output: dataSheet");
    XmlDocument xmlDocument = xmlEngine.readXmlTemplate(
        "org/openbravo/erpCommon/ad_callouts/CallOut").createXmlDocument();

    if (strConceptFormulaId != null && !strConceptFormulaId.equals(""))
      {
        String strConceptFormulaValue = AddConceptData.select(this, strConceptFormulaId);
        strFormula = strFormula + " <" + strConceptFormulaValue + ">";
      }

    StringBuffer resultado = new StringBuffer();
    resultado.append("var calloutName='Add_Concept';\n\n");
    resultado.append("var respuesta = new Array(");

    resultado.append("new Array(\"inpformula\", \"" + strFormula.trim() + "\")");

    resultado.append(");");
    xmlDocument.setParameter("array", resultado.toString());
    xmlDocument.setParameter("frameName", "appFrame");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }
}
