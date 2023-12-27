package com.sidesoft.hrm.payroll.ad_process;

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
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;

import com.sidesoft.hrm.payroll.Concept;
import com.sidesoft.hrm.payroll.Sspr_OtherTaxIncome;
import com.sidesoft.hrm.payroll.Sspr_OtherTaxIncomeLine;

import au.com.bytecode.opencsv.CSVReader;

public class OtherTaxIncomeLoadLines extends DalBaseProcess {
    private final Logger logger = Logger.getLogger(OtherTaxIncomeLoadLines.class);

    final String attachPath = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("attach.path");
    final String tableId = "6017C3D7B42348A0A9F2B4FB3C176B86"; // sspr_other_tax_income
    final String dataType = "text/csv";

    @Override
    public void doExecute(ProcessBundle bundle) throws Exception {
        OBError msg = new OBError();

        try {
            OBContext.setAdminMode(true);
            logger.info("Begin OtherTaxIncomeLoadLines");

            String id = (String) bundle.getParams().get("Sspr_Other_Tax_Income_ID");
            Sspr_OtherTaxIncome otherTaxIncome = OBDal.getInstance().get(Sspr_OtherTaxIncome.class, id);

            process(otherTaxIncome);

            OBDal.getInstance().commitAndClose();

            msg.setType("Success");
            msg.setTitle(OBMessageUtils.messageBD("Success"));
        } catch (final Exception e) {
            OBDal.getInstance().rollbackAndClose();
            String message = getErrorMessage(logger, e);
            logger.error(message);

            msg.setTitle(OBMessageUtils.messageBD("Error"));
            msg.setType("Error");
            msg.setMessage(message);
        } finally {
            bundle.setResult(msg);
            OBContext.restorePreviousMode();
            logger.info("Finish OtherTaxIncomeLoadLines");
        }
    }

    private void process(Sspr_OtherTaxIncome otherTaxIncome) throws Exception {
        Table table = OBDal.getInstance().get(Table.class, tableId);
        Attachment attach = getAttachment(otherTaxIncome.getId(), table);
        if (attach == null) {
            throw new OBException("Archivo CSV no encontrado");
        }
        String filename = attachPath + File.separator + attach.getPath() + File.separator + attach.getName();
        logger.info(filename);
        List<Map<String, String>> data = loadCSV(filename, 4);
        validate(data);
        insert(otherTaxIncome, data);
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

            if (StringUtils.isEmpty(row.get("Documento"))) {
                message += "El [Documento] es obligatorio<br />";
            }

            if (StringUtils.isEmpty(row.get("Identificador"))) {
                message += "El [Identificador] es obligatorio<br />";
            }

            if (StringUtils.isEmpty(row.get("Concepto"))) {
                message += "El [Concepto] es obligatorio<br />";
            }

            if (StringUtils.isEmpty(row.get("Valor"))) {
                message += "El [Valor] es obligatorio<br />";
            }
            if (getBigDecimal(row.get("Valor")) == null) {
                message += "Formato numerico invalido para [Valor]<br />";
            }

            if (!message.isEmpty()) {
                throw new OBException("Fila: " + i + "<br />" + message);
            }
            i++;
        }
    }

    private void insert(Sspr_OtherTaxIncome otherTaxIncome, List<Map<String, String>> data) throws Exception {
        int i = 2;
        for (Map<String, String> row : data) {
            String documentNo = row.get("Documento").trim();

            String cell = row.get("Identificador").trim();
            OBCriteria<BusinessPartner> qBPartner = OBDal.getInstance().createCriteria(BusinessPartner.class);
            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SSPRSTATUS, "A"));
            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_EMPLOYEE, true));
            qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID, cell));
            qBPartner.setMaxResults(1);
            if (qBPartner.list().size() == 0) {
                throw new OBException("Fila: " + i + "<br />Empleado con identificador [" + cell + "] no encontrado");
            }
            BusinessPartner bPartner = qBPartner.list().get(0);

            cell = row.get("Concepto").trim();
            OBCriteria<Concept> qConcept = OBDal.getInstance().createCriteria(Concept.class);
            qConcept.add(Restrictions.eq(Concept.PROPERTY_ACTIVE, true));
            qConcept.add(Restrictions.eq(Concept.PROPERTY_VALUE, cell));
            qConcept.add(Restrictions.eq(Concept.PROPERTY_CONCEPTSUBTYPE, "In"));
            qConcept.add(Restrictions.isNotNull(Concept.PROPERTY_CODEFORMULARY107));
            qConcept.setMaxResults(1);
            if (qConcept.list().size() == 0) {
                throw new OBException("Fila: " + i + "<br />Concepto [" + cell + "] no encontrado");
            }
            Concept concept = qConcept.list().get(0);

            BigDecimal amount = getBigDecimal(row.get("Valor")).setScale(2,
                    RoundingMode.HALF_UP);

            Sspr_OtherTaxIncomeLine otherTaxIncomeLine = OBProvider.getInstance().get(Sspr_OtherTaxIncomeLine.class);
            otherTaxIncomeLine.setSsprOtherTaxIncome(otherTaxIncome);
            otherTaxIncomeLine.setTaxID(bPartner.getTaxID());
            otherTaxIncomeLine.setBusinessPartner(bPartner);
            otherTaxIncomeLine.setConcept(concept);
            otherTaxIncomeLine.setAmount(amount);

            OBDal.getInstance().save(otherTaxIncomeLine);
            OBDal.getInstance().flush();
        }
    }

    private BigDecimal getBigDecimal(String value)
            throws Exception {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String getErrorMessage(Logger logger, Exception e) {
        Throwable throwable = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(throwable.getMessage()).getMessage();
        logger.error(message);
        return message;
    }

}
