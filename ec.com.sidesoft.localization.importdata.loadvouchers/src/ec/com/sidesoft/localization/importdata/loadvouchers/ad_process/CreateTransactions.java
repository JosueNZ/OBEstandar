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
import java.util.Locale;
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
import ec.com.sidesoft.localization.ecuador.withholdingssales.SSWSWithholdingSale;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvPartner;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvPurchaseIinvoice;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvVoucherData;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvVoucherDataLine;
import ec.com.sidesoft.localization.importdata.loadvouchers.Imdlv_Lines;

import java.io.FileReader;
//import ec.com.sidesoft.workorder.update.price.UpdatePriceWO;
//import ec.com.sidesoft.workorder.update.price.UpdatePriceLineWO;
import org.openbravo.model.pricing.pricelist.*;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.apache.log4j.Logger;

import org.openbravo.model.pricing.pricelist.*;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.apache.log4j.Logger;

public class CreateTransactions extends DalBaseProcess {
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
		
	    String strClientID = OBContext.getOBContext().getCurrentClient().getId();
	    String strOrgID = OBContext.getOBContext().getCurrentOrganization().getId();
	    String strUserID = OBContext.getOBContext().getUser().getId();
	

			//Creación de terceros
			ImdlvVoucherData LinesImporData = OBDal.getInstance().get(ImdlvVoucherData.class, recordId);

			Object[] objMsgLines = InsertPurchaseLines(LinesImporData);
			
			objMsgLines = InsertWithholdingsLines(LinesImporData);
			
			//objMsgLines = InsertWithholdingsLinesTmp(LinesImporData);
			
			objMsgLines = InsertOrder(LinesImporData, strClientID, strOrgID, strUserID );

			typeM= objMsgLines[0].toString();
			messageT= objMsgLines[1].toString();
			
			
			ConnectionProvider connStatus = new DalConnectionProvider(false);
			
			String strSQL= "update imdlv_voucher_purchase set Docstatus='PR'  where imdlv_voucher_purchase_id ='"+ recordId +"'"
					+ " and 0=(select count(*) " + 
					" from imdlv_voucherpurchline il" + 
					" where il.imdlv_voucher_purchase_id = '"+ recordId + 
					"' and isprocess='N'" + 
					" )";
			Object[] objInvoice = executeInsertsOrder(connStatus,strSQL);
			
			strSQL= "update imdlv_voucher_purchase set Createtrx2='RP' where imdlv_voucher_purchase_id ='"+ recordId +"'"
					+ " and (select count(*) " + 
					" from imdlv_voucherpurchline il" + 
					" where il.imdlv_voucher_purchase_id = '"+ recordId + 
					"' and isprocess='N'" + 
					" )>0";
			objInvoice = executeInsertsOrder(connStatus,strSQL);
			
			connStatus.getConnection().close();
			
			
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
	


public Object[] InsertPurchaseLines(ImdlvVoucherData purchaseHeader) {
	Object[] ObjMsg= new Object[2];
	Object[] ObjLinesMsg= null;
	Object[] ObjLinesByMsg= new Object[2];
	String strUserId= OBContext.getOBContext().getUser().getId();
	String strOrgId= OBContext.getOBContext().getCurrentOrganization().getId();
	String strClienId= OBContext.getOBContext().getCurrentClient().getId();
	int intIdx=0;
	
	String strDocumentXMLProcess = purchaseHeader.getDocumentno();
	
	ConnectionProvider connPartner = new DalConnectionProvider(false);

	OBCriteria<ImdlvVoucherDataLine> obc = OBDal.getInstance().createCriteria(ImdlvVoucherDataLine.class);
	obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_IMDLVVOUCHERPURCHASE, purchaseHeader));
	obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ISPROCESS, false));
	obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ISCREATEINVOICE, true));
	obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ISCREATEORDER, false));
	obc.add(Restrictions.isNotNull(ImdlvVoucherDataLine.PROPERTY_XMLSRI));
	
	OBCriteria<ImdlvPartner> obcDefaultPartner = OBDal.getInstance().createCriteria(ImdlvPartner.class);
	obcDefaultPartner.add(Restrictions.eq(ImdlvPartner.PROPERTY_ACTIVE, true));
	
	
	OBCriteria<ImdlvPurchaseIinvoice> purchaseInvoiceDf = OBDal.getInstance().createCriteria(ImdlvPurchaseIinvoice.class);
	purchaseInvoiceDf.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_ACTIVE, true));
	purchaseInvoiceDf.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_TYPETRXXML,"factura"));
	
	if (purchaseInvoiceDf.list().size()==0) {
		
		String StrMsgError="Msg: No existe un tipo de documento configurado para las facturas.  ";
		//String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
		//Object[] objInvDate = executeInserts(strSQLDate);
		Object objMsg[]=new Object[2];
		objMsg[0]="Error";
    	objMsg[1]=StrMsgError;
    	return objMsg;
    	
		
	}
	List<ImdlvVoucherDataLine> lstPurchLine = obc.list();
	
	if (lstPurchLine.size()>0) {
				ObjLinesMsg= new Object[lstPurchLine.size()];
			
				for (ImdlvVoucherDataLine colPurchLines: obc.list()) {
					
					if (colPurchLines.getDescription().equals("comprobanteRetencion")) {
						continue;
					}
					
		
						String StrInvoiceID ="";
						StrInvoiceID= getUUID(connPartner);
						StrInvoiceID= getUUID(connPartner);
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
						
//purchaseInvoice.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_DOCTYPEPURCHASE, purchaseHeader.getDoctype()));

						
						if (purchaseInvoiceDf.list().size()>0) {
							
						
							
							/*
							 * VALIDAR TERCERO - CONFIGURACIÓN TIPO DE CONTRIBUYENTE
							 * */
							
							String strProduct_Rent="ND";
							String strProduct_VAT="ND";
							
							
	//purchaseInvoiceDf.add(Restrictions.isNotNull(ImdlvPurchaseIinvoice.PROPERTY_PRODUCTDEFAULT));
					        
						
							strProductID = purchaseInvoiceDf.list().get(0).getProductDefault().getId();
							
							strWithhDoctypeId ="'" + purchaseInvoiceDf.list().get(0).getDoctypeWithholding().getId() +"'" ; 
							strDocTypeID=purchaseInvoiceDf.list().get(0).getDoctypePurchase().getId();
							strLiveliHoodId = "'" +purchaseInvoiceDf.list().get(0).getSswhLivelihoodt().getId()+"'";
							strCodeLiveliHoodId = "'" +purchaseInvoiceDf.list().get(0).getSswhCodelivelihoodt().getId()+"'";
							isElectronic=true;
							
							if (purchaseInvoiceDf.list().get(0).getDoctypeWithholding().getSSWHAuthorizationList().size()>0) {
								strAuthorizationWithh = "'" +purchaseInvoiceDf.list().get(0).getDoctypeWithholding().getSSWHAuthorizationList().get(0)
										.getAuthorizationno()+ "'" ;
							}
							
							DocumentType docObj = OBDal.getInstance().get(DocumentType.class, strDocTypeID);
						    
							if (docObj.getDocumentSequence()!=null) {
						     
								Sequence seq = docObj.getDocumentSequence();
						      
								strDocumetnNo = (seq.getPrefix()!=null?seq.getPrefix().toString():"")+  seq.getNextAssignedNumber().toString();
						      
								seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
						      
								OBDal.getInstance().save(seq);
								OBDal.getInstance().flush();
						      
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
						

						if (obcDefaultPartner.list().size()>0) {
							
							strPaymentTermID= obcDefaultPartner.list().get(0).getPaymentterm().getId();
							strPriceListID= obcDefaultPartner.list().get(0).getPricelist().getId();
							strGroupPartner = obcDefaultPartner.list().get(0).getBpGroup().getId();
							strFinPaymentMethodID= obcDefaultPartner.list().get(0).getFINPaymentmethod().getId();
							strCurrency = obcDefaultPartner.list().get(0).getCurrency().getId();
						}
						
						strFinPaymentMethodID = BPartner.getPOPaymentMethod()==null?strFinPaymentMethodID: BPartner.getPOPaymentMethod().getId();
						//strPriceListID = BPartner.getPurchasePricelist()==null?strPriceListID: BPartner.getPurchasePricelist().getId();
						//strPaymentTermID = BPartner.getPOPaymentTerms()==null?strPaymentTermID: BPartner.getPOPaymentTerms().getId();
						
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
											",processing,paymentrule,EM_Sswh_Nroauthorization,EM_Sswh_Expirationdate,EM_Eei_Description"+
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
								
							}else if (strNewMSg.contains("em_sswh_documentno")){
								ObjMsg[1]="Msg: La referencia proveedor para esta factura ya existe. ";
								
							}else {
								ObjMsg[1]=objInvoice[1];
							}
							//updateLinesPl.setLogserror(strLog +" - " + ObjMsg[1].toString() );
							//OBDal.getInstance().save(updateLinesPl);
							
							//String StrMsgError="Msg: No se puede procesar la factura por falta de correo electrónico.  ";
							String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ ObjMsg[1].toString() +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
							Object[] objInvDate = executeInserts(strSQLDate);
				        	
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
							Object[] objInvoiceLines=null;
							//strEtiquetas ="cantidad,precioUnitario,impuestos,impuesto,tarifa,baseImponible,valor";
							int intLine=0;
							
							ImdlvVoucherDataLine SearchLinesPl= colPurchLines;

							OBCriteria<Imdlv_Lines> oblines = OBDal.getInstance().createCriteria(Imdlv_Lines.class);
							oblines.add(Restrictions.eq(Imdlv_Lines.PROPERTY_IMDLVVOUCHERPURCHLINE, SearchLinesPl));

							List<Imdlv_Lines> oblinesList = oblines.list();
							
							if (oblines.list().size()==0) {

								String StrMsgError= "Msg: El comprobante no tiene lineas de productos registrados. ";
								String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
								Object[] objInvDate = executeInserts(strSQLDate);

								strSQL= "delete from c_invoice where c_invoice_id ='"+ StrInvoiceID +"'";
								objInvoice = executeInserts(strSQL);

								//return ObjMsg;
								countErrors++;
								continue;
							
							}
							for (Imdlv_Lines collLines:oblinesList) {
								
								intLine=intLine+10;
						
								dblqty= collLines.getQtyinvoiced().toString();
								dblprice=collLines.getPriceactual().toString();
								
								double dblTotLine = Double.valueOf(dblqty) * Double.valueOf(dblprice);
								String dblLinenetamt= String.valueOf(dblTotLine);
								dblrate="";
								dbltaxamt= collLines.getTaxamt().toString();
								if(dblrate.equals("0") && dblrate.equals("000")) {
									strTaxResult = strTax0;
								}else {
									strTaxResult = strTax12;
								}
								strProductID = collLines.getProduct().getId();
								strTaxResult = collLines.getTax().getId();

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
										",'" + strProductID+"','"+strTaxResult+"',(select c_uom_id from m_product where m_product_id ='"+ strProductID +"'),"+dbltaxamt+""+
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
								strSQL= "update imdlv_voucherpurchline set isprocess='Y', logserror = coalesce(logserror,'') || 'Transacción creada satisfactoriamente' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId() +"'";
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
		
	}else {
		ObjMsg[0]="OK";
		ObjMsg[1]="Se ha completado correctamente el proceso.";
		
	}
	
	if (countErrors>0) {
		ObjMsg[0]="Info";
		if (intIdx==0) {
			intIdx=countErrors;
		}
		ObjMsg[1]= intIdx + " linea(s) con error(es), revise los logs de los registros no procesados para más información";
	}else if (intIdx==0) {
		ObjMsg[0]="OK";
		ObjMsg[1]="Se ha completado correctamente el proceso.";
		
		
	}
	
	try {
		connPartner.getStatement().getConnection().commit();
		connPartner.getStatement().close();
		connPartner.destroy();
	}catch (Exception e) {
		// TODO: handle exception
	}
	return ObjMsg;
	
}

public Object[] InsertWithholdingsLinesTmp(ImdlvVoucherData purchaseHeader) {
	
	
	Object[] ObjMsg2=new Object[2];
	ObjMsg2[0]="OK";
	ObjMsg2[1]="Se ha completado correctamente el proceso.";


return ObjMsg2;
}
public Object[] InsertWithholdingsLines(ImdlvVoucherData purchaseHeader) {
	
	
	Object[] ObjMsg= new Object[2];
	Object[] ObjLinesMsg= null;
	Object[] ObjLinesByMsg= new Object[2];
	String strUserId= OBContext.getOBContext().getUser().getId();
	String strOrgId= OBContext.getOBContext().getCurrentOrganization().getId();
	String strClienId= OBContext.getOBContext().getCurrentClient().getId();
	int intIdx=0;
	ConnectionProvider connPartner = new DalConnectionProvider(false);

	
	OBCriteria<ImdlvVoucherDataLine> obc = OBDal.getInstance().createCriteria(ImdlvVoucherDataLine.class);
	obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_IMDLVVOUCHERPURCHASE, purchaseHeader));
	obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ISPROCESS, false));
	obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_DESCRIPTION, "comprobanteRetencion"));
	obc.add(Restrictions.isNotNull(ImdlvVoucherDataLine.PROPERTY_XMLSRI));

	
	OBCriteria<ImdlvPartner> obcDefaultPartner = OBDal.getInstance().createCriteria(ImdlvPartner.class);
	obcDefaultPartner.add(Restrictions.eq(ImdlvPartner.PROPERTY_ACTIVE, true));
	
	
	OBCriteria<ImdlvPurchaseIinvoice> purchaseInvoiceDf = OBDal.getInstance().createCriteria(ImdlvPurchaseIinvoice.class);
	purchaseInvoiceDf.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_ACTIVE, true));
	purchaseInvoiceDf.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_TYPETRXXML, "comprobanteRetencion"));
	
	if (purchaseInvoiceDf.list().size()==0) {
		
		String StrMsgError="Msg: No existe un tipo de documento configurado para los comprobantes de retención.  ";
		//String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
		//Object[] objInvDate = executeInserts(strSQLDate);
		Object objMsg[]=new Object[2];
		objMsg[0]="Error";
    	objMsg[1]=StrMsgError;
    	return objMsg;
    	
		
	}
	
	List<ImdlvVoucherDataLine> lstPurchLine = obc.list();
	

	String strCurrencyID="USD";
	String StrDateAcctInvoice ="";
	String strCurrency="(select c_currency_id from c_currency where iso_code='"+ strCurrencyID +"')";
	String strPaymentTermID ="";
	String strPriceListID="";
	String strGroupPartner="";
	
	String strFinPaymentMethodID ="";
	String strPaymentMethodXLS = "";
	

	if (obcDefaultPartner.list().size()>0) {
		
		strPaymentTermID= obcDefaultPartner.list().get(0).getPaymentterm().getId();
		strPriceListID= obcDefaultPartner.list().get(0).getPricelist().getId();
		strGroupPartner = obcDefaultPartner.list().get(0).getBpGroup().getId();
		strFinPaymentMethodID= obcDefaultPartner.list().get(0).getFINPaymentmethod().getId();
		strCurrency = obcDefaultPartner.list().get(0).getCurrency().getId();
	}
	
	if (lstPurchLine.size()>0) {
				ObjLinesMsg= new Object[lstPurchLine.size()];
			
				for (ImdlvVoucherDataLine colPurchLines: obc.list()) {
					
					System.out.println("Linea" + colPurchLines.getLine().toString() + " " + colPurchLines.getInvoiceno());
					
					if (!colPurchLines.getDescription().equals("comprobanteRetencion")) {
						continue;
					}
					
					if (!colPurchLines.getXMLSri().contains("numDocSustento")) {
						String StrMsgError="Msg: El comprobante de retención corresponde a un lote de retenciones.";
						String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
						Object[] objInvDate = executeInserts(strSQLDate);
						countErrors++;
						continue;
					}
					
					if (!colPurchLines.getXMLSri().contains("valorRetenido")) {
						String StrMsgError="Msg: El comprobante no tiene el tag <valorRetenido>.";
						String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
						Object[] objInvDate = executeInserts(strSQLDate);
					}
					
					
		
						String StrInvoiceID ="";
						String strWithholdingSalesID= getUUID(connPartner);
						String strTaxid=colPurchLines.getTaxid();

						BusinessPartner BPartner = ValidateBPartner(strTaxid);
						if (BPartner==null) {
							continue;
						}
						String strPartnerLocationID ="";
						String strProductID="";
						String strDocumetnNo ="";
                                                String strSequenceNo ="";
						String strDocTypeID="";
						String strWithhDoctypeId="null";
						String strKeySri = colPurchLines.getAuthorizationno();
						String strValidDocumentNo = colPurchLines.getInvoiceno().replace("-", "");
						String strPaymentID="11111111111111111111111111111111";
						
						
						boolean isElectronic = false;
						double dblWithhAmount = 0; 
						double dblWithhAmount2 = 0; 
						Sequence seq = null;
						if (purchaseInvoiceDf.list().size()>0) {
							
					
							strDocTypeID = purchaseInvoiceDf.list().get(0).getDoctypePurchase().getId() ; 

							DocumentType docObj = OBDal.getInstance().get(DocumentType.class, strDocTypeID);
													
							strDocumetnNo = colPurchLines.getInvoiceno();
						    
						}
						String strInvoiceReference = colPurchLines.getNumdocsustento();
						
						//SearchInvoiceData
						
						String StrInvoiceSearch = get_Invoice(connPartner,strInvoiceReference,"Y",  BPartner.getId());
						
						//OBCriteria<Invoice> obcInv = OBDal.getInstance().createCriteria(Invoice.class);
						//obcInv.add(Restrictions.eq(Invoice.PROPERTY_DOCUMENTNO, strInvoiceReference ));
						
						if ( StrInvoiceSearch.equals("ND") ) {
							
						
							String StrMsgError="Msg: No se puede crear el comprobante, la Factura no existe. Msg: Existe un comprobante registrado con el mismo Nro. de factura";
							String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
							Object[] objInvDate = executeInserts(strSQLDate);
							countErrors++;
				        	continue;
						}else {
							//StrInvoiceID = obcInv.list().get(0).getId();
							StrInvoiceID = StrInvoiceSearch;
						}
						
						
						OBCriteria<SSWSWithholdingSale> obcWS = OBDal.getInstance().createCriteria(SSWSWithholdingSale.class);
						obcWS.add(Restrictions.eq(SSWSWithholdingSale.PROPERTY_NUMAUTO, colPurchLines.getKeyacess() ));
						
						if (obcWS.list().size()>=1) {
							
						
							String StrMsgError="Msg: El comprobante de retención ya existe.  ";
							String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
							Object[] objInvDate = executeInserts(strSQLDate);
							countErrors++;
				        	continue;
						}
							
						String StrDateInvoice ="";
	                    String StrDateAuth ="";
						String StrDueDate="null";
						StrDateInvoice = new SimpleDateFormat("yyyy-MM-dd",   Locale.getDefault()).format(colPurchLines.getDateemision());
						StrDateAuth = new SimpleDateFormat("yyyy-MM-dd",   Locale.getDefault()).format(colPurchLines.getDateauthorization());


						
				
						
						
						
						
						
						String strSQL="INSERT INTO public.ssws_withholdingsale(" + 
								"	ssws_withholdingsale_id, ad_client_id, ad_org_id, isactive, " + 
								"	created, createdby, updated, updatedby, " + 
								"	description, withholdingdate, processing, " + 
								"	processed, posted, dateacct, totalwhrentalamt, " + 
								"	documentno, c_doctype_id, totalwhivaamt, docstatus, " + 
								"	docaction, c_bpartner_id, c_invoice_id, withholdingtype, " + 
								"	getlines, c_currency_id, codigo, numauto, " + 
								"	fechaautotext, paidinvoice, c_glitem_id, " + 
								"	isagreemment, fin_payment_scheduledetail_id)" + 
								"	VALUES ('"+strWithholdingSalesID+"', '" + strClienId+"', '"+strOrgId+"', 'Y', " +
											"now(), '"+strUserId+"', now(), '"+strUserId+"', '" +
											strDocumetnNo+"', '"+StrDateInvoice+"', 'N',  " +
											"'N', 'N','"+StrDateInvoice+"', 0, " +
											"Ad_Sequence_Doctype('"+strDocTypeID +"','','Y'), '"+strDocTypeID+"', 0, 'DR', " +
											"'CO', '"+BPartner.getId()+"', '"+StrInvoiceID+"', 'WS', " +
											"'N','" + BPartner.getCurrency().getId()+ "' , '"+strKeySri+"', '"+ strKeySri+"', " +
											"'"+StrDateAuth+"', 'N', null, "+
											"'N',null"+
											")";
											;	
						//Object[] objInvoice = InsertInvoice(connPartner,strSQL);
						System.out.println("Retención: " + colPurchLines.getInvoiceno() );
						
						Object[] objInvoice = executeInserts(strSQL);
						System.out.println("Retención : " +strSQL);

						
						if (objInvoice[0].toString().contains("ERROR")) {
							intIdx++;
						
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
							String StrMsgError=ObjMsg[1].toString();
							String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
							Object[] objInvDate = executeInserts(strSQLDate);
							ObjMsg[0]="Error";
							
							countErrors++;
							//return ObjMsg;
						}else {

							String strTaxRateID="";
							String strTaxResult="";
							String dblbaseamt = "0";
							String dbltaxamt = "0";
							String strcode  ="";// - ((colPurchLines.getBaseimpxls().doubleValue()<0? -1:1)* colPurchLines.getBaseimpxls().doubleValue());
							String strcodRet="";
							String strrate="";
							String strcodSustento="";
							
							String strAmountRent ="0";
							String strWithRent ="0";
							String strAmountVAT ="0";
							String strWithVAT ="0";
							String strWithType="";
							
							double CountErrorLine=0;
							//String strXml = colPurchLines.getXMLSri();
							Object[] objInvoiceLines=null;
							//strEtiquetas ="cantidad,precioUnitario,impuestos,impuesto,tarifa,baseImponible,valor";
							int intLine=0;
							
							ImdlvVoucherDataLine SearchLinesPl= colPurchLines;

							OBCriteria<Imdlv_Lines> obcImdlvLines = OBDal.getInstance().createCriteria(Imdlv_Lines.class);
							obcImdlvLines.add(Restrictions.eq(Imdlv_Lines.PROPERTY_IMDLVVOUCHERPURCHLINE, SearchLinesPl ));
							
							List<Imdlv_Lines> lstLines = obcImdlvLines.list();
							
							if (obcImdlvLines.list().size()==0) {

								String StrMsgError= "Msg: El comprobante no tiene lineas de productos registrados. ";
								String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
								Object[] objInvDate = executeInserts(strSQLDate);

								strSQL= "delete from ssws_withholdingsale where ssws_withholdingsale_id ='"+ strWithholdingSalesID +"'";
								objInvoice = executeInserts(strSQL);
	
								//return ObjMsg;
								countErrors++;
								continue; 
							}
							
							for (Imdlv_Lines collLines : lstLines) {
								 
								intLine=intLine+10;

										
								strcode  = collLines.getCodexml();
								strcodRet= collLines.getCoderet();
								
								BigDecimal bgdRate = new BigDecimal(collLines.getCodetax().toString());
								BigDecimal bdgR = bgdRate.multiply(new BigDecimal("-1"));
								
								OBCriteria<TaxRate> obctaxRate = OBDal.getInstance().createCriteria(TaxRate.class);
								obctaxRate.add(Restrictions.eq(TaxRate.PROPERTY_EEISRITAXCATCODE, strcodRet));
								obctaxRate.add(Restrictions.eq(TaxRate.PROPERTY_EEISRITAXTYPE, strcode));
								obctaxRate.add(Restrictions.eq(TaxRate.PROPERTY_RATE, bdgR));
								
								if (obctaxRate.list().size()>0) {
									
									
										strTaxResult = obctaxRate.list().get(0).getTaxCategory().getSswhWithholdingtype();
										strTaxRateID= obctaxRate.list().get(0).getId();
								}
								if (strTaxResult.equals("")) {
									strTaxRateID = collLines.getTax().getId();
								}
								strProductID = collLines.getProduct().getId();
								
								if(strTaxResult.equals("VA")) {
									strAmountRent ="0";
									strWithRent ="0";
									strAmountVAT =collLines.getLineamtvat().toString();
									strWithVAT = collLines.getVatamt().toString();
									strWithType="N";
								}else {
									strAmountRent =collLines.getLineamtrent().toString();
									strWithRent = collLines.getWithhrent().toString();
									strAmountVAT ="0";
									strWithVAT ="0";
									strWithType="Y";
								}
								
								//strWithholdingSalesID
								
								String strSqlLines="INSERT INTO public.ssws_withholdingsaleline(" + 
										"	ssws_withholdingsaleline_id, ad_client_id, ad_org_id, isactive, " + 
										"	created, createdby, updated, updatedby, " + 
										"	description, ssws_withholdingsale_id, line, m_product_id, " + 
										"	linenetamt, lineivaamt, whrentalamt, whivaamt, " + 
										"	c_tax_id, c_invoiceline_id, isrental)" + 
										"	VALUES (get_uuid(), '"+strClienId+"', '"+strOrgId+"', 'Y', "+
										"now(), '"+strUserId+"', now(), '"+strUserId+"', "+
										"'--', '" + strWithholdingSalesID +"',  "+ intLine +", '"+ strProductID +"', "+
										strAmountRent +", "+ strAmountVAT +", "+ strWithRent +", "+ strWithVAT +", "+
										"'" + strTaxRateID +"', null, '"+ strWithType+"');";
										;
								System.out.println("Lines 1: " + strSqlLines);

								 objInvoiceLines  = executeInserts(strSqlLines) ;
								 
								 if (objInvoiceLines[0].toString().contains("ERROR")) {
									 CountErrorLine++;
								 }
							}
							
							if (objInvoiceLines[0].toString().contains("ERROR")) {
								intIdx++;
								
								ObjMsg[1]=objInvoiceLines[1];
								
								String StrMsgError=ObjMsg[1].toString();
								String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
								Object[] objInvDate = executeInserts(strSQLDate);
								
								
								if (CountErrorLine>0) {
								
									strSQL= "delete from ssws_withholdingsale where ssws_withholdingsale_id ='"+ strWithholdingSalesID +"'";
									objInvoice = executeInserts(strSQL);
									
									strSQL= "delete from ssws_withholdingsaleline where ssws_withholdingsale_id ='"+ strWithholdingSalesID +"'";
									objInvoice = executeInserts(strSQL);

									ObjMsg[0]="Error";
								}
								//return ObjMsg;
								countErrors++;
							}
							
						
							
							try {
								ObjMsg= completeWithholdins(strWithholdingSalesID);
								
								Object[] objInvoicetmp = executeInserts("commit");
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							String strNewMSg =ObjMsg[0].toString();
							
							if (strNewMSg.equals("ERROR")) {
								
								strSQL= "delete from ssws_withholdingsaleline where ssws_withholdingsale_id ='"+ strWithholdingSalesID +"'";
								objInvoice = executeInserts(strSQL);
								
								strSQL= "delete from ssws_withholdingsale where ssws_withholdingsale_id ='"+ strWithholdingSalesID +"'";
								objInvoice = executeInserts(strSQL);
								

								ObjMsg[0]="Error";
								
								strNewMSg = ObjMsg[1].toString();
								if (strNewMSg.contains("Ssws_ErrorExistPeriod")) {
									ObjMsg[1]="Msg: El período no existe";
								}else if (strNewMSg.contains("Ssws_InvoiceNotPosted")) {
									ObjMsg[1]="Msg: La factura no se encuentra contabilizada";
								}else {
									ObjMsg[1]=strNewMSg;
								}
								
								String StrMsgError= ObjMsg[1].toString();
								String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
								Object[] objInvDate = executeInserts(strSQLDate);
								countErrors++;
								CountErrorLine++;
							}
							
							if (CountErrorLine==0) {
								strSQL= "update imdlv_voucherpurchline set isprocess='Y',logserror = coalesce(logserror,'') || 'Transacción creada satisfactoriamente'  where imdlv_voucherpurchline_id ='"+ colPurchLines.getId() +"'";
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
		if (intIdx==0) {
			intIdx=countErrors;
		}
		ObjMsg[1]= intIdx + " linea(s) con error(es), revise los logs de los registros no procesados para más información";
	}else if (intIdx==0) {
		ObjMsg[0]="OK";
		ObjMsg[1]="Se ha completado correctamente el proceso.";
		
		
	}
	try {
		connPartner.getStatement().getConnection().commit();
		connPartner.getStatement().close();
		connPartner.destroy();
	}catch (Exception e) {
		// TODO: handle exception
	}
	return ObjMsg;
	
}

	  public Object[] getDataReaderRetencionesXML(String strXML,String strheader) {
			List<Object> listaLineas = new ArrayList<>();
			
			int indx=0;
			  int indxAux=0;
			  Document document = parseXmlFile(strXML);
			  document.getDocumentElement().normalize();
			  document.getDocumentElement().normalize();
		   

				String strEtiquetas ="baseImponible,valorRetenido";
				Object[] objEtiquetasXML= strEtiquetas.split(",");
		  
				  document.getDocumentElement().normalize();
				  document.getDocumentElement().normalize();
			      System.out.println("Root Element :" + document.getDocumentElement().getNodeName());
			      NodeList nList = document.getElementsByTagName(strheader);
			      System.out.println("----------------------------");
			      
			      Object[] objLines = new Object[6]; 
			      objLines[0]="0";
			      objLines[1]="0";
			      objLines[2]="";
			      objLines[3]="";
			      objLines[4]="";
			      objLines[5]="";
			      
			      
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
			                                objLines[2] = eElementimp.getElementsByTagName("codigo").item(0).getTextContent();
			                                objLines[3] = eElementimp.getElementsByTagName("codigoRetencion").item(0).getTextContent();
			                                objLines[4] = eElementimp.getElementsByTagName("porcentajeRetener").item(0).getTextContent();
			                                objLines[5] = eElementimp.getElementsByTagName("codDocSustento").item(0).getTextContent();

			                                

			                          }
			                          
			                          listaLineas.add(objLines);
			                          objLines[0]="0";
			        			      objLines[1]="0";
			        			      objLines[2]="";
			        			      objLines[3]="";
			        			      objLines[4]="";
			        			      objLines[5]="";
	                       }
	                          
	                 
	                      
			              }
			              
			          }
			      }

			      /*
			      System.out.println("**** Valores ******");
			      	
					for (int printObj=0; printObj<obj.length;printObj++) {
							System.out.println(" Valor :" + printObj + " -> " + obj[printObj].toString());
						
						
					}*/
			      Object[] obj=null;
			      
			      if (listaLineas.size()>0) {
			      	obj = new Object[listaLineas.size()];
			      	int idx=0;
			      	for (Object newObj: listaLineas) {
			      		obj[idx]= newObj;

			      		idx++;
			      	}
			      }
			  
				return obj;
				
			
			
		}


public Object[] completeWithholdins(String strID){
		  
		  Object[] objMsg= new Object[2];
		  String returnString="";
		  try {
				// Se llama a la función para insertar en las líneas de detalle
				org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
		        CallableStatement InsertLineStatement = cp.getConnection()
		              .prepareCall("{call IMDLV_WHSALE_PROCESS(?)}"); // ORIGINAL
		        InsertLineStatement.setString(1,strID);
		        try {
		        	InsertLineStatement.execute();
		        }catch(Exception epos) {
		        	objMsg[0]="ERROR";
					
					if (returnString.equals("")) {
						returnString=epos.getMessage().toString();
					}
		        	objMsg[1]=returnString;
		        }
		        
		        ResultSet rs = InsertLineStatement.getResultSet();
		        if (rs==null) {
		        	  cp.destroy();
		        	  InsertLineStatement.close();
		        	return objMsg;
		        }
		        	
		        while (rs.next()) {
		        	returnString=(rs.getString(1));
		        }
		        if (returnString.contains("ERROR")) {
		        	objMsg[0]="ERROR";
		        	objMsg[1]=returnString;
		        	
		        	if (returnString.contains("PeriodNotAvailable")) {
		        		objMsg[1]="Msg: El periódo no se encuentra abierto. ";
		        	}
		        	if (returnString.contains("EEI_Authorization_Ret")) {
		        		objMsg[1]="Msg: La autorización electrónica de la retención es obligatoria. ";
		        	}
		        	 
		        	if (returnString.contains("EEI_Authorization")) {
		        		objMsg[1]="Msg: La autorización electrónica de la factura es obligatoria. ";
		        	}
		        	if (returnString.contains("SSWS_InsuficientAmountForWithholding")) {
		        		objMsg[1]="Msg: Monto insuficiente para la retención ";
		        	}
		        	// , , , 
		        	//, 
		        	if (returnString.contains("Ssws_ErrorExistPeriod")) {
		        		objMsg[1]="Msg: No existe el período";
		        	} 
		        	if (returnString.contains("Ssws_ErrorPeriod") && returnString.contains("Ssws_ErrorWithholdingDate")) {
		        		objMsg[1]="Msg: Error en la fecha de retención";
		        	}
		        	if (returnString.contains("SSWS_NoPaymentSchedDetail")) {
		        		objMsg[1]="Msg: No existe un detalle de pago para procesar la retención";
		        	}
		        	if (returnString.contains("SSWS_NoWithholdingAmount")) {
		        		objMsg[1]="Msg: No existen montos para esta retención";
		        	}
		        	if (returnString.contains("Ssws_InvoiceNotPosted")) {
		        		objMsg[1]="Msg: La factura relacionada a esta retención no esta contabilizada";
		        	}
		        	if (returnString.contains("SSWS_NotConfigWithholding")) {
		        		objMsg[1]="Msg: No existen configuraciones para generar la retención";
		        	}
		        	
		        }else {
		        	returnString="OK";
		        }
		        if(returnString.contains("OK")) {
		        	objMsg[0]="OK";
		        	objMsg[1]="Sucess";
		        }
		        InsertLineStatement.close();
		    	try {
		    		cp.getStatement().getConnection().commit();
		    		cp.getStatement().close();
		    	}catch (Exception e) {
		    		// TODO: handle exception
		    	}
		        cp.destroy();
		        rs.close();
		} catch (Exception e) {
			objMsg[0]="ERROR";
			
			if (returnString.equals("")) {
				returnString=e.getMessage().toString();
			}
        	objMsg[1]=returnString;
		}
		  return objMsg;
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
	      try {
	    	  connectionProvider.getStatement().close();
	    	  connectionProvider.destroy();
	      }catch(Exception ee) {
	    	  
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
	        cp.getStatement().getConnection().commit();
	        cp.getStatement().close();
	        InsertLineStatement.close();
	        cp.destroy();
	        rs.close();
	} catch (Exception e) {
		objMsg[0]="ERROR";
    	objMsg[1]=returnString;
	}
	  return objMsg;
  }

  
  public Object[] executeInsertsOrder(ConnectionProvider cp,String strSQL) {
	  
	  Object[] objMsg= new Object[2];
	  String returnString="OK";
	  CallableStatement InsertLineStatement =null;
	  try {
			// Se llama a la función para insertar en las líneas de detalle
			//org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
	        InsertLineStatement = cp.getConnection()
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
	        cp.getStatement().getConnection().commit();
	        //cp.getTransactionConnection().commit();
	        //cp.getTransactionConnection().close();
	        InsertLineStatement.close();
	        cp.getStatement().close();
	        cp.destroy();
	        rs.close();
	} catch (Exception e) {
		objMsg[0]="ERROR";
    	objMsg[1]=returnString;
    	try {
			cp.getStatement().getConnection().commit();
			 InsertLineStatement.close();
		        cp.getStatement().close();
		        cp.destroy();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        //cp.getTransactionConnection().commit();
        //cp.getTransactionConnection().close();
       
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

  public String getDataBPHeaderXML(String strXML,String strheader) {
		String strAddress="";
		int indx=0;
		  int indxAux=0;
		  Document document = parseXmlFile(strXML);
		  document.getDocumentElement().normalize();

		      NodeList nList = document.getElementsByTagName(strheader);
		      System.out.println("----------------------------");
		      
		      Object[] objLines = new Object[5]; 
		      objLines[0]="";
		      objLines[1]="";
		      objLines[2]="";
		      objLines[3]="";
		      objLines[4]="";
		      
		      
		      for (int intmain = 0; intmain < nList.getLength(); intmain++) {
		          Node nNodemain = nList.item(intmain);
		          System.out.println("\nCurrent Element :" + nNodemain.getNodeName());
		          if (nNodemain.getNodeType() == Node.ELEMENT_NODE) {
		              Element eElementmain = (Element) nNodemain;
		             // System.out.println("detalle : " + intmain + eElementmain.getAttribute("detalles"));
		              strAddress= eElementmain.getElementsByTagName("dirEstablecimiento").item(0).getTextContent();
		          }
		      }
		      Object[] obj=null;
		      obj=objLines;
		      
		      /*
		      System.out.println("**** Valores ******");
		      	
				for (int printObj=0; printObj<obj.length;printObj++) {
						System.out.println(" Valor :" + printObj + " -> " + obj[printObj].toString());
					
					
				}*/
		  
			return strAddress;
		
		
	}
 
  
  public Object[] getDataOnlyReaderHeaderXML(String strXML,String strheader) {
		List<Object> listaLineas = new ArrayList<>();
		
		int indx=0;
		  int indxAux=0;
		  Document document = parseXmlFile(strXML);
		  document.getDocumentElement().normalize();
		  document.getDocumentElement().normalize();
	   

			String strEtiquetas ="estab,ptoEmi,secuencial";
			Object[] objEtiquetasXML= strEtiquetas.split(",");
	  
			  document.getDocumentElement().normalize();
			  document.getDocumentElement().normalize();
		      System.out.println("Root Element :" + document.getDocumentElement().getNodeName());
		      NodeList nList = document.getElementsByTagName("infoTributaria");
		      System.out.println("----------------------------");
		      
		      Object[] objLines = new Object[3]; 
		      objLines[0]="";
		      objLines[1]="";
		      objLines[2]="";

		      
		      
		      for (int intmain = 0; intmain < nList.getLength(); intmain++) {
		          Node nNodemain = nList.item(intmain);
		          System.out.println("\nCurrent Element :" + nNodemain.getNodeName());
		          if (nNodemain.getNodeType() == Node.ELEMENT_NODE) {
		              Element eElementmain = (Element) nNodemain;
                      objLines[0] = eElementmain.getElementsByTagName("estab").item(0).getTextContent();
                      objLines[1] = eElementmain.getElementsByTagName("ptoEmi").item(0).getTextContent();
                      objLines[2] = eElementmain.getElementsByTagName("secuencial").item(0).getTextContent();
                   
                      
		              
		          }
		      }

		      /*
		      System.out.println("**** Valores ******");
		      	
				for (int printObj=0; printObj<obj.length;printObj++) {
						System.out.println(" Valor :" + printObj + " -> " + obj[printObj].toString());
					
					
				}*/
		      Object[] obj=null;
		      
		      if (listaLineas.size()>0) {
		      	obj = new Object[listaLineas.size()];
		      	int idx=0;
		      	for (Object newObj: listaLineas) {
		      		obj[idx]= newObj;

		      		idx++;
		      	}
		      }
		  
			return obj;
			
		
		
	}

  
  public  Object[] InsertOrder(ImdlvVoucherData LinesImporData,String strClientID,String strOrgID, String strUserID) {

	  	String strSql = "";
	  	String strOrderID="";
	  	String strOrderLineID="";
	  	String strInoutID="";
	  	String strInoutLineID="";
	  	String strDocumetnNo="";
	  	String strDocTypeOrderID="";
	  	String strDocTypeID="";
	  	String strDocTypeInoutID="";
	  	String strDateOrder="";
	  	
	  	String strCurrencyID="";
	  	String strLanguageID="";
	  	String strPaymentTermsID="";
	  	String strPaymentMethodID="";
	  	String strWarehouseID = "";
	  	String strPriceListID="";
	  	String strFilename ="";
	  	
	  	String strLocationID="";
	  	
	  	int intCountErrors=0;
		strFilename = LinesImporData.getFilenamedata();
		String strDocumentXMLProcess=LinesImporData.getDocumentno();
				
		ConnectionProvider connOrder = new DalConnectionProvider(false);

		
	    Object[] strMsg=new Object[2];

		
		OBCriteria<ImdlvVoucherDataLine> obc = OBDal.getInstance().createCriteria(ImdlvVoucherDataLine.class);
		obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_IMDLVVOUCHERPURCHASE, LinesImporData));
		obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ISPROCESS, false));
		obc.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ISCREATEORDER, true));
		obc.add(Restrictions.isNotNull(ImdlvVoucherDataLine.PROPERTY_XMLSRI));
		
		OBCriteria<ImdlvPartner> obcDefaultPartner = OBDal.getInstance().createCriteria(ImdlvPartner.class);
		obcDefaultPartner.add(Restrictions.eq(ImdlvPartner.PROPERTY_ACTIVE, true));
		
		
		strCurrencyID = obcDefaultPartner.list().get(0).getCurrency().getId();
		strLanguageID = obcDefaultPartner.list().get(0).getLanguage().getId();
		strPaymentTermsID = obcDefaultPartner.list().get(0).getPaymentterm().getId();
		strPaymentMethodID = obcDefaultPartner.list().get(0).getFINPaymentmethod().getId();
		
		strPriceListID = obcDefaultPartner.list().get(0).getPricelist().getId();
		
		
		
		OBCriteria<ImdlvPurchaseIinvoice> purchaseInvoiceDf = OBDal.getInstance().createCriteria(ImdlvPurchaseIinvoice.class);
		purchaseInvoiceDf.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_ACTIVE, true));
		purchaseInvoiceDf.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_TYPETRXXML,"factura"));
		
		if (purchaseInvoiceDf.list().size()==0) {
			
			String StrMsgError="Msg: No existe un tipo de documento configurado para las facturas.  ";
			//String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror || ' ' ,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ colPurchLines.getId()  +"'";
			//Object[] objInvDate = executeInserts(strSQLDate);
			Object objMsg[]=new Object[2];
			objMsg[0]="Error";
	    	objMsg[1]=StrMsgError;
	    	return objMsg;
	    	
			
		}
		
		if (purchaseInvoiceDf.list().size()>0) {
			
			try {
			strDocTypeOrderID=purchaseInvoiceDf.list().get(0).getDoctypeOrder().getId();	
			//strDocTypeID="F6F9BD7DD1794BE7AA614497162A3ADD"; 
			strDocTypeInoutID=purchaseInvoiceDf.list().get(0).getDoctypeInout().getId();
			
			strWarehouseID=purchaseInvoiceDf.list().get(0).getWarehouse().getId();
			}catch(Exception e) {
				
			}

		}
		

		
		
	    List<ImdlvVoucherDataLine> listVoucherDataLine= obc.list();
	    if (listVoucherDataLine.size()==0) {
	    	strMsg[0]= "OK";
	    	strMsg[1]= "Success";
	    	return  strMsg;
	    }
	    
	    for (ImdlvVoucherDataLine collDataline: listVoucherDataLine) {
	    	
	    	if (!collDataline.getDescription().equals("factura")) {
				continue;
			}
	    	
			 if (strDocTypeOrderID.equals("") || strDocTypeInoutID.equals("") || strWarehouseID.equals("") ) {

			    	String StrMsgErrors="Msg: No se encuentra configurado la bodega y/o el tipo de documento: orden y albarán";
					String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror,' ')||  '"+ StrMsgErrors +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
					Object[] objInvDate = executeInserts(strSQLDate);
					strMsg[0]="Error";
					strMsg[1]=StrMsgErrors;
					countErrors++;
					continue;
			    }
			 
			ImdlvVoucherDataLine Datalinetmp = collDataline;
			
			OBCriteria<Imdlv_Lines> vLines = OBDal.getInstance().createCriteria(Imdlv_Lines.class);
			vLines.add(Restrictions.eq(Imdlv_Lines.PROPERTY_IMDLVVOUCHERPURCHLINE, Datalinetmp));
			

			List<Imdlv_Lines> listLines = vLines.list();
			
			if (listLines.size()==0) {
				continue;
			}
			
			String strPoreference = collDataline.getInvoiceno();
			

			
		  	strOrderID="";
		  	strOrderLineID="";
		  	strInoutID="";
		  	strInoutLineID="";
	    	

	    	
			BusinessPartner BPartner = ValidateBPartner(collDataline.getTaxid());
			
			if (BPartner==null) {
				countErrors++;
				continue;
			}
			
			
			strPaymentMethodID = BPartner.getPOPaymentMethod()==null?strPaymentMethodID: BPartner.getPOPaymentMethod().getId();
			//strPriceListID = BPartner.getPurchasePricelist()==null?strPriceListID: BPartner.getPurchasePricelist().getId();
			//strPaymentTermsID = BPartner.getPOPaymentTerms()==null?strPaymentTermsID: BPartner.getPOPaymentTerms().getId();
			
			
			if (!strPoreference.equals("null") && !strPoreference.equals("ND") &&  !strPoreference.equals(null) ) {
				
				String StrInvoiceSearch = get_InvoiceExists(connOrder,strPoreference,"N", BPartner.getId() );
				
				//OBCriteria<Invoice> obcInv = OBDal.getInstance().createCriteria(Invoice.class);
				//obcInv.add(Restrictions.eq(Invoice.PROPERTY_DOCUMENTNO, strInvoiceReference ));
				
				if ( !StrInvoiceSearch.equals("ND") ) {
					
					
					String StrMsgError="Msg: La factura ya se encuentra registrada. ";
					String strSQLDate= "update imdlv_voucherpurchline set logserror = coalesce(logserror,' ')||  '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
					Object[] objInvDate = executeInserts(strSQLDate);
					strMsg[0]="Error";
					strMsg[1]=StrMsgError;
					countErrors++;
					continue;
					
					
				}
			
			}
			
			strOrderID=getUUID(connOrder);
			
			strLocationID = BPartner.getBusinessPartnerLocationList().get(0).getId();
			strDateOrder = new SimpleDateFormat("yyyy-MM-dd",   Locale.getDefault()).format(collDataline.getDateemision());


			DocumentType docObj = OBDal.getInstance().get(DocumentType.class, strDocTypeOrderID);
		    
			if (docObj.getDocumentSequence()!=null) {
		     
				Sequence seq = docObj.getDocumentSequence();
		      
				strDocumetnNo = (seq.getPrefix()!=null?seq.getPrefix().toString():"")+ seq.getNextAssignedNumber().toString();
		      
				seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
		      
				OBDal.getInstance().save(seq);
				OBDal.getInstance().flush();
				OBDal.getInstance().refresh(seq);
		    }else {
		    	strDocumetnNo="001-001-000000001";
		    }
			String strSqlMsg="";
			
			//Creación de Orden
			if (!strDocumetnNo.equals("")) {

			    strSql = "insert into c_order(c_order_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, " + 
			    		"issotrx, documentno, docstatus, docaction, processing, processed, c_doctype_id, c_doctypetarget_id, " + 
			    		"isdelivered, isinvoiced, isprinted, isselected, dateordered, datepromised,  "+ 
			    		"dateprinted, dateacct, c_bpartner_id, billto_id, c_bpartner_location_id, isdiscountprinted,  "+ 
			    		"c_currency_id, paymentrule, c_paymentterm_id, invoicerule, deliveryrule, freightcostrule, freightamt,  "+ 
			    		"deliveryviarule, chargeamt, priorityrule, totallines, grandtotal, m_warehouse_id,  "+ 
			    		"m_pricelist_id, istaxincluded, posted,  "+ 
			    		"isselfservice, "+ 
			    		"fin_paymentmethod_id,create_polines, iscashvat, " + 
			    		"iscancelled , description) "+
			    		"values('"+ strOrderID + "','"+ strClientID +"','"+ strOrgID +"','Y',now(),'"+ strUserID + "',now(),'"+strUserID+"',"+
			    		"'N','" +strDocumetnNo  +"', 'DR','CO','N','N','" + strDocTypeOrderID +"','"+ strDocTypeOrderID   +"',"+
			    		"'N','N','N','N','"+ strDateOrder +"','" + strDateOrder + "'," +
			    		"'" +strDateOrder +"','" + strDateOrder + "','" + BPartner.getId() + "','" + strLocationID + "','" + strLocationID + "','N',"+
			    		"'" + strCurrencyID + "','P','"+strPaymentTermsID  + "','I','A','I',0,"+
			    		"'D',0,5,0,0,'" + strWarehouseID + "',"+
			    		"'" + strPriceListID+"','N','N',"+
			    		"'N',"+
			    		"'" + strPaymentMethodID + "','N','N','N'"+
			    		",'" + strDocumentXMLProcess + " - " + strFilename + "'"+
			    		")";
		
			    System.out.println(strSql);
			    strMsg =  executeInsertsOrder(connOrder,strSql);
			}
			int intLine=0;
			strSqlMsg = strMsg[0].toString(); 
			if (strSqlMsg.equals("OK")) {
				strMsg[0]="";
				for (Imdlv_Lines collLines: listLines) {
					intLine= intLine + 10;
					//strOrderLineID=getUUID(connOrder);
					
					strSql ="insert into c_orderline (c_orderline_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, c_order_id," + 
							"line, c_bpartner_id, c_bpartner_location_id, dateordered, datepromised, " + 
							"m_product_id, m_warehouse_id, directship, c_uom_id, qtyordered, qtyreserved, qtydelivered, qtyinvoiced, " + 
							"c_currency_id, pricelist, priceactual, pricelimit, linenetamt, discount, freightamt, chargeamt, c_tax_id, " + 
							"isdescription,   " + 
							"pricestd, cancelpricead, iseditlinenetamt, taxbaseamt,  " + 
							"gross_unit_price, line_gross_amount,   " + 
							"explode, " + 
							"print_description,  relate_orderline  " + 
							//"em_ssip_costorigin, -- preguntar si existe el módulo para insertar este dato" + 
							//"em_ssip_identifier  -- preguntar si existe el módulo para insertar este dato" + 
							")"+
							"values(get_uuid(),'" + strClientID + "','" + strOrgID + "','Y',now(),'" + strUserID + "',now(),'" +strUserID +"','"+ strOrderID + "'," +  
							String.valueOf(intLine) + ",'" + BPartner.getId()+ "','"+  strLocationID + "','"+ strDateOrder+ "','" + strDateOrder+"','"+
							collLines.getProduct().getId() + "','" + strWarehouseID + "','N','" +collLines.getProduct().getUOM().getId()+ "'," + collLines.getQtyinvoiced() +",0,0,0,'"+  
							strCurrencyID + "'," + collLines.getPriceactual() + ","+ collLines.getPriceactual() + ","+ collLines.getPriceactual() + "," + collLines.getLinenetamt() +",0,0,0,'"+ collLines.getTax().getId() + "'," +
							"'N'," + collLines.getPriceactual()+ ",'N','N'," + collLines.getTaxamt() + ","+
							"0,0,'N','N','N'"+
							")"
							;
					
					 System.out.println(strSql);
					 strMsg =  executeInsertsOrder(connOrder,strSql);
					 strSqlMsg = strMsg[0].toString(); 
					 if (strSqlMsg.equals("ERROR")) {
						 intCountErrors++;
						 String StrMsgError =  strMsg[1].toString() ;
							String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
							Object[] objInvoice2 = executeInsertsOrder(connOrder,strSQL2);
							strMsg[0]= "Error";
					 }
					 
					 
				}
			}else {
				strSqlMsg = strMsg[1].toString(); 
				String StrMsgError=strSqlMsg;
				String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
				Object[] objInvoice2 = executeInsertsOrder(connOrder,strSQL2);
				countErrors++;
				strMsg[0]= "Info";
				
				strSql= "delete from c_orderlinetax where c_order_id ='"+ strOrderID +"'";
				Object[] objInvoice = executeInsertsOrder(connOrder,strSql);
				
				strSql= "delete from c_orderline where c_order_id ='"+ strOrderID +"'";
				objInvoice =  executeInsertsOrder(connOrder,strSql);
				
				strSql= "delete from c_ordertax where c_order_id ='"+ strOrderID +"'";
				objInvoice =  executeInsertsOrder(connOrder,strSql);
				
				strSql= "delete from c_order where c_order_id ='"+ strOrderID +"'";
				objInvoice =  executeInsertsOrder(connOrder,strSql);

				continue;
				

			}
		
			
			strSqlMsg= strMsg[0].toString();
			if (strSqlMsg.equals("OK")) {
				String StrFunction ="{call IMDLV_ORDER_POST1(null,?,'Y')}";
				strMsg = completeFunction(connOrder,StrFunction, strOrderID);
				strSqlMsg= strMsg[0].toString();
				
				//Creación de Albarán
				if (strSqlMsg.equals("OK")) {
					
					
					strInoutID = getUUID(connOrder);
					strDateOrder = new SimpleDateFormat("yyyy-MM-dd",   Locale.getDefault()).format(collDataline.getDateemision());

					
					DocumentType docObjInout = OBDal.getInstance().get(DocumentType.class, strDocTypeInoutID);
				    
					if (docObjInout.getDocumentSequence()!=null) {
				     
						Sequence seq = docObjInout.getDocumentSequence();
				      
						strDocumetnNo = (seq.getPrefix()!=null?seq.getPrefix().toString():"")+ seq.getNextAssignedNumber().toString();
				      
						seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
				      
						OBDal.getInstance().save(seq);
						OBDal.getInstance().flush();
						OBDal.getInstance().refresh(seq);

				      
				    }else {
				    	strDocumetnNo="001-001-000000001";
				    }
					//strDateOrder="2022-01-15";
					//StrDateInvoice="2022-01-15";
				    strSql = "insert into m_inout(m_inout_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, issotrx, " + 
				    		"documentno, docaction, docstatus, posted, processing, processed, c_doctype_id, c_order_id, " + 
				    		"dateordered, isprinted, movementtype, movementdate, dateacct, c_bpartner_id, c_bpartner_location_id, " + 
				    		"m_warehouse_id, deliveryrule, freightcostrule, freightamt, deliveryviarule,  " + 
				    		"chargeamt, priorityrule,createfrom, generateto, ad_user_id, " + 
				    		"islogistic, generatelines, calculate_freight, " + 
				    		"rm_receipt_pickedit, rm_shipment_pickedit, " + 
				    		"process_goods_java, isnettingshipment,description) values(" +
				    		"'" +  strInoutID + "','" + strClientID + "','" + strOrgID + "','Y',now(),'" + strUserID + "',now(),'" +strUserID +"','N','"+ 
				    		strDocumetnNo + "','CO','DR','N','N','N','" + strDocTypeInoutID + "','" + strOrderID + "','"+
				    		strDateOrder + "','N','V+','" + strDateOrder +"','" + strDateOrder  +"','" + BPartner.getId() + "','" + strLocationID +  "','" +
				    		strWarehouseID + "','A','I',0,'P',"+
				    		"0,'S','N','N','" + strUserID+ "','"+
				    		"N','N','N'," +
				    		"'N','N'," +
				    		"'CO','N','" + strDocumentXMLProcess  + " - " + strFilename + "'"+
				    		")";
				    System.out.println(strSql);
					strMsg = executeInsertsOrder(connOrder,strSql);// executeInserts(strSql);
					strSqlMsg= strMsg[0].toString();
					
					if (strSqlMsg.equals("ERROR")) {
						 intCountErrors++;
						 String StrMsgError =  strMsg[1].toString() ;
							String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
							Object[] objInvoice2 = executeInsertsOrder(connOrder,strSQL2);
					}
					
					//Creación de líneas Albarán
					if (strSqlMsg.equals("OK")) {
						
						
						Order obdOrder = OBDal.getInstance().get(Order.class, strOrderID);

						OBCriteria<OrderLine> vOrderLines = OBDal.getInstance().createCriteria(OrderLine.class);
						vOrderLines.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, obdOrder));
						
						if (vOrderLines.list().size()>0) {
							
						  	//strInoutLineID = getUUID(connOrder);
						  	intLine=0;
						  	for (OrderLine collOrdLines: vOrderLines.list()) {
						  		
						  		intLine= intLine+10;
								strSql = "insert into m_inoutline(m_inoutline_id, ad_client_id, ad_org_id, isactive, created, createdby, updated, updatedby, " + 
										 "line, m_inout_id, c_orderline_id, m_locator_id, " + 
										 "m_product_id, c_uom_id, movementqty, " + 
										 "isinvoiced, isdescription, c_bpartner_id, explode) values(" +
										  "get_uuid(),'" + strClientID + "','" + strOrgID + "','Y',now(),'" + strUserID + "',now(),'" +strUserID +"',"+
										 intLine + ",'" + strInoutID + "','" + collOrdLines.getId() + "','" + collOrdLines.getWarehouse().getLocatorList().get(0).getId() + "','" +
										 collOrdLines.getProduct().getId() + "','" + collOrdLines.getProduct().getUOM().getId() + "'," + collOrdLines.getOrderedQuantity().toString() +",'" + 
										 "N','N','" + BPartner.getId() + "','N'" +
										 ")";
								System.out.println(strSql);
								strMsg =  executeInsertsOrder(connOrder,strSql);// executeInserts(strSql);
								strSqlMsg= strMsg[0].toString();
								if (strSqlMsg.equals("ERROR")) {
									intCountErrors++;
									 String StrMsgError =  strMsg[1].toString() ;
										String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
										Object[] objInvoice2 = executeInsertsOrder(connOrder,strSQL2);
								}
						  	}
						  	
						  	/*StrFunction ="{call IMDLV_INOUT_POST(null,?)}";
							strMsg = completeFunction(connOrder,StrFunction, strInoutID);
							strSqlMsg= strMsg[0].toString();
							
							strSqlMsg="OK";
							
							if (strSqlMsg.equals("ERROR")) {
								intCountErrors++;
								 String StrMsgError =  strMsg[1].toString() ;
									String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
									Object[] objInvoice2 = executeInserts(strSQL2);
							}*/
							
							
							if (intCountErrors==0) {
											//strMsg =  executeInserts("commit");
											//strSqlMsg= strMsg[0].toString();
											
											String StrInvoiceID=getUUID(connOrder);
											strDocumetnNo ="";
											strDocTypeID="";
											String strValDocTypeID="";
											String strWithhDoctypeId="null";
											String strLiveliHoodId="null";
											String strCodeLiveliHoodId="null";
											String strAuthorizationWithh="null";
											String strValidDocumentNo = collDataline.getInvoiceno().replace("-", "");
											String strProductID="";							
											String StrDateInvoice = new SimpleDateFormat("yyyy-MM-dd",   Locale.getDefault()).format(collDataline.getDateemision());
			
											strWithhDoctypeId ="'" + purchaseInvoiceDf.list().get(0).getDoctypeWithholding().getId() +"'" ; 
											strDocTypeID=purchaseInvoiceDf.list().get(0).getDoctypePurchase().getId();
											strLiveliHoodId = "'" +purchaseInvoiceDf.list().get(0).getSswhLivelihoodt().getId()+"'";
											strCodeLiveliHoodId = "'" +purchaseInvoiceDf.list().get(0).getSswhCodelivelihoodt().getId()+"'";
											boolean isElectronic=true;
												
											strPoreference = collDataline.getInvoiceno();
											
											OBCriteria<Invoice> obdInv = OBDal.getInstance().createCriteria(Invoice.class);
											obdInv.add(Restrictions.eq(Invoice.PROPERTY_ORDERREFERENCE, strPoreference));
											obdInv.add(Restrictions.eq(Invoice.PROPERTY_BUSINESSPARTNER, BPartner));

											List<Invoice> obInvList = obdInv.list();
											
											if (obInvList.size()>1) {
												
												strSql= "alter table m_inout disable trigger all";
												Object[] objInvoice = executeInsertsOrder(connOrder,strSql);
												
												strSql= "alter table c_order disable trigger all";
												objInvoice = executeInsertsOrder(connOrder,strSql);
												
												
												
												strSql= "delete from m_inoutline where m_inout_id ='"+ strInoutID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSql);
												
												strSql= "delete from m_inout where m_inout_id ='"+ strInoutID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSql);
												
												
												strSql= "delete from c_orderlinetax where c_order_id ='"+ strOrderID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSql);
												
												strSql= "delete from c_orderline where c_order_id ='"+ strOrderID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSql);
												
												strSql= "delete from c_ordertax where c_order_id ='"+ strOrderID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSql);
												
												strSql= "delete from c_order where c_order_id ='"+ strOrderID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSql);

												strSql= "alter table m_inout enable trigger all";
												objInvoice = executeInsertsOrder(connOrder,strSql);
												
												strSql= "alter table c_order enable trigger all";
												objInvoice = executeInsertsOrder(connOrder,strSql);
												
												String strSQL= "delete from c_invoicelinetax where c_invoice_id ='"+ StrInvoiceID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSQL);
												
												strSQL= "delete from c_invoiceline where c_invoice_id ='"+ StrInvoiceID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSQL);
												
												strSQL= "delete from c_invoicetax where c_invoice_id ='"+ StrInvoiceID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSQL);
												
												strSQL= "delete from c_invoice where c_invoice_id ='"+ StrInvoiceID +"'";
												objInvoice = executeInsertsOrder(connOrder,strSQL);
												
												String StrMsgError =  "Msg: El número de referencia ya ha sido utilizado por otro documento para el mismo proveedor.";
												String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
												Object[] objInvoice2 = executeInsertsOrder(connOrder,strSQL2);//executeInserts(strSQL2);
												intCountErrors++;
												
												countErrors++;
												continue;
											}
											
												
											String StrDueDate="null";
											String strAuthorizationSRI="null";
											strAuthorizationSRI = "'" + collDataline.getAuthorizationno() + "'";
											strAuthorizationWithh = "'" + collDataline.getAuthorizationno().substring(0, 10) + "'" ;
											StrDueDate=new SimpleDateFormat("yyyy-MM-dd",   Locale.getDefault()).format(collDataline.getDateemision());
											//StrDueDate="2022-01-15";
											//StrDateInvoice="2022-01-15";
											//strCurrencyID = obcDefaultPartner.list().get(0).getCurrency().getId();
											//strLanguageID = obcDefaultPartner.list().get(0).getLanguage().getId();
											//strPaymentTermsID = obcDefaultPartner.list().get(0).getPaymentterm().getId();
											//strPaymentMethodID = obcDefaultPartner.list().get(0).getFINPaymentmethod().getId();
											strDocTypeID=purchaseInvoiceDf.list().get(0).getDoctypePurchase().getId();	
			
											DocumentType docObjInv = OBDal.getInstance().get(DocumentType.class, strDocTypeID);
										    
											if (docObjInv.getDocumentSequence()!=null) {
										     
												Sequence seq = docObjInv.getDocumentSequence();
										      
												strDocumetnNo = (seq.getPrefix()!=null?seq.getPrefix().toString():"")+ seq.getNextAssignedNumber().toString();
										      
												seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
										      
												OBDal.getInstance().save(seq);
												OBDal.getInstance().flush();
												OBDal.getInstance().refresh(seq);
										      
										    }else {
										    	strDocumetnNo="001-001-000000001";
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
													",processing,paymentrule,EM_Sswh_Nroauthorization,EM_Sswh_Expirationdate,EM_Eei_Description"+
													",c_order_id) values("+
													"'"+ StrInvoiceID+"','"+strClientID +"','"+ strOrgID +"','Y',"+
													"now(),'"+strUserID +"', now(), '"+ strUserID+"',"+
													"'N','"+ strDocumetnNo+"','DR','CO',"+
													"'N','N','"+ strDocTypeID+"','"+strDocTypeID+"',"+
													"'N','"+ StrDateInvoice +"','" + StrDateInvoice +"','"+ BPartner.getId()  +"',"+
													"'"+ strLocationID +"','N',"+strCurrencyID+",'"+strPaymentTermsID+"',"+
													"0,0,'"+strPriceListID+"','N',"+
													"'N',0,0,0,"+
													"0,'N','N',0,"+
													"'N',0,0,"+
													"'"+ (isElectronic?'Y':'N') +"','N',"+
													"'N','N',"+
													"'N','N'"+
													//",NULL"+
													",'"+ strPaymentMethodID +"','"+strPoreference +"',"+strWithhDoctypeId+","+strLiveliHoodId+""+
													","+strCodeLiveliHoodId +","+strAuthorizationWithh+",'"+StrDateInvoice+ "'"+
													",'N','P'," + strAuthorizationSRI+ ",'"+ StrDueDate+ "','" + ( collDataline.getInvoiceno() + " - " + strFilename)+ "'"+
													",'" + strOrderID+"'"+
													")"
													;	
								//Object[] objInvoice = InsertInvoice(connPartner,strSQL);
								System.out.println("Fact: " + collDataline.getInvoiceno() );
								
								Object[] objInvoice = executeInsertsOrder(connOrder,strSQL);// executeInserts(strSQL);
								System.out.println("c_invoice : " +strSQL);
			
								Object[] ObjMsg= objInvoice;
								if (objInvoice[0].toString().contains("ERROR")) {
									intCountErrors++;
									
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
									String StrMsgError =  ObjMsg[1].toString() ;
									String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
									Object[] objInvoice2 = executeInsertsOrder(connOrder,strSQL2);//executeInserts(strSQL2);
									 
									strMsg[0]= "Error";
								    strMsg[1]= StrMsgError;
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
									//String strXml = colPurchLines.getXMLSri();
									Object[] objInvoiceLines=null;
									//strEtiquetas ="cantidad,precioUnitario,impuestos,impuesto,tarifa,baseImponible,valor";
									intLine=0;
									
									
									Order obdOrderInv = OBDal.getInstance().get(Order.class, strOrderID);
			
									OBCriteria<OrderLine> vOrderLinesInv = OBDal.getInstance().createCriteria(OrderLine.class);
									vOrderLinesInv.add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, obdOrderInv));
									
									ImdlvVoucherDataLine SearchLinesPl= collDataline;
									
									
									List<OrderLine> oborderlinesList = vOrderLinesInv.list();
									
									if (oborderlinesList.size()>0) {
										
										
										for (OrderLine collOrderLines:oborderlinesList) {
											Long lngLine = collOrderLines.getLineNo();
											OrderLine SearchOrderlines= collOrderLines;
											String strOrderLineIDSearch  = SearchOrderlines.getId();
											String strInoutLineIDSearch  = "null";
											Product prodInv = collOrderLines.getProduct();
											
											OBCriteria<ShipmentInOutLine> oblinesInout = OBDal.getInstance().createCriteria(ShipmentInOutLine.class);
											oblinesInout.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SHIPMENTRECEIPT, SearchOrderlines));

											if (oblinesInout.list().size()>0) {
												strInoutLineIDSearch = "'"+ oblinesInout.list().get(0).getId() +"'";
											}
											
											OBCriteria<Imdlv_Lines> oblines = OBDal.getInstance().createCriteria(Imdlv_Lines.class);
											oblines.add(Restrictions.eq(Imdlv_Lines.PROPERTY_IMDLVVOUCHERPURCHLINE, SearchLinesPl));
											oblines.add(Restrictions.eq(Imdlv_Lines.PROPERTY_PRODUCT, prodInv));
											oblines.add(Restrictions.eq(Imdlv_Lines.PROPERTY_LINE, lngLine));
					
											List<Imdlv_Lines> oblinesList = oblines.list();
											for (Imdlv_Lines collLines:oblinesList) {
												
												intLine=intLine+10;
										
												dblqty= collLines.getQtyinvoiced().toString();
												dblprice=collLines.getPriceactual().toString();
												
												double dblTotLine = Double.valueOf(dblqty) * Double.valueOf(dblprice);
												String dblLinenetamt= String.valueOf(dblTotLine);
												dblrate="";
												dbltaxamt= collLines.getTaxamt().toString();
												if(dblrate.equals("0")) {
													strTaxResult = strTax0;
												}else {
													strTaxResult = strTax12;
												}
												strProductID = collLines.getProduct().getId();
												strTaxResult = collLines.getTax().getId();
					
												String strSqlLines="insert into c_invoiceline"+ 
														"(c_invoiceline_id,   ad_client_id,   ad_org_id,                  isactive,"+
														"created,             createdby,      updated,                    updatedby,"+
														"c_invoice_id,       line,           financial_invoice_line,     qtyinvoiced,"+
														"pricelist,          priceactual,    pricelimit,                 linenetamt,"+
														
														"isdescription,      pricestd,       grosspricestd,              grosspricelist,"+
														"isdeferred,         explode," +
														"m_product_id,c_tax_id,c_uom_id,taxamt,c_bpartner_id,excludeforwithholding,em_imdlv_qty, em_imdlv_price, em_imdlv_totalline, em_imdlv_taxamt, em_imdlv_netprice,c_orderline_id,m_inoutline_id)"+
														"values("+
														"get_uuid()"+",'"+strClientID +"','"+ strOrgID +"','Y',"+
														"now(),'"+strUserID +"', now(), '"+ strUserID+"',"+
														"'" + StrInvoiceID +"', " + intLine +",'N',"+ dblqty +
														"," + dblprice + ","+ dblprice + ",0,"+ dblLinenetamt +
														",'N'," + dblprice +",0,0,"+
														"'N','N'" +
														",'" + strProductID+"','"+strTaxResult+"',(select c_uom_id from m_product where m_product_id ='"+ strProductID +"'),"+dbltaxamt+""+
														",(select c_bpartner_id from c_invoice where c_invoice_id ='"+ StrInvoiceID +"'),'N'"+
														"," + dblqty + "," + dblprice  + "," + dblTotLine + "," + dbltaxamt+ "," + dblprice + 
														",'" + strOrderLineIDSearch  + "',"+ strInoutLineIDSearch +""+
														")"
														;
												System.out.println("Lines 1: " + strSqlLines);
					
												 objInvoiceLines  = executeInsertsOrder(connOrder,strSqlLines);//executeInserts(strSqlLines) ;
												 
												 if (objInvoiceLines[0].toString().contains("ERROR")) {
													 intCountErrors++;
												 }
											}
											
										}
									}
									if (objInvoiceLines[0].toString().contains("ERROR")) {
										
			
										
										String strNewMSg = objInvoiceLines[1].toString();
										if (strNewMSg.contains("SSWH_NoDocumentNoFormat")) {
											ObjMsg[1]="Msg: El formato de la autorización debe ser '001-001-000000001'. ";
										}else if (strNewMSg.contains("SSWH_NoWithholdingTaxIncomeForTheDate")){
											ObjMsg[1]="Msg: No se ha encontrado retención (Renta) para la fecha. ";
										}
										else {
											ObjMsg[1]=objInvoiceLines[1];
										}
										String StrMsgError =  ObjMsg[1].toString() ;
										String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
										Object[] objInvoice2 = executeInsertsOrder(connOrder,strSQL2);
										 
										strMsg[0]= "ERROR";
									    strMsg[1]= StrMsgError;
									    intCountErrors++;
										strMsg[0]="Error";
			
										if (intCountErrors>0) {
										
											strSQL= "delete from c_invoicelinetax where c_invoice_id ='"+ StrInvoiceID +"'";
											objInvoice = executeInsertsOrder(connOrder,strSQL);
											
											strSQL= "delete from c_invoiceline where c_invoice_id ='"+ StrInvoiceID +"'";
											objInvoice = executeInsertsOrder(connOrder,strSQL);
											
											strSQL= "delete from c_invoicetax where c_invoice_id ='"+ StrInvoiceID +"'";
											objInvoice = executeInsertsOrder(connOrder,strSQL);
											
											strSQL= "delete from c_invoice where c_invoice_id ='"+ StrInvoiceID +"'";
											objInvoice = executeInsertsOrder(connOrder,strSQL);
											ObjMsg[0]="Error";
										}
										//return ObjMsg;
										intCountErrors++;
									}
									if (CountErrorLine==0) {
										strSQL= "update imdlv_voucherpurchline set isprocess='Y', logserror = coalesce(logserror,'') || 'Transacción creada satisfactoriamente'  where imdlv_voucherpurchline_id ='"+ collDataline.getId() +"'";
										objInvoice = executeInsertsOrder(connOrder,strSQL);
										strMsg[0]="OK";
									}else {
										
										strSql= "alter table m_inout disable trigger all";
										objInvoice = executeInsertsOrder(connOrder,strSql);
										
										strSql= "alter table c_order disable trigger all";
										objInvoice = executeInsertsOrder(connOrder,strSql);
										
										
										
										strSql= "delete from m_inoutline where m_inout_id ='"+ strInoutID +"'";
										objInvoice = executeInsertsOrder(connOrder,strSql);
										
										strSql= "delete from m_inout where m_inout_id ='"+ strInoutID +"'";
										objInvoice = executeInsertsOrder(connOrder,strSql);
										
										
										strSql= "delete from c_orderlinetax where c_order_id ='"+ strOrderID +"'";
										objInvoice = executeInsertsOrder(connOrder,strSql);
										
										strSql= "delete from c_orderline where c_order_id ='"+ strOrderID +"'";
										objInvoice = executeInsertsOrder(connOrder,strSql);
										
										strSql= "delete from c_ordertax where c_order_id ='"+ strOrderID +"'";
										objInvoice = executeInsertsOrder(connOrder,strSql);
										
										strSql= "delete from c_order where c_order_id ='"+ strOrderID +"'";
										objInvoice = executeInsertsOrder(connOrder,strSql);

										strSql= "alter table m_inout enable trigger all";
										objInvoice = executeInsertsOrder(connOrder,strSql);
										
										strSql= "alter table c_order enable trigger all";
										objInvoice = executeInsertsOrder(connOrder,strSql);
										
										strMsg[0]="Info";
										strMsg[1]="Revise los logs";
										countErrors++;
									}
									
								}
							}else {
								
								if (intCountErrors>0) {
									
									
									strSql= "alter table m_inout disable trigger all";
									Object[] objInvoice = executeInsertsOrder(connOrder,strSql);
									
									strSql= "alter table c_order disable trigger all";
									objInvoice = executeInsertsOrder(connOrder,strSql);
									
									
									
									strSql= "delete from m_inoutline where m_inout_id ='"+ strInoutID +"'";
									objInvoice = executeInsertsOrder(connOrder,strSql);
									
									strSql= "delete from m_inout where m_inout_id ='"+ strInoutID +"'";
									objInvoice = executeInsertsOrder(connOrder,strSql);
									
									
									strSql= "delete from c_orderlinetax where c_order_id ='"+ strOrderID +"'";
									objInvoice = executeInsertsOrder(connOrder,strSql);
									
									strSql= "delete from c_orderline where c_order_id ='"+ strOrderID +"'";
									objInvoice = executeInsertsOrder(connOrder,strSql);
									
									strSql= "delete from c_ordertax where c_order_id ='"+ strOrderID +"'";
									objInvoice = executeInsertsOrder(connOrder,strSql);
									
									strSql= "delete from c_order where c_order_id ='"+ strOrderID +"'";
									objInvoice = executeInsertsOrder(connOrder,strSql);

									strSql= "alter table m_inout enable trigger all";
									objInvoice = executeInsertsOrder(connOrder,strSql);
									
									strSql= "alter table c_order enable trigger all";
									objInvoice = executeInsertsOrder(connOrder,strSql);
									
									
									strMsg[0]="Info";
									strMsg[1]="Revise los logs";
									countErrors++;
								}
							}
							
							
							
						}
					}
					
				}else {
					
					String StrMsgError =  strMsg[1].toString() ;
					String strSQL2= "update imdlv_voucherpurchline set logserror = coalesce(logserror,'') || '"+ StrMsgError +"' where imdlv_voucherpurchline_id ='"+ collDataline.getId()  +"'";
					Object[] objInvoice2 = executeInsertsOrder(connOrder,strSQL2);
					 
					
					strSql= "alter table c_order disable trigger all";
					Object[]  objInvoice = executeInsertsOrder(connOrder,strSql);

					
					strSql= "delete from c_orderlinetax where c_order_id ='"+ strOrderID +"'";
					objInvoice = executeInsertsOrder(connOrder,strSql);
					
					strSql= "delete from c_orderline where c_order_id ='"+ strOrderID +"'";
					objInvoice = executeInsertsOrder(connOrder,strSql);
					
					strSql= "delete from c_ordertax where c_order_id ='"+ strOrderID +"'";
					objInvoice = executeInsertsOrder(connOrder,strSql);
					
					strSql= "delete from c_order where c_order_id ='"+ strOrderID +"'";
					objInvoice = executeInsertsOrder(connOrder,strSql);
					strMsg[0]="Info";
					strMsg[1]="Revise los logs";
					strSql= "alter table c_order enable trigger all";
					objInvoice = executeInsertsOrder(connOrder,strSql);
					countErrors++;

					
					
				}
			}
			
			
			 //OBDal.getInstance().flush();
			
	    }
	    try {
	    	//connOrder.getTransactionConnection().close();
	    	connOrder.getStatement().close();
	    	connOrder.destroy();
	    }catch (Exception e) {
			// TODO: handle exception
		}
	    if (countErrors==0) {
	    	strMsg[0]= "OK";
	    	strMsg[1]= "Success";
	    }else {
	    	strMsg[0]= "Error";
	    	strMsg[1]= "El proceso ha terminado con errores";
	    }
	    
	    return strMsg;
	  }
  
  public Object[] completeFunction(ConnectionProvider cp,String strSQL, String strID){
	  
	  Object[] objMsg= new Object[2];
	  String returnString="";
	  boolean sqlExe=true;
	  try {
			// Se llama a la función para insertar en las líneas de detalle
			//org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
	        CallableStatement InsertLineStatement = null;

	        try {
		        InsertLineStatement  = cp.getConnection()
			              .prepareCall(strSQL); // ORIGINAL
			    InsertLineStatement.setString(1,strID);
	        	InsertLineStatement.execute();
	        }catch(Exception epos) {
	        	objMsg[0]="ERROR";
				
				if (returnString.equals("")) {
					returnString=epos.getMessage().toString();
				}
	        	objMsg[1]=returnString;
	        	sqlExe=false;
	        }
	        
	        if (!sqlExe) {
	        	    
		        	objMsg[0]="ERROR";
		        	objMsg[1]=returnString;
		        	
		        	if (returnString.contains("PeriodNotAvailable")) {
		        		objMsg[1]="Msg: El periódo no se encuentra abierto. ";
		        	}
		        	if (returnString.contains("EEI_Authorization_Ret")) {
		        		objMsg[1]="Msg: La autorización electrónica de la retención es obligatoria. ";
		        	}
		        	 
		        	if (returnString.contains("EEI_Authorization")) {
		        		objMsg[1]="Msg: La autorización electrónica de la factura es obligatoria. ";
		        	}
		        	
		        	if (returnString.contains("NotCorrectOrgDoctypeOrder")) {
		        		objMsg[1]="Msg: La organización del  Tipo Doc.Objetivo  es diferente o no depende de la organización del Pedido. ";
		        	}
		        	cp.getStatement().getConnection().commit();
			        cp.getStatement().close();
			        InsertLineStatement.close();
			        //cp.getTransactionConnection().commit();
		        	//cp.getTransactionConnection().close();
			        InsertLineStatement.close();
			        cp.destroy();
			        cp.getStatement().close();
			        
	        	return objMsg;
	        }
	        ResultSet rs = InsertLineStatement.getResultSet();

	        while (rs.next()) {
	        	returnString=(rs.getString(1));
	        }
	        if (returnString.contains("ERROR")) {
	        	objMsg[0]="ERROR";
	        	objMsg[1]=returnString;
	        	
	        	if (returnString.contains("PeriodNotAvailable")) {
	        		objMsg[1]="Msg: El periódo no se encuentra abierto. ";
	        	}
	        	if (returnString.contains("EEI_Authorization_Ret")) {
	        		objMsg[1]="Msg: La autorización electrónica de la retención es obligatoria. ";
	        	}
	        	 
	        	if (returnString.contains("EEI_Authorization")) {
	        		objMsg[1]="Msg: La autorización electrónica de la factura es obligatoria. ";
	        	}
	        	
	        	if (returnString.contains("NotCorrectOrgDoctypeOrder")) {
	        		objMsg[1]="Msg: La organización del  Tipo Doc.Objetivo  es diferente o no depende de la organización del Pedido. ";
	        	}
	        	 
	        	 
	        }else {
	        	returnString="OK";
	        }
	        if(returnString.contains("OK")) {
	        	objMsg[0]="OK";
	        	objMsg[1]="Sucess";
	        }
	        
	        cp.getStatement().getConnection().commit();
	        cp.getStatement().close();
	        InsertLineStatement.close();
	        cp.destroy();
	        rs.close();
	} catch (Exception e) {
		objMsg[0]="ERROR";
		
		if (returnString.equals("")) {
			returnString=e.getMessage().toString();
		}
    	objMsg[1]=returnString;
	}
	  return objMsg;
  }  
  
  public static String get_Invoice(ConnectionProvider connectionProvider,String strDoc, String strTrx,String strPartnerID) {
	    String strSql = "";
	    strSql = strSql + "  select\n" + 
	    		"	c_invoice_id as new_doc \n" + 
	    		"	from \n" + 
	    		"	(select \n" + 
	    		"	case when coalesce(split_part(trim(documentno),'-',3),'ND')<>'ND' then\n" + 
	    		"	split_part(trim(documentno),'-',1) || '-' ||\n" + 
	    		"	split_part(trim(documentno),'-',2) || '-' ||\n" + 
	    		"	lpad((split_part(trim(documentno),'-',3)),9,'0')\n" + 
	    		"	else\n" + 
	    		"	'' end as new_doc  \n" + 
	    		"	,trim(documentno) as documentno  \n, c_invoice_id" + 
	    		"	from c_invoice  \n" + 
	    		"	where issotrx='" + strTrx + "'" +  
	    		"	and docstatus ='CO'  \n" + 
	    		"   and c_bpartner_id ='" + strPartnerID  + "'"+
	    		"	) rep\n" + 
	    		"	where new_doc='" + strDoc+ "' and (not exists(select 1 from ssws_withholdingsale sws where " + 
	    				" sws.c_invoice_id = rep.c_invoice_id ))" ;

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
	        strReturn = UtilSql.getValue(result, "new_doc");
	      }
	      try {
	    	  connectionProvider.getStatement().close();
	    	  connectionProvider.destroy();
	      }catch(Exception ee) {
	    	  
	      }
	      result.close();
	      st.close();
	    } catch (SQLException e) {
	      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
	      
	    }
	    
	    if (strReturn==null) {
	    	strReturn="ND";
	    	
	    }
	    return (strReturn);
	  }
  
  
  public static String get_InvoiceExists(ConnectionProvider connectionProvider,String strDoc, String strTrx, String strPartnerID) {
	    String strSql = "";
	    strSql = strSql + "  select\n" + 
	    		"	c_invoice_id as new_doc \n" + 
	    		"	from \n" + 
	    		"	(select \n" + 
	    		"	case when coalesce(split_part(trim(documentno),'-',3),'ND')<>'ND' then\n" + 
	    		"	split_part(trim(documentno),'-',1) || '-' ||\n" + 
	    		"	split_part(trim(documentno),'-',2) || '-' ||\n" + 
	    		"	lpad((split_part(trim(documentno),'-',3)),9,'0')\n" + 
	    		"	else\n" + 
	    		"	'' end as new_doc  \n" + 
	    		"	,trim(documentno) as documentno  \n, c_invoice_id" + 
	    		"	from c_invoice  \n" + 
	    		"	where issotrx='" + strTrx + "'" +  
	    		"	and docstatus ='CO'  \n" + 
	    		"   and c_bpartner_id ='" + strPartnerID  + "'"+
	    		"	) rep\n" + 
	    		"	where new_doc='" + strDoc+ "'" ;

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
	        strReturn = UtilSql.getValue(result, "new_doc");
	      }
	      try {
	    	  connectionProvider.getStatement().close();
	    	  connectionProvider.destroy();
	      }catch(Exception ee) {
	    	  
	      }
	      result.close();
	      st.close();
	    } catch (SQLException e) {
	      // log4j.error("SQL error in query: " + strSql + "Exception:"+ e);
	      
	    }
	    
	    if (strReturn==null) {
	    	strReturn="ND";
	    	
	    }
	    return (strReturn);
	  }

 
}
