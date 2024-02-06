package com.sidesoft.localization.ecuador.withholdings.create_xml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.hibernate.criterion.Restrictions;
import org.hibernate.type.StringType;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.UtilSql;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlEngine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sidesoft.localization.ecuador.withholdings.SSWHLivelihoodt;
import com.sidesoft.localization.ecuador.withholdings.SswhDcNote;
import com.sidesoft.localization.ecuador.withholdings.SswhInvoiceExportation;
import com.sidesoft.localization.ecuador.withholdings.SswhRptcOrgInform;
import com.sidesoft.localization.ecuador.withholdings.SswhRptcPurchaseDetail;
import com.sidesoft.localization.ecuador.withholdings.SswhRptcPurchasePaym;
import com.sidesoft.localization.ecuador.withholdings.SswhRptcPurchaseWith;
import com.sidesoft.localization.ecuador.withholdings.SswhRptcSalesByStabOrg;
import com.sidesoft.localization.ecuador.withholdings.SswhRptcSalesComp;
import com.sidesoft.localization.ecuador.withholdings.SswhRptcSalesPayForm;
import com.sidesoft.localization.ecuador.withholdings.SswhRptcSalesRefund;
import com.sidesoft.localization.ecuador.withholdings.SswhWithhCardCredit;
import com.sidesoft.localization.ecuador.withholdings.SswhWithholdingsVoided;

import com.sidesoft.localization.ecuador.withholdings.CsspidSalesTickets;

public class Create_xml extends DalBaseProcess {

  public XmlEngine xmlEngine = null;
  public static String strDireccion;
  public String StrCodigoCompra = "";
  public String Stridprov = "";
  public String StrPeriodId = "";
  public Element ventas=null;

  @SuppressWarnings({ "deprecation", "null" })
  public void doExecute(ProcessBundle bundle) throws Exception {

    final OBError message = new OBError();

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    // ConnectionProvider conn = new DalConnectionProvider(false);

    // ConnectionProvider conn = bundle.getConnection();

    org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
    CallableStatement callSQL = cp.getConnection()
        .prepareCall("{call SSWH_EXECUTE_ATS_SQL(?,?,?,?,?,?)}");

    OBContext.setAdminMode(true);

    // VariablesSecureApp varsAux = bundle.getContext().toVars();
    // HttpServletRequest request = RequestContext.get().getRequest();
    HttpServletResponse response = RequestContext.get().getResponse();

    try {

      // retrieve the parameters from the bundle
      // Recupera los parametros de la seción

      StrPeriodId = (String) bundle.getParams().get("cPeriodId");
      final String StrOrgID = (String) bundle.getParams().get("adOrgId");
      final String StrTypeATS = (String) bundle.getParams().get("atsType");
      final String StrNumberPeriod = (String) bundle.getParams().get("semester");
      final String Stryear = (String) bundle.getParams().get("cYearId");

      // Begin of the process for create XML
      // Inicio del proceso para crear el XML

      DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      try {
        docBuilder = docFactory.newDocumentBuilder();
      } catch (ParserConfigurationException e2) {
        // TODO Auto-generated catch block
        e2.printStackTrace();
      }

      // Search of period in the "table c_period" for getting of the
      // starting date and ending date
      // Busqueda del periodo en la tabla c_period para obtener la fecha
      // inicio y fin.

      Date StartDate = null;
      Date EndDate = null;
      Period SrchPeriod = null;
      Period SrchPeriod2 = null;
      Year dalyear;

      if (StrTypeATS.equals("M")) {

        if (StrPeriodId.length() == 0) {
          throw new OBException("Tipo ATS - Mensual. Seleccione el período para continuar.");
        } else {
          SrchPeriod = OBDal.getInstance().get(Period.class, StrPeriodId);
          SrchPeriod2 = OBDal.getInstance().get(Period.class, StrPeriodId);
          StrPeriodId = SrchPeriod2.getId();
        }

        StartDate = SrchPeriod.getStartingDate();
        EndDate = SrchPeriod.getEndingDate();
      }
      if (StrTypeATS.equals("S")) {

        if (Stryear.length() == 0) {
          throw new OBException("Tipo ATS - Semestral. Seleccione el Año para continuar.");
        } else {
          dalyear = OBDal.getInstance().get(Year.class, Stryear);
        }

        if (StrNumberPeriod.equals("1")) {

          OBCriteria<Period> ojbDateFrom = OBDal.getInstance().createCriteria(Period.class);
          ojbDateFrom.add(Restrictions.eq(Period.PROPERTY_PERIODNO, Long.valueOf("1")));
          ojbDateFrom.add(Restrictions.eq(Period.PROPERTY_YEAR, dalyear));

          OBCriteria<Period> ojbDateTo = OBDal.getInstance().createCriteria(Period.class);
          ojbDateTo.add(Restrictions.eq(Period.PROPERTY_PERIODNO, Long.valueOf("6")));
          ojbDateTo.add(Restrictions.eq(Period.PROPERTY_YEAR, dalyear));

          SrchPeriod = OBDal.getInstance().get(Period.class, (ojbDateFrom.list().get(0).getId()));
          SrchPeriod2 = OBDal.getInstance().get(Period.class, (ojbDateTo.list().get(0).getId()));
          StrPeriodId = SrchPeriod2.getId();
        } else if (StrNumberPeriod.equals("2")) {

          OBCriteria<Period> ojbDateFrom = OBDal.getInstance().createCriteria(Period.class);
          ojbDateFrom.add(Restrictions.eq(Period.PROPERTY_PERIODNO, Long.valueOf("6")));
          ojbDateFrom.add(Restrictions.eq(Period.PROPERTY_YEAR, dalyear));

          OBCriteria<Period> ojbDateTo = OBDal.getInstance().createCriteria(Period.class);
          ojbDateTo.add(Restrictions.eq(Period.PROPERTY_PERIODNO, Long.valueOf("12")));
          ojbDateTo.add(Restrictions.eq(Period.PROPERTY_YEAR, dalyear));

          SrchPeriod = OBDal.getInstance().get(Period.class, (ojbDateFrom.list().get(0).getId()));
          SrchPeriod2 = OBDal.getInstance().get(Period.class, (ojbDateTo.list().get(0).getId()));
          StrPeriodId = SrchPeriod2.getId();
        }

        StartDate = SrchPeriod.getStartingDate();
        EndDate = SrchPeriod2.getEndingDate();
      }

      final String strDate1 = "01-04-2019";
      final String strDate2 = "01-04-2019";

      SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
      final Date startDate = formatter.parse(strDate1);
      final Date endDate = formatter.parse(strDate2);

      StartDate = startDate;
      EndDate = endDate;

      // Para dar formato los números decimales
      DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
      simbolos.setDecimalSeparator('.');
      DecimalFormat formateador = new DecimalFormat("#########0.00", simbolos);

      SimpleDateFormat ecFormat = new SimpleDateFormat("dd/MM/yyyy");

      // Initialization of the XML document
      // Inicialización o preparación del documento XML

      Document doc = docBuilder.newDocument();
      doc.setXmlStandalone(true);

      // Tag main sales
      // Tag principal de las ventas
      Element rootElement = doc.createElement("iva");
      doc.appendChild(rootElement);

      // inicio

      String StrAdorgID = "";

      if (StrOrgID == null || StrOrgID.equals("")) {
        StrAdorgID = (String) bundle.getContext().getOrganization();
      } else {
        StrAdorgID = StrOrgID;
      }
      // Organization SrchOrg = OBDal.getInstance().get(Organization.class, StrAdorgID);

      String strProcessID = getUUID(cp);

      if (!cp.getRDBMS().equals("ORACLE")) {
        executeATSSql(callSQL, SrchPeriod.getId(), StrAdorgID, strProcessID, StrTypeATS,
            StrNumberPeriod, Stryear);
      } else {
        String strresult = processSaveTableTmp(cp, StrAdorgID, strProcessID, SrchPeriod.getId(),
            StrTypeATS, StrNumberPeriod, Stryear);
      }

      int countstabl = 0;
      double formvalor = 0;
      double SumSalesStab = 0;
      double AcumSalesStab = 0;
      String StrRuc = "";
      String StrTipoRuc = "";
      String Stranio = "";
      String StrMesp = "";
      int StrMes = 0;
      String StrnameOrg = "";
      String StrOrg = "";

      OBCriteria<SswhRptcOrgInform> ObjSampleForm = OBDal.getInstance()
          .createCriteria(SswhRptcOrgInform.class);
      ObjSampleForm.add(Restrictions.eq(SswhRptcOrgInform.PROPERTY_PROCESS, strProcessID));

      int intCountList = ObjSampleForm.list().size();

      SswhRptcOrgInform ObjOrgInfo = OBDal.getInstance().get(SswhRptcOrgInform.class, StrAdorgID);
      try {
        StrOrg = ObjSampleForm.list().get(0).getLegalName();
        StrRuc = ObjSampleForm.list().get(0).getIdentif();
        StrTipoRuc = ObjSampleForm.list().get(0).getTypeID();
      }

      catch (Exception e) {
      }

      if (StrTipoRuc == null || StrTipoRuc.equals("")) {

        throw new OBException(
            "El campo Tipo de identificación del informante(TipoIdInformante) es obligatorio");
      }

      // System.out.println("Tipo de identificación del informante(TipoIdInformante): ");

      if (StrRuc == null || StrRuc.equals("")) {

        throw new OBException(
            "El campo Número de Identificación del Informante(IdInformante) es obligatorio");
      } else if (StrRuc.length() > 13) {
        throw new OBException(
            "El campo Número de Identificación del Informante(IdInformante) ha excedido los 13 digitos permitidos.");
      }

      // System.out.println("Número de Identificación del Informante(IdInformante): "
      // + StrRuc);

      String StrMessage = "";
      if (StrOrg == null || StrOrg.equals("")) {

        throw new OBException("El campo Organización es obligatorio");
      } else if (StrOrg.length() < 5) {
        throw new OBException(
            "El campo Organización debe tener un mínimo de 5 letras que idenifiquen a la misma");
      } else if (StrOrg.length() >= 501) {
        throw new OBException("El campo Organización ha excedido los 500 caracteres permitidos.");
      } else {
        StrnameOrg = StrOrg == "" ? " " : StrOrg;
      }

      // System.out.println("Organización: " + StrnameOrg);

      Stranio = SrchPeriod.getYear().getFiscalYear();

      if (Stranio.equals("") || Stranio == null) {
        throw new OBException("El año es obligatorio");
      }

      // System.out.println("Anio: " + Stranio);

      StrMes = (SrchPeriod.getPeriodNo().intValue());

      if (Stranio == null) {
        throw new OBException("El mes es obligatorio");
      }

      // System.out.println("Mes: " + String.valueOf(StrMes));
      if (StrMes < 10) {
        StrMesp = "0" + String.valueOf(StrMes);
      } else {
        StrMesp = String.valueOf(StrMes);
        ;
      }

      if (StrTypeATS.equals("S")) {
        Organization SrchOrg = OBDal.getInstance().get(Organization.class, StrAdorgID);

        if (SrchOrg != null) {
          String strMicroBusiness = (SrchOrg.getEeiMicroBusiness() == null ? "ND"
              : SrchOrg.getEeiMicroBusiness());

          if (strMicroBusiness.equals("ND")) {
            throw new OBException("La organización debe estar configurada como Micro Empresa");
          } else {

            if (StrNumberPeriod.equals("1")) {
              StrMesp = "06";
            } else {
              StrMesp = "12";
            }
          }

        }

      }

      Element TipoIDInformante = doc.createElement("TipoIDInformante");
      TipoIDInformante.appendChild(doc.createTextNode(StrTipoRuc));
      rootElement.appendChild(TipoIDInformante);

      Element IdInformante = doc.createElement("IdInformante");
      IdInformante.appendChild(doc.createTextNode(StrRuc));
      rootElement.appendChild(IdInformante);

      Element razonSocial = doc.createElement("razonSocial");
      razonSocial.appendChild(doc.createTextNode(getOnlyStrings(StrnameOrg)));
      rootElement.appendChild(razonSocial);

      Element Anio = doc.createElement("Anio");
      Anio.appendChild(doc.createTextNode(Stranio));
      rootElement.appendChild(Anio);

      Element Mes = doc.createElement("Mes");
      Mes.appendChild(doc.createTextNode(StrMesp));
      rootElement.appendChild(Mes);

      if (StrTypeATS.equals("S")) {
        Element regimenMicroempresa = doc.createElement("regimenMicroempresa");
        regimenMicroempresa.appendChild(doc.createTextNode("SI"));
        rootElement.appendChild(regimenMicroempresa);
      }

      OBCriteria<SswhRptcSalesByStabOrg> ObjSalesByStabListDetCount = OBDal.getInstance()
          .createCriteria(SswhRptcSalesByStabOrg.class);
      ObjSalesByStabListDetCount
          .add(Restrictions.eq(SswhRptcSalesByStabOrg.PROPERTY_PROCESS, strProcessID));

      // ObjSalesByStabListDetCount.add(Restrictions.between("accountingDate", StartDate, EndDate));

      // if (!StrOrgID.isEmpty()) {
      // ObjSalesByStabListDetCount
      // .add(Restrictions.eq(SswhRptcSalesByStab.PROPERTY_ORGANIZATION, SrchOrg));
      // }

      // DESARROLLO AGRUPACION - FI

      // VARIABLES PARA VALIDAR ESTABLECIEMIENTOS - FI
      String v_count_est = "";
      String StrEstabAnt1 = "";
      String StrEstabAnt = "";
      String StrEstabNew = "";
      int countnum_est = 0;
      int countdata = 0;
      double AcumSalesStabNew = 0;
      double SumSalesStabNew = 0;
      double valueNew = 0;
      double formatvalorNew = 0;

      // CONSULTA XSQL PARA AGRUPACION POR ESTABLECIMIENTO - FI
      String v_stardate = "";
      String v_enddate = "";

      v_stardate = (StartDate != null) ? formatDate(StartDate) : "";
      v_enddate = (EndDate != null) ? formatDate(EndDate) : "";

      countdata = ObjSalesByStabListDetCount.list().size();

      // List<SswhRptcSalesByStab> dataStablCab = dataStablCab;

      for (SswhRptcSalesByStabOrg ListSalesStab : ObjSalesByStabListDetCount.list()) {
        // countnum_est++;

        if (countdata != 0) {
          if (countnum_est == 0) {
            try {
              StrEstabAnt = ListSalesStab.getEstablecimiento();
            } catch (Exception e) {
            }
            countnum_est++;
          } else {
            try {
              StrEstabNew = ListSalesStab.getEstablecimiento();
            } catch (Exception e) {
            }
            if (StrEstabAnt != StrEstabNew) {
              StrEstabAnt = StrEstabNew;
              countnum_est++;
            }
          }
        }
        try {
          valueNew = Double.valueOf(ListSalesStab.getValor().toString());
        } catch (Exception e) {
        }
        formatvalorNew = Double.valueOf(valueNew);
        String ValorNew = "";
        ValorNew = formateador.format(formatvalorNew).toString() == null ? "0"
            : formateador.format(formatvalorNew);
        SumSalesStabNew = Double.valueOf(ValorNew);
        AcumSalesStabNew = AcumSalesStabNew + SumSalesStabNew;

        v_count_est = /* "00" *+ */String.valueOf(countnum_est);
        // FIN CONSULTA XSQL PARA AGRUPACION POR ESTABLECIMIENTO - FI
      }
      // FIN DESARROLLO AGRUPACION - FI

      List<SswhRptcSalesByStabOrg> SalesbyStabListCount = ObjSalesByStabListDetCount.list();

      int CountRecordSalesbystabCount = SalesbyStabListCount.size();
      if (CountRecordSalesbystabCount > 0) {

        for (SswhRptcSalesByStabOrg ListDetSalesStabdet : ObjSalesByStabListDetCount.list()) {
          countstabl++;

          String valuet = "";
          try {
            valuet = ListDetSalesStabdet.getValor().toString();
          } catch (Exception e) {
          }
          formvalor = Double.valueOf(valuet);
          String Valor1 = "";
          Valor1 = formateador.format(formvalor).toString() == null ? "0"
              : formateador.format(formvalor);
          SumSalesStab = Double.valueOf(Valor1);
          AcumSalesStab = AcumSalesStab + SumSalesStab;

        }

        // String NumEstab = res;//COMENTADO POR VALIDACION DE NUEMEROS
        // DE ESTABLECIMIENTO - FI
        String NumEstab = String.valueOf("00" + String.valueOf(v_count_est));

        /*
         * COMENTADO POR AGRUPACION DE ESTABLECIMIENTO - FI String Gtotal =
         * formateador.format(AcumSalesStab).toString() == null ? "0.00" :
         * formateador.format(AcumSalesStab);
         */
        String Gtotal = formateador.format(AcumSalesStabNew).toString() == null ? "0.00"
            : formateador.format(AcumSalesStabNew);

        if (NumEstab.equals("") || NumEstab == null) {
          throw new OBException(
              "Número de Establecimientos del sujeto pasivo inscritos en el RUC(numEstabRuc) es obligatorio");
        }
        // System.out.println("Número de Establecimientos del sujeto
        // pasivo inscritos en el
        // RUC(numEstabRuc): "
        // + NumEstab);

        if (Gtotal.equals("") || Gtotal == null) {
          throw new OBException(
              "Total venta reportadas en el período informado(totalVentas) es obligatorio");
        }
        // System.out.println("Total venta reportadas en el período informado(totalVentas): "
        // +
        // Gtotal);

        Element numEstabRuc = doc.createElement("numEstabRuc");
        numEstabRuc.appendChild(doc.createTextNode(NumEstab));
        rootElement.appendChild(numEstabRuc);

        Element totalVentas = doc.createElement("totalVentas");
        totalVentas.appendChild(doc.createTextNode(Gtotal));
        rootElement.appendChild(totalVentas);

        Element codigoOperativo = doc.createElement("codigoOperativo");
        codigoOperativo.appendChild(doc.createTextNode("IVA"));
        rootElement.appendChild(codigoOperativo);
      } else {
        Element numEstabRuc = doc.createElement("numEstabRuc");
        numEstabRuc.appendChild(doc.createTextNode("001"));
        rootElement.appendChild(numEstabRuc);

        Element totalVentas = doc.createElement("totalVentas");
        totalVentas.appendChild(doc.createTextNode("0.00"));
        rootElement.appendChild(totalVentas);

        Element codigoOperativo = doc.createElement("codigoOperativo");
        codigoOperativo.appendChild(doc.createTextNode("IVA"));
        rootElement.appendChild(codigoOperativo);
      }

      //
      // Second Tag of the Purchase detail

      OBCriteria<SswhRptcPurchaseDetail> ObjPurchaseDetList = OBDal.getInstance()
          .createCriteria(SswhRptcPurchaseDetail.class);
      ObjPurchaseDetList
          .add(Restrictions.eq(SswhRptcPurchaseDetail.PROPERTY_PROCESS, strProcessID));
      /*
       * ObjPurchaseDetList.add(Restrictions.between("fechaRegistro", StartDate, EndDate)); if
       * (!StrOrgID.isEmpty()) {
       * 
       * ObjPurchaseDetList .add(Restrictions.eq(SswhRptcPurchaseDetail.PROPERTY_ORGANIZATION,
       * SrchOrg)); }
       */

      List<SswhRptcPurchaseDetail> PurchasesList = ObjPurchaseDetList.list();
      int CountPurchase = PurchasesList.size();
      if (CountPurchase > 0) {
        Element compras = doc.createElement("compras");
        rootElement.appendChild(compras);

        // Element CabecerasCompras =
        // doc.createElement("cabecerasCompras");
        // compras.appendChild(CabecerasCompras);

        for (SswhRptcPurchaseDetail ListDetPurchase : ObjPurchaseDetList.list()) {
          StrCodigoCompra = "";
          String strCInvoiceID = "";
          String StrCodCmp = "";
          String StrcodSustento = "";
          String StrtpIdProv = "";
          String StridProv = "";
          String StrtipoComprobante = "";
          String StrtipoProv = "";
          String Strtipoprov = "";
          String StrdenoProv = "";
          String Strdenoprov = "";
          String StrparteRel = "";
          String Strparterel = "";
          String StrfechaRegistro = "";
          String Strestablecimiento = "";
          String StrpuntoEmision = "";
          String Strsecuencial = "";
          String StrfechaEmision = "";
          String Strautorizacion = "";
          String Strestablecimiento2 = "";
          String Straut = "";
          String strreg = "";
          String StrEst = "";
          String StrEst2 = "";
          String StrCods = "";
          String Strtidprov = "";
          Stridprov = "";
          String StrComp = "";
          String strsec = "";
          String strfeche = "";
          String strpunt = "";
          String strbasenogravaiva = "";
          String StrfechaEmiRet1 = "";
          String paisfecpago = "";
          String strptoretem1 = "";
          String StrptoEmiRetencion1 = "";
          String strsecret = "";
          String StrsecRetencion1 = "";
          String strautret = "";
          String StrautRetencion1 = "";
          String pagolocext = "";
          String tiporegi = "";
          String paisefecpagogen = "";
          String paisefecpagoparfis = "";
          String denopagoregfis = "";
          String strformp = "";
          String StrformaPago = "";
          String aplicconvdobtrib = "";
          String strfecheret = "";
          String Strpagolocext = "";
          String Strtiporegi = "";
          String Strpaisefecpagogen = "";
          String Strpaisefecpagoparfis = "";
          String Strdenopagoregfis = "";
          String strpaisefecpago = "";
          String straplicconvdobtrib = "";
          String strpagExtSujRetNorLeg = "";
          String strpagextsj = "";
          String strregimenfiscal = "";
          Date ddate;
          String strisrefung = "";

          double basenogravaiva = 0;
          double baseimponible = 0;
          double baseimpgrav = 0;
          double montoice = 0;
          double montoiva = 0;
          double valorretbienes = 0;
          double valorretserv = 0;
          double valretserv100 = 0;
          double baseexenta = 0;
          double baseimponibletotales = 0;
          double valorret10 = 0;
          double valorret20 = 0;
          double valorret50 = 0;

          // Detalle de Compras

          Element detalleCompras = doc.createElement("detalleCompras");
          compras.appendChild(detalleCompras);

          strCInvoiceID = ListDetPurchase.getInvoice().getId();

          try {

            StrCodCmp = ListDetPurchase.getCodigoCompra();
            StrCods = ListDetPurchase.getCODSustento();
            Strtidprov = ListDetPurchase.getTipoIdentificador();
            Stridprov = ListDetPurchase.getNumeroIdent();
            StrComp = ListDetPurchase.getTipoComprobante();

            OBCriteria<SSWHLivelihoodt> ObjIsRefundList = OBDal.getInstance() // CREAR VISTA -
                                                                              // PENDIENTE
                .createCriteria(SSWHLivelihoodt.class);
            ObjIsRefundList.add(Restrictions.eq(SSWHLivelihoodt.PROPERTY_SEARCHKEY, StrComp));
            /*
             * if (!StrOrgID.isEmpty()) {
             * 
             * ObjIsRefundList.add(Restrictions.eq(SSWHLivelihoodt.PROPERTY_ORGANIZATION, SrchOrg));
             * }
             */

            List<SSWHLivelihoodt> IsRefundList = ObjIsRefundList.list();

            strregimenfiscal = ListDetPurchase.getFiscalregime();
            Strtipoprov = ListDetPurchase.getTipoProveedor();
            Strdenoprov = ListDetPurchase.getFiscalname();
            Strparterel = ListDetPurchase.getParteRelacionada();
            ddate = ListDetPurchase.getFechaRegistro();
            strreg = ecFormat.format(ddate);
            StrEst = ListDetPurchase.getEstablecimiento();
            strpunt = ListDetPurchase.getPuntoEmision();
            strsec = ListDetPurchase.getSecuencia();
            ddate = ListDetPurchase.getFechaEmision();
            strfeche = ecFormat.format(ddate);
            Straut = ListDetPurchase.getAutorizacion();
            Strpagolocext = ListDetPurchase.getPagoLocal();
            Strtiporegi = ListDetPurchase.getTipoRegimen();
            Strpaisefecpagogen = ListDetPurchase.getPaisPagoGeneral();
            Strpaisefecpagoparfis = ListDetPurchase.getPaisPagoParaiso();
            Strdenopagoregfis = ListDetPurchase.getDenoPagoRegfis();
            strpaisefecpago = ListDetPurchase.getPaisPago();
            straplicconvdobtrib = ListDetPurchase.getConvenioTrib();
            strpagextsj = ListDetPurchase.getNormaLegal();
            strformp = ListDetPurchase.getCodigoCompra();
            basenogravaiva = Math.abs(ListDetPurchase.getBaseNoIva().doubleValue());
            baseimponible = Math.abs(ListDetPurchase.getBaseIvaCero().doubleValue());
            baseimpgrav = Math.abs(ListDetPurchase.getBaseIvaDoce().doubleValue());
            montoice = Math.abs(ListDetPurchase.getMontoIce().doubleValue());
            montoiva = Math.abs(ListDetPurchase.getMontoIva().doubleValue());
            valorretbienes = Math.abs(ListDetPurchase.getRETIva30().doubleValue());
            valorretserv = Math.abs(ListDetPurchase.getRETIva70().doubleValue());
            valretserv100 = Math.abs(ListDetPurchase.getRETIva100().doubleValue());
            Strestablecimiento2 = ListDetPurchase.getEstablecimientoRet();
            strptoretem1 = ListDetPurchase.getPuntoEmisionRet();
            strsecret = ListDetPurchase.getSecuenciaRet();
            strautret = ListDetPurchase.getAutoriRetencion();
            ddate = ListDetPurchase.getFechaEmisionRet();
            strfecheret = ecFormat.format(ddate);
            baseexenta = ListDetPurchase.getBaseExcenta().doubleValue();
            valorret10 = ListDetPurchase.getRETIva10().doubleValue();
            valorret20 = ListDetPurchase.getRETIva20().doubleValue();
            valorret50 = ListDetPurchase.getRETIva50().doubleValue();

            if (IsRefundList.get(0).isRefund()) {
              baseimponibletotales = Math.abs(ListDetPurchase.getBaseNoIva().doubleValue()
                  + ListDetPurchase.getBaseIvaCero().doubleValue()
                  + ListDetPurchase.getBaseIvaDoce().doubleValue()
                  + ListDetPurchase.getBaseExcenta().doubleValue());
            } else {
              baseimponibletotales = 0.00;
            }

          } catch (Exception e) {
          }
          StrCodigoCompra = StrCodCmp == null ? " " : StrCodCmp;

          if (StrCods == null || StrCods.equals("")) {
            throw new OBException("Identificación del sustento tributario(codSustento)");
          }
          // System.out.println("Identificación del sustento tributario(codSustento): "
          // + StrCods);

          if (!(StrCods == null || StrCods.equals(""))) {
            while (StrCods.length() < 2) {
              StrCods = "0".concat(StrCods);
            }
          }
          StrcodSustento = StrCods == null ? " " : StrCods;
          Element codSustento = doc.createElement("codSustento");
          codSustento.appendChild(doc.createTextNode(StrcodSustento));
          detalleCompras.appendChild(codSustento);

          if (Strtidprov == null || Strtidprov.equals("")) {
            throw new OBException("Tipo de Identificación del Proveedor(tpIdProv)");
          }
          // System.out.println("Tipo de Identificación del Proveedor(tpIdProv): "
          // + Strtidprov);

          StrtpIdProv = Strtidprov == null ? " " : Strtidprov;
          Element tpIdProv = doc.createElement("tpIdProv");
          tpIdProv.appendChild(doc.createTextNode(StrtpIdProv));
          detalleCompras.appendChild(tpIdProv);

          if (Stridprov == null || Stridprov.equals("")) {

            throw new OBException(
                "El campo No. de Identificación del Proveedor(idProv) es obligatorio");
          } else if (Stridprov.length() < 3) {
            throw new OBException(
                "El campo No. de Identificación del Proveedor(idProv) debe tener un mínimo de 3 letras que idenifiquen a la misma");
          } else if (Stridprov.length() >= 14) {
            throw new OBException(
                "El campo No. de Identificación del Proveedor(idProv) ha excedido los 13 caracteres permitidos.");
          }
          // System.out.println("El campo No. de Identificación del Proveedor(idProv): "
          // +
          // Stridprov);

          StridProv = Stridprov == null ? " " : Stridprov;
          Element idProv = doc.createElement("idProv");
          idProv.appendChild(doc.createTextNode(StridProv));
          detalleCompras.appendChild(idProv);

          if (StrComp == null || StrComp.equals("")) {
            throw new OBException("Código tipo de comprobante(tipoComprobante) es obligatorio");
          }
          // System.out.println("Código tipo de comprobante(tipoComprobante): "
          // + StrComp);

          StrtipoComprobante = StrComp == null ? " " : StrComp;
          Element tipoComprobante = doc.createElement("tipoComprobante");
          tipoComprobante.appendChild(doc.createTextNode(StrtipoComprobante));
          detalleCompras.appendChild(tipoComprobante);

          // if (StrComp == null || StrComp.equals("")) {
          // throw new
          // OBException("Código tipo de comprobante(tipoComprobante) es obligatorio");
          // }
          // System.out.println("Tipo Proveedor(tipoProveedor): " +
          // Strtipoprov);

          if (StrtpIdProv.trim().equals("03")) {

            StrtipoProv = Strtipoprov == null ? " " : Strtipoprov;
            Element tipoProv = doc.createElement("tipoProv");
            tipoProv.appendChild(doc.createTextNode(StrtipoProv));
            detalleCompras.appendChild(tipoProv);

            StrdenoProv = Strdenoprov == null ? " " : Strdenoprov;
            Element denoProv = doc.createElement("denoProv");
            denoProv.appendChild(doc.createTextNode(getOnlyStrings(StrdenoProv)));
            detalleCompras.appendChild(denoProv);
          }

          StrparteRel = Strparterel == null ? " " : Strparterel;
          Element parteRel = doc.createElement("parteRel");
          parteRel.appendChild(doc.createTextNode(StrparteRel));
          detalleCompras.appendChild(parteRel);

          if (strreg == null || strreg.equals("")) {
            throw new OBException(
                "Fecha de registro contable del comprobante de venta(fechaRegistro) es obligatorio");
          }
          // System.out.println("Fecha de registro contable del
          // comprobante de venta(fechaRegistro):
          // "+
          // strreg);

          StrfechaRegistro = strreg == null ? " " : strreg;
          Element fechaRegistro = doc.createElement("fechaRegistro");
          fechaRegistro.appendChild(doc.createTextNode(StrfechaRegistro));
          detalleCompras.appendChild(fechaRegistro);

          if (StrEst == null || StrEst.equals("")) {
            throw new OBException(
                "No. de serie del comprobante de venta – establecimiento(establecimiento) es obligatorio");
          }
          // System.out.println("No. de serie del comprobante de venta
          // –
          // establecimiento(establecimiento): "
          // + StrEst);

          Strestablecimiento = StrEst == null ? " " : StrEst;
          Element establecimiento = doc.createElement("establecimiento");
          establecimiento.appendChild(doc.createTextNode(Strestablecimiento));
          detalleCompras.appendChild(establecimiento);

          if (strpunt == null || strpunt.equals("")) {
            throw new OBException(
                "No. de serie del comprobante de venta - punto de emisión(puntoEmision) es obligatorio");
          }
          // System.out.println("No. de serie del comprobante de venta
          // - punto de
          // emisión(puntoEmision): "
          // + strpunt);

          StrpuntoEmision = strpunt == null ? " " : strpunt;
          Element puntoEmision = doc.createElement("puntoEmision");
          puntoEmision.appendChild(doc.createTextNode(StrpuntoEmision));
          detalleCompras.appendChild(puntoEmision);

          if (strsec == null || strsec.equals("")) {
            throw new OBException(
                "No. de serie del comprobante de venta - punto de emisión(secuencial) es obligatorio");
          }
          // //System.out.println("No. de serie del comprobante de
          // venta - punto de
          // emisión(puntoEmision): "
          // + strpunt);

          Strsecuencial = strsec == null ? " " : strsec;
          Element secuencial = doc.createElement("secuencial");
          secuencial.appendChild(doc.createTextNode(Strsecuencial));
          detalleCompras.appendChild(secuencial);

          if (strfeche == null || strfeche.equals("")) {
            throw new OBException(
                "Fecha de emisión del comprobante de venta(fechaEmision) es obligatorio");
          }

          StrfechaEmision = strfeche == null ? " " : strfeche;
          Element fechaEmision = doc.createElement("fechaEmision");
          fechaEmision.appendChild(doc.createTextNode(StrfechaEmision));
          detalleCompras.appendChild(fechaEmision);

          if (Straut == null || Straut.equals("")) {
            throw new OBException(
                "No. de autorización del comprobante de venta(autorización) es obligatorio");
          }

          Strautorizacion = Straut == null ? " " : Straut;
          Element autorizacion = doc.createElement("autorizacion");
          autorizacion.appendChild(doc.createTextNode(Strautorizacion));
          detalleCompras.appendChild(autorizacion);

          strbasenogravaiva = formateador.format(basenogravaiva).toString() == null ? " "
              : formateador.format(basenogravaiva);
          if (strbasenogravaiva == null || strbasenogravaiva.equals(" ")) {
            throw new OBException("Base Imponible No objeto de IVA(baseNoGraIva) es obligatorio");
          }

          Element baseNoGraIva = doc.createElement("baseNoGraIva");
          baseNoGraIva.appendChild(doc.createTextNode(strbasenogravaiva));
          detalleCompras.appendChild(baseNoGraIva);

          String strbaseImponible = "";
          strbaseImponible = formateador.format(baseimponible).toString() == null ? " "
              : formateador.format(baseimponible);
          if (strbaseImponible == null || strbaseImponible.equals(" ")) {
            throw new OBException("Base Imponible tarifa 0% IVA(baseImponible) es obligatorio");
          }

          Element baseImponible = doc.createElement("baseImponible");
          baseImponible.appendChild(doc.createTextNode(strbaseImponible));
          detalleCompras.appendChild(baseImponible);

          String strbaseImpGrav = "";
          strbaseImpGrav = formateador.format(baseimpgrav).toString() == null ? " "
              : formateador.format(baseimpgrav);
          if (strbaseImpGrav == null || strbaseImpGrav.equals(" ")) {
            throw new OBException(
                "Base Imponible tarifa IVA diferente de 0%(baseImpGrav) es obligatorio");
          }

          Element baseImpGrav = doc.createElement("baseImpGrav");
          baseImpGrav.appendChild(doc.createTextNode(strbaseImpGrav));
          detalleCompras.appendChild(baseImpGrav);

          // CAMBIOS NUEVO ANEXO 2015 - (FI)
          String strBaseExenta = "";
          strBaseExenta = formateador.format(baseexenta).toString() == null ? " "
              : formateador.format(baseexenta);
          if (strBaseExenta == null || strBaseExenta.equals(" ")) {
            throw new OBException("Base Exenta es obligatorio");
          }

          Element baseImpExe = doc.createElement("baseImpExe");
          baseImpExe.appendChild(doc.createTextNode(strBaseExenta));
          detalleCompras.appendChild(baseImpExe);
          // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)

          String strmontoice = "";
          strmontoice = formateador.format(montoice).toString() == null ? " "
              : formateador.format(montoice);
          if (strmontoice == null || strmontoice.equals(" ")) {
            throw new OBException("Monto ICE(baseImpGrav) es obligatorio");
          }

          Element montoIce = doc.createElement("montoIce");
          montoIce.appendChild(doc.createTextNode(strmontoice));
          detalleCompras.appendChild(montoIce);

          String strmontoiva = "";
          strmontoiva = formateador.format(montoiva).toString() == null ? " "
              : formateador.format(montoiva).toString();
          if (strmontoiva == null || strmontoiva.equals(" ")) {
            throw new OBException("Monto IVA(montoIva) es obligatorio");
          }

          Element montoIva = doc.createElement("montoIva");
          montoIva.appendChild(doc.createTextNode(strmontoiva));
          detalleCompras.appendChild(montoIva);

          // CAMBIOS NUEVO ANEXO 2015 - (FI)
          String strvalorret10 = "";
          strvalorret10 = formateador.format(valorret10).toString() == null ? " "
              : formateador.format(valorret10);
          if (strvalorret10 == null || strvalorret10.equals(" ")) {
            throw new OBException("Retencion 10 % (valRetBien10) es obligatorio");
          }

          Element valRetBien10 = doc.createElement("valRetBien10");
          valRetBien10.appendChild(doc.createTextNode(strvalorret10));
          detalleCompras.appendChild(valRetBien10);

          String strvalorret20 = "";
          strvalorret20 = formateador.format(valorret20).toString() == null ? " "
              : formateador.format(valorret20);
          if (strvalorret20 == null || strvalorret20.equals(" ")) {
            throw new OBException("Retencion 20 % (valRetServ20) es obligatorio");
          }

          Element valRetServ20 = doc.createElement("valRetServ20");
          valRetServ20.appendChild(doc.createTextNode(strvalorret20));
          detalleCompras.appendChild(valRetServ20);
          // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)

          String strvalorretbienes = "";
          strvalorretbienes = formateador.format(valorretbienes).toString() == null ? " "
              : formateador.format(valorretbienes);

          if (strvalorretbienes == null || strvalorretbienes.equals(" ")) {
            throw new OBException("Retención IVA Bienes(valorRetBienes) es obligatorio");
          }

          Element valorRetBienes = doc.createElement("valorRetBienes");
          valorRetBienes.appendChild(doc.createTextNode(strvalorretbienes));
          detalleCompras.appendChild(valorRetBienes);

          String strvalorret50 = "";
          strvalorret50 = formateador.format(valorret50).toString() == null ? " "
              : formateador.format(valorret50);
          if (strvalorret50 == null || strvalorret50.equals(" ")) {
            throw new OBException("Retencion 50 % (valRetServ50) es obligatorio");
          }

          Element valRetServ50 = doc.createElement("valRetServ50");
          valRetServ50.appendChild(doc.createTextNode(strvalorret50));
          detalleCompras.appendChild(valRetServ50);

          String strvalorRetServicios = "";
          strvalorRetServicios = formateador.format(valorretserv).toString() == null ? " "
              : formateador.format(valorretserv);
          if (strvalorRetServicios == null || strvalorRetServicios.equals(" ")) {
            throw new OBException("Retención IVA Servicios(valorRetServicios) es obligatorio");
          }

          Element valorRetServicios = doc.createElement("valorRetServicios");
          valorRetServicios.appendChild(doc.createTextNode(strvalorRetServicios));
          detalleCompras.appendChild(valorRetServicios);

          String strvalretserv100 = "";
          strvalretserv100 = formateador.format(valretserv100).toString() == null ? " "
              : formateador.format(valretserv100);
          if (strvalorRetServicios == null || strvalorRetServicios.equals(" ")) {
            throw new OBException("Retención IVA 100%(valRetServ100) es obligatorio");
          }

          Element valRetServ100 = doc.createElement("valRetServ100");
          valRetServ100.appendChild(doc.createTextNode(strvalretserv100));
          detalleCompras.appendChild(valRetServ100);

          // CAMBIOS NUEVO ANEXO 2015 - (FI)

          // BASES IMPONIBLES REEMBOLSOS
          OBCriteria<SswhRptcSalesRefund> ObjRefundList2 = OBDal.getInstance()
              .createCriteria(SswhRptcSalesRefund.class);
          Invoice invoice2 = OBDal.getInstance().get(Invoice.class, strCInvoiceID);

          ObjRefundList2.add(Restrictions.eq(SswhRptcSalesRefund.PROPERTY_INVOICE, invoice2));
          ObjRefundList2.add(Restrictions.eq(SswhRptcSalesRefund.PROPERTY_PROCESS, strProcessID));

          List<SswhRptcSalesRefund> PurchasesRefList2 = ObjRefundList2.list();
          int CountPurchasePaym2 = PurchasesRefList2.size();
          double bdTotalReembolsos = 0;
          if (CountPurchasePaym2 > 0) {
            for (SswhRptcSalesRefund SearchDocument : PurchasesRefList2) {

              bdTotalReembolsos = bdTotalReembolsos
                  + (SearchDocument.getBaseImpGrabReem() == null ? 0
                      : SearchDocument.getBaseImpGrabReem().doubleValue())
                  + (SearchDocument.getBaseNoGrabIva() == null ? 0
                      : SearchDocument.getBaseNoGrabIva().doubleValue())
                  + (SearchDocument.getBaseImpGrabada() == null ? 0
                      : SearchDocument.getBaseImpGrabada().doubleValue())
                  + (SearchDocument.getBaseExenta() == null ? 0
                      : SearchDocument.getBaseExenta().doubleValue());

            }
          }

          String strTotalImpBasesRefund = "";
          strTotalImpBasesRefund = formateador.format(bdTotalReembolsos).toString() == null ? "0.00"
              : formateador.format(bdTotalReembolsos);

          if (strTotalImpBasesRefund == null || strTotalImpBasesRefund.equals(" ")) {
            throw new OBException("Total Bases Imponibles(totBasesImpReemb) es obligatorio");
          }

          Element totBasesImpReemb = doc.createElement("totbasesImpReemb");
          totBasesImpReemb.appendChild(doc.createTextNode(strTotalImpBasesRefund));
          detalleCompras.appendChild(totBasesImpReemb);
          // FIN BASES IMPONIBLES REEMBOLSOS

          // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)

          // Detalle de Pago Exterior

          Element pagoExterior = doc.createElement("pagoExterior");
          detalleCompras.appendChild(pagoExterior);

          // pagoLocExt

          if (Strpagolocext == null || Strpagolocext.equals("")) {
            throw new OBException("Pago Local o al Exterior(pagoLocExt) es obligatorio");
          }
          pagolocext = Strpagolocext == null ? " " : Strpagolocext;
          Element pagoLocExt = doc.createElement("pagoLocExt");
          pagoLocExt.appendChild(doc.createTextNode(pagolocext));
          pagoExterior.appendChild(pagoLocExt);

          if (Strtiporegi != null && !Strtiporegi.equals("")) {
            tiporegi = Strtiporegi == null ? " " : Strtiporegi;
            Element tipoRegi = doc.createElement("tipoRegi");
            tipoRegi.appendChild(doc.createTextNode(tiporegi));
            pagoExterior.appendChild(tipoRegi);

            if (tiporegi.trim().equals("01")) {
              if (Strpaisefecpagogen == null || Strpaisefecpagogen.equals("")) {
                throw new OBException(
                    "Código del país al que se efectúa el pago general(paisEfecPagoGen) es obligatorio");
              } else {
                paisefecpagogen = Strpaisefecpagogen == null ? " " : Strpaisefecpagogen;
                Element paisEfecPagoGen = doc.createElement("paisEfecPagoGen");
                paisEfecPagoGen.appendChild(doc.createTextNode(paisefecpagogen));
                pagoExterior.appendChild(paisEfecPagoGen);
              }
            }

            if (tiporegi.trim().equals("02")) {
              if (Strpaisefecpagoparfis == null || Strpaisefecpagoparfis.equals("")) {
                throw new OBException(
                    "Código del país al que se efectúa el pago paraíso fiscal(paisEfecPagoParFis) es obligatorio");
              } else {
                paisefecpagoparfis = Strpaisefecpagoparfis == null ? " " : Strpaisefecpagoparfis;
                Element paisEfecPagoParFis = doc.createElement("paisEfecPagoParFis");
                paisEfecPagoParFis.appendChild(doc.createTextNode(paisefecpagoparfis));
                pagoExterior.appendChild(paisEfecPagoParFis);
              }
            }

            if (tiporegi.trim().equals("03")) {
              if (Strdenopagoregfis == null || Strdenopagoregfis.equals("")) {
                throw new OBException(
                    "Denominación del país al que se efectúa el pago jurisdicción de menor imposición(denopagoRegFis) es obligatorio");
              } else {
                denopagoregfis = Strdenopagoregfis == null ? " " : Strdenopagoregfis;
                Element denopagoRegFis = doc.createElement("denopagoRegFis");
                denopagoRegFis.appendChild(doc.createTextNode(denopagoregfis));
                pagoExterior.appendChild(denopagoRegFis);
              }
            }
          }

          if (strpaisefecpago == null || strpaisefecpago.equals("")) {
            throw new OBException("País al que se Efectúa el Pago(paisEfecPago) es obligatorio");
          }
          paisfecpago = strpaisefecpago == null ? " " : strpaisefecpago;
          Element paisEfecPago = doc.createElement("paisEfecPago");
          paisEfecPago.appendChild(doc.createTextNode(paisfecpago));
          pagoExterior.appendChild(paisEfecPago);

          if (straplicconvdobtrib == null || straplicconvdobtrib.equals("")) {
            throw new OBException(
                "Aplica Convenio de Doble Tributación en el pago(aplicConvDobTrib) es obligatorio");
          }
          aplicconvdobtrib = straplicconvdobtrib == null ? " " : straplicconvdobtrib;
          Element aplicConvDobTrib = doc.createElement("aplicConvDobTrib");
          aplicConvDobTrib.appendChild(doc.createTextNode(aplicconvdobtrib));
          pagoExterior.appendChild(aplicConvDobTrib);

          if (strpagextsj == null || strpagextsj.equals("")) {
            throw new OBException(
                "Pago al exterior sujeto a retención en aplicación a la norma legal(pagExtSujRetNorLeg) es obligatorio");
          }
          strpagExtSujRetNorLeg = strpagextsj == null ? " " : strpagextsj;
          Element pagExtSujRetNorLeg = doc.createElement("pagExtSujRetNorLeg");
          pagExtSujRetNorLeg.appendChild(doc.createTextNode(strpagExtSujRetNorLeg));
          pagoExterior.appendChild(pagExtSujRetNorLeg);

          // CAMBIOS NUEVO ANEXO 2015 - (FI)

          String SrtPagoRegFis = strregimenfiscal == null ? "NA" : strregimenfiscal;
          String StrTmpPagoFis = null;

          if (SrtPagoRegFis.equals("NA")) {
            StrTmpPagoFis = "NA";
          } else {
            StrTmpPagoFis = strregimenfiscal;
          }

          // Thread.currentThread();
          // Thread.sleep(300L);

          // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)

          // DG. Close purchase
          // DG. 14-05-2015
          // Start new development payment form

          try {

            OBCriteria<SswhRptcPurchasePaym> ObjPurchasePaymentList = OBDal.getInstance()
                .createCriteria(SswhRptcPurchasePaym.class);
            // ObjPurchasePaymentList.add(Restrictions.between("accountingDate",
            // StartDate,
            // EndDate));
            ObjPurchasePaymentList
                .add(Restrictions.eq(SswhRptcPurchasePaym.PROPERTY_DOCUMENTNO, StrCodigoCompra));

            ObjPurchasePaymentList
                .add(Restrictions.eq(SswhRptcPurchasePaym.PROPERTY_PROCESS, strProcessID));

            ObjPurchasePaymentList
                .add(Restrictions.eq(SswhRptcPurchasePaym.PROPERTY_TAXID, Stridprov));

            /*
             * if (!StrOrgID.isEmpty()) {
             * 
             * ObjPurchasePaymentList
             * .add(Restrictions.eq(SswhRptcPurchasePaym.PROPERTY_ORGANIZATION, SrchOrg)); }
             */

            List<SswhRptcPurchasePaym> PurchasesPaymList = ObjPurchasePaymentList.list();
            int CountPurchasePaym1 = PurchasesPaymList.size();

            if (CountPurchasePaym1 > 0) {

              Element formasDePago = doc.createElement("formasDePago");
              detalleCompras.appendChild(formasDePago);

              for (SswhRptcPurchasePaym SearchDocument : ObjPurchasePaymentList.list()) {

                Element formaPagos = doc.createElement("formaPago");
                formasDePago.appendChild(formaPagos);

                String srtdocumentnonew = "";
                String strformpaynew = "";
                String StrformaPagonew = "";
                String Strcomparedocnew = ""; // SearchDocument.getDocumentNo();
                // if (Strcomparedoc.equals(strformp))
                // {
                try {
                  strformpaynew = SearchDocument.getFormaPago();
                  srtdocumentnonew = SearchDocument.getDocumentNo();
                } catch (Exception e) {
                }

                if (strformpaynew == null || strformpaynew.equals("")) {
                  throw new OBException("Formas de Pago(formaPago) es obligatorio");
                }
                Strcomparedocnew = srtdocumentnonew == null ? " " : srtdocumentnonew;
                /*
                 * Element documentno = doc.createElement("codCompra"); documentno.appendChild
                 * (doc.createTextNode(Strcomparedocnew)); formaPagos.appendChild(documentno);
                 */

                StrformaPagonew = strformpaynew == null ? " " : strformpaynew;
                // Element formaPago =
                // doc.createElement("forma");
                // formaPago.appendChild(doc.createTextNode(StrformaPagonew));
                formaPagos.appendChild(doc.createTextNode(StrformaPagonew));
                // }
              }
            }
          } catch (Exception e) {
          } // Hasta aqui desarrollo de formas de pago

          // Inica desarrollo de retenciones

          OBCriteria<SswhRptcPurchaseWith> ObjSswhRptcPurchaseWith = OBDal.getInstance()
              .createCriteria(SswhRptcPurchaseWith.class);
          // ObjSswhRptcPurchaseWith.add(Restrictions.between("invoiceDate",
          // StartDate, EndDate));

          ObjSswhRptcPurchaseWith
              .add(Restrictions.eq(SswhRptcPurchaseWith.PROPERTY_DOCUMENTNO, StrCodigoCompra));
          ObjSswhRptcPurchaseWith
              .add(Restrictions.eq(SswhRptcPurchaseWith.PROPERTY_PROCESS, strProcessID));

          ObjSswhRptcPurchaseWith
              .add(Restrictions.eq(SswhRptcPurchaseWith.PROPERTY_TAXID, Stridprov));

          /*
           * if (!StrOrgID.isEmpty()) {
           * 
           * ObjSswhRptcPurchaseWith
           * .add(Restrictions.eq(SswhRptcPurchaseWith.PROPERTY_ORGANIZATION, SrchOrg)); }
           */

          List<SswhRptcPurchaseWith> SswhRptcPurchaseWith = ObjSswhRptcPurchaseWith.list();
          int CountWithholding = SswhRptcPurchaseWith.size();

          if (CountWithholding > 0) {

            Element air = doc.createElement("air");
            detalleCompras.appendChild(air);

            for (SswhRptcPurchaseWith SswhRptcPurchaseWithList : ObjSswhRptcPurchaseWith.list()) {

              Element detalleAir = doc.createElement("detalleAir");
              air.appendChild(detalleAir);

              String Strcodcompranew = "";
              String Strcodretairnew = "";
              String Strcodcompranew1 = "";
              String Strcodretairnew1 = "";

              double basImpAirnew = 0;
              double porcentajeAirnew = 0;
              double valRetAirnew = 0;
              String StrbasImpAirnew1 = "";
              String StrbasImpAirnew2 = "";
              String StrporcentajeAirnew1 = "";
              String StrporcentajeAirnew2 = "";
              String StrvalRetAirnew1 = "";
              String StrvalRetAirnew2 = "";
              String StrfechaPagoDiv = "";
              String StrimRentaSoc = "";
              String StranioUtDiv = "";
              String StrnumCajBan = "";
              String StrprecCajBan = "";

              try {

                Strcodcompranew = SswhRptcPurchaseWithList.getDocumentNo();
                Strcodretairnew = SswhRptcPurchaseWithList.getCodigoRet();
                basImpAirnew = SswhRptcPurchaseWithList.getBaseImp().doubleValue();
                porcentajeAirnew = SswhRptcPurchaseWithList.getPorcenRet().doubleValue();
                valRetAirnew = SswhRptcPurchaseWithList.getValorRet().doubleValue();

              } catch (Exception e) {
              }

              /*
               * Strcodcompranew1 = Strcodcompranew == null ? " " : Strcodcompranew; Element
               * documentno = doc.createElement("codCompra"); documentno.appendChild
               * (doc.createTextNode(Strcodcompranew1)); detalleAir.appendChild(documentno);
               */

              Strcodretairnew1 = Strcodretairnew == null ? " " : Strcodretairnew;
              Element codigoretencion = doc.createElement("codRetAir");
              codigoretencion.appendChild(doc.createTextNode(Strcodretairnew1));
              detalleAir.appendChild(codigoretencion);

              StrbasImpAirnew1 = formateador.format(basImpAirnew);
              StrbasImpAirnew2 = StrbasImpAirnew1 == null ? "0.00" : StrbasImpAirnew1;
              Element basImpAir = doc.createElement("baseImpAir");
              basImpAir.appendChild(doc.createTextNode(StrbasImpAirnew2));
              detalleAir.appendChild(basImpAir);

              StrporcentajeAirnew1 = formateador.format(porcentajeAirnew);
              StrporcentajeAirnew2 = StrporcentajeAirnew1 == null ? "0.00" : StrporcentajeAirnew1;
              Element porcentajeAir = doc.createElement("porcentajeAir");
              porcentajeAir.appendChild(doc.createTextNode(StrporcentajeAirnew2));
              detalleAir.appendChild(porcentajeAir);

              StrvalRetAirnew1 = formateador.format(valRetAirnew);
              StrvalRetAirnew2 = StrvalRetAirnew1 == null ? "0.00" : StrvalRetAirnew1;
              Element valRetAir = doc.createElement("valRetAir");
              valRetAir.appendChild(doc.createTextNode(StrvalRetAirnew2));
              detalleAir.appendChild(valRetAir);

              // CAMBIOS NUEVO ANEXO 2015 - (FI)
              if (!StrfechaPagoDiv.equals("")) {
                Element fechaPagoDiv = doc.createElement("fechaPagoDiv");
                fechaPagoDiv.appendChild(doc.createTextNode(StrfechaPagoDiv));
                detalleAir.appendChild(fechaPagoDiv);
              }

              if (!StrimRentaSoc.equals("")) {
                Element imRentaSoc = doc.createElement("imRentaSoc");
                imRentaSoc.appendChild(doc.createTextNode(StrimRentaSoc));
                detalleAir.appendChild(imRentaSoc);
              }

              if (!StranioUtDiv.equals("")) {
                Element anioUtDiv = doc.createElement("anioUtDiv");
                anioUtDiv.appendChild(doc.createTextNode(StranioUtDiv));
                detalleAir.appendChild(anioUtDiv);
              }

              if (!StrnumCajBan.equals("")) {
                Element numCajBan = doc.createElement("numCajBan");
                numCajBan.appendChild(doc.createTextNode(StrnumCajBan));
                detalleAir.appendChild(numCajBan);
              }

              if (!StrprecCajBan.equals("")) {
                Element precCajBan = doc.createElement("precCajBan");
                precCajBan.appendChild(doc.createTextNode(StrprecCajBan));
                detalleAir.appendChild(precCajBan);
              }
              // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)

            }
          }

          // DG. 14 -05-2015
          // Finish new development payment form

          // ***INICIO REEMBOLSOS
          OBCriteria<SswhRptcSalesRefund> ObjRefundList = OBDal.getInstance()
              .createCriteria(SswhRptcSalesRefund.class);
          // ObjRefundList.add(Restrictions.between("accountingDate", StartDate, EndDate));

          Invoice invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceID);

          ObjRefundList.add(Restrictions.eq(SswhRptcSalesRefund.PROPERTY_INVOICE, invoice));
          ObjRefundList.add(Restrictions.eq(SswhRptcSalesRefund.PROPERTY_PROCESS, strProcessID));

          /*
           * if (!StrOrgID.isEmpty()) {
           * 
           * ObjRefundList.add(Restrictions.eq(SswhRptcSalesRefund.PROPERTY_ORGANIZATION, SrchOrg));
           * }
           */

          List<SswhRptcSalesRefund> PurchasesRefList = ObjRefundList.list();
          int CountPurchasePaym = PurchasesRefList.size();
          int aux = 0;
          if (CountPurchasePaym > 0) {
            Element reembolsos = doc.createElement("reembolsos");
            detalleCompras.appendChild(reembolsos);
            for (SswhRptcSalesRefund SearchDocument : ObjRefundList.list()) {
              String Strcomparedoc = SearchDocument.getCodigoCompra();
              // Tag for the lines refund

              aux++;
              // if (Strcomparedoc.equals(strformp)) {

              Element reembolso = doc.createElement("reembolso");
              reembolsos.appendChild(reembolso);
              String StrCodCompra = "";
              String strcodcompra = "";
              String StrtipoComprobanteReemb = "";
              String strtipocomprobantereemb = "";
              String StrtpIdProvReemb = "";
              String strtpidprobreemb = "";
              String StridProvReemb = "";
              String stridprovreemb = "";
              String StrestablecimientoReemb = "";
              String strestablecimientoreemb = "";
              String StrpuntoEmisionReemb = "";
              String strpuntoemisionreem = "";
              String StrsecuencialReemb = "";
              String strsecuencialreemb = "";
              String StrfechaEmisionReemb = "";
              String strfechaemisionreemb = "";
              String StrautorizacionReemb = "";
              String strautorizacionreemb = "";
              String StrbaseImponibleReemb = "";
              String strbaseimponiblereemb = "";
              String StrbaseImpGravReemb = "";
              String strbaseimpgravreemb = "";
              String StrbaseNoGraIvaReemb = "";
              String strbasenograivareemb = "";
              String StrmontoIceRemb = "";
              String strmontoiceremb = "";
              String StrmontoIvaRemb = "";
              String strmontoivaremb = "";
              Date strdate;
              String strbaseexentareemb = "";

              double baseimponiblereemb = 0;
              double baseimpgravreemb = 0;
              double basenograivareemb = 0;
              double montoiceremb = 0;
              double montoivaremb = 0;
              double baseexentareemb = 0;
              try {

                strcodcompra = SearchDocument.getCodigoCompra();
                strtipocomprobantereemb = SearchDocument.getTipoCompReemb();
                strtpidprobreemb = SearchDocument.getTipoIdentificador();
                stridprovreemb = SearchDocument.getIdentificadorProveedor();
                strestablecimientoreemb = SearchDocument.getEstablecimiento();
                strpuntoemisionreem = SearchDocument.getPuntoEmision();
                strsecuencialreemb = SearchDocument.getSecuencial();
                strdate = SearchDocument.getFechaEmision();
                strfechaemisionreemb = ecFormat.format(strdate);
                strautorizacionreemb = SearchDocument.getAuthorization();
                baseimponiblereemb = Double.valueOf(SearchDocument.getBaseImpGrabReem().toString());
                baseimpgravreemb = SearchDocument.getBaseNoGrabIva().doubleValue();
                basenograivareemb = SearchDocument.getBaseImpGrabada().doubleValue();
                montoiceremb = SearchDocument.getMontoIce().doubleValue();
                montoivaremb = SearchDocument.getMontoRetIva().doubleValue();
                baseexentareemb = SearchDocument.getBaseExenta().doubleValue();

              } catch (Exception e) {
              }

              /*
               * StrCodCompra = strcodcompra == null ? " " : strcodcompra; Element codCompra =
               * doc.createElement("codCompra"); codCompra.appendChild
               * (doc.createTextNode(StrCodCompra)); reembolso.appendChild(codCompra);
               */

              StrtipoComprobanteReemb = strtipocomprobantereemb == null ? " "
                  : strtipocomprobantereemb;
              Element tipoComprobanteReemb = doc.createElement("tipoComprobanteReemb");
              tipoComprobanteReemb.appendChild(doc.createTextNode(StrtipoComprobanteReemb));
              reembolso.appendChild(tipoComprobanteReemb);

              if ((!strtpidprobreemb.equals("01") && !strtpidprobreemb.equals("02")
                  && !strtpidprobreemb.equals("03") && !strtpidprobreemb.equals("04")
                  && !strtpidprobreemb.equals("05") && !strtpidprobreemb.equals("06")
                  && !strtpidprobreemb.equals("07") && !strtpidprobreemb.equals("08")
                  && !strtpidprobreemb.equals("09") && !strtpidprobreemb.equals("10")
                  && !strtpidprobreemb.equals("11") && !strtpidprobreemb.equals("12")
                  && !strtpidprobreemb.equals("13") && !strtpidprobreemb.equals("14")
                  && !strtpidprobreemb.equals("15") && !strtpidprobreemb.equals("16")
                  && !strtpidprobreemb.equals("17") && !strtpidprobreemb.equals("18")
                  && !strtpidprobreemb.equals("19"))) {
                throw new OBException(
                    "El código del Tipo de Identificación del Proveedor Reembolso(tpIdProvReemb) es invalida. ["
                        + strtpidprobreemb + "]");
              }
              StrtpIdProvReemb = strtpidprobreemb == null ? " " : strtpidprobreemb;
              Element tpIdProvReemb = doc.createElement("tpIdProvReemb");
              tpIdProvReemb.appendChild(doc.createTextNode(StrtpIdProvReemb));
              reembolso.appendChild(tpIdProvReemb);

              StridProvReemb = stridprovreemb == null ? " " : stridprovreemb;
              Element idProvReemb = doc.createElement("idProvReemb");
              idProvReemb.appendChild(doc.createTextNode(StridProvReemb));
              reembolso.appendChild(idProvReemb);

              StrestablecimientoReemb = strestablecimientoreemb == null ? " "
                  : strestablecimientoreemb;
              Element establecimientoReemb = doc.createElement("establecimientoReemb");
              establecimientoReemb.appendChild(doc.createTextNode(StrestablecimientoReemb));
              reembolso.appendChild(establecimientoReemb);

              StrpuntoEmisionReemb = strpuntoemisionreem == null ? " " : strpuntoemisionreem;
              Element puntoEmisionReemb = doc.createElement("puntoEmisionReemb");
              puntoEmisionReemb.appendChild(doc.createTextNode(StrpuntoEmisionReemb));
              reembolso.appendChild(puntoEmisionReemb);

              StrsecuencialReemb = strsecuencialreemb == null ? " " : strsecuencialreemb;
              Element secuencialReemb = doc.createElement("secuencialReemb");
              secuencialReemb.appendChild(doc.createTextNode(StrsecuencialReemb));
              reembolso.appendChild(secuencialReemb);

              StrfechaEmisionReemb = strfechaemisionreemb == null ? " " : strfechaemisionreemb;
              Element fechaEmisionReemb = doc.createElement("fechaEmisionReemb");
              fechaEmisionReemb.appendChild(doc.createTextNode(StrfechaEmisionReemb));
              reembolso.appendChild(fechaEmisionReemb);

              StrautorizacionReemb = strautorizacionreemb == null ? " " : strautorizacionreemb;
              Element autorizacionReemb = doc.createElement("autorizacionReemb");
              autorizacionReemb.appendChild(doc.createTextNode(StrautorizacionReemb));
              reembolso.appendChild(autorizacionReemb);

              baseimponiblereemb = SearchDocument.getBaseImpGrabReem().doubleValue();
              strbaseimponiblereemb = formateador.format(baseimponiblereemb).toString() == null
                  ? "0.00"
                  : formateador.format(baseimponiblereemb);
              StrbaseImponibleReemb = strbaseimponiblereemb;
              Element baseImponibleReemb = doc.createElement("baseImponibleReemb");
              baseImponibleReemb.appendChild(doc.createTextNode(StrbaseImponibleReemb));
              reembolso.appendChild(baseImponibleReemb);

              strbaseimpgravreemb = formateador.format(baseimpgravreemb).toString() == null ? "0.00"
                  : formateador.format(baseimpgravreemb);
              StrbaseImpGravReemb = strbaseimpgravreemb;
              Element baseImpGravReemb = doc.createElement("baseImpGravReemb");
              baseImpGravReemb.appendChild(doc.createTextNode(StrbaseImpGravReemb));
              reembolso.appendChild(baseImpGravReemb);

              strbasenograivareemb = formateador.format(basenograivareemb).toString() == null
                  ? "0.00"
                  : formateador.format(basenograivareemb);
              StrbaseNoGraIvaReemb = strbasenograivareemb;
              Element baseNoGraIvaReemb = doc.createElement("baseNoGraIvaReemb");
              baseNoGraIvaReemb.appendChild(doc.createTextNode(StrbaseNoGraIvaReemb));
              reembolso.appendChild(baseNoGraIvaReemb);

              // CAMBIOS NUEVO ANEXO 2015 - (FI)
              strbaseexentareemb = formateador.format(baseexentareemb).toString() == null ? "0.00"
                  : formateador.format(baseexentareemb);
              StrbaseNoGraIvaReemb = strbasenograivareemb;
              Element baseImpExeReemb = doc.createElement("baseImpExeReemb");
              baseImpExeReemb.appendChild(doc.createTextNode(strbaseexentareemb));
              reembolso.appendChild(baseImpExeReemb);
              // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)

              strmontoiceremb = formateador.format(montoiceremb).toString() == null ? "0.00"
                  : formateador.format(montoiceremb);
              StrmontoIceRemb = strmontoiceremb;
              Element montoIceRemb = doc.createElement("montoIceRemb");
              montoIceRemb.appendChild(doc.createTextNode(StrmontoIceRemb));
              reembolso.appendChild(montoIceRemb);

              strmontoivaremb = formateador.format(montoivaremb).toString() == null ? "0.00"
                  : formateador.format(montoivaremb);
              StrmontoIvaRemb = strmontoivaremb;
              Element montoIvaRemb = doc.createElement("montoIvaRemb");
              montoIvaRemb.appendChild(doc.createTextNode(StrmontoIvaRemb));
              reembolso.appendChild(montoIvaRemb);
              // }
            }
          } // ***FIN REEMBOLSOS

          // Notas de Débito o Crédito

          // SswhDebitCreditNoteDetData[] dataDebitCredit;

          // dataDebitCredit = SswhDebitCreditNoteDetData.select(conn, strCInvoiceID);

          OBCriteria<SswhDcNote> ObjSswhDcNoteList = OBDal.getInstance()
              .createCriteria(SswhDcNote.class);
          ObjSswhDcNoteList.add(Restrictions.eq(SswhDcNote.PROPERTY_PROCESS, strProcessID));

          Invoice invoicenc = OBDal.getInstance().get(Invoice.class, strCInvoiceID);

          ObjSswhDcNoteList.add(Restrictions.eq(SswhDcNote.PROPERTY_INVOICE, invoicenc));

          List<SswhDcNote> lstDcNote = ObjSswhDcNoteList.list();
          if (lstDcNote.size() > 0) {

            for (SswhDcNote debitCreditData : ObjSswhDcNoteList.list()) {

              String strTipoComprobanteDC = "";
              String strEstablecimientoDC = "";
              String strPtoEmisionDC = "";
              String strSecuenialDC = "";
              String strAutorizacionDC = "";

              try {
                strTipoComprobanteDC = debitCreditData.getTipoComprobante();
                strEstablecimientoDC = debitCreditData.getEstablecimmiento();
                strPtoEmisionDC = debitCreditData.getCaja();
                strSecuenialDC = debitCreditData.getSecuencia();
                strAutorizacionDC = debitCreditData.getAutorizacion();

              } catch (Exception e) {

              }

              if (strAutorizacionDC.equals("") || strAutorizacionDC.isEmpty()) {

                throw new OBException("La Factura " + strEstablecimientoDC + "-" + strPtoEmisionDC
                    + "-" + strSecuenialDC + ", de tipo: " + strTipoComprobanteDC
                    + " no tiene Nro. de Autorización");
              }

              String StrdocModificado = "";
              StrdocModificado = strTipoComprobanteDC == null ? " " : strTipoComprobanteDC;
              Element docModificado = doc.createElement("docModificado");
              docModificado.appendChild(doc.createTextNode(StrdocModificado));
              detalleCompras.appendChild(docModificado);

              String StrestabModificado = "";
              StrestabModificado = strEstablecimientoDC == null ? " " : strEstablecimientoDC;
              Element estabModificado = doc.createElement("estabModificado");
              estabModificado.appendChild(doc.createTextNode(StrestabModificado));
              detalleCompras.appendChild(estabModificado);

              String StrptoEmiModificado = "";
              StrptoEmiModificado = strPtoEmisionDC == null ? " " : strPtoEmisionDC;
              Element ptoEmiModificado = doc.createElement("ptoEmiModificado");
              ptoEmiModificado.appendChild(doc.createTextNode(StrptoEmiModificado));
              detalleCompras.appendChild(ptoEmiModificado);

              String StrsecModificado = "";
              StrsecModificado = strSecuenialDC == null ? " " : strSecuenialDC;
              Element secModificado = doc.createElement("secModificado");
              secModificado.appendChild(doc.createTextNode(StrsecModificado));
              detalleCompras.appendChild(secModificado);

              String StrautModificado = "";
              StrautModificado = strAutorizacionDC == null ? " " : strAutorizacionDC;
              Element autModificado = doc.createElement("autModificado");
              autModificado.appendChild(doc.createTextNode(StrautModificado));
              detalleCompras.appendChild(autModificado);

            }
          }

          // FIN - Notas de Débito o Crédito

        } // *** Fin Detalle de Compras

      }

      // Third Tag of the sales detailgetMontoRetRenta

      // inicio ventas
      // try {

      SswhRptcSalesDetData data[];

      data = SswhRptcSalesDetData.select(cp, strProcessID);

      // List<SswhRptcSalesDet> SearchDetSales = data;

      // int CountRecord = SearchDetSales.size(); // FI
      int CountRecord = data.length;
      int aux = 0;

      // ****TICKET 1917 A.M. 04/10/2018

      // ****FIN 1917 A.M. 04/10/2018

      if (CountRecord > 0) {
        ventas = doc.createElement("ventas");
        rootElement.appendChild(ventas);

        // while (aux <= CountRecord) {
        // for (SswhRptcSalesDet SswhSearchDetSales :
        // ObjSalesDetList.list()) { // FI
        if (CountRecord > 0) {
          for (SswhRptcSalesDetData SswhSearchDetSales : data) {
            aux++;
            // Detalle de ventas
            Element detalleVentas = doc.createElement("detalleVentas");
            ventas.appendChild(detalleVentas);

            String strtpidcl2 = "";
            String StrtpIdCliente1 = "";
            String Stridcliente1 = "";
            String stridcliente2 = "";
            String StrtipoComprobante1 = "";
            String strtipocomprobante2 = "";
            String StrnumeroComprobantes1 = "";
            String strnumerocomprobantes2 = "";
            String strbasenograviva2 = "";
            String StrbaseNoGraIva1 = "";
            double basenogravaiva = 0;
            String Strbaseimponible1 = "";
            String strbaseimponible2 = "";
            double baseimponible = 0;
            String Strbaseimpgrav1 = "";
            String strbaseimpgrav2 = "";
            double baseimpgrav = 0;
            String Strmontoiva1 = "";
            String strmontoiva2 = "";
            double montoiva = 0;
            String Strvalorretiva1 = "";
            String strvalorretiva2 = "";
            double valorretiva = 0;
            String Strvalorterenta1 = "";
            String strvalorterenta2 = "";
            double valorterenta = 0;
            String strparterelacionadaV = "";
            String parterelacionadaV = "";
            String strtipocliente = "";
            String StrtipoCliente = "";
            String strdenocli = "";
            String StrdenoCli = "";
            String strtipoem = "";
            String StrtipoEm = "";
            double montoiceV = 0;
            String strmontoice1 = "";
            String strmontoice2 = "";

            // java.math.BigDecimal bgdnumerocomprobantes;
            String bgdnumerocomprobantes;

            String strTaxid = SswhSearchDetSales.identifCliente == null ? "ND"
                : SswhSearchDetSales.tipoIdentificador;

            OBCriteria<SswhWithhCardCredit> objWithhCardCredit = OBDal.getInstance()
                .createCriteria(SswhWithhCardCredit.class);

            if (!strTaxid.equals("ND")) {
              OBCriteria<BusinessPartner> crPartnerLt = OBDal.getInstance()
                  .createCriteria(BusinessPartner.class);
              crPartnerLt.add(Restrictions.sqlRestriction("TRIM(taxID) = ?",
                  SswhSearchDetSales.identifCliente, StringType.INSTANCE)); 
              
              objWithhCardCredit.add(Restrictions.ge(SswhWithhCardCredit.PROPERTY_DATEDOC,
                  SrchPeriod.getStartingDate()));
              objWithhCardCredit.add(Restrictions.le(SswhWithhCardCredit.PROPERTY_DATEDOC,
                  SrchPeriod2.getEndingDate()));
              objWithhCardCredit.add(Restrictions.eq(SswhWithhCardCredit.PROPERTY_STATUS, "CO"));
              if (crPartnerLt.count() > 0) {
                objWithhCardCredit
                .add(Restrictions.in(SswhWithhCardCredit.PROPERTY_BPARTNER, crPartnerLt.list()));
              } else {
              objWithhCardCredit.add(Restrictions.eq(SswhWithhCardCredit.PROPERTY_BPARTNER,
                  OBDal.getInstance().get(BusinessPartner.class, strTaxid)));
              }
            }

            try {

              strtpidcl2 = SswhSearchDetSales.tipoIdentificador;
              stridcliente2 = SswhSearchDetSales.identifCliente;// +
                                                                // " "
                                                                // +
                                                                // aux;
              strtipocomprobante2 = SswhSearchDetSales.codTipoComprobante;
              bgdnumerocomprobantes = SswhSearchDetSales.count.toString();
              strnumerocomprobantes2 = String
                  .valueOf(bgdnumerocomprobantes.toString().replace(",", "."));
              basenogravaiva = Math.abs(Double.valueOf(SswhSearchDetSales.baseNoIva));
              baseimponible = Math.abs(Double.valueOf(SswhSearchDetSales.baseIvaCero));
              baseimpgrav = Math.abs(Double.valueOf(SswhSearchDetSales.baseIvaDoce));
              montoiva = Math.abs(Double.valueOf(SswhSearchDetSales.montoIva));
              valorretiva = Double.valueOf(SswhSearchDetSales.montoRetIva);
              valorterenta = Double.valueOf(SswhSearchDetSales.montoRetRenta);
              parterelacionadaV = SswhSearchDetSales.parteRelacionada;
              strtipocliente = SswhSearchDetSales.tipoCliente;
              strdenocli = SswhSearchDetSales.denoCli;
              strtipoem = SswhSearchDetSales.tipoEm;

              // System.out.println("'" + stridcliente2 + "';" +
              // (basenogravaiva + baseimponible +
              // baseimpgrav));
            } catch (Exception e) {

              // System.out.println(e.getMessage()); // CC
            }

            if (objWithhCardCredit.list().size() > 0) {

              List<SswhWithhCardCredit> lstWithhCreditCard = objWithhCardCredit.list();

              double dblvoucherNumberWcc = 0;
              double dblVATWCc = 0;
              double dblRentWCc = 0;

              for (SswhWithhCardCredit collWithhCC : lstWithhCreditCard) {

                dblvoucherNumberWcc = dblvoucherNumberWcc
                    + collWithhCC.getVouchersNumber().doubleValue();
                dblVATWCc = dblVATWCc + collWithhCC.getWithhVat().doubleValue();
                dblRentWCc = dblRentWCc + collWithhCC.getWithhRent().doubleValue();
              }
              valorterenta = valorterenta + dblRentWCc;
              valorretiva = valorretiva + dblVATWCc;
              dblvoucherNumberWcc = dblvoucherNumberWcc
                  + Double.valueOf(SswhSearchDetSales.count.toString());

              bgdnumerocomprobantes = String.valueOf(dblvoucherNumberWcc);
              strnumerocomprobantes2 = String
                  .valueOf(bgdnumerocomprobantes.toString().replace(",", "."));

            }

            if ((!strtpidcl2.equals("01") && !strtpidcl2.equals("02") && !strtpidcl2.equals("03")

                && !strtpidcl2.equals("04") && !strtpidcl2.equals("05") && !strtpidcl2.equals("06")
                && !strtpidcl2.equals("07") && !strtpidcl2.equals("08") && !strtpidcl2.equals("09")
                && !strtpidcl2.equals("10") && !strtpidcl2.equals("11") && !strtpidcl2.equals("12")
                && !strtpidcl2.equals("13") && !strtpidcl2.equals("14") && !strtpidcl2.equals("15")
                && !strtpidcl2.equals("16") && !strtpidcl2.equals("17") && !strtpidcl2.equals("18")
                && !strtpidcl2.equals("19"))) {
              throw new OBException(
                  "El código del Tipo de Identificación del Cliente(tpIdCliente) es obligatorio.");
            }

            StrtpIdCliente1 = strtpidcl2 == null ? "" : strtpidcl2;
            Element tpIdCliente = doc.createElement("tpIdCliente");
            tpIdCliente.appendChild(doc.createTextNode(StrtpIdCliente1));
            detalleVentas.appendChild(tpIdCliente);

            if (stridcliente2.equals("") || stridcliente2 == null) {
              throw new OBException("No. de Identificación del Cliente(idCliente) es obligatorio.");
            }
            Stridcliente1 = stridcliente2 == null ? "" : stridcliente2;
            Element idcliente = doc.createElement("idCliente");
            idcliente.appendChild(doc.createTextNode(Stridcliente1));
            detalleVentas.appendChild(idcliente);

            // CAMBIOS NUEVO ANEXO 2015 - (FI)
            /*
             * if (parterelacionadaV.equals("") || parterelacionadaV == null) { throw new
             * OBException( "Parte relacionada(parteRelVtas) es obligatorio."); }
             */
            strparterelacionadaV = parterelacionadaV == null ? "" : parterelacionadaV;

            String StrTmpParteRelacionada = "";

            if (strparterelacionadaV == null
                || strparterelacionadaV.trim().equals("") && !StrtpIdCliente1.equals("07")) {
              StrTmpParteRelacionada = "NO";
            } else if (!StrtpIdCliente1.equals("07")) {

              StrTmpParteRelacionada = strparterelacionadaV;
            }

            Element parteRelVtas = doc.createElement("parteRelVtas");
            // Thread.currentThread();
            // Thread.sleep(300L);

            parteRelVtas.appendChild(doc.createTextNode(StrTmpParteRelacionada));
            detalleVentas.appendChild(parteRelVtas);
            // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)

            if (strtipocliente != null && !strtipocliente.equals("")) {
              StrtipoCliente = strtipocliente == null ? "" : strtipocliente;
              Element tipoCliente = doc.createElement("tipoCliente");
              tipoCliente.appendChild(doc.createTextNode(StrtipoCliente));
              detalleVentas.appendChild(tipoCliente);
            }

            if (strdenocli != null && !strdenocli.equals("")) {
              StrdenoCli = strdenocli == null ? "" : strdenocli;
              Element denoCli = doc.createElement("denoCli");
              denoCli.appendChild(doc.createTextNode(getOnlyStrings(StrdenoCli)));
              detalleVentas.appendChild(denoCli);
            }

            if (strtipocomprobante2.equals("") || strtipocomprobante2 == null) {
              throw new OBException("Código tipo de comprobante(tipoComprobante) es obligatorio.");
            }
            StrtipoComprobante1 = strtipocomprobante2 == null ? "" : strtipocomprobante2;
            Element tipoComprobante = doc.createElement("tipoComprobante");
            tipoComprobante.appendChild(doc.createTextNode(StrtipoComprobante1));
            detalleVentas.appendChild(tipoComprobante);

            if (strtipoem != null && !strtipoem.equals("")) {
              StrtipoEm = strtipoem == null ? "" : strtipoem;
              Element tipoEm = doc.createElement("tipoEmision");
              tipoEm.appendChild(doc.createTextNode(StrtipoEm));
              detalleVentas.appendChild(tipoEm);
            }

            if (strnumerocomprobantes2.equals("") || strnumerocomprobantes2 == null) {
              throw new OBException(
                  "No. de Comprobantes Emitidos(numeroComprobantes) es obligatorio.");
            }
            StrnumeroComprobantes1 = strnumerocomprobantes2 == null ? "" : strnumerocomprobantes2;
            Element numeroComprobantes = doc.createElement("numeroComprobantes");
            numeroComprobantes.appendChild(doc.createTextNode(StrnumeroComprobantes1));
            detalleVentas.appendChild(numeroComprobantes);

            strbasenograviva2 = formateador.format(basenogravaiva);
            StrbaseNoGraIva1 = strbasenograviva2 == null ? "0.00" : strbasenograviva2;
            Element baseNoGraIva = doc.createElement("baseNoGraIva");
            baseNoGraIva.appendChild(doc.createTextNode(StrbaseNoGraIva1));
            detalleVentas.appendChild(baseNoGraIva);

            strbaseimponible2 = formateador.format(baseimponible);
            Strbaseimponible1 = strbaseimponible2 == null ? "0.00" : strbaseimponible2;
            Element baseImponible = doc.createElement("baseImponible");
            baseImponible.appendChild(doc.createTextNode(Strbaseimponible1));
            detalleVentas.appendChild(baseImponible);

            strbaseimpgrav2 = formateador.format(baseimpgrav);
            Strbaseimpgrav1 = strbaseimpgrav2 == null ? "0.00" : strbaseimpgrav2;
            Element baseImpGrav = doc.createElement("baseImpGrav");
            baseImpGrav.appendChild(doc.createTextNode(Strbaseimpgrav1));
            detalleVentas.appendChild(baseImpGrav);

            strmontoiva2 = formateador.format(montoiva);
            Strmontoiva1 = strmontoiva2 == null ? "0.00" : strmontoiva2;
            Element montoIva = doc.createElement("montoIva");
            montoIva.appendChild(doc.createTextNode(Strmontoiva1));
            detalleVentas.appendChild(montoIva);

            // SswhRptcSalesCompData dataComp[];

            OBCriteria<SswhRptcSalesComp> dataComp = OBDal.getInstance()
                .createCriteria(SswhRptcSalesComp.class);
            dataComp.add(Restrictions.eq(SswhRptcSalesComp.PROPERTY_PROCESS, strProcessID));

            /*
             * if (!StrOrgID.isEmpty()) { dataComp = SswhRptcSalesCompData.select(conn, StrPeriodId,
             * SrchOrg.getId().toString(), Stridcliente1, StrPeriodId, SrchOrg.getId().toString(),
             * Stridcliente1, StrPeriodId, SrchOrg.getId().toString(), Stridcliente1); } else {
             * dataComp = SswhRptcSalesCompData.select(conn, StrPeriodId, null, Stridcliente1,
             * StrPeriodId, null, Stridcliente1, StrPeriodId, null, Stridcliente1); }
             */
            if (dataComp.list().size() > 0) {
              Element compensaciones = doc.createElement("compensaciones");
              for (SswhRptcSalesComp SswhSearchCompSales : dataComp.list()) {

                String strtipocomp = "";
                String strtipoComp = "";
                double montocomp = 0;
                String strmontocomp = "";
                String strmontoComp = "";

                strtipocomp = SswhSearchCompSales.getCompType();
                strtipoComp = strtipocomp == null ? "" : strtipocomp;

                montocomp = Double.valueOf(SswhSearchCompSales.getCompensatedAmount().toString());
                strmontocomp = formateador.format(montocomp);
                strmontoComp = strmontocomp == null ? "0.00" : strmontocomp;

                Element compensacion = doc.createElement("compensacion");

                Element tipoComp = doc.createElement("tipoCompe");
                tipoComp.appendChild(doc.createTextNode(strtipoComp));
                compensacion.appendChild(tipoComp);

                Element monto = doc.createElement("monto");
                monto.appendChild(doc.createTextNode(strmontoComp));
                compensacion.appendChild(monto);

                compensaciones.appendChild(compensacion);

              }

              detalleVentas.appendChild(compensaciones);

            }

            // CAMBIOS NUEVO ANEXO 2015 - (FI)
            strmontoice2 = formateador.format(montoiceV);
            strmontoice1 = strmontoice2 == null ? "0.00" : strmontoice2;
            Element montoIce = doc.createElement("montoIce");
            montoIce.appendChild(doc.createTextNode(strmontoice1));
            detalleVentas.appendChild(montoIce);
            // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)

            strvalorretiva2 = formateador.format(valorretiva);
            Strvalorretiva1 = strvalorretiva2 == null ? "0.00" : strvalorretiva2;
            Element valorRetIva = doc.createElement("valorRetIva");
            valorRetIva.appendChild(doc.createTextNode(Strvalorretiva1));
            detalleVentas.appendChild(valorRetIva);

            strvalorterenta2 = formateador.format(valorterenta);
            Strvalorterenta1 = strvalorterenta2 == null ? "0.00" : strvalorterenta2;
            Element valorRetRenta = doc.createElement("valorRetRenta");
            valorRetRenta.appendChild(doc.createTextNode(Strvalorterenta1));
            detalleVentas.appendChild(valorRetRenta);

            // SswhRptcSalesPayFormData dataPayForm[];

            OBCriteria<SswhRptcSalesPayForm> dataPayForm = OBDal.getInstance()
                .createCriteria(SswhRptcSalesPayForm.class);
            dataPayForm.add(Restrictions.eq(SswhRptcSalesPayForm.PROPERTY_PROCESS, strProcessID));
            dataPayForm
                .add(Restrictions.eq(SswhRptcSalesPayForm.PROPERTY_IDENTIFCLIENTE, Stridcliente1));
            /*
             * if (!StrOrgID.isEmpty()) { dataPayForm = SswhRptcSalesPayFormData.select(conn,
             * StrPeriodId, SrchOrg.getId().toString(), Stridcliente1, StrPeriodId,
             * SrchOrg.getId().toString(), Stridcliente1); } else { dataPayForm =
             * SswhRptcSalesPayFormData.select(conn, StrPeriodId, null, Stridcliente1, StrPeriodId,
             * null, Stridcliente1); }
             */
            if (dataPayForm.list().size() > 0) {
              Element formasDePago = doc.createElement("formasDePago");
              for (SswhRptcSalesPayForm SswhSearchPayFormSales : dataPayForm.list()) {

                String strformapago = "";
                String strformaPago = "";

                strformapago = SswhSearchPayFormSales.getValue();
                strformaPago = strformapago == null ? "" : strformapago;

                Element formaPago = doc.createElement("formaPago");
                formaPago.appendChild(doc.createTextNode(strformaPago));
                formasDePago.appendChild(formaPago);

              }
              if (!StrtipoComprobante1.equals("04")) {
                detalleVentas.appendChild(formasDePago);
              }
            }
            
            
            
          }
        }
      
        
      //*********************************
        // Tickets de Venta
        //*********************************            

        OBCriteria<CsspidSalesTickets> objSalesTickets = OBDal.getInstance()
            .createCriteria(CsspidSalesTickets.class);

        if (objSalesTickets.list().size()>0) {
        	
            if (CountRecord ==0) {
                ventas = doc.createElement("ventas");
                rootElement.appendChild(ventas);
            }

            
        	objSalesTickets.add(Restrictions.ge(CsspidSalesTickets.PROPERTY_DATEDOC,
                SrchPeriod.getStartingDate()));
        	objSalesTickets.add(Restrictions.le(CsspidSalesTickets.PROPERTY_DATEDOC,
                SrchPeriod2.getEndingDate()));
        	objSalesTickets.add(Restrictions.eq(CsspidSalesTickets.PROPERTY_STATUS, "CO"));
        	//objSalesTickets.add(Restrictions.eq(CsspidSalesTickets.PROPERTY_BPARTNER,
            //    OBDal.getInstance().get(BusinessPartner.class, strTaxid)));
        	
        	
        	 if (objSalesTickets.list().size() > 0) {

                 List<CsspidSalesTickets> lstSalesTickets = objSalesTickets.list();

                 double dblvoucherNumberWcc = 0;
                 double dblVATWCc = 0;
                 double dblRentWCc = 0;

                 for (CsspidSalesTickets collSalesTickets : lstSalesTickets) {
                	 
                     
                 	Element detalleVentas = doc.createElement("detalleVentas");
                     ventas.appendChild(detalleVentas);

                   dblvoucherNumberWcc = dblvoucherNumberWcc
                       + collSalesTickets.getVouchersNumber().doubleValue();
                   dblVATWCc = dblVATWCc + collSalesTickets.getBaseNoVAT().doubleValue();
                   dblRentWCc = dblRentWCc + collSalesTickets.getBaseZero().doubleValue();
               
                
                		   
                 double valorterenta =  dblRentWCc;
                 double valorretiva = dblVATWCc;
                 dblvoucherNumberWcc = dblvoucherNumberWcc
                     + Double.valueOf(collSalesTickets.getVouchersNumber());

                 String bgdnumerocomprobantes = String.valueOf(dblvoucherNumberWcc);
                 String strnumerocomprobantes2 = String
                     .valueOf(bgdnumerocomprobantes.toString().replace(",", "."));

                 String strTypeIdentify = collSalesTickets.getBpartner().getSswhTaxidtype();
                 String strtpidcl2 = "";
                 if (strTypeIdentify.equals("R")) {
                	 strtpidcl2 = "01";
                 }else if (strTypeIdentify.equals("P")) {
                	 strtpidcl2 = "03";
                 }else if (strTypeIdentify.equals("D")) {
                	 strtpidcl2 = "02";
                 }else if (strTypeIdentify.equals("C")) {
                	 strtpidcl2 = "OTRO";
                 }else {
                	 strtpidcl2 ="";
                 }
                 
                 String strTaxidST = collSalesTickets.getBpartner().getTaxID();
                 
    			 String strtipocomprobante2 = "18";

    			 String strtipoem = "F";

                 
                 
                 strnumerocomprobantes2 = collSalesTickets.getVouchersNumber().toString();
                 
                 double basenogravaiva = collSalesTickets.getBaseNoVAT().doubleValue();
                 double baseimponible =collSalesTickets.getBaseZero().doubleValue();
                 
        	 /*
        	  * 
        	  *       WHEN 'R'::text THEN to_char('01'::text)
                        WHEN 'P'::text THEN to_char('03'::text)
                        WHEN 'D'::text THEN to_char('02'::text)
                        WHEN 'C'::text THEN to_char('OTRO'::text)
                        ELSE NULL::character varying
        	  * 
        	  * */
        	 
        	 if ((!strtpidcl2.equals("01") && !strtpidcl2.equals("02") && !strtpidcl2.equals("03")

                     && !strtpidcl2.equals("04") && !strtpidcl2.equals("05") && !strtpidcl2.equals("06")
                     && !strtpidcl2.equals("07") && !strtpidcl2.equals("08") && !strtpidcl2.equals("09")
                     && !strtpidcl2.equals("10") && !strtpidcl2.equals("11") && !strtpidcl2.equals("12")
                     && !strtpidcl2.equals("13") && !strtpidcl2.equals("14") && !strtpidcl2.equals("15")
                     && !strtpidcl2.equals("16") && !strtpidcl2.equals("17") && !strtpidcl2.equals("18")
                     && !strtpidcl2.equals("19"))) {
                   throw new OBException(
                       "El código del Tipo de Identificación del Cliente(tpIdCliente) es obligatorio.");
                 }

                 String StrtpIdCliente1 = strtpidcl2 == null ? "" : strtpidcl2;
                 Element tpIdClienteSt = doc.createElement("tpIdCliente");
                 tpIdClienteSt.appendChild(doc.createTextNode(StrtpIdCliente1));
                 detalleVentas.appendChild(tpIdClienteSt);

                 if (strTaxidST.equals("") || strTaxidST == null) {
                   throw new OBException("No. de Identificación del Cliente(idCliente) es obligatorio.");
                 }
                 String Stridcliente1 = strTaxidST == null ? "" : strTaxidST;
                 Element idclienteST = doc.createElement("idCliente");
                 idclienteST.appendChild(doc.createTextNode(Stridcliente1));
                 detalleVentas.appendChild(idclienteST);

                 String parterelacionadaV="";
                 String strparterelacionadaV = parterelacionadaV == null ? "" : parterelacionadaV;

                 String StrTmpParteRelacionadaST = "";

                 if (strparterelacionadaV == null
                     || strparterelacionadaV.trim().equals("") && !StrtpIdCliente1.equals("07")) {
                	 StrTmpParteRelacionadaST = "NO";
                 } else if (!StrtpIdCliente1.equals("07")) {

                	 StrTmpParteRelacionadaST = strparterelacionadaV;
                 }

                 Element parteRelVtasST = doc.createElement("parteRelVtas");
                 // Thread.currentThread();
                 // Thread.sleep(300L);

                 parteRelVtasST.appendChild(doc.createTextNode(StrTmpParteRelacionadaST));
                 detalleVentas.appendChild(parteRelVtasST);
                 // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)
                 
                 String strtipocliente ="";
                 
                 if (strtipocliente != null && !strtipocliente.equals("")) {
                	 String StrtipoCliente = strtipocliente == null ? "" : strtipocliente;
                   Element tipoCliente = doc.createElement("tipoCliente");
                   tipoCliente.appendChild(doc.createTextNode(StrtipoCliente));
                   detalleVentas.appendChild(tipoCliente);
                 }
                 String strdenocli="";
                 if (strdenocli != null && !strdenocli.equals("")) {
                   String StrdenoCli = strdenocli == null ? "" : strdenocli;
                   Element denoCli = doc.createElement("denoCli");
                   denoCli.appendChild(doc.createTextNode(getOnlyStrings(StrdenoCli)));
                   detalleVentas.appendChild(denoCli);
                 }

                 if (strtipocomprobante2.equals("") || strtipocomprobante2 == null) {
                   throw new OBException("Código tipo de comprobante(tipoComprobante) es obligatorio.");
                 }
                 String StrtipoComprobante1 = strtipocomprobante2 == null ? "" : strtipocomprobante2;
                 Element tipoComprobanteST = doc.createElement("tipoComprobante");
                 tipoComprobanteST.appendChild(doc.createTextNode(StrtipoComprobante1));
                 detalleVentas.appendChild(tipoComprobanteST);

                 if (strtipoem != null && !strtipoem.equals("")) {
                   String StrtipoEm = strtipoem == null ? "" : strtipoem;
                   Element tipoEm = doc.createElement("tipoEmision");
                   tipoEm.appendChild(doc.createTextNode(StrtipoEm));
                   detalleVentas.appendChild(tipoEm);
                 }

                 if (strnumerocomprobantes2.equals("") || strnumerocomprobantes2 == null) {
                   throw new OBException(
                       "No. de Comprobantes Emitidos(numeroComprobantes) es obligatorio.");
                 }
                 String StrnumeroComprobantes1 = strnumerocomprobantes2 == null ? "" : strnumerocomprobantes2;
                 Element numeroComprobantesST = doc.createElement("numeroComprobantes");
                 numeroComprobantesST.appendChild(doc.createTextNode(StrnumeroComprobantes1));
                 detalleVentas.appendChild(numeroComprobantesST);


                 
                 String strbasenograviva2 = formateador.format(basenogravaiva);
                 String StrbaseNoGraIva1 = strbasenograviva2 == null ? "0.00" : strbasenograviva2;
                 Element baseNoGraIvaST = doc.createElement("baseNoGraIva");
                 baseNoGraIvaST.appendChild(doc.createTextNode(StrbaseNoGraIva1));
                 detalleVentas.appendChild(baseNoGraIvaST);

                 String strbaseimponible2 = formateador.format(baseimponible);
                 String Strbaseimponible1 = strbaseimponible2 == null ? "0.00" : strbaseimponible2;
                 Element baseImponibleST = doc.createElement("baseImponible");
                 baseImponibleST.appendChild(doc.createTextNode(Strbaseimponible1));
                 detalleVentas.appendChild(baseImponibleST);

                 double baseimpgrav= 0;
                 String strbaseimpgrav2 = formateador.format(baseimpgrav);
                 String Strbaseimpgrav1 = strbaseimpgrav2 == null ? "0.00" : strbaseimpgrav2;
                 Element baseImpGravST = doc.createElement("baseImpGrav");
                 baseImpGravST.appendChild(doc.createTextNode(Strbaseimpgrav1));
                 detalleVentas.appendChild(baseImpGravST);

                 
                 double montoiva=0;
                 String strmontoiva2 = formateador.format(montoiva);
                 String Strmontoiva1 = strmontoiva2 == null ? "0.00" : strmontoiva2;
                 Element montoIvaST = doc.createElement("montoIva");
                 montoIvaST.appendChild(doc.createTextNode(Strmontoiva1));
                 detalleVentas.appendChild(montoIvaST);
                 
                 valorretiva=0;
                 String strvalorretiva2 = formateador.format(valorretiva);
                 String Strvalorretiva1 = strvalorretiva2 == null ? "0.00" : strvalorretiva2;
                 Element valorRetIva = doc.createElement("valorRetIva");
                 valorRetIva.appendChild(doc.createTextNode(Strvalorretiva1));
                 detalleVentas.appendChild(valorRetIva);

                 valorterenta=0;
                 String strvalorterenta2 = formateador.format(valorterenta);
                 String Strvalorterenta1 = strvalorterenta2 == null ? "0.00" : strvalorterenta2;
                 Element valorRetRenta = doc.createElement("valorRetRenta");
                 valorRetRenta.appendChild(doc.createTextNode(Strvalorterenta1));
                 detalleVentas.appendChild(valorRetRenta);
                 
                 
                 if (collSalesTickets.getFINPaymentmethod()!=null) {
                     Element formasDePago = doc.createElement("formasDePago");

                       String strformapago = "";
                       String strformaPago = "";

                       try {
                       strformapago = collSalesTickets.getFINPaymentmethod().getSswhCodeats();
                       }catch(Exception exsql2) {
                    	   
                       }
                       
                       strformaPago = strformapago == null ? "" : strformapago;

                       Element formaPago = doc.createElement("formaPago");
                       formaPago.appendChild(doc.createTextNode(strformaPago));
                       formasDePago.appendChild(formaPago);

                       if (!StrtipoComprobante1.equals("04")) {
                       detalleVentas.appendChild(formasDePago);
                       }
                   }
                 
        	 }
             }

        }

        // ****TICKET 1917 A.M. 04/10/2018
        /*
         * if (intCountWithhCreditCard > 0) { for (SswhWithhCardCredit lstWithhCard :
         * lstWithhCreditCard) { // Detalle de ventas Element detalleVentas =
         * doc.createElement("detalleVentas"); ventas.appendChild(detalleVentas); // TIPO
         * IDENTIFICACIÓN String strBPTaxIdType = (lstWithhCard.getBpartner().getSswhTaxidtype() ==
         * null ? "" : lstWithhCard.getBpartner().getSswhTaxidtype());
         * 
         * if (strBPTaxIdType.equals("R")) { strBPTaxIdType = "04"; } else if
         * (strBPTaxIdType.equals("P")) { strBPTaxIdType = "06"; } else if
         * (strBPTaxIdType.equals("D")) { strBPTaxIdType = "05"; } else if
         * (strBPTaxIdType.equals("C")) { strBPTaxIdType = "07"; }
         * 
         * if ((!strBPTaxIdType.equals("01") && !strBPTaxIdType.equals("02") &&
         * !strBPTaxIdType.equals("03") && !strBPTaxIdType.equals("04") &&
         * !strBPTaxIdType.equals("05") && !strBPTaxIdType.equals("06") &&
         * !strBPTaxIdType.equals("07") && !strBPTaxIdType.equals("08") &&
         * !strBPTaxIdType.equals("09") && !strBPTaxIdType.equals("10") &&
         * !strBPTaxIdType.equals("11") && !strBPTaxIdType.equals("12") &&
         * !strBPTaxIdType.equals("13") && !strBPTaxIdType.equals("14") &&
         * !strBPTaxIdType.equals("15") && !strBPTaxIdType.equals("16") &&
         * !strBPTaxIdType.equals("17") && !strBPTaxIdType.equals("18") &&
         * !strBPTaxIdType.equals("19"))) { throw new OBException(
         * "El código del Tipo de Identificación del Cliente es obligatorio (Retención Tarjetas).");
         * }
         * 
         * if (strBPTaxIdType == null || strBPTaxIdType.equals("")) { throw new OBException(
         * "No. de Identificación del Cliente(idCliente) es obligatorio (Retención Tarjetas)."); }
         * 
         * Element tpIdCliente = doc.createElement("tpIdCliente");
         * tpIdCliente.appendChild(doc.createTextNode(strBPTaxIdType));
         * detalleVentas.appendChild(tpIdCliente); // IDENTIFICACIÓN String strBPartnerTaxID =
         * (lstWithhCard.getBpartner().getTaxID() == null ? "" :
         * lstWithhCard.getBpartner().getTaxID()); if (strBPartnerTaxID == null ||
         * strBPartnerTaxID.equals("")) { throw new OBException(
         * "No. de Identificación del Cliente(idCliente) es obligatorio (Retención Tarjetas)."); }
         * Element idcliente = doc.createElement("idCliente");
         * idcliente.appendChild(doc.createTextNode(strBPartnerTaxID));
         * detalleVentas.appendChild(idcliente);
         * 
         * // PARTERELVTAS boolean boolParterelacionadaV =
         * lstWithhCard.getBpartner().getSSWHTaxpayer() .isRelatedPart();
         * 
         * String StrTmpParteRelacionada = "";
         * 
         * if (boolParterelacionadaV && !strBPTaxIdType.equals("07")) { StrTmpParteRelacionada =
         * "SI"; } else { StrTmpParteRelacionada = "NO"; }
         * 
         * Element parteRelVtas = doc.createElement("parteRelVtas"); // Thread.currentThread(); //
         * Thread.sleep(300L);
         * 
         * parteRelVtas.appendChild(doc.createTextNode(StrTmpParteRelacionada));
         * detalleVentas.appendChild(parteRelVtas);
         * 
         * // TIPOCOMPROBANTE String strTipoComprobante =
         * (lstWithhCard.getSswhTransactionType().getCode() == null ? "" :
         * lstWithhCard.getSswhTransactionType().getCode()).toString();
         * 
         * if (strTipoComprobante == null || strBPartnerTaxID.equals("")) { throw new OBException(
         * "Código de tipo de transacción no configurado (Retención Tarjetas)."); }
         * 
         * Element tipoComprobante = doc.createElement("tipoComprobante");
         * tipoComprobante.appendChild(doc.createTextNode(strTipoComprobante));
         * detalleVentas.appendChild(tipoComprobante);
         * 
         * // TIPO EMISIÓN boolean boolTipoEmision =
         * lstWithhCard.getSswhTransactionType().isElectronic(); String strTipoEmision = ""; if
         * (boolTipoEmision) { strTipoEmision = "E"; } else { strTipoEmision = "F"; }
         * 
         * Element tipoEm = doc.createElement("tipoEmision");
         * tipoEm.appendChild(doc.createTextNode(strTipoEmision));
         * detalleVentas.appendChild(tipoEm);
         * 
         * // NÚMERO COMPROBANTES
         * 
         * if (lstWithhCard.getVouchersNumber() == null ||
         * lstWithhCard.getVouchersNumber().equals("") ||
         * Integer.parseInt(lstWithhCard.getVouchersNumber().toString()) <= 0) { throw new
         * OBException(
         * "No. de Comprobantes Emitidos es obligatorio y debe ser mayor a cero (Retención Tarjetas)."
         * ); } String strVouchersNumber = lstWithhCard.getVouchersNumber().toString();
         * 
         * Element numeroComprobantes = doc.createElement("numeroComprobantes");
         * numeroComprobantes.appendChild(doc.createTextNode(strVouchersNumber));
         * detalleVentas.appendChild(numeroComprobantes); // baseNoGraIva String strCero = "0.00";
         * 
         * Element baseNoGraIva = doc.createElement("baseNoGraIva");
         * baseNoGraIva.appendChild(doc.createTextNode(strCero));
         * detalleVentas.appendChild(baseNoGraIva); // baseImponible Element baseImponible =
         * doc.createElement("baseImponible");
         * baseImponible.appendChild(doc.createTextNode(strCero));
         * detalleVentas.appendChild(baseImponible); // baseImpGrav Element baseImpGrav =
         * doc.createElement("baseImpGrav"); baseImpGrav.appendChild(doc.createTextNode(strCero));
         * detalleVentas.appendChild(baseImpGrav); // montoIva Element montoIva =
         * doc.createElement("montoIva"); montoIva.appendChild(doc.createTextNode(strCero));
         * detalleVentas.appendChild(montoIva); // montoIce Element montoIce =
         * doc.createElement("montoIce"); montoIce.appendChild(doc.createTextNode(strCero));
         * detalleVentas.appendChild(montoIce); // valorRetIva BigDecimal bdCero = new
         * BigDecimal(0);
         * 
         * if (lstWithhCard.getWithhVat().compareTo(bdCero) == 0 &&
         * lstWithhCard.getWithhRent().compareTo(bdCero) == 0) { throw new OBException(
         * "Al menos un valor de retención debe ser mayor a cero (Retención Tarjetas)."); } if
         * (lstWithhCard.getWithhVat().compareTo(bdCero) == -1 ||
         * lstWithhCard.getWithhRent().compareTo(bdCero) == -1) { throw new OBException(
         * "Los valores de retenciones no pueden ser negativos (Retención Tarjetas)."); } BigDecimal
         * bdValorRetIva = (lstWithhCard.getWithhVat() == null ? bdCero :
         * lstWithhCard.getWithhVat()); String strValorRetIva =
         * formateador.format(bdValorRetIva.doubleValue()).toString(); Element valorRetIva =
         * doc.createElement("valorRetIva");
         * valorRetIva.appendChild(doc.createTextNode(strValorRetIva));
         * detalleVentas.appendChild(valorRetIva); // valorRetRenta BigDecimal bdValorRetRenta =
         * (lstWithhCard.getWithhRent() == null ? bdCero : lstWithhCard.getWithhRent()); String
         * strValorRetRenta = formateador.format(bdValorRetRenta.doubleValue()).toString(); Element
         * valorRetRenta = doc.createElement("valorRetRenta");
         * valorRetRenta.appendChild(doc.createTextNode(strValorRetRenta));
         * detalleVentas.appendChild(valorRetRenta); // FORMA DE PAGO
         * 
         * if (lstWithhCard.getFINPaymentmethod() == null) { throw new
         * OBException("Forma de pago obligatoria (Retención Tarjetas)."); } Element formasDePago =
         * doc.createElement("formasDePago");
         * 
         * if (lstWithhCard.getFINPaymentmethod().getSswhCodeats() == null ||
         * lstWithhCard.getFINPaymentmethod().getSswhCodeats().equals("")) { throw new
         * OBException("Código ATS no configurado para el método de pago " +
         * lstWithhCard.getFINPaymentmethod() + " (Retención Tarjetas)."); }
         * 
         * Element formaPago = doc.createElement("formaPago"); formaPago.appendChild(
         * doc.createTextNode(lstWithhCard.getFINPaymentmethod().getSswhCodeats()));
         * formasDePago.appendChild(formaPago);
         * 
         * detalleVentas.appendChild(formasDePago); } }
         */

        // **** FIN TICKET 1917 A.M. 04/10/2018

      }

      // fin ventas

      // Sales of Stablishment Detail

      OBCriteria<SswhRptcSalesByStabOrg> ObjSalesByStabListDet = OBDal.getInstance()
          .createCriteria(SswhRptcSalesByStabOrg.class);
      ObjSalesByStabListDet
          .add(Restrictions.eq(SswhRptcSalesByStabOrg.PROPERTY_PROCESS, strProcessID));

      /*
       * ObjSalesByStabListDet.add(Restrictions.between("accountingDate", StartDate, EndDate));
       * 
       * if (!StrOrgID.isEmpty()) { ObjSalesByStabListDet
       * .add(Restrictions.eq(SswhRptcSalesByStab.PROPERTY_ORGANIZATION, SrchOrg)); }
       */

      // CONSULTA XSQL PARA AGRUPACION POR ESTABLECIMIENTO - FI

      String v_stardateDetStab = "";
      String v_enddateDetStab = "";

      // v_stardateDetStab = (StartDate != null) ? formatDate(StartDate) : "";
      // v_enddateDetStab = (EndDate != null) ? formatDate(EndDate) : "";

      /*
       * SswhRptcSalesByStablData dataStabl[]; if (!StrOrgID.isEmpty()) { dataStabl =
       * SswhRptcSalesByStablData.select(conn, v_stardateDetStab, v_enddateDetStab,
       * SrchOrg.getId().toString(), SrchOrg.getId().toString()); } else {
       * 
       * dataStabl = SswhRptcSalesByStablData.select(conn, v_stardateDetStab, v_enddateDetStab,
       * null, null);
       * 
       * }
       */
      // FIN CONSULTA XSQL PARA AGRUPACION POR ESTABLECIMIENTO - FI

      List<SswhRptcSalesByStabOrg> SalesbyStabList = ObjSalesByStabListDet.list();

      int CountRecordSalesbystab = SalesbyStabList.size();
      if (CountRecordSalesbystab > 0) {

        Element ventasEstablecimiento = doc.createElement("ventasEstablecimiento");
        rootElement.appendChild(ventasEstablecimiento);

        String StrcodEstab = "";
        String strcodestab = "";// aqui
        String StrventasEstab = "";
        String strventasestab = "";
        double strIvaComp = 0;
        double ventasestab = 0;

        // for (SswhRptcSalesByStab ListDetSalesVoideddet :
        // ObjSalesByStabListDet.list()) {
        for (SswhRptcSalesByStabOrg ListDetSalesVoideddet : ObjSalesByStabListDet.list()) {

          try {
            /*
             * strcodestab = ListDetSalesVoideddet.getEstablecimiento(); ventasestab = ventasestab +
             * ListDetSalesVoideddet.getValor().doubleValue();
             */// COMENTADO POR CAMBIO PARA AGRUPACION
            strcodestab = ListDetSalesVoideddet.getEstablecimiento();
            ventasestab = Double.valueOf(ListDetSalesVoideddet.getValor().toString());
            strIvaComp = Double.valueOf(ListDetSalesVoideddet.getCompensacion().toString());
          } catch (Exception e) {
          }

          Element ventaEst = doc.createElement("ventaEst");
          ventasEstablecimiento.appendChild(ventaEst);

          if (strcodestab.equals("") || strcodestab == null) {
            throw new OBException("Código del Establecimiento(codEstab) es obligatorio.");
          }
          StrcodEstab = strcodestab == null ? " " : strcodestab;
          Element codEstab = doc.createElement("codEstab");
          codEstab.appendChild(doc.createTextNode(StrcodEstab));
          ventaEst.appendChild(codEstab);

          strventasestab = formateador.format(ventasestab).toString() == null ? "0.00"
              : formateador.format(ventasestab);
          StrventasEstab = strventasestab;
          Element ventasEstab = doc.createElement("ventasEstab");
          ventasEstab.appendChild(doc.createTextNode(StrventasEstab));
          ventaEst.appendChild(ventasEstab);

          String strIvaCompensacion = "";
          strIvaCompensacion = formateador.format(strIvaComp).toString() == null ? "0.00"
              : formateador.format(strIvaComp);

          Element ivaComp = doc.createElement("ivaComp");
          ivaComp.appendChild(doc.createTextNode(strIvaCompensacion));
          ventaEst.appendChild(ivaComp);

        }
      } else {

        Element ventasEstablecimiento = doc.createElement("ventasEstablecimiento");
        rootElement.appendChild(ventasEstablecimiento);

        Element ventaEst = doc.createElement("ventaEst");
        ventasEstablecimiento.appendChild(ventaEst);

        Element codEstab = doc.createElement("codEstab");
        codEstab.appendChild(doc.createTextNode("001"));
        ventaEst.appendChild(codEstab);

        Element ventasEstab = doc.createElement("ventasEstab");
        ventasEstab.appendChild(doc.createTextNode("0.00"));
        ventaEst.appendChild(ventasEstab);

        Element ivaComp = doc.createElement("ivaComp");
        ivaComp.appendChild(doc.createTextNode("0.00"));
        ventaEst.appendChild(ivaComp);
      }

      // Exportaciones -- Actividad #9139

      OBCriteria<SswhInvoiceExportation> ObjExportation = OBDal.getInstance()
          .createCriteria(SswhInvoiceExportation.class);
      ObjExportation.add(Restrictions.eq(SswhInvoiceExportation.PROPERTY_PROCESS, strProcessID));

      if (ObjExportation.list().size() > 0) {

        Element exportaciones = doc.createElement("exportaciones");
        rootElement.appendChild(exportaciones);

        for (SswhInvoiceExportation lstExportation : ObjExportation.list()) {

          String strTypeClientIDEx = "";
          String strIDClientEx = "";
          String strParteRelEx = "";
          String strTypeClient = "";
          String strDenoClient = "";
          String strTypeRegimen = "";
          String strCountryEfectPaymentGeneral = "";
          String strCountryEfecPayment = "";
          String strExportationOf = "";
          String strIngExt = "";
          String strExtGravOtherCountry = "";
          String strVoucherType = "";
          Date strShippingDate = null;
          String strValueFOB = "";
          String strValueFOBVoucher = "";
          String strEstablishment = "";
          String strPointEmition = "";
          String strSequence = "";
          String strAuthorization = "";
          Date strDateEmition = null;
          try {
            strTypeClientIDEx = lstExportation.getClientType() == null ? ""
                : lstExportation.getClientType();
            strIDClientEx = lstExportation.getClientIdentif() == null ? ""
                : lstExportation.getClientIdentif();
            strParteRelEx = lstExportation.getRelationPart() == null ? ""
                : lstExportation.getRelationPart();
            strTypeClient = lstExportation.getIdentifierType() == null ? ""
                : lstExportation.getIdentifierType();
            strDenoClient = lstExportation.getDenoCli() == null ? "" : lstExportation.getDenoCli();
            strTypeRegimen = lstExportation.getRegimenType() == null ? ""
                : lstExportation.getRegimenType();
            strCountryEfectPaymentGeneral = lstExportation.getEfecPaymGeneral() == null ? ""
                : lstExportation.getEfecPaymGeneral();
            strCountryEfecPayment = lstExportation.getEfecPaymGeneral() == null ? ""
                : lstExportation.getEfecPaymGeneral(); //
            strExportationOf = lstExportation.getExportFrom() == null ? ""
                : lstExportation.getExportFrom();
            strIngExt = lstExportation.getTypeIngExt() == null ? ""
                : lstExportation.getTypeIngExt();
            strExtGravOtherCountry = lstExportation.getINGExtGravOthCountry() == null ? ""
                : lstExportation.getINGExtGravOthCountry();
            strVoucherType = lstExportation.getVoucherType() == null ? ""
                : lstExportation.getVoucherType();
            strShippingDate = lstExportation.getShippingDate();
            strValueFOB = lstExportation.getValueFob() == null ? "" : lstExportation.getValueFob();
            strValueFOBVoucher = lstExportation.getValueFobComprobante() == null ? ""
                : lstExportation.getValueFobComprobante();
            strEstablishment = lstExportation.getEstablishment() == null ? ""
                : lstExportation.getEstablishment();
            strPointEmition = lstExportation.getPointEmision() == null ? ""
                : lstExportation.getPointEmision();
            strSequence = lstExportation.getSecuence() == null ? "" : lstExportation.getSecuence();
            strAuthorization = lstExportation.getINVAuthorization() == null ? ""
                : lstExportation.getINVAuthorization();
            strDateEmition = lstExportation.getDateacct();

          } catch (Exception e) {

          }

          Element detalleExportaciones = doc.createElement("detalleExportaciones");
          exportaciones.appendChild(detalleExportaciones);

          strTypeClientIDEx = strTypeClientIDEx == null ? " " : strTypeClientIDEx;
          Element tpIdClienteEx = doc.createElement("tpIdClienteEx");
          tpIdClienteEx.appendChild(doc.createTextNode(strTypeClientIDEx));
          detalleExportaciones.appendChild(tpIdClienteEx);

          strIDClientEx = strIDClientEx == null ? " " : strIDClientEx;
          Element idClienteEx = doc.createElement("idClienteEx");
          idClienteEx.appendChild(doc.createTextNode(strIDClientEx));
          detalleExportaciones.appendChild(idClienteEx);

          strParteRelEx = strParteRelEx == null ? " " : strParteRelEx;
          Element parteRelExp = doc.createElement("parteRelExp");
          parteRelExp.appendChild(doc.createTextNode(strParteRelEx));
          detalleExportaciones.appendChild(parteRelExp);

          strTypeClient = strTypeClient == null ? " " : strTypeClient;
          Element tipoCli = doc.createElement("tipoCli");
          tipoCli.appendChild(doc.createTextNode(strTypeClient));
          detalleExportaciones.appendChild(tipoCli);

          strDenoClient = strDenoClient == null ? " " : strDenoClient;
          Element denoExpCli = doc.createElement("denoExpCli");
          denoExpCli.appendChild(doc.createTextNode(strDenoClient));
          detalleExportaciones.appendChild(denoExpCli);

          strTypeRegimen = strTypeRegimen == null ? " " : strTypeRegimen;
          Element tipoRegi = doc.createElement("tipoRegi");
          tipoRegi.appendChild(doc.createTextNode(strTypeRegimen));
          detalleExportaciones.appendChild(tipoRegi);

          strCountryEfectPaymentGeneral = strCountryEfectPaymentGeneral == null ? " "
              : strCountryEfectPaymentGeneral;
          Element paisEfecPagoGen = doc.createElement("paisEfecPagoGen");
          paisEfecPagoGen.appendChild(doc.createTextNode(strCountryEfectPaymentGeneral));
          detalleExportaciones.appendChild(paisEfecPagoGen);

          strCountryEfecPayment = strCountryEfecPayment == null ? " " : strCountryEfecPayment;
          Element paisEfecExp = doc.createElement("paisEfecExp");
          paisEfecExp.appendChild(doc.createTextNode(strCountryEfecPayment));
          detalleExportaciones.appendChild(paisEfecExp);

          strExportationOf = strExportationOf == null ? " " : strExportationOf;
          Element exportacionDe = doc.createElement("exportacionDe");
          exportacionDe.appendChild(doc.createTextNode(strExportationOf));
          detalleExportaciones.appendChild(exportacionDe);

          strIngExt = strIngExt == null ? " " : strIngExt;
          Element tipIngExt = doc.createElement("tipIngExt");
          tipIngExt.appendChild(doc.createTextNode(strIngExt));
          detalleExportaciones.appendChild(tipIngExt);

          strExtGravOtherCountry = strExtGravOtherCountry == null ? " " : strExtGravOtherCountry;
          Element ingExtGravOtroPais = doc.createElement("ingExtGravOtroPais");
          ingExtGravOtroPais.appendChild(doc.createTextNode(strExtGravOtherCountry));
          detalleExportaciones.appendChild(ingExtGravOtroPais);

          strVoucherType = strVoucherType == null ? " " : strVoucherType;
          Element tipoComprobante = doc.createElement("tipoComprobante");
          tipoComprobante.appendChild(doc.createTextNode(strVoucherType));
          detalleExportaciones.appendChild(tipoComprobante);

          String strShippingDate2 = ecFormat.format(strShippingDate);
          Element fechaEmbarque = doc.createElement("fechaEmbarque");
          fechaEmbarque.appendChild(doc.createTextNode(strShippingDate2));
          detalleExportaciones.appendChild(fechaEmbarque);

          strValueFOB = strValueFOB == null ? " " : strValueFOB;
          Element valorFOB = doc.createElement("valorFOB");
          valorFOB.appendChild(doc.createTextNode(strValueFOB));
          detalleExportaciones.appendChild(valorFOB);

          strValueFOBVoucher = strValueFOBVoucher == null ? " " : strValueFOBVoucher;
          Element valorFOBComprobante = doc.createElement("valorFOBComprobante");
          valorFOBComprobante.appendChild(doc.createTextNode(strValueFOBVoucher));
          detalleExportaciones.appendChild(valorFOBComprobante);

          strEstablishment = strEstablishment == null ? " " : strEstablishment;
          Element establecimiento = doc.createElement("establecimiento");
          establecimiento.appendChild(doc.createTextNode(strEstablishment));
          detalleExportaciones.appendChild(establecimiento);

          strPointEmition = strPointEmition == null ? " " : strPointEmition;
          Element puntoEmision = doc.createElement("puntoEmision");
          puntoEmision.appendChild(doc.createTextNode(strPointEmition));
          detalleExportaciones.appendChild(puntoEmision);

          strSequence = strSequence == null ? " " : strSequence;
          Element secuencial = doc.createElement("secuencial");
          secuencial.appendChild(doc.createTextNode(strSequence));
          detalleExportaciones.appendChild(secuencial);

          strAuthorization = strAuthorization == null ? " " : strAuthorization;
          Element autorizacion = doc.createElement("autorizacion");
          autorizacion.appendChild(doc.createTextNode(strAuthorization));
          detalleExportaciones.appendChild(autorizacion);

          String strDateEmition2 = ecFormat.format(strDateEmition);
          Element fechaEmision = doc.createElement("fechaEmision");
          fechaEmision.appendChild(doc.createTextNode(strDateEmition2));
          detalleExportaciones.appendChild(fechaEmision);

        }

      }

      // FIN: Exportaciones -- Actividad #9139

      // CREAR VISTA PENDIENTE
      OBCriteria<SswhWithholdingsVoided> ObjSalesVoidedListDet = OBDal.getInstance()
          .createCriteria(SswhWithholdingsVoided.class);
      ObjSalesVoidedListDet.add(Restrictions.between("withholdingdate", StartDate, EndDate));
      ObjSalesVoidedListDet.add(Restrictions.eq(SswhWithholdingsVoided.PROPERTY_PROCESSED, "Y"));
      /*
       * if (!StrOrgID.isEmpty()) { ObjSalesVoidedListDet
       * .add(Restrictions.eq(SswhWithholdingsVoided.PROPERTY_ORGANIZATION, SrchOrg)); }
       */

      List<SswhWithholdingsVoided> SalesVoidedList = ObjSalesVoidedListDet.list();

      int CountRecordVoided = SalesVoidedList.size();
      if (CountRecordVoided > 0) {

        Element anulados = doc.createElement("anulados");
        rootElement.appendChild(anulados);

        for (SswhWithholdingsVoided ListDetSalesVoideddet : ObjSalesVoidedListDet.list()) {

          String StrTipoComprobante = "";
          String strtipocomprobante = "";
          String StrEstablecimiento = "";
          String strestablecimiento = "";
          String StrPuntoEmision = "";
          String strpuntoemision = "";
          String StrSecuencialInicio = "";
          String strsecuencialinicio = "";
          String StrSecuencialFin = "";
          String strsecuencialfin = "";
          String StrAutorizacion = "";
          String strautorizacion = "";

          try {
            strtipocomprobante = ListDetSalesVoideddet.getDOCVoided();
            strestablecimiento = ListDetSalesVoideddet.getStablishment();
            strpuntoemision = ListDetSalesVoideddet.getShell();
            strsecuencialinicio = ListDetSalesVoideddet.getReferencenoFrom();
            strsecuencialfin = ListDetSalesVoideddet.getReferencenoTo();
            strautorizacion = ListDetSalesVoideddet.getAuthorizationno();

          } catch (Exception e) {
          }

          Element detalleAnulados = doc.createElement("detalleAnulados");
          anulados.appendChild(detalleAnulados);

          if (strtipocomprobante.equals("I")) { // factura
            StrTipoComprobante = "01";
          } else if (strtipocomprobante.equals("CN")) { // Nota de
                                                        // crédito
            StrTipoComprobante = "04";
          } else if (strtipocomprobante.equals("ND")) { // Nota de
                                                        // débito
            StrTipoComprobante = "05";
          } else if (strtipocomprobante.equals("IS")) { // Guía de
                                                        // Remisión
            StrTipoComprobante = "06";
          } else if (strtipocomprobante.equals("W")) { // Retenciones
            StrTipoComprobante = "07";
          } else if (strtipocomprobante.equals("LC")) { // Liquidación
                                                        // de
                                                        // Compras
            StrTipoComprobante = "03";
          }

          // StrTipoComprobante = strtipocomprobante == null ? " " :
          // strtipocomprobante;
          Element tipoComprobante = doc.createElement("tipoComprobante");
          tipoComprobante.appendChild(doc.createTextNode(StrTipoComprobante));
          detalleAnulados.appendChild(tipoComprobante);

          StrEstablecimiento = strestablecimiento == null ? " " : strestablecimiento;
          Element establecimiento = doc.createElement("establecimiento");
          establecimiento.appendChild(doc.createTextNode(StrEstablecimiento));
          detalleAnulados.appendChild(establecimiento);

          StrPuntoEmision = strpuntoemision == null ? " " : strpuntoemision;
          Element puntoEmision = doc.createElement("puntoEmision");
          puntoEmision.appendChild(doc.createTextNode(StrPuntoEmision));
          detalleAnulados.appendChild(puntoEmision);

          StrSecuencialInicio = strsecuencialinicio == null ? " " : strsecuencialinicio;
          Element secuencialInicio = doc.createElement("secuencialInicio");
          secuencialInicio.appendChild(doc.createTextNode(StrSecuencialInicio));
          detalleAnulados.appendChild(secuencialInicio);

          StrSecuencialFin = strsecuencialfin == null ? " " : strsecuencialfin;
          Element secuencialFin = doc.createElement("secuencialFin");
          secuencialFin.appendChild(doc.createTextNode(StrSecuencialFin));
          detalleAnulados.appendChild(secuencialFin);

          StrAutorizacion = strautorizacion == null ? " " : strautorizacion;
          Element autorizacion = doc.createElement("autorizacion");
          autorizacion.appendChild(doc.createTextNode(StrAutorizacion));
          detalleAnulados.appendChild(autorizacion);
        }
      }
      // fin

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      try {
        transformer = transformerFactory.newTransformer();
      } catch (TransformerConfigurationException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }

      DOMSource source = new DOMSource(doc);

      final Period period = OBDal.getInstance().get(Period.class, StrPeriodId);
      String nameFile = period.getIdentifier().toString().replace("-", "_") + "anexo.xml";

      StreamResult result = new StreamResult(new StringWriter());
      transformer.transform(source, result);

      response.setCharacterEncoding("UTF-8");
      response.setContentType("application/xml");
      response.setHeader("Content-Disposition", "attachment; filename=" + nameFile);
      PrintWriter out = response.getWriter();
      // out.println((Utility.fileToString(file.getAbsolutePath())));
      out.println(result.getWriter().toString());
      out.close();

      // message.setTitle(Utility.messageBD(conn, "ProcessOK", language));
      // message.setType("Success");
      // message.setMessage(Utility.messageBD(conn, "Success", language));
      /*
       * } catch (final Exception e) { e.printStackTrace(System.err);
       * 
       * message.setTitle(Utility.messageBD(conn, "Error", language)); message.setType("Error");
       * message.setMessage(e.getMessage() + e.fillInStackTrace());
       */
    } finally {
      bundle.setResult(message);
    }
  }

  protected String formatDate(java.util.Date date) {
    return new SimpleDateFormat((String) OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .get(KernelConstants.DATE_FORMAT_PROPERTY)).format(date);
  }

  protected void executeATSSql(CallableStatement callsql, String strPeriodID,
      String strOrganizationID, String strProcessID, String StrAtsType, String StrSemeter,
      String Stryear) {
    try {
      // Periodo
      callsql.setString(1, strPeriodID);
      // Organizacion
      callsql.setString(2, strOrganizationID);
      // Proceso ID
      callsql.setString(3, strProcessID);
      callsql.setString(4, StrAtsType);
      callsql.setString(5, StrSemeter);
      callsql.setString(6, Stryear);
      callsql.execute();

    } catch (Exception e) {
      throw new OBException(e.getMessage(), e);
    }
  }

  public static String getUUID(org.openbravo.database.ConnectionProvider connectionProvider)
      throws ServletException {
    String strSql = "";
    strSql = strSql + "       SELECT get_uuid() as name" + "       FROM dual";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "name");
      }
      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public static String getOnlyStrings(String s) {
    Pattern pattern = Pattern.compile("[^a-z A-Z]");
    Matcher matcher = pattern.matcher(s);
    String number = matcher.replaceAll("");
    return number;
  }

  public static String processSaveTableTmp(
      org.openbravo.database.ConnectionProvider connectionProvider, String strAdOrg,
      String strProcess, String strPeriod, String strTypeAts, String strSemester, String stryear)
      throws ServletException {
    String strSql = "";
    // String strOrgId = strAdOrg!=null
    strSql = strSql + " call SSWH_EXECUTE_ATS_SQL('" + strPeriod + "','" + strAdOrg + "','"
        + strProcess + "','" + strTypeAts + "','" + strSemester + "','" + stryear + "')";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);

      result = st.executeQuery();

      result.close();
      st.close();
    } catch (SQLException e) {
      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {
      // log4j.error("Exception in query: " + strSql + "Exception:"+ ex);
      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return ("OK");
  }
}
