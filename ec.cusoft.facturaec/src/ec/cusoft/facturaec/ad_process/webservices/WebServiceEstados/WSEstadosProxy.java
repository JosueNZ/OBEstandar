package ec.cusoft.facturaec.ad_process.webservices.WebServiceEstados;

public class WSEstadosProxy implements WSEstados_PortType {
  private String _endpoint = null;
  private WSEstados_PortType wSEstados_PortType = null;
  
  public WSEstadosProxy() {
    _initWSEstadosProxy();
  }
  
  public WSEstadosProxy(String endpoint) {
    _endpoint = endpoint;
    _initWSEstadosProxy();
  }
  
  private void _initWSEstadosProxy() {
    try {
      wSEstados_PortType = (new WSEstados_ServiceLocator()).getWSEstadosPort();
      if (wSEstados_PortType != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)wSEstados_PortType)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)wSEstados_PortType)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (wSEstados_PortType != null)
      ((javax.xml.rpc.Stub)wSEstados_PortType)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public WSEstados_PortType getWSEstados_PortType() {
    if (wSEstados_PortType == null)
      _initWSEstadosProxy();
    return wSEstados_PortType;
  }
  
  public java.lang.String getLastStatus(java.lang.String RUC, java.lang.String ID, java.lang.String docType) throws java.rmi.RemoteException{
    if (wSEstados_PortType == null)
      _initWSEstadosProxy();
    return wSEstados_PortType.getLastStatus(RUC, ID, docType);
  }
  
  
}