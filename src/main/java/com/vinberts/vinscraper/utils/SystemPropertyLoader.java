package com.vinberts.vinscraper.utils;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SystemPropertyLoader {
    private static final String CHROME_DRIVER_PATH = "webdriver.chrome.driver";
    private static final String DATABASE_URL = "database_url";

    public static void checkRequiredPropValues() {
        if (!System.getenv().containsKey(CHROME_DRIVER_PATH)) {
            log.error("Required system property missing: " + CHROME_DRIVER_PATH);
            System.exit(0);
        }
        if (!System.getenv().containsKey(DATABASE_URL)) {
            log.error("Required system property missing: " + DATABASE_URL);
            System.exit(0);
        }
    }

}
