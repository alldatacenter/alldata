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
import com.alibaba.sreworks.domain.DO.TeamAccount;
import com.alibaba.sreworks.domain.repository.TeamAccountRepository;
import com.alibaba.sreworks.flyadmin.server.DTO.PluginResource;
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
public class PluginResourceService {

    @Autowired
    TeamAccountRepository teamAccountRepository;

    public Map<String, Map<String, String>> serviceMap;

    private static final ScheduledThreadPoolExecutor SCHEDULE_POOL_EXECUTOR = new ScheduledThreadPoolExecutor(
        2, new BasicThreadFactory.Builder().namingPattern("%d").daemon(true).build()
    );

    @PostConstruct
    public void postConstruct() throws ApiException {
        try {
            refreshService();
        } catch (ApiException e) {
            log.error("response Code: {} Headers: {} Body: {}",
                e.getCode(), e.getResponseHeaders(), e.getResponseBody());
            log.error("", e);
        }
        SCHEDULE_POOL_EXECUTOR.scheduleAtFixedRate(() -> {
            try {
                refreshService();
            } catch (ApiException e) {
                log.error("response Code: {} Headers: {} Body: {}",
                    e.getCode(), e.getResponseHeaders(), e.getResponseBody());
                log.error("", e);
            } catch (Exception e) {
                log.error("", e);
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
    }

    private void refreshService() throws ApiException {
        List<V1Service> serviceList = K8sUtil.listServiceForAllNamespaces("pluginType=RESOURCE");
        Map<String, Map<String, String>> serviceMap = new HashMap<>();
        for (V1Service service : serviceList) {
            try {
                String accountType = K8sUtil.getServiceLabel(service, "accountType");
                String resourceType = K8sUtil.getServiceLabel(service, "resourceType");

                if (!serviceMap.containsKey(accountType)) {
                    serviceMap.put(accountType, new HashMap<>());
                }
                serviceMap.get(accountType).put(resourceType, K8sUtil.getServiceEndpoint(service).get(0));
            } catch (Exception e) {
                log.error("", e);
            }
        }
        this.serviceMap = serviceMap;
    }

    public List<PluginResource> list(String accountType, String resourceType, JSONObject account, String user)
        throws IOException {
        String endpoint = serviceMap.get(accountType).get(resourceType);
        return new Requests(endpoint + "/list")
            .postJson("account", account).headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful()
            .getJSONArray(PluginResource.class);
    }

    public JSONObject getUsageDetail(Long accountId, String resourceType, String instanceName, String user)
        throws IOException {
        TeamAccount teamAccount = teamAccountRepository.findFirstById(accountId);
        String endpoint = serviceMap.get(teamAccount.getType()).get(resourceType);
        return new Requests(endpoint + "/getUsageDetail")
            .postJson(
                "account", teamAccount.detail(),
                "name", instanceName
            )
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful()
            .getJSONObject();
    }

}
