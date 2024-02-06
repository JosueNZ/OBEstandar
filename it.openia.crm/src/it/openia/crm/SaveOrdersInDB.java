///* Copyright (C) 2008-2013 Openia S.r.l.
//   - This Source Code Form is subject to the terms of the Mozilla Public
//   - License, v. 2.0. If a copy of the MPL was not distributed with this
//   - file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package it.openia.crm;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;

/**
 *
 * @author nicholas
 */
public class SaveOrdersInDB extends HttpBaseServlet{
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        
        PrintWriter out = response.getWriter();
        response.setContentType("text/plain; charset=utf-8");
        
        //Checking the session with AuthUtils.isLoggeOut(request, response, getServletContext()) will return the login.jsp page.
        //Checking the session by calling OBContext.getObContext() in order to return a custom error string in case of expiration.
        if (OBContext.getOBContext() == null){
            out.print("ERROR: User Session expired, please log in"); 
            return;
        }
        
        
        VariablesSecureApp vars = new VariablesSecureApp(request);

        String strUserId = vars.getUser();
        String strOrgId = OBContext.getOBContext().getCurrentOrganization().getId();
        String strClientId = OBContext.getOBContext().getCurrentClient().getId();
        
        if (strUserId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getUser().getId();
        }
        
        String jsonOrders;  
        if(request.getParameter("jsonOrders")!=null)
            jsonOrders = request.getParameter("jsonOrders");
        else jsonOrders = "";
        
        Gson gson = new Gson();
        
        OpcrmOrder ordersFromJson = gson.fromJson(jsonOrders,  OpcrmOrder.class);
        
        try {
            //if(ordersFromJson.length==1)
                out.print(SaveOrderNew(ordersFromJson, strOrgId, strClientId, strUserId));
            //else    
            //    out.print(SaveOrders(ordersFromJson, strOrgId, strClientId, strUserId)); DEPRECATED SINCE 18/04/2014
        } catch (Exception ex) {
            out.print("ERROR: " + ex.getMessage());
            Logger.getLogger(SaveOrdersInDB.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    //DEPRECATED SINCE 18/04/2014
    public int SaveOrders(OpcrmOrder [] orders, String orgId, String clientId, String usrId){
        
        Long lineNumber = null;
        ArrayList <OpcrmOrderline> returnItems = null;
        
        for(OpcrmOrder o : orders){
            
            lineNumber = new Long(10);
            
            // SAVE ORDER
            Order tmp = SaveOrder(o,orgId,clientId,usrId);

            for(OpcrmOrderline ol : o.getOrderLines()){
                SaveOrderline(ol,tmp, orgId, clientId,usrId,lineNumber);
                lineNumber+=10;        
            }

            OBDal.getInstance().flush();

            //COMPLETING THE ORDER:...
            try{
                OBContext.setAdminMode(true);
                long ordRes = bookOrder(tmp.getId());
                OBContext.restorePreviousMode();
            }catch(SQLException e){
                Logger.getLogger(SaveOrdersInDB.class.getName()).log(Level.SEVERE, null, e);
                return -1;
            }
            
        }
        
        return orders.length;
    }
    
    public String SaveOrderNew(OpcrmOrder order, String orgId, String clientId, String usrId){
    
        Long lineNumber = new Long(10);
        
        // SAVE ORDER
        Order tmp = SaveOrder(order,orgId,clientId,usrId);

        for(OpcrmOrderline ol : order.getOrderLines()){
            SaveOrderline(ol,tmp, orgId, clientId,usrId,lineNumber);
            lineNumber+=10;        
        }

        OBDal.getInstance().flush();

        //COMPLETING THE ORDER:...
        try{
            OBContext.setAdminMode(true);
            long ordRes = bookOrder(tmp.getId());
            OBContext.restorePreviousMode();
        }catch(SQLException e){
            Logger.getLogger(SaveOrdersInDB.class.getName()).log(Level.SEVERE, null, e);
            return "ERROR: "+e.toString();
        }
        
        return order.getOrderIndex();
    }
    
    
    public Order SaveOrder(OpcrmOrder ord, String orgId, String clientId, String usrId){
        
        Date orderDate = null;
        Date deliveryDate = null;
        
        /* CLIENT */
        final OBCriteria<Client> clientList = OBDal.getInstance().createCriteria(Client.class);
        clientList.add(Restrictions.eq(Client.PROPERTY_ID, clientId));
        /* USER */
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, usrId));
        /* ORGANIZATION */
        final OBCriteria<Organization> orgList = OBDal.getInstance().createCriteria(Organization.class);
        orgList.add(Restrictions.eq(Organization.PROPERTY_ID, orgId));
        /* BUSINESS PARTNER */
        final OBCriteria<BusinessPartner> bpList = OBDal.getInstance().createCriteria(BusinessPartner.class);
        bpList.add(Restrictions.eq(BusinessPartner.PROPERTY_ID,ord.getBPartnerId()));
        /* BUSINESS PARTNER ADDRESS */
        final OBCriteria<Location> bpLocationList = OBDal.getInstance().createCriteria(Location.class);
        bpLocationList.add(Restrictions.eq(Location.PROPERTY_ID,ord.getBPartnerAddressId()));
        /* TRANSACTION DOCUMENT */
        final OBCriteria<DocumentType> docTList = OBDal.getInstance().createCriteria(DocumentType.class);
        docTList.add(Restrictions.eq(DocumentType.PROPERTY_ID,ord.getTrxDocId()));
        DocumentType dType = docTList.list().get(0);
       
        /* ORDER DATE & DELIVERY DATE */
        SimpleDateFormat dayMonthYear =  new SimpleDateFormat("dd/MM/yyyy");
        try{
            orderDate = dayMonthYear.parse(ord.getOrderDate());
            deliveryDate = dayMonthYear.parse(ord.getDeliveryDate());
        }catch (ParseException e){
            Logger.getLogger(SaveOrdersInDB.class.getName()).log(Level.SEVERE, null, e);
        }
        /* PAYMENT TERM */
        final OBCriteria<PaymentTerm> termList = OBDal.getInstance().createCriteria(PaymentTerm.class);
        termList.add(Restrictions.eq(PaymentTerm.PROPERTY_ID, ord.getPaymentTermId()));
        /* INVOICE TERM (queried with SearchKey) */
        OBContext.setAdminMode(true);
        String referenceName = "C_Order InvoiceRule";
        
        final OBCriteria<Reference> referenceList = OBDal.getInstance().createCriteria(Reference.class);
        referenceList.add(Restrictions.eq(Reference.PROPERTY_NAME,referenceName));
        
        final OBCriteria <org.openbravo.model.ad.domain.List> reflistList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
        reflistList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY,ord.getInvoiceTermKey()));
        
        OBContext.restorePreviousMode();
        /* INVOICE ADDRESS */
        final OBCriteria<Location> invoiceAddressList = OBDal.getInstance().createCriteria(Location.class);
        invoiceAddressList.add(Restrictions.eq(Location.PROPERTY_ID,ord.getInvoiceAddressId()));
        /* WAREHOUSE */
        final OBCriteria<Warehouse> warehouseList = OBDal.getInstance().createCriteria(Warehouse.class);
        warehouseList.add(Restrictions.eq(Warehouse.PROPERTY_ID,ord.getWarehouseId()));
        
        /* PRICE LIST - - - */
        final OBCriteria<PriceList> pricelistList = OBDal.getInstance().createCriteria(PriceList.class);
        pricelistList.add(Restrictions.eq(PriceList.PROPERTY_ID,ord.getPriceListId()));
        
        /* PAYMENT METHOD - - - */
        final OBCriteria<FIN_PaymentMethod> paymentMethodList = OBDal.getInstance().createCriteria(FIN_PaymentMethod.class);
        paymentMethodList.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_ID,ord.getPaymentMethodId()));
        
        Order newOrd = null;
        
        if(!ord.getOrderIndex().contains("draft"))
            newOrd = OBProvider.getInstance().get(Order.class);
        else{
            final OBCriteria<Order> ordList = OBDal.getInstance().createCriteria(Order.class);
            ordList.add(Restrictions.eq(Order.PROPERTY_ID, ord.getOrderId()));
            newOrd = ordList.list().get(0);
            //for (OrderLine ol : newOrd.getOrderLineList())
            //    OBDal.getInstance().remove(ol);
        }
        
        newOrd.setActive(true);
        newOrd.setCreationDate(new Date());
        newOrd.setUpdated(new Date());
        newOrd.setUpdatedBy(userList.list().get(0));
        newOrd.setCreatedBy(userList.list().get(0));
        newOrd.setClient(clientList.list().get(0));
        newOrd.setCurrency(clientList.list().get(0).getCurrency()); //Client's Default Currency 
        newOrd.setOrganization(orgList.list().get(0));
        newOrd.setBusinessPartner(bpList.list().get(0));
        newOrd.setPartnerAddress(bpLocationList.list().get(0));
        newOrd.setTransactionDocument(dType);
        newOrd.setDocumentType(dType);
        newOrd.setOrderDate(orderDate);
        newOrd.setAccountingDate(orderDate);
        newOrd.setScheduledDeliveryDate(deliveryDate);
        newOrd.setPaymentTerms(termList.list().get(0));
        newOrd.setPaymentMethod(paymentMethodList.list().get(0));
        
        OBContext.setAdminMode(true);
        newOrd.setInvoiceTerms(reflistList.list().get(0).getSearchKey());
        OBContext.restorePreviousMode();
        
        newOrd.setInvoiceAddress(invoiceAddressList.list().get(0));
        newOrd.setWarehouse(warehouseList.list().get(0));
        newOrd.setPriceList(pricelistList.list().get(0));
        
        OBDal.getInstance().save(newOrd);
        OBDal.getInstance().flush();
        
        return newOrd;
    }
    
    /**
         * Book an order. This process will create a record in ad_pinstance
         * and will call the stored procedure C_ORDER_POST.<br/>
         * <b>Make sure to be in administrative mode before executing this procedure.</b>
         * 
         * @param orderId The order's ID to be booked
         * @return An integer representing the result of C_ORDER_POST
         * @throws SQLException
         */
        private long bookOrder( String orderId ) throws SQLException
        {
            // Create the ad_pinstance record, from the ad_process table, id:104 -> C_Order Post process ID
            final org.openbravo.model.ad.ui.Process process = OBDal.getInstance().get( org.openbravo.model.ad.ui.Process.class, "104" );
            final ProcessInstance pInstance = 
                            OBProvider.getInstance().get( ProcessInstance.class );
            pInstance.setProcess( process );
            pInstance.setActive( true );
            pInstance.setRecordID( orderId );
            pInstance.setUserContact( OBContext.getOBContext().getUser() );
            OBDal.getInstance().save( pInstance );
            OBDal.getInstance().flush();
            
            
            // Perform the C_ORDER_POST stored procedure
            Connection connection = OBDal.getInstance().getConnection();
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM C_ORDER_POST(?)");
            ps.setString( 1, pInstance.getId() );
            ps.execute();
            
            // Update the pInstance object and return the result
            OBDal.getInstance().getSession().refresh( pInstance );
            return pInstance.getResult();
        }
    
    public void SaveOrderline (OpcrmOrderline ordline, Order ord, String orgId, String clientId, String usrId, Long lineNo){
        
        /* CLIENT */
        final OBCriteria<Client> clientList = OBDal.getInstance().createCriteria(Client.class);
        clientList.add(Restrictions.eq(Client.PROPERTY_ID, clientId));
        /* USER */
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, usrId));
        /* ORGANIZATION */
        final OBCriteria<Organization> orgList = OBDal.getInstance().createCriteria(Organization.class);
        orgList.add(Restrictions.eq(Organization.PROPERTY_ID, orgId));
        /* PRODUCT */
        final OBCriteria<Product> productList = OBDal.getInstance().createCriteria(Product.class);
        productList.add(Restrictions.eq(DocumentType.PROPERTY_ID,ordline.getProductId()));
        Product p = productList.list().get(0);
        
        
        /* TAX RATE */
        final OBCriteria<TaxRate> taxList = OBDal.getInstance().createCriteria(TaxRate.class);
        
        //if(p.getTaxCategory() != null){
            taxList.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY,p.getTaxCategory()));
            taxList.add(Restrictions.eq(TaxRate.PROPERTY_ACTIVE,true));
        //}
        
        
        OrderLine newOrdLine = null;
        //if(ordline.getOrderLineIndex()==null || ordline.getOrderLineIndex().isEmpty()) //MODIFICA ANDREA E VANNI
            newOrdLine = OBProvider.getInstance().get(OrderLine.class);
        /*else{  //MODIFICA ANDREA E VANNI
            final OBCriteria<OrderLine> ordLine = OBDal.getInstance().createCriteria(OrderLine.class); //MODIFICA ANDREA E VANNI
            ordLine.add(Restrictions.eq(OrderLine.PROPERTY_ID, ordline.getOrderLineIndex())); //MODIFICA ANDREA E VANNI
            
            newOrdLine = ordLine.list().get(0); //MODIFICA ANDREA E VANNI
        }*/ //MODIFICA ANDREA E VANNI
        
        
        newOrdLine.setSalesOrder(ord);
        newOrdLine.setOrderDate(ord.getOrderDate()); // -- getting DateOrder from order header
        newOrdLine.setWarehouse(ord.getWarehouse()); // -- getting Warehouse from order header
        newOrdLine.setCurrency(ord.getCurrency());  // -- getting Currency from order header
        newOrdLine.setActive(true);
        newOrdLine.setCreationDate(new Date());
        newOrdLine.setUpdated(new Date());
        newOrdLine.setUpdatedBy(userList.list().get(0));
        newOrdLine.setCreatedBy(userList.list().get(0));
        newOrdLine.setClient(clientList.list().get(0));
        newOrdLine.setOrganization(orgList.list().get(0));
        newOrdLine.setProduct(p);
        newOrdLine.setUOM(productList.list().get(0).getUOM()); // -- getting default transaction unit of measure from product -- HARDCODED!! --
        
        BigDecimal qty=null;
        
        double grossAmt;
        if(ordline.getReturnItem() == null || ordline.getReturnItem().matches("false")){
            grossAmt = Double.parseDouble(ordline.getUnitPrice()) * Double.parseDouble(ordline.getQuantity());
            newOrdLine.setLineGrossAmount( new BigDecimal(grossAmt));
            qty=new BigDecimal(ordline.getQuantity());
        }
        else{
            grossAmt = Double.parseDouble(ordline.getUnitPrice()) * Double.parseDouble(ordline.getQuantity());
            newOrdLine.setLineGrossAmount( new BigDecimal(grossAmt).negate());
            qty = new BigDecimal(ordline.getQuantity()).negate();
        }
        newOrdLine.setOrderedQuantity(qty);
        
        newOrdLine.setUnitPrice(new BigDecimal(ordline.getNetPrice()));
        
        newOrdLine.setListPrice(new BigDecimal(ordline.getListPrice())); 
        newOrdLine.setGrossUnitPrice(new BigDecimal(ordline.getUnitPrice()));
        newOrdLine.setGrossListPrice(new BigDecimal(ordline.getListPrice()));
        
        double netAmt = Double.parseDouble(ordline.getNetPrice()) * Double.parseDouble(ordline.getQuantity());
        newOrdLine.setLineNetAmount(new BigDecimal(netAmt));
        
        double discount=0;
        if(ordline.getDiscount()!=null)
            discount = Double.parseDouble(ordline.getDiscount());
        newOrdLine.setDiscount(new BigDecimal(discount));
        
        //Getting tax rate from product->cTaxCategory->cTax
        TaxRate r=null;
        //if(p.getTaxCategory() != null &&  !taxList.list().isEmpty())
        r=taxList.list().get(0);
        
       //if(r!=null)
            newOrdLine.setTax(r); //Assuming a one on one TaxCategory <-> TaxRate Relation
        //else newOrdLine.setTax(p.getTaxCategory().getFinancialMgmtTaxRateList().get(0)); //FIXME: setting a random TaxRate if there is no active taxrate -- HARDCODED!! --
        
        newOrdLine.setLineNo(lineNo);
        
        OBDal.getInstance().save(newOrdLine);
       // OBDal.getInstance().flush();
        
    }
    
}
