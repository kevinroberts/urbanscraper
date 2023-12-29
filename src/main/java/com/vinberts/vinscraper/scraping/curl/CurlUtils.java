package com.vinberts.vinscraper.scraping.curl;

import com.roxstudio.utils.CUrl;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.nio.charset.StandardCharsets;

import static com.vinberts.vinscraper.scraping.ScrapingConstants.USER_AGENT_WINDOWS;

/**
 *
 */
public class CurlUtils {

    /**
     * Automatically resolve curl's output
     * to its HTML structure
     */
    private static final CUrl.Resolver<Document> htmlResolver = (httpCode, responseBody) -> {
        String html = new String(responseBody, StandardCharsets.UTF_8);
        return Jsoup.parse(html);
    };

    private static final CUrl.Resolver<JSONObject> jsonObjectResolver = (httpCode, responseBody) -> {
        String jsonText = new String(responseBody, StandardCharsets.UTF_8);
        return new JSONObject(jsonText);
    };


    public static Document getHtmlViaCurl(String url) {
        CUrl cUrl = new CUrl(url).opt("-A", USER_AGENT_WINDOWS).opt("-L");
        return cUrl.exec(htmlResolver, null);
    }

    public static JSONObject getJsonViaCurl(String url) {
        CUrl cUrl = new CUrl(url).opt("-A", USER_AGENT_WINDOWS).opt("-L");
        return cUrl.exec(jsonObjectResolver, null);
    }

}
