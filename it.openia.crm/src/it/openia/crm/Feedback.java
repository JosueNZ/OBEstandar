/*
 * Copyright (C) 2008-2013 Openia S.r.l.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. 
 */

package it.openia.crm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.module.Module;

/**
 *
 * @author diurno
 */
public class Feedback {

    private static HashMap<String, Boolean> feedbacks;

    static {
        feedbacks = new HashMap<String, Boolean>();
    }

    private static String getHTML(String urlToRead) throws MalformedURLException, IOException {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        String line;
        String result = "";

        url = new URL(urlToRead);
        conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5000);
        conn.setRequestMethod("GET");
        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        while ((line = rd.readLine()) != null) {
            result += line;
        }
        rd.close();

        return result;
    }

    public static void sendFeedback(String clientId, String clientName, String lang, String username) throws MalformedURLException, IOException {
        sendFeedback(clientId, clientName, lang, username, "no-version"); 
    }
    
    public static void sendFeedback(String clientId, String clientName, String lang, String username, String version) throws MalformedURLException, IOException {
        
        DateTime today = new DateTime();
        
        OBContext.setAdminMode(true);
        OBCriteria<Module> modList = OBDal.getInstance().createCriteria(Module.class);
        modList.add(Restrictions.eq(Module.PROPERTY_JAVAPACKAGE,"it.openia.crm"));
        
        
        String hashKey = "" + today.getYear() + today.getDayOfYear() + username + lang + clientId;

        // if (feedbacks.get((String) hashKey) == null) {

        //     String url = "http://www.openia.it/feedback_crm.php"
        //             + "?insert=1"
        //             + "&client_name=" + URLEncoder.encode(clientName, "UTF-8")
        //             + "&lang=" + URLEncoder.encode(lang, "UTF-8")
        //             + "&username=" + URLEncoder.encode(username, "UTF-8")
		//     + "&version=" + URLEncoder.encode(modList.list().get(0).getVersion(), "UTF-8");

        //     getHTML(url);
            
        //     feedbacks.put(hashKey, Boolean.TRUE);

        // }
        OBContext.restorePreviousMode();
    }
    
}
