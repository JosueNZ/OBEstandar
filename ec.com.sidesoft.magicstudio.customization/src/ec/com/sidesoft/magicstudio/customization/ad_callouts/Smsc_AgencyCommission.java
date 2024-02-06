package ec.com.sidesoft.magicstudio.customization.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import ec.com.sidesoft.magicstudio.customization.SmscAgencyFee;

public class Smsc_AgencyCommission extends SimpleCallout {

	@Override
	protected void execute(CalloutInfo info) throws ServletException {
		// TODO Auto-generated method stub
		String inpSmscInitvalue = info.getStringParameter("inpemSmscInitvalue");
		String inpSmscAgencyfee =  info.vars.getStringParameter("inpemSmscAgencyfeeId");
		//String inpSmscAgencyfee = info.getStringParameter("inpemSmscAgencyfeeId");
		
		SmscAgencyFee agencycommissions = OBDal.getInstance().get(SmscAgencyFee.class, inpSmscAgencyfee);
        BigDecimal agencyCommissionValue = new BigDecimal(agencycommissions.getAgencyCommission());

		BigDecimal promotionPrice = new BigDecimal(0);
        BigDecimal currentPrice = new BigDecimal(0);

		
		if (inpSmscInitvalue != null && !inpSmscInitvalue.isEmpty() && inpSmscAgencyfee != null && !inpSmscAgencyfee.isEmpty()) {
			
			BigDecimal initvalue = new BigDecimal(inpSmscInitvalue);
			//initvalue = initvalue.compareTo(BigDecimal.ZERO) == 0 ? initvalue : initvalue.divide(new BigDecimal(100));
		
		promotionPrice = new BigDecimal(inpSmscInitvalue);
		//initvalue.multiply(agencyCommissionValue.divide(new BigDecimal(100)));		
            //promotionPrice = agencyCommissionValue.multiply(initvalue);
            
            if(promotionPrice.compareTo(BigDecimal.ZERO) > 0)
            {
            	 BigDecimal sumResult = promotionPrice.add(new BigDecimal(inpSmscInitvalue));

                 // Convierte el resultado a String y actualiza el campo inppriceactual
                 info.addResult("inppriceactual", sumResult.toString());

            }
		}
		info.addResult("inpemSmscAgencycom", promotionPrice);
	}

}
