package ec.cusoft.facturaec.ad_process.webservices.util.wsoap;

public class WSRecepcionProxy
    implements ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_PortType {
  private String _endpoint = null;
  private ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_PortType wSRecepcion_PortType = null;

  public WSRecepcionProxy() {
    _initWSRecepcionProxy();
  }

  public WSRecepcionProxy(String endpoint) {
    _endpoint = endpoint;
    _initWSRecepcionProxy();
  }

  private void _initWSRecepcionProxy() {
    try {
      wSRecepcion_PortType = (new ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_ServiceLocator())
          .getWSRecepcionPort();
      if (wSRecepcion_PortType != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub) wSRecepcion_PortType)
              ._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String) ((javax.xml.rpc.Stub) wSRecepcion_PortType)
              ._getProperty("javax.xml.rpc.service.endpoint.address");
      }

    } catch (javax.xml.rpc.ServiceException serviceException) {
    }
  }

  public String getEndpoint() {
    return _endpoint;
  }

  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (wSRecepcion_PortType != null)
      ((javax.xml.rpc.Stub) wSRecepcion_PortType)
          ._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);

  }

  public ec.cusoft.facturaec.ad_process.webservices.util.wsoap.WSRecepcion_PortType getWSRecepcion_PortType() {
    if (wSRecepcion_PortType == null)
      _initWSRecepcionProxy();
    return wSRecepcion_PortType;
  }

  public java.lang.String generar(java.lang.String xml, java.lang.String subtotal12,
      java.lang.String subtotal0, java.lang.String subtotalnoiva, java.lang.String subtotalexento,
      java.lang.String iva12, java.lang.String irbpnr, java.lang.String clidireccion,
      java.lang.String clitelefono, java.lang.String clicorreo, int intTimeOutPara) throws java.rmi.RemoteException {
    if (wSRecepcion_PortType == null)
      _initWSRecepcionProxy();
    return wSRecepcion_PortType.generar(xml, subtotal12, subtotal0, subtotalnoiva, subtotalexento,
        iva12, irbpnr, clidireccion, clitelefono, clicorreo,intTimeOutPara);
  }

}