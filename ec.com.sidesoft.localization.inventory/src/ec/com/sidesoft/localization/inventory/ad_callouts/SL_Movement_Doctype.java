package ec.com.sidesoft.localization.inventory.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;

public class SL_Movement_Doctype extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
	String strChanged = info.getStringParameter("inpLastFieldChanged", null);
    String documentTypeId = info.getStringParameter("inpemSsinDoctypeId", null);
    DocumentType docObj = OBDal.getInstance().get(DocumentType.class, documentTypeId);
    if (docObj.getDocumentSequence() != null) {
      Sequence seq = docObj.getDocumentSequence();
      if (seq.isAutoNumbering()) {
          info.addResult("inpemSsinDocumentno", "<" + (seq.getPrefix() != null ? seq.getPrefix() : "")
              + seq.getNextAssignedNumber().toString()
              + (seq.getSuffix() != null ? seq.getSuffix() : "") + ">");
        }
    }
  }
}