package ec.com.sidesoft.importdata.payments.ad_process;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.common.businesspartner.BankAccount;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

import com.sidesoft.localization.ecuador.finances.ssfiBanktransfer;

import au.com.bytecode.opencsv.CSVReader;
import ec.com.sidesoft.importdata.payments.Simppys_PaymentDataUpload;
import ec.com.sidesoft.importdata.payments.Simppys_PaymentDetail;
import ec.com.sidesoft.importdata.payments.utils.Simppys_Helper;

public class Simppys_LoadLines extends DalBaseProcess {
    private final Logger logger = Logger.getLogger(Simppys_LoadLines.class);

    final String attachPath = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("attach.path");
    final String tableId = "37A80EBCAA134F2EB34A96B544382302"; // simppys_paymentdataupload
    final String dataType = "text/csv";

    @Override
    public void doExecute(ProcessBundle bundle) throws Exception {
        OBError msg = new OBError();

        try {
            OBContext.setAdminMode(true);
            logger.info("Begin LoadLines");

            String id = (String) bundle.getParams().get("Simppys_PaymentDataUpload_ID");
            Simppys_PaymentDataUpload paymentDataUpload = OBDal.getInstance().get(Simppys_PaymentDataUpload.class, id);

            process(paymentDataUpload);

            OBDal.getInstance().commitAndClose();

            msg.setType("Success");
            msg.setTitle(OBMessageUtils.messageBD("Success"));
        } catch (final Exception e) {
            OBDal.getInstance().rollbackAndClose();
            String message = Simppys_Helper.getErrorMessage(logger, e);
            logger.error(message);

            msg.setTitle(OBMessageUtils.messageBD("Error"));
            msg.setType("Error");
            msg.setMessage(message);
        } finally {
            bundle.setResult(msg);
            OBContext.restorePreviousMode();
            logger.info("Finish LoadLines");
        }
    }

    private void process(Simppys_PaymentDataUpload paymentDataUpload) throws Exception {
        Table table = OBDal.getInstance().get(Table.class, tableId);
        Attachment attach = getAttachment(paymentDataUpload.getId(), table);
        if (attach == null) {
            throw new OBException("Archivo CSV no encontrado");
        }
        String filename = attachPath + File.separator + attach.getPath() + File.separator + attach.getName();
        logger.info(filename);
        List<Map<String, String>> data = loadCSV(filename, 22);
        validate(data);
        insert(paymentDataUpload, data);
    }

    private Attachment getAttachment(String recordId, Table table) throws Exception {
        Attachment attach = null;

        OBCriteria<Attachment> attachmentList = OBDal.getInstance().createCriteria(Attachment.class);
        attachmentList.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
        attachmentList.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordId));
        attachmentList.add(Restrictions.eq(Attachment.PROPERTY_DATATYPE, dataType));
        attachmentList.setFilterOnReadableOrganization(false);
        attachmentList.uniqueResult();

        if (attachmentList.list().size() > 0) {
            attach = attachmentList.list().get(0);
        }

        return attach;
    }

    private List<Map<String, String>> loadCSV(String filename, int numberOfColumns) throws Exception {
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        CSVReader csvReader = null;
        try {
            csvReader = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"), ',', '\"', '\\', 0,
                    false, true);
            List<String[]> csv = csvReader.readAll();
            List<String> title = new ArrayList<String>();
            int i = 0;
            for (String[] row : csv) {
                int j = 0;
                Map<String, String> record = new HashMap<String, String>();
                String cells = "";
                for (String cell : row) {
                    if (i == 0) {
                        title.add(cell != null ? cell.trim() : cell);
                    } else {
                        record.put(title.get(j), cell != null ? cell.trim() : cell);
                    }
                    cells += cell + "\t";
                    j++;
                }
                logger.info(cells);
                if (j != numberOfColumns) {
                    throw new OBException("El numero de columnas no coincide con el formato");
                }
                if (i > 0) {
                    data.add(record);
                }
                i++;
            }
            if (i == 0) {
                throw new OBException("No se encontraron datos en el archivo");
            }
        } finally {
            if (csvReader != null) {
                csvReader.close();
            }
        }
        return data;
    }

    private void validate(List<Map<String, String>> data) throws Exception {
        int i = 2;
        for (Map<String, String> row : data) {
            String message = "";

            if (StringUtils.isEmpty(row.get("Empresa"))) {
                message += "La [Empresa] es obligatoria<br />";
            }

            if (StringUtils.isEmpty(row.get("Cliente"))) {
                message += "El [Cliente] es obligatorio<br />";
            }

            if (StringUtils.isEmpty(row.get("Tipo de Documento"))) {
                message += "El [Tipo de Documento] es obligatorio<br />";
            }

            if (StringUtils.isEmpty(row.get("Fecha de Pago"))) {
                message += "La [Fecha de Pago] es obligatoria<br />";
            } else if (Simppys_Helper.getDate(row.get("Fecha de Pago")) == null) {
                message += "Formato de fecha invalido para [Fecha de Pago]<br />";
            }

            if (StringUtils.isEmpty(row.get("Metodo de Pago"))) {
                message += "El [Metodo de Pago] es obligatorio<br />";
            }

            if (StringUtils.isEmpty(row.get("Cuenta Financiera"))) {
                message += "La [Cuenta Financiera] es obligatoria<br />";
            }

            if (StringUtils.isEmpty(row.get("Moneda"))) {
                message += "La [Moneda] es obligatoria<br />";
            }

            if (StringUtils.isEmpty(row.get("Valor"))) {
                message += "El [Valor] es obligatorio<br />";
            } else if (Simppys_Helper.getBigDecimal(row.get("Valor")) == null) {
                message += "Formato numerico invalido para [Valor]<br />";
            }

            if (StringUtils.isEmpty(row.get("Factura"))) {
                message += "La [Factura] es obligatorio<br />";
            }

            if (StringUtils.isNotEmpty(row.get("Fecha de Cuota"))
                    && Simppys_Helper.getDate(row.get("Fecha de Cuota")) == null) {
                message += "Formato de fecha invalido para [Fecha de Cuota]<br />";
            }

            String onlyPayment = row.get("Solo Cobro");
            if (StringUtils.isEmpty(onlyPayment)) {
                message += "El [Solo Cobro] es obligatorio<br />";
            } else if (!(onlyPayment.trim().equals("Y") || onlyPayment.trim().equals("N"))) {
                message += "Valor invalido para [Solo Cobro]<br />";
            }

            if (!message.isEmpty()) {
                throw new OBException("Fila: " + i + "<br />" + message);
            }
            i++;
        }
    }

    private void insert(Simppys_PaymentDataUpload paymentDataUpload, List<Map<String, String>> data) throws Exception {
        int i = 2;
        for (Map<String, String> row : data) {
            String cell = row.get("Empresa").trim();
            OBCriteria<Organization> qOrg = OBDal.getInstance().createCriteria(Organization.class);
            qOrg.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
            qOrg.add(Restrictions.sqlRestriction("TRIM(name) = ?", cell, StringType.INSTANCE));
            qOrg.setMaxResults(1);
            if (qOrg.list().size() == 0) {
                throw new OBException("Fila: " + i + "<br />Empresa [" + cell + "] no encontrada");
            }
            Organization org = qOrg.list().get(0);

            cell = row.get("Cliente").trim();
            OBCriteria<BusinessPartner> qBPartner = OBDal.getInstance().createCriteria(BusinessPartner.class);
            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
            qBPartner.add(Restrictions.sqlRestriction("TRIM(value) = ?", cell, StringType.INSTANCE));
//            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, cell));
            qBPartner.setMaxResults(1);
            if (qBPartner.list().size() == 0) {
                throw new OBException("Fila: " + i + "<br />Cliente [" + cell + "] no encontrado");
            }
            BusinessPartner bPartner = qBPartner.list().get(0);

            cell = row.get("Tipo de Documento").trim();
            OBCriteria<DocumentType> qDocType = OBDal.getInstance().createCriteria(DocumentType.class);
            qDocType.add(Restrictions.eq(DocumentType.PROPERTY_ACTIVE, true));
            qDocType.add(Restrictions.sqlRestriction("TRIM(name) = ?", cell, StringType.INSTANCE));
            qDocType.setMaxResults(1);
            if (qDocType.list().size() == 0) {
                throw new OBException("Fila: " + i + "<br />Tipo de Documento [" + cell + "] no encontrado");
            }
            DocumentType docType = qDocType.list().get(0);

            cell = row.get("Metodo de Pago").trim();
            OBCriteria<FIN_PaymentMethod> qPMethod = OBDal.getInstance().createCriteria(FIN_PaymentMethod.class);
            qPMethod.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_ACTIVE, true));
            qPMethod.add(Restrictions.sqlRestriction("TRIM(name) = ?", cell, StringType.INSTANCE));
            qPMethod.setMaxResults(1);
            if (qPMethod.list().size() == 0) {
                throw new OBException("Fila: " + i + "<br />Metodo de Pago [" + cell + "] no encontrado");
            }
            FIN_PaymentMethod pMethod = qPMethod.list().get(0);

            cell = row.get("Cuenta Financiera").trim();
            OBCriteria<FIN_FinancialAccount> qFAccount = OBDal.getInstance().createCriteria(FIN_FinancialAccount.class);
            qFAccount.add(Restrictions.eq(FIN_FinancialAccount.PROPERTY_ACTIVE, true));
            qFAccount.add(Restrictions.sqlRestriction("TRIM(name) = ?", cell, StringType.INSTANCE));
            qFAccount.setMaxResults(1);
            if (qFAccount.list().size() == 0) {
                throw new OBException("Fila: " + i + "<br />Cuenta Financiera [" + cell + "] no encontrada");
            }
            FIN_FinancialAccount fAccount = qFAccount.list().get(0);

            cell = row.get("Moneda").trim();
            OBCriteria<Currency> qCurrency = OBDal.getInstance().createCriteria(Currency.class);
            qCurrency.add(Restrictions.eq(Currency.PROPERTY_ACTIVE, true));
            qCurrency.add(Restrictions.sqlRestriction("TRIM(iso_code) = ?", cell, StringType.INSTANCE));
            qCurrency.setMaxResults(1);
            if (qCurrency.list().size() == 0) {
                throw new OBException("Fila: " + i + "<br />Moneda [" + cell + "] no encontrada");
            }
            Currency currency = qCurrency.list().get(0);

            int precision = currency.getStandardPrecision().intValue();

            cell = row.get("Factura").trim();
            OBCriteria<Invoice> qInvoice = OBDal.getInstance().createCriteria(Invoice.class);
            qInvoice.add(Restrictions.eq(Invoice.PROPERTY_ACTIVE, true));
            if (paymentDataUpload.getPaymentType().equals("C")) {
                qInvoice.add(Restrictions.sqlRestriction("TRIM(documentno) = ?", cell, StringType.INSTANCE));
                qInvoice.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, true));

            } else {
                qInvoice.add(Restrictions.sqlRestriction("TRIM(poreference) = ?", cell, StringType.INSTANCE));
                qInvoice.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, false));
            }
            qInvoice.setMaxResults(1);
            if (qInvoice.list().size() == 0) {
                throw new OBException("Fila: " + i + "<br />Factura [" + cell + "] no encontrada");
            }
            Invoice invoice = qInvoice.list().get(0);

            if (!invoice.getBusinessPartner().getId().equals(bPartner.getId())) {
                throw new OBException("Fila: " + i + "<br />Cliente [" + bPartner.getName()
                        + "] no coincide con el cliente de la factura");
            }

            Simppys_PaymentDetail paymentDetail = OBProvider.getInstance().get(Simppys_PaymentDetail.class);
            paymentDetail.setOrganization(org);
            paymentDetail.setClient(org.getClient());
            paymentDetail.setSimppysPaymentDataUpload(paymentDataUpload);
            paymentDetail.setBusinessPartner(bPartner);
            paymentDetail.setPaymentDocType(docType);
            paymentDetail.setDocumentNo(Simppys_Helper.getString(row.get("No. de Documento")));
            paymentDetail.setPaymentDate(Simppys_Helper.getDate(row.get("Fecha de Pago")));
            paymentDetail.setReferenceNo(Simppys_Helper.getString(row.get("No. de Referencia")));
            paymentDetail.setPaymentMethod(pMethod);
            paymentDetail.setFinancialAccount(fAccount);
            paymentDetail.setCurrency(currency);
            paymentDetail.setDescription(Simppys_Helper.getString(row.get("Descripcion")));
            BigDecimal amount = Simppys_Helper.getBigDecimal(row.get("Valor")).setScale(precision,
                    RoundingMode.HALF_UP);
            paymentDetail.setAmount(amount);
            paymentDetail.setInvoice(invoice);
            paymentDetail.setDueDate(Simppys_Helper.getDate(row.get("Fecha de Cuota")));
            paymentDetail.setOnlyPayment(row.get("Solo Cobro").trim().equals("Y"));

            cell = Simppys_Helper.getString(row.get("Centro de Costos"));
            if (cell != null) {
                OBCriteria<Costcenter> qCCenter = OBDal.getInstance()
                        .createCriteria(Costcenter.class);
                qCCenter.add(Restrictions.eq(Costcenter.PROPERTY_ACTIVE, true));
                qCCenter.add(Restrictions.sqlRestriction("TRIM(name) = ?", cell, StringType.INSTANCE));
                qCCenter.setMaxResults(1);
                if (qCCenter.list().size() == 0) {
                    throw new OBException("Fila: " + i + "<br />Centro de Costos [" + cell + "] no encontrado");
                }
                Costcenter costCenter = qCCenter.list().get(0);
                paymentDetail.setCostCenter(costCenter);
            }

            cell = Simppys_Helper.getString(row.get("Usuario 1"));
            if (cell != null) {
                OBCriteria<UserDimension1> qUser1 = OBDal.getInstance()
                        .createCriteria(UserDimension1.class);
                qUser1.add(Restrictions.eq(UserDimension1.PROPERTY_ACTIVE, true));
                qUser1.add(Restrictions.sqlRestriction("TRIM(name) = ?", cell, StringType.INSTANCE));
                qUser1.setMaxResults(1);
                if (qUser1.list().size() == 0) {
                    throw new OBException("Fila: " + i + "<br />Usuario 1 [" + cell + "] no encontrado");
                }
                UserDimension1 user1 = qUser1.list().get(0);
                paymentDetail.setStDimension(user1);
            }

            cell = Simppys_Helper.getString(row.get("Dimension 2"));
            if (cell != null) {
                OBCriteria<UserDimension2> qUser2 = OBDal.getInstance()
                        .createCriteria(UserDimension2.class);
                qUser2.add(Restrictions.eq(UserDimension2.PROPERTY_ACTIVE, true));
                qUser2.add(Restrictions.sqlRestriction("TRIM(name) = ?", cell, StringType.INSTANCE));
                qUser2.setMaxResults(1);
                if (qUser2.list().size() == 0) {
                    throw new OBException("Fila: " + i + "<br />Dimension 2 [" + cell + "] no encontrado");
                }
                UserDimension2 user2 = qUser2.list().get(0);
                paymentDetail.setNdDimension(user2);
            }

            if (paymentDataUpload.getPaymentType().equals("C")) {
                cell = Simppys_Helper.getString(row.get("Banco Cliente"));
                if (cell != null) {
                    OBCriteria<ssfiBanktransfer> qBTransfer = OBDal.getInstance()
                            .createCriteria(ssfiBanktransfer.class);
                    qBTransfer.add(Restrictions.eq(ssfiBanktransfer.PROPERTY_ACTIVE, true));
                    qBTransfer.add(Restrictions.sqlRestriction("TRIM(name) = ?", cell, StringType.INSTANCE));
                    qBTransfer.setMaxResults(1);
                    if (qBTransfer.list().size() == 0) {
                        throw new OBException("Fila: " + i + "<br />Banco Cliente [" + cell + "] no encontrado");
                    }
                    ssfiBanktransfer bTransfer = qBTransfer.list().get(0);
                    paymentDetail.setSsfiBanktransfer(bTransfer);
                }

                paymentDetail.setCheckno(Simppys_Helper.getString(row.get("No. de Cheque")));
                paymentDetail.setDepositno(Simppys_Helper.getString(row.get("No. de Deposito")));

                cell = Simppys_Helper.getString(row.get("Cobrado por"));
                if (cell != null) {
                    OBCriteria<BusinessPartner> qSeller = OBDal.getInstance().createCriteria(BusinessPartner.class);
                    qSeller.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
                    qSeller.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, cell));
                    qSeller.setMaxResults(1);
                    if (qSeller.list().size() == 0) {
                        throw new OBException("Fila: " + i + "<br />Cobrado por [" + cell + "] no encontrado");
                    }
                    BusinessPartner seller = qSeller.list().get(0);
                    paymentDetail.setSeller(seller);
                }
            } else {
                cell = Simppys_Helper.getString(row.get("Cuenta Bancaria"));
                if (cell != null) {
                    OBCriteria<BankAccount> qBankAccount = OBDal.getInstance().createCriteria(BankAccount.class);
                    qBankAccount.add(Restrictions.eq(BankAccount.PROPERTY_ACTIVE, true));
                    qBankAccount.add(Restrictions.sqlRestriction("TRIM(name) = ?", cell, StringType.INSTANCE));
                    qBankAccount.setMaxResults(1);
                    if (qBankAccount.list().size() == 0) {
                        throw new OBException("Fila: " + i + "<br />Cuenta Bancaria [" + cell + "] no encontrada");
                    }
                    BankAccount bAccount = qBankAccount.list().get(0);
                    paymentDetail.setPartnerBankAccount(bAccount);
                }

            }

            OBDal.getInstance().save(paymentDetail);
            OBDal.getInstance().flush();
        }
    }

}
