package com.github.catvod.spider;

import android.text.TextUtils;

import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * MSO网盘搜索站爬虫
 * 站点: https://mso.moepal.top/
 * 接口: POST https://mso.moepal.top/api/search
 *
 * 使用说明:
 * 1. 将本文件放入 CatVodSpider 工程的 com.github.catvod.spider 包下
 * 2. 编译打包生成 jar
 * 3. 影视仓配置:
 *    {
 *      "key": "mso",
 *      "name": "MSO网盘搜索",
 *      "type": 3,
 *      "api": "csp_Mso",
 *      "searchable": 1,
 *      "quickSearch": 1,
 *      "spider": "clan://localhost/Download/mso.jar"
 *    }
 *
 * 仅用于个人学习测试
 */
public class csp_Mso extends Spider {

    private static final String API_URL = "https://mso.moepal.top/api/search";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    // 网盘类型映射
    private static final Map<String, String> CLOUD_NAMES = new LinkedHashMap<>();
    static {
        CLOUD_NAMES.put("quark", "夸克");
        CLOUD_NAMES.put("ali", "阿里云盘");
        CLOUD_NAMES.put("aliyundrive", "阿里云盘");
        CLOUD_NAMES.put("aliyun", "阿里云盘");
        CLOUD_NAMES.put("alipan", "阿里云盘");
        CLOUD_NAMES.put("baidu", "百度网盘");
        CLOUD_NAMES.put("xunlei", "迅雷网盘");
        CLOUD_NAMES.put("uc", "UC网盘");
        CLOUD_NAMES.put("189", "天翼云盘");
        CLOUD_NAMES.put("cloud189", "天翼云盘");
        CLOUD_NAMES.put("cili", "磁力");
        CLOUD_NAMES.put("magnet", "磁力");
        CLOUD_NAMES.put("123pan", "123盘");
        CLOUD_NAMES.put("pikpak", "PikPak");
    }

    // 轮询配置
    private static final int POLL_INTERVAL_MS = 2000;  // 轮询间隔2秒
    private static final int MAX_POLL_COUNT = 10;       // 最多轮询10次（约20秒）
    private static final int STABLE_THRESHOLD = 3;      // 结果连续3次不变则提前结束

    @Override
    public String homeContent(boolean filter) throws Exception {
        // 没有首页分类，返回空
        JSONObject result = new JSONObject();
        result.put("class", new JSONArray());
        return result.toString();
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        // 没有分类页，返回空
        JSONObject result = new JSONObject();
        result.put("page", pg);
        result.put("pagecount", 0);
        result.put("limit", 0);
        result.put("total", 0);
        result.put("list", new JSONArray());
        return result.toString();
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        // 用LinkedHashMap去重，保持插入顺序
        LinkedHashMap<String, JSONObject> allResults = new LinkedHashMap<>();
        int lastCount = 0;
        int stableCount = 0;

        // 第一次搜索：refresh=true 触发新搜索
        JSONObject firstData = requestSearch(key, true);
        if (firstData != null) {
            parseAndMerge(firstData, allResults);
        }

        // 渐进式轮询
        for (int i = 0; i < MAX_POLL_COUNT; i++) {
            Thread.sleep(POLL_INTERVAL_MS);

            JSONObject data = requestSearch(key, false);
            if (data == null) continue;

            parseAndMerge(data, allResults);

            // 判断结果是否稳定
            if (allResults.size() == lastCount && allResults.size() > 0) {
                stableCount++;
                if (stableCount >= STABLE_THRESHOLD) {
                    break; // 结果稳定，提前结束
                }
            } else {
                stableCount = 0;
            }
            lastCount = allResults.size();
        }

        // 组装影视仓标准格式
        JSONArray list = new JSONArray();
        for (JSONObject vod : allResults.values()) {
            list.put(vod);
        }

        JSONObject result = new JSONObject();
        result.put("list", list);
        return result.toString();
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        // ids.get(0) 是我们在搜索时编码的详情信息JSON
        String id = ids.get(0);
        JSONObject info = new JSONObject(id);

        String cloudType = info.optString("t", "");
        String url = info.optString("u", "");
        String password = info.optString("p", "");
        String title = info.optString("n", "资源");
        String cloudName = CLOUD_NAMES.getOrDefault(cloudType, cloudType);

        // 拼装播放地址：网盘名$链接$$提取码
        String playUrl = cloudName + "$" + url;
        if (!TextUtils.isEmpty(password)) {
            playUrl = playUrl + "$$" + password;
        }

        JSONObject vod = new JSONObject();
        vod.put("vod_id", id);
        vod.put("vod_name", title);
        vod.put("vod_pic", "");
        vod.put("vod_content", "来源：" + cloudName + "\n链接：" + url + "\n提取码：" + (TextUtils.isEmpty(password) ? "无" : password));
        vod.put("vod_play_from", cloudName);
        vod.put("vod_play_url", playUrl);

        JSONArray list = new JSONArray();
        list.put(vod);

        JSONObject result = new JSONObject();
        result.put("list", list);
        return result.toString();
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        // 网盘链接由影视仓内置播放器处理，这里直接返回链接
        JSONObject result = new JSONObject();
        result.put("parse", 0);
        result.put("url", id);
        return result.toString();
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 发起一次搜索请求
     */
    private JSONObject requestSearch(String keyword, boolean refresh) {
        try {
            JSONObject body = new JSONObject();
            body.put("wd", keyword);
            body.put("res", "merge");
            body.put("src", "all");
            body.put("refresh", refresh);
            body.put("cloud_types", new JSONArray());

            RequestBody requestBody = RequestBody.create(body.toString(), JSON);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            headers.put("Referer", "https://mso.moepal.top/");
            headers.put("Origin", "https://mso.moepal.top");

            String resp = OkHttp.post(API_URL, requestBody, headers);
            if (TextUtils.isEmpty(resp)) return null;

            JSONObject root = new JSONObject(resp);
            if (root.optInt("code", -1) != 0 && root.optInt("code", -1) != 200) {
                return null;
            }
            return root.optJSONObject("data");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 解析返回数据并合并到结果集
     */
    private void parseAndMerge(JSONObject data, LinkedHashMap<String, JSONObject> results) {
        if (data == null) return;

        // merged_by_type 是按网盘类型分组的对象
        JSONObject mergedByType = data.optJSONObject("merged_by_type");
        if (mergedByType == null) {
            // 兼容旧格式：直接在data下按网盘类型分组
            mergedByType = data;
        }

        JSONArray types = mergedByType.names();
        if (types == null) return;

        for (int i = 0; i < types.length(); i++) {
            String cloudType = types.optString(i);
            JSONArray items = mergedByType.optJSONArray(cloudType);
            if (items == null) continue;

            String cloudName = CLOUD_NAMES.getOrDefault(cloudType, cloudType);

            for (int j = 0; j < items.length(); j++) {
                JSONObject item = items.optJSONObject(j);
                if (item == null) continue;

                String title = item.optString("note", "");
                if (TextUtils.isEmpty(title)) title = item.optString("work_title", "");
                if (TextUtils.isEmpty(title)) title = item.optString("title", "");
                if (TextUtils.isEmpty(title)) title = item.optString("url", "未命名资源");

                String url = item.optString("url", "");
                String password = item.optString("password", "");
                String datetime = item.optString("datetime", "");

                // 详情信息编码为JSON作为vod_id
                JSONObject detailInfo = new JSONObject();
                detailInfo.put("t", cloudType);
                detailInfo.put("u", url);
                detailInfo.put("p", password);
                detailInfo.put("n", title);
                String vodId = detailInfo.toString();

                // 去重key：网盘类型+链接
                String dedupKey = cloudType + "|" + url;
                if (results.containsKey(dedupKey)) continue;

                // 拼装备注
                StringBuilder remark = new StringBuilder(cloudName);
                if (!TextUtils.isEmpty(password)) {
                    remark.append(" · 密码:").append(password);
                }
                if (!TextUtils.isEmpty(datetime) && !datetime.startsWith("0001")) {
                    remark.append(" · ").append(datetime.substring(0, Math.min(10, datetime.length())));
                }

                JSONObject vod = new JSONObject();
                vod.put("vod_id", vodId);
                vod.put("vod_name", title);
                vod.put("vod_pic", "");
                vod.put("vod_remarks", remark.toString());

                results.put(dedupKey, vod);
            }
        }
    }
}
