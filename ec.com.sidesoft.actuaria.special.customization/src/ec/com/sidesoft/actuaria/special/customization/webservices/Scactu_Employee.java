package ec.com.sidesoft.actuaria.special.customization.webservices;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.service.web.WebService;

import com.sidesoft.hrm.payroll.Contract;
import com.sidesoft.hrm.payroll.ContractPosition;
import com.sidesoft.hrm.payroll.Position;
import com.sidesoft.hrm.payroll.biometrical.SPRBIArea;

import ec.com.sidesoft.actuaria.special.customization.utils.Scactu_Helper;

public class Scactu_Employee implements WebService {
	private final Logger logger = Logger.getLogger(Scactu_Employee.class);

	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	@Override
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = new JSONObject();

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin Scactu_Employee doGet");

			String taxId = request.getParameter("taxId");
			if (StringUtils.isEmpty(taxId)) {
				throw new OBException("taxId is required");
			}

			JSONObject body = new JSONObject();
			getEmployee(taxId.trim(), body);
			json = Scactu_Helper.getResponse(body);
		} catch (Exception e) {
			OBDal.getInstance().rollbackAndClose();
			String message = Scactu_Helper.getErrorMessage(logger, e);
			logger.error(message);
			json = Scactu_Helper.getErrorResponse(message);
		} finally {
			OBContext.restorePreviousMode();
			logger.info("Finish Scactu_Employee doGet");
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		Writer w = response.getWriter();
		w.write(json.toString());
		w.close();
	}

	@Override
	public void doPut(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	public void getEmployee(String taxId, JSONObject body) throws Exception {
		OBCriteria<BusinessPartner> qBPartner = OBDal.getInstance()
				.createCriteria(BusinessPartner.class);
		qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
		qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_EMPLOYEE, true));
		qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SSPRSTATUS, "A"));
		qBPartner.add(Restrictions.sqlRestriction("UPPER(TRIM(taxid)) = ?", taxId.toUpperCase(),
				StringType.INSTANCE));
		qBPartner.setMaxResults(1);
		if (qBPartner.list().size() == 0) {
			throw new OBException("taxId [" + taxId + "] not found");
		}
		BusinessPartner bPartner = qBPartner.list().get(0);

		bPartner.setScactuIsParticipant(true);
		OBDal.getInstance().save(bPartner);
		OBDal.getInstance().flush();

		body.put("taxId", bPartner.getTaxID());
		body.put("firstName", bPartner.getSsprFirstname().toUpperCase());
		body.put("lastName", bPartner.getSsprLastname().toUpperCase());

		body.put("position", "");
		OBCriteria<Contract> qContract = OBDal.getInstance().createCriteria(Contract.class);
		qContract.add(Restrictions.eq(Contract.PROPERTY_ACTIVE, true));
		qContract.add(Restrictions.eq(Contract.PROPERTY_BUSINESSPARTNER, bPartner));
		qContract.addOrder(Order.desc(Contract.PROPERTY_ENDDATE));
		qContract.setMaxResults(1);
		if (qContract.list().size() > 0) {
			Contract contract = qContract.list().get(0);
			if (contract.getSSPRContractPositionList().size() > 0) {
				ContractPosition contractPosition = contract.getSSPRContractPositionList().get(0);
				Position position = contractPosition.getSsprPosition();
				if (position != null) {
					body.put("position", position.getName().toUpperCase());
				}
			}
		}

		body.put("area", "");
		if (bPartner.getSprbiArea() != null) {
			SPRBIArea area = bPartner.getSprbiArea();
			body.put("area", area.getName().toUpperCase());
		}

		body.put("unit", "");
		if (bPartner.getSsprCostcenter() != null) {
			Costcenter costCenter = bPartner.getSsprCostcenter();
			body.put("unit", costCenter.getName().toUpperCase());
		}

		body.put("salary", bPartner.getSsprCurrentsalary());
		body.put("dateAdmission", Scactu_Helper.getDateString(bPartner.getSSPREntrydate()));
		body.put("status", bPartner.getSSPRStatus());

		OBDal.getInstance().commitAndClose();
	}
}
