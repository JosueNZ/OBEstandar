/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, _ */

enyo.kind({
  name: 'OB.UI.WindowView',
  windowmodel: null,
  create: function () {
    var me = this;
    this.inherited(arguments);
    this.model = new this.windowmodel();
    if (this.model.initModels) {
      var initUI = function (callback) {
          if (me.init) {
            me.init();
          }
          callback();
          };
      var renderUI = function (callback) {
          OB.UTIL.HookManager.executeHooks('ModelReady:' + me.name.replace(new RegExp('\\d+', 'g'), ''), null, function () {
            OB.MobileApp.model.trigger('window:ready', this);
            var loginTimer = new Date().getTime() - OB.UTIL.localStorage.getItem('LOGINTIMER');
            OB.info("Total time since logged in: " + (loginTimer / 1000) + " seconds");
            if (OB.UTIL.localStorage.getItem('benchmarkScore')) {
              OB.info('Performance test result: ' + OB.UTIL.localStorage.getItem('benchmarkScore'));
            }
            callback();
          });
          };
      this.model.initModels_initUI_loadModels_renderUI(initUI, renderUI);
    } else {
      OB.UTIL.VersionManagement.deprecated(30610, function () {
        this.model.on('windowReady', function () {
          if (this.init) {
            this.init();
          }
          OB.UTIL.HookManager.executeHooks('ModelReady:' + me.name, null, function () {
            OB.MobileApp.model.trigger('window:ready', this);
            var loginTimer = new Date().getTime() - OB.UTIL.localStorage.getItem('LOGINTIMER');
            OB.info("Total time since logged in: " + (loginTimer / 1000) + " seconds");
          });
        }, this);
        this.model.load();
      }, null, this);
    }

  },
  statics: {
    initChildren: function (view, model) {
      if (!view || !view.getComponents) {
        return;
      }
      enyo.forEach(view.getComponents(), function (child) {
        OB.UI.WindowView.initChildren(child, model);
        if (child.init) {
          child.init(model);
        }
      });
    },
    registerPopup: function (windowClass, dialogToAdd) {
      var kind;
      kind = enyo.getObject(windowClass);
      if (!_.isEmpty(kind)) {
        kind.prototype.popups.push({
          dialog: dialogToAdd,
          windowClass: windowClass
        });
      } else {
        OB.UTIL.showWarning("An error occurs adding the pop up " + dialogToAdd.kind + ". The window class " + windowClass + " cannot be found.");
      }
    },
    destroyModels: function (view) {
      var p;
      if (!view) {
        return;
      }
      for (p in view) {
        if (view.hasOwnProperty(p) && view[p] && view[p].off) {
          view[p].off();
          delete view[p];
        }
      }
      if (!view.getComponents) {
        return;
      }

      enyo.forEach(view.getComponents(), function (child) {
        OB.UI.WindowView.destroyModels(child);
      });
    }
  },
  popups: [],
  init: function () {
    //Modularity
    //Add new dialogs
    enyo.forEach(this.popups, function (dialog) {
      if (dialog.windowClass === this.kindName) {
        if (!this.$[dialog.dialog.name]) {
          this.createComponent(dialog.dialog);
        }
      }
    }, this);

    // Calling init in sub components
    OB.UI.WindowView.initChildren(this, this.model);
  },

  destroy: function () {
    if (this.model) {
      this.model.setOff();
    }
    this.model = null;
    OB.UI.WindowView.destroyModels(this);

    try {
      this.inherited(arguments);
    } catch (e) {
      OB.error('error destroying components', e);
    }
  }
});

OB.Customizations = {};
OB.Customizations.pointOfSale = {};
OB.Customizations.pointOfSale.dialogs = {
  push: function (kind) {
    //developers help
    OB.warn('WARNING! OB.Customizations.pointOfSale.dialogs has been deprecated. Use OB.UI.WindowView.registerPopup() instead.');
    OB.UI.WindowView.registerPopup('OB.OBPOSPointOfSale.UI.PointOfSale', kind);
  }
};
OB.Customizations.pointOfSale.dialogs = OB.Customizations.pointOfSale.dialogs || [];