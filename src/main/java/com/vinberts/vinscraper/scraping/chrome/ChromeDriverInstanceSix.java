package com.vinberts.vinscraper.scraping.chrome;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
@Slf4j
public class ChromeDriverInstanceSix implements ChromeDriverInstance {

    private static ChromeDriverInstanceSix instance = new ChromeDriverInstanceSix();
    private ChromeDriverEx driver;

    private ChromeDriverInstanceSix() {
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

    public static ChromeDriverInstanceSix getInstance() {
        return instance;
    }

    public ChromeDriverEx getDriver() {
        return driver;
    }
}
