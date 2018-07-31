/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.altaik.parser.sendmails.ets;

import com.altaik.bo.Lot;
import com.altaik.parser.sendmails.ets.bo.Envelope;
import com.altaik.parser.sendmails.ets.bo.Envelopes;
import com.altaik.parser.sendmails.ets.bo.ProcPurchase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author admin
 */
public class SendEmailsEtsUtils {

    protected Logger logger = Logger.getLogger(SendEmailsEtsUtils.class.getName());

    private Element GetItemTable(String title, String value) {
        Element element;
        element = new Element(Tag.valueOf("tr"), "");
        element.append("<td>" + (title != null ? title : "") + "</td><td>" + (value != null ? value : "") + "</td>");
        return element;
    }

    private Element GetPurchaseTable(ProcPurchase purchase) {
        Document document = Jsoup.parseBodyFragment("<table cellpadding=\"0\" cellspacing=\"0\"></table>");

        Element table = document.select("table").get(0);
        table.appendChild(GetItemTable("<b>ОБЪЯВЛЕНИЕ</b>", "<a href=\"http://com.altaik.db.altatender.kz/lotdescription.jsp?neg=" + purchase.number + "\">" + purchase.number + "</a>"));
        table.appendChild(GetItemTable("Наименование закупки", purchase.ruName));
        table.appendChild(GetItemTable("Организатор", purchase.customer));
        if (purchase.addition != null) {
            table.appendChild(GetItemTable("Источник", "<a href=\"" + purchase.link + "\">" + purchase.addition.sitename + "</a>"));
            table.appendChild(GetItemTable("Метод", purchase.addition.methodname));
        }
        table.appendChild(GetItemTable("Дата начала", purchase.startDay));
        table.appendChild(GetItemTable("Дата окончания", purchase.endDay));

        for (Lot lot : purchase.lots) {
            table.append("<tr class=\"separation\"></tr>");
            table.appendChild(GetItemTable("<b>ЛОТ</b>", lot.ruName));
            table.appendChild(GetItemTable("Описание", lot.ruDescription));
            table.appendChild(GetItemTable("Ссылка", lot.link));
            table.appendChild(GetItemTable("Место поставки", lot.deliveryPlace));
            table.appendChild(GetItemTable("Количество", lot.deliveryPlace));
            table.appendChild(GetItemTable("Цена", lot.price));
            table.appendChild(GetItemTable("Сумма", lot.sum));
        }
        return table;
    }

    private Document GetTemplateMessage() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("templates/MessageTemplate.html");
            return Jsoup.parse(is, "utf-8", "");
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public Document GenerateMessage(Envelope envelope) {
        Document document = GetTemplateMessage();
        if (document == null) {
            return null;
        }
        List<ProcPurchase> procPurchases = envelope.getPurchases();
        document.select("#count").html(String.valueOf(procPurchases.size()));

        Element purchaseBody = document.select("#purchasesBody").get(0);
        for (ProcPurchase procPurchase : procPurchases) {
            purchaseBody.appendChild(GetPurchaseTable(procPurchase));
        }
        return document;
    }

    private Element GenerateItemOrder(Envelope envelope) {
        Element tr = new Element(Tag.valueOf("tr"), "");
        Element tdCompanyName = tr.appendElement("td");
        Element tdEmail = tr.appendElement("td");
        Element tdLots = tr.appendElement("td");
        tdCompanyName.text(envelope.getDescription());
        tdEmail.text(envelope.getEmail());
        String purchases = "";
        for (ProcPurchase p : envelope.getPurchases()) {
            purchases += (!purchases.isEmpty() ? ", " : "") + "<a href=\"http://com.altaik.db.altatender.kz/lotdescription.jsp?neg=" + p.number + "\">" + p.number + "</a>";
            
        }
        tdLots.html(purchases);
        return tr;
    }

    public Document GenerateOrder(Envelopes envelopes) {
        Document doc = null;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("templates/OperationReport.html");
            doc = Jsoup.parse(is, "utf-8", "");
            if (doc == null) {
                throw new IOException("Error parsing HTML page. Page is null.");
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error load reporte template", ex);
            return null;
        }
        Date currentDate = new Date();
        DateFormat df = new SimpleDateFormat("HH:mm dd.MM.yyyy");
        DateFormat dfForTitle = new SimpleDateFormat("yyyy.MM.dd");
        doc.title(doc.title() + dfForTitle.format(currentDate));
        doc.select("#createDate").html(df.format(currentDate));
        doc.select("#countMessage").html(String.valueOf(envelopes.size()));

        Elements table = doc.select("#envelopes");

        for (Envelope e : envelopes) {
            if (e == null) {
                continue;
            }
            table.append(GenerateItemOrder(e).html());
        }

        doc.updateMetaCharsetElement();
        return doc;
    }
}
