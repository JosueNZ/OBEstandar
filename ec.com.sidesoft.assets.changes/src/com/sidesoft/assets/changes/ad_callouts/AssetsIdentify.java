package ec.com.sidesoft.assets.changes.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;

public class AssetsIdentify extends SimpleCallout {

  private static final long serialVersionUID = 3653617759010780960L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String AssetId = info.getStringParameter("inpaAssetId", null);

    Asset assetObj = OBDal.getInstance().get(Asset.class, AssetId);
    if (assetObj.getId() != null) {

      info.addResult("inpname",assetObj.getName() );
    }
  }

}
