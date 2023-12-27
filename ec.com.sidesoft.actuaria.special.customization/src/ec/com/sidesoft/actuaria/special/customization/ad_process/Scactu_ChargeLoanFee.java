package ec.com.sidesoft.actuaria.special.customization.ad_process;

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.hrm.payroll.ConceptAmount;
import com.sidesoft.hrm.payroll.ssprpayrollaut;

import ec.com.sidesoft.actuaria.special.customization.Scactu_ConfigActuapar;
import ec.com.sidesoft.actuaria.special.customization.Scactu_Log;
import ec.com.sidesoft.actuaria.special.customization.utils.Scactu_Helper;

public class Scactu_ChargeLoanFee extends DalBaseProcess {
	private final Logger logger = Logger.getLogger(Scactu_ChargeLoanFee.class);

	@Override
	public void doExecute(ProcessBundle bundle) throws Exception {
		OBError msg = new OBError();
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin Scactu_ChargeLoanFee");

			String id = (String) bundle.getParams().get("Sspr_Payroll_Aut_ID");
			ssprpayrollaut payroll = OBDal.getInstance().get(ssprpayrollaut.class, id);
			process(payroll);

			msg.setType("Success");
			msg.setTitle(OBMessageUtils.messageBD("Success"));
		} catch (final Exception e) {
			OBDal.getInstance().rollbackAndClose();
			String message = Scactu_Helper.getErrorMessage(logger, e);
			logger.error(message);

			msg.setTitle(OBMessageUtils.messageBD("Error"));
			msg.setType("Error");
			msg.setMessage(message);
		} finally {
			try {
				conn.getConnection().close();
				conn.destroy();
			} catch (Exception e) {
				String message = Scactu_Helper.getErrorMessage(logger, e);
				logger.error(message);

				msg.setTitle(OBMessageUtils.messageBD("Error"));
				msg.setType("Error");
				msg.setMessage(message);
			}

			bundle.setResult(msg);
			OBContext.restorePreviousMode();
			logger.info("Finish Scactu_ChargeLoanFee");
		}
	}

	private void process(ssprpayrollaut payroll) throws Exception {
		OBCriteria<Scactu_ConfigActuapar> qConfig = OBDal.getInstance()
				.createCriteria(Scactu_ConfigActuapar.class);
		qConfig.add(Restrictions.eq(Scactu_ConfigActuapar.PROPERTY_ACTIVE, true));
		qConfig.addOrder(
				org.hibernate.criterion.Order.desc(Scactu_ConfigActuapar.PROPERTY_ORGANIZATION));
		qConfig.setMaxResults(1);
		if (qConfig.list().size() == 0) {
			throw new OBException("Configuration not found");
		}
		Scactu_ConfigActuapar config = qConfig.list().get(0);

		OBCriteria<BusinessPartner> qBPartner = OBDal.getInstance()
				.createCriteria(BusinessPartner.class);
		qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
		qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_EMPLOYEE, true));
		qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SSPRSTATUS, "A"));

		Period period = payroll.getPeriod();

		OBCriteria<Scactu_Log> qLog = OBDal.getInstance().createCriteria(Scactu_Log.class);
		qLog.add(Restrictions.eq(Scactu_Log.PROPERTY_RECORDID, payroll.getId()));

		for (Scactu_Log log : qLog.list()) {
			OBDal.getInstance().remove(log);
		}
		OBDal.getInstance().flush();
		OBDal.getInstance().getConnection().commit();

		OBCriteria<ConceptAmount> qConceptAmount = OBDal.getInstance()
				.createCriteria(ConceptAmount.class);
		qConceptAmount.add(Restrictions.eq(ConceptAmount.PROPERTY_ACTIVE, true));
		qConceptAmount.add(Restrictions.eq(ConceptAmount.PROPERTY_SCACTUISACTUAPAR, true));
		qConceptAmount.add(Restrictions.eq(ConceptAmount.PROPERTY_PERIOD, period));
		qConceptAmount
				.add(Restrictions.eq(ConceptAmount.PROPERTY_SSPRCONCEPT, config.getSsprConcept()));

		for (ConceptAmount conceptAmount : qConceptAmount.list()) {
			OBDal.getInstance().remove(conceptAmount);
		}
		OBDal.getInstance().flush();
		OBDal.getInstance().getConnection().commit();

		for (BusinessPartner bPartner : qBPartner.list()) {
			String logId = null;
			try {
				String token = Scactu_Helper.postActuaparToken(config.getEndpoint(),
						config.getUser(), config.getPass());
				if (token.isEmpty()) {
					throw new OBException("The token could not be obtained");
				}

				JSONObject body = new JSONObject();
				body.put("cedula", bPartner.getTaxID());
				body.put("desde", Scactu_Helper.getDateString(period.getStartingDate()));
				body.put("hasta", Scactu_Helper.getDateString(period.getEndingDate()));

				Scactu_Log log = OBProvider.getInstance().get(Scactu_Log.class);
				log.setInterface("A");
				log.setEndpoint("getEmployeeFee");
				log.setJsonRequest(body.toString());
				log.setRecordID(payroll.getId());
				log.setReferenceNo(payroll.getDocumentNo());
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				OBDal.getInstance().getConnection().commit();

				OBDal.getInstance().refresh(log);
				logId = log.getId();

				String endpoint = config.getEndpoint() + "aportadores/getCuotaPersona";
				String response = Scactu_Helper.post(endpoint, body, token);
				log.setJsonResponse(response);
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				OBDal.getInstance().getConnection().commit();

				if (!response.equals("")) {
					JSONArray array = new JSONArray(response);
					JSONObject json = array.getJSONObject(0);
					ConceptAmount conceptAmount = OBProvider.getInstance().get(ConceptAmount.class);
					conceptAmount.setScactuIsActuapar(true);
					conceptAmount.setSsprConcept(config.getSsprConcept());
					conceptAmount.setPeriod(payroll.getPeriod());
					conceptAmount.setBusinessPartner(bPartner);
					BigDecimal amount = new BigDecimal(json.getString("alicuota"));
					conceptAmount.setAmount(amount);
					OBDal.getInstance().save(conceptAmount);
					OBDal.getInstance().flush();
					OBDal.getInstance().getConnection().commit();
				}

				log.setResult("OK");
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				OBDal.getInstance().getConnection().commit();
			} catch (Exception e) {
				OBDal.getInstance().getConnection().rollback();
				String message = Scactu_Helper.getErrorMessage(logger, e);
				logger.error(message);
				if (logId != null) {
					Scactu_Log log = OBDal.getInstance().get(Scactu_Log.class, logId);
					log.setResult("ERROR");
					log.setError(message);
					OBDal.getInstance().save(log);
					OBDal.getInstance().flush();
					OBDal.getInstance().getConnection().commit();
				}
			}
		}
	}
}
