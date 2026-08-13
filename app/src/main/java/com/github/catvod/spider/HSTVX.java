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

import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

public class HSTVX extends Spider {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    private static final String SITE_URL = "aHR0cHM6Ly93d3cuZWlkcHB4di5jb20";
    private static final String VOD_SITE = "aHR0cHM6Ly9oc3R2eC5jb20";
    private static final String DATA_BASE = "W1siYWxsIiwi6Imy5oOF5Lit5b+DIl0sWyIxNjAiLCLkvJrmiYDmjqLoirEiXSxbIjE2NiIsIuS5seS8puaNouWmuyJdLFsiMTY1Iiwi5Lit5paH5a2X5bmVIl0sWyIxNjQiLCLmrKfnvo7lt6jmoLkiXSxbIjE2MSIsIum7keaWmeWQg+eTnCJdLFsiMTU3Iiwi5riv5Y+w5LiJ57qnIl0sWyIxNTgiLCLml6Xpn6nkuInnuqciXSxbIjE1OSIsIuasp+e+juS4iee6pyJdLFsiMTY5Iiwi56aB5ryr5aSp5aCCIl0sWyIxNjciLCLmiJDkurrnu7zoiboiXSxbIjE3MCIsIuWbveS6p+eyvumAiSJdLFsiMTM5IiwiOTHliLbniYfljoIiXSxbIjE0MSIsIuWFtuS7luS8oOWqkiJdLFsiMTQwIiwi5p6c5Ya75Lyg5aqSIl0sWyIxNDIiLCLpurvosYbkvKDlqpIiXSxbIjE2OCIsIuaXoOeggSJdLFsiNTIiLCLkuprmtLLkuroiXSxbIjE2MyIsIuS4nOWNl+S6miJdLFsiMSIsIuWPo+S6pCJdLFsiMTYyIiwi56aP5Yip5aesIl0sWyI5MSIsIuWNsOW6puS6uiJdLFsiMTcyIiwi6auY5riF6Imy5oOF54mHIl0sWyIxOTEiLCLohLHooaPoiJ4iXSxbIjE5MCIsIuWPjOaAp+aBi+eUtyJdLFsiMTg3Iiwi6Z+z5LmQIl0sWyIxODUiLCLpm4bkvZPpopzlsIQiXSxbIjE3OSIsIueBq+i+o+S/neWnhiJdLFsiMTc4Iiwi55S35oCn6Ieq5oWwIl0sWyIxNzciLCLot6jmgKfliKsiXSxbIjE3NSIsIueJh+WcuuebtOWHuyJdLFsiMTc0IiwiOTHljp/liJsiXSxbIjIiLCLlpbPmgKfpq5jmva4iXSxbIjMiLCLlt6jkubMiXSxbIjQiLCLlt6jlsYwiXSxbIjE1Iiwi5Yqy54iG6YeN5Y+j5ZGzIl0sWyIxNyIsIuWls+aAp+iHquaFsCJdLFsiMjAiLCJDb3NwbGF5Il0sWyIyOSIsIjNQIl0sWyIzNCIsIuWwhOeyviJdLFsiMzciLCLkvanmiLTlvI/pmLPlhbciXSxbIjM4Iiwi5aWz5ZCMIl0sWyI0NiIsIuWPjOm+meWFpea0niJdLFsiNDgiLCLkv4Tlm73kuroiXSxbIjUxIiwi5YaF5bCE5Lit5Ye6Il0sWyIxNzMiLCLnq5blsY/op4bpopEiXSxbIjYwIiwi5YWs5LyX6YeO5oiYIl0sWyI2NyIsIjYw5binIl0sWyI2OCIsIuWls+aAp+S5i+mAiSJdLFsiNjkiLCLlkI3kuroiXSxbIjczIiwi5aSn5Y+3576O5aWzIl0sWyI4MSIsIuWoh+Wmu+WBt+WQgyJdLFsiODQiLCLlpKflraYiXSxbIjg1Iiwi6K6k6K+B5oOF5L6jIl0sWyI5OSIsIuWKqOa8q+WNoemAmiJdLFsiMTE2Iiwi6Jma5ouf546w5a6eIl0sWyIxNTUiLCJKVklEIl0sWyIxNTYiLCJTV0FHIl1d";
    private static final String DATA_CATE = "eyJhbGwiOltbIuacgOi/keabtOaWsCIsImxhdGVzdCJdLFsi5pyA5aSa5Zac5qyiIiwibW9zdC1mYXZvcml0ZWQiXSxbIuacgOWkmuingueciyIsIm1vc3Qtdmlld2VkIl0sWyLmnIDlpJrngrnotZ4iLCJtb3N0LWxpa2VkIl0sWyLmnIDlpJror4TorroiLCJtb3N0LWNvbW1lbnRlZCJdLFsi5pyA6ZW/5pe26Ze0IiwibG9uZ2VzdCJdXX0";

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

        for (Element video : doc.select("main.dx-container li:has(a.poster)")) {
            Element post = video.selectFirst("a[class*=line-clamp], h3 a[href]");
            if (post == null) post = video.select("a[href]").stream().filter(a -> !a.text().isEmpty()).findFirst().orElse(null);
            if (post == null || post.toString().contains("data-ad_id")) continue;
            String link = post.attr("href").trim();
            if (link.isEmpty()) continue;
            String name = post.text();
            String id = CommonUtil.normalizeId(link);
            Element img = video.selectFirst("img[data-src]");
            String image = img == null ? "" : img.attr("data-src").trim();

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

    private String extractPlayUrls(Document doc) {
        StringBuilder playUrl = new StringBuilder();
        int index = 1;
        for (Element element : doc.select("#videoPlayer")) {
            String m3u8Url = element.attr("src");
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

        LinkedHashMap<String, List<Filter>> filters = buildFilters(items);
        return Result.string(items, parseVods(doc), filters);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        int page = Integer.parseInt(pg);
        String path = Objects.equals(tid, "all")
                ? "/videos/" + cate(extend.getOrDefault("cateId", ""), "all") + "/page/" + pg
                : "/category/" + tid + "/page/" + pg;
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + path));
        return Result.string(page, pagerCount(doc, page), 0, 0, parseVods(doc));
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        String id = ids.get(0);
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + id));

        String name = doc.select("meta[property=og:title]").attr("content");
        String pic = doc.select("meta[property=og:image]").attr("content");
        String desc = doc.select("meta[property=og:description]").attr("content");
        String year = doc.select("meta[property=video:release_date]").attr("content");
        String playUrl = extractPlayUrls(doc);

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
        String path = ("/search/" + URLEncoder.encode(key, "UTF-8"));
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + path));
        return Result.string(parseVods(doc));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).chrome().string();
    }
}
