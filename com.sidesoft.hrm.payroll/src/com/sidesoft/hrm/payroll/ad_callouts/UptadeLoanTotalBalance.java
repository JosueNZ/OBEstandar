package com.sidesoft.hrm.payroll.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

public class UptadeLoanTotalBalance extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 12L;

  @SuppressWarnings("null")
@Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub

      BigDecimal LoanAdvance = info.getBigDecimalParameter("inploanAdvance");
      BigDecimal Amounloan = info.getBigDecimalParameter("inpamount");
      BigDecimal LessLoanAmount = null;
      
      if ( Integer.valueOf(LoanAdvance.intValue()) > 0 && (Integer.valueOf(LoanAdvance.intValue()) <= Integer.valueOf(Amounloan.intValue())))
      {
    	  LessLoanAmount =  Amounloan.subtract(LoanAdvance);
    	  info.addResult("inptotalBalance",   String.valueOf(LessLoanAmount));
      }
      else if (Integer.valueOf(LoanAdvance.intValue()) > Integer.valueOf(Amounloan.intValue()))
      {
    	  info.addResult("inptotalBalance",   String.valueOf(0));
    	  info.addResult("inploanAdvance",   String.valueOf(0));
      }
  }

}
