/*
 ************************************************************************************
 * Copyright (C) 2012-2018 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Audio, Backbone, document, _ */

enyo.kind({
  name: 'OB.UI.Keyboard',
  commands: {},
  buttons: {},
  maxButtons: 6,
  status: '',
  payment: null,
  sideBarEnabled: false,
  destroy: function () {
    this.buttons = null;
    this.commands = null;
    this.inherited(arguments);
  },
  keyMatcher: /^([0-9]|\.|\/|,| |%|\\|\*|[a-z]|[A-Z])$/,
  classes: 'row-fluid',
  components: [{
    kind: 'OB.UI.MoreButtons',
    name: 'OB_UI_MoreButtons'
  }, {
    name: 'toolbarcontainer',
    classes: 'span3'
  }, {
    classes: 'span9',
    components: [{
      classes: 'row-fluid',
      components: [{
        classes: 'span8',
        components: [{
          style: 'margin:5px',
          components: [{
            style: 'text-align: right; width: 100%; height: 40px;',
            components: [{
              style: 'margin: 0px 0px 9px 0px; background-color: whiteSmoke; border: 1px solid #CCC; word-wrap: break-word; font-size: 35px; height: 33px; padding: 22px 5px 0px 0px;',
              components: [{
                name: 'editbox',
                style: 'border: 0px; word-wrap: break-word; font-size: 30px; font-family: monospace; height: 54px; text-align: right; width: 99%;',
                adjustFontSize: function () {
                  var contentLength = this.getContent().length;
                  var newFontSize = 30;
                  if (contentLength >= 15) {
                    newFontSize = 15;
                  } else if (contentLength >= 11) {
                    newFontSize = 23;
                  }
                  var newStyle = enyo.format("border: 0px; word-wrap: break-word; font-size: %spx; font-family: monospace; height: 54px; text-align: right; width: 99%;", newFontSize);
                  this.setStyle(newStyle);
                }
              }]
            }]
          }]
        }]
      }, {
        classes: 'span4',
        components: [{
          kind: 'OB.UI.ButtonKey',
          name: 'OBKEY_backspace',
          classButton: 'btn-icon btn-icon-backspace',
          command: 'del'
        }]
      }, {
        classes: 'row-fluid',
        name: 'OBKeyBoard_centerAndRightSideContainer',
        components: [{ // keypadcontainer
          classes: 'span8',
          name: 'keypadcontainer'
        }, {
          name: 'OBKeyBoard_rightSideToolbar',
          classes: 'span4',
          components: [{
            // rigth toolbar with qty, discount... buttons
            name: 'sideenabled',
            components: [{
              classes: 'row-fluid',
              components: [{
                classes: 'span6',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  classButton: 'btnkeyboard-num btnkeyboard-minus',
                  command: '-',
                  name: 'minusBtn',
                  init: function () {
                    this.switchButton();
                  }
                }]
              }, {
                classes: 'span6',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  label: '+',
                  classButton: 'btnkeyboard-num btnkeyboard-plus',
                  command: '+',
                  name: 'plusBtn',
                  init: function () {
                    this.switchButton();
                  }
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  name: 'btnQty',
                  kind: 'OB.UI.ButtonKey',
                  command: 'line:qty',
                  init: function (model) {
                    this.switchButton();
                  }
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'btnPrice',
                  classButton: 'btnkeyboard-inactive',
                  disabled: true,
                  permission: 'OBPOS_order.changePrice',
                  command: 'line:price'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'btnDiscount',
                  permission: 'OBPOS_order.discount',
                  command: 'line:dto',
                  init: function (model) {
                    var receipt = model.get('order'),
                        me = this;
                    if (receipt) {
                      if (OB.MobileApp.model.get('permissions')["OBPOS_retail.discountkeyboard"] === false) {
                        this.command = 'line:dto';
                      } else {
                        receipt.on('change', function () {
                          OB.UTIL.isDisableDiscount(receipt, function (disable) {
                            me.waterfall('onDisableButton', {
                              disabled: disable
                            });
                          });
                        }, this);
                        receipt.get('lines').on('add remove', function () {
                          if (model.get('leftColumnViewManager') && model.get('leftColumnViewManager').isMultiOrder()) {
                            this.waterfall('onDisableButton', {
                              disabled: true
                            });
                          }
                          if (OB.UTIL.isDisableDiscount) {
                            OB.UTIL.isDisableDiscount(receipt, function (disable) {
                              if (!OB.MobileApp.model.hasPermission(me.permission, true) || disable) {
                                me.waterfall('onDisableButton', {
                                  disabled: true
                                });
                              } else {
                                me.waterfall('onDisableButton', {
                                  disabled: false
                                });
                              }
                            });
                          }
                        }, this);
                        this.command = 'screen:dto';
                      }
                    }
                  }
                }]
              }]
            }]
          }, {
            // empty right toolbar used in case the keyboard
            // shouldn't support these buttons
            name: 'sidedisabled',
            components: [{
              classes: 'row-fluid',
              components: [{
                classes: 'span6',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_disabled_A'
                }]
              }, {
                classes: 'span6',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_disabled_B'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_disabled_C'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_disabled_D'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_disabled_E'
                }]
              }]
            }]
          }, {
            // right toolbar for ticket discounts
            name: 'ticketDiscountsToolbar',
            components: [{
              classes: 'row-fluid',
              components: [{
                classes: 'span6',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_discounts_A'
                }]
              }, {
                classes: 'span6',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_discounts_B'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_discounts_C'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_discounts_D'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_discounts_E',
                  description: 'Discount button when discounts sidebar is used',
                  permission: 'OBPOS_retail.advDiscounts',
                  command: 'ticket:discount'
                }]
              }]
            }]
          }, {
            // rigth toolbar with plus and minus (Cash upr side toolbar)
            name: 'sidecashup',
            components: [{
              classes: 'row-fluid',
              components: [{
                classes: 'span6',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  label: '-',
                  classButton: 'btnkeyboard-num btnkeyboard-minus',
                  command: '-',
                  name: 'OBKEY_right_cashup_A',
                  description: '- button used for cashup'
                }]
              }, {
                classes: 'span6',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  label: '+',
                  classButton: 'btnkeyboard-num btnkeyboard-plus',
                  name: 'OBKEY_right_cashup_B'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_cashup_C'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_cashup_D'
                }]
              }]
            }, {
              classes: 'row-fluid',
              components: [{
                classes: 'span12',
                components: [{
                  kind: 'OB.UI.ButtonKey',
                  name: 'OBKEY_right_cashup_E'
                }]
              }]
            }]
          }, {
            classes: 'row-fluid',
            components: [{
              classes: 'span12',
              components: [{
                kind: 'OB.UI.ButtonKey',
                classButton: 'btn-icon btn-icon-enter',
                command: 'OK',
                name: 'btnEnter'
              }]
            }]
          }]
        }]
      }]
    }]
  }],

  events: {
    onCommandFired: '',
    onStatusChanged: '',
    onHoldActiveCmd: ''
  },

  handlers: {
    onGlobalKeypress: 'globalKeypressHandler',
    onCommandFired: 'commandHandler',
    onRegisterButton: 'registerButton',
    onKeyboardDisabled: 'keyboardDisabled',
    onClearEditBox: 'clearEditBox',
    onEnableQtyButton: 'enableQtyButton',
    onEnablePlusButton: 'enablePlusButton',
    onEnableMinusButton: 'enableMinusButton'
  },
  isEnabled: true,

  disableCommandKey: function (inSender, inEvent) {
    this.waterfall('onDisableButton', inEvent);
  },

  keyboardDisabled: function (inSender, inEvent) {
    if (inEvent.status) {
      _.each(this.buttons, function (btn) {
        if (!btn.hasClass('btnkeyboard-inactive')) {
          btn.setDisabled(true);
          btn.addClass('btnkeyboard-inactive');
        }
      });
      this.isEnabled = false;
    } else {
      _.each(this.buttons, function (btn) {
        if (btn.disabled) {
          btn.setDisabled(false);
          btn.removeClass('btnkeyboard-inactive');
        }
      });
      this.isEnabled = true;
    }
  },

  keyEquivalence: [{
    keyUp: [107, 187],
    keyPress: 43,
    char: '+',
    action: function (keyboard) {
      var content = keyboard.$.editbox.getContent(),
          contentLength = content.length,
          lastCharacter = content.substring(contentLength - 1, contentLength);
      if ((!OB.MobileApp.view.scanMode || !OB.MobileApp.scanning) && lastCharacter !== '\\') {
        keyboard.doCommandFired({
          key: '+'
        });
      } else {
        keyboard.writeCharacter(this.char);
      }
    }
  }, {
    keyUp: [111, 191],
    keyPress: 47,
    char: '/'
  }, {
    keyUp: 186,
    keyPress: 58,
    char: ':'
  }, {
    keyUp: [109, 189],
    keyPress: 45,
    char: '-',
    action: function (keyboard) {
      var content = keyboard.$.editbox.getContent(),
          contentLength = content.length,
          lastCharacter = content.substring(contentLength - 1, contentLength);
      if ((!OB.MobileApp.view.scanMode || !OB.MobileApp.scanning) && lastCharacter !== '\\') {
        keyboard.doCommandFired({
          key: '-'
        });
      } else {
        keyboard.writeCharacter(this.char);
      }
    }
  }, {
    keyUp: 46,
    keyPress: [200, 240],
    action: function (keyboard) {
      keyboard.doCommandFired({
        key: "line:delete"
      });
    }
  }, {
    keyUp: 8,
    keyPress: 8,
    action: function (keyboard) {
      OB.MobileApp.view.scanningFocus();
      keyboard.writeCharacter('del');
    }
  }, {
    keyUp: [110, 190],
    keyPress: 46,
    action: function (keyboard) {
      keyboard.writeCharacter(OB.Format.defaultDecimalSymbol);
    }
  }],

  getKeyActionFromCode: function (keyCode) {
    var i, kEqCode, keyFits = false,
        findFunc = function (elem) {
        return elem === keyCode;
        };
    for (i = 0; i < this.keyEquivalence.length; i++) {
      if (OB.UTIL.isIOS()) {
        kEqCode = this.keyEquivalence[i].keyPress;
      } else {
        kEqCode = this.keyEquivalence[i].keyUp;
      }
      if (kEqCode instanceof Array) {
        keyFits = _.find(kEqCode, findFunc);
      } else if (kEqCode === keyCode) {
        keyFits = true;
      }
      if (keyFits) {
        return this.keyEquivalence[i];
      }
    }

    return null;
  },

  /**
   * Managing key up events. KeyDown or KeyPress is not properly
   * working in Android Chrome 26
   */
  globalKeypressHandler: function (inSender, inEvent) {
    var i, which = inEvent.keyboardEvent.which,
        key = inEvent.keyboardEvent.key,
        actualStatus = null,
        actualChar, keeper, keyAction;
    if (inEvent.keyboardEvent.originator && (inEvent.keyboardEvent.originator.hasClass('modal-dialog') || inEvent.keyboardEvent.originator.hasClass('modal-popup'))) {
      return true;
    }

    if (OB.MobileApp.model.get('useBarcode') && OB.MobileApp.view.scanMode) {
      //Issue 25013. This flag is checked by keypressHandler function in ob-terminal-component.js
      OB.MobileApp.keyPressProcessed = true;

      if (which >= 96 && which <= 105) {
        which -= 48;
      }

      if (OB.UTIL.isIOS() && key) {
        actualChar = key;
      } else {
        actualChar = (key > ' ' && key < '\u007F') ? key // key pressed is an standard ASCII character
        : String.fromCharCode(which); // key pressed must be calculated from keycode
      }

      keyAction = this.getKeyActionFromCode(which);
      if (keyAction && actualChar !== '*' && actualChar !== OB.Format.defaultDecimalSymbol) {
        if (keyAction.char) {
          actualChar = keyAction.char;
        }
        if (keyAction.action) {
          keyAction.action(this);
        }
      }


      if (inEvent.keyboardEvent.code === 'F11' || inEvent.keyboardEvent.code === 'F10' || inEvent.keyboardEvent.code === 'F9' || inEvent.keyboardEvent.code === 'F8' || inEvent.keyboardEvent.code === 'F7' || inEvent.keyboardEvent.code === 'F23' || inEvent.keyboardEvent.code === 'Delete') {
        actualChar = '';
      }
      if (OB.UTIL.isIOS() && inEvent.keyboardEvent.key === 'Backspace') {
        //Handle special case of backspace key in iOS
        this.writeCharacter('del');
      }

      if (actualChar && actualChar.match(this.keyMatcher)) {
        this.writeCharacter(actualChar);
      } else if (which === 13) { //Handle ENTER key
        actualStatus = this.getStatus();
        keeper = document.getElementById('_focusKeeper');
        if (keeper) {
          if (keeper.value && keeper.value.length > 0) {
            this.$.editbox.setContent(keeper.value);
            keeper.value = '';
          } else {
            keeper.value = '';
            this.$.editbox.setContent(this.$.editbox.getContent());
          }
        }
        if (this.$.editbox.getContent() === '0') {
          this.doCommandFired({
            key: "OK"
          });
        } else if (actualStatus) {
          this.execCommand(actualStatus, this.getString());
          this.setStatus('');
        } else {
          OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_KeypadTargetMissing'));
        }
      } else if (OB.Format.defaultDecimalSymbol === '.' && actualChar !== '+' && actualChar !== '-') { //Handle any keypress except any kind of dot (.) '+' or '-'
        if (which === 229) {
          if (this.$.editbox.getContent() !== null) {
            //It seems we are already scanning something, we should ignore this event as it's likely repeated
            return true;
          }
          var scan = document.getElementById('_focusKeeper').value;
          for (i = 0; i < scan.length; i++) {
            this.writeCharacter(scan.charAt(i));
          }
        } else {
          this.writeCharacter(actualChar);
        }
      }
      return true;
    } else {
      OB.MobileApp.keyPressProcessed = true;
      //scanMode is disable, raise an error sound only if the preference allows it.
      if (OB.MobileApp.model.hasPermission('OBMOBC_ReproduceErrorSoundOnFailedScan', true) && OB.MobileApp.model.get('reproduceErrorSound')) {
        var error_sound = new Audio('../org.openbravo.mobile.core/sounds/Computer_Error.mp3');
        error_sound.play();
      }
      return true;
    }
  },

  virtualKeypressHandler: function (key, options) {
    var content = this.$.editbox.getContent(),
        contentLength = content.length,
        lastCharacter = content.substring(contentLength - 1, contentLength);
    if (options && options.fromPopup) {
      this.waterfall('onCloseAllPopups');
    }
    if (key.match(this.keyMatcher) || (key === 'del')) {
      this.writeCharacter(key);
    } else {
      this.doCommandFired({
        key: key
      });
    }
  },

  writeCharacter: function (character) {
    var content = this.$.editbox.getContent();
    var contentLength = content.length;
    if (contentLength >= 20 && character !== 'del') {
      OB.UTIL.showError(OB.I18N.getLabel('OBMOBC_ErrorMaxNumber'));
      return;
    }
    if ((character.match(this.keyMatcher) || character === '+' || character === '-') && this.isEnabled) {
      this.$.editbox.setContent(content + character);
    } else if (character === 'del') {
      if (contentLength > 0) {
        this.$.editbox.setContent(content.substring(0, contentLength - 1));
      }
    }
    this.$.editbox.adjustFontSize();
    if (!OB.UTIL.isIOS()) {
      document.getElementById('_focusKeeper').value = this.$.editbox.getContent();
    }
  },

  setStatus: function (newstatus) {
    var btn = this.buttons[this.status];

    if (btn && (btn.classButtonActive || (btn.owner && btn.owner.classButtonActive))) {
      btn.removeClass(btn.classButtonActive || btn.owner.classButtonActive);
    }
    this.status = newstatus;

    // sending the event to the components bellow this one
    this.waterfall('onStatusChanged', {
      status: newstatus
    });
    // sending the event to the components above this one
    this.doStatusChanged({
      keyboard: this,
      payment: OB.MobileApp.model.paymentnames ? OB.MobileApp.model.paymentnames[this.status] : null,
      status: this.status
    });

    // set the right keypad by default
    if (this.namedkeypads[this.payment || this.status]) {
      this.showKeypad(this.namedkeypads[this.payment || this.status]);
    } else {
      this.showKeypad('basic');
    }

    btn = this.buttons[this.status];
    if (btn && (btn.classButtonActive || (btn.owner && btn.owner.classButtonActive))) {
      btn.addClass(btn.classButtonActive || btn.owner.classButtonActive);
    }
    this.lastStatus = this.status;
  },

  getStatus: function () {
    //returns the current status of the keyboard. If the keyboard doesn't have any status then
    //the function returns the default action for the keyboard.
    if (this.status) {
      if (this.status === "") {
        return this.defaultcommand;
      } else {
        return this.status;
      }
    }
    return this.defaultcommand;
  },

  execCommand: function (cmd, txt) {
    var cmddefinition = this.commands[cmd];
    //if (!cmddefinition.permission || OB.MobileApp.model.hasPermission(cmddefinition.permission)) {
    cmddefinition.action(this, txt);
    //}
  },

  execStatelessCommand: function (cmd, txt) {
    this.commands[cmd].action(this, txt);
  },

  getNumber: function () {
    return OB.I18N.parseNumber(this.getString());
  },

  getString: function () {
    var s = this.$.editbox.getContent();
    this.$.editbox.setContent('');
    return s;
  },

  clearInput: function () {
    this.$.editbox.setContent('');
    document.getElementById('_focusKeeper').value = '';
    this.lastStatus = '';
    this.setStatus('');
  },
  clearEditBox: function () {
    this.$.editbox.setContent('');
    document.getElementById('_focusKeeper').value = '';
  },
  enableQtyButton: function (inSender, inEvent) {
    if (inEvent.enable) {
      this.$.btnQty.disableButton(this.$.btnQty, {
        disabled: false
      });
    } else {
      this.$.btnQty.disableButton(this.$.btnQty, {
        disabled: true
      });
    }
  },
  enablePlusButton: function (inSender, inEvent) {
    if (inEvent.enable) {
      this.$.plusBtn.disableButton(this.$.plusBtn, {
        disabled: false
      });
    } else {
      this.$.plusBtn.disableButton(this.$.plusBtn, {
        disabled: true
      });
    }
  },
  enableMinusButton: function (inSender, inEvent) {
    if (inEvent.enable) {
      this.$.minusBtn.disableButton(this.$.minusBtn, {
        disabled: false
      });
    } else {
      this.$.minusBtn.disableButton(this.$.minusBtn, {
        disabled: true
      });
    }
  },
  commandHandler: function (inSender, inEvent) {
    var txt;
    var cmd = inEvent.key;

    if (cmd === 'OK') {
      txt = this.getString();

      // Shortcut to lock screen
      // Check for preference to disable lock screen, pass second parameter "true" to avoid false
      // positives on automatic roles.
      // hasPermission only loads preferences that belong to the mobile application in use. The
      // preference is defined using a posterminal preference property. Other applications that
      // want to disable the shortcut should deliver its own preference property and add it to
      // this if clause.
      if (txt === '0' && this.status === '' && !OB.MobileApp.model.hasPermission('DisableLockShortcut', true, true)) {
        OB.MobileApp.model.lock();
        return;
      }
      if (txt && this.status === '') {
        if (this.defaultcommand) {
          this.execCommand(this.defaultcommand, txt);
        } else {
          OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_KeypadTargetMissing'));
        }
      } else if (txt && this.status !== '') {
        this.execCommand(this.status, txt);
        if (this.commands[this.status] && !this.commands[this.status].holdActive) {
          this.setStatus('');
        }
      }
    } else if (this.commands[cmd]) {
      txt = this.getString();
      if (this.commands[cmd].stateless) {
        // Stateless commands: add, subs, ...
        this.execStatelessCommand(cmd, txt);
      } else {
        // Statefull commands: quantity, price, discounts, payments ...
        if (txt) {
          this.setStatus(cmd);
          this.execCommand(cmd, txt);
        } else {
          if (this.status === cmd) {
            this.setStatus('');
          } else {
            this.setStatus(cmd);
          }
        }
      }
      if (this.commands[cmd].holdActive) {
        this.doHoldActiveCmd({
          cmd: cmd
        });
      }
    } else {
      OB.UTIL.showWarning(OB.I18N.getLabel('OBMOBC_NoActionDefined'));
    }
  },

  registerButton: function (inSender, inEvent) {
    var me = this,
        button = inEvent.originator;
    if (button.command) {
      if (button.definition) {
        this.addCommand(button.command, button.definition);
      }
      if (button.command === '---') {
        // It is the null command
        button.command = false;
      } else if (!button.command.match(this.keyMatcher) && button.command !== 'OK' && button.command !== 'del' && button.command !== String.fromCharCode(13) && !this.commands[button.command]) {
        // is not a key and does not exists the command
        button.command = false;
      } else if (button.permission && !OB.MobileApp.model.hasPermission(button.permission)) {
        // does not have permissions.
        button.command = false;
      }
    }

    if (button.command) {
      button.$.button.tap = function () {
        if (button.$.button.disabled) {
          return true;
        }
        if (button && button.definition && button.definition.includedInPopUp) {
          me.virtualKeypressHandler(button.command, {
            fromPopup: button.definition.includedInPopUp
          });
        } else {
          me.virtualKeypressHandler(button.command);
        }
      };

      this.addButton(button.command, button.$.button);
      button.$.button.removeClass('btnkeyboard-inactive');

    } else {
      button.disableButton(button, {
        disabled: true
      });
    }
  },

  initComponents: function () {
    var undef;
    this.buttons = {}; // must be intialized before calling super, not after.
    this.activekeypads = [];
    this.namedkeypads = {};
    OB.MobileApp.view.keyReceivers.push(this);
    this.inherited(arguments);
    // setting labels
    this.$.minusBtn.$.button.setContent(OB.I18N.getLabel('OBMOBC_Character')[3]);
    this.$.btnQty.$.button.setContent(OB.I18N.getLabel('OBMOBC_KbQuantity'));
    this.$.btnPrice.$.button.setContent(OB.I18N.getLabel('OBMOBC_KbPrice'));
    this.$.btnDiscount.$.button.setContent(OB.I18N.getLabel('OBMOBC_KbDiscount'));
    this.$.OBKEY_right_discounts_E.$.button.setContent(OB.I18N.getLabel('OBMOBC_KbDiscount'));

    if (this.buttonsDef) {
      if (this.buttonsDef.sideBar) {
        if (this.buttonsDef.sideBar.plusI18nLbl !== undef) {
          this.$.plusBtn.setLabel(OB.I18N.getLabel(this.buttonsDef.sideBar.plusI18nLbl));
        }
        if (this.buttonsDef.sideBar.minusI18nLbl !== undef) {
          this.$.minusBtn.setLabel(OB.I18N.getLabel(this.buttonsDef.sideBar.minusI18nLbl));
        }
        if (this.buttonsDef.sideBar.qtyI18nLbl !== undef) {
          this.$.btnQty.setLabel(OB.I18N.getLabel(this.buttonsDef.sideBar.qtyI18nLbl));
        }
        if (this.buttonsDef.sideBar.priceI18nLbl !== undef) {
          this.$.btnPrice.setLabel(OB.I18N.getLabel(this.buttonsDef.sideBar.priceI18nLbl));
        }
        if (this.buttonsDef.sideBar.discountI18nLbl !== undef) {
          this.$.btnDiscount.setLabel(OB.I18N.getLabel(this.buttonsDef.sideBar.discountI18nLbl));
        }
      }
    }

    this.state = new Backbone.Model();

    this.$.toolbarcontainer.destroyComponents();
    this.$.keypadcontainer.destroyComponents();

    this.showSidepad('sidedisabled');

    if (this.sideBarEnabled) {
      this.$.sideenabled.show();
      this.$.sidedisabled.hide();
      this.$.ticketDiscountsToolbar.hide();
      this.$.sidecashup.hide();
    } else {
      this.$.ticketDiscountsToolbar.hide();
      this.$.sidecashup.hide();
      this.$.sideenabled.hide();
      this.$.sidedisabled.show();
    }

    //this.$.btnPrice.hide();
    console.log(this.$.btnPrice.$.button.setDisabled());

    //this.$.btnPrice.setDisabled(true);

    this.addKeypad('OB.UI.KeypadBasic');
    this.showKeypad('basic');
  },

  addToolbar: function (newToolbar) {
    var toolbar = this.$.toolbarcontainer.createComponent({
      toolbarName: newToolbar.name,
      shown: newToolbar.shown,
      keboard: this
    });
    var emptyBtn = {
      kind: 'OB.UI.BtnSide',
      btn: {}
    },
        i = 0,
        hasMore = newToolbar.buttons.length > this.maxButtons,
        displayedButtons = hasMore ? this.maxButtons - 1 : newToolbar.buttons.length,
        displayedEmptyButtons = hasMore ? 0 : this.maxButtons - newToolbar.buttons.length,
        btnDef, dialogButtons = {};

    for (i = 0; i < newToolbar.buttons.length; i++) {
      btnDef = newToolbar.buttons[i];
      if (i < displayedButtons) {
        // Send button to toolbar
        if (btnDef.command) {
          toolbar.createComponent({
            kind: 'OB.UI.BtnSide',
            btn: btnDef
          });
        } else {
          toolbar.createComponent(emptyBtn);
        }
      } else {
        // Send button to dialog.
        dialogButtons[btnDef.command] = btnDef;
        this.addCommand(btnDef.command, btnDef.definition); // It needs to register command before showing the button.
      }
      if (btnDef.initialize) {
        btnDef.initialize(this);
      }
    }

    if (hasMore) {
      // Add the button More..
      toolbar.createComponent({
        name: 'btnMore',
        keyboard: this,
        dialogButtons: dialogButtons,
        kind: 'OB.UI.ToolbarMore'
      });
    }

    // populate toolbar up to maxButtons with empty buttons
    for (i = 0; i < displayedEmptyButtons; i++) {
      toolbar.createComponent(emptyBtn);
    }

    //A toolbar could be create async, so we will hide it after creation.
    toolbar.hide();
  },

  addToolbarComponent: function (newToolbar) {
    this.$.toolbarcontainer.createComponent({
      kind: newToolbar,
      keyboard: this
    });
  },

  showToolbar: function (toolbarName) {
    this.show();
    enyo.forEach(this.$.toolbarcontainer.getComponents(), function (toolbar) {
      if (toolbar.toolbarName === toolbarName) {
        toolbar.render();
        toolbar.show();
        if (toolbar.shown) {
          toolbar.shown();
        }
      } else {
        toolbar.hide();
      }
    }, this);
  },

  addCommand: function (cmd, definition) {
    this.commands[cmd] = definition;
  },

  addButton: function (cmd, btn) {
    if (this.buttons[cmd]) {
      if (this.buttons[cmd].add) {
        this.buttons[cmd] = this.buttons[cmd].add(btn);
      }
    } else {
      this.buttons[cmd] = btn;
    }
  },

  addKeypad: function (keypad) {

    if (this.destroyed) {
      return;
    }
    var keypadconstructor = enyo.constructorForKind(keypad);
    this.activekeypads.push({
      name: keypadconstructor.prototype.padName,
      payment: keypadconstructor.prototype.padPayment || '',
      label: keypadconstructor.prototype.label || '',
      active: true
    });
    if (keypadconstructor.prototype.padPayment) {
      this.namedkeypads[keypadconstructor.prototype.padPayment] = keypadconstructor.prototype.padName;
    }

    this.$.keypadcontainer.createComponent({
      kind: keypad,
      keyboard: this
    }).render().hide();
  },

  getActiveKeypads: function () {
    return _.filter(this.activekeypads, function (keypad) {
      return keypad.active;
    });
  },

  showKeypad: function (keypadName) {
    var keypad, firstLabel = null,
        foundLabel = false,
        existsLabel = false;

    enyo.forEach(this.$.keypadcontainer.getComponents(), function (pad) {
      if (!firstLabel) {
        firstLabel = pad.label;
      } else if (foundLabel) {
        this.state.set('keypadNextLabel', this.state.get('keypadName') !== 'basic' ? this.$.keypadcontainer.children[0].label : this.searchLabelPad(this.namedkeypads[this.payment || this.status]));
        foundLabel = false;
      }
      if (pad.padName === keypadName) {
        this.state.set('keypadName', keypadName);
        foundLabel = true;
        existsLabel = true;
        pad.show();
        // Set the right payment status. If needed.
        keypad = _.find(this.activekeypads, function (keypad) {
          return keypad.active && keypad.payment === pad.padPayment;
        });
        if (keypad && pad.padPayment && (this.payment || this.status) !== pad.padPayment) {
          this.setStatus(pad.padPayment);
        }
      } else {
        pad.hide();
      }
    }, this);
    if (foundLabel) {
      this.state.set('keypadNextLabel', firstLabel);
    }

    // if keypadName does not exists show the 'basic' panel that always exists
    if (!existsLabel) {
      this.showKeypad('basic');
    }
  },

  showNextKeypad: function () {
    var i, max, keypads, current = this.state.get('keypadName');

    if (current !== 'basic') {
      this.showKeypad('basic');
    } else {
      keypads = this.getActiveKeypads();
      for (i = 0, max = keypads.length; i < max; i++) {
        if (keypads.length > 1 && keypads[i].name === current) {
          if (this.namedkeypads[this.payment || this.status] === keypads[1].name && keypads.length > 2) {
            this.showKeypad(i < keypads.length - 1 ? keypads[i + 2].name : keypads[0].name);
            break;
          } else {
            this.showKeypad(i < keypads.length - 1 ? keypads[i + 1].name : keypads[0].name);
            break;
          }
        }
      }
    }
  },

  searchLabelPad: function (status) {
    var i, keypads;
    if (!_.isUndefined(status)) {
      keypads = this.getActiveKeypads();
      if (keypads.length > 2) {
        for (i = 0; i < keypads.length; i++) {
          if (keypads[i].name === status) {
            return i < keypads.length - 1 ? this.$.keypadcontainer.children[i + 1].label : this.$.keypadcontainer.children[1].label;
          }
        }
      } else {
        return this.$.keypadcontainer.children[1].label;
      }
    } else {
      return this.$.keypadcontainer.children[1].label;
    }
  },

  showSidepad: function (sidepadname) {
    this.$.sideenabled.hide();
    this.$.sidedisabled.hide();
    this.$.ticketDiscountsToolbar.hide();
    this.$.sidecashup.hide();
    this.$[sidepadname].show();
  }
});

enyo.kind({
  name: 'OB.UI.BtnSide',
  style: 'display:table; width:100%',
  handlers: {
    onGlobalKeypress: 'changeScanningMode'
  },
  changeScanningMode: function () {
    if (this.btn.command === 'code') {
      OB.MobileApp.scanning = this.children[0].children[0].hasClass(this.btn.classButtonActive);
    }
  },
  initComponents: function () {
    this.createComponent({
      kind: 'OB.UI.ButtonKey',
      name: this.btn.command,
      label: this.btn.i18nLabel ? OB.I18N.getLabel(this.btn.i18nLabel) : this.btn.label,
      command: this.btn.command,
      permission: this.btn.permission,
      definition: this.btn.definition,
      classButtonActive: this.btn.classButtonActive || 'btnactive-green'
    });
  }
});

/**
 * Abstract class to handle barcode scan, findProductByBarcode and addProductToReceipt
 * methods need to be implemented
 */
enyo.kind({
  name: 'OB.UI.AbstractBarcodeActionHandler',
  kind: enyo.Object,
  action: function (keyboard, txt) {
    var me = this,
        attrs = {};

    var isNewCode = function (code) {
        if (this.scanningCode === code) {
          return false;
        } else {
          this.scanningCode = code;
          return true;
        }
        };

    var launchFindProduct = function (keyboard, isFindTime, isTimeoutEvent) {
        if (isFindTime) {
          OB.debug('Find launched: ' + JSON.stringify(me.productsScanned));
          var oldCode, code, launchAddProduct = function (code, keyboard, attrs) {
              me.findProductByBarcode(code, function (product) {
                me.addProductToReceipt(keyboard, product, attrs);
              }, keyboard, attrs);
              };
          for (code in me.productsScanned) {
            if (me.productsScanned.hasOwnProperty(code)) {
              var unitsToAdd = me.productsScanned[code];
              if (unitsToAdd > 0) {
                attrs = {
                  unitsToAdd: unitsToAdd
                };
                launchAddProduct(code, keyboard, attrs);
                if (code !== this.scanningCode) {
                  oldCode = code;
                } else {
                  me.productsScanned[code] = 0;
                }
              }
            }
          }

          // Delete the oldCode and leave only the new code
          if (!OB.UTIL.isNullOrUndefined(oldCode)) {
            delete me.productsScanned[oldCode];
          }
          if (!OB.UTIL.isNullOrUndefined(isTimeoutEvent) && isTimeoutEvent) {
            delete me.productsScanned;
            this.scanningCode = undefined;
          }
        }
        OB.debug('List of codes: ' + JSON.stringify(me.productsScanned) + ' / Actual code: ' + this.scanningCode);
        };

    if (OB.UTIL.isNullOrUndefined(this.productsScanned)) {
      this.productsScanned = {};
    }
    if (OB.UTIL.isNullOrUndefined(this.productsScanned[txt])) {
      this.productsScanned[txt] = 1;
    } else {
      this.productsScanned[txt] = this.productsScanned[txt] + 1;
    }

    launchFindProduct(keyboard, isNewCode(txt));
    var timeoutBarcodeId;
    timeoutBarcodeId = setTimeout(function () {
      if (me.timeoutBarcodeId === timeoutBarcodeId) {
        OB.debug('Timeout reached: ' + JSON.stringify(me.productsScanned));
        launchFindProduct(keyboard, true, true);
      }
    }, 500);
    this.timeoutBarcodeId = timeoutBarcodeId;
  }
});

enyo.kind({
  name: 'OB.UI.ToolbarMore',
  style: 'display:table; width:100%;',
  handlers: {
    onStatusChanged: 'statusChanged'
  },
  components: [{
    style: 'margin: 5px;',
    components: [{
      kind: 'OB.UI.Button',
      classes: 'btnkeyboard',
      name: 'btn',
      label: ''
    }]
  }],
  initComponents: function () {
    this.inherited(arguments);
    this.$.btn.setContent(OB.I18N.getLabel('OBMOBC_More'));
    this.activegreen = false;
  },
  tap: function () {
    if (this.activegreen) {
      this.keyboard.setStatus('');
    } else {
      this.keyboard.$.OB_UI_MoreButtons.showMoreButtons(this.dialogButtons);
    }
  },
  statusChanged: function (inSender, inEvent) {
    var status = inEvent.status;

    // Hide the More actions dialog if visible.
    if (this.keyboard.$.OB_UI_MoreButtons.hasNode()) {
      this.keyboard.$.OB_UI_MoreButtons.hide();
    }

    if (this.activegreen) {
      this.$.btn.setContent(OB.I18N.getLabel('OBMOBC_More'));
      this.$.btn.removeClass('btnactive-green');
      this.activegreen = false;
    }

    if (this.dialogButtons[status]) {
      this.$.btn.setContent(this.dialogButtons[status].label);
      this.$.btn.addClass('btnactive-green');
      this.activegreen = true;
    }
  }
});

enyo.kind({
  name: 'OB.UI.MoreButtons',
  kind: 'OB.UI.Modal',
  topPosition: '125px',
  i18nHeader: 'OBMOBC_MoreButtons',
  sideButtons: [],
  body: {
    classes: 'row-fluid',
    components: [{
      classes: 'span12',
      components: [{
        style: 'border-bottom: 1px solid #cccccc;',
        classes: 'row-fluid',
        components: [{
          name: 'morebuttonslist',
          classes: 'span12'
        }]
      }]
    }]
  },
  showMoreButtons: function (dialogButtons) {

    // Destroy previous buttons
    this.$.body.$.morebuttonslist.destroyComponents();

    // Create the new buttons.
    _.each(dialogButtons, function (btnDef) {
      this.$.body.$.morebuttonslist.createComponent({
        kind: 'OB.UI.BtnSide',
        btn: btnDef
      });
    }, this);

    // Render the new components created
    this.$.body.$.morebuttonslist.render();

    // Finally show the dialog.
    this.show();
  }
});