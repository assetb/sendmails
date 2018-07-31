/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.sendmails.processes;

import com.altaik.bo.Customer;
import com.altaik.bo.Customers;
import com.altaik.bo.Lot;
import com.altaik.db.DatabaseManager;
import com.altaik.db.IDatabaseManager;
import com.altaik.db.RepositoryFactory;
import com.altaik.parser.sendmails.EmailSender;
import com.altaik.parser.sendmails.db.DatabaseRepository;
import com.altaik.parser.sendmails.ets.bo.Envelope;
import com.altaik.parser.sendmails.ets.bo.ProcPurchase;
import de.neuland.jade4j.Jade4J;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author admin
 */
public class EtsProcess extends SendMailProcesses {
    private static final int MAXIMUM_SIZE_THE_ENVELOPE = 30;
    private final DatabaseRepository databaseRepository;
    private final List<Envelope> envelopes = new ArrayList<>();
    private final EmailSender emailSender = new EmailSender();
    private List<ProcPurchase> procPurchases;

    private String testEmailCustomer;
    private String testEmail;

    public EtsProcess(Properties properties) {
        super(properties);
        IDatabaseManager dbManager = RepositoryFactory.getRepository(DatabaseManager.class, properties);
        databaseRepository = new DatabaseRepository(dbManager);
        procPurchases = databaseRepository.findNewPurchases();

        emailSender.setDefaulSubject("Рассылка новых объявлений с товарной биржы ЕТС");
        //todo Вставить данные почты корунда

        testEmailCustomer = getTestEmailCustomer();
        testEmail = getTestEmail();
    }

    @Override
    protected void onClose() {
        databaseRepository.close();
    }

    /**
     * Подготовка "конвертов" для отправки
     */
    private void preparationEnvelopes() {
        if (procPurchases == null)
            return;

        for (ProcPurchase procPurchase : procPurchases) {
            logger.log(Level.INFO, "Select lots for purchase {0}. ", procPurchase.number);
            procPurchase.lots = databaseRepository.getLotsByPurchaseNumber(procPurchase.number);
            logger.log(Level.INFO, "Found {0} lots", procPurchase.lots.size());

            for (Lot lot : procPurchase.lots) {
                logger.log(Level.INFO, "Select customers for lot {0}. ", lot.lotNumber);
                Customers customers = databaseRepository.findCustomersForProductName(lot.ruName);
                logger.log(Level.INFO, "Found {0} customers", customers.size());

                for (Customer customer : customers) {
                    Envelope envelope = envelopes.stream()
                            .filter(e -> e.getEmail().equals(customer.email))
                            .findFirst()
                            .orElse(null);

                    if (envelope == null) {
                        envelope = new Envelope(customer.email);
                        envelope.setDescription(customer.companyRuName);
                        envelopes.add(envelope);
                    }

                    List<ProcPurchase> purchases = envelope.getPurchases();
                    boolean isFindPurchase = purchases.stream().noneMatch((p) -> (p.number.equals(procPurchase.number)));

                    if (isFindPurchase && purchases.size() < MAXIMUM_SIZE_THE_ENVELOPE) {
                        purchases.add(procPurchase);
                    }
                }
            }
        }
    }

    /**
     * Сформировать текст HTML сообщения для откравки
     *
     * @param envelope Конферт с данными для отправки
     * @return Текст HTML сообщения
     * @throws IOException Ошибка возникает если не был найден ресурсный файл шаблона
     */
    private String getMessageBody(Envelope envelope) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource("ets.message.jade");
        List<ProcPurchase> purchases = envelope.getPurchases();
        Map<String, Object> dictionary = new HashMap<>();
        dictionary.put("envelope", envelope);
        dictionary.put("count", purchases.size());
        dictionary.put("purchases", purchases);

        return Jade4J.render(url, dictionary);
    }

    /**
     * Формирует сообщение отчета о работе
     *
     * @param envelopes Список обработанных конфертов
     * @return Текст HTML сообщения
     * @throws IOException Ошибка возникает если не был найден ресурсный файл шаблона
     */
    private String getOrderMessage(List<Envelope> envelopes) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource("ets.order.message.jade");
        Map<String, Object> map = new HashMap<>();
        int countSendedMessages = 0;

        List<Envelope> list = envelopes.stream().filter(e -> e.isSended()).collect(Collectors.toList());
        countSendedMessages = list.size();

//        for (Envelope envelope : envelopes) {
//            if (envelope.isSended())
//                countSendedMessages += 1;
//        }

        map.put("envelopes", list);
        map.put("createDate", new Date());
        map.put("countMessage", countSendedMessages);

        return Jade4J.render(url, map);
    }

    private void saveEnvelopes() {
        for (Envelope envelope : envelopes) {
            databaseRepository.insertEnvelope(envelope);
        }
    }

    /**
     * Процедура отправки всех конфертов
     */
    private void sendAllEnvelopes() {
        for (Envelope envelope : envelopes) {
            try {
                String email = envelope.getEmail();

                if (isTestMode() && testEmailCustomer != null && !email.equals(testEmailCustomer))
                    continue;

                String emailRecipient = isTestMode() && testEmail != null ? testEmail : email;
                String message = getMessageBody(envelope);
                logger.log(Level.INFO, "Ready send mail to {0}", email);
                emailSender.Send(emailRecipient, message);
                logger.log(Level.INFO, "Message sended.", email);
                envelope.setSended(true);
                databaseRepository.updateEnvelopenSended(envelope.getId(), envelope.isSended());
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Error with the send messages: {0}", ex.getMessage());
            }
        }
    }

    private void finishedAndGenerateOrder() {
        String emailForOrder = getEmailForOrder();
        if (emailForOrder != null) {
//            Document order = sendEmailsEtsUtils.GenerateOrder(envelopes);
//            emailSender.Send(emailForOrder, "Модуль рассылки. Отчет", order.html());
            try {
                String order = getOrderMessage(envelopes);
                emailSender.Send(emailForOrder, "Модуль рассылки. Отчет", order);
                logger.log(Level.INFO, "The report sended to {0}", emailForOrder);
            } catch (IOException ex) {
                logger.log(Level.SEVERE, "Error of generate report message. {0}", ex);
            }
        } else {
            logger.log(Level.INFO, "No email for report submission");
        }

        logger.log(Level.INFO, "{0} sent envelopes", envelopes.size());

        if (!isTestMode()) {
            int lastPurchaseId = GetMaxPurchaseId(procPurchases);

            if (lastPurchaseId > 0) {
                databaseRepository.updateLastProcessedPurchase(lastPurchaseId);
            }
        }

        logger.log(Level.INFO, "Finishing the work a ETS module");
    }

    public void Do() {
        logger.log(Level.INFO, "Started work the ETS module.");

        if (isTestMode()) {
            logger.log(Level.INFO, "Test mode");

            if (testEmailCustomer != null)
                logger.log(Level.INFO, "Test company email {0}", testEmailCustomer);

            if (testEmail != null)
                logger.log(Level.INFO, "Test recipient email {0}", testEmail);
        }

        preparationEnvelopes();
        saveEnvelopes();
        sendAllEnvelopes();
        finishedAndGenerateOrder();
    }

    private int GetMaxPurchaseId(List<ProcPurchase> purchases) {
        if (purchases.isEmpty())
            return 0;

        return purchases.stream().max(Comparator.comparing(p -> p.id)).get().id;
    }

    @Override
    protected void onStart() {
        try {
            Do();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Fatal error in process", ex);
        }
    }
}
