/*
 ************************************************************************************
 * Copyright (C) 2012-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, console, _, enyo, Uint8Array, $, moment */

(function () {

  OB.UTIL = window.OB.UTIL || {};

  OB.UTIL.preventRefreshByUser = function () {
    if (OB.UTIL.preventingRefreshByUser) {
      return;
    }
    OB.UTIL.preventingRefreshByUser = true;
    document.addEventListener('keydown', OB.UTIL.captureRefreshKeys);
    window.addEventListener('beforeunload', OB.UTIL.capturePageLeave);
  };

  OB.UTIL.captureRefreshKeys = function (event) {
    switch (event.keyCode) {
    case 116:
      event.preventDefault();
      break;
    case 82:
      if (event.ctrlKey || event.metaKey) {
        event.preventDefault();
        break;
      }
      break;
    }
  };

  OB.UTIL.capturePageLeave = function (ev) {
    // note: message will not be displayed, return non-null will force the popup to show up
    ev.returnValue = 'Data is being processed do not leave the page';
  };

  OB.UTIL.resetPreventRefreshByUser = function () {
    document.removeEventListener('keydown', OB.UTIL.captureRefreshKeys);
    window.removeEventListener('beforeunload', OB.UTIL.capturePageLeave);
    OB.UTIL.preventingRefreshByUser = false;
  };

  OB.UTIL.getParameterByName = function (name) {
    var n = name.replace(/[\[]/, '\\[').replace(/[\]]/, '\\]');
    var regexS = '[\\?&]' + n + '=([^&#]*)';
    var regex = new RegExp(regexS);
    var results = regex.exec(window.location.search);
    return (results) ? decodeURIComponent(results[1].replace(/\+/g, ' ')) : '';
  };
  OB.UTIL.isDisableDiscount = function (receipt, callback) {
    if (OB.Model.Discounts && OB.Model.Discounts.getManualPromotions) {
      if (receipt.get('lines').length > 0) {
        // Set disable promotion discount property
        OB.Dal.findUsingCache('ManualDiscountsExist', OB.Model.Discount, {
          _whereClause: "where m_offer_type_id in (" + OB.Model.Discounts.getManualPromotions() + ")"
        }, function (promos) {
          if (promos.length === 0) {
            callback(true);
          } else {
            callback(false);
          }
        }, {
          modelsAffectedByCache: ['Discount']
        }, function () {
          callback(true);
        });
      } else {
        callback(true);
      }
    } else {
      callback(true);
    }
  };
  OB.UTIL.escapeRegExp = function (text) {
    return text.replace(/[\-\[\]{}()+?.,\\\^$|#\s]/g, '\\$&');
  };

  function S4() {
    return (Math.floor((1 + Math.random()) * 0x10000)).toString(16).substring(1).toUpperCase();
  }

  OB.UTIL.get_UUID = function () {
    var array;
    var uuid = "",
        i, digit = "";
    if (window.crypto && window.crypto.getRandomValues) {
      array = new Uint8Array(16);
      window.crypto.getRandomValues(array);

      for (i = 0; i < array.length; i++) {
        digit = array[i].toString(16).toUpperCase();
        if (digit.length === 1) {
          digit = "0" + digit;
        }
        uuid += digit;
      }

      return uuid;
    }

    return (S4() + S4() + S4() + S4() + S4() + S4() + S4() + S4());
  };

  OB.UTIL.padNumber = function (n, p) {
    var s = n.toString();
    while (s.length < p) {
      s = '0' + s;
    }
    return s;
  };

  OB.UTIL.encodeXMLComponent = function (s) {
    if (s && s.replace) {
      return s.replace(/\&/g, '&amp;').replace(/</g, '&lt;').replace(/\>/g, '&gt;').replace(/\'/g, '&apos;').replace(/\"/g, '&quot;');
    } else {
      return '';
    }
  };

  OB.UTIL.decodeXMLComponent = function (s) {
    if (s && s.replace) {
      return s.replace(/\&amp\;/g, '&').replace(/\&lt\;/g, '<').replace(/\&gt\;/g, '>').replace(/\&apos\;/g, '\'').replace(/\&quot\;/g, '\"');
    } else {
      return '';
    }
  };

  /**
   * Prepares multi line string to be printed with HW Manager format
   */
  OB.UTIL.encodeXMLMultiLineComponent = function (str, width) {
    if (str && str.split) {
      var startBlock = '<line><text>',
          endBlock = '</text></line>\n',
          lines = str.split('\n'),
          l, line, result = '';


      for (l = 0; l < lines.length; l++) {
        line = lines[l].trim();
        if (width && line.length > width) {
          result += OB.UTIL.encodeXMLMultiLineComponent(OB.UTIL.wordwrap(line, width));
        } else {
          result += startBlock + OB.UTIL.encodeXMLComponent(line) + endBlock;
        }
      }

      return result;
    } else {
      return '';
    }
  };

  /**
   * Wraps words in several lines with width length
   */
  OB.UTIL.wordwrap = function (str, width) {
    if (!str || !width) {
      return str;
    }

    return str.match(RegExp('.{1,' + width + '}(\\s|$)|\\S+?(\\s|$)', 'g')).join('\n');
  };

  OB.UTIL.loadResource = function (res, callback, context) {
    var ajaxRequest = new enyo.Ajax({
      url: res,
      cacheBust: false,
      method: 'GET',
      handleAs: 'text',
      contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
      success: function (inSender, inResponse) {
        callback.call(context || this, inResponse);
      },
      fail: function () {
        callback.call(context || this);
      }
    });
    ajaxRequest.go().response('success').error('fail');
  };

  OB.UTIL.queueStatus = function (queue) {
    // Expects an object where the value element is true/false depending if is processed or not
    if (!_.isObject(queue)) {
      throw 'Object expected';
    }
    return _.reduce(queue, function (memo, val) {
      return memo && val;
    }, true);
  };

  OB.UTIL.checkContextChange = function (oldContext, newContext, successCallback) {
    if (newContext.userId !== oldContext.user.id || newContext.orgId !== oldContext.organization.id || newContext.clientId !== oldContext.client.id || newContext.roleId !== oldContext.role.id) {
      OB.warn("The context has changed");
      OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_ContextChanged'), OB.I18N.getLabel('OBMOBC_ContextChangedMessage'), [{
        isConfirmButton: true,
        label: OB.I18N.getLabel('OBMOBC_LblOk'),
        action: function () {
          OB.MobileApp.model.lock();
          return true;
        }
      }]);
    } else {
      if (successCallback) {
        successCallback();
      }
    }
  };

  OB.UTIL.checkConnectivityStatus = function (connectedCallback, notConnectedCallback) {
    var currentlyConnected = OB.MobileApp.model.get('connectedToERP');
    var oldContext = OB.MobileApp.model.get('context');
    if (currentlyConnected && oldContext) {
      new OB.DS.Request('org.openbravo.mobile.core.login.ContextInformation').exec({
        terminal: OB.MobileApp.model.get('terminalName'),
        ignoreForConnectionStatus: true
      }, function (data) {
        var newContext;

        if (!data) {
          return;
        }
        if (data && data.exception) {
          OB.MobileApp.model.lock();
        }
        if (data[0]) {
          newContext = data[0];
          OB.UTIL.checkContextChange(oldContext, newContext, connectedCallback);
        }
      }, function () {
        // if single server then that server controls offline/online
        if (OB.MobileApp.model && OB.MobileApp.model.get('connectedToERP') && OB.RR.RequestRouter.servers.length === 1) {
          OB.MobileApp.model.triggerOffLine();
        }
        if (notConnectedCallback) {
          notConnectedCallback();
        }
      }, true, 20000);
      return;
    } else if (navigator.onLine) {
      // It can be a false positive, make sure with the ping
      OB.UTIL.checkOffLineConnectivity(20000, connectedCallback, notConnectedCallback);
    } else {
      if (currentlyConnected) {
        if (OB.MobileApp.model) {
          OB.MobileApp.model.triggerOffLine();
        }
      }
    }
  };

  OB.UTIL.checkOffLineConnectivity = function (timeout, connectedCallback, notConnectedCallback) {
    // make sure of offline with the ping
    var rr, ajaxRequest = new enyo.Ajax({
      url: '../../org.openbravo.mobile.core/MobileSessionActive?id=0',
      cacheBust: true,
      timeout: timeout,
      method: 'GET',
      handleAs: 'json',
      contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
      success: function () {
        if (connectedCallback) {
          connectedCallback();
        }
      },
      fail: function () {
        if (notConnectedCallback) {
          notConnectedCallback();
        }
      }
    });
    rr = new OB.RR.Request({
      ajaxRequest: ajaxRequest
    });
    rr.exec(ajaxRequest.url);
  };

  OB.UTIL.updateDocumentSequenceInDB = function (documentNo) {
    var criteria = {
      'posSearchKey': OB.MobileApp.model.get('terminal').searchKey
    };
    OB.Dal.find(OB.Model.DocumentSequence, criteria, function (documentSequenceList) {
      var posDocumentNoPrefix = OB.MobileApp.model.get('terminal').docNoPrefix,
          orderDocumentSequence = parseInt(documentNo.substr(posDocumentNoPrefix.length + 1), 10) + 1,
          docSeqModel;
      if (documentSequenceList && documentSequenceList.length !== 0) {
        docSeqModel = documentSequenceList.at(0);
        if (orderDocumentSequence > docSeqModel.get('documentSequence')) {
          docSeqModel.set('documentSequence', orderDocumentSequence);
        }
      } else {
        docSeqModel = new OB.Model.DocumentSequence();
        docSeqModel.set('posSearchKey', OB.MobileApp.model.get('terminal').searchKey);
        docSeqModel.set('documentSequence', orderDocumentSequence);
      }
      OB.Dal.save(docSeqModel, null, null);
    });
  };

  OB.UTIL.getCharacteristicValues = function (characteristicDescripcion) {
    var ch_desc = '';
    _.each(characteristicDescripcion.split(','), function (character, index, collection) {
      ch_desc = ch_desc + character.substring(character.indexOf(':') + 2);
      if (index !== collection.length - 1) {
        ch_desc = ch_desc + ', ';
      }
    }, this);
    return ch_desc;
  };

  //Returns true if the value is null or undefined
  OB.UTIL.isNullOrUndefined = function (value) {
    if (_.isNull(value) || _.isUndefined(value)) {
      return true;
    }
    return false;
  };

  //Returns the first Not null and not undefined value.
  //Can be used in the same way as we use a = b || c; But using this function 0 will be a valid value for b
  OB.UTIL.getFirstValidValue = function (valuesToCheck) {
    var valueToReturn = _.find(valuesToCheck, function (value) {
      if (!OB.UTIL.isNullOrUndefined(value)) {
        return true;
      }
    });
    return valueToReturn;
  };

  function processConsoleLevel(logLevel, args) {
    try {

      var saveInServer = OB.UTIL.checkPermissionLog(logLevel, "save");
      if (saveInServer) {
        var isSaveOnlyTraceOfCaller = true;
        if (logLevel === 'Error' || logLevel === "Critical") {
          isSaveOnlyTraceOfCaller = false;
        }
        var serverMessage = OB.UTIL.argumentsToStringifyed(args) + "; stackTrace: " + OB.UTIL.getStackTrace('OB.' + logLevel.toLowerCase(), isSaveOnlyTraceOfCaller);
        // cut the message
        serverMessage = (serverMessage.length < 100000000) ? serverMessage : serverMessage.substring(0, 100000000);
        OB.UTIL.saveLogClient(serverMessage, logLevel);
      }

      var showInConsole = OB.UTIL.checkPermissionLog(logLevel, "console");
      if (OB.UTIL.Debug.isDebug() || showInConsole) {
        saveInServer = saveInServer && (OB && OB.MobileApp && OB.MobileApp.model && OB.MobileApp.model.get('logClientStatus') === 'OK');
        var tags = (showInConsole || saveInServer) ? ("(" + (showInConsole ? "*" : "") + ((showInConsole && saveInServer) ? "," : "") + (saveInServer ? "+" : "") + ")") : "";
        var consoleMessage = OB.UTIL.argumentsToStringifyed(args) + "; line: " + OB.UTIL.getStackTrace('OB.' + logLevel.toLowerCase(), true) + " " + tags;
        if (logLevel === "Debug" || logLevel === "Trace") {
          console.debug(consoleMessage);
        } else if (logLevel === "Info") {
          console.info(consoleMessage);
        } else if (logLevel === "Warn") {
          console.warn(consoleMessage);
        } else if (logLevel === "Error" || logLevel === "Critical") {
          console.error(consoleMessage);
        } else {
          OB.UTIL.Debug.execute(function () {
            console.error("The '" + logLevel + "' log level is not implemented");
          });
        }
      }

    } catch (e) {
      console.error(e.message + "; arguments:", args);
    }
  }

  OB.trace = function () {
    processConsoleLevel('Trace', arguments);
  };

  OB.debug = function () {
    processConsoleLevel('Debug', arguments);
  };

  OB.info = function () {
    processConsoleLevel('Info', arguments);
  };

  OB.warn = function () {
    processConsoleLevel('Warn', arguments);
  };

  OB.error = function () {
    processConsoleLevel('Error', arguments);
  };

  // deprecated
  OB.critical = function () {
    processConsoleLevel('Critical', arguments);
  };

  // this function receive as arguments first the level of log, the rest of the arguments are the message
  // deprecated. use OB.debug, OB.info, OB.warn or OB.error instead
  OB.log = function () {
    var level, argsWithoutFirst, i, msg;
    try {
      level = arguments[0];
      argsWithoutFirst = [];
      for (i = 1; i < arguments.length; i++) {
        argsWithoutFirst[i - 1] = arguments[i];
      }

      msg = OB.UTIL.composeMessage(argsWithoutFirst);

      if (OB.UTIL.checkPermissionLog(level, "save")) {
        OB.UTIL.saveLogClient(msg, level);
      }
      if (OB.UTIL.checkPermissionLog(level, "console")) {
        if (level === "Info") {
          console.info.apply(console, OB.UTIL.argumentsWithLink(argsWithoutFirst));
        } else if (level === "Warn") {
          console.warn.apply(console, OB.UTIL.argumentsWithLink(argsWithoutFirst));
        } else if (level === "Error" || level === "Critical") {
          console.error.apply(console, OB.UTIL.argumentsWithLink(argsWithoutFirst));
        } else {
          console.log.apply(console, OB.UTIL.argumentsWithLink(argsWithoutFirst));
        }
      }
    } catch (e) {
      console.error.apply(console, arguments);
    }
  };

  OB.UTIL.saveLogClient = function (msg, level) {
    try {
      if (OB.MobileApp && OB.MobileApp.model && OB.MobileApp.model.supportLogClient()) {
        if (!OB.Data.localDB) {
          // don't log if the local database is not ready
          OB.MobileApp.model.set('logClientStatus', 'NA');
          return;
        }

        var logClientModel = new OB.Model.LogClient();

        // verify that the model is initialized
        if (!logClientModel.modelName) {
          console.warn("the local database must be ready for the LogClient to save log");
          OB.MobileApp.model.set('logClientStatus', 'NA');
          return;
        }

        var date = new Date();
        logClientModel.set('created', date.getTime());
        logClientModel.set('createdby', OB.MobileApp.model.get('orgUserId'));
        logClientModel.set('loglevel', level);
        logClientModel.set('msg', msg);
        logClientModel.set('deviceId', OB.MobileApp.model.get('logConfiguration').deviceIdentifier);
        logClientModel.set('cacheSessionId', null);
        if (OB.UTIL.localStorage.getItem('cacheSessionId') !== null) {
          logClientModel.set('cacheSessionId', OB.UTIL.localStorage.getItem('cacheSessionId'));
        }
        logClientModel.set('link', OB.UTIL.getStackLink());

        _.each(OB.MobileApp.model.get('logConfiguration').logPropertiesExtension, function (f) {
          logClientModel.set(f());
        });
        logClientModel.set('json', JSON.stringify(logClientModel.toJSON()));

        OB.Dal.save(logClientModel, function () {
          // success
          if (OB.MobileApp.model.get('logClientStatus') !== 'OK') {
            console.debug("LogClient is available");
          }
          OB.MobileApp.model.set('logClientStatus', 'OK');
        }, function () {
          // failure
          // ob.dal skips showing this errors because they should not be shown in the error log level
          // this errors are because the assigned storage is not ready or is failing
          // still log it, as we want to know that an error happened, but only once and in debug level
          if (OB.MobileApp.model.get('logClientStatus') !== 'NA') {
            console.debug("LogClient is not available");
          }
          OB.MobileApp.model.set('logClientStatus', 'NA');
        });
      }
    } catch (e) {
      console.error("saveLogClient " + e);
    }
  };

  // it checks if the log should be saved in backend (property 'OBMOBC_logClient.saveLog') and the level saved (property 'OBMOBC_logClient.levelLog')
  // or the level of log should be displayed in console (property 'OBMOBC_logClient.consoleLevelLog')
  // "type "will be "save" to check if the log should be saved in backend or "console" to check if the log should be displayed in console
  OB.UTIL.checkPermissionLog = function (level, type) {
    try {
      if ((OB.MobileApp && OB.MobileApp.model && OB.MobileApp.model.get('permissions') !== null) === false) {
        // save important log messages even if the preference is unknown
        if (level === 'Warn' || level === 'Error' || level === 'Critical') {
          return true;
        }
        return false;
      }

      if (type === "save") {
        if (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.saveLog'] === false) {
          return false;
        }
        if (level === "Trace") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Trace');
        }
        if (level === "Debug") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Debug');
        }
        if (level === "Info") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Debug' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Info');
        }
        if (level === "Warn") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Debug' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Info' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Warn');
        }
        if (level === "Error") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Debug' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Info' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Warn' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Error');
        }
        if (level === "Critical") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Debug' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Info' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Warn' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Error' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.levelLog'] === 'Critical');
        }
      } else {
        if (level === "Trace") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Trace');
        }
        if (level === "Debug") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Debug');
        }
        if (level === "Info") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Debug' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Info');
        }
        if (level === "Warn") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Debug' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Info' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Warn');
        }
        if (level === "Error") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Debug' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Info' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Warn' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Error');
        }
        if (level === "Critical") {
          return (OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Trace' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Debug' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Info' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Warn' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Error' || OB.MobileApp.model.get('permissions')['OBMOBC_logClient.consoleLevelLog'] === 'Critical');
        }
      }
      return true;
    } catch (e) {
      return true;
    }
  };

  OB.UTIL.getStackLink = function (deepness) {
    deepness = deepness || 4;
    try {
      var errorobj = new Error();
      var link = errorobj.stack.split('\n')[deepness].split('at ')[1];
      if (link.indexOf('(') >= 0) {
        link = link.split('(')[1];
        link = link.substring(0, link.length - 1);
      }
      if (link) {
        return link;
      }
    } catch (e) {
      console.error("OB.UTIL.getStackLink: " + e);
    }
    return '';
  };

  OB.UTIL.getStackTrace = function (callerName, isReturnOnlyCallerParent) {
    // argument checks
    OB.UTIL.Debug.isDefined(isReturnOnlyCallerParent, "Missing 'isReturnOnlyCallerParent' argument in 'getStackTrace");

    function replacements(element) {
      if (typeof (element) === 'string') {
        return element.replace("null.<anonymous>", "<async response>");
      }
      return element;
    }

    var stackTrace = '';
    try {
      var errorobj = new Error();
      var links = errorobj.stack.split('\n');
      var i;
      var isTraceStartingPointReached = false;
      for (i = 0; i < links.length; i++) {
        var linkCandidate = links[i];
        if (!isTraceStartingPointReached && linkCandidate.indexOf(callerName) >= 0) {
          isTraceStartingPointReached = true;
        } else if (isTraceStartingPointReached) {
          linkCandidate = linkCandidate.split('at ')[1];
          stackTrace += (stackTrace.length > 0 ? ";\n" : "") + replacements(linkCandidate);
          if (isReturnOnlyCallerParent) {
            return stackTrace;
          }
        }
      }
    } catch (e) {
      console.error("OB.UTIL.getStackTrace: " + e);
    }
    return stackTrace;
  };

  OB.UTIL.argumentsWithLink = function (args) {
    var arrayArgs, i;
    try {
      arrayArgs = [];
      for (i = 0; i < args.length; i++) {
        arrayArgs.push(args[i]);
      }
      arrayArgs.push(OB.UTIL.getStackLink());
      return arrayArgs;
    } catch (e) {
      console.error("OB.UTIL.argumentsWithLink: " + e);
      return '';
    }
  };

  OB.UTIL.argumentsToStringifyed = function () {

    function replacer(key, value) {
      if (typeof value === 'function') {
        return value.toString().replace(/\s{2,}/g, ' ');
      }
      return value;
    }

    function flattenArray(array) {
      var stringifyed = '';
      _.each(array, function (element) {
        if (element) {
          if (stringifyed !== '') {
            stringifyed += '; ';
          }
          if (typeof (element) === 'string') {
            stringifyed += element;
          } else if (element.length) {
            if (element.length > 0) {
              stringifyed += flattenArray(element);
            } else {
              stringifyed += flattenArray(element[0]);
            }
          } else if (element.DATABASE_ERR !== undefined && element.code !== undefined && element.message !== undefined) {
            stringifyed += "SQLError: code: " + element.code + ", message: " + element.message;
          } else {
            stringifyed += JSON.stringify(element, replacer, 0);
          }
        }
      });
      return stringifyed;
    }

    try {
      return flattenArray(arguments);
    } catch (e) {
      console.error("OB.UTIL.argumentsToStringifyed: " + e);
    }
    return '';
  };

  // deprecated
  OB.UTIL.composeMessage = function (args) {
    var msg = '';
    _.each(args, function (arg) {
      if (msg !== '') {
        msg += '   ';
      }
      if ((!(_.isNull(arg) || _.isUndefined(arg))) && _.isObject(arg)) {
        var stringifyed = JSON.stringify(arg);
        if (stringifyed) {
          // add the stringifyed arg to the msg. if the stringify string is too long, cut it
          msg += (stringifyed.length < 1000) ? stringifyed : stringifyed.substring(0, 1000);
        }
      }
    });
    return msg;
  };

  OB.UTIL.isIOS = function () {
    return navigator.userAgent.match(/iPhone|iPad|iPod/i);
  };

  function initializeCloneTarget(object, target) {
    if (target) {
      return target;
    } else if (object.constructor) {
      return new object.constructor();
    }
    return {};
  }

  function eventDetecterDaemon(event, object) {
    var errorMessage = "OB.UTIL.clone: Events are not allowed to be fired while cloning an object. Detected event fired: '" + event + "'. Emitter model: '" + object.modelName + "' (cid: " + object.cid + ")";
    if (event === 'add' || event === 'selected') {
      console.error(errorMessage);
      return;
    }
    throw errorMessage;
  }

  var objectsWithEventDetecterDaemonAttached = null;

  function cloneRecursively(args, source, target) {
    var clonedObject;
    if (typeof (source) === 'object') {
      clonedObject = initializeCloneTarget(source, target);
      // attach the eventDetecterDaemon if the model is not the UI receipt
      var isSilent = !args.isTriggerEventsIfTargetOfSourceWhenCloning;
      if (source.on) {
        if (isSilent) {
          // attach a daemon to verify that no event is fired
          objectsWithEventDetecterDaemonAttached.push(clonedObject);
          clonedObject.on("all", eventDetecterDaemon);
        }
      } else {
        throw OB.UTIL.argumentsToStringifyed("OB.UTIL.clone do not implement non-backbone objects. Related source:", source);
      }
      _.each(_.keys(source.attributes), function (key) {
        if (!_.isUndefined(source.get(key))) {
          if (source.get(key) === null) {
            clonedObject.set(key, null, {
              silent: isSilent
            });
          } else if (source.get(key).at) {
            //collection
            clonedObject.get(key).reset();
            source.get(key).forEach(function (elem) {
              clonedObject.get(key).add(cloneRecursively(args, elem), {
                silent: isSilent
              });
            });
          } else if (source.get(key).get) {
            //backboneModel
            clonedObject.set(key, cloneRecursively(args, source.get(key)), {
              silent: isSilent
            });
          } else if (_.isArray(source)) {
            //Array
            clonedObject.set(key, [], {
              silent: isSilent
            });
            source.get(key).forEach(function (elem) {
              clonedObject.get(key).push(cloneRecursively(args, elem));
            });
          } else {
            //property
            clonedObject.set(key, source.get(key), {
              silent: isSilent
            });
          }
        }
      });
      // remove any property not present in the original source
      _.each(_.keys(clonedObject.attributes), function (key) {
        if (_.isUndefined(source.get(key))) {
          clonedObject.unset(key, {
            silent: isSilent
          });
        }
      });
    } else {
      OB.UTIL.Debug.execute(function () {
        throw "cloneRecursively: Cannot clone the source because is not of type 'object'";
      });
    }
    return clonedObject;
  }

  // count of the clones created so far
  var cloneCount = 0;

  /*
   * Copies all the properties of 'object' in 'target'
   * This method doesn't fire events
   */
  OB.UTIL.clone = function (source, target) {
    cloneCount += 1;
    objectsWithEventDetecterDaemonAttached = [];
    var clonedObject = initializeCloneTarget(source, target);

    // decide if events will be fired
    var isTriggerEventsIfTargetOfSourceWhenCloning = target && target.triggerEventsIfTargetOfSourceWhenCloning && target.triggerEventsIfTargetOfSourceWhenCloning();
    var args = {
      isTriggerEventsIfTargetOfSourceWhenCloning: isTriggerEventsIfTargetOfSourceWhenCloning
    };
    OB.debug("(" + cloneCount + ") Cloning" + (isTriggerEventsIfTargetOfSourceWhenCloning ? " firing events" : "") + ": '" + source.modelName + "' (cid: " + source.cid + " -> " + clonedObject.cid + ")");
    // create the clone
    var finalClonedObject = cloneRecursively(args, source, clonedObject);

    // detach all the attached eventDetecterDaemons
    objectsWithEventDetecterDaemonAttached.forEach(function (element) {
      element.off('all', eventDetecterDaemon);
    });

    OB.debug("Cloning finished: '" + source.modelName + "' (cid: " + source.cid + " -> " + clonedObject.cid + ")");
    return finalClonedObject;
  };

  OB.UTIL.checkSourceVersion = function (sourceVersion, forceReload) {
    if (!OB.UTIL.localStorage.getItem("SourceVersion_" + OB.MobileApp.model.get('appName'))) {
      OB.UTIL.localStorage.setItem("SourceVersion_" + OB.MobileApp.model.get('appName'), sourceVersion);
    } else {
      if (OB.UTIL.localStorage.getItem("SourceVersion_" + OB.MobileApp.model.get('appName')) !== sourceVersion) {
        if (forceReload) {
          OB.UTIL.localStorage.setItem("SourceVersion_" + OB.MobileApp.model.get('appName'), sourceVersion);
          window.location.reload();
          return;
        }
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_SourceVersionChanged'), OB.I18N.getLabel('OBMOBC_SourceVersionChangedMessage'), [{
          isConfirmButton: true,
          label: OB.I18N.getLabel('OBMOBC_LblOk'),
          action: function () {
            OB.UTIL.localStorage.setItem("SourceVersion_" + OB.MobileApp.model.get('appName'), sourceVersion);
            window.location.reload();
            return true;
          }
        }]);
      }
    }
  };

  OB.UTIL.executeCallbackQueue = function (queue, arg) {
    while (queue.length > 0) {
      var f = queue.shift();
      if (f) {
        f(arg);
      }
    }
  };

  OB.UTIL.unAccent = function (str) {
    var i;
    var defaultDiacriticsRemovalMap = [{
      'base': 'A',
      'letters': /[\u0041\u24B6\uFF21\u00C0\u00C1\u00C2\u1EA6\u1EA4\u1EAA\u1EA8\u00C3\u0100\u0102\u1EB0\u1EAE\u1EB4\u1EB2\u0226\u01E0\u00C4\u01DE\u1EA2\u00C5\u01FA\u01CD\u0200\u0202\u1EA0\u1EAC\u1EB6\u1E00\u0104\u023A\u2C6F]/g
    }, {
      'base': 'AA',
      'letters': /[\uA732]/g
    }, {
      'base': 'AE',
      'letters': /[\u00C6\u01FC\u01E2]/g
    }, {
      'base': 'AO',
      'letters': /[\uA734]/g
    }, {
      'base': 'AU',
      'letters': /[\uA736]/g
    }, {
      'base': 'AV',
      'letters': /[\uA738\uA73A]/g
    }, {
      'base': 'AY',
      'letters': /[\uA73C]/g
    }, {
      'base': 'B',
      'letters': /[\u0042\u24B7\uFF22\u1E02\u1E04\u1E06\u0243\u0182\u0181]/g
    }, {
      'base': 'C',
      'letters': /[\u0043\u24B8\uFF23\u0106\u0108\u010A\u010C\u00C7\u1E08\u0187\u023B\uA73E]/g
    }, {
      'base': 'D',
      'letters': /[\u0044\u24B9\uFF24\u1E0A\u010E\u1E0C\u1E10\u1E12\u1E0E\u0110\u018B\u018A\u0189\uA779]/g
    }, {
      'base': 'DZ',
      'letters': /[\u01F1\u01C4]/g
    }, {
      'base': 'Dz',
      'letters': /[\u01F2\u01C5]/g
    }, {
      'base': 'E',
      'letters': /[\u0045\u24BA\uFF25\u00C8\u00C9\u00CA\u1EC0\u1EBE\u1EC4\u1EC2\u1EBC\u0112\u1E14\u1E16\u0114\u0116\u00CB\u1EBA\u011A\u0204\u0206\u1EB8\u1EC6\u0228\u1E1C\u0118\u1E18\u1E1A\u0190\u018E]/g
    }, {
      'base': 'F',
      'letters': /[\u0046\u24BB\uFF26\u1E1E\u0191\uA77B]/g
    }, {
      'base': 'G',
      'letters': /[\u0047\u24BC\uFF27\u01F4\u011C\u1E20\u011E\u0120\u01E6\u0122\u01E4\u0193\uA7A0\uA77D\uA77E]/g
    }, {
      'base': 'H',
      'letters': /[\u0048\u24BD\uFF28\u0124\u1E22\u1E26\u021E\u1E24\u1E28\u1E2A\u0126\u2C67\u2C75\uA78D]/g
    }, {
      'base': 'I',
      'letters': /[\u0049\u24BE\uFF29\u00CC\u00CD\u00CE\u0128\u012A\u012C\u0130\u00CF\u1E2E\u1EC8\u01CF\u0208\u020A\u1ECA\u012E\u1E2C\u0197]/g
    }, {
      'base': 'J',
      'letters': /[\u004A\u24BF\uFF2A\u0134\u0248]/g
    }, {
      'base': 'K',
      'letters': /[\u004B\u24C0\uFF2B\u1E30\u01E8\u1E32\u0136\u1E34\u0198\u2C69\uA740\uA742\uA744\uA7A2]/g
    }, {
      'base': 'L',
      'letters': /[\u004C\u24C1\uFF2C\u013F\u0139\u013D\u1E36\u1E38\u013B\u1E3C\u1E3A\u0141\u023D\u2C62\u2C60\uA748\uA746\uA780]/g
    }, {
      'base': 'LJ',
      'letters': /[\u01C7]/g
    }, {
      'base': 'Lj',
      'letters': /[\u01C8]/g
    }, {
      'base': 'M',
      'letters': /[\u004D\u24C2\uFF2D\u1E3E\u1E40\u1E42\u2C6E\u019C]/g
    }, {
      'base': 'N',
      'letters': /[\u004E\u24C3\uFF2E\u01F8\u0143\u00D1\u1E44\u0147\u1E46\u0145\u1E4A\u1E48\u0220\u019D\uA790\uA7A4]/g
    }, {
      'base': 'NJ',
      'letters': /[\u01CA]/g
    }, {
      'base': 'Nj',
      'letters': /[\u01CB]/g
    }, {
      'base': 'O',
      'letters': /[\u004F\u24C4\uFF2F\u00D2\u00D3\u00D4\u1ED2\u1ED0\u1ED6\u1ED4\u00D5\u1E4C\u022C\u1E4E\u014C\u1E50\u1E52\u014E\u022E\u0230\u00D6\u022A\u1ECE\u0150\u01D1\u020C\u020E\u01A0\u1EDC\u1EDA\u1EE0\u1EDE\u1EE2\u1ECC\u1ED8\u01EA\u01EC\u00D8\u01FE\u0186\u019F\uA74A\uA74C]/g
    }, {
      'base': 'OI',
      'letters': /[\u01A2]/g
    }, {
      'base': 'OO',
      'letters': /[\uA74E]/g
    }, {
      'base': 'OU',
      'letters': /[\u0222]/g
    }, {
      'base': 'P',
      'letters': /[\u0050\u24C5\uFF30\u1E54\u1E56\u01A4\u2C63\uA750\uA752\uA754]/g
    }, {
      'base': 'Q',
      'letters': /[\u0051\u24C6\uFF31\uA756\uA758\u024A]/g
    }, {
      'base': 'R',
      'letters': /[\u0052\u24C7\uFF32\u0154\u1E58\u0158\u0210\u0212\u1E5A\u1E5C\u0156\u1E5E\u024C\u2C64\uA75A\uA7A6\uA782]/g
    }, {
      'base': 'S',
      'letters': /[\u0053\u24C8\uFF33\u1E9E\u015A\u1E64\u015C\u1E60\u0160\u1E66\u1E62\u1E68\u0218\u015E\u2C7E\uA7A8\uA784]/g
    }, {
      'base': 'T',
      'letters': /[\u0054\u24C9\uFF34\u1E6A\u0164\u1E6C\u021A\u0162\u1E70\u1E6E\u0166\u01AC\u01AE\u023E\uA786]/g
    }, {
      'base': 'TZ',
      'letters': /[\uA728]/g
    }, {
      'base': 'U',
      'letters': /[\u0055\u24CA\uFF35\u00D9\u00DA\u00DB\u0168\u1E78\u016A\u1E7A\u016C\u00DC\u01DB\u01D7\u01D5\u01D9\u1EE6\u016E\u0170\u01D3\u0214\u0216\u01AF\u1EEA\u1EE8\u1EEE\u1EEC\u1EF0\u1EE4\u1E72\u0172\u1E76\u1E74\u0244]/g
    }, {
      'base': 'V',
      'letters': /[\u0056\u24CB\uFF36\u1E7C\u1E7E\u01B2\uA75E\u0245]/g
    }, {
      'base': 'VY',
      'letters': /[\uA760]/g
    }, {
      'base': 'W',
      'letters': /[\u0057\u24CC\uFF37\u1E80\u1E82\u0174\u1E86\u1E84\u1E88\u2C72]/g
    }, {
      'base': 'X',
      'letters': /[\u0058\u24CD\uFF38\u1E8A\u1E8C]/g
    }, {
      'base': 'Y',
      'letters': /[\u0059\u24CE\uFF39\u1EF2\u00DD\u0176\u1EF8\u0232\u1E8E\u0178\u1EF6\u1EF4\u01B3\u024E\u1EFE]/g
    }, {
      'base': 'Z',
      'letters': /[\u005A\u24CF\uFF3A\u0179\u1E90\u017B\u017D\u1E92\u1E94\u01B5\u0224\u2C7F\u2C6B\uA762]/g
    }, {
      'base': 'a',
      'letters': /[\u0061\u24D0\uFF41\u1E9A\u00E0\u00E1\u00E2\u1EA7\u1EA5\u1EAB\u1EA9\u00E3\u0101\u0103\u1EB1\u1EAF\u1EB5\u1EB3\u0227\u01E1\u00E4\u01DF\u1EA3\u00E5\u01FB\u01CE\u0201\u0203\u1EA1\u1EAD\u1EB7\u1E01\u0105\u2C65\u0250]/g
    }, {
      'base': 'aa',
      'letters': /[\uA733]/g
    }, {
      'base': 'ae',
      'letters': /[\u00E6\u01FD\u01E3]/g
    }, {
      'base': 'ao',
      'letters': /[\uA735]/g
    }, {
      'base': 'au',
      'letters': /[\uA737]/g
    }, {
      'base': 'av',
      'letters': /[\uA739\uA73B]/g
    }, {
      'base': 'ay',
      'letters': /[\uA73D]/g
    }, {
      'base': 'b',
      'letters': /[\u0062\u24D1\uFF42\u1E03\u1E05\u1E07\u0180\u0183\u0253]/g
    }, {
      'base': 'c',
      'letters': /[\u0063\u24D2\uFF43\u0107\u0109\u010B\u010D\u00E7\u1E09\u0188\u023C\uA73F\u2184]/g
    }, {
      'base': 'd',
      'letters': /[\u0064\u24D3\uFF44\u1E0B\u010F\u1E0D\u1E11\u1E13\u1E0F\u0111\u018C\u0256\u0257\uA77A]/g
    }, {
      'base': 'dz',
      'letters': /[\u01F3\u01C6]/g
    }, {
      'base': 'e',
      'letters': /[\u0065\u24D4\uFF45\u00E8\u00E9\u00EA\u1EC1\u1EBF\u1EC5\u1EC3\u1EBD\u0113\u1E15\u1E17\u0115\u0117\u00EB\u1EBB\u011B\u0205\u0207\u1EB9\u1EC7\u0229\u1E1D\u0119\u1E19\u1E1B\u0247\u025B\u01DD]/g
    }, {
      'base': 'f',
      'letters': /[\u0066\u24D5\uFF46\u1E1F\u0192\uA77C]/g
    }, {
      'base': 'g',
      'letters': /[\u0067\u24D6\uFF47\u01F5\u011D\u1E21\u011F\u0121\u01E7\u0123\u01E5\u0260\uA7A1\u1D79\uA77F]/g
    }, {
      'base': 'h',
      'letters': /[\u0068\u24D7\uFF48\u0125\u1E23\u1E27\u021F\u1E25\u1E29\u1E2B\u1E96\u0127\u2C68\u2C76\u0265]/g
    }, {
      'base': 'hv',
      'letters': /[\u0195]/g
    }, {
      'base': 'i',
      'letters': /[\u0069\u24D8\uFF49\u00EC\u00ED\u00EE\u0129\u012B\u012D\u00EF\u1E2F\u1EC9\u01D0\u0209\u020B\u1ECB\u012F\u1E2D\u0268\u0131]/g
    }, {
      'base': 'j',
      'letters': /[\u006A\u24D9\uFF4A\u0135\u01F0\u0249]/g
    }, {
      'base': 'k',
      'letters': /[\u006B\u24DA\uFF4B\u1E31\u01E9\u1E33\u0137\u1E35\u0199\u2C6A\uA741\uA743\uA745\uA7A3]/g
    }, {
      'base': 'l',
      'letters': /[\u006C\u24DB\uFF4C\u0140\u013A\u013E\u1E37\u1E39\u013C\u1E3D\u1E3B\u017F\u0142\u019A\u026B\u2C61\uA749\uA781\uA747]/g
    }, {
      'base': 'lj',
      'letters': /[\u01C9]/g
    }, {
      'base': 'm',
      'letters': /[\u006D\u24DC\uFF4D\u1E3F\u1E41\u1E43\u0271\u026F]/g
    }, {
      'base': 'n',
      'letters': /[\u006E\u24DD\uFF4E\u01F9\u0144\u00F1\u1E45\u0148\u1E47\u0146\u1E4B\u1E49\u019E\u0272\u0149\uA791\uA7A5]/g
    }, {
      'base': 'nj',
      'letters': /[\u01CC]/g
    }, {
      'base': 'o',
      'letters': /[\u006F\u24DE\uFF4F\u00F2\u00F3\u00F4\u1ED3\u1ED1\u1ED7\u1ED5\u00F5\u1E4D\u022D\u1E4F\u014D\u1E51\u1E53\u014F\u022F\u0231\u00F6\u022B\u1ECF\u0151\u01D2\u020D\u020F\u01A1\u1EDD\u1EDB\u1EE1\u1EDF\u1EE3\u1ECD\u1ED9\u01EB\u01ED\u00F8\u01FF\u0254\uA74B\uA74D\u0275]/g
    }, {
      'base': 'oi',
      'letters': /[\u01A3]/g
    }, {
      'base': 'ou',
      'letters': /[\u0223]/g
    }, {
      'base': 'oo',
      'letters': /[\uA74F]/g
    }, {
      'base': 'p',
      'letters': /[\u0070\u24DF\uFF50\u1E55\u1E57\u01A5\u1D7D\uA751\uA753\uA755]/g
    }, {
      'base': 'q',
      'letters': /[\u0071\u24E0\uFF51\u024B\uA757\uA759]/g
    }, {
      'base': 'r',
      'letters': /[\u0072\u24E1\uFF52\u0155\u1E59\u0159\u0211\u0213\u1E5B\u1E5D\u0157\u1E5F\u024D\u027D\uA75B\uA7A7\uA783]/g
    }, {
      'base': 's',
      'letters': /[\u0073\u24E2\uFF53\u00DF\u015B\u1E65\u015D\u1E61\u0161\u1E67\u1E63\u1E69\u0219\u015F\u023F\uA7A9\uA785\u1E9B]/g
    }, {
      'base': 't',
      'letters': /[\u0074\u24E3\uFF54\u1E6B\u1E97\u0165\u1E6D\u021B\u0163\u1E71\u1E6F\u0167\u01AD\u0288\u2C66\uA787]/g
    }, {
      'base': 'tz',
      'letters': /[\uA729]/g
    }, {
      'base': 'u',
      'letters': /[\u0075\u24E4\uFF55\u00F9\u00FA\u00FB\u0169\u1E79\u016B\u1E7B\u016D\u00FC\u01DC\u01D8\u01D6\u01DA\u1EE7\u016F\u0171\u01D4\u0215\u0217\u01B0\u1EEB\u1EE9\u1EEF\u1EED\u1EF1\u1EE5\u1E73\u0173\u1E77\u1E75\u0289]/g
    }, {
      'base': 'v',
      'letters': /[\u0076\u24E5\uFF56\u1E7D\u1E7F\u028B\uA75F\u028C]/g
    }, {
      'base': 'vy',
      'letters': /[\uA761]/g
    }, {
      'base': 'w',
      'letters': /[\u0077\u24E6\uFF57\u1E81\u1E83\u0175\u1E87\u1E85\u1E98\u1E89\u2C73]/g
    }, {
      'base': 'x',
      'letters': /[\u0078\u24E7\uFF58\u1E8B\u1E8D]/g
    }, {
      'base': 'y',
      'letters': /[\u0079\u24E8\uFF59\u1EF3\u00FD\u0177\u1EF9\u0233\u1E8F\u00FF\u1EF7\u1E99\u1EF5\u01B4\u024F\u1EFF]/g
    }, {
      'base': 'z',
      'letters': /[\u007A\u24E9\uFF5A\u017A\u1E91\u017C\u017E\u1E93\u1E95\u01B6\u0225\u0240\u2C6C\uA763]/g
    }];

    for (i = 0; i < defaultDiacriticsRemovalMap.length; i++) {
      str = str.replace(defaultDiacriticsRemovalMap[i].letters, defaultDiacriticsRemovalMap[i].base);
    }
    return str;
  };

  /**
   * Returns a JSON object with the difference between the provided JSONs
   * Experimental. Do not be use for retail operations
   */
  OB.UTIL.diffJson = function (obj1, obj2) {
    // argument checks
    OB.UTIL.Debug.execute(function () {
      try {
        obj1 = JSON.parse(JSON.stringify(obj1));
        obj2 = JSON.parse(JSON.stringify(obj2));
      } catch (e) {
        OB.error("Illegal argument exception. Both arguments must be valid JSON objects (Error: " + e + ")");
      }
    });

    var key, key2, result = {};
    for (key in obj1) {
      if (obj1.hasOwnProperty(key)) {
        if (obj1[key] === undefined || obj1[key] === null || obj2[key] === undefined || obj2[key] === null) {
          if (obj1[key] !== obj2[key]) {
            result[key] = [obj2[key], obj1[key]];
          }
        } else if ($.isArray(obj1[key])) {
          if (!$.isArray(obj2[key]) || obj1[key].length !== obj2[key].length) {
            result[key] = [obj2[key], obj1[key]];
          } else {
            var preResult = OB.UTIL.diffJson(obj1[key], obj2[key]);
            if (JSON.stringify(preResult) !== '{}') {
              result[key] = preResult;
            }
          }
        } else if ($.isPlainObject(obj1[key])) {
          if (!$.isPlainObject(obj2[key])) {
            result[key] = [obj2[key], obj1[key]];
          } else {
            var preResult2 = OB.UTIL.diffJson(obj1[key], obj2[key]);
            if (JSON.stringify(preResult2) !== '{}') {
              result[key] = preResult2;
            }
          }
        } else if (obj2[key] !== obj1[key]) {
          result[key] = [obj2[key], obj1[key]];
        }
      }
    }

    // find any properties which are in obj2 but not in obj1
    for (key2 in obj2) {
      if (obj2.hasOwnProperty(key2)) {
        if (!obj1.hasOwnProperty(key2)) {
          result[key2] = [obj2[key2], obj1[key2]];
        }
      }
    }

    return result;
  };

  //Returns if the object obj is included in the array arr
  OB.UTIL.isObjectInArray = function (obj, arr) {
    var exists = false,
        i = 0;
    while (!exists && i < arr.length) {
      exists = _.isEqual(obj, arr[i]);
      i++;
    }
    return exists;
  };

  OB.UTIL.mergeArrays = function (arr1, arr2) {
    var i, res = [].concat(arr1);
    for (i = 0; i < arr2.length; i++) {
      if (!OB.UTIL.isObjectInArray(arr2[i], res)) {
        res.push(arr2[i]);
      }
    }
    return res;
  };

  OB.UTIL.isHTTPSAvailable = function () {
    return window.location.protocol === 'https:' || window.location.hostname.indexOf('localhost') !== -1 || window.location.host.indexOf('127.0.0.1') !== -1;
  };

  OB.UTIL.isInThePast = function (date) {
    return moment(date, OB.Format.date.toUpperCase()).format("YYYY-MM-DD") < moment().format("YYYY-MM-DD");
  };

}());