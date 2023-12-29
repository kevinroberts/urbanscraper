package com.vinberts.vinscraper.scraping.queues;

import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverEx;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.List;

import static com.vinberts.vinscraper.scraping.ScrapingConstants.MAIN_UL_SELECTOR;

/**
 *  WordQueueProcessing
 *  process a list of words / urls in
 *  a self contained java thread using its own
 *  chrome headless driver
 */
@Slf4j
public class WordQueueProcessingWebDriver extends WordLoader implements Runnable {

    private ChromeDriverEx driver;
    private List<WordQueue> wordQueueList;

    public WordQueueProcessingWebDriver(final ChromeDriverEx driver, final List<WordQueue> wordQueueList) {
        this.driver = driver;
        this.wordQueueList = wordQueueList;
    }

    @Override
    public void run() {
        log.info("processing in thread " +
                Thread.currentThread().getName() +
                " for word queue of size " + wordQueueList.size());
        for (WordQueue queue: wordQueueList) {
            try {
                String link = getFullUrlFromLink(queue.getUrl());
                driver.navigate().to(link);
                List<WebElement> defPanels = driver.findElements(By.className(MAIN_UL_SELECTOR));
                attemptSaveNewDefinition(defPanels.get(0));
                DatabaseHelper.updateWordQueueProcess(queue, true);
            } catch (WebDriverException ue) {
                log.error("Exception reached trying to load word in queue " + queue.getWord(), ue);
                DatabaseHelper.updateWordQueueProcess(queue, false);
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e2) {
                    log.error("InterruptedException thread exception", e2);
                }
            }
            try {
                Thread.sleep(RandomUtils.nextInt(300,500));
            } catch (InterruptedException e) {
                log.error("InterruptedException thread exception", e);
            }
        }
        log.info("Finished processing list in thread " + Thread.currentThread().getName());
        driver.quit();
    }

}
