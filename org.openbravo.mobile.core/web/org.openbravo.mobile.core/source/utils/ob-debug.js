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

/*global isDebug, debugCauses, console, exports*/

/**
 * This class activates when StaticResourceComponentJava add the 'isDebug' variable to the javascript generated file
 * If a module is in development or the application is running the tests, add the 'isDebug' variable will be set to true
 *
 * This option is intended to run additional code (checks, etc) that will not be run while in production
 * This improves performance at the same time that the developer have a tool to improve stability
 */

// Initialize the global OB namespace
var OB = {};
window.OB = OB;

OB.UTIL = {};
OB.UTIL.Debug = {};

(function () {
  var root = this;
  var debug;
  if (typeof exports !== 'undefined') {
    OB.UTIL.Debug = exports;
  } else {
    OB.UTIL.Debug = root.OB.UTIL.Debug = {};
  }
  OB.UTIL.Debug.VERSION = "0.3";

  OB.UTIL.Debug.isDebug = function () {
    return (typeof isDebug !== 'undefined');
  };

  /**
   *
   * @return {}   an object with the causes that provoked the application to enter Debug mode
   */
  OB.UTIL.Debug.getDebugCauses = function () {
    if (OB.UTIL.Debug.isDebug()) {
      if (typeof debugCauses !== 'undefined') {
        return debugCauses;
      }
    }
    return null;
  };


  if (OB.UTIL.Debug.isDebug()) {
    console.warn("OB.UTIL.Debug is active (v" + OB.UTIL.Debug.VERSION + ")");
  }

  /**
   * Executes the callback if the 'isDebug' variable is set to true (inserted by the StaticResourceComponentJava.java class)
   * @param  {Function}   callback      the callback to be executed
   * @param  {object}     callerThis    if the 'this' variable needs to be passed
   * @return {}           nothing
   */
  OB.UTIL.Debug.execute = function (callback, callerThis) {
    if (OB.UTIL.Debug.isDebug()) {
      if (!callback) {
        console.error("You are calling OB.UTIL.Debug.execute without a callback");
        return;
      }
      callback.call(callerThis);
    }
  };

  /**
   * Verifies if the object is defined, if not, an exception is thrown
   * @param  {string}     object       object to be tested
   * @param  {string}     message      message to be shown if the object is undefined
   * @return {boolean}                 true if defined, false if undefined
   */
  OB.UTIL.Debug.isDefined = function (object, message) {
    if (OB.UTIL.Debug.isDebug()) {
      if (typeof object === 'undefined') {
        if (message) {
          throw message;
        } else {
          throw "'" + object.constructor.name + "' is undefined";
        }
      }
      return true;
    }
  };

  OB.UTIL.Debug.simulateOffline = function () {
    if (OB.UTIL.Debug.isDebug()) {
      OB.UTIL.localStorage.setItem('SimulateOffline', true);
    }
  };

  OB.UTIL.Debug.isSimulateOffline = function () {
    if (OB.UTIL.Debug.isDebug()) {
      return false;
    }
    var isFlag = OB.UTIL.localStorage.getItem('SimulateOffline');
    if (isFlag) {
      if (isFlag === 'true') {
        return true;
      }
    }
    return false;
  };

  // OB.UTIL.Debug.isTrue = function (condition, message) {
  //   if (OB.UTIL.Debug.isDebug()) {
  //     if (!OB.UTIL.Debug.isDefined(condition)) {
  //       return;
  //     }
  //     if (condition) {
  //       if (message) {
  //         console.error(message);
  //       } else {
  //         console.error("Object is undefined");
  //       }
  //     }
  //   }
  // };
}());