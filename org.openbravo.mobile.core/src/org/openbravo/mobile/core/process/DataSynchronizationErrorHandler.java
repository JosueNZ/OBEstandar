/*
 ************************************************************************************
 * Copyright (C) 2012 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */
package org.openbravo.mobile.core.process;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.Entity;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.importprocess.ImportEntry;

public abstract class DataSynchronizationErrorHandler {

  public static String getErrorMessage(Throwable e) {
    StringWriter sb = new StringWriter();

    PrintWriter pw = new PrintWriter(sb);
    final Throwable cause = DbUtility.getUnderlyingSQLException(e);
    cause.printStackTrace(pw);
    return sb.toString();
  }

  /**
   * This method should return true if the imported data entry should be kept in status error in the
   * {@link ImportEntry} table. If false is returned then the {@link ImportEntry} is set as
   * Processed in the {@link ImportEntry} table.
   */
  public boolean setImportEntryStatusToError() {
    return true;
  }

  public abstract void handleError(Throwable e, Entity entity, JSONObject result,
      JSONObject jsonRecord);
}
