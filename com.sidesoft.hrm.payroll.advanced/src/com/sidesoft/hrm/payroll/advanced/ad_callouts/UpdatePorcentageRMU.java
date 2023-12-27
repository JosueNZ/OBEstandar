package com.sidesoft.hrm.payroll.advanced.ad_callouts;

import java.math.BigDecimal;

import javassist.expr.Cast;

import javax.servlet.ServletException;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import com.sidesoft.hrm.payroll.advanced.SfprRve;

public class UpdatePorcentageRMU extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 12L;

  @SuppressWarnings("null")
@Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
	  BigDecimal porcHandimax = info.getBigDecimalParameter("inpporcHandimax");
	  BigDecimal porcPanamax = info.getBigDecimalParameter("inpporcPanamax");
      BigDecimal porcAfromax = info.getBigDecimalParameter("inpporcAframax");
	  BigDecimal RVEAF = info.getBigDecimalParameter("inprmu");
	  
	  //total = (porcHandimax.multiply(RVEAF));
	  if (porcHandimax != null || porcHandimax.equals(0) ){
	  BigDecimal TotalH = RVEAF.multiply(porcHandimax).divide( BigDecimal.valueOf(100));
	  
	  info.addResult("inprveHandimax",   TotalH.toString());
	  }
	  if (porcPanamax != null || porcPanamax.equals(0) ){
	  BigDecimal TotalP = RVEAF.multiply(porcPanamax).divide( BigDecimal.valueOf(100));
	  
	  info.addResult("inprvePanamax",   TotalP.toString());
	  }
	  if (porcAfromax != null || porcAfromax.equals(0) ){
	  BigDecimal TotalA = RVEAF.multiply(porcAfromax).divide( BigDecimal.valueOf(100));
	  
	  info.addResult("inprveAframax",   TotalA.toString());
	  }
	  
  }

}
