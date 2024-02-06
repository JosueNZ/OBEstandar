package ec.com.sidesoft.saleorder.relations.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SL_Invoice_DocType;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentType;

public class SL_Invoice_DocTypeCreditNote extends SL_Invoice_DocType {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strDocTypeTarget = info.getStringParameter("inpcDoctypetargetId", IsIDFilter.instance);

    // inpemSsorelIsreturn
    String strIsSOTrx = Utility.getContext(this, info.vars, "isSOTrx", info.getWindowId());
    if ("Y".equals(strIsSOTrx)) {
      DocumentType documentType = OBDal.getInstance().get(DocumentType.class, strDocTypeTarget);
      String inpemSsorelIsreturn = documentType.isReturn() ? "Y" : "N";
      info.addResult("inpemSsorelIsreturn", inpemSsorelIsreturn);
    }

    super.execute(info);

  }

}
