/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm.ad_forms;

import it.openia.crm.Feedback;
import it.openia.crm.AuthUtils;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;

public class CrmCalendar extends HttpBaseServlet {

    private static final long serialVersionUID = 1L;
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {

        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }

        response.setContentType("text/html; charset=utf-8");
        VariablesSecureApp vars = new VariablesSecureApp(request);

        if (vars.commandIn("DEFAULT")) {
            printPageDataSheet(request, response, vars);
        } /*
         * else { pageError(response); }
         */
    }

    private void printPageDataSheet(HttpServletRequest request, HttpServletResponse response, VariablesSecureApp vars)
            throws IOException, ServletException {

        Feedback.sendFeedback(OBContext.getOBContext().getCurrentClient().getId(),
                OBContext.getOBContext().getCurrentClient().getName(),
                OBContext.getOBContext().getLanguage().getLanguage(),
                OBContext.getOBContext().getUser().getUsername());


        try {

            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/web/it.openia.crm/mainNew.jsp");
            dispatcher.forward(request, response);

        } catch (Exception ex) {
        }

    }

    @Override
    public String getServletInfo() {
        return "Openia Crm Calendar Servlet";

    } // end of getServletInfo() method
}
