package ec.com.sidesoft.saleorder.relations;

import java.util.Map;

import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

@ComponentProvider.Qualifier("7C4C1E6EEA0E4EE3906826BFA82E01EE")
public class CreditNoteHqlQueryTransformer extends HqlQueryTransformer {

  final static String TABLE_ID = "7C4C1E6EEA0E4EE3906826BFA82E01EE";

  @Override
  public String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {

    final String invoiceIdParameter = requestParameters.get("@Invoice.id@");
    final Invoice invoice = OBDal.getInstance().get(Invoice.class, invoiceIdParameter);
    final String orgId = invoice.getOrganization().getId();

    final String whereFilter = " invl.organization.id = '" + orgId + "'"
        + " AND NOT EXISTS (SELECT 1 " //
        + "                                   FROM InvoiceLine invl01  " //
        + "                                  WHERE invl01.invoice.id = '" + invoiceIdParameter + "'"
        + "                                    AND invl01.ssorelInvlineid = invl.id) ";

    hqlQuery = hqlQuery.replace("@removeLine_filters@", whereFilter);
    return hqlQuery;
  }

}
