/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.sidesoft.hrm.payroll.ad_process;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import com.sidesoft.hrm.payroll.Contract;
import com.sidesoft.hrm.payroll.advanced.SfprEvolutionSalary;

/**
 * 
 * @author Dieguito
 */
public class ImportEvolutionSalaryExcell extends IdlServiceJava {

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("searchkey", Parameter.STRING),
        new Parameter("value", Parameter.STRING), new Parameter("startdate", Parameter.STRING),
        new Parameter("enddate", Parameter.STRING), new Parameter("salary", Parameter.STRING) };
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 32);
    validator.checkString(values[1], 32);
    validator.checkDate(values[2]);
    validator.checkDate(values[3]);
    validator.checkBigDecimal(values[4]);
    return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createProduct((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4]);
  }

  public BaseOBObject createProduct(final String searchkey, final String value,
      final String startdate, final String enddate, final String salary) throws Exception {

    BusinessPartner partner = findDALInstance(false, BusinessPartner.class, new Value(
        BusinessPartner.PROPERTY_SEARCHKEY, searchkey));

    String activey = "Y";
    Contract contract = findDALInstance(false, Contract.class,
        new Value("businessPartner", partner), new Value("active", true));

    if (contract != null) {
      OBCriteria<SfprEvolutionSalary> SfprEvolSalary = OBDal.getInstance().createCriteria(
          SfprEvolutionSalary.class);
      SfprEvolSalary.add(Restrictions.eq(SfprEvolutionSalary.PROPERTY_SSPRCONTRACT, contract));

      if (SfprEvolSalary.list().size() > 0) {
        List<SfprEvolutionSalary> lstEvolSalary = SfprEvolSalary.list();
        for (SfprEvolutionSalary collEvolSalary : lstEvolSalary) {

          SfprEvolutionSalary evolutionsalary2 = OBDal.getInstance().get(SfprEvolutionSalary.class,
              collEvolSalary.getId());
          evolutionsalary2.setActive(false);
          evolutionsalary2.setStatusEs("E");
          Date myDate = new Date();
          evolutionsalary2.setEndingDate(Parameter.DATE.parse(new SimpleDateFormat("yyyy-MM-dd")
              .format(myDate)));
          OBDal.getInstance().save(evolutionsalary2);
          OBDal.getInstance().flush();
        }

      }

    }

    SfprEvolutionSalary evolutionsalary = OBProvider.getInstance().get(SfprEvolutionSalary.class);

    try {

      evolutionsalary.setActive(true);
      evolutionsalary.setSearchKey(value);
      evolutionsalary.setStartingDate(Parameter.DATE.parse(startdate));
      evolutionsalary.setEndingDate(Parameter.DATE.parse(enddate));
      evolutionsalary.setAmount(Parameter.BIGDECIMAL.parse(salary));
      evolutionsalary.setSsprContract(contract);
      evolutionsalary.setStatusEs("U");
      OBDal.getInstance().save(evolutionsalary);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      BusinessPartner bpartnerSalary = OBDal.getInstance().get(BusinessPartner.class,
          partner.getId());
      bpartnerSalary.setSsprCurrentsalary(Parameter.BIGDECIMAL.parse(salary));
      OBDal.getInstance().save(bpartnerSalary);
      OBDal.getInstance().flush();
    } catch (Exception e1) {
      e1.printStackTrace();
    }

    OBDal.getInstance().commitAndClose();
    return evolutionsalary;
  }
}
