// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 27/12/2011 14:02:08
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   GeneradorCfdi.java

package ec.cusoft.facturaec.generador;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.log4j.BasicConfigurator;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.utils.FormatUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ec.cusoft.facturaec.EEIInvoiceLog;
import ec.cusoft.facturaec.EEIMailServer;
import ec.cusoft.facturaec.EEIParamFacturae;
import ec.cusoft.facturaec.autorizacion.AutorizacionComprobantes;
import ec.cusoft.facturaec.autorizacion.AutorizacionComprobantesService;
import ec.cusoft.facturaec.autorizacion.RespuestaComprobante;
import ec.cusoft.facturaec.email.EMailUtils;
import ec.cusoft.facturaec.pruebas.autorizacion.Mensaje;
import ec.cusoft.facturaec.recepcion.RecepcionComprobantes;
import ec.cusoft.facturaec.recepcion.RecepcionComprobantesService;
import ec.cusoft.facturaec.recepcion.RespuestaSolicitud;
import ec.cusoft.facturaec.utils.CryptUtils;
import ec.cusoft.facturaec.utils.DOMUtils;
import ec.cusoft.facturaec.utils.FechaUtils;
import ec.cusoft.facturaec.utils.FileUtils;
import es.mityc.firmaJava.libreria.utilidades.UtilidadTratarNodo;
import es.mityc.firmaJava.libreria.xades.DataToSign;
import es.mityc.firmaJava.libreria.xades.EnumFormatoFirma;
import es.mityc.firmaJava.libreria.xades.FirmaXML;
import es.mityc.firmaJava.libreria.xades.XAdESSchemas;
import es.mityc.javasign.xml.refs.InternObjectToSign;
import es.mityc.javasign.xml.refs.ObjectToSign;

@SuppressWarnings("deprecation")
public class ECWSClient {

  public EEIParamFacturae getParamFEC(Invoice invoice) throws ServletException {
    // Parametros Facturae
    OBCriteria<EEIParamFacturae> paramsC = OBDal.getInstance()
        .createCriteria(EEIParamFacturae.class);
    // paramsC.add(Restrictions.eq("organization", invoice.getOrganization()));
    paramsC.add(Restrictions.eq("active", true));

    List<EEIParamFacturae> paramsL = paramsC.list();
    if (paramsL.size() == 0)
      throw new ServletException(
          "No existen parametros de Facturación Electrónica para la Organización "
              + invoice.getOrganization().getName());
    return paramsL.get(0);
  }

  @SuppressWarnings("unused")
  public void firmarEC(File file, EEIParamFacturae param) throws Exception {
    // FIRMADO DE ECUADOR
    BasicConfigurator.configure();
    Object[] cert_key = CryptUtils.getPrivatekeyAndCertificate(param.getDIRCertificado(),
        FormatUtilities.encryptDecrypt(param.getPassword(), false));
    java.security.cert.X509Certificate certificate = (java.security.cert.X509Certificate) cert_key[0];
    byte ext[] = certificate.getExtensionValue("1.3.6.1.4.1.37947.3.11");
    String ruc = new String(ext);
    char ruc0 = ruc.charAt(0);
    char ruc1 = ruc.charAt(1);
    char ruc2 = ruc.charAt(2);
    char ruc3 = ruc.charAt(3);
    char ruc4 = ruc.charAt(4);
    char ruc5 = ruc.charAt(5);
    char ruc6 = ruc.charAt(6);
    char ruc7 = ruc.charAt(7);
    char ruc8 = ruc.charAt(8);
    char ruc9 = ruc.charAt(9);
    char ruc10 = ruc.charAt(10);
    char ruc11 = ruc.charAt(11);
    char ruc12 = ruc.charAt(12);
    char ruc13 = ruc.charAt(13);
    char ruc14 = ruc.charAt(14);
    char ruc15 = ruc.charAt(15);
    char ruc16 = ruc.charAt(16);
    String ruct = (new StringBuilder()).append("").append(ruc4).append(ruc5).append(ruc6)
        .append(ruc7).append(ruc8).append(ruc9).append(ruc10).append(ruc11).append(ruc12)
        .append(ruc13).append(ruc14).append(ruc15).append(ruc16).toString();
    if (certificate != null) {
      DataToSign dataToSign = new DataToSign();
      dataToSign.setXadesFormat(EnumFormatoFirma.XAdES_BES);
      dataToSign.setEsquema(XAdESSchemas.XAdES_132);
      dataToSign.setAddPolicy(false);
      dataToSign.setXMLEncoding("UTF-8");
      dataToSign.setEnveloped(true);
      dataToSign.addObject(new ObjectToSign(new InternObjectToSign("comprobante"),
          "Documento de ejemplo", null, "text/xml", null));
      dataToSign.setParentSignNode("comprobante");
      dataToSign.setDocument(DOMUtils.loadXML(file.getAbsolutePath()));
      File fichero = new File(file.getAbsolutePath());
      String nombre = fichero.getName();
      /*
       * if(!"1768141520001".equals(ruct)) ESTE NO { throw new ServletException(
       * "LICENCIA  DE USO EXCLUSIVO PARA OAE, COMPROBANTE NO PROCESADO POR INVIOLABILIDAD DE LICENCIA!!"
       * ); } else
       */
      {
        Object res[] = (new FirmaXML()).signFile(certificate, dataToSign, (PrivateKey) cert_key[1],
            (java.security.Provider) cert_key[2]);
        FileOutputStream archivo = new FileOutputStream(
            file.getAbsolutePath().replace("nofirmado", "firmado"));
        UtilidadTratarNodo.saveDocumentToOutputStream((Document) res[0], archivo, true);
        archivo.close();
      }
    }

  }

  public void saveOBLog(Invoice invoice, String description, String type) throws ServletException {
    OBContext.setAdminMode();
    try {
      OBCriteria<EEIInvoiceLog> contLog = OBDal.getInstance().createCriteria(EEIInvoiceLog.class);
      contLog.add(Restrictions.eq("invoice", invoice));
      int cont = contLog.list().size();

      EEIInvoiceLog log = OBProvider.getInstance().get(EEIInvoiceLog.class);
      log.setInvoice(invoice);
      log.setLine(Long.parseLong(String.valueOf(cont * 10 + 10)));
      log.setLogtype(type);
      log.setDescription(description);
      OBDal.getInstance().save(log);
      OBDal.getInstance().flush();
    } catch (Exception exc) {
      throw new ServletException("Database connection Error");
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public byte[] archivoToByte(File file) throws IOException {
    byte[] buffer = new byte[(int) file.length()];
    InputStream ios = null;
    try {
      ios = new FileInputStream(file);
      if (ios.read(buffer) == -1) {
        throw new IOException("EOF reached while trying to read the whole file");
      }
    } finally {
      try {
        if (ios != null) {
          ios.close();
        }
      } catch (IOException e) {
        //
      }
    }

    return buffer;
  }

  public File go(Document docomprobante, String rootDirectory, String claveAcceso, Invoice invoice,
      String docName, String ftpFolder)
      throws Exception, JAXBException, ParserConfigurationException, SAXException, IOException,
      TransformerConfigurationException, TransformerException, ServletException,
      ClassNotFoundException, XMLStreamException, FactoryConfigurationError {
    try {
      EEIParamFacturae param = getParamFEC(invoice);

      System.out.println("0- Comenzando Proceso de Comunicación con Web Services");

      File file = FileUtils.salvarNoFirmado(docomprobante, invoice, rootDirectory, param, docName);
      firmarEC(file, param);

      byte[] bFirmado = FileUtils.readFile(file.getAbsolutePath().replace("nofirmado", "firmado"));
      byte[] bFirmado2 = archivoToByte(
          new java.io.File(file.getAbsolutePath().replace("nofirmado", "firmado")));
      // PERSISTENCIA
      invoice.setEeiCodigo(claveAcceso);
      OBDal.getInstance().save(invoice);
      OBDal.getInstance().flush();
      /*
       * if (invoice.getEeiCodigo() == null) { invoice.setEeiCodigo(claveAcceso);
       * OBDal.getInstance().save(invoice); OBDal.getInstance().flush();
       * 
       * }else{ claveAcceso = invoice.getEeiCodigo(); }
       */
      // INTERACCIÓN CON LOS WS
      // RespuestaComprobante respuestaC;
      // RespuestaSolicitud respuesta = enviarRecepcionECWS(bFirmado);
      // ec.cusoft.facturaec.pruebas.recepcion.RespuestaSolicitud respuesta =
      // enviarRecepcionECWSPRUEBAS(bFirmado, param.getURLWsValidacion());
      // ec.gob.sri.comprobantes.ws.RespuestaSolicitud answer = enviarRecepcionECWSSRI(bFirmado,
      // param.getURLWsValidacion());
      //
      //
      // org.w3c.dom.Document documento = null;
      // if(answer.getEstado().equals("RECIBIDA")/*respuesta.getEstado().equals("RECIBIDA")*/){
      //
      // Thread.currentThread();
      // Thread.sleep(3000);
      //
      // ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante autorizaciones =
      // enviarAutorizacionECWSSRI(claveAcceso, param.getURLWsAutorizacion());
      //
      // if(autorizaciones != null)
      // {
      // if(autorizaciones.getAutorizaciones() == null ||
      // autorizaciones.getAutorizaciones().getAutorizacion() == null)
      // System.out.println("----------- autorizaciones.getAutorizaciones() = NULL");
      // else{
      // int size = autorizaciones.getAutorizaciones().getAutorizacion().size();
      //
      // System.out.println("--------- Hay " + size + " autorizaciones.");
      // }
      //
      //
      // //obtenemos la url del webService
      // String wsURL = param.getURLWsAutorizacion();
      // //hacemos una llamada manual al webservice\
      // //dyr.o Object aut[] = ManualCall.getAutorizacionArray(claveAcceso, wsURL);
      //
      // System.out.println("Realizando la llamada Manual al web service");
      //
      // Object[] aut = null; //dyr.n
      // aut = ManualCall.getAutorizacionArray(claveAcceso, wsURL); //dyr.n
      //
      // documento = (Document)aut[0]; // old
      // /*
      // DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance(); // CC
      // DocumentBuilder db = dbf.newDocumentBuilder();
      // InputSource is = new InputSource(new StringReader((String) aut[0]));
      // documento = db.parse(is);
      // // CC*/
      // String resA = (String)aut[1];
      //
      // //dyr.n
      // org.w3c.dom.Node numAutoAux = null;
      // numAutoAux = documento.getElementsByTagName("numeroAutorizacion").item(0);
      //
      // int acum = 0;
      // int errorsecuencia = 0;
      // if (numAutoAux == null) {
      // if (!resA.contains("ERROR SECUENCIAL REGISTRADO")) {
      // System.out.println("Numero de Autorizacion esta en Null");
      // while (numAutoAux == null && acum <= 500) {
      // aut = ManualCall.getAutorizacionArray(claveAcceso, wsURL);
      // documento = (Document) aut[0]; //old
      // /*DocumentBuilderFactory dbf2 = DocumentBuilderFactory.newInstance(); // CC
      // DocumentBuilder db2 = dbf2.newDocumentBuilder();
      // InputSource is2 = new InputSource(new StringReader((String) aut[0]));
      // documento = db2.parse(is2); // CC*/
      // resA = (String)aut[1];
      // numAutoAux = documento.getElementsByTagName("numeroAutorizacion").item(0);
      // System.out.println(" Acumulador =" + acum);
      // Thread.currentThread();
      // Thread.sleep(300L);
      // acum++;
      // if (resA.contains("ERROR SECUENCIAL REGISTRADO")){
      // acum = 501;
      // errorsecuencia = 1;
      // }
      // }
      // } else
      // throw new IOException("El secuencial de la transacción se encuentra registrado en el SRI");
      // }
      //
      // if (errorsecuencia == 1){
      // throw new IOException("El secuencial de la transacción se encuentra registrado en el SRI");
      // }
      //
      // if (numAutoAux == null) {
      // System.out.println("Error. El servicio Web del SRI no ha logrado crear el archivo xml
      // autorizado");
      // }
      // //dyr.n
      // org.w3c.dom.Node xml = documento.getElementsByTagName("autorizaciones").item(0);
      // List<Mensaje> mensajes = getListaMensajesRespuesta(documento);
      //
      // String[] autValues = saveAutValues(documento, invoice);
      //
      // file.renameTo(new File(file.getAbsolutePath().replace("nofirmado", "autorizado")));
      // FileUtils.saveNodeIntoComp(xml, file.getAbsolutePath().replace("nofirmado", "autorizado"));
      // file = new File(file.getAbsolutePath().replace("nofirmado", "autorizado"));
      //
      // System.out.println("3- Creación del archivo: " + file.getAbsolutePath());
      //
      // //adjuntar el XML
      // String ruta = file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-4);
      // String name = file.getName().substring(0, file.getName().length()-4);
      // FileUtils.attachFacturaeAtPageCFDI(rootDirectory.replace("/" + ftpFolder + "/", ""),
      // invoice.getId(), ruta, name);
      //
      // System.out.println("4- Se adjuntó archivo XML a la factura con ID: " + invoice.getId() +
      // " en la ruta " + ruta);
      //
      // if(aceptada(resA)){
      // //generar el PDF
      // String strReportPath = "@attach@/RptC_Invoice_Electronic.jrxml";
      // String documentTypeName = "Factura";
      //
      // if(invoice.getDocumentType().getEeiEdocType().equals("07")){//Documento de Retención
      // strReportPath = "@attach@/RptSswh_Withholding_Electronic.jrxml";
      //
      // documentTypeName = "Retención";
      // }
      //
      // if(invoice.getDocumentType().getEeiEdocType().equals("04")){//Documento de Nota de Credito
      // strReportPath = "@attach@/RptC_Invoice_Credit.jrxml";
      //
      // documentTypeName = "Factura";
      // }
      //
      // if (invoice.getDocumentType().getEeiEdocType().equals("06")) {// Documento de Nota de
      // // Credito
      // strReportPath = "@attach@/RptC_Invoice_Credit_Debit.jrxml";
      //
      // documentTypeName = "Factura";
      // }
      //
      // HashMap<String, Object> parameters = new HashMap<String, Object>();
      // parameters.put("DOCUMENT_ID", invoice.getId());
      // File pdfFile = FileUtils.actualizaPDFFacturae(rootDirectory.replace("/" + ftpFolder + "/",
      // ""), invoice.getId(), strReportPath, param.getDIRFiles(), name+".pdf", parameters, null);
      //
      // System.out.println("5- Se creó el archivo " + name+".pdf" + " a la factura con ID: " +
      // invoice.getId());
      //
      // saveOBLog(invoice, documentTypeName + " RECIBIDA satisfactoriamente.", "S");
      // saveOBLog(invoice, documentTypeName +" AUTORIZADA satisfactoriamente.", "S");
      //
      // for (int i = 0; i < mensajes.size(); i++) {
      // saveOBLog(invoice, mensajes.get(i).getMensaje()+" ("+mensajes.get(i).getTipo()+")", "S");
      // }
      // }else{
      // //ANULAR LA FACTURA Y LOG
      // saveOBLog(invoice, "Factura RECIBIDA satisfactoriamente.", "S");
      // saveOBLog(invoice, "Factura NO AUTORIZADA.", "E");
      // for (int i = 0; i < mensajes.size(); i++) {
      // saveOBLog(invoice, mensajes.get(i).getMensaje()+" ("+mensajes.get(i).getTipo()+")", "E");
      // }
      // }
      // }
      // else{ //ANULAR LA FACTURA
      // saveOBLog(invoice, "Factura Rechazada en el Webservice de Autorización.", "E");
      // }
      // }
      // //CONTROL DE LOG
      // else{
      // saveOBLog(invoice, "Factura DEVUELTA al enviar en el Webservice de Recepción.", "E");
      // }
      return file;

    } catch (JAXBException ex) {
      throw new ServletException(ex.getMessage());
    }

  }

  private String[] saveAutValues(Document documento, Invoice invoice)
      throws DatatypeConfigurationException {
    org.w3c.dom.Node numA = documento.getElementsByTagName("numeroAutorizacion").item(0);
    String sNumA = null;
    String sFA = null;
    if (numA != null) {
      sNumA = numA.getTextContent();

      org.w3c.dom.Node fechaA = documento.getElementsByTagName("fechaAutorizacion").item(0);
      sFA = fechaA.getTextContent();

      XMLGregorianCalendar fecha = FechaUtils.CalendarToXml(Calendar.getInstance());

      fecha.setYear(new Integer(sFA.substring(0, 4)));
      fecha.setMonth(new Integer(sFA.substring(5, 7)));
      fecha.setDay(new Integer(new Integer(sFA.substring(8, 10))));
      fecha.setHour(new Integer(sFA.substring(11, 13)));
      fecha.setMinute(new Integer(sFA.substring(14, 16)));
      fecha.setSecond(new Integer(sFA.substring(17, 19)));

      Date dateA = fecha.toGregorianCalendar().getTime();

      OBContext.setAdminMode();
      try {
        //
        invoice.setEeiFechaautotext(sFA);
        invoice.setEeiNumauto(sNumA);
        invoice.setEeiFechaauto(dateA);
        invoice.setSswhAuthorization(sNumA);
        invoice.setEeiRsiAuthNo(sNumA);

        OBDal.getInstance().save(invoice);
        OBDal.getInstance().flush();

      } catch (Exception ex) {

      } finally {
        OBContext.restorePreviousMode();
      }
    }
    String[] res = { sNumA, sFA };
    return res;
  }

  private boolean aceptada(String resA) {

    System.out.println(
        "********* Mostrando resultado de la autorización: " + resA + "***********************");

    /*
     * String answer = resA.substring(0, 13); if(answer.equalsIgnoreCase("NO AUTORIZADO")) return
     * false; else return true;
     */

    if (resA.contains("NO AUTORIZADO"))
      return false;
    else
      return true;

  }

  public List<Mensaje> getListaMensajesRespuesta(Document documento) {
    NodeList listaMensajes = documento.getElementsByTagName("identificador");
    List<Mensaje> lista = new ArrayList<Mensaje>();
    for (int item = 0; item < listaMensajes.getLength(); item++) {
      try {
        Mensaje mensaje = new Mensaje();
        mensaje.setIdentificador(listaMensajes.item(item).getTextContent());
        mensaje.setMensaje(listaMensajes.item(item).getNextSibling().getTextContent());
        mensaje
            .setTipo(listaMensajes.item(item).getNextSibling().getNextSibling().getTextContent());
        lista.add(mensaje);
      } catch (Exception e) {
      }
    }
    return lista;

  }

  public static Object[] getAutorizacion(String claveAcceso, String wsURL)
      throws MalformedURLException, IOException, TransformerConfigurationException,
      TransformerException {
    // Code to make a webservice HTTP request
    String responseString = "";
    String outputString = "";

    URL url = new URL(wsURL);
    URLConnection connection = url.openConnection();
    HttpURLConnection httpConn = (HttpURLConnection) connection;
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    String xmlInput = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ec=\"http://ec.gob.sri.ws.autorizacion\">\n"
        + "<soapenv:Header/>\n" + "<soapenv:Body>\n" + "<ec:autorizacionComprobante>\n"
        + "<!--Optional:-->\n" + "<claveAccesoComprobante>" + claveAcceso
        + "</claveAccesoComprobante>\n" + "</ec:autorizacionComprobante>\n" + "</soapenv:Body>\n"
        + "</soapenv:Envelope>";
    byte[] buffer = new byte[xmlInput.length()];
    buffer = xmlInput.getBytes();
    bout.write(buffer);
    byte[] b = bout.toByteArray();
    String SOAPAction = "";
    // Set the appropriate HTTP parameters.
    httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
    httpConn.setRequestProperty("Content-Type", "text/xml");
    httpConn.setRequestProperty("SOAPAction", SOAPAction);
    httpConn.setRequestMethod("POST");
    httpConn.setDoOutput(true);
    httpConn.setDoInput(true);
    OutputStream out = httpConn.getOutputStream();
    // Write the content of the request to the outputstream of the HTTP Connection.
    out.write(b);
    out.close();
    // Ready with sending the request.
    // Read the response.
    InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
    BufferedReader in = new BufferedReader(isr);
    // Write the SOAP message response to a String.
    while ((responseString = in.readLine()) != null) {
      outputString = outputString + responseString;
    }
    // Parse the String output to a org.w3c.dom.Document and be able to reach every node with the
    // org.w3c.dom API.
    Document document = FileUtils.parseXmlFile(outputString);
    NodeList nodeLst = document.getElementsByTagName("autorizaciones");
    String weatherResult = nodeLst.item(0).getTextContent();
    System.out.println("Weather: " + weatherResult);
    // Write the SOAP message formatted to the console.
    String formattedSOAPResponse = CryptUtils.formatXML(outputString);
    System.out.println(formattedSOAPResponse);
    Object aut[] = new Object[2];
    aut[0] = document;
    aut[1] = weatherResult;
    return aut;
  }

  @SuppressWarnings("unused")
  private RespuestaSolicitud enviarRecepcionECWS(byte[] sComp)
      throws JAXBException, Exception, ParserConfigurationException, SAXException, IOException,
      ServletException, ClassNotFoundException {
    try {
      RecepcionComprobantesService service = new RecepcionComprobantesService();
      RecepcionComprobantes pt = (RecepcionComprobantes) service.getRecepcionComprobantesPort();
      RespuestaSolicitud respuesta = pt.validarComprobante(sComp);
      return respuesta;
    } catch (Exception ex) {
      System.out.println((ex.getMessage()));
      throw new ServletException((ex.getMessage()));
    }
  }

  @SuppressWarnings("unused")
  private ec.gob.sri.comprobantes.ws.RespuestaSolicitud enviarRecepcionECWSSRI(byte[] sComp)
      throws JAXBException, Exception, ParserConfigurationException, SAXException, IOException,
      ServletException, ClassNotFoundException {
    try {
      ec.gob.sri.comprobantes.ws.RecepcionComprobantesService service = new ec.gob.sri.comprobantes.ws.RecepcionComprobantesService();

      ec.gob.sri.comprobantes.ws.RecepcionComprobantes pt = service.getRecepcionComprobantesPort();
      ec.gob.sri.comprobantes.ws.RespuestaSolicitud answer = pt.validarComprobante(sComp);

      System.out.println("Answer: " + answer.getEstado());

      return answer;
    } catch (Exception ex) {
      System.out.println((ex.getMessage()));
      throw new ServletException((ex.getMessage()));
    }
  }

  @SuppressWarnings("unused")
  private ec.gob.sri.comprobantes.ws.RespuestaSolicitud enviarRecepcionECWSSRI(byte[] sComp,
      String URL) throws JAXBException, Exception, ParserConfigurationException, SAXException,
      IOException, ServletException, ClassNotFoundException {
    try {
      ec.gob.sri.comprobantes.ws.RecepcionComprobantesService service = new ec.gob.sri.comprobantes.ws.RecepcionComprobantesService(
          new URL(URL));

      ec.gob.sri.comprobantes.ws.RecepcionComprobantes pt = service.getRecepcionComprobantesPort();
      ec.gob.sri.comprobantes.ws.RespuestaSolicitud answer = pt.validarComprobante(sComp);

      System.out.println("Answer: " + answer.getEstado());

      return answer;
    } catch (Exception ex) {
      System.out.println((ex.getMessage()));
      throw new ServletException((ex.getMessage()));
    }
  }

  // PRUEBAS
  private ec.cusoft.facturaec.pruebas.recepcion.RespuestaSolicitud enviarRecepcionECWSPRUEBAS(
      byte[] comprobante, String url) throws JAXBException, Exception, ParserConfigurationException,
      SAXException, IOException, ServletException, ClassNotFoundException {
    try {
      System.out.println("Entramos a enviarRecepcionECWSPRUEBAS");
      ec.cusoft.facturaec.pruebas.recepcion.RecepcionComprobantesService service = new ec.cusoft.facturaec.pruebas.recepcion.RecepcionComprobantesService(
          new URL(url));

      System.out.println("Pasamos la linea 418");
      ec.cusoft.facturaec.pruebas.recepcion.RecepcionComprobantes port = service
          .getRecepcionComprobantesPort();

      System.out.println("Pasamos la linea 421");
      ec.cusoft.facturaec.pruebas.recepcion.RespuestaSolicitud result = port
          .validarComprobante(comprobante);

      System.out.println("1- Prueba Recepción Comprobante: Result = " + result.getEstado());

      /*
       * List<Comprobante> comps = null;
       * 
       * if(result.getComprobantes() != null) comps = result.getComprobantes().getComprobante();
       * 
       * if(comps != null){ System.out.println("Mostrando mensajes:");
       * 
       * for (Comprobante comprobante2 : comps) {
       * 
       * List<ec.cusoft.facturaec.pruebas.recepcion.Mensaje> mens = null;
       * 
       * if(comprobante2.getMensajes() != null) mens = comprobante2.getMensajes().getMensaje();
       * 
       * } }
       */

      return result;
    } catch (Exception ex) {
      System.out.println((ex.getMessage()));
      throw new ServletException((ex.getMessage()));
    }
  }

  @SuppressWarnings("unused")
  private RespuestaComprobante enviarAutorizacionECWS(String claveAcceso)
      throws JAXBException, Exception, ParserConfigurationException, SAXException, IOException,
      ServletException, ClassNotFoundException {
    try {
      AutorizacionComprobantesService service = new AutorizacionComprobantesService();
      AutorizacionComprobantes pt = (AutorizacionComprobantes) service
          .getAutorizacionComprobantesPort();
      RespuestaComprobante respuesta = pt.autorizacionComprobante(claveAcceso);// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
      return respuesta;
    } catch (Exception ex) {
      System.out.println((ex.getMessage()));
      throw new ServletException((ex.getMessage()));
    }
  }

  private ec.cusoft.facturaec.pruebas.autorizacion.RespuestaComprobante enviarAutorizacionECWSPruebas(
      String claveAcceso, String url) throws JAXBException, Exception, ParserConfigurationException,
      SAXException, IOException, ServletException, ClassNotFoundException {
    ec.cusoft.facturaec.pruebas.autorizacion.RespuestaComprobante result = null;
    try {
      try {
        ec.cusoft.facturaec.pruebas.autorizacion.AutorizacionComprobantesService service = new ec.cusoft.facturaec.pruebas.autorizacion.AutorizacionComprobantesService(
            new URL(url));
        ec.cusoft.facturaec.pruebas.autorizacion.AutorizacionComprobantes port = service
            .getAutorizacionComprobantesPort();
        result = port.autorizacionComprobante(claveAcceso);
        System.out.println(
            "2- Prueba Autorización Comprobante: Result = " + result.getClaveAccesoConsultada());
      } catch (Exception ex) {
        // TODO handle custom exceptions here
      }
      return result;
    } catch (Exception ex) {
      System.out.println((ex.getMessage()));
      throw new ServletException((ex.getMessage()));
    }
  }

  private ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante enviarAutorizacionECWSSRI(
      String claveAcceso) throws JAXBException, Exception, ParserConfigurationException,
      SAXException, IOException, ServletException, ClassNotFoundException {
    ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante result = null;

    try {
      try {
        ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesService service = new ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesService();
        ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantes port = service
            .getAutorizacionComprobantesPort();

        result = port.autorizacionComprobante(claveAcceso);

        System.out
            .println("Respuesta Servicio Autorizacion = " + result.getClaveAccesoConsultada());

      } catch (Exception ex) {
        // TODO handle custom exceptions here
      }

      return result;
    } catch (Exception ex) {
      System.out.println((ex.getMessage()));
      throw new ServletException((ex.getMessage()));
    }
  }

  private ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante enviarAutorizacionECWSSRI(
      String claveAcceso, String URL) throws JAXBException, Exception, ParserConfigurationException,
      SAXException, IOException, ServletException, ClassNotFoundException {
    ec.gob.sri.comprobantes.ws.aut.RespuestaComprobante result = null;

    try {
      try {
        ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesService service = new ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantesService(
            new URL(URL));

        int i = 0;
        do {
          if (i >= 5)
            break;
          ec.gob.sri.comprobantes.ws.aut.AutorizacionComprobantes port = service
              .getAutorizacionComprobantesPort();
          result = port.autorizacionComprobante(claveAcceso);
          if (!result.getAutorizaciones().getAutorizacion().isEmpty())
            break;
          Thread.currentThread();
          Thread.sleep(300L);
          i++;
        } while (true);

        System.out
            .println("Respuesta Servicio Autorizacion = " + result.getClaveAccesoConsultada());

      } catch (Exception ex) {
        // TODO handle custom exceptions here
      }

      return result;
    } catch (Exception ex) {
      System.out.println((ex.getMessage()));
      throw new ServletException((ex.getMessage()));
    }
  }

  static final boolean $assertionsDisabled = false;

  // SEND BY MAIL
  public String send(ConnectionProvider con, File file, Invoice invoice, String strLanguage)
      throws ServletException {
    String send = "NO ENVIADO";
    // sender
    EEIMailServer mailServer = (invoice.getUserContact() != null)
        ? invoice.getUserContact().getEeiMailserver()
        : ((invoice.getOrganization().getOrganizationInformationList().size() > 0 && invoice
            .getOrganization().getOrganizationInformationList().get(0).getUserContact() != null)
                ? invoice.getOrganization().getOrganizationInformationList().get(0).getUserContact()
                    .getEeiMailserver()
                : null);

    String mailUser = (invoice.getUserContact() != null) ? invoice.getUserContact().getEmail()
        : ((invoice.getOrganization().getOrganizationInformationList().size() > 0 && invoice
            .getOrganization().getOrganizationInformationList().get(0).getUserContact() != null)
                ? invoice.getOrganization().getOrganizationInformationList().get(0).getUserContact()
                    .getEmail()
                : null);
    String username = (invoice.getUserContact() != null)
        ? invoice.getUserContact().getEeiEmailuser()
        : ((invoice.getOrganization().getOrganizationInformationList().size() > 0 && invoice
            .getOrganization().getOrganizationInformationList().get(0).getUserContact() != null)
                ? invoice.getOrganization().getOrganizationInformationList().get(0).getUserContact()
                    .getEeiEmailuser()
                : null);
    String password = (invoice.getUserContact() != null)
        ? invoice.getUserContact().getEmailServerPassword()
        : ((invoice.getOrganization().getOrganizationInformationList().size() > 0 && invoice
            .getOrganization().getOrganizationInformationList().get(0).getUserContact() != null)
                ? invoice.getOrganization().getOrganizationInformationList().get(0).getUserContact()
                    .getEmailServerPassword()
                : null);

    String destinatary = (invoice.getSalesRepresentative() != null)
        ? invoice.getSalesRepresentative().getEmail()
        : invoice.getBusinessPartner().getEEIEmail();

    if (invoice.getDocumentType().getEeiEdocType().equals("07"))// retención
      destinatary = invoice.getBusinessPartner().getEEIEmail();

    if (mailServer != null && username != null && password != null && destinatary != null) {
      String message = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" /><title>Notificación de Factura Electrónica</title></head><body><p>Estimado Cliente,</p><p>Le estamos enviando por este medio la factura electr&oacute;nica que nos solicit&oacute; y al mismo tiempo nos reiteramos a sus &oacute;rdenes para cualquier aclaraci&oacute;n al respecto.</p><p>Agradeciendo su preferencia esperamos haberle servido como usted merece.</p><p>Atentamente, "
          + invoice.getOrganization().getName() + "</p><p>&nbsp;</p></body></html>";
      EMailUtils email = new EMailUtils(null, mailServer.getServername(), mailUser, destinatary,
          "Envío de Factura Electrónica", message, mailServer.getPort().toString(),
          mailServer.isRequiretls(), mailServer.isRequiressl());
      email.addAttachment(file);
      File filePDF = new File(file.getAbsolutePath().replace(".xml", ".pdf"));
      email.addAttachment(file);
      email.addAttachment(filePDF);
      email.setMessageHTML(message);
      email.addCc(destinatary);
      send = email.send2(username, FormatUtilities.encryptDecrypt(password, false));
    }

    return send.equals("OK") ? "Success" : send;
  }

  public EEIParamFacturae getGenericParamFEC(Organization org) throws ServletException {
    // Parametros Facturae
    OBCriteria<EEIParamFacturae> paramsC = OBDal.getInstance()
        .createCriteria(EEIParamFacturae.class);
    paramsC.add(Restrictions.eq("organization", org));
    paramsC.add(Restrictions.eq("active", true));

    List<EEIParamFacturae> paramsL = paramsC.list();
    if (paramsL.size() == 0)
      throw new ServletException(
          "No existen parametros de Facturación Electrónica para la Organización " + org.getName());
    return paramsL.get(0);
  }

  public File go(Document docomprobante, String rootDirectory, String claveAcceso, Organization org,
      String documentNo, String docName, String ftpFolder)
      throws Exception, JAXBException, ParserConfigurationException, SAXException, IOException,
      TransformerConfigurationException, TransformerException, ServletException,
      ClassNotFoundException, XMLStreamException, FactoryConfigurationError {
    try {
      EEIParamFacturae param = getGenericParamFEC(org);

      System.out.println("0- Comenzando Proceso de Comunicación con Web Services");

      File file = FileUtils.salvarNoFirmado(docomprobante, documentNo, rootDirectory, param,
          docName);
      firmarEC(file, param);

      byte[] bFirmado = FileUtils.readFile(file.getAbsolutePath().replace("nofirmado", "firmado"));
      byte[] bFirmado2 = archivoToByte(
          new java.io.File(file.getAbsolutePath().replace("nofirmado", "firmado")));

      return file;

      // PERSISTENCIA
    } catch (JAXBException ex) {
      throw new ServletException(ex.getMessage());
    }

  }
}