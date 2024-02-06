package com.sidesoft.ecuador.asset.move.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;

public class ssam_UpdateFieldDocumentType extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub

    String documentTypeId = info.getStringParameter("inpcDoctypeId", null);
    DocumentType docObj = OBDal.getInstance().get(DocumentType.class, documentTypeId);

    if (docObj.getDocumentSequence() != null) {

      Sequence seq = docObj.getDocumentSequence();
      String seqnumber = "<" + (seq.getPrefix() != null ? seq.getPrefix() : "")
          + seq.getNextAssignedNumber().toString()
          + (seq.getSuffix() != null ? seq.getSuffix() : "") + ">";
      if (seqnumber.matches("^<.+>$")) {

        String subseqnumber = seqnumber.substring(1, seqnumber.length() - 1);

        info.addResult("inpdocumentno", subseqnumber);
      }
    }
  }

}
