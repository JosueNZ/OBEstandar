package com.sidesoft.hrm.payroll.event;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.enterprise.event.Observes;
import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.hrm.payroll.ssprleaveemp;
import com.sidesoft.hrm.payroll.ssprleavetype;

public class ValidateHourDays extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(ssprleaveemp.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final ssprleaveemp leaveemp = (ssprleaveemp) event.getTargetInstance();

    ReturnQuery(leaveemp);

  }

  public void onUpdate(@Observes EntityUpdateEvent event) throws ServletException {
    if (!isValidEvent(event)) {
      return;
    }

    final ssprleaveemp leaveemp2 = (ssprleaveemp) event.getTargetInstance();

    ReturnQuery(leaveemp2);
  }

  private void ReturnQuery(ssprleaveemp Strserach) {

    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    DateFormat hourFormat = new SimpleDateFormat("hh:mm");
    // String hora1 = hora;

    Double StrDaysLeaveP = 0.00;
    Double StrHoursP = 0.00;

    // OBTENGO NO DIAS DEL PERMISO
    if (Double.valueOf(Strserach.getNoDays()) != 0
        || Double.valueOf(Strserach.getNoDays()) != null) {
      StrDaysLeaveP = Double.valueOf(Strserach.getNoDays());
    }

    // OBTENGO EL NUMERO DE HORAS DEL PERMISO
    if (Double.valueOf(Strserach.getNoHours().toString()) != 0
        || Double.valueOf(Strserach.getNoHours().toString()) != null) {
      StrHoursP = Double.valueOf(Strserach.getNoHours().toString());
    }

    // OBTENGO EL TIPO DE PERMISO
    String StrLeaveType = Strserach.getLeaveType().getId().toString();

    if (!StrLeaveType.isEmpty()) {

      OBCriteria<ssprleavetype> ObjLeaveType = OBDal.getInstance()
          .createCriteria(ssprleavetype.class);
      ObjLeaveType.add(Restrictions.eq(ssprleavetype.PROPERTY_ID, StrLeaveType));

      if (ObjLeaveType.count() > 0) {

        // OBTENGO HORAS DE TYPO DE PERMISO
        Double StrHoursT = Double.valueOf(ObjLeaveType.list().get(0).getNohours().toString());

        // OBTENGO DIAS DEL TIPO DE PERMISO
        Double StrDaysT = Double.valueOf(ObjLeaveType.list().get(0).getNodays().toString());

        // VALIDO HORAS
        if (StrHoursT != null) {
          if (StrHoursP > StrHoursT) {
            throw new OBException("Error: Las Horas de permiso tienen que ser menores a "
                + StrHoursP + " horas o escoja otro Tipo de Permiso");
          }
        }

        // /VALIDO DIAS
        if (StrDaysT != null) {
          if (StrDaysLeaveP > StrDaysT) {

            throw new OBException("Error: Los Dias de permiso tienen que ser menores a " + StrDaysT
                + " dias o escoja otro Tipo de Permiso");
          }
        }

      }

    }

  }
}
