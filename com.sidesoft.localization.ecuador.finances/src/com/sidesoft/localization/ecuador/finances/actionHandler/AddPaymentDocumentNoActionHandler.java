package com.sidesoft.localization.ecuador.finances.actionHandler;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.enterprise.Organization;
import com.sidesoft.localization.ecuador.finances.SsfiFinancialUser;

import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

public class AddPaymentDocumentNoActionHandler extends BaseActionHandler {

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {

    OBContext.setAdminMode(true);
    OBContext.getOBContext().getUser();
    try {
      final JSONObject jsonData = new JSONObject(data);
      JSONObject result = new JSONObject();
      String strDocNo = null;
      String strDocType = null;

      final String strfinancialAccount = jsonData.getString("financialaccount");
      if (StringUtils.isNotEmpty(strfinancialAccount)) {
        FIN_FinancialAccount financialAccount = OBDal.getInstance().get(FIN_FinancialAccount.class,
            strfinancialAccount);
        OBCriteria<SsfiFinancialUser> financialUser = OBDal.getInstance()
            .createCriteria(SsfiFinancialUser.class);
        financialUser
            .add(Restrictions.eq(SsfiFinancialUser.PROPERTY_FINANCIALACCOUNT, financialAccount));
        financialUser.add(Restrictions.eq(SsfiFinancialUser.PROPERTY_USERCONTACT,
            OBContext.getOBContext().getUser()));
        financialUser.add(Restrictions.eq(SsfiFinancialUser.PROPERTY_DEFAULT, true));
        if (financialUser.list().size() > 0) {
          SsfiFinancialUser user_doc = financialUser.list().get(0);
          DocumentType documentType = OBDal.getInstance().get(DocumentType.class,
              user_doc.getDocumentType().getId());
          Sequence seq = OBDal.getInstance().get(Sequence.class,
              documentType.getDocumentSequence().getId());
          StringBuilder nextDocNumber = new StringBuilder();
          if (seq.getPrefix() != null) {
            nextDocNumber.append(seq.getPrefix());
          }
          nextDocNumber.append(seq.getNextAssignedNumber().toString());
          if (seq.getSuffix() != null) {
            nextDocNumber.append(seq.getSuffix());
          }
          strDocNo = nextDocNumber.toString();
          strDocType = documentType.getId();
        } else {
          strDocNo = "";
          strDocType = "";

        }
      }
      result.put("payment_documentno", "<" + strDocNo + ">");
      result.put("payment_document_type", strDocType);
      return result;
    } catch (Exception e) {
      throw new OBException(e);
    }
  }
}
