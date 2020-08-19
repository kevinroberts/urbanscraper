package com.vinberts.vinscraper.scraping.chrome;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
@Slf4j
public class ChromeDriverInstanceThree implements ChromeDriverInstance {

    private static ChromeDriverInstanceThree instance = new ChromeDriverInstanceThree();
    private ChromeDriverEx driver;

    private ChromeDriverInstanceThree() {
        try {
            driver = loadDriver();
        } catch (NoSuchMethodException e) {
            log.error("NoSuchMethodException", e);
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException ", e);
        } catch (InvocationTargetException e) {
            log.error("InvocationTargetException ", e);
        }
    }

    public static ChromeDriverInstanceThree getInstance() {
        return instance;
    }

    public ChromeDriverEx getDriver() {
        return driver;
    }
}
