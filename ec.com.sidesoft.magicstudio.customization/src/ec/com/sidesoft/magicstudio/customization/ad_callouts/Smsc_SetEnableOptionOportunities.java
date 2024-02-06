package ec.com.sidesoft.magicstudio.customization.ad_callouts;

import javax.servlet.ServletException;

import ec.com.sidesoft.magicstudio.customization.SmscOrigin;
import ec.com.sidesoft.magicstudio.customization.SmscOpportunities;

import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.scheduling.ProcessBundle;

public class Smsc_SetEnableOptionOportunities extends SimpleCallout{

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		// info->vars->httpRequest->parameters
		
		String inpsmscOpportunitiesId = info.getStringParameter("inpsmscOpportunitiesId");
		
		String inpsmscOriginId = info.getStringParameter("inpsmscOriginId");
		String inpenableOption = info.getStringParameter("inpenableOption");

		SmscOrigin origin = OBDal.getInstance().get(SmscOrigin.class, inpsmscOriginId);
		//SmscOpportunities opportunity = OBDal.getInstance().get(SmscOpportunities.class, inpsmscOpportunitiesId);
		
	    int tmp = 1;
	    
		if (origin.isEnableOption()){
			//opportunity.setEnableOption(true);
			info.addResult(inpenableOption, true);
		}else{
			info.addResult(inpenableOption, false);
		}
		
	}
}
