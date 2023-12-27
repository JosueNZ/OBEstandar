package com.sidesoft.hrm.payroll.ad_callouts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.calendar.Period;

public class CalcAge extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String inpcPeriodId = info.getStringParameter("inpcPeriodId", null);
    String inpdateBirth = info.getStringParameter("inpdateBirth", null);
    String inpdateAdmission = info.getStringParameter("inpdateAdmission", null);
    int years = 0;
    try {
      Period period = OBDal.getInstance().get(Period.class, inpcPeriodId);
      Calendar dateBirth = new GregorianCalendar();
      dateBirth.setTime(getDate(inpdateBirth));
      Calendar now = new GregorianCalendar();
      now.setTime(period.getEndingDate());
      years = now.get(Calendar.YEAR) - dateBirth.get(Calendar.YEAR);
      if (now.get(Calendar.MONTH) < dateBirth.get(Calendar.MONTH)) {
        // if (now.get(Calendar.DAY_OF_MONTH) <= dateBirth.get(Calendar.DAY_OF_MONTH)) {
        // years--;
        // }
        years--;
      }

      // *** Calculate field Remuneration
      if (StringUtils.isNotEmpty(inpdateAdmission)) {
        Calendar endDateTmp = new GregorianCalendar();
        endDateTmp.setTime(period.getEndingDate());
        // Format date into output format
        DateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date dateInitTmp = outputFormat.parse(inpdateAdmission.toString().replace('-', '/'));
        YearHelper yearH = new YearHelper();

        String strInitDate = outputFormat.format(dateInitTmp);
        String strEndDate = yearH.dateFormats((endDateTmp.get(Calendar.DAY_OF_MONTH)),
            (endDateTmp.get(Calendar.MONTH)), (endDateTmp.get(Calendar.YEAR)));

        float daysRemuneration = yearH.calculateYearByDaysDate(strInitDate, strEndDate);
        info.addResult("inpts", daysRemuneration);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    info.addResult("inpage", years);
  }

  private Date getDate(String value) throws Exception {
    SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
    return df.parse(value.trim());
  }
}
