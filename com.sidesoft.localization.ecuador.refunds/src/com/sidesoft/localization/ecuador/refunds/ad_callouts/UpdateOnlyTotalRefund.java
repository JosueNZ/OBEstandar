package com.sidesoft.localization.ecuador.refunds.ad_callouts;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.ServletException;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.db.DalConnectionProvider;
//import org.openbravo.erpCommon.businessUtility.BpartnerMiscData;
//importar el xsql - en estecaso desde eldirectorio - pakete

public class UpdateOnlyTotalRefund extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  protected void execute(CalloutInfo info) throws ServletException {

    // recupera el valor de la pagina web
    BigDecimal strTaxbaseamt = info.getBigDecimalParameter("inptaxbaseamt");
    BigDecimal strExemptbase = info.getBigDecimalParameter("inpexemptbase");
    BigDecimal strTaxedBasis = info.getBigDecimalParameter("inpuntaxedBasis");
    BigDecimal strTaxamt = info.getBigDecimalParameter("inptaxamt");
    BigDecimal strTaxice = info.getBigDecimalParameter("inptaxice");
    BigDecimal strTaxbase = info.getBigDecimalParameter("inptaxbase");
    BigDecimal strTaxbaserefund = info.getBigDecimalParameter("inptaxbaserefund");
    BigDecimal strGrantotal = info.getBigDecimalParameter("inpgrandtotal");
    String strInvoiceID = info.getStringParameter("inpcInvoiceId", null);

    Double strTotal = 0.00;

    strTotal = Double.valueOf(strTaxbaseamt.toString()) + Double.valueOf(strExemptbase.toString())
        + Double.valueOf(strTaxedBasis.toString()) + Double.valueOf(strTaxamt.toString())
        + Double.valueOf(strTaxice.toString()) + Double.valueOf(strTaxbase.toString())
        + Double.valueOf(strTaxbaserefund.toString());

    info.addResult("inpgrandtotal", strTotal);

  }
}
