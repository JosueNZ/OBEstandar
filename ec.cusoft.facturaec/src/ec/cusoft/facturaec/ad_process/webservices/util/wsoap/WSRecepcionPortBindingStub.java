/**
 * WSRecepcionPortBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package ec.cusoft.facturaec.ad_process.webservices.util.wsoap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.openbravo.base.exception.OBException;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;

public class WSRecepcionPortBindingStub extends org.apache.axis.client.Stub implements
    ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_PortType {
  private java.util.Vector cachedSerClasses = new java.util.Vector();
  private java.util.Vector cachedSerQNames = new java.util.Vector();
  private java.util.Vector cachedSerFactories = new java.util.Vector();
  private java.util.Vector cachedDeserFactories = new java.util.Vector();
  private int intTimeout = 120000;

  static org.apache.axis.description.OperationDesc[] _operations;

  static {
    _operations = new org.apache.axis.description.OperationDesc[1];
    _initOperationDesc1();
  }

  private static void _initOperationDesc1() {
    org.apache.axis.description.OperationDesc oper;
    org.apache.axis.description.ParameterDesc param;
    oper = new org.apache.axis.description.OperationDesc();
    oper.setName("generar");
    param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("", "xml"),
        org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName(
            "http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("",
        "subtotal12"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName(
        "http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("",
        "subtotal0"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName(
        "http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("",
        "subtotalnoiva"), org.apache.axis.description.ParameterDesc.IN,
        new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"),
        java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("",
        "subtotalexento"), org.apache.axis.description.ParameterDesc.IN,
        new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"),
        java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    param = new org.apache.axis.description.ParameterDesc(
        new javax.xml.namespace.QName("", "iva12"), org.apache.axis.description.ParameterDesc.IN,
        new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"),
        java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("",
        "irbpnr"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName(
        "http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("",
        "clidireccion"), org.apache.axis.description.ParameterDesc.IN,
        new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"),
        java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("",
        "clitelefono"), org.apache.axis.description.ParameterDesc.IN,
        new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"),
        java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("",
        "clicorreo"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName(
        "http://www.w3.org/2001/XMLSchema", "string"), java.lang.String.class, false, false);
    param.setOmittable(true);
    oper.addParameter(param);
    oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
    oper.setReturnClass(java.lang.String.class);
    oper.setReturnQName(new javax.xml.namespace.QName("", "return"));
    oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
    oper.setUse(org.apache.axis.constants.Use.LITERAL);
    _operations[0] = oper;

  }

  public WSRecepcionPortBindingStub() throws org.apache.axis.AxisFault {
    this(null);
  }

  public WSRecepcionPortBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service)
      throws org.apache.axis.AxisFault {
    this(service);
    super.cachedEndpoint = endpointURL;
  }

  public WSRecepcionPortBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
    if (service == null) {
      super.service = new org.apache.axis.client.Service();
    } else {
      super.service = service;
    }
    ((org.apache.axis.client.Service) super.service).setTypeMappingVersion("1.2");
  }

  protected org.apache.axis.client.Call createCall(int intTimeOutPara) throws java.rmi.RemoteException {
    try {
      org.apache.axis.client.Call _call = super._createCall();
      if (super.maintainSessionSet) {
        _call.setMaintainSession(super.maintainSession);
      }
      if (super.cachedUsername != null) {
        _call.setUsername(super.cachedUsername);
      }
      if (super.cachedPassword != null) {
        _call.setPassword(super.cachedPassword);
      }
      if (super.cachedEndpoint != null) {
        _call.setTargetEndpointAddress(super.cachedEndpoint);
      }
      // if (super.cachedTimeout != null) {
      // _call.setTimeout(super.cachedTimeout);

      // **************************TIEMPO DE ESPERA PARA WEBSERVICE

      try {
    	if(intTimeOutPara==0){
    		intTimeout = SelectTimeOut();
    	}else{
    		intTimeout=intTimeOutPara;
    	}
      } catch (Exception e) {

      }
      _call.setTimeout(new Integer(intTimeout));
      // }
      if (super.cachedTimeout != null) {
        _call.setTimeout(super.cachedTimeout);
      }
      if (super.cachedPortName != null) {
        _call.setPortName(super.cachedPortName);
      }
      java.util.Enumeration keys = super.cachedProperties.keys();
      while (keys.hasMoreElements()) {
        java.lang.String key = (java.lang.String) keys.nextElement();
        _call.setProperty(key, super.cachedProperties.get(key));
      }
      return _call;
    } catch (java.lang.Throwable _t) {
      throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
    }
  }

  public java.lang.String generar(java.lang.String xml, java.lang.String subtotal12,
      java.lang.String subtotal0, java.lang.String subtotalnoiva, java.lang.String subtotalexento,
      java.lang.String iva12, java.lang.String irbpnr, java.lang.String clidireccion,
      java.lang.String clitelefono, java.lang.String clicorreo, int intTimeOut) throws java.rmi.RemoteException {
    if (super.cachedEndpoint == null) {
      throw new org.apache.axis.NoEndPointException();
    }
    org.apache.axis.client.Call _call = createCall(intTimeOut);
    _call.setOperation(_operations[0]);
    _call.setUseSOAPAction(true);
    _call.setSOAPActionURI("");
    _call.setEncodingStyle(null);
    _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
    _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
    _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
    _call.setOperationName(new javax.xml.namespace.QName("http://WebService/", "generar"));

    setRequestHeaders(_call);
    setAttachments(_call);
    try {
      java.lang.Object _resp = _call.invoke(new java.lang.Object[] { xml, subtotal12, subtotal0,
          subtotalnoiva, subtotalexento, iva12, irbpnr, clidireccion, clitelefono, clicorreo });

      if (_resp instanceof java.rmi.RemoteException) {
        throw (java.rmi.RemoteException) _resp;
      } else {
        extractAttachments(_call);
        try {
          return (java.lang.String) _resp;
        } catch (java.lang.Exception _exception) {
          return (java.lang.String) org.apache.axis.utils.JavaUtils.convert(_resp,
              java.lang.String.class);
        }
      }
    } catch (org.apache.axis.AxisFault axisFaultException) {
      throw axisFaultException;
    }
  }

  public static int SelectTimeOut() {
    ConnectionProvider conn = new DalConnectionProvider(false);

    try {
      String strSql = "SELECT timeout_response  FROM eei_param_facturae where isactive='Y' and url_ws_validacion is not null";
      PreparedStatement st = null;
      int intParametro = 0;
      st = conn.getPreparedStatement(strSql);
      ResultSet rsConsulta = st.executeQuery();
      int contador = 0;
      while (rsConsulta.next()) {
        contador = contador + 1;
        intParametro = rsConsulta.getInt("timeout_response");

      }
      if (contador == 0) {
        throw new OBException(
            "No se encontró parametrización de url de webservice en documentos electrónicos.");
      } else if (contador > 1) {

        throw new OBException(
            "Existe más de una parametrización activa de documentos electrónicos.");
      }
      return intParametro;
    } catch (Exception e) {

      throw new OBException(
          "Error al consultar la tabla eei_param_facturae (Parámetro Tiempo de espera) " + e);
    } finally {
      try {
        conn.destroy();
      } catch (Exception e) {

      }
    }

  }

}
