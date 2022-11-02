package com.alibaba.sreworks.appcenter.server.services;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.K8sUtil;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DO.Cluster;
import com.alibaba.sreworks.domain.DTO.Port;
import com.alibaba.sreworks.domain.repository.AppComponentInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppInstanceRepository;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.sreworks.flyadmin.server.services.PluginClusterService;

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.ExtensionsV1beta1Api;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1HTTPIngressPath;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1HTTPIngressRuleValue;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1IngressBackend;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1IngressRule;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1IngressSpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.dynamic.DynamicKubernetesApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppComponentInstanceService {

    @Autowired
    AppComponentInstanceRepository appComponentInstanceRepository;

    @Autowired
    AppInstanceRepository appInstanceRepository;

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    PluginClusterService pluginClusterService;

    public void stop(Long id) throws IOException {
        AppComponentInstance appComponentInstance = appComponentInstanceRepository.findFirstById(id);
        AppInstance appInstance = appInstanceRepository.findFirstById(appComponentInstance.getAppInstanceId());
        Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
        setReplicas(appInstance, appComponentInstance, cluster, 0L);
    }

    public void start(Long id) throws IOException {
        AppComponentInstance appComponentInstance = appComponentInstanceRepository.findFirstById(id);
        AppInstance appInstance = appInstanceRepository.findFirstById(appComponentInstance.getAppInstanceId());
        Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
        setReplicas(appInstance, appComponentInstance, cluster, appComponentInstance.detail().getReplicas());
    }

    public void restart(Long id) throws IOException, ApiException {
        AppComponentInstance appComponentInstance = appComponentInstanceRepository.findFirstById(id);
        AppInstance appInstance = appInstanceRepository.findFirstById(appComponentInstance.getAppInstanceId());
        Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
        deleteDeployment(appInstance, appComponentInstance, cluster);
    }

    private void deleteDeployment(AppInstance appInstance, AppComponentInstance appComponentInstance, Cluster cluster)
        throws IOException, ApiException {
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        AppsV1Api appsV1Api = new AppsV1Api(client);
        log.info("{} {} ", appInstance.namespace(), appComponentInstance.microserviceName(appInstance));
        appsV1Api.deleteNamespacedDeployment(
            appComponentInstance.microserviceName(appInstance), appInstance.namespace(),
            null, null, null, null, null, null
        );
    }

    private void setReplicas(AppInstance appInstance, AppComponentInstance appComponentInstance, Cluster cluster,
        Long replicas) throws IOException {
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        DynamicKubernetesApi dynamicKubernetesApi = new DynamicKubernetesApi(
            "apps.abm.io", "v1", "microservices", client);
        JSONArray patchArray = JsonUtil.list(JsonUtil.map(
            "op", "replace",
            "path", "/spec/replicas",
            "value", replicas
        ));
        V1Patch patch = new V1Patch(JSONObject.toJSONString(patchArray));
        dynamicKubernetesApi.patch(appInstance.namespace(), appComponentInstance.microserviceName(appInstance),
            "replace", patch);
    }

    public String getIngressHost(
        AppInstance appInstance, AppComponentInstance appComponentInstance, Port port) {
        try {
            Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
            String microserviceName = appComponentInstance.microserviceName(appInstance);
            String ingressName = microserviceName + "-" + port.getName();
            ApiClient client = K8sUtil.client(cluster.getKubeconfig());
            ExtensionsV1beta1Api api = new ExtensionsV1beta1Api(client);
            ExtensionsV1beta1Ingress ingress = api.readNamespacedIngress(
                ingressName, appInstance.namespace(), null, null, null);
            ExtensionsV1beta1IngressSpec spec = ingress.getSpec();
            if (spec == null) {
                return "";
            }
            List<ExtensionsV1beta1IngressRule> rules = spec.getRules();
            if (rules == null) {
                return "";
            }
            return rules.get(0).getHost();
        } catch (ApiException e) {
            log.error("" + e.getResponseBody(), e);
        } catch (Exception e) {
            log.error("", e);
        }
        return "";
    }

    public void createIngress(Long id, Port port, String user) throws IOException, ApiException {
        AppComponentInstance appComponentInstance = appComponentInstanceRepository.findFirstById(id);
        AppInstance appInstance = appInstanceRepository.findFirstById(appComponentInstance.getAppInstanceId());
        Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
        String microserviceName = appComponentInstance.microserviceName(appInstance);

        String ingressHost = pluginClusterService.getIngressHost(
            cluster, microserviceName + "-" + port.getName() + "-" + appInstance.namespace(), user);
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        ExtensionsV1beta1Api api = new ExtensionsV1beta1Api(client);
        ExtensionsV1beta1Ingress body = new ExtensionsV1beta1Ingress()
            .apiVersion("extensions/v1beta1")
            .kind("Ingress")
            .metadata(new V1ObjectMeta()
                .name(microserviceName + "-" + port.getName())
                .namespace(appInstance.namespace())
            )
            .spec(new ExtensionsV1beta1IngressSpec()
                .rules(Collections.singletonList(new ExtensionsV1beta1IngressRule()
                    .host(ingressHost)
                    .http(new ExtensionsV1beta1HTTPIngressRuleValue()
                        .paths(Collections.singletonList(new ExtensionsV1beta1HTTPIngressPath()
                            .backend(new ExtensionsV1beta1IngressBackend()
                                .serviceName(microserviceName)
                                .servicePort(new IntOrString(port.getValue().intValue()))
                            )
                            .path("/")
                            .pathType("ImplementationSpecific")
                        ))
                    )
                ))
            );
        log.info("{} {}", appInstance.namespace(), body.toString());
        api.createNamespacedIngress(appInstance.namespace(), body, null, null, null);
    }
}
