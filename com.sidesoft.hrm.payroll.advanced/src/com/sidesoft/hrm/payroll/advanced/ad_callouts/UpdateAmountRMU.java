package com.sidesoft.hrm.payroll.advanced.ad_callouts;

import java.math.BigDecimal;

import javassist.expr.Cast;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import com.sidesoft.hrm.payroll.advanced.SfprGrade;

public class UpdateAmountRMU extends SimpleCallout {

  /**
   * 3653617759010780960L
   */
  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
    // inpcDoctypeId
    // inpnoCertificate

   /* String documentTypeId = info.getStringParameter("", null);
    DocumentType docObj = OBDal.getInstance().get(DocumentType.class, documentTypeId);*/

	  String strGrade = info.getStringParameter("inpsfprGradeId", null);
	  SfprGrade rveObj = OBDal.getInstance().get(SfprGrade.class, strGrade);

	  if (rveObj.getRmu() != null) {

 
      //Sequence seq = docObj.getDocumentSequence();
      //info.addResult("inpdocumentno",   seq.getNextAssignedNumber().toString() );
      
	      BigDecimal rmu = rveObj.getRmu();
	      info.addResult("inprmu",   rmu.toString() );
	      
    }
  }

}
