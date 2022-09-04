package com.alibaba.tesla.appmanager.common.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;

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
            httpClient = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool())
                .sslSocketFactory(TRUST_ALL_SSL_SOCKET_FACTORY, (X509TrustManager)TRUST_ALL_CERTS[0])
                .hostnameVerifier((hostname, session) -> true)
                .build();
        }
        return httpClient;
    }

    public static OkHttpClient newHttpClient(OkHttpClient.Builder builder) {
        return builder
                .connectionPool(new ConnectionPool())
                .sslSocketFactory(TRUST_ALL_SSL_SOCKET_FACTORY, (X509TrustManager)TRUST_ALL_CERTS[0])
                .hostnameVerifier((hostname, session) -> true)
                .build();
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

    public static <T> T transfer(String resp, Class<T> tClass) throws Exception {
        if (tClass == JSONObject.class || tClass == JSONArray.class) {
            return (T)JSONObject.parse(resp);
        } else if (tClass == String.class) {
            return (T)resp;
        } else {
            throw new Exception(String.format("resp: %s 不支持class: %s", resp, tClass.getName()));
        }
    }

    public static String get(String url, JSONObject params, JSONObject headers) throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.get().build();
        Response response = getHttpClient().newCall(request).execute();
        assert response.body() != null;
        return response.body().string();
    }

    public static <T> T get(String url, JSONObject params, JSONObject headers, Class<T> tClass) throws Exception {
        log.info("GET_URL: {}, PARAMS: {}", url, params.toJSONString());
        String resp = get(url, params, headers);
        return transfer(resp, tClass);
    }

    public static <T> T get(String url, Class<T> tClass) throws Exception {
        String resp = get(url, null, null);
        return transfer(resp, tClass);
    }

    public static String post(String url, JSONObject params, String postJson, JSONObject headers)
        throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.post(RequestBody.create(MediaType.parse("application/json"),
            postJson)).build();
        Response response = getHttpClient().newCall(request).execute();
        assert response.body() != null;
        return response.body().string();
    }

    public static String post(OkHttpClient client, String url, JSONObject params, String postJson, JSONObject headers)
            throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.post(RequestBody.create(MediaType.parse("application/json"),
                postJson)).build();
        Response response = client.newCall(request).execute();
        assert response.body() != null;
        return response.body().string();
    }

    public static <T> T post(String url, JSONObject params, String postJson, JSONObject headers, Class<T> tClass)
        throws Exception {
        String resp = post(url, params, postJson, headers);
        return transfer(resp, tClass);
    }

    public static <T> T post(String url, String postJson, Class<T> tClass) throws Exception {
        String resp = post(url, new JSONObject(), postJson, new JSONObject());
        return transfer(resp, tClass);
    }

    public static <T> T post(OkHttpClient client, String url, String postJson, Class<T> tClass) throws Exception {
        String resp = post(client, url, new JSONObject(), postJson, new JSONObject());
        return transfer(resp, tClass);
    }

    public static String put(String url, JSONObject params, String postJson, JSONObject headers)
        throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.put(RequestBody.create(MediaType.parse("application/json"), postJson)).build();
        Response response = getHttpClient().newCall(request).execute();
        assert response.body() != null;
        return response.body().string();
    }

    public static <T> T put(String url, JSONObject params, String postJson, JSONObject headers, Class<T> tClass)
        throws Exception {
        String resp = put(url, params, postJson, headers);
        return transfer(resp, tClass);
    }

    public static String delete(String url, JSONObject params, JSONObject headers) throws IOException {
        Request.Builder requestBuilder = createRequestBuilder(url, params, headers);
        Request request = requestBuilder.delete().build();
        Response response = getHttpClient().newCall(request).execute();
        assert response.body() != null;
        return response.body().string();
    }

    public static <T> T delete(String url, JSONObject params, JSONObject headers, Class<T> tClass) throws Exception {
        String resp = delete(url, params, headers);
        return transfer(resp, tClass);
    }

}

