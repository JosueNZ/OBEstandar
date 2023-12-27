/**
 * WSRecepcion_ServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ec.cusoft.facturaec.ad_process.webservices.util.wsoap;

public class WSRecepcion_ServiceLocator extends org.apache.axis.client.Service
    implements ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_Service {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public WSRecepcion_ServiceLocator() {
  }

  public WSRecepcion_ServiceLocator(org.apache.axis.EngineConfiguration config) {
    super(config);
  }

  public WSRecepcion_ServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName)
      throws javax.xml.rpc.ServiceException {
    super(wsdlLoc, sName);
  }

  // Use to get a proxy class for WSRecepcionPort
  private java.lang.String WSRecepcionPort_address = null;

  public void setUrl(String Url) {
    WSRecepcionPort_address = Url;
  }

  public java.lang.String getWSRecepcionPortAddress() {
    return WSRecepcionPort_address;
  }

  // The WSDD service name defaults to the port name.
  private java.lang.String WSRecepcionPortWSDDServiceName = "WSRecepcionPort";

  public java.lang.String getWSRecepcionPortWSDDServiceName() {
    return WSRecepcionPortWSDDServiceName;
  }

  public void setWSRecepcionPortWSDDServiceName(java.lang.String name) {
    WSRecepcionPortWSDDServiceName = name;
  }

  public ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_PortType getWSRecepcionPort()
      throws javax.xml.rpc.ServiceException {
    java.net.URL endpoint;
    try {
      endpoint = new java.net.URL(WSRecepcionPort_address);
    } catch (java.net.MalformedURLException e) {
      throw new javax.xml.rpc.ServiceException(e);
    }
    return getWSRecepcionPort(endpoint);
  }

  public ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_PortType getWSRecepcionPort(
      java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
    try {
      ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcionPortBindingStub _stub = new ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcionPortBindingStub(
          portAddress, this);
      _stub.setPortName(getWSRecepcionPortWSDDServiceName());
      return _stub;
    } catch (org.apache.axis.AxisFault e) {
      return null;
    }
  }

  public void setWSRecepcionPortEndpointAddress(java.lang.String address) {
    WSRecepcionPort_address = address;
  }

  /**
   * For the given interface, get the stub implementation. If this service has no port for the given
   * interface, then ServiceException is thrown.
   */
  public java.rmi.Remote getPort(Class serviceEndpointInterface)
      throws javax.xml.rpc.ServiceException {
    try {
      if (ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_PortType.class
          .isAssignableFrom(serviceEndpointInterface)) {
        ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcionPortBindingStub _stub = new ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcionPortBindingStub(
            new java.net.URL(WSRecepcionPort_address), this);
        _stub.setPortName(getWSRecepcionPortWSDDServiceName());
        return _stub;
      }
    } catch (java.lang.Throwable t) {
      throw new javax.xml.rpc.ServiceException(t);
    }
    throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  "
        + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
  }

  /**
   * For the given interface, get the stub implementation. If this service has no port for the given
   * interface, then ServiceException is thrown.
   */
  public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface)
      throws javax.xml.rpc.ServiceException {
    if (portName == null) {
      return getPort(serviceEndpointInterface);
    }
    java.lang.String inputPortName = portName.getLocalPart();
    if ("WSRecepcionPort".equals(inputPortName)) {
      return getWSRecepcionPort();
    } else {
      java.rmi.Remote _stub = getPort(serviceEndpointInterface);
      ((org.apache.axis.client.Stub) _stub).setPortName(portName);
      return _stub;
    }
  }

  public javax.xml.namespace.QName getServiceName() {
    return new javax.xml.namespace.QName("http://WebService/", "WSRecepcion");
  }

  private java.util.HashSet ports = null;

  public java.util.Iterator getPorts() {
    if (ports == null) {
      ports = new java.util.HashSet();
      ports.add(new javax.xml.namespace.QName("http://WebService/", "WSRecepcionPort"));
    }
    return ports.iterator();
  }

  /**
   * Set the endpoint address for the specified port name.
   */
  public void setEndpointAddress(java.lang.String portName, java.lang.String address)
      throws javax.xml.rpc.ServiceException {

    if ("WSRecepcionPort".equals(portName)) {
      setWSRecepcionPortEndpointAddress(address);
    } else { // Unknown Port Name
      throw new javax.xml.rpc.ServiceException(
          " Cannot set Endpoint Address for Unknown Port" + portName);
    }
  }

  /**
   * Set the endpoint address for the specified port name.
   */
  public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address)
      throws javax.xml.rpc.ServiceException {
    setEndpointAddress(portName.getLocalPart(), address);
  }

}
