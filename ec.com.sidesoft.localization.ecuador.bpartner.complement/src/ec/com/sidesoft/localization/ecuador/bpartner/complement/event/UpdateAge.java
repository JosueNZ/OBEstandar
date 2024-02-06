package ec.com.sidesoft.localization.ecuador.bpartner.complement.event;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.common.businesspartner.BusinessPartner;

public class UpdateAge extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      BusinessPartner.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final BusinessPartner Bpartner = (BusinessPartner) event.getTargetInstance();

    if (Bpartner.getSbpcDatebirth() != null) {

      Date DateBirthday = Bpartner.getSbpcDatebirth();
      // String DocumentPartner = (Bpartner.getSSPRDocumentNo()!= null) ?
      // Bpartner.getSSPRDocumentNo().toString():"";

      try {
        /**
         * Se puede cambiar la mascara por el formato de la fecha que se quiera recibir, por ejemplo
         * año mes día "yyyy-MM-dd" en este caso es día mes año
         */

        Date fechaNac = new SimpleDateFormat("dd-MM-yyyy").parse((DateBirthday.toString()));
      } catch (Exception ex) {
        System.out.println("Error:" + ex);
      }
      Calendar fechaNacimiento = Calendar.getInstance();
      // Se crea un objeto con la fecha actual
      Calendar fechaActual = Calendar.getInstance();
      // Se asigna la fecha recibida a la fecha de nacimiento.
      fechaNacimiento.setTime(DateBirthday);
      // Se restan la fecha actual y la fecha de nacimiento
      int anio = fechaActual.get(Calendar.YEAR) - fechaNacimiento.get(Calendar.YEAR);
      int mes = fechaActual.get(Calendar.MONTH) - fechaNacimiento.get(Calendar.MONTH);
      int dia = fechaActual.get(Calendar.DATE) - fechaNacimiento.get(Calendar.DATE);
      // Se ajusta el año dependiendo el mes y el día
      if (mes < 0 || (mes == 0 && dia < 0)) {
        anio--;
      }
      // Regresa la edad en base a la fecha de nacimiento
      String PartnerAge2 = (String.valueOf(anio) != null) ? String.valueOf(anio) : null;

      if (PartnerAge2.toString() != null) {

        final Entity BpartnerEntity = ModelProvider.getInstance().getEntity(
            BusinessPartner.ENTITY_NAME);
        final Property valueProperty = BpartnerEntity.getProperty(BusinessPartner.PROPERTY_SBPCAGE);

        event.setCurrentState(valueProperty, PartnerAge2);

      }

    }

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final BusinessPartner Bpartner = (BusinessPartner) event.getTargetInstance();

    if (Bpartner.getSbpcDatebirth() != null) {

      Date DateBirthday = Bpartner.getSbpcDatebirth();
      // String DocumentPartner = (Bpartner.getSSPRDocumentNo()!= null) ?
      // Bpartner.getSSPRDocumentNo().toString():"";

      try {
        /**
         * Se puede cambiar la mascara por el formato de la fecha que se quiera recibir, por ejemplo
         * año mes día "yyyy-MM-dd" en este caso es día mes año
         */

        Date fechaNac = new SimpleDateFormat("dd-MM-yyyy").parse((DateBirthday.toString()));
      } catch (Exception ex) {
        System.out.println("Error:" + ex);
      }
      Calendar fechaNacimiento = Calendar.getInstance();
      // Se crea un objeto con la fecha actual
      Calendar fechaActual = Calendar.getInstance();
      // Se asigna la fecha recibida a la fecha de nacimiento.
      fechaNacimiento.setTime(DateBirthday);
      // Se restan la fecha actual y la fecha de nacimiento
      int anio = fechaActual.get(Calendar.YEAR) - fechaNacimiento.get(Calendar.YEAR);
      int mes = fechaActual.get(Calendar.MONTH) - fechaNacimiento.get(Calendar.MONTH);
      int dia = fechaActual.get(Calendar.DATE) - fechaNacimiento.get(Calendar.DATE);
      // Se ajusta el año dependiendo el mes y el día
      if (mes < 0 || (mes == 0 && dia < 0)) {
        anio--;
      }
      // Regresa la edad en base a la fecha de nacimiento
      String PartnerAge2 = (String.valueOf(anio) != null) ? String.valueOf(anio) : null;

      if (PartnerAge2.toString() != null) {

        final Entity BpartnerEntity = ModelProvider.getInstance().getEntity(
            BusinessPartner.ENTITY_NAME);
        final Property valueProperty = BpartnerEntity.getProperty(BusinessPartner.PROPERTY_SBPCAGE);

        event.setCurrentState(valueProperty, PartnerAge2);

      }

    }

  }
}
