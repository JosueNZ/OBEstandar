package ec.com.sidesoft.actuaria.special.customization.actionhandler;

import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.ReportDefinition;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.client.application.report.BaseReportActionHandler;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StickerReportActionHandler extends BaseReportActionHandler {

	private static final Logger log = LoggerFactory.getLogger(StickerReportActionHandler.class);
	private static final String REPORT_FILE_EXT = ExportType.PDF.getExtension();

	@Override
	protected void addAdditionalParameters(ReportDefinition process, JSONObject jsonContent,
			Map<String, Object> parameters) {
		try {
			String orderId = (String) jsonContent.get("C_Order_ID");

			parameters.put("ORDER_ID", orderId);
			parameters.put("LANGUAGE", OBContext.getOBContext().getLanguage().getLanguage());
		} catch (Exception e) {
			log.debug(e.getMessage());
		}
		super.addAdditionalParameters(process, jsonContent, parameters);
	}

	@Override
	protected JSONObject doExecute(Map<String, Object> parameters, String content) {

		try {
			parameters.replace("reportId", "64D75AA66B96433ABB57F358E4C80856");

			final JSONObject jsonContent = new JSONObject(content);
			jsonContent.put(ApplicationConstants.BUTTON_VALUE, REPORT_FILE_EXT);

			return super.doExecute(parameters, jsonContent.toString());
		} catch (Exception e) {
			log.error("Error generating report id: {}", parameters.get("reportId"), e);
			return getResponseBuilder().retryExecution().showResultsInProcessView()
					.showMsgInProcessView(MessageType.ERROR,
							OBMessageUtils.translateError(e.getMessage()).getMessage())
					.build();
		}
	}
}
