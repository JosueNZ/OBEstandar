package ec.cusoft.facturaec.templates;

import java.io.File;
import java.util.Set;

import org.openbravo.model.common.invoice.Invoice;

/**
 * Defines the interface for Electronic Invoicing in Openbravo ERP
 * 
 * @author openbravo
 * 
 */
public interface OBEInvoice_I {

  /**
   * Returns an electronic invoice file for the given invoice
   * 
   * @param invoice
   *          Openbravo's invoice used for generating the electronic invoice
   * @param tmpDir
   *          Temporal directory to generate the file. Can be null if the class which implements the
   *          method does not need a temporal directory
   * @param lang
   *          Language. Used for displaying error messages in the correct language
   * @return a File with the electronic invoice
   * @throws Exception
   */
  public String generateFile(Invoice invoice, String tmpDir, String lang) throws Exception;

  /**
   * Returns an electronic invoice file for the given list of invoices
   * 
   * @param invoices
   *          List of Openbravo's invoices used for generating the electronic invoice
   * @param tmpDir
   *          Temporal directory to generate the file. Can be null if the class which implements the
   *          method does not need a temporal directory
   * @param lang
   *          Language. Used for displaying error messages in the correct language
   * @return a File with the electronic invoice
   * @throws Exception
   */
  public String generateFile(Set<Invoice> invoices, String tmpDir, String lang) throws Exception;

  /**
   * Validates the given electronic invoice
   * 
   * @param file
   *          electronic invoice
   * @return true if valid file
   * @throws Exception
   */
  public boolean validateFile(File file) throws Exception;

  public String getFTPFolderName();
}
