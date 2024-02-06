/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm;

/**
 *
 * @author nicholas
 */
public class OpcrmOrder {
    private String OrderId;
    private String OrderIndex;
    private String BPartnerId;
    private String BPartnerAddressId;
    private String TrxDocId;
    private String OrderDate;
    private String DeliveryDate;
    private String PaymentTermId;
    private String InvoiceTermKey;
    private String InvoiceAddressId;
    private String WarehouseId;
    private String PriceListId;
    private OpcrmOrderline []  OrderLines;
    private String PaymentMethodId;
    private String TotalPaid;
    private String Status;

    public String getBPartnerAddressId() {
        return BPartnerAddressId;
    }

    public void setBPartnerAddressId(String BPartnerAddressId) {
        this.BPartnerAddressId = BPartnerAddressId;
    }

    public String getBPartnerId() {
        return BPartnerId;
    }

    public void setBPartnerId(String BPartnerId) {
        this.BPartnerId = BPartnerId;
    }

    public String getDeliveryDate() {
        return DeliveryDate;
    }

    public void setDeliveryDate(String DeliveryDate) {
        this.DeliveryDate = DeliveryDate;
    }

    public String getInvoiceAddressId() {
        return InvoiceAddressId;
    }

    public void setInvoiceAddressId(String InvoiceAddressId) {
        this.InvoiceAddressId = InvoiceAddressId;
    }

    public String getInvoiceTermKey() {
        return InvoiceTermKey;
    }

    public void setInvoiceTermKey(String InvoiceTermKey) {
        this.InvoiceTermKey = InvoiceTermKey;
    }

    public String getOrderDate() {
        return OrderDate;
    }

    public void setOrderDate(String OrderDate) {
        this.OrderDate = OrderDate;
    }

    public String getOrderIndex() {
        return OrderIndex;
    }

    public void setOrderIndex(String OrderIndex) {
        this.OrderIndex = OrderIndex;
    }

    public OpcrmOrderline [] getOrderLines() {
        return OrderLines;
    }

    public void setOrderLines(OpcrmOrderline [] OrderLines) {
        this.OrderLines = OrderLines;
    }

    public String getPaymentTermId() {
        return PaymentTermId;
    }

    public void setPaymentTermId(String PaymentTermId) {
        this.PaymentTermId = PaymentTermId;
    }

    public String getTrxDocId() {
        return TrxDocId;
    }

    public void setTrxDocId(String TrxDocId) {
        this.TrxDocId = TrxDocId;
    }

    public String getWarehouseId() {
        return WarehouseId;
    }

    public void setWarehouseId(String WarehouseId) {
        this.WarehouseId = WarehouseId;
    }
    
    public String getPriceListId() {
        return PriceListId;
    }
    
    public void setPriceListId(String PriceListId){
        this.PriceListId = PriceListId;
    }
    
    public String getPaymentMethodId(){
        return PaymentMethodId;
    }
    
    public void setPaymentMethodId(String PaymentMethodId){
        this.PaymentMethodId = PaymentMethodId;
    }
    
    public String getTotalPaid(){
        return TotalPaid;
    }
    
    public void setTotalPaid(String TotalPaid){
        this.TotalPaid = TotalPaid;
    }
    
    public String getStatus(){
        return Status;
    }
    
    public void setStatus(String Status){
        this.Status = Status;
    }
    
    public String getOrderId(){
        return this.OrderId;
    }
    
    public void setOrderId(String OrderId){
        this.OrderId = OrderId;
    }
}
