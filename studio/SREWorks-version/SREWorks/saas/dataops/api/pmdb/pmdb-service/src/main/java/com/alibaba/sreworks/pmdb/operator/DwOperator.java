package com.alibaba.sreworks.pmdb.operator;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.common.properties.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 数仓工具类
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/11/29 16:50
 */
@Service
@Slf4j
public class DwOperator extends HttpOperator {

    @Autowired
    ApplicationProperties properties;

    private String getDWModelWithFieldsPath = "/model/meta/getModelWithFieldsByName";

    private String getModelByNamePath = "/model/meta/getModelByName";

    public JSONObject getDWModelWithFields(String modelName) throws Exception {
        String url = properties.getDwProtocol() + "://" + properties.getDwHost() + ":" + properties.getDwPort() + getDWModelWithFieldsPath;
        Map<String, String> params = new HashMap<>();
        params.put("name", modelName);
        return requestGet(url, params);
    }

    public JSONObject getDWModel(String modelName) throws Exception {
        String url = properties.getDwProtocol() + "://" + properties.getDwHost() + ":" + properties.getDwPort() + getModelByNamePath;
        Map<String, String> params = new HashMap<>();
        params.put("name", modelName);
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
