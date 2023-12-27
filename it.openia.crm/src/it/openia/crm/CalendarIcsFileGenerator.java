/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;


/**
 *
 * @author nicholas
 */
public class CalendarIcsFileGenerator extends HttpBaseServlet{
    
    static final long serialVersionUID = 1L;
    private static final int BUFSIZE = 4096;
    private String filePath;
    
    public void init() {
        filePath = getServletContext().getRealPath("") + File.separator;
    }
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException,
            ServletException {
                
        OBError errobj = new OBError();
        VariablesSecureApp vars = new VariablesSecureApp(request);
        
        String actId = "";
        if(request.getParameter("activityId")!=null)
            actId = request.getParameter("activityId");
        else
            actId = vars.getGlobalVariable("Opcrm_Activity_ID", "", "");
            
        
        final OBCriteria<Opcrmactivity> activityRecord = OBDal.getInstance().createCriteria(Opcrmactivity.class);
        activityRecord.add(Restrictions.eq(Opcrmactivity.PROPERTY_ID, actId));
        
        Opcrmactivity act = activityRecord.list().get(0);
        
        ICalMaker calendarIcsFileMaker = new ICalMaker();
        calendarIcsFileMaker.setLang(vars.getLanguage());
        calendarIcsFileMaker.setSubject(act.getActivitySubject());
        calendarIcsFileMaker.setStart(act.getActivityStartdate());
        File calendarActivityFile;
        
        try{
            calendarActivityFile = calendarIcsFileMaker.Create(filePath+act.getId()+"activity.ics");
        }catch(IOException e){
            Logger.getLogger(CalendarIcsFileGenerator.class.getName()).log(Level.SEVERE, null, e);
            return;
        }
        
        int length = 0;
        ServletOutputStream outStream = response.getOutputStream();
        ServletContext context  = getServletConfig().getServletContext();
        String mimetype = context.getMimeType(filePath+act.getId()+"activity.ics");
        
        // sets response content type
        if (mimetype == null) {
            mimetype = "text/calendar";
        }
        response.setContentType(mimetype);
        response.setContentLength((int)calendarActivityFile.length());
        
        // sets HTTP header
        response.setHeader("Content-Disposition", "attachment; filename=\"activity.ics\"");
        
        byte[] byteBuffer = new byte[BUFSIZE];
        DataInputStream in = new DataInputStream(new FileInputStream(calendarActivityFile));
        
        // reads the file's bytes and writes them to the response stream
        while ((in != null) && ((length = in.read(byteBuffer)) != -1))
        {
            outStream.write(byteBuffer,0,length);
        }
        
        in.close();
        outStream.close();
        
        boolean success = calendarActivityFile.delete();
        if(!success)
            Logger.getLogger(CalendarIcsFileGenerator.class.getName()).log(Level.SEVERE, null, "Delete: File "+calendarActivityFile.getName()+" deletion failed");
    }
    
    
}
