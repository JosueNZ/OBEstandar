package ec.com.sidesoft.actuaria.special.customization.ad_process;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.actuaria.special.customization.Scactu_Log;
import ec.com.sidesoft.actuaria.special.customization.utils.Scactu_Helper;
import ec.com.sidesoft.actuaria.special.customization.webservices.Scactu_BusinessPartner;
import ec.com.sidesoft.actuaria.special.customization.webservices.Scactu_PaymentIn;
import ec.com.sidesoft.actuaria.special.customization.webservices.Scactu_SaleOrder;

public class Scactu_ProcessLog extends DalBaseProcess {
	private final Logger logger = Logger.getLogger(Scactu_ProcessLog.class);

	@Override
	public void doExecute(ProcessBundle bundle) throws Exception {
		OBError msg = new OBError();
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin ProcessLog");

			String logId = (String) bundle.getParams().get("Scactu_Log_ID");
			Scactu_Log log = OBDal.getInstance().get(Scactu_Log.class, logId);

			JSONObject body = new JSONObject(log.getJsonRequest());
			logger.info(body.toString());

			switch (log.getEndpoint()) {
			case "createBusinessPartner":
				new Scactu_BusinessPartner().createBusinessPartner(conn, body);
				log.setReferenceNo(body.getString("taxId"));
				break;
			case "updateBusinessPartner":
				new Scactu_BusinessPartner().updateBusinessPartner(conn, body);
				log.setReferenceNo(body.getString("taxId"));
				break;
			case "createSaleOrder":
				new Scactu_SaleOrder().createSaleOrder(conn, body);
				log.setReferenceNo(body.getString("documentNo"));
				break;
			case "updateSaleOrder":
				new Scactu_SaleOrder().updateSaleOrder(conn, body);
				log.setReferenceNo(body.getString("documentNo"));
				break;
			case "createPaymentIn":
				new Scactu_PaymentIn().createPaymentIn(conn, body);
				log.setReferenceNo(body.getString("documentNo"));
				break;
			}

			JSONObject json = Scactu_Helper.getResponse(body);
			log.setJsonResponse(json.toString());
			log.setRecordID(body.getString("id"));
			log.setResult("OK");
			log.setError(null);

			OBDal.getInstance().save(log);
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();

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
			logger.info("Finish ProcessLog");
		}
	}

}
