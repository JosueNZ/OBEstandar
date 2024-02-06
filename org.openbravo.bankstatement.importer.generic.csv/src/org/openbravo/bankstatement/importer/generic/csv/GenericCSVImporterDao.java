/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 ************************************************************************************
 */

package org.openbravo.bankstatement.importer.generic.csv;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.bankstatement.importer.generic.csv.model.CsvConfiguration;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

public class GenericCSVImporterDao {

  /**
   * gets the configuration parameter (dateFormat and decimalSeparator) for the given className and
   * organization
   * 
   * @param className
   *          Java class name in charge of the import process
   * @param org
   *          organization
   * @return a Map<String, String> with the following keys:
   *         <ul>
   *         <li>dateFormat: contains the date format</li>
   *         <li>decimalSeparator: contains the decimal format</li>
   *         </ul>
   */
  public Map<String, String> getConfigParameters(final String className, final Organization org) {
    Map<String, String> configParams = new HashMap<String, String>(2);
    configParams.put("dateFormat", GenericCSVImporter.DEFAULT_DATEFORMAT);
    configParams.put("decimalSeparator", GenericCSVImporter.DEFAULT_DECIMALSEPARATOR);
    configParams.put("fieldDelimiter", GenericCSVImporter.DEFAULT_FIELDDELIMITER);

    try {
      OBContext.setAdminMode(true);

      final OBCriteria<CsvConfiguration> obc = OBDal.getInstance().createCriteria(
          CsvConfiguration.class);
      obc.add(Restrictions.eq(CsvConfiguration.PROPERTY_JAVACLASSNAME, className));

      for (final CsvConfiguration bfl : obc.list()) {
        final String dateFormat = StringUtils.isEmpty(bfl.getDateFormat()) ? GenericCSVImporter.DEFAULT_DATEFORMAT
            : StringUtils.trim(bfl.getDateFormat());
        final String decimalSeparator = StringUtils.isEmpty(bfl.getDecimalSeparator()) ? GenericCSVImporter.DEFAULT_DECIMALSEPARATOR
            : bfl.getDecimalSeparator();
        final String fieldDelimiter = StringUtils.isEmpty(bfl.getFieldDelimiter()) ? GenericCSVImporter.DEFAULT_FIELDDELIMITER
            : bfl.getFieldDelimiter();
        configParams.put("dateFormat", dateFormat);
        configParams.put("decimalSeparator", decimalSeparator);
        configParams.put("fieldDelimiter", fieldDelimiter);
        break;
      }
    } finally {
      OBContext.restorePreviousMode();
    }

    return configParams;
  }

}
