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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressSpec;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Networking Ingress Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class NetworkingIngressTrait extends BaseTrait {

    public NetworkingIngressTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        WorkloadResource workloadResource = getWorkloadRef();
        String namespace = workloadResource.getMetadata().getNamespace();

        // 获取指定 cluster 的 kubernetes client
        WorkloadResource workloadRef = getWorkloadRef();
        KubernetesClientFactory clientFactory = SpringBeanUtil.getBean(KubernetesClientFactory.class);
        String clusterId = ((JSONObject) workloadRef.getMetadata().getLabels())
                .getString("labels.appmanager.oam.dev/clusterId");
        if (StringUtils.isEmpty(clusterId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find clusterId in workload labels|workload=%s",
                            JSONObject.toJSONString(workloadRef)));
        }
        DefaultKubernetesClient client = clientFactory.get(clusterId);

        JSONArray records = getSpec().getJSONArray("ingress");
        for (JSONObject record : records.toJavaList(JSONObject.class)) {
            String name = record.getString("name");
            JSONObject labels = new JSONObject();
            if (record.getJSONObject("labels") != null) {
                labels.putAll(record.getJSONObject("labels"));
            }
            JSONObject annotations = new JSONObject();
            if (record.getJSONObject("annotations") != null) {
                annotations.putAll(record.getJSONObject("annotations"));
            }
            JSONObject spec = record.getJSONObject("spec");
            JSONObject cr = generateIngress(namespace, name, labels, annotations, spec);

            // 应用到集群
            try {
                Resource<Ingress> resource = client.network().ingresses()
                        .load(new ByteArrayInputStream(cr.toJSONString().getBytes(StandardCharsets.UTF_8)));
                Ingress current = client.network().ingresses().inNamespace(namespace).withName(name).get();
                if (current == null) {
                    Ingress result = resource.create();
                    log.info("cr yaml has created in kubernetes|cluster={}|namespace={}|name={}|cr={}" +
                                    "result={}", clusterId, namespace, name, cr.toJSONString(),
                            JSONObject.toJSONString(result));
                } else {
                    ObjectMapper mapper = new ObjectMapper();
                    IngressSpec newSpec = mapper.readValue(cr.getJSONObject("spec").toJSONString(), IngressSpec.class);
                    final JSONObject finalLabels = labels;
                    final JSONObject finalAnnotations = annotations;
                    client.network().ingresses()
                            .inNamespace(namespace)
                            .withName(name)
                            .edit(s -> new IngressBuilder(s)
                                    .editMetadata()
                                    .withLabels(JSON.parseObject(finalLabels.toJSONString(), new TypeReference<Map<String, String>>() {}))
                                    .withAnnotations(JSON.parseObject(finalAnnotations.toJSONString(), new TypeReference<Map<String, String>>() {}))
                                    .endMetadata()
                                    .withSpec(newSpec)
                                    .build());
                    log.info("cr yaml has updated in kubernetes|cluster={}|namespace={}|name={}|labels={}|" +
                                    "annotations={}|newSpec={}", clusterId, namespace, name,
                            JSONObject.toJSONString(labels), JSONObject.toJSONString(annotations),
                            JSONObject.toJSONString(newSpec));
                }
            } catch (Exception e) {
                String errorMessage = String.format("apply cr yaml to kubernetes failed|cluster=%s|namespace=%s|" +
                                "exception=%s|cr=%s", clusterId, namespace, ExceptionUtils.getStackTrace(e),
                        cr.toJSONString());
                log.error(errorMessage);
                throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
            }
        }
    }

    /**
     * 创建 Ingress JSON spec
     *
     * @param namespace 命名空间
     * @param name      标识名称
     * @param spec      spec 定义
     * @return JSONObject
     */
    private JSONObject generateIngress(
        String namespace, String name, Object labels, Object annotations, JSONObject spec) {
        String ingressStr = JSONObject.toJSONString(ImmutableMap.of(
            "apiVersion", "networking.k8s.io/v1beta1",
            "kind", "Ingress",
            "metadata", ImmutableMap.of(
                "namespace", namespace,
                "name", name,
                "labels", labels,
                "annotations", annotations
            )
        ));
        JSONObject ingress = JSONObject.parseObject(ingressStr);
        ingress.put("spec", spec);
        return ingress;
    }
}
