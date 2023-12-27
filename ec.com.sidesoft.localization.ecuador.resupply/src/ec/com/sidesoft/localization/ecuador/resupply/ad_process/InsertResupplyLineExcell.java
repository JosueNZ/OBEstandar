/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package ec.com.sidesoft.localization.ecuador.resupply.ad_process;

import java.math.BigDecimal;
import java.util.List;

import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductUOM;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.common.uom.UOMConversion;
import org.openbravo.module.idljava.proc.IdlServiceJava;

import ec.com.sidesoft.localization.ecuador.resupply.ssrsresupply;
import ec.com.sidesoft.localization.ecuador.resupply.ssrsresupplyline;

/**
 * 
 * @author Dieguito
 */
public class InsertResupplyLineExcell extends IdlServiceJava {

  @Override
  public String getEntityName() {
    return "Simple Products";
  }

  @Override
  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("document", Parameter.STRING),
        new Parameter("barcode", Parameter.STRING), new Parameter("quantity", Parameter.STRING),
        new Parameter("uom", Parameter.STRING), new Parameter("uomsec", Parameter.STRING) };
  }

  @Override
  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 32);
    validator.checkString(values[1], 32);
    validator.checkBigDecimal(values[2]);
    return values;
  }

  @Override
  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createProduct((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4]);
  }

  public BaseOBObject createProduct(final String document, final String barcode,
      final String quantity, final String uom, final String uomsec) throws Exception {

    // RESUPPLY
    ssrsresupply resupplyhead = findDALInstance(false, ssrsresupply.class, new Value("documentNo",
        document));
    ssrsresupplyline line = OBProvider.getInstance().get(ssrsresupplyline.class);
    try {
      line.setActive(true);
      line.setOrganization(rowTransactionalOrg);
      line.setSsrsResupply(resupplyhead);
      line.setProduct(findDALInstance(true, Product.class, new Value("searchKey", barcode)));
      line.setOrderQuantity(Parameter.BIGDECIMAL.parse(quantity));

      line.setLineNo(10L);
      line.setSearchKey(barcode);
      line.setUOM(line.getProduct().getUOM());
      Product product = line.getProduct();
      ProductUOM secUOM = product.getProductUOMList().get(0);
      line.setOrderUOM(secUOM);
      UOM uominicio = product.getUOM();
      // ProductUOM strInitUOM = OBDal.getInstance().get(ProductUOM.class, secUOM);
      UOM conver = secUOM.getUOM();
      BigDecimal multiply = BigDecimal.ZERO, uomProduct = BigDecimal.ZERO;
      if (conver.getId().equals(uominicio)) {
        multiply = BigDecimal.ONE;
      } else {
        List<UOMConversion> multiplyRateList = conver.getUOMConversionList();
        for (UOMConversion first : multiplyRateList) {
          if (first.getToUOM().equals(uominicio) && first.getUOM().equals(conver)) {
            multiply = first.getMultipleRateBy();
          }
        }
      }

      if (Parameter.BIGDECIMAL.parse(quantity).compareTo(BigDecimal.ZERO) == 0
          || multiply.compareTo(BigDecimal.ZERO) == 0) {
        line.setQuantity(Parameter.BIGDECIMAL.parse(quantity));
      } else {
        uomProduct = Parameter.BIGDECIMAL.parse(quantity).multiply(multiply);
        line.setQuantity(uomProduct);
      }

      OBDal.getInstance().save(line);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
    OBDal.getInstance().commitAndClose();
    return line;
  }

}
