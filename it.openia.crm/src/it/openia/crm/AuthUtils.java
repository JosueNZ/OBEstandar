/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package it.openia.crm;

import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openbravo.dal.core.OBContext;

/**
 *
 * @author diurno
 */
public class AuthUtils {

    public static boolean isLoggeOut(HttpServletRequest request,
            HttpServletResponse response, ServletContext sc) throws ServletException, IOException {

        if (OBContext.getOBContext() == null) {

            RequestDispatcher dispatcher = sc.getRequestDispatcher("/it.openia.crm/?requestUrl=" + request.getRequestURI().substring(request.getContextPath().length()));
            dispatcher.forward(request, response);

            return true;
        }

        return false;

    }
}
