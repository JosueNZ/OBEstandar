/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, Backbone */

OB.Model = window.OB.Model || {};

(function () {

  // navigation logic
  OB.Model.RouterHelper = Backbone.Router.extend({

    terminal: null,

    initialize: function (associatedTerminal) {
      if (!associatedTerminal) {
        OB.UTIL.Debug.execute(function () {
          throw "The router needs to be associated to a terminal";
        });
        return;
      }
      this.terminal = associatedTerminal;
    },

    // routes
    routes: {
      // list of possible navigation routes
      // also new routes are added on the fly when registeredWindows are loaded (check: loginRegisteredWindows)
      '': 'root',
      'login': 'login',
      '*route': 'any'
    },
    any: function (route) {
      // this can happen if the webpage is reloaded and the user was in a registeredWindow that no longer exists
      if (route) {
        OB.MobileApp.model.set('nextWindow', route);
      }
      this.terminal.navigate('');
    },
    root: function () {
      if (!OB.MobileApp.view) {
        OB.UTIL.Debug.execute(function () {
          throw "OB.MobileApp.view must be defined before navigating";
        });
        return;
      }
      this.terminal.initializeCommonComponents();
    },
    login: function () {
      this.terminal.renderLogin();
    },
    renderGenericWindow: function (windowName, params) {
      // the registered windows are rendered within this function
      this.terminal.renderGenericWindow(windowName, params);
    }
  });

}());