package com.altaik.parser.sendmails.processes;

import com.altaik.bp.BaseProcesses;

import java.util.Properties;

/**
 * Created by admin on 25.09.2017.
 */
public abstract class SendMailProcesses extends BaseProcesses {
    public static final String TEST_MODE = "process.test.mode";
    public static final String TEST_EMAIL = "process.test.email";
    public static final String TEST_EMAIL_CUSTOMER = "process.test.email.customer";
    public static final String ORDER_EMAIL = "process.order.email";

    private String testEmail;
    private String testEmailCustomer;
    private String emailForOrder;
    private boolean testMode;

    SendMailProcesses(Properties properties) {
        super(properties);
        if (properties != null) {
            testMode = "true".equals(properties.getProperty(TEST_MODE));
            testEmail = properties.getProperty(TEST_EMAIL);
            testEmailCustomer = properties.getProperty(TEST_EMAIL_CUSTOMER);
            emailForOrder = properties.getProperty(ORDER_EMAIL);
        }
    }

    /**
     * @return
     */
    String getTestEmail() {
        return testEmail;
    }

    String getTestEmailCustomer() {
        return testEmailCustomer;
    }


    /**
     * Получить электронный адрес на который необходимо отправлять отчет
     *
     * @return Электронный адрес получателя отчетов
     * @see
     */
    String getEmailForOrder() {
        return emailForOrder;
    }

    /**
     * Запушен ли процесс в тестовом режиме(process.test.mode)
     *
     * @return Возвращает true если процесс запущен в тестовом режиме, иначе false
     */
    Boolean isTestMode() {
        return testMode;
    }
}
