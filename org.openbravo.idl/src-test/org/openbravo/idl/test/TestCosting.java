/*
 ************************************************************************************
 * Copyright (C) 2009-2017 Openbravo S.L.U.
 * Licensed under the Openbravo Commercial License version 1.0
 * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
 * or in the legal folder of this module distribution.
 ************************************************************************************
 */

package org.openbravo.idl.test;

import static org.junit.Assert.fail;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.idl.proc.CostingsProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.OBBaseTest;

/**
 * 
 * @author adrian
 */
public class TestCosting extends OBBaseTest {
  @Test
  public void testCostingImports() {

    // setContext("IDLAdmin", "IDL Admin", "IDL", "*");
    OBContext.setOBContext("BC64492184D54BC0AA716A1545DE6BA0", "26BB96AF82E44372B2C082432242B461",
        "BAA57A5061444E5CB69A831D98DF9D6B", "EF6321E567F449A497338032F8FD2D08");

    CostingsProcess cproc = new CostingsProcess();
    cproc.init(new DalConnectionProvider(), new VariablesSecureApp("Openbravo", "IDL", "*"));

    try {

      cproc.checkOrganization("" + "Costing", "*");
      // cproc.checkTransactionalOrganization("Stock", "Pamplona");

      BaseOBObject costing = cproc.createCosting("*", "rma", "Standard", "5.50", "2009-01-01",
          "2009-12-31", "100.00", "120.00", "3.25", "", "");
      assert (costing != null);
      System.out.println(costing);
    } catch (Exception e) {
      fail(e.toString());
    }

    try {
      BaseOBObject costing = cproc.createCosting("*", "rmb", "Average", "5.50", "2009-01-01",
          "9999-12-31", "", "", "", "", "");
      assert (costing != null);
      System.out.println(costing);
    } catch (Exception e) {
      fail(e.toString());
    }

    try {
      cproc.createCosting("*", "rmb", "-------------------", "5.50", "2009-01-01", "9999-12-31",
          "", "", "", "", "");
      fail("must fail");
    } catch (Exception e) {
      System.out.println(e.getMessage());
      ;
    }

  }
}
