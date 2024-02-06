/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, _, OB, window, document, navigator, setTimeout, clearTimeout */

// Container for the whole application
enyo.kind({
  name: 'OB.UI.Terminal',

  events: {
    onDestroyMenu: ''
  },
  published: {
    scanning: false
  },

  classes: 'pos-container',
  style: 'height:700px',
  components: [{
    classes: 'pos-container-top-separator'
  }, {
    components: [{
      name: 'containerLoading',
      components: [{
        classes: 'POSLoadingCenteredBox',
        components: [{
          name: 'containerLoading_label',
          classes: 'POSLoadingPromptLabel'
        }, {
          classes: 'POSLoadingProgressBarImg',
          components: [{
            tag: 'span',
            style: 'width: 0%',
            name: 'containerLoading_fillingBar',
            classes: 'POSLoadingBarFilling'
          }]
        }, {
          classes: 'POSLoadingProgressBar',
          components: [{
            name: 'containerLoading_loadedModels',
            showing: false,
            classes: 'POSLoadingBarMessage'
          }]
        }]
      }]
    }, {
      name: 'containerLoggingOut',
      showing: false,
      components: [{
        classes: 'POSLoadingCenteredBox',
        components: [{
          name: 'containerLoggingOut_label',
          classes: 'POSLoadingPromptLabel'
        }, {
          classes: 'POSLoadingProgressBarImg',
          components: [{
            tag: 'span',
            name: 'containerLoggingOut_fillingBar',
            classes: 'POSLoadingBarFilling'
          }]
        }]
      }]
    }, {
      name: 'containerWindow',
      handlers: {
        onShowPopup: 'showPopupHandler',
        onHidePopup: 'hidePopupHandler'
      },
      getRoot: function () {
        return this.getComponents()[0];
      },
      showPopupHandler: function (inSender, inEvent) {
        if (inEvent.popup) {
          this.showPopup(inEvent.popup, inEvent.args);
        }
      },
      showPopup: function (popupName, args) {
        var componentsArray, i;
        if (OB.MobileApp.view.$[popupName]) {
          OB.MobileApp.view.$[popupName].show(args);
          if (OB.MobileApp.view.$.containerLoading.showing) {
            OB.MobileApp.view.$[popupName].setStyle('display:none');
          }
          return true;
        }
        componentsArray = this.getComponents();
        for (i = 0; i < componentsArray.length; i++) {
          if (componentsArray[i].$[popupName]) {
            componentsArray[i].$[popupName].show(args);
            if (OB.MobileApp.view.$.containerLoading.showing) {
              componentsArray[i].$[popupName].setStyle('display:none');
            }
            break;
          }
        }
        return true;
      },
      hidePopupHandler: function (inSender, inEvent) {
        if (inEvent.popup) {
          this.hidePopup(inEvent.popup, inEvent.args);
        }
      },
      hidePopup: function (popupName, args) {
        var componentsArray, i;
        if (OB.MobileApp.view.$[popupName]) {
          OB.MobileApp.view.$[popupName].hide(args);
          return true;
        }
        componentsArray = this.getComponents();
        for (i = 0; i < componentsArray.length; i++) {
          if (componentsArray[i].$[popupName]) {
            componentsArray[i].$[popupName].hide(args);
            break;
          }
        }
        return true;
      }
    }]
  }, {
    name: 'dialogsContainer'
  }, {
    name: 'processingContainer',
    kind: 'OB.UI.Popup',
    classes: 'popup-processing',
    autoDismiss: false,
    closeOnEscKey: false,
    components: [{
      classes: 'processing-container',
      components: [{
        classes: 'processing-spinner',
        allowHtml: true,
        content: '<div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div><div></div>'
      }, {
        classes: 'processing-label',
        name: 'processingLabel',
        content: ''
      }]
    }]
  }, {
    name: 'alertContainer',
    components: [{
      kind: 'OB.UTIL.showAlert',
      name: 'alertQueue'
    }]
  }, {
    name: 'confirmationContainer',
    getCurrentPopup: function () {
      var comp = this.getComponents();
      if (_.isArray(comp) && comp.length > 0) {
        if (comp.length > 1) {
          OB.warn('More than 1 component in confirmation container. Returning the first one');
        }
        return comp[0];
      } else {
        return null;
      }
    }
  }, {
    kind: 'OB.UI.Profile.SessionInfo',
    name: 'profileSessionInfo'
  }, {
    kind: 'OB.UI.Profile.UserInfo',
    name: 'profileUserInfo'
  }, {
    // To globaly handle keypup events when not using focusKeeper
    kind: enyo.Signals,
    onkeyup: "keypressHandler",
    onkeydown: "keyDownHandler"
  }, {
    kind: 'OB.Modals.ModalModelsToSynch',
    name: 'modalModelsToSynch'
  }, {
    kind: 'OB.Modals.ModalItemsToSynch',
    name: 'modalItemsToSynch'
  }],
  keyReceivers: [],

  /**
   * Sets the focus in the _focuskeeper
   */
  setFocusOnFocusKeeper: function () {
    var keeper = document.getElementById('_focusKeeper');
    keeper.focus();
  },

  /**
   * When using focus keeper for barcode scanners, keep always the focus there
   */
  keeperBlur: function (event) {
    function setFocusIfNecessary(target) {
      var tagName;
      if (OB.UTIL.isIOS()) {
        if (target) {
          tagName = target.tagName;
        } else {
          tagName = '';
        }
      } else {
        tagName = document.activeElement.tagName;
      }

      if (OB.MobileApp.view.scanMode && (tagName !== 'INPUT' && tagName !== 'SELECT' && tagName !== 'TEXTAREA')) {
        OB.debug('keeperBlur');
        OB.MobileApp.view.setFocusOnFocusKeeper();
      } else {
        OB.debug('skip keeperBlur');
      }
    }
    if (OB.UTIL.isIOS()) {
      setFocusIfNecessary(event.relatedTarget);
    } else {
      setTimeout(setFocusIfNecessary, 10);
    }
  },

  /**
   * Key up on focus keeper. We are using key up event because keydown doesn't work
   * properly on Android Chrome 26
   */
  keeperKey: function (k) {
    OB.debug('keeperKey - keyCode: ' + k.keyCode + " - keyIdentifier: " + k.keyIdentifier + " - charCode: " + k.charCode + " - which: " + k.which);
    _.each(this.keyReceivers, function (receiver) {
      receiver.waterfall('onGlobalKeypress', {
        keyboardEvent: k
      });
    });
  },

  iosKeyEvent: function (k) {
    OB.debug('key event for iOS - keyCode: ' + k.keyCode + " - keyIdentifier: " + k.keyIdentifier + " - charCode: " + k.charCode + " - which: " + k.which);
    var me = this;
    var timeoutTime = k.which === 13 ? 200 : 1;
    setTimeout(function () {
      _.each(me.keyReceivers, function (receiver) {
        receiver.waterfall('onGlobalKeypress', {
          keyboardEvent: k,
          enterEvent: true
        });
      });
    }, timeoutTime);
  },

  setOriginalScanMode: function (originalScanMode) {
    if (OB.UTIL.isNullOrUndefined(this.originalScanMode)) {
      this.originalScanMode = originalScanMode;
    }
  },

  restoreOriginalScanMode: function () {
    if (OB.UTIL.isNullOrUndefined(OB.MobileApp.view.activePopups) || OB.MobileApp.view.activePopups.length === 0) {
      OB.MobileApp.view.scanningFocus(this.originalScanMode);
      delete this.originalScanMode;
    }
  },

  /**
   * Checks whether focus should be places on focusKeeper and puts it there if needed
   */
  scanningFocus: function (scanMode) {
    var me = this;
    OB.UTIL.HookManager.executeHooks('OBMOBC_PreScanningFocus', {
      context: this,
      scanMode: scanMode
    }, function (args) {
      if (!args.cancelOperation) {
        var keeper;
        if (!OB.MobileApp.model.get('useBarcode')) {
          return;
        }
        OB.debug('scanningfocus');
        if (scanMode !== undefined) {
          me.scanMode = scanMode;
        }

        // This shouldn't be done in iOS, if done the focuskeeper loses the focus
        if (me.scanMode && navigator.userAgent.match(/iPhone|iPad|iPod/i)) {
          OB.debug('scanningfocus. iOS. Focused');
          keeper = document.getElementById('_focusKeeper');
          keeper.disabled = false;
          me.setFocusOnFocusKeeper();
        }

        if (!me.scanMode) {
          return;
        }

        // using timeout due problems in android stock browser because of 2 reasons:
        //   1. need to set the focus back to keeper even if not in scanMode, if not,
        //      when in a non scanMode tab using scanner writes in the URL tab. So
        //      it is not keep in keeper only in case the destination element is a input
        //      to know the destination timeout is needed
        //   2. if timeout is not used, on-screen keyboard is shown when settin focus in
        //      keeper
        setTimeout(function () {
          if (me.scanMode && (document.activeElement.tagName !== 'INPUT' && document.activeElement.tagName !== 'SELECT' && document.activeElement.tagName !== 'TEXTAREA')) {
            me.setFocusOnFocusKeeper();
          }
        }, 500);
      }
    });
  },

  /**
   * Global key up event. Used when not using focusKeeper
   */
  keypressHandler: function (inSender, inEvent) {
    var keyCode = inEvent.keyCode;
    if (keyCode === 17) {
      OB.MobileApp.model.ctrlPressed = false;
    }
    if (keyCode === 16) {
      OB.MobileApp.model.shiftPressed = false;
    }
    OB.debug('keypressHandler - keyCode: ' + inEvent.keyCode + " - keyIdentifier: " + inEvent.keyIdentifier + " - charCode: " + inEvent.charCode + " - which: " + inEvent.which);
    if (OB.MobileApp.model.get('useBarcode') && this.scanMode && !(OB.UTIL.isIOS() && inEvent.key === 'Backspace')) {
      //We ignore all keypress events except in the special case of iOS and the backspace key.
      return;
    }
    if (inEvent.originator && (inEvent.originator.hasClass('modal-dialog') || inEvent.originator.hasClass('modal-popup'))) {
      return;
    }
    //Issue 25013. This flag is set by globalKeypressHandler function in ob-keyboard.js
    if (OB.MobileApp.model.get('useBarcode') && OB.MobileApp.keyPressProcessed) {
      delete OB.MobileApp.keyPressProcessed;
    }
    _.each(this.keyReceivers, function (receiver) {
      receiver.waterfall('onGlobalKeypress', {
        keyboardEvent: inEvent
      });
    });
  },

  /**
   * Global key down event. Used when not using focusKeeper
   */
  keyDownHandler: function (inSender, inEvent) {
    var keyCode = inEvent.keyCode;
    OB.MobileApp.model.ctrlPressed = keyCode === 17;
    OB.MobileApp.model.shiftPressed = keyCode === 16;
  },

  resizeWindow: function () {
    var winHeight = window.innerHeight,
        winWidth = window.innerWidth,
        percentage, appHeight = 700;
    OB.resizing = true;

    // resize in case of window rotation but not if virtual keyboard appears
    // hack: virtual keyboard is detected because only y is resized but not x
    if (!this.sizeProperties || (this.sizeProperties.x !== winWidth && this.sizeProperties.y !== winHeight)) {
      //When the window is resized we need to close the menu
      //to avoid a strange problem when rotating mobile devices
      //See issue https://issues.openbravo.com/view.php?id=23669
      this.waterfall('onDestroyMenu');
      this.sizeProperties = {
        x: window.innerWidth,
        y: window.innerHeight
      };

      percentage = window.innerHeight * 100 / appHeight;
      percentage = Math.floor(percentage) / 100;
      document.documentElement.style.zoom = percentage;

      // after zooming, force render again
      // if this is not done, iPad's Safari messes the layout when going from 1 to 2 columns
      this.render();
      OB.resizing = false;
    }
  },

  rendered: function () {
    var keeper;
    this.inherited(arguments);
    //Do not allow showing context menu (mouse right click)
    window.oncontextmenu = function () {
      if (!OB.UTIL.Debug.isDebug()) {
        return false;
      }
    };

    // Creating focus keeper. This needs to be done in this way instead of using enyo components
    // because currently, it is not possible to place focus (in iOS)
    keeper = document.getElementById('_focusKeeper');
    if (!keeper) {
      keeper = document.createElement('input');
      keeper.setAttribute('id', '_focusKeeper');
      keeper.setAttribute('style', 'position: fixed; top: -100px; left: -100px;');
      if (OB.MobileApp.model.get('useEmbededBarcode')) {
        keeper.setAttribute('readonly', 'true');
        keeper.onfocus = function (event) {
          if (OB.MobileApp.model.get('useEmbededBarcode')) {
            setTimeout(function () {
              keeper.readOnly = false;
            }, 1);
          }
        };
      }
      keeper.onblur = function (event) {
        if (OB.MobileApp.model.get('useEmbededBarcode')) {
          keeper.readOnly = true;
        } else {
          if (keeper.readOnly) {
            keeper.readOnly = false;
          }
        }
        OB.MobileApp.view.keeperBlur(event);
      };
      if (OB.UTIL.isIOS()) {
        keeper.setAttribute('autocapitalize', 'off');
        keeper.setAttribute('autocorrect', 'off');
      }
      // Avoid the use of Alt + Left Arrow, Alt + Right Arrow, Shift + Backspace: Navigate Back/Next in the browser
      // Fixed 25288: key up is not working fine in IOS7. Because of that we are
      // listening to keypress and then sending it to the standard flow.
      if (OB.UTIL.isIOS()) {
        keeper.addEventListener('keypress', function (k) {
          OB.MobileApp.view.iosKeyEvent(k);

          if ((k.keyCode === 37 && k.altKey) || (k.keyCode === 39 && k.altKey) || (k.keyCode === 8 && ((k.srcElement.tagName !== 'INPUT' && k.srcElement.tagName !== 'TEXTAREA') || k.srcElement.id === '_focusKeeper')) || (k.keyCode === 8 && k.shiftKey)) {
            return false;
          }
        });
      } else {
        document.body.onkeydown = function (k) {
          if ((k.keyCode === 37 && k.altKey) || (k.keyCode === 39 && k.altKey) || (k.keyCode === 8 && ((k.srcElement.tagName !== 'INPUT' && k.srcElement.tagName !== 'TEXTAREA') || k.srcElement.id === '_focusKeeper')) || (k.keyCode === 8 && k.shiftKey)) {
            return false;
          }
        };
        keeper.addEventListener('keyup', function (k) {
          OB.debug('keyup happened: which:' + k.which);
          OB.MobileApp.view.keeperKey(k);
        });
      }
      document.body.appendChild(keeper);
    }
    this.scanningFocus();
  },

  initComponents: function () {
    window.OB.MobileApp = OB.MobileApp || {};
    window.OB.MobileApp.view = this;
    window.OB.MobileApp.view.currentWindow = 'initializing';
    window.OB.MobileApp.model.set('currentWindow', 'initializing');
    window.OB.MobileApp.view.currentWindowState = 'unknown';
    window.OB.MobileApp.model.set('currentWindowState', 'unknown');

    var args = arguments,
        initialized = false;

    //this.inherited(args);
    this.terminal.on('initializedCommonComponents', function () {
      var me = this;
      if (!initialized) {
        this.inherited(args);
        initialized = true;

        this.terminal.on('change:context', function () {
          var ctx = this.terminal.get('context');
          if (!ctx) {
            return;
          }
          if (this.$.dialogsContainer.$.profileDialog) {
            this.$.dialogsContainer.$.profileDialog.destroy();
          }
          if (OB.UI.ModalProfile) {
            this.$.dialogsContainer.createComponent({
              kind: 'OB.UI.ModalProfile',
              name: 'profileDialog'
            });
          }
        }, this);

        this.terminal.on('change:connectedToERP', function (model) {
          if (model.get('loggedOffline') !== undefined) {
            if (model.get('connectedToERP')) {
              //We will do an additional request to verify whether the connection is still active or not
              var rr, ajaxRequest2 = new enyo.Ajax({
                url: '../../org.openbravo.mobile.core.context',
                cacheBust: false,
                method: 'GET',
                handleAs: 'json',
                timeout: 20000,
                data: {
                  ignoreForConnectionStatus: true
                },
                contentType: 'application/json;charset=utf-8',
                success: function (inSender, inResponse) {
                  if (inResponse && !inResponse.exception) {
                    OB.info('Connection came back. Session still valid, so no action required');
                    OB.MobileApp.model.returnToOnline();
                  } else {
                    if (model.get('supportsOffline')) {
                      model.set('sessionLost', true);
                      OB.UTIL.stopLogClient();
                      if (!model.get('reloginMessageShown')) {
                        OB.warn('Connection came back. Session no longer valid, so relogin required');
                        model.set('reloginMessageShown', true);
                      }
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
                    } else {

                      if (inResponse && 'invalidRequestOrigin' === inResponse.exception) {
                        OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_InvalidOriginTitle'), OB.I18N.getLabel('OBMOBC_InvalidOriginMsg'), [{
                          isConfirmButton: true,
                          label: OB.I18N.getLabel('OBMOBC_LblOk'),
                          action: function () {
                            OB.MobileApp.model.triggerLogout();
                            return true;
                          }
                        }]);
                        return;
                      }

                      OB.MobileApp.model.triggerLogout();
                    }
                  }
                },
                fail: function (inSender, inResponse) {}
              });
              rr = new OB.RR.Request({
                ajaxRequest: ajaxRequest2
              });
              rr.exec(ajaxRequest2.url);
            } else {
              if (model.get('supportsOffline')) {
                OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_OfflineModeWarning'));
              }
            }
          }
        }, this);

        // TODO: POS specific, move it to somewhere else
        this.terminal.on('change:terminal change:bplocation change:location change:pricelist change:pricelistversion', function () {
          var name = '';
          var clientname = '';
          var orgname = '';
          var pricelistname = '';
          var currencyname = '';
          var locationname = '';

          if (this.terminal.get('terminal')) {
            name = this.terminal.get('terminal')._identifier;
            clientname = this.terminal.get('terminal')['client' + OB.Constants.FIELDSEPARATOR + '_identifier'];
            orgname = this.terminal.get('terminal')['organization' + OB.Constants.FIELDSEPARATOR + '_identifier'];
          }
          if (this.terminal.get('pricelist')) {
            pricelistname = this.terminal.get('pricelist')._identifier;
            currencyname = this.terminal.get('pricelist')['currency' + OB.Constants.FIELDSEPARATOR + '_identifier'];
          }
          if (this.terminal.get('location')) {
            locationname = this.terminal.get('location')._identifier;
          }
        }, this);
      }
      this.$.dialogsContainer.destroyComponents();
      if (OB.UI.ModalLogout) {
        this.$.dialogsContainer.createComponent({
          kind: 'OB.UI.ModalLogout',
          name: 'logoutDialog'
        });
      }

      this.render();
      this.resizeWindow();
      window.OB.MobileApp.view.currentWindowState = 'renderUI'; // for the 'initializing' window
      window.OB.MobileApp.model.set('currentWindowState', 'renderUI');
      window.onresize = function () {
        me.resizeWindow();
      };

    }, this);

  }
});