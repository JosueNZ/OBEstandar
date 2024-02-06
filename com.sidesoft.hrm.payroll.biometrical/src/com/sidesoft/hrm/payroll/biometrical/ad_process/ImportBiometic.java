/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.sidesoft.hrm.payroll.biometrical.ad_process;

import java.sql.Timestamp;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import com.sidesoft.hrm.payroll.biometrical.sprbibiometric;

/**
 * 
 * @author Dieguito
 */
public class ImportBiometic extends IdlServiceJava {

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("codigo", Parameter.STRING),
        new Parameter("fecha", Parameter.STRING), new Parameter("entrada1", Parameter.STRING),
        new Parameter("salida1", Parameter.STRING), new Parameter("entrada2", Parameter.STRING),
        new Parameter("salida2", Parameter.STRING) };
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 30);
    validator.checkDate(values[1]);
    validator.checkString(values[2], 20);
    validator.checkString(values[3], 20);
    validator.checkString(values[4], 20);
    validator.checkString(values[5], 20);
    return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createbiometric((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5]);
  }

  public BaseOBObject createbiometric(final String taxid, final String datemovement,
      final String hora1, final String hora2, final String hora3, final String hora4)
      throws Exception {

    BusinessPartner partner = findDALInstance(false, BusinessPartner.class,
        new Value(BusinessPartner.PROPERTY_TAXID, taxid));
    if (partner == null || partner.equals("")) {
      throw new OBException("Empleado:  " + taxid + " no existe");
    }

    sprbibiometric validatebiometric = findDALInstance(false, sprbibiometric.class,
        new Value(sprbibiometric.PROPERTY_BUSINESSPARTNER, partner),
        new Value(sprbibiometric.PROPERTY_DATEMOVEMENT, Parameter.DATE.parse(datemovement)));
    if (validatebiometric != null) {
      throw new OBException("Ya existe un registro con el mismo identificador: " + taxid
          + " en la misma fecha: " + datemovement);
    }

    // Concateno horas y fechas
    String strhora1 = null;
    String strhora2 = null;
    String strhora3 = null;
    String strhora4 = null;

    if (hora1 != null && !hora1.isEmpty()) {
      strhora1 = datemovement + ' ' + hora1 + ":00";
    }
    if (hora2 != null && !hora2.isEmpty()) {
      strhora2 = datemovement + ' ' + hora2 + ":00";
    }
    if (hora3 != null && !hora3.isEmpty()) {
      strhora3 = datemovement + ' ' + hora3 + ":00";
    }
    if (hora4 != null && !hora4.isEmpty()) {
      strhora4 = datemovement + ' ' + hora4 + ":00";
    }

    sprbibiometric biometric = OBProvider.getInstance().get(sprbibiometric.class);

    try {
      biometric.setIdentify(taxid);
      biometric.setDatemovement(Parameter.DATE.parse(datemovement));
      if (strhora1 != null && !strhora1.isEmpty()) {
        biometric.setEntryhourM(Timestamp.valueOf(strhora1));
      }
      if (strhora2 != null && !strhora2.isEmpty()) {
        biometric.setExithourM(Timestamp.valueOf(strhora2));
      }
      if (strhora3 != null && !strhora3.isEmpty()) {
        biometric.setEntryhourA(Timestamp.valueOf(strhora3));
      }
      if (strhora4 != null && !strhora4.isEmpty()) {
        biometric.setExithourA(Timestamp.valueOf(strhora4));
      }
      biometric.setState("DR");
      biometric.setBusinessPartner(partner);

      OBDal.getInstance().save(biometric);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }

    OBDal.getInstance().commitAndClose();
    return biometric;
  }
}
