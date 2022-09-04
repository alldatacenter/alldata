package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory;
import com.alibaba.tesla.appmanager.spring.util.SpringBeanUtil;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/*
 *
 * https://yuque.antfin.com/docs/share/ecd6f47b-815c-4e5f-8cca-76ba124d787a?# 《appmanager configmap trait 设计》
 *  traits:
 *  - name: configmap.trait.abm.io
 *    spec:
 *      targets:
 *      - type: env
 *        initContainer: db-migration
 *        configmap: cm1
 *      - type: volumeMount
 *        container: server
 *        mountPath: /etc/config
 *        configmap: cm2
 *      - type: envValue
 *        container: server
 *        values:
 *        - name: kkk
 *          configmap: cm1
 *          key: a
 *      configmaps:
 *        cm1:
 *          a: b
 *          a1: b1
 *        cm2:
 *          a: b
 *          a1: b1
 *
 */

/**
 * Configmap Trait
 *
 * @author jiongen.zje@alibaba-inc.com
 */
@Slf4j
public class ConfigmapTrait extends BaseTrait {

    public ConfigmapTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        /**
         * 1. get metadata from workload
         */
        log.info("start execute configmap trait {}", getSpec().toJSONString());

        WorkloadResource workloadResource = getWorkloadRef();
        String namespace = workloadResource.getMetadata().getNamespace();
        JSONObject labels = getSpec().getJSONObject("labels");
        JSONObject annotations = getSpec().getJSONObject("annotations");
        getSpec().remove("labels");
        getSpec().remove("annotations");
        labels = labels == null ? new JSONObject() : labels;
        annotations = annotations == null ? new JSONObject() : annotations;
        if (labels.size() == 0) {
            labels = (JSONObject) workloadResource.getMetadata().getLabels();
        }
        if (annotations.size() == 0) {
            annotations = (JSONObject) workloadResource.getMetadata().getAnnotations();
        }

        /**
         * 2. get k8s cluster info
         */
        WorkloadResource workloadRef = getWorkloadRef();
        KubernetesClientFactory clientFactory = SpringBeanUtil.getBean(KubernetesClientFactory.class);
        String clusterId = getComponent().getClusterId();
        if (StringUtils.isEmpty(clusterId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find clusterId in workload labels|workload=%s",
                            JSONObject.toJSONString(workloadRef)));
        }
        DefaultKubernetesClient client = clientFactory.get(clusterId);

        /**
         * 3. get configmaps and apply to k8s cluster
         */
        JSONObject configmaps = getSpec().getJSONObject("configmaps");
        log.info("start to apply configmap in configmap trait {}", configmaps.toJSONString());
        for (Map.Entry<String, Object> item : configmaps.entrySet()) {
            String configmapName = item.getKey();
            Object configmapData = item.getValue();
            JSONObject configmap = generateConfigmap(namespace, configmapName, configmapData, labels, annotations);
            applyConfigmap(client, configmap, labels, annotations);
        }


        /**
         4. patch targets in values
         */
        JSONArray targets = getSpec().getJSONArray("targets");
        if (targets == null) {
            return;
        }
        for (int i = 0; i < targets.size(); i++) {
            JSONObject target = targets.getJSONObject(i);
            if ("env".equals(target.getString("type"))) {
                /**
                 *   containers:
                 *   - name: test-container
                 *     image: k8s.gcr.io/busybox
                 *     command: [ "/bin/sh", "-c", "env" ]
                 *     envFrom:
                 *     - configMapRef:
                 *         name: special-config
                 */
                if (target.getString("configmap") == null) {
                    String errorMessage = String.format("configmap key not found %s", target.toJSONString());
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
                }
                JSONArray updateDatas = new JSONArray();
                updateDatas.add(JSONObject.parseObject(JSONObject.toJSONString(ImmutableMap.of(
                                "configMapRef", ImmutableMap.of(
                                        "name", target.getString("configmap"))
                        )
                )));
                if (target.getString("initContainer") != null) {
                    updateContainers("initContainer", target.getString("initContainer"), "envFrom", updateDatas);
                } else if (target.getString("container") != null) {
                    updateContainers("container", target.getString("container"), "envFrom", updateDatas);
                } else {
                    String errorMessage = String.format("not found container or initContainer %s", target.toJSONString());
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
                }
            } else if ("volumeMount".equals(target.getString("type"))) {
                /**
                 *  containers:
                 *  - name: test-container
                 *    image: k8s.gcr.io/busybox
                 *    command: [ "/bin/sh", "-c", "ls /etc/config/" ]
                 *    volumeMounts:
                 *    - name: config-volume
                 *      mountPath: /etc/config
                 *  volumes:
                 *     - name: config-volume
                 *       configMap:
                 *         name: special-config
                 */
                if (target.getString("configmap") == null || target.getString("mountPath") == null) {
                    String errorMessage = String.format("configmap or mountPath not found %s", target.toJSONString());
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
                }
                JSONArray volumes = new JSONArray();
                volumes.add(JSONObject.parseObject(JSONObject.toJSONString(ImmutableMap.of(
                        "name", target.getString("configmap"),
                        "configMap", ImmutableMap.of(
                                "name", target.getString("configmap")
                        )
                ))));
                updateVolumes(volumes);
                JSONArray updateDatas = new JSONArray();
                if (StringUtils.isNotEmpty(target.getString("subPath"))) {
                    updateDatas.add(JSONObject.parseObject(JSONObject.toJSONString(ImmutableMap.of(
                            "name", target.getString("configmap"),
                            "mountPath", target.getString("mountPath"),
                            "subPath", target.getString("subPath")
                    ))));
                } else {
                    updateDatas.add(JSONObject.parseObject(JSONObject.toJSONString(ImmutableMap.of(
                            "name", target.getString("configmap"),
                            "mountPath", target.getString("mountPath")
                    ))));
                }
                if (target.getString("initContainer") != null) {
                    updateContainers("initContainer", target.getString("initContainer"), "volumeMounts", updateDatas);
                } else if (target.getString("container") != null) {
                    updateContainers("container", target.getString("container"), "volumeMounts", updateDatas);
                } else {
                    String errorMessage = String.format("not found container or initContainer %s", target.toJSONString());
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
                }
            } else if ("envValue".equals(target.getString("type"))) {
                /**
                 *   containers:
                 *     - name: test-container
                 *       image: k8s.gcr.io/busybox
                 *       command: [ "/bin/sh", "-c", "echo $(SPECIAL_LEVEL_KEY) $(SPECIAL_TYPE_KEY)" ]
                 *       env:
                 *         - name: SPECIAL_LEVEL_KEY
                 *           valueFrom:
                 *             configMapKeyRef:
                 *               name: special-config
                 *               key: SPECIAL_LEVEL
                 */
                JSONArray updateEnvs = new JSONArray();
                for (int j = 0; j < target.getJSONArray("values").size(); j++) {
                    JSONObject v = target.getJSONArray("values").getJSONObject(j);
                    updateEnvs.add(JSONObject.parseObject(JSONObject.toJSONString(ImmutableMap.of(
                            "name", v.getString("name"),
                            "valueFrom", ImmutableMap.of(
                                    "configMapKeyRef", ImmutableMap.of(
                                            "name", v.getString("configmap"),
                                            "key", v.getString("key")
                                    )
                            )
                    ))));
                }
                if (target.getString("initContainer") != null) {
                    updateContainers("initContainer", target.getString("initContainer"), "env", updateEnvs);
                } else if (target.getString("container") != null) {
                    updateContainers("container", target.getString("container"), "env", updateEnvs);
                } else {
                    String errorMessage = String.format("not found container or initContainer %s", target.toJSONString());
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
                }
            }
        }
    }

    /**
     * update containers in string
     *
     * @param type        container or initContainer
     * @param name
     * @param targetKey
     * @param updateDatas
     * @return
     */
    private void updateContainers(String type, String name, String targetKey, JSONArray updateDatas) {
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        log.info("configmap trait parent workload {}", workloadSpec.toJSONString());
        JSONArray containers;
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            containers = cloneSetSpec.getJSONArray(type + "s");
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            containers = advancedStatefulSetSpec.getJSONArray(type + "s");
        } else if ("Deployment".equals(workloadSpec.getString("kind"))) {
            containers = workloadSpec.getJSONArray(type + "s");
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "not supported");
        }
        for (int i = 0; i < containers.size(); i++) {
            JSONObject container = containers.getJSONObject(i);
            if (!Objects.equals(container.getString("name"), name)) {
                log.info("container name not match {} {}", container.getString("name"), name);
                continue;
            }
            container.putIfAbsent(targetKey, new JSONArray());
            JSONArray target = container.getJSONArray(targetKey);
            for (int j = 0; j < updateDatas.size(); j++) {
                JSONObject updateData = updateDatas.getJSONObject(i);
                log.info("update container {} {}", target.toJSONString(), updateData.toJSONString());
                target.add(updateData);
            }
        }
        log.info("configmap trait parent workload after update {}", workloadSpec.toJSONString());
    }

    /**
     * update volumes in string
     *
     * @param updateDatas
     * @return
     */
    private void updateVolumes(JSONArray updateDatas) {
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        JSONArray volumes;
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            volumes = cloneSetSpec.getJSONArray("volumes");
            if (volumes == null) {
                cloneSetSpec.put("volumes", new JSONArray());
                volumes = cloneSetSpec.getJSONArray("volumes");
            }
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            volumes = advancedStatefulSetSpec.getJSONArray("volumes");
            if (volumes == null) {
                advancedStatefulSetSpec.put("volumes", new JSONArray());
                volumes = advancedStatefulSetSpec.getJSONArray("volumes");
            }
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "not supported");
        }
        for (int i = 0; i < updateDatas.size(); i++) {
            JSONObject updateData = updateDatas.getJSONObject(i);
            volumes.add(updateData);
        }
    }

    /**
     * 创建配置文件 Configmap
     *
     * @param namespace 命名空间
     * @param name      标识名称
     * @param data      配置数据
     * @return JSONObject
     */
    private JSONObject generateConfigmap(
            String namespace, String name, Object data, Object labels, Object annotations) {
        String configmapStr = JSONObject.toJSONString(ImmutableMap.of(
                "apiVersion", "v1",
                "kind", "ConfigMap",
                "metadata", ImmutableMap.of(
                        "namespace", namespace,
                        "name", name,
                        "labels", labels,
                        "annotations", annotations
                ),
                "data", data
        ));
        JSONObject configmap = JSONObject.parseObject(configmapStr);
        return configmap;
    }

    /**
     * configmap apply k8s cluster
     *
     * @param client      已经被实例化的k8s连接客户端
     * @param configmap   待提交的configmap cr对象
     * @param labels
     * @param annotations
     * @return
     */
    private void applyConfigmap(DefaultKubernetesClient client, JSONObject configmap, JSONObject labels, JSONObject annotations) {
        // 应用到集群
        try {
//            String namespace = configmap.getJSONObject("metadata").getString("namespace");
            String namespace = getComponent().getNamespaceId();
            String name = configmap.getJSONObject("metadata").getString("name");
            Resource<ConfigMap> resource = client.configMaps()
                    .load(new ByteArrayInputStream(configmap.toJSONString().getBytes(StandardCharsets.UTF_8)));
            try {
                ConfigMap current = client.configMaps().inNamespace(namespace).withName(name).get();
                if (current == null) {
                    ConfigMap result = resource.create();
                    log.info("cr yaml has created in kubernetes|cluster={}|namespace={}|name={}|cr={}" +
                                    "result={}", client, namespace, name, configmap.toJSONString(),
                            JSONObject.toJSONString(result));
                } else {
                    Map<String, String> newData = new HashMap<>();
                    for (Map.Entry<String, Object> entry : configmap.getJSONObject("data").entrySet()) {
                        newData.put(entry.getKey(), String.valueOf(entry.getValue()));
                    }
                    JSONObject finalLabels = labels;
                    JSONObject finalAnnotations = annotations;
                    ConfigMap result = client.configMaps()
                            .inNamespace(namespace)
                            .withName(name)
                            .edit(s -> new ConfigMapBuilder(s)
                                    .editMetadata()
                                    .withLabels(JSON.parseObject(finalLabels.toJSONString(), new TypeReference<Map<String, String>>() {
                                    }))
                                    .withAnnotations(JSON.parseObject(finalAnnotations.toJSONString(), new TypeReference<Map<String, String>>() {
                                    }))
                                    .endMetadata()
                                    .withData(newData)
                                    .build());
                    log.info("cr yaml has updated in kubernetes|cluster={}|namespace={}|name={}|labels={}|" +
                                    "annotations={}|newData={}|result={}", client, namespace, name,
                            JSONObject.toJSONString(labels), JSONObject.toJSONString(annotations),
                            JSONObject.toJSONString(newData), JSONObject.toJSONString(result));
                }
            } catch (KubernetesClientException e) {
                if (e.getCode() == 422) {
                    log.error("service apply failed, exception={}", ExceptionUtils.getStackTrace(e));
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            String errorMessage = String.format("apply cr yaml to kubernetes failed|cluster=%s|" +
                            "exception=%s|cr=%s", client, ExceptionUtils.getStackTrace(e),
                    configmap.toJSONString());
            log.error(errorMessage);
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
        }
    }
}
