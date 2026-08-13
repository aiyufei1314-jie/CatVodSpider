package com.github.catvod.spider;

import android.content.Context;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.BaseUtil;
import com.github.catvod.utils.CommonUtil;
import com.github.catvod.utils.CryptoUtil;
import com.github.catvod.utils.DecImgUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BLUEGAY extends Spider {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static final String SITE_URL = "aHR0cHM6Ly93d3cueWZpdnhucG8uY2M";
    private static final String VOD_SITE = "aHR0cHM6Ly94bGd2dy5jb20";
    private static final String DATA_BASE = "W1sienhibCIsIuacgOaWsOeIhuaWmSJdLFsieHJuZyIsIumynOiCieWltueLlyJdLFsianpiYiIsIumHkeS4u+eIuOeIuCJdLFsidGNubSIsIuWkqeiPnOeUt+aooSJdLFsib25seWZhbnMiLCLogozogonnjJvnlLciXSxbInhhenkiLCLkvZPogrLnlJ8iXSxbInp6eWgiLCLmraPoo4XnoazmsYkiXSxbImh3Y2ciLCLmrKfnvo7lpKfniYciXSxbImR5d2giLCLmipbpn7PnvZHnuqIiXSxbIm14aGwiLCLmmI7mmJ/pu5HmlpkiXSxbIm55dGciLCLok53lj4vmipXnqL8iXSxbInNqbSIsIuWkmuS6uua4uOaIjyJdLFsibGxkZCIsIuebtOeUt+a4uOaIjyJdLFsidHhnbSIsIuWQjOaAp0fmvKsiXV0";

    private String baseUrl;
    private String vodSite;

    @Override
    public void init(Context context, String extend) throws Exception {
        this.baseUrl = CryptoUtil.base64ToString(SITE_URL);
        this.vodSite = CryptoUtil.base64ToString(VOD_SITE);
    }

    private String extractScriptImage(Element element) {
        Element script = element.selectFirst("script");
        if (script == null) return "";
        Pattern regex = Pattern.compile("'(https?://[^']+)");
        Matcher matcher = regex.matcher(script.data());
        return matcher.find() ? matcher.group(1) : "";
    }

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        List<Future<Vod>> futures = new ArrayList<>();

        for (Element article : doc.select("article")) {
            Element titleElement = article.selectFirst(".post-card-container");
            if (titleElement == null) continue;
            titleElement.select("div, span").remove();
            String name = titleElement.text();
            if (name.isEmpty()) continue;
            Element linkElement = article.selectFirst("a");
            if (linkElement == null) continue;
            String link = linkElement.attr("href").trim();
            if (link.isEmpty()) continue;
            String id = CommonUtil.normalizeId(link);

            String image = "";
            Element script = article.selectFirst("script");
            if (script != null) {
                image = extractScriptImage(article);
            }
            if (image.isEmpty()) {
                Element background = article.selectFirst(".blog-background");
                if (background == null) continue;
                image = background.attr("data-bg-src").trim();
            }
            final String finalImage = image;

            futures.add(EXECUTOR.submit(() -> {
                String base64Img = finalImage.isEmpty() ? BaseUtil.ALIVIDEO : DecImgUtil.loadBackgroundImage(finalImage);
                return new Vod(id, name, base64Img);
            }));
        }

        for (Future<Vod> future : futures) {
            try {
                Vod vod = future.get(3, TimeUnit.SECONDS);
                if (vod != null) list.add(vod);
            } catch (Exception e) {
                future.cancel(true);
            }
        }

        return list;
    }

    private String scriptUrls(Document doc, String detailUrl) {
        StringBuilder playUrl = new StringBuilder();
        int index = 1;
        for (Element script : doc.select("script")) {
            String data = script.data();
            if (!data.contains("eval(function")) continue;
            String m3u8Url =  CommonUtil.blueM3u8Url(data, detailUrl);
            if (m3u8Url.isEmpty()) continue;
            if (playUrl.length() > 0) playUrl.append("#");
            playUrl.append("第").append(index++).append("集$").append(m3u8Url);
        }
        return playUrl.toString();
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        Document doc = Jsoup.parse(OkHttp.string(baseUrl));

        JsonArray outer = JsonParser.parseString(CryptoUtil.base64ToString(DATA_BASE)).getAsJsonArray();
        List<Class> items = new ArrayList<>();
        for (JsonElement element : outer) {
            JsonArray inner = element.getAsJsonArray();
            String id = inner.get(0).getAsString();
            String name = inner.get(1).getAsString();
            items.add(new Class(id, name));
        }

        List<Vod> list = parseVods(doc);
        return Result.string(items, list);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        int page = Integer.parseInt(pg);
        String path = "/category/" + tid + "/" + pg + "/";
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + path));
        int pagerCount = CommonUtil.hasNextPage(doc, path) ? page + 1 : page;
        return Result.string(page, pagerCount, 0, 0, parseVods(doc));
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String id = ids.get(0);
        String detailUrl = baseUrl + id;
        Document doc = Jsoup.parse(OkHttp.string(detailUrl));

        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String desc = extractParagraphs(doc);
        if (desc.isEmpty()) desc = doc.select("meta[property=og:description]").attr("content");
        String year = doc.select("meta[property=og:published_time]").attr("content");
        String playUrl = scriptUrls(doc, detailUrl);

        Vod vod = new Vod();
        vod.setVodId(id);
        vod.setVodName(name);
        vod.setVodPic(pic);
        vod.setVodYear(year);
        vod.setVodContent(vodSite + id + "\n\n" + desc);
        vod.setVodPlayFrom("v1.m3u8");
        vod.setVodPlayUrl(playUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String path = "/search/" + URLEncoder.encode(key, "UTF-8") + "/";
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + path));
        return Result.string(parseVods(doc));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).chrome().string();
    }

    private String extractParagraphs(Document doc) {
        String[] keywords = {"网址", "浏览器"};
        Elements paragraphs = doc.select("article p:not([class])");
        return CommonUtil.extractParagraphs(paragraphs, keywords);
    }
}
