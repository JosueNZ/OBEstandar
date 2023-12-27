package ec.com.sidesoft.localization.ecuador.withholdingssales.ad_process;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;

import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.domain.List;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.client.kernel.RequestContext;

import ec.com.sidesoft.localization.ecuador.withholdingssales.SSWSConfig;
import ec.com.sidesoft.localization.ecuador.withholdingssales.SSWSWithholdingSale;
import ec.com.sidesoft.localization.ecuador.withholdingssales.SswsAdvancePayment;

import org.openbravo.advpaymentmngt.actionHandler.AddPaymentActionHandler;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.client.kernel.RequestContext;
import org.apache.log4j.Logger;

public class ProcessWithholdingSale extends DalBaseProcess {

  OBError message;
  protected ConnectionProvider conn;
  protected VariablesSecureApp vars;
  protected String language;
  private static String newline1 = System.lineSeparator();
  private static Logger logger = Logger.getLogger(ProcessWithholdingSale.class);

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception, IOException {
    VariablesSecureApp vars = bundle.getContext().toVars();
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    this.conn = conn;
    this.vars = vars;
    this.language = language;

    try {
      message = new OBError();
      processRequest(bundle);

    } catch (Exception e) {
      logger.info("exeption" + e);
      message.setMessage(e.getMessage());
      message.setTitle(Utility.messageBD(conn, "Error", language));
      message.setType("Error");
    } finally {
      bundle.setResult(message);
    }
  }

  public void processRequest(ProcessBundle bundle) throws Exception {

    String messageT = "";
    String typeM = "";
    String titleM = "";
    String recordId = (String) bundle.getParams().get("Ssws_Withholdingsale_ID");
    SSWSWithholdingSale record = OBDal.getInstance().get(SSWSWithholdingSale.class, recordId);
    SswsAdvancePayment config = getconfig(record);

    if (record.isGenerateAdvancePayment() && config == null) {
      messageT = OBMessageUtils.parseTranslation("@ssws_config_advance_payment@");
      throw new OBException(messageT);
    }

    if (record.getDocumentStatus().equals("CO") && record.getProcessed().equals("Y")
        && record.getFINPaymentEMSwhpiWithholdingsaleIDList().size() > 0) {
      messageT = OBMessageUtils.parseTranslation("@ssws_exist_advance_payment@");
      throw new OBException(messageT);
    }

    if (record.getGlitem() == null && record.isGenerateAdvancePayment() && config != null
        && config.getGLItem() != null) {
      record.setGlitem(config.getGLItem());
      OBDal.getInstance().save(record);
    }
    messageT = generateWithholdingSale(config, record);

    titleM = "ProcessOK";
    typeM = "Success";
    message.setMessage(messageT);
    message.setTitle(Utility.messageBD(conn, titleM, language));
    message.setType(typeM);
  }

  public SswsAdvancePayment getconfig(SSWSWithholdingSale record) {
    OBCriteria<SswsAdvancePayment> crtconfig = OBDal.getInstance()
        .createCriteria(SswsAdvancePayment.class);
    crtconfig
        .add(Restrictions.eq(SswsAdvancePayment.PROPERTY_ORGANIZATION, record.getOrganization()));
    crtconfig.setMaxResults(1);
    SswsAdvancePayment config = (SswsAdvancePayment) crtconfig.uniqueResult();
    return config;
  }

  public static String generateWithholdingSale(SswsAdvancePayment config,
      SSWSWithholdingSale record) {

    OBContext.setAdminMode(true);
    String message = "";
    String messagePayment = "";
    try {
      // ad_process_id = 99498D75CAE24D839AC788636CD7B457
      // Function = WHSale_Process
      Process process = OBDal.getInstance().get(Process.class, "99498D75CAE24D839AC788636CD7B457");

      ProcessInstance pInstance = CallProcess.getInstance().call(process, record.getId(), null);

      OBError oberror = OBMessageUtils.getProcessInstanceMessage(pInstance);
      message = oberror.getMessage();
      if (record.isGenerateAdvancePayment() && !record.getDocumentStatus().equals("CO")
          && pInstance.getResult() != 0L) {
        messagePayment = generatePayment(config, record);
      }

    } catch (Exception e) {
      // TODO: handle exception
      logger.info("generateWithholdingSale: " + e.getMessage());
    }
    OBContext.setAdminMode(false);
    return message.concat(newline1 + messagePayment);
  }

  public static String generatePayment(SswsAdvancePayment config, SSWSWithholdingSale record) {
    String message = "";
    OBContext.setAdminMode(true);
    try {
      // OBTENEMOS LA SECUENCIA DEL DOCUMENTO DEL COBRO
      String strPaymentDocumentNo = FIN_Utility.getDocumentNo(config.getDocumentType(),
          "FIN_Payment");
      // Action to do
      final String strAction = config.getActionPayment();
      String strDifferenceAction = "credit";
      BigDecimal amount_payment = record.getTotalWhIVAAmt();
      amount_payment = amount_payment.add(record.getTotalWhRentalAmt());
      // GENERAMOS LA CABECERA DEL COBRO
      FIN_Payment payment = (new AdvPaymentMngtDao()).getNewPayment(true, config.getOrganization(),
          config.getDocumentType(), strPaymentDocumentNo, record.getBusinessPartner(),
          config.getPaymentMethod(), config.getDepositTo(), amount_payment.toString(),
          record.getWithholdingDate(), record.getDocumentNo(), record.getCurrency(),
          BigDecimal.ZERO, BigDecimal.ZERO, null);
      payment.setAmount(amount_payment);
      OBDal.getInstance().save(payment);
      // PROCESAMOS EL COBRO
      OBError messagePayment = processPayment(payment, strAction, strDifferenceAction);
      payment.setSwhpiWithretention(true);
      payment.setSwhpiWithholdingsale(record);
      OBDal.getInstance().getConnection(true).commit();
      return messagePayment.getMessage();
    } catch (Exception e) {
      // TODO: handle exception
      logger.info("generatePayment: " + e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
    return message;
  }

  public static OBError processPayment(FIN_Payment payment, String strAction,
      String strDifferenceAction) throws Exception {
    ConnectionProvider conn = new DalConnectionProvider(true);
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();

    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
    BigDecimal assignedAmount = BigDecimal.ZERO;
    for (FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
      assignedAmount = assignedAmount.add(paymentDetail.getAmount());
    }

    if (assignedAmount.compareTo(payment.getAmount()) == -1) {
      FIN_PaymentScheduleDetail refundScheduleDetail = dao.getNewPaymentScheduleDetail(
          payment.getOrganization(), payment.getAmount().subtract(assignedAmount));
      dao.getNewPaymentDetail(payment, refundScheduleDetail,
          payment.getAmount().subtract(assignedAmount), BigDecimal.ZERO, false, null);
    }

    OBError message = FIN_AddPayment.processPayment(vars, conn,
        (strAction.equals("PRP") || strAction.equals("PPP")) ? "P" : "D", payment, null);
    String strNewPaymentMessage = OBMessageUtils
        .parseTranslation("@PaymentCreated@" + " " + payment.getDocumentNo()) + ".";

    if (!"Error".equalsIgnoreCase(message.getType())) {
      message.setMessage(strNewPaymentMessage + " " + message.getMessage());
      message.setType(message.getType().toLowerCase());
    }

    return message;
  }

}
