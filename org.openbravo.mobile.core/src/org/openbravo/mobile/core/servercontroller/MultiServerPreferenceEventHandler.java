/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.domain.Preference;

/**
 * Reads the url of all servers and their allowed domains and creates one global list. The list is
 * reset when a mobile server definition changes.
 * 
 * @author mtaal
 */
@ApplicationScoped
public class MultiServerPreferenceEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance()
      .getEntity(Preference.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    check((Preference) event.getTargetInstance());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    check((Preference) event.getTargetInstance());
  }

  private void check(Preference pref) {
    if (!"OBMOBC_MultiServerArchitecture".equals(pref.getProperty())) {
      return;
    }
    if (!("0".equals(pref.getClient().getId()) && "0".equals(pref.getOrganization().getId()))) {
      throw new OBException(OBMessageUtils.getI18NMessage("OBMOBC_MultiServerPropertyDefinedIn0",
          null));
    }
    if (pref.getVisibleAtClient() != null || pref.getVisibleAtOrganization() != null
        || pref.getVisibleAtRole() != null || pref.getUserContact() != null) {
      throw new OBException(OBMessageUtils.getI18NMessage("OBMOBC_MultiServerPropertyNoVisibility",
          null));
    }
    if (pref.getModule() == null && !pref.isSelected()) {
      throw new OBException(OBMessageUtils.getI18NMessage("OBMOBC_MultiServerPropertySelected",
          null));
    }
  }
}