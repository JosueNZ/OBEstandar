package ec.cusoft.facturaec.utils;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLPrinter {
	private static boolean skipNL;
	
	public static void printXMLInConsole(Node rootNode){
		System.out.println(printXML(rootNode));
	}
	
	private static String printXML(Node rootNode) {
	    String tab = "";
	    skipNL = false;
	    return(printXML(rootNode, tab));
	}
	
	private static String printXML(Node rootNode, String tab) {
	    String print = "";
	    if(rootNode.getNodeType()==Node.ELEMENT_NODE) {
	        print += "\n"+tab+"<"+rootNode.getNodeName();
	        
	        NamedNodeMap map = rootNode.getAttributes();
	        
	        if(map.getNamedItem("id") != null){
	        	String id = map.getNamedItem("id").getTextContent();
	        	
	        	print += " id=\"" + id + "\"";	        	
	        }
	        
	        if(map.getNamedItem("version") != null){
	        	String version = map.getNamedItem("version").getTextContent();
	        	
	        	print += " version=\"" + version + "\"";	        	
	        }
	        
	        		
	        print += ">";
	    }
	    
	    NodeList nl = rootNode.getChildNodes();
	    if(nl.getLength()>0) {
	        for (int i = 0; i < nl.getLength(); i++) {
	            print += printXML(nl.item(i), tab+"  ");    // \t
	        }
	    } else {
	        if(rootNode.getNodeValue()!=null) {
	            print = rootNode.getNodeValue();
	        }
	        skipNL = true;
	    }
	    if(rootNode.getNodeType()==Node.ELEMENT_NODE) {
	        if(!skipNL) {
	            print += "\n"+tab;
	        }
	        skipNL = false;
	        print += "</"+rootNode.getNodeName()+">";
	    }
	    return(print);
	}
}
