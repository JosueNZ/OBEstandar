package ec.cusoft.facturaec.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import org.hibernate.criterion.Restrictions;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.model.ad.utility.FileType;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.TabAttachmentsData;
import org.openbravo.erpCommon.utility.JRFormatFactory;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.invoice.Invoice;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import ec.cusoft.facturaec.EEIParamFacturae;
import antlr.collections.impl.Vector;


public class FileUtils {
	public static byte[] readFile(String rutaArchivoFirmado)
    {
        //String RutaArchivoFirmado = ObtenerKeyConfiguracion("RutaArchivoFirmado");    
        java.io.File archivo = new java.io.File(rutaArchivoFirmado);
        byte[] archivoBytes = null;

        long tamanoArch = archivo.length(); //Nos quedamos con el tamaño del archivo.

        //Con esta condicional controlamos si el archivo es demasiado grande
        if (tamanoArch > Integer.MAX_VALUE) {
            //log.debug("El archivo es demasiado largo.");
        } 
        else 
        {

            archivoBytes = new byte[(int) tamanoArch]; //Le damos al array el tamaño del archivo.

            try {
                //Nos creamos esta variable para poder leer el archivo.
                FileInputStream docu = new FileInputStream(archivo);

                //Leemos los bytes del archivo y a la vez se van insertando en el array de bytes creado.
                int numBytes = docu.read(archivoBytes);
                System.out.println("El archivo tiene " + numBytes + " bytes.");
                docu.close();
                } 
                catch (FileNotFoundException e) {
                    System.out.println("No se ha encontrado el archivo. " +  e.getMessage());
                } catch (IOException e) {
                    System.out.println("No se ha podido leer el archivo. " +  e.getMessage());
                }
            
        }
        return archivoBytes;
    }
	
	public static File salvarNoFirmado(Document docomprobante, Invoice invoice, String rootDirectory, EEIParamFacturae param) throws TransformerException{
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(docomprobante);        
        //SALVADO LOCAL
        File file = new File(rootDirectory, "comprobante_"+invoice.getDocumentNo()+"_nofirmado.xml");
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
        File folder = new File(param.getDIRFiles());
        if(!folder.exists())
        	folder.mkdirs();
        file = new File(param.getDIRFiles(), "comprobante_"+invoice.getDocumentNo()+"_nofirmado.xml");
        result = new StreamResult(file);
        transformer.transform(source, result);
       
        
        return file;
	}
	
	public static File salvarNoFirmado(Document docomprobante, Invoice invoice, String rootDirectory, EEIParamFacturae param, String docName) throws TransformerException{
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(docomprobante);        
        //SALVADO LOCAL
        File file = new File(rootDirectory, docName + "_" + invoice.getDocumentNo() + "_nofirmado.xml");
        StreamResult result = new StreamResult(file);
        transformer.transform(source, result);
        File folder = new File(param.getDIRFiles());
        if(!folder.exists())
        	folder.mkdirs();
        file = new File(param.getDIRFiles(), docName + "_" + invoice.getDocumentNo() + "_nofirmado.xml");
        result = new StreamResult(file);
        transformer.transform(source, result);
       
        
        return file;
	}
	
	public static void saveNodeIntoComp(org.w3c.dom.Node nodoXML, String fileName) throws TransformerConfigurationException, TransformerException
    {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(nodoXML);

        StreamResult result =  new StreamResult(new File(fileName));
        transformer.transform(source, result);
    
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
	
	public static void attachFacturaeAtPageCFDI(String ftp_dir, String key, String ruta, String strNombre){
		OBContext.setAdminMode();
		try{
		  
			String tableId=null;
			
			final OBCriteria<Table> critT=OBDal.getInstance().createCriteria(Table.class);
    		critT.add(Restrictions.eq("dBTableName", "C_Invoice"));
    		Table tableInv = critT.list().get(0);
    		tableId = tableInv.getId();
    		
    		final OBCriteria<FileType> critDT=OBDal.getInstance().createCriteria(FileType.class);
    		critDT.add(Restrictions.eq("name", "Facturae XML"));
    		FileType dataType = critDT.list().get(0);
    		
    		final OBCriteria<Attachment> critAtt=OBDal.getInstance().createCriteria(Attachment.class);
    		critAtt.add(Restrictions.eq("name", strNombre+".xml"));
    		critAtt.add(Restrictions.eq("record", key));
    		int iCoincidences = critAtt.list().size();
    		
			
			//lectura de los fichero locales
			  try {
			RandomAccessFile XmlFile = new RandomAccessFile (ruta+".xml", "r" );	
			java.util.Vector<String> lines =new java.util.Vector<String>();
			String sFirstLine="";
			while( (sFirstLine=XmlFile.readLine())!=null){
			  	lines.add(new String(sFirstLine.getBytes("UTF-8")));
			}
			
		     if(iCoincidences==0){
		    	  final Attachment attXML = OBProvider.getInstance().get(Attachment.class); 
		    	  attXML.setDataType(dataType.getId());
		    	  attXML.setName(strNombre+".xml");
		    	  attXML.setRecord(key);
		    	  
		    	  final OBCriteria<Attachment> critRF=OBDal.getInstance().createCriteria(Attachment.class);
		    	  critRF.add(Restrictions.eq("record", key));
		    	  int iRF = critRF.list().size();
		    	  attXML.setSequenceNumber(Long.parseLong(String.valueOf((iRF+1)*10)));
		    	  
		    	  attXML.setTable(tableInv);
		    	  attXML.setText("Generated by SRI");
		    	  
		    	  OBDal.getInstance().save(attXML);
		    	  OBDal.getInstance().flush();
		    	  
		    	  try {							
					        File uploadedDir = new File(ftp_dir+"/"+tableId+"-"+key);
					        if (!uploadedDir.exists()) 
					        	uploadedDir.mkdirs();
					        File uploadedFile = new File(uploadedDir, strNombre+".xml");			        
					        XmlFile.close();
					        //escritura del fichero remoto
					        if(lines.size()>0){
						        XmlFile=new RandomAccessFile(uploadedFile, "rw" );
							    for (int i = 0; i < lines.size(); i++) {
									if(i==0){
										String utf8=new String((""+lines.get(i)).getBytes("UTF-8"));
										XmlFile.writeBytes(utf8);
									}
									else XmlFile.writeBytes(new String(("\n"+lines.get(i)).getBytes("UTF-8")));
								}				    
							    XmlFile.close();
					        
					        }	
					      } catch (Exception ex) {
					        throw new ServletException(ex);
					      }
					
		      }
		    } catch (Exception e) {
		      
		      e.printStackTrace();
		  }
		    
		}catch(Exception ex){
			
		}finally{
			OBContext.restorePreviousMode();
		}
	  }
	
	
	
	public static File actualizaPDFFacturae(String ftp, String id, String rutaJRXML, String rutaLOCAL, String nombre, HashMap<String, Object> designParameters, Map<Object, Object> exportParameters) throws ServletException{

	    String strAttach = ftp+"/reports";

	    String strLanguage = "en_US";
	    Locale locLocale = new Locale(strLanguage.substring(0,2),strLanguage.substring(3,5));

	    String strBaseDesign = ftp+"/reports";

	    rutaJRXML = org.openbravo.utils.Replace.replace(org.openbravo.utils.Replace.replace(rutaJRXML,"@basedesign@",strBaseDesign),"@attach@",strAttach);
	    
	    try {
	      JasperDesign jasperDesign= JRXmlLoader.load(rutaJRXML);
	      JasperReport jasperReport= JasperCompileManager.compileReport(jasperDesign);
	      if (designParameters == null) designParameters = new HashMap<String, Object>();
	      designParameters.put("IS_IGNORE_PAGINATION",  false );
	      designParameters.put("ATTACH", strAttach);
	      designParameters.put("LANGUAGE", strLanguage);
	      designParameters.put("LOCALE", locLocale);

	      JRFormatFactory jrFormatFactory = new JRFormatFactory();
	      jrFormatFactory.setDatePattern(OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("dateFormat.java"));
	      designParameters.put(JRParameter.REPORT_FORMAT_FACTORY, jrFormatFactory);

	      JasperPrint jasperPrint;
	      Connection con = null;
	      try {
	        con = OBDal.getInstance().getConnection();
	        jasperPrint = JasperFillManager.fillReport(jasperReport, designParameters, con);	        
	      } catch (Exception e){
	        throw new ServletException(e.getMessage());
	      }
	      
	      if (exportParameters == null) exportParameters = new HashMap<Object, Object>();
	      
	          	//generando fichero local de Facturae
		        JasperExportManager.exportReportToPdfFile(jasperPrint,rutaLOCAL+"/"+nombre);
		    	//generando fichero remoto de Facturae
		    	OBContext.setAdminMode();
				try{				  
					String tableId=null;
					
					final OBCriteria<Table> critT=OBDal.getInstance().createCriteria(Table.class);
		    		critT.add(Restrictions.eq("dBTableName", "C_Invoice"));
		    		Table tableInv = critT.list().get(0);
		    		tableId = tableInv.getId();
		    		
		    		final OBCriteria<FileType> critDT=OBDal.getInstance().createCriteria(FileType.class);
		    		critDT.add(Restrictions.eq("name", "Acrobat"));
		    		FileType dataType = critDT.list().get(0);
		    		
		    		final OBCriteria<Attachment> critAtt=OBDal.getInstance().createCriteria(Attachment.class);
		    		critAtt.add(Restrictions.eq("name", nombre));
		    		critAtt.add(Restrictions.eq("record", id));
		    		int iCoincidences = critAtt.list().size();
		    		
		    		if(iCoincidences==0){
		    			final Attachment attXML = OBProvider.getInstance().get(Attachment.class); 
				    	  attXML.setDataType(dataType.getId());
				    	  attXML.setName(nombre);
				    	  attXML.setRecord(id);
				    	  
				    	  final OBCriteria<Attachment> critRF=OBDal.getInstance().createCriteria(Attachment.class);
				    	  critRF.add(Restrictions.eq("record", id));
				    	  int iRF = critRF.list().size();
				    	  attXML.setSequenceNumber(Long.parseLong(String.valueOf((iRF+1)*10)));
				    	  
				    	  attXML.setTable(tableInv);
				    	  attXML.setText("Generated by Openbravo ERP");
				    	  
				    	  OBDal.getInstance().save(attXML);
				    	  OBDal.getInstance().flush();
				    	  
				    	  File uploadedDir = new File(ftp+"/"+tableInv.getId()+"-"+id);
					      if (!uploadedDir.exists()) 
					    		uploadedDir.mkdirs();
					      File uploadedFile = new File(uploadedDir, nombre);
					      JasperExportManager.exportReportToPdfFile(jasperPrint,uploadedFile.getAbsolutePath());
					      
					      return uploadedDir;
		    			
		    		}
				}catch(Exception exc){
					exc.printStackTrace();
				}
	    } catch (JRException e) {	      
	      e.printStackTrace();
	      throw new ServletException(e.getMessage());
	    } catch (Exception e) {
	      throw new ServletException(e.getMessage());
	    } finally {	      
	    }
	    return null;
	  }

  public static File salvarNoFirmado(Document docomprobante, String documentNo,
      String rootDirectory, EEIParamFacturae param, String docName) throws TransformerException {
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    DOMSource source = new DOMSource(docomprobante);
    // SALVADO LOCAL
    File file = new File(rootDirectory, docName + "_" + documentNo + "_nofirmado.xml");
    StreamResult result = new StreamResult(file);
    transformer.transform(source, result);
    File folder = new File(param.getDIRFiles());
    if (!folder.exists())
      folder.mkdirs();
    file = new File(param.getDIRFiles(), docName + "_" + documentNo + "_nofirmado.xml");
    result = new StreamResult(file);
    transformer.transform(source, result);

    return file;
  }
	
	
}
