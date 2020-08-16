package com.vinberts.vinscraper;

import com.google.common.collect.ImmutableMap;
import com.vinberts.vinscraper.utils.ChromeDriverEx;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 *
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            Files.delete(Path.of("screenshot.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String chromeDriverPath = "/usr/local/bin/chromedriver";
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200","--ignore-certificate-errors");
        options.addArguments("disable-infobars");
        options.setExperimentalOption("useAutomationExtension", false);
        try {
            ChromeDriverEx driver = new ChromeDriverEx(options);
            String[] blocked = {"*pubmatic.com*", "*facebook*", "*adroll.com*"};
            driver.executeCdpCommand("Network.setUserAgentOverride", ImmutableMap.of ( "userAgent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36"));
            driver.executeCdpCommand("Network.setBlockedURLs", ImmutableMap.of("urls", blocked));
            driver.get("https://www.urbandictionary.com/");
            File screenshot = driver.getFullScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshot, new File("screenshot.png"));
            driver.quit();
        } catch (WebDriverException e) {
            System.err.println("finished with error");
            System.err.println(e);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.info("finished");
    }
}
