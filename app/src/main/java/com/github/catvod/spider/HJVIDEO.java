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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class HJVIDEO extends Spider {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static final String SITE_URL = "aHR0cHM6Ly93d3cuc2xobm5haS5jYw";
    private static final String VOD_SITE = "aHR0cHM6Ly9oanZpZGVvLmNvbQ";
    private static final String DATA_BASE = "W1siaG90Iiwi6L+R5pyf54Ot6ZeoIl0sWyJ0b2RheSIsIuS7iuaXpeabtOaWsCJdLFsibHVhbmx1biIsIua1t+inkuS5seS8piJdLFsiZG9uZ21hbiIsIua1t+inkuWKqOa8qyJdLFsidGFuaHVhIiwi5rW36KeS5o6i6IqxIl0sWyJrYW5waWFuIiwi5rW36KeS55yL54mHIl0sWyJjaGlndWEiLCLmtbfop5LlkIPnk5wiXV0";
    private static final String DATA_CATE = "eyJsdWFubHVuIjpbWyLmr43lrZDkubHkvKYiLCJtdXppLWx1YW5sdW4iXSxbIuWFhOWmueS5seS8piIsInhpb25nbWVpLWx1YW5sdW4iXSxbIuWnkOW8n+S5seS8piIsImppZWRpLWx1YW5sdW4iXSxbIueItuWls+S5seS8piIsImZ1bnYtbHVhbmx1biJdXSwiZG9uZ21hbiI6W1si5LiN6ZmQIiwiaGFpamlhby1kb25nbWFuIl0sWyLlkIzkurpI5ryrIiwidG9uZ3Jlbi1obWFuJTIwJTIwIl0sWyLph4znlarliqjmvKsiLCIlMjBsaWZhbi1kb25nbWFuJTIwJTIwIl1dLCJ0YW5odWEiOltbIuS4jemZkCIsImhhaWppYW8tdGFodWEiXSxbIuaOouiKseeyvumAiSIsIiUyMHRhbmh1YS1qaW5neHVhbiUyMCUyMCJdLFsi5bCP5a6d5o6i6IqxIiwieGlhb2Jhby10YW5odWElMjAlMjAiXSxbIueYpueMtOaOouiKsSIsIiUyMHNob3Vob3UtdGFuaHVhJTIwJTIwIl0sWyLliKnlk6XmjqLoirEiLCIlMjBsaWdlLXRhbmh1YSUyMCUyMCJdLFsi6LW15oC75a+76IqxIiwiJTIwemhhb3pvbmcteHVuaHVhJTIwJTIwIl1dLCJrYW5waWFuIjpbWyLkuI3pmZAiLCJoYWlqaWFvLWthbnBpYW4iXSxbIuasp+e+juWkp+eJhyIsIm91bWVpLWRhcGlhbiUyMCUyMCJdLFsi5Zu95Lqn5Ymn5oOFIiwiJTIwZ3VvY2hhbi1qdXFpbmclMjAlMjAiXSxbIuS4reaWh+Wtl+W5lSIsIiUyMHpob25nd2VuLXppbXUlMjAlMjAiXSxbIuaXoOeggemrmOa4hSIsInd1bWEtZ2FvcWluZyUyMCUyMCJdXSwiY2hpZ3VhIjpbWyLkuI3pmZAiLCJoYWlqaWFvLWNnIl0sWyLmmI7mmJ/pu5HmlpkiLCJtaW5neGluZy1oZWlsaWFvJTIwJTIwIl0sWyLlsJHlpofkurrlprsiLCIlMjBzaGFvZnUtcmVucWklMjAlMjAiXSxbIueDremXqOWkp+eTnCIsInJlbWVuLWRhZ3VhJTIwJTIwIl0sWyLnvZHnuqLpu5HmlpkiLCJ3YW5naG9uZy1oZWlsaWFvJTIwJTIwIl1dfQ";

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

    private List<Vod> parseVods(Document doc) {
        List<Vod> list = new ArrayList<>();
        List<Future<Vod>> futures = new ArrayList<>();

        for (Element article : doc.select("article")) {
            Element post = article.select("a[class*=line-clamp], a:has(.dx-title)").first();
            if (post == null || post.toString().contains("data-ad_id")) continue;
            String link = post.attr("href").trim();
            if (link.isEmpty()) continue;
            String name = post.text();
            String id = CommonUtil.normalizeId(link);
            Element img = article.selectFirst("img[data-src]");
            String image = img == null ? "" : img.attr("data-src").trim();

            futures.add(EXECUTOR.submit(() -> {
                String base64Img = image.isEmpty() ? BaseUtil.ALIVIDEO : DecImgUtil.loadBackgroundImage(image, true);
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
            String m3u8Url = CommonUtil.videoM3u8Url(data, detailUrl);
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
        return Result.string(items, parseVods(doc), filters);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        int page = Integer.parseInt(pg);
        String path = Objects.equals(tid, "hot") || Objects.equals(tid, "today")
                ? "/sort/" + tid + "/page/" + pg + "/"
                : "/category/" + cate(extend.getOrDefault("cateId", ""), tid) + "/page/" + pg + "/";
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
        String year = doc.select("meta[property=article:published_time]").attr("content");
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
        String path = ("/search/" + URLEncoder.encode(key, "UTF-8") + "/");
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
