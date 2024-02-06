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
 * Contributor(s):  ___Fernanda_______.
 ************************************************************************
 */
//package org.openbravo.erpCommon.ad_callouts;
package com.sidesoft.hrm.payroll.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class SS_IDcompers extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("null")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String StrIsEmployee = info.getStringParameter("inpisemployee", null);
    String StrCompersId = info.getStringParameter("inpemSsprCompersId", null);

    if (StrIsEmployee.equals("Y")) {

      if (StrCompersId.equals("0")) {

        // *** OBTENGO NUMERO MAYOR
        SSIDcompersData xsqlCompers[] = SSIDcompersData.select(this);

        if (xsqlCompers != null && xsqlCompers.length > 0) {
          String StrValor = xsqlCompers[0].numMax;
          Double StrMax = Double.valueOf(StrValor) + 1.00;

          // ACTUALIZA ID COMPERS
          if (StrMax != 0) {
            info.addResult("inpemSsprCompersId", String.valueOf(StrMax));
          } else {
            info.addResult("inpemSsprCompersId", "0");
          }
        }
      }
    }
  }
}
