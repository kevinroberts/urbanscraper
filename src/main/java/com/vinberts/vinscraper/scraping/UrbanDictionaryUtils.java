package com.vinberts.vinscraper.scraping;

import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.WordQueue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.WebElement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * UrbanDictionaryUtils
 */
@Slf4j
public class UrbanDictionaryUtils {

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
                queue.setHasError(false);
                DatabaseHelper.insertNewWordQueue(queue);
                log.info("Word loaded to queue: " + word);
            } else {
                log.info("Word already loaded: " + existingWord.get().getWord());
            }
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
