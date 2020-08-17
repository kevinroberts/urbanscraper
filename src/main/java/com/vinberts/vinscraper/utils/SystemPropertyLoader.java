package com.vinberts.vinscraper.utils;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SystemPropertyLoader {
    private static final String CHROME_DRIVER_PATH = "webdriver.chrome.driver";

    public static void loadPropValues() {
        if (!System.getenv().containsKey(CHROME_DRIVER_PATH)) {
            log.error("Required system property missing: " + CHROME_DRIVER_PATH);
            System.exit(0);
        }
    }

    public static String getChromeDriverPath() {
        return System.getenv().get(CHROME_DRIVER_PATH);
    }
}
