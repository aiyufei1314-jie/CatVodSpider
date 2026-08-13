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

public class CG51 extends Spider {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Pattern SCRIPT_IMG = Pattern.compile("'(https?://[^']+)");

    private static final String SITE_URL = "aHR0cHM6Ly93d3cuc3Bud2hnYXcuY29t";
    private static final String VOD_SITE = "aHR0cHM6Ly81MWNnMS5jb20";
    private static final String DATA_BASE = "W1sid3BjeiIsIuS7iuaXpeWQg+eTnCJdLFsieHN4eSIsIuWtpueUn+agoeWbrSJdLFsid2hobCIsIue9kee6oum7keaWmSJdLFsicmRzaiIsIueDremXqOWkp+eTnCJdLFsiYmtkZyIsIuW/heeci+Wkp+eTnCJdLFsiY2JkaiIsIuaTpui+ueefreWJpyJdLFsid2hteCIsIuaYjuaYn+m7keaWmSJdLFsiaHdjZyIsIua1t+WkluWQg+eTnCJdLFsicnJjZyIsIuS6uuS6uuWQg+eTnCJdLFsibGRjZyIsIumihuWvvOW5sumDqCJdLFsicXViayIsIuWQg+eTnOeci+aIjyJdLFsiY2d4dyIsIuWQg+eTnOaWsOmXuyJdXQ";

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
        Matcher matcher = SCRIPT_IMG.matcher(script.data());
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
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + id));

        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String desc = extractParagraphs(doc);
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
        String path = "/search/" + URLEncoder.encode(key, "UTF-8") + "/";
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + path));
        return Result.string(parseVods(doc));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).chrome().string();
    }

    private String extractParagraphs(Document doc) {
        String[] keywords = {"网址", "浏览器", "版权声明："};
        Elements paragraphs = doc.select("article p:not([class])");
        return CommonUtil.extractParagraphs(paragraphs, keywords);
    }
}
