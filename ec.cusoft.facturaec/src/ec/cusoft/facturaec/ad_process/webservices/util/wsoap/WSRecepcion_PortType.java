/**
 * WSRecepcion_PortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ec.cusoft.facturaec.ad_process.webservices.util.wsoap;

public interface WSRecepcion_PortType extends java.rmi.Remote {
  public java.lang.String generar(java.lang.String xml, java.lang.String subtotal12,
      java.lang.String subtotal0, java.lang.String subtotalnoiva, java.lang.String subtotalexento,
      java.lang.String iva12, java.lang.String irbpnr, java.lang.String clidireccion,
      java.lang.String clitelefono, java.lang.String clicorreo, int intTimeOutPara) throws java.rmi.RemoteException;
}
