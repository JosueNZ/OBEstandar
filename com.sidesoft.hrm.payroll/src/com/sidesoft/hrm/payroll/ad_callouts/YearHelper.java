package com.sidesoft.hrm.payroll.ad_callouts;

import java.util.Calendar;
import java.util.Date;

public class YearHelper {

  public float calculateYearByDaysDate(String fechaInicial, String fechaFinal) {

    String[] fechaI = fechaInicial.split("/");
    String[] fechaF = fechaFinal.split("/");

    Calendar cal = Calendar.getInstance();

    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(fechaI[0]));
    cal.set(Calendar.MONTH, Integer.parseInt(fechaI[1]));
    cal.set(Calendar.YEAR, Integer.parseInt(fechaI[2]));
    Date firstDate = cal.getTime();

    cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(fechaF[0]));
    cal.set(Calendar.MONTH, Integer.parseInt(fechaF[1]));
    cal.set(Calendar.YEAR, Integer.parseInt(fechaF[2]));
    Date secondDate = cal.getTime();

    long diferencia = secondDate.getTime() - firstDate.getTime();
    // System.out.println("Diferencia en dias: " + diferencia / 1000 / 60 / 60 / 24);
    int parseDays = (int) (diferencia / 1000 / 60 / 60 / 24);
    double totalInYear = (double) parseDays / (double) 360;
    // System.out.println("totalInYear " + totalInYear);
    return (float) totalInYear;
  }

  public String dateFormats(int endDateDay, int endDateMonth, int endDateYear) {
    return Integer.toString(endDateDay) + "/" + Integer.toString(endDateMonth) + "/"
        + Integer.toString(endDateYear);
  }

}
