package ec.com.sidesoft.accounting.process;

import java.util.Map;

import org.openbravo.client.application.FilterExpression;

public class DocumenTypeFilterExpression implements FilterExpression {

  @Override
  public String getExpression(Map<String, String> requestMap) {
    StringBuilder whereClause = new StringBuilder();
    whereClause.append("e.table.id = '" + requestMap.get("AD_Table_ID") + "'");
    return whereClause.toString();
  }

}
