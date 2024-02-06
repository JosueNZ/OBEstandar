/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.sidesoft.hrm.payroll.ad_process;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
import com.sidesoft.hrm.payroll.ConceptAmount;

/**
 * 
 * @author Hernan Orozco
 */
public class ConceptAmountHourExcel extends IdlServiceJava {

  @Override
  public String getEntityName() {
    return "Simple Products";
  }

  @Override
  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("concept value", Parameter.STRING),
        new Parameter("anio", Parameter.STRING), new Parameter("periodo", Parameter.LONG),
        new Parameter("codigo del empleado value", Parameter.STRING),
        new Parameter("horas", Parameter.STRING), new Parameter("dias", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
    boolean correct = false;
    validator.checkString(values[0], 60);
    validator.checkBigDecimal(values[1]);
    validator.checkBigDecimal(values[2]);
    validator.checkString(values[3], 40);
    if (values[4] != null && !"".equals(values[4].trim())) {
      validator.checkBigDecimal(values[4]);
      correct = !correct;
    }
    if (values[5] != null && !"".equals(values[5].trim())) {
      validator.checkBigDecimal(values[5]);
      correct = !correct;
    }
    if(!correct){
        throw new Exception("Se debe ingresar un solo valor horas o días");
    }
    return values;
  }

  @Override
  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createConceptAmount((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5]);
  }

  public BaseOBObject createConceptAmount(final String concept, final String anio,
      final String periodo, final String codEmployee, final String hours, final String days)
      throws Exception {

    // Concept Amount
    ConceptAmount conceptAmount = OBProvider.getInstance().get(ConceptAmount.class);
    try {
      conceptAmount.setActive(true);
      conceptAmount.setOrganization(rowTransactionalOrg);
      conceptAmount
          .setSsprConcept(findDALInstance(true, Concept.class, new Value("value", concept)));
      if (!findDALInstance(true, Year.class, new Value("fiscalYear", anio)).equals(null)) {
        if (!findDALInstance(true, Period.class, new Value("periodNo", Long.parseLong(periodo)))
            .getOpenClose().equals("N")) {
          Year yearobj = findDALInstance(false, Year.class, new Value("fiscalYear", anio));
          conceptAmount.setPeriod(findDALInstance(false, Period.class,
              new Value("periodNo", Long.parseLong(periodo)), new Value("year", yearobj)));
        } else {
          throw new Exception();
        }
      } else {
        throw new Exception();
      }

      conceptAmount.setBusinessPartner(
          findDALInstance(true, BusinessPartner.class, new Value("searchKey", codEmployee)));

      if (days != null && !days.equals("")) {
        conceptAmount.setAmount(Parameter.BIGDECIMAL.parse(days));
      }
      if (hours != null && !hours.equals("")) {
        BigDecimal convertdays = (new BigDecimal(hours).divide(new BigDecimal("24"), 2, RoundingMode.HALF_UP));
        conceptAmount.setAmount(Parameter.BIGDECIMAL.parse(convertdays.toString()));
        conceptAmount.setHoursamt(Parameter.BIGDECIMAL.parse(hours));
      }

      OBDal.getInstance().save(conceptAmount);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
    OBDal.getInstance().commitAndClose();
    return conceptAmount;
  }

}
