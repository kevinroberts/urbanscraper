package com.vinberts.vinscraper;

import com.google.common.collect.Lists;
import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceFive;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceFour;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceOne;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceSix;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceThree;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceTwo;
import com.vinberts.vinscraper.scraping.queues.AlphaWordLoader;
import com.vinberts.vinscraper.scraping.queues.WordQueueProcessing;
import com.vinberts.vinscraper.utils.SystemPropertyLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


/**
 *
 */
@Slf4j
public class Main {

    public static void main(String[] args) {
        SystemPropertyLoader.checkRequiredPropValues();
        //processAlphaLoading(Arrays.asList("C", "D", "E", "F", "G"));
        processQueues(6000);
    }

    private static void processAlphaLoading(List<String> letters) {
        Thread threadOne = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceOne.getInstance().getDriver(), letters.get(0), 1)
        );
        Thread threadTwo = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceTwo.getInstance().getDriver(), letters.get(1), 1)
        );
        Thread threadThree = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceThree.getInstance().getDriver(), letters.get(2), 1)
        );
        Thread threadFour = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceFour.getInstance().getDriver(), letters.get(3), 1)
        );
        Thread threadFive = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceFive.getInstance().getDriver(), letters.get(4), 1)
        );
        // start processing!
        threadOne.start();
        threadTwo.start();
        threadThree.start();
        threadFour.start();
        threadFive.start();
    }

    private static void processQueues(int limit) {

        List<WordQueue> queueList = DatabaseHelper.getUnprocessedWordQueue(limit);
        // split list into equal parts
        int splitSize = queueList.size() / 6;
        List<List<WordQueue>> subSets = Lists.partition(queueList, splitSize);


        // create separate processing threads
        Thread threadOne = new Thread(
                new WordQueueProcessing(ChromeDriverInstanceOne.getInstance().getDriver(),
                        subSets.get(0)));
        Thread threadTwo = new Thread(
                new WordQueueProcessing(ChromeDriverInstanceTwo.getInstance().getDriver(),
                        subSets.get(1)));
        Thread threadThree = new Thread(
                new WordQueueProcessing(ChromeDriverInstanceThree.getInstance().getDriver(),
                        subSets.get(2)));
        Thread threadFour = new Thread(
                new WordQueueProcessing(ChromeDriverInstanceFour.getInstance().getDriver(),
                        subSets.get(3)));
        Thread threadFive = new Thread(
                new WordQueueProcessing(ChromeDriverInstanceFive.getInstance().getDriver(),
                        subSets.get(4)));
        Thread threadSix = new Thread(
                new WordQueueProcessing(ChromeDriverInstanceSix.getInstance().getDriver(),
                        subSets.get(5)));
        // start processing!
        threadOne.start();
        threadTwo.start();
        threadThree.start();
        threadFour.start();
        threadFive.start();
        threadSix.start();
    }

}
