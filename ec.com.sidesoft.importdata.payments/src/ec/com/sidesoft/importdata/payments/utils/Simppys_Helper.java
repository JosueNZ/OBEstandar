package ec.com.sidesoft.importdata.payments.utils;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;

public abstract class Simppys_Helper {

    static public String getErrorMessage(Logger logger, Exception e) {
        Throwable throwable = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(throwable.getMessage()).getMessage();
        logger.error(message);
        return message;
    }

    static public String getString(String value) throws Exception {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    static public BigDecimal getBigDecimal(String value)
            throws Exception {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    static public Date getDate(String value) throws Exception {
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return df.parse(value.trim());
        } catch (Exception e) {
            return null;
        }
    }

    static public String getDateString(Date date) throws IOException {
        if (date == null) {
            return null;
        } else {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            return df.format(date);
        }

    }

}
