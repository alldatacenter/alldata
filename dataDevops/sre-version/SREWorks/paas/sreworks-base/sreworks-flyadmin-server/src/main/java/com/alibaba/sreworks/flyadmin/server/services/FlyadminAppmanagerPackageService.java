package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.AppmanagerServiceUtil;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DO.AppPackage;
import com.alibaba.sreworks.domain.DO.TeamRegistry;
import com.alibaba.sreworks.domain.DO.TeamRepo;
import com.alibaba.sreworks.domain.repository.AppComponentRepository;
import com.alibaba.sreworks.domain.repository.TeamRegistryRepository;
import com.alibaba.sreworks.domain.repository.TeamRepoRepository;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.alibaba.sreworks.domain.utils.AppUtil.appmanagerId;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Service
public class FlyadminAppmanagerPackageService {

    @Autowired
    FlyadminAppmanagerComponentService acService;

    @Autowired
    AppComponentRepository appComponentRepository;

    @Autowired
    TeamRepoRepository teamRepoRepository;

    @Autowired
    TeamRegistryRepository teamRegistryRepository;

    public List<JSONObject> listHistory(Long appId, String user) throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/apps/" + appmanagerId(appId) + "/app-package-tasks")
            .params("page", 1, "pageSize", 10000).headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data").getJSONArray("items").toJavaList(JSONObject.class);
    }

    public List<JSONObject> list(Long appId, String user) throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/apps/" + appmanagerId(appId) + "/app-packages")
            .params("page", 1, "pageSize", 10000).headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data").getJSONArray("items").toJavaList(JSONObject.class);
    }

    public String getLastVersion(Long appId, String user) throws IOException, ApiException {
        String url = AppmanagerServiceUtil.getEndpoint() + "/apps/" + appmanagerId(appId)
            + "/app-packages/latest-version";
        return new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getString("data");
    }

    private String getComponentLastVersion(Long appId, String componentType, String componentName, String user) {
        try {
            return new Requests(AppmanagerServiceUtil.getEndpoint()
                + "/apps/" + appmanagerId(appId) + "/component-packages/" + componentType + "/" + componentName
                + "/latest-version")
                .headers(HttpHeaderNames.X_EMPL_ID, user)
                .get().isSuccessful()
                .getJSONObject()
                .getJSONArray("data").getJSONObject(0).getString("name");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public JSONObject getConfig(Long appId, String version) {
        List<AppComponent> appComponentList = appComponentRepository.findAllByAppId(appId);
        return JsonUtil.map(
            "tags", JsonUtil.list("sreworks=true"),
            "version", version,
            "components", appComponentList.stream()
                .map(component -> {
                    switch (component.type()) {
                        case REPO:
                            TeamRegistry teamRegistry = teamRegistryRepository.findFirstById(
                                component.repoDetail().getTeamRegistryId()
                            );
                            TeamRepo teamRepo = teamRepoRepository.findFirstById(
                                component.repoDetail().getTeamRepoId()
                            );
                            return JsonUtil.map(
                                "componentName", component.getName(),
                                "componentType", "K8S_MICROSERVICE",
                                "version", version,
                                "useRawOptions", true,
                                "options", JsonUtil.map(
                                    "kind", "CloneSet",
                                    "containers", JsonUtil.list(JsonUtil.map(
                                        "name", component.getName(),
                                        "ports", JsonUtil.list(),
                                        "build", JsonUtil.map(
                                            "args", JsonUtil.map(),
                                            "dockerfileTemplateArgs", JsonUtil.map(),
                                            "dockerfileTemplate", component.repoDetail().getDockerfileTemplate(),
                                            "branch", component.repoDetail().getBranch(),
                                            "repo", component.repoDetail().getUrl(),
                                            "ciToken", teamRepo.getCiToken(),
                                            "imagePush", true,
                                            "imagePushRegistry", teamRegistry.getUrl(),
                                            "imagePushAuth", teamRegistry.getAuth()
                                        )
                                    ))
                                )
                            );
                        case HELM:
                            return JsonUtil.map(
                                "componentName", component.getName(),
                                "componentType", "HELM",
                                "version", version,
                                "useRawOptions", true,
                                "options", JsonUtil.map(
                                    "chartUrl", component.helmDetail().getChartUrl(),
                                    "repoUrl", component.helmDetail().getRepoUrl(),
                                    "chartName", component.helmDetail().getChartName(),
                                    "chartVersion", component.helmDetail().getChartVersion()
                                )
                            );
                        default:
                            return null;
                    }

                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    public JSONObject start(Long appId, String version, String user) throws IOException, ApiException {
        JSONObject postJson = getConfig(appId, version);
        log.info(JSONObject.toJSONString(postJson, true));
        return new Requests(
            AppmanagerServiceUtil.getEndpoint() + "/apps/" + appmanagerId(appId) + "/app-package-tasks")
            .postJson(postJson)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful()
            .getJSONObject().getJSONObject("data");
    }

    public JSONObject get(String appId, Long appPackageTaskId, String user) throws IOException, ApiException {
        String url = AppmanagerServiceUtil.getEndpoint() + "/apps/" + appId + "/app-package-tasks/" + appPackageTaskId;
        return new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject().getJSONObject("data");
    }

    public JSONObject logs(AppPackage appPackage, String user) throws IOException, ApiException {
        JSONObject logs = new JSONObject();
        String appId = appmanagerId(appPackage.getAppId());
        String url = AppmanagerServiceUtil.getEndpoint()
            + "/apps/" + appId + "/component-package-tasks";
        JSONArray items = new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .params(
                "appPackageTaskId", appPackage.getAppPackageTaskId(),
                "app_id", appId,
                "pageSize", 100
            )
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data").getJSONArray("items");
        for (JSONObject item : items.toJavaList(JSONObject.class)) {
            Long id = item.getLong("id");
            String logUrl = AppmanagerServiceUtil.getEndpoint()
                + "/apps/" + appId + "/component-package-tasks/" + id + "/logs";
            String log = "";
            try {
                log = new Requests(logUrl)
                    .headers(HttpHeaderNames.X_EMPL_ID, user)
                    .get().isSuccessful()
                    .getJSONObject()
                    .getJSONObject("data").getJSONObject("params").getString("log");
            } catch (Exception e) {
                log = e.getLocalizedMessage();
            }
            logs.put(item.getString("componentName"), log);
        }
        return logs;
    }

    public void releaseAsCustom(AppPackage appPackage, String user) throws IOException, ApiException {

        String url = AppmanagerServiceUtil.getEndpoint() + "/apps/" + appmanagerId(appPackage.getAppId())
            + "/app-packages/" + appPackage.getAppPackageId() + "/release-as-custom";
        new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .postJson(
                "addonId", appPackage.app().getName(),
                "addonVersion", appPackage.getSimpleVersion()
            )
            .post().isSuccessful();
    }

}
