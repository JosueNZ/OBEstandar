package com.sidesoft.hrm.payroll.ad_callouts;

import java.math.BigDecimal;

import javax.servlet.ServletException;

import org.openbravo.erpCommon.ad_callouts.SimpleCallout;

public class SumTotal extends SimpleCallout {

    @Override
    protected void execute(CalloutInfo info) throws ServletException {

        String inpobligation = info.getStringParameter("inpobligation", null);
        String inpcost = info.getStringParameter("inpcost", null);
        String inpinterest = info.getStringParameter("inpinterest", null);

        BigDecimal total = new BigDecimal(0);
        try {
            total = total.add(getBigDecimal(inpobligation));
            total = total.add(getBigDecimal(inpcost));
            total = total.add(getBigDecimal(inpinterest));
        } catch (Exception e) {
            e.printStackTrace();
        }

        info.addResult("inptotal", total);
    }

    private BigDecimal getBigDecimal(String value)
            throws Exception {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            return new BigDecimal(0);
        }
    }
}
