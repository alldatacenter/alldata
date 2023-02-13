package com.alibaba.tesla.appmanager.trait.plugin;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.domain.core.WorkloadResource;
import com.alibaba.tesla.appmanager.domain.schema.TraitDefinition;
import com.alibaba.tesla.appmanager.kubernetes.KubernetesClientFactory;
import com.alibaba.tesla.appmanager.spring.util.SpringBeanUtil;
import com.alibaba.tesla.appmanager.trait.BaseTrait;
import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * PVC Trait
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Slf4j
public class PvcTrait extends BaseTrait {

    public PvcTrait(String name, TraitDefinition traitDefinition, JSONObject spec, WorkloadResource ref) {
        super(name, traitDefinition, spec, ref);
    }

    @Override
    public void execute() {
        log.info("start execute pvc trait {}", getSpec().toJSONString());

        // 获取 Kubernetes Client
        WorkloadResource workloadRef = getWorkloadRef();
        KubernetesClientFactory clientFactory = SpringBeanUtil.getBean(KubernetesClientFactory.class);
        String clusterId = getComponent().getClusterId();
        if (StringUtils.isEmpty(clusterId)) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                    String.format("cannot find clusterId in workload labels|workload=%s",
                            JSONObject.toJSONString(workloadRef)));
        }
        DefaultKubernetesClient client = clientFactory.get(clusterId);

        // patch targets in values
        JSONObject pvcs = getSpec().getJSONObject("pvcs");
        JSONArray targets = getSpec().getJSONArray("targets");
        for (int i = 0; i < targets.size(); i++) {
            JSONObject target = targets.getJSONObject(i);

            /**
             *  containers:
             *  - name: test-container
             *    image: k8s.gcr.io/busybox
             *    command: [ "/bin/sh", "-c", "ls /etc/config/" ]
             *    volumeMounts:
             *    - name: www
             *      mountPath: /usr/share/nginx/html
             *  volumeClaimTemplates:
             *  - metadata:
             *      name: www
             *    spec:
             *      accessModes: [ "ReadWriteOnce" ]
             *      resources:
             *        requests:
             *          storage: 1Gi
             */
            if (target.getString("pvc") == null || target.getString("mountPath") == null) {
                String errorMessage = String.format("pvc or mountPath not found %s", target.toJSONString());
                throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
            }

            String type = target.getString("type");
            if (StringUtils.isEmpty(type)) {
                // type 为空时的默认行为，注入自身的 volumeClaimTemplates 中
                addVolumeClaimTemplate(JSONObject.parseObject(JSONObject.toJSONString(ImmutableMap.of(
                        "metadata", ImmutableMap.of(
                                "name", target.getString("pvc")
                        ),
                        "spec", pvcs.getJSONObject(target.getString("pvc"))
                ))));
            } else if ("innerPvc".equals(type)) {
                // type==innerPvc，注入自身的 volumeClaimTemplates 中，完整 pvc 定义
                JSONObject pvc = pvcs.getJSONObject(target.getString("pvc"));
                if (pvc == null) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                            String.format("cannot find pvc %s in pvcs", target.getString("pvc")));
                }
                addVolumeClaimTemplate(pvc);
            } else if ("staticPvc".equals(type)) {
                // type==staticPvc 时，创建独立 PVC 并挂载
                JSONObject pvc = pvcs.getJSONObject(target.getString("pvc"));
                if (pvc == null) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                            String.format("cannot find pvc %s in pvcs", target.getString("pvc")));
                }
                applyPvc(client, pvc);

                // add volume to self
                String pvcMetadataName = pvc.getJSONObject("metadata").getString("name");
                if (StringUtils.isEmpty(pvcMetadataName)) {
                    throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                            String.format("invalid pvc %s, cannot find name in metadata",
                                    target.getString("pvc")));
                }
                JSONObject volume = new JSONObject();
                volume.put("name", target.getString("pvc"));
                volume.put("persistentVolumeClaim", new JSONObject());
                volume.getJSONObject("persistentVolumeClaim").put("claimName", pvcMetadataName);
                addVolume(volume);
            } else {
                throw new AppException(AppErrorCode.INVALID_USER_ARGS,
                        String.format("invalid target type %s in trait spec", type));
            }

            // update mountPath
            JSONObject updateData = new JSONObject();
            updateData.put("name", target.getString("pvc"));
            updateData.put("mountPath", target.getString("mountPath"));
            if (StringUtils.isNotEmpty(target.getString("initContainer"))) {
                updateContainers("initContainer", target.getString("initContainer"), updateData);
            } else if (StringUtils.isNotEmpty(target.getString("container"))) {
                updateContainers("container", target.getString("container"), updateData);
            } else {
                String errorMessage = String.format("not found container or initContainer %s", target.toJSONString());
                throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
            }
        }
    }

    /**
     * update containers in string
     *
     * @param type        container or initContainer
     * @param name
     * @param updateData
     * @return
     */
    private void updateContainers(String type, String name, JSONObject updateData) {
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        log.info("pvc trait parent workload {}", workloadSpec.toJSONString());
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
        } else if ("StatefulSet".equals(workloadSpec.getString("kind"))) {
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
            container.putIfAbsent("volumeMounts", new JSONArray());
            JSONArray target = container.getJSONArray("volumeMounts");
            target.add(updateData);
        }
        log.info("pvc trait parent workload after update {}", workloadSpec.toJSONString());
    }

    /**
     * add volumeClaimTemplates
     *
     * @param updateData 更新的数据
     */
    private void addVolumeClaimTemplate(JSONObject updateData) {
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        JSONArray volumeClaimTemplates;
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSet = workloadSpec
                    .getJSONObject("cloneSet");
            cloneSet.putIfAbsent("volumeClaimTemplates", new JSONArray());
            volumeClaimTemplates = cloneSet.getJSONArray("volumeClaimTemplates");
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSet = workloadSpec.getJSONObject("advancedStatefulSet");
            advancedStatefulSet.putIfAbsent("volumeClaimTemplates", new JSONArray());
            volumeClaimTemplates = advancedStatefulSet.getJSONArray("volumeClaimTemplates");
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "not supported");
        }
        volumeClaimTemplates.add(updateData);
    }

    /**
     * update volumes
     *
     * @param volume 更新的数据
     */
    private void addVolume(JSONObject volume) {
        JSONObject workloadSpec = (JSONObject) getWorkloadRef().getSpec();
        JSONArray volumes;
        if (workloadSpec.get("cloneSet") != null) {
            JSONObject cloneSetSpec = workloadSpec
                    .getJSONObject("cloneSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            cloneSetSpec.putIfAbsent("volumes", new JSONArray());
            volumes = cloneSetSpec.getJSONArray("volumes");
        } else if (workloadSpec.get("advancedStatefulSet") != null) {
            JSONObject advancedStatefulSetSpec = workloadSpec
                    .getJSONObject("advancedStatefulSet")
                    .getJSONObject("template")
                    .getJSONObject("spec");
            advancedStatefulSetSpec.putIfAbsent("volumes", new JSONArray());
            volumes = advancedStatefulSetSpec.getJSONArray("volumes");
        } else {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "not supported");
        }
        volumes.add(volume);
    }


    /**
     * 应用 PVC 到 K8S 集群
     *
     * @param client K8S Client
     * @param pvc PVC CR 对象
     */
    private void applyPvc(DefaultKubernetesClient client, JSONObject pvc) {
        try {
            String namespace = getComponent().getNamespaceId();
            String name = pvc.getJSONObject("metadata").getString("name");
            pvc.getJSONObject("metadata").put("namespace", namespace);
            Resource<PersistentVolumeClaim> resource = client.persistentVolumeClaims()
                    .load(new ByteArrayInputStream(pvc.toJSONString().getBytes(StandardCharsets.UTF_8)));
            PersistentVolumeClaim current = client.persistentVolumeClaims()
                    .inNamespace(namespace)
                    .withName(name)
                    .get();
            if (current == null) {
                PersistentVolumeClaim result = resource.create();
                log.info("cr yaml has created in kubernetes|namespace={}|name={}|cr={}|result={}",
                        namespace, name, pvc.toJSONString(), JSONObject.toJSONString(result));
            } else {
                log.info("cr exists, do nothing|namespace={}|name={}|pvc={}",
                        namespace, name, pvc.toJSONString());
            }
        } catch (Exception e) {
            String errorMessage = String.format("apply cr yaml to kubernetes failed|cluster=%s|" +
                            "exception=%s|cr=%s", client, ExceptionUtils.getStackTrace(e),
                    pvc.toJSONString());
            log.error(errorMessage);
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, errorMessage);
        }
    }
}
