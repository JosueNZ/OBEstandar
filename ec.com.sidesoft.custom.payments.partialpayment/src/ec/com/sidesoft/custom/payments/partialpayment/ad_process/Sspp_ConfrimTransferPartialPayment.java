package ec.com.sidesoft.custom.payments.partialpayment.ad_process;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.custom.payments.partialpayment.SSPPPAYMENTS;
import ec.com.sidesoft.custom.payments.partialpayment.SSPPPAYMENTSLINE;

public class Sspp_ConfrimTransferPartialPayment extends DalBaseProcess {
  OBError message;

  private ConnectionProvider conn = null;

  private String language = "";
  private String strMessage = "";

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    try {
      message = new OBError();
      processRequest(bundle);
    } catch (Exception e) {
      language = OBContext.getOBContext().getLanguage().getLanguage();
      conn = new DalConnectionProvider(false);

      message.setMessage(e.getMessage());
      message.setTitle(Utility.messageBD(conn, "Error", language));
      message.setType("Error");
    } finally {
      bundle.setResult(message);
    }
  }

  private void processRequest(ProcessBundle bundle) {
    String partialPaymentsID = (String) bundle.getParams().get("Sspp_Payments_ID");

    SSPPPAYMENTS updatePayment = OBDal.getInstance().get(SSPPPAYMENTS.class, partialPaymentsID);

    if (updatePayment.getAlertStatus().equals("AP") || updatePayment.getAlertStatus().equals("SE")) {

      OBCriteria<SSPPPAYMENTSLINE> paymentsLine = OBDal.getInstance().createCriteria(
          SSPPPAYMENTSLINE.class);
      paymentsLine.add(Restrictions.eq(SSPPPAYMENTSLINE.PROPERTY_SSPPPAYMENTS, updatePayment));

      int countPayments = paymentsLine.count();
      if (countPayments > 0) {

        if (updatePayment.getConfirmTransfer().equals("CT")) {

          for (SSPPPAYMENTSLINE collpaymentLine : paymentsLine.list()) {

            if (collpaymentLine.isPaidOut()) {
              SSPPPAYMENTSLINE updatePaymentsLine = OBDal.getInstance().get(SSPPPAYMENTSLINE.class,
                  collpaymentLine.getId().toString());

              updatePaymentsLine.setConfirmTransfer("RT");

              OBDal.getInstance().save(updatePaymentsLine);
              OBDal.getInstance().flush();

            }
          }

          updatePayment.setConfirmTransfer("RT");
          updatePayment.setAlertStatus("SE");
          updatePayment.setConfirm("RT");

          OBDal.getInstance().save(updatePayment);
          OBDal.getInstance().flush();

          strMessage = "Transferencia Confirmada";

        } else if (updatePayment.getConfirmTransfer().equals("RT")) {

          for (SSPPPAYMENTSLINE collpaymentLine : paymentsLine.list()) {

            if (collpaymentLine.isPaidOut()) {
              SSPPPAYMENTSLINE updatePaymentsLine = OBDal.getInstance().get(SSPPPAYMENTSLINE.class,
                  collpaymentLine.getId().toString());

              updatePaymentsLine.setConfirmTransfer("CT");

              OBDal.getInstance().save(updatePaymentsLine);
              OBDal.getInstance().flush();

            }
          }

          updatePayment.setConfirmTransfer("CT");
          updatePayment.setAlertStatus("SE");
          updatePayment.setConfirm("CT");
          OBDal.getInstance().save(updatePayment);
          OBDal.getInstance().flush();

          strMessage = "Transferencia Reactivada";
        }

      }
    }

    message.setMessage(Utility.messageBD(conn, strMessage, language));
    message.setType("Success");
    message.setTitle(Utility.messageBD(conn, "ProcessOK", language));

  }
}
