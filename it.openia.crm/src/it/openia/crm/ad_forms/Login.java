/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package it.openia.crm.ad_forms;

import it.openia.crm.Feedback;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.dal.core.OBContext;
import org.openbravo.service.web.UserContextCache;

/**
 *
 * @author diurno
 */
public class Login extends HttpBaseServlet {

    private static final Logger log = Logger.getLogger(Login.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        login(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        login(request, response);
    }

    protected void login(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if (request.getRequestURI().contains(".crm")
                && !request.getRequestURI().contains(".crm/")) {

            String qs = request.getQueryString();
            if (qs == null) {
                qs = "";
            } else {
                qs = "?" + qs;
            }

            response.sendRedirect(response.encodeRedirectURL(request.getRequestURI() + "/" + qs));
            return;
        }

        if (request.getParameter("l") != null
                && request.getParameter("p") != null) {

            if (OBContext.getOBContext() != null) {
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/it.openia.crm.ad_forms/MobileMenu.html");
                dispatcher.forward(request, response);
                return;
            }

            AuthenticationManager authManager = AuthenticationManager.getAuthenticationManager(this);
            String userId = authManager.webServiceAuthenticate(request);

            if (userId == null) {

                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/web/it.openia.crm/login.jsp");
                dispatcher.forward(request, response);
                return;
            }

            OBContext.setOBContext(UserContextCache.getInstance().getCreateOBContext(userId));
            OBContext.setOBContextInSession(request, OBContext.getOBContext());
            OBContext.setOBContext(userId);

            Feedback.sendFeedback(OBContext.getOBContext().getCurrentClient().getId(),
                    OBContext.getOBContext().getCurrentClient().getName(),
                    OBContext.getOBContext().getLanguage().getLanguage(),
                    OBContext.getOBContext().getUser().getUsername());

            if (request.getParameter("requestUrl") != null) {
                    RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(request.getParameter("requestUrl"));
                dispatcher.forward(request, response);
                return;
            } else {
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/it.openia.crm.ad_forms/MobileMenu.html");
                dispatcher.forward(request, response);
                return;
            }
            
        } else if (request.getParameter("logout") != null) {

            request.getSession(true).invalidate();
            OBContext.setOBContext((OBContext) null);

            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/web/it.openia.crm/login.jsp");
            dispatcher.forward(request, response);

        } else {

            if (OBContext.getOBContext() != null) {
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/it.openia.crm.ad_forms/MobileMenu.html");
                dispatcher.forward(request, response);
                return;
            }

            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/web/it.openia.crm/login.jsp");
            dispatcher.forward(request, response);
        }
    }
}
