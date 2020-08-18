package com.vinberts.vinscraper;

import com.vinberts.vinscraper.utils.MyChromeDriver;
import com.vinberts.vinscraper.utils.SystemPropertyLoader;
import com.vinberts.vinscraper.utils.UrbanDictionaryUtils;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 *
 */
@Slf4j
public class Main {

    public static void main(String[] args) {
        SystemPropertyLoader.loadPropValues();
        loadDefinitionsByAlpha();
//                int pagesToVisit = 3;
//                int currentPage = 1;
//        loadDefinitionsByPage(pagesToVisit, currentPage);
    }

    private static void loadDefinitionsByAlpha() {
        Map<String, String> linkedMap = new LinkedHashMap<>();
        MyChromeDriver chromeDriver = MyChromeDriver.getInstance();
        List<String> list = Arrays.asList("A", "B", "C",
                "D", "E", "F", "G", "H", "I", "J",
                "K", "L", "M", "N", "O", "P", "Q",
                "R", "S", "T", "U", "V", "W", "X",
                "Y", "Z");
        for (String letter: list) {
            int pagesToVisit = 800;
            int currentPage = 12;
            String pageToLoad = String.format("https://www.urbandictionary.com/browse.php?character=%s&page=%d", letter, currentPage);
            chromeDriver.getDriver().get(pageToLoad);
            WebElement listOfWords = chromeDriver.getDriver().findElement(By.className("no-bullet"));
            List<WebElement> anchors = listOfWords.findElements(By.tagName("a"));
            log.info(String.format("Found %d words on page %d for letter %s", anchors.size(), currentPage, letter));
            loadToLinkedMap(linkedMap, anchors);
            loadLinkedMapToDB(linkedMap);
            linkedMap.clear();
            currentPage++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("InterruptedException thread exception", e);
            }
            for (int i = pagesToVisit; i >= 1; i--) {
                String nextPageToLoad = String.format("https://www.urbandictionary.com/browse.php?character=%s&page=%d", letter, currentPage);
                chromeDriver.getDriver().navigate().to(nextPageToLoad);
                WebElement listWords = chromeDriver.getDriver().findElement(By.className("no-bullet"));
                List<WebElement> wordAnchors = listWords.findElements(By.tagName("a"));
                log.info(String.format("Found %d words on page %d for letter %s", wordAnchors.size(), currentPage, letter));
                currentPage++;
                loadToLinkedMap(linkedMap, wordAnchors);
                loadLinkedMapToDB(linkedMap);
                linkedMap.clear();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("InterruptedException thread exception", e);
                }
            }
        }
    }

    private static void loadLinkedMapToDB(final Map<String, String> linkedMap) {
        MyChromeDriver chromeDriver = MyChromeDriver.getInstance();
        for (Map.Entry<String, String> entry : linkedMap.entrySet()) {
            String link = entry.getValue();
            chromeDriver.getDriver().navigate().to(link);
            List<WebElement> defPanels = chromeDriver.getDriver().findElements(By.className("def-panel"));
            UrbanDictionaryUtils.attemptSaveNewDefinition(defPanels.get(0));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("InterruptedException thread exception", e);
            }
        }
    }

    private static void loadToLinkedMap(final Map<String, String> linkedMap, final List<WebElement> anchors) {
        for (WebElement element: anchors) {
            String word = element.getText();
            linkedMap.put(word, element.getAttribute("href"));
        }
    }


}
