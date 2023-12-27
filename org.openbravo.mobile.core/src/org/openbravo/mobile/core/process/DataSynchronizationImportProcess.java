/*
 ************************************************************************************
 * Copyright (C) 2015 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.mobile.core.process;

import java.io.Writer;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.service.importprocess.ImportEntry;

/**
 * Interface which flags an {@link DataSynchronizationProcess} to be a process for which the json
 * data is first stored in the {@link ImportEntry} table.
 * 
 * @author mtaal
 */
public interface DataSynchronizationImportProcess {

  public void executeCreateImportEntry(Writer w, JSONObject jsonObject);

  public void setImportEntryId(String importEntryId);
}
