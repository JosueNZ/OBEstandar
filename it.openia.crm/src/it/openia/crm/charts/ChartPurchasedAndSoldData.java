/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm.charts;

import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.*;
import org.openbravo.model.common.order.Order;

/**
 *
 * @author diurno
 */
public class ChartPurchasedAndSoldData {

    public static String getData(int years) {

        double[] sold = new double[years];
        double[] purchased = new double[years];

        //SOLD
        DateTime beginOfYear = new DateTime().withMonthOfYear(1).withDayOfMonth(1);
        DateTime endOfYear = new DateTime().withMonthOfYear(12).withDayOfMonth(31);

        OBCriteria<Order> salesOrders = null;

        for (int i = 0; i < years; i++) {

            if (i > 0) {
                beginOfYear = beginOfYear.minusYears(1);
                endOfYear = endOfYear.minusYears(1);
            }

            salesOrders = OBDal.getInstance().createCriteria(Order.class);
            salesOrders.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, true));
            salesOrders.add(Restrictions.or(Property.forName(Order.PROPERTY_DOCUMENTSTATUS).eq("CO"), Property.forName(Order.PROPERTY_DOCUMENTSTATUS).eq("CL")));
            salesOrders.add(Restrictions.ge(Order.PROPERTY_ORDERDATE, beginOfYear.toDate()));
            salesOrders.add(Restrictions.le(Order.PROPERTY_ORDERDATE, endOfYear.toDate()));
            salesOrders.add(Restrictions.le(Order.PROPERTY_ORDERDATE, endOfYear.toDate()));

            for (Order o : salesOrders.list()) {

                if (!"OB".equals(o.getDocumentType().getSOSubType())) {
                    sold[i] += o.getSummedLineAmount().doubleValue();
                }
            }

        }

        //PURCHASED
        beginOfYear = new DateTime().withMonthOfYear(1).withDayOfMonth(1);
        endOfYear = new DateTime().withMonthOfYear(12).withDayOfMonth(31);

        OBCriteria<Order> purchasedOrders = null;

        for (int i = 0; i < years; i++) {

            if (i > 0) {
                beginOfYear = beginOfYear.minusYears(1);
                endOfYear = endOfYear.minusYears(1);
            }

            purchasedOrders = OBDal.getInstance().createCriteria(Order.class);
            purchasedOrders.add(Restrictions.eq(Order.PROPERTY_SALESTRANSACTION, false));
            purchasedOrders.add(Restrictions.or(Property.forName(Order.PROPERTY_DOCUMENTSTATUS).eq("CO"), Property.forName(Order.PROPERTY_DOCUMENTSTATUS).eq("CL")));
            purchasedOrders.add(Restrictions.ge(Order.PROPERTY_ORDERDATE, beginOfYear.toDate()));
            purchasedOrders.add(Restrictions.le(Order.PROPERTY_ORDERDATE, endOfYear.toDate()));

            for (Order o : purchasedOrders.list()) {
                purchased[i] += o.getSummedLineAmount().doubleValue();
            }

        }

        StringBuilder sb = new StringBuilder();
        for (int i = years - 1; i >= 0; i--) {

            String pattern = "['$YEAR', $SOLD, $PURCHASED]";

            int year = (new DateTime()).getYear() - i;
            pattern = pattern.replace("$YEAR", "" + year);
            pattern = pattern.replace("$SOLD", "" + sold[i]);
            pattern = pattern.replace("$PURCHASED", "" + purchased[i]);

            sb.append(pattern);

            if (i > 0) {
                sb.append(",");
            }

        }

        return sb.toString();
    }
    
    public static String getTitle(){
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        String res ="";
        
        if(lang.matches("it_IT"))
            res = "Performance Aziendale";
        else res = "Business Performance";
                    
        return res ;
    }
    
    public static String getSalesText(){
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        String res ="";
        
        if(lang.matches("it_IT"))
            res = "Vendite";
        else res = "Sales";
                    
        return res ;
    }
    public static String getPurchaseText(){
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        String res ="";
        
        if(lang.matches("it_IT"))
            res = "Acquisti";
        else res = "Purchases";
                    
        return res ;
        
    }
    public static String getYearText(){
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        String res ="";
        
        if(lang.matches("it_IT"))
            res = "Anno";
        else res = "Year";
                    
        return res ;
    }
    
    public static String GetField(String key){
        
        OBCriteria<Message> msgList;
        OBCriteria<Language> langList;
        String lang = OBContext.getOBContext().getLanguage().getLanguage();
        String res="";
        
        langList = OBDal.getInstance().createCriteria(Language.class);
        langList.add(Restrictions.eq(Language.PROPERTY_LANGUAGE, lang));
        
        OBContext.setAdminMode(true);
        
        msgList = OBDal.getInstance().createCriteria(Message.class);
        msgList.add(Restrictions.eq(Message.PROPERTY_SEARCHKEY, key));
        
        if (!lang.matches("en_US")) {
            OBCriteria<MessageTrl> trlList;
            trlList = OBDal.getInstance().createCriteria(MessageTrl.class);
            trlList.add(Restrictions.eq(MessageTrl.PROPERTY_MESSAGE, msgList.list().get(0)));
            trlList.add(Restrictions.eq(MessageTrl.PROPERTY_LANGUAGE, langList.list().get(0)));
            
            if (!trlList.list().isEmpty())
                res = trlList.list().get(0).getMessageText();
            else
                res = msgList.list().get(0).getMessageText();
        }
        else 
            res = msgList.list().get(0).getMessageText();
        
        OBContext.restorePreviousMode();
        
        
        return res;
    }
    
}
