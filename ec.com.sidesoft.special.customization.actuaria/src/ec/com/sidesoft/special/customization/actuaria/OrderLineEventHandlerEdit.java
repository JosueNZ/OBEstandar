package ec.com.sidesoft.special.customization.actuaria;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

public class OrderLineEventHandlerEdit extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(OrderLine.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    OrderLine currentOrderLine = (OrderLine) event.getTargetInstance();

    if (currentOrderLine.getSalesOrder().isSalesTransaction()) {

      final Order order = currentOrderLine.getSalesOrder();
      if ("I".equals(order.getScactuInterface()) || "P".equals(order.getScactuInterface())) {
        checkUserRoleForEditImportedOrder(order);
      }
    }

  }

  private void checkUserRoleForEditImportedOrder(final Order order) {
    if (!StringUtils.equals(OBContext.getOBContext().getRole().getId(),
        "1F919FC867D0406596F286843E3D6845")
        && !StringUtils.equals(OBContext.getOBContext().getRole().getId(),
            "8B0456862144422BA3878D8E08A04DC0")) {
      throw new OBException("El actual usuario no tiene permiso para editar este pedido.");
    }
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    OrderLine currentOrderLine = (OrderLine) event.getTargetInstance();

    if (currentOrderLine.getSalesOrder().isSalesTransaction()) {

      final Order order = currentOrderLine.getSalesOrder();

      if ("I".equals(order.getScactuInterface()) || "P".equals(order.getScactuInterface())) {
        checkUserRoleForEditImportedOrder(order);
      }

    }

  }

}
