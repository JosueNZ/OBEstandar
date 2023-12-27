package com.sidesoft.hrm.payroll.biometrical.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import com.sidesoft.hrm.payroll.Concept;
import com.sidesoft.hrm.payroll.Contract;
import com.sidesoft.hrm.payroll.Shift;
import com.sidesoft.hrm.payroll.biometrical.SprbiDays;
import com.sidesoft.hrm.payroll.advanced.SfprEvolutionSalary;
import ec.com.sidesoft.holidays.sshdholidaysperiodline;
import ec.com.sidesoft.holidays.sshdholidaysstandards;
import ec.com.sidesoft.payroll.events.SPEVConfigNews;

import org.openbravo.model.common.businesspartner.BusinessPartner;

public class SprbiEmployee extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String identify = info.getStringParameter("inpidentify", null);

    try {
      if (!identify.isEmpty()) {
        OBCriteria<BusinessPartner> employee = OBDal.getInstance()
            .createCriteria(BusinessPartner.class);
        employee.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, identify));
        if (employee.list().size() > 0) {
          info.addResult("inpcBpartnerId", employee.list().get(0).getId());
          OBCriteria<Contract> contract = OBDal.getInstance().createCriteria(Contract.class);
          contract.add(Restrictions.eq(Contract.PROPERTY_BUSINESSPARTNER, employee.list().get(0)));
          if (contract.list().size() > 0) {
            Shift shift = contract.list().get(0).getSsprShift();
            info.addResult("inpentry1", shift.getStarttime());
            info.addResult("inpexit1", shift.getEndtime());
          }
        }
      }
    } catch (Exception e) {
    }
  }

}
