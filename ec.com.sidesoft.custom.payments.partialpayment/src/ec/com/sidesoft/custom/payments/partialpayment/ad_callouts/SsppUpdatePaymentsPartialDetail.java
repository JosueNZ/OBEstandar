package ec.com.sidesoft.custom.payments.partialpayment.ad_callouts;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;

import ec.com.sidesoft.custom.payments.partialpayment.SSPPPAYMENTSLINE;

public class SsppUpdatePaymentsPartialDetail extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub

    String infoConceptDetail = info.getStringParameter("inppayTo", null) == null ? "" : info
        .getStringParameter("inppayTo", null);

    String ConcatDetailPayment = "";

    if (infoConceptDetail.equals("PR")) {

      String partnerID = info.getStringParameter("inpcBpartnerId", null) == null ? "" : info
          .getStringParameter("inpcBpartnerId", null);
      String documentTypeID = info.getStringParameter("inpcDoctypePaymentId", null) == null ? ""
          : info.getStringParameter("inpcDoctypePaymentId", null);

      String strDateFrom = info.getStringParameter("inpdatefrom", null) == null ? "" : info
          .getStringParameter("inpdatefrom", null);
      String strDateTo = info.getStringParameter("inpdateto", null) == null ? "" : info
          .getStringParameter("inpdateto", null);

      String finFinancialAccountID = info.getStringParameter("inpfinFinancialAccountId", null) == null ? ""
          : info.getStringParameter("inpfinFinancialAccountId", null);

      String finPaymentMethodID = info.getStringParameter("inpfinPaymentmethodId", null) == null ? ""
          : info.getStringParameter("inpfinPaymentmethodId", null);

      String documentFrom = info.getStringParameter("inpdocumentnofrom", null) == null ? "" : info
          .getStringParameter("inpdocumentnofrom", null);
      String documentTo = info.getStringParameter("inpdocumentnoto", null) == null ? "" : info
          .getStringParameter("inpdocumentnoto", null);

      FIN_FinancialAccount finFinancialAccount = OBDal.getInstance().get(
          FIN_FinancialAccount.class, finFinancialAccountID);

      FIN_PaymentMethod finPaymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class,
          finPaymentMethodID);

      java.util.Date dateFrom = null;
      java.util.Date dateTo = null;

      dateFrom = formatDate(strDateFrom);
      dateTo = formatDate(strDateTo);

      OBCriteria<FIN_Payment> obcFinPayment = OBDal.getInstance().createCriteria(FIN_Payment.class);
      obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_ACCOUNT, finFinancialAccount));
      obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_PAYMENTMETHOD, finPaymentMethod));
      obcFinPayment.add(Restrictions.between(FIN_Payment.PROPERTY_PAYMENTDATE, dateFrom, dateTo));
      obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_RECEIPT, false));

      if (!documentFrom.equals("") || !documentTo.equals("")) {
        obcFinPayment.add(Restrictions.between(FIN_Payment.PROPERTY_DOCUMENTNO, documentFrom,
            documentTo));
      }

      if (!partnerID.equals("")) {

        BusinessPartner businessPartner = OBDal.getInstance().get(BusinessPartner.class, partnerID);
        obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_BUSINESSPARTNER, businessPartner));
      }

      if (!documentTypeID.equals("")) {
        DocumentType obddocumentType = OBDal.getInstance().get(DocumentType.class, documentTypeID);
        obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_DOCUMENTTYPE, obddocumentType));

      }
      obcFinPayment.addOrderBy(FIN_Payment.PROPERTY_DOCUMENTNO, true);

      ConcatDetailPayment = "";

      // int CountFinPayment = obcFinPayment.count();

      for (FIN_Payment colFinPayment : obcFinPayment.list()) {

        String strdocumentPayment = (colFinPayment.getDocumentNo() == null ? "" : colFinPayment
            .getDocumentNo());

        BusinessPartner businessPartnerPaymentsLine = OBDal.getInstance().get(
            BusinessPartner.class, colFinPayment.getBusinessPartner().getId());

        OBCriteria<SSPPPAYMENTSLINE> obcSsppPaymentLines = OBDal.getInstance().createCriteria(
            SSPPPAYMENTSLINE.class);
        obcSsppPaymentLines.add(Restrictions.eq(SSPPPAYMENTSLINE.PROPERTY_BUSINESSPARTNER,
            businessPartnerPaymentsLine));
        obcSsppPaymentLines.add(Restrictions.eq(SSPPPAYMENTSLINE.PROPERTY_DOCUMENTNO,
            strdocumentPayment));

        if (obcSsppPaymentLines.count() == 0) {
          ConcatDetailPayment = ConcatDetailPayment + strdocumentPayment + ":";
        }

      }

    }
    // info.addResult("inpdetail", String.valueOf(ConcatDetailPayment));
    int CountDetailPayment;
    CountDetailPayment = ConcatDetailPayment.length();
    String resultDetailPayment = "";
    if (CountDetailPayment > 0) {

      resultDetailPayment = "F:" + ConcatDetailPayment.substring(0, (CountDetailPayment - 1));
    }

    info.addResult("inpdescription", String.valueOf(resultDetailPayment));

    // info.addResult("inpdocumentnoto", ConcatDetailPayment);

  }

  protected java.util.Date formatDate(String date) {
    try {
      return new SimpleDateFormat((String) OBPropertiesProvider.getInstance()
          .getOpenbravoProperties().get(KernelConstants.DATE_FORMAT_PROPERTY)).parse(date);
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

}