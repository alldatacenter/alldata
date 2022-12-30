package com.alibaba.tesla.tkgone.server.common.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * HTTP工具类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/29 16:50
 */
@Service
@Slf4j
public class HttpOperator {

    public String get(String url, Map<String, String> headers, Map<String, String> params) throws Exception {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(urlBuilder::addQueryParameter);
        }

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .headers(buildHeaders(headers))
                .build();
        return doRequest(request);
    }

    public String post(String url, Map<String, String> headers, Map<String, String> params, MediaType mediaType, String body) throws Exception {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(urlBuilder::addQueryParameter);
        }

        RequestBody requestBody = RequestBody.create(mediaType, body);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .headers(buildHeaders(headers))
                .post(requestBody)
                .build();
        return doRequest(request);
    }

    private Headers buildHeaders(Map<String, String> headers) {
        okhttp3.Headers.Builder headersBuild = new okhttp3.Headers.Builder();
        if (!CollectionUtils.isEmpty(headers)) {
            for (String key : headers.keySet()) {
                headersBuild.add(key, headers.get(key));
            }
        }
        return headersBuild.build();
    }

    private String doRequest(Request request) throws Exception {
        OkHttpClient okHttpClient = HttpClient.getHttpClient();
        Response response = okHttpClient.newCall(request).execute();
        if (response.code() != 200) {
            throw new Exception(String.format("请求接口[%s]异常, 异常信息:%s", request.url().url(), response.cacheResponse()));
        }

        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            throw new Exception(String.format("请求接口[%s]异常, 响应体为空, 异常信息:%s", request.url().url(), response.message()));
        }
        return responseBody.string();
    }
}
