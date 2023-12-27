package ec.com.sidesoft.actuaria.special.customization.webservices;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.service.web.WebService;

import ec.com.sidesoft.actuaria.special.customization.utils.Scactu_Helper;

public class Scactu_ProductTax implements WebService {
	private final Logger logger = Logger.getLogger(Scactu_ProductTax.class);

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
			logger.info("Begin ProductTax doGet");

			String searchKey = request.getParameter("searchKey");
			if (StringUtils.isEmpty(searchKey)) {
				throw new OBException("searchKey is required");
			}

			JSONObject body = new JSONObject();
			getProductTax(searchKey.trim(), body);
			json = Scactu_Helper.getResponse(body);
		} catch (Exception e) {
			String message = Scactu_Helper.getErrorMessage(logger, e);
			logger.error(message);
			json = Scactu_Helper.getErrorResponse(message);
		} finally {
			OBContext.restorePreviousMode();
			logger.info("Finish ProductTax doGet");
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

	public void getProductTax(String searchKey, JSONObject body) throws Exception {
		OBCriteria<Product> qProduct = OBDal.getInstance().createCriteria(Product.class);
		qProduct.add(Restrictions.eq(Product.PROPERTY_ACTIVE, true));
		qProduct.add(Restrictions.sqlRestriction("UPPER(TRIM(value)) = ?", searchKey.toUpperCase(),
				StringType.INSTANCE));
		qProduct.setMaxResults(1);
		if (qProduct.list().size() == 0) {
			throw new OBException("searchKey [" + searchKey + "] not found");
		}
		Product product = qProduct.list().get(0);

		OBCriteria<TaxRate> qTax = OBDal.getInstance().createCriteria(TaxRate.class);
		qTax.add(Restrictions.eq(TaxRate.PROPERTY_ACTIVE, true));
		qTax.add(Restrictions.eq(TaxRate.PROPERTY_TAXCATEGORY, product.getTaxCategory()));
		qTax.add(Restrictions.eq(TaxRate.PROPERTY_DEFAULT, true));
		qTax.setMaxResults(1);
		if (qTax.list().size() == 0) {
			throw new OBException("Tax rate not found");
		}
		TaxRate tax = qTax.list().get(0);

		body.put("taxRate", tax.getRate());

	}
}
