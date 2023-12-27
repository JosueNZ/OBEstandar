package com.sidesoft.hrm.payroll.biometrical.ad_process;

import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import org.apache.log4j.*;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.idl.proc.Value;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import org.openbravo.model.common.businesspartner.BusinessPartner;
import com.sidesoft.hrm.payroll.biometrical.sprbibiometric;

/**
 *
 * @author RARC
 */
public class ImportBiometricMarking extends IdlServiceJava {

  private static Logger log = Logger.getLogger(ImportBiometricMarking.class);
  int fila = 1;

  @Override
  public String getEntityName() {
    return "Biometric Marking";
  }

  @Override
  public Parameter[] getParameters() {
    fila = 1;
    return new Parameter[] { new Parameter("Identify", Parameter.STRING),
        new Parameter("Datemovement", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {
    validator.checkString(values[0], 32);
    // validator.checkDate(values[1]);
    boolean datef = true;
    datef = isValidDate(values[1].toString(), "yyyy-MM-dd HH:mm") ? true
        : isValidDate(values[1].toString(), "dd/MM/yyyy HH:mm");
    if (datef == false) {
      System.out.println("El formato de fecha no es valido yyyy-MM-dd HH:mm o dd/MM/yyyy HH:mm: "
          + values[1] + " en la Fila " + fila);
      validator.checkNotNull(null, "Fecha");
    }
    fila++;
    return values;
  }

  @Override
  public BaseOBObject internalProcess(Object... values) throws Exception {
    return create((String) values[0], (String) values[1]);
  }

  public BaseOBObject create(final String Identify, final String Datemovement) throws Exception {
    Date date = null;
    Timestamp time = null;
    sprbibiometric marking = null;
    try {
      // Employee
      BusinessPartner employee = findDALInstance(false, BusinessPartner.class,
          new Value(BusinessPartner.PROPERTY_SEARCHKEY, Identify));
      if (employee == null) {
        throw new RuntimeException("SPRBI_EmployeeNotExits");
      }

      // try {
      // date = new SimpleDateFormat().parse(Datemovement);
      // time = new Timestamp(date.getTime());
      // } catch (Exception e) {
      // throw new RuntimeException("SPRBI_DateFormatInvalid");
      // }
      date = isValidDate(Datemovement, "yyyy-MM-dd HH:mm")
          ? parseDate(Datemovement, "yyyy-MM-dd HH:mm")
          : parseDate(Datemovement, "dd/MM/yyyy HH:mm");
      time = new Timestamp(date.getTime());

      // Biometric Marking
      sprbibiometric checkMarking = findDALInstance(false, sprbibiometric.class,
          new Value(sprbibiometric.PROPERTY_IDENTIFY, Identify),
          new Value(sprbibiometric.PROPERTY_DATEMOVEMENT, date),
          new Value(sprbibiometric.PROPERTY_ENTRYHOURM, time));
      if (checkMarking != null) {
        OBDal.getInstance().rollbackAndClose();
        throw new RuntimeException("SPRBI_MarkingExits");
      }
      marking = OBProvider.getInstance().get(sprbibiometric.class);

      marking.setActive(true);
      marking.setDatemovement(date);
      marking.setEntryhourM(time);
      marking.setIdentify(Identify);
      marking.setBusinessPartner(employee);

      OBDal.getInstance().save(marking);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(Utility.messageBD(conn, e.getMessage(), vars.getLanguage()));
    }

    // End process
    // OBDal.getInstance().commitAndClose();

    return marking;
  }

  public boolean isValidDate(String dateStr, String format) {

    DateFormat sdf = new SimpleDateFormat(format);
    sdf.setLenient(false);
    try {
      sdf.parse(dateStr);
    } catch (ParseException e) {
      return false;
    }
    return true;
  }

  public Date parseDate(String dateStr, String format) throws ParseException {

    DateFormat sdf = new SimpleDateFormat(format);
    sdf.setLenient(false);
    try {
      sdf.parse(dateStr);
    } catch (ParseException e) {
      throw new RuntimeException("SPRBI_DateFormatInvalid");
    }
    return sdf.parse(dateStr);
  }
}