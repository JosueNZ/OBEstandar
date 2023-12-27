/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

/**
 *
 * @author nicholas
 */
public class FocusRequestDispatcher extends HttpBaseServlet{
     @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
         
        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }
         
        response.setContentType("application/json; charset=utf-8");
    
        VariablesSecureApp vars = new VariablesSecureApp(request);
        String lang = vars.getLanguage();
        if (lang.isEmpty())
            lang=OBContext.getOBContext().getLanguage().getLanguage();
         
        String recordId = request.getParameter("recordId");
        LookupEnum enReq = LookupEnum.valueOf(request.getParameter("lookupType"));
        
        PrintWriter out = response.getWriter();

        try {
            out.println(Dispatch(recordId,enReq,lang));
        } catch (Exception ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }
        
     }
     
     public String Dispatch(String recordId, LookupEnum lookup, String lang){
        String res="";
        ArrayList dispatched = new ArrayList();
        Detail names = new Detail("","",""); 
        
        switch (lookup) {
            
            case QUOTATION:
                dispatched = GetOrderLineArray(recordId);
                names = GetOrderLineNames(lang);
                break;
            
            case ORDER:
                dispatched = GetOrderLineArray(recordId);
                names = GetOrderLineNames(lang);
                break;
            case SHIPMENT:
                dispatched = GetShipLineArray(recordId);
                names = GetShipLineNames(lang);
                break;
                
            case INVOICE:
                dispatched = GetInvoiceLineArray(recordId);
                names = GetInvoiceLineNames(lang);
                break;
                
            case ACTIVITY:
                
                break;
                
            case OPPORTUNITY:
                
                break;
        }
        
        return ToJson(dispatched,names);
     }
     
     public ArrayList <Detail> GetOrderLineArray(String recordId){
         ArrayList <Detail> res = new ArrayList<Detail>();
         
         final OBCriteria<Order> orderList = OBDal.getInstance().createCriteria(Order.class);
         orderList.add(Restrictions.eq(Order.PROPERTY_ID,recordId));
         
         final OBCriteria<OrderLine> orderLineList = OBDal.getInstance().createCriteria(OrderLine.class);
         orderLineList.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER,orderList.list().get(0)));
         
         for(OrderLine ol : orderLineList.list()){
             res.add(new Detail( ol.getProduct().getSearchKey() +" : "+ ol.getProduct().getName(),
                                (ol.getOrderedQuantity()!=null) ? Double.toString(ol.getOrderedQuantity().doubleValue()) : "",
                                (ol.getLineNetAmount()!=null) ? ol.getLineNetAmount().toString() : "",
                                 ol.getId()));
         }
         
         return res;
     }
     
     public Detail GetOrderLineNames(String lang){
        
        String fieldName="Name";
        String fieldQty="QuantityOrder";
        String fieldNetAmt="LineNetAmt";
        
        return new Detail (getTrlName(fieldName,lang), getTrlName(fieldQty,lang) ,getTrlName(fieldNetAmt,lang));
    }
     
     public ArrayList <Detail> GetShipLineArray(String recordId){
         ArrayList <Detail> res = new ArrayList<Detail>();
         
         final OBCriteria<ShipmentInOut> shipList = OBDal.getInstance().createCriteria(ShipmentInOut.class);
         shipList.add(Restrictions.eq(ShipmentInOut.PROPERTY_ID,recordId));
         
         final OBCriteria<ShipmentInOutLine> shipLineList = OBDal.getInstance().createCriteria(ShipmentInOutLine.class);
         shipLineList.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT,shipList.list().get(0)));
         
         for(ShipmentInOutLine sl : shipLineList.list()){
             
             //inserisci in res
             if(sl.getProduct()!=null)
                res.add(new Detail( sl.getProduct().getSearchKey() +" : "+ sl.getProduct().getName() ,
                                (sl.getMovementQuantity()!=null) ? sl.getMovementQuantity().toString() :"",
                                (sl.getUOM() != null) ? sl.getUOM().getName() :"",
                                 sl.getId()));
         }
         
         return res;
     }
     
     public Detail GetShipLineNames(String lang){
        
        String fieldName="Name";
        String fieldMovQty="MovementQty";
        String fieldNetAmt="C_UOM_ID";
        
        return new Detail (getTrlName(fieldName,lang), getTrlName(fieldMovQty,lang) ,getTrlName(fieldNetAmt,lang));
    }
     
     public ArrayList <Detail> GetInvoiceLineArray(String recordId){
         ArrayList <Detail> res = new ArrayList<Detail>();
         
         final OBCriteria<Invoice> invList = OBDal.getInstance().createCriteria(Invoice.class);
         invList.add(Restrictions.eq(Invoice.PROPERTY_ID,recordId));
         
         final OBCriteria<InvoiceLine> invLineList = OBDal.getInstance().createCriteria(InvoiceLine.class);
         invLineList.add(Restrictions.eq(InvoiceLine.PROPERTY_INVOICE,invList.list().get(0)));
         
         for(InvoiceLine il : invLineList.list()){
             //inserisci in res
             res.add(new Detail( il.getProduct().getSearchKey() +" : "+ il.getProduct().getName(),
                                (il.getLineNetAmount()!=null) ? il.getLineNetAmount().toString():"",
                                (il.getInvoicedQuantity()!=null) ? il.getInvoicedQuantity().toString():"",
                                 il.getId()));
         }
         
         return res;
     }
     
     public Detail GetInvoiceLineNames(String lang){
        
        String fieldName="Name";
        String fieldNetAmt="LineNetAmt";
        String fieldQtyInvoiced="QtyInvoiced";
        
        return new Detail (getTrlName(fieldName,lang), getTrlName(fieldNetAmt,lang) ,getTrlName(fieldQtyInvoiced,lang));
    }
     
     public String ToJson(ArrayList <Detail> focusList, Detail localization){
        
        
        DetailList detList = new DetailList();
        detList.setDetailList(focusList);
        detList.setValueNames(localization);
        return JSONSerializer.getJSON(detList);
        
     }
     
     public String getTrlName(String columnName, String lang){
        String res="";
        boolean found=false;
        
        OBContext.setAdminMode(true);
        
        final OBCriteria<Column> colList = OBDal.getInstance().createCriteria(Column.class);
        colList.add(Restrictions.eq(Column.PROPERTY_DBCOLUMNNAME, columnName));
        
        final OBCriteria<Field> fieldList = OBDal.getInstance().createCriteria(Field.class);
        fieldList.add(Restrictions.eq(Field.PROPERTY_COLUMN, colList.list().get(0)));
        
        final OBCriteria<FieldTrl> fieldTrlList = OBDal.getInstance().createCriteria(FieldTrl.class);
        fieldTrlList.add(Restrictions.eq(FieldTrl.PROPERTY_FIELD,fieldList.list().get(0)));
        
        
        for(FieldTrl trl : fieldTrlList.list()){
            if(trl.getLanguage().getLanguage().equals(lang)){
                res = trl.getName();
                found=true;
            }
        }        
                
        if(!found)
            res = fieldList.list().get(0).getName();
        
        OBContext.restorePreviousMode();
        
        return res;
    }
     
     
     
     
    
}
