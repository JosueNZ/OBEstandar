package ec.com.sidesoft.backoffice.discount;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.openbravo.client.kernel.BaseComponentProvider;
import org.openbravo.client.kernel.Component;

@ApplicationScoped
public class ApplicationProvider extends BaseComponentProvider {

    @Override
    public Component getComponent(String componentId, Map<String, Object> parameters) {
        return null;
    }

    public List<ComponentResource> getGlobalComponentResources() {
        final List<ComponentResource> globalResources = new ArrayList<ComponentResource>();
        globalResources.add(createStaticResource(
                "web/ec.com.sidesoft.backoffice.discount/js/ecsap-authorization-process.js", true));
        return globalResources;
    }
}
