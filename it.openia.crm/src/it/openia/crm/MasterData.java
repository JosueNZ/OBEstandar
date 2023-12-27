/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package it.openia.crm;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.domain.ListTrl;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.FieldTrl;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.payment.PaymentTermTrl;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;

/**
 *
 * @author nicholas
 */
public class MasterData extends HttpBaseServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {

        if (AuthUtils.isLoggeOut(request, response, getServletContext())) {
            return;
        }

        response.setContentType("text/javascript; charset=utf-8");

        VariablesSecureApp vars = new VariablesSecureApp(request);

        String strUserId = vars.getUser();
        String strOrgId = OBContext.getOBContext().getCurrentOrganization().getId();

        if (strUserId == null || "".equals(strUserId)) { // in case of mobile ...
            strUserId = OBContext.getOBContext().getUser().getId();
        }

        /*
         * if (strOrgId == null || "".equals(strOrgId)) { // in case of mobile
         * ... strOrgId =
         * OBContext.getOBContext().getCurrentOrganization().getId(); }
         */

        String lang = vars.getLanguage();
        if (lang.isEmpty()) {
            lang = OBContext.getOBContext().getLanguage().getLanguage();
        }

        String sessionWareHouseId = "";
        
        try {
            sessionWareHouseId = OBContext.getOBContext().getWarehouse().getId();
        }catch (NullPointerException ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        PrintWriter out = response.getWriter();

        StringBuilder MasterDataOutput = new StringBuilder();

        try {

            MasterDataOutput.append(CreateBPartners());
            MasterDataOutput.append(CreateBPAddresses());
            MasterDataOutput.append(CreateOrderEntries(lang));
            MasterDataOutput.append(CreateTransactionDocs());
            MasterDataOutput.append(CreatePaymentTerms(lang));
            MasterDataOutput.append(CreateInvoiceTerms(lang));
            MasterDataOutput.append(CreateWarehouses(sessionWareHouseId));
            MasterDataOutput.append(CreatePriceLists());
            
            //MasterDataOutput.append(CreateProducts());
            //String test = CreateProductsFromView();
            MasterDataOutput.append(CreateProductsFromView());
            
            MasterDataOutput.append(CreateProductCategories());
            MasterDataOutput.append(CreateOrderLineEntries(lang));
            MasterDataOutput.append(CreatepaymentMethod());
            
            
            
            out.println(MasterDataOutput.toString());

        } catch (Exception ex) {
            Logger.getLogger(GetEvents.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    // - - - GENERATING BUSINESS PARTNERS - - - //
    private String CreateBPartners() {
        int count = 0;
        StringBuilder str = new StringBuilder();
        try {

            // Write BPartners 
            String id = "";
            String name = "";
            String loc = "";
            String locId = "";
            String pListId = "";
            String docId="";
            boolean isdefault=false;
            
            String paymentTerms = "";
            String invoiceTerms = "";
            String paymentMethod = "";

            str.append("var myBPS=new Array();\n");

            for (BusinessPartner bp : GetPartners()) {

                id = bp.getId();
                name = bp.getName();

                if (bp.getPriceList() != null) {
                    pListId = bp.getPriceList().getId();
                } else {
                    pListId = "";
                }

                if (bp.getPaymentTerms() != null) {
                    paymentTerms = bp.getPaymentTerms().getId();
                }

                if (bp.getInvoiceTerms() != null) {
                    invoiceTerms = bp.getInvoiceTerms();
                }

                if (bp.getPaymentMethod() != null) {
                    paymentMethod = bp.getPaymentMethod().getId();
                }

                if (!bp.getBusinessPartnerLocationList().isEmpty() && bp.getBusinessPartnerLocationList().get(0) != null) {
                    loc = bp.getBusinessPartnerLocationList().get(0).getName();
                    locId = bp.getBusinessPartnerLocationList().get(0).getId();
                } else {
                    loc = "";
                    locId = "";
                }
                
                if(bp.getOpcrmDoctype()!=null){
                    docId=bp.getOpcrmDoctype().getId();
                }
                else docId="";
                
                if(bp.isOpcrmIsposdefault()!=null)
                    isdefault=bp.isOpcrmIsposdefault();
                else isdefault=false;
                    
                str.append("myBPS[").append(count).append("]= new BusinessPartner(\"").append(id).append("\",\"").append(name).append("\",\"").append(loc).append("\",\"").append(locId).append("\",\"").
                                                                                      append(pListId).append("\",\"").append(paymentTerms).append("\",\"").
                                                                                    append(invoiceTerms).append("\",\"").append(paymentMethod).
                                                                                    append("\",\"").append(docId).append("\",\"").append(isdefault).append("\");\n");
                count++;

            }

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        return str.toString();

    }

    public List<BusinessPartner> GetPartners() {

        final OBCriteria<BusinessPartner> bpList = OBDal.getInstance().createCriteria(BusinessPartner.class);
        bpList.addOrderBy(BusinessPartner.PROPERTY_NAME, true);

        return bpList.list();
    }

    // - - - GENERATING BUSINESS PARTNER ADDRESSES - - - //
    private String CreateBPAddresses() {

        int count = 0;
        StringBuilder str = new StringBuilder();

        try {

            String addressId = "";
            String bPartnerId = "";
            String locationName = "";

            str.append("var myBPAddresses = new Array();\n");

            for (Location bpLoc : GetBPAddresses()) {

                addressId = bpLoc.getId();
                bPartnerId = bpLoc.getBusinessPartner().getId();
                locationName = bpLoc.getName();

                str.append("myBPAddresses[").append(count).append("]= new BPartnerAddress(\"").append(addressId).append("\",\"").append(bPartnerId).append("\",\"").append(locationName).append("\");\n");
                count++;

            }


        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        return str.toString();
    }

    public List<Location> GetBPAddresses() {

        final OBCriteria<Location> bpLocList = OBDal.getInstance().createCriteria(Location.class);

        return bpLocList.list();
    }

    // - - - GENERATING TRANSACTION DOCUMENTS - - - - - //
    private String CreateTransactionDocs() {
        int count = 0;
        StringBuilder str = new StringBuilder();

        try {
            String docId = "";
            String name = "";

            str.append("var myTrxDocuments = new Array();\n");

            for (DocumentType doc : GetDocTypes()) {

                docId = doc.getId();
                name = doc.getName();

                str.append("myTrxDocuments[").append(count).append("]= new TransactionDocument(\"").append(docId).append("\",\"").append(name).append("\");\n");
                count++;

            }

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        return str.toString();
    }

    public List<DocumentType> GetDocTypes() {
        final OBCriteria<DocumentType> docTList = OBDal.getInstance().createCriteria(DocumentType.class);
        docTList.add(Restrictions.eq(DocumentType.PROPERTY_SALESTRANSACTION, true));
        docTList.add(Restrictions.eq(DocumentType.PROPERTY_RETURN, false));
        docTList.add(Restrictions.ne(DocumentType.PROPERTY_SOSUBTYPE, "OB"));

        Disjunction orSOO;
        Disjunction orPOO;

        orSOO = Restrictions.disjunction();
        orSOO.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, "SOO"));
        orPOO = Restrictions.disjunction();
        orPOO.add(Restrictions.eq(DocumentType.PROPERTY_DOCUMENTCATEGORY, "POO"));

        docTList.add(Restrictions.or(orSOO, orPOO));


        return docTList.list();
    }

    // - - - GENERATING PAYMENT TERMS - - - - //
    private String CreatePaymentTerms(String lang) {
        int count = 0;
        StringBuilder str = new StringBuilder();

        OBCriteria<PaymentTermTrl> termTrlList;
        OBCriteria<Language> langList = null;

        OBContext.setAdminMode(true);

        try {
            String termId = "";
            String name = "";

            str.append("var myPaymentTerms = new Array();\n");

            if (!lang.matches("en_US")) {
                langList = OBDal.getInstance().createCriteria(Language.class);
                langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
            }

            for (PaymentTerm term : GetPaymentTerms()) {

                termId = term.getId();
                name = term.getName();

                str.append("myPaymentTerms[").append(count).append("]= new PaymentTerm(\"").append(termId).append("\",\"");

                if (lang.matches("en_US")) {
                    str.append(name).append("\");\n");
                } else {
                    termTrlList = OBDal.getInstance().createCriteria(PaymentTermTrl.class);
                    termTrlList.add(Restrictions.eq(ListTrl.PROPERTY_LANGUAGE, langList.list().get(0)));
                    termTrlList.add(Restrictions.eq(PaymentTermTrl.PROPERTY_PAYMENTTERMS, term));

                    if (!termTrlList.list().isEmpty()) {
                        str.append(termTrlList.list().get(0).getName()).append("\");\n");
                    } else {
                        str.append(name).append("\");\n");
                    }

                }

                count++;

            }


        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        OBContext.restorePreviousMode();

        return str.toString();
    }

    public List<PaymentTerm> GetPaymentTerms() {

        final OBCriteria<PaymentTerm> termList = OBDal.getInstance().createCriteria(PaymentTerm.class);
        termList.addOrderBy(BusinessPartner.PROPERTY_NAME, true);

        return termList.list();
    }

    // - - - GENERATING INVOICE TERMS - - - //
    private String CreateInvoiceTerms(String lang) {

        int count = 0;
        StringBuilder str = new StringBuilder();

        OBCriteria<ListTrl> refTrlList;
        OBCriteria<Language> langList = null;

        OBContext.setAdminMode(true);

        str.append("var myInvoiceTerms = new Array();\n");

        if (!lang.matches("en_US")) {
            langList = OBDal.getInstance().createCriteria(Language.class);
            langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        }

        try {

            String key = "";
            String name = "";

            for (org.openbravo.model.ad.domain.List l : GetInvoiceTerms()) {

                key = l.getSearchKey();
                name = l.getName();

                str.append("myInvoiceTerms[").append(count).append("]= new InvoiceTerm(\"").append(key).append("\",\"");

                if (lang.matches("en_US")) {
                    str.append(name).append("\");\n");
                } else {
                    refTrlList = OBDal.getInstance().createCriteria(ListTrl.class);
                    refTrlList.add(Restrictions.eq(ListTrl.PROPERTY_LANGUAGE, langList.list().get(0)));
                    refTrlList.add(Restrictions.eq(ListTrl.PROPERTY_LISTREFERENCE, l));

                    if (!refTrlList.list().isEmpty()) {
                        str.append(refTrlList.list().get(0).getName()).append("\");\n");
                    } else {
                        str.append(name).append("\");\n");
                    }
                }


                count++;

            }

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        OBContext.restorePreviousMode();

        return str.toString();
    }

    public List<org.openbravo.model.ad.domain.List> GetInvoiceTerms() {

        String referenceName = "C_Order InvoiceRule";

        final OBCriteria<Reference> referenceList = OBDal.getInstance().createCriteria(Reference.class);
        referenceList.add(Restrictions.eq(Reference.PROPERTY_NAME, referenceName));

        final OBCriteria<org.openbravo.model.ad.domain.List> reflistList = OBDal.getInstance().createCriteria(org.openbravo.model.ad.domain.List.class);
        reflistList.add(Restrictions.eq(org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE, referenceList.list().get(0)));

        return reflistList.list();
    }

    // - - - GENERATING WAREHOUSES - - - //
    private String CreateWarehouses(String sessionWareHouseId) {

        int count = 0;
        StringBuilder str = new StringBuilder();

        try {
            String warehouseId = "";
            String name = "";

            str.append("var myWarehouses = new Array();\n");

            for (Warehouse w : GetWarehouses()) {

                warehouseId = w.getId();
                name = w.getName();

                str.append("myWarehouses[").append(count).append("]= new Warehouse(\"").append(warehouseId).append("\",\"").append(name).append("\",").append(warehouseId.matches(sessionWareHouseId)).append(");\n");
                count++;

            }

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        return str.toString();
    }

    public List<Warehouse> GetWarehouses() {

        final OBCriteria<Warehouse> warehouseList = OBDal.getInstance().createCriteria(Warehouse.class);
        warehouseList.add(Restrictions.eq(Warehouse.PROPERTY_ACTIVE, true));

        return warehouseList.list();
    }

    // - - - GENERATING PRODUCTS - - - //
    private String CreateProducts() {

        int count = 0;
        StringBuilder str = new StringBuilder();

        try {
            String productId = "";
            String name = "";
            String standardPrice = "";
            String pListArray = "";
            String category = "";
            String imageId = "";
            String upc = "";
            BigDecimal taxRate = BigDecimal.ZERO;
            OBCriteria<TaxRate> taxList; 
            
            str.append("var myProducts = new Array();\n");

            for (Product p : GetProducts()) {
                standardPrice = "";
                productId = p.getId();
                name = p.getName().replace("\"","\\\"");
                
                category = p.getProductCategory().getId();
                
                if (p.getPricingProductPriceList() != null
                        && !p.getPricingProductPriceList().isEmpty()
                        && p.getPricingProductPriceList().get(0) != null) {
                    standardPrice = p.getPricingProductPriceList().get(0).getStandardPrice().toString();
                } else {
                    if (p.getStandardCost() != null) {
                        standardPrice = p.getStandardCost().toString();
                    }
                }
                
                if (p.getImage() != null) {
                    imageId = p.getImage().getId();
                } else {
                    imageId = "";
                }

                if (p.getUPCEAN() != null) {
                    upc = p.getUPCEAN();
                } else {
                    upc = "";
                }
                
                pListArray = GetPriceListArray(p);
                
                taxList= OBDal.getInstance().createCriteria(TaxRate.class);
                if(p.getTaxCategory() != null){
                    taxList.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY,p.getTaxCategory()));
                    taxList.add(Restrictions.eq(TaxRate.PROPERTY_ACTIVE,true));
                }
                
                if (!taxList.list().isEmpty())
                    taxRate = taxList.list().get(0).getRate();
                else taxRate = BigDecimal.ZERO;
                
                str.append("myProducts[").append(count).append("]= new Product(\"").append(productId).append("\",\"").append(name).append("\",\"").append(standardPrice).append("\",").append(pListArray).append(",\"").append(category).append("\",\"").append(imageId).append("\",\"").append(upc).append("\",\"").append(taxRate).append("\");\n");
                count++;

            }

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }


        return str.toString();
    }

    public List<Product> GetProducts() {
        
        final OBCriteria<Product> productList = OBDal.getInstance().createCriteria(Product.class);
        productList.add(Restrictions.eq(Product.PROPERTY_ACTIVE, true));        
        productList.addOrderBy(Product.PROPERTY_NAME, true);
        
        return productList.list();
    }
    
    public String GetPriceListArray(Product prod) {
        StringBuilder str = new StringBuilder();

        String pListId = "";
        String stdPrice = "";
        String lstPrice = "";
        boolean includestaxes = false;
        boolean PriceListIsIn = false;
        
        int count = 0;
        
        
        str.append("new Array(");
        
        for (ProductPrice prodPrice : prod.getPricingProductPriceList()) {
            //da gestire con IS ACTIVE!!!!    
            if(prodPrice.getPriceListVersion().isActive()){
                
                pListId = prodPrice.getPriceListVersion().getPriceList().getId();
                includestaxes = prodPrice.getPriceListVersion().getPriceList().isPriceIncludesTax();
                stdPrice = prodPrice.getStandardPrice().toString();
                lstPrice = prodPrice.getListPrice().toString();
                
                
                str.append("new ProductPList(\"").append(pListId).append("\",\"").append(stdPrice).append("\",\"").append(lstPrice).append("\",\"").
                                                    append(includestaxes).append("\")");
                PriceListIsIn=true;
            }
            else PriceListIsIn=false;
            
            count++;

            if (PriceListIsIn && count != prod.getPricingProductPriceList().size()) {
                str.append(", ");
            }
        }

        str.append(")");
        return str.toString();
    }
    
    // - - - GENERATING PRODUCTS FROM OPCRMPRODUCTVIEW (IN DEVELOPMENT!!) - - - //
    private String CreateProductsFromView() {

        int count = 0;
        StringBuilder str = new StringBuilder();
        StringBuilder prodListStr = new StringBuilder();
        StringBuilder pListArray = null;
        
        try {
            String productId = "";
            String name = "";
            String standardPrice = "";
            String category = "";
            String imageId = "";
            String upc = "";String upc2 = "";String upc3 = "";
            BigDecimal taxRate = BigDecimal.ZERO;
            OBCriteria<TaxRate> taxList; 
            
            str.append("var myProducts = new Array();\n");
            List<OpcrmProductviewV> ProductViewList = GetProductsFromView();
            int i;
            int j;
            String tempId="";
            //Product p =null;
            OpcrmProductviewV viewTemp = null;
            
            OBContext.setAdminMode(true);
            
            for (i=0;i<ProductViewList.size();i++) {
                
                OpcrmProductviewV pView = ProductViewList.get(i);
                
                //p = pView.getProduct();
                
                standardPrice = "";
                
                productId = pView.getProduct().getId();
                name = pView.getCommercialName().replace("\"","\\\"");
                
                category = pView.getProductCategory().getId();
                
                standardPrice="";
                
                if (pView.getImage() != null) {
                    imageId = pView.getImage().getId();
                } else {
                    imageId = "";
                }

                if (pView.getUPCEAN() != null) {
                    upc = pView.getUPCEAN();
                } else {
                    upc = "";
                }
                
                if (pView.getUpc2() != null) {
                    upc2 = pView.getUpc2();
                } else {
                    upc2 = "";
                }
                
                if (pView.getUpc3() != null) {
                    upc3 = pView.getUpc3();
                } else {
                    upc3 = "";
                }
                
                StringBuilder strProdList = new StringBuilder();

                String pListId = "";
                String stdPrice = "";
                String lstPrice = "";
                String prodList = "";
                boolean includestaxes = false;
                boolean PriceListIsIn = false;
                boolean EndOfList = false;
                j=0;
                viewTemp=ProductViewList.get(i+j);
                
                pListArray = new StringBuilder();
                pListArray.append("new Array(");
                
                while( productId.matches( viewTemp.getProduct().getId()) && (!EndOfList) ){
                    
                    prodList = GetPriceListObject(viewTemp);
                    
                    if(!prodList.isEmpty())
                        pListArray.append(prodList);
                    
                    j=j+1;
                    
                    if(i+j != ProductViewList.size())
                        viewTemp = ProductViewList.get(i+j);
                    else EndOfList=true;
                        
                    if(!prodList.isEmpty() && (!EndOfList) && productId.matches( viewTemp.getProduct().getId() ))
                        pListArray.append(", ");
                }
                
                pListArray.append(")");
                
                
                taxList= OBDal.getInstance().createCriteria(TaxRate.class);
                if(pView.getTaxCategory() != null){
                    taxList.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY,pView.getTaxCategory()));
                    taxList.add(Restrictions.eq(TaxRate.PROPERTY_ACTIVE,true));
                }
                
                if (!taxList.list().isEmpty())
                    taxRate = taxList.list().get(0).getRate();
                else taxRate = BigDecimal.ZERO;
                
                str.append("myProducts[").append(count).append("]= new Product(\"").append(productId).append("\",\"").append(name).append("\",\"").
                                            append(standardPrice).append("\",").append(pListArray).append(",\"").append(category).append("\",\"").
                                            append(imageId).append("\",\"").append(upc).append("\",\"").append(upc2).append("\",\"").
                                            append(upc3).append("\",\"").append(taxRate).append("\");\n");
                count++;
                
                i= i + j - 1;
            }

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        OBContext.restorePreviousMode();
        return str.toString();
    }
    
    public List <OpcrmProductviewV> GetProductsFromView(){
        
        final OBCriteria<OpcrmProductviewV> prodView = OBDal.getInstance().createCriteria(OpcrmProductviewV.class);
        prodView.addOrderBy(OpcrmProductviewV.PROPERTY_COMMERCIALNAME, true);
        
        return prodView.list();
    }
    
    public String GetPriceListObject(OpcrmProductviewV view){
        String pListId="";
        boolean includestaxes=false;
        BigDecimal stdPrice;
        BigDecimal lstPrice;
        StringBuilder str = new StringBuilder();
        
        OBContext.setAdminMode(true);
        
        if(view.getPriceList() !=null && view.isActiveversion()){
            pListId = view.getPriceList().getId();
            includestaxes = view.getPriceList().isPriceIncludesTax();
            stdPrice = view.getStandardPrice();
            lstPrice = view.getListPrice();
                
                
            str.append("new ProductPList(\"").append(pListId).append("\",\"").append(stdPrice).append("\",\"").append(lstPrice).append("\",\"").
                                                    append(includestaxes).append("\")");
        }
        
        OBContext.restorePreviousMode();
        return str.toString();
    }
    
    //- - - - GENERATING PRODUCT CATEGORIES - - - - - -//
    private String CreateProductCategories() {
        int count = 0;
        StringBuilder str = new StringBuilder();

        try {
            String id = "";
            String name = "";
            String imageId = "";

            str.append("var myProductCategories = new Array();\n");

            for (ProductCategory pCat : GetProductCategories()) {

                id = pCat.getId();
                name = pCat.getName();

                if (pCat.getImage() != null) {
                    imageId = pCat.getImage().getId();
                } else {
                    imageId = "";
                }

                str.append("myProductCategories[").append(count).append("]= new ProductCategory(\"").append(id).append("\",\"").append(name).append("\",\"").append(imageId).append("\");\n");
                count++;

            }

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        return str.toString();

    }

    public List<ProductCategory> GetProductCategories() {

        final OBCriteria<ProductCategory> prodCatList = OBDal.getInstance().createCriteria(ProductCategory.class);
        prodCatList.addOrderBy(ProductCategory.PROPERTY_NAME, true);

        return prodCatList.list();
    }

    // - - - - - GENERATING PRICE LIST - - - - - - - - //
    private String CreatePriceLists() {

        int count = 0;
        StringBuilder str = new StringBuilder();

        try {
            String listId = "";
            String listName = "";
            boolean isDefault;

            str.append("var myPriceLists = new Array();\n");

            for (PriceList pl : GetPriceLists()) {

                listId = pl.getId();
                listName = pl.getName();
                isDefault = pl.isDefault();

                str.append("myPriceLists[").append(count).append("]= new PriceList(\"").append(listId).append("\",\"").append(listName).append("\",\"").append(isDefault).append("\");\n");

                count++;

            }


        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }

        return str.toString();
    }

    public List<PriceList> GetPriceLists() {

        final OBCriteria<PriceList> pList = OBDal.getInstance().createCriteria(PriceList.class);
        pList.add(Restrictions.eq(PriceList.PROPERTY_SALESPRICELIST, true));

        return pList.list();
    }

    // - - - - - GENERATING PAYMENT METHOD - - - - - - - - //
    private String CreatepaymentMethod() {

        int count = 0;
        StringBuilder str = new StringBuilder();

        try {
            String id = "";
            String name = "";

            str.append("var myPaymentMethods = new Array();\n");

            for (FIN_PaymentMethod pMet : GetPaymentMethod()) {

                id = pMet.getId();
                name = pMet.getName();

                str.append("myPaymentMethods[").append(count).append("]= new PaymentMethod(\"").append(id).append("\",\"").append(name).append("\");\n");
                count++;

            }

        } catch (Exception e) {//Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }


        return str.toString();
    }

    public List<FIN_PaymentMethod> GetPaymentMethod() {

        final OBCriteria<FIN_PaymentMethod> paymentMethodList = OBDal.getInstance().createCriteria(FIN_PaymentMethod.class);
        paymentMethodList.addOrderBy(BusinessPartner.PROPERTY_NAME, true);
        
        return paymentMethodList.list();
    }

    // - - - GENERATING ORDER HEADER FIELDS - - - //
    private String CreateOrderEntries(String lang) {

        StringBuilder res = new StringBuilder();

        //hardcoded field ids
        String BusinessPartner = "1573";
        String PartnerAddress = "2590";
        String TransactionDocument = "1085";
        String OrderDate = "1093";
        String ScheduledDeliveryDate = "1094";
        String PaymentTerms = "1099";
        String InvoiceTerms = "1104";
        String InvoiceAddress = "1101";
        String Warehouse = "1114";
        String Pricelist = "1173";
        String PaymentMethod = "B4F8BA22DBD249B29BE578A68377D0AB";

        ArrayList<String> fieldIds = new ArrayList<String>();

        fieldIds.add(BusinessPartner);
        fieldIds.add(PartnerAddress);
        fieldIds.add(TransactionDocument);
        fieldIds.add(OrderDate);
        fieldIds.add(ScheduledDeliveryDate);
        fieldIds.add(PaymentTerms);
        fieldIds.add(InvoiceTerms);
        fieldIds.add(InvoiceAddress);
        fieldIds.add(Warehouse);
        fieldIds.add(Pricelist);

        fieldIds.add(PaymentMethod);

        OBCriteria<Field> fieldList;
        OBCriteria<Language> langList;
        OBContext.setAdminMode(true);

        langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        int count = 0;

        res.append("var HeaderFields = new Array();\n");

        if (!lang.contains("en_")) {
            OBCriteria<FieldTrl> trlList;

            for (String fId : fieldIds) {
                fieldList = OBDal.getInstance().createCriteria(Field.class);
                fieldList.add(Restrictions.eq(Field.PROPERTY_ID, fId));

                trlList = OBDal.getInstance().createCriteria(FieldTrl.class);
                trlList.add(Restrictions.eq(FieldTrl.PROPERTY_FIELD, fieldList.list().get(0)));
                trlList.add(Restrictions.eq(FieldTrl.PROPERTY_LANGUAGE, langList.list().get(0)));

                if (!trlList.list().isEmpty()) {
                    //crea oggetto nuovo campo header tradotto..
                    res.append("HeaderFields[").append(count).append("]= new OrderHeaderField(\"").append(count).append("\",\"").append(trlList.list().get(0).getName()).append("\",\"").append(OrderHeaderEnum.values()[count]).append("\",\"\", \"\");\n");


                } else {
                    //crea oggetto nuovo campo header non tradotto (eng)..

                    res.append("HeaderFields[").append(count).append("]= new OrderHeaderField(\"").append(count).append("\",\"").append(fieldList.list().get(0).getName()).append("\",\"").append(OrderHeaderEnum.values()[count]).append("\",\"\", \"\");\n");

                }

                count++;

            }

        } else {
            count = 0;

            for (String fId : fieldIds) {
                fieldList = OBDal.getInstance().createCriteria(Field.class);
                fieldList.add(Restrictions.eq(Field.PROPERTY_ID, fId));

                //aggiungi campo header non tradotto..

                res.append("HeaderFields[").append(count).append("]= new OrderHeaderField(\"").append(count).append("\",\"").append(fieldList.list().get(0).getName()).append("\",\"").append(OrderHeaderEnum.values()[count]).append("\",\"\", \"\");\n");

                count++;
            }


        }


        OBContext.restorePreviousMode();

        return res.toString();
    }

    // - - - GENERATING ORDER LINE FIELDS - - - //
    private String CreateOrderLineEntries(String lang) {
        StringBuilder res = new StringBuilder();

        //hardcoded field ids
        String Product = "1127";
        String Quantity = "1130";
        String UnitPrice = "1138";

        ArrayList<String> fieldIds = new ArrayList<String>();

        fieldIds.add(Product);
        fieldIds.add(Quantity);
        fieldIds.add(UnitPrice);

        OBCriteria<Field> fieldList;
        OBCriteria<Language> langList;
        OBContext.setAdminMode(true);

        langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        int count = 0;

        res.append("var myOrderLineFields = new Array();\n");

        if (!lang.contains("en_")) {
            OBCriteria<FieldTrl> trlList;

            for (String fId : fieldIds) {

                fieldList = OBDal.getInstance().createCriteria(Field.class);
                fieldList.add(Restrictions.eq(Field.PROPERTY_ID, fId));

                trlList = OBDal.getInstance().createCriteria(FieldTrl.class);
                trlList.add(Restrictions.eq(FieldTrl.PROPERTY_FIELD, fieldList.list().get(0)));
                trlList.add(Restrictions.eq(FieldTrl.PROPERTY_LANGUAGE, langList.list().get(0)));

                if (!trlList.list().isEmpty()) {
                    //crea oggetto nuovo campo orderline tradotto..
                    res.append("myOrderLineFields[").append(count).append("]= new OrderLineField(\"").append(count).append("\",\"").append(trlList.list().get(0).getName()).append("\",\"").append(OrderLineEnum.values()[count]).append("\",\"\", \"\");\n");
                } else {
                    //crea oggetto nuovo campo orderline non tradotto (eng)..
                    res.append("myOrderLineFields[").append(count).append("]= new OrderLineField(\"").append(count).append("\",\"").append(fieldList.list().get(0).getName()).append("\",\"").append(OrderLineEnum.values()[count]).append("\",\"\", \"\");\n");

                }


                count++;
            }

        } else {
            count = 0;

            for (String fId : fieldIds) {
                fieldList = OBDal.getInstance().createCriteria(Field.class);
                fieldList.add(Restrictions.eq(Field.PROPERTY_ID, fId));

                //aggiungi campo header non tradotto..

                res.append("myOrderLineFields[").append(count).append("]= new OrderLineField(\"").append(count).append("\",\"").append(fieldList.list().get(0).getName()).append("\",\"").append(OrderLineEnum.values()[count]).append("\",\"\", \"\");\n");

                count++;
            }


        }

        OBContext.restorePreviousMode();

        return res.toString();
    }
}
