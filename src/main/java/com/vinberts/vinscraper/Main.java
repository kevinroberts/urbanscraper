package com.vinberts.vinscraper;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import com.vinberts.vinscraper.database.DatabaseHelper;
import com.vinberts.vinscraper.database.models.WordQueue;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverEx;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceFive;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceFour;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceOne;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceSeven;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceSix;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceThree;
import com.vinberts.vinscraper.scraping.chrome.ChromeDriverInstanceTwo;
import com.vinberts.vinscraper.scraping.queues.AlphaWordLoaderCurl;
import com.vinberts.vinscraper.scraping.queues.NewWordLoaderCurl;
import com.vinberts.vinscraper.scraping.queues.WordQueueProcessingCurl;
import com.vinberts.vinscraper.utils.ConsoleUtil;
import com.vinberts.vinscraper.utils.SystemPropertyLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;


/**
 * Main App
 * entry point to Urbanscraper
 */
@Slf4j
public class Main {

    public static void main(String[] args) {
        SystemPropertyLoader.checkAndLoadRequiredPropValues();
        Scanner console = new Scanner(System.in);
        int quitCode = 5;
        int userInput = 1;
        do {
            if (userInput == quitCode) {
                break;
            }
            ConsoleUtil.info("\nUrbanScraper menu:\n " +
                    "1. Start alpha loading\n " +
                    "2. Start processing word queue \n " +
                    "3. Load new words by date (from today) \n " +
                    "4. Load new words by date (specified)" +
                    "\n " + quitCode + ". Quit the application");

            try {
                userInput = console.nextInt();
            } catch (InputMismatchException ex) {
                ConsoleUtil.info("Sorry that was not an acceptable input. (enter a number between 1 and " + quitCode + ")");
                break;
            }

            switch (userInput) {
                case 1: // '\001'
                    List<Integer> startingPages = new ArrayList<>();
                    ConsoleUtil.info("Enter list of letters (separated by commas):");
                    String letterList = console.next();

                    List<String> letters = Splitter.on(",").trimResults()
                            .splitToList(letterList);

                    ConsoleUtil.info("Enter pages to start at (separated by commas)?");
                    String pagesList = console.next();
                    List<String> startingPagesStr = Splitter.on(",").trimResults()
                            .splitToList(pagesList);
                    for (String startPage: startingPagesStr) {
                        startingPages.add(Ints.tryParse(startPage));
                    }

                    ConsoleUtil.info("Running alpha loading for " + Arrays.toString(letters.toArray()));
                    processAlphaLoading(letters, startingPages);
                    break;
                case 2:
                    ConsoleUtil.info("How many queue records do you want to process?");
                    int recordsToProcess = console.nextInt();
                    if (recordsToProcess > 0) {
                        ConsoleUtil.info("Starting process for loading " + recordsToProcess + " words");
                        ConsoleUtil.info("How many browser threads do you want to use?");
                        int threads = console.nextInt();
                        if (threads < 0 || threads > 6) {
                            threads = 1;
                        }
                        processQueues(recordsToProcess, threads);
                    }
                    break;
                case 3:
                    ConsoleUtil.info("How many days do you want to process?");
                    int days = console.nextInt();
                    if (days > 0) {
                        ConsoleUtil.info("Starting process for loading " + days + " days worth of words");
                        Thread thread = new Thread(
                                new NewWordLoaderCurl(days));
                        thread.start();
                    }
                    break;
                case 4:
                    ConsoleUtil.info("Specify date string in format: 2020-01-30");
                    String dateString = console.next();
                    if (StringUtils.isNotEmpty(dateString)) {
                        ConsoleUtil.info("Starting process for loading " + dateString + " new words");
                        Thread thread = new Thread(
                                new NewWordLoaderCurl(1, dateString));
                        thread.start();
                    }
                    break;
                case 5:
                    ConsoleUtil.info("Goodbye");
                    break;
            }
        } while (true);
    }

    private static void processAlphaLoading(List<String> letters, List<Integer> startingPages) {
        for (int i = 0; i < letters.size(); i++) {
            Thread thread = new Thread(
                    new AlphaWordLoaderCurl(letters.get(i), startingPages.get(i)));
            thread.start();
        }
    }

    private static void processQueues(int limit, int threads) {
        List<WordQueue> queueList = DatabaseHelper.getUnprocessedWordQueue(limit);
        if (queueList.size() > 0) {
            DatabaseHelper.markWordQueueAsInProcess(limit);
            // split list into equal parts
            int splitSize = queueList.size() / threads;
            List<List<WordQueue>> subSets = Lists.partition(queueList, splitSize);

            for (int i = 0; i < threads; i++) {
                Thread thread = new Thread(
                        new WordQueueProcessingCurl(subSets.get(i)));
                thread.start();
            }
        } else {
            log.info("No entries found in unprocessed word queue");
        }
    }

    private static ChromeDriverEx getNewDriver(int threadNumber) {
        switch (threadNumber) {
            case 1:
                return ChromeDriverInstanceTwo.getInstance().getDriver();
            case 2:
                return ChromeDriverInstanceThree.getInstance().getDriver();
            case 3:
                return ChromeDriverInstanceFour.getInstance().getDriver();
            case 4:
                return ChromeDriverInstanceFive.getInstance().getDriver();
            case 5:
                return ChromeDriverInstanceSix.getInstance().getDriver();
            case 6:
                return ChromeDriverInstanceSeven.getInstance().getDriver();
            default:
                return ChromeDriverInstanceOne.getInstance().getDriver();
        }
    }

}
