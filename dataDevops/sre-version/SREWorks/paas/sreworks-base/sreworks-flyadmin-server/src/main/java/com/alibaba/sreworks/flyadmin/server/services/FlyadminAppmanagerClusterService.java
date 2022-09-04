package com.alibaba.sreworks.flyadmin.server.services;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.common.util.AppmanagerServiceUtil;
import com.alibaba.sreworks.common.util.JsonUtil;
import com.alibaba.sreworks.common.util.Requests;
import com.alibaba.sreworks.domain.DO.Cluster;
import com.alibaba.tesla.web.constant.HttpHeaderNames;

import io.kubernetes.client.openapi.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FlyadminAppmanagerClusterService {

    public void create(Cluster cluster, String user) throws IOException, ApiException {
        String name = String.format("%s#%s#%s", cluster.getTeamId(), cluster.getAccountId(), cluster.getName());
        log.info(AppmanagerServiceUtil.getEndpoint() + "/clusters");
        new Requests(AppmanagerServiceUtil.getEndpoint() + "/clusters")
            .postJson(
                "clusterId", cluster.getId() + "id",
                "clusterName", name,
                "clusterType", "kubernetes",
                "clusterConfig", JsonUtil.map(
                    "kube", cluster.kubeJsonObject()
                ),
                "masterFlag", false
            )
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .post()
            .isSuccessful();

    }

    public void delete(Long clusterId, String user) throws IOException, ApiException {
        new Requests(AppmanagerServiceUtil.getEndpoint() + "/clusters/" + clusterId)
            .headers(HttpHeaderNames.X_EMPL_ID, user)
            .delete().isSuccessful();
    }

}
