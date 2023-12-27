package ec.cusoft.facturaec.filewriter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.InvoiceLineTax;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;

import ec.cusoft.facturaec.EEIParamFacturae;

public abstract class AbstractFileGeneration {
  protected int mod11(String key) {
    int mod11, result, total = 0;

    if (!key.matches("^\\d{48}$")) {
      throw new OBException("El formato de la clave de acceso es incorrecto.");
    }

    for (int i = key.length() - 1, weight = 2; i >= 0; i--) {
      total = total + (Character.getNumericValue(key.charAt(i)) * weight);

      if (weight == 7) {
        weight = 2;
      } else {
        weight++;
      }
    }

    mod11 = 11 - (total % 11);

    switch (mod11) {
    case 11:
      result = 0;
      break;
    case 10:
      result = 1;
      break;
    default:
      result = mod11;
      break;
    }

    return result;
  }

  protected EEIParamFacturae getParamFEC(Invoice invoice) throws ServletException {
    // Parametros Facturae
    OBCriteria<EEIParamFacturae> paramsC = OBDal.getInstance().createCriteria(
        EEIParamFacturae.class);
    paramsC.add(Restrictions.eq("organization", invoice.getOrganization()));
    paramsC.add(Restrictions.eq("active", true));

    List<EEIParamFacturae> paramsL = paramsC.list();
    if (paramsL.size() == 0)
      throw new ServletException(
          "No existen parametros de Facturación Electrónica para la Organización "
              + invoice.getOrganization().getName());
    return paramsL.get(0);
  }

  protected List<InvoiceLine> hasInvoiceLinesWithNegativeAmounts(List<InvoiceLine> lines,
      BigDecimal netSubTotal) throws OBException {
    List<InvoiceLine> line = null;

    boolean hasNegatives = false;

    double count = 0;
    double unitPrice = 0;
    double netPriceWithoutTax = 0;
    double taxAmount = 0;
    double taxableAmount = 0;

    InvoiceLine first = lines.get(0);
    
    for (InvoiceLine invoiceLine : lines) {
    
      if (invoiceLine.getProduct() == null) {
    	  throw new OBException("No existe un producto seleccionado en la línea: "+invoiceLine.getLineNo());
      }
      if (invoiceLine.getInvoiceLineTaxList().size() == 0) {
    	  throw new OBException("No se encontraron impuestos relacionados en la línea: "+invoiceLine.getLineNo());
      }
      double tempCount = invoiceLine.getInvoicedQuantity().doubleValue();
      double tempUnitPrice = invoiceLine.getUnitPrice().doubleValue();
      double tempPriceWithoutTax = invoiceLine.getLineNetAmount().doubleValue();

      // cantidad
      if (tempCount < 0) {
        hasNegatives = true;

        tempCount = tempCount * -1;
      }

      count += tempCount;

      // precio unitario
      unitPrice += tempUnitPrice;

      // total sin impuestos
      if (tempPriceWithoutTax < 0) {
        hasNegatives = true;

        tempPriceWithoutTax = tempPriceWithoutTax * -1;
      }

      netPriceWithoutTax += tempPriceWithoutTax;

      // impuesto
      double tempTaxAmount = invoiceLine.getInvoiceLineTaxList().get(0).getTaxAmount()
          .doubleValue();
      double tempTaxableAmount = invoiceLine.getInvoiceLineTaxList().get(0).getTaxableAmount()
          .doubleValue();

      taxAmount += tempTaxAmount;
      taxableAmount += tempTaxableAmount;
    }

    if (hasNegatives) {
      line = new ArrayList<InvoiceLine>();

      InvoiceLine newLine = copyInvoiceLine(first);

      // newLine.setInvoicedQuantity(new BigDecimal(count));
      // newLine.setUnitPrice(new BigDecimal(unitPrice));
      // newLine.setLineNetAmount(new BigDecimal(netPriceWithoutTax));

      newLine.setUnitPrice(netSubTotal);
      newLine.setLineNetAmount(netSubTotal);

      newLine.getInvoiceLineTaxList().get(0).setTaxAmount(new BigDecimal(taxAmount));
      newLine.getInvoiceLineTaxList().get(0).setTaxableAmount(new BigDecimal(taxableAmount));

      line.add(newLine);
    }

    return line;
  }

  protected InvoiceLine copyInvoiceLine(InvoiceLine source) {
    InvoiceLine target = new InvoiceLine();

    InvoiceLineTax lineTax = new InvoiceLineTax();

    lineTax.setTax(source.getInvoiceLineTaxList().get(0).getTax());

    target.getInvoiceLineTaxList().add(lineTax);

    // valores a copiar
    target.setInvoicedQuantity(new BigDecimal(1));
    target.setProduct(source.getProduct());
    target.setAttributeSetValue(source.getAttributeSetValue());
    target.setTax(source.getTax());

    return target;
  }

  static public int statMod11(String key) {
    int mod11, result, total = 0;

    if (!key.matches("^\\d{48}$")) {
      throw new OBException("El formato de la clave de acceso es incorrecto.");
    }

    for (int i = key.length() - 1, weight = 2; i >= 0; i--) {
      total = total + (Character.getNumericValue(key.charAt(i)) * weight);

      if (weight == 7) {
        weight = 2;
      } else {
        weight++;
      }
    }

    mod11 = 11 - (total % 11);

    switch (mod11) {
    case 11:
      result = 0;
      break;
    case 10:
      result = 1;
      break;
    default:
      result = mod11;
      break;
    }

    return result;
  }

  static public EEIParamFacturae getGenericParamFEC(Organization org) throws ServletException {
    // Parametros Facturae
    OBCriteria<EEIParamFacturae> paramsC = OBDal.getInstance().createCriteria(
        EEIParamFacturae.class);
    paramsC.add(Restrictions.eq("organization", org));
    paramsC.add(Restrictions.eq("active", true));

    List<EEIParamFacturae> paramsL = paramsC.list();
    if (paramsL.size() == 0)
      throw new ServletException(
          "No existen parametros de Facturación Electrónica para la Organización " + org.getName());
    return paramsL.get(0);
  }
}
