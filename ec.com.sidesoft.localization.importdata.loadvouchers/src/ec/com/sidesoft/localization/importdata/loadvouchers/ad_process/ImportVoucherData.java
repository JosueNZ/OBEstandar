package ec.com.sidesoft.localization.importdata.loadvouchers.ad_process;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;

import au.com.bytecode.opencsv.CSVReader;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvPurchaseIinvoice;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvVoucherData;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvVoucherDataLine;

public class ImportVoucherData {

  public static String loadLinesObject(Object[] objRows, String strProcessID, String StrFilename) {

    String strmsg = "";
    Long intnewLine = 0L;

    ImdlvVoucherData headerImporData = OBDal.getInstance().get(ImdlvVoucherData.class,
        strProcessID);
    // headerImporData.setFilenamedata(StrFilename);
    // OBDal.getInstance().save(headerImporData);

    OBCriteria<ImdlvPurchaseIinvoice> purchaseInvoiceDf = OBDal.getInstance()
        .createCriteria(ImdlvPurchaseIinvoice.class);
    purchaseInvoiceDf.add(Restrictions.eq(ImdlvPurchaseIinvoice.PROPERTY_ACTIVE, true));

    for (int j = 1; j < objRows.length; j++) {

      String strValidateRow = objRows[j] == null ? "" : (objRows[j].toString());

      if (objRows[j] != null) {
        System.out.println("LINEA:" + j);

        List<String> listRow = new ArrayList<String>(Arrays.asList(strValidateRow.split(",")));
        ;
        int counList = listRow.size();

        if (counList > 0 && counList < 2) {
          continue;
        }

        String strAuthorization = (listRow.get(11).toString().trim().replace("]", ""));

        OBCriteria<ImdlvVoucherDataLine> obcLinesTmp = OBDal.getInstance()
            .createCriteria(ImdlvVoucherDataLine.class);
        obcLinesTmp
            .add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_AUTHORIZATIONNO, strAuthorization));
        int countRepeatAuth = obcLinesTmp.list().size();
        // No inserta el registro cuando una autorización esta registrada.
        String strDocumentAuth = "";
        for (ImdlvVoucherDataLine collAuth : obcLinesTmp.list()) {

          strDocumentAuth = strDocumentAuth + collAuth.getImdlvVoucherPurchase().getDocumentno()
              + ". Estado " + (collAuth.isProcess() ? "Procesado" : "Sin Procesar") + ",";
        }

        intnewLine = intnewLine + 10;

        ImdlvVoucherDataLine newLine = OBProvider.getInstance().get(ImdlvVoucherDataLine.class);
        newLine.setNewOBObject(true);
        newLine.setImdlvVoucherPurchase(headerImporData);
        newLine.setLine(new BigDecimal(intnewLine));
        String strYpDoc = listRow.get(0).toString().trim();

        if (strYpDoc.contains("Comprobante de Retenci")) {
          strYpDoc = "Comprobante de Retención";
        }
        if (strYpDoc.contains("Notas de D")) {
          strYpDoc = "Notas de Débito";
        }
        if (strYpDoc.contains("Notas de C")) {
          strYpDoc = "Notas de Crédito";
        }
        newLine.setDocumenttype(strYpDoc.replace("[", ""));
        newLine.setInvoiceno(listRow.get(2).toString().trim());
        newLine.setTaxid(listRow.get(3).toString().trim());

        String StrReplaceSocialname = listRow.get(4).toString().trim();
        StrReplaceSocialname = StrReplaceSocialname.replace("*coma*", ",").replace("*puntocoma*",
            ";");
        newLine.setBpartner(StrReplaceSocialname);
        Date date1 = null;
        try {
          date1 = new SimpleDateFormat("dd/MM/yyyy").parse(listRow.get(5).toString().trim());
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        newLine.setDateemision(date1); // Fecha del archivo
        date1 = null;
        try {
          String strDate = listRow.get(6).toString().trim();
          date1 = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(strDate);
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        newLine.setDateauthorization(date1);
        newLine.setSubdocumenttype(listRow.get(7).toString().trim());
        newLine.setDocumentaffected(listRow.get(9).toString().trim());

        newLine.setKeyacess(listRow.get(10).toString().trim());
        newLine.setAuthorizationno(listRow.get(11).toString().trim().replace("]", ""));

        newLine.setSubtotal(BigDecimal.ZERO);
        newLine.setVat(BigDecimal.ZERO);
        newLine.setTotalinvoice(BigDecimal.ZERO);

        newLine.setStatusinvoice("ND");
        newLine.setReferenceinvoice("ND");
        newLine.setStatusemail("ND");
        newLine.setEmails("ND");

        boolean isinvoice = true;
        boolean isorder = true;
        if (purchaseInvoiceDf.list().size() > 0) {
          String strTrx = purchaseInvoiceDf.list().get(0).getTypetrx();
          if (strTrx.equals("O")) {
            isinvoice = false;
          } else {
            isorder = false;
          }
          newLine.setCreateorder(isorder);
          newLine.setCreateinvoice(isinvoice);
        }
        if (!strDocumentAuth.equals("")) {

          strDocumentAuth = "Msg: Este comprobante ya se encuentra registrando en: "
              + strDocumentAuth + ". ";
          newLine.setLogserror(strDocumentAuth);
        }

        OBDal.getInstance().save(newLine);
        headerImporData.getImdlvVoucherpurchlineList().add(newLine);
        strmsg = "OK";
      }
    }

    OBDal.getInstance().flush();

    return strmsg;
  }

  public static String[] loadFileLinesTmp(String filename) throws Exception, IOException {

    CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"),
        ']', '\"', '\\', 0, false, true);

    int count = 0;
    // read line by line
    String[] record = null;
    // skip header row
    reader.readNext();

    while ((record = reader.readNext()) != null) {
      if (record.length > 1 || record[0].length() > 0) {
        count++;
      }
    }
    reader.close();
    CSVReader reader2 = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"),
        ']', '\"', '\\', 0, false, true);
    String[] emps = new String[count + 3];
    int intidx = 0;
    while ((record = reader2.readNext()) != null) {
      if (record.length > 1 || record[0].length() > 0) {

        String strRecord = (String) record[0];
        // String strReplace = strRecord.replace(",", "*coma*").replace(";", "*puntocoma*");
        String[] strParseString = strRecord.split(",");
        List<String> strRow = new ArrayList<String>(Arrays.asList(strParseString));
        emps[intidx] = strRow.toString();
        intidx++;
      }
    }
    reader2.close();

    return emps;
  }

  public static String ValidateloadLinesObject(Object[] objRows) {

    String strmsg = "";
    Long intnewLine = 0L;
    String strMsg = "";
    int countLinesNotInsert = 0;

    for (int j = 1; j < objRows.length; j++) {

      String strValidateRow = objRows[j] == null ? "" : (objRows[j].toString());

      if (objRows[j] != null) {
        // System.out.println(objRows[j].toString());
        // System.out.println("************************************");
        // System.out.println("*****Validar si existen autorizaciones registradas********");
        // System.out.println("************************************");
        System.out.println("LINEA:" + strValidateRow);

        List<String> listRow = new ArrayList<String>(Arrays.asList(strValidateRow.split(",")));
        ;
        int counList = listRow.size();

        if (counList > 0 && counList < 2) {
          continue;
        }

        String strAuthorization = (listRow.get(11).toString().trim().replace("]", ""));

        OBCriteria<ImdlvVoucherDataLine> obcLinesTmp = OBDal.getInstance()
            .createCriteria(ImdlvVoucherDataLine.class);
        obcLinesTmp
            .add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_AUTHORIZATIONNO, strAuthorization));
        int countRepeatAuth = obcLinesTmp.list().size();

        if (countRepeatAuth > 0) {
          strMsg = strMsg + strAuthorization + ",";
        } else {
          countLinesNotInsert++;
        }

        // System.out.println("Linea insertada:" + j);

      }
    }
    if (strMsg.equals("")) {
      strMsg = "OK";
    } else {

      if (countLinesNotInsert == 0) {
        strMsg = "Error\nTodas las lineas han sido registradas anteriormente";
      } else {

        if (countLinesNotInsert > 0) {
          strMsg = "OK";
        } else {
          strMsg = "Error\nAutorizaciones registradas:" + strMsg;
        }
      }

    }
    return strMsg;
  }

}
