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
 * All portions are Copyright (C) 2008-2012 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package com.sidesoft.hrm.payroll.ad_callouts;

import java.util.List;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
//import org.openbravo.erpCommon.info.BusinessPartner;

import com.sidesoft.hrm.payroll.Contract;

public class Add_contract extends SimpleCallout {
  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strcBpartnerId = info.getStringParameter("inpcBpartnerId", null);

    OBCriteria<Contract> OBcontract = OBDal.getInstance().createCriteria(Contract.class);

    OBcontract.add(Restrictions.eq(Contract.PROPERTY_BUSINESSPARTNER, strcBpartnerId));
    OBcontract.add(Restrictions.eq(Contract.PROPERTY_ACTIVE, true));
    List<Contract> OBcontractcriteria = OBcontract.list();
    String strcontractId = OBcontractcriteria.get(0).getId().toString();

    Contract contractobj = OBDal.getInstance().get(Contract.class, strcontractId);

    info.addResult("inpenddatecontract", contractobj.getEnddate());
    info.addResult("inpreasonendcontract", contractobj.getReasonendperiod());
    info.addResult("inpssprcontractid", contractobj.getId());
  }
}
