package com.alibaba.tesla.authproxy.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

public class HttpUtil {

    /**
     * 根据 Request 发起真正的 HTTP 请求，并返回 Response 字符串
     */
    public static String makeHttpCall(OkHttpClient client, Request request) throws IOException {
        Response response = client.newCall(request).execute();
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            response.close();
            throw new IOException("NULL body from channel service interface");
        }
        String responseStr = responseBody.string();
        response.close();
        return responseStr;
    }
}
