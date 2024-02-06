/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm.ad_forms;

import it.openia.crm.Feedback;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;

/**
 *
 * @author nicholas
 */
public class ActivityFromLead extends HttpBaseServlet {

    //This Class is Frozen and under development (along with CreateActivityGetData.java and ActFromLead.jsp)
    
    private static final long serialVersionUID = 1L;
    public static String leadId="";
    public static String name="";
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
        
        response.setContentType("text/html; charset=utf-8");
        VariablesSecureApp vars = new VariablesSecureApp(request);
        
        
        leadId = vars.getGlobalVariable("AD_User_ID", "", "");
        
        
        if (vars.commandIn("DEFAULT")) {
            printPageDataSheet(request, response, vars);
        }
        
    }
    
    private void printPageDataSheet(HttpServletRequest request, HttpServletResponse response, VariablesSecureApp vars)
            throws IOException, ServletException {


        try {

                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/web/it.openia.crm/ActFromLead.jsp");
                dispatcher.forward(request, response);

            } catch (Exception ex) {
        }

    }
    
    public static String GetLeadId(){
        return leadId;
    }
    
    public static String GetName(){
        final OBCriteria<User> userList = OBDal.getInstance().createCriteria(User.class);
        userList.add(Restrictions.eq(User.PROPERTY_ID, leadId));
        User usr = null;
        String res="";
        
        if(!userList.list().isEmpty())
            usr=userList.list().get(0);
        
        if(usr.getOpcrmCommercialname()!=null)
            res=usr.getOpcrmCommercialname();
        else
        { 
            if(usr.getFirstName()!=null)
                res += usr.getFirstName() +" "; 
            if(usr.getLastName()!=null)
                res += usr.getLastName();
        }
        
        
        return res;
    }
    
    @Override
    public String getServletInfo() {
        return "Openia Crm Create Activity From Lead Servlet";
    }
    
}
