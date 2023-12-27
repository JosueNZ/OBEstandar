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

//import org.openbravo.erpCommon.businessUtility.BpartnerMiscData;
//importar el xsql - en estecaso desde eldirectorio - pakete
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

import com.sidesoft.hrm.payroll.ssprleavecategory;

public class SS_LeaveCategory extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("null")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String StrLeaveCategory = info.getStringParameter("inpssprLeaveCategoryId", null);

    if (!StrLeaveCategory.isEmpty()) {

      OBCriteria<ssprleavecategory> ObjLeaveCategory = OBDal.getInstance().createCriteria(
          ssprleavecategory.class);
      ObjLeaveCategory.add(Restrictions.eq(ssprleavecategory.PROPERTY_ID, StrLeaveCategory));

      if (ObjLeaveCategory.count() > 0) {
        // configuracion vacaiones
        String StrSpecs = ObjLeaveCategory.list().get(0).getSpecs().toString();

        if (!StrSpecs.isEmpty()) {
          info.addResult("inpspecs", StrSpecs);
        } else {
          info.addResult("inpspecs", StrSpecs);
        }
      }
    }

  }

}
