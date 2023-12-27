/*
 ************************************************************************************
 * Copyright (C) 2013-2014 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 *
 * Author RAL
 *
 */

/*global OB, console, exports */

/**
 * This class is intended for:
 * - version management
 * - easily maintain deprecated code
 *
 * TODO:
 * - please, check issue https://issues.openbravo.com/view.php?id=27400
 * - in the StaticResourceComponent.java, add code to transfer versions to this class
 * - add the mobile.core version
 * - add check to get is the version is up, equal or down the current version
 */

OB.UTIL = OB.UTIL || {};

(function () {
  var root = this;
  if (typeof exports !== 'undefined') {
    OB.UTIL.VersionManagement = exports;
  } else {
    OB.UTIL.VersionManagement = root.OB.UTIL.VersionManagement = {};
  }
  OB.UTIL.VersionManagement.VERSION = "0.2";
  // console.log("OB.UTIL.VersionManagement " + OB.UTIL.VersionManagement.VERSION + " is available");
  var registeredDeprecations = [];

  OB.UTIL.VersionManagement.buildVersionString = function (version) {
    if (version.minor !== 0) {
      return "RR" + version.year + "Q" + version.major + "." + version.minor;
    } else {
      return "RR" + version.year + "Q" + version.major;
    }
  };

  // Current version info
  OB.UTIL.VersionManagement.current = [];

  /**
   * TODO: return if the versionToCheck is the current or a higher version
   */
  // OB.UTIL.VersionManagement.isCurrentOrHigher = function (versionToCheck) {
  // };
  /**
   * A deprecation must be registered first and loaded when the javascript is loaded.
   * This provides a fast way to know all the deprecations in the code
   * @param  {int}      deprecationId           each deprecation must provide a unique id
   * @param  {version}  introducedInVersion     the version in which the deprecation is introduced
   * @param  {string}   inDevelopmentMessage    the message to show in development
   * @return {undefined}
   */
  OB.UTIL.VersionManagement.registerDeprecation = function (deprecationId, introducedInVersion, inDevelopmentMessage) {
    // in development argument checks
    OB.UTIL.Debug.isDefined(deprecationId, "Missing required argument 'deprecationId' in OB.UTIL.VersionManagement.registerDeprecation");
    OB.UTIL.Debug.isDefined(introducedInVersion, "Missing required argument 'introducedInVersion' in OB.UTIL.VersionManagement.registerDeprecation");
    OB.UTIL.Debug.isDefined(inDevelopmentMessage, "Missing required argument 'inDevelopmentMessage' in OB.UTIL.VersionManagement.registerDeprecation");
    OB.UTIL.Debug.execute(function () {
      var isAlreadyRegistered = registeredDeprecations.some(function (element) {
        if (element.deprecationId === deprecationId) {
          return true;
        }
      });
      if (isAlreadyRegistered) {
        console.error("The deprecation id: " + deprecationId + " ('" + inDevelopmentMessage + "'') have already been registered");
      }
    });

    // register the deprecation
    registeredDeprecations.push({
      deprecationId: deprecationId,
      introducedInVersion: introducedInVersion,
      inDevelopmentMessage: inDevelopmentMessage
    });
  };

  /**
   * Handles deprecated code.
   * The backwardCompatibleCallback will execute while in production
   * The inDevelopmentCallback will only be executed while in development
   * @param  {int}      deprecationId                 the deprecation id provided when registerDeprecation was call
   * @param  {callback} backwardCompatibleCallback    the callback that will always be executed
   * @param  {callback} inDevelopmentCallback         the callback to be executed only while in development. use warning to inform the developer
   * @param  {object}   callerThis                    the 'this' of the caller
   * @return {undefined}
   */
  OB.UTIL.VersionManagement.deprecated = function (deprecationId, backwardCompatibleCallback, inDevelopmentCallback, callerThis) {
    // in development argument checks
    OB.UTIL.Debug.isDefined(deprecationId, "Missing required argument 'deprecationId' in OB.UTIL.VersionManagement.deprecated");
    OB.UTIL.Debug.isDefined(backwardCompatibleCallback, "Missing required argument 'backwardCompatibleCallback' in OB.UTIL.VersionManagement.deprecated");

    // check if its already registered
    var registeredDeprecation;
    registeredDeprecations.some(function (element) {
      if (element.deprecationId === deprecationId) {
        registeredDeprecation = element;
        return;
      }
    });
    OB.UTIL.Debug.isDefined(registeredDeprecation, "The deprecation with id: " + deprecationId + " has not been registered with OB.UTIL.registerDeprecation");

    // execute the code that provides the backward compatilibily
    backwardCompatibleCallback.call(callerThis);

    // Execute the warning callback only in development. We don't want this warnings to reach production
    OB.UTIL.Debug.execute(function () {
      if (!inDevelopmentCallback) {
        inDevelopmentCallback = function (deprecationMessage) {
          console.warn(deprecationMessage);
        };
      }
      var deprecationMessage = "Deprecated (since " + OB.UTIL.VersionManagement.buildVersionString(registeredDeprecation.introducedInVersion) + "): " + registeredDeprecation.inDevelopmentMessage;
      inDevelopmentCallback.call(callerThis, deprecationMessage);
    });
  };

  /**
   * Shows in the console all the registered deprecations
   */
  OB.UTIL.VersionManagement.getDeprecations = function () {
    registeredDeprecations.forEach(function (element) {
      console.log(element.deprecationId + " (since " + OB.UTIL.VersionManagement.buildVersionString(element.introducedInVersion) + "): " + element.inDevelopmentMessage);
    });
  };

  /**
   * Verifies if a deprecation is present
   * @param  {int}       deprecationId      the deprecation id to be verified
   * @return {boolean}                      true if registered, false if not
   */
  OB.UTIL.VersionManagement.isDeprecationRegistered = function (deprecationId) {
    // argument check
    OB.UTIL.Debug.execute(function () {
      if (typeof (deprecationId) !== 'number') {
        console.error("The deprecationId ('" + deprecationId + "') must be an integer");
      }
    });
    var isDeprecationRegistered = false;
    registeredDeprecations.some(function (element) {
      if (element.deprecationId === deprecationId) {
        isDeprecationRegistered = true;
        return;
      }
    });
    return isDeprecationRegistered;
  };
}());