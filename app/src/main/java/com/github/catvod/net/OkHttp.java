package com.github.catvod.net;

import com.github.catvod.crawler.Spider;
import com.github.catvod.utils.BaseUtil;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import okhttp3.*;

public class OkHttp {

    public static final String POST = "POST";
    public static final String GET = "GET";
    public static final long DEFAULT_TIMEOUT = 30;

    private static volatile OkHttpClient defaultClient;
    private static volatile OkHttpClient noRedirectClient;

    private OkHttp() {
    }

    public static Response newCall(Request request) throws IOException {
        return client().newCall(request).execute();
    }

    public static Response newCall(String url) throws IOException {
        return client().newCall(new Request.Builder().url(url).build()).execute();
    }

    public static Response newCall(String url, String tag) throws IOException {
        return client().newCall(new Request.Builder().url(url).tag(tag).build()).execute();
    }

    public static Response newCall(String url, Map<String, String> header) throws IOException {
        return client().newCall(new Request.Builder().url(url).headers(Headers.of(header)).build()).execute();
    }

    private static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("User-Agent", BaseUtil.CHROME);
        return headers;
    }

    public static String string(String url) {
        return string(url, getHeaders());
    }

    public static String string(String url, long timeout) {
        return string(url, null, getHeaders(), timeout);
    }

    public static String string(String url, Map<String, String> header) {
        return string(url, null, header);
    }

    public static String string(String url, Map<String, String> params, Map<String, String> header) {
        return string(url, params, header, DEFAULT_TIMEOUT);
    }

    public static String string(String url, Map<String, String> params, Map<String, String> header, long timeout) {
        if (!url.startsWith("http")) return "";
        OkRequest request = new OkRequest(GET, url, params, header);
        try (OkResult result = request.execute(client(timeout))) {
            return result.getBody();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String post(String url, Map<String, String> params) {
        return post(url, params, null).getBody();
    }

    public static OkResult post(String url, Map<String, String> params, Map<String, String> header) {
        OkRequest request = new OkRequest(POST, url, params, header);
        return request.execute(client());
    }

    public static String post(String url, String json) {
        return post(url, json, null).getBody();
    }

    public static OkResult post(String url, String json, Map<String, String> header) {
        OkRequest request = new OkRequest(POST, url, json, header);
        return request.execute(client());
    }

    public static String getLocation(String url, Map<String, String> header) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        if (header != null && !header.isEmpty()) {
            builder.headers(Headers.of(header));
        }
        Request request = builder.build();

        try (Response response = noRedirectClient().newCall(request).execute()) {
            String location = response.header("Location");
            if (location == null) {
                location = response.header("location");
            }
            return location;
        }
    }

    public static String getLocation(Map<String, List<String>> headers) {
        if (headers == null) return null;
        if (headers.containsKey("location")) return headers.get("location").get(0);
        if (headers.containsKey("Location")) return headers.get("Location").get(0);
        return null;
    }

    public static void cancel(String tag) {
        cancel(client(), tag);
    }

    public static void cancel(OkHttpClient client, String tag) {
        if (client == null) return;
        for (Call call : client.dispatcher().queuedCalls()) if (tag.equals(call.request().tag())) call.cancel();
        for (Call call : client.dispatcher().runningCalls()) if (tag.equals(call.request().tag())) call.cancel();
    }

    public static void cancelAll() {
        cancelAll(client());
    }

    public static void cancelAll(OkHttpClient client) {
        if (client == null) return;
        client.dispatcher().cancelAll();
    }

    private static OkHttpClient client() {
        try {
            OkHttpClient custom = Spider.client();
            if (custom != null) {
                return custom;
            }
        } catch (Throwable ignored) {
            // fall through
        }
        return getDefaultClient();
    }

    private static OkHttpClient client(long timeout) {
        return client().newBuilder()
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .build();
    }

    public static OkHttpClient noRedirectClient() {
        OkHttpClient client = noRedirectClient;
        if (client == null) {
            synchronized (OkHttp.class) {
                client = noRedirectClient;
                if (client == null) {
                    client = getDefaultClient().newBuilder()
                            .followRedirects(false)
                            .followSslRedirects(false)
                            .build();
                    noRedirectClient = client;
                }
            }
        }
        return client;
    }

    private static OkHttpClient getDefaultClient() {
        OkHttpClient client = defaultClient;
        if (client == null) {
            synchronized (OkHttp.class) {
                client = defaultClient;
                if (client == null) {
                    client = buildClient();
                    defaultClient = client;
                }
            }
        }
        return client;
    }

    private static OkHttpClient buildClient() {
        return new OkHttpClient.Builder()
                .dns(safeDns())
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .hostnameVerifier((hostname, session) -> true)
                .sslSocketFactory(new SSLCompat(), SSLCompat.TM)
                .build();
    }

    public static String extractBaseUrl(String url) {
        if (url == null) return "";
        HttpUrl parsed = HttpUrl.parse(url);
        if (parsed == null) return "";
        String scheme = parsed.scheme();
        String host = parsed.host();
        int port = parsed.port();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
            return scheme + "://" + host;
        } else {
            return scheme + "://" + host + ":" + port;
        }
    }

    private static Dns safeDns() {
        try {
            Dns dns = Spider.safeDns();
            if (dns != null) {
                return dns;
            }
        } catch (Throwable ignored) {
        }
        return Dns.SYSTEM;
    }
}
