/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package it.openia.crm;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import javax.servlet.ServletException;
import org.apache.log4j.Logger;


/**
 *
 * @author nicholas
 */
public class EmailManager{

    
    private static Logger log4j = Logger.getLogger(org.openbravo.erpCommon.utility.poc.EmailManager.class);
        
    public static void sendEmail(String host, boolean auth, String username, String password,
      String connSecurity, int port, String senderAddress, String recipientTO, String recipientCC,
      String recipientBCC, String replyTo, String subject, String content, String contentType,
      List<File> attachments, String attachmentType,Date sentDate, List<String> headerExtras) throws Exception {
    try {
      Properties props = new Properties();

      if (log4j.isDebugEnabled()) {
        props.put("mail.debug", "true");
      }
      props.put("mail.transport.protocol", "smtp");
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.port", port);

      if (connSecurity != null) {
        connSecurity = connSecurity.replaceAll(", *", ",");
        String[] connSecurityArray = connSecurity.split(",");
        for (int i = 0; i < connSecurityArray.length; i++) {
          if ("STARTTLS".equals(connSecurityArray[i])) {
            props.put("mail.smtp.starttls.enable", "true");
          }
          if ("SSL".equals(connSecurityArray[i])) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.socketFactory.port", port);
          }
        }
      }

      Session mailSession = null;
      if (auth) {
        props.put("mail.smtp.auth", "true");
        Authenticator authentification = new EmailManager.SMTPAuthenticator(username, password);
        mailSession = Session.getInstance(props, authentification);
      } else {
        mailSession = Session.getInstance(props, null);
      }

      Transport transport = mailSession.getTransport();

      MimeMessage message = new MimeMessage(mailSession);

      message.setFrom(new InternetAddress(senderAddress));

      if (recipientTO != null) {
        recipientTO = recipientTO.replaceAll(";", ",");
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientTO));
      }
      if (recipientCC != null) {
        recipientCC = recipientCC.replaceAll(";", ",");
        message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(recipientCC));
      }
      if (recipientBCC != null) {
        recipientBCC = recipientBCC.replaceAll(";", ",");
        message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(recipientBCC));
      }

      if (replyTo != null) {
        replyTo = replyTo.replaceAll(";", ",");
        replyTo = replyTo.replaceAll(", *", ",");
        String[] replyToArray = replyTo.split(",");

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
          if (contentType == null) {
            contentType = "text/plain; charset=utf-8";
          }
          messagePart.setContent(content, contentType);
          multipart.addBodyPart(messagePart);
        }
        
        int count=0;
        MimeBodyPart attachmentPart = null;
        for (File attachmentFile : attachments) {
          attachmentPart = new MimeBodyPart();
          
            if (attachmentFile.exists() && attachmentFile.canRead()) {
                
                FileDataSource f = new FileDataSource (attachmentFile)
                    {
                        @Override
                        public String getContentType() {
                            return "text/calendar;method=\"REQUEST\";charset=\"UTF-8\";";
                        }
                    };
                attachmentPart.setDataHandler(new DataHandler(f));
                attachmentPart.setFileName(f.getName());
                
                multipart.addBodyPart(attachmentPart);
          }
        }
        message.setContent(multipart);
      } else {
        if (content != null) {
          if (contentType == null) {
            contentType = "text/plain; charset=utf-8";
          }
          message.setContent(content, contentType);
        }
      }
      
      message.saveChanges();
      
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
