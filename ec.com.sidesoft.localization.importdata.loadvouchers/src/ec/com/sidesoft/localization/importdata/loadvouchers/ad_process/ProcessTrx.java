package ec.com.sidesoft.localization.importdata.loadvouchers.ad_process;



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
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.io.InputStreamReader;

import java.sql.BatchUpdateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;

import org.openbravo.base.secureApp.VariablesSecureApp;

import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.application.attachment.AttachImplementationManager;
import org.openbravo.client.application.attachment.AttachmentAH;
import org.openbravo.client.application.attachment.AttachmentUtils;
import org.openbravo.client.application.attachment.CoreAttachImplementation;
import org.openbravo.base.session.OBPropertiesProvider;

import org.openbravo.xmlEngine.XmlDocument;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.ad.datamodel.Table;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import au.com.bytecode.opencsv.CSVReader;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvPartner;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvPurchaseIinvoice;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvVoucherData;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvVoucherDataLine;


import java.io.FileReader;
//import ec.com.sidesoft.workorder.update.price.UpdatePriceWO;
//import ec.com.sidesoft.workorder.update.price.UpdatePriceLineWO;
import org.openbravo.model.pricing.pricelist.*;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.apache.log4j.Logger;

import org.openbravo.model.pricing.pricelist.*;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.apache.log4j.Logger;

public class ProcessTrx extends DalBaseProcess {
	OBError message;
	private boolean insert = false;
	private int processed;
	private int notprocessed;
	private int rejected;
	private boolean cancelled;
	protected ConnectionProvider conn;
	protected VariablesSecureApp vars;
	protected String language;
	
	public static String strRowOrg="";
	public static int strTakereplace1 = 12;
	//public UpdatePriceLineWO line = null;
	public int countErrors=0;
	// private static Logger log4j1 = Logger.getLogger(UploadCSV.class);

	final String attachmentFolderPath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
			.getProperty("attach.path");

	@Override
	protected void doExecute(ProcessBundle bundle) throws Exception, IOException {

		VariablesSecureApp vars = bundle.getContext().toVars();
		ConnectionProvider conn = new DalConnectionProvider(false);
		String language = OBContext.getOBContext().getLanguage().getLanguage();
		this.conn = conn;
		this.vars = vars;
		this.language = language;

		try {
			message = new OBError();
			processRequest(bundle);
		} catch (Exception e) {
			// log4j1.info("exeption" + e);
			message.setMessage(e.getMessage());
			message.setTitle(Utility.messageBD(conn, "Error", language));
			message.setType("Error");
		} finally {
			bundle.setResult(message);
		}
	}

	public void processRequest(ProcessBundle bundle) throws Exception {

		List<String> data;
		String[]  dataLines;
		String messageT = "";
		String typeM = "";
		String titleM = "";
		String recordId = (String) bundle.getParams().get("Imdlv_Voucher_Purchase_ID");
		String pathFile = getLocationFileCSV(recordId);

		//if (pathFile == "") {
		/*	typeM = "Error";
			titleM = "Error";
			messageT = "No se encontro ningún archivo de tipo <b>csv</b> en los adjuntos";
			message.setMessage(messageT);
			message.setTitle(Utility.messageBD(conn, titleM, language));
			message.setType(typeM);
		} else {*/
			//String strClave="0102202201099001751400121430020007230550000000011";
			String strURL="https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
			String strmessage = SearchXmlSri(strURL);  //Cargar Datos SRI

			//Creación de terceros
			ImdlvVoucherData LinesImporData = OBDal.getInstance().get(ImdlvVoucherData.class, recordId);

			Object[] objMsgLines = ReadPurchaseLines(LinesImporData,false);
			typeM= objMsgLines[0].toString();
			if (typeM.equals("Error") || typeM.equals("Info")) {
				typeM= objMsgLines[0].toString();
				messageT= objMsgLines[1].toString();
			}else {
				objMsgLines = InsertPurchaseLines(LinesImporData);
			}
			typeM= objMsgLines[0].toString();
			messageT= objMsgLines[1].toString();
			if(countErrors++>0) {
				typeM= "Info";
				messageT= "El proceso termino con alguno errores, revise los logs";
			}
			if (typeM.equals("Success") || typeM.equals("OK")) {
				titleM = "ProcessOK";
				typeM = "Success";
			//	messageT = "La lineas se cargaron correctamente sin observaciones.";
				message.setMessage(messageT);
				message.setTitle(Utility.messageBD(conn, titleM, language));
				message.setType(typeM);
			}else if(typeM.equals("Info")) {
				titleM = "Information";
				message.setMessage(messageT);
				message.setTitle(Utility.messageBD(conn, titleM, language));
				message.setType(typeM);
			}else if(typeM.equals("Error")) {
				titleM = "Error";
				message.setMessage(messageT);
				message.setTitle(Utility.messageBD(conn, titleM, language));
				message.setType(typeM);
			}
		//}
	}
	
	public String SearchXmlSri(String wsURL) {
		
		Object[] objMessage=null;
		
		String claveAcceso="";
		
		
		
		try{
			OBCriteria<ImdlvVoucherDataLine> obcLines = OBDal.getInstance().createCriteria(
					ImdlvVoucherDataLine.class);
			obcLines.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ACTIVE,
					true));
			obcLines.add(Restrictions.isNull(ImdlvVoucherDataLine.PROPERTY_XMLSRI));
						
				
			int intList= obcLines.list().size();
			if (intList>0) {
				for (ImdlvVoucherDataLine listaVoucher: obcLines.list()) {
						
						String strClaveAcceso= listaVoucher.getKeyacess();
						try {
							//Thread.sleep(2000);
							objMessage=getAutorizacion(strClaveAcceso,wsURL);
						}catch(Exception ee) {
							
						}
						
						if (objMessage.length>0) {
							String strXML=objMessage[1].toString();
							
							if (strXML.length()>0) {
								ImdlvVoucherDataLine LinesImporData = OBDal.getInstance().get(ImdlvVoucherDataLine.class, listaVoucher.getId());
								LinesImporData.setXMLSri(strXML);
								String strheaderxml="";
								if (strXML.contains("<factura")) {
									strheaderxml="factura";
								}
								if (strXML.contains("<notaDebito")) {
									strheaderxml="notaDebito";
								}
								if (strXML.contains("<notaCredito")) {
									strheaderxml="notaCredito";
								}
								if (strXML.contains("<comprobanteRetencion")) {
									strheaderxml="comprobanteRetencion";
								}
								
								LinesImporData.setDescription(strheaderxml);
								OBDal.getInstance().save(LinesImporData);
							}
						}
				}
				OBDal.getInstance().commitAndClose();
			}
			
		}catch(Exception e) {
			
		}
		
		return "OK";
}
	
	public Object[] getAutorizacion(String claveAcceso, String wsURL)
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
		    Document document = parseXmlFile(outputString);
		    NodeList nodeLst = document.getElementsByTagName("autorizaciones");
		    String weatherResult = nodeLst.item(0).getTextContent();
		    //System.out.println("Weather: " + weatherResult);
		    // Write the SOAP message formatted to the console.
		    String formattedSOAPResponse = formatXML(outputString);
		    //System.out.println(formattedSOAPResponse);
		    
		    org.w3c.dom.Node xml = document.getElementsByTagName("comprobante").item(0);
		    weatherResult = xml.getTextContent();
		    formattedSOAPResponse = formatXML(weatherResult);
		    //System.out.println(formattedSOAPResponse);
		    
		    Document documentFinal = parseXmlFile(formattedSOAPResponse);
		    //org.w3c.dom.Node xmlFinal =  documentFinal.getElementsByTagName("totalConImpuestos").item(0);
		    //weatherResult = xmlFinal.getTextContent();
		    //formattedSOAPResponse = formatXML(weatherResult);
		    //System.out.println(formattedSOAPResponse);
		    /*documentFinal.getDocumentElement().normalize();
            //System.out.println("Elemento raiz:" + documentFinal.getDocumentElement().getNodeName());
            NodeList listaNodos = documentFinal.getElementsByTagName("infoFactura");
            for (int temp = 0; temp < listaNodos.getLength(); temp++) {
                Node nodo = listaNodos.item(temp);
                System.out.println("Elemento:" + nodo.getNodeName());
                if (nodo.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) nodo;
                    System.out.println("fechaEmision: " + element.getAttribute("fechaEmision"));
                    System.out.println("dirEstablecimiento: " + element.getElementsByTagName("dirEstablecimiento").item(0).getTextContent());
                    System.out.println("contribuyenteEspecial: " + element.getElementsByTagName("obligadoContabilidad").item(0).getTextContent());
                    System.out.println("tipoIdentificacionComprador: " + element.getElementsByTagName("tipoIdentificacionComprador").item(0).getTextContent());
                }
            }*/
		    
		    
		    Object aut[] = new Object[2];
		    //aut[0] = document;
		    //aut[1] = weatherResult;
		    aut[0] = "OK";
		    aut[1] = formattedSOAPResponse;
		    return aut;
		  }
	
	public static String formatXML(String unformattedXml) {    
		try {        
			Document document = parseXmlFile(unformattedXml);        
			OutputFormat format = new OutputFormat(document);        
			format.setIndenting(true);        
			format.setIndent(3);        
			format.setOmitXMLDeclaration(true);        
			Writer out = new StringWriter();        
			XMLSerializer serializer = new XMLSerializer(out, format);        
			serializer.serialize(document);        
			return out.toString();    
		} catch (IOException e) {        
			throw new RuntimeException(e);    
		}
	} 
	
	public static Document parseXmlFile(String in) {    
        try {        
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();        
            DocumentBuilder db = dbf.newDocumentBuilder();        
            org.xml.sax.InputSource is = new org.xml.sax.InputSource(new StringReader(in));        
            return db.parse(is);    
        } 
        catch (ParserConfigurationException e) {        
            throw new RuntimeException(e);    
        } catch (SAXException e) {        
            throw new RuntimeException(e);    } 
        catch (IOException e) {        
            throw new RuntimeException(e);    }
    }
	  private static String nodeToString(Node node) {
		    StringWriter sw = new StringWriter();
		    try {
		      Transformer t = TransformerFactory.newInstance().newTransformer();
		      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		      t.setOutputProperty(OutputKeys.INDENT, "yes");
		      t.transform(new DOMSource(node), new StreamResult(sw));
		    } catch (TransformerException te) {
		      System.out.println("nodeToString Transformer Exception");
		    }
		    return sw.toString();
		  }

	public String getLocationFileCSV(String recordId) throws OBException {
		OBContext.setAdminMode(true);
		OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
		final Table table = OBDal.getInstance().get(Table.class, "094B879B858745BA878EFC56ECFEC067");
		obc.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordId));
		obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
		obc.add(Restrictions.eq(Attachment.PROPERTY_DATATYPE, "text/plain"));
		obc.addOrderBy(Attachment.PROPERTY_SEQUENCENUMBER, false);
		obc.setFilterOnReadableOrganization(false);
		obc.setMaxResults(1);
		Attachment attach = (Attachment) obc.uniqueResult();
		if (attach == null) {
			return "";
		}
		return attachmentFolderPath + "/" + attach.getPath() + '/' + attach.getName();
	}

	public List<String> loadFileLines(String filename) throws Exception, IOException {

		CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"), ',', '\"', '\\',
				0, false, true);

		List<String> emps = new ArrayList<String>();
		// read line by line
		String[] record = null;
		// skip header row
		reader.readNext();

		while ((record = reader.readNext()) != null) {
			if (record.length > 1 || record[0].length() > 0) {
				emps.add((String) record[0]);
			}
		}
		reader.close();
		return emps;
	}

	public String[] loadFileLinesTmp(String filename) throws Exception, IOException {

		CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"), ']', '\"', '\\',
				0, false, true);

		int count=0;
		// read line by line
		String[] record = null;
		// skip header row
		reader.readNext();

		while ((record = reader.readNext()) != null) {
			if (record.length > 1 || record[0].length() > 0) {
				
				String strRecord = (String) record[0];
				String[] strParseString = strRecord.split("\t");
				
				String objtmp=strParseString.toString();
				count++;
			}
		}
		reader.close();
		CSVReader reader2 = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"), ']', '\"', '\\',
				0, false, true);
		String[] emps = new String[count+3];
		int intidx=0;
		while ((record = reader2.readNext()) != null) {
			if (record.length > 1 || record[0].length() > 0) {
				
				String strRecord = (String) record[0];
				String strReplace = strRecord.replace(",", "*coma*").replace(";", "*puntocoma*");
				String[] strParseString = strReplace.split("\t");
				List<String> strRow = new ArrayList<String>(Arrays.asList(strParseString));  
				emps[intidx]=strRow.toString();
				intidx++;
			}
		}
		reader2.close();
		
		return emps;
	}

	public static Object[] getObjetRows(String[] strRows) {
		

		//strRows = new String[3];
		boolean flagconitnue = true;
		int countIterator = 0;
		
		//strRows[0]="20210909            ECUEFT  ALTAMIRANO CHERREZ ANGELA INES                              Francisco de Orellana y Quito Barrio Par593994081395   1802044857001       0000137002202121/09/10  USD0000000000.00ALTAMIRANO-163096405                                         Ajuste de Renta                         1SH12586  inesangela2066@hotmail.com                                                                    ALTAMIRANO-1630964056737           0000000060.0021/09/07000000000000.00                                               21/09/06";
		//strRows[1]="20210909            ECUEFT  ALTAMIRANO CHERREZ ANGELA INES                              Francisco de Orellana y Quito Barrio Par593994081395   1802044857001       0000137002202121/09/10  USD0000000000.00ALTAMIRANO-1630964056511990   2135                           ALTAMIRANO CHERREZ ANGELA INES          1SH12586  inesangela2066@hotmail.com                                                                    ALTAMIRANO-1630964056737           0000000060.0021/09/07000000000000.00                                               21/09/06";
		//strRows[2]="20210909            ECUEFT  CARVAJAL AREVALO MARIANA ERMILA                             Arturo Hidalgo 282 Y Dr Raul Mantalvo   5930990892621  1001794658001       0000137003202121/09/10  USD0000000000.00CARVAJAL A-163096368                                         Reembolso de Agua/Luz                   1SH12585  Marianacarvajal0029@hotmail.com                                                               CARVAJAL A-1630963687509           0000000024.7821/09/07000000000000.00                                               21/09/06";
		Object[] objRows = new Object[strRows.length];		
		for (int i=1; i<strRows.length;i++) {
			

			strRowOrg =String.valueOf(strRows[i]);
			//System.out.println("Linea:" + i + " - Contar Linea:" + strRowOrg.trim().length() +"\n");
			//System.out.println(String.valueOf(strRows[i]));
			if (strRowOrg.trim().length()>9){
			List<String> listRow = new ArrayList<String>();

			String strRowAux = String.valueOf(strRows[i]);
			countIterator=0;
			flagconitnue=true;
			while(flagconitnue) {
				String strNewValue ="";
				countIterator++;
				
				
				if (strRowOrg.trim().equals("")) {
					flagconitnue=false;
				}else {
					
					if (countIterator==1) {
						strNewValue= strRowOrg.substring(0,8);
						strRowOrg = strRowOrg.replace(strNewValue,"");
						//System.out.println(strNewValue);
					}
					if (countIterator==2) {
						strRowOrg = strRowOrg.substring(strTakereplace1, (strRowOrg.length()- strTakereplace1));
						//strRowOrg = strRowOrg.replace(strNewValue,";");
						strNewValue= strRowOrg.substring(0,8);
						strRowOrg = strRowOrg.replace(strNewValue,"");
						strRowOrg = strRowOrg.trim();
						
						//strNewValue= strRowOrg.substring(1,3);
						String strfield4 = strNewValue.substring(0,3);
						String strfield5 = strNewValue.replace(strfield4,"");
						listRow.add(strfield4.trim().length()>0?strfield4:"null");
						listRow.add(strfield5.trim().length()>0?strfield5:"null");
						strNewValue="";
						//System.out.println(strNewValue);
						

					}
					if (countIterator==3) {
						
						int countSpace = getFieldValue(strRowOrg);
						strNewValue= strRowOrg.trim().substring(0,countSpace );
						//System.out.println("reemplazar: "+ strNewValue);
						strRowOrg = strRowOrg.replace(strNewValue,"");
						
						//System.out.println(countSpace);
					}
					if (countIterator==4) {
						
						//strRowOrg = strRowOrg.substring(55, (strRowOrg.length()- strTakereplace1));
						//strRowOrg = strRowOrg.replace(strNewValue,";");
						strNewValue= strRowOrg.substring(0,55);
						strRowOrg = strRowOrg.replace(strNewValue,"");
						strRowOrg = strRowOrg.trim();

						//System.out.println(strNewValue + ":");
						//System.out.println(strRowOrg + ":");	
						//int intnewCount4 = getFieldValueReverse(strNewValue.trim());
						//if (intnewCount4>0) {
							//String strfield4 = strNewValue.substring(0,strNewValue.trim().length()+1-intnewCount4 );
							//String strfield5 = strNewValue.replace(strfield4, "");
							String strfield4 = strNewValue.substring(0,40);
							String strfield5 = strNewValue.replace(strfield4,"");
							listRow.add(strfield4.trim().length()>0?strfield4:"null");
							listRow.add(strfield5.trim().length()>0?strfield5:"null");
							strNewValue="";		
							//System.out.println(strfield4);
							//System.out.println(strfield5);

						//}
						
					}
					
					if (countIterator==5) {
						
						strNewValue= strRowOrg.substring(1,20);
						strRowOrg = strRowOrg.replace(strNewValue,"");
						strRowOrg = strRowOrg.trim();
							
						///System.out.println(strNewValue);
					}
					
					if (countIterator==6) {
						
						strNewValue= strRowOrg.substring(0,24);
					
						//System.out.println("strNewValue: " + strNewValue);
						strRowOrg = strRowOrg.replace(strNewValue,"");
						strRowOrg = strRowOrg.trim();
						
						strNewValue= strNewValue.trim();
						int countField6= strNewValue.length();
						
						int countAuxposf = countField6-12;
						
						String strfield4 = strNewValue.substring(0,countAuxposf);
						strNewValue = strNewValue.replace(strfield4, "");
						//System.out.println("strNewValue 2: " + strNewValue);
						countAuxposf = strNewValue.length();
						String strfield6 = strNewValue.substring(4, countAuxposf);

						String strfield5 = strNewValue.replace(strfield6, "");
						listRow.add(strfield4.trim().length()>0?strfield4:"null");
						listRow.add(strfield6.trim().length()>0?strfield5:"null");
						listRow.add(strfield5.trim().length()>0?strfield6:"null");
						
						
						
						strNewValue="";	
						//flagconitnue=false;
						//System.out.println("strfield4: "+strfield4);
						//System.out.println("strfield5: "+strfield5);
						//System.out.println("strfield6: "+strfield6);
						//break;
						
					}
					
					
					if (countIterator==7) {
						
						strNewValue= strRowOrg.substring(0,16);
						//System.out.println(strNewValue);

						strRowOrg = strRowOrg.replace(strNewValue,"");
						//strRowOrg = strRowOrg.trim();
						String strfield4 = strNewValue.substring(0,3 );
						String strfield5 = strNewValue.replace(strfield4, "");
						listRow.add(strfield4.trim().length()>0?strfield4:"null");
						listRow.add(strfield5.trim().length()>0?strfield5:"null");
						strNewValue="";	

					}
					
					if (countIterator==8) {
						
						strNewValue= strRowOrg.substring(0,61);
						System.out.println(strNewValue);

						strRowOrg = strRowOrg.replace(strNewValue,"");
						//strRowOrg = strRowOrg.trim();
						int count8=0;

						String strfield4 = strNewValue.substring(0,20 );
						strNewValue= strNewValue.replace(strfield4, "");
						if (strNewValue.trim().length()>=0 && strNewValue.trim().length()<=10) {
							count8=strNewValue.trim().length();
						}else {
							count8=10;
						}
						System.out.println(strfield4);
						
						String strfield5 = strNewValue.substring(0,count8 );
						strNewValue= strNewValue.replace(strfield5, "");
						//strNewValue.replace(strfield5, "");
						
						System.out.println(strfield5);
						
						if (strNewValue.trim().length()>0 && strNewValue.trim().length()<=10) {
							count8=strNewValue.trim().length();
						}else {
							
							count8=10;
						}
						
						String strfield6 = strNewValue.substring(0,count8 );
						strNewValue= strNewValue.replace(strfield6, "");
						
						System.out.println(strfield6);
						
						
						if (strNewValue.trim().length()>0 && strNewValue.trim().length()<=21) {
							count8=strNewValue.trim().length();
						}else {
							count8=0;
						}
						String strfield7 = strNewValue.substring(0,count8 );
						
						System.out.println(strfield7);
						
						listRow.add(strfield4.trim().length()>0?strfield4:"null");
						listRow.add(strfield5.trim().length()>0?strfield5:"null");
						listRow.add(strfield6.trim().length()>0?strfield6:"null");
						listRow.add(strfield7.trim().length()>0?strfield7:"null");

						strNewValue="";	

						System.out.println(strRowOrg);
					}
					
					if (countIterator==9) {
						//System.out.println("strRowOrg: " +strRowOrg);
						strNewValue= strRowOrg.substring(0,40);
						strRowOrg = strRowOrg.replace(strNewValue,"");
						strRowOrg = strRowOrg.trim();
						//flagconitnue=false;						
					
					}
					if (countIterator==10) {
						// 1SH12586
						
						strNewValue= strRowOrg.substring(0,10);
						strRowOrg = strRowOrg.replace(strNewValue,"");
						strRowOrg = strRowOrg;
						
						String strfield4 = strNewValue.substring(0,3 );
						String strfield5 = strNewValue.replace(strfield4, "");
						listRow.add(strfield4.trim().length()>0?strfield4:"null");
						listRow.add(strfield5.trim().length()>0?strfield5:"null");
						strNewValue="";
						//flagconitnue=false;						
						//System.out.println(strRowOrg);
					}
					
					if (countIterator==11) {
						
						strNewValue= strRowOrg.substring(0,94);
						
						strRowOrg = strRowOrg.replace(strNewValue,"");
						String strfield4 = strNewValue.substring(0,70 );
						String strfield5 = strNewValue.replace(strfield4, "");
						listRow.add(strfield4.trim().length()>0?strfield4:"null");
						listRow.add(strfield5.trim().length()>0?strfield5:"null");
						strNewValue="";
						
						strRowOrg = strRowOrg.trim();

					}
					if (countIterator==12) {
						
						strNewValue= strRowOrg.substring(0,35);
						strRowOrg = strRowOrg.replace(strNewValue,"");
						strRowOrg = strRowOrg.trim();

						
					}

					if (countIterator==13) {
						//System.out.println(strRowOrg);
						int countfinalRow =0;
						if (strRowOrg.length()<43) {
							countfinalRow=strRowOrg.length();
						}else {
							countfinalRow=43;
						}
						strNewValue= strRowOrg.substring(0,countfinalRow);
						int countfinal = strNewValue.length();

						strRowOrg = strRowOrg.replace(strNewValue,"");
						strRowOrg = strRowOrg.trim();
						String strfield4 = strNewValue.substring(0,13 );
						//System.out.println("strNewValue 1: "+strNewValue);

						strNewValue =strNewValue.substring(strfield4.length(),countfinalRow);

						countfinal = strNewValue.length();

						String strfield5 = strNewValue.substring(0,8 );
						
						if (strRowOrg.length()<=22) {
							countfinalRow=22;
						}else {
							countfinalRow=30;
						}
						strNewValue =strNewValue.substring(strfield5.length(),countfinalRow);
						countfinal = strNewValue.length();
						//System.out.println("strNewValue 2: "+strNewValue);
						//System.out.println("countfinal 2: "+countfinal);

						String strfield6 = strNewValue.substring(0,countfinal );
						
						//strNewValue =strNewValue.substring(strfield6.length(),22);
						strNewValue =strNewValue.replace(strfield6,"");
						
						countfinal = strNewValue.length();
						String strfield7 = strNewValue;
						
						listRow.add(strfield4.trim().length()>0?strfield4:"null");
						listRow.add(strfield5.trim().length()>0?strfield5:"null");
						listRow.add(strfield6.trim().length()>0?strfield6:"null");
						listRow.add(strfield7.trim().length()>0?strfield7:"null");

						strNewValue="";	
						
						
						//System.out.println("final:" + strRowOrg);
					}
					
					if (countIterator==13) {

						strNewValue= strRowAux.substring(0,526);
						//System.out.println("final:" + strNewValue);
						strNewValue= strRowAux.replace(strNewValue, "");
						//System.out.println("final:" + strNewValue);
						strRowOrg = "";

						
					}
					if (!strNewValue.equals("")) {
					listRow.add(strNewValue);
					}


				}
			}
			objRows[i]= listRow;
			//System.out.println(i);
			//System.out.println(objRows[i].toString());
		}
		}
		return objRows;
		
	
	}
	
	
	public static int getFieldValue(String strValue) {
		

		int result=0;
		String newCharacter="";
		String oldCharacter="";
		String strCha ="";
		int countSpace=0;
		char[] ch =  strValue.toCharArray();
		 for (int i = 0; i < strValue.length(); i++) {
			
			 oldCharacter = String.valueOf(ch[i]);
			 newCharacter =  String.valueOf(ch[i+1]);
			 	if (newCharacter.equals(" ") && oldCharacter.equals(" ")) {
			 		countSpace++;
			 	}else {
					 strCha = strCha + oldCharacter;
					// System.out.println(strCha);
					 if (countSpace>=2 && !newCharacter.equals(" ") ) {
						i = strValue.length()+2;
					 }
			 	}
	        }
		 result = countSpace + strCha.length();
		return result;
	}
	
	public static int getFieldValueReverse(String strValue) {
		
		//System.out.println("reversa: " + strValue.trim() + ":");

		
		int result=0;
		String newCharacter="";
		String oldCharacter="";
		String strCha ="";
		int countSpace=0;
		char[] ch =  strValue.toCharArray();
		
		//System.out.println("reversa count: " + ch.length + ":");

		 for (int i = strValue.length()-1; i >0 ; i--) {
			
			 oldCharacter = String.valueOf(ch[i]);
			
			 	if ( oldCharacter.equals(" ")) {
			 		countSpace++;
			 	}else {
					 strCha = strCha + oldCharacter;
					// System.out.println(strCha);
					 if (countSpace>=1 && !newCharacter.equals(" ") ) {
						i =-1;
					 }
			 	}
	        }
		 result = countSpace + strCha.length();
		return result;
	}
	
	public static String loadLinesObject(Object[] objRows,String strProcessID,String StrFilename) {
		
		String strmsg="";
		Long intnewLine=0L;
		
		ImdlvVoucherData headerImporData = OBDal.getInstance().get(ImdlvVoucherData.class, strProcessID);

		
		for (int j =1;j< objRows.length;j++) {
			
			String strValidateRow = objRows[j]==null?"":(objRows[j].toString());
			
			//if (strValidateRow.trim().length()<=4) {
			//	continue;
		//	}
			
			if(objRows[j]!=null) {
				//System.out.println(objRows[j].toString());
				//System.out.println("************************************");
				//System.out.println("*****CREAR OBJETO LINEAS TMP********");
				//System.out.println("************************************");
				System.out.println("LINEA:" + j );
				
				List<String> listRow = new ArrayList<String>(Arrays.asList(strValidateRow.split(",")));;
				int counList = listRow.size();
			    
				if (counList>0 && counList<2) {
					continue;
				}
				
				String strAuthorization = (listRow.get(10).toString().trim().replace("]", ""));
				
				OBCriteria<ImdlvVoucherDataLine> obcLinesTmp = OBDal.getInstance().createCriteria(
						ImdlvVoucherDataLine.class);
				obcLinesTmp.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_AUTHORIZATIONNO,
						strAuthorization));
				int countRepeatAuth = obcLinesTmp.list().size();
				//No inserta el registro cuando una autorización esta registrada. 
		
				if (countRepeatAuth>0) {
					continue;
				}
				
				intnewLine=intnewLine+10;

				ImdlvVoucherDataLine newLine = OBProvider.getInstance().get(ImdlvVoucherDataLine.class);
				newLine.setImdlvVoucherPurchase(headerImporData);
				newLine.setLine(new BigDecimal(intnewLine));
				String strYpDoc = listRow.get(0).toString().trim();
				newLine.setDocumenttype(strYpDoc.replace("[", ""));
				newLine.setInvoiceno(listRow.get(1).toString().trim());
				newLine.setTaxid(listRow.get(2).toString().trim());
				
				
				
				String StrReplaceSocialname =listRow.get(3).toString().trim();
				StrReplaceSocialname = StrReplaceSocialname.replace("*coma*", ",").replace("*puntocoma*", ";");
				newLine.setBpartner(StrReplaceSocialname);
				Date date1=null;
				try {
					date1=new SimpleDateFormat("dd/MM/yyyy").parse(listRow.get(4).toString().trim());
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				newLine.setDateemision(date1); // Fecha del archivo
				date1=null;
				try {
					String strDate = listRow.get(5).toString().trim();
					date1= new  java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(strDate);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				newLine.setDateauthorization(date1);
				newLine.setSubdocumenttype(listRow.get(6).toString().trim());
				newLine.setDocumentaffected(listRow.get(8).toString().trim());
				
				newLine.setKeyacess(listRow.get(9).toString().trim());
				newLine.setAuthorizationno(listRow.get(10).toString().trim().replace("]", ""));


				
				newLine.setSubtotal(BigDecimal.ZERO);
				newLine.setVat(BigDecimal.ZERO);
				newLine.setTotalinvoice(BigDecimal.ZERO);

				newLine.setStatusinvoice("ND");
				newLine.setReferenceinvoice("ND");
				newLine.setStatusemail("ND");
				newLine.setEmails("ND");
				
				 OBDal.getInstance().save(newLine);
				

				 strmsg="OK";
			}
		}
		
		return strmsg;
	}
	
	public String getFileName(String recordId) throws OBException {
		OBContext.setAdminMode(true);
		OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
		final Table table = OBDal.getInstance().get(Table.class, "094B879B858745BA878EFC56ECFEC067");
		obc.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordId));
		obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
		obc.add(Restrictions.eq(Attachment.PROPERTY_DATATYPE, "text/csv"));
		obc.addOrderBy(Attachment.PROPERTY_SEQUENCENUMBER, false);
		obc.setFilterOnReadableOrganization(false);
		obc.setMaxResults(1);
		Attachment attach = (Attachment) obc.uniqueResult();
		if (attach == null) {
			return "";
		}
		return attach.getName().replace(".CSV", "").replace(".txt", "");
	}
	
	public String getFileNameExt(String recordId) throws OBException {
		OBContext.setAdminMode(true);
		OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
		final Table table = OBDal.getInstance().get(Table.class, "094B879B858745BA878EFC56ECFEC067");
		obc.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordId));
		obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
		obc.add(Restrictions.eq(Attachment.PROPERTY_DATATYPE, "text/plain"));
		obc.addOrderBy(Attachment.PROPERTY_SEQUENCENUMBER, false);
		obc.setFilterOnReadableOrganization(false);
		obc.setMaxResults(1);
		Attachment attach = (Attachment) obc.uniqueResult();
		if (attach == null) {
			return "";
		}
		return attach.getName();
	}
	
	public int getFileNameCount(String filename) throws OBException {
		OBContext.setAdminMode(true);
		OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
		final Table table = OBDal.getInstance().get(Table.class, "094B879B858745BA878EFC56ECFEC067");
		obc.add(Restrictions.eq(Attachment.PROPERTY_NAME, filename));
		obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
		obc.add(Restrictions.eq(Attachment.PROPERTY_DATATYPE, "text/plain"));
		obc.addOrderBy(Attachment.PROPERTY_SEQUENCENUMBER, false);
		obc.setFilterOnReadableOrganization(false);
		
		
		return obc.list().size();
	}
	
public static String ValidateloadLinesObject(Object[] objRows) {
		
		String strmsg="";
		Long intnewLine=0L;
		String strMsg="";
		int countLinesNotInsert=0;
		
		for (int j =1;j< objRows.length;j++) {
			
			String strValidateRow = objRows[j]==null?"":(objRows[j].toString());

			if(objRows[j]!=null) {
				//System.out.println(objRows[j].toString());
				//System.out.println("************************************");
				//System.out.println("*****Validar si existen autorizaciones registradas********");
				//System.out.println("************************************");
				System.out.println("LINEA:" + strValidateRow );
				
				List<String> listRow = new ArrayList<String>(Arrays.asList(strValidateRow.split(",")));;
				int counList = listRow.size();
			    
				if (counList>0 && counList<2) {
					continue;
				}
				

		
				String strAuthorization = (listRow.get(10).toString().trim().replace("]", ""));
				
				OBCriteria<ImdlvVoucherDataLine> obcLinesTmp = OBDal.getInstance().createCriteria(
						ImdlvVoucherDataLine.class);
				obcLinesTmp.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_AUTHORIZATIONNO,
						strAuthorization));
				int countRepeatAuth = obcLinesTmp.list().size();
		
				if (countRepeatAuth>0) {
					strMsg = strMsg + strAuthorization+ ",";
				}else {
					countLinesNotInsert++;
				}
				
				
				// System.out.println("Linea insertada:" + j);

				 
			}
		}
		if (strMsg.equals("")) {
			strMsg="OK";
		}else {
			
			if (countLinesNotInsert==0) {
				strMsg = "Error\nTodas las lineas han sido registradas anteriormente";
			}else {	
				
				if (countLinesNotInsert>0) {
					strMsg="OK";
				}else {
					strMsg = "Error\nAutorizaciones registradas:" + strMsg ;
				}
			}

		}
		return strMsg;
	}


public Object[] ReadPurchaseLines(ImdlvVoucherData purchaseHeader,boolean flagError) {
	
	Object[] ObjMsg= null;
	ObjMsg= new Object[2];
	Object[] ObjLinesMsg=null;
	Object[] ObjMsgByLines=new Object[2];
	int countLinesError=0;
	int countTotalLinesprocess=0;
	int countPartnerOK=0;
	Object[] objDataDefaultMsg=	ValidateDataDefault(); // Validar datos por defecto
	int countErrors=0;
	objDataDefaultMsg[0]="Continue";
	if (objDataDefaultMsg[0].toString().equals("Continue")) {		
			
					
			OBCriteria<ImdlvVoucherDataLine> obc = OBDal.getInstance().createCriteria(ImdlvVoucherDataLine.class);
			obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_IMDLVVOUCHERPURCHASE, purchaseHeader));
			obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ISPROCESS, false));
		
			List<ImdlvVoucherDataLine> lstPurchLine = obc.list();
			
			if (lstPurchLine.size()>0) {
				
				ObjLinesMsg= new Object[lstPurchLine.size()];
				countTotalLinesprocess=lstPurchLine.size();
				for (ImdlvVoucherDataLine colPurchLine:lstPurchLine) {
					

					
					String strValue = colPurchLine.getTaxid();

					BusinessPartner BPartner = ValidateBPartner(strValue);
					
					if (BPartner==null) {
						ConnectionProvider connPartner = new DalConnectionProvider(false);
						String StrPartnerID="";
						
						StrPartnerID = getUUID(connPartner);
						String StrLocationID = getUUID(connPartner);
						String StrPartnerLocationID = getUUID(connPartner);
						String strOrg = OBContext.getOBContext().getCurrentOrganization().getId();
						String strMsgLocation="";
						
						// Insertar Tercero

						
						Object[] objInsertPartner= savePartnerDB(colPurchLine,StrPartnerID,flagError);
						
						strMsgLocation = objInsertPartner[0].toString();
						
						if (strMsgLocation.contains("ERRCED")) {
							
							objInsertPartner= savePartnerDB(colPurchLine,StrPartnerID,true);

							ObjMsgByLines[0]=colPurchLine.getId();
							ObjMsgByLines[1]=objInsertPartner[1];
							ObjLinesMsg[countLinesError]= ObjMsgByLines;
							countLinesError++;
							String strSQL= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || ' Msg: "+objInsertPartner[1].toString() +"' where imdlv_voucherpurchline_id ='"+ colPurchLine.getId()  +"'";
							Object[] objInvoice = executeInserts(strSQL);
							countErrors++;
							strMsgLocation = objInsertPartner[0].toString();
							if (strMsgLocation.contains("ERROR")) {
								ObjMsgByLines[0]=colPurchLine.getId();
								ObjMsgByLines[1]=objInsertPartner[1];
								ObjLinesMsg[countLinesError]= ObjMsgByLines;
								countLinesError++;
								String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || 'Msg: "+objInsertPartner[1].toString() +"' where imdlv_voucherpurchline_id ='"+ colPurchLine.getId()  +"'";
								Object[] objInvoice2 = executeInserts(strSQL2);
								countErrors++;
								continue;
							}
							
							
							
						}else {
							strMsgLocation = objInsertPartner[0].toString();
							if (strMsgLocation.contains("ERROR")) {
								//strMsgLocation=objInsertPartner[1].toString();
								//ObjMsg[0]="Error";
								//ObjMsg[1]=strMsgLocation.replace("Error", "");
								//return ObjMsg;
								ObjMsgByLines[0]=colPurchLine.getId();
								ObjMsgByLines[1]=objInsertPartner[1];
								ObjLinesMsg[countLinesError]= ObjMsgByLines;
								countLinesError++;
								String strSQL3= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || 'Msg: "+objInsertPartner[1].toString() +"' where imdlv_voucherpurchline_id ='"+ colPurchLine.getId()  +"'";
								Object[] objInvoice3 = executeInserts(strSQL3);
								countErrors++;
								continue;
							}
							
							
						}
						
				        String strAddress =colPurchLine.getKeyacess(); // direccion 
				        String strPhone ="--";
					
				        //Insertar dirección
						Object[] objLocation =InsertLocation(strOrg, strAddress,StrLocationID);
						
						strMsgLocation = objLocation[0].toString();
						
						if (strMsgLocation.contains("ERROR")) {
							strMsgLocation=objLocation[1].toString();
							ObjMsg[0]="Error";
							ObjMsg[1]=strMsgLocation.replace("Error", "");
							countErrors++;
							return ObjMsg;
						}
						strMsgLocation="";
						
						//Insertar y ligar dirección al tercero
						Object[] objPartnerLocation=InsertPartnerLocation(strOrg, strAddress,strPhone,StrLocationID,StrPartnerID,StrLocationID);
						strMsgLocation =objPartnerLocation[0].toString();
						if (strMsgLocation.contains("ERROR")) {
							strMsgLocation=objLocation[1].toString();
							ObjMsg[0]="Error";
							ObjMsg[1]=strMsgLocation.replace("Error", "");
							countErrors++;
							return ObjMsg;
						}
						
						countPartnerOK++;
						
						try {
							connPartner.destroy();
						} catch (Exception e) {
							ObjMsg[0]="OK";
							ObjMsg[1]="OK";							}
					}else {
						ConnectionProvider connPartner = new DalConnectionProvider(false);
						countPartnerOK++;
						if (BPartner.getBusinessPartnerLocationList().size()==0){
						String StrPartnerID = BPartner.getId();
						
						/*
						 * VALIDAR TERCERO - CONFIGURACIÓN TIPO DE CONTRIBUYENTE
						 * */
						
						OBCriteria<ImdlvPurchaseIinvoice> obcDefaultTaxPayer = OBDal.getInstance().createCriteria(ImdlvPurchaseIinvoice.class);
					    obcDefaultTaxPayer.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_ACTIVE, true));
						
						
					
					        	
						String strAddress =colPurchLine.getKeyacess(); 
					    String strPhone ="--";
						String StrLocationID = getUUID(connPartner);
						String StrPartnerLocationID = getUUID(connPartner);
						String strOrg = OBContext.getOBContext().getCurrentOrganization().getId();
						String strMsgLocation="";
						  //Insertar dirección
						Object[] objLocation =InsertLocation(strOrg, strAddress,StrLocationID);
						
						strMsgLocation = objLocation[0].toString();
						
						if (strMsgLocation.contains("ERROR")) {
							strMsgLocation=objLocation[1].toString();
							ObjMsg[0]="Error";
							ObjMsg[1]=strMsgLocation.replace("Error", "");
							countErrors++;
							return ObjMsg;
						}
						strMsgLocation="";
						
						//Insertar y ligar dirección al tercero
						Object[] objPartnerLocation=InsertPartnerLocation(strOrg, strAddress,strPhone,StrLocationID,StrPartnerID,StrLocationID);
						strMsgLocation =objPartnerLocation[0].toString();
						if (strMsgLocation.contains("ERROR")) {
							strMsgLocation=objPartnerLocation[1].toString();
							ObjMsg[0]="Error";
							ObjMsg[1]=strMsgLocation.replace("Error", "");
							countErrors++;
							return ObjMsg;
						}
					}
						
						try {
							connPartner.destroy();
						} catch (Exception e) {
							ObjMsg[0]="OK";
							ObjMsg[1]="OK";		
					}
				}
				
			}
			
	}else {
		

		ObjMsg = objDataDefaultMsg;
		
	}
	
	if (countLinesError>0) {
		
		for (int i=0;i< ObjLinesMsg.length;i++) {
			
			Object[] objLines= (Object[]) ObjLinesMsg[i];
			System.out.println(i);
			if (objLines!=null) {
				String strLinesID = objLines[0].toString();
				String strLinesMsgError = objLines[1].toString();
				if (strLinesMsgError.contains("Error")) {
					strLinesMsgError="Se asignó tipo de documento = Pasaporte";
				}
					//CsspidPurchaseImportDataLine UpdateLine = OBDal.getInstance().get(CsspidPurchaseImportDataLine.class,strLinesID );
					//UpdateLine.setlogserror(strLinesMsgError);
					//OBDal.getInstance().save(UpdateLine);
					
					String strSQL= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || 'Msg: "+strLinesMsgError +"' where imdlv_voucherpurchline_id ='"+ strLinesID  +"'";
					Object[] objInvoice = executeInserts(strSQL);
					//OBDal.getInstance().commitAndClose();
				
			}
			
		}
		//OBDal.getInstance().commitAndClose();
		
		//ObjMsg[0]="Reprocesar";
		//ObjMsg[1]="Reprocesar";
	}
	
	if(countLinesError==countTotalLinesprocess) {
		ObjMsg[0]="Error";
		ObjMsg[1]="Todas las líneas se encuentran con error";
		countErrors++;
	}else if (countLinesError<countTotalLinesprocess) {
		
		if (countPartnerOK>0) {
			ObjMsg[0]="OK";
			ObjMsg[1]="Revise los log, algunas líneas se encuentran con error";
		}else {
			countErrors++;
			ObjMsg[0]="Error";
			ObjMsg[1]="Revise los log, las líneas se encuentran con error";
		}
	}
	
	if(countLinesError==0) {
		
		ObjMsg[0]="OK";
		ObjMsg[1]="Todas las líneas han sido registradas";
	}
	
}
	return ObjMsg;

}

public Object[] InsertPurchaseLines(ImdlvVoucherData purchaseHeader) {
	Object[] ObjMsg= new Object[2];
	Object[] ObjLinesMsg= null;
	Object[] ObjLinesByMsg= new Object[2];
	int intIdx=0;
	
	
	OBCriteria<ImdlvVoucherDataLine> obcLines = OBDal.getInstance().createCriteria(ImdlvVoucherDataLine.class);
	obcLines.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_IMDLVVOUCHERPURCHASE, purchaseHeader));
	obcLines.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ISPROCESS, false));
	List<ImdlvVoucherDataLine> lstLine = obcLines.list();
	
	if (lstLine.size()>0) {
		for(ImdlvVoucherDataLine imdlvLines:obcLines.list()) {
			
			ImdlvVoucherDataLine updLines= OBDal.getInstance().get(ImdlvVoucherDataLine.class, imdlvLines.getId());
			String strXMLupd= imdlvLines.getXMLSri();
			
			String strHeader="";
			Object[] obLine=null;
			if (updLines.getDescription().equals("comprobanteRetencion")) {
				 obLine=getDataHeaderRetencionesXML(strXMLupd,"comprobanteRetencion");
			}else {
				if (updLines.getDescription().equals("notaCredito")) {
					strHeader ="infoNotaCredito";
				}
				if (updLines.getDescription().equals("factura")) {
					strHeader ="infoFactura";
				}
				if (updLines.getDescription().equals("notaDedito")) {
					strHeader ="infoNotaDedito";
				}
				obLine=getDataHeaderXML(strXMLupd,strHeader);
			} 
			BigDecimal bgTotalBase= new BigDecimal(obLine[0].toString()) ;
			BigDecimal bgTotalTax = new BigDecimal(obLine[2].toString());
			BigDecimal bgTotal= bgTotalTax.add(bgTotalBase);
			updLines.setTotalinvoice(bgTotal);
			updLines.setVat(bgTotalTax);
			updLines.setSubtotal(bgTotalBase);
			OBDal.getInstance().save(updLines);

		}
		OBDal.getInstance().flush();
	}
	
	
	OBCriteria<ImdlvVoucherDataLine> obc = OBDal.getInstance().createCriteria(ImdlvVoucherDataLine.class);
	obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_IMDLVVOUCHERPURCHASE, purchaseHeader));
	obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ISPROCESS, false));

	List<ImdlvVoucherDataLine> lstPurchLine = obc.list();
	
	if (lstPurchLine.size()>0) {
				ObjLinesMsg= new Object[lstPurchLine.size()];
			
				for (ImdlvVoucherDataLine colPurchLines: obc.list()) {
					
					if (colPurchLines.getDescription().equals("comprobanteRetencion")) {
						continue;
					}
					
						ConnectionProvider connPartner = new DalConnectionProvider(false);
		
						String StrInvoiceID ="";
						StrInvoiceID= getUUID(connPartner);
						StrInvoiceID= getUUID(connPartner);
						String strUserId= OBContext.getOBContext().getUser().getId();
						String strOrgId= OBContext.getOBContext().getCurrentOrganization().getId();
						String strClienId= OBContext.getOBContext().getCurrentClient().getId();
						String strTaxid=colPurchLines.getTaxid();
						BusinessPartner BPartner = ValidateBPartner(strTaxid);
						if (BPartner==null) {
							continue;
						}
						String strPartnerLocationID ="";
						int intPartnerLocation = BPartner.getBusinessPartnerLocationList().size();
						if (intPartnerLocation>0) {
							
							strPartnerLocationID=BPartner.getBusinessPartnerLocationList().get(0).getId();
						}

						String strMailBp ="";
						
						try {
							strMailBp=BPartner.getEEIEmail();
						}catch(Exception excemail) {
							
						}
						int counMail=0;
						counMail=strMailBp.length();
						
						if (counMail==0) {
				        	
								String StrMsgError="Msg: No se puede procesar la factura por falta de correo electrónico.  ";
								String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
								Object[] objInvDate = executeInserts(strSQLDate);
					        	continue;
				        }
	
						
						String strDocumetnNo ="";
						String strDocTypeID="";
						String strValDocTypeID="";
						String strWithhDoctypeId="null";
						String strLiveliHoodId="null";
						String strCodeLiveliHoodId="null";
						String strAuthorizationWithh="null";
						String strValidDocumentNo = colPurchLines.getInvoiceno().replace("-", "");
						String strProductID="";
						
						
						boolean isNumeric =  strValidDocumentNo.matches("[+-]?\\d*(\\.\\d+)?");
						boolean isElectronic = false;
						double dblWithhAmount = 0; //colPurchLines.getPerctRentxls().doubleValue();
						double dblWithhAmount2 = 0; //colPurchLines.getPerctVatxls().doubleValue();
						
						OBCriteria<ImdlvPurchaseIinvoice> purchaseInvoice = OBDal.getInstance().createCriteria(ImdlvPurchaseIinvoice.class);
						purchaseInvoice.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_ACTIVE, true));
						//purchaseInvoice.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_DOCTYPEPURCHASE, purchaseHeader.getDoctype()));

						
						if (purchaseInvoice.list().size()>0) {
							
						
							
							/*
							 * VALIDAR TERCERO - CONFIGURACIÓN TIPO DE CONTRIBUYENTE
							 * */
							
							String strProduct_Rent="ND";
							String strProduct_VAT="ND";
							
							
								
							OBCriteria<ImdlvPurchaseIinvoice> purchaseInvoiceDf = OBDal.getInstance().createCriteria(ImdlvPurchaseIinvoice.class);
							purchaseInvoiceDf.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_ACTIVE, true));
							//purchaseInvoiceDf.add(Restrictions.isNotNull(ImdlvPurchaseIinvoice.PROPERTY_PRODUCTDEFAULT));
					        
						
							strProductID = purchaseInvoiceDf.list().get(0).getProductDefault().getId();
							
							strWithhDoctypeId ="'" + purchaseInvoiceDf.list().get(0).getDoctypeWithholding().getId() +"'" ; 
							strDocTypeID=purchaseInvoiceDf.list().get(0).getDoctypePurchase().getId();
							strLiveliHoodId = "'" +purchaseInvoiceDf.list().get(0).getSswhLivelihoodt().getId()+"'";
							strCodeLiveliHoodId = "'" +purchaseInvoiceDf.list().get(0).getSswhCodelivelihoodt().getId()+"'";
							isElectronic=true;
							
							if (purchaseInvoice.list().get(0).getDoctypeWithholding().getSSWHAuthorizationList().size()>0) {
								strAuthorizationWithh = "'" +purchaseInvoiceDf.list().get(0).getDoctypeWithholding().getSSWHAuthorizationList().get(0)
										.getAuthorizationno()+ "'" ;
							}
							
							DocumentType docObj = OBDal.getInstance().get(DocumentType.class, strDocTypeID);
						    
							if (docObj.getDocumentSequence()!=null) {
						     
								Sequence seq = docObj.getDocumentSequence();
						      
								strDocumetnNo = seq.getNextAssignedNumber().toString();
						      
								seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
						      
								OBDal.getInstance().save(seq);
						      
						    }else {
						    	strDocumetnNo="001-001-000000001";
						    }
						    
						    
						}
						String strPoreference = colPurchLines.getInvoiceno();
						
							
							
						String StrDateInvoice ="";
						String StrDueDate="null";
						String strAuthorizationSRI="null";
						DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");  
						//String strDate = dateFormat.format(date);  
						
						String strAuthorizariontmp = colPurchLines.getAuthorizationno()==null?"":colPurchLines.getAuthorizationno();
						strAuthorizariontmp="9999999999";
						
						if (!strAuthorizariontmp.equals("9999999999")) {
							String strd1 =dateFormat.format(colPurchLines.getDateauthorization().toString());
							System.out.println(strd1);
							StrDateInvoice = dateFormat.format(colPurchLines.getDateemision()).toString();
							strAuthorizariontmp.trim();
							int countDteDue = strAuthorizariontmp.trim().length();
							if (!strAuthorizariontmp.equals("") && countDteDue >0 ) { 
								strAuthorizationSRI = "'" + strAuthorizariontmp + "'";
								
								String formatDate="";
								
								try {
									formatDate = dateFormat.format(colPurchLines.getDateemision());

								} catch(Exception excp) {
									
								}
								if (formatDate.equals("")) {
									
									String StrMsgError="Msg: Falta la fecha de vencimiento.   ";
									String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || '   ',' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
									Object[] objInvDate = executeInserts(strSQLDate);
									continue;
								}
								
								formatDate = dateFormat.format(colPurchLines.getDateemision());
								String strReplace = formatDate.replace("-", "");
								String[] arrSplit = formatDate.split("-");
								String strMonth = arrSplit[1].toString();
								String stryear = arrSplit[0].toString();
								String strday =""; 
								
								if (strMonth.equals("01")) {
									strday="31";
								}
								if (strMonth.equals("02")) {
									if (Integer.parseInt(stryear) % 4==0) {
										strday="29";
									}else {
										strday="28";
									}
								}
								if (strMonth.equals("03")) {
									strday="31";
								}
								if (strMonth.equals("04")) {
									strday="30";
								}
								if (strMonth.equals("05")) {
									strday="31";
								}
								if (strMonth.equals("06")) {
									strday="30";
								}
								if (strMonth.equals("07")) {
									strday="31";
								}
								if (strMonth.equals("08")) {
									strday="31";
								}
								if (strMonth.equals("09")) {
									strday="30";
								}
								if (strMonth.equals("10")) {
									strday="31";
								}
								
								if (strMonth.equals("11")) {
									strday="30";
								}
								if (strMonth.equals("12")) {
									strday="31";
								}
								
								StrDueDate ="'"+ stryear + "-" + strMonth + "-" + strday + "'"; 
								
								//StrDueDate="'"+ dateFormat.format(colPurchLines.getDateinvoicedxls())+ "'";

							}

						}else if( strAuthorizariontmp.equals("9999999999")) {
							
							
							if (colPurchLines!=null) {
								
								StrDateInvoice = dateFormat.format(colPurchLines.getDateemision()).toString();
								
								String strValidDate = "ND";
								try
								{
										strValidDate =colPurchLines.getDateemision()==null?"ND":"Continue";
								
								}catch(Exception exsq) {
									
								}
								
								if (strValidDate.equals("ND")) {
									String StrMsgError="Msg: Falta la fecha de vencimiento. ";
									String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
									Object[] objInvDate = executeInserts(strSQLDate);
									ObjMsg[0]="Error";
									ObjMsg[1]="Falta la fecha de vencimiento";
									countErrors++;
									continue;
									//return ObjMsg;
								}
								
								String formatDate = dateFormat.format(colPurchLines.getDateemision());
								String strReplace = formatDate.replace("-", "");
								String[] arrSplit = formatDate.split("-");
								String strMonth = arrSplit[1].toString();
								String stryear = arrSplit[0].toString();
								String strday =""; 
								
								if (strMonth.equals("01")) {
									strday="31";
								}
								if (strMonth.equals("02")) {
									if (Integer.parseInt(stryear) % 4==0) {
										strday="29";
									}else {
										strday="28";
									}
								}
								if (strMonth.equals("03")) {
									strday="31";
								}
								if (strMonth.equals("04")) {
									strday="30";
								}
								if (strMonth.equals("05")) {
									strday="31";
								}
								if (strMonth.equals("06")) {
									strday="30";
								}
								if (strMonth.equals("07")) {
									strday="31";
								}
								if (strMonth.equals("08")) {
									strday="31";
								}
								if (strMonth.equals("09")) {
									strday="30";
								}
								if (strMonth.equals("10")) {
									strday="31";
								}
								
								if (strMonth.equals("11")) {
									strday="30";
								}
								if (strMonth.equals("12")) {
									strday="31";
								}
								
								StrDueDate = stryear + "-" + strMonth + "-" + strday; 
								
								strAuthorizationSRI = "'" + colPurchLines.getAuthorizationno() + "'";
							}else {
								StrDateInvoice = dateFormat.format(colPurchLines.getDateemision().toString());

							}
							StrDueDate="'"+ dateFormat.format(colPurchLines.getDateemision())+ "'";
							
						}
						strAuthorizationSRI = "'" + colPurchLines.getAuthorizationno() + "'";
						strAuthorizationWithh = "'" + colPurchLines.getAuthorizationno().substring(0, 10) + "'" ;

						String strCurrencyID="USD";
						String StrDateAcctInvoice =StrDateInvoice;
						String strCurrency="(select c_currency_id from c_currency where iso_code='"+ strCurrencyID +"')";
						String strPaymentTermID ="";
						String strPriceListID="";
						String strGroupPartner="";
						
						String strFinPaymentMethodID ="";
						String strPaymentMethodXLS = "";
						
						
						OBCriteria<ImdlvPartner> obcDefaultPartner = OBDal.getInstance().createCriteria(ImdlvPartner.class);
						obcDefaultPartner.add(Restrictions.eq(ImdlvPartner.PROPERTY_ACTIVE, true));
						
						if (obcDefaultPartner.list().size()>0) {
							
							strPaymentTermID= obcDefaultPartner.list().get(0).getPaymentterm().getId();
							strPriceListID= obcDefaultPartner.list().get(0).getPricelist().getId();
							strGroupPartner = obcDefaultPartner.list().get(0).getBpGroup().getId();
							strFinPaymentMethodID= obcDefaultPartner.list().get(0).getFINPaymentmethod().getId();
							strCurrency = obcDefaultPartner.list().get(0).getCurrency().getId();
						}
						
						
						//colPurchLines.getPaymentmethodxls();
						
						/*
						 * validar si existe la factura
						 * */
						
						//strPoreference

						if (!strPoreference.equals("null") && !strPoreference.equals("ND") &&  !strPoreference.equals(null) ) {
						
							OBCriteria<Invoice> obcInvoiceSearch = OBDal.getInstance().createCriteria(Invoice.class);
							obcInvoiceSearch.add(Restrictions.eq(Invoice.PROPERTY_ORDERREFERENCE, strPoreference));
							obcInvoiceSearch.add(Restrictions.eq(Invoice.PROPERTY_BUSINESSPARTNER, BPartner));

							if (obcInvoiceSearch.list().size()>0) {
								
								
								String StrMsgError="Msg: La factura ya se encuentra registrada. ";
								String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
								Object[] objInvDate = executeInserts(strSQLDate);
								ObjMsg[0]="Error";
								ObjMsg[1]=StrMsgError;
								countErrors++;
								continue;
								
								
							}
						
						}
						
						
						
						
						
						String strSQL="Insert into c_invoice("+
											"c_invoice_id,				ad_client_id,		ad_org_id,		isactive, "+
											"created,					createdby,			updated,		updatedby, "+
											"issotrx,					documentno,			docstatus,		docaction, "+
											"processed,					posted,				c_doctype_id,	c_doctypetarget_id, "+
											"isprinted,					dateinvoiced,		dateacct,		c_bpartner_id, "+
											"c_bpartner_location_id,	isdiscountprinted,	c_currency_id,	c_paymentterm_id, "+			
											"totallines,				grandtotal,			m_pricelist_id,	isselfservice, "+
											"ispaid,					totalpaid,			outstandingamt,	daystilldue, "+
											"dueamt,			updatepaymentmonitor, 		iscashvat,		prepaymentamt, "+		
											"em_ssre_isrefunded,		em_sswh_totalwithholdingincome,		em_sswh_totalwithholdingvat, "+
											"em_sswh_iseinvoice,			em_scnr_isref_inv, "+
											"em_eei_generated,			em_eei_is_inv_ref, "+
											"em_eei_withholding_send,	em_eei_generate_offline "+
											//",em_sscutxt_is_cert_qa" +
											",fin_paymentmethod_id,poreference,EM_Sswh_C_Doctype_ID,EM_Sswh_Livelihood"+
											",EM_Sswh_Codelivelihood,EM_Sswh_Authorization,EM_Sswh_Datewithhold"+
											",processing,paymentrule,EM_Sswh_Nroauthorization,EM_Sswh_Expirationdate,description"+
											") values("+
											"'"+ StrInvoiceID+"','"+strClienId +"','"+ strOrgId +"','Y',"+
											"now(),'"+strUserId +"', now(), '"+ strUserId+"',"+
											"'N','"+ strDocumetnNo+"','DR','CO',"+
											"'N','N','"+ strDocTypeID+"','"+strDocTypeID+"',"+
											"'N','"+ StrDateInvoice +"','" + StrDateAcctInvoice +"','"+ BPartner.getId()  +"',"+
											"'"+ strPartnerLocationID +"','N',"+strCurrency+",'"+strPaymentTermID+"',"+
											"0,0,'"+strPriceListID+"','N',"+
											"'N',0,0,0,"+
											"0,'N','N',0,"+
											"'N',0,0,"+
											"'"+ (isElectronic?'Y':'N') +"','N',"+
											"'N','N',"+
											"'N','N'"+
											//",NULL"+
											",'"+ strFinPaymentMethodID +"','"+strPoreference +"',"+strWithhDoctypeId+","+strLiveliHoodId+""+
											","+strCodeLiveliHoodId +","+strAuthorizationWithh+",'"+StrDateInvoice+ "'"+
											",'N','P'," + strAuthorizationSRI+ ","+ StrDueDate+ ",'" + (purchaseHeader.getDocumentno() + " - " + purchaseHeader.getFilenamedata())+ "'"+
											")"
											;	
						//Object[] objInvoice = InsertInvoice(connPartner,strSQL);
						System.out.println("Fact: " + colPurchLines.getInvoiceno() );
						
						Object[] objInvoice = executeInserts(strSQL);
						System.out.println("c_invoice : " +strSQL);

						
						if (objInvoice[0].toString().contains("ERROR")) {
							intIdx++;
							ImdlvVoucherDataLine updateLinesPl= OBDal.getInstance().get(ImdlvVoucherDataLine.class, colPurchLines.getId());
							String strLog = "";
							try {
							strLog = updateLinesPl.getLogserror().trim();
							}catch(Exception ee) {
								
							}
							String strNewMSg = objInvoice[1].toString();
							if (strNewMSg.contains("SSWH_NoDocumentNoFormat")) {
								ObjMsg[1]="Msg: El formato de la autorización debe ser '001-001-000000001'. ";
							}else if (strNewMSg.contains("SSWH_SupplierReferenceUnauthorized")) {
								ObjMsg[1]="Msg: Referencia del proveedor no autorizada. ";
							}else if (strNewMSg.contains("SSWH_WithholdingUnauthorized")){
								ObjMsg[1]="Msg: El tipo de documento configurado en 'Tipo de Factura de Compra' no tiene creado la autorización. ";
								
							}else if (strNewMSg.contains("c_invoice_fin_paymentmethod")){
								ObjMsg[1]="Msg: El método de pago '"//+ colPurchLines.getPaymentmethodxls()
								+  "' no se encuentra homologado. ";
								
							}else {
								ObjMsg[1]=objInvoice[1];
							}
							updateLinesPl.setLogserror(strLog +" - " + ObjMsg[1].toString() );
							OBDal.getInstance().save(updateLinesPl);
							ObjMsg[0]="Error";
							
							countErrors++;
							//return ObjMsg;
						}else {
							
							String strTax12= "'IVA 12%'";
							String strTax0= "'IVA 0% - 01/02/2014'";
							String strTaxResult="";
							String dblbaseamt = "0";
							String dbltaxamt = "0";
							String dblprice ="0";// - ((colPurchLines.getBaseimpxls().doubleValue()<0? -1:1)* colPurchLines.getBaseimpxls().doubleValue());
							String dblqty="0";
							String dblrate="0";
							double CountErrorLine=0;
							String strXml = colPurchLines.getXMLSri();
							Object[] objLinesData= getDataLinesXML(strXml);
							Object[] objInvoiceLines=null;
							//strEtiquetas ="cantidad,precioUnitario,impuestos,impuesto,tarifa,baseImponible,valor";
							int intLine=0;
							for (int printObj=0; printObj<objLinesData.length;printObj++) {
								intLine=intLine+10;
								Object[] strValores = (Object[]) objLinesData[printObj]; 
								
								dblqty=strValores[0]!=null?strValores[0].toString():"0";
								dblprice=strValores[1]!=null?strValores[1].toString():"0";
								
								double dblTotLine = Double.valueOf(dblqty) * Double.valueOf(dblprice);
								String dblLinenetamt= String.valueOf(dblTotLine);
								dblrate=strValores[2]!=null?strValores[2].toString():"0";
								dbltaxamt=strValores[4]!=null?strValores[2].toString():"0";
								if(dblrate.equals("0")) {
									strTaxResult = strTax0;
								}else {
									strTaxResult = strTax12;
								}
								
								String strSqlLines="insert into c_invoiceline"+ 
										"(c_invoiceline_id,   ad_client_id,   ad_org_id,                  isactive,"+
										"created,            createdby,      updated,                    updatedby,"+
										"c_invoice_id,       line,           financial_invoice_line,     qtyinvoiced,"+
										"pricelist,          priceactual,    pricelimit,                 linenetamt,"+
										
										"isdescription,      pricestd,       grosspricestd,              grosspricelist,"+
										"isdeferred,         explode," +
										"m_product_id,c_tax_id,c_uom_id,taxamt,c_bpartner_id,excludeforwithholding,em_imdlv_qty, em_imdlv_price, em_imdlv_totalline, em_imdlv_taxamt, em_imdlv_netprice)"+
										"values("+
										"get_uuid()"+",'"+strClienId +"','"+ strOrgId +"','Y',"+
										"now(),'"+strUserId +"', now(), '"+ strUserId+"',"+
										"'" + StrInvoiceID +"', " + intLine +",'N',"+ dblqty +
										"," + dblprice + ","+ dblprice + ",0,"+ dblLinenetamt +
										",'N'," + dblprice +",0,0,"+
										"'N','N'" +
										",'" + strProductID+"',(select c_tax_id from c_tax where name="+ strTaxResult +"),(select c_uom_id from m_product where m_product_id ='"+ strProductID +"'),"+dbltaxamt+""+
										",(select c_bpartner_id from c_invoice where c_invoice_id ='"+ StrInvoiceID +"'),'N'"+
										"," + dblqty + "," + dblprice  + "," + dblTotLine + "," + dbltaxamt+ "," + dblprice + ")"
										;
								System.out.println("Lines 1: " + strSqlLines);

								 objInvoiceLines  = executeInserts(strSqlLines) ;
								 
								 if (objInvoiceLines[0].toString().contains("ERROR")) {
									 CountErrorLine++;
								 }
							}
							
							
							if (objInvoiceLines[0].toString().contains("ERROR")) {
								intIdx++;
								ImdlvVoucherDataLine updateLinesPl= OBDal.getInstance().get(ImdlvVoucherDataLine.class, colPurchLines.getId());
								String strLog = "";
								try {
								strLog = updateLinesPl.getLogserror().trim();
								}catch(Exception ee) {
									
								}
								
								String strNewMSg = objInvoiceLines[1].toString();
								if (strNewMSg.contains("SSWH_NoDocumentNoFormat")) {
									ObjMsg[1]="Msg: El formato de la autorización debe ser '001-001-000000001'. ";
								}else if (strNewMSg.contains("SSWH_NoWithholdingTaxIncomeForTheDate")){
									ObjMsg[1]="Msg: No se ha encontrado retención (Renta) para la fecha. ";
								}
								else {
									ObjMsg[1]=objInvoiceLines[1];
								}
								
								updateLinesPl.setLogserror(strLog +" - " + ObjMsg[1].toString() );
								OBDal.getInstance().save(updateLinesPl);
								
								if (CountErrorLine>0) {
								
									strSQL= "delete from c_invoicelinetax where c_invoice_id ='"+ StrInvoiceID +"'";
									objInvoice = executeInserts(strSQL);
									
									strSQL= "delete from c_invoiceline where c_invoice_id ='"+ StrInvoiceID +"'";
									objInvoice = executeInserts(strSQL);
									
									strSQL= "delete from c_invoicetax where c_invoice_id ='"+ StrInvoiceID +"'";
									objInvoice = executeInserts(strSQL);
									
									strSQL= "delete from c_invoice where c_invoice_id ='"+ StrInvoiceID +"'";
									objInvoice = executeInserts(strSQL);
									ObjMsg[0]="Error";
								}
								//return ObjMsg;
								countErrors++;
							}
							if (CountErrorLine==0) {
								strSQL= "update imdlv_voucherpurchline set isprocess='Y' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId() +"'";
								objInvoice = executeInserts(strSQL);
								ObjMsg[0]="OK";
							}
							
						} 
						
						try {
							connPartner.destroy();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				
		}
		
	}
	
	if (countErrors>0) {
		ObjMsg[0]="Info";
		ObjMsg[1]= intIdx + " linea(s) con error(es), revise los logs de los registros no procesados para más información";
	}else if (intIdx==0) {
		ObjMsg[0]="OK";
		ObjMsg[1]="Se ha completado correctamente el proceso.";
		
		
	}
	
	return ObjMsg;
	
}

public Object[] getDataLinesXML(String strXML) {
	List<Object> listaLineas = new ArrayList<>();
	  
	String strEtiquetas ="cantidad,precioUnitario,impuestos,impuesto,tarifa,baseImponible,valor";
	Object[] objEtiquetasXML= strEtiquetas.split(",");
	int indx=0;
	  int indxAux=0;
	  Document document = parseXmlFile(strXML);
	  document.getDocumentElement().normalize();
	  document.getDocumentElement().normalize();
      System.out.println("Root Element :" + document.getDocumentElement().getNodeName());
      NodeList nList = document.getElementsByTagName("detalles");
      System.out.println("----------------------------");
      int listxml = nList.getLength();
      for (int intmain = 0; intmain < nList.getLength(); intmain++) {
          Node nNodemain = nList.item(intmain);
          System.out.println("\nCurrent Element :" + nNodemain.getNodeName());
          if (nNodemain.getNodeType() == Node.ELEMENT_NODE) {
              Element eElementmain = (Element) nNodemain;
             // System.out.println("detalle : " + intmain + eElementmain.getAttribute("detalles"));
              
              String StringDetalle = nodeToString(nNodemain);
             // System.out.println(StringDetalle);
              Document documentDetalle = parseXmlFile(StringDetalle);
              NodeList nListDetalles = documentDetalle.getElementsByTagName("detalle");
              Object[] objLines = new Object[5]; 
              indxAux=0;
              for (int intDetalles = 0; intDetalles < nListDetalles.getLength(); intDetalles++) {
                  Node nNodedetalles = nListDetalles.item(intDetalles);
                 // System.out.println("\nCurrent eElementdet :" + nNodedetalles.getNodeName());
                  System.out.println("Iteraciones: "+ intDetalles);
                  if (nNodedetalles.getNodeType() == Node.ELEMENT_NODE) {
                  	Element eElementdet = (Element) nNodedetalles;
                      
                  	String strTag= objEtiquetasXML[indxAux].toString();
                  	
                  	for (int etq=0;etq<objEtiquetasXML.length;etq++) {
                  		strTag= objEtiquetasXML[etq].toString();
                  		//System.out.println("tag :"+ etq + eElementdet.getElementsByTagName(strTag).item(0).getNodeName());
                  		
                  		System.out.println("tag :"+ etq + " - " + strTag+": " + eElementdet.getElementsByTagName(strTag).item(0).getTextContent());
                  		
                  		if (etq<2) objLines[etq] = eElementdet.getElementsByTagName(strTag).item(0).getTextContent();
                  		
                  		String strgNodeName =eElementdet.getElementsByTagName(strTag).item(0).getNodeName();

                      	if (strgNodeName.equals("impuestos") && etq==2){
                      		indxAux=etq+2;
                      		etq=100;
                      		objLines[indxAux] = eElementdet.getElementsByTagName("impuestos").item(0).getTextContent();
                      		String StringImpuestos = nodeToString(nNodedetalles);
                              Document documentImpuestos = parseXmlFile(StringImpuestos);
                              NodeList nListImpuestoss = documentImpuestos.getElementsByTagName("impuestos");
                              
                                for (int intImpuestos = 0; intImpuestos < nListImpuestoss.getLength(); intImpuestos++) {
                                    Node nNodeImpuestos = nListImpuestoss.item(intImpuestos);
                                    //System.out.println("\nCurrent eElementdet :" + nNodedetalles.getNodeName());
                                		

                                    if (nNodeImpuestos.getNodeType() == Node.ELEMENT_NODE) {
                                  	  Element eElementimp = (Element) nNodeImpuestos;
                                  	  /*strTag= objEtiquetasXML[indxAux].toString();
                                  	  indxAux++;
                                       // System.out.println("tarifa : " + eElementimp.getElementsByTagName("tarifa").item(0).getTextContent());
	                                      objLines[3] = eElementimp.getElementsByTagName("tarifa").item(0).getTextContent();
	                                      indxAux++;
	                                      objLines[4] = eElementimp.getElementsByTagName("baseImponible").item(0).getTextContent();
	                                      indxAux++;
	                                      objLines[5] = eElementimp.getElementsByTagName("valor").item(0).getTextContent();
	                                      System.out.println("StringImpuestos :" + String.valueOf(objLines[5]));
	                                      */
                                  	  int indximp=2;
	                                      for (int imp=indxAux;imp<objEtiquetasXML.length;imp++) {
	                                    	strTag= objEtiquetasXML[imp].toString();
	                                  		
	                                    	System.out.println("tag :"+ imp + " - " + strTag+": " + eElementimp.getElementsByTagName(strTag).item(0).getTextContent());
	                                		
	                                  		objLines[indximp] = eElementimp.getElementsByTagName(strTag).item(0).getTextContent();
	                                		indximp++;


	                                      }

                                    }
                               }
                      	}
                  	}
                  	
                  	//System.out.println("tag 2:" + eElementdet.getElementsByTagName(strTag).item(0).getNodeName());
                  	
                  
                  	
                  	if (indxAux>2) {
                  		System.out.println("Reiniciando valores");
                  		indxAux=0;
                  		listaLineas.add(objLines);
                  		objLines=new Object[5];

                  		
                  	}

                  }
              }
              
          }
      }
      Object[] obj=null;
      
      if (listaLineas.size()>0) {
      	obj = new Object[listaLineas.size()];
      	int idx=0;
      	for (Object newObj: listaLineas) {
      		obj[idx]= newObj;

      		idx++;
      	}
      }
      
      /*System.out.println("**** Valores ******");

		for (int printObj=0; printObj<obj.length;printObj++) {
		      System.out.println("**** Linea :" + printObj);

			Object[] strValores = (Object[]) obj[printObj]; 
			for (int i = 0; i<strValores.length;i++) {
				System.out.println(" Valor :" + i + " -> " + strValores[i].toString());
			}
			
		}*/
  
	return obj;
	
}

public Object[] ValidateDataDefault() {
	
	Object[] ObjMsg= new Object[2];
	Object[] ObjLinesMsg= null;
	List<String> lstMsg = new ArrayList<String>();
	
	String strMsg="";
	//Validar Datos por Defecto
	int idxObj=0;
	OBCriteria<ImdlvPartner> partner = OBDal.getInstance().createCriteria(ImdlvPartner.class);
	partner.add(Restrictions.eq(ImdlvPartner.PROPERTY_ACTIVE, true));
	if (partner.list().size()==0) {
		strMsg = ("Msg: No existen datos por defecto para los terceros, ");
		idxObj++;
		
	}
	

	OBCriteria<ImdlvPurchaseIinvoice> purchaseInvoice = OBDal.getInstance().createCriteria(ImdlvPurchaseIinvoice.class);
	purchaseInvoice.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_ACTIVE, true));
	
	if (purchaseInvoice.list().size()==0) {
		strMsg = strMsg+ ("Msg: No existen datos por defecto - creación Factura de Compra, ");
		idxObj++;
	}
	if (idxObj==0) {
		ObjMsg[0] = "Continue";
		ObjMsg[1] = "Continue";
		
	}else {
		ObjMsg[0] = "Error";
		ObjMsg[1] = strMsg;
		
	}

	return ObjMsg;
}

public BusinessPartner ValidateBPartner(String strTaxid) {
	
	BusinessPartner Partner=null;
	String StrPartnerID="";
	
	OBCriteria<BusinessPartner> obcPartner = OBDal.getInstance().createCriteria(BusinessPartner.class);
	obcPartner.add(Restrictions.eq(BusinessPartner.PROPERTY_TAXID,strTaxid ));
	
	if (obcPartner.list().size()==0) {
		Partner=null;
		return null; 

	}else {
		StrPartnerID= obcPartner.list().get(0).getId();
	}
	
	Partner = OBDal.getInstance().get(BusinessPartner.class, StrPartnerID);

	
	return Partner; 
	
}



public Object[]  savePartnerDB(ImdlvVoucherDataLine colPurchLine,String strID,boolean blerror)  {
    Object[] strMsg = new Object[2] ;

    	    
    try {
      
        
       
        String strName = colPurchLine.getBpartner().length()>60?colPurchLine.getBpartner().substring(0,58):colPurchLine.getBpartner();
        String strDescription = colPurchLine.getBpartner();
		String strUserId= OBContext.getOBContext().getUser().getId();
		String strOrgId= OBContext.getOBContext().getCurrentOrganization().getId();
		String strClienId= OBContext.getOBContext().getCurrentClient().getId();
        String StrGroupID= "";

        String strMail = "" ;
        
        try {
        	strMail = colPurchLine.getEmails();
        }catch(Exception esqmail) {
        	
        }
        
        
        String StrTaxid = "";
        
        try {
        	StrTaxid = colPurchLine.getTaxid().replace("null", "");
        }catch(Exception exced) {
        	
        }
        
        String strTaxidType= "";
        int counTaxid= StrTaxid.length();
        if (counTaxid==0) {
        	
        	String StrMsgError= ("Msg: El tercero no tiene cedula , factura " + colPurchLine.getInvoiceno().toString()) + ". ";
		     
		     String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLine.getId()  +"'";
			 Object[] objInvoice2 = executeInserts(strSQL2);
			 
			 strMsg[0]= "ERROR";
		     strMsg[1]= StrMsgError;
		     countErrors++;
			 return strMsg;
        }
        if (counTaxid>0 && counTaxid==10) {
        	strTaxidType="D";
        	
        }
        if (counTaxid>0 && counTaxid>10 && counTaxid<=13) {
        	strTaxidType="R";
        	
        }
        if (counTaxid>0 && counTaxid>13) {
        	strTaxidType="P";
        	
        }
        
        if (counTaxid>0 && strTaxidType.length()==0) {
        	
        	strTaxidType="P";
        	
        	String newMsgbx=" Msg: Tercero creado como pasaporte. ";
			String strSQL3= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || ' Msg: "+ newMsgbx +"' where imdlv_voucherpurchline_id ='"+ colPurchLine.getId()  +"'";
			Object[] objInvoice3 = executeInserts(strSQL3);
			
			// strMsg[0]= "ERRCED";
		    // strMsg[1]= newMsgbx;
		    // countErrors++;
			// return strMsg;
        }
        
        if (blerror) {
        	strTaxidType="P";
        }
        
        String strTaxPayer=""; //Tipo de Contribuyente;
        
        OBCriteria<ImdlvPartner> obcDefaultTaxPayer = OBDal.getInstance().createCriteria(ImdlvPartner.class);
        obcDefaultTaxPayer.add(Restrictions.eq(ImdlvPartner.PROPERTY_ACTIVE, true));
        
        String strProductRENT="ND";
       
        
        int inttaxpayer =0;
        try {
        	inttaxpayer= obcDefaultTaxPayer.list().size();
        }catch(Exception et) {
        	
        }
        if (inttaxpayer>0 && inttaxpayer==1) {
        	strTaxPayer = obcDefaultTaxPayer.list().get(0).getSswhTaxpayer().getId();
        }else
        {
        	strTaxPayer="";
        }
        if (strTaxPayer.equals("")) {
        	
        	 
		     String StrMsgError= ("Msg: El tipo de contribuyente se creo por default. ") ;
		     
		     String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLine.getId()  +"'";
			 Object[] objInvoice2 = executeInserts(strSQL2);
		     //return strMsg; 
			 
			 /*
			 OBCriteria<ImdlvPurchaseIinvoice> obcPurchInvoice = OBDal.getInstance().createCriteria(ImdlvPurchaseIinvoice.class);
			 obcPurchInvoice.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_ACTIVE, true));
		        
			 if  (obcPurchInvoice.list().size()>0) {
				 
				 String strTaxPayerID ="ND";
				 try {
					 strTaxPayerID=obcPurchInvoice.list().get(0).gets;
				 }catch(Exception etp) {
					 
				 }
				 
				 if (strTaxPayerID.equals("ND")) {
					 strMsg[0]= "ERROR";
				      strMsg[1]= "Msg: El tipo de contribuyente se creo por default. ";
				      countErrors++;
					 return strMsg;
				 }else {
					 strTaxPayer = strTaxPayerID;
				 }
			 }*/
        }
        
        
        String strCurrency="100";//colPurchLine.getCurrencyxls();
        String strLanguage="";
        String strPriceList="";
        String strPaymentMethod=""; // Métodos de pagos
        String strPaymentTerm=""; // Condiciones de pagos
        
        OBCriteria<ImdlvPartner> obcDefaultPartner = OBDal.getInstance().createCriteria(ImdlvPartner.class);
		obcDefaultPartner.add(Restrictions.eq(ImdlvPartner.PROPERTY_ACTIVE, true));
		
		if (obcDefaultPartner.list().size()>0) {
			
			StrGroupID= obcDefaultPartner.list().get(0).getBpGroup().getId();
			strPaymentTerm = obcDefaultPartner.list().get(0).getPaymentterm().getId();
			strPriceList =  obcDefaultPartner.list().get(0).getPricelist().getId();
			strPaymentMethod =  obcDefaultPartner.list().get(0).getFINPaymentmethod().getId();
			strLanguage=  obcDefaultPartner.list().get(0).getCurrency().getId();
		}
		
		/*String strPaymentMethodxls = colPurchLine.getPaymentmethodxls();
		OBCriteria<IJSUDPaymentMethod> paymentMethod = OBDal.getInstance().createCriteria(IJSUDPaymentMethod.class);
		paymentMethod.add(Restrictions.eq(IJSUDPaymentMethod.PROPERTY_ACTIVE, true));
		paymentMethod.add(Restrictions.eq(IJSUDPaymentMethod.PROPERTY_PAYMENTMETHODCFIS,strPaymentMethodxls));

		if (paymentMethod.list().size()>0) {
			strPaymentMethod = paymentMethod.list().get(0).getPaymentMethod().getId();

		}*/
		
		
        String strEmail =colPurchLine.getEmails();

		String strSql="Insert into c_bpartner(" + 
        		"c_bpartner_id,		ad_client_id,		ad_org_id,				isactive,	created,	createdby,	updated,	updatedby," + 
        		"value,				name,name2," + 
        		"issummary,			c_bp_group_id,		isonetime,				isprospect," + 
        		"isvendor,			iscustomer,			isemployee,				issalesrep," + 
        		"showpriceinorder,	customer_blocking,	vendor_blocking,		so_payment_blocking," + 
        		"po_payment_blocking,so_invoice_blocking,po_invoice_blocking,	so_order_blocking," + 
        		"po_order_blocking,	so_goods_blocking,	po_goods_blocking,		iscashvat," + 
        		"update_currency,	em_sfpr_isfpr,		em_sspr_create_payroll,	em_sspr_isdisabled," + 
        		"em_sspr_compers_id, em_sspr_thirteenth,	em_sspr_fourteenth,		em_sspr_isexecutive," + 
        		"em_sspr_galapagosbenf,em_sspr_workwci,	em_sspr_work_spouse,	em_sspr_netsalarysys," + 
        		"em_eei_eeioice,		em_eei_detail_ei,	em_eei_eeioicev,	ad_language," +
        		"PO_PaymentTerm_ID,	PO_PriceList_ID,		PO_Paymentmethod_ID,	 em_sshr_email,"+
        		"em_sspr_documentno,taxid,EM_Sswh_Taxidtype,EM_SSWH_Taxpayer_ID,em_eei_email,description)"+
        		"values('"+ strID +"','"+ strClienId +"','"+ strOrgId +"','Y',now(),'"+ strUserId+ "',now(),'"+ strUserId +"',"
        		+"'"+StrTaxid+"','"+strName+"','"+ strName + "',"
        		+"'N','"+StrGroupID +"','N','N',"
        		+"'Y','N','N','N',"
        		+"'Y','N','N','N',"
        		+"'N','Y','Y','Y',"
        		+"'N','N','N','N',"
        		+"  0,'N','N','N',"
        		//+"'N','N','N','N'," ERROR:  la sintaxis de entrada no es válida para tipo numeric: «N»
        		+" 0,'N','N','N',"
        		+"'N','N','N','N',"
        		+"'Y','N','Y','es_ES',"
        		+"'"+ strPaymentTerm+"','"+ strPriceList+"','"+strPaymentMethod+"',split_part('"+ strEmail+"','|',1),"
        		+"'"+StrTaxid+"','"+ StrTaxid+"','"+strTaxidType+"','"+strTaxPayer+"','" +strMail + "'"
        		+ ",'" + strDescription + "')";
	 System.out.println(strSql);
     // psInsert.setString(1,  null);	     
      strMsg =  executeInserts(strSql);
      String strMsg1= strMsg[0].toString();
      if (strMsg1.equals("ERROR")) {
    	  countErrors++;
    	  // @ERROR=@SSWH_NifMustBeLengthNumeric@ SSWH_CifDigitLocation
    	  strMsg1= strMsg[1].toString();
    	  String strNewMsg = strMsg1.replace("ERROR", "").replace("@", "").replace("=", "");
    	  if (strNewMsg.equals("SSWH_NifMustBeLengthNumeric") || strNewMsg.equals("SSWH_DigitVerfied")) {
    		  strNewMsg="Msg: Error al validar la cedula. ";
    		  strMsg[0]= "ERRCED";
		      strMsg[1]= strNewMsg;
    	  }else {
    		  strNewMsg = strMsg1.replace("ERROR", "").replace("@", "").replace("=", "");
    		  strMsg[1]= strNewMsg;
    		  if (strNewMsg.equals("SSWH_CifDigitLocation")) {
    			  strNewMsg="Msg: El código de Localidad de la Cédula es incorrecta. ";
	    		  strMsg[0]= "ERRCED";
			      strMsg[1]= strNewMsg;
    			  
    		  }
    	  }
      }else {
    	  strMsg[0]= "OK";
    	  if (blerror) {
    		  strMsg[1]= "Msg: Identificación guardado como pasaporte. ";
    	  }else {
		      strMsg[1]= "Sucess";
    	  }
      }
     
  
    } catch (Exception e) {
     
    }
    
    return strMsg;
    
  }

  public static PreparedStatement getPreparedStatement(Connection conn, String SQLPreparedStatement)
	      throws SQLException {
	    if (conn == null || SQLPreparedStatement == null || SQLPreparedStatement.equals(""))
	      return null;
	    PreparedStatement ps = null;

	    try {

	      ps = conn.prepareStatement(SQLPreparedStatement, ResultSet.TYPE_SCROLL_INSENSITIVE,
	          ResultSet.CONCUR_READ_ONLY);
	 
	    } catch (SQLException e) {
		      if (conn != null) {
	        try {
	          conn.setAutoCommit(true);
	          conn.close();
	        } catch (Exception ex) {
	          ex.printStackTrace();
	        }
	      }
	    }
	    return (ps);
	  }
  
  private static void releasePreparedStatement(PreparedStatement ps) {
	    if (ps != null) {
	      try {
	        ps.close();
	      } catch (Exception e) {
	      }
	    }
	  }
  
  
  public  Object[] InsertLocation(String strOrg,
	      String StrAddress,String strID) {
	    String strSql = "";
	    
	    String strAddress1 = "";
	    String strAddress2 = "";
	    Object[] strMsg=new Object[2];
	    
	    int countAddress = StrAddress.length();
	    
	    if (countAddress>0 && countAddress<=60) {
	    	strAddress1 = StrAddress.substring(0,countAddress);
	    }
	    if (countAddress > 60) {
	    	strAddress2 = StrAddress.substring(60,countAddress);
	    }
	    
	    String strCountryID="";
	    
	    OBCriteria<ImdlvPartner> obcDefaultPartner = OBDal.getInstance().createCriteria(ImdlvPartner.class);
	 			obcDefaultPartner.add(Restrictions.eq(ImdlvPartner.PROPERTY_ACTIVE, true));
	 			
			if (obcDefaultPartner.list().size()>0) {
				
				strCountryID= obcDefaultPartner.list().get(0).getCountry().getId();

			}
	    
	    strSql = strSql
	        + "insert into c_location(c_location_id, ad_client_id, ad_org_id, isactive, createdby, created, updatedby, updated,"
	    	+	"address1, address2,c_country_id"
	    	+ " ) values("
	        + "'"+ strID + "','" + OBContext.getOBContext().getCurrentClient().getId() + "','" + strOrg
	        + "','Y','" + OBContext.getOBContext().getUser().getId() + "',now(),'"
	        + OBContext.getOBContext().getUser().getId() + "',now(),"  
	        + "'"+ strAddress1+"','"+ strAddress2+"','"+strCountryID+"')";

	    System.out.println(strSql);
	    
	    strMsg =  executeInserts(strSql);
	    return strMsg;
	  }
  
  public  Object[] InsertPartnerLocation(  String strOrg,
	      String StrAddress,String strPhone,String strID,String strBpartnerID, String strLocationID) {
	    String strSql = "";
	    
	    String strAddress1 = "";
	    String strAddress2 = "";
	    Object[] strMsg=new Object[2];
	    
	    int countAddress = StrAddress.length();
	    
	    if (countAddress > 60) {
	    	strAddress1 = StrAddress.substring(0,59);
	    }else {
	    	strAddress1 = StrAddress.substring(0,countAddress);
	    }
	    
	    strSql = strSql
	        + "insert into c_bpartner_location(c_bpartner_location_id, ad_client_id, ad_org_id, isactive, createdby, created, updatedby, updated,"
	    	+	"name, isbillto,phone, c_bpartner_id, c_location_id"
	    	+ " ) values("
	        + "get_uuid(),'" + OBContext.getOBContext().getCurrentClient().getId() + "','" + strOrg
	        + "','Y','" + OBContext.getOBContext().getUser().getId() + "',now(),'"
	        + OBContext.getOBContext().getUser().getId() + "',now(),"  
	        + "'"+ strAddress1+"','Y','"+ strPhone+"','"+strBpartnerID +"','"+ strLocationID +"')";

	    System.out.println(strSql);
	    strMsg =  executeInserts(strSql);
	    
	    return strMsg;
	  }
  
 
  public Object[] InsertInvoice(ConnectionProvider connectionProvider,String strSQL)  {
	    
	  	//OBContext.getOBContext().getCurrentOrganization().getId()
	  	Object[] objMsg = new  Object[2];
	    ResultSet result;
	    int strReturn = 0;
	    PreparedStatement st = null;

	    try {
	      try {
			st = connectionProvider.getPreparedStatement(strSQL);
			  result = st.executeQuery();
		      //if (result.next()) {
		      //  strReturn = Integer.parseInt(UtilSql.getValue(result, "countpay"));
		      //}
		      result.close();
		      st.close();
		      objMsg[0] = "OK";
			  objMsg[1] = "Success";
		} catch (Exception e) {
			// TODO Auto-generated catch block
			objMsg[0] = "Error";
			objMsg[1] = e.getMessage().toString();
		}

	    
	    }  finally {
	      try {
	        connectionProvider.releasePreparedStatement(st);
	      } catch (Exception ignore) {
	        ignore.printStackTrace();
	      }
	    }
	    return objMsg;
	  }
  
  public static String getUUID(ConnectionProvider connectionProvider) {
	    String strSql = "";
	    strSql = strSql + "       SELECT get_uuid() as name" + "       FROM dual";

	    ResultSet result;
	    String strReturn = null;
	    PreparedStatement st = null;

	    try {
	      try {
			st = connectionProvider.getPreparedStatement(strSql);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	      result = st.executeQuery();
	      if (result.next()) {
	        strReturn = UtilSql.getValue(result, "name");
	      }
	      result.close();
	      st.close();
	    } catch (SQLException e) {
	      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
	      
	    }
	    return (strReturn);
	  }
  public Object[] executeInserts(String strSQL) {
	  
	  Object[] objMsg= new Object[2];
	  String returnString="OK";
	  try {
			// Se llama a la función para insertar en las líneas de detalle
			org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
	        CallableStatement InsertLineStatement = cp.getConnection()
	              .prepareCall("{call IMDLV_INSERTS(?)}"); // ORIGINAL
	        InsertLineStatement.setString(1,strSQL);
	        InsertLineStatement.execute(); 
	        ResultSet rs = InsertLineStatement.getResultSet();
	        while (rs.next()) {
	        	returnString=(rs.getString(1));
	        }
	        if (returnString.contains("ERROR")) {
	        	objMsg[0]="ERROR";
	        	objMsg[1]=returnString;
	        	 
	        }
	        if(returnString.contains("OK")) {
	        	objMsg[0]="OK";
	        	objMsg[1]="Sucess";
	        }
	        InsertLineStatement.close();
	        cp.destroy();
	        rs.close();
	} catch (Exception e) {
		objMsg[0]="ERROR";
    	objMsg[1]=returnString;
	}
	  return objMsg;
  }

  public Object[] getDataHeaderXML(String strXML,String strheader) {
		List<Object> listaLineas = new ArrayList<>();
		
		int indx=0;
		  int indxAux=0;
		  Document document = parseXmlFile(strXML);
		  document.getDocumentElement().normalize();
		  document.getDocumentElement().normalize();
	   

			String strEtiquetas ="baseImponible,valor,valor";
			Object[] objEtiquetasXML= strEtiquetas.split(",");
	  
			  document.getDocumentElement().normalize();
			  document.getDocumentElement().normalize();
		      System.out.println("Root Element :" + document.getDocumentElement().getNodeName());
		      NodeList nList = document.getElementsByTagName(strheader);
		      System.out.println("----------------------------");
		      
		      Object[] objLines = new Object[5]; 
		      objLines[0]="0";
		      objLines[1]="0";
		      objLines[2]="0";
		      objLines[3]="0";
		      objLines[4]="0";
		      
		      
		      for (int intmain = 0; intmain < nList.getLength(); intmain++) {
		          Node nNodemain = nList.item(intmain);
		          System.out.println("\nCurrent Element :" + nNodemain.getNodeName());
		          if (nNodemain.getNodeType() == Node.ELEMENT_NODE) {
		              Element eElementmain = (Element) nNodemain;
		             // System.out.println("detalle : " + intmain + eElementmain.getAttribute("detalles"));
		              
		              String StringDetalle = nodeToString(nNodemain);
		             // System.out.println(StringDetalle);
		              Document documentDetalle = parseXmlFile(StringDetalle);
		              NodeList nListDetalles = documentDetalle.getElementsByTagName("totalConImpuestos");
		             
		              indxAux=0;
		              int contar=  nListDetalles.getLength();
		              for (int intDetalles = 0; intDetalles < nListDetalles.getLength(); intDetalles++) {
		            	  Node nNodeDet = nList.item(intDetalles);
		            	  //System.out.println("\nCurrent eElementdet :" + nNodedetalles.getNodeName());
		            	  String StringImpuestos = nodeToString(nNodeDet);
                        Document documentDet = parseXmlFile(StringDetalle);
                        NodeList nListDet = documentDet.getElementsByTagName("totalImpuesto");
                        //Object[] objLines = new Object[5]; 
                        indxAux=nListDet.getLength();
                        
                        for (int intTotImp = 0; intTotImp < nListDet.getLength(); intTotImp++) {
                            Node nNodedetalles = nListDet.item(intTotImp);
                           // System.out.println("\nCurrent eElementdet :" + nNodedetalles.getNodeName());
                            System.out.println("Iteraciones: "+ intTotImp);

		                          if (nNodedetalles.getNodeType() == Node.ELEMENT_NODE) {
		                        	  Element eElementimp = (Element) nNodedetalles;
		
		                        	  int indximp=2;
		                                for (int imp=0;imp<objEtiquetasXML.length;imp++) {
		                              	String strTag= objEtiquetasXML[imp].toString();
		                            		
		                              	System.out.println("tag :"+ intDetalles + " - " + strTag+": " + eElementimp.getElementsByTagName(strTag).item(0).getTextContent());
		                          		
		                              	double btl1= Double.valueOf(eElementimp.getElementsByTagName(strTag).item(0).getTextContent());
		                              	System.out.println(objLines[imp].toString());
		                              	double btl2= Double.valueOf(objLines[imp].toString());
		                              	
		                              	double dblTotal= btl1 + btl2;
		                              	
		                              	//System.out.println("tag valores:"+ imp + " -> " + String.valueOf(dblTotal));
		                              			
		                            	objLines[imp] = String.valueOf(dblTotal);
		                          		indximp++;
		
		
		                                }
		
		                          }
                         }
                            
                   
                        
		              }
		              
		          }
		      }
		      Object[] obj=null;
		      obj=objLines;
		      
		      /*
		      System.out.println("**** Valores ******");
		      	
				for (int printObj=0; printObj<obj.length;printObj++) {
						System.out.println(" Valor :" + printObj + " -> " + obj[printObj].toString());
					
					
				}*/
		  
			return obj;
			
		
		
	}
  public Object[] getDataHeaderRetencionesXML(String strXML,String strheader) {
		List<Object> listaLineas = new ArrayList<>();
		
		int indx=0;
		  int indxAux=0;
		  Document document = parseXmlFile(strXML);
		  document.getDocumentElement().normalize();
		  document.getDocumentElement().normalize();
	   

			String strEtiquetas ="baseImponible,valorRetenido,valorRetenido";
			Object[] objEtiquetasXML= strEtiquetas.split(",");
	  
			  document.getDocumentElement().normalize();
			  document.getDocumentElement().normalize();
		      System.out.println("Root Element :" + document.getDocumentElement().getNodeName());
		      NodeList nList = document.getElementsByTagName(strheader);
		      System.out.println("----------------------------");
		      
		      Object[] objLines = new Object[5]; 
		      objLines[0]="0";
		      objLines[1]="0";
		      objLines[2]="0";
		      objLines[3]="0";
		      objLines[4]="0";
		      
		      
		      for (int intmain = 0; intmain < nList.getLength(); intmain++) {
		          Node nNodemain = nList.item(intmain);
		          System.out.println("\nCurrent Element :" + nNodemain.getNodeName());
		          if (nNodemain.getNodeType() == Node.ELEMENT_NODE) {
		              Element eElementmain = (Element) nNodemain;
		             // System.out.println("detalle : " + intmain + eElementmain.getAttribute("detalles"));
		              
		              String StringDetalle = nodeToString(nNodemain);
		             // System.out.println(StringDetalle);
		              Document documentDetalle = parseXmlFile(StringDetalle);
		              NodeList nListDetalles = documentDetalle.getElementsByTagName("impuestos");
		             
		              indxAux=0;
		              int contar=  nListDetalles.getLength();
		              for (int intDetalles = 0; intDetalles < nListDetalles.getLength(); intDetalles++) {
		            	  Node nNodeDet = nList.item(intDetalles);
		            	  //System.out.println("\nCurrent eElementdet :" + nNodedetalles.getNodeName());
		            	  String StringImpuestos = nodeToString(nNodeDet);
                      Document documentDet = parseXmlFile(StringDetalle);
                      NodeList nListDet = documentDet.getElementsByTagName("impuesto");
                      //Object[] objLines = new Object[5]; 
                      indxAux=nListDet.getLength();
                      
                      for (int intTotImp = 0; intTotImp < nListDet.getLength(); intTotImp++) {
                          Node nNodedetalles = nListDet.item(intTotImp);
                         // System.out.println("\nCurrent eElementdet :" + nNodedetalles.getNodeName());
                          System.out.println("Iteraciones: "+ intTotImp);

		                          if (nNodedetalles.getNodeType() == Node.ELEMENT_NODE) {
		                        	  Element eElementimp = (Element) nNodedetalles;
		
		                        	  int indximp=2;
		                                for (int imp=0;imp<objEtiquetasXML.length;imp++) {
		                              	String strTag= objEtiquetasXML[imp].toString();
		                            		
		                              	System.out.println("tag :"+ intDetalles + " - " + strTag+": " + eElementimp.getElementsByTagName(strTag).item(0).getTextContent());
		                          		
		                              	double btl1= Double.valueOf(eElementimp.getElementsByTagName(strTag).item(0).getTextContent());
		                              	System.out.println(objLines[imp].toString());
		                              	double btl2= Double.valueOf(objLines[imp].toString());
		                              	
		                              	double dblTotal= btl1 + btl2;
		                              	
		                              	//System.out.println("tag valores:"+ imp + " -> " + String.valueOf(dblTotal));
		                              			
		                            	objLines[imp] = String.valueOf(dblTotal);
		                          		indximp++;
		
		
		                                }
		
		                          }
                       }
                          
                 
                      
		              }
		              
		          }
		      }
		      Object[] obj=null;
		      
		      obj=objLines;
		      /*
		      System.out.println("**** Valores ******");
		      	
				for (int printObj=0; printObj<obj.length;printObj++) {
						System.out.println(" Valor :" + printObj + " -> " + obj[printObj].toString());
					
					
				}*/
		  
			return obj;
			
		
		
	}

}
