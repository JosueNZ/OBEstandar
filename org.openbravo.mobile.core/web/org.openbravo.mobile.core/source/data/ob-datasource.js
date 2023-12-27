/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone, _, enyo, $, console */

(function () {
  OB.DS = window.OB.DS || {};

  OB.DS.MAXSIZE = 100;
  OB.DS.requestAllowed = OB.DS.requestAllowed || true;

  OB.DS.allowRequests = OB.DS.allowRequests || {};
  OB.DS.allowRequests = function (isAllowed) {
    OB.DS.requestAllowed = isAllowed;
  };

  function serviceSuccess(inSender, inResponse, callback, tx) {
    if (inResponse._entityname) {
      callback([inResponse]);
    } else {
      var response = inResponse.response;

      if (!response) {
        // the response is empty
        response = {};
        response.error = {};
        response.error.message = "Unknown error";
        response.status = 501;
      }

      // if the context has changed, lock the terminal
      if (response.contextInfo && OB.MobileApp.model.get('context')) {
        OB.UTIL.checkContextChange(OB.MobileApp.model.get('context'), response.contextInfo, function () {
          //Source version will be checked to ensure that Web POS is updated. Only check for requests from code loaded server.
          if (response && response.sourceVersion && inSender.url.indexOf(document.location.host) !== -1) {
            OB.UTIL.checkSourceVersion(response.sourceVersion, false);
          }
        });
      }

      var status = response.status;

      if (status === 0) {
        callback(response.data, response.message, response.lastUpdated, (response.endRow + 1), tx);
        return;
      }

      // an error has been sent in the successful response
      if (!response.ignoreForClientLog) {
        OB.error("serviceSuccess error: status: " + response.status + ((response.error && response.error.message) ? (", error.message: " + response.error.message) : ""));
      }

      // generic error message
      var exception = {
        message: 'Unknown error',
        status: response
      };

      if (response.errors) {
        if (OB.MobileApp.model.get('isLoggingIn')) {
          OB.MobileApp.model.set("datasourceLoadFailed", true);
        }
        exception.message = response.errors.id;
      }

      if (response.error && response.error.message) {
        if (OB.MobileApp.model.get('isLoggingIn') && !response.error.invalidPermission) {
          OB.MobileApp.model.set("datasourceLoadFailed", true);
        }
        exception.invalidPermission = response.error.invalidPermission;
        exception.message = response.error.message;
      } else if (response.error) {
        exception.message = response.error;
      }

      // argument checks
      OB.UTIL.Debug.execute(function () {
        if (!exception) {
          console.error("The exception message is missing");
        }
      });

      callback({
        exception: exception
      });
    }
  }

  function serviceError(inSender, inResponse, callback, callbackError) {
    if (OB.MobileApp.model.get('isLoggingIn')) {
      OB.MobileApp.model.set("datasourceLoadFailed", true);
    }
    if (callbackError) {
      callbackError({
        exception: {
          message: OB.I18N.getLabel('OBMOBC_MsgApplicationServerNotAvailable'),
          status: inResponse,
          inSender: inSender
        }
      });
    } else {
      callback({
        exception: {
          message: OB.I18N.getLabel('OBMOBC_MsgApplicationServerNotAvailable'),
          status: inResponse,
          inSender: inSender
        }
      });
    }
  }

  OB.DS.servicePOSTRequests = [];

  function servicePOST(source, dataparams, callback, callbackError, async, timeout) {
    OB.DS.servicePOSTRequests.push(source);
    //Timeout priority: Default service timeout, User service timeout, Default preference timeout, 15000
    //This priority can be changed and force a timeout setting overrideDefaultTimeout
    var rr, tmOut = dataparams && dataparams.parameters && dataparams.parameters.overrideDefaultTimeout && dataparams.parameters.overrideDefaultTimeout.value ? timeout : OB.RR.RequestRouter.getServiceByName(source).getServiceTimeout(OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true) ? OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true) * 1000 : 60000);
    //var rr, tmOut = dataparams && dataparams.parameters && dataparams.parameters.overrideDefaultTimeout && dataparams.parameters.overrideDefaultTimeout.value ? timeout : OB.RR.RequestRouter.getServiceByName(source).getServiceTimeout(OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true) ? OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true) * 1000 : 800000);

    //send timeout in parameters to use it in backend
    dataparams.timeout = tmOut;
    if (async !== false) {
      async = true;
    }
    var ajaxRequest = new enyo.Ajax({
      url: '../../org.openbravo.mobile.core.service.jsonrest/' + source,
      cacheBust: false,
      sync: !async,
      timeout: tmOut,
      //timeout: 180000,
      method: 'POST',
      handleAs: 'json',
      contentType: 'application/json;charset=utf-8',
      ignoreForConnectionStatus: dataparams.parameters && dataparams.parameters.ignoreForConnectionStatus ? dataparams.parameters.ignoreForConnectionStatus : false,
      data: JSON.stringify(dataparams),
      success: function (inSender, inResponse, tx) {
        OB.DS.servicePOSTRequests.pop(source);
        if (this.processHasFailed) {
          return;
        }
        serviceSuccess(inSender, inResponse, callback, tx);
      },
      fail: function (inSender, inResponse) {
        OB.DS.servicePOSTRequests.pop(source);
        OB.UTIL.Debug.execute(function () {
          // show an error while in debug mode to help debugging and testing
          console.error("servicePOST error: " + inSender + ", source: " + source);
        });
        this.processHasFailed = true;
        inResponse = inResponse || {};
        if (inSender && inSender === "timeout") {
          inResponse.timeout = true;
        }
        serviceError(inSender, inResponse, callback, callbackError);
      }
    });
    //Set paramaters received in dataparams.extraParams
    if (dataparams.extraParams) {
      _.each(_.keys(dataparams.extraParams), function (key) {
        ajaxRequest[key] = dataparams.extraParams[key];
      });
    }
    rr = new OB.RR.Request({
      ajaxRequest: ajaxRequest
    });
    rr.exec(source);
  }

  OB.DS.serviceGETRequests = [];

  function serviceGET(source, dataparams, callback, callbackError, async, timeout) {
    OB.DS.serviceGETRequests.push(source);
    var rr, synchId = OB.UTIL.SynchronizationHelper.busyUntilFinishes('serviceGET ' + source);
    //Timeout priority: Default service timeout, User service timeout, Default preference timeout, 15000
    //This priority can be changed and force a timeout setting overrideDefaultTimeout
    //var tmOut = dataparams && dataparams.parameters && dataparams.parameters.overrideDefaultTimeout && dataparams.parameters.overrideDefaultTimeout.value ? timeout : OB.RR.RequestRouter.getServiceByName(source).getServiceTimeout(OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true) ? OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true) * 1000 : 60000);
    var tmOut = dataparams && dataparams.parameters && dataparams.parameters.overrideDefaultTimeout && dataparams.parameters.overrideDefaultTimeout.value ? timeout : OB.RR.RequestRouter.getServiceByName(source).getServiceTimeout(OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true) ? OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true) * 1000 : 1800000);
    //send timeout in parameters to use it in backend
    dataparams.timeout = tmOut;

    console.log('TIMEOUT POS ', dataparams.timeout);
    console.log(OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true));
    console.log(OB.MobileApp.model.hasPermission('OBMOBC_DefaultRequestTimeout', true) * 1000);

    if (async !== false) {
      async = true;
    }
    var ajaxRequest = new enyo.Ajax({
      url: '../../org.openbravo.mobile.core.service.jsonrest/' + source + '/' + encodeURI(JSON.stringify(dataparams)),
      cacheBust: false,
      sync: !async,
      method: 'GET',
      handleAs: 'json',
      //timeout: tmOut,
      timeout: 0,
      ignoreForConnectionStatus: dataparams.parameters && dataparams.parameters.ignoreForConnectionStatus ? dataparams.parameters.ignoreForConnectionStatus : false,
      contentType: 'application/json;charset=utf-8',
      success: function (inSender, inResponse) {
        OB.DS.serviceGETRequests.pop(source);
        OB.UTIL.SynchronizationHelper.finished(synchId, 'serviceGET');
        serviceSuccess(inSender, inResponse, callback);
      },
      fail: function (inSender, inResponse) {
        OB.DS.serviceGETRequests.pop(source);
        OB.UTIL.SynchronizationHelper.finished(synchId, 'serviceGET');
        OB.UTIL.Debug.execute(function () {
          // show an error while in debug mode to help debugging and testing
          if (inSender && inSender === 401) {
            console.warn("serviceGET error: " + inSender + ", source: " + source);
          } else {
            console.error("serviceGET error: " + inSender + ", source: " + source);
          }
        });
        serviceError(inSender, inResponse, callback, callbackError);
      }
    });
    //Set paramaters received in dataparams.parameters
    if (dataparams.parameters) {
      _.each(_.keys(dataparams.parameters), function (key) {
        ajaxRequest[key] = dataparams.parameters[key];
      });
    }
    rr = new OB.RR.Request({
      ajaxRequest: ajaxRequest
    });
    rr.exec(source);
  }

  // Process object
  OB.DS.Process = function (source) {
    this.source = source;
  };

  OB.DS.Process.prepareData = function (params) {
    var data = {},
        i, attr;

    for (attr in params) {
      if (params.hasOwnProperty(attr)) {
        data[attr] = params[attr];
      }
    }

    if (OB.DS.commonParams) {
      for (i in OB.DS.commonParams) {
        if (OB.DS.commonParams.hasOwnProperty(i)) {
          data[i] = OB.DS.commonParams[i];
        }
      }
    }

    data.appName = OB.MobileApp.model.get('appName') || 'OBMOBC';
    return data;
  };

  OB.DS.Process.prototype.exec = function (params, callback, callbackError, async, timeout) {
    if (OB.DS.requestAllowed) {
      /* 
       * We can add extraParams calling exec function of OB.DS.Process, see example:
       *   params.extraParams = {
       *     example: true
       *  };
       */
      var data = OB.DS.Process.prepareData(params);

      // run all transactional services synchronized if synchronized mode is enabled
      if (OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true) && OB.RR.RequestRouter.isTransactionalService(this.source)) {
        var contentData = [];
        data._serviceName = this.source;

        contentData.push(data);

        OB.MobileApp.model.showSynchronizingDialog();
        this.source = 'org.openbravo.mobile.core.servercontroller.SynchronizedServerProcessCaller';
        var syncData = {
          messageId: params.messageId ? params.messageId : OB.UTIL.get_UUID(),
          _source: 'WEBPOS',
          _executeInOneServer: true,
          _tryCentralFromStore: true,
          posTerminal: OB.MobileApp.model.get('terminal').id,
          data: contentData,
          extraParams: {
            isSynchronizeModeTransaction: true
          }
        };
        syncData = OB.DS.Process.prepareData(syncData);

        servicePOST(this.source, syncData, function (args) {
          if (callback) {
            // unpack the synchronized result
            callback(args.result && args.result[0] && args.result[0].data ? args.result[0].data : args);
          }
          OB.MobileApp.model.hideSynchronizingDialog();
        }, function (args) {
          OB.MobileApp.model.hideSynchronizingDialog();
          if (callbackError) {
            // unpack the synchronized result
            callbackError(args.result && args.result[0] ? args.result[0] : args);
          }
        }, async, timeout);

      } else {
        servicePOST(this.source, data, callback, callbackError, async, timeout);
      }
    } else {
      OB.warn('Process ' + this.source + ' is not allowed at this moment. Skip execution of the process.');
    }
  };

  // Process object: This process adds a popup (Processing Transaction in the server)
  OB.DS.Process.FailOver = function (source) {
    this.source = source;
  };

  OB.DS.Process.FailOver.prototype.exec = function (params, callback, callbackError, async, timeout) {
    var process = new OB.DS.Process(this.source),
        oldCallback = callback,
        oldcallbackError = callbackError;
    callback = function (data) {
      OB.MobileApp.model.hideSynchronizingDialog();
      oldCallback(data);
    };
    callbackError = function (data) {
      OB.MobileApp.model.hideSynchronizingDialog();
      oldcallbackError(data);
    };
    OB.MobileApp.model.showSynchronizingDialog();
    process.exec(params, callback, callbackError, async, timeout);
  };

  // Source object
  OB.DS.Request = function (source, lastUpdated) {
    this.model = source && source.prototype && source.prototype.modelName && source; // we're using a Backbone.Model as source
    this.source = (this.model && this.model.prototype.source) || source; // we're using a plain String as source
    OB.UTIL.Debug.execute(function () {
      if (!this.source) {
        console.error("A Request must have a source");
      }
    }, this);

    this.lastUpdated = lastUpdated;
  };

  OB.DS.Request.prototype.exec = function (params, callback, callbackError, async, timeout) {
    var p, i;
    var data = {};
    if (OB.DS.requestAllowed) {
      if (params) {
        p = {};
        for (i in params) {
          if (params.hasOwnProperty(i)) {
            //Parameters starting with '_' are not query params so we will set them in data object
            if (i.startsWith('_')) {
              data[i] = params[i];
            } else {
              if (typeof params[i] === 'string') {
                p[i] = {
                  value: params[i],
                  type: 'string'
                };
              } else if (typeof params[i] === 'number') {
                if (params[i] === Math.round(params[i])) {
                  p[i] = {
                    value: params[i],
                    type: 'long'
                  };
                } else {
                  p[i] = {
                    value: params[i],
                    type: 'bigdecimal'
                  };
                }
              } else if (typeof params[i] === 'boolean') {
                p[i] = {
                  value: params[i],
                  type: 'boolean'
                };
              } else {
                p[i] = params[i];
              }
            }
          }
        }
        data.parameters = p;
      }

      if (OB.DS.commonParams) {
        for (i in OB.DS.commonParams) {
          if (OB.DS.commonParams.hasOwnProperty(i)) {
            data[i] = OB.DS.commonParams[i];
          }
        }
      }
      if (this.lastUpdated) {
        data.lastUpdated = this.lastUpdated;
      }

      data.appName = OB.MobileApp.model.get('appName') || 'OBMOBC';

      serviceGET(this.source, data, callback, callbackError, async, timeout);
    } else {
      OB.warn('Request ' + this.source + ' is not allowed at this moment. Skip execution of the request.');
    }
  };

  function check(elem, filter) {
    var p;

    for (p in filter) {
      if (filter.hasOwnProperty(p)) {
        if (typeof (filter[p]) === 'object') {
          return check(elem[p], filter[p]);
        } else {
          if (filter[p].substring(0, 2) === '%i') {
            if (!new RegExp(filter[p].substring(2), 'i').test(elem[p])) {
              return false;
            }
          } else if (filter[p].substring(0, 2) === '%') {
            if (!new RegExp(filter[p].substring(2)).test(elem[p])) {
              return false;
            }
          } else if (filter[p] !== elem[p]) {
            return false;
          }
        }
      }
    }
    return true;
  }

  function findInData(data, filter) {
    var i, max;

    if ($.isEmptyObject(filter)) {
      return {
        exception: 'filter not defined'
      };
    } else {
      for (i = 0, max = data.length; i < max; i++) {
        if (check(data[i], filter)) {
          return data[i];
        }
      }
      return null;
    }
  }

  function execInData(data, filter, filterfunction) {
    var newdata, info, i, max, f, item;

    if ($.isEmptyObject(filter) && !filterfunction) {
      return {
        data: data.slice(0, OB.DS.MAXSIZE),
        info: (data.length > OB.DS.MAXSIZE ? 'OBMOBC_DataMaxReached' : null)
      };
    } else {
      f = filterfunction ||
      function (item) {
        return item;
      };
      newdata = [];
      info = null;
      for (i = 0, max = data.length; i < max; i++) {
        if (check(data[i], filter)) {
          item = f(data[i]);
          if (item) {
            if (newdata.length >= OB.DS.MAXSIZE) {
              info = 'OBMOBC_DataMaxReached';
              break;
            }
            newdata.push(data[i]);
          }
        }
      }
      return {
        data: newdata,
        info: info
      };
    }
  }

  // DataSource objects
  // OFFLINE GOES HERE
  OB.DS.DataSource = function (request) {
    this.request = request;
    this.cache = null;
  };
  _.extend(OB.DS.DataSource.prototype, Backbone.Events);

  OB.DS.DataSource.prototype.load = function (params, incremental) {
    this.modelPagination = 1;
    OB.info('[sdreresh-' + (incremental ? 'inc' : 'full') + '] The model ' + this.request.model.prototype.modelName + ' has started masterdata load');
    var me = this,
        handleError, handleIncrementalRequest, dataLoaded = 0;
    OB.UTIL.localStorage.setItem('recordsFromBackendFor' + me.request.model.prototype.modelName, 0);
    handleError = function () {
      OB.error("Error in table '" + me.request.model.prototype.modelName + "' during initCache or insertData: " + arguments);

      me.trigger('ready', 'failed');
    };
    handleIncrementalRequest = function (limit, offset, params, incremental) {
      params = params || {};
      params._limit = limit;
      params._offset = offset;
      params._count = OB.UTIL.localStorage.getItem('recordsFromBackendFor' + me.request.model.prototype.modelName) ? parseInt(OB.UTIL.localStorage.getItem('recordsFromBackendFor' + me.request.model.prototype.modelName), 10) : 0;
      params._isMasterdata = true;
      params.overrideDefaultTimeout = true;
      if (offset === 0) {
        
        console.log('VALOR DEL INCREMENTAL: ', incremental);
        if(me.request.model.prototype.modelName === 'BusinessPartner' || me.request.model.prototype.modelName === 'BPLocation'){
          var d = new Date();
          d.setDate(d.getDate() - 130);
          console.log('NOMBRE DEL MODELO: ', me.request.model.prototype.modelName);
          console.log('HORA UNIX: ', d.getTime());
          if(incremental){
            OB.UTIL.localStorage.setItem('requestTimestamp' + me.request.model.prototype.modelName, (new Date()).getTime());
          }else{
            OB.UTIL.localStorage.setItem('requestTimestamp' + me.request.model.prototype.modelName, d.getTime());            
          }
          
        }else{
          OB.UTIL.localStorage.setItem('requestTimestamp' + me.request.model.prototype.modelName, (new Date()).getTime());
        }

        //OB.UTIL.localStorage.setItem('requestTimestamp' + me.request.model.prototype.modelName, (new Date()).getTime());
        OB.UTIL.showLoadingMessage(OB.I18N.getLabel('OBMOBC_LoadingMessageModel', [me.request.model.prototype.modelName]));
      } else {
        OB.UTIL.showLoadingMessage(OB.I18N.getLabel('OBMOBC_LoadingMessageModelPage', [me.request.model.prototype.modelName, offset / limit + 1]));
      }
      me.request.exec(params, function (data, message, lastUpdated, totalRows) {
        function success() {
          dataLoaded += data.length;
          if (data.length >= limit) {
            OB.debug('[sdreresh-' + (incremental ? 'inc' : 'full') + '] The model ' + me.request.model.prototype.modelName + ' has loaded from ' + offset + ' to ' + (offset + limit + 1) + ' with a ' + data.length + ' records in the pagination ' + me.modelPagination);
            me.modelPagination = me.modelPagination + 1;
            //remove the lastUpdated timestamp while the paged request is being processed to prevent half loaded models
            OB.UTIL.localStorage.removeItem('lastUpdatedTimestamp' + me.request.model.prototype.modelName);
            OB.UTIL.localStorage.setItem('recordsFromBackendFor' + me.request.model.prototype.modelName, totalRows);
            handleIncrementalRequest(limit, offset + limit, params, incremental);
          } else {
            OB.UTIL.completeLoadingStep();
            OB.debug('[sdreresh-' + (incremental ? 'inc' : 'full') + '] The model ' + me.request.model.prototype.modelName + ' has loaded from ' + offset + ' to ' + (offset + limit + 1) + ' with a ' + data.length + ' records in the pagination ' + me.modelPagination);
            OB.info('[sdreresh-' + (incremental ? 'inc' : 'full') + '] The model ' + me.request.model.prototype.modelName + ' has finished loading with a total of ' + dataLoaded + ' records. ' + (me.modelPagination === 1 ? 'There was no pagination.' : 'The number of paginations created were ' + me.modelPagination));
            if (lastUpdated && data.length > 0) {
              if (OB.UTIL.localStorage.getItem('requestTimestamp' + me.request.model.prototype.modelName) && OB.UTIL.localStorage.getItem('requestTimestamp' + me.request.model.prototype.modelName) !== 'null') {
                OB.UTIL.localStorage.setItem('lastUpdatedTimestamp' + me.request.model.prototype.modelName, OB.UTIL.localStorage.getItem('requestTimestamp' + me.request.model.prototype.modelName));
              } else {
                OB.error('[lastUpdatedTimestamp] ' + 'local storage (lastUpdatedTimestamp' + me.request.model.prototype.modelName + ') attempted to be set as null. Ignored. Current Value: ' + OB.UTIL.localStorage.getItem('lastUpdatedTimestamp' + me.request.model.prototype.modelName));
              }
              OB.UTIL.localStorage.removeItem('requestTimestamp' + me.request.model.prototype.modelName);
              OB.UTIL.localStorage.setItem('recordsFromBackendFor' + me.request.model.prototype.modelName, totalRows);
            } else {
              if (OB.UTIL.localStorage.getItem('requestTimestamp' + me.request.model.prototype.modelName) && OB.UTIL.localStorage.getItem('requestTimestamp' + me.request.model.prototype.modelName) !== 'null') {
                OB.UTIL.localStorage.setItem('lastUpdatedTimestamp' + me.request.model.prototype.modelName, OB.UTIL.localStorage.getItem('requestTimestamp' + me.request.model.prototype.modelName));
              }
            }
            me.trigger('ready');
          }
        }
        if (data.exception) {
          OB.error('Error in datasource: ' + data.exception);
          me.trigger('ready', 'failed');
          return;
        }
        if (me.request.model && me.request.model.prototype.online) {
          me.cache = (me.cache ? me.cache.concat(data) : data);
        }
        if (me.request.model && !me.request.model.prototype.online) {
          if (offset === 0) {
            OB.Dal.initCache(me.request.model, data, success, handleError, incremental);
          } else {
            OB.Dal.insertData(me.request.model, data, success, handleError, incremental, dataLoaded);
          }
        } else {
          success();
        }
      }, function (data) {
        OB.UTIL.Debug.execute(function () {
          if (data && data.exception && data.exception.message) {
            OB.error("OB.DS.DataSource.prototype.exec: " + data.exception.message);
            return;
          }
        });
        if (data && data.exception && data.exception.message && data.exception.message === 'Application server is not available.') {
          me.trigger('ready', 'timeout');
        } else {
          me.trigger('ready', 'failed');
        }
      }, true, incremental ? OB.RR.RequestRouter.getServiceByName(me.request.source).getServiceTimeout(5000) : OB.RR.RequestRouter.getServiceByName(me.request.source).getServiceTimeout(5000) * 20);
    };

    this.cache = null;
    handleIncrementalRequest(OB.MobileApp.model.hasPermission('OBMOBC_MasterdataBatchSize', true) ? OB.DEC.abs(OB.MobileApp.model.hasPermission('OBMOBC_MasterdataBatchSize', true)) : 10000, 0, params, incremental);
  };

  OB.DS.DataSource.prototype.find = function (filter, callback) {
    if (this.cache) {
      callback(findInData(this.cache, filter));
    } else {
      this.on('ready', function () {
        callback(findInData(this.cache, filter));
      }, this);
    }
  };

  OB.DS.DataSource.prototype.exec = function (filter, callback) {
    if (this.cache) {
      var result1 = execInData(this.cache, filter);
      callback(result1.data, result1.info);
    } else {
      this.on('ready', function () {
        var result2 = execInData(this.cache, filter);
        callback(result2.data, result2.info);
      }, this);
    }
  };
}());