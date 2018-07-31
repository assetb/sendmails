package com.altaik.parser.sendmails.db;

import com.altaik.bo.Customer;
import com.altaik.bo.Customers;
import com.altaik.bo.Lot;
import com.altaik.bo.utils.KeywordUtils;
import com.altaik.db.IDatabaseManager;
import com.altaik.parser.sendmails.ets.bo.Envelope;
import com.altaik.parser.sendmails.ets.bo.ProcPurchase;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.altaik.bo.utils.KeywordUtils.excludedPretextContains;

public class DatabaseRepository {
    private IDatabaseManager databaseManager;
    private Logger logger = Logger.getLogger(DatabaseRepository.class.getName());

    public DatabaseRepository(IDatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Добавляет конверт в базу данных
     *
     * @param envelope
     */
    public void insertEnvelope(Envelope envelope) {
        String query = String.format("insert into envelopes(email, description, sended) value('%s', '%s', %b)",
                envelope.getEmail(),
                envelope.getDescription(),
                envelope.isSended());
        if (databaseManager.Insert(query)) {
            envelope.setId(databaseManager.getLastId());
            List<ProcPurchase> purchases = envelope.getPurchases();

            if (purchases.size() > 0) {
                for (ProcPurchase purchase : purchases) {
                    String queryPurchase = String.format("insert into envelope_purchases(envelope_id, purchase_number) VALUE (%d, '%s')",
                            envelope.getId(),
                            purchase.number);
                    if (!databaseManager.Insert(queryPurchase))
                        logger.log(Level.WARNING, "Error with insert purchase (id {0}) for envelope(id {1})",
                                new Object[]{purchase.id, envelope.getId()});
                }
            }
        } else {
            logger.warning("Error with insert envelope");
        }
    }

    /**
     * Проверка адреса электронной почты на корректность
     *
     * @param email Адрес электронной почты
     * @return Возвращает true если электронный адрес корректный, иначе false
     * @author Vladimir Kovalev (v.kovalev@com.altaik.db.altatender.kz) on 12.03.2018
     */
    private boolean isEmailCorrected(String email) {
        if (email == null) {
            return false;
        }

        Pattern p = Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

        return p.matcher(email).find();
    }

    public void updateEnvelopenSended(int envelopeId, boolean sended) {
        String q = String.format("UPDATE envelopes SET sended = %b WHERE id = %d;", sended, envelopeId);
        databaseManager.Update(q);
    }

    /**
     * Обработка строки с перечнем электронных адресов
     *
     * @param emails Строка содержащая перечень электронных адресов
     * @return Позвращает перечен корректных электронных адресов
     * @author Vladimir Kovalev (v.kovalev@com.altaik.db.altatender.kz) on 12.03.2018
     */
    private String processingEmailAddresses(String emails) {
        StringBuilder currectedEmails = new StringBuilder();

        if (emails == null || emails.isEmpty())
            return null;


        for (String item : emails.split("[\\s,]+")) {

            if (isEmailCorrected(item)) {

                if (currectedEmails.length() > 0)
                    currectedEmails.append(", ");

                currectedEmails.append(item);
            }
        }

        return currectedEmails.toString();
    }

    /**
     * Поиск заказчика по электронному адресу
     *
     * @param customers Список заказчиков
     * @param email     Электронный адрес
     * @return Возвращает заказчика если электронный адрес совпал, иначе вернет null
     * @author Vladimir Kovalev (v.kovalev@com.altaik.db.altatender.kz) on 12.03.2018
     */
    private Customer findCustomerByEmail(Customers customers, String email) {

        if (customers != null && !customers.isEmpty()) {

            for (Customer customer : customers) {

                if (customer.email.equals(email)) {
                    return customer;
                }

            }

        }

        return null;
    }

    /**
     * Поиск новых объявлений
     *
     * @return Возвращает список найденых объявлений
     * @author Vladimir Kovalev (v.kovalev@com.altaik.db.altatender.kz) on 12.03.2018
     */
    public List<ProcPurchase> findNewPurchases() {
        List<ProcPurchase> purchases = new ArrayList<>();

        try {
            ResultSet resultSet = databaseManager.Execute("SELECT p.id, p.source, p.number, p.runame, p.customer, p.organizer, p.status, p.startday, p.endday, p.attribute, p.isum, p.type, p.venue, p.method FROM procpurchase as p where p.id > (SELECT id FROM ipurchaseets LIMIT 1) and (p.source in (4, 5, 10));");
//            ResultSet resultSet = databaseManager.Execute("SELECT p.id, p.source, p.number, p.runame, p.customer, p.organizer, p.status, p.startday, p.endday, p.attribute, p.isum, p.type, p.venue, p.method FROM procpurchase as p where (p.source in (4, 5, 10));");

            if (resultSet != null) {
                while (resultSet.next()) {
                    ProcPurchase procpurchase = new ProcPurchase();
                    procpurchase.id = resultSet.getInt("id");
                    procpurchase.isource = resultSet.getInt("source");
                    procpurchase.number = resultSet.getString("number");
                    procpurchase.ruName = resultSet.getString("runame");
                    procpurchase.customer = resultSet.getString("customer");
                    procpurchase.organizer = resultSet.getString("organizer");
                    procpurchase.status = resultSet.getString("status");
                    procpurchase.startDay = resultSet.getString("startday");
                    procpurchase.endDay = resultSet.getString("endday");
                    procpurchase.attribute = resultSet.getString("attribute");
                    procpurchase.fsum = resultSet.getFloat("isum");
                    procpurchase.type = resultSet.getString("type");
                    procpurchase.ivenue = resultSet.getInt("venue");
                    procpurchase.imethod = resultSet.getInt("method");
                    purchases.add(procpurchase);
                }
            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error get new purchase: {0}", ex);
            purchases.clear();
        }

        return purchases;
    }

    /**
     * Поиск лотов по номеру объявления
     *
     * @param purchaseNumber Номер объявления
     * @return Возвращает список лотов.
     */
    public List<Lot> getLotsByPurchaseNumber(String purchaseNumber) {
        List<Lot> lots = new ArrayList<>();

        try {
            ResultSet execute = databaseManager.Execute("SELECT * FROM lots WHERE negnumber = '" + purchaseNumber + "';");

            if (execute != null) {

                while (execute.next()) {
                    Lot lot = new Lot();
                    lot.purchaseNumber = execute.getString("negnumber");
                    lot.lotNumber = execute.getString("number");
                    lot.kzName = execute.getString("kzname");
                    lot.ruName = execute.getString("runame");
                    lot.ruDescription = execute.getString("rudescription");
                    lot.kzDescription = execute.getString("kzdescription");
                    lot.price = execute.getString("price");
                    lot.sum = execute.getString("sum");
                    lot.unit = execute.getString("unit");
                    lot.quantity = execute.getString("quantity");
                    lot.deliveryPlace = execute.getString("deliveryplace");
                    lot.deliverySchedule = execute.getString("deliveryschedule");
                    lot.deliveryTerms = execute.getString("deliveryterms");
                    lots.add(lot);
                }

            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error get lots by purchase number. {0}", ex);
            lots.clear();
        }

        return lots;
    }


    public void updateLastProcessedPurchase(int purchaseId) {
        databaseManager.Update("UPDATE ipurchaseets SET id = " + purchaseId + ";");
    }

    /**
     * Поиск заказчиков по наименованию товара/услуг
     *
     * @param productName Наименование товара/услуг
     * @return Возвращает список заказчиков
     */
    public Customers findCustomersForProductName(String productName) {
        Customers customers = new Customers();
        StringBuilder where = new StringBuilder();

        for (String product : productName.split("[\\s]+")) {

            if (product.isEmpty() || excludedPretextContains(product)) {
                continue;
            }

            product = KeywordUtils.WordRoot(product.replaceAll("[.,]+$", ""));

            if (where.length() > 0)
                where.append(" OR ");

            where.append("goodname LIKE '%").append(product).append("%'");
        }

        try {
            ResultSet res = databaseManager.Execute("SELECT email, runame FROM goodsbycompanyview WHERE email IS NOT NULL and (" + where + ");");

            while (res.next()) {
                String emails = processingEmailAddresses(res.getString("email"));
                String name = res.getString("runame");

                if (emails == null)
                    continue;

                for (String email : emails.split("[\\s,]+")) {
                    if (email.isEmpty())
                        continue;

                    Customer customer = findCustomerByEmail(customers, email);

                    if (customer == null)
                        customers.add(new Customer(email, name, "", ""));
                }

            }
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, "Error find customers for product name: {0}", ex);

            return null;
        }

        return customers;
    }

    public void close() {
        databaseManager.close();
    }
}
