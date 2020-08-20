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
                    List<String> letters = new ArrayList<>();
                    List<Integer> startingPages = new ArrayList<>();
                    ConsoleUtil.info("Enter first letter:");
                    String letter = console.next();
                    letters.add(letter);

                    ConsoleUtil.info("Which page to start at?");
                    int startPage = console.nextInt();
                    startingPages.add(startPage);

                    ConsoleUtil.info("Enter second letter:");
                    letter = console.next();
                    letters.add(letter);

                    ConsoleUtil.info("Which page to start at?");
                    startPage = console.nextInt();
                    startingPages.add(startPage);

                    ConsoleUtil.info("Enter third letter:");
                    letter = console.next();
                    letters.add(letter);

                    ConsoleUtil.info("Which page to start at?");
                    startPage = console.nextInt();
                    startingPages.add(startPage);

                    ConsoleUtil.info("Enter fourth letter:");
                    letter = console.next();
                    letters.add(letter);

                    ConsoleUtil.info("Which page to start at?");
                    startPage = console.nextInt();
                    startingPages.add(startPage);

                    ConsoleUtil.info("Enter fifth letter:");
                    letter = console.next();
                    letters.add(letter);

                    ConsoleUtil.info("Which page to start at?");
                    startPage = console.nextInt();
                    startingPages.add(startPage);

                    ConsoleUtil.info("Running alpha loading for " + Arrays.toString(letters.toArray()));
                    processAlphaLoading(letters, startingPages);
                    break;
                case 2:
                    ConsoleUtil.info("How many queue records do you want to process?");
                    int recordsToProcess = console.nextInt();
                    if (recordsToProcess > 0) {
                        ConsoleUtil.info("Starting process for loading " + recordsToProcess + " words");
                        processQueues(recordsToProcess);
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
        Thread threadTwo = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceTwo.getInstance().getDriver(), letters.get(1), startingPages.get(1))
        );
        Thread threadThree = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceThree.getInstance().getDriver(), letters.get(2), startingPages.get(2))
        );
        Thread threadFour = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceFour.getInstance().getDriver(), letters.get(3), startingPages.get(3))
        );
        Thread threadFive = new Thread(
                new AlphaWordLoader(ChromeDriverInstanceFive.getInstance().getDriver(), letters.get(4), startingPages.get(4))
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
