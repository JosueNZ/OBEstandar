/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, _*/


(function () {
  OB.Cache = window.OB.Cache || {};

  function buildIdentifier(obj) {
    var identifier = '';
    delete obj.sql;

    // The case of object empty sometimes are passed a empty array and others ""
    // to workaround this, added this if to return "" for both cases
    if (Object.keys(obj).length > 0) {
      identifier = JSON.stringify(obj);
    }

    return identifier;
  }

  function saveObj(cacheName, paramObj, value) {
    OB.Cache.dataStructure[cacheName][buildIdentifier(paramObj)] = value;
  }

  OB.Cache.dataStructure = {};

  OB.Cache.initCache = function (cacheName, models) {
    if (!OB.Cache.dataStructure[cacheName]) {
      OB.Cache.dataStructure[cacheName] = {};
      if (models) {
        if (!OB.Cache.models) {
          OB.Cache.models = {};
        }
        _.forEach(models, function (modelName) {
          if (!OB.Cache.models[modelName]) {
            OB.Cache.models[modelName] = [];
          }
          OB.Cache.models[modelName].push(cacheName);
        });
      }
    }
  };

  OB.Cache.putItem = function (cacheName, paramObj, value, models) {
    OB.Cache.initCache(cacheName, models);
    saveObj(cacheName, paramObj, value);
  };

  OB.Cache.getItem = function (cacheName, paramObj, models) {
    OB.Cache.initCache(cacheName, models);
    return OB.Cache.dataStructure[cacheName][buildIdentifier(paramObj)];
  };

  OB.Cache.hasItem = function (cacheName, paramObj, models) {
    OB.Cache.initCache(cacheName, models);
    return !OB.UTIL.isNullOrUndefined(OB.Cache.getItem(cacheName, paramObj));
  };

  OB.Cache.resetCacheForModel = function (modelName) {
    if (OB.Cache && OB.Cache.models && OB.Cache.models[modelName]) {
      _.forEach(OB.Cache.models[modelName], function (cacheName) {
        OB.Cache.resetCache(cacheName);
      });
    }
  };

  OB.Cache.resetCache = function (cacheName) {
    if (OB.Cache.dataStructure[cacheName]) {
      OB.Cache.dataStructure[cacheName] = {};
    }
  };
}());