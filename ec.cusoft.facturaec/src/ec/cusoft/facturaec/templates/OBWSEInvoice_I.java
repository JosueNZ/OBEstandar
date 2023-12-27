package ec.cusoft.facturaec.templates;

import java.io.File;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.common.invoice.Invoice;

public interface OBWSEInvoice_I {
  public String sendFile(ConnectionProvider con, File file, Invoice invoice, String strLanguage)
      throws Exception;
}