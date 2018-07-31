/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.sendmails;

import com.altaik.bo.Lot;
import com.altaik.bo.Purchase;
import com.altaik.bo.Purchases;
import com.altaik.bo.Recipient;
import com.altaik.bo.settings.DeliverySettings;
import com.altaik.bp.purchase.PurchaseProcesses;
import com.altaik.bp.recipient.Recipients;
import com.altaik.db.DatabaseManager;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.altaik.parser.sendmails.Filter.addIgnore;
import static com.altaik.parser.sendmails.Filter.isRecipient;

/**
 *
 * @author Aset
 */
public class SendEmails {

    protected static final Logger logger = Logger.getLogger(SendEmails.class.getName());

    private static final int NUMBER_OF_MAX_SENT_PURCHASES = 30;

//    static String outgoingMailServer = "smtp.gmail.com";
//    static String outgoingMailPort = "587";
//    static final String username = "com.altaik.db.altatender@gmail.com";
//    static final String password = "ikalta7&";
    static boolean _TEST_MODE = false;
    static String testEmail = null;

    static String outgoingMailServer = "88.204.230.205";
    static String outgoingMailPort = "25";
    static final String username = "delivery@com.altaik.db.altatender.kz";
    static final String password = "ghjc20vjnh";

    static Properties props;
    static Session session;

    public static void Execute() {
        try (DatabaseManager dbManager = new DatabaseManager(true); Recipients recipients = new Recipients(dbManager)) {

            for (Recipient recipient : recipients) {
                if (isRecipient(recipient, dbManager)) {
                    try {
                        if (recipient.customer == null && recipient.user == null) {
                            logger.log(Level.WARNING, "Not found customer and user");
                            continue;
                        } else if ((recipient.customer == null || recipient.customer.email == null || recipient.customer.email.isEmpty()) && (recipient.user == null || recipient.user.email == null || recipient.user.email.isEmpty())) {
                            logger.log(Level.WARNING, "Not found emails for sender");
                            continue;
                        }
                        logger.log(Level.INFO, "Preparing purchases for recipient: {0}", recipient.user.email);

                        String keysQuery = "";
                        for (DeliverySettings settings : recipient.deliverySettingsCollection) {
                            //logger.log(Level.INFO, "In the delivery settings collection");
                            keysQuery += (keysQuery.isEmpty() ? "" : " OR ") + settings.GetWhereQuery();
                        }
                        String whereQuery = "WHERE p.id>" + recipient.user.lastpurchase + " AND p.dendday>=now() AND p.istatus=1";
                        if (!keysQuery.isEmpty()) {
                            whereQuery += " AND (" + keysQuery + ")";
                        }
                        whereQuery += addIgnore(recipient, dbManager);

//                String purchaseQuery = "SELECT DISTINCT p.* FROM procpurchase p LEFT JOIN lots l ON p.number=l.negnumber " + whereQuery + " ORDER BY p.dendday,l.sum,p.id";
                        String viewQuery = "SELECT p.*,l.* FROM procpurchase p LEFT JOIN lots l ON p.number=l.negnumber " + whereQuery + " ORDER BY p.isum desc, p.dendday, l.isum desc LIMIT 0,30;";

                        logger.log(Level.INFO, "QUERY: {0}", viewQuery);

                        PurchaseProcesses purchaseProcesses = new PurchaseProcesses(dbManager);
//                Purchases purchases = purchaseProcesses.PurchasesLoad(purchaseQuery);
                        Purchases purchases = purchaseProcesses.ViewLoad(viewQuery);

                        Purchases sentPurchases = new Purchases();

                        for (Purchase purchase : purchases) {
                            for (DeliverySettings settings : recipient.deliverySettingsCollection) {
                                purchase.ruName = settings.TripleEmphasize(purchase.ruName);
                                purchase.kzName = settings.TripleEmphasize(purchase.kzName);
                                if (purchase.lots == null) {
                                    continue;
                                }
                                for (Lot lot : purchase.lots) {
                                    lot.ruName = settings.TripleEmphasize(lot.ruName);
                                    lot.ruDescription = settings.TripleEmphasize(lot.ruDescription);
                                    lot.kzName = settings.TripleEmphasize(lot.kzName);
                                    lot.kzDescription = settings.TripleEmphasize(lot.kzDescription);
                                }
                            }
                            sentPurchases.add(purchase);
                            if (sentPurchases.size() == NUMBER_OF_MAX_SENT_PURCHASES) {
                                break;
                            }
                        }

                        try (HtmlMessage htmlMessage = new HtmlMessage(sentPurchases)) {
                            String message = htmlMessage.GetPreparedHtml();
                            if (!message.isEmpty()) {

                                logger.log(Level.INFO, "lastpurchase before {0}", recipient.user.lastpurchase);

                                if (htmlMessage.lastProcessedPurchaseId > recipient.user.lastpurchase) {
                                    recipient.user.lastpurchase = htmlMessage.lastProcessedPurchaseId;
                                }

                                try (EmailSender sender = new EmailSender()) {
                                    String email = recipient.customer != null && recipient.customer.email != null && !recipient.customer.email.isEmpty() ? recipient.customer.email : recipient.user.email;
                                    if (_TEST_MODE) {
                                        if (testEmail != null) {
                                            if (!email.equals(testEmail)) {
                                                logger.log(Level.INFO, "Next recipient");
                                                continue;
                                            } else {
                                                sender.Send(email, message);
                                            }
                                        }
                                        logger.log(Level.INFO, "Message sent to {0}({1})", new Object[]{email, recipient.customer != null && recipient.customer.email != null && !recipient.customer.email.isEmpty() ? "customer email" : "user email"});

                                    } else {
                                        dbManager.Insert("update users set lastpurchase = " + recipient.user.lastpurchase + " where id='" + recipient.user.id + "'");
                                        sender.Send(email, message);
                                    }
//                            sender.Send("aset.b@rambler.ru", message);
//                            sender.Send("t6166634@gmail.ru", message);
                                    logger.log(Level.INFO, "lastpurchase after {0}", recipient.user.lastpurchase);

                                }
                            }
                        }

                        purchases.clear();
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, "Error processing client {0}. Error: {1}", new Object[]{recipient.customer.email, ex.getMessage()});
                    }
                } else {
                    logger.log(Level.INFO, "Recipient status is ignored. ID = " + recipient.user.id + " email = " + recipient.user.email);
                }
            }
        }
    }

    public static boolean Send(String from, String emails, String subject, String mesText) {

        if (null == props) {
            props = new Properties();
            props.put("mail.smtp.auth", "true");
//            properties.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", outgoingMailServer);
            props.put("mail.smtp.port", outgoingMailPort);
        }

        if (null == session) {
            session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        }

        try {
            Message message = new MimeMessage(session);
            message.setHeader("Content-Type", "text/html; charset=UTF-8");
            message.setFrom(new InternetAddress(username, from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emails));
            message.setSubject(subject);
            ((MimeMessage) message).setText(mesText, "utf8", "html");

            Transport.send(message);

//            Transport transport = session.getTransport("smtps");
//            transport.connect(outgoingMailServer, 465, username, password);
//            transport.sendMessage(message, message.getAllRecipients());
//            transport.close();
            logger.log(Level.INFO, "Message sent to emails: {0}", emails);
            return true;

        } catch (MessagingException | UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public static boolean Send(String emails, String subject, String mesText) {

        if (null == props) {
            props = new Properties();
            props.put("mail.smtp.auth", "true");
//            properties.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", outgoingMailServer);
            props.put("mail.smtp.port", outgoingMailPort);
        }

        if (null == session) {
            session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        }

        try {
            Message message = new MimeMessage(session);
            message.setHeader("Content-Type", "text/html; charset=UTF-8");
            message.setFrom(new InternetAddress(username, "Альта и К"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emails));
            message.setSubject(subject);
            ((MimeMessage) message).setText(mesText, "utf8", "html");

            Transport.send(message);

//            Transport transport = session.getTransport("smtps");
//            transport.connect(outgoingMailServer, 465, username, password);
//            transport.sendMessage(message, message.getAllRecipients());
//            transport.close();
            logger.log(Level.INFO, "Message sent to emails: {0}", emails);
            return true;

        } catch (MessagingException | UnsupportedEncodingException ex) {
            logger.log(Level.SEVERE, null, ex);
            return false;
        }
    }

    public static void Send(String emails, String mesText) {
        Send(emails, "Тендер рассылка по Вашему запросу", mesText);
    }

//    public static void main(String[] args) {
////        logger.setLevel(Level.ALL);
//        InputStream stream = SendEmails.class.getResourceAsStream("/settings/application.properties");
//        try {
//            LogManager.getLogManager().readConfiguration(stream);
//        } catch (IOException | SecurityException ex) {
//            logger.log(Level.SEVERE, "Error load settings.\n{0}", ex);
//            return;
//        }
//
////        FileHandler fh = null;
////        try {
////            fh = new FileHandler("sendmails_logger.log");
////        } catch (IOException ex) {
////            logger.log(Level.SEVERE, null, ex);
////            return;
////        } catch (SecurityException ex) {
////            logger.log(Level.SEVERE, null, ex);
////            return;
////        }
////        fh.setFormatter(new SimpleFormatter());
////        logger.addHandler(fh);
//        if (args.length > 0) {
//            switch (args[0].toLowerCase()) {
//                case ("test"): {
//                    _TEST_MODE = true;
//                    logger.log(Level.INFO, "Running {0}", args[0].toLowerCase());
//                    EtsProcess sendEmailsEts = new EtsProcess(_TEST_MODE);
//                    sendEmailsEts.Do();
//                }
//                break;
//                case ("testemail"): {
//                    _TEST_MODE = true;
//                    testEmail = args[1];
//                    logger.log(Level.INFO, "Running {0}({1})", new Object[]{args[0].toLowerCase(), testEmail});
//                    EtsProcess sendEmailsEts = new EtsProcess(_TEST_MODE);
//                    sendEmailsEts.Do();
//                }
//                break;
//                case ("ets"): {
//                    EtsProcess sendEmailsEts = new EtsProcess();
//                    sendEmailsEts.Do();
//                }
//                break;
//                case ("com.altaik.db.altatender"): {
//                    Execute();
//                }
//                break;
//                default: {
//                    logger.log(Level.SEVERE, "Error argument.");
//                    return;
//                }
//            }
//        }
////        Execute();
////
////        EtsProcess sendEmailsEts = new EtsProcess(_TEST_MODE);
////        sendEmailsEts.Do();
////        CoreImpl();
//
//        logger.log(Level.SEVERE, "Finish.");
//    }

    public static void SpamExecute() {

    }

    //initial implementation
    public static void CoreImpl() {
        DatabaseManager dbManager = new DatabaseManager();

        String emailquery = "select a.id,a.primeemail,a.lastpurchase,a.keyword1,a.keyword2,a.keyword3,c.email as secondemail,c.runame,c.firstname,c.lastname,r.region,m.method,s.source,e.minsum,e.maxsum from (select u.id, u.email as primeemail,u.lastpurchase,d.keyword1,d.keyword2,d.keyword3 from users u, deliveries d where u.id=d.userid and (d.keyword1 is not null or d.keyword2 is not null or d.keyword3 is not null)) a left join customer c on a.id=c.userid left join userregions r on a.id=r.userid left join usermethods m on a.id=m.userid left join usersources s on a.id=s.userid left join usersumrange e on a.id=e.userid order by id";

        String lastpurchase = "";
        ResultSet emailSet = dbManager.Execute(emailquery);
        try {
            while (null != emailSet && emailSet.next()) {
                try {
                    lastpurchase = emailSet.getString("lastpurchase");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (null == lastpurchase || lastpurchase.isEmpty()) {
                    lastpurchase = "5000";
                }
                logger.log(Level.INFO, "lastpurchase before {0}", lastpurchase);

                String userid = null;
                try {
                    userid = emailSet.getString("id");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                    continue;
                }
                if (null == userid || userid.isEmpty() || userid.equals("214")) {
                    continue;
                }

//                int iuserid = Integer.parseInt(userid);
//                if(iuserid < 247) continue;
                String email;
                String primeemail = null;
                try {
                    primeemail = emailSet.getString("primeemail");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                String secondemail = null;
                try {
                    secondemail = emailSet.getString("secondemail");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (secondemail != null && !secondemail.isEmpty()) {
                    email = secondemail;
                } else {
                    email = primeemail;
                }
                //email = "aset.b@rambler.ru";
                if (null == email || email.isEmpty()) {
                    continue;
                }

                String client = "";
                try {
                    client = emailSet.getString("runame");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                logger.log(Level.INFO, "Client " + client + " processing...");

                String keyquery = "";

                String keyword1 = null;
                try {
                    keyword1 = emailSet.getString("keyword1");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (null != keyword1 && !keyword1.isEmpty()) {
                    keyquery = keyquery + Like(keyword1);
                }

                String keyword2 = null;
                try {
                    keyword2 = emailSet.getString("keyword2");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (null != keyword2 && !keyword2.isEmpty()) {
                    if (keyquery.length() > 1) {
                        keyquery = keyquery + " OR";
                    }
                    keyquery = keyquery + Like(keyword2);
                }

                String keyword3 = null;
                try {
                    keyword3 = emailSet.getString("keyword3");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (null != keyword3 && !keyword3.isEmpty()) {
                    if (keyquery.length() > 1) {
                        keyquery = keyquery + " OR";
                    }
                    keyquery = keyquery + Like(keyword3);
                }

                String maxquery = " select count(*) as coun";
                maxquery = maxquery + " from procpurchase p left join lots l on p.number=l.negnumber left join methodsenum m on p.method=m.id left join sites s on p.source=s.siteid";
                maxquery = maxquery + " where (" + keyquery + ") and p.istatus=1 and p.dendday >= now() AND p.id > " + lastpurchase;

                String region = null;
                try {
                    region = emailSet.getString("region");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (null != region) {
                    maxquery = maxquery + " and (p.venue='" + region + "' or p.venue is null)";
                }
                String method = null;
                try {
                    method = emailSet.getString("method");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (null != method) {
                    maxquery = maxquery + " and (p.method='" + method + "' or p.method is null)";
                }
                String source = null;
                try {
                    source = emailSet.getString("source");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (null != source) {
                    maxquery = maxquery + " and (p.source='" + source + "' or p.source is null)";
                }

                ResultSet maxSet = dbManager.Execute(maxquery);
                int max = 0;
                try {
                    maxSet.next();
                    logger.log(Level.INFO, "Max: " + maxSet.getString("coun"));
                    max = maxSet.getInt("coun");
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }

                //String today = new SimpleDateFormat("dd.MM.yyyy").format(Calendar.getInstance().getTime());
                String query = "SELECT p.id,p.source,p.type,p.runame AS pru,s.sitename,p.link AS plink,l.runame,p.number AS pnum,l.number AS number,l.rudescription,l.link AS llink,p.customer,m.name AS methodname,p.startday,p.endday,l.quantity,l.price,l.sum,l.deliveryplace";
                query = query + " FROM procpurchase p LEFT JOIN lots l ON p.number=l.negnumber LEFT JOIN methodsenum m ON p.method=m.id LEFT JOIN sites s ON p.source=s.siteid";
                query = query + " WHERE (" + keyquery + ") AND p.istatus=1 AND p.dendday >= now() AND p.id > " + lastpurchase;

                if (null != region) {
                    query = query + " AND (p.venue='" + region + "' OR p.venue is null)";
                }
                if (null != method) {
                    query = query + " AND (p.method='" + method + "' OR p.method is null)";
                }
                if (null != source) {
                    query = query + " AND (p.source='" + source + "' OR p.source is null)";
                }

//            query = query + " ORDER BY p.dendday, p.number desc, l.sum desc";
                query = query + " ORDER BY l.isum desc";
//                if (max > 90) {
//                    query = query + " LIMIT " + String.valueOf(max - 90) + ",30";
//                } else if (max > 60) {
//                    query = query + " LIMIT " + String.valueOf(max - 60) + ",30";
//                } else if (max > 30) {
//                    query = query + " LIMIT " + String.valueOf(max - 30) + ",30";
//                } else {
                query = query + " LIMIT 0,30";
//                }

//                logger.log(Level.INFO, query);
                ResultSet purchaseSet = dbManager.Execute(query);

                String mesText = "Рассылка пуста.";
                boolean first = true;
                int i = 0;
                String pnumprev = "";
                boolean isOpen = false;
                try {
                    while (null != purchaseSet && purchaseSet.next()) {
                        if (first) {
                            mesText = "";
                            first = false;
                        }
                        String pnum = "";
                        try {
                            pnum = purchaseSet.getString("pnum");
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        if (!pnum.equals(pnumprev)) {
                            pnumprev = pnum;
                            if (isOpen) {
                                mesText = mesText + "</table>";
                                isOpen = false;
                            }
                            mesText = mesText + "<br/><br/><table cellpadding=\"0\" cellspacing=\"0\" width=\"800\" align=\"left\" style=\"border-collapse:collapse; border:#cccccc 1px solid; background-color: whitesmoke; margin:10px\"><th width=100px></th><th></th><tr><td><br/></td><td><br/></td></tr><tr><td><br/></td><td><br/></td></tr>";
                            isOpen = true;
                            mesText = mesText + "<tr>";
                            mesText = mesText + "<td><b>ОБЪЯВЛЕНИЕ</b></td><td> " + pnum + "</td></tr><tr>";
                            String pru = null;
                            try {
                                pru = purchaseSet.getString("pru");
                            } catch (SQLException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                            if (null != pru && !pru.isEmpty()) {
                                pru = TripleEmphasize(pru, keyword1, keyword2, keyword3);
                                mesText = mesText + "<td></td><td>" + pru + "</td>";
                            }
                            mesText = mesText + "</tr><tr>";
                            mesText = mesText + "<td>Источник:</td><td>";
                            String plink = null;
                            try {
                                plink = purchaseSet.getString("plink");
                            } catch (SQLException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                            if (null != plink && !plink.isEmpty()) {
                                mesText = mesText + "<a href=\'" + plink + "\'>";
                            } else {
                                String psource = "";
                                try {
                                    psource = purchaseSet.getString("source");
                                } catch (SQLException ex) {
                                    logger.log(Level.SEVERE, null, ex);
                                }
                                if (psource.equals("1")) {
                                    String type = "";
                                    try {
                                        type = purchaseSet.getString("type");
                                    } catch (SQLException ex) {
                                        logger.log(Level.SEVERE, null, ex);
                                    }
                                    if (type == null) {
                                        plink = purchaseSet.getString("link");
                                    } else {
                                        if (type.equals("1")) {
                                            plink = "http://portal.goszakup.gov.kz/portal/index.php/ru/oebs/showbuy/" + pnum + "/" + pnum;
                                        }
                                        if (type.equals("2")) {
                                            plink = "http://portal.goszakup.gov.kz/portal/index.php/ru/publictrade/showbuy/" + pnum;
                                        }
                                        if (type.equals("3")) {
                                            plink = "http://portal.goszakup.gov.kz/portal/index.php/ru/oebs/showauc/" + pnum;
                                        }
                                        if (type.equals("4")) {
                                            plink = "https://v3bl.goszakup.gov.kz/ru/announce/actionAjaxModalShowFiles/" + pnum;
                                        }
                                    }
                                }
                                if (psource.equals("2")) {
                                    plink = "http://tender.sk.kz/index.php/ru/negs/show/" + pnum;
                                }
                                if (null != plink && !plink.isEmpty()) {
                                    mesText = mesText + "<a href=\'" + plink + "\'>";
                                }
                            }
                            try {
                                mesText = mesText + purchaseSet.getString("sitename");
                            } catch (SQLException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                            if (null != plink && !plink.isEmpty()) {
                                mesText = mesText + "</a>";
                            }
                            mesText = mesText + "</td>";
                            mesText = mesText + "</tr><tr>";
                            String customer = null;
                            try {
                                customer = purchaseSet.getString("customer");
                            } catch (SQLException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                            if (null != customer && !customer.isEmpty()) {
                                mesText = mesText + "<td>Организатор:</td><td>" + customer + "</td>";
                            }
                            mesText = mesText + "</tr><tr>";
                            String pmethod = null;
                            try {
                                pmethod = purchaseSet.getString("methodname");
                            } catch (SQLException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                            if (null != pmethod && !pmethod.isEmpty()) {
                                mesText = mesText + "<td>Метод:</td><td>" + pmethod + "</td>";
                            }
                            mesText = mesText + "</tr><tr>";
                            String startday = null;
                            try {
                                startday = purchaseSet.getString("startday");
                            } catch (SQLException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                            String endday = null;
                            try {
                                endday = purchaseSet.getString("endday");
                            } catch (SQLException ex) {
                                logger.log(Level.SEVERE, null, ex);
                            }
                            if (null != startday && !startday.isEmpty() && null != endday && !endday.isEmpty()) {
                                mesText = mesText + "<td>Дата начала:</td><td>" + startday + "</td>";
                                mesText = mesText + "</tr><tr>";
                                mesText = mesText + "<td>Дата окончания:</td><td>" + endday + "</td>";
                            }
                            mesText = mesText + "</tr><tr>";
                        }

                        String runame = null;
                        try {
                            runame = purchaseSet.getString("runame");
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        if (null != runame && !runame.isEmpty()) {
                            mesText = mesText + "<td><br/></td><td><br/></td></tr><tr><td><b>ЛОТ:</b></td><td>";
                            runame = TripleEmphasize(runame, keyword1, keyword2, keyword3);
                            mesText = mesText + runame;
                            mesText = mesText + "</td></tr><tr>";
                        }

//                mesText = mesText + "Номер: " + purchaseSet.getString("pnum") + "/" + purchaseSet.getString("number") + "\n";
                        String description = null;
                        try {
                            description = purchaseSet.getString("rudescription");
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        if (null != description && !description.isEmpty()) {
                            description = TripleEmphasize(description, keyword1, keyword2, keyword3);
                            mesText = mesText + "<td>Описание:</td><td>" + description + "</td>";
                        }
                        mesText = mesText + "</tr><tr>";
                        String llink = null;
                        try {
                            llink = purchaseSet.getString("llink");
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        if (null != llink && !llink.isEmpty()) {
                            mesText = mesText + "<td>Ссылка:</td><td>" + llink + "</td>";
                        }
                        mesText = mesText + "</tr><tr>";
                        try {
                            if (null != purchaseSet.getString("deliveryplace")) {
                                mesText = mesText + "<td>Место поставки:</td><td>" + purchaseSet.getString("deliveryplace") + "</td>";
                            }
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        mesText = mesText + "</tr><tr>";
                        try {
                            if (null != purchaseSet.getString("quantity")) {
                                mesText = mesText + "<td>Количество:</td><td>" + purchaseSet.getString("quantity") + "</td>";
                            }
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        mesText = mesText + "</tr><tr>";
                        try {
                            if (null != purchaseSet.getString("price")) {
                                mesText = mesText + "<td>Цена:</td><td>" + purchaseSet.getString("price") + "</td>";
                            }
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        mesText = mesText + "</tr><tr>";
                        try {
                            if (null != purchaseSet.getString("sum")) {
                                mesText = mesText + "<td>Сумма:</td><td>" + purchaseSet.getString("sum") + "</td>";
                            }
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        mesText = mesText + "</tr>";

                        String purchaseid = null;
                        try {
                            purchaseid = purchaseSet.getString("id");
                        } catch (SQLException ex) {
                            logger.log(Level.SEVERE, null, ex);
                        }
                        if (null != purchaseid && !purchaseid.isEmpty() && Integer.parseInt(purchaseid) > Integer.parseInt(lastpurchase)) {
                            lastpurchase = purchaseid;
                        }
                        i++;
                    }
                } catch (SQLException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
                if (isOpen) {
                    mesText = mesText + "</table>";
                }
                if (!mesText.isEmpty()) {
                    mesText = mesText + "</body></html>";
                }
                logger.log(Level.INFO, "Number lots: " + String.valueOf(i));

                if (i > 0) {
                    mesText = "<center><h3>" + Math.max(i, max) + " новых лотов по автопоиску</h3><center>" + mesText;
                    mesText = "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><style>td {padding:1px;}</style></head><body>" + mesText;
                    mesText = "<html>" + mesText;
                    logger.log(Level.INFO, "lastpurchase after " + lastpurchase + " and " + i + " lots prepared.");
                    dbManager.Insert("update users set lastpurchase = " + lastpurchase + " where id='" + userid + "'");

                    Send(email, mesText);
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
        logger.log(Level.INFO, "Finish.");

    }

//<editor-fold defaultstate="collapsed" desc="old morpho analysis">
    public static String Lingwo(String keya) {
        keya = keya.trim();
        if (keya.length() > 6 && (keya.endsWith("ого"))) {
            keya = keya.substring(0, keya.length() - 3);
        } else if (keya.length() > 5 && (keya.endsWith("ая") || keya.endsWith("oe") || keya.endsWith("ый") || keya.endsWith("ой") || keya.endsWith("ые"))) {
            keya = keya.substring(0, keya.length() - 2);
        } else if (keya.length() > 4 && (keya.endsWith("а") || keya.endsWith("о") || keya.endsWith("е") || keya.endsWith("и") || keya.endsWith("ы") || keya.endsWith("у") || keya.endsWith("ю"))) {
            keya = keya.substring(0, keya.length() - 1);
        }
        return keya;
    }

    public static String Morpho(String keya) {
        keya = keya.trim();
        if (keya.length() > 6 && (keya.endsWith("ого"))) {
            keya = keya.substring(0, keya.length() - 3) + "___";
        } else if (keya.length() > 5 && (keya.endsWith("ая") || keya.endsWith("oe") || keya.endsWith("ый") || keya.endsWith("ой") || keya.endsWith("ые"))) {
            keya = keya.substring(0, keya.length() - 2) + "__";
        } else if (keya.length() > 4 && (keya.endsWith("а") || keya.endsWith("о") || keya.endsWith("е") || keya.endsWith("и") || keya.endsWith("ы") || keya.endsWith("у") || keya.endsWith("ю"))) {
            keya = keya.substring(0, keya.length() - 1) + "_";
        }
        return keya;
    }

    public static String Like(String keyword) {
        String keyquery = "";
        String keyquery1 = "";
        String keyquery2 = "";
        String keyquery3 = "";
        String keyquery4 = "";
        String keyquery5 = "";
        String keyquery6 = "";
        String[] keyarray = keyword.split(",");
        for (String keya : keyarray) {
            String[] keyarray2 = keya.trim().split(" ");
            String key = "";
            for (String keyb : keyarray2) {
                keyb = Morpho(keyb);
                key = key + keyb + " ";
            }
            if (!key.isEmpty()) {
                key = key.substring(0, key.length() - 1);
                keyquery1 = keyquery1 + " p.runame LIKE '%" + key + "%' OR";
                keyquery2 = keyquery2 + " l.runame LIKE '%" + key + "%' OR";
                keyquery3 = keyquery3 + " l.rudescription LIKE '%" + key + "%' OR";
                keyquery4 = keyquery4 + " p.kzname LIKE '%" + key + "%' OR";
                keyquery5 = keyquery5 + " l.kzname LIKE '%" + key + "%'  OR";
                keyquery6 = keyquery6 + " l.kzdescription LIKE '%" + key + "%' OR";
            }
        }
        if (!keyquery1.isEmpty()) {
            keyquery = keyquery + " (" + keyquery1.substring(0, keyquery1.length() - 3) + ")";
        }
        if (!keyquery2.isEmpty()) {
            if (keyquery.length() > 1) {
                keyquery = keyquery + " OR";
            }
            keyquery = keyquery + " (" + keyquery2.substring(0, keyquery2.length() - 3) + ")";
        }
        if (!keyquery3.isEmpty()) {
            if (keyquery.length() > 1) {
                keyquery = keyquery + " OR";
            }
            keyquery = keyquery + " (" + keyquery3.substring(0, keyquery3.length() - 3) + ")";
        }
//            if (!keyquery14.isEmpty()) {
//                keyquery = keyquery + "    (" + keyquery14.substring(0, keyquery14.length() - 4) + ")";
//            }
//            if (!keyquery15.isEmpty()) {
//                keyquery = keyquery + " OR (" + keyquery15.substring(0, keyquery15.length() - 4) + ")";
//            }
//            if (!keyquery16.isEmpty()) {
//                keyquery = keyquery + " OR (" + keyquery16.substring(0, keyquery16.length() - 4) + ")";
//            }
        return keyquery;
    }

    public static String Replace(String word, String key) {
        if (word.contains(key)) {
            word = word.replace(key, "<u><b>" + key + "</b></u>");
        }
        return word;
    }

    public static String Emphasize(String word, String keyword) {
//        logger.log(Level.INFO, "Emphasize keword = \"{0}\"", keyword);
        if (keyword == null) {
            keyword = "";
        }
        String[] keyarray = keyword.replace(" ", ",").split(",");
        for (String keya : keyarray) {
            keya = Lingwo(keya);
            keya = keya.toLowerCase();
            if (!keya.isEmpty()) {
                word = Replace(word, keya);
                word = Replace(word, keya.toUpperCase());
                word = Replace(word, keya.substring(0, 1).toUpperCase() + keya.substring(1));
            }
        }
        return word;
    }

    public static String TripleEmphasize(String word, String keyword1, String keyword2, String keyword3) {
        word = Emphasize(word, keyword1);
        word = Emphasize(word, keyword2);
        word = Emphasize(word, keyword3);
        return word;
    }
//</editor-fold>
}
