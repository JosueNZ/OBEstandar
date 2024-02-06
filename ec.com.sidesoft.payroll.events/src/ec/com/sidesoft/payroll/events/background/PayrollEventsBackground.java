package ec.com.sidesoft.payroll.events.background;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.apache.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.cusoft.facturaec.filewriter.FileGeneration;

public class PayrollEventsBackground extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(PayrollEventsBackground.class);
  private ProcessLogger logger;
  FileGeneration filegeneration = new FileGeneration();
  String msgTitle = "";
  String msgMessage = "";
  String msgType = ""; // success, warning or error
  public ConfigParameters cf;

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
      System.out.println("ENTRA A LA CLASE A HACER LA LLAMADA DE LAS FUNCIONES");
      // ********************************************************************//
      // PROCESOS DIARIOS
      // ********************************************************************//

      // PROCESO FALTANTES DE CAJA Y MULTAS FALTANTES DE CAJA
      cashMissing();
      // PROCESO PEDIDOS MOTORIZADOS
      motorizedOrder();
      // PROCESO FACTURA CLIENTE
      processedInvoice();

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

  public static void cashMissing() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;

    try {

      strSql = "SELECT spev_cashmissing() FROM DUAL;";

      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      System.out.println("SE EJECUTO LA FUNCION DE FALTANTES DE CAJA. ");
      // return strResult;

    } catch (Exception e) {
      System.out.println("HUBO ERRORES EN LA EJECUCION DE FALTANTES DE CAJA " + e);
      // return null;
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static void motorizedOrder() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;

    try {

      strSql = "SELECT spev_motorizedorder() FROM DUAL;";

      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      System.out.println("SE EJECUTO LA FUNCION DE PEDIDO MOTORIZADO. ");

    } catch (Exception e) {
      System.out.println("HUBO ERRORES EN LA EJECUCION DE PEDIDO MOTORIZADO " + e);
      // return null;
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static void processedInvoice() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;

    try {

      strSql = "SELECT spev_processed_invoice() FROM DUAL;";

      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      System.out.println("SE EJECUTO LA FUNCION DE FALTANTES DE CAJA. ");
      // return strResult;

    } catch (Exception e) {
      System.out.println("HUBO ERRORES EN LA EJECUCION DE FALTANTES DE CAJA " + e);
      // return null;
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

}
