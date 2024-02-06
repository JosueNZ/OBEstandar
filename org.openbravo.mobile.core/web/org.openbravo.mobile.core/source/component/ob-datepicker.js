/*
 ************************************************************************************
 * Copyright (C) 2015-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, $ */

enyo.kind({
  name: 'OB.UI.DatePickerSimple',
  kind: 'onyx.DatePicker',
  events: {
    onModified: ''
  },
  create: function () {
    this.inherited(arguments);
    this._datenull = false;
  },
  setValue: function (value) {

    var args = arguments;
    var newvalue = value;

    if (value) {
      this._datenull = false;
      this.inherited(args);
    } else {
      args[0] = new Date();
      this._datenull = true;
      this.inherited(args);
    }
    this.doModified({
      'value': newvalue
    });
  },
  getValue: function () {
    if (this._datenull) {
      return null;
    } else {
      return this.inherited(arguments);
    }
  },
  rendered: function () {
    this.inherited(arguments);
    if (this._datenull) {
      this.$.yearPickerButton.setContent(OB.I18N.getLabel('OBMOBC_Character')[3]);
      this.$.dayPickerButton.setContent(OB.I18N.getLabel('OBMOBC_Character')[3]);
      this.$.monthPickerButton.setContent(OB.I18N.getLabel('OBMOBC_Character')[3]);
    }
  }
});

enyo.kind({
  name: 'OB.UI.DatePicker',
  style: 'position: relative;',
  components: [{
    kind: 'OB.UI.DatePickerSimple',
    name: 'datepick',
    onModified: 'modifiedDate',
    style: 'float: left;'
  }, {
    name: 'resetbutton',
    classes: 'btnlink-gray btn-icon-small btn-icon-clear',
    style: 'margin: 2px 5px; width: 20px; float: left; cursor: pointer; padding: 8px;',
    ontap: 'resetTap'
  }, {
    style: 'clear: both;'
  }],
  modifiedDate: function (inSender, inEvent) {
    this.$.resetbutton.setShowing(inEvent.value);
  },
  resetTap: function (inSender, inEvent) {
    this.$.datepick.setValue(null);
  },
  setValue: function (value) {
    this.$.datepick.setValue(value);
  },
  getValue: function () {
    return this.$.datepick.getValue();
  },
  setLocale: function (locale) {
    this.$.datepick.setLocale(locale);
  },
  getLocale: function () {
    return this.$.datepick.getLocale();
  },
  initComponents: function () {
    var me = this;
    this.inherited(arguments);
    this.$.datepick.$.yearPicker.parent.setStyle("margin: 0px 5px;");
    this.$.datepick.$.monthPicker.parent.setStyle("margin: 0px 5px;");
    this.$.datepick.$.dayPicker.parent.setStyle("margin: 0px 5px;");
  }
});