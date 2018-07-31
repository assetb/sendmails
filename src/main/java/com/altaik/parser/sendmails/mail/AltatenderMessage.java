package com.altaik.parser.sendmails.mail;

import com.altaik.bo.view.CurrentDayDeliveryView;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by admin on 27.09.2017.
 */
public class AltatenderMessage extends Message<List<CurrentDayDeliveryView>> {
    Logger logger = Logger.getLogger(AltatenderMessage.class.getName());

    public AltatenderMessage(String email, String subject, String pathTemplateFile, List<CurrentDayDeliveryView> currentDayDeliveryList) {
        super(email, subject, pathTemplateFile, currentDayDeliveryList);
    }

    private int getCountPurhcase() {
        return t.size();
    }

    @Override
    protected Map<String, Object> getMap() {
        Map<String, Object> map = super.getMap();
        map.put("count", getCountPurhcase());
        map.put("helper", new Utils());
        return map;
    }
}

class Utils {
    public String formating(String source, String pattern) {
        String regex = String.format("(?i)([\\w\\d]{0,}(%s)[\\w\\d]{0,})", String.join("|", pattern.replaceAll("(^%|%$)", "").split("%")));
        return source.replaceAll(regex, "<b>$1</b>");
    }
}