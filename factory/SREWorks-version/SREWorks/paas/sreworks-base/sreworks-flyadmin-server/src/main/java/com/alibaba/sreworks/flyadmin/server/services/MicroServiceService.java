package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.K8sUtil;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DO.Cluster;
import com.alibaba.sreworks.domain.repository.ClusterRepository;

import com.google.gson.JsonObject;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.util.Yaml;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MicroServiceService {

    @Autowired
    ClusterRepository clusterRepository;

    private DynamicKubernetesApi dynamicApi(AppInstance appInstance) throws IOException {
        Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        return new DynamicKubernetesApi("apps.abm.io", "v1", "microservices", client);
    }

    private String msName(AppInstance appInstance, AppComponentInstance appComponentInstance) {
        return String.format("%s-sreworks%s-%s",
            appInstance.getStageId(), appInstance.getAppId(), appComponentInstance.getName());
    }

    public JSONObject get(AppInstance appInstance, AppComponentInstance appComponentInstance) throws IOException {
        DynamicKubernetesApi dynamicApi = dynamicApi(appInstance);
        String msName = msName(appInstance, appComponentInstance);
        JsonObject raw = dynamicApi.get(appInstance.namespace(), msName).getObject().getRaw();
        return JSONObject.parseObject(raw.toString());
    }

    public void delete(AppInstance appInstance, AppComponentInstance appComponentInstance) throws IOException {
        DynamicKubernetesApi dynamicApi = dynamicApi(appInstance);
        String msName = msName(appInstance, appComponentInstance);
        dynamicApi.delete(appInstance.namespace(), msName);
    }

    public JSONObject getService(AppInstance appInstance, AppComponentInstance appComponentInstance)
        throws IOException, ApiException {
        Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        CoreV1Api api = new CoreV1Api(client);
        V1Service service = api.readNamespacedService(
            msName(appInstance, appComponentInstance), appInstance.namespace(), null);
        return YamlUtil.toJsonObject(Yaml.dump(service));
    }

    public void metricOn(AppInstance appInstance, AppComponentInstance appComponentInstance)
        throws IOException, ApiException {
        Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        new CoreV1Api(client).patchNamespacedService(
            msName(appInstance, appComponentInstance),
            appInstance.namespace(),
            new V1Patch(JSONObject.toJSONString(JsonUtil.list(JsonUtil.map(
                "op", "replace",
                "path", "/metadata/labels/sreworks-telemetry",
                "value", "metric"
            )))),
            null, null, null, null
        );
        new AppsV1Api(client).patchNamespacedDeployment(
            msName(appInstance, appComponentInstance),
            appInstance.namespace(),
            new V1Patch(JSONObject.toJSONString(JsonUtil.list(JsonUtil.map(
                "op", "replace",
                "path", "/spec/template/metadata/labels/sreworks-telemetry",
                "value", "log"
            )))),
            null, null, null, null
        );
    }

    public void metricOff(AppInstance appInstance, AppComponentInstance appComponentInstance)
        throws IOException, ApiException {
        Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        new CoreV1Api(client).patchNamespacedService(
            msName(appInstance, appComponentInstance),
            appInstance.namespace(),
            new V1Patch(JSONObject.toJSONString(JsonUtil.list(JsonUtil.map(
                "op", "replace",
                "path", "/metadata/labels/sreworks-telemetry",
                "value", "metricOff"
            )))),
            null, null, null, null
        );
        new AppsV1Api(client).patchNamespacedDeployment(
            msName(appInstance, appComponentInstance),
            appInstance.namespace(),
            new V1Patch(JSONObject.toJSONString(JsonUtil.list(JsonUtil.map(
                "op", "replace",
                "path", "/spec/template/metadata/labels/sreworks-telemetry",
                "value", "logOff"
            )))),
            null, null, null, null
        );
    }

}
