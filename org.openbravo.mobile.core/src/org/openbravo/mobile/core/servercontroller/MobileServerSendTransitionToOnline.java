/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.servercontroller;

import java.util.Map;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.MobileServerDefinition;

/**
 * Sends a request to the mobile server to force it to transition to online.
 * 
 * @author Martin Taal
 */
public class MobileServerSendTransitionToOnline extends BaseProcessActionHandler {

  private static final Logger log = Logger.getLogger(MobileServerSendTransitionToOnline.class);

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    try {
      final JSONObject contentJSON = new JSONObject(content);
      final String serverId = contentJSON.getString("inpobmobcServerDefinitionId");
      final MobileServerDefinition serverDefinition = OBDal.getInstance()
          .get(MobileServerDefinition.class, serverId);
      final JSONObject result = MobileServerRequestExecutor.getInstance().executeServerRequest(
          serverDefinition.getMobileServerKey(),
          MobileServerUtils.OBWSPATH + ForceTransitionToOnline.class.getName(), new JSONObject());
      if (result.has("serverStatus")) {
        serverDefinition.setStatus(result.getString("serverStatus"));
      }
      return result;
    } catch (Exception e) {
      log.error(e.getMessage() + content, e);
    }
    return new JSONObject();
  }
}
