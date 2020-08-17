package com.vinberts.vinscraper;

import com.google.common.collect.ImmutableMap;
import com.vinberts.vinscraper.utils.ChromeDriverEx;
import com.vinberts.vinscraper.utils.SystemPropertyLoader;
import com.vinberts.vinscraper.utils.UrbanDictionaryUtils;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 *
 */
@Slf4j
public class Main {

    public static void main(String[] args) {
        SystemPropertyLoader.loadPropValues();
        int pagesToVisit = 100;
        int currentPage = 101;
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless", "--disable-gpu", "--window-size=1920,1200","--ignore-certificate-errors");
        options.addArguments("disable-infobars");
        options.setExperimentalOption("useAutomationExtension", false);
        try {
            ChromeDriverEx driver = new ChromeDriverEx(options);
            String[] blocked = {"*pubmatic.com*", "*facebook*", "*adroll.com*", "*doubleclick.net*", "*adform.net*", "*rubiconproject.com*", "*quantserve.com*", "*google-analytics*", "*moatads.com*", "*serverbid.com*"};
            driver.executeCdpCommand("Network.setUserAgentOverride", ImmutableMap.of ( "userAgent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.81 Safari/537.36"));
            driver.executeCdpCommand("Network.setBlockedURLs", ImmutableMap.of("urls", blocked));

            // load first page
            String pageToLoad = String.format("https://www.urbandictionary.com/?page=%d", currentPage);
            driver.get(pageToLoad);
            currentPage++;
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                log.error("InterruptedException thread exception", e);
            }

            for (int i = pagesToVisit; i >= 1; i--) {
                log.info("loading page " + currentPage);
                driver.navigate().to(String.format("https://www.urbandictionary.com/?page=%d", currentPage));
                currentPage++;
                List<WebElement> elements = driver.findElements(By.className("def-panel"));
                log.info("Found defs: " + elements.size());
                for (WebElement element: elements) {
                    UrbanDictionaryUtils.attemptSaveNewDefinition(element);
                }
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    log.error("InterruptedException thread exception", e);
                }
            }

            //File screenshot = driver.getFullScreenshotAs(OutputType.FILE);
            //FileUtils.copyFile(screenshot, new File("screenshot.png"));
            log.info("finished");
            //driver.close();
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
        } finally {
            System.exit(0);
        }
    }
}
