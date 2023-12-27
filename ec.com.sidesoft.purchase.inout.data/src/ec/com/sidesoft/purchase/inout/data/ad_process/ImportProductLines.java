package ec.com.sidesoft.purchase.inout.data.ad_process;

import java.math.BigDecimal;
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
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Attribute;
import org.openbravo.model.common.plm.AttributeInstance;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.AttributeUse;
import org.openbravo.model.common.plm.AttributeValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.module.idljava.proc.IdlServiceJava;

public class ImportProductLines extends IdlServiceJava {

  int fila = 1;

  @Override
  public String getEntityName() {
    return "ProductLines";
  }

  @Override
  public Parameter[] getParameters() {
    fila = 1;
    return new Parameter[] { new Parameter("Documento Albaran", Parameter.STRING),
        new Parameter("Documento Pedido", Parameter.STRING),
        new Parameter("Ubicacion", Parameter.STRING), new Parameter("Cantidad", Parameter.STRING),
        new Parameter("Codigo", Parameter.STRING), new Parameter("RMW", Parameter.STRING),
        new Parameter("Chazis", Parameter.STRING), new Parameter("Motor", Parameter.STRING),
        new Parameter("Color", Parameter.STRING), new Parameter("Importación", Parameter.STRING),
        new Parameter("Color", Parameter.STRING), new Parameter("Talla", Parameter.STRING),
        new Parameter("Serie", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    /******************** VALIDACIÓN PARA DOCUMENTO ALBARAN ************************************/
    ShipmentInOut shipmentInOut = null;
    validator.checkString(values[0], 200, "Documento Albaran");// Documento Albaran
    if (values[0] != null || !values[0].equals("")) {
      OBCriteria<ShipmentInOut> shipmentInOutList = OBDal.getInstance()
          .createCriteria(ShipmentInOut.class);
      shipmentInOutList.add(Restrictions.eq(ShipmentInOut.PROPERTY_DOCUMENTNO, values[0]));
      if (shipmentInOutList.count() != 0) {
        shipmentInOut = shipmentInOutList.list().get(0);
      } else {
        System.out.println("Documento albaran no encontrado: " + values[0] + " de la Fila " + fila);
        validator.checkNotNull(null, "Documento Albaran");
      }
    }
    /******************** VALIDACIÓN PARA DOCUMENTO ALBARAN ************************************/

    /********************
     * VALIDACIÓN PARA DOCUMENTO PEDIDO COMPRA
     ************************************/
    validator.checkString(values[1], 200, "Documento Pedido");// Documento Pedido
    Order order = null;
    if (values[1] != null || !values[1].equals("")) {
      OBCriteria<Order> OrderList = OBDal.getInstance().createCriteria(Order.class);
      OrderList.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, values[1]));
      OrderList.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, false));

      if (OrderList.count() != 0 && OrderList.count() < 2) {
        order = OrderList.list().get(0);
      } else if (OrderList.count() > 1) {
        System.out.println("Hay mas de un Pedido de compra encontrado con el No. : " + values[1]
            + " de la Fila " + fila);
        validator.checkNotNull(null, "Documento Pedido");
      } else {
        System.out.println(
            "Pedido de compra no encontrado con el No. : " + values[1] + " de la Fila " + fila);
        validator.checkNotNull(null, "Documento Pedido");
      }
    }
    /********************
     * VALIDACIÓN PARA DOCUMENTO PEDIDO COMPRA
     ************************************/

    /******************** VALIDACIÓN UBICACION ************************************/
    validator.checkString(values[2], 200, "Ubicacion");// Ubicacion
    Locator locator = null;
    if (values[2] != null || !values[2].equals("")) {
      OBCriteria<Locator> locatorList = OBDal.getInstance().createCriteria(Locator.class);
      locatorList.add(Restrictions.eq(Locator.PROPERTY_SEARCHKEY, values[2]));
      if (locatorList.count() != 0) {
        locator = locatorList.list().get(0);
      } else {
        System.out.println("Ubicacion no encontrada: " + values[2] + " de la Fila " + fila);
        validator.checkNotNull(null, "Ubicacion");
      }
    }
    /******************** VALIDACIÓN UBICACION ************************************/

    /******************** VALIDACIÓN HUECO ************************************/
    if (shipmentInOut != null && locator != null) {
      if (!shipmentInOut.getOrganization().getId().equals(locator.getOrganization().getId())) {
        System.out.println(
            "No se puede registrar lineas con Huecos de otra Organizacion revisar la Ubicacion de la Fila "
                + fila);
        validator.checkNotNull(null, "Ubicacion");
      }
    }
    /******************** VALIDACIÓN HUECO ************************************/

    validator.checkBigDecimal(values[3]);// Cantidad

    /******************** VALIDACIÓN Producto ************************************/
    validator.checkString(values[4], 200, "Codigo");// Codigo
    Product product = null;
    if (values[4] != null || !values[4].equals("")) {
      OBCriteria<Product> productLIst = OBDal.getInstance().createCriteria(Product.class);
      productLIst.add(Restrictions.eq(Product.PROPERTY_SEARCHKEY, values[4]));
      if (productLIst.count() != 0) {
        product = productLIst.list().get(0);
        // Valida que el atributo del producto corresponda con el proporcionado
        if (product != null) {
          if (product.getAttributeSet() != null) {
            OBCriteria<AttributeUse> attributeUseList = OBDal.getInstance()
                .createCriteria(AttributeUse.class);
            attributeUseList.add(
                Restrictions.eq(AttributeUse.PROPERTY_ATTRIBUTESET, product.getAttributeSet()));
            // Validacion para Atributo RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN
            if (attributeUseList.count() == 5) {
              if (!values[5].equals("")) {
                Attribute attribute = findDALInstance(false, Attribute.class,
                    new Value(Attribute.PROPERTY_NAME, "COLOR"));

                AttributeValue attributeValue = findDALInstance(false, AttributeValue.class,
                    new Value(AttributeValue.PROPERTY_ATTRIBUTE, attribute),
                    new Value(AttributeValue.PROPERTY_NAME, values[5]));

                Attribute attributeI = findDALInstance(false, Attribute.class,
                    new Value(Attribute.PROPERTY_NAME, "N° IMPORTACIÓN"));

                AttributeValue attributeValueI = findDALInstance(false, AttributeValue.class,
                    new Value(AttributeValue.PROPERTY_ATTRIBUTE, attributeI),
                    new Value(AttributeValue.PROPERTY_NAME, values[9]));

                if (attributeValue == null) {
                  System.out.println("El color " + values[5]
                      + " no forma parte de los colores predifinidos en el sistema verificar la primera columna Color de la fila "
                      + fila);
                  validator.checkNotNull(null, "Color");
                }

                if (attributeValueI == null) {
                  System.out.println("El N° IMPORTACIÓN " + values[9]
                      + " no forma parte de los valores predifinidos en el sistema verificar la columna Importación de la fila "
                      + fila);
                  validator.checkNotNull(null, "Importación");
                }

                if (values[5].equals("") || values[6].equals("") || values[7].equals("")
                    || values[8].equals("") || values[9].equals("") || !values[10].equals("")
                    || !values[11].equals("") || !values[12].equals("")) {
                  System.out.println(
                      "La informacion del producto no coincide con la proporcionada revisar el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN no este en blanco y que la COLOR,TALLA,SERIE este en blanco en la Fila "
                          + fila);
                  validator.checkNotNull(null, "Codigo");
                }
              } else {
                System.out.println(
                    "La informacion del producto no coincide con la proporcionada revisar el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN no este en blanco y que la COLOR,TALLA,SERIE este en blanco en la Fila "
                        + fila);
                validator.checkNotNull(null, "Color");
              }
            }
            // Validacion para Atributo Serie
            if (attributeUseList.count() == 1) {
              if (!values[5].equals("") || !values[6].equals("") || !values[7].equals("")
                  || !values[8].equals("") || !values[9].equals("") || !values[10].equals("")
                  || !values[11].equals("") || values[12].equals("")) {
                System.out.println(
                    "La informacion del producto no coincide con la proporcionada revisar la SERIE no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,COLOR,TALLA esten en blanco en la Fila "
                        + fila);
                validator.checkNotNull(null, "Codigo");
              }
            }
            // Validacion para Atributo Color,Talla
            if (attributeUseList.count() == 2) {
              if (!values[10].equals("")) {
                Attribute attribute = findDALInstance(false, Attribute.class,
                    new Value(Attribute.PROPERTY_NAME, "COLOR"));

                AttributeValue attributeValue = findDALInstance(false, AttributeValue.class,
                    new Value(AttributeValue.PROPERTY_ATTRIBUTE, attribute),
                    new Value(AttributeValue.PROPERTY_NAME, values[10]));

                if (attributeValue == null) {
                  System.out.println("El color " + values[10]
                      + " no forma parte de los colores predefinidos en el sistema verificar la segunda columna Color de la fila "
                      + fila);
                  validator.checkNotNull(null, "Color");
                }
                if (!values[5].equals("") || !values[6].equals("") || !values[7].equals("")
                    || !values[8].equals("") || !values[9].equals("") || values[10].equals("")
                    || values[11].equals("") || !values[12].equals("")) {
                  System.out.println(
                      "La informacion del producto no coincide con la proporcionada revisar que COLOR,TALLA no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,SERIE esten en blanco en la Fila"
                          + fila);
                  validator.checkNotNull(null, "Codigo");
                }
              } else {
                System.out.println(
                    "La informacion del producto no coincide con la proporcionada revisar que COLOR,TALLA no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,SERIE esten en blanco en la Fila"
                        + fila);
                validator.checkNotNull(null, "Color");
              }
              /*************************************************TALLA************************************************************ */
              if (!values[11].equals("")) {
                Attribute attribute = findDALInstance(false, Attribute.class,
                    new Value(Attribute.PROPERTY_NAME, "TALLA"));

                AttributeValue attributeValue = findDALInstance(false, AttributeValue.class,
                    new Value(AttributeValue.PROPERTY_ATTRIBUTE, attribute),
                    new Value(AttributeValue.PROPERTY_NAME, values[11]));

                if (attributeValue == null) {
                  System.out.println("La talla " + values[11]
                      + " no forma parte de los valores predefinidos en el sistema verificar la columna Talla de la fila "
                      + fila);
                  validator.checkNotNull(null, "Talla");
                }
                if (!values[5].equals("") || !values[6].equals("") || !values[7].equals("")
                    || !values[8].equals("") || !values[9].equals("") || values[10].equals("")
                    || values[11].equals("") || !values[12].equals("")) {
                  System.out.println(
                      "La informacion del producto no coincide con la proporcionada revisar que COLOR,TALLA no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,SERIE esten en blanco en la Fila"
                          + fila);
                  validator.checkNotNull(null, "Codigo");
                }
              } else {
                System.out.println(
                    "La informacion del producto no coincide con la proporcionada revisar que COLOR,TALLA no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,SERIE esten en blanco en la Fila"
                        + fila);
                validator.checkNotNull(null, "Talla");
              }
            /*************************************************TALLA************************************************************ */
            }
          }
          // Validacion para productos sin Atributo asignado
          if (product.getAttributeSet() == null) {
            if (!values[5].equals("") || !values[6].equals("") || !values[7].equals("")
                || !values[8].equals("") || !values[9].equals("") || !values[10].equals("")
                || !values[11].equals("") || !values[12].equals("")) {
              System.out.println(
                  "La informacion del producto no coincide con la proporcionada revisar que el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,COLOR,TALLA,SERIE esten en blanco de la Fila "
                      + fila);
              validator.checkNotNull(null, "Codigo");
            }
          }
        }
      } else {
        System.out
            .println("Producto con el codigo no encontrado: " + values[4] + " de la Fila " + fila);
        validator.checkNotNull(null, "Codigo");
      }
    }
    /******************** VALIDACIÓN Producto ************************************/

    /*********** VALIDACIÓN Linea Existente y Lineas con el mismo producto ********/
    if (order != null && product != null) {
      OBCriteria<OrderLine> orderLineList = OBDal.getInstance().createCriteria(OrderLine.class);
      orderLineList.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, order));
      // orderLineList.add(Restrictions.eq(OrderLine.PROPERTY_ORDEREDQUANTITY,Parameter.BIGDECIMAL.parse(values[3])));
      orderLineList.add(Restrictions.eq(OrderLine.PROPERTY_PRODUCT, product));
      if (orderLineList.count() == 0) {
        System.out.println(
            "El pedido de compra no tiene ninguna linea que cumpla con estas caracteristicas cod. producto: "
                + values[4] + " en la Fila " + fila);
        validator.checkNotNull(null, "Codigo");
      } else if (orderLineList.count() > 1) {
        System.out.println(
            "El pedido de compra tiene mas de una linea que cumple con estas caracteristicas cod. producto: "
                + values[4] + " en la Fila " + fila);
        validator.checkNotNull(null, "Codigo");
      } else {
      }
    }
    /*********** VALIDACIÓN Linea Existente y Lineas con el mismo producto ********/
    validator.checkString(values[5], 200, "Color");// Color
    validator.checkString(values[6], 200, "RMW");// RMW
    validator.checkString(values[7], 200, "Chazis");// Chazis
    validator.checkString(values[8], 200, "Motor");// Motor
    validator.checkString(values[9], 200, "Importación");// Importacion
    validator.checkString(values[10], 200, "Color");// Color
    validator.checkString(values[11], 200, "Talla");// Talla
    validator.checkString(values[12], 200, "Serie");// Serie
    fila++;
    return values;
  }

  @Override
  public BaseOBObject internalProcess(Object... values) throws Exception {
    return create((String) values[0], (String) values[1], (String) values[2], (String) values[3],
        (String) values[4], (String) values[5], (String) values[6], (String) values[7],
        (String) values[8], (String) values[9], (String) values[10], (String) values[11],
        (String) values[12]);
  }

  public BaseOBObject create(final String albaran, final String pedido, final String ubicacion,
      final String cantidad, final String codigo, final String colorM,final String rmw, final String chasis,
      final String motor,final String importacion, final String color,
      final String talla, final String serie) throws Exception {

    String valueAttribute = "";
    OrderLine orderLinenew = null;
    AttributeSetInstance attributeSetInstance = null;
    Order order = null;

    // Recuperar el ALbaran
    ShipmentInOut shipmentInOut = findDALInstance(false, ShipmentInOut.class,
        new Value(ShipmentInOut.PROPERTY_DOCUMENTNO, albaran));
    if (shipmentInOut == null || shipmentInOut.equals("")) {
      throw new OBException("Albaran con el código :  " + albaran + " no existe");
    }

    // Recuperar el pedido de compra
    OBCriteria<Order> OrderList = OBDal.getInstance().createCriteria(Order.class);
    OrderList.add(Restrictions.eq(Order.PROPERTY_DOCUMENTNO, pedido));
    OrderList.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, false));
    if (OrderList.count() != 0 && OrderList.count() < 2) {
      order = OrderList.list().get(0);
    } else if (OrderList.count() > 1) {
      throw new OBException("Hay mas de un pedido de compra con el No. :  " + pedido);
    } else {
      order = null;
      throw new OBException("El pedido de compra con el No. :  " + pedido + " no existe");
    }

    // Recuperar Ubicacion con su identificador
    Locator locator = findDALInstance(false, Locator.class,
        new Value(Locator.PROPERTY_SEARCHKEY, ubicacion));
    if (locator == null || locator.equals("")) {
      throw new OBException("La ubicacion con el nombre :  " + ubicacion + " no existe");
    }
    if (!shipmentInOut.getOrganization().getId().equals(locator.getOrganization().getId())) {
      throw new OBException("No se puede registrar lineas con Huecos de otra Organizacion");
    }

    // Recuperar Producto con su identificador
    Product product = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, codigo));
    if (product == null || product.equals("")) {
      throw new OBException("Producto con el código :  " + codigo + " no existe");
    }

    // Valida que el atributo del producto corresponda con el proporcionado
    if (product != null) {
      if (product.getAttributeSet() != null) {
        OBCriteria<AttributeUse> attributeUseList = OBDal.getInstance()
            .createCriteria(AttributeUse.class);
        attributeUseList
            .add(Restrictions.eq(AttributeUse.PROPERTY_ATTRIBUTESET, product.getAttributeSet()));
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
              valueAttribute = colorM.trim() + "_" +chasis.trim() + "_" + motor.trim() + "_" + rmw.trim() + "_"
                  + importacion.trim();
              // if(validateAttributeSetInstance(product,valueAttribute)){
              // attributeSetInstance= getAttributeSetInstance(product,valueAttribute);
              // }else{
              attributeSetInstance = getAttributeSetInstance(product, valueAttribute);
              setAttributeSetValues(attributeSetInstance, attributeUseList.list(), colorM.trim(),
                  rmw.trim(), chasis.trim(), motor.trim(), importacion.trim());
              // }
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

            // if(validateAttributeSetInstance(product,valueAttribute)){
            // attributeSetInstance= getAttributeSetInstance(product,valueAttribute);
            // }else{
            attributeSetInstance = getAttributeSetInstance(product, valueAttribute);
            setAttributeSetValues(attributeSetInstance, attributeUseList.list(), serie.trim());
            // }

          }
        }
        if (attributeUseList.count() == 2) {
          /**********************************TALLA********************************************/
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
          /**********************************TALLA********************************************/
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
              // if(validateAttributeSetInstance(product,valueAttribute)){
              // attributeSetInstance= getAttributeSetInstance(product,valueAttribute);
              // }else{
              attributeSetInstance = getAttributeSetInstance(product, valueAttribute);
              setAttributeSetValues(attributeSetInstance, attributeUseList.list(), color.trim(),
                  talla.trim());
              // }
            }
          } else {
            throw new OBException(
                "La informacion del producto no coincide con la proporcionada revisar la COLOR,TALLA no este en blanco y el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,SERIE esten en blanco");
          }
        }
      }
      if (product.getAttributeSet() == null) {
        if (!rmw.equals("") || !chasis.equals("") || !motor.equals("") || !colorM.equals("")
            || !importacion.equals("") || !serie.equals("") || !color.equals("")
            || !talla.equals("")) {
          throw new OBException(
              "La informacion del producto no coincide con la proporcionada revisar que el RMW,CHASIS,MOTOR,COLOR,IMPORTACIÓN,COLOR,TALLA,SERIE esten en blanco");
        } else {
          valueAttribute = "";
        }
      }
    }

    // Valida que el pedido cuente con una linea con el producto y la cantidad proporcionada
    if (order != null || !order.equals("")) {
      if (product != null || !product.equals("")) {
        OBCriteria<OrderLine> orderLineList = OBDal.getInstance().createCriteria(OrderLine.class);
        orderLineList.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, order));
        // orderLineList.add(Restrictions.eq(OrderLine.PROPERTY_ORDEREDQUANTITY,Parameter.BIGDECIMAL.parse(cantidad)));
        orderLineList.add(Restrictions.eq(OrderLine.PROPERTY_PRODUCT, product));

        if (orderLineList.count() > 0 && orderLineList.count() < 2) {
          // for(OrderLine orderLine : orderLineList.list()){
          OBCriteria<ShipmentInOutLine> shipmentInOutList = OBDal.getInstance()
              .createCriteria(ShipmentInOutLine.class);
          shipmentInOutList
              .add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, shipmentInOut));
          shipmentInOutList.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE,
              orderLineList.list().get(0)));
          BigDecimal quantity = new BigDecimal("0");
          BigDecimal limit = new BigDecimal("0");
          for (ShipmentInOutLine shipmentInOutLine : shipmentInOutList.list()) {
            quantity = shipmentInOutLine.getMovementQuantity().add(quantity);
          }
          BigDecimal quantityt = orderLineList.list().get(0).getOrderedQuantity()
              .subtract(quantity);
          if (quantityt.intValue() > 0) {
            orderLinenew = orderLineList.list().get(0);
          }
        } else if (orderLineList.count() > 1) {
          throw new OBException(
              "El pedido de compra tiene mas de una linea que cumple con estas caracteristicas cod. producto: "
                  + codigo);
        } else {
          throw new OBException(
              "El pedido de compra no tiene ninguna linea que cumpla con estas caracteristicas cod. producto: "
                  + codigo);
        }

      }
    }

    ShipmentInOutLine shipmentInOutLine = OBProvider.getInstance().get(ShipmentInOutLine.class);

    // Create Line Albaran
    try {
      if (orderLinenew != null) {
        shipmentInOutLine.setLineNo(getSequenceNumber(shipmentInOut));
        shipmentInOutLine.setShipmentReceipt(shipmentInOut);
        shipmentInOutLine.setProduct(product);
        shipmentInOutLine.setMovementQuantity(Parameter.BIGDECIMAL.parse(cantidad));
        shipmentInOutLine.setSprliIdentifier(product.getSearchKey());
        shipmentInOutLine.setUOM(product.getUOM());
        shipmentInOutLine
            .setAttributeSetValue((valueAttribute.equals("") ? null : attributeSetInstance));
        shipmentInOutLine.setStorageBin(locator);
        shipmentInOutLine.setSalesOrderLine(orderLinenew);
        shipmentInOutLine.setOrganization(shipmentInOut.getOrganization());
        shipmentInOutLine.setBusinessPartner(shipmentInOut.getBusinessPartner());

        OBDal.getInstance().save(shipmentInOutLine);
        OBDal.getInstance().flush();

        shipmentInOut.setSalesOrder(order);
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(Utility.messageBD(conn, e.getMessage(), vars.getLanguage()));
    }

    // End process
    OBDal.getInstance().commitAndClose();

    return shipmentInOutLine;
  }

  public Long getSequenceNumber(ShipmentInOut shipmentInOut) throws OBException {
    OBCriteria<ShipmentInOutLine> obc = OBDal.getInstance().createCriteria(ShipmentInOutLine.class);
    obc.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, shipmentInOut));
    obc.addOrderBy(ShipmentInOutLine.PROPERTY_LINENO, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setMaxResults(1);
    ShipmentInOutLine attach = (ShipmentInOutLine) obc.uniqueResult();
    if (attach == null) {
      return 10L;
    }
    return attach.getLineNo() + 10L;
  }

  public AttributeSetInstance getAttributeSetInstance(Product product, String data)
      throws OBException {

    // // Valida si existe una intancia con los mismos atributos
    // OBCriteria<AttributeSetInstance> obc =
    // OBDal.getInstance().createCriteria(AttributeSetInstance.class);
    // obc.add(Restrictions.eq(AttributeSetInstance.PROPERTY_DESCRIPTION, data));
    // obc.add(Restrictions.eq(AttributeSetInstance.PROPERTY_ATTRIBUTESET,
    // product.getAttributeSet()));
    // obc.setMaxResults(1);
    // AttributeSetInstance attach = (AttributeSetInstance) obc.uniqueResult();

    // // Caso contrario crea una nueva
    // if (attach == null) {
    AttributeSetInstance attributeSetInstance = OBProvider.getInstance()
        .get(AttributeSetInstance.class);
    try {
      attributeSetInstance.setAttributeSet(product.getAttributeSet());
      attributeSetInstance.setDescription(data);

      OBDal.getInstance().save(attributeSetInstance);
      OBDal.getInstance().flush();

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw new OBException(Utility.messageBD(conn, e.getMessage(), vars.getLanguage()));
    }

    return attributeSetInstance;
    // }else{
    // return attach;
    // }
  }

  public Boolean validateAttributeSetInstance(Product product, String data) throws OBException {

    // Valida si existe una intancia con los mismos atributos
    OBCriteria<AttributeSetInstance> obc = OBDal.getInstance()
        .createCriteria(AttributeSetInstance.class);
    obc.add(Restrictions.eq(AttributeSetInstance.PROPERTY_DESCRIPTION, data));
    obc.add(Restrictions.eq(AttributeSetInstance.PROPERTY_ATTRIBUTESET, product.getAttributeSet()));
    obc.setMaxResults(1);
    AttributeSetInstance attach = (AttributeSetInstance) obc.uniqueResult();

    if (attach == null) {
      return false;
    }
    return true;

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

}