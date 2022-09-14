package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.AppmanagerServiceUtil;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.sreworks.domain.DTO.AppComponentRepoDetail;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import io.kubernetes.client.openapi.ApiException;
import org.springframework.stereotype.Service;

import static com.alibaba.sreworks.domain.utils.AppUtil.appmanagerId;

/**
 * @author jinghua.yjh
 */
@Service
public class FlyadminAppmanagerComponentService {

    public List<String> listName(Long appId, String user) throws IOException, ApiException {
        String url = AppmanagerServiceUtil.getEndpoint() + "/apps/" + appmanagerId(appId) + "/k8s-microservices";
        JSONArray items = new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject().getJSONObject("data").getJSONArray("items");
        return items.toJavaList(JSONObject.class).stream()
            .map(x -> x.getString("microServiceId")).collect(Collectors.toList());
    }

    public Long create(Long appId, String name, AppComponentRepoDetail repo, String user)
        throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/apps/" + appmanagerId(appId) + "/k8s-microservices")
            .postJson(
                "appId", appmanagerId(appId),
                "componentType", "K8S_MICROSERVICE",
                "name", name,
                "microServiceId", name,
                "containerObjectList", JsonUtil.list(JsonUtil.map(
                    "containerType", "CONTAINER",
                    "repoType", "THIRD_REPO",
                    "repo", repo.getUrl(),
                    "branch", repo.getBranch(),
                    "dockerfileTemplate", repo.getDockerfileTemplate()
                ))
            )
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful()
            .getJSONObject().getJSONObject("data").getLongValue("id");
    }

    public void delete(Long appId, Long id, String user) throws IOException, ApiException {
        new Requests(AppmanagerServiceUtil.getEndpoint() + "/apps/" + appmanagerId(appId) + "/k8s-microservices/" + id)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .delete().isSuccessful();
    }

}
