package ec.com.sidesoft.assets.reports.filterexpression;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;

public class SubCategoryFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    StringBuilder whereClause = new StringBuilder();
    String AssetGroupId = requestMap.get("inpaAssetGroupId");
    String WhereAsset = " e.id in (select b.id from SSASL_Subcategory b  ";

    if (AssetGroupId != null && !AssetGroupId.equals("")) {
      WhereAsset = WhereAsset + " where  b.assetCategory.id = '" + AssetGroupId + "' ";
    }

    WhereAsset = WhereAsset + " ) ";
    whereClause.append(WhereAsset);
    return whereClause.toString();
  }

}
