/*
 ************************************************************************************
 * Copyright (C) 2012-2016 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

/*global OB, moment */

(function () {

  // Mockup for OB.I18N
  OB.I18N = window.OB.I18N || {};

  // Quantity scale.
  OB.I18N.qtyScale = function () {
    return OB.Format.formats.qtyEdition.length - OB.Format.formats.qtyEdition.indexOf('.') - 1;
  };

  OB.I18N.formatCurrency = function (number) {
    var maskNumeric = OB.Format.formats.priceInform,
        decSeparator = OB.Format.defaultDecimalSymbol,
        groupSeparator = OB.Format.defaultGroupingSymbol,
        groupInterval = OB.Format.defaultGroupingSize;

    maskNumeric = maskNumeric.replace(',', 'dummy').replace('.', decSeparator).replace('dummy', groupSeparator);

    return OB.Utilities.Number.JSToOBMasked(number, maskNumeric, decSeparator, groupSeparator, groupInterval);
  };

  OB.I18N.formatCurrencyWithSymbol = function (number, symbol, currencySymbolToTheRight) {
    if (currencySymbolToTheRight) {
      return OB.I18N.formatCurrency(number) + symbol;
    } else {
      return symbol + OB.I18N.formatCurrency(number);
    }
  };

  OB.I18N.formatCoins = function (number) {
    var val = OB.I18N.formatCurrency(number);
    var decSeparator = OB.Format.defaultDecimalSymbol;
    return val.replace(new RegExp('[' + decSeparator + '][0]+$'), '');
  };

  OB.I18N.formatRate = function (number) {
    var symbol = '%',
        maskNumeric = OB.Format.formats.taxInform || OB.Format.formats.euroEdition,
        decSeparator = OB.Format.defaultDecimalSymbol,
        groupSeparator = OB.Format.defaultGroupingSymbol,
        groupInterval = OB.Format.defaultGroupingSize;

    maskNumeric = maskNumeric.replace(',', 'dummy').replace('.', decSeparator).replace('dummy', groupSeparator);

    var formattedNumber = OB.Utilities.Number.JSToOBMasked(number, maskNumeric, decSeparator, groupSeparator, groupInterval);
    formattedNumber = formattedNumber + symbol;
    return formattedNumber;
  };

  /*
   * Try to convert the provided argument 'anyDateObject' to a valid date
   * Valid values are:
   *   - null (the default value for an instantiated date variable)
   *   - any valid date in any of the different valid strings
   * Returns a normalized date string (ISO) or null if the value has been found imposible to be converted to a valid date
   * Accepts any value as argument (undefined, null, '', 'adsf', '333', '2015 08 06', 'Thu Aug 06 2015 12:17:04 GMT+0200 (CEST)', '2015-08-06T10:17:04.000Z', etc)
   */
  OB.I18N.normalizeDate = function (anyDateObject) {

    function defaultValue() {
      return null;
    }

    if (anyDateObject === null) {
      return defaultValue();
    }
    var anyDateObjectStringifyed = OB.UTIL.argumentsToStringifyed(anyDateObject);
    if (!anyDateObject) {
      OB.error("The date provided '" + anyDateObjectStringifyed + "' is not a valid date object");
      return defaultValue();
    }
    var normalizedDate = new Date(anyDateObject);
    try {
      if (normalizedDate.toISOString() !== normalizedDate.toJSON()) {
        OB.error("The date object '" + OB.UTIL.argumentsToStringifyed(normalizedDate) + "' generates different strings if 'toISOString' or 'toJSON' are used. It is important that they should generate the same string because there are dates that will be converted to json and will have to be converted back to a valid date");
        return defaultValue();
      }
    } catch (e) {
      OB.error("The date provided '" + anyDateObjectStringifyed + "' is not a valid date object. Error: '" + e + "'");
      return defaultValue();
    }
    if (normalizedDate === new Date(null)) {
      OB.error("The date provided '" + anyDateObjectStringifyed + "' is not a valid date object");
      return defaultValue();
    }
    return normalizedDate.toISOString();
  };

  OB.I18N.formatDate = function (JSDate) {
    if (!OB.Format) {
      OB.error("OB.I18N.formatDate() requires OB.Format to be initialized");
      return null;
    }
    if (!OB.Format.date) {
      OB.error("OB.I18N.formatDate() requires OB.Format.date to be initialized");
      return null;
    }
    var dateFormat = OB.Format.date;
    if (OB.Utilities && OB.Utilities.Date) {
      return OB.Utilities.Date.JSToOB(JSDate, dateFormat);
    } else {
      return JSDate;
    }
  };

  OB.I18N.formatDateISO = function (d) {
    if (typeof (d) === "string") {
      var date = new Date(d);
      if (date.toISOString() === d) {
        d = date;
      } else {
        OB.error("The argument must be a date or an iso string");
        return;
      }
    }
    var curr_date = d.getDate();
    var curr_month = d.getMonth();
    var curr_year = d.getFullYear();
    var curr_hour = d.getHours();
    var curr_min = d.getMinutes();
    var curr_sec = d.getSeconds();
    var curr_mill = d.getMilliseconds();

    return OB.UTIL.padNumber(curr_year, 4) + '-' + OB.UTIL.padNumber(curr_month + 1, 2) + '-' + OB.UTIL.padNumber(curr_date, 2) + ' ' + OB.UTIL.padNumber(curr_hour, 2) + ':' + OB.UTIL.padNumber(curr_min, 2) + ':' + OB.UTIL.padNumber(curr_sec, 2) + '.' + OB.UTIL.padNumber(curr_mill, 3);
  };

  OB.I18N.getNormalizedDateFromFormatedString = function (valueToConvert, format) {
    var newFormat = format || OB.Format.date.toUpperCase();
    var isValidDate;
    var momentDate;
    isValidDate = moment(valueToConvert, newFormat, true).isValid();
    if (isValidDate) {
      momentDate = moment(valueToConvert, newFormat, true);
      return OB.I18N.normalizeDate(momentDate.toDate());
    } else {
      OB.error('String (' + valueToConvert + ') with format (' + newFormat + ') cannot be normalized');
      return null;
    }
  };

  OB.I18N.formatHour = function (d, includeSeconds) {
    if (typeof (d) === "string") {
      var date = new Date(d);
      if (date.toISOString() === d) {
        d = date;
      } else {
        OB.error("The argument must be a date or an iso string");
        return;
      }
    }
    var curr_hour = d.getHours();
    var curr_min = d.getMinutes();
    var curr_sec = d.getSeconds();
    var formattedHour = OB.UTIL.padNumber(curr_hour, 2) + ':' + OB.UTIL.padNumber(curr_min, 2);
    if (includeSeconds) {
      formattedHour += ":" + OB.UTIL.padNumber(curr_sec, 2);
    }
    return formattedHour;
  };

  OB.I18N.parseServerDate = function (d) {
    // for example if server and client are in different time zones
    // parserServerDate("2014-02-05T00:00:00+01:00") returns Wed Feb 05 2014 00:00:00 GMT-0500 (CET)
    return moment(d, "YYYY-MM-DD").toDate();
  };


  OB.I18N.parseNumber = function (s) {
    if (OB.Format.defaultDecimalSymbol !== '.') {
      s = s.toString();
      while (s.indexOf(OB.Format.defaultDecimalSymbol) !== -1) {
        s = s.replace(OB.Format.defaultDecimalSymbol, '.');
      }
    }
    return parseFloat(s, 10);
  };

  OB.I18N.isValidNumber = function (number) {
    var type = {};
    type.maskNumeric = OB.Format.formats.priceInform;
    type.decSeparator = OB.Format.defaultDecimalSymbol;
    type.groupSeparator = OB.Format.defaultGroupingSymbol;
    return OB.Utilities.Number.IsValidValueString(type, number);
  };

  OB.I18N.getLabel = function (key, params, object, property) {
    if (key === '') {
      return '';
    }

    if (key.indexOf('OBUIAPP_GroupBy') === 0) {
      // Don't show error for GroupBy* labels, they are used by core's decimal
      // but don't really needed
      return '';
    }
    if (!OB.I18N.labels) {
      OB.UTIL.Debug.execute(function () {
        OB.error('labels not loaded. key asked: ' + key);
      });
      return 'UNDEFINED ' + key;
    }
    if (!OB.I18N.labels[key]) {
      OB.UTIL.Debug.execute(function () {
        OB.warn('not found label: ' + key);
      });
      return 'UNDEFINED ' + key;
    }
    var label = OB.I18N.labels[key],
        i;
    if (params && params.length && params.length > 0) {
      for (i = 0; i < params.length; i++) {
        label = label.replace("%" + i, params[i]);
      }
    }
    if (object && property) {
      if (Object.prototype.toString.call(object[property]) === '[object Function]') {
        object[property](label);
      } else {
        object[property] = label;
      }
    }
    return label;
  };

  OB.I18N.hasLabel = function (key, params, object, property) {
    return OB.I18N.labels[key] ? true : false;
  };

  OB.I18N.getDateFormatLabel = function () {
    var year, month, day, label = '',
        i, char, format;
    year = OB.I18N.getLabel('OBMOBC_YearCharLbl');
    month = OB.I18N.getLabel('OBMOBC_MonthCharLbl');
    day = OB.I18N.getLabel('OBMOBC_DayCharLbl');
    format = OB.Format.date.toLowerCase();
    for (i = 0; i < format.length; i++) {
      char = format[i];
      switch (char) {
      case 'y':
        label += year;
        break;
      case 'm':
        label += month;
        break;
      case 'd':
        label += day;
        break;
      default:
        label += char;
      }
    }
    return label;
  };

  OB.I18N.lists = {};

  OB.I18N.getLocalizedList = function (listname) {
    if (!OB.I18N.lists[listname]) {
      OB.I18N.lists[listname] = OB.I18N.getLabel(listname).split('_');
    }
    return OB.I18N.lists[listname];
  };

  OB.I18N.getLocalizedListElement = function (listname, i) {
    return OB.I18N.getLocalizedList(listname)[i];
  };

  OB.I18N.getMonthsList = function () {
    return OB.I18N.getLocalizedList('OBMOBC_Date_Months');
  };

  OB.I18N.getMonth = function (i) { // Starts with January
    return OB.I18N.getLocalizedListElement('OBMOBC_Date_Months', i);
  };

  OB.I18N.getMonthsShortList = function () {
    return OB.I18N.getLocalizedList('OBMOBC_Date_Months_Short');
  };

  OB.I18N.getMonthShort = function (i) { // Starts with Jan
    return OB.I18N.getLocalizedListElement('OBMOBC_Date_Months_Short', i);
  };

  OB.I18N.getWeekdaysList = function () {
    return OB.I18N.getLocalizedList('OBMOBC_Date_Weekdays');
  };

  OB.I18N.getWeekday = function (i) { // Starts with Sunday
    return OB.I18N.getLocalizedListElement('OBMOBC_Date_Weekdays', i);
  };

  OB.I18N.getWeekdaysShortList = function () {
    return OB.I18N.getLocalizedList('OBMOBC_Date_Weekdays_Short');
  };

  OB.I18N.getWeekdayShort = function (i) { // Starts with Sun
    return OB.I18N.getLocalizedListElement('OBMOBC_Date_Weekdays_Short', i);
  };

  OB.I18N.getWeekdaysMinList = function () {
    return OB.I18N.getLocalizedList('OBMOBC_Date_Weekdays_Min');
  };

  OB.I18N.getWeekdayMin = function (i) { // Starts with Su
    return OB.I18N.getLocalizedListElement('OBMOBC_Date_Weekdays_Min', i);
  };

}());