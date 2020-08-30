package com.vinberts.vinscraper.scraping.queues;

import com.google.common.primitives.Ints;
import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.scraping.curl.CurlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * AlphaWordLoaderCurl
 */
@Slf4j
public class AlphaWordLoaderCurl implements Runnable {
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
            String nextPageToLoad = String.format("https://www.urbandictionary.com/browse.php?character=%s&page=%d",
                    letterToLoad.toUpperCase(), currentPage);
            Document document = CurlUtils.getHtmlViaCurl(nextPageToLoad);

            Element listWords = Objects.nonNull(document) ? document.selectFirst(".no-bullet") : null;
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
            Elements paginationEles = document.select(".pagination li");
            if (Objects.nonNull(paginationEles)) {
                Element paginationEleLast = paginationEles.last();
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

    private void loadToDBQueue(final List<Element> anchors) {
        for (Element element: anchors) {
            String word = element.text();
            Optional<WordQueue> existingWord = DatabaseHelper.getWordQueueByWord(word);
            if (existingWord.isEmpty()) {
                WordQueue queue = new WordQueue();
                queue.setWord(word);
                queue.setUrl(element.attr("href"));
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
}
