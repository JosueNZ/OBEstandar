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
package ec.com.sidesoft.payroll.overtime.ad_callouts;

import java.sql.Timestamp;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;

public class Calculategeneratehours extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    
    String StartDate = info.getStringParameter("inpauthorizedTime", null);
    String Enddate = info.getStringParameter("inpexitShift2", null);
    
    String documentTypeId = null;
    DocumentType docObj = OBDal.getInstance().get(DocumentType.class, documentTypeId);
    if (docObj.getDocumentSequence() != null) {
      Sequence seq = docObj.getDocumentSequence();
 

      info.addResult("inpdocumentnonew", "<" + (seq.getPrefix() != null ? seq.getPrefix() : "")
          + seq.getNextAssignedNumber().toString()
          + (seq.getSuffix() != null ? seq.getSuffix() : "") + ">");
    }
  }
}
