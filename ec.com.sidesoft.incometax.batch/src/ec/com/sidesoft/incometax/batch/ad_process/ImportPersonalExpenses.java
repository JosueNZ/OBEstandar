package ec.com.sidesoft.incometax.batch.ad_process;

import java.math.BigDecimal;
import java.util.Date;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.idl.proc.Parameter;
import org.openbravo.idl.proc.Validator;
import org.openbravo.idl.proc.Value;
import com.sidesoft.hrm.payroll.Costemployee;
import com.sidesoft.hrm.payroll.CostEmployeeline;
import com.sidesoft.hrm.payroll.ssprcodeformulary107;

import net.sf.jasperreports.components.sort.SortElementUtils;

import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.openbravo.model.common.enterprise.OrganizationType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.service.db.DalConnectionProvider;

import org.openbravo.base.session.OBPropertiesProvider;

import org.openbravo.module.idljava.proc.IdlServiceJava;
import org.openbravo.service.db.DalConnectionProvider;

public class ImportPersonalExpenses extends IdlServiceJava {

  private Object object;
  private Object object2;

  public String getEntityName() {
    return "Simple Products";
  }

  public Parameter[] getParameters() {
    return new Parameter[] { new Parameter("Ejercicio", Parameter.STRING), // 0
        new Parameter("Fecha Inicio", Parameter.STRING), // 1
        new Parameter("Fecha Fin", Parameter.STRING), // 2
        new Parameter("Tercero", Parameter.STRING), // 3
        new Parameter("Gasto Deducible", Parameter.STRING), // 4
        new Parameter("Monto", Parameter.STRING), // 5
        new Parameter("Concepto Formulario", Parameter.STRING) };
  }

  protected Object[] validateProcess(Validator validator, String... values) throws Exception {

    validator.checkString(values[0], 120);
    validator.checkString(values[1], 120);
    validator.checkString(values[2], 120);
    validator.checkString(values[3], 120);
    validator.checkString(values[4], 120);
    validator.checkString(values[5], 120);
    validator.checkString(values[6], 120);

    return values;
  }

  public BaseOBObject internalProcess(Object... values) throws Exception {

    return createPersonalExpense((String) values[0], (String) values[1], (String) values[2],
        (String) values[3], (String) values[4], (String) values[5], (String) values[6]);
  }

  public BaseOBObject createPersonalExpense(final String Periodo, final String FechaInicio,
      final String FechaFin, final String Tercero, final String GastoD, final String Monto,
      final String ConceptoF) throws Exception, ParseException {

    Costemployee costEmployee = OBProvider.getInstance().get(Costemployee.class);
    Date start = new Date();
    Date end = new Date();
    boolean f_start = true;
    boolean f_end = true;

    // Validar Periodo existente
    Year ObjYear = findDALInstance(false, Year.class, new Value(Year.PROPERTY_FISCALYEAR, Periodo));
    if (ObjYear == null || ObjYear.equals("")) {
      throw new OBException("El Periodo: " + Periodo + " no existe.");
    }

    // Validar Tercero Existente
    OBCriteria<BusinessPartner> ObjsBusinessPartner = OBDal.getInstance()
        .createCriteria(BusinessPartner.class);
    ObjsBusinessPartner
        .add(Restrictions.or(Restrictions.eq(BusinessPartner.PROPERTY_SEARCHKEY, Tercero.trim()),
            Restrictions.eq(BusinessPartner.PROPERTY_TAXID, Tercero.trim())));
    ObjsBusinessPartner.setMaxResults(1);

    BusinessPartner partner = (BusinessPartner) ObjsBusinessPartner.uniqueResult();

    if (ObjsBusinessPartner.count() == 0) {
      throw new OBException("El tercero con el identificador: " + Tercero + " no existe.");
    }
    // Validar Impuesto Existente
    ssprcodeformulary107 ObjFormulary = findDALInstance(false, ssprcodeformulary107.class,
        new Value(ssprcodeformulary107.PROPERTY_VALUE, ConceptoF));
    if (ObjFormulary == null || ObjFormulary.equals("")) {
      throw new OBException("El Concepto de Formulario: " + ConceptoF + " no existe.");
    }

    SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
    formatter.setLenient(false);

    // Valida Fecha Inicio
    if (isValidDate(FechaInicio)) {
      Date dateStart = formatter.parse(FechaInicio);
      start = dateStart;
    } else {
      throw new OBException("La Fecha Inicio: " + FechaInicio + " no es válida (dd-mm-yyyy).");
    }
    // Valida Fecha Fin
    if (isValidDate(FechaFin)) {
      Date dateEnd = formatter.parse(FechaFin);
      end = dateEnd;
    } else {
      throw new OBException("La Fecha Fin: " + FechaFin + " no es válida (dd-mm-yyyy).");
    }

    OBCriteria<Costemployee> ObjsCostemployee = OBDal.getInstance()
        .createCriteria(Costemployee.class);
    ObjsCostemployee.add(Restrictions.eq(Costemployee.PROPERTY_YEAR, ObjYear));
    ObjsCostemployee.add(Restrictions.eq(Costemployee.PROPERTY_BUSINESSPARTNER, partner));
    ObjsCostemployee.add(Restrictions.eq(Costemployee.PROPERTY_STARTINGDATE, start));
    ObjsCostemployee.add(Restrictions.eq(Costemployee.PROPERTY_ENDINGDATE, end));
    ObjsCostemployee.setMaxResults(1);

    Costemployee attach = (Costemployee) ObjsCostemployee.uniqueResult();

    if (ObjsCostemployee.count() == 0) {
      // OrganizationType ObjOrganizationType
      OBCriteria<OrganizationType> ObjOrganizationType = OBDal.getInstance()
          .createCriteria(OrganizationType.class);
      ObjOrganizationType.add(Restrictions.eq(OrganizationType.PROPERTY_LEGALENTITY, true));
      ObjOrganizationType
          .add(Restrictions.eq(OrganizationType.PROPERTY_LEGALENTITYWITHACCOUNTING, true));
      ObjOrganizationType.setMaxResults(1);

      OrganizationType orgT = (OrganizationType) ObjOrganizationType.uniqueResult();

      // OrganizationType ObjOrganizationType
      OBCriteria<Organization> ObjOrganization = OBDal.getInstance()
          .createCriteria(Organization.class);
      ObjOrganization.add(Restrictions.eq(Organization.PROPERTY_ORGANIZATIONTYPE, orgT));
      ObjOrganization.setMaxResults(1);

      Organization org = (Organization) ObjOrganization.uniqueResult();

      try {
        // Setear organización por defecto
        costEmployee.setOrganization(org);
        // Setear periodo defecto
        costEmployee.setYear(ObjYear);
        // Setear fecha inicio
        costEmployee.setStartingDate(start);
        // Setear fecha fin
        costEmployee.setEndingDate(end);
        // Setear monto
        costEmployee.setAmountCost(new BigDecimal("0"));
        // Setear tercero
        costEmployee.setBusinessPartner(partner);

        OBDal.getInstance().save(costEmployee);
        OBDal.getInstance().flush();
        createPersonalExpenseLine(costEmployee, ObjFormulary, Monto, GastoD);
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      createPersonalExpenseLine(attach, ObjFormulary, Monto, GastoD);
    }
    OBDal.getInstance().commitAndClose();
    return costEmployee;
  }

  public void createPersonalExpenseLine(Costemployee costemployee, ssprcodeformulary107 formulary,
      String monto, String gastoD) throws Exception {
    CostEmployeeline costEmployeeLine = OBProvider.getInstance().get(CostEmployeeline.class);

    ConnectionProvider conn = new DalConnectionProvider(false);

    ReferentListData data[] = ReferentListData.select(conn, gastoD.trim().toUpperCase());

    if (data == null || data.length == 0) {
      throw new OBException("El gasto deducible: " + gastoD
          + " no es válido o no existe.\r\n Opciones: ALIMENTACION,EDUCACIÓN,SALUD,TURISMO,VESTIMENTA,VIVIENDA");
    }

    try {
      for (ReferentListData valueData : data) {
        // Setear organización por defecto
        costEmployeeLine.setOrganization(costemployee.getOrganization());
        // Setear periodo defecto
        costEmployeeLine.setCodeFormulary107(formulary);
        costEmployeeLine.setAmountDeductible(new BigDecimal(changeFormatBigDecimal(monto)));
        costEmployeeLine.setCostemployee(costemployee);
        costEmployeeLine.setDeductibleExpense(valueData.value);
        OBDal.getInstance().save(costEmployeeLine);
        OBDal.getInstance().flush();
        costemployee.setAmountCost(
            costemployee.getAmountCost().add(costEmployeeLine.getAmountDeductible()));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String changeFormatBigDecimal(String numbers) {
    String Remplace = "0";

    Remplace = numbers.replace(".", "");
    Remplace = numbers.replace(",", ".");

    return Remplace;
  }

  public static boolean isValidDate(String inDate) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
    dateFormat.setLenient(false);
    try {
      dateFormat.parse(inDate.trim());
    } catch (ParseException pe) {
      return false;
    }
    return true;
  }

}
