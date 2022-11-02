package com.alibaba.sreworks.common.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import com.alibaba.tesla.appmanager.client.AppManagerClient;

/**
 * @author yangjinghua
 */
@Slf4j
@Data
@NoArgsConstructor
public class Requests {

    private static AppManagerClient APPMANAGER_HTTP_CLIENT = null;

    private static OkHttpClient HTTP_CLIENT = null;

    static {
        if (System.getenv("APPMANAGER_ENDPOINT") != null) {
            APPMANAGER_HTTP_CLIENT = new AppManagerClient(
                System.getenv("APPMANAGER_ENDPOINT"),
                System.getenv("APPMANAGER_USERNAME"),
                System.getenv("APPMANAGER_PASSWORD"),
                System.getenv("APPMANAGER_CLIENT_ID"),
                System.getenv("APPMANAGER_CLIENT_SECRET")
            );
        } else {
            HTTP_CLIENT = new OkHttpClient.Builder().connectionPool(new ConnectionPool()).build();
        }
    }

    private void sendRequest(Builder requestBuilder) throws IOException {
        if (APPMANAGER_HTTP_CLIENT != null) {
            response = APPMANAGER_HTTP_CLIENT.sendRequestSimple(requestBuilder);
        } else {
            response = HTTP_CLIENT.newCall(requestBuilder.build()).execute();
        }
        responseBodyString = Objects.requireNonNull(response.body()).string();
    }

    private static Request.Builder createRequestBuilder(String url, JSONObject params, JSONObject headers) {
        HttpUrl.Builder queryUrl = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        for (String key : params.keySet()) {
            queryUrl.addQueryParameter(key, params.getString(key));
        }
        Request.Builder requestBuilder = new Request.Builder().url(queryUrl.build());
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

    public Requests(String url) {
        this.url = url;
    }

    public Requests(String url, Boolean showLog) {
        this.url = url;
        this.showLog = showLog;
    }

    public Requests url(String url) {
        this.url = url;
        return this;
    }

    public Requests params(JSONObject params) {
        this.params = params;
        return this;
    }

    public Requests params(Object... args) {
        this.params = JsonUtil.map(args);
        return this;
    }

    public Requests headers(JSONObject headers) {
        this.headers = headers;
        return this;
    }

    public Requests headers(Object... args) {
        this.headers = JsonUtil.map(args);
        return this;
    }

    public Requests postJson(JSONObject postJson) {
        this.postJson = JSONObject.toJSONString(postJson);
        return this;
    }

    public Requests postJson(Object... args) {
        this.postJson = JSONObject.toJSONString(JsonUtil.map(args));
        return this;
    }

    public Requests postJson(String postJson) {
        this.postJson = postJson;
        return this;
    }

    public Response response;

    public String responseBodyString;

    public Requests isSuccessful() throws IOException {
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
//            log.info(JSONObject.toJSONString(jsonObject));
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

    public Requests get() throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder.get());
        return this;
    }

    public Requests post() throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder
            .post(RequestBody.create(MediaType.parse("application/json"), postJson)));
        return this;
    }

    public Requests upload(File file) throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder
            .post(RequestBody.create(MediaType.parse("application/octet-stream"), file)));
        return this;
    }

    public Requests put() throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder
            .put(RequestBody.create(MediaType.parse("application/json"), postJson)));
        return this;
    }

    public Requests delete() throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        sendRequest(requestBuilder.delete());
        return this;
    }

}

