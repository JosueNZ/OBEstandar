/*
 ************************************************************************************
 * Copyright (C) 2012-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, _, window, OB */

enyo.kind({
  name: 'OB.UI.ModalProfile',
  kind: 'OB.UI.ModalAction',

  handlers: {
    onSelectRole: 'roleSelected',
    onSelectOrganization: 'orgSelected'
  },

  bodyContent: {
    style: 'background-color: #ffffff;',
    components: [{
      components: [{
        classes: 'properties-label',
        components: [{
          name: 'roleLbl',
          style: 'padding: 5px 8px 0px 0px; font-size: 15px;'
        }]
      }, {
        kind: 'OB.UI.ModalProfile.Role',
        classes: 'properties-field',
        name: 'roleList'
      }]
    }, {
      style: 'clear: both'
    },

    {
      name: 'orgSelector',
      components: [{
        classes: 'properties-label',
        components: [{
          style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
          name: 'orgLbl'
        }]
      }, {
        kind: 'OB.UI.ModalProfile.Organization',
        classes: 'properties-field',
        name: 'orgList'
      }]
    },

    {
      style: 'clear: both'
    }, {
      name: 'warehouseSelector',
      components: [{
        classes: 'properties-label',
        components: [{
          style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
          name: 'warehouseLbl'
        }]
      }, {
        components: [{
          kind: 'OB.UI.List',
          name: 'warehouseList',
          tag: 'select',
          classes: 'modal-dialog-profile-combo',
          renderEmpty: enyo.Control,
          renderLine: enyo.kind({
            kind: 'enyo.Option',
            initComponents: function () {
              this.inherited(arguments);
              this.setValue(this.model.get('id'));
              this.setContent(this.model.get('_identifier'));
              if (OB.MobileApp.model.get('context') && OB.MobileApp.model.get('context').user && OB.MobileApp.model.get('context').user.defaultWarehouse && this.model.get('id') === OB.MobileApp.model.get('context').user.defaultWarehouse) {
                this.parent.setSelected(this.parent.children.length - 1);
              }
            }
          })
        }]
      }]
    }, {
      style: 'clear: both'
    }, {
      components: [{
        classes: 'properties-label',
        components: [{
          style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
          name: 'langLbl'
        }]
      }, {
        name: 'lang',
        components: [{
          kind: 'OB.UI.List',
          name: 'langList',
          tag: 'select',
          classes: 'modal-dialog-profile-combo',
          renderEmpty: enyo.Control,
          renderLine: enyo.kind({
            kind: 'enyo.Option',
            initComponents: function () {
              this.inherited(arguments);
              this.setValue(this.model.get('id'));
              this.setContent(this.model.get('_identifier'));
              if (this.model.get('id') === this.owner.owner.owner.language) {
                this.parent.setSelected(this.parent.children.length - 1);
              }
            }
          })
        }]
      }]
    }, {
      style: 'clear: both'
    }, {
      components: [{
        classes: 'properties-label',
        components: [{
          style: 'padding: 5px 8px 0px 0px; font-size: 15px;',
          name: 'setAsDefaultLbl'
        }]
      }, {
        components: [{
          classes: 'modal-dialog-profile-checkbox',
          components: [{
            kind: 'OB.UI.CheckboxButton',
            name: 'defaultBox',
            classes: 'modal-dialog-btn-check'
          }]
        }]
      }]
    }, {
      style: 'clear: both'
    }]
  },
  bodyButtons: {
    components: [{
      style: 'clear: both'
    }, {
      kind: 'OB.UI.ProfileDialogApply'
    }, {
      kind: 'OB.UI.ProfileDialogCancel',
      name: 'profileCancelButton'
    }]
  },

  roleSelected: function (inSender, inEvent) {
    // This code can be used to replace _.filter when underscore is upgraded to 1.4.4
    //    this.selectedRoleOptions = _.findWhere(this.availableRoles, {
    //      id: inEvent.newRoleId
    //    });
    this.selectedRoleOptions = _.filter(this.availableRoles, function (role) {
      return role.id === inEvent.newRoleId;
    })[0];

    this.organizations.reset(this.selectedRoleOptions.organizationValueMap);
    if (this.$.bodyContent) {
      this.$.bodyContent.$.orgList.changeOrganization(); // force redraw of warehouse list
    }
  },

  orgSelected: function (inSender, inEvent) {
    // This code can be used to replace _.filter when underscore is upgraded to 1.4.4
    //    var whMap = _.findWhere(this.selectedRoleOptions.warehouseOrgMap, {
    //      orgId: inEvent.newOrgId
    //    }).warehouseMap;
    var whMap = _.filter(this.selectedRoleOptions.warehouseOrgMap, function (orgMap) {
      return orgMap.orgId === inEvent.newOrgId;
    })[0].warehouseMap;

    this.warehouses.reset(whMap);
  },

  initComponents: function () {
    this.inherited(arguments);
    this.setStyle('width: 625px;');
    this.roles = new Backbone.Collection();
    this.organizations = new Backbone.Collection();
    this.warehouses = new Backbone.Collection();
    this.language = null;

    this.$.bodyContent.$.roleList.setCollection(this.roles);
    this.$.bodyContent.$.orgList.setCollection(this.organizations);
    this.$.bodyContent.$.warehouseList.setCollection(this.warehouses);

    this.ctx = OB.MobileApp.model.get('context');

    // labels
    this.$.bodyContent.$.roleLbl.setContent(OB.I18N.getLabel('OBMOBC_Role'));
    this.$.bodyContent.$.orgLbl.setContent(OB.I18N.getLabel('OBMOBC_Org'));
    this.$.bodyContent.$.warehouseLbl.setContent(OB.I18N.getLabel('OBMOBC_Warehouse'));
    this.$.bodyContent.$.langLbl.setContent(OB.I18N.getLabel('OBMOBC_Language'));
    this.$.bodyContent.$.setAsDefaultLbl.setContent(OB.I18N.getLabel('OBMOBC_SetAsDefault'));

    this.setHeader(OB.I18N.getLabel('OBMOBC_ProfileDialogTitle'));
  },
  show: function (response) {
    this.inherited(arguments);

    var languages = new Backbone.Collection(),
        options;
    this.$.bodyContent.$.langList.setCollection(languages);

    this.availableRoles = response.role.roles;
    this.roles.reset(response.role.valueMap);
    this.language = response.language.value;
    languages.reset(response.language.valueMap);
    if (this.$.bodyContent) {
      this.$.bodyContent.$.roleList.setSelectedValue(response.role.value, 'id');
      this.$.bodyContent.$.langList.setSelectedValue(this.language, 'id');
      if (this.$.bodyContent.$.langList.selected < 0) {
        this.$.bodyContent.$.langList.setSelected(0);
      }
    }

    options = OB.MobileApp.model.get('profileOptions');
    if (options) {
      if (options.showOrganization) {
        this.selectedRoleOptions = _.filter(this.availableRoles, function (role) {
          return role.id === response.role.value;
        })[0];

        this.organizations.reset(this.selectedRoleOptions.organizationValueMap);
        if (this.$.bodyContent) {
          this.$.bodyContent.$.orgSelector.setShowing(true);
          this.$.bodyContent.$.orgList.setSelectedValue(OB.MobileApp.model.get('context').organization.id, 'id');
        }
      } else {
        this.$.bodyContent.$.orgSelector.setShowing(false);
      }

      if (options.showWarehouse) {
        if (this.$.bodyContent) {
          this.$.bodyContent.$.orgList.changeOrganization(); // force redraw of warehouse list
          this.$.bodyContent.$.warehouseSelector.setShowing(true);
          if (options.getCurrentWarehouse) {
            this.$.bodyContent.$.warehouseList.setSelectedValue(options.getCurrentWarehouse().id, 'id');
          } else {
            OB.warn('Developer: getCurrentWarehouse method is not implemented in Terminal Model. It should be located inside profileOptions property');
          }
        }
      } else {
        this.$.bodyContent.$.warehouseSelector.setShowing(false);
      }
    }

    // Focus will be set on a button to avoid a redraw which will make
    // the focuskeeper visible
    this.$.bodyButtons.$.profileCancelButton.focus();
  }
});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OB.UI.ProfileDialogApply',
  isActive: true,
  isDefaultAction: true,
  tap: function () {
    var ajaxRequest, rr, widgetForm = this.owner.owner.$.bodyContent.$,
        options = OB.MobileApp.model.get('profileOptions'),
        newLanguageId = widgetForm.langList.collection.at(widgetForm.langList.selected).get('id'),
        newLanguageSk = widgetForm.langList.collection.at(widgetForm.langList.selected).get('SK'),
        newRoleId = widgetForm.roleList.getValue(),
        isDefault = widgetForm.defaultBox.checked,
        actionURL = '../../org.openbravo.client.kernel?command=save&_action=org.openbravo.client.application.navigationbarcomponents.UserInfoWidgetActionHandler',
        postData = {
        language: newLanguageId,
        role: newRoleId,
        'default': isDefault
        };

    if (!this.isActive) {
      return;
    }
    this.isActive = false;

    if (options) {
      if (options.showOrganization) {
        postData.organization = widgetForm.orgList.getValue();
      }
      if (options.showWarehouse) {
        postData.warehouse = widgetForm.warehouseList.getValue();
      }
      if (isDefault) {
        postData.defaultProperties = options.defaultProperties;
      }
    }
    OB.UTIL.HookManager.executeHooks('OBMOBC_ProfileDialogApply', {
      profileDialogProp: this
    }, function (args) {
      OB.DS.allowRequests(false);
      OB.UTIL.localStorage.setItem('POSlanguageId', newLanguageId);
      OB.UTIL.localStorage.setItem('POSlanguageSk', newLanguageSk);
      ajaxRequest = new enyo.Ajax({
        url: actionURL,
        cacheBust: false,
        method: 'POST',
        handleAs: 'json',
        contentType: 'application/json;charset=utf-8',
        data: JSON.stringify(postData),
        success: function (inSender, inResponse) {
          OB.DS.allowRequests(true);
          if (inResponse.result === 'success') {
            OB.UTIL.localStorage.removeItem('cacheAvailableForUser:' + OB.MobileApp.model.get('orgUserId'));
            window.location.reload();
          } else {
            OB.UTIL.showError(inResponse.result);
          }
          this.isActive = true;
        },
        fail: function (inSender, inResponse) {
          OB.DS.allowRequests(true);
          OB.UTIL.showError(inResponse);
          this.isActive = true;
        }
      });
      rr = new OB.RR.Request({
        ajaxRequest: ajaxRequest
      });
      rr.exec(ajaxRequest.url);
    });

  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBMOBC_LblApply'));
  }

});

enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OB.UI.ProfileDialogCancel',
  tap: function () {
    this.doHideThisPopup();
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBMOBC_LblCancel'));
  }
});

enyo.kind({
  name: 'OB.UI.ModalProfile.Role',
  kind: 'OB.UI.List',
  classes: 'modal-dialog-profile-combo',
  published: {
    disabled: false
  },
  events: {
    onSelectRole: ''
  },

  handlers: {
    onchange: 'changeRole'
  },

  renderEmpty: enyo.Control,
  renderLine: enyo.kind({
    kind: 'enyo.Option',
    initComponents: function () {
      this.inherited(arguments);
      this.setValue(this.model.get('id'));
      this.setContent(this.model.get('_identifier'));
      if (OB.MobileApp.model.get('context') && OB.MobileApp.model.get('context').role && this.model.get('id') === OB.MobileApp.model.get('context').role.id) {
        this.parent.setSelected(this.parent.children.length - 1);
      }
    }
  }),

  changeRole: function (inSender, inEvent) {
    var id = this.children[this.getSelected()];
    if (OB.UTIL.isNullOrUndefined(id)) {
      return;
    }
    this.doSelectRole({
      newRoleId: id.getValue()
    });
  }
});

enyo.kind({
  name: 'OB.UI.ModalProfile.Organization',
  kind: 'OB.UI.List',
  classes: 'modal-dialog-profile-combo',

  events: {
    onSelectOrganization: ''
  },

  handlers: {
    onchange: 'changeOrganization'
  },

  renderEmpty: enyo.Control,
  renderLine: enyo.kind({
    kind: 'enyo.Option',
    initComponents: function () {
      this.inherited(arguments);
      this.setValue(this.model.get('id'));
      this.setContent(this.model.get('_identifier'));
      if (OB.MobileApp.model.get('context') && OB.MobileApp.model.get('context').organization && this.model.get('id') === OB.MobileApp.model.get('context').organization.id) {
        this.parent.setSelected(this.parent.children.length - 1);
      }
    }
  }),

  changeOrganization: function (inSender, inEvent) {
    var id = this.children[this.getSelected()];
    if (OB.UTIL.isNullOrUndefined(id)) {
      return;
    }
    this.doSelectOrganization({
      newOrgId: id.getValue()
    });
  }
});

enyo.kind({
  name: 'OB.UI.Profile.SessionInfo',
  kind: 'OB.UI.Popup',
  classes: 'modal',
  components: [{
    classes: 'widget-profile-title widget-profile-title-connection',
    name: 'connStatus'
  }, {
    style: 'height: 5px;'
  }, {
    kind: 'OB.UI.MenuAction',
    name: 'lockOption',
    allowHtml: true,
    tap: function () {
      this.owner.hide();
      OB.MobileApp.model.lock();
    }
  }, {
    style: 'height: 5px;'
  }, {
    kind: 'OB.UI.MenuAction',
    name: 'endSessionButton',
    i18nLabel: 'OBMOBC_EndSession',
    tap: function () {
      var me = this;
      if (OB.UTIL.SynchronizationHelper.isModelsSynchronizing()) {
        me.owner.hide();
        OB.UTIL.showError(OB.I18N.getLabel('OBMOBC_SynchronizationWasNotDoneYet'));
        return;
      }
      this.owner.hide();
      me.showLogoutDialog();
    },
    showLogoutDialog: function () {
      var havePendingOrders = false,
          orderModels = OB.MobileApp.model.orderList ? OB.MobileApp.model.orderList.models : [],
          i;
      for (i = 0; i < orderModels.length; i++) {
        if (orderModels[i].get('hasbeenpaid') === 'N' && orderModels[i].get('session') === OB.MobileApp.model.get('session') && orderModels[i].get('lines').length > 0) {
          havePendingOrders = true;
          break;
        }
      }
      if (havePendingOrders) {
        OB.MobileApp.view.$.dialogsContainer.$.logoutDialog.setBodyContent(OB.I18N.getLabel('OBMOBC_LogoutDialogText_PendingTicket'));
      } else {
        OB.MobileApp.view.$.dialogsContainer.$.logoutDialog.setBodyContent(OB.I18N.getLabel('OBMOBC_LogoutDialogText'));
      }
      OB.MobileApp.view.$.dialogsContainer.$.logoutDialog.show();
    }
  }, {
    style: 'height: 5px;'
  }],

  setConnectionStatus: function () {
    var connected = OB.MobileApp.model.get('connectedToERP');
    this.$.connStatus.setContent(OB.I18N.getLabel(connected ? 'OBMOBC_Online' : 'OBMOBC_Offline'));
    this.$.connStatus.addRemoveClass('onlineicon', connected);
    this.$.connStatus.addRemoveClass('offlineicon', !connected);
  },

  executeOnShow: function () {
    this.$.lockOption.setLabel(OB.I18N.getLabel('OBMOBC_LogoutDialogLock') + '<span style="padding-left:190px">0\u21B5</span>');
    this.$.endSessionButton.setLabel(OB.I18N.getLabel('OBMOBC_EndSession'));
  },

  initComponents: function () {
    this.inherited(arguments);
    this.$.lockOption.setShowing(OB.MobileApp.model.get('supportsOffline'));

    OB.MobileApp.model.on('change:connectedToERP', function () {
      this.setConnectionStatus();
    }, this);
    this.setConnectionStatus();

  }
});

enyo.kind({
  name: 'OB.UI.Profile.UserInfo',
  kind: 'OB.UI.Popup',
  classes: 'modal',
  components: [{
    name: 'userWidget',
    kind: 'OB.UI.Profile.UserWidget'
  }, {
    style: 'height: 5px;'
  }, {
    kind: 'OB.UI.MenuAction',
    i18nLabel: 'OBMOBC_Profile',
    name: 'profileButton',
    tap: function () {
      this.owner.hide(); // Manual dropdown menu closure
      var proc, profileHandler = OB.MobileApp.model.get('profileHandlerUrl') || 'org.openbravo.mobile.core.login.ProfileUtils';
      proc = new OB.DS.Process(profileHandler);

      OB.UTIL.showLoading(true);
      proc.exec({}, enyo.bind(this, function (response) {
        if (response.exception) {
          // we are offline...
          OB.UTIL.showLoading(false);
          return;
        }
        OB.UTIL.showLoading(false);
        OB.MobileApp.view.$.dialogsContainer.$.profileDialog.show(response);
      }));
    }
  }],

  show: function () {
    enyo.$.scrim.show();
    this.args = arguments;
    var me = this;
    var params = OB.MobileApp.model.get('loginUtilsParams') || {};
    params.appName = OB.MobileApp.model.get('appName');

    params.command = 'currentUserImage';

    var rr, ajaxRequest = new OB.OBPOSLogin.UI.LoginRequest({
      url: OB.MobileApp.model.get('loginUtilsUrl'),
      timeout: 10000,
      data: params,
      success: function (inSender, inResponse) {
        me.$.userWidget.setUserImg(inResponse.data[0].img);
        me.$.profileButton.setShowing(OB.MobileApp.model.hasPermission('OBMOBC_ChangeProfile'));
        me.inherited(me.args);
      },
      fail: function (inSender, inResponde) {
        me.$.userWidget.setUserImg(undefined);
        me.$.profileButton.setShowing(OB.MobileApp.model.hasPermission('OBMOBC_ChangeProfile'));
        me.inherited(me.args);
      }
    });

    rr = new OB.RR.Request({
      ajaxRequest: ajaxRequest
    });
    rr.exec(ajaxRequest.url);
  },

  initComponents: function () {
    this.inherited(arguments);
    OB.MobileApp.model.on('change:context', function () {
      var ctx = OB.MobileApp.model.get('context');
      if (!ctx) {
        return;
      }
      this.$.userWidget.setUserName(ctx.user._identifier);
      this.$.userWidget.setUserRole(ctx.role._identifier);
    }, this);
  }
});

enyo.kind({
  name: 'OB.UI.Profile.UserWidget',
  style: 'width: 100%',
  published: {
    userName: '',
    userRole: '',
    userImg: ''
  },
  components: [{
    //style: 'height: 60px; background-color: #FFF899;',
    classes: 'widget-profile-title ',
    components: [{
      style: 'float: left; width: 55px; margin: -15px 0px 0px 6px;',
      components: [{
        kind: 'OB.UI.Thumbnail',
        'default': '../org.openbravo.mobile.core/assets/img/anonymous-icon.png',
        name: 'img'
      }]
    }, {
      style: 'float: left; margin: 6px 0px 0px 0px; line-height: 150%;',
      components: [{
        components: [{
          components: [{
            tag: 'span',
            style: 'font-weight: 800; margin: 0px 0px 0px 5px;',
            name: 'username'
          }]
        }]
      }]
    }, {
      style: 'float: left; margin: 6px 0px 0px 0px; line-height: 150%;',
      components: [{
        components: [{
          components: [{
            tag: 'span',
            style: 'font-weight: 800; margin: 0px 0px 0px 5px;',
            content: '-'
          }]
        }]
      }]
    }, {
      style: 'float: left; margin: 6px 0px 0px 0px; line-height: 150%;',
      components: [{
        components: [{
          components: [{
            tag: 'span',
            style: 'font-weight: 800; margin: 0px 0px 0px 5px;',
            name: 'role'
          }]
        }]
      }]
    }]
  }],

  userNameChanged: function () {
    this.$.username.setContent(this.userName);
  },

  userRoleChanged: function () {
    this.$.role.setContent(this.userRole);
  },

  userImgChanged: function () {
    this.$.img.setImg(this.userImg);
  }
});