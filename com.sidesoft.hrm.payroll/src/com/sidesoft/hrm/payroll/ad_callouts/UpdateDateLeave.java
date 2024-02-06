package com.sidesoft.hrm.payroll.ad_callouts;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import com.sidesoft.hrm.payroll.ssprleavetype;

public class UpdateDateLeave extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 12L;

  @SuppressWarnings("null")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    String StartDate = info.getStringParameter("inpstardate", null);
    String Enddate = info.getStringParameter("inpenddate", null);

    // Tipo de permiso
    String leaveTypeID = info.getStringParameter("inpssprLeaveTypeId", null);
    Long val_nodays;

    // Obtengo numero de dias del tipo de permiso
    OBCriteria<ssprleavetype> ObjLeaveType = OBDal.getInstance()
        .createCriteria(ssprleavetype.class);
    ObjLeaveType.add(Restrictions.eq(ssprleavetype.PROPERTY_ID, leaveTypeID));

    val_nodays = ObjLeaveType.list().get(0).getNodays();
    if (val_nodays == 0) {

      info.addResult("inpnodays", String.valueOf(0));
    } else {

      SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
      Date dates = null;
      Date datee = null;

      try {

        dates = formatter.parse(StartDate);

      } catch (Exception e) {

      }

      try {

        datee = formatter.parse(Enddate);

      } catch (Exception e) {

      }

      Calendar DateStart = Calendar.getInstance();

      // Se asigna la fecha recibida a la fecha de nacimiento.
      DateStart.setTime(dates);
      Calendar DateEnd = Calendar.getInstance();

      // Se asigna la fecha recibida a la fecha de nacimiento.
      DateEnd.setTime(datee);

      // New development
      if (DateEnd.compareTo(DateStart) > 0) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(DateEnd.getTime().getTime() - DateStart.getTime().getTime());
        info.addResult("inpnodays", c.get(Calendar.DAY_OF_YEAR) + 1);
      } else {
        if (DateEnd.equals(DateStart)) {

          long nodays = 1;
          info.addResult("inpnodays", String.valueOf(nodays));
        } else {
          long nodays = 0;
          info.addResult("inpnodays", String.valueOf(nodays));
        }
      }

    }

    // long nodays = DateEnd.get(Calendar.DATE) - DateStart.get(Calendar.DATE);
    // long nodays = DateEnd.get(Calendar.DAY_OF_MONTH) - DateStart.get(Calendar.DAY_OF_MONTH);

    // info.addResult("inpnodays", String.valueOf(nodays));

  }

}
