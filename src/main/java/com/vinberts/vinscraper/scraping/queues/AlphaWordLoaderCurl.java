package com.vinberts.vinscraper.scraping.queues;

import com.google.common.primitives.Ints;
import com.vinberts.vinscraper.scraping.curl.CurlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Objects;

import static com.vinberts.vinscraper.scraping.ScrapingConstants.MAIN_UL_SELECTOR;
import static com.vinberts.vinscraper.scraping.ScrapingConstants.URBAN_DIC_URL;

/**
 * AlphaWordLoaderCurl
 */
@Slf4j
public class AlphaWordLoaderCurl extends WordLoader implements Runnable {
    private final String letterToLoad;
    private final int startPage;

    public AlphaWordLoaderCurl(final String letterToLoad) {
        this.letterToLoad = letterToLoad;
        this.startPage = 1;
    }

    public AlphaWordLoaderCurl(final String letterToLoad, final int startPage) {
        this.letterToLoad = letterToLoad;
        this.startPage = startPage;
    }

    @Override
    public void run() {
        int currentPage = startPage;
        final int pagesToVisit = 9000;
        Integer lastPage = startPage;
        for (int i = pagesToVisit; i >= 1; i--) {
            String nextPageToLoad = String.format(URBAN_DIC_URL + "/browse.php?character=%s&page=%d",
                    letterToLoad.toUpperCase(), currentPage);
            Document document = CurlUtils.getHtmlViaCurl(nextPageToLoad);

            Element listWords = Objects.nonNull(document) ? document.selectFirst(MAIN_UL_SELECTOR) : null;
            if (Objects.nonNull(listWords)) {
                List<Element> wordAnchors = listWords.select("a");
                log.info(String.format("Thread %s: Found %d words on page %d for letter %s",
                        Thread.currentThread().getName(),
                        wordAnchors.size(),
                        currentPage,
                        letterToLoad));
                loadToDBQueue(wordAnchors);
            } else {
                log.warn("Could not load any words for page " + currentPage + " for letter " + letterToLoad);
            }
            Elements paginationElements = document.select(".pagination li");
            if (Objects.nonNull(paginationElements) && !paginationElements.isEmpty()) {
                Element paginationEleLast = paginationElements.last();
                String lastPageStr = StringUtils.substringAfterLast(paginationEleLast.child(0).attr("href"), "page=");
                if (StringUtils.isNotEmpty(lastPageStr)) {
                    lastPage = Ints.tryParse(lastPageStr);
                }
                if (Objects.nonNull(lastPage) && lastPage == currentPage) {
                    log.info(String.format("Thread %s has reached final page %d for alpha load for letter %s",
                            Thread.currentThread().getName(),
                            currentPage,
                            letterToLoad));
                    break;
                }
            }
            currentPage++;
            try {
                Thread.sleep(RandomUtils.nextInt(300,400));
            } catch (InterruptedException e) {
                log.error("InterruptedException thread exception", e);
            }
        }
        log.info(String.format("Thread %s Finished processing alpha load for letter %s",
                Thread.currentThread().getName(),
                letterToLoad));
    }

}
