/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2012-2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package com.sidesoft.hrm.payroll.biometrical.event;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.enterprise.event.Observes;
import org.apache.log4j.Logger;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.CancelAndReplaceUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

import org.openbravo.erpCommon.businessUtility.WindowTabsData;
import com.sidesoft.hrm.payroll.Shift;
import com.sidesoft.hrm.payroll.biometrical.SprbiDays;

public class SprbiShiftEventListener extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Shift.ENTITY_NAME) };
  private static String validformattedDate = "00:00:00";
  private static String formatHour = "HH:mm:ss";

  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final Shift record = (Shift) event.getTargetInstance();

    if (record != null && record.isSprbiConfigurationLines().equals(true)) {
      // Validacion cabecera
      validColumnsShift(record);
    } else {
      // Validacion lineas de dias configuradas
      if (record.getSprbiDaysList().size() > 0) {
        for (SprbiDays line : record.getSprbiDaysList()) {
          validColumnsDays(line);
        }
      }
    }
  }

  public void onSave(@Observes EntityNewEvent event) {

    if (!isValidEvent(event)) {
      return;
    }

    final Shift record = (Shift) event.getTargetInstance();
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);

    String formattedDate = "";

    if (record != null && record.isSprbiConfigurationLines().equals(true)) {
      String message = Utility.messageBD(conn, "sprbi_config_line", language);

      // Entrada 1
      if (record.getStarttime() != null) {
        formattedDate = new SimpleDateFormat(formatHour).format(record.getStarttime());
        if (!formattedDate.equals(validformattedDate)) {
          logger.info("getStarttime: " + formattedDate);
          throw new OBException(message);
        }
      }
      // Entrada desde
      if (record.getSprbiEntryFrom() != null) {
        formattedDate = new SimpleDateFormat(formatHour).format(record.getSprbiEntryFrom());
        if (!formattedDate.equals(validformattedDate)) {
          logger.info("getSprbiEntryFrom: " + formattedDate);
          throw new OBException(message);
        }
      }
      // Entrada hasta
      if (record.getSprbiEntryUntil() != null) {
        formattedDate = new SimpleDateFormat(formatHour).format(record.getSprbiEntryUntil());
        if (!formattedDate.equals(validformattedDate)) {
          logger.info("getSprbiEntryUntil: " + formattedDate);
          throw new OBException(message);
        }
      }
      // Salida 1
      if (record.getEndtime() != null) {
        formattedDate = new SimpleDateFormat(formatHour).format(record.getEndtime());
        if (!formattedDate.equals(validformattedDate)) {
          logger.info("getEndtime: " + formattedDate);
          throw new OBException(message);
        }
      }
      // Salida desde
      if (record.getSprbiExitFrom() != null) {
        formattedDate = new SimpleDateFormat(formatHour).format(record.getSprbiExitFrom());
        if (!formattedDate.equals(validformattedDate)) {
          logger.info("getSprbiExitFrom: " + formattedDate);
          throw new OBException(message);
        }
      }
      // Salida hasta
      if (record.getSprbiEntryUntil() != null) {
        formattedDate = new SimpleDateFormat(formatHour).format(record.getSprbiEntryUntil());
        if (!formattedDate.equals(validformattedDate)) {
          logger.info("getSprbiEntryUntil: " + formattedDate);
          throw new OBException(message);
        }
      }
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
  }

  public void validColumnsShift(Shift record) {

    String formattedDate = "";
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);
    String message = Utility.messageBD(conn, "sprbi_config_line", language);
    String messagerq = Utility.messageBD(conn, "sprbi_config_required", language);
    // Entrada 1
    if (record.getStarttime() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getStarttime());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getStarttime: " + formattedDate);
        throw new OBException(message);
      }
    } else {
      throw new OBException(messagerq);
    }
    // Entrada desde
    if (record.getSprbiEntryFrom() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getSprbiEntryFrom());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getSprbiEntryFrom: " + formattedDate);
        throw new OBException(message);
      }
    } else {
      throw new OBException(messagerq);
    }
    // Entrada hasta
    if (record.getSprbiEntryUntil() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getSprbiEntryUntil());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getSprbiEntryUntil: " + formattedDate);
        throw new OBException(message);
      }
    } else {
      throw new OBException(messagerq);
    }
    // Salida 1
    if (record.getEndtime() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getEndtime());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getEndtime: " + formattedDate);
        throw new OBException(message);
      }
    } else {
      throw new OBException(messagerq);
    }
    // Salida desde
    if (record.getSprbiExitFrom() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getSprbiExitFrom());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getSprbiExitFrom: " + formattedDate);
        throw new OBException(message);
      }
    } else {
      throw new OBException(messagerq);
    }
    // Salida hasta
    if (record.getSprbiEntryUntil() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getSprbiEntryUntil());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getSprbiEntryUntil: " + formattedDate);
        throw new OBException(message);
      }
    } else {
      throw new OBException(messagerq);
    }
  }

  public void validColumnsDays(SprbiDays record) {

    String formattedDate = "";
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);
    String message = Utility.messageBD(conn, "sprbi_existing_config_line", language);
    // Entrada 1
    if (record.getStartingTime() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getStartingTime());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getStartingTime: " + formattedDate);
        throw new OBException(message);
      }
    }
    // Entrada 2
    if (record.getEntry() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getEntry());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getEntry: " + formattedDate);
        throw new OBException(message);
      }
    }
    // Entrada desde
    if (record.getEntryFrom() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getEntryFrom());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getEntryFrom: " + formattedDate);
        throw new OBException(message);
      }
    }
    // Entrada hasta
    if (record.getEntryUntil() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getEntryUntil());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getEntryUntil: " + formattedDate);
        throw new OBException(message);
      }
    }
    // Salida 1
    if (record.getEndingTime() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getEndingTime());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getEndingTime: " + formattedDate);
        throw new OBException(message);
      }
    }
    // Salida 2
    if (record.getExit() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getExit());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getExit: " + formattedDate);
        throw new OBException(message);
      }
    }
    // Salida desde
    if (record.getExitFrom() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getExitFrom());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getExitFrom: " + formattedDate);
        throw new OBException(message);
      }
    }
    // Salida hasta
    if (record.getExitUntil() != null) {
      formattedDate = new SimpleDateFormat(formatHour).format(record.getExitUntil());
      if (!formattedDate.equals(validformattedDate)) {
        logger.info("getExitUntil: " + formattedDate);
        throw new OBException(message);
      }
    }
  }

}
