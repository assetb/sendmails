/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.sendmails;

import com.altaik.bo.Lot;
import com.altaik.bo.Purchase;
import com.altaik.bo.Purchases;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Aset
 */
public class HtmlMessage implements AutoCloseable{
    
    private final Purchases purchases;
    public int lastProcessedPurchaseId = 0;
    
    protected static final Logger logger = Logger.getLogger(HtmlMessage.class.getName());
    
    
    public HtmlMessage(Purchases purchases){
        this.purchases = purchases;
    }
    
    
    public String GetPreparedHtml() {
        //int nLots = sum(purchases, on(Purchase.class).lots.size());
        int nLots = 0;
        for(Purchase purchase:purchases){
            if(purchase.lots==null){
                purchase.lots = new ArrayList<>();
            }
            nLots += purchase.lots.size();
        }
        
        
        logger.log(Level.INFO, "Count lots in purchases: {0}", nLots);
        
        if(nLots == 0) return "";

        String message = "<html>";
        message = message + "<head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"><style>td {padding:1px;}</style></head><body>";
        message = message + "<center><h3>" + nLots + " новых лотов по автопоиску</h3><center>";

        for (Purchase purchase : purchases) {
            message = message + GetPurchaseHtml(purchase);

            if (purchase.addition.id > lastProcessedPurchaseId) {
                lastProcessedPurchaseId = purchase.addition.id;
            }
        }

        message = message + "</body></html>";

        return message;
    }

 
    private String GetItemHtml(String name, String value){
        return ((value != null && !value.isEmpty()) ? "<tr><td>" + name + ((name == null || name.isEmpty()) ? "" : ":") + "</td><td>" + value + "</td></tr>" : "");
    }

 
    private String GetItemHtml(String name, String value, String link){
        value = ((link != null && !link.isEmpty()) ? "<a href=\'" + link + "\'>" + value + "</a>" : value);
        return GetItemHtml(name, value);
    }
    
    
    private String GetPurchaseHtml(Purchase purchase) {
        String mesText = "<br/><br/><table cellpadding=\"0\" cellspacing=\"0\" width=\"800\" align=\"left\" style=\"border-collapse:collapse; border:#cccccc 1px solid; background-color: whitesmoke; margin:10px\"><th width=100px></th><th></th><tr><td><br/></td><td><br/></td></tr><tr><td><br/></td><td><br/></td></tr>";

        mesText += GetItemHtml("<b>ОБЪЯВЛЕНИЕ</b>", purchase.number);
        mesText += GetItemHtml("", purchase.ruName);
        if(purchase.addition != null)
            mesText += GetItemHtml("Источник", purchase.addition.sitename,purchase.link);
        mesText += GetItemHtml("Организатор", purchase.customer);
        mesText += GetItemHtml("Метод", purchase.addition.methodname);
        if(purchase.addition != null)
            mesText += GetItemHtml("Дата начала", purchase.startDay);
        mesText += GetItemHtml("Дата окончания", purchase.endDay);

        for (Lot lot : purchase.lots)
            mesText += GetLotSection(lot);

        mesText += "</table>";

        return mesText;
    }


    private String GetLotSection(Lot lot) {
        String result = "";

        result += GetItemHtml("<br/></td><td><br/></td></tr><tr><td><b>ЛОТ:</b>", lot.ruName);
        result += GetItemHtml("Описание", lot.ruDescription);
        result += GetItemHtml("Ссылка", lot.link);
        result += GetItemHtml("Место поставки", lot.deliveryPlace);
        result += GetItemHtml("Количество", lot.quantity);
        result += GetItemHtml("Цена", lot.price);
        result += GetItemHtml("Сумма", lot.sum);

        return result;
    }

    @Override
    public void close() {
        
    }

}
