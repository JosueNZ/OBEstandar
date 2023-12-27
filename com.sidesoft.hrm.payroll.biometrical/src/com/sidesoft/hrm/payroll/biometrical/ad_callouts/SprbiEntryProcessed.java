package com.sidesoft.hrm.payroll.biometrical.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.sql.Timestamp;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.hrm.payroll.Concept;
import com.sidesoft.hrm.payroll.Contract;
import com.sidesoft.hrm.payroll.Shift;
import com.sidesoft.hrm.payroll.advanced.SfprEvolutionSalary;
import com.sidesoft.hrm.payroll.biometrical.SprbiDays;

import ec.com.sidesoft.payroll.events.SPEVConfigNews;

public class SprbiEntryProcessed extends SimpleCallout {
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    TimeZone tz = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("0"));

    String movement = info.getStringParameter("inpdatemovement", null);
    String employee = info.getStringParameter("inpcBpartnerId", null);
    String entry1 = info.getStringParameter("inpentry1", null);
    String exit1 = info.getStringParameter("inpexit1", null);
    String entryProcessed = info.getStringParameter("inpentryTimeProcessed", null);
    String outputProcessed = info.getStringParameter("inpoutputTimeProcessed", null);

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      // sdf.setTimeZone(TimeZone.getTimeZone("-05:00"));
      SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy");
      SimpleDateFormat noSeconds = new SimpleDateFormat("HH:mm");

      Long fix = new Long(18000000);
      Long day = new Long(68400000);

      // Limpiamos los campos calculados
      info.addResult("inpgeneratedTime", sdf.format(new Date(fix)));
      info.addResult("inpworkedHour", sdf.format(new Date(fix)));
      info.addResult("inpvalidated", false);
      info.addResult("inpdelay1", sdf.format(new Date(fix)));
      info.addResult("inpauthorizedTime", sdf.format(new Date(fix)));
      info.addResult("inpearlyDismissalHours", sdf.format(new Date(fix)));
      info.addResult("inptime50", sdf.format(new Date(fix)));
      info.addResult("inptimeValue50", 0.00);
      info.addResult("inptime100", sdf.format(new Date(fix)));
      info.addResult("inptimeValue100", 0.00);

      Date movementD = df.parse(movement);

      Date entry1D = sdf.parse(entry1);
      entry1D = new Date(
          entry1D.getTime() >= fix ? entry1D.getTime() - fix : entry1D.getTime() + day);

      Date exit1D = sdf.parse(exit1);
      exit1D = new Date(exit1D.getTime() >= fix ? exit1D.getTime() - fix : exit1D.getTime() + day);

      Date entryD = sdf.parse(entryProcessed);
      entryD = new Date(entryD.getTime() >= fix ? entryD.getTime() - fix : entryD.getTime() + day);

      Date outputD = sdf.parse(outputProcessed);
      outputD = new Date(
          outputD.getTime() >= fix ? outputD.getTime() - fix : outputD.getTime() + day);

      String auth = getAuthHours(movementD, employee);
      Date authD = sdf.parse(auth);
      info.addResult("inpauthorizedTime", sdf.format(new Date(authD.getTime() + fix)));

      /**
       * Horas trabajadas y Validado Si la entrada y salida procesada tienen valor
       */
      if (getTimeValue(entryD) > 0 && getTimeValue(outputD) > 0) {
        Long worked = outputD.getTime() - entryD.getTime();
        Date workedD = new Date(worked + fix);
        info.addResult("inpworkedHour", noSeconds.format(workedD) + ":00");
        info.addResult("inpvalidated", true);
      }

      /**
       * Horas generadas Si la salida procesada es mayor a la salida del turno
       */
      if (outputD.getTime() > exit1D.getTime()) {
        Long overtime = outputD.getTime() - exit1D.getTime();
        Date overtimeD = new Date(overtime + fix);
        info.addResult("inpgeneratedTime", noSeconds.format(overtimeD) + ":00");
      }
      /**
       * Salida temprano Si la salida procesada es menor a la salida del turno
       */
      if (getTimeValue(outputD) > 0 && outputD.getTime() < exit1D.getTime()) {
        Long early = exit1D.getTime() - outputD.getTime();
        Date earlyD = new Date(early + fix);

        info.addResult("inpearlyDismissalHours", noSeconds.format(earlyD) + ":00");
      }

      if (getTimeValue(entryD) > 0 && getTimeValue(outputD) > 0 && !employee.isEmpty()) {
        // Obtenemos el empleado
        BusinessPartner employeeE = OBDal.getInstance().get(BusinessPartner.class, employee);
        // Obtenermos el contrato
        OBCriteria<Contract> contract = OBDal.getInstance().createCriteria(Contract.class);
        contract.add(Restrictions.eq(Contract.PROPERTY_BUSINESSPARTNER, employeeE));

        if (contract.list().size() > 0) {
          // Obtenemos el turno
          Shift shift = contract.list().get(0).getSsprShift();

          /**
           * Atraso Si la entrada procesada es mayor a la entrada del turno
           */
          Long delayParameter = getTimeValue(shift.getSprbiMinuteDelayParameter());
          if (entryD.getTime() > (entry1D.getTime() + delayParameter)) {
            Long delay = entryD.getTime() - entry1D.getTime() - delayParameter;
            Date delayD = new Date(delay + fix);
            info.addResult("inpdelay1", noSeconds.format(delayD) + ":00");
          }

          int weekday = dayOfWeek(movementD);
          boolean holiday = isHoliday(movementD);
          int _weekday = weekday == 1 ? 7 : weekday - 1;
          OBCriteria<SprbiDays> days = OBDal.getInstance().createCriteria(SprbiDays.class);
          days.add(Restrictions.eq(SprbiDays.PROPERTY_SHIFT, shift));
          days.add(Restrictions.eq(SprbiDays.PROPERTY_DAY, String.valueOf(_weekday)));
          Timestamp overtimeParameter = null;
          if (days.list().size() > 0 && shift.isSprbiConfigurationLines()) {
            SprbiDays dayShift = (SprbiDays) days.list().get(0);
            overtimeParameter = dayShift.getOvertimeParam();
          } else {
            overtimeParameter = shift.getSprbiOvertimeParameter();
          }

          /**
           * Nro horas 50: Empleado sin check de horas extra, Dia entre lunes y viernes configurado,
           * Horas autorizadas, Que no sea feriado
           */
          Long hour50 = null;
          if (!employeeE.isSsprExtraHours() && weekday > 1 && weekday < 7 && days.list().size() > 0
              && getTimeValue(authD) > 0 && !holiday) {
            // Verificar que cumpla con las horas laborales
            Long wH = getTimeValue(outputD) - getTimeValue(entryD);
            // y si la salida procesada es mayor a la del turno
            if (wH > getTimeValue(shift.getSprovWorkingHours())
                && outputD.getTime() >= exit1D.getTime() + getTimeValue(overtimeParameter)) {
              hour50 = outputD.getTime() - exit1D.getTime();

              // Se le resta el retraso a las horas extras
              Long delay = getTimeValue(entry1D) - getTimeValue(entryD);
              delay = delay > 0 ? new Long(0) : delay * -1;
              if (delay > 0) {
                if (delay > hour50) {
                  hour50 = new Long(0);
                } else {
                  hour50 = hour50 - delay;
                }
              }

              if (hour50 > authD.getTime()) {
                hour50 = authD.getTime();
              }

              // Horas maximas diarias
              Long hMD = getTimeValue(shift.getSprbiHoursMaxDay());
              if (hMD > 0 && hour50 > hMD) {
                hour50 = hMD;
              }

              // Acumulado horas extras a la semana
              String hourWeek = getHourWeek(movementD, employee);
              Date hW = sdf.parse(hourWeek);
              Long hMW = getTimeValue(shift.getSprbiHoursMaxWeek());
              if (hMW > 0 && hW.getTime() + hour50 > hMW) {
                hour50 = hMW - hW.getTime();
              }

              // Acumulado horas extras al mes
              BigDecimal oneHour = new BigDecimal(3600000);
              BigDecimal hourMonth = getHourMonth(movementD, employee).multiply(oneHour);
              BigDecimal h50 = new BigDecimal(hour50);
              BigDecimal total = hourMonth.add(h50);
              BigDecimal hMM = shift.getSprbiHoursMaxMonth().multiply(oneHour);
              if (hMM.compareTo(BigDecimal.ZERO) > 0 && total.compareTo(hMM) > 0) {
                total = hMM.subtract(hourMonth);
                hour50 = total.longValue();
              }

              Date hour50D = new Date(hour50 + fix);

              info.addResult("inptime50", noSeconds.format(hour50D) + ":00");
            }
          }

          /**
           * Nro horas 100
           */
          Long hour100 = new Long(0);
          // Sino tiene check de horas extras
          if (!employeeE.isSsprExtraHours()) {
            // Sino es feriado
            if (!holiday) {
              // Si tiene horas autorizadas
              if (getTimeValue(authD) > 0) {
                // Si es sabado o domingo y lo tiene configurado
                if ((weekday == 1 || weekday == 7) && days.list().size() > 0) {
                  if (outputD.getTime() >= exit1D.getTime() + getTimeValue(overtimeParameter)) {
                    hour100 = outputD.getTime() - exit1D.getTime();

                    if (hour100 > authD.getTime()) {
                      hour100 = authD.getTime();
                    }

                    Date hour100D = new Date(hour100 + fix);
                    info.addResult("inptime100", noSeconds.format(hour100D) + ":00");
                  }
                }
                // Sino tiene el dia configurado
                if (days.list().size() == 0) {
                  hour100 = outputD.getTime() - entryD.getTime();
                  // Si las horas 100 generadas son mayores al minimo de horas extras
                  if (shift.isLunch() && hour100 > getTimeValue(shift.getSprovMinimumOvertime())) {
                    hour100 = hour100 - getTimeValue(shift.getSprbiFeedParam());
                  }

                  if (hour100 > authD.getTime()) {
                    hour100 = authD.getTime();
                  }

                  Date hour100D = new Date(hour100 + fix);
                  info.addResult("inptime100", noSeconds.format(hour100D) + ":00");
                }
              }
              // Si es feriado
            } else {
              hour100 = outputD.getTime() - entryD.getTime();
              // Si tiene check de alimentacion y si las horas 100 generadas son mayores al minimo
              // de horas extras
              if (shift.isLunch() && hour100 > getTimeValue(shift.getSprovMinimumOvertime())) {
                hour100 = hour100 - getTimeValue(shift.getSprbiFeedParam());
              }

              Date hour100D = new Date(hour100 + fix);
              info.addResult("inptime100", noSeconds.format(hour100D) + ":00");
            }

            // Si tiene horas 100, no se calculan atrasos
            if (hour100 > 0) {
              info.addResult("inpdelay1", sdf.format(new Date(fix)));
            }
          }

          /**
           * Calculo de horas extras 50
           * 
           */
          String salaryId = getSalaryId(contract.list().get(0).getId(), movementD);
          SfprEvolutionSalary salary = OBDal.getInstance().get(SfprEvolutionSalary.class, salaryId);

          OBCriteria<Concept> concept = OBDal.getInstance().createCriteria(Concept.class);
          concept.add(Restrictions.eq(Concept.PROPERTY_CONCEPTTYPEPAYROLL, "HF"));

          if (concept.list().size() > 0 && salary != null) {
            OBCriteria<SPEVConfigNews> event = OBDal.getInstance()
                .createCriteria(SPEVConfigNews.class);
            event.add(Restrictions.eq(SPEVConfigNews.PROPERTY_SSPRCONCEPT, concept.list().get(0)));
            event.add(Restrictions.eq(SPEVConfigNews.PROPERTY_FUNCTION, "sprbi_nhour_50"));

            if (event.list().size() > 0 && hour50 != null) {
              SPEVConfigNews e = event.list().get(0);

              // Eliminamos los segundos
              hour50 = sdf.parse(noSeconds.format(new Date(hour50)) + ":00").getTime();

              Double nhour = Double.valueOf(hour50) / Double.valueOf(3600000);
              BigDecimal calc = (salary.getAmount().divide(e.getValue(), 4, RoundingMode.CEILING))
                  .multiply(e.getPercentage()).multiply(new BigDecimal(nhour))
                  .setScale(2, RoundingMode.DOWN);
              info.addResult("inptimeValue50", calc);
            }
          }

          /**
           * Calculo de horas extras 100
           */
          concept = OBDal.getInstance().createCriteria(Concept.class);
          concept.add(Restrictions.eq(Concept.PROPERTY_CONCEPTTYPEPAYROLL, "HO"));

          if (concept.list().size() > 0 && salary != null) {
            OBCriteria<SPEVConfigNews> event = OBDal.getInstance()
                .createCriteria(SPEVConfigNews.class);
            event.add(Restrictions.eq(SPEVConfigNews.PROPERTY_SSPRCONCEPT, concept.list().get(0)));
            event.add(Restrictions.eq(SPEVConfigNews.PROPERTY_FUNCTION, "sprbi_nhour_100"));

            if (event.list().size() > 0 && hour100 != null) {
              SPEVConfigNews e = event.list().get(0);

              // Eliminamos los segundos
              hour100 = sdf.parse(noSeconds.format(new Date(hour100)) + ":00").getTime();

              Double nhour = Double.valueOf(hour100) / Double.valueOf(3600000);
              BigDecimal calc = (salary.getAmount().divide(e.getValue(), 4, RoundingMode.CEILING))
                  .multiply(e.getPercentage()).multiply(new BigDecimal(nhour))
                  .setScale(2, RoundingMode.DOWN);

              info.addResult("inptimeValue100", calc);
            }
          }
        }
      }
    } catch (Exception e) {
      System.out.println("Execute: " + e.getMessage());
    } finally {
      TimeZone.setDefault(tz);
    }

  }

  int dayOfWeek(Date date) {
    try {
      Calendar c = Calendar.getInstance();
      c.setTime(date);
      return c.get(Calendar.DAY_OF_WEEK);
    } catch (Exception e) {
      System.out.println("dayOfWeek: " + e.getMessage());
      return 0;
    }
  }

  boolean isHoliday(Date date) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String result = "";
    Integer count = 0;

    try {
      String value = new SimpleDateFormat("yyyy-MM-dd").format(date);
      String sql = "SELECT value FROM sshd_holidays_period_line WHERE value::DATE = ?::DATE";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(sql);
      st.setString(1, value);
      ResultSet resultSet = st.executeQuery();

      while (resultSet.next()) {
        result = resultSet.getString("value");
      }
    } catch (Exception e) {
      System.out.println("isHoliday: " + e.getMessage());
    }
    return result != null && !result.isEmpty();
  }

  Long getTimeValue(Date date) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
      String value = sdf.format(date);
      return sdf.parse(value).getTime();
    } catch (Exception e) {
      System.out.println("getTimeValue: " + e.getMessage());
      return null;
    }
  }

  private String getHourWeek(Date movement, String bPartner) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String result = "";
    Integer count = 0;

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      String value = sdf.format(movement);

      String sql = "SELECT COALESCE(SUM(time_50::TIME), '00:00:00') AS result"
          + " FROM sprov_employee_overtime" + " WHERE c_bpartner_id=?"
          + " AND datemovement>=date_trunc('week', ?::DATE)"
          + " AND datemovement <= date_trunc('week', ?::DATE)+'6 days'::INTERVAL"
          + " AND datemovement != ?::DATE;";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(sql);
      st.setString(1, bPartner);
      st.setString(2, value);
      st.setString(3, value);
      st.setString(4, value);
      ResultSet resultSet = st.executeQuery();

      while (resultSet.next()) {
        result = resultSet.getString("result");
      }
    } catch (Exception e) {
      System.out.println("getHourWeek: " + e.getMessage());
    }
    return result;
  }

  private BigDecimal getHourMonth(Date movement, String bPartner) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String result = "";
    BigDecimal time = new BigDecimal(0);

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      String value = sdf.format(movement);

      String sql = "SELECT COALESCE(EXTRACT(epoch FROM SUM(time_50::TIME)::TIME)/3600, 0) AS result"
          + " FROM sprov_employee_overtime" + " WHERE c_bpartner_id=?"
          + " AND datemovement::TEXT LIKE TO_CHAR(?::DATE, 'yyyy-mm')||'%'"
          + " AND datemovement != ?::DATE;";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(sql);
      st.setString(1, bPartner);
      st.setString(2, value);
      st.setString(3, value);
      ResultSet resultSet = st.executeQuery();

      while (resultSet.next()) {
        result = resultSet.getString("result");
      }
      time = new BigDecimal(result);
    } catch (Exception e) {
      System.out.println("getHourMonth: " + e.getMessage());
    }
    return time;
  }

  String getAuthHours(Date movement, String bPartner) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String result = "00:00:00";

    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
      String value = sdf.format(movement);

      String sql = "SELECT SUM(PA.n_hour::TIME) AS result FROM sprov_period AS P "
          + "JOIN sprov_employee AS E ON E.sprov_period_id = P.sprov_period_id "
          + "JOIN sprov_planned_activity PA ON PA.sprov_employee_id = E.sprov_employee_id "
          + "WHERE P.status = '1' AND PA.payment = 'Y' AND E.c_bpartner_id = ? "
          + "AND PA.date::DATE = ?::DATE;";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(sql);
      st.setString(1, bPartner);
      st.setString(2, value);
      ResultSet resultSet = st.executeQuery();

      while (resultSet.next()) {
        if (resultSet.getString("result") != null)
          result = resultSet.getString("result");
      }
    } catch (Exception e) {
      System.out.println("getAuthHours: " + e.getMessage());
    }
    return result;
  }

  String getSalaryId(String contractId, Date date) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String result = null;

    try {
      String value = new SimpleDateFormat("yyyy-MM-dd").format(date);
      String sql = "SELECT sfpr_evolution_salary_id FROM sfpr_evolution_salary WHERE sspr_contract_id=? AND startdate::DATE <= ?::DATE AND enddate >= ?::DATE";
      PreparedStatement st = null;
      st = conn.getPreparedStatement(sql);
      st.setString(1, contractId);
      st.setString(2, value);
      st.setString(3, value);
      ResultSet resultSet = st.executeQuery();

      while (resultSet.next()) {
        result = resultSet.getString("sfpr_evolution_salary_id");
      }
    } catch (Exception e) {
      System.out.println("getSalaryId: " + e.getMessage());
    }
    return result;
  }
}
