/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.sidesoft.localization.ecuador.invoices.ad_process;

import java.sql.CallableStatement;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.module.idljava.proc.IdlServiceJava;
import org.openbravo.service.db.DalConnectionProvider;

import com.sidesoft.localization.ecuador.withholdings.SSWHCodelivelihoodt;
import com.sidesoft.localization.ecuador.withholdings.SSWHLivelihoodt;

/**
 * 
 * @author Rodney
 */
public class ImportProvideInvoice extends IdlServiceJava {
  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Organizacion", Parameter.STRING), // 0
        new Parameter("Tipo Documento", Parameter.STRING), // 1
        new Parameter("Numero documento", Parameter.STRING), // 2
        new Parameter("Fecha factura", Parameter.STRING), // 3
        new Parameter("Tercero", Parameter.STRING), // 4
        new Parameter("Tarifa", Parameter.STRING), // 5
        new Parameter("Condiciones de pago", Parameter.STRING), // 6
        new Parameter("Metodo de pago", Parameter.STRING), // 7
        new Parameter("Numero de factura", Parameter.STRING), // 8
        new Parameter("Autorizacion", Parameter.STRING), // 9
        new Parameter("Fecha caducidad", Parameter.STRING), // 10
        new Parameter("Tipo de comprobante", Parameter.STRING), // 11
        new Parameter("Tipo sustento", Parameter.STRING), // 12
        new Parameter("Factura Electronica", Parameter.STRING), // 13
        new Parameter("Producto", Parameter.STRING), // 14
        new Parameter("Cantidad", Parameter.STRING), // 15
        new Parameter("Precio unitario", Parameter.STRING), // 16
        new Parameter("Importe linea", Parameter.STRING), // 17
        new Parameter("Impuesto", Parameter.STRING) }; // 18
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 32);
    validator.checkString(values[1], 500);
    validator.checkString(values[2], 30);
    validator.checkDate(values[3]);
    validator.checkString(values[4], 32);
    validator.checkString(values[5], 32);
    validator.checkString(values[6], 32);
    validator.checkString(values[7], 32);
    validator.checkString(values[8], 20);
    validator.checkString(values[9], 255);
    validator.checkDate(values[10]);
    validator.checkString(values[11], 32);
    validator.checkString(values[12], 32);
    validator.checkString(values[13], 4);
    validator.checkString(values[14], 500);
    validator.checkString(values[15], 10);
    validator.checkString(values[16], 10);
    validator.checkString(values[17], 10);
    validator.checkString(values[18], 32);

    return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createphysicalinventory((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6],
        (String) values[7], (String) values[8], (String) values[9], (String) values[10],
        (String) values[11], (String) values[12], (String) values[13], (String) values[14],
        (String) values[15], (String) values[16], (String) values[17], (String) values[18]);
  }

  public BaseOBObject createphysicalinventory(final String Organization_id, // 0
      final String DocType, // 1
      final String NumDoc, // 2
      final String InvoiceDate, // 3
      final String Partner, // 4
      final String Rate, // 5
      final String PaymentConditions, // 6
      final String PaymentMethod, // 7
      final String InvoiceNum, // 8
      final String Autorization, // 9
      final String Expirydate, // 10
      final String VoucherType, // 11
      final String SupportType, // 12
      final String InvoiceElectronic, // 13
      final String Product_Id, // 14
      final String Quantify, // 15
      final String UnitPrice, // 16
      final String ImportLine, // 17
      final String Tax // 18
  ) throws Exception {

    OBCriteria<Organization> ObjOrga = OBDal.getInstance().createCriteria(Organization.class);
    ObjOrga.add(Restrictions.eq("name", Organization_id));

    // Valida tipo de docuemto por el nombre
    DocumentType DocTypeId = findDALInstance(false, DocumentType.class,
        new Value(DocumentType.PROPERTY_NAME, DocType));
    if (DocTypeId == null || DocTypeId.equals("")) {
      throw new OBException(
          "Error, Tipo de documento  " + DocType + " no existe como nombre en la tabla c_doctype");
    }
    // Valida El partner con el numero de cedula o RUC
    BusinessPartner PartnerId = findDALInstance(false, BusinessPartner.class,
        new Value(BusinessPartner.PROPERTY_TAXID, Partner));
    if (PartnerId == null || PartnerId.equals("")) {
      throw new OBException(
          "Error, el ruc" + Partner + " no existe como CIF/NIF  en c_bpartner, ventana  Tercero");
    }
    // Valida la tarifa por el nombre
    PriceList RateId = findDALInstance(false, PriceList.class,
        new Value(PriceList.PROPERTY_NAME, Rate));
    if (RateId == null || RateId.equals("")) {
      throw new OBException("Error, La tarifa Tarifa con el nombre  " + Rate + " no existe");
    }
    // Valida las condiciones de pago por el nombre
    PaymentTerm PaymentConditionsId = findDALInstance(false, PaymentTerm.class,
        new Value(PaymentTerm.PROPERTY_NAME, PaymentConditions));
    if (PaymentConditionsId == null || PaymentConditionsId.equals("")) {
      throw new OBException(
          "Error, Condiciones de pago con el nombre " + PaymentConditions + " no existe");
    }
    // Valida el Metodo de pago por el nombre
    FIN_PaymentMethod PaymentMethodId = findDALInstance(false, FIN_PaymentMethod.class,
        new Value(FIN_PaymentMethod.PROPERTY_NAME, PaymentMethod));
    if (PaymentMethodId == null || PaymentMethodId.equals("")) {
      throw new OBException("Error, Metodo de pago con el nombre " + PaymentMethod + " no existe");
    }
    // Valida el Tipo de comprobante por el identificador
    SSWHLivelihoodt VoucherTypeId = findDALInstance(false, SSWHLivelihoodt.class,
        new Value(SSWHLivelihoodt.PROPERTY_SEARCHKEY, VoucherType));
    if (VoucherTypeId == null || VoucherTypeId.equals("")) {
      throw new OBException(
          "Error, Tipo de comprobante con el identificador " + VoucherType + " no existe");
    }
    // Valida el tipo de sustento por el identificador
    SSWHCodelivelihoodt SupportTypeId = findDALInstance(false, SSWHCodelivelihoodt.class,
        new Value(SSWHCodelivelihoodt.PROPERTY_SEARCHKEY, SupportType));
    if (SupportTypeId == null || SupportTypeId.equals("")) {
      throw new OBException(
          "Error,Tipo de sustento con el identificador  " + SupportType + " no existe");
    }
    /*
     * validaciones para lineas
     */
    // Valida el producto por el nombre
    Product ProductId = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_NAME, Product_Id));
    if (ProductId == null || ProductId.equals("")) {
      throw new OBException("Error, El producto con el nombre " + Product_Id + " no existe");
    }
    // Valida la localizacion del tercero
    Location LocationId = findDALInstance(false, Location.class,
        new Value(Location.PROPERTY_BUSINESSPARTNER, PartnerId),
        new Value(Location.PROPERTY_INVOICETOADDRESS, true),
        new Value(Location.PROPERTY_ACTIVE, true));
    if (LocationId == null || LocationId.equals("")) {
      throw new OBException(
          "Error, No existe una dirreccion asociada a este al Tercero con este identificador: "
              + PartnerId);
    }

    // Valida el impuesto por el número de indice
    TaxRate TaxId = findDALInstance(false, TaxRate.class,
        new Value(TaxRate.PROPERTY_RATE, Parameter.BIGDECIMAL.parse(Tax)));
    if (TaxId == null || TaxId.equals("")) {
      throw new OBException("Error, el impuesto con el indice " + Tax + " no existe");
    }
    // Captura la unidad del producto ya presente.
    String umons = PartnerId.getCurrency().getId().toString();
    // Estado del registro en PROCESADO
	// String DocStatus = "SSWH_PR";

    Invoice invoice = OBProvider.getInstance().get(Invoice.class);

    Currency currencyid = findDALInstance(false, Currency.class,
        new Value(Currency.PROPERTY_ID, PartnerId.getCurrency().getId().toString()));

    try {

      invoice.setOrganization(rowOrganization);
      invoice.setTransactionDocument(DocTypeId);
      invoice.setDocumentType(DocTypeId);
      invoice.setDocumentNo(NumDoc);
      invoice.setInvoiceDate(Parameter.DATE.parse(InvoiceDate));
      invoice.setAccountingDate(Parameter.DATE.parse(InvoiceDate));
      invoice.setBusinessPartner(PartnerId);
      invoice.setPartnerAddress(LocationId);
      invoice.setPriceList(RateId);
      invoice.setCurrency(currencyid);
      invoice.setPaymentTerms(PaymentConditionsId);
      invoice.setPaymentMethod(PaymentMethodId);
      invoice.setOrderReference(InvoiceNum);
      invoice.setSswhNroauthorization(Autorization);
      invoice.setSalesTransaction(false);
      invoice.setSswhExpirationdate(Parameter.DATE.parse(Expirydate));
      invoice.setSswhLivelihood(VoucherTypeId);
      invoice.setSswhCodelivelihood(SupportTypeId);
      invoice.setSswhIseinvoice(InvoiceElectronic.equals("true")?true:false);
      // invoice.setDocumentStatus(DocStatus);
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }

    Invoice Invoice_line = findDALInstance(false, Invoice.class,
        new Value(Invoice.PROPERTY_DOCUMENTNO, NumDoc));
    if (Invoice_line == null || Invoice_line.equals("")) {
      throw new OBException("Hueco: " + NumDoc + " no existe");
    }

    InvoiceLine newInvoiceLine = OBProvider.getInstance().get(InvoiceLine.class);

    try {

      newInvoiceLine.setProduct(ProductId);
      newInvoiceLine.setInvoice(Invoice_line);
      newInvoiceLine.setLineNo((long) 10);
      newInvoiceLine.setInvoicedQuantity(Parameter.BIGDECIMAL.parse(Quantify));
      newInvoiceLine.setUOM(ProductId.getUOM());
      newInvoiceLine.setUnitPrice(Parameter.BIGDECIMAL.parse(UnitPrice));
      newInvoiceLine.setLineNetAmount(Parameter.BIGDECIMAL.parse(ImportLine));
      newInvoiceLine.setTax(TaxId);
      OBDal.getInstance().save(newInvoiceLine);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
      CallableStatement cs = cp.getConnection().prepareCall("{call C_Invoice_Post (null,?)}");

      // client
      cs.setString(1, Invoice_line.getId());

      cs.execute();
      cs.close();
    } catch (Exception e) {
      throw new OBException(e.getMessage(), e);
    }

    OBDal.getInstance().commitAndClose();
    return invoice;
  }
}
