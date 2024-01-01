package com.vinberts.vinscraper.scraping.queues;

import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.scraping.curl.CurlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Slf4j
public class WordQueueProcessingCurl extends WordLoader implements Runnable {

    private final List<WordQueue> wordQueueList;
    private boolean isReRun = false;

    public WordQueueProcessingCurl(List<WordQueue> wordQueueList, boolean isReRun) {
        this.wordQueueList = wordQueueList;
        this.isReRun = isReRun;
    }

    @Override
    public void run() {
        log.info("processing in thread " +
                Thread.currentThread().getName() +
                " for word queue of size " + wordQueueList.size());
        for (WordQueue queue: wordQueueList) {
            String encodedWord = URLEncoder.encode(queue.getWord(), StandardCharsets.UTF_8);
            JSONObject defObj = CurlUtils.getJsonViaCurl("https://api.urbandictionary.com/v0/define?term=" + encodedWord);
            if (Objects.isNull(defObj)) {
                log.warn(queue.getWord() + " could not be loaded or errored out");
                DatabaseHelper.updateWordQueueProcess(queue, false, isReRun);
            } else {
                JSONArray defList = defObj.getJSONArray("list");
                // Don't re-run for single definition or empty list
                if (isReRun && (defList.length() == 1 || defList.isEmpty())) {
                    DatabaseHelper.updateWordQueueProcess(queue, true, isReRun);
                } else {
                    DatabaseHelper.updateWordQueueProcess(queue, attemptSaveNewDefinition(defObj), isReRun);
                }
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
