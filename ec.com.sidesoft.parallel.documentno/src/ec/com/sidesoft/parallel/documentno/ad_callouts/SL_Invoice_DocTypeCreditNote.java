package ec.com.sidesoft.parallel.documentno.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SL_Invoice_DocType;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;

public class SL_Invoice_DocTypeCreditNote extends SL_Invoice_DocType {

	@Override
	protected void execute(CalloutInfo info) throws ServletException {

		String strDocTypeTarget = info.getStringParameter("inpcDoctypetargetId", IsIDFilter.instance);

		if (strDocTypeTarget != null) {
			DocumentType documentType = OBDal.getInstance().get(DocumentType.class, strDocTypeTarget);

			if (documentType.getSpdnSequence() == null) {
				info.addResult("inpemSpdnDocumentno", "");
			} else {
				Sequence sequence = documentType.getSpdnSequence();
				String documentNo = sequence.getNextAssignedNumber().toString();
				info.addResult("inpemSpdnDocumentno", "<" + documentNo + ">");
			}

			String inpemSsorelIsreturn = documentType.isReturn() ? "Y" : "N";
			info.addResult("inpemSsorelIsreturn", inpemSsorelIsreturn);
		}

		super.execute(info);
	}
}
