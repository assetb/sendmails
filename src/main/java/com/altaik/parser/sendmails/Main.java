package com.altaik.parser.sendmails;

import com.altaik.bp.utils.EmailLoggerHandler;
import com.altaik.parser.sendmails.processes.AltatenderProcess;
import com.altaik.parser.sendmails.processes.EtsProcess;
import com.altaik.parser.sendmails.processes.SendMailProcesses;
import com.altaik.utils.Options;
import com.altaik.utils.OptionsSet;
import com.altaik.utils.Settings;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Created by admin on 25.09.2017.
 */
public final class Main {
    private static final String SENDMAILS_TYPE = "sendmails.type";
    private static final String TYPE_ALTATENDER = "altatender";
    private static final String TYPE_ETS = "ets";
    private static final String defaultApplicationPropertiesFile = "settings/application.properties";
    private static SendMailProcesses app = null;
    private static Logger logger = Logger.getLogger(Main.class.getName());
    private static Properties properties = new Properties();

    private static boolean loadParameters(String fileName) {
        try (InputStream settingsStream = new FileInputStream(fileName)) {
            properties.load(settingsStream);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error open settings file {0}", fileName);

            return false;
        }

        return true;
    }

    private static void loadDefaultParameters() {
        try (InputStream resource = Main.class.getClassLoader().getResourceAsStream(defaultApplicationPropertiesFile)) {
            properties.load(resource);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error load default properties");
        }
    }

    private static void addEmailHandlerInLogger() {
        EmailLoggerHandler handler = new EmailLoggerHandler();
        handler.setName("Система рассылки");
        handler.setLevel(Level.SEVERE);
        logger.addHandler(handler);

    }

    public static void main(String[] args) {
        loadDefaultParameters();
        addEmailHandlerInLogger();
        Options options = new Options(args);
        OptionsSet setting = options.addSet("(s|setting)");
        OptionsSet type = options.addSet("(t|type)");
        OptionsSet testMode = options.addSet("testMode");
        OptionsSet testEmail = options.addSet("testEmail");
        OptionsSet orderEmail = options.addSet("orderEmail");
        OptionsSet xparams = options.addSet("X[^\\s]+");

        if (setting.isSet())
            if (!loadParameters(setting.getData()))
                return;

        if (orderEmail.isSet())
            properties.setProperty(SendMailProcesses.ORDER_EMAIL, orderEmail.getData());

        if (testEmail.isSet()) {
            properties.setProperty(SendMailProcesses.TEST_EMAIL, testEmail.getData());
        }

        if (testMode.isSet()) {
            properties.setProperty(SendMailProcesses.TEST_MODE, testMode.getData());
        }

        if (xparams.isSet()) {
            Map<String, String> all = xparams.getFindAll();
            all.forEach((key, value) -> properties.setProperty(key.replaceAll("X(.*)", "$1"), value));
        }

        if (type.isSet()) {
            properties.setProperty(SENDMAILS_TYPE, type.getData());
        }

        Settings.setLogManagerSetting(LogManager.getLogManager(), properties);
        String property = properties.getProperty(SENDMAILS_TYPE);

        if (property == null || property.isEmpty()) {
            logger.log(Level.SEVERE, "No type for launching the application");
            return;
        }

        SendMailProcesses process = null;

        try {
            switch (property) {
                case TYPE_ALTATENDER:
                    process = new AltatenderProcess(properties);
                    break;
                case TYPE_ETS:
                    process = new EtsProcess(properties);
                    break;
                default:
                    process = null;
            }

            if (process != null) {
                process.run();
            } else {
                logger.log(Level.SEVERE, "Invalid {0} field", SENDMAILS_TYPE);
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fatal error!!!", e);
        } finally {
            if (process != null)
                process.close();
        }
    }
}

