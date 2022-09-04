package com.alibaba.tesla.tkgone.server.services.category;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.tkgone.server.common.Constant;
import com.alibaba.tesla.tkgone.server.common.MysqlHelper;
import com.alibaba.tesla.tkgone.server.common.Tools;
import com.alibaba.tesla.tkgone.server.services.config.BaseConfigService;
import com.alibaba.tesla.tkgone.server.services.config.CategoryConfigService;
import lombok.Data;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author yangjinghua
 */
@Service
@Data
public class GetExData {

    @Autowired
    CategoryConfigService categoryConfigService;

    @Autowired
    BaseConfigService baseConfigService;

    OkHttpClient okHttpClient = new OkHttpClient.Builder()
        .connectionPool(new ConnectionPool())
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .connectTimeout(120, TimeUnit.SECONDS)
        .build();

    JSONArray getResourceFromTable(JSONObject resourceInfo) throws SQLException {
        List<JSONObject> jsonObjectList;
        String password = (String)resourceInfo.getOrDefault("password", resourceInfo.get("passwd"));
        String username = (String)resourceInfo.getOrDefault("username", resourceInfo.get("user"));
        MysqlHelper mysqlHelp = new MysqlHelper(resourceInfo.getString("host"), resourceInfo.getString("port"),
            resourceInfo.getString("db"), username, password);
        String sql = resourceInfo.getString("sql");
        jsonObjectList = mysqlHelp.executeQuery(sql);
        return new JSONArray(new ArrayList<>(jsonObjectList));
    }

    Object getResourceFromUrl(JSONObject resourceInfo) throws IOException {
        String url = resourceInfo.getString("url");
        String method = resourceInfo.getString("method");
        JSONObject headers = resourceInfo.getJSONObject("headers");
        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (headers != null) {
            for (String key : headers.keySet()) {
                requestBuilder.addHeader(key, headers.getString(key));
            }
        }
        Request request;
        Response response;
        if ("GET".equals(method)) {
            request = requestBuilder.build();
        } else {
            return null;
        }
        response = okHttpClient.newCall(request).execute();
        assert response.body() != null;
        return JSONObject.parse(response.body().string());
    }

    private Object getOriginalResourceData(JSONObject node, JSONObject resourceConfigJson) throws Exception {

        if (resourceConfigJson == null) {
            return new JSONObject();
        }
        resourceConfigJson = JSONObject.parseObject(
            Tools.processTemplateString(JSONObject.toJSONString(resourceConfigJson), node));
        String resourceConfigType = resourceConfigJson.getString("type");
        JSONObject resourceConfigInfo = resourceConfigJson.getJSONObject("info");
        switch (resourceConfigType) {
            case "table":
                return getResourceFromTable(resourceConfigInfo);
            case "url":
                return getResourceFromUrl(resourceConfigInfo);
            default:
                return new JSONArray();
        }
    }

    private Object getResourceData(JSONObject node, JSONObject resourceConfigJson) throws Exception {
        Object originalResourceData = getOriginalResourceData(node, resourceConfigJson);
        JSONObject transferConfigJson = resourceConfigJson.getJSONObject("transfer");
        if (transferConfigJson == null) { return originalResourceData; }
        String transferConfigType = transferConfigJson.getString("type");
        JSONObject transferConfigInfoJson = transferConfigJson.getJSONObject("info");
        if ("splitToMore".equals(transferConfigType)) {
            JSONArray retArray = new JSONArray();
            String splitString = transferConfigInfoJson.getString("splitString");
            String splitField = transferConfigInfoJson.getString("splitField");
            originalResourceData = JSON.toJSON(originalResourceData);
            if (originalResourceData instanceof JSONArray) {
                ((JSONArray)originalResourceData).toJavaList(JSONObject.class).forEach(lineJson -> {
                    for (String subFieldValue : lineJson.getString(splitField).split(splitString)) {
                        JSONObject subLineJson = new JSONObject();
                        subLineJson.putAll(lineJson);
                        subLineJson.put(splitField, subFieldValue);
                        retArray.add(subLineJson);
                    }
                });
                return retArray;
            }
        }
        return originalResourceData;
    }

    private JSONObject getExDataConfigJson(String category, JSONObject node, String exDataName) {
        String type = node.getString(Constant.INNER_TYPE);
        JSONObject exDataConfigJson = categoryConfigService.getCategoryTypeSpecExDataConfig(category, type, exDataName);
        JSONObject resourceConfigJson = exDataConfigJson.getJSONObject("resource");
        resourceConfigJson = JSONObject.parseObject(
            Tools.processTemplateString(JSONObject.toJSONString(resourceConfigJson), node)
        );
        exDataConfigJson.put("resource", resourceConfigJson);
        return exDataConfigJson;
    }

    private JSONArray getTableData(JSONArray resourceData, JSONArray headers) {
        for (JSONObject resourceDataLine : resourceData.toJavaList(JSONObject.class)) {
            for (JSONObject header : headers.toJavaList(JSONObject.class)) {
                resourceDataLine.put(header.getString("name"),
                    Tools.processTemplateString(header.getString("value"), resourceDataLine));
            }
        }
        return resourceData;
    }

    public JSONObject getData(String category, JSONObject node, String exDataName) throws Exception {
        JSONObject exDataConfigJson = getExDataConfigJson(category, node, exDataName);
        JSONObject resourceConfigJson = exDataConfigJson.getJSONObject("resource");
        Object resourceData = getResourceData(node, resourceConfigJson);
        resourceData = JSON.toJSON(resourceData);

        JSONObject dataConfigJson = exDataConfigJson.getJSONObject("data");
        String dataConfigType = dataConfigJson.getString("type");
        String dataConfigTitle = dataConfigJson.getString("title");

        JSONObject retData = new JSONObject();
        switch (dataConfigType) {
            case "grafana":
                String contentString = JSONObject.toJSONString(dataConfigJson.getJSONArray("content"));
                contentString = Tools.processTemplateString(contentString, node);
                JSONArray contentArray = JSONObject.parseArray(contentString);
                retData.put("content", contentArray);
                break;
            case "components":
                retData.put("content", resourceData);
                break;
            case "table":
                if (resourceData instanceof JSONArray) {
                    JSONArray headers = dataConfigJson.getJSONArray("headers");
                    retData.put("headers", headers);
                    retData.put("content", getTableData((JSONArray)resourceData, headers));
                }
                break;
            default:
                retData.put("content", null);
                break;
        }
        retData.put("type", dataConfigType);
        retData.put("title", dataConfigTitle);

        return retData;
    }

}
