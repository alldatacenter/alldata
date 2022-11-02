package com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeploy;

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.UuidUtil;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DO.ClusterResource;
import com.alibaba.sreworks.domain.DTO.AppComponentInstanceDetail;
import com.alibaba.sreworks.domain.DTO.Config;
import com.alibaba.sreworks.domain.DTO.Port;
import com.alibaba.sreworks.domain.DTO.Volume;
import com.alibaba.sreworks.domain.repository.ClusterResourceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class FlyadminAppmanagerDeployRepoAcService {

    @Autowired
    ClusterResourceRepository clusterResourceRepository;

    private void patchAcSpecComponent(
        JSONArray components, AppInstance appInstance, AppComponentInstance appComponentInstance) {
        AppComponentInstanceDetail appComponentInstanceDetail = appComponentInstance.detail();
        components.add(JsonUtil.map(
            "revisionName", "K8S_MICROSERVICE|" + appComponentInstance.getName() + "|_",
            "scopes", JsonUtil.list(
                JsonUtil.map(
                    "scopeRef", JsonUtil.map(
                        "apiVersion", "flyadmin.alibaba.com/v1alpha1",
                        "kind", "Namespace",
                        "name", appInstance.namespace()
                    )
                ),
                JsonUtil.map(
                    "scopeRef", JsonUtil.map(
                        "apiVersion", "flyadmin.alibaba.com/v1alpha1",
                        "kind", "Cluster",
                        "name", appInstance.getClusterId() + "id"
                    )
                ),
                JsonUtil.map(
                    "scopeRef", JsonUtil.map(
                        "apiVersion", "flyadmin.alibaba.com/v1alpha1",
                        "kind", "Stage",
                        "name", appInstance.getStageId()
                    )
                )
            ),
            "traits", new JSONArray(),
            "parameterValues", JsonUtil.list(JsonUtil.map(
                "name", "REPLICAS",
                "value", appComponentInstanceDetail.getReplicas(),
                "toFieldPaths", JsonUtil.list("spec.replicas")
            ))
        ));
    }

    private void patchConfigmapTrait(JSONArray traits, AppInstance appInstance,
        AppComponentInstance appComponentInstance) {

        JSONArray targets = new JSONArray();
        JSONObject configmaps = new JSONObject();
        traits.add(JsonUtil.map(
            "name", "configmap.trait.abm.io",
            "runtime", "pre",
            "spec", JsonUtil.map(
                "targets", targets,
                "configmaps", configmaps
            )
        ));
        AppComponentInstanceDetail appComponentInstanceDetail = appComponentInstance.detail();
        List<Config> configs = appComponentInstanceDetail.configs();
        for (Config config : configs) {
            String name = UuidUtil.shortUuid();
            targets.add(JsonUtil.map(
                "type", "volumeMount",
                "container", appComponentInstance.getName(),
                "mountPath", config.getParentPath(),
                "configmap", name
            ));
            configmaps.put(name, JsonUtil.map(
                config.getName(), config.getContent()
            ));
        }

        List<Long> clusterResourceIdList = appInstance.detail().clusterResourceIdList();
        if (CollectionUtils.isEmpty(clusterResourceIdList)) {
            return;
        }
        for (Long clusterResourceId : clusterResourceIdList) {
            ClusterResource clusterResource = clusterResourceRepository.findFirstById(clusterResourceId);
            JSONObject usageDetail = clusterResource.usageDetail();
            String name = UuidUtil.shortUuid();
            targets.add(JsonUtil.map(
                "type", "env",
                "container", appComponentInstance.getName(),
                "configmap", name
            ));
            JSONObject configmap = new JSONObject();
            configmaps.put(name, configmap);
            for (String key : usageDetail.keySet()) {
                Object value = usageDetail.get(key);
                configmap.put(clusterResource.getName() + "_" + key, value);
            }
        }

    }

    private void patchPvcTrait(JSONArray traits, AppComponentInstance appComponentInstance) {
        JSONArray targets = new JSONArray();
        JSONObject pvcs = new JSONObject();
        traits.add(JsonUtil.map(
            "name", "pvc.trait.abm.io",
            "runtime", "pre",
            "spec", JsonUtil.map(
                "targets", targets,
                "pvcs", pvcs
            )
        ));
        AppComponentInstanceDetail appComponentInstanceDetail = appComponentInstance.detail();
        List<Volume> volumes = appComponentInstanceDetail.volumes();
        for (Volume volume : volumes) {
            targets.add(JsonUtil.map(
                "container", appComponentInstance.getName(),
                "mountPath", volume.getPath(),
                "pvc", volume.getName()
            ));
            pvcs.put(volume.getName(), JsonUtil.map(
                "accessModes", JsonUtil.list("ReadWriteMany"),
                "resources", JsonUtil.map(
                    "requests", JsonUtil.map(
                        "storage", volume.getStorage()
                    )
                ),
                "storageClassName", volume.getStorageClassName()
            ));
        }

    }

    private void patchServiceTrait(JSONArray traits, AppComponentInstance appComponentInstance) {
        AppComponentInstanceDetail appComponentInstanceDetail = appComponentInstance.detail();
        List<Port> ports = appComponentInstanceDetail.ports();
        ports.add(Port.builder().name("metric").value(10080L).build());
        traits.add(JsonUtil.map(
            "name", "service.trait.abm.io",
            "runtime", "post",
            "spec", JsonUtil.map(
                "ports", ports.stream().map(port -> JsonUtil.map(
                    "name", port.getName(),
                    "protocol", "TCP",
                    "port", port.getValue(),
                    "targetPort", port.getValue()
                )).collect(Collectors.toList())
            )
        ));
    }

    private void patchResourceLimitTrait(JSONArray traits, AppComponentInstance appComponentInstance) {
        AppComponentInstanceDetail detail = appComponentInstance.detail();
        traits.add(JsonUtil.map(
            "name", "resourceLimit.trait.abm.io",
            "runtime", "pre",
            "spec", JsonUtil.map(
                "resources", JsonUtil.map(
                    "limits", JsonUtil.map(
                        "cpu", detail.getResource().getLimits().getCpu(),
                        "memory", detail.getResource().getLimits().getMemory()
                    ),
                    "requests", JsonUtil.map(
                        "cpu", detail.getResource().getRequests().getCpu(),
                        "memory", detail.getResource().getRequests().getMemory()
                    )
                )
            )
        ));
    }

    public void patchAc(JSONArray components, AppInstance appInstance, AppComponentInstance appComponentInstance) {
        patchAcSpecComponent(components, appInstance, appComponentInstance);
        JSONArray traits = components.getJSONObject(components.size() - 1).getJSONArray("traits");
        patchConfigmapTrait(traits, appInstance, appComponentInstance);
        patchServiceTrait(traits, appComponentInstance);
        patchPvcTrait(traits, appComponentInstance);
        patchResourceLimitTrait(traits, appComponentInstance);
    }

}
