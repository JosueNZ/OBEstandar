package com.sidesoft.localization.ecuador.refunds.ad_callouts;

import java.util.List;

import javax.servlet.ServletException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;

public class UpdateTaxidEmployeeRefund extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 12L;

@Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
	  String StrCbpartnerID = info.getStringParameter("inpcBpartnerId",null);
      
	  BusinessPartner ObjBpartner = OBDal.getInstance().get(BusinessPartner.class, StrCbpartnerID);

	  if (!ObjBpartner.getTaxID().equals("") || !ObjBpartner.getTaxID().equals(null))
	  {
		  String StrTaxidRUC = ObjBpartner.getTaxID().toString();
		  
		  String StrTaxidType = ObjBpartner.getSswhTaxidtype().toString();
		  
		  info.addResult("inptaxidruc", StrTaxidRUC);
		  
		  info.addResult("inptaxidtype", StrTaxidType);
	  }

  }

}
