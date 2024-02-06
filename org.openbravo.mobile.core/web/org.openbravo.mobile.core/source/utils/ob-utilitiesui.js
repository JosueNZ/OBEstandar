/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, window, enyo, Backbone, console, _, setTimeout */

OB.UTIL = window.OB.UTIL || {};

enyo.kind({
  name: 'OB.UI.Thumbnail',
  published: {
    img: null,
    imgUrl: null
  },
  cssClass: 'image-wrap',
  tag: 'div',
  contentType: 'image/png',
  width: '49px',
  height: '49px',
  'default': '../org.openbravo.mobile.core/assets/img/box.png',
  components: [{
    tag: 'div',
    name: 'image',
    style: 'margin: auto; height: 100%; width: 100%; background-size: contain;'
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.addClass(this.cssClass);
    this.applyStyle('height', this.height);
    this.applyStyle('width', this.width);
    this.imgChanged();
  },
  drawImage: function () {
    var url;
    if (this.img || this.imgUrl) {
      if (this.img === 'iconBestSellers') {
        url = 'img/iconBestSellers.png';
      } else if (this.img) {
        // 'PD94bW' are the *magic* first characters in Base64 for a svg image
        url = 'data:' + (this.img.startsWith('PD94bW') ? 'image/svg+xml' : this.contentType) + ';base64,' + this.img;
      } else if (this.imgUrl) {
        url = this.imgUrl;
      }
    } else {
      url = this['default'];
    }
    this.$.image.applyStyle('background', '#ffffff url(' + url + ') center center no-repeat');
    this.$.image.applyStyle('background-size', 'contain');
  },
  imgChanged: function () {
    this.drawImage();
  },
  imgUrlChanged: function () {
    this.drawImage();
  }
});

enyo.kind({
  name: 'OB.UTIL.showAlert',
  classes: 'alert alert-fade',
  style: 'right:0; top:0; padding:2px; margin:0; cursor:pointer; border:0;',
  tap: function () {
    this.hide();
  },
  statics: {
    /**
     * Adds a new message to the bubble of messages
     * @param  {[type]} txt         the message to be shown
     * @param  {[type]} title       the title (deprecated)
     * @param  {[type]} type        alert-success, alert-warning, alert-error
     * @param  {[type]} keepVisible false: the message will be hiden after some time. true: you will execute the hide() function of the returned object when the line must dissapear
     * @param  {[type]} idMessage   a unique identifier for this message
     * @param  {[type]} hidesIdMessage destroys any message present in the bubble of messages with this id
     * @return {[type]}             the OB.UTIL.alertLine created and added to the bubble of messages
     */
    display: function (txt, title, type, keepVisible, idMessage, hidesIdMessage) {
      if (!type) {
        type = 'alert-warning';
      }
      if (!OB.MobileApp.view) {
        console.error("OB.MobileApp.view is not defined yet. Trying to show this alert:" + txt);
        return;
      }
      var componentsArray = OB.MobileApp.view.$.alertQueue.getComponents(),
          i;

      // iterate the existing messages in the bubble
      for (i = 0; i < componentsArray.length; i++) {
        var element = componentsArray[i];
        // if this message hides another idMessage, destroy that message
        if (element.idMessage === hidesIdMessage) {
          element.destroy();
          continue;
        }
        // we don't want to annoy the user... do not repeat error and warning messages in the bubble
        if (type === 'alert-warning' || type === 'alert-error') {
          if (element.txt === txt) {
            element.destroy();
          }
        }
      }

      // add a new line to the bubble of messages
      OB.MobileApp.view.$.alertQueue.show();
      var alertLine = OB.MobileApp.view.$.alertQueue.createComponent({
        kind: 'OB.UTIL.alertLine',
        title: title,
        txt: txt,
        type: type,
        keepVisible: keepVisible,
        idMessage: idMessage
      }).render();

      OB.UTIL.showAlert.showOrHideMessagesBubble();

      return alertLine;
    },
    showOrHideMessagesBubble: function () {
      var componentsArray = OB.MobileApp.view.$.alertQueue.getComponents();
      if (componentsArray.length === 0) {
        OB.MobileApp.view.$.alertQueue.removeClass('alert-fade-in');
      } else {
        OB.MobileApp.view.$.alertQueue.addClass('alert-fade-in');
      }
    }
  }
});

enyo.kind({
  name: 'OB.UTIL.alertLine',
  classes: '',
  style: 'padding:2px;',
  components: [{
    name: 'title',
    style: 'display:none; float: left; margin-right:10px;' // hiden because is obvious info but still there for future use
  }, {
    name: 'txt'
  }],
  initComponents: function () {
    var me = this,
        destroyTimeout;
    this.inherited(arguments);

    this.$.title.setContent(this.title);
    this.$.txt.setContent(this.txt);

    this.addClass(this.type);

    // Define the timeout of the message. These values should be moved taken from global settings
    switch (this.type) {
    case 'alert-success':
      destroyTimeout = OB.MobileApp.model.get('permissions') ? OB.MobileApp.model.get('permissions').OBMOBC_AlertTimeoutForSuccess : 2000;
      break;
    case 'alert-warning':
      destroyTimeout = OB.MobileApp.model.get('permissions') ? OB.MobileApp.model.get('permissions').OBMOBC_AlertTimeoutForWarning : 2000;
      break;
    case 'alert-error':
      destroyTimeout = OB.MobileApp.model.get('permissions') ? OB.MobileApp.model.get('permissions').OBMOBC_AlertTimeoutForError : 4000;
      break;
    default:
      OB.error("DEVELOPER: The message of type '" + this.type + "' needs more code to be processed");
    }

    if (!this.keepVisible) {
      setTimeout(function () {
        me.hide();
        OB.UTIL.showAlert.showOrHideMessagesBubble();
      }, destroyTimeout);
    }
  },
  /**
   * Hides this particular message of the bubble of messages
   * @return undefined
   */
  hide: function () {
    this.destroy();
  }
});

/**
 * Shows a confirmation box.
 *
 * arguments:
 *   title: string with the title of the dialog
 *   text: string with the text of the dialog
 *   buttons: (optional) array of buttons. If this parameter is not present,
 *            a OK button will be shown, clicking on it will just close the
 *            confirmation dialog box.
 *            Each button in the array should have these attributes:
 *               label: text to be shown within the button
 *               action: (optional) function to be executed when the button
 *                       is tapped. If this attribute is not specified, the
 *                       action will be just to close the popup.
 *   options: (optional) array of options.
 *            - options.autoDismiss     // if true, any click outside the popup, closes it; if false, it behaves as if it was modal
 *            - options.onHideFunction: function () {}  // to be executed when the popup is closed with the close (X) button
 *            - options.style
 *
 */
enyo.kind({
  name: 'OB.UTIL.showConfirmation',
  statics: {
    display: function (title, text, buttons, options) {
      var container = OB.MobileApp.view.$.confirmationContainer ? OB.MobileApp.view.$.confirmationContainer : OB.MobileApp.view,
          components = container.getComponents ? container.getComponents() : [],
          i, dialog;

      function getDialog() {

        // Allow display in a confirmation message a literal or a list of components or literal messages
        var bodyContent;
        if (Array.isArray(text)) {
          var textComponents = [];
          _.each(text, function (currentText) {
            if (typeof currentText === 'string' || currentText instanceof String) {
              textComponents.push({
                kind: 'enyo.Control',
                content: currentText
              });
            } else {
              textComponents.push(currentText);
            }
          });
          bodyContent = {
            kind: 'enyo.Control',
            components: textComponents
          };
        } else {
          bodyContent = {
            kind: 'enyo.Control',
            content: text
          };
        }

        var box = {
          kind: 'OB.UI.ModalAction',
          name: 'dynamicConfirmationPopup',
          header: title,
          bodyContent: bodyContent,
          hideCloseButton: options && options.hideCloseButton,
          autoDismiss: !OB.UTIL.isNullOrUndefined(options) && !OB.UTIL.isNullOrUndefined(options.autoDismiss) ? options.autoDismiss : true,
          executeOnShow: function () {
            if (options && options.onShowFunction) {
              options.onShowFunction(this);
            }
            return true;
          },
          executeOnHide: function () {
            //the hide function only will be executed when
            //a button without action is used or when popup
            //is closed using background or x button
            if (options && !this.args.actionExecuted && options.onHideFunction) {
              options.onHideFunction(this);
            }
            return true;
          },
          bodyButtons: {}
        };

        if (options && options.style) {
          box.style = options.style;
        }

        //Test
        if (options && options.confirmFunction) {
          box.confirm = options.confirmFunction;
        }

        if (!buttons) {
          box.bodyButtons = {
            kind: 'OB.UI.ModalDialogButton',
            name: 'confirmationPopup_btnOk',
            isDefaultAction: !OB.UTIL.isNullOrUndefined(options) && !OB.UTIL.isNullOrUndefined(options.defaultAction) ? options.defaultAction : true,
            content: OB.I18N.getLabel('OBMOBC_LblOk'),
            tap: function () {
              this.doHideThisPopup();
            }
          };
          box.confirm = function () {
            if (this.$.bodyButtons.$.confirmationPopup_btnOk) {
              this.$.bodyButtons.$.confirmationPopup_btnOk.tap();
            }
          };
        } else {
          box.bodyButtons.components = [];
          _.forEach(buttons, function (btn) {
            var componentName;
            if (btn && btn.name) {
              componentName = btn.name;
            } else {
              componentName = 'confirmationPopup_btn' + btn.label;
            }
            var button = {
              kind: 'OB.UI.ModalDialogButton',
              name: componentName,
              content: btn.label,
              style: btn.style,
              isDefaultAction: !OB.UTIL.isNullOrUndefined(options) && !OB.UTIL.isNullOrUndefined(options.defaultAction) ? options.defaultAction : true,
              tap: function () {
                var params = {
                  actionExecuted: false
                };
                if (btn.action) {
                  params.actionExecuted = true;
                  btn.action(this);
                }
                this.doHideThisPopup({
                  args: params
                });
              }
            };
            box.bodyButtons.components.push(button);
            if (btn.isConfirmButton) {
              box.confirm = function () {
                this.$.bodyButtons.$[button.name].tap();
              };
            }
          }, this);
        }
        return box;
      }

      // remove old confirmation box
      for (i = 0; i < components.length; i++) {
        components[i].hide({
          ignoreOBEvents: true
        });
        components[i].destroy();
      }

      dialog = container.createComponent(getDialog());

      dialog.show();

      OB.UTIL.showLoading(false);
      return dialog;
    },
    setHeader: function (header, options) {
      var container = OB.MobileApp.view.$.confirmationContainer ? OB.MobileApp.view.$.confirmationContainer : OB.MobileApp.view,
          components = container.getComponents ? container.getComponents() : [],
          dialog = components.length > 0 ? components[0] : undefined;
      if (dialog) {
        dialog.setHeader(header);
      } else {
        OB.error("No dialog shown, cannot set the header.");
      }
    },
    setText: function (text, options) {
      var container = OB.MobileApp.view.$.confirmationContainer ? OB.MobileApp.view.$.confirmationContainer : OB.MobileApp.view,
          components = container.getComponents ? container.getComponents() : [],
          dialog = components.length > 0 ? components[0] : undefined;
      if (dialog && dialog.$.bodyContent && dialog.$.bodyContent.getControls().length > 0) {
        dialog.$.bodyContent.getControls()[0].content = text;
        dialog.$.bodyContent.render();
      } else {
        OB.error("No dialog shown, cannot set the text.");
      }
    }
  }
});

OB.UTIL.isSupportedBrowser = function () {

  return OB.UTIL.getSupportedBrowserInfo().isAllowed;
};

OB.UTIL.getSupportedBrowserInfo = function () {

  var pl = enyo.platform;
  var browserDef, version, i;

  for (i = 0; i < OB.UTIL.SupportedBrowsers.length; i++) {
    browserDef = OB.UTIL.SupportedBrowsers[i];
    version = pl[browserDef.name];
    if (version) {
      if (version < browserDef.allowed) {
        return {
          enyoPlatform: pl,
          browser: browserDef,
          isAllowed: false,
          isSupported: false,
          isPreferred: false
        };
      } else if (version < browserDef.required) {
        return {
          enyoPlatform: pl,
          browser: browserDef,
          isAllowed: true,
          isSupported: false,
          isPreferred: false
        };
      } else if (version < browserDef.preferred) {
        return {
          enyoPlatform: pl,
          browser: browserDef,
          isAllowed: true,
          isSupported: true,
          isPreferred: false
        };
      } else {
        return {
          enyoPlatform: pl,
          browser: browserDef,
          isAllowed: true,
          isSupported: true,
          isPreferred: true
        };
      }
    }
  }

  return {
    enyoPlatform: pl,
    browser: null,
    isAllowed: false,
    isSupported: false,
    isPreferred: false
  };
};

OB.UTIL.showLoadingConditions = new Backbone.Collection();

OB.UTIL.showLoading = function (value, condition) {
  if (value) {
    OB.MobileApp.view.$.containerLoading_label.setContent(OB.I18N.getLabel('OBMOBC_LblLoading'));
    if (condition) {
      OB.UTIL.showLoadingConditions.push(condition);
    }
    if (OB.MobileApp.view.$.containerLoading_fillingBar.hasNode() && !OB.MobileApp.model.get('isLoggingIn')) {
      OB.MobileApp.view.$.containerLoading_fillingBar.node.style.width = '100%';
      OB.MobileApp.view.$.containerLoading_loadedModels.hide();
    }
    if (!OB.UTIL.isNullOrUndefined(OB.MobileApp.view.activePopups)) {
      _.each(OB.MobileApp.view.activePopups, function (popup) {
        popup.setStyle('display:none');
      });
    }
    if (!OB.UTIL.isNullOrUndefined(OB.MobileApp.view.activeMenus)) {
      _.each(OB.MobileApp.view.activeMenus, function (menu) {
        menu.setStyle('display:none');
      });
    }
    OB.MobileApp.view.$.containerWindow.hide();
    OB.MobileApp.view.$.containerLoading.show();
    if (!OB.UTIL.showLoading.synchId) {
      OB.UTIL.showLoading.synchId = OB.UTIL.SynchronizationHelper.busyUntilFinishes('showLoading');
    }
  } else {
    if (condition) {
      OB.UTIL.showLoadingConditions.pop(condition);
    }
    if (!OB.UTIL.isNullOrUndefined(OB.MobileApp.view) && OB.UTIL.showLoadingConditions.length === 0 && !OB.UTIL.isNullOrUndefined(OB.MobileApp.view.$.containerLoading)) {
      OB.MobileApp.view.$.containerLoading_loadedModels.hide();
      OB.MobileApp.view.$.containerLoading.hide();
      OB.MobileApp.view.$.containerWindow.show();
      //After showLoading we need to adapt font sizes because when containerWindow is hidden we cannot resize component's font
      OB.UTIL.adaptAllFontSizes();
    }
    if (!OB.UTIL.isNullOrUndefined(OB.MobileApp.view.activePopups)) {
      _.each(OB.MobileApp.view.activePopups, function (popup) {
        popup.setStyle('display:initial');
      });
    }
    if (!OB.UTIL.isNullOrUndefined(OB.MobileApp.view.activeMenus)) {
      _.each(OB.MobileApp.view.activeMenus, function (menu) {
        menu.setStyle('display:initial');
      });
    }
    if (!OB.UTIL.isNullOrUndefined(OB.UTIL.showLoading.synchId)) {
      OB.UTIL.SynchronizationHelper.finished(OB.UTIL.showLoading.synchId, 'showLoading');
      OB.UTIL.showLoading.synchId = null;
    }
  }
};

OB.UTIL.showProcessing = function (value, label) {
  if (value) {
    OB.MobileApp.view.$.processingLabel.setContent(OB.UTIL.isNullOrUndefined(label) ? 'Processing...' : label);
    OB.MobileApp.view.$.processingContainer.show();
  } else {
    OB.MobileApp.view.$.processingContainer.hide();
  }
};

OB.UTIL.showLoggingOut = function (value) {
  if (value) {
    OB.MobileApp.view.$.containerLoggingOut_label.setContent(OB.I18N.getLabel('OBMOBC_LblLoggingOut'));
    OB.MobileApp.view.$.containerWindow.hide();
    OB.MobileApp.view.$.containerLoggingOut.show();
  } else {
    OB.MobileApp.view.$.containerLoggingOut.hide();
    OB.MobileApp.view.$.containerWindow.show();
  }
};

OB.UTIL.showSuccess = function (s) {
  return OB.UTIL.showAlert.display(s, OB.I18N.getLabel('OBMOBC_LblSuccess'), 'alert-success');
};

OB.UTIL.showWarning = function (s) {
  return OB.UTIL.showAlert.display(s, OB.I18N.getLabel('OBMOBC_LblWarning'), 'alert-warning');
};

OB.UTIL.showError = function (s) {
  OB.error(s);
  if (OB.MobileApp.model.hasPermission('OBMOBC_EnableErrorPopup', true)) {
    return OB.UTIL.showConfirmation.display("", s);
  } else {
    return OB.UTIL.showAlert.display(s, OB.I18N.getLabel('OBMOBC_LblError'), 'alert-error');
  }
};

OB.UTIL.showStatus = function (s) {
  return OB.UTIL.showAlert.display(s, 'Wait', '');
};

/**
 * Shows a success type message in the bubble of messages
 * @param  string   idLabel       the id of the label to be shown
 * @param  string   hidesIdLabel  the id of the message to be destroyed when this new message is shown. leave empty fo no effect
 */
OB.UTIL.showI18NSuccess = function (idLabel, hidesIdLabel) {
  return OB.UTIL.showAlert.display(OB.I18N.getLabel(idLabel), OB.I18N.getLabel('OBMOBC_LblSuccess'), 'alert-success', false, idLabel, hidesIdLabel);
};

/**
 * Shows a warning type message in the bubble of messages
 * @param  string   idLabel       the id of the label to be shown
 * @param  string   hidesIdLabel  the id of the message to be destroyed when this new message is shown. leave empty fo no effect
 */
OB.UTIL.showI18NWarning = function (idLabel, hidesIdLabel) {
  return OB.UTIL.showAlert.display(OB.I18N.getLabel(idLabel), OB.I18N.getLabel('OBMOBC_LblWarning'), 'alert-warning', false, idLabel, hidesIdLabel);
};

/**
 * Shows an error type message in the bubble of messages
 * @param  string   idLabel       the id of the label to be shown
 * @param  string   hidesIdLabel  the id of the message to be destroyed when this new message is shown. leave empty fo no effect
 */
OB.UTIL.showI18NError = function (idLabel, hidesIdLabel) {
  return OB.UTIL.showAlert.display(OB.I18N.getLabel(idLabel), OB.I18N.getLabel('OBMOBC_LblWarning'), 'alert-error', false, idLabel, hidesIdLabel);
};

// This funtion returns an array of Enyo components (the object and its
// children) sorted
// in the same way it they are rendered in the html
OB.UTIL.getAllChildsSorted = function (component, result) {
  var i;
  if (Object.prototype.toString.call(component) !== '[object Array]') {
    component = [component];
  }
  if (!result) {
    result = [];
  }
  for (i = 0; i < component.length; i++) {
    result.push(component[i]);
    if (typeof component[i].children !== 'undefined' && component[i].children.length !== 0) {
      result = OB.UTIL.getAllChildsSorted(component[i].children, result);
    }
  }
  return result;
};

// Utilities for the login process
OB.UTIL.showLoadingMessage = function (message) {
  if (OB.MobileApp.view && OB.MobileApp.model.get('isLoggingIn')) {
    OB.MobileApp.view.$.containerLoading_loadedModels.show();
    OB.MobileApp.view.$.containerLoading_loadedModels.setContent(message);
  }
};
OB.UTIL.startLoadingSteps = function () {
  OB.UTIL.loadingSteps = 3;
  OB.UTIL.loadingSteps += OB.UTIL.getNonLocalModels().length;
  OB.UTIL.currentLoadingStep = 0;
};
OB.UTIL.completeLoadingStep = function () {
  var width;
  if (OB.MobileApp.view && OB.MobileApp.model.get('isLoggingIn')) {
    OB.UTIL.currentLoadingStep += 1;
    width = (OB.UTIL.currentLoadingStep / OB.UTIL.loadingSteps) * 100 + '%';
    if (OB.MobileApp.view.$.containerLoading_fillingBar.hasNode()) {
      OB.MobileApp.view.$.containerLoading_fillingBar.node.style.width = width;
    }
  }
};
OB.UTIL.getNonLocalModels = function () {
  var nonLocalModels = [];
  _.each(OB.MobileApp.model.windowRegistry.registeredWindows, function (windowp) {
    var windowClass, windowName, datasources, w, c, path;
    windowClass = windowp.windowClass;
    windowName = windowp.route;
    if (OB && OB.DATA && OB.DATA[windowName]) {
      datasources = OB.DATA[windowName];
    } else if (windowClass.prototype && windowClass.prototype.windowmodel && windowClass.prototype.windowmodel.prototype && windowClass.prototype.windowmodel.prototype.models) {
      datasources = windowClass.prototype.windowmodel.prototype.models;
    } else if (typeof windowClass === 'string') {
      w = window;
      path = windowClass.split('.');
      for (c = 0; c < path.length; c++) {
        w = w[path[c]];
      }
      if (w.prototype && w.prototype.windowmodel && w.prototype.windowmodel.prototype && w.prototype.windowmodel.prototype.models) {
        datasources = w.prototype.windowmodel.prototype.models;
      }
    }
    _.each(datasources, function (model) {
      var modelObj;
      if (model && model.modelName) {
        modelObj = OB.Model[model.modelName];
      } else if (model && model.prototype && model.prototype.modelName) {
        modelObj = OB.Model[model.prototype.modelName];
      }
      if (modelObj && modelObj.prototype && modelObj.prototype.modelName && modelObj.prototype.source && !modelObj.prototype.local) {
        nonLocalModels.push(modelObj.prototype);
      }
    });
  });
  return nonLocalModels;
};

OB.UTIL.createElipsisEffect = function (enyoObject, options) {
  var maxCharactersPerLine = 11;
  var ellipseText = '...';
  var maxWordCharactersWithEllipsis = maxCharactersPerLine - ellipseText.length;
  var maxWords = 4;

  var truncated = false;
  var line1 = '';
  var line2 = '';
  var line1Try = '';
  var line2Try = '';
  var line1IsFull = false;
  var line2IsFull = false;
  var j = 0;
  var k = 0;
  var origContent = enyoObject.getContent();
  var divContentArray, reversedContentArray;
  options = options || {};

  if (options.reUseOriginalContent === true) {
    if (OB.UTIL.isNullOrUndefined(enyoObject.origContent)) {
      enyoObject.origContent = origContent;
    } else {
      origContent = enyoObject.origContent;
    }
  }

  if (enyo.Panels.isScreenNarrow()) {
    if (window.innerWidth >= 650) {
      maxCharactersPerLine = 13;
      maxWordCharactersWithEllipsis = maxCharactersPerLine - ellipseText.length;
    }
  } else {
    if (window.innerWidth >= 1300) {
      maxCharactersPerLine = 13;
      maxWordCharactersWithEllipsis = maxCharactersPerLine - ellipseText.length;
    }
    if (window.innerWidth <= 1000) {
      maxCharactersPerLine = 9;
      maxWordCharactersWithEllipsis = maxCharactersPerLine - ellipseText.length;
    }
  }
  if (OB.UTIL.isNullOrUndefined(origContent)) {
    return;
  } else {
    divContentArray = origContent.split(' ');
    reversedContentArray = origContent.split(' ');
    reversedContentArray.reverse();
    if (divContentArray.length === 1) {
      if (divContentArray[0].length <= maxCharactersPerLine) {
        line1 = divContentArray[0];
      } else {
        line1 = divContentArray[0].substr(0, maxWordCharactersWithEllipsis) + ellipseText;
      }
      enyoObject.setContent(line1);
      return;
    } else {
      if (divContentArray.length > maxWords) {
        while (divContentArray.length > maxWords) {
          divContentArray.pop();
          truncated = true;
        }
      }
      for (j = 0; j <= divContentArray.length - 1; j++) {
        if (line1.length < maxCharactersPerLine && !line1IsFull) {
          line1Try = line1;
          if (line1Try.length === 0) {
            line1Try = line1Try + divContentArray[j];
          } else {
            line1Try = line1Try + ' ' + divContentArray[j];
          }
          if (line1Try.length <= maxCharactersPerLine) {
            if (line1.length === 0) {
              line1 = line1 + reversedContentArray.pop();
            } else {
              line1 = line1 + ' ' + reversedContentArray.pop();
            }
          } else {
            line1IsFull = true;
            break;
          }
        } else {
          break;
        }
      }
      line1IsFull = true;
      for (k = j; k <= divContentArray.length - 1; k++) {
        if (line2.length < maxCharactersPerLine && !line2IsFull) {
          line2Try = line2;
          if (line2Try.length === 0) {
            line2Try = line2Try + divContentArray[k];
          } else {
            line2Try = line2Try + ' ' + divContentArray[k];
          }
          if (line2Try.length <= maxCharactersPerLine) {
            if (line2.length === 0) {
              line2 = line2 + reversedContentArray.pop();
            } else {
              line2 = line2 + ' ' + reversedContentArray.pop();
            }
          } else {
            line2IsFull = true;
            if (line2.length === 0) {
              line2 = line2 + reversedContentArray.pop();
            } else {
              line2 = line2 + ' ' + reversedContentArray.pop();
            }
            break;
          }
        } else {
          line2IsFull = true;
          break;
        }
      }
      if (line2IsFull) {
        line2 = line2.substr(0, maxWordCharactersWithEllipsis) + ellipseText;
      } else {
        if (truncated) {
          if ((line2 + ellipseText).length <= maxCharactersPerLine) {
            line2 += ellipseText;
          } else {
            line2 = line2.substr(0, maxWordCharactersWithEllipsis) + ellipseText;
          }
        }
      }
      if (line1.length > 0) {
        enyoObject.setContent(line1 + ' ' + line2);
      } else {
        enyoObject.setContent(line2);
      }
      return;
    }
  }
};

/* Adapt component font for dom object, passing the obj itself or its id as first argument */
OB.UTIL.adaptFontSize = function (obj, minFntSz, maxFntSz, maxHgt, minLnCalc) {

  var maxWidth = false,
      currentWidth = false,
      currentBox = false,
      currentTextObj = false,
      classNameArr = ['fitText', 'buttonText'],
      minFontSize = minFntSz || 9,
      maxFontSize = maxFntSz || 16,
      maxHeight = maxHgt || 18,
      minLengthForCalc = minLnCalc || 10;

  function fit(maxFontSize) {
    //to number
    var tmpFontSize = OB.DEC.add(currentTextObj.style.fontSize.replace('px', ''), 0, 0);
    currentTextObj.style.fontSize = tmpFontSize + 1 + 'px';
    var tmpWidth = currentTextObj.offsetWidth,
        tmpHeight = currentTextObj.offsetHeight,
        finalFontSize = 0;
    if (tmpWidth >= currentWidth && tmpWidth < maxWidth && tmpHeight < maxHeight && tmpFontSize < maxFontSize) {
      currentWidth = currentTextObj.offsetWidth;
      fit(maxFontSize);
    } else {
      finalFontSize = OB.DEC.add(currentTextObj.style.fontSize.replace('px', ''), 0, 0) - OB.DEC.One;
      if (maxFontSize && maxFontSize < finalFontSize) {
        finalFontSize = maxFontSize;
      }
      currentTextObj.style.fontSize = finalFontSize + 'px';
    }
  }
  if (typeof obj === 'string') {
    currentBox = document.getElementById(obj);
  } else if (typeof obj === 'object') {
    currentBox = obj;
  }
  if (!currentBox) {
    return;
  }
  currentTextObj = currentBox.getElementsByTagName('SPAN')[0];
  currentTextObj.style.fontSize = maxFontSize + 'px';
  maxWidth = currentBox.offsetWidth;
  if (currentTextObj.offsetWidth !== 0 && currentTextObj.textContent.length >= minLengthForCalc) {
    // start with the minFontSize(9px or 16px) to reduce iterations
    // 9px is the minimum font size for the text and 16px is the minimun font size for the button
    currentTextObj.style.fontSize = minFontSize + 'px';
    currentWidth = currentTextObj.offsetWidth;
    fit(maxFontSize);
  } else {
    currentTextObj.style.fontSize = maxFontSize + 'px';
  }
};

/* Adapt all component's font for components with 'fitText' or 'buttonText' class */
OB.UTIL.adaptAllFontSizes = function () {
  var classNameArr = ['fitText', 'buttonText'],
      minFontSize, maxFontSize, maxHeight, minLengthForCalc = 10;

  _.each(classNameArr, function (className) {
    var objs = document.getElementsByClassName(className);
    if (objs.length !== 0) {
      minFontSize = className === 'buttonText' ? 15 : 9;
      maxFontSize = className === 'buttonText' ? 30 : 16;
      maxHeight = className === 'buttonText' ? 57 : 18;
      _.each(objs, function (obj) {
        OB.UTIL.adaptFontSize(obj, minFontSize, maxFontSize, maxHeight, minLengthForCalc);
      });
    }
  });
};