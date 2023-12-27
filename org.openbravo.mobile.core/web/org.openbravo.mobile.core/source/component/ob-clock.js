/*
 ************************************************************************************
 * Copyright (C) 2012-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global enyo, OB */

enyo.kind({
  name: 'OB.UI.Clock',
  tag: 'div',
  components: [{
    tag: 'div',
    classes: 'clock-time',
    name: 'clock'
  }, {
    tag: 'div',
    classes: 'clock-date',
    name: 'date'
  }],

  initComponents: function () {
    var me = this;

    var updateClock = function () {
        // execute when the heap is empty (application is idle)
        setTimeout(function () {
          var d = new Date();
          if (!me.$.clock) {
            return;
          }
          me.$.clock.setContent(OB.I18N.formatHour(d));
          me.$.date.setContent(OB.I18N.formatDate(d));
        }, 0);
        };

    this.inherited(arguments);
    updateClock();
    setInterval(updateClock, 1000);
  }
});