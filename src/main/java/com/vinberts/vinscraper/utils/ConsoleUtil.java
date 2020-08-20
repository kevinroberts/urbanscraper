package com.vinberts.vinscraper.utils;

/**
 *
 */
public class ConsoleUtil {

    public static void info(String message) {
        consolePrintLine(message);
    }

    private static void consolePrintLine(String msg) {
        System.out.println(msg);
    }

    private static void consolePrint(String msg) {
        System.out.print(msg);
    }
}
