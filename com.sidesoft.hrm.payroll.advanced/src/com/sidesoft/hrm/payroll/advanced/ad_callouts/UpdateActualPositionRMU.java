package com.sidesoft.hrm.payroll.advanced.ad_callouts;


import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;

import com.sidesoft.hrm.payroll.Contract;

public class UpdateActualPositionRMU extends SimpleCallout {

	  /**
	   * 
	   */
private static final long serialVersionUID = 12L;

	@SuppressWarnings("null")
	@Override
	protected void execute(CalloutInfo info) throws ServletException {
	    // TODO Auto-generated method stub
		
		  String strContractID = info.getStringParameter("inpssprContractId", null);
		  Contract CtrObj = OBDal.getInstance().get(Contract.class, strContractID);
		  
		  if (CtrObj.getPermanentremuneration() != null)
		  {
			  String Partner_RMU = CtrObj.getPermanentremuneration().toString();
			  
			  info.addResult("inpactualRmu", Partner_RMU);
		  }

		  if (CtrObj.getBusinessPartner() != null){
			  
			  String  StrContract = CtrObj.getBusinessPartner().getId().toString();
			  BusinessPartner BpObj = OBDal.getInstance().get(BusinessPartner.class,StrContract );
			  
			  String StrCostCenterId = BpObj.getSsprCostcenter().getId().toString();
			  info.addResult("inpcCostcenterId", StrCostCenterId);
		  }
		  
  }

}
