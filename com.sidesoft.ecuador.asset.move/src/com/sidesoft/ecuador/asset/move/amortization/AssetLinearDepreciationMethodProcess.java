/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html 
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License. 
 * The Original Code is Openbravo ERP. 
 * The Initial Developer of the Original Code is Openbravo SLU 
 * All portions are Copyright (C) 2013 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package com.sidesoft.ecuador.asset.move.amortization;

import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.CallableStatement;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.log4j.Logger;
import org.hibernate.Query;

import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.UtilSql;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.assetmgmt.Amortization;
import org.openbravo.model.financialmgmt.assetmgmt.AmortizationLine;
import org.openbravo.model.financialmgmt.assetmgmt.Asset;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

import com.sidesoft.ecuador.asset.changetranslation.SeactAssetSetup;

public class AssetLinearDepreciationMethodProcess extends DalBaseProcess {
  private static final Logger log4j = Logger.getLogger(AssetLinearDepreciationMethodProcess.class);
  private static final String LINEAR = "LI";
  private static final String TIME = "TI";
  private static final String PERCENTAGE = "PE";
  private static final String MONTH = "MO";
  private static final String YEAR = "YE";
  private static final BigDecimal HUNDRED = new BigDecimal("100");
  private static final BigDecimal THIRTY = new BigDecimal("30");
  private static final BigDecimal YEARDAYS = new BigDecimal("365");
  private static final MathContext mc = new MathContext(32, RoundingMode.HALF_UP);
  
  private DocumentType documentType;

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {
    OBError msg = new OBError();
    try {

      final String strAssetId = (String) bundle.getParams().get("A_Asset_ID");
      final Asset asset = (Asset) OBDal.getInstance().getProxy(Asset.ENTITY_NAME, strAssetId);
      
      OBCriteria<SeactAssetSetup> assetSetup = OBDal.getInstance().createCriteria(SeactAssetSetup.class);
      assetSetup.add(Restrictions.eq(SeactAssetSetup.PROPERTY_DEPRECIATION, true));
      if(assetSetup.list().size()==0) {
        throw new RuntimeException("@SEACT_DoctypeNotDefined@");
      }
      documentType = assetSetup.list().get(0).getDoctype();
      
      msg = generateAmortizationPlan(asset);

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Exception found in AssetLinearDepreciationMethodProcess process: ", e);

      Throwable ex = DbUtility.getUnderlyingSQLException(e);
      String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(message);

    } finally {
      bundle.setResult(msg);
    }
  }

  public OBError generateAmortizationPlan(Asset asset) throws Exception {
    OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));

    String strParentHaveChilds = null;
   try {
      strParentHaveChilds = getParentAndChild(2, asset.getId());

      if (strParentHaveChilds.equals("Y")) {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage(OBMessageUtils.messageBD("SSAM_PARENT_WITH_SON"));
        return msg;        
      }
    } catch (Exception e) {
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(OBMessageUtils.messageBD("Error al obtener información de activos hijos. "+e.getMessage()));
      return msg;
      
    }

    // INICIO Ticket 2710 A.M. 10/04/2018
    String strTypeofchange = null;
    String strParentID = null;
    Date dtStartdateParent = null;
    Date dtMaxEndDateLines = null;
    int intContador = 0;
    boolean booChangeValue = false;

    Date startDate = asset.getDepreciationStartDate();
    if (startDate == null) {
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(OBMessageUtils.messageBD("ASSET_MANDATORY_STARTDATE"));
      return msg;
    }

    booChangeValue = (asset.isSsamChangeofvalue() == null ? false : asset.isSsamChangeofvalue());
    if (booChangeValue) {

      strTypeofchange = asset.getSsamTypeofchange();
      if (strTypeofchange == null || strTypeofchange.equals("")) {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage(OBMessageUtils.messageBD("SSAM_TYPEOFCHANGE_NULL"));

        return msg;
      }
      try {
        strParentID = getParentAndChild(1, asset.getId());

        if (strParentID == null || strParentID.trim().equals("")
            || strParentID.trim().equals("0")) {
          msg.setType("Error");
          msg.setTitle(OBMessageUtils.messageBD("Error"));
          msg.setMessage(OBMessageUtils.messageBD("SSAM_PARENT_NULL"));
          return msg;
        }
      } catch (Exception e) {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage(
            OBMessageUtils.messageBD("Error al obtener ID del activo padre. " + e.getMessage()));
        return msg;
      }

      Asset assetparent;
      try {
        assetparent = OBDal.getInstance().get(Asset.class, strParentID);

        dtStartdateParent = assetparent.getDepreciationStartDate();

        if (dtStartdateParent.equals(startDate) || dtStartdateParent.after(startDate)) {
          msg.setType("Error");
          msg.setTitle(OBMessageUtils.messageBD("Error"));
          msg.setMessage(OBMessageUtils.messageBD("La fecha de inicio del activo '"
              + assetparent.getName() + "' es posterior o igual a la fecha de revaluación."));
          return msg;
        }
      } catch (Exception e) {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage(OBMessageUtils
            .messageBD("Error al obtener fecha de inicio de los activos. " + e.getMessage()));
        return msg;
      }
      try {
        dtMaxEndDateLines = getMaxDate(assetparent.getId());

      } catch (Exception e) {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage(OBMessageUtils
            .messageBD("Error al obtener fecha de inicio de los activos. " + e.getMessage()));
        return msg;
      }

      try {
        intContador = getAmortizationProcessed(assetparent.getId(), startDate);
        if (intContador > 0) {
          msg.setType("Error");
          msg.setTitle(OBMessageUtils.messageBD("Error"));
          msg.setMessage(OBMessageUtils.messageBD("El activo hijo '" + asset.getName()
              + "' no puede depreciarse en un periodo ya procesado por el activo padre '"
              + assetparent.getName() + "'."));
          return msg;
        }
      } catch (Exception e) {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage(OBMessageUtils.messageBD(
            "Error al obtener registros ya procesados del activo padre. " + e.getMessage()));
        return msg;
      }
      // Eliminación de líneas padre
      try {
        String strRespuesta = deleteParentLines(assetparent.getId(), startDate);
        if (!strRespuesta.equals("OK")) {
          msg.setType("Error");
          msg.setTitle(OBMessageUtils.messageBD("Error"));
          msg.setMessage(OBMessageUtils
              .messageBD("Error al eliminar líneas de amortización del activo padre. "));
          return msg;
        }
      } catch (Exception e) {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage(OBMessageUtils.messageBD(
            "Error al eliminar líneas de amortización del activo padre. " + e.getMessage()));
        return msg;
      }
      // Actualización cabecera padre a cabecera hijo
      try {
        Asset asset2 = OBDal.getInstance().get(Asset.class, asset.getId());
        BigDecimal DepreciatedAmt = assetparent.getDepreciatedValue().add(assetparent.getPreviouslyDepreciatedAmt());
        asset2.setPurchaseDate(assetparent.getPurchaseDate());
        asset2.setPreviouslyDepreciatedAmt(DepreciatedAmt);   
        BigDecimal ValueInBooks = asset2.getAssetValue().subtract(asset2.getDepreciatedValue()).subtract(asset2.getPreviouslyDepreciatedAmt());   
        asset2.setSsamValueInBooks(ValueInBooks);
        OBDal.getInstance().save(asset2);
        OBDal.getInstance().flush();
        // OBDal.getInstance().commitAndClose();
      } catch (Exception e) {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage(OBMessageUtils
            .messageBD("Error al actualizar fecha de compra, depreciación anterior y valor en libros."));
        return msg;
      }
    }
    // FIN Ticket 2710 A.M. 10/04/2018

    // =========== Read asset properties ===========

    log4j.debug("A_ASSET_POST: " + asset.getName() + " - " + asset.getDocumentNo());

    Calendar calStart = Calendar.getInstance();
    calStart.setTime(startDate);
    calStart.set(Calendar.HOUR, 0);
    calStart.set(Calendar.MINUTE, 0);
    calStart.set(Calendar.SECOND, 0);
    startDate = calStart.getTime();

    // End date is calculated based on depreciation type
    Calendar calRealEnd = Calendar.getInstance();
    calRealEnd.setTime(startDate);
    String depreciationType = asset.getDepreciationType(); // Linear
    String calculateType = asset.getCalculateType(); // Time or Percentage
    String amortizationFrequency = asset.getAmortize(); // Monthly or Yearly
    BigDecimal uselifeMonths = asset.getUsableLifeMonths() == null ? BigDecimal.ZERO
        : new BigDecimal(asset.getUsableLifeMonths());
    boolean is30DayMonth = asset.isEveryMonthIs30Days();
    boolean is365DayYear = asset.isEveryMonthIs30Days();
    BigDecimal uselifeYears = asset.getUsableLifeYears() == null ? BigDecimal.ZERO
        : new BigDecimal(asset.getUsableLifeYears());
    BigDecimal annualDepreciation = asset.getAnnualDepreciation() == null ? BigDecimal.ZERO : asset
        .getAnnualDepreciation();
    BigDecimal depreciationAmount = asset.getDepreciationAmt() == null ? BigDecimal.ZERO : asset
        .getDepreciationAmt();
    BigDecimal previouslyDepreciatedAmount = asset.getPreviouslyDepreciatedAmt() == null ? BigDecimal.ZERO
        : asset.getPreviouslyDepreciatedAmt();

    BigDecimal totalPeriods = BigDecimal.ZERO; // Total amortization lines (records in the tab)
    BigDecimal amountPerPeriod = BigDecimal.ZERO; // Amount to depreciate per period (month or year)

    List<AmortizationLine> amortizationLineList = asset.getFinancialMgmtAmortizationLineList();
    BigDecimal alreadyAmortizedPeriods = new BigDecimal(amortizationLineList.size());
    // When recalculating the amortization the asset can be partially amortized
    BigDecimal alreadyAmortizedAmount = BigDecimal.ZERO;
    Calendar maxAmortizationDateCal = (Calendar) calStart.clone();
    for (AmortizationLine al : amortizationLineList) {
      alreadyAmortizedAmount = alreadyAmortizedAmount.add(al.getAmortizationAmount());
      Date amortizationEndDate = al.getAmortization().getEndingDate();
      if (amortizationEndDate.compareTo(maxAmortizationDateCal.getTime()) > 0) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(amortizationEndDate);
        // Add one because you want to new the next day
        cal.add(Calendar.DATE, 1);
        maxAmortizationDateCal = cal;
      }
    }

    // Total amount to be depreciated (including already depreciated amount)
    BigDecimal amount = depreciationAmount.subtract(previouslyDepreciatedAmount);
    // Pending amount to be depreciated
    BigDecimal pendingAmountToAmortize = amount.subtract(alreadyAmortizedAmount);
    BigDecimal totalDays = BigDecimal.ZERO;

    boolean isMonthly = false;
    boolean isYearly = false;
    boolean isPercentage = false;

    // Validation activo enajenado
    boolean alienated = asset.isSsamAlienated();

    if (alienated) {
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(OBMessageUtils.messageBD("ssam_assetalienated"));
      return msg;
    }

    // =========== Validations ===========

    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(String.format(OBMessageUtils.messageBD("ASSET_MANDATORY_POSSITIVE"),
          OBMessageUtils.messageBD("ASSET_DEPRECIATION_AMOUNT")));
      return msg;
    }
    if (pendingAmountToAmortize.compareTo(BigDecimal.ZERO) <= 0) {
      msg.setType("Warning");
      msg.setTitle("");
      msg.setMessage(OBMessageUtils.messageBD("ASSET_FULLY_DEPRECIATED"));
      return msg;
    }
    if (LINEAR.equals(depreciationType) && PERCENTAGE.equals(calculateType)
        && annualDepreciation.compareTo(BigDecimal.ZERO) <= 0) {
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(String.format(OBMessageUtils.messageBD("ASSET_MANDATORY_POSSITIVE"),
          OBMessageUtils.messageBD("ASSET_ANNUAL_DEPRECIATION")));
      return msg;
    }
    if (TIME.equals(calculateType) && MONTH.equals(amortizationFrequency)
        && uselifeMonths.compareTo(BigDecimal.ZERO) <= 0) {
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(String.format(OBMessageUtils.messageBD("ASSET_MANDATORY_POSSITIVE"),
          OBMessageUtils.messageBD("ASSET_USABLE_LIFE_MONTHS")));
      return msg;
    }
    if (TIME.equals(calculateType) && YEAR.equals(amortizationFrequency)
        && uselifeYears.compareTo(BigDecimal.ZERO) <= 0) {
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(String.format(OBMessageUtils.messageBD("ASSET_MANDATORY_POSSITIVE"),
          OBMessageUtils.messageBD("ASSET_USABLE_LIFE_YEARS")));
      return msg;
    }
    if (asset.getCurrency() == null) {
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(OBMessageUtils.messageBD("ASSET_CURRENCY_MANDATORY"));
      return msg;
    }
    int stdPrecision = 2;
    try {
      OBContext.setAdminMode(true);
      stdPrecision = asset.getCurrency().getStandardPrecision().intValue();
    } finally {
      OBContext.restorePreviousMode();
    }

    // =========== Calculate amount to amortize per period ===========
    if (LINEAR.equals(depreciationType)) {
      if (PERCENTAGE.equals(calculateType)) {
        isPercentage = true;
        totalPeriods = HUNDRED.divide(annualDepreciation, RoundingMode.CEILING);
        // If the depreciation start date is not the first day of the period increment by 1 the
        // number of periods
        totalPeriods = (calStart.get(Calendar.DAY_OF_MONTH) == 1
            && calStart.get(Calendar.MONTH) == 0) ? totalPeriods : totalPeriods.add(BigDecimal.ONE);
        amountPerPeriod = amount.multiply(annualDepreciation, mc).divide(HUNDRED, mc);

      } else if (TIME.equals(calculateType)) {
        if (MONTH.equals(amortizationFrequency)) {
          // Calculate real end date
          // From: 01/01/2013, 2 months, calRealdEnd = 01/03/2012
          calRealEnd.add(Calendar.MONTH, uselifeMonths.intValue());
          isMonthly = true;
          // If the depreciation start date is not the first day of the period increment by 1 the
          // number of periods
          totalPeriods = (calStart.get(Calendar.DAY_OF_MONTH) == 1) ? uselifeMonths
              : uselifeMonths.add(BigDecimal.ONE);

          if (is30DayMonth) {
            // Calculate total days proportional to months of 30 days
            if (alreadyAmortizedPeriods.compareTo(BigDecimal.ZERO) > 0) {
              // totalDays = calculateNumberOfDaysIn30DayMonths(maxAmortizationDateCal, calRealEnd);
              totalDays = getDaysBetweenProportionalPeriods(maxAmortizationDateCal, calRealEnd,
                  isYearly, isMonthly);
            } else {
              totalDays = uselifeMonths.multiply(THIRTY, mc);
            }
          } else {
            // Calculate total natural days
            totalDays = new BigDecimal(
                FIN_Utility.getDaysBetween(maxAmortizationDateCal.getTime(), calRealEnd.getTime()));
          }

          if (totalDays.compareTo(BigDecimal.ZERO) == 0) {
            return msg;
          }

          // From: 01/01/2013, 2 months, calRealdEnd = 01/03/2012
          // Decrease by one. Last day should be the last day (included) of the range (28/02/2013)
          calRealEnd.add(Calendar.DATE, -1);

        } else if (YEAR.equals(amortizationFrequency)) {
          // Calculate real end date
          // From: 01/01/2013, 1 year, calRealdEnd = 01/01/2014
          calRealEnd.add(Calendar.YEAR, uselifeYears.intValue());
          isYearly = true;
          // If the depreciation start date is not the first day of the period increment by 1 the
          // number of periods
          totalPeriods = (calStart.get(Calendar.DAY_OF_MONTH) == 1
              && calStart.get(Calendar.MONTH) == 0) ? uselifeYears
                  : uselifeYears.add(BigDecimal.ONE);
          amountPerPeriod = amount.divide(uselifeYears, mc);

          if (is365DayYear) {
            // Calculate total days proportional to years of 365 days
            if (alreadyAmortizedPeriods.compareTo(BigDecimal.ZERO) > 0) {
              totalDays = getDaysBetweenProportionalPeriods(maxAmortizationDateCal, calRealEnd,
                  isYearly, isMonthly);
            } else {
              totalDays = uselifeYears.multiply(YEARDAYS, mc);
            }
          } else {
            // Calculate total natural days
            totalDays = new BigDecimal(
                FIN_Utility.getDaysBetween(maxAmortizationDateCal.getTime(), calRealEnd.getTime()));
          }

          if (totalDays.compareTo(BigDecimal.ZERO) == 0) {
            return msg;
          }

          // Decrease by one. Last day should be the last day (included) of the range (31/12/2013)
          // From: 01/01/2013, 1 year, calRealdEnd = 01/01/2014
          calRealEnd.add(Calendar.DATE, -1);

        } else {
          msg.setType("Error");
          msg.setTitle(OBMessageUtils.messageBD("Error"));
          msg.setMessage("Unsupported amortization frequency: " + amortizationFrequency);
          return msg;
        }
      } else {
        msg.setType("Error");
        msg.setTitle(OBMessageUtils.messageBD("Error"));
        msg.setMessage("Unsupported amortization calculation type: " + calculateType);
        return msg;
      }
    }

    // Calendar auxiliary variables
    Calendar calAux = Calendar.getInstance();
    calAux.setTime(startDate);
    // First and last period (month or year) day. For example, 2 month, starting on 17/01/12.
    // period 1: calFirstDayOfPeriod = 01/01/12, calLastDayOfPeriod = 31/01/12.
    // period 2: calFirstDayOfPeriod = 01/02/12, calLastDayOfPeriod = 29/02/12.
    // period 3: calFirstDayOfPeriod = 01/03/12, calLastDayOfPeriod = 31/03/12.
    Calendar calLastDayOfPeriod = Calendar.getInstance(); // Last period (month or year) day.
    Calendar calFirstDayOfPeriod = (Calendar) calStart.clone(); // First period (month or year) day.
    calFirstDayOfPeriod.set(Calendar.DAY_OF_MONTH, 1);
    if (YEAR.equals(amortizationFrequency)) {
      calFirstDayOfPeriod.set(Calendar.MONTH, 0);
    }

    int currentYear = calStart.get(Calendar.YEAR);
    int currentMonth = calStart.get(Calendar.MONTH);
    int currentMonthDay = calStart.get(Calendar.DAY_OF_MONTH);
    int currentYearDay = calAux.get(Calendar.DAY_OF_YEAR);
    int lastDayOfMonth = calStart.getActualMaximum(Calendar.DAY_OF_MONTH);
    int lastDayOfYear = calStart.getActualMaximum(Calendar.DAY_OF_YEAR);

    BigDecimal totalizedAmount = BigDecimal.ZERO;
    BigDecimal totalizedPercentage = BigDecimal.ZERO;
    BigDecimal proportionalAmount = BigDecimal.ZERO;
    BigDecimal contDays = BigDecimal.ZERO;
    int contPeriods = 0;
    boolean endOfRange = false;
    int maxDaysOfRange = 0;
    Long seqNoAsset = null;

    // Loop till full amount is depreciated
    for (; !calAux.after(calRealEnd)
        || (isPercentage && totalizedAmount.compareTo(amount) < 0); calAux.add(Calendar.DATE, 1)) {
      contDays = contDays.add(BigDecimal.ONE);
      if (currentYear != calAux.get(Calendar.YEAR)) {
        currentYear = calAux.get(Calendar.YEAR);
        lastDayOfYear = calAux.getActualMaximum(Calendar.DAY_OF_YEAR);
      }
      if (currentMonth != calAux.get(Calendar.MONTH)) {
        currentMonth = calAux.get(Calendar.MONTH);
        lastDayOfMonth = calAux.getActualMaximum(Calendar.DAY_OF_MONTH);
      }

      currentMonthDay = calAux.get(Calendar.DAY_OF_MONTH);
      currentYearDay = calAux.get(Calendar.DAY_OF_YEAR);

      // Month
      if (isMonthly && currentMonthDay == lastDayOfMonth) {
        endOfRange = true;
        maxDaysOfRange = lastDayOfMonth;
      }
      // Percentage or Year
      if ((isPercentage || isYearly) && currentYearDay == lastDayOfYear) {
        endOfRange = true;
        maxDaysOfRange = lastDayOfYear;
      }

      // Last day of range (last iteration in the loop)
      if (!isPercentage && calAux.compareTo(calRealEnd) == 0) {
        endOfRange = true;
        // Month
        if (isMonthly) {
          maxDaysOfRange = lastDayOfMonth;
        }
        // Percentage or Year
        if (isPercentage || isYearly) {
          maxDaysOfRange = lastDayOfYear;
        }
      }

      if (endOfRange) {
        contPeriods += 1;
        calLastDayOfPeriod = (Calendar) calAux.clone();
        calLastDayOfPeriod.set(Calendar.DAY_OF_MONTH,
            calAux.getActualMaximum(Calendar.DAY_OF_MONTH));
        if (isYearly) {
          calLastDayOfPeriod.set(Calendar.MONTH, calAux.getActualMaximum(Calendar.MONTH));
        }

        AmortizationLine amortizationLine = getAmortizationLine(asset, null,
            calLastDayOfPeriod.getTime());

        if (amortizationLine != null) {
          // Recalculate percentage because the amount could have been changed
          BigDecimal proportionaldPercentage = amortizationLine.getAmortizationAmount()
              .multiply(HUNDRED).divide(amount, stdPrecision, RoundingMode.HALF_UP);
          totalizedAmount = totalizedAmount.add(amortizationLine.getAmortizationAmount());
          totalizedPercentage = totalizedPercentage.add(proportionaldPercentage);
          if (amortizationLine.getAmortizationPercentage() != null && amortizationLine
              .getAmortizationPercentage().compareTo(proportionaldPercentage) != 0) {
            Amortization amortization = amortizationLine.getAmortization();
            boolean isAmortizationProcessed = false;
            if (amortization != null) {
              isAmortizationProcessed = "Y".equals(amortization.getProcessed());
            }
            if (isAmortizationProcessed) {
              // Avoid a_amortizationline_trg trigger error
              amortization.setProcessed("N");
              OBDal.getInstance().save(amortization);
              OBDal.getInstance().flush();
            }
            amortizationLine.setAmortizationPercentage(proportionaldPercentage);
            if (isAmortizationProcessed) {
              amortization.setProcessed("Y");
              OBDal.getInstance().save(amortization);
            }
          }

          OBDal.getInstance().save(amortizationLine);

        } else {
          // Calculate the proportional amount for current period (amortization line)
          if (isPercentage) {
            proportionalAmount = contDays.multiply(amountPerPeriod, mc)
                .divide(new BigDecimal(maxDaysOfRange), mc);
          } else {
            if (isMonthly && is30DayMonth) {
              contDays = contDays.multiply(THIRTY, mc).divide(new BigDecimal(maxDaysOfRange), mc);
            }
            if (isYearly && is365DayYear) {
              contDays = contDays.multiply(YEARDAYS, mc).divide(new BigDecimal(maxDaysOfRange), mc);
            }
            proportionalAmount = contDays.multiply(pendingAmountToAmortize, mc).divide(totalDays,
                mc);

          }

          // Calculate percentage
          BigDecimal proportionaldPercentage = proportionalAmount.multiply(HUNDRED)
              .divide(amount, mc).setScale(stdPrecision, RoundingMode.HALF_UP);

          // Round the amount after using it for percentage calculation
          proportionalAmount = proportionalAmount.setScale(stdPrecision, RoundingMode.HALF_UP);

          // Last period. Adjust for avoiding rounding issues.
          if (((!isPercentage && new BigDecimal(contPeriods).compareTo(totalPeriods) == 0))
              || totalizedAmount.add(proportionalAmount).compareTo(amount) > 0) {
            proportionalAmount = amount.subtract(totalizedAmount);
            proportionaldPercentage = HUNDRED.subtract(totalizedPercentage);
          }

          log4j.debug(OBDateUtils.formatDate(calFirstDayOfPeriod.getTime()) + " to "
              + OBDateUtils.formatDate(calLastDayOfPeriod.getTime()) + "  " + proportionalAmount
              + "  " + proportionaldPercentage);

          // Accumulate amount and percentage
          totalizedAmount = totalizedAmount.add(proportionalAmount);
          totalizedPercentage = totalizedPercentage.add(proportionaldPercentage);

          // Search for not processed amortization (calFirstDayOfPeriod - calLastDayOfPeriod)
          Amortization amortization = getAmortization(asset.getOrganization(), null,
              calLastDayOfPeriod.getTime(), asset.getProject());
          if (amortization == null) {
            amortization = createNewAmortization(asset.getOrganization(),
                OBDateUtils.formatDate(calLastDayOfPeriod.getTime()), null /* description */,
                calFirstDayOfPeriod.getTime(), calLastDayOfPeriod.getTime(), asset.getCurrency(),
                asset.getProject(), null /* campaign */, null /* activity */, null /* user1 */,
                null /* user2 */);
          }

          // Calculate asset sequence number.
          // Asset 1
          // January lineno = 10, seqnoasset = 10
          // February lineno = 10, seqnoasset = 20
          if (seqNoAsset == null) {
            seqNoAsset = getMaxSeqNoAsset(asset) + 10L;
          }

          // Calculate amortization line number because the amortization can already exists.
          Long lineNo = getMaxLineNo(amortization) + 10L;

          // Create the amortization line
          amortizationLine = createNewAmortizationLine(amortization, lineNo, seqNoAsset, asset,
              proportionaldPercentage, proportionalAmount, asset.getCurrency(), asset.getProject(),
              null /* campaign */, null /* activity */, null /* user1 */, null /* user2 */,
              null /* costcenter */, strTypeofchange);

          seqNoAsset += 10L;
        }

        // Initialize new range
        contDays = BigDecimal.ZERO;
        calFirstDayOfPeriod = (Calendar) calAux.clone();
        calFirstDayOfPeriod.add(Calendar.DAY_OF_MONTH, 1);
        endOfRange = false;
      }
    }

    asset.setProcessed("Y");
    asset.setProcessAsset("Y");
    asset.setDepreciatedPlan(amount);
    OBDal.getInstance().save(asset);

    OBDal.getInstance().flush();
    return msg;
  }

  /**
   * Get the amortization period matching given start/end date, organization and project. The
   * amortization period must be not processed.
   * 
   * @param org
   *          Organization.
   * @param startDate
   *          Start date.
   * @param endDate
   *          End date.
   * @return amortization period matching given start/end date, organization and project.
   */
  private Amortization getAmortization(Organization org, Date startDate, Date endDate,
      Project project) {
    OBCriteria<Amortization> obc = OBDal.getInstance().createCriteria(Amortization.class);
    obc.add(Restrictions.eq(Amortization.PROPERTY_ORGANIZATION, org));
    if (startDate != null) {
      obc.add(Restrictions.eq(Amortization.PROPERTY_STARTINGDATE, startDate));
    }
    obc.add(Restrictions.eq(Amortization.PROPERTY_ENDINGDATE, endDate));
    obc.add(Restrictions.eq(Amortization.PROPERTY_PROCESSED, "N"));
    if (project != null) {
      obc.add(Restrictions.eq(Amortization.PROPERTY_PROJECT, project));
    } else {
      obc.add(Restrictions.isNull(Amortization.PROPERTY_PROJECT));
    }
    obc.setFilterOnReadableOrganization(false);
    List<Amortization> amortizationList = obc.list();
    if (amortizationList.size() == 0) {
      return null;
    } else if (amortizationList.size() > 1) {
      throw new OBException("Existe más de una cabecera de depreciación con fechas desde: " + (startDate == null ? " null "
          : startDate.toString()) + " hasta: "+ (endDate == null ? " null ": endDate.toString()) + " para la organización: " +(org == null ? " null ": org.getName()));
    }
    return amortizationList.get(0);
  }

  /**
   * Get the amortization line associated to the given asset and included in the amortization for
   * the given period range.
   * 
   * @param asset
   *          Asset.
   * @param startDate
   *          Start date.
   * @param endDate
   *          End date.
   * @return Amortization line associated to the given asset and included in the amortization for
   *         the given period range.
   * @throws OBException
   *           If more than one amortization line exists for the given period.
   */
  private AmortizationLine getAmortizationLine(Asset asset, Date startDate, Date endDate)
      throws OBException {
    StringBuilder whereClause = new StringBuilder();
    final List<Object> parameters = new ArrayList<Object>();
    whereClause.append(" as aml join aml.amortization as am ");
    whereClause.append(" where aml.asset.id = ? ");
    if (startDate != null) {
      whereClause.append("       and am.startingDate = ?");
    }
    whereClause.append("       and am.endingDate = ?");
    final OBQuery<AmortizationLine> obq = OBDal.getInstance().createQuery(AmortizationLine.class,
        whereClause.toString());
    obq.setFilterOnReadableOrganization(false);
    parameters.add(asset.getId());
    if (startDate != null) {
      parameters.add(startDate);
    }
    parameters.add(endDate);
    obq.setParameters(parameters);
    List<AmortizationLine> amortizationLineList = obq.list();
    if (amortizationLineList.size() == 0) {
      return null;
    } else if (amortizationLineList.size() > 1) {
      throw new OBException("Existe más de una línea de depreciación con fechas desde: " + (startDate == null ? " null "
          : startDate.toString()) + " hasta: "+ (endDate == null ? " null ": endDate.toString()) + " para el activo: " +(asset == null ? " null ": asset.getName()));
    }
    return amortizationLineList.get(0);
  }

  /**
   * Create a new amortization.
   * 
   * @return Amortization
   */
  private Amortization createNewAmortization(Organization organization, String name,
      String description, Date startDate, Date endDate, Currency currency, Project project,
      Campaign campaign, ABCActivity activity, UserDimension1 user1, UserDimension2 user2) {
    Amortization am = OBProvider.getInstance().get(Amortization.class);
    am.setOrganization(organization);
    am.setName(name);
    am.setDescription(description);
    am.setStartingDate(startDate);
    am.setEndingDate(endDate);
    am.setAccountingDate(endDate);
    am.setCurrency(currency);
    am.setProject(project);
    am.setSalesCampaign(campaign);
    am.setActivity(activity);
    am.setStDimension(user1);
    am.setNdDimension(user2);
    am.setDocumentType(documentType);

    OBDal.getInstance().save(am);
    return am;
  }

  /**
   * Create a new amortization line.
   * 
   * @return Amortization line.
   */
  private AmortizationLine createNewAmortizationLine(Amortization amortization, Long lineNo,
      Long assetSeqNo, Asset asset, BigDecimal amortizationPercentage,
      BigDecimal amortizationAmount, Currency currency, Project project, Campaign campaign,
      ABCActivity activity, UserDimension1 user1, UserDimension2 user2, Costcenter costCenter,
      String srtTypeofChange) {
    AmortizationLine aml = OBProvider.getInstance().get(AmortizationLine.class);
    aml.setOrganization(amortization.getOrganization());
    aml.setAmortization(amortization);
    aml.setLineNo(lineNo);
    aml.setSEQNoAsset(assetSeqNo);
    aml.setAsset(asset);
    aml.setAmortizationPercentage(amortizationPercentage);
    aml.setAmortizationAmount(amortizationAmount);
    aml.setCurrency(currency);
    aml.setProject(project);
    aml.setStDimension(user1);
    aml.setNdDimension(user2);
    aml.setCostcenter(costCenter);
    aml.setSsamAAssetGroup(asset.getAssetCategory());
    aml.setSSAMTypeOfChange(srtTypeofChange);
    OBDal.getInstance().save(aml);
    amortization.getFinancialMgmtAmortizationLineList().add(aml);
    OBDal.getInstance().save(amortization);
    return aml;
  }

  /**
   * Calculate total days between given dates being the months of 30 days and years of 365 (no
   * leap-years).
   * 
   * @param startDate
   *          Start date.
   * @param endDate
   *          End date.
   * @param isYearly
   *          Is yearly amortization.
   * @param isMonthly
   *          Is monthly amortization.
   * @return total days between given dates being the months of 30 days and years of 365 (no
   *         leap-years). The result can have decimals.
   */
  private BigDecimal getDaysBetweenProportionalPeriods(Calendar startDate, Calendar _endDate,
      boolean isYearly, boolean isMonthly) {
    Calendar calAux = (Calendar) startDate.clone();
    Calendar endDate = (Calendar) _endDate.clone();
    endDate.add(Calendar.DATE, -1);
    int currentYear = startDate.get(Calendar.YEAR);
    int currentMonth = startDate.get(Calendar.MONTH);
    int currentMonthDay = startDate.get(Calendar.DAY_OF_MONTH);
    int currentYearDay = startDate.get(Calendar.DAY_OF_YEAR);
    int lastDayOfMonth = startDate.getActualMaximum(Calendar.DAY_OF_MONTH);
    int lastDayOfYear = startDate.getActualMaximum(Calendar.DAY_OF_YEAR);
    BigDecimal totalDays = BigDecimal.ZERO;
    boolean endOfRange = false;
    for (; !calAux.after(endDate); calAux.add(Calendar.DATE, 1)) {
      if (currentYear != calAux.get(Calendar.YEAR)) {
        currentYear = calAux.get(Calendar.YEAR);
        lastDayOfYear = calAux.getActualMaximum(Calendar.DAY_OF_YEAR);
      }
      if (currentMonth != calAux.get(Calendar.MONTH)) {
        currentMonth = calAux.get(Calendar.MONTH);
        lastDayOfMonth = calAux.getActualMaximum(Calendar.DAY_OF_MONTH);
      }

      currentMonthDay = calAux.get(Calendar.DAY_OF_MONTH);
      currentYearDay = calAux.get(Calendar.DAY_OF_YEAR);

      // Month
      if (isMonthly && currentMonthDay == lastDayOfMonth) {
        endOfRange = true;
      }
      // Year
      if (isYearly && currentYearDay == lastDayOfYear) {
        endOfRange = true;
      }

      // Last day of range (last iteration in the loop)
      if (calAux.compareTo(endDate) == 0) {
        endOfRange = true;
      }

      if (endOfRange) {
        BigDecimal proportionalDays = BigDecimal.ZERO;
        if (isMonthly) {
          proportionalDays = THIRTY.multiply(new BigDecimal(currentMonthDay), mc)
              .divide(new BigDecimal(lastDayOfMonth), mc);

        } else if (isYearly) {
          proportionalDays = YEARDAYS.multiply(new BigDecimal(currentYearDay), mc)
              .divide(new BigDecimal(lastDayOfYear), mc);
        }

        totalDays = totalDays.add(proportionalDays);
        endOfRange = false;
      }
    }

    return totalDays;
  }

  /**
   * Get max asset sequence number in amortization lines related to the given asset.
   * 
   * @param asset
   *          Asset.
   * @return Max asset sequence number in amortization lines related to the given asset. 0 if there
   *         is no amortization line related to the given asset.
   */
  private Long getMaxSeqNoAsset(Asset asset) {
    StringBuilder hql = new StringBuilder();
    hql.append(" select coalesce(max(al.sEQNoAsset), 0) as maxSeqNoAsset ");
    hql.append(" from FinancialMgmtAmortizationLine as al where al.asset.id = ? ");
    Query query = OBDal.getInstance().getSession().createQuery(hql.toString());
    query.setString(0, (String) DalUtil.getId(asset));
    for (Object obj : query.list()) {
      if (obj != null) {
        return (Long) obj;
      }
    }
    return 0l;
  }

  /**
   * Get max asset sequence number in amortization lines that belong to the given amortization.
   * 
   * @param amortization
   *          Amortization.
   * @return max asset sequence number in amortization lines that belong to the given amortization.
   *         0 if there is no amortization line related to the given amortization.
   */
  private Long getMaxLineNo(Amortization amortization) {
    StringBuilder hql = new StringBuilder();
    hql.append(" select coalesce(max(al.lineNo), 0) as maxSeqNoAsset ");
    hql.append(" from FinancialMgmtAmortizationLine as al where al.amortization.id = ? ");
    Query query = OBDal.getInstance().getSession().createQuery(hql.toString());
    query.setString(0, (String) DalUtil.getId(amortization));
    for (Object obj : query.list()) {
      if (obj != null) {
        return (Long) obj;
      }
    }
    return 0l;
  }

  public static String getParentAndChild(int intKeyToReturn, String strRecordSon)
      throws OBException {
    OBError msg = new OBError();
    ConnectionProvider connectionProvider = new DalConnectionProvider(false);
    String strSql = "";
    strSql = strSql + "       SELECT ssam_get_record_parent(?,?) as idparent   FROM dual";

    ResultSet result;
    String strReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      st.setInt(1, intKeyToReturn);
      st.setString(2, strRecordSon);
      result = st.executeQuery();
      if (result.next()) {
        strReturn = UtilSql.getValue(result, "idparent");
      }
      result.close();
      st.close();
    }   catch (Exception e) {

      throw new OBException(e.getMessage());
   
      
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (strReturn);
  }

  public static Date getMaxDate(String strParentID) throws ServletException {
    ConnectionProvider connectionProvider = new DalConnectionProvider(false);
    String strSql = "";
    strSql = strSql
        + "SELECT MAX(AM.enddate) as enddate FROM A_Amortizationline AL LEFT JOIN A_Amortization AM ON AL.A_Amortization_id =AM.A_Amortization_id WHERE AL.A_ASSET_ID=?";

    ResultSet result;
    Date dtReturn = null;
    PreparedStatement st = null;

    try {
      st = connectionProvider.getPreparedStatement(strSql);
      st.setString(1, strParentID);
      result = st.executeQuery();
      if (result.next()) {
        dtReturn = result.getTimestamp("enddate");
      }
      result.close();
      st.close();
    } catch (SQLException e) {

      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {

      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (dtReturn);
  }

  public static int getAmortizationProcessed(String strParentID, Date dtStartDateSon)
      throws ServletException {
    ConnectionProvider connectionProvider = new DalConnectionProvider(false);
    String strSql = "";
    strSql = "  SELECT  COALESCE(COUNT(*),0) AS contador" + "  FROM A_AMORTIZATIONLINE AL "
        + "  INNER JOIN A_AMORTIZATION AM ON AL.A_AMORTIZATION_ID = AM.A_AMORTIZATION_ID    "
        + "  WHERE AL.A_ASSET_ID =?"
        + "  AND ? >= AM.STARTDATE AND ? <= AM.DateAcct AND AM.PROCESSED='Y'";

    ResultSet result;
    int intReturn = 0;
    PreparedStatement st = null;
    java.sql.Date sqlDate = null;
    try {
      sqlDate = new java.sql.Date(dtStartDateSon.getTime());
    } catch (Exception e) {

      throw new ServletException("@CODE=@" + e.getMessage());
    }

    try {

      st = connectionProvider.getPreparedStatement(strSql);
      st.setString(1, strParentID);
      st.setDate(2, sqlDate);
      st.setDate(3, sqlDate);

      result = st.executeQuery();
      if (result.next()) {
        intReturn = result.getInt("contador");
      }
      result.close();
      st.close();
    } catch (SQLException e) {

      throw new ServletException(
          "@CODE=" + Integer.toString(e.getErrorCode()) + "@" + e.getMessage());
    } catch (Exception ex) {

      throw new ServletException("@CODE=@" + ex.getMessage());
    } finally {
      try {
        connectionProvider.releasePreparedStatement(st);
      } catch (Exception ignore) {
        ignore.printStackTrace();
      }
    }
    return (intReturn);
  }

  public String deleteParentLines(String strParentID, Date dtStartDateSon) {
    try {
      org.openbravo.database.ConnectionProvider cp = new DalConnectionProvider(false);
      CallableStatement cs = cp.getConnection()
          .prepareCall("{call ssam_delete_amortization_lines(?,?)}");

      java.sql.Date sqlDate = null;
      try {
        sqlDate = new java.sql.Date(dtStartDateSon.getTime());
      } catch (Exception e) {

        throw new ServletException("@CODE=@" + e.getMessage());
      }
      cs.setDate(1, sqlDate);
      cs.setString(2, strParentID);
      cs.execute();
      cs.close();
      return "OK";
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return e.getMessage().toString();
    }

  }

}
