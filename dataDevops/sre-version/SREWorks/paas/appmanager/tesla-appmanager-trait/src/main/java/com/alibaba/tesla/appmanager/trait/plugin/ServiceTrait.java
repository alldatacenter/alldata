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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class ServiceTrait extends BaseTrait {

    public ServiceTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        // 获取 clusterId
        WorkloadResource workloadRef = getWorkloadRef();
        String clusterId = ((JSONObject) workloadRef.getMetadata().getLabels())
                .getString("labels.appmanager.oam.dev/clusterId");
        if (StringUtils.isEmpty(clusterId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find clusterId in workload labels|workload=%s",
                            JSONObject.toJSONString(workloadRef)));
        }

        // 获取基本信息
        String namespace = workloadRef.getMetadata().getNamespace();
        String ownerReference = getOwnerReference();

        // 支持多个 Service 配置
        JSONArray services = getSpec().getJSONArray("services");
        if (services != null && services.size() > 0) {
            for (JSONObject service : services.toJavaList(JSONObject.class)) {
                JSONObject metadata = service.getJSONObject("metadata");
                String name = workloadRef.getMetadata().getName();
                if (metadata != null && StringUtils.isNotEmpty(metadata.getString("name"))) {
                    name = metadata.getString("name");
                }
                JSONObject labels = new JSONObject();
                JSONObject annotations = new JSONObject();
                if (metadata != null && metadata.getJSONObject("labels") != null) {
                    labels = metadata.getJSONObject("labels");
                }
                if (metadata != null && metadata.getJSONObject("annotations") != null) {
                    annotations = metadata.getJSONObject("annotations");
                }
                JSONObject spec = service.getJSONObject("spec");
                if (spec == null) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS, "spec is required in service trait");
                }
                JSONObject cr = generateService(namespace, name, labels, annotations, ownerReference, spec);
                applyService(clusterId, cr, namespace, name, labels, annotations, ownerReference);
            }
        } else {
            String name = workloadRef.getMetadata().getName();
            JSONObject labels = getSpec().getJSONObject("labels");
            JSONObject annotations = getSpec().getJSONObject("annotations");
            getSpec().remove("labels");
            getSpec().remove("annotations");
            labels = labels == null ? new JSONObject() : labels;
            annotations = annotations == null ? new JSONObject() : annotations;
            if (labels.size() == 0) {
                labels = (JSONObject) workloadRef.getMetadata().getLabels();
            }
            if (annotations.size() == 0) {
                annotations = (JSONObject) workloadRef.getMetadata().getAnnotations();
            }
            JSONObject cr = generateService(namespace, name, labels, annotations, ownerReference, getSpec());
            applyService(clusterId, cr, namespace, name, labels, annotations, ownerReference);

            // 写入暴露值
            getSpec().put("serviceName", name);
        }
    }

    /**
     * 应用 Service 到集群中
     *
     * @param clusterId      集群 ID
     * @param cr             CR
     * @param namespace      Namespace
     * @param name           Name
     * @param labels         Labels
     * @param annotations    Annotations
     * @param ownerReference Owner Reference
     */
    private void applyService(
            String clusterId, JSONObject cr, String namespace, String name, JSONObject labels,
            JSONObject annotations, String ownerReference) {
        KubernetesClientFactory clientFactory = SpringBeanUtil.getBean(KubernetesClientFactory.class);
        DefaultKubernetesClient client = clientFactory.get(clusterId);
        // 应用到集群
        try {
            ServiceResource<Service> resource = client.services()
                    .load(new ByteArrayInputStream(cr.toJSONString().getBytes(StandardCharsets.UTF_8)));
            try {
                Service current = client.services().inNamespace(namespace).withName(name).get();
                if (current == null) {
                    Service result = resource.create();
                    log.info("cr yaml has created in kubernetes|cluster={}|namespace={}|name={}|cr={}" +
                                    "result={}", clusterId, namespace, name, cr.toJSONString(),
                            JSONObject.toJSONString(result));
                } else {
                    ObjectMapper mapper = new ObjectMapper();
                    ServiceSpec newSpec = mapper.readValue(cr.getJSONObject("spec").toJSONString(), ServiceSpec.class);
                    Integer healthCheckNodePort = current.getSpec().getHealthCheckNodePort();
                    if (healthCheckNodePort != null && healthCheckNodePort > 0) {
                        newSpec.setHealthCheckNodePort(healthCheckNodePort);
                    }
                    String clusterIp = current.getSpec().getClusterIP();
                    if (StringUtils.isNotEmpty(clusterIp)) {
                        newSpec.setClusterIP(clusterIp);
                    }
                    final JSONObject finalLabels = labels;
                    final JSONObject finalAnnotations = annotations;
                    Service result = client.services()
                            .inNamespace(namespace)
                            .withName(name)
                            .edit(s -> {
                                if (StringUtils.isNotEmpty(ownerReference)) {
                                    try {
                                        return new ServiceBuilder(s)
                                                .editMetadata()
                                                .withLabels(JSON.parseObject(finalLabels.toJSONString(), new TypeReference<Map<String, String>>() {
                                                }))
                                                .withAnnotations(JSON.parseObject(finalAnnotations.toJSONString(), new TypeReference<Map<String, String>>() {
                                                }))
                                                .withOwnerReferences(mapper.readValue(ownerReference, OwnerReference.class))
                                                .endMetadata()
                                                .withSpec(newSpec)
                                                .build();
                                    } catch (JsonProcessingException e) {
                                        throw new RuntimeException(e);
                                    }
                                } else {
                                    return new ServiceBuilder(s)
                                            .editMetadata()
                                            .withLabels(JSON.parseObject(finalLabels.toJSONString(), new TypeReference<Map<String, String>>() {
                                            }))
                                            .withAnnotations(JSON.parseObject(finalAnnotations.toJSONString(), new TypeReference<Map<String, String>>() {
                                            }))
                                            .endMetadata()
                                            .withSpec(newSpec)
                                            .build();
                                }
                            });
                    log.info("cr yaml has updated in kubernetes|cluster={}|namespace={}|name={}|labels={}|" +
                                    "annotations={}|newSpec={}|result={}", clusterId, namespace, name,
                            JSONObject.toJSONString(labels), JSONObject.toJSONString(annotations),
                            JSONObject.toJSONString(newSpec), JSONObject.toJSONString(result));
                }
            } catch (KubernetesClientException e) {
                if (e.getCode() == 422) {
                    log.error("service apply failed, exception={}", ExceptionUtils.getStackTrace(e));
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            String errorMessage = String.format("apply cr yaml to kubernetes failed|cluster=%s|namespace=%s|" +
                            "exception=%s|cr=%s", clusterId, namespace, ExceptionUtils.getStackTrace(e),
                    cr.toJSONString());
            log.error(errorMessage);
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
        }
    }

    /**
     * 创建服务 JSON spec
     *
     * @param namespace   命名空间
     * @param name        标识名称
     * @param labels      Labels
     * @param annotations Annotations
     * @param spec        spec 定义
     * @return JSONObject
     */
    private JSONObject generateService(
            String namespace, String name, JSONObject labels, JSONObject annotations, String ownerReference,
            JSONObject spec) {
        String serviceStr = JSONObject.toJSONString(ImmutableMap.of(
                "apiVersion", "v1",
                "kind", "Service",
                "metadata", ImmutableMap.of(
                        "namespace", namespace,
                        "name", name,
                        "labels", labels,
                        "annotations", annotations
                ),
                "spec", ImmutableMap.of(
                        "selector", ImmutableMap.of(
                                "name", getWorkloadRef().getMetadata().getName()
                        )
                )
        ));
        JSONObject service = JSONObject.parseObject(serviceStr);
        if (StringUtils.isNotEmpty(ownerReference)) {
            service.getJSONObject("metadata").put("ownerReferences", new JSONArray());
            service.getJSONObject("metadata").getJSONArray("ownerReferences")
                    .add(JSONObject.parseObject(ownerReference));
        }
        service.getJSONObject("spec").putAll(spec);
        return service;
    }
}
