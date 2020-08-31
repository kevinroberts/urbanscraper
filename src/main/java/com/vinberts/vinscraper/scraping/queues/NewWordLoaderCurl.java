package com.vinberts.vinscraper.scraping.queues;

import com.google.common.primitives.Ints;
import com.vinberts.vinscraper.scraping.curl.CurlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
    private String dateString;

    public NewWordLoaderCurl(final int numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    public NewWordLoaderCurl(final int numberOfDays, final String dateString) {
        this.numberOfDays = numberOfDays;
        this.dateString = dateString;
    }

    @Override
    public void run() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(URBAN_DATE_FORMAT_SHORT);
        final long subDays = 1L;
        LocalDate date = LocalDate.now();
        if (StringUtils.isNotEmpty(dateString)) {
            date = LocalDate.parse(dateString, formatter);
        }
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
            Integer currentPage = 1;
            Integer lastPage = 2;
            // check if this page contains a pagination list of child pages
            Elements paginationElements = document.select(".pagination li");
            if (Objects.nonNull(paginationElements) && !paginationElements.isEmpty()) {
                Element paginationEleLast = paginationElements.last();
                String lastPageStr = StringUtils.substringAfterLast(paginationEleLast.child(0).attr("href"), "page=");
                if (StringUtils.isNotEmpty(lastPageStr)) {
                    lastPage = Ints.tryParse(lastPageStr);
                }
                while (Objects.nonNull(lastPage) && lastPage > currentPage) {
                    currentPage++;
                    log.info("Loading page " + currentPage + " for date " + dateString);
                    String nextPageToLoadInner = String.format("https://www.urbandictionary.com/yesterday.php?date=%s&page=%d",
                            dateString, currentPage);
                    Document documentPage = CurlUtils.getHtmlViaCurl(nextPageToLoadInner);
                    Element listWordsPage = Objects.nonNull(documentPage) ? documentPage.selectFirst(".no-bullet") : null;
                    if (Objects.nonNull(listWordsPage)) {
                        List<Element> wordAnchors = listWords.select("a");
                        log.info(String.format("Thread %s: Found %d words for date %s on page %d",
                                Thread.currentThread().getName(),
                                wordAnchors.size(),
                                dateString, currentPage));
                        loadToDBQueue(wordAnchors);
                    } else {
                        log.warn("Could not load any words for date " + dateString);
                    }
                }
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
