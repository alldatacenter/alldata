package com.alibaba.tesla.appmanager.trait.plugin.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okhttp3.Request.Builder;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @author yangjinghua
 */
@Slf4j
@Data
@NoArgsConstructor
public class RequestsUtil {

    private static OkHttpClient HTTP_CLIENT = null;

    static {
        HTTP_CLIENT = new OkHttpClient.Builder().connectionPool(new ConnectionPool()).build();
    }

    private void sendRequest(Builder requestBuilder) throws IOException {
        response = HTTP_CLIENT.newCall(requestBuilder.build()).execute();
        responseBodyString = Objects.requireNonNull(response.body()).string();
    }

    private static Builder createRequestBuilder(String url, JSONObject params, JSONObject headers) {
        HttpUrl.Builder queryUrl = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        for (String key : params.keySet()) {
            queryUrl.addQueryParameter(key, params.getString(key));
        }
        Builder requestBuilder = new Builder().url(queryUrl.build());
        for (String key : headers.keySet()) {
            requestBuilder.addHeader(key, headers.getString(key));
        }
        return requestBuilder;
    }

    public String url;

    public JSONObject params = new JSONObject();

    public JSONObject headers = new JSONObject();

    public String postJson = "{}";

    public Boolean showLog = false;

    public RequestsUtil(String url) {
        this.url = url;
    }

    public RequestsUtil(String url, Boolean showLog) {
        this.url = url;
        this.showLog = showLog;
    }

    public RequestsUtil url(String url) {
        this.url = url;
        return this;
    }

    public RequestsUtil params(JSONObject params) {
        this.params = params;
        return this;
    }

    public RequestsUtil params(Object... args) {
        this.params = JsonUtil.map(args);
        return this;
    }

    public RequestsUtil headers(JSONObject headers) {
        this.headers = headers;
        return this;
    }

    public RequestsUtil headers(Object... args) {
        this.headers = JsonUtil.map(args);
        return this;
    }

    public RequestsUtil postJson(JSONObject postJson) {
        this.postJson = JSONObject.toJSONString(postJson);
        return this;
    }

    public RequestsUtil postJson(Object... args) {
        this.postJson = JSONObject.toJSONString(JsonUtil.map(args));
        return this;
    }

    public RequestsUtil postJson(String postJson) {
        this.postJson = postJson;
        return this;
    }

    public Response response;

    public String responseBodyString;

    public RequestsUtil isSuccessful() throws IOException {
        if (!response.isSuccessful()) {
            throw new IOException(String.format(
                "response is not successful: %s; retBody: %s", response.toString(), getString()
            ));
        }

        ResponseBody body = response.body();
        if (body != null) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject = JSONObject.parseObject(responseBodyString);
            } catch (Exception ignored) {
            }
            log.info(JSONObject.toJSONString(jsonObject));
            if (jsonObject.getLongValue("code") >= 300) {
                throw new IOException(String.format(
                    "response is not successful: %s; retBody: %s", response.toString(), getString()
                ));
            }
        }

        return this;
    }

    public String getString() {
        if (this.showLog) {
            log.info("SHOW_RESPONSE: " + responseBodyString);
        }
        return responseBodyString;
    }

    public <T> T getObject(Class<T> clazz) throws IOException {
        return JSONObject.parseObject(getString(), clazz);
    }

    public JSONObject getJSONObject() throws IOException {
        return getObject(JSONObject.class);
    }

    public JSONArray getJSONArray() throws IOException {
        return getObject(JSONArray.class);
    }

    public <T> List<T> getJSONArray(Class<T> clazz) throws IOException {
        return getObject(JSONArray.class).toJavaList(clazz);
    }

    public Boolean getBoolean() throws IOException {
        return getObject(Boolean.class);
    }

    public RequestsUtil get() throws IOException {
        Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder.get());
        return this;
    }

    public RequestsUtil post() throws IOException {
        Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder
            .post(RequestBody.create(MediaType.parse("application/json"), postJson)));
        return this;
    }

    public RequestsUtil upload(File file) throws IOException {
        Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder
            .post(RequestBody.create(MediaType.parse("application/octet-stream"), file)));
        return this;
    }

    public RequestsUtil put() throws IOException {
        Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder
            .put(RequestBody.create(MediaType.parse("application/json"), postJson)));
        return this;
    }

    public RequestsUtil delete() throws IOException {
        Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder.delete());
        return this;
    }

}

