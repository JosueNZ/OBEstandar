/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2012-2015 Openbravo SLU
 * All Rights Reserved.
 ************************************************************************
 */

// ob-formitem-canvas

isc.defineClass('SalesStatus_Field', isc.Label);

isc.SalesStatus_Field.addProperties({
  height: 1,
  width: 100,
  initWidget: function () {
      console.log(this.record);
    if (this.record && this.record.documentStatus) {
      this.createField(this.record.documentStatus);
    }
    this.Super('initWidget', arguments);
  },

  createField: function (code) {
    var backGroundColor = '#C0C0C0',
      text, align = 'center';
    if (code === 'DR' ) {
      backGroundColor = '#00FF00';
      text = '';
    } else if (code === 'CO') {
      backGroundColor = '#FEE327';
      text = '';
    } else if (code === 'IP') {
      backGroundColor = '#FF4040';
      text = '';
    } else {
      backGroundColor = '#AFE31B';
      text = '';
    }
    this.setBackgroundColor(backGroundColor);
    this.setAlign(align);
  }
});
