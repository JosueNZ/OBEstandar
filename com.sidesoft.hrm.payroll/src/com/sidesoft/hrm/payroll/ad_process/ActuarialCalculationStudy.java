package com.sidesoft.hrm.payroll.ad_process;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import com.sidesoft.hrm.payroll.Sspr_ActuarialCalcStudy;

public class ActuarialCalculationStudy extends IdlServiceJava {
    private final Logger logger = Logger.getLogger(ActuarialCalculationStudy.class);

    @Override
    public String getEntityName() {
        return "Sspr_ActuarialCalcStudy";
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[] {
                new Parameter("Periodo", Parameter.STRING),
                new Parameter("Cedula", Parameter.STRING),
                new Parameter("TS", Parameter.STRING),
                new Parameter("Remuneracion", Parameter.STRING),
                new Parameter("Concepto", Parameter.STRING),
                new Parameter("Obligacion", Parameter.STRING),
                new Parameter("Costo corriente proyectado", Parameter.STRING),
                new Parameter("Interes neto proyectado", Parameter.STRING) };
    }

    @Override
    protected Object[] validateProcess(Validator validator, String... values) throws Exception {
        String message = String.join("|", values);

        String periodo = validator.checkDate(values[0]);
        Date period = getDate(periodo.trim());
        OBCriteria<Period> qPeriod = OBDal.getInstance().createCriteria(Period.class);
        qPeriod.add(Restrictions.eq(Period.PROPERTY_ACTIVE, true));
        qPeriod.add(Restrictions.or(Restrictions.le(Period.PROPERTY_STARTINGDATE, period),
                Restrictions.ge(Period.PROPERTY_ENDINGDATE, period)));
        if (qPeriod.list().size() == 0) {
            throw new OBException(message + "\nPeriodo [" + periodo + "] no encontado");
        }

        String taxId = validator.checkString(values[1], 20);
        OBCriteria<BusinessPartner> qBPartner = OBDal.getInstance().createCriteria(BusinessPartner.class);
        qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
        qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SSPRSTATUS, "A"));
        qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_EMPLOYEE, true));
        qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, taxId));
        if (qBPartner.list().size() == 0) {
            throw new OBException(message + "\nEmpleado con cedula [" + taxId + "] no encontado");
        }

        validator.checkBigDecimal(values[2]);
        validator.checkBigDecimal(values[3]);

        String concept = validator.checkString(values[4], 60).trim();
        if (!(concept.equals("D") || concept.equals("JP"))) {
            throw new OBException(
                    message + "\nEl concepto debe ser [D] para desahucio o [JP] para jubilación patronal");
        }

        validator.checkBigDecimal(values[5]);
        validator.checkBigDecimal(values[6]);
        validator.checkBigDecimal(values[7]);

        return values;
    }

    @Override
    public BaseOBObject internalProcess(Object... values) throws Exception {
        return process((String) values[0], (String) values[1], (String) values[2], (String) values[3],
                (String) values[4], (String) values[5], (String) values[6], (String) values[7]);
    }

    public BaseOBObject process(String periodo, String cedula, String ts, String remuneracion, String concepto,
            String obligacion, String costo, String interes) throws Exception {
        Sspr_ActuarialCalcStudy actuarialCalcStudy = null;
        try {
            Date periodDate = getDate(periodo.trim());
            OBCriteria<Period> qPeriod = OBDal.getInstance().createCriteria(Period.class);
            qPeriod.add(Restrictions.eq(Period.PROPERTY_ACTIVE, true));
            qPeriod.add(Restrictions.le(Period.PROPERTY_STARTINGDATE, periodDate));
            qPeriod.add(Restrictions.ge(Period.PROPERTY_ENDINGDATE, periodDate));
            Period period = qPeriod.list().get(0);

            OBCriteria<Sspr_ActuarialCalcStudy> qActuarialCalcStudy = OBDal.getInstance()
                    .createCriteria(Sspr_ActuarialCalcStudy.class);
            qActuarialCalcStudy.add(Restrictions.eq(Sspr_ActuarialCalcStudy.PROPERTY_ACTIVE, true));
            qActuarialCalcStudy.add(Restrictions.eq(Sspr_ActuarialCalcStudy.PROPERTY_PERIOD, period));
            qActuarialCalcStudy.add(Restrictions.eq(Sspr_ActuarialCalcStudy.PROPERTY_TAXID, cedula.trim()));
            qActuarialCalcStudy.add(Restrictions.eq(Sspr_ActuarialCalcStudy.PROPERTY_CONCEPT, concepto.trim()));
            if (qActuarialCalcStudy.list().size() > 0) {
                actuarialCalcStudy = qActuarialCalcStudy.list().get(0);
            } else {
                actuarialCalcStudy = OBProvider.getInstance().get(Sspr_ActuarialCalcStudy.class);
            }

            actuarialCalcStudy.setPeriod(period);

            OBCriteria<BusinessPartner> qBPartner = OBDal.getInstance().createCriteria(BusinessPartner.class);
            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SSPRSTATUS, "A"));
            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_EMPLOYEE, true));
            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, cedula.trim()));
            BusinessPartner bPartner = qBPartner.list().get(0);

            actuarialCalcStudy.setTaxID(bPartner.getTaxID());
            actuarialCalcStudy.setName(bPartner.getName());
            actuarialCalcStudy.setDateBirth(bPartner.getSSPRBirthday());
            actuarialCalcStudy.setDateAdmission(bPartner.getSSPREntrydate());

            Calendar dateBirth = new GregorianCalendar();
            dateBirth.setTime(actuarialCalcStudy.getDateBirth());
            Calendar now = new GregorianCalendar();
            now.setTime(periodDate);
            int years = now.get(Calendar.YEAR) - dateBirth.get(Calendar.YEAR);
            if (now.get(Calendar.MONTH) < dateBirth.get(Calendar.MONTH)) {
//                if (now.get(Calendar.DAY_OF_MONTH) <= dateBirth.get(Calendar.DAY_OF_MONTH)) {
//                    years--;
//                }
                years--;
            }
            actuarialCalcStudy.setAge(new Long(years));

            actuarialCalcStudy.setTS(getBigDecimal(ts));
            actuarialCalcStudy.setRemuneration(getBigDecimal(remuneracion));
            actuarialCalcStudy.setConcept(concepto.trim());
            actuarialCalcStudy.setObligation(getBigDecimal(obligacion));
            actuarialCalcStudy.setCost(getBigDecimal(costo));
            actuarialCalcStudy.setInterest(getBigDecimal(interes));
            actuarialCalcStudy.setTotal(actuarialCalcStudy.getObligation().add(actuarialCalcStudy.getCost())
                    .add(actuarialCalcStudy.getInterest()));

            OBDal.getInstance().save(actuarialCalcStudy);
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            OBDal.getInstance().rollbackAndClose();
        }

        return actuarialCalcStudy;
    }

    private BigDecimal getBigDecimal(String value)
            throws Exception {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Date getDate(String value) throws Exception {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        return df.parse(value.trim());
    }

}
