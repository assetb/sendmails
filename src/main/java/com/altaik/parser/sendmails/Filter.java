package com.altaik.parser.sendmails;

import com.altaik.bo.Recipient;
import com.altaik.bo.settings.DeliverySettings;
import com.altaik.db.DatabaseManager;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Filter {

    protected static final Logger logger = Logger.getLogger(Filter.class.getName());

    public static String addIgnore(Recipient recipient, DatabaseManager databaseManager) throws SQLException {
        String whereQuerySection = " WHERE d.userid = " + recipient.user.id;
        String deliveriesQuery = "SELECT d.id,d.keyword1,d.keyword2,d.keyword3 FROM ignoredDeliveries as d " + whereQuerySection;
        ArrayList<DeliverySettings> deliverySettingsCollection = new ArrayList<>();
        DeliverySettings deliverySettings;
        ResultSet resultSet = databaseManager.Execute(deliveriesQuery);
        for (; null != resultSet && resultSet.next(); deliverySettingsCollection.add(deliverySettings)) {
            deliverySettings = new DeliverySettings();
            deliverySettings.keyword1 = resultSet.getString("keyword1");
            deliverySettings.keyword2 = resultSet.getString("keyword2");
            deliverySettings.keyword3 = resultSet.getString("keyword3");
            int deliveryId = resultSet.getInt("id");
            String deliveryIdQuerySection = whereQuerySection + " AND d.deliveryid = " + deliveryId;
            otherCriteriaFilter(deliverySettings, deliveryIdQuerySection, databaseManager);
        }
        String keysQuery = "";
        for (DeliverySettings settings : deliverySettingsCollection) {
            keysQuery += (keysQuery.isEmpty() ? "" : " OR ") + settings.GetWhereQuery();
        }
        String ignoreWhereQuery = "";
        if (!keysQuery.isEmpty()) {
            ignoreWhereQuery += " AND NOT (" + keysQuery + ")";
        }
        return (ignoreWhereQuery);
    }

    public static boolean isRecipient(Recipient recipient, DatabaseManager databaseManager) {
        String query = "SELECT isrecipient FROM ignoredDeliveries as d WHERE d.userid = " + recipient.user.id;
        try {
            ResultSet resultSet = databaseManager.Execute(query);
            int isrecipient = 1;
            if(resultSet.next()) {
                isrecipient = resultSet.getInt("isrecipient");
            }
            return isrecipient == 1;
        } catch (SQLException ex) {
            logger.log(Level.WARNING, ex.getMessage());
            return true;
        }
    }


    private static void otherCriteriaFilter(DeliverySettings deliverySettings, String deliveryIdQuerySection, DatabaseManager dbManager) {
        ResultSet ex1;
        Throwable var11;
        try {
            ex1 = dbManager.Execute("SELECT region FROM ignoreduserregion d " + deliveryIdQuerySection);
            var11 = null;

            try {
                deliverySettings.regions = new ArrayList();

                while (null != ex1 && ex1.next()) {
                    deliverySettings.regions.add(Integer.valueOf(ex1.getInt("region")));
                }
            } catch (Throwable var202) {
                var11 = var202;
                throw var202;
            } finally {
                if (ex1 != null) {
                    if (var11 != null) {
                        try {
                            ex1.close();
                        } catch (Throwable var197) {
                            var11.addSuppressed(var197);
                        }
                    } else {
                        ex1.close();
                    }
                }

            }
        } catch (SQLException var204) {
            Logger.getLogger(Filter.class.getName()).log(Level.SEVERE, (String) null, var204);
        }

        try {
            ex1 = dbManager.Execute("SELECT method FROM ignoredusermethod d " + deliveryIdQuerySection);
            var11 = null;

            try {
                deliverySettings.methods = new ArrayList();

                while (null != ex1 && ex1.next()) {
                    deliverySettings.methods.add(Integer.valueOf(ex1.getInt("method")));
                }
            } catch (Throwable var205) {
                var11 = var205;
                throw var205;
            } finally {
                if (ex1 != null) {
                    if (var11 != null) {
                        try {
                            ex1.close();
                        } catch (Throwable var196) {
                            var11.addSuppressed(var196);
                        }
                    } else {
                        ex1.close();
                    }
                }

            }
        } catch (SQLException var207) {
            Logger.getLogger(Filter.class.getName()).log(Level.SEVERE, (String) null, var207);
        }

        try {
            ex1 = dbManager.Execute("SELECT source FROM ignoredusersource d " + deliveryIdQuerySection);
            var11 = null;

            try {
                deliverySettings.sources = new ArrayList();

                while (null != ex1 && ex1.next()) {
                    deliverySettings.sources.add(Integer.valueOf(ex1.getInt("source")));
                }
            } catch (Throwable var208) {
                var11 = var208;
                throw var208;
            } finally {
                if (ex1 != null) {
                    if (var11 != null) {
                        try {
                            ex1.close();
                        } catch (Throwable var195) {
                            var11.addSuppressed(var195);
                        }
                    } else {
                        ex1.close();
                    }
                }

            }
        } catch (SQLException var210) {
            Logger.getLogger(Filter.class.getName()).log(Level.SEVERE, (String) null, var210);
        }
    }
}
