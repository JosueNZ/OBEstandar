package ec.com.sidesoft.saleorder.relations;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.apache.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;

public class InvoiceLineEventHandleCreditNote extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(InvoiceLine.ENTITY_NAME) };
  protected Logger logger = Logger.getLogger(this.getClass());

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Property invoiceProperty = event.getTargetInstance().getEntity()
        .getProperty(InvoiceLine.PROPERTY_INVOICE);

    Invoice invoiceCurrentState = (Invoice) event.getCurrentState(invoiceProperty);
    if (invoiceCurrentState.isSalesTransaction()) {

      final Property orderLineProperty = event.getTargetInstance().getEntity()
          .getProperty(InvoiceLine.PROPERTY_SALESORDERLINE);

      OrderLine orderLine = (OrderLine) event.getCurrentState(orderLineProperty);

      if (invoiceCurrentState.getTransactionDocument().isReturn() && orderLine == null) {
        throw new OBException(
            "No es posible adicionar esta linea a la nota de credito, esta no contiene un pedido de venta relacionado.");
      }

      if (invoiceCurrentState.isScnrIsrefInv() && invoiceCurrentState.getScnrInvoice() != null
          && invoiceCurrentState.getScnrInvoice().getSalesOrder() != null) {

        if (invoiceCurrentState.getTransactionDocument().isReversal()) {
          final Property productProperty = event.getTargetInstance().getEntity()
              .getProperty(InvoiceLine.PROPERTY_PRODUCT);
          Product product = (Product) event.getCurrentState(productProperty);

          OBCriteria<OrderLine> obcOrderLine = OBDal.getInstance().createCriteria(OrderLine.class);
          obcOrderLine.add(
              Restrictions.eq(OrderLine.PROPERTY_SALESORDER, invoiceCurrentState.getSalesOrder()));
          obcOrderLine.add(Restrictions.eq(OrderLine.PROPERTY_PRODUCT, product));
          obcOrderLine.setMaxResults(1);
          orderLine = (OrderLine) obcOrderLine.uniqueResult();
        }

        final Property qtyInvoicedProperty = event.getTargetInstance().getEntity()
            .getProperty(InvoiceLine.PROPERTY_INVOICEDQUANTITY);
        BigDecimal invoicedQty = (BigDecimal) event.getCurrentState(qtyInvoicedProperty);

        if (orderLine != null && invoicedQty.abs().compareTo(orderLine.getOrderedQuantity()) == 1) {
          throw new OBException(" La cantidad a revertir (" + invoicedQty
              + ") no puede ser mayor que la cantidad ordenada ( "
              + orderLine.getSalesOrder().getDocumentNo() + "- linea: " + orderLine.getLineNo()
              + " - Cantidad ordeneda : " + orderLine.getOrderedQuantity() + " ).  ");
        }

        if (orderLine != null && invoiceCurrentState.getTransactionDocument().isReversal()) {
          event.setCurrentState(orderLineProperty, orderLine);
        }

      }
    }

  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final Property invoiceProperty = event.getTargetInstance().getEntity()
        .getProperty(InvoiceLine.PROPERTY_INVOICE);

    Invoice invoiceCurrentState = (Invoice) event.getCurrentState(invoiceProperty);

    if (invoiceCurrentState.isSalesTransaction() && invoiceCurrentState.isScnrIsrefInv()
        && invoiceCurrentState.getScnrInvoice() != null
        && invoiceCurrentState.getScnrInvoice().getSalesOrder() != null) {

      final Property productProperty = event.getTargetInstance().getEntity()
          .getProperty(InvoiceLine.PROPERTY_PRODUCT);

      final Property orderLineProperty = event.getTargetInstance().getEntity()
          .getProperty(InvoiceLine.PROPERTY_SALESORDERLINE);

      final Property qtyInvoicedProperty = event.getTargetInstance().getEntity()
          .getProperty(InvoiceLine.PROPERTY_INVOICEDQUANTITY);

      Product product = (Product) event.getCurrentState(productProperty);

      OBCriteria<OrderLine> obcOrderLine = OBDal.getInstance().createCriteria(OrderLine.class);
      obcOrderLine
          .add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, invoiceCurrentState.getSalesOrder()));
      obcOrderLine.add(Restrictions.eq(OrderLine.PROPERTY_PRODUCT, product));
      obcOrderLine.setMaxResults(1);
      OrderLine orderLine = (OrderLine) obcOrderLine.uniqueResult();

      BigDecimal invoicedQty = (BigDecimal) event.getCurrentState(qtyInvoicedProperty);

      if (invoicedQty.abs().compareTo(orderLine.getOrderedQuantity()) == 1) {
        throw new OBException(" La cantidad a revertir (" + invoicedQty
            + ") no puede ser mayor que la cantidad ordenada ( "
            + orderLine.getSalesOrder().getDocumentNo() + "- linea: " + orderLine.getLineNo()
            + " - Cantidad ordeneda : " + orderLine.getOrderedQuantity() + " ).  ");
      }
      event.setCurrentState(orderLineProperty, orderLine);
    }

  }
}
