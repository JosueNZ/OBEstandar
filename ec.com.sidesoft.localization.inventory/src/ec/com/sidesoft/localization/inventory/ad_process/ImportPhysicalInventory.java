/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.localization.inventory.ad_process;

import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.common.uom.UOMTrl;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.module.idljava.proc.IdlServiceJava;

/**
 * 
 * @author Dieguito
 */
public class ImportPhysicalInventory extends IdlServiceJava {

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Numero documento", Parameter.STRING),
        new Parameter("Numero Linea", Parameter.STRING),
        new Parameter("Producto", Parameter.STRING), new Parameter("Hueco", Parameter.STRING),
        new Parameter("Cantidad", Parameter.STRING), new Parameter("Unidad", Parameter.STRING),
        new Parameter("Costo", Parameter.STRING) };
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 32);
    validator.checkString(values[1], 32);
    validator.checkString(values[2], 32);
    validator.checkString(values[3], 32);
    validator.checkString(values[4], 32);
    validator.checkString(values[5], 32);
    validator.checkString(values[6], 32);
    return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createphysicalinventory((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6]);
  }

  public BaseOBObject createphysicalinventory(final String documentno, final String line,
      final String product, final String locator, final String amount, final String unit,
      final String cost) throws Exception {

    InventoryCount inventoryid = findDALInstance(false, InventoryCount.class,
        new Value(InventoryCount.PROPERTY_DOCUMENTNO, documentno));
    if (inventoryid == null || inventoryid.equals("")) {
      throw new OBException("Numero de documento  " + documentno + " no existe");
    }

    Product productid = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, product));
    if (productid == null || productid.equals("")) {
      throw new OBException("Producto: " + product + " no existe");
    }

    Locator locatorid = findDALInstance(false, Locator.class,
        new Value(Locator.PROPERTY_SEARCHKEY, locator));
    if (locatorid == null || locatorid.equals("")) {
      throw new OBException("Hueco: " + locator + " no existe");
    }

    // Validacion unidad de medida traduccion
    UOMTrl uomtrlid = findDALInstance(false, UOMTrl.class, new Value(UOMTrl.PROPERTY_NAME, unit));
    if (uomtrlid == null || uomtrlid.equals("")) {
      throw new OBException("Unidad de medida: " + unit + " no existe");
    }
    // new Value(UOMTrl.PROPERTY_LANGUAGE, La));
    UOM uomid = findDALInstance(false, UOM.class,
        new Value(UOM.PROPERTY_EDICODE, uomtrlid.getUOM().getEDICode()));

    if (uomid == null || uomid.equals("")) {
      throw new OBException("Unidad : " + unit + " no existe");
    } else {
      Product uomproduct = findDALInstance(false, Product.class, new Value("searchKey", product),
          new Value("uOM", uomid));
      if (uomproduct == null || uomproduct.equals("")) {
        throw new OBException("Unidad: " + unit + " del producto: " + product
            + " no corresponde a la unidad configurada en el maestro de productos");
      }
    }
    // INICIO A.M. 1102 16/07/2018

    OBCriteria<StorageDetail> ObjStorageDetail = OBDal.getInstance()
        .createCriteria(StorageDetail.class);
    ObjStorageDetail.add(Restrictions.and(Restrictions.eq("product", productid),
        Restrictions.eq("storageBin", locatorid)));
    ObjStorageDetail.add(Restrictions.eq(StorageDetail.PROPERTY_UOM, productid.getUOM()));

    String strBookQuatity = "0";
    List<StorageDetail> lstStorageDetail = ObjStorageDetail.list();
    if (lstStorageDetail.size() > 0) {

      for (StorageDetail detalle : lstStorageDetail) {
        try {
          if (detalle.getQuantityOnHand() != null
              && !detalle.getQuantityOnHand().toString().trim().equals("")) {
            strBookQuatity = (detalle.getQuantityOnHand()).toString();
          }
        } catch (Exception e) {
          System.out.println("Error al obtener valor de stock" + e.getMessage());
        }
      }

    }
    // FIN A.M. 1102 16/07/2018

    InventoryCountLine inventoryline = OBProvider.getInstance().get(InventoryCountLine.class);

    try {
      inventoryline.setPhysInventory(inventoryid);
      inventoryline.setLineNo(new Long(line));
      inventoryline.setProduct(productid);
      inventoryline.setStorageBin(locatorid);
      inventoryline.setUOM(uomid);
      inventoryline.setBookQuantity(Parameter.BIGDECIMAL.parse(strBookQuatity));
      inventoryline.setQuantityCount(Parameter.BIGDECIMAL.parse(amount));

      OBDal.getInstance().save(inventoryline);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }

    OBDal.getInstance().commitAndClose();
    return inventoryline;
  }
}
