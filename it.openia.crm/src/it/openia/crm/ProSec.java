package it.openia.crm;

import org.joda.time.DateTime;

/**
 *
 * @author Andrea Checchi <a.checchi@openia.it>
 */
public class ProSec {

    private String nRot(int n, String s) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 'a' && c <= 'm') {
                c += n;
            } else if (c >= 'A' && c <= 'M') {
                c += n;
            } else if (c >= 'n' && c <= 'z') {
                c -= n;
            } else if (c >= 'N' && c <= 'Z') {
                c -= n;
            }
            sb.append(c);
        }

        return sb.toString();
    }
   
    
    public boolean check(){
    
        String sn = null; //TODO
        
        //trasformazioni
        String client = null;
        int nUsers = 0;
        DateTime expirationDate = null;
        
        
        //check date
        //check users
        
        return false;
        
    }
    
}
