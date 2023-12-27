package com.sidesoft.ecuador.asset.allocation.ad_callouts;

import javassist.expr.Cast;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.plm.Product;

public class SearchIdentifierProductAsset extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    // inpcDoctypeId
    // inpnoCertificate

    String StrProductID = info.getStringParameter("inpmProductId", null);
    Product ObjProductIdentifier = OBDal.getInstance().get(Product.class, StrProductID);
    

    if (ObjProductIdentifier.getIdentifier() != null) {
    	
      String StrIdentifier = ObjProductIdentifier.getIdentifier();
      info.addResult("inpvalue", StrIdentifier );
      
    }
  }

}