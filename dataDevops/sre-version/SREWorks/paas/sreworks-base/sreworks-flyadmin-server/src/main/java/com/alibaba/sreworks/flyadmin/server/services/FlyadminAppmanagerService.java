package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.AppmanagerServiceUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.sreworks.domain.DO.Cluster;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import io.kubernetes.client.openapi.ApiException;
import org.springframework.stereotype.Service;

@Service
public class FlyadminAppmanagerService {

    public List<JSONObject> listResourceAddon(String user) throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/addon")
            .params("addonType", "RESOURCE_ADDON")
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data").getJSONArray("items").toJavaList(JSONObject.class)
            .stream().filter(x -> "RESOURCE_ADDON".equals(x.getString("addonType"))).collect(Collectors.toList());
    }

    public List<JSONObject> listMarketApp(String user) throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/market/apps")
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data").getJSONArray("items").toJavaList(JSONObject.class);
    }

    public List<JSONObject> listTrait(String user) throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/traits")
            .params("page", 1, "pageSize", 10000).headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data").getJSONArray("items").toJavaList(JSONObject.class);
    }

    public JSONObject k8sMicroservice(String bizApp, String user, String appId, String componentTypeList) throws IOException {
        String[] bizAppList = bizApp.split(",", 2);
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/apps/" + appId + "/k8s-microservices")
            .params(
                "appId", appId,
                "componentTypeList", componentTypeList
            )
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .headers(HttpHeaderNames.X_BIZ_APP, appId + "," + bizAppList[1])
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data");
    }

    public JSONObject helm(String bizApp, String user, String appId) throws IOException {
        String[] bizAppList = bizApp.split(",", 2);
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/apps/" + appId + "/helm")
            .params()
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .headers(HttpHeaderNames.X_BIZ_APP, appId + "," + bizAppList[1])
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data");
    }
}
