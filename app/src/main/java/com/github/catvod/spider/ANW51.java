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
import com.google.gson.JsonObject;
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

public class ANW51 extends Spider {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static final String SITE_URL = "aHR0cHM6Ly93d3cud3dzZnVyZnMuY29t";
    private static final String VOD_SITE = "aHR0cHM6Ly81MWF3LmNvbQ";
    private static final String DATA_BASE = "W1sianJyZyIsIuS7iuaXpeWQg+eTnCJdLFsicXdycyIsIuWFqOe9keeDreaQnCJdLFsiZGFyay1oaXN0b3J5Iiwi5pqX5Y+y5qGj5qGIIl0sWyJhd2NnIiwi5pqX572R54iG5paZIl0sWyJkeXdoIiwi5oqW6Z+z572R57qiIl0sWyJmY2xsIiwi5Y+N5beu5Lym55CGIl0sWyJ4eWNnIiwi5a2m55Sf5qCh5ZutIl0sWyJhbndhbmdsdWFubHVuIiwi55yf5a6e5Lmx5LymIl0sWyJzeHpxIiwi56aP5Yip6KeG6aKRIl0sWyJod2F3Iiwi5rW35aSW5aSn54mHIl0sWyJhd2R6IiwiQVbop6Por7QiXSxbImF3bHEiLCLph43lj6PnjI7lpYciXSxbIm1yZHMiLCLmr4/ml6XlpKfotZsiXSxbInRhbmh1YSIsIuaOouiKseWBt+aLjSJdLFsiY3VuemhpIiwi5a+45q2i5oyR5oiYIl0sWyJkbXR0Iiwi5Yqo5ryr5aSp5aCCIl1d";

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
        Pattern p = Pattern.compile("'(https?://[^']+)");
        Matcher m = p.matcher(script.data());
        return m.find() ? m.group(1) : "";
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
            String image = extractScriptImage(article);

            futures.add(EXECUTOR.submit(() -> {
                String base64Img = image.isEmpty() ? BaseUtil.ALIVIDEO : DecImgUtil.loadBackgroundImage(image);
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

    private String dplayerUrls(Document doc) {
        StringBuilder playUrl = new StringBuilder();
        int index = 1;
        for (Element element : doc.select("div.dplayer")) {
            String dataConfig = element.attr("data-config");
            JsonObject dataRoot = JsonParser.parseString(dataConfig).getAsJsonObject();
            JsonObject dataVideo = dataRoot.getAsJsonObject("video");
            if (dataVideo == null || dataVideo.isEmpty()) continue;
            if (playUrl.length() > 0) playUrl.append("#");
            playUrl.append("第").append(index++).append("集$").append(dataVideo.get("url").getAsString());
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

        return Result.string(items, parseVods(doc));
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
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + id));

        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String desc = getDesc(doc);
        if (desc.isEmpty()) desc = doc.select("meta[property=og:description]").attr("content");
        String year = doc.select("meta[property=article:published_time]").attr("content");
        String playUrl = dplayerUrls(doc);

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
        String path = ("/search/" + URLEncoder.encode(key, "UTF-8") + "/");
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + path));
        return Result.string(parseVods(doc));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).chrome().string();
    }

    private String getDesc(Document doc) {
        String[] keywords = {"网址", "浏览器", "重磅吃瓜", "点击分享", "永久免费", "吃瓜回家不迷路"};
        Elements paragraphs = doc.select("article p:not([class])");
        return CommonUtil.extractParagraphs(paragraphs, keywords);
    }
}
