package com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeploy;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.repository.ClusterResourceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FlyadminAppmanagerDeployHelmAcService {

    @Autowired
    ClusterResourceRepository clusterResourceRepository;

    private void patchAcSpecComponent(
        JSONArray components, AppInstance appInstance, AppComponentInstance appComponentInstance) {
        components.add(JsonUtil.map(
            "dataInputs", new JSONArray(),
            "dataOutputs", new JSONArray(),
            "dependencies", new JSONArray(),
            "parameterValues", JsonUtil.list(JsonUtil.map(
                "name", "values",
                "value", appComponentInstance.detail().values(),
                "toFieldPaths", JsonUtil.list(
                    "spec.values"
                )
            )),
            "revisionName", "HELM|" + appComponentInstance.getName() + "|_",
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
                        //"name", "master"
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
            "traits", new JSONArray()
        ));
    }

    public void patchAc(JSONArray components, AppInstance appInstance, AppComponentInstance appComponentInstance) {
        patchAcSpecComponent(components, appInstance, appComponentInstance);
    }

}
