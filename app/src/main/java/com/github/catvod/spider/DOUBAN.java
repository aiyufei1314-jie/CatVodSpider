package com.github.catvod.spider;

import android.content.Context;
import android.text.TextUtils;

import com.github.catvod.bean.Class;
import com.github.catvod.bean.Result;
import com.github.catvod.bean.Vod;
import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;
import com.github.catvod.utils.DouBanData;
import com.github.catvod.utils.BaseUtil;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.StringReader;
import java.net.URLEncoder;
import java.util.*;

public class DOUBAN extends Spider {

    private final String siteUrl = "https://frodo.douban.com/api/v2";
    private final String apikey = "?apikey=0ac44ae016490db2204ce0a042db2916";
    private static final int PAGE_SIZE = 32;
    private String extend;

    private Map<String, String> getHeader() {
        Map<String, String> header = new HashMap<>();
        header.put("Host", "frodo.douban.com");
        header.put("Connection", "Keep-Alive");
        header.put("Referer", "https://servicewechat.com/wx2f9b06c1de1ccfca/99/page-frame.html");
        header.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36 MicroMessenger/7.0.20.1781 NetType/WIFI MiniProgramEnv/Windows WindowsWechat/WMPF WindowsWechat UnifiedPCWindowsWechat XWEB/19841");
        return header;
    }

    @Override
    public void init(Context context, String extend) throws Exception {
        this.extend = buildConfig();
    }

    private String buildConfig() {
        String movie = arrayOf(DouBanData.MOVIE_TYPE, DouBanData.MOVIE_AREA, DouBanData.COMMON_YEAR, DouBanData.COMMON_SORT);
        String tv = arrayOf(DouBanData.TV_TYPE, DouBanData.TV_AREA, DouBanData.TV_FORM, DouBanData.COMMON_YEAR, DouBanData.COMMON_SORT);
        String show = arrayOf(DouBanData.SHOW_TYPE, DouBanData.TV_AREA, DouBanData.TV_FORM, DouBanData.COMMON_YEAR, DouBanData.COMMON_SORT);
        String obscure = arrayOf(DouBanData.OBSCURE_AREA);
        return "{"
                + "\"movie\":" + movie + ","
                + "\"tv\":" + tv + ","
                + "\"show\":" + show + ","
                + "\"anime\":" + movie + ","
                + "\"documentary\":" + movie + ","
                + "\"short\":" + movie + ","
                + "\"comic\":" + tv + ","
                + "\"obscure\":" + obscure + ","
                + "\"rating\":" + obscure
                + "}";
    }

    private String arrayOf(String... items) {
        return "[" + String.join(",", items) + "]";
    }

    @Override
    public String homeContent(boolean filter) throws Exception {

        List<Class> classes = new ArrayList<>();
        String[][] typePairs = {
                {"movie", "电影"},
                {"tv", "电视剧"},
                {"anime", "动画电影"},
                {"comic", "动画动漫"},
                {"documentary", "纪录片"},
                {"show", "综艺"},
                {"short", "短片"},
                {"rating", "豆瓣高分"},
                {"obscure", "冷门佳片"}
        };
        for (String[] pair : typePairs) {
            classes.add(new Class(pair[0], pair[1]));
        }

        String recommendUrl = siteUrl + "/movie/suggestion" + apikey + "&start=0&count=48&new_struct=1&with_review=1";
        return Result.string(classes, parseVods(OkHttp.string(recommendUrl, getHeader())), extend);
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        String sort = extend.getOrDefault("sort", "U");
        int start = (Integer.parseInt(pg) - 1) * PAGE_SIZE;

        String baseUrl;
        String defaultTag = "";
        String defaultType = "全部";

        if (Arrays.asList("movie", "anime", "documentary", "short").contains(tid)) {
            baseUrl = siteUrl + "/movie/recommend" + apikey;
            if (tid.equals("anime")) defaultTag = "动画";
            else if (tid.equals("documentary")) defaultTag = "纪录片";
            else if (tid.equals("short")) defaultTag = "短片";
        } else {
            baseUrl = siteUrl + "/tv/recommend" + apikey;
            if (tid.equals("comic")) defaultTag = "动画";
            else if (tid.equals("show")) defaultTag = "综艺";
        }

        StringBuilder tags = new StringBuilder();
        if (!TextUtils.isEmpty(defaultTag)) {
            tags.append(defaultTag);
        }
        for (String key : extend.keySet()) {
            if ("sort".equals(key)) continue;
            String value = extend.get(key);
            if (!TextUtils.isEmpty(value) && !value.contains("全部") && !value.contains("不限")) {
                if (tags.length() > 0) tags.append(",");
                tags.append(value);
            }
        }

        String cateUrl;
        if (Objects.equals(tid, "rating")) {
            String area = extend.get("area");
            if (!TextUtils.isEmpty(area) && !"全部".equals(area)) {
                defaultType = area;
            }
            cateUrl = siteUrl + "/subject/recent_hot/movie" + apikey
                    + "&category=%E8%B1%86%E7%93%A3%E9%AB%98%E5%88%86"
                    + "&type=" + URLEncoder.encode(defaultType, "UTF-8")
                    + "&start=" + start + "&count=" + PAGE_SIZE;
        } else if (Objects.equals(tid, "obscure")) {
            String area = extend.get("area");
            if (!TextUtils.isEmpty(area) && !"全部".equals(area)) {
                defaultType = area;
            }
            cateUrl = siteUrl + "/subject/recent_hot/movie" + apikey
                    + "&category=%E5%86%B7%E9%97%A8%E4%BD%B3%E7%89%87"
                    + "&type=" + URLEncoder.encode(defaultType, "UTF-8")
                    + "&start=" + start + "&count=" + PAGE_SIZE;
        } else {
            cateUrl = baseUrl + "&sort=" + sort
                    + "&tags=" + URLEncoder.encode(tags.toString(), "UTF-8")
                    + "&start=" + start + "&count=" + PAGE_SIZE;
        }
        List<Vod> list = parseVods(OkHttp.string(cateUrl, getHeader()));

        int page = Integer.parseInt(pg);
        return Result.string(page, 0, PAGE_SIZE, 0, list);
    }

    private List<Vod> parseVods(String json) {
        List<Vod> list = new ArrayList<>();
        try {
            JsonReader reader = new JsonReader(new StringReader(json));
            reader.beginObject();
            while (reader.hasNext()) {
                if (!"items".equals(reader.nextName())) {
                    reader.skipValue();
                    continue;
                }
                reader.beginArray();
                while (reader.hasNext()) {
                    Vod vod = readVod(reader);
                    if (vod != null) list.add(vod);
                }
                reader.endArray();
                reader.close();
                return list;
            }
            reader.close();
        } catch (Exception e) {
            // ignore
        }
        return list;
    }

    private Vod readVod(JsonReader reader) throws java.io.IOException {
        reader.beginObject();
        String id = "", title = "", pic = "", rating = "";
        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "id":
                    id = readValue(reader);
                    break;
                case "title":
                    title = readValue(reader);
                    break;
                case "pic":
                    pic = readNested(reader, "normal");
                    break;
                case "rating":
                    rating = readNested(reader, "value");
                    break;
                default:
                    reader.skipValue();
            }
        }
        reader.endObject();
        if (title.isEmpty()) return null;
        if (title.contains("高分经典") || title.contains("评分最高")) return null;
        String picUrl = pic.isEmpty()
                ? BaseUtil.ALIVIDEO
                : pic + "@Referer=https://api.douban.com/@User-Agent=" + BaseUtil.CHROME;
        String remark = rating.isEmpty() ? "" : "豆瓣" + rating + "分";
        return new Vod("msearch:" + id, title, picUrl, remark);
    }

    private String readValue(JsonReader reader) throws java.io.IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.skipValue();
            return "";
        }
        return reader.nextString();
    }

    private String readNested(JsonReader reader, String key) throws java.io.IOException {
        String value = "";
        if (reader.peek() != JsonToken.NULL) {
            reader.beginObject();
            while (reader.hasNext()) {
                if (key.equals(reader.nextName())) {
                    value = readValue(reader);
                } else {
                    reader.skipValue();
                }
            }
            reader.endObject();
        } else {
            reader.skipValue();
        }
        return value;
    }

}