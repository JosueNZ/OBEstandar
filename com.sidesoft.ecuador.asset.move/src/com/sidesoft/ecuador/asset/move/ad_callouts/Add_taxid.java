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
package com.sidesoft.ecuador.asset.move.ad_callouts;

//import java.util.List;

import javax.servlet.ServletException;

//import org.hibernate.criterion.Restrictions;
//import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
//import org.openbravo.erpCommon.info.BusinessPartner;

import org.openbravo.model.common.businesspartner.BusinessPartner;
//import org.openbravo.model.common.enterprise.DocumentType;
//import org.openbravo.model.common.plm.Product;

public class Add_taxid extends SimpleCallout {
  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strcBpartnerId = info.getStringParameter("inpemSsamCBpartnerId", null);
    
    BusinessPartner docObj = OBDal.getInstance().get(BusinessPartner.class, strcBpartnerId);

    String strtaxid = docObj.getTaxID();


    info.addResult("inpemSsamTaxid", strtaxid);
  }
}
