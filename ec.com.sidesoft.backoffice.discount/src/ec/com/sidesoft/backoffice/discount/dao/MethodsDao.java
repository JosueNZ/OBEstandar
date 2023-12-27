package ec.com.sidesoft.backoffice.discount.dao;

import java.util.List;
import java.util.ArrayList;

import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.FieldProviderFactory;

import ec.com.sidesoft.backoffice.discount.SsbodGiftTemp;

public class MethodsDao {

    public static FieldProvider[] getTransactionsFiltered(List<SsbodGiftTemp> regalo) {

    	OBContext.setAdminMode();
        try {
            if(regalo.size()<=0){
            FieldProvider[] dataT = FieldProviderFactory.getFieldProviderArray(getList());
                FieldProviderFactory.setField(dataT[0], "idproduct", " ");
                FieldProviderFactory.setField(dataT[0], "nameproduct", "");
                FieldProviderFactory.setField(dataT[0], "rownum", "");
                return dataT;
            }

            FieldProvider[] data = FieldProviderFactory.getFieldProviderArray(regalo);

            for (int i = 0; i < data.length; i++) {
                FieldProviderFactory.setField(data[i], "idproduct", String.valueOf(i));
                FieldProviderFactory.setField(data[i], "nameproduct", regalo.get(i).getProduct().getName());
              }
            return data;
            
        } catch (Exception e) {
            throw new OBException(e);
        } finally {
            OBContext.restorePreviousMode();
        }
    }

    public static List getList() {
        List<String> firstOBList = new ArrayList<String>();
        firstOBList.add("");
        return firstOBList;        
    }
}