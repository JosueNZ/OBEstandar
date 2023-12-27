package ec.com.sidesoft.custom.advpaymentmngt.multiple.payments;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(Cscamp_PaymentProporsalMultiplePaymentsProvider.QUALIFIER)
public class Cscamp_PaymentProporsalMultiplePaymentsProvider extends BaseComponentProvider {
  public static final String QUALIFIER = "CSCAMP";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    final String prefix = "web/ec.com.sidesoft.custom.advpaymentmngt.multiple.payments";

    final String[] erpResourceDependency = { "/js/multiplepayments" };

    for (String resource : erpResourceDependency) {
      globalResources.add(createStaticResource(prefix + resource + ".js", true));
    }

    return globalResources;
  }

}
