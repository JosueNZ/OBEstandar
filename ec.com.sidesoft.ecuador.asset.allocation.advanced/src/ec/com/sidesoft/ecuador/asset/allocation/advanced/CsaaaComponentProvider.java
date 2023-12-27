package ec.com.sidesoft.ecuador.asset.allocation.advanced;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(CsaaaComponentProvider.QUALIFIER)
public class CsaaaComponentProvider extends BaseComponentProvider {
  public static final String QUALIFIER = "CSAAA_MAIN";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {

    final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
    final String prefix = "web/ec.com.sidesoft.ecuador.asset.allocation.advanced";

    final String[] erpResourceDependency = { "/js/barcodeSearch" };

    for (String resource : erpResourceDependency) {
      globalResources.add(createStaticResource(prefix + resource + ".js", true));
    }

    return globalResources;
  }

}