/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, _, enyo, Backbone, CryptoJS, console, alert, Promise, localStorage */

OB.Model = window.OB.Model || {};
OB.MobileApp = OB.MobileApp || {};
OB.Data = OB.Data || {};

// used for test purposes to keep track that sync dialog was shown
OB.Data.showSynchronizedDialogChangeTime = 0;

OB.MobileApp.windowRegistry = new(Backbone.Model.extend({
  registeredWindows: [],

  registerWindow: function (window) {
    if (OB && OB.MobileApp && OB.MobileApp.model && OB.UTIL.isNullOrUndefined(OB.MobileApp.model.get('defaultWindow')) && window.defaultWindow) {
      OB.MobileApp.model.set('defaultWindow', window.route);
    } else if (window.defaultWindow) {
      if (OB && OB.error) {
        OB.warn('Default window ignored. ' + window.windowClass);
      } else {
        console.warn('Default window ignored. ' + window.windowClass);
      }
    }
    this.registeredWindows.push(window);
  }
}))();

OB.Model.Collection = Backbone.Collection.extend({
  constructor: function (data) {
    this.ds = data.ds;
    Backbone.Collection.prototype.constructor.call(this);
  },
  inithandler: function (init) {
    if (init) {
      init.call(this);
    }
  },
  exec: function (filter) {
    var me = this;
    if (this.ds) {
      this.ds.exec(filter, function (data, info) {
        var i;
        me.reset();
        me.trigger('info', info);
        if (data.exception) {
          OB.UTIL.showError(data.exception.message);
        } else {
          for (i in data) {
            if (data.hasOwnProperty(i)) {
              me.add(data[i]);
            }
          }
        }
      });
    }
  }
});

// Terminal model.
OB.Model.Terminal = Backbone.Model.extend({

  defaults: {
    terminal: null,
    context: null,
    permissions: null,
    businesspartner: null,
    location: null,
    pricelist: null,
    pricelistversion: null,
    currency: null,
    connectedToERP: null,
    loginUtilsUrl: '../../org.openbravo.mobile.core.loginutils',
    loginUtilsParams: {},
    supportsOffline: false,
    loginHandlerUrl: '../../org.openbravo.mobile.core/LoginHandler',
    applicationFormatUrl: '../../org.openbravo.mobile.core/OBCLKER_Kernel/Application',
    logConfiguration: {
      deviceIdentifier: '',
      logPropertiesExtension: []
    },
    windows: null,
    appName: 'OBMOBC',
    appDisplayName: 'Openbravo Mobile',
    useBarcode: false,
    useEmbededBarcode: true,
    profileOptions: {
      showOrganization: true,
      showWarehouse: true,
      defaultProperties: {
        role: null,
        organization: null,
        warehouse: null,
        languagage: null
      }
    },
    localDB: {
      size: OB.UTIL.VersionManagement.current.mobileCore.WebSQLDatabase.size,
      name: OB.UTIL.VersionManagement.current.mobileCore.WebSQLDatabase.name,
      displayName: OB.UTIL.VersionManagement.current.mobileCore.WebSQLDatabase.displayName
    },
    shouldExecuteBenchmark: false,
    propertiesLoaders: [],
    dataSyncModels: []
  },

  initialize: function () {
    var me = this;
    // expose terminal globally
    window.OB.MobileApp = OB.MobileApp || {};
    window.OB.MobileApp.model = this;

    OB.Dal.openWebSQL();


    OB.UTIL.HookManager.executeHooks('OBMOBC_InitActions', {}, function (args) {
      if (args && args.cancelOperation && args.cancelOperation === true) {
        return;
      } else {
        me.initActions(function () {});
      }
    });

    // attach objects to model
    this.router = new OB.Model.RouterHelper(this);
    this.windowRegistry = OB.MobileApp.windowRegistry;
    OB.UTIL.VersionManagement.deprecated(27349, function () {
      this.hookManager = OB.UTIL.HookManager;
    }, function (deprecationMessage) {
      this.hookManager = {
        registerHook: function (qualifier, func) {
          OB.warn(deprecationMessage + ". Hook: '" + qualifier + "'");
          OB.UTIL.HookManager.registerHook(qualifier, func);
        },
        executeHooks: function (qualifier, args, callback) {
          OB.warn(deprecationMessage + ". Hook: '" + qualifier + "'");
          OB.UTIL.HookManager.executeHooks(qualifier, args, callback);
        },
        callbackExecutor: function (args, callback) {
          OB.warn(deprecationMessage);
          OB.UTIL.HookManager.callbackExecutor(args, callback);
        }
      };
    }, this);

    // DEVELOPER: activate this to see all the events that are being fired by the backbone components of the terminal
    // this.on('all', function(eventName, object, third) {
    //   if (eventName.indexOf('change') >= 0) {
    //     return;
    //   }
    //   var enyoName = object ? object.name : '';
    //   OB.info('event fired: ' + eventName + ', ' + enyoName + ', ' + third);
    // });
    // model events
    this.on('window:ready', this.renderContainerWindow); // When the active window is loaded, the window is rendered with the renderContainerWindow function
    this.on('terminalInfoLoaded', function () {
      OB.debug("next process: processPropertyLoaders");
      OB.UTIL.localStorage.setItem('cacheAvailableForUser:' + OB.MobileApp.model.get('orgUserId'), true);
      this.processPropertyLoaders();
    });
    this.on('propertiesLoadersReady', function () {
      OB.debug("next process: loadRegisteredWindows, renderMain, postLoginActions");
      OB.UTIL.completeLoadingStep();
      me.saveBrowserTabId();
      this.loadRegisteredWindows();
      this.renderMain();
      this.postLoginActions();
    }, this);

    // when the page has finished loading the components, load the root webpage
    var synchId = OB.UTIL.SynchronizationHelper.busyUntilFinishes('rootWindow');
    window.addEventListener('load', function (e) {
      // verifying that all dataSyncModels have modelFunc defined
      _.each(OB.MobileApp.model.get('dataSyncModels'), function (syncModel) {
        if (syncModel && OB.UTIL.isNullOrUndefined(syncModel.modelFunc)) {
          if (syncModel.model && syncModel.model.prototype && syncModel.model.prototype.modelName) {
            syncModel.modelFunc = 'OB.Model.' + syncModel.model.prototype.modelName;
          }
        }
      }, this);
      // explicitly setting pushState to false tries to ensure...
      // that future default modes will not activate the pushState...
      // breaking the POS navigation
      // setTimeout to move the execution until all 'load' listeners have finished
      setTimeout(function () {
        Backbone.history.start({
          root: '',
          pushState: false
        });
        OB.UTIL.SynchronizationHelper.finished(synchId, 'rootWindow');
      }, 0);
    }, false);
  },

  /**
   * Method to be called when there is an error while loading the application
   * @param  {String}  caller   a string that identifies the process that failed
   */
  loadingErrorsActions: function (caller) {
    // arguments check
    OB.UTIL.Debug.execute(function () {
      if (!caller) {
        throw "missing parameter: caller";
      }
    });

    // remember that the loading has failed. used by the LogClient
    OB.MobileApp.model.loadingErrorsActions.executed = true;

    var link = 'NA';
    try {
      var errorobj = new Error();
      link = OB.UTIL.getStackLink(3);
    } catch (ex) {
      console.error("executeSqlErrorHandler.getStackLink: " + ex);
    }
    console.error("loadingErrorsActions: '" + caller + "' failed while loading the application. line: " + link);
  },

  cleanTerminalData: function () {
    _.each(this.get('propertiesLoaders'), function (curPropertiesToLoadProcess) {
      _.each(curPropertiesToLoadProcess.properties, function (curProperty) {
        this.set(curProperty, null);
      }, this);
    }, this);
  },

  returnToOnline: function () {
    //DEVELOPERS: overwrite this function to perform actions when it happens
    OB.info('The system has come back to online');
  },

  //DEVELOPER: This function allow us to execute some code to add properties to to terminal model. Each position of the array
  // must identify the properties which are set and needs to define a loadFunction which will be executed to get the data.
  // when the data is ready terminalModel.propertiesReady(properties) should be executed.
  addPropertiesLoader: function (obj) {
    this.get('propertiesLoaders').push(obj);
    this.syncAllPropertiesLoaded = _.after(this.get('propertiesLoaders').length, this.allPropertiesLoaded);
  },

  handlePropertiesLoader: function (properties, source, params, successCallback) {

    function handleError() {
      var msg = OB.I18N.getLabel('OBMOBC_TerminalPropertiesErrorBody', [properties.join(', ')]) + OB.I18N.getLabel('OBMOBC_LoadingErrorBody'),
          isErrorPopupShown = OB.MobileApp.view.$.confirmationContainer.getCurrentPopup() && OB.MobileApp.view.$.confirmationContainer.getCurrentPopup().getShowing();
      if (OB.MobileApp.model.get('isLoggingIn') === true && !isErrorPopupShown) {
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_TerminalPropertiesErrorHeader'), msg, [{
          label: OB.I18N.getLabel('OBMOBC_Reload'),
          action: function () {
            window.location.reload();
          }
        }], {
          onShowFunction: function (popup) {
            OB.UTIL.localStorage.removeItem('cacheAvailableForUser:' + OB.MobileApp.model.get('orgUserId'));
            popup.$.headerCloseButton.hide();
            OB.MobileApp.view.$.containerWindow.destroyComponents();
          },
          autoDismiss: false
        });
      }
    }

    new OB.DS.Request(source).exec(params, function (data) {
      if (data.exception) {
        handleError();
      } else {
        if (typeof successCallback === 'function') {
          successCallback(data);
        }
      }
    }, function (data) {
      handleError();
    });
  },

  propertiesReady: function (properties) {
    OB.info("[sdrefresh] '" + properties + "' property loaded");
    this.syncAllPropertiesLoaded();
  },

  processPropertyLoaders: function () {
    OB.debug("next process: allPropertiesLoaded");
    var termInfo, msg, key, keysToSkip = ['dataSyncModels'];
    if (this.get('loggedOffline')) {
      //Load from termInfo
      if (this.usermodel.get('terminalinfo')) {
        termInfo = JSON.parse(this.usermodel.get('terminalinfo'));
        for (key in termInfo) {
          if (termInfo.hasOwnProperty(key) && keysToSkip.indexOf(key) === -1) { //If it is not in the keys to skip
            this.set(key, termInfo[key]);
          }
        }
        //overwrite loaded values which was stored into DB when system was online
        this.set('loggedOffline', true);
        this.set('connectedToERP', false);
        // Force the initialization of the document sequence info
        OB.MobileApp.model.saveDocumentSequence();
        //Funtions are not recoverd when obj is loaded from DB. Recover Them
        this.set('logConfiguration', {
          deviceIdentifier: OB.UTIL.localStorage.getItem('terminalAuthentication') === 'Y' ? OB.UTIL.localStorage.getItem('terminalName') : OB.UTIL.getParameterByName("terminal"),
          logPropertiesExtension: [

          function () {
            return {
              isOnline: OB.MobileApp.model.get('connectedToERP')
            };
          }]
        });

        _.each(this.get('dataSyncModels'), function (item) {
          item.model = eval(item.modelFunc);
        });
        //Overwrite offline values
        this.set('connectedToERP', false);
        this.set('loggedOffline', true);

        this.allPropertiesLoaded();
      } else {
        // not enough data in cache
        OB.MobileApp.model.loadingErrorsActions('processPropertyLoaders: not enough data in cache');

        msg = OB.I18N.getLabel('OBMOBC_NotEnoughDataInCache') + OB.I18N.getLabel('OBMOBC_LoadingErrorBody');
        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_TerminalPropertiesErrorHeader'), msg, [{
          label: OB.I18N.getLabel('OBMOBC_Reload'),
          action: function () {
            window.location.reload();
          }
        }], {
          onShowFunction: function (popup) {
            OB.UTIL.localStorage.removeItem('cacheAvailableForUser:' + OB.MobileApp.model.get('orgUserId'));
            popup.$.headerCloseButton.hide();
          },
          autoDismiss: false
        });
      }
      return;
    }
    this.set('loggedOffline', false);
    //Loading the properties of the array
    OB.info("[terminal] Starting to load properties based on properties loaders, count: " + this.get('propertiesLoaders').length);
    OB.UTIL.completeLoadingStep();
    OB.UTIL.showLoadingMessage(OB.I18N.getLabel('OBMOBC_LoadingTerminalProps'));

    if (this.get('propertiesLoaders') && this.get('propertiesLoaders').length > 0) {
      _.each(this.get('propertiesLoaders'), function (curProperty) {
        //each loadFunction will call to propertiesReady function. This function will trigger
        //allPropertiesLoaded when all of the loadFunctions are done.
        curProperty.loadFunction(this);
      }, this);
    } else {
      this.allPropertiesLoaded();
    }
  },

  //DEVELOPER: this function will be automatically called when all the properties defined in
  //this.get('propertiesLoaders') are loaded. To indicate that a property is loaded
  //me.propertiesReady(properties) should be executed by loadFunction of each property
  allPropertiesLoaded: function () {
    var me = this;
    OB.debug("next process: propertiesLoadersReady");
    if (!OB.MobileApp.model.get('datasourceLoadFailed')) {
      OB.UTIL.HookManager.executeHooks('OBMOBC_TerminalLoaded', {
        terminalInfo: me
      }, function (args) {
        OB.info('[terminal] Properties have been successfully loaded');
        // activate this line to see all the attributes (all the key data from the server)
        // OB.debug('[terminal] Properties have been successfully loaded', this.attributes);
        if (!me.get('loggedOffline')) {
          //In online mode, we save the terminal information in the local db
          me.usermodel.set('terminalinfo', JSON.stringify(me));
          OB.Dal.save(me.usermodel, function () {}, function () {
            OB.error("allPropertiesLoaded", arguments);
          });
        }
        //renderMain
        me.trigger('propertiesLoadersReady');
      });

    }
  },

  /**
   * This function calls to route function of backbone.
   * Replace parameter by default is true (if not passed), it means that the current history point will be replaced with the new one without create a new point.
   * If you want to create a new point in the history then pReplace should be passed as false
   * http://backbonejs.org/#Router-navigate
   */
  navigate: function (route, pReplace) {
    if (pReplace !== false) {
      pReplace = true;
    }
    OB.debug("Navigating to '" + route + "' Replace: " + pReplace);
    OB.MobileApp.view.keyReceivers.splice(0, OB.MobileApp.view.keyReceivers.length);
    this.router.navigate(route, {
      trigger: true,
      replace: pReplace
    });
  },

  /**
   * Loads registered windows and calls renderMain method implemented by each app
   */
  renderTerminalMain: function () {
    OB.debug("next process: databaseInitialization");
    OB.UTIL.Debug.execute(function () {
      if (!this.renderMain) {
        throw "There is no renderMain method in Terminal Model";
      }
    }, this);
    this.databaseInitialization();
  },

  checkAllPermissions: function (permissions) {
    // Verify all permissions
    var i;
    for (i in permissions) {
      if (permissions.hasOwnProperty(i)) {
        if (this.get('permissions')) {
          if (permissions[i] !== this.get('permissions')[i]) {
            return true;
          }
        }
      }
    }

    return false;
  },

  /**
   * Database initialization
   */
  databaseInitialization: function () {
    OB.debug("next process: loadTerminalInfo");
    if (window.openDatabase) {
      this.initLocalDB();
    } else {
      this.loadTerminalInfo();
    }
  },

  /**
   * Loads all needed stuff for the terminal such as permissions, Application...
   */
  loadTerminalInfo: function () {
    OB.debug("next process: terminalInfoLoaded");
    OB.UTIL.completeLoadingStep();
    OB.UTIL.showLoadingMessage(OB.I18N.getLabel('OBMOBC_LoadingTerminalInfo'));
    var me = this;

    if (OB.MobileApp.model.get('loggedOffline')) {
      OB.Format = JSON.parse(OB.UTIL.localStorage.getItem('AppFormat'));
      OB.I18N.labels = JSON.parse(OB.UTIL.localStorage.getItem('I18NLabels_' + OB.UTIL.localStorage.getItem('languageForUser_' + me.usermodel.get('id'))));
    }
    this.setUserModelOnline(function () {
      me.trigger('terminalInfoLoaded');
    });

  },

  isSafeToResetDatabase: function (callbackIsSafe, callbackIsNotSafe) {
    callbackIsSafe();
  },

  databaseCannotBeResetAction: function () {
    //Will never happen in standard mobile core. Should be overwritten in applications if isSafeToResetDatabase is implemented
  },

  initLocalDB: function () {

    OB.Dal.openWebSQL();

    function dropTable(modelObj) {
      return new Promise(function (resolve, reject) {
        function dropTableFunction(modelObj) {
          OB.Dal.dropTable(modelObj, function () {
            var modelName = modelObj.prototype.modelName;
            var modelChecksum = OB.Data.getModelStructureChecksum(modelObj);
            //If this table belongs to a masterdata model, the masterdata needs to be fully loaded
            if (OB.UTIL.localStorage.getItem('lastUpdatedTimestamp' + modelName)) {
              OB.UTIL.localStorage.removeItem('lastUpdatedTimestamp' + modelName);
              OB.UTIL.localStorage.removeItem('POSLastTotalRefresh');
              OB.UTIL.localStorage.removeItem('POSLastIncRefresh');
            }
            OB.Dal.initCache(modelObj, [], function () {
              //Set checksum after the model has been created to prevent inconsistent states (e.g. checksum set but missing model)
              OB.debug('Set model ' + modelName + " checksum to " + modelChecksum);
              OB.UTIL.localStorage.setItem('structureChecksum-' + modelName, modelChecksum);
              resolve();
            }, function () {
              OB.error("Couldn't initialize table for model: " + modelName);
              resolve();
            });
          }, function () {
            OB.error(OB.UTIL.argumentsToStringifyed("dropTable", arguments));
            reject();
          });
        }

        var syncModel = OB.MobileApp.model.getSyncModel(modelObj);
        if (syncModel) {
          OB.MobileApp.model.syncModelHasData(syncModel, function () {
            //Sync model has data. We will try to synchronize, and if we fail then the databaseCannotBeReset action will be executed
            OB.MobileApp.model.syncAllModels(function () {
              dropTableFunction(modelObj);
            }, OB.MobileApp.model.databaseCannotBeResetAction);
          }, function () {
            dropTableFunction(modelObj);
          });
        } else {
          dropTableFunction(modelObj);
        }
      });
    }

    // check if terminal id has changed
    if (OB.UTIL.localStorage.getItem('loggedTerminalName') && (OB.UTIL.localStorage.getItem('loggedTerminalName') !== OB.MobileApp.model.get('terminalName'))) {
      console.warn(OB.UTIL.argumentsToStringifyed("terminal changed (" + OB.UTIL.localStorage.getItem('loggedTerminalName') + " -> " + OB.MobileApp.model.get('terminalName') + ")"));
      OB.info('Terminal has been changed. Resetting database and local storage information.');
      OB.UTIL.localStorage.clear();
      OB.UTIL.localStorage.setItem('terminalName', OB.MobileApp.model.get('terminalName'));
      OB.MobileApp.model.logout();
      return;
    }

    var tablesToDrop = [];
    // Check models individually
    OB.info('Checking database models...');
    _.each(OB.Model, function (model) {
      if (!model.prototype || !model.prototype.modelName) {
        return;
      }
      var modelName = model.prototype.modelName;
      var modelChecksum = OB.Data.getModelStructureChecksum(model);
      if (modelChecksum === '') {
        return;
      }
      if (OB.UTIL.localStorage.getItem('structureChecksum-' + modelName) !== modelChecksum) {
        if (!OB.UTIL.localStorage.getItem('structureChecksum-' + modelName)) {
          OB.debug("Model '" + modelName + "' should not exists because there isn't a paired entry in the localStorage. Performning preemptive table dropping...");
        } else {
          OB.info("Model '" + modelName + "' changed. Rebuilding...");
        }
        tablesToDrop.push(dropTable(model));
      }
    });
    if (tablesToDrop.length === 0) {
      OB.info('No models changed.');
      OB.MobileApp.model.loadTerminalInfo();
      return;
    }

    var synchId = OB.UTIL.SynchronizationHelper.busyUntilFinishes('dropTables');
    Promise.all(tablesToDrop).then(function () {
      OB.UTIL.SynchronizationHelper.finished(synchId, 'dropTables');
      OB.MobileApp.model.loadTerminalInfo();
    }, function () {
      OB.UTIL.SynchronizationHelper.finished(synchId, 'dropTables');
      OB.MobileApp.model.loadingErrorsActions('initLocalDB.dropTalbles');
    });

  },

  /**
   * initActions actions is executed before initializeCommonComponents.
   *
   * Override this in case you app needs to do something special
   * Do not forget to execute callback when overriding the method
   */
  initActions: function (callback) {

    var params = {},
        me = this,
        loginRequest, rr;
    var cacheSessionId = null;
    if (OB.UTIL.localStorage.getItem('cacheSessionId') && OB.UTIL.localStorage.getItem('cacheSessionId').length === 32) {
      cacheSessionId = OB.UTIL.localStorage.getItem('cacheSessionId');
    }
    params.cacheSessionId = cacheSessionId;
    params.command = 'initActions';
    loginRequest = new OB.OBPOSLogin.UI.LoginRequest({
      url: (this.get('loginUtilsUrl') ? this.get('loginUtilsUrl') : '../../org.openbravo.mobile.core.loginutils'),
      data: params
    }).response(this, function (inSender, inResponse) {
      if (!(OB.UTIL.localStorage.getItem('cacheSessionId') && OB.UTIL.localStorage.getItem('cacheSessionId').length === 32)) {
        OB.UTIL.localStorage.setItem('cacheSessionId', inResponse.cacheSessionId);
      }
      //      Save available servers and services and initialize Request Router layer
      if (inResponse.servers) {
        OB.UTIL.localStorage.setItem('servers', JSON.stringify(inResponse.servers));
      }
      if (inResponse.services) {
        OB.UTIL.localStorage.setItem('services', JSON.stringify(inResponse.services));
      }
      if (inResponse.executePerformanceTest) {
        OB.UTIL.localStorage.setItem('executePerformanceTest', inResponse.executePerformanceTest);
      }
      OB.RR.RequestRouter.initialize();

      if (callback) {
        callback();
      }
    }).error(function () {
      if (callback) {
        callback();
      }
    }).go(params);
  },

  /**
   * PreLoadContext actions is executed before loading the context.
   *
   * Override this in case you app needs to do something special
   * Do not forget to execute callback when overriding the method
   */
  preLoadContext: function (callback) {
    callback();
  },

  /**
   * Returns the corresponding sync model for a data model. Returns null if it's not a sync model
   */
  getSyncModel: function (dataModel) {
    var theSyncModel = null;
    _.each(this.get('dataSyncModels'), function (syncModel) {
      if (syncModel.model === dataModel) {
        theSyncModel = syncModel;
      }
    });
    return theSyncModel;
  },

  /**
   * Checks if the corresponding synchronization model has data pending to be synchronized.
   * - If data is still present, hasDataCallback will be executed, and the data will be sent
   *   as a parameter in the callback
   * - If there is no data, doesntHaveDataCallback will be executed
   */
  syncModelHasData: function (modelObj, hasDataCallback, doesntHaveDataCallback) {
    var criteria = modelObj.changesPendingCriteria ? modelObj.changesPendingCriteria : (modelObj.getCriteria ? modelObj.getCriteria() : (modelObj.criteria ? modelObj.criteria : null));
    var modelObject = modelObj;
    if (modelObj.model) {
      modelObject = modelObj.model;
    }
    if (criteria) {
      criteria._limit = -1;
    }

    OB.Dal.find(modelObject, criteria, function (dataToSync) {
      if (dataToSync.length === 0) {
        if (doesntHaveDataCallback) {
          doesntHaveDataCallback(modelObject);
        }
      } else {
        if (hasDataCallback) {
          hasDataCallback(modelObject, dataToSync);
        }
      }
    }, function () {
      if (doesntHaveDataCallback) {
        doesntHaveDataCallback(modelObject);
      }
    }, {
      doNotShowErrors: true
    });
  },

  /**
   * @return {Array}  The list of the Sync Models (models which data is sent to the server) with a flag indicating if they have data pending to be sent to the server
   *
   * Howto use it:
   *
   *    OB.MobileApp.model.getSyncModelsWithHasData().then(function(syncModelsWithHasData) {
   *      console.log(syncModelsWithHasData);
   *    })
   *
   * Please note that this function is experimental and that can be changed or removed at any time without notice
   */
  getSyncModelsWithHasData: function () {
    return new Promise(function (resolve, reject) {
      var syncModelsWithHasData = []; // list of models that requires synchronization and their data status
      var promisesOfModelsToVerify = [];
      _.each(OB.Model, function (model) {
        if (!model.prototype || !model.prototype.modelName) {
          return;
        }
        var modelName = model.prototype.modelName;
        var modelChecksum = OB.Data.getModelStructureChecksum(model);
        if (modelChecksum === '') {
          return;
        }
        var promiseOfModelToVerify = new Promise(function (resolve, reject) {
          var syncModel = OB.MobileApp.model.getSyncModel(model);
          if (syncModel) {
            // add the Sync Model to the array with a hasData flag
            var callbackHasData = function () {
                syncModelsWithHasData.push({
                  model: model,
                  hasData: true
                });
                resolve();
                };
            var callbackNoData = function () {
                syncModelsWithHasData.push({
                  model: model,
                  hasData: false
                });
                resolve();
                };
            OB.MobileApp.model.syncModelHasData(syncModel, callbackHasData, callbackNoData);
          } else {
            resolve();
          }
        });
        promisesOfModelsToVerify.push(promiseOfModelToVerify);
      });
      Promise.all(promisesOfModelsToVerify).then(function () {
        resolve(syncModelsWithHasData);
      }, function () {
        OB.error("Could not gather information about the models");
        reject();
      });
    });
  },


  /**
   * Is used by the synchronized mode logic to reset tables to their previous state
   * before making the synchronized call to the server. So if the user then
   * refreshes the browser during the call at least the tables are in their previous
   * versions. This is mainly relevant for cashup information which gets updated
   * along the way and needs to be reversed if needed.
   *
   * If the synchronized call fails then the old cashup information still applies.
   *
   * The flow is as follows:
   * # application code calls saveBackupBeforeChange function, it stores all the relevant tables
   * for that action
   * # application then changes what it wants
   * # then calls runsync process
   * # runsyncprocess will just before calling the server backup the new version of the
   *  tables (saveBackupBeforeSyncRestorePreChangeBackup function) and restore the
   *  before change version
   * # when the server encounters an error or something else then nothing is changed as
   * all the data is already in the previous version
   * # when the server returns correctly the backup tables are restored to their state just
   * before the send to the server
   *
   */
  _synchronizedCheckpointModels: [],
  addSyncCheckpointModel: function (model) {
    this._synchronizedCheckpointModels.push(model);
  },
  _prechangeCheckpoint: null,
  _presyncCheckpoint: null,
  synchProcessingTransaction: null,
  setSynchronizedCheckpoint: function (callback) {
    var me = OB.MobileApp.model;

    if (!OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true)) {
      return;
    }

    // already show the dialog now
    if (!me.showSynchronizedDialog) {
      me.showSynchronizingDialog();
    }
    OB.Dal.createDataDump(me._synchronizedCheckpointModels, function (checkPoint) {
      me._prechangeCheckpoint = checkPoint;
      if (callback) {
        callback();
      }
    });
  },
  synchronizingDialogConditions: new Backbone.Collection(),
  showSynchronizingDialog: function (condition) {
    var me = OB.MobileApp.model;
    if (condition) {
      this.synchronizingDialogConditions.push(condition);
    }
    me.synchProcessingTransaction = OB.UTIL.SynchronizationHelper.busyUntilFinishes('ProcessingTransaction');
    me.showSynchronizedDialog = OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_ProcessingTransactionTitle'), OB.I18N.getLabel('OBMOBC_ProcessingTransaction'), [], {
      hideCloseButton: true,
      autoDismiss: false,
      closeOnEscKey: false
    });
    OB.Data.showSynchronizedDialogChangeTime = new Date().getTime();
    OB.UTIL.preventRefreshByUser();
  },

  hideSynchronizingDialog: function (condition) {
    var me = OB.MobileApp.model;
    if (condition) {
      this.synchronizingDialogConditions.pop(condition);
    }
    if (me.showSynchronizedDialog && this.synchronizingDialogConditions.length === 0) {
      me.showSynchronizedDialog.hide();
      me.showSynchronizedDialog = null;
      OB.UTIL.resetPreventRefreshByUser();
    }
    OB.UTIL.SynchronizationHelper.finished(me.synchProcessingTransaction, 'ProcessingTransaction');
  },

  createPresyncCheckpoint: function (callback) {
    var me = OB.MobileApp.model;

    OB.Dal.createDataDump(me._synchronizedCheckpointModels, function (checkPoint) {
      me._presyncCheckpoint = checkPoint;
      if (callback) {
        callback();
      }
    });
  },
  moveToPrechangeCheckpoint: function (callback) {
    var me = OB.MobileApp.model,
        dataDump = me._prechangeCheckpoint;
    if (!dataDump) {
      if (callback) {
        callback();
      }
      return;
    }
    // be on the save side, set it to null if it fails we won't get it 
    // back anyway
    me._prechangeCheckpoint = null;
    OB.Dal.restoreDataDump(dataDump, function () {
      me._prechangeCheckpoint = null;
      if (callback) {
        callback();
      }
    });
  },
  resetCheckpointData: function () {
    var me = OB.MobileApp.model;

    me._presyncCheckpoint = null;
    me._prechangeCheckpoint = null;
  },
  moveToPresyncCheckpoint: function (callback) {
    var me = OB.MobileApp.model;
    // reset this one also
    me._prechangeCheckpoint = null;

    if (!me._presyncCheckpoint) {
      if (callback) {
        callback();
      }
      return;
    }
    OB.Dal.restoreDataDump(me._presyncCheckpoint, function () {
      me._presyncCheckpoint = null;
      if (callback) {
        callback();
      }
    });
  },

  preSyncPromises: [],
  doPreSynchronizedCallActions: function (callback, syncData) {
    var me = OB.MobileApp.model;
    OB.UTIL.localStorage.setItem('synchronizedMessageId', syncData.messageId);
    new Promise(function (resolve, reject) {
      me.createPresyncCheckpoint(function () {
        resolve();
      });
    }).then(function () {
      return new Promise(function (resolve, reject) {
        me.moveToPrechangeCheckpoint(function () {
          resolve();
        });
      });
    }).then(function () {
      return Promise.all(me.preSyncPromises);
    }).then(

    function () {
      me.preSyncPromises = [];
      if (callback) {
        callback();
      }
    }, function (reason) {
      OB.error("Error while processing promise " + reason);
    });
  },
  doPostSynchronizedCallActions: function (callback, syncData) {
    if (OB.UTIL.localStorage.getItem('synchronizedMessageId') === syncData.messageId) {
      OB.UTIL.localStorage.removeItem('synchronizedMessageId');
    } else {
      OB.error("Message id of the last request doesn't much with the current messageId");
    }
    if (callback) {
      callback();
    }
  },
  /**
   * Sets the OBMOBC_SynchronizedMode permission and returns the previous value.
   * Can be used to temporarily disable synchronized mode.
   */
  setSynchronizedPreference: function (newValue) {
    var currentValue = OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true);
    OB.MobileApp.model.get('permissions').OBMOBC_SynchronizedMode = newValue;
    return currentValue;
  },

  /**
   * Should not be called directly. syncAllModels should be used instead
   *
   * Recursively syncs models. The recursion will end if:
   *   - all models have successfully synchronized
   *  or if
   *   - an exception is raised
   *
   */
  syncModelQueue: null,
  syncModel: function () {
    var me = this;

    // stop the recursion when the queue of syncModels is empty
    // the rest of the actions are in the removeSyncedElemsCallback
    if (me.syncModelQueue.length === 0) {
      //There aren't anymore elements to synchronize
      if (OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true)) {

        // clean up in any case after success or error
        // note: moveToPresyncCheckpoint must be called before the reset
        me.syncModelSuccessCallbacks.unshift(function () {
          me.resetCheckpointData();
        });
        me.syncModelErrorCallbacks.push(function () {
          me.resetCheckpointData();
        });

        // nothing to do go away
        if (me.synchronizedData.length === 0) {
          me.syncModelExecuteSuccessCallbacks();
          return;
        }

        // already show the dialog now
        if (!me.showSynchronizedDialog) {
          me.showSynchronizingDialog();
        }
        //now send all the collected json as one request
        var syncProc = new OB.DS.Process('org.openbravo.mobile.core.servercontroller.SynchronizedServerProcessCaller');
        var syncData = {
          messageId: OB.UTIL.get_UUID(),
          _source: 'WEBPOS',
          _executeInOneServer: true,
          _tryCentralFromStore: true,
          posTerminal: OB.MobileApp.model.get('terminal').id,
          data: me.synchronizedData,
          extraParams: {
            isSynchronizeModeTransaction: true
          }
        };

        // clear some memory
        me.synchronizedData = [];

        // and execute the complete request, first do the presync actions
        me.doPreSynchronizedCallActions(function () {
          syncProc.exec(syncData, function () {
            var args = arguments;
            me.doPostSynchronizedCallActions(function () {
              var params;
              if (args.length > 0) {
                params = args[0];
                if (args[0].result && args[0].result.length > 0) {
                  params = args[0].result[0];
                }
              }
              me.syncModelExecuteSuccessCallbacks(params);
            }, syncData);
          }, function () {
            var args = arguments;
            me.syncModelExecuteErrorCallbacks(args);
          }, null, me.synchronizedTimeOut);
        }, syncData);
      } else {
        me.syncModelExecuteSuccessCallbacks();
      }
      return;
    }

    var modelObj = me.syncModelQueue.shift();
    var model = modelObj.model;
    var criteria;
    if (modelObj.getCriteria) {
      criteria = modelObj.getCriteria();
    } else if (modelObj.criteria) {
      criteria = modelObj.criteria;
    } else {
      criteria = {
        hasBeenProcessed: 'Y'
      };
    }
    criteria._limit = -1;
    OB.Dal.find(model, criteria, function (dataToSync) {

      me.skipSyncModel = dataToSync.length === 0;

      if (modelObj.preSendModel) {
        // preSendModel is like a hook which can be used by custom code
        // to influence the logic, for example setting me.skipSyncModel
        modelObj.preSendModel(me, dataToSync);
      }

      if (me.skipSyncModel) {
        me.skipSyncModel = false;
        me.syncModel();
        return;
      }

      var className = modelObj.className;
      if (model === OB.Model.ChangedBusinessPartners && OB.UTIL.processCustomerClass) {
        className = OB.UTIL.processCustomerClass;
      } else if (model === OB.Model.Order && OB.UTIL.processOrderClass) {
        className = OB.UTIL.processOrderClass;
      }

      var mdl, modelIndex = 0,
          modelNotFullySynced = false,
          dataCorruption = false,
          newDataToSync = new Backbone.Collection();
      while (modelIndex < dataToSync.length) {
        mdl = dataToSync.at(modelIndex);
        if (!_.isUndefined(mdl.get('json')) && (_.isEmpty(mdl.get('json')) || _.isNull(mdl.get('json')))) {
          OB.error('[syncModel] Wrong model to Synchronize in backend: ' + mdl.modelName + ' because json column is undefined.');
          dataCorruption = true;
        } else {
          newDataToSync.add(dataToSync.at(modelIndex));
        }

        modelIndex++;
        // partition the data in chunks
        if (modelIndex >= 100 && modelIndex < dataToSync.length) {
          modelNotFullySynced = true;
          // add the model again to the beginning of the queue
          me.syncModelQueue.unshift(modelObj);
          break;
        }
      }
      if (dataCorruption) {
        var modelProto = model.constructor.prototype;
        var criteria = {
          whereClause: ' WHERE json is null OR json= "" '
        };
        OB.Dal.removeAll(model, criteria);
      }

      var dataToSend = [];
      newDataToSync.each(function (record) {
        if (record.get('json')) {
          dataToSend.push(JSON.parse(record.get('json')));
        } else if (!_.isUndefined(record.get('objToSend'))) {
          if (!_.isNull(record.get('objToSend'))) {
            dataToSend.push(JSON.parse(record.get('objToSend')));
          }
        } else {
          dataToSend.push(record);
        }
      });

      var timeout = modelObj.timeout || 20000;
      var timePerRecord = modelObj.timePerRecord || 1000;
      var data = {
        messageId: OB.UTIL.get_UUID(),
        data: dataToSend
      };

      // add an additional pre-sync action, remove any non-persistent data
      // before really going to the server
      if (OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true)) {
        newDataToSync.each(function (record) {
          me.preSyncPromises.push(new Promise(function (resolve, reject) {
            if (!modelObj.isPersistent) {
              OB.Dal.remove(record, function () {
                resolve();
              }, function (tx, err) {
                OB.UTIL.showError(err);
                reject();
              });
            } else {
              resolve();
            }
          }));
        });
      }

      var procErrorCallBack = function () {
          var promises = [];

          // proc.exec  mcallbackerror
          OB.warn("Error while synchronizing model '" + OB.Dal.getTableName(model) + "'");

          me.syncModelExecuteErrorCallbacks();
          };

      var procSuccessCallBack = function (data, message, lastUpdated, endRow, tx) {

          // error
          if (data && data.exception) {
            OB.warn("The model '" + OB.Dal.getTableName(model) + "'' has not been synchronized with the server");
            if (data.exception.invalidPermission && !me.get('displayedInvalidPermission')) {
              // invalid permission message only will be displayed once time
              me.set('displayedInvalidPermission', true);
              OB.UTIL.showConfirmation.display('Info', OB.I18N.getLabel('OBMOBC_NoPermissionToSyncModel', [OB.Dal.getTableName(model), OB.Dal.getTableName(model)]), [{
                label: OB.I18N.getLabel('OBMOBC_LblOk'),
                isConfirmButton: true,
                action: function () {}
              }]);
            }
            me.syncModelExecuteErrorCallbacks();
            return;
          }
          // success. Elements can be now deleted from the database
          var removeSyncedElemsCallback = function (tx) {
              OB.info("Purging the '" + OB.Dal.getTableName(model) + "' table");
              var promises = [];

              if (modelObj.successSendModel) {
                promises.push(new Promise(function (resolve, reject) {
                  modelObj.successSendModel();
                  resolve();
                }));
              }

              newDataToSync.each(function (record) {
                promises.push(new Promise(function (resolve, reject) {
                  if (modelObj.isPersistent) {
                    // Persistent model. Do not delete, just mark it as processed.
                    OB.Dal.updateRecordColumn(record, "isbeingprocessed", "Y", function () {
                      resolve();
                    }, function (tx, err) {
                      reject();
                    });
                  } else {
                    // no persistent model (Default).
                    OB.Dal.removeInTransaction(tx, record, function () {
                      resolve();
                    }, function (tx, err) {
                      OB.UTIL.showError(err);
                      reject();
                    });
                  }
                }));
              });

              Promise.all(promises).then(function () {
                // if the model has been partitioned
                if (modelNotFullySynced) {
                  OB.info(newDataToSync.length + " records of the table '" + OB.Dal.getTableName(model) + "' have been successfully synchronized with the server. " + (dataToSync.length - newDataToSync.length) + " records remaining to be synchronized.");
                  me.syncModel();
                  return;
                }
                OB.info("The table '" + OB.Dal.getTableName(model) + "' has been fully synchronized with the server");
              }, function (err) {
                OB.error("Could not purge the '" + OB.Dal.getTableName(model) + "' table. Error message: " + err);
              });
              // not synchronized mode do the next in the success callback
              // with synchronized mode the recall syncmodel is below in the code
              if (!OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true)) {
                me.syncModel();
              }
              };
          if (modelObj.postProcessingFunction) {
            modelObj.postProcessingFunction(newDataToSync, removeSyncedElemsCallback);
          } else {
            removeSyncedElemsCallback(tx);
          }
          };

      if (OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true)) {
        me.collectSyncData(className, data, timeout + timePerRecord * newDataToSync.length);
        me.syncModelSuccessCallbacks.push(procSuccessCallBack);
        me.syncModel();
      } else {
        var proc = new OB.DS.Process(className);
        proc.exec(data, procSuccessCallBack, procErrorCallBack, null, timeout + timePerRecord * newDataToSync.length);
      }
    },


    function () {
      // there is no model in the local database. this is ok. move to the next model
      me.syncModel();
    }, {
      // do not show an error when the table is not present in the local database because it could happen when the cache is still clean
      doNotShowErrors: true
    });
  },
  syncModelSuccessCallbacks: [],
  syncModelErrorCallbacks: [],
  syncModelExecuteSuccessCallbacks: function (result) {
    OB.UTIL.Debug.execute(function () {
      if (OB.MobileApp.model.syncModelQueue && OB.MobileApp.model.syncModelQueue.length > 0) {
        throw "The 'syncModelQueue' should be empty at this point";
      }
    });

    // exception occurred but was returned in the json as a valid response
    if (result && result.exception) {
      OB.MobileApp.model.syncModelExecuteErrorCallbacks(arguments);
      return;
    }
    if (OB.MobileApp.model.showSynchronizedDialog) {
      OB.MobileApp.model.hideSynchronizingDialog();
    }

    OB.MobileApp.model.syncModelQueue = [];
    OB.MobileApp.model.syncModelErrorCallbacks = [];
    new Promise(function (resolve, reject) {
      // is only doing anything in case of synchronized mode
      OB.MobileApp.model.moveToPresyncCheckpoint(function () {
        resolve();
      });
    }).then(function () {
      OB.UTIL.executeCallbackQueue(OB.MobileApp.model.syncModelSuccessCallbacks, result);
    });
  },
  syncModelExecuteErrorCallbacks: function () {
    OB.MobileApp.model.syncModelQueue = [];
    OB.MobileApp.model.syncModelSuccessCallbacks = [];

    if (OB.MobileApp.model.hasPermission('OBMOBC_SynchronizedMode', true)) {
      var msg = arguments[0][0] && arguments[0][0].exception ? arguments[0][0].exception.message : "No Details";

      if (OB.MobileApp.model.showSynchronizedDialog) {
        OB.MobileApp.model.hideSynchronizingDialog();
      }

      OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_TransactionFailedTitle'), OB.I18N.getLabel('OBMOBC_TransactionFailed', [msg]), [{
        isConfirmButton: true,
        label: OB.I18N.getLabel('OBMOBC_LblOk'),
        action: function () {
          if (OB && OB.POS) {
            OB.POS.navigate('retail.pointofsale');
            return true;
          }
        }
      }]);
    }

    OB.UTIL.executeCallbackQueue(OB.MobileApp.model.syncModelErrorCallbacks);
  },

  // collect the data to sync
  collectSyncData: function (source, params, timeout) {
    var data = OB.DS.Process.prepareData(params);

    data._serviceName = source;

    this.synchronizedTimeOut = this.synchronizedTimeOut + timeout;

    this.synchronizedData.push(data);
  },

  /**
   * Synchronizes all dataSyncModels (models which data is kept localy until flushed to the server)
   *
   * If this method is called while a previous data synchronization is in place:
   * - the data synchronization of the model being synchronized at the time this method is called, will be finished
   * - the remaining models will not be synchronized
   * - the synchronization will start over with the first model
   * - all the callbacks (success or error) are gathered in a queue, all of them will be executed when the synchronization ends
   */
  syncAllModels: function (successCallback, errorCallback) {
    var me = this;

    // if the user is not authenticated, the server will reject all the connections
    if (!OB.MobileApp.model.isUserAuthenticated()) {
      OB.UTIL.Debug.execute(function () {
        console.warn("'OB.MobileApp.model.syncAllModels' cannot be executed because it requires an autheticated user");
      });
      if (errorCallback) {
        errorCallback();
      }
      return;
    }
    // if there is no model to be synchronized
    if (!me.get('dataSyncModels') || me.get('dataSyncModels').length === 0) {
      if (successCallback) {
        successCallback();
      }
      return;
    }
    // flow note: this line must be located before the queues are reset. Check models queue and callbacks queues, the process finishes when the callback are called
    var isCurrentlySynchronizing = (me.syncModelQueue && me.syncModelQueue.length > 0) || (me.syncModelSuccessCallbacks && me.syncModelSuccessCallbacks.length > 0) || (me.syncModelErrorCallbacks && me.syncModelErrorCallbacks.length > 0);
    // start the synchronization if it was not already in progress
    if (!isCurrentlySynchronizing) {

      me.syncModelSuccessCallbacks = [];
      me.syncModelErrorCallbacks = [];

      // remember the callbacks in the callbacks queue
      me.syncModelSuccessCallbacks.push(successCallback);
      me.syncModelErrorCallbacks.push(errorCallback);

      // all models must restart the synchronization when syncAllModels is called. this is attained reseting the queue
      me.syncModelQueue = [];
      // add all the models to the queue
      var i;
      for (i = 0; i < this.get('dataSyncModels').length; i++) {
        var modelObj = this.get('dataSyncModels')[i];
        me.syncModelQueue.push(modelObj);
      }

      // reset some members used in synchronized comms
      me.synchronizedData = [];
      me.synchronizedTimeOut = 0;

      me.syncModel(this.syncModelExecuteSuccessCallbacks, this.syncModelExecuteErrorCallbacks);
    }
  },

  /**
   * Returns true if a user is authenticated
   */
  isUserAuthenticated: function () {
    return !!OB.MobileApp.model.get('context');
  },

  /**
   * If any module needs to perform operations with properties coming from the server
   * this function must be overwritten
   */
  isUserCacheAvailable: function () {
    return false;
  },

  /**
   * Invoked from initComponents in terminal view
   */
  initializeCommonComponents: function () {
    OB.debug("next process: navigate 'login' or renderTerminalMain or loginUsingCache");

    var me = this,
        params = {};
    OB.UTIL.localStorage.setItem('LOGINTIMER', new Date().getTime());

    params = this.get('loginUtilsParams') || {};
    params.command = 'preRenderActions';
    params.appModuleId = this.get('appModuleId');
    // initialize labels and other common stuff
    var rr, ajaxRequest = new enyo.Ajax({
      url: this.get('loginUtilsUrl'),
      cacheBust: false,
      timeout: 5000,
      // don't do retries as this gives side effects as re-showing login page
      maxNumOfRequestRetries: 0,
      method: 'GET',
      handleAs: 'json',
      contentType: 'application/json;charset=utf-8',
      data: params,
      success: function (inSender, inResponse) {
        OB.appCaption = inResponse.appCaption;
        OB.UTIL.localStorage.setItem('POSlanguageId', inResponse.labels.languageId);
        OB.UTIL.localStorage.setItem('POSlanguageSK', inResponse.labels.languageSk);
        if (_.isEmpty(inResponse.labels) || _.isNull(inResponse.labels) || _.isUndefined(inResponse.labels)) {
          OB.I18N.labels = JSON.parse(OB.UTIL.localStorage.getItem('I18NLabels'));
        } else {
          OB.I18N.labels = inResponse.labels;
          if (inResponse.activeSession) {
            // The labels will only be saved in the localStorage if they come from an active session
            // We do this to prevent the labels loaded in the login page from being stored here and overwritting
            // the labels in the correct language (see issue 23613)
            OB.UTIL.localStorage.setItem('languageForUser_' + inResponse.labels.userId, inResponse.labels.languageId);
            OB.UTIL.localStorage.setItem('I18NLabels', JSON.stringify(OB.I18N.labels));
            OB.UTIL.localStorage.setItem('I18NLabels_' + inResponse.labels.languageId, JSON.stringify(OB.I18N.labels));
          }
        }
        if (_.isEmpty(inResponse.lists) || _.isNull(inResponse.lists) || _.isUndefined(inResponse.lists)) {
          OB.I18N.lists = JSON.parse(OB.UTIL.localStorage.getItem('I18NLists'));
        } else {
          OB.I18N.lists = inResponse.lists;
          if (inResponse.activeSession) {
            // The reference list will only be saved in the localStorage if they come from an active session
            // We do this to prevent the reference list loaded in the login page from being stored here and overwritting
            // the reference list in the correct language
            OB.UTIL.localStorage.setItem('I18NLists', JSON.stringify(OB.I18N.lists));
            OB.UTIL.localStorage.setItem('I18NLists_' + inResponse.lists.languageId, JSON.stringify(OB.I18N.lists));
          }
        }
        if (_.isEmpty(inResponse.dateFormats) || _.isNull(inResponse.dateFormats) || _.isUndefined(inResponse.dateFormats)) {
          OB.Format = JSON.parse(OB.UTIL.localStorage.getItem('AppFormat'));
        } else {
          OB.Format = inResponse.dateFormats;
        }

        var permissions = {},
            i, separator;
        if (inResponse.permissions) {
          for (i = 0; i < inResponse.permissions.length; i++) {
            permissions[inResponse.permissions[i].key] = inResponse.permissions[i].value; // Add the permission value.
            separator = inResponse.permissions[i].key.indexOf('_');
            if (separator >= 0) {
              permissions[inResponse.permissions[i].key.substring(separator + 1)] = inResponse.permissions[i].value; // if key has a DB prefix, add also the permission value without this prefix
            }
          }
        } else {
          permissions = null;
        }

        me.set('permissions', permissions);


        if ('invalidRequestOrigin' === inResponse.exception) {
          OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_InvalidOriginTitle'), OB.I18N.getLabel('OBMOBC_InvalidOriginMsg'));
          return;
        }
        me.trigger('initializedCommonComponents');
        me.preLoadContext(function () {


          if (!inResponse.contextInfo) {
            //Not authenticated, need to navigate to login page
            OB.MobileApp.model.navigate('login');
            return;
          }
          OB.MobileApp.model.triggerOnLine();
          OB.MobileApp.model.set('isLoggingIn', true);
          OB.MobileApp.model.set('orgUserId', inResponse.contextInfo.user.id);
          me.set('context', inResponse.contextInfo); // fires the 'change:context' event
          window.OB.Format = inResponse.format;
          window.OB.UTIL.localStorage.setItem('AppFormat', JSON.stringify(OB.Format));
          OB.UTIL.startLoadingSteps();
          me.renderTerminalMain();
          return;
        });
      },
      fail: function (inSender, inResponse) {
        if (this.alreadyProcessed) {
          return;
        }
        this.alreadyProcessed = true;
        // we are likely offline. Attempt to navigate to the login page
        OB.I18N.labels = JSON.parse(OB.UTIL.localStorage.getItem('I18NLabels'));
        OB.Format = JSON.parse(OB.UTIL.localStorage.getItem('AppFormat'));
        me.trigger('initializedCommonComponents');
        if (!OB.I18N.labels || !OB.Format) {
          // These 2 objects are needed for the application to show i18n messages and date and number formats
          OB.UTIL.showLoading(false);
          var errorMsg = "The server is not available and the cache has not enough data. Connect to the server and try again.";
          OB.error("initializeCommonComponents", errorMsg);
          OB.UTIL.showConfirmation.display('Error', errorMsg, [{
            label: 'Retry',
            isConfirmButton: true,
            action: function () {
              // Retry. If the server is not available, the message will open again
              me.initializeCommonComponents();
            }
          }], {
            onHideFunction: function () {
              // Retry. If the server is not available, the message will open again
              me.initializeCommonComponents();
            }
          });
          return;
        }
        me.attemptToLoginOffline();
      }
    });

    rr = new OB.RR.Request({
      ajaxRequest: ajaxRequest
    });
    rr.exec(ajaxRequest.url);
  },

  renderLogin: function () {
    if (!OB.MobileApp.view) {
      // the terminal view has yet to be created
      OB.UTIL.Debug.execute(function () {
        throw "OB.MobileApp.view must be defined";
      });
      OB.MobileApp.model.navigate('');
      return;
    }
    if (!OB.MobileApp.view.$.containerWindow) {
      // this can happen if the webpage is reloaded
      // OB.UTIL.Debug.execute(function () {
      //   console.error("OB.MobileApp.view.$.containerWindow must be defined");
      // });
      OB.MobileApp.model.navigate('');
      return;
    }

    if (OB.Data.localDB) {
      OB.Dal.initCache(OB.Model.LogClient, [], null, null);
    }

    OB.MobileApp.view.$.containerWindow.destroyComponents();
    OB.MobileApp.view.$.containerWindow.createComponent({
      kind: OB.OBPOSLogin.UI.Login
    }).render();
    OB.UTIL.showLoading(false);
  },

  loadModels: function (windowv, incremental) {
    var windows, i, windowName, windowClass, datasources, timestamp = 0,
        w, c, path;

    if (OB.MobileApp.model.get('loggedOffline')) {
      return;
    }

    if (windowv) {
      windows = [windowv];
    } else {
      windows = [];
      _.each(this.windowRegistry.registeredWindows, function (windowp) {
        windows.push(windowp);
      });
    }

    OB.info('[sdrefresh] Load models ' + (incremental ? 'incrementally' : 'full') + '.');

    for (i = 0; i < windows.length; i++) {
      windowClass = windows[i].windowClass;
      windowName = windows[i].route;
      if (OB && OB.DATA && OB.DATA[windowName]) {
        // old way of defining datasources...
        datasources = OB.DATA[windowName];
      } else if (windowClass.prototype && windowClass.prototype.windowmodel && windowClass.prototype.windowmodel.prototype && windowClass.prototype.windowmodel.prototype.models) {
        datasources = windowClass.prototype.windowmodel.prototype.models;
      } else if (typeof windowClass === 'string') {
        w = window; // global window
        path = windowClass.split('.');
        for (c = 0; c < path.length; c++) {
          w = w[path[c]];
        }

        if (w.prototype && w.prototype.windowmodel && w.prototype.windowmodel.prototype && w.prototype.windowmodel.prototype.models) {
          datasources = w.prototype.windowmodel.prototype.models;
        }
      }

      _.extend(datasources, Backbone.Events);

      OB.debug('[sdrefresh] window: ' + windowName);

      OB.Dal.loadModels(false, datasources, null, incremental);

      this.postLoadModels();
    }
  },
  postLoadModels: function () {

  },

  cleanWindows: function () {
    this.windows = new(Backbone.Collection.extend({
      comparator: function (window) {
        // sorts by menu position, 0 if not defined
        var position = window.get('menuPosition');
        return position ? position : 0;
      }
    }))();
  },

  registerWindow: function (window) {
    this.windowRegistry.registerWindow(window);
  },

  /**
   * Iterates over all registered windows loading their models
   */
  loadRegisteredWindows: function () {
    var me = this;
    var func_handleRouting = function (windowName, params) {
        //TODO default route
        var defaultRoute = OB.MobileApp.model.get('defaultWindow');
        //Find dest window
        var destinationWindow = _.find(OB.MobileApp.windowRegistry.registeredWindows, function (win) {
          return win.route === windowName;
        });
        //Check if destination window is valid. If not navigate to main window
        if (OB.UTIL.isNullOrUndefined(destinationWindow)) {
          OB.MobileApp.model.navigate(defaultRoute);
        }
        //If destination window has permission, check if user is allowed to navigate
        //if not, navigate to main window
        if (destinationWindow.permission) {
          if (!OB.MobileApp.model.hasPermission(destinationWindow.permission)) {
            OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_NotAllowed'), OB.I18N.getLabel('OBMOBC_AccessNotAllowed', [destinationWindow.permission]), null, {
              onHideFunction: function () {
                OB.MobileApp.model.navigate(defaultRoute);
              }
            });
            return;
          }
        }

        //Execute hook -> OBMOBC_PreWindowNavigate
        OB.UTIL.HookManager.executeHooks('OBMOBC_PreWindowNavigate', {
          window: destinationWindow,
          route: destinationWindow.route,
          params: params
        }, function (args) {
          if (args && args.cancellation && args.cancellation === true) {
            OB.MobileApp.model.navigate(defaultRoute);
            return;
          }

          if (args.window && args.window.navigateTo) {
            args.window.navigateTo(args, function () {
              me.renderGenericWindow(args.route, params);
            }, function () {
              OB.MobileApp.model.navigate(defaultRoute);
            });
          } else if (args.route) {
            me.renderGenericWindow(args.route, params);
          }
        });
        };

    OB.debug("next process: renderMain");

    this.cleanWindows();
    OB.MobileApp.model.set('modelsToLoad', []);

    OB.UTIL.activateLogClient();

    var countOfLoadedWindows = 0;
    _.each(this.windowRegistry.registeredWindows, function (windowp) {
      var datasources = [],
          windowClass, windowName = windowp.route,
          minTotalRefresh, minIncRefresh, lastTotalRefresh, lastIncRefresh, intervalTotal, intervalInc, now, terminalType;

      this.windows.add(windowp);

      terminalType = OB.MobileApp.model.get('terminal') && OB.MobileApp.model.get('terminal').terminalType;
      if (terminalType) {
        minTotalRefresh = OB.MobileApp.model.get('terminal').terminalType.minutestorefreshdatatotal * 60 * 1000;
        minIncRefresh = OB.MobileApp.model.get('terminal').terminalType.minutestorefreshdatainc;
        lastTotalRefresh = OB.UTIL.localStorage.getItem('POSLastTotalRefresh');
        lastIncRefresh = OB.UTIL.localStorage.getItem('POSLastIncRefresh');

        // lastTotalRefresh should be used to set lastIncRefresh when it is null or minor.
        if (lastIncRefresh === null) {
          lastIncRefresh = lastTotalRefresh;
        } else {
          if (lastTotalRefresh > lastIncRefresh) {
            lastIncRefresh = lastTotalRefresh;
          }
        }
        minIncRefresh = (minIncRefresh > 99999 ? 99999 : minIncRefresh) * 60 * 1000;
      }
      if ((!minTotalRefresh && !minIncRefresh) || (!lastTotalRefresh && !lastIncRefresh)) {
        // If no configuration of the masterdata loading has been done,
        // or an initial load has not been done, then always do
        // a total refresh during the login
        this.loadModels(windowp, false);
      } else {
        now = new Date().getTime();
        intervalTotal = lastTotalRefresh ? (now - lastTotalRefresh - minTotalRefresh) : 0;
        intervalInc = lastIncRefresh ? (now - lastIncRefresh - minIncRefresh) : 0;
        if (intervalTotal > 0) {
          this.set('FullRefreshWasDone', true);

          //It's time to do a full refresh
          this.loadModels(windowp, false);
        } else {
          if (OB.MobileApp.model.hasPermission('OBMOBC_NotAutoLoadIncrementalAtLogin', true)) {
            if (intervalInc >= 0) {
              this.loadModels(windowp, true);
            }
          } else {
            if (OB.UTIL.isNullOrUndefined(lastTotalRefresh)) {
              this.set('FullRefreshWasDone', true);

              //It's time to do a full refresh
              this.loadModels(windowp, false);
            } else {
              this.loadModels(windowp, true);
            }
          }
        }
      }

      this.router.route(windowName, windowName, function () {
        func_handleRouting(windowName);
      });

      this.router.route(windowName + "/:params", windowName, function (params) {
        func_handleRouting(windowName, params);
      });
      countOfLoadedWindows += 1;
    }, this);
    // checks if the windows loaded are more than the windows registered, which never should happen
    var countOfRegisteredWindows = this.windowRegistry.registeredWindows.length;
    console.assert(countOfRegisteredWindows === countOfLoadedWindows, 'DEVELOPER: There are ' + countOfRegisteredWindows + ' registered windows but ' + countOfLoadedWindows + ' are being loaded');
  },

  showFrozenPopUp: function () {
    OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_BrowserTabHeader', [OB.MobileApp.model.get('appName')]), OB.I18N.getLabel('OBMOBC_BrowserTabBody', [OB.MobileApp.model.get('appName')]), undefined, {
      onShowFunction: function (popup) {
        popup.$.headerCloseButton.hide();
        popup.$.bodyButtons.$.confirmationPopup_btnOk.hide();
      },
      autoDismiss: false
    });
  },

  saveBrowserTabId: function () {
    var me = this;
    var browserTabId = OB.UTIL.get_UUID();
    if (OB.MobileApp.model.get('terminal')) {
      OB.MobileApp.model.get('terminal').browserTabId = browserTabId;
    } else {
      OB.MobileApp.model.browserTabId = browserTabId;
    }
    OB.UTIL.localStorage.setItem('browserTabId', browserTabId);

    // When focus the browser tab
    window.onfocus = function () {
      var actualBrowserTabId = OB.MobileApp.model.get('terminal') ? OB.MobileApp.model.get('terminal').browserTabId : OB.MobileApp.model.browserTabId;
      if (OB.UTIL.localStorage.getItem('browserTabId') !== actualBrowserTabId) {
        me.showFrozenPopUp();
      }
    };
  },

  isWindowOnline: function (route) {
    var i, windows;
    windows = this.windows.toArray();
    for (i = 0; i < windows.length; i++) {
      if (windows[i].get('route') === route) {
        return windows[i].get('online');
      }
    }
    return false;
  },

  renderGenericWindow: function (windowName, params) {
    if (!OB.MobileApp.view.$.containerWindow) {
      OB.UTIL.Debug.execute(function () {
        console.error("OB.MobileApp.view.$.containerWindow must be defined");
      });
      OB.MobileApp.model.loadingErrorsActions('renderGenericWindow I');
      return;
    }

    OB.MobileApp.view.currentWindow = 'unknown';
    OB.MobileApp.model.set('currentWindow', 'unknown');
    OB.MobileApp.view.currentWindowState = 'unknown';
    OB.MobileApp.model.set('currentWindowState', 'unknown');
    OB.UTIL.showLoading(true);

    var windowObject = this.windows.where({
      route: windowName
    })[0];
    var windowClass = windowObject.get('windowClass');
    OB.MobileApp.view.$.containerWindow.destroyComponents();
    OB.MobileApp.view.$.containerWindow.createComponent({
      kind: windowClass,
      params: params
    });

    //remove possible external classes
    _.each(this.windows.models, function (win) {
      if (win && win.get('containerCssClass')) {
        if (OB.MobileApp.view.hasClass(win.get('containerCssClass'))) {
          OB.MobileApp.view.removeClass(win.get('containerCssClass'));
        }
      }
    });
    //Add original css-class
    if (!OB.MobileApp.view.hasClass('pos-container')) {
      OB.MobileApp.view.addClass('pos-container');
    }
    //If window is overwritting container, remove original and put new one
    if (windowObject && windowObject.get('containerCssClass')) {
      if (windowObject.get('overwriteOriginalContainerCssClass')) {
        OB.MobileApp.view.removeClass('pos-container');
      }
      OB.MobileApp.view.addClass(windowObject.get('containerCssClass'));
    }

    OB.MobileApp.view.currentWindow = windowName;
    OB.MobileApp.model.set('currentWindow', windowName);
  },

  renderContainerWindow: function () {
    if (!OB.MobileApp.view.$.containerWindow.showing) {
      OB.MobileApp.view.$.containerWindow.render();
      OB.UTIL.showLoading(false);
      OB.MobileApp.view.$.containerWindow.resized();
    }
  },

  updateSession: function (user, callback) {
    // argument check
    OB.UTIL.Debug.execute(function () {
      if (!callback) {
        throw "This method requires a callback";
      }
    });

    OB.Dal.find(OB.Model.Session, {
      'user': user.get('id')
    }, function (sessions) {
      var session;
      if (sessions.models.length === 0) {
        session = new OB.Model.Session();
        session.set('user', user.get('id'));
        session.set('terminal', OB.MobileApp.model.get('terminalName'));
      } else {
        session = sessions.models[0];
      }
      session.set('active', 'Y');
      OB.Dal.save(session, callback, function () {
        OB.error("updateSession: failed to save the existing session", arguments);
      });
      OB.MobileApp.model.set('session', session.get('id'));
    });
  },

  /**
   * This method is invoked when closing session, it is in charge
   * of dealing with all stuff needed to close session. After it is
   * done it MUST do triggerLogout
   */
  postCloseSession: function (session) {
    OB.MobileApp.model.triggerLogout();
  },

  closeSession: function () {
    var sessionId = OB.MobileApp.model.get('session'),
        me = this;
    if (!this.get('supportsOffline') || !OB.Model.Session || !sessionId) {
      OB.MobileApp.model.triggerLogout();
      return;
    }

    OB.Dal.get(OB.Model.Session, sessionId, function (session) {
      if (!session) {
        OB.MobileApp.model.triggerLogout();
        return;
      }
      session.set('active', 'N');
      OB.Dal.save(session, function () {
        me.postCloseSession(session);
      }, function () {
        OB.error("closeSession", arguments);
        OB.MobileApp.model.triggerLogout();
      });
    }, function () {
      OB.MobileApp.model.triggerLogout();
    }, function () {
      OB.MobileApp.model.triggerLogout();
    });
  },

  generate_sha1: function (theString) {
    return CryptoJS.enc.Hex.stringify(CryptoJS.SHA1(theString));
  },

  attemptToLoginOffline: function () {
    var me = this;
    if (!me.user) {
      OB.MobileApp.model.navigate('login');
      return;
    }
    OB.MobileApp.model.set('windowRegistered', undefined);
    OB.MobileApp.model.set('loggedOffline', true);
    OB.Dal.find(OB.Model.User, {
      'name': me.user
    }, function (users) {
      var user;
      if (users.models.length === 0) {
        OB.UTIL.showConfirmation.display('', OB.I18N.getLabel('OBMOBC_OfflinePasswordNotCorrect'), null, {
          onHideFunction: function () {
            window.location.reload();
          }
        });
      } else {
        if (users.models[0].get('password') === me.generate_sha1(me.password + users.models[0].get('created'))) {
          me.usermodel = users.models[0];
          me.set('orgUserId', users.models[0].id);
          me.updateSession(me.usermodel, function () {
            me.renderTerminalMain();
          });
        } else {
          OB.UTIL.showConfirmation.display('', OB.I18N.getLabel('OBMOBC_OfflinePasswordNotCorrect'), null, {
            onHideFunction: function () {
              window.location.reload();
            }
          });
        }
      }
    }, function () {});
  },

  postLoginActions: function () {
    OB.debug("next process: none");
  },

  loginUsingCache: function () {
    OB.debug("next process: renderTerminalMain");
    var me = this;
    OB.Dal.find(OB.Model.User, {
      'id': OB.MobileApp.model.get('orgUserId')
    }, function (users) {
      if (!users.models[0]) {
        // the data for the user is not valid
        OB.MobileApp.model.loadingErrorsActions('loginUsingCache I');
        return;
      }
      me.usermodel = users.models[0];
      me.set('orgUserId', users.models[0].id);
      OB.MobileApp.model.set('windowRegistered', undefined);
      me.updateSession(me.usermodel, function () {
        me.renderTerminalMain();
      });
    }, function () {
      // if the cache is corrupted
      OB.MobileApp.model.loadingErrorsActions('loginUsingCache II');
    });
  },

  /**
   * Prelogin actions is executed before login, it should take care of
   * removing all session values specific by application.
   *
   * Override this in case you app needs to do something special
   */
  preLoginActions: function () {
    OB.debug("next process: none. set your application start point here");
  },

  login: function (user, password, mode, command) {
    if (this.get('isLoggingIn') === true) {
      OB.UTIL.Debug.execute(function () {
        throw "There is already a logging process in progress";
      });
      return;
    }
    this.set('isLoggingIn', true);

    var params;
    OB.UTIL.showLoading(true);
    var me = this;
    me.user = user;
    me.password = password;

    // invoking app specific actions
    this.preLoginActions();

    params = JSON.parse(JSON.stringify(this.get('loginUtilsParams'))) || {};
    params.user = user;
    params.password = password;
    params.Command = command ? command : 'DEFAULT';
    if (params.Command === 'FORCE_RESET_PASSWORD') {
      params.resetPassword = true;
    }
    params.IsAjaxCall = 1;
    params.appName = this.get('appName');

    var rr, ajaxRequest = new enyo.Ajax({
      url: this.get('loginHandlerUrl'),
      cacheBust: false,
      method: 'POST',
      timeout: 5000,
      // don't do retries as this gives side effects as re-showing login page
      maxNumOfRequestRetries: 0,
      contentType: 'application/x-www-form-urlencoded; charset=UTF-8',
      data: params,
      success: function (inSender, inResponse) {
        var pos, baseUrl;
        if (this.alreadyProcessed) {
          return;
        }
        this.alreadyProcessed = true;
        if (inResponse && inResponse.sourceVersion && (OB.RR.RequestRouter.servers.length === 1 || inSender.url.indexOf(document.location.host) !== -1)) {
          OB.UTIL.checkSourceVersion(inResponse.sourceVersion, true);
        }
        if (OB.MobileApp.model.get('timeoutWhileLogin')) {
          OB.MobileApp.model.set('timeoutWhileLogin', false);
          return;
        }
        if (inResponse && inResponse.showMessage) {
          if (inResponse.command === 'FORCE_NAMED_USER') {
            OB.UTIL.showConfirmation.display(inResponse.messageTitle, inResponse.messageText, [{
              label: OB.I18N.getLabel('OBMOBC_LblOk'),
              action: function () {
                OB.MobileApp.model.set('isLoggingIn', false);
                OB.MobileApp.model.login(user, password, mode, 'FORCE_NAMED_USER');
                return;
              }
            }, {
              label: OB.I18N.getLabel('OBMOBC_LblCancel'),
              action: function () {
                OB.MobileApp.model.set('isLoggingIn', false);
                return;
              }
            }], {
              autoDismiss: false
            });
            return;
          } else if (inResponse.resetPassword) {
            OB.UTIL.showLoading(false);
            this.dialog = OB.MobileApp.view.$.confirmationContainer.createComponent({
              kind: 'OB.UI.ExpirationPassword',
              context: this
            });
            this.dialog.show();
            this.dialog.$.bodyContent.$.newheader.setContent(inResponse.messageTitle);
            return;
          } else {
            me.triggerLoginFail(401, mode, inResponse);
            return;
          }
        }
        if (OB.MobileApp.model.get('terminalName') && !OB.UTIL.localStorage.getItem('loggedTerminalName')) {
          OB.UTIL.localStorage.setItem('loggedTerminalName', OB.MobileApp.model.get('terminalName'));
        }
        if ((!OB.UTIL.isNullOrUndefined(OB.MobileApp.model.get('terminalName')) || !OB.UTIL.isNullOrUndefined(OB.UTIL.localStorage.getItem('loggedTerminalName'))) && OB.MobileApp.model.get('terminalName') !== OB.UTIL.localStorage.getItem('loggedTerminalName')) {
          OB.UTIL.localStorage.clear();
        }
        OB.MobileApp.model.set('orgUserId', inResponse.userId);
        me.set('loggedOffline', false);
        var setUserModelOnlineCallback = function () {
            me.initializeCommonComponents();
            };
        if (me.get('supportsOffline')) {
          me.setUserModelOnline(setUserModelOnlineCallback);
        } else {
          setUserModelOnlineCallback();
        }
      },
      fail: function (inSender, inResponse) {
        var lastTotalRefresh, lastIncRefresh;
        if (this.alreadyProcessed) {
          return;
        }
        this.alreadyProcessed = true;
        lastTotalRefresh = OB.UTIL.localStorage.getItem('POSLastTotalRefresh');
        lastIncRefresh = OB.UTIL.localStorage.getItem('POSLastIncRefresh');
        if (!lastTotalRefresh && !lastIncRefresh) {
          //There hasn't been a complete login yet, so we can't allow to login offline.
          var msg = OB.I18N.getLabel('OBMOBC_OfflineModeNoLogin') + OB.I18N.getLabel('OBMOBC_LoadingErrorBody');
          OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_Error'), msg, [{
            label: OB.I18N.getLabel('OBMOBC_Reload'),
            action: function () {
              window.location.reload();
            }
          }], {
            onShowFunction: function (popup) {
              OB.UTIL.localStorage.removeItem('cacheAvailableForUser:' + OB.MobileApp.model.get('orgUserId'));
              popup.$.headerCloseButton.hide();
            },
            autoDismiss: false
          });
        } else {
          me.attemptToLoginOffline();
        }
      }
    });
    rr = new OB.RR.Request({
      ajaxRequest: ajaxRequest
    });
    rr.exec(this.get('loginHandlerUrl'));
  },

  setUserModelOnline: function (callback) {
    OB.debug("next process:", callback);
    // argument checks
    OB.UTIL.Debug.execute(function () {
      if (!callback) {
        throw "This method should not be executed asynchronously. Please provide a callback";
      }
    });
    var me = this;
    OB.Dal.initCache(OB.Model.Message, [], function () {
      OB.Dal.initCache(OB.Model.LogClient, [], function () {
        OB.Dal.initCache(OB.Model.Session, [], function () {
          OB.Dal.initCache(OB.Model.User, [], function () {
            OB.Dal.find(OB.Model.User, {
              'id': OB.MobileApp.model.get('orgUserId')
            }, function (users) {
              var user, session, date, savedPass;
              if (users.models.length === 0) {
                date = new Date().toString();
                user = new OB.Model.User();
                user.set('name', me.user);
                user.set('id', OB.MobileApp.model.get('orgUserId'));
                savedPass = me.generate_sha1(me.password + date);
                user.set('password', savedPass);
                user.set('created', date);
                user.set('formatInfo', JSON.stringify(OB.Format));
                me.usermodel = user;
                OB.Dal.save(user, function () {
                  me.updateSession(user, function () {
                    if (callback) {
                      callback();
                    }
                  });
                }, function () {
                  OB.MobileApp.model.loadingErrorsActions('setUserModelOnline I');
                  // TODO: an error is shown when the transaction fail because a constraints error that shouldn't appear and must be analyzed
                  // error shown: Table 'ad_user' save error: (6) could not execute statement due to a constaint failure (19 constraint failed)
                  // QUICK FIX: trying again fixes the problem
                  // me.setUserModelOnline(callback);
                  return;
                }, true);
              } else {
                user = users.models[0];
                me.usermodel = user;
                if (me.password) {
                  //The password will only be recomputed in case it was properly entered
                  //(that is, if the call comes from the login page directly)
                  savedPass = me.generate_sha1(me.password + user.get('created'));
                  user.set('password', savedPass);
                }
                user.set('formatInfo', JSON.stringify(OB.Format));
                OB.Dal.save(user, function () {
                  me.updateSession(user, function () {
                    if (callback) {
                      callback();
                    }
                  });
                }, function () {
                  OB.MobileApp.model.loadingErrorsActions('setUserModelOnline II');
                  return;
                });
              }
            }, function () {
              OB.error("setUserModelOnline", arguments);
            });
          }, null);
        }, null);
      }, null);
    }, null);
  },

  /**
   * Set of app specific actions executed before loging out.
   *
   *  Override this method if your app needs to do something special
   */
  preLogoutActions: function (callback) {
    callback();
  },

  logout: function () {
    var me = this;

    function callback() {
      OB.UTIL.showLoggingOut(true);
      OB.Dal.removeRemoteModels();

      OB.UTIL.localStorage.removeItem('authenticationClient');
      OB.UTIL.localStorage.removeItem('authenticationToken');

      var rr, ajaxRequest = new enyo.Ajax({
        url: '../../org.openbravo.mobile.core.logout',
        cacheBust: false,
        method: 'GET',
        handleAs: 'json',
        timeout: 20000,
        contentType: 'application/json;charset=utf-8',
        success: function (inSender, inResponse) {
          me.closeSession();
        },
        fail: function (inSender, inResponse) {
          me.closeSession();
        }
      });
      rr = new OB.RR.Request({
        ajaxRequest: ajaxRequest
      });
      rr.exec(ajaxRequest.url);
    }
    this.preLogoutActions(callback);
  },

  lock: function () {
    var me = this;

    if (OB && OB.POS && OB.POS.hwserver !== undefined) {
      OB.POS.hwserver.print(new OB.DS.HWResource(OB.OBPOSPointOfSale.Print.GoodByeTemplate), {});
    }


    OB.UTIL.localStorage.removeItem('authenticationClient');
    OB.UTIL.localStorage.removeItem('authenticationToken');

    var rr, ajaxRequest = new enyo.Ajax({
      url: '../../org.openbravo.mobile.core.logout',
      cacheBust: false,
      method: 'GET',
      handleAs: 'json',
      timeout: 20000,
      contentType: 'application/json;charset=utf-8',
      success: function (inSender, inResponse) {
        me.triggerLogout();
      },
      fail: function (inSender, inResponse) {
        me.triggerLogout();
      }
    });
    rr = new OB.RR.Request({
      ajaxRequest: ajaxRequest
    });
    rr.exec(ajaxRequest.url);
  },

  triggerLogout: function () {
    // deactivate the loginfail trigger
    this.off('loginfail');

    // trigger for custom applications
    this.trigger('logout');

    window.location.reload();
  },

  triggerLoginSuccess: function () {
    this.trigger('loginsuccess');
  },

  triggerOnLine: function () {
    if (!OB.MobileApp.model.isLoggingIn) {
      this.set('connectedToERP', true);
    }
  },

  triggerOffLine: function () {
    if (!OB.MobileApp.model.isLoggingIn) {
      this.set('connectedToERP', false);
    }
  },

  triggerLoginFail: function (e, mode, data) {
    OB.UTIL.showLoading(false);
    if (mode === 'userImgPress') {
      this.trigger('loginUserImgPressfail', e);
    } else {
      this.trigger('loginfail', e, data);
    }
    this.set('isLoggingIn', false);
  },

  hasPermission: function (p, checkForAutomaticRoles, smartDbPrefix) {
    var permission;
    var dbPrefix;
    var separator = '_';
    var resolvedPreference;
    permission = p.approval ? p.approval : p;

    //This code will get the preference based on the app
    if (smartDbPrefix === true) {
      dbPrefix = OB.MobileApp.model.get('appModulePrefix');
      if (dbPrefix) {
        resolvedPreference = dbPrefix + separator + permission;
        if (this.get('permissions') && this.get('permissions')[resolvedPreference] !== null && this.get('permissions')[resolvedPreference] !== undefined) {
          permission = resolvedPreference;
        } else {
          resolvedPreference = dbPrefix + permission;
          if (this.get('permissions') && this.get('permissions')[resolvedPreference] !== null && this.get('permissions')[resolvedPreference] !== undefined) {
            permission = resolvedPreference;
          } else {
            return false;
          }
        }
      } else {
        return false;
      }
    }

    return (!checkForAutomaticRoles && !(this.get('context') && this.get('context').role.manual)) || (this.get('permissions') && this.get('permissions')[permission]);
  },

  supportLogClient: function () {
    var supported = true;
    if ((OB.MobileApp && OB.MobileApp.model && OB.MobileApp.model.get('supportsOffline')) === false) {
      supported = false;
    }
    return supported;
  },

  saveTerminalInfo: function (callback) {
    OB.info('saving terminal info:', this.attributes);
    //In online mode, we save the terminal information in the local db
    this.usermodel.set('terminalinfo', JSON.stringify(this));
    OB.Dal.save(this.usermodel, function () {
      if (callback) {
        callback();
      }
    }, function () {
      OB.error("saveTerminalInfo", arguments);
    });
  }

});