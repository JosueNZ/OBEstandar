// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 04/12/2013 15:57:07
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   KSStore.java

package ec.cusoft.facturaec.utils;

import es.mityc.javasign.pkstore.*;
import java.security.*;
import java.security.cert.CertPath;
import java.security.cert.X509Certificate;
import java.util.List;

// Referenced classes of package es.mityc.javasign.pkstore.keystore:
//            KeyTool

public class KSStore
   implements IPKStoreManager
{

    public KSStore(KeyStore keystore, IPassStoreKS passwordHandler)
    {
        nullPassword = KeyTool.EMPTY_STRING;
        ks = keystore;
        passHandler = passwordHandler;
        provider = keystore.getProvider();
    }

    public KSStore(KeyStore keystore, IPassStoreKS passwordHandler, char nullpass[])
    {
        nullPassword = KeyTool.EMPTY_STRING;
        ks = keystore;
        passHandler = passwordHandler;
        provider = keystore.getProvider();
        nullPassword = nullpass;
    }

    public KSStore(KeyStore keystore, Provider specificProvider, IPassStoreKS passwordHandler)
    {
        nullPassword = KeyTool.EMPTY_STRING;
        ks = keystore;
        passHandler = passwordHandler;
        provider = specificProvider;
    }

    public KSStore(KeyStore keystore, Provider specificProvider, IPassStoreKS passwordHandler, char nullpass[])
    {
        nullPassword = KeyTool.EMPTY_STRING;
        ks = keystore;
        passHandler = passwordHandler;
        provider = specificProvider;
        nullPassword = nullpass;
    }

    public CertPath getCertPath(X509Certificate certificate)
        throws CertStoreException
    {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public PrivateKey getPrivateKey(X509Certificate certificate)
        throws CertStoreException
    {
        return KeyTool.findPrivateKey(ks, certificate, passHandler, nullPassword);
    }

    public Provider getProvider(X509Certificate cert)
    {
        return provider;
    }

    public List getSignCertificates()
        throws CertStoreException
    {
        return KeyTool.getCertificatesWithKeys(ks);
    }

    public List getTrustCertificates()
        throws CertStoreException
    {
        return KeyTool.getTrustCertificates(ks);
    }

    private KeyStore ks;
    private IPassStoreKS passHandler;
    private Provider provider;
    private char nullPassword[];
}
