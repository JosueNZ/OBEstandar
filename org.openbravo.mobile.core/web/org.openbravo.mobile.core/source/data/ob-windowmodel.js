/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone, _ */

OB.Model.WindowModel = Backbone.Model.extend({
  data: {},
  initModels_initUI_loadModels_renderUI: function (initUI, renderUI) {
    // backward compatibility
    if (!this.initModels) {
      OB.UTIL.VersionManagement.deprecated(30610, function () {
        this.load();
      }, null, this);
      return;
    }

    var me = this;

    if (!this.models) {
      this.models = [];
    }
    _.extend(this.models, Backbone.Events);

    // the main differences between this flow and the 'load' is that the 4 stages of the loading of a window, can be managed

    function loadTheWindow() {
      OB.MobileApp.view.currentWindowState = 'unknown';
      OB.MobileApp.model.set('currentWindowState', 'unknown');
      me.initModels(function () {
        OB.MobileApp.view.currentWindowState = 'initModels';
        OB.MobileApp.model.set('currentWindowState', 'initModels');
        initUI(function () {
          OB.MobileApp.view.currentWindowState = 'initUI';
          OB.MobileApp.model.set('currentWindowState', 'initUI');
          me.loadModels(function () {
            OB.MobileApp.view.currentWindowState = 'loadModels';
            OB.MobileApp.model.set('currentWindowState', 'loadModels');
            renderUI(function () {
              // the view should be visible at this point
              OB.MobileApp.view.currentWindowState = 'renderUI';
              OB.MobileApp.model.set('currentWindowState', 'renderUI');
              OB.MobileApp.model.set('isLoggingIn', false);
            });
          });
        });
      });
    }

    OB.MobileApp.model.on('allModelsLoaded', function () {
      OB.MobileApp.model.off('allModelsLoaded');
      me.models.trigger('ready');
    });

    this.models.on('ready', function () {
      loadTheWindow();
    }, this);

    if (!OB.MobileApp.model.get('loggedOffline')) {
      OB.Dal.loadModels(true, me.models, me.data); // fires the 'ready' event
    } else {
      loadTheWindow();
    }

  },

  load: function () {
    OB.MobileApp.view.currentWindowState = 'unknown';
    OB.MobileApp.model.set('currentWindowState', 'unknown');
    var me = this,
        initIfInit = function () {
        if (me.init) {
          me.init();
        }
        };
    if (!this.models) {
      this.models = [];
    }

    _.extend(this.models, Backbone.Events);

    OB.MobileApp.model.on('allModelsLoaded', function () {
      OB.MobileApp.model.off('allModelsLoaded');
      me.models.trigger('ready');
    });

    this.models.on('ready', function () {
      initIfInit();
      this.trigger('windowReady');
      OB.MobileApp.view.currentWindowState = 'renderUI';
      OB.MobileApp.model.set('currentWindowState', 'renderUI');
      OB.MobileApp.model.set('isLoggingIn', false);
    }, this);
    if (!OB.MobileApp.model.get('loggedOffline')) {
      OB.Dal.loadModels(true, me.models, me.data);
    } else {
      initIfInit();
      this.trigger('windowReady');
      OB.MobileApp.view.currentWindowState = 'renderUI';
      OB.MobileApp.model.set('currentWindowState', 'renderUI');
      OB.MobileApp.model.set('isLoggingIn', false);
    }
  },

  setAllOff: function (model) {
    var p;
    if (model.off) {
      model.off();
    }
    if (model.attributes) {
      for (p in model.attributes) {
        if (model.attributes.hasOwnProperty(p) && model.attributes[p]) {
          this.setAllOff(model);
        }
      }
    }
  },

  setOff: function () {
    if (!this.data) {
      return;
    }
    if (this.data) {
      _.forEach(this.data, function (model) {
        this.setAllOff(model);
      }, this);
    }
    this.data = null;

    if (this.models) {
      _.forEach(this.models, function (model) {
        if (model.off) {
          model.off();
        }
      }, this);
      if (this.models.off) {
        this.models.off();
      }
    }
    this.models = null;
  },

  getData: function (dsName) {
    return this.data[dsName];
  }
});