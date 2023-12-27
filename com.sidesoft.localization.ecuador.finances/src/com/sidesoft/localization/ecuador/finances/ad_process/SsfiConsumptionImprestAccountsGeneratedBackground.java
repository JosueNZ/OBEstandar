package com.sidesoft.localization.ecuador.finances.ad_process;

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

/**
 * @author Charlie :D
 * 
 */
public class SsfiConsumptionImprestAccountsGeneratedBackground extends DalBaseProcess {
  private static final Logger log4j = Logger
      .getLogger(SsfiConsumptionImprestAccountsGeneratedBackground.class);
  private ProcessLogger logger;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    OBError result = new OBError();
    try {
      OBContext.setAdminMode(false);
      result.setType("Success");
      result.setTitle(OBMessageUtils.messageBD("Success"));

      ConnectionProvider conn = bundle.getConnection();

      // Lamo al Dataset que contiene los pagos/cobros con problemas en el campo 'use credir'

      SsfiConsumptionImprestAccountsGeneratedBackgroundData DataConsumptionAcct[] = SsfiConsumptionImprestAccountsGeneratedBackgroundData
          .select(conn, String.valueOf("1"));

      for (SsfiConsumptionImprestAccountsGeneratedBackgroundData lstConsumptionAcct : DataConsumptionAcct) {
        try {
          /* SFBBudgetVersion version = OBDal.getInstance().get(SFBBudgetVersion.class, versionId); */
          FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class,
              lstConsumptionAcct.finPaymentId);
          payment.setUsedCredit(new BigDecimal(lstConsumptionAcct.amount));
          OBDal.getInstance().save(payment);
          OBDal.getInstance().flush();
        } catch (Exception e) {
          result = OBMessageUtils.translateError(bundle.getConnection(), bundle.getContext()
              .toVars(), OBContext.getOBContext().getLanguage().getLanguage(), e.getMessage());
          log4j.error(result.getMessage(), e);
          logger.logln(result.getMessage());
          bundle.setResult(result);
          return;
        }

        // If cost has been calculated successfully do a commit.
        OBDal.getInstance().getConnection().commit();
      }
      OBDal.getInstance().getSession().clear();

      logger.logln(OBMessageUtils.messageBD("Success"));
      bundle.setResult(result);
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      result = OBMessageUtils.translateError(bundle.getConnection(), bundle.getContext().toVars(),
          OBContext.getOBContext().getLanguage().getLanguage(), e.getMessage());
      log4j.error(result.getMessage(), e);
      logger.logln(result.getMessage());
      bundle.setResult(result);
      return;
    } finally {
      OBContext.restorePreviousMode();
    }
  }
}
