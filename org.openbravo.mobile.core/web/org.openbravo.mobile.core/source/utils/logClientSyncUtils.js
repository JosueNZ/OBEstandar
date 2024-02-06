/*
 ************************************************************************************
 * Copyright (C) 2012-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, console, _, Backbone*/

(function () {

  OB.UTIL = window.OB.UTIL || {};

  OB.UTIL.activateLogClient = function () {
    if (!OB.UTIL.logClientIntervalId) {
      OB.UTIL.logClientIntervalId = setInterval(OB.UTIL.processLogClientAll, OB.MobileApp.model.hasPermission('OBMOBC_LogClientInterval', true) * 1000);
    }
  };

  OB.UTIL.stopLogClient = function () {
    if (OB.UTIL.logClientIntervalId) {
      clearInterval(OB.UTIL.logClientIntervalId);
      OB.UTIL.logClientIntervalId = null;
    }
  };

  OB.UTIL.processLogClientAll = function () {
    // if the application is busy, delay flushing the log to the server
    if (!OB.MobileApp.model.loadingErrorsActions.executed) {
      if (OB.MobileApp.model.get('isLoggingIn') || !OB.UTIL.SynchronizationHelper.isSynchronized()) {
        OB.debug("LogClient flushing delayed");
        return false;
      }
    }

    // Processes log client
    if (OB.MobileApp.model.get('connectedToERP') && !OB.MobileApp.model.get('sessionLost')) {
      // batch the queue. be sure that we pick manageable numbers
      var criteria = {
        _limit: !OB.MobileApp.model.hasPermission('OBMOBC_LogClientMaxBatchLines', true) || _.isNaN(parseInt(OB.MobileApp.model.hasPermission('OBMOBC_LogClientMaxBatchLines', true), 10)) ? 200 : parseInt(OB.MobileApp.model.hasPermission('OBMOBC_LogClientMaxBatchLines', true), 10)
      };
      OB.Dal.find(OB.Model.LogClient, criteria, function (logClientsNotProcessed) {
        if (!logClientsNotProcessed || logClientsNotProcessed.length === 0) {
          return;
        }
        var i, totalBytes = 0,
            logClientsToProcess = [],
            maxBytes = !OB.MobileApp.model.hasPermission('OBMOBC_LogClientMaxBatchSize', true) || _.isNaN(parseInt(OB.MobileApp.model.hasPermission('OBMOBC_LogClientMaxBatchSize', true), 10)) ? 2000 : parseInt(OB.MobileApp.model.hasPermission('OBMOBC_LogClientMaxBatchSize', true), 10);

        function getBytes(s) {
          return ~ - encodeURI(s).split(/%..|./).length;
        }

        for (i = 0; i < logClientsNotProcessed.models.length; i++) {
          totalBytes += getBytes(logClientsNotProcessed.models[i].get('json'));
          logClientsToProcess.push(logClientsNotProcessed.models[i]);
          if (totalBytes >= maxBytes * 1024) {
            break;
          }
        }
        OB.UTIL.processLogClients(new Backbone.Collection(logClientsToProcess), null, null);
      });
    }
  };

  OB.UTIL.processLogClientClass = 'org.openbravo.mobile.core.utils.LogClientLoader';
  OB.UTIL.processLogClients = function (logClients, successCallback, errorCallback) {

    // do not flush if a session is not active
    if (!OB.MobileApp.model.isUserAuthenticated()) {
      OB.UTIL.Debug.execute(function () {
        console.warn("'OB.UTIL.processLogClients' cannot be executed because it requires an autheticated user");
      });
      return;
    }

    // serialize the messages
    var logClientsToJson = [];
    logClients.each(function (logClient) {
      var toJson = logClient.serializeToJSON();
      if (toJson && toJson.msg) {
        if (toJson.msg.toLowerCase().indexOf('logclient') < 0) {
          logClientsToJson.push({
            id: toJson.id,
            json: toJson.json
          });
          return;
        }
        console.warn("Log client failed to log:\n" + toJson.msg);
        OB.Dal.remove(logClient, null, function (tx, err) {
          OB.UTIL.showError(err);
        });
      }
    });

    if (logClientsToJson.length === 0) {
      return;
    }

    // send the messages to the server
    this.proc = new OB.DS.Process(OB.UTIL.processLogClientClass);
    if (OB.MobileApp.model.get('connectedToERP') && !OB.MobileApp.model.get('sessionLost')) {
      this.proc.exec({
        logclient: logClientsToJson,
        extraParams: {
          ignoreForConnectionStatus: true
        }
      }, function (data) {
        if (data && data.exception) {
          if (errorCallback) {
            errorCallback();
          }
        } else {
          var ids = [];
          logClients.each(function (logClient) {
            ids.push("'" + logClient.get('id') + "'");
          });
          var criteria = {
            whereClause: ' WHERE obmobc_logclient_id in (' + ids.join() + ')'
          };
          OB.Dal.removeAll(OB.Model.LogClient, criteria, function () {
            if (successCallback) {
              successCallback();
            }
          }, function () {
            if (errorCallback) {
              errorCallback();
            }
          });
        }
      }, null, null, 20000);
    }

  };
}());