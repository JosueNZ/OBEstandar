// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 04/12/2013 15:59:06
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   KeyTool.java

package ec.cusoft.facturaec.utils;

import es.mityc.javasign.i18n.I18nFactory;
import es.mityc.javasign.i18n.II18nManager;
import es.mityc.javasign.pkstore.CertStoreException;
import es.mityc.javasign.pkstore.IPassStoreKS;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class KeyTool
{

	public static List getCertificatesWithKeys(KeyStore ks)
			throws CertStoreException
			{
		try
		{
			Enumeration total = ks.aliases();
			ArrayList lista = new ArrayList();
			do
			{
				if(!total.hasMoreElements())
					break;
				String alias = (String)total.nextElement();
				if(ks.isKeyEntry(alias))
				{
					Certificate cert = ks.getCertificate(alias);
					if(cert instanceof X509Certificate)
						lista.add((X509Certificate)cert);
				}
			} while(true);
			return lista;
		}
		catch(KeyStoreException ex)
		{
			throw new CertStoreException(I18N.getLocalMessage("i18n.mityc.cert.ks.1", new Object[] {
					ks.getType(), ex.getMessage(), ex
			}));
		}
			}

	public static PrivateKey findPrivateKey(KeyStore ks, X509Certificate certificate, IPassStoreKS passHandler, char nullPassword[])
			throws CertStoreException
			{
		try
		{
			String alias;
			label0:
			{
				alias = ks.getCertificateAlias(certificate);
				if(ks.isKeyEntry(alias))
					break label0;
				try
				{
					Enumeration total = ks.aliases();
					String keyAlias;
					Certificate cert;
					do
					{
						do
						{
							if(!total.hasMoreElements())
								break label0;
							keyAlias = (String)total.nextElement();
						} while(!ks.isKeyEntry(keyAlias));
						cert = ks.getCertificate(keyAlias);
					} while(!(cert instanceof X509Certificate) || !cert.equals(certificate));
					alias = keyAlias;
				}
				catch(KeyStoreException ex)
				{
					throw new CertStoreException(I18N.getLocalMessage("i18n.mityc.cert.ks.1", new Object[] {
							ex.getMessage(), ex
					}));
				}
			}
			if(alias == null)
				throw new CertStoreException(I18N.getLocalMessage("i18n.mityc.cert.ks.6"));
			PrivateKey resultado = null;
			try
			{
				resultado = (PrivateKey)ks.getKey(alias, nullPassword);
			}
			catch(UnrecoverableKeyException e)
			{
				char passwd[] = passHandler.getPassword(certificate, alias);
				resultado = (PrivateKey)ks.getKey(alias, passwd);
			}
			if(resultado == null)
				throw new CertStoreException(I18N.getLocalMessage("i18n.mityc.cert.ks.5"));
			else
				return resultado;
		}
		catch(KeyStoreException ex)
		{
			throw new CertStoreException(I18N.getLocalMessage("i18n.mityc.cert.ks.1", new Object[] {
					ex.getMessage(), ex
			}));
		}
		catch(NoSuchAlgorithmException ex)
		{
			throw new CertStoreException(I18N.getLocalMessage("i18n.mityc.cert.ks.2", new Object[] {
					ex.getMessage(), ex
			}));
		}
		catch(UnrecoverableKeyException ex)
		{
			throw new CertStoreException(I18N.getLocalMessage("i18n.mityc.cert.ks.4", new Object[] {
					ex.getMessage()
			}));
		}
			}

	public static PrivateKey findPrivateKey(KeyStore ks, X509Certificate certificate, IPassStoreKS passHandler)
			throws CertStoreException
			{
		return findPrivateKey(ks, certificate, passHandler, EMPTY_STRING);
			}

	public static List getTrustCertificates(KeyStore ks)
			throws CertStoreException
			{
		try
		{
			Enumeration total = ks.aliases();
			ArrayList lista = new ArrayList();
			do
			{
				if(!total.hasMoreElements())
					break;
				String alias = (String)total.nextElement();
				if(ks.isCertificateEntry(alias) && !ks.isKeyEntry(alias))
				{
					Certificate cert = ks.getCertificate(alias);
					if(cert instanceof X509Certificate)
						lista.add((X509Certificate)cert);
				}
			} while(true);
			return lista;
		}
		catch(KeyStoreException ex)
		{
			throw new CertStoreException(I18N.getLocalMessage("i18n.mityc.cert.ks.1", new Object[] {
					ex.getMessage(), ex
			}));
		}
			}

	private KeyTool()
	{
	}

	private static final II18nManager I18N = I18nFactory.getI18nManager("MITyCLibCert");
	static final char EMPTY_STRING[] = "".toCharArray();

}
