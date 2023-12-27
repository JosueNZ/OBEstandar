package ec.cusoft.facturaec.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.plm.Product;

public class UpdateIdentifierByProduct extends SimpleCallout {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    // inpcDoctypeId
    // inpnoCertificate

    String StrProductId = info.getStringParameter("inpmProductId", null);
    Product ObjProduct = OBDal.getInstance().get(Product.class, StrProductId);
    

    if (ObjProduct.getName() != null) {

      info.addResult("inpvalue", ObjProduct.getName().toString());
      
    }
  }

}
