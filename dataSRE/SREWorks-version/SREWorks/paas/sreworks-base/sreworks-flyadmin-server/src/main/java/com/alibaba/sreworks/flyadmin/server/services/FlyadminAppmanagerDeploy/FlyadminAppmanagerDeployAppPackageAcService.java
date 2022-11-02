package com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeploy;

import java.io.IOException;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.domain.DO.AppComponent;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DO.AppPackage;
import com.alibaba.sreworks.domain.repository.AppComponentRepository;
import com.alibaba.sreworks.domain.repository.AppPackageRepository;
import com.alibaba.sreworks.domain.repository.ClusterResourceRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerPackageService;

import io.kubernetes.client.openapi.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FlyadminAppmanagerDeployAppPackageAcService {

    @Autowired
    ClusterResourceRepository clusterResourceRepository;

    @Autowired
    AppComponentRepository appComponentRepository;

    @Autowired
    AppPackageRepository appPackageRepository;

    @Autowired
    FlyadminAppmanagerPackageService flyadminAppmanagerPackageService;

    private void patchAcSpecComponent(
        JSONArray components, AppInstance appInstance, AppComponentInstance appComponentInstance)
        throws IOException, ApiException {

        AppComponent appComponent = appComponentRepository.findFirstById(appComponentInstance.getAppComponentId());
        Long appPackageId = appComponent.appPackageDetail().getId();
        AppPackage appPackage = appPackageRepository.findFirstById(appPackageId);
        flyadminAppmanagerPackageService.releaseAsCustom(appPackage, appInstance.getLastModifier());

        components.add(JsonUtil.map(
            "revisionName", String.format("CUSTOM_ADDON|%s@%s|%s",
                appPackage.app().getName(), appPackage.app().getName(), appPackage.getSimpleVersion()),
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
            )
        ));
    }

    public void patchAc(JSONArray components, AppInstance appInstance, AppComponentInstance appComponentInstance)
        throws IOException, ApiException {
        patchAcSpecComponent(components, appInstance, appComponentInstance);
    }

}
