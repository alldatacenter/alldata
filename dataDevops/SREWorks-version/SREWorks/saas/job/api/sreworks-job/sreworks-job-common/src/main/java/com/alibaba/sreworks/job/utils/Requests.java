package com.alibaba.sreworks.job.utils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class Requests {

    static HttpClient client = HttpClient.newBuilder().build();

    private static String addParams(String url, JSONObject params) {

        if (params == null) {
            return url;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        for (String key : params.keySet()) {
            String value = params.getString(key);
            builder.queryParam(key, value);
        }
        return builder.build().toUri().toString();
    }

    private static void addHeaders(HttpRequest.Builder builder, JSONObject headers) {

        if (headers == null) {
            return;
        }
        for (String key : headers.keySet()) {
            String value = headers.getString(key);
            builder.header(key, value);
        }

    }

    public static HttpResponse<String> post(
        String url, JSONObject headers, JSONObject params, String postBody) throws IOException, InterruptedException {

        if (postBody == null) {
            postBody = "";
        }
        url = addParams(url, params);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(postBody));
        addHeaders(builder, headers);
        return client.send(builder.build(), BodyHandlers.ofString());

    }

    public static HttpResponse<String> get(
        String url, JSONObject headers, JSONObject params) throws IOException, InterruptedException {

        url = addParams(url, params);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET();
        addHeaders(builder, headers);
        return client.send(builder.build(), BodyHandlers.ofString());

    }

    public static HttpResponse<String> delete(
        String url, JSONObject headers, JSONObject params) throws IOException, InterruptedException {

        url = addParams(url, params);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .DELETE();
        addHeaders(builder, headers);
        return client.send(builder.build(), BodyHandlers.ofString());

    }

    public static void checkResponseStatus(HttpResponse<String> response) throws Exception {
        if (response.statusCode() >= 300) {
            throw new Exception("response statusCode is " + response.statusCode() + " body is " + response.body());
        } else {
            String strBody = response.body();
            if (StringUtils.isNotEmpty(strBody)) {
                JSONValidator validator = JSONValidator.from(response.body());
                boolean validJSON = validator.validate();
                if (validJSON) {
                    JSONObject body = JSONObject.parseObject(response.body());
                    int execCode = body.getIntValue("code");
                    if (execCode >= 300) {
                        throw new Exception("response is " + response.body());
                    }
                }
            }
        }
    }

}
