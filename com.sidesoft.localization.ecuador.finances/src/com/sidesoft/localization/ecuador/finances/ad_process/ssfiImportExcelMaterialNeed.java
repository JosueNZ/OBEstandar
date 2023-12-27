/*
 ************************************************************************************
 * Copyright (C) 2009-2010 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package com.sidesoft.localization.ecuador.finances.ad_process;

import java.text.SimpleDateFormat;
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
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.common.uom.UOMTrl;
import org.openbravo.model.procurement.Requisition;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.module.idljava.proc.IdlServiceJava;
import org.openbravo.model.pricing.pricelist.PriceList;

/**
 * 
 * @author El Rodney
 */
public class ssfiImportExcelMaterialNeed extends IdlServiceJava {

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { 
        new Parameter("no.documento", Parameter.STRING),
        new Parameter("no.linea", Parameter.STRING),
        new Parameter("codigo prod.", Parameter.STRING),
        new Parameter("producto", Parameter.STRING), 
        new Parameter("unidad", Parameter.STRING),
        new Parameter("cantidad", Parameter.STRING),
        // new Parameter("descripcion", Parameter.STRING),
        new Parameter("tarifa", Parameter.STRING),
        new Parameter("precio unitario", Parameter.STRING)
         };
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 60);
    validator.checkString(values[1], 3);
    validator.checkString(values[2], 60);
    validator.checkString(values[3], 60);
    validator.checkString(values[4], 32);
    validator.checkBigDecimal(values[5]);
    // validator.checkString(values[6], 200);
    validator.checkString(values[6], 60);
    validator.checkBigDecimal(values[7]);
	return values;

  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createProduct((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6],
        (String) values[7] );
  }

  public void iniciarCiclo(Requisition Obj_Requisition) {
    OBCriteria<RequisitionLine> ObjEeiProduct = OBDal.getInstance()
        .createCriteria(RequisitionLine.class);
    ObjEeiProduct.add(Restrictions.eq(RequisitionLine.PROPERTY_REQUISITION, Obj_Requisition));

    if (ObjEeiProduct.list().size() > 0) {

      for (RequisitionLine coldelete : ObjEeiProduct.list()) {

        RequisitionLine removeReqLine = OBDal.getInstance().get(RequisitionLine.class,
            coldelete.getId());

        OBDal.getInstance().remove(removeReqLine);
      }
      OBDal.getInstance().commitAndClose();

    }

    // String productName = detalleObj.getProduct().getName();
    /*
     * RequisitionLine objRequisitionLine =
     * OBProvider.getInstance().get(RequisitionLine.class,Obj_Requisition.getId()); try {
     * //objRequisitionLine.setRequisition(Obj_Requisition);
     * OBDal.getInstance().remove(objRequisitionLine); OBDal.getInstance().flush(); } catch
     * (Exception e) { e.printStackTrace(); }
     */
  }

  public BaseOBObject createProduct(final String documentno, final String no_linea,
      final String codigo_pro, final String product, final String unidad, final String cantidad,
      final String tarifa,final String precio_u ) throws Exception {
      // final String description,final String tarifa,final String precio_u ) throws Exception {

    // variable para UOM
    int valor = 0;

    // Recuperar el registro a quien se le cargaran las lineas por el numero de documento.
    Requisition Obj_Requisition = findDALInstance(false, Requisition.class,
        new Value(Requisition.PROPERTY_DOCUMENTNO, documentno));
    if (Obj_Requisition == null || Obj_Requisition.equals("")) {
      throw new OBException(
          "Necesidad de material con el No. documento  :  " + documentno + " no existe");
    }

    // Recuperar Tarifa con el nombre proporcionado.
    PriceList Obj_PriceList = findDALInstance(false, PriceList.class,
        new Value(PriceList.PROPERTY_NAME, tarifa.toUpperCase()));
    if (Obj_PriceList == null || Obj_PriceList.equals("")) {
      iniciarCiclo(Obj_Requisition);
      throw new OBException(
          "La Tarifa proporcionada con el nombre:  " + tarifa.toUpperCase() + " no existe");
    }

    // Recuperar Producto con su identificador
    Product product_id = findDALInstance(false, Product.class,
        new Value(Product.PROPERTY_SEARCHKEY, codigo_pro));
    if (product_id == null || product_id.equals("")) {
      iniciarCiclo(Obj_Requisition);
      throw new OBException("Producto con el código :  " + codigo_pro + " no existe");
    }

    // recuperar unidad del producto en su traduccion
    UOMTrl UOMTRL = findDALInstance(false, UOMTrl.class, new Value(UOMTrl.PROPERTY_NAME, unidad));
    // recuperar unidad del producto
    UOM UOMobj = findDALInstance(false, UOM.class, new Value(UOM.PROPERTY_NAME, unidad));
    if (UOMTRL == null || UOMTRL.equals("")) {
      valor = 1;// variable para condiciones de unidad
      if (UOMobj == null || UOMobj.equals("")) {
        iniciarCiclo(Obj_Requisition);
        throw new OBException("Unidad con el nombre :  " + unidad + " no existe");
      }
    }
    // comparar que la unidad del producto sea el mismo que el extraido del documento
    if (valor == 1) {
      if (UOMobj.getId() != product_id.getUOM().getId()) {
        iniciarCiclo(Obj_Requisition);
        throw new OBException("La unidad original del producto con el codigo: " + codigo_pro
            + " no coincide con la dispuesta en el documento.");
      }
    } else if (!product_id.getUOM().getId().equals(UOMTRL.getUOM().getId())) {
      iniciarCiclo(Obj_Requisition);
      throw new OBException("La unidad original del producto con el codigo: " + codigo_pro
          + " no coincide con la dispuesta en el documento.");
    }

    // Tabla a la cual se incertara
    RequisitionLine objRequisitionLine = OBProvider.getInstance().get(RequisitionLine.class);

    try {

      objRequisitionLine.setRequisition(Obj_Requisition);
      objRequisitionLine.setLineNo(new Long(no_linea));
      objRequisitionLine.setProduct(product_id);
      // condicion para unidad.(con traduccion o sin traduccion))
      if (valor == 1) {
        objRequisitionLine.setUOM(UOMobj);
      } else {
        objRequisitionLine.setUOM(UOMTRL.getUOM());
      }
      objRequisitionLine.setQuantity(Parameter.BIGDECIMAL.parse(cantidad));
      objRequisitionLine.setPriceList(Obj_PriceList);
      objRequisitionLine.setUnitPrice(Parameter.BIGDECIMAL.parse(precio_u));
      objRequisitionLine.setCurrency(Obj_Requisition.getOrganization().getCurrency());
      objRequisitionLine.setLineNetAmount(Parameter.BIGDECIMAL.parse(precio_u).multiply(objRequisitionLine.getQuantity()));
      Date myDate = new Date();
      objRequisitionLine
          .setNeedByDate(Parameter.DATE.parse(new SimpleDateFormat("yyyy-MM-dd").format(myDate)));

      OBDal.getInstance().save(objRequisitionLine);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
    OBDal.getInstance().commitAndClose();
    return objRequisitionLine;
  }
}
