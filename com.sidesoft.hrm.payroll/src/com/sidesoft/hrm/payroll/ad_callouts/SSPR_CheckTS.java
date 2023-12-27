package com.sidesoft.hrm.payroll.ad_callouts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.calendar.Period;

public class SSPR_CheckTS extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    // String strRemuneration = info.getStringParameter("inpremuneration", null);
    // String strNname = info.getStringParameter("inpname", null);
    String inpdateAdmission = info.getStringParameter("inpdateAdmission", null);
    String inpcPeriodId = info.getStringParameter("inpcPeriodId", null);

    try {
      Period period = OBDal.getInstance().get(Period.class, inpcPeriodId);
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
      throw new OBException(e.getMessage(), e);
    }

  }
}
