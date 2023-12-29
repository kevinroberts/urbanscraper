package com.vinberts.vinscraper.scraping.queues;

import com.google.common.primitives.Longs;
import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.Definition;
import com.vinberts.vinscraper.database.models.WordQueue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
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
import static com.vinberts.vinscraper.scraping.ScrapingConstants.URBAN_DIC_URL;

/**
 * WordLoader
 */
@Slf4j
public abstract class WordLoader {
    /**
     * attemptSaveNewDefinition
     *
     * @param element (Element or WebElement)
     * @return boolean
     */
    protected boolean attemptSaveNewDefinition(Object element) {
        String definitionId = StringUtils.EMPTY;
        if (element instanceof WebElement) {
            WebElement ele = (WebElement) element;
            definitionId = ele.getAttribute("data-defid");
        } else if (element instanceof Element) {
            Element ele = (Element) element;
            definitionId = ele.attr("data-defid");
        }

        Optional<Definition> definitionCheck = DatabaseHelper.getDefinitionById(definitionId);
        if (definitionCheck.isEmpty()) {
            Definition definition = new Definition();
            definition.setId(definitionId);
            String dateString = StringUtils.EMPTY;
            String upCountText = StringUtils.EMPTY;
            String downCountText = StringUtils.EMPTY;
            Long upCount = 0L;
            Long downCount = 0L;

            // if the element was retrieved via web driver
            if (element instanceof WebElement) {
                WebElement ele = (WebElement) element;
                // get word from WebElement
                WebElement wordEle = ele.findElement(By.className("word"));
                definition.setWord(wordEle.getText());
                // get meaning from WebElement
                WebElement meaningEle = ele.findElement(By.className("meaning"));
                definition.setMeaning(meaningEle.getText());
                // get example from WebElement
                WebElement exampleEle = ele.findElement(By.className("example"));
                definition.setExample(exampleEle.getText());
                // get contributor
                WebElement contribEle = ele.findElement(By.className("contributor"));
                WebElement contribLink = contribEle.findElement(By.tagName("a"));
                definition.setUsername(contribLink.getText());
                dateString = StringUtils.substringAfter(contribEle.getText(),
                        definition.getUsername() + " ");
                // get up votes / down votes
                WebElement footer = ele.findElement(By.cssSelector("div.items-center"));
                WebElement upEle = footer.findElement(By.tagName("button"));
                WebElement upCountEle = upEle.findElement(By.tagName("span"));
                upCountText = upCountEle.getText();
                WebElement downEle = footer.findElement(By.tagName("button"));
                WebElement downCountEle = downEle.findElement(By.tagName("span"));
                downCountText = downCountEle.getText();
            } else if (element instanceof Element) {
                // else the element was retrieved via curl:
                Element ele = (Element) element;
                // get word from element
                Element wordEle = ele.selectFirst(".word");
                definition.setWord(wordEle.text());
                // get meaning from element
                Element meaningEle = ele.selectFirst(".meaning");
                definition.setMeaning(meaningEle.text());
                // get example from element
                Element exampleEle = ele.selectFirst(".example");
                definition.setExample(exampleEle.text());
                // get contributor
                Element contribEle = ele.selectFirst(".contributor");
                Element contribLink = contribEle.selectFirst("a");
                definition.setUsername(contribLink.text());
                // extract date added | "by For the greater good August 15, 2020"
                dateString = StringUtils.substringAfter(contribEle.text(),
                        definition.getUsername() + " ");
                // get up votes / down votes
                Element footer = ele.selectFirst("div.items-center");
                Element upEle = footer.selectFirst("button");
                Element upCountEle = upEle.selectFirst("span");
                upCountText = upCountEle.text();
                Element downEle = footer.select("button").get(1);
                Element downCountEle = downEle.selectFirst("span");
                downCountText = downCountEle.text();
            }

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

            // Parse Up count from String
            try {
                upCount = Longs.tryParse(upCountText);
                if (Objects.isNull(upCount)) {
                    upCount = 0L;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            definition.setUpVotes(upCount);

            try {
                downCount = Longs.tryParse(downCountText);
                if (Objects.isNull(upCount)) {
                    downCount = 0L;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            definition.setDownVotes(downCount);

            if (definition.getMeaning().length() > 23000) {
                definition.setMeaning(StringUtils.abbreviate(definition.getMeaning(), "", 23000));
            }
            if (DatabaseHelper.insertNewDefinition(definition)) {
                log.info(Thread.currentThread().getName() +
                        ": Stored new definition for word "
                        + definition.getWord());
                return true;
            }

        } else {
            log.info(Thread.currentThread().getName() +
                    ": Definition already scraped for word: "
                    + definitionCheck.get().getWord());
        }
        return false;

    }

    protected void loadToDBQueue(final List<?> anchors) {
        for (Object element: anchors) {
            String word = StringUtils.EMPTY;
            if (element instanceof WebElement) {
                WebElement ele = (WebElement) element;
                word = ele.getText();
            } else if (element instanceof Element) {
                Element ele = (Element) element;
                word = ele.text();
            }
            Optional<WordQueue> existingWord = DatabaseHelper.getWordQueueByWord(word);
            if (existingWord.isEmpty()) {
                WordQueue queue = new WordQueue();
                queue.setWord(word);
                if (element instanceof WebElement) {
                    WebElement ele = (WebElement) element;
                    queue.setUrl(ele.getAttribute("href"));
                } else if (element instanceof Element) {
                    Element ele = (Element) element;
                    queue.setUrl(ele.attr("href"));
                }
                queue.setDateAdded(LocalDateTime.now());
                queue.setProcessed(false);
                queue.setBeingProcessed(false);
                queue.setHasError(false);
                DatabaseHelper.insertNewWordQueue(queue);
                log.info("Word loaded to queue: " + word);
            } else {
                log.info("Word already loaded: " + existingWord.get().getWord());
            }
        }
    }

    protected String getFullUrlFromLink(String link) {
        if (StringUtils.startsWithIgnoreCase(link, "/")) {
            return URBAN_DIC_URL + link;
        } else {
            return link;
        }
    }
}
