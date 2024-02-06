package ec.cusoft.facturaec.background;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.SchedulerContext;

import ec.cusoft.facturaec.ad_process.GenerateFE;
import ec.cusoft.facturaec.ad_process.webservices.util.ClientSOAP;
import ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcionPortBindingStub;
import ec.cusoft.facturaec.filewriter.FileGeneration;

public class EEIOfflineBatchBackground extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(EEIOfflineBatchBackground.class);
  private ProcessLogger logger;
  private int maxTransactions = 0;
  FileGeneration filegeneration = new FileGeneration();
  String msgTitle = "";
  String msgMessage = "";
  String msgType = ""; // success, warning or error
  public ConfigParameters cf;
  private SchedulerContext ctx;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    cf = bundle.getConfig(); // Obtener la configuración de la App OB
    logger = bundle.getLogger();
    OBError result = new OBError();
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    String strSessionUserId = OBContext.getOBContext().getUser().getId();
    try {

      OBContext.setAdminMode(false);
      result.setType("Error");
      result.setTitle(OBMessageUtils.messageBD("Error"));

      ArrayList<String> lstInvoice = SelectInvoices(bundle);
      ArrayList<String> strResult = new ArrayList<String>();
      ArrayList<String> strTipoResult = new ArrayList<String>();
      GenerateFE clsGenerate = new GenerateFE();
      int intContador = 0;

      if (lstInvoice != null && lstInvoice.size() != 0) {
        log4j.debug("INICIANDO PROCESO DE ENVÍO DE DOCUMENTOS ELECTRÓNICOS EN LISTA.");

        String strURLWSOffline = ClientSOAP.SelectParams(1);
        String strCorreoPorDefecto = ClientSOAP.SelectParams(2);
        String strGenerarClaveAcceso = ClientSOAP.SelectParams(3);
        int intTimeOutPara = WSRecepcionPortBindingStub.SelectTimeOut();

        for (int i = 0; i < lstInvoice.size(); i++) {
          log4j.debug("NÚMERO DE PROCESAMIENTO: " + i + 1);
          try {
            Invoice invoice = OBDal.getInstance().get(Invoice.class, lstInvoice.get(i).toString());
            Boolean boolButtonStatus = invoice.isEeiTemporalsend();

            // ******SE COMPRUEBA SI EL BOTÓN FUE PRESIONADO
            if (!boolButtonStatus) {
              clsGenerate.processRequest(invoice.getId(), strURLWSOffline, strCorreoPorDefecto,
                  strGenerarClaveAcceso,strSessionUserId,intTimeOutPara);
            } else {
              log4j.debug("BOTÓN YA PRESIONADO.");
            }

          } catch (Exception e) {
            log4j.debug("Error en documento. " + e.getMessage());
          }

        }

      } else {
        log4j.debug("No existen registros para la ejecución del proceso en cola.");
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

  public static ArrayList<String> SelectInvoices(ProcessBundle bundle) {
    ConnectionProvider conn = new DalConnectionProvider(false);

    String strTypeOfBatch = null;
    String strOrgList = null;
    String strDateLimit = "null";
    try {
      strTypeOfBatch = SelectParams();
      log4j.debug("Tipo de proceso en lote: " + strTypeOfBatch);
      if (strTypeOfBatch.length() == 0 || strTypeOfBatch == null || strTypeOfBatch.equals("")) {
        throw new OBException(
            "No se encontró parametrización de tipo de procesamiento en lote en documentos electrónicos.");
      }
    } catch (Exception e) {
      throw new OBException("Error al obtener la parametrización de Facturación Electrónica. "
          + e.getMessage());
    }

    try {

      StringBuffer where = new StringBuffer();
      where.append(" as o");
      where.append(" where ");
      where.append(" ad_isorgincluded(o.id, '" + bundle.getContext().getOrganization() + "', '"
          + bundle.getContext().getClient() + "') <> -1 ");
      OBQuery<Organization> orgQry = OBDal.getInstance().createQuery(Organization.class,
          where.toString());
      List<Organization> orgs = orgQry.list();
      if (orgs.size() == 0) {
        throw new OBException("Error al obtener la parametrización de Facturación Electrónica. ");
      }
      int intContador = 0;
      for (Organization org : orgs) {
        strOrgList = (strOrgList == null ? "" : strOrgList) + "'"
            + (org.getId() == null ? "" : org.getId()) + "'";
        intContador = intContador + 1;
        if (intContador != orgs.size()) {
          strOrgList = strOrgList + ",";
        }
      }

      Date DateProcess = null;
      String StrADProcessRequestID = bundle.getProcessRequestId();
      ProcessRequest ObjADProcessRequest = OBDal.getInstance().get(ProcessRequest.class,
          StrADProcessRequestID);

      if (ObjADProcessRequest.getSbkpDateprosses() != null) {
        DateProcess = ObjADProcessRequest.getSbkpDateprosses();
        SimpleDateFormat ecFormat = new SimpleDateFormat("dd/MM/yyyy");
        strDateLimit = "'" + ecFormat.format(DateProcess) + "'";
      }

    } catch (Exception e) {
      throw new OBException("Error al obtener lista de organizaciones de envío. " + e.getMessage());
    }

    try {
      String strSql = null;
      if (strTypeOfBatch.equals("AD")) {
        strSql = "SELECT CI.dateinvoiced,CI.EM_EEI_STATUS,CI.issotrx,CI.documentno,c_invoice_id \r\n"
            + "FROM C_INVOICE CI INNER JOIN C_DOCTYPE CD ON CI.C_DOCTYPE_ID = CD.C_DOCTYPE_ID\r\n"
            + " LEFT JOIN C_DOCTYPE DT ON CI.EM_SSWH_C_DOCTYPE_ID = DT.C_DOCTYPE_ID\r\n"
            + " WHERE CI.DOCSTATUS='CO' AND (CI.EM_EEI_STATUS ='NA' OR CI.EM_EEI_STATUS ='NR' OR CI.EM_EEI_STATUS ='NG' OR CI.EM_EEI_STATUS IS NULL) AND ((CD.EM_EEI_IS_EDOC ='Y' AND CI.ISSOTRX='Y')\r\n"
            + " OR(CD.EM_EEI_IS_EDOC ='Y' AND CI.EM_EEI_STATUS_2='AU' AND DT.EM_EEI_IS_EDOC ='Y' AND CI.ISSOTRX='N') OR (CD.EM_EEI_IS_EDOC ='N' AND DT.EM_EEI_IS_EDOC ='Y' AND CI.ISSOTRX='N')) \r\n"
            + " AND CI.ad_org_id IN ("
            + strOrgList
            + ") \r\n"
            + " AND (CI.dateinvoiced <= TO_DATE("
            + strDateLimit
            + ") OR "
            + strDateLimit
            + " IS NULL) \r\n" + " ORDER BY CI.dateinvoiced ASC";
      } else {

        strSql = "SELECT CI.c_invoice_id FROM C_INVOICE CI \r\n"
            + " INNER JOIN C_DOCTYPE CD ON CI.C_DOCTYPE_ID = CD.C_DOCTYPE_ID\r\n"
            + " LEFT JOIN C_DOCTYPE DT ON CI.EM_SSWH_C_DOCTYPE_ID = DT.C_DOCTYPE_ID\r\n"            
            + " WHERE CI.DOCSTATUS='CO' AND CI.EM_EEI_STATUS IS NULL AND ((CD.EM_EEI_IS_EDOC ='Y' AND CI.ISSOTRX='Y')\r\n"
            + " OR(CD.EM_EEI_IS_EDOC ='Y' AND CI.EM_EEI_STATUS_2='AU' AND DT.EM_EEI_IS_EDOC ='Y' AND CI.ISSOTRX='N') OR (CD.EM_EEI_IS_EDOC ='N' AND DT.EM_EEI_IS_EDOC ='Y' AND CI.ISSOTRX='N')) \r\n"
            + " AND CI.ad_org_id IN (" + strOrgList + ") \r\n"
            + " AND (CI.dateinvoiced <= TO_DATE(" + strDateLimit + ") OR " + strDateLimit
            + " IS NULL) \r\n" + " ORDER BY CI.dateinvoiced ASC";
      }

      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      ArrayList<String> strResult = new ArrayList<String>();
      strResult.clear();
      int contador = 0;
      while (rsConsulta.next()) {

        strResult.add(rsConsulta.getString("c_invoice_id"));
        contador = contador + 1;
      }
      log4j.debug("Número de transacciones a procesar. " + contador);
      return strResult;

    } catch (Exception e) {
      log4j.debug("Error al consultar la tabla c_invoice " + e);
      return null;
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

  public static String SelectParams() {
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {
      String strSql = "SELECT type_of_batch  FROM eei_param_facturae where isactive='Y' and type_of_batch is not null";
      PreparedStatement st = null;
      String strParametro = null;
      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      int contador = 0;
      while (rsConsulta.next()) {
        contador = contador + 1;
        strParametro = rsConsulta.getString("type_of_batch");

      }
      if (contador == 0) {
        throw new OBException(
            "No se encontró parametrización de tipo de procesamiento en lote en documentos electrónicos.");
      } else if (contador > 1) {

        throw new OBException(
            "Existe más de una parametrización activa de documentos electrónicos.");
      }
      return strParametro;
    } catch (Exception e) {

      throw new OBException("Error al consultar la tabla eei_param_facturae (Tipo de Lote WS) " + e);
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }
}
