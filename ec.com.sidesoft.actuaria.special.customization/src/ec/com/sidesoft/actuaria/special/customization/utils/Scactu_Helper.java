package ec.com.sidesoft.actuaria.special.customization.utils;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.soap.SOAPMessage;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.json.XML;
import org.openbravo.base.exception.OBException;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;

public abstract class Scactu_Helper {

	static public JSONObject getResponse(JSONObject data) throws Exception {
		JSONObject response = new JSONObject();
		response.put("status", 0);
		response.put("data", data);
		response.put("message", "");
		return response;
	}

	static public JSONObject getErrorResponse(String message) throws Exception {
		JSONObject response = new JSONObject();
		response.put("status", -1);
		response.put("data", new JSONObject());
		response.put("message", message);
		return response;
	}

	static public String post(String endpoint, JSONObject body, String token) throws Exception {
		URL url = new URL(endpoint);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setRequestProperty("Authorization", token);

		String jsonInputString = body.toString();
		OutputStream os = conn.getOutputStream();
		byte[] input = jsonInputString.getBytes("utf-8");
		os.write(input, 0, input.length);

		int status = conn.getResponseCode();
		if (status != 200 && status != 204) {
			throw new OBException(conn.getResponseMessage());
		}

		InputStream responseStream = conn.getInputStream();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream))) {
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line.trim());
			}
			return response.toString();
		}
	}

	static public String postActuaparToken(String endpoint, String username, String password)
			throws Exception {
		JSONObject body = new JSONObject();
		body.put("usuario", username);
		body.put("password", password);

		URL url = new URL(endpoint + "login");
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");

		String jsonInputString = body.toString();
		OutputStream os = conn.getOutputStream();
		byte[] input = jsonInputString.getBytes("utf-8");
		os.write(input, 0, input.length);

		int status = conn.getResponseCode();
		if (status != 200 && status != 204) {
			throw new OBException(conn.getResponseMessage());
		}

		InputStream responseStream = conn.getInputStream();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream))) {
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				response.append(line.trim());
			}
			return response.toString();
		}
	}

	static public String getErrorMessage(Logger logger, Exception e) {
		Throwable throwable = DbUtility.getUnderlyingSQLException(e);
		String message = OBMessageUtils.translateError(throwable.getMessage()).getMessage();
		logger.error(message);
		return message;
	}

	static public JSONObject readAllIntoJSONObject(InputStream content) throws Exception {
		JSONObject json = null;
		try {
			BufferedReader rd = new BufferedReader(
					new InputStreamReader(content, StandardCharsets.UTF_8));
			String jsonText = IOUtils.toString(rd);
			json = new JSONObject(jsonText);
		} catch (Exception e) {
			throw new OBException("Invalid JSON format");
		}
		return json;
	}

	static public String getString(JSONObject json, String key, boolean required) throws Exception {
		String value = json.has(key) ? json.getString(key) : null;
		if (value == null || value.trim().isEmpty()) {
			if (required) {
				throw new OBException(key + " is required");
			}
			return null;
		}
		return value.trim();
	}

	static public Long getLong(JSONObject json, String key, boolean required) throws Exception {
		String value = json.has(key) ? json.getString(key) : null;
		if (value == null || value.trim().isEmpty()) {
			if (required) {
				throw new OBException(key + " is required");
			}
			return null;
		}
		value = value.trim();
		try {
			return new Long(value);
		} catch (Exception e) {
			throw new OBException("Wrong integer format for" + key);
		}
	}

	static public BigDecimal getBigDecimal(JSONObject json, String key, boolean required)
			throws Exception {
		String value = json.has(key) ? json.getString(key) : null;
		if (value == null || value.trim().isEmpty()) {
			if (required) {
				throw new OBException(key + " is required");
			}
			return null;
		}
		value = value.trim();
		try {
			return new BigDecimal(value);
		} catch (Exception e) {
			throw new OBException("Wrong double format for" + key);
		}
	}

	static public Boolean getBoolean(JSONObject json, String key, boolean required)
			throws Exception {
		String value = json.has(key) ? json.getString(key) : null;
		if (value == null || value.trim().isEmpty()) {
			if (required) {
				throw new OBException(key + " is required");
			}
			return null;
		}
		value = value.trim();
		if (!(value.equals("true") || value.equals("false"))) {
			throw new OBException("Wrong data type for" + key);
		}
		return value.equals("true");
	}

	static public Date getDate(JSONObject json, String key, boolean required) throws Exception {
		String value = json.has(key) ? json.getString(key) : null;
		if (value == null || value.trim().isEmpty()) {
			if (required) {
				throw new OBException(key + " is required");
			}
			return null;
		}
		try {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			return df.parse(value.trim());
		} catch (Exception e) {
			throw new OBException("Wrong date format for" + key);
		}
	}

	static public String getDateString(Date date) throws IOException {
		if (date == null) {
			return null;
		} else {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
			return df.format(date);
		}
	}

	static public String getTimestampString(Date date) throws IOException {
		if (date == null) {
			return null;
		} else {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			return df.format(date);
		}

	}

	static public String xmlToString(SOAPMessage soapMessage) {
		String xml = null;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			soapMessage.writeTo(out);
			xml = new String(out.toByteArray());
		} catch (Exception e) {
			throw new OBException(e.getMessage());
		}
		return xml;
	}

	static public JSONObject xmlToJSON(SOAPMessage soapMessage) {
		JSONObject json = new JSONObject();
		try {
			String xml = xmlToString(soapMessage);
			json = new JSONObject(XML.toJSONObject(xml).toString());
			json = json.getJSONObject("soap:Envelope").getJSONObject("soap:Body");
		} catch (Exception e) {
			throw new OBException(e.getMessage());
		}
		return json;
	}

}
