/*
 ************************************************************************************
 * Copyright (C) 2015-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.authenticate;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.system.Client;

/**
 * Utilities for working with authentication using encrypted keys.
 * 
 * The encryption uses AES which is symmetric encryption where both sides (encrypt and decrypt) know
 * the shared key. This is faster and also simpler than RSA (asymmetric) and in our case both sides
 * have access to the same key as they get it from the database.
 * 
 * New tokens are only send back on login action and specific GetToken requests to a server on which
 * the user logged in previously with a valid login/password. So new tokens are not to send back on
 * requests from a server on which the user logged in using a token. A token can only be refreshed
 * from a server on which the user authenticated.
 * 
 * This combined with token expiry means that anyone 'taking' a token only has a limited time frame
 * before the token becomes invalid and he/she can only get a new token from a newly logged in
 * client.
 * 
 * To work in a test environment set the authentication.test.key property in Openbravo.properties,
 * for example: authentication.test.key = uMbsk0ZYPS43uUY6D1KU3A==
 * 
 * @author mtaal
 */
public class MobileAuthenticationKeyUtils {
  private static final Logger log = Logger.getLogger(MobileAuthenticationKeyUtils.class);

  private static final int DEFAULT_MAX_TOKEN_AGE = 12 * 60 * 60 * 1000;

  public static final String AUTHENTICATION_TOKEN_PARAM = "authenticationToken";
  public static final String AUTHENTICATION_CLIENT_PARAM = "authenticationClient";
  public static final String AUTHENTICATED_BY_TOKEN_SESSION_PARAM = "#Authenticated_by_Token";
  public static final String AUTHENTICATION_KEY_PROPERTY = "authentication.test.key";
  public static final String USE_USER_CACHE_PARAMETER = "useUserCache";

  private static final String IV_SEPARATOR = "__;__";

  // prevent a key which was accidentally exported in sample data to
  // be used
  // https://issues.openbravo.com/view.php?id=32284
  private static final String INVALID_KEY = "Siuh6xr7DcqSvfx3TVdewA==";

  private static int maxTokenAge = -1;

  public static String getEncryptedAuthenticationToken(String clientId, String orgId,
      String userId, String roleId, String terminalId) {
    final AuthenticationToken authenticationToken = new AuthenticationToken(clientId, orgId,
        userId, roleId, terminalId);
    final String encryptedToken = encrypt(authenticationToken);
    return encryptedToken;
  }

  public static String encrypt(AuthenticationToken authenticationToken) {
    if ("0".equals(authenticationToken.clientId)) {
      throw new OBException("Authentication token generation not supported for client zero "
          + authenticationToken);
    }
    try {
      // get the key
      final byte[] byteKeys = Base64.decodeBase64(getKey(authenticationToken.clientId));
      final SecretKey key = new SecretKeySpec(byteKeys, 0, byteKeys.length, "AES");

      // get and fill the iv
      // note iv is not strictly needed but it is safer
      final byte[] iv = new byte[16];
      new SecureRandom().nextBytes(iv);

      // the iv will be prepended to the token, so make a string
      // this is okay to publically share/show the iv
      final String ivKey = Base64.encodeBase64String(iv);

      // http://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html
      final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

      // encrypt
      final byte[] encryptedBytes = cipher.doFinal(authenticationToken.getToken().getBytes());
      final String encryptedString = Base64.encodeBase64String(encryptedBytes);

      return ivKey + IV_SEPARATOR + encryptedString;
    } catch (Exception e) {
      throw new OBException("Exception when encrypting " + authenticationToken, e);
    }
  }

  private static String getKey(String clientId) {

    if (OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty(AUTHENTICATION_KEY_PROPERTY) != null) {

      // log a warning and don't use if not in a test environment
      if (!"true".equals(OBPropertiesProvider.getInstance().getOpenbravoProperties()
          .getProperty("test.environment"))) {
        log.warn("Test authentication key is set but the system is not set as a test system, also set the test.environment property in Openbravo.properties to the value true");
      } else {
        log.info("Using test authentication key from property file");
        return OBPropertiesProvider.getInstance().getOpenbravoProperties()
            .getProperty(AUTHENTICATION_KEY_PROPERTY);
      }
    }

    OBDal.getInstance().flush();
    OBContext.setAdminMode(false);
    try {
      final Client client = OBDal.getInstance().get(Client.class, clientId);
      if (client.getObmobcAuthKey() != null && !INVALID_KEY.equals(client.getObmobcAuthKey())) {
        return client.getObmobcAuthKey();
      }
      // generate and store the key:
      final String strKey = generateKey();
      client.setObmobcAuthKey(strKey);
      return strKey;
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      try {
        OBDal.getInstance().flush();
      } finally {
        OBContext.restorePreviousMode();
      }
    }
  }

  /**
   * Method to generate the mobile server authentication key using the AES KeyGenerator
   * 
   * @return a new authentication key
   */
  public static String generateKey() {
    final int AES_KEYLENGTH = 128;
    String strKey = "";
    KeyGenerator keyGen;
    try {
      keyGen = KeyGenerator.getInstance("AES");
      keyGen.init(AES_KEYLENGTH);
      final SecretKey secretKey = keyGen.generateKey();
      strKey = Base64.encodeBase64String(secretKey.getEncoded());
    } catch (NoSuchAlgorithmException e) {
      log.error(e);
    }
    return strKey;
  }

  public static AuthenticationToken decrypt(String clientId, String encryptedToken) {
    try {
      final int ivIndex = encryptedToken.indexOf(IV_SEPARATOR);
      if (ivIndex == -1) {
        throw new AuthenticationException("Invalid token");
      }
      final String ivStr = encryptedToken.substring(0, ivIndex);
      final byte[] iv = Base64.decodeBase64(ivStr);
      final String encryptedString = encryptedToken.substring(ivIndex + IV_SEPARATOR.length());
      final byte[] encryptedBytes = Base64.decodeBase64(encryptedString);

      // get the key
      final byte[] byteKeys = Base64.decodeBase64(getKey(clientId));
      final SecretKey key = new SecretKeySpec(byteKeys, 0, byteKeys.length, "AES");

      final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
      cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

      final byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
      final String decryptedString = new String(decryptedBytes);

      final AuthenticationToken token = new AuthenticationToken(decryptedString);
      if ((System.currentTimeMillis() - token.timestamp) > getMaxTokenAge()) {
        throw new AuthenticationException("Invalid token " + encryptedToken);
      }

      return new AuthenticationToken(decryptedString);
    } catch (GeneralSecurityException se) {
      throw new AuthenticationException("Invalid token " + encryptedToken, se);
    } catch (AuthenticationException aex) {
      throw aex;
    } catch (Exception e) {
      throw new OBException("Exception when decrypting " + encryptedToken, e);
    }
  }

  private static int getMaxTokenAge() {
    if (maxTokenAge == -1) {
      int value = DEFAULT_MAX_TOKEN_AGE;
      try {
        value = Integer.parseInt(Preferences.getPreferenceValue("OBMOBC_AuthenticationTokenMaxAge",
            true, null, null, null, null, (String) null));
      } catch (Throwable ignore) {
      }
      maxTokenAge = value;
    }
    return maxTokenAge;
  }

  public static class AuthenticationToken {
    private final static String SEPARATOR = "_;_";

    public final String clientId;
    public final String orgId;
    public final String userId;
    public final String roleId;
    public final String terminalId;
    public final long timestamp;

    public AuthenticationToken(String clientId, String orgId, String userId, String roleId,
        String terminalId) {
      this.clientId = clientId;
      this.userId = userId;
      this.timestamp = System.currentTimeMillis();
      this.roleId = roleId;
      this.orgId = orgId;
      this.terminalId = terminalId;
    }

    public AuthenticationToken(String token) {
      this.clientId = getFrom(token, 0);
      this.orgId = getFrom(token, 1);
      this.userId = getFrom(token, 2);
      this.timestamp = Long.parseLong(getFrom(token, 3));
      this.roleId = getFrom(token, 4);
      this.terminalId = getFrom(token, 5);
    }

    public String getUserId() {
      return userId;
    }

    private String getFrom(String token, int position) {
      final String[] parts = token.split(SEPARATOR);
      return parts[position];
    }

    public String getToken() {
      return clientId + SEPARATOR + orgId + SEPARATOR + userId + SEPARATOR + timestamp + SEPARATOR
          + roleId + SEPARATOR + terminalId;
    }

    public String toString() {
      return getToken();
    }

    public String getOrgId() {
      return orgId;
    }

    public String getRoleId() {
      return roleId;
    }

    public String getClientId() {
      return clientId;
    }

    public String getTerminalId() {
      return terminalId;
    }
  }
}
