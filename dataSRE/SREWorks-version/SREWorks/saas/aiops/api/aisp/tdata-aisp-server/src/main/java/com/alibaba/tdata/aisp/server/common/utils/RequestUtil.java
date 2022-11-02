package com.alibaba.tdata.aisp.server.common.utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.exception.PlatformInternalException;

import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author yangjinghua
 */
@Slf4j
public class RequestUtil {

    private static OkHttpClient httpClient;

    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[] {
        new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws CertificateException {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws CertificateException {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[] {};
            }
        }
    };

    private static final SSLContext TRUST_ALL_SSL_CONTEXT;

    static {
        try {
            TRUST_ALL_SSL_CONTEXT = SSLContext.getInstance("SSL");
            TRUST_ALL_SSL_CONTEXT.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private static final SSLSocketFactory TRUST_ALL_SSL_SOCKET_FACTORY = TRUST_ALL_SSL_CONTEXT.getSocketFactory();

    private static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            Dispatcher dispatcher = new Dispatcher();
            dispatcher.setMaxRequests(500);
            dispatcher.setMaxRequestsPerHost(500);
            httpClient = new Builder()
                .connectionPool(new ConnectionPool(500, 5, TimeUnit.MINUTES))
                .dispatcher(dispatcher)
                .readTimeout(1, TimeUnit.MINUTES)
                .writeTimeout(1, TimeUnit.MINUTES)
                .callTimeout(1, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true)
                .sslSocketFactory(TRUST_ALL_SSL_SOCKET_FACTORY, (X509TrustManager)TRUST_ALL_CERTS[0])
                .hostnameVerifier((hostname, session) -> true)
                .build();
        }
        return httpClient;
    }

    private static Request.Builder createRequestBuilder(String url, JSONObject params, JSONObject headers) {
        if (params == null) {
            params = new JSONObject();
        }
        if (headers == null) {
            headers = new JSONObject();
        }
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

    public static <T> T transfer(String resp, Class<T> tClass) {
        if (tClass == JSONObject.class || tClass == JSONArray.class) {
            return (T)JSONObject.parse(resp);
        } else if (tClass == String.class) {
            return (T)resp;
        } else {
            throw new PlatformInternalException(String.format("resp: %s 不支持class: %s", resp, tClass.getName()));
        }
    }

    public static String get(String url, JSONObject params, JSONObject headers) throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.get().build();
        Response response = getHttpClient().newCall(request).execute();
        if (!response.isSuccessful()){
            throw new PlatformInternalException("action=get || response failed! " + response.message());
        }
        assert response.body() != null;
        return response.body().string();
    }

    public static String get(OkHttpClient httpClient, String url, JSONObject params, JSONObject headers) throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.get().build();
        Response response = httpClient.newCall(request).execute();
        if (!response.isSuccessful()){
            throw new PlatformInternalException("action=get || response failed! " + response.message());
        }
        assert response.body() != null;
        return response.body().string();
    }

    public static <T> T get(String url, JSONObject params, JSONObject headers, Class<T> tClass) throws IOException {
        log.debug("GET_URL: {}, PARAMS: {}", url, params.toJSONString());
        String resp = get(url, params, headers);
        return transfer(resp, tClass);
    }

    public static <T> T get(OkHttpClient httpClient,String url, JSONObject params, JSONObject headers, Class<T> tClass)
        throws IOException {
        log.debug("GET_URL: {}, PARAMS: {}", url, params.toJSONString());
        String resp = get(httpClient, url, params, headers);
        return transfer(resp, tClass);
    }

    public static <T> T get(String url, Class<T> tClass) throws IOException {
        String resp = get(url, null, null);
        return transfer(resp, tClass);
    }

    public static String post(String url, JSONObject params, String postJson, JSONObject headers)
        throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.post(RequestBody.create(postJson, MediaType.parse("application/json"))).build();
        Response response = getHttpClient().newCall(request).execute();
        if (!response.isSuccessful()){
            throw new PlatformInternalException("action=post || response failed! " + response.message());
        }
        assert response.body() != null;
        return response.body().string();
    }

    public static <T> T post(String url, JSONObject params, String postJson, JSONObject headers, Class<T> tClass)
        throws IOException {
        String resp = post(url, params, postJson, headers);
        return transfer(resp, tClass);
    }

    public static <T> T post(String url, String postJson, Class<T> tClass) throws IOException {
        String resp = post(url, new JSONObject(), postJson, new JSONObject());
        return transfer(resp, tClass);
    }

    public static String put(String url, JSONObject params, String postJson, JSONObject headers)
        throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.put(RequestBody.create(postJson, MediaType.parse("application/json"))).build();
        Response response = getHttpClient().newCall(request).execute();
        assert response.body() != null;
        return response.body().string();
    }

    public static <T> T put(String url, JSONObject params, String postJson, JSONObject headers, Class<T> tClass)
        throws IOException {
        String resp = put(url, params, postJson, headers);
        return transfer(resp, tClass);
    }

    public static String delete(String url, JSONObject params, JSONObject headers) throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.delete().build();
        Response response = getHttpClient().newCall(request).execute();
        if (!response.isSuccessful()){
            throw new PlatformInternalException("action=delete || response failed! " + response.message());
        }
        assert response.body() != null;
        return response.body().string();
    }

    public static <T> T delete(String url, JSONObject params, JSONObject headers, Class<T> tClass) throws IOException {
        String resp = delete(url, params, headers);
        return transfer(resp, tClass);
    }

}

