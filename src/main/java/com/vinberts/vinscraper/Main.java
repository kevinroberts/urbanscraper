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
import com.vinberts.vinscraper.scraping.queues.AlphaWordLoader;
import com.vinberts.vinscraper.scraping.queues.WordQueueProcessing;
import com.vinberts.vinscraper.utils.ConsoleUtil;
import com.vinberts.vinscraper.utils.SystemPropertyLoader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;


/**
 *
 */
@Slf4j
public class Main {

    public static void main(String[] args) {
        SystemPropertyLoader.checkAndLoadRequiredPropValues();
        Scanner console = new Scanner(System.in);
        int quitCode = 3;
        int userInput = 1;
        do {
            if (userInput == quitCode) {
                break;
            }
            ConsoleUtil.info("\nUrbanScraper menu:\n " +
                    "1. Start alpha loading\n " +
                    "2. Start processing word queue " +
                    "\n " + quitCode + ". Quit the application");

            try {
                userInput = console.nextInt();
            } catch (InputMismatchException ex) {
                ConsoleUtil.info("Sorry that was not an acceptable input.");
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
                    ConsoleUtil.info("Goodbye");
                    break;
            }
        } while (true);
    }

    private static void processAlphaLoading(List<String> letters, List<Integer> startingPages) {

        Thread threadOne = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceOne.getInstance().getDriver(), letters.get(0), startingPages.get(0))
        );
        threadOne.start();
        if (letters.size() > 1) {
            Thread threadTwo = new Thread(
                    new AlphaWordLoader(ChromeDriverInstanceTwo.getInstance().getDriver(), letters.get(1), startingPages.get(1))
            );
            threadTwo.start();
            if (letters.size() > 2) {
                Thread threadThree = new Thread(
                        new AlphaWordLoader(ChromeDriverInstanceThree.getInstance().getDriver(), letters.get(2), startingPages.get(2))
                );
                threadThree.start();
            }
            if (letters.size() > 3) {
                Thread threadFour = new Thread(
                        new AlphaWordLoader(ChromeDriverInstanceFour.getInstance().getDriver(), letters.get(3), startingPages.get(3))
                );
                threadFour.start();
            }
            if (letters.size() > 4) {
                Thread threadFive = new Thread(
                        new AlphaWordLoader(ChromeDriverInstanceFive.getInstance().getDriver(), letters.get(4), startingPages.get(4))
                );
                threadFive.start();
            }
        }
    }

    private static void processQueues(int limit, int threads) {

        List<WordQueue> queueList = DatabaseHelper.getUnprocessedWordQueue(limit);
        // split list into equal parts
        int splitSize = queueList.size() / threads;
        List<List<WordQueue>> subSets = Lists.partition(queueList, splitSize);

        for (int i = 0; i <= threads; i++) {
            Thread thread = new Thread(
                    new WordQueueProcessing(getNewDriver(i),
                            subSets.get(i)));
            thread.start();
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
