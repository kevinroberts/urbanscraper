package com.vinberts.vinscraper.scraping.queues;

import com.vinberts.vinscraper.scraping.curl.CurlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import static com.vinberts.vinscraper.scraping.ScrapingConstants.URBAN_DATE_FORMAT_SHORT;

/**
 *
 */
@Slf4j
public class NewWordLoaderCurl extends WordLoader implements Runnable {
    private final int numberOfDays;

    public NewWordLoaderCurl(final int numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    @Override
    public void run() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(URBAN_DATE_FORMAT_SHORT);

        long subDays = 1L;
        LocalDate date = LocalDate.now();
        for (int i = 0; i < numberOfDays; i++) {
            String dateString = date.format(formatter);
            String nextPageToLoad = String.format("https://www.urbandictionary.com/yesterday.php?date=%s",
                    dateString);
            Document document = CurlUtils.getHtmlViaCurl(nextPageToLoad);
            Element listWords = Objects.nonNull(document) ? document.selectFirst(".no-bullet") : null;
            if (Objects.nonNull(listWords)) {
                List<Element> wordAnchors = listWords.select("a");
                log.info(String.format("Thread %s: Found %d words for date %s",
                        Thread.currentThread().getName(),
                        wordAnchors.size(),
                        dateString));
                loadToDBQueue(wordAnchors);
            } else {
                log.warn("Could not load any words for date " + dateString);
            }

            date = date.minusDays(subDays);
            try {
                Thread.sleep(RandomUtils.nextInt(300,400));
            } catch (InterruptedException e) {
                log.error("InterruptedException thread exception", e);
            }
        }

        log.info(String.format("Thread %s Finished processing alpha load for number of days %d",
                Thread.currentThread().getName(),
                numberOfDays));
    }

}
