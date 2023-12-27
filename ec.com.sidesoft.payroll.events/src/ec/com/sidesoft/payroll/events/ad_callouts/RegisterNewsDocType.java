package ec.com.sidesoft.payroll.events.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;

public class RegisterNewsDocType extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String documentTypeId = info.getStringParameter("inpcDoctypetargetId", null);
    DocumentType docObj = OBDal.getInstance().get(DocumentType.class, documentTypeId);
    Sequence seq = docObj.getDocumentSequence();
    info.addResult("inpdoumentno", "<" + seq.getNextAssignedNumber().toString() + ">");

  }

}
