package com.vinberts.vinscraper.scraping.queues;

import com.vinberts.vinscraper.scraping.UrbanDictionaryUtils;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverEx;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * AlphaWordLoader
 */
@Slf4j
public class AlphaWordLoader implements Runnable {
    private final ChromeDriverEx driver;
    private final String letterToLoad;
    private final int startPage;

    public AlphaWordLoader(final ChromeDriverEx driver, final String letterToLoad) {
        this.driver = driver;
        this.letterToLoad = letterToLoad;
        this.startPage = 1;
    }

    public AlphaWordLoader(final ChromeDriverEx driver, final String letterToLoad, final int startPage) {
        this.driver = driver;
        this.letterToLoad = letterToLoad;
        this.startPage = startPage;
    }

    @Override
    public void run() {
        int currentPage = startPage;
        final int pagesToVisit = 800;
        for (int i = pagesToVisit; i >= 1; i--) {
            String nextPageToLoad = String.format("https://www.urbandictionary.com/browse.php?character=%s&page=%d",
                    letterToLoad, currentPage);
            driver.navigate().to(nextPageToLoad);
            WebElement listWords = driver.findElement(By.className("no-bullet"));
            List<WebElement> wordAnchors = listWords.findElements(By.tagName("a"));
            log.info(String.format("Thread %s: Found %d words on page %d for letter %s",
                            Thread.currentThread().getName(),
                            wordAnchors.size(),
                            currentPage,
                            letterToLoad));
            currentPage++;
            UrbanDictionaryUtils.loadToDBQueue(wordAnchors);
            try {
                Thread.sleep(RandomUtils.nextInt(300,500));
            } catch (InterruptedException e) {
                log.error("InterruptedException thread exception", e);
            }
        }
        log.info(String.format("Thread %s Finished processing alpha load for letter %s",
                Thread.currentThread().getName(),
                letterToLoad));
        driver.quit();
    }
}
