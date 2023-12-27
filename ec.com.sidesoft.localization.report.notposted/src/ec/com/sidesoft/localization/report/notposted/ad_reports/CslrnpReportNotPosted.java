package ec.com.sidesoft.localization.report.notposted.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.data.UtilSql;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class CslrnpReportNotPosted extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  // static Category log4j = Category.getInstance(CslrnpReportNotPosted.class);

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "ReportNotPosted|DateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportNotPosted|DateTo", "");
      printPageDataSheet(response, vars, strDateFrom, strDateTo);
    } else if (vars.commandIn("FIND")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom", "ReportNotPosted|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "ReportNotPosted|DateTo");
      printPageDataSheet(response, vars, strDateFrom, strDateTo);
    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDateFrom, String strDateTo) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;
    // if (strDateFrom.equals("") && strDateTo.equals("")) {
    // xmlDocument =
    // xmlEngine.readXmlTemplate("ec/com/sidesoft/localization/report/notposted/ReportNotPosted",
    // discard).createXmlDocument();
    // data = ReportNotPostedData.set();
    // } else {
    xmlDocument = xmlEngine
        .readXmlTemplate("ec/com/sidesoft/localization/report/notposted/ad_reports/ReportNotPosted")
        .createXmlDocument();

    // Use ReadOnly Connection Provider
    // ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();

    org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);

    String strProcessID = getUUID(cp);

    @SuppressWarnings("unused")
    String strExecuteScript = "";

    if (!cp.getRDBMS().equals("ORACLE")) {
      strExecuteScript = executeSQL(cp, vars.getLanguage(), vars.getClient(), strDateFrom,
          strDateTo, strProcessID);
    } else {
      String strresult = processSaveTableTmp(cp, vars.getLanguage(), vars.getClient(), strDateFrom,
          strDateTo, strProcessID);
    }

    CslrnpReportNotPostedData[] data = CslrnpReportNotPostedData.select(cp, vars.getLanguage(),
        vars.getClient(), strDateFrom, strDateTo, strProcessID);
    // }// DateTimeData.nDaysAfter

    ToolBar toolbar = new ToolBar(cp, vars.getLanguage(), "ReportNotPosted", false, "", "", "",
        false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();

    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(cp, vars,
          "ec.com.sidesoft.localization.report.notposted.ad_reports.CslrnpReportNotPosted");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(cp, vars.getLanguage(), "ReportNotPosted.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(cp, vars.getLanguage(), "ReportNotPosted.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportNotPosted");
      vars.removeMessage("ReportNotPosted");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    if (vars.commandIn("FIND") && data.length == 0) {
      // No data has been found. Show warning message.
      xmlDocument.setParameter("messageType", "WARNING");
      xmlDocument.setParameter("messageTitle",
          Utility.messageBD(cp, "ProcessStatus-W", vars.getLanguage()));
      xmlDocument.setParameter("messageMessage",
          Utility.messageBD(cp, "NoDataFound", vars.getLanguage()));
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setData("structure1", data);

    deleteDataTemp(cp, strProcessID);

    out.println(xmlDocument.print());
    out.close();
  }

  public static String getUUID(org.openbravo.database.ConnectionProvider connectionProvider)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "       SELECT get_uuid() as name" + "       FROM dual";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "name");
      }
      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public static String deleteDataTemp(org.openbravo.database.ConnectionProvider connectionProvider,
      String strProcessID) throws ServletException {
    String strSql = "";
    strSql = strSql + " delete from cslrnp_data_notposted where processid = '" + strProcessID + "'";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      int i = st.executeUpdate();

      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public static String executeSQL(org.openbravo.database.ConnectionProvider connectionProvider,
      String strLanguage, String strClient, String strStartDate, String StrEndDate,
      String strProcessID) throws ServletException {
    String strSql = "";
    strSql = strSql + "       SELECT cslrnp_execute_sql('" + strLanguage + "','" + strClient + "','"
        + strStartDate + "','" + StrEndDate + "','" + strProcessID + "') as process"
        + "       FROM dual";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "process");
      }
      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public static String processSaveTableTmp(
      org.openbravo.database.ConnectionProvider connectionProvider, String strLanguage,
      String strClient, String strStartDate, String StrEndDate, String strProcessID)
      throws ServletException {
    String strSql = "";
    // String strOrgId = strAdOrg!=null
    strSql = strSql + " call cslrnp_execute_sql2('" + strLanguage + "','" + strClient + "','"
        + strStartDate + "','" + StrEndDate + "','" + strProcessID + "')";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();

      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return ("OK");
  }

  public String getServletInfo() {
    return "Servlet ReportNotPosted. This Servlet was made by Juan Pablo Calvente";
  } // end of the getServletInfo() method
}
