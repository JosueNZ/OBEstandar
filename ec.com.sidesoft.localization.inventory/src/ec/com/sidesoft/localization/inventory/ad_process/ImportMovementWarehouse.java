/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.localization.inventory.ad_process;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.common.uom.UOMTrl;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.module.idljava.proc.IdlServiceJava;

/**
 * 
 * @author Dieguito
 */
public class ImportMovementWarehouse extends IdlServiceJava {

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("No Documento", Parameter.STRING),
        new Parameter("No Linea", Parameter.STRING),
        new Parameter("Item transferido", Parameter.STRING),
        new Parameter("Bodega Inicio", Parameter.STRING),
        new Parameter("Bodega Final", Parameter.STRING),
        new Parameter("Cantidad", Parameter.STRING), new Parameter("Unidad", Parameter.STRING) };
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

    return createmovement((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6]);
  }

  public BaseOBObject createmovement(final String documentno, final String line,
      final String product, final String locatorfrom, final String locatorto, final String amount,
      final String unit) throws Exception {

    InternalMovement movementid = findDALInstance(false, InternalMovement.class,
        new Value(InternalMovement.PROPERTY_DOCUMENTNO, documentno));
    if (movementid == null || movementid.equals("")) {
      throw new OBException("Numero de documento  " + documentno + " no existe");
    }
    // Valida estado procesado
    InternalMovement movementprocess = findDALInstance(false, InternalMovement.class,
        new Value(InternalMovement.PROPERTY_DOCUMENTNO, documentno),
        new Value(InternalMovement.PROPERTY_PROCESSED, true));
    if (movementprocess != null) {
      throw new OBException(documentno + " @20501@");
    }
    // Valida estado contabilizado
    InternalMovement movementposted = findDALInstance(false, InternalMovement.class,
        new Value(InternalMovement.PROPERTY_DOCUMENTNO, documentno),
        new Value(InternalMovement.PROPERTY_POSTED, "Y"));
    if (movementposted != null) {
      throw new OBException(documentno + " @20501@");
    }

    Product productid = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, product));
    if (productid == null || productid.equals("")) {
      throw new OBException("Producto: " + product + " no existe");
    }

    Locator locatorfromid = findDALInstance(false, Locator.class,
        new Value(Locator.PROPERTY_SEARCHKEY, locatorfrom));
    if (locatorfromid == null || locatorfromid.equals("")) {
      throw new OBException("Bodega inicio: " + locatorfrom + " no existe");
    }

    Locator locatortoid = findDALInstance(false, Locator.class,
        new Value(Locator.PROPERTY_SEARCHKEY, locatorto));
    if (locatortoid == null || locatortoid.equals("")) {
      throw new OBException("Bodega final: " + locatorto + " no existe");
    }
    
    // Validacion unidad de medida traduccion
    UOMTrl uomtrlid = findDALInstance(false, UOMTrl.class, new Value(UOMTrl.PROPERTY_NAME, unit));
    if (uomtrlid == null || uomtrlid.equals("")) {
      throw new OBException("Unidad de medida: " + unit + " no existe");
    }
    // new Value(UOMTrl.PROPERTY_LANGUAGE, La));
    UOM uomid = findDALInstance(false, UOM.class,
        new Value(UOM.PROPERTY_EDICODE, uomtrlid.getUOM().getEDICode()));
    if ((uomid == null || uomid.equals(""))) {
      throw new OBException("Unidad : " + unit + " no existe");
    }else {
      Product uomproduct = findDALInstance(false, Product.class, new Value("searchKey", product),
          new Value("uOM", uomid));
      if (uomproduct == null || uomproduct.equals("")) {
        throw new OBException("Unidad: " + unit + " del producto: " + product
            + " no corresponde a la unidad configurada en el maestro de productos");
      }
    }

    InternalMovementLine internalmovementline = OBProvider.getInstance()
        .get(InternalMovementLine.class);

    try {
      internalmovementline.setMovement(movementid);
      internalmovementline.setLineNo(new Long(line));
      internalmovementline.setProduct(productid);
      internalmovementline.setMovementQuantity(Parameter.BIGDECIMAL.parse(amount));
      internalmovementline.setUOM(uomid);
      internalmovementline.setStorageBin(locatorfromid);
      internalmovementline.setNewStorageBin(locatortoid);

      OBDal.getInstance().save(internalmovementline);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }

    OBDal.getInstance().commitAndClose();
    return internalmovementline;
  }
}
