package com.sidesoft.hrm.payroll.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;

public class OtherTaxIncomeEmployee extends SimpleCallout {

    @Override
    protected void execute(CalloutInfo info) throws ServletException {

        String inpcBpartnerId = info.getStringParameter("inpcBpartnerId", null);

        BusinessPartner bPartner = OBDal.getInstance().get(BusinessPartner.class, inpcBpartnerId);

        info.addResult("inptaxid", bPartner.getTaxID());
    }

}
