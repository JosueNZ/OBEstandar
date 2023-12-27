package ec.com.sidesoft.block.delete.events;

import javax.enterprise.event.Observes;

import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.gl.GLJournal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import com.sidesoft.hrm.payroll.sspr_settlement;
import org.openbravo.model.financialmgmt.assetmgmt.Amortization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ec.com.sidesoft.localization.ecuador.withholdingssales.SSWSWithholdingSale;

public class SSBD_BlockRecord extends EntityPersistenceEventObserver {
	private static final Logger logger = LoggerFactory.getLogger(SSBD_BlockRecord.class);

	private static Entity[] entities = { ModelProvider.getInstance().getEntity(Order.ENTITY_NAME),
			ModelProvider.getInstance().getEntity(FIN_Payment.ENTITY_NAME),
			ModelProvider.getInstance().getEntity(GLJournal.ENTITY_NAME),
			ModelProvider.getInstance().getEntity(SSWSWithholdingSale.ENTITY_NAME),
			ModelProvider.getInstance().getEntity(sspr_settlement.ENTITY_NAME),
			ModelProvider.getInstance().getEntity(FIN_FinancialAccount.ENTITY_NAME),
			ModelProvider.getInstance().getEntity(Amortization.ENTITY_NAME),
			ModelProvider.getInstance().getEntity(Invoice.ENTITY_NAME) };

	@Override
	protected Entity[] getObservedEntities() {
		return entities;
	}

	public void onUpdate(@Observes EntityUpdateEvent event) {
		if (!isValidEvent(event)) {
			return;
		}
	}

	public void onSave(@Observes EntityNewEvent event) {
		if (!isValidEvent(event)) {
			return;
		}
	}

	public void onDelete(@Observes EntityDeleteEvent event) {
		if (!isValidEvent(event)) {
			return;
		}

		switch (event.getTargetInstance().getEntityName()) {
		case Order.ENTITY_NAME:
			Order order = (Order) event.getTargetInstance();
			errorMessage();
			break;
		case FIN_Payment.ENTITY_NAME:
			FIN_Payment payment = (FIN_Payment) event.getTargetInstance();
			errorMessage();
			break;
		case GLJournal.ENTITY_NAME:
			GLJournal GL_Journal = (GLJournal) event.getTargetInstance();
			errorMessage();
			break;
		case SSWSWithholdingSale.ENTITY_NAME:
			SSWSWithholdingSale SSWS_WithholdingSale = (SSWSWithholdingSale) event.getTargetInstance();
			errorMessage();
			break;
		case FIN_FinancialAccount.ENTITY_NAME:
			FIN_FinancialAccount FIN_Financial_Account = (FIN_FinancialAccount) event.getTargetInstance();
			errorMessage();
			break;
		case sspr_settlement.ENTITY_NAME:
			sspr_settlement ssprSettlement = (sspr_settlement) event.getTargetInstance();
			errorMessage();
			break;
		case Amortization.ENTITY_NAME:
			Amortization AAmortization = (Amortization) event.getTargetInstance();
			errorMessage();
			break;	
		case Invoice.ENTITY_NAME:
			Invoice C_Invoice = (Invoice) event.getTargetInstance();
			errorMessage();
			break;
		default:
			return;
		}
	}

	private void errorMessage() {
		String message = OBMessageUtils.messageBD("El registro no puede ser Eliminado");
		logger.error(message);
		throw new OBException(message);
	}

}
