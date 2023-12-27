/*
 ************************************************************************************
 * Copyright (C) 2014-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, enyo, Backbone, _, alert */

enyo.kind({
  name: 'OB.UTIL.Approval',
  kind: 'OB.UI.ModalAction',
  statics: {
    /**
     * Static method to display the approval popup.
     *
     * When the approval is requested and checked, 'approvalChecked' event is
     * triggered in model parameter. This event has a boolean parameter 'approved'
     * that determines whether the approval was accepted or rejected.
     */
    requestApproval: function (model, approvalType, callback) {
      var dialog;
      var haspermission;

      if (model) {
        if (!model.approvedRequest || !model.checkApproval) {
          callback(true);
          return;
        }
      }

      if (_.isArray(approvalType)) {
        haspermission = _.every(approvalType, function (a) {
          return OB.MobileApp.model.hasPermission(a, true);
        });
      } else if (approvalType) {
        haspermission = OB.MobileApp.model.hasPermission(approvalType, true);
      } else {
        callback(true);
        return;
      }

      if (haspermission) {
        if (model && model.approvedRequest) {
          model.approvedRequest(true, new Backbone.Model(OB.MobileApp.model.get('context').user), approvalType, callback); // I'am a supervisor
        } else {
          callback(true, new Backbone.Model(OB.MobileApp.model.get('context').user), approvalType);
        }
      } else {
        OB.MobileApp.view.$.confirmationContainer.destroyComponents();
        dialog = OB.MobileApp.view.$.confirmationContainer.createComponent({
          kind: 'OB.UTIL.Approval',
          name: 'approval-panel',
          model: model,
          approvalType: approvalType,
          callback: callback
        });

        dialog.show();
        //This dialog can be shown when loading message is shown. In that case
        //approval has priority. Show it in the front
        if (OB.MobileApp.view.$.containerLoading.getShowing() && dialog && dialog.hasNode && dialog.hasNode() && dialog.hasNode().style && dialog.hasNode().style.zIndex) {
          dialog.hasNode().style.zIndex = 2000000;
        }
      }
    }
  },
  handlers: {
    onCheckCredentials: 'checkCredentials',
    onUserImgClick: 'handleUserImgClick'
  },

  i18nHeader: 'OBMOBC_ApprovalRequiredTitle',
  bodyContent: {
    components: [{
      name: 'explainApprovalTxt',
      kind: 'Scroller',
      maxHeight: '200px',
      style: 'text-align: left; padding-left: 0.5em;'
    }, {
      classes: 'login-header-row',
      style: 'color:black; line-height: 20px;',
      components: [{
        classes: 'span6',
        components: [{
          kind: 'Scroller',
          maxHeight: '300px',
          thumb: true,
          horizontal: 'hidden',
          name: 'loginUserContainer',
          classes: 'login-user-container',
          style: 'background-color:#5A5A5A; margin: 5px;',
          content: ['.']
        }]
      }, {
        classes: 'span6',
        components: [{
          classes: 'login-inputs-container',
          components: [{
            name: 'loginInputs',
            classes: 'login-inputs-browser-compatible',
            components: [{
              components: [{
                kind: 'OB.UTIL.Approval.Input',
                name: 'username'
              }]
            }, {
              components: [{
                kind: 'OB.UTIL.Approval.Input',
                name: 'password',
                type: 'password'
              }]
            }, {
              components: [{
                name: 'approvalWarning',
                classes: 'approval-warning',
                allowHtml: true
              }]
            }]
          }]
        }]
      }]
    }]
  },

  bodyButtons: {
    components: [{
      kind: 'OB.UTIL.Approval.ApproveButton'
    }, {
      kind: 'OB.UI.ModalDialogButton',
      i18nLabel: 'OBMOBC_LblCancel',
      tap: function () {
        this.bubble('onHideThisPopup', {
          args: {
            approvalSent: false
          }
        });
      }
    }]
  },

  executeOnHide: function () {
    if (this.args && this.args.approvalSent) {
      return;
    } else {
      if (this.model) {
        this.model.approvedRequest(false, new Backbone.Model(OB.MobileApp.model.get('context').user), this.approvalType, this.callback);
      } else {
        this.callback(false, new Backbone.Model(OB.MobileApp.model.get('context').user), this.approvalType);
      }
    }
  },

  handleUserImgClick: function (inSender, inEvent) {
    var u = inEvent.originator.user;
    this.$.bodyContent.$.username.setValue(u);
    this.$.bodyContent.$.password.setValue('');
    this.$.bodyContent.$.password.focus();
    return true;
  },

  initComponents: function () {
    var i, j;
    this.inherited(arguments);
    this.$.bodyContent.$.username.attributes.placeholder = OB.I18N.getLabel('OBMOBC_LoginUserInput');
    this.$.bodyContent.$.password.attributes.placeholder = OB.I18N.getLabel('OBMOBC_LoginPasswordInput');
    if (!Array.isArray(this.approvalType)) {
      this.approvalType = [this.approvalType];
    }
    for (i = 0; i < this.approvalType.length; i++) {
      if (this.approvalType[i] instanceof Object) {
        if (this.approvalType[i].message instanceof Object) {
          // Complex format approvalType[i] = { message: [ { message, padding, fonstSize, fontWeight, color } ]}
          for (j = 0; j < this.approvalType[i].message.length; j++) {
            this.addTextComponent(this.approvalType[i].message[j]);
          }
        } else {
          // Approval with approvalType[i] = { message, params }
          this.addTextComponent({
            message: OB.I18N.getLabel(this.approvalType[i].message, this.approvalType[i].params) || OB.I18N.getLabel('OBMOBC_ApprovalTextHeader')
          });
        }
      } else {
        // Simple approval
        this.addTextComponent({
          message: OB.I18N.labels[this.approvalType[i]] || OB.I18N.getLabel('OBMOBC_ApprovalTextHeader')
        });
      }
    }
    this.postRenderActions();
  },

  addTextComponent: function (msg) {
    var style = '';
    if (msg.padding) {
      style += 'padding-left: ' + msg.padding + ';';
    }
    if (msg.fontSize) {
      style += 'font-size: ' + msg.fontSize + ';';
    }
    if (msg.fontWeight) {
      style += 'font-weight: ' + msg.fontWeight + ';';
    }
    if (msg.color) {
      style += 'color: ' + msg.color + ';';
    }
    this.$.bodyContent.$.explainApprovalTxt.createComponent({
      style: style,
      content: msg.message,
      allowHtml: true
    });
  },

  setUserImages: function (inSender, inResponse) {
    var name = [],
        userId = [],
        userName = [],
        image = [],
        connected = [],
        me = this,
        jsonImgData, i;

    if (!inResponse.data) {
      OB.Dal.find(OB.Model.User, {}, function (users) {
        var i, user, session;
        for (i = 0; i < users.models.length; i++) {
          user = users.models[i];
          name.push(user.get('name'));
          userId.push(user.get('id'));
          userName.push(user.get('name'));
          connected.push(false);
        }
        me.renderUserButtons(name, userId, userName, image, connected);
      }, function () {
        OB.error(arguments);
      });
      return true;
    }
    jsonImgData = inResponse.data;
    var userPOS = OB.POS.modelterminal.get('context').user;
    if (jsonImgData.length > 0) {
      enyo.forEach(jsonImgData, function (v) {
        if(v.userId == userPOS.supervisor){
          name.push(v.name);
          userId.push(v.userId);
          userName.push(v.userName);
          image.push(v.image);
        }
      });
      this.renderUserButtons(name, userId, userName, image, connected);
    } else {
      this.showing = false;
      OB.UTIL.showConfirmation.display(OB.I18N.getLabel('OBMOBC_ApprovalRequiredTitle'), OB.I18N.getLabel('OBMOBC_NoSupervisorsText'), [{
        label: OB.I18N.getLabel('OBMOBC_LblOk'),
        isConfirmButton: true
      }], {
        autoDismiss: false,
        onHideFunction: function (popup) {
          if (me.model && me.model.approvedRequest) {
            me.model.approvedRequest(false, new Backbone.Model(OB.MobileApp.model.get('context').user), me.approvalType, me.callback);
          } else {
            me.callback(false, new Backbone.Model(OB.MobileApp.model.get('context').user), me.approvalType);
          }
        }
      });
    }
  },

  renderUserButtons: function (name, userId, userName, image) {
    var i, target = this.$.bodyContent.$.loginUserContainer;
    for (i = 0; i < name.length; i++) {
      target.createComponent({
        kind: 'OB.OBPOSLogin.UI.UserButton',
        user: userName[i],
        userId: userId[i],
        userImage: image[i],
        showConnectionStatus: false
      });
    }
    target.render();
    return true;
  },

  postRenderActions: function () {
    var approvalList = [],
        me = this;
    this.approvalType.forEach(function (approvalType) {
      approvalList.push(typeof (approvalType) === 'object' ? approvalType.approval : approvalType);
    });

    var params = OB.MobileApp.model.get('loginUtilsParams') || {};
    params.appName = OB.MobileApp.model.get('appName');

    params.command = 'userImages';
    params.approvalType = JSON.stringify(approvalList);

    var rr, ajaxRequest = new OB.OBPOSLogin.UI.LoginRequest({
      url: OB.MobileApp.model.get('loginUtilsUrl'),
      data: params,
      success: function (inSender, inResponse) {
        me.setUserImages(inSender, inResponse);
      }
    });

    rr = new OB.RR.Request({
      ajaxRequest: ajaxRequest
    });
    rr.exec(ajaxRequest.url);
  },

  checkCredentials: function () {
    var u = this.$.bodyContent.$.username.getValue(),
        p = this.$.bodyContent.$.password.getValue(),
        emptyUserPassword = (!u || !p),
        me = this;

    this.$.bodyContent.$.approvalWarning.setContent('');
    if (emptyUserPassword) {
      this.$.bodyContent.$.approvalWarning.setContent(OB.I18N.getLabel('OBMOBC_EmptyUserPassword'));
      return;
    }

    OB.UTIL.checkApproval(this.approvalType, u, p, function (approved, supervisor, approvalType, continueCallback, warningMsg) {
      if (!OB.UTIL.isNullOrUndefined(warningMsg)) {
        me.$.bodyContent.$.approvalWarning.setContent(warningMsg);
        return;
      }

      /*******************************************************************/
      /*                    INICIO VERIFICACIONES                        */
      /*******************************************************************/
      var userPOS = OB.POS.modelterminal.get('context').user;
      var process = new OB.DS.Process('ec.com.sidesoft.retail.reverse.authorization.verifyPasswordSupervisor');
      process.exec({
        messageId: OB.UTIL.get_UUID(), 
        supervisorid: userPOS.supervisor,
        timeout: 300000
       }, function(data) {
        if(data.exception.status.tipo == 0){
          
          if (continueCallback) {
            if (me.model && me.model.approvedRequest) {
              me.model.approvedRequest(approved, supervisor, approvalType, me.callback);
            } else {
              me.callback(approved, supervisor, approvalType);
            }
          }
          me.waterfall('onHideThisPopup', {
            args: {
              approvalSent: true
            }
          });          

        }else if(data.exception.status.tipo == 1){
          me.$.bodyContent.$.approvalWarning.setContent(OB.I18N.getLabel('SRRATH_ExpiredTime'));
        }else if(data.exception.status.tipo == 2){
          me.$.bodyContent.$.approvalWarning.setContent(OB.I18N.getLabel('SRRATH_PasswordUsed'));
        }
      });          
      /*******************************************************************/
      /*           FIN ENVIO DEL CORREO ACTUALIZACION PASSWORD           */
      /*******************************************************************/ 

    }, this.model);
  }
});

enyo.kind({
  name: 'OB.UTIL.Approval.ApproveButton',
  kind: 'OB.UI.ModalDialogButton',
  i18nLabel: 'OBMOBC_Approve',
  events: {
    onCheckCredentials: ''
  },
  tap: function () {
    this.doCheckCredentials();
  }
});

enyo.kind({
  name: 'OB.UTIL.Approval.Input',
  kind: 'enyo.Input',
  type: 'text',
  classes: 'input-login',
  handlers: {
    onkeydown: 'inputKeydownHandler'
  },
  events: {
    onCheckCredentials: ''
  },
  inputKeydownHandler: function (inSender, inEvent) {
    var keyCode = inEvent.keyCode;
    if (keyCode === 13) { //Handle ENTER key
      this.doCheckCredentials();
      return true;
    }
    return false;
  }
});