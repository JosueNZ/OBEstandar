/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm.charts;

import it.openia.crm.AuthUtils;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;

/**
 *
 * @author diurno
 */
public class ChartPurchasedAndSold extends HttpSecureAppServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (AuthUtils.isLoggeOut(request, response, getServletContext())) {
            return;
        }

        response.setContentType("text/html; charset=utf-8");
        VariablesSecureApp vars = new VariablesSecureApp(request);

        if (vars.commandIn("DEFAULT")) {
            try {               
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/web/it.openia.crm/charts/purchase_and_sold.jsp");
                dispatcher.forward(request, response);
            } catch (Exception ex) {
                System.out.println(ex.toString());
            }
        }
    }
}