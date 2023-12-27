package ec.com.sidesoft.assets.reports.filterexpression;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;

public class AssetFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    StringBuilder whereClause = new StringBuilder();
    whereClause.append(" '1' = '1' ");
    String Assettype = requestMap.get("inpemSsamAssettype");
    String BpartnerId = requestMap.get("inpcBpartnerId");
    String SubcategoryId = requestMap.get("inpemSsaslSubcategoryId");
    if (Assettype != null && !Assettype.equals("")) {
      whereClause.append(" and e.ssamAssettype = '" + Assettype + "' ");
    }
    if (BpartnerId != null && !BpartnerId.equals("")) {
      whereClause.append(" and e.ssalCustodio.id = '" + BpartnerId + "' ");
    }
    if (SubcategoryId != null && !SubcategoryId.equals("")) {
      whereClause.append(" and e.ssaslSubcategory.id = '" + SubcategoryId + "' ");
    }

    return whereClause.toString();
  }

}
