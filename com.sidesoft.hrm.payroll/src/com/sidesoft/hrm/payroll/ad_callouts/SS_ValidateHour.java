package com.sidesoft.hrm.payroll.ad_callouts;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;

import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class SS_ValidateHour extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 12L;
  private static final int SECOND = 1000;
  private static final int MINUTE = 60 * SECOND;
  private static final int HOUR = 60 * MINUTE;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strStartHour = info.getStringParameter("inpstarthour", null);
    String strEndHour = info.getStringParameter("inpendhour", null);

    Date dateStart = null;
    Date dateEnd = null;

    try {
      dateStart = formatDate(strStartHour);
    } catch (Exception e) {
    }

    try {
      dateEnd = formatDate(strEndHour);
    } catch (Exception e) {
    }

    Calendar calDateStart = Calendar.getInstance();
    calDateStart.setTime(dateStart);

    Calendar calDateEnd = Calendar.getInstance();
    calDateEnd.setTime(dateEnd);

    // New development
    if (calDateEnd.after(calDateStart)) {
      long lapsus = calDateEnd.getTimeInMillis() - calDateStart.getTimeInMillis();
      Double hours = Double.valueOf(lapsus) / HOUR;
      info.addResult("inpnohours", hours);
    } else {
      long nodays = 0;
      info.addResult("inpnodays", String.valueOf(nodays));
    }
  }

  protected Date formatDate(String date) throws ParseException {
    return new SimpleDateFormat((String) OBPropertiesProvider.getInstance()
        .getOpenbravoProperties().get(KernelConstants.DATETIME_FORMAT_PROPERTY)).parse(date);
  }

}
