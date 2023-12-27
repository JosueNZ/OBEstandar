/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.sidesoft.hrm.payroll.ad_process;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import com.sidesoft.hrm.payroll.Concept;
import com.sidesoft.hrm.payroll.ConceptAmount;;

/**
 * 
 * @author Hernan Orozco
 */
public class ConceptAmountExcel extends IdlServiceJava {

  @Override
  public String getEntityName() {
    return "Simple Products";
  }

  @Override
  public Parameter[] getParameters() {
    return new Parameter[] { 
    		new Parameter("concept value", Parameter.STRING),
        	new Parameter("anio", Parameter.STRING), 
        	new Parameter("periodo", Parameter.LONG),
        	new Parameter("codigo del empleado value", Parameter.STRING),
        	new Parameter("monto", Parameter.STRING)};
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
    validator.checkString(values[0], 60);
    validator.checkBigDecimal(values[1]);
    validator.checkBigDecimal(values[2]);
    validator.checkString(values[3], 40);
    validator.checkBigDecimal(values[4]);
    return values;
  }

  @Override
  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createConceptAmount((String) values[0], (String) values[1], (String) values[2],
        (String) values[3],(String) values[4]);
  }

  public BaseOBObject createConceptAmount(final String concept, final String anio, 
		  final String periodo, final String codEmployee, final String monto) throws Exception {

    // Concept Amount
    ConceptAmount conceptAmount = OBProvider.getInstance().get(ConceptAmount.class);
    try {
      conceptAmount.setActive(true);
      conceptAmount.setOrganization(rowTransactionalOrg);
      conceptAmount.setSsprConcept(findDALInstance(true, Concept.class, new Value("value", concept)));
      if (!findDALInstance(true, Year.class, new Value("fiscalYear", anio)).equals(null)){ 
    	  if(!findDALInstance(true, Period.class, new Value("periodNo", Long.parseLong(periodo))).getOpenClose().equals("N")){
		   Year yearobj = findDALInstance(false, Year.class, new Value("fiscalYear",anio));
                   conceptAmount.setPeriod(findDALInstance(false, Period.class,
        	   new Value("periodNo", Long.parseLong(periodo)), new Value("year",yearobj)));
    	  }else {
    		  throw new Exception();
    	  }
      }else {
    	  throw new Exception();
      }

      conceptAmount.setBusinessPartner(findDALInstance(true, BusinessPartner.class, 
    		  new Value("searchKey", codEmployee)));
      conceptAmount.setAmount(Parameter.BIGDECIMAL.parse(monto));

      OBDal.getInstance().save(conceptAmount);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
    OBDal.getInstance().commitAndClose();
    return conceptAmount;
  }

}
