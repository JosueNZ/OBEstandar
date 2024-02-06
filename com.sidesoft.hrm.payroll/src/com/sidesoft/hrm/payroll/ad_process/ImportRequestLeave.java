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
import java.util.Calendar;
import java.util.Date;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.geography.City;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import com.sidesoft.hrm.payroll.ssprleavecategory;
import com.sidesoft.hrm.payroll.ssprleaveemp;
import com.sidesoft.hrm.payroll.ssprleavetype;

/**
 * 
 * @author Dieguito
 */
public class ImportRequestLeave extends IdlServiceJava {

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("tipo documento", Parameter.STRING),
        new Parameter("numero", Parameter.STRING), new Parameter("identificador", Parameter.STRING),
        new Parameter("tipo permiso", Parameter.STRING),
        new Parameter("categoria", Parameter.STRING), new Parameter("fecha", Parameter.STRING),
        new Parameter("inicio", Parameter.STRING), new Parameter("fin", Parameter.STRING),
        new Parameter("ciudad", Parameter.STRING), new Parameter("tipo vacacion", Parameter.STRING),
        new Parameter("estado", Parameter.STRING) };
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 32);
    validator.checkString(values[1], 32);
    validator.checkString(values[2], 32);
    validator.checkString(values[3], 32);
    validator.checkString(values[4], 32);
    validator.checkDate(values[5]);
    validator.checkDate(values[6]);
    validator.checkDate(values[7]);
    validator.checkString(values[8], 32);
    validator.checkString(values[9], 32);
    validator.checkString(values[10], 32);
    return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createleave((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6],
        (String) values[7], (String) values[8], (String) values[9], (String) values[10]);
  }

  public BaseOBObject createleave(final String doctype, final String documentno, final String taxid,
      final String typeleave, final String category, final String date, final String startdate,
      final String enddate, final String city, final String typevacation, final String state)
      throws Exception {

    BusinessPartner partner = findDALInstance(false, BusinessPartner.class,
        new Value(BusinessPartner.PROPERTY_TAXID, taxid));
    if (partner == null || partner.equals("")) {
      throw new OBException("Empleado con CI: " + taxid + "no existe");
    }
    DocumentType doctypeid = findDALInstance(false, DocumentType.class,
        new Value(DocumentType.PROPERTY_NAME, doctype));
    if (doctypeid == null || doctypeid.equals("")) {
      throw new OBException("Campo tipo de documento es incorrecto empleado: " + taxid);
    }
    ssprleavetype leavetype = findDALInstance(false, ssprleavetype.class,
        new Value(ssprleavetype.PROPERTY_SEARCHKEY, typeleave));
    if (leavetype == null || leavetype.equals("")) {
      throw new OBException("Campo tipo de permiso es incorrecto empleado: " + taxid);
    }
    ssprleavecategory leavecategory = findDALInstance(false, ssprleavecategory.class,
        new Value(ssprleavecategory.PROPERTY_VALUE, category));
    if (leavecategory == null || leavecategory.equals("")) {
      throw new OBException("Campo categoria de permiso es incorrecto empleado: " + taxid);
    }
    City cityid = findDALInstance(false, City.class, new Value(City.PROPERTY_NAME, city));
    if (cityid == null || cityid.equals("")) {
      throw new OBException("Campo ciudad es incorrecto empleado: " + taxid);
    }
    ssprleaveemp leavevalidation = findDALInstance(false, ssprleaveemp.class,
        new Value("businessPartner", partner),
        new Value("stardateabsent", Parameter.DATE.parse(date)),
        new Value("stardate", Parameter.DATE.parse(startdate)),
        new Value("endingDate", Parameter.DATE.parse(enddate)));
    if (leavevalidation != null) {
      throw new OBException(
          "Ya existe un registro para el mismo idenficador para el empleado: " + taxid);
    }

    // Contract contract = findDALInstance(false, Contract.class,
    // new Value("businessPartner", partner), new Value("active", true));

    ssprleaveemp leaveemp = OBProvider.getInstance().get(ssprleaveemp.class);

    try {
      long nodays = 1;
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
      Date sdate = formatter.parse(startdate);
      Date edate = formatter.parse(enddate);
      Calendar DateStartcompare = Calendar.getInstance();
      Calendar DateEndcompare = Calendar.getInstance();
      DateStartcompare.setTime(sdate);
      DateEndcompare.setTime(edate);

      if (DateEndcompare.compareTo(DateStartcompare) > 0) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(
            DateEndcompare.getTime().getTime() - DateStartcompare.getTime().getTime());
        nodays = c.get(Calendar.DAY_OF_YEAR) + 1;
      } else {
        if (DateEndcompare.equals(DateStartcompare)) {
          nodays = 1;
        } else {
          nodays = 0;
        }
      }

      leaveemp.setDocumentType(doctypeid);
      leaveemp.setDocumentNo(documentno);
      leaveemp.setBusinessPartner(partner);
      leaveemp.setLeaveType(leavetype);
      leaveemp.setLeaveCategory(leavecategory);
      leaveemp.setStardateabsent(Parameter.DATE.parse(date));
      leaveemp.setStardate(Parameter.DATE.parse(startdate));
      leaveemp.setEndingDate(Parameter.DATE.parse(enddate));
      leaveemp.setNoDays(nodays);
      leaveemp.setAddToVacancies(true);
      leaveemp.setCity(cityid);
      leaveemp.setTypevacations(typevacation);
      leaveemp.setStatusRequest(state);
      OBDal.getInstance().save(leaveemp);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }

    OBDal.getInstance().commitAndClose();
    return leaveemp;
  }
}
