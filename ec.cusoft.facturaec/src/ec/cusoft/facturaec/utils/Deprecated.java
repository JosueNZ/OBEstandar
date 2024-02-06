package ec.cusoft.facturaec.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.security.cert.CertificateException;
import javax.servlet.ServletException;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.openbravo.utils.FormatUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ec.cusoft.facturaec.EEIParamFacturae;

import es.mityc.javasign.pkstore.CertStoreException;

public class Deprecated {

	private static String newline = System.getProperty("line.separator");

	public static Document firmar(Document comprobante, EEIParamFacturae param) throws ParserConfigurationException, NoSuchAlgorithmException, TransformerConfigurationException, TransformerException, IOException, DatatypeConfigurationException, InstantiationException, IllegalAccessException, ClassNotFoundException, CertificateException, ServletException, UnrecoverableKeyException, KeyStoreException, java.security.cert.CertificateException, CertStoreException{
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		Object[] key_cert = CryptUtils.getPrivatekeyAndCertificate(param.getDIRCertificado(), FormatUtilities.encryptDecrypt(param.getPassword(), false));


		// root elements
		Document doc = docBuilder.newDocument();
		doc.setXmlStandalone(true);
		Element rootElement = doc.createElement("ds:Signature");
		rootElement.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
		rootElement.setAttribute("xmlns:etsi", "http://uri.etsi.org/01903/v1.3.2#");
		rootElement.setAttribute("Id", "Signature620397");
		doc.appendChild(rootElement);

		Element signedInfo = doc.createElement("ds:SignedInfo");
		signedInfo.setAttribute("Id", "Signature-SignedInfo814463");

		Element canonicalizationMethod = doc.createElement("ds:CanonicalizationMethod");
		canonicalizationMethod .setAttribute("Algorithm", "http://www.w3.org/TR/2001/REC-xml-c14n-20010315");
		canonicalizationMethod.setTextContent("");
		signedInfo.appendChild(canonicalizationMethod); 

		Element signatureMethod = doc.createElement("ds:SignatureMethod");
		signatureMethod .setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#rsa-sha1");
		signatureMethod.setTextContent("");
		signedInfo.appendChild(signatureMethod);

		Element reference = doc.createElement("ds:Reference");
		reference.setAttribute("Id", "SignedPropertiesID157683");
		reference.setAttribute("Type", "http://uri.etsi.org/01903#SignedProperties");
		reference.setAttribute("URI", "#Signature620397-SignedProperties24123");

		Element digestMethod = doc.createElement("ds:DigestMethod");
		digestMethod.setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#sha1");
		reference.appendChild(digestMethod);

		Element digestValue = doc.createElement("ds:DigestValue");
		digestValue.setTextContent("HASH O DIGEST DEL ELEMENTO etsi:SignedProperties");//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		reference.appendChild(digestValue);
		signedInfo.appendChild(reference);

		Element reference2 = doc.createElement("ds:Reference");
		reference2.setAttribute("URI", "#Certificate1562780");

		Element digestMethod2 = doc.createElement("ds:DigestMethod");
		digestMethod2.setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#sha1");
		reference2.appendChild(digestMethod2);

		Element digestValue2 = doc.createElement("ds:DigestValue");
		String certSHA1 = CryptUtils.digestCertificado(((java.security.cert.X509Certificate)key_cert[0]).getEncoded());//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		digestValue2.setTextContent(certSHA1);
		reference2.appendChild(digestValue2);
		signedInfo.appendChild(reference2);

		Element reference3 = doc.createElement("ds:Reference");
		reference3.setAttribute("Id", "Reference-ID-363558");
		reference3.setAttribute("URI", "#comprobante");

		Element transforms = doc.createElement("ds:Transforms");

		Element transform = doc.createElement("ds:Transform");
		transform.setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#enveloped-signature");
		transforms.appendChild(transform);			        
		reference3.appendChild(transforms);

		Element digestMethod3 = doc.createElement("ds:DigestMethod");
		digestMethod3.setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#sha1");
		reference3.appendChild(digestMethod3);

		Element digestValue3 = doc.createElement("ds:DigestValue");
		String sComprobante = DOMUtils.getStringFromDocument(comprobante);
		sComprobante = sComprobante.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
		String digestComprobante = CryptUtils.digestToSHA1(sComprobante);
		//digestValue3.setTextContent("HASH O DIGEST DE TODO EL ARCHIVO XML IDENTIFICADO POR EL id=comprobante");
		digestValue3.setTextContent(digestComprobante);//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		reference3.appendChild(digestValue3);
		signedInfo.appendChild(reference3);
		rootElement.appendChild(signedInfo);

		Element signatureValue = doc.createElement("ds:SignatureValue");
		signatureValue.setAttribute("Id", "SignatureValue398963");
		String certSHA1WithRSA=null;
		try {
			certSHA1WithRSA = CryptUtils.encryptSHA1WithRSA(key_cert).replaceAll(newline, "");
		} catch (UnrecoverableKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (java.security.cert.CertificateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//signatureValue.setTextContent("VALOR DE LA FIRMA (ENCRIPTADO CON LA LLAVE PRIVADA DEL CERTIFICADO DIGITAL)");
		signatureValue.setTextContent(certSHA1WithRSA); ////!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		rootElement.appendChild(signatureValue);

		Element keyInfo = doc.createElement("ds:KeyInfo");
		keyInfo.setAttribute("Id", "Certificate1562780");

		Element X509Data = doc.createElement("ds:X509Data");

		Element X509Certificate = doc.createElement("ds:X509Certificate");
		String certBase64 = CryptUtils.toBase64(((java.security.cert.X509Certificate)key_cert[0]).getEncoded());//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		X509Certificate.setTextContent(certBase64);
		X509Data.appendChild(X509Certificate);	        
		keyInfo.appendChild(X509Data);

		Element KeyValue = doc.createElement("ds:KeyValue");

		Element RSAKeyValue = doc.createElement("ds:RSAKeyValue");

		Element modulus = doc.createElement("ds:Modulus");	

		//byte[] bcert = CryptUtils.readCertificado(param.getDIRCertificado());
		//javax.security.cert.X509Certificate cert2 = javax.security.cert.X509Certificate.getInstance(bcert);
		java.security.cert.X509Certificate cert2 = (java.security.cert.X509Certificate)key_cert[0];
		String[] sArray = Utils.extractByCharInArray(cert2.toString(), newline);
		String mod=null;
		for (int i = 0; i < sArray.length; i++)
			if(sArray[i].contains("modulus")){
				String sModulus = sArray[i].trim();
				mod = Utils.extractByCharInArray(sModulus, ": ")[1].trim();
				break;
			}	                

		modulus.setTextContent((mod!=null)?mod:"MODULO DEL CERTIFICADO X509");//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!1
		RSAKeyValue.appendChild(modulus);

		Element exponent = doc.createElement("ds:Exponent");	
		exponent.setTextContent("AQAB");
		RSAKeyValue.appendChild(exponent);

		KeyValue.appendChild(RSAKeyValue);

		keyInfo.appendChild(KeyValue);        	

		rootElement.appendChild(keyInfo);

		//Falta de Object para abajo
		Element object = doc.createElement("ds:Object");
		object.setAttribute("Id", "Signature620397-Object231987");

		Element qualifyingProperties = doc.createElement("etsi:QualifyingProperties");
		qualifyingProperties.setAttribute("Target", "#Signature620397");

		Element signedProperties = doc.createElement("etsi:SignedProperties");
		signedProperties.setAttribute("Id", "Signature620397-SignedProperties24123");

		Element signedSignatureProperties = doc.createElement("etsi:SignedSignatureProperties");

		Element signingTime = doc.createElement("etsi:SigningTime");

		Calendar cal = Calendar.getInstance();	  
		XMLGregorianCalendar fecha = FechaUtils.CalendarToXml(cal);
		String sFecha = fecha.toString();
		//signingTime.setTextContent("2012-03-05T16:57:32-05:00");
		signingTime.setTextContent(sFecha);//!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		signedSignatureProperties.appendChild(signingTime);

		Element SigningCertificate = doc.createElement("etsi:SigningCertificate");

		Element cert = doc.createElement("etsi:Cert");

		Element certDigest = doc.createElement("etsi:CertDigest");

		Element digestMethodO = doc.createElement("ds:DigestMethod");
		digestMethodO.setAttribute("Algorithm", "http://www.w3.org/2000/09/xmldsig#sha1");
		certDigest.appendChild(digestMethodO);

		Element digestValueO = doc.createElement("ds:DigestValue");
		digestValueO.setTextContent("xUQewsj7MrjSfyMnhWz5DhQnWJM=");//?????????????????????????????????????????????????????????
		certDigest.appendChild(digestValueO);
		cert.appendChild(certDigest);

		Element issuerSerial = doc.createElement("etsi:IssuerSerial");

		Element X509IssuerName = doc.createElement("ds:X509IssuerName");
		X509IssuerName.setTextContent("CN=AC BANCO CENTRAL DEL ECUADOR,L=QUITO,OU=ENTIDAD DE CERTIFICACION DE INFORMACION-ECIBCE,O=BANCO CENTRAL DEL ECUADOR,C=EC");//??????????????????????????????????????
		issuerSerial.appendChild(X509IssuerName);

		Element X509SerialNumber = doc.createElement("ds:X509SerialNumber");
		X509SerialNumber.setTextContent("1312833444");//?????????????????????????????????????????????????????????????????????????
		issuerSerial.appendChild(X509SerialNumber);
		cert.appendChild(issuerSerial);

		SigningCertificate.appendChild(cert);
		signedSignatureProperties.appendChild(SigningCertificate);
		signedProperties.appendChild(signedSignatureProperties);

		Element signedDataObjectProperties = doc.createElement("etsi:SignedDataObjectProperties");

		Element dataObjectFormat = doc.createElement("etsi:DataObjectFormat");
		dataObjectFormat.setAttribute("ObjectReference", "#Reference-ID-363558");			        	

		Element description = doc.createElement("etsi:Description");
		description.setTextContent("contenido comprobante");//???????????????????????????????????????????????????????????????????????
		dataObjectFormat.appendChild(description);

		Element mimeType = doc.createElement("etsi:MimeType");
		mimeType.setTextContent("text/xml");//????????????????????????????????????????????????????????????????????????????????????
		dataObjectFormat.appendChild(mimeType);
		signedDataObjectProperties.appendChild(dataObjectFormat);
		signedProperties.appendChild(signedDataObjectProperties);

		qualifyingProperties.appendChild(signedProperties);

		//actualizar nodo DigestValue con el SignedProperties
		String sSignedProperties = DOMUtils.ElementToString(signedProperties);
		String hashSignedProperties = CryptUtils.digestToSHA1(sSignedProperties);
		digestValue.setTextContent(hashSignedProperties);


		object.appendChild(qualifyingProperties);

		rootElement.appendChild(object);

		return doc;

	}

	public Document firmar2() throws javax.xml.crypto.MarshalException, java.security.cert.CertificateException, CertificateException{
		String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");
		Document firma = null;
		try {

			XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());
			DigestMethod digestMethod = factory.newDigestMethod(DigestMethod.SHA1, null);
			Transform transform = factory.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null);
			Reference reference = factory.newReference("", digestMethod, Collections.singletonList(transform), null, null);
			CanonicalizationMethod canonicalizationMethod = factory.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null);
			SignatureMethod signatureMethod = factory.newSignatureMethod(SignatureMethod.RSA_SHA1, null);
			SignedInfo signedInfo = factory.newSignedInfo(canonicalizationMethod, signatureMethod, Collections.singletonList(reference));

			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(512);
			KeyPair keyPair = kpg.generateKeyPair();
			KeyInfoFactory keyInfoFactory = factory.getKeyInfoFactory();

			byte[] bcert = CryptUtils.readCertificado("C:\\Certificados\\00001000000104671238.cer");
			javax.security.cert.X509Certificate cert = javax.security.cert.X509Certificate.getInstance(bcert);             

			KeyValue keyValue = keyInfoFactory.newKeyValue(cert.getPublicKey());


			List x509 = new ArrayList();

			x509.add(cert);
			X509Data x509Data = keyInfoFactory.newX509Data(x509);
			List items = new ArrayList();

			items.add(x509Data);
			items.add(keyValue);
			KeyInfo keyInfo = keyInfoFactory.newKeyInfo(items);

			DocumentBuilderFactory dbf =
					DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			Document doc =
					dbf.newDocumentBuilder().
					parse(new FileInputStream("C:\\XmlFile.xml"));

			DOMSignContext dsc = new DOMSignContext(keyPair.getPrivate(), doc.getDocumentElement());

			XMLSignature signature = factory.newXMLSignature(signedInfo, keyInfo);
			signature.sign(dsc);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			firma = (doc);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MarshalException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLSignatureException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} /*catch (KeyStoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (java.security.cert.CertificateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }*/

		return firma;

	}
}
