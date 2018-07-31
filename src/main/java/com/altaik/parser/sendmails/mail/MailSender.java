package com.altaik.parser.sendmails.mail;

import javax.mail.Message;
import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by admin on 25.09.2017.
 */
public final class MailSender implements AutoCloseable {
    String signature;
    String login;
    String password;
    Properties properties = new Properties();
    Session session;
    Logger logger = Logger.getLogger(MailSender.class.getName());

    public MailSender(String login, String password, String signature) {
        this.signature = signature;
        this.login = login;
        this.password = password;
    }

    public MailSender(String server, String port, String login, String password, String signature) {
        this(login, password, signature);
        properties.put("mail.smtp.host", server);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", true);
    }

    private Session getSession() {
        if (session == null) {
            session = Session.getDefaultInstance(properties, new SMTPAuthenticator(login, password));
        }
        return session;
    }

    public boolean send(String emailTo, String subject, String message) {
        try {
            Message mimeMessage = new MimeMessage(getSession());
            mimeMessage.setHeader("Content-Type", "text/html; charset=UTF-8");
            mimeMessage.setFrom(new InternetAddress(login, signature));
            mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));
            mimeMessage.setSubject(subject);
            ((MimeMessage) mimeMessage).setText(message, "utf8", "html");
            Transport.send(mimeMessage);
            logger.log(Level.INFO, "Message sent to emails: {0}", emailTo);
            return true;
        } catch (UnsupportedEncodingException ex) {
            logger.log(Level.WARNING, "Unsupported email address of the sender ({0}) \n - {1}", new Object[]{login, ex.getMessage()});
        } catch (AddressException e) {
            logger.log(Level.WARNING, "Invalid recipient address {0}\n - {1}", new Object[]{emailTo, e.getMessage()});
        } catch (MessagingException e) {
            logger.log(Level.WARNING, "Error send mail to {0}\n - {1}", new Object[]{emailTo, e.getMessage()});
        } finally {
            return false;
        }
    }


    @Override
    public void close() throws Exception {

    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {
        String login;
        String password;

        public SMTPAuthenticator(String login, String password) {
            super();
            this.login = login;
            this.password = password;
        }

        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(login, password);
        }
    }
}
