package ec.cusoft.facturaec.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;

import javax.security.cert.CertificateException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;


import es.mityc.javasign.pkstore.CertStoreException;
import es.mityc.javasign.pkstore.IPKStoreManager;
import es.mityc.javasign.pkstore.keystore.KSStore;

public class CryptUtils {
	private static String newline = System.getProperty("line.separator");
	public static String algorithm = "SHA1WithRSA";

	public static String toBase64(String url_certificado){
		String sFinal="";	
		try {			 
			//este metodo lee el archivo de llave privada y regresa los bytes del mismo. es una simple lectura completa del archivo  	    
			java.io.File file_cer=new java.io.File(url_certificado); FileInputStream in_cer;
			in_cer = new FileInputStream(file_cer);
			byte[] certificado=new byte[(int)file_cer.length()];

			in_cer.read(certificado);
			BASE64Encoder enc=new BASE64Encoder();
			sFinal=new String(enc.encode(certificado).getBytes("UTF-8"));		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		sFinal=sFinal.replace(newline, "");
		return sFinal; 
	}
	public static String toBase64(byte[] certificado){
		String sFinal="";	
		try {			 
			//este metodo lee el archivo de llave privada y regresa los bytes del mismo. es una simple lectura completa del archivo  	    
			BASE64Encoder enc=new BASE64Encoder();
			sFinal=new String(enc.encode(certificado).getBytes("UTF-8"));		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

		sFinal=sFinal.replace(newline, "");
		return sFinal; 
	}

	public static String getBase64StringFromCerFile(String filePath)
			throws IOException
			{
		File file = new File(filePath);
		InputStream is = new FileInputStream(file);
		long length = file.length();
		if(length <= 0x7fffffffL);
		byte bytes[] = new byte[(int)length];
		int offset = 0;
		for(int numRead = 0; offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0; offset += numRead);
		if(offset < bytes.length)
		{
			throw new IOException((new StringBuilder()).append("Could not completely read file ").append(file.getName()).toString());
		} else
		{
			is.close();
			BASE64Encoder enc=new BASE64Encoder();
			return new String(enc.encode(bytes).getBytes("UTF-8"));
			//return Base64.encodeBase64String(bytes);
		}
			}

	public static String encryptSHA1WithRSA_old(String url_llave, String url_certificado, String password) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, UnrecoverableKeyException, java.security.cert.CertificateException, FileNotFoundException, IOException{
		String sFinal="";
		byte[] mensaje;

		try {			 
			//este metodo lee el archivo de llave privada y regresa los bytes del mismo. es una simple lectura completa del archivo
			byte[] llavePrivada=null;
			if(url_llave!=null){
				java.io.File file_key=new java.io.File(url_llave); 
				FileInputStream in_key = new FileInputStream(file_key);
				llavePrivada=new byte[(int)file_key.length()];
				in_key.read(llavePrivada);
			}

			byte[] cert=CryptUtils.readCertificado(url_certificado);

			//Pruebas de traceo innecesarias funcionalmente con el MD5
			String md5;
			mensaje = cert;  	 
			//ya obtuvimos la llave privada. Ahora es cuando viene la clase PKCS8Key del famoso commom-ssl.jar de apache 
			//la cual se le pasan los bytes leidos del archivo de la llave privada y un array de chars del password. 
			//Esta clase además nos evita tener que saber cual formato se utilizo para la llave privada ya que lo detecta automático.	  
			org.apache.commons.ssl.PKCS8Key pkcs8;
			try {
				pkcs8 = new org.apache.commons.ssl.PKCS8Key(llavePrivada, password.toCharArray());	
				//obtenemos la llave privada
				PrivateKey privateKey = pkcs8.getPrivateKey();
				//creamos un firmador con el algortimo RSA, que es el que utiliza el SAT. Aqui se digesta a MD5 y se encripta a RSA en un solo proceso,	 
				String algorithm=privateKey.getAlgorithm();
				//java.security.Signature rsa = java.security.Signature.getInstance("MD5WithRSA");
				java.security.Signature rsa = java.security.Signature.getInstance("SHA1WithRSA");
				//le indicamos la llave privada a utilizar
				rsa.initSign(privateKey);
				//le indicamos el mensaje a firmar
				rsa.update(mensaje);
				//obtenemos la firma digital
				byte[] firmaDigital = rsa.sign();	  
				/*javax.security.cert.X509Certificate cert;

	  		try {
	  			cert = javax.security.cert.X509Certificate.getInstance(llavePublica);
	  			cert.checkValidity();
	  			rsa.initVerify(cert.getPublicKey());
	  		} catch (CertificateException e) {
	  			// TODO Auto-generated catch block
	  			e.printStackTrace();			
	  	  if (rsa.verify(firmaDigital)) {*/
				//convertimos la salida de firma digital a base64.
				BASE64Encoder enc=new BASE64Encoder();
				sFinal= new String(enc.encode(firmaDigital).getBytes("UTF-8"));
				//System.out.print(sFinal);		  
			} catch (GeneralSecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		return sFinal; 
	}


	public static String encryptSHA1WithRSA(Object[] kc) throws NoSuchAlgorithmException, CertificateException, KeyStoreException, UnrecoverableKeyException, java.security.cert.CertificateException, FileNotFoundException, IOException{
		String sFinal="";
		byte[] mensaje = ((X509Certificate)kc[0]).getSignature();  	 
		try {
			java.security.Signature rsa = java.security.Signature.getInstance(algorithm);
			//le indicamos la llave privada a utilizar
			rsa.initSign((PrivateKey)kc[1]);
			//le indicamos el mensaje a firmar
			rsa.update(mensaje);
			//obtenemos la firma digital
			byte[] firmaDigital = rsa.sign();	  
			//convertimos la salida de firma digital a base64.
			BASE64Encoder enc=new BASE64Encoder();
			sFinal= new String(enc.encode(firmaDigital).getBytes("UTF-8"));

		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	

		return sFinal; 
	}	

	@SuppressWarnings("rawtypes")
	public static Object[] getPrivatekeyAndCertificate(String ruta, String password) throws KeyStoreException, NoSuchAlgorithmException, java.security.cert.CertificateException, FileNotFoundException, IOException, UnrecoverableKeyException, CertStoreException{
		KeyStore ks;
		java.security.cert.X509Certificate certificate=null;
		PrivateKey privateKey=null;
		java.security.Provider Provider=null;
		String alias;
		ks = KeyStore.getInstance("PKCS12");
		ks.load(new BufferedInputStream(new java.io.FileInputStream(ruta)), password.toCharArray());
		IPKStoreManager storeManager = new KSStore(ks, new PassStoreKS(password));
		List certificates = storeManager.getSignCertificates();
		if(certificates.size() >= 1)
		{
			certificate = (X509Certificate)certificates.get(0);
			privateKey = storeManager.getPrivateKey(certificate);
			Provider = storeManager.getProvider(certificate);

		}
		/*for (Enumeration aliasEnum = ks.aliases(); aliasEnum.hasMoreElements();) {
	  		  alias = (String) aliasEnum.nextElement();
	  		  if (ks.isKeyEntry(alias)) {
	  			  certificate = (java.security.cert.X509Certificate) ks.getCertificate(alias);
	  			  privateKey = (PrivateKey) ks.getKey(alias, password.toCharArray());
	  			  Provider= storeManager.getProvider(certificate);
	  			break;
	  		}
	  	  }*/
		Object[] kc = new Object[3];
		kc[0]=certificate;
		kc[1]=privateKey;
		kc[2]=Provider;
		return kc;
	}


	public static String digestToSHA1(String texto) throws NoSuchAlgorithmException, FileNotFoundException, UnsupportedEncodingException {    	

		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		byte[] result = mDigest.digest(texto.getBytes("UTF-8"));
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
		}        
		return sb.toString();
	}

	public static String digestToSHA1(byte[] texto) throws NoSuchAlgorithmException, FileNotFoundException {    	

		MessageDigest mDigest = MessageDigest.getInstance("SHA1");
		byte[] result = mDigest.digest(texto);
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
		}        
		return sb.toString();
	}

	public static String digestCertificado(String url) throws NoSuchAlgorithmException, IOException{
		String sFinal=new String(readCertificado(url));
		String digestCert = digestToSHA1(sFinal);  		
		return digestCert;
	}

	public static String digestCertificado(byte[] c) throws NoSuchAlgorithmException, IOException{
		String digestCert = digestToSHA1(c);  		
		return digestCert;
	}

	public static byte[] readCertificado(String url) throws NoSuchAlgorithmException, IOException{
		java.io.File file_cer=new java.io.File(url); 
		FileInputStream in_cer = new FileInputStream(file_cer); 
		byte[] cert=new byte[(int)file_cer.length()];
		in_cer.read(cert);

		return cert;
	}
	//format the XML in your Stringpublic 
	@SuppressWarnings("deprecation")
	public static String formatXML(String unformattedXml) {    
		try {        
			Document document = FileUtils.parseXmlFile(unformattedXml);        
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


}

