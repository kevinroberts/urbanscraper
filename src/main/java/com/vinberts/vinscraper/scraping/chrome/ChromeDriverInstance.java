package com.vinberts.vinscraper.scraping.chrome;

import com.google.common.collect.ImmutableMap;
import org.openqa.selenium.chrome.ChromeOptions;

import java.lang.reflect.InvocationTargetException;

import static com.vinberts.vinscraper.scraping.ScrapingConstants.USER_AGENT_WINDOWS;

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
        driver.executeCdpCommand("Network.setUserAgentOverride", ImmutableMap.of ( "userAgent", USER_AGENT_WINDOWS));
        driver.executeCdpCommand("Network.setBlockedURLs", ImmutableMap.of("urls", blocked));
        return driver;
    }

}
