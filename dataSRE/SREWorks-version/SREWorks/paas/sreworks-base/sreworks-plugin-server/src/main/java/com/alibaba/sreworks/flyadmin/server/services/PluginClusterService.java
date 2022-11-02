package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.K8sUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.sreworks.domain.DO.Cluster;
import com.alibaba.sreworks.domain.DO.TeamAccount;
import com.alibaba.sreworks.domain.repository.TeamAccountRepository;
import com.alibaba.sreworks.flyadmin.server.DTO.PluginCluster;
import com.alibaba.sreworks.flyadmin.server.DTO.PluginComponent;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Service
public class PluginClusterService {

    @Autowired
    TeamAccountRepository teamAccountRepository;

    public Map<String, String> serviceMap;

    private static final ScheduledThreadPoolExecutor SCHEDULE_POOL_EXECUTOR = new ScheduledThreadPoolExecutor(
        2, new BasicThreadFactory.Builder().namingPattern("%d").daemon(true).build()
    );

    @PostConstruct
    public void postConstruct() throws ApiException {
        refreshService();
        SCHEDULE_POOL_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                refreshService();
            } catch (Exception e) {
                log.error("", e);
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
    }

    private void refreshService() throws ApiException {
        List<V1Service> serviceList = K8sUtil.listServiceForAllNamespaces("pluginType=CLUSTER");
        Map<String, String> serviceMap = new HashMap<>(0);
        for (V1Service service : serviceList) {
            try {
                String serviceEndpoint = K8sUtil.getServiceEndpoint(service).get(0);
                String accountType = K8sUtil.getServiceLabel(service, "accountType");
                serviceMap.put(accountType, serviceEndpoint);
            } catch (Exception e) {
                log.error("", e);
            }
        }
        this.serviceMap = serviceMap;
    }

    public List<PluginCluster> list(Long accountId, String user) throws IOException {
        TeamAccount teamAccount = teamAccountRepository.findFirstById(accountId);
        log.info(JSONObject.toJSONString(accountId));
        log.info(JSONObject.toJSONString(teamAccount));
        log.info(JSONObject.toJSONString(serviceMap.get(teamAccount.getType())));
        return new Requests(serviceMap.get(teamAccount.getType()) + "/list")
            .postJson("account", teamAccount.detail())
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful()
            .getJSONArray(PluginCluster.class);
    }

    public String getKubeConfig(Long accountId, String clusterName, String user) throws IOException {
        TeamAccount teamAccount = teamAccountRepository.findFirstById(accountId);
        return new Requests(serviceMap.get(teamAccount.getType()) + "/getKubeConfig")
            .postJson("account", teamAccount.detail(), "name", clusterName)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful()
            .getString();
    }

    public String getIngressHost(Cluster cluster, String subIngressHost, String user) throws IOException {

        TeamAccount teamAccount = teamAccountRepository.findFirstById(cluster.getAccountId());
        return new Requests(serviceMap.get(teamAccount.getType()) + "/getIngressHost")
            .postJson(
                "account", teamAccount.detail(),
                "name", cluster.getClusterName(),
                "subIngressHost", subIngressHost
            )
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful()
            .getString();

    }

    public List<PluginComponent> getComponent(Cluster cluster, String user) throws IOException {

        TeamAccount teamAccount = teamAccountRepository.findFirstById(cluster.getAccountId());
        return new Requests(serviceMap.get(teamAccount.getType()) + "/getComponent", false)
            .postJson(
                "account", teamAccount.detail(),
                "name", cluster.getClusterName()
            )
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful()
            .getJSONArray(PluginComponent.class);

    }
}
