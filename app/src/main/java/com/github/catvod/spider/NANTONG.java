package com.github.catvod.spider;

import android.content.Context;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Filter;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.BaseUtil;
import com.github.catvod.utils.CommonUtil;
import com.github.catvod.utils.CryptoUtil;
import com.github.catvod.utils.DecImgUtil;
import com.github.catvod.utils.UnpackUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

public class NANTONG extends Spider {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final int PAGE_SIZE = 32;

    private static final String SITE_URL = "aHR0cHM6Ly93d3cubWtvZnRxemh5LmNj";
    private static final String VOD_SITE = "aHR0cHM6Ly85MW50LmNvbQ";
    private static final String DATA_BASE = "W1siYWxsIiwi57K+6YCJ5b2x54mHIl0sWyJwb3N0cyIsIuWQjOW/l+W4luWtkCJdLFsieHJiaiIsIumynOiCieiWhOiCjCJdLFsid3RucyIsIuaXoOWll+WGheWwhCJdLFsiemZ5aCIsIuWItuacjeivseaDkSJdLFsiZG1maiIsIuiAvee+juWkqeiPnCJdLFsianJtbiIsIuiCjOiCieeMm+eUtyJdLFsicmhndiIsIuaXpemfqeS4k+WMuiJdLFsib21qZCIsIuasp+e+juW3qOWxjCJdLFsiZHJxaiIsIuWkmuS6uue+pOS6pCJdLFsia2p5cyIsIuWPo+S6pOminOWwhCJdLFsidGpzbSIsIuiwg+aVmVNNIl1d";
    private static final String DATA_CATE = "eyJhbGwiOltbIuato+WcqOaSreaUviIsIndhdGNoaW5ncyJdLFsi6auY5riFIiwiaGQiXSxbIuW9k+WJjeacgOeDrSIsInBvcHVsYXIiXSxbIuacgOi/keabtOaWsCIsIm5ldyJdLFsi5pys5pyI5pyA54OtIiwibW9uIl0sWyIxMOWIhumSn+S7peS4iiIsIjEwbWluIl0sWyIyMOWIhumSn+S7peS4iiIsIjIwbWluIl0sWyLmr4/mnIjmnIDng60iLCJldmVyeSJdLFsi5pys5pyI5pS26JePIiwiY29sbGVjdCJdLFsi5pS26JeP5pyA5aSaIiwibW9zdCJdLFsi5pys5pyI6K6o6K66IiwiY3VycmVudCJdLFsi5bCP6JOd5Y6f5YibIiwieGlhb2xhbiJdXSwicG9zdHMiOltbIuWFqOmDqCIsImFsbCJdLFsi5ZCM5b+X6buR5paZIiwidHpobCJdLFsi572R57qi54iG5paZIiwid2hibCJdLFsi6bKc6IKJ5aW254uXIiwieHJuZyJdLFsi5q2j6KOF5Yi25pyNIiwienp6ZiJdLFsi5aSp6I+c55S35qihIiwidGNubSJdLFsi6IC9576O5ryr55WqIiwiZG1tZiJdXX0";

    private String baseUrl;
    private String vodSite;
    private Map<String, String[][]> extendCate = new HashMap<>();

    @Override
    public void init(Context context, String extend) throws Exception {
        this.baseUrl = CryptoUtil.base64ToString(SITE_URL);
        this.vodSite = CryptoUtil.base64ToString(VOD_SITE);
        this.extendCate = CommonUtil.parseConfig(CryptoUtil.base64ToString(DATA_CATE));
    }

    private LinkedHashMap<String, List<Filter>> buildFilters(List<Class> classes) {
        LinkedHashMap<String, List<Filter>> filters = new LinkedHashMap<>();
        for (Class cls : classes) {
            String[][] subCases = extendCate.get(cls.getTypeId());
            if (subCases == null) continue;
            List<Filter.Value> values = new ArrayList<>();
            for (String[] sub : subCases) values.add(new Filter.Value(sub[0], sub[1]));
            List<Filter> filterList = new ArrayList<>();
            filterList.add(new Filter("cateId", "类型", values));
            filters.put(cls.getTypeId(), filterList);
        }
        return filters;
    }

    private List<Vod> parseVods(Document doc, boolean limit) {
        List<Vod> list = new ArrayList<>();
        List<Future<Vod>> futures = new ArrayList<>();

        int count = 0;
        for (Element video : doc.select("li:has(div.poster)")) {
            if (limit && count >= PAGE_SIZE) break;
            Element post = video.select("a[class*=line-clamp], a:has([class*=line-clamp])").first();
            if (post == null || post.toString().contains("data-ad_id")) continue;
            String link = post.attr("href").trim();
            if (link.isEmpty() || (limit && link.contains("posts/"))) continue;
            String name = post.text();
            String id = CommonUtil.normalizeId(link);
            Element img = video.selectFirst("img[data-src]");
            String image = img == null ? "" : img.attr("data-src").trim();
            count++;

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

    private int pagerCount(Document doc, int page) {
        try {
            Element ul = doc.selectFirst("ul.pager");
            if (ul == null) return page + 1;
            int limit = Integer.parseInt(ul.attr("data-rec-per-page"));
            int total = Integer.parseInt(ul.attr("data-rec-total"));
            return (total + limit - 1) / limit;
        } catch (Exception e) {
            return page + 1;
        }
    }

    private String cate(String cateId, String fallback) {
        if (!cateId.isEmpty()) return cateId;
        String[][] subCases = extendCate.get(fallback);
        return subCases != null && subCases.length > 0 ? subCases[0][1] : fallback;
    }

    private String scriptUrls(Document doc, String detailUrl) {
        StringBuilder playUrl = new StringBuilder();
        int index = 1;
        for (Element script : doc.select("script")) {
            String data = script.data();
            if (!data.contains("eval(function")) continue;
            String m3u8Url = CommonUtil.tongM3u8Url(data, detailUrl);
            if (m3u8Url == null || m3u8Url.isEmpty()) continue;
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

        LinkedHashMap<String, List<Filter>> filters = buildFilters(items);
        return Result.string(items, parseVods(doc, true), filters);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        int page = Integer.parseInt(pg);
        String cateId = extend.getOrDefault("cateId", "");
        String path;
        if (Objects.equals(tid, "all")) {
            path = "/videos/all/" + cate(cateId, "all") + "/" + pg + "/";
        } else if (Objects.equals(tid, "posts")) {
            path = "/posts/category/" + cate(cateId, "posts") + "/" + pg + "/";
        } else {
            path = "/videos/category/" + tid + "/" + pg + "/";
        }
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + path));
        return Result.string(page, pagerCount(doc, page), 0, 0, parseVods(doc, false));
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String id = ids.get(0);
        String detailUrl = baseUrl + id;
        Document doc = Jsoup.parse(OkHttp.string(detailUrl));

        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String desc = doc.select("meta[property=og:description]").attr("content");
        String year = detailUrl.contains("/posts/")
                ? doc.select("meta[property=article:published_time]").attr("content")
                : doc.select("meta[property=video:release_date]").attr("content");
        String playUrl = scriptUrls(doc, detailUrl);

        Vod vod = new Vod();
        vod.setVodId(id);
        vod.setVodPic(pic);
        vod.setVodYear(year);
        vod.setVodName(name);
        vod.setVodContent(vodSite + id + "\n\n" + desc);
        vod.setVodPlayFrom("v1.m3u8");
        vod.setVodPlayUrl(playUrl);
        return Result.string(vod);
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        String path = ("/videos/search/" + URLEncoder.encode(key, "UTF-8") + "/");
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + path));
        return Result.string(parseVods(doc, false));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).chrome().string();
    }
}
