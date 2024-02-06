/*
 ************************************************************************************
 * Copyright (C) 2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.servercontroller;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Event Handler that controls that there is only one server request that references the
 * ServerStateBackground process, and that is the one created in StatusBackgroundProcessScheduler
 *
 */
public class ServerStateBackgroundEventHandler extends EntityPersistenceEventObserver {

  private static final String SERVER_STATE_BACKGROUND_PROCESS_ID = "A8D3265435F94CCFB3AF14437E5FAD1F";
  private static final String OFFICIAL_PROCESS_REQUEST_ID = "9067AECD3B044552BDFFA0CF9056B2C9";
  private static final String IMMEDIATE_TIMING_OPTON = "I";

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(
      ProcessRequest.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onCreate(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ProcessRequest processRequest = (ProcessRequest) event.getTargetInstance();
    // don't allow creating new process requests that reference the server state background process
    if (processRequest.getProcess() != null
        && processRequest.getProcess().getId().equals(SERVER_STATE_BACKGROUND_PROCESS_ID)
        && !processRequest.getId().equals(OFFICIAL_PROCESS_REQUEST_ID)
        && !IMMEDIATE_TIMING_OPTON.equals(processRequest.getTiming())) {
      throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
          "OBMOBC_OnlyOneServerStateProcessRequest", OBContext.getOBContext().getLanguage()
              .getLanguage()));
    }

  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final ProcessRequest processRequest = (ProcessRequest) event.getTargetInstance();
    // don't allow deleting the process request created in StatusBackgroundProcessScheduler
    if (processRequest.getId().equals(OFFICIAL_PROCESS_REQUEST_ID)) {
      throw new OBException(Utility.messageBD(new DalConnectionProvider(false),
          "OBMOBC_OfficialServerStateProcessRequestCannotBeDeleted", OBContext.getOBContext()
              .getLanguage().getLanguage()));
    }
  }
}
