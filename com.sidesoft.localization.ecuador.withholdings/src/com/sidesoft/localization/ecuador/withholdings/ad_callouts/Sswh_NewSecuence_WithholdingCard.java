package com.sidesoft.localization.ecuador.withholdings.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;

public class Sswh_NewSecuence_WithholdingCard extends SimpleCallout {

  /**
           * 
           */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    // inpcDoctypeId
    // inpnoCertificate

    String documentTypeId = info.getStringParameter("inpcDoctypeId", null);
    DocumentType docObj = OBDal.getInstance().get(DocumentType.class, documentTypeId);

    if (docObj.getDocumentSequence() != null) {

      Sequence seq = docObj.getDocumentSequence();

      String seqnumber = "<" + (seq.getPrefix() != null ? seq.getPrefix() : "")
          + seq.getNextAssignedNumber().toString()
          + (seq.getSuffix() != null ? seq.getSuffix() : "") + ">";
      info.addResult("inpdocumentno", seqnumber);

    }
  }

}
