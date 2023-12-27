package com.sidesoft.hrm.payroll.event;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.service.db.DalConnectionProvider;

public class UpdateBirthdayEmployeePREvent extends EntityPersistenceEventObserver {

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

    if (Bpartner.getSSPRBirthday() != null) {

      Date DateBirthday = Bpartner.getSSPRBirthday();
      String DocumentPartner = (Bpartner.getSSPRDocumentNo() != null) ? Bpartner
          .getSSPRDocumentNo().toString() : "";

      Date fechaNac = null;
      try {
        /**
         * Se puede cambiar la mascara por el formato de la fecha que se quiera recibir, por ejemplo
         * año mes día "yyyy-MM-dd" en este caso es día mes año
         */

        fechaNac = new SimpleDateFormat("dd-MM-yyyy").parse((DateBirthday.toString()));
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

      if (DocumentPartner != null) {

        final Entity BpartnerEntity = ModelProvider.getInstance().getEntity(
            BusinessPartner.ENTITY_NAME);
        final Property valueProperty = BpartnerEntity
            .getProperty(BusinessPartner.PROPERTY_SSPRDOCUMENTNO);

        event.setCurrentState(valueProperty, DocumentPartner);

      }
      if (PartnerAge2.toString() != null) {

        final Entity BpartnerEntity = ModelProvider.getInstance().getEntity(
            BusinessPartner.ENTITY_NAME);
        final Property valueProperty = BpartnerEntity.getProperty(BusinessPartner.PROPERTY_SSHRAGE);

        event.setCurrentState(valueProperty, PartnerAge2);

      }

    }
    if (Bpartner.isSsprReservefundsiess()) {

      ConnectionProvider conn = new DalConnectionProvider(false);
      String language = OBContext.getOBContext().getLanguage().getLanguage();

      String strConeptID = Bpartner.getSsprConcept() == null ? "ND" : Bpartner.getSsprConcept()
          .getId();
      if (strConeptID.equals("ND")) {
        throw new OBException(Utility.messageBD(conn, "@Sspr_ValidateRefundIess@", language));
      }

    }

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final BusinessPartner Bpartner = (BusinessPartner) event.getTargetInstance();

    if (Bpartner.getSSPRBirthday() != null) {

      Date DateBirthday = Bpartner.getSSPRBirthday();
      String DocumentPartner = (Bpartner.getSSPRDocumentNo() != null) ? Bpartner
          .getSSPRDocumentNo().toString() : "";

      Date fechaNac = null;
      try {
        /**
         * Se puede cambiar la mascara por el formato de la fecha que se quiera recibir, por ejemplo
         * año mes día "yyyy-MM-dd" en este caso es día mes año
         */

        fechaNac = new SimpleDateFormat("dd-MM-yyyy").parse((DateBirthday.toString()));
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

      if (DocumentPartner != null) {

        final Entity BpartnerEntity = ModelProvider.getInstance().getEntity(
            BusinessPartner.ENTITY_NAME);
        final Property valueProperty = BpartnerEntity
            .getProperty(BusinessPartner.PROPERTY_SSPRDOCUMENTNO);

        event.setCurrentState(valueProperty, DocumentPartner);

      }
      if (PartnerAge2.toString() != null) {

        final Entity BpartnerEntity = ModelProvider.getInstance().getEntity(
            BusinessPartner.ENTITY_NAME);
        final Property valueProperty = BpartnerEntity.getProperty(BusinessPartner.PROPERTY_SSHRAGE);

        event.setCurrentState(valueProperty, PartnerAge2);

      }

    }

    if (Bpartner.isSsprReservefundsiess()) {

      ConnectionProvider conn = new DalConnectionProvider(false);
      String language = OBContext.getOBContext().getLanguage().getLanguage();

      String strConeptID = Bpartner.getSsprConcept() == null ? "ND" : Bpartner.getSsprConcept()
          .getId();
      if (strConeptID.equals("ND")) {
        throw new OBException(Utility.messageBD(conn, "@Sspr_ValidateRefundIess@", language));
      }

    }

  }
}
