package com.alibaba.sreworks.clustermanage.server.controllers;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.K8sUtil;
import com.alibaba.sreworks.common.util.YamlUtil;
import com.alibaba.sreworks.domain.DO.Cluster;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.sreworks.flyadmin.server.services.PluginClusterService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonObject;
import io.kubernetes.client.custom.QuantityFormatter;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.CustomObjectsApi;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.util.Yaml;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/clusterDetail")
public class ClusterDetailController extends BaseController {

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    PluginClusterService pluginClusterService;

    @RequestMapping(value = "nodes", method = RequestMethod.GET)
    public TeslaBaseResult nodes(Long id) throws IOException, ApiException {
        Cluster cluster = clusterRepository.findFirstById(id);
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        CoreV1Api coreV1Api = new CoreV1Api(client);
        V1NodeList nodeList = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, null);
        return buildSucceedResult(nodeList.getItems().stream()
            .map(x -> {
                try {
                    return YamlUtil.toJsonObject(Yaml.dump(x));
                } catch (JsonProcessingException e) {
                    log.error("", e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList())
        );
    }

    @RequestMapping(value = "components", method = RequestMethod.GET)
    public TeslaBaseResult components(Long id) throws IOException {
        Cluster cluster = clusterRepository.findFirstById(id);

        return buildSucceedResult(pluginClusterService.getComponent(cluster, getUserEmployeeId()));
    }

//    private Long parseCpu(String str){
//        Quantity cpu = new Quantity(str);
//        BigDecimal cpuNumber = (new Quantity(str)).getNumber().multiply(new BigDecimal("1000")).multiply(new BigDecimal("1000")).multiply(new BigDecimal("1000"));
//        return cpuNumber.longValue();
//    }

    @RequestMapping(value = "topPods", method = RequestMethod.GET)
    public TeslaBaseResult topPods(Long id) throws IOException, ApiException {
        Cluster cluster = clusterRepository.findFirstById(id);
        if(cluster == null || cluster.getKubeconfig() == null){
            return buildSucceedResult(new JSONArray());
        }
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());

        CustomObjectsApi apiInstance = new CustomObjectsApi(client);
        Object result = apiInstance.listClusterCustomObject(
                "metrics.k8s.io", "v1beta1", "pods",
                null, null, null, null, null, 1000000,
                null, null, null, false);
        JSONObject jsonResult = (JSONObject) JSONObject.toJSON(result);
        JSONArray items = jsonResult.getJSONArray("items");
        QuantityFormatter quantityFormatter = new QuantityFormatter();
        for(int i=0;i<items.size();i++) {
            JSONObject item = items.getJSONObject(i);
            Long cpu = item.getJSONArray("containers").stream().filter(Objects::nonNull).map(o -> (JSONObject) o).map(
                    x -> (new Quantity(x.getJSONObject("usage").getString("cpu"))).getNumber().multiply(new BigDecimal("1000000000")).longValue()
            ).mapToLong(Long::longValue).sum();
            Long memory = item.getJSONArray("containers").stream().filter(Objects::nonNull).map(o -> (JSONObject) o).map(
                     x -> (new Quantity(x.getJSONObject("usage").getString("memory"))).getNumber().divide(new BigDecimal("1024")).longValue()
            ).mapToLong(Long::longValue).sum();
            item.put("cpu", cpu);
            item.put("memory", memory);
            item.put("cpuDisplay", quantityFormatter.format(new Quantity(cpu + "n")));
            item.put("memoryDisplay", quantityFormatter.format(new Quantity(memory + "Ki")));
            if (cpu / 1000 > 1){
                if (cpu / 1000 / 1000 / 1000 <= 1){
                    item.put("cpuDisplay", cpu/1000/1000 + "m");
                }else{
                    item.put("cpuDisplay", cpu/1000/1000/1000);
                }
            }
            if (memory / 1024 > 1){
                if (memory / 1024 / 1024 <= 1){
                    item.put("memoryDisplay", memory / 1024 + "Mi");
                }else if(memory / 1024 / 1024 / 1024 <= 1){
                    item.put("memoryDisplay", memory / 1024 / 1024 + "Gi");
                }else{
                    item.put("memoryDisplay", memory / 1024 / 1024 / 1024 + "Ti");
                }
            }
        }
        List<JSONObject> res = items.stream().map(o -> (JSONObject) o).sorted(
                (x, y) -> Long.valueOf(y.getLong("cpu") - x.getLong("cpu")).intValue()
        ).limit(10).collect(Collectors.toList());
        return buildSucceedResult(res);
    }

    @RequestMapping(value = "resourceCounts", method = RequestMethod.GET)
    public TeslaBaseResult resourceCounts(Long id) throws IOException, ApiException {
        Cluster cluster = clusterRepository.findFirstById(id);
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        if(cluster == null || cluster.getKubeconfig() == null){
            return buildSucceedResult(new JSONObject());
        }

        CoreV1Api coreV1Api = new CoreV1Api(client);
        V1NodeList nodeList = coreV1Api.listNode(null, null, null, null, null, null, null, null, null, null);
        V1NamespaceList nsList = coreV1Api.listNamespace(null, null, null, null, null, null, null, null, null, null);
        V1ServiceList svcList = coreV1Api.listServiceForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
        V1PodList podList = coreV1Api.listPodForAllNamespaces(null, null, null, null, null, null, null, null, null, null);
//        Object result = (new CustomObjectsApi(client)).listClusterCustomObject(
//                "metrics.k8s.io", "v1beta1", "pods",
//                null, null, null, null, null, 1000000,
//                null, null, null, false);
//        JSONObject jsonResult = (JSONObject) JSONObject.toJSON(result);
//        JSONArray podList = jsonResult.getJSONArray("items");
        JSONObject podStatusMap = new JSONObject();
        for(V1Pod pod: podList.getItems()){
            String status = pod.getStatus().getPhase();
            if(podStatusMap.containsKey(status)){
                podStatusMap.put(status, podStatusMap.getInteger(status) + 1);
            }else{
                podStatusMap.put(status, 1);
            }
        }

        JSONObject res = new JSONObject();
        res.put("node", nodeList.getItems().size());
        res.put("namespace", nsList.getItems().size());
        res.put("pod", podList.getItems().size());
        res.put("service", svcList.getItems().size());
        res.put("podStatusMap", podStatusMap);
        return buildSucceedResult(res);
    }

    @RequestMapping(value = "system-components", method = RequestMethod.GET)
    public TeslaBaseResult systemComponents(Long id) throws IOException, ApiException {
        Cluster cluster = clusterRepository.findFirstById(id);
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        CoreV1Api coreV1Api = new CoreV1Api(client);
        V1PodList podList = coreV1Api.listNamespacedPod("kube-system", null, null, null,null, null, null, null, null, null, null);

        List<String> images = podList.getItems().stream().map(V1Pod::getSpec).map(podSpec -> {
            List<V1Container> containers = podSpec.getContainers();
            if (CollectionUtils.isEmpty(containers)) {
                return null;
            }
            return containers;
        }).filter(Objects::nonNull)
                .flatMap(Collection::stream).map(V1Container::getImage).distinct().collect(Collectors.toList());

        List<Map<String, String>> imagesObjectList = images.stream().map(image -> {
            String[] items = image.split("/");
            String im = items[items.length - 1];
            String[] vs = im.split(":");
            Map<String, String> map = new HashMap<>(8);
            map.put("name", vs[0]);
            map.put("version", vs[1]);
            return map;
        }).collect(Collectors.toList());

        return buildSucceedResult(imagesObjectList);
    }

}
