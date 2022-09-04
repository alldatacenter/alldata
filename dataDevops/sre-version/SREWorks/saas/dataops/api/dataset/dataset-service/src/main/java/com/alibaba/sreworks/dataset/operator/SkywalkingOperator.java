package com.alibaba.sreworks.dataset.operator;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.dataset.common.properties.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * SW工具类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/29 16:50
 */
@Service
@Slf4j
public class SkywalkingOperator extends HttpOperator implements InitializingBean {

    @Autowired
    ApplicationProperties properties;

    String skywalkingEndpoint = null;

    private String executeGraphQL = "/graphql";

    @Override
    public void afterPropertiesSet() {
        skywalkingEndpoint = properties.getSkywalkingProtocol() + "://" + properties.getSkywalkingHost() + ":" + properties.getSkywalkingPort();
    }

    public JSONObject executeGraphQL(String graphQL, JSONObject variables) throws Exception {
        String url = skywalkingEndpoint + executeGraphQL;
        HttpUrl.Builder urlBuilder = Objects.requireNonNull(HttpUrl.parse(url)).newBuilder();

        JSONObject body = new JSONObject();
        body.put("query", graphQL);
        body.put("variables", variables);

        RequestBody requestBody = RequestBody.create(MediaType.get("application/json; charset=utf-8"), JSONObject.toJSONString(body));

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .post(requestBody)
                .build();
        JSONObject ret = doRequest(request);
        return ret.getJSONObject("data");
    }
}
