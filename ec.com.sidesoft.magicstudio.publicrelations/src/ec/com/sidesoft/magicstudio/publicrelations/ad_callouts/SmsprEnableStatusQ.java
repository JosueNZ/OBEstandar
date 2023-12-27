package ec.com.sidesoft.magicstudio.publicrelations.ad_callouts;

import javax.servlet.ServletException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.Timestamp;

import ec.com.sidesoft.magicstudio.customization.SmscQuoteStatus;

public class SmsprEnableStatusQ extends SimpleCallout{

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		
		String inpemSmsprQuotestatusId = info.getStringParameter("inpemSmsprQuotestatusId");
		String inpemSmsprFollowDate = info.getStringParameter("inpemSmsprFollowDate");

		SmscQuoteStatus quoteStatus = OBDal.getInstance().get(SmscQuoteStatus.class, inpemSmsprQuotestatusId);
	    
	    Date date = new Date();
	    Timestamp now = new Timestamp(date.getTime());
	    String alert = "";
		
		if(inpemSmsprQuotestatusId.length() > 0) {
			alert = quoteStatus.getAlertStatus();
		} else if (alert == "DO") {
			info.addResult(inpemSmsprFollowDate, now);
		} else {
			info.addResult(inpemSmsprFollowDate, null);
		}	
	}
}