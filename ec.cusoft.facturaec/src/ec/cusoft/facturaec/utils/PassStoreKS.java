// Decompiled by DJ v3.11.11.95 Copyright 2009 Atanas Neshkov  Date: 04/12/2013 15:02:01
// Home Page: http://members.fortunecity.com/neshkov/dj.html  http://www.neshkov.com/dj.html - Check often for new version!
// Decompiler options: packimports(3) 
// Source File Name:   PassStoreKS.java

package ec.cusoft.facturaec.utils;

import es.mityc.javasign.pkstore.IPassStoreKS;
import java.security.cert.X509Certificate;

public class PassStoreKS
    implements IPassStoreKS
{

    public PassStoreKS(String pass)
    {
        password = new String(pass);
    }

    public char[] getPassword(X509Certificate certificate, String alias)
    {
        return password.toCharArray();
    }

    private transient String password;
}
