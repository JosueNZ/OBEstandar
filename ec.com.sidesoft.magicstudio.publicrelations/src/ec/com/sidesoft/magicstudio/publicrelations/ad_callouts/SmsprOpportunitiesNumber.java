package ec.com.sidesoft.magicstudio.publicrelations.ad_callouts;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.ad.utility.Sequence;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;

public class SmsprOpportunitiesNumber extends SimpleCallout {

  private static final long serialVersionUID = 1L;
  
  OBCriteria<Sequence> sequ = OBDal.getInstance().createCriteria(Sequence.class);
  
  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    // TODO Auto-generated method stub
   
    String doctype_id = info.getStringParameter("inpemSmsprCDoctypeId", null);
    
    DocumentType doctype = OBDal.getInstance().get(DocumentType.class,doctype_id);
    
    String nameseq = doctype.getDocumentSequence().getName();
    String sequence = doctype.getDocumentSequence().getPrefix();
    sequence = sequence + " - "+doctype.getDocumentSequence().getNextAssignedNumber();
    
    sequence = getDocumentNo(doctype, "opcrm_opportunities");
    
    
    info.addResult("inpemSmsprDocumentno", sequence);
  }

  ///
  
  public static String getDocumentNo(DocumentType docType, String tableName) {
	    String nextDocNumber = "";
	    if (docType != null) {
	      Sequence seq = docType.getDocumentSequence();
	      if (seq == null && tableName != null) {
	        OBCriteria<Sequence> obcSeq = OBDal.getInstance().createCriteria(Sequence.class);
	        obcSeq.add(Restrictions.eq(Sequence.PROPERTY_NAME, tableName));
	        if (obcSeq != null && obcSeq.list().size() > 0) {
	          seq = obcSeq.list().get(0);
	        }
	      }
	      if (seq != null) {
	        if (seq.getPrefix() != null)
	          nextDocNumber = seq.getPrefix();
	        nextDocNumber += seq.getNextAssignedNumber().toString();
	        if (seq.getSuffix() != null)
	          nextDocNumber += seq.getSuffix();
	        seq.setNextAssignedNumber(seq.getNextAssignedNumber() + seq.getIncrementBy());
	        OBDal.getInstance().save(seq);
	         OBDal.getInstance().flush();
	      }
	    }

	    return nextDocNumber;
	  }

  
  ///
}
//


//