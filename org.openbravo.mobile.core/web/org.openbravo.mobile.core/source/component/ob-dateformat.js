/*
 ************************************************************************************
 * Copyright (C) 2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, Backbone, */


// This definition overwrites the enyo component 'enyo.g11n.Fmts' used for example in onyx.DatePicker
// This way localization is defined in Openbravo
enyo.kind({
  name: 'enyo.g11n.Fmts',

  getMonthFields: function () {
    return OB.I18N.getMonthsShortList();
  },

  getDateFieldOrder: function () {
    var enyoDate = '',
        dateformat = OB.Format.date.toLowerCase(),
        i, targetChars = 'dmy';
    for (i = 0; i < dateformat.length; i++) {
      if (enyoDate.indexOf(dateformat[i]) < 0 && targetChars.indexOf(dateformat[i]) >= 0) {
        enyoDate = enyoDate + dateformat[i];
      }
    }
    return enyoDate;
  },

  getTimeFieldOrder: function () {
    return 'hma';
  }
});