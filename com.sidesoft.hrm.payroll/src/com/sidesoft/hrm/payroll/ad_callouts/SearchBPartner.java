package com.sidesoft.hrm.payroll.ad_callouts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.calendar.Period;

public class SearchBPartner extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String inptaxid = info.getStringParameter("inptaxid", null);
    String inpcPeriodId = info.getStringParameter("inpcPeriodId", null);

    if (StringUtils.isNotEmpty(inptaxid)) {
      OBCriteria<BusinessPartner> qBPartner = OBDal.getInstance()
          .createCriteria(BusinessPartner.class);
      qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
      qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SSPRSTATUS, "A"));
      qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_EMPLOYEE, true));
      qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, inptaxid.trim()));
      if (qBPartner.list().size() == 0) {
        clear(info);
        return;
      }

      BusinessPartner bPartner = qBPartner.list().get(0);
      info.addResult("inpname", bPartner.getName());
      info.addResult("inpdateBirth", bPartner.getSSPRBirthday());
      info.addResult("inpdateAdmission", bPartner.getSSPREntrydate());
      info.addResult("inpremuneration", bPartner.getSsprCurrentsalary());

      if (StringUtils.isNotEmpty(inpcPeriodId)) {
        Period period = OBDal.getInstance().get(Period.class, inpcPeriodId.trim());

        Calendar dateBirth = new GregorianCalendar();
        dateBirth.setTime(bPartner.getSSPRBirthday());
        Calendar now = new GregorianCalendar();
        now.setTime(period.getEndingDate());
        int years = now.get(Calendar.YEAR) - dateBirth.get(Calendar.YEAR);
        if (now.get(Calendar.MONTH) < dateBirth.get(Calendar.MONTH)) {
          // if (now.get(Calendar.DAY_OF_MONTH) <= dateBirth.get(Calendar.DAY_OF_MONTH)) {
          // years--;
          // }
          years--;
        }
        info.addResult("inpage", years);

        // *** Calculate field Remuneration
        if (StringUtils.isNotEmpty(bPartner.getSSPREntrydate().toString())) {
          Calendar endDateTmp = new GregorianCalendar();
          endDateTmp.setTime(period.getEndingDate());
          // Format date into output format
          DateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
          YearHelper yearH = new YearHelper();
          String strInitDate = outputFormat.format(bPartner.getSSPREntrydate());
          String strEndDate = yearH.dateFormats((endDateTmp.get(Calendar.DAY_OF_MONTH)),
              (endDateTmp.get(Calendar.MONTH)), (endDateTmp.get(Calendar.YEAR)));

          float daysRemuneration = yearH.calculateYearByDaysDate(strInitDate, strEndDate);
          info.addResult("inpts", daysRemuneration);
        }
      }

    } else {
      clear(info);
    }
  }

  private void clear(CalloutInfo info) {
    info.addResult("inpname", null);
    info.addResult("inpdateBirth", null);
    info.addResult("inpdateAdmission", null);
    info.addResult("inpage", null);
    info.addResult("inpremuneration", null);
  }

}
