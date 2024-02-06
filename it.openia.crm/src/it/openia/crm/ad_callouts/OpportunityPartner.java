package it.openia.crm.ad_callouts;

import javax.servlet.ServletException;

import org.openbravo.dal.service.OBDal;
import org.hibernate.criterion.Restrictions;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;

public class OpportunityPartner extends SimpleCallout {

  private static final long serialVersionUID = 1L;

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String partnerId = info.getStringParameter("inpcBpartnerId", null);

    if(partnerId!=null && !partnerId.equals("")){
      BusinessPartner partner = OBDal.getInstance().get(BusinessPartner.class, partnerId);
      if(partner!=null){
        OBCriteria<Location> locationlist = OBDal.getInstance().createCriteria(Location.class);
          locationlist.add(Restrictions.eq(Location.PROPERTY_BUSINESSPARTNER, partner));
          locationlist.setMaxResults(1);
          info.addResult("inptaxBpartner",partner.getTaxID());
          info.addResult("inpcellphoneBpartner",locationlist.list().get(0).getPhone());
      }else{
        info.addResult("inptaxBpartner",null);
        info.addResult("inpcellphoneBpartner",null);
      }

    }else{
        info.addResult("inptaxBpartner",null);
        info.addResult("inpcellphoneBpartner",null);
    }
  }
}
