package com.vinberts.vinscraper.scraping.chrome;

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.chrome.ChromeOptions;

import java.lang.reflect.InvocationTargetException;

/**
 *
 */
public interface ChromeDriverInstance {

    default ChromeDriverEx loadDriver () throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200", "--ignore-certificate-errors");
        options.addArguments("disable-infobars");
        options.setExperimentalOption("useAutomationExtension", false);
        ChromeDriverEx driver = new ChromeDriverEx(options);
        String[] blocked = {"*pubmatic.com*", "*facebook*", "*adroll.com*",
                "*doubleclick.net*", "*adform.net*", "*rubiconproject.com*",
                "*quantserve.com*", "*google-analytics*", "*moatads.com*", "*serverbid.com*"};
        driver.executeCdpCommand("Network.setUserAgentOverride", ImmutableMap.of ( "userAgent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36"));
        driver.executeCdpCommand("Network.setBlockedURLs", ImmutableMap.of("urls", blocked));
        return driver;
    }

}
