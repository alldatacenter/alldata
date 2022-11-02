package com.alibaba.tesla.tkgone.server.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author yangjinghua
 */
public class Requests {

    private static OkHttpClient httpClient;
    private static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool())
                .readTimeout(600, TimeUnit.SECONDS)
                .writeTimeout(600, TimeUnit.SECONDS)
                .connectTimeout(600, TimeUnit.SECONDS)
                //.connectionPool(new ConnectionPool(5000, 5, TimeUnit.MINUTES))
                .build();
        }
        return httpClient;
    }

    public static JSONObject get(String url, JSONObject params) throws IOException {
        return get(url, params, null);
    }

    public static JSONObject get(String url, JSONObject params, JSONObject headers) throws IOException {
        return JSONObject.parseObject(getString(url, params, headers));
    }

    public static JSONArray getArray(String url, JSONObject params) throws IOException {
        String retString = getString(url, params, null);
        return JSONArray.parseArray(retString);
    }

    public static String getString(String url, JSONObject params, JSONObject headers) throws IOException {
        if (CollectionUtils.isEmpty(params)) {
            params = new JSONObject();
        }
        if (CollectionUtils.isEmpty(headers)) {
            headers = new JSONObject();
        }
        HttpUrl.Builder queryUrl = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        for (String key : params.keySet()) {
            queryUrl.addQueryParameter(key, params.getString(key));
        }
        Request.Builder requestBuilder = new Request.Builder().url(queryUrl.build())
            .addHeader("Accept", "application/json; charset=UTF-8");
        for (String key : headers.keySet()) {
            requestBuilder.addHeader(key, headers.getString(key));
        }
        Response response = getHttpClient().newCall(requestBuilder.get().build()).execute();
        assert response.body() != null;
        return response.body().string();
    }

    public static String postString(String url, JSONObject postJson) throws IOException {
        String postBody = JSONObject.toJSONString(postJson);
        HttpUrl.Builder queryUrl = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        Request.Builder requestBuilder = new Request.Builder().url(queryUrl.build())
            .addHeader("Accept", "application/json; charset=UTF-8");
        Response response = getHttpClient().newCall(
            requestBuilder.post(RequestBody.create(MediaType.parse("application/json"), postBody)).build()
        ).execute();
        assert response.body() != null;
        return response.body().string();
    }

    public static JSONObject post(String url, JSONObject postJson) throws IOException {
        return JSONObject.parseObject(postString(url, postJson));
    }

    public static JSONArray postArray(String url, JSONObject postJson) throws IOException {
        return JSONObject.parseArray(postString(url, postJson));
    }
}

