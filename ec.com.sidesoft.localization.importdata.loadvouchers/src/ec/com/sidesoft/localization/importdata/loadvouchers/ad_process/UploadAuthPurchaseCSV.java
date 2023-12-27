package ec.com.sidesoft.localization.importdata.loadvouchers.ad_process;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.Attachment;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import au.com.bytecode.opencsv.CSVReader;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvPurchaseIinvoice;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvVoucherData;
import ec.com.sidesoft.localization.importdata.loadvouchers.ImdlvVoucherDataLine;

public class UploadAuthPurchaseCSV extends DalBaseProcess {
  OBError message;
  protected ConnectionProvider conn;
  protected VariablesSecureApp vars;
  protected String language;

  public static String strRowOrg = "";
  public static int strTakereplace1 = 12;

  private final static String IMPORT_ROMATO_CSV = "text/csv";

  final String attachmentFolderPath = OBPropertiesProvider.getInstance().getOpenbravoProperties()
      .getProperty("attach.path");

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception, IOException {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    this.conn = conn;
    this.language = language;
    try {
      message = new OBError();
      processRequest(bundle);
    } catch (Exception e) {
      // log4j1.info("exeption" + e);
      message.setMessage(e.getMessage());
      message.setTitle(Utility.messageBD(conn, "Error", language));
      message.setType("Error");
    } finally {
      bundle.setResult(message);
    }
  }

  public void processRequest(ProcessBundle bundle) throws Exception {

    String[] dataLines;
    String messageT = "";
    String typeM = "";
    String titleM = "";

    String recordId = (String) bundle.getParams().get("Imdlv_Voucher_Purchase_ID");
    ImdlvVoucherData headerImporData = OBDal.getInstance().get(ImdlvVoucherData.class, recordId);
    String pathFile = getLocationFileCSV(recordId, headerImporData.getFormato());

    if (pathFile == "") {
      typeM = "Error";
      titleM = "Error";
      messageT = "No se encontro ningún archivo de tipo <b>csv</b> en los adjuntos";
      message.setMessage(messageT);
      message.setTitle(Utility.messageBD(conn, titleM, language));
      message.setType(typeM);
    } else {

      String strFilename = getFileName(recordId, headerImporData.getFormato());
      dataLines = IMPORT_ROMATO_CSV.equals(headerImporData.getFormato()) == true
          ? ImportVoucherData.loadFileLinesTmp(pathFile)
          : loadFileLinesTmp(pathFile);

      String strmsg = "";
      typeM = "";
      titleM = "";
      boolean status = false;

      String strFileNameExt = getFileNameExt(recordId, headerImporData.getFormato());

      int CountFile = getFileNameCount(strFileNameExt, headerImporData.getFormato());

      if (CountFile > 1) {
        typeM = "info";
        titleM = "Information";
        messageT = "El archivo que intenta subir ya ha sido cargado.";
        status = false;
      } else {
        if (headerImporData.getDocstatus().equals("PR")) {
          typeM = "Error";
          titleM = "Error";
          messageT = "La transacción se encuentra en estado procesado";
          status = false;
        } else {

          OBCriteria<ImdlvVoucherDataLine> obcLinesTmp = OBDal.getInstance()
              .createCriteria(ImdlvVoucherDataLine.class);
          obcLinesTmp.add(
              Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_IMDLVVOUCHERPURCHASE, headerImporData));

          if (obcLinesTmp.list().size() > 0) {
            List<ImdlvVoucherDataLine> listLinestmp = obcLinesTmp.list();
            for (ImdlvVoucherDataLine colLinestmp : listLinestmp) {
              ImdlvVoucherDataLine removeLines = colLinestmp;

              OBDal.getInstance().remove(removeLines);
            }
          }

          String strMsgLinesRepeat = IMPORT_ROMATO_CSV.equals(headerImporData.getFormato()) == true
              ? ImportVoucherData.ValidateloadLinesObject(dataLines)
              : ValidateloadLinesObject(dataLines);

          strmsg = IMPORT_ROMATO_CSV.equals(headerImporData.getFormato()) == true
              ? ImportVoucherData.loadLinesObject(dataLines, recordId, strFilename)
              : loadLinesObject(dataLines, recordId, strFilename);

          OBCriteria<ImdlvVoucherDataLine> obcLines = OBDal.getInstance()
              .createCriteria(ImdlvVoucherDataLine.class);
          obcLines.add(
              Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_IMDLVVOUCHERPURCHASE, headerImporData));
          if (obcLines.list().size() > 0) {
            status = true;
            typeM = "Success";
            titleM = "ProcessOK";
          } else {
            status = false;
            typeM = "Error";
            titleM = "Error";
            messageT = "Error el cargar el archivo.";

          }

          if (strmsg.equals("OK")) {
            status = true;
            typeM = "Success";
            titleM = "ProcessOK";
          }

          if (strMsgLinesRepeat.contains("Error")) {
            status = false;
            typeM = "Info";
            titleM = "Info";
            messageT = strMsgLinesRepeat.replace("Error", "");
          }

        }
      }

      /*
       * org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
       * CallableStatement InsertLineStatement = cp.getConnection()
       * .prepareCall("{call CSSPID_LOADLINES(?)}"); // ORIGINAL
       * InsertLineStatement.setString(1,recordId); InsertLineStatement.execute();
       * 
       * InsertLineStatement.close(); cp.destroy();
       */

      if (status != false) {
        messageT = "La lineas se cargaron correctamente sin observaciones.";
        titleM = "ProcessOK";
        /*
         * String strURL=
         * "https://cel.sri.gob.ec/comprobantes-electronicos-ws/AutorizacionComprobantesOffline?wsdl";
         * 
         * 
         * try{ SearchXmlSri(strURL); }catch(Exception ee) {}
         */
        // record.setAlertStatus("SSWOUP_AP");
        message.setMessage(messageT);
        message.setTitle(Utility.messageBD(conn, titleM, language));
        message.setType(typeM);
      } else {

        // record.setAlertStatus("SSWOUP_AP");
        message.setMessage(messageT);
        message.setTitle(Utility.messageBD(conn, titleM, language));
        message.setType(typeM);
      }
    }
  }

  public String getLocationFileCSV(String recordId, final String formato) throws OBException {
    OBContext.setAdminMode(true);
    OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
    final Table table = OBDal.getInstance().get(Table.class, "094B879B858745BA878EFC56ECFEC067");
    obc.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordId));
    obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
    obc.add(Restrictions.eq(Attachment.PROPERTY_DATATYPE, formato));
    obc.addOrderBy(Attachment.PROPERTY_SEQUENCENUMBER, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setMaxResults(1);
    Attachment attach = (Attachment) obc.uniqueResult();
    if (attach == null) {
      return "";
    }
    return attachmentFolderPath + "/" + attach.getPath() + '/' + attach.getName();
  }

  public List<String> loadFileLines(String filename) throws Exception, IOException {

    CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"),
        ',', '\"', '\\', 0, false, true);

    List<String> emps = new ArrayList<String>();
    // read line by line
    String[] record = null;
    // skip header row
    reader.readNext();

    while ((record = reader.readNext()) != null) {
      if (record.length > 1 || record[0].length() > 0) {
        emps.add((String) record[0]);
      }
    }
    reader.close();
    return emps;
  }

  public String[] loadFileLinesTmp(String filename) throws Exception, IOException {

    CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"),
        ']', '\"', '\\', 0, false, true);

    int count = 0;
    // read line by line
    String[] record = null;
    // skip header row
    reader.readNext();

    while ((record = reader.readNext()) != null) {
      if (record.length > 1 || record[0].length() > 0) {

        String strRecord = (String) record[0];
        String[] strParseString = strRecord.split("\t");

        String objtmp = strParseString.toString();
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
        String strReplace = strRecord.replace(",", "*coma*").replace(";", "*puntocoma*");
        String[] strParseString = strReplace.split("\t");
        List<String> strRow = new ArrayList<String>(Arrays.asList(strParseString));
        emps[intidx] = strRow.toString();
        intidx++;
      }
    }
    reader2.close();

    return emps;
  }

  public static Object[] getObjetRows(String[] strRows) {

    // strRows = new String[3];
    boolean flagconitnue = true;
    int countIterator = 0;

    // strRows[0]="20210909 ECUEFT ALTAMIRANO CHERREZ ANGELA INES Francisco de Orellana y Quito
    // Barrio Par593994081395 1802044857001 0000137002202121/09/10
    // USD0000000000.00ALTAMIRANO-163096405 Ajuste de Renta 1SH12586 inesangela2066@hotmail.com
    // ALTAMIRANO-1630964056737 0000000060.0021/09/07000000000000.00 21/09/06";
    // strRows[1]="20210909 ECUEFT ALTAMIRANO CHERREZ ANGELA INES Francisco de Orellana y Quito
    // Barrio Par593994081395 1802044857001 0000137002202121/09/10
    // USD0000000000.00ALTAMIRANO-1630964056511990 2135 ALTAMIRANO CHERREZ ANGELA INES 1SH12586
    // inesangela2066@hotmail.com ALTAMIRANO-1630964056737 0000000060.0021/09/07000000000000.00
    // 21/09/06";
    // strRows[2]="20210909 ECUEFT CARVAJAL AREVALO MARIANA ERMILA Arturo Hidalgo 282 Y Dr Raul
    // Mantalvo 5930990892621 1001794658001 0000137003202121/09/10 USD0000000000.00CARVAJAL
    // A-163096368 Reembolso de Agua/Luz 1SH12585 Marianacarvajal0029@hotmail.com CARVAJAL
    // A-1630963687509 0000000024.7821/09/07000000000000.00 21/09/06";
    Object[] objRows = new Object[strRows.length];
    for (int i = 1; i < strRows.length; i++) {

      strRowOrg = String.valueOf(strRows[i]);
      // System.out.println("Linea:" + i + " - Contar Linea:" + strRowOrg.trim().length() +"\n");
      // System.out.println(String.valueOf(strRows[i]));
      if (strRowOrg.trim().length() > 9) {
        List<String> listRow = new ArrayList<String>();

        String strRowAux = String.valueOf(strRows[i]);
        countIterator = 0;
        flagconitnue = true;
        while (flagconitnue) {
          String strNewValue = "";
          countIterator++;

          if (strRowOrg.trim().equals("")) {
            flagconitnue = false;
          } else {

            if (countIterator == 1) {
              strNewValue = strRowOrg.substring(0, 8);
              strRowOrg = strRowOrg.replace(strNewValue, "");
              // System.out.println(strNewValue);
            }
            if (countIterator == 2) {
              strRowOrg = strRowOrg.substring(strTakereplace1,
                  (strRowOrg.length() - strTakereplace1));
              // strRowOrg = strRowOrg.replace(strNewValue,";");
              strNewValue = strRowOrg.substring(0, 8);
              strRowOrg = strRowOrg.replace(strNewValue, "");
              strRowOrg = strRowOrg.trim();

              // strNewValue= strRowOrg.substring(1,3);
              String strfield4 = strNewValue.substring(0, 3);
              String strfield5 = strNewValue.replace(strfield4, "");
              listRow.add(strfield4.trim().length() > 0 ? strfield4 : "null");
              listRow.add(strfield5.trim().length() > 0 ? strfield5 : "null");
              strNewValue = "";
              // System.out.println(strNewValue);

            }
            if (countIterator == 3) {

              int countSpace = getFieldValue(strRowOrg);
              strNewValue = strRowOrg.trim().substring(0, countSpace);
              // System.out.println("reemplazar: "+ strNewValue);
              strRowOrg = strRowOrg.replace(strNewValue, "");

              // System.out.println(countSpace);
            }
            if (countIterator == 4) {

              // strRowOrg = strRowOrg.substring(55, (strRowOrg.length()- strTakereplace1));
              // strRowOrg = strRowOrg.replace(strNewValue,";");
              strNewValue = strRowOrg.substring(0, 55);
              strRowOrg = strRowOrg.replace(strNewValue, "");
              strRowOrg = strRowOrg.trim();

              // System.out.println(strNewValue + ":");
              // System.out.println(strRowOrg + ":");
              // int intnewCount4 = getFieldValueReverse(strNewValue.trim());
              // if (intnewCount4>0) {
              // String strfield4 =
              // strNewValue.substring(0,strNewValue.trim().length()+1-intnewCount4 );
              // String strfield5 = strNewValue.replace(strfield4, "");
              String strfield4 = strNewValue.substring(0, 40);
              String strfield5 = strNewValue.replace(strfield4, "");
              listRow.add(strfield4.trim().length() > 0 ? strfield4 : "null");
              listRow.add(strfield5.trim().length() > 0 ? strfield5 : "null");
              strNewValue = "";
              // System.out.println(strfield4);
              // System.out.println(strfield5);

              // }

            }

            if (countIterator == 5) {

              strNewValue = strRowOrg.substring(1, 20);
              strRowOrg = strRowOrg.replace(strNewValue, "");
              strRowOrg = strRowOrg.trim();

              /// System.out.println(strNewValue);
            }

            if (countIterator == 6) {

              strNewValue = strRowOrg.substring(0, 24);

              // System.out.println("strNewValue: " + strNewValue);
              strRowOrg = strRowOrg.replace(strNewValue, "");
              strRowOrg = strRowOrg.trim();

              strNewValue = strNewValue.trim();
              int countField6 = strNewValue.length();

              int countAuxposf = countField6 - 12;

              String strfield4 = strNewValue.substring(0, countAuxposf);
              strNewValue = strNewValue.replace(strfield4, "");
              // System.out.println("strNewValue 2: " + strNewValue);
              countAuxposf = strNewValue.length();
              String strfield6 = strNewValue.substring(4, countAuxposf);

              String strfield5 = strNewValue.replace(strfield6, "");
              listRow.add(strfield4.trim().length() > 0 ? strfield4 : "null");
              listRow.add(strfield6.trim().length() > 0 ? strfield5 : "null");
              listRow.add(strfield5.trim().length() > 0 ? strfield6 : "null");

              strNewValue = "";
              // flagconitnue=false;
              // System.out.println("strfield4: "+strfield4);
              // System.out.println("strfield5: "+strfield5);
              // System.out.println("strfield6: "+strfield6);
              // break;

            }

            if (countIterator == 7) {

              strNewValue = strRowOrg.substring(0, 16);
              // System.out.println(strNewValue);

              strRowOrg = strRowOrg.replace(strNewValue, "");
              // strRowOrg = strRowOrg.trim();
              String strfield4 = strNewValue.substring(0, 3);
              String strfield5 = strNewValue.replace(strfield4, "");
              listRow.add(strfield4.trim().length() > 0 ? strfield4 : "null");
              listRow.add(strfield5.trim().length() > 0 ? strfield5 : "null");
              strNewValue = "";

            }

            if (countIterator == 8) {

              strNewValue = strRowOrg.substring(0, 61);
              System.out.println(strNewValue);

              strRowOrg = strRowOrg.replace(strNewValue, "");
              // strRowOrg = strRowOrg.trim();
              int count8 = 0;

              String strfield4 = strNewValue.substring(0, 20);
              strNewValue = strNewValue.replace(strfield4, "");
              if (strNewValue.trim().length() >= 0 && strNewValue.trim().length() <= 10) {
                count8 = strNewValue.trim().length();
              } else {
                count8 = 10;
              }
              System.out.println(strfield4);

              String strfield5 = strNewValue.substring(0, count8);
              strNewValue = strNewValue.replace(strfield5, "");
              // strNewValue.replace(strfield5, "");

              System.out.println(strfield5);

              if (strNewValue.trim().length() > 0 && strNewValue.trim().length() <= 10) {
                count8 = strNewValue.trim().length();
              } else {

                count8 = 10;
              }

              String strfield6 = strNewValue.substring(0, count8);
              strNewValue = strNewValue.replace(strfield6, "");

              System.out.println(strfield6);

              if (strNewValue.trim().length() > 0 && strNewValue.trim().length() <= 21) {
                count8 = strNewValue.trim().length();
              } else {
                count8 = 0;
              }
              String strfield7 = strNewValue.substring(0, count8);

              System.out.println(strfield7);

              listRow.add(strfield4.trim().length() > 0 ? strfield4 : "null");
              listRow.add(strfield5.trim().length() > 0 ? strfield5 : "null");
              listRow.add(strfield6.trim().length() > 0 ? strfield6 : "null");
              listRow.add(strfield7.trim().length() > 0 ? strfield7 : "null");

              strNewValue = "";

              System.out.println(strRowOrg);
            }

            if (countIterator == 9) {
              // System.out.println("strRowOrg: " +strRowOrg);
              strNewValue = strRowOrg.substring(0, 40);
              strRowOrg = strRowOrg.replace(strNewValue, "");
              strRowOrg = strRowOrg.trim();
              // flagconitnue=false;

            }
            if (countIterator == 10) {
              // 1SH12586

              strNewValue = strRowOrg.substring(0, 10);
              strRowOrg = strRowOrg.replace(strNewValue, "");
              strRowOrg = strRowOrg;

              String strfield4 = strNewValue.substring(0, 3);
              String strfield5 = strNewValue.replace(strfield4, "");
              listRow.add(strfield4.trim().length() > 0 ? strfield4 : "null");
              listRow.add(strfield5.trim().length() > 0 ? strfield5 : "null");
              strNewValue = "";
              // flagconitnue=false;
              // System.out.println(strRowOrg);
            }

            if (countIterator == 11) {

              strNewValue = strRowOrg.substring(0, 94);

              strRowOrg = strRowOrg.replace(strNewValue, "");
              String strfield4 = strNewValue.substring(0, 70);
              String strfield5 = strNewValue.replace(strfield4, "");
              listRow.add(strfield4.trim().length() > 0 ? strfield4 : "null");
              listRow.add(strfield5.trim().length() > 0 ? strfield5 : "null");
              strNewValue = "";

              strRowOrg = strRowOrg.trim();

            }
            if (countIterator == 12) {

              strNewValue = strRowOrg.substring(0, 35);
              strRowOrg = strRowOrg.replace(strNewValue, "");
              strRowOrg = strRowOrg.trim();

            }

            if (countIterator == 13) {
              // System.out.println(strRowOrg);
              int countfinalRow = 0;
              if (strRowOrg.length() < 43) {
                countfinalRow = strRowOrg.length();
              } else {
                countfinalRow = 43;
              }
              strNewValue = strRowOrg.substring(0, countfinalRow);
              int countfinal = strNewValue.length();

              strRowOrg = strRowOrg.replace(strNewValue, "");
              strRowOrg = strRowOrg.trim();
              String strfield4 = strNewValue.substring(0, 13);
              // System.out.println("strNewValue 1: "+strNewValue);

              strNewValue = strNewValue.substring(strfield4.length(), countfinalRow);

              countfinal = strNewValue.length();

              String strfield5 = strNewValue.substring(0, 8);

              if (strRowOrg.length() <= 22) {
                countfinalRow = 22;
              } else {
                countfinalRow = 30;
              }
              strNewValue = strNewValue.substring(strfield5.length(), countfinalRow);
              countfinal = strNewValue.length();
              // System.out.println("strNewValue 2: "+strNewValue);
              // System.out.println("countfinal 2: "+countfinal);

              String strfield6 = strNewValue.substring(0, countfinal);

              // strNewValue =strNewValue.substring(strfield6.length(),22);
              strNewValue = strNewValue.replace(strfield6, "");

              countfinal = strNewValue.length();
              String strfield7 = strNewValue;

              listRow.add(strfield4.trim().length() > 0 ? strfield4 : "null");
              listRow.add(strfield5.trim().length() > 0 ? strfield5 : "null");
              listRow.add(strfield6.trim().length() > 0 ? strfield6 : "null");
              listRow.add(strfield7.trim().length() > 0 ? strfield7 : "null");

              strNewValue = "";

              // System.out.println("final:" + strRowOrg);
            }

            if (countIterator == 13) {

              strNewValue = strRowAux.substring(0, 526);
              // System.out.println("final:" + strNewValue);
              strNewValue = strRowAux.replace(strNewValue, "");
              // System.out.println("final:" + strNewValue);
              strRowOrg = "";

            }
            if (!strNewValue.equals("")) {
              listRow.add(strNewValue);
            }

          }
        }
        objRows[i] = listRow;
        // System.out.println(i);
        // System.out.println(objRows[i].toString());
      }
    }
    return objRows;

  }

  public static int getFieldValue(String strValue) {

    int result = 0;
    String newCharacter = "";
    String oldCharacter = "";
    String strCha = "";
    int countSpace = 0;
    char[] ch = strValue.toCharArray();
    for (int i = 0; i < strValue.length(); i++) {

      oldCharacter = String.valueOf(ch[i]);
      newCharacter = String.valueOf(ch[i + 1]);
      if (newCharacter.equals(" ") && oldCharacter.equals(" ")) {
        countSpace++;
      } else {
        strCha = strCha + oldCharacter;
        // System.out.println(strCha);
        if (countSpace >= 2 && !newCharacter.equals(" ")) {
          i = strValue.length() + 2;
        }
      }
    }
    result = countSpace + strCha.length();
    return result;
  }

  public static int getFieldValueReverse(String strValue) {

    // System.out.println("reversa: " + strValue.trim() + ":");

    int result = 0;
    String newCharacter = "";
    String oldCharacter = "";
    String strCha = "";
    int countSpace = 0;
    char[] ch = strValue.toCharArray();

    // System.out.println("reversa count: " + ch.length + ":");

    for (int i = strValue.length() - 1; i > 0; i--) {

      oldCharacter = String.valueOf(ch[i]);

      if (oldCharacter.equals(" ")) {
        countSpace++;
      } else {
        strCha = strCha + oldCharacter;
        // System.out.println(strCha);
        if (countSpace >= 1 && !newCharacter.equals(" ")) {
          i = -1;
        }
      }
    }
    result = countSpace + strCha.length();
    return result;
  }

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

      // if (strValidateRow.trim().length()<=4) {
      // continue;
      // }

      if (objRows[j] != null) {
        // System.out.println(objRows[j].toString());
        // System.out.println("************************************");
        // System.out.println("*****CREAR OBJETO LINEAS TMP********");
        // System.out.println("************************************");
        System.out.println("LINEA:" + j);

        List<String> listRow = new ArrayList<String>(Arrays.asList(strValidateRow.split(",")));
        ;
        int counList = listRow.size();

        if (counList > 0 && counList < 2) {
          continue;
        }

        String strAuthorization = (listRow.get(10).toString().trim().replace("]", ""));

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
        newLine.setInvoiceno(listRow.get(1).toString().trim());
        newLine.setTaxid(listRow.get(2).toString().trim());

        String StrReplaceSocialname = listRow.get(3).toString().trim();
        StrReplaceSocialname = StrReplaceSocialname.replace("*coma*", ",").replace("*puntocoma*",
            ";");
        newLine.setBpartner(StrReplaceSocialname);
        Date date1 = null;
        try {
          date1 = new SimpleDateFormat("dd/MM/yyyy").parse(listRow.get(4).toString().trim());
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        newLine.setDateemision(date1); // Fecha del archivo
        date1 = null;
        try {
          String strDate = listRow.get(5).toString().trim();
          date1 = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(strDate);
        } catch (ParseException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        newLine.setDateauthorization(date1);
        newLine.setSubdocumenttype(listRow.get(6).toString().trim());
        newLine.setDocumentaffected(listRow.get(8).toString().trim());

        newLine.setKeyacess(listRow.get(9).toString().trim());
        newLine.setAuthorizationno(listRow.get(10).toString().trim().replace("]", ""));

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

        strmsg = "OK";
      }
    }

    return strmsg;
  }

  public String getFileName(final String recordId, final String formato) throws OBException {
    OBContext.setAdminMode(true);
    OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
    final Table table = OBDal.getInstance().get(Table.class, "094B879B858745BA878EFC56ECFEC067");
    obc.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordId));
    obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
    obc.add(Restrictions.eq(Attachment.PROPERTY_DATATYPE, formato));
    obc.addOrderBy(Attachment.PROPERTY_SEQUENCENUMBER, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setMaxResults(1);
    Attachment attach = (Attachment) obc.uniqueResult();
    if (attach == null) {
      return "";
    }
    return attach.getName().replace(".CSV", "").replace(".txt", "");
  }

  public String getFileNameExt(String recordId, final String formato) throws OBException {
    OBContext.setAdminMode(true);
    OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
    final Table table = OBDal.getInstance().get(Table.class, "094B879B858745BA878EFC56ECFEC067");
    obc.add(Restrictions.eq(Attachment.PROPERTY_RECORD, recordId));
    obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
    obc.add(Restrictions.eq(Attachment.PROPERTY_DATATYPE, formato));
    obc.addOrderBy(Attachment.PROPERTY_SEQUENCENUMBER, false);
    obc.setFilterOnReadableOrganization(false);
    obc.setMaxResults(1);
    Attachment attach = (Attachment) obc.uniqueResult();
    if (attach == null) {
      return "";
    }
    return attach.getName();
  }

  public int getFileNameCount(final String filename, final String formato) throws OBException {
    OBContext.setAdminMode(true);
    OBCriteria<Attachment> obc = OBDal.getInstance().createCriteria(Attachment.class);
    final Table table = OBDal.getInstance().get(Table.class, "094B879B858745BA878EFC56ECFEC067");
    obc.add(Restrictions.eq(Attachment.PROPERTY_NAME, filename));
    obc.add(Restrictions.eq(Attachment.PROPERTY_TABLE, table));
    obc.add(Restrictions.eq(Attachment.PROPERTY_DATATYPE, formato));
    obc.addOrderBy(Attachment.PROPERTY_SEQUENCENUMBER, false);
    obc.setFilterOnReadableOrganization(false);

    return obc.list().size();
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

        String strAuthorization = (listRow.get(10).toString().trim().replace("]", ""));

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

  public String SearchXmlSri(String wsURL) {

    Object[] objMessage = null;

    String claveAcceso = "";

    try {
      OBCriteria<ImdlvVoucherDataLine> obcLines = OBDal.getInstance()
          .createCriteria(ImdlvVoucherDataLine.class);
      obcLines.add(Restrictions.eq(ImdlvVoucherDataLine.PROPERTY_ACTIVE, true));

      int intList = obcLines.list().size();
      for (ImdlvVoucherDataLine listaVoucher : obcLines.list()) {

        String strClaveAcceso = listaVoucher.getKeyacess();
        try {
          Thread.sleep(9000);
          objMessage = getAutorizacion(strClaveAcceso, wsURL);
        } catch (Exception ee) {

        }

        if (objMessage.length > 0) {
          String strXML = objMessage[1].toString();

          if (strXML.length() > 0) {
            ImdlvVoucherDataLine LinesImporData = OBDal.getInstance()
                .get(ImdlvVoucherDataLine.class, listaVoucher.getId());
            LinesImporData.setXMLSri(strXML);
            OBDal.getInstance().save(LinesImporData);
          }
        }
      }
      OBDal.getInstance().commitAndClose();

    } catch (Exception e) {

    }

    return "OK";
  }

  public Object[] getAutorizacion(String claveAcceso, String wsURL) throws MalformedURLException,
      IOException, TransformerConfigurationException, TransformerException {
    // Code to make a webservice HTTP request
    String responseString = "";
    String outputString = "";

    URL url = new URL(wsURL);
    URLConnection connection = url.openConnection();
    HttpURLConnection httpConn = (HttpURLConnection) connection;
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    String xmlInput = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ec=\"http://ec.gob.sri.ws.autorizacion\">\n"
        + "<soapenv:Header/>\n" + "<soapenv:Body>\n" + "<ec:autorizacionComprobante>\n"
        + "<!--Optional:-->\n" + "<claveAccesoComprobante>" + claveAcceso
        + "</claveAccesoComprobante>\n" + "</ec:autorizacionComprobante>\n" + "</soapenv:Body>\n"
        + "</soapenv:Envelope>";
    byte[] buffer = new byte[xmlInput.length()];
    buffer = xmlInput.getBytes();
    bout.write(buffer);
    byte[] b = bout.toByteArray();
    String SOAPAction = "";
    // Set the appropriate HTTP parameters.
    httpConn.setRequestProperty("Content-Length", String.valueOf(b.length));
    httpConn.setRequestProperty("Content-Type", "text/xml");
    httpConn.setRequestProperty("SOAPAction", SOAPAction);
    httpConn.setRequestMethod("POST");
    httpConn.setDoOutput(true);
    httpConn.setDoInput(true);
    OutputStream out = httpConn.getOutputStream();
    // Write the content of the request to the outputstream of the HTTP Connection.
    out.write(b);
    out.close();
    // Ready with sending the request.
    // Read the response.
    InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
    BufferedReader in = new BufferedReader(isr);
    // Write the SOAP message response to a String.
    while ((responseString = in.readLine()) != null) {
      outputString = outputString + responseString;
    }
    // Parse the String output to a org.w3c.dom.Document and be able to reach every node with the
    // org.w3c.dom API.
    Document document = parseXmlFile(outputString);
    NodeList nodeLst = document.getElementsByTagName("autorizaciones");
    String weatherResult = nodeLst.item(0).getTextContent();
    // System.out.println("Weather: " + weatherResult);
    // Write the SOAP message formatted to the console.
    String formattedSOAPResponse = formatXML(outputString);
    // System.out.println(formattedSOAPResponse);

    org.w3c.dom.Node xml = document.getElementsByTagName("comprobante").item(0);
    weatherResult = xml.getTextContent();
    formattedSOAPResponse = formatXML(weatherResult);
    System.out.println(formattedSOAPResponse);

    Document documentFinal = parseXmlFile(formattedSOAPResponse);

    Object aut[] = new Object[2];
    aut[0] = "OK";
    aut[1] = formattedSOAPResponse;
    return aut;
  }

  public static String formatXML(String unformattedXml) {
    try {
      Document document = parseXmlFile(unformattedXml);
      OutputFormat format = new OutputFormat(document);
      format.setIndenting(true);
      format.setIndent(3);
      format.setOmitXMLDeclaration(true);
      Writer out = new StringWriter();
      XMLSerializer serializer = new XMLSerializer(out, format);
      serializer.serialize(document);
      return out.toString();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static Document parseXmlFile(String in) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      DocumentBuilder db = dbf.newDocumentBuilder();
      org.xml.sax.InputSource is = new org.xml.sax.InputSource(new StringReader(in));
      return db.parse(is);
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    } catch (SAXException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
