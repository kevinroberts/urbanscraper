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
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;


/**
 * Main App
 * entry point to Urbanscraper / vinscraper
 */
@Slf4j
public class Main {

    public static final String SYNTAX = "vinscraper";

    public static void main(String[] args) {
        SystemPropertyLoader.checkAndLoadRequiredPropValues();

        // create command line options
        Options options = new Options();
        Option help = new Option( "help", "print this message" );
        Option version = new Option( "version", "print the current version and exit" );
        Option alpha = Option.builder("alpha")
                .argName("letters=startPage")
                .desc("start alphabetical loading based on a list of letters and start pages | e.g: vinscraper -a a,b=1,1 (would be letters a,b - starting at page 1,1)")
                .hasArgs().valueSeparator('=')
                .build();
        Option queue = Option.builder("q")
                .desc("start processing of unloaded words with specified limit argument NUMBER_TO_PROCESS NUMBER_OF_THREADS")
                .hasArgs()
                .numberOfArgs(2)
                .argName("NUMBER_TO_PROCESS NUMBER_OF_THREADS")
                .build();
        Option days = Option.builder("d")
                .desc("start processing of new words by date, NUMBER_OF_DAYS START_DATE (Start date format yyyy-mm-dd)")
                .hasArgs()
                .numberOfArgs(2)
                .optionalArg(true)
                .argName("NUMBER_OF_DAYS START_DATE")
                .build();
        options.addOption(help);
        options.addOption(version);
        options.addOption(alpha);
        options.addOption(queue);
        options.addOption(days);
        // create the parser
        CommandLineParser parser = new DefaultParser();
        try {
            // parse the command line arguments
            CommandLine line = parser.parse( options, args );
            if (line.hasOption(help.getOpt())) {
                printHelp(options);
            } else if (line.hasOption(version.getOpt())) {
                Properties systemProps = SystemPropertyLoader.getSystemProps();
                ConsoleUtil.info("Version: " + systemProps.get("version"));
            } else if (line.hasOption(alpha.getOpt())) {
                String propertyName = line.getOptionValues(alpha.getOpt())[0];  // will be "key / letters"
                String propertyValue = line.getOptionValues(alpha.getOpt())[1]; // will be "value / pages"
                List<Integer> startingPages = new ArrayList<>();
                List<String> startingPagesStr = Splitter.on(",").trimResults()
                        .splitToList(propertyValue);
                for (String startPage: startingPagesStr) {
                    startingPages.add(Ints.tryParse(startPage));
                }
                List<String> letters = Splitter.on(",").trimResults()
                        .splitToList(propertyName);
                ConsoleUtil.info("Running alpha loading for letters "
                        + Arrays.toString(letters.toArray()) + " " +
                        "Starting at pages " + Arrays.toString(startingPagesStr.toArray()));
                processAlphaLoading(letters, startingPages);
            } else if (line.hasOption(queue.getOpt())) {
                String numberOfItemsStr = line.getOptionValues(queue.getOpt())[0];
                String numberOfThreadsStr = line.getOptionValues(queue.getOpt())[1];
                Integer numberOfItems = Ints.tryParse(numberOfItemsStr);
                Integer numberOfThreads = Ints.tryParse(numberOfThreadsStr);
                if (Objects.nonNull(numberOfItems) && Objects.nonNull(numberOfThreads)) {
                    ConsoleUtil.info("Running unprocessed queue for number of items: " + numberOfItemsStr + " with number of threads " + numberOfThreads);
                    if (numberOfThreads < 0 || numberOfThreads > 6) {
                        ConsoleUtil.info("Threads specified greater than max threshold, setting to max value of 6");
                        numberOfThreads = 6;
                    }
                    processQueues(numberOfItems, numberOfThreads);
                } else {
                    throw new ParseException("Invalid number of items / threads specified, must be an integer value");
                }
            } else if (line.hasOption(days.getOpt())) {
                String[] optionValues = line.getOptionValues(days.getOpt());
                String numberOfDaysStr = optionValues[0];
                String dateString = StringUtils.EMPTY;
                if (optionValues.length > 1) {
                    dateString = optionValues[1];
                }
                Integer numberOfDays = Ints.tryParse(numberOfDaysStr);
                if (Objects.nonNull(numberOfDays)) {
                    ConsoleUtil.info("Running day loading for new words : number of days "
                            + numberOfDaysStr + " " +
                            "Starting at date " + dateString);
                    if (StringUtils.isNotEmpty(dateString)) {
                        Thread thread = new Thread(
                                new NewWordLoaderCurl(numberOfDays, dateString));
                        thread.start();
                    } else {
                        Thread thread = new Thread(
                                new NewWordLoaderCurl(numberOfDays));
                        thread.start();
                    }
                } else {
                    throw new ParseException("Invalid number of days; must be an integer value");
                }
            } else {
                ConsoleUtil.info("Please, follow the instructions below:");
                printUsage(options);
            }
        } catch( ParseException exp ) {
            // oops, something went wrong
            ConsoleUtil.info( "Parsing failed.  Reason: " + exp.getMessage() );
            ConsoleUtil.info("Please, follow the instructions below:");
            printHelp(options);
            System.exit(1);
        }
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


    private static void printUsage(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        ConsoleUtil.info("\n==============");
        ConsoleUtil.info("Vinscraper use instructions");
        ConsoleUtil.info("==============");
        final PrintWriter pw  = new PrintWriter(System.out);
        formatter.printUsage(pw, 80, SYNTAX, options);
        pw.flush();
    }


    /**
     * Generate help information with Apache Commons CLI.
     *
     * @param options Instance of Options to be used to prepare
     *    help formatter.
     * @return HelpFormatter instance that can be used to print
     *    help information.
     */
    private static void printHelp(final Options options) {
        final HelpFormatter formatter = new HelpFormatter();
        final String usageHeader = "Vinscraper use instruction";
        final String usageFooter = "";
        formatter.printHelp(SYNTAX, usageHeader, options, usageFooter);
    }

}
