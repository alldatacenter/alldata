package com.alibaba.sreworks.appdev.server.services;

import java.io.IOException;
import java.util.List;

import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.repository.AppComponentInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppComponentRepository;
import com.alibaba.sreworks.domain.repository.AppInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppRepository;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeployService;

import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AppInstallService extends AbstractAppDeployService {

    @Autowired
    AppRepository appRepository;

    @Autowired
    AppInstanceRepository appInstanceRepository;

    @Autowired
    AppComponentRepository appComponentRepository;

    @Autowired
    AppComponentInstanceRepository appComponentInstanceRepository;

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    FlyadminAppmanagerDeployService flyadminAppmanagerDeployService;

    private void createNamespace(AppInstance appInstance) throws IOException, ApiException {
        CoreV1Api api = api(appInstance);
        V1Namespace namespace = new V1Namespace()
            .apiVersion("v1")
            .kind("Namespace")
            .metadata(new V1ObjectMeta()
                .name(appInstance.namespace())
                .labels(ImmutableMap.of(
                    "appInstanceId", appInstance.getId().toString())
                )
            );
        api.createNamespace(namespace, null, null, null);
    }

    private void createResourceQuota(AppInstance appInstance) throws IOException, ApiException {
        CoreV1Api api = api(appInstance);
        api.createNamespacedResourceQuota(appInstance.namespace(), getResourceQuota(appInstance),
            null, null, null);
    }

    public void deploy(AppInstance appInstance, List<AppComponentInstance> appComponentInstanceList)
        throws IOException, ApiException {
        createNamespace(appInstance);
        if (!"0".equals(appInstance.detail().getResource().getRequests().getCpu()) &&
            !"0".equals(appInstance.detail().getResource().getRequests().getMemory())) {
            createResourceQuota(appInstance);
        }
        run(appInstance, appComponentInstanceList);
    }

}
