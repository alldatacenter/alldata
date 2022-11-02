package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.AppmanagerServiceUtil;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.sreworks.common.util.UuidUtil;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DO.ClusterResource;
import com.alibaba.sreworks.domain.DTO.AppComponentInstanceDetail;
import com.alibaba.sreworks.domain.DTO.Config;
import com.alibaba.sreworks.domain.DTO.Port;
import com.alibaba.sreworks.domain.DTO.Volume;
import com.alibaba.sreworks.domain.repository.AppComponentRepository;
import com.alibaba.sreworks.domain.repository.AppPackageRepository;
import com.alibaba.sreworks.domain.repository.ClusterResourceRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeploy.FlyadminAppmanagerDeployAppPackageAcService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeploy.FlyadminAppmanagerDeployHelmAcService;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeploy.FlyadminAppmanagerDeployRepoAcService;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.auxiliary.AuxiliaryType.SignatureRelevant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import static com.alibaba.sreworks.domain.utils.AppUtil.appmanagerId;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Service
public class FlyadminAppmanagerDeployService {

    @Autowired
    AppComponentRepository appComponentRepository;

    @Autowired
    ClusterResourceRepository clusterResourceRepository;

    @Autowired
    AppPackageRepository appPackageRepository;

    @Autowired
    FlyadminAppmanagerDeployRepoAcService flyadminAppmanagerDeployRepoAcService;

    @Autowired
    FlyadminAppmanagerDeployAppPackageAcService flyadminAppmanagerDeployAppPackageAcService;

    @Autowired
    FlyadminAppmanagerDeployHelmAcService flyadminAppmanagerDeployHelmAcService;

    public List<JSONObject> list(Long appId, String user) throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/deployments")
            .params("appId", appmanagerId(appId), "page", 1, "pageSize", 10000)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data").getJSONArray("items").toJavaList(JSONObject.class);
    }

    public JSONObject listPagination(
        Long appId, String user,
        String page, String pageSize,
        String stageIdWhiteList, String optionKey, String optionValue
    ) throws IOException, ApiException {
        JSONObject payload = new JSONObject();

        if(appId != null){
            payload.put("appId", appmanagerId(appId));
        }
        if(stageIdWhiteList != null) {
            payload.put("stageIdWhiteList", stageIdWhiteList);
        }
        if(optionKey != null){
            payload.put("optionKey", optionKey);
        }
        if(optionValue != null){
            payload.put("optionValue", optionValue);

        }
        payload.put("page", page);
        payload.put("pageSize", pageSize);

        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/deployments")
            .params(payload)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data");
//            .getJSONObject("data").getJSONArray("items").toJavaList(JSONObject.class);
    }

    public JSONObject get(String deployId, String user) throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/deployments/" + deployId)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data");
    }

    public String start(AppInstance appInstance, List<AppComponentInstance> appComponentInstanceList)
        throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/deployments/launch")
            .postJson(getAc(appInstance, appComponentInstanceList))
            .headers(HttpHeaderNames.X_EMPL_ID, appInstance.getLastModifier())
            .post().isSuccessful()
            .getJSONObject()
            .getJSONObject("data").getString("deployAppId");
    }

    private JSONObject getAcMeta(AppInstance appInstance) {
        return JsonUtil.map(
            "apiVersion", "core.oam.dev/v1alpha2",
            "kind", "ApplicationConfiguration",
            "metadata", JsonUtil.map(
                "name", appmanagerId(appInstance.getAppId()),
                "annotations", JsonUtil.map(
                    "appId", appmanagerId(appInstance.getAppId()),
                    "appPackageId", appPackageRepository.findFirstById(appInstance.getAppPackageId()).getAppPackageId(),
                    "clusterId", appInstance.getClusterId() + "id",
                    "namespaceId", appInstance.namespace(),
                    "stageId", appInstance.getStageId()
                )
            ),
            "spec", JsonUtil.map(
                "parameterValues", JsonUtil.list(
                    JsonUtil.map(
                        "name", "CLUSTER_ID",
                        "value", appInstance.getClusterId() + "id"
                    ),
                    JsonUtil.map(
                        "name", "NAMESPACE_ID",
                        "value", appInstance.namespace()
                    ),
                    JsonUtil.map(
                        "name", "STAGE_ID",
                        "value", appInstance.getStageId()
                    ),
                    JsonUtil.map(
                        "name", "ABM_CLUSTER",
                        "value", "default-cluster"
                    ),
                    JsonUtil.map(
                        "name", "CLOUD_TYPE",
                        "value", "PaaS"
                    ),
                    JsonUtil.map(
                        "name", "ENV_TYPE",
                        "value", "PaaS"
                    ),
                    JsonUtil.map(
                        "name", "ENDPOINT_PAAS_PRODUCTOPS",
                        "value", "prod-flycore-paas-productops"
                    )
                ),
                "components", new JSONArray()
            )
        );
    }

    public String getAc(AppInstance appInstance, List<AppComponentInstance> appComponentInstanceList)
        throws IOException, ApiException {
        JSONObject acJsonObject = getAcMeta(appInstance);
        JSONArray components = acJsonObject.getJSONObject("spec").getJSONArray("components");
        for (AppComponentInstance appComponentInstance : appComponentInstanceList) {
            switch (appComponentInstance.type()) {
                case REPO:
                    flyadminAppmanagerDeployRepoAcService.patchAc(components, appInstance, appComponentInstance);
                    break;
                case APP_PACKAGE:
                    flyadminAppmanagerDeployAppPackageAcService.patchAc(components, appInstance, appComponentInstance);
                    break;
                case HELM:
                    flyadminAppmanagerDeployHelmAcService.patchAc(components, appInstance, appComponentInstance);
                default:
                    break;
            }
        }
        log.info(YamlUtil.toYaml(acJsonObject));
        return YamlUtil.toYaml(acJsonObject);
    }

    public JSONObject logs(String id, String user) throws IOException, ApiException {
        return new Requests(AppmanagerServiceUtil.getEndpoint() + "/deployments/" + id + "/attributes")
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONObject()
            .getJSONObject("data");
    }

}
