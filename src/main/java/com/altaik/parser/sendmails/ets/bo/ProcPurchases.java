/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.sendmails.ets.bo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author admin
 */
public class ProcPurchases extends ArrayList<ProcPurchase> {
    
    public int GetMaxId(){
        int max = 0;
        for(ProcPurchase procPurchase : this){
            if(max < procPurchase.id) max = procPurchase.id;
        }
        return max;
    }
    
    public static ProcPurchases Parse(ResultSet resultSet) {
        ProcPurchases purchases = null;
        if (resultSet != null) {
            try {
                purchases = new ProcPurchases();
//                resultSet.first();
                while (resultSet.next()) {
                    ProcPurchase procpurchase = new ProcPurchase();
                    procpurchase.id = resultSet.getInt("id");
                    procpurchase.isource = resultSet.getInt("source");
                    procpurchase.number = resultSet.getString("number");
                    procpurchase.ruName = resultSet.getString("runame");
                    procpurchase.customer = resultSet.getString("customer");
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
            } catch (SQLException ex) {
                Logger.getLogger(ProcPurchases.class.getName()).log(Level.SEVERE, null, ex);
                purchases.clear();
                return purchases;
            }
        }
        return purchases;
    }
}
