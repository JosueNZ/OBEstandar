/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, localStorage, _ */

(function () {
  OB.UTIL = window.OB.UTIL || {};
  OB.UTIL.localStorage = window.OB.UTIL.localStorage || {};

  OB.UTIL.localStorage.getAppName = function () {
    if (OB.MobileApp && OB.MobileApp.model) {
      return OB.MobileApp.model.get('appName');
    } else {
      return null;
    }
  };

  OB.UTIL.localStorage.getItem = function (key) {
    var appName = OB.UTIL.localStorage.getAppName();
    var value;

    if (!_.isNull(appName)) {
      value = window.localStorage.getItem(appName + '.' + key);
    }

    if (_.isNull(appName) || _.isNull(value) || _.isUndefined(value)) {
      value = window.localStorage.getItem(key);
    }
    return value;
  };

  OB.UTIL.localStorage.setItem = function (key, value) {
    var appName = OB.UTIL.localStorage.getAppName();
    if (!_.isNull(appName)) {
      window.localStorage.setItem(appName + '.' + key, value);
    } else {
      window.localStorage.setItem(key, value);
    }
  };

  OB.UTIL.localStorage.removeItem = function (key) {
    var appName = OB.UTIL.localStorage.getAppName();
    if (!_.isNull(appName)) {
      window.localStorage.removeItem(appName + '.' + key);
    } else {
      window.localStorage.removeItem(key);
    }
  };

  OB.UTIL.localStorage.clear = function () {
    window.localStorage.clear();
  };
}());