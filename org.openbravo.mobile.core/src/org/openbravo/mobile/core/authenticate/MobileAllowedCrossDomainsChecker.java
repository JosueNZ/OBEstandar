/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.authenticate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.jfree.util.Log;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.AllowedCrossDomainsHandler.AllowedCrossDomainsChecker;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.mobile.core.MobileServerDefinition;
import org.openbravo.mobile.core.servercontroller.MobileServerUtils;

/**
 * Reads the url of all servers and their allowed domains and creates one global list. The list is
 * reset when a mobile server definition changes.
 * 
 * @author mtaal
 */
@ApplicationScoped
public class MobileAllowedCrossDomainsChecker extends AllowedCrossDomainsChecker {

  private static final Logger log = Logger.getLogger(MobileAllowedCrossDomainsChecker.class);

  // on purpose static to let it be nullified by eventhandler
  private static Collection<String> domainRegExList = null;
  private static Set<String> allowedDomains = new HashSet<String>();

  @Override
  public boolean isAllowedOrigin(HttpServletRequest request, String origin) {
    if (domainRegExList == null) {
      buildDomainList();
    }

    if (allowedDomains.contains(origin)) {
      return true;
    }

    if (domainRegExList.isEmpty()) {
      return false;
    }

    for (String domainRegEx : domainRegExList) {
      // matches is 5 times slower than precompile regex
      // but as we cache allowed domains anyway (see allowedDomains set)
      // there is less need to optimize
      if (origin.matches(domainRegEx)) {
        allowedDomains.add(origin);
        return true;
      }
    }

    log.error("Origin " + origin + " is not allowed, request information: "
        + request.getRequestURL() + "-" + request.getQueryString());

    return false;
  }

  private synchronized void buildDomainList() {
    allowedDomains = new HashSet<String>();

    if (!MobileServerUtils.isMultiServerEnabled()) {
      domainRegExList = new ArrayList<String>();
      return;
    }

    final Collection<String> localDomainList = new ArrayList<String>();
    try {
      OBContext.setAdminMode(true);
      final OBQuery<MobileServerDefinition> qry = OBDal.getInstance().createQuery(
          MobileServerDefinition.class, "");
      // on purpose reading all clients as this is a singleton which servers all clients
      qry.setFilterOnReadableOrganization(false);
      qry.setFilterOnReadableClients(false);
      for (MobileServerDefinition serverDefinition : qry.list()) {
        localDomainList.addAll(getAllowedDomains(serverDefinition));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    domainRegExList = localDomainList;
  }

  private List<String> getAllowedDomains(MobileServerDefinition serverDefinition) {
    final List<String> result = new ArrayList<String>();
    if (serverDefinition.getURL() != null) {
      result.add(getAllowedOriginExp(serverDefinition.getURL()));
    }
    if (serverDefinition.getAlloweddomains() != null) {
      String tmpDomains = serverDefinition.getAlloweddomains();
      // support line breaks instead of commas, support all the platforms
      // as the browser may run in different OS on the client side
      tmpDomains = tmpDomains.replace("\r\n", ",");
      tmpDomains = tmpDomains.replace("\n", ",");
      tmpDomains = tmpDomains.replace("\r", ",");
      // all the separators are now commas, split on it
      final String[] domains = tmpDomains.split(",");
      for (String domain : domains) {
        String regEx = getAllowedOriginExp(domain.trim());
        // test the regEx
        try {
          Pattern.compile(regEx);
          result.add(regEx);
        } catch (Exception logIt) {
          Log.error("Reg ex " + regEx + " does not compile " + logIt.getMessage(), logIt);
        }
      }
    }
    return result;
  }

  private String getAllowedOriginExp(String url) {
    // note url can contain dots, but on purpose not escaping those
    // to allow flexibility, if the user wants explicit dots he/she
    // should escape them

    String localUrl = url.toLowerCase();
    int nextIndex = 2 + localUrl.indexOf("//");
    if (localUrl.indexOf("/", nextIndex) == -1) {
      return localUrl;
    }
    localUrl = localUrl.substring(0, localUrl.indexOf("/", nextIndex));
    return localUrl;
  }

  public static class MobileServerDefinitionEventHandler extends EntityPersistenceEventObserver {

    private static Entity[] entities = { ModelProvider.getInstance().getEntity(
        MobileServerDefinition.ENTITY_NAME) };
    protected Logger logger = Logger.getLogger(this.getClass());

    @Override
    protected Entity[] getObservedEntities() {
      return entities;
    }

    public void onSave(@Observes EntityNewEvent event) {
      if (!isValidEvent(event)) {
        return;
      }
      domainRegExList = null;
    }

    public void onUpdate(@Observes EntityUpdateEvent event) {
      if (!isValidEvent(event)) {
        return;
      }
      domainRegExList = null;
    }

    public void onDelete(@Observes EntityDeleteEvent event) {
      if (!isValidEvent(event)) {
        return;
      }
      domainRegExList = null;
    }
  }
}
