package ec.com.sidesoft.magicstudio.custom.commentary.background;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.isismtt.x509.Restriction;
import org.eclipse.jdt.internal.compiler.ast.ForeachStatement;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.info.SalesOrder;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.scheduling.KillableProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.ToDoubleFunction;

import javax.persistence.criteria.CriteriaBuilder.In;

public class SmsccUpdateProyectStatus extends DalBaseProcess implements KillableProcess {

	private final Logger logger = Logger.getLogger(Costcenter.class);
	private boolean killProcess = false;

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception {
		// TODO Auto-generated method stub
		OBError msg = new OBError();
		OBContext.setAdminMode(true);
		
		try {
			OBCriteria<Costcenter> CCost = OBDal.getInstance().createCriteria(Costcenter.class);
			List<Costcenter> Ccenterlist = CCost.list();
			for (Costcenter costcenter : Ccenterlist) {
				if (killProcess) {
					throw new OBException("Process Killed");
				}
				
				BigDecimal totalval = costcenter.getSmspTotal() == null ? BigDecimal.ZERO : costcenter.getSmspTotal();
				totalval = costcenter.getSmspComputedvalue() == null ? BigDecimal.ZERO : costcenter.getSmspComputedvalue();
				OBCriteria<Invoice> invoi = OBDal.getInstance().createCriteria(Invoice.class);
				invoi.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, true));
				invoi.add(Restrictions.eq(Invoice.PROPERTY_COSTCENTER, costcenter));
				List<Invoice> invoic = invoi.list();
				if (invoic.size() != 0) {
					String docno = null;
					String docstat = null;
					/////// nueva edicion /////////
					BigDecimal totalfacturado = new BigDecimal(0);
					BigDecimal totalPPpagado = new BigDecimal(0);
					BigDecimal totalProviPagado = new BigDecimal(0);
					BigDecimal totalProviPagar = new BigDecimal(0);
					boolean tienefac = false;
					boolean tienepag = false;
					boolean provetienepag = false;
					
					for (Invoice invoice : invoic) {
						docno = invoice.getDocumentNo();
						docstat = invoice.getDocumentStatus();
						if (docstat.equals("CO")) {
							tienefac = true;
							totalfacturado = totalfacturado.add(invoice.getGrandTotalAmount());
							
//							costcenter.setSmspIsinvoiced(totalfacturado);
//							OBDal.getInstance().save(costcenter);
//							OBDal.getInstance().flush();

							OBCriteria<FIN_PaymentSchedule> totalPPagos = OBDal.getInstance()
									.createCriteria(FIN_PaymentSchedule.class);
							List<FIN_PaymentSchedule> fin_PaymentSchedules = invoice.getFINPaymentScheduleList();
							for (FIN_PaymentSchedule fin_PaymentSchedule : fin_PaymentSchedules) {
								totalPPpagado = totalPPpagado.add(fin_PaymentSchedule.getPaidAmount());
//								costcenter.setSmspIscharged(totalPPpagado);
//								OBDal.getInstance().save(costcenter);
//								OBDal.getInstance().flush();
								int sinpp = totalPPpagado.compareTo(new BigDecimal(0));
								if (sinpp > 0) {
									tienepag = true;
								}
							}
						}
					}
					int com_TF_TP = totalfacturado.compareTo(totalval);
					if (com_TF_TP >= 0) {
						costcenter.setSmspProjectstatus("INV");
						costcenter.setSmsccProjectstatusfield("INV");
						OBDal.getInstance().save(costcenter);
						OBDal.getInstance().flush();
						int com_TP_TF = totalPPpagado.compareTo(totalfacturado);
						if (com_TP_TF >= 0) {
							costcenter.setSmspProjectstatus("COLL");
							costcenter.setSmsccProjectstatusfield("COLL");
							OBDal.getInstance().save(costcenter);
							OBDal.getInstance().flush();
							/// Pago proveedor ///
							OBCriteria<Order> PedidoCompra = OBDal.getInstance().createCriteria(Order.class);
							PedidoCompra.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, false));
							PedidoCompra.add(Restrictions.eq(Order.PROPERTY_DOCUMENTSTATUS, "CO"));
							PedidoCompra.add(Restrictions.eq(Order.PROPERTY_COSTCENTER, costcenter));
							List<Order> ordComp = PedidoCompra.list();
							for (Order orden : ordComp) {

								if (orden.isSalesTransaction().equals(false)
										&& orden.getDocumentStatus().equals("CO")) {

									List<FIN_PaymentSchedule> fin_PaymentScheduleOrder = orden
											.getFINPaymentScheduleList();
									
									OBCriteria<Invoice> factPedidoCompra = OBDal.getInstance().createCriteria(Invoice.class);
									factPedidoCompra.add(Restrictions.eq(Invoice.PROPERTY_SALESTRANSACTION, false));
									factPedidoCompra.add(Restrictions.eq(Invoice.PROPERTY_DOCUMENTSTATUS, "CO"));
									factPedidoCompra.add(Restrictions.eq(Invoice.PROPERTY_COSTCENTER, costcenter));
									
									List<Invoice> facturaProveed = factPedidoCompra.list();

									for (Invoice InvoiceProvider : facturaProveed) {
										totalProviPagado = totalProviPagado.add(InvoiceProvider.getTotalPaid());
										totalProviPagar = totalProviPagar.add(InvoiceProvider.getGrandTotalAmount());
										
										if (totalProviPagado.compareTo(new BigDecimal(0)) > 0) {
											provetienepag = true;
										}
									}
									
//									for (FIN_PaymentSchedule fin_PaymentSchedule : fin_PaymentScheduleOrder) {
//										totalProviPagado = totalProviPagado.add(fin_PaymentSchedule.getPaidAmount());
//										totalProviPagar = totalProviPagar.add(fin_PaymentSchedule.getAmount());
//										
//										if (totalProviPagado.compareTo(new BigDecimal(0)) > 0) {
//											provetienepag = true;
//										}
//										
////										costcenter.setSmspIspaid(totalProviPagado);
////										OBDal.getInstance().save(costcenter);
////										OBDal.getInstance().flush();
//									}
									
									if (provetienepag == true) {
									int com_PP_TP = totalProviPagado.compareTo(totalProviPagar);
									if (com_PP_TP < 0) {
										costcenter.setSmspProjectstatus("COLLPPS");
										costcenter.setSmsccProjectstatusfield("COLLPPS");
										OBDal.getInstance().save(costcenter);
										OBDal.getInstance().flush();
									} else {
										costcenter.setSmspProjectstatus("COLLPS");
										costcenter.setSmsccProjectstatusfield("COLLPS");
										OBDal.getInstance().save(costcenter);
										OBDal.getInstance().flush();
									}
									}
								}
							} // Fin Pago proveedor //
						}
						if (com_TP_TF < 0 && tienepag == true) {
							costcenter.setSmspProjectstatus("PCOLL");
							costcenter.setSmsccProjectstatusfield("PCOLL");
							OBDal.getInstance().save(costcenter);
							OBDal.getInstance().flush();
						}
					}
					if (com_TF_TP < 0 && tienefac == true) {
						costcenter.setSmspProjectstatus("PINV");
						costcenter.setSmsccProjectstatusfield("PINV");
						OBDal.getInstance().save(costcenter);
						OBDal.getInstance().flush();
					}
				} // Fin edicion //
			}
			
		} catch (Exception e) {
			String mess = e.getMessage();
		}
	}

	@Override
	public void kill(ProcessBundle processBundle) throws Exception {
		// TODO Auto-generated method stub
		OBDal.getInstance().flush();
		this.killProcess = true;

	}

}
