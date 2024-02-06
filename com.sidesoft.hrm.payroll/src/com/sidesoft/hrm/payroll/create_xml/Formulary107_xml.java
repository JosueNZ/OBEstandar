package com.sidesoft.hrm.payroll.create_xml;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.List;

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
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.calendar.Year;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.xmlEngine.XmlEngine;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sidesoft.hrm.payroll.ssprformulary107detailv;

public class Formulary107_xml extends DalBaseProcess {

  public XmlEngine xmlEngine = null;
  public static String strDireccion;

  @SuppressWarnings({ "deprecation", "null" })
  public void doExecute(ProcessBundle bundle) throws Exception {

    final OBError message = new OBError();

    String language = OBContext.getOBContext().getLanguage().getLanguage();
    // ConnectionProvider conn = new DalConnectionProvider(false);

    ConnectionProvider conn = bundle.getConnection();

    // VariablesSecureApp varsAux = bundle.getContext().toVars();
    // HttpServletRequest request = RequestContext.get().getRequest();
    HttpServletResponse response = RequestContext.get().getResponse();

    try {

      // retrieve the parameters from the bundle
      // Recupera los parametros de la seción
      final String StrYearId = (String) bundle.getParams().get("cYearId");

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

      // Search of period in the "table c_period" for getting of the starting date and ending date
      // Busqueda del periodo en la tabla c_period para obtener la fecha inicio y fin.
      Year SrchYear = OBDal.getInstance().get(Year.class, StrYearId);

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
      Element rootElement = doc.createElement("rdep");
      doc.appendChild(rootElement);

      // DATOS FORMULARIO 107
      OBCriteria<ssprformulary107detailv> ObjFormulary107 = OBDal.getInstance()
          .createCriteria(ssprformulary107detailv.class);
      ObjFormulary107.add(Restrictions.eq("year", SrchYear));

      Formulary107Data formulary107Data[] = Formulary107Data.select(conn, StrYearId);

      if (formulary107Data != null && formulary107Data.length > 0) {

        List<ssprformulary107detailv> Formulary107List = ObjFormulary107.list();
        int CountFormulary = Formulary107List.size();

        // List data107[] =(List[]) formulary107Data;

        // DATOS DE CABECERA
        String StrRuc = Formulary107List.get(0).getTaxidOrg();
        if (StrRuc == null || StrRuc.equals("")) {
          throw new OBException(
              "Número de Registro Único del Contribuyente RUC del empleador es obligatorio");
        }

        Element numRuc = doc.createElement("numRuc");
        numRuc.appendChild(doc.createTextNode(StrRuc));
        rootElement.appendChild(numRuc);

        String StrYear = SrchYear.getFiscalYear();
        Element anio = doc.createElement("anio");
        anio.appendChild(doc.createTextNode(StrYear));
        rootElement.appendChild(anio);

        Element retRelDep = doc.createElement("retRelDep");
        rootElement.appendChild(retRelDep);

        // DETALLE DEL FORMULARIO
        if (CountFormulary > 0) {

          for (Formulary107Data SsprFormularyList : formulary107Data) {

            Element datRetRelDep = doc.createElement("datRetRelDep");
            retRelDep.appendChild(datRetRelDep);

            Element empleado = doc.createElement("empleado");
            datRetRelDep.appendChild(empleado);

            String StrTipoIdentificacion = "";
            String StrNumeroIdentificacion = "";
            String StrApeliidoTrab = "";
            String StrNombreTrab = "";
            String StrEstab = "";
            String StrResidenciaTrab = "";
            String StrPaisResidenciaTrab = "";
            String StrAplicaConvenio = "";
            String StrtipoTrabajDiscap = "";
            String StrporcentajeDiscap = "";
            String StrtipIdDiscapacidad = "";
            String StrIdDiscap = "";
            String strGalapagosBenef = "";
            String strworkwci = "";

            BigDecimal StrSueldo = null;
            BigDecimal StrSobresueldo = null;
            BigDecimal StrUtilidades = null;
            BigDecimal StrIngGravGen = null;
            BigDecimal StrImpRenta = null;
            BigDecimal StrDecimotercero = null;
            BigDecimal StrDecimoCuarto = null;
            BigDecimal StrFondosReserva = null;
            BigDecimal StrSalarioDigno = null;
            BigDecimal StrOtrosIngRentaGrav = null;
            BigDecimal StrIngGravConEsteEmpl = null;
            BigDecimal StrSistSalarioNet = null;
            BigDecimal StrportePersonaIess = null;
            BigDecimal StrAportePersonalOtrosEmpl = null;
            BigDecimal StrDeduccionVivienda = null;
            BigDecimal StrDeduccionSalud = null;
            BigDecimal StrDecuccionEdu = null;
            BigDecimal StrDeduccionAlimen = null;
            BigDecimal StrDeduccionVesti = null;
            BigDecimal StrDeduccionTuris = null;
            BigDecimal StrExoDiscapacidad = null;
            BigDecimal StrExoTercerEdad = null;
            BigDecimal StrBaseImp = null;
            BigDecimal StrImpRentaCausado = null;
            BigDecimal StrValorRetAsumidoOtrosEmpl = null;
            BigDecimal StrImpAsumidoEsteEmpl = null;
            BigDecimal StrImpRetenido = null;
            BigDecimal Strrebajagastospersonales = null;
            BigDecimal Strdespuesrebajagastospersonales = null;

            try {

              StrTipoIdentificacion = SsprFormularyList.tipoidentificacion;
              StrNumeroIdentificacion = SsprFormularyList.numeroidentificacion;
              StrApeliidoTrab = removeAcute(SsprFormularyList.empleadoapellido);
              StrNombreTrab = removeAcute(SsprFormularyList.empleadonombre);
              StrEstab = SsprFormularyList.codigoestab;
              StrResidenciaTrab = SsprFormularyList.pais;
              StrPaisResidenciaTrab = SsprFormularyList.codigopais;
              StrAplicaConvenio = SsprFormularyList.aplicaconvenio;
              StrtipoTrabajDiscap = SsprFormularyList.discapacitado;
              StrporcentajeDiscap = SsprFormularyList.porcentajediscapacidad;
              StrtipIdDiscapacidad = SsprFormularyList.tipoidentdiscapacidad;
              StrIdDiscap = SsprFormularyList.numeroidentifdiscapacidad;
              strGalapagosBenef = SsprFormularyList.galapagosbenf;
              strworkwci = SsprFormularyList.workwci;

              StrSueldo = new BigDecimal(
                  SsprFormularyList.sueldo.equals("") ? "0" : SsprFormularyList.sueldo);
              StrSobresueldo = new BigDecimal(
                  SsprFormularyList.bonos.equals("") ? "0" : SsprFormularyList.bonos);
              StrUtilidades = new BigDecimal(
                  SsprFormularyList.utilidades.equals("") ? "0" : SsprFormularyList.utilidades);
              StrIngGravGen = new BigDecimal(SsprFormularyList.ingresosgravados.equals("") ? "0"
                  : SsprFormularyList.ingresosgravados);
              StrImpRenta = new BigDecimal(SsprFormularyList.impuestorenta.equals("") ? "0"
                  : SsprFormularyList.impuestorenta);
              StrDecimotercero = new BigDecimal(SsprFormularyList.decimotercero.equals("") ? "0"
                  : SsprFormularyList.decimotercero);
              StrDecimoCuarto = new BigDecimal(
                  SsprFormularyList.decimocuarto.equals("") ? "0" : SsprFormularyList.decimocuarto);
              StrFondosReserva = new BigDecimal(SsprFormularyList.fondosreserva.equals("") ? "0"
                  : SsprFormularyList.fondosreserva);
              StrSalarioDigno = new BigDecimal(
                  SsprFormularyList.compensacionsalariodigno.equals("") ? "0"
                      : SsprFormularyList.compensacionsalariodigno);
              StrOtrosIngRentaGrav = new BigDecimal(SsprFormularyList.impuestorenta.equals("") ? "0"
                  : SsprFormularyList.impuestorenta);
              StrIngGravConEsteEmpl = new BigDecimal(
                  SsprFormularyList.ingresosgrav349.equals("") ? "0"
                      : SsprFormularyList.ingresosgrav349);
              StrSistSalarioNet = new BigDecimal(SsprFormularyList.ssprNetsalarysys.equals("") ? "0"
                  : SsprFormularyList.ssprNetsalarysys);
              StrportePersonaIess = new BigDecimal(SsprFormularyList.aportepersonal.equals("") ? "0"
                  : SsprFormularyList.aportepersonal);
              StrAportePersonalOtrosEmpl = new BigDecimal(
                  SsprFormularyList.aportepersonalo.equals("") ? "0"
                      : SsprFormularyList.aportepersonalo);
              StrDeduccionVivienda = new BigDecimal(
                  SsprFormularyList.gastosvivienda.equals("") ? "0"
                      : SsprFormularyList.gastosvivienda);
              StrDeduccionSalud = new BigDecimal(
                  SsprFormularyList.gastossalud.equals("") ? "0" : SsprFormularyList.gastossalud);
              StrDecuccionEdu = new BigDecimal(SsprFormularyList.gastoseducacion.equals("") ? "0"
                  : SsprFormularyList.gastoseducacion);
              StrDeduccionAlimen = new BigDecimal(
                  SsprFormularyList.gastoalimentacion.equals("") ? "0"
                      : SsprFormularyList.gastoalimentacion);
              StrDeduccionVesti = new BigDecimal(SsprFormularyList.gastosvestimenta.equals("") ? "0"
                  : SsprFormularyList.gastosvestimenta);
              StrDeduccionTuris = new BigDecimal(SsprFormularyList.gastosturismo.equals("") ? "0"
                      : SsprFormularyList.gastosturismo);              
              StrExoDiscapacidad = new BigDecimal(
                  SsprFormularyList.exoneracionpordiscapacidad.equals("") ? "0"
                      : SsprFormularyList.exoneracionpordiscapacidad);
              if (StrExoDiscapacidad.compareTo(BigDecimal.ZERO) == 0) {
	              StrExoTercerEdad = new BigDecimal(
	                  SsprFormularyList.exoneracionportercerasedad.equals("") ? "0"
	                      : SsprFormularyList.exoneracionportercerasedad);
              }else {
            	  StrExoTercerEdad = BigDecimal.ZERO;
              }
              StrBaseImp = new BigDecimal(SsprFormularyList.baseimponiblegrav.equals("") ? "0"
                  : SsprFormularyList.baseimponiblegrav);
              StrImpRentaCausado = new BigDecimal(
                  SsprFormularyList.impuestorentacausado.equals("") ? "0"
                      : SsprFormularyList.impuestorentacausado);
              StrValorRetAsumidoOtrosEmpl = new BigDecimal(
                  SsprFormularyList.valorimpret403.equals("") ? "0"
                      : SsprFormularyList.valorimpret403);
              StrImpAsumidoEsteEmpl = new BigDecimal(
                  SsprFormularyList.valorimpasumido405.equals("") ? "0"
                      : SsprFormularyList.valorimpasumido405);
              StrImpRetenido = new BigDecimal(
                  SsprFormularyList.valorimprettrabajador.equals("") ? "0"
                      : SsprFormularyList.valorimprettrabajador);
              Strrebajagastospersonales = new BigDecimal(SsprFormularyList.rebajagastospersonales.equals("") ? "0"
                      : SsprFormularyList.rebajagastospersonales);
              Strdespuesrebajagastospersonales = new BigDecimal(SsprFormularyList.despuesrebajagastospersonales.equals("") ? "0"
                      : SsprFormularyList.despuesrebajagastospersonales);

            } catch (Exception e) {
            }

            if (StrTipoIdentificacion == null || StrTipoIdentificacion.equals("")) {
              throw new OBException(
                  "El campo Tipo de Identificacion del trabajador(tipIdRet) es obligatorio. Empleado "
                      + StrApeliidoTrab);
            }

            String strGalapagosBenefnew = strGalapagosBenef == null ? " " : strGalapagosBenef;
            Element benGalpg = doc.createElement("benGalpg");
            benGalpg.appendChild(doc.createTextNode(strGalapagosBenefnew));
            empleado.appendChild(benGalpg);

            String StrTipoIdentificacionnew = StrTipoIdentificacion == null ? " "
                : StrTipoIdentificacion;
            Element tipIdRet = doc.createElement("tipIdRet");
            tipIdRet.appendChild(doc.createTextNode(StrTipoIdentificacionnew));
            empleado.appendChild(tipIdRet);

            if (StrNumeroIdentificacion == null || StrNumeroIdentificacion.equals("")) {
              throw new OBException(
                  "El campo Número de identificación del trabajador (idRet) es obligatorio. Empleado "
                      + StrApeliidoTrab);
            }
            String StrNumeroIdentificacionnew = StrNumeroIdentificacion == null ? " "
                : StrNumeroIdentificacion;
            Element idRet = doc.createElement("idRet");
            idRet.appendChild(doc.createTextNode(StrNumeroIdentificacionnew));
            empleado.appendChild(idRet);

            String StrApeliidoTrabnew = StrApeliidoTrab == null ? " " : StrApeliidoTrab;
            Element apellidoTrab = doc.createElement("apellidoTrab");
            apellidoTrab.appendChild(doc.createTextNode(StrApeliidoTrabnew));
            empleado.appendChild(apellidoTrab);

            String StrNombreTrabnew = StrNombreTrab == null ? " " : StrNombreTrab;
            Element nombreTrab = doc.createElement("nombreTrab");
            nombreTrab.appendChild(doc.createTextNode(StrNombreTrabnew));
            empleado.appendChild(nombreTrab);

            if (StrEstab == null || StrEstab.equals("")) {
              throw new OBException(
                  "El campo Código del establecimiento (estab) es obligatorio. Empleado "
                      + StrApeliidoTrab);
            }
            String StrEstabnew = StrEstab == null ? " " : StrEstab;
            Element estab = doc.createElement("estab");
            estab.appendChild(doc.createTextNode(StrEstabnew));
            empleado.appendChild(estab);

            if (StrResidenciaTrab == null || StrResidenciaTrab.equals("")) {
              throw new OBException(
                  "El campo Residencia del trabajador (residenciaTrab) es obligatorio. Empleado "
                      + StrApeliidoTrab);
            }
            String StrResidenciaTrabnew = StrResidenciaTrab == null ? " " : StrResidenciaTrab;
            Element residenciaTrab = doc.createElement("residenciaTrab");
            residenciaTrab.appendChild(doc.createTextNode(StrResidenciaTrabnew));
            empleado.appendChild(residenciaTrab);

            if (StrPaisResidenciaTrab == null || StrPaisResidenciaTrab.equals("")) {
              throw new OBException(
                  "El campo Pais de Residencia (paisResidencia) es obligatorio. Empleado "
                      + StrApeliidoTrab);
            }
            String StrPaisResidenciaTrabnew = StrPaisResidenciaTrab == null
                || StrPaisResidenciaTrab.equals("") ? " " : StrPaisResidenciaTrab;
            Element paisResidencia = doc.createElement("paisResidencia");
            paisResidencia.appendChild(doc.createTextNode(StrPaisResidenciaTrabnew));
            empleado.appendChild(paisResidencia);

            String StrAplicaConvenionew = StrAplicaConvenio == null ? "NA" : StrAplicaConvenio;
            Element aplicaConvenio = doc.createElement("aplicaConvenio");
            aplicaConvenio.appendChild(doc.createTextNode(StrAplicaConvenionew));
            empleado.appendChild(aplicaConvenio);

            String StrtipoTrabajDiscapnew = StrtipoTrabajDiscap == null ? " " : StrtipoTrabajDiscap;
            Element tipoTrabajDiscap = doc.createElement("tipoTrabajDiscap");
            tipoTrabajDiscap.appendChild(doc.createTextNode(StrtipoTrabajDiscapnew));
            empleado.appendChild(tipoTrabajDiscap);

            String StrporcentajeDiscapnew = StrporcentajeDiscap == null ? " " : StrporcentajeDiscap;
            Element porcentajeDiscap = doc.createElement("porcentajeDiscap");
            porcentajeDiscap.appendChild(doc.createTextNode(StrporcentajeDiscapnew));
            empleado.appendChild(porcentajeDiscap);

            String StrtipIdDiscapacidadnew = StrtipIdDiscapacidad == null
                || StrtipIdDiscapacidad.equals("") ? "N" : StrtipIdDiscapacidad;
            Element tipIdDiscap = doc.createElement("tipIdDiscap");
            tipIdDiscap.appendChild(doc.createTextNode(StrtipIdDiscapacidadnew));
            empleado.appendChild(tipIdDiscap);

            String StrIdDiscapnew = StrIdDiscap == null || StrIdDiscap.equals("") ? "999"
                : StrIdDiscap;
            Element idDiscap = doc.createElement("idDiscap");
            idDiscap.appendChild(doc.createTextNode(StrIdDiscapnew));
            empleado.appendChild(idDiscap);

            String StrSueldoNew = formateador.format(StrSueldo).toString() == null ? "0.00"
                : formateador.format(StrSueldo);
            Element suelSal = doc.createElement("suelSal");
            suelSal.appendChild(doc.createTextNode(StrSueldoNew));
            datRetRelDep.appendChild(suelSal);

            String StrSobresueldoNew = formateador.format(StrSobresueldo).toString() == null
                ? "0.00"
                : formateador.format(StrSobresueldo);
            Element sobSuelComRemu = doc.createElement("sobSuelComRemu");
            sobSuelComRemu.appendChild(doc.createTextNode(String.valueOf(StrSobresueldoNew)));
            datRetRelDep.appendChild(sobSuelComRemu);

            String StrUtilidadesNew = formateador.format(StrUtilidades).toString() == null ? "0.00"
                : formateador.format(StrUtilidades);
            Element partUtil = doc.createElement("partUtil");
            partUtil.appendChild(doc.createTextNode(String.valueOf(StrUtilidadesNew)));
            datRetRelDep.appendChild(partUtil);

            String StrIngGravGenNew = formateador.format(StrIngGravGen).toString() == null ? "0.00"
                : formateador.format(StrIngGravGen);
            Element intGrabGen = doc.createElement("intGrabGen");
            intGrabGen.appendChild(doc.createTextNode(StrIngGravGenNew));
            datRetRelDep.appendChild(intGrabGen);

            String StrImpRentaNew = formateador.format(StrImpRenta).toString() == null ? "0.00"
                : formateador.format(StrImpRenta);
            Element impRentEmpl = doc.createElement("impRentEmpl");
            impRentEmpl.appendChild(doc.createTextNode(StrImpRentaNew));
            datRetRelDep.appendChild(impRentEmpl);

            String StrDecimoterceroNew = formateador.format(StrDecimotercero).toString() == null
                ? "0.00"
                : formateador.format(StrDecimotercero);
            Element decimTer = doc.createElement("decimTer");
            decimTer.appendChild(doc.createTextNode(String.valueOf(StrDecimoterceroNew)));
            datRetRelDep.appendChild(decimTer);

            String StrDecimoCuartoNew = formateador.format(StrDecimoCuarto).toString() == null
                ? "0.00"
                : formateador.format(StrDecimoCuarto);
            Element decimCuar = doc.createElement("decimCuar");
            decimCuar.appendChild(doc.createTextNode(StrDecimoCuartoNew));
            datRetRelDep.appendChild(decimCuar);

            String StrFondosReservaNew = formateador.format(StrFondosReserva).toString() == null
                ? "0.00"
                : formateador.format(StrFondosReserva);
            Element fondoReserva = doc.createElement("fondoReserva");
            fondoReserva.appendChild(doc.createTextNode(StrFondosReservaNew));
            datRetRelDep.appendChild(fondoReserva);

            String StrSalarioDignoNew = formateador.format(StrSalarioDigno).toString() == null
                ? "0.00"
                : formateador.format(StrSalarioDigno);
            Element salarioDigno = doc.createElement("salarioDigno");
            salarioDigno.appendChild(doc.createTextNode(StrSalarioDignoNew));
            datRetRelDep.appendChild(salarioDigno);

            String StrOtrosIngRentaGravNew = formateador.format(StrOtrosIngRentaGrav)
                .toString() == null ? "0.00" : formateador.format(StrOtrosIngRentaGrav);
            Element otrosIngRenGrav = doc.createElement("otrosIngRenGrav");
            otrosIngRenGrav.appendChild(doc.createTextNode(StrOtrosIngRentaGravNew));
            datRetRelDep.appendChild(otrosIngRenGrav);

            String StrIngGravConEsteEmplNew = formateador.format(StrIngGravConEsteEmpl)
                .toString() == null ? "0.00" : formateador.format(StrIngGravConEsteEmpl);
            Element ingGravConEsteEmpl = doc.createElement("ingGravConEsteEmpl");
            ingGravConEsteEmpl.appendChild(doc.createTextNode(StrIngGravConEsteEmplNew));
            datRetRelDep.appendChild(ingGravConEsteEmpl);

            String StrSistSalarioNetNew = StrSistSalarioNet.toString() == null ? "0"
                : StrSistSalarioNet.toString();
            Element sisSalNet = doc.createElement("sisSalNet");
            sisSalNet.appendChild(doc.createTextNode(StrSistSalarioNetNew));
            datRetRelDep.appendChild(sisSalNet);

            String StrportePersonaIessNew = formateador.format(StrportePersonaIess)
                .toString() == null ? "0.00" : formateador.format(StrportePersonaIess);
            Element apoPerIess = doc.createElement("apoPerIess");
            apoPerIess.appendChild(doc.createTextNode(StrportePersonaIessNew));
            datRetRelDep.appendChild(apoPerIess);

            String StrAportePersonalOtrosEmplNew = formateador.format(StrAportePersonalOtrosEmpl)
                .toString() == null ? "0.00" : formateador.format(StrAportePersonalOtrosEmpl);
            Element aporPerIessConOtrosEmpls = doc.createElement("aporPerIessConOtrosEmpls");
            aporPerIessConOtrosEmpls.appendChild(doc.createTextNode(StrAportePersonalOtrosEmplNew));
            datRetRelDep.appendChild(aporPerIessConOtrosEmpls);

            String StrDeduccionViviendaNew = formateador.format(StrDeduccionVivienda)
                .toString() == null ? "0.00" : formateador.format(StrDeduccionVivienda);
            Element deducVivienda = doc.createElement("deducVivienda");
            deducVivienda.appendChild(doc.createTextNode(StrDeduccionViviendaNew));
            datRetRelDep.appendChild(deducVivienda);

            String StrDeduccionSaludNew = formateador.format(StrDeduccionSalud).toString() == null
                ? "0.00"
                : formateador.format(StrDeduccionSalud);
            Element deducSalud = doc.createElement("deducSalud");
            deducSalud.appendChild(doc.createTextNode(StrDeduccionSaludNew));
            datRetRelDep.appendChild(deducSalud);

            String StrDecuccionEduNew = formateador.format(StrDecuccionEdu).toString() == null
                ? "0.00"
                : formateador.format(StrDecuccionEdu);
            Element deducEducartcult = doc.createElement("deducEducartcult");
            deducEducartcult.appendChild(doc.createTextNode(StrDecuccionEduNew));
            datRetRelDep.appendChild(deducEducartcult);

            String StrDeduccionAlimenNew = formateador.format(StrDeduccionAlimen).toString() == null
                ? "0.00"
                : formateador.format(StrDeduccionAlimen);
            Element deducAliement = doc.createElement("deducAliement");
            deducAliement.appendChild(doc.createTextNode(StrDeduccionAlimenNew));
            datRetRelDep.appendChild(deducAliement);

            String StrDeduccionVestiNew = formateador.format(StrDeduccionVesti).toString() == null
                ? "0.00"
                : formateador.format(StrDeduccionVesti);
            Element deducVestim = doc.createElement("deducVestim");
            deducVestim.appendChild(doc.createTextNode(StrDeduccionVestiNew));
            datRetRelDep.appendChild(deducVestim);

            String StrDeduccionTurismoNew = formateador.format(StrDeduccionTuris).toString() == null
                    ? "0.00"
                    : formateador.format(StrDeduccionTuris);
                Element deducTuris = doc.createElement("deduccionTurismo");
                deducTuris.appendChild(doc.createTextNode(StrDeduccionTurismoNew));
                datRetRelDep.appendChild(deducTuris);

            String StrExoDiscapacidadNew = formateador.format(StrExoDiscapacidad).toString() == null
                ? "0.00"
                : formateador.format(StrExoDiscapacidad);
            Element exoDiscap = doc.createElement("exoDiscap");
            exoDiscap.appendChild(doc.createTextNode(StrExoDiscapacidadNew));
            datRetRelDep.appendChild(exoDiscap);

            String StrExoTercerEdadNew = formateador.format(StrExoTercerEdad).toString() == null
                ? "0.00"
                : formateador.format(StrExoTercerEdad);
            Element exoTerEd = doc.createElement("exoTerEd");
            exoTerEd.appendChild(doc.createTextNode(StrExoTercerEdadNew));
            datRetRelDep.appendChild(exoTerEd);

            String StrBaseImpNew = formateador.format(StrBaseImp).toString() == null ? "0.00"
                : formateador.format(StrBaseImp);
            Element basImp = doc.createElement("basImp");
            basImp.appendChild(doc.createTextNode(StrBaseImpNew));
            datRetRelDep.appendChild(basImp);

            String StrImpRentaCausadoNew = formateador.format(StrImpRentaCausado).toString() == null
                ? "0.00"
                : formateador.format(StrImpRentaCausado);
            Element impRentCaus = doc.createElement("impRentCaus");
            impRentCaus.appendChild(doc.createTextNode(StrImpRentaCausadoNew));
            datRetRelDep.appendChild(impRentCaus);
			
            String StrrebajagastospersonalesNew = formateador.format(Strrebajagastospersonales).toString() == null
                ? "0.00"
                : formateador.format(Strrebajagastospersonales);
            Element rebajaGastosPersonales = doc.createElement("rebajaGastosPersonales");
            rebajaGastosPersonales.appendChild(doc.createTextNode(StrrebajagastospersonalesNew));
            datRetRelDep.appendChild(rebajaGastosPersonales);
			
            String StrdespuesrebajagastospersonalesNew = formateador.format(Strdespuesrebajagastospersonales).toString() == null
                ? "0.00"
                : formateador.format(Strdespuesrebajagastospersonales);
            Element impuestoRentaRebajaGastosPersonales = doc.createElement("impuestoRentaRebajaGastosPersonales");
            impuestoRentaRebajaGastosPersonales.appendChild(doc.createTextNode(StrdespuesrebajagastospersonalesNew));
            datRetRelDep.appendChild(impuestoRentaRebajaGastosPersonales);

            String StrValorRetAsumidoOtrosEmplNew = formateador.format(StrValorRetAsumidoOtrosEmpl)
                .toString() == null ? "0.00" : formateador.format(StrValorRetAsumidoOtrosEmpl);
            Element valRetAsuOtrosEmpls = doc.createElement("valRetAsuOtrosEmpls");
            valRetAsuOtrosEmpls.appendChild(doc.createTextNode(StrValorRetAsumidoOtrosEmplNew));
            datRetRelDep.appendChild(valRetAsuOtrosEmpls);

            String StrImpAsumidoEsteEmplNew = formateador.format(StrImpAsumidoEsteEmpl)
                .toString() == null ? "0.00" : formateador.format(StrImpAsumidoEsteEmpl);
            Element valImpAsuEsteEmpl = doc.createElement("valImpAsuEsteEmpl");
            valImpAsuEsteEmpl.appendChild(doc.createTextNode(StrImpAsumidoEsteEmplNew));
            datRetRelDep.appendChild(valImpAsuEsteEmpl);

            String StrImpRetenidoNew = formateador.format(StrImpRetenido).toString() == null
                ? "0.00"
                : formateador.format(StrImpRetenido);
            Element valRet = doc.createElement("valRet");
            valRet.appendChild(doc.createTextNode(StrImpRetenidoNew));
            datRetRelDep.appendChild(valRet);

            // FIN CAMBIOS NUEVO ANEXO 2015 - (FI)

          }
        }

        // Third Tag of the sales detailgetMontoRetRenta

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

        // final Period period = OBDal.getInstance().get(Period.class, StrPeriodId);
        String nameFile = "RDEP" + StrYear + ".xml";

        StreamResult result = new StreamResult(new StringWriter());
        transformer.transform(source, result);

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/xml");
        response.setHeader("Content-Disposition", "attachment; filename=" + nameFile);
        PrintWriter out = response.getWriter();
        // out.println((Utility.fileToString(file.getAbsolutePath())));
        out.println(result.getWriter());
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
      }
    } finally {
      bundle.setResult(message);
    }
  }

  protected String formatDate(java.util.Date date) {
    return new SimpleDateFormat((String) OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .get(KernelConstants.DATE_FORMAT_PROPERTY)).format(date);
  }

  public static String removeAcute(String input) {
    // Cadena de caracteres original a sustituir.
    String original = "áàäéèëíìïóòöúùuñÁÀÄÉÈËÍÌÏÓÒÖÚÙÜÑçÇ";
    // Cadena de caracteres ASCII que reemplazarán los originales.
    String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
    String output = input;
    for (int i = 0; i < original.length(); i++) {
      // Reemplazamos los caracteres especiales.
      output = output.replace(original.charAt(i), ascii.charAt(i));
    } // for i
    return output;
  }// removeAcute
}

