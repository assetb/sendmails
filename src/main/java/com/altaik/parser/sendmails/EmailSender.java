/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.sendmails;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.UnsupportedEncodingException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Aset
 */
public class EmailSender implements AutoCloseable {

    protected Logger logger = Logger.getLogger(EmailSender.class.getName());
    private boolean isAuth = true;
    private String outgoingMailServer = "88.204.230.205";
    private String outgoingMailPort = "25";
    private String username = "delivery@com.altaik.db.altatender.kz";
    private String password = "ghjc20vjnh";
    private String fullname = "Альта и К";
    private String defaulSubject = "Тендер рассылка по Вашему запросу";

    private Session session;

    private Message message;

    public EmailSender(String host, String port, String username1, String password1, String fullname) {
        this(username1, password1, fullname);
        setMailServer(host, port);
    }

    public EmailSender(String username1, String password1, String fullname) {
        setSenderSettings(fullname, username1, password1);
    }

    public EmailSender() {
    }

    public void setMailServer(String mailServer) {
        setMailServer(mailServer, "25", true);
    }

    public void setDefaulSubject(String subject) {
        this.defaulSubject = subject;
    }

    public void setMailServer(String mailServer, String port) {
        setMailServer(mailServer, port, true);
    }

    public void setMailServer(String mailServer, String port, boolean auth) {
        this.outgoingMailServer = mailServer;
        this.outgoingMailPort = port;
        this.isAuth = auth;
    }

    public void setSenderSettings(String name, String login, String password) {
        this.fullname = name;
        this.username = login;
        this.password = password;
    }

    public void sessionInitialize() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", isAuth ? "true" : "false");
        props.put("mail.smtp.host", outgoingMailServer);
        props.put("mail.smtp.port", outgoingMailPort);
        session = Session.getInstance(props,
                new javax.mail.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });
    }

    private void SetMessage(String subject, String emails, String mesText) throws MessagingException, UnsupportedEncodingException {
        if (session == null)
            sessionInitialize();

        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setHeader("Content-Type", "text/html; charset=utf-8");
        mimeMessage.setFrom(new InternetAddress(username, fullname));
        mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emails));
        mimeMessage.setSubject(MimeUtility.encodeText(subject, "utf-8", "Q"));
        mimeMessage.setText(mesText, "utf-8", "html");

        message = mimeMessage;
    }

    private void Send() throws MessagingException {
        Transport.send(message);
        logger.log(Level.INFO, "Message sent.");
    }

    /**
     * Отправить сообещние
     *
     * @param emails  Электронный адрес получателя
     * @param subject Тема письма
     * @param mesText Текст письма
     */
    public void Send(String emails, String subject, String mesText) {
        try {
            SetMessage(subject, emails, mesText);
            Send();
        } catch (Exception exeption) {
            logger.log(Level.SEVERE, "Error with send message: {0}", exeption);
        }
    }

    /**
     * @param emails  emails to send separated by ,
     * @param mesText text to send
     */
    public void Send(String emails, String mesText) {
        Send(emails, defaulSubject, mesText);
    }

    @Override
    public void close() {
        if (session != null) {
            session = null;
        }
    }

}
