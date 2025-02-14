package ec.com.sidesoft.payroll.events.background;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

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
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.payroll.events.SPEVConfigInventory;
import ec.com.sidesoft.payroll.events.SPEVTempInventory;

public class PayrollEventsMissingInventory extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(PayrollEventsMissingInventory.class);
  private ProcessLogger logger;
  String msgTitle = "";
  String msgMessage = "";
  String msgType = ""; // success, warning or error
  public ConfigParameters cf;

  private static String urlWS = "";

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    cf = bundle.getConfig(); // Obtener la configuración de la App OB
    logger = bundle.getLogger();
    OBError result = new OBError();
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);
    try {
      OBContext.setAdminMode(false);
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));

      String message;

      // CONSUMO EL SERVICIO DE FLATANTE DE INVENTARIOS
      JSONObject consumews = consumeWSInventory();

      message = String.format(consumews.getString("message"), consumews.getString("error"));
      logger.logln(message);

      // HAGO LA LLAMADA A LA FUNCION POSTGRES
      if (consumews.getString("error").equals("0")) {
        missingInventory();

        OBCriteria<SPEVConfigInventory> config = OBDal.getInstance()
            .createCriteria(SPEVConfigInventory.class);
        config.add(Restrictions.eq(SPEVConfigInventory.PROPERTY_ACTIVE, true));
        config.addOrderBy(SPEVConfigInventory.PROPERTY_CREATIONDATE, false);
        config.setMaxResults(1);

        if (config.list() != null && config.list().size() > 0) {
          String id = config.list().get(0).getId().trim();

          // ACTUALIZO LA FECHA DE PROCESAMIENTO EN LA CONFIGURACION DEL FALTANTE INVENTARIO
          SPEVConfigInventory temp = OBDal.getInstance().get(SPEVConfigInventory.class, id);
          temp.setLastDateProcessed(formatDateSave());
          OBDal.getInstance().save(temp);
          OBDal.getInstance().flush();

        }
      }

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

  private static JSONObject consumeWSInventory() throws JSONException, ParseException {

    JSONObject result = new JSONObject();

    // VERIFICO QUE EXISTEN REGISTROS EN LA CONFIGURACION DE INVENTARIO FALTANTE
    OBCriteria<SPEVConfigInventory> config = OBDal.getInstance()
        .createCriteria(SPEVConfigInventory.class);
    config.add(Restrictions.eq(SPEVConfigInventory.PROPERTY_ACTIVE, true));
    config.addOrderBy(SPEVConfigInventory.PROPERTY_CREATIONDATE, false);
    config.setMaxResults(1);

    if (config.list() != null && config.list().size() > 0) {

      try {

        urlWS = config.list().get(0).getUrlbase().trim();

        // VERIFICO QUE NO EXISTAN REGISTROS EN LA TABLA TEMPORAL PARA EL PERIODO EN CURSO
        Integer verify = verifyDuplicity();
        String fecha = formatDateString();

        if (verify > 0) {
          result.put("error", "1");
          result.put("message", "Ya existen registros en la tabla temporal para la fecha " + fecha);
        } else {

          URL url = new URL(urlWS + "missing_inventory?fecha=" + fecha);
          HttpURLConnection conexion = (HttpURLConnection) url.openConnection();
          conexion.setRequestMethod("GET");

          if (conexion.getResponseCode() != 200) {

            InputStream is = conexion.getErrorStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            String response;
            while ((response = rd.readLine()) != null) {
              JSONObject error = new JSONObject(response);
              result.put("error", "1");
              result.put("message", error.getString("message"));
            }

          } else {

            BufferedReader br = new BufferedReader(
                new InputStreamReader((conexion.getInputStream())));

            String output;
            while ((output = br.readLine()) != null) {

              JSONObject res = new JSONObject(output);
              JSONObject data = res.getJSONObject("data");

              // ITERO SOBRE LAS PROPIEDADES DEL OBJETO JSON
              for (int i = 0; i < data.names().length(); i++) {

                String orgValue = data.names().getString(i).trim();

                // VERIFICO QUE LA ORGANIZACION EXISTA EN OPENBRAVO
                OBCriteria<Organization> orgQuery = OBDal.getInstance()
                    .createCriteria(Organization.class);
                orgQuery.add(Restrictions.eq(Organization.PROPERTY_SEARCHKEY, orgValue));
                orgQuery.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
                orgQuery.setMaxResults(1);

                if (orgQuery.list() != null && orgQuery.list().size() > 0) {

                  JSONArray infoOrg = data.getJSONArray(data.names().getString(i));

                    // ITERO SOBRE EL ARRAY DE LOS EMPLEADOS DE LA ORGANIZACION
                    for (int j = 0; j < infoOrg.length(); j++) {

                      JSONObject empInfo = infoOrg.getJSONObject(j);
                      String emp_ci = empInfo.getString("emp_ci").trim();

                      // VERIFICAR QUE EL EMPLEADO EXISTA Y QUE SEA DE TIPO LOCAL
                      OBCriteria<BusinessPartner> employee = OBDal.getInstance()
                          .createCriteria(BusinessPartner.class);
                      employee.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, emp_ci));
                      employee.add(Restrictions.eq(BusinessPartner.PROPERTY_EMPLOYEE, true));
                      employee.add(Restrictions.eq(BusinessPartner.PROPERTY_SPEVLOCAL, true));
                      employee.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
                      employee.setMaxResults(1);
                      
                      if (employee.list() != null && employee.list().size() > 0) {

                        Organization org = OBDal.getInstance().get(Organization.class,
                            orgQuery.list().get(0).getId().trim());

                        BusinessPartner emp = OBDal.getInstance().get(BusinessPartner.class,
                            employee.list().get(0).getId().trim());
                        
                        saveDataTempInventory(org, org.getClient(), org.getCreatedBy(), emp,
                            org.getSpevCCostcenter(), formatDate(fecha));

                      }

                    }                    

                }

              }
            }

            result.put("error", "0");
            result.put("message", "Proceso Ejecutado Exitosamente");

          }

          conexion.disconnect();

        }

      } catch (MalformedURLException e) {
        e.printStackTrace();

      } catch (IOException e) {
        e.printStackTrace();
      }

    } else {

      result.put("error", "1");
      result.put("message", "No Hay configuracion disponible para el faltante de inventario");

    }
    return result;

  }

  private static void saveDataTempInventory(Organization ad_org_id, Client ad_client_id,
      User created_by, BusinessPartner c_bpartner_id, Costcenter c_costcenter_id,
      Date processed_date) {

    UUID uuid = UUID.randomUUID();
    String randomUUIDString = uuid.toString().replaceAll("-", "").toUpperCase();

    SPEVTempInventory temp = OBProvider.getInstance().get(SPEVTempInventory.class);
    temp.setNewOBObject(true);
    temp.setId(randomUUIDString);
    temp.setClient(ad_client_id);
    temp.setOrganization(ad_org_id);
    temp.setBusinessPartner(c_bpartner_id);
    temp.setCostCenter(c_costcenter_id);
    temp.setProcessedDate(processed_date);
    temp.setProcessed(false);
    temp.setCreatedBy(created_by);
    temp.setUpdatedBy(created_by);
    OBDal.getInstance().save(temp);
    OBDal.getInstance().flush();

  }

  private static String formatDateString() throws ParseException {

    Date today = new Date();

    Calendar calendar = Calendar.getInstance();
    calendar.setTime(today);
    calendar.add(Calendar.DATE, -1);
    Date day = calendar.getTime();
    DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    String fecha = sdf.format(day);

    return fecha;

  }

  private static Date formatDate(String fecha) throws ParseException {

    String strDateFormat = "yyyy-MM-dd";
    DateFormat dateFormat = new SimpleDateFormat(strDateFormat);
    Date date = dateFormat.parse(fecha);

    return date;

  }

  private static Date formatDateSave() throws ParseException {

    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    String dateString = format.format(new Date());
    Date date = format.parse(dateString);

    return date;

  }

  private static void missingInventory() {

    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;

    try {

      strSql = "SELECT spev_missinginventory() FROM DUAL;";

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

      String strSql = "  SELECT  COUNT(spev_temp_inventory_id) as contador \n"
          + "  FROM spev_temp_inventory \n"
          + "  WHERE to_char(processed_date,'yyyy-MM-dd') = to_char((current_date - interval '1 day'), 'yyyy-MM-DD')\n"
          + "  AND processed = 'Y' ;";
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

}
