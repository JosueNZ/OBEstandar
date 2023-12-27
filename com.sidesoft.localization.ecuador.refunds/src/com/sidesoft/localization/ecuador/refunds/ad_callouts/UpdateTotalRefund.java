package com.sidesoft.localization.ecuador.refunds.ad_callouts;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
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

public class UpdateTotalRefund extends SimpleCallout {
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

    Date date;
    try {
      OBContext.setAdminMode(true);
      date = OBDal.getInstance().get(Invoice.class, strInvoiceID).getInvoiceDate();
    } catch (Exception e) {
      date = new Date();
    } finally {
      OBContext.restorePreviousMode();
    }

    double dblTax = 0;
    try {
      org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);

      dblTax = Double.valueOf(getTax(cp, strInvoiceID));
    } catch (Exception e2) {

    }
    
    DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
    simbolos.setDecimalSeparator('.');
    DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);

    if (dblTax != 0) {

      BigDecimal bgdCalcTax = BigDecimal.ZERO;
      bgdCalcTax = (strTaxbaserefund.multiply(new BigDecimal(dblTax)))
          .divide(new BigDecimal("100"));

      info.addResult("inptaxamt", String.valueOf(formateador.format(bgdCalcTax.doubleValue())));

      strTaxamt = new BigDecimal(formateador.format(bgdCalcTax.doubleValue()).toString());
    }

    strTotal = Double.valueOf(formateador.format(strTaxbaseamt.doubleValue())) + Double.valueOf(formateador.format(strExemptbase.doubleValue()))
        + Double.valueOf(formateador.format(strTaxedBasis.doubleValue())) + Double.valueOf(formateador.format(strTaxamt.doubleValue()))
        + Double.valueOf(formateador.format(strTaxice.doubleValue())) + Double.valueOf(formateador.format(strTaxbase.doubleValue()))
        + Double.valueOf(formateador.format(strTaxbaserefund.doubleValue()));

    info.addResult("inpgrandtotal", strTotal);

  }

  public static String getTax(ConnectionProvider connectionProvider, String v_fecha)
      throws ServletException {
    String strSql = "";
    strSql = strSql
        + "   select coalesce( "
        + " (select to_char(e.rate) from c_tax e where  e.rate <> 0  and e.istaxdeductable ='Y' and e.isactive='Y'"
        + " and validfrom = (select max(validfrom) from c_tax e where e.rate <> 0  and e.isactive='Y' and e.istaxdeductable ='Y' and validfrom <= dateinvoiced"
        + " )),to_char(0))" + " as tax_res from c_invoice where c_invoice_id = '" + v_fecha + "'";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "tax_res");
      }
      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException("@CODE=" + Integer.toString(e.getErrorCode()) + "@"
          + e.getMessage());
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
