/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
 OB.SSFI = OB.SSFI || {};
 OB.SSFI.AddPayment ={
  financialAccountOnChange : function (item, view, form, grid) {
    var affectedParams = [],
            fin_financial_account = form.getItem('fin_financial_account_id'),
            callback;
    OB.APRM.AddPayment.paymentMethodMulticurrency(view, form, true);
    OB.APRM.AddPayment.checkSingleActionAvailable(form);
    affectedParams.push(form.getField('c_currency_id_readonly_logic').paramId);
  
  
    callback = function (response, data, request) {
    form.getItem('payment_documentno').setValue(data.payment_documentno);
    form.getItem('payment_document_type').setValue(data.payment_document_type);
    };
  
      OB.RemoteCallManager.call('com.sidesoft.localization.ecuador.finances.actionHandler.AddPaymentDocumentNoActionHandler', {
        financialaccount: fin_financial_account.getValue()
      }, {}, callback);
  
    OB.APRM.AddPayment.recalcDisplayLogicOrReadOnlyLogic(form, view, affectedParams);
  }
}
