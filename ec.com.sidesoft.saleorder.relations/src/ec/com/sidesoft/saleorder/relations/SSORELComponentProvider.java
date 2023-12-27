package ec.com.sidesoft.saleorder.relations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;
import org.openbravo.client.kernel.ComponentProvider;

@ApplicationScoped
@ComponentProvider.Qualifier(SSORELComponentProvider.SSOREL_COMPONENT_TYPE)
public class SSORELComponentProvider extends BaseComponentProvider {

  public static final String SSOREL_COMPONENT_TYPE = "SSOREL_ViewType";

  @Override
  public Component getComponent(String componentId, Map<String, Object> parameters) {
    throw new IllegalArgumentException("Component id " + componentId + " not supported.");
  }

  @Override
  public List<ComponentResource> getGlobalComponentResources() {
    final ArrayList<ComponentResource> resources = new ArrayList<ComponentResource>();
    resources.add(createStaticResource(
        "web/ec.com.sidesoft.saleorder.relations/js/invoice-lines-selected.js", false));
    return resources;
  }

}
