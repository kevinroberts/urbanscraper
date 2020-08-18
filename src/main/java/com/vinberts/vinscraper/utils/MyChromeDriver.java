package com.vinberts.vinscraper.utils;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.chrome.ChromeOptions;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
@Slf4j
public class MyChromeDriver {

    private static MyChromeDriver instance = new MyChromeDriver();
    private ChromeDriverEx driver;

    private MyChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200","--ignore-certificate-errors");
        options.addArguments("disable-infobars");
        options.setExperimentalOption("useAutomationExtension", false);
        try {
            driver = new ChromeDriverEx(options);
        } catch (NoSuchMethodException e) {
            log.error("NoSuchMethodException ", e);
        } catch (IllegalAccessException e) {
            log.error("IllegalAccessException ", e);
        } catch (InvocationTargetException e) {
            log.error("InvocationTargetException ", e);
        }
        String[] blocked = {"*pubmatic.com*", "*facebook*", "*adroll.com*", "*doubleclick.net*", "*adform.net*", "*rubiconproject.com*", "*quantserve.com*", "*google-analytics*", "*moatads.com*", "*serverbid.com*"};
        driver.executeCdpCommand("Network.setUserAgentOverride", ImmutableMap.of ( "userAgent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36"));
        driver.executeCdpCommand("Network.setBlockedURLs", ImmutableMap.of("urls", blocked));
    }

    public static MyChromeDriver getInstance() {
        return instance;
    }

    public ChromeDriverEx getDriver() {
        return driver;
    }
}
