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
 * All portions are Copyright (C) 2001-2009 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s): Maykel Glez Hdez <mgonzalez@sidesoft.com.ec>.
 ************************************************************************
 */
//package org.openbravo.erpCommon.ad_callouts;
package com.sidesoft.localization.ecuador.withholdings.ad_callouts;

import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.servlet.ServletException;

import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BankAccount;

// the name of the class corresponds to the filename that holds it 
// hence, modules/modules/org.openbravo.howtos/src/org/openbravo/howtos/ad_callouts/ProductConstructSearchKey.java.
// The class must extend SimpleCallout
public class SSWH_BankAccount extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strBankAccount = info.getStringParameter("inpemSswhBpBankaccountId", null);// Business
                                                                                      // Partner
    // Bank Account

    // Get data from database
    BankAccount bankAccount = OBDal.getInstance().get(BankAccount.class, strBankAccount);

    // Calculate and inject result
    if (bankAccount != null) {
      String bankName = bankAccount.getBankName();
      info.addResult("inpemSswhBankName", bankName);
    }
  }

  protected Time getGMTTime(Timestamp ts) {

    Calendar calendar = Calendar.getInstance();
    int gmtMillisecondOffset = (calendar.get(Calendar.ZONE_OFFSET) + calendar
        .get(Calendar.DST_OFFSET));

    calendar.setTime(new Time(ts.getTime()));
    calendar.add(Calendar.MILLISECOND, -gmtMillisecondOffset);

    return new Time(calendar.getTime().getTime());
  }

  protected String formatDate(java.util.Date date) {
    return new SimpleDateFormat((String) OBPropertiesProvider.getInstance()
        .getOpenbravoProperties().get(KernelConstants.DATE_FORMAT_PROPERTY)).format(date);
  }
}