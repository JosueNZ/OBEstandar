package ec.com.sidesoft.purchase.inout.data.ad_process;

import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.*;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.module.idljava.proc.IdlServiceJava;

public class ImportLinesOrders extends IdlServiceJava {

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Linea", Parameter.STRING), // 0
        new Parameter("No. Pedido", Parameter.STRING), // 1
        new Parameter("Producto", Parameter.STRING), // 2
        new Parameter("Cantidad", Parameter.STRING), // 3
        new Parameter("Precio", Parameter.STRING), // 4
        new Parameter("Total", Parameter.STRING), // 5
        new Parameter("Impuesto", Parameter.STRING), // 6
        new Parameter("Unidad", Parameter.STRING) };// 7

  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 20);
    validator.checkString(values[1], 60);
    validator.checkString(values[2], 60);
    validator.checkString(values[3], 17);
    validator.checkString(values[4], 17);
    validator.checkString(values[5], 17);
    validator.checkString(values[6], 60);
    validator.checkString(values[7], 60);

    return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createOrderline((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6],
        (String) values[7]);
  }

  public BaseOBObject createOrderline(final String Linea, final String NoPedido,
      final String Producto, final String Cantidad, final String Precio, final String Total,
      final String Impuesto, final String Unidad) throws Exception {

    OrderLine ObjOrderLine = OBProvider.getInstance().get(OrderLine.class);
    // Validar Pedido existente
    Order ObjOrder = findDALInstance(false, Order.class,
        new Value(Order.PROPERTY_DOCUMENTNO, NoPedido),
        new Value(Order.PROPERTY_SALESTRANSACTION, false));
    if (ObjOrder == null || ObjOrder.equals("")) {
      throw new OBException("Pedido con el número de documento: " + NoPedido + " no existe.");
    }

    // Validar Producto Existente
    Product ObjProduct = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, Producto));
    if (ObjProduct == null || ObjProduct.equals("")) {
      throw new OBException("Producto con el identificador: " + Producto + " no existe.");
    }

    // Validar Impuesto Existente
    TaxRate ObjTaxRate = findDALInstance(false, TaxRate.class,
        new Value(TaxRate.PROPERTY_NAME, Impuesto));
    if (ObjTaxRate == null || ObjTaxRate.equals("")) {
      throw new OBException("Impuesto con el nombre: " + Impuesto + " no existe.");
    }
    PriceList ObjPriceList = ObjOrder.getPriceList();

    // Validar Unidad de medidad Existente
    UOMTrl ObjUOM = findDALInstance(false, UOMTrl.class, new Value(UOMTrl.PROPERTY_NAME, Unidad));
    if (ObjUOM == null || ObjUOM.equals("")) {
      throw new OBException("Unidad de medida con el nombre: " + Unidad + " no existe.");
    }

    // Buscar precio tarifa
    OBCriteria<PriceListVersion> ObjsPriceListVersion = OBDal.getInstance()
        .createCriteria(PriceListVersion.class);
    ObjsPriceListVersion.add(Restrictions.eq(PriceListVersion.PROPERTY_PRICELIST, ObjPriceList));
    ObjsPriceListVersion.addOrderBy(PriceListVersion.PROPERTY_VALIDFROMDATE, false);
    ObjsPriceListVersion.setFilterOnReadableOrganization(false);
    ObjsPriceListVersion.setMaxResults(1);

    PriceListVersion priceVersion = (PriceListVersion) ObjsPriceListVersion.uniqueResult();

    String PrecioTarifa = Precio;

    if (priceVersion != null) {
      OBCriteria<ProductPrice> ObjsProductPrice = OBDal.getInstance()
          .createCriteria(ProductPrice.class);
      ObjsProductPrice.add(Restrictions.eq(ProductPrice.PROPERTY_PRICELISTVERSION, priceVersion));
      ObjsProductPrice.add(Restrictions.eq(ProductPrice.PROPERTY_PRODUCT, ObjProduct));

      if (ObjsProductPrice.list().size() > 0) {
        for (ProductPrice prodPriceList : ObjsProductPrice.list()) {
          if (prodPriceList.getListPrice().compareTo(BigDecimal.ZERO) != 0) {
            PrecioTarifa = prodPriceList.getListPrice().toString();
          }
        }
      }
    }
    try {
      // Setear organización por defecto
      ObjOrderLine.setOrganization(ObjOrder.getOrganization());
      // Pedido padre
      ObjOrderLine.setSalesOrder(ObjOrder);
      // fecha
      ObjOrderLine.setOrderDate(new Date());
      // moneda del tercero
      ObjOrderLine.setCurrency(ObjOrder.getCurrency());
      // Almacen
      ObjOrderLine.setWarehouse(ObjOrder.getWarehouse());
      // Linea
      ObjOrderLine.setLineNo(new Long(Linea));
      // Producto
      ObjOrderLine.setProduct(ObjProduct);
      // Cantidad
      ObjOrderLine.setOrderedQuantity(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Cantidad)));
      // Precio
      ObjOrderLine.setUnitPrice(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Precio)));
      // Precio Tarifa
      ObjOrderLine.setListPrice(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(PrecioTarifa)));
      // Imp. Linea
      ObjOrderLine.setLineNetAmount(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Total)));
      // impuesto
      ObjOrderLine.setTax(ObjTaxRate);
      // Unidad de medida
      ObjOrderLine.setUOM(ObjUOM.getUOM());
      // Precio Base Neto Unitario
      ObjOrderLine
          .setStandardPrice(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(PrecioTarifa)));
      // taxbaseAmount base Imponible
      ObjOrderLine.setTaxableAmount(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Total)));
      /*
       * // Total
       * ObjOrderLine.setUnitPrice(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Precio))); //
       * TipoTarjeta ObjOrderLine.setCardType(StrTipoTarjeta); // Lote
       * ObjOrderLine.setLotName(Lote); // Fecha
       * ObjOrderLine.setDateTransaction((Parameter.DATE.parse(Fecha))); // ValorCobrado
       */
      OBDal.getInstance().save(ObjOrderLine);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      e.printStackTrace();
    }

    OBDal.getInstance().commitAndClose();
    return ObjOrderLine;
  }

  public String changeFormatBigDecimal(String numbers) {
    String Remplace = "0";

    Remplace = numbers.replace(".", "");
    Remplace = numbers.replace(",", ".");

    return Remplace;
  }

}
