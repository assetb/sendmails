package com.altaik.parser.sendmails.mail;

import de.neuland.jade4j.Jade4J;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by admin on 27.09.2017.
 */
public abstract class Message<T> {
    T t;
    String email;
    String subject;
    String pathTemplateFile;

    public Message(String email, String subject, String pathTemplateFile, T t) {
        this.email = email;
        this.subject = subject;
        this.pathTemplateFile = pathTemplateFile;
        this.t = t;
    }

    protected Map<String, Object> getMap(){
        Map<String, Object> map = new HashMap<>();
        map.put("t", t);
        return map;
    }

    public String getEmail() {
        return email;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() throws IOException {
//String templateName = "AltatenderMessage";
//        String inputStream = this.getClass().getClassLoader().getResource("templates/AltatenderMessage.jade").getFile();
        return Jade4J.render(pathTemplateFile, getMap());
    }
}
