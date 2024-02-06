package com.sidesoft.hrm.payroll.modulescript;

import java.sql.PreparedStatement;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.ModuleScript;

public class UpdateSsprContract extends ModuleScript {

  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      PreparedStatement ps = cp
          .getPreparedStatement("UPDATE sspr_contract SET isparttime='N' WHERE isparttime IS NULL");

      ps.executeUpdate();

    } catch (Exception e) {
      handleError(e);
    }
  }
}
