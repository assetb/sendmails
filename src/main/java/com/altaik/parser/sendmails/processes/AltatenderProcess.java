package com.altaik.parser.sendmails.processes;

import com.altaik.bo.*;
import com.altaik.bo.view.CurrentDayDeliveryView;
import com.altaik.db.HibernateRepository;
import com.altaik.db.IDatabaseManager;
import com.altaik.db.RepositoryFactory;
import com.altaik.parser.sendmails.mail.MailSender;
import de.neuland.jade4j.Jade4J;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Created by admin on 25.09.2017.
 */
public class AltatenderProcess extends SendMailProcesses {

    private final IDatabaseManager dbManager;
    private String testEmail;

    public AltatenderProcess(Properties properties) {
        super(properties);
        dbManager = RepositoryFactory.getRepository(HibernateRepository.class, properties);
        testEmail = getTestEmail();
    }

    @Override
    protected void onClose() {
        dbManager.close();
    }


    private void processing() {
        dbManager.clearCurrentDeliveries();
        List<Delivery> deliveries = dbManager.getDeliveries(true);
        List<CurrentDayDelivery> currentDayDeliveryList = new ArrayList<>();
        logger.info("find deliveries " + deliveries.size());

        for (Delivery delivery : deliveries) {
//            if (!delivery.getUser().getEmail().equalsIgnoreCase("7182cda@mail.ru"))
//                continue;
            List<Lot> lots = dbManager.filterDeliveryLots(delivery);
            logger.info(String.format("Find lots %d for user %s", lots.size(), delivery.getUser().getEmail()));

            for (int currCountDeliveries = 0, lotIndex = 0; lotIndex < lots.size() && currCountDeliveries < 30; lotIndex++) {
                Lot lot = lots.get(lotIndex);
                CurrentDayDelivery currentDayDelivery = new CurrentDayDelivery();
                currentDayDelivery.setEmail(delivery.getUser().getEmail());
                String keywords = (delivery.getKeyWordFirst() + ", " + delivery.getKeyWordTwo() + ", " + delivery.getKeyWordThree());
                keywords = keywords.replaceAll("[\\s,]+", ", ");

                if (keywords.length() > 255)
                    keywords = keywords.substring(0, 254);

                currentDayDelivery.setKeyword(keywords);
                currentDayDelivery.setRuName(lot.getProcessedPurchase().getRuName());
                currentDayDelivery.setKzName(lot.getProcessedPurchase().getKzName());
                currentDayDelivery.setPurchaseNumber(lot.getPurchaseNumber());
                currentDayDelivery.setUserId(delivery.getUserId());
                currentDayDelivery.setPurchaseId(lot.getProcessedPurchase().getId());

                if (currentDayDeliveryList.stream().noneMatch(currentDayDeliveryPredicate ->
                        currentDayDeliveryPredicate.getPurchaseId().equals(currentDayDelivery.getPurchaseId())
                                && currentDayDeliveryPredicate.getUserId().equals(currentDayDelivery.getUserId()))) {
                    currentDayDeliveryList.add(currentDayDelivery);
                    currCountDeliveries++;
                }
            }

            User user = delivery.getUser();

            if (user.getLastpurchase() == null)
                user.setLastpurchase(0);

            Integer lastPurchase = user.getLastpurchase();

            for (Lot lot : lots) {
                Integer currentPurchase = lot.getProcessedPurchase().getId();
                if (currentPurchase > lastPurchase) {
                    lastPurchase = currentPurchase;
                }
            }

            if (user.getLastpurchase() < lastPurchase) {

                user.setLastpurchase(lastPurchase);
                dbManager.updateUser(user);
            }
        }

        currentDayDeliveryList.forEach(dbManager::updateCurrentDeliveries);
    }

    private void sendMails() {
        Map<Integer, List<CurrentDayDeliveryView>> mailSettingsMap = dbManager.getCurrentDeliveriesView().stream().collect(Collectors.groupingBy(CurrentDayDeliveryView::getMailSettingsId));
        List<SettingsMailer> settingsMailers = dbManager.getSettingsMailSenders();
        mailSettingsMap.forEach((Integer index, List<CurrentDayDeliveryView> currentDayDeliveryList) -> {
            SettingsMailer currentSettingsMailer = settingsMailers.stream().filter(s -> s.getId() == index).findFirst().get();
            MailSender mailSender = new MailSender(
                    currentSettingsMailer.getServerIp(),
                    currentSettingsMailer.getServerPort(),
                    currentSettingsMailer.getLogin(),
                    currentSettingsMailer.getPassword(),
                    currentSettingsMailer.getSignature()
            );
            logger.log(Level.INFO, "Use settings sender mail {0}", currentSettingsMailer.getName());
            Map<String, List<CurrentDayDeliveryView>> emailsDeliveries = currentDayDeliveryList.stream().collect(Collectors.groupingBy(CurrentDayDeliveryView::getEmail));
            emailsDeliveries.forEach((email, currentDayDeliveryList1) -> {

                logger.log(Level.INFO, "Count found purchase {0} from {1}",
                        new Object[]{currentDayDeliveryList1.size(), email});

                try {
                    String msg = getMessage(currentDayDeliveryList1);
                    mailSender.send((isTestMode() && testEmail != null ? testEmail : email), currentSettingsMailer.getSubject(), msg);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Error send message to {0}. Error: {1}",
                            new Object[]{email, e.getMessage()});
                }
            });

        });
        dbManager.close();
    }

    private String getMessage(List<CurrentDayDeliveryView> currentDayDeliveryViews) throws IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        URL url = classLoader.getResource("altatender.message.jade");
        Map<String, Object> map = new HashMap<>();
        map.put("deliveries", currentDayDeliveryViews);
        map.put("count", currentDayDeliveryViews.size());

        return Jade4J.render(url, map);
    }

    @Override
    protected void onStart() {
        try {
            logger.info("Start processing deliveries");
            processing();
            logger.info("Finish processing");
            logger.info("Start sending mails");
            sendMails();
            logger.info("Finish");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fatal error in process.", e);
        }
    }
}
