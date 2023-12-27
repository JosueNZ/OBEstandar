/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, OB, navigator, setTimeout, _ */

enyo.kind({
  name: 'OB.OBPOSLogin.UI.UserButton',
  classes: 'login-user-button',
  user: null,
  userImage: null,
  userId: null,
  showConnectionStatus: true,
  events: {
    onUserImgClick: ''
  },
  components: [{
    classes: 'login-user-button-bottom',
    components: [{
      name: 'bottomText',
      classes: 'login-user-button-bottom-text'
    }]
  }],
  tap: function () {
    this.doUserImgClick();
  },
  create: function () {
    var me = this;
    this.inherited(arguments);

    if (this.userImage && this.userImage !== 'none') {
      this.applyStyle('background-image', 'url(' + this.userImage + ')');
    }

    if (this.showConnectionStatus) {
      OB.Dal.initCache(OB.Model.Session, [], null, null);
    }

    if (this.user) {
      this.$.bottomText.setContent(this.user);
    }
  }
});

enyo.kind({
  name: 'OB.OBPOSLogin.UI.LoginButton',
  kind: 'OB.UI.ModalDialogButton',
  style: 'min-width: 115px;',
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
    this.content = OB.I18N.getLabel('OBMOBC_LoginButton');
    this.setDisabled(false);
  }
});

enyo.kind({
  kind: 'enyo.Ajax',
  name: 'OB.OBPOSLogin.UI.LoginRequest',
  url: '../../org.openbravo.retail.posterminal.service.loginutils',
  method: 'GET',
  handleAs: 'json',
  contentType: 'application/json;charset=utf-8'
});

enyo.kind({
  name: 'OB.OBPOSLogin.UI.Login',
  tag: 'section',
  events: {
    onShowPopup: ''
  },
  components: [{
    kind: 'OB.UI.ModalInfo',
    name: 'DatabaseDialog',
    header: 'DB version change',
    // TODO: OB.I18N.getLabel('OBPOS_DatabaseVersionChange'),
    bodyContent: {
      content: 'DB version change'
      // TODO:OB.I18N.getLabel('OBPOS_DatabaseVersionChangeLong')
    }
  }, {
    classes: 'login-header-row',
    components: [{
      name: 'loginHeaderCompany',
      classes: 'login-header-company'
    }, {
      classes: 'login-header-caption',
      name: 'appCaption'
    }, {
      classes: 'login-header-ob',
      name: 'appName'
    }]
  }, {
    style: 'height: 600px',
    components: [{
      classes: 'span6',
      components: [{
        kind: 'Scroller',
        thumb: true,
        horizontal: 'hidden',
        name: 'loginUserContainer',
        classes: 'login-user-container notouchactions',
        content: ['.']
      }]
    }, {
      classes: 'span6',
      components: [{
        classes: 'login-inputs-container',
        name: 'loginInputsContainer',
        components: [{
          name: 'loginInputs',
          classes: 'login-inputs-browser-compatible',
          components: [{
            components: [{
              classes: 'login-status-info',
              style: 'float: left;',
              name: 'connectStatus'
            }, {
              classes: 'login-status-info',
              name: 'screenLockedLbl'
            }]
          }, {
            components: [{
              kind: 'enyo.Input',
              type: 'text',
              name: 'username',
              classes: 'input-login',
              onkeydown: 'inputKeydownHandler'
            }]
          }, {
            components: [{
              kind: 'enyo.Input',
              type: 'password',
              name: 'password',
              classes: 'input-login',
              onkeydown: 'inputKeydownHandler'
            }]
          }, {
            name: 'loginWarning',
            classes: 'login-warning'
          }, {
            classes: 'login-inputs-loginbutton',
            components: [{
              kind: 'OB.OBPOSLogin.UI.LoginButton',
              onclick: 'loginButtonAction'
            }]
          }]
        }, {
          name: 'loginBrowserNotSupported',
          style: 'display: none;',
          components: [{
            components: [{
              name: 'LoginBrowserNotSupported_Title_Lbl',
              classes: 'login-browsernotsupported-title'
            }]
          }, {
            style: 'padding-bottom: 20px;',
            components: [{
              components: [{
                name: 'LoginBrowserNotSupported_P1_Lbl',
                classes: 'login-browsernotsupported-content'
              }, {
                name: 'LoginBrowserNotSupported_P2_Lbl',
                classes: 'login-browsernotsupported-content'
              }, {
                name: 'supportedBrowsers'
              }]
            }]
          }]
        }]
      }]
    }]
  }],

  setConnectedStatus: function () {
    var me = this;
    if (!this.$.connectStatus) {
      // don't try to set it, when it is not already built
      return;
    }
    OB.UTIL.checkConnectivityStatus(function () {
      if (me && me.$ && me.$.connectStatus) {
        me.$.connectStatus.setContent(OB.I18N.getLabel('OBMOBC_Online'));
      }
    }, function () {
      if (me && me.$ && me.$.connectStatus) {
        me.$.connectStatus.setContent(OB.I18N.getLabel('OBMOBC_Offline'));
      }
    });
  },
  hasTerminalChanged: function () {
    if (OB.UTIL.localStorage.getItem('loggedTerminalName') && (OB.UTIL.localStorage.getItem('loggedTerminalName') !== OB.MobileApp.model.get('terminalName'))) {
      OB.info('Terminal has been changed. Resetting database and local storage information.');
      OB.UTIL.localStorage.clear();
      OB.UTIL.localStorage.setItem('terminalName', OB.MobileApp.model.get('terminalName'));
      OB.MobileApp.model.logout();
    }
  },

  initComponents: function () {
    var notSupportedBrowserMsg;
    this.inherited(arguments);

    OB.MobileApp.view.currentWindow = 'login';
    OB.MobileApp.model.set('currentWindow', 'login');
    OB.MobileApp.view.currentWindowState = 'unknown';
    OB.MobileApp.model.set('currentWindowState', 'unknown');

    this.$.screenLockedLbl.setContent(OB.I18N.getLabel('OBMOBC_LoginScreenLocked'));
    this.$.username.attributes.placeholder = OB.I18N.getLabel('OBMOBC_LoginUserInput');
    this.$.password.attributes.placeholder = OB.I18N.getLabel('OBMOBC_LoginPasswordInput');

    this.$.LoginBrowserNotSupported_Title_Lbl.setContent(OB.I18N.getLabel('OBMOBC_LoginBrowserNotSupported'));
    if (OB.MobileApp && OB.MobileApp.model && OB.MobileApp.model.get('appDisplayName')) {
      notSupportedBrowserMsg = OB.I18N.getLabel('OBMOBC_LoginBrowserNotSupported_P1', [OB.MobileApp.model.get('appDisplayName')]);
    } else {
      notSupportedBrowserMsg = OB.I18N.getLabel('OBMOBC_LoginBrowserNotSupported_P1_generic');
    }
    this.$.LoginBrowserNotSupported_P1_Lbl.setContent(notSupportedBrowserMsg);
    this.$.LoginBrowserNotSupported_P2_Lbl.setContent(OB.I18N.getLabel('OBMOBC_LoginBrowserNotSupported_P2'));

    this.$.appName.setContent(OB.MobileApp.model.get('appDisplayName'));
    this.$.appCaption.setContent(OB.appCaption || '');

    this.setConnectedStatus();
    this.hasTerminalChanged();

    OB.MobileApp.model.on('change:connectedToERP', function () {
      if (OB.MobileApp.model.get('supportsOffline')) {
        this.setConnectedStatus();
      }
    }, this);

    this.postRenderActions();

    OB.MobileApp.view.currentWindowState = 'renderUI'; // for the login window
    OB.MobileApp.model.set('currentWindowState', 'renderUI');
    var browserInfo = OB.UTIL.getSupportedBrowserInfo();

    if (browserInfo.isSupported) {
      if (!OB.UTIL.Debug.getDebugCauses() || (!OB.UTIL.Debug.getDebugCauses().isInDevelopment && !OB.UTIL.Debug.getDebugCauses().isTestEnvironment)) {
        if (OB.MobileApp.model.get('shouldExecuteBenchmark') && OB.UTIL.localStorage.getItem('doNotExecuteBenchmark') === null) {
          if (OB.UTIL.localStorage.getItem('executePerformanceTest') !== 'N') {
            this.dialog = OB.MobileApp.view.$.confirmationContainer.createComponent({
              kind: 'OB.UI.ModalBenchmark',
              name: 'modalBenchmark',
              context: this
            });
            this.dialog.show();
          }
        }
      }
    }
  },

  handlers: {
    onUserImgClick: 'handleUserImgClick'
  },

  handleUserImgClick: function (inSender, inEvent) {
    var u = inEvent.originator.user;
    this.$.username.setValue(u);
    this.$.password.setValue('');

    if (navigator.userAgent.match(/iPhone|iPad|iPod/i)) {
      var setFocusTo = this.$.password;
      setTimeout(function () {
        setFocusTo.focus();
      }, 400);
    } else {
      this.$.password.focus();
    }

    return true;
  },

  inputKeydownHandler: function (inSender, inEvent) {
    var keyCode = inEvent.keyCode;
    if (keyCode === 13) { //Handle ENTER key
      this.loginButtonAction();
      return true;
    }
    return false;
  },

  loginButtonAction: function () {
    var u = this.$.username.getValue(),
        p = this.$.password.getValue();
    if (!u || !p) {
      OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_EmptyUserPassword'));
    } else {
      OB.MobileApp.model.login(u, p);
    }
  },

  setCompanyLogo: function (inSender, inResponse) {
    if (!inResponse.logoUrl) {
      return;
    }
    if (this.$.loginHeaderCompany) {
      this.$.loginHeaderCompany.applyStyle('background-image', 'url(' + inResponse.logoUrl + ')');
    }
    return true;
  },

  setUserImages: function (inSender, inResponse) {
    var name = [],
        userId = [],
        userName = [],
        image = [],
        target = this.$.loginUserContainer,
        me = this,
        jsonImgData;
    if (!target) {
      return;
    }
    if (!inResponse.data) {
      OB.Dal.find(OB.Model.User, {}, function (users) {
        var i, user;
        for (i = 0; i < users.models.length; i++) {
          user = users.models[i];
          name.push(user.get('name'));
          userId.push(user.get('id'));
          userName.push(user.get('name'));
        }
        me.renderUserButtons(name, userId, userName, image, target);
      }, function () {
        OB.error(arguments);
      });
      return true;
    }
    jsonImgData = inResponse.data;
    enyo.forEach(jsonImgData, function (v) {
      name.push(v.name);
      userId.push(v.userId);
      userName.push(v.userName);
      image.push(v.image);
    });
    this.renderUserButtons(name, userId, userName, image, target);
  },

  renderUserButtons: function (name, userId, userName, image, target) {
    var i;
    if (name.length === 0) {
      this.$.loginInputsContainer.hide();
      this.$.loginHeaderCompany.hide();
      OB.UTIL.showConfirmation.display('', OB.I18N.getLabel('OBMOBC_NotTerminalOrNotUser', [OB.MobileApp.model.get('terminalName')]));
    }
    for (i = 0; i < name.length; i++) {
      target.createComponent({
        kind: 'OB.OBPOSLogin.UI.UserButton',
        user: userName[i],
        userId: userId[i],
        userImage: image[i]
      });
    }
    target.render();
    OB.UTIL.showLoading(false);
    return true;
  },

  postRenderActions: function () {
    var params, me = this;
    OB.UTIL.showLoggingOut(false);
    OB.MobileApp.model.on('loginfail', function (status, data) {
      var msg;
      if (data && data.messageTitle) {
        msg = data.messageTitle;
      }

      if (data && data.messageText) {
        msg += (msg ? '\n' : '') + data.messageText.replace('<br>', '\n');
      }
      msg = msg || OB.I18N.getLabel('OBMOBC_InvalidUserPassword');
      OB.UTIL.showConfirmation.display('', msg);
      if (this.$.password) {
        this.$.password.setValue('');
      }
      if (this.$.username) {
        this.$.username.focus();
      }
    }, this);
    OB.MobileApp.model.on('loginUserImgPressfail', function () {
      //If the user image press (try to login with default password) fails, then no alert is shown and the focus goes directly to the password input
      if (this.$.password) {
        this.$.password.setValue('');
        this.$.password.focus();
      }
    }, this);

    var warningMsg, browserInfo = OB.UTIL.getSupportedBrowserInfo();

    if (!browserInfo.isAllowed) { //If the browser is not allowed, show message and finish.
      this.$.loginInputs.setStyle('display: none');
      this.$.loginBrowserNotSupported.setStyle('display: block');

      var browsers = [];
      _.each(OB.UTIL.SupportedBrowsers, function (browser) {
        browsers.push({
          tag: 'li',
          content: OB.I18N.getLabel('OBMOBC_LoginBrowserNotSupported_Info', [browser.label, browser.required, browser.preferred])
        });
      });


      this.$.supportedBrowsers.createComponent({
        tag: 'ul',
        classes: 'login-browsernotsupported-list',
        components: browsers
      }).render();

      OB.UTIL.showLoading(false);
      return true;
    }

    if (!OB.UTIL.isHTTPSAvailable()) {
      warningMsg = OB.I18N.getLabel('OBMOBC_HTTPSMissing');
    } else if (browserInfo.isPreferred) {
      // If browser is a recommended version show the normal login.
      warningMsg = '';
    } else if (browserInfo.isSupported) {
      // If browser not recommended but supported, show a warning message.
      warningMsg = OB.I18N.getLabel('OBMOBC_LoginBrowserNotRecommended');
    } else {
      // If browser not supported, show an strong warning message.
      warningMsg = OB.I18N.getLabel('OBMOBC_LoginBrowserNotSupportedButAllowed');
    }

    this.$.loginWarning.setContent(warningMsg);
    if (warningMsg === '') {
      this.$.loginWarning.hide();
    } else {
      this.$.loginWarning.show();
    }


    params = OB.MobileApp.model.get('loginUtilsParams') || {};
    params.appName = OB.MobileApp.model.get('appName');

    params.command = 'companyLogo';
    var rr, ajaxRequest = new OB.OBPOSLogin.UI.LoginRequest({
      url: OB.MobileApp.model.get('loginUtilsUrl'),
      data: params,
      success: function (inSender, inResponse) {
        me.setCompanyLogo(inSender, inResponse);
      }
    });

    rr = new OB.RR.Request({
      ajaxRequest: ajaxRequest
    });
    rr.exec(ajaxRequest.url);

    params.command = 'userImages';
    var rr2, ajaxRequest2 = new OB.OBPOSLogin.UI.LoginRequest({
      url: OB.MobileApp.model.get('loginUtilsUrl'),
      data: params,
      success: function (inSender, inResponse) {
        me.setUserImages(inSender, inResponse);
      }
    });

    rr2 = new OB.RR.Request({
      ajaxRequest: ajaxRequest2
    });
    rr2.exec(ajaxRequest2.url);


    this.$.username.focus();
  }

});