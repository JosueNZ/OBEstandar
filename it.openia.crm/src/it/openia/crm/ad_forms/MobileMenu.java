/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm.ad_forms;

import it.openia.crm.AuthUtils;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.openbravo.base.HttpBaseServlet;

/**
 *
 * @author diurno
 */
public class MobileMenu extends HttpBaseServlet {

    private static final Logger log = Logger.getLogger(MobileMenu.class);

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        process(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    protected void process(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        if (AuthUtils.isLoggeOut(request, response, getServletContext())){
            return;
        }

        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/web/it.openia.crm/mobilemenu.jsp");
        dispatcher.forward(request, response);
    }
}
