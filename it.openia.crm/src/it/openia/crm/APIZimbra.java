/*
 ************************************************************************************
 * Copyright (C) 2008-2013 Openia S.r.l.
 * Licensed under the Mozilla Public License version 2.0
 * You may obtain a copy of the License at http://www.mozilla.org/MPL/2.0/
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */

package it.openia.crm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author diurno
 */
public class APIZimbra {

    public static JSONObject getJson(String host, long port, String user, String password, String param, boolean ssl) throws MalformedURLException, IOException, JSONException, KeyManagementException, NoSuchAlgorithmException {

        //TRUST ALL CERTIFICATES
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(
                        java.security.cert.X509Certificate[] certs, String authType) {
                }
            }
        };
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());


        JSONObject result = null;

        String userPassword = user + ":" + password;
        String encoding = Base64.encodeBase64String(userPassword.getBytes());

        host = host.replace("http://", "");
        host = host.replace("https://", "");
        
        String httpType = "http";
        if (ssl) {
            httpType = "https";
        }

        URL url = new URL(httpType + "://" + host + ":" + port + "/home/" + user + "/" + param + "?fmt=json");

        URLConnection uc = null;

        if (ssl) {
            uc = (HttpsURLConnection) url.openConnection();
        } else {
            uc = (HttpURLConnection) url.openConnection();
        }

        uc.setDoInput(true);
        uc.setRequestProperty("Authorization", "Basic " + encoding);
        uc.addRequestProperty("User-Agent", "Mozilla/4.76");

        InputStream in = uc.getInputStream();
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(in));

        StringBuilder str1 = new StringBuilder();
        String line = null;

        while ((line = reader.readLine()) != null) {
            str1.append(line);
        }
        in.close();

        result = new JSONObject(str1.toString());

        return result;
    }
}