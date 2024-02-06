/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.sidesoft.localization.ecuador.finances.ad_process;

import java.sql.CallableStatement;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder.Coalesce;

import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.common.uom.UOMTrl;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.module.idljava.proc.IdlServiceJava;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * 
 * @author Dieguito
 */
public class ImportConciliationPos extends IdlServiceJava {

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Idenficador", Parameter.STRING),
        new Parameter("Cuenta Financiera Pago", Parameter.STRING),
        new Parameter("Tipo Operacion Pago", Parameter.STRING), 
        new Parameter("Fecha", Parameter.STRING),
        new Parameter("Concepto Contable", Parameter.STRING), 
        new Parameter("Valor Deposito", Parameter.STRING),
        new Parameter("Description", Parameter.STRING),
        new Parameter("Centro de Costo", Parameter.STRING),
        new Parameter("Cuenta Financiera Cobro", Parameter.STRING),
        new Parameter("Tipo Operacion Cobro", Parameter.STRING)};
    
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 32);
    validator.checkString(values[1], 100);
    validator.checkString(values[2], 100);
    validator.checkDate(values[3]);
    validator.checkString(values[4], 100);
    validator.checkString(values[5], 100);
    validator.checkString(values[6], 100);
    validator.checkString(values[7], 100);
    validator.checkString(values[8], 100);
    validator.checkString(values[9], 100);
    return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createConciliationPos((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6], (String) values[7],
        (String) values[8], (String) values[9]);
  }
  
  private void processTransaction(String strAction, FIN_FinaccTransaction transaction)
      throws OBException {
    FIN_TransactionProcess.doTransactionProcess(strAction, transaction);
  }

  public BaseOBObject createConciliationPos (final String identifier, final String financialaccountpay,
      final String paytype, final String datemovement, final String accountingconcept, final String depositvalue,
      final String description, final String costcenterstr, final String financialaccountrec, final String rectype) throws Exception {

      
    FIN_FinancialAccount financialaccount = findDALInstance(false, FIN_FinancialAccount.class, new Value(
        FIN_FinancialAccount.PROPERTY_NAME, financialaccountpay));
    if (financialaccount == null || financialaccount.equals("")) {
      throw new OBException("Cuenta Financiera " + financialaccountpay + " de pago no existe");
    }
    
    GLItem item = findDALInstance(false, GLItem.class, new Value(
        GLItem.PROPERTY_NAME, accountingconcept));
    if (item == null || item.equals("")) {
      throw new OBException("Concepto Contable " + accountingconcept + " no existe");
    }
    
    Costcenter costcenter = findDALInstance(false, Costcenter.class, new Value(
        Costcenter.PROPERTY_SEARCHKEY, costcenterstr));
    if (costcenter == null || costcenter.equals("")) {
      throw new OBException("Centro de costo " + costcenterstr + " no existe o esta desactivo");
    }
    
    Currency currency  = findDALInstance(false, Currency.class, new Value(
        Currency.PROPERTY_ID, "100"));
       
    FIN_FinaccTransaction finacctransaction = OBProvider.getInstance().get(FIN_FinaccTransaction.class);
    String line = "10";
    

    try {
      finacctransaction.setAccount(financialaccount);
      finacctransaction.setTransactionType("BPW");
      finacctransaction.setTransactionDate(Parameter.DATE.parse(datemovement));
      finacctransaction.setDateAcct((Parameter.DATE.parse(datemovement)));
      finacctransaction.setDescription(description);  
      finacctransaction.setCostCenter(costcenter);
      finacctransaction.setGLItem(item);
      finacctransaction.setPaymentAmount(Parameter.BIGDECIMAL.parse(depositvalue));
      finacctransaction.setCurrency(currency);
      finacctransaction.setLineNo(new Long(line));
     
      OBDal.getInstance().save(finacctransaction);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
    try {
      processTransaction("P", finacctransaction);
    } catch (Exception e) {
      throw new OBException(e.getMessage(), e);
    }
    
    if (!financialaccountrec.equals("") ) {
      FIN_FinancialAccount financialaccountrecobj = findDALInstance(false, FIN_FinancialAccount.class, new Value(
          FIN_FinancialAccount.PROPERTY_NAME, financialaccountrec));
      if (financialaccountrecobj == null || financialaccountrecobj.equals("")) {
        throw new OBException("Cuenta Financiera " + financialaccountpay + " de cobro no existe");
      }
      
      FIN_FinaccTransaction finacctransactionrec = OBProvider.getInstance().get(FIN_FinaccTransaction.class);

      try {
        finacctransactionrec.setAccount(financialaccountrecobj);
        finacctransactionrec.setTransactionType("BPD");
        finacctransactionrec.setTransactionDate(Parameter.DATE.parse(datemovement));
        finacctransactionrec.setDateAcct((Parameter.DATE.parse(datemovement)));
        finacctransactionrec.setDescription(description);  
        finacctransactionrec.setCostCenter(costcenter);
        finacctransactionrec.setGLItem(item);
        finacctransactionrec.setDepositAmount(Parameter.BIGDECIMAL.parse(depositvalue));
        finacctransactionrec.setCurrency(currency);
        finacctransactionrec.setLineNo(new Long(line));
       
        OBDal.getInstance().save(finacctransactionrec);
        OBDal.getInstance().flush();
      } catch (Exception e) {
        e.printStackTrace();
      }
      
      try {
        processTransaction("P", finacctransactionrec);
      } catch (Exception e) {
        throw new OBException(e.getMessage(), e);
      }
    }
    
  
    OBDal.getInstance().commitAndClose();
    return finacctransaction;
  }
}
