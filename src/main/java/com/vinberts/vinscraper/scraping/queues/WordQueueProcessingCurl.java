package com.vinberts.vinscraper.scraping.queues;

import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.scraping.curl.CurlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.List;
import java.util.Objects;

/**
 *
 */
@Slf4j
public class WordQueueProcessingCurl extends WordLoader implements Runnable {

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

}
