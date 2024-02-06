/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm;

/**
 *
 * @author nicholas
 */
public class OpcrmOrderline {

    private String OrderLineIndex;
    private String ProductId;
    private String ProductName;
    private String Quantity;
    private String UnitPrice;
    private String NetPrice;
    private String ListPrice;
    private String Discount;
    private String department;
    private String ReturnItem;
    private String DiscountAmt;
    
    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getOrderLineIndex() {
        return OrderLineIndex;
    }

    public void setOrderLineIndex(String OrderLineIndex) {
        this.OrderLineIndex = OrderLineIndex;
    }

    public String getProductId() {
        return ProductId;
    }

    public void setProductId(String ProductId) {
        this.ProductId = ProductId;
    }

    public String getProductName() {
        return ProductName;
    }

    public void setProductName(String ProductName) {
        this.ProductName = ProductName;
    }

    public String getQuantity() {
        return Quantity;
    }

    public void setQuantity(String Quantity) {
        this.Quantity = Quantity;
    }

    public String getUnitPrice() {
        return UnitPrice;
    }

    public String getListPrice() {
        return ListPrice;
    }

    public String getDiscount() {
        return Discount;
    }
 
    public String getDiscountAmt(){
        return DiscountAmt;
    }
    
    public void setUnitPrice(String UnitPrice) {
        this.UnitPrice = UnitPrice;
    }

    public String getNetPrice() {
        return NetPrice;
    }
    
    public String getReturnItem(){
        return this.ReturnItem;
    }

    public void setNetPrice(String NetPrice) {
        this.NetPrice = NetPrice;
    }

    public void setListPrice(String ListPrice) {
        this.ListPrice = ListPrice;
    }

    public void setDiscount(String discount) {
        this.Discount = discount;
    }
    
    public void setReturnItem(String ReturnItem){
        this.ReturnItem = ReturnItem;
    }
    
    public void setDiscountAmt(String DiscountAmt){
        this.DiscountAmt = DiscountAmt;
    }
    
    
}
