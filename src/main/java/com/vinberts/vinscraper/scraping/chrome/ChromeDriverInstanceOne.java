package com.vinberts.vinscraper.scraping.chrome;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
@Slf4j
public class ChromeDriverInstanceOne implements ChromeDriverInstance {

    private static ChromeDriverInstanceOne instance = new ChromeDriverInstanceOne();
    private ChromeDriverEx driver;

    private ChromeDriverInstanceOne() {
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

    public static ChromeDriverInstanceOne getInstance() {
        return instance;
    }

    public ChromeDriverEx getDriver() {
        return driver;
    }
}
