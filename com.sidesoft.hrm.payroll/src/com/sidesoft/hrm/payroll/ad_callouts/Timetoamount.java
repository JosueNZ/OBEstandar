package com.sidesoft.hrm.payroll.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class Timetoamount extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 12L;

  @SuppressWarnings("null")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    BigDecimal houramt = info.getBigDecimalParameter("inphoursamt");
    info.addResult("inpamount", houramt.divide(new BigDecimal(24), 2, RoundingMode.HALF_UP));
  }

}
