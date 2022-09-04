package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.DTO.NameAlias;
import com.alibaba.sreworks.common.util.K8sUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Service;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.stereotype.Service;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Service
public class PluginAccountService {

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
        List<V1Service> serviceList = K8sUtil.listServiceForAllNamespaces("pluginType=ACCOUNT");
        Map<String, String> serviceMap = new HashMap<>(0);
        for (V1Service service : serviceList) {
            try {
                String accountType = K8sUtil.getServiceLabel(service, "accountType");
                String serviceEndpoint = K8sUtil.getServiceEndpoint(service).get(0);
                serviceMap.put(accountType, serviceEndpoint);
            } catch (Exception e) {
                log.error("", e);
            }
        }
        this.serviceMap = serviceMap;
    }

    public List<NameAlias> keys(String type, String user) throws IOException {
        String url = serviceMap.get(type) + "/keys";
        return new Requests(url)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .get().isSuccessful()
            .getJSONArray(NameAlias.class);
    }

    public void check(String type, JSONObject detail, String user) throws IOException {
        String url = serviceMap.get(type) + "/check";
        boolean check = new Requests(url)
            .postJson(detail)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post().isSuccessful()
            .getBoolean();
        if (!check) {
            throw new RuntimeException("ACCOUNT DO NOT PASS CHECK");
        }
    }

}
