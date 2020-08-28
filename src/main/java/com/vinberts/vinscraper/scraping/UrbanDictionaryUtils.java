package com.vinberts.vinscraper.scraping;

import com.google.common.primitives.Longs;
import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.Definition;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceOne;
import lombok.extern.slf4j.Slf4j;
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
 * UrbanDictionaryUtils
 */
@Slf4j
public class UrbanDictionaryUtils {

    public static void attemptSaveNewDefinition(WebElement element) {
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

    public static void loadDefinitionsByPage(int pagesToVisit, int currentPage) {
        try {
            ChromeDriverInstanceOne chromeDriver = ChromeDriverInstanceOne.getInstance();
            // load first page
            String pageToLoad = String.format("https://www.urbandictionary.com/?page=%d", currentPage);
            chromeDriver.getDriver().get(pageToLoad);
            loadElementsIntoDB(chromeDriver.getDriver().findElements(By.className("def-panel")));
            currentPage++;
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                log.error("InterruptedException thread exception", e);
            }
            for (int i = pagesToVisit; i >= 1; i--) {
                log.info("loading page " + currentPage);
                chromeDriver.getDriver().navigate().to(String.format("https://www.urbandictionary.com/?page=%d", currentPage));
                currentPage++;
                loadElementsIntoDB(chromeDriver.getDriver().findElements(By.className("def-panel")));
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
            chromeDriver.getDriver().quit();
        } catch (WebDriverException e) {
            System.err.println("finished with error");
            System.err.println(e);
        } finally {
            System.exit(0);
        }
    }

    public static void loadToDBQueue(final List<WebElement> anchors) {
        for (WebElement element: anchors) {
            String word = element.getText();
            Optional<WordQueue> existingWord = DatabaseHelper.getWordQueueByWord(word);
            if (existingWord.isEmpty()) {
                WordQueue queue = new WordQueue();
                queue.setWord(word);
                queue.setUrl(element.getAttribute("href"));
                queue.setDateAdded(LocalDateTime.now());
                queue.setProcessed(false);
                DatabaseHelper.insertNewWordQueue(queue);
                log.info("Word loaded to queue: " + word);
            } else {
                log.info("Word already loaded: " + existingWord.get().getWord());
            }
        }
    }

    public static void loadElementsIntoDB(final List<WebElement> elements) {
        log.info("Found defs: " + elements.size());
        for (WebElement element: elements) {
            UrbanDictionaryUtils.attemptSaveNewDefinition(element);
        }
    }

    public static String getFullUrlFromLink(String link) {
        if (StringUtils.startsWithIgnoreCase(link, "/")) {
            return "https://www.urbandictionary.com" + link;
        } else {
            return link;
        }
    }
}
