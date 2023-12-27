/* The contents of this file are subject to the Openbravo  Public  License
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
 * All portions are Copyright (C) 2001-2009 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  Rayko Emilio Torres Cruz.
 ************************************************************************
 */
//package org.openbravo.erpCommon.ad_callouts;
package ec.com.sidesoft.holidays.ad_callouts;

import java.io.IOException;

import javax.servlet.ServletException;

//import org.openbravo.erpCommon.businessUtility.BpartnerMiscData;
//importar el xsql - en estecaso desde eldirectorio - pakete
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;

public class sshd_Concatenatedaymonth extends SimpleCallout {
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("null")
  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String srtday = info.getStringParameter("inpday", null);
    String srtmonth = info.getStringParameter("inpmonth", null);

    if (!srtday.isEmpty()) {

      String valor = "";
      try {
        if (Validar(srtday)) {
          if (srtday.length() == 1) {
            valor = "0" + srtday + "-" + srtmonth;
            info.addResult("inpvalue", valor);
          } else {
            valor = srtday + "-" + srtmonth;
            info.addResult("inpvalue", valor);
          }
        }
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

    }
  }

  private boolean Validar(String var) throws IOException {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();

    if (!var.isEmpty()) {

      try {
        int day = Integer.parseInt(var);

        if (day > 31) {
          throw new OBException(Utility.messageBD(conn, "@sshd_day@", language));
        } else {
          return true;
        }
      } catch (NumberFormatException nfe) {
        throw new OBException(Utility.messageBD(conn, "@Error, valor con letras@", language));
      }

    }
    return false;

  }
}
