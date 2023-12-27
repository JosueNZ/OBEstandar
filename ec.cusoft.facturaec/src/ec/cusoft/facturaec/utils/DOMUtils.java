package ec.cusoft.facturaec.utils;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class DOMUtils {
	
	public static String getStringFromDocument(Document doc)
    throws TransformerConfigurationException, TransformerException
{
    DOMSource domSource = new DOMSource(doc);
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    TransformerFactory tf = TransformerFactory.newInstance();
    Transformer transformer = tf.newTransformer();
    transformer.transform(domSource, result);
    return writer.toString();
}
	
	public static void saveComprobante(String comprobante, String rutaSalida)
        throws UnsupportedEncodingException, FileNotFoundException, IOException
    {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(rutaSalida), "UTF8"));
        out.write(comprobante);
        out.close();
    }
	
	public static String readFile(Document doc) throws TransformerException {
	  	  TransformerFactory tf = TransformerFactory.newInstance();
	  	  Transformer transformer = tf.newTransformer();
	  	  transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	  	  StringWriter writer = new StringWriter();
	  	  transformer.transform(new DOMSource(doc), new StreamResult(writer));
	  	  String output = writer.getBuffer().toString().replaceAll("\n|\r", "");
	  	  
	  	  return output;
	  	
	  }
	
	public static String ElementToString(Element e) throws TransformerException{
    	TransformerFactory transFactory = TransformerFactory.newInstance();
    	Transformer transformer = transFactory.newTransformer();
    	StringWriter buffer = new StringWriter();
    	transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    	transformer.transform(new DOMSource(e), new StreamResult(buffer));
    	String str = buffer.toString();
    	return str;
    }
	public static Document loadXML(String path) throws ParserConfigurationException, SAXException, IOException
	{
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    dbf.setNamespaceAware(true);
	    return dbf.newDocumentBuilder().parse(new BufferedInputStream(new FileInputStream(path)));
	}


}
