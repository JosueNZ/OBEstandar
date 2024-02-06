package ec.com.sidesoft.localization.inventoryaccounting.acc_template;

import java.math.BigDecimal;
import java.sql.Connection;

import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.costing.CostingStatus;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.Account;
import org.openbravo.erpCommon.ad_forms.AcctSchema;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

public class DocLine_Material extends DocLines {
  static Logger log4jDocLine_Material = Logger.getLogger(DocLine_Material.class);

  /**
   * Constructor
   * 
   * @param DocumentType
   *          document type
   * @param TrxHeader_ID
   *          trx header id
   * @param TrxLine_ID
   *          trx line id
   */
  public DocLine_Material(String DocumentType, String TrxHeader_ID, String TrxLine_ID) {
    super(DocumentType, TrxHeader_ID, TrxLine_ID);
  }

  /** Locator */
  public String m_M_Locator_ID = "";
  public String m_M_LocatorTo_ID = "";
  public String m_M_Warehouse_ID = "";
  /** Production */
  public String m_Productiontype = "";
  public MaterialTransaction transaction = null;
  public String m_breakdownqty = "";

  /**
   * Set Trasaction Quantity and Storage Qty
   * 
   * @param qty
   *          qty
   */
  public void setQty(String qty, ConnectionProvider conn) {
    log4jDocLine_Material.debug(" setQty - qty= " + qty);
    super.setQty(qty); // save TrxQty
    p_productInfo.setQty(qty, p_productInfo.m_C_UOM_ID, conn);
    log4jDocLine_Material.debug(" setQty - productInfo.qty = " + p_productInfo.m_qty);
  } // setQty

  private String getQty() {
    return m_qty;
  }

  public void setTransaction(MaterialTransaction transaction) {
    this.transaction = transaction;
  }

  public Warehouse getWarehouse() {
    return OBDal.getInstance().get(Warehouse.class, m_M_Warehouse_ID);
  }

  /**
   * Get Total Product Costs. If exists a transaction retrieves the cost from it, otherwise calls
   * the
   * {@link ProductInfo#getProductCosts(String, String, AcctSchema, ConnectionProvider, Connection)}
   * 
   * @param date
   *          String with the accounting date used in case there is no material transaction.
   * @param as
   */

  public String getProductCosts(String date, AcctSchema as, ConnectionProvider conn, Connection con) {
    if (transaction != null && transaction.getTransactionCost() != null
        && CostingStatus.getInstance().isMigrated()) {
      BigDecimal sign = new BigDecimal(new BigDecimal(getQty()).signum());
      return transaction.getTransactionCost().multiply(sign).toString();
    } else if (transaction != null && CostingStatus.getInstance().isMigrated()) {
      return "";
    } else if (CostingStatus.getInstance().isMigrated()) {
      // If there isn't any material transaction get the default cost of the product.
      try {
        Organization legalEntity = OBContext.getOBContext()
            .getOrganizationStructureProvider(p_productInfo.m_AD_Client_ID)
            .getLegalEntity(OBDal.getInstance().get(Organization.class, m_AD_Org_ID));
        return p_productInfo.getProductDefaultCosts(date, null, legalEntity, getWarehouse(),
            legalEntity.getCurrency() != null ? legalEntity.getCurrency() : legalEntity.getClient()
                .getCurrency());
      } catch (OBException e) {
        log4jDocLine_Material.error("No standard cost found for product: "
            + OBDal.getInstance().get(Product.class, m_M_Product_ID).getIdentifier()
            + " DocumentType: " + p_DocumentType + " record id: " + m_TrxHeader_ID);
        return "";
      }
    }
    return p_productInfo.getProductCosts(date, "", as, conn, con);
  } // getProductCosts

  /**
   * Line Account from Product
   * 
   * @param AcctType
   *          see ProoductInfo.ACCTTYPE_* (0..3)
   * @param as
   *          accounting schema
   * @return Requested Product Account
   */
  public Account getAccount(String AcctType, AcctSchema as, ConnectionProvider conn) {
    return p_productInfo.getAccount(AcctType, as, conn);
  } // getAccount

  public String getBreakdownQty() {
    return m_breakdownqty;
  }

  public void setBreakdownQty(String breakdownqty) {
    this.m_breakdownqty = breakdownqty;
  }

  public String getServletInfo() {
    return "Servlet for the accounting";
  } // end of getServletInfo() method
}
