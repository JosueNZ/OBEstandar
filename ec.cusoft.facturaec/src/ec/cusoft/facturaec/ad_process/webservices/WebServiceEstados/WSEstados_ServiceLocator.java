/**
 * WSEstados_ServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ec.cusoft.facturaec.ad_process.webservices.WebServiceEstados;

public class WSEstados_ServiceLocator extends org.apache.axis.client.Service implements WSEstados_Service {

    public WSEstados_ServiceLocator() {
    }


    public WSEstados_ServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public WSEstados_ServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for WSEstadosPort
    private java.lang.String WSEstadosPort_address = null;
    
    public void setUrl(String Url) {
    	WSEstadosPort_address = Url;
    }        

    public java.lang.String getWSEstadosPortAddress() {
        return WSEstadosPort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String WSEstadosPortWSDDServiceName = "WSEstadosPort";


    public java.lang.String getWSEstadosPortWSDDServiceName() {
        return WSEstadosPortWSDDServiceName;
    }

    public void setWSEstadosPortWSDDServiceName(java.lang.String name) {
        WSEstadosPortWSDDServiceName = name;
    }

    public WSEstados_PortType getWSEstadosPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(WSEstadosPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getWSEstadosPort(endpoint);
    }

    public WSEstados_PortType getWSEstadosPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            WSEstadosPortBindingStub _stub = new WSEstadosPortBindingStub(portAddress, this);
            _stub.setPortName(getWSEstadosPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setWSEstadosPortEndpointAddress(java.lang.String address) {
        WSEstadosPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (WSEstados_PortType.class.isAssignableFrom(serviceEndpointInterface)) {
                WSEstadosPortBindingStub _stub = new WSEstadosPortBindingStub(new java.net.URL(WSEstadosPort_address), this);
                _stub.setPortName(getWSEstadosPortWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("WSEstadosPort".equals(inputPortName)) {
            return getWSEstadosPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://WebService/", "WSEstados");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://WebService/", "WSEstadosPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("WSEstadosPort".equals(portName)) {
            setWSEstadosPortEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
