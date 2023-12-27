/*
 ************************************************************************************
 * Copyright (C) 2010-2013 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */
package org.openbravo.module.standardOFX.utility;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import net.sf.ofx4j.io.DefaultHandler;
import net.sf.ofx4j.io.OFXParseException;
import net.sf.ofx4j.io.nanoxml.NanoXMLOFXReader;

import org.openbravo.advpaymentmngt.utility.FIN_BankStatementImport;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;

public class StandardOFX extends FIN_BankStatementImport {

  FIN_BankStatementLine bsl = null;
  List<Map<String, String>> bankAccOfxList = null;
  public Long lineNo = 10l;
  Date startingDate = null;
  Date endingDate = null;
  String bankIdentifier = "";
  SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
  private static final String FIELD_TRNTYPE = "TRNTYPE";
  private static final String FIELD_INCOMETYPE = "INCOMETYPE";
  private static final String FIELD_BUYTYPE = "BUYTYPE";
  private static final String FIELD_SELLTYPE = "SELLTYPE";
  private static final String FIELD_DTPOSTED = "DTPOSTED";
  private static final String FIELD_DTTRADE = "DTTRADE";
  private static final String FIELD_TOTAL = "TOTAL";
  private static final String FIELD_TRNAMT = "TRNAMT";
  private static final String FIELD_FITID = "FITID";
  private static final String FIELD_CHECKNUM = "CHECKNUM";
  private static final String FIELD_NAME = "NAME";
  private static final String FIELD_MEMO = "MEMO";
  private static final String FIELD_LEDGERBAL = "LEDGERBAL";
  private static final String FIELD_INVPOSLIST = "INVPOSLIST";
  private static final String FIELD_BANKID = "BANKID";

  /**
   * Method used to load the file and retrieve the values from the ofx file.
   * 
   * @param in
   *          . Input Stream contains the file information.
   * @param targetBankStatement
   *          . FIN_BankStatement object.
   * @return. Returns the FIN_BankStatementLine list.
   */
  @Override
  public List<FIN_BankStatementLine> loadFile(InputStream in, FIN_BankStatement targetBankStatement) {
    BigDecimal totalMovementsAmt = BigDecimal.ZERO;
    ArrayList<FIN_BankStatementLine> newLines = new ArrayList<FIN_BankStatementLine>();
    NanoXMLOFXReader reader = new NanoXMLOFXReader();
    final Map<String, String> headers = new HashMap<String, String>();
    final Stack<Map<String, Object>> aggregateStack = new Stack<Map<String, Object>>();
    bankAccOfxList = new ArrayList<Map<String, String>>();

    OBError msg = new OBError();
    msg.setType("Error");
    msg.setTitle(FIN_Utility.messageBD("Error"));

    reader.setContentHandler(new DefaultHandler() {
      @Override
      public void onHeader(String name, String value) {
        headers.put(name, value);
      }

      @Override
      public void onElement(String name, String value) {
        char[] tabs = new char[aggregateStack.size() * 2];
        Arrays.fill(tabs, ' ');
        addKeyValue(name, value);
      }

      @Override
      public void startAggregate(String aggregateName) {
        char[] tabs = new char[aggregateStack.size() * 2];
        Arrays.fill(tabs, ' ');
        addKeyValue(aggregateName, "");
      }
    });

    try {
      // Parsing the file.
      reader.parse(in);
    } catch (OFXParseException e) {
      msg.setMessage("Error when parsing data: " + e);
      setMyError(msg);
      throw new OBException("Error when parsing data: " + e);
    } catch (IOException e) {
      msg.setMessage("Error when reading file: " + e);
      setMyError(msg);
      throw new OBException("Error when reading file: " + e);
    }

    try {
      // Getting the total no of statements.
      List<BankAccTrxList> bankAccTrxList = getBanAccTrxList();
      for (BankAccTrxList bankAccTrx : bankAccTrxList) {
        String debitAmt = "0.00";
        String creditAmt = "0.00";
        String trnAmt = bankAccTrx.getTrnAmt();
        if (trnAmt.substring(0, 1).equals("-")) {
          debitAmt = trnAmt.substring(1);
        } else {
          creditAmt = trnAmt;
        }
        newLines.add(insertBankStatementLine(targetBankStatement,
            sdf.parse(bankAccTrx.getDtPosted()), bankAccTrx.getMemo(), debitAmt, creditAmt,
            bankAccTrx.getTrnType(), bankAccTrx.getNameMemo()));
        totalMovementsAmt = totalMovementsAmt.add(new BigDecimal(bankAccTrx.getTrnAmt()));
      }
    } catch (Exception e) {
      msg.setMessage("Error when retrieving data: " + e);
      setMyError(msg);
      throw new OBException("Error when retrieving data: " + e);
    }
    updateBankStatement(targetBankStatement, totalMovementsAmt.toString());
    return newLines;
  }

  /**
   * Method used to add the key and value into the map from the ofx file.
   * 
   * @param key
   *          . Name of the tag.
   * @param value
   *          . Value of the tag.
   */
  private void addKeyValue(String key, String value) {
    TreeMap<String, String> ofxTagReader = new TreeMap<String, String>();
    ofxTagReader.put(key, value);
    bankAccOfxList.add(ofxTagReader);
  }

  /**
   * Method used to insert the bank statements into the records FIN_BankStatementLine table
   * 
   * @param bankstatement
   *          . FIN_BankStatement object.
   * @param movement_dateMovement
   *          date of the transaction.
   * @param v_concept
   *          . Name of the concept.
   * @param debit
   *          . Debit amount of the transaction.
   * @param credit
   *          . Credit amount of the transaction.
   * @param referenceNo
   *          . Reference number of the transaction.
   * @param bpartnername
   *          . Business Partner name.
   * @return. Returns FIN_BankStatementLine.
   */
  FIN_BankStatementLine insertBankStatementLine(FIN_BankStatement bankstatement,
      Date movement_date, String v_concept, String debit, String credit, String referenceNo,
      String bpartnername) {
    final FIN_BankStatementLine newBankStatementLine = OBProvider.getInstance().get(
        FIN_BankStatementLine.class);
    newBankStatementLine.setBankStatement(bankstatement);
    newBankStatementLine.setClient(bankstatement.getClient());
    newBankStatementLine.setOrganization(bankstatement.getOrganization());
    newBankStatementLine.setDramount(new BigDecimal(debit));
    newBankStatementLine.setCramount(new BigDecimal(credit));
    newBankStatementLine.setLineNo(lineNo);
    lineNo = lineNo + 10;
    newBankStatementLine.setBpartnername(bpartnername);
    newBankStatementLine.setDescription((v_concept != null && v_concept.length() > 255) ? v_concept
        .substring(0, 252).concat("...") : v_concept);
    newBankStatementLine.setTransactionDate(movement_date);
    newBankStatementLine.setReferenceNo((referenceNo == null || referenceNo.equals("")) ? "**"
        : referenceNo);
    OBDal.getInstance().save(newBankStatementLine);
    OBDal.getInstance().flush();
    return newBankStatementLine;
  }

  /**
   * Updating Bank Statement.
   * 
   * @param bankstatement
   *          . FIN_BankStatement object.
   * @param startingDate
   *          .Starting date of the bank accounting transaction
   * @param endingDate
   *          .Ending date of the bank accounting transaction
   * @param totalMovementsAmt
   *          . Indicates the total movement of the account
   * @return. Returns FIN_BankStatement object.
   */
  FIN_BankStatement updateBankStatement(FIN_BankStatement bankstatement, String totalMovementsAmt) {
    String dateFormat = OBPropertiesProvider.getInstance().getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat outputFormat = new SimpleDateFormat(dateFormat);
    OBContext.setAdminMode();
    try {
      bankstatement.setName(outputFormat.format(startingDate) + " - "
          + outputFormat.format(endingDate) + " - (" + totalMovementsAmt + " "
          + bankstatement.getAccount().getCurrency().getSymbol() + ")");
    } finally {
      OBContext.restorePreviousMode();
    }
    OBDal.getInstance().save(bankstatement);
    OBDal.getInstance().flush();
    return bankstatement;
  }

  /**
   * Inner Class for constructing structure for transaction statements.
   * 
   */
  private class BankAccTrxList {
    private String trnType = "";
    private String dtPosted = "";
    private String trnAmt = "";
    private String fiTid = "";
    private String name = "";
    private String memo = "";
    private String checkNum = "";

    @SuppressWarnings("unused")
    public String getCheckNum() {
      return checkNum;
    }

    public void setCheckNum(String checkNum) {
      this.checkNum = checkNum;
    }

    public String getTrnType() {
      return trnType;
    }

    public void setTrnType(String trnType) {
      this.trnType = trnType;
    }

    public String getDtPosted() {
      return dtPosted;
    }

    public void setDtPosted(String dtPosted) {
      this.dtPosted = dtPosted;
    }

    public String getTrnAmt() {
      return trnAmt;
    }

    public void setTrnAmt(String trnAmt) {
      this.trnAmt = trnAmt;
    }

    @SuppressWarnings("unused")
    public String getFiTid() {
      return fiTid;
    }

    public void setFiTid(String fiTid) {
      this.fiTid = fiTid;
    }

    @SuppressWarnings("unused")
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getMemo() {
      return memo;
    }

    public void setMemo(String memo) {
      this.memo = memo;
    }

    public String getNameMemo() {
      if ((name.length() == 0) || (name.equals(trnType))) {
        return (memo.length() > 60) ? memo.substring(0, 57) + "..." : memo;
      }
      return name;
    }
  }

  /**
   * Method used to retrieve the statements from the ofx file.
   * 
   * @return. No of statements available in the ofx file.
   * @throws Exception. In
   *           case of parsing invalid data.
   */
  private List<BankAccTrxList> getBanAccTrxList() throws Exception {
    BankAccTrxList bankList = null;
    List<BankAccTrxList> bankAccTrxList = new ArrayList<BankAccTrxList>();
    int count = 0;
    int numberOfTransactions = countTransactions();

    for (Map<String, String> type : bankAccOfxList) {
      if (type.containsKey(FIELD_BANKID)) {
        String bank = type.get(FIELD_BANKID);
        bankIdentifier = (bank != null && bank.length() > 60) ? bank.substring(0, 57) + "..."
            : bank;
      } else if (type.containsKey("STMTTRN") || type.containsKey("INVTRAN")) {
        if (count == 0) {
          bankList = new BankAccTrxList();
        } else {
          bankAccTrxList.add(bankList);
          bankList = new BankAccTrxList();
        }
        count = count + 1;
      } else if (type.containsKey(FIELD_TRNTYPE)) {
        bankList.setTrnType(type.get(FIELD_TRNTYPE));
      } else if (type.containsKey(FIELD_INCOMETYPE)) {
        bankList.setTrnType(type.get(FIELD_INCOMETYPE));
      } else if (type.containsKey(FIELD_BUYTYPE)) {
        bankList.setTrnType(type.get(FIELD_BUYTYPE));
      } else if (type.containsKey(FIELD_SELLTYPE)) {
        bankList.setTrnType(type.get(FIELD_SELLTYPE));
      } else if (type.containsKey(FIELD_DTPOSTED) || type.containsKey(FIELD_DTTRADE)) {
        String strDatePosted = null;
        if (type.containsKey(FIELD_DTPOSTED)) {
          strDatePosted = type.get(FIELD_DTPOSTED).substring(0, 8);
        } else {
          strDatePosted = type.get(FIELD_DTTRADE).substring(0, 8);
        }
        bankList.setDtPosted(strDatePosted);

        Date datePosted = sdf.parse(strDatePosted);
        if (startingDate == null) {
          startingDate = datePosted;
        } else {
          if (datePosted.compareTo(startingDate) < 0) {
            startingDate = datePosted;
          }
        }
        if (endingDate == null) {
          endingDate = datePosted;
        } else {
          if (datePosted.compareTo(endingDate) > 0) {
            endingDate = datePosted;
          }
        }

      } else if (type.containsKey(FIELD_TRNAMT)) {
        bankList.setTrnAmt(type.get(FIELD_TRNAMT));
      } else if (type.containsKey(FIELD_TOTAL)) {
        bankList.setTrnAmt(type.get(FIELD_TOTAL));
      } else if (type.containsKey(FIELD_FITID)) {
        String valueFitID = type.get(FIELD_FITID);
        bankList.setFiTid(valueFitID);
      } else if (type.containsKey(FIELD_CHECKNUM)) {
        bankList.setCheckNum(type.get(FIELD_CHECKNUM));
      } else if (type.containsKey(FIELD_NAME)) {
        bankList.setName(type.get(FIELD_NAME));
      } else if (type.containsKey(FIELD_MEMO)) {
        bankList.setMemo(type.get(FIELD_MEMO));
      } else if ((type.containsKey(FIELD_LEDGERBAL) || type.containsKey(FIELD_INVPOSLIST))
          && (count == numberOfTransactions)) {
        bankAccTrxList.add(bankList);
        // all transactions have been loaded - skip the rest of the file
        break;
      }
    }
    return bankAccTrxList;
  }

  private int countTransactions() {
    int count = 0;
    for (Map<String, String> type : bankAccOfxList) {
      if (type.containsKey("STMTTRN") || type.containsKey("INVTRAN")) {
        count = count + 1;
      }
    }
    return count;
  }

}
