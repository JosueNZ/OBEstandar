package ec.com.sidesoft.backgroundprocess.ad_callouts;

import java.math.BigDecimal;
import java.util.List;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.ProcessTrl;

public class Sbkp_UpdateImportantInformation extends SimpleCallout {

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		// info->vars->httpRequest->parameters
		String inpProcessId = info.getStringParameter("inpadProcessId");
		if(inpProcessId != null && !inpProcessId.isEmpty()) {
			Process process = OBDal.getInstance().get(Process.class, inpProcessId);
			List<ProcessTrl> processTrlList = process.getADProcessTrlList();
			if(processTrlList.size() > 0) {
				info.addResult("inpemSbkpImportantInfo", processTrlList.get(0).getHelpComment());				
			}

		}
	}
}
