package com.alibaba.sreworks.health.operator;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.common.properties.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 应用工具类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/29 16:50
 */
@Service
@Slf4j
public class AppOperator extends HttpOperator {

    @Autowired
    ApplicationProperties properties;

    private String getAppByIdPath = "/appdev/app/detail";

    private String getAllAppsPath = "/appdev/app/listAll";

    public JSONObject getAllApps() throws Exception {
        String url = properties.getAppProtocol() + "://" + properties.getAppHost() + ":" + properties.getAppPort() + getAllAppsPath;
        Map<String, String> params = new HashMap<>();
        JSONObject ret = requestGet(url, params);
        return ret.getJSONObject("data");
    }

    public JSONObject getAppById(String appId) throws Exception {
        String url = properties.getAppProtocol() + "://" + properties.getAppHost() + ":" + properties.getAppPort() + getAppByIdPath;
        Map<String, String> params = new HashMap<>();
        params.put("appId", appId);
        JSONObject ret = requestGet(url, params);
        return ret.getJSONObject("data");
    }

    private JSONObject requestGet(String url, Map<String, String> params) throws Exception {
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        params.forEach(urlBuilder::addQueryParameter);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("X-EmpId", "PMDB")
                .build();
        return doRequest(request);
    }
}
