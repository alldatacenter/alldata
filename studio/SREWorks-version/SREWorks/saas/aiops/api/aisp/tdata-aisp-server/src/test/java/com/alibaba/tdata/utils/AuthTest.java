package com.alibaba.tdata.utils;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tdata.aisp.server.common.utils.TeslaAuthUtil;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.junit.Test;

/**
 * @ClassName: AuthTest
 * @Author: dyj
 * @DATE: 2021-12-13
 * @Description:
 **/
public class AuthTest {
    @Test
    public void test() throws IOException, NoSuchAlgorithmException {
        OkHttpClient httpClient = new OkHttpClient().newBuilder().build();
        String body = "{\"taskType\":\"sync\",\"series\":[[1635216096000,23.541],[1635216097000,33.541]]}";
        Request request = new Request.Builder().url("")
            .addHeader("x-auth-app", "tdata_aisp")       // 用自己的鉴权，替换x-auth-app字段。
            .addHeader("x-auth-key", "")       // 用自己的鉴权，替换x-auth-key字段。
            .addHeader("x-auth-user", "tdata_aisp")     // 用自己的鉴权，替换x-auth-user字段。
            .addHeader("x-auth-passwd", "") // 一天有效，请用自己的鉴权，替换x-auth-passwd字段！
            .post(RequestBody.create(MediaType.parse("application/json"), body))
            .build();
        Response response = httpClient.newCall(request).execute();
        assert response.body() != null;
        System.out.println(response.body().string());
    }
}
