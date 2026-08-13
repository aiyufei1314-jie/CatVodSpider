package com.github.catvod.utils;

import com.github.catvod.net.OkHttp;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {

    public static String dateToString(String isoDate, String pattern) {
        OffsetDateTime odt = OffsetDateTime.parse(isoDate);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return odt.format(formatter);
    }

    public static String dateToString(String isoDate) {
        return dateToString(isoDate, "yyyy年MM月dd日");
    }

    public static String normalizeId(String link) {
        if (link.startsWith("http")) {
            int schemeEndIndex = link.indexOf("://") + 3;
            int pathStartIndex = link.indexOf('/', schemeEndIndex);
            if (pathStartIndex != -1) link = link.substring(pathStartIndex);
        }
        return link.startsWith("/") ? link : "/" + link;
    }

    public static Map<String, String[][]> parseConfig(String extendJson) {
        Map<String, String[][]> config = new LinkedHashMap<>();
        try {
            JsonReader reader = new JsonReader(new StringReader(extendJson));
            reader.beginObject();
            while (reader.hasNext()) {
                String key = reader.nextName();
                reader.beginArray();
                List<String[]> subCases = new ArrayList<>();
                while (reader.hasNext()) {
                    reader.beginArray();
                    String a = reader.nextString();
                    String b = reader.hasNext() ? reader.nextString() : "";
                    reader.endArray();
                    subCases.add(new String[]{a, b});
                }
                reader.endArray();
                config.put(key, subCases.toArray(new String[0][]));
            }
            reader.endObject();
            reader.close();
        } catch (IOException e) {
            // ignore
        }
        return config;
    }

    public static String missM3u8Url(String script, String baseUrl) {
        try {
            String originBaseUrl = OkHttp.extractBaseUrl(baseUrl);
            HashMap<String, String> headers = new HashMap<>();
            headers.put("User-Agent", BaseUtil.CHROME);
            headers.put("Origin", originBaseUrl);
            headers.put("Referer", baseUrl);

            Pattern p0 = Pattern.compile("eval(\\(function.*\\))");
            Matcher m0 = p0.matcher(script);
            if (!m0.find()) return "";

            String realJs = UnpackUtil.unpack(m0.group(1));
            if (realJs == null) return "";

            Pattern p6 = Pattern.compile("'api':'([^']+)'.*'key':'([^']+)','iv':'([^']+)'");
            Matcher m6 = p6.matcher(realJs);
            if (!m6.find()) return "";

            String apiUrl = originBaseUrl + m6.group(1);
            JsonObject dataConfig = JsonParser.parseString(OkHttp.string(apiUrl, headers)).getAsJsonObject();
            if (dataConfig.isEmpty()) return "";
            String result = CryptoUtil.aesDecrypt(dataConfig.get("data").getAsString(), m6.group(2), m6.group(3), false);
            return CryptoUtil.base64ToString(result);

        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    public static String videoM3u8Url(String script, String baseUrl) {
        try {
            String originBaseUrl = OkHttp.extractBaseUrl(baseUrl);
            HashMap<String, String> headers = new HashMap<>();
            headers.put("User-Agent", BaseUtil.CHROME);
            headers.put("Origin", originBaseUrl);
            headers.put("Referer", baseUrl);

            String realJs = UnpackUtil.unpack(script);
            if (realJs == null) return "";

            Pattern m6 = Pattern.compile("src='([^']+)");
            Matcher matcher6 = m6.matcher(realJs);
            if (!matcher6.find()) return "";

            String apiUrl = originBaseUrl + matcher6.group(1) + System.currentTimeMillis() / 1000 / 2000;
            String dataJS = OkHttp.string(apiUrl, headers);
            if (dataJS.isEmpty()) return "";

            String m3u8Js = UnpackUtil.unpack(dataJS);
            if (m3u8Js == null) return "";
            m3u8Js = m3u8Js.replaceAll("\\\\", "").replaceAll("\\s", "");

            Pattern m9 = Pattern.compile("\"url\":\"([^\"]+)");
            Matcher matcher9 = m9.matcher(m3u8Js);
            return matcher9.find() ? matcher9.group(1) : "";
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    public static String blueM3u8Url(String script, String baseUrl) {
        try {
            String originBaseUrl = OkHttp.extractBaseUrl(baseUrl);
            HashMap<String, String> headers = new HashMap<>();
            headers.put("User-Agent", BaseUtil.CHROME);
            headers.put("Origin", originBaseUrl);
            headers.put("Referer", baseUrl);

            String dplayerJs = UnpackUtil.unpack(script);
            if (dplayerJs == null) return "";

            dplayerJs = dplayerJs.replaceAll("\\\\", "").replaceAll("\\s", "");
            Pattern p6 = Pattern.compile("src='(.*?)'");
            Matcher m6 = p6.matcher(dplayerJs);
            if (!m6.find()) return "";

            String detailUrl = originBaseUrl + m6.group(1) + System.currentTimeMillis() / 1000 / 2000 + ".js";
            String dataConfigJS = OkHttp.string(detailUrl, headers);
            if (dataConfigJS.isEmpty()) return "";

            String realJs = UnpackUtil.unpack(dataConfigJS);
            if (realJs == null) return "";

            Pattern m9 = Pattern.compile("data-config='(.*?)'");
            Matcher matcher9 = m9.matcher(realJs);
            if (!matcher9.find()) return "";

            String dataConfig = CryptoUtil.base64ToString(matcher9.group(1));
            JsonObject dataRoot = JsonParser.parseString(dataConfig).getAsJsonObject();
            JsonObject dataVideo = dataRoot.getAsJsonObject("video");
            if (dataVideo == null) return "";

            return dataVideo.get("url").getAsString();
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    public static String tongM3u8Url(String script, String baseUrl) {
        try {
            String originBaseUrl = OkHttp.extractBaseUrl(baseUrl);
            HashMap<String, String> headers = new HashMap<>();
            headers.put("User-Agent", BaseUtil.CHROME);
            headers.put("Origin", originBaseUrl);
            headers.put("Referer", baseUrl);

            String uParam = getPlayerJS(script);
            if (uParam.isEmpty()) return "";
            String detailUrl = originBaseUrl + uParam + "&t=" + System.currentTimeMillis() / 1000 / 1800;

            String m3u8JS = OkHttp.string(detailUrl, headers);
            if (m3u8JS.isEmpty()) return "";

            String realJs = UnpackUtil.unpack(m3u8JS);
            if (realJs == null || realJs.isEmpty()) return "";
            realJs = realJs.replaceAll("\\\\", "").replaceAll("\\s", "");

            Pattern p6 = Pattern.compile("data-api=\"([^\"]*)\"data-key=\"([^\"]*)\"data-iv=\"([^\"]*)\"");
            Pattern p9 = Pattern.compile("data-url=\"([^\"]*)\"");

            Matcher m6 = p6.matcher(realJs);
            if (!m6.find()) {
                Matcher m9 = p9.matcher(realJs);
                return m9.find() ? m9.group(1) : "";
            } else {
                String apiUrl = originBaseUrl + m6.group(1);
                JSONObject object = new JSONObject(OkHttp.string(apiUrl, headers));
                String data6 = object.optString("data");
                if (data6.isEmpty()) return "";
                String result = CryptoUtil.aesDecrypt(data6, m6.group(2), m6.group(3), false);
                return CryptoUtil.base64ToString(result);
            }

        } catch (Exception e) {
            // ignore
        }
        return "";
    }

    public static String getPlayerJS(String script) {
        String realJs = UnpackUtil.unpack(script);
        if (realJs == null || realJs.isEmpty()) return "";
        realJs = realJs.replaceAll("\\\\", "").replaceAll("\\s", "");

        Pattern p6 = Pattern.compile("\\+encodeURIComponent\\(\"(.*?)\"\\)\\+");
        Matcher m6 = p6.matcher(realJs);
        if (!m6.find()) return "";
        String uParam = m6.group(1);
        uParam = uParam.replace("+", "%2B")
                .replace("/", "%2F")
                .replace("=", "%3D");

        Pattern p9 = Pattern.compile("src=\"(.*?)\\?");
        Matcher m9 = p9.matcher(realJs);
        if (!m9.find()) return "";
        String urlPath = m9.group(1);

        return urlPath + "?u=" + uParam;
    }

    public static boolean hasNextPage(Document doc, String path) {
        String base = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        int lastSlash = base.lastIndexOf('/');
        String nextPagePath = null;
        try {
            String prefix = base.substring(0, lastSlash + 1);
            String numStr = base.substring(lastSlash + 1);
            nextPagePath = prefix + (Integer.parseInt(numStr) + 1);
        } catch (Exception ignored) {
        }
        Elements links = doc.select("a[href]");
        for (Element link : links) {
            String href = link.attr("href");
            if (nextPagePath != null && href.replaceAll("\\s", "%20").contains(nextPagePath)) {
                return true;
            }
            String text = link.text();
            if (text.trim().equals("下一页")) {
                return true;
            }
        }
        return false;
    }

    public static String extractParagraphs(Elements paragraphs, String[] keywords) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Element p : paragraphs) {
                if (!p.select("a, div, strong").isEmpty()) continue;
                String text = p.text();
                if (text.trim().isEmpty()) continue;
                boolean shouldSkip = false;
                for (String keyword : keywords) {
                    if (text.contains(keyword)) {
                        shouldSkip = true;
                        break;
                    }
                }
                if (shouldSkip) continue;
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(text);
            }
            return sb.toString();
        } catch (Exception e) {
            // ignore
        }
        return "";
    }

}
