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

public class PayrollEventsBackgroundMonth extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(PayrollEventsBackgroundMonth.class);
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
      // PROCESOS MENSUALES
      // ********************************************************************//

      // PROCESO BONO VENTAS
      bonusSales();
      // PROCESO BONO ESPECIAL CC
      specialCCbonus();

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

  public static void bonusSales() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;

    try {

      strSql = "SELECT spev_salesbonus() FROM DUAL;";

      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      System.out.println("SE EJECUTO LA FUNCION DE BONO DE VENTA. ");

    } catch (Exception e) {
      System.out.println("HUBO ERRORES EN LA EJECUCION DE BONO DE VENTA " + e);
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static void specialCCbonus() {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;

    try {

      strSql = "SELECT spev_special_bonus_cc() FROM DUAL;";

      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      System.out.println("SE EJECUTO LA FUNCION DE BONO ESPECIAL CC. ");

    } catch (Exception e) {
      System.out.println("HUBO ERRORES EN LA EJECUCION DE BONO ESPECIAL CC " + e);
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

}
