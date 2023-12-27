/*
 ************************************************************************************
 * Copyright (C) 2015 - 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */


/*global OB, enyo */


enyo.kind({
  kind: 'OB.UI.ModalDialogButton',
  name: 'OB.UI.ExpirationOkButton',
  isDefaultAction: true,
  events: {
    onApplyChanges: ''
  },
  tap: function () {
    if (this.doApplyChanges()) {
      this.doHideThisPopup();
    }
  },
  initComponents: function () {
    this.inherited(arguments);
    this.setContent(OB.I18N.getLabel('OBMOBC_LblApply'));
  }
});

enyo.kind({
  name: 'OB.UI.ExpirationPassword',
  kind: 'OB.UI.ModalAction',
  i18nHeader: 'OBMOBC_PasswordChange',
  closeOnEscKey: false,
  autoDismiss: false,
  handlers: {
    onApplyChanges: 'applyChanges'
  },
  newAttributes: [{
    kind: 'OB.UI.renderTextProperty',
    type: 'password',
    name: 'password',
    i18nLabel: 'OBMOBC_LoginPasswordInput'
  }, {
    kind: 'OB.UI.renderTextProperty',
    type: 'password',
    name: 'confirmpassword',
    i18nLabel: 'OBMOBC_ConfirmPasswordInput'
  }],
  bodyContent: {
    kind: 'Scroller',
    maxHeight: '225px',
    style: 'background-color: #ffffff;',
    thumb: true,
    horizontal: 'hidden',
    components: [{
      style: 'background-color: #6cb33f;',
      content: '',
      name: 'newheader'
    }, {
      name: 'attributes'
    }]
  },
  bodyButtons: {
    components: [{
      kind: 'OB.UI.ExpirationOkButton',
      name: 'expirationOkButton'
    }, {
      kind: 'OB.UI.ModalDialogButton',
      i18nContent: 'OBMOBC_LblCancel',
      isDefaultAction: true,
      tap: function () {
        OB.MobileApp.model.set('isLoggingIn', false);
        this.owner.owner.hide();
        this.owner.owner.destroy();
        return;
      }
    }]
  },
  applyChanges: function (inSender, inEvent) {
    var inputpassword = this.propertycomponents.password;
    var inputconfirmpassword = this.propertycomponents.confirmpassword;
    var messagenewheader = this.$.bodyContent.$.newheader;
    if (inputpassword.getValue() === '') {
      messagenewheader.setContent(OB.I18N.getLabel('OBMOBC_EmptyPassword'));
      OB.MobileApp.model.set('isLoggingIn', false);
      return;
    } else if (inputpassword.getValue() !== inputconfirmpassword.getValue()) {
      inputconfirmpassword.setValue('');
      inputpassword.setValue('');
      messagenewheader.setContent(OB.I18N.getLabel('OBMOBC_DifferentPassword'));
      OB.MobileApp.model.set('isLoggingIn', false);
      return;
    }
    OB.MobileApp.model.set('isLoggingIn', false);
    OB.MobileApp.model.login(OB.MobileApp.model.get('user'), inputpassword.getValue(), OB.MobileApp.model.get('mode'), 'FORCE_RESET_PASSWORD');
    this.hide();
    this.destroy();
    return;
  },
  initComponents: function () {
    this.inherited(arguments);
    this.propertycomponents = {};
    enyo.forEach(this.newAttributes, function (natt) {
      var editline = this.$.bodyContent.$.attributes.createComponent({
        kind: 'OB.UI.PropertyEditLine',
        name: 'line_' + natt.name,
        newAttribute: natt
      });
      this.propertycomponents[natt.name] = editline.propertycomponent;
      this.propertycomponents[natt.name].propertiesDialog = this;
    }, this);
    this.$.headerCloseButton.hide();
  }
});