package ec.com.sidesoft.inventory.blind.register.ad_process;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.QueryTimeoutException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.exception.GenericJDBCException;
import org.hibernate.impl.SessionFactoryImpl;
import org.hibernate.impl.SessionImpl;
import org.hibernate.type.DateType;
import org.hibernate.type.StringType;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.materialmgmt.hook.InventoryCountCheckHook;
import org.openbravo.materialmgmt.hook.InventoryCountProcessHook;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.AttributeSet;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.PeriodControl;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import ec.com.sidesoft.inventory.blind.register.SiblrPhysicalInventory;

public class ProcessedInventory extends DalBaseProcess {
  OBError message;
  static Logger log4j = Logger.getLogger(ProcessedInventory.class);
  @Inject
  @Any
  private Instance<InventoryCountCheckHook> inventoryCountChecks;

  @Inject
  @Any
  private Instance<InventoryCountProcessHook> inventoryCountProcesses;

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    ConnectionProvider conn = new DalConnectionProvider(false);
    message = new OBError();
    SiblrPhysicalInventory order = null;
    User objUser = null;
    OBError myMessage = null;

    try {
      
      OBContext.setAdminMode(true);
      
      String strOrderID = (String) bundle.getParams().get("Siblr_Physical_Inventory_ID");
      String strUserID = bundle.getContext().getUser();

      order = OBDal.getInstance().get(SiblrPhysicalInventory.class, strOrderID);
      objUser = OBDal.getInstance().get(User.class, strUserID);
      myMessage = ProcesarNEW(strOrderID, order, objUser, bundle);
      
    }catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      message.setType("Error");          
      message.setMessage(e.getMessage());
      message.setTitle(Utility.messageBD(conn, "Error", bundle.getContext().getLanguage()));
      bundle.setResult(message);
    }finally {
      OBContext.setAdminMode(false);          
    }
    
    if(myMessage != null) {
      message.setType("Success");
      message.setTitle(Utility.messageBD(conn, "Success", bundle.getContext().getLanguage()));
      message.setMessage(Utility.messageBD(conn, myMessage.getMessage() , bundle.getContext().getLanguage()));
      bundle.setResult(message);
    }

  }
  
  private OBError ProcesarNEW(String strC_Order_Id, SiblrPhysicalInventory order, User user,
      ProcessBundle bundle) throws Exception {

    final ConnectionProvider conn = bundle.getConnection();
    final VariablesSecureApp vars = bundle.getContext().toVars();
    
    OBError myMessage = new OBError(); 
    
    OBError msg = new OBError();
    msg.setType("Error");
    msg.setTitle(OBMessageUtils.messageBD("Error"));    
    
    org.openbravo.model.ad.ui.Process process = OBDal.getInstance().get(
        org.openbravo.model.ad.ui.Process.class, "95AB1E9EBC4144CA8D306E8A2627CDE0");

    final ProcessInstance pInstance = CallProcess.getInstance().call(process,
        order.getId(), null);     

    myMessage = Utility.getProcessInstanceMessage(conn, vars,
        PInstanceProcessData.select(new DalConnectionProvider(), pInstance.getId()));
    
    if (pInstance.getResult() == 0) {
      // error processing
      throw new OBException(myMessage.getMessage());
    }else {
      
      //try {
        
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().refresh(pInstance);
        OBDal.getInstance().commitAndClose();
        
        order = OBDal.getInstance().get(SiblrPhysicalInventory.class, strC_Order_Id);
        
        String documentno = getdocumentNo(order.getId());
        
        if (order.getDocumentStatus().equals("CO")) {
          // call other the SP
          try {
            // first get a connection
            final Connection connection = OBDal.getInstance().getConnection();

            final PreparedStatement ps = connection
                .prepareStatement("SELECT m_inventory_id FROM m_inventory WHERE documentno  = ?");
            ps.setString(1, documentno);
            ps.execute();
            ResultSet sad = ps.getResultSet();
            while (sad.next()) {
              String m_inventory_id = sad.getString("m_inventory_id");
              //Completar(m_inventory_id);

              /************* COMPLETAR ******************/
              try {

                OBContext.setAdminMode(true);

                myMessage = Completar(m_inventory_id, user, bundle);

                OBDal.getInstance().save(user);
                OBDal.getInstance().flush();

                myMessage.setType("Success");
                myMessage.setMessage(
                    " Se ingresó y completó un nuevo registro en en la ventana de Inventario Físico con el número de documento: "
                        + documentno);
                myMessage
                    .setTitle(Utility.messageBD(conn, "Success", bundle.getContext().getLanguage()));
                bundle.setResult(myMessage);

              } catch (Exception e) {
                OBDal.getInstance().rollbackAndClose();
                myMessage.setType("Error");
                myMessage.setMessage(e.getMessage());
                myMessage.setTitle(Utility.messageBD(conn, "Error", bundle.getContext().getLanguage()));
                bundle.setResult(myMessage);
                RollBackInventory(order.getId().toString(), m_inventory_id);
                throw new OBException(myMessage.getMessage());

              } finally {
                OBContext.setAdminMode(false);
              }
              /************* COMPLETAR ******************/              
            }

          } catch (Exception e) {
            throw new OBException(e.getMessage());
          }
        }
        
      /*} catch (Exception e) {
        throw new OBException(e.getMessage());
      } */     
      
    } 
    
    return myMessage;
    
  }  

  /*************************************
   * Replica de completado de inventario Original
   ***************************************/
  public OBError Completar(String recordID, User user, ProcessBundle bundle) {
    
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));
    
    try {
      final InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, recordID);

      // lock inventory
      if (inventory.isProcessNow()) {
        throw new OBException(OBMessageUtils.parseTranslation("@OtherProcessActive@"));
      }
      inventory.setProcessNow(true);
      OBDal.getInstance().save(inventory);
      if (SessionHandler.isSessionHandlerPresent()) {
        SessionHandler.getInstance().commitAndStart();
      }

      OBContext.setAdminMode(false);
      try {
        msg = processInventory(inventory);
      } catch (Exception e) {
        throw new OBException(e.getMessage());
      } finally {
        OBContext.restorePreviousMode();
      }

      inventory.setProcessNow(false);

      OBDal.getInstance().save(inventory);
      OBDal.getInstance().flush();

      // bundle.setResult(msg);

      // Postgres wraps the exception into a GenericJDBCException
    } catch (GenericJDBCException ge) {
      log4j.error("Exception processing physical inventory", ge);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD(bundle.getConnection(), "Error", bundle.getContext()
       .getLanguage()));
      msg.setMessage(ge.getSQLException().getMessage());
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
      // final String recordID = (String) bundle.getParams().get("M_Inventory_ID");
      final InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, recordID);
      inventory.setProcessNow(false);
      OBDal.getInstance().save(inventory);
      // Oracle wraps the exception into a QueryTimeoutException
    } catch (QueryTimeoutException qte) {
      log4j.error("Exception processing physical inventory", qte);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD(bundle.getConnection(), "Error", bundle.getContext()
      .getLanguage()));
      msg.setMessage(qte.getSQLException().getMessage().split("\n")[0]);
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
      // final String recordID = (String) bundle.getParams().get("M_Inventory_ID");
      final InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, recordID);
      inventory.setProcessNow(false);
      OBDal.getInstance().save(inventory);
    } catch (final Exception e) {
      log4j.error("Exception processing physical inventory", e);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD(bundle.getConnection(), "Error", bundle.getContext()
      .getLanguage()));
      msg.setMessage(FIN_Utility.getExceptionMessage(e));
      bundle.setResult(msg);
      OBDal.getInstance().rollbackAndClose();
      // final String recordID = (String) bundle.getParams().get("M_Inventory_ID");
      final InventoryCount inventory = OBDal.getInstance().get(InventoryCount.class, recordID);
      inventory.setProcessNow(false);
      OBDal.getInstance().save(inventory);
      throw new OBException(msg.getMessage());
    }
    
    return msg;

  }

  
  public OBError Procesar(String strC_Order_Id, SiblrPhysicalInventory order, User user,
      ProcessBundle bundle) throws NoConnectionAvailableException, SQLException, ServletException {
    // setUserContext("100");

    // 104 C_Order_Post
    ConnectionProvider con = new DalConnectionProvider(false);
    final org.openbravo.model.ad.ui.Process process = OBDal.getInstance()
        .get(org.openbravo.model.ad.ui.Process.class, "BEFD893286EA4129BB3DC055E11F80B1");

    final ProcessInstance pInstance = OBProvider.getInstance().get(ProcessInstance.class);
    pInstance.setProcess(process);
    pInstance.setActive(true);
    pInstance.setRecordID(strC_Order_Id);
    pInstance.setUserContact(OBContext.getOBContext().getUser());
    OBDal.getInstance().save(pInstance);
    OBDal.getInstance().flush();
    String strPInstanceId = pInstance.getId();
    try {

      CallableStatement cs = con.getConnection().prepareCall("{call siblr_doc_register (?)}");

      cs.setString(1, strPInstanceId);
      cs.execute();
      cs.close();
    } catch (Exception e) {
      throw new OBException(e.getMessage(), e);
    }

    OBDal.getInstance().flush();
    OBDal.getInstance().getSession().refresh(pInstance);
    OBDal.getInstance().commitAndClose();

    VariablesSecureApp vars = new VariablesSecureApp(user.getId(),
        (String) bundle.getParams().get("Ad_Client_Id"),
        (String) bundle.getParams().get("Ad_Org_Id"));

    Connection conn = con.getTransactionConnection();

    PInstanceProcessData[] pinstanceData = PInstanceProcessData.selectConnection(conn, con,
        strPInstanceId);
    OBError myMessage = Utility.getProcessInstanceMessage(con, vars, pinstanceData);
    try {
      // SiblrPhysicalInventory order = null;
      order = OBDal.getInstance().get(SiblrPhysicalInventory.class, strC_Order_Id);

      if (order.getDocumentStatus().equals("CO")) {
        // call other the SP
        try {
          // first get a connection
          final Connection connection = OBDal.getInstance().getConnection();

          final PreparedStatement ps = connection
              .prepareStatement("SELECT m_inventory_id FROM m_inventory WHERE documentno  = ?");
          ps.setString(1, order.getDocumentNo());
          ps.execute();
          ResultSet sad = ps.getResultSet();
          while (sad.next()) {
            String m_inventory_id = sad.getString("m_inventory_id");
            //Completar(m_inventory_id);
          }

        } catch (Exception e) {
          throw new OBException(e.getMessage());
        }
      }
    } catch (Exception e) {
      throw new OBException(e.getMessage());

    }

    /*************************************
     * Replica de completado de inventario Original
     ***************************************/

    // refresh the pInstance as the SP has changed it
    // OBDal.getInstance().getSession().refresh(pInstance);
    return myMessage;
  }

    public OBError processInventory(InventoryCount inventory) throws OBException {
    return processInventory(inventory, true);
  }

  public OBError processInventory(InventoryCount inventory, boolean checkReservationQty)
      throws OBException {
    return processInventory(inventory, checkReservationQty, false);
  }

  public OBError processInventory(InventoryCount inventory, boolean checkReservationQty,
      boolean checkPermanentCost) throws OBException {
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));
    runChecks(inventory);

    // In case get_uuid is not already registered, it's registered now.
    final Dialect dialect = ((SessionFactoryImpl) ((SessionImpl) OBDal.getInstance().getSession())
        .getSessionFactory()).getDialect();
    Map<String, SQLFunction> function = dialect.getFunctions();
    if (!function.containsKey("get_uuid")) {
      dialect.getFunctions().put("get_uuid", new StandardSQLFunction("get_uuid", new StringType()));
    }
    if (!function.containsKey("now")) {
      dialect.getFunctions().put("now", new StandardSQLFunction("now", new DateType()));
    }
    if (!function.containsKey("to_date")) {
      dialect.getFunctions().put("to_date", new StandardSQLFunction("to_date", new DateType()));
    }
    if (!function.containsKey("to_timestamp")) {
      dialect.getFunctions().put("to_timestamp",
          new StandardSQLFunction("to_timestamp", new DateType()));
    }
    StringBuffer insert = new StringBuffer();
    insert.append("insert into " + MaterialTransaction.ENTITY_NAME + "(");
    insert.append(" id ");
    insert.append(", " + MaterialTransaction.PROPERTY_ACTIVE);
    insert.append(", " + MaterialTransaction.PROPERTY_CLIENT);
    insert.append(", " + MaterialTransaction.PROPERTY_ORGANIZATION);
    insert.append(", " + MaterialTransaction.PROPERTY_CREATIONDATE);
    insert.append(", " + MaterialTransaction.PROPERTY_CREATEDBY);
    insert.append(", " + MaterialTransaction.PROPERTY_UPDATED);
    insert.append(", " + MaterialTransaction.PROPERTY_UPDATEDBY);
    insert.append(", " + MaterialTransaction.PROPERTY_MOVEMENTTYPE);
    insert.append(", " + MaterialTransaction.PROPERTY_CHECKRESERVEDQUANTITY);
    insert.append(", " + MaterialTransaction.PROPERTY_ISCOSTPERMANENT);
    insert.append(", " + MaterialTransaction.PROPERTY_MOVEMENTDATE);
    insert.append(", " + MaterialTransaction.PROPERTY_STORAGEBIN);
    insert.append(", " + MaterialTransaction.PROPERTY_PRODUCT);
    insert.append(", " + MaterialTransaction.PROPERTY_ATTRIBUTESETVALUE);
    insert.append(", " + MaterialTransaction.PROPERTY_MOVEMENTQUANTITY);
    insert.append(", " + MaterialTransaction.PROPERTY_UOM);
    insert.append(", " + MaterialTransaction.PROPERTY_ORDERQUANTITY);
    insert.append(", " + MaterialTransaction.PROPERTY_ORDERUOM);
    insert.append(", " + MaterialTransaction.PROPERTY_PHYSICALINVENTORYLINE);
    insert.append(", " + MaterialTransaction.PROPERTY_TRANSACTIONPROCESSDATE);
    // select from inventory line
    insert.append(" ) \n select get_uuid() ");
    insert.append(", e." + InventoryCountLine.PROPERTY_ACTIVE);
    insert.append(", e." + InventoryCountLine.PROPERTY_CLIENT);
    insert.append(", e." + InventoryCountLine.PROPERTY_ORGANIZATION);
    insert.append(", now()");
    insert.append(", u");
    insert.append(", now()");
    insert.append(", u");
    insert.append(", 'I+'");
    // We have to set check reservation quantity flag equal to checkReservationQty
    // InventoryCountLine.PROPERTY_ACTIVE-->> Y
    // InventoryCountLine.PROPERTY_PHYSINVENTORY + "." + InventoryCount.PROPERTY_PROCESSED -->> N
    if (checkReservationQty) {
      insert.append(", e." + InventoryCountLine.PROPERTY_ACTIVE);
    } else {
      insert.append(", e." + InventoryCountLine.PROPERTY_PHYSINVENTORY + "."
          + InventoryCount.PROPERTY_PROCESSED);
    }
    // We have to set check permanent cost flag
    // InventoryCountLine.PROPERTY_ACTIVE-->> Y
    // InventoryCountLine.PROPERTY_PHYSINVENTORY + "." + InventoryCount.PROPERTY_PROCESSED -->> N
    if (checkPermanentCost) {
      insert.append(", e." + InventoryCountLine.PROPERTY_ACTIVE);
    } else {
      insert.append(", e." + InventoryCountLine.PROPERTY_PHYSINVENTORY + "."
          + InventoryCount.PROPERTY_PROCESSED);
    }
    insert.append(", e." + InventoryCountLine.PROPERTY_PHYSINVENTORY + "."
        + InventoryCount.PROPERTY_MOVEMENTDATE);
    insert.append(", e." + InventoryCountLine.PROPERTY_STORAGEBIN);
    insert.append(", e." + InventoryCountLine.PROPERTY_PRODUCT);
    insert.append(", asi");
    insert.append(", e." + InventoryCountLine.PROPERTY_QUANTITYCOUNT + " - COALESCE(" + "e."
        + InventoryCountLine.PROPERTY_BOOKQUANTITY + ", 0)");
    insert.append(", e." + InventoryCountLine.PROPERTY_UOM);
    insert.append(", e." + InventoryCountLine.PROPERTY_ORDERQUANTITY + " - COALESCE(" + "e."
        + InventoryCountLine.PROPERTY_QUANTITYORDERBOOK + ", 0)");
    insert.append(", e." + InventoryCountLine.PROPERTY_ORDERUOM);
    insert.append(", e");
    insert.append(", to_timestamp(to_char(:currentDate), to_char('DD-MM-YYYY HH24:MI:SS'))");
    insert.append(" \nfrom " + InventoryCountLine.ENTITY_NAME + " as e");
    insert.append(" , " + User.ENTITY_NAME + " as u");
    insert.append(" , " + AttributeSetInstance.ENTITY_NAME + " as asi");
    insert.append(" , " + Product.ENTITY_NAME + " as p");
    insert.append(" \nwhere e." + InventoryCountLine.PROPERTY_PHYSINVENTORY + ".id = :inv");
    insert.append(" and (e." + InventoryCountLine.PROPERTY_QUANTITYCOUNT + " != e."
        + InventoryCountLine.PROPERTY_BOOKQUANTITY);
    insert.append(" or e." + InventoryCountLine.PROPERTY_ORDERQUANTITY + " != e."
        + InventoryCountLine.PROPERTY_QUANTITYORDERBOOK + ")");
    insert.append(" and u.id = :user");
    insert.append(
        " and asi.id = COALESCE(e." + InventoryCountLine.PROPERTY_ATTRIBUTESETVALUE + ".id , '0')");
    // Non Stockable Products should not generate warehouse transactions
    insert.append(" and e." + InventoryCountLine.PROPERTY_PRODUCT + ".id = p.id and p."
        + Product.PROPERTY_STOCKED + " = 'Y' and p." + Product.PROPERTY_PRODUCTTYPE + " = 'I'");

    Query queryInsert = OBDal.getInstance().getSession().createQuery(insert.toString());
    queryInsert.setString("inv", inventory.getId());
    queryInsert.setString("user", OBContext.getOBContext().getUser().getId());
    final SimpleDateFormat dateFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    queryInsert.setString("currentDate", dateFormatter.format(new Date()));
    // queryInsert.setBoolean("checkReservation", checkReservationQty);
    queryInsert.executeUpdate();

    if (!"C".equals(inventory.getInventoryType()) && !"O".equals(inventory.getInventoryType())) {
      checkStock(inventory);
    }

    try {
      executeHooks(inventoryCountProcesses, inventory);
    } catch (Exception e) {
      OBException obException = new OBException(e.getMessage(), e.getCause());
      throw obException;
    }

    inventory.setProcessed(true);
    return msg;
  }

  private void runChecks(InventoryCount inventory) throws OBException {

    try {
      executeHooks(inventoryCountChecks, inventory);
    } catch (Exception e) {
      OBException obException = new OBException(e.getMessage(), e.getCause());
      throw obException;
    }

    if (inventory.isProcessed()) {
      throw new OBException(OBMessageUtils.parseTranslation("@AlreadyPosted@"));
    }
    // Products without attribute set.
    StringBuffer where = new StringBuffer();
    where.append(" as icl");
    where.append("   join icl." + InventoryCountLine.PROPERTY_PRODUCT + " as p");
    where.append("   join p." + Product.PROPERTY_ATTRIBUTESET + " as aset");
    where.append(" where icl." + InventoryCountLine.PROPERTY_PHYSINVENTORY + ".id = :inventory");
    where.append("   and aset." + AttributeSet.PROPERTY_REQUIREATLEASTONEVALUE + " = true");
    where.append("   and coalesce(p." + Product.PROPERTY_USEATTRIBUTESETVALUEAS + ", '-') <> 'F'");
    where.append(
        "   and coalesce(icl." + InventoryCountLine.PROPERTY_ATTRIBUTESETVALUE + ", '0') = '0'");
    where.append("  order by icl." + InventoryCountLine.PROPERTY_LINENO);
    OBQuery<InventoryCountLine> iclQry = OBDal.getInstance().createQuery(InventoryCountLine.class,
        where.toString());
    iclQry.setNamedParameter("inventory", inventory.getId());
    iclQry.setMaxResult(1);
    Object icl = iclQry.uniqueResult();
    if (icl != null) {
      throw new OBException(OBMessageUtils.parseTranslation(
          "@Inline@ " + ((InventoryCountLine) icl).getLineNo() + " @productWithoutAttributeSet@"
              + " " + ((InventoryCountLine) icl).getProduct().getName()));
    }

    // duplicated product
    where = new StringBuffer();
    where.append(" as icl");
    where.append(" where icl." + InventoryCountLine.PROPERTY_PHYSINVENTORY + ".id = :inventory");
    where.append("   and exists (select 1 from " + InventoryCountLine.ENTITY_NAME + " as icl2");
    where.append("       where icl." + InventoryCountLine.PROPERTY_PHYSINVENTORY + " = icl2."
        + InventoryCountLine.PROPERTY_PHYSINVENTORY);
    where.append("         and icl." + InventoryCountLine.PROPERTY_PRODUCT + " = icl2."
        + InventoryCountLine.PROPERTY_PRODUCT);
    where.append("         and coalesce(icl." + InventoryCountLine.PROPERTY_ATTRIBUTESETVALUE
        + ", '0') = coalesce(icl2." + InventoryCountLine.PROPERTY_ATTRIBUTESETVALUE + ", '0')");
    where.append("         and coalesce(icl." + InventoryCountLine.PROPERTY_ORDERUOM
        + ", '0') = coalesce(icl2." + InventoryCountLine.PROPERTY_ORDERUOM + ", '0')");
    where.append(" and coalesce(icl." + InventoryCountLine.PROPERTY_UOM + ", '0') = coalesce(icl2."
        + InventoryCountLine.PROPERTY_UOM + ", '0')");
    where.append("         and icl." + InventoryCountLine.PROPERTY_STORAGEBIN + " = icl2."
        + InventoryCountLine.PROPERTY_STORAGEBIN);
    where.append("         and icl." + InventoryCountLine.PROPERTY_LINENO + " <> icl2."
        + InventoryCountLine.PROPERTY_LINENO + ")");
    where.append(" order by icl." + InventoryCountLine.PROPERTY_PRODUCT);
    where.append(", icl." + InventoryCountLine.PROPERTY_ATTRIBUTESETVALUE);
    where.append(", icl." + InventoryCountLine.PROPERTY_STORAGEBIN);
    where.append(", icl." + InventoryCountLine.PROPERTY_ORDERUOM);
    where.append(", icl." + InventoryCountLine.PROPERTY_LINENO);
    iclQry = OBDal.getInstance().createQuery(InventoryCountLine.class, where.toString());
    iclQry.setNamedParameter("inventory", inventory.getId());
    List<InventoryCountLine> iclList = iclQry.list();
    if (!iclList.isEmpty()) {
      String lines = "";
      for (InventoryCountLine icl2 : iclList) {
        lines += icl2.getLineNo().toString() + ", ";
      }
      throw new OBException(
          OBMessageUtils.parseTranslation("@Thelines@ " + lines + "@sameInventorylines@"));
    }

    Organization org = inventory.getOrganization();
    if (!org.isReady()) {
      throw new OBException(OBMessageUtils.parseTranslation("@OrgHeaderNotReady@"));
    }
    if (!org.getOrganizationType().isTransactionsAllowed()) {
      throw new OBException(OBMessageUtils.parseTranslation("@OrgHeaderNotTransAllowed@"));
    }
    OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(inventory.getClient().getId());
    Organization headerLEorBU = osp.getLegalEntityOrBusinessUnit(org);
    iclQry = OBDal.getInstance().createQuery(InventoryCountLine.class,
        InventoryCountLine.PROPERTY_PHYSINVENTORY + ".id = :inventory and "
            + InventoryCountLine.PROPERTY_ORGANIZATION + ".id <> :organization");
    iclQry.setNamedParameter("inventory", inventory.getId());
    iclQry.setNamedParameter("organization", org.getId());
    iclList = iclQry.list();
    if (!iclList.isEmpty()) {
      for (InventoryCountLine icl2 : iclList) {
        if (!headerLEorBU.getId()
            .equals(osp.getLegalEntityOrBusinessUnit(icl2.getOrganization()).getId())) {
          throw new OBException(OBMessageUtils.parseTranslation("@LinesAndHeaderDifferentLEorBU@"));
        }
      }
    }
    if (headerLEorBU.getOrganizationType().isLegalEntityWithAccounting()) {
      where = new StringBuffer();
      where.append(" as pc ");
      where.append("   join pc." + PeriodControl.PROPERTY_PERIOD + " as p");
      where.append(" where p." + Period.PROPERTY_STARTINGDATE + " <= :dateStarting");
      where.append("   and p." + Period.PROPERTY_ENDINGDATE + " >= :dateEnding");
      where.append("   and pc." + PeriodControl.PROPERTY_DOCUMENTCATEGORY + " = 'MMI' ");
      where.append("   and pc." + PeriodControl.PROPERTY_ORGANIZATION + ".id = :org");
      where.append("   and pc." + PeriodControl.PROPERTY_PERIODSTATUS + " = 'O'");
      OBQuery<PeriodControl> pQry = OBDal.getInstance().createQuery(PeriodControl.class,
          where.toString());
      pQry.setFilterOnReadableClients(false);
      pQry.setFilterOnReadableOrganization(false);
      pQry.setNamedParameter("dateStarting", inventory.getMovementDate());
      pQry.setNamedParameter("dateEnding",
          DateUtils.truncate(inventory.getMovementDate(), Calendar.DATE));
      pQry.setNamedParameter("org", osp.getPeriodControlAllowedOrganization(org).getId());
      pQry.setMaxResult(1);
      if (pQry.uniqueResult() == null) {
        throw new OBException(OBMessageUtils.parseTranslation("@PeriodNotAvailable@"));
      }
    }
  }

  private void checkStock(InventoryCount inventory) {
    String attribute;
    final StringBuilder hqlString = new StringBuilder();
    hqlString.append("select sd.id ");
    hqlString.append(" from MaterialMgmtInventoryCountLine as icl");
    hqlString.append(" , MaterialMgmtStorageDetail as sd");
    hqlString.append(" , Locator as l");
    hqlString.append(" , MaterialMgmtInventoryStatus as invs");
    hqlString.append(" where icl.physInventory.id = ?");
    hqlString.append("   and sd.product = icl.product");
    hqlString.append("   and (sd.quantityOnHand < 0");
    hqlString.append("     or sd.onHandOrderQuanity < 0");
    hqlString.append("     )");
    // Check only negative Stock for the Bins of the Lines of the Physical Inventory
    hqlString.append("   and sd.storageBin.id = icl.storageBin.id");
    hqlString.append("   and l.id = icl.storageBin.id");
    hqlString.append("   and l.inventoryStatus.id = invs.id");
    hqlString.append("   and invs.overissue = false");
    hqlString.append(" order by icl.lineNo");

    final Session session = OBDal.getInstance().getSession();
    final Query query = session.createQuery(hqlString.toString());
    query.setString(0, inventory.getId());
    query.setMaxResults(1);

    if (!query.list().isEmpty()) {
      StorageDetail storageDetail = OBDal.getInstance().get(StorageDetail.class,
          query.list().get(0).toString());
      attribute = (!storageDetail.getAttributeSetValue().getIdentifier().isEmpty())
          ? " @PCS_ATTRIBUTE@ '" + storageDetail.getAttributeSetValue().getIdentifier() + "', "
          : "";
      throw new OBException(Utility
          .messageBD(new DalConnectionProvider(), "insuffient_stock",
              OBContext.getOBContext().getLanguage().getLanguage())
          .replaceAll("%1", storageDetail.getProduct().getIdentifier()).replaceAll("%2", attribute)
          .replaceAll("%3", storageDetail.getUOM().getIdentifier())
          .replaceAll("%4", storageDetail.getStorageBin().getIdentifier()));
    }
  }

  private void executeHooks(Instance<? extends Object> hooks, InventoryCount inventory)
      throws Exception {
    if (hooks != null) {
      for (Iterator<? extends Object> procIter = hooks.iterator(); procIter.hasNext();) {
        Object proc = procIter.next();
        if (proc instanceof InventoryCountProcessHook) {
          ((InventoryCountProcessHook) proc).exec(inventory);
        } else {
          ((InventoryCountCheckHook) proc).exec(inventory);
        }
      }
    }
  }
  
  private static String getdocumentNo(String orderId) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String strResult = null;
    try {

      String strSql = "SELECT documentno FROM siblr_physical_inventory WHERE siblr_physical_inventory_id = '" + orderId + "'";
      PreparedStatement st = null;

      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();

      while (rsConsulta.next()) {
        strResult = rsConsulta.getString("documentno");
      }

      return strResult;

    } catch (Exception e) {

      throw new OBException("Error al consultar canton de la Organizacion. " + e.getMessage());
    }

  }  
  
  public static void updateDocstatusInventory(String SiblrPhysicalInventory_ID, String documentNoTemp) {

    ConnectionProvider conn = new DalConnectionProvider(false);
    String strSql = null;
    
    String documentNo = documentNoTemp.substring(documentNoTemp.indexOf("-") + 1);

    strSql = "UPDATE siblr_physical_inventory SET docstatus = 'REG', documentno = '"+documentNo+"' WHERE siblr_physical_inventory_id = '" + SiblrPhysicalInventory_ID + "'";

    int updateCount = 0;
    PreparedStatement st = null;

    try {
      st = conn.getPreparedStatement(strSql);

      updateCount = st.executeUpdate();
      if (updateCount > 0) {
        System.out.println("Estado de transacción actualizado.");
      }
      st.close();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      // throw new OBException(e.getMessage());
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
      // throw new OBException(ex.getMessage());
    } finally {
      try {
        conn.releasePreparedStatement(st);
      } catch (Exception ignore) {
        System.out.println(ignore.getMessage());
        ignore.printStackTrace();
        // throw new OBException(ignore.getMessage());
      }
    }
  }  
  
  public OBError RollBackInventory(String SiblrPhysicalInventory_ID, String inventoryID)
      throws OBException {
    OBError msg = new OBError();
    msg.setType("Error");
    msg.setTitle(OBMessageUtils.messageBD("Error"));

    SiblrPhysicalInventory SiblrPhysicalInventoryObj = OBDal.getInstance()
        .get(SiblrPhysicalInventory.class, SiblrPhysicalInventory_ID);
   
    updateDocstatusInventory(SiblrPhysicalInventory_ID, SiblrPhysicalInventoryObj.getDocumentNo());

    final InventoryCount inventoryObj = OBDal.getInstance().get(InventoryCount.class, inventoryID);

    OBCriteria<InventoryCountLine> ObjInventoryCountLine = OBDal.getInstance()
        .createCriteria(InventoryCountLine.class);
//    ObjInventoryCountLine
//        .add(Restrictions.eq(InventoryCountLine.PROPERTY_PHYSINVENTORY, inventoryObj));
    ObjInventoryCountLine
    .add(Restrictions.eq(InventoryCountLine.PROPERTY_ID, inventoryObj.getId()));    

    if (ObjInventoryCountLine.list().size() > 0) {

      for (InventoryCountLine coldelete : ObjInventoryCountLine.list()) {

        InventoryCountLine removeInvLine = OBDal.getInstance().get(InventoryCountLine.class,
            coldelete.getId());

        OBDal.getInstance().remove(removeInvLine);
      }

    }
    OBDal.getInstance().remove(inventoryObj);
    OBDal.getInstance().commitAndClose();

    return msg;
  }  
  
}
