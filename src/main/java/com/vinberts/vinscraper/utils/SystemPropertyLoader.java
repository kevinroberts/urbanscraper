package com.vinberts.vinscraper.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

/**
 *
 */
@Slf4j
public class SystemPropertyLoader {
    private static final String CHROME_DRIVER_PATH = "webdriver.chrome.driver";
    public static final String DATABASE_URL = "database_url";
    public static final String DATABASE_USERNAME = "database_username";
    public static final String DATABASE_PASS = "database_password";
    private static Properties defaultProps = new Properties();

    public static void checkAndLoadRequiredPropValues() {
        InputStream inputStream;
        String defaultPath = "app.properties";
        try {
            inputStream = SystemPropertyLoader.class.getClassLoader().getResourceAsStream(defaultPath);
            if (inputStream != null) {
                defaultProps.load(inputStream);
                log.info("loaded " + defaultProps.size() + " properties");
            } else {
                throw new FileNotFoundException("default property file '" + defaultPath + "' not found in the classpath");
            }
        } catch (Exception e) {
            log.error("Property loader exception: ", e);
        }
        checkForRequiredEnvVariable(CHROME_DRIVER_PATH);
    }

    public static Properties getSystemProps() {
        if (defaultProps.size() <= 0) {
            checkAndLoadRequiredPropValues();
        }
        return defaultProps;
    }

    private static void checkForRequiredEnvVariable(String key) {
        if (!System.getenv().containsKey(key)) {
            log.warn("System env property missing: " + key);
            System.setProperty(CHROME_DRIVER_PATH, getSystemProps().getProperty(CHROME_DRIVER_PATH));
        }
    }

}
