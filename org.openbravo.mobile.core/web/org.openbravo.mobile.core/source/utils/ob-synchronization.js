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

/*global OB, exports, console, Backbone, enyo, document, setTimeout, clearTimeout */

/*
  The SynchornizationHelper class is intended to watch asynchronous calls in the application

  This class does only 2 things:
    - fire a 'synchronizing' event when registered (see below) asynchronous calls in the application starts
    - fire a 'synchronized' event when all the registered asynchronous calls have finished

  These events can and should be received by:
    - ui elements that have to be disabled because the underlying asynchronous calls are blocking its correct behavior (e.g.: the login button or the total to pay button)
    - model elements that have to wait until underlying asynchronous calls have finished (e.g.: update the local database)

  Required:
    - The asynchronous calls to be watched have to be registered by one of the available OB.UTIL.SynchronizationHelper methods

  Howto implement the SynchronizationHelper:
    - find the asynchronous calls that are vital for the application
    - add a SynchronizationHelper.busyUntilFinishes and get the returning id
    - add a SynchronizationHelper.finished(<busyUntilFinishes_id>) when the asynchronous call have finished, success or fail
    - subscribe to the events 'synchronizing' and 'synchronized', the elements that you want to depend on the SynchronizationHelper status

  Notes:
    - to see the SynchronizationHelper log, set verbose = true
    - console.error is used because it attaches the stacktrace to the message

  Example of registering/unregistering asynchronous calls (ob-datasource.js):
    function serviceGET(source, dataparams, callback, callbackError, async) {
      var synchId = SynchronizationHelper.busyUntilFinishes('serviceGET ' + source);
      ...
      var ajaxRequest = new enyo.Ajax({
        ...
        success: function (inSender, inResponse) {
          SynchronizationHelper.finished(synchId, 'serviceGET');
          ...
        },
        fail: function (status, inResponse) {
          SynchronizationHelper.finished(synchId, 'serviceGET');
          ...
        }
      });
      ajaxRequest.go().response('success').error('fail');
    }

  Example of event subscriptions (ob-login.js):
    enyo.kind({
      name: 'OB.OBPOSLogin.UI.LoginButton',
      ...
      handlers: {
        synchronizing: 'disableButton',
        synchronized: 'enableButton'
      },
        disableButton: function () {
        this.setDisabled(true);
      },
      enableButton: function () {
        this.setDisabled(false);
      },
      initComponents: function () {
        this.inherited(arguments);
        ...
        this.setDisabled(true);
      }
    });
*/

OB.UTIL = OB.UTIL || {};

(function () {
  var root = this;
  if (typeof exports !== "undefined") {
    OB.UTIL.SynchronizationHelper = exports;
  } else {
    OB.UTIL.SynchronizationHelper = root.OB.UTIL.SynchronizationHelper = {};
  }
  OB.UTIL.SynchronizationHelper.VERSION = "0.8";

  var verbose = false;
  var delaySynchronizedEvent = 150;
  var timeoutThreshold = 60000;
  var isTimeoutThresholdActivated = false;
  OB.UTIL.Debug.execute(function () {
    // the timeout is forced active while in development to catch unbalanced calls and/or adjust the timeoutThreshold
    timeoutThreshold = 30000;
    isTimeoutThresholdActivated = true;
  });
  var eventId = null;
  var timeoutEventId = null;
  var clearCount = 0;
  var busyCount = -1;
  var watchedModelName = [];
  var isSynchronizingSent = false;
  var isTimeout = false;
  var busyProcessIdCounter = 0;
  var busyProcessArray = [];
  var STATUS = {
    SYNCHRONIZED: 0,
    SYNCHRONIZING: 1,
    NOTSYNCHRONIZED: 2,
    UNKNOWN: -1
  };
  var currentStatus = STATUS.UNKNOWN;
  var lastAccess = new Date().getTime();
  var modelsSynchronized = STATUS.UNKNOWN;

  if (verbose) {
    console.log("SynchronizationHelper %s is available and verbosing", OB.UTIL.SynchronizationHelper.VERSION);
  }

  OB.UTIL.SynchronizationHelper.getBusyQueueStringified = function () {
    var stringifiedBusyQueue = JSON.stringify(busyProcessArray);
    return stringifiedBusyQueue;
  };

  /**
   * Launch an event when an model has been synchronized
   * @return {void}
   */
  OB.UTIL.SynchronizationHelper.eachModelSynchronized = function (model) {
    OB.MobileApp.model.trigger('eachModelsSynchronized', model);
  };

  /**
   * Sets synchronization state to synchronized.
   * @return {void}
   */
  OB.UTIL.SynchronizationHelper.modelsSynchronized = function () {
    modelsSynchronized = STATUS.SYNCHRONIZED;
    OB.MobileApp.model.trigger('modelsSynchronized');
    OB.UTIL.showI18NSuccess('OBMOBC_SynchronizationWasSuccessfulMessage', 'OBMOBC_SynchronizingDataMessage');
  };
  /**
   * Sets synchronization state to synchronizing.
   * @return {void}
   */
  OB.UTIL.SynchronizationHelper.modelsSynchronizing = function () {
    modelsSynchronized = STATUS.SYNCHRONIZING;
    OB.MobileApp.model.trigger('modelsSynchronizing');
    OB.UTIL.showI18NWarning('OBMOBC_SynchronizingDataMessage', 'OBMOBC_SynchronizationWasSuccessfulMessage');
  };
  /**
   * Sets synchronization state to not synchronized.
   * @return {void}
   */
  OB.UTIL.SynchronizationHelper.modelsNotSynchronized = function () {
    modelsSynchronized = STATUS.NOTSYNCHRONIZED;
    OB.MobileApp.model.trigger('modelsNotSynchronized');
  };
  /**
   * Retrieves the current models synchronization state.
   * @return {Boolean}    true: models are synchronized; false: models are synchronizing
   */
  OB.UTIL.SynchronizationHelper.isModelsSynchronized = function () {
    return modelsSynchronized === STATUS.SYNCHRONIZED;
  };

  /**
   * Retrieves the current models synchronization state.
   * @return {Boolean}    false: models are synchronized or not synchronized; true: models are synchronizing
   */
  OB.UTIL.SynchronizationHelper.isModelsSynchronizing = function () {
    return modelsSynchronized === STATUS.SYNCHRONIZING;
  };

  /**
   * Retrieves the current models synchronization state.
   * @return {Boolean}    false: models are synchronized or synchronizing; true: models are not synchronized
   */
  OB.UTIL.SynchronizationHelper.isModelsNotSynchronized = function () {
    return modelsSynchronized === STATUS.NOTSYNCHRONIZED;
  };

  /**
   * Retrieves the current synchronization state. This is a vital method for the automation tests
   * @return {Boolean}    true: the app is synchronized; false: the app is synchronizing
   */
  OB.UTIL.SynchronizationHelper.isSynchronized = function () {
    return currentStatus === STATUS.SYNCHRONIZED;
  };

  var callbacksWaitingToBeExecuted = [];

  /**
   * Executes a callback after all busy processes have finished and before the currentStatus changes to synchronized
   * If the makeSyncrhonous argument is true, the callback is wrapped and the currentStatus will not change to syncrhonized until the callback has finished
   * If the makeSyncrhonous argument is true, the returning callback must be executed
   *
   * args:
   *   makeSynchronous  <Boolean>  undefined or false: execute asynchronously; true: the SynchronizationHelper wraps the call with the SyncrhonizationHelper.busyUntilFinishes logic
   *
   * returns:
   *   a callback that must be used to finalize the incomingCallback
   *
   */
  OB.UTIL.SynchronizationHelper.executeWhenSynchronized = function (args, callback) {
    //  argument checks
    OB.UTIL.Debug.execute(function () {
      if (!args || typeof (args) !== 'object') {
        throw "invalid argument 'args' (value = '" + args + "')";
      }
      if (!callback || typeof (callback) !== 'function') {
        throw "invalid argument 'callback' (typeof = '" + typeof (callback) + "')";
      }
    });
    var callbackToExecute = null;
    if (args.makeSynchronous) {
      callbackToExecute = function () {
        var synchIdOnReady = OB.UTIL.SynchronizationHelper.busyUntilFinishes('executeWhenSynchronized');
        callback({}, function () {
          OB.UTIL.SynchronizationHelper.finished(synchIdOnReady, 'executeWhenSynchronized');
        });
      };
    } else {
      callbackToExecute = callback;
    }
    if (OB.UTIL.SynchronizationHelper.isSynchronized()) {
      callbackToExecute();
      return;
    }
    callbacksWaitingToBeExecuted.push(callbackToExecute);
  };

  var elapsedTime = function () {
      return (new Date().getTime() - lastAccess);
      };

  /**
   * Sends the 'syncrhonizing' event
   */
  var sendSynchronizing;
  sendSynchronizing = function () {
    setTimeout(function () {
      // stop trying to synchronize if there has been a timeout
      if (isTimeout) {
        isSynchronizingSent = false;
        return;
      }
      // delay the event until the UI has loaded
      if (document.readyState === "complete") {
        busyCount += 1;
        if (OB.MobileApp.view) {
          OB.MobileApp.view.waterfall('synchronizing');
        }
        if (OB.MobileApp.model) {
          OB.MobileApp.model.trigger('synchronizing');
        }
        if (verbose) {
          console.error("[dev] SH: (" + busyCount + ") synchronizing sent");
        }
      } else {
        if (verbose) {
          console.error("[dev] SH: (" + (busyCount + 1) + ") synchronizing delayed");
        }
        sendSynchronizing();
      }
    }, 10);
  };

  /**
   * Sends the 'syncrhonized' event
   */
  var sendSynchronized = function () {
      isSynchronizingSent = false;
      clearCount = 0;
      currentStatus = STATUS.SYNCHRONIZED;
      // execute the callbacks that were waiting until the application was synchronized
      while (callbacksWaitingToBeExecuted.length > 0) {
        var waitingCallback = callbacksWaitingToBeExecuted[0];
        waitingCallback();
        callbacksWaitingToBeExecuted.shift();
        // the callback could contain actions that set the application in a non synchronized state
        if (OB.UTIL.SynchronizationHelper.isSynchronized() === false) {
          // return because this method will be executed again by the callback that just started
          return;
        }
      }
      // fire the event when the heap is empty
      setTimeout(function () {
        if (OB.UTIL.SynchronizationHelper.isSynchronized()) {
          if (OB.MobileApp.model) {
            OB.MobileApp.model.trigger('synchronized');
          }
          if (OB.MobileApp.view) {
            OB.MobileApp.view.waterfall('synchronized');
          }
          if (verbose) {
            console.error("[dev] SH: (" + busyCount + ") synchronized sent");
          }
        }
      }, 1);
      };

  /**
   * If busy is unable to return to a 'synchronized' state, it was because the 'busyUntilFinishes' and the 'finished' calls are
   * unpaired. We don't want to be staled in a 'synchronizing' state for ever. So, raise an exception and free the state
   */
  var timeout = function () {
      isTimeout = true;
      var s = "";
      busyProcessArray.forEach(function (element) {
        s += "(" + element.busyProcessId + ") " + element.msg + "\n";
      });

      // if the timeout is deactivated, do not perform actions and show a console message
      if (!isTimeoutThresholdActivated) {
        console.warn("SynchronizationHelper timeout reached but the watchers poll is not cleared because the timout actions are deactivated. This is happening because an asynchronous call is lasting more than " + timeoutThreshold + " milliseconds (set a longer timeout) or because there is an unbalanced process registered in the SynchronizationHelper watchers poll (fix the process). To activate the timeout actions (i.e. clear the poll and fire the 'synchronized' event), set 'isTimeoutThresholdActivated' to 'true'. This process(s) didn't finish: " + s);
        return;
      }

      // this error should never raise in production
      console.error("SynchronizationHelper timeout reached: This process(s) didn't finish: " + s);

      // clear the poll and fire the synchronized event
      busyProcessArray = [];
      sendSynchronized();
      };

  /**
   * Main algorithm to watch and update the synchronization state
   */
  var busy = function () {
      if (OB.UTIL.SynchronizationHelper.isSynchronized()) {
        // set to log level error, so it shows in the automated tests browser.log. Have to be updated to debug anytime soon (tm)
        OB.UTIL.Debug.execute(function () {
          console.debug("SynchronizationHelper: Elapsed time since the last synchronized state (" + elapsedTime() + ")");
        });
      }
      lastAccess = new Date().getTime();

      currentStatus = STATUS.SYNCHRONIZING;

      if (isSynchronizingSent === false) {
        isTimeout = false;
        isSynchronizingSent = true;
        sendSynchronizing();
      }

      if (eventId !== null) {
        clearTimeout(eventId);
        eventId = null;
        clearCount += 1;
      }

      if (timeoutEventId !== null) {
        clearTimeout(timeoutEventId);
        timeoutEventId = null;
      }
      // be sure that the synchronizing state is released
      timeoutEventId = setTimeout(function () {
        timeout();
      }, timeoutThreshold);

      if (busyProcessArray.length > 0) {
        return;
      }

      clearTimeout(timeoutEventId);
      timeoutEventId = null;
      isTimeout = false;
      eventId = setTimeout(function () {
        if (isSynchronizingSent === false) {
          console.error("SynchronizationHelper: A 'synchronized' event should always be preceeded by a 'synchronizing' event");
        }
        sendSynchronized();
      }, delaySynchronizedEvent);
      };

  /**
   * Call this method when want to watch a backbone model. Be careful because it adds a lot of overhead depending on the number of events fired in the model
   */
  OB.UTIL.SynchronizationHelper.watchModel = function (model, msg, forceWatch) {
    var modelName = model.modelName || "NA";
    // we don't want the LogClient to modify the synchronization state
    if (modelName === "LogClient") {
      return;
    }
    if (!forceWatch) {
      busy(modelName + ", " + msg);
      return;
    }
    if (watchedModelName.indexOf(modelName) < 0) {
      watchedModelName.push(modelName);
      model.on('all', function (eventName) {
        // if (eventName.indexOf('change') >= 0) {
        //   return;
        // }
        busy(modelName + ", fired: " + eventName + ", " + msg);
      });
    }
  };

  /**
   * Call this method when the process you want to add to the SynchronizationHelper watch is not an asynchronous call but, still, vital
   */
  OB.UTIL.SynchronizationHelper.busy = function (msg) {
    if (verbose) {
      console.error("[dev] SH.busy (+%sms): '%s'", new Date().getTime() - lastAccess, msg);
    }
    busy(msg);
  };

  /**
   * Call this method before an asynchronous is made and keep the returned id
   * @return  {int}   assigned id
   */
  OB.UTIL.SynchronizationHelper.busyUntilFinishes = function (msg) {
    OB.UTIL.Debug.execute(function () {
      // argument check
      if (!msg) {
        console.error("A message must be specified. The message should be meaningful and unique so it would be easier to find the cause of an unbalanced call; preferably, the same passed to the paired finished method.");
      }
    });
    var busyProcessId = busyProcessIdCounter;
    if (verbose) {
      console.error("[dev] SH.busyUntilFinishes (" + busyProcessId + ") " + msg);
    }
    busyProcessIdCounter += 1;
    busyProcessArray.push({
      busyProcessId: busyProcessId,
      msg: msg
    });
    busy(msg);
    return busyProcessId;
  };

  /**
   * Call this method when the asynchronous call have finished, providing the id retrieved in the 'busyUntilFinishes' method
   * @param  {int}   busyProcessId    the id that was assigned by the 'busyUntilFinishes' method
   */
  OB.UTIL.SynchronizationHelper.finished = function (busyProcessId, msg) {
    OB.UTIL.Debug.execute(function () {
      // argument check
      if (!msg) {
        console.error("A message must be specified. The message should be meaningful and unique so it would be easier to find the cause of an unbalanced call; preferably, the same passed to the busyUntilFinishes method.");
      }
    });
    if (verbose) {
      console.error("[dev] SH.finished (" + busyProcessId + ") " + msg);
    }
    var index = -1;
    var i;
    for (i = 0; i < busyProcessArray.length; i++) {
      var element = busyProcessArray[i];
      if (element.busyProcessId === busyProcessId) {
        index = i;
        break;
      }
    }
    if (index < 0) {
      console.error("OB.UTIL.SynchronizationHelper: Unbalanced finished. The process '(" + busyProcessId + ") " + msg + "' cannot be found");
      return;
    }
    busyProcessArray.splice(index, 1);
    busy(msg);
  };

  /**
   * Just for logging. With timestamp. Useful to check if a call 'busyUntilFinishes' or 'busy' is needed
   * The timestamp value should always be less that the 'delaySynchronizedEvent' value is no 'synchronized' event should be fired inbetween
   */
  OB.UTIL.SynchronizationHelper.log = function (msg) {
    OB.UTIL.Debug.execute(function () {
      console.error("[dev] SH.log (+%sms): '%s'", elapsedTime(), msg);
    });
  };

  /**
   * Show when enyo and backbone are available
   */
  if (verbose) {
    var dummyModel = new(Backbone.Model.extend({
      initialize: function () {
        console.log('SH: backbone loaded');
      }
    }))();
    enyo.load('', function () {
      console.log('SH: enyo loaded'); // this gets called when enyo has loaded the library
    });
  }

}());