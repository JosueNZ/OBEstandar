package ec.com.sidesoft.assets.changes.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;

public class UpdateDocNumber extends SimpleCallout {

  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String documentTypeId = info.getStringParameter("inpcDoctypeId", null);

    DocumentType docObj = OBDal.getInstance().get(DocumentType.class, documentTypeId);
    if (docObj.getDocumentSequence() != null) {
      Sequence seq = docObj.getDocumentSequence();
      info.addResult("inpdocumentno", "<" + (seq.getPrefix() != null ? seq.getPrefix() : "")
    		  + seq.getNextAssignedNumber().toString() + 
    		  (seq.getSuffix() != null ? seq.getSuffix() : "") + ">");
    }
  }

}
