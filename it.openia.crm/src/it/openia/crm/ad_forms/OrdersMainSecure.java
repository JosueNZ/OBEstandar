/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */
package it.openia.crm.ad_forms;

import it.openia.crm.AuthUtils;
import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.model.common.businesspartner.BusinessPartner;

/**
 *
 * @author nicholas
 */
public class OrdersMainSecure extends HttpSecureAppServlet{
    
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
        }
    }
    
    private void printPageDataSheet(HttpServletRequest request, HttpServletResponse response, VariablesSecureApp vars)
            throws IOException, ServletException {
        try {
            
            RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/web/it.openia.crm/ordersmain.jsp");
            dispatcher.forward(request, response);

        } catch (Exception ex) {
            System.out.println(ex.toString());
        }
    }
    
    private void updateManifest(){
        try {
            String manifestFileUrl = getServletContext().getResource("/web/it.openia.crm/test.appcache").getPath();
        } catch (MalformedURLException ex) {
            Logger.getLogger(OrdersMainSecure.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        String manifestFileName = "/web/it.openia.crm/test.appcache";
        
        try{
            BufferedReader bf = new BufferedReader(new FileReader(manifestFileName));

            BufferedWriter out  = new BufferedWriter(new FileWriter(manifestFileName));
            
            String line1=null;
            List<String> lines = new ArrayList<String>();
                    
            while (( line1 = bf.readLine()) != null)
            {
                if(line1.contains("#DATELINE"))
                    line1.replace(line1, "#DATELINE"+new Date().toString()+"\n");;
            
                lines.add(line1);    
            }
            
            for (String l : lines)
                out.write(l);
    
            out.close();
            
            
        }catch (IOException e){
            System.out.println(e.toString());
        }
        
    }
    
    @Override
    public String getServletInfo() {
        return "Openia Crm Orders Insert Table Servlet. Still under development";

    }
    
       
}
