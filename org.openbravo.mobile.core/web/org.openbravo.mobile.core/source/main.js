/*
 ************************************************************************************
 * Copyright (C) 2013-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 *
 * Author RAL
 *
 */

/*global OB, console*/

(function () {

  /**
   * catches application wide uncaught exceptions
   * this method should be overriden by client applications
   * do not change this method, instead create the new assignment at the client application starting point
   */
  window.onerror = function (message, url, line, column, errorObj) {
    if (!errorObj) {
      if (typeof (message) !== 'string') {
        return;
      }
    }
    var errorMessage;
    if (errorObj) {
      // the last 2 arguments may not be implemented by the browser
      errorMessage = errorObj.stack;
      console.log(line, column);
    } else {
      errorMessage = message + "; line: " + url + ":" + line;
    }
    // try to log the error in the backend
    if (OB.error) {
      if (OB.UTIL && OB.UTIL.showError) {
        OB.UTIL.showError(errorMessage);
      } else {
        // the OB.error is already being sent in the showError method
        OB.error(errorMessage);
      }
    } else {
      console.error(errorMessage);
    }
  };

  /**
   * Global versions for mobile.core
   */
  // Add WebSQL database version for mobile.core
  OB.UTIL.VersionManagement.current.mobileCore = {
    WebSQLDatabase: {
      name: 'OBMOBCDB',
      size: 4 * 1024 * 1024,
      displayName: 'Mobile DB'
    }
  };

  // This object lists the required and preferred browser versions by the Openbravo Mobile platform
  //
  // name: The identifier of the browsers as returned by enyo.platform
  // label: The browser name displayed to the user. Not localizable.
  // allowed: Browser version not supported and not recommended but the user is allowed to login.
  //     An strong and red warning message is displayed in the login page.
  //     Previous versions do not allow the user to login and a message informing the situation is displayed.
  // required: Browser version supported but not recommended: A red warning message is shown in the login window.
  // preferred: Recommended browser version. The normal login window is displayed.
  // 
  // To check the current broser name and version just execute in the javascript console: "enyo.platform"
  // This command returns the name of the browser and the version as expected by OB.UTIL.SupportedBrowsers
  OB.UTIL.SupportedBrowsers = [{
    name: 'chrome',
    label: 'Chrome',
    allowed: 30,
    required: 50,
    preferred: 51
  }, {
    name: 'androidChrome',
    label: 'Chrome for Android',
    allowed: 30,
    required: 50,
    preferred: 51
  }, {
    name: 'safari',
    label: 'Safari',
    allowed: 7,
    required: 8,
    preferred: 9
  }, {
    name: 'ios',
    label: 'Apple iOS',
    allowed: 7,
    required: 8,
    preferred: 9
  }];

  /**
   * Register active deprecations.
   * Deprecations must be registered before calling the 'deprecated' method
   */

  OB.UTIL.VersionManagement.registerDeprecation(27332, {
    year: 14,
    major: 4,
    minor: 0
  }, "'OB.UTILS' namespace has been removed");

  OB.UTIL.VersionManagement.registerDeprecation(27349, {
    year: 14,
    major: 4,
    minor: 0
  }, "'OB.MobileApp.model.hookManager' is a child of the model of the terminal. Please use 'OB.UTIL.HookManager' instead. More info: http://wiki.openbravo.com/wiki/How_to_Add_Hooks");

  OB.UTIL.VersionManagement.registerDeprecation(30610, {
    year: 15,
    major: 4,
    minor: 0
  }, "The 'init' method to call windows, has been deprecated. Use the new methods provided in 'OB.UI.WindowView' and 'OB.Model.WindowModel'");

  OB.UTIL.VersionManagement.registerDeprecation(30768, {
    year: 15,
    major: 4,
    minor: 0
  }, "The 'OB.Dal.get_uuid' method has been deprecated. Use the OB.UTIL.get_UUID method");

  OB.UTIL.VersionManagement.registerDeprecation(31753, {
    year: 16,
    major: 1,
    minor: 0
  }, "The old discounts flow has been deprecated. Please activate the new discounts flow");

  /**
   * Global deprecations have to be executed somewhere, this is a good place
   */

}());