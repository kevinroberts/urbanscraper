package com.vinberts.vinscraper.scraping.queues;

import com.google.common.primitives.Longs;
import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.Definition;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.scraping.curl.CurlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.vinberts.vinscraper.scraping.ScrapingConstants.URBAN_DATE_FORMAT;
import static com.vinberts.vinscraper.scraping.UrbanDictionaryUtils.getFullUrlFromLink;

/**
 *
 */
@Slf4j
public class WordQueueProcessingCurl implements Runnable {

    private final List<WordQueue> wordQueueList;

    public WordQueueProcessingCurl(final List<WordQueue> wordQueueList) {
        this.wordQueueList = wordQueueList;
    }

    @Override
    public void run() {
        log.info("processing in thread " +
                Thread.currentThread().getName() +
                " for word queue of size " + wordQueueList.size());
        for (WordQueue queue: wordQueueList) {
            String link = getFullUrlFromLink(queue.getUrl());
            Document document = CurlUtils.getHtmlViaCurl(link);
            Element defDiv = document.select(".def-panel").first();
            if (Objects.isNull(defDiv)) {
                log.warn(queue.getWord() + " could not be loaded and errored out");
                DatabaseHelper.updateWordQueueProcess(queue, false);
            } else {
                DatabaseHelper.updateWordQueueProcess(queue, attemptSaveNewDefinition(defDiv));
            }
            try {
                Thread.sleep(RandomUtils.nextInt(300,400));
            } catch (InterruptedException e) {
                log.error("InterruptedException thread exception", e);
            }
        }
        log.info("Finished processing list in thread " + Thread.currentThread().getName());
    }

    private boolean attemptSaveNewDefinition(Element element) {
            final String definitionId = element.attr("data-defid");

            Optional<Definition> definitionCheck = DatabaseHelper.getDefinitionById(definitionId);
            if (definitionCheck.isEmpty()) {
                Definition definition = new Definition();
                definition.setId(definitionId);
                // get word from element
                Element header = element.selectFirst(".def-header");
                Element wordEle = header.selectFirst(".word");
                definition.setWord(wordEle.text());
                // get meaning from element
                Element meaningEle = element.selectFirst(".meaning");
                definition.setMeaning(meaningEle.text());
                if (definition.getMeaning().length() > 23000) {
                    definition.setMeaning(StringUtils.abbreviate(definition.getMeaning(), "", 23000));
                }
                // get example from element
                Element exampleEle = element.selectFirst(".example");
                definition.setExample(exampleEle.text());
                // get contributor
                Element contribEle = element.selectFirst(".contributor");
                Element contribLink = contribEle.selectFirst("a");
                definition.setUsername(contribLink.text());
                // extract date added | "by For the greater good August 15, 2020"
                String dateString = StringUtils.substringAfter(contribEle.text(),
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
                Element footer = element.selectFirst(".def-footer");
                Element upEle = footer.selectFirst(".up");
                Element upCountEle = upEle.selectFirst(".count");
                Long upCount = 0L;

                try {
                    upCount = Longs.tryParse(upCountEle.text());
                    if (Objects.isNull(upCount)) {
                        upCount = 0L;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                definition.setUpVotes(upCount);

                Element downEle = footer.selectFirst(".down");
                Element downCountEle = downEle.selectFirst(".count");
                Long downCount = 0L;

                try {
                    downCount = Longs.tryParse(downCountEle.text());
                    if (Objects.isNull(upCount)) {
                        downCount = 0L;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                definition.setDownVotes(downCount);

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
}
