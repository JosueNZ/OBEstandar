/*
 ************************************************************************************
 * Copyright (C) 2015-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Backbone, _, localStorage, console */

(function () {
  OB.RR = window.OB.RR || {};

  //Try to retrieve information from servers following the order of the list
  OB.RR.ServTypeFailover = {
    name: 'Failover',
    callServer: function (online, index, service, ajaxRequest) {
      var servers = OB.RR.RequestRouter.servers,
          server = servers.at(index),
          me = this,
          maxNumOfRequestRetries;

      if (!OB.UTIL.isNullOrUndefined(ajaxRequest.maxNumOfRequestRetries)) {
        maxNumOfRequestRetries = ajaxRequest.maxNumOfRequestRetries;
      } else if (!OB.UTIL.isNullOrUndefined(OB.UTIL.localStorage.getItem('maxNumOfRequestRetries'))) {
        maxNumOfRequestRetries = OB.UTIL.localStorage.getItem('maxNumOfRequestRetries');
      } else {
        maxNumOfRequestRetries = 0;
      }

      if (index === servers.length) {
        // tried the last server online, now try the offline server one more time
        if (online) {
          if (servers.length > 1) {
            ajaxRequest.numberOfRetries = 0;
          }
          this.callServer(false, 0, service, ajaxRequest);
        } else {
          console.error('The logic should not get to this line ' + service.get('name'));
        }
        return true;
      }

      if (online === Boolean(server.get('online')) && (server.get('allServices') || _.filter(server.get('services'), function (srvc) {
        return srvc === service.get('name');
      }).length > 0)) {
        if (!ajaxRequest.origUrl) {
          ajaxRequest.origUrl = ajaxRequest.url;
        }
        if (server.get('address')) {
          ajaxRequest.url = ajaxRequest.origUrl.replace('../../', server.get('address'));
        }
        if (!ajaxRequest.origFail) {
          ajaxRequest.origFail = ajaxRequest.fail;

        }
        ajaxRequest.fail = function (inSender, inResponse) {
          // found a special behavior:
          // if a request fails right away then fail is called. Then after the timeout
          // again fail is called.
          if (ajaxRequest.done) {
            // done is set in the final fail call below.
            return;
          }

          if (inSender === 401) {
            //The server is online but need to relogin because token has expired or have been deleted
            // so reset the online status directly
            server.set('online', true);
            server.handle401();
          } else {
            // tried last not online server and still failing, call origFail
            // In SynchronizedMode we won't stop retrying till we know the status of the message
            if (!online && OB.RR.RequestRouter.servers.length === (index + 1) && !OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true)) {
              // we tried all offline servers, no other option left, fail
              if (!ajaxRequest.done) {
                // the done flag keeps track that the request was done.
                OB.RR.sendingMessages = false;
                ajaxRequest.done = true;
                if (ajaxRequest.origFail) {
                  ajaxRequest.origFail(inSender, inResponse);
                }
              }
            } else if ((maxNumOfRequestRetries <= 0 || ajaxRequest.numberOfRetries >= maxNumOfRequestRetries) && OB.RR.RequestRouter.servers.length === (index + 1)) {
              // last server, no retries on offline servers even
              if (!ajaxRequest.done) {
                //If the message is being processed in backend OR if the server/connection is down continue retrying till is finished
                if (OB.RR.RequestRouter.isSynchronizedMode() && (ajaxRequest.isProcessingInBackend || (ajaxRequest.isSynchronizeModeTransaction && (inSender === "timeout" || inSender === 0)))) {
                  //Ignore fail executions with 0 because enyo always send 2 responses when timing out :"timeout" and 0
                  if (inSender !== 0) {
                    ajaxRequest.numberOfRetries = ajaxRequest.numberOfRetries + 1;
                    OB.UTIL.showConfirmation.setText(OB.I18N.getLabel('OBMOBC_DataIsBeingProcessed') + " " + OB.I18N.getLabel('OBMOBC_NumOfRetries') + " " + ajaxRequest.numberOfRetries);
                    setTimeout(function () {
                      me.callServer(online, index, service, ajaxRequest);
                    }, ajaxRequest.timeout);
                  }

                } else {
                  // the done flag keeps track that the request was done.
                  server.changeOnlineStatus(false, ajaxRequest.ignoreForConnectionStatus);
                  OB.RR.sendingMessages = false;
                  ajaxRequest.done = true;
                  if (ajaxRequest.origFail) {
                    ajaxRequest.origFail(inSender, inResponse);
                  }
                }
              }
            } else if (maxNumOfRequestRetries <= 0 || ajaxRequest.numberOfRetries >= maxNumOfRequestRetries) {
              // no retries needed, or
              // did all the retries, change to offline for the current server, move to next server
              server.changeOnlineStatus(false, ajaxRequest.ignoreForConnectionStatus);
              ajaxRequest.numberOfRetries = 0;
              me.callServer(online, index + 1, service, ajaxRequest);
            } else {
              // do a retry
              ajaxRequest.numberOfRetries = ajaxRequest.numberOfRetries + 1;
              setTimeout(function () {
                me.callServer(online, index, service, ajaxRequest);
              }, ajaxRequest.isProcessingInBackend ? ajaxRequest.timeout : 50);
            }
          }
        };

        if (!ajaxRequest.origSuccess) {
          ajaxRequest.origSuccess = ajaxRequest.success;
        }

        ajaxRequest.success = function (inSender, inResponse) {
          var callback = function () {
              if (inResponse.authenticationClient && inResponse.authenticationToken) {
                OB.UTIL.localStorage.setItem('authenticationClient', encodeURIComponent(inResponse.authenticationClient));
                OB.UTIL.localStorage.setItem('authenticationToken', encodeURIComponent(inResponse.authenticationToken));
              }
              if (!ajaxRequest.done) {
                ajaxRequest.done = true;
                if (ajaxRequest.origSuccess) {
                  ajaxRequest.origSuccess(inSender, inResponse);
                }
              }
              // server is back
              server.changeOnlineStatus(true, ajaxRequest.ignoreForConnectionStatus);
              };
          if (inResponse && inResponse.response && inResponse.response.serverStatusSignal) {
            OB.RR.RequestRouter.handleResponse(inResponse.response.serverStatusSignal, callback, ajaxRequest);
          } else {
            if (inResponse && inResponse.response && inResponse.response.showMessage) {
              //showMessage is set when we are processing a message and didn't finished
              //In SynchronizedMode we set the existing popup text
              if (OB.RR.RequestRouter.isSynchronizedMode()) {
                if (inResponse.response.showMessageTitle) {
                  OB.UTIL.showConfirmation.setHeader(inResponse.response.showMessageTitle);
                }
                OB.UTIL.showConfirmation.setText(inResponse.response.showMessage + " " + OB.I18N.getLabel('OBMOBC_NumOfRetries') + " " + ajaxRequest.numberOfRetries);
                ajaxRequest.isProcessingInBackend = true;
                ajaxRequest.fail(inSender, inResponse);
              } else {
                //In regular mode we need to create the popup
                OB.UTIL.showConfirmation.display(inResponse.response.showMessageTitle ? inResponse.response.showMessageTitle : '', inResponse.response.showMessage, [{
                  isConfirmButton: true,
                  label: OB.I18N.getLabel('OBMOBC_LblOk'),
                  action: function () {
                    callback();
                    return true;
                  }
                }]);
              }
            } else {
              callback();
            }
          }
        };
        OB.RR.RequestRouter.execAjax(ajaxRequest);
      } else {
        me.callServer(online, index + 1, service, ajaxRequest);
      }
    },

    counter: 0,
    implementation: function (service, ajaxRequest) {
      ajaxRequest.numberOfRetries = 0;
      ajaxRequest.counter = this.counter;
      this.counter = this.counter + 1;
      this.callServer(true, 0, service, ajaxRequest);
    }
  };

  // Send the request to all servers supporting it, ignore failures
  OB.RR.ServTypeBroadcast = {
    name: 'Broadcast',
    callServer: function (server, ajaxRequest) {
      if (!ajaxRequest.origUrl) {
        ajaxRequest.origUrl = ajaxRequest.url;
      }
      if (server.get('address')) {
        ajaxRequest.url = ajaxRequest.origUrl.replace('../../', server.get('address'));
      }

      OB.RR.RequestRouter.execAjax(ajaxRequest);

      // do nothing on fail/success for next requests
      ajaxRequest.fail = function (inSender, inResponse) {};
      ajaxRequest.success = function (inSender, inResponse) {};
    },

    implementation: function (service, ajaxRequest) {
      var me = this;
      var servers = _.filter(OB.RR.RequestRouter.servers.models, function (server) {
        return server.get('allServices') || _.filter(server.get('services'), function (srvc) {
          return srvc === service.get('name');
        }).length > 0;
      });
      _.each(servers, function (server) {
        me.callServer(server, ajaxRequest);
      });

    }
  };

  // Send information to all servers to save it everywhere, failed messages are retried
  OB.RR.ServTypeTransaction = {
    name: 'Transaction',

    createRequest: function (message, pendingMessages) {
      var me = this;
      return {
        url: message.get('url'),
        cacheBust: false,
        sync: false,
        timeout: OB.RR.RequestRouter.getServiceByName(message.get('service')).getServiceTimeout(10000),
        method: 'POST',
        handleAs: 'json',
        contentType: 'application/json;charset=utf-8',
        ignoreForConnectionStatus: false,
        data: message.get('messageObj'),
        success: function (inSender, inResponse) {
          var callback = function () {
              // get some authentication stuff  
              if (inResponse.authenticationClient && inResponse.authenticationToken) {
                OB.UTIL.localStorage.setItem('authenticationClient', encodeURIComponent(JSON.stringify(inResponse.authenticationClient)));
                OB.UTIL.localStorage.setItem('authenticationToken', encodeURIComponent(JSON.stringify(inResponse.authenticationToken)));
              }

              // message was successfull, remove it
              OB.Dal.remove(message, function () {
                OB.UTIL.SynchronizationHelper.eachModelSynchronized();

                // if the context has changed, lock the terminal
                if (inResponse.response && inResponse.response.contextInfo && OB.MobileApp.model.get('context')) {
                  OB.UTIL.checkContextChange(OB.MobileApp.model.get('context'), inResponse.response.contextInfo, function () {});
                }

                // process the remaining messages
                me.processMessages(pendingMessages);

              }, function (error) {
                OB.error(arguments);
              });
              };

          if (inResponse && inResponse.response && inResponse.response.serverStatusSignal) {
            OB.RR.RequestRouter.handleResponse(inResponse.response.serverStatusSignal, callback, this);
          } else {
            if (inResponse && inResponse.response && inResponse.response.showMessage) {
              OB.UTIL.showConfirmation.display(inResponse.response.showMessageTitle ? inResponse.response.showMessageTitle : '', inResponse.response.showMessage, [{
                isConfirmButton: true,
                label: OB.I18N.getLabel('OBMOBC_LblOk'),
                action: function () {
                  callback();
                  return true;
                }
              }]);
            } else {
              callback();
            }
          }
        },
        fail: function (inSender, inResponse) {
          // save the message in error status
          message.set('status', 'failure');
          OB.Dal.save(message, function () {
            me.updateSyncIcon();

            // in multi server online/offline notification is based on presence of messages to sync
            if (OB.MobileApp.model.get('connectedToERP') && OB.RR.RequestRouter.isMultiServer()) {
              OB.MobileApp.model.triggerOffLine();
            }
          }, function () {
            OB.error(arguments);
          });
        }
      };
    },

    updateSyncIcon: function () {
      // update the sync icon/messages
      OB.Dal.find(OB.Model.Message, {}, function (msgs) {
        if (_.reduce(msgs.models, function (val, msg) {
          return val || msg.get('status') === 'failure';
        }, false)) {
          OB.UTIL.SynchronizationHelper.modelsNotSynchronized();
        } else if (msgs.length > 0 && !OB.UTIL.SynchronizationHelper.isModelsSynchronizing()) {
          OB.UTIL.SynchronizationHelper.modelsSynchronizing();
        } else if (msgs.length === 0 && !OB.UTIL.SynchronizationHelper.isModelsSynchronized()) {
          OB.UTIL.SynchronizationHelper.modelsSynchronized();
        }
        // in multi server online/offline notification is based on presence of messages to sync
        if (msgs.length === 0 && !OB.MobileApp.model.get('connectedToERP') && OB.RR.RequestRouter.isMultiServer()) {
          OB.MobileApp.model.triggerOnLine();
        }

      }, function () {
        OB.error(arguments);
      });
    },

    processMessages: function (pendingMessages) {
      var me = this,
          message, ajaxRequest, request;

      // nothing more to process, restart the sending
      // for a final check if new messages arrived
      if (pendingMessages.length === 0) {
        OB.RR.sendingMessages = false;
        this.sendMessages();
        return;
      }

      // pop the first message to process it
      message = pendingMessages.shift();
      request = this.createRequest(message, pendingMessages);

      var service = _.find(OB.RR.RequestRouter.availableServices.models, function (srvc) {
        return message.get('service') === srvc.get('name');
      });

      if (!service) {
        OB.UTIL.showError('Service ' + message.get('service') + ' not found.');
        OB.RR.sendingMessages = false;
        return;
      }

      // send the message using failover, if successfull it will be removed
      OB.RR.ServTypeFailover.implementation(service, request);
    },

    sendMessages: function () {
      var me = this;

      // not yet initialized
      if (!OB.Model.Message || !OB.Model.Message.areTablesCreated) {
        return;
      }

      // already here go away
      if (OB.RR.sendingMessages) {
        return;
      }

      me.updateSyncIcon();

      // on purpose after updateSyncIcon to not get stuck 
      // if that method fails somehow
      OB.RR.sendingMessages = true;

      OB.Dal.find(OB.Model.Message, {
        '_orderByClause': 'time asc'
      }, function (messages) {
        if (messages.length > 0) {
          me.processMessages(messages);
        } else {
          OB.RR.sendingMessages = false;
          me.updateSyncIcon();
        }
      }, function () {
        OB.RR.sendingMessages = false;
        OB.error(arguments);
      });
    },

    implementation: function (service, ajaxRequest) {

      var me = this,
          successCallback = function (tx) {
          // execute the success callbacks
          new enyo.Ajax(ajaxRequest).success(null, {
            response: {
              contextInfo: null,
              result: '0',
              status: 0
            }
          }, tx);
          me.sendMessages();
          };

      if (!ajaxRequest.data || !JSON.parse(ajaxRequest.data).data) {
        var errorMessage = OB.I18N.getLabel('OBMOBC_WrongMobileService', [ajaxRequest.url]);
        OB.error(errorMessage);

        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_LblWarning'), OB.I18N.getLabel('OBMOBC_WrongMobileService', [ajaxRequest.url]), [{
          isConfirmButton: true,
          label: OB.I18N.getLabel('OBMOBC_LblOk'),
          action: function () {
            OB.MobileApp.model.lock();
            return true;
          }
        }], {
          onHideFunction: function () {
            OB.MobileApp.model.lock();
            return true;
          }
        });
        return;
      }
      var parsedData = JSON.parse(ajaxRequest.data);
      if (parsedData.data.length > 0) {
        var message = new OB.Model.Message();
        message.set('service', service.get('name'));
        message.set('url', ajaxRequest.url);
        message.set('time', new Date().getTime());
        message.set('messageObj', ajaxRequest.data);
        OB.Dal.transaction(function (tx) {
          OB.Dal.saveInTransaction(tx, message, function () {
            successCallback(tx);
          }, function () {
            OB.error(arguments);
          });
        });
      } else {
        successCallback();
      }
    }
  };

  //Check if offline servers are back
  OB.RR.ServTypePing = {
    name: 'Ping',
    implementation: function (service, ajaxRequest) {
      var servers = OB.RR.RequestRouter.servers,
          me = this,
          offlineServers = _.filter(servers.models, function (server) {
          return !server.get('online');
        });

      // nothing to do, call the success call back, prevents a warning
      // that unbalanced call to synchronized helper
      if (offlineServers.length === 0) {
        new enyo.Ajax(ajaxRequest).success(null, {
          response: {
            contextInfo: null,
            result: '0',
            status: 0
          }
        });
        return;
      }

      _.each(offlineServers, function (server) {
        if (!ajaxRequest.origUrl) {
          ajaxRequest.origUrl = ajaxRequest.url;
        }
        if (server.get('address')) {
          ajaxRequest.url = ajaxRequest.origUrl.replace('../../', server.get('address'));
        }
        if (!ajaxRequest.origFail) {
          ajaxRequest.origFail = ajaxRequest.fail;
        }
        ajaxRequest.fail = function (inSender, inResponse) {

          if (inSender === 401) {
            //The server is online but need to relogin because token has expired or have been deleted
            // so reset the online status directly
            server.set('online', true);
            server.handle401();
            ajaxRequest.origFail(inSender, inResponse);
            ajaxRequest.origFail = null;
            ajaxRequest.done = true;
            return;
          }

          server.changeOnlineStatus(false, ajaxRequest.ignoreForConnectionStatus);
          // ajaxRequest is in the scope of the implementation function
          // so there is only one instance even if multiple servers are offline
          if (ajaxRequest.done) {
            return;
          }

          ajaxRequest.done = true;
          // in case of one server then real offline 
          if (!OB.RR.RequestRouter.isMultiServer()) {
            ajaxRequest.origFail(inSender, inResponse);
            ajaxRequest.origFail = null;
          } else if (ajaxRequest.origSuccess) {
            // in case of multiserver then don't do real offline
            // just call success, status of server has been set anyway
            ajaxRequest.origSuccess(null, {
              response: {
                contextInfo: null,
                result: '0',
                status: 0
              }
            });
          }
        };

        if (!ajaxRequest.origSuccess) {
          ajaxRequest.origSuccess = ajaxRequest.success;
        }
        ajaxRequest.success = function (inSender, inResponse) {
          var callback = function () {
              if (inResponse.authenticationClient && inResponse.authenticationToken) {
                OB.UTIL.localStorage.setItem('authenticationClient', encodeURIComponent(inResponse.authenticationClient));
                OB.UTIL.localStorage.setItem('authenticationToken', encodeURIComponent(inResponse.authenticationToken));
              }
              if (!ajaxRequest.done) {
                ajaxRequest.done = true;
                if (ajaxRequest.origSuccess) {
                  ajaxRequest.origSuccess(inSender, inResponse);
                }
              }
              // server is back
              server.changeOnlineStatus(true, ajaxRequest.ignoreForConnectionStatus);
              };
          if (inResponse && inResponse.response && inResponse.response.serverStatusSignal) {
            OB.RR.RequestRouter.handleResponse(inResponse.response.serverStatusSignal, callback, ajaxRequest);
          } else {
            callback();
          }
        };
        OB.RR.RequestRouter.execAjax(ajaxRequest);
      });
    }
  };

  OB.RR.Service = Backbone.Model.extend({
    defaults: {
      name: null,
      type: null
    },
    initialize: function (attributes) {
      if (attributes) {
        this.set('name', attributes.name);
        this.set('defaultTimeout', attributes.defaultTimeout ? attributes.defaultTimeout * 1000 : undefined);
        this.set('timeout', attributes.timeout ? attributes.timeout * 1000 : undefined);
        switch (attributes.type) {
        case 'Failover':
          this.set('type', OB.RR.ServTypeFailover);
          break;
        case 'Transaction':
          this.set('type', OB.RR.ServTypeTransaction);
          break;
        case 'Broadcast':
          this.set('type', OB.RR.ServTypeBroadcast);
          break;
        case 'Ping':
          this.set('type', OB.RR.ServTypePing);
          break;
        default:
          this.set('type', OB.RR.ServTypeFailover);
          break;
        }
      }
    },
    getServiceTimeout: function (timeout, ajaxRequest) {
      //Timeout priority: Default service timeout, User service timeout, Passed by parameters timeout
      //This priority can be changed and force a timeout setting overrideDefaultTimeout
      return ajaxRequest && ajaxRequest.overrideDefaultTimeout && ajaxRequest.overrideDefaultTimeout.value ? timeout : this.get('timeout') || this.get('defaultTimeout') || timeout;
    }
  });

  OB.RR.Server = Backbone.Model.extend({
    defaults: {
      name: null,
      address: null,
      online: true,
      mainServer: false,
      allServices: false,
      services: []
    },

    handle401: function () {
      var server = this;
      if (_.find(OB.RR.RequestRouter.servers.models, function (server) {
        return server.get('online') && (server.get('allServices') || _.find(server.get('services'), function (service) {
          return service === "org.openbravo.retail.posterminal.POSLoginHandler";
        }));
      })) {
        //do relogin
        OB.MobileApp.model.set('sessionLost', true);
        OB.UTIL.stopLogClient();
        OB.UTIL.showConfirmation.display(
        OB.I18N.getLabel('OBMOBC_Online'), OB.I18N.getLabel('OBMOBC_OnlineConnectionHasReturned'), [{
          label: OB.I18N.getLabel('OBMOBC_LblLoginAgain'),
          action: function () {
            OB.MobileApp.model.lock();
            OB.UTIL.showLoading(true);
          }
        }], {
          autoDismiss: false,
          onHideFunction: function () {
            OB.MobileApp.model.lock();
            OB.UTIL.showLoading(true);
          }
        });
      }
    },
    reconnect: undefined,
    shouldTransitionToOnline: function (level) {
      var params = {
        appModuleId: OB.MobileApp.model.get('appModuleId')
      },
          me = this,
          intervalTime, retries, checkingIntervalTime, requestValidationCounter = 0,
          defaultTime = OB.UTIL.localStorage.getItem('timeToGoOnline') || 30000,
          maxTime = OB.UTIL.localStorage.getItem('maxTimeToGoOnline') || 300000,
          checkingTime = OB.UTIL.localStorage.getItem('checkingTimeToGoOnline') || 0;

      //One request is done at the beginning of each level and then the defined retries
      switch (level) {
      case 0:
        //Fast attempt forced by the user
        intervalTime = 5000;
        retries = 2;
        break;
      case 1:
        //Default time at first attempt
        intervalTime = defaultTime;
        retries = 3;
        break;
      case 2:
        //Middle time between default and max time
        intervalTime = OB.DEC.add(OB.DEC.sub(maxTime, defaultTime) / 2, defaultTime);
        retries = 4;
        break;
      case 3:
        //Max time at last attempt. Next attempts will have this max time
        intervalTime = maxTime;
        retries = 5;
        break;
      default:
        intervalTime = defaultTime;
        retries = 3;
      }
      checkingIntervalTime = intervalTime >= checkingTime && checkingTime !== '0' ? checkingTime : intervalTime;
      OB.UTIL.localStorage.setItem('nextTimeToGoOnline', OB.DEC.add(new Date().getTime(), intervalTime));
      var ajaxRequest = new enyo.Ajax({
        url: '../../org.openbravo.mobile.core.checkServerAvailability',
        cacheBust: false,
        method: 'GET',
        handleAs: 'json',
        timeout: OB.UTIL.localStorage.getItem('timeoutToGoOnline') || 5000,
        data: {
          params: JSON.stringify(params)
        },
        contentType: 'application/json;charset=utf-8',
        success: function (inSender, inResponse) {
          if (!this.done) {
            this.done = true;
            if (!inResponse || !inResponse.response) {
              return;
            }
            if (inResponse.response.error) {
              OB.info('[ConnectionStatus] Reconnect attempt failed. Server replied: ' + inResponse.response.error);
            } else {
              requestValidationCounter--;
              if (parseInt(OB.UTIL.localStorage.getItem('nextTimeToGoOnline'), 10) <= new Date().getTime()) {
                if (requestValidationCounter === 0) {
                  OB.info('[ConnectionStatus] Go to Online');
                  OB.MobileApp.model.triggerOnLine();
                  clearInterval(me.reconnect);
                  setTimeout(function () {
                    OB.RR.ServTypeTransaction.sendMessages();
                  }, 10000);
                } else {
                  me.shouldTransitionToOnline(level >= 3 ? level : level + 1);
                }
              }
            }
          }
        },
        fail: function (inSender, inResponse) {
          if (!this.done) {
            this.done = true;
            OB.info('[ConnectionStatus] Reconnect attempt failed, keep trying to reconnect');
            if (parseInt(OB.UTIL.localStorage.getItem('nextTimeToGoOnline'), 10) <= new Date().getTime()) {
              me.shouldTransitionToOnline(level >= 3 ? level : level + 1);
            }
          }
        }
      });
      clearInterval(me.reconnect);
      setTimeout(function () {
        me.reconnect = setInterval(function () {
          requestValidationCounter++;
          OB.RR.RequestRouter.execAjax(ajaxRequest, true);
        }, checkingIntervalTime / retries);
      }, intervalTime - checkingIntervalTime);
    },
    failedRequestsToOffline: 0,
    failedRequestsToOfflineTimeOut: undefined,
    shouldTransitionToOffline: function () {
      var me = this;
      OB.info('[ConnectionStatus] A request failed, check if we should move to Offline');
      this.failedRequestsToOffline++;
      if (this.failedRequestsToOffline >= OB.UTIL.localStorage.getItem('numFailedReqToOffline')) {
        this.failedRequestsToOffline = 0;
        if (this.failedRequestsToOfflineTimeOut) {
          clearTimeout(this.failedRequestsToOfflineTimeOut);
          this.failedRequestsToOfflineTimeOut = undefined;
        }
        OB.MobileApp.model.triggerOffLine();
        OB.info('[ConnectionStatus] Gone to offline and start transition to Online process');
        this.shouldTransitionToOnline(1);
      } else if (!this.failedRequestsToOfflineTimeOut) {
        this.failedRequestsToOfflineTimeOut = setTimeout(function () {
          me.failedRequestsToOffline = 0;
          clearTimeout(me.failedRequestsToOfflineTimeOut);
          me.failedRequestsToOfflineTimeOut = undefined;
        }, OB.UTIL.localStorage.getItem('timeFailedReqToOffline'));
      }
    },
    changeOnlineStatus: function (newStatus, ignoreForConnectionStatus) {

      if (OB.RR.RequestRouter.isMultiServer() || OB.RR.RequestRouter.isSynchronizedMode() || ignoreForConnectionStatus) {
        var oldStatus = Boolean(this.get('online'));
        // single server controls online/offline status
        // in case of multi-server then online/offline is controlled by
        // if there are messages to be synced.
        if (newStatus && !oldStatus) {
          OB.RR.ServTypeTransaction.sendMessages();
        }
        if (!ignoreForConnectionStatus) {
          this.set('online', newStatus);
          this.checkFireOnlineNotification();
        }
      } else {
        if (!newStatus && (OB.UTIL.isNullOrUndefined(OB.MobileApp.model.get('connectedToERP')) || OB.MobileApp.model.get('connectedToERP'))) {
          this.shouldTransitionToOffline();
        }
      }
    },

    checkFireOnlineNotification: function () {
      var connected = OB.MobileApp.model.get('connectedToERP');
      // only use this approach for single server cases
      // in multi-server the online/offline concept is
      // based on the check if there are messages to sync
      // if there are messages to sync then it is offline
      if (OB.RR.RequestRouter.isMultiServer()) {
        return;
      }

      if (!this.get('currentServer')) {
        return;
      }
      if (this.get('online') && !connected) {
        OB.MobileApp.model.triggerOnLine();
      } else if (!this.get('online') && connected) {
        OB.MobileApp.model.triggerOffLine();
      }
    },

    initialize: function (attributes) {
      var lowerCaseAddress, correctedAddress, shouldSetProtocol;
      if (attributes && attributes.name) {
        this.set('name', attributes.name);
        if (attributes.address) {
          correctedAddress = attributes.address.trim();
          shouldSetProtocol = attributes.currentServer;
          lowerCaseAddress = correctedAddress.toLowerCase();
          shouldSetProtocol |= !lowerCaseAddress.startsWith('http');
          if (shouldSetProtocol) {
            if (lowerCaseAddress.startsWith('http:')) {
              correctedAddress = document.location.protocol + '//' + correctedAddress.substring(7);
            } else if (lowerCaseAddress.startsWith('https:')) {
              correctedAddress = document.location.protocol + '//' + correctedAddress.substring(8);
            } else {
              correctedAddress = document.location.protocol + '//' + correctedAddress;
            }
          }
          if (correctedAddress.charAt(correctedAddress.length - 1) !== '/') {
            correctedAddress += '/';
          }
          this.set('address', correctedAddress);
        } else {
          this.set('address', attributes.address);
        }
        this.set('online', attributes.online);
        this.set('mainServer', attributes.mainServer);
        this.set('allServices', attributes.allServices);
        this.set('currentServer', attributes.currentServer);
        if (attributes.services) {
          this.set('services', attributes.services);
        } else {
          this.set('services', []);
        }
      }
    }
  });

  OB.RR.Request = Backbone.Model.extend({
    defaults: {
      ajaxRequest: null
    },
    initialize: function (attributes) {
      if (attributes && attributes.ajaxRequest) {
        this.set('ajaxRequest', attributes.ajaxRequest);
      }
    },
    exec: function (serviceName) {
      var service;

      OB.RR.RequestRouter.initialize();

      //if servers are not loaded yet, save request to send them after servers are ready
      if (OB.RR.RequestRouter.servers.length === 0) {
        OB.RR.RequestRouter.pendingRequests.push({
          request: this,
          serviceName: serviceName
        });
        return true;
      }
      if (this.get('ajaxRequest')) {
        service = OB.RR.RequestRouter.getServiceByName(serviceName);

        //set timeout
        this.get('ajaxRequest').timeout = service.getServiceTimeout(this.get('ajaxRequest').timeout, this.get('ajaxRequest'));
        service.get('type').implementation(service, this.get('ajaxRequest'));
      }
    }
  });

  OB.RR.RequestRouter = {
    servers: new Backbone.Collection(),
    availableServices: new Backbone.Collection(),
    pendingRequests: [],
    initialize: function () {
      var me = this,
          tmpPendingRequests, foundCurrentServer = false;

      OB.RR.RequestRouter.initializePreferences();

      tmpPendingRequests = this.pendingRequests;
      if (this.availableServices && this.availableServices.length > 0) {
        this.pendingRequests = [];
        _.each(tmpPendingRequests, function (obj) {
          obj.request.exec(obj.serviceName);
        });
        return;
      }

      // set this before returning as the servers collection is checked
      // to see if the layer has been initialized
      this.servers = new Backbone.Collection();
      this.availableServices = new Backbone.Collection();

      if (OB.UTIL.localStorage.getItem('servers')) {
        _.each(JSON.parse(OB.UTIL.localStorage.getItem('servers')), function (server) {
          var newServer = new OB.RR.Server(server);
          // make sure that there is max one server the current one
          if (!foundCurrentServer && newServer.get('currentServer')) {
            foundCurrentServer = true;
          } else {
            newServer.set('currentServer', false);
          }
          me.servers.add(newServer);
        });

        // no current server, use location 
        if (!foundCurrentServer) {
          _.each(this.servers.models, function (srv) {
            if (srv.get('address') && srv.get('address').split('/')[2] === document.location.host) {
              srv.set('currentServer', true);
              foundCurrentServer = true;
            }
          });
        }
      }

      if (OB.UTIL.localStorage.getItem('services')) {
        _.each(JSON.parse(OB.UTIL.localStorage.getItem('services')), function (service) {
          me.availableServices.add(new OB.RR.Service(service));
        });
      }

      //In case current server is not in the list
      if (!foundCurrentServer) {
        me.servers.add(new OB.RR.Server({
          name: 'Default',
          address: null,
          online: true,
          mainServer: true,
          currentServer: true,
          allServices: true,
          services: []
        }));
      }

      if (this.availableServices && this.availableServices.length > 0) {
        this.pendingRequests = [];
        _.each(tmpPendingRequests, function (obj) {
          obj.request.exec(obj.serviceName);
        });
      }
    },

    sendAllMessages: function () {
      // send any pending messages
      OB.RR.ServTypeTransaction.sendMessages();
    },

    initializePreferences: function () {
      var me = this;

      if (!OB || !OB.MobileApp || !OB.MobileApp.model || !OB.MobileApp.model.get('permissions')) {
        // not yet ready, bail out
        return;
      }

      if (this.preferencesInitialized) {
        return;
      }
      this.preferencesInitialized = true;

      function getPreference(preference, minValue, defaultValue) {
        try {
          var val = OB.MobileApp.model.get('permissions')[preference];
          if (!val || !_.isNumber(parseInt(val, 10)) || parseInt(val, 10) < minValue) {
            return defaultValue;
          }
          return parseInt(val, 10);
        } catch (e) {
          return defaultValue;
        }
      }

      OB.UTIL.localStorage.setItem('offlinePingInterval', getPreference('OBMOBC_RequestRouterPingTime', 60000, OB.UTIL.localStorage.getItem('offlinePingInterval') || 60000));
      OB.UTIL.localStorage.setItem('tokenRefreshInterval', getPreference('OBMOBC_RequestRouterTokenRefreshInterval', 10 * 60000, OB.UTIL.localStorage.getItem('tokenRefreshInterval') || 30 * 60000));
      OB.UTIL.localStorage.setItem('maxNumOfRequestRetries', getPreference('OBMOBC_NumberOfRequestRetries', 0, OB.UTIL.localStorage.getItem('maxNumOfRequestRetries') || 0));

      OB.UTIL.localStorage.setItem('timeToGoOnline', getPreference('OBMOBC_TimeToGoOnline', 3000, OB.UTIL.localStorage.getItem('timeToGoOnline') || 30000));
      OB.UTIL.localStorage.setItem('timeoutToGoOnline', getPreference('OBMOBC_TimeoutToGoOnline', 3000, OB.UTIL.localStorage.getItem('timeoutToGoOnline') || 5000));
      OB.UTIL.localStorage.setItem('maxTimeToGoOnline', getPreference('OBMOBC_MaxTimeToGoOnline', 30000, OB.UTIL.localStorage.getItem('maxTimeToGoOnline') || 300000));
      OB.UTIL.localStorage.setItem('timeFailedReqToOffline', getPreference('OBMOBC_TimeFailedReqOffline', 5000, OB.UTIL.localStorage.getItem('timeFailedReqToOffline') || 60000));
      OB.UTIL.localStorage.setItem('numFailedReqToOffline', getPreference('OBMOBC_NumFailedReqOffline', 1, OB.UTIL.localStorage.getItem('numFailedReqToOffline') || 1));
      OB.UTIL.localStorage.setItem('checkingTimeToGoOnline', getPreference('OBMOBC_CheckTimeToGoOnline', 0, OB.UTIL.localStorage.getItem('checkingtimeToGoOnline') || 0));

      if (this.offlinePing) {
        clearInterval(this.offlinePing);
      }
      if (this.isMultiServer() || this.isSynchronizedMode()) {
        this.offlinePing = setInterval(function () {
          if (_.filter(me.servers.models, function (server) {
            return !server.get('online');
          }).length > 0) {
            OB.UTIL.checkConnectivityStatus();
          }
        }, OB.UTIL.localStorage.getItem('offlinePingInterval'));
      }

      if (this.refreshTokenProcess) {
        clearInterval(this.refreshTokenInterval);
      }
      this.countRefreshTokenErrors = 0;
      this.refreshTokenInterval = setInterval(function () {
        me.refreshToken();
      }, OB.UTIL.localStorage.getItem('tokenRefreshInterval'));
    },

    refreshToken: function () {
      var me = this;
      new OB.DS.Request('org.openbravo.mobile.core.authenticate.GetToken').exec({
        ignoreForConnectionStatus: true
      }, function (data) {
        if (data.authenticationClient && data.authenticationToken) {
          me.countRefreshTokenErrors = 0;
          OB.UTIL.localStorage.setItem('authenticationClient', encodeURIComponent(data.authenticationClient));
          OB.UTIL.localStorage.setItem('authenticationToken', encodeURIComponent(data.authenticationToken));
        } else {
          me.countRefreshTokenErrors++;
        }
        // too many errors, stop trying
        if (me.countRefreshTokenErrors > 3) {
          me.countRefreshTokenErrors = 0;
          clearInterval(me.refreshTokenInterval);
        }
      }, function () {
        me.countRefreshTokenErrors++;
        // too many errors, stop trying
        if (me.countRefreshTokenErrors > 3) {
          me.countRefreshTokenErrors = 0;
          clearInterval(me.refreshTokenInterval);
        }
      }, true, 20000);
    },

    isTransactionalService: function (serviceName) {
      var service = this.getServiceByName(serviceName);
      if (!service) {
        return false;
      }
      return service.get('type') === OB.RR.ServTypeTransaction;
    },

    ignoreManifestLoadError: function () {
      var result = false;

      this.initialize();

      // single server, don't ignore
      if (!this.servers || !this.isMultiServer()) {
        return result;
      }

      // if the current server is offline then we should ignore
      // the manifest error
      _.each(this.servers.models, function (srv) {
        if (srv.get('currentServer') && !srv.get('online')) {
          result = true;
        }
      });
      return result;
    },
    handleResponseCallbacks: [{
      name: 'ON',
      handleAction: function (callback, ajaxRequest) {
        callback();
      }
    }, {
      name: 'OFF',
      handleAction: function (callback, ajaxRequest) {
        callback();
      }
    }, {
      name: 'TON',
      handleAction: function (callback, ajaxRequest) {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_LblTransitionToOnlineTitle'), OB.I18N.getLabel('OBMOBC_LblTransitionToOnline'), [{
          isConfirmButton: true,
          label: OB.I18N.getLabel('OBMOBC_LblOk'),
          action: function () {
            OB.MobileApp.model.lock();
            return true;
          }
        }]);
      }
    }, {
      name: 'TOFF',
      handleAction: function (callback, ajaxRequest) {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_LblTransitionToOfflineTitle'), OB.I18N.getLabel('OBMOBC_LblTransitionToOffline'), [{
          isConfirmButton: true,
          label: OB.I18N.getLabel('OBMOBC_LblOk'),
          action: function () {
            OB.MobileApp.model.lock();
            return true;
          }
        }]);
      }
    }],
    handleResponse: function (keyName, callback, ajaxRequest) {
      var cbk = _.filter(this.handleResponseCallbacks, function (callbackFunc) {
        return callbackFunc.name === keyName;
      });
      if (cbk && cbk.length > 0) {
        cbk[0].handleAction(callback, ajaxRequest);
      } else {
        callback();
      }
    },
    execAjax: function (ajaxRequest, forceExec) {

      if (!this.isMultiServer() && !this.isSynchronizedMode() && OB.MobileApp.model.get('connectedToERP') === false && !forceExec) {
        //Do not call to the server if we are offline
        new enyo.Ajax(ajaxRequest).fail();
      } else {
        if (OB.UTIL.localStorage.getItem('authenticationClient') && OB.UTIL.localStorage.getItem('authenticationToken')) {
          if (ajaxRequest.url.indexOf('?') === -1) {
            ajaxRequest.url = ajaxRequest.url + '?';
          } else {
            ajaxRequest.url = ajaxRequest.url + '&';
          }
          ajaxRequest.url = ajaxRequest.url + 'authenticationClient=' + OB.UTIL.localStorage.getItem('authenticationClient') + '&authenticationToken=' + OB.UTIL.localStorage.getItem('authenticationToken');
        }
        ajaxRequest.xhrFields = {
          'withCredentials': true
        };
        // do the real send with a new ajaxRequest object, otherwise 
        // in case of timeouts Enyo get's confused and starts to call the fail method
        // set on the shared ajaxRequest object which is passed around.
        new enyo.Ajax(ajaxRequest).go(ajaxRequest.data).response('success').error('fail');
      }
    },
    isMultiServer: function () {
      return this.servers.length > 1;
    },
    isSynchronizedMode: function () {
      return OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true);
    },
    shouldTransitionToOnline: function (level) {
      if (this.servers.length > 1) {
        OB.error('We only can call shouldTransitionToOnline with single Server');
        return;
      }
      this.servers.models[0].shouldTransitionToOnline(level);
    },
    getServiceByName: function (serviceName) {
      var service, services = _.filter(OB.RR.RequestRouter.availableServices.models, function (srvc) {
        return serviceName.indexOf(srvc.get('name')) !== -1;
      });
      if (services.length === 0) {
        //If services list is empty we do not have to check if we forget to define a service or not
        if (OB.RR.RequestRouter.availableServices.models.length > 0) {
          OB.error("The service does not exist. Add it to the Mobile Services window in the backend (service requested: '" + serviceName + "')");
        }
        service = new OB.RR.Service({
          name: 'Generic',
          type: OB.RR.ServTypeFailover
        });
      } else if (services.length > 1) {
        //In case of having more than one service, get longest name service. Longest name service will be the most similar to serviceName
        _.each(services, function (iterSrvc) {
          if (!service || service.get('name').length < iterSrvc.get('name').length) {
            service = iterSrvc;
          }
        });
      } else {
        service = services[0];
      }
      return service;
    }
  };

  OB.RR.RequestRouter.initialize();
}());