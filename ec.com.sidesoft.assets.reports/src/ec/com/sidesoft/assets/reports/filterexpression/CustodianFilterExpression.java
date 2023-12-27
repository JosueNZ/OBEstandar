package ec.com.sidesoft.assets.reports.filterexpression;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;

public class CustodianFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    StringBuilder whereClause = new StringBuilder();
    whereClause.append(" e.employee = 'Y' ");
    String Assettype = requestMap.get("inpemSsamAssettype");
    String BpartnerId = requestMap.get("inpcBpartnerId");
    String SubcategoryId = requestMap.get("inpemSsaslSubcategoryId");
    String WhereAsset = " and e.id in (select b.ssalCustodio.id from FinancialMgmtAsset b where '1' = '1' ";

    if (Assettype != null && !Assettype.equals("")) {
      WhereAsset = WhereAsset + " and b.ssamAssettype = '" + Assettype + "' ";
    }
    if (SubcategoryId != null && !SubcategoryId.equals("")) {
      WhereAsset = WhereAsset + " and b.ssaslSubcategory.id = '" + SubcategoryId + "' ";
    }
    WhereAsset = WhereAsset + " ) ";
    whereClause.append(WhereAsset);
    return whereClause.toString();
  }

}
