package ec.com.sidesoft.transaction.reversal;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.openbravo.base.model.Property;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.util.CheckException;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;

public class ReversosContableUtils {

  private static final Logger log4j = Logger.getLogger(ReversosContableUtils.class);

  static public void reverseAccounting(final List<String> transactions, final String tableId) {

    final Query query = getAccountCollection(transactions, tableId);

    if (!query.list().isEmpty()) {

      Long maxSeq = getMaxSeqNoByDocumentAndTable(transactions, tableId);

      for (int i = 0; i < query.list().size(); i++) {

        AccountingFact accountingFact = OBDal.getInstance().get(AccountingFact.class,
            query.list().get(i).toString());

        AccountingReversal accountingReversal = getAccountingReversalFromAccountingFact(
            accountingFact);
        accountingReversal.setSequenceNumber(maxSeq);
        createReversal(accountingReversal);
      }

      OBDal.getInstance().flush();
    }
  }

  private static Query getAccountCollection(final List<String> transactions, final String tableId) {
    final String whereClause = " select f_acc.id from FinancialMgmtAccountingFact as f_acc  where f_acc.table.id = :tableId and f_acc.recordID in :transactions ";
    OBContext.setAdminMode(true);
    final Query query = OBDal.getInstance().getSession().createQuery(whereClause.toString());
    query.setParameterList("transactions", transactions);
    query.setString("tableId", tableId);
    return query;
  }

  static private AccountingReversal getAccountingReversalFromAccountingFact(
      final AccountingFact accountingFact) {
    AccountingReversal accountingReversal = OBProvider.getInstance().get(AccountingReversal.class);
    accountingReversal.setId(SequenceIdData.getUUID());
    accountingReversal.setNewOBObject(true);
    for (final Property p : accountingFact.getEntity().getProperties()) {
      try {
        Property reversalProperty = accountingReversal.getEntity().getProperty(p.getName());
        if (!p.isId() && !p.isOneToMany() && reversalProperty != null) {
          final Object value = accountingFact.getValue(p.getName());
          accountingReversal.setValue(p.getName(), value);
        }
      } catch (CheckException e) {
        log4j.info(e.getMessage());
      }
    }
    OBDal.getInstance().save(accountingReversal);
    return accountingReversal;
  }

  static private void createReversal(final AccountingReversal accountingReversal) {
    AccountingReversal newAccountingFactEntity = OBProvider.getInstance()
        .get(AccountingReversal.class);
    newAccountingFactEntity = (AccountingReversal) DalUtil.copy(accountingReversal, false);
    newAccountingFactEntity.setId(SequenceIdData.getUUID());
    newAccountingFactEntity.setNewOBObject(true);
    newAccountingFactEntity.setCredit(accountingReversal.getDebit());
    newAccountingFactEntity.setDebit(accountingReversal.getCredit());
    newAccountingFactEntity.setForeignCurrencyCredit(accountingReversal.getForeignCurrencyDebit());
    newAccountingFactEntity.setForeignCurrencyDebit(accountingReversal.getForeignCurrencyCredit());
    newAccountingFactEntity.setReverso(true);
    OBDal.getInstance().save(newAccountingFactEntity);
  }

  static private Long getMaxSeqNoByDocumentAndTable(final List<String> transactions,
      final String tableId) {
    Long maxSeq = 10L;

    final String whereClause = " select coalesce(max(sequenceNumber),0) + 10 from STXREV_FinanciallAccounting   where table.id = :tableId and recordID in :transactions ";
    final Query query = OBDal.getInstance().getSession().createQuery(whereClause.toString());
    query.setParameterList("transactions", transactions);
    query.setString("tableId", tableId);
    query.setMaxResults(1);
    maxSeq = (Long) query.uniqueResult();

    return maxSeq;
  }

}
