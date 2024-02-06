/*
 ************************************************************************************
 * Copyright (C) 2015-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.servercontroller;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.mobile.core.MobileServerDefinition;
import org.openbravo.mobile.core.MobileServerService;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Class which should be used to execute all inter-server requests. It triggers offline behavior if a server can not be reached.
 * 
 * @author mtaal
 */
public class MobileServerRequestExecutor {

  private static final String COOKIE_SESSION_VAR = "MS_SESSION_COOKIE";
  private static final String JSESSIONID_PARAM = "JSESSIONID";

  private final static Logger log = Logger.getLogger(MobileServerRequestExecutor.class);

  private static MobileServerRequestExecutor instance = new MobileServerRequestExecutor();

  public static MobileServerRequestExecutor getInstance() {
    return instance;
  }

  public static void setInstance(MobileServerRequestExecutor instance) {
    MobileServerRequestExecutor.instance = instance;
  }

  private Integer maxRetries = null;
  private Integer connectionTimeout = null;
  private Integer readTimeout = null;
  private Boolean readTimeoutForcesOffline = null;

  private HttpURLConnection createConnection(String serverKey, String serverUrl, String wsPart,
      String method, JSONObject parameters) throws Exception {
    String localServerUrl = serverUrl;
    // TODO: maybe handle this with a validation on the formfield
    if (!localServerUrl.toLowerCase().startsWith("http://")
        && !localServerUrl.toLowerCase().startsWith("https://")) {
      localServerUrl = "https://" + localServerUrl;
    }
    final URL url = new URL(localServerUrl + wsPart);
    final HttpURLConnection hc = (HttpURLConnection) url.openConnection();
    hc.setRequestMethod(method);
    hc.setAllowUserInteraction(false);
    hc.setDefaultUseCaches(false);
    hc.setDoOutput(true);
    hc.setConnectTimeout(getConnectionTimeout());
    hc.setReadTimeout(getReadTimeout());
    hc.setDoInput(true);
    hc.setInstanceFollowRedirects(true);
    hc.setUseCaches(false);
    hc.setRequestProperty("Content-Type", "application/json;charset=utf-8");
    setSessionCookie(serverKey, hc);
    OutputStream os = new BufferedOutputStream(hc.getOutputStream());
    os.write(parameters.toString().getBytes());
    os.flush();
    return hc;
  }

  /**
   * Performs a request on a specific server, does not do state handling or finding a server based on the service name. Just calls
   * the specified server with authentication information.
   */
  public String request(MobileServerDefinition server, String serviceName,
      String authenticationParams, String method, JSONObject parameters) throws Exception {

    String wsPart = serviceName;
    // add authentication parameters
    if (MobileServerUtils.isOpenbravoServer(server)) {
      wsPart = wsPart
          + (authenticationParams == null ? "" : (serviceName.contains("?") ? "&" : "?")
              + authenticationParams);
    }

    final HttpURLConnection hc = createConnection(server.getMobileServerKey(), server.getURL(),
        wsPart, method, parameters);
    long logTimeStamp = System.currentTimeMillis();
    if (log.isDebugEnabled()) {
      log.debug("Executing request (" + logTimeStamp + ") " + hc.getURL() + " - " + method + " - "
          + parameters);
    }
    hc.connect();

    final InputStream is = hc.getInputStream();
    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

    final StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line).append("\n");
    }
    if (log.isDebugEnabled()) {
      log.debug("Received response (" + logTimeStamp + ") " + sb.toString());
    }

    // set the session cookie after trying to get the data, as then connection
    // failures are shown earlier
    storeSessionCookie(server.getMobileServerKey(), hc);

    return sb.toString();
  }

  // for docs: http://www.hccp.org/java-net-cookie-how-to.html
  private void setSessionCookie(String serverKey, HttpURLConnection hc) {
    if (RequestContext.get().getRequest() == null) {
      return;
    }
    final String cookie = (String) RequestContext.get().getSessionAttribute(
        serverKey + "-" + COOKIE_SESSION_VAR);
    if (cookie != null) {
      hc.setRequestProperty("Cookie", cookie);
    }
  }

  private void storeSessionCookie(String serverKey, HttpURLConnection hc) {
    if (RequestContext.get().getRequest() == null) {
      return;
    }
    final List<String> value = hc.getHeaderFields().get("Set-Cookie");
    if (value == null) {
      return;
    }
    final String cookies = value.get(0);
    if (cookies.toUpperCase().contains(JSESSIONID_PARAM)) {
      int start = cookies.toUpperCase().indexOf(JSESSIONID_PARAM);
      int end = cookies.indexOf(";", start + 1);
      // store the session cookie by serverkey
      RequestContext.get().setSessionAttribute(serverKey + "-" + COOKIE_SESSION_VAR,
          cookies.substring(start, end));
    }
  }

  /**
   * Executes a request (the service with serviceName) on the server passing the parameters. The tries parameter keeps track on
   * how many calls have been tried.
   * 
   * The maximum number of tries is defined by a preference. If after these number of tries the server still responds with an
   * error then the tryConnection will stop and return any empty jsonobject.
   */
  private JSONObject tryConnection(MobileServerDefinition server, String serviceName,
      JSONObject parameters) {
    int tries = 0;
    boolean readTimeoutOccurred = false;
    
    JSONObject resp = new JSONObject();
    while (tries <= getMaxRetries()) {
      readTimeoutOccurred = false;
      try {
        if (tries > 0) {
          // when retrying then
          // wait a short time to give network/others to recover from internal
          // issues
          Thread.sleep(50);
        }
        tries++;
        final String result = request(server, serviceName,
            MobileServerUtils.getAuthenticationQueryParams(), "POST", parameters);
        resp = new JSONObject(result).getJSONObject("response");
        if (resp.has("serverStatus")) {
          final String respStatus = resp.getString("serverStatus");
          if (!server.getStatus().equals(respStatus)) {
            server.setStatus(resp.getString("serverStatus"));
            OBDal.getInstance().save(server);
            if (MobileServerUtils.isTriggerStateServer(server)
                && respStatus.equals(MobileServerState.OFFLINE.getValue())) {
              log.info("Central server seems to be offline, starting transition to offline");
              MobileServerController.getInstance().transitionToOffline();
            }
          }
        }
        return resp;
      } catch (Throwable t) {
        // note that in case the other server takes a really long time in processing
        // a request and when we retry it then the other server can detect that the
        // request is already being processed (this depends on where the processing is stuck).
        // The second and later tries will then
        // not timeout but will return a response like 'please wait for the results'.
        // this means that a readtimeout in this case will never get to go-offline code
        // below. This is fine for most cases anyway.
        readTimeoutOccurred = "Read timed out".equals(t.getMessage());
        log.error("Trying connection to " + server.getMobileServerKey() + " " + serviceName
            + " json: " + parameters, t);
        
      }
    }
    if (!readTimeoutOccurred || doesReadTimeoutForceOffline()) {
      server.setStatus(MobileServerState.OFFLINE.getValue());
      OBDal.getInstance().save(server);
      if (MobileServerUtils.isTriggerStateServer(server)) {
        MobileServerController.getInstance().transitionToOffline();
      }
    }
    return resp;
  }

  private Integer getConnectionTimeout() {
    if (connectionTimeout == null) {
      connectionTimeout = MobileServerUtils.getIntegerPreference(
          "OBMOBC_CONNECTION_TIMEOUT_REQUEST", 5) * 1000;
    }
    return connectionTimeout;
  }

  private Integer getReadTimeout() {
    if (readTimeout == null) {
      readTimeout = MobileServerUtils.getIntegerPreference("OBMOBC_READ_TIMEOUT_REQUEST", 15) * 60 * 1000;
    }
    return readTimeout;
  }

  private Integer getMaxRetries() {
    if (maxRetries == null) {
      maxRetries = MobileServerUtils.getIntegerPreference("OBMOBC_NumberOfRequestRetries", 3);
      if (maxRetries == 0) {
        log.error("Preference OBMOBC_NumberOfRequestRetries is set to zero, this is incorrect, using value 1 as minimum value now.");
        maxRetries = 1;
      }
    }
    return maxRetries;
  }

  private Boolean doesReadTimeoutForceOffline() {
    if (readTimeoutForcesOffline == null) {
      readTimeoutForcesOffline = MobileServerUtils.getBooleanPreference(
          "OBMOBC_ReadTimeoutForcesOffline", false);
    }
    return readTimeoutForcesOffline;
  }

  /**
   * Call a service directly on the server (defined by the server key) without checking if the server supports the service.
   * 
   * This method performs so-called store server state handling (through the internal/private tryConnection method). If the
   * calling server can not be reached then this server can decide to go offline this depends on the
   * {@link MobileServerUtils#isTriggerStateServer(MobileServerDefinition)} value of the called server.
   */
  public JSONObject executeServerRequest(String serverKey, String serviceName, JSONObject parameters) {
    try {
      OBContext.setAdminMode(false);
      final MobileServerDefinition otherServer = MobileServerController.getInstance()
          .getMobileServer(serverKey);
      final MobileServerDefinition thisServerDef = MobileServerController.getInstance()
          .getThisServerDefinition();
      parameters.put("mobileServerKey", thisServerDef.getMobileServerKey());
      parameters.put("serverStatus", thisServerDef.getStatus());

      JSONObject resp = tryConnection(otherServer, serviceName, parameters);
      if (resp.length() != 0) {
        handleOtherServerStatus(otherServer, resp);
        return resp;
      } else {
        throw new OBException("No result obtained from " + otherServer + " for service "
            + serviceName);
      }
    } catch (JSONException e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Call a service directly on the central server without checking if the central server supports the service.
   * 
   * This method performs so-called store server state handling (through the internal/private tryConnection method). If the
   * calling server can not be reached then this server can decide to go offline this depends on the
   * {@link MobileServerUtils#isTriggerStateServer(MobileServerDefinition)} value of the called server.
   */
  public JSONObject executeCentralRequest(String serviceName, JSONObject parameters) {
    return executeServerRequest(MobileServerController.getInstance().getCentralServerKey(),
        serviceName, parameters);
  }

  /**
   * Finds a server which can execute the service. If found calls that server.
   * 
   * This method performs so-called store server state handling (through the internal/private tryConnection method). If the
   * calling server can not be reached then this server can decide to go offline this depends on the
   * {@link MobileServerUtils#isTriggerStateServer(MobileServerDefinition)} value of the called server.
   */
  public JSONObject executeRequest(String webserviceSegment, JSONObject parameters) {
    try {
      OBContext.setAdminMode(false);
      MobileServerDefinition server = null;

      // Find a server which can execute the service
      OBQuery<MobileServerDefinition> servers = OBDal.getInstance().createQuery(
          MobileServerDefinition.class,
          "(" + MobileServerDefinition.PROPERTY_ALLORGS + "=true or :org in elements("
              + MobileServerDefinition.PROPERTY_OBMOBCSERVERORGSLIST
              + "))  and client.id=:clientId and " + MobileServerDefinition.PROPERTY_ACTIVE
              + "=true and " + MobileServerDefinition.PROPERTY_MOBILESERVERKEY + "!='"
              + MobileServerController.getInstance().getMobileServerKey() + "' order by "
              + MobileServerDefinition.PROPERTY_PRIORITY);
      servers.setFilterOnReadableClients(false);
      servers.setFilterOnReadableOrganization(false);
      servers.setNamedParameter("org",
          OBDal.getInstance().getProxy(Organization.class, parameters.getString("organization")));

      if (!parameters.has("client")) {
        final Organization org = OBDal.getInstance().get(Organization.class,
            parameters.getString("organization"));
        parameters.put("client", org.getClient().getId());
      }
      servers.setNamedParameter("clientId", parameters.getString("client"));

      for (MobileServerDefinition srv : servers.list()) {
        // only call online servers
        if (!MobileServerState.ONLINE.getValue().equals(srv.getStatus())) {
          continue;
        }
        server = null;
        if (srv.isAllservices()) {
          server = srv;
        } else {
          for (MobileServerService service : srv.getOBMOBCSERVERSERVICESList()) {
            final String serviceIdentifier = service.getObmobcServices().getService();
            if (webserviceSegment.contains(serviceIdentifier)) {
              server = srv;
            }
          }
        }

        // found a server, call it
        if (server != null) {
          parameters.put("mobileServerKey", MobileServerController.getInstance()
              .getMobileServerKey());
          parameters.put("serverStatus", MobileServerController.getInstance()
              .getThisServerDefinition().getStatus());

          JSONObject resp = tryConnection(server, webserviceSegment, parameters);
          if (resp.length() != 0) {
            handleOtherServerStatus(server, resp);
            return resp;
          }
          // continue with the next server until all have been
        }
      }
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
    }
    throw new OBException("No server found for service " + webserviceSegment);
  }

  private void handleOtherServerStatus(MobileServerDefinition otherServer, JSONObject resp)
      throws JSONException {

    // other server is not online
    if (targetServerIsNotOnline(resp)) {

      otherServer.setStatus(resp.getString("serverStatus"));
      OBDal.getInstance().save(otherServer);

      // code in transitionToOffline checks if current status is online
      if (MobileServerUtils.isTriggerStateServer(otherServer)) {
        log.info("Central server seems to be offline, starting a transition to offline");
        MobileServerController.getInstance().transitionToOffline();
      }
      return;
    }

    // rest is for other server is online code

    // update other server
    if (!cachedTargetServerVersionIsOnline(otherServer)) {
      otherServer.setStatus(MobileServerState.ONLINE.getValue());
      OBDal.getInstance().save(otherServer);
    }

    // if not a state changer, then go away
    if (!MobileServerUtils.isTriggerStateServer(otherServer)) {
      return;
    }

    if (MobileServerController.getInstance().isThisAStoreServer()) {
      // if offline-->online, then trigger an online transition
      MobileServerController.getInstance().transitionToOnline();
    }
  }

  private boolean cachedTargetServerVersionIsOnline(MobileServerDefinition otherServer) {
    return otherServer.getStatus().equals(MobileServerState.ONLINE.getValue());
  }

  private boolean targetServerIsNotOnline(JSONObject resp) throws JSONException {
    return resp.has("serverStatus")
        && !resp.get("serverStatus").equals(MobileServerState.ONLINE.getValue());
  }
}
