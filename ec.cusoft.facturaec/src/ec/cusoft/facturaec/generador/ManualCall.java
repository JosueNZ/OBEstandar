/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ec.cusoft.facturaec.generador;

//import com.sun.org.apache.xml.internal.serialize.OutputFormat;
//import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import ec.gob.sri.comprobantes.ws.aut.Mensaje;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 
 * @author Santiago
 */
public class ManualCall {

	public static Document getAutorizacion(String claveAcceso, String wsURL)
			throws MalformedURLException, IOException,
			TransformerConfigurationException, TransformerException {
		// Code to make a webservice HTTP request
		String responseString = "";
		String outputString = "";

		URL url = new URL(wsURL);
		URLConnection connection = url.openConnection();
		HttpURLConnection httpConn = (HttpURLConnection) connection;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		String xmlInput = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ec=\"http://ec.gob.sri.ws.autorizacion\">\n"
				+ "<soapenv:Header/>\n"
				+ "<soapenv:Body>\n"
				+ "<ec:autorizacionComprobante>\n"
				+ "<!--Optional:-->\n"
				+ "<claveAccesoComprobante>"
				+ claveAcceso
				+ "</claveAccesoComprobante>\n"
				+ "</ec:autorizacionComprobante>\n"
				+ "</soapenv:Body>\n"
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
		//OutputStream out = httpConn.getOutputStream();
		// Write the content of the request to the outputstream of the HTTP
		// Connection.
		//out.write(b);
		//out.close();
		// Ready with sending the request.
		// Read the response.
		InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
		BufferedReader in = new BufferedReader(isr);
		// Write the SOAP message response to a String.
		while ((responseString = in.readLine()) != null) {
			outputString = outputString + responseString;
		}
		// Parse the String output to a org.w3c.dom.Document and be able to
		// reach every node with the org.w3c.dom API.
		Document document = parseXmlFile(outputString);
		NodeList nodeLst = document.getElementsByTagName("autorizaciones");
		String weatherResult = nodeLst.item(0).getTextContent();
		System.out.println("Weather: " + weatherResult);
		// Write the SOAP message formatted to the console.
		String formattedSOAPResponse = formatXML(outputString);
		System.out.println(formattedSOAPResponse);
		return document;
	}

	public static Object[] getAutorizacionArray(String claveAcceso, String wsURL)
			throws MalformedURLException, IOException,
			TransformerConfigurationException, TransformerException {
		// Code to make a webservice HTTP request
		String responseString = "";
		String outputString = "";

		URL url = new URL(wsURL);
		URLConnection connection = url.openConnection();
		HttpURLConnection httpConn = (HttpURLConnection) connection;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		String xmlInput = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ec=\"http://ec.gob.sri.ws.autorizacion\">\n"
				+ "<soapenv:Header/>\n"
				+ "<soapenv:Body>\n"
				+ "<ec:autorizacionComprobante>\n"
				+ "<!--Optional:-->\n"
				+ "<claveAccesoComprobante>"
				+ claveAcceso
				+ "</claveAccesoComprobante>\n"
				+ "</ec:autorizacionComprobante>\n"
				+ "</soapenv:Body>\n"
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
		// Write the content of the request to the outputstream of the HTTP
		// Connection.
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
		// Parse the String output to a org.w3c.dom.Document and be able to
		// reach every node with the org.w3c.dom API.
		Document document = parseXmlFile(outputString);
		NodeList nodeLst = document.getElementsByTagName("autorizaciones");
		String weatherResult = nodeLst.item(0).getTextContent();
		System.out.println("Weather: " + weatherResult);
		// Write the SOAP message formatted to the console.
		String formattedSOAPResponse = formatXML(outputString);
		System.out.println(formattedSOAPResponse);

		Object aut[] = new Object[2];
		aut[0] = document;
		// aut[0] = outputString;
		aut[1] = weatherResult;
		return aut;
	}

	// format the XML in your Stringpublic
	private static String formatXML(String unformattedXml) {
		/*
		 * try { Document document = parseXmlFile(unformattedXml); OutputFormat
		 * format = new OutputFormat(document); format.setIndenting(true);
		 * format.setIndent(3); format.setOmitXMLDeclaration(true);
		 */
		Writer out = new StringWriter();
		/*
		 * XMLSerializer serializer = new XMLSerializer(out, format);
		 * serializer.serialize(document);
		 */
		return out.toString();
		/*
		 * } catch (IOException e) { throw new RuntimeException(e); }
		 */
	}

	private static Document parseXmlFile(String in) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			InputSource is = new InputSource(new StringReader(in));
			return db.parse(is);
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static List<Mensaje> ObtenerListaMensajesRespeusta(Document documento) {
		NodeList listaMensajes = documento
				.getElementsByTagName("identificador");
		List<Mensaje> lista = new ArrayList<Mensaje>();
		for (int item = 0; item < listaMensajes.getLength(); item++) {
			try {
				Mensaje mensaje = new Mensaje();
				mensaje.setIdentificador(listaMensajes.item(item)
						.getTextContent());
				mensaje.setMensaje(listaMensajes.item(item).getNextSibling()
						.getTextContent());
				mensaje.setTipo(listaMensajes.item(item).getNextSibling()
						.getNextSibling().getTextContent());
				lista.add(mensaje);
			} catch (Exception e) {
			}
		}
		return lista;

	}

}
