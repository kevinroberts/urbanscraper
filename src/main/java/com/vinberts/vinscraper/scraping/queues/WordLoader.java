package com.vinberts.vinscraper.scraping.queues;

import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.Definition;
import com.vinberts.vinscraper.database.models.WordQueue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.nodes.Element;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.vinberts.vinscraper.scraping.ScrapingConstants.URBAN_DATE_FORMAT;
import static com.vinberts.vinscraper.scraping.ScrapingConstants.URBAN_DIC_URL;
import static com.vinberts.vinscraper.scraping.curl.CurlUtils.getJsonViaCurl;

/**
 * WordLoader
 */
@Slf4j
public abstract class WordLoader {

    protected boolean attemptSaveNewDefinition(JSONObject definitions) {
        JSONArray defList = definitions.getJSONArray("list");
        boolean success = false;
        for (int i = 0; i < defList.length(); i++) {
            JSONObject defObject = defList.getJSONObject(i);
            String definitionId = defObject.getBigInteger("defid").toString();
            Optional<Definition> definitionCheck = DatabaseHelper.getDefinitionById(definitionId);
            if (definitionCheck.isEmpty()) {
                Definition definition = new Definition();
                definition.setId(definitionId);
                definition.setWord(defObject.getString("word"));
                definition.setMeaning(defObject.getString("definition"));
                definition.setUsername(defObject.getString("author"));
                definition.setExample(defObject.getString("example"));
                definition.setUpVotes(defObject.getLong("thumbs_down"));
                definition.setDownVotes(defObject.getLong("thumbs_up"));
                String dateString = defObject.getString("written_on");
                definition.setDateAdded(
                        convertToLocalDateTimeViaInstant(Date.from(Instant.parse(dateString)))
                );

                if (definition.getMeaning().length() > 23000) {
                    log.warn("Meaning was too long to store into 23k for word: " + definition.getWord());
                    definition.setMeaning(StringUtils.abbreviate(definition.getMeaning(), "", 23000));
                }
                if (definition.getExample().length() > 14000) {
                    log.warn("Example was too long to store into 14k for word: " + definition.getWord());
                    definition.setExample(StringUtils.abbreviate(definition.getExample(), "", 14000));
                }
                if (DatabaseHelper.insertNewDefinition(definition)) {
                    log.info(Thread.currentThread().getName() +
                            ": Stored new definition for word "
                            + definition.getWord() + " defId: " + definitionId);
                    success = true;
                } else {
                    log.warn(Thread.currentThread().getName() +
                            ": Database error storing "
                            + definition.getWord());
                }
            }
        }
        return success;
    }
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
            if (StringUtils.isEmpty(definition.getMeaning())) {
                log.info(Thread.currentThread().getName() +
                        ": definition for word was empty "
                        + definition.getWord());
                return false;
            }

            JSONObject votes = getJsonViaCurl("https://api.urbandictionary.com/v0/uncacheable?ids=" + definitionId);
            definition.setUpVotes(Long.valueOf((Integer) votes.getJSONArray("thumbs").getJSONObject(0).get("up")));
            definition.setDownVotes(Long.valueOf((Integer) votes.getJSONArray("thumbs").getJSONObject(0).get("down")));

            if (definition.getMeaning().length() > 23000) {
                log.warn("Meaning was too long to store into 23k for word: " + definition.getWord());
                definition.setMeaning(StringUtils.abbreviate(definition.getMeaning(), "", 23000));
            }
            if (definition.getExample().length() > 14000) {
                log.warn("Example was too long to store into 14k for word: " + definition.getWord());
                definition.setExample(StringUtils.abbreviate(definition.getExample(), "", 14000));
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
        return true;

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

    private LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

}
