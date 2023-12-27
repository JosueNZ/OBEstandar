/*
 * ************************************************************************ The
 * contents of this file are subject to the Openbravo Public License Version 1.1
 * (the "License"), being the Mozilla Public License Version 1.1 with a
 * permitted attribution clause; you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.openbravo.com/legal/license.html Software distributed under the
 * License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing rights and limitations under the License. The Original Code is
 * Openbravo ERP. The Initial Developer of the Original Code is Openbravo SLU All
 * portions are Copyright (C) 2001-2018 Openbravo SLU All Rights Reserved.
 * Contributor(s): ______________________________________.
 * ***********************************************************************
 */
package ec.com.sidesoft.hrm.payroll.payment.rol.utility;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;

import org.apache.log4j.Logger;

public class EmailManager {
  private static Logger log4j = Logger.getLogger(EmailManager.class);
  private static final long SMTP_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

  public static void sendEmail(String senderAddress, String recipientTO, String recipientCC,
      String recipientBCC, String replyTo, String subject, String content, String contentType,
      List<File> attachments, Date sentDate, List<String> headerExtras, Session mailSession)
      throws Exception {
    String localReplyTo = replyTo;
    String localRecipientTO = recipientTO;
    String localRecipientCC = recipientCC;
    String localRecipientBCC = recipientBCC;
    String localContentType = contentType;
    try {

      Transport transport = mailSession.getTransport();
      MimeMessage message = new MimeMessage(mailSession);

      message.setFrom(new InternetAddress(senderAddress));

      if (localRecipientTO != null) {
        localRecipientTO = localRecipientTO.replaceAll(";", ",");
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(localRecipientTO));
      }
      if (localRecipientCC != null) {
        localRecipientCC = localRecipientCC.replaceAll(";", ",");
        message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(localRecipientCC));
      }
      if (localRecipientBCC != null) {
        localRecipientBCC = localRecipientBCC.replaceAll(";", ",");
        message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(localRecipientBCC));
      }

      if (localReplyTo != null) {
        localReplyTo = localReplyTo.replaceAll(";", ",");
        localReplyTo = localReplyTo.replaceAll(", *", ",");
        String[] replyToArray = localReplyTo.split(",");

        Address[] replyToAddresses = new InternetAddress[replyToArray.length];
        for (int i = 0; i < replyToArray.length; i++) {
          replyToAddresses[i] = new InternetAddress(replyToArray[i]);
        }

        message.setReplyTo(replyToAddresses);
      }

      if (subject != null) {
        message.setSubject(subject);
      }
      if (sentDate != null) {
        message.setSentDate(sentDate);
      }

      if (headerExtras != null && headerExtras.size() > 0) {
        String[] headerExtrasArray = headerExtras.toArray(new String[headerExtras.size()]);
        for (int i = 0; i < headerExtrasArray.length - 1; i++) {
          message.addHeader(headerExtrasArray[i], headerExtrasArray[i + 1]);
          i++;
        }
      }

      if (attachments != null && attachments.size() > 0) {
        Multipart multipart = new MimeMultipart();

        if (content != null) {
          MimeBodyPart messagePart = new MimeBodyPart();
          if (localContentType == null) {
            localContentType = "text/plain; charset=utf-8";
          }
          messagePart.setContent(content, localContentType);
          multipart.addBodyPart(messagePart);
        }

        MimeBodyPart attachmentPart = null;
        for (File attachmentFile : attachments) {
          attachmentPart = new MimeBodyPart();
          if (attachmentFile.exists() && attachmentFile.canRead()) {
            attachmentPart.attachFile(attachmentFile);
            multipart.addBodyPart(attachmentPart);
          }
        }

        message.setContent(multipart);
      } else {
        if (content != null) {
          if (localContentType == null) {
            localContentType = "text/plain; charset=utf-8";
          }
          message.setContent(content, localContentType);
        }
      }

      transport.connect();
      transport.sendMessage(message, message.getAllRecipients());
      transport.close();
    } catch (final AddressException exception) {
      log4j.error(exception);
      throw new ServletException(exception);
    } catch (final MessagingException exception) {
      log4j.error(exception);
      throw new ServletException(exception.getMessage(), exception);
    }

  }

  public static Session getSessionMail(String host, boolean auth, String username, String password,
      String connSecurity, int port) {
    String localConnSecurity = connSecurity;

    Properties props = new Properties();

    if (log4j.isDebugEnabled()) {
      props.put("mail.debug", "true");
    }
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);

    props.put("mail.smtp.timeout", SMTP_TIMEOUT);
    props.put("mail.smtp.connectiontimeout", SMTP_TIMEOUT);

    if (localConnSecurity != null) {
      localConnSecurity = localConnSecurity.replaceAll(", *", ",");
      String[] connSecurityArray = localConnSecurity.split(",");
      for (int i = 0; i < connSecurityArray.length; i++) {
        if ("STARTTLS".equals(connSecurityArray[i])) {
          props.put("mail.smtp.starttls.enable", "true");
        }
        if ("SSL".equals(connSecurityArray[i])) {
          props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
          props.put("mail.smtp.socketFactory.fallback", "false");
          props.put("mail.smtp.socketFactory.port", port);
          props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        }
      }
    }
    Session mailSession = null;
    if (auth) {
      props.put("mail.smtp.auth", "true");
      Authenticator authentification = new SMTPAuthenticator(username, password);
      mailSession = Session.getInstance(props, authentification);
    } else {
      mailSession = Session.getInstance(props, null);
    }
    return mailSession;

  }

  private static class SMTPAuthenticator extends javax.mail.Authenticator {
    private String _username;
    private String _password;

    public SMTPAuthenticator(String username, String password) {
      _username = username;
      _password = password;
    }

    public PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(_username, _password);
    }
  }

}
