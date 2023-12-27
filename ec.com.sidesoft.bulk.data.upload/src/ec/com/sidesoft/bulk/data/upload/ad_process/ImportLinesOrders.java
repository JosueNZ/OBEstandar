package ec.com.sidesoft.bulk.data.upload.ad_process;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Attribute;
import org.openbravo.model.common.plm.AttributeInstance;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.AttributeUse;
import org.openbravo.model.common.plm.AttributeValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOMTrl;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.onhandquantity.ProductStockView;
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
        new Parameter("Unidad", Parameter.STRING), new Parameter("colorM", Parameter.STRING),
        new Parameter("RMW", Parameter.STRING), new Parameter("Chazis", Parameter.STRING),
        new Parameter("Motor", Parameter.STRING), new Parameter("Importación", Parameter.STRING),
        new Parameter("Color", Parameter.STRING), new Parameter("Talla", Parameter.STRING),
        new Parameter("Serie", Parameter.STRING) };// 7

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
        (String) values[7], (String) values[8], (String) values[9], (String) values[10],
        (String) values[11], (String) values[12], (String) values[13], (String) values[14],
        (String) values[15]);
  }

  public BaseOBObject createOrderline(final String Linea, final String NoPedido,
      final String Producto, final String Cantidad, final String Precio, final String Total,
      final String Impuesto, final String Unidad, final String colorM, final String rmw,
      final String chasis, final String motor, final String importacion, final String color,
      final String talla, final String serie) throws Exception {
    String valueAttribute = "";
    AttributeSetInstance attributeSetInstance = null;
    OrderLine ObjOrderLine = OBProvider.getInstance().get(OrderLine.class);
    // Validar Pedido de venta existente
    Order ObjOrder = findDALInstance(false, Order.class,
        new Value(Order.PROPERTY_DOCUMENTNO, NoPedido),
        new Value(Order.PROPERTY_SALESTRANSACTION, true));
    if (ObjOrder == null || ObjOrder.equals("")) {
      throw new OBException("Pedido con el número de documento: " + NoPedido + " no existe.");
    }

    // Validar Producto Existente
    Product ObjProduct = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, Producto));
    if (ObjProduct == null || ObjProduct.equals("")) {
      throw new OBException("Producto con el identificador: " + Producto + " no existe.");
    }

    // Valida que el atributo del producto corresponda con el proporcionado

    if (ObjProduct.getAttributeSet() != null) {
      OBCriteria<AttributeUse> attributeUseList = OBDal.getInstance()
          .createCriteria(AttributeUse.class);
      attributeUseList
          .add(Restrictions.eq(AttributeUse.PROPERTY_ATTRIBUTESET, ObjProduct.getAttributeSet()));
      if (attributeUseList.count() == 5) {
        if (!colorM.equals("")) {
          Attribute attributeC = findDALInstance(false, Attribute.class,
              new Value(Attribute.PROPERTY_NAME, "COLOR"));

          Attribute attributeI = findDALInstance(false, Attribute.class,
              new Value(Attribute.PROPERTY_NAME, "N° IMPORTACIÓN"));

          AttributeValue attributeValue = findDALInstance(false, AttributeValue.class,
              new Value(AttributeValue.PROPERTY_ATTRIBUTE, attributeC),
              new Value(AttributeValue.PROPERTY_NAME, colorM.trim()));

          AttributeValue attributeValueI = findDALInstance(false, AttributeValue.class,
              new Value(AttributeValue.PROPERTY_ATTRIBUTE, attributeI),
              new Value(AttributeValue.PROPERTY_NAME, importacion.trim()));

          if (attributeValue == null) {
            throw new OBException("El color " + colorM
                + " no forma parte de los colores predefinidos en el sistema verificar la primera columna Color");
          }
          if (attributeValueI == null) {
            throw new OBException("El N° IMPORTACIÓN " + importacion
                + " no forma parte de los valores predefinidos en el sistema verificar la columna N° IMPORTACIÓN");
          }
          if (rmw.equals("") || chasis.equals("") || motor.equals("") || colorM.equals("")
              || importacion.equals("") || !serie.equals("") || !color.equals("")
              || !talla.equals("")) {
            throw new OBException(
                "La informacion del producto no coincide con la proporcionada revisar el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN no este en blanco y que la SERIE,COLOR,TALLA este en blanco");
          } else {
            valueAttribute = colorM.trim() + "_" + chasis.trim() + "_" + motor.trim() + "_"
                + rmw.trim() + "_" + importacion.trim();
            attributeSetInstance = getAttributeSetInstance(ObjProduct, valueAttribute, ObjOrder);
            // setAttributeSetValues(attributeSetInstance, attributeUseList.list(),
            // colorM.trim(),rmw.trim(), chasis.trim(), motor.trim(), importacion.trim());
          }
        } else {
          throw new OBException(
              "La informacion del producto no coincide con la proporcionada revisar el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN no este en blanco y que la SERIE,COLOR,TALLA este en blanco");
        }
      }
      if (attributeUseList.count() == 1) {
        if (!rmw.equals("") || !chasis.equals("") || !motor.equals("") || !colorM.equals("")
            || !importacion.equals("") || serie.equals("") || !color.equals("")
            || !talla.equals("")) {
          throw new OBException(
              "La informacion del producto no coincide con la proporcionada revisar la SERIE no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,COLOR,TALLA esten en blanco");
        } else {
          valueAttribute = serie.trim();
          attributeSetInstance = getAttributeSetInstance(ObjProduct, valueAttribute, ObjOrder);
          // setAttributeSetValues(attributeSetInstance, attributeUseList.list(), serie.trim());
        }
      }
      if (attributeUseList.count() == 2) {
        /********************************** TALLA ********************************************/
        if (!talla.equals("")) {
          Attribute attribute = findDALInstance(false, Attribute.class,
              new Value(Attribute.PROPERTY_NAME, "TALLA"));

          AttributeValue attributeValue = findDALInstance(false, AttributeValue.class,
              new Value(AttributeValue.PROPERTY_ATTRIBUTE, attribute),
              new Value(AttributeValue.PROPERTY_NAME, talla.trim()));

          if (attributeValue == null) {
            throw new OBException("La talla " + talla
                + " no forma parte de los valores predefinidos en el sistema verificar la columna TALLA");
          }
          if (!rmw.equals("") || !chasis.equals("") || !motor.equals("") || !colorM.equals("")
              || !importacion.equals("") || !serie.equals("") || color.equals("")
              || talla.equals("")) {
            throw new OBException(
                "La informacion del producto no coincide con la proporcionada revisar la COLOR,TALLA no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,SERIE esten en blanco");
          }
        } else {
          throw new OBException(
              "La informacion del producto no coincide con la proporcionada revisar la COLOR,TALLA no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,SERIE esten en blanco");
        }
        /********************************** TALLA ********************************************/
        if (!color.equals("")) {
          Attribute attribute = findDALInstance(false, Attribute.class,
              new Value(Attribute.PROPERTY_NAME, "COLOR"));

          AttributeValue attributeValue = findDALInstance(false, AttributeValue.class,
              new Value(AttributeValue.PROPERTY_ATTRIBUTE, attribute),
              new Value(AttributeValue.PROPERTY_NAME, color.trim()));

          if (attributeValue == null) {
            throw new OBException("El color " + color
                + " no forma parte de los colores predefinidos en el sistema verificar la primera columna Color");
          }
          if (!rmw.equals("") || !chasis.equals("") || !motor.equals("") || !colorM.equals("")
              || !importacion.equals("") || !serie.equals("") || color.equals("")
              || talla.equals("")) {
            throw new OBException(
                "La informacion del producto no coincide con la proporcionada revisar la COLOR,TALLA no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,SERIE esten en blanco");
          } else {
            valueAttribute = color.trim() + "_" + talla.trim();
            attributeSetInstance = getAttributeSetInstance(ObjProduct, valueAttribute, ObjOrder);
            // setAttributeSetValues(attributeSetInstance, attributeUseList.list(),
            // color.trim(),talla.trim());
          }
        } else {
          throw new OBException(
              "La informacion del producto no coincide con la proporcionada revisar la COLOR,TALLA no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,SERIE esten en blanco");
        }
      }
    } else {
      if (!rmw.equals("") || !chasis.equals("") || !motor.equals("") || !colorM.equals("")
          || !importacion.equals("") || !serie.equals("") || !color.equals("")
          || !talla.equals("")) {
        throw new OBException(
            "La informacion del producto no coincide con la proporcionada revisar que el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,COLOR,TALLA,SERIE esten en blanco");
      } else {
        valueAttribute = "";
      }
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
      // Precio Estandar
      ObjOrderLine
          .setStandardPrice(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(PrecioTarifa)));
      // Tax Base Amount
      ObjOrderLine.setTaxableAmount(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(PrecioTarifa))
          .multiply(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Cantidad))));
      // Imp. Linea
      ObjOrderLine.setLineNetAmount(Parameter.BIGDECIMAL.parse(changeFormatBigDecimal(Total)));
      // Imp. Linea
      ObjOrderLine.setScheduledDeliveryDate(ObjOrder.getOrderDate());
      // impuesto
      ObjOrderLine.setTax(ObjTaxRate);
      // ObjOrderLinev
      ObjOrderLine.setBusinessPartner(ObjOrder.getBusinessPartner());
      // ObjOrderLinev
      ObjOrderLine.setPartnerAddress(ObjOrder.getPartnerAddress());
      // Unidad de medida
      ObjOrderLine.setUOM(ObjUOM.getUOM());
      // Valor de Atributo
      ObjOrderLine.setAttributeSetValue((valueAttribute.equals("") ? null : attributeSetInstance));
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

  public void setAttributeSetValues(AttributeSetInstance attributeSetInstance,
      List<AttributeUse> attributeUseList, String... value) throws OBException {

    for (AttributeUse attributeUse : attributeUseList) {

      AttributeInstance attributeInstance = OBProvider.getInstance().get(AttributeInstance.class);

      try {
        attributeInstance.setAttributeSetValue(attributeSetInstance);
        attributeInstance.setAttribute(attributeUse.getAttribute());
        // Valida cada unos de los distintos tipo de atributos y casos posibles
        if (attributeUse.getAttribute().getName().equals("RAMV")) {
          attributeInstance.setSearchKey(value[1]);
        }
        if (attributeUse.getAttribute().getName().equals("TALLA")) {
          attributeInstance.setSearchKey(value[1]);
        }
        if (attributeUse.getAttribute().getName().equals("SERIE")) {
          attributeInstance.setSearchKey(value[0]);
        }
        if (attributeUse.getAttribute().getName().equals("CHASIS")) {
          attributeInstance.setSearchKey(value[2]);
        }
        if (attributeUse.getAttribute().getName().equals("MOTOR")) {
          attributeInstance.setSearchKey(value[3]);
        }
        if (attributeUse.getAttribute().getName().equals("COLOR")) {
          attributeInstance.setSearchKey(value[0]);
        }
        if (attributeUse.getAttribute().getName().equals("N° IMPORTACIÓN")) {
          attributeInstance.setSearchKey(value[4]);
        }
        if (attributeUse.getAttribute().isList() == true) {
          if (attributeUse.getAttribute().getName().equals("COLOR")) {
            AttributeValue attributeValue = findDALInstance(false, AttributeValue.class,
                new Value(AttributeValue.PROPERTY_ATTRIBUTE, attributeUse.getAttribute()),
                new Value(AttributeValue.PROPERTY_NAME, value[0]));
            attributeInstance.setAttributeValue(attributeValue);
          }
          if (attributeUse.getAttribute().getName().equals("TALLA")) {
            AttributeValue attributeValue = findDALInstance(false, AttributeValue.class,
                new Value(AttributeValue.PROPERTY_ATTRIBUTE, attributeUse.getAttribute()),
                new Value(AttributeValue.PROPERTY_NAME, value[1]));
            attributeInstance.setAttributeValue(attributeValue);
          }
          if (attributeUse.getAttribute().getName().equals("N° IMPORTACIÓN")) {
            AttributeValue attributeValue = findDALInstance(false, AttributeValue.class,
                new Value(AttributeValue.PROPERTY_ATTRIBUTE, attributeUse.getAttribute()),
                new Value(AttributeValue.PROPERTY_NAME, value[4]));
            attributeInstance.setAttributeValue(attributeValue);
          }
        }
        OBDal.getInstance().save(attributeInstance);
        OBDal.getInstance().flush();

      } catch (Exception e) {
        OBDal.getInstance().rollbackAndClose();
        throw new OBException(Utility.messageBD(conn, e.getMessage(), vars.getLanguage()));
      }
    }
  }

  public AttributeSetInstance getAttributeSetInstance(Product product, String data, Order ObjOrder)
      throws OBException {
    AttributeSetInstance attach = null;

    // Valida si existe una intancia con los mismos atributos
    OBCriteria<AttributeSetInstance> obc = OBDal.getInstance()
        .createCriteria(AttributeSetInstance.class);
    obc.add(Restrictions.eq(AttributeSetInstance.PROPERTY_DESCRIPTION, data));
    obc.add(Restrictions.eq(AttributeSetInstance.PROPERTY_ATTRIBUTESET, product.getAttributeSet()));
    // obc.setMaxResults(1);
    // AttributeSetInstance attach = (AttributeSetInstance) obc.uniqueResult();

    // // Caso contrario crea una nueva
    for (AttributeSetInstance attributeset : obc.list()) {
      OBCriteria<ProductStockView> obcWarehouse = OBDal.getInstance()
          .createCriteria(ProductStockView.class);
      obcWarehouse.add(Restrictions.eq(ProductStockView.PROPERTY_ATTRIBUTESETVALUE, attributeset));
      obcWarehouse.add(Restrictions.eq(ProductStockView.PROPERTY_PRODUCT, product));
      obcWarehouse
          .add(Restrictions.eq(ProductStockView.PROPERTY_WAREHOUSE, ObjOrder.getWarehouse()));
      obcWarehouse.add(Restrictions.eq(ProductStockView.PROPERTY_STOCKED, true));
      obcWarehouse.setMaxResults(1);
      ProductStockView objProductStockView = (ProductStockView) obcWarehouse.uniqueResult();
      if (objProductStockView != null) {
        attach = objProductStockView.getAttributeSetValue();
        break;
      }
    }

    if (attach == null) {
      throw new OBException("No se encontró en stock producto con el atributo:" + data);
    }
    return attach;
  }

  public String changeFormatBigDecimal(String numbers) {
    String Remplace = "0";

    Remplace = numbers.replace(".", "");
    Remplace = numbers.replace(",", ".");

    return Remplace;
  }

}
