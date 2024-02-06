package com.sidesoft.localization.ecuador.finances.ad_process;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.APRM_FinaccTransactionV;
import org.openbravo.advpaymentmngt.ProcessInvoiceHook;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.data.FieldProvider;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonUtility;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.ReversedInvoice;
import org.openbravo.model.financialmgmt.accounting.FIN_FinancialAccountAccounting;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetailV;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ProcessInvoices extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  private List<FIN_Payment> creditPayments = new ArrayList<FIN_Payment>();
  private final AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
  private static final String Purchase_Invoice_Window = "183";

  @Inject
  @Any
  private Instance<ProcessInvoiceHook> hooks;

  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      final String strWindowId = vars.getGlobalVariable("inpwindowId", "ProcessInvoice|Window_ID",
          IsIDFilter.instance);
      final String strTabId = vars.getGlobalVariable("inpTabId", "ProcessInvoice|Tab_ID",
          IsIDFilter.instance);

      final String strC_Invoice_ID = vars.getGlobalVariable("inpcInvoiceId",
          strWindowId + "|C_Invoice_ID", "", IsIDFilter.instance);

      final String strdocaction = vars.getStringParameter("inpdocaction");
      final String strProcessing = vars.getStringParameter("inpprocessing", "Y");
      final String strOrg = vars.getRequestGlobalVariable("inpadOrgId", "ProcessInvoice|Org_ID",
          IsIDFilter.instance);
      final String strClient = vars.getStringParameter("inpadClientId", IsIDFilter.instance);

      final String strdocstatus = vars.getRequiredStringParameter("inpdocstatus");
      final String stradTableId = "318";
      final int accesslevel = 1;

      if ((org.openbravo.erpCommon.utility.WindowAccessData.hasReadOnlyAccess(this, vars.getRole(),
          strTabId))
          || !(Utility.isElementInList(
              Utility.getContext(this, vars, "#User_Client", strWindowId, accesslevel), strClient)
              && Utility.isElementInList(
                  Utility.getContext(this, vars, "#User_Org", strWindowId, accesslevel), strOrg))) {
        OBError myError = Utility.translateError(this, vars, vars.getLanguage(),
            Utility.messageBD(this, "NoWriteAccess", vars.getLanguage()));
        vars.setMessage(strTabId, myError);
        printPageClosePopUp(response, vars);
      } else {
        printPageDocAction(response, vars, strC_Invoice_ID, strdocaction, strProcessing,
            strdocstatus, stradTableId, strWindowId);
      }
    } else if (vars.commandIn("SAVE_BUTTONDocAction111")) {
      final String strWindowId = vars.getGlobalVariable("inpwindowId", "ProcessInvoice|Window_ID",
          IsIDFilter.instance);
      final String strTabId = vars.getGlobalVariable("inpTabId", "ProcessInvoice|Tab_ID",
          IsIDFilter.instance);
      final String strC_Invoice_ID = vars.getGlobalVariable("inpKey", strWindowId + "|C_Invoice_ID",
          "");
      final String strdocaction = vars.getStringParameter("inpdocaction");
      final String strVoidInvoiceDate = vars.getStringParameter("inpVoidedDocumentDate");
      final String strVoidInvoiceAcctDate = vars.getStringParameter("inpVoidedDocumentAcctDate");
      final String strOrg = vars.getGlobalVariable("inpadOrgId", "ProcessInvoice|Org_ID",
          IsIDFilter.instance);

      // 1466 - Validaciones para anular una factura

      OBError myMessageVoided = null;
      try {
        if (strdocaction.equals("RC")) {
          int resCountPay = 0;
          resCountPay = getPaymentCount(this, strC_Invoice_ID);
          if (resCountPay > 0) {

            myMessageVoided = Utility.translateError(this, vars, vars.getLanguage(),
                "@Ssfi_ErrorVoidInvoice@");
            vars.setMessage(strTabId, myMessageVoided);
            String strWindowPath = Utility.getTabURL(strTabId, "R", true);
            if (strWindowPath.equals(""))
              strWindowPath = strDefaultServlet;
            printPageClosePopUp(response, vars, strWindowPath);
            OBDal.getInstance().rollbackAndClose();
            return;

          }

          String strTypeTransaction = "";
          strTypeTransaction = getTypeTransaction(this, strC_Invoice_ID) != null
              ? getTypeTransaction(this, strC_Invoice_ID)
              : "ND";

          if (strTypeTransaction.equals("Y")) {
            int resWithhSalesCount = 0;
            resWithhSalesCount = getWithhSalesCount(this, strC_Invoice_ID);
            if (resWithhSalesCount > 0) {

              myMessageVoided = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_ErrorWhithSalesInvoice@");
              vars.setMessage(strTabId, myMessageVoided);
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              OBDal.getInstance().rollbackAndClose();
              return;

            }
          }

        }

        // String strMsg = rollBackInvoice(this, strC_Invoice_ID);
        // blDoctypePament = false;

      } catch (ServletException ex) {
        myMessageVoided = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
        if (!myMessageVoided.isConnectionAvailable()) {
          bdErrorConnection(response);
          return;
        } else
          vars.setMessage(strTabId, myMessageVoided);
      }

      // 1466 - Fin Validaciones para anular una factura

      OBError myMessage = null;
      try {

        Invoice invoice = dao.getObject(Invoice.class, strC_Invoice_ID);
        invoice.setDocumentAction(strdocaction);

        OBDal.getInstance().save(invoice);
        OBDal.getInstance().flush();

        OBError msg = null;
        for (ProcessInvoiceHook hook : hooks) {
          msg = hook.preProcess(invoice, strdocaction);
          if (msg != null && "Error".equals(msg.getType())) {
            vars.setMessage(strTabId, msg);
            String strWindowPath = Utility.getTabURL(strTabId, "R", true);
            if (strWindowPath.equals(""))
              strWindowPath = strDefaultServlet;
            printPageClosePopUp(response, vars, strWindowPath);
            return;
          }
        }
        // check BP currency
        if ("CO".equals(strdocaction)) {
          // check BP currency
          if (invoice.getBusinessPartner().getCurrency() == null) {
            String errorMSG = Utility.messageBD(this, "InitBPCurrencyLnk", vars.getLanguage(),
                false);
            msg = new OBError();
            msg.setType("Error");
            msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
            msg.setMessage(String.format(errorMSG, invoice.getBusinessPartner().getId(),
                invoice.getBusinessPartner().getName()));

            vars.setMessage(strTabId, msg);
            printPageClosePopUp(response, vars, Utility.getTabURL(strTabId, "R", true));
            return;
          }
        }

        OBContext.setAdminMode(true);
        Process process = null;
        try {
          process = dao.getObject(Process.class, "111");
        } finally {
          OBContext.restorePreviousMode();
        }

        Date voidDate = null;
        Date voidAcctDate = null;
        Map<String, String> parameters = null;
        if (!strVoidInvoiceDate.isEmpty() && !strVoidInvoiceAcctDate.isEmpty()) {
          try {
            voidDate = OBDateUtils.getDate(strVoidInvoiceDate);
            voidAcctDate = OBDateUtils.getDate(strVoidInvoiceAcctDate);
          } catch (ParseException pe) {
            voidDate = new Date();
            voidAcctDate = new Date();
            log4j.error("Not possible to parse the following date: " + strVoidInvoiceDate, pe);
            log4j.error("Not possible to parse the following date: " + strVoidInvoiceAcctDate, pe);
          }
          parameters = new HashMap<String, String>();
          parameters.put("voidedDocumentDate", OBDateUtils.formatDate(voidDate, "yyyy-MM-dd"));
          parameters.put("voidedDocumentAcctDate",
              OBDateUtils.formatDate(voidAcctDate, "yyyy-MM-dd"));

        }

        // In case of void a non paid invoice, create a dummy payment related to it with zero amount
        FIN_Payment dummyPayment = null;
        if ("RC".equals(strdocaction) && !invoice.isPaymentComplete()
            && invoice.getTotalPaid().compareTo(BigDecimal.ZERO) == 0) {
          try {
            OBContext.setAdminMode(true);
            final boolean isSOTrx = invoice.isSalesTransaction();
            final DocumentType docType = FIN_Utility.getDocumentType(invoice.getOrganization(),
                isSOTrx ? AcctServer.DOCTYPE_ARReceipt : AcctServer.DOCTYPE_APPayment);
            final String strPaymentDocumentNo = FIN_Utility.getDocumentNo(docType,
                docType.getTable() != null ? docType.getTable().getDBTableName() : "");

            // Get default Financial Account as it is done in Add Payment
            FIN_FinancialAccount bpFinAccount = null;
            if (isSOTrx && invoice.getBusinessPartner().getAccount() != null
                && FIN_Utility.getFinancialAccountPaymentMethod(invoice.getPaymentMethod().getId(),
                    invoice.getBusinessPartner().getAccount().getId(), isSOTrx,
                    invoice.getCurrency().getId(), invoice.getOrganization().getId()) != null) {
              bpFinAccount = invoice.getBusinessPartner().getAccount();
            } else if (!isSOTrx && invoice.getBusinessPartner().getPOFinancialAccount() != null
                && FIN_Utility.getFinancialAccountPaymentMethod(invoice.getPaymentMethod().getId(),
                    invoice.getBusinessPartner().getPOFinancialAccount().getId(), isSOTrx,
                    invoice.getCurrency().getId(), invoice.getOrganization().getId()) != null) {
              bpFinAccount = invoice.getBusinessPartner().getPOFinancialAccount();
            } else {
              FinAccPaymentMethod fpm = FIN_Utility.getFinancialAccountPaymentMethod(
                  invoice.getPaymentMethod().getId(), null, isSOTrx, invoice.getCurrency().getId(),
                  invoice.getOrganization().getId());
              if (fpm != null) {
                bpFinAccount = fpm.getAccount();
              }
            }

            // If no Financial Account exists, show an Error
            if (bpFinAccount == null) {
              msg = new OBError();
              msg.setType("Error");
              msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
              msg.setMessage(OBMessageUtils.messageBD("APRM_NoFinancialAccountAvailable"));
              vars.setMessage(strTabId, msg);
              printPageClosePopUp(response, vars, Utility.getTabURL(strTabId, "R", true));
              return;
            }

            // If Invoice has a awaiting execution payment related, show an Error
            List<FIN_PaymentSchedule> psl = invoice.getFINPaymentScheduleList();
            for (FIN_PaymentSchedule ps : psl) {
              List<FIN_PaymentScheduleDetail> psdl = ps
                  .getFINPaymentScheduleDetailInvoicePaymentScheduleList();
              for (FIN_PaymentScheduleDetail psd : psdl) {
                FIN_PaymentDetail pd = psd.getPaymentDetails();
                if (pd != null && (pd.getFinPayment().getStatus().equals("RPAE")
                    || pd.getFinPayment().getStatus().equals("RPAP"))) {
                  msg = new OBError();
                  msg.setType("Error");
                  msg.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
                  msg.setMessage(
                      OBMessageUtils.messageBD("APRM_InvoiceAwaitingExcutionPaymentRelated"));
                  vars.setMessage(strTabId, msg);
                  printPageClosePopUp(response, vars, Utility.getTabURL(strTabId, "R", true));
                  return;
                }
              }
            }

            // Reversed invoice's date: voidDate in Purchase Invoice, new Date() in Sales Invoice
            Date reversedDate = voidDate != null ? voidDate : new Date();

            // Calculate Conversion Rate
            BigDecimal rate = null;
            if (!StringUtils.equals(invoice.getCurrency().getId(),
                bpFinAccount.getCurrency().getId())) {
              final ConversionRate conversionRate = FinancialUtils.getConversionRate(reversedDate,
                  invoice.getCurrency(), bpFinAccount.getCurrency(), invoice.getOrganization(),
                  invoice.getClient());
              if (conversionRate != null) {
                rate = conversionRate.getMultipleRateBy();
              }
            }

            // Create dummy payment
            dummyPayment = dao.getNewPayment(isSOTrx, invoice.getOrganization(), docType,
                strPaymentDocumentNo, invoice.getBusinessPartner(), invoice.getPaymentMethod(),
                bpFinAccount, "0", reversedDate, invoice.getDocumentNo(), invoice.getCurrency(),
                rate, null);
            OBDal.getInstance().save(dummyPayment);

            List<FIN_PaymentDetail> paymentDetails = new ArrayList<FIN_PaymentDetail>();
            List<FIN_PaymentScheduleDetail> paymentScheduleDetails = dao
                .getInvoicePendingScheduledPaymentDetails(invoice);
            for (FIN_PaymentScheduleDetail psd : paymentScheduleDetails) {
              FIN_PaymentDetail pd = OBProvider.getInstance().get(FIN_PaymentDetail.class);
              pd.setOrganization(psd.getOrganization());
              pd.setFinPayment(dummyPayment);
              pd.setAmount(psd.getAmount());
              pd.setRefund(false);
              OBDal.getInstance().save(pd);

              paymentDetails.add(pd);
              psd.setPaymentDetails(pd);
              pd.getFINPaymentScheduleDetailList().add(psd);
              OBDal.getInstance().save(psd);
            }
            dummyPayment.setFINPaymentDetailList(paymentDetails);

            // Copy exchange rate from invoice
            for (ConversionRateDoc conversionRateDoc : invoice.getCurrencyConversionRateDocList()) {
              ConversionRateDoc newConversionRateDoc = OBProvider.getInstance()
                  .get(ConversionRateDoc.class);
              newConversionRateDoc.setClient(conversionRateDoc.getClient());
              newConversionRateDoc.setOrganization(conversionRateDoc.getOrganization());
              newConversionRateDoc.setCurrency(conversionRateDoc.getCurrency());
              newConversionRateDoc.setToCurrency(conversionRateDoc.getToCurrency());
              newConversionRateDoc.setRate(conversionRateDoc.getRate());
              newConversionRateDoc.setForeignAmount(BigDecimal.ZERO);
              newConversionRateDoc.setPayment(dummyPayment);
              dummyPayment.getCurrencyConversionRateDocList().add(newConversionRateDoc);
              OBDal.getInstance().save(newConversionRateDoc);
            }

            OBDal.getInstance().save(dummyPayment);
          } catch (final Exception e) {
            log4j.error(
                "Exception while creating dummy payment for the invoice: " + strC_Invoice_ID);
            e.printStackTrace();
          } finally {
            OBContext.restorePreviousMode();
          }
        }

        final ProcessInstance pinstance = CallProcess.getInstance().call(process, strC_Invoice_ID,
            parameters);

        OBDal.getInstance().getSession().refresh(invoice);
        invoice.setAPRMProcessinvoice(invoice.getDocumentAction());

        if ("RC".equals(strdocaction) && pinstance.getResult() != 0L) {
          try {
            OBContext.setAdminMode(true);

            // Get reversed payment
            OBCriteria<ReversedInvoice> revInvoiceCriteria = OBDal.getInstance()
                .createCriteria(ReversedInvoice.class);
            revInvoiceCriteria
                .add(Restrictions.eq(ReversedInvoice.PROPERTY_REVERSEDINVOICE, invoice));
            revInvoiceCriteria.setMaxResults(1);
            ReversedInvoice revInvoice = (ReversedInvoice) revInvoiceCriteria.uniqueResult();

            if (revInvoice != null && dummyPayment != null) {

              List<FIN_PaymentDetail> paymentDetails = new ArrayList<FIN_PaymentDetail>();
              List<FIN_PaymentScheduleDetail> paymentScheduleDetails = dao
                  .getInvoicePendingScheduledPaymentDetails(revInvoice.getInvoice());
              for (FIN_PaymentScheduleDetail psd : paymentScheduleDetails) {
                FIN_PaymentDetail pd = OBProvider.getInstance().get(FIN_PaymentDetail.class);
                pd.setOrganization(psd.getOrganization());
                pd.setFinPayment(dummyPayment);
                pd.setAmount(psd.getAmount());
                pd.setRefund(false);
                OBDal.getInstance().save(pd);

                paymentDetails.add(pd);
                psd.setPaymentDetails(pd);
                pd.getFINPaymentScheduleDetailList().add(psd);
                OBDal.getInstance().save(psd);
              }
              dummyPayment.getFINPaymentDetailList().addAll(paymentDetails);
              OBDal.getInstance().save(dummyPayment);

              // Process dummy payment related with both actual invoice and reversed invoice
              OBError message = FIN_AddPayment.processPayment(vars, this, "P", dummyPayment);
              if ("Error".equals(message.getType())) {
                message.setMessage(
                    OBMessageUtils.messageBD("PaymentError") + " " + message.getMessage());
                vars.setMessage(strTabId, message);
                String strWindowPath = Utility.getTabURL(strTabId, "R", true);
                if (strWindowPath.equals(""))
                  strWindowPath = strDefaultServlet;
                printPageClosePopUp(response, vars, strWindowPath);
                return;
              }
            }
          } catch (final Exception e) {
            log4j.error(
                "Exception while creating dummy payment for the invoice: " + strC_Invoice_ID);
            e.printStackTrace();
          } finally {
            OBContext.restorePreviousMode();
          }
        }

        // Remove invoice's used credit description
        if ("RE".equals(strdocaction) && pinstance.getResult() != 0L) {
          final String invDesc = invoice.getDescription();
          if (invDesc != null) {
            final String creditMsg = Utility.messageBD(this, "APRM_InvoiceDescUsedCredit",
                vars.getLanguage());
            if (creditMsg != null) {
              final StringBuffer newDesc = new StringBuffer();
              for (final String line : invDesc.split("\n")) {
                if (!line.startsWith(creditMsg.substring(0, creditMsg.lastIndexOf("%s")))) {
                  newDesc.append(line);
                  if (!"".equals(line))
                    newDesc.append("\n");
                }
              }
              invoice.setDescription(newDesc.toString());
            }
          }
        }
        OBDal.getInstance().save(invoice);
        OBDal.getInstance().flush();

        OBContext.setAdminMode();
        try {
          // on error close popup
          if (pinstance.getResult() == 0L) {
            OBDal.getInstance().commitAndClose();
            final PInstanceProcessData[] pinstanceData = PInstanceProcessData.select(this,
                pinstance.getId());
            myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
            log4j.debug(myMessage.getMessage());
            vars.setMessage(strTabId, myMessage);

            String strWindowPath = Utility.getTabURL(strTabId, "R", true);
            if (strWindowPath.equals(""))
              strWindowPath = strDefaultServlet;
            printPageClosePopUp(response, vars, strWindowPath);
            return;
          }
        } finally {
          OBContext.restorePreviousMode();
        }

        for (ProcessInvoiceHook hook : hooks) {
          msg = hook.postProcess(invoice, strdocaction);
          if (msg != null && "Error".equals(msg.getType())) {
            vars.setMessage(strTabId, msg);
            String strWindowPath = Utility.getTabURL(strTabId, "R", true);
            if (strWindowPath.equals(""))
              strWindowPath = strDefaultServlet;
            printPageClosePopUp(response, vars, strWindowPath);
            OBDal.getInstance().rollbackAndClose();
            return;
          }
        }

        OBDal.getInstance().commitAndClose();
        final PInstanceProcessData[] pinstanceData = PInstanceProcessData.select(this,
            pinstance.getId());
        myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);

        // Ticket 2505 -- Cruce automático Notas de Crédito
        boolean v_FlagNC = false;

        String strfinPaymentID = null;
        String strCreditNoteID = "ND";
        String strNewInvoiceID = null;
        BigDecimal v_bdcTotalCreditNote = BigDecimal.ZERO;
        double v_bdcTotalInvoice = 0;
        double v_bdcTotalInvoiceForPay = 0;

        Invoice invoice2 = dao.getObject(Invoice.class, strC_Invoice_ID); // Recupero objeto Invoice

        if ("CO".equals(strdocaction)) {

          boolean blDoctypePament = true;
          boolean blDoctypePament2 = true;
          boolean blFinancialAccount = true;
          boolean blInvoiceCreditNote = true;
          boolean blTotalInvoice = true;
          boolean blBPartner = true;

          // Valida las configuraciones del tipo de Documento
          if ((invoice2.isSalesTransaction() && invoice2.getDocumentType().isReversal()
              && invoice2.getDocumentType().getDocumentCategory().equals("ARI_RM"))
              || (!invoice2.isSalesTransaction() && invoice2.getDocumentType().isReversal())) {

            // TIPO DE DOCUMENTO
            DocumentType DocumentType = invoice.getDocumentType();

            Table objTable = null;
            objTable = OBDal.getInstance().get(Table.class, "D1A97202E832470285C9B1EB026D54E2");// ID
                                                                                                // DE
                                                                                                // LA
                                                                                                // TABLA
                                                                                                // FIN_PAYMENT
            // Valida que el tipo de documento para los cobros debe estar marcado el check
            // ssfiIscrossing
            OBCriteria<DocumentType> ObjDocumentType = OBDal.getInstance()
                .createCriteria(DocumentType.class);
            ObjDocumentType.add(Restrictions.eq("table", objTable));
            ObjDocumentType.add(Restrictions.eq("ssfiIscrossing", true));

            List<DocumentType> lstDocumentType = ObjDocumentType.list();

            if (lstDocumentType.size() == 0) {

              String strMsg = rollBackInvoice(this, strC_Invoice_ID);
              blDoctypePament = false;
              myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_AutoCrossing1@");
              vars.setMessage(strTabId, myMessage);
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              OBDal.getInstance().rollbackAndClose();
              return;

            }
            if (lstDocumentType.size() > 1) {
              String strMsg = rollBackInvoice(this, strC_Invoice_ID);
              blDoctypePament2 = false;
              myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_AutoCrossing2@");
              vars.setMessage(strTabId, myMessage);
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              OBDal.getInstance().rollbackAndClose();
              return;
            }

            // CUENTA FINANCIERA
            OBCriteria<FIN_FinancialAccount> ObjFinancialAccount = OBDal.getInstance()
                .createCriteria(FIN_FinancialAccount.class);
            ObjFinancialAccount.add(Restrictions.eq("default", true));

            List<FIN_FinancialAccount> lstFinalcialAccount = ObjFinancialAccount.list();
            if (lstFinalcialAccount.size() == 0) {
              String strMsg = rollBackInvoice(this, strC_Invoice_ID);
              blFinancialAccount = false;
              myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_AutoCrossing3@");
              vars.setMessage(strTabId, myMessage);
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              OBDal.getInstance().rollbackAndClose();
              return;
            }
            if (lstFinalcialAccount.size() > 1) {
              String strMsg = rollBackInvoice(this, strC_Invoice_ID);
              blFinancialAccount = false;

              myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_AutoCrossing4@");
              vars.setMessage(strTabId, myMessage);
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              return;
            }
            FIN_FinancialAccount obdalFinancialAccount = OBDal.getInstance()
                .get(FIN_FinancialAccount.class, ObjFinancialAccount.list().get(0).getId());// ID

            // FORMA DE PAGO - Cuenta Financiera
            FIN_PaymentMethod objPaymentMethodFinAccount = invoice2.getPaymentMethod();

            OBCriteria<FinAccPaymentMethod> ObjFinAccPaymentMethod = OBDal.getInstance()
                .createCriteria(FinAccPaymentMethod.class);
            ObjFinAccPaymentMethod
                .add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, obdalFinancialAccount));
            ObjFinAccPaymentMethod.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD,
                objPaymentMethodFinAccount));

            if (ObjFinAccPaymentMethod.list().size() == 0) {
              String strMsg = rollBackInvoice(this, strC_Invoice_ID);
              blFinancialAccount = false;

              myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_AutoCrossingMethodPayment@");
              vars.setMessage(strTabId, myMessage);
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              return;
            }

            String strFinancialAccountID = lstFinalcialAccount.get(0).getId();

            // TERCERO
            BusinessPartner objBusinessPartner = invoice2.getBusinessPartner();
            if (objBusinessPartner == null || objBusinessPartner.toString().equals("")) {
              String strMsg = rollBackInvoice(this, strC_Invoice_ID);

              blBPartner = false;

              myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_AutoCrossing5@");
              vars.setMessage(strTabId, myMessage);
              // throw new OBException("@Ssfi_AutoCrossing5@");
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              OBDal.getInstance().rollbackAndClose();
              return;
            }
            // FECHA
            Date dtInvoiceDate = invoice2.getInvoiceDate();

            // MONEDA
            Currency currency = invoice2.getCurrency();

            // FORMA DE PAGO
            FIN_PaymentMethod objPaymentMethod = invoice2.getPaymentMethod();

            // IMPORTE TOTAL
            Invoice objInvoice = invoice2.getScnrInvoice();

            if (objInvoice == null || objInvoice.equals("")) {
              String strMsg = rollBackInvoice(this, strC_Invoice_ID);
              blInvoiceCreditNote = false;

              myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_AutoCrossing6@");
              vars.setMessage(strTabId, myMessage);
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              OBDal.getInstance().rollbackAndClose();
              return;
            }

            List<FIN_PaymentSchedule> lstPaymentSchedule = objInvoice.getFINPaymentScheduleList();

            // Valido si la factura origen se encuentra competada
            if (lstPaymentSchedule == null || lstPaymentSchedule.size() == 0) {
              String strMsg = rollBackInvoice(this, strC_Invoice_ID);
              blInvoiceCreditNote = false;

              myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_ErrorStatusInvoiceReference@");
              vars.setMessage(strTabId, myMessage);
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              OBDal.getInstance().rollbackAndClose();
              return;
            }

            // CONTRA VALOR PENDIENTE POR PAGAR/COBRAR
            BigDecimal bdcTotalInvoiceForPay = objInvoice.getFINPaymentScheduleList().get(0)
                .getOutstandingAmount();

            // BigDecimal bdcTotalInvoiceForPay = new BigDecimal(
            // getValueOutStanding(this,objInvoice.getId() );

            BigDecimal bdcTotalCreditNote = invoice2.getGrandTotalAmount();

            /*
             * if(bdcTotalInvoice.compareTo(0)) { //FACTURA REFERENCIADA YA PAGADA }
             */
            if (bdcTotalCreditNote.compareTo(bdcTotalInvoiceForPay) > 0) {
              String strMsg = rollBackInvoice(this, strC_Invoice_ID);

              blTotalInvoice = false;

              myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                  "@Ssfi_AutoCrossing7@");
              vars.setMessage(strTabId, myMessage);
              String strWindowPath = Utility.getTabURL(strTabId, "R", true);
              if (strWindowPath.equals(""))
                strWindowPath = strDefaultServlet;
              printPageClosePopUp(response, vars, strWindowPath);
              return;
            }
            BigDecimal bdcTotal = BigDecimal.valueOf(0); // 0.00 POR CRUCE

            // ORGANIZACIÓN

            Organization obj0rganization = null;
            obj0rganization = OBDal.getInstance().get(Organization.class,
                invoice2.getOrganization().getId());

            // PAGO CON VALOR DE LA NOTA DE CRÉDITO EN POSITIVO

            BigDecimal bdcTotalInvoice = BigDecimal.valueOf(
                (bdcTotalCreditNote.doubleValue() < 0 ? (bdcTotalCreditNote.doubleValue() * -1)
                    : bdcTotalCreditNote.doubleValue()));
            bdcTotalCreditNote = BigDecimal.valueOf(
                (bdcTotalCreditNote.doubleValue() > 0 ? (bdcTotalCreditNote.doubleValue() * -1)
                    : bdcTotalCreditNote.doubleValue()));

            if ((blDoctypePament) && (blDoctypePament2) && (blFinancialAccount)
                && (blInvoiceCreditNote) && (blTotalInvoice) && (blBPartner)) {

              // Nro. Documento
              String strNumber = Utility.getDocumentNo(this, vars, "", "Fin_Payment",
                  ObjDocumentType.list().get(0).getId(), ObjDocumentType.list().get(0).getId(),
                  false, true);

              OBError messagePayment = createPaymentQB(strFinancialAccountID, strNumber,
                  objBusinessPartner, dtInvoiceDate, currency, bdcTotal, bdcTotal,
                  String.valueOf(bdcTotal), objPaymentMethod, vars, objInvoice, bdcTotal,
                  obj0rganization, bdcTotalCreditNote, bdcTotalInvoice, bdcTotalInvoiceForPay,
                  invoice2);

              strCreditNoteID = invoice2.getId();

              OBCriteria<FIN_PaymentSchedule> ObjFinPaymentSch = OBDal.getInstance()
                  .createCriteria(FIN_PaymentSchedule.class);
              ObjFinPaymentSch.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_INVOICE, invoice2));

              List<FIN_PaymentScheduleDetail> finpaymsch = ObjFinPaymentSch.list().get(0)
                  .getFINPaymentScheduleDetailInvoicePaymentScheduleList();

              FIN_Payment newPayment = finpaymsch.get(0).getPaymentDetails().getFinPayment();

              strfinPaymentID = newPayment.getId() == null ? "ND" : newPayment.getId();
              OBDal.getInstance().commitAndClose();

              /*
               * if (!strfinPaymentID.equals("ND")) { insertFin_Transaction(bdcTotalInvoice,
               * strfinPaymentID, strFinancialAccountID, String.valueOf(new Date()), null, vars,
               * this);
               * 
               * }
               */

              strNewInvoiceID = objInvoice.getId();

              v_FlagNC = true;

              v_bdcTotalCreditNote = bdcTotalCreditNote;

              v_bdcTotalInvoice = bdcTotalInvoice.doubleValue();

              v_bdcTotalInvoiceForPay = bdcTotalInvoiceForPay.doubleValue();

              UpdateInvoicePaid(this, strCreditNoteID, v_bdcTotalCreditNote);

              DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
              simbolos.setDecimalSeparator('.');
              DecimalFormat formatDecimal = new DecimalFormat("#########0.00", simbolos);
              Double dblTotalCreditNote = Double.valueOf((formatDecimal.format(v_bdcTotalInvoice)));
              Double dblTotalInvoice = Double
                  .valueOf((formatDecimal.format(v_bdcTotalInvoiceForPay)));
              dblTotalCreditNote = (dblTotalCreditNote < 0 ? dblTotalCreditNote * -1
                  : dblTotalCreditNote);
              dblTotalInvoice = (dblTotalInvoice < 0 ? dblTotalInvoice * -1 : dblTotalInvoice);
              Double dblTotal = dblTotalInvoice - dblTotalCreditNote;
              BigDecimal bdcTotalCreditNotes = BigDecimal.valueOf(dblTotalCreditNote);

              if (dblTotal == 0) {

                // ACTUALIZA ESTADO FACTURA A PAGADA
                UpdateInvoicePaid(this, strNewInvoiceID, bdcTotalCreditNotes);
                UpdateInvoiceSchedulePaid(this, strNewInvoiceID,
                    BigDecimal.valueOf(v_bdcTotalInvoiceForPay));
              }

              String strMsg = updateInvoiceOrigin(this, strCreditNoteID);
              myMessage = messagePayment;

            }

          } // FIN Valida las configuraciones del tipo de Documento

        }

        // FIN Ticket 2505

        log4j.debug(myMessage.getMessage());
        vars.setMessage(strTabId, myMessage);

        OBContext.setAdminMode();
        try {
          if (!"CO".equals(strdocaction)) {
            String strWindowPath = Utility.getTabURL(strTabId, "R", true);
            if (strWindowPath.equals(""))
              strWindowPath = strDefaultServlet;
            printPageClosePopUp(response, vars, strWindowPath);
            return;
          }
        } finally {
          OBContext.restorePreviousMode();
        }

        if ("CO".equals(strdocaction)) {
          // Need to refresh the invoice again from the db
          invoice = dao.getObject(Invoice.class, strC_Invoice_ID);
          OBContext.setAdminMode(false);
          String invoiceDocCategory = "";
          try {
            invoiceDocCategory = invoice.getDocumentType().getDocumentCategory();

            /*
             * Print a grid popup in case of credit payment
             */
            // If the invoice grand total is ZERO or already has payments (due to
            // payment method automation) or the business partner does not have a default financial
            // account defined or invoice's payment method is not inside BP's financial
            // account or the business partner's currency is not equal to the invoice's currency do
            // not cancel credit
            if (BigDecimal.ZERO.compareTo(invoice.getGrandTotalAmount()) != 0
                && isPaymentMethodConfigured(invoice) && !isInvoiceWithPayments(invoice)
                && (AcctServer.DOCTYPE_ARInvoice.equals(invoiceDocCategory)
                    || AcctServer.DOCTYPE_APInvoice.equals(invoiceDocCategory))
                && (invoice.getBusinessPartner().getCurrency() != null
                    && StringUtils.equals(invoice.getCurrency().getId(),
                        invoice.getBusinessPartner().getCurrency().getId()))) {
              creditPayments = dao.getCustomerPaymentsWithCredit(invoice.getOrganization(),
                  invoice.getBusinessPartner(), invoice.isSalesTransaction(),
                  invoice.getCurrency());
              if (creditPayments != null && !creditPayments.isEmpty()) {
                printPageCreditPaymentGrid(response, vars, strC_Invoice_ID, strdocaction, strTabId,
                    strC_Invoice_ID, strdocaction, strWindowId, strTabId, invoice.getInvoiceDate(),
                    strOrg);
              }
            }
          } finally {
            OBContext.restorePreviousMode();
          }

          executePayments(response, vars, strWindowId, strTabId, strC_Invoice_ID, strOrg);

          if (!strCreditNoteID.equals("ND")) {
            String strMsg = updateInvoiceOrigin(this, strCreditNoteID);
          }

        }

      } catch (ServletException ex) {
        myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
        if (!myMessage.isConnectionAvailable()) {
          bdErrorConnection(response);
          return;
        } else
          vars.setMessage(strTabId, myMessage);
      }

    } else if (vars.commandIn("GRIDLIST")) {
      final String strWindowId = vars.getGlobalVariable("inpwindowId", "ProcessInvoice|Window_ID",
          IsIDFilter.instance);
      final String strC_Invoice_ID = vars.getGlobalVariable("inpKey", strWindowId + "|C_Invoice_ID",
          "", IsIDFilter.instance);

      printGrid(response, vars, strC_Invoice_ID);
    } else if (vars.commandIn("USECREDITPAYMENTS") || vars.commandIn("CANCEL_USECREDITPAYMENTS")) {
      final String strWindowId = vars.getGlobalVariable("inpwindowId", "ProcessInvoice|Window_ID",
          IsIDFilter.instance);
      final String strTabId = vars.getGlobalVariable("inpTabId", "ProcessInvoice|Tab_ID",
          IsIDFilter.instance);
      final String strC_Invoice_ID = vars.getGlobalVariable("inpKey", strWindowId + "|C_Invoice_ID",
          "");
      final String strPaymentDate = vars.getRequiredStringParameter("inpPaymentDate");
      final String strOrg = vars.getGlobalVariable("inpadOrgId", "ProcessInvoice|Org_ID",
          IsIDFilter.instance);

      final String strCreditPaymentIds;
      if (vars.commandIn("CANCEL_USECREDITPAYMENTS")) {
        strCreditPaymentIds = null;
      } else {
        strCreditPaymentIds = vars.getInParameter("inpCreditPaymentId", IsIDFilter.instance);
      }

      /*
       * Use credit logic
       */
      if (strCreditPaymentIds != null && !strCreditPaymentIds.isEmpty()) {
        List<FIN_Payment> selectedCreditPayment = FIN_Utility.getOBObjectList(FIN_Payment.class,
            strCreditPaymentIds);
        HashMap<String, BigDecimal> selectedCreditPaymentAmounts = FIN_AddPayment
            .getSelectedBaseOBObjectAmount(vars, selectedCreditPayment, "inpPaymentAmount");
        try {
          OBContext.setAdminMode(true);
          final Invoice invoice = OBDal.getInstance().get(Invoice.class, strC_Invoice_ID);

          final StringBuffer creditPaymentsIdentifiers = new StringBuffer();
          BigDecimal totalUsedCreditAmt = BigDecimal.ZERO;
          for (final FIN_Payment creditPayment : selectedCreditPayment) {
            final BigDecimal usedCreditAmt = selectedCreditPaymentAmounts
                .get(creditPayment.getId());
            // Set Used Credit = Amount + Previous used credit introduced by the user
            creditPayment.setUsedCredit(usedCreditAmt.add(creditPayment.getUsedCredit()));
            final StringBuffer description = new StringBuffer();
            if (creditPayment.getDescription() != null
                && !creditPayment.getDescription().equals(""))
              description.append(creditPayment.getDescription()).append("\n");
            description.append(String.format(
                Utility.messageBD(this, "APRM_CreditUsedinInvoice", vars.getLanguage()),
                invoice.getDocumentNo()));
            String truncateDescription = (description.length() > 255)
                ? description.substring(0, 251).concat("...").toString()
                : description.toString();
            creditPayment.setDescription(truncateDescription);
            totalUsedCreditAmt = totalUsedCreditAmt.add(usedCreditAmt);
            creditPaymentsIdentifiers.append(creditPayment.getDocumentNo());
            creditPaymentsIdentifiers.append(", ");
          }
          creditPaymentsIdentifiers.delete(creditPaymentsIdentifiers.length() - 2,
              creditPaymentsIdentifiers.length());
          creditPaymentsIdentifiers.append("\n");

          final List<FIN_PaymentScheduleDetail> paymentScheduleDetails = new ArrayList<FIN_PaymentScheduleDetail>();
          final HashMap<String, BigDecimal> paymentScheduleDetailsAmounts = new HashMap<String, BigDecimal>();
          BigDecimal allocatedAmt = BigDecimal.ZERO;
          for (final FIN_PaymentScheduleDetail paymentScheduleDetail : dao
              .getInvoicePendingScheduledPaymentDetails(invoice)) {
            if (totalUsedCreditAmt.compareTo(allocatedAmt) > 0) {
              final BigDecimal pendingToAllocate = totalUsedCreditAmt.subtract(allocatedAmt);
              paymentScheduleDetails.add(paymentScheduleDetail);

              final BigDecimal psdAmt = paymentScheduleDetail.getAmount();
              if (psdAmt.compareTo(pendingToAllocate) <= 0) {
                paymentScheduleDetailsAmounts.put(paymentScheduleDetail.getId(), psdAmt);
                allocatedAmt = allocatedAmt.add(psdAmt);
              } else {
                paymentScheduleDetailsAmounts.put(paymentScheduleDetail.getId(), pendingToAllocate);
                allocatedAmt = allocatedAmt.add(pendingToAllocate);
              }
            }
          }

          // Create new Payment
          final boolean isSalesTransaction = invoice.isSalesTransaction();
          final DocumentType docType = FIN_Utility.getDocumentType(invoice.getOrganization(),
              isSalesTransaction ? AcctServer.DOCTYPE_ARReceipt : AcctServer.DOCTYPE_APPayment);
          final String strPaymentDocumentNo = FIN_Utility.getDocumentNo(docType,
              docType.getTable() != null ? docType.getTable().getDBTableName() : "");
          final FIN_FinancialAccount bpFinAccount = isSalesTransaction
              ? invoice.getBusinessPartner().getAccount()
              : invoice.getBusinessPartner().getPOFinancialAccount();
          // Calculate Conversion Rate
          final ConversionRate conversionRate = StringUtils.equals(invoice.getCurrency().getId(),
              bpFinAccount.getCurrency().getId())
                  ? null
                  : FinancialUtils.getConversionRate(FIN_Utility.getDate(strPaymentDate),
                      invoice.getCurrency(), bpFinAccount.getCurrency(), invoice.getOrganization(),
                      invoice.getClient());
          final FIN_Payment newPayment = FIN_AddPayment.savePayment(null, isSalesTransaction,
              docType, strPaymentDocumentNo, invoice.getBusinessPartner(),
              invoice.getPaymentMethod(), bpFinAccount, "0", FIN_Utility.getDate(strPaymentDate),
              invoice.getOrganization(), invoice.getDocumentNo(), paymentScheduleDetails,
              paymentScheduleDetailsAmounts, false, false, invoice.getCurrency(),
              conversionRate != null ? conversionRate.getMultipleRateBy() : null, null);
          newPayment.setAmount(BigDecimal.ZERO);
          newPayment.setGeneratedCredit(BigDecimal.ZERO);
          newPayment.setUsedCredit(totalUsedCreditAmt);

          // Link new Payment with the credit payments used
          for (final FIN_Payment creditPayment : selectedCreditPayment) {
            final BigDecimal usedCreditAmt = selectedCreditPaymentAmounts
                .get(creditPayment.getId());
            FIN_PaymentProcess.linkCreditPayment(newPayment, usedCreditAmt, creditPayment);
          }

          // Process the new payment
          OBError message = FIN_AddPayment.processPayment(vars, this, "P", newPayment);
          if ("Success".equals(message.getType())) {
            // Update Invoice's description
            final StringBuffer invDesc = new StringBuffer();
            if (invoice.getDescription() != null) {
              invDesc.append(invoice.getDescription());
              invDesc.append("\n");
            }
            invDesc.append(String.format(
                Utility.messageBD(this, "APRM_InvoiceDescUsedCredit", vars.getLanguage()),
                creditPaymentsIdentifiers.toString()));
            invoice.setDescription(invDesc.toString());
          } else {
            message
                .setMessage(OBMessageUtils.messageBD("PaymentError") + " " + message.getMessage());
            vars.setMessage(strTabId, message);
          }

        } catch (final Exception e) {
          log4j.error("Exception while canceling the credit in the invoice: " + strC_Invoice_ID);
          e.printStackTrace();
        } finally {
          OBContext.restorePreviousMode();
        }
      }
      executePayments(response, vars, strWindowId, strTabId, strC_Invoice_ID, strOrg);
    }
  }

  private void executePayments(HttpServletResponse response, VariablesSecureApp vars,
      final String strWindowId, final String strTabId, final String strC_Invoice_ID,
      final String strOrg) throws IOException, ServletException {
    OBError myMessage = new OBError();

    List<FIN_Payment> payments = null;
    try {
      OBContext.setAdminMode(true);
      payments = dao.getPendingExecutionPayments(strC_Invoice_ID);
    } finally {
      OBContext.restorePreviousMode();
    }
    if (payments != null && payments.size() > 0) {
      vars.setSessionValue("ExecutePayments|Window_ID", strWindowId);
      vars.setSessionValue("ExecutePayments|Tab_ID", strTabId);
      vars.setSessionValue("ExecutePayments|Org_ID", strOrg);
      vars.setSessionValue("ExecutePayments|payments", Utility.getInStrList(payments));
      if (myMessage != null)
        vars.setMessage("ExecutePayments|message", myMessage);
      response.sendRedirect(
          strDireccion + "/org.openbravo.advpaymentmngt.ad_actionbutton/ExecutePayments.html");
    } else {
      String strWindowPath = Utility.getTabURL(strTabId, "R", true);
      if (strWindowPath.equals(""))
        strWindowPath = strDefaultServlet;
      printPageClosePopUp(response, vars, strWindowPath);
    }

    vars.removeSessionValue("ProcessInvoice|Window_ID");
    vars.removeSessionValue("ProcessInvoice|Tab_ID");
    vars.removeSessionValue("ProcessInvoice|Org_ID");
  }

  void printPageDocAction(HttpServletResponse response, VariablesSecureApp vars,
      String strC_Invoice_ID, String strdocaction, String strProcessing, String strdocstatus,
      String stradTableId, String strWindowId) throws IOException, ServletException {
    log4j.debug("Output: Button process 111");
    String[] discard = { "newDiscard" };
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/DocAction", discard)
        .createXmlDocument();
    xmlDocument.setParameter("key", strC_Invoice_ID);
    xmlDocument.setParameter("processing", strProcessing);
    xmlDocument.setParameter("form", "ProcessInvoices.html");
    xmlDocument.setParameter("window", strWindowId);
    xmlDocument.setParameter("css", vars.getTheme());
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("dateDisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("processId", "111");
    xmlDocument.setParameter("cancel", Utility.messageBD(this, "Cancel", vars.getLanguage()));
    xmlDocument.setParameter("ok", Utility.messageBD(this, "OK", vars.getLanguage()));

    OBError myMessage = vars.getMessage("111");
    vars.removeMessage("111");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }

    xmlDocument.setParameter("docstatus", strdocstatus);
    if (strWindowId.equals(Purchase_Invoice_Window)) {
      // VOID action: Reverse sales/purchase invoice by default takes today as document date and
      // accounting date.
      xmlDocument.setParameter("voidedDocumentDate", DateTimeData.today(this));
      xmlDocument.setParameter("voidedDocumentAcctDate", DateTimeData.today(this));
      Invoice invoice = (Invoice) OBDal.getInstance().getProxy(Invoice.ENTITY_NAME,
          strC_Invoice_ID);
      xmlDocument.setParameter("documentDate", OBDateUtils.formatDate(invoice.getInvoiceDate()));
      xmlDocument.setParameter("documentAcctDate",
          OBDateUtils.formatDate(invoice.getAccountingDate()));
    }
    xmlDocument.setParameter("adTableId", stradTableId);
    xmlDocument.setParameter("processId", "111");
    xmlDocument.setParameter("processDescription", "Process Invoice");
    xmlDocument.setParameter("docaction", (strdocaction.equals("--") ? "CL" : strdocaction));
    FieldProvider[] dataDocAction = ActionButtonUtility.docAction(this, vars, strdocaction, "135",
        strdocstatus, strProcessing, stradTableId);
    xmlDocument.setData("reportdocaction", "liststructure", dataDocAction);
    StringBuffer dact = new StringBuffer();
    if (dataDocAction != null) {
      dact.append("var arrDocAction = new Array(\n");
      for (int i = 0; i < dataDocAction.length; i++) {
        dact.append("new Array(\"" + dataDocAction[i].getField("id") + "\", \""
            + dataDocAction[i].getField("name") + "\", \""
            + dataDocAction[i].getField("description") + "\")\n");
        if (i < dataDocAction.length - 1)
          dact.append(",\n");
      }
      dact.append(");");
    } else
      dact.append("var arrDocAction = null");
    xmlDocument.setParameter("array", dact.toString());

    out.println(xmlDocument.print());
    out.close();

  }

  void printPageCreditPaymentGrid(HttpServletResponse response, VariablesSecureApp vars,
      String strC_Invoice_ID, String strdocaction, String strProcessing, String strdocstatus,
      String stradTableId, String strWindowId, String strTabId, Date invoiceDate, String strOrg)
      throws IOException, ServletException {
    log4j.debug("Output: Credit Payment Grid popup");
    String[] discard = { "" };
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CreditPaymentGrid", discard)
        .createXmlDocument();
    xmlDocument.setParameter("css", vars.getTheme());
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("window", strWindowId);
    xmlDocument.setParameter("tab", strTabId);
    xmlDocument.setParameter("adOrgId", strOrg);

    xmlDocument.setParameter("messageType", "SUCCESS");
    xmlDocument.setParameter("messageTitle",
        Utility.messageBD(this, "InvoiceComplete", vars.getLanguage()));

    xmlDocument.setParameter("invoiceGrossAmt",
        dao.getObject(Invoice.class, strC_Invoice_ID).getGrandTotalAmount().toString());

    OBError myMessage = vars.getMessage("ProcessInvoice|CreditPaymentGrid");
    vars.removeMessage("ProcessInvoice|CreditPaymentGrid");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }

    xmlDocument.setParameter("dateDisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("paymentDate",
        Utility.formatDate(invoiceDate, vars.getJavaDateFormat()));

    out.println(xmlDocument.print());
    out.close();

  }

  private void printGrid(HttpServletResponse response, VariablesSecureApp vars, String invoiceId)
      throws IOException, ServletException {
    log4j.debug("Output: Grid with credit payments");

    final Invoice invoice = dao.getObject(Invoice.class, invoiceId);

    String[] discard = {};
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/AddCreditPaymentGrid", discard)
        .createXmlDocument();

    xmlDocument.setData("structure", getCreditPayments(invoice));

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private FieldProvider[] getCreditPayments(Invoice invoice) {
    FieldProvider[] data = FieldProviderFactory.getFieldProviderArray(creditPayments);
    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);

    BigDecimal pendingToPay = invoice.getGrandTotalAmount();
    try {
      OBContext.setAdminMode(true);
      for (int i = 0; i < data.length; i++) {
        FieldProviderFactory.setField(data[i], "finCreditPaymentId", creditPayments.get(i).getId());
        FieldProviderFactory.setField(data[i], "documentNo", creditPayments.get(i).getDocumentNo());
        FieldProviderFactory.setField(data[i], "paymentDescription",
            creditPayments.get(i).getDescription());
        if (creditPayments.get(i).getPaymentDate() != null) {
          FieldProviderFactory.setField(data[i], "documentDate",
              dateFormater.format(creditPayments.get(i).getPaymentDate()).toString());
        }

        final BigDecimal outStandingAmt = creditPayments.get(i).getGeneratedCredit()
            .subtract(creditPayments.get(i).getUsedCredit());
        FieldProviderFactory.setField(data[i], "outstandingAmount", outStandingAmt.toString());

        FieldProviderFactory.setField(data[i], "paymentAmount",
            pendingToPay.compareTo(outStandingAmt) > 0 ? outStandingAmt.toString()
                : (pendingToPay.compareTo(BigDecimal.ZERO) > 0 ? pendingToPay.toString() : ""));
        pendingToPay = pendingToPay.subtract(outStandingAmt);

        FieldProviderFactory.setField(data[i], "finSelectedCreditPaymentId",
            "".equals(data[i].getField("paymentAmount")) ? "" : creditPayments.get(i).getId());
        FieldProviderFactory.setField(data[i], "rownum", String.valueOf(i));
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return data;
  }

  private boolean isInvoiceWithPayments(Invoice invoice) {
    for (FIN_PaymentSchedule ps : OBDao.getFilteredCriteria(FIN_PaymentSchedule.class,
        Restrictions.eq(FIN_PaymentSchedule.PROPERTY_INVOICE, invoice)).list()) {
      for (FIN_PaymentDetailV pdv : OBDao.getFilteredCriteria(FIN_PaymentDetailV.class,
          Restrictions.eq(FIN_PaymentDetailV.PROPERTY_INVOICEPAYMENTPLAN, ps)).list()) {
        if (pdv.getPayment() != null && !"RPVOID".equals(pdv.getPayment().getStatus())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks if the invoice business partner has defined a default financial account and if the
   * payment method selected in the invoice belongs to the default financial account.
   * 
   * @param invoice
   *          Invoice.
   * @return True if the invoice business partner has defined a default financial account and the
   *         payment method selected in the invoice belongs to the default financial account. False
   *         in other cases.
   */
  private boolean isPaymentMethodConfigured(Invoice invoice) {
    final FIN_FinancialAccount bpFinAccount = invoice.isSalesTransaction()
        ? invoice.getBusinessPartner().getAccount()
        : invoice.getBusinessPartner().getPOFinancialAccount();
    if (bpFinAccount != null) {
      for (final FinAccPaymentMethod bpFinAccPaymentMethod : bpFinAccount
          .getFinancialMgmtFinAccPaymentMethodList()) {
        if (bpFinAccPaymentMethod.getPaymentMethod().equals(invoice.getPaymentMethod())) {
          return true;
        }
      }
    }
    return false;
  }

  public String getServletInfo() {
    return "Servlet to Process Invoice";
  }

  public OBError createPaymentQB(String strFin_AccountID, String strDocumentno,
      BusinessPartner businessPartner, Date paymentDate, Currency currency, BigDecimal exchangeRate,
      BigDecimal convertedAmount, String strActualPayment, FIN_PaymentMethod finPaymentMethod,
      VariablesSecureApp vars, Invoice objInvoice, BigDecimal bgdTotalPaid,
      Organization objOrganization, BigDecimal bgdTotalPaidCreditNote,
      BigDecimal bgdTotalPaidInvoice, BigDecimal bgdTotalInvoice, Invoice objCreditNote) {

    OBError message = new OBError();
    FIN_Payment payment = OBProvider.getInstance().get(FIN_Payment.class);

    JSONObject jsonResponse = new JSONObject();
    JSONObject jsonparams = null;

    String comingFrom = "TRANSACTION";
    /*
     * try { jsonparams = jsonResponse.getJSONObject("_params"); } catch (JSONException e1) { //
     * TODO Auto-generated catch block e1.printStackTrace(); }
     */
    // Action to do
    final String strAction = "PRP";

    final boolean isReceipt = true;

    // Payment is already created. Load it.
    final String strFinPaymentID = "";
    JSONObject jsonparams_cp = new JSONObject();
    try {

      jsonparams_cp.put("fin_financial_account_id", strFin_AccountID);
      jsonparams_cp.put("payment_documentno", strDocumentno);
      jsonparams_cp.put("fin_paymentmethod_id", finPaymentMethod.getId());
      jsonparams_cp.put("reference_no", "");
      payment = createNewPayment(jsonparams_cp, isReceipt, objOrganization, businessPartner,
          paymentDate, currency, new BigDecimal("1"), convertedAmount, strActualPayment);
    } catch (JSONException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (SQLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // List<String> pdToRemove = new ArrayList<String>();
    // pdToRemove = OBDao.getIDListFromOBObject(payment.getFINPaymentDetailList());
    // OverPayment action
    String strDifferenceAction = "refund";
    BigDecimal differenceAmount = BigDecimal.ZERO;

    payment.setAmount(new BigDecimal(strActualPayment));

    // FIN_AddPayment.setFinancialTransactionAmountAndRate(vars, payment, exchangeRate,
    // convertedAmount);
    payment.setFinancialTransactionAmount(new BigDecimal(strActualPayment));
    payment.setFinancialTransactionConvertRate(new BigDecimal("1"));
    OBDal.getInstance().save(payment);

    if (!payment.getId().isEmpty()) {

      String strFin_PaymentID = "";
      strFin_PaymentID = payment.getId();

      OBCriteria<FIN_PaymentSchedule> ObcriteriaFPS = OBDal.getInstance()
          .createCriteria(FIN_PaymentSchedule.class);
      ObcriteriaFPS.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_INVOICE, objCreditNote));
      String strFinPaymeSchedulID = "";

      String strNewPD_ID = null;
      String strNewPDInvoice_ID = null;
      try {
        org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);

        strNewPD_ID = getUUID(this);
        strNewPDInvoice_ID = getUUID(this);
      } catch (Exception e) {

      }

      if (!ObcriteriaFPS.list().isEmpty()) {
        strFinPaymeSchedulID = ObcriteriaFPS.list().get(0).getId();

        try {
          FIN_PaymentSchedule FPS = OBDal.getInstance().get(FIN_PaymentSchedule.class,
              strFinPaymeSchedulID);

          FPS.setPaidAmount(bgdTotalPaid);
          FPS.setOutstandingAmount(BigDecimal.ZERO);
          OBDal.getInstance().save(FPS);
          OBDal.getInstance().commitAndClose();

          try {

            // ACTUALIZACIÓN LÍNEA DE NOTA DE CRÉDITO
            org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
            NewPaymentDetail(this, strNewPD_ID, payment.getId(), bgdTotalPaidInvoice); // Payment
                                                                                       // Detail
                                                                                       // para la NC
            NewPaymentDetail(this, strNewPDInvoice_ID, payment.getId(), // Payment Detail para la
                                                                        // Factura Origen
                objCreditNote.getGrandTotalAmount().multiply(new BigDecimal("-1")));
            // strFinPaymentScheduleDetailID = FPS.getId();

            OBCriteria<FIN_PaymentScheduleDetail> ObcriteriaFPSD = OBDal.getInstance()
                .createCriteria(FIN_PaymentScheduleDetail.class);
            ObcriteriaFPSD.add(
                Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE, FPS));
            // ObcriteriaFPSD.add(Restrictions.eq("paymentDetails", null));

            FIN_PaymentScheduleDetail FPSD = OBDal.getInstance()
                .get(FIN_PaymentScheduleDetail.class, ObcriteriaFPSD.list().get(0).getId());

            // DETALLE DE PAGO NOTA DE CRÉDITO
            UpdatePaymentSchedulDetail(this, strNewPD_ID, bgdTotalPaidCreditNote,
                objCreditNote.getBusinessPartner().getId(), FPSD.getId());

            UpdatePayment(this, strFin_PaymentID, bgdTotalPaid, objCreditNote.getDocumentNo());
          } catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log4j.error(e.getMessage());
            // throw new OBException(Utility.messageBD(conn, e.toString(), language));

          }

        } catch (OBException e) {
          OBDal.getInstance().rollbackAndClose();
          log4j.error(e.getMessage());

          // throw new OBException(Utility.messageBD(conn, e.toString(), language));

        }
      }

      String strFin_PaymentID2 = "";
      strFin_PaymentID2 = payment.getId();

      OBCriteria<FIN_PaymentSchedule> ObcriteriaFPS2 = OBDal.getInstance()
          .createCriteria(FIN_PaymentSchedule.class);
      ObcriteriaFPS2.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_INVOICE, objInvoice));
      String strFinPaymeSchedulID2 = "";
      // int tmp = ObcriteriaFPS2.list().size();
      if (!ObcriteriaFPS2.list().isEmpty()) {
        strFinPaymeSchedulID2 = ObcriteriaFPS2.list().get(0).getId();

        try {

          FIN_PaymentSchedule FPS2 = OBDal.getInstance().get(FIN_PaymentSchedule.class,
              strFinPaymeSchedulID2);
          DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
          simbolos.setDecimalSeparator('.');
          DecimalFormat formatDecimal = new DecimalFormat("#########0.00", simbolos);
          Double dblTotalCreditNote = Double
              .valueOf((formatDecimal.format(bgdTotalPaidCreditNote.doubleValue())));
          Double dblTotalInvoice = Double
              .valueOf((formatDecimal.format(bgdTotalInvoice.doubleValue())));
          dblTotalCreditNote = (dblTotalCreditNote < 0 ? dblTotalCreditNote * -1
              : dblTotalCreditNote);
          dblTotalInvoice = (dblTotalInvoice < 0 ? dblTotalInvoice * -1 : dblTotalInvoice);
          Double dblTotal = dblTotalInvoice - dblTotalCreditNote;

          FPS2.setPaidAmount(bgdTotalPaidInvoice);
          FPS2.setOutstandingAmount(BigDecimal.valueOf(dblTotal));
          OBDal.getInstance().save(FPS2);
          OBDal.getInstance().flush(); // Aqui se quedaba, se cambio el CommitAndClose por flush.

          try {

            org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);

            OBCriteria<FIN_PaymentScheduleDetail> ObcriteriaFPSD = OBDal.getInstance()
                .createCriteria(FIN_PaymentScheduleDetail.class);
            ObcriteriaFPSD.add(
                Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE, FPS2));
            ObcriteriaFPSD
                .add(Restrictions.isNull(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS));

            List<FIN_PaymentScheduleDetail> lstfpsd = ObcriteriaFPSD.list();

            if (lstfpsd.size() > 0) {

              // DETALLE DE PAGO FACTURA

              FIN_PaymentScheduleDetail FPSD = OBDal.getInstance()
                  .get(FIN_PaymentScheduleDetail.class, lstfpsd.get(0).getId());
              // SI ES PAGO PARCIAL
              if (dblTotal != 0) {
                org.openbravo.database.ConnectionProvider conn = new DalConnectionProvider(false);
                String strNewFinPaymentScheDetailId = getUUID(this);

                // INSERTA NUEVA FILA CON EL RESTANTE
                org.openbravo.database.ConnectionProvider conn2 = new DalConnectionProvider(false);

                String strNewPD2_ID = getUUID(this);

                NewPaymentSchedulDetail(this, strNewPD2_ID, BigDecimal.valueOf(dblTotal),
                    objInvoice.getBusinessPartner().getId(), FPS2.getId());

              }
              UpdatePaymentSchedulDetail(this, strNewPDInvoice_ID, bgdTotalPaidInvoice,
                  objInvoice.getBusinessPartner().getId(), FPSD.getId());

              // }

              UpdatePayment(this, strFin_PaymentID2, bgdTotalPaid, objInvoice.getDocumentNo());
            }
          } catch (ServletException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            log4j.error(e.getMessage());
            // throw new OBException(Utility.messageBD(conn, e.toString(), language));

          }

        } catch (OBException e) {
          OBDal.getInstance().rollbackAndClose();
          log4j.error(e.getMessage());

          // throw new OBException(Utility.messageBD(conn, e.toString(), language));

        }

      }

    }

    if (strAction.equals("PRP") || strAction.equals("PPP") || strAction.equals("PRD")
        || strAction.equals("PPW")) {

      try {
        message = processPayment(payment, strAction, strDifferenceAction, differenceAmount,
            exchangeRate, jsonparams_cp, comingFrom, objCreditNote, bgdTotalPaid);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.out.print(e.getMessage());

        log4j.error(e.getMessage());
      }

    }
    OBDal.getInstance().commitAndClose();

    return message;

  }

  private FIN_Payment createNewPayment(JSONObject jsonparams, boolean isReceipt, Organization org,
      BusinessPartner bPartner, Date paymentDate, Currency currency, BigDecimal conversionRate,
      BigDecimal convertedAmt, String strActualPayment)
      throws OBException, JSONException, SQLException {
    JSONObject obj = new JSONObject();

    String strPaymentDocumentNo = jsonparams.getString("payment_documentno");
    String strReferenceNo = "";
    if (jsonparams.get("reference_no") != JSONObject.NULL) {
      strReferenceNo = jsonparams.getString("reference_no");
    }
    String strFinancialAccountId = jsonparams.getString("fin_financial_account_id");
    FIN_FinancialAccount finAccount = OBDal.getInstance().get(FIN_FinancialAccount.class,
        strFinancialAccountId);
    String strPaymentMethodId = jsonparams.getString("fin_paymentmethod_id");
    FIN_PaymentMethod paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class,
        strPaymentMethodId);

    boolean paymentDocumentEnabled = getDocumentConfirmation(finAccount, paymentMethod, isReceipt,
        strActualPayment, true);
    String strAction = (isReceipt ? "PRP" : "PPP");
    boolean documentEnabled = true;
    if ((strAction.equals("PRD") || strAction.equals("PPW")
        || FIN_Utility.isAutomaticDepositWithdrawn(finAccount, paymentMethod, isReceipt))
        && new BigDecimal(strActualPayment).signum() != 0) {
      documentEnabled = paymentDocumentEnabled
          || getDocumentConfirmation(finAccount, paymentMethod, isReceipt, strActualPayment, false);
    } else {
      documentEnabled = paymentDocumentEnabled;
    }

    DocumentType documentType = FIN_Utility.getDocumentType(org, isReceipt ? "ARR" : "APP");
    String strDocBaseType = documentType.getDocumentCategory();

    OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(OBContext.getOBContext().getCurrentClient().getId());
    boolean orgLegalWithAccounting = osp.getLegalEntityOrBusinessUnit(org).getOrganizationType()
        .isLegalEntityWithAccounting();
    if (documentEnabled
        && !FIN_Utility.isPeriodOpen(OBContext.getOBContext().getCurrentClient().getId(),
            strDocBaseType, org.getId(), OBDateUtils.formatDate(paymentDate))
        && orgLegalWithAccounting) {
      String messag = OBMessageUtils.messageBD("PeriodNotAvailable");
      throw new OBException(messag);
    }

    String strPaymentAmount = "0";
    if (strPaymentDocumentNo.startsWith("<")) {
      // get DocumentNo
      strPaymentDocumentNo = FIN_Utility.getDocumentNo(documentType, "FIN_Payment");
    }

    FIN_Payment payment = (new AdvPaymentMngtDao()).getNewPayment(isReceipt, org, documentType,
        strPaymentDocumentNo, bPartner, paymentMethod, finAccount, strPaymentAmount, paymentDate,
        strReferenceNo, currency, conversionRate, convertedAmt);
    OBDal.getInstance().getConnection(true).commit();
    return payment;
  }

  private boolean getDocumentConfirmation(FIN_FinancialAccount finAccount,
      FIN_PaymentMethod finPaymentMethod, boolean isReceipt, String strPaymentAmount,
      boolean isPayment) {
    // Checks if this step is configured to generate accounting for the selected financial account
    boolean confirmation = false;
    OBContext.setAdminMode(true);
    try {
      OBCriteria<FinAccPaymentMethod> obCriteria = OBDal.getInstance()
          .createCriteria(FinAccPaymentMethod.class);
      obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, finAccount));
      obCriteria.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, finPaymentMethod));
      obCriteria.setFilterOnReadableClients(false);
      obCriteria.setFilterOnReadableOrganization(false);
      obCriteria.setMaxResults(1);
      FinAccPaymentMethod finAccPayMethod = (FinAccPaymentMethod) obCriteria.uniqueResult();
      String uponUse = "";
      if (isPayment) {
        if (isReceipt) {
          uponUse = finAccPayMethod.getUponReceiptUse();
        } else {
          uponUse = finAccPayMethod.getUponPaymentUse();
        }
      } else {
        if (isReceipt) {
          uponUse = finAccPayMethod.getUponDepositUse();
        } else {
          uponUse = finAccPayMethod.getUponWithdrawalUse();
        }
      }
      for (FIN_FinancialAccountAccounting account : finAccount.getFINFinancialAccountAcctList()) {
        if (confirmation) {
          return confirmation;
        }
        if (isReceipt) {
          if ("INT".equals(uponUse) && account.getInTransitPaymentAccountIN() != null) {
            confirmation = true;
          } else if ("DEP".equals(uponUse) && account.getDepositAccount() != null) {
            confirmation = true;
          } else if ("CLE".equals(uponUse) && account.getClearedPaymentAccount() != null) {
            confirmation = true;
          }
        } else {
          if ("INT".equals(uponUse) && account.getFINOutIntransitAcct() != null) {
            confirmation = true;
          } else if ("WIT".equals(uponUse) && account.getWithdrawalAccount() != null) {
            confirmation = true;
          } else if ("CLE".equals(uponUse) && account.getClearedPaymentAccountOUT() != null) {
            confirmation = true;
          }
        }
        // For payments with Amount ZERO always create an entry as no transaction will be created
        if (isPayment) {
          BigDecimal amount = new BigDecimal(strPaymentAmount);
          if (amount.signum() == 0) {
            confirmation = true;
          }
        }
      }
    } catch (Exception e) {
      return confirmation;
    } finally {
      OBContext.restorePreviousMode();
    }
    return confirmation;
  }

  public static String getUUID(ConnectionProvider connectionProvider) throws ServletException {
    String strSql = "";
    strSql = strSql + "       SELECT get_uuid() as name" + "       FROM dual";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "name");
      }
      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public void NewPaymentDetail(ConnectionProvider connectionProvider, String strNewPaymentDetailID,
      String strPaymentID, BigDecimal bgdAmount) throws ServletException {
    String strSql = "";
    strSql = strSql
        + "insert into fin_payment_detail(fin_payment_detail_id, ad_client_id, ad_org_id, isactive, createdby, created, updatedby, updated"
        + ", fin_payment_id, amount,refund,writeoffamt,c_glitem_id,isprepayment) values('"
        + strNewPaymentDetailID + "','" + OBContext.getOBContext().getCurrentClient().getId()
        + "','" + OBContext.getOBContext().getCurrentOrganization().getId() + "','Y','"
        + OBContext.getOBContext().getUser().getId() + "',now(),'"
        + OBContext.getOBContext().getUser().getId() + "',now(),'" + strPaymentID + "'," + bgdAmount
        + ",'N',0,null,'N')";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();

    } catch (SQLException e) {
      log4j.error(e.getMessage());

    } catch (Exception ex) {
      log4j.error(ex.getMessage());

    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
        log4j.error(ignore.getMessage());

      }
    }
  }

  public void NewPaymentDetail2(ConnectionProvider connectionProvider, String strNewPaymentDetailID,
      String strPaymentID, BigDecimal bgdAmount) throws ServletException {
    String strSql = "";
    strSql = strSql
        + "insert into fin_payment_detail(fin_payment_detail_id, ad_client_id, ad_org_id, isactive, createdby, created, updatedby, updated"
        + ", fin_payment_id, amount,refund,writeoffamt,c_glitem_id,isprepayment) values('"
        + strNewPaymentDetailID + "','" + OBContext.getOBContext().getCurrentClient().getId()
        + "','" + OBContext.getOBContext().getCurrentOrganization().getId() + "','Y','"
        + OBContext.getOBContext().getUser().getId() + "',now(),'"
        + OBContext.getOBContext().getUser().getId() + "',now(),'" + strPaymentID + "'," + bgdAmount
        + ",'N',0,null,'N')";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();

    } catch (SQLException e) {
      log4j.error(e.getMessage());

    } catch (Exception ex) {
      log4j.error(ex.getMessage());

    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
        log4j.error(ignore.getMessage());

      }
    }
  }

  public void UpdatePaymentSchedulDetail(ConnectionProvider connectionProvider,
      String strPaymentDetailID, BigDecimal bgdAmount, String strPartnerID,
      String strPaymenSchedulDetailID) throws ServletException {
    String strSql = "";
    /*
     * strSql = strSql +
     * "insert into fin_payment_scheduledetail(fin_payment_scheduledetail_id, ad_client_id, ad_org_id, isactive, createdby, created, updatedby, updated"
     * +
     * ", fin_payment_schedule_invoice, amount,writeoffamt,iscanceled,c_bpartner_id,fin_payment_detail_id) values("
     * + "get_uuid(),'" + OBContext.getOBContext().getCurrentClient().getId() + "','" +
     * OBContext.getOBContext().getCurrentOrganization().getId() + "','Y','" +
     * OBContext.getOBContext().getUser().getId() + "',now(),'" +
     * OBContext.getOBContext().getUser().getId() + "',now(),'" + strPaymenSchedulID + "'," +
     * bgdAmount + ",0,'N',null,'" + strPaymentDetailID + "')";
     */

    strSql = "update fin_payment_scheduledetail set amount=?, updated=now(), updatedby=?,fin_payment_detail_id=? where fin_payment_scheduledetail_id=?";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      st.setBigDecimal(1, bgdAmount);
      st.setString(2, OBContext.getOBContext().getUser().getId());
      st.setString(3, strPaymentDetailID);
      st.setString(4, strPaymenSchedulDetailID);
      updateCount = st.executeUpdate();
      st.close();

    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      // throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" +
      // e.getMessage());
      log4j.error(e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
      log4j.error(ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
        log4j.error(ignore.getMessage());
      }
    }
  }

  public void NewPaymentSchedulDetail(ConnectionProvider connectionProvider,
      String strPaymentSchedulDetailID, BigDecimal bgdAmount, String strPartnerID,
      String strPaymenSchedulID) throws ServletException {
    String strSql = "";

    /*
     * strSql = strSql +
     * "insert into fin_payment_scheduledetail(fin_payment_scheduledetail_id, ad_client_id, ad_org_id, isactive, createdby, created, updatedby, updated"
     * +
     * ", fin_payment_schedule_invoice, amount,writeoffamt,iscanceled,c_bpartner_id,fin_payment_detail_id) values("
     * + "get_uuid(),'" + OBContext.getOBContext().getCurrentClient().getId() + "','" +
     * OBContext.getOBContext().getCurrentOrganization().getId() + "','Y','" +
     * OBContext.getOBContext().getUser().getId() + "',now(),'" +
     * OBContext.getOBContext().getUser().getId() + "',now(),'" + strPaymenSchedulID + "'," +
     * bgdAmount + ",0,'N',null,'" + strPaymentDetailID + "')";
     */
    strSql = strSql
        + "insert into fin_payment_scheduledetail(fin_payment_scheduledetail_id, ad_client_id, ad_org_id, isactive, createdby, created, updatedby, updated"
        + ",fin_payment_schedule_invoice, amount,writeoffamt,iscanceled,c_bpartner_id,fin_payment_detail_id) "
        + "values('" + strPaymentSchedulDetailID + "',?,?,'Y',?,now(),?,now(),?,?,0,'N',?,null)";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      st.setString(1, OBContext.getOBContext().getCurrentClient().getId());
      st.setString(2, OBContext.getOBContext().getCurrentOrganization().getId());
      st.setString(3, OBContext.getOBContext().getUser().getId());
      st.setString(4, OBContext.getOBContext().getUser().getId());
      st.setString(5, strPaymenSchedulID);
      st.setBigDecimal(6, bgdAmount);
      st.setString(7, strPartnerID);

      updateCount = st.executeUpdate();
      st.close();

    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      // throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" +
      // e.getMessage());
      log4j.error(e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
      log4j.error(ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
        log4j.error(ignore.getMessage());
      }
    }
  }

  public void UpdatePayment(ConnectionProvider connectionProvider, String strPaymentID,
      BigDecimal bgdAmount, String Documentno) throws ServletException {
    String strSql = "";
    strSql = strSql + "update fin_payment set status = 'RDNC',processed='Y', amount = " + bgdAmount
        + ",posted='N',finacc_txn_convert_rate=1 where fin_payment_id ='" + strPaymentID + "'";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();

    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      // throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" +
      // e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
  }

  private OBError processPayment(FIN_Payment payments, String strAction, String strDifferenceAction,
      BigDecimal refundAmount, BigDecimal exchangeRate, JSONObject jsonparams, String comingFrom,
      Invoice newInvoice, BigDecimal bgdTotalPaid) throws Exception {
    ConnectionProvider conn = new DalConnectionProvider(true);
    VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    FIN_Payment payment = payments;
    BigDecimal assignedAmount = BigDecimal.ZERO;
    for (FIN_PaymentDetail paymentDetail : payment.getFINPaymentDetailList()) {
      assignedAmount = assignedAmount.add(paymentDetail.getAmount());
    }

    /*
     * if (assignedAmount.compareTo(payment.getAmount()) == -1) { FIN_PaymentScheduleDetail
     * refundScheduleDetail = getNewPaymentScheduleDetail( payment.getOrganization(),
     * payment.getAmount().subtract(assignedAmount)); getNewPaymentDetail(payment,
     * refundScheduleDetail, payment.getAmount().subtract(assignedAmount), BigDecimal.ZERO, false,
     * null); }
     */

    OBError message2 = FIN_AddPayment.processPayment(vars, conn,
        (strAction.equals("PRP") || strAction.equals("PPP")) ? "P" : "D", payment, comingFrom);
    String strNewPaymentMessage = OBMessageUtils
        .parseTranslation("@PaymentCreated@" + " " + payment.getDocumentNo()) + ".";
    if (!"Error".equalsIgnoreCase(message2.getType())) {
      message2.setMessage(strNewPaymentMessage + " " + message2.getMessage());
      message2.setType(message2.getType().toLowerCase());
    } else {
      conn = new DalConnectionProvider(true);
      OBDal.getInstance().getSession().clear();
      payment = OBDal.getInstance().get(FIN_Payment.class, payment.getId());
    }

    return message2;
  }

  public void UpdateInvoicePaid(ConnectionProvider connectionProvider, String strInvoiceID,
      BigDecimal bgdAmount) throws ServletException {
    String strSql = "";
    strSql = strSql + "update c_invoice set ispaid='Y', updated=now(), updatedby = '"
        + OBContext.getOBContext().getUser().getId() + "',totalpaid=" + bgdAmount
        + ",outstandingamt=0, daystilldue =0, dueamt=0, percentageoverdue=0 where c_invoice_id = '"
        + strInvoiceID + "'";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();

    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      // throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" +
      // e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
  }

  public void UpdateInvoiceSchedulePaid(ConnectionProvider connectionProvider, String strInvoiceID,
      BigDecimal bgdAmount) throws ServletException {
    String strSql = "";
    strSql = strSql + "update fin_payment_schedule set  updated=now(), updatedby = '"
        + OBContext.getOBContext().getUser().getId() + "',outstandingamt=0 ,paidamt=" + bgdAmount
        + "  where c_invoice_id = '" + strInvoiceID + "'";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();

    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      // throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" +
      // e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
  }

  public void DeleteInvoiceSchedulePaid(ConnectionProvider connectionProvider,
      String strScheludeDetail) throws ServletException {
    String strSql = "";
    strSql = strSql
        + "delete from fin_payment_scheduledetail  where fin_payment_detail_id is null  and fin_payment_schedule_invoice = '"
        + strScheludeDetail + "'";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();

    } catch (SQLException e) {
      log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      // throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" +
      // e.getMessage());
    } catch (Exception ex) {
      log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
  }

  public static String getValueOutStanding(ConnectionProvider connectionProvider,
      String strInvoicdID) throws ServletException {
    String strSql = "";
    strSql = strSql
        + "select to_char(amount) as resultado from fin_payment_scheduledetail ps where ps.fin_payment_schedule_invoice in (select fin_payment_schedule_id from fin_payment_schedule where c_invoice_id = '"
        + strInvoicdID + "') and ps.fin_payment_detail_id is null ";

    int updateCount = 0;
    ResultSet result;
    PreparedStatement st = null;
    String strReturn = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "resultado");
      }
      st.close();
      result.close();

    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      // throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" +
      // e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public static String updateInvoiceOrigin(ConnectionProvider connectionProvider,
      String strInvoicdID) throws ServletException {
    String strSql = "";
    strSql = strSql + "select ssfi_pymntscheduledetail_sum2('" + strInvoicdID
        + "') as resultado from dual ";

    int updateCount = 0;
    ResultSet result;
    PreparedStatement st = null;
    String strReturn = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "resultado");
      }
      st.close();
      result.close();

    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      // throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" +
      // e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public static String rollBackInvoice(ConnectionProvider connectionProvider, String strInvoicdID)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "select ssfi_revertInvoice('" + strInvoicdID + "') as resultado from dual ";

    int updateCount = 0;
    ResultSet result;
    PreparedStatement st = null;
    String strReturn = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "resultado");
      }
      st.close();
      result.close();

    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:" + e);
      // throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@" +
      // e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:" + ex);
      // throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public void insertFin_Transaction(BigDecimal bdcTotalInvoice, String selectedPaymentIds,
      String strFinancialAccountId, String strTransactionDate, String strFinBankStatementLineId,
      VariablesSecureApp vars, ConnectionProvider conn) {
    try {

      OBContext.setAdminMode();

      AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
      String strMessage = "";
      OBError msg = new OBError();

      String strTransactionType = "P";
      if (strTransactionType.equals("P")) { // Payment

        List<FIN_Payment> selectedPayments = FIN_Utility.getOBObjectList(FIN_Payment.class,
            selectedPaymentIds);

        for (FIN_Payment p : selectedPayments) {
          BigDecimal depositAmt = FIN_Utility.getDepositAmount(p.isReceipt(),
              p.getFinancialTransactionAmount());
          BigDecimal paymentAmt = FIN_Utility.getPaymentAmount(p.isReceipt(),
              p.getFinancialTransactionAmount());

          String description = null;
          if (p.getDescription() != null) {
            description = p.getDescription().replace("\n", ". ");
          }

          FIN_FinaccTransaction finTrans = getNewFinancialTransaction2(p.getOrganization(),
              OBDal.getInstance().get(FIN_FinancialAccount.class, strFinancialAccountId),
              TransactionsDao.getTransactionMaxLineNo(
                  OBDal.getInstance().get(FIN_FinancialAccount.class, strFinancialAccountId)) + 10,
              p, description, new Date(), null, p.isReceipt() ? "RDNC" : "PWNC", bdcTotalInvoice,
              bdcTotalInvoice, null, null, null, p.isReceipt() ? "BPD" : "BPW", new Date(),
              p.getCurrency(), p.getFinancialTransactionConvertRate(), p.getAmount());

          OBDal.getInstance().commitAndClose();
          try {
            // processTransactionError = processTransaction(vars, conn, "P", finTrans);
            processTransaction(finTrans, "P", this);
          } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
          }

          if (!"".equals(strFinBankStatementLineId)) {
            // matchBankStatementLine(vars, finTrans, strFinBankStatementLineId, dao);
          }
        }

        if (selectedPaymentIds != null && selectedPayments.size() > 0) {
          strMessage = selectedPayments.size() + " " + "@RowsInserted@";
        }

      }

      if ("".equals(strFinBankStatementLineId)) {
        // printPageClosePopUpAndRefreshParent(response, vars);
      } else {
        log4j.debug("Output: PopUp Response");

      }

    } catch (OBException e) {
      System.out.println(e.getMessage());

    } finally {
      OBContext.restorePreviousMode();

    }
  }

  public void processTransaction(FIN_FinaccTransaction transaction, String strAction,
      ConnectionProvider conProvider) {
    try {

      if (strAction.equals("P")) {
        // ***********************
        // Process Transaction
        // ***********************

        /*
         * if (documentEnabled && !FIN_Utility.isPeriodOpen(transaction.getClient().getId(),
         * AcctServer.DOCTYPE_FinAccTransaction, transaction.getOrganization().getId(),
         * OBDateUtils.formatDate(transaction.getDateAcct())) && orgLegalWithAccounting) {
         * OBDal.getInstance().rollbackAndClose(); return; }
         */
        final FIN_FinancialAccount financialAccount = transaction.getAccount();
        financialAccount.setCurrentBalance(financialAccount.getCurrentBalance()
            .add(transaction.getDepositAmount().subtract(transaction.getPaymentAmount())));
        transaction.setProcessed(true);
        FIN_Payment payment = transaction.getFinPayment();
        if (payment != null) {
          if (transaction.getBusinessPartner() == null) {
            transaction.setBusinessPartner(payment.getBusinessPartner());
          }
          payment.setStatus(payment.isReceipt() ? "RDNC" : "PWNC");
          transaction.setStatus(payment.isReceipt() ? "RDNC" : "PWNC");
          OBDal.getInstance().save(payment);
          if (transaction.getDescription() == null || "".equals(transaction.getDescription())) {
            transaction.setDescription(payment.getDescription());
          }
          Boolean invoicePaidold = false;
          for (FIN_PaymentDetail pd : payment.getFINPaymentDetailList()) {
            for (FIN_PaymentScheduleDetail psd : pd.getFINPaymentScheduleDetailList()) {
              invoicePaidold = psd.isInvoicePaid();
              if (!invoicePaidold) {
                if ((FIN_Utility.invoicePaymentStatus(payment).equals(payment.getStatus()))) {
                  psd.setInvoicePaid(true);
                }
                if (psd.isInvoicePaid()) {
                  FIN_Utility.updatePaymentAmounts(psd);
                  FIN_Utility.updateBusinessPartnerCredit(payment);
                }
                OBDal.getInstance().save(psd);
              }
            }
          }

        } else {
          transaction.setStatus(
              transaction.getDepositAmount().compareTo(transaction.getPaymentAmount()) > 0 ? "RDNC"
                  : "PWNC");
        }
        if (transaction.getForeignCurrency() != null
            && !transaction.getCurrency().equals(transaction.getForeignCurrency())
            && getConversionRateDocument(transaction).size() == 0) {
          insertConversionRateDocument(transaction);
        }
        transaction.setDateAcct(new Date());
        transaction.setTransactionDate(new Date());
        OBDal.getInstance().save(financialAccount);
        OBDal.getInstance().save(transaction);
        OBDal.getInstance().flush();
        // bundle.setResult(msg);
        OBDal.getInstance().commitAndClose();

      }

    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      System.out.println(e.getMessage());
    }

  }

  private List<ConversionRateDoc> getConversionRateDocument(FIN_FinaccTransaction transaction) {
    OBContext.setAdminMode();
    try {
      OBCriteria<ConversionRateDoc> obc = OBDal.getInstance()
          .createCriteria(ConversionRateDoc.class);
      obc.add(
          Restrictions.eq(ConversionRateDoc.PROPERTY_CURRENCY, transaction.getForeignCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_TOCURRENCY, transaction.getCurrency()));
      obc.add(Restrictions.eq(ConversionRateDoc.PROPERTY_FINANCIALACCOUNTTRANSACTION, transaction));
      return obc.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private ConversionRateDoc insertConversionRateDocument(FIN_FinaccTransaction transaction) {
    OBContext.setAdminMode();
    try {
      ConversionRateDoc newConversionRateDoc = OBProvider.getInstance()
          .get(ConversionRateDoc.class);
      newConversionRateDoc.setOrganization(transaction.getOrganization());
      newConversionRateDoc.setCurrency(transaction.getForeignCurrency());
      newConversionRateDoc.setToCurrency(transaction.getCurrency());
      newConversionRateDoc.setRate(transaction.getForeignConversionRate());
      newConversionRateDoc.setForeignAmount(transaction.getForeignAmount());
      newConversionRateDoc.setFinancialAccountTransaction(
          OBDal.getInstance().get(APRM_FinaccTransactionV.class, transaction.getId()));
      OBDal.getInstance().save(newConversionRateDoc);
      OBDal.getInstance().flush();
      return newConversionRateDoc;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public FIN_FinaccTransaction getNewFinancialTransaction2(Organization organization,
      FIN_FinancialAccount account, Long line, FIN_Payment payment, String description,
      Date accountingDate, GLItem glItem, String status, BigDecimal depositAmount,
      BigDecimal paymentAmount, Project project, Campaign campaing, ABCActivity activity,
      String transactionType, Date statementDate, Currency paymentCurrency, BigDecimal convertRate,
      BigDecimal sourceAmount) {
    FIN_FinaccTransaction finTrans = OBProvider.getInstance().get(FIN_FinaccTransaction.class);
    finTrans.setActive(true);
    finTrans.setOrganization(organization);
    finTrans.setCurrency(account.getCurrency());
    finTrans.setAccount(account);
    finTrans.setLineNo(line);
    if (payment != null) {
      OBDal.getInstance().refresh(payment);
    }
    finTrans.setFinPayment(payment);
    String truncateDescription = null;
    if (description != null) {
      truncateDescription = (description.length() > 255)
          ? description.substring(0, 252).concat("...").toString()
          : description.toString();
    }
    finTrans.setDescription(truncateDescription);
    finTrans.setDateAcct(accountingDate);
    finTrans.setGLItem(glItem);
    finTrans.setStatus(status);
    finTrans.setDepositAmount(depositAmount);
    finTrans.setPaymentAmount(paymentAmount);
    finTrans.setProject(project);
    finTrans.setSalesCampaign(campaing);
    finTrans.setActivity(activity);
    finTrans.setTransactionType(transactionType);
    finTrans.setTransactionDate(statementDate);

    if (paymentCurrency != null && !paymentCurrency.equals(finTrans.getCurrency())) {
      finTrans.setForeignCurrency(paymentCurrency);
      finTrans.setForeignConversionRate(convertRate);
      finTrans.setForeignAmount(sourceAmount);
    }
    OBDal.getInstance().save(finTrans);
    OBDal.getInstance().flush();

    return finTrans;
  }

  public void insert_FinTrans(ConnectionProvider connectionProvider, String strFinTransID,
      Organization organization, FIN_FinancialAccount account, Long line, FIN_Payment payment,
      String description, Date accountingDate, GLItem glItem, String status,
      BigDecimal depositAmount, BigDecimal paymentAmount, Project project, Campaign campaing,
      ABCActivity activity, String transactionType, Date statementDate, Currency paymentCurrency,
      BigDecimal convertRate, BigDecimal sourceAmount) throws ServletException {
    String strSql = "";
    strSql = strSql
        + "insert into fin_finacc_transaction(fin_finacc_transaction_id, ad_client_id, ad_org_id, isactive, createdby, created, updatedby, updated"
        + ", c_currency_id, fin_financial_account_id, line, fin_payment_id, dateacct, c_glitem_id,status,paymentamt, depositamt,trxtype,statementdate, description) values('"
        + strFinTransID + "','" + OBContext.getOBContext().getCurrentClient().getId() + "','"
        + OBContext.getOBContext().getCurrentOrganization().getId() + "','Y','"
        + OBContext.getOBContext().getUser().getId() + "',now(),'"
        + OBContext.getOBContext().getUser().getId() + "',now(),'" + account.getCurrency().getId()
        + "','" + account.getId() + "'," + line + ",'" + payment.getId() + "'," + "now(),"
        + (glItem != null ? "'" + glItem.getId() + "'" : "null") + ",'" + status + "',"
        + paymentAmount + "," + depositAmount + ",'" + transactionType + "',now(),'"
        + ((description.length() > 255) ? description.substring(0, 252).concat("...").toString()
            : description.toString())
        + "')";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      updateCount = st.executeUpdate();
      st.close();

    } catch (SQLException e) {
      log4j.error(e.getMessage());

    } catch (Exception ex) {
      log4j.error(ex.getMessage());

    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
        log4j.error(ignore.getMessage());

      }
    }
  }

  public static int getPaymentCount(ConnectionProvider connectionProvider, String strInvoiceID)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "       SELECT ssfi_returnpaymentcount('" + strInvoiceID
        + "') as countpay FROM dual";

    ResultSet result;
    int strReturn = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = Integer.parseInt(UtilSql.getValue(result, "countpay"));
      }
      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public static int getWithhSalesCount(ConnectionProvider connectionProvider, String strInvoiceID)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "       SELECT ssfi_returnwithsalescount('" + strInvoiceID
        + "') as countpay FROM dual";

    ResultSet result;
    int strReturn = 0;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = Integer.parseInt(UtilSql.getValue(result, "countpay"));
      }
      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public static String getTypeTransaction(ConnectionProvider connectionProvider,
      String strInvoiceID) throws ServletException {
    String strSql = "";
    strSql = strSql + "       SELECT issotrx as name  FROM c_invoice where c_invoice_id = '"
        + strInvoiceID + "'";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "name");
      }
      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }
}