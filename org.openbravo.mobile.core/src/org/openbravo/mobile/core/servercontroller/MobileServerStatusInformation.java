/*
 ************************************************************************************
 * Copyright (C) 2015-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Query;
import org.hibernate.Session;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.mobile.core.process.JSONRowConverter;
import org.openbravo.mobile.core.process.ProcessHQLQuery;

@AuthenticationManager.Stateless
public class MobileServerStatusInformation extends ProcessHQLQuery {

  private static final Logger log = Logger.getLogger(MobileServerStatusInformation.class);

  @Override
  public void exec(Writer w, JSONObject jsonsent) throws IOException, ServletException {
    try {
      OBContext.setAdminMode(false);

      MobileServerUtils.updateServerStatusFromJSON(jsonsent);

      final Session session = OBDal.getInstance().getSession();
      final String hql = "select status from OBMOBC_SERVER_DEFINITION where id=:serverId";
      final Query query = session.createQuery(hql);
      query.setString("serverId", MobileServerController.getInstance().getThisServerDefinitionId());
      final Object qryResult = query.uniqueResult();
      updateLastPingAndTransitionToOnlineIfNeeded(session);
      if (qryResult != null) {
        w.write("\"serverStatus\": \"" + qryResult + "\"");
      }
    } catch (Throwable t) {
      log.error(t.getMessage(), t);
      JSONRowConverter.addJSONExceptionFields(w, t);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void updateLastPingAndTransitionToOnlineIfNeeded(Session session) {
    Date lastPingFromCentral = ServerStateBackground.getLastPingFromCentral();
    Date lastPingThreshold = ServerStateBackground.getLastPingThreshold();
    boolean lastPingFromCentralWasExpired = lastPingFromCentral.before(lastPingThreshold);

    final Query query = session
        .createQuery("UPDATE OBMOBC_SERVER_DEFINITION SET lastPing=now() WHERE servertype = 'MAIN'");
    query.executeUpdate();

    if (lastPingFromCentralWasExpired) {
      log.info("Store offline, new Ping received from central server after >3 pings missed. Transitioning to online.");
      // if the last ping from the central server was received a long time ago that probably means
      // the central server has been offline, try to make the store server transition to online
      MobileServerController.getInstance().forceTransitionToOnline();
    }
  }

  @Override
  protected List<String> getQuery(JSONObject jsonsent) throws JSONException {

    List<String> hqls = new ArrayList<>();
    String hql = "Select status as serverStatus from OBMOBC_SERVER_DEFINITION where id='"
        + MobileServerController.getInstance().getThisServerDefinitionId() + "'";
    hqls.add(hql);
    return hqls;
  }

  @Override
  protected boolean bypassSecurity() {
    return true;
  }

}
