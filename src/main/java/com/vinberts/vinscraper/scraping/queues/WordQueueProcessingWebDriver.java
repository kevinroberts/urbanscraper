package com.vinberts.vinscraper.scraping.queues;

import com.google.common.primitives.Longs;
import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.Definition;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverEx;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.vinberts.vinscraper.scraping.ScrapingConstants.URBAN_DATE_FORMAT;

/**
 *  WordQueueProcessing
 *  process a list of words / urls in
 *  a self contained java thread using its own
 *  chrome headless driver
 */
@Slf4j
public class WordQueueProcessingWebDriver implements Runnable {

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
                String link = queue.getUrl();
                driver.navigate().to(link);
                List<WebElement> defPanels = driver.findElements(By.className("def-panel"));
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

    private void attemptSaveNewDefinition(WebElement element) {
        final String definitionId = element.getAttribute("data-defid");
        // check if this definition has been saved already:
        Optional<Definition> definitionCheck = DatabaseHelper.getDefinitionById(definitionId);
        if (definitionCheck.isEmpty()) {
            Definition definition = new Definition();
            definition.setId(definitionId);

            // get word from WebElement
            WebElement header = element.findElement(By.className("def-header"));
            WebElement wordEle = header.findElement(By.className("word"));
            definition.setWord(wordEle.getText());
            // get meaning from WebElement
            WebElement meaningEle = element.findElement(By.className("meaning"));
            definition.setMeaning(meaningEle.getText());
            // get example from WebElement
            WebElement exampleEle = element.findElement(By.className("example"));
            definition.setExample(exampleEle.getText());
            // get contributor
            WebElement contribEle = element.findElement(By.className("contributor"));
            WebElement contribLink = contribEle.findElement(By.tagName("a"));
            definition.setUsername(contribLink.getText());
            // extract date added | "by For the greater good August 15, 2020"
            String dateString = StringUtils.substringAfter(contribEle.getText(),
                    definition.getUsername() + " ");
            log.debug("date added text: " + dateString);
            SimpleDateFormat formatter = new SimpleDateFormat(URBAN_DATE_FORMAT);
            try {
                Date dateAdded = formatter.parse(dateString);
                definition.setDateAdded(dateAdded.toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            } catch (ParseException e) {
                log.warn("Could not parse date string: " + dateString);
                definition.setDateAdded(LocalDateTime.now());
            }

            // get up votes / down votes
            WebElement footer = element.findElement(By.className("def-footer"));
            WebElement upEle = footer.findElement(By.className("up"));
            WebElement upCountEle = upEle.findElement(By.className("count"));
            Long upCount = 0L;
            try {
                upCount = Longs.tryParse(upCountEle.getText());
                if (Objects.isNull(upCount)) {
                    upCount = 0L;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            definition.setUpVotes(upCount);

            WebElement downEle = footer.findElement(By.className("down"));
            WebElement downCountEle = downEle.findElement(By.className("count"));
            Long downCount = 0L;
            try {
                downCount = Longs.tryParse(downCountEle.getText());
                if (Objects.isNull(upCount)) {
                    downCount = 0L;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            definition.setDownVotes(downCount);

            if (DatabaseHelper.insertNewDefinition(definition)) {
                log.info("Stored new definition for word " + definition.getWord());
            }
        } else {
            log.info("Definition already scraped for word: " + definitionCheck.get().getWord());
        }
    }
}
