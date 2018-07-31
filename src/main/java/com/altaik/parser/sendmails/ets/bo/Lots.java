/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.sendmails.ets.bo;

import com.altaik.bo.Lot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author admin
 */
public class Lots extends ArrayList<com.altaik.bo.Lot> {

    public static List<Lot> Parse(ResultSet resultSet) {
        List<Lot> lots = new ArrayList<>();
        if (resultSet != null) {
            try {
//                resultSet.first();
                while (resultSet.next()) {
                    Lot lot = new Lot();
                    lot.purchaseNumber = resultSet.getString("negnumber");
                    lot.lotNumber = resultSet.getString("number");
                    lot.kzName = resultSet.getString("kzname");
                    lot.ruName = resultSet.getString("runame");
                    lot.ruDescription = resultSet.getString("rudescription");
                    lot.kzDescription = resultSet.getString("kzdescription");
                    lot.price = resultSet.getString("price");
                    lot.sum = resultSet.getString("sum");
                    lot.unit = resultSet.getString("unit");
                    lot.quantity = resultSet.getString("quantity");
                    lot.deliveryPlace = resultSet.getString("deliveryplace");
                    lot.deliverySchedule = resultSet.getString("deliveryschedule");
                    lot.deliveryTerms = resultSet.getString("deliveryterms");
                    lots.add(lot);
                }
            } catch (SQLException ex) {
                Logger.getLogger(Lots.class.getName()).log(Level.SEVERE, null, ex);
                lots.clear();
                return lots;
            }
        }
        return lots;
    }
}
