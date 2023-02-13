package com.alibaba.sreworks.appdev.server.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.alibaba.sreworks.common.util.K8sUtil;
import com.alibaba.sreworks.domain.DO.AppComponentInstance;
import com.alibaba.sreworks.domain.DO.AppInstance;
import com.alibaba.sreworks.domain.DO.Cluster;
import com.alibaba.sreworks.domain.DTO.AppInstanceDetail;
import com.alibaba.sreworks.domain.repository.AppComponentInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppComponentRepository;
import com.alibaba.sreworks.domain.repository.AppInstanceRepository;
import com.alibaba.sreworks.domain.repository.AppRepository;
import com.alibaba.sreworks.domain.repository.ClusterRepository;
import com.alibaba.sreworks.flyadmin.server.services.FlyadminAppmanagerDeployService;

import com.google.common.collect.ImmutableMap;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1ResourceQuota;
import io.kubernetes.client.openapi.models.V1ResourceQuotaSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public abstract class AbstractAppDeployService {

    @Autowired
    AppComponentInstanceRepository appComponentInstanceRepository;

    @Autowired
    ClusterRepository clusterRepository;

    @Autowired
    FlyadminAppmanagerDeployService flyadminAppmanagerDeployService;

    public CoreV1Api api(AppInstance appInstance) throws IOException {
        Cluster cluster = clusterRepository.findFirstById(appInstance.getClusterId());
        ApiClient client = K8sUtil.client(cluster.getKubeconfig());
        return new CoreV1Api(client);
    }

    @SuppressWarnings("all")
    public V1ResourceQuota getResourceQuota(AppInstance appInstance) {
        V1ResourceQuotaSpec spec = new V1ResourceQuotaSpec().hard(new HashMap<>());
        AppInstanceDetail appInstanceDetail = appInstance.detail();
        if (appInstanceDetail.getResource() != null) {
            spec.getHard().putAll(appInstanceDetail.getResource().toV1ResourceQuotaSpec().getHard());
        }
        V1ResourceQuota resourceQuota = new V1ResourceQuota()
            .apiVersion("v1")
            .kind("ResourceQuota")
            .metadata(new V1ObjectMeta().name("mem-cpu"))
            .spec(spec);
        return resourceQuota;
    }

    public void run(AppInstance appInstance, List<AppComponentInstance> appComponentInstanceList)
        throws IOException, ApiException {
        appInstance.setAc(flyadminAppmanagerDeployService.getAc(appInstance, appComponentInstanceList));
        String appDeployId = flyadminAppmanagerDeployService.start(appInstance, appComponentInstanceList);
        String status = flyadminAppmanagerDeployService.get(appDeployId, appInstance.getCreator())
            .getString("deployStatus");
        appInstance.setAppDeployId(appDeployId);
        appInstance.setStatus(status);
    }

}
