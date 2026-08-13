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

public class STGAY extends Spider {

    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final int PAGE_SIZE = 32;

    private static final String SITE_URL = "aHR0cHM6Ly93d3cuYm9zY2Z3dHZ4LmNvbQ";
    private static final String VOD_SITE = "aHR0cHM6Ly9zdGdheS5jb20";
    private static final String DATA_BASE = "W1siYWxsIiwi5pCc5ZCM57K+6YCJIl0sWyJuYW50b25nLXhpYW5yb3UiLCLnlLflkIzpspzogokiXSxbInNodWFpZ2UtZmVpamkiLCLluIXlk6Xpo57mnLoiXSxbInpoaWZ1LXlvdWh1byIsIuWItuacjeivseaDkSJdLFsiemh1bnUtdGlhb2ppYW8iLCLkuLvlpbTosIPmlZkiXSxbImppcm91LW1lbmduYW4iLCLogozogonnjJvnlLciXSxbInRvbmd4aW5nLUdtYW4iLCLlkIzmgKdH5ryrIl1d";
    private static final String DATA_CATE = "eyJhbGwiOltbIuato+WcqOaSreaUviIsInBsYXkiXSxbIuW9k+WJjeacgOeDrSIsImhvdCJdLFsi5pyA6L+R5pu05pawIiwibmV3Il0sWyIxMOWIhumSn+S7peS4iiIsInRlbiJdLFsiMjDliIbpkp/ku6XkuIoiLCJ0d2VudHkiXSxbIuavj+aciOacgOeDrSIsImV2ZXJ5bW9udGhob3QiXSxbIuacrOaciOaUtuiXjyIsIm1vbnRoY29sbGVjdCJdLFsi5pS26JeP5pyA5aSaIiwibWF4Y29sbGVjdCJdLFsi5pys5pyI6K6o6K66IiwibW9udGhjb21tZW50Il1dfQ";

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
            if (link.isEmpty()) continue;
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
        String path = Objects.equals(tid, "all")
                ? "/videos/all/" + cate(extend.getOrDefault("cateId", ""), "all") + "/" + pg
                : "/videos/category/" + tid + "/" + pg;
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
        String year = doc.select("meta[property=video:release_date]").attr("content");
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
        String path = ("/videos/search/" + URLEncoder.encode(key, "UTF-8"));
        Document doc = Jsoup.parse(OkHttp.string(baseUrl + path));
        return Result.string(parseVods(doc, false));
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return Result.get().url(id).chrome().string();
    }
}
