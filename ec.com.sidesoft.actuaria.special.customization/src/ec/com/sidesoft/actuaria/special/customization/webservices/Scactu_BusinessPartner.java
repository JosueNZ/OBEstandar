package ec.com.sidesoft.actuaria.special.customization.webservices;

import java.io.Writer;
import java.math.BigDecimal;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.geography.Country;
import org.openbravo.model.common.geography.Region;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.web.WebService;

import com.sidesoft.localization.ecuador.withholdings.Taxpayer;

import ec.com.sidesoft.actuaria.special.customization.Scactu_Config;
import ec.com.sidesoft.actuaria.special.customization.Scactu_Log;
import ec.com.sidesoft.actuaria.special.customization.utils.Scactu_Helper;
import ec.com.sidesoft.localization.geography.secpm_canton;

public class Scactu_BusinessPartner implements WebService {
	private final Logger logger = Logger.getLogger(Scactu_BusinessPartner.class);

	@Override
	public void doPost(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = new JSONObject();
		String logId = null;
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin BusinessPartner doPost");

			JSONObject body = Scactu_Helper.readAllIntoJSONObject(request.getInputStream());
			logger.info(body.toString());

			Scactu_Log log = OBProvider.getInstance().get(Scactu_Log.class);
			log.setInterface(Scactu_Helper.getString(body, "interface", false));
			log.setEndpoint("createBusinessPartner");
			log.setJsonRequest(body.toString());
			OBDal.getInstance().save(log);
			OBDal.getInstance().flush();
			OBDal.getInstance().getConnection().commit();
			logId = log.getId();

			createBusinessPartner(conn, body);
			json = Scactu_Helper.getResponse(body);

			log.setJsonResponse(json.toString());
			log.setRecordID(body.getString("id"));
			log.setReferenceNo(body.getString("taxId"));
			log.setResult("OK");
			OBDal.getInstance().save(log);
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();
		} catch (Exception e) {
			OBDal.getInstance().rollbackAndClose();
			String message = Scactu_Helper.getErrorMessage(logger, e);
			logger.error(message);
			Scactu_Log log = OBDal.getInstance().get(Scactu_Log.class, logId);
			if (log != null) {
				log.setResult("ERROR");
				log.setError(message);
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				OBDal.getInstance().getConnection().commit();
			}
			json = Scactu_Helper.getErrorResponse(message);
		} finally {
			try {
				conn.getConnection().close();
				conn.destroy();
			} catch (Exception e) {
				Scactu_Helper.getErrorMessage(logger, e);
			}
			OBContext.restorePreviousMode();
			logger.info("Finish BusinessPartner doPost");
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		Writer w = response.getWriter();
		w.write(json.toString());
		w.close();
	}

	@Override
	public void doGet(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		JSONObject json = new JSONObject();

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin BusinessPartner doGet");

			String taxId = request.getParameter("taxId");
			if (StringUtils.isEmpty(taxId)) {
				throw new OBException("taxId is required");
			}

			JSONObject body = new JSONObject();
			getBusinessPartner(taxId.trim(), body);
			json = Scactu_Helper.getResponse(body);
		} catch (Exception e) {
			String message = Scactu_Helper.getErrorMessage(logger, e);
			logger.error(message);
			json = Scactu_Helper.getErrorResponse(message);
		} finally {
			OBContext.restorePreviousMode();
			logger.info("Finish BusinessPartner doGet");
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
		JSONObject json = new JSONObject();
		String logId = null;
		ConnectionProvider conn = new DalConnectionProvider(false);

		try {
			OBContext.setAdminMode(true);
			logger.info("Begin BusinessPartner doPut");

			JSONObject body = Scactu_Helper.readAllIntoJSONObject(request.getInputStream());
			logger.info(body.toString());

			Scactu_Log log = OBProvider.getInstance().get(Scactu_Log.class);
			log.setInterface(Scactu_Helper.getString(body, "interface", false));
			log.setEndpoint("updateBusinessPartner");
			log.setJsonRequest(body.toString());
			OBDal.getInstance().save(log);
			OBDal.getInstance().flush();
			OBDal.getInstance().getConnection().commit();
			logId = log.getId();

			updateBusinessPartner(conn, body);
			json = Scactu_Helper.getResponse(body);

			log.setJsonResponse(json.toString());
			log.setRecordID(body.getString("id"));
			log.setReferenceNo(body.getString("taxId"));
			log.setResult("OK");
			OBDal.getInstance().save(log);
			OBDal.getInstance().flush();
			OBDal.getInstance().commitAndClose();
		} catch (Exception e) {
			OBDal.getInstance().rollbackAndClose();
			String message = Scactu_Helper.getErrorMessage(logger, e);
			logger.error(message);
			Scactu_Log log = OBDal.getInstance().get(Scactu_Log.class, logId);
			if (log != null) {
				log.setResult("ERROR");
				log.setError(message);
				OBDal.getInstance().save(log);
				OBDal.getInstance().flush();
				OBDal.getInstance().getConnection().commit();
			}
			json = Scactu_Helper.getErrorResponse(message);
		} finally {
			try {
				conn.getConnection().close();
				conn.destroy();
			} catch (Exception e) {
				Scactu_Helper.getErrorMessage(logger, e);
			}
			OBContext.restorePreviousMode();
			logger.info("Finish BusinessPartner doPut");
		}

		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		Writer w = response.getWriter();
		w.write(json.toString());
		w.close();
	}

	@Override
	public void doDelete(String path, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
	}

	public BusinessPartner createBusinessPartner(ConnectionProvider conn, JSONObject body)
			throws Exception {
		String pInterface = Scactu_Helper.getString(body, "interface", true).toUpperCase();
		String taxId = Scactu_Helper.getString(body, "taxId", true);
		String taxIdType = Scactu_Helper.getString(body, "taxIdType", true);
		String fiscalName = Scactu_Helper.getString(body, "fiscalName", true);
		String commercialName = Scactu_Helper.getString(body, "commercialName", true);
		String email = Scactu_Helper.getString(body, "email", true);
		String typeTaxPayerId = Scactu_Helper.getString(body, "typeTaxPayerId", true);
		String mainAccount = Scactu_Helper.getString(body, "mainAccount", false);
		String url = Scactu_Helper.getString(body, "url", false);
		String activity = Scactu_Helper.getString(body, "activity", false);
		String categorization = Scactu_Helper.getString(body, "categorization", false);

		OBCriteria<Scactu_Config> qConfig = OBDal.getInstance().createCriteria(Scactu_Config.class);
		qConfig.add(Restrictions.eq(Scactu_Config.PROPERTY_ACTIVE, true));
		qConfig.addOrder(org.hibernate.criterion.Order.desc(Scactu_Config.PROPERTY_ORGANIZATION));
		qConfig.setMaxResults(1);
		if (qConfig.list().size() == 0) {
			throw new OBException("Configuration not found");
		}
		Scactu_Config config = qConfig.list().get(0);

		OBCriteria<BusinessPartner> qBusinessPartner = OBDal.getInstance()
				.createCriteria(BusinessPartner.class);
		qBusinessPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, taxId));
		qBusinessPartner.setMaxResults(1);
		if (qBusinessPartner.list().size() > 0) {
			throw new OBException(
					"Business partner with taxId [" + taxId + "] is already registered");
		}

		OBCriteria<Organization> qOrg = OBDal.getInstance().createCriteria(Organization.class);
		qOrg.add(Restrictions.eq(Organization.PROPERTY_ACTIVE, true));
		qOrg.add(Restrictions.eq(Organization.PROPERTY_SUMMARYLEVEL, true));
		qOrg.add(Restrictions.eq(Organization.PROPERTY_ALLOWPERIODCONTROL, true));
		qOrg.add(Restrictions.isNotNull(Organization.PROPERTY_GENERALLEDGER));
		qOrg.setMaxResults(1);
		Organization org = qOrg.list().get(0);

		BusinessPartner businessPartner = OBProvider.getInstance().get(BusinessPartner.class);
		businessPartner.setClient(org.getClient());
		businessPartner.setOrganization(org);
		businessPartner.setCustomer(true);
		businessPartner.setSearchKey(taxId);
		businessPartner.setTaxID(taxId);
		businessPartner.setBusinessPartnerCategory(config.getBusinessPartnerCategory());
		businessPartner.setPriceList(config.getPriceList());
		businessPartner.setPaymentMethod(config.getPaymentMethod());
		businessPartner.setPaymentTerms(config.getPaymentTerms());

		boolean isTaxIdValid = false;
		switch (taxIdType) {
		case "D":
			// isTaxIdValid = Scactu_ValidateTaxId.validarCedula(taxId);
			isTaxIdValid = taxId.length() == 10;
			break;
		case "R":
			// isTaxIdValid = Scactu_ValidateTaxId.validarRucPersonaNatural(taxId) ||
			// Scactu_ValidateTaxId.validarRucSociedadPrivada(taxId);
			isTaxIdValid = taxId.length() == 13;
			String suffix = taxId.substring(10, 13);
			if (!suffix.equals("001")) {
				String prefix = taxId.toString().substring(0, 10);
				businessPartner.setTaxID(prefix + "001");
			}
			break;
		case "P":
			isTaxIdValid = true;
			break;
		}
		if (!isTaxIdValid) {
			throw new OBException("taxId [" + taxId + "] is not valid");
		}
		businessPartner.setSswhTaxidtype(taxIdType);

		businessPartner.setName(fiscalName);
		businessPartner.setName2(commercialName);
		businessPartner.setEEIEeioice(true);
		businessPartner.setEEIEmail(email);
		businessPartner.setCreditLimit(new BigDecimal(0));
		businessPartner.setURL(url);

		OBCriteria<Taxpayer> qTaxPayer = OBDal.getInstance().createCriteria(Taxpayer.class);
		qTaxPayer.add(Restrictions.eq(Taxpayer.PROPERTY_ACTIVE, true));
		qTaxPayer.add(Restrictions.eq(Taxpayer.PROPERTY_ID, typeTaxPayerId));
		qTaxPayer.setMaxResults(1);
		if (qTaxPayer.list().size() == 0) {
			throw new OBException("typeTaxPayerId [" + typeTaxPayerId + "] not found");
		}
		businessPartner.setSSWHTaxpayer(qTaxPayer.list().get(0));

		businessPartner.setScactuMainAccount(mainAccount);
		businessPartner.setScactuActivity(activity);
		businessPartner.setScactuCategorization(categorization);

		JSONObject administrativeSection = body.has("administrativeSection")
				? body.getJSONObject("administrativeSection")
				: null;
		if (administrativeSection != null) {
			Long maxDayInvoice = Scactu_Helper.getLong(administrativeSection, "maxDayInvoice",
					false);
			String commercialTeam = Scactu_Helper.getString(administrativeSection, "commercialTeam",
					false);
			String operationsTeam = Scactu_Helper.getString(administrativeSection, "operationsTeam",
					false);
			String addBillingInfo = Scactu_Helper.getString(administrativeSection, "addBillingInfo",
					false);
			Boolean requiresPO = Scactu_Helper.getBoolean(administrativeSection, "requiresPO",
					true);
			Boolean isVIP = Scactu_Helper.getBoolean(administrativeSection, "isVIP", true);

			businessPartner.setScactuMaxDayInvoice(maxDayInvoice);
			businessPartner.setScactuCommercialTeam(commercialTeam);
			businessPartner.setScactuOperationsTeam(operationsTeam);
			businessPartner.setScactuAddBillingInfo(addBillingInfo);
			businessPartner.setScactuRequiresPo(requiresPO);
			businessPartner.setScactuIsVip(isVIP);
		}

		OBDal.getInstance().save(businessPartner);
		OBDal.getInstance().flush();

		body.put("id", businessPartner.getId());

		JSONArray addresses;
		try {
			addresses = body.has("addresses") ? body.getJSONArray("addresses") : new JSONArray();
		} catch (Exception e) {
			throw new OBException("addresses should be an array");
		}
		if (addresses.length() == 0) {
			throw new OBException("addresses is required");
		}

		for (int i = 0; i < addresses.length(); i++) {
			JSONObject address = addresses.getJSONObject(i);
			Location businessPartnerLocation = createBusinessPartnerLocation(address,
					businessPartner);
			address.put("id", businessPartnerLocation.getId());
			addresses.put(i, address);
		}
		body.put("addresses", addresses);

		JSONArray contactPersons;
		try {
			contactPersons = body.has("contactPersons") ? body.getJSONArray("contactPersons")
					: new JSONArray();
		} catch (Exception e) {
			throw new OBException("contactPersons should be an array");
		}

		for (int i = 0; i < contactPersons.length(); i++) {
			JSONObject contactPerson = contactPersons.getJSONObject(i);
			User user = createContactPerson(contactPerson, businessPartner);
			contactPerson.put("id", user.getId());
			contactPersons.put(i, contactPerson);
		}
		body.put("contactPersons", contactPersons);

		return businessPartner;
	}

	public BusinessPartner updateBusinessPartner(ConnectionProvider conn, JSONObject body)
			throws Exception {
		String pInterface = Scactu_Helper.getString(body, "interface", true).toUpperCase();
		String taxId = Scactu_Helper.getString(body, "taxId", true);
		String fiscalName = Scactu_Helper.getString(body, "fiscalName", true);
		String commercialName = Scactu_Helper.getString(body, "commercialName", true);
		String email = Scactu_Helper.getString(body, "email", true);
		String typeTaxPayerId = Scactu_Helper.getString(body, "typeTaxPayerId", true);
		String mainAccount = Scactu_Helper.getString(body, "mainAccount", false);
		String url = Scactu_Helper.getString(body, "url", false);
		String activity = Scactu_Helper.getString(body, "activity", false);
		String categorization = Scactu_Helper.getString(body, "categorization", false);

		OBCriteria<BusinessPartner> qBusinessPartner = OBDal.getInstance()
				.createCriteria(BusinessPartner.class);
		qBusinessPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, taxId));
		qBusinessPartner.setMaxResults(1);
		if (qBusinessPartner.list().size() == 0) {
			throw new OBException("Business partner taxId [" + taxId + "] not found");
		}
		BusinessPartner businessPartner = qBusinessPartner.list().get(0);
		body.put("id", businessPartner.getId());

		businessPartner.setCustomer(true);
		businessPartner.setName(fiscalName);
		businessPartner.setName2(commercialName);
		businessPartner.setEEIEeioice(true);
		businessPartner.setEEIEmail(email);
		businessPartner.setURL(url);

		OBCriteria<Taxpayer> qTaxPayer = OBDal.getInstance().createCriteria(Taxpayer.class);
		qTaxPayer.add(Restrictions.eq(Taxpayer.PROPERTY_ACTIVE, true));
		qTaxPayer.add(Restrictions.eq(Taxpayer.PROPERTY_ID, typeTaxPayerId));
		qTaxPayer.setMaxResults(1);
		if (qTaxPayer.list().size() == 0) {
			throw new OBException("typeTaxPayerId [" + typeTaxPayerId + "] not found");
		}
		businessPartner.setSSWHTaxpayer(qTaxPayer.list().get(0));

		businessPartner.setScactuMainAccount(mainAccount);
		businessPartner.setScactuActivity(activity);
		businessPartner.setScactuCategorization(categorization);

		JSONObject administrativeSection = body.has("administrativeSection")
				? body.getJSONObject("administrativeSection")
				: null;
		if (administrativeSection != null) {
			Long maxDayInvoice = Scactu_Helper.getLong(administrativeSection, "maxDayInvoice",
					false);
			String commercialTeam = Scactu_Helper.getString(administrativeSection, "commercialTeam",
					false);
			String operationsTeam = Scactu_Helper.getString(administrativeSection, "operationsTeam",
					false);
			String addBillingInfo = Scactu_Helper.getString(administrativeSection, "addBillingInfo",
					false);
			Boolean requiresPO = Scactu_Helper.getBoolean(administrativeSection, "requiresPO",
					true);
			Boolean isVIP = Scactu_Helper.getBoolean(administrativeSection, "isVIP", true);

			businessPartner.setScactuMaxDayInvoice(maxDayInvoice);
			businessPartner.setScactuCommercialTeam(commercialTeam);
			businessPartner.setScactuOperationsTeam(operationsTeam);
			businessPartner.setScactuAddBillingInfo(addBillingInfo);
			businessPartner.setScactuRequiresPo(requiresPO);
			businessPartner.setScactuIsVip(isVIP);
		}

		OBDal.getInstance().save(businessPartner);
		OBDal.getInstance().flush();

		JSONArray addresses;
		try {
			addresses = body.has("addresses") ? body.getJSONArray("addresses") : new JSONArray();
		} catch (Exception e) {
			throw new OBException("addresses should be an array");
		}

		for (int i = 0; i < addresses.length(); i++) {
			JSONObject address = addresses.getJSONObject(i);

			OBCriteria<Location> qBusinessPartnerLocation = OBDal.getInstance()
					.createCriteria(Location.class);
			qBusinessPartnerLocation
					.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, businessPartner));
			qBusinessPartnerLocation.add(Restrictions.eq(Location.PROPERTY_SCACTUINTERFACE, true));
			qBusinessPartnerLocation.setMaxResults(1);

			Location businessPartnerLocation = null;
			if (qBusinessPartnerLocation.list().size() == 0) {
				businessPartnerLocation = createBusinessPartnerLocation(address, businessPartner);
				address.put("id", businessPartnerLocation.getId());
				addresses.put(i, address);
			} else {
				businessPartnerLocation = updateBusinessPartnerLocation(address, businessPartner);
			}
		}
		body.put("addresses", addresses);

		JSONArray contactPersons;
		try {
			contactPersons = body.has("contactPersons") ? body.getJSONArray("contactPersons")
					: new JSONArray();
		} catch (Exception e) {
			throw new OBException("contactPersons should be an array");
		}

		// Eliminar o desactivar contactos
		OBCriteria<User> qUserList = OBDal.getInstance().createCriteria(User.class);
		qUserList.add(Restrictions.eq(User.PROPERTY_BUSINESSPARTNER, businessPartner));
		List<User> userList = qUserList.list();
		for (User user : userList) {
			JSONObject userFound = null;
			for (int i = 0; i < contactPersons.length(); i++) {
				JSONObject userSearch = contactPersons.getJSONObject(i);
				String userEmail = Scactu_Helper.getString(userSearch, "email", true);
				if (user.getEmail() != null
						&& user.getEmail().toLowerCase().trim().equals(userEmail.toLowerCase())) {
					userFound = userSearch;
					i = contactPersons.length();
				}
			}

			if (userFound == null) {
				user.setActive(false);
				OBDal.getInstance().save(user);
				OBDal.getInstance().flush();
			}
		}

		for (int i = 0; i < contactPersons.length(); i++) {
			JSONObject contactPerson = contactPersons.getJSONObject(i);
			String contactPersonEmail = Scactu_Helper.getString(contactPerson, "email", false);

			OBCriteria<User> qUser = OBDal.getInstance().createCriteria(User.class);
			qUser.add(Restrictions.eq(User.PROPERTY_BUSINESSPARTNER, businessPartner));
			qUser.add(Restrictions.sqlRestriction("UPPER(TRIM(email)) = ?",
					contactPersonEmail.toUpperCase(), StringType.INSTANCE));
			qUser.setMaxResults(1);
			qUser.setFilterOnActive(false);

			User user = null;
			if (qUser.list().size() == 0) {
				user = createContactPerson(contactPerson, businessPartner);
				contactPerson.put("id", user.getId());
				contactPersons.put(i, contactPerson);
			} else {
				updateContactPerson(contactPerson, businessPartner);
			}
		}
		body.put("contactPersons", contactPersons);

		return businessPartner;
	}

	private Location createBusinessPartnerLocation(JSONObject address,
			BusinessPartner businessPartner) throws Exception {
		String mainAddress = Scactu_Helper.getString(address, "address", true);
		String reference = Scactu_Helper.getString(address, "reference", false);
		String countryCode = Scactu_Helper.getString(address, "countryCode", true).toUpperCase();
		String province = Scactu_Helper.getString(address, "province", true);
		String canton = Scactu_Helper.getString(address, "canton", true);
		boolean isShipping = Scactu_Helper.getBoolean(address, "isShipping", true);
		boolean isBilling = Scactu_Helper.getBoolean(address, "isBilling", true);
		String phoneNumber = Scactu_Helper.getString(address, "phoneNumber", false);
		String alternativePhoneNumber = Scactu_Helper.getString(address, "alternativePhoneNumber",
				false);
		String cellPhoneNumber = Scactu_Helper.getString(address, "cellPhoneNumber", false);

		String address1 = null;
		String address2 = null;
		if (mainAddress.length() > 60) {
			address1 = mainAddress.substring(0, 60);
			address2 = mainAddress.substring(60,
					mainAddress.length() > 120 ? 120 : mainAddress.length());
		} else {
			address1 = mainAddress;
			address2 = reference;
		}

		org.openbravo.model.common.geography.Location location = OBProvider.getInstance()
				.get(org.openbravo.model.common.geography.Location.class);
		location.setClient(businessPartner.getClient());
		location.setOrganization(businessPartner.getOrganization());
		location.setAddressLine1(address1);
		location.setAddressLine2(address2);

		OBCriteria<Country> qCountry = OBDal.getInstance().createCriteria(Country.class);
		qCountry.add(Restrictions.eq(Country.PROPERTY_ACTIVE, true));
		qCountry.add(Restrictions.eq(Country.PROPERTY_ISOCOUNTRYCODE, countryCode));
		qCountry.setMaxResults(1);
		if (qCountry.list().size() == 0) {
			throw new OBException("countryCode [" + countryCode + "] not found");
		}
		location.setCountry(qCountry.list().get(0));

		if (province != null) {
			OBCriteria<Region> qRegion = OBDal.getInstance().createCriteria(Region.class);
			qRegion.add(Restrictions.eq(Region.PROPERTY_ACTIVE, true));
			qRegion.add(Restrictions.eq(Region.PROPERTY_SEARCHKEY, province));
			qRegion.setMaxResults(1);
			if (qRegion.list().size() == 0) {
				throw new OBException("province [" + province + "] not found");
			}
			location.setRegion(qRegion.list().get(0));
		}

		if (canton != null) {
			OBCriteria<secpm_canton> qCanton = OBDal.getInstance()
					.createCriteria(secpm_canton.class);
			qCanton.add(Restrictions.eq(secpm_canton.PROPERTY_ACTIVE, true));
			qCanton.add(Restrictions.eq(secpm_canton.PROPERTY_IDENTIFICADOR, canton));
			qCanton.setMaxResults(1);
			if (qCanton.list().size() == 0) {
				throw new OBException("canton [" + canton + "] not found");
			}
			secpm_canton _canton = qCanton.list().get(0);
			location.setSdinccCanton(_canton);
			location.setCityName(_canton.getName());
		}

		OBDal.getInstance().save(location);
		OBDal.getInstance().flush();

		Location businessPartnerLocation = OBProvider.getInstance().get(Location.class);
		businessPartnerLocation.setClient(businessPartner.getClient());
		businessPartnerLocation.setOrganization(businessPartner.getOrganization());
		businessPartnerLocation.setBusinessPartner(businessPartner);
		businessPartnerLocation.setLocationAddress(location);
		businessPartnerLocation.setName(address1);
		businessPartnerLocation.setShipToAddress(isShipping);
		businessPartnerLocation.setInvoiceToAddress(isBilling);
		businessPartnerLocation.setPhone(phoneNumber);
		businessPartnerLocation.setAlternativePhone(alternativePhoneNumber);
		businessPartnerLocation.setScactuCellphoneNumber(cellPhoneNumber);
		businessPartnerLocation.setScactuInterface(true);
		OBDal.getInstance().save(businessPartnerLocation);
		OBDal.getInstance().flush();

		return businessPartnerLocation;
	}

	private Location updateBusinessPartnerLocation(JSONObject address,
			BusinessPartner businessPartner) throws Exception {
		String mainAddress = Scactu_Helper.getString(address, "address", true);
		String reference = Scactu_Helper.getString(address, "reference", false);
		String countryCode = Scactu_Helper.getString(address, "countryCode", true).toUpperCase();
		String province = Scactu_Helper.getString(address, "province", true);
		String canton = Scactu_Helper.getString(address, "canton", true);
		boolean isShipping = Scactu_Helper.getBoolean(address, "isShipping", true);
		boolean isBilling = Scactu_Helper.getBoolean(address, "isBilling", true);
		String phoneNumber = Scactu_Helper.getString(address, "phoneNumber", false);
		String alternativePhoneNumber = Scactu_Helper.getString(address, "alternativePhoneNumber",
				false);
		String cellPhoneNumber = Scactu_Helper.getString(address, "cellPhoneNumber", false);

		String address1 = null;
		String address2 = null;
		if (mainAddress.length() > 60) {
			address1 = mainAddress.substring(0, 60);
			address2 = mainAddress.substring(60,
					mainAddress.length() > 120 ? 120 : mainAddress.length());
		} else {
			address1 = mainAddress;
			address2 = reference;
		}

		OBCriteria<Location> qBusinessPartnerLocation = OBDal.getInstance()
				.createCriteria(Location.class);
		qBusinessPartnerLocation
				.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, businessPartner));
		qBusinessPartnerLocation.add(Restrictions.eq(Location.PROPERTY_SCACTUINTERFACE, true));
		qBusinessPartnerLocation.setMaxResults(1);
		if (qBusinessPartnerLocation.list().size() == 0) {
			return createBusinessPartnerLocation(address, businessPartner);
		}
		Location businessPartnerLocation = qBusinessPartnerLocation.list().get(0);

		org.openbravo.model.common.geography.Location location = businessPartnerLocation
				.getLocationAddress();
		location.setAddressLine1(address1);
		location.setAddressLine2(address2);

		OBCriteria<Country> qCountry = OBDal.getInstance().createCriteria(Country.class);
		qCountry.add(Restrictions.eq(Country.PROPERTY_ACTIVE, true));
		qCountry.add(Restrictions.eq(Country.PROPERTY_ISOCOUNTRYCODE, countryCode));
		qCountry.setMaxResults(1);
		if (qCountry.list().size() == 0) {
			throw new OBException("countryCode [" + countryCode + "] not found");
		}
		location.setCountry(qCountry.list().get(0));

		if (province != null) {
			OBCriteria<Region> qRegion = OBDal.getInstance().createCriteria(Region.class);
			qRegion.add(Restrictions.eq(Region.PROPERTY_ACTIVE, true));
			qRegion.add(Restrictions.eq(Region.PROPERTY_SEARCHKEY, province));
			qRegion.setMaxResults(1);
			if (qRegion.list().size() == 0) {
				throw new OBException("province [" + province + "] not found");
			}
			location.setRegion(qRegion.list().get(0));
		}

		if (canton != null) {
			OBCriteria<secpm_canton> qCanton = OBDal.getInstance()
					.createCriteria(secpm_canton.class);
			qCanton.add(Restrictions.eq(secpm_canton.PROPERTY_ACTIVE, true));
			qCanton.add(Restrictions.eq(secpm_canton.PROPERTY_IDENTIFICADOR, canton));
			qCanton.setMaxResults(1);
			if (qCanton.list().size() == 0) {
				throw new OBException("canton [" + canton + "] not found");
			}
			secpm_canton _canton = qCanton.list().get(0);
			location.setSdinccCanton(_canton);
			location.setCityName(_canton.getName());
		}

		OBDal.getInstance().save(location);
		OBDal.getInstance().flush();

		businessPartnerLocation.setLocationAddress(location);
		businessPartnerLocation.setName(address1);
		businessPartnerLocation.setShipToAddress(isShipping);
		businessPartnerLocation.setInvoiceToAddress(isBilling);
		businessPartnerLocation.setPhone(phoneNumber);
		businessPartnerLocation.setAlternativePhone(alternativePhoneNumber);
		businessPartnerLocation.setScactuCellphoneNumber(cellPhoneNumber);

		OBDal.getInstance().save(businessPartnerLocation);
		OBDal.getInstance().flush();

		return businessPartnerLocation;
	}

	private User createContactPerson(JSONObject contactPerson, BusinessPartner businessPartner)
			throws Exception {
		String firstname = Scactu_Helper.getString(contactPerson, "firstname", true);
		String lastname = Scactu_Helper.getString(contactPerson, "lastname", false);
		lastname = lastname != null ? lastname : "";
		String email = Scactu_Helper.getString(contactPerson, "email", true);
		String alternativeEmail = Scactu_Helper.getString(contactPerson, "alternativeEmail", false);
		String phoneNumber = Scactu_Helper.getString(contactPerson, "phoneNumber", false);
		String extension = Scactu_Helper.getString(contactPerson, "extension", false);
		String alternativePhoneNumber = Scactu_Helper.getString(contactPerson,
				"alternativePhoneNumber", false);
		String position = Scactu_Helper.getString(contactPerson, "position", false);
		String comments = Scactu_Helper.getString(contactPerson, "comments", false);
		boolean isBilling = Scactu_Helper.getBoolean(contactPerson, "isBilling", true);
		boolean isCollection = Scactu_Helper.getBoolean(contactPerson, "isCollection", true);

		User user = OBProvider.getInstance().get(User.class);
		user.setBusinessPartner(businessPartner);
		user.setName(firstname + " " + lastname);
		user.setFirstName(firstname);
		user.setLastName(lastname);
		user.setEmail(email);
		user.setScactuAlternativeEmail(alternativeEmail);
		user.setPhone(phoneNumber);
		user.setScactuExtension(extension);
		user.setAlternativePhone(alternativePhoneNumber);
		user.setPosition(position);
		user.setComments(comments);
		user.setScactuIsBilling(isBilling);
		user.setScactuIsCollection(isCollection);

		OBDal.getInstance().save(user);
		OBDal.getInstance().flush();

		return user;
	}

	private User updateContactPerson(JSONObject contactPerson, BusinessPartner businessPartner)
			throws Exception {
		String firstname = Scactu_Helper.getString(contactPerson, "firstname", true);
		String lastname = Scactu_Helper.getString(contactPerson, "lastname", false);
		lastname = lastname != null ? lastname : "";
		String email = Scactu_Helper.getString(contactPerson, "email", true);
		String alternativeEmail = Scactu_Helper.getString(contactPerson, "alternativeEmail", false);
		String phoneNumber = Scactu_Helper.getString(contactPerson, "phoneNumber", false);
		String extension = Scactu_Helper.getString(contactPerson, "extension", false);
		String alternativePhoneNumber = Scactu_Helper.getString(contactPerson,
				"alternativePhoneNumber", false);
		String position = Scactu_Helper.getString(contactPerson, "position", false);
		String comments = Scactu_Helper.getString(contactPerson, "comments", false);
		boolean isBilling = Scactu_Helper.getBoolean(contactPerson, "isBilling", true);
		boolean isCollection = Scactu_Helper.getBoolean(contactPerson, "isCollection", true);

		OBCriteria<User> qUser = OBDal.getInstance().createCriteria(User.class);
		qUser.add(Restrictions.eq(User.PROPERTY_BUSINESSPARTNER, businessPartner));
		qUser.add(Restrictions.sqlRestriction("UPPER(TRIM(email)) = ?", email.toUpperCase(),
				StringType.INSTANCE));
		qUser.setMaxResults(1);
		qUser.setFilterOnActive(false);
		if (qUser.list().size() == 0) {
			return createContactPerson(contactPerson, businessPartner);
		}
		User user = qUser.list().get(0);

		user.setActive(true);
		user.setName(firstname + " " + lastname);
		user.setFirstName(firstname);
		user.setLastName(lastname);
		user.setEmail(email);
		user.setScactuAlternativeEmail(alternativeEmail);
		user.setPhone(phoneNumber);
		user.setScactuExtension(extension);
		user.setAlternativePhone(alternativePhoneNumber);
		user.setPosition(position);
		user.setComments(comments);
		user.setScactuIsBilling(isBilling);
		user.setScactuIsCollection(isCollection);

		OBDal.getInstance().save(user);
		OBDal.getInstance().flush();

		return user;
	}

	public void getBusinessPartner(String taxId, JSONObject body) throws Exception {
		OBCriteria<BusinessPartner> qBPartner = OBDal.getInstance()
				.createCriteria(BusinessPartner.class);
		qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_ACTIVE, true));
		qBPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, taxId.trim()));
		qBPartner.setMaxResults(1);
		if (qBPartner.list().size() == 0) {
			throw new OBException("taxId [" + taxId + "] not found");
		}
		BusinessPartner bPartner = qBPartner.list().get(0);

		body.put("id", bPartner.getId());
		body.put("taxId", bPartner.getSearchKey());
		body.put("taxIdType", bPartner.getSswhTaxidtype());
		body.put("fiscalName", bPartner.getName());
		body.put("commercialName", bPartner.getName2());
		body.put("email", bPartner.getEEIEmail() != null ? bPartner.getEEIEmail() : "");
		body.put("typeTaxPayerId",
				bPartner.getSSWHTaxpayer() != null ? bPartner.getSSWHTaxpayer().getId() : "");
		body.put("mainAccount",
				bPartner.getScactuMainAccount() != null ? bPartner.getScactuMainAccount() : "");
		body.put("url", bPartner.getURL() != null ? bPartner.getURL() : "");
		body.put("activity",
				bPartner.getScactuActivity() != null ? bPartner.getScactuActivity() : "");
		body.put("categorization",
				bPartner.getScactuCategorization() != null ? bPartner.getScactuCategorization()
						: "");

		JSONObject administrativeSection = new JSONObject();
		administrativeSection.put("maxDayInvoice",
				bPartner.getScactuMaxDayInvoice() != null ? bPartner.getScactuMaxDayInvoice() : "");
		administrativeSection.put("commercialTeam",
				bPartner.getScactuCommercialTeam() != null ? bPartner.getScactuCommercialTeam()
						: "");
		administrativeSection.put("operationsTeam",
				bPartner.getScactuOperationsTeam() != null ? bPartner.getScactuOperationsTeam()
						: "");
		administrativeSection.put("addBillingInfo",
				bPartner.getScactuAddBillingInfo() != null ? bPartner.getScactuAddBillingInfo()
						: "");
		administrativeSection.put("requiresPO", bPartner.isScactuRequiresPo());
		administrativeSection.put("isVIP", bPartner.isScactuIsVip());
		body.put("administrativeSection", administrativeSection);

		JSONArray addresses = new JSONArray();
		for (Location bPartnerLocation : bPartner.getBusinessPartnerLocationList()) {
			if (bPartnerLocation.isScactuInterface()) {
				org.openbravo.model.common.geography.Location location = bPartnerLocation
						.getLocationAddress();

				JSONObject address = new JSONObject();
				address.put("id", bPartnerLocation.getId());
				address.put("address",
						location.getAddressLine1() != null ? location.getAddressLine1() : "");
				address.put("reference",
						location.getAddressLine2() != null ? location.getAddressLine2() : "");
				address.put("countryCode",
						location.getCountry() != null ? location.getCountry().getISOCountryCode()
								: "");
				address.put("province",
						location.getRegion() != null ? location.getRegion().getSearchKey() : "");
				address.put("canton",
						location.getSdinccCanton() != null
								? location.getSdinccCanton().getIdentificador()
								: "");
				address.put("isShipping", bPartnerLocation.isShipToAddress());
				address.put("isBilling", bPartnerLocation.isInvoiceToAddress());
				address.put("phoneNumber",
						bPartnerLocation.getPhone() != null ? bPartnerLocation.getPhone() : "");
				address.put("alternativePhoneNumber",
						bPartnerLocation.getAlternativePhone() != null
								? bPartnerLocation.getAlternativePhone()
								: "");
				address.put("cellPhoneNumber",
						bPartnerLocation.getScactuCellphoneNumber() != null
								? bPartnerLocation.getScactuCellphoneNumber()
								: "");
				addresses.put(address);
			}
		}
		body.put("addresses", addresses);

		JSONArray contactPersons = new JSONArray();
		for (User user : bPartner.getADUserList()) {
			if (user.isActive()) {
				JSONObject contact = new JSONObject();
				contact.put("id", user.getId());
				contact.put("firstname", user.getFirstName() != null ? user.getFirstName() : "");
				contact.put("lastname", user.getLastName() != null ? user.getLastName() : "");
				contact.put("email", user.getEmail() != null ? user.getEmail() : "");
				contact.put("alternativeEmail",
						user.getScactuAlternativeEmail() != null ? user.getScactuAlternativeEmail()
								: "");
				contact.put("phoneNumber", user.getPhone() != null ? user.getPhone() : "");
				contact.put("extension",
						user.getScactuExtension() != null ? user.getScactuExtension() : "");
				contact.put("alternativePhoneNumber",
						user.getAlternativePhone() != null ? user.getAlternativePhone() : "");
				contact.put("position", user.getPosition() != null ? user.getPosition() : "");
				contact.put("comments", user.getComments() != null ? user.getComments() : "");
				contact.put("isBilling", user.isScactuIsBilling());
				contact.put("isCollection", user.isScactuIsCollection());
				contactPersons.put(contact);
			}
		}
		body.put("contactPersons", contactPersons);
	}
}
