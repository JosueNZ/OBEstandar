package ec.com.sidesoft.payroll.events.background;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.payroll.events.SPEVConfigIauditor;
import ec.com.sidesoft.payroll.events.SPEVConfigTemplate;
import ec.com.sidesoft.payroll.events.SPEVTempAuditor;

public class PayrollEventsChecklistBonus extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(PayrollEventsChecklistBonus.class);
  private ProcessLogger logger;
  String msgTitle = "";
  String msgMessage = "";
  String msgType = ""; // success, warning or error
  public ConfigParameters cf;

  private static String urlWS = "";
  private static String token = "";

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    cf = bundle.getConfig(); // Obtener la configuración de la App OB
    logger = bundle.getLogger();
    OBError result = new OBError();
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    try {
      OBContext.setAdminMode(false);
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));

      String message;

      // CONSUMO EL SERVICIO DE IAUDITOR
      JSONObject consumews = consumeIAuditorWS(logger);
      message = String.format(consumews.getString("message"), consumews.getString("error"));
      logger.logln(message);

      // HAGO LA LLAMADA A LA FUNCION POSTGRES BONUS CHECKLIST
      bonusChecklist();

    } catch (Exception e) {
      result.setTitle(Utility.messageBD(conn, "Error", language));
      result.setType("Error");
      result.setMessage(e.getMessage());
      log4j.error(result.getMessage(), e);
      logger.logln(result.getMessage());
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  private static JSONObject consumeIAuditorWS(ProcessLogger logger)
      throws JSONException, ParseException {

    JSONObject resultLogger = new JSONObject();

    OBCriteria<SPEVConfigIauditor> config = OBDal.getInstance()
        .createCriteria(SPEVConfigIauditor.class);
    config.add(Restrictions.eq(SPEVConfigIauditor.PROPERTY_ACTIVE, true));
    config.addOrderBy(SPEVConfigIauditor.PROPERTY_CREATIONDATE, false);
    config.setMaxResults(1);

    if (config.list() != null && config.list().size() > 0) {

      urlWS = config.list().get(0).getUrlbase().trim();
      token = config.list().get(0).getToken().trim();

      try {

        String[] fechas = startEndDate();

        String urlWSSSS = urlWS
            + "search?field=audit_id&field=modified_at&field=template_id&modified_after="
            + fechas[0] + "&modified_before=" + fechas[1];

        logger.logln("URL SERVICIO: " + urlWSSSS);

        // VERIFICO QUE NO EXISTAN REGISTROS EN LA TABLA TEMPORAL PARA EL PERIODO EN CURSO
        Integer verify = verifyDuplicity();

        if (verify > 0) {

          // YA HAY REGISTROS EN LA TABLA TEMPORAL
          logger.logln("Ya existen registros en la tabla temporal para las fechas " + fechas[0]
              + " hasta el " + fechas[1]);

          resultLogger.put("error", "1");
          resultLogger.put("message", "Ya existen registros en la tabla temporal para las fechas "
              + fechas[0] + " hasta el " + fechas[1]);

        } else {

          SSLContext sc = null;
          try {
            sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, null, new java.security.SecureRandom());
          } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
          } // $NON-NLS-1$
          catch (KeyManagementException e) {
            e.printStackTrace();
          }

          URL url = new URL(
              urlWS + "search?field=audit_id&field=modified_at&field=template_id&modified_after="
                  + fechas[0] + "&modified_before=" + fechas[1]);
          HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
          conn.setSSLSocketFactory(sc.getSocketFactory());
          conn.setRequestMethod("GET");
          conn.setRequestProperty("Authorization", token);

          if (conn.getResponseCode() != 200) {

            String response;

            InputStream is = conn.getErrorStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            while ((response = rd.readLine()) != null) {

              JSONObject dataError = new JSONObject(response);
              logger.logln("Hubo un error al consultar las auditorias desde " + fechas[0]
                  + " hasta el " + fechas[1] + " Error: " + conn.getResponseCode() + " - " + dataError.getString("message").trim());
              resultLogger.put("error", "1");
              resultLogger.put("message", "Hubo un error al consultar las auditorias desde "
                  + fechas[0] + " hasta el " + fechas[1]);
            }

          } else {

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {

              // CONVIERTO LA RESPUESTA EN UN OBJETO JSON
              JSONObject data = new JSONObject(output);

              // OBTENGO EL ARRAY DE OBJETOS DE LAS AUDITORIAS
              JSONArray audits = data.getJSONArray("audits");

              // ITERO SOBRE EL ARRAY DE AUDITS
              for (int i = 0; i < audits.length(); i++) {

                JSONObject jsonAuditLine = audits.getJSONObject(i);

                String audit_id = jsonAuditLine.getString("audit_id").trim();
                String template_id = jsonAuditLine.getString("template_id").trim();
                String audit_date = jsonAuditLine.getString("modified_at").trim();

                // CONSULTA EN LA BASE DE DATOS CON EL TEMPLATE_ID, SI EXISTE EL REGISTRO ENTONCES
                // HAGO LA LLAMADA EL SERVICIO DEL DETALLE DE LA AUDITORIA
                OBCriteria<SPEVConfigTemplate> templateQuery = OBDal.getInstance()
                    .createCriteria(SPEVConfigTemplate.class);
                templateQuery.add(Restrictions.eq(SPEVConfigTemplate.PROPERTY_VALUE, template_id));
                templateQuery.add(Restrictions.eq(SPEVConfigTemplate.PROPERTY_ACTIVE, true));
                templateQuery.addOrderBy(SPEVConfigTemplate.PROPERTY_CREATIONDATE, false);
                templateQuery.setMaxResults(1);

                if (templateQuery.list() != null && templateQuery.list().size() > 0) {

                  String configTemplateID = templateQuery.list().get(0).getId();
                  String configTemplateType = templateQuery.list().get(0).getType();

                  JSONObject result = new JSONObject();

                  result.put("audit_id", audit_id);
                  result.put("template_id_ob", configTemplateID);
                  result.put("template_type", configTemplateType);
                  result.put("template_id", template_id);
                  result.put("audit_date", audit_date);
                  result.put("start_date", fechas[0]);
                  result.put("end_date", fechas[2]);

                  // LLAMO AL SERVICIO DEL DETALLE DE LA AUDITORIA
                  JSONObject auditData = getDataAudit(result, logger);
                  resultLogger.put("error", "0");
                  resultLogger.put("message", auditData.getString("message"));

                }

              }

            }

          }

          conn.disconnect();

        }

      } catch (MalformedURLException e) {
        logger.logln("Error: MalformedURLException Consulta PLantillas Fechas " + e);
        e.printStackTrace();
      } catch (IOException e) {
        logger.logln("Error: IOException Consulta PLantillas Fechas " + e);
        e.printStackTrace();
      }

    } else {

      logger.logln("No Hay configuracion disponible para el bono checklist");

      resultLogger.put("error", "1");
      resultLogger.put("message", "No Hay configuracion disponible para el bono checklist");

    }

    return resultLogger;

  }

  private static JSONObject getDataAudit(JSONObject jsonsent, ProcessLogger logger)
      throws JSONException, ParseException {

    JSONObject resultAudit = new JSONObject();

    try {

      String audit_id = jsonsent.getString("audit_id");
      String template_id_ob = jsonsent.getString("template_id_ob");
      String template_type = jsonsent.getString("template_type");

      String start_date = jsonsent.getString("start_date");
      String end_date = jsonsent.getString("end_date");

      SSLContext sc = null;
      try {
        sc = SSLContext.getInstance("TLSv1.2");
        sc.init(null, null, new java.security.SecureRandom());
      } catch (NoSuchAlgorithmException e) {
        e.printStackTrace();
      } // $NON-NLS-1$
      catch (KeyManagementException e) {
        e.printStackTrace();
      }

      URL url = new URL(urlWS + audit_id);
      HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
      conn.setSSLSocketFactory(sc.getSocketFactory());
      conn.setRequestMethod("GET");
      conn.setRequestProperty("Authorization", token);

      if (conn.getResponseCode() != 200) {

        InputStream is = conn.getErrorStream();
        BufferedReader rd = new BufferedReader(new InputStreamReader(is));
        String response;
        while ((response = rd.readLine()) != null) {

          logger.logln("No se pudo obtener la informacion de la Auditoria " + audit_id);
          resultAudit.put("error", "1");
          resultAudit.put("message",
              "No se pudo obtener la informacion de la Auditoria " + audit_id);
        }

      } else {

        BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

        String output;
        //org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);

        while ((output = br.readLine()) != null) {

          JSONObject data = new JSONObject(output);

          JSONObject audits = data.getJSONObject("audit_data");
          String[] orgName = audits.getString("name").split("/");

          String srtCreateDate = data.getString("created_at").toString();
          String strYear = srtCreateDate.substring(0, 4);
          String strMonth = srtCreateDate.substring(6, 7);

          Object[] dates = startEndDate();
          String strYeardates = dates[0].toString().substring(0, 4);
          String strMonthdates = dates[0].toString().substring(6, 7);


          if (strYear.equals(strYeardates) && strMonth.equals(strMonthdates)) {

            if (indexInBound(orgName, 2) && orgName[2] != null) {

              String organizationValue = orgName[2].trim();

              // CONSULTA EN LA BASE DE DATOS CON EL TEMPLATE_ID, SI EXISTE EL REGISTRO ENTONCES
              // HAGO LA LLAMADA EL SERVICIO DEL DETALLE DE LA AUDITORIA
              OBCriteria<Organization> orgQuery = OBDal.getInstance()
                  .createCriteria(Organization.class);
              orgQuery.add(Restrictions.eq(Organization.PROPERTY_SEARCHKEY, organizationValue));
              orgQuery.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
              orgQuery.setMaxResults(1);

              if (orgQuery.list() != null && orgQuery.list().size() > 0) {

                Organization org = OBDal.getInstance().get(Organization.class,
                    orgQuery.list().get(0).getId().trim());

                SPEVConfigTemplate configTemplate = OBDal.getInstance()
                    .get(SPEVConfigTemplate.class, template_id_ob);

                BigDecimal percentage = new BigDecimal(
                    audits.getString("score_percentage").replaceAll(",", ""));

                // INSERTAR EN TABLA TEMPORAL

                saveTempData(org, org.getClient(), org.getCreatedBy(), configTemplate,
                    template_type, percentage, formatDate(start_date), formatDate(end_date),
                    logger);

                // InsertAuditEmp(cp, org.getId(), configTemplate.getId().toString(),
                // String.valueOf(percentage), start_date, end_date, template_type, "N");

              }

            }

          }

          resultAudit.put("error", "0");
          resultAudit.put("message", "Proceso Ejecutado Exitosamente");

        }

      }

      conn.disconnect();

    } catch (MalformedURLException e) {
      logger.logln("Error: MalformedURLException Consulta PLantilla Especifica " + e);
      e.printStackTrace();

    } catch (IOException e) {
      logger.logln("Error: IOException Consulta PLantilla Especifica " + e);
      e.printStackTrace();

    }

    return resultAudit;
  }

  private static void saveTempData(Organization ad_org_id_temp, Client ad_client_id_temp,
      User created_by, SPEVConfigTemplate spev_config_template_id, String type,
      BigDecimal percentage, Date start_date, Date end_date, ProcessLogger logger) {

    UUID uuid = UUID.randomUUID();
    String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

    SPEVTempAuditor temp = OBProvider.getInstance().get(SPEVTempAuditor.class);
    temp.setNewOBObject(true);
    temp.setId(randomUUIDString);
    temp.setClient(ad_client_id_temp);
    temp.setOrganization(ad_org_id_temp);
    temp.setSpevConfigTemplate(spev_config_template_id);
    temp.setPercentage(percentage);
    temp.setAuditDate(null);
    temp.setStartDate(start_date);
    temp.setENDDate(end_date);
    temp.setType(type);
    temp.setCreatedBy(created_by);
    temp.setUpdatedBy(created_by);
    OBDal.getInstance().save(temp);
    OBDal.getInstance().flush();

  }

  private static boolean indexInBound(String[] data, int index) {
    return data != null && index >= 0 && index < data.length;
  }

  private static Date formatDate(String fecha) throws ParseException {

    String strDateFormat = "yyyy-MM-dd";
    DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
    Date date = dateFormat.parse(fecha);

    return date;

  }

  private static String[] startEndDate() {

    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.MONTH, -1);
    Date data = cal.getTime();

    Calendar start = Calendar.getInstance();
    Calendar end = Calendar.getInstance();
    Calendar end2 = Calendar.getInstance();

    start.setTime(data);
    start.set(Calendar.DAY_OF_MONTH, start.getActualMinimum(Calendar.DAY_OF_MONTH));
    start.set(Calendar.HOUR_OF_DAY, 0);
    start.set(Calendar.MINUTE, 0);
    start.set(Calendar.SECOND, 0);

    end.setTime(data);
    end.set(java.util.Calendar.DAY_OF_MONTH, end.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
    end.add(java.util.Calendar.MONTH, +1);
    end.add(java.util.Calendar.DATE, +1);
    end.set(java.util.Calendar.HOUR_OF_DAY, 23);
    end.set(java.util.Calendar.MINUTE, 59);
    end.set(java.util.Calendar.SECOND, 59);

    end2.setTime(data);
    end2.set(Calendar.DAY_OF_MONTH, end2.getActualMaximum(Calendar.DAY_OF_MONTH));
    end2.add(Calendar.DATE, +1);
    end2.set(Calendar.HOUR_OF_DAY, 23);
    end2.set(Calendar.MINUTE, 59);
    end2.set(Calendar.SECOND, 59);

    String strDateFormat = "yyyy-MM-dd";
    DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
    String formattedDateStart = dateFormat.format(start.getTime());
    String formattedDateEnd = dateFormat.format(end.getTime());
    String formattedDateEnd2 = dateFormat.format(end2.getTime());

    String[] ret = new String[3];
    ret[0] = formattedDateStart;
    ret[1] = formattedDateEnd;
    ret[2] = formattedDateEnd2;

    return ret;
  }

  private static void bonusChecklist() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;

    try {

      strSql = "SELECT spev_checklistbonus() FROM DUAL;";

      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      st.executeQuery();

    } catch (Exception e) {
      e.printStackTrace();

    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  private static Integer verifyDuplicity() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = "";
    Integer count = 0;

    try {

      String strSql = "SELECT  COUNT(ad_org_id) AS contador \n" + "FROM spev_temp_auditor \n"
          + "WHERE to_char(start_date,'yyyy-MM-dd') = to_char((current_date - interval '1 month'), 'yyyy-MM-01')\n"
          + "AND to_char(end_date,'yyyy-MM-dd') = to_char(date_trunc('month',(current_date - interval '1 month'))+'1month'::interval-'0day'::interval,'yyyy-MM-dd')\n"
          + "AND processed = 'Y';";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("contador");
      }

      count = Integer.parseInt(strResult);

      return count;

    } catch (Exception e) {
      throw new OBException("Error al consultar canton de la Organizacion. " + e.getMessage());
    }

  }

  public static void InsertAuditEmp(ConnectionProvider connectionProvider, String strOrg,
      String strSpevConfigTemplateId, String strpercentage, String strstartDate, String strendDate,
      String strType, String strProcessed) {
    String strSql = "";
    strSql = strSql
        + "insert into spev_temp_auditor(spev_temp_auditor_id, ad_client_id, ad_org_id, isactive, createdby, created, updatedby, updated, spev_config_template_id, percentage, audit_date, start_date, end_date, type, processed) values("
        + "get_uuid(),'" + OBContext.getOBContext().getCurrentClient().getId() + "','" + strOrg
        + "','Y','" + OBContext.getOBContext().getUser().getId() + "',now(),'"
        + OBContext.getOBContext().getUser().getId() + "',now(),'" + strSpevConfigTemplateId + "',"
        + strpercentage + ",'1999-01-01','" + strstartDate + "','" + strendDate + "','" + strType
        + "','" + strProcessed + "')";

    System.out.println(strSql);
    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      updateCount = st.executeUpdate();
      st.close();
      // e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
  }

}
