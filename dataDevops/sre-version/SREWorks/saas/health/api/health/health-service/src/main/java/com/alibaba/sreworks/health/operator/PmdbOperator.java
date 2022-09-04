package com.alibaba.sreworks.health.operator;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.health.common.properties.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * PMDB工具类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/29 16:50
 */
@Service
@Slf4j
public class PmdbOperator extends HttpOperator {

    @Autowired
    ApplicationProperties properties;

    private String getMetricsPath = "/metric/getMetrics";

    public JSONArray getMetrics(String metricName, String appId, String appName) throws Exception {
        String url = properties.getPmdbProtocol() + "://" + properties.getPmdbHost() + ":" + properties.getPmdbPort() + getMetricsPath;
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();
        urlBuilder.addQueryParameter("name", metricName);

        JSONObject bodyParams = new JSONObject();
        bodyParams.put("app_id", appId);
        bodyParams.put("app_name", appName);

        RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), JSONObject.toJSONString(bodyParams));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .post(requestBody)
                .build();
        JSONObject ret = doRequest(request);
        return ret.getJSONArray("data");
    }
}
