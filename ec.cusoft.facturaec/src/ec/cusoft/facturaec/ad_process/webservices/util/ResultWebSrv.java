package ec.cusoft.facturaec.ad_process.webservices.util;

public class ResultWebSrv {
  private String id = null;
  private String documentNo = null;
  private Boolean estatus = null;
  private String message = null;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public void setDocumentNo(String documentNo) {
    this.documentNo = documentNo;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Boolean getEstatus() {
    return estatus;
  }

  public void setEstatus(Boolean estatus) {
    this.estatus = estatus;
  }

}
