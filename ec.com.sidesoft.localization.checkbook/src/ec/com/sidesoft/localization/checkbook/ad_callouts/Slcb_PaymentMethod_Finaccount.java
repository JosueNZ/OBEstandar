package ec.com.sidesoft.localization.checkbook.ad_callouts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

import ec.com.sidesoft.localization.checkbook.SlcbCheckBook;
import ec.com.sidesoft.localization.checkbook.SlcbCheckBookLine;

public class Slcb_PaymentMethod_Finaccount  extends SimpleCallout {

	  @Override
	  protected void execute(CalloutInfo info) throws ServletException {

	    String tabId = info.getTabId();
	    boolean isVendorTab = "224".equals(tabId);
	    String finIsReceipt = info.getStringParameter("inpisreceipt", null);
	    boolean isPaymentOut = isVendorTab || "N".equals(finIsReceipt);
	    String strOrgId = info.getStringParameter("inpadOrgId", IsIDFilter.instance);
	    
	    String strCheckbook = (info.getStringParameter("inpemSlcbCheckbooklineId", null)==null || info.getStringParameter("inpemSlcbCheckbooklineId", null).equals(""))?"ND": info.getStringParameter("inpemSlcbCheckbooklineId", null);

	    String strSelectedPaymentMethod = info.getStringParameter(isVendorTab ? "inppoPaymentmethodId"
	        : "inpfinPaymentmethodId", IsIDFilter.instance);

	    FIN_PaymentMethod paymentMethod = OBDal.getInstance().get(FIN_PaymentMethod.class,
	        strSelectedPaymentMethod);

	    String strSelectedFinancialAccount = info.getStringParameter(
	        isVendorTab ? "inppoFinancialAccountId" : "inpfinFinancialAccountId", IsIDFilter.instance);

	    FIN_FinancialAccount financialAccount = OBDal.getInstance().get(FIN_FinancialAccount.class,
	        strSelectedFinancialAccount);

	    boolean isMultiCurrencyEnabled = false;

	    if (paymentMethod != null && financialAccount != null) {
	      OBCriteria<FinAccPaymentMethod> obc = OBDal.getInstance().createCriteria(
	          FinAccPaymentMethod.class);
	      // (paymentmethod, financial_account) is unique
	      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
	      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, financialAccount));
	      obc.add(Restrictions.in("organization.id", OBContext.getOBContext()
	          .getOrganizationStructureProvider().getNaturalTree(strOrgId)));

	      FinAccPaymentMethod selectedAccPaymentMethod = (FinAccPaymentMethod) obc.uniqueResult();
	      if (selectedAccPaymentMethod != null) {
	        if (isPaymentOut) {
	          isMultiCurrencyEnabled = selectedAccPaymentMethod.isPayoutAllow()
	              && selectedAccPaymentMethod.isPayoutIsMulticurrency();
	        } else {
	          isMultiCurrencyEnabled = selectedAccPaymentMethod.isPayinAllow()
	              && selectedAccPaymentMethod.isPayinIsMulticurrency();
	        }
	      }
	    }
	    
	    if (paymentMethod != null && financialAccount != null) {
		      OBCriteria<FinAccPaymentMethod> obc = OBDal.getInstance().createCriteria(
		          FinAccPaymentMethod.class);
		      // (paymentmethod, financial_account) is unique
		      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
		      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, financialAccount));
		      obc.add(Restrictions.in("organization.id", OBContext.getOBContext()
		          .getOrganizationStructureProvider().getNaturalTree(strOrgId)));
		      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_SLCBISGENERATECHECK, true ));

		      
		      FinAccPaymentMethod selectedAccPaymentMethod = (FinAccPaymentMethod) obc.uniqueResult();
		      if (selectedAccPaymentMethod != null) {
		        if (isPaymentOut) {
		          isMultiCurrencyEnabled = selectedAccPaymentMethod.isPayoutAllow()
		              && selectedAccPaymentMethod.isPayoutIsMulticurrency();
		        } else {
		          isMultiCurrencyEnabled = selectedAccPaymentMethod.isPayinAllow()
		              && selectedAccPaymentMethod.isPayinIsMulticurrency();
		        }
		      }
		      
		      OBCriteria<SlcbCheckBook> obcCheck = OBDal.getInstance().createCriteria(
		    		  SlcbCheckBook.class);
		      obcCheck.add(Restrictions.eq(SlcbCheckBook.PROPERTY_ACTIVE, true));
		      obcCheck.add(Restrictions.eq(SlcbCheckBook.PROPERTY_FINFINANCIALACCOUNT, financialAccount));
		      
		      if (obcCheck.count()==1 && strCheckbook.equals("ND")) {
		    	  
		    	  SlcbCheckBook checkbook = OBDal.getInstance().get(SlcbCheckBook.class,
		    			  obcCheck.list().get(0).getId());
		    	  
		    	  Object obj[] = new Object[2]; 
		    	  obj = getCheckbookLineID(this, checkbook.getId());
		    	  
		    	  if (obj.length>0) {
		    		  
			    	  SlcbCheckBookLine checkbookLine = OBDal.getInstance().get(SlcbCheckBookLine.class,
			    			  String.valueOf(obj[1]));
			    	  
			    	  if (checkbookLine!=null) {
		    		  
			    	  String strDocumentNo = info.getStringParameter("inpdocumentno",null);
			    	  
			    	  
			    	  checkbookLine.setStatus("U");
			    	  checkbookLine.setPaymentno(strDocumentNo);
			    	  
			    	  OBDal.getInstance().save(checkbookLine);
			    	  OBDal.getInstance().flush();
			    	  
			    	  
		    		  info.addResult("inpemSlcbCheckbooklineId", checkbookLine.getId().toString());
		    		  info.addResult("inpreferenceno", String.valueOf(obj[0]));
			    	  }
		    		  
		    	  }
	    	
		      } 
		      
		}
	    
	    info.addResult("inpismulticurrencyenabled", isMultiCurrencyEnabled ? "Y" : "N");
	    
	    
	  }
	  
	  public static Object[] getCheckbookLineID(ConnectionProvider connectionProvider, String strCheckbook)
		      throws ServletException {
		  
		    Object obj[] = new Object[2]; 
		    String strSql = "";
		    
		    strSql =" select to_char(checkno) as checkno,slcb_checkbookline_id as checkbokklineid from slcb_checkbookline where slcb_checkbook_id = '" + strCheckbook +  "' and status is null\n" + 
		        "and line in ( " + 
		        "select min(line) from slcb_checkbookline where slcb_checkbook_id = '" + strCheckbook +  "' and status is null\n" + 
		        ")" ;

		    ResultSet result;
		    PreparedStatement st = null;

		    try {
		      st = connectionProvider.getPreparedStatement(strSql);

		      result = st.executeQuery();
		      if (result.next()) {
		        //strReturn = UtilSql.getValue(result, "checkno");
		        obj[0] = UtilSql.getValue(result, "checkno");
		        obj[1] = UtilSql.getValue(result, "checkbokklineid");
		      }
		      result.close();
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
		    return (obj);
		  }
}
